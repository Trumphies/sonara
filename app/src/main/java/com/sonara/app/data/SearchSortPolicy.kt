package com.sonara.app.data

/**
 * Urutan hasil pencarian — MURNI. "Best match" = urutan server apa
 * adanya (perilaku lama, tidak berubah). "Most played"/"Newest"
 * adalah urutan SISI KLIEN di atas data yang benar-benar dibawa
 * respons (hitungan/tahun hasil parse — lihat MediaTextParse):
 * respons YTM signed-out tidak membawa kontrol sort server.
 * Tanpa hitungan/tahun = urutan tidak berubah (stabil), bukan
 * ditebak ke depan.
 */
enum class SearchSort { BEST_MATCH, MOST_PLAYED, NEWEST }

/** Urutan stabil: yang tanpa data kunci tetap di urutan server, di belakang. */
fun sortSearchTracks(tracks: List<Track>, sort: SearchSort): List<Track> {
    return when (sort) {
        SearchSort.BEST_MATCH -> tracks
        SearchSort.MOST_PLAYED -> tracks.sortedWith(
            compareByDescending<Track> { it.playCount > 0 }
                .thenByDescending { it.playCount },
        )
        SearchSort.NEWEST -> tracks.sortedWith(
            compareByDescending<Track> { it.year > 0 }
                .thenByDescending { it.year },
        )
    }
}
