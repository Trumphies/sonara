package com.sonara.app.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

/** Status sinkron untuk UI (Koleksi + Pengaturan). */
data class SyncUiState(
    val configured: Boolean = SyncConfig.isConfigured,
    val signedIn: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val lastSyncedAtMs: Long? = null,
    val pending: Boolean = false,
    val error: String? = null,
)

/** Kredensial Jam: identitas + ID token Firebase (JANGAN pernah di-log). */
data class JamCredentials(
    val uid: String,
    val displayName: String,
    val photoUrl: String,
    val idToken: String,
)

/**
 * Kredensial Jam tidak bisa dipulihkan: refresh token ditolak
 * definitif oleh server atau tidak ada refresh token tersimpan.
 * Satu-satunya kondisi yang boleh menampilkan "Session expired".
 */
class JamAuthInvalid : Exception("Jam credentials invalid")

/**
 * Kegagalan TRANSIEN saat memulihkan kredensial Jam (jaringan /
 * server). Sesi tersimpan UTUH — pemanggil DILARANG menghapus sesi
 * atau menampilkan pesan session-expired.
 */
class JamAuthTransient : Exception("Jam credentials refresh failed transiently")

enum class LibraryChangeKind { LIKED, PLAYLIST, RECENT }

/**
 * Login Google + sinkron cloud (Firebase Auth + Firestore via REST,
 * tanpa Firebase SDK). Local-first: data lokal tetap sumber
 * kebenaran di perangkat; cloud = salinan yang diselaraskan.
 *
 * - Login pertama: data lokal di-push dulu ke cloud (bukan ditimpa
 *   data kosong), lalu pull.
 * - Setelahnya: pull dulu (merge last-write-wins per dokumen),
 *   lalu push. Hapus eksplisit (unlike) tercatat sebagai tombstone
 *   supaya ikut terhapus di cloud dan tidak bangkit lagi saat pull.
 * - Logout: data lokal TETAP di perangkat; sesi + token dihapus.
 * - Semua token disimpan terenkripsi (SecurePrefs, pola PKCE dulu);
 *   tidak pernah di-log.
 */
class SyncManager(
    private val context: Context,
    private val store: LocalStore,
    private val rest: FirebaseRest = FirebaseRest(
        SonaraHttp.defaultHttp(),
        SyncConfig.FIREBASE_API_KEY,
        SyncConfig.FIREBASE_PROJECT_ID,
    ),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secure = store.secure

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    /** True saat menerapkan data dari cloud — callback perubahan lokal dibisukan. */
    @Volatile
    private var applyingRemote = false
    private var pushJob: Job? = null

    // Auto-sync 1 menit (v1.10.6): loop foreground di AutoSyncLoop;
    // tick-nya memanggil syncPeriodic (tenang saat sukses, toast
    // ramah + dedupe saat gagal). Pemanggil (SonaraApp) start saat
    // ON_START dan stop saat ON_STOP — tidak ada WorkManager /
    // foreground service (lihat BUILD-v1.10.6.md, batasan platform).
    private val autoSyncLoop = AutoSyncLoop { syncPeriodic() }
    private var lastPeriodicErrorToastMs = 0L

    /** Mulai loop auto-sync 1 menit (idempotent). */
    fun startPeriodicSync() = autoSyncLoop.start(scope)

    /** Hentikan loop auto-sync (idempotent). */
    fun stopPeriodicSync() = autoSyncLoop.stop()

    /**
     * Satu putaran sinkron berkala: tenang saat sukses (tidak ada
     * toast/status — kartu sinkron sudah dihapus), gagal -> toast
     * ramah yang didedupe (maks. 1x per 60 detik) supaya sesi
     * kedaluwarsa tidak men-toast tiap menit. No-op bila belum
     * login / belum dikonfigurasi.
     */
    fun syncPeriodic(onMessage: (String) -> Unit = {}) {
        scope.launch {
            try {
                syncNowInternal()
            } catch (e: SessionInvalid) {
                clearSessionOnly()
                _uiState.value = SyncUiState(configured = true, signedIn = false)
                emitMessageDeduped(onMessage, "Session expired, sign in again.")
            } catch (e: SessionTransient) {
                // v1.10.7: sesi TIDAK dihapus; toast ramah yang
                // didedupe, percobaan berikutnya mengulang refresh.
                setError("No internet — try again later.")
                emitMessageDeduped(onMessage, "Sync failed — try again later.")
            } catch (e: Exception) {
                setError(friendlySyncError(e))
                emitMessageDeduped(onMessage, "Sync failed — try again later.")
            }
        }
    }

    private suspend fun emitMessageDeduped(onMessage: (String) -> Unit, message: String) {
        val now = System.currentTimeMillis()
        if (now - lastPeriodicErrorToastMs < 60_000L) return
        lastPeriodicErrorToastMs = now
        emitMessage(onMessage, message)
    }

    /**
     * Callback pesan UI (toast) SELALU di main thread (v1.10.1).
     * Scope SyncManager jalan di Dispatchers.IO — memanggil onMessage
     * langsung dari sana membuat Toast crash (laporan v1.10).
     */
    private suspend fun emitMessage(onMessage: (String) -> Unit, msg: String) {
        withContext(Dispatchers.Main) { onMessage(msg) }
    }

    /**
     * Sesi Firebase di memori (ID token + kedaluwarsa). Refresh token
     * di SecurePrefs. Sumber kebenaran = SecurePrefs: keputusan
     * berlaku/tidaknya token membaca KEY_ID_TOKEN/KEY_ID_EXP dari
     * sana (bukan hanya field memori), supaya panggilan yang datang
     * sebelum restoreSession selesai pun bisa memulihkan sesi.
     */
    @Volatile
    private var session: FirebaseRest.FirebaseSession? = null

    /**
     * Sesi TIDAK BISA dipulihkan (refresh token ditolak definitif
     * oleh server, atau tidak ada refresh token tersimpan).
     * HANYA error ini yang boleh menghapus sesi tersimpan dan
     * menampilkan "Session expired, sign in again."
     */
    private class SessionInvalid : Exception("Session invalid")

    /**
     * Kegagalan TRANSIEN (jaringan/server) saat memulihkan sesi.
     * Sesi TETAP tersimpan — pemanggil DILARANG menghapusnya atau
     * menampilkan pesan session-expired; percobaan berikutnya
     * (tick auto-sync / foreground / aksi Jam) akan mencoba lagi.
     */
    private class SessionTransient : Exception("Session refresh failed transiently")

    /** Pengunci single-flight refresh token (blokir, di Dispatchers.IO). */
    private val tokenLock = Any()

    /** Margin kedaluwarsa ID token: refresh bila sisa < 5 menit (v1.10.7, dulu 60 dtk). */
    private val tokenRefreshMarginMs = 300_000L

    init {
        store.onLibraryChanged = { kind, changedIds, removedIds ->
            onLocalChange(kind, changedIds, removedIds)
        }
        restoreSession()
    }

    // ---- Google Sign-In ----------------------------------------------

    /** Intent login Google, atau null bila sinkron belum dikonfigurasi. */
    fun googleSignInIntent(): Intent? {
        if (!SyncConfig.isConfigured) return null
        return try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(SyncConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso).signInIntent
        } catch (e: Exception) {
            setError("Google sign-in isn't available on this device.")
            null
        }
    }

    /** Hasil dari launcher login — tukar ID token Google → sesi Firebase, lalu sinkron. */
    fun handleGoogleSignInResult(data: Intent?, onMessage: (String) -> Unit) {
        if (data == null) {
            // Callback launcher jalan di main thread — langsung aman.
            onMessage("Sign-in cancelled.")
            return
        }
        scope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                val googleIdToken = account.idToken
                    ?: throw IllegalStateException("Couldn't get a Google token.")
                val fresh = rest.signInWithGoogleIdToken(googleIdToken)
                // Akun berbeda dari sesi sinkron sebelumnya → buku
                // sync lama tidak berlaku; mulai dari sinkron pertama.
                if (store.syncAccountUid() != null && store.syncAccountUid() != fresh.uid) {
                    store.saveSyncMeta(JSONObject())
                    store.saveSyncTombstones(JSONObject())
                    store.setLastSyncAtMs(0L)
                }
                store.setSyncAccountUid(fresh.uid)
                // v1.10.4: login baru = identitas baru. Bersihkan
                // nama/email/foto tersimpan DULU — persistSession
                // hanya menimpa nilai non-null, jadi tanpa ini akun
                // lain bisa mewarisi nama/foto akun sebelumnya bila
                // field barunya null.
                secure.put(KEY_NAME, null)
                secure.put(KEY_EMAIL, null)
                secure.put(KEY_PHOTO, null)
                persistSession(fresh, account.displayName, account.email, account.photoUrl?.toString())
                setSignedIn()
                syncNowInternal()
                emitMessage(onMessage, "Signed in as ${account.displayName ?: account.email ?: "Google"}.")
            } catch (e: ApiException) {
                emitMessage(onMessage, googleErrorMessage(e))
            } catch (e: Exception) {
                val friendly = friendlyLoginError(e)
                setError(friendly)
                emitMessage(onMessage, friendly)
            }
        }
    }

    private fun googleErrorMessage(e: ApiException): String = when (e.statusCode) {
        12501 -> "Sign-in cancelled."
        7 -> "No internet — try again later."
        10 -> "Sign-in isn't configured correctly (check the Web Client ID / SHA-1 in Firebase)."
        12500 -> "Sign-in failed — try again."
        else -> "Sign-in failed (${e.statusCode})."
    }

    // ---- Sesi ---------------------------------------------------------

    private fun restoreSession() {
        if (!SyncConfig.isConfigured) return
        if (secure.get(KEY_REFRESH) == null) return
        scope.launch {
            try {
                ensureIdToken()
                setSignedIn()
                syncNowInternal()
            } catch (e: SessionInvalid) {
                // Refresh token ditolak definitif — sesi memang
                // tidak bisa dipulihkan. Hapus diam-diam, data
                // lokal tetap. Bukan error yang perlu diumumkan.
                clearSessionOnly()
                _uiState.value = SyncUiState(configured = true, signedIn = false)
            } catch (_: Exception) {
                // v1.10.7 (AKAR MASALAH #1): kegagalan TRANSIEN
                // (offline/slow saat cold start) DULU masuk ke
                // catch-all ini dan MENGHAPUS sesi tersimpan, jadi
                // user harus login ulang setiap update. Sekarang
                // kredensial TETAP tersimpan; UI tampil logout
                // sampai sinkron berikutnya berhasil me-refresh.
                _uiState.value = SyncUiState(configured = true, signedIn = false)
            }
        }
    }

    private fun persistSession(
        fresh: FirebaseRest.FirebaseSession,
        displayName: String?,
        email: String?,
        photoUrl: String?,
    ) {
        session = fresh
        secure.put(KEY_REFRESH, fresh.refreshToken)
        secure.put(KEY_ID_TOKEN, fresh.idToken)
        secure.put(KEY_ID_EXP, (System.currentTimeMillis() + fresh.expiresInSec * 1000L).toString())
        secure.put(KEY_UID, fresh.uid)
        if (displayName != null) secure.put(KEY_NAME, displayName)
        if (email != null) secure.put(KEY_EMAIL, email)
        if (photoUrl != null) secure.put(KEY_PHOTO, photoUrl)
    }

    /**
     * Status login dari identitas tersimpan di SecurePrefs (nama /
     * email / foto + waktu sinkron terakhir). Dipanggil setelah
     * sesi dipulihkan — tidak bergantung pada objek sesi memori.
     */
    private fun setSignedIn() {
        _uiState.value = SyncUiState(
            configured = true,
            signedIn = true,
            displayName = secure.get(KEY_NAME).orEmpty(),
            email = secure.get(KEY_EMAIL).orEmpty(),
            photoUrl = secure.get(KEY_PHOTO).orEmpty(),
            lastSyncedAtMs = store.lastSyncAtMs().takeIf { it > 0 },
            pending = false,
            error = null,
        )
    }

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message, pending = false)
    }

    /**
     * Pesan error sinkron yang RAMAH untuk user (v1.10.2, direvisi
     * v1.10.7): tidak pernah berisi JSON mentah / body HTTP dari
     * server. Detail teknis hanya ditulis ke logcat. Berlaku untuk
     * SEMUA kegagalan sinkron — login, sync, hapus data cloud.
     *
     * v1.10.7: HTTP 401/403 TIDAK LAGI otomatis berarti sesi
     * kedaluwarsa. 401 ditangani retry-then-refresh di
     * syncNowInternal; 403 (rules) tidak pernah menghapus sesi.
     * Hanya [SessionInvalid] yang berarti "Session expired".
     */
    private fun friendlySyncError(e: Exception): String {
        Log.w("SyncManager", "Sync failed: ${e.message}", e)
        val msg = e.message.orEmpty()
        return when {
            e is SessionInvalid -> "Session expired, sign in again."
            e is SessionTransient || e is java.io.IOException ->
                "No internet — try again later."
            msg.contains("HTTP 400") ->
                "Sync failed — data couldn't be sent. Try again later."
            else -> "Sync failed — try again later."
        }
    }

    private fun friendlyLoginError(e: Exception): String {
        Log.w("SyncManager", "Sign-in failed: ${e.message}", e)
        return if (e is java.io.IOException) {
            "No internet — try again later."
        } else {
            "Sign-in failed — try again."
        }
    }

    /**
     * ID token Firebase yang masih berlaku; perbarui diam-diam bila
     * hampir kedaluwarsa (margin 5 menit) atau [forceRefresh].
     *
     * Sumber kebenaran = SecurePrefs (bukan field memori), dan
     * refresh di-single-flight lewat [tokenLock] supaya dua
     * pemanggil bersamaan tidak menukar refresh token dua kali.
     *
     * Lempar [SessionInvalid] hanya bila refresh token tidak ada /
     * ditolak definitif; lempar [SessionTransient] untuk gagal
     * jaringan/server — pemanggil DILARANG menghapus sesi untuk
     * yang kedua. Tidak pernah me-log token.
     */
    private fun ensureIdToken(forceRefresh: Boolean = false): String {
        synchronized(tokenLock) {
            val now = System.currentTimeMillis()
            if (!forceRefresh) {
                val storedToken = secure.get(KEY_ID_TOKEN)
                val expMs = secure.get(KEY_ID_EXP)?.toLongOrNull() ?: 0L
                if (!storedToken.isNullOrEmpty() && expMs > now + tokenRefreshMarginMs) {
                    return storedToken
                }
            }
            val refreshToken = secure.get(KEY_REFRESH) ?: throw SessionInvalid()
            val fresh = try {
                rest.refreshIdToken(refreshToken)
            } catch (e: FirebaseRest.RefreshTokenInvalid) {
                throw SessionInvalid()
            } catch (e: Exception) {
                throw SessionTransient()
            }
            if (fresh.idToken.isEmpty()) {
                throw SessionTransient()
            }
            // v1.10.8: respons refresh bisa tidak membawa
            // refresh_token baru (tidak dirotasi) — pertahankan
            // refresh token lama; JANGAN gagalkan refresh yang
            // sudah menghasilkan ID token valid.
            val effective = if (fresh.refreshToken.isEmpty()) {
                fresh.copy(refreshToken = refreshToken)
            } else {
                fresh
            }
            persistSession(effective, null, null, null)
            return effective.idToken
        }
    }

    /**
     * Kredensial untuk Jamming Together (v1.10.6, HANYA tambahan —
     * logika sesi/sinkron di atas tidak berubah): uid + nama + foto
     * + ID token Firebase yang masih berlaku. Dipakai JamSessionManager
     * untuk otorisasi REST Realtime Database (`?auth=`).
     *
     * v1.10.8: melempar [JamAuthInvalid] (refresh ditolak definitif /
     * tidak ada refresh token) atau [JamAuthTransient] (gagal
     * jaringan/server) — pemanggil WAJIB membedakannya: hanya
     * Invalid yang boleh menampilkan "Session expired". Fungsi ini
     * MEMBLOKIR (I/O jaringan) — hanya panggil dari Dispatcher.IO.
     * Token TIDAK PERNAH di-log; pemanggil juga dilarang me-log-nya.
     */
    fun jamCredentialsOrThrow(forceRefresh: Boolean = false): JamCredentials {
        val uid = secure.get(KEY_UID) ?: throw JamAuthInvalid()
        return try {
            val token = ensureIdToken(forceRefresh = forceRefresh)
            JamCredentials(
                uid = uid,
                displayName = secure.get(KEY_NAME).orEmpty(),
                photoUrl = secure.get(KEY_PHOTO).orEmpty(),
                idToken = token,
            )
        } catch (e: SessionInvalid) {
            throw JamAuthInvalid()
        } catch (_: Exception) {
            throw JamAuthTransient()
        }
    }

    fun signOut() {
        scope.launch {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (_: Exception) {
            }
            clearSessionOnly()
            _uiState.value = SyncUiState(configured = SyncConfig.isConfigured, signedIn = false)
        }
    }

    private fun clearSessionOnly() {
        pushJob?.cancel()
        session = null
        secure.put(KEY_REFRESH, null)
        secure.put(KEY_ID_TOKEN, null)
        secure.put(KEY_ID_EXP, null)
        secure.put(KEY_UID, null)
        secure.put(KEY_NAME, null)
        secure.put(KEY_EMAIL, null)
        secure.put(KEY_PHOTO, null)
    }

    // ---- Perubahan lokal → push tertunda ------------------------------

    private fun onLocalChange(kind: LibraryChangeKind, changedIds: List<String>, removedIds: List<String>) {
        if (applyingRemote) return
        val now = System.currentTimeMillis()
        val meta = store.syncMeta()
        val coll = when (kind) {
            LibraryChangeKind.LIKED -> "liked"
            LibraryChangeKind.PLAYLIST -> "playlists"
            LibraryChangeKind.RECENT -> "history"
        }
        for (id in changedIds) setMetaTs(meta, coll, docIdFor(id), now)
        store.saveSyncMeta(meta)
        // Hapus eksplisit dicatat sebagai tombstone supaya ikut
        // terhapus di cloud dan tidak bangkit lagi saat pull.
        if (removedIds.isNotEmpty() && (coll == "liked" || coll == "playlists")) {
            val tomb = store.syncTombstones()
            for (id in removedIds) {
                val did = docIdFor(id)
                val c = tomb.optJSONObject(coll) ?: JSONObject().also { tomb.put(coll, it) }
                c.put(did, now)
                removeMetaTs(meta, coll, did)
            }
            store.saveSyncTombstones(tomb)
            store.saveSyncMeta(meta)
        }
        if (_uiState.value.signedIn) {
            _uiState.value = _uiState.value.copy(pending = true, error = null)
            pushJob?.cancel()
            pushJob = scope.launch {
                delay(2000L)
                runCatching { syncNowInternal() }
            }
        }
    }

    /** Sinkron manual (tombol "Sinkronkan sekarang"). */
    fun syncNow(onMessage: (String) -> Unit = {}) {
        scope.launch {
            try {
                syncNowInternal()
                emitMessage(onMessage, "Synced.")
            } catch (e: SessionInvalid) {
                clearSessionOnly()
                _uiState.value = SyncUiState(configured = true, signedIn = false)
                emitMessage(onMessage, "Session expired, sign in again.")
            } catch (e: SessionTransient) {
                setError("No internet — try again later.")
                emitMessage(onMessage, "Sync failed — try again later.")
            } catch (e: Exception) {
                setError(friendlySyncError(e))
                emitMessage(onMessage, "Sync failed — try again later.")
            }
        }
    }

    /**
     * Satu putaran sinkron penuh dengan RETRY-ON-401 (v1.10.7,
     * AKAR MASALAH #2): ID token bisa kedaluwarsa DI TENGAH batch
     * pull+push (puluhan request di jaringan lambat). Bila server
     * menjawab 401, token di-refresh paksa SEKALI lalu seluruh
     * putaran diulang; 401 kedua = sesi benar-benar tidak berlaku
     * → [SessionInvalid]. 403 tidak pernah menghapus sesi.
     */
    private fun syncNowInternal() {
        if (!SyncConfig.isConfigured) return
        val uid = secure.get(KEY_UID) ?: return
        _uiState.value = _uiState.value.copy(pending = true, error = null)
        var attempt = 0
        while (true) {
            attempt++
            try {
                val idToken = ensureIdToken()
                runSyncWithToken(uid, idToken)
                return
            } catch (e: SessionInvalid) {
                clearSessionOnly()
                _uiState.value = SyncUiState(configured = true, signedIn = false)
                throw e
            } catch (e: SessionTransient) {
                setError("No internet — try again later.")
                throw e
            } catch (e: FirebaseRest.FirebaseException) {
                if (FirebaseRest.isHttpCode(e, 401) && attempt == 1) {
                    try {
                        ensureIdToken(forceRefresh = true)
                    } catch (re: SessionInvalid) {
                        clearSessionOnly()
                        _uiState.value = SyncUiState(configured = true, signedIn = false)
                        throw re
                    } catch (_: Exception) {
                        setError("No internet — try again later.")
                        throw SessionTransient()
                    }
                    continue
                }
                if (FirebaseRest.isHttpCode(e, 401)) {
                    clearSessionOnly()
                    _uiState.value = SyncUiState(configured = true, signedIn = false)
                    throw SessionInvalid()
                }
                setError(friendlySyncError(e))
                throw e
            } catch (e: Exception) {
                setError(friendlySyncError(e))
                throw e
            }
        }
    }

    /** Isi satu putaran sinkron (pull/push) dengan token tertentu. Idempotent — aman diulang. */
    private fun runSyncWithToken(uid: String, idToken: String) {
        val firstSync = store.lastSyncAtMs() == 0L
        if (firstSync) {
            // Login pertama: data lokal naik dulu, baru tarik.
            pushAll(idToken, uid)
            pullAll(idToken, uid)
        } else {
            pullAll(idToken, uid)
            pushAll(idToken, uid)
        }
        store.setLastSyncAtMs(System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(
            pending = false,
            error = null,
            lastSyncedAtMs = store.lastSyncAtMs(),
        )
    }

    // ---- Pull ----------------------------------------------------------

    private fun pullAll(idToken: String, uid: String) {
        applyingRemote = true
        try {
            pullPlaylists(idToken, uid)
            pullLiked(idToken, uid)
            pullHistory(idToken, uid)
        } finally {
            applyingRemote = false
        }
    }

    private fun pullPlaylists(idToken: String, uid: String) {
        val cloud = rest.listDocuments(uid, "playlists", idToken)
        val cloudById = cloud.associateBy { it.docId }
        val meta = store.syncMeta()
        val tomb = store.syncTombstones()
        val local = store.playlists().toMutableList()
        val localById = local.associateBy { it.id }
        val lastSync = store.lastSyncAtMs()
        for (doc in cloud) {
            val pid = doc.docId
            val cloudTs = FirebaseRest.fieldLong(doc.fields, "updatedAtMs")
            val existing = localById[pid]
            if (existing == null) {
                if (tombHas(tomb, "playlists", pid)) continue
                local.add(decodePlaylist(doc))
                setMetaTs(meta, "playlists", pid, cloudTs)
                cacheSnapshots(doc)
            } else if (cloudTs > metaTs(meta, "playlists", pid)) {
                val idx = local.indexOfFirst { it.id == pid }
                if (idx >= 0) local[idx] = decodePlaylist(doc)
                setMetaTs(meta, "playlists", pid, cloudTs)
                cacheSnapshots(doc)
            }
        }
        if (lastSync > 0) {
            // Hilang di cloud + pernah di-push + tidak lebih baru
            // dari sinkron terakhir + bukan tombstone → dihapus di
            // perangkat lain. v1.10.4: item yang BELUM PERNAH
            // di-push (metaTs == 0) TIDAK PERNAH dibuang di sini —
            // cloud kosong tidak boleh menghapus playlist lokal.
            val removed = local.filter { pl ->
                shouldDropOnPull(
                    metaTs(meta, "playlists", pl.id),
                    lastSync,
                    pl.id in cloudById,
                    tombHas(tomb, "playlists", pl.id),
                )
            }
            for (r in removed) {
                local.removeAll { it.id == r.id }
                removeMetaTs(meta, "playlists", r.id)
            }
        }
        store.savePlaylists(local)
        store.saveSyncMeta(meta)
    }

    private fun pullLiked(idToken: String, uid: String) {
        val cloud = rest.listDocuments(uid, "liked", idToken)
        val cloudById = cloud.associateBy { it.docId }
        val meta = store.syncMeta()
        val tomb = store.syncTombstones()
        val local = store.likedTracks().toMutableList()
        val localByDoc = local.associateBy { docIdFor(it.id) }
        val lastSync = store.lastSyncAtMs()
        for (doc in cloud) {
            val did = doc.docId
            val cloudTs = FirebaseRest.fieldLong(doc.fields, "updatedAtMs")
            val existing = localByDoc[did]
            if (existing == null) {
                if (tombHas(tomb, "liked", did)) continue
                local.add(snapshotToTrack(doc.fields))
                setMetaTs(meta, "liked", did, cloudTs)
            } else if (cloudTs > metaTs(meta, "liked", did)) {
                val idx = local.indexOfFirst { docIdFor(it.id) == did }
                if (idx >= 0) local[idx] = snapshotToTrack(doc.fields)
                setMetaTs(meta, "liked", did, cloudTs)
            }
        }
        if (lastSync > 0) {
            // Sama seperti playlist (v1.10.4): lagu disukai yang
            // belum pernah di-push tidak boleh dibuang oleh pull.
            val removed = local.filter { t ->
                val did = docIdFor(t.id)
                shouldDropOnPull(
                    metaTs(meta, "liked", did),
                    lastSync,
                    did in cloudById,
                    tombHas(tomb, "liked", did),
                )
            }
            for (r in removed) {
                local.removeAll { it.id == r.id }
                removeMetaTs(meta, "liked", docIdFor(r.id))
            }
        }
        store.replaceLikedTracks(local)
        store.saveSyncMeta(meta)
    }

    private fun pullHistory(idToken: String, uid: String) {
        val cloud = rest.listDocuments(uid, "history", idToken)
        val meta = store.syncMeta()
        // Gabung lokal + cloud per videoId, pakai playedAt terbaru.
        val merged = LinkedHashMap<String, Pair<Track, Long>>()
        for (t in store.recentTracks()) {
            val did = docIdFor(t.id)
            merged[did] = t to metaTs(meta, "history", did)
        }
        for (doc in cloud) {
            val did = doc.docId
            val playedAt = FirebaseRest.fieldLong(doc.fields, "playedAtMs")
            val cloudTs = FirebaseRest.fieldLong(doc.fields, "updatedAtMs")
            val track = snapshotToTrack(doc.fields)
            val prev = merged[did]
            if (prev == null || playedAt >= prev.second) {
                merged[did] = track to playedAt
            }
            setMetaTs(meta, "history", did, cloudTs)
        }
        val kept = merged.values.sortedByDescending { it.second }.take(20).map { it.first }
        store.setRecentTracks(kept)
        store.cacheKnownTracks(kept)
        store.saveSyncMeta(meta)
    }

    // ---- Push ----------------------------------------------------------

    private fun pushAll(idToken: String, uid: String) {
        pushPlaylists(idToken, uid)
        pushLiked(idToken, uid)
        pushHistory(idToken, uid)
        pushProfile(idToken, uid)
        // Tombstone yang sudah di-push dibersihkan.
        store.saveSyncTombstones(JSONObject())
    }

    private fun pushPlaylists(idToken: String, uid: String) {
        val meta = store.syncMeta()
        val resolver = knownTrackMap()
        val now = System.currentTimeMillis()
        for (pl in store.playlists()) {
            var ts = metaTs(meta, "playlists", pl.id)
            if (ts == 0L) {
                ts = now
                setMetaTs(meta, "playlists", pl.id, ts)
            }
            rest.upsertDocument(uid, "playlists", pl.id, encodePlaylist(pl, resolver, ts), idToken)
        }
        val tomb = store.syncTombstones()
        val removedIds = tomb.optJSONObject("playlists")?.keys()?.asSequence()?.toList().orEmpty()
        for (id in removedIds) {
            runCatching { rest.deleteDocument(uid, "playlists", id, idToken) }
        }
        store.saveSyncMeta(meta)
    }

    private fun pushLiked(idToken: String, uid: String) {
        val meta = store.syncMeta()
        val now = System.currentTimeMillis()
        for (t in store.likedTracks()) {
            val did = docIdFor(t.id)
            var ts = metaTs(meta, "liked", did)
            if (ts == 0L) {
                ts = now
                setMetaTs(meta, "liked", did, ts)
            }
            rest.upsertDocument(uid, "liked", did, encodeLiked(t, ts), idToken)
        }
        val tomb = store.syncTombstones()
        val removedIds = tomb.optJSONObject("liked")?.keys()?.asSequence()?.toList().orEmpty()
        for (id in removedIds) {
            runCatching { rest.deleteDocument(uid, "liked", id, idToken) }
        }
        store.saveSyncMeta(meta)
    }

    private fun pushHistory(idToken: String, uid: String) {
        val meta = store.syncMeta()
        val now = System.currentTimeMillis()
        val local = store.recentTracks()
        for (t in local) {
            val did = docIdFor(t.id)
            val playedAt = metaTs(meta, "history", did).takeIf { it > 0 } ?: now
            var ts = metaTs(meta, "history", did)
            if (ts == 0L) {
                ts = now
                setMetaTs(meta, "history", did, ts)
            }
            rest.upsertDocument(uid, "history", did, encodeHistory(t, playedAt, ts), idToken)
        }
        // Cloud dibatasi 50 riwayat terbaru — hapus sisanya.
        val cloud = rest.listDocuments(uid, "history", idToken)
        val byPlayed = cloud.sortedByDescending {
            FirebaseRest.fieldLong(it.fields, "playedAtMs")
        }
        val extra = byPlayed.drop(50).map { it.docId }
        if (extra.isNotEmpty()) {
            runCatching { rest.deleteDocuments(uid, "history", extra, idToken) }
        }
        store.saveSyncMeta(meta)
    }

    private fun pushProfile(idToken: String, uid: String) {
        val now = System.currentTimeMillis()
        val fields = JSONObject()
        fields.put("displayName", FirebaseRest.stringField(secure.get(KEY_NAME).orEmpty()))
        fields.put("email", FirebaseRest.stringField(secure.get(KEY_EMAIL).orEmpty()))
        fields.put("photoUrl", FirebaseRest.stringField(secure.get(KEY_PHOTO).orEmpty()))
        fields.put("updatedAtMs", FirebaseRest.intField(now))
        fields.put("updatedAt", FirebaseRest.timestampField(now))
        runCatching { rest.upsertDocument(uid, "meta", "profile", fields, idToken) }
    }

    /** Hapus SELURUH data cloud user ini, lalu keluar (data lokal tetap). */
    fun deleteCloudData(onMessage: (String) -> Unit) {
        scope.launch {
            try {
                val uid = secure.get(KEY_UID) ?: run {
                    emitMessage(onMessage, "Not signed in — no cloud data.")
                    return@launch
                }
                // Retry-on-401 seperti syncNowInternal: token bisa
                // kedaluwarsa di tengah penghapusan batch.
                var attempt = 0
                var deleted = false
                while (!deleted) {
                    attempt++
                    try {
                        val idToken = ensureIdToken(forceRefresh = attempt > 1)
                        deleteCloudDataWithToken(uid, idToken)
                        deleted = true
                    } catch (e: FirebaseRest.FirebaseException) {
                        if (FirebaseRest.isHttpCode(e, 401) && attempt == 1) continue
                        throw e
                    }
                }
                store.saveSyncMeta(JSONObject())
                store.saveSyncTombstones(JSONObject())
                store.setLastSyncAtMs(0L)
                store.setSyncAccountUid(null)
                clearSessionOnly()
                _uiState.value = SyncUiState(configured = SyncConfig.isConfigured, signedIn = false)
                emitMessage(onMessage, "Cloud data deleted. Data on this device remains.")
            } catch (e: SessionInvalid) {
                clearSessionOnly()
                _uiState.value = SyncUiState(configured = true, signedIn = false)
                emitMessage(onMessage, "Session expired, sign in again.")
            } catch (e: SessionTransient) {
                setError("No internet — try again later.")
                emitMessage(onMessage, "Failed to delete cloud data — try again later.")
            } catch (e: Exception) {
                Log.w("SyncManager", "Failed to delete cloud data: ${e.message}", e)
                setError("Failed to delete cloud data — try again later.")
                emitMessage(onMessage, "Failed to delete cloud data — try again later.")
            }
        }
    }

    /** Penghapusan dokumen cloud dengan token tertentu (idempotent). */
    private fun deleteCloudDataWithToken(uid: String, idToken: String) {
        for (coll in listOf("playlists", "liked", "history")) {
            val docs = rest.listDocuments(uid, coll, idToken)
            if (docs.isNotEmpty()) {
                rest.deleteDocuments(uid, coll, docs.map { it.docId }, idToken)
            }
        }
        runCatching { rest.deleteDocument(uid, "meta", "profile", idToken) }
    }

    // ---- Codec dokumen --------------------------------------------------

    private fun encodePlaylist(p: Playlist, resolver: Map<String, Track>, updatedAtMs: Long): JSONObject {
        val o = JSONObject()
        o.put("name", FirebaseRest.stringField(p.name))
        o.put("updatedAtMs", FirebaseRest.intField(updatedAtMs))
        o.put("updatedAt", FirebaseRest.timestampField(updatedAtMs))
        val trackFields = p.trackIds.map { id ->
            val t = resolver[id] ?: placeholderTrack(id)
            FirebaseRest.mapField(snapshotFields(t))
        }
        o.put("tracks", FirebaseRest.arrayField(trackFields))
        return o
    }

    private fun decodePlaylist(doc: FirebaseRest.RawDocument): Playlist {
        val trackIds = FirebaseRest.fieldMapList(doc.fields, "tracks").map { f ->
            FirebaseRest.fieldString(f, "trackId")
        }.filter { it.isNotEmpty() }
        return Playlist(
            id = doc.docId,
            name = FirebaseRest.fieldString(doc.fields, "name"),
            trackIds = trackIds,
            createdAt = FirebaseRest.fieldLong(doc.fields, "updatedAtMs"),
        )
    }

    private fun cacheSnapshots(doc: FirebaseRest.RawDocument) {
        val tracks = FirebaseRest.fieldMapList(doc.fields, "tracks").map { snapshotToTrack(it) }
        if (tracks.isNotEmpty()) store.cacheKnownTracks(tracks)
    }

    private fun encodeLiked(t: Track, updatedAtMs: Long): JSONObject {
        val o = snapshotFields(t)
        o.put("likedAtMs", FirebaseRest.intField(updatedAtMs))
        o.put("updatedAtMs", FirebaseRest.intField(updatedAtMs))
        o.put("updatedAt", FirebaseRest.timestampField(updatedAtMs))
        return o
    }

    private fun encodeHistory(t: Track, playedAtMs: Long, updatedAtMs: Long): JSONObject {
        val o = snapshotFields(t)
        o.put("playedAtMs", FirebaseRest.intField(playedAtMs))
        o.put("updatedAtMs", FirebaseRest.intField(updatedAtMs))
        o.put("updatedAt", FirebaseRest.timestampField(updatedAtMs))
        return o
    }

    private fun snapshotFields(t: Track): JSONObject {
        val o = JSONObject()
        o.put("trackId", FirebaseRest.stringField(t.id))
        o.put("videoId", FirebaseRest.stringField(t.youtubeVideoId ?: t.id.removePrefix("ytm:")))
        o.put("title", FirebaseRest.stringField(t.title))
        o.put("artist", FirebaseRest.stringField(t.artist))
        o.put("imageUrl", FirebaseRest.stringField(t.imageUrl ?: ""))
        o.put("source", FirebaseRest.stringField(t.source.name))
        return o
    }

    private fun snapshotToTrack(f: JSONObject): Track {
        val trackId = FirebaseRest.fieldString(f, "trackId")
        val videoId = FirebaseRest.fieldString(f, "videoId")
        val source = try {
            Source.valueOf(FirebaseRest.fieldString(f, "source").ifEmpty { "YOUTUBE_MUSIC" })
        } catch (_: IllegalArgumentException) {
            Source.YOUTUBE_MUSIC
        }
        return Track(
            id = trackId.ifEmpty { "ytm:$videoId" },
            title = FirebaseRest.fieldString(f, "title"),
            artist = FirebaseRest.fieldString(f, "artist"),
            source = source,
            engine = Engine.EXOPLAYER,
            youtubeVideoId = videoId.ifEmpty { null },
            imageUrl = FirebaseRest.fieldString(f, "imageUrl").takeIf { it.isNotEmpty() },
        )
    }

    private fun placeholderTrack(id: String): Track {
        val videoId = id.removePrefix("ytm:")
        return Track(
            id = id,
            title = "",
            artist = "",
            source = if (id.startsWith("ytm:")) Source.YOUTUBE_MUSIC else Source.LOCAL,
            engine = Engine.EXOPLAYER,
            youtubeVideoId = videoId.takeIf { id.startsWith("ytm:") },
        )
    }

    // ---- Pembantu -------------------------------------------------------

    /** Peta id → Track dari semua sumber lokal yang dikenal (untuk snapshot). */
    private fun knownTrackMap(): Map<String, Track> {
        val map = LinkedHashMap<String, Track>()
        for (t in store.likedTracks()) map[t.id] = t
        for (t in store.recentTracks()) map[t.id] = t
        for (t in store.knownTracks()) map[t.id] = t
        runCatching {
            val downloads = Downloads(context)
            for (t in downloads.listTracks()) map[t.id] = t
        }
        return map
    }

    private fun metaTs(meta: JSONObject, coll: String, docId: String): Long =
        meta.optJSONObject(coll)?.optLong(docId, 0L) ?: 0L

    private fun setMetaTs(meta: JSONObject, coll: String, docId: String, ts: Long) {
        val c = meta.optJSONObject(coll) ?: JSONObject().also { meta.put(coll, it) }
        c.put(docId, ts)
    }

    private fun removeMetaTs(meta: JSONObject, coll: String, docId: String) {
        meta.optJSONObject(coll)?.remove(docId)
    }

    private fun tombHas(tomb: JSONObject, coll: String, docId: String): Boolean =
        tomb.optJSONObject(coll)?.has(docId) == true

    companion object {
        /** ID dokumen Firestore untuk sebuah track — aman (tanpa "/"). */
        fun docIdFor(trackId: String): String {
            if (trackId.startsWith("ytm:")) {
                val videoId = trackId.removePrefix("ytm:")
                if (videoId.isNotEmpty() && '/' !in videoId) return videoId
            }
            val digest = MessageDigest.getInstance("SHA-1")
                .digest(trackId.toByteArray(Charsets.UTF_8))
            return "local-" + digest.joinToString("") { "%02x".format(it) }.take(32)
        }

        private const val KEY_UID = "sync_uid"
        private const val KEY_NAME = "sync_name"
        private const val KEY_EMAIL = "sync_email"
        private const val KEY_PHOTO = "sync_photo"
        private const val KEY_ID_TOKEN = "sync_id_token"
        private const val KEY_ID_EXP = "sync_id_exp_ms"
        private const val KEY_REFRESH = "sync_refresh_token"
    }
}
