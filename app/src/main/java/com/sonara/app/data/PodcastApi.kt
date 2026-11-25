package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Halaman acara podcast YouTube Music — PURE Kotlin (tanpa Android),
 * parsing dari respons InnerTube WEB_REMIX yang sudah diverifikasi
 * live dari VM 25 Nov 2026 (fixture harness150).
 *
 * Bentuk respons (browseId MPSP…):
 * - Header: `musicResponsiveHeaderRenderer` (judul, strapline = host,
 *   deskripsi, thumbnail persegi playlist).
 * - Episode: `musicMultiRowListItemRenderer` di `musicShelfRenderer`
 *   (judul, subtitle "N x ditonton • tanggal", durasi di
 *   `playbackProgress.durationText` format Indonesia "2 j, 6 mnt",
 *   videoId di onTap/overlay watchEndpoint, deskripsi episode).
 * - Paginasi: `continuations[0].nextContinuationData.continuation` di
 *   shelf; halaman lanjutan di `continuationContents.musicShelfContinuation`.
 *
 * Acara juga muncul di hasil search sebagai
 * `musicResponsiveListItemRenderer` dengan browseEndpoint MPSP… dan
 * kolom kedua "Podcast • <Host>" — [parseSearchShows] menemukannya.
 * Episode di hasil search: kolom kedua "Episode • <tanggal> • <Acara>".
 *
 * Transkrip TIDAK dibangun: panel "Transcript" ada di /next, tapi
 * endpoint get_transcript mengembalikan HTTP 400 "Precondition check
 * failed" signed-out dari VM (dua varian params dicoba) — tidak ada
 * data transkrip yang bisa ditampilkan jujur. Chapters DITANGANI
 * terpisah di ChaptersApi (panel macro-markers di /next yang sama).
 */

/** Satu acara podcast yang diikuti. [key] stabil: browseId MPSP…. */
data class FollowedShow(
    val key: String,
    val title: String,
    val browseId: String,
    val imageUrl: String?,
    val host: String,
    val followedAtMs: Long,
)

/** Satu episode dari halaman acara. */
data class PodcastEpisode(
    val videoId: String,
    val title: String,
    val subtitle: String,
    val durationMs: Long,
    val imageUrl: String?,
    val description: String,
    /** browseId episode (MPED…) dari run judul; null bila tak dibawa. */
    val browseId: String?,
)

/** Satu acara dari halaman browse. */
data class PodcastShow(
    val browseId: String,
    val title: String,
    val host: String,
    val description: String,
    val imageUrl: String?,
    val episodes: List<PodcastEpisode>,
    /** Token halaman episode berikutnya; null = tidak ada lagi. */
    val continuationToken: String?,
)

/** Satu baris acara dari hasil search. */
data class PodcastShowRow(
    val browseId: String,
    val title: String,
    val host: String,
    val imageUrl: String?,
)

object PodcastApi {

    /** Durasi format Indonesia dari `durationText`: "2 j, 6 mnt" / "35 mnt" / "45 dtk". */
    fun parseIdDuration(text: String?): Long? {
        val t = text?.trim()?.removePrefix("•")?.trim() ?: return null
        if (t.isEmpty()) return null
        // "1:02:03" / "3:45" — bentuk jam, bukan kata (fallback defensif).
        if (t.matches(Regex("""\d{1,2}:\d{2}(:\d{2})?"""))) {
            val parts = t.split(":").mapNotNull { it.toLongOrNull() }
            return when (parts.size) {
                2 -> (parts[0] * 60 + parts[1]) * 1000
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                else -> null
            }
        }
        val m = Regex(
            """^(?:(\d+)\s*j(?:am)?,?\s*)?(?:(\d+)\s*mnt)?(?:\s*,?\s*(\d+)\s*dtk)?$""",
        ).matchEntire(t) ?: run {
            // Bentuk Inggris defensif: "2 hr 6 min" / "35 min".
            val en = Regex(
                """^(?:(\d+)\s*h(?:r|ours?)?,?\s*)?(?:(\d+)\s*min)?(?:\s*,?\s*(\d+)\s*s(?:ec)?)?$""",
            ).matchEntire(t) ?: return null
            val h = en.groupValues[1].toLongOrNull() ?: 0L
            val min = en.groupValues[2].toLongOrNull() ?: 0L
            val sec = en.groupValues[3].toLongOrNull() ?: 0L
            if (h == 0L && min == 0L && sec == 0L) return null
            return ((h * 3600) + (min * 60) + sec) * 1000
        }
        val h = m.groupValues[1].toLongOrNull() ?: 0L
        val min = m.groupValues[2].toLongOrNull() ?: 0L
        val sec = m.groupValues[3].toLongOrNull() ?: 0L
        if (h == 0L && min == 0L && sec == 0L) return null
        return ((h * 3600) + (min * 60) + sec) * 1000
    }

    private fun runsText(textObj: JSONObject?): String? {
        if (textObj == null) return null
        val runs = textObj.optJSONArray("runs") ?: return textObj.optString("simpleText", null)
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
        }
        return sb.toString().ifEmpty { null }
    }

    private fun thumbsUrl(thumbObj: JSONObject?): String? {
        val thumbs = thumbObj?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: return null
        val candidates = ArrayList<ThumbCandidate>()
        for (i in 0 until thumbs.length()) {
            val t = thumbs.optJSONObject(i) ?: continue
            candidates += ThumbCandidate(t.optInt("width", 0), t.optString("url", ""))
        }
        return selectArtworkUrl(candidates)
    }

    private fun findWatchVideoId(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                val we = node.optJSONObject("watchEndpoint")
                if (we != null) {
                    val v = we.optString("videoId", "")
                    if (v.isNotEmpty()) return v
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    findWatchVideoId(node.get(keys.next()))?.let { return it }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    findWatchVideoId(node.get(i))?.let { return it }
                }
            }
        }
        return null
    }

    /** Satu episode dari `musicMultiRowListItemRenderer`; null bila tak bisa diputar. */
    fun parseEpisode(item: JSONObject): PodcastEpisode? {
        val videoId = findWatchVideoId(item.optJSONObject("onTap"))
            ?: findWatchVideoId(item.optJSONObject("overlay"))
            ?: return null
        val titleObj = item.optJSONObject("title")
        val title = runsText(titleObj) ?: return null
        var episodeBrowseId: String? = null
        val titleRuns = titleObj?.optJSONArray("runs")
        if (titleRuns != null) {
            for (i in 0 until titleRuns.length()) {
                val bid = titleRuns.optJSONObject(i)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId", null)
                    ?.takeIf { it.isNotBlank() }
                if (bid != null && bid.startsWith("MPED")) {
                    episodeBrowseId = bid
                    break
                }
            }
        }
        val subtitle = runsText(item.optJSONObject("subtitle")) ?: ""
        val durationText = item.optJSONObject("playbackProgress")
            ?.optJSONObject("musicPlaybackProgressRenderer")
            ?.optJSONObject("durationText")
            ?.let { runsText(it) } ?: ""
        val durationMs = parseIdDuration(durationText) ?: 0L
        val imageUrl = thumbsUrl(
            item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer"),
        ) ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        val description = runsText(item.optJSONObject("description")) ?: ""
        return PodcastEpisode(
            videoId = videoId,
            title = title,
            subtitle = subtitle,
            durationMs = durationMs,
            imageUrl = imageUrl,
            description = description,
            browseId = episodeBrowseId,
        )
    }

    private fun parseShelfEpisodes(shelf: JSONObject): List<PodcastEpisode> {
        val out = ArrayList<PodcastEpisode>()
        val contents = shelf.optJSONArray("contents") ?: return out
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i)
                ?.optJSONObject("musicMultiRowListItemRenderer") ?: continue
            parseEpisode(item)?.let { out += it }
        }
        return out
    }

    private fun shelfContinuation(shelf: JSONObject): String? {
        val cont = shelf.optJSONArray("continuations") ?: return null
        for (i in 0 until cont.length()) {
            val token = cont.optJSONObject(i)
                ?.optJSONObject("nextContinuationData")
                ?.optString("continuation", null)
                ?.takeIf { it.isNotBlank() }
            if (token != null) return token
        }
        return null
    }

    /** Halaman acara penuh dari respons /browse; null bila header tak ditemukan. */
    fun parseShowPage(root: JSONObject, browseId: String): PodcastShow? {
        var header: JSONObject? = null
        var shelf: JSONObject? = null
        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    if (header == null && node.has("musicResponsiveHeaderRenderer")) {
                        header = node.getJSONObject("musicResponsiveHeaderRenderer")
                    }
                    if (shelf == null && node.has("musicShelfRenderer")) {
                        val s = node.getJSONObject("musicShelfRenderer")
                        // Shelf episode = yang isinya multi-row item.
                        val contents = s.optJSONArray("contents")
                        if (contents != null && contents.length() > 0 &&
                            contents.optJSONObject(0)?.has("musicMultiRowListItemRenderer") == true
                        ) {
                            shelf = s
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
        val h = header ?: return null
        val title = runsText(h.optJSONObject("title")) ?: return null
        val host = runsText(h.optJSONObject("straplineTextOne")) ?: ""
        val description = h.optJSONObject("description")
            ?.optJSONObject("musicDescriptionShelfRenderer")
            ?.optJSONObject("description")
            ?.let { runsText(it) } ?: ""
        val imageUrl = thumbsUrl(
            h.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer"),
        )
        val episodes = shelf?.let { parseShelfEpisodes(it) } ?: emptyList()
        return PodcastShow(
            browseId = browseId,
            title = title,
            host = host,
            description = description,
            imageUrl = imageUrl,
            episodes = episodes,
            continuationToken = shelf?.let { shelfContinuation(it) },
        )
    }

    /** Halaman lanjutan episode dari respons /browse + continuation. */
    fun parseShowContinuation(root: JSONObject): Pair<List<PodcastEpisode>, String?> {
        val shelf = root.optJSONObject("continuationContents")
            ?.optJSONObject("musicShelfContinuation") ?: return Pair(emptyList(), null)
        return Pair(parseShelfEpisodes(shelf), shelfContinuation(shelf))
    }

    /** Baris acara (MPSP…) dari respons /search; episode/lagu dilewati. */
    fun parseSearchShows(root: JSONObject): List<PodcastShowRow> {
        val out = ArrayList<PodcastShowRow>()
        val seen = HashSet<String>()
        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    if (node.has("musicResponsiveListItemRenderer")) {
                        val item = node.getJSONObject("musicResponsiveListItemRenderer")
                        val bid = item.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("browseEndpoint")
                            ?.optString("browseId", null)
                            ?.takeIf { it.isNotBlank() }
                        if (bid != null && bid.startsWith("MPSP") && seen.add(bid)) {
                            val cols = item.optJSONArray("flexColumns")
                            val title = if (cols != null && cols.length() > 0) {
                                runsText(
                                    cols.getJSONObject(0)
                                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text"),
                                )
                            } else {
                                null
                            }
                            var host = ""
                            if (cols != null && cols.length() > 1) {
                                val second = runsText(
                                    cols.getJSONObject(1)
                                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text"),
                                ) ?: ""
                                // "Podcast • Andrew Huberman" → host = bagian kedua.
                                val parts = second.split(" • ")
                                host = parts.lastOrNull { it.isNotBlank() && !it.equals("Podcast", ignoreCase = true) }
                                    ?: ""
                                if (host.equals("Podcast", ignoreCase = true)) host = ""
                            }
                            if (!title.isNullOrBlank()) {
                                val imageUrl = thumbsUrl(
                                    item.optJSONObject("thumbnail")
                                        ?.optJSONObject("musicThumbnailRenderer"),
                                )
                                out += PodcastShowRow(bid, title, host, imageUrl)
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
        walk(root)
        return out
    }

    /** Episode → Track siap antre (pola yang sama dengan hasil search). */
    fun episodeToTrack(
        episode: PodcastEpisode,
        showBrowseId: String,
        showTitle: String,
    ): Track = Track(
        id = "ytm:${episode.videoId}",
        title = episode.title,
        artist = showTitle,
        album = "",
        source = Source.YOUTUBE_MUSIC,
        engine = Engine.YOUTUBE,
        durationMs = episode.durationMs,
        youtubeVideoId = episode.videoId,
        imageUrl = episode.imageUrl,
        explicit = false,
        isPodcastEpisode = true,
        podcastShowBrowseId = showBrowseId,
        podcastShowTitle = showTitle,
    )
}
