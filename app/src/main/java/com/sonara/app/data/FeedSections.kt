package com.sonara.app.data

/**
 * Logika MURNI tumpukan Home + Browse baru (v1.10.18) — tanpa
 * Android, bisa diuji di JVM. TIDAK ADA data karangan: semua seksi
 * dibangun dari hasil pencarian YTM asli atau data lokal asli
 * (riwayat + play_counts_v1); kosong → seksi disembunyikan pemanggil
 * (fungsi di sini mengembalikan list kosong / null, bukan placeholder).
 */

// ---- Chip mood (Home) -----------------------------------------------------

/** Chip mood di bawah header Home (tap-only, seperti segmen ARUS). */
enum class MoodChip { ALL, FEEL_GOOD, RELAX, COMMUTE, ENERGIZE }

/** Label chip mood (Inggris, seperti UI). */
fun moodChipLabel(mood: MoodChip): String = when (mood) {
    MoodChip.ALL -> "All"
    MoodChip.FEEL_GOOD -> "Feel good"
    MoodChip.RELAX -> "Relax"
    MoodChip.COMMUTE -> "Commute"
    MoodChip.ENERGIZE -> "Energize"
}

/** Satu query pencarian + konteks region/bahasa untuk satu mood. */
data class MoodQuery(val query: String, val gl: String, val hl: String)

/**
 * Query untuk tiap mood. v1.10.25: SEMUA mood memakai query
 * berbahasa Indonesia + gl=ID/hl=id lewat jalur Kotlin yang dipaksa
 * (forceKotlin di call site) — pola Charts 10.19 yang terbukti:
 * bahasa query-lah yang menentukan campuran, dan mesin Go
 * mengabaikan gl/hl. "All" = dua query feed Beranda (bukan lagi
 * feed global US). Feed global tetap ada lewat chip Global di
 * Charts (ChartRegion.GLOBAL, query Inggris + gl=US/hl=en).
 * Probe live 23 Nov 2026 (harness125/feeds_probe_after.json):
 * 0 lagu India di semua 8 feed, 84/115 lagu Indonesia.
 */
fun moodQueries(mood: MoodChip): List<MoodQuery> = when (mood) {
    MoodChip.ALL -> listOf(
        MoodQuery(HOME_TRENDING_QUERY, "ID", "id"),
        MoodQuery(HOME_TOP_QUERY, "ID", "id"),
    )
    MoodChip.FEEL_GOOD -> listOf(MoodQuery(MOOD_FEEL_GOOD_QUERY, "ID", "id"))
    MoodChip.RELAX -> listOf(MoodQuery(MOOD_RELAX_QUERY, "ID", "id"))
    MoodChip.COMMUTE -> listOf(MoodQuery(MOOD_COMMUTE_QUERY, "ID", "id"))
    MoodChip.ENERGIZE -> listOf(MoodQuery(MOOD_ENERGIZE_QUERY, "ID", "id"))
}

/** Query feed Beranda (v1.10.25, probe harness125): "lagu viral Indonesia" → 0 IN, 11/15 ID. */
const val HOME_TRENDING_QUERY = "lagu viral Indonesia"

/** Query feed Beranda (v1.10.25, probe harness125): "lagu terpopuler Indonesia" → 0 IN, 12/14 ID. */
const val HOME_TOP_QUERY = "lagu terpopuler Indonesia"

/** Query mood Feel good (v1.10.25): "lagu senang Indonesia" → 0 IN, 11/14 ID. */
const val MOOD_FEEL_GOOD_QUERY = "lagu senang Indonesia"

/** Query mood Relax (v1.10.25): "lagu santai Indonesia" → 0 IN, 14/15 ID. */
const val MOOD_RELAX_QUERY = "lagu santai Indonesia"

/** Query mood Commute (v1.10.25): "lagu perjalanan Indonesia" → 0 IN, 8/12 ID. */
const val MOOD_COMMUTE_QUERY = "lagu perjalanan Indonesia"

/** Query mood Energize (v1.10.25): "lagu semangat Indonesia" → 0 IN, 9/15 ID. */
const val MOOD_ENERGIZE_QUERY = "lagu semangat Indonesia"

// ---- Perakitan seksi dari hasil search ------------------------------------

/**
 * Gabung lagu yang sama jadi SATU baris — seperti screenshot
 * Beranda/Cari. Kunci: judul + artis ternormalisasi (huruf kecil,
 * tanpa tanda baca). Urutan = kemunculan pertama; track pertama
 * dipertahankan. Dipakai untuk menghilangkan duplikat antar
 * daftar (mis. trending + disukai memuat lagu yang sama).
 *
 * v1.10.19: pindah dari UiCommon (murni — bisa diuji JVM) + dedupe
 * per id DULU: video yang sama dengan artis ter-parse berbeda antar
 * jalur (Go = nama asli, Kotlin lama = label) punya kunci judul+artis
 * berbeda dan lolos sebagai dua baris dengan key id sama → crash
 * LazyColumn. Kemunculan pertama dipertahankan.
 */
fun mergeTracks(tracks: List<Track>): List<Pair<Track, Set<Source>>> {
    fun norm(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() || it == ' ' }
            .split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    val seenIds = HashSet<String>()
    val order = ArrayList<String>()
    val byKey = LinkedHashMap<String, Pair<Track, MutableSet<Source>>>()
    for (t in tracks) {
        if (!seenIds.add(t.id)) continue
        val key = norm(t.title) + "|" + norm(t.artist)
        val existing = byKey[key]
        if (existing == null) {
            order.add(key)
            byKey[key] = t to mutableSetOf(t.source)
        } else {
            existing.second.add(t.source)
        }
    }
    return order.map { byKey.getValue(it).let { (t, s) -> t to s.toSet() } }
}

/** Dedup berdasarkan id lagu (judul+artis sama dari dua sumber tetap satu), urutan dipertahankan. */
fun dedupeTracks(tracks: List<Track>, limit: Int): List<Track> {
    if (limit <= 0) return emptyList()
    val seen = LinkedHashSet<String>()
    val out = ArrayList<Track>()
    for (t in tracks) {
        if (seen.add(t.id)) out.add(t)
        if (out.size >= limit) break
    }
    return out
}

/** Kartu poster "Hits today" dari feed (artwork asli, tap = putar). */
fun posterTracks(feed: List<Track>, limit: Int = 8): List<Track> =
    dedupeTracks(feed, limit)

/**
 * Lagu untuk kartu bertinta "Community Picks": 3 lagu pertama dari
 * SATU hasil pencarian bertema. Label UI jujur (bukan personalisasi)
 * — lihat HomeScreen. Kosong → kartu disembunyikan.
 */
fun communityPicksTracks(results: List<Track>, limit: Int = 3): List<Track> =
    dedupeTracks(results, limit)

// ---- Mix + recap dari data lokal ------------------------------------------

/**
 * "Archive Mix": riwayat terakhir dulu, lalu lagu teratas dari
 * play_counts yang belum masuk — campuran riwayat + hitungan putar.
 * Kosong (tidak ada riwayat DAN tidak ada hitungan) → list kosong
 * (seksi disembunyikan, tanpa mix karangan).
 */
fun archiveMixTracks(
    recent: List<Track>,
    counts: List<PlayCount>,
    limit: Int = 10,
): List<Track> {
    val merged = ArrayList<Track>()
    merged.addAll(recent)
    for (pc in mostPlayedTracks(counts, limit)) {
        merged.add(pc.track)
    }
    return dedupeTracks(merged, limit)
}

/** "Replay Mix": lagu teratas dari play_counts_v1 saja. */
fun replayMixTracks(counts: List<PlayCount>, limit: Int = 10): List<Track> =
    mostPlayedTracks(counts, limit).map { it.track }

/** Satu kartu recap (warna digambar sendiri di UI, bukan aset YTM). */
data class RecapCard(
    /** "Most played" / "Recently played" — tanpa klaim rentang waktu. */
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
)

/**
 * Kartu "Your recap": kartu 1 = lagu teratas (play_counts_v1),
 * kartu 2 = terakhir diputar (recent_tracks). Data kosong → tidak
 * ada kartu (seksi disembunyikan). Tidak ada rentang bulan/tanggal.
 */
fun recapCards(counts: List<PlayCount>, recent: List<Track>): List<RecapCard> {
    val cards = ArrayList<RecapCard>()
    val top = replayMixTracks(counts, limit = 5)
    if (top.isNotEmpty()) {
        cards.add(
            RecapCard(
                title = "Most played",
                subtitle = "Your most played songs",
                tracks = top,
            ),
        )
    }
    val recentDeduped = dedupeTracks(recent, limit = 5)
    if (recentDeduped.isNotEmpty()) {
        cards.add(
            RecapCard(
                title = "Recently played",
                subtitle = "Songs you played recently",
                tracks = recentDeduped,
            ),
        )
    }
    return cards
}

// ---- Charts (sub-halaman Browse) ------------------------------------------

enum class ChartRegion { INDONESIA, GLOBAL }

/** Parameter region Charts: Indonesia = gl=ID/hl=id, Global = gl=US/hl=en. */
fun chartRegionParams(region: ChartRegion): MoodQuery = when (region) {
    ChartRegion.INDONESIA -> MoodQuery("", "ID", "id")
    ChartRegion.GLOBAL -> MoodQuery("", "US", "en")
}

/**
 * Query Charts per region (v1.10.19, hasil probe live 22 Nov 2026).
 * gl/hl saja TIDAK mengubah campuran hasil untuk query Inggris generik
 * ("trending songs" gl=ID → 2/18 lagu Indonesia, 3 India, sisanya
 * global): bahasa query-lah yang menentukan. Chip Indonesia memakai
 * query berbahasa Indonesia ("lagu trending Indonesia" → 14/15
 * Indonesia, 0 India; "top lagu Indonesia" → 11/15, 0 India);
 * chip Global tetap query Inggris generik.
 */
data class ChartQueries(val trendingQuery: String, val topQuery: String)

fun chartQueries(region: ChartRegion): ChartQueries = when (region) {
    ChartRegion.INDONESIA -> ChartQueries(
        trendingQuery = "lagu trending Indonesia",
        topQuery = "top lagu Indonesia",
    )
    ChartRegion.GLOBAL -> ChartQueries(
        trendingQuery = "trending songs",
        topQuery = "top songs today",
    )
}

/**
 * PROVENANCE JUJUR Charts: app TIDAK punya endpoint chart resmi
 * (search YTM hanya mengembalikan lagu per query — lihat
 * YoutubeMusicApi: YtmResult tanpa tipe playlist/album/chart dan
 * tanpa angka apa pun). Charts dibangun dari pencarian
 * trending/top; subjudul ini tampil di UI Charts.
 */
const val CHARTS_PROVENANCE = "Built from trending searches on YouTube Music"

/** Satu baris berperingkat (rank = posisi di daftar yang dikembalikan API). */
data class RankedTrack(val rank: Int, val track: Track)

/** Satu artis berperingkat + lagu perwakilannya (kemunculan pertama). */
data class RankedArtist(val rank: Int, val artist: String, val topTrack: Track)

/** Urutan rank: dedup + nomor 1..N (tanpa angka karangan lain). */
fun rankTracks(tracks: List<Track>, limit: Int): List<RankedTrack> =
    dedupeTracks(tracks, limit).mapIndexed { i, t -> RankedTrack(i + 1, t) }

/**
 * Artis teratas dari daftar lagu: dikelompokkan per artis (nama
 * dinormalisasi trim + lowercase), urutan = kemunculan pertama di
 * daftar, perwakilan = lagu pertama artis itu. Tanpa angka
 * subscriber/putar (API tidak mengembalikannya).
 */
fun topArtistsFromTracks(tracks: List<Track>, limit: Int): List<RankedArtist> {
    if (limit <= 0) return emptyList()
    val seen = LinkedHashMap<String, Track>()
    for (t in dedupeTracks(tracks, Int.MAX_VALUE)) {
        // v1.10.19: artis label tipe / kosong dilewati (data lama).
        val key = displayArtist(t.artist).lowercase()
        if (key.isEmpty()) continue
        seen.putIfAbsent(key, t)
    }
    return seen.values.take(limit).mapIndexed { i, t ->
        RankedArtist(i + 1, displayArtist(t.artist), t)
    }
}

/** Data Charts lengkap dari dua hasil pencarian (trending + top). */
data class ChartData(
    val trending: List<RankedTrack>,
    val dailyTop: List<RankedTrack>,
    val topArtists: List<RankedArtist>,
)

fun buildChartData(
    trendingResults: List<Track>,
    topResults: List<Track>,
): ChartData {
    val trending = rankTracks(trendingResults, limit = 20)
    // v1.10.19: lagu yang sama di kedua query HANYA tampil di Trending
    // (Daily Top di-dedupe + di-rank ulang) — dua section berbagi SATU
    // LazyColumn, dan id+rank yang sama di dua section = crash key
    // duplikat (P0 22 Nov 2026).
    val trendingIds = trending.map { it.track.id }.toSet()
    val dailyTop = rankTracks(
        topResults.filterNot { it.id in trendingIds },
        limit = 20,
    )
    val artists = topArtistsFromTracks(trendingResults + topResults, limit = 5)
    return ChartData(trending, dailyTop, artists)
}

// ---- Explore + Moods & genres (Browse) ------------------------------------

/** Tile Explore di Browse (ikon dari set ikon app sendiri di UI). */
enum class ExploreDestination { NEW_RELEASES, CHARTS, MOODS_AND_GENRES }

data class ExploreTile(
    val destination: ExploreDestination,
    val title: String,
    val subtitle: String,
)

val EXPLORE_TILES = listOf(
    ExploreTile(ExploreDestination.NEW_RELEASES, "New releases", "Fresh songs, just out"),
    ExploreTile(ExploreDestination.CHARTS, "Charts", "What's trending now"),
    ExploreTile(ExploreDestination.MOODS_AND_GENRES, "Moods & genres", "Find your vibe"),
)

/** Satu grup grid di sub-halaman Moods & genres (label = query Inggris). */
data class MoodGroup(val title: String, val tiles: List<String>)

val MOOD_GROUPS = listOf(
    MoodGroup(
        "For you",
        listOf("Family", "Romance", "Commute", "Jazz", "1970s", "Focus"),
    ),
    MoodGroup(
        "Moods & moments",
        listOf("Chill", "Energize", "Feel good", "Party", "Sad", "Sleep", "Workout", "Gaming"),
    ),
    MoodGroup(
        "Genres",
        listOf("Pop", "Rock", "Hip-Hop", "R&B", "Metal", "Indie", "K-Pop", "Dangdut", "Latin", "Classical"),
    ),
)

/**
 * Query pencarian rilisan baru (v1.10.19: query Indonesia + gl=ID/hl=id
 * jalur Kotlin — probe live: "new releases" gl=US → 0/16 lagu Indonesia;
 * "lagu baru Indonesia" gl=ID → 12/15 Indonesia, 0 India) + workout.
 */
const val NEW_RELEASES_QUERY = "lagu baru Indonesia"

/**
 * Query Community Picks (v1.10.25: query Indonesia + gl=ID/hl=id jalur
 * Kotlin — probe live: "lagu favorit Indonesia" → 0 IN, 13/15 ID;
 * sebelumnya "community favorites songs" gl=US → 0 IN, 0/15 ID).
 */
const val COMMUNITY_PICKS_QUERY = "lagu favorit Indonesia"

/**
 * Query Workout (v1.10.25: query Indonesia + gl=ID/hl=id jalur Kotlin
 * — probe live: "lagu workout Indonesia" → 0 IN, 6/15 ID; campuran
 * sisanya musik gym generik, bukan lagu India).
 */
const val WORKOUT_QUERY = "lagu workout Indonesia"
