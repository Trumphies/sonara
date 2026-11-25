package com.sonara.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import coil.Coil
import coil.request.ImageRequest
import com.sonara.app.MainActivity
import com.sonara.app.SonaraApplication
import com.sonara.app.data.Track
import com.sonara.app.widget.SonaraWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service supaya audio ExoPlayer tetap jalan saat app
 * di-background.
 *
 * Notifikasi media (v1.6 dipoles): artwork lagu sebagai large icon,
 * judul + artis + sumber yang benar, kontrol prev/play/next/stop
 * yang berfungsi, dan update tepat saat ganti lagu ATAU play/pause
 * berubah. MediaSession framework (tanpa dependensi baru) memberi
 * kontrol media sistem: lockscreen, Bluetooth/AVRCP, dan tampilan
 * MediaStyle ringkas. Kalau izin notifikasi ditolak user, pemutaran
 * tetap jalan di dalam app (service tetap start, tapi notifikasi
 * tidak tampil).
 */
class PlaybackService : Service() {
    companion object {
        const val CHANNEL_ID = "sonara_playback"
        const val NOTIFICATION_ID = 1
        const val ACTION_TOGGLE = "com.sonara.app.action.TOGGLE"
        const val ACTION_NEXT = "com.sonara.app.action.NEXT"
        const val ACTION_PREV = "com.sonara.app.action.PREV"
        const val ACTION_STOP = "com.sonara.app.action.STOP"
    }

    private var started = false
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collecting = false
    private var artworkJob: Job? = null
    private var lastArtwork: Bitmap? = null
    private var lastArtworkUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SONARA Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shows the currently playing song" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        // MediaSession: kontrol dari lockscreen / headset / Bluetooth
        // masuk lewat callback ini (jalur kedua selain tombol aksi
        // notifikasi — keduanya memanggil PlayerManager yang sama).
        mediaSession = MediaSession(this, "SONARA").apply {
            setCallback(
                object : MediaSession.Callback() {
                    override fun onPlay() {
                        (application as? SonaraApplication)?.player?.let {
                            if (!it.isPlaying.value) it.togglePlay()
                        }
                    }

                    override fun onPause() {
                        (application as? SonaraApplication)?.player?.let {
                            if (it.isPlaying.value) it.togglePlay()
                        }
                    }

                    override fun onSkipToNext() {
                        (application as? SonaraApplication)?.player?.next()
                    }

                    override fun onSkipToPrevious() {
                        (application as? SonaraApplication)?.player?.previous()
                    }

                    override fun onStop() {
                        (application as? SonaraApplication)?.player?.stop()
                        stopSelf()
                    }
                },
                Handler(Looper.getMainLooper()),
            )
            isActive = true
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        artworkJob?.cancel()
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {
        }
        mediaSession = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? SonaraApplication
        when (intent?.action) {
            ACTION_TOGGLE -> app?.player?.togglePlay()
            ACTION_NEXT -> app?.player?.next()
            ACTION_PREV -> app?.player?.previous()
            ACTION_STOP -> {
                app?.player?.stop()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // Ikuti perubahan lagu + status putar: setiap ganti lagu atau
        // toggle play/pause, session + notifikasi diperbarui. Sebelum
        // v1.6, service hanya di-start ulang saat ganti lagu, jadi
        // ikon play/pause notifikasi basi setelah toggle.
        if (!collecting && app != null) {
            collecting = true
            serviceScope.launch {
                combine(
                    app.player.currentTrack,
                    app.player.isPlaying,
                ) { track, playing -> track to playing }
                    .collect { (track, playing) ->
                        updateSession(track, playing)
                        refreshNotification(track, playing)
                        // v1.10.17: widget home screen mengikuti state
                        // yang sama (ganti lagu / play-pause). Suspend
                        // di dalam collect yang sudah coroutine; gagal
                        // = widget tetap di state terakhir, pemutaran
                        // tidak terganggu.
                        runCatching { SonaraWidget.refresh(applicationContext) }
                    }
            }
        }
        if (!started) {
            try {
                startForeground(NOTIFICATION_ID, buildNotification())
                // v1.0.2: `started` HANYA setelah startForeground SUKSES.
                // Sebelumnya flag ini disetel sebelum try, jadi sekali
                // startForeground gagal (mis. pembatasan start FGS
                // Android 12+ dari background), service berjalan TANPA
                // status foreground dan tidak pernah mencoba lagi —
                // gampang dibunuh sistem tanpa jejak. Kini gagal =
                // tetap false + tercatat di logcat, dan start berikutnya
                // (setiap ganti lagu / resume memanggil startService)
                // mencoba lagi. Notifikasi tetap diposting lewat
                // nm.notify di bawah (updateNotification) supaya kontrol
                // tetap terlihat walau foreground belum berhasil.
                started = true
            } catch (e: Exception) {
                // Izin foreground service / notifikasi ditolak, atau
                // start dari background ditolak sistem — lanjut tanpa
                // notifikasi foreground, dengan jejak di logcat.
                Log.w(
                    "PlaybackService",
                    "startForeground failed: ${e.message}",
                    e,
                )
            }
        } else {
            updateNotification()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getService(this, action.hashCode(), intent, flags)
    }

    /** Metadata + state sesi media untuk kontrol sistem (lockscreen/BT). */
    private fun updateSession(track: Track?, playing: Boolean) {
        val session = mediaSession ?: return
        try {
            val duration = (application as? SonaraApplication)?.player?.durationMs?.value ?: 0L
            val position = (application as? SonaraApplication)?.player?.positionMs?.value ?: 0L
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track?.title ?: "SONARA")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track?.artist ?: "")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track?.album ?: "")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
            if (lastArtwork != null && track?.imageUrl == lastArtworkUrl) {
                metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, lastArtwork)
            }
            session.setMetadata(metadata.build())
            val state = PlaybackState.Builder()
                .setState(
                    if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    position,
                    1f,
                )
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_STOP,
                )
                .build()
            session.setPlaybackState(state)
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(artwork: Bitmap? = lastArtwork): Notification {
        val app = application as? SonaraApplication
        val player = app?.player
        val track = player?.currentTrack?.value
        val playing = player?.isPlaying?.value ?: false
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0),
        )
        fun action(iconRes: Int, title: String, action: String): Notification.Action =
            Notification.Action.Builder(
                Icon.createWithResource(this, iconRes),
                title,
                actionIntent(action),
            ).build()
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(track?.title ?: "SONARA")
            .setContentText(track?.let { "${it.artist} • ${it.source.label}" } ?: "No song")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(action(android.R.drawable.ic_media_previous, "Previous", ACTION_PREV))
            .addAction(
                action(
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (playing) "Pause" else "Play",
                    ACTION_TOGGLE,
                ),
            )
            .addAction(action(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))
            .addAction(
                action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", ACTION_STOP),
            )
        if (artwork != null) builder.setLargeIcon(artwork)
        val session = mediaSession
        if (session != null) {
            builder.setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
        }
        return builder.build()
    }

    /** Notifikasi ulang tanpa artwork (cepat, sinkron). */
    private fun updateNotification() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) {
        }
    }

    /**
     * Notifikasi ulang + muat artwork lagu aktif lewat cache Coil
     * (sama dengan yang dipakai UI). Artwork dimuat sekali per URL;
     * gagal dimuat = notifikasi tanpa artwork, pemutaran tidak
     * terganggu.
     */
    private fun refreshNotification(track: Track?, playing: Boolean) {
        updateNotification()
        val url = track?.imageUrl
        if (url.isNullOrBlank()) {
            lastArtwork = null
            lastArtworkUrl = null
            return
        }
        if (url == lastArtworkUrl && lastArtwork != null) {
            updateSession(track, playing)
            return
        }
        artworkJob?.cancel()
        artworkJob = serviceScope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadArtwork(url) }
            if (bitmap != null) {
                lastArtwork = bitmap
                lastArtworkUrl = url
                updateNotification()
                updateSession(track, playing)
            }
        }
    }

    private suspend fun loadArtwork(url: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .size(512, 512)
                .build()
            val result = Coil.imageLoader(this).execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {
            null
        }
    }
}
