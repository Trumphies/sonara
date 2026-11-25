package com.sonara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.data.Selection
import com.sonara.app.data.bulkQueueAllowed
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.StageBg

/**
 * Status seleksi multi-lagu per daftar (v1.10.21). Satu instance
 * per daftar — seleksi tidak pernah bocor antar-daftar; instance
 * diingat (remember) di scope daftarnya dan dibuang saat daftar
 * ditinggalkan. Logika himpunannya murni ([Selection]).
 *
 * [modeActive] terpisah dari isi himpunan: mode bisa aktif dengan
 * 0 terpilih (masuk lewat tombol "Select"); keluar mode lewat
 * Back/× atau saat daftar berubah (pemilik me-reset).
 */
class SelectionState {
    var selection by mutableStateOf(Selection())
        private set
    var modeActive by mutableStateOf(false)
        private set

    val count: Int get() = selection.count

    /** Masuk mode + pilih baris ini (long-press). */
    fun enter(key: String) {
        selection = Selection(setOf(key))
        modeActive = true
    }

    /** Masuk mode tanpa pilihan awal (tombol "Select"). */
    fun enterMode() {
        modeActive = true
    }

    fun toggle(key: String) {
        selection = selection.toggle(key)
    }

    fun isSelected(key: String): Boolean = key in selection.keys

    fun exitMode() {
        modeActive = false
        selection = Selection()
    }
}

@Composable
fun rememberSelectionState(): SelectionState = remember { SelectionState() }

/**
 * Bar aksi mode seleksi: hitungan + "Add to queue" (DISEMBUNYIKAN
 * selama sesi Jam) + "Save to playlist" + × keluar. Bar gelap di
 * semua layar (konteks mode yang jelas, tanpa redesign tema).
 */
@Composable
fun SelectionActionBar(
    count: Int,
    jamActive: Boolean,
    onAddToQueue: () -> Unit,
    onSaveToPlaylist: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionColor = Color.White.copy(alpha = if (count > 0) 1f else 0.4f)
    Row(
        modifier
            .fillMaxWidth()
            .background(StageBg)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (count == 1) "1 selected" else "$count selected",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        if (bulkQueueAllowed(jamActive)) {
            TextButton(onClick = { if (count > 0) onAddToQueue() }) {
                Text("Add to queue", color = actionColor, fontSize = 14.sp)
            }
        }
        TextButton(onClick = { if (count > 0) onSaveToPlaylist() }) {
            Text("Save to playlist", color = actionColor, fontSize = 14.sp)
        }
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Exit selection",
                tint = Color.White,
            )
        }
    }
}
