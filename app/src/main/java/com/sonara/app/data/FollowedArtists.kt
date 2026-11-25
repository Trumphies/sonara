package com.sonara.app.data

/**
 * Artis yang diikuti + rak rilisan MURNI (v1.10.21, tanpa Android —
 * bisa diuji di JVM). Himpunan diikuti disimpan LOKAL SAJA (tidak
 * disinkron — cakupan sync tidak berubah). Tanpa background work /
 * notifikasi: rak diambil saat Home dibuka.
 */

/** Satu artis yang diikuti. [key] stabil: browseId bila ada, else nama. */
data class FollowedArtist(
    val key: String,
    val name: String,
    val browseId: String?,
    val followedAtMs: Long,
)

/** Kunci follow: `uc:<browseId>` bila browseId ada, else nama ternormalisasi. */
fun followKey(name: String, browseId: String?): String =
    if (!browseId.isNullOrBlank()) "uc:${browseId.trim()}"
    else "name:${name.trim().lowercase()}"

/** TTL cache rilisan per artis: refresh paling cepat 1 jam sekali. */
const val FOLLOWED_CACHE_TTL_MS: Long = 3_600_000L

/** Jumlah artis maksimum yang di-refresh dalam satu kali buka Home. */
const val FOLLOWED_REFRESH_MAX_ARTISTS: Int = 5

/** Jumlah item maksimum di rak "New from artists you follow". */
const val FOLLOWED_SHELF_MAX_ITEMS: Int = 10

/** True bila cache artis ini sudah basi (atau belum pernah diambil). */
fun followedCacheStale(fetchedAtMs: Long?, nowMs: Long): Boolean {
    if (fetchedAtMs == null || fetchedAtMs <= 0L) return true
    return nowMs - fetchedAtMs >= FOLLOWED_CACHE_TTL_MS
}

/**
 * Gabung rilisan per artis menjadi daftar rak: round-robin (satu
 * lagu per artis bergiliran) supaya satu artis tidak menguasai rak,
 * dedupe per id (kemunculan pertama), dibatasi [limit]. Daftar
 * kosong dari artis yang tidak mengembalikan apa pun dilewati —
 * tidak ada konten palsu.
 */
fun mergeFollowedReleases(
    perArtist: List<List<Track>>,
    limit: Int = FOLLOWED_SHELF_MAX_ITEMS,
): List<Track> {
    val out = ArrayList<Track>(limit)
    val seen = HashSet<String>()
    var index = 0
    var progressed: Boolean
    do {
        progressed = false
        for (list in perArtist) {
            if (out.size >= limit) break
            if (index < list.size) {
                progressed = true
                val t = list[index]
                if (seen.add(t.id)) out.add(t)
            }
        }
        index++
    } while (progressed && out.size < limit)
    return out
}
