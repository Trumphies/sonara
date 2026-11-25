package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Halaman genre ala YouTube Music (v1.10.33) — parsing MURNI dari
 * JSONObject, tanpa dependensi jaringan, supaya bisa diuji dari
 * fixture respons mentah (harness133).
 *
 * Dua jalur, keduanya data REAL:
 * 1. Browse (utama): tombol genre di halaman Explore
 *    (browseId FEmusic_explore) membawa browseEndpoint
 *    FEmusic_moods_and_genres_category + params opak per genre.
 *    Params TIDAK bisa dibangun dari label (token opak server) —
 *    harus ditemukan saat runtime dari respons Explore, lalu
 *    di-cache per sesi di SonaraState. Label yang tidak punya
 *    tombol di Explore jatuh ke jalur 2.
 * 2. Search (fallback): pencarian biasa label itu; Songs = hasil
 *    bertipe lagu, Music videos = sisanya. Seksi playlist/album
 *    TIDAK dibuat-buat — halaman fallback hanya menampilkan seksi
 *    yang datanya benar-benar ada.
 *
 * TIDAK ADA judul lagu/playlist/album yang di-hardcode di sini:
 * semua baris berasal dari respons. Judul rak dicocokkan dari
 * daftar judul yang diketahui (Indonesia + Inggris, sesuai hl
 * yang diminta) — rak tak dikenal dilewati, bukan ditebak.
 */
object GenreApi {

    /** Satu lagu di rak Songs halaman genre. */
    data class GenreSong(
        val videoId: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val imageUrl: String?,
        val explicit: Boolean,
        val artistBrowseId: String? = null,
    )

    /** Satu kartu di rak playlist/video/album (twoRow item). */
    data class GenreCardItem(
        val title: String,
        val subtitle: String,
        val imageUrl: String?,
        /** browseId koleksi (VL… playlist / MPRE… album); null untuk video. */
        val browseId: String?,
        /** videoId; hanya untuk rak Music videos. */
        val videoId: String?,
    )

    /** Isi halaman genre. [fromBrowse]=false berarti komposisi fallback. */
    data class GenrePageData(
        val title: String,
        val songs: List<GenreSong>,
        val featured: List<GenreCardItem>,
        val community: List<GenreCardItem>,
        val videos: List<GenreCardItem>,
        val albums: List<GenreCardItem>,
        val fromBrowse: Boolean,
    )

    /** browseId halaman kategori genre di Explore. */
    const val GENRE_CATEGORY_BROWSE_ID = "FEmusic_moods_and_genres_category"

    /** browseId halaman Explore (sumber tombol genre + params). */
    const val EXPLORE_BROWSE_ID = "FEmusic_explore"

    /** Batas tampil per rak — sama dengan batas hasil search (v1.10.18). */
    const val SECTION_LIMIT = 20

    // ---- pencocokan judul rak (normalisasi: huruf kecil, alfanumerik) ----

    private fun norm(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }

    private val SONG_TITLES = setOf("lagu", "songs")
    private val FEATURED_TITLES = setOf("playlistunggulan", "featuredplaylists")
    private val COMMUNITY_TITLES = setOf("playlistkomunitas", "communityplaylists")
    private val VIDEO_TITLES = setOf("videomusik", "musicvideos")
    private val ALBUM_TITLES = setOf("album", "albums")

    // ---- alias label app -> label tombol Explore (normalisasi) ----
    //
    // Tombol Explore berbahasa Indonesia (hl=id). Label app berbahasa
    // Inggris; yang tidak punya padanan (Dangdut, Heartbreak,
    // Podcasts, 1970s) SENGAJA tidak dipetakan — "Decades" bukan
    // "1970s", jadi tidak dipaksakan. Tanpa padanan = jalur fallback.
    private val LABEL_ALIASES: Map<String, Set<String>> = mapOf(
        "hiphop" to setOf("hiphop"),
        "rb" to setOf("rbsoul"),
        "indie" to setOf("indiealternatif"),
        "electronic" to setOf("danceelektronik"),
        "acoustic" to setOf("folkakustik"),
        "classical" to setOf("musikklasik"),
        "chill" to setOf("santai"),
        "sad" to setOf("sedih"),
        "sleep" to setOf("tidur"),
        "focus" to setOf("fokus"),
        "party" to setOf("parti"),
        "workout" to setOf("olahraga"),
        "feelgood" to setOf("merasasenang"),
        "energize" to setOf("semangat"),
        "family" to setOf("keluarga"),
        "romance" to setOf("romantis"),
        "commute" to setOf("bepergian"),
    )

    /**
     * Params kategori genre untuk [label] dari respons Explore yang
     * sudah diambil, atau null bila label itu tidak punya tombol
     * (→ pemanggil memakai jalur fallback search). MURNI.
     */
    fun resolveGenreParams(exploreRoot: JSONObject, label: String): String? {
        val wanted = norm(label)
        val candidates = (LABEL_ALIASES[wanted] ?: emptySet()) + wanted
        var found: String? = null
        fun walk(node: Any?) {
            if (found != null) return
            when (node) {
                is JSONObject -> {
                    val btn = node.optJSONObject("musicNavigationButtonRenderer")
                    if (btn != null) {
                        val btnLabel = norm(runsText(btn.optJSONObject("buttonText")) ?: "")
                        val endpoint = btn.optJSONObject("clickCommand")
                            ?.optJSONObject("browseEndpoint")
                        if (endpoint != null &&
                            endpoint.optString("browseId") == GENRE_CATEGORY_BROWSE_ID &&
                            btnLabel in candidates
                        ) {
                            found = endpoint.optString("params", null)?.takeIf { it.isNotBlank() }
                            return
                        }
                    }
                    val keys = node.keys()
                    while (keys.hasNext() && found == null) walk(node.get(keys.next()))
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        if (found != null) return
                        walk(node.get(i))
                    }
                }
            }
        }
        walk(exploreRoot)
        return found
    }

    /**
     * Parse halaman kategori genre (browse FEmusic_moods_and_genres_category).
     * Mengembalikan null hanya bila respons tidak punya struktur yang
     * dikenali sama sekali (payload error/kosong). Rak tak dikenal
     * dilewati; rak kosong dipertahankan kosong (bukan diisi).
     */
    fun parseGenreBrowse(root: JSONObject, titleFallback: String): GenrePageData? {
        var headerTitle: String? = null
        val songs = ArrayList<GenreSong>()
        val featured = ArrayList<GenreCardItem>()
        val community = ArrayList<GenreCardItem>()
        val videos = ArrayList<GenreCardItem>()
        val albums = ArrayList<GenreCardItem>()
        val seenShelves = HashSet<String>()

        fun visitShelves(node: Any?) {
            when (node) {
                is JSONObject -> {
                    if (node.has("musicHeaderRenderer")) {
                        if (headerTitle == null) {
                            headerTitle = runsText(
                                node.getJSONObject("musicHeaderRenderer").optJSONObject("title"),
                            )?.takeIf { it.isNotBlank() }
                        }
                    }
                    if (node.has("musicCarouselShelfRenderer")) {
                        val shelf = node.getJSONObject("musicCarouselShelfRenderer")
                        val title = runsText(
                            shelf.optJSONObject("header")
                                ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                                ?.optJSONObject("title"),
                        )
                        val kind = when (norm(title ?: "")) {
                            in SONG_TITLES -> "songs"
                            in FEATURED_TITLES -> "featured"
                            in COMMUNITY_TITLES -> "community"
                            in VIDEO_TITLES -> "videos"
                            in ALBUM_TITLES -> "albums"
                            else -> null
                        }
                        if (kind != null && seenShelves.add(kind)) {
                            val contents = shelf.optJSONArray("contents")
                            if (contents != null) {
                                when (kind) {
                                    "songs" -> {
                                        for (i in 0 until contents.length()) {
                                            val item = contents.optJSONObject(i)
                                                ?.optJSONObject("musicResponsiveListItemRenderer")
                                                ?: continue
                                            parseGenreSong(item)?.let { songs.add(it) }
                                        }
                                    }
                                    else -> {
                                        val target = when (kind) {
                                            "featured" -> featured
                                            "community" -> community
                                            "videos" -> videos
                                            else -> albums
                                        }
                                        for (i in 0 until contents.length()) {
                                            val twoRow = contents.optJSONObject(i)
                                                ?.optJSONObject("musicTwoRowItemRenderer")
                                                ?: continue
                                            parseCardItem(twoRow)?.let { target.add(it) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val keys = node.keys()
                    while (keys.hasNext()) visitShelves(node.get(keys.next()))
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) visitShelves(node.get(i))
                }
            }
        }
        visitShelves(root)
        if (headerTitle == null && songs.isEmpty() && featured.isEmpty() &&
            community.isEmpty() && videos.isEmpty() && albums.isEmpty()
        ) {
            return null
        }
        return GenrePageData(
            title = headerTitle ?: titleFallback,
            songs = songs.distinctBy { it.videoId }.take(SECTION_LIMIT),
            featured = featured.distinctBy { it.title + (it.browseId ?: "") }.take(SECTION_LIMIT),
            community = community.distinctBy { it.title + (it.browseId ?: "") }.take(SECTION_LIMIT),
            videos = videos.distinctBy { it.videoId ?: it.title }.take(SECTION_LIMIT),
            albums = albums.distinctBy { it.title + (it.browseId ?: "") }.take(SECTION_LIMIT),
            fromBrowse = true,
        )
    }

    /** Baris lagu rak Songs — bentuk sama dengan hasil search. */
    private fun parseGenreSong(item: JSONObject): GenreSong? {
        val videoId = findVideoId(item) ?: return null
        val columns = item.optJSONArray("flexColumns") ?: return null
        if (columns.length() < 1) return null
        val title = runsText(
            columns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text"),
        ) ?: return null
        var artist = ""
        var durationMs = 0L
        var explicit = false
        if (columns.length() > 1) {
            val second = runsText(
                columns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text"),
            ) ?: ""
            artist = extractArtistFromSubtitle(second)
            for (p in second.split(" • ")) {
                parseDuration(p)?.let { durationMs = it }
            }
            explicit = second.contains("Explicit", ignoreCase = true)
        }
        var imageUrl: String? = largestThumb(item.optJSONObject("thumbnail"))
        if (imageUrl.isNullOrBlank()) imageUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        val artistBrowseId = artistBrowseIdFrom(item)
        return GenreSong(videoId, title, artist, durationMs, imageUrl, explicit, artistBrowseId)
    }

    /** Kartu twoRow (playlist/video/album) — browseId/videoId dari endpoint. */
    private fun parseCardItem(twoRow: JSONObject): GenreCardItem? {
        val title = runsText(twoRow.optJSONObject("title")) ?: return null
        val subtitle = runsText(twoRow.optJSONObject("subtitle")) ?: ""
        val endpoint = twoRow.optJSONObject("navigationEndpoint")
        val browseId = endpoint?.optJSONObject("browseEndpoint")
            ?.optString("browseId", null)?.takeIf { it.isNotBlank() }
        val videoId = endpoint?.optJSONObject("watchEndpoint")
            ?.optString("videoId", null)?.takeIf { it.isNotBlank() }
            ?: findVideoId(twoRow.optJSONObject("thumbnailRenderer"))
        return GenreCardItem(
            title = title,
            subtitle = subtitle,
            imageUrl = largestThumb(twoRow.optJSONObject("thumbnailRenderer")),
            browseId = browseId,
            videoId = videoId,
        )
    }

    /**
     * Daftar track koleksi (browse playlist VL… / album MPRE…):
     * semua musicResponsiveListItemRenderer di respons. Kolom
     * koleksi berbeda dari kolom search (artis di kolom 1, durasi
     * di kolom akhir, kolom hitungan penonton di antaranya) — jadi
     * durasi dicari di SEMUA kolom, dan artis hanya dari kolom 1
     * yang bukan teks hitungan penonton.
     */
    fun parseCollectionItems(root: JSONObject): List<GenreSong> {
        val out = ArrayList<GenreSong>()
        val seen = HashSet<String>()
        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    val item = node.optJSONObject("musicResponsiveListItemRenderer")
                    if (item != null) {
                        parseCollectionItem(item)?.let {
                            if (seen.add(it.videoId)) out.add(it)
                        }
                    }
                    val keys = node.keys()
                    while (keys.hasNext()) walk(node.get(keys.next()))
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) walk(node.get(i))
                }
            }
        }
        walk(root)
        return out
    }

    private fun parseCollectionItem(item: JSONObject): GenreSong? {
        val videoId = findVideoId(item) ?: return null
        val columns = item.optJSONArray("flexColumns") ?: return null
        if (columns.length() < 1) return null
        fun colText(idx: Int): String? {
            if (idx >= columns.length()) return null
            return runsText(
                columns.getJSONObject(idx)
                    .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text"),
            )
        }
        val title = colText(0) ?: return null
        var artist = ""
        var durationMs = 0L
        var explicit = false
        for (i in 1 until columns.length()) {
            val text = colText(i) ?: continue
            if (i == 1 && !looksLikeViewCount(text)) {
                artist = extractArtistFromSubtitle(text)
            }
            if (durationMs == 0L) {
                for (p in text.split(" • ")) {
                    parseDuration(p)?.let { durationMs = it }
                }
            }
            if (text.contains("Explicit", ignoreCase = true)) explicit = true
        }
        val fixed = item.optJSONArray("fixedColumns")
        if (fixed != null && durationMs == 0L) {
            for (i in 0 until fixed.length()) {
                val t = runsText(
                    fixed.getJSONObject(i)
                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text"),
                )
                parseDuration(t ?: "")?.let { durationMs = it }
            }
        }
        var imageUrl: String? = largestThumb(item.optJSONObject("thumbnail"))
        if (imageUrl.isNullOrBlank()) imageUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        return GenreSong(
            videoId, title, artist, durationMs, imageUrl, explicit,
            artistBrowseIdFrom(item),
        )
    }

    /** "56 jt pemutaran" / "540 x ditonton" — bukan nama artis. */
    private fun looksLikeViewCount(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("ditonton") || t.contains("pemutaran")
    }

    /** browseId artis (UC…) dari run kolom artis — pola parseItem. */
    private fun artistBrowseIdFrom(item: JSONObject): String? {
        val columns = item.optJSONArray("flexColumns") ?: return null
        if (columns.length() < 2) return null
        val runs = columns.getJSONObject(1)
            .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs") ?: return null
        for (i in 0 until runs.length()) {
            val bid = runs.getJSONObject(i)
                .optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId", null)
                ?.takeIf { it.isNotBlank() }
            if (bid != null && bid.startsWith("UC")) return bid
        }
        return null
    }

    private fun findVideoId(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                if (node.has("videoId")) {
                    val v = node.optString("videoId", "")
                    if (v.isNotEmpty()) return v
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    findVideoId(node.get(keys.next()))?.let { return it }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    findVideoId(node.get(i))?.let { return it }
                }
            }
        }
        return null
    }

    private fun runsText(textObj: JSONObject?): String? {
        if (textObj == null) return null
        val runs = textObj.optJSONArray("runs")
            ?: return textObj.optString("simpleText", null)
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.getJSONObject(i).optString("text", ""))
        }
        return sb.toString().ifEmpty { null }
    }

    /** Thumbnail terbesar dari musicThumbnailRenderer / thumbnailRenderer. */
    private fun largestThumb(node: JSONObject?): String? {
        if (node == null) return null
        val thumbs = node.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: node.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: return null
        if (thumbs.length() == 0) return null
        val candidates = ArrayList<ThumbCandidate>()
        for (i in 0 until thumbs.length()) {
            val t = thumbs.optJSONObject(i) ?: continue
            candidates += ThumbCandidate(t.optInt("width", 0), t.optString("url", ""))
        }
        return selectArtworkUrl(candidates)
    }

    /** "3:45" / "2.59" / "1:02:03" → ms; null bila bukan durasi. */
    private fun parseDuration(s: String): Long? {
        val t = s.trim()
        val normalized = if (t.matches(Regex("\\d{1,2}\\.\\d{2}"))) t.replace(".", ":") else t
        if (!normalized.matches(Regex("\\d{1,2}:\\d{2}(:\\d{2})?"))) return null
        val parts = normalized.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            else -> null
        }
    }

    /** GenreSong → Track pemutar (pemetaan yang sama dengan search()). */
    fun genreSongToTrack(song: GenreSong): Track = Track(
        id = "ytm:${song.videoId}",
        title = song.title,
        artist = song.artist,
        album = "",
        source = Source.YOUTUBE_MUSIC,
        engine = Engine.YOUTUBE,
        durationMs = song.durationMs,
        youtubeVideoId = song.videoId,
        imageUrl = song.imageUrl,
        explicit = song.explicit,
        artistBrowseId = song.artistBrowseId,
    )

    /** Kartu video → Track pemutar (audio, seperti semua baris app). */
    fun cardVideoToTrack(card: GenreCardItem): Track? {
        val videoId = card.videoId ?: return null
        return Track(
            id = "ytm:$videoId",
            title = card.title,
            artist = extractArtistFromSubtitle(card.subtitle),
            album = "",
            source = Source.YOUTUBE_MUSIC,
            engine = Engine.YOUTUBE,
            durationMs = 0,
            youtubeVideoId = videoId,
            imageUrl = card.imageUrl,
        )
    }
}
