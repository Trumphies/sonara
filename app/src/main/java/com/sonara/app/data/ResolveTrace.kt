package com.sonara.app.data

/**
 * Jejak resolve stream untuk sheet "Track info" — MURNI (data saja).
 * Setiap langkah rantai resolve dicatat apa adanya: mesin Go dulu,
 * lalu tiap client InnerTube (VISIONOS → WEB → ANDROID → TVHTML5)
 * dengan hasil masing-masing, lalu fallback embed bila dipakai.
 * Tidak ada diagnostik karangan: langkah yang tidak dijalankan
 * tidak dicatat, dan pesan gagal memakai pesan asli dari jalurnya.
 */
data class ResolveAttempt(
    /** Nama langkah, mis. "Go engine", "VISIONOS", "YouTube player (fallback)". */
    val step: String,
    /** Hasil langkah, mis. "OK — Opus 251" atau pesan gagal aslinya. */
    val detail: String,
)

data class ResolveTrace(val attempts: List<ResolveAttempt>) {
    /** Baris tampil "LANGKAH — hasil", urutan = urutan percobaan. */
    fun toDisplayLines(): List<String> = attempts.map { "${it.step} — ${it.detail}" }

    companion object {
        val EMPTY = ResolveTrace(emptyList())
    }
}
