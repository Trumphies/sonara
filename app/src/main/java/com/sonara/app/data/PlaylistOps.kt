package com.sonara.app.data

/**
 * Operasi playlist MURNI (v1.10.21, tanpa Android — bisa diuji di JVM).
 * Dipakai UI kelola playlist; penyimpanan + sinkron tetap lewat
 * [LocalStore.savePlaylists] (diff-nya yang menulis tombstone).
 */

/** Masalah nama playlist; null = nama sah. */
enum class PlaylistNameProblem { BLANK, DUPLICATE }

/**
 * Validasi nama baru: kosong (setelah trim) ditolak; duplikat
 * (perbandingan case-insensitive setelah trim) dengan nama playlist
 * lain ditolak. [otherNames] = nama playlist LAIN (bukan yang
 * diganti namanya).
 */
fun validatePlaylistName(rawName: String, otherNames: List<String>): PlaylistNameProblem? {
    val name = rawName.trim()
    if (name.isEmpty()) return PlaylistNameProblem.BLANK
    if (otherNames.any { it.trim().equals(name, ignoreCase = true) }) {
        return PlaylistNameProblem.DUPLICATE
    }
    return null
}

/**
 * Pindah satu id dalam daftar trackId playlist (semantik sama
 * dengan [com.sonara.app.player.PlayerManager.moveQueue]:
 * hapus di [from], sisip di [to]). Indeks di luar jangkauan =
 * daftar tidak berubah.
 */
fun moveTrackIds(ids: List<String>, from: Int, to: Int): List<String> {
    if (from !in ids.indices || to !in ids.indices) return ids
    if (from == to) return ids
    val mutable = ids.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to, item)
    return mutable
}

/** Ganti nama satu playlist (identitas id dipertahankan). */
fun renamePlaylistIn(list: List<Playlist>, id: String, newName: String): List<Playlist> =
    list.map { if (it.id == id) it.copy(name = newName.trim()) else it }

/** Hapus satu playlist dari daftar (lagu/liked/riwayat tidak ikut). */
fun deletePlaylistFrom(list: List<Playlist>, id: String): List<Playlist> =
    list.filter { it.id != id }
