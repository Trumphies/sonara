package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Saran pencarian YTM (endpoint music/get_search_suggestions) —
 * parsing MURNI dari JSONObject (fixture harness148 = respons live
 * 25 Nov 2026: 6 saran untuk "coldplay", SEMUANYA query
 * searchEndpoint — tidak ada saran entitas signed-out).
 *
 * [SearchSuggestion] membawa KEDUA bentuk: [videoId] bila saran
 * menunjuk langsung ke video (ketuk = langsung putar), [query] bila
 * saran adalah pelengkap query (ketuk = cari query itu). Bila
 * endpoint membawa browseEndpoint (artis) atau tidak dikenal, query
 * jatuh ke teks saran — pencarian atas nama itu, bukan tebakan
 * konten. Tidak ada saran yang dikarang: daftar kosong = tidak ada
 * saran yang ditampilkan.
 */
data class SearchSuggestion(
    val text: String,
    val query: String?,
    val videoId: String?,
)

private fun suggestionRunsText(textObj: JSONObject?): String? {
    if (textObj == null) return null
    val runs = textObj.optJSONArray("runs") ?: return textObj.optString("simpleText").ifEmpty { null }
    val sb = StringBuilder()
    for (i in 0 until runs.length()) {
        sb.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
    }
    return sb.toString().ifEmpty { null }
}

/** Kumpulkan semua searchSuggestionRenderer di seluruh respons. */
fun parseSearchSuggestions(root: JSONObject): List<SearchSuggestion> {
    val out = ArrayList<SearchSuggestion>()
    fun walk(node: Any?) {
        when (node) {
            is JSONObject -> {
                if (node.has("searchSuggestionRenderer")) {
                    val item = node.getJSONObject("searchSuggestionRenderer")
                    val text = suggestionRunsText(item.optJSONObject("suggestion")) ?: ""
                    if (text.isNotBlank()) {
                        val endpoint = item.optJSONObject("navigationEndpoint")
                        val watch = endpoint?.optJSONObject("watchEndpoint")
                        val videoId = watch?.optString("videoId", "")?.takeIf { it.isNotBlank() }
                        val query = endpoint?.optJSONObject("searchEndpoint")
                            ?.optString("query", "")?.takeIf { it.isNotBlank() }
                            ?: text
                        out.add(SearchSuggestion(text, query, videoId))
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
