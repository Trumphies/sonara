package com.sonara.app.data

import android.content.Context
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Download rekaman YouTube Music untuk didengar offline.
 *
 * Alur: resolve stream langsung (visionOS, Opus 251 / AAC 140 — sama
 * seperti pemutaran) → unduh bytes ke filesDir/downloads/<videoId>.webm
 * → simpan metadata di SharedPreferences (JSON). File yang sudah
 * diunduh diputar sebagai Track EXOPLAYER biasa dengan streamUrl
 * "file://..." — tidak butuh internet lagi.
 *
 * Bukan DRM bypass: yang diunduh adalah stream audio yang sama dengan
 * yang diputar; file hanya untuk pemakaian pribadi di perangkat ini.
 */
class Downloads(
    private val context: Context,
    audioQuality: AudioQuality = AudioQuality.BEST,
    private val ytmApi: YoutubeMusicApi = YoutubeMusicApi().apply { this.audioQuality = audioQuality },
    private val http: OkHttpClient = SonaraHttp.defaultHttp(),
) {
    private val prefs = context.getSharedPreferences("sonara", Context.MODE_PRIVATE)
    private val dir: File
        get() = File(context.filesDir, "downloads").apply { mkdirs() }

    data class Entry(
        val videoId: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val bytes: Long,
    )

    fun listEntries(): List<Entry> {
        val raw = prefs.getString("downloads", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Entry(
                    videoId = o.optString("videoId"),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    durationMs = o.optLong("durationMs", 0),
                    bytes = o.optLong("bytes", 0),
                )
            }.filter { File(dir, "${it.videoId}.webm").isFile }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Daftar download sebagai Track yang langsung bisa diputar offline. */
    fun listTracks(): List<Track> = listEntries().map { e ->
        Track(
            id = "dl:${e.videoId}",
            title = e.title,
            artist = e.artist,
            source = Source.YOUTUBE_MUSIC,
            engine = Engine.EXOPLAYER,
            durationMs = e.durationMs,
            streamUrl = "file://${File(dir, "${e.videoId}.webm").absolutePath}",
            youtubeVideoId = e.videoId,
        )
    }

    fun isDownloaded(videoId: String): Boolean =
        File(dir, "$videoId.webm").isFile

    /** Berkas final satu unduhan (v1.0.6, dipakai DownloadManager). */
    fun fileFor(videoId: String): File = File(dir, "$videoId.webm")

    /** Berkas parsial satu unduhan yang sedang berjalan (v1.0.6). */
    fun partFileFor(videoId: String): File = File(dir, "$videoId.part")

    /** Catat satu entri yang sudah selesai diunduh (dedup per videoId). */
    fun addEntry(entry: Entry) {
        val kept = listEntries().filter { it.videoId != entry.videoId } + entry
        saveEntries(kept)
    }

    fun totalBytes(): Long = listEntries().sumOf { it.bytes }

    /** Unduh satu track YTM. Dipanggil dari thread latar. */
    fun download(track: Track): Result<Entry> {
        val videoId = track.youtubeVideoId
            ?: return Result.failure(IllegalStateException("This song isn't from YouTube Music"))
        if (isDownloaded(videoId)) {
            return Result.failure(IllegalStateException("This song is already downloaded"))
        }
        val stream = ytmApi.resolveStream(videoId).getOrElse {
            return Result.failure(it)
        }
        val target = File(dir, "$videoId.webm")
        return try {
            val req = okhttp3.Request.Builder().url(stream.url).get().build()
            var written = 0L
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(
                        IllegalStateException("Download failed (HTTP ${resp.code})"),
                    )
                }
                val body = resp.body ?: return Result.failure(
                    IllegalStateException("Download failed: empty response"),
                )
                body.byteStream().use { input ->
                    FileOutputStream(target).use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            written += n
                        }
                    }
                }
            }
            val entry = Entry(
                videoId = videoId,
                title = track.title,
                artist = track.artist,
                durationMs = if (stream.durationMs > 0) stream.durationMs else track.durationMs,
                bytes = written,
            )
            saveEntries(listEntries() + entry)
            Result.success(entry)
        } catch (e: Exception) {
            runCatching { target.delete() }
            Result.failure(IllegalStateException("Download failed: ${e.message}"))
        }
    }

    /** Hapus SEMUA unduhan (file + metadata). Dipakai Settings. */
    fun deleteAll() {
        for (entry in listEntries()) {
            runCatching { File(dir, "${entry.videoId}.webm").delete() }
        }
        saveEntries(emptyList())
    }

    fun delete(videoId: String) {
        runCatching { File(dir, "$videoId.webm").delete() }
        saveEntries(listEntries().filter { it.videoId != videoId })
    }

    private fun saveEntries(list: List<Entry>) {
        val arr = JSONArray()
        for (e in list) {
            val o = JSONObject()
            o.put("videoId", e.videoId)
            o.put("title", e.title)
            o.put("artist", e.artist)
            o.put("durationMs", e.durationMs)
            o.put("bytes", e.bytes)
            arr.put(o)
        }
        prefs.edit().putString("downloads", arr.toString()).apply()
    }
}
