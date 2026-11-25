package com.sonara.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Lirik sinkron (ala YouTube Music) via LRCLIB — API gratis tanpa key
 * (lrclib.net, dipakai juga oleh MixMusic sebagai salah satu sumber
 * lirik). Endpoint /api/get mengembalikan `syncedLyrics` format LRC:
 * baris "[mm:ss.cc] teks". Kalau lagu tidak ada di LRCLIB, hasilnya
 * list kosong — UI menampilkan "Lirik tidak tersedia", bukan karangan.
 *
 * v1.10.1: cakupan diperluas TANPA mengarang — (1) varian query
 * LRCLIB (tanpa album, tanpa durasi, nama artis/judul tanpa
 * "feat."), (2) fallback lirik POLOS dari LRCLIB bila hanya teks
 * tanpa timing yang ada (ditandai jujur "lirik tanpa timing",
 * TIDAK pura-pura sinkron), (3) fallback lirik bawaan YouTube
 * Music via InnerTube (jalur tidak resmi yang sama dengan
 * search/stream app — browse lyrics, teks polos saja, sumber
 * Musixmatch). Lirik TIDAK PERNAH dikarang/ditebak.
 */
/**
 * Kebijakan versi lirik (v1.10.39) — fungsi MURNI, dipakai kode
 * produksi dan diuji harness139.
 *
 * Masalah yang diperbaiki: LRCLIB menyimpan BEBERAPA versi satu lagu
 * (durasi berbeda) yang timestamp-nya bergeser puluhan detik —
 * mis. "Blinding Lights" 200 dtk (baris pertama 13,13) vs 263 dtk
 * (baris pertama 35,99). Bila durasi lagu tidak diketahui saat fetch
 * (salinan feed: durationMs 0), entri versi-salah diterima tanpa
 * verifikasi dan tidak pernah diperbaiki — lirik tampak "gak
 * singkron" di banyak lagu. Aturan: durasi tak diketahui = terima
 * provisional (tampil cepat); durasi diketahui + selisih > 10 dtk =
 * tolak entri sinkron itu (turun jadi lirik polos, jujur — bukan
 * pura-pura sinkron). Ambil-ulang dipicu saat durasi yang
 * terverifikasi tiba setelah fetch provisional.
 */
object LyricsVersionPolicy {
    /** Toleransi versi (sama dengan guard handleHit sejak v1.10.1). */
    const val VERSION_TOLERANCE_MS: Long = 10_000L

    /** Perubahan durasi di atas ini dianggap durasi baru (ambil-ulang). */
    const val REFETCH_DRIFT_MS: Long = 2_000L

    /**
     * True bila entri sinkron boleh dipakai: durasi entri/lagu tak
     * diketahui = provisional (tidak ada yang bisa diverifikasi);
     * bila keduanya diketahui, selisih harus dalam toleransi.
     */
    fun isSyncedEntryUsable(entryDurationMs: Long, knownDurationMs: Long): Boolean {
        if (knownDurationMs <= 0 || entryDurationMs <= 0) return true
        return kotlin.math.abs(entryDurationMs - knownDurationMs) <= VERSION_TOLERANCE_MS
    }

    /**
     * True bila lirik harus (di)ambil: lagu berganti, fetch
     * sebelumnya provisional (durasi 0) dan durasi kini diketahui,
     * atau durasi yang diketahui berubah material.
     */
    fun needsFetch(
        fetchedTrackId: String?,
        fetchedDurationUsedMs: Long,
        trackId: String,
        knownDurationMs: Long,
    ): Boolean {
        if (fetchedTrackId != trackId) return true
        if (fetchedDurationUsedMs <= 0 && knownDurationMs > 0) return true
        if (fetchedDurationUsedMs > 0 && knownDurationMs > 0 &&
            kotlin.math.abs(knownDurationMs - fetchedDurationUsedMs) > REFETCH_DRIFT_MS
        ) return true
        return false
    }
}

object LyricsApi {
    /** Satu kata + kapan mulai dinyanyikan (hanya bila hasWordSync). */
    data class Word(val timeMs: Long, val text: String)

    data class Line(
        val timeMs: Long,
        val text: String,
        /** Kata-kata + timestamp-nya; kosong kalau hanya lirik per baris. */
        val words: List<Word> = emptyList(),
        val endMs: Long = 0L,
    )

    /** Jenis lirik yang didapat: ber-timing, polos, atau tidak ada. */
    enum class Kind { SYNCED, PLAIN, EMPTY }

    /** Hasil pencarian lirik: baris + jenis + sumber asli (untuk caption jujur). */
    data class LyricsResult(
        val lines: List<Line>,
        val kind: Kind,
        val source: String,
    )

    private val http: OkHttpClient = SonaraHttp.defaultHttp()

    /** Kunci publik client web YouTube (ada di source youtube.com) — BUKAN rahasia. */
    private const val YTM_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val YTM_NEXT_URL =
        "https://music.youtube.com/youtubei/v1/next?key=$YTM_API_KEY"
    private const val YTM_BROWSE_URL =
        "https://music.youtube.com/youtubei/v1/browse?key=$YTM_API_KEY"

    /** Sumber lirik yang dipilih pengguna (v1.0.4). AUTO = perilaku lama. */
    enum class LyricsSource { AUTO, LRCLIB, YOUTUBE_MUSIC }

    /**
     * Cari lirik dari sumber pilihan (v1.0.4). AUTO = LRCLIB dulu
     * (sinkron), lalu lirik bawaan YTM, lalu lirik polos LRCLIB —
     * persis urutan lama. Pilihan eksplisit TIDAK jatuh ke sumber
     * lain: kosong dari sumber itu = [Kind.EMPTY] yang jujur.
     * Mengembalikan [Kind.EMPTY] bila tidak ada sumber yang punya
     * lirik — pemanggil menampilkan state kosong yang jujur.
     */
    fun fetchLyrics(
        artist: String,
        title: String,
        album: String,
        durationMs: Long,
        videoId: String? = null,
        source: LyricsSource = LyricsSource.AUTO,
    ): LyricsResult {
        return when (source) {
            LyricsSource.LRCLIB -> fetchFromLrclib(artist, title, album, durationMs)
            LyricsSource.YOUTUBE_MUSIC -> fetchFromYtm(videoId)
            LyricsSource.AUTO -> {
                val lrclib = fetchFromLrclib(artist, title, album, durationMs)
                if (lrclib.kind == Kind.SYNCED) return lrclib
                val ytm = fetchFromYtm(videoId)
                if (ytm.lines.isNotEmpty()) return ytm
                lrclib
            }
        }
    }

    /**
     * Rantai LRCLIB saja (v1.10.1, dipisah v1.0.4): sinkron (varian
     * query) → polos. Mengembalikan [Kind.EMPTY] bila LRCLIB tidak
     * punya lirik lagu ini.
     */
    fun fetchFromLrclib(
        artist: String,
        title: String,
        album: String,
        durationMs: Long,
    ): LyricsResult {
        var plainFallback: LyricsResult? = null
        fun recordPlain(lines: List<Line>, source: String) {
            if (plainFallback == null && lines.isNotEmpty()) {
                plainFallback = LyricsResult(lines, Kind.PLAIN, source)
            }
        }
        /**
         * Nilai satu entri LRCLIB: lirik sinkron dikembalikan hanya
         * bila durasi entrinya cocok dengan lagu (toleransi 10 detik).
         * Entri versi lain (mis. lirik versi standar untuk lagu
         * extended 641 detik) timingnya salah — diturunkan jadi
         * lirik POLOS, bukan pura-pura sinkron. Null = lanjut cari.
         */
        fun handleHit(hit: JSONObject): LyricsResult? {
            val synced = parseSyncedHit(hit)
            if (synced.isEmpty()) {
                recordPlain(parsePlainHit(hit), "LRCLIB")
                return null
            }
            val entryDurMs = (hit.optDouble("duration", -1.0) * 1000).toLong()
            if (!LyricsVersionPolicy.isSyncedEntryUsable(entryDurMs, durationMs)) {
                recordPlain(synced.map { Line(0L, it.text) }, "LRCLIB")
                return null
            }
            return LyricsResult(synced, Kind.SYNCED, "LRCLIB")
        }
        // Varian query LRCLIB — berhenti di lirik sinkron pertama.
        // (Durasi: /api/get juga mencocokkan durasi di server; varian
        // tanpa durasi + /api/search dengan toleransi ±2 detik menutup
        // lagu yang durasinya beda versi, mis. Heartstrings/extended.)
        val artistClean = stripFeat(artist)
        val titleClean = stripFeat(title)
        val titleBase = titleBase(titleClean)
        val attempts = listOf(
            Triple(artist, title, album to durationMs),
            Triple(artist, title, "" to durationMs),
            Triple(artist, title, "" to 0L),
            Triple(artistClean, title, "" to 0L),
            Triple(artist, titleClean, "" to 0L),
            Triple(artistClean, titleClean, "" to 0L),
        ) + if (titleBase.isNotBlank() && titleBase != titleClean) {
            listOf(Triple(artistClean, titleBase, "" to 0L))
        } else {
            emptyList()
        }
        for ((a, t, ad) in attempts) {
            val (alb, dur) = ad
            val hit = lrclibGet(a, t, alb, dur) ?: continue
            handleHit(hit)?.let { return it }
        }
        // /api/search + toleransi durasi ±2 detik (varian nama bersih).
        val searchPairs = listOf(artist to title, artistClean to titleClean) +
            if (titleBase.isNotBlank() && titleBase != titleClean) {
                listOf(artistClean to titleBase)
            } else {
                emptyList()
            }
        for ((a, t) in searchPairs) {
            val hit = lrclibSearch(a, t, durationMs) ?: continue
            handleHit(hit)?.let { return it }
        }
        // Lirik polos LRCLIB tetap lebih baik daripada kosong.
        plainFallback?.let { return it }
        return LyricsResult(emptyList(), Kind.EMPTY, "")
    }

    /** Lirik bawaan YouTube Music saja (InnerTube browse) — teks polos. */
    fun fetchFromYtm(videoId: String?): LyricsResult {
        if (videoId.isNullOrBlank()) return LyricsResult(emptyList(), Kind.EMPTY, "")
        val ytm = ytmLyrics(videoId)
        if (ytm.isEmpty()) return LyricsResult(emptyList(), Kind.EMPTY, "")
        return LyricsResult(ytm, Kind.PLAIN, "YouTube Music")
    }

    /** Kompatibilitas: hanya lirik sinkron (dipakai jalur lama). */
    fun syncedLyrics(artist: String, title: String, album: String, durationMs: Long): List<Line> {
        val r = fetchLyrics(artist, title, album, durationMs, null)
        return if (r.kind == Kind.SYNCED) r.lines else emptyList()
    }

    /** Hapus penanda kolaborator dari nama artis/judul: "(feat. X)", "feat. X", "ft. X", "featuring X". */
    internal fun stripFeat(name: String): String {
        var out = name
        // "(feat. ...)" / "[feat. ...]" di dalam kurung — hapus beserta kurungnya.
        out = out.replace(Regex("""[\[(]\s*(feat\.?|ft\.?|featuring)\b[^\]\)]*[\])]""", RegexOption.IGNORE_CASE), "")
        // "feat. X" / "ft. X" di akhir tanpa kurung.
        out = out.replace(Regex("""\s+(feat\.?|ft\.?|featuring)\b.*$""", RegexOption.IGNORE_CASE), "")
        // "Artist & ..." / "Artist x ..." — JANGAN dipotong: "&" dan
        // "x" juga nama grup yang sah (mis. duo). Hanya rapikan spasi.
        return out.replace(Regex("""\s+"""), " ").trim().trim(',', ' ')
    }

    /** Judul dasar: buang tag versi "(...)" / "[...]" — "Blinding Lights (Extended Version)" → "Blinding Lights". */
    internal fun titleBase(name: String): String {
        val stripped = stripFeat(name)
        val cut = stripped.indexOfFirst { it == '(' || it == '[' }
        return if (cut > 0) stripped.substring(0, cut).trim() else stripped
    }

    /** Satu panggilan /api/get; null bila tidak ada entri / HTTP gagal. */
    private fun lrclibGet(
        artist: String,
        title: String,
        album: String,
        durationMs: Long,
    ): JSONObject? {
        return try {
            val params = buildString {
                append("artist_name=").append(URLEncoder.encode(artist, "UTF-8"))
                append("&track_name=").append(URLEncoder.encode(title, "UTF-8"))
                if (album.isNotBlank()) {
                    append("&album_name=").append(URLEncoder.encode(album, "UTF-8"))
                }
                if (durationMs > 0) {
                    append("&duration=").append(durationMs / 1000)
                }
            }
            val req = Request.Builder()
                .url("https://lrclib.net/api/get?$params")
                .header("User-Agent", "SONARA/1.0 (https://terablox.store)")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val text = resp.body?.string() ?: return null
                val json = JSONObject(text)
                // /api/get mengembalikan {} kosong (tanpa status) bila
                // tidak ada — anggap tidak ada entri.
                if (json.optString("trackName", "").isBlank() &&
                    json.optString("syncedLyrics", "").isBlank() &&
                    json.optString("plainLyrics", "").isBlank()
                ) {
                    return null
                }
                json
            }
        } catch (_: Exception) {
            null
        }
    }

    /** /api/search: pilih entri durasi ±2 detik yang punya lirik; null bila tidak ada. */
    private fun lrclibSearch(artist: String, title: String, durationMs: Long): JSONObject? {
        return try {
            val params = "artist_name=${URLEncoder.encode(artist, "UTF-8")}" +
                "&track_name=${URLEncoder.encode(title, "UTF-8")}"
            val req = Request.Builder()
                .url("https://lrclib.net/api/search?$params")
                .header("User-Agent", "SONARA/1.0 (https://terablox.store)")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val text = resp.body?.string() ?: return null
                val arr = org.json.JSONArray(text)
                if (arr.length() == 0) return null
                val entries = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                fun hasLyrics(o: JSONObject) =
                    o.optString("syncedLyrics", "").isNotBlank() ||
                        o.optString("plainLyrics", "").isNotBlank()
                // Prioritas: durasi cocok ±2 detik + punya lirik.
                val inTolerance = entries.filter { o ->
                    val d = (o.optDouble("duration", -1.0) * 1000).toLong()
                    durationMs <= 0 || (d >= 0 && kotlin.math.abs(d - durationMs) <= 2000)
                }.filter(::hasLyrics)
                inTolerance.firstOrNull()
                    ?: entries.filter(::hasLyrics).firstOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Lirik sinkron dari satu entri LRCLIB (word-sync diutamakan, lalu LRC). */
    private fun parseSyncedHit(json: JSONObject): List<Line> {
        val withWords = if (json.optBoolean("hasWordSync", false)) {
            parseLyricsFile(json.optString("lyricsfile", ""))
        } else {
            emptyList()
        }
        if (withWords.isNotEmpty()) return withWords
        return parseLrc(json.optString("syncedLyrics", ""))
    }

    /** Lirik polos dari satu entri LRCLIB → baris tanpa timing (timeMs 0). */
    private fun parsePlainHit(json: JSONObject): List<Line> {
        val plain = json.optString("plainLyrics", "")
        if (plain.isBlank()) return emptyList()
        return plain.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { Line(0L, it) }
    }

    // ---- Lirik bawaan YouTube Music (InnerTube browse, teks polos) ----

    /**
     * Ambil lirik bawaan YTM untuk sebuah videoId (v1.10.1).
     * Jalur: POST next → cari tab "Lyrics" (browseId MPLY…) → POST
     * browse → musicDescriptionShelfRenderer.description. Hasilnya
     * teks polos TANPA timing (footer "Sumber: Musixmatch" dibuang).
     * Diverifikasi dari VM 22 Nov 2026 (8/10 lagu batch). Kosong bila
     * endpoint tidak memberi lirik — pemanggil TIDAK mengarang.
     */
    private fun ytmLyrics(videoId: String): List<Line> {
        return try {
            val browseId = ytmLyricsBrowseId(videoId) ?: return emptyList()
            val body = JSONObject().apply {
                put("browseId", browseId)
                put("context", ytmContext())
            }.toString().toRequestBody(JSON_MEDIA)
            val req = Request.Builder()
                .url(YTM_BROWSE_URL)
                .header("User-Agent", YTM_UA)
                .header("Accept", "application/json")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val text = resp.body?.string() ?: return emptyList()
                val root = JSONObject(text)
                val shelfText = walkJson(root) { node ->
                    val shelf = node.optJSONObject("musicDescriptionShelfRenderer")
                        ?: return@walkJson null
                    runsText(shelf.optJSONObject("description"))
                        ?: shelf.optJSONObject("description")?.optString("simpleText")
                } ?: return emptyList()
                shelfText.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("Sumber:", ignoreCase = true) }
                    .map { Line(0L, it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Cari browseId tab Lyrics (MPLY…) dari respons watch-next. */
    private fun ytmLyricsBrowseId(videoId: String): String? {
        return try {
            val body = JSONObject().apply {
                put("videoId", videoId)
                put("context", ytmContext())
            }.toString().toRequestBody(JSON_MEDIA)
            val req = Request.Builder()
                .url(YTM_NEXT_URL)
                .header("User-Agent", YTM_UA)
                .header("Accept", "application/json")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val text = resp.body?.string() ?: return null
                val root = JSONObject(text)
                // Lapis 1: tab yang judulnya "Lyrics".
                val titled = walkJson(root) { node ->
                    val tab = node.optJSONObject("tabRenderer") ?: return@walkJson null
                    val titleRaw = tab.opt("title")
                    val title = when (titleRaw) {
                        is String -> titleRaw
                        is JSONObject -> runsText(titleRaw)
                            ?: titleRaw.optString("simpleText", "")
                        else -> ""
                    }
                    if (!title.equals("Lyrics", ignoreCase = true)) return@walkJson null
                    tab.optJSONObject("endpoint")
                        ?.optJSONObject("browseEndpoint")
                        ?.optString("browseId", null)
                        ?.takeIf { it.startsWith("MPLY") }
                }
                if (titled != null) return titled
                // Lapis 2: browseId MPLY… pertama yang ditemukan.
                walkJson(root) { node ->
                    node.optString("browseId", null)
                        ?.takeIf { it.startsWith("MPLY") }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun ytmContext(): JSONObject {
        return JSONObject().apply {
            put("client", JSONObject().apply {
                put("clientName", "WEB_REMIX")
                put("clientVersion", "1.20240101.00.00")
                put("hl", "id")
                put("gl", "ID")
            })
        }
    }

    private val JSON_MEDIA = "application/json".toMediaType()
    private const val YTM_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"

    /** Jalan rekursif JSON; kembalikan hasil visit pertama yang non-null. */
    private fun walkJson(node: Any?, visit: (JSONObject) -> String?): String? {
        when (node) {
            is JSONObject -> {
                visit(node)?.let { return it }
                val keys = node.keys()
                while (keys.hasNext()) {
                    walkJson(node.get(keys.next()), visit)?.let { return it }
                }
            }
            is org.json.JSONArray -> {
                for (i in 0 until node.length()) {
                    walkJson(node.get(i), visit)?.let { return it }
                }
            }
        }
        return null
    }

    /** Gabung runs[].text (atau simpleText) jadi satu string. */
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

    /**
     * Parse `lyricsfile` LRCLIB — subset YAML yang strukturnya tetap:
     *
     * ```
     * lines:
     * - text: Look at the stars
     *   start_ms: 33370
     *   end_ms: 34667
     *   words:
     *   - text: 'Look '
     *     start_ms: 33370
     * ```
     *
     * Parser ditulis tangan untuk format ini saja (tanpa pustaka YAML):
     * item `- ` di indent 0 = baris baru, di indent 2 = kata baru;
     * pasangan `kunci: nilai` di indent ≤2 = field baris, indent ≥4 =
     * field kata. Nilai ber-quote dipertahankan spasi dalamnya
     * (kata 'Look ' memang diakhiri spasi di dalam quote).
     */
    fun parseLyricsFile(yaml: String): List<Line> {
        if (yaml.isBlank()) return emptyList()
        val out = ArrayList<Line>()
        var inLines = false
        var lineText = ""
        var lineStart = 0L
        var lineEnd = 0L
        var words = ArrayList<Word>()
        var wordText: String? = null
        var wordStart = 0L
        var haveLine = false

        fun flushWord() {
            if (wordText != null) {
                words.add(Word(wordStart, wordText!!))
                wordText = null
            }
        }
        fun flushLine() {
            flushWord()
            if (haveLine && lineText.isNotBlank()) {
                out.add(Line(lineStart, lineText, words.toList(), lineEnd))
            }
            lineText = ""
            lineStart = 0L
            lineEnd = 0L
            words = ArrayList()
            haveLine = false
        }
        fun parseScalar(raw: String): String {
            val v = raw.trim()
            if (v.length >= 2 && v.startsWith("'") && v.endsWith("'")) {
                return v.substring(1, v.length - 1).replace("''", "'")
            }
            if (v.length >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                return v.substring(1, v.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            }
            return v
        }
        fun applyField(key: String, rawValue: String, indent: Int) {
            val value = parseScalar(rawValue)
            if (indent >= 4) {
                // Field kata.
                when (key) {
                    "text" -> wordText = value
                    "start_ms" -> wordStart = value.toLongOrNull() ?: 0L
                }
            } else {
                // Field baris.
                when (key) {
                    "text" -> lineText = value
                    "start_ms" -> lineStart = value.toLongOrNull() ?: 0L
                    "end_ms" -> lineEnd = value.toLongOrNull() ?: 0L
                }
            }
        }

        for (raw in yaml.lines()) {
            if (raw.isBlank()) continue
            val indent = raw.takeWhile { it == ' ' }.length
            val trimmed = raw.trimStart()
            if (!inLines) {
                if (trimmed == "lines:") inLines = true
                continue
            }
            if (trimmed.startsWith("- ")) {
                val rest = trimmed.substring(2)
                val colon = rest.indexOf(':')
                if (colon < 0) continue
                val key = rest.substring(0, colon).trim()
                val rawValue = rest.substring(colon + 1)
                if (indent == 0) {
                    flushLine()
                    haveLine = true
                    applyField(key, rawValue, indent)
                } else {
                    // Item kata (indent 2): field-nya field KATA, bukan
                    // field baris — paksa indent ≥4 supaya applyField
                    // menulis wordText/wordStart, bukan menimpa lineText.
                    flushWord()
                    applyField(key, rawValue, 4)
                }
            } else {
                val colon = trimmed.indexOf(':')
                if (colon < 0) continue
                val key = trimmed.substring(0, colon).trim()
                val rawValue = trimmed.substring(colon + 1)
                if (key == "words" && indent <= 2) continue // penanda seksi kata
                applyField(key, rawValue, indent)
            }
        }
        flushLine()
        return out.sortedBy { it.timeMs }
    }

    /** Parse format LRC: "[mm:ss.cc] teks" (beberapa tag per baris diabaikan). */
    fun parseLrc(lrc: String): List<Line> {
        val out = ArrayList<Line>()
        val timeRe = Regex("""\[(\d+):(\d{2})(?:[.:](\d{1,3}))?]""")
        for (raw in lrc.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val m = timeRe.find(line) ?: continue
            val min = m.groupValues[1].toLongOrNull() ?: continue
            val sec = m.groupValues[2].toLongOrNull() ?: continue
            val frac = m.groupValues[3]
            val ms = when (frac.length) {
                1 -> frac.toLongOrNull()?.times(100) ?: 0L
                2 -> frac.toLongOrNull()?.times(10) ?: 0L
                3 -> frac.toLongOrNull() ?: 0L
                else -> 0L
            }
            val text = line.substring(m.range.last + 1).trim()
            if (text.isNotEmpty()) {
                out.add(Line((min * 60 + sec) * 1000 + ms, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }
}
