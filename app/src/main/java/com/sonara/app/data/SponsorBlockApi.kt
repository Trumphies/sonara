package com.sonara.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Pengambilan segmen SponsorBlock (API publik sponsor.ajay.app) —
 * best-effort, tidak pernah memblokir pemutaran. HTTP 404 dari API
 * berarti video tidak punya segmen di kategori yang diminta
 * (jawaban sah → daftar kosong). Kegagalan lain (jaringan, timeout)
 * juga menjadi daftar kosong — skip adalah peningkatan, bukan
 * syarat putar. Dipanggil dari background thread, sama seperti
 * YoutubeMusicApi.
 */
class SponsorBlockApi(
    private val http: OkHttpClient = defaultClient(),
) {
    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /** Segmen skip untuk [videoId]; kosong = tidak ada / tidak terjangkau. */
    fun fetchSegments(videoId: String): List<SponsorSegment> {
        if (videoId.isBlank()) return emptyList()
        return try {
            val params = SPONSORBLOCK_CATEGORIES.joinToString("&") { "category=$it" }
            val url = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&$params"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "SONARA/1.0")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val text = resp.body?.string() ?: return emptyList()
                parseSegments(text)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
