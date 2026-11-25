package com.sonara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.data.PlaybackSpeedPolicy
import com.sonara.app.ui.theme.AccentFill
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.OnAccent
import com.sonara.app.ui.theme.SegmentBg

/**
 * Dialog pilihan kecepatan putar (v1.0.5) — dipakai layar detail,
 * TrackSheet, dan Settings (kecepatan default). Pilihan dari
 * [PlaybackSpeedPolicy.SPEED_CHOICES]; nilai terpilih ditandai. Penjelasan di bawah
 * menyatakan aturan yang berlaku: pilihan disimpan sebagai default
 * untuk lagu baru (dan per episode untuk konten panjang), dan
 * selama sesi Jam drift Jam yang mengendalikan kecepatan.
 */
@Composable
fun SpeedDialog(
    currentRate: Float,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            Column {
                Text(
                    "Applies right away, and becomes the default for new tracks. " +
                        "For long episodes, the choice is remembered per episode. " +
                        "During a Jam, Jam drift controls the speed instead.",
                )
                Spacer(Modifier.height(14.dp))
                val rows = PlaybackSpeedPolicy.SPEED_CHOICES.chunked(4)
                for ((rowIndex, row) in rows.withIndex()) {
                    Row(Modifier.fillMaxWidth()) {
                        for (rate in row) {
                            val selected = rate == currentRate
                            Box(
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(Corners.button))
                                    .background(if (selected) AccentFill else SegmentBg)
                                    .clickable { onSelect(rate) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    PlaybackSpeedPolicy.formatSpeed(rate),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) OnAccent else InkBlack,
                                )
                            }
                            if (rate != row.last()) {
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                        // Baris terakhir yang kurang dari 4: isi sisa
                        // dengan spacer supaya lebar chip konsisten.
                        repeat(4 - row.size) {
                            Spacer(Modifier.width(8.dp))
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (rowIndex != rows.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = sonaraTextButtonColors(),
            ) { Text("Cancel") }
        },
    )
}
