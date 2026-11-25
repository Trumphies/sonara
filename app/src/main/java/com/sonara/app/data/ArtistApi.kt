package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Profil artis + History (v1.10.18).
 *
 * Semua data dari respons InnerTube WEB_REMIX yang SUDAH dipakai app
 * (endpoint /browse yang sama dengan jalur lirik). Tidak ada data
 * karangan: nama/artwork/about/lagu/album/artis-serupa HANYA dari
 * JSON yang dikembalikan API; angka (subscriber, play count, view
 * count) TIDAK PERNAH ditampilkan meskipun ada di respons.
 *
 * Fungsi parse di sini MURNI (menerima JSONObject) supaya bisa diuji
 * dari fixture JSON nyata tanpa jaringan. Fungsi fetch bersifat
 * blocking — pemanggil WAJIB memanggilnya dari Dispatchers.IO,
 * seperti pemanggilan YoutubeMusicApi lainnya.
 */
object ArtistApi {

    /** Satu album/single/EP dari rak "Albums" / "Singles & EPs". */
    data class AlbumItem(
        val title: String,
        /** Tahun/teks subtitle apa adanya dari API (mis. "2026"). */
        val subtitle: String,
        val imageUrl: String?,
        val browseId: String?,
        /**
         * "Album" / "Single" / "EP" HANYA bila kata itu benar-benar
         * ada di subtitle respons. Subtitle yang hanya berisi tahun
         * (kasus umum rak Albums) → "" = tanpa label tipe.
         */
        val kindLabel: String,
    )

    data class SimilarArtist(
        val name: String,
        val imageUrl: String?,
        val browseId: String?,
    )

    data class ArtistProfile(
        val name: String,
        val browseId: String,
        val imageUrl: String?,
        /** Teks "About" verbatim dari API; null bila tidak dikembalikan. */
        val aboutText: String?,
        val topSongs: List<YoutubeMusicApi.YtmResult>,
        val albums: List<AlbumItem>,
        val similarArtists: List<SimilarArtist>,
        /** True = halaman ini fallback hasil pencarian, BUKAN profil browse. */
        val isSearchFallback: Boolean,
        /** Terisi hanya saat fallback: hasil pencarian nama artis. */
        val searchTracks: List<Track> = emptyList(),
    )

    /** Satu blok isi sheet History: teks VERBATIM + label jenis/sumber. */
    data class HistoryBlock(
        val kind: HistoryKind,
        /** Label jenis blok, mis. "From the video description". */
        val label: String,
        /** Teks verbatim dari API. */
        val text: String,
    )

    enum class HistoryKind { VIDEO_DESCRIPTION, ARTIST_ABOUT }

    data class HistoryContent(val blocks: List<HistoryBlock>)

    // ---------- helpers JSON (mirip YoutubeMusicApi, mandiri) ----------

    /** Gabungkan runs[].text — endpoint navigasi sengaja dibuang. */
    fun runsText(textObj: JSONObject?): String? {
        if (textObj == null) return null
        val runs = textObj.optJSONArray("runs") ?: return textObj.optString("simpleText", null)
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
        }
        val out = sb.toString()
        return out.ifBlank { null }
    }

    /**
     * Thumbnail terbesar. Bentuk yang ditemui di respons: pemanggil
     * meneruskan wrapper `thumbnail` / `thumbnailRenderer` yang
     * bentuknya {musicThumbnailRenderer: {thumbnail: {thumbnails}}},
     * atau parent yang membawa kunci `thumbnail`. Rantai ke-2
     * (v1.10.18, perbaikan bug harness118b): tanpa rantai ini semua
     * artwork profil/album/serupa selalu null — pemanggil tidak
     * pernah meneruskan parent, sehingga rantai pertama tidak
     * pernah kena.
     */
    fun largestThumb(node: JSONObject?): String? {
        if (node == null) return null
        val thumbs = node.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: node.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            ?: node.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: node.optJSONArray("thumbnails")
            ?: return null
        if (thumbs.length() == 0) return null
        val candidates = ArrayList<ThumbCandidate>()
        for (i in 0 until thumbs.length()) {
            val t = thumbs.optJSONObject(i) ?: continue
            candidates += ThumbCandidate(t.optInt("width", 0), t.optString("url", ""))
        }
        return selectArtworkUrl(candidates)
    }

    private fun browseIdOfRun(run: JSONObject?): String? =
        run?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?.optString("browseId", null)?.takeIf { it.isNotBlank() }

    private fun firstRunWithBrowseId(textObj: JSONObject?): Pair<String, String?>? {
        if (textObj == null) return null
        val runs = textObj.optJSONArray("runs") ?: return null
        for (i in 0 until runs.length()) {
            val run = runs.optJSONObject(i) ?: continue
            val text = run.optString("text", "")
            val bid = browseIdOfRun(run)
            if (bid != null) return text to bid
        }
        return null
    }

    /** Nama artis pertama dari string "A, B, C" (ketuk baris artis). */
    fun firstArtistName(artist: String): String =
        artist.split(",").firstOrNull()?.trim().orEmpty().ifBlank { artist.trim() }

    // ---------- resolve browseId artis dari hasil search ----------

    /**
     * Cari browseId artis (UC…) dari JSON respons search mentah.
     * Preferensi: run yang teksnya SAMA PERSIS dengan nama artis
     * (judul card-shelf / run artis di list item). Bila tidak ada yang
     * persis, null — pemanggil memakai fallback halaman pencarian.
     * Terverifikasi dari fixture search "Tory Lanez" (22 Nov 2026):
     * satu-satunya pasangan (teks, browseId) adalah
     * ("Tory Lanez", UCYKF8xDQoRGUAFr867VQUTQ).
     */
    fun resolveArtistBrowseId(searchRoot: JSONObject, artistName: String): String? {
        val wanted = artistName.trim()
        if (wanted.isEmpty()) return null
        val pairs = ArrayList<Pair<String, String>>()
        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    val runs = node.optJSONArray("runs")
                    if (runs != null) {
                        for (i in 0 until runs.length()) {
                            val run = runs.optJSONObject(i) ?: continue
                            val bid = browseIdOfRun(run) ?: continue
                            if (bid.startsWith("UC")) {
                                pairs.add(run.optString("text", "") to bid)
                            }
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
        walk(searchRoot)
        // 1) teks sama persis (case-insensitive)
        pairs.firstOrNull { it.first.equals(wanted, ignoreCase = true) }?.let { return it.second }
        // 2) run artis pertama yang teksnya diawali nama artis
        // (mis. "Tory Lanez" di antara run lain) — tetap UC yang sama.
        pairs.firstOrNull { it.first.trim().equals(wanted, ignoreCase = true) }?.let { return it.second }
        return null
    }

    // ---------- parse halaman browse artis ----------

    /**
     * Parse respons /browse artis. Null bila tidak ada header nama
     * artis (respons bukan halaman artis) — pemanggil memakai
     * fallback halaman pencarian.
     */
    fun parseArtistBrowse(root: JSONObject, browseId: String): ArtistProfile? {
        var header: JSONObject? = null
        fun findHeader(node: Any?) {
            if (header != null) return
            when (node) {
                is JSONObject -> {
                    if (node.has("musicImmersiveHeaderRenderer")) {
                        header = node.getJSONObject("musicImmersiveHeaderRenderer")
                        return
                    }
                    if (node.has("musicDetailHeaderRenderer")) {
                        header = node.getJSONObject("musicDetailHeaderRenderer")
                        return
                    }
                    val keys = node.keys()
                    while (keys.hasNext() && header == null) findHeader(node.get(keys.next()))
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        if (header != null) return
                        findHeader(node.get(i))
                    }
                }
            }
        }
        findHeader(root)
        val name = header?.let { runsText(it.optJSONObject("title")) } ?: return null
        val imageUrl = header?.let { largestThumb(it.optJSONObject("thumbnail")) }

        // About: musicDescriptionShelfRenderer yang header-nya "About".
        var aboutText: String? = null
        val topSongs = ArrayList<YoutubeMusicApi.YtmResult>()
        val albums = ArrayList<AlbumItem>()
        val similar = ArrayList<SimilarArtist>()

        fun visitShelves(node: Any?) {
            when (node) {
                is JSONObject -> {
                    if (node.has("musicDescriptionShelfRenderer")) {
                        val shelf = node.getJSONObject("musicDescriptionShelfRenderer")
                        val shelfHeader = runsText(shelf.optJSONObject("header"))
                        if (shelfHeader == "About" && aboutText == null) {
                            aboutText = runsText(shelf.optJSONObject("description"))
                                ?.trim()?.takeIf { it.isNotEmpty() }
                        }
                    }
                    if (node.has("musicShelfRenderer")) {
                        val shelf = node.getJSONObject("musicShelfRenderer")
                        val title = runsText(shelf.optJSONObject("title"))
                        if (title == "Top songs") {
                            val contents = shelf.optJSONArray("contents")
                            if (contents != null) {
                                for (i in 0 until contents.length()) {
                                    val item = contents.optJSONObject(i)
                                        ?.optJSONObject("musicResponsiveListItemRenderer")
                                        ?: continue
                                    parseListItem(item, name)?.let { topSongs.add(it) }
                                }
                            }
                        }
                    }
                    if (node.has("musicCarouselShelfRenderer")) {
                        val shelf = node.getJSONObject("musicCarouselShelfRenderer")
                        val shelfHeader = shelf.optJSONObject("header")
                            ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                            ?.optJSONObject("title")
                        val title = runsText(shelfHeader)
                        val contents = shelf.optJSONArray("contents")
                        if (contents != null && (title == "Albums" || title == "Singles & EPs")) {
                            for (i in 0 until contents.length()) {
                                val twoRow = contents.optJSONObject(i)
                                    ?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                                parseAlbumItem(twoRow)?.let { albums.add(it) }
                            }
                        }
                        if (contents != null && title == "Fans might also like") {
                            for (i in 0 until contents.length()) {
                                val twoRow = contents.optJSONObject(i)
                                    ?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                                val titlePair = firstRunWithBrowseId(twoRow.optJSONObject("title"))
                                val nm = runsText(twoRow.optJSONObject("title")) ?: continue
                                // v1.10.19: dedupe nama (key LazyColumn =
                                // nama) — rak yang sama bisa ter-walk dua kali.
                                if (similar.any { it.name == nm }) continue
                                val bid = titlePair?.second
                                    ?: twoRow.optJSONObject("navigationEndpoint")
                                        ?.optJSONObject("browseEndpoint")
                                        ?.optString("browseId", null)
                                similar.add(
                                    SimilarArtist(
                                        name = nm,
                                        imageUrl = largestThumb(twoRow.optJSONObject("thumbnailRenderer")),
                                        browseId = bid?.takeIf { it.startsWith("UC") },
                                    ),
                                )
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

        return ArtistProfile(
            name = name,
            browseId = browseId,
            imageUrl = imageUrl,
            aboutText = aboutText,
            topSongs = topSongs,
            // v1.10.19: dedupe judul+subtitle (key LazyColumn) — rak yang
            // sama bisa ter-walk dua kali; kemunculan pertama dipertahankan.
            albums = albums.distinctBy { it.title + it.subtitle },
            similarArtists = similar,
            isSearchFallback = false,
        )
    }

    /** Baris lagu dari rak "Top songs" — bentuk sama dengan hasil search. */
    fun parseListItem(
        item: JSONObject,
        fallbackArtist: String,
    ): YoutubeMusicApi.YtmResult? {
        var videoId: String? = null
        fun findVideo(node: Any?) {
            if (videoId != null) return
            when (node) {
                is JSONObject -> {
                    val we = node.optJSONObject("watchEndpoint")
                    if (we != null) {
                        val id = we.optString("videoId", null)?.takeIf { it.isNotBlank() }
                        if (id != null) { videoId = id; return }
                    }
                    val keys = node.keys()
                    while (keys.hasNext() && videoId == null) findVideo(node.get(keys.next()))
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        if (videoId != null) return
                        findVideo(node.get(i))
                    }
                }
            }
        }
        findVideo(item)
        val vid = videoId ?: return null
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
        var artist = fallbackArtist
        var durationMs = 0L
        var explicit = false
        if (columns.length() > 1) {
            val second = colText(1) ?: ""
            if (second.isNotBlank()) {
                // v1.10.19: lewati label tipe seperti parseItem; bila
                // tidak ada bagian non-label, fallbackArtist dipertahankan
                // (nama artis profil) — label tidak pernah menimpanya.
                val extracted = extractArtistFromSubtitle(second)
                if (extracted.isNotEmpty()) artist = extracted
                for (p in second.split(" • ")) {
                    parseDuration(p)?.let { durationMs = it }
                }
                explicit = second.contains("Explicit", ignoreCase = true)
            }
        }
        var imageUrl: String? = largestThumb(item.optJSONObject("thumbnail"))
        if (imageUrl.isNullOrBlank()) imageUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
        return YoutubeMusicApi.YtmResult(vid, title, artist, durationMs, false, explicit, imageUrl)
    }

    /** Item rak Albums/Singles — kindLabel hanya dari kata di subtitle. */
    fun parseAlbumItem(twoRow: JSONObject): AlbumItem? {
        val title = runsText(twoRow.optJSONObject("title")) ?: return null
        val subtitle = runsText(twoRow.optJSONObject("subtitle")) ?: ""
        val kindLabel = when {
            subtitle.contains("Single", ignoreCase = true) -> "Single"
            subtitle.contains("Album", ignoreCase = true) -> "Album"
            subtitle.contains("EP", ignoreCase = true) -> "EP"
            else -> ""
        }
        val bid = firstRunWithBrowseId(twoRow.optJSONObject("title"))?.second
            ?: twoRow.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId", null)
        return AlbumItem(
            title = title,
            subtitle = subtitle,
            imageUrl = largestThumb(twoRow.optJSONObject("thumbnailRenderer")),
            browseId = bid?.takeIf { it.isNotBlank() },
            kindLabel = kindLabel,
        )
    }

    /** "3:45" / "4.32" → ms; null bila bukan durasi. */
    fun parseDuration(text: String): Long? {
        val t = text.trim()
        val m = Regex("""^(\d+)[.:](\d{2})$""").matchEntire(t) ?: return null
        val min = m.groupValues[1].toLongOrNull() ?: return null
        val sec = m.groupValues[2].toLongOrNull() ?: return null
        if (sec >= 60) return null
        return (min * 60 + sec) * 1000
    }

    // ---------- fetch (blocking; panggil dari Dispatchers.IO) ----------

    /**
     * Ambil profil artis. Jalur: browseId yang diberikan → resolve
     * lewat search (forceKotlin, gl/hl dihormati) → browse. Bila
     * semua gagal, kembalikan halaman fallback hasil pencarian
     * (isSearchFallback = true) — BUKAN profil palsu.
     */
    fun fetchArtistProfile(
        api: YoutubeMusicApi,
        artistName: String,
        browseId: String?,
        gl: String = "ID",
        hl: String = "id",
    ): ArtistProfile {
        val name = firstArtistName(artistName)
        var bid = browseId?.takeIf { it.isNotBlank() }
        if (bid == null) {
            // Resolve lewat search mentah (Kotlin) supaya gl/hl
            // dihormati — Go mengabaikan region.
            bid = resolveFromSearch(api, name, gl, hl)
        }
        if (bid != null) {
            val root = api.browse(bid, gl, hl).getOrNull()
            if (root != null) {
                val profile = runCatching { parseArtistBrowse(root, bid) }.getOrNull()
                if (profile != null) return profile
            }
        }
        // Fallback jujur: hasil pencarian nama artis.
        val tracks = api.search(name, gl, hl, forceKotlin = true)
            .getOrElse { emptyList() }
        return ArtistProfile(
            name = name,
            browseId = bid ?: "",
            imageUrl = null,
            aboutText = null,
            topSongs = emptyList(),
            albums = emptyList(),
            similarArtists = emptyList(),
            isSearchFallback = true,
            searchTracks = tracks,
        )
    }

    /**
     * Resolve browseId dari hasil search mentah (Kotlin): hanya
     * dipakai bila run artis hasil itu bernama SAMA dengan nama yang
     * dicari — nama ambigu tidak boleh salah ambil artis lain.
     */
    private fun resolveFromSearch(
        api: YoutubeMusicApi,
        name: String,
        gl: String,
        hl: String,
    ): String? {
        val results = api.searchRaw(name, gl, hl, forceKotlin = true).getOrNull()
            ?: return null
        return results.firstOrNull {
            it.artist.equals(name, ignoreCase = true) && it.artistBrowseId != null
        }?.artistBrowseId
    }

    /**
     * Rakit isi sheet History untuk satu lagu: deskripsi video
     * (verbatim) + About artis (verbatim). Blok yang datanya kosong
     * tidak dibuat — bila keduanya kosong, blocks kosong = empty
     * state jujur di UI.
     */
    fun fetchHistory(
        api: YoutubeMusicApi,
        track: Track,
    ): HistoryContent {
        val blocks = ArrayList<HistoryBlock>()
        val videoId = track.youtubeVideoId
        if (!videoId.isNullOrBlank()) {
            val desc = runCatching { api.fetchVideoDescription(videoId) }.getOrNull()
            if (!desc.isNullOrBlank()) {
                blocks.add(
                    HistoryBlock(
                        kind = HistoryKind.VIDEO_DESCRIPTION,
                        label = "From the video description",
                        text = desc,
                    ),
                )
            }
        }
        val profile = runCatching {
            fetchArtistProfile(api, track.artist, track.artistBrowseId)
        }.getOrNull()
        val about = profile?.aboutText?.takeIf { it.isNotBlank() }
        if (about != null) {
            blocks.add(
                HistoryBlock(
                    kind = HistoryKind.ARTIST_ABOUT,
                    label = "About the artist",
                    text = about,
                ),
            )
        }
        return HistoryContent(blocks)
    }
}
