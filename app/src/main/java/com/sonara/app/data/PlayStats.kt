package com.sonara.app.data

/**
 * Logika MURNI (tanpa Android) untuk v1.10.16 — bisa diuji di JVM:
 * statistik putar, riwayat pencarian, dan pemilihan baris kartu lirik.
 *
 * Kejujuran data: hitungan putar HANYA berasal dari pemutaran nyata
 * yang dicatat [LocalStore.recordPlay] (riwayat "Terakhir diputar"
 * yang lama tidak punya timestamp/hitungan — daftarnya dedup
 * maks 20 — sehingga frekuensi TIDAK BISA diturunkan darinya dan
 * tidak dikarang; label UI karenanya "Most played" tanpa klaim
 * rentang waktu).
 */

/** Satu lagu + berapa kali benar-benar mulai diputar + kapan terakhir. */
data class PlayCount(
    val track: Track,
    val count: Int,
    val lastPlayedAtMs: Long,
)

/** Satu artis + total putar semua lagunya + kapan terakhir diputar. */
data class ArtistPlays(
    val artist: String,
    val plays: Int,
    val lastPlayedAtMs: Long,
)

/** Lagu paling sering diputar: hitungan turun, lalu terakhir diputar turun. */
fun mostPlayedTracks(counts: List<PlayCount>, limit: Int): List<PlayCount> {
    if (limit <= 0) return emptyList()
    return counts
        .filter { it.count > 0 }
        .sortedWith(
            compareByDescending<PlayCount> { it.count }
                .thenByDescending { it.lastPlayedAtMs },
        )
        // v1.10.19: satu id satu baris (buku hitungan lama bisa
        // mengandung id ganda) — key LazyColumn harus unik.
        .distinctBy { it.track.id }
        .take(limit)
}

/**
 * Artis paling sering diputar: lagu dikelompokkan per artis
 * (nama dinormalisasi trim + lowercase; nama tampil diambil dari
 * lagu teratas grup itu), total putar dijumlah, urutan sama dengan
 * [mostPlayedTracks].
 */
fun mostPlayedArtists(counts: List<PlayCount>, limit: Int): List<ArtistPlays> {
    if (limit <= 0) return emptyList()
    val groups = LinkedHashMap<String, MutableList<PlayCount>>()
    for (pc in counts) {
        // v1.10.19: artis label tipe / kosong (data lama) tidak membentuk grup.
        val artist = displayArtist(pc.track.artist)
        if (artist.isEmpty() || pc.count <= 0) continue
        groups.getOrPut(artist.lowercase()) { mutableListOf() }.add(pc)
    }
    return groups.values.mapNotNull { group ->
        if (group.isEmpty()) return@mapNotNull null
        val top = group.maxWithOrNull(
            compareBy<PlayCount> { it.count }.thenBy { it.lastPlayedAtMs },
        ) ?: return@mapNotNull null
        ArtistPlays(
            artist = displayArtist(top.track.artist),
            plays = group.sumOf { it.count },
            lastPlayedAtMs = group.maxOf { it.lastPlayedAtMs },
        )
    }
        .sortedWith(
            compareByDescending<ArtistPlays> { it.plays }
                .thenByDescending { it.lastPlayedAtMs },
        )
        .take(limit)
}

/**
 * Daftar riwayat pencarian setelah satu query baru: query di-trim,
 * diabaikan bila < 2 karakter, duplikat (case-insensitive) dipindah
 * ke depan dengan ejaan terbaru, dibatasi [max] entri.
 */
fun addRecentSearchQuery(
    existing: List<String>,
    query: String,
    max: Int = 8,
): List<String> {
    val q = query.trim()
    if (q.length < 2) return existing
    val deduped = existing.filterNot { it.equals(q, ignoreCase = true) }
    return (listOf(q) + deduped).take(max.coerceAtLeast(0))
}

/**
 * Baris lirik yang masuk kartu lirik (teks ASLI, tidak diedit):
 * - SYNCED: baris aktif saat ini; bila belum ada baris aktif
 *   (posisi 0), baris pertama (yang akan dinyanyikan).
 * - PLAIN: 4 baris tak-kosong pertama.
 * - EMPTY: tidak ada baris.
 */
fun selectLyricCardLines(
    kind: LyricsApi.Kind,
    lines: List<LyricsApi.Line>,
    activeIndex: Int,
): List<String> {
    return when (kind) {
        LyricsApi.Kind.SYNCED -> {
            val idx = if (activeIndex in lines.indices) activeIndex else 0
            val text = lines.getOrNull(idx)?.text?.takeIf { it.isNotBlank() }
            if (text != null) listOf(text) else emptyList()
        }
        LyricsApi.Kind.PLAIN ->
            lines.map { it.text }.filter { it.isNotBlank() }.take(4)
        LyricsApi.Kind.EMPTY -> emptyList()
    }
}
