package com.sonara.app.data

/**
 * Matematika warna artwork MURNI (tanpa dependensi Android/Compose) —
 * dipakai layar detail dan diuji harness JVM.
 *
 * Latar detail selalu gelap: warna turunan dari artwork harus menjaga
 * hue-nya tapi menahan value tetap rendah, supaya teks putih/sekunder
 * yang warnanya TETAP (DetailTextPrimary/Secondary, DetailAccent)
 * selalu terbaca. Tidak ada foreground adaptif — semua adaptasi ada
 * di sisi warna latar (kebalikan yang rapuh).
 */

/** Luminans relatif WCAG (sRGB) untuk warna ARGB pak (alpha diabaikan). */
fun relativeLuminance(argb: Int): Double {
    fun lin(channel: Int): Double {
        val s = channel / 255.0
        return if (s <= 0.04045) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * lin((argb shr 16) and 0xFF) +
        0.7152 * lin((argb shr 8) and 0xFF) +
        0.0722 * lin(argb and 0xFF)
}

/** Rasio kontras WCAG antara dua warna ARGB pak. */
fun contrastRatio(argb1: Int, argb2: Int): Double {
    val l1 = relativeLuminance(argb1)
    val l2 = relativeLuminance(argb2)
    val hi = maxOf(l1, l2)
    val lo = minOf(l1, l2)
    return (hi + 0.05) / (lo + 0.05)
}

/** Skala kanal RGB dengan [factor] (0..1), hue dipertahankan. */
private fun scaleRgb(argb: Int, factor: Double): Int {
    val a = (argb ushr 24) and 0xFF
    fun scale(channel: Int): Int =
        (channel * factor).toInt().coerceIn(0, 255)
    return (a shl 24) or
        (scale((argb shr 16) and 0xFF) shl 16) or
        (scale((argb shr 8) and 0xFF) shl 8) or
        scale(argb and 0xFF)
}

/**
 * Latar panel detail dari rata-rata mentah artwork: hue dipertahankan,
 * value ditahan ke bawah sampai luminans <= [maxLuminance] (pita gelap
 * yang selama ini dipakai panel tetap #3D3119/#403517). Dipaksa opak.
 *
 * Plafon 0.055 dipilih dari kontras KETIGA foreground tetap: putih
 * (>=4.5), sekunder (DetailTextSecondary, >=4.5, termasuk varian
 * alpha 0.8 di caption), dan aksen oranye (DetailAccent, >=3.0 untuk
 * bar indikator non-teks). Plafon yang lebih longgar (0.085) terbukti
 * harness MENGGAGALKAN aksen (rasio ~2.45) — jangan dilonggarkan
 * tanpa menghitung ulang ketiganya.
 */
fun derivePanelColor(avgArgb: Int, maxLuminance: Double = 0.055): Int {
    var c = avgArgb or (0xFF shl 24)
    var guard = 0
    while (relativeLuminance(c) > maxLuminance && guard < 40) {
        c = scaleRgb(c, 0.9)
        guard++
    }
    return c
}

/**
 * Latar mini-header: panel dicerahkan satu tingkat (campur 16% menuju
 * putih), tetap di bawah plafon luminans 0.09 — selalu lebih terang
 * dari panel tapi tetap gelap. Plafon 0.09 menjaga kontras sekunder
 * (DetailTextSecondary, artis di mini-header) >= 4.5.
 */
fun deriveMiniHeaderColor(panelArgb: Int, maxLuminance: Double = 0.09): Int {
    fun mix(channel: Int): Int =
        (channel + (255 - channel) * 0.16).toInt().coerceIn(0, 255)
    val a = (panelArgb ushr 24) and 0xFF
    var c = (a shl 24) or
        (mix((panelArgb shr 16) and 0xFF) shl 16) or
        (mix((panelArgb shr 8) and 0xFF) shl 8) or
        mix(panelArgb and 0xFF)
    var guard = 0
    while (relativeLuminance(c) > maxLuminance && guard < 40) {
        c = scaleRgb(c, 0.9)
        guard++
    }
    return c
}
