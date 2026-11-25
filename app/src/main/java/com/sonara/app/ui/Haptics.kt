package com.sonara.app.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * HAPTICS (v1.6) — getar halus sebagai respons sentuh.
 *
 * `View.performHapticFeedback` menghormati pengaturan getar sistem:
 * kalau pengguna mematikan "Haptic feedback"/getar di Pengaturan
 * Android, panggilan ini tidak menghasilkan apa-apa. Jadi tidak ada
 * toggle sendiri di app — sistem yang menentukan.
 *
 * Dipakai di: Like (Confirm), play/pause + ganti lagu (Tap), ganti
 * tab/segmen (Selection), lepas knob seek (Confirm), mulai drag
 * antrean (LongPress), antrean berpindah slot saat drag (Tap).
 */
enum class HapticKind {
    Tap,
    Confirm,
    Selection,
    LongPress,
}

@Composable
fun rememberHaptic(): (HapticKind) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { kind ->
            val constant = when (kind) {
                HapticKind.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
                HapticKind.Confirm -> HapticFeedbackConstants.CONTEXT_CLICK
                HapticKind.Selection -> HapticFeedbackConstants.VIRTUAL_KEY
                HapticKind.LongPress -> HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(constant)
        }
    }
}

/** Haptic untuk pemakaian di luar Composable (butuh View). */
fun View.performHaptic(kind: HapticKind) {
    val constant = when (kind) {
        HapticKind.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticKind.Confirm -> HapticFeedbackConstants.CONTEXT_CLICK
        HapticKind.Selection -> HapticFeedbackConstants.VIRTUAL_KEY
        HapticKind.LongPress -> HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}
