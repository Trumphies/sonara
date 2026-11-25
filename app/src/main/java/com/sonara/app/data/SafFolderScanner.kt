package com.sonara.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore

/**
 * Pemindai folder SAF v1.0.7 — API framework saja, tanpa dependensi
 * baru. Folder dipilih user lewat pemilih dokumen sistem
 * (OpenDocumentTree); izin persistable diambil saat memilih.
 *
 * - Hanya file audio (MIME audio / ekstensi audio, lihat
 *   LocalFilesPolicy); subfolder dijelajahi sampai kedalaman 6.
 * - Durasi dibaca lewat MediaMetadataRetriever dari URI dokumen;
 *   gagal baca = 0 (placeholder jujur "--:--", bukan tebakan).
 * - Cap 2.000 file per pemindaian (LocalFilesPolicy.MAX_SAF_FILES)
 *   supaya folder raksasa tidak menggantung UI.
 * - ID track = "saf:" + URI dokumen (stabil antar restart selama
 *   izin persistable masih berlaku).
 */
object SafFolderScanner {
    private const val MAX_DEPTH = 6

    private val PROJECTION = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )

    /** Pindai satu tree URI; kosong bila izin hilang / tree tak terbaca. */
    fun scanFolder(context: Context, treeUri: Uri): List<Track> {
        val out = ArrayList<Track>()
        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            scanChildren(context, treeUri, rootUri, depth = 0, out)
        } catch (_: Exception) {
            // Izin dicabut / tree tak terbaca: kembalikan apa adanya
            // (bisa kosong) — pemanggil menggabungkan dengan hasil
            // MediaStore, bukan crash.
        }
        return out
    }

    private fun scanChildren(
        context: Context,
        treeUri: Uri,
        parentUri: Uri,
        depth: Int,
        out: MutableList<Track>,
    ) {
        if (depth > MAX_DEPTH || out.size >= LocalFilesPolicy.MAX_SAF_FILES) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getDocumentId(parentUri),
        )
        try {
            context.contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    if (out.size >= LocalFilesPolicy.MAX_SAF_FILES) return
                    val docId = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = cursor.getString(mimeCol) ?: ""
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    when {
                        LocalFilesPolicy.isDirectory(name, mime) -> {
                            scanChildren(context, treeUri, docUri, depth + 1, out)
                        }
                        LocalFilesPolicy.isAudioFile(name, mime) -> {
                            out.add(
                                Track(
                                    id = "saf:$docUri",
                                    title = name.substringBeforeLast('.'),
                                    artist = "Unknown artist",
                                    album = "",
                                    source = Source.LOCAL,
                                    engine = Engine.EXOPLAYER,
                                    durationMs = readDuration(context, docUri),
                                    streamUrl = docUri.toString(),
                                ),
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Subtree tak terbaca: lewati, lanjutkan sisanya.
        }
    }

    private fun readDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Apakah file MediaStore masih ada. True = provider mengembalikan
     * baris; false = provider melaporkan tidak ada; null = pemeriksaan
     * gagal (dianggap "tidak diketahui", BUKAN hilang).
     */
    fun mediaStoreExists(context: Context, contentUri: Uri): Boolean? {
        return try {
            context.contentResolver.query(
                contentUri, arrayOf(MediaStore.Audio.Media._ID), null, null, null,
            )?.use { cursor -> cursor.moveToFirst() }
        } catch (_: Exception) {
            null
        }
    }

    /** Apakah dokumen SAF masih ada (null = pemeriksaan gagal). */
    fun documentExists(context: Context, docUri: Uri): Boolean? {
        return try {
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null, null, null,
            )?.use { cursor -> cursor.moveToFirst() }
        } catch (_: Exception) {
            null
        }
    }
}
