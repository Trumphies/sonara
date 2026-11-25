package com.sonara.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Local storage for user-playlist cover images (v1.10.41).
 *
 * A picked gallery image is copied — never referenced — into
 * app-private storage: the photo-picker Uri grant is not guaranteed
 * to survive a restart, and a private copy keeps the cover working
 * offline forever. Covers are keyed by playlist id, so rename and
 * reorder keep them automatically. No cloud sync: covers stay on
 * this device (see [PlaylistCoverPolicy]).
 *
 * All file work here MUST run off the main thread (decode + JPEG
 * encode of a gallery photo). Every function is failure-tolerant:
 * a bad image returns false / is ignored, never crashes the UI.
 */

/** App-private directory holding one `<playlistId>.jpg` per cover. */
fun playlistCoverDir(context: Context): File =
    File(context.filesDir, PlaylistCoverPolicy.DIR_NAME)
        .also { if (!it.exists()) it.mkdirs() }

fun playlistCoverFile(context: Context, playlistId: String): File =
    File(playlistCoverDir(context), PlaylistCoverPolicy.fileNameFor(playlistId))

fun playlistCoverExists(context: Context, playlistId: String): Boolean =
    playlistCoverFile(context, playlistId).exists()

/** Delete one playlist's cover file (playlist deletion). Ignores errors. */
fun deletePlaylistCover(context: Context, playlistId: String) {
    try {
        playlistCoverFile(context, playlistId).delete()
    } catch (_: Exception) {
    }
}

/**
 * Delete cover files whose playlist no longer exists (e.g. removed
 * by a cloud sync pull). Files for live ids are never touched.
 */
fun prunePlaylistCovers(context: Context, liveIds: Collection<String>) {
    val keep = liveIds.mapTo(HashSet()) { PlaylistCoverPolicy.fileNameFor(it) }
    val dir = playlistCoverDir(context)
    dir.listFiles()?.forEach { file ->
        if (file.name !in keep && file.name.endsWith(".jpg")) {
            try {
                file.delete()
            } catch (_: Exception) {
            }
        }
    }
}

/**
 * Decode [source], downscale so the longest side is at most
 * [PlaylistCoverPolicy.MAX_DIMENSION_PX], bake in the EXIF
 * orientation, and write a JPEG to [dest] atomically (temp file +
 * rename). Returns false on any failure; [dest] is untouched then.
 */
private fun writeCoverImage(context: Context, source: Uri, dest: File): Boolean {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return false
        var sample = 1
        while (w / sample > PlaylistCoverPolicy.MAX_DIMENSION_PX ||
            h / sample > PlaylistCoverPolicy.MAX_DIMENSION_PX
        ) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return false
        val longest = maxOf(bitmap.width, bitmap.height)
        val scale = PlaylistCoverPolicy.MAX_DIMENSION_PX / longest.toFloat()
        if (scale < 1f) {
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            )
            if (scaled !== bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }
        // EXIF orientation (fresh stream: decoding consumed the earlier
        // ones). A sideways phone photo must be stored upright.
        val rotation = try {
            context.contentResolver.openInputStream(source)?.use { stream ->
                when (
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) {
            0f
        }
        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
            )
            if (rotated !== bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }
        val tmp = File(
            dest.parentFile,
            dest.name + ".tmp-" + UUID.randomUUID(),
        )
        try {
            FileOutputStream(tmp).use { out ->
                if (!bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        PlaylistCoverPolicy.JPEG_QUALITY,
                        out,
                    )
                ) {
                    return false
                }
            }
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            true
        } finally {
            if (tmp.exists()) tmp.delete()
            bitmap.recycle()
        }
    } catch (_: Exception) {
        false
    }
}

/**
 * Copy a picked image into a uniquely-named staging file in the
 * cache dir. The caller commits it with [commitCoverFile] or
 * discards it with [discardCoverFile]. Returns null on failure.
 */
fun stagePlaylistCover(context: Context, source: Uri): File? {
    val staging = File(
        context.cacheDir,
        PlaylistCoverPolicy.STAGING_PREFIX + UUID.randomUUID() +
            PlaylistCoverPolicy.STAGING_SUFFIX,
    )
    return if (writeCoverImage(context, source, staging)) {
        staging
    } else {
        staging.delete()
        null
    }
}

/**
 * Move a staged file into place as [playlistId]'s cover. The staged
 * file is consumed (renamed, or copied + deleted as a fallback).
 */
fun commitCoverFile(context: Context, playlistId: String, staged: File): Boolean {
    val dest = playlistCoverFile(context, playlistId)
    return try {
        if (staged.renameTo(dest)) {
            true
        } else {
            staged.copyTo(dest, overwrite = true)
            staged.delete()
            true
        }
    } catch (_: Exception) {
        false
    }
}

/** Delete a staging file that was never committed (dialog cancelled). */
fun discardCoverFile(staged: File?) {
    if (staged == null) return
    try {
        staged.delete()
    } catch (_: Exception) {
    }
}
