package com.sonara.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Loop sinkron berkala (v1.10.6): memanggil [tick] tiap
 * [intervalMs] selama berjalan. Murni Kotlin + coroutines, tanpa
 * import Android — bisa diuji di JVM biasa.
 *
 * Dipakai SyncManager untuk auto-sync 1 menit saat app di
 * foreground: pemanggil (SonaraApp) memanggil [start] saat
 * ON_START dan [stop] saat ON_STOP. Tick PERTAMA terjadi setelah
 * satu interval penuh — sinkron langsung saat kembali foreground
 * dilakukan pemanggil secara terpisah. Tidak ada WorkManager /
 * foreground service: Android tidak mengizinkan background work
 * periodik 1 menit, dan sinkron 1 menit hanya berlaku selama app
 * terbuka (lihat BUILD-v1.10.6.md).
 */
class AutoSyncLoop(
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val tick: suspend () -> Unit,
) {
    private var job: Job? = null

    /** Mulai loop; tidak ada efek bila sudah berjalan. */
    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(intervalMs)
                tick()
            }
        }
    }

    /** Hentikan loop; memanggil berulang kali aman. */
    fun stop() {
        job?.cancel()
        job = null
    }

    val running: Boolean get() = job?.isActive == true

    companion object {
        const val DEFAULT_INTERVAL_MS: Long = 60_000L
    }
}
