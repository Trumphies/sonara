package com.sonara.app.data

import org.json.JSONObject

/**
 * Tema kustom v1.0.7 — murni Kotlin, melapis DI ATAS tabel token
 * [AppThemePalette] yang sudah ada (file itu TIDAK diubah).
 *
 * Pengguna memilih dari pilihan TERBATAS: 8 aksen + 3 skala radius.
 * Setiap kombinasi yang disimpan DIJAMIN lolos batang kontras yang
 * sama dengan audit 10.32/10.39 (teks 4.5:1): aksen peran teks vs
 * background, dan tinta vs isi aksen. Bila aksen pilihan tidak
 * lolos, warnanya digelapkan/dicerakan bertahap sampai lolos —
 * tidak pernah disimpan apa adanya bila gagal. Struktur tema
 * (font, border, treatment, gerak) tetap milik tema dasar.
 */
enum class CustomRadius {
    SHARP,
    BALANCED,
    ROUND,
}

data class CustomThemeOverride(
    val enabled: Boolean = false,
    val accent: Long = 0xFFFF4E24L,
    val radius: CustomRadius = CustomRadius.BALANCED,
) {
    companion object {
        val DEFAULT = CustomThemeOverride()
    }
}

/** Warna hasil resolve: siap dipakai menimpa field palet dasar. */
data class ResolvedCustomTheme(
    val accent: Long,
    val accentFill: Long,
    val accentDark: Long,
    val accentSoft: Long,
    val onAccent: Long,
)

object CustomThemePolicy {
    /** 8 aksen pilihan — semuanya opaque; resolve menjamin kontrasnya. */
    val ACCENT_SWATCHES: List<Long> = listOf(
        0xFFFF4E24L, // Burnt orange (ARUS)
        0xFF7C3AEDL, // Violet
        0xFF0F766EL, // Teal
        0xFF1D4ED8L, // Blue
        0xFFE11D48L, // Rose
        0xFFB45309L, // Amber
        0xFF15803DL, // Green
        0xFFBE185DL, // Pink
    )

    private const val MIN_TEXT_RATIO = 4.5

    private fun red(argb: Long): Int = ((argb shr 16) and 0xFF).toInt()
    private fun green(argb: Long): Int = ((argb shr 8) and 0xFF).toInt()
    private fun blue(argb: Long): Int = (argb and 0xFF).toInt()

    private fun rgb(r: Int, g: Int, b: Int): Long =
        0xFF000000L or (r.coerceIn(0, 255).toLong() shl 16) or
            (g.coerceIn(0, 255).toLong() shl 8) or b.coerceIn(0, 255).toLong()

    private fun darken(argb: Long, factor: Double): Long =
        rgb((red(argb) * factor).toInt(), (green(argb) * factor).toInt(), (blue(argb) * factor).toInt())

    private fun lighten(argb: Long, factor: Double): Long {
        fun up(c: Int): Int = (c + (255 - c) * factor).toInt()
        return rgb(up(red(argb)), up(green(argb)), up(blue(argb)))
    }

    private fun blend(fg: Long, bg: Long, alpha: Double): Long {
        fun mix(a: Int, b: Int): Int = (a * alpha + b * (1.0 - alpha)).toInt()
        return rgb(mix(red(fg), red(bg)), mix(green(fg), green(bg)), mix(blue(fg), blue(bg)))
    }

    /**
     * Geser [color] sampai kontrasnya vs [background] >= [minRatio],
     * mencoba menggelapkan DAN mencerahkan, mengambil yang paling
     * sedikit berubah. Gagal total -> null (pemanggil memakai fallback).
     */
    private fun adjustForContrast(color: Long, background: Long, minRatio: Double): Long? {
        if (contrastRatio(color, background) >= minRatio) return color
        var dark: Long? = null
        var light: Long? = null
        var current = color
        for (step in 1..24) {
            current = darken(current, 0.9)
            if (contrastRatio(current, background) >= minRatio) {
                dark = current
                break
            }
        }
        current = color
        for (step in 1..24) {
            current = lighten(current, 0.1)
            if (contrastRatio(current, background) >= minRatio) {
                light = current
                break
            }
        }
        // Pilih yang perubahannya paling kecil (diukur dari selisih
        // luminans); seri -> gelap (lebih lazim untuk aksen teks).
        return when {
            dark != null && light != null -> {
                val baseLum = relativeLuminance(color)
                val darkDelta = kotlin.math.abs(relativeLuminance(dark) - baseLum)
                val lightDelta = kotlin.math.abs(relativeLuminance(light) - baseLum)
                if (darkDelta <= lightDelta) dark else light
            }
            dark != null -> dark
            else -> light
        }
    }

    /** Tinta di atas isi aksen: hitam/putih, mana yang kontrasnya lolos. */
    private fun deriveOnAccent(fill: Long): Long {
        val darkInk = 0xFF1B1813L
        val lightInk = 0xFFFFFFFFL
        val darkRatio = contrastRatio(darkInk, fill)
        val lightRatio = contrastRatio(lightInk, fill)
        return if (darkRatio >= lightRatio) darkInk else lightInk
    }

    /** Resolve override terhadap palet dasar; tidak pernah melanggar batang. */
    fun resolve(base: AppThemePalette, override: CustomThemeOverride): ResolvedCustomTheme {
        val adjusted = adjustForContrast(override.accent, base.background, MIN_TEXT_RATIO)
        if (adjusted == null) {
            // Fallback total: pasangan aksen tema dasar (sudah diaudit).
            return ResolvedCustomTheme(
                accent = base.accent,
                accentFill = base.accentFill,
                accentDark = base.accentDark,
                accentSoft = base.accentSoft,
                onAccent = base.onAccent,
            )
        }
        val onAccent = deriveOnAccent(adjusted)
        // deriveOnAccent selalu punya pemenang >= ~4.58 (bukti:
        // maks(kontras vs hitam, vs putih) minimum di L~=0.18);
        // bila entah bagaimana gagal, fallback ke pasangan dasar.
        if (contrastRatio(onAccent, adjusted) < MIN_TEXT_RATIO) {
            return ResolvedCustomTheme(
                accent = base.accent,
                accentFill = base.accentFill,
                accentDark = base.accentDark,
                accentSoft = base.accentSoft,
                onAccent = base.onAccent,
            )
        }
        return ResolvedCustomTheme(
            accent = adjusted,
            accentFill = adjusted,
            accentDark = darken(adjusted, 0.75),
            accentSoft = blend(adjusted, base.surface, 0.12),
            onAccent = onAccent,
        )
    }

    /** Skala radius di atas field sudut palet dasar. */
    fun applyRadius(base: AppThemePalette, radius: CustomRadius): AppThemePalette {
        if (radius == CustomRadius.BALANCED) return base
        fun sharp(v: Int): Int = if (v >= 999) 8 else minOf(v, 6)
        // ROUND tidak pernah mengecilkan sudut dasar: radius di bawah
        // 28 naik ke 28, di atasnya tetap (pill >= 999 tetap pill).
        fun round(v: Int): Int = if (v >= 999) 999 else maxOf(v, 28)
        fun map(v: Int): Int = when (radius) {
            CustomRadius.SHARP -> sharp(v)
            CustomRadius.ROUND -> round(v)
            CustomRadius.BALANCED -> v
        }
        return base.copy(
            cornerSmall = map(base.cornerSmall),
            cornerMedium = map(base.cornerMedium),
            cornerLarge = map(base.cornerLarge),
            cornerPoster = map(base.cornerPoster),
            // ROUND: bentuk pill SELALU penuh (999) walau tema dasarnya
            // pill-nya hanya 28 (mis. ARUS Light); SHARP/BALANCED ikut map.
            cornerPill = if (radius == CustomRadius.ROUND) 999 else map(base.cornerPill),
            cornerRowCover = map(base.cornerRowCover),
            cornerCard = map(base.cornerCard),
            cornerTile = map(base.cornerTile),
            cornerChip = map(base.cornerChip),
            cornerSheet = map(base.cornerSheet),
            cornerButton = map(base.cornerButton),
            cornerMiniPlayer = map(base.cornerMiniPlayer),
            cornerDetailCover = if (base.cornerDetailCover < 0) {
                base.cornerDetailCover
            } else {
                map(base.cornerDetailCover)
            },
        )
    }

    fun toJson(override: CustomThemeOverride): String {
        val obj = JSONObject()
        obj.put("e", override.enabled)
        obj.put("a", override.accent)
        obj.put("r", override.radius.name)
        return obj.toString()
    }

    fun fromJson(raw: String?): CustomThemeOverride? {
        if (raw.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(raw)
            if (!obj.has("a")) return null
            val radius = try {
                CustomRadius.valueOf(obj.optString("r", "BALANCED"))
            } catch (_: Exception) {
                CustomRadius.BALANCED
            }
            CustomThemeOverride(
                enabled = obj.optBoolean("e", false),
                accent = obj.optLong("a", 0xFFFF4E24L),
                radius = radius,
            )
        } catch (_: Exception) {
            null
        }
    }
}
