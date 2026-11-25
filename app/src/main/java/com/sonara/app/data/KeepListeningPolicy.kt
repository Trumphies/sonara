package com.sonara.app.data

/**
 * Rak "Keep listening" — PURE Kotlin (v1.0.5).
 *
 * Sumbernya [ResumeSnapshot]: posisi-lanjut yang disimpan PlayerManager
 * bersama cuplikan identitas track (judul/artis/artwork/durasi), supaya
 * rak bisa dirender tanpa memutar ulang / menebak metadata. Entri
 * tanpa cuplikan (data lama sebelum v1.0.5) tidak pernah masuk rak —
 * tanpa konten palsu.
 *
 * Aturan tampil: aktivitas terbaru dulu, episode yang ditandai
 * "played" dikeluarkan, dibatasi [limit]. "Played" TIDAK mengembalikan
 * posisi saat un-mark (posisi sudah dihapus saat mark — mulai dari 0
 * dan posisi baru tersimpan saat didengarkan lagi).
 */

/** Cuplikan track + posisi tersimpan untuk rak Keep listening. */
data class ResumeSnapshot(
    val trackId: String,
    val videoId: String?,
    val title: String,
    val artist: String,
    val showTitle: String?,
    val imageUrl: String?,
    val durationMs: Long,
    val savedAtMs: Long,
    val positionMs: Long,
    /** browseId acara MPSP… ("" bila tidak diketahui) — tombol artis di detail. */
    val showBrowseId: String = "",
)

object KeepListeningPolicy {

    /** Jumlah maksimum entri snapshot yang disimpan (yang terlama dibuang). */
    const val MAX_SNAPSHOTS: Int = 20

    /** Jumlah maksimum baris di rak. */
    const val SHELF_LIMIT: Int = 10

    /** Daftar rak: terbaru dulu, tanpa yang sudah played, dibatasi. */
    fun keepListeningList(
        snapshots: List<ResumeSnapshot>,
        playedIds: Set<String>,
        limit: Int = SHELF_LIMIT,
    ): List<ResumeSnapshot> {
        return snapshots
            .filter { it.trackId !in playedIds && it.positionMs > 0L }
            .sortedByDescending { it.savedAtMs }
            .take(limit)
    }

    /** Sisa waktu episode dalam ms (>= 0). */
    fun remainingMs(snapshot: ResumeSnapshot): Long =
        (snapshot.durationMs - snapshot.positionMs).coerceAtLeast(0L)
}
