package com.sonara.app.data

import org.json.JSONObject

/**
 * Kebijakan file lokal v1.0.7 — murni Kotlin, tanpa Android.
 *
 * - Tag override: judul/artis/album TAMPILAN per file, disimpan
 *   lokal saja; file aslinya TIDAK PERNAH ditulis ulang.
 * - Duplikat: salinan yang sama (judul + artis + durasi, setelah
 *   normalisasi) digabung jadi satu entri; sumber yang dipakai
 *   bisa dipilih per grup.
 * - Hilang: file yang provider-nya laporkan tidak ada ditandai
 *   (bukan dihapus diam-diam); pemeriksaan yang gagal (exception)
 *   dianggap "tidak diketahui", bukan hilang.
 * - Folder SAF: daftar URI tree yang dipilih user; klasifikasi
 *   audio memakai MIME + ekstensi (tanpa menebak isi file).
 */
data class LocalTagOverride(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
) {
    fun isEmpty(): Boolean = title.isBlank() && artist.isBlank() && album.isBlank()
}

/** Satu folder musik pilihan user (SAF tree URI + label tampil). */
data class LocalFolderEntry(
    val uriString: String,
    val displayName: String,
)

/** Satu grup tampil seksi musik lokal: salinan + efektif + hilang. */
data class LocalTrackGroup(
    /** Kunci grup (judul + artis + durasi ternormalisasi). */
    val key: String,
    val copies: List<Track>,
    /** Salinan yang diputar saat baris diketuk. */
    val effective: Track,
    /** True bila TIDAK ADA salinan yang filenya masih ada. */
    val missing: Boolean,
)
data class LocalGroup(
    val copies: List<Track>,
    val effectiveTrack: Track,
)

/** Entri tampil seksi musik lokal. */
data class LocalEntry(
    val track: Track,
    val copies: List<Track>,
    val missing: Boolean,
)

object LocalFilesPolicy {
    const val MAX_SAF_FILES = 2000
    const val MAX_FOLDERS = 8

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "ogg", "oga", "opus", "m4a", "aac", "wav", "wma", "aiff", "aif",
    )

    const val DIRECTORY_MIME = "vnd.android.document/directory"

    /** Terapkan override tampil; field kosong = pakai nilai asli. */
    fun applyOverrides(
        tracks: List<Track>,
        overrides: Map<String, LocalTagOverride>,
    ): List<Track> = tracks.map { track ->
        val ov = overrides[track.id] ?: return@map track
        track.copy(
            title = ov.title.ifBlank { track.title },
            artist = ov.artist.ifBlank { track.artist },
            album = ov.album.ifBlank { track.album },
        )
    }

    fun overridesToJson(overrides: Map<String, LocalTagOverride>): String {
        val root = JSONObject()
        for ((id, ov) in overrides) {
            if (ov.isEmpty()) continue
            val obj = JSONObject()
            obj.put("t", ov.title)
            obj.put("a", ov.artist)
            obj.put("al", ov.album)
            root.put(id, obj)
        }
        return root.toString()
    }

    fun overridesFromJson(raw: String?): Map<String, LocalTagOverride> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = LinkedHashMap<String, LocalTagOverride>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val obj = root.optJSONObject(id) ?: continue
                val ov = LocalTagOverride(
                    title = obj.optString("t", ""),
                    artist = obj.optString("a", ""),
                    album = obj.optString("al", ""),
                )
                if (!ov.isEmpty()) out[id] = ov
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Kunci grup duplikat: judul + artis ternormalisasi + durasi persis. */
    fun groupKey(track: Track): String {
        fun norm(s: String): String = s.lowercase().trim().split(Regex("\\s+")).joinToString(" ")
        return norm(track.title) + "\u0001" + norm(track.artist) + "\u0001" + track.durationMs.toString()
    }

    /**
     * Gabungkan duplikat. [preferred] memetakan kunci grup -> id track
     * yang dipilih user (bentuk yang disimpan UI); sebagai toleransi,
     * id salinan pertama grup juga diterima sebagai kunci (bentuk
     * yang dipakai harness/fixtures lama). Tanpa pilihan, salinan
     * pertama yang menang. Urutan grup = urutan kemunculan pertama.
     */
    fun groupDuplicates(
        tracks: List<Track>,
        preferred: Map<String, String> = emptyMap(),
    ): List<LocalGroup> {
        val ordered = LinkedHashMap<String, MutableList<Track>>()
        for (track in tracks) {
            ordered.getOrPut(groupKey(track)) { ArrayList() }.add(track)
        }
        return ordered.values.map { copies ->
            val key = groupKey(copies.first())
            val wanted = preferred[key] ?: preferred[copies.first().id]
            val effective = copies.firstOrNull { it.id == wanted } ?: copies.first()
            LocalGroup(copies = copies.toList(), effectiveTrack = effective)
        }
    }

    /**
     * Tandai tiap track apakah filenya hilang, satu entri per track
     * masukan (urut sama). [copies] = salinan segrupnya; [missing] =
     * id track ada di [missingIds]. Pengelompokan sendiri ditangani
     * [groupDuplicates]/[scanAll] — fungsi ini hanya menandai.
     */
    fun markMissing(
        tracks: List<Track>,
        missingIds: Set<String>,
    ): List<LocalEntry> {
        val groups = groupDuplicates(tracks)
        val copiesById = HashMap<String, List<Track>>()
        for (group in groups) {
            for (copy in group.copies) copiesById[copy.id] = group.copies
        }
        return tracks.map { track ->
            LocalEntry(
                track = track,
                copies = copiesById[track.id].orEmpty(),
                missing = track.id in missingIds,
            )
        }
    }

    /** True bila dokumen ini file audio (MIME audio ATAU ekstensi audio). */
    fun isAudioFile(displayName: String, mimeType: String): Boolean {
        if (mimeType.startsWith("audio/")) return true
        val ext = displayName.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }

    fun isDirectory(displayName: String, mimeType: String): Boolean =
        mimeType == DIRECTORY_MIME

    /** Bersihkan daftar URI folder: buang kosong, dedupe, cap 8. */
    fun normalizeFolderUris(uris: List<String>): List<String> {
        val seen = LinkedHashSet<String>()
        for (uri in uris) {
            val trimmed = uri.trim()
            if (trimmed.isNotEmpty()) seen.add(trimmed)
            if (seen.size >= MAX_FOLDERS) break
        }
        return seen.toList()
    }

    /**
     * Nama tampil folder dari URI tree SAF, tanpa query ContentResolver:
     * segmen terakhir document id setelah ':' lalu setelah '/'
     * ("primary:Music" → "Music"). Bukan nama file — hanya label chip.
     */
    fun folderDisplayName(uriString: String): String {
        val decoded = runCatching {
            java.net.URLDecoder.decode(uriString, "UTF-8")
        }.getOrDefault(uriString)
        val docId = decoded.substringAfter("/tree/", "")
        val name = docId.substringAfterLast(':').substringAfterLast('/')
        return name.takeIf { it.isNotBlank() } ?: "Folder"
    }

    /**
     * Label satu salinan untuk dialog pilih salinan: SAF → nama berkas
     * dari document id; MediaStore → "Media library". Tidak pernah
     * menampilkan URI mentah ke user.
     */
    fun copyLabel(track: Track): String {
        if (track.id.startsWith("saf:")) {
            val decoded = runCatching {
                java.net.URLDecoder.decode(track.id.removePrefix("saf:"), "UTF-8")
            }.getOrDefault(track.id)
            val docId = decoded.substringAfter("/document/", "")
            val name = docId.substringAfterLast('/').substringAfterLast(':')
            return if (name.isNotBlank()) "Folder file: $name" else "Folder file"
        }
        return "Media library"
    }

    fun preferredSourcesToJson(preferred: Map<String, String>): String {
        val root = JSONObject()
        for ((k, v) in preferred) root.put(k, v)
        return root.toString()
    }

    fun preferredSourcesFromJson(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val root = JSONObject(raw)
            val out = LinkedHashMap<String, String>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = root.optString(k, "")
                if (v.isNotEmpty()) out[k] = v
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
