package com.sonara.app.data

/**
 * Nama artis dari subtitle YTM + perlakuan label tipe (v1.10.19) — MURNI.
 *
 * BUG: subtitle kolom kedua YTM sering berbentuk "Lagu • <Artis> • 3:24"
 * atau HANYA label tipe ("Video", "Song") tanpa nama artis. Parser lama
 * mengambil bagian pertama apa adanya, sehingga label tipe tampil di
 * slot artis ("Song", "Video") di detail, baris, dan mini player.
 * Perbaikan: bagian yang merupakan label tipe / durasi / tanggal /
 * "Explicit" dilewati; bagian non-label PERTAMA adalah artis; bila
 * tidak ada → "" (kosong, bukan label). Data tersimpan lama yang
 * artisnya sudah terlanjur label tipe diperlakukan kosong saat render
 * lewat [displayArtist] — tanpa migrasi/ubah data tersimpan.
 */

/** Bagian subtitle yang BUKAN nama artis (label tipe, case-insensitive). */
val SUBTITLE_TYPE_LABELS: Set<String> = setOf(
    "song", "video", "music video", "lagu", "album", "single", "ep",
    "artist", "playlist", "podcast", "episode",
)

private val DURATION_PART = Regex("""\d{1,2}[:.]\d{2}(:\d{2})?""")
private val YEAR_PART = Regex("""\d{4}""")
private val DATE_PART = Regex(
    """(\d{1,2}\s+(jan|feb|mar|apr|mei|may|jun|jul|agu|aug|sep|okt|oct|nov|des|dec)(\s+\d{4})?""" +
        """|(jan|feb|mar|apr|mei|may|jun|jul|agu|aug|sep|okt|oct|nov|des|dec)\s+\d{1,2}(,?\s+\d{4})?)""",
    RegexOption.IGNORE_CASE,
)

/** True bila satu bagian subtitle (dipisah " • ") bukan nama artis. */
fun isNonArtistSubtitlePart(part: String): Boolean {
    val t = part.trim()
    if (t.isEmpty()) return true
    val lower = t.lowercase()
    if (lower in SUBTITLE_TYPE_LABELS) return true
    if (lower == "explicit") return true
    if (DURATION_PART.matches(t)) return true
    if (YEAR_PART.matches(t)) return true
    if (DATE_PART.matches(t)) return true
    return false
}

/**
 * Artis dari teks subtitle kolom kedua: bagian non-label pertama.
 * Contoh: "Lagu • Virgoun • 3:24" → "Virgoun";
 * "Video • Everpop Radio • 29 rb x ditonton" → "Everpop Radio";
 * "Video" → "".
 */
fun extractArtistFromSubtitle(subtitle: String): String {
    for (part in subtitle.split(" • ")) {
        if (!isNonArtistSubtitlePart(part)) return part.trim()
    }
    return ""
}

/**
 * Artis untuk ditampilkan: "" bila kosong atau HANYA label tipe
 * (data lama hasil parser lama, mis. "Video"/"Song"/"Lagu").
 * Pemanggil yang butuh teks selalu-pengisi (header detail) memakai
 * "Unknown artist" sendiri; baris/mini player membiarkan "" (kosong).
 */
fun displayArtist(artist: String): String {
    val t = artist.trim()
    if (t.isEmpty()) return ""
    if (t.lowercase() in SUBTITLE_TYPE_LABELS) return ""
    return t
}

/** Artis bisa dipakai untuk logika (cari lagu terkait, mix, profil). */
fun isUsableArtist(artist: String): Boolean = displayArtist(artist).isNotEmpty()
