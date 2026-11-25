package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Chapters episode/video — PURE Kotlin (tanpa Android), parsing dari
 * respons /next (WEB, bentuk yang sama dengan CommentsApi) yang
 * sudah diverifikasi live dari VM 25 Nov 2026 (fixture harness150).
 *
 * Panel chapters: `engagementPanels[]` →
 * `engagementPanelSectionListRenderer` dengan panelIdentifier
 * `engagement-panel-macro-markers-description-chapters` (atau header
 * "Chapters") → `macroMarkersListRenderer.contents[]` →
 * `macroMarkersListItemRenderer` {title, timeDescription.simpleText
 * "M:SS"/"H:MM:SS"}. Tidak ada field millis — waktu diparse dari teks
 * (deterministik; formatnya selalu jam:menit:detik).
 *
 * Transkrip TIDAK dibangun: panel "Transcript" di /next yang sama
 * hanya membawa continuation get_transcript yang mengembalikan HTTP
 * 400 "Precondition check failed" signed-out — tidak ada data yang
 * bisa ditampilkan jujur (lihat PodcastApi).
 */

/** Satu chapter: judul + posisi mulai dalam ms. */
data class Chapter(
    val title: String,
    val startMs: Long,
)

object ChaptersApi {

    /** "1:02:03" → 3723000; "9:41" → 581000; "0:00" → 0; sampah → null. */
    fun parseChapterTime(text: String?): Long? {
        val t = text?.trim() ?: return null
        if (!t.matches(Regex("""\d{1,2}:\d{2}(:\d{2})?"""))) return null
        val parts = t.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            else -> null
        }
    }

    private fun runsText(textObj: JSONObject?): String? {
        if (textObj == null) return null
        val runs = textObj.optJSONArray("runs") ?: return textObj.optString("simpleText", null)
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
        }
        return sb.toString().ifEmpty { null }
    }

    /** Daftar chapter dari respons /next; kosong bila panel tak ada. */
    fun parseChapters(nextRoot: JSONObject): List<Chapter> {
        val panels = nextRoot.optJSONArray("engagementPanels") ?: return emptyList()
        for (i in 0 until panels.length()) {
            val section = panels.optJSONObject(i)
                ?.optJSONObject("engagementPanelSectionListRenderer") ?: continue
            val panelId = section.optString("panelIdentifier", "")
            val headerTitle = section.optJSONObject("header")
                ?.optJSONObject("engagementPanelTitleHeaderRenderer")
                ?.optJSONObject("title")
                ?.let { runsText(it) } ?: ""
            val isChapters = panelId.contains("macro-markers") ||
                headerTitle.equals("Chapters", ignoreCase = true)
            if (!isChapters) continue
            val contents = section.optJSONObject("content")
                ?.optJSONObject("macroMarkersListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()
            val out = ArrayList<Chapter>()
            for (j in 0 until contents.length()) {
                val marker = contents.optJSONObject(j)
                    ?.optJSONObject("macroMarkersListItemRenderer") ?: continue
                val title = runsText(marker.optJSONObject("title")) ?: continue
                val startMs = parseChapterTime(
                    marker.optJSONObject("timeDescription")?.optString("simpleText", null),
                ) ?: continue
                out += Chapter(title, startMs)
            }
            // Naik + dedupe posisi yang sama (kemunculan pertama menang).
            val seen = HashSet<Long>()
            return out.sortedBy { it.startMs }.filter { seen.add(it.startMs) }
        }
        return emptyList()
    }

    /** Chapter yang sedang berjalan pada [positionMs]; null bila daftar kosong. */
    fun activeChapter(chapters: List<Chapter>, positionMs: Long): Chapter? {
        if (chapters.isEmpty()) return null
        var active: Chapter? = null
        for (c in chapters) {
            if (c.startMs <= positionMs) active = c else break
        }
        return active ?: chapters.first()
    }
}
