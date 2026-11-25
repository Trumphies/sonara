package com.sonara.app.data

/**
 * Kebijakan murni sleep timer (v1.10.44) — tanpa import Android,
 * dikompilasi standalone oleh harness144. Mesinnya tetap
 * [com.sonara.app.player.SleepTimer] lewat
 * `PlayerManager.setSleepTimer`; file ini hanya menghitung tampilan
 * (pilihan menit + sisa waktu) dari `endsAtMs` yang sama.
 */

/** Pilihan menit yang sama di Settings (Playback) dan di pill detail. */
val SLEEP_TIMER_CHOICES_MINUTES: List<Int> = listOf(0, 15, 30, 60)

/**
 * Sisa waktu dalam menit penuh (pembulatan KE ATAS, min 1 selama
 * masih aktif), atau null bila timer tidak aktif / sudah habis.
 * Pembulatan ke atas supaya pill tidak pernah menampilkan "0m"
 * padahal timer masih jalan.
 */
fun sleepRemainingMinutes(endsAtMs: Long?, nowMs: Long): Int? {
    if (endsAtMs == null) return null
    val remainingMs = endsAtMs - nowMs
    if (remainingMs <= 0L) return null
    return ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
}

/** Label sisa waktu ("24m") atau null bila timer tidak aktif. */
fun sleepRemainingLabel(endsAtMs: Long?, nowMs: Long): String? {
    val minutes = sleepRemainingMinutes(endsAtMs, nowMs) ?: return null
    return "${minutes}m"
}
