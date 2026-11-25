package com.sonara.app.data

/**
 * Parser link YouTube Music MURNI (v1.10.21, operasi string saja —
 * bisa diuji di JVM; tanpa android.net.Uri, mengikuti pola
 * [parseJamDeepLink]). Dipakai MainActivity untuk intent VIEW dari
 * app lain; intent-filter-nya di AndroidManifest (chooser, TANPA
 * autoVerify — filter App Link Jam tidak disentuh).
 *
 * Bentuk yang diterima:
 * - `https://music.youtube.com/watch?v=<ID11>` (param ekstra
 *   seperti `t=`, `si=`, `list=` diabaikan; `v=` pertama dipakai).
 * - `https://youtu.be/<ID11>` (segmen path pertama; query diabaikan).
 * Host dicocokkan case-insensitive; videoId case-sensitive, tepat
 * 11 karakter `[A-Za-z0-9_-]` (format ID YouTube).
 * - Link playlist saja (`list=` tanpa `v=`) → [PlaylistOnly] —
 *   perilaku jujur "belum didukung", tanpa konten karangan.
 * - Selain itu → [Invalid] (buka app biasa + toast, tanpa crash).
 */
sealed interface YtmLinkResult {
    data class Video(val videoId: String) : YtmLinkResult
    data class PlaylistOnly(val listId: String) : YtmLinkResult
    data object Invalid : YtmLinkResult
}

private fun isValidVideoId(id: String): Boolean {
    if (id.length != 11) return false
    return id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
}

private fun queryParam(query: String, name: String): String? {
    for (part in query.split('&')) {
        val eq = part.indexOf('=')
        if (eq < 0) continue
        if (part.substring(0, eq) == name) return part.substring(eq + 1)
    }
    return null
}

/** True bila URI tampak seperti link YTM (host + skema cocok). */
fun isYtmLinkUri(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    val trimmed = raw.trim()
    val schemeEnd = trimmed.indexOf("://")
    if (schemeEnd < 0) return false
    val scheme = trimmed.substring(0, schemeEnd).lowercase()
    if (scheme != "https" && scheme != "http") return false
    val afterScheme = trimmed.substring(schemeEnd + 3)
    val host = afterScheme.substringBefore('/').substringBefore(':').lowercase()
    return host == "music.youtube.com" || host == "youtu.be"
}

fun parseYtmLink(raw: String?): YtmLinkResult {
    if (!isYtmLinkUri(raw)) return YtmLinkResult.Invalid
    val trimmed = raw!!.trim()
    val afterScheme = trimmed.substringAfter("://", "")
    val host = afterScheme.substringBefore('/').substringBefore(':').lowercase()
    val rest = afterScheme.substringAfter('/', "")
    val pathAndQuery = rest.substringBefore('#')
    val path = pathAndQuery.substringBefore('?')
    val query = if ('?' in pathAndQuery) pathAndQuery.substringAfter('?', "") else ""
    return when (host) {
        "music.youtube.com" -> {
            val firstSegment = path.split('/').firstOrNull().orEmpty()
            if (firstSegment != "watch") return YtmLinkResult.Invalid
            val v = queryParam(query, "v")?.takeIf { it.isNotEmpty() }
            if (v != null) {
                if (isValidVideoId(v)) YtmLinkResult.Video(v) else YtmLinkResult.Invalid
            } else {
                val list = queryParam(query, "list")?.takeIf { it.isNotEmpty() }
                if (list != null) YtmLinkResult.PlaylistOnly(list) else YtmLinkResult.Invalid
            }
        }
        "youtu.be" -> {
            val id = path.split('/').firstOrNull { it.isNotEmpty() }.orEmpty()
            if (isValidVideoId(id)) YtmLinkResult.Video(id) else YtmLinkResult.Invalid
        }
        else -> YtmLinkResult.Invalid
    }
}
