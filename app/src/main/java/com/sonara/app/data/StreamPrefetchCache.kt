package com.sonara.app.data

/**
 * Cache in-memory stream hasil prefetch (v1.10.15, multi-slot
 * v1.10.35) — beberapa lagu ke depan, sesuai keputusan v1.10.1:
 * URL stream TIDAK PERNAH dipersist ke disk (URL YouTube
 * kedaluwarsa ~6 jam; URL basi adalah penyebab klasik error putar
 * instan).
 *
 * v1.10.35: SATU slot (v1.10.15) hanya menutup lagu pertama
 * antrean — lompat ke lagu ke-2/ke-3, atau prefetch yang tertimpa
 * sebelum dipakai, selalu jatuh ke cold resolve. Kini map
 * berkapasitas [maxSlots] (default 4: 3 target prefetch + 1 ruang
 * untuk lagu state yang di-kick lebih dulu): [put] menggantikan
 * entri id yang sama dan mengusir yang TERLAMA bila penuh;
 * [take] mengonsumsi HANYA entri id yang diminta (saudara-saudaranya
 * tidak terganggu); entri kedaluwarsa dibuang saat dicek.
 *
 * Kelas MURNI (tanpa dependensi Android) supaya bisa diuji di JVM
 * biasa bersama JamModel. Akses disinkronkan: prefetch (IO) dan
 * jalur putar (Main) menyentuh slot yang sama.
 *
 * TTL default 5 menit: cukup untuk lagu berikutnya di antrean sesi,
 * cukup pendek supaya URL tidak sempat basi di tangan tamu.
 */
class StreamPrefetchCache(
    private val ttlMs: Long = 300_000L,
    private val maxSlots: Int = 4,
) {

    data class Entry(
        val videoId: String,
        val url: String,
        val durationMs: Long,
        val cachedAtMs: Long,
    )

    /** Urutan penyisipan — kunci pertama = yang terlama (korban eviksi). */
    private val slots = LinkedHashMap<String, Entry>()

    /** True bila ada entri segar untuk videoId ini (tidak mengonsumsi). */
    @Synchronized
    fun isFresh(videoId: String, nowMs: Long): Boolean {
        val e = slots[videoId] ?: return false
        return nowMs - e.cachedAtMs < ttlMs
    }

    /** Simpan hasil resolve; menggantikan entri id yang sama, mengusir yang terlama bila penuh. */
    @Synchronized
    fun put(videoId: String, url: String, durationMs: Long, nowMs: Long) {
        if (videoId.isBlank() || url.isBlank()) return
        // Buang entri kedaluwarsa dulu supaya slot tidak terbuang.
        val expired = slots.entries.filter { nowMs - it.value.cachedAtMs >= ttlMs }.map { it.key }
        for (key in expired) slots.remove(key)
        // Masukkan sebagai entri terbaru.
        slots.remove(videoId)
        while (slots.size >= maxSlots) {
            val eldest = slots.keys.firstOrNull() ?: break
            slots.remove(eldest)
        }
        slots[videoId] = Entry(videoId, url, durationMs, nowMs)
    }

    /**
     * Ambil + konsumsi entri segar untuk videoId ini, atau null bila
     * tidak ada / videoId berbeda / kedaluwarsa. Entri kedaluwarsa
     * dibuang saat dicek supaya tidak dipakai nanti. Entri id lain
     * TIDAK PERNAH tersentuh (v1.10.35 — dulu satu slot dipakai
     * bersama, take() untuk satu lagu bisa membuang lagu lain
     * yang belum sempat dipakai).
     */
    @Synchronized
    fun take(videoId: String, nowMs: Long): Entry? {
        val e = slots[videoId] ?: return null
        if (nowMs - e.cachedAtMs >= ttlMs) {
            slots.remove(videoId)
            return null
        }
        slots.remove(videoId)
        return e
    }

    @Synchronized
    fun clear() {
        slots.clear()
    }
}
