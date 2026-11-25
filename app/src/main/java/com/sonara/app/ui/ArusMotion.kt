package com.sonara.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sonara.app.ui.theme.BurntOrange
import com.sonara.app.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * GERAKAN + RESPONS SENTUH (token dari prototype ARUS).
 *
 * Semua animasi di v1.5 menghormati pengaturan animasi sistem:
 * kalau skala durasi animator = 0 (aksesibilitas "hapus animasi"),
 * gerakan mengalir/putar/berjenjang dimatikan — konten langsung tampil.
 * Transisi tak berujung (aliran gelombang, denyut, napas) hanya dibuat
 * saat benar-benar dibutuhkan, supaya tidak ada recomposition 60fps
 * saat layar diam.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Penampilan status bar + navigation bar mengikuti latar layar.
 *
 * Layar terang ARUS (kertas) butuh ikon sistem GELAP; layar gelap
 * (detail lagu) butuh ikon TERANG. Dipakai di detail lagu saja —
 * layar lain memakai default terang. Nilai sebelumnya dikembalikan
 * saat layar ditinggalkan, supaya tidak bocor ke layar lain.
 */
@Composable
fun SystemBarAppearance(darkBackground: Boolean) {
    val view = LocalView.current
    DisposableEffect(darkBackground) {
        var ctx: Context? = view.context
        var activity: Activity? = null
        while (ctx != null) {
            if (ctx is Activity) {
                activity = ctx
                break
            }
            ctx = (ctx as? ContextWrapper)?.baseContext
        }
        val window = activity?.window
        if (window == null) {
            return@DisposableEffect onDispose {}
        }
        val controller = WindowCompat.getInsetsController(window, view)
        val prevStatus = controller.isAppearanceLightStatusBars
        val prevNav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = !darkBackground
        controller.isAppearanceLightNavigationBars = !darkBackground
        onDispose {
            controller.isAppearanceLightStatusBars = prevStatus
            controller.isAppearanceLightNavigationBars = prevNav
        }
    }
}

/**
 * Masuk berjenjang (staggered): konten naik + memudar, tertunda
 * [index] × token stagger. Kalau reduced-motion, langsung tampil.
 * Key harus menyertakan konteks (mis. segmen aktif) supaya ganti
 * filter memutar ulang animasi masuknya.
 */
@Composable
fun StaggerIn(
    index: Int,
    modifier: Modifier = Modifier,
    /** False = tampil langsung tanpa animasi masuk (v1.10.6: dipakai
     *  detail lagu setelah intro pertama, supaya pindah tab instan). */
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    var shown by remember { mutableStateOf(reducedMotion || !enabled) }
    LaunchedEffect(Unit) {
        if (!reducedMotion && enabled) {
            delay(Motion.staggerMs * index.coerceIn(0, 12))
            shown = true
        }
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(Motion.mediumMs)) +
            slideInVertically(tween(Motion.mediumMs, easing = FastOutSlowInEasing)) { it / 4 },
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * Tombol yang "membal" saat ditekan (respons sentuh): skala mengecil
 * sesaat dengan spring, kembali saat dilepas. Setiap ketukan juga
 * memberi getar halus (v1.6) — dihormati pengaturan getar sistem.
 */
@Composable
fun BouncyBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    haptic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bouncy",
    )
    val hapticFeedback = rememberHaptic()
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                if (haptic) hapticFeedback(HapticKind.Tap)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Ikon navigasi yang "meloncat" saat menjadi aktif — skala membal
 * sekali tiap kali [selected] berubah menjadi true.
 */
@Composable
fun JumpIcon(
    selected: Boolean,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected && !reducedMotion) {
            bounce.snapTo(1f)
            bounce.animateTo(
                1.28f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
            bounce.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        } else {
            bounce.snapTo(1f)
        }
    }
    Box(Modifier.scale(bounce.value)) { content() }
}

/**
 * Equalizer mini — 4 batang yang "menari" selama lagu diputar
 * (spec §5.6: 4 batang, 320–620ms).
 * Diam (batang pendek statis) saat pause atau reduced-motion.
 */
@Composable
fun MiniEqualizer(
    playing: Boolean,
    modifier: Modifier = Modifier,
    // v1.10.40: aksen teks (dulu colorScheme.primary/accentFill —
    // pucat di SENJA/GETAR Light).
    color: Color = BurntOrange,
) {
    val reducedMotion = rememberReducedMotion()
    if (!playing || reducedMotion) {
        Row(
            modifier = modifier.height(14.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(4) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "equalizer")
    val bar1 by transition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing)),
        label = "eq1",
    )
    val bar2 by transition.animateFloat(
        initialValue = 12f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(340, easing = FastOutSlowInEasing)),
        label = "eq2",
    )
    val bar3 by transition.animateFloat(
        initialValue = 6f, targetValue = 13f,
        animationSpec = infiniteRepeatable(tween(530, easing = FastOutSlowInEasing)),
        label = "eq3",
    )
    val bar4 by transition.animateFloat(
        initialValue = 9f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(610, easing = FastOutSlowInEasing)),
        label = "eq4",
    )
    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (h in listOf(bar1, bar2, bar3, bar4)) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

/**
 * Pembungkus cover yang berputar seperti piringan selama [playing].
 * Sudut dipertahankan saat pause (tidak melompat ke 0). Diam total
 * kalau reduced-motion.
 */
@Composable
fun SpinningCover(
    playing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(playing, reducedMotion) {
        if (playing && !reducedMotion) {
            while (true) {
                rotation.animateTo(
                    rotation.value + 360f,
                    animationSpec = tween(Motion.spinMs, easing = LinearEasing),
                )
            }
        }
    }
    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

