package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Item radio dari /next — parsing MURNI dari JSONObject (fixture
 * harness148 = respons live /next + playlistId RD… 25 Nov 2026:
 * 25 playlistPanelVideoRenderer, masing-masing videoId + judul +
 * longBylineText (artis) + lengthText ("4.26") + satu thumbnail
 * i.ytimg). Tidak ada field yang dikarang: tanpa videoId = item
 * dibuang; tanpa durasi = 0 (placeholder jujur "--:--" di UI).
 */
data class RadioItem(
    val videoId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val imageUrl: String?,
)

private fun radioRunsText(textObj: JSONObject?): String {
    if (textObj == null) return ""
    val runs = textObj.optJSONArray("runs")
    if (runs != null) {
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
        }
        return sb.toString()
    }
    return textObj.optString("simpleText", "")
}

fun parseRadioPanels(root: JSONObject): List<RadioItem> {
    val out = ArrayList<RadioItem>()
    fun walk(node: Any?) {
        when (node) {
            is JSONObject -> {
                if (node.has("playlistPanelVideoRenderer")) {
                    val item = node.getJSONObject("playlistPanelVideoRenderer")
                    val videoId = item.optString("videoId", "")
                    if (videoId.isNotBlank()) {
                        val title = radioRunsText(item.optJSONObject("title"))
                        val artist = radioRunsText(item.optJSONObject("longBylineText"))
                        val length = radioRunsText(item.optJSONObject("lengthText"))
                        val durationMs = parseDurationText(length) ?: 0L
                        var imageUrl: String? = null
                        val thumbs = item.optJSONObject("thumbnail")
                            ?.optJSONArray("thumbnails")
                        if (thumbs != null && thumbs.length() > 0) {
                            imageUrl = thumbs.optJSONObject(thumbs.length() - 1)
                                ?.optString("url", "")?.takeIf { it.isNotBlank() }
                        }
                        if (imageUrl.isNullOrBlank()) {
                            imageUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        }
                        out.add(RadioItem(videoId, title, artist, durationMs, imageUrl))
                    }
                }
                val keys = node.keys()
                while (keys.hasNext()) walk(node.get(keys.next()))
            }
            is JSONArray -> {
                for (i in 0 until node.length()) walk(node.get(i))
            }
        }
    }
    walk(root)
    return out
}

/** Ubah item radio menjadi Track yang bisa diputar (bukan radio = flag di PlayerManager). */
fun RadioItem.toTrack(): Track = Track(
    id = "ytm:$videoId",
    title = title,
    artist = artist,
    album = "",
    source = Source.YOUTUBE_MUSIC,
    engine = Engine.YOUTUBE,
    durationMs = durationMs,
    youtubeVideoId = videoId,
    imageUrl = imageUrl,
    explicit = false,
)
