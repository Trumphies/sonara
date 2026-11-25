package com.sonara.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Durasi lagu per videoId (v1.10.31).
 *
 * Latar: jalur feed Kotlin/WEB_REMIX TIDAK membawa durasi sama sekali
 * (subtitle hanya "Lagu • Artis"; probe 0/30 item punya durasi) — jadi
 * durasi hanya bisa diketahui lewat [lookupVideo] (lengthSeconds dari
 * videoDetails). Kelas ini mengisi kesenjangan itu TANPA mengubah
 * parser: baris yang tampil meminta durasinya sendiri (hanya baris
 * yang masuk komposisi = baris terlihat), hasilnya di-cache di memori
 * + dipersistensikan, dan baris lain untuk lagu yang sama langsung
 * ikut menampilkan tanpa request ulang.
 *
 * Bukan resolve stream: tidak ada URL yang diambil, tidak ada yang
 * kedaluwarsa. Satu request = satu POST /player (+1 retry di dalam
 * lookupVideo). Tidak pernah dipakai untuk memutar.
 *
 * Murni Kotlin + coroutines (tanpa dependensi Android) supaya bisa
 * diuji harness JVM dengan store/lookup palsu.
 */
class TrackDurations(
    loadPersisted: () -> Map<String, Long>,
    private val savePersisted: (Map<String, Long>) -> Unit,
    private val lookup: (String) -> Result<Long>,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    companion object {
        /** Batas entri cache (tertua dibuang duluan). */
        const val MAX_ENTRIES = 500

        /** Jeda antar request lookup — debounce ledakan baris yang tampil bersamaan. */
        const val REQUEST_SPACING_MS = 120L

        /** Jangan ulangi video yang lookup-nya gagal selama ini. */
        const val FAILURE_COOLDOWN_MS = 60_000L

        /** Video yang lookup-nya sukses tapi tanpa durasi (live/dihapus): jangan diulang terus. */
        const val UNKNOWN_COOLDOWN_MS = 600_000L
    }

    private val _cache = MutableStateFlow<Map<String, Long>>(
        LinkedHashMap<String, Long>().also { map ->
            loadPersisted().forEach { (k, v) ->
                if (k.isNotBlank() && v > 0) map[k] = v
            }
        },
    )

    /** videoId -> durationMs yang sudah diketahui (dipersistensikan). */
    val cache: StateFlow<Map<String, Long>> = _cache.asStateFlow()

    private val queue = Channel<String>(Channel.UNLIMITED)
    private val lock = Any()
    private val pendingOrFlight = HashSet<String>()
    private val cooldownUntil = HashMap<String, Long>()

    init {
        scope.launch {
            for (id in queue) {
                delay(REQUEST_SPACING_MS)
                val result = runCatching { lookup(id) }.getOrElse { Result.failure(it) }
                val ms = result.getOrNull() ?: 0L
                synchronized(lock) { pendingOrFlight.remove(id) }
                when {
                    result.isSuccess && ms > 0 -> put(id, ms)
                    result.isSuccess -> synchronized(lock) {
                        cooldownUntil[id] = System.currentTimeMillis() + UNKNOWN_COOLDOWN_MS
                    }
                    else -> synchronized(lock) {
                        cooldownUntil[id] = System.currentTimeMillis() + FAILURE_COOLDOWN_MS
                    }
                }
            }
        }
    }

    /** Durasi cache untuk [videoId], 0 = belum diketahui. */
    fun cachedMs(videoId: String?): Long {
        if (videoId.isNullOrBlank()) return 0L
        return _cache.value[videoId] ?: 0L
    }

    /**
     * Durasi tampil untuk [track]: durasi bawaan track bila ada,
     * else durasi cache. Tidak pernah mengarang — 0 = belum diketahui.
     */
    fun resolvedMs(track: Track): Long =
        track.durationMs.takeIf { it > 0 } ?: cachedMs(track.youtubeVideoId)

    /**
     * Minta durasi [videoId] di-resolve (idempotent: dedupe cache,
     * antrean, dan cooldown). Aman dipanggil tiap komposisi baris.
     */
    fun request(videoId: String?) {
        if (videoId.isNullOrBlank()) return
        synchronized(lock) {
            if (_cache.value.containsKey(videoId)) return
            if (pendingOrFlight.contains(videoId)) return
            val until = cooldownUntil[videoId] ?: 0L
            if (System.currentTimeMillis() < until) return
            pendingOrFlight.add(videoId)
        }
        queue.trySend(videoId)
    }

    /** Masukkan durasi yang sudah diketahui (dari lookup / resolve lain) + persistensikan. */
    fun put(videoId: String, durationMs: Long) {
        if (videoId.isBlank() || durationMs <= 0) return
        val next = LinkedHashMap(_cache.value)
        next.remove(videoId)
        next[videoId] = durationMs
        while (next.size > MAX_ENTRIES) {
            val eldest = next.keys.firstOrNull() ?: break
            next.remove(eldest)
        }
        _cache.value = next
        runCatching { savePersisted(next) }
    }
}
