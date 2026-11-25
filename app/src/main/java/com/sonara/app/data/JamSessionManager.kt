package com.sonara.app.data

import android.content.Context
import android.util.Log
import com.sonara.app.player.PlayerManager
import com.sonara.app.ui.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/** Peran client di sesi Jam. */
enum class JamRole { HOST, GUEST }

/** Status koneksi sesi — ditampilkan jujur di UI, tidak pernah mengklaim sinkron bila tidak. */
enum class JamConnection { IDLE, CONNECTING, LIVE, RECONNECTING, OFFLINE }

/** State UI sesi Jam untuk semua layar (bar sesi, dialog, pill LIVE). */
data class JamUiState(
    val active: Boolean = false,
    val role: JamRole = JamRole.GUEST,
    val code: String = "",
    val hostName: String = "",
    val connection: JamConnection = JamConnection.IDLE,
    /** Transport aktif: "SSE" atau "Polling" (fallback). Kosong bila tidak aktif. */
    val transport: String = "",
    /** Anggota yang heartbeat-nya segar (< 90 detik). */
    val members: List<JamMember> = emptyList(),
    val queue: List<JamQueueItem> = emptyList(),
    /**
     * Antrean sesi yang masih akan diputar — [queue] dibuang sampai
     * lagu yang sedang diputar host (lihat [upcomingJamQueue]).
     * Daftar inilah yang ditampilkan dialog Jam DAN tab Up Next,
     * sehingga keduanya selalu sama dan maju bersama.
     */
    val upcomingQueue: List<JamQueueItem> = emptyList(),
    val nowTitle: String = "",
    val nowArtist: String = "",
    /** True bila host sedang memutar lagu lokal (tamu tidak bisa mengikuti). */
    val hostLocalOnly: Boolean = false,
    /** True bila state host belum ada (tamu menunggu lagu pertama). */
    val waitingForHost: Boolean = false,
    /** True bila host tidak ada heartbeat > 60 detik — playback lokal terus, tidak pura-pura sinkron. */
    val hostAway: Boolean = false,
    /**
     * Masalah mengikuti audio host (v1.10.18, tamu saja): null bila
     * mengikuti dengan sehat; teks jujur bila player lokal tidak
     * berbunyi padahal snapshot host bilang playing (retry otomatis
     * sedang berjalan / sudah habis). Tidak pernah mengklaim
     * playing palsu — lihat guestFollowWatch.
     */
    val followProblem: String? = null,
    /**
     * Reaksi cepat yang masih segar (< 60 detik, lihat
     * [pruneReactions]) — ephemeral, bukan riwayat. UI menampilkannya
     * sekilas; tidak pernah disimpan permanen.
     */
    val recentReactions: List<JamReaction> = emptyList(),
    /**
     * True bila tamu boleh MEMINTA kontrol putar (v1.10.42): toggle
     * sesi milik host (default false). Tamu tetap tidak pernah
     * memutar lokal — permintaan lewat controlRequests.
     */
    val sharedControl: Boolean = false,
    /** True bila client ini boleh mengontrol/minta kontrol putar (host, atau tamu saat sharedControl). */
    val canControl: Boolean = true,
    /** Jumlah vote per push key antrean (v1.10.42). */
    val voteCounts: Map<String, Int> = emptyMap(),
    /** Push key antrean yang sudah di-vote HP ini (untuk status toggle tombol vote). */
    val votedKeys: Set<String> = emptySet(),
    /** Pesan chat sesi, urutan kirim (v1.10.42). */
    val messages: List<JamChatMessage> = emptyList(),
    /** Anggota yang heartbeat-nya < 30 detik — yang tampil di orbit (v1.10.42). */
    val orbitMembers: List<JamMember> = emptyList(),
)

/**
 * Orkestrasi Jamming Together (v1.10.6).
 *
 * Prinsip: HOST adalah satu-satunya penulis state pemutaran; tamu
 * HANYA membaca + menerapkan. Tidak ada echo loop karena host tidak
 * pernah menerapkan state dari server dan tamu tidak pernah menulis
 * state. Antrean + presence ditulis semua peran lewat path-nya
 * masing-masing (push key / members/$uid) sehingga tidak bentrok.
 *
 * Jam MEMAKAI jalur resolve + playback PlayerManager yang sudah ada
 * (playTrack / pause / resume / seekTo) — tidak ada logika putar baru
 * dan tidak ada perubahan pada PlayerManager.
 *
 * Sinkronisasi posisi (v1.10.12): state membawa positionMs +
 * updatedAtMs jam SERVER, tapi tamu TIDAK menghitung dari jam
 * server — `stateReceivedAtMs` (jam lokal saat versi state tiba)
 * adalah baseline-nya, sehingga skew jam antar-HP tidak menjadi
 * offset. Host mempublish ulang saat lagu benar-benar mulai
 * berbunyi (hook audible-start), publish flip di-retry, dan SSE
 * yang stall langsung fallback ke polling.
 */
class JamSessionManager(
    private val appContext: Context,
    private val player: PlayerManager,
    private val sync: SyncManager,
    private val rest: JamRest = JamRest(SonaraHttp.defaultHttp()),
    private val clockMs: () -> Long = System::currentTimeMillis,
    /**
     * Jeda publish posisi berkala host (v1.10.15, default ~10 detik).
     * Parameter supaya harness bisa memakai jeda pendek; perilaku
     * produksi tidak berubah.
     */
    private val positionPublishIntervalMs: Long = 10_000L,
    /**
     * Parameter watchdog follow-health tamu (v1.10.18) — default
     * produksi; harness memakai nilai pendek supaya skenario
     * berjalan dalam hitungan detik, perilaku produksi tidak berubah.
     */
    private val followWatchIntervalMs: Long = 2_000L,
    private val followStallMs: Long = 6_000L,
    private val followBufferStallMs: Long = 20_000L,
    /**
     * Interval jaring pengaman polling tamu (v1.10.35, default
     * produksi): GET sesi hanya bila tidak ada snapshot segar.
     * Parameter supaya perilaku produksi tidak berubah bila call
     * site lama dipakai; nilai default yang dipakai app.
     */
    private val safetyPollIntervalMs: Long = 3_000L,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    /** True bila tamu sedang mengikuti sesi — kontrol putar lokal dikunci di UI. */
    private val _guestLocked = MutableStateFlow(false)
    val guestLocked: StateFlow<Boolean> = _guestLocked.asStateFlow()

    private class SessionGone : Exception("Session expired, sign in again.")

    /**
     * Kegagalan TRANSIEN saat memulihkan kredensial di tengah sesi
     * (jaringan/server saat retry-401). BUKAN sesi kedaluwarsa:
     * pemanggil harus backoff/polling ulang, TIDAK BOLEH toast
     * session-expired atau teardown (v1.10.8 — sebelumnya
     * withToken meruntuhkan SEMUA kegagalan kredensial menjadi
     * SessionGone).
     */
    private class JamTransient : Exception("Jam credentials refresh failed transiently")

    @Volatile
    private var sessionScope: CoroutineScope? = null

    /** True selama sesi berjalan — kondisi semua loop sesi (bukan Job.isActive). */
    @Volatile
    private var sessionActive: Boolean = false

    @Volatile
    private var currentCreds: JamCredentials? = null

    @Volatile
    private var sessionCode: String = ""

    @Volatile
    private var role: JamRole = JamRole.GUEST

    /** Offset jam server (ms). 0 = tidak tersedia (fallback). Dipakai HANYA untuk freshness presence, bukan posisi. */
    @Volatile
    private var clockOffsetMs: Long = 0L

    /**
     * Jam lokal (clockMs) saat versi state terakhir TIBA di HP ini
     * (v1.10.12) — baseline `expectedPositionMs`, menggantikan jam
     * server. Diperbarui hanya bila `versionKey` state berubah
     * (lihat [noteStateReceipt]); event heartbeat/anggota yang
     * membawa state lama TIDAK boleh memajukannya.
     */
    @Volatile
    private var stateReceivedAtMs: Long = 0L

    @Volatile
    private var lastStateVersionKey: String = ""

    /**
     * Kunci generasi lagu yang sudah dipublish audible-start-nya
     * (v1.10.12): "$trackId@$generation". Satu generasi = satu
     * publish audible-start; callback berulang untuk generasi yang
     * sama (resume, callback embed periodik) di-dedup di sini.
     */
    @Volatile
    private var audiblePublishedKey: String? = null

    /**
     * True bila publish flip terakhir GAGAL total setelah retry
     * (v1.10.12) — publishUi menampilkannya sebagai RECONNECTING
     * (jujur: state host belum tentu sampai ke tamu), sampai ada
     * publish berikutnya yang berhasil. Tidak mengubah `connection`
     * milik streamLoop.
     */
    @Volatile
    private var publishDegraded: Boolean = false

    /** Node sesi mentah (hasil gabungan event SSE / polling). */
    @Volatile
    private var nodeJson: JSONObject = JSONObject()

    private val snapshotFlow = MutableStateFlow<JamSnapshot?>(null)

    @Volatile
    private var lastSnapshotAtMs: Long = 0L

    @Volatile
    private var hadContent: Boolean = false

    @Volatile
    private var endedHandled: Boolean = false

    @Volatile
    private var connection: JamConnection = JamConnection.IDLE

    @Volatile
    private var transport: String = ""

    /** Push key antrean yang sudah ditambahkan host ke antrean lokal (anti-duplikat). */
    private val appendedQueueKeys = mutableSetOf<String>()

    /**
     * Pemetaan queueUid salinan antrean lokal host → push key antrean
     * sesi (v1.10.18). Dibangun saat hostQueueWatch meng-append
     * (nilai kembali addToQueue membawa queueUid salinannya), dipakai
     * publishFromPlayer untuk menulis queueKey state. Tanpa ini host
     * tidak bisa membedakan dua salinan dengan videoId sama.
     */
    private val queueKeyByQueueUid = mutableMapOf<String, String>()

    /** High-water mark konsumsi lokal host (lihat JamState.consumedThroughKey). */
    private var consumedThroughKeyLocal: String = ""

    // ---- Lompat Up Next + autoplay (v1.10.34) -------------------------
    /** videoId yang sudah dimainkan sesi ini (host) — autoplay tidak boleh mengulanginya langsung. */
    private val sessionPlayedVideoIds = mutableSetOf<String>()

    /** Kunci permintaan lompat yang sudah diterapkan host (anti-aplikasi-ganda). */
    private val appliedJumpRequestKeys = mutableSetOf<String>()

    /** Kunci permintaan reorder yang sudah diterapkan host (anti-aplikasi-ganda, v1.10.36). */
    private val appliedReorderRequestKeys = mutableSetOf<String>()

    /** Kunci permintaan kontrol yang sudah diterapkan host (anti-aplikasi-ganda, v1.10.42). */
    private val appliedControlRequestKeys = mutableSetOf<String>()

    /**
     * Waktu kirim chat terakhir dari HP ini (v1.10.42) — rate limit
     * lokal 1 pesan per detik. Direset tiap sesi baru + teardown.
     */
    @Volatile
    private var lastChatSentAtMs: Long = 0L

    /** True selama satu percobaan autoplay sedang berjalan (anti pemicu ganda untuk lagu yang sama). */
    @Volatile
    private var autoplayInFlight: Boolean = false

    /** Client YTM untuk fetch feed + cek resolve autoplay (dibuat malas — hanya host yang memakainya). */
    private val autoplayApi by lazy { YoutubeMusicApi() }

    private data class Published(val videoId: String, val playing: Boolean, val positionMs: Long, val atMs: Long)

    @Volatile
    private var lastPublished: Published? = null

    /**
     * Status prefetch tamu (v1.10.15, multi-target v1.10.35): waktu
     * percobaan prefetch terakhir PER videoId. Cooldown per id
     * (PREFETCH_COOLDOWN_MS) supaya satu snapshot yang sama tidak
     * memicu resolve berulang saat prefetch sedang berjalan, tanpa
     * memblokir percobaan untuk lagu lain. Direset tiap sesi baru +
     * teardown; entri lama dibuang berkala di maybePrefetchNext.
     */
    private val prefetchAttemptAtMs = mutableMapOf<String, Long>()

    /**
     * Waktu kirim reaksi terakhir dari HP ini (v1.10.17) — rate
     * limit lokal 1 reaksi per 2 detik. Direset tiap sesi baru +
     * teardown. Hanya pengirim yang dibatasi; menerima tidak.
     */
    @Volatile
    private var lastReactionSentAtMs: Long = 0L

    // ---- Watchdog follow-health tamu (v1.10.18) -------------------
    // Akar masalah (laporan HP 22 Nov, sesi 25QHCB): progres tamu
    // tetap jalan tapi suara hilang, pulih setelah play manual.
    // Penyebab dari kode: _isPlaying PlayerManager bersifat
    // OPTIMISTIS (resume()/playExo() menandainya true sebelum audio
    // benar-benar berbunyi, dan tidak pernah dikoreksi bila player
    // masuk IDLE setelah error / ENDED tanpa antrean lanjutan),
    // sementara loop tamu (konvergensi + drift) mempercayai klaim
    // itu — resume() tidak pernah dipanggil ulang karena isPlaying
    // sudah true, dan tidak ada yang me-resolve ulang. Play manual
    // memulihkan karena playTrack me-resolve dari nol. Watchdog ini
    // memeriksa KESEHATAN player lokal (bukan klaimnya): bila
    // snapshot bilang playing tapi player tidak berbunyi (error/
    // ended/stall melewati ambang), ia me-resolve ulang otomatis
    // dengan anggaran terbatas + backoff, lalu menampilkan status
    // jujur bila tetap gagal. TIDAK PERNAH membiarkan status
    // playing palsu tanpa tindakan.
    @Volatile
    private var followVersionKey: String = ""
    @Volatile
    private var followAttempts: Int = 0
    @Volatile
    private var followTick: Long = 0L
    @Volatile
    private var followLastAttemptTick: Long = -100L
    @Volatile
    private var followLastPosMs: Long = 0L
    @Volatile
    private var followLastPosAtMs: Long = 0L
    @Volatile
    private var followPrevTickPosMs: Long = 0L
    @Volatile
    private var followBufferingSinceMs: Long = 0L
    @Volatile
    private var followEndedSinceMs: Long = 0L
    @Volatile
    private var followHealthyTicks: Int = 0
    @Volatile
    private var followProblemInternal: String? = null

    private fun setFollowProblem(text: String?) {
        if (followProblemInternal == text) return
        followProblemInternal = text
        publishUi()
    }

    // ---- API publik (UI) -------------------------------------------------

    /** Teks share kode sesi (bahasa Inggris, seperti semua string baru v1.10.6). */
    fun shareText(code: String): String =
        "Join my Jam on SONARA! Tap to join: ${jamShareUrl(code)} — " +
            "or open SONARA, tap Jam, and enter the code $code."

    fun isInSession(): Boolean = sessionScope != null

    /** Buat sesi baru sebagai host. Callback di thread apa pun (toast aman). */
    fun createSession(onResult: (Result<String>) -> Unit) {
        scope.launch {
            // v1.10.7: pastikan token SEGAR dulu (refresh bila perlu)
            // SEBELUM menulis apa pun ke RTDB — jangan tampilkan
            // dialog kode untuk sesi yang tidak pernah tertulis.
            // v1.10.8: bedakan Invalid (minta login) dari Transient
            // (jaringan — sesi utuh, jangan minta login ulang).
            val creds = try {
                sync.jamCredentialsOrThrow(forceRefresh = true)
            } catch (e: JamAuthInvalid) {
                onResult(Result.failure(Exception("Sign in with Google to start a Jam.")))
                return@launch
            } catch (e: JamAuthTransient) {
                onResult(Result.failure(Exception("No internet — try again later.")))
                return@launch
            }
            var code = ""
            for (attempt in 1..5) {
                val candidate = generateJamCode()
                val existing = runCatching {
                    rest.getSessionJson(candidate, creds.idToken)
                }.getOrNull()
                val snap = existing?.let { parseJamSnapshot(it) }
                if (snap == null || snap.ended) {
                    code = candidate
                    break
                }
            }
            if (code.isEmpty()) {
                onResult(Result.failure(Exception("Couldn't create a session code — try again.")))
                return@launch
            }
            val body = JSONObject()
            body.put(
                "host",
                JSONObject().put("uid", creds.uid)
                    .put("name", creds.displayName.ifEmpty { "Host" }),
            )
            body.put("queue", JSONObject())
            body.put(
                "members",
                JSONObject().put(creds.uid, memberJson(creds)),
            )
            body.put("ended", false)
            body.put("createdAtMs", serverTimestampJson())
            try {
                rest.putSession(code, body, creds.idToken)
            } catch (e: Exception) {
                Log.w(TAG, "Jam create failed: ${e.message}")
                onResult(Result.failure(Exception(friendlyJamError(e))))
                return@launch
            }
            currentCreds = creds
            startSession(code, JamRole.HOST, creds)
            showToast(appContext, "Jam started — code $code")
            onResult(Result.success(code))
        }
    }

    /** Gabung sesi dengan kode. Callback di thread apa pun. */
    fun joinSession(codeRaw: String, onResult: (Result<Unit>) -> Unit) {
        val code = normalizeJamCode(codeRaw)
        if (!isValidJamCode(code)) {
            onResult(Result.failure(Exception("Enter the 6-character Jam code.")))
            return
        }
        scope.launch {
            // v1.10.7: token segar dulu sebelum membaca sesi (sama
            // seperti createSession). v1.10.8: bedakan Invalid dari
            // Transient seperti createSession.
            val creds = try {
                sync.jamCredentialsOrThrow(forceRefresh = true)
            } catch (e: JamAuthInvalid) {
                onResult(Result.failure(Exception("Sign in with Google to join a Jam.")))
                return@launch
            } catch (e: JamAuthTransient) {
                onResult(Result.failure(Exception("No internet — try again later.")))
                return@launch
            }
            val json = try {
                rest.getSessionJson(code, creds.idToken)
            } catch (e: Exception) {
                Log.w(TAG, "Jam join read failed: ${e.message}")
                onResult(Result.failure(Exception(friendlyJamError(e))))
                return@launch
            }
            if (json == null) {
                onResult(Result.failure(Exception("No Jam with that code.")))
                return@launch
            }
            val snap = parseJamSnapshot(json)
            if (snap?.ended == true) {
                onResult(Result.failure(Exception("This Jam has ended.")))
                return@launch
            }
            currentCreds = creds
            startSession(code, JamRole.GUEST, creds, seedJson = json)
            try {
                withToken { c ->
                    rest.patchNode(
                        "jam/$code/members/${c.uid}",
                        memberJson(c),
                        c.idToken,
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Jam presence write failed: ${e.message}")
            }
            showToast(appContext, "Joined the Jam.")
            onResult(Result.success(Unit))
        }
    }

    /** Tamu keluar — playback lokal lanjut normal, tidak disentuh. */
    fun leaveSession() {
        val code = sessionCode
        val creds = currentCreds
        scope.launch {
            if (code.isNotEmpty() && creds != null && role == JamRole.GUEST) {
                runCatching {
                    rest.deleteNode("jam/$code/members/${creds.uid}", creds.idToken)
                }
            }
        }
        teardown()
        showToast(appContext, "You left the Jam.")
    }

    /** Host mengakhiri sesi untuk semua orang. */
    fun endSession() {
        val code = sessionCode
        // v1.10.9: tangkap kredensial SEBELUM teardown (pola yang
        // sama dengan leaveSession). Sebelumnya withToken dipanggil
        // dari coroutine yang jalan SETELAH teardown mengosongkan
        // currentCreds → SessionGone → flag ended TIDAK PERNAH
        // tertulis, tamu tidak pernah tahu sesi berakhir.
        val creds = currentCreds
        scope.launch {
            if (code.isNotEmpty() && creds != null) {
                runCatching {
                    rest.patchNode("jam/$code", JSONObject().put("ended", true), creds.idToken)
                }
            }
        }
        teardown()
        showToast(appContext, "Jam ended.")
    }

    /** Tambah lagu ke antrean sesi (semua peran). Lagu lokal ditolak jujur. */
    fun addTrackToQueue(track: Track, onResult: (String) -> Unit) {        val code = sessionCode
        if (sessionScope == null || code.isEmpty()) {
            onResult("You're not in a Jam.")
            return
        }
        val videoId = track.youtubeVideoId
        if (videoId.isNullOrBlank()) {
            onResult("Only YouTube Music songs can be added to a Jam.")
            return
        }
        scope.launch {
            val creds = currentCreds
            if (creds == null) {
                // Sesi sedang berakhir/teardown — bukan sesi
                // kedaluwarsa; pesan jujur yang sama dengan guard
                // di atas (v1.10.8).
                onResult("You're not in a Jam.")
                return@launch
            }
            val item = JSONObject()
            item.put("videoId", videoId)
            item.put("title", track.title)
            item.put("artist", track.artist)
            item.put("artworkUrl", track.imageUrl.orEmpty())
            item.put("addedBy", creds.uid)
            item.put("addedByName", creds.displayName.ifEmpty { "Someone" })
            item.put("addedAtMs", serverTimestampJson())
            try {
                withToken { c -> rest.postQueueItem(code, item, c.idToken) }
                onResult("Added to the Jam queue.")
            } catch (e: Exception) {
                Log.w(TAG, "Jam queue add failed: ${e.message}")
                onResult(friendlyJamError(e))
            }
        }
    }

    /**
     * Hapus SATU item dari antrean sesi (v1.10.18, HOST SAJA).
     * Keputusan desain: tamu read-only seperti kontrol playback —
     * tamu tidak melihat tombol hapus dan panggilan tamu ditolak
     * dengan toast host-only yang sama polanya dengan kontrol.
     *
     * Audit hostQueueWatch + upcomingJamQueue sebelum menulis ini:
     * - Item yang sudah lewat (prefix sebelum lagu berjalan) aman
     *   dihapus: upcomingJamQueue membuang prefix itu dari snapshot,
     *   dan hostQueueWatch hanya meng-append kunci yang BELUM
     *   dilihat — kunci yang dihapus tidak pernah di-append, kunci
     *   yang sudah di-append tidak di-append ulang.
     * - Item yang sedang berjalan TIDAK boleh dihapus: panggilan
     *   ini no-op jujur (tanpa DELETE RTDB) supaya advance host
     *   dan lagu berjalan tidak berubah. UI tidak menampilkan
     *   tombol hapus untuk lagu berjalan; guard ini jaring pengaman.
     * - Item upcoming: DELETE child queue/$key lewat deleteNode +
     *   withToken yang sama dengan addTrackToQueue (rules auth-only
     *   yang sama, TANPA perubahan skema/rules), lalu salinan
     *   lokalnya ikut dibuang lewat removeQueuedByVideoId supaya
     *   pemutar host tidak advance ke lagu yang baru dihapus.
     *   Snapshot berikutnya menghitung ulang upcomingQueue di host
     *   + tamu lewat jalur stream/polling yang sama.
     * TANPA Undo: antrean RTDB terurut push-key — tambah ulang
     * menaruh lagu di AKHIR, jadi undo akan berbohong soal posisi.
     */
    fun removeQueueItem(item: JamQueueItem, onResult: (String) -> Unit) {
        val code = sessionCode
        if (sessionScope == null || code.isEmpty()) {
            onResult("You're not in a Jam.")
            return
        }
        if (role != JamRole.HOST) {
            onResult("Only the host can remove songs from the Jam queue.")
            return
        }
        if (item.key.isBlank()) {
            onResult("That song is no longer in the Jam queue.")
            return
        }
        val st = snapshotFlow.value?.state
        // v1.10.18: lagu berjalan dilindungi PER KUNCI antrean
        // (bukan per videoId) — salinan lain dengan videoId sama
        // BOLEH dihapus. Fallback videoId hanya bila state belum
        // membawa queueKey (state lama / lagu luar antrean).
        val isCurrent = when {
            !st?.queueKey.isNullOrBlank() -> item.key == st.queueKey
            !st?.videoId.isNullOrBlank() -> item.videoId == st.videoId
            else -> false
        }
        if (isCurrent) {
            onResult("The song that's playing can't be removed from the Jam queue.")
            return
        }
        scope.launch {
            val creds = currentCreds
            if (creds == null) {
                onResult("You're not in a Jam.")
                return@launch
            }
            try {
                withToken { c -> rest.deleteNode("jam/$code/queue/${item.key}", c.idToken) }
                // v1.10.42: vote untuk item yang dihapus ikut dibuang
                // (kalau tidak, node votes menumpuk vote basi untuk
                // kunci yang tidak ada lagi di antrean).
                runCatching {
                    withToken { c -> rest.deleteNode("jam/$code/votes/${item.key}", c.idToken) }
                }
                // Buang salinan lokal host (lihat KDoc di atas) —
                // lewat thread Main seperti append hostQueueWatch.
                // v1.10.18: pemetaan uid→kunci untuk kunci yang
                // dihapus juga dibuang (salinan lokalnya ikut
                // terhapus di bawah).
                queueKeyByQueueUid.entries.removeAll { it.value == item.key }
                withContext(Dispatchers.Main) {
                    runCatching { player.removeQueuedByVideoId(item.videoId) }
                }
                onResult("Removed from the Jam queue.")
            } catch (e: Exception) {
                Log.w(TAG, "Jam queue remove failed: ${e.message}")
                onResult(friendlyJamError(e))
            }
        }
    }

    // ---- Lompat ke lagu Up Next + autoplay (v1.10.34) ------------------

    /**
     * Apakah item ini lagu yang sedang diputar host (guard bersama
     * lompat + hapus): per kunci antrean dulu, fallback videoId bila
     * state belum membawa queueKey (state lama / lagu luar antrean).
     */
    private fun isCurrentSessionItem(item: JamQueueItem, st: JamState?): Boolean {
        return when {
            !st?.queueKey.isNullOrBlank() -> item.key == st.queueKey
            !st?.videoId.isNullOrBlank() -> item.videoId == st.videoId
            else -> false
        }
    }

    /**
     * Lompat sesi ke satu item Up Next — HOST SAJA, dipanggil dari
     * ketuk baris Up Next (kartu Jam + tab detail).
     *
     * Host memutar SALINAN antrean lokal yang dipetakan ke push key
     * item itu (bukan `playTrack` per id — dua salinan dengan videoId
     * sama harus dibedakan, kalau tidak salinan yang sudah lewat bisa
     * terputar lagi dan Up Next tidak maju). Bila salinan itu belum
     * sempat di-append hostQueueWatch (snapshot lebih dulu dari
     * append), host meng-append-nya sendiri di thread Main dan
     * mencatat pemetaannya, supaya tidak ter-append ganda. Setelah
     * lagu berganti, state diterbitkan paksa lewat jalur publish
     * yang sama — tamu mengikuti lewat jalur sinkron yang sudah ada,
     * TANPA langkah tunggu baru sebelum tamu bisa berganti lagu.
     * Ketuk lagu yang sedang diputar = no-op jujur. Tamu yang
     * memanggil ini ditolak (pakai [requestJumpToQueueItem]).
     */
    fun jumpToQueueItem(item: JamQueueItem, onResult: (String) -> Unit) {
        val code = sessionCode
        if (sessionScope == null || code.isEmpty()) {
            onResult("You're not in a Jam.")
            return
        }
        if (role != JamRole.HOST) {
            onResult("Only the host can jump to a song in the Jam queue.")
            return
        }
        if (item.key.isBlank() || item.videoId.isBlank()) {
            onResult("That song is no longer in the Jam queue.")
            return
        }
        if (isCurrentSessionItem(item, snapshotFlow.value?.state)) {
            onResult("\"${item.title}\" is already playing.")
            return
        }
        scope.launch {
            val applied = applyJumpToItem(item)
            onResult(
                if (applied) "Jumped to \"${item.title}\"."
                else "That song is no longer in the Jam queue.",
            )
        }
    }

    /**
     * Terapkan lompat ke item (host, dipakai ketuk host + permintaan
     * tamu). Mengembalikan true bila lagu berhasil diputar.
     * Dijalankan dari scope IO; sentuhan player lewat thread Main.
     */
    private suspend fun applyJumpToItem(item: JamQueueItem): Boolean {
        if (!sessionActive || role != JamRole.HOST) return false
        val targetKey = item.key
        val applied = withContext(Dispatchers.Main) {
            val list = player.queue.value
            val idx = list.indexOfFirst { track ->
                queueKeyByQueueUid[track.queueUid] == targetKey
            }
            if (idx >= 0) {
                player.playAt(idx)
                true
            } else {
                // Belum di-append (balapan snapshot vs hostQueueWatch):
                // append sendiri + catat kunci supaya hostQueueWatch
                // tidak meng-append-nya dua kali.
                appendedQueueKeys.add(targetKey)
                val appended = player.addToQueue(item.toTrack())
                queueKeyByQueueUid[appended.queueUid] = targetKey
                val at = player.queue.value.indexOfFirst { it.queueUid == appended.queueUid }
                if (at >= 0) {
                    player.playAt(at)
                    true
                } else {
                    false
                }
            }
        }
        if (applied) {
            sessionPlayedVideoIds.add(item.videoId)
            publishFromPlayer(force = true)
        }
        return applied
    }

    /**
     * Minta host melompat ke satu item Up Next — SEMUA peran (jalur
     * tamu untuk ketuk baris Up Next, v1.10.34). Tamu TIDAK PERNAH
     * memutar lokal: permintaan ditulis ke node `jumpRequests`
     * (pola reaksi), host menerapkannya lewat
     * [hostJumpRequestWatch] dan menghapus child-nya. Host yang
     * memanggil ini langsung melompat (tanpa lewat RTDB).
     * Ketuk lagu yang sedang diputar = no-op jujur.
     */
    fun requestJumpToQueueItem(item: JamQueueItem, onResult: (String) -> Unit) {
        val code = sessionCode
        if (sessionScope == null || code.isEmpty()) {
            onResult("You're not in a Jam.")
            return
        }
        if (item.key.isBlank() || item.videoId.isBlank()) {
            onResult("That song is no longer in the Jam queue.")
            return
        }
        if (isCurrentSessionItem(item, snapshotFlow.value?.state)) {
            onResult("\"${item.title}\" is already playing.")
            return
        }
        if (role == JamRole.HOST) {
            jumpToQueueItem(item, onResult)
            return
        }
        scope.launch {
            val creds = currentCreds
            if (creds == null) {
                onResult("You're not in a Jam.")
                return@launch
            }
            val req = JSONObject()
            req.put("queueKey", item.key)
            req.put("videoId", item.videoId)
            req.put("senderUid", creds.uid)
            req.put("senderName", creds.displayName.ifEmpty { "Someone" })
            req.put("sentAtMs", serverTimestampJson())
            try {
                withToken { c -> rest.postChildItem(code, "jumpRequests", req, c.idToken) }
                onResult("Asked the host to jump to \"${item.title}\".")
            } catch (e: Exception) {
                Log.w(TAG, "Jam jump request failed: ${e.message}")
                onResult(friendlyJamError(e))
            }
        }
    }

    /**
     * Host: terapkan permintaan lompat segar dari snapshot. Satu kunci
     * permintaan hanya diterapkan sekali (dedup + hapus child setelah
     * diterapkan); permintaan basi (> TTL) diabaikan dan dibersihkan.
     * Dijalankan sebagai loop sesi host atas snapshotFlow yang sama
     * dengan hostQueueWatch — tidak ada koneksi/stream kedua.
     */
    private suspend fun hostJumpRequestWatch() {
        snapshotFlow.collect { snap ->
            if (snap == null) return@collect
            val now = clockMs()
            for (req in snap.jumpRequests) {
                val fresh = isJumpRequestFresh(req, now)
                if (!fresh) {
                    // Bersihkan permintaan basi supaya node tidak
                    // tumbuh tanpa batas; gagal hapus = diam (snapshot
                    // berikutnya mencoba lagi, TTL tetap menolaknya).
                    runCatching {
                        withToken { c ->
                            rest.deleteNode("jam/$sessionCode/jumpRequests/${req.key}", c.idToken)
                        }
                    }
                    appliedJumpRequestKeys.add(req.key)
                    continue
                }
                if (req.key in appliedJumpRequestKeys) continue
                appliedJumpRequestKeys.add(req.key)
                val target = resolveJumpTarget(snap.queue, req.queueKey, req.videoId)
                if (target == null) {
                    runCatching {
                        withToken { c ->
                            rest.deleteNode("jam/$sessionCode/jumpRequests/${req.key}", c.idToken)
                        }
                    }
                    continue
                }
                if (isCurrentSessionItem(target, snap.state)) {
                    runCatching {
                        withToken { c ->
                            rest.deleteNode("jam/$sessionCode/jumpRequests/${req.key}", c.idToken)
                        }
                    }
                    continue
                }
                val applied = applyJumpToItem(target)
                // Hapus child apa pun hasilnya — permintaan yang gagal
                // diterapkan (lagu hilang dari antrean) tidak boleh
                // dicoba ulang tanpa batas oleh snapshot berikutnya.
                runCatching {
                    withToken { c ->
                        rest.deleteNode("jam/$sessionCode/jumpRequests/${req.key}", c.idToken)
                    }
                }
                if (!applied) {
                    Log.w(TAG, "Jam jump request ${req.key} could not be applied")
                }
            }
        }
    }

    // ---- Susun ulang Up Next sesi (v1.10.36) -------------------------

    /**
     * Susun ulang lagu-lagu Up Next sesi TANPA menghapus (host + tamu).
     * [newOrder] = daftar upcoming LENGKAP dalam urutan baru (dihitung
     * pemanggil dari daftar upcoming snapshot terbaru). Lagu yang sedang
     * diputar tidak ada di upcoming — tidak bisa dipindah (validasi
     * permutasi menolak kunci asing). TANPA Undo (seret balik = undo).
     *
     * HOST: menulis node `queueOrder` (daftar kunci PENUH: prefix
     * terkonsumsi + upcoming baru) + menyelaraskan antrean lokalnya,
     * supaya advance host mengikuti urutan baru. Tamu mengikuti lewat
     * snapshot berikutnya (jalur stream/polling yang sama — TANPA
     * langkah tunggu baru sebelum tamu melihat urutan baru).
     *
     * TAMU: menulis SATU child ke node baru `reorderRequests` (pola
     * jumpRequests; rules auth-only yang sama, TANPA perubahan
     * rules/skema lain). Tamu TIDAK PERNAH menulis queueOrder atau
     * antrean lokalnya — tidak ada urutan desync lokal.
     */
    fun reorderSessionQueue(newOrder: List<JamQueueItem>, onResult: (String) -> Unit) {
        val code = sessionCode
        if (sessionScope == null || code.isEmpty()) {
            onResult("You're not in a Jam.")
            return
        }
        val snap = snapshotFlow.value
        if (snap == null) {
            onResult("The Jam queue isn't loaded yet.")
            return
        }
        val currentKeys = upcomingJamQueue(snap.queue, snap.state).map { it.key }
        val newKeys = newOrder.map { it.key }
        if (newKeys == currentKeys) {
            onResult("The queue is already in that order.")
            return
        }
        if (!validateReorderKeys(currentKeys, newKeys)) {
            onResult("That queue order is no longer current — try again.")
            return
        }
        if (role == JamRole.HOST) {
            scope.launch {
                val applied = applyReorder(newKeys, snap)
                onResult(if (applied) "Queue order updated." else "Couldn't update the queue order.")
            }
            return
        }
        scope.launch {
            val creds = currentCreds
            if (creds == null) {
                onResult("You're not in a Jam.")
                return@launch
            }
            val req = JSONObject()
            req.put("orderedKeys", JSONArray(newKeys))
            req.put("senderUid", creds.uid)
            req.put("senderName", creds.displayName.ifEmpty { "Someone" })
            req.put("sentAtMs", serverTimestampJson())
            try {
                withToken { c -> rest.postChildItem(code, "reorderRequests", req, c.idToken) }
                onResult("Asked the host to reorder the queue.")
            } catch (e: Exception) {
                Log.w(TAG, "Jam reorder request failed: ${e.message}")
                onResult(friendlyJamError(e))
            }
        }
    }

    /**
     * Terapkan urutan upcoming baru: tulis queueOrder + selaraskan
     * antrean lokal host. Dipakai host untuk reorder-nya sendiri DAN
     * untuk permintaan reorder tamu yang valid. Mengembalikan true bila
     * queueOrder berhasil ditulis.
     */
    private suspend fun applyReorder(newUpcomingKeys: List<String>, snap: JamSnapshot): Boolean {
        val currentUpcomingKeys = upcomingJamQueue(snap.queue, snap.state).map { it.key }
        val fullOrder = buildReorderedQueueOrder(snap.queue, currentUpcomingKeys, newUpcomingKeys)
            ?: return false
        val code = sessionCode
        if (code.isEmpty()) return false
        try {
            withToken { c ->
                val body = JSONObject()
                body.put("queueOrder", JSONArray(fullOrder))
                rest.patchNode("jam/$code", body, c.idToken)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Jam queueOrder write failed: ${e.message}")
            return false
        }
        // Selaraskan antrean lokal host: salinan upcoming yang sudah
        // terpetakan dipindah mengikuti urutan baru; salinan tanpa
        // pemetaan dan lagu yang sedang diputar tidak disentuh.
        // Gagal selaras lokal tidak menggagalkan tulis (snapshot
        // berikutnya tetap benar; append hostQueueWatch mengikuti
        // urutan baru untuk item yang belum di-append).
        runCatching { reorderLocalQueue(newUpcomingKeys) }
        return true
    }

    /**
     * Selaraskan antrean lokal host dengan urutan upcoming sesi yang
     * baru. Hanya salinan yang terpetakan ke push key (via
     * queueKeyByQueueUid) dan berada SESUDAH lagu berjalan yang
     * dipindah; himpunan posisi absolut yang dihuni salinan-salinan
     * itu INVARIAN selama langkah move (from/to selalu di dalam
     * himpunan), jadi tiap langkah menemukan targetnya dari antrean
     * segar. Dijalankan di thread Main (moveQueue menyentuh StateFlow
     * + penyimpanan antrean, seperti pemanggil UI-nya).
     */
    private suspend fun reorderLocalQueue(newUpcomingKeys: List<String>) {
        val queue0 = player.queue.value
        val currentIndex = player.currentIndex.value
        val uidForKey = queueKeyByQueueUid.entries.associate { (uid, key) -> key to uid }
        val targetPositions = mutableListOf<Int>()
        val mappedUids = mutableSetOf<String>()
        for (i in queue0.indices) {
            if (i <= currentIndex) continue
            val key = queueKeyByQueueUid[queue0[i].queueUid] ?: continue
            if (key !in newUpcomingKeys) continue
            targetPositions.add(i)
            mappedUids.add(queue0[i].queueUid)
        }
        if (targetPositions.size < 2) return
        val wantedUidOrder = newUpcomingKeys.mapNotNull { uidForKey[it] }
            .filter { it in mappedUids }
        if (wantedUidOrder.size < 2) return
        withContext(Dispatchers.Main) {
            for ((posIndex, pos) in targetPositions.withIndex()) {
                if (posIndex >= wantedUidOrder.size) break
                val wantUid = wantedUidOrder[posIndex]
                val from = player.queue.value.indexOfFirst { it.queueUid == wantUid }
                if (from < 0 || from == pos) continue
                player.moveQueue(from, pos)
            }
        }
    }

    /**
     * Host: terapkan permintaan reorder segar dari snapshot. Pola yang
     * sama dengan hostJumpRequestWatch: satu kunci permintaan hanya
     * diterapkan sekali (dedup + hapus child setelah diterapkan);
     * permintaan basi (> TTL) atau tidak valid (bukan permutasi
     * upcoming saat ini — snapshot sudah berubah sejak tamu menyeret)
     * diabaikan + dibersihkan. Loop sesi host atas snapshotFlow yang
     * sama — tidak ada koneksi/stream kedua.
     */
    private suspend fun hostReorderRequestWatch() {
        snapshotFlow.collect { snap ->
            if (snap == null) return@collect
            val now = clockMs()
            for (req in snap.reorderRequests) {
                val fresh = isReorderRequestFresh(req, now)
                if (!fresh) {
                    runCatching {
                        withToken { c ->
                            rest.deleteNode("jam/$sessionCode/reorderRequests/${req.key}", c.idToken)
                        }
                    }
                    appliedReorderRequestKeys.add(req.key)
                    continue
                }
                if (req.key in appliedReorderRequestKeys) continue
                appliedReorderRequestKeys.add(req.key)
                val currentUpcoming = upcomingJamQueue(snap.queue, snap.state).map { it.key }
                if (!validateReorderKeys(currentUpcoming, req.orderedKeys)) {
                    runCatching {
                        withToken { c ->
                            rest.deleteNode("jam/$sessionCode/reorderRequests/${req.key}", c.idToken)
                        }
                    }
                    continue
                }
                val applied = applyReorder(req.orderedKeys, snap)
                // Hapus child apa pun hasilnya — permintaan yang gagal
                // diterapkan tidak boleh dicoba ulang tanpa batas oleh
                // snapshot berikutnya.
                runCatching {
                    withToken { c ->
                        rest.deleteNode("jam/$sessionCode/reorderRequests/${req.key}", c.idToken)
                    }
                }
                if (!applied) {
                    Log.w(TAG, "Jam reorder request ${req.key} could not be applied")
                }
            }
        }
    }

    /**
     * Akhir-natural lagu (hook PlayerManager, v1.10.34): HANYA host
     * yang bertindak; tamu tidak pernah autoplay lokal. Dipanggil
     * SEBELUM advance player, supaya keputusan memakai antrean sesi
     * pada momen lagu berakhir: antrean tak kosong = diam total
     * (lagu berikutnya dari antrean yang menang, advance normal);
     * antrean kosong = coba autoplay fallback (lihat
     * [runAutoplayFallback]). Pause manual TIDAK PERNAH memicu ini —
     * hook hanya menyala di onTrackEnded (lagu selesai sendiri).
     */
    private fun onNaturalTrackEnd(endedTrack: Track) {
        if (!sessionActive || role != JamRole.HOST) return
        sessionPlayedVideoIds.add(endedTrack.youtubeVideoId.orEmpty())
        if (autoplayInFlight) return
        scope.launch { runAutoplayFallback(endedTrack) }
    }

    /**
     * Autoplay fallback host (v1.10.34): lagu berakhir natural,
     * antrean sesi kosong. Pilih SATU lagu nyata dari feed Beranda
     * (query + jalur yang sama dengan feed 10.25 — bukan karangan),
     * lewati yang sudah dimainkan sesi ini / masih di antrean,
     * resolve-nya harus berhasil lewat jalur resolve yang sama
     * sebelum di-commit (coba sampai 5 kandidat), lalu masukkan ke
     * antrean sesi (terlihat di Up Next kedua HP) dan putar.
     * Gagal total = toast jujur SEKALI, sesi berhenti seperti dulu
     * (tidak ada error palsu, tidak ada retry tanpa henti).
     * TIDAK menambah langkah tunggu untuk pergantian lagu yang
     * sedang berjalan: resolve hanya terjadi SETELAH lagu berakhir
     * dan antrean kosong — tamu tidak sedang menunggu lagu yang
     * masih berbunyi.
     */
    private suspend fun runAutoplayFallback(endedTrack: Track) {
        if (!sessionActive || role != JamRole.HOST) return
        autoplayInFlight = true
        try {
            val snap = snapshotFlow.value
            // Aturan 3: antrean user SELALU menang — diam bila ada
            // lagu antrean yang masih akan diputar.
            if (upcomingJamQueue(snap?.queue.orEmpty(), snap?.state).isNotEmpty()) return
            // Advance normal sudah berjalan SEBELUM coroutine ini
            // sempat jalan (hook dipanggil sebelum next()): bila
            // player sudah pindah ke lagu lain / sedang memutar
            // (lagu antrean berikutnya, sisa antrean pra-sesi, atau
            // lagu yang ditekan user), autoplay tidak boleh menyalip.
            // Hanya lanjut bila player berhenti di lagu yang berakhir.
            if (!isPlayerStoppedOn(endedTrack)) return
            // Feed nyata, urutan feed dipertahankan: trending dulu,
            // lalu terpopuler (jalur + query yang sama dengan Home).
            val feed = fetchAutoplayFeed()
            if (feed.isEmpty()) {
                showToast(appContext, "Couldn't load a song to keep the Jam going.")
                return
            }
            val queuedIds = snap?.queue.orEmpty().map { it.videoId }.toSet()
            var remaining = JAM_AUTOPLAY_MAX_RESOLVE_TRIES
            var cursor = feed
            while (remaining > 0) {
                val pick = pickAutoplayTrack(
                    cursor,
                    sessionPlayedVideoIds,
                    queuedIds,
                    endedTrack.youtubeVideoId,
                ) ?: break
                // Maju kursor melewati pick ini supaya kandidat
                // berikutnya dicoba bila resolve pick ini gagal.
                cursor = cursor.dropWhile { it.youtubeVideoId != pick.youtubeVideoId }.drop(1)
                remaining--
                val videoId = pick.youtubeVideoId.orEmpty()
                val resolvable = runCatching {
                    withContext(Dispatchers.IO) { autoplayApi.resolveStream(videoId).isSuccess }
                }.getOrDefault(false)
                if (!resolvable) continue
                // Kondisi bisa berubah selama fetch/resolve (user
                // menambah lagu / menekan play): validasi ulang.
                if (!autoplayStillValid(endedTrack)) return
                if (commitAutoplayPick(pick)) return
                // Commit gagal (sesi berakhir di tengah jalan) —
                // jangan coba kandidat lain tanpa sesi.
                return
            }
            showToast(appContext, "Couldn't load a song to keep the Jam going.")
        } finally {
            autoplayInFlight = false
        }
    }

    /** True bila player berhenti (tidak memutar) di lagu yang berakhir — syarat autoplay boleh jalan. */
    private suspend fun isPlayerStoppedOn(endedTrack: Track): Boolean {
        return withContext(Dispatchers.Main) {
            val current = player.currentTrack.value
            current?.youtubeVideoId == endedTrack.youtubeVideoId && !player.isPlaying.value
        }
    }

    /** Validasi ulang sesaat sebelum commit pick (kondisi bisa berubah selama fetch feed). */
    private suspend fun autoplayStillValid(endedTrack: Track): Boolean {
        if (!sessionActive || role != JamRole.HOST) return false
        val snap = snapshotFlow.value
        if (upcomingJamQueue(snap?.queue.orEmpty(), snap?.state).isNotEmpty()) return false
        return isPlayerStoppedOn(endedTrack)
    }

    /** Ambil kandidat feed autoplay (dua query Home, forceKotlin — pola 10.25). */
    private suspend fun fetchAutoplayFeed(): List<Track> {
        return withContext(Dispatchers.IO) {
            val first = runCatching {
                autoplayApi.search(HOME_TRENDING_QUERY, gl = "ID", hl = "id", forceKotlin = true).getOrNull()
            }.getOrNull().orEmpty()
            if (first.isNotEmpty()) return@withContext first
            runCatching {
                autoplayApi.search(HOME_TOP_QUERY, gl = "ID", hl = "id", forceKotlin = true).getOrNull()
            }.getOrNull().orEmpty()
        }
    }

    /**
     * Commit satu pick autoplay: tulis ke antrean sesi (push key
     * server), append ke antrean lokal host dengan pemetaan kunci,
     * putar. Mengembalikan false bila sesi berakhir di tengah jalan.
     */
    private suspend fun commitAutoplayPick(pick: Track): Boolean {
        val code = sessionCode
        if (!sessionActive || code.isEmpty()) return false
        val creds = currentCreds ?: return false
        val videoId = pick.youtubeVideoId.orEmpty()
        if (videoId.isBlank()) return false
        val item = JSONObject()
        item.put("videoId", videoId)
        item.put("title", pick.title)
        item.put("artist", pick.artist)
        item.put("artworkUrl", pick.imageUrl.orEmpty())
        item.put("addedBy", creds.uid)
        item.put("addedByName", creds.displayName.ifEmpty { "Someone" })
        item.put("addedAtMs", serverTimestampJson())
        val key = try {
            withToken { c -> rest.postQueueItem(code, item, c.idToken) }
        } catch (e: Exception) {
            Log.w(TAG, "Jam autoplay queue add failed: ${e.message}")
            return false
        }
        if (!sessionActive) return false
        val played = withContext(Dispatchers.Main) {
            if (key.isNotEmpty()) appendedQueueKeys.add(key)
            val appended = player.addToQueue(pick)
            if (key.isNotEmpty()) queueKeyByQueueUid[appended.queueUid] = key
            val at = player.queue.value.indexOfFirst { it.queueUid == appended.queueUid }
            if (at >= 0) {
                player.playAt(at)
                true
            } else {
                false
            }
        }
        if (played) {
            sessionPlayedVideoIds.add(videoId)
            publishFromPlayer(force = true)
        }
        return played
    }

    /**
     * Kirim reaksi cepat ke sesi (semua peran, v1.10.17). Reaksi
     * BUKAN kontrol playback — tamu yang mengirim reaksi tidak
     * membuka kunci kontrolnya, dan reaksi tidak mengubah state
     * pemutaran/antrean. Ditulis ke node baru `reactions` di bawah
     * sesi yang sama (rules auth-only yang sama, tidak diubah);
     * dibaca semua anggota lewat stream/polling yang sama (tidak
     * ada koneksi kedua). [onError] hanya dipanggil bila gagal
     * (ID tidak dikenal / rate limit / sesi mati / jaringan) —
     * sukses diam saja supaya tidak ada toast untuk tiap reaksi.
     */
    fun sendReaction(reactionId: String, onError: (String) -> Unit) {
        val code = sessionCode
        if (!isValidReactionId(reactionId)) {
            onError("That reaction isn't available.")
            return
        }
        if (sessionScope == null || code.isEmpty()) {
            onError("You're not in a Jam.")
            return
        }
        val now = clockMs()
        if (!canSendReaction(lastReactionSentAtMs, now)) {
            onError("Wait a moment before sending another reaction.")
            return
        }
        scope.launch {
            val creds = currentCreds
            if (creds == null) {
                onError("You're not in a Jam.")
                return@launch
            }
            val item = JSONObject()
            item.put("reactionId", reactionId)
            item.put("senderUid", creds.uid)
            item.put("senderName", creds.displayName.ifEmpty { "Someone" })
            item.put("sentAtMs", serverTimestampJson())
            try {
                withToken { c -> rest.postChildItem(code, "reactions", item, c.idToken) }
                lastReactionSentAtMs = clockMs()
            } catch (e: Exception) {
                Log.w(TAG, "Jam reaction failed: ${e.message}")
                onError(friendlyJamError(e))
            }
        }
    }

    // ---- Vote antrean + chat + kontrol bersama (v1.10.42) ---------------

    /**
     * Toggle vote untuk satu item antrean (semua peran, termasuk
     * host). Satu vote per anggota per item: sudah vote -> vote
     * dihapus; belum -> vote ditambah. Tulisnya langsung ke node
     * `votes/{queueKey}/{uid}` (PATCH tambah / DELETE hapus) —
     * tanpa read-modify-write dan tanpa perantara host, jadi dua
     * HP yang vote bersamaan tidak saling menimpa. Urutan tampil
     * + lagu berikutnya ditentukan dari hitungan ini (lihat
     * [sortUpcomingByVotes] + [hostVoteOrderWatch]); lagu yang
     * sedang diputar tidak bisa di-vote (tidak ada di upcoming).
     */
    fun toggleVote(item: JamQueueItem, onResult: (String) -> Unit) {
        val code = sessionCode
        if (sessionScope == null || code.isEmpty()) {
            onResult("You're not in a Jam.")
            return
        }
        if (item.key.isBlank()) {
            onResult("That song is no longer in the Jam queue.")
            return
        }
        val snap = snapshotFlow.value
        val votedByMe = currentCreds?.uid?.takeIf { it.isNotEmpty() }?.let { uid ->
            snap?.voteMap?.get(item.key)?.contains(uid)
        } == true
        scope.launch {
            val creds = currentCreds
            if (creds == null) {
                onResult("You're not in a Jam.")
                return@launch
            }
            try {
                withToken { c ->
                    if (toggleVoteAction(votedByMe) == VoteToggle.REMOVE) {
                        rest.deleteNode("jam/$code/votes/${item.key}/${creds.uid}", c.idToken)
                    } else {
                        val body = JSONObject().put(creds.uid, true)
                        rest.patchNode("jam/$code/votes/${item.key}", body, c.idToken)
                    }
                }
                onResult("")
            } catch (e: Exception) {
                Log.w(TAG, "Jam vote failed: ${e.message}")
                onResult(friendlyJamError(e))
            }
        }
    }

    /**
     * Kirim satu pesan chat ke sesi (semua peran). Teks verbatim
     * (trim + cap 500, tanpa sensor — sesi berkode privat), rate
     * limit lokal 1 pesan/detik. Ditulis ke node `messages`
     * (POST push key, pola reaksi); pengirim membuang pesan
     * terlama bila node melewati ambang prune (histeresis, lihat
     * [chatKeysToPrune]). [onError] hanya dipanggil bila gagal —
     * sukses diam saja (pesan tampil lewat snapshot).
     */
    fun sendChatMessage(rawText: String, onError: (String) -> Unit) {
        val code = sessionCode
        val text = sanitizeChatText(rawText)
        if (text == null) {
            onError("Write something first.")
            return
        }
        if (sessionScope == null || code.isEmpty()) {
            onError("You're not in a Jam.")
            return
        }
        val now = clockMs()
        if (!canSendChat(lastChatSentAtMs, now)) {
            onError("Wait a moment before sending another message.")
            return
        }
        scope.launch {
            val creds = currentCreds
            if (creds == null) {
                onError("You're not in a Jam.")
                return@launch
            }
            val item = JSONObject()
            item.put("senderUid", creds.uid)
            item.put("senderName", creds.displayName.ifEmpty { "Someone" })
            item.put("text", text)
            item.put("sentAtMs", serverTimestampJson())
            try {
                withToken { c -> rest.postChildItem(code, "messages", item, c.idToken) }
                lastChatSentAtMs = clockMs()
                // Prune oportunistik oleh pengirim: hanya bila node
                // melewati ambang — penghapusan per kunci, bounded.
                val pruneKeys = chatKeysToPrune(snapshotFlow.value?.messages.orEmpty())
                for (key in pruneKeys) {
                    runCatching {
                        withToken { c -> rest.deleteNode("jam/$code/messages/$key", c.idToken) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Jam chat failed: ${e.message}")
                onError(friendlyJamError(e))
            }
        }
    }

    /**
     * Ubah toggle kontrol bersama — HOST SAJA. Ditulis sebagai
     * field `sharedControl` di root sesi (PATCH); tamu membaca
     * lewat snapshot yang sama (tidak ada koneksi kedua). Mematikan
     * toggle tidak membatalkan lagu yang sedang diputar — hanya
     * permintaan kontrol tamu yang belum diterapkan yang diabaikan
     * host (lihat [shouldApplyControlRequest]).
     */
    fun setSharedControl(enabled: Boolean, onResult: (String) -> Unit) {
        val code = sessionCode
        if (sessionScope == null || code.isEmpty()) {
            onResult("You're not in a Jam.")
            return
        }
        if (role != JamRole.HOST) {
            onResult("Only the host can change who controls playback.")
            return
        }
        scope.launch {
            try {
                withToken { c ->
                    val body = JSONObject().put("sharedControl", enabled)
                    rest.patchNode("jam/$code", body, c.idToken)
                }
                onResult("")
            } catch (e: Exception) {
                Log.w(TAG, "Jam shared-control write failed: ${e.message}")
                onResult(friendlyJamError(e))
            }
        }
    }

    /**
     * Minta kontrol putar — jalur TAMU saat shared control menyala
     * (v1.10.42). Tamu TIDAK PERNAH memutar lokal: permintaan
     * ditulis ke node `controlRequests` (pola jumpRequests), host
     * menerapkannya lewat [hostControlRequestWatch] dan menghapus
     * child-nya. Host yang memanggil ini tidak pernah sampai ke
     * sini (UI host memakai aksi lokal langsung); tamu di sesi
     * sharedControl MATI ditolak di sini (UI juga mengunci tombol
     * lewat guestLocked — penolakan ini jaring pengaman kedua).
     * [positionMs] hanya dipakai SEEK.
     */
    fun requestPlaybackControl(
        action: JamControlAction,
        positionMs: Long = 0L,
        onResult: (String) -> Unit,
    ) {
        val code = sessionCode
        if (sessionScope == null || code.isEmpty()) {
            onResult("You're not in a Jam.")
            return
        }
        if (role != JamRole.GUEST) {
            onResult("You're not a guest in this Jam.")
            return
        }
        if (snapshotFlow.value?.sharedControl != true) {
            onResult("Only the host can control playback during a Jam.")
            return
        }
        scope.launch {
            val creds = currentCreds
            if (creds == null) {
                onResult("You're not in a Jam.")
                return@launch
            }
            val req = JSONObject()
            req.put("action", action.wireName)
            req.put("positionMs", positionMs)
            req.put("senderUid", creds.uid)
            req.put("senderName", creds.displayName.ifEmpty { "Someone" })
            req.put("sentAtMs", serverTimestampJson())
            try {
                withToken { c -> rest.postChildItem(code, "controlRequests", req, c.idToken) }
                // Sukses diam saja (seperti reaksi): hasilnya
                // terlihat saat host menerapkannya lewat snapshot.
                onResult("")
            } catch (e: Exception) {
                Log.w(TAG, "Jam control request failed: ${e.message}")
                onResult(friendlyJamError(e))
            }
        }
    }

    /**
     * Host: terapkan permintaan kontrol segar dari snapshot. Pola
     * yang sama dengan hostJumpRequestWatch: satu kunci permintaan
     * hanya diterapkan sekali (dedup + hapus child setelah
     * diterapkan); permintaan basi (> TTL) atau yang tiba saat
     * shared control MATI diabaikan + dibersihkan. Penerapan lewat
     * thread Main (player) + publish paksa, supaya tamu mengikuti
     * lewat jalur sinkron yang sudah ada.
     */
    private suspend fun hostControlRequestWatch() {
        snapshotFlow.collect { snap ->
            if (snap == null) return@collect
            val now = clockMs()
            for (req in snap.controlRequests) {
                val fresh = isControlRequestFresh(req, now)
                val allowed = shouldApplyControlRequest(snap.sharedControl, req, now)
                if (!fresh || !allowed) {
                    runCatching {
                        withToken { c ->
                            rest.deleteNode("jam/$sessionCode/controlRequests/${req.key}", c.idToken)
                        }
                    }
                    appliedControlRequestKeys.add(req.key)
                    continue
                }
                if (req.key in appliedControlRequestKeys) continue
                appliedControlRequestKeys.add(req.key)
                val applied = applyControlRequest(req)
                runCatching {
                    withToken { c ->
                        rest.deleteNode("jam/$sessionCode/controlRequests/${req.key}", c.idToken)
                    }
                }
                if (!applied) {
                    Log.w(TAG, "Jam control request ${req.key} could not be applied")
                }
            }
        }
    }

    /** Terapkan satu permintaan kontrol di player host (thread Main). */
    private suspend fun applyControlRequest(req: JamControlRequest): Boolean {
        if (!sessionActive || role != JamRole.HOST) return false
        withContext(Dispatchers.Main) {
            when (req.action) {
                JamControlAction.TOGGLE -> player.togglePlay()
                JamControlAction.NEXT -> player.next()
                JamControlAction.PREVIOUS -> player.previous()
                JamControlAction.SEEK -> player.seekTo(req.positionMs.coerceAtLeast(0L))
            }
        }
        publishFromPlayer(force = true)
        return true
    }

    /**
     * Host: selaraskan antrean lokal dengan urutan tampil efektif
     * (vote terbanyak dulu, tiebreak queueOrder) setiap snapshot
     * berubah. Tanpa ini, advance natural player mengikuti urutan
     * queueOrder lama dan lagu vote-tertinggi tidak muter duluan.
     * Memakai [reorderLocalQueue] yang sama dengan reorder (10.36)
     * — hanya salinan terpetakan sesudah lagu berjalan yang
     * dipindah; lagu berjalan dipin. Vote basi untuk kunci yang
     * tidak lagi upcoming ikut dibuang oportunistik (bounded:
     * hanya kunci yang ada di voteMap).
     */
    private suspend fun hostVoteOrderWatch() {
        snapshotFlow.collect { snap ->
            if (snap == null) return@collect
            val upcoming = upcomingJamQueue(snap.queue, snap.state)
            val effectiveKeys = sortUpcomingByVotes(upcoming, voteCounts(snap.voteMap)).map { it.key }
            runCatching { reorderLocalQueue(effectiveKeys) }
            val stale = staleVoteKeys(
                snap.voteMap,
                upcoming.map { it.key }.toSet(),
                snap.state?.queueKey.orEmpty(),
            )
            for (key in stale) {
                runCatching {
                    withToken { c -> rest.deleteNode("jam/$sessionCode/votes/$key", c.idToken) }
                }
            }
        }
    }

    // ---- Siklus sesi ------------------------------------------------------

    private fun startSession(code: String, newRole: JamRole, creds: JamCredentials, seedJson: JSONObject? = null) {
        teardown()
        // v1.0.3: radio TIDAK PERNAH jalan selama Jam — antrean milik
        // sesi. Radio yang sedang aktif dibersihkan di SATU-SATUNYA
        // titik aktivasi sesi ini (host + tamu sama-sama lewat sini).
        player.clearRadio()
        // v1.10.9 (AKAR MASALAH — terbukti dari kode + reproduksi
        // harness): teardown() di atas mengosongkan currentCreds,
        // dan startSession TIDAK PERNAH mengisinya kembali. Akibatnya
        // withToken selalu melempar SessionGone di streamLoop
        // (toast "Session expired" palsu + teardown dalam hitungan
        // milidetik), tambah lagu selalu "You're not in a Jam", dan
        // dialog kembali ke menu — persis laporan HP v1.10.8. Pulihkan
        // kredensial sesi DI SINI, segera setelah teardown.
        currentCreds = creds
        sessionCode = code
        role = newRole
        endedHandled = false
        hadContent = false
        appendedQueueKeys.clear()
        queueKeyByQueueUid.clear()
        consumedThroughKeyLocal = ""
        // v1.10.34: reset status lompat + autoplay untuk sesi baru
        // (riwayat putar di-seed dari snapshot di bawah, setelah
        // nodeJson diisi — supaya host yang restart tidak mengulang
        // lagu yang sudah dimainkan sesi ini).
        sessionPlayedVideoIds.clear()
        appliedJumpRequestKeys.clear()
        appliedReorderRequestKeys.clear()
        appliedControlRequestKeys.clear()
        lastChatSentAtMs = 0L
        autoplayInFlight = false
        lastPublished = null
        lastSnapshotAtMs = 0L
        // v1.10.12: reset baseline terima + dedup audible-start +
        // status publish untuk sesi baru.
        lastStateVersionKey = ""
        stateReceivedAtMs = 0L
        audiblePublishedKey = null
        publishDegraded = false
        // v1.10.15: reset status prefetch tamu untuk sesi baru.
        prefetchAttemptAtMs.clear()
        // v1.10.17: reset rate limit reaksi untuk sesi baru.
        lastReactionSentAtMs = 0L
        // v1.10.18: reset watchdog follow-health untuk sesi baru.
        followVersionKey = ""
        followAttempts = 0
        followTick = 0L
        followLastAttemptTick = -100L
        followLastPosMs = 0L
        followLastPosAtMs = 0L
        followPrevTickPosMs = 0L
        followBufferingSinceMs = 0L
        followEndedSinceMs = 0L
        followHealthyTicks = 0
        followProblemInternal = null
        clockOffsetMs = runCatching { rest.getServerTimeOffset(creds.idToken) }.getOrDefault(0L)
        val seed = seedJson ?: runCatching {
            rest.getSessionJson(code, creds.idToken)
        }.getOrNull()
        nodeJson = seed ?: JSONObject()
        val snap = parseJamSnapshot(nodeJson)
        snapshotFlow.value = snap
        if (snap != null) {
            hadContent = true
            lastSnapshotAtMs = clockMs()
            // v1.10.34: seed riwayat putar sesi dari snapshot — lagu
            // yang sedang diputar + prefix antrean yang sudah
            // terkonsumsi (<= consumedThroughKey) + lagu state.
            // Host yang restart tidak mengulang lagu-lagu itu lewat
            // autoplay.
            snap.state?.videoId?.takeIf { it.isNotEmpty() }?.let { sessionPlayedVideoIds.add(it) }
            val playedMarker = snap.state?.consumedThroughKey.orEmpty()
            if (playedMarker.isNotEmpty()) {
                for (item in snap.queue) {
                    if (item.key <= playedMarker) sessionPlayedVideoIds.add(item.videoId)
                }
            }
            snap.state?.queueKey?.takeIf { it.isNotEmpty() }?.let { qk ->
                snap.queue.firstOrNull { it.key == qk }?.let { sessionPlayedVideoIds.add(it.videoId) }
            }
            // v1.10.18: host yang (kembali) bergabung mewarisi
            // high-water mark konsumsi dari state yang sudah ada —
            // lagu yang sudah diputar sebelum host restart tidak
            // boleh bangkit lagi di Up Next.
            val seedMarker = snap.state?.consumedThroughKey.orEmpty()
            if (newRole == JamRole.HOST && seedMarker.isNotEmpty()) {
                consumedThroughKeyLocal = seedMarker
            }
            // v1.10.12: seed adalah versi state pertama yang
            // diterima — baseline posisi tamu mulai di sini.
            noteStateReceipt(snap)
        }
        connection = JamConnection.CONNECTING
        transport = ""
        _guestLocked.value = (newRole == JamRole.GUEST)
        publishUi()
        val sScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        sessionScope = sScope
        sessionActive = true
        // v1.10.12: hook audible-start hanya untuk host — publish
        // ulang saat lagu benar-benar berbunyi (lihat
        // onAudibleStart). Tamu tidak menulis state.
        if (newRole == JamRole.HOST) {
            player.onAudibleStart = { trackId, generation ->
                onAudibleStart(trackId, generation)
            }
            // v1.10.34: hook akhir-natural hanya untuk host —
            // autoplay fallback. Tamu tidak pernah autoplay lokal.
            player.onTrackEndedNatural = { endedTrack ->
                onNaturalTrackEnd(endedTrack)
            }
        } else {
            player.onTrackEndedNatural = null
        }
        // v1.10.8: publikasikan active=true SEKARANG (bukan hanya
        // lewat publishUi() di atas yang jalan sebelum sessionScope
        // diisi) — UI/dialog tidak boleh mengira sesi mati di
        // detik-detik pertama, dan teardown yang cepat tidak boleh
        // lolos dari pantauan dialog.
        publishUi()
        sScope.launch { streamLoop() }
        sScope.launch { heartbeatLoop() }
        sScope.launch { presenceTicker() }
        if (newRole == JamRole.HOST) {
            sScope.launch { hostPublishLoop() }
            sScope.launch { hostSeekWatch() }
            sScope.launch { hostQueueWatch() }
            // v1.10.34: host menerapkan permintaan lompat tamu.
            sScope.launch { hostJumpRequestWatch() }
            // v1.10.36: host menerapkan permintaan reorder tamu.
            sScope.launch { hostReorderRequestWatch() }
            // v1.10.42: host menerapkan permintaan kontrol tamu
            // (shared control) + menyelaraskan antrean lokal dengan
            // urutan vote.
            sScope.launch { hostControlRequestWatch() }
            sScope.launch { hostVoteOrderWatch() }
        } else {
            sScope.launch { guestApplyLoop() }
            sScope.launch { guestConvergenceTicker() }
            sScope.launch { guestDriftTicker() }
            sScope.launch { guestHoldPauseWatch() }
            // v1.10.18: watchdog kesehatan follow (lihat
            // guestFollowWatch) — tamu saja.
            sScope.launch { guestFollowWatch() }
            // v1.10.35: jaring pengaman polling — tamu saja.
            sScope.launch { guestSafetyPoll() }
        }
    }

    /** Hentikan semua loop sesi + reset UI. Playback lokal TIDAK disentuh. */
    private fun teardown() {
        sessionActive = false
        // v1.10.12: lepas hook audible-start — player tidak boleh
        // mempublish untuk sesi yang sudah berakhir.
        player.onAudibleStart = null
        // v1.10.34: lepas hook akhir-natural — tidak ada autoplay
        // setelah sesi berakhir.
        player.onTrackEndedNatural = null
        sessionPlayedVideoIds.clear()
        appliedJumpRequestKeys.clear()
        appliedReorderRequestKeys.clear()
        appliedControlRequestKeys.clear()
        lastChatSentAtMs = 0L
        autoplayInFlight = false
        // v1.10.15: lepas rate koreksi drift — pemutaran lokal setelah
        // sesi HARUS 1.0×. Dipanggil dari thread apa pun (IO/ Main),
        // sementara setPlaybackSpeed milik thread Main — post lewat
        // scope + Dispatchers.Main (pola yang sama dengan toast
        // main-thread). startTrack juga me-reset rate, jadi ini
        // jaring pengaman untuk lagu yang sedang berjalan.
        scope.launch {
            withContext(Dispatchers.Main) {
                runCatching { player.setPlaybackRate(1.0f) }
            }
        }
        prefetchAttemptAtMs.clear()
        lastReactionSentAtMs = 0L
        queueKeyByQueueUid.clear()
        consumedThroughKeyLocal = ""
        followVersionKey = ""
        followAttempts = 0
        followTick = 0L
        followLastAttemptTick = -100L
        followLastPosMs = 0L
        followLastPosAtMs = 0L
        followPrevTickPosMs = 0L
        followBufferingSinceMs = 0L
        followEndedSinceMs = 0L
        followHealthyTicks = 0
        followProblemInternal = null
        rest.cancelStream()
        sessionScope?.cancel()
        sessionScope = null
        sessionCode = ""
        currentCreds = null
        nodeJson = JSONObject()
        snapshotFlow.value = null
        connection = JamConnection.IDLE
        transport = ""
        publishDegraded = false
        _guestLocked.value = false
        _uiState.value = JamUiState()
    }

    /**
     * Hook audible-start dari PlayerManager (v1.10.12): audio host
     * benar-benar mulai berbunyi untuk satu generasi lagu.
     * Publish ulang state dengan force=true — inilah baseline
     * posisi yang benar untuk tamu (bukan stempel saat resolve
     * dimulai). Satu generasi = satu publish; callback berulang
     * untuk generasi yang sama di-dedup.
     */
    private fun onAudibleStart(trackId: String, generation: Int) {
        if (!sessionActive || role != JamRole.HOST) return
        val key = "$trackId@$generation"
        if (key == audiblePublishedKey) return
        audiblePublishedKey = key
        publishFromPlayer(force = true)
    }

    // ---- Jaringan sesi ----------------------------------------------------

    private fun memberJson(creds: JamCredentials): JSONObject {
        val o = JSONObject()
        o.put("name", creds.displayName.ifEmpty { "Someone" })
        o.put("photoUrl", creds.photoUrl)
        o.put("lastSeenMs", serverTimestampJson())
        return o
    }

    /**
     * Jalankan [block] dengan token yang masih berlaku; bila server
     * menjawab 401, perbarui token sekali lalu ulangi. Gagal lagi
     * atau tidak ada sesi → SessionGone. Gagal TRANSIEN saat
     * refresh → JamTransient (pemanggil backoff, sesi tetap hidup).
     */
    private suspend fun <T> withToken(block: (JamCredentials) -> T): T {
        var creds = currentCreds ?: throw SessionGone()
        try {
            return block(creds)
        } catch (e: JamRest.JamRestException) {
            if (e.code != 401) throw e
            creds = try {
                sync.jamCredentialsOrThrow(forceRefresh = true)
            } catch (e: JamAuthInvalid) {
                throw SessionGone()
            } catch (e: JamAuthTransient) {
                throw JamTransient()
            }
            currentCreds = creds
            return block(creds)
        }
    }

    /**
     * Catat waktu terima versi state yang baru tiba (v1.10.12).
     * Hanya versi BARU yang memajukan baseline — snapshot yang
     * sama (mis. event patch anggota yang membawa ulang state
     * lama) tidak boleh me-reset elapsed tamu ke nol.
     */
    private fun noteStateReceipt(snap: JamSnapshot?) {
        val st = snap?.state ?: return
        if (st.versionKey != lastStateVersionKey) {
            lastStateVersionKey = st.versionKey
            stateReceivedAtMs = clockMs()
        }
    }

    private fun onSseEvent(event: JamRest.SseEvent) {
        if (event.kind != "put" && event.kind != "patch") return
        nodeJson = mergeSseEvent(nodeJson, event.path, event.data, event.kind == "patch")
        val snap = parseJamSnapshot(nodeJson)
        if (snap != null) {
            hadContent = true
            lastSnapshotAtMs = clockMs()
            noteStateReceipt(snap)
            snapshotFlow.value = snap
            // v1.10.17: segarkan UI untuk SETIAP snapshot baru, bukan
            // hanya saat status koneksi berubah — reaksi + anggota
            // harus tampil secepatnya untuk host maupun tamu.
            // publishUi() idempotent (StateFlow tidak me-emit ulang
            // nilai yang sama), jadi panggilan tiap event murah.
            publishUi()
        }
        if (connection != JamConnection.LIVE) {
            connection = JamConnection.LIVE
            transport = "SSE"
            publishUi()
        }
    }

    private fun onPolledSnapshot(json: JSONObject?) {
        if (json == null) {
            // Node hilang = sesi dihapus/diakhiri.
            if (hadContent) onNodeGone()
            return
        }
        nodeJson = json
        val snap = parseJamSnapshot(json)
        if (snap != null) {
            hadContent = true
            lastSnapshotAtMs = clockMs()
            noteStateReceipt(snap)
            snapshotFlow.value = snap
            // v1.10.17: alasan yang sama dengan onSseEvent — reaksi
            // yang tiba lewat polling harus tampil tanpa menunggu
            // ticker presence 5 detik.
            publishUi()
        }
    }

    private fun onNodeGone() {
        if (role == JamRole.GUEST && !endedHandled) {
            endedHandled = true
            showToast(appContext, "The Jam session has ended.")
            teardown()
        }
    }

    /** Loop stream SSE + reconnect + fallback polling. Berjalan sampai scope sesi batal. */
    private suspend fun streamLoop() {
        var sseFailures = 0
        while (sessionActive) {
            var streamOk = false
            try {
                connection = JamConnection.CONNECTING
                publishUi()
                withToken { creds ->
                    rest.streamSession(
                        sessionCode,
                        creds.idToken,
                        onConnected = {
                            connection = JamConnection.LIVE
                            transport = "SSE"
                            publishUi()
                        },
                        onEvent = ::onSseEvent,
                    )
                }
                // Stream berakhir bersih (server menutup) — sambung lagi.
                streamOk = true
                sseFailures = 0
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SessionGone) {
                // v1.10.9: bila sesi sudah diakhiri sengaja
                // (teardown berjalan — mis. user menekan End/Leave),
                // JANGAN tampilkan toast session-expired palsu.
                if (!sessionActive) return
                Log.w(TAG, "Jam session expired")
                showToast(appContext, "Session expired, sign in again.")
                teardown()
                return
            } catch (e: JamRest.SseStalledException) {
                // v1.10.12: SSE stall (tidak ada byte dalam
                // readTimeout) — JANGAN hitung sebagai satu failure
                // biasa + reconnect SSE (itu mengulang stall).
                // Langsung fallback polling; setelah 30 detik coba
                // SSE lagi dari loop.
                Log.w(TAG, "Jam SSE stalled — falling back to polling")
                pollingFallback()
                sseFailures = 0
                continue
            } catch (e: Exception) {
                Log.w(TAG, "Jam stream failed: ${e.message}")
            }
            if (streamOk) {
                delay(1000L)
                continue
            }
            sseFailures++
            if (sseFailures >= 3) {
                // Fallback polling 1,5 detik selama 30 detik, lalu coba SSE lagi.
                pollingFallback()
                sseFailures = 0
            } else {
                connection = JamConnection.RECONNECTING
                publishUi()
                // v1.10.35: backoff mulai 500ms (dari 1 detik) —
                // reconnect pertama yang gagal tidak boleh membuat
                // tamu tuli 2 detik tepat saat lagu berganti; eskalasi
                // ke polling tetap setelah 3 kegagalan.
                delay((500L shl sseFailures.coerceAtMost(3)).coerceAtMost(8000L))
            }
        }
    }

    /** Polling GET 1,5 detik selama 30 detik (fallback saat SSE gagal/stall). */
    private suspend fun pollingFallback() {
        connection = JamConnection.RECONNECTING
        transport = "Polling"
        publishUi()
        val pollUntil = clockMs() + 30_000L
        while (sessionActive && clockMs() < pollUntil) {
            try {
                val json = withToken { c -> rest.getSessionJson(sessionCode, c.idToken) }
                onPolledSnapshot(json)
                if (connection != JamConnection.LIVE) {
                    connection = JamConnection.LIVE
                    publishUi()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SessionGone) {
                // v1.10.9: pengaman yang sama seperti cabang
                // SSE di atas — teardown yang disengaja bukan
                // sesi kedaluwarsa.
                if (!sessionActive) return
                showToast(appContext, "Session expired, sign in again.")
                teardown()
                return
            } catch (e: Exception) {
                connection = JamConnection.RECONNECTING
                publishUi()
            }
            delay(1500L)
        }
    }

    /**
     * Jaring pengaman polling tamu (v1.10.35, TAMU saja): SSE yang
     * half-open tidak terdeteksi sampai readTimeout 45 detik, dan
     * rantai reconnect membuat tamu tuli beberapa detik tepat saat
     * lagu berganti. Loop ringan ini melakukan GET sesi tiap
     * [safetyPollIntervalMs] HANYA bila tidak ada snapshot segar yang
     * tiba lewat jalur mana pun (SSE/polling) — saat SSE sehat dan
     * ada aktivitas (heartbeat anggota ≤ 15 detik, publish host),
     * hampir selalu dilewati; saat SSE mati, tamu belajar lagu baru
     * dalam ~1 interval, bukan 45 detik. GET memakai
     * [onPolledSnapshot] yang sama dengan pollingFallback (ganti node
     * utuh + dedupe versionKey), dan TIDAK menyentuh status koneksi/
     * transport — itu milik streamLoop (loop ini hanya menjaga DATA
     * tetap segar, supaya status LIVE tidak berkedip di sesi sepi).
     * Biaya: satu GET kecil tiap interval saat sesi sepi; node sesi
     * kecil dan radio sudah sibuk streaming audio.
     */
    private suspend fun guestSafetyPoll() {
        while (sessionActive) {
            delay(safetyPollIntervalMs)
            if (!sessionActive || role != JamRole.GUEST) continue
            // Snapshot segar = jalur utama hidup; jangan menambah traffic.
            if (clockMs() - lastSnapshotAtMs < safetyPollIntervalMs) continue
            runCatching {
                val json = withToken { c -> rest.getSessionJson(sessionCode, c.idToken) }
                onPolledSnapshot(json)
            }
        }
    }

    /** Heartbeat presence tiap 15 detik. */
    private suspend fun heartbeatLoop() {
        while (sessionActive) {
            // v1.10.42: 10 detik (dari 15) — orbit menampilkan anggota
            // yang heartbeat-nya < 30 detik, jadi cadence harus jauh
            // di bawah ambang itu supaya anggota yang masih hidup
            // tidak berkedip keluar-masuk orbit.
            delay(10_000L)
            runCatching {
                withToken { c ->
                    rest.patchNode(
                        "jam/$sessionCode/members/${c.uid}",
                        memberJson(c),
                        c.idToken,
                    )
                }
            }
        }
    }

    /** Perbarui daftar anggota segar + status host-away tiap 5 detik (tanpa event pun basi terlihat). */
    private suspend fun presenceTicker() {
        while (sessionActive) {
            delay(5_000L)
            publishUi()
        }
    }

    // ---- Host: terbitkan state ---------------------------------------------

    /**
     * Terbitkan state dari player lokal. Dedupe: jangan menulis ulang state yang nyaris sama.
     *
     * v1.10.12: publish yang mengubah lagu/playing ("flip") di-RETRY
     * terbatas (2×, backoff 300ms) — sebelumnya fire-and-forget
     * tanpa retry: satu publish flip yang hilang membuat tamu tidak
     * pernah tahu flip itu. Publish posisi murni (lagu + playing
     * sama) tetap satu percobaan. Bila flip tetap gagal setelah
     * retry, status publish ditandai degraded (UI: RECONNECTING)
     * sampai ada publish berikutnya yang berhasil.
     */
    private fun publishFromPlayer(force: Boolean) {
        val sScope = sessionScope ?: return
        val track = player.currentTrack.value ?: return
        // v1.10.34: catat lagu yang dipublikasikan host sebagai
        // "sudah dimainkan sesi ini" (autoplay tidak mengulanginya).
        track.youtubeVideoId?.takeIf { it.isNotEmpty() }?.let { sessionPlayedVideoIds.add(it) }
        val videoId = track.youtubeVideoId.orEmpty()
        val playing = player.isPlaying.value
        val position = player.positionMs.value
        val now = clockMs()
        val last = lastPublished
        val isNewTrack = last?.videoId != videoId
        val isFlip = last == null || isNewTrack || last.playing != playing
        if (!force && !isNewTrack && last != null &&
            last.playing == playing &&
            abs(last.positionMs - position) < 1500L &&
            now - last.atMs < 3000L
        ) {
            return
        }
        lastPublished = Published(videoId, playing, position, now)
        // v1.10.18: marker konsumsi per push key. queueKey = kunci
        // antrean dari salinan yang sedang diputar host ("" bila di
        // luar antrean); consumedThrough = high-water mark (tidak
        // pernah mundur) — digabung dengan marker dari snapshot
        // terakhir supaya host yang restart tidak kehilangan jejak.
        val snapQueue = snapshotFlow.value?.queue.orEmpty()
        val currentKey = queueKeyByQueueUid[track.queueUid].orEmpty()
        if (currentKey.isNotEmpty()) {
            consumedThroughKeyLocal =
                advanceConsumedThrough(snapQueue, consumedThroughKeyLocal, currentKey)
        }
        val snapshotMarker = snapshotFlow.value?.state?.consumedThroughKey.orEmpty()
        if (snapshotMarker.isNotEmpty()) {
            consumedThroughKeyLocal =
                advanceConsumedThrough(snapQueue, consumedThroughKeyLocal, snapshotMarker)
        }
        val state = JamState(
            videoId = videoId,
            title = track.title,
            artist = track.artist,
            artworkUrl = track.imageUrl.orEmpty(),
            durationMs = player.durationMs.value.takeIf { it > 0 } ?: track.durationMs,
            positionMs = position,
            playing = playing,
            updatedAtMs = 0L, // diisi server (.sv)
            queueKey = currentKey,
            consumedThroughKey = consumedThroughKeyLocal,
        )
        sScope.launch {
            var attempt = 0
            while (true) {
                try {
                    withToken { c ->
                        rest.patchNode(
                            "jam/$sessionCode/state",
                            jamStateToJson(state, includeServerTimestamp = true),
                            c.idToken,
                        )
                    }
                    // Berhasil — pulihkan status degraded bila ada.
                    if (publishDegraded) {
                        publishDegraded = false
                        publishUi()
                    }
                    return@launch
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: SessionGone) {
                    // Kredensial sesi habis — retry tidak akan
                    // menolong; streamLoop/teardown yang menangani.
                    return@launch
                } catch (e: Exception) {
                    attempt++
                    if (!isFlip || attempt > 2 || !sessionActive) {
                        Log.w(TAG, "Jam publish failed after $attempt attempt(s): ${e.message}")
                        // State ini TIDAK sampai ke server — jangan
                        // biarkan catatan optimistisnya men-dedupe
                        // pengiriman ulang berikutnya. Hanya hapus
                        // bila catatan masih milik publish ini
                        // (publish yang lebih baru tidak boleh
                        // ikut terhapus).
                        val mine = Published(videoId, playing, position, now)
                        if (lastPublished == mine) lastPublished = null
                        if (isFlip && sessionActive) {
                            publishDegraded = true
                            publishUi()
                        }
                        return@launch
                    }
                    delay(300L)
                }
            }
        }
    }

    /** Ikuti perubahan lagu + play/pause host. */
    private suspend fun hostPublishLoop() {
        combine(player.currentTrack, player.isPlaying) { track, _ -> track }
            .collect {
                publishFromPlayer(force = false)
            }
    }

    /**
     * Deteksi seek host dari lompatan posisi + publish posisi BERKALA
     * (polling 250ms, pola yang sama dengan video sync).
     *
     * v1.10.15: selagi host MEMUTAR, state posisi dikirim ulang tiap
     * [positionPublishIntervalMs] (~10 detik) lewat jalur
     * [publishFromPlayer] yang sama — jaring pengaman bila satu
     * publish flip hilang di jalan (tamu tidak lagi menggantung pada
     * state yang semakin basi). Posisi murni = 1× tanpa retry
     * (aturan yang sama dengan publish posisi v1.10.12: gagal =
     * diam, publish berikutnya yang memperbaiki). Berhenti saat
     * pause (flip pause yang mengurusnya), saat lagu lokal
     * (videoId kosong), dan saat sesi berakhir. Jam 10 detik
     * dimulai ulang tiap resume, supaya resume tidak langsung
     * menggandakan publish flip-nya.
     */
    private suspend fun hostSeekWatch() {
        var lastPos = -1L
        var lastAt = 0L
        var lastPeriodicAtMs = 0L
        var wasPlaying = false
        while (sessionActive) {
            delay(250L)
            val pos = player.positionMs.value
            val now = clockMs()
            val playing = player.isPlaying.value
            if (playing) {
                if (!wasPlaying) {
                    wasPlaying = true
                    lastPeriodicAtMs = now
                } else if (now - lastPeriodicAtMs >= positionPublishIntervalMs) {
                    val videoId = player.currentTrack.value?.youtubeVideoId.orEmpty()
                    if (videoId.isNotEmpty()) {
                        publishFromPlayer(force = true)
                        lastPeriodicAtMs = now
                    }
                }
            } else {
                wasPlaying = false
            }
            if (lastPos >= 0 && playing) {
                val expected = lastPos + (now - lastAt)
                if (abs(pos - expected) > 2000L) {
                    publishFromPlayer(force = true)
                }
            }
            lastPos = pos
            lastAt = now
        }
    }

    /** Host: lagu baru di antrean sesi → tambahkan ke antrean lokal (sekali per push key). */
    private suspend fun hostQueueWatch() {
        snapshotFlow.collect { snap ->
            if (snap == null) return@collect
            for (item in snap.queue) {
                if (item.key in appendedQueueKeys) continue
                appendedQueueKeys.add(item.key)
                val track = item.toTrack()
                // v1.10.18: addToQueue mengembalikan salinan yang
                // masuk antrean lokal (queueUid segar) — petakan
                // uid salinan itu ke push key, supaya publish tahu
                // kunci antrean lagu yang sedang diputar host.
                val appended = withContext(Dispatchers.Main) { player.addToQueue(track) }
                queueKeyByQueueUid[appended.queueUid] = item.key
            }
        }
    }

    // ---- Tamu: terapkan state ----------------------------------------------

    /**
     * Prefetch stream lagu BERIKUTNYA di antrean sesi (v1.10.15,
     * TAMU saja). Host tidak membutuhkannya (host yang memutar
     * duluan); di luar Jam tidak pernah dipanggil.
     *
     * Lagu berikutnya = item pertama [upcomingJamQueue] (sudah
     * membuang lagu yang sedang diputar). Bila stream-nya sudah
     * segar di cache player, atau permintaan untuk lagu yang sama
     * masih dalam cooldown 30 detik (prefetch sedang berjalan),
     * tidak ada tindakan. Gagal prefetch diam-diam di player —
     * resolve normal saat lagu diputar tetap menjadi jaring
     * pengaman. Dipanggil dari loop tamu (bukan thread Main —
     * hanya membaca StateFlow + memanggil prefetch async player).
     */
    private fun maybePrefetchNext() {
        if (!sessionActive || role != JamRole.GUEST) return
        val snap = snapshotFlow.value ?: return
        // v1.10.15: prefetch HANYA bila lagu state saat ini sudah
        // diterapkan tamu. Snapshot yang membawa lagu BARU harus
        // memakai cache untuk lagu itu DULU (applyStateToPlayer di
        // bawah memanggil playTrack yang mengonsumsi cache) — bila
        // prefetch lagu SESUDAHNYA jalan lebih dulu, slot cache
        // tertimpa dan entri lagu baru hilang sebelum dipakai.
        // Pengecualian: belum ada state (menunggu host) — lagu
        // pertama antrean boleh di-prefetch. Guard ini DIPERTAHANKAN
        // v1.10.35: cache multi-slot mengurangi risiko tertimpa,
        // tapi resolve untuk lagu yang akan datang tetap tidak boleh
        // mendahului resolve lagu yang sedang ditunggu.
        val stateVideoId = snap.state?.videoId
        val currentVideoId = player.currentTrack.value?.youtubeVideoId
        if (stateVideoId != null && stateVideoId != currentVideoId) {
            return
        }
        // v1.10.35: prefetch beberapa lagu ke depan (bukan hanya
        // yang pertama) — lompat host ke lagu ke-2/ke-3 antrean
        // kini ikut tertutup. Tiap target punya cooldown sendiri
        // (PREFETCH_COOLDOWN_MS): prefetch yang gagal untuk satu lagu
        // tidak memblokir lagu lain, dan percobaan ulang terjadi
        // dalam hitungan detik, bukan 30 detik.
        val now = clockMs()
        prefetchAttemptAtMs.entries.removeAll { now - it.value > 60_000L }
        for (videoId in prefetchTargetVideoIds(snap.queue, snap.state, currentVideoId)) {
            if (player.hasPrefetchedStream(videoId)) continue
            val lastAttempt = prefetchAttemptAtMs[videoId] ?: 0L
            if (now - lastAttempt < PREFETCH_COOLDOWN_MS) continue
            prefetchAttemptAtMs[videoId] = now
            runCatching { player.prefetchStream(videoId) }
        }
    }

    /**
     * Koreksi posisi tamu terhadap posisi host (v1.10.15) — HARUS
     * dipanggil di thread Main (menyentuh ExoPlayer lewat
     * setPlaybackRate/seekTo, sama seperti pemanggil lainnya).
     *
     * Selisih menengah → rate halus (0.98×/1.02×) sampai < 250ms;
     * selisih > 2 detik → seek + rate kembali 1.0×. Tidak bertindak
     * saat buffering / tidak memutar / lagu berbeda (ticker
     * berikutnya yang menilai ulang).
     */
    private fun applyDriftCorrection(st: JamState) {
        if (player.isBuffering.value || !player.isPlaying.value) return
        if (player.currentTrack.value?.youtubeVideoId != st.videoId) return
        val expected = expectedPositionMs(st, clockMs(), stateReceivedAtMs)
        when (driftCorrection(player.positionMs.value, expected, player.currentPlaybackRate())) {
            DriftCorrection.SEEK -> {
                player.setPlaybackRate(1.0f)
                player.seekTo(expected)
            }
            DriftCorrection.SPEED_UP -> player.setPlaybackRate(DRIFT_RATE_SPEED_UP)
            DriftCorrection.SLOW_DOWN -> player.setPlaybackRate(DRIFT_RATE_SLOW_DOWN)
            DriftCorrection.RESET_RATE -> player.setPlaybackRate(1.0f)
            DriftCorrection.NONE -> Unit
        }
    }

    /** Terapkan snapshot baru (versi state baru / ended / anggota). */
    private suspend fun guestApplyLoop() {
        var lastVersionSeen = ""
        snapshotFlow.collect { snap ->
            if (snap == null) return@collect
            if (snap.ended && !endedHandled) {
                endedHandled = true
                showToast(appContext, "The Jam session has ended.")
                teardown()
                return@collect
            }
            if (snap.hostUid.isEmpty() && hadContent && snap.state == null &&
                snap.members.isEmpty() && snap.queue.isEmpty()
            ) {
                // Node dikosongkan (dihapus) setelah sempat ada isinya.
                onNodeGone()
                return@collect
            }
            publishUi()
            // v1.10.15: prefetch dinilai di TIAP snapshot — penambahan
            // antrean tidak mengubah versionKey state, jadi panggilan
            // ini HARUS sebelum early-return versionKey di bawah.
            maybePrefetchNext()
            val st = snap.state ?: return@collect
            if (st.versionKey == lastVersionSeen) return@collect
            lastVersionSeen = st.versionKey
            // v1.10.35: mulai resolve lagu BARU sedini mungkin. Kick
            // prefetch ini (IO, async) + single-flight di player
            // membuat apply di bawah bergabung ke resolve yang sama,
            // bukan memulai rantai Go + Kotlin kedua. Tidak menambah
            // langkah tunggu: kick kembali langsung, apply tetap
            // jalan; lagu yang sama tidak di-kick ulang (guard
            // videoId di bawah).
            val newVideoId = st.videoId
            if (newVideoId.isNotBlank() &&
                newVideoId != player.currentTrack.value?.youtubeVideoId
            ) {
                runCatching { player.prefetchStream(newVideoId) }
            }
            withContext(Dispatchers.Main) { applyStateToPlayer(st) }
            publishUi()
        }
    }

    /** Terapkan satu state host ke player lokal (jalur resolve yang sudah ada). */
    private fun applyStateToPlayer(st: JamState) {
        if (st.videoId.isBlank()) {
            // Host memutar lagu lokal — tamu tidak bisa mengikuti;
            // jangan ubah pemutaran lokal, UI yang menjelaskan.
            return
        }
        val current = player.currentTrack.value
        if (current?.youtubeVideoId == st.videoId) {
            if (st.playing) {
                if (!player.isPlaying.value) player.resume()
                // v1.10.15: koreksi drift halus (rate/seek) menggantikan
                // seek mentah — respons cepat tanpa menunggu ticker.
                applyDriftCorrection(st)
            } else {
                if (player.isPlaying.value) player.pause()
                // Host pause: rate kembali normal; posisi tetap
                // dikoreksi seek bila meleset (posisi diam saat pause).
                player.setPlaybackRate(1.0f)
                val expected = expectedPositionMs(st, clockMs(), stateReceivedAtMs)
                if (needsSeek(player.positionMs.value, expected)) {
                    player.seekTo(expected)
                }
            }
            return
        }
        val track = Track(
            id = "ytm:${st.videoId}",
            title = st.title,
            artist = st.artist,
            source = Source.YOUTUBE_MUSIC,
            engine = Engine.YOUTUBE,
            durationMs = st.durationMs,
            youtubeVideoId = st.videoId,
            imageUrl = st.artworkUrl.takeIf { it.isNotEmpty() },
        )
        // Host pause: guestHoldPauseWatch (berdasar snapshot terbaru,
        // bukan flag) akan pause begitu resolve selesai dan pemutaran
        // mulai — tidak ada status hold yang disimpan.
        player.playTrack(track)
    }

    /**
     * Tamu: bila resolve selesai dan host sedang pause, langsung pause
     * (tutup celah 2 detik sebelum ticker konvergensi).
     *
     * v1.10.10: status hold DITURUNKAN dari snapshot terbaru setiap
     * kali — TIDAK ADA flag sticky. Sebelumnya `holdPausedVideoId`
     * di-set saat host pause dan tidak pernah dihapus saat host
     * kembali memutar, sehingga watch ini + ticker konvergensi
     * mem-pause tamu lagi dalam hitungan ms untuk lagu yang sama
     * (diam permanen, tanpa error, dialog tampak sehat — persis
     * laporan HP v1.10.9). Kini: snapshot berkata playing=true →
     * tidak ada penahanan; snapshot berkata pause → tahan.
     */
    private suspend fun guestHoldPauseWatch() {
        player.isPlaying.collect { playing ->
            if (!playing) return@collect
            val st = snapshotFlow.value?.state ?: return@collect
            if (st.videoId.isBlank() || st.playing) return@collect
            val vid = player.currentTrack.value?.youtubeVideoId ?: return@collect
            if (vid == st.videoId) {
                withContext(Dispatchers.Main) {
                    player.pause()
                    // v1.10.15: rate koreksi tidak berlaku saat pause.
                    player.setPlaybackRate(1.0f)
                }
            }
        }
    }

    /** Tamu: konvergensi play/pause + koreksi lagu yang diganti manual, tiap 2 detik. */
    private suspend fun guestConvergenceTicker() {
        while (sessionActive) {
            delay(2000L)
            val snap = snapshotFlow.value ?: continue
            val st = snap.state ?: continue
            if (st.videoId.isBlank()) continue
            if (!isSnapshotFresh()) continue
            // v1.10.15: nilai prefetch tiap tick — antrean bisa berubah
            // tanpa snapshot state baru yang lolos versionKey.
            maybePrefetchNext()
            withContext(Dispatchers.Main) {
                val current = player.currentTrack.value
                if (current?.youtubeVideoId != st.videoId) {
                    // Tamu pindah lagu sendiri (atau belum menerapkan
                    // state) — kembalikan ke lagu host.
                    applyStateToPlayer(st)
                    return@withContext
                }
                if (player.isBuffering.value) return@withContext
                // v1.10.10: patokan SATU-SATUNYA adalah snapshot
                // terbaru — tanpa flag hold sticky (lihat
                // guestHoldPauseWatch). Resume tidak lagi diblokir
                // oleh flag basi dari pause sebelumnya.
                if (st.playing && !player.isPlaying.value) {
                    player.resume()
                } else if (!st.playing && player.isPlaying.value) {
                    player.pause()
                    // v1.10.15: rate koreksi tidak berlaku saat pause.
                    player.setPlaybackRate(1.0f)
                    val expected = expectedPositionMs(st, clockMs(), stateReceivedAtMs)
                    if (needsSeek(player.positionMs.value, expected)) {
                        player.seekTo(expected)
                    }
                }
            }
        }
    }

    /** Tamu: koreksi drift posisi tiap 2 detik (v1.10.12: dari 5 detik; toleransi 750ms via needsSeek). */
    private suspend fun guestDriftTicker() {
        while (sessionActive) {
            delay(2000L)
            val snap = snapshotFlow.value ?: continue
            val st = snap.state ?: continue
            if (!st.playing || st.videoId.isBlank()) continue
            if (!isSnapshotFresh()) continue
            withContext(Dispatchers.Main) {
                // v1.10.15: selisih menengah → rate halus; selisih
                // besar → seek (lihat applyDriftCorrection).
                applyDriftCorrection(st)
            }
        }
    }

    /**
     * Tamu: watchdog kesehatan follow (v1.10.18) — SATU-SATUNYA loop
     * yang mempertanyakan klaim `_isPlaying` player lokal.
     *
     * Tiga bukti "tidak berbunyi" (semuanya butuh snapshot segar yang
     * bilang playing + lagu lokal == lagu host):
     * 1. ended-stuck: posisi di ujung durasi, tidak memutar, tidak
     *    buffering — lagu selesai tapi antrean habis, resume() biasa
     *    tidak bisa menghidupkannya lagi (playWhenReady sudah true).
     *    Butuh 2 tick berturut-turut supaya pergantian lagu host
     *    yang tinggal milidetik tidak memicu restart sia-sia.
     * 2. stall: mengklaim memutar tapi posisi tidak bergerak melewati
     *    [followStallMs] (player IDLE setelah error / embed gagal
     *    diam-diam — posisi beku, klaim playing tetap true).
     * 3. buffering-stall: buffering terus melewati
     *    [followBufferStallMs] (resolve/buffer macet).
     *
     * Tindakan: [PlayerManager.playTrack] pada lagu yang sama — jalur
     * yang SAMA dengan play manual user (me-resolve dari nol),
     * maksimal 3x per versi state, backoff 0/3/5 tick. Sehat 2 tick
     * berturut-turut → anggaran diisi ulang + status dibersihkan.
     * Anggaran habis dan masih macet → status jujur di kartu Jam
     * (followProblem); tidak ada retry tanpa henti. Versi state baru
     * = kesempatan baru (anggaran reset).
     */
    private suspend fun guestFollowWatch() {
        while (sessionActive) {
            delay(followWatchIntervalMs)
            followTick++
            if (role != JamRole.GUEST) continue
            val snap = snapshotFlow.value ?: continue
            val st = snap.state
            val now = clockMs()
            if (st == null || st.videoId.isBlank() || !st.playing || !isSnapshotFresh()) {
                // Tidak ada yang perlu diikuti (host pause / belum ada
                // state / snapshot basi): baseline ulang + status bersih.
                followLastPosMs = player.positionMs.value
                followLastPosAtMs = now
                followPrevTickPosMs = player.positionMs.value
                followBufferingSinceMs = 0L
                followEndedSinceMs = 0L
                followHealthyTicks = 0
                if (followProblemInternal != null) setFollowProblem(null)
                continue
            }
            if (st.versionKey != followVersionKey) {
                // State baru dari host = kesempatan baru: anggaran
                // penuh, status bersih, baseline posisi baru.
                followVersionKey = st.versionKey
                followAttempts = 0
                followLastAttemptTick = -100L
                followLastPosMs = player.positionMs.value
                followLastPosAtMs = now
                followPrevTickPosMs = player.positionMs.value
                followBufferingSinceMs = 0L
                followEndedSinceMs = 0L
                followHealthyTicks = 0
                if (followProblemInternal != null) setFollowProblem(null)
            }
            val current = player.currentTrack.value
            if (current?.youtubeVideoId != st.videoId) {
                // Lagu lokal belum menerapkan state ini — jalur apply
                // yang memilikinya; watchdog tidak ikut campur.
                followLastPosMs = player.positionMs.value
                followLastPosAtMs = now
                followPrevTickPosMs = player.positionMs.value
                continue
            }
            val pos = player.positionMs.value
            val dur = player.durationMs.value
            val playing = player.isPlaying.value
            val buffering = player.isBuffering.value
            if (pos != followLastPosMs) {
                followLastPosMs = pos
                followLastPosAtMs = now
            }
            if (buffering) {
                if (followBufferingSinceMs == 0L) followBufferingSinceMs = now
            } else {
                followBufferingSinceMs = 0L
            }
            // Bukti 1: ended-stuck (2 tick berturut-turut).
            val atEnd = dur > 0L && pos >= dur - 300L && !playing && !buffering
            if (atEnd) {
                if (followEndedSinceMs == 0L) followEndedSinceMs = now
            } else {
                followEndedSinceMs = 0L
            }
            val endedStuck = atEnd && followEndedSinceMs != 0L &&
                now - followEndedSinceMs >= followWatchIntervalMs
            // Bukti 2: stall — klaim memutar, posisi beku.
            val stalledClaim = playing && !buffering &&
                now - followLastPosAtMs >= followStallMs
            // Bukti 3: buffering macet.
            val bufferingStuck = buffering && followBufferingSinceMs != 0L &&
                now - followBufferingSinceMs >= followBufferStallMs
            val stuck = endedStuck || stalledClaim || bufferingStuck
            // Bukti sehat: memutar + posisi benar-benar maju.
            val advancing = playing && !buffering && pos > followPrevTickPosMs + 100L
            followPrevTickPosMs = pos
            if (!stuck) {
                if (advancing) {
                    followHealthyTicks++
                    if (followHealthyTicks >= 2) {
                        followAttempts = 0
                        followLastAttemptTick = -100L
                        if (followProblemInternal != null) setFollowProblem(null)
                    }
                } else {
                    followHealthyTicks = 0
                }
                continue
            }
            followHealthyTicks = 0
            if (followAttempts >= 3) {
                setFollowProblem(
                    "Couldn't play the host's song. The Jam is still live — " +
                        "try leaving and rejoining.",
                )
                continue
            }
            val ticksSinceAttempt = followTick - followLastAttemptTick
            val backoffTicks = when (followAttempts) {
                0 -> 0L
                1 -> 3L
                else -> 5L
            }
            if (ticksSinceAttempt < backoffTicks) {
                if (followAttempts > 0) {
                    setFollowProblem(
                        "Having trouble following the host's audio — retrying…",
                    )
                }
                continue
            }
            followAttempts++
            followLastAttemptTick = followTick
            // Me-resolve dari nol (jalur play manual): startTrack baru
            // + resolve + prepare. Di thread Main seperti jalur apply.
            withContext(Dispatchers.Main) { player.playTrack(current) }
            // Resolve segar butuh jendela penuh: baseline stall di-arm ulang.
            followLastPosMs = player.positionMs.value
            followLastPosAtMs = clockMs()
            followBufferingSinceMs = 0L
            followEndedSinceMs = 0L
        }
    }

    private fun isSnapshotFresh(): Boolean {
        if (lastSnapshotAtMs <= 0L) return false
        return clockMs() - lastSnapshotAtMs < 30_000L
    }

    // ---- UI state -----------------------------------------------------------

    /** Uid anggota saat ini (untuk UI: penanda pesan chat sendiri). */
    internal fun currentUidForChatUi(): String = currentCreds?.uid.orEmpty()

    private fun publishUi() {
        val snap = snapshotFlow.value
        val now = clockMs()
        val freshMembers = snap?.members
            ?.filter { isMemberFresh(it.lastSeenMs, now, clockOffsetMs) }
            .orEmpty()
        // v1.10.42: kunci kontrol tamu mengikuti toggle sesi dari
        // snapshot terbaru — tamu di sesi sharedControl=false
        // tetap terkunci (perilaku lama); tamu di sesi
        // sharedControl=true boleh MEMINTA kontrol (bukan memutar
        // lokal). Host tidak pernah terkunci. Diperbarui di sini
        // (bukan hanya di startSession) supaya toggle yang diubah
        // host di tengah sesi langsung berlaku di HP tamu.
        val shared = snap?.sharedControl == true
        val locked = role == JamRole.GUEST && sessionScope != null && !shared
        if (_guestLocked.value != locked) _guestLocked.value = locked
        val ownUid = currentCreds?.uid.orEmpty()
        val counts = voteCounts(snap?.voteMap.orEmpty())
        // v1.10.42: urutan tampil Up Next = vote terbanyak dulu,
        // tiebreak urutan queueOrder (derivasi per kunci 10.18
        // tidak berubah — hanya urutan tampilnya yang memakai vote).
        // Daftar yang sama dipakai kartu Jam DAN tab Up Next detail,
        // supaya keduanya selalu sama (prinsip 10.10).
        val rawUpcoming = upcomingJamQueue(snap?.queue.orEmpty(), snap?.state)
        val votedUpcoming = sortUpcomingByVotes(rawUpcoming, counts)
        val hostMember = snap?.members?.firstOrNull { it.uid == snap.hostUid }
        val hostAway = role == JamRole.GUEST && snap != null &&
            (hostMember == null || !isHostFresh(hostMember.lastSeenMs, now, clockOffsetMs))
        val st = snap?.state
        _uiState.value = JamUiState(
            active = sessionScope != null,
            role = role,
            code = sessionCode,
            hostName = snap?.hostName.orEmpty(),
            // v1.10.12: publish flip yang gagal total tampil sebagai
            // RECONNECTING (jujur) sampai publish berikutnya
            // berhasil — tanpa mengubah `connection` milik streamLoop.
            connection = if (publishDegraded && sessionScope != null) {
                JamConnection.RECONNECTING
            } else {
                connection
            },
            transport = transport,
            members = freshMembers,
            queue = snap?.queue.orEmpty(),
            // v1.10.10: satu turunan untuk dialog + Up Next.
            // v1.10.18: derivasi per kunci dari state (host + tamu
            // menghitung dari snapshot yang sama).
            // v1.10.42: urutan tampil memakai vote (lihat di atas).
            upcomingQueue = votedUpcoming,
            nowTitle = st?.title.orEmpty(),
            nowArtist = st?.artist.orEmpty(),
            hostLocalOnly = st != null && st.videoId.isBlank(),
            waitingForHost = sessionScope != null && st == null,
            hostAway = hostAway,
            // v1.10.17: hanya reaksi segar (< 60 detik) yang sampai
            // ke UI; yang lebih tua dibuang di sini, bukan di server.
            recentReactions = pruneReactions(snap?.reactions.orEmpty(), now),
            // v1.10.18: status jujur watchdog follow-health (tamu).
            followProblem = followProblemInternal,
            sharedControl = shared,
            canControl = role == JamRole.HOST || shared,
            voteCounts = counts,
            votedKeys = snap?.voteMap.orEmpty()
                .filter { (_, voters) -> ownUid.isNotEmpty() && ownUid in voters }
                .keys,
            messages = snap?.messages.orEmpty(),
            // v1.10.42: orbit memakai batas 30 detik (bukan 90) —
            // anggota yang heartbeat-nya basi tidak tampil di orbit,
            // tapi tetap dihitung di daftar "listening" (freshMembers).
            orbitMembers = snap?.members.orEmpty()
                .filter { isOrbitMemberFresh(it.lastSeenMs, now, clockOffsetMs) },
        )
    }

    private fun friendlyJamError(e: Exception): String {
        return when (e) {
            is SessionGone -> "Session expired, sign in again."
            // v1.10.8: gagal transien di tengah sesi — jaringan,
            // bukan sesi kedaluwarsa; jangan minta login ulang.
            is JamTransient -> "Couldn't reach the Jam session — check your internet and try again."
            is JamRest.JamRestException ->
                when {
                    // 401 = token tidak berlaku (sudah di-retry via
                    // withToken; bila masih gagal, sesi memang habis).
                    e.code == 401 -> "Session expired, sign in again."
                    // v1.10.7: 403 = rules menolak (bukan sesi
                    // kedaluwarsa) — jangan minta login ulang.
                    else -> "Couldn't reach the Jam session — check your internet and try again."
                }
            is java.io.IOException -> "No internet — try again later."
            else -> e.message?.takeIf { it.isNotBlank() }
                ?: "Something went wrong — try again."
        }
    }

    companion object {
        private const val TAG = "Jam"
    }
}
