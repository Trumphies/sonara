package com.sonara.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
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
 * Kartu statistik tahunan (v1.0.6): gambar tema ARUS dari angka
 * NYATA perangkat ini (peristiwa putar tercatat + hitungan putar
 * lokal), dibagikan lewat FileProvider — pola yang sama dengan
 * LyricCard. Tidak ada angka karangan: tanpa data, kartu tidak
 * dibuat ([share] mengembalikan false).
 *
 * Warna literal mencerminkan token tema di Theme.kt (PaperBg
 * #FFF9ED, InkBlack #1B1813, InkMuted #8A8378, BurntOrange
 * #FF4E24) — android.graphics tidak memakai Compose Color.
 */
object StatsCard {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val COLOR_PAPER = 0xFFFFF9ED.toInt()
    private const val COLOR_INK = 0xFF1B1813.toInt()
    private const val COLOR_MUTED = 0xFF8A8378.toInt()
    private const val COLOR_ORANGE = 0xFFFF4E24.toInt()

    /**
     * Render kartu 1080×1350: jumlah putar besar, menit dengar,
     * artis + lagu teratas, dan catatan kejujuran (menit hanya dari
     * putar yang durasinya diketahui; peristiwa tercatat mulai
     * v1.0.6).
     */
    fun renderBitmap(
        stats: YearStats,
        year: Int,
        headlineTypeface: Typeface? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_PAPER)

        // Garis oranye di atas.
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ORANGE }
        val ruleW = 96f
        val ruleH = 10f
        val ruleTop = 220f
        canvas.drawRoundRect(
            (WIDTH - ruleW) / 2f, ruleTop,
            (WIDTH + ruleW) / 2f, ruleTop + ruleH,
            ruleH / 2f, ruleH / 2f, rulePaint,
        )

        fun centeredText(
            text: String,
            y: Float,
            size: Float,
            color: Int,
            bold: Boolean,
            maxWidth: Float = (WIDTH - 160).toFloat(),
            letterSpacing: Float = 0f,
            typeface: Typeface? = null,
        ): Float {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = size
                this.typeface = typeface
                    ?: Typeface.create(
                        Typeface.DEFAULT,
                        if (bold) Typeface.BOLD else Typeface.NORMAL,
                    )
                this.letterSpacing = letterSpacing
            }
            val ellipsized = TextUtils.ellipsize(
                text, paint, maxWidth, TextUtils.TruncateAt.END,
            ).toString()
            canvas.drawText(
                ellipsized,
                (WIDTH - paint.measureText(ellipsized)) / 2f,
                y,
                paint,
            )
            return y
        }

        // Label tahun.
        centeredText(
            text = "YOUR $year",
            y = 340f,
            size = 40f,
            color = COLOR_MUTED,
            bold = true,
            letterSpacing = 0.35f,
        )
        // Angka putar besar.
        centeredText(
            text = "${stats.plays}",
            y = 520f,
            size = 220f,
            color = COLOR_INK,
            bold = true,
            typeface = headlineTypeface?.let { Typeface.create(it, Typeface.BOLD) },
        )
        centeredText(
            text = if (stats.plays == 1) "play" else "plays",
            y = 600f,
            size = 44f,
            color = COLOR_MUTED,
            bold = false,
        )
        // Menit dengar.
        centeredText(
            text = "${stats.minutes} minutes listening",
            y = 700f,
            size = 48f,
            color = COLOR_INK,
            bold = true,
        )
        // Artis + lagu teratas.
        var rowY = 800f
        val topArtist = stats.topArtists.firstOrNull()
        if (topArtist != null) {
            centeredText(
                text = "Top artist: ${topArtist.artist} (${topArtist.plays} plays)",
                y = rowY,
                size = 40f,
                color = COLOR_INK,
                bold = false,
            )
            rowY += 64f
        }
        val topTrack = stats.topTracks.firstOrNull()
        if (topTrack != null) {
            val trackLine = if (topTrack.artist.isNotBlank()) {
                "Top song: ${topTrack.title} — ${topTrack.artist} (${topTrack.plays} plays)"
            } else {
                "Top song: ${topTrack.title} (${topTrack.plays} plays)"
            }
            centeredText(
                text = trackLine,
                y = rowY,
                size = 40f,
                color = COLOR_INK,
                bold = false,
            )
            rowY += 64f
        }
        // Catatan kejujuran.
        if (stats.knownDurationPlays < stats.plays) {
            centeredText(
                text = "Listening time counts ${stats.knownDurationPlays} of " +
                    "${stats.plays} plays with known lengths.",
                y = rowY + 40f,
                size = 30f,
                color = COLOR_MUTED,
                bold = false,
            )
        }
        centeredText(
            text = "Plays recorded on this device since v1.0.6.",
            y = HEIGHT - 220f,
            size = 30f,
            color = COLOR_MUTED,
            bold = false,
        )

        // Kaki kartu: mark SONARA.
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
     * tidak ada data tahun itu atau pembuatan gagal.
     */
    suspend fun share(
        context: Context,
        stats: YearStats,
        year: Int,
    ): Boolean = withContext(Dispatchers.Default) {
        if (!stats.hasData) return@withContext false
        try {
            val headlineTypeface = runCatching {
                ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_regular)
            }.getOrNull()
            val bitmap = renderBitmap(stats, year, headlineTypeface)
            val dir = File(context.cacheDir, "stats-cards").apply { mkdirs() }
            val file = File(dir, "stats-card-$year-${System.currentTimeMillis()}.png")
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
                putExtra(
                    Intent.EXTRA_TEXT,
                    "My $year in SONARA: ${stats.plays} plays, " +
                        "${stats.minutes} minutes listening.",
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Share year card"))
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
