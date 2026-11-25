package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Status tugas unduhan (v1.0.6). Logika MURNI status/progres di
 * [DownloadTasks]; eksekusi jaringan + file ada di [DownloadManager].
 * Format file final TIDAK berubah: filesDir/downloads/<videoId>.webm
 * + metadata prefs "downloads" (lihat Downloads.kt). Berkas parsial
 * memakai <videoId>.part di direktori yang sama.
 */

enum class DownloadTaskState {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    FAILED,
    COMPLETED,
}

object DownloadTasks {

    /** Satu tugas unduhan (satu lagu). */
    data class DownloadTask(
        val videoId: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val state: DownloadTaskState,
        /** Byte yang sudah tertulis di berkas .part (0 = belum mulai). */
        val downloadedBytes: Long,
        /** Total byte (0 = belum diketahui — server tidak memberi Content-Length). */
        val totalBytes: Long,
        /** Pesan kegagalan terakhir (hanya untuk FAILED). */
        val error: String?,
    )

    /** Fraksi progres 0..1; total tidak diketahui = 0 (bukan tebakan). */
    fun progress(downloadedBytes: Long, totalBytes: Long): Float {
        if (totalBytes <= 0 || downloadedBytes <= 0) return 0f
        return (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    }

    /** Layak dilanjutkan dari .part (bukan mulai dari nol)? */
    fun shouldResume(
        state: DownloadTaskState,
        downloadedBytes: Long,
        totalBytes: Long,
    ): Boolean {
        if (state != DownloadTaskState.PAUSED && state != DownloadTaskState.FAILED) return false
        if (downloadedBytes <= 0) return false
        if (totalBytes > 0 && downloadedBytes >= totalBytes) return false
        return true
    }

    /**
     * Server mengabaikan Range (jawab 200 padahal offset > 0) atau
     * menolaknya (416) → mulai dari nol. 206 = lanjutkan menempel.
     */
    fun shouldRestartOnStatus(httpCode: Int, offsetBytes: Long): Boolean {
        if (offsetBytes <= 0) return false
        return httpCode == 200 || httpCode == 416
    }

    /** Tampilan ukuran byte yang jujur: B / KB / MB, tanpa pembulatan palsu. */
    fun formatBytes(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> "%.1f MB".format(java.util.Locale.US, bytes / (1024.0 * 1024.0))
    }

    // ---- serialisasi JSON (org.json; LocalStore hanya menyimpan string) ----

    fun tasksToJson(tasks: List<DownloadTask>): JSONArray {
        val arr = JSONArray()
        for (t in tasks) {
            arr.put(JSONObject().apply {
                put("videoId", t.videoId)
                put("title", t.title)
                put("artist", t.artist)
                put("durationMs", t.durationMs)
                put("state", t.state.name)
                put("downloaded", t.downloadedBytes)
                put("total", t.totalBytes)
                put("error", t.error ?: "")
            })
        }
        return arr
    }

    fun tasksFromJson(raw: String?): List<DownloadTask> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<DownloadTask>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val videoId = o.optString("videoId")
                if (videoId.isBlank()) continue
                val state = try {
                    DownloadTaskState.valueOf(o.optString("state", "QUEUED"))
                } catch (_: IllegalArgumentException) {
                    DownloadTaskState.QUEUED
                }
                out += DownloadTask(
                    videoId = videoId,
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    durationMs = o.optLong("durationMs", 0L),
                    state = state,
                    downloadedBytes = o.optLong("downloaded", 0L),
                    totalBytes = o.optLong("total", 0L),
                    error = o.optString("error").takeIf { it.isNotEmpty() },
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Bantu fixture: round-trip lewat string JSON. */
    fun tasksRoundTrip(tasks: List<DownloadTask>): List<DownloadTask> =
        tasksFromJson(tasksToJson(tasks).toString())
}
