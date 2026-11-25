package com.sonara.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * YouTube Music — dua jalur, keduanya signed-out (tanpa login / API key):
 *
 * 1. SEARCH — API web yang dipakai situs music.youtube.com sendiri
 *    (InnerTube, client WEB_REMIX). Jalur TIDAK RESMI (tidak ada API
 *    publik YTM). Diverifikasi dari VM 2026-11-20: POST search
 *    mengembalikan HTTP 200 + 31 hasil untuk "coldplay yellow".
 *
 * 2. STREAM — player API YouTube persis seperti MixMusic
 *    (fzn0x/mixmusic, internal/stream/innertube.go): sebagai aplikasi
 *    YouTube untuk visionOS (client VISIONOS 1.02), signed-out, dengan
 *    visitor ID yang diminta dulu lewat endpoint visitor_id dan dikirim
 *    balik. YouTube menjawab dengan stream Opus (itag 251) yang URL-nya
 *    langsung bisa diputar — tanpa signature untuk dipecahkan, tanpa
 *    tantangan — dan tanpa iklan pre-roll. Diputar native oleh Media3
 *    ExoPlayer, BUKAN lewat embed WebView. Diverifikasi dari VM
 *    2026-11-20: visitor_id OK, player OK (playability OK, 23 format,
 *    Opus 251 URL langsung, tanpa signatureCipher), Range-check
 *    bytes=0-0 → HTTP 206.
 *
 * v1.0 dulu memutar YTM lewat embed IFrame di WebView 1×1 dp yang
 * tersembunyi — di HP user semua fungsi YTM error: embed tidak pernah
 * memberi error yang jelas (script IFrame gagal dimuat = diam saja),
 * dan banyak video musik tidak bisa di-embed. Karena itu v1.1
 * memutar lewat stream langsung; embed hanya fallback terakhir kalau
 * stream langsung gagal.
 *
 * Batasan jujur: YouTube bisa mengubah API ini kapan saja; sebagian
 * lagu (khusus anak-anak, khusus member, khusus dewasa login) tidak
 * diberi stream oleh client ini — fungsi mengembalikan Result.failure
 * dengan alasan yang jelas, bukan data karangan. Kunci API di bawah
 * adalah kunci publik client web YouTube yang tertanam di setiap
 * halaman youtube.com — BUKAN kredensial rahasia.
 */
class YoutubeMusicApi(
    private val http: OkHttpClient = SonaraHttp.defaultHttp(),
) {
    companion object {
        /** Kunci publik client web YouTube (ada di source youtube.com). */
        private const val WEB_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        private const val SEARCH_URL =
            "https://music.youtube.com/youtubei/v1/search?key=$WEB_API_KEY"
        private const val PLAYER_BASE = "https://www.youtube.com/youtubei/v1"

        // --- Client visionOS, PERSIS seperti MixMusic (innertube.go) ---
        private const val VISION_NAME = "VISIONOS"
        private const val VISION_VERSION = "1.02"
        private const val VISION_NUMBER = "101" // X-YouTube-Client-Name
        private const val VISION_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/26.0 Safari/605.1.15"
        private const val VISION_MAKE = "Apple"
        private const val VISION_MODEL = "RealityDevice17,1"
        private const val VISION_OS = "visionOS"
        private const val VISION_OS_VERSION = "26.5.23O471"

        // --- Client fallback (v1.10.1): bila visionOS tidak memberi
        // format, coba WEB → ANDROID → TVHTML5 sebelum menyerah.
        // Nama/versi/UA adalah nilai publik client web YouTube yang
        // sama dipakai situs/aplikasi YouTube sendiri.
        private const val WEB_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val ANDROID_AGENT =
            "com.google.android.youtube/19.09.37 (Linux; U; Android 14; Pixel 7) gzip"

        private val JSON = "application/json".toMediaType()
    }

    /**
     * Satu client InnerTube. [useVisitor] hanya untuk visionOS
     * (client lain tidak memakai visitorData yang sama).
     */
    private data class InnertubeClient(
        val name: String,
        val version: String,
        val clientNumber: String,
        val userAgent: String,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val osName: String? = null,
        val osVersion: String? = null,
        val androidSdkVersion: Int? = null,
        val useVisitor: Boolean = false,
    )

    /** Urutan coba client stream: utama dulu, fallback sesudahnya. */
    private val streamClients = listOf(
        InnertubeClient(
            VISION_NAME, VISION_VERSION, VISION_NUMBER, VISION_AGENT,
            VISION_MAKE, VISION_MODEL, VISION_OS, VISION_OS_VERSION,
            useVisitor = true,
        ),
        InnertubeClient("WEB", "2.20240111.09.00", "1", WEB_AGENT),
        InnertubeClient(
            "ANDROID", "19.09.37", "3", ANDROID_AGENT,
            androidSdkVersion = 34,
        ),
        InnertubeClient("TVHTML5", "7.20240111.09.00", "7", WEB_AGENT),
    )

    data class YtmResult(
        val videoId: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val isSong: Boolean,
        val explicit: Boolean,
        val imageUrl: String? = null,
        /** browseId artis (UC…) dari run artis — null bila hasil tidak membawanya. */
        val artistBrowseId: String? = null,
        /** Hitungan putar/tonton dari subtitle (v1.0.3); 0 = tidak dibawa respons. */
        val playCount: Long = 0,
        /** "plays"/"views"/"" — lihat MediaTextParse. */
        val countKind: String = "",
        /** Tahun rilis dari subtitle; 0 = tidak ada. */
        val year: Int = 0,
        /** Episode podcast (v1.0.5): subtitle "Episode • <tanggal> • <Acara>". */
        val isPodcastEpisode: Boolean = false,
        /** Nama acara episode (bagian terakhir subtitle); "" bila bukan episode. */
        val podcastShowTitle: String = "",
    )

    /** Stream audio langsung yang sudah dicek bisa diunduh (Range 0-0). */
    data class YtmStream(
        val url: String,
        val itag: Int,
        val mimeType: String,
        val durationMs: Long,
        val isLive: Boolean = false,
        val hlsManifestUrl: String? = null,
        /** Bitrate format terpilih (bps, dari adaptiveFormats); 0 = tidak dilaporkan. */
        val bitrate: Int = 0,
    )

    /** Kualitas audio yang diminta user (di-set PlayerManager dari LocalStore). */
    @Volatile
    var audioQuality: AudioQuality = AudioQuality.BEST

    // ---- visitor ID (MixMusic: disimpan 6 jam, dipakai ulang) ----

    @Volatile
    private var cachedVisitor: String? = null

    @Volatile
    private var cachedVisitorAtMs: Long = 0L

    private val visitorHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Client gate Range-check (v1.10.1): timeout HARUS longgar —
     * byte pertama googlevideo dari jaringan lambat (VM: ~6 detik)
     * membuat gate 4 detik gagal total dan semua lagu jatuh ke HLS
     * (terbukti lewat salinan debug terinstrumentasi, 22 Nov 2026:
     * gate 4s → 251/250/249 semua false; gate 15s → 250 true).
     * 10s/12s memberi ruang untuk jaringan HP yang lambat tanpa
     * menggantung resolve terlalu lama.
     */
    private val gateHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /** Konteks client InnerTube untuk [client] (semua client sama bentuknya). */
    private fun clientContext(client: InnertubeClient, visitor: String?): JSONObject {
        val c = JSONObject().apply {
            put("clientName", client.name)
            put("clientVersion", client.version)
            client.deviceMake?.let { put("deviceMake", it) }
            client.deviceModel?.let { put("deviceModel", it) }
            client.osName?.let { put("osName", it) }
            client.osVersion?.let { put("osVersion", it) }
            client.androidSdkVersion?.let { put("androidSdkVersion", it) }
            put("userAgent", client.userAgent)
            put("hl", "en")
            put("timeZone", "UTC")
            put("utcOffsetMinutes", 0)
            if (client.useVisitor && !visitor.isNullOrBlank()) put("visitorData", visitor)
        }
        return JSONObject().apply { put("client", c) }
    }

    private fun clientHeaders(client: InnertubeClient, visitor: String?): Request.Builder {
        val b = Request.Builder()
            .header("Content-Type", "application/json")
            .header("User-Agent", client.userAgent)
            .header("Origin", "https://www.youtube.com")
            .header("X-YouTube-Client-Name", client.clientNumber)
            .header("X-YouTube-Client-Version", client.version)
        if (client.useVisitor && !visitor.isNullOrBlank()) {
            b.header("X-Goog-Visitor-Id", visitor)
        }
        return b
    }

    private fun visionContext(visitor: String?): JSONObject =
        clientContext(streamClients[0], visitor)

    private fun visionHeaders(visitor: String?): Request.Builder =
        clientHeaders(streamClients[0], visitor)

    /** Body request /player — sama untuk semua client (field wajib:
     * playbackContext + contentCheckOk + racyCheckOk). */
    private fun playerRequestBody(
        videoId: String,
        client: InnertubeClient,
        visitor: String?,
    ): okhttp3.RequestBody {
        return JSONObject().apply {
            put("videoId", videoId)
            put(
                "playbackContext",
                JSONObject().apply {
                    put(
                        "contentPlaybackContext",
                        JSONObject().apply {
                            put("html5Preference", "HTML5_PREF_WANTS")
                        },
                    )
                },
            )
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            put("context", clientContext(client, visitor))
        }.toString().toRequestBody(JSON)
    }

    /** POST /player untuk satu client; gagal HTTP/jaringan = failure (coba client lain). */
    private fun playerResponse(
        videoId: String,
        client: InnertubeClient,
        visitor: String?,
    ): Result<JSONObject> {
        return try {
            val req = clientHeaders(client, visitor)
                .url("$PLAYER_BASE/player?prettyPrint=false")
                .post(playerRequestBody(videoId, client, visitor))
                .build()
            visitorHttp.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return Result.failure(
                        IllegalStateException("YouTube didn't respond (HTTP ${resp.code})."),
                    )
                }
                Result.success(JSONObject(text))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to fetch the stream: ${friendly(e)}"))
        }
    }

    /** Minta visitor ID baru ke YouTube (endpoint visitor_id). */
    private fun fetchVisitorId(): String? {
        return try {
            val body = JSONObject().apply {
                put("context", visionContext(null))
            }.toString().toRequestBody(JSON)
            val req = visionHeaders(null)
                .url("$PLAYER_BASE/visitor_id?prettyPrint=false")
                .post(body)
                .build()
            visitorHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val text = resp.body?.string() ?: return null
                JSONObject(text).optJSONObject("responseContext")
                    ?.optString("visitorData", null)?.takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun visitorId(): String? {
        val now = System.currentTimeMillis()
        val cached = cachedVisitor
        if (cached != null && now - cachedVisitorAtMs < 6 * 60 * 60 * 1000L) return cached
        val fresh = fetchVisitorId()
        if (fresh != null) {
            cachedVisitor = fresh
            cachedVisitorAtMs = now
        }
        return fresh ?: cached
    }

    private fun dropVisitor() {
        cachedVisitor = null
        cachedVisitorAtMs = 0L
    }

    // ---- stream langsung (player API visionOS) ----

    /**
     * Ambil stream audio langsung untuk sebuah videoId.
     * Retry 1x dengan visitor ID baru kalau YouTube menolak (pola
     * MixMusic: visitor ID yang tidak dipercaya diganti).
     * Pembungkus [resolveStreamDetailed] — pemanggil lama tidak berubah.
     */
    fun resolveStream(videoId: String): Result<YtmStream> =
        resolveStreamDetailed(videoId).result

    /** Rantai resolve + jejak langkahnya (dipakai sheet "Track info", v1.0.3). */
    data class ResolveOutcome(val result: Result<YtmStream>, val trace: ResolveTrace)

    fun resolveStreamDetailed(videoId: String): ResolveOutcome {
        val attempts = ArrayList<ResolveAttempt>()
        // Mesin Go asli MixMusic dulu (stream langsung visionOS).
        val go = GoEngine.resolveStream(videoId)
        if (go.isSuccess) {
            val stream = go.getOrThrow()
            // Cek seperti jalur Kotlin: minta byte pertama dulu.
            if (stream.isLive || streamServes(stream.url)) {
                attempts += ResolveAttempt(
                    "Go engine",
                    "OK — ${stream.mimeType.ifBlank { "stream" }}",
                )
                return ResolveOutcome(go, ResolveTrace(attempts))
            }
            attempts += ResolveAttempt("Go engine", "stream rejected (Range check failed)")
        } else {
            attempts += ResolveAttempt(
                "Go engine",
                go.exceptionOrNull()?.message ?: "Go engine returned an error",
            )
        }
        val first = resolveStreamOnce(videoId, visitorId(), attempts)
        if (first.isSuccess) return ResolveOutcome(first, ResolveTrace(attempts))
        // Sekali coba lagi dengan visitor baru — kalau tetap gagal, itu
        // jawabannya (bukan network sesaat).
        dropVisitor()
        attempts += ResolveAttempt("New visitor ID", "requested; retrying the client chain")
        val second = resolveStreamOnce(videoId, visitorId(), attempts)
        if (second.isSuccess) return ResolveOutcome(second, ResolveTrace(attempts))
        return ResolveOutcome(first, ResolveTrace(attempts)) // error pertama (paling informatif)
    }

    // ---- deskripsi video untuk sheet History (v1.10.18) ----
    // Deskripsi diambil dari /player visionOS yang sama dengan
    // resolve audio (pola retry visitor yang sama). Hanya
    // shortDescription yang dibaca — teks ditampilkan VERBATIM di
    // sheet History dengan label sumbernya, tidak pernah diolah
    // menjadi narasi. Kosong/gagal = tidak ada blok deskripsi
    // (bukan error yang ditampilkan ke user).

    /**
     * Ambil shortDescription video dari /player visionOS.
     * Null = tidak ada deskripsi yang bisa dipakai (gagal, status
     * bukan OK, atau deskripsinya kosong).
     */
    fun fetchVideoDescription(videoId: String): String? {
        val first = fetchVideoDescriptionOnce(videoId, visitorId())
        if (first != null) return first
        dropVisitor()
        return fetchVideoDescriptionOnce(videoId, visitorId())
    }

    private fun fetchVideoDescriptionOnce(
        videoId: String,
        visitor: String?,
    ): String? {
        val root = playerResponse(videoId, streamClients[0], visitor).getOrNull()
            ?: return null
        val status = root.optJSONObject("playabilityStatus")
            ?.optString("status", "") ?: ""
        if (status != "OK") return null
        val desc = root.optJSONObject("videoDetails")
            ?.optString("shortDescription", "") ?: ""
        return desc.trim().takeIf { it.isNotEmpty() }
    }

    // ---- metadata video untuk link YTM (v1.10.21) ----
    // Judul + artis + durasi + artwork dibaca dari videoDetails
    // endpoint /player yang SAMA dengan resolve audio (pola retry
    // visitor yang sama dengan fetchVideoDescription). Tanpa judul
    // = gagal — tidak ada track karangan yang dibuat dari videoId
    // saja.

    /** Metadata video dari videoDetails; gagal = failure. */
    data class VideoMeta(
        val videoId: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val imageUrl: String?,
        /** True bila videoDetails menandai konten live (v1.0.3). */
        val isLive: Boolean = false,
    )

    fun lookupVideo(videoId: String): Result<VideoMeta> {
        val first = lookupVideoOnce(videoId, visitorId())
        if (first.isSuccess) return first
        dropVisitor()
        return lookupVideoOnce(videoId, visitorId())
    }

    private fun lookupVideoOnce(videoId: String, visitor: String?): Result<VideoMeta> {
        val root = playerResponse(videoId, streamClients[0], visitor).getOrNull()
            ?: return Result.failure(
                IllegalStateException("Couldn't load that video."),
            )
        val status = root.optJSONObject("playabilityStatus")
            ?.optString("status", "") ?: ""
        if (status != "OK") {
            return Result.failure(
                IllegalStateException("That video isn't playable."),
            )
        }
        val details = root.optJSONObject("videoDetails")
            ?: return Result.failure(IllegalStateException("Couldn't load that video."))
        val title = details.optString("title", "").trim()
        if (title.isEmpty()) {
            return Result.failure(IllegalStateException("Couldn't load that video."))
        }
        val artist = details.optString("author", "").trim()
        val lengthSec = details.optString("lengthSeconds", "0").toLongOrNull() ?: 0L
        val isLive = details.optBoolean("isLiveContent", false)
        var imageUrl: String? = null
        val thumbs = details.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        if (thumbs != null) {
            val candidates = ArrayList<ThumbCandidate>()
            for (i in 0 until thumbs.length()) {
                val t = thumbs.optJSONObject(i) ?: continue
                candidates += ThumbCandidate(
                    t.optInt("width", 0),
                    t.optString("url", ""),
                )
            }
            imageUrl = selectArtworkUrl(candidates)
        }
        return Result.success(
            VideoMeta(
                videoId = videoId,
                title = title,
                artist = artist,
                durationMs = lengthSec * 1000,
                imageUrl = imageUrl,
                isLive = isLive,
            ),
        )
    }

    /** Konversi hasil pencarian/profil artis menjadi [Track] yang bisa diputar. */
    fun trackFromResult(r: YtmResult): Track = Track(
        id = "ytm:${r.videoId}",
        title = r.title,
        artist = r.artist,
        album = "",
        source = Source.YOUTUBE_MUSIC,
        engine = Engine.YOUTUBE,
        durationMs = r.durationMs,
        youtubeVideoId = r.videoId,
        imageUrl = r.imageUrl,
        explicit = r.explicit,
        artistBrowseId = r.artistBrowseId,
        playCount = r.playCount,
        countKind = r.countKind,
        year = r.year,
        isPodcastEpisode = r.isPodcastEpisode,
        podcastShowTitle = r.podcastShowTitle.takeIf { it.isNotBlank() },
    )

    /**
     * Satu percobaan resolve (v1.10.1, jauh lebih bandel dari v1.10):
     * coba client berurutan (VISIONOS → WEB → ANDROID → TVHTML5);
     * tiap client yang menjawab OK dicoba rantai format audionya
     * (Opus 251/250/249 → AAC 140 → format lain, masing-masing
     * di-gate Range 0-0, maks 3 gate per client). Bila tidak ada
     * format DASH yang lolos gate, fallback ke manifest HLS, lalu
     * ke kandidat DASH terbaik tanpa gate (ExoPlayer yang
     * memvalidasi; error putar ditangani jujur di PlayerManager).
     * Pembatasan playability yang sah (dihapus/usia/wilayah/login)
     * tetap dilaporkan apa adanya — tidak dipaksa.
     */
    private fun resolveStreamOnce(
        videoId: String,
        visitor: String?,
        attempts: MutableList<ResolveAttempt>,
    ): Result<YtmStream> {
        var firstRestriction: String? = null
        var lastGateError: String? = null
        var anyClientOk = false
        var firstHls: String? = null
        var bestUngated: JSONObject? = null
        var bestUngatedClient: String? = null
        var bestUngatedDurationMs = 0L
        for (client in streamClients) {
            val root = playerResponse(videoId, client, visitor).getOrNull() ?: continue
            val status = root.optJSONObject("playabilityStatus")
            val playStatus = status?.optString("status", "") ?: ""
            if (playStatus != "OK") {
                val message = playabilityMessage(playStatus, status)
                if (firstRestriction == null) {
                    firstRestriction = message
                }
                attempts += ResolveAttempt(client.name, message)
                continue
            }
            anyClientOk = true
            val videoDetails = root.optJSONObject("videoDetails")
            val lengthSec = videoDetails?.optString("lengthSeconds", "0")
                ?.toLongOrNull() ?: 0L
            val isLive = videoDetails?.optBoolean("isLive", false) ?: false
            val streaming = root.optJSONObject("streamingData")
            if (firstHls == null) {
                firstHls = streaming?.optString("hlsManifestUrl", "")
                    ?.takeIf { it.isNotBlank() }
            }
            if (isLive) {
                val hls = streaming?.optString("hlsManifestUrl", "") ?: ""
                if (hls.isBlank()) {
                    attempts += ResolveAttempt(client.name, "OK, live but no HLS manifest")
                    continue
                }
                attempts += ResolveAttempt(client.name, "OK — live stream (HLS manifest)")
                return Result.success(
                    YtmStream(
                        url = hls,
                        itag = 0,
                        mimeType = "application/x-mpegURL",
                        durationMs = 0L,
                        isLive = true,
                        hlsManifestUrl = hls,
                    ),
                )
            }
            val formats = streaming?.optJSONArray("adaptiveFormats")
            if (formats == null) {
                attempts += ResolveAttempt(client.name, "OK, but no streaming data")
                continue
            }
            val candidates = audioCandidates(formats)
            if (candidates.isEmpty()) {
                attempts += ResolveAttempt(client.name, "OK, but no audio formats")
                continue
            }
            val approxMs = candidates[0].optString("approxDurationMs", "")
                .toLongOrNull() ?: 0L
            val durationMs = if (lengthSec > 0) lengthSec * 1000 else approxMs
            if (bestUngated == null) {
                bestUngated = candidates[0]
                bestUngatedClient = client.name
                bestUngatedDurationMs = durationMs
            }
            // Lapis 1: gate maks 3 kandidat pertama client ini.
            var gated = false
            for (f in candidates.take(3)) {
                val url = f.optString("url", "")
                if (url.isBlank()) continue
                if (!streamServes(url)) {
                    lastGateError = "YouTube rejected this song's stream. Try again later."
                    continue
                }
                val bitrate = f.optInt("bitrate", 0)
                attempts += ResolveAttempt(
                    client.name,
                    "OK — ${f.optString("mimeType", "audio")} (itag ${f.optInt("itag", 0)}" +
                        (if (bitrate > 0) ", ${bitrate / 1000} kbps" else "") + ")",
                )
                gated = true
                return Result.success(
                    YtmStream(
                        url = url,
                        itag = f.optInt("itag", 0),
                        mimeType = f.optString("mimeType", ""),
                        durationMs = durationMs,
                        isLive = false,
                        bitrate = bitrate,
                    ),
                )
            }
            if (!gated) {
                attempts += ResolveAttempt(
                    client.name,
                    "OK, but every audio format failed the Range check",
                )
            }
        }
        // Lapis 2: manifest HLS (non-live) — endpoint terpisah yang
        // biasanya tetap memberi stream walau format DASH ditolak.
        val hls = firstHls
        if (hls != null) {
            attempts += ResolveAttempt("HLS manifest", "OK — HLS manifest (fallback)")
            return Result.success(
                YtmStream(
                    url = hls,
                    itag = 0,
                    mimeType = "application/x-mpegURL",
                    durationMs = 0L,
                    isLive = false,
                    hlsManifestUrl = hls,
                ),
            )
        }
        // Lapis 3: kandidat DASH terbaik tanpa gate — ExoPlayer yang
        // memutuskan; kalau gagal, PlayerManager re-resolve + pesan jujur.
        val ungated = bestUngated
        if (ungated != null) {
            attempts += ResolveAttempt(
                "${bestUngatedClient ?: "Unknown client"} (ungated)",
                "best format without Range check — the player decides",
            )
            return Result.success(
                YtmStream(
                    url = ungated.optString("url"),
                    itag = ungated.optInt("itag", 0),
                    mimeType = ungated.optString("mimeType", ""),
                    durationMs = bestUngatedDurationMs,
                    isLive = false,
                    bitrate = ungated.optInt("bitrate", 0),
                ),
            )
        }
        return Result.failure(
            IllegalStateException(
                if (anyClientOk) {
                    lastGateError
                        ?: "YouTube didn't provide a playable stream " +
                            "for this song (it may be members-only / sign-in-only)."
                } else {
                    firstRestriction
                        ?: "YouTube didn't provide a stream for this song."
                },
            ),
        )
    }

    /**
     * Rantai kandidat format audio sesuai kualitas yang diminta user
     * (v1.10.1; dulu [pickAudioFormat] hanya mengambil SATU format —
     * kalau URL-nya ditolak, resolve langsung gagal total).
     * BEST → Opus bitrate tertinggi dulu (251/250/249), lalu AAC 140,
     * lalu format audio lain; DATA_SAVER → AAC 140 dulu. Format tanpa
     * URL langsung (butuh signature) dan format DRC dibuang. Maks 6
     * kandidat supaya rantai tetap cepat.
     */
    private fun audioCandidates(formats: JSONArray): List<JSONObject> {
        val opus = ArrayList<JSONObject>()
        var aac140: JSONObject? = null
        val others = ArrayList<JSONObject>()
        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            val url = f.optString("url", "")
            if (url.isBlank()) continue // butuh signature — lewati
            if (f.optBoolean("isDrc", false)) continue // dinamika ditekan
            val mime = f.optString("mimeType", "")
            if (!mime.startsWith("audio/")) continue
            when {
                mime.contains("opus") -> opus.add(f)
                f.optInt("itag", 0) == 140 && mime.contains("mp4a.40.2") -> {
                    if (aac140 == null) aac140 = f
                }
                else -> others.add(f)
            }
        }
        val opusSorted = opus.sortedByDescending { it.optInt("bitrate", 0) }
        val othersSorted = others.sortedByDescending { it.optInt("bitrate", 0) }
        val ordered = when (audioQuality) {
            AudioQuality.BEST -> opusSorted + listOfNotNull(aac140) + othersSorted
            AudioQuality.DATA_SAVER ->
                listOfNotNull(aac140) +
                    opusSorted.sortedBy { it.optInt("bitrate", 0) } + othersSorted
            AudioQuality.BALANCED -> opusSorted + listOfNotNull(aac140) + othersSorted
        }
        return ordered.take(6)
    }

    /** Minta byte pertama stream (Range 0-0): harus 206/200. */
    private fun streamServes(url: String): Boolean {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .get()
                .build()
            gateHttp.newCall(req).execute().use { resp ->
                resp.code == 206 || resp.code == 200
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun playabilityMessage(status: String, obj: JSONObject?): String {
        val reason = obj?.optString("reason", "") ?: ""
        val reasonLower = reason.lowercase()
        return when {
            status == "LOGIN_REQUIRED" && reason.contains("bot", ignoreCase = true) ->
                "YouTube asked for verification (suspected bot). Try again later."
            status == "LOGIN_REQUIRED" ->
                "This song only plays when signed in to YouTube."
            status == "AGE_VERIFICATION_REQUIRED" || status == "AGE_GATE" ->
                "This song is age-restricted to signed-in YouTube users."
            // v1.10.1: bedakan alasan dari teks reason, bukan satu pesan generik.
            reasonLower.contains("age") || reasonLower.contains("dewasa") ->
                "This song is age-restricted — it only plays when signed in to YouTube."
            reasonLower.contains("country") || reasonLower.contains("region") ||
                reasonLower.contains("not available in your") ||
                reasonLower.contains("wilayah") ->
                "This song isn't available in your region."
            reasonLower.contains("private") || reasonLower.contains("deleted") ||
                reasonLower.contains("removed") || reasonLower.contains("dihapus") ->
                "This song's video was deleted or made private."
            reasonLower.contains("premium") || reasonLower.contains("member") ->
                "This song is for YouTube members / Premium subscribers."
            reasonLower.contains("copyright") || reasonLower.contains("hak cipta") ->
                "This song is blocked for copyright reasons."
            status == "RESTRICTED" ->
                "This song is restricted (age, region, or content)."
            status == "UNPLAYABLE" &&
                (reasonLower.contains("video unavailable") ||
                    reasonLower.contains("this video is unavailable") ||
                    reason.isBlank()) ->
                "This song isn't available on YouTube."
            status == "UNPLAYABLE" && reason.isNotBlank() ->
                "Can't play: $reason"
            status == "UNPLAYABLE" ->
                "This song can't be played on YouTube Music."
            status == "ERROR" ->
                "YouTube error for this song. Try another song."
            status.isBlank() ->
                "YouTube didn't return a readable response."
            else ->
                "YouTube answered '$status' for this song" +
                    if (reason.isNotBlank()) ": $reason" else "."
        }
    }

    // ---- search (WEB_REMIX, verified) ----
    //
    // v1.10.4: `gl`/`hl` bisa diubah pemanggil — default "ID"/"id"
    // seperti sebelumnya, supaya pencarian biasa user tidak berubah.
    // v1.10.25: feed Beranda/Browse/mood memakai gl="ID", hl="id" +
    // forceKotlin (pola Charts 10.19); feed global hanya lewat chip
    // Global di Charts (gl="US", hl="en").

    /**
     * v1.10.18: [forceKotlin] melewati mesin Go — Go hanya menerima
     * query dan MENGABAIKAN gl/hl, jadi Charts yang butuh region
     * (Indonesia vs Global) harus memaksa jalur Kotlin yang
     * menghormati gl/hl. Jalur pencarian biasa tidak berubah
     * (Go dulu, fallback Kotlin).
     */
    fun search(
        query: String,
        gl: String = "ID",
        hl: String = "id",
        forceKotlin: Boolean = false,
    ): Result<List<Track>> {
        return searchRaw(query, gl, hl, forceKotlin).map { results ->
            results.map { r ->
                Track(
                    id = "ytm:${r.videoId}",
                    title = r.title,
                    artist = r.artist,
                    album = "",
                    source = Source.YOUTUBE_MUSIC,
                    engine = Engine.YOUTUBE,
                    durationMs = r.durationMs,
                    youtubeVideoId = r.videoId,
                    imageUrl = r.imageUrl,
                    explicit = r.explicit,
                    artistBrowseId = r.artistBrowseId,
                    playCount = r.playCount,
                    countKind = r.countKind,
                    year = r.year,
                    isPodcastEpisode = r.isPodcastEpisode,
                    podcastShowTitle = r.podcastShowTitle.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    fun searchRaw(
        query: String,
        gl: String = "ID",
        hl: String = "id",
        forceKotlin: Boolean = false,
    ): Result<List<YtmResult>> {
        // Mesin Go asli MixMusic dulu; gagal -> implementasi Kotlin (v1.1).
        // v1.10.19: hasil di-dedupe per videoId di titik ini (satu-satunya
        // pintu keluar kedua jalur) — satu video dua kali dalam satu
        // daftar adalah bug data DAN pemicu crash key duplikat di LazyColumn.
        val result: Result<List<YtmResult>> = if (!forceKotlin) {
            val go = GoEngine.searchYtm(query)
            if (go.isSuccess) {
                go
            } else {
                val first = searchRawOnce(query, gl, hl)
                if (first.isSuccess) first else searchRawOnce(query, gl, hl)
            }
        } else {
            val first = searchRawOnce(query, gl, hl)
            if (first.isSuccess) first else searchRawOnce(query, gl, hl)
        }
        return result.map { list -> list.distinctBy { it.videoId } }
    }

    private fun searchRawOnce(query: String, gl: String, hl: String): Result<List<YtmResult>> {
        return try {
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.00.00")
                        put("hl", hl)
                        put("gl", gl)
                    })
                })
                put("query", query)
            }.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url(SEARCH_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "YouTube Music didn't respond (HTTP ${resp.code}). " +
                                "Check your internet, then try again.",
                        ),
                    )
                }
                Result.success(parseSearch(JSONObject(text)))
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("YouTube Music search failed: ${friendly(e)}"),
            )
        }
    }

    /**
     * POST /youtubei/v1/browse (WEB_REMIX) untuk halaman artis/album
     * (v1.10.18, profil artis + About artis untuk sheet History).
     * Mengembalikan JSON mentah — parsing ada di ArtistApi supaya
     * bisa diuji murni dari fixture. Pola request sama seperti
     * searchRawOnce (client WEB_REMIX + gl/hl).
     * v1.10.33: [params] untuk endpoint berparameter (halaman
     * kategori genre FEmusic_moods_and_genres_category).
     */
    fun browse(
        browseId: String,
        gl: String = "ID",
        hl: String = "id",
        params: String? = null,
    ): Result<JSONObject> {
        return try {
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.00.00")
                        put("hl", hl)
                        put("gl", gl)
                    })
                })
                put("browseId", browseId)
                if (!params.isNullOrBlank()) put("params", params)
            }.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?key=$WEB_API_KEY")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "YouTube Music didn't respond (HTTP ${resp.code}). " +
                                "Check your internet, then try again.",
                        ),
                    )
                }
                Result.success(JSONObject(text))
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("YouTube Music browse failed: ${friendly(e)}"),
            )
        }
    }

    /**
     * Halaman lanjutan /browse dari token continuation (v1.0.5,
     * paginasi episode halaman acara podcast): body yang sama, field
     * `continuation` menggantikan `browseId`. Terverifikasi dari VM
     * 25 Nov 2026: HTTP 200, 100 episode + token berikutnya.
     */
    fun browseContinuation(token: String): Result<JSONObject> {
        return try {
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.00.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
                put("continuation", token)
            }.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?key=$WEB_API_KEY")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "YouTube Music didn't respond (HTTP ${resp.code}). " +
                                "Check your internet, then try again.",
                        ),
                    )
                }
                Result.success(JSONObject(text))
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("YouTube Music browse failed: ${friendly(e)}"),
            )
        }
    }

    /**
     * Baris acara podcast (MPSP…) dari /search (v1.0.5): request yang
     * sama dengan searchRawOnce, parsing HANYA baris acara
     * (PodcastApi.parseSearchShows). Dipanggil terpisah dari search()
     * supaya acara muncul apa pun mesin search yang menjawab (jalur Go
     * tidak mengembalikan baris acara) — dipakai layar Search saja.
     * Gagal = daftar kosong (acara adalah pelengkap, bukan hasil utama).
     */
    fun searchShows(query: String): List<PodcastShowRow> {
        if (query.isBlank()) return emptyList()
        return try {
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.00.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
                put("query", query)
            }.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url(SEARCH_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val text = resp.body?.string() ?: return emptyList()
                PodcastApi.parseSearchShows(JSONObject(text))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Chapters episode dari /next (v1.0.5): panel macro-markers.
     * Memakai [nextRaw] yang sama dengan CommentsApi (konteks WEB
     * hl=en/gl=US + header client WEB — bentuk yang terbukti bekerja
     * untuk /next). Gagal = failure (pemanggil menampilkan empty state
     * jujur, bukan tab palsu).
     */
    fun fetchChapters(videoId: String): Result<List<Chapter>> {
        val body = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20240111.09.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("videoId", videoId)
        }
        return nextRaw(body).map { ChaptersApi.parseChapters(it) }
    }

    /**
     * POST /youtubei/v1/next (client WEB) — dipakai komentar v1.10.19.
     * Body dibangun pemanggil (context + videoId/continuation); header
     * mencerminkan entri fallback WEB jalur stream (clientNumber "1",
     * versi 2.20240111.09.00, UA + Origin yang sama). Mengembalikan
     * JSON mentah — parsing ada di CommentsApi supaya bisa diuji
     * murni dari fixture. Pola request sama seperti browse().
     */
    fun nextRaw(body: JSONObject): Result<JSONObject> {
        return try {
            val client = streamClients[1] // WEB
            val req = clientHeaders(client, null)
                .url("$PLAYER_BASE/next?key=$WEB_API_KEY")
                .post(body.toString().toRequestBody(JSON))
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "YouTube didn't respond (HTTP ${resp.code}). " +
                                "Check your internet, then try again.",
                        ),
                    )
                }
                Result.success(JSONObject(text))
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("YouTube comments failed: ${friendly(e)}"),
            )
        }
    }

    // ---- saran pencarian + radio (v1.0.3) ----
    //
    // Saran: POST /youtubei/v1/music/get_search_suggestions (WEB_REMIX,
    // signed-out). Terverifikasi dari VM 25 Nov 2026: HTTP 200, 6 saran
    // untuk "coldplay" — semuanya query searchEndpoint (tidak ada
    // saran entitas signed-out; parser tetap mendukung watchEndpoint
    // bila YTM mengembalikannya). Kosong/gagal = daftar kosong, bukan
    // error yang ditampilkan (saran adalah pelengkap, bukan hasil).
    //
    // Radio: POST /youtubei/v1/next (client WEB) dengan
    // playlistId "RD"+videoId — radio lagu-lagu seperti seed.
    // Terverifikasi dari VM 25 Nov 2026: HTTP 200, 25
    // playlistPanelVideoRenderer (judul + artis + durasi + artwork).
    // Continuation tidak dipakai — isi ulang memakai seed lagu radio
    // terakhir (lihat RadioPolicy), supaya radio mengalir terus.

    /** Saran ketikan untuk [input]; gagal = daftar kosong (bukan failure). */
    fun searchSuggestions(input: String): List<SearchSuggestion> {
        if (input.isBlank()) return emptyList()
        return try {
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.00.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
                put("input", input)
            }.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/music/get_search_suggestions?key=$WEB_API_KEY")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val text = resp.body?.string() ?: return emptyList()
                parseSearchSuggestions(JSONObject(text))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Daftar radio (lagu-lagu seperti [seedVideoId]) dari /next. */
    fun fetchRadio(seedVideoId: String): Result<List<RadioItem>> {
        return try {
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240111.09.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
                put("videoId", seedVideoId)
                put("playlistId", "RD$seedVideoId")
            }.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url("$PLAYER_BASE/next?key=$WEB_API_KEY")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "YouTube Music didn't respond (HTTP ${resp.code}). " +
                                "Check your internet, then try again.",
                        ),
                    )
                }
                Result.success(parseRadioPanels(JSONObject(text)))
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("Couldn't load the radio: ${friendly(e)}"),
            )
        }
    }

    private fun friendly(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("Unable to resolve host", ignoreCase = true) ->
                "no internet / host unreachable"
            msg.contains("timeout", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true) ->
                "connection timed out — slow internet"
            msg.contains("Failed to connect", ignoreCase = true) ->
                "couldn't connect to YouTube"
            else -> msg.ifBlank { "unknown error" }
        }
    }

    private fun parseSearch(root: JSONObject): List<YtmResult> {
        val out = ArrayList<YtmResult>()
        val seen = HashSet<String>()
        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    if (node.has("musicResponsiveListItemRenderer")) {
                        val item = node.getJSONObject("musicResponsiveListItemRenderer")
                        parseItem(item)?.let {
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
        return out.take(20)
    }

    private fun parseItem(item: JSONObject): YtmResult? {
        // videoId: dari overlay / navigationEndpoint
        val videoId = findVideoId(item) ?: return null
        val columns = item.optJSONArray("flexColumns") ?: return null
        if (columns.length() < 1) return null
        val title = runsText(columns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")) ?: return null
        var artist = ""
        var durationMs = 0L
        var explicit = false
        var playCount = 0L
        var countKind = ""
        var year = 0
        var isPodcastEpisode = false
        var podcastShowTitle = ""
        if (columns.length() > 1) {
            val second = runsText(columns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")) ?: ""
            // v1.10.19: artis = bagian non-label pertama ("Lagu • X" → X;
            // "Video" saja → ""), bukan bagian pertama mentah.
            artist = extractArtistFromSubtitle(second)
            // v1.0.5: episode podcast — "Episode • <tanggal> • <Acara>":
            // artis ekstraktor akan mengembalikan tanggal ("1 h lalu"),
            // jadi tangani eksplisit: artis = nama acara (bagian terakhir).
            val epParts = second.split(" • ")
            if (epParts.firstOrNull()?.trim().equals("Episode", ignoreCase = true)) {
                isPodcastEpisode = true
                podcastShowTitle = epParts.lastOrNull()?.trim() ?: ""
                artist = podcastShowTitle
            }
            // durasi biasanya di kolom terakhir "3:45" (atau "4.32" di locale id)
            val parts = second.split(" • ")
            for (p in parts) {
                parseDuration(p)?.let { durationMs = it }
            }
            explicit = second.contains("Explicit", ignoreCase = true)
            // v1.0.3: hitungan + tahun dari bagian subtitle yang sama
            // (aturan parse di MediaTextParse — "audiens bulanan"
            // artis tidak pernah jadi hitungan).
            val count = parseCountFromSubtitle(parts)
            playCount = count.count
            countKind = count.kind
            year = parseYearFromSubtitle(parts)
        }
        // fixedColumns kadang membawa durasi
        val fixed = item.optJSONArray("fixedColumns")
        if (fixed != null && durationMs == 0L) {
            for (i in 0 until fixed.length()) {
                val t = runsText(fixed.getJSONObject(i).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text"))
                parseDuration(t ?: "")?.let { durationMs = it }
            }
        }
        // cover: thumbnail terbesar dari musicThumbnailRenderer
        // (v1.0.1: seleksi lebar-terbesar + naikkan parameter ukuran
        // googleusercontent; dulu elemen terakhir — rapuh bila urutan
        // array berubah, dan mentok 120px di sebagian besar feed).
        var imageUrl: String? = null
        val thumbs = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        if (thumbs != null) {
            val candidates = ArrayList<ThumbCandidate>()
            for (i in 0 until thumbs.length()) {
                val t = thumbs.optJSONObject(i) ?: continue
                candidates += ThumbCandidate(
                    t.optInt("width", 0),
                    t.optString("url", ""),
                )
            }
            imageUrl = selectArtworkUrl(candidates)
        }
        if (imageUrl.isNullOrBlank()) imageUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        // browseId artis: run pertama di kolom artis yang membawa
        // browseEndpoint UC… (terverifikasi dari fixture browse/search
        // 22 Nov 2026). Tanpa ini profil artis memakai fallback search.
        var artistBrowseId: String? = null
        if (columns.length() > 1) {
            val runs = columns.getJSONObject(1)
                .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")?.optJSONArray("runs")
            if (runs != null) {
                for (i in 0 until runs.length()) {
                    val bid = runs.getJSONObject(i)
                        .optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")
                        ?.optString("browseId", null)
                        ?.takeIf { it.isNotBlank() }
                    if (bid != null && bid.startsWith("UC")) { artistBrowseId = bid; break }
                }
            }
        }
        val isSong = item.optJSONObject("overlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("playNavigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("watchEndpointMusicSupportedConfigs")
            ?.contains("MUSIC_VIDEO_TYPE_ATV") == true ||
            item.toString().contains("MUSIC_VIDEO_TYPE_ATV")
        return YtmResult(videoId, title, artist, durationMs, isSong, explicit, imageUrl, artistBrowseId, playCount, countKind, year, isPodcastEpisode, podcastShowTitle)
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
        val runs = textObj.optJSONArray("runs") ?: return textObj.optString("simpleText", null)
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.getJSONObject(i).optString("text", ""))
        }
        return sb.toString().ifEmpty { null }
    }

    private fun parseDuration(s: String): Long? {
        val t = s.trim()
        // "3:45" atau "4.32" (locale id memakai titik) atau "1:02:03"
        val normalized = if (t.matches(Regex("\\d{1,2}\\.\\d{2}"))) t.replace(".", ":") else t
        if (!normalized.matches(Regex("\\d{1,2}:\\d{2}(:\\d{2})?"))) return null
        val parts = normalized.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            else -> null
        }
    }
}
