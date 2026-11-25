package com.sonara.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sonara.app.data.getarSeedFor
import com.sonara.app.data.getarSeedPhase
import kotlin.math.PI
import kotlin.math.sin

/**
 * Field generatif GETAR (v1.10.26) — port ringan dari prototype
 * GETAR: garis-garis aliran dari seed per lagu, mengalir saat lagu
 * jalan, diam saat jeda, statis saat reduced-motion.
 *
 * - Seed deterministik dari id lagu (hashCode): lagu yang sama
 *   selalu punya field yang sama; lagu lain field-nya berbeda.
 * - Animasi HANYA saat [playing] dan bukan reduced-motion: fase
 *   digerakkan Animatable dalam loop; saat jeda animasi berhenti
 *   di fase terakhir (settle, tanpa snap) dan tidak ada
 *   recomposition 60fps.
 * - Garis digambar di Canvas dengan alpha rendah — dekorasi latar,
 *   tidak memengaruhi kontras teks di atasnya.
 * - Warna garis = seed fill di dasar gelap, seed deep di dasar
 *   terang (keduanya dari pasangan seed yang terverifikasi kontras).
 */
@Composable
fun GetarField(
    trackId: String,
    playing: Boolean,
    darkGround: Boolean,
    modifier: Modifier = Modifier,
    lineAlpha: Float = 0.16f,
) {
    val reducedMotion = rememberReducedMotion()
    val seed = remember(trackId) { getarSeedFor(trackId) }
    val seedPhase = remember(trackId) { getarSeedPhase(trackId) }
    val lineColor = if (darkGround) Color(seed.fill) else Color(seed.deep)

    // Fase mengalir hanya saat bermain; diam = nilai terakhir.
    val phase = remember { Animatable(seedPhase) }
    LaunchedEffect(playing, reducedMotion, trackId) {
        if (!playing || reducedMotion) return@LaunchedEffect
        while (true) {
            phase.animateTo(
                targetValue = phase.value + 1f,
                animationSpec = tween(durationMillis = 14_000, easing = LinearEasing),
            )
        }
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val lines = 12
        val phaseNow = if (reducedMotion) seedPhase else phase.value
        for (i in 0 until lines) {
            // Jitter deterministik per garis (LCG dari seed + indeks).
            var rnd = (trackId.hashCode() and 0x7fffffff) + i * 97 + 13
            fun next(): Float {
                rnd = (rnd * 1103515245 + 12345) and 0x7fffffff
                return (rnd % 1000) / 1000f
            }
            val baseY = (i + 0.5f) / lines * h + (next() - 0.5f) * h * 0.05f
            val amp1 = h * 0.045f * (0.7f + next() * 0.6f)
            val amp2 = h * 0.022f * (0.7f + next() * 0.6f)
            val waveLen1 = w * (0.55f + next() * 0.5f)
            val waveLen2 = w * (0.3f + next() * 0.35f)
            val phi = next() * 2f * PI.toFloat()
            val alpha = lineAlpha * (0.45f + next() * 0.55f)
            val path = Path()
            var x = -24f
            var first = true
            while (x <= w + 24f) {
                val t1 = 2f * PI.toFloat() * (x / waveLen1 + phaseNow) + phi
                val t2 = 2f * PI.toFloat() * (x / waveLen2 - phaseNow * 0.7f) + phi * 2f
                val y = baseY + amp1 * sin(t1) + amp2 * sin(t2)
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 24f
            }
            drawPath(
                path = path,
                color = lineColor.copy(alpha = alpha),
                style = Stroke(width = 1.5f * density, cap = StrokeCap.Round),
            )
        }
        // Titik seed — satu dot kecil di kiri atas, penanda seed.
        drawCircle(
            color = lineColor.copy(alpha = lineAlpha * 1.6f),
            radius = 3f * density,
            center = Offset(w * 0.08f, h * 0.12f),
        )
    }
}
