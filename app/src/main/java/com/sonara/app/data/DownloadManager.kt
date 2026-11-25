package com.sonara.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Manajer unduhan (v1.0.6): progres, jeda/lanjut (HTTP Range),
 * coba lagi, dan hapus — dengan state yang bisa diamati UI.
 *
 * Satu-satunya pemilik pekerjaan unduhan: TrackSheet (tombol
 * Download) dan tampilan Offline di Koleksi berbagi state ini, jadi
 * progres terlihat di kedua tempat. Format file final TIDAK berubah
 * (lihat Downloads.kt): filesDir/downloads/<videoId>.webm + metadata
 * prefs "downloads". Berkas parsial <videoId>.part di direktori yang
 * sama; jeda/lanjut memakai Range dari ukuran .part.
 *
 * State tugas dipersist di prefs "download_tasks_v1" (lihat
 * LocalStore): DOWNLOADING yang tersisa saat aplikasi mati dibaca
 * ulang sebagai PAUSED (berkas .part tetap, bisa dilanjutkan).
 * Tugas tidak pernah disinkron ke cloud.
 */
object DownloadManager {
    private val _tasks = MutableStateFlow<List<DownloadTasks.DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTasks.DownloadTask>> = _tasks.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val calls = ConcurrentHashMap<String, Call>()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val pauseRequested = ConcurrentHashMap.newKeySet<String>()
    private val lastPersistAtMs = ConcurrentHashMap<String, Long>()

    @Volatile
    private var loaded = false

    /** Muat tugas tersimpan (sekali per proses); DOWNLOADING lama -> PAUSED. */
    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val store = LocalStore(context.applicationContext)
            val restored = store.downloadTasks().map { task ->
                if (task.state == DownloadTaskState.DOWNLOADING ||
                    task.state == DownloadTaskState.QUEUED
                ) {
                    task.copy(state = DownloadTaskState.PAUSED)
                } else {
                    task
                }
            }
            _tasks.value = restored
            loaded = true
        }
    }

    fun taskFor(videoId: String): DownloadTasks.DownloadTask? =
        _tasks.value.firstOrNull { it.videoId == videoId }

    fun isActive(videoId: String): Boolean {
        val state = taskFor(videoId)?.state
        return state == DownloadTaskState.QUEUED || state == DownloadTaskState.DOWNLOADING
    }

    private fun upsert(context: Context, task: DownloadTasks.DownloadTask, persist: Boolean = true) {
        _tasks.update { current ->
            val idx = current.indexOfFirst { it.videoId == task.videoId }
            if (idx >= 0) {
                current.toMutableList().also { it[idx] = task }
            } else {
                current + task
            }
        }
        if (persist) persistTasks(context)
    }

    private fun removeTask(context: Context, videoId: String) {
        _tasks.update { current -> current.filter { it.videoId != videoId } }
        persistTasks(context)
    }

    private fun persistTasks(context: Context) {
        runCatching {
            LocalStore(context.applicationContext).saveDownloadTasks(_tasks.value)
        }
    }

    /** Mulai unduhan untuk satu track (atau lanjutkan tugas yang dijeda). */
    fun start(context: Context, track: Track) {
        val appContext = context.applicationContext
        ensureLoaded(appContext)
        val videoId = track.youtubeVideoId ?: return
        val downloads = Downloads(appContext)
        if (downloads.isDownloaded(videoId)) {
            upsert(
                appContext,
                DownloadTasks.DownloadTask(
                    videoId = videoId,
                    title = track.title,
                    artist = track.artist,
                    durationMs = track.durationMs,
                    state = DownloadTaskState.COMPLETED,
                    downloadedBytes = downloads.listEntries()
                        .firstOrNull { it.videoId == videoId }?.bytes ?: 0L,
                    totalBytes = 0L,
                    error = null,
                ),
            )
            return
        }
        if (isActive(videoId)) return
        val existing = taskFor(videoId)
        upsert(
            appContext,
            DownloadTasks.DownloadTask(
                videoId = videoId,
                title = track.title,
                artist = track.artist,
                durationMs = track.durationMs,
                state = DownloadTaskState.QUEUED,
                downloadedBytes = existing?.downloadedBytes ?: 0L,
                totalBytes = existing?.totalBytes ?: 0L,
                error = null,
            ),
        )
        jobs[videoId] = scope.launch {
            runDownload(appContext, videoId, track.title, track.artist, track.durationMs)
        }
    }

    /** Lanjutkan tugas yang dijeda/gagal dari metadata tersimpan. */
    fun resume(context: Context, videoId: String) {
        val appContext = context.applicationContext
        ensureLoaded(appContext)
        val task = taskFor(videoId) ?: return
        if (isActive(videoId)) return
        if (task.state != DownloadTaskState.PAUSED && task.state != DownloadTaskState.FAILED) return
        upsert(appContext, task.copy(state = DownloadTaskState.QUEUED, error = null))
        jobs[videoId] = scope.launch {
            runDownload(appContext, videoId, task.title, task.artist, task.durationMs)
        }
    }

    /** Coba lagi dari nol: berkas .part dibuang, unduhan diulang penuh. */
    fun retry(context: Context, videoId: String) {
        val appContext = context.applicationContext
        ensureLoaded(appContext)
        val task = taskFor(videoId) ?: return
        if (isActive(videoId)) return
        runCatching { Downloads(appContext).partFileFor(videoId).delete() }
        upsert(
            appContext,
            task.copy(
                state = DownloadTaskState.QUEUED,
                downloadedBytes = 0L,
                totalBytes = 0L,
                error = null,
            ),
        )
        jobs[videoId] = scope.launch {
            runDownload(appContext, videoId, task.title, task.artist, task.durationMs)
        }
    }

    /** Jeda: panggilan jaringan dibatalkan, .part dipertahankan. */
    fun pause(context: Context, videoId: String) {
        ensureLoaded(context.applicationContext)
        if (!isActive(videoId)) return
        pauseRequested.add(videoId)
        calls.remove(videoId)?.cancel()
        jobs.remove(videoId)?.cancel()
    }

    /** Hapus tugas: batalkan bila aktif, buang .part (+ .webm bila selesai). */
    fun delete(context: Context, videoId: String) {        val appContext = context.applicationContext
        ensureLoaded(appContext)
        pauseRequested.remove(videoId)
        calls.remove(videoId)?.cancel()
        jobs.remove(videoId)?.cancel()
        val wasCompleted = taskFor(videoId)?.state == DownloadTaskState.COMPLETED
        scope.launch {
            jobs[videoId]?.cancelAndJoin()
            val downloads = Downloads(appContext)
            runCatching { downloads.partFileFor(videoId).delete() }
            if (wasCompleted) runCatching { downloads.delete(videoId) }
            removeTask(appContext, videoId)
        }
    }

    /**
     * Lupakan catatan tugas saja (tanpa operasi file/batal): dipakai
     * saat berkas dihapus lewat jalur lain (baris Delete tampilan
     * Offline). Tidak menyentuh tugas yang sedang aktif.
     */
    fun forget(context: Context, videoId: String) {
        val appContext = context.applicationContext
        ensureLoaded(appContext)
        if (isActive(videoId)) return
        removeTask(appContext, videoId)
    }

    /**
     * Hentikan semua unduhan + hapus semua catatan tugas + buang
     * berkas .part. Berkas .webm TIDAK disentuh (pemanggil yang
     * menghapus unduhan — mis. "Delete all downloads" di Settings —
     * mengurusnya sendiri).
     */
    fun resetAll(context: Context) {
        val appContext = context.applicationContext
        ensureLoaded(appContext)
        pauseRequested.clear()
        calls.values.forEach { runCatching { it.cancel() } }
        calls.clear()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        scope.launch {
            val downloads = Downloads(appContext)
            for (task in _tasks.value) {
                runCatching { downloads.partFileFor(task.videoId).delete() }
            }
            _tasks.value = emptyList()
            persistTasks(appContext)
        }
    }

    /**
     * Auto-download: lagu yang baru ditambahkan ke playlist yang
     * sakelarnya menyala diunduh diam-diam. Mengembalikan jumlah yang
     * benar-benar mulai diunduh (lagu yang sudah terunduh / sedang
     * diunduh dilewati). Lokal saja, default mati.
     */
    fun maybeAutoDownload(
        context: Context,
        store: LocalStore,
        playlistId: String,
        addedTracks: List<Track>,
    ): Int {
        val appContext = context.applicationContext
        ensureLoaded(appContext)
        if (!LibraryOrgPolicy.isAutoDownload(store.playlistOrg(), playlistId)) return 0
        val downloads = Downloads(appContext)
        var started = 0
        for (track in addedTracks) {
            val videoId = track.youtubeVideoId ?: continue
            if (track.source != Source.YOUTUBE_MUSIC) continue
            if (downloads.isDownloaded(videoId) || isActive(videoId)) continue
            if (taskFor(videoId)?.state == DownloadTaskState.COMPLETED) continue
            start(appContext, track)
            started++
        }
        return started
    }

    private suspend fun runDownload(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
    ) {
        val downloads = Downloads(context)
        val http: OkHttpClient = SonaraHttp.defaultHttp()
        val partFile = downloads.partFileFor(videoId)
        val targetFile = downloads.fileFor(videoId)
        try {
            val store = LocalStore(context)
            val ytmApi = YoutubeMusicApi().apply { audioQuality = store.audioQuality }
            val stream = ytmApi.resolveStream(videoId).getOrElse { err ->
                fail(context, videoId, err.message ?: "Couldn't resolve this song")
                return
            }
            var offset = if (partFile.isFile) partFile.length() else 0L
            var total = taskFor(videoId)?.totalBytes ?: 0L
            var response = fetch(http, stream.url, offset, videoId)
            if (DownloadTasks.shouldRestartOnStatus(response.code, offset)) {
                runCatching { response.close() }
                runCatching { partFile.delete() }
                offset = 0L
                total = 0L
                response = fetch(http, stream.url, 0L, videoId)
            }
            if (!response.isSuccessful) {
                runCatching { response.close() }
                fail(context, videoId, "Download failed (HTTP ${response.code})")
                return
            }
            val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
            if (contentLength > 0) total = offset + contentLength
            upsert(
                context,
                DownloadTasks.DownloadTask(
                    videoId, title, artist,
                    if (stream.durationMs > 0) stream.durationMs else durationMs,
                    DownloadTaskState.DOWNLOADING, offset, total, null,
                ),
            )
            var written = offset
            var lastUiUpdateAtMs = 0L
            try {
                val body = response.body
                    ?: throw IllegalStateException("Download failed: empty response")
                body.byteStream().use { input ->
                    FileOutputStream(partFile, offset > 0).use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = runInterruptible { input.read(buf) }
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            written += n
                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdateAtMs >= 250) {
                                lastUiUpdateAtMs = now
                                upsert(
                                    context,
                                    DownloadTasks.DownloadTask(
                                        videoId, title, artist,
                                        if (stream.durationMs > 0) stream.durationMs else durationMs,
                                        DownloadTaskState.DOWNLOADING, written, total, null,
                                    ),
                                    persist = false,
                                )
                                // Persist berkala supaya jeda aplikasi
                                // di tengah unduhan tetap bisa lanjut.
                                if (now - (lastPersistAtMs[videoId] ?: 0L) >= 2000) {
                                    lastPersistAtMs[videoId] = now
                                    persistTasks(context)
                                }
                            }
                        }
                    }
                }
            } finally {
                runCatching { response.close() }
            }
            // Selesai: .part -> .webm + catat metadata (format lama).
            val finalDuration = if (stream.durationMs > 0) stream.durationMs else durationMs
            if (targetFile.exists()) runCatching { targetFile.delete() }
            val renamed = runCatching { partFile.renameTo(targetFile) }.getOrDefault(false)
            if (!renamed) {
                runCatching {
                    partFile.copyTo(targetFile, overwrite = true)
                    partFile.delete()
                }
            }
            if (!targetFile.isFile) {
                fail(context, videoId, "Download failed: couldn't save the file")
                return
            }
            downloads.addEntry(
                Downloads.Entry(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    durationMs = finalDuration,
                    bytes = written,
                ),
            )
            upsert(
                context,
                DownloadTasks.DownloadTask(
                    videoId, title, artist, finalDuration,
                    DownloadTaskState.COMPLETED, written, total, null,
                ),
            )
        } catch (e: Exception) {
            if (videoId in pauseRequested) {
                pauseRequested.remove(videoId)
                val current = taskFor(videoId)
                if (current != null) {
                    upsert(
                        context,
                        current.copy(
                            state = DownloadTaskState.PAUSED,
                            downloadedBytes = if (partFile.isFile) partFile.length() else current.downloadedBytes,
                            error = null,
                        ),
                    )
                }
            } else if (e is kotlinx.coroutines.CancellationException) {
                throw e
            } else {
                fail(context, videoId, "Download failed: ${e.message}")
            }
        } finally {
            calls.remove(videoId)
            jobs.remove(videoId)
        }
    }

    private fun fetch(
        http: OkHttpClient,
        url: String,
        offset: Long,
        videoId: String,
    ): okhttp3.Response {
        val builder = okhttp3.Request.Builder().url(url).get()
        if (offset > 0) builder.header("Range", "bytes=$offset-")
        val call = http.newCall(builder.build())
        // Dilacak agar pause() bisa membatalkan panggilan yang sedang
        // berjalan (execute() memblokir thread IO; cancel job saja
        // tidak menghentikannya).
        calls[videoId] = call
        return call.execute()
    }

    private fun fail(context: Context, videoId: String, message: String) {
        val current = taskFor(videoId) ?: return
        upsert(
            context,
            current.copy(state = DownloadTaskState.FAILED, error = message),
        )
    }
}
