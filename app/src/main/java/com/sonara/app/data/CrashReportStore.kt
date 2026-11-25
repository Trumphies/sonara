package com.sonara.app.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Crash reporter — pola yang terbukti di Widget Pet v2.2/v2.3.
 * Handler dipasang sebagai baris pertama di Application.onCreate.
 * Laporan ditulis sinkron ke filesDir + cacheDir (maks. 3 per lokasi),
 * dan diduplikasi ke SharedPreferences supaya satu kegagalan tulis
 * tidak menghilangkan satu-satunya salinan. MainActivity membaca
 * laporan SEBELUM setContent dan menampilkannya lewat AlertDialog
 * polos (tanpa Compose), supaya laporan tetap sampai walau
 * setContent-lah yang crash.
 */
object CrashReportStore {
    private const val DIR_NAME = "crash-reports"
    private const val MARKER_NAME = "handler-installed.txt"
    private const val PREFS_NAME = "sonara_crash"
    private const val PREF_LAST_REPORT = "last_report"
    private const val MAX_REPORTS = 3

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeReport(appContext, thread, throwable)
            } catch (_: Throwable) {
            }
            try {
                previous?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {
            }
            if (previous == null) {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }
        try {
            writeMarker(appContext)
        } catch (_: Throwable) {
        }
    }

    fun readHandlerMarker(context: Context): String? {
        return try {
            val file = File(File(context.filesDir, DIR_NAME), MARKER_NAME)
            if (!file.isFile) return null
            file.readText().trim().ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    fun readLatest(context: Context): String? {
        // SharedPreferences dulu (duplikat), lalu file terbaru.
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fromPrefs = prefs.getString(PREF_LAST_REPORT, null)
            if (!fromPrefs.isNullOrEmpty()) return fromPrefs
        } catch (_: Exception) {
        }
        return try {
            latestFile(context)?.readText()
        } catch (_: Exception) {
            null
        }
    }

    fun clearAll(context: Context) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(PREF_LAST_REPORT).apply()
        } catch (_: Exception) {
        }
        try {
            reportDirs(context).forEach { dir ->
                dir.listFiles()?.forEach { runCatching { it.delete() } }
            }
        } catch (_: Exception) {
        }
    }

    private fun reportDirs(context: Context): List<File> {
        val out = ArrayList<File>(2)
        try {
            out.add(File(context.filesDir, DIR_NAME))
        } catch (_: Exception) {
        }
        try {
            val cache = context.cacheDir
            if (cache != null) out.add(File(cache, DIR_NAME))
        } catch (_: Exception) {
        }
        return out
    }

    private fun latestFile(context: Context): File? {
        val files = ArrayList<File>()
        for (dir in reportDirs(context)) {
            try {
                dir.listFiles()
                    ?.filter { it.isFile && it.name.startsWith("crash-") && it.name.endsWith(".txt") }
                    ?.let { files.addAll(it) }
            } catch (_: Exception) {
            }
        }
        return files.maxByOrNull { it.name }
    }

    private fun writeMarker(context: Context) {
        val dir = File(context.filesDir, DIR_NAME)
        dir.mkdirs()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        File(dir, MARKER_NAME).writeText("Handler installed: $time\n")
    }

    private fun writeReport(context: Context, thread: Thread, throwable: Throwable) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val version = try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            "${info.versionName} (${info.versionCode})"
        } catch (_: Exception) {
            "unknown"
        }
        val traceWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(traceWriter))
        val text = buildString {
            appendLine("SONARA — Crash Report")
            appendLine("Time: $time")
            appendLine("App: $version")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
            appendLine()
            appendLine("--- Stack trace ---")
            append(traceWriter.toString())
        }
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(PREF_LAST_REPORT, text).apply()
        } catch (_: Throwable) {
        }
        for (dir in reportDirs(context)) {
            try {
                dir.mkdirs()
                File(dir, "crash-${System.currentTimeMillis()}.txt").writeText(text)
                val files = dir.listFiles()
                    ?.filter { it.isFile && it.name.startsWith("crash-") && it.name.endsWith(".txt") }
                    ?.sortedBy { it.name }
                    ?: emptyList()
                files.dropLast(MAX_REPORTS).forEach { runCatching { it.delete() } }
            } catch (_: Throwable) {
            }
        }
    }
}
