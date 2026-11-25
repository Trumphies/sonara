package com.sonara.app.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sonara.app.data.Engine
import com.sonara.app.data.LocalStore
import com.sonara.app.data.PlayEvent
import com.sonara.app.data.PlaybackSpeedPolicy
import com.sonara.app.data.RESUME_MIN_POSITION_MS
import com.sonara.app.data.RESUME_SAVE_INTERVAL_MS
import com.sonara.app.data.ResolveTrace
import com.sonara.app.data.Source
import com.sonara.app.data.SponsorBlockApi
import com.sonara.app.data.SponsorSegment
import com.sonara.app.data.StreamPrefetchCache
import com.sonara.app.data.Track
import com.sonara.app.data.YoutubeMusicApi
import com.sonara.app.data.decideSkip
import com.sonara.app.data.filterRadioItems
import com.sonara.app.data.needsRadioRefill
import com.sonara.app.data.positionToStore
import com.sonara.app.data.resumePositionFor
import com.sonara.app.data.toTrack
import com.sonara.app.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Satu antrean, dua jalur putar — pola MixMusic.
 *
 * - Engine EXOPLAYER (Media3 native): stream langsung
 *   YouTube Music (player API visionOS, itag Opus 251 / AAC 140).
 * - Engine YOUTUBE: player resmi YouTube IFrame — HANYA fallback
 *   kalau stream langsung gagal.
 *
 * Error selalu jujur: pesan yang jelas + bisa coba lagi, bukan
 * diam-diam gagal. Setiap pemutaran YTM yang gagal di stream
 * langsung jatuh ke embed; kalau embed juga gagal, error tampil.
 */
class PlayerManager(
    private val appContext: Context,
    private val store: LocalStore,
    val embed: WebEmbedPlayer,
) {
    enum class RepeatMode { OFF, ALL, ONE }

    /**
     * Undo lompatan SponsorBlock (v1.0.3): posisi SEBELUM lompat
     * (detik) + lagu yang dilompati + batas waktu tampil chip.
     * Tidak ada Snackbar di app ini — chip sementara di layar detail.
     */
    data class SkipUndo(
        val trackId: String,
        val skippedFromSec: Double,
        val expiresAtMs: Long,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Sleep timer (v1.10.6): berbunyi sekali -> pause. Timer sesi
    // (tidak persisten antar-restart); memilih durasi baru memulai
    // ulang. Berbasis jam dinding, tetap jalan walau lagu berganti.
    val sleepTimer = SleepTimer { pause() }

    /** Sleep timer dalam menit; 0 = mati/batal. Memilih menit mematikan mode akhir-lagu. */
    fun setSleepTimer(minutes: Int) {
        _sleepEndOfTrack.value = false
        if (minutes <= 0) sleepTimer.cancel()
        else sleepTimer.start(scope, minutes * 60_000L)
    }

    /** Epoch-ms saat sleep timer berbunyi, atau null bila tidak aktif. */
    val sleepTimerEndsAtMs: StateFlow<Long?> = sleepTimer.endsAtMs

    /**
     * Mode sleep timer "akhir lagu" (v1.0.5): berhenti saat lagu yang
     * sedang berjalan berakhir natural (bukan berdasarkan jam dinding).
     * Tetap terpasang walau lagu di-skip; hanya berbunyi sekali, lalu
     * mati sendiri. Nonaktif selama sesi Jam (posisi/kontrol milik
     * sesi) — gerbangnya di [onTrackEnded]. Memilih durasi menit
     * menggantikan mode ini, dan sebaliknya.
     */
    private val _sleepEndOfTrack = MutableStateFlow(false)
    val sleepEndOfTrack: StateFlow<Boolean> = _sleepEndOfTrack.asStateFlow()

    /** Pasang mode akhir-lagu (menggantikan timer menit bila ada). */
    fun setSleepTimerEndOfTrack() {
        sleepTimer.cancel()
        _sleepEndOfTrack.value = true
    }

    /** Kecepatan putar pilihan user (v1.0.5): default persist + rate per-episode. */
    private val _userPlaybackRate = MutableStateFlow(store.defaultPlaybackRate)
    val userPlaybackRate: StateFlow<Float> = _userPlaybackRate.asStateFlow()

    /**
     * Ubah kecepatan putar pilihan user: disimpan sebagai default,
     * disimpan per-episode bila lagu berjalan tergolong konten panjang,
     * dan langsung diterapkan — kecuali selama sesi Jam (rate milik
     * sesi; UI kecepatan disembunyikan saat Jam aktif).
     */
    fun setUserPlaybackRate(rate: Float) {
        val r = PlaybackSpeedPolicy.coerceRate(rate)
        _userPlaybackRate.value = r
        store.defaultPlaybackRate = r
        val current = _currentTrack.value
        if (current != null) {
            val dur = _durationMs.value.takeIf { it > 0 } ?: current.durationMs
            if (PlaybackSpeedPolicy.isLongForm(dur)) {
                store.saveEpisodeSpeed(current.id, r)
            }
        }
        if (!jamActiveProvider()) setPlaybackRate(r)
    }

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Jejak resolve stream lagu berjalan + stream terpilihnya
     * (v1.0.3, sheet "Track info"): di-reset tiap startTrack, diisi
     * saat resolve selesai (sukses/gagal). Stream null = lagu
     * berjalan dari URL tersimpan / jalur embed (tidak ada info
     * format langsung — sheet menampilkannya apa adanya).
     */
    private val _resolveTrace = MutableStateFlow(ResolveTrace.EMPTY)
    val resolveTrace: StateFlow<ResolveTrace> = _resolveTrace.asStateFlow()

    private val _currentStream = MutableStateFlow<YoutubeMusicApi.YtmStream?>(null)
    val currentStream: StateFlow<YoutubeMusicApi.YtmStream?> = _currentStream.asStateFlow()

    /**
     * Antrean RADIO (v1.0.3) — daftar TERPISAH dari antrean user
     * ([_queue]): lagu radio tidak pernah masuk antrean user, tidak
     * pernah disimpan (saveQueue hanya menulis _queue), dan tidak
     * pernah menyentuh matematika antrean/Jam. [_radioIndex] menunjuk
     * lagu radio yang sedang diputar (-1 = belum ada); [_currentIndex]
     * tetap menunjuk posisi terakhir di antrean user, supaya
     * [next] selalu bisa mendahulukan lagu user yang belum diputar.
     * Radio nonaktif selama Jam (JamSessionManager memanggil
     * [clearRadio] saat sesi dimulai).
     */
    private val _radioQueue = MutableStateFlow<List<Track>>(emptyList())
    val radioQueue: StateFlow<List<Track>> = _radioQueue.asStateFlow()

    private val _radioIndex = MutableStateFlow(-1)
    val radioIndex: StateFlow<Int> = _radioIndex.asStateFlow()

    private val _radioActive = MutableStateFlow(false)
    val radioActive: StateFlow<Boolean> = _radioActive.asStateFlow()

    private val _radioSeed = MutableStateFlow<Track?>(null)
    val radioSeed: StateFlow<Track?> = _radioSeed.asStateFlow()

    /** Generasi pengambilan radio — hasil fetch basi dibuang. */
    private var radioGeneration = 0

    /** True bila pengambilan isi-ulang radio sedang berjalan. */
    private var radioRefillInFlight = false

    /**
     * Segmen SponsorBlock lagu berjalan (v1.0.3) + undo lompatan
     * terakhir. Segmen di-fetch best-effort per lagu; kosong = tidak
     * ada / toggle mati / tidak terjangkau — tidak ada perilaku skip.
     */
    private val sponsorBlockApi = SponsorBlockApi()

    private val _segments = MutableStateFlow<List<SponsorSegment>>(emptyList())
    val segments: StateFlow<List<SponsorSegment>> = _segments.asStateFlow()

    private val _skipUndo = MutableStateFlow<SkipUndo?>(null)
    val skipUndo: StateFlow<SkipUndo?> = _skipUndo.asStateFlow()

    private var skipUndoTimeout: Runnable? = null

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeat = MutableStateFlow(RepeatMode.OFF)
    val repeat: StateFlow<RepeatMode> = _repeat.asStateFlow()

    private val _volume = MutableStateFlow(80)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val ytmApi = YoutubeMusicApi()

    /**
     * True bila sesi Jam sedang aktif (v1.10.21, diisi
     * SonaraApplication setelah Jam dibuat — player dibuat lebih
     * dulu). Dipakai gerbang fitur yang milik sesi: posisi-lanjut
     * podcast nonaktif selama Jam (posisi milik sesi).
     */
    var jamActiveProvider: () -> Boolean = { false }

    /** Epoch-ms simpan posisi-lanjut terakhir (polling 5 detik). */
    private var lastResumeSaveAtMs: Long = 0L

    /**
     * Cache prefetch stream tamu Jam (v1.10.15): 1 lagu ke depan,
     * in-memory saja, TTL 5 menit (lihat StreamPrefetchCache).
     * Diisi [prefetchStream] (hanya dipanggil JamSessionManager saat
     * tamu mengikuti sesi); dikonsumsi [playYtmTrack] sebelum resolve
     * — tamu tidak resolve ulang lagu yang sudah di-prefetch.
     */
    private val prefetchCache = StreamPrefetchCache()

    /**
     * Resolve stream sekali-jalan per videoId (v1.10.35): prefetch
     * tamu Jam + jalur putar berbagi SATU resolve yang sedang
     * berjalan untuk lagu yang sama. Tanpa ini keduanya memulai
     * rantai Go + Kotlin sendiri-sendiri di jaringan yang sama
     * (traffic ganda, keduanya melambat) tepat saat lagu berganti.
     * Entri dihapus dari map saat resolve-nya selesai (sukses/
     * gagal); hasilnya Result — tidak pernah throw ke pemanggil.
     * Hanya diakses dari coroutine IO (prefetch/putar); guard
     * synchronized untuk pembuatan + penghapusan entri.
     */
    private val inFlightResolves =
        mutableMapOf<String, Deferred<YoutubeMusicApi.ResolveOutcome>>()

    /**
     * Kembalikan resolve yang sedang berjalan untuk videoId ini,
     * atau mulai satu bila belum ada. Pemanggil meng-await hasilnya;
     * resolve yang sama tidak pernah dijalankan dua kali bersamaan.
     * Varian detailed membawa jejak langkahnya (sheet Track info);
     * [sharedResolve] adalah pembungkus Result-nya untuk pemanggil
     * lama (prefetch Jam).
     */
    private fun sharedResolveDetailed(
        videoId: String,
    ): Deferred<YoutubeMusicApi.ResolveOutcome> {
        synchronized(inFlightResolves) {
            inFlightResolves[videoId]?.let { return it }
            val deferred = scope.async {
                try {
                    ytmApi.audioQuality = store.audioQuality
                    ytmApi.resolveStreamDetailed(videoId)
                } catch (e: Exception) {
                    YoutubeMusicApi.ResolveOutcome(
                        Result.failure(e),
                        ResolveTrace.EMPTY,
                    )
                }
            }
            // Hapus dari map saat resolve-nya selesai (sukses/gagal/
            // batal) — didaftarkan SETELAH pembuatan karena lambda di
            // atas tidak boleh merujuk variabelnya sendiri.
            deferred.invokeOnCompletion {
                synchronized(inFlightResolves) {
                    if (inFlightResolves[videoId] === deferred) {
                        inFlightResolves.remove(videoId)
                    }
                }
            }
            inFlightResolves[videoId] = deferred
            return deferred
        }
    }

    private fun sharedResolve(videoId: String): Deferred<Result<YoutubeMusicApi.YtmStream>> =
        scope.async { sharedResolveDetailed(videoId).await().result }

    /**
     * Kecepatan putar ExoPlayer saat ini (v1.10.15) — SATU-SATUNYA
     * titik kontrol rate. 1.0× normal; Jam tamu memakai 0.98×/1.02×
     * untuk koreksi drift halus. Di luar Jam selalu 1.0× (di-reset
     * di [startTrack], [stop], dan teardown Jam).
     */
    private var playbackRate: Float = 1.0f

    /**
     * Ubah kecepatan putar (ExoPlayer `setPlaybackSpeed`). No-op bila
     * nilainya sama — pemanggil Jam boleh memanggil tiap evaluasi
     * ticker tanpa efek samping. Dipanggil dari thread Main (semua
     * jalur Jam memanggilnya di Main); teardown Jam yang berjalan
     * di IO mem-post lewat [mainHandler] di JamSessionManager.
     */
    fun setPlaybackRate(rate: Float) {
        if (rate == playbackRate) return
        playbackRate = rate
        try {
            exoPlayer.setPlaybackSpeed(rate)
        } catch (_: Exception) {
        }
    }

    fun currentPlaybackRate(): Float = playbackRate

    /**
     * Resolve stream lagu di background untuk dipakai SEBENTAR LAGI
     * (prefetch tamu Jam, v1.10.15). Gagal diam-diam (log tidak
     * ada — tanpa toast/snackbar): jalur resolve normal tetap
     * menjadi jaring pengaman saat lagu itu benar-benar diputar.
     * Tidak pernah dipanggil di luar sesi Jam tamu.
     */
    fun prefetchStream(videoId: String) {
        if (videoId.isBlank()) return
        if (prefetchCache.isFresh(videoId, System.currentTimeMillis())) return
        scope.launch {
            // v1.10.35: berbagi resolve yang sedang berjalan dengan
            // jalur putar (single-flight) — tidak ada rantai ganda.
            val stream = sharedResolve(videoId).await().getOrNull() ?: return@launch
            prefetchCache.put(videoId, stream.url, stream.durationMs, System.currentTimeMillis())
        }
    }

    /** True bila ada stream prefetch yang masih segar untuk videoId ini. */
    fun hasPrefetchedStream(videoId: String): Boolean =
        prefetchCache.isFresh(videoId, System.currentTimeMillis())

    /**
     * v1.0.2 background: player dibangun dengan wake mode + atribut
     * audio media. WAKE_MODE_LOCAL = ExoPlayer memegang PARTIAL_WAKE_LOCK
     * sendiri selama putar (izin WAKE_LOCK sudah dideklarasi) — CPU
     * tidak tidur saat layar mati. Atribut USAGE_MEDIA + CONTENT_TYPE_MUSIC
     * mengklasifikasikan stream sebagai musik untuk routing/fokus sistem.
     * handleAudioFocus=false: fokus dikelola PlayerManager sendiri
     * ([acquirePlaybackFocus]) supaya perlakuannya IDENTIK untuk jalur
     * ExoPlayer dan jalur embed — fokus bawaan ExoPlayer hanya menutup
     * jalur ExoPlayer, dan ducking internalnya akan dilawan applyVolume()
     * yang menulis volume tiap 100ms. Becoming-noisy juga dimatikan di
     * sini (setHandleAudioBecomingNoisy(false)) dan dimiliki PlayerManager
     * lewat satu receiver — lagi-lagi supaya jalur embed ikut terjaga.
     */
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ false,
        )
        .setHandleAudioBecomingNoisy(false)
        .build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _isBuffering.value = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) {
                    onTrackEnded()
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (activeEngine == Engine.EXOPLAYER) {
                    _isPlaying.value = playing
                    // v1.10.12: ExoPlayer isPlaying=true hanya bila
                    // playWhenReady + STATE_READY — inilah momen
                    // audio benar-benar berbunyi (bukan janji
                    // playExo). Jam host memakai ini sebagai
                    // baseline publish ulang.
                    if (playing) {
                        onAudibleStart?.invoke(
                            _currentTrack.value?.id.orEmpty(),
                            generation,
                        )
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (activeEngine == Engine.EXOPLAYER) {
                    _isBuffering.value = false
                    _isPlaying.value = false
                    val track = _currentTrack.value
                    // v1.10.1: stream yang mati DI AWAL lagu (URL
                    // kedaluwarsa/ditolak — penyebab klasik teks
                    // "source error") dicoba resolve ulang SEKALI
                    // sebelum menyerah dengan pesan jujur. Error di
                    // tengah lagu tidak di-retry otomatis.
                    if (track != null && track.youtubeVideoId != null &&
                        resolveRetryTrackId != track.id &&
                        exoPlayer.currentPosition < 2000
                    ) {
                        resolveRetryTrackId = track.id
                        retryResolve(track, error)
                        return
                    }
                    _error.value = friendlyPlaybackError(error)
                }
            }
        })
    }

    private var activeEngine: Engine? = null
    private var generation = 0
    private var embedWatchdog: Runnable? = null

    /**
     * Hook sinkron Jam (v1.10.12): dipanggil saat audio BENAR-BENAR
     * mulai berbunyi untuk satu generasi lagu — ExoPlayer mencapai
     * STATE_READY + playing, atau embed IFrame melapor playing.
     * Berbeda dari StateFlow isPlaying yang di-set optimistis di
     * playExo() SEBELUM buffering selesai (itulah yang membuat
     * transisi READY tidak memicu emisi combine dan host tidak
     * pernah mempublish ulang saat lagu benar-benar berbunyi).
     * Dipasang/dilepas JamSessionManager hanya selama sesi host;
     * null di luar sesi. Argumen: id track + nomor generasi
     * startTrack (naik tiap lagu baru/replay — pemanggil
     * men-dedup per generasi supaya satu lagu hanya memicu satu
     * publish audible-start).
     */
    var onAudibleStart: ((trackId: String, generation: Int) -> Unit)? = null

    /**
     * Hook akhir-natural lagu (v1.10.34): dipanggil dari
     * [onTrackEnded] HANYA bila lagu benar-benar selesai sendiri
     * (bukan pause manual, bukan repeat-one yang memutar ulang lagu
     * yang sama). Dipakai JamSessionManager (host saja) untuk
     * autoplay fallback saat antrean sesi kosong. Lagu yang
     * berakhir diteruskan supaya host tahu apa yang baru selesai.
     * Tidak pernah dipanggil untuk lagu lokal tanpa antrean Jam —
     * pemanggil yang memutuskan (guard sesi + peran di sana).
     */
    var onTrackEndedNatural: ((endedTrack: Track) -> Unit)? = null

    /** Id track yang sudah memakai jatah re-resolve sekali (anti-loop). */
    private var resolveRetryTrackId: String? = null

    /**
     * Pesan error putar yang JUJUR dan spesifik (v1.10.1). Sebelumnya
     * pesan mentah `PlaybackException.message` ditampilkan apa adanya
     * — untuk kegagalan source, Media3 mengembalikan teks generik
     * "Source error", itulah tulisan yang dilihat user di HP.
     */
    private fun friendlyPlaybackError(error: PlaybackException): String {
        val raw = error.message ?: ""
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "There's a network problem playing this song. " +
                    "Check your internet, then try again."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
                "This song's stream was rejected or expired by YouTube. " +
                    "Try again — the stream will be fetched again."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                "This device doesn't support this song's audio format."
            else ->
                if (raw.isBlank() || raw.equals("Source error", ignoreCase = true)) {
                    "YouTube couldn't open this song's stream. Try again."
                } else {
                    "Couldn't play audio: $raw"
                }
        }
    }

    /** Resolve ulang stream track yang gagal di awal lagu (sekali saja). */
    private fun retryResolve(track: Track, originalError: PlaybackException) {
        val videoId = track.youtubeVideoId ?: run {
            _error.value = friendlyPlaybackError(originalError)
            return
        }
        val gen = generation
        scope.launch {
            val result = ytmApi.resolveStream(videoId)
            mainHandler.post {
                if (gen != generation) return@post
                if (_currentTrack.value?.id != track.id) return@post
                val stream = result.getOrNull()
                if (stream != null) {
                    val resolved = track.copy(
                        engine = Engine.EXOPLAYER,
                        streamUrl = stream.url,
                        durationMs = if (stream.durationMs > 0) {
                            stream.durationMs
                        } else {
                            track.durationMs
                        },
                    )
                    replaceCurrentTrack(resolved)
                    playExo(resolved)
                } else {
                    _error.value = friendlyPlaybackError(originalError)
                }
            }
        }
    }

    // ---- Audio focus + becoming-noisy (v1.0.2) ----
    // Satu pemilik untuk kedua engine (lihat catatan di builder
    // ExoPlayer di atas). Keputusan murni di AudioFocusPolicy;
    // di sini hanya efeknya.
    private val audioManager: AudioManager by lazy {
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var pausedByFocusLoss = false
    private var focusDucked = false
    private var focusDuckScale = 1f

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        onAudioFocusChange(change)
    }

    private val focusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    private fun acquirePlaybackFocus() {
        try {
            val result = audioManager.requestAudioFocus(focusRequest)
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED &&
                result != AudioManager.AUDIOFOCUS_REQUEST_DELAYED
            ) {
                Log.w("PlayerManager", "Audio focus request returned $result; playing anyway")
            }
        } catch (e: Exception) {
            Log.w("PlayerManager", "requestAudioFocus failed: ${e.message}", e)
        }
        registerNoisyReceiver()
    }

    private fun releasePlaybackFocus() {
        try {
            audioManager.abandonAudioFocusRequest(focusRequest)
        } catch (e: Exception) {
            Log.w("PlayerManager", "abandonAudioFocus failed: ${e.message}", e)
        }
        unregisterNoisyReceiver()
    }

    private fun onAudioFocusChange(change: Int) {
        when (AudioFocusPolicy.actionFor(change, pausedByFocusLoss, focusDucked)) {
            AudioFocusPolicy.Action.PAUSE_PERMANENT -> {
                // Kehilangan permanen: jeda + lepas fokus; gain
                // berikutnya TIDAK boleh me-resume sendiri.
                pausedByFocusLoss = false
                pauseEngines()
                releasePlaybackFocus()
            }
            AudioFocusPolicy.Action.PAUSE_TRANSIENT -> {
                // Kehilangan sementara: jeda, tapi fokus + receiver
                // dipertahankan supaya gain bisa me-resume.
                pausedByFocusLoss = true
                pauseEngines()
            }
            AudioFocusPolicy.Action.DUCK -> {
                focusDucked = true
                focusDuckScale = AudioFocusPolicy.DUCK_SCALE
                applyVolume()
            }
            AudioFocusPolicy.Action.RESTORE_AND_RESUME_IF_PAUSED -> {
                val resumeAfter = pausedByFocusLoss
                focusDucked = false
                focusDuckScale = 1f
                pausedByFocusLoss = false
                applyVolume()
                if (resumeAfter && _currentTrack.value != null && !_isPlaying.value) {
                    resume()
                }
            }
            AudioFocusPolicy.Action.IGNORE -> Unit
        }
    }

    /** Headphone dicabut / audio jadi "noisy": jeda, kedua engine. */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && _isPlaying.value) {
                pause()
            }
        }
    }
    private var noisyRegistered = false

    private fun registerNoisyReceiver() {
        if (noisyRegistered) return
        try {
            ContextCompat.registerReceiver(
                appContext,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            noisyRegistered = true
        } catch (e: Exception) {
            Log.w("PlayerManager", "register noisy receiver failed: ${e.message}", e)
        }
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyRegistered) return
        noisyRegistered = false
        try {
            appContext.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {
        }
    }

    // ---- Envelope volume: fade in/out (v1.6) ----
    // fadeScale 0..1 dikalikan dengan volume dasar DAN skala crossfade
    // di applyVolume() — satu-satunya tempat volume ExoPlayer ditulis.
    // Dengan begitu fade hidup berdampingan dengan crossfade/gapless:
    // crossfade tetap memudarkan akhir lagu, gapless tetap tanpa jeda
    // (fade dilewati), dan fade-in melembutkan awal tiap lagu.
    private var fadeScale = 1f
    private var fadeGen = 0
    private var fadeRunnable: Runnable? = null
    private var lastEmbedVolume = -1

    /** Satu-satunya penulis volume ExoPlayer. Embed hanya menerima volume dasar. */
    private fun applyVolume(crossfadeScale: Float = 1f) {
        // v1.0.2: skala duck fokus ikut di sini (satu-satunya penulis
        // volume) — ducking tidak dilawan polling 100ms.
        val exoScale = (_volume.value / 100f) * fadeScale * crossfadeScale.coerceIn(0f, 1f) * focusDuckScale
        try {
            exoPlayer.volume = exoScale
        } catch (_: Exception) {
        }
        // IFrame YouTube mereset volume ke 100 saat onReady — envelope
        // fade/crossfade tidak bisa diandalkan di jalur embed, jadi
        // embed hanya menerima volume dasar (dikirim hanya saat berubah).
        // Duck fokus ikut dihitung di sini supaya jalur embed ikut meredup.
        val embedPct = (_volume.value * focusDuckScale).toInt().coerceIn(0, 100)
        if (embedPct != lastEmbedVolume) {
            lastEmbedVolume = embedPct
            embed.setVolume(embedPct)
        }
    }

    /** Fade masuk 300ms saat lagu mulai. Dilewati kalau gapless ON. */
    private fun startFadeIn() {
        cancelFade()
        if (store.gapless) {
            fadeScale = 1f
            applyVolume()
            return
        }
        val gen = ++fadeGen
        fadeScale = 0f
        applyVolume()
        var ticks = 0
        val step = object : Runnable {
            override fun run() {
                if (gen != fadeGen) return
                ticks++
                fadeScale = (ticks / 12f).coerceIn(0f, 1f)
                applyVolume()
                if (ticks < 12) {
                    fadeRunnable = this
                    mainHandler.postDelayed(this, 25)
                } else {
                    fadeRunnable = null
                }
            }
        }
        fadeRunnable = step
        mainHandler.postDelayed(step, 25)
    }

    /** Fade keluar 150ms, lalu jeda betulan (pause manual). */
    private fun fadeOutThenPause() {
        cancelFade()
        val gen = ++fadeGen
        var ticks = 0
        val step = object : Runnable {
            override fun run() {
                if (gen != fadeGen) return
                ticks++
                fadeScale = (1f - ticks / 6f).coerceIn(0f, 1f)
                applyVolume()
                if (ticks < 6) {
                    fadeRunnable = this
                    mainHandler.postDelayed(this, 25)
                } else {
                    fadeRunnable = null
                    try {
                        exoPlayer.pause()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        fadeRunnable = step
        mainHandler.postDelayed(step, 25)
    }

    private fun cancelFade() {
        fadeGen++
        fadeRunnable?.let { mainHandler.removeCallbacks(it) }
        fadeRunnable = null
    }

    init {
        embed.onState = { state ->
            // Abaikan callback basi dari track sebelumnya.
            if (activeEngine == Engine.YOUTUBE) {
                _isPlaying.value = state.playing
                _positionMs.value = state.positionMs
                if (state.durationMs > 0) _durationMs.value = state.durationMs
                if (state.playing) {
                    cancelEmbedWatchdog()
                    // v1.10.12: embed melapor playing = audio
                    // benar-benar berbunyi (jalur fallback) — hook
                    // audible-start yang sama dengan ExoPlayer.
                    onAudibleStart?.invoke(
                        _currentTrack.value?.id.orEmpty(),
                        generation,
                    )
                }
                if (state.ended) onTrackEnded()
            }
        }
        embed.onError = { message ->
            if (activeEngine == Engine.YOUTUBE) {
                cancelEmbedWatchdog()
                _isBuffering.value = false
                _isPlaying.value = false
                _error.value = "YouTube player error ($message). Try again."
            }
        }
        startExoPolling()
        restoreQueue()
    }

    // ---- antrean ----

    fun playQueue(tracks: List<Track>, index: Int = 0) {
        if (tracks.isEmpty()) return
        // Antrean eksplisit yang baru menggantikan radio basi (v1.0.3):
        // radio milik seed lama tidak boleh mengalir di belakang
        // antrean yang sengaja dipilih user.
        clearRadio()
        // Setiap salinan yang masuk antrean diberi UID segar (kunci
        // stabil drag-to-reorder) — lagu yang sama boleh muncul 2x.
        _queue.value = tracks.map {
            it.copy(queueUid = java.util.UUID.randomUUID().toString())
        }
        saveQueue()
        playAt(index)
    }

    fun playTrack(track: Track) {
        val list = _queue.value.toMutableList()
        val existing = list.indexOfFirst { it.id == track.id }
        if (existing >= 0) {
            playAt(existing)
        } else {
            list.add(track.copy(queueUid = java.util.UUID.randomUUID().toString()))
            _queue.value = list
            saveQueue()
            playAt(list.lastIndex)
        }
    }

    /**
     * Tambah ke akhir antrean; mengembalikan SALINAN yang masuk
     * antrean (queueUid segar — v1.10.18: JamSessionManager memetakan
     * queueUid salinan ini ke push key antrean sesi, supaya state
     * host membawa kunci lagu yang sedang diputar).
     */
    fun addToQueue(track: Track): Track {
        val copy = track.copy(queueUid = java.util.UUID.randomUUID().toString())
        _queue.value = _queue.value + copy
        saveQueue()
        return copy
    }

    /**
     * Tambah BANYAK lagu ke akhir antrean sekaligus (v1.10.21,
     * aksi bulk multi-select): tiap salinan diberi UID segar, satu
     * kali simpan. Mengembalikan jumlah yang ditambahkan.
     */
    fun addTracksToQueue(tracks: List<Track>): Int {
        if (tracks.isEmpty()) return 0
        val copies = tracks.map {
            it.copy(queueUid = java.util.UUID.randomUUID().toString())
        }
        _queue.value = _queue.value + copies
        saveQueue()
        return copies.size
    }

    fun playNext(track: Track) {
        val list = _queue.value.toMutableList()
        val at = (_currentIndex.value + 1).coerceIn(0, list.size)
        list.add(at, track.copy(queueUid = java.util.UUID.randomUUID().toString()))
        _queue.value = list
        saveQueue()
    }

    fun removeFromQueue(index: Int) {
        val list = _queue.value.toMutableList()
        if (index !in list.indices) return
        val wasCurrent = index == _currentIndex.value
        list.removeAt(index)
        _queue.value = list
        saveQueue()
        if (wasCurrent) {
            if (list.isEmpty()) stop() else playAt(index.coerceAtMost(list.lastIndex))
        } else {
            _currentIndex.value = indexAfterRemove(_currentIndex.value, index)
        }
    }

    fun clearQueue() {
        stop()
        _queue.value = emptyList()
        _currentIndex.value = -1
        saveQueue()
    }

    /**
     * Hapus SATU salinan lagu dari antrean lokal berdasarkan videoId,
     * TANPA PERNAH menyentuh lagu yang sedang berjalan (v1.10.18,
     * dipakai JamSessionManager saat host menghapus item antrean
     * sesi: hostQueueWatch sudah lebih dulu meng-append item itu ke
     * antrean lokal, jadi menghapusnya dari RTDB saja tidak cukup —
     * tanpa ini pemutar lokal tetap advance ke lagu yang dihapus
     * dan menerbitkannya lagi ke tamu). Kandidat diutamakan SETELAH
     * lagu berjalan, lalu sebelum; bila tidak ada salinan non-current,
     * tidak terjadi apa-apa. True = satu salinan dihapus.
     */
    fun removeQueuedByVideoId(videoId: String): Boolean {
        if (videoId.isBlank()) return false
        val list = _queue.value
        val cur = _currentIndex.value
        val idx = list.indices.firstOrNull { it > cur && list[it].youtubeVideoId == videoId }
            ?: list.indices.firstOrNull { it != cur && list[it].youtubeVideoId == videoId }
            ?: return false
        removeFromQueue(idx)
        return true
    }

    /**
     * Sisipkan kembali lagu yang dihapus ke posisi semula (Undo
     * swipe-hapus, v1.10.16). Track yang sama mempertahankan
     * queueUid-nya (copy() tidak mengganti identitas antrean),
     * sehingga kunci LazyColumn tetap stabil. Indeks saat ini
     * digeser bila sisipan berada di depannya — cerminan terbalik
     * dari removeFromQueue.
     */
    fun insertIntoQueue(index: Int, track: Track) {
        val list = _queue.value.toMutableList()
        val at = index.coerceIn(0, list.size)
        list.add(at, track)
        _queue.value = list
        saveQueue()
        _currentIndex.value = indexAfterInsert(_currentIndex.value, at)
    }

    fun moveQueue(from: Int, to: Int) {
        val list = _queue.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        _queue.value = list
        saveQueue()
        val cur = _currentIndex.value
        _currentIndex.value = when {
            cur == from -> to
            from < cur && to >= cur -> cur - 1
            from > cur && to <= cur -> cur + 1
            else -> cur
        }
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        if (_shuffle.value) {
            val list = _queue.value.toMutableList()
            val cur = _currentIndex.value
            if (cur in list.indices) {
                val current = list.removeAt(cur)
                list.shuffle()
                list.add(0, current)
                _queue.value = list
                _currentIndex.value = 0
                saveQueue()
            }
        }
    }

    fun cycleRepeat() {
        _repeat.value = when (_repeat.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    /** Putar ulang lagu yang sedang dipilih (tombol "Coba lagi" di UI). */
    fun retryCurrent() {
        val track = _currentTrack.value ?: return
        _error.value = null
        resolveRetryTrackId = null
        // v1.10.1: paksa resolve stream BARU (URL lama dibuang — bisa
        // jadi sudah kedaluwarsa); tidak auto-skip ke lagu lain.
        // v1.10.3: copy ini (engine EXOPLAYER + streamUrl null) HARUS
        // lewat guard startTrack → playYtmTrack; sebelum v1.10.3 ia
        // jatuh langsung ke playExo dan gagal "Tidak ada stream".
        startTrack(track.copy(streamUrl = null))
    }

    // ---- pemutaran ----

    fun playAt(index: Int) {
        val list = _queue.value
        if (index !in list.indices) return
        _currentIndex.value = index
        startTrack(list[index])
    }

    fun togglePlay() {
        val track = _currentTrack.value ?: return
        if (_isPlaying.value) pause() else resume(track)
    }

    fun pause() {
        // Jeda oleh user (atau headphone dicabut): bukan jeda karena
        // fokus — gain fokus berikutnya tidak boleh me-resume sendiri.
        pausedByFocusLoss = false
        pauseEngines()
        releasePlaybackFocus()
    }

    /** Jeda mesinnya saja, tanpa mengubah kepemilikan fokus. */
    private fun pauseEngines() {
        // v1.10.21: simpan posisi-lanjut SEBELUM jeda (bukan Jam).
        saveResumeForCurrent()
        _isPlaying.value = false
        when (activeEngine) {
            Engine.EXOPLAYER -> fadeOutThenPause()
            Engine.YOUTUBE -> embed.pause() // IFrame mereset volume saat ready — fade tidak diandalkan
            else -> Unit
        }
    }

    fun resume(track: Track? = _currentTrack.value) {
        if (track == null) return
        // v1.0.2: resume (termasuk dari notifikasi lockscreen) harus
        // memegang fokus + memastikan foreground service hidup — service
        // bisa saja mati saat jeda panjang, dan resume tanpa FGS akan
        // dibunuh begitu app di-background-kan lagi.
        acquirePlaybackFocus()
        when (activeEngine) {
            Engine.EXOPLAYER -> {
                exoPlayer.play()
                startFadeIn()
            }
            Engine.YOUTUBE -> embed.play()
            else -> startTrack(track)
        }
        _isPlaying.value = true
        startService()
    }

    fun next(manual: Boolean = true) {
        if (_repeat.value == RepeatMode.ONE && !manual) {
            _currentTrack.value?.let { startTrack(it) }
            return
        }
        if (!jamActiveProvider()) {
            // Di luar Jam (v1.0.3): lagu user SELALU menang — maju
            // ke lagu user berikutnya yang tidak masuk daftar tidak
            // suka; radio hanya bila antrean user sudah habis; lalu
            // Repeat ALL mengulang dari lagu user pertama yang boleh
            // diputar. Lagu yang tidak disukai tidak pernah diputar
            // lewat advance (ketuk eksplisit tetap menang — playAt).
            val list = _queue.value
            val nextUser = nextPlayableIndex(list.size, _currentIndex.value) {
                store.isDisliked(list[it].id)
            }
            if (nextUser >= 0) {
                playAt(nextUser)
                return
            }
            val nextRadio = _radioIndex.value + 1
            if (_radioActive.value && nextRadio in _radioQueue.value.indices) {
                playRadioAt(nextRadio)
                return
            }
            if (_repeat.value == RepeatMode.ALL && list.isNotEmpty()) {
                val wrap = nextPlayableIndex(list.size, -1) {
                    store.isDisliked(list[it].id)
                }
                if (wrap >= 0) {
                    playAt(wrap)
                    return
                }
            }
            _isPlaying.value = false
            return
        }
        // Jam: perilaku lama persis — antrean milik sesi, tanpa
        // lompat-tidak-suka dan tanpa radio.
        val list = _queue.value
        if (list.isEmpty()) return
        val nextIndex = _currentIndex.value + 1
        if (nextIndex in list.indices) {
            playAt(nextIndex)
        } else if (_repeat.value == RepeatMode.ALL) {
            playAt(0)
        } else {
            _isPlaying.value = false
        }
    }

    fun previous() {
        if (_positionMs.value > 3000) {
            seekTo(0)
            return
        }
        val prev = _currentIndex.value - 1
        if (prev >= 0) playAt(prev) else seekTo(0)
    }

    // ---- radio (v1.0.3, NON-Jam) ----

    /**
     * Mulai radio dari [seed]: ambil lagu-lagu seperti seed dari
     * /next (RD…), simpan TERPISAH dari antrean user. Seed-nya
     * sendiri diputar bila belum sedang diputar (tidak me-restart
     * lagu yang sedang bunyi); radio mengalir lewat [next] begitu
     * antrean user habis. Selama Jam: no-op (UI menyembunyikan
     * aksinya; sesi memiliki antrean).
     */
    fun startRadio(seed: Track) {
        if (jamActiveProvider()) return
        val seedVideoId = seed.youtubeVideoId?.takeIf { it.isNotBlank() } ?: return
        radioGeneration++
        val gen = radioGeneration
        _radioSeed.value = seed
        _radioQueue.value = emptyList()
        _radioIndex.value = -1
        _radioActive.value = true
        radioRefillInFlight = false
        scope.launch {
            val result = ytmApi.fetchRadio(seedVideoId)
            val items = result.getOrNull() ?: emptyList()
            val disliked = store.dislikedIds()
            val tracks = filterRadioItems(items, seedVideoId, disliked)
                .map { it.toTrack().copy(queueUid = java.util.UUID.randomUUID().toString()) }
            mainHandler.post {
                if (gen != radioGeneration || !_radioActive.value) return@post
                _radioQueue.value = tracks
                maybeRefillRadio()
            }
        }
        if (_currentTrack.value?.id != seed.id) {
            playTrack(seed)
        }
    }

    /** Putar lagu radio di [index] (ketuk baris di seksi Radio). */
    fun playRadioAt(index: Int) {
        val list = _radioQueue.value
        if (index !in list.indices) return
        _radioIndex.value = index
        startTrack(list[index])
        maybeRefillRadio()
    }

    /** Hentikan radio sepenuhnya (dipakai UI, antrean baru, dan awal sesi Jam). */
    fun clearRadio() {
        radioGeneration++
        radioRefillInFlight = false
        _radioQueue.value = emptyList()
        _radioIndex.value = -1
        _radioActive.value = false
        _radioSeed.value = null
    }

    /**
     * Isi ulang radio bila sisa tinggal sedikit (v1.0.3): seed-nya
     * lagu radio TERAKHIR (bukan seed awal) supaya radio mengalir
     * terus; hasil disaring dari lagu yang sudah ada, yang tidak
     * disukai, dan seed-nya. Best-effort: gagal = radio habis di
     * lagu terakhir, tanpa error.
     */
    private fun maybeRefillRadio() {
        if (!_radioActive.value || jamActiveProvider()) return
        val list = _radioQueue.value
        val remaining = list.size - (_radioIndex.value + 1)
        if (!needsRadioRefill(remaining, radioRefillInFlight)) return
        val lastVideoId = list.lastOrNull()?.youtubeVideoId
            ?.takeIf { it.isNotBlank() } ?: return
        radioRefillInFlight = true
        val gen = radioGeneration
        scope.launch {
            try {
                val result = ytmApi.fetchRadio(lastVideoId)
                val items = result.getOrNull() ?: emptyList()
                val existing = list.mapNotNull { it.youtubeVideoId }.toSet()
                val fresh = items.filter { it.videoId !in existing }
                val disliked = store.dislikedIds()
                val tracks = filterRadioItems(fresh, lastVideoId, disliked)
                    .map { it.toTrack().copy(queueUid = java.util.UUID.randomUUID().toString()) }
                mainHandler.post {
                    radioRefillInFlight = false
                    if (gen != radioGeneration || !_radioActive.value) return@post
                    if (tracks.isNotEmpty()) _radioQueue.value = list + tracks
                }
            } catch (_: Exception) {
                mainHandler.post { radioRefillInFlight = false }
            }
        }
    }

    /**
     * Lagu [trackId] baru saja tidak disukai (v1.0.3): bila lagu itu
     * sedang diputar di luar Jam, lewati ke lagu berikutnya (aturan
     * [next] — lagu user menang, lalu radio). Selama Jam: tidak ada
     * lompatan (antrean milik sesi).
     */
    fun onTrackDisliked(trackId: String) {
        if (jamActiveProvider()) return
        if (_currentTrack.value?.id == trackId) {
            next(manual = true)
        }
    }

    /** Kembali ke posisi sebelum lompatan SponsorBlock terakhir (chip Undo). */
    fun undoLastSkip() {
        val undo = _skipUndo.value ?: return
        if (_currentTrack.value?.id != undo.trackId) return
        if (activeEngine == Engine.EXOPLAYER) {
            try {
                exoPlayer.seekTo((undo.skippedFromSec * 1000).toLong())
            } catch (_: Exception) {
            }
        }
        clearSkipUndo()
    }

    /** Hapus chip Undo (dipakai setelah undo / kedaluwarsa 6 detik). */
    fun clearSkipUndo() {
        _skipUndo.value = null
        skipUndoTimeout?.let { mainHandler.removeCallbacks(it) }
        skipUndoTimeout = null
    }

    fun seekTo(positionMs: Long) {
        when (activeEngine) {
            Engine.EXOPLAYER -> exoPlayer.seekTo(positionMs)
            Engine.YOUTUBE -> embed.seekSeconds(positionMs / 1000.0)
            else -> Unit
        }
        _positionMs.value = positionMs
        // v1.10.21: seek sengaja ke < 10 detik menghapus posisi
        // tersimpan (bukan Jam — posisi milik sesi).
        if (positionMs < RESUME_MIN_POSITION_MS && !jamActiveProvider()) {
            _currentTrack.value?.let { track ->
                runCatching { store.clearResumePosition(track.id) }
            }
        }
    }

    fun setVolume(percent: Int) {
        val v = percent.coerceIn(0, 100)
        _volume.value = v
        applyVolume()
    }

    fun stop() {
        // v1.10.21: simpan posisi-lanjut SEBELUM state dibersihkan.
        saveResumeForCurrent()
        generation++
        cancelFade()
        fadeScale = 1f
        cancelEmbedWatchdog()
        // v1.10.15: rate koreksi Jam tidak boleh bocor ke pemutaran
        // berikutnya — lagu baru selalu mulai 1.0×.
        setPlaybackRate(1.0f)
        try {
            exoPlayer.stop()
        } catch (_: Exception) {
        }
        embed.stop()
        activeEngine = null
        _isPlaying.value = false
        _positionMs.value = 0
        _currentTrack.value = null
        // v1.0.2: stop melepas fokus + receiver dan mereset state duck.
        pausedByFocusLoss = false
        focusDucked = false
        focusDuckScale = 1f
        releasePlaybackFocus()
        stopService()
    }

    fun clearError() {
        _error.value = null
    }

    fun release() {
        try {
            exoPlayer.release()
        } catch (_: Exception) {
        }
        embed.destroy()
    }

    // ---- internal ----

    private fun startTrack(track: Track) {
        generation++
        val gen = generation
        cancelEmbedWatchdog()
        resolveRetryTrackId = null
        // v1.10.15: lagu baru selalu mulai 1.0× — rate koreksi drift
        // Jam milik lagu SEBELUMNYA, tidak boleh terbawa.
        // v1.0.5: rate awal = rate efektif user (per-episode/default),
        // kecuali selama Jam (rate milik sesi → 1.0×, drift yang mengatur).
        setPlaybackRate(
            PlaybackSpeedPolicy.effectiveStartRate(
                jamActive = jamActiveProvider(),
                perEpisodeRate = if (PlaybackSpeedPolicy.isLongForm(track.durationMs)) {
                    store.episodeSpeed(track.id)
                } else {
                    null
                },
                defaultRate = _userPlaybackRate.value,
            ),
        )
        // v1.10.21: timer simpan posisi-lanjut mulai lagi per lagu.
        lastResumeSaveAtMs = 0L
        _error.value = null
        _currentTrack.value = track
        _positionMs.value = 0
        _durationMs.value = track.durationMs
        _isBuffering.value = true
        // v1.0.3: info lagu baru = info lagu baru — jejak resolve,
        // stream, segmen, dan undo lompatan lagu sebelumnya dibuang.
        _resolveTrace.value = ResolveTrace.EMPTY
        _currentStream.value = null
        _segments.value = emptyList()
        clearSkipUndo()
        // v1.0.3: segmen SponsorBlock di-fetch best-effort per lagu
        // (hanya bila toggle nyala + ada videoId); kosong = tidak
        // ada perilaku skip. Fetch berjalan paralel dengan resolve.
        val videoIdForSegments = track.youtubeVideoId?.takeIf { it.isNotBlank() }
        if (store.skipSponsoredSegments && videoIdForSegments != null) {
            scope.launch {
                val segs = sponsorBlockApi.fetchSegments(videoIdForSegments)
                mainHandler.post {
                    if (gen == generation && segs.isNotEmpty()) {
                        _segments.value = segs
                    }
                }
            }
        }
        // "Terakhir diputar" (Koleksi, v1.7): catat lagu yang
        // benar-benar mulai diputar — terbaru dulu, maks 20.
        runCatching { store.addRecentTrack(track) }
        // v1.10.16: hitungan putar untuk "Most played" — dicatat di
        // titik yang sama (lagu benar-benar mulai diputar), lokal
        // saja, tidak disinkron.
        runCatching { store.recordPlay(track) }
        // v1.0.6: peristiwa putar bertimestamp untuk statistik "Your
        // Year" — titik yang sama dengan recordPlay (lagu benar-benar
        // mulai diputar), lokal saja, tidak disinkron.
        runCatching {
            store.recordPlayEvent(
                PlayEvent(
                    trackId = track.id,
                    atMs = System.currentTimeMillis(),
                    durationMs = track.durationMs,
                    title = track.title,
                    artist = track.artist,
                    imageUrl = track.imageUrl,
                ),
            )
        }
        // Kualitas audio dari Pengaturan dipakai saat resolve stream.
        ytmApi.audioQuality = store.audioQuality
        // v1.10.3: track EXOPLAYER TANPA streamUrl (antrean pulihan,
        // "Coba lagi", riwayat) HARUS di-resolve dulu — playExo langsung
        // hanya menghasilkan error "Tidak ada stream untuk lagu ini"
        // tanpa pernah mencoba resolve. Regresi v1.10.1: restoreQueue
        // membuang streamUrl tersimpan tapi mempertahankan engine
        // EXOPLAYER, sehingga SEMUA lagu antrean pulihan gagal total.
        // Track lokal/unduhan (streamUrl content:// atau file://) tidak
        // kena: URL-nya tidak kosong, jadi tetap langsung ke playExo.
        val needsResolve = track.engine == Engine.YOUTUBE ||
            (track.streamUrl.isNullOrBlank() && !track.youtubeVideoId.isNullOrEmpty())
        if (needsResolve) {
            // Track YTM: ambil stream langsung dulu (ExoPlayer).
            playYtmTrack(track, gen)
        } else {
            playExo(track)
        }
    }

    /** Track YTM: stream langsung (visionOS) → ExoPlayer; gagal → embed. */
    private fun playYtmTrack(track: Track, gen: Int) {
        val videoId = track.youtubeVideoId
        if (videoId.isNullOrEmpty()) {
            _error.value = "Unknown YouTube video for this song"
            _isBuffering.value = false
            return
        }
        // Kalau URL stream sudah pernah di-resolve, langsung pakai.
        val knownUrl = track.streamUrl
        if (!knownUrl.isNullOrBlank()) {
            playExo(track)
            return
        }
        // v1.10.15: stream hasil prefetch tamu Jam (masih segar) dipakai
        // tanpa resolve ulang — tamu tidak menunggu resolve saat lagu
        // berganti. Cache dikonsumsi sekali pakai; kedaluwarsa/basi →
        // jatuh ke resolve normal di bawah (jangan pernah putar URL
        // basi sampai error).
        val prefetched = prefetchCache.take(videoId, System.currentTimeMillis())
        if (prefetched != null) {
            val resolved = track.copy(
                engine = Engine.EXOPLAYER,
                streamUrl = prefetched.url,
                durationMs = if (prefetched.durationMs > 0) prefetched.durationMs else track.durationMs,
            )
            replaceCurrentTrack(resolved)
            playExo(resolved)
            return
        }
        scope.launch {
            // v1.10.35: single-flight — bila prefetch tamu sedang
            // me-resolve lagu ini, jalur putar bergabung ke resolve
            // yang sama (bukan rantai kedua); bila tidak, resolve
            // dimulai di sini seperti dulu.
            // v1.0.3: varian detailed — jejak langkahnya disimpan
            // untuk sheet Track info (sukses maupun gagal).
            val outcome = sharedResolveDetailed(videoId).await()
            val result = outcome.result
            mainHandler.post {
                if (gen != generation) return@post
                _resolveTrace.value = outcome.trace
                val stream = result.getOrNull()
                if (stream != null) {
                    _currentStream.value = stream
                    val resolved = track.copy(
                        engine = Engine.EXOPLAYER,
                        streamUrl = stream.url,
                        durationMs = if (stream.durationMs > 0) stream.durationMs else track.durationMs,
                        isLive = stream.isLive || track.isLive,
                    )
                    replaceCurrentTrack(resolved)
                    playExo(resolved)
                } else {
                    // Fallback terakhir: player resmi YouTube (embed).
                    // Alasan resolve diteruskan — kalau embed juga
                    // gagal, yang tampil alasan aslinya, bukan
                    // pesan generik watchdog.
                    _error.value = null
                    playEmbed(
                        track,
                        gen,
                        result.exceptionOrNull()?.message,
                    )
                }
            }
        }
    }

    private fun replaceCurrentTrack(track: Track) {
        val list = _queue.value.toMutableList()
        val idx = _currentIndex.value
        // v1.0.3: tulis ke antrean HANYA bila lagu di indeks itu
        // memang lagu yang sedang diputar. Lagu radio tidak ada di
        // antrean user — tanpa guard ini, resolve lagu radio akan
        // menimpa lagu user di posisi terakhir (korupsi antrean).
        if (idx in list.indices && list[idx].id == track.id) {
            list[idx] = track
            _queue.value = list
            saveQueue()
        }
        _currentTrack.value = track
        _durationMs.value = track.durationMs
    }

    /** Fallback: putar lewat IFrame YouTube di WebView (audio saja). */
    private fun playEmbed(track: Track, gen: Int, resolveReason: String? = null) {
        val id = track.youtubeVideoId
        if (id.isNullOrEmpty()) {
            _error.value = "Unknown YouTube video for this song"
            _isBuffering.value = false
            return
        }
        activeEngine = Engine.YOUTUBE
        _isPlaying.value = false // jangan klaim bunyi sebelum embed melapor
        acquirePlaybackFocus()
        embed.loadYouTube(id)
        startService()
        // Watchdog: embed yang tidak pernah melapor dalam 12 detik
        // dianggap gagal (v1.0 diam saja di titik ini).
        val watchdog = Runnable {
            if (gen == generation && activeEngine == Engine.YOUTUBE && !_isPlaying.value) {
                _isBuffering.value = false
                _error.value = resolveReason
                    ?: "The YouTube player isn't responding. Check your internet, then try again."
            }
        }
        embedWatchdog = watchdog
        mainHandler.postDelayed(watchdog, 12_000)
    }

    private fun cancelEmbedWatchdog() {
        embedWatchdog?.let { mainHandler.removeCallbacks(it) }
        embedWatchdog = null
    }

    private fun playExo(track: Track) {
        val url = track.streamUrl
        if (url.isNullOrEmpty()) {
            _error.value = "No stream for this song"
            _isBuffering.value = false
            return
        }
        activeEngine = Engine.EXOPLAYER
        acquirePlaybackFocus()
        // v1.0.5: durasi bisa baru diketahui SETELAH resolve (track
        // salinan hasil resolve membawa durationMs) — rate per-episode
        // diterapkan ulang di sini bila ada; default sudah diterapkan
        // di startTrack. Selama Jam tidak disentuh (rate milik sesi).
        if (!jamActiveProvider() && PlaybackSpeedPolicy.isLongForm(track.durationMs)) {
            store.episodeSpeed(track.id)?.let { setPlaybackRate(it) }
        }
        try {
            // v1.10.1: manifest HLS (live + fallback non-live) HARUS
            // dinyatakan mime-nya — URL googlevideo tanpa ekstensi
            // .m3u8 akan salah ditebak sebagai Progressive oleh
            // DefaultMediaSourceFactory dan gagal putar.
            val isHls = url.contains("manifest.googlevideo.com") ||
                url.endsWith(".m3u8", ignoreCase = true)
            val item = if (isHls) {
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            } else {
                MediaItem.fromUri(url)
            }
            exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
            // Fade masuk melembutkan awal lagu; volume dasar diterapkan
            // lewat applyVolume() (bukan tulis langsung) supaya skala
            // crossfade/fade tidak saling menimpa.
            startFadeIn()
            exoPlayer.play()
            _isPlaying.value = true
            // v1.10.21: lompat ke posisi tersimpan konten panjang.
            applyResumeSeek(track)
            startService()
        } catch (e: Exception) {
            _error.value = "Couldn't play: ${e.message}"
            _isBuffering.value = false
        }
    }

    private fun onTrackEnded() {
        // v1.10.21: lagu yang SELESAI diputar tidak punya posisi
        // lanjut — lain kali mulai dari 0.
        val endedTrack = _currentTrack.value
        endedTrack?.let { track ->
            runCatching { store.clearResumePosition(track.id) }
        }
        // v1.0.5: mode sleep "akhir lagu" — berhenti di sini (di luar
        // Jam saja), tanpa advance/repeat/autoplay. Sekali pakai.
        if (_sleepEndOfTrack.value && !jamActiveProvider()) {
            _sleepEndOfTrack.value = false
            sleepTimer.cancel()
            pause()
            return
        }
        if (_repeat.value == RepeatMode.ONE) {
            _currentTrack.value?.let { startTrack(it) }
        } else {
            // v1.10.34: beri tahu pendengar (Jam host) bahwa lagu
            // berakhir natural — SEBELUM advance, supaya pendengar
            // masih melihat lagu yang berakhir + antrean saat itu.
            // next() di bawah tetap berjalan seperti biasa (antrean
            // tak kosong = lagu berikutnya yang menang; hook tidak
            // boleh menghentikannya).
            endedTrack?.let { track -> runCatching { onTrackEndedNatural?.invoke(track) } }
            next(manual = false)
        }
    }

    private fun startExoPolling() {
        val r = object : Runnable {
            override fun run() {
                try {
                    if (activeEngine == Engine.EXOPLAYER) {
                        _positionMs.value = exoPlayer.currentPosition
                        val d = exoPlayer.duration
                        if (d > 0) _durationMs.value = d
                        // v1.10.21: simpan posisi-lanjut berkala
                        // (konten panjang, bukan Jam).
                        val nowMs = System.currentTimeMillis()
                        if (_isPlaying.value && !jamActiveProvider() &&
                            nowMs - lastResumeSaveAtMs >= RESUME_SAVE_INTERVAL_MS
                        ) {
                            lastResumeSaveAtMs = nowMs
                            saveResumeForCurrent()
                        }
                        // v1.0.3 SponsorBlock: posisi masuk segmen
                        // yang harus dilewati → lompat ke ujungnya
                        // + tampilkan chip Undo 6 detik. Hanya bila
                        // toggle nyala dan segmen benar-benar ada;
                        // lompatan tidak pernah mundur.
                        if (store.skipSponsoredSegments && _segments.value.isNotEmpty()) {
                            val posSec = exoPlayer.currentPosition / 1000.0
                            val targetSec = decideSkip(posSec, _segments.value)
                            if (targetSec != null) {
                                val targetMs = (targetSec * 1000).toLong()
                                val curTrack = _currentTrack.value
                                if (curTrack != null && targetMs > exoPlayer.currentPosition) {
                                    val undo = SkipUndo(
                                        trackId = curTrack.id,
                                        skippedFromSec = posSec,
                                        expiresAtMs = nowMs + 6_000,
                                    )
                                    _skipUndo.value = undo
                                    skipUndoTimeout?.let { mainHandler.removeCallbacks(it) }
                                    val timeout = Runnable {
                                        if (_skipUndo.value == undo) _skipUndo.value = null
                                        skipUndoTimeout = null
                                    }
                                    skipUndoTimeout = timeout
                                    mainHandler.postDelayed(timeout, 6_000)
                                    try {
                                        exoPlayer.seekTo(targetMs)
                                    } catch (_: Exception) {
                                    }
                                    _positionMs.value = targetMs
                                }
                            }
                        }
                        // Crossfade (fade keluar): di N detik terakhir,
                        // volume memudar. Gapless ON = tanpa fade.
                        // Skala crossfade dikalikan dengan envelope fade
                        // di applyVolume() — keduanya hidup berdampingan.
                        var crossScale = 1f
                        val fadeSec = store.crossfadeSeconds
                        if (fadeSec > 0 && !store.gapless && d > 0) {
                            val remaining = d - exoPlayer.currentPosition
                            val window = fadeSec * 1000L
                            if (remaining in 1 until window) {
                                crossScale = (remaining.toFloat() / window).coerceIn(0.15f, 1f)
                            }
                        }
                        applyVolume(crossScale)
                    }
                } catch (_: Exception) {
                }
                mainHandler.postDelayed(this, 100)
            }
        }
        mainHandler.postDelayed(r, 100)
    }

    /**
     * Posisi-lanjut (v1.10.21): simpan posisi lagu berjalan bila
     * aturannya mengizinkan; posisi yang tidak layak disimpan
     * (terlalu awal / hampir habis / durasi pendek) justru
     * menghapus posisi tersimpan. Durasi belum diketahui = lewati
     * (jangan hapus posisi lama atas dasar durasi 0).
     */
    private fun saveResumeForCurrent() {
        if (jamActiveProvider()) return
        // Hanya jalur ExoPlayer yang posisinya bisa dibaca — posisi
        // ExoPlayer basi saat engine embed sedang bunyi, dan
        // menyimpannya akan menghapus posisi yang benar.
        if (activeEngine != Engine.EXOPLAYER) return
        val track = _currentTrack.value ?: return
        val pos = try {
            exoPlayer.currentPosition
        } catch (_: Exception) {
            _positionMs.value
        }
        val dur = _durationMs.value.takeIf { it > 0 } ?: track.durationMs
        if (dur <= 0L) return
        val toStore = positionToStore(pos, dur)
        runCatching {
            if (toStore != null) {
                // v1.0.5: cuplikan ikut disimpan untuk rak Keep listening.
                store.saveResumeSnapshot(track, toStore, System.currentTimeMillis())
            } else {
                store.clearResumePosition(track.id)
            }
        }
    }

    /**
     * Lompat ke posisi tersimpan saat lagu panjang mulai berbunyi
     * (jalur ExoPlayer; embed tidak punya posisi yang bisa
     * diandalkan). Langsung ke ExoPlayer — BUKAN lewat [seekTo]
     * supaya tidak memicu aturan hapus posisi.
     */
    private fun applyResumeSeek(track: Track) {
        if (jamActiveProvider()) return
        val duration = track.durationMs.takeIf { it > 0 } ?: return
        val saved = store.resumePosition(track.id)
        val resumeAt = resumePositionFor(saved, duration) ?: return
        try {
            exoPlayer.seekTo(resumeAt)
        } catch (_: Exception) {
            return
        }
        _positionMs.value = resumeAt
    }

    private fun startService() {
        try {
            val intent = Intent(appContext, PlaybackService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
        } catch (e: Exception) {
            // v1.0.2: kegagalan start (mis. pembatasan FGS Android 12+
            // saat start dari background) TIDAK boleh hilang diam-diam —
            // dicatat ke logcat; PlaybackService sendiri akan mencoba
            // lagi saat start berikutnya (started tetap false).
            Log.w("PlayerManager", "startForegroundService failed: ${e.message}", e)
        }
    }

    private fun stopService() {
        try {
            appContext.stopService(Intent(appContext, PlaybackService::class.java))
        } catch (_: Exception) {
        }
    }

    // ---- persist antrean (JSON minimal) ----

    private fun saveQueue() {
        try {
            val prefs = appContext.getSharedPreferences("sonara", Context.MODE_PRIVATE)
            val arr = JSONArray()
            for (t in _queue.value) {
                val o = JSONObject()
                o.put("id", t.id)
                o.put("title", t.title)
                o.put("artist", t.artist)
                o.put("album", t.album)
                o.put("source", t.source.name)
                o.put("engine", t.engine.name)
                o.put("durationMs", t.durationMs)
                o.put("streamUrl", t.streamUrl ?: "")
                o.put("youtubeVideoId", t.youtubeVideoId ?: "")
                o.put("imageUrl", t.imageUrl ?: "")
                o.put("explicit", t.explicit)
                arr.put(o)
            }
            prefs.edit().putString("last_queue_json", arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun restoreQueue() {
        try {
            val prefs = appContext.getSharedPreferences("sonara", Context.MODE_PRIVATE)
            val raw = prefs.getString("last_queue_json", null) ?: return
            val arr = JSONArray(raw)
            val list = ArrayList<Track>()
            for (i in 0 until arr.length()) {
                try {
                    val o = arr.getJSONObject(i)
                    // Migrasi v1.1: track dari sumber yang sudah tidak ada
                    // dibuang bersih (satu item rusak tidak boleh
                    // menggagalkan seluruh antrean).
                    val source = try {
                        Source.valueOf(o.optString("source", "YOUTUBE_MUSIC"))
                    } catch (_: IllegalArgumentException) {
                        continue
                    }
                    val engine = try {
                        Engine.valueOf(o.optString("engine", "EXOPLAYER"))
                    } catch (_: IllegalArgumentException) {
                        Engine.EXOPLAYER
                    }
                    // Track YTM lama (engine YOUTUBE tanpa
                    // streamUrl) tetap bisa diputar — stream di-resolve
                    // ulang saat diputar.
                    // v1.10.1: streamUrl tersimpan SENGAJA dibuang —
                    // URL stream YouTube kedaluwarsa (~6 jam), dan URL
                    // basi adalah penyebab klasik error putar instan
                    // ("source error") saat app dibuka kembali.
                    // v1.10.3: HANYA URL http(s) YouTube yang dibuang.
                    // URI lokal (content:// lagu lokal, file://
                    // unduhan) TIDAK kedaluwarsa dan HARUS dipertahankan
                    // — membuangnya merusak pemutaran lokal/unduhan
                    // setelah restart. Dan engine dinormalisasi ke
                    // YOUTUBE bila URL dibuang tapi videoId ada, supaya
                    // track pulihan selalu lewat jalur resolve
                    // (pertahanan kedua di samping guard startTrack).
                    val savedUrl = o.optString("streamUrl")
                        .takeIf { it.isNotEmpty() }
                    val keptUrl = savedUrl?.takeIf {
                        it.startsWith("content://") || it.startsWith("file://")
                    }
                    val videoId = o.optString("youtubeVideoId")
                        .takeIf { it.isNotEmpty() }
                    val restoredEngine =
                        if (keptUrl == null && videoId != null) Engine.YOUTUBE
                        else engine
                    list.add(
                        Track(
                            id = o.optString("id"),
                            title = o.optString("title"),
                            artist = o.optString("artist"),
                            album = o.optString("album"),
                            source = source,
                            engine = restoredEngine,
                            durationMs = o.optLong("durationMs", 0),
                            streamUrl = keptUrl,
                            youtubeVideoId = videoId,
                            imageUrl = o.optString("imageUrl").takeIf { it.isNotEmpty() },
                            explicit = o.optBoolean("explicit", false),
                        ),
                    )
                } catch (_: Exception) {
                    continue
                }
            }
            if (list.isNotEmpty()) {
                _queue.value = list
                _currentIndex.value = 0
                _currentTrack.value = list[0]
                _durationMs.value = list[0].durationMs
            }
        } catch (_: Exception) {
        }
    }
}
