package com.sonara.app

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sonara.app.data.CrashReportStore
import com.sonara.app.data.isJamDeepLinkUri
import com.sonara.app.data.isYtmLinkUri
import com.sonara.app.data.parseJamDeepLink
import com.sonara.app.data.parseYtmLink
import com.sonara.app.data.YtmLinkResult
import com.sonara.app.ui.SonaraApp
import com.sonara.app.ui.showToast

/**
 * Entry point SONARA — pola Widget Pet v2.2/v2.3:
 * laporan crash dibaca di onCreate SEBELUM setContent, ditampilkan
 * lewat AlertDialog polos (tanpa Compose) supaya laporan tetap
 * sampai walau setContent-lah yang crash.
 */
class MainActivity : ComponentActivity() {
    private var pendingCrashReport: String? = null
    private var pendingHandlerMarker: String? = null
    private var contentStarted = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    private val mediaPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // v1.10.11/10.18: deep link Jam (sonara://jam/<KODE> atau
        // https://moloqo.github.io/sonara/jam/<KODE>) — cold start.
        // Kode disimpan di Application (StateFlow) dan dikonsumsi
        // sekali oleh SonaraApp; aman dipanggil sebelum setContent.
        handleJamDeepLink(intent)
        // v1.10.21: link YouTube Music (cold start) — lihat
        // handleYtmLink; Jam ditangani di atas, host-nya berbeda.
        handleYtmLink(intent)
        // LANGKAH PERTAMA: baca laporan + marker sebelum apa pun yang bisa crash.
        pendingCrashReport = runCatching { CrashReportStore.readLatest(this) }.getOrNull()
        pendingHandlerMarker = runCatching { CrashReportStore.readHandlerMarker(this) }.getOrNull()

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Izin media lokal (MP3 di perangkat) — kalau ditolak, Pustaka
            // → Musik Lokal tampil kosong, bukan crash.
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                mediaPermission.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            mediaPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val report = pendingCrashReport
        if (report != null) {
            runCatching { showCrashReportDialog(report) }
                .onFailure { startContent() }
        } else {
            startContent()
        }
    }

    /**
     * Deep link Jam (v1.10.11 custom scheme; v1.10.18 App Link https):
     * ACTION_VIEW + sonara://jam/<KODE> atau
     * https://moloqo.github.io/sonara/jam/<KODE>.
     * Kode sah → antre di Application untuk join sekali; URI Jam tapi
     * kode tak sah → toast jujur, tanpa crash, tanpa join. Intent
     * lain (launcher dsb.) diabaikan.
     */
    private fun handleJamDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val raw = intent.dataString ?: return
        if (!isJamDeepLinkUri(raw)) return
        val code = parseJamDeepLink(raw)
        if (code != null) {
            (application as SonaraApplication).pendingJamCode.value = code
        } else {
            showToast(this, "That Jam link isn't valid.")
        }
    }

    /**
     * Link YouTube Music (v1.10.21): ACTION_VIEW +
     * https://music.youtube.com/watch?v=... atau https://youtu.be/...
     * (intent filter TANPA autoVerify — chooser). videoId sah →
     * antre di Application untuk resolve + putar sekali;
     * playlist-only / tak sah → toast jujur, tanpa crash.
     * Intent lain diabaikan.
     */
    private fun handleYtmLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val raw = intent.dataString ?: return
        if (!isYtmLinkUri(raw)) return
        when (val result = parseYtmLink(raw)) {
            is YtmLinkResult.Video -> {
                (application as SonaraApplication).pendingYtmVideoId.value = result.videoId
            }
            is YtmLinkResult.PlaylistOnly -> {
                showToast(this, "Playlist links aren't supported yet.")
            }
            YtmLinkResult.Invalid -> {
                showToast(this, "That YouTube Music link couldn't be opened.")
            }
        }
    }

    // v1.10.11: warm start (singleTop) — ketuk link saat app sudah
    // jalan masuk lewat sini, bukan onCreate baru.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleJamDeepLink(intent)
        handleYtmLink(intent)
    }

    private fun startContent() {
        if (contentStarted) return
        contentStarted = true
        enableEdgeToEdge()
        setContent {
            val app = application as SonaraApplication
            var crashReport = pendingCrashReport
            if (crashReport != null) {
                // Laporan sudah tampil lewat dialog; layar Compose hanya
                // menampilkan app normal setelah dialog ditutup. Kalau
                // user memilih "Lanjutkan" tanpa hapus, laporan tetap
                // bisa dibuka dari Pengaturan → Laporan Crash.
                SonaraApp(app = app, crashReport = null) {}
            } else {
                SonaraApp(app = app, crashReport = null) {}
            }
        }
    }

    private fun showCrashReportDialog(report: String) {
        val statusLine = pendingHandlerMarker
            ?: "The handler is NOT installed — if the app crashes, the cause won't be recorded."
        val fullText = "$statusLine\n\n$report"
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val textView = TextView(this).apply {
            text = fullText
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setPadding(pad, pad, pad, pad)
            setTextIsSelectable(true)
        }
        val scrollView = ScrollView(this).apply { addView(textView) }
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Crash Report")
            .setView(scrollView)
            .setPositiveButton("Continue", null)
            .setNegativeButton("Delete") { _, _ ->
                runCatching { CrashReportStore.clearAll(applicationContext) }
                pendingCrashReport = null
            }
            .setNeutralButton("Copy Report", null)
            .setOnDismissListener {
                if (!isFinishing && !isDestroyed) startContent()
            }
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            runCatching {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("Crash Report", fullText),
                )
                showToast(this, "Report copied")
            }
        }
    }
}

@Composable
private fun LoadingSplash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
