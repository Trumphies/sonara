package com.sonara.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.sonara.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Kartu lirik (v1.10.16): gambar tema ARUS dari baris lirik ASLI
 * (dipilih [selectLyricCardLines] — teks tidak pernah diedit atau
 * dikarang), dibagikan lewat FileProvider. Warna literal di bawah
 * mencerminkan token tema di Theme.kt (PaperBg #FFF9ED, InkBlack
 * #1B1813, InkMuted #8A8378, BurntOrange #FF4E24) — android.graphics
 * tidak memakai Compose Color.
 */
object LyricCard {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val COLOR_PAPER = 0xFFFFF9ED.toInt()
    private const val COLOR_INK = 0xFF1B1813.toInt()
    private const val COLOR_MUTED = 0xFF8A8378.toInt()
    private const val COLOR_ORANGE = 0xFFFF4E24.toInt()

    /**
     * Render kartu 1080×1350: baris lirik besar di tengah, judul —
     * artis kecil di bawah, "SONARA" + sumber lirik di kaki kartu.
     * Ukuran teks lirik mengecil otomatis bila tidak muat.
     */
    fun renderBitmap(
        lyricLines: List<String>,
        title: String,
        artist: String,
        source: String,
        /** Typeface baris lirik (v1.10.18: Plus Jakarta Sans bila tersedia). */
        lyricTypeface: Typeface? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_PAPER)

        // Garis oranye di atas blok lirik.
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ORANGE }
        val ruleW = 96f
        val ruleH = 10f
        val ruleTop = 300f
        canvas.drawRoundRect(
            (WIDTH - ruleW) / 2f, ruleTop,
            (WIDTH + ruleW) / 2f, ruleTop + ruleH,
            ruleH / 2f, ruleH / 2f, rulePaint,
        )

        // Blok lirik: StaticLayout terpusat, ukuran mengecil sampai muat.
        val maxTextWidth = WIDTH - 160
        val maxBlockHeight = 560
        var textSize = 88f
        var layout: StaticLayout? = null
        val joined = lyricLines.joinToString("\n")
        while (textSize >= 52f) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK
                this.textSize = textSize
                // Plus Jakarta Sans (BOLD sintetis dari Regular) bila
                // pemanggil berhasil memuatnya; fallback DEFAULT.
                typeface = lyricTypeface?.let { Typeface.create(it, Typeface.BOLD) }
                    ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val candidate = StaticLayout.Builder
                .obtain(joined, 0, joined.length, paint, maxTextWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.3f)
                .build()
            layout = candidate
            if (candidate.height <= maxBlockHeight) break
            textSize -= 8f
        }
        val lyricLayout = layout!!
        val blockTop = 380f
        canvas.save()
        canvas.translate((WIDTH - lyricLayout.width) / 2f, blockTop)
        lyricLayout.draw(canvas)
        canvas.restore()

        // Judul — artis, satu baris terpotong jujur (ellipsis).
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 44f
            typeface = Typeface.DEFAULT
        }
        val titleText = if (artist.isNotBlank()) "$title — $artist" else title
        val ellipsized = TextUtils.ellipsize(
            titleText, titlePaint, maxTextWidth.toFloat(), TextUtils.TruncateAt.END,
        ).toString()
        val titleY = blockTop + lyricLayout.height + 72f
        canvas.drawText(
            ellipsized,
            (WIDTH - titlePaint.measureText(ellipsized)) / 2f,
            titleY,
            titlePaint,
        )

        // Kaki kartu: sumber lirik (bila ada) + mark SONARA.
        val footPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 30f
            typeface = Typeface.DEFAULT
        }
        if (source.isNotBlank()) {
            val src = "Lyrics from $source"
            canvas.drawText(
                src,
                (WIDTH - footPaint.measureText(src)) / 2f,
                HEIGHT - 168f,
                footPaint,
            )
        }
        val markPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.35f
        }
        val mark = "SONARA"
        canvas.drawText(
            mark,
            (WIDTH - markPaint.measureText(mark)) / 2f,
            HEIGHT - 96f,
            markPaint,
        )
        return bitmap
    }

    /**
     * Buat PNG kartu di cache + bagikan sebagai gambar. False bila
     * tidak ada baris lirik yang bisa dibagikan atau pembuatan gagal
     * (pemanggil menampilkan pesan jujur, tanpa toast karangan).
     */
    suspend fun share(
        context: Context,
        track: Track,
        kind: LyricsApi.Kind,
        lines: List<LyricsApi.Line>,
        activeIndex: Int,
        source: String,
    ): Boolean = withContext(Dispatchers.Default) {
        val cardLines = selectLyricCardLines(kind, lines, activeIndex)
        if (cardLines.isEmpty()) return@withContext false
        try {
            // Font kartu = font lirik panel (Plus Jakarta Sans, OFL);
            // gagal muat → null → DEFAULT di renderBitmap.
            val lyricTypeface = runCatching {
                ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_regular)
            }.getOrNull()
            val bitmap = renderBitmap(
                cardLines, track.title, track.artist, source,
                lyricTypeface = lyricTypeface,
            )
            val dir = File(context.cacheDir, "lyric-cards").apply { mkdirs() }
            val file = File(dir, "lyric-card-${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            val uri = FileProvider.getUriForFile(
                context,
                "com.sonara.app.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareTextFor(track))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Share lyric card"))
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
