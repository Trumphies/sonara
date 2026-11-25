package com.sonara.app.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * SATU-SATUNYA cara menampilkan Toast di app (v1.10.1, perbaikan
 * crash 22 Nov 2026).
 *
 * Crash v1.10: `Toast.makeText` dipanggil dari thread background
 * (callback login Google di Dispatchers.IO) — Android melempar
 * "Can't toast on a thread that has not called Looper.prepare()".
 * Helper ini aman dipanggil dari thread apa pun: kalau pemanggil
 * bukan main thread, toast di-post ke main looper. Selalu pakai
 * applicationContext supaya toast tidak menahan Activity.
 */
fun showToast(context: Context, message: String, long: Boolean = false) {
    val appContext = context.applicationContext
    val duration = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast.makeText(appContext, message, duration).show()
    } else {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, message, duration).show()
        }
    }
}
