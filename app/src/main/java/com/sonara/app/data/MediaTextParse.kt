package com.sonara.app.data

/**
 * Parsing teks tampilan YTM — MURNI (tanpa Android / org.json) supaya
 * harness bisa mengompilasinya mandiri. Sumber kebenaran: respons
 * search WEB_REMIX live (fixture harness148, 25 Nov 2026):
 * subtitle kolom kedua berbentuk "Lagu • Coldplay • 2,4 M pemutaran",
 * "Coldplay • 48 jt x ditonton • 4.32", "Single • Coldplay • 2003".
 *
 * Aturan jujur: angka HANYA dibaca bila bagiannya membawa kata
 * "pemutaran" (plays) atau "ditonton"/"views" (views). "460 jt
 * audiens bulanan" (artis) BUKAN hitungan putar dan diabaikan —
 * kalau tidak, baris artis akan tampil seolah paling sering diputar.
 */

/** Hitungan putar/tonton hasil parse; count 0 = tidak ada angka yang sah. */
data class CountInfo(val count: Long, val kind: String)

/**
 * Durasi "3:45", "4.32" (locale id memakai titik), atau "1:02:03"
 * → ms. Bukan pola durasi → null. (Salinan perilaku parseDuration
 * lama di YoutubeMusicApi — diekstrak tanpa berubah supaya parser
 * radio + harness memakai aturan yang sama persis.)
 */
fun parseDurationText(s: String): Long? {
    val t = s.trim()
    val normalized = if (t.matches(Regex("\\d{1,2}\\.\\d{2}"))) t.replace(".", ":") else t
    if (!normalized.matches(Regex("\\d{1,2}:\\d{2}(:\\d{2})?"))) return null
    val parts = normalized.split(":").mapNotNull { it.toLongOrNull() }
    return when (parts.size) {
        2 -> (parts[0] * 60 + parts[1]) * 1000
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
        else -> null
    }
}

private val COUNT_RE = Regex("(\\d+(?:[.,]\\d+)?)\\s*(rb|jt|K|M|B)\\b")

/**
 * Hitungan dari bagian-bagian subtitle (sudah di-split " • ").
 * Bentuk ringkas YTM: "48 jt", "1,1 jt", "2,4 M", "418 rb",
 * "1.2M", "500K". Koma maupun titik dibaca sebagai desimal
 * (bentuk ringkas tidak pernah memakai pemisah ribuan).
 */
fun parseCountFromSubtitle(parts: List<String>): CountInfo {
    for (part in parts) {
        val lower = part.lowercase()
        val kind = when {
            lower.contains("pemutaran") || lower.contains("plays") -> "plays"
            lower.contains("ditonton") || lower.contains("views") -> "views"
            else -> continue
        }
        val m = COUNT_RE.find(part) ?: continue
        val number = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: continue
        val multiplier = when (m.groupValues[2]) {
            "rb", "K" -> 1_000L
            "jt", "M" -> 1_000_000L
            "B" -> 1_000_000_000L
            else -> 1L
        }
        return CountInfo((number * multiplier).toLong(), kind)
    }
    return CountInfo(0L, "")
}

/** Tahun rilis dari bagian subtitle ("Single • Coldplay • 2003"); 0 = tidak ada. */
fun parseYearFromSubtitle(parts: List<String>): Int {
    val yearRe = Regex("^(19|20)\\d{2}$")
    for (part in parts) {
        val t = part.trim()
        if (yearRe.matches(t)) return t.toInt()
    }
    return 0
}

/** Tampilan ringkas hitungan yang jujur: angka yang di-parse, dibulatkan tampil. */
fun formatPlayCount(count: Long, kind: String): String {
    val num = when {
        count >= 1_000_000_000L -> {
            val v = count / 1_000_000_000.0
            (if (v == v.toLong().toDouble()) "${v.toLong()}" else "%.1f".format(v)) + "B"
        }
        count >= 1_000_000L -> {
            val v = count / 1_000_000.0
            (if (v == v.toLong().toDouble()) "${v.toLong()}" else "%.1f".format(v)) + "M"
        }
        count >= 1_000L -> "${count / 1_000L}K"
        else -> "$count"
    }
    return if (kind.isNotBlank()) "$num $kind" else num
}
