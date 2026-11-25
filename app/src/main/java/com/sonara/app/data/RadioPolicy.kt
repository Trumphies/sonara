package com.sonara.app.data

/**
 * Aturan antrean radio — MURNI. Radio adalah daftar TERPISAH dari
 * antrean user: lagu user SELALU menang (advance user dulu, radio
 * hanya bila antrean user habis), dan radio tidak pernah
 * menyisipkan lagu yang sudah tidak disukai / duplikat / seed-nya
 * sendiri. Isi ulang (refill) dipicu saat sisa tinggal sedikit,
 * dari lagu radio terakhir — bukan dari seed awal yang sama
 * (supaya radio terus mengalir, bukan mengulang).
 */

/** Maksimum lagu radio yang disimpan per pengambilan (respons /next membawa ~25). */
const val RADIO_FETCH_CAP = 25

/** Sisa lagu radio yang memicu pengambilan berikutnya. */
const val RADIO_REFILL_THRESHOLD = 3

/**
 * Saring hasil radio mentah: buang seed, lagu yang tidak disukai,
 * dan duplikat (videoId sama muncul sekali — kemunculan pertama
 * menang). Urutan server dipertahankan; hasil dibatasi cap.
 */
fun filterRadioItems(
    items: List<RadioItem>,
    seedVideoId: String,
    dislikedTrackIds: Set<String>,
): List<RadioItem> {
    val seen = HashSet<String>()
    val out = ArrayList<RadioItem>()
    for (item in items) {
        if (item.videoId == seedVideoId) continue
        if (dislikedTrackIds.contains("ytm:${item.videoId}")) continue
        if (!seen.add(item.videoId)) continue
        out.add(item)
        if (out.size >= RADIO_FETCH_CAP) break
    }
    return out
}

/** True bila pengambilan radio berikutnya harus dimulai (sisa sedikit + belum berjalan). */
fun needsRadioRefill(remaining: Int, refillInFlight: Boolean): Boolean =
    !refillInFlight && remaining <= RADIO_REFILL_THRESHOLD
