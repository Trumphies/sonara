package com.sonara.app.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sleep timer (v1.10.6): setelah [delayMs], [onFire] dipanggil
 * sekali (PlayerManager: pause). Murni Kotlin + coroutines, tanpa
 * import Android — bisa diuji di JVM biasa.
 *
 * Timer berbasis jam dinding (bukan jumlah lagu): tetap jalan
 * walau lagu berganti; dibatalkan bila pengguna memilih Off atau
 * timer sudah berbunyi. TIDAK persisten antar-restart app (timer
 * sesi, seperti kebiasaan umum) — memilih durasi baru memulai
 * timer baru. Berjalan selama proses app hidup; saat playback
 * jalan, foreground service PlaybackService menjaga proses tetap
 * hidup.
 */
class SleepTimer(
    private val onFire: () -> Unit,
) {
    private var job: Job? = null

    private val _endsAtMs = MutableStateFlow<Long?>(null)

    /** Epoch-ms saat timer berbunyi, atau null bila tidak aktif. */
    val endsAtMs: StateFlow<Long?> = _endsAtMs.asStateFlow()

    val isActive: Boolean get() = _endsAtMs.value != null

    /** Mulai timer baru (menggantikan timer lama bila ada). */
    fun start(scope: CoroutineScope, delayMs: Long) {
        cancel()
        if (delayMs <= 0L) return
        _endsAtMs.value = System.currentTimeMillis() + delayMs
        job = scope.launch {
            delay(delayMs)
            _endsAtMs.value = null
            onFire()
        }
    }

    /** Batalkan timer; memanggil berulang kali aman. */
    fun cancel() {
        job?.cancel()
        job = null
        _endsAtMs.value = null
    }
}
