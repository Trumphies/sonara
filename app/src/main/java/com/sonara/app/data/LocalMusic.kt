package com.sonara.app.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

/**
 * Scan MP3 / audio lokal dari MediaStore perangkat.
 *
 * Hanya file musik (IS_MUSIC = 1, durasi > 30 detik). Hasilnya Track
 * sumber LOKAL dengan streamUrl berupa content:// URI — ExoPlayer
 * bisa memutarnya langsung. Izin READ_MEDIA_AUDIO (API 33+) atau
 * READ_EXTERNAL_STORAGE (API ≤ 32) diminta di MainActivity; kalau
 * ditolak, fungsi ini mengembalikan list kosong (bukan crash).
 */
object LocalMusic {
    fun scan(context: Context): List<Track> {
        val out = ArrayList<Track>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} > 30000"
        } else {
            "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} > 30000"
        }
        return try {
            context.contentResolver.query(
                uri, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    out.add(
                        Track(
                            id = "local:$id",
                            title = cursor.getString(titleCol) ?: "Untitled",
                            artist = cursor.getString(artistCol) ?: "Unknown artist",
                            album = cursor.getString(albumCol) ?: "",
                            source = Source.LOCAL,
                            engine = Engine.EXOPLAYER,
                            durationMs = cursor.getLong(durCol),
                            streamUrl = contentUri.toString(),
                        ),
                    )
                }
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }
}
