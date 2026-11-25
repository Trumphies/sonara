package com.sonara.app.data

/**
 * Pemilihan artwork HD — PURE Kotlin (tanpa dependensi Android) supaya
 * harness bisa mengompilasinya mandiri.
 *
 * Latar (probe live 25 Nov 2026, WEB_REMIX search + fixture
 * harness131): array thumbnail YTM terurut menaik DAN puncaknya
 * bervariasi — 11/33 array segar mentok di 120x120, sisanya 400/544/
 * 1200. Kode lama mengambil elemen TERAKHIR (parseItem, largestThumb)
 * atau lebar-terbesar (detail, GoEngine): benar di array menaik, rapuh
 * bila urutan berubah. Di sini seleksinya eksplisit lebar-terbesar.
 *
 * URL lh3/yt3 googleusercontent memakai SATU id gambar yang sama;
 * hanya akhiran `=wXX-hXX` yang berbeda (terbukti di respons live:
 * id yang sama dilayani di 60/120/226/544). [upgradeThumbUrl] meminta
 * ukuran 544 yang TERBUKTI dilayani untuk id yang sama — bukan URL
 * karangan, bukan upscale: layanan gambar yang sama, parameter ukuran
 * yang sama dipakai klien web YTM. Hanya untuk URL yang SUDAH membawa
 * parameter `=w..-h..` di host googleusercontent, hanya bila persegi
 * (w == h) dan masih di bawah 544; URL lain (i.ytimg, radioart, tanpa
 * parameter, non-persegi, atau sudah >= 544) tidak disentuh.
 * Fallback berantai pemanggil (hqdefault dkk.) tidak diubah.
 */
data class ThumbCandidate(val width: Int, val url: String)

/** Kandidat lebar-terbesar; seri -> yang terakhir; lebar tak dikenal (<=0) kalah dari yang dikenal. */
fun pickLargestThumbUrl(candidates: List<ThumbCandidate>): String? {
    var bestWidth = 0
    var bestUrl: String? = null
    var lastUrl: String? = null
    for (c in candidates) {
        val url = c.url.trim()
        if (url.isEmpty()) continue
        lastUrl = url
        if (c.width > bestWidth) {
            bestWidth = c.width
            bestUrl = url
        } else if (c.width == bestWidth && c.width > 0) {
            bestUrl = url
        }
    }
    // Bila tak ada lebar yang dikenal, pakai yang terakhir (perilaku lama).
    return bestUrl ?: lastUrl
}

private val SIZE_PARAM = Regex("=w(\\d+)-h(\\d+)")

/** Cap ukuran sisi yang terbukti dilayani untuk thumb persegi googleusercontent. */
const val THUMB_UPGRADE_PX = 544

fun upgradeThumbUrl(url: String): String {
    if (!url.contains("googleusercontent.com/")) return url
    val m = SIZE_PARAM.find(url) ?: return url
    val w = m.groupValues[1].toIntOrNull() ?: return url
    val h = m.groupValues[2].toIntOrNull() ?: return url
    if (w <= 0 || w != h || w >= THUMB_UPGRADE_PX) return url
    return url.replaceRange(m.range, "=w$THUMB_UPGRADE_PX-h$THUMB_UPGRADE_PX")
}

/** Satu pintu: pilih yang terbesar lalu naikkan parameter ukurannya. */
fun selectArtworkUrl(candidates: List<ThumbCandidate>): String? {
    val picked = pickLargestThumbUrl(candidates) ?: return null
    return upgradeThumbUrl(picked)
}
