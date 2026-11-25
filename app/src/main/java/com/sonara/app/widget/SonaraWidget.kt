package com.sonara.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import coil.Coil
import coil.request.ImageRequest
import com.sonara.app.MainActivity
import com.sonara.app.SonaraApplication
import com.sonara.app.R
import com.sonara.app.data.Track
import com.sonara.app.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Widget home screen (v1.10.17) — remote control pemutaran app, BUKAN
 * pemutar sendiri. Dua ukuran (2×1 dan 4×1) memakai satu provider
 * class ini dengan metadata berbeda di manifest.
 *
 * State dibaca dari PlayerManager lewat StateFlow yang SAMA dengan
 * yang dipakai notifikasi media (currentTrack + isPlaying) — tidak
 * ada service/DB baru. Update di-push dari dua tempat:
 * - PlaybackService: tiap ganti lagu / toggle play-pause (jalur yang
 *   sama dengan refresh notifikasi).
 * - AppWidgetProvider.onUpdate: saat widget ditambahkan / sistem
 *   meminta (proses app mungkin mati → empty state jujur).
 *
 * Tombol bekerja tanpa membuka app: PendingIntent langsung ke
 * PlaybackService dengan ACTION_* yang sama dengan tombol
 * notifikasi. Tap bagian lain widget → buka MainActivity.
 * Tidak ada permission baru. Tampilan di launcher Infinix XOS
 * (grid + mask) di luar kendali app — hanya bisa dibuktikan dari HP.
 */
class SonaraWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        SonaraWidget.requestRefresh(context.applicationContext)
    }
}

object SonaraWidget {
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Artwork terakhir yang berhasil dimuat (dipakai ulang antar-push). */
    @Volatile
    private var lastArtwork: Bitmap? = null

    @Volatile
    private var lastArtworkUrl: String? = null

    /** Minta refresh dari konteks sinkron (provider). Fire-and-forget. */
    fun requestRefresh(context: Context) {
        widgetScope.launch {
            runCatching { refresh(context.applicationContext) }
        }
    }

    /**
     * Baca state player + muat artwork (cache Coil yang sama dengan
     * UI/notifikasi), lalu push RemoteViews ke SEMUA instance widget
     * (kedua ukuran). Dipanggil PlaybackService tiap status putar
     * berubah — suspend supaya pemuatan artwork tidak memblokir
     * thread pemanggil.
     */
    suspend fun refresh(context: Context) {
        val app = context.applicationContext as? SonaraApplication
        val player = runCatching { app?.player }.getOrNull()
        val track = player?.currentTrack?.value
        val playing = player?.isPlaying?.value ?: false
        val artwork = artworkFor(context, track)
        pushAll(context, track, playing, artwork)
    }

    private suspend fun artworkFor(context: Context, track: Track?): Bitmap? {
        val url = track?.imageUrl
        if (url.isNullOrBlank()) {
            lastArtwork = null
            lastArtworkUrl = null
            return null
        }
        if (url == lastArtworkUrl && lastArtwork != null) return lastArtwork
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(256, 256)
                    .build()
                val result = Coil.imageLoader(context).execute(request)
                (result.drawable as? BitmapDrawable)?.bitmap
            }.getOrNull()
        }
        if (bitmap != null) {
            lastArtwork = bitmap
            lastArtworkUrl = url
        }
        return bitmap
    }

    private fun pushAll(context: Context, track: Track?, playing: Boolean, artwork: Bitmap?) {
        val manager = AppWidgetManager.getInstance(context)
        val component = android.content.ComponentName(context, SonaraWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        for (id in ids) {
            val layout = manager.getAppWidgetInfo(id)?.initialLayout
                ?: R.layout.widget_sonara_small
            val views = buildViews(context, layout, track, playing, artwork)
            runCatching { manager.updateAppWidget(id, views) }
        }
    }

    private fun buildViews(
        context: Context,
        layoutRes: Int,
        track: Track?,
        playing: Boolean,
        artwork: Bitmap?,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutRes)
        // Tap bagian mana pun yang bukan tombol → buka app.
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        if (track == null) {
            views.setViewVisibility(R.id.widget_content, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_logo_ripples)
            views.setViewVisibility(R.id.widget_play_pause, View.GONE)
            views.setViewVisibility(R.id.widget_next, View.GONE)
            if (hasPrevButton(layoutRes)) {
                views.setViewVisibility(R.id.widget_prev, View.GONE)
            }
            return views
        }
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, View.GONE)
        views.setViewVisibility(R.id.widget_play_pause, View.VISIBLE)
        views.setViewVisibility(R.id.widget_next, View.VISIBLE)
        if (hasPrevButton(layoutRes)) {
            views.setViewVisibility(R.id.widget_prev, View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.widget_prev,
                serviceIntent(context, PlaybackService.ACTION_PREV, 1003),
            )
        }
        views.setTextViewText(R.id.widget_title, track.title)
        views.setTextViewText(R.id.widget_artist, track.artist)
        if (artwork != null) {
            views.setImageViewBitmap(R.id.widget_artwork, artwork)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_logo_ripples)
        }
        views.setImageViewResource(
            R.id.widget_play_pause,
            if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
        )
        views.setOnClickPendingIntent(
            R.id.widget_play_pause,
            serviceIntent(context, PlaybackService.ACTION_TOGGLE, 1001),
        )
        views.setOnClickPendingIntent(
            R.id.widget_next,
            serviceIntent(context, PlaybackService.ACTION_NEXT, 1002),
        )
        return views
    }

    private fun hasPrevButton(layoutRes: Int): Boolean =
        layoutRes == R.layout.widget_sonara_wide

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, 1000, intent, pendingFlags())
    }

    private fun serviceIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(context, requestCode, intent, pendingFlags())
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
}
