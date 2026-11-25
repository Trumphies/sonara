package com.sonara.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Organisasi Koleksi lokal (v1.0.6): pin, folder, tag, dan sakelar
 * auto-download per playlist. Logika MURNI (tanpa Android).
 *
 * Disimpan TERPISAH dari model [Playlist] yang disinkron ke cloud
 * (pola yang sama dengan cover playlist v1.10.41): menambah field
 * ke Playlist akan mengubah kesetaraan diff sinkron dan memicu
 * re-push seluruh playlist. Semua state di sini lokal saja, tidak
 * pernah disinkron.
 */

/** Satu folder playlist buatan user. */
data class PlaylistFolder(
    val id: String,
    val name: String,
)

/** Seluruh state organisasi Koleksi (satu dokumen JSON di prefs). */
data class LibraryOrg(
    /** Id playlist yang di-pin, urutan = urutan mem-pin. */
    val pinnedIds: List<String> = emptyList(),
    /** Folder, urutan = urutan pembuatan. */
    val folders: List<PlaylistFolder> = emptyList(),
    /** playlistId -> folderId. Id yang foldernya tidak ada diabaikan. */
    val playlistFolder: Map<String, String> = emptyMap(),
    /** playlistId -> tag (sudah dinormalisasi). */
    val tags: Map<String, Set<String>> = emptyMap(),
    /** Id playlist yang auto-download-nya menyala. */
    val autoDownloadIds: Set<String> = emptySet(),
)

object LibraryOrgPolicy {
    const val MAX_TAG_LENGTH = 24
    const val MAX_TAGS_PER_PLAYLIST = 8

    /** Urutan tampil: yang di-pin dulu (urutan pin), sisanya urutan simpan. */
    fun applyOrgToPlaylists(playlists: List<Playlist>, org: LibraryOrg): List<Playlist> {
        val byId = playlists.associateBy { it.id }
        val pinned = org.pinnedIds.mapNotNull { byId[it] }
        val pinnedSet = pinned.map { it.id }.toSet()
        return pinned + playlists.filter { it.id !in pinnedSet }
    }

    /** Kelompok tampil: pin, lalu tiap folder (urutan buat), lalu tanpa folder. */
    data class PlaylistGroups(
        val pinned: List<Playlist>,
        val folders: List<Pair<PlaylistFolder, List<Playlist>>>,
        val unfiled: List<Playlist>,
    )

    fun groupPlaylists(playlists: List<Playlist>, org: LibraryOrg): PlaylistGroups {
        val ordered = applyOrgToPlaylists(playlists, org)
        val pinnedSet = org.pinnedIds.toSet()
        val liveFolderIds = org.folders.map { it.id }.toSet()
        val pinned = ordered.filter { it.id in pinnedSet }
        val rest = ordered.filter { it.id !in pinnedSet }
        val folderGroups = org.folders.map { folder ->
            folder to rest.filter { org.playlistFolder[it.id] == folder.id }
        }
        val groupedIds = folderGroups.flatMap { it.second }.map { it.id }.toSet()
        val unfiled = rest.filter { it.id !in groupedIds }
        return PlaylistGroups(pinned, folderGroups, unfiled)
    }

    /** Pin/unpin satu playlist; urutan pin = urutan mem-pin. */
    fun togglePin(org: LibraryOrg, playlistId: String): LibraryOrg {
        val pinned = org.pinnedIds.toMutableList()
        if (!pinned.remove(playlistId)) pinned.add(playlistId)
        return org.copy(pinnedIds = pinned)
    }

    fun isPinned(org: LibraryOrg, playlistId: String): Boolean =
        playlistId in org.pinnedIds

    /** Buat folder; null bila nama kosong (setelah trim). */
    fun createFolder(org: LibraryOrg, rawName: String): LibraryOrg? {
        val name = rawName.trim()
        if (name.isEmpty()) return null
        val folder = PlaylistFolder(
            id = java.util.UUID.randomUUID().toString(),
            name = name.take(40),
        )
        return org.copy(folders = org.folders + folder)
    }

    /** Ganti nama folder; null bila folder tidak ada atau nama kosong. */
    fun renameFolder(org: LibraryOrg, folderId: String, rawName: String): LibraryOrg? {
        val name = rawName.trim()
        if (name.isEmpty()) return null
        if (org.folders.none { it.id == folderId }) return null
        return org.copy(
            folders = org.folders.map {
                if (it.id == folderId) it.copy(name = name.take(40)) else it
            },
        )
    }

    /** Hapus folder; playlist di dalamnya TIDAK ikut terhapus (jadi tanpa folder). */
    fun deleteFolder(org: LibraryOrg, folderId: String): LibraryOrg {
        return org.copy(
            folders = org.folders.filter { it.id != folderId },
            playlistFolder = org.playlistFolder.filterValues { it != folderId },
        )
    }

    /**
     * Pindahkan playlist ke folder ([folderId] null = keluarkan dari
     * folder). Folder yang tidak dikenal = tidak berubah.
     */
    fun movePlaylistToFolder(
        org: LibraryOrg,
        playlistId: String,
        folderId: String?,
    ): LibraryOrg {
        if (folderId != null && org.folders.none { it.id == folderId }) return org
        val updated = org.playlistFolder.toMutableMap()
        if (folderId == null) updated.remove(playlistId) else updated[playlistId] = folderId
        return org.copy(playlistFolder = updated)
    }

    /** Folder tempat playlist berada, atau null (tanpa folder / folder tidak ada). */
    fun folderOf(org: LibraryOrg, playlistId: String): String? {
        val folderId = org.playlistFolder[playlistId] ?: return null
        return folderId.takeIf { id -> org.folders.any { it.id == id } }
    }

    /** Jumlah playlist per folder (hanya playlist yang masih ada). */
    fun folderCounts(playlists: List<Playlist>, org: LibraryOrg): Map<String, Int> {
        val liveIds = playlists.map { it.id }.toSet()
        val counts = HashMap<String, Int>()
        for ((playlistId, folderId) in org.playlistFolder) {
            if (playlistId in liveIds && org.folders.any { it.id == folderId }) {
                counts[folderId] = (counts[folderId] ?: 0) + 1
            }
        }
        return counts
    }

    /** Normalisasi tag: trim + lowercase; null bila kosong / terlalu panjang. */
    fun normalizeTag(raw: String): String? {
        val tag = raw.trim().lowercase()
        if (tag.isEmpty() || tag.length > MAX_TAG_LENGTH) return null
        return tag
    }

    /** Tambah/hapus satu tag pada playlist (maks [MAX_TAGS_PER_PLAYLIST]). */
    fun toggleTag(org: LibraryOrg, playlistId: String, rawTag: String): LibraryOrg {
        val tag = normalizeTag(rawTag) ?: return org
        val current = org.tags[playlistId].orEmpty().toMutableSet()
        if (!current.remove(tag) && current.size < MAX_TAGS_PER_PLAYLIST) current.add(tag)
        val updated = org.tags.toMutableMap()
        if (current.isEmpty()) updated.remove(playlistId) else updated[playlistId] = current
        return org.copy(tags = updated)
    }

    /** Semua tag yang dipakai, terurut. */
    fun allTagsUsed(org: LibraryOrg): List<String> =
        org.tags.values.flatten().toSortedSet().toList()

    /** Saring playlist: harus punya SEMUA tag terpilih (AND); tanpa filter = semua. */
    fun filterPlaylistsByTags(
        playlists: List<Playlist>,
        org: LibraryOrg,
        selected: Set<String>,
    ): List<Playlist> {
        if (selected.isEmpty()) return playlists
        return playlists.filter { pl ->
            val tags = org.tags[pl.id].orEmpty()
            selected.all { it in tags }
        }
    }

    /** Sakelar auto-download satu playlist. */
    fun toggleAutoDownload(org: LibraryOrg, playlistId: String): LibraryOrg {
        val ids = org.autoDownloadIds.toMutableSet()
        if (!ids.remove(playlistId)) ids.add(playlistId)
        return org.copy(autoDownloadIds = ids)
    }

    fun isAutoDownload(org: LibraryOrg, playlistId: String): Boolean =
        playlistId in org.autoDownloadIds

    /** Saring entri tampilan detail playlist dari query cari. */
    fun filterPlaylistEntries(entries: List<Track>, query: String): List<Track> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return entries
        return entries.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
        }
    }

    /**
     * Urutkan entri tampilan detail playlist. DATE_ADDED = urutan
     * simpan trackIds (identitas). PLAYS memakai hitungan putar
     * (tanpa hitungan = 0); seri (tie) mempertahankan urutan simpan.
     */
    fun sortPlaylistEntries(
        entries: List<Track>,
        sort: PlaylistDetailSort,
        playCounts: Map<String, Int>,
    ): List<Track> {
        return when (sort) {
            PlaylistDetailSort.DATE_ADDED -> entries
            PlaylistDetailSort.TITLE ->
                entries.sortedBy { it.title.lowercase() }
            PlaylistDetailSort.ARTIST ->
                entries.sortedBy { it.artist.lowercase() }
            PlaylistDetailSort.PLAYS ->
                entries.withIndex().sortedWith(
                    compareByDescending<IndexedValue<Track>> { playCounts[it.value.id] ?: 0 }
                        .thenBy { it.index },
                ).map { it.value }
        }
    }

    // ---- serialisasi JSON (org.json; LocalStore hanya menyimpan string) ----

    fun orgToJson(org: LibraryOrg): JSONObject {
        val o = JSONObject()
        o.put("pinned", JSONArray(org.pinnedIds))
        val foldersArr = JSONArray()
        for (f in org.folders) {
            foldersArr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
            })
        }
        o.put("folders", foldersArr)
        val pf = JSONObject()
        for ((k, v) in org.playlistFolder) pf.put(k, v)
        o.put("playlistFolder", pf)
        val tagsObj = JSONObject()
        for ((k, v) in org.tags) tagsObj.put(k, JSONArray(v.toList()))
        o.put("tags", tagsObj)
        o.put("autoDownload", JSONArray(org.autoDownloadIds.toList()))
        return o
    }

    fun orgFromJson(obj: JSONObject): LibraryOrg {
        val pinned = mutableListOf<String>()
        val pinnedArr = obj.optJSONArray("pinned")
        if (pinnedArr != null) {
            for (i in 0 until pinnedArr.length()) {
                pinnedArr.optString(i).takeIf { it.isNotBlank() }?.let { pinned += it }
            }
        }
        val folders = mutableListOf<PlaylistFolder>()
        val foldersArr = obj.optJSONArray("folders")
        if (foldersArr != null) {
            for (i in 0 until foldersArr.length()) {
                val f = foldersArr.optJSONObject(i) ?: continue
                val id = f.optString("id")
                val name = f.optString("name")
                if (id.isNotBlank() && name.isNotBlank()) folders += PlaylistFolder(id, name)
            }
        }
        val playlistFolder = HashMap<String, String>()
        val pf = obj.optJSONObject("playlistFolder")
        if (pf != null) {
            val keys = pf.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = pf.optString(k)
                if (k.isNotBlank() && v.isNotBlank()) playlistFolder[k] = v
            }
        }
        val tags = HashMap<String, Set<String>>()
        val tagsObj = obj.optJSONObject("tags")
        if (tagsObj != null) {
            val keys = tagsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = tagsObj.optJSONArray(k) ?: continue
                val set = HashSet<String>()
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { set += it }
                }
                if (set.isNotEmpty()) tags[k] = set
            }
        }
        val auto = HashSet<String>()
        val autoArr = obj.optJSONArray("autoDownload")
        if (autoArr != null) {
            for (i in 0 until autoArr.length()) {
                autoArr.optString(i).takeIf { it.isNotBlank() }?.let { auto += it }
            }
        }
        return LibraryOrg(pinned, folders, playlistFolder, tags, auto)
    }

    fun orgToJsonString(org: LibraryOrg): String = orgToJson(org).toString()

    /** String null/kosong/rusak = org default, tanpa crash. */
    fun orgFromJsonString(raw: String?): LibraryOrg {
        if (raw.isNullOrBlank()) return LibraryOrg()
        return try {
            orgFromJson(JSONObject(raw))
        } catch (_: Exception) {
            LibraryOrg()
        }
    }

    /** Bantu fixture: round-trip lewat string JSON. */
    fun orgRoundTrip(org: LibraryOrg): LibraryOrg =
        orgFromJsonString(orgToJsonString(org))
}

/** Urutan tampilan detail playlist (v1.0.6). */
enum class PlaylistDetailSort { DATE_ADDED, TITLE, ARTIST, PLAYS }
