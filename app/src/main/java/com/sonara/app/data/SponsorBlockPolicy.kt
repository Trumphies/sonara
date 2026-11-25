package com.sonara.app.data

import org.json.JSONArray

/**
 * Segmen SponsorBlock — MURNI. Bentuk respons mengikuti API publik
 * SponsorBlock (sponsor.ajay.app/api/skipSegments): array objek
 * {category, segment:[startSec,endSec], UUID, ...}. HANYA kategori
 * yang mengubah pengalaman musik yang dipakai: sponsor, intro,
 * outro, selfpromo, music_offtopic (bagian non-musik), interaction
 * (pengingat interaksi). Kategori lain (chapter, poi_*, filler,
 * ...) diabaikan — bukan ditebak.
 *
 * Kejujuran data: API mengembalikan HTTP 404 "Not Found" bila video
 * tidak punya segmen di kategori yang diminta — itu jawaban sah
 * (daftar kosong), BUKAN error. Tidak ada segmen = tidak ada
 * perilaku skip sama sekali. Belum ada payload segmen sungguhan
 * yang berhasil diambil dari VM (semua videoId yang di-probe
 * mengembalikan 404) — perilaku skip dibuktikan harness dari bentuk
 * respons yang didokumentasikan, dan HP adalah pembuktian akhirnya.
 */
data class SponsorSegment(
    val startSec: Double,
    val endSec: Double,
    val category: String,
)

val SPONSORBLOCK_CATEGORIES: Set<String> = setOf(
    "sponsor", "intro", "outro", "selfpromo", "music_offtopic", "interaction",
)

/** Parse body JSON array API; entri rusak / kategori tak dikenal dilewati. */
fun parseSegments(body: String): List<SponsorSegment> {
    if (body.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(body)
        val out = ArrayList<SponsorSegment>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val category = o.optString("category", "")
            if (category !in SPONSORBLOCK_CATEGORIES) continue
            val seg = o.optJSONArray("segment") ?: continue
            if (seg.length() < 2) continue
            val start = seg.optDouble(0, Double.NaN)
            val end = seg.optDouble(1, Double.NaN)
            if (start.isNaN() || end.isNaN() || end <= start || start < 0) continue
            out.add(SponsorSegment(start, end, category))
        }
        out.sortedBy { it.startSec }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Keputusan skip pada satu posisi putar (detik): bila posisi berada
 * di dalam sebuah segmen → detik AKHIR segmen itu (lompat ke sana);
 * di luar semua segmen → null (tidak ada lompatan). Segmen yang
 * tumpang tindih diselesaikan ke ujung terjauh yang menutupi posisi
 * — tidak pernah lompat mundur.
 */
fun decideSkip(positionSec: Double, segments: List<SponsorSegment>): Double? {
    var target: Double? = null
    for (seg in segments) {
        if (positionSec >= seg.startSec && positionSec < seg.endSec) {
            if (target == null || seg.endSec > target) target = seg.endSec
        }
    }
    return target
}
