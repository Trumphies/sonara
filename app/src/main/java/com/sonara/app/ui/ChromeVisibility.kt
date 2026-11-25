package com.sonara.app.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

/**
 * Auto-hide klaster bawah (v1.10.18, logika MURNI tanpa Android —
 * bisa diuji di JVM): chrome bersembunyi saat scroll ke bawah
 * melewati ambang dan kembali seketika saat scroll ke atas.
 *
 * Posisi scroll dipetakan ke piksel semu: tiap item dihitung
 * setinggi [hideThresholdPx] (perkiraan — tinggi item bervariasi,
 * jadi ambang bersifat perkiraan 24–48dp perjalanan, sesuai brief),
 * offset di dalam item dijepit ke rentang itu. Aturannya:
 * - Gerak ke atas sekecil apa pun → tampil seketika, anchor reset.
 * - Gerak ke bawah melewati ambang dari anchor → sembunyi, anchor
 *   reset (supaya jitter di sekitar ambang tidak flicker: tampil
 *   ulang butuh gerak naik, sembunyi ulang butuh satu ambang penuh).
 * - [forceVisible] dipakai saat ganti lagu / sheet overlay terbuka /
 *   pindah layar: chrome harus terlihat, anchor diselaraskan ke
 *   posisi terakhir supaya tidak langsung sembunyi lagi.
 */
class ChromeVisibilityState(private val hideThresholdPx: Int) {

    var visible: Boolean = true
        private set

    private var anchorPos: Int = 0
    private var lastPos: Int = 0

    /** Terima posisi scroll terbaru; kembalikan visibilitas chrome. */
    fun onScroll(firstVisibleItemIndex: Int, firstVisibleItemScrollOffsetPx: Int): Boolean {
        val pos = firstVisibleItemIndex * hideThresholdPx +
            firstVisibleItemScrollOffsetPx.coerceIn(0, hideThresholdPx)
        if (pos < lastPos) {
            visible = true
            anchorPos = pos
        } else if (pos - anchorPos > hideThresholdPx) {
            visible = false
            anchorPos = pos
        }
        lastPos = pos
        return visible
    }

    /** Paksa tampil (ganti lagu / overlay / pindah layar). */
    fun forceVisible() {
        visible = true
        anchorPos = lastPos
    }
}

/**
 * Observer scroll untuk auto-hide chrome (v1.10.18): melaporkan
 * (firstVisibleItemIndex, firstVisibleItemScrollOffset) ke pemanggil
 * setiap kali posisi berubah. [enabled] = false saat sheet/dialog
 * terbuka atau layar bukan list yang bisa di-scroll — saat itu
 * posisi TIDAK dilaporkan (chrome tidak bersembunyi di tengah
 * sheet). Pemanggil meneruskan ke [ChromeVisibilityState.onScroll]
 * dan mencerminkan hasilnya ke state Compose.
 */
@Composable
fun ChromeScrollEffect(
    listState: LazyListState,
    enabled: Boolean,
    onScroll: (Int, Int) -> Unit,
) {
    LaunchedEffect(listState, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
            listState.firstVisibleItemIndex to
                listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) -> onScroll(index, offset) }
    }
}
