package com.sonara.app.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sonara.app.data.DetailMotionSpec
import com.sonara.app.data.deriveMiniHeaderColor
import com.sonara.app.data.derivePanelColor
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.DetailBgFallback
import com.sonara.app.ui.theme.Motion
import com.sonara.app.ui.theme.themeEasing
import com.sonara.app.ui.theme.WaveCream
import com.sonara.app.ui.theme.WaveGreen
import com.sonara.app.ui.theme.WaveLightGreen
import com.sonara.app.ui.theme.WaveRed
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
/**
 * GARIS ARUS — tanda khas SONARA v1.5.
 *
 * Gelombang anyaman dua warna (hijau + merah) yang dipakai sebagai progress bar, hiasan poster, dan penanda lagu
 * yang sedang diputar. Gelombangnya MENGALIR selama lagu jalan dan
 * diam saat pause (token GERAKAN). Bisa diketuk DAN diseret untuk seek.
 *
 * Performa: transisi fase & denyut knob HANYA berjalan saat lagu jalan
 * dan reduced-motion mati — saat pause tidak ada recomposition 60fps
 * yang sia-sia (transisi tidak dibuat sama sekali).
 *
 * @param progress 0f..1f posisi putar.
 * @param playing aliran gelombang hidup hanya saat true.
 * @param onSeek dipanggil dengan fraksi 0f..1f saat diketuk/diseret.
 * @param showKnob tampilkan knob putih di ujung progress.
 */
@Composable
fun ArusWaveProgress(
    progress: Float,
    playing: Boolean,
    onSeek: ((Float) -> Unit)? = null,
    onSeekEnd: (() -> Unit)? = null,
    showKnob: Boolean = true,
    amplitude: Dp = 5.dp,
    strokeWidth: Dp = 3.dp,
    height: Dp = 28.dp,
    // Warna gelombang — default merah + hijau muda (fallback tanpa
    // artwork v1.10.6); detail lagu dan strip kini mengoper palet
    // artwork lewat rememberWavePalette (v1.10.6).
    waveColorA: Color = WaveLightGreen,
    waveColorB: Color = WaveRed,
    trackAlpha: Float = 0.28f,
    /**
     * v1.10.24 (detail saja): amplitudo merespons main/jeda —
     * mengembang saat jalan, mendatar saat jeda (DetailMotionSpec).
     * Default false: strip mini player + pemanggil lain tidak berubah.
     */
    responsiveAmplitude: Boolean = false,
    /**
     * Penanda segmen SponsorBlock (v1.0.3): posisi FRAKSI 0f..1f —
     * digambar sebagai tick kecil melintasi gelombang, SETELAH
     * clipRect isian (tidak pernah menggeser batas progress).
     * Kosong = tidak ada penanda; pemanggil lain tidak berubah.
     */
    markers: List<Float> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val flowing = playing && !reducedMotion
    // v1.10.7 (animasi mengalir — permintaan user): fase gelombang
    // BERGERAK lembut selama lagu BERMAIN dan MEMBEKU saat pause.
    // Fase digerakkan loop frame-time yang hanya berjalan saat
    // `flowing` true — saat pause loop dibatalkan dan nilai fase
    // TERAHAN, jadi pola tidak melompat saat lanjut main. Siklus
    // penuh 2π = 2× waveFlowMs (selaras dekorasi gelombang).
    // Sinkronisasi TIDAK dikorbankan: batas bagian terisi SELALU
    // clipRect di width * progress — fase hanya menggeser pola
    // sinus di dalam clip, tidak pernah menggeser batas x-nya;
    // knob tetap di x = width * frac dan mengikuti fase di sumbu y.
    // (v1.10.6 mengunci fase 0 karena ripple menggeser pola; user
    // 22 Nov 14:16 meminta animasi kembali dengan syarat batas
    // progress tetap eksak — clipRect menjaminnya.)
    var phase by remember { mutableStateOf(0f) }
    LaunchedEffect(flowing) {
        if (!flowing) return@LaunchedEffect
        var lastNanos = withFrameNanos { it }
        val periodMs = (Motion.waveFlowMs * 2).toFloat()
        while (true) {
            val nowNanos = withFrameNanos { it }
            val dtMs = (nowNanos - lastNanos) / 1_000_000f
            lastNanos = nowNanos
            phase = (phase + dtMs / periodMs * (2 * PI).toFloat()) % (2 * PI).toFloat()
        }
    }
    val phaseValue = phase
    // Knob berdenyut (skala 1↔1,15, 900ms, spec §5.6) — hanya saat jalan.
    val knobPulse = if (flowing) {
        val pulseTransition = rememberInfiniteTransition(label = "knob-pulse")
        pulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "knob-pulse-scale",
        ).value
    } else {
        1f
    }
    val density = LocalDensity.current
    // v1.10.24: amplitudo responsif (hanya pemanggil opt-in) —
    // animasi 450ms FastOutSlowIn; reduced-motion = satu amplitudo
    // statis (DetailMotionSpec.waveAmplitudeDp).
    val waveTokens = CurrentTokens
    val animatedAmplitude by animateDpAsState(
        targetValue = if (responsiveAmplitude) {
            DetailMotionSpec.waveAmplitudeDp(playing, reducedMotion).dp
        } else {
            amplitude
        },
        animationSpec = tween(
            durationMillis = waveTokens.waveAmpMs,
            easing = themeEasing(waveTokens),
        ),
        label = "waveAmplitude",
    )
    val amplitudePx = with(density) { animatedAmplitude.toPx() }
    val strokePx = with(density) { strokeWidth.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (onSeek != null) {
                    Modifier.pointerInput(Unit) {
                        // Tap + seret dalam satu gesture: tekan langsung
                        // seek, geser sambil menahan terus seek, lepas
                        // selesai. Event di-consume supaya klik induk
                        // (mis. mini player) tidak ikut terpanggil.
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            onSeek((down.position.x / size.width).coerceIn(0f, 1f))
                            down.consume()
                            var current = down
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == current.id }
                                    ?: break
                                if (!change.pressed) break
                                onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                                change.consume()
                                current = change
                            }
                            // Jari terangkat — pemanggil boleh memberi
                            // feedback (haptic) bahwa seek selesai.
                            onSeekEnd?.invoke()
                        }
                    }
                } else Modifier,
            ),
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val wavelength = 48.dp.toPx()
        val frac = progress.coerceIn(0f, 1f)

        fun wavePath(colorPhase: Float): Path {
            val path = Path()
            var x = 0f
            var first = true
            while (x <= width) {
                val y = centerY + amplitudePx *
                    sin((2 * PI * x / wavelength).toFloat() + phaseValue + colorPhase).toFloat()
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 4f
            }
            return path
        }

        val style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Track (redup) — gelombang penuh.
        drawPath(wavePath(0f), waveColorA.copy(alpha = trackAlpha), style = style)
        drawPath(wavePath(PI.toFloat()), waveColorB.copy(alpha = trackAlpha), style = style)
        // Isian (jelas) — dibatasi sampai progress.
        clipRect(right = width * frac) {
            drawPath(wavePath(0f), waveColorA, style = style)
            drawPath(wavePath(PI.toFloat()), waveColorB, style = style)
        }
        // v1.0.3: tick penanda segmen (SponsorBlock) — di atas
        // gelombang, tidak kena clipRect isian.
        for (marker in markers) {
            val m = marker.coerceIn(0f, 1f)
            val x = width * m
            val tickHalf = amplitudePx + 3.dp.toPx()
            drawLine(
                color = waveColorB,
                start = Offset(x, centerY - tickHalf),
                end = Offset(x, centerY + tickHalf),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        // Knob putih 12dp (diameter, radius 6dp) + halo lembut — spec §5.3.
        // Radius dikali denyut knob saat lagu jalan.
        if (showKnob && frac > 0f && frac < 1f) {
            val knobX = width * frac
            val knobY = centerY + amplitudePx *
                sin((2 * PI * knobX / wavelength).toFloat() + phaseValue).toFloat()
            drawCircle(
                color = Color.Black.copy(alpha = 0.22f),
                radius = 8.dp.toPx() * knobPulse,
                center = Offset(knobX, knobY + 1.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = 10.dp.toPx() * knobPulse,
                center = Offset(knobX, knobY),
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx() * knobPulse,
                center = Offset(knobX, knobY),
            )
        }
    }
}

/**
 * Hiasan Garis Arus (non-interaktif) untuk Kartu Poster — gelombang
 * mengalir pelan, tanpa knob dan tanpa progress.
 */
@Composable
fun ArusWaveDecoration(
    playing: Boolean,
    modifier: Modifier = Modifier,
    amplitude: Dp = 6.dp,
) {
    val reducedMotion = rememberReducedMotion()
    val animated = playing && !reducedMotion
    // Transisi fase HANYA dibuat saat mengalir (lihat ArusWaveProgress).
    val phaseValue = if (animated) {
        val transition = rememberInfiniteTransition(label = "arus-decor")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                tween(Motion.waveFlowMs * 2, easing = LinearEasing),
            ),
            label = "arus-decor-phase",
        ).value
    } else {
        0f
    }
    val density = LocalDensity.current
    val amplitudePx = with(density) { amplitude.toPx() }

    Canvas(modifier = modifier.fillMaxWidth().height(24.dp)) {
        val width = size.width
        val centerY = size.height / 2f
        val wavelength = 48.dp.toPx()
        val style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun wavePath(colorPhase: Float): Path {
            val path = Path()
            var x = 0f
            var first = true
            while (x <= width) {
                val y = centerY + amplitudePx *
                    sin((2 * PI * x / wavelength).toFloat() + phaseValue + colorPhase).toFloat()
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 4f
            }
            return path
        }
        drawPath(wavePath(0f), WaveGreen.copy(alpha = 0.85f), style = style)
        drawPath(wavePath(PI.toFloat()), WaveRed.copy(alpha = 0.85f), style = style)
    }
}

/**
 * Hiasan Garis Arus di Kartu Poster (v1.7, dari screenshot Beranda):
 * anyaman HIJAU + KREM di kiri bawah poster — bukan hijau/merah.
 * Dua gelombang sinus berlawanan fase, mengalir pelan saat lagu
 * jalan, diam saat pause / reduced-motion. Tanpa knob, tanpa progress.
 */
@Composable
fun PosterWaveDecoration(
    playing: Boolean,
    modifier: Modifier = Modifier,
    amplitude: Dp = 5.dp,
) {
    val reducedMotion = rememberReducedMotion()
    val animated = playing && !reducedMotion
    val phaseValue = if (animated) {
        val transition = rememberInfiniteTransition(label = "poster-wave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                tween(Motion.waveFlowMs * 2, easing = LinearEasing),
            ),
            label = "poster-wave-phase",
        ).value
    } else {
        0f
    }
    val density = LocalDensity.current
    val amplitudePx = with(density) { amplitude.toPx() }
    Canvas(modifier = modifier.fillMaxWidth().height(22.dp)) {
        val width = size.width
        val centerY = size.height / 2f
        val wavelength = 44.dp.toPx()
        val style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun wavePath(colorPhase: Float): Path {
            val path = Path()
            var x = 0f
            var first = true
            while (x <= width) {
                val y = centerY + amplitudePx *
                    sin((2 * PI * x / wavelength).toFloat() + phaseValue + colorPhase).toFloat()
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 4f
            }
            return path
        }
        drawPath(wavePath(0f), WaveGreen.copy(alpha = 0.95f), style = style)
        drawPath(wavePath(PI.toFloat()), WaveCream.copy(alpha = 0.95f), style = style)
    }
}

/**
 * Rata-rata RGB artwork 64×64 (sampling step 4, piksel transparan
 * dilewati) lewat singleton Coil yang sama dengan CoverArt — tidak ada
 * unduhan kedua. Null = artwork kosong/gagal. Dipanggil di IO.
 */
private suspend fun extractArtworkAverageColor(
    context: android.content.Context,
    imageUrl: String,
): Triple<Int, Int, Int>? {
    val loader = coil.Coil.imageLoader(context)
    val result = loader.execute(
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(64, 64)
            .allowHardware(false)
            .build(),
    )
    val drawable = (result as? SuccessResult)?.drawable as? BitmapDrawable
        ?: return null
    val bmp = drawable.bitmap
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L
    val step = 4
    for (y in 0 until bmp.height step step) {
        for (x in 0 until bmp.width step step) {
            val px = bmp.getPixel(x, y)
            val a = android.graphics.Color.alpha(px)
            if (a < 128) continue
            rSum += android.graphics.Color.red(px)
            gSum += android.graphics.Color.green(px)
            bSum += android.graphics.Color.blue(px)
            count++
        }
    }
    if (count == 0L) return null
    return Triple((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
}

/**
 * Warna dominan artwork — untuk latar detail lagu yang "ketint" warna
 * cover (pola YTM). Diambil dari bitmap cover 64×64 lewat Coil, lalu
 * digelapkan. Fallback netral kalau tidak ada gambar / gagal.
 *
 * v1.10.31: state TANPA kunci imageUrl — warna lagu sebelumnya
 * dipertahankan selama artwork lagu baru dimuat (tanpa kedip ke
 * fallback tiap ganti lagu); gagal total tetap jatuh ke fallback.
 */
@Composable
fun rememberDominantColor(imageUrl: String?, fallback: Color = DetailBgFallback): Color {
    val context = LocalContext.current
    var color by remember { mutableStateOf(fallback) }
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            color = fallback
            return@LaunchedEffect
        }
        val avg = withContext(Dispatchers.IO) {
            runCatching { extractArtworkAverageColor(context, imageUrl) }.getOrNull()
        }
        color = avg?.let { (r, g, b) ->
            // Gelapkan 62% menuju hitam — latar detail harus tetap gelap.
            Color(
                red = (r * 0.38f).toInt(),
                green = (g * 0.38f).toInt(),
                blue = (b * 0.38f).toInt(),
            )
        } ?: fallback
    }
    return color
}

/** Warna panel lembar detail turunan artwork: panel + mini-header, satu keluarga. */
data class DetailPanelColors(val panel: Color, val miniHeader: Color)

/**
 * Warna panel detail dari artwork lagu (v1.10.31): hue artwork
 * dipertahankan, value ditahan di pita gelap (lihat ArtworkColors)
 * supaya teks yang warnanya TETAP (DetailTextPrimary/Secondary,
 * DetailAccent) selalu terbaca. Null = tanpa artwork / ekstraksi
 * gagal → pemanggil memakai warna tetap lama (#3D3119/#403517/
 * #473706). Warna lagu sebelumnya dipertahankan selama ekstraksi
 * lagu baru berjalan (tanpa kedip ke fallback); lagu tanpa artwork
 * langsung null (tidak mewarisi warna lagu sebelumnya).
 */
@Composable
fun rememberDetailPanelColors(imageUrl: String?): DetailPanelColors? {
    val context = LocalContext.current
    var avg by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            avg = null
            return@LaunchedEffect
        }
        avg = withContext(Dispatchers.IO) {
            runCatching { extractArtworkAverageColor(context, imageUrl) }.getOrNull()
        }
    }
    val a = avg ?: return null
    val panelArgb = derivePanelColor(
        (0xFF shl 24) or (a.first shl 16) or (a.second shl 8) or a.third,
    )
    return DetailPanelColors(
        panel = Color(panelArgb),
        miniHeader = Color(deriveMiniHeaderColor(panelArgb)),
    )
}

/** Sepasang warna anyaman Garis Arus: strand A (fase 0) + strand B (fase π). */
data class WavePalette(val colorA: Color, val colorB: Color)

/** Gelombang tanpa artwork: MERAH + HIJAU MUDA (v1.10.6, koreksi user). */
val WaveFallbackPalette = WavePalette(WaveLightGreen, WaveRed)

/**
 * Palet gelombang Garis Arus dari ARTWORK lagu (v1.10.6).
 *
 * Dua warna diekstrak dari bitmap artwork 64×64 yang SUDAH dimuat
 * app (singleton Coil yang sama dengan CoverArt — tidak ada unduhan
 * kedua): warna dominan + warna pendamping yang hue-nya berjarak
 * (harmonis, bukan kembar), lalu dinormalisasi ke saturasi/value
 * menengah supaya terbaca di strip terang maupun latar detail
 * gelap. Ini proksi suasana dari artwork — BUKAN analisis audio /
 * mood detection (tidak ada).
 *
 * Transisi HALUS: warna target dianimasikan 600ms; selama artwork
 * lagu baru masih dimuat, warna lagu sebelumnya dipertahankan
 * (tanpa kedip ke fallback); bila artwork kosong/gagal total,
 * target = fallback merah + hijau muda. Tanpa artwork (lagu lokal
 * tanpa cover / URL kosong) langsung fallback.
 */
@Composable
fun rememberWavePalette(imageUrl: String?): WavePalette {
    val context = LocalContext.current
    var target by remember { mutableStateOf(WaveFallbackPalette) }
    val animatedA by animateColorAsState(
        targetValue = target.colorA,
        animationSpec = tween(600),
        label = "waveA",
    )
    val animatedB by animateColorAsState(
        targetValue = target.colorB,
        animationSpec = tween(600),
        label = "waveB",
    )
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            target = WaveFallbackPalette
            return@LaunchedEffect
        }
        val extracted = withContext(Dispatchers.IO) {
            runCatching { extractWavePalette(context, imageUrl) }.getOrNull()
        }
        // Gagal total (artwork rusak/offline): fallback — transisi
        // animasi yang membuatnya halus, bukan kedipan.
        target = extracted ?: WaveFallbackPalette
    }
    return WavePalette(animatedA, animatedB)
}

/** Kuantisasi palet 2 warna dari bitmap artwork (jalan di IO). Null = gagal. */
private suspend fun extractWavePalette(context: android.content.Context, imageUrl: String): WavePalette? {
    val loader = coil.Coil.imageLoader(context)
    val result = loader.execute(
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(64, 64)
            .allowHardware(false)
            .build(),
    )
    val drawable = (result as? SuccessResult)?.drawable as? BitmapDrawable
        ?: return null
    val bmp = drawable.bitmap
    // Hitung bucket warna 4-bit per kanal.
    val counts = HashMap<Int, Int>()
    val step = 2
    for (y in 0 until bmp.height step step) {
        for (x in 0 until bmp.width step step) {
            val px = bmp.getPixel(x, y)
            if (android.graphics.Color.alpha(px) < 128) continue
            val key = (android.graphics.Color.red(px) shr 4 shl 8) or
                (android.graphics.Color.green(px) shr 4 shl 4) or
                (android.graphics.Color.blue(px) shr 4)
            counts[key] = (counts[key] ?: 0) + 1
        }
    }
    if (counts.isEmpty()) return null
    val ranked = counts.entries.sortedByDescending { it.value }
    val dominantKey = ranked.first().key
    val dominantHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(bucketCenter(dominantKey), dominantHsv)
    // Pendamping: bucket terbanyak yang hue-nya berjarak ≥50° dari
    // dominan; kalau tidak ada, bucket terbanyak kedua.
    val companionKey = ranked.firstOrNull { entry ->
        if (entry.key == dominantKey) return@firstOrNull false
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(bucketCenter(entry.key), hsv)
        var diff = kotlin.math.abs(hsv[0] - dominantHsv[0]) % 360f
        if (diff > 180f) diff = 360f - diff
        diff >= 50f
    }?.key ?: ranked.getOrNull(1)?.key ?: dominantKey
    return WavePalette(
        colorA = normalizePaletteColor(bucketCenter(dominantKey)),
        colorB = normalizePaletteColor(bucketCenter(companionKey)),
    )
}

/** Tengah bucket 4-bit → warna ARGB (alpha penuh). */
private fun bucketCenter(key: Int): Int {
    val r = ((key shr 8) and 0xF) * 16 + 8
    val g = ((key shr 4) and 0xF) * 16 + 8
    val b = (key and 0xF) * 16 + 8
    return android.graphics.Color.rgb(r, g, b)
}

/**
 * Normalisasi warna palet: saturasi diangkat (≥0,55) dan value
 * dijaga menengah (0,45–0,75) supaya gelombang terbaca di strip
 * kertas terang maupun latar detail gelap.
 */
private fun normalizePaletteColor(argb: Int): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    hsv[1] = hsv[1].coerceAtLeast(0.55f)
    hsv[2] = hsv[2].coerceIn(0.45f, 0.75f)
    val normalized = android.graphics.Color.HSVToColor(hsv)
    return Color(normalized)
}

/**
 * Prefetch artwork lagu-lagu berikutnya ke cache Coil (v1.6) —
 * dipanggil saat lagu berganti, supaya cover lagu berikutnya sudah
 * siap saat gilirannya tiba (tanpa placeholder berkedip). Enqueue
 * tanpa target = hanya mengisi cache, tidak menampilkan apa-apa.
 */
fun prefetchArtwork(context: android.content.Context, urls: List<String?>) {
    val loader = coil.Coil.imageLoader(context)
    for (url in urls) {
        if (url.isNullOrBlank()) continue
        runCatching {
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
            )
        }
    }
}
