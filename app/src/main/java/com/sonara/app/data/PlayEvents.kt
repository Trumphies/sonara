package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

/**
 * Catatan peristiwa putar (v1.0.6): satu baris per lagu yang
 * benar-benar mulai diputar, dengan timestamp. [LocalStore.play_counts_v1]
 * hanya menyimpan hitungan + terakhir diputar — darinya statistik
 * "Your Year" TIDAK BISA dihitung jujur (semua putar lama akan
 * diklaim tahun ini). Karena itu peristiwa dicatat mulai v1.0.6
 * dan "Your Year" hanya memakai peristiwa tercatat, dengan label
 * jujur di UI. Logika MURNI (tanpa Android).
 *
 * Kejujuran data: menit dengar hanya dari peristiwa yang durasinya
 * diketahui (> 0); peristiwa tanpa durasi tetap dihitung putarnya
 * tapi tidak menyumbang menit (selisihnya ditampilkan di UI).
 */

/** Satu peristiwa putar yang tercatat. */
data class PlayEvent(
    val trackId: String,
    val atMs: Long,
    val durationMs: Long,
    val title: String,
    val artist: String,
    val imageUrl: String? = null,
)

data class YearTopTrack(
    val trackId: String,
    val title: String,
    val artist: String,
    val plays: Int,
)

data class YearTopArtist(
    val artist: String,
    val plays: Int,
)

/** Statistik satu tahun kalender dari peristiwa tercatat. */
data class YearStats(
    /** Jumlah peristiwa putar pada tahun itu. */
    val plays: Int,
    /** Menit dengar (durasi diketahui saja), pembulatan ke bawah. */
    val minutes: Long,
    /** Peristiwa yang durasinya diketahui (> 0). */
    val knownDurationPlays: Int,
    val topTracks: List<YearTopTrack>,
    val topArtists: List<YearTopArtist>,
) {
    val hasData: Boolean get() = plays > 0
}

object PlayEvents {
    /** Batas simpan prefs: peristiwa tertua dibuang (lihat [recordPlayEvent]). */
    const val MAX_EVENTS = 2000

    const val TOP_LIMIT = 5

    /** Tahun kalender (UTC) dari epoch-ms. */
    fun yearOf(atMs: Long): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = atMs
        return cal.get(Calendar.YEAR)
    }

    /** Tambah satu peristiwa, buang yang tertua bila melewati batas. */
    fun recordPlayEvent(
        existing: List<PlayEvent>,
        event: PlayEvent,
        maxEvents: Int = MAX_EVENTS,
    ): List<PlayEvent> {
        if (maxEvents <= 0) return emptyList()
        return (existing + event).takeLast(maxEvents)
    }

    /** Statistik tahun [year] dari peristiwa tercatat (tahun lain dikecualikan). */
    fun computeYearStats(events: List<PlayEvent>, year: Int): YearStats {
        val inYear = events.filter { yearOf(it.atMs) == year }
        val known = inYear.filter { it.durationMs > 0 }
        val minutes = known.sumOf { it.durationMs } / 60_000L
        val trackGroups = LinkedHashMap<String, MutableList<PlayEvent>>()
        for (e in inYear) trackGroups.getOrPut(e.trackId) { mutableListOf() }.add(e)
        val topTracks = trackGroups.values.mapNotNull { group ->
            val latest = group.maxByOrNull { it.atMs } ?: return@mapNotNull null
            YearTopTrack(
                trackId = latest.trackId,
                title = latest.title,
                artist = displayArtist(latest.artist),
                plays = group.size,
            )
        }.sortedWith(
            compareByDescending<YearTopTrack> { it.plays }.thenBy { it.title.lowercase() },
        ).take(TOP_LIMIT)
        val artistGroups = LinkedHashMap<String, MutableList<PlayEvent>>()
        for (e in inYear) {
            val artist = displayArtist(e.artist)
            if (artist.isEmpty()) continue
            artistGroups.getOrPut(artist.lowercase()) { mutableListOf() }.add(e)
        }
        val topArtists = artistGroups.values.mapNotNull { group ->
            val latest = group.maxByOrNull { it.atMs } ?: return@mapNotNull null
            YearTopArtist(
                artist = displayArtist(latest.artist),
                plays = group.size,
            )
        }.sortedWith(
            compareByDescending<YearTopArtist> { it.plays }.thenBy { it.artist.lowercase() },
        ).take(TOP_LIMIT)
        return YearStats(
            plays = inYear.size,
            minutes = minutes,
            knownDurationPlays = known.size,
            topTracks = topTracks,
            topArtists = topArtists,
        )
    }

    // ---- serialisasi JSON (org.json; LocalStore hanya menyimpan string) ----

    fun eventsToJson(events: List<PlayEvent>): JSONArray {
        val arr = JSONArray()
        for (e in events) {
            arr.put(JSONObject().apply {
                put("id", e.trackId)
                put("at", e.atMs)
                put("dur", e.durationMs)
                put("title", e.title)
                put("artist", e.artist)
                put("img", e.imageUrl ?: "")
            })
        }
        return arr
    }

    fun eventsFromJson(raw: String?): List<PlayEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<PlayEvent>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id")
                val at = o.optLong("at", 0L)
                if (id.isBlank() || at <= 0) continue
                out += PlayEvent(
                    trackId = id,
                    atMs = at,
                    durationMs = o.optLong("dur", 0L),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    imageUrl = o.optString("img").takeIf { it.isNotEmpty() },
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Bantu fixture: round-trip lewat string JSON. */
    fun eventsRoundTrip(events: List<PlayEvent>): List<PlayEvent> =
        eventsFromJson(eventsToJson(events).toString())
}
