package com.sonara.app

import android.app.Application
import com.sonara.app.data.CrashReportStore
import com.sonara.app.data.JamSessionManager
import com.sonara.app.data.LocalStore
import com.sonara.app.data.SyncManager
import com.sonara.app.player.PlayerManager
import com.sonara.app.player.WebEmbedPlayer

/**
 * Application — baris pertama: pasang crash reporter (skill
 * android_launch_crash_diagnosis). Baru setelah itu buat store +
 * player. Tidak ada init jaringan / berat di sini.
 */
class SonaraApplication : Application() {
    lateinit var store: LocalStore
        private set
    lateinit var player: PlayerManager
        private set
    lateinit var embedPlayer: WebEmbedPlayer
        private set

    /** Login Google + sinkron cloud (v1.10). Dibuat setelah store. */
    lateinit var sync: SyncManager
        private set

    /** Jamming Together (v1.10.6). Dibuat setelah player + sync. */
    lateinit var jam: JamSessionManager
        private set

    /**
     * Kode Jam dari deep link yang diketuk (sonara://jam/<KODE>
     * atau https://moloqo.github.io/sonara/jam/<KODE>,
     * v1.10.11) — ditulis MainActivity (cold + warm start), dikonsumsi
     * SEKALI oleh SonaraApp (lalu di-nol-kan) supaya tidak join
     * berulang tiap recomposition.
     */
    val pendingJamCode = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /**
     * videoId YouTube Music dari link yang diketuk di app lain
     * (v1.10.21) — ditulis MainActivity, dikonsumsi SEKALI oleh
     * SonaraApp (resolve metadata + putar). Link tak sah / playlist
     * ditolak di MainActivity dengan toast, tidak sampai ke sini.
     */
    val pendingYtmVideoId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate() {
        // LANGKAH PERTAMA — sebelum apa pun yang bisa crash.
        CrashReportStore.install(this)
        super.onCreate()
        store = LocalStore(this)
        embedPlayer = WebEmbedPlayer(this)
        player = PlayerManager(this, store, embedPlayer)
        // SyncManager memasang callback perubahan koleksi di store
        // dan memulihkan sesi bila ada; konstruktornya tidak
        // melakukan I/O sinkron, jadi aman di onCreate.
        sync = SyncManager(this, store)
        // Jam memakai player + sync yang sudah ada (tidak mengubahnya).
        jam = JamSessionManager(this, player, sync)
        // v1.10.21: gerbang posisi-lanjut — player tahu kapan sesi
        // Jam aktif (posisi milik sesi, bukan milik perangkat).
        player.jamActiveProvider = { jam.uiState.value.active }
    }
}
