package com.sonara.app.data

/**
 * Kebijakan kecepatan putar — PURE Kotlin (v1.0.5).
 *
 * Kepemilikan rate (satu-satunya titik kontrol tetap
 * PlayerManager.setPlaybackRate):
 * - Di luar Jam: rate pilihan user. Lagu baru mulai dengan rate
 *   per-episode bila tersimpan (hanya konten panjang), else rate
 *   default user.
 * - Selama sesi Jam: rate MILIK sesi (koreksi drift 0.98×/1.02×) —
 *   pilihan user diabaikan sampai sesi berakhir; UI kecepatan
 *   disembunyikan selama Jam (bukan sekadar dinonaktifkan).
 * - Embed (IFrame) tidak mendukung setPlaybackSpeed yang andal —
 *   kecepatan hanya berlaku di jalur ExoPlayer; fallback embed
 *   memutar 1.0× (dicatat jujur di BUILD doc).
 */
object PlaybackSpeedPolicy {

    /** Pilihan kecepatan di dialog: 0.5×–2.0×. */
    val SPEED_CHOICES: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    /** Rate normal. */
    const val DEFAULT_RATE: Float = 1.0f

    /** Batas clamp pilihan. */
    const val MIN_RATE: Float = 0.5f
    const val MAX_RATE: Float = 2.0f

    /** Label tampil: "1.0×" / "0.75×" / "1.25×". */
    fun formatSpeed(rate: Float): String {
        val r = coerceRate(rate)
        val text = if (r == r.toInt().toFloat() || (r * 2) == (r * 2).toInt().toFloat()) {
            "%.1f".format(r)
        } else {
            "%.2f".format(r)
        }
        return "${text}×"
    }

    /** Jepit ke rentang pilihan. */
    fun coerceRate(rate: Float): Float = rate.coerceIn(MIN_RATE, MAX_RATE)

    /** True bila track tergolong konten panjang (ambang yang sama dengan posisi-lanjut). */
    fun isLongForm(durationMs: Long): Boolean = durationMs >= RESUME_MIN_DURATION_MS

    /**
     * Rate lagu baru: Jam aktif → 1.0× (sesi yang memiliki rate);
     * else rate per-episode bila ada, else default user.
     */
    fun effectiveStartRate(
        jamActive: Boolean,
        perEpisodeRate: Float?,
        defaultRate: Float,
    ): Float {
        if (jamActive) return DEFAULT_RATE
        if (perEpisodeRate != null) return coerceRate(perEpisodeRate)
        return coerceRate(defaultRate)
    }
}
