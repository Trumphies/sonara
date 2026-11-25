package com.sonara.app.data

/**
 * Pure policy for user-playlist cover images (v1.10.41).
 *
 * Deliberately free of Android imports: harness141 compiles this file
 * standalone. Covers are LOCAL ONLY — app-private files keyed by
 * playlist id. The [Playlist] model and Firestore sync never see
 * them (sync records carry id/name/trackIds/createdAt only), so a
 * cover never syncs to another device and can never break a sync
 * merge. Rename/reorder keep the cover (id unchanged); deleting the
 * playlist deletes its cover file.
 */
object PlaylistCoverPolicy {
    /** Longest side of a stored cover, px — gallery photos are downscaled. */
    const val MAX_DIMENSION_PX = 1024
    const val JPEG_QUALITY = 85
    const val DIR_NAME = "playlist_covers"
    const val STAGING_PREFIX = "playlist_cover_staging_"
    const val STAGING_SUFFIX = ".jpg"

    /**
     * File-safe id: letters/digits/'-'/'_' only, max 64 chars.
     * Anything else (path separators, "..", punctuation) is dropped,
     * so a hostile id can never escape the covers directory.
     */
    fun sanitizeId(id: String): String {
        val clean = id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(64)
        return clean.ifEmpty { "cover" }
    }

    fun fileNameFor(playlistId: String): String = sanitizeId(playlistId) + ".jpg"
}
