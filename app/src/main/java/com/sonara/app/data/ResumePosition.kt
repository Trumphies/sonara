package com.sonara.app.data

/**
 * Keputusan posisi-lanjut podcast/konten panjang MURNI (v1.10.21,
 * tanpa Android — bisa diuji di JVM).
 *
 * Aturan (dicatat di BUILD-v1.10.21.md):
 * - Hanya track berdurasi >= 10 menit.
 * - Posisi tersimpan dipakai bila > 10 detik DAN < durasi - 15 detik.
 * - Penyimpanan: posisi < 10 detik tidak disimpan; posisi >=
 *   durasi - 15 detik MENGHAPUS posisi tersimpan (track hampir
 *   habis = mulai dari 0 lain kali); seek sengaja ke < 10 detik
 *   juga menghapus.
 * - Nonaktif selama sesi Jam (posisi milik sesi) — gate-nya di
 *   PlayerManager, bukan di sini.
 */

/** Durasi minimum track agar posisinya disimpan/dipulihkan. */
const val RESUME_MIN_DURATION_MS: Long = 600_000L

/** Posisi di bawah ini dianggap "baru mulai" — tidak disimpan/dipakai. */
const val RESUME_MIN_POSITION_MS: Long = 10_000L

/** Margin akhir: posisi di dalam margin ini = track dianggap habis. */
const val RESUME_END_MARGIN_MS: Long = 15_000L

/** Interval simpan berkala saat memutar. */
const val RESUME_SAVE_INTERVAL_MS: Long = 5_000L

/**
 * Posisi mulai untuk track ini, atau null bila mulai dari 0.
 * [savedMs] null/<=0 = tidak ada posisi tersimpan.
 */
fun resumePositionFor(savedMs: Long?, durationMs: Long): Long? {
    if (savedMs == null || savedMs <= 0L) return null
    if (durationMs < RESUME_MIN_DURATION_MS) return null
    if (savedMs <= RESUME_MIN_POSITION_MS) return null
    if (savedMs >= durationMs - RESUME_END_MARGIN_MS) return null
    return savedMs
}

/**
 * Nilai yang harus disimpan untuk [positionMs] saat ini, atau null
 * yang berarti JANGAN simpan (dan hapus bila sudah tersimpan):
 * durasi terlalu pendek, posisi terlalu awal, atau posisi sudah
 * hampir habis.
 */
fun positionToStore(positionMs: Long, durationMs: Long): Long? {
    if (durationMs < RESUME_MIN_DURATION_MS) return null
    if (positionMs <= RESUME_MIN_POSITION_MS) return null
    if (positionMs >= durationMs - RESUME_END_MARGIN_MS) return null
    return positionMs
}
