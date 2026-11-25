package com.sonara.app.data

import android.content.Context
import android.net.Uri

/**
 * Scan gabungan file lokal (v1.0.7) — butuh Android, jadi hidup di
 * file sendiri supaya LocalFilesPolicy.kt tetap murni Kotlin (dan
 * tetap bisa diuji harness JVM tanpa android.jar).
 *
 * Sumber: MediaStore ([LocalMusic]) + folder SAF pilihan user
 * ([SafFolderScanner]). Override tag, pengelompokan duplikat, dan
 * penandaan berkas hilang mengikuti [LocalFilesPolicy]. WAJIB
 * dipanggil dari thread IO (query ContentResolver + baca durasi).
 */
object LocalFilesScan {

    /**
     * Mengembalikan (grup tampil, grup hilang) — grup hilang dihitung
     * SEBELUM filter [hideMissing], supaya jumlahnya jujur walau
     * barisnya disembunyikan. Pemeriksaan keberadaan yang gagal
     * (null) dianggap ADA, bukan hilang.
     */
    fun scanAll(
        context: Context,
        folders: List<LocalFolderEntry>,
        overrides: Map<String, LocalTagOverride>,
        preferredSources: Map<String, String>,
        hideMissing: Boolean,
    ): Pair<List<LocalTrackGroup>, List<LocalTrackGroup>> {
        val found = ArrayList<Track>()
        found += LocalMusic.scan(context)
        for (folder in folders) {
            val treeUri = runCatching { Uri.parse(folder.uriString) }.getOrNull()
                ?: continue
            found += SafFolderScanner.scanFolder(context, treeUri)
        }
        val withTags = LocalFilesPolicy.applyOverrides(found, overrides)
        val exists = HashMap<String, Boolean>()
        for (track in withTags) {
            if (track.id in exists) continue
            val uri = runCatching { Uri.parse(track.streamUrl) }.getOrNull()
            val result = if (uri == null) {
                null
            } else if (track.id.startsWith("saf:")) {
                SafFolderScanner.documentExists(context, uri)
            } else {
                SafFolderScanner.mediaStoreExists(context, uri)
            }
            exists[track.id] = result != false
        }
        val ordered = LinkedHashMap<String, MutableList<Track>>()
        for (track in withTags) {
            ordered.getOrPut(LocalFilesPolicy.groupKey(track)) { ArrayList() }.add(track)
        }
        val groups = ordered.entries.map { (key, copies) ->
            val wanted = preferredSources[key] ?: preferredSources[copies.first().id]
            val present = copies.filter { exists[it.id] == true }
            val effective = copies.firstOrNull { it.id == wanted && exists[it.id] == true }
                ?: present.firstOrNull()
                ?: copies.firstOrNull { it.id == wanted }
                ?: copies.first()
            LocalTrackGroup(
                key = key,
                copies = copies.toList(),
                effective = effective,
                missing = present.isEmpty(),
            )
        }
        val missing = groups.filter { it.missing }
        val shown = if (hideMissing) groups.filter { !it.missing } else groups
        return shown to missing
    }
}
