package com.sonara.app.data

import mobileapi.Mobileapi
import org.json.JSONArray
import org.json.JSONObject

/**
 * Jembatan ke mesin Go asli MixMusic (fzn0x/mixmusic, MIT) yang di-bind
 * lewat gomobile menjadi libgojni.so + mobileapi.Mobileapi.
 *
 * Yang dipakai di sini: search YouTube Music (signed-out) dan stream
 * langsung (visionOS player, Opus 251) — kode yang sama persis
 * dengan yang jalan di MixMusic desktop, bukan tiruan Kotlin-nya.
 * Semua panggilan sinkron (network) — panggil dari background
 * thread, sama seperti YoutubeMusicApi.
 *
 * Kalau library Go gagal dimuat (.so hilang / ABI tidak cocok),
 * [isAvailable] jadi false dan pemanggil HARUS jatuh kembali ke
 * implementasi Kotlin di YoutubeMusicApi. Aplikasi tidak boleh jadi
 * lebih buruk dari v1.1 hanya karena mesin Go-nya tidak ada.
 */
object GoEngine {

    @Volatile
    private var available: Boolean? = null

    /** True kalau libgojni.so berhasil dimuat dan Init menjawab ok. */
    fun isAvailable(): Boolean {
        available?.let { return it }
        val ok = try {
            Mobileapi.touch()
            val res = JSONObject(Mobileapi.init(""))
            res.optBoolean("ok", false)
        } catch (_: Throwable) {
            false
        }
        available = ok
        return ok
    }

    private fun <T> fail(msg: String): Result<T> = Result.failure(IllegalStateException(msg))

    private fun errorOf(root: JSONObject): String? {
        if (root.optBoolean("ok", false)) return null
        return root.optString("error", "Go engine returned an error").ifBlank { "Go engine returned an error" }
    }

    private fun parseArtists(track: JSONObject): String {
        val arr = track.optJSONArray("artists") ?: return ""
        val names = ArrayList<String>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.optString("name", "")?.takeIf { it.isNotBlank() }?.let { names += it }
        }
        return names.joinToString(", ")
    }

    private fun parseImage(track: JSONObject): String? {
        val arr = track.optJSONArray("artwork") ?: return null
        val candidates = ArrayList<ThumbCandidate>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            candidates += ThumbCandidate(o.optInt("width", 0), o.optString("url", ""))
        }
        return selectArtworkUrl(candidates)
    }

    private fun itemToResult(item: JSONObject): YoutubeMusicApi.YtmResult? {
        val track = item.optJSONObject("track") ?: return null
        val rawId = track.optString("id", "")
        if (rawId.isBlank()) return null
        val videoId = rawId.removePrefix("yt:")
        val kind = item.optString("kind", "")
        return YoutubeMusicApi.YtmResult(
            videoId = videoId,
            title = track.optString("title", ""),
            artist = parseArtists(track),
            durationMs = track.optLong("durationMs", 0L),
            isSong = kind == "track" && !track.optBoolean("video", false),
            explicit = track.optBoolean("explicit", false),
            imageUrl = parseImage(track),
        )
    }

    /** Cari lagu di YouTube Music lewat mesin Go (signed-out). */
    fun searchYtm(query: String): Result<List<YoutubeMusicApi.YtmResult>> {
        if (!isAvailable()) return fail("Go engine unavailable")
        return try {
            val root = JSONObject(Mobileapi.searchYTM(query))
            errorOf(root)?.let { return fail("YouTube Music (Go engine): $it") }
            val arr = root.optJSONArray("items") ?: JSONArray()
            val out = ArrayList<YoutubeMusicApi.YtmResult>()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { itemToResult(it) }?.let { out += it }
            }
            Result.success(out)
        } catch (e: Exception) {
            fail("YouTube Music search (Go engine) failed: ${e.message ?: "unknown error"}")
        }
    }

    /** Ambil stream audio langsung lewat mesin Go (visionOS player). */
    fun resolveStream(videoId: String): Result<YoutubeMusicApi.YtmStream> {
        if (!isAvailable()) return fail("Go engine unavailable")
        return try {
            val root = JSONObject(Mobileapi.streamYTM(videoId))
            errorOf(root)?.let { return fail("YouTube (Go engine): $it") }
            val s = root.optJSONObject("stream")
                ?: return fail("YouTube (Go engine): no stream for this song.")
            val url = s.optString("url", "")
            if (url.isBlank()) return fail("YouTube (Go engine): empty stream.")
            val mime = s.optString("mimeType", "")
            val isHls = mime.contains("x-mpegURL")
            val itag = when {
                mime.contains("opus") -> 251
                mime.contains("mp4a.40.2") -> 140
                else -> 0
            }
            Result.success(
                YoutubeMusicApi.YtmStream(
                    url = url,
                    itag = itag,
                    mimeType = mime,
                    durationMs = s.optLong("durationMs", 0L),
                    isLive = isHls,
                    hlsManifestUrl = if (isHls) url else null,
                ),
            )
        } catch (e: Exception) {
            fail("Failed to fetch the YouTube stream (Go engine): ${e.message ?: "unknown error"}")
        }
    }
}
