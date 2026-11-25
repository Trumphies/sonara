package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Logika lirik batch 2 (v1.0.4) — MURNI, bisa diuji tanpa Compose.
 *
 * Batas data (dibuktikan probe live 25 Nov 2026, fixture di
 * harness149/fixtures/): LRCLIB TIDAK mengembalikan field terjemahan
 * atau romaji apa pun (79 entri, 4 lagu EN/JP/KR/ID — union key
 * identik, tanpa `translatedLyrics`/`romanizedLyrics`); endpoint
 * timedtext YouTube mengembalikan body KOSONG untuk semua bahasa dan
 * respons player TIDAK punya `captionTracks` — jadi terjemahan
 * per-baris, romaji, dan lirik-dari-caption TIDAK DIBANGUN (membangun
 * nya berarti mengarang data). Yang dibangun di sini: offset timing,
 * cari-di-lirik, edit lokal, pemilih sumber (dua sumber NYATA yang
 * sudah dipakai app: LRCLIB + lirik bawaan YouTube Music), dan gaya
 * tampilan lirik fullscreen.
 */

/**
 * Offset timing lirik (v1.0.4): posisi efektif = posisi putar +
 * offset global + offset per-lagu. Offset TIDAK mengubah posisi
 * putar/pemutar — hanya baris aktif mana yang dianggap sedang
 * dinyanyikan (dan highlight per-kata).
 */
object LyricsOffsetPolicy {
    /** Langkah slider offset: 0,5 detik. */
    const val OFFSET_STEP_MS: Long = 500L

    /** Offset maksimum per arah: 10 detik. */
    const val OFFSET_MAX_MS: Long = 10_000L

    /** Jepit offset ke ±[OFFSET_MAX_MS]. */
    fun clampOffset(offsetMs: Long): Long =
        offsetMs.coerceIn(-OFFSET_MAX_MS, OFFSET_MAX_MS)

    /** Posisi lirik efektif; tidak pernah negatif. */
    fun effectivePositionMs(
        positionMs: Long,
        globalOffsetMs: Long,
        perSongOffsetMs: Long,
    ): Long = (positionMs + globalOffsetMs + perSongOffsetMs).coerceAtLeast(0L)

    /** Label offset untuk UI, mis. "+1.5s", "-0.5s", "0s". */
    fun offsetLabel(offsetMs: Long): String {
        if (offsetMs == 0L) return "0s"
        val sign = if (offsetMs > 0) "+" else "-"
        val abs = kotlin.math.abs(offsetMs)
        val whole = abs / 1000
        val frac = (abs % 1000) / 100
        return if (frac == 0L) "$sign${whole}s" else "$sign$whole.${frac}s"
    }
}

/** Cari di dalam lirik yang sudah dimuat (v1.0.4) — substring, tak peduli kapital. */
object LyricsFindPolicy {
    /** Indeks baris yang mengandung [query]; query kosong = tidak ada. */
    fun findMatches(lines: List<LyricsApi.Line>, query: String): List<Int> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return lines.mapIndexedNotNull { i, line ->
            i.takeIf { line.text.contains(q, ignoreCase = true) }
        }
    }

    /**
     * Match berikutnya/sebelumnya dari [currentMatchPos] (posisi di
     * dalam daftar [matches], -1 = belum ada); melingkar. -1 bila
     * tidak ada match.
     */
    fun nextMatch(matches: List<Int>, currentMatchPos: Int, forward: Boolean): Int {
        if (matches.isEmpty()) return -1
        return if (forward) {
            matches[(currentMatchPos + 1).mod(matches.size)]
        } else {
            matches[(currentMatchPos - 1).mod(matches.size)]
        }
    }
}

/** Satu override edit lirik untuk satu lagu (lokal saja, tidak disinkron). */
data class LyricsEdit(
    val kind: LyricsApi.Kind,
    val lines: List<LyricsApi.Line>,
    val savedAtMs: Long,
)

/** Codec JSON untuk [LyricsEdit] — dipakai LocalStore (pola yang sama dengan resume positions). */
object LyricsEditCodec {
    fun editToJson(edit: LyricsEdit): String {
        val root = JSONObject()
        root.put("kind", edit.kind.name)
        root.put("savedAt", edit.savedAtMs)
        val arr = JSONArray()
        for (line in edit.lines) {
            val o = JSONObject()
            o.put("t", line.timeMs)
            o.put("text", line.text)
            if (line.endMs > 0) o.put("end", line.endMs)
            if (line.words.isNotEmpty()) {
                val warr = JSONArray()
                for (w in line.words) {
                    val wo = JSONObject()
                    wo.put("t", w.timeMs)
                    wo.put("text", w.text)
                    warr.put(wo)
                }
                o.put("words", warr)
            }
            arr.put(o)
        }
        root.put("lines", arr)
        return root.toString()
    }

    fun editFromJson(raw: String?): LyricsEdit? {
        if (raw.isNullOrBlank()) return null
        return try {
            val root = JSONObject(raw)
            val kind = try {
                LyricsApi.Kind.valueOf(root.optString("kind", ""))
            } catch (_: IllegalArgumentException) {
                return null
            }
            val arr = root.optJSONArray("lines") ?: return null
            val lines = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val warr = o.optJSONArray("words")
                val words = if (warr == null) {
                    emptyList()
                } else {
                    (0 until warr.length()).mapNotNull { j ->
                        val wo = warr.optJSONObject(j) ?: return@mapNotNull null
                        LyricsApi.Word(wo.optLong("t", 0L), wo.optString("text", ""))
                    }
                }
                LyricsApi.Line(
                    o.optLong("t", 0L),
                    o.optString("text", ""),
                    words,
                    o.optLong("end", 0L),
                )
            }.filter { it.text.isNotBlank() }
            if (lines.isEmpty()) return null
            LyricsEdit(kind, lines, root.optLong("savedAt", 0L))
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Terapkan teks hasil edit ke [original]: teks per indeks, jumlah
     * baris tetap; baris yang teks barunya kosong DIBUANG; timestamp
     * dipertahankan; timing per-kata dibuang HANYA pada baris yang
     * teksnya berubah (timing kata lama tidak lagi cocok dengan teks
     * baru — mempertahankannya berarti highlight kata yang salah).
     */
    fun applyEdit(
        original: List<LyricsApi.Line>,
        editedTexts: List<String>,
    ): List<LyricsApi.Line> {
        return original.mapIndexedNotNull { i, line ->
            val newText = editedTexts.getOrNull(i)?.trim() ?: line.text
            if (newText.isBlank()) return@mapIndexedNotNull null
            if (newText == line.text) line
            else LyricsApi.Line(line.timeMs, newText, emptyList(), line.endMs)
        }
    }
}

/** Codec JSON untuk peta offset per-lagu: {trackId: offsetMs}. */
object LyricsOffsetsCodec {
    fun toJson(offsets: Map<String, Long>): String {
        val root = JSONObject()
        for ((k, v) in offsets) root.put(k, v)
        return root.toString()
    }

    fun fromJson(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = LinkedHashMap<String, Long>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = root.optLong(k, 0L)
            }
            out.filterValues { it != 0L }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

/** Urutan sumber yang dicoba untuk satu preferensi (v1.0.4). */
object LyricsSourceOrder {
    fun fetchOrder(pref: LyricsApi.LyricsSource): List<LyricsApi.LyricsSource> {
        return when (pref) {
            LyricsApi.LyricsSource.AUTO -> listOf(
                LyricsApi.LyricsSource.LRCLIB,
                LyricsApi.LyricsSource.YOUTUBE_MUSIC,
            )
            LyricsApi.LyricsSource.LRCLIB -> listOf(LyricsApi.LyricsSource.LRCLIB)
            LyricsApi.LyricsSource.YOUTUBE_MUSIC ->
                listOf(LyricsApi.LyricsSource.YOUTUBE_MUSIC)
        }
    }
}

/** Gaya tampilan lirik fullscreen (v1.0.4): ukuran teks saja — warna + follow sama. */
enum class LyricsStyle(val fontSizeSp: Int, val lineHeightSp: Int) {
    STANDARD(22, 28),
    LARGE(30, 38),
}

/** True bila ada baris yang membawa timing per-kata ASLI dari sumber. */
fun hasWordTimings(lines: List<LyricsApi.Line>): Boolean =
    lines.any { it.words.isNotEmpty() }
