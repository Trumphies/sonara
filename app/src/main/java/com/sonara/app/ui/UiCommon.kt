package com.sonara.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.roundToInt
import com.sonara.app.data.DetailMotionSpec
import com.sonara.app.data.Source
import com.sonara.app.data.Track
import com.sonara.app.data.TrackDurations
import com.sonara.app.data.displayArtist
import com.sonara.app.data.formatDurationMs
import com.sonara.app.data.formatPlayCount
import com.sonara.app.ui.theme.AccentFill
import com.sonara.app.ui.theme.BurntOrange
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.Density
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkFixed
import com.sonara.app.ui.theme.PaperSurface
import com.sonara.app.ui.theme.PosterFallback
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.theme.MonoLetterSpacing
import com.sonara.app.ui.theme.Motion
import com.sonara.app.ui.theme.OnAccent
import com.sonara.app.ui.theme.PosterGold
import com.sonara.app.ui.theme.SegmentBg
import com.sonara.app.ui.theme.Sizes
import com.sonara.app.ui.theme.Spacing
import com.sonara.app.ui.theme.Type
import com.sonara.app.ui.theme.WaveLightGreen
import com.sonara.app.ui.theme.WaveRed
import com.sonara.app.ui.theme.WaveStripProgress
import com.sonara.app.ui.theme.WaveStripTrack
import com.sonara.app.ui.theme.displayCased
import com.sonara.app.ui.theme.sourceDot
import com.sonara.app.ui.theme.themeEasing

/** Margin konten layar terang — diukur dari screenshot (≈19dp). */
val ContentMargin = 19.dp

/**
 * Bayangan keras Blok (v1.10.27): kotak solid bergeser [offset] di
 * belakang konten — tanpa blur, seperti prototype (poster 5px,
 * tile 4px, chip 3px). Dipasang SEBELUM clip() dalam rantai
 * modifier supaya bayangannya tidak terpotong bentuk. Di tema lain
 * tidak menggambar apa-apa.
 */
@Composable
fun Modifier.hardShadowDraw(
    corner: Dp,
    offset: Dp,
): Modifier {
    val tokens = CurrentTokens
    if (!tokens.hardShadow || offset <= 0.dp) return this
    return drawBehind {
        val o = offset.toPx()
        val r = corner.toPx()
        drawRoundRect(
            color = tokens.text,
            topLeft = Offset(o, o),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(r, r),
        )
    }
}

/**
 * Glow lembut Senja (v1.10.27): bayangan aksen di sekeliling elemen
 * yang sedang jalan (baris lagu aktif, artwork detail, mini player).
 * Hanya Senja + [active]; tema lain tidak berubah.
 */
@Composable
fun Modifier.senjaGlow(
    shape: androidx.compose.ui.graphics.Shape,
    active: Boolean,
): Modifier {
    val tokens = CurrentTokens
    return if (active && tokens.glowElevation > 0.dp) {
        this.shadow(
            elevation = tokens.glowElevation,
            shape = shape,
            ambientColor = tokens.accent.copy(alpha = tokens.glowAlpha),
            spotColor = tokens.accent.copy(alpha = tokens.glowAlpha),
        )
    } else {
        this
    }
}

/**
 * Treatment v1.10.37 (RASI / PASAR / SENYAP). Semua helper di bawah
 * no-op di tema lain (flag token mati) — pola yang sama dengan
 * [hardShadowDraw]/[senjaGlow]: bentuk dibaca dari token, bukan dari
 * literal di composable.
 */

/**
 * Latar dot-grid RASI (prototype: radial-gradient 22px): titik-titik
 * kecil warna teks 10% di belakang konten. Dipasang di Box konten
 * root (SonaraApp) — layar tidak melukis latar opak sendiri.
 */
@Composable
fun Modifier.dotGridBackground(): Modifier {
    val tokens = CurrentTokens
    if (!tokens.dotGridBackground) return this
    val dotColor = tokens.text.copy(alpha = 0.10f)
    return drawBehind {
        val step = 22.dp.toPx()
        var y = step / 2f
        while (y < size.height) {
            var x = step / 2f
            while (x < size.width) {
                drawCircle(color = dotColor, radius = 1.6f, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

/**
 * Garis konektor konstelasi RASI: satu garis vertikal tipis di tengah
 * kolom cover, digambar di belakang baris. Baris yang menumpuk
 * membentuk garis bersambung antar cover bundar (prototype: garis
 * 1px di belakang feed).
 */
@Composable
fun Modifier.constellationLine(): Modifier {
    val tokens = CurrentTokens
    if (!tokens.constellationLines) return this
    val lineColor = tokens.outline
    return drawBehind {
        val x = ContentMargin.toPx() + Sizes.rowCover.toPx() / 2f
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

/** Border putus-putus PASAR (stub tiket): garis 8dp + celah 6dp. */
fun Modifier.dashedBorder(width: Dp, color: Color, corner: Dp): Modifier = drawBehind {
    val w = width.toPx()
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = w / 2f,
                top = w / 2f,
                right = size.width - w / 2f,
                bottom = size.height - w / 2f,
                cornerRadius = CornerRadius(corner.toPx(), corner.toPx()),
            ),
        )
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = w,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f),
        ),
    )
}

/** Divider putus-putus PASAR (pengganti Box hairline di TrackRow). */
@Composable
fun DashedDividerLine() {
    val tokens = CurrentTokens
    Box(
        Modifier
            .fillMaxWidth()
            .height(tokens.dividerThickness)
            .drawBehind {
                val y = size.height / 2f
                drawLine(
                    color = tokens.outline.copy(alpha = tokens.dividerAlpha),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                        0f,
                    ),
                )
            },
    )
}

/**
 * Cincin orbit RASI di atas artwork bundar: lingkaran tipis warna
 * aksen tepat di tepi cover + satu titik yang mengorbit 9 detik
 * (linear, seperti prototype). Statis saat reduced-motion. Di mode
 * gelap, lingkaran kedua alpha rendah memberi kesan glow. Overlay —
 * tidak mengubah ukuran cover.
 */
@Composable
fun OrbitRingOverlay() {
    val tokens = CurrentTokens
    if (!tokens.orbitRing) return
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "orbit")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbitAngle",
    )
    val a = if (reducedMotion) 0f else angle
    val accent = tokens.accent
    val isDark = tokens.background.luminance() < 0.5f
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // v1.10.39: semua elemen orbit HARUS tetap di dalam
                // batas cover — sebelumnya titik (r 4dp) berpusat
                // TEPAT di ring sehingga menjorok 3dp ke luar cover
                // dan tampak seperti titik biru liar di atas artwork
                // (screenshot HP user, RASI Dark). Kini: pusat titik
                // digeser ke dalam sebesar jari-jarinya (tepi luar =
                // ring), dan stroke glow digeser ke dalam 2dp supaya
                // tepi luarnya = ring, bukan 1dp di luar cover.
                val ringRadius = minOf(size.width, size.height) / 2f - 1.dp.toPx()
                val dotRadius = 4.dp.toPx()
                if (isDark) {
                    drawCircle(
                        color = accent.copy(alpha = 0.30f),
                        radius = ringRadius - 2.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 4.dp.toPx()),
                    )
                }
                drawCircle(
                    color = accent,
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx()),
                )
                val rad = Math.toRadians(a.toDouble())
                drawCircle(
                    color = accent,
                    radius = dotRadius,
                    center = Offset(
                        cx + (ringRadius - dotRadius) * kotlin.math.cos(rad).toFloat(),
                        cy + (ringRadius - dotRadius) * kotlin.math.sin(rad).toFloat(),
                    ),
                )
            },
    )
}

/**
 * Label mono kapital — teks kecil ber-letter-spacing lebar seperti
 * caption "SEMUA LAGU DARI DUA SUMBER" / "8 LAGU" di screenshot.
 * Font + spacing mengikuti tema (GETAR = JetBrains Mono).
 */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = InkMuted,
    fontSize: TextUnit = Type.sourceLabel,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        fontFamily = CurrentTokens.labelFont,
        fontSize = fontSize,
        letterSpacing = CurrentTokens.labelLetterSpacing,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}



/**
 * Cover art: gambar asli kalau ada. Kalau tidak ada, placeholder
 * persis screenshot: gold #E8BA0F + lingkaran vinyl hitam terpotong
 * tepi kanan atas + judul putih di kiri bawah.
 */
@Composable
fun CoverArt(
    track: Track,
    size: Dp,
    corner: Dp = Corners.small,
    letterSize: TextUnit = Type.trackTitleLarge,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val url = track.imageUrl
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(PosterGold),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            // Vinyl hitam terpotong tepi kanan atas.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = size * 0.16f, y = -size * 0.14f)
                    .size(size * 0.44f)
                    .clip(CircleShape)
                    .background(InkFixed),
            )
            // Judul putih di kiri bawah.
            Text(
                text = track.title,
                fontSize = (size.value * 0.22f).coerceIn(9f, 22f).sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(400)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** Tile huruf untuk playlist (tanpa gambar). */
@Composable
fun PlaylistCover(
    name: String,
    size: Dp = Sizes.playlistCover,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(Corners.rowCover))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "M",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

/**
 * Baris track persis screenshot — cover 54dp radius 14dp, judul
 * Bold 17sp, "Artis · Album" redup (maks 2 baris), kanan: titik
 * sumber + durasi. Divider hairline antar baris.
 * Lagu yang sedang diputar: equalizer mini di samping judul
 * (perilaku v1.5/v1.6 dipertahankan).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    onMenu: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    /** v1.10.21: baris terpilih di mode multi-select (tint + centang). */
    selected: Boolean = false,
    animateIn: Boolean = false,
    index: Int = 0,
    trailing: (@Composable () -> Unit)? = null,
    sources: Set<Source> = setOf(track.source),
    showDivider: Boolean = true,
    modifier: Modifier = Modifier,
    /**
     * Durasi per videoId (v1.10.31): jalur feed tidak membawa durasi,
     * jadi baris yang tampil meminta durasinya dan menampilkan hasil
     * cache saat tiba. Null = perilaku lama (durasi bawaan track saja).
     */
    durations: TrackDurations? = null,
    /**
     * Nomor chart SENYAP (v1.10.37): posisi 1-based di daftar induk.
     * Null = pemanggil tidak menghitung posisi → numeral tidak tampil.
     * Hanya dirender bila token chartNumerals aktif.
     */
    chartNumber: Int? = null,
    /**
     * Hitungan putar di bawah subjudul (v1.0.3): hanya pemanggil
     * Search yang mengaktifkannya, dan hanya bila respons YTM
     * benar-benar membawa hitungan (playCount > 0) — tanpa angka
     * karangan.
     */
    showPlayCount: Boolean = false,
) {
    val tokens = CurrentTokens
    // Durasi tampil: bawaan track bila ada, else cache lookupVideo.
    // collectAsState = baris ikut terkomposisi ulang saat durasinya tiba.
    // Tanpa durations, alirannya peta kosong statis (perilaku lama).
    val durationCache by (durations?.cache
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap<String, Long>()) })
        .collectAsState()
    val shownDurationMs = track.durationMs.takeIf { it > 0 }
        ?: (durationCache[track.youtubeVideoId] ?: 0L)
    // Hanya baris yang masuk komposisi yang meminta (visible-rows-only);
    // dedupe + debounce + cooldown hidup di TrackDurations.
    LaunchedEffect(track.youtubeVideoId, durations) {
        if (durations != null && track.durationMs <= 0) {
            durations.request(track.youtubeVideoId)
        }
    }
    val rowContent: @Composable () -> Unit = {
        Column(
            modifier.senjaGlow(
                RoundedCornerShape(Corners.card),
                active = isCurrent && isPlaying,
            ).constellationLine(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .heightIn(min = Density.rowMinHeight)
                    .combinedClickable(
                        onClick = onClick,
                        onClickLabel = "Play ${track.title}",
                        onLongClick = onLongClick ?: onMenu,
                        onLongClickLabel = "Song menu",
                    )
                    .padding(horizontal = ContentMargin, vertical = Density.rowPaddingV),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            // v1.10.40: peran ISI eksplisit (dulu
                            // colorScheme.primary — nilai sama, peran
                            // kini eksplisit agar tak tertukar lagi).
                            .background(AccentFill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            // v1.10.40: OnAccent (dulu Color.White —
                            // 1,69:1 di SENJA Light).
                            tint = OnAccent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                }
                // Numeral chart SENYAP: 01/02/… Fraunces Light di samping
                // cover (prototype). Hanya bila pemanggil memberi nomor.
                if (tokens.chartNumerals && chartNumber != null) {
                    Text(
                        text = chartNumber.toString().padStart(2, '0'),
                        fontFamily = tokens.displayFont,
                        fontWeight = FontWeight.Light,
                        fontSize = 30.sp,
                        color = tokens.text,
                        maxLines = 1,
                        // v1.10.39: lebar minimum, bukan tetap — numeral
                        // 3 digit (100+) terpotong pada width(44.dp).
                        modifier = Modifier.widthIn(min = 44.dp),
                    )
                    Spacer(Modifier.width(Spacing.md))
                }
                CoverArt(track = track, size = Sizes.rowCover, corner = Corners.rowCover)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.title,
                            fontSize = Type.trackTitle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isCurrent) {
                            Spacer(Modifier.width(Spacing.sm))
                            MiniEqualizer(playing = isPlaying)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    val subtitle = run {
                        // v1.10.19: artis label tipe / kosong (data lama)
                        // tidak tampil; album saja bila ada, else kosong.
                        val artistText = displayArtist(track.artist)
                        when {
                            track.album.isNotBlank() && artistText.isNotBlank() ->
                                "$artistText · ${track.album}"
                            track.album.isNotBlank() -> track.album
                            else -> artistText
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = Type.artist,
                        color = InkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // v1.0.3: baris hitungan di bawah subjudul —
                    // hanya di Search, hanya bila hitungannya nyata.
                    if (showPlayCount && track.playCount > 0) {
                        Text(
                            text = formatPlayCount(track.playCount, track.countKind),
                            fontSize = 13.sp,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (s in sources.sortedBy { it.ordinal }) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(sourceDot(s)),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatDurationMs(shownDurationMs),
                        fontSize = 13.sp,
                        color = InkMuted,
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(Spacing.xs))
                    trailing()
                }
            }
            if (showDivider) {
                if (tokens.dashedBorders) {
                    DashedDividerLine()
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(tokens.dividerThickness)
                            .background(
                                if (tokens.dividerAlpha >= 1f) {
                                    MaterialTheme.colorScheme.outline
                                } else {
                                    InkBlack.copy(alpha = tokens.dividerAlpha)
                                },
                            ),
                    )
                }
            }
        }
    }
    if (animateIn) {
        StaggerIn(index = index) { rowContent() }
    } else {
        rowContent()
    }
}

/**
 * Kolom cari persis screenshot Cari — pill PUTIH tinggi 56dp,
 * border gelap 2dp, ikon kaca pembesar, placeholder
 * "Ketik judul atau artis...". Border tetap gelap saat fokus
 * (bukan oranye — screenshot tidak menunjukkan state fokus oranye).
 */
@Composable
fun ArusSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** Dipanggil saat tombol Search keyboard ditekan (v1.10.16: catat riwayat cari). */
    onSearchSubmit: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 2.dp else 2.dp,
        animationSpec = tween(Motion.fastMs),
        label = "searchBorderWidth",
    )
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(Corners.pill))
            .background(PaperSurface)
            .border(borderWidth, InkBlack, RoundedCornerShape(Corners.pill))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = InkBlack,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            if (onClick != null) {
                Text(
                    "Type a title or artist...",
                    fontSize = 17.sp,
                    color = InkMuted,
                )
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearchSubmit?.invoke()
                            focusManager.clearFocus()
                        },
                    ),
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = InkBlack,
                    ),
                    cursorBrush = SolidColor(InkBlack),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                "Type a title or artist...",
                                fontSize = 17.sp,
                                color = InkMuted,
                            )
                        }
                        inner()
                    },
                )
            }
        }
    }
}

/**
 * Chip filter kecil — outline hangat; aktif = isi aksen + teks OnAccent.
 * Bentuk mengikuti tema (Blok = kotak 8dp + bayangan keras 3dp).
 */
@Composable
fun FilterChipYtm(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = CurrentTokens
    val chipShape = RoundedCornerShape(Corners.chip)
    Box(
        modifier = modifier
            .height(32.dp)
            .hardShadowDraw(Corners.chip, tokens.hardShadowChip)
            .clip(chipShape)
            .then(
                // v1.10.40: peran ISI eksplisit (dulu colorScheme.primary).
                if (selected) Modifier.background(AccentFill)
                else Modifier.border(
                    tokens.cardBorderWidth,
                    MaterialTheme.colorScheme.outline,
                    chipShape,
                ),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) OnAccent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Tombol Back ARUS (v1.10.1) — dipakai di kiri atas SEMUA layar:
 * Beranda, Cari, Koleksi, Pengaturan, detail lagu (penuh + header
 * mini), antrean, laporan crash, dan sheet menu (di sana = tutup
 * sheet). Area sentuh 48dp, label aksesibilitas "Kembali", panah
 * 24dp. Varian terang = lingkaran putih ber-outline; varian gelap
 * (detail/sheet) = lingkaran putih tembus pandang di atas
 * background gelap.
 *
 * v1.10.34 (AKAR MASALAH — laporan HP 20:58 WIB, Settings ARUS
 * Dark): panah varian terang memakai InkBlack (token TEKS yang
 * membalik antar mode) di atas lingkaran putih PATEN — di 4 varian
 * yang teksnya terang (ARUS Dark 1.05:1, SENJA Light 1.14:1, BLOK
 * Dark 1.12:1, GETAR Light 1.15:1) panahnya praktis tak kelihatan.
 * Kelas cacat yang sama dengan label nav 10.32. Perbaikan: panah
 * memakai InkFixed (hitam pekat TETAP, 17.70:1 di atas putih di
 * semua varian) — lingkaran tidak berubah, ukuran/bentuk tidak
 * berubah. Varian gelap tidak berubah (putih di atas tembus pandang
 * gelap: 9.60–13.32:1).
 */
@Composable
fun ArusBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (dark) Color.White.copy(alpha = 0.14f) else Color.White,
            )
            .border(
                1.dp,
                if (dark) Color.White.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                CircleShape,
            ),
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            // v1.10.34: InkFixed, bukan InkBlack — lihat KDoc di atas.
            tint = if (dark) Color.White else InkFixed,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Warna peran TEKS aksen untuk TextButton + field teks (v1.10.40).
 * TextButton bawaan Material3 memakai `primary` (= accentFill, peran
 * ISI) sebagai warna teks — 1,41:1 di SENJA Light, 1,77:1 di GETAR
 * Light. Peran teks yang benar adalah [BurntOrange] (aksen): >= 3,0
 * di semua varian (bar aksen/interaktif 10.32).
 */
@Composable
fun sonaraTextButtonColors() =
    ButtonDefaults.textButtonColors(contentColor = BurntOrange)

/**
 * Warna field teks (v1.10.40): border/label fokus memakai aksen teks
 * [BurntOrange] (bukan `primary`/accentFill — alasan sama dengan
 * [sonaraTextButtonColors]); sisanya default skema (onSurfaceVariant
 * / outline) supaya perilaku unfocused tidak berubah.
 */
@Composable
fun sonaraTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BurntOrange,
        focusedLabelColor = BurntOrange,
        focusedLeadingIconColor = BurntOrange,
        focusedTrailingIconColor = BurntOrange,
        cursorColor = BurntOrange,
    )

/** Tombol utama — tinggi 56dp, bentuk tema, isi aksen + teks OnAccent. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(Corners.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentFill,
            // v1.10.40: OnAccent (dulu Color.White hardcoded — 1,69:1
            // di SENJA Light; OnAccent >= 4,99 di semua varian).
            contentColor = OnAccent,
        ),
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Tombol sekunder — tinggi 48dp, bentuk tema, permukaan hangat + outline. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        // v1.10.2: heightIn (bukan height tetap) — label dua baris
        // ("Sinkronkan sekarang" di setengah lebar HP kecil) tumbuh
        // dan tampil utuh, bukan terpotong. Satu baris tetap 48dp.
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(Corners.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Header seksi persis screenshot — judul Black kiri + label mono
 * kanan ("8 LAGU" / "TAP BUAT MUTER"). Ukuran + casing + spacing
 * mengikuti tema (Blok 32sp kapital, Senja 26sp rapat, GETAR 22sp).
 */
@Composable
fun SectionHeader(
    title: String,
    rightText: String,
    modifier: Modifier = Modifier,
) {
    val tokens = CurrentTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ContentMargin,
                vertical = Density.sectionPaddingV,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayCased(title, tokens),
            fontSize = tokens.sectionTitleSize,
            fontFamily = tokens.displayFont,
            letterSpacing = tokens.displayLetterSpacing,
            fontWeight = FontWeight.Black,
            color = InkBlack,
            modifier = Modifier.weight(1f),
        )
        MonoLabel(text = rightText)
    }
}

/**
 * Judul seksi — 24sp bold; dipakai layar Pengaturan. Font + casing
 * mengikuti tema; jarak atas mengikuti densitas tema.
 */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    /** Ikon seksi opsional (v1.10.6, redesign Settings). Null = teks saja seperti semula. */
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val tokens = CurrentTokens
    if (icon == null) {
        Text(
            text = displayCased(text, tokens),
            fontSize = Type.screenTitle,
            fontFamily = tokens.displayFont,
            letterSpacing = tokens.displayLetterSpacing,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Density.sectionTopPadding,
                bottom = Density.sectionPaddingV,
            ),
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Density.sectionTopPadding,
                bottom = Density.sectionPaddingV,
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = displayCased(text, tokens),
                fontSize = Type.screenTitle,
                fontFamily = tokens.displayFont,
                letterSpacing = tokens.displayLetterSpacing,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Empty state jujur — ikon netral 64dp, judul, penjelasan, aksi opsional. */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = subtitle,
            fontSize = Type.artist,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            SecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

/**
 * Skeleton loading — bentuk kasar layar yang akan muncul (poster +
 * baris lagu), dengan denyut lembut.
 */
@Composable
private fun skeletonAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    return transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    ).value
}

@Composable
private fun SkeletonBar(
    alpha: Float,
    widthFraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(Corners.small))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
    )
}

@Composable
fun SkeletonTrackRow(alpha: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Density.rowMinHeight)
            .padding(horizontal = ContentMargin, vertical = Density.rowPaddingV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(Sizes.rowCover)
                .clip(RoundedCornerShape(Corners.rowCover))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            SkeletonBar(alpha = alpha, widthFraction = 0.62f, height = 14.dp)
            Spacer(Modifier.height(6.dp))
            SkeletonBar(alpha = alpha, widthFraction = 0.42f, height = 11.dp)
        }
        Spacer(Modifier.width(Spacing.sm))
        SkeletonBar(alpha = alpha, widthFraction = 1f, height = 11.dp, modifier = Modifier.width(36.dp))
    }
}

/** Daftar skeleton — baris-baris lagu berdenyut. */
@Composable
fun SkeletonRows(count: Int = 6, modifier: Modifier = Modifier) {
    val alpha = skeletonAlpha()
    Column(modifier) {
        repeat(count) { SkeletonTrackRow(alpha = alpha) }
    }
}

/** Skeleton Beranda — poster + baris lagu, bentuknya sama dengan isi asli. */
@Composable
fun SkeletonHome(modifier: Modifier = Modifier) {
    val alpha = skeletonAlpha()
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentMargin, vertical = Spacing.sm)
                .height(Sizes.posterHeight)
                .clip(RoundedCornerShape(Corners.poster))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
        )
        repeat(6) { SkeletonTrackRow(alpha = alpha) }
    }
}

/**
 * Ikon play/pause yang BERMORF (v1.10.24): ikon lama memudar +
 * mengecil, ikon baru membesar + memudar masuk — 150ms
 * FastOutSlowIn. Dipakai tombol play besar detail + tombol play
 * mini-header. Reduced-motion: ganti ikon instan (transisi state,
 * bukan gerak dekoratif — tapi tanpa morph).
 */
@Composable
fun MorphPlayPauseIcon(
    isPlaying: Boolean,
    tint: Color,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val tokens = CurrentTokens
    val morphMs = tokens.morphMs
    val easing = themeEasing(tokens)
    if (reducedMotion) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = tint,
            modifier = modifier.size(iconSize),
        )
        return
    }
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn(tween(morphMs, easing = easing)) +
                scaleIn(
                    tween(morphMs, easing = easing),
                    initialScale = 0.7f,
                )) togetherWith
                (fadeOut(tween(morphMs, easing = easing)) +
                    scaleOut(
                        tween(morphMs, easing = easing),
                        targetScale = 0.7f,
                    ))
        },
        label = "playPauseMorph",
        modifier = modifier,
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Tombol play bundar PUTIH (detail lagu) — 72dp, ikon gelap 36dp.
 * "Membal" saat ditekan (respons sentuh). Ikon bermorf 150ms
 * (v1.10.24) — tanpa key() di call site supaya morph beranimasi,
 * bukan restart komposisi.
 */
@Composable
fun BigPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BouncyBox(
        onClick = onClick,
        modifier = modifier
            .size(Sizes.playButton)
            .clip(CircleShape)
            .background(Color.White),
    ) {
        MorphPlayPauseIcon(
            isPlaying = isPlaying,
            tint = Color.Black,
            iconSize = Sizes.playButtonIcon,
        )
    }
}

/**
 * Kartu Poster editorial persis screenshot Beranda — gold #E8BA0F
 * (atau artwork ASLI lagu bila ada), radius 28dp, tinggi 204dp:
 * pill gelap "▮▮▮ PILIHAN HARI INI" kiri atas, lingkaran vinyl
 * hitam kanan atas terpotong tepi (hanya bila tanpa artwork),
 * judul PUTIH Black sangat besar, "Artis — Album" putih 70%,
 * Garis Arus hijau+krem kiri bawah, tombol play PUTIH 64dp
 * kanan bawah. Ketuk kartu = putar lagu.
 */
@Composable
fun PosterCard(
    track: Track,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasArtwork = !track.imageUrl.isNullOrBlank()
    val tokens = CurrentTokens
    val posterShape = RoundedCornerShape(Corners.poster)
    Box(
        modifier
            .fillMaxWidth()
            .height(Sizes.posterHeight),
    ) {
        // Blok: bayangan keras di belakang kartu (prototype: 5px).
        if (tokens.hardShadow) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(tokens.hardShadowPoster, tokens.hardShadowPoster)
                    .clip(posterShape)
                    .background(tokens.text),
            )
        }
    BouncyBox(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize()
            .clip(posterShape)
            .then(
                when {
                    // PASAR: border putus-putus stub tiket.
                    tokens.dashedBorders -> Modifier.dashedBorder(2.dp, tokens.text, Corners.poster)
                    tokens.hardShadow -> Modifier.border(2.dp, tokens.text, posterShape)
                    else -> Modifier
                },
            ),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (hasArtwork) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.imageUrl)
                        .crossfade(400)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                // Scrim bawah supaya judul putih terbaca di atas artwork.
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f),
                                ),
                            ),
                        ),
                )
            } else {
                // SENYAP: tanpa isi kartu — latar transparan (layar yang
                // terlihat), tanpa vinyl; judul memakai warna teks tema.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (tokens.flatNoCards) Color.Transparent else PosterFallback),
                )
                // GETAR: field generatif seeded lagu di atas latar poster.
                if (tokens.flowField) {
                    GetarField(
                        trackId = track.id,
                        playing = playing,
                        darkGround = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Lingkaran vinyl hitam besar, terpotong tepi kartu.
                // SENYAP tidak memakai hiasan ini (tanpa kartu).
                if (!tokens.flatNoCards) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 28.dp, y = (-28).dp)
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(InkFixed),
                    )
                }
            }
            // Label "TODAY'S PICK": pill bawaan; PASAR = stiker kuning
            // miring; SENYAP = teks polos bergaris bawah, tanpa pill.
            // Di atas artwork tanpa scrim, teks SENYAP tetap putih;
            // tanpa artwork (latar transparan) memakai warna teks tema.
            val pickLabelColor = if (tokens.flatNoCards && !hasArtwork) tokens.text else Color.White
            if (tokens.stampBadge) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .graphicsLayer { rotationZ = -4f }
                        .background(tokens.avatar)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "TODAY'S PICK",
                        fontFamily = tokens.labelFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = tokens.labelLetterSpacing,
                        color = tokens.onAvatar,
                        maxLines = 1,
                    )
                }
                // Stempel bundar "PICK" (hanya di atas artwork — tanpa
                // artwork, lingkaran vinyl menempati sudut itu).
                if (hasArtwork) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .graphicsLayer { rotationZ = 12f }
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(2.dp, tokens.accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "PICK",
                            fontFamily = tokens.labelFont,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = tokens.labelLetterSpacing,
                            color = tokens.accent,
                            maxLines = 1,
                        )
                    }
                }
            } else if (tokens.flatNoCards) {
                Text(
                    text = "TODAY'S PICK",
                    fontFamily = tokens.labelFont,
                    fontSize = 11.sp,
                    letterSpacing = tokens.labelLetterSpacing,
                    color = pickLabelColor,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                )
            } else {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(Corners.pill))
                        .background(Color(0xFF574A12).copy(alpha = 0.85f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) {
                        Box(
                            Modifier
                                .size(width = 4.dp, height = 11.dp)
                                .background(Color.White),
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "TODAY'S PICK",
                        fontFamily = tokens.labelFont,
                        fontSize = 11.sp,
                        letterSpacing = tokens.labelLetterSpacing,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
            // Judul + artis + Garis Arus kiri bawah.
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 96.dp, bottom = 16.dp),
            ) {
                Text(
                    // v1.10.39: judul lagu TIDAK pernah di-caps
                    // (aturan displayCased) — sama seperti judul
                    // detail; hanya judul display/seksi yang boleh.
                    text = track.title,
                    fontSize = tokens.posterTitleSize,
                    fontFamily = tokens.displayFont,
                    letterSpacing = tokens.displayLetterSpacing,
                    fontWeight = FontWeight.Black,
                    // SENYAP tanpa artwork: judul warna teks tema di atas
                    // latar transparan; selebihnya putih (scrim/fallback).
                    color = if (tokens.flatNoCards && !hasArtwork) tokens.text else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = run {
                    // v1.10.19: sama seperti TrackRow (label tipe → kosong).
                    val artistText = displayArtist(track.artist)
                    when {
                        track.album.isNotBlank() && artistText.isNotBlank() ->
                            "$artistText — ${track.album}"
                        track.album.isNotBlank() -> track.album
                        else -> artistText
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    // SENYAP tanpa artwork: subjudul redup tema di atas
                    // latar transparan; selebihnya putih 70%.
                    color = if (tokens.flatNoCards && !hasArtwork) InkMuted else Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                PosterWaveDecoration(
                    playing = playing,
                    modifier = Modifier.width(200.dp),
                )
            }
            // Tombol play bulat kanan bawah. SENYAP: lingkaran tinta +
            // ikon warna latar (tanpa kartu putih); selebihnya putih/hitam.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(Sizes.posterPlayButton)
                    .clip(CircleShape)
                    .background(if (tokens.flatNoCards) tokens.text else Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = if (tokens.flatNoCards) tokens.background else Color.Black,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
        // PASAR: notch stub tiket — dua lingkaran warna latar layar di
        // tengah tepi kiri/kanan kartu, melubangi tepi border putus-putus.
        if (tokens.ticketNotches) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-9).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(tokens.background),
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 9.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(tokens.background),
            )
        }
    }
}

/**
 * Pembungkus swipe-hapus (v1.10.16): geser baris ke kiri
 * (end-to-start) sampai ambang → baris meluncur keluar, lalu
 * [onDismissed] dipanggil (pemanggil menghapus item dari daftar;
 * baris keluar dari komposisi bersama state dismiss-nya, sehingga
 * Undo menyisipkan baris segar). Latar merah + ikon hapus HANYA
 * tampil SELAMA swipe berlangsung (v1.10.19, bug P0: background
 * yang selalu ter-composed membuat baris Up Next/Queue berlatar
 * merah penuh + ikon menimpa durasi meski tidak digeser, karena
 * konten baris berlatar transparan). Gate memakai
 * `dismissState.dismissDirection`: Settled ⟺ offset == 0f (di dalam
 * state — terverifikasi dari bytecode Material3 proyek ini), jadi
 * saat diam background kosong total; saat digeser/dianimasikan
 * keluar, arahnya EndToStart dan latar merah terungkap. Geser kanan
 * dinonaktifkan. Dipakai antrean penuh + UP NEXT detail;
 * berdampingan dengan drag-to-reorder vertikal (long-press) tanpa
 * berebut gesture: swipe cepat horizontal ditangani kotak ini,
 * seret vertikal setelah long-press ditangani pengenal di dalam
 * konten.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissDelete(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    // Hapus SETELAH animasi settle selesai (currentValue berubah),
    // bukan di confirmValueChange — supaya baris terlihat meluncur
    // keluar dulu, bukan hilang seketika.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDismissed()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // v1.10.19 P0: latar HANYA saat swipe berlangsung.
            // dismissDirection == Settled ⟺ offset == 0 (diam) —
            // saat diam tidak ada yang digambar, baris normal bersih.
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFFB3261E)),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 20.dp).size(24.dp),
                    )
                }
            }
        },
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        content = content,
    )
}

/**
 * Gagang seret (drag handle) — 2 kolom × 3 titik, digambar langsung
 * (tanpa artefak ikon extended). Dipakai baris Up Next (normal + Jam)
 * supaya drag-to-reorder bisa DITEMUKAN, bukan cuma long-press buta
 * (v1.10.36). Pemanggil menaruhnya lewat [handleModifier] dari
 * [ReorderableColumn] / baris Up Next normal.
 */
@Composable
fun DragHandleDots(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(
                        Modifier.size(3.dp).background(tint, CircleShape),
                    )
                }
            }
        }
    }
}

/**
 * Kolom baris yang bisa disusun ulang lewat GAGANG SERET (v1.10.36).
 * Dipakai daftar Up Next sesi Jam (kartu Jam + tab detail): baris
 * tetap bisa diketuk (main dari Up Next, 10.34) dan long-press
 * seleksi tetap hidup — drag HANYA dimulai dari gagang, lewat
 * [handleModifier] yang diteruskan ke [rowContent]. Tanpa Undo
 * (seret balik = undo). Baris dikunci [keyOf] (pelajaran 10.20:
 * state baris tidak boleh diwarisi baris lain saat urutan berubah).
 * Asumsi yang sama dengan ReorderableUpcoming: tinggi baris seragam
 * (target drop dihitung dari tinggi baris pertama).
 *
 * [onMove] menerima indeks (dari, ke) ke dalam [items] saat ini.
 * Selama [dragEnabled] false (mis. mode seleksi), pemanggil tidak
 * menampilkan gagang dan drag tidak aktif.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    keyOf: (T) -> String,
    dragEnabled: Boolean,
    onMove: (Int, Int) -> Unit,
    haptic: (HapticKind) -> Unit = {},
    rowContent: @Composable RowScope.(item: T, index: Int, handleModifier: Modifier) -> Unit,
) {
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    var dropTarget by remember { mutableStateOf<Int?>(null) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    var listTopPx by remember { mutableFloatStateOf(0f) }
    val rowTops = remember { HashMap<String, Float>() }
    val handleTops = remember { HashMap<String, Float>() }
    var dragStartFingerY by remember { mutableFloatStateOf(0f) }

    fun resetDrag() {
        draggingKey = null
        dragDeltaY = 0f
        dropTarget = null
    }
    fun computeTarget(): Int {
        val h = rowHeightPx
        if (h <= 0f) return -1
        val fingerY = dragStartFingerY + dragDeltaY
        return ((fingerY - listTopPx) / h).toInt()
            .coerceIn(0, items.lastIndex)
    }
    fun beginDrag(key: String, index: Int, fingerY: Float) {
        draggingKey = key
        dragDeltaY = 0f
        dropTarget = index
        dragStartFingerY = fingerY
        haptic(HapticKind.LongPress)
    }
    fun updateDrag(dragAmountY: Float) {
        dragDeltaY += dragAmountY
        val target = computeTarget()
        if (target >= 0 && target != dropTarget) {
            dropTarget = target
        }
    }
    fun endDrag(fromIndex: Int) {
        val target = dropTarget
        val key = draggingKey
        resetDrag()
        if (key != null && target != null && target != fromIndex) {
            haptic(HapticKind.Tap)
            onMove(fromIndex, target)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { listTopPx = it.boundsInWindow().top },
    ) {
        Column(Modifier.fillMaxWidth()) {
            for ((index, item) in items.withIndex()) {
                key(keyOf(item)) {
                    val itemKey = keyOf(item)
                    val dragging = draggingKey == itemKey
                    val handleModifier = if (!dragEnabled) {
                        Modifier
                    } else {
                        Modifier
                            .onGloballyPositioned { coords ->
                                handleTops[itemKey] = coords.boundsInWindow().top
                            }
                            .pointerInput(itemKey, dragEnabled) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { down ->
                                        beginDrag(
                                            itemKey,
                                            index,
                                            (handleTops[itemKey] ?: rowTops[itemKey] ?: 0f) + down.y,
                                        )
                                    },
                                    onDragEnd = { endDrag(index) },
                                    onDragCancel = { resetDrag() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        updateDrag(dragAmount.y)
                                    },
                                )
                            }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                rowTops[itemKey] = coords.boundsInWindow().top
                                if (rowHeightPx == 0f) rowHeightPx = coords.size.height.toFloat()
                            }
                            .graphicsLayer {
                                translationY = if (dragging) dragDeltaY else 0f
                                scaleX = if (dragging) 1.03f else 1f
                                scaleY = if (dragging) 1.03f else 1f
                            }
                            .shadow(if (dragging) 8.dp else 0.dp, RoundedCornerShape(Corners.small))
                            .zIndex(if (dragging) 1f else 0f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowContent(item, index, handleModifier)
                    }
                }
            }
        }
        // Garis indikator posisi drop (pola ReorderableUpcoming).
        val target = dropTarget
        if (draggingKey != null && target != null && rowHeightPx > 0f) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(0, (target * rowHeightPx).roundToInt()) }
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(BurntOrange, RoundedCornerShape(2.dp)),
            )
        }
    }
}
