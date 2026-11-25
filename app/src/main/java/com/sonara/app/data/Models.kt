package com.sonara.app.data

/** Sumber sebuah track. Satu antrean boleh mencampur semuanya. */
enum class Source(val label: String) {
    YOUTUBE_MUSIC("YouTube Music"),
    LOCAL("Local"),
}

/** Cara sebuah track diputar di perangkat. */
enum class Engine {
    /** Media3 ExoPlayer native (stream langsung YTM / file lokal). */
    EXOPLAYER,
    /** Player resmi YouTube (IFrame) — HANYA fallback kalau stream langsung gagal. */
    YOUTUBE,
}

/** Kualitas audio stream YouTube Music (dipakai saat memilih format). */
enum class AudioQuality(val label: String) {
    /** Opus bitrate tertinggi (itag 251). */
    BEST("Best (Opus)"),
    /** Opus kalau ada, kalau tidak AAC 140. */
    BALANCED("Balanced"),
    /** AAC 140 saja (hemat data). */
    DATA_SAVER("Data saver (AAC)"),
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val source: Source,
    val engine: Engine,
    /** Durasi dalam ms; 0 = belum diketahui (akan diisi player). */
    val durationMs: Long = 0,
    /** URL stream langsung (stream YTM via player API / file lokal). */
    val streamUrl: String? = null,
    /** ID video YouTube (untuk engine YOUTUBE / hasil YTM). */
    val youtubeVideoId: String? = null,
    val imageUrl: String? = null,
    val explicit: Boolean = false,
    /** browseId artis (UC…) bila hasil search membawanya; null = fallback search. */
    val artistBrowseId: String? = null,
    /**
     * Hitungan putar/tonton dari subtitle hasil search (v1.0.3) —
     * 0 = respons tidak membawanya (bukan tebakan). [countKind] =
     * "plays"/"views"/"" (lihat MediaTextParse: "audiens bulanan"
     * artis TIDAK pernah jadi hitungan).
     */
    val playCount: Long = 0,
    val countKind: String = "",
    /** Tahun rilis dari subtitle ("Single • 2003"); 0 = tidak ada. */
    val year: Int = 0,
    /** True bila stream yang sedang/bakal diputar adalah siaran live (HLS). */
    val isLive: Boolean = false,
    /**
     * Penanda podcast (v1.0.5): true bila track ini episode podcast
     * (subtitle "Episode • …" di hasil search / item halaman acara).
     * [podcastShowBrowseId] = browseId acara (MPSP…), [podcastShowTitle]
     * = nama acara — dipakai membuka halaman acara + follow.
     */
    val isPodcastEpisode: Boolean = false,
    val podcastShowBrowseId: String? = null,
    val podcastShowTitle: String? = null,
    /**
     * ID unik per SALINAN di antrean (bukan ID lagu) — kunci stabil
     * LazyColumn saat drag-to-reorder (v1.6). Dua salinan lagu yang
     * sama di antrean punya queueUid berbeda. `copy()` mempertahankannya
     * (penggantian track tidak boleh mengganti identitas antrean);
     * sisipan BARU ke antrean selalu diberi UID segar oleh PlayerManager.
     */
    val queueUid: String = java.util.UUID.randomUUID().toString(),
) {
    val displayDuration: String
        get() = formatDurationMs(durationMs)
}

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Format durasi tampil: "3:45". 0/negatif = belum diketahui ("--:--",
 * placeholder jujur — bukan angka karangan).
 */
fun formatDurationMs(durationMs: Long): String {
    if (durationMs <= 0) return "--:--"
    val s = durationMs / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
/**
 * Teks share lagu (v1.10.4, fungsi murni): judul + artis + link
 * YouTube Music yang bisa diputar, dibangun dari videoId ASLI lagu
 * itu, dengan atribusi "(via SONARA)". Lagu tanpa videoId (lokal /
 * unduhan) → teks saja, TANPA link karangan.
 */
fun shareTextFor(track: Track): String {
    val url = track.youtubeVideoId
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://music.youtube.com/watch?v=$it" }
    // v1.10.19: artis kosong/label → judul saja (bukan "judul — Video").
    val head = displayArtist(track.artist).takeIf { it.isNotBlank() }
        ?.let { "${track.title} — $it (via SONARA)" }
        ?: "${track.title} (via SONARA)"
    return head + if (url != null) "\n$url" else ""
}
