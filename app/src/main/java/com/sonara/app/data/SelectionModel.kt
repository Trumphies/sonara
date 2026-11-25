package com.sonara.app.data

/**
 * Model seleksi multi-lagu MURNI (v1.10.21, tanpa Android/Compose —
 * bisa diuji di JVM). Holder Compose-nya ([SelectionState] di
 * SelectionUi.kt) mendelegasikan ke fungsi-fungsi ini.
 *
 * Kunci seleksi SELALU ber-scope daftar ([selectionKey]) supaya
 * seleksi tidak pernah bocor antar-daftar: daftar yang berbeda
 * tidak bisa memilih kunci yang sama.
 */

/** Himpunan kunci terpilih (immutable; operasi mengembalikan salinan). */
data class Selection(val keys: Set<String> = emptySet()) {
    val count: Int get() = keys.size
    val isEmpty: Boolean get() = keys.isEmpty()

    /** Toggle satu kunci. */
    fun toggle(key: String): Selection =
        if (key in keys) Selection(keys - key) else Selection(keys + key)

    /** Tambah satu kunci (masuk mode + pilih baris itu). */
    fun with(key: String): Selection = Selection(keys + key)

    fun cleared(): Selection = Selection(emptySet())
}

/**
 * Kunci seleksi untuk satu baris: scope daftar + kunci kemunculan
 * (`id + "#" + nomor kemunculan`, stabil saat reorder — kemunculan
 * ke-k dari satu id tetap kemunculan ke-k walau posisi berubah).
 */
fun selectionKey(scope: String, occurrenceKey: String): String = "$scope#$occurrenceKey"

/**
 * Kunci kemunculan untuk daftar id (boleh berisi id yang sama dua
 * kali — playlist memang mengizinkannya): kemunculan pertama `id#0`,
 * kedua `id#1`, dst.
 */
fun occurrenceKeys(ids: List<String>): List<String> {
    val seen = HashMap<String, Int>()
    return ids.map { id ->
        val n = seen.getOrDefault(id, 0)
        seen[id] = n + 1
        "$id#$n"
    }
}

/**
 * Aksi bulk "Add to queue" hanya di luar sesi Jam — antrean sesi
 * milik sesi (tamu read-only, host pun tidak mengubahnya dari
 * layar antrean lokal). "Save to playlist" tidak terpengaruh.
 */
fun bulkQueueAllowed(jamActive: Boolean): Boolean = !jamActive
