package com.sonara.app.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ripple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonara.app.data.CommentsApi
import com.sonara.app.data.DownloadManager
import com.sonara.app.data.DownloadTaskState
import com.sonara.app.data.Downloads
import com.sonara.app.data.Source
import com.sonara.app.data.Playlist
import com.sonara.app.data.Track
import com.sonara.app.data.ArtistApi
import com.sonara.app.data.YoutubeMusicApi
import com.sonara.app.data.displayArtist
import com.sonara.app.data.formatDurationMs
import com.sonara.app.data.PlaybackSpeedPolicy
import com.sonara.app.data.isUsableArtist
import com.sonara.app.data.shareTextFor
import com.sonara.app.ui.ArusBackButton
import com.sonara.app.ui.sonaraTextButtonColors
import com.sonara.app.ui.sonaraTextFieldColors
import com.sonara.app.ui.CoverArt
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.theme.BurntOrange
import com.sonara.app.ui.theme.DetailAccent
import com.sonara.app.ui.theme.ThemeError
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.theme.Motion
import com.sonara.app.ui.theme.PaperBg
import com.sonara.app.ui.theme.SegmentBg
import com.sonara.app.ui.theme.Spacing
import com.sonara.app.ui.showToast
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Warna sheet ARUS v1.10.4 — kertas hangat + tinta + aksen oranye (token Theme.kt). */
private val SheetBg: Color @Composable get() = PaperBg
private val SheetSurface: Color @Composable get() = SegmentBg
private val SheetText: Color @Composable get() = InkBlack
private val SheetMuted: Color @Composable get() = InkMuted

/**
 * Warna sheet menu lagu GELAP MENEMPEL v1.10.11 — mengikuti pola
 * referensi (permukaan gelap + teks/ikon terang), dengan token
 * ARUS sendiri: latar hitam hangat, tile abu gelap, teks sekunder
 * krem, aksen tetap BurntOrange. TIDAK menyalin aset/branding
 * YouTube Music; hanya polanya (menempel, gelap) yang dipakai.
 */
private val DarkSheetBg = Color(0xFF211D19)
private val DarkSheetTile = Color(0xFF37322C)
private val DarkSheetText = Color(0xFFFFFFFF)
private val DarkSheetMuted = Color(0xFFD8C9B8)
private val DarkSheetDivider = Color.White.copy(alpha = 0.12f)

// v1.10.6: TIDAK ADA lagi angka angkat tetap. Perbaikan v1.10.4
// (216dp) ternyata menghitung ganda: Scaffold SUDAH mencadangkan
// chrome bawah lewat content padding di SEMUA call site, sementara
// sheet adalah overlay root-level — padding 216dp justru mendorong
// kartu ke atas sampai tinggi efektifnya tinggal ~429-471dp untuk
// konten ~736dp, dan urutan modifier yang salah (clip/background
// DI DALAM scroll) membuat baris bawah terpotong visual. Kini
// tinggi sheet dibatasi fraksi tinggi layar yang terlihat dan
// clearance bawah hanya inset navigation bar + napas 12dp.

/**
 * Kerangka bottom sheet ARUS: latar redup (ketuk = tutup) + kartu
 * bawah yang naik dengan slide-up. Keluarnya juga beranimasi
 * sebelum [onDismiss] dipanggil.
 *
 * Polish v1.5: handle atas bisa diseret ke bawah untuk menutup
 * (seret > 90dp = tutup, kurang dari itu = kembali membal); isi
 * terangkat di atas navigation bar; konten bisa di-scroll seperti
 * biasa tanpa berebut gesture dengan handle.
 *
 * v1.10.6: tinggi maks = tinggi layar terlihat - 24dp (fraksi,
 * bukan angka tetap); clearance bawah = inset navigation bar +
 * 12dp di luar kartu; clip + background SEBELUM verticalScroll
 * supaya kartu terpotong rapi dan seluruh isi bisa di-scroll
 * sampai baris terakhir ("Go to artist") + padding bawahnya.
 */
@Composable
fun ArusBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    fun requestDismiss() {
        visible = false
    }
    // v1.10.1: Back sistem saat sheet terbuka menutup sheet dulu
    // (sheet ini overlay, bukan Dialog — BackHandler root tidak
    // menjangkaunya). Handler ini lebih dalam dari handler root
    // sehingga diprioritaskan saat sheet sedang tampil.
    BackHandler(enabled = visible) { requestDismiss() }
    LaunchedEffect(visible) {
        if (!visible) {
            kotlinx.coroutines.delay(Motion.mediumMs.toLong())
            onDismiss()
        }
    }
    // Seret handle: offset dikembalikan membal kalau tidak sampai ambang.
    var dragY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val sheetOffset by animateFloatAsState(
        targetValue = if (dragging) dragY else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "sheetDrag",
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dismissThresholdPx = with(density) { 90.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Tinggi maks: fraksi tinggi layar yang terlihat - napas
        // atas 24dp; lantai 240dp supaya layar sangat pendek tetap
        // menampilkan sebagian isi yang bisa di-scroll sampai habis.
        val maxSheetHeight = (maxHeight - 24.dp).coerceAtLeast(240.dp)
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(Motion.mediumMs)),
            exit = fadeOut(tween(Motion.mediumMs)),
        ) {
            // v1.10.40: scrim HARUS buram penuh (opaque). Dengan alpha
            // 0.55, teks terang layar di baliknya (mis. tombol "Select"
            // di atas panel UP NEXT) tetap terbaca lewat celah atas
            // 24dp / bawah di luar kartu — persis laporan user.
            // Kartu sheet sendiri tidak berubah (tinggi, handle,
            // padding, SheetBg sama).
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { requestDismiss() },
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(Motion.slowMs)) { it } + fadeIn(tween(Motion.mediumMs)),
            exit = slideOutVertically(tween(Motion.mediumMs)) { it } + fadeOut(tween(Motion.mediumMs)),
            modifier = Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .graphicsLayer { translationY = sheetOffset }
                    .clip(RoundedCornerShape(topStart = Corners.sheet, topEnd = Corners.sheet))
                    .background(SheetBg)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                // Handle seret — area sentuh 28dp, hanya bagian ini
                // yang menangkap gesture vertikal (isi tetap scroll).
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .draggable(
                            state = rememberDraggableState { delta ->
                                dragY = (dragY + delta).coerceAtLeast(0f)
                            },
                            orientation = Orientation.Vertical,
                            onDragStarted = { dragging = true },
                            onDragStopped = {
                                dragging = false
                                if (dragY > dismissThresholdPx) {
                                    requestDismiss()
                                } else {
                                    dragY = 0f
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SheetMuted.copy(alpha = 0.5f)),
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                // v1.10.1: tombol Back terlihat di sheet (Back = tutup
                // sheet), di bawah handle seret. v1.10.4: varian
                // terang (sheet kini kertas hangat, bukan gelap).
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArusBackButton(onClick = { requestDismiss() })
                }
                Spacer(Modifier.height(Spacing.sm))
                content()
                // Napas bawah di dalam area scroll — baris terakhir
                // tidak pernah menempel di tepi bawah kartu.
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}

/**
 * Bottom sheet GELAP MENEMPEL v1.10.11 — HANYA untuk menu lagu
 * (TrackActionSheet). Sheet lain (SaveToPlaylistSheet, CommentsPanel)
 * tetap memakai [ArusBottomSheet] dan tidak diubah.
 *
 * Kenapa Dialog, bukan overlay di dalam konten: sheet yang dirender
 * di dalam area konten Scaffold tidak pernah bisa mencapai tepi
 * bawah layar — padding Scaffold + bottom chrome (bar Jam, strip,
 * mini player, nav) selalu terlihat di bawahnya, sehingga kartu
 * tampak "melayang" (keluhan user 22 Nov 16:35). Dialog adalah
 * window tersendiri di atas segalanya: sheet menempel edge-to-edge
 * (lebar penuh, tanpa margin), bawahnya menyentuh tepi bawah
 * layar dan MENUTUPI bottom chrome selama terbuka. Area gesture
 * bar sistem ditangani sebagai padding DI DALAM sheet (bukan celah
 * di bawahnya), jadi tidak ada garis terang di bawah kartu.
 *
 * Perilaku sama dengan [ArusBottomSheet]: latar buram penuh (ketuk =
 * tutup), slide-up + fade, handle seret ke bawah untuk menutup,
 * konten scroll internal di layar pendek (tidak ada aksi terpotong),
 * Back sistem menutup sheet (Dialog onDismissRequest → tutup
 * beranimasi, bukan hilang mendadak).
 */
@Composable
private fun DockedDarkSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    fun requestDismiss() {
        visible = false
    }
    LaunchedEffect(visible) {
        if (!visible) {
            kotlinx.coroutines.delay(Motion.mediumMs.toLong())
            onDismiss()
        }
    }
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val sheetOffset by animateFloatAsState(
        targetValue = if (dragging) dragY else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "dockedSheetDrag",
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dismissThresholdPx = with(density) { 90.dp.toPx() }
    Dialog(
        onDismissRequest = { requestDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Tinggi maks = tinggi jendela - napas atas 24dp; lantai
            // 240dp supaya layar sangat pendek tetap menampilkan
            // sebagian isi yang bisa di-scroll sampai habis.
            val maxSheetHeight = (maxHeight - 24.dp).coerceAtLeast(240.dp)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(Motion.mediumMs)),
                exit = fadeOut(tween(Motion.mediumMs)),
            ) {
                // v1.10.40: scrim HARUS buram penuh (opaque) — sama
                // seperti ArusBottomSheet; dengan 0.55, teks layar di
                // baliknya tembus lewat celah atas 24dp.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { requestDismiss() },
                )
            }
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(Motion.slowMs)) { it } + fadeIn(tween(Motion.mediumMs)),
                exit = slideOutVertically(tween(Motion.mediumMs)) { it } + fadeOut(tween(Motion.mediumMs)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                // Lebar penuh di HP; di layar lebar (tablet) dibatasi
                // 720dp terpusat seperti konten root (lihat SonaraApp).
                // TIDAK ADA margin horizontal/bawah: kartu menempel.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .graphicsLayer { translationY = sheetOffset }
                        .clip(RoundedCornerShape(topStart = Corners.sheet, topEnd = Corners.sheet))
                        .background(DarkSheetBg),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxSheetHeight)
                            .verticalScroll(rememberScrollState())
                            // Inset navigation bar DI DALAM sheet:
                            // area gesture bar tetap warna sheet,
                            // bukan celah di bawah kartu.
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    ) {
                        // Handle seret — area sentuh 28dp, hanya
                        // bagian ini yang menangkap gesture vertikal.
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .draggable(
                                    state = rememberDraggableState { delta ->
                                        dragY = (dragY + delta).coerceAtLeast(0f)
                                    },
                                    orientation = Orientation.Vertical,
                                    onDragStarted = { dragging = true },
                                    onDragStopped = {
                                        dragging = false
                                        if (dragY > dismissThresholdPx) {
                                            requestDismiss()
                                        } else {
                                            dragY = 0f
                                        }
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(width = 40.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(DarkSheetMuted.copy(alpha = 0.6f)),
                            )
                        }
                        content()
                        // Napas bawah di dalam area scroll — baris
                        // terakhir tidak menempel di tepi bawah kartu.
                        Spacer(Modifier.height(Spacing.md))
                    }
                }
            }
        }
    }
}

private fun shareTrack(context: android.content.Context, track: Track) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        // v1.10.4: teks share membawa link YouTube Music asli lagu
        // itu (dari videoId) + atribusi; tanpa videoId (lagu lokal)
        // fallback ke teks seperti sebelumnya, tanpa link karangan.
        putExtra(Intent.EXTRA_TEXT, shareTextFor(track))
    }
    context.startActivity(Intent.createChooser(send, "Share song"))
}

/**
 * Sheet menu lagu ala referensi YTM: header lagu + like + X, 3 tile
 * (Play next / Save to playlist / Share), lalu 8 aksi. Semua tombol
 * berperilaku NYATA (atau toast jujur bila belum didukung) — tidak ada
 * tombol mati.
 */
@Composable
fun TrackActionSheet(
    track: Track,
    state: SonaraState,
    onDismiss: () -> Unit,
    onOpenSearch: (String) -> Unit,
    /** Buka profil artis (v1.10.18). Null = fallback cari nama artis. */
    onOpenArtist: ((String, String?) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloads = remember { Downloads(context, state.store.audioQuality) }
    // Durasi tampil (v1.10.31): bawaan track bila ada, else cache
    // lookupVideo — sheet dibuka = baris "terlihat", jadi minta di sini.
    val durationCache by state.durations.cache.collectAsState()
    val shownDurationMs = track.durationMs.takeIf { it > 0 }
        ?: (durationCache[track.youtubeVideoId] ?: 0L)
    val shownDuration = formatDurationMs(shownDurationMs)
    LaunchedEffect(track.youtubeVideoId) {
        if (track.durationMs <= 0) state.durations.request(track.youtubeVideoId)
    }
    var showSaveSheet by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    var showTrackInfo by remember { mutableStateOf(false) }
    // v1.0.5: dialog kecepatan + segarkan label played setelah toggle.
    var showSpeedDialog by remember { mutableStateOf(false) }
    val userRate by state.player.userPlaybackRate.collectAsState()
    var playedTick by remember(track.id) { mutableStateOf(0) }
    var liked by remember(track.id) { mutableStateOf(state.store.isLiked(track.id)) }
    val canDownload = track.source == com.sonara.app.data.Source.YOUTUBE_MUSIC &&
        track.youtubeVideoId != null
    // v1.0.6: status unduhan dari DownloadManager — progres/jeda/
    // lanjut terlihat di tampilan Offline Koleksi.
    val downloadTasks by DownloadManager.tasks.collectAsState()
    LaunchedEffect(Unit) { DownloadManager.ensureLoaded(context) }
    val downloadTask = track.youtubeVideoId?.let { id ->
        downloadTasks.firstOrNull { it.videoId == id }
    }
    val downloadLabel = when {
        !canDownload -> "Download"
        track.youtubeVideoId?.let { downloads.isDownloaded(it) } == true -> "Downloaded"
        downloadTask?.state == DownloadTaskState.DOWNLOADING -> "Downloading…"
        downloadTask?.state == DownloadTaskState.QUEUED -> "Download queued"
        downloadTask?.state == DownloadTaskState.PAUSED -> "Download paused — resume"
        downloadTask?.state == DownloadTaskState.FAILED -> "Download failed — retry"
        else -> "Download"
    }
    // v1.10.11: gate opsi "Add to Jam queue" — sesi Jam yang
    // BENAR-BENAR hidup (aktif + kode tampil), definisi yang sama
    // dengan sessionAlive di dialog Jam (v1.10.8). Tanpa sesi aktif,
    // opsi tidak ditampilkan sama sekali (tanpa entri mati).
    val jamState by state.jam.uiState.collectAsState()
    val jamAlive = jamState.active && jamState.code.isNotEmpty()

    fun close() = onDismiss()

    // v1.10.11: sheet gelap menempel (DockedDarkSheet) — bukan lagi
    // kartu mengambang ArusBottomSheet. Aksi-aksinya TETAP SAMA
    // PERSIS; hanya presentasi yang berubah.
    DockedDarkSheet(onDismiss = onDismiss) {
        // ---- Header lagu ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverArt(track = track, size = 48.dp, corner = Corners.small, letterSize = 18.sp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSheetText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // v1.10.19: artis kosong → durasi saja.
                    run {
                        val a = displayArtist(track.artist)
                        if (a.isNotBlank()) "$a • $shownDuration" else shownDuration
                    },
                    fontSize = 13.sp,
                    color = DarkSheetMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = {
                liked = !liked
                state.store.setLikedTrack(track, liked)
            }) {
                Icon(
                    imageVector = if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Like",
                    tint = if (liked) DetailAccent else DarkSheetMuted,
                )
            }
            IconButton(onClick = { close() }) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = DarkSheetText)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        // Pembatas seperti referensi: header, garis tipis, lalu tile.
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DarkSheetDivider),
        )
        Spacer(Modifier.height(Spacing.lg))

        // ---- 3 tile besar ----
        Row(Modifier.fillMaxWidth()) {
            SheetTile(
                icon = Icons.Filled.QueueMusic,
                label = "Play next",
                modifier = Modifier.weight(1f),
                onClick = {
                    state.player.playNext(track)
                    showToast(context, "Playing next")
                    close()
                },
            )
            Spacer(Modifier.width(Spacing.sm))
            SheetTile(
                icon = Icons.Filled.PlaylistAdd,
                label = "Save to playlist",
                modifier = Modifier.weight(1f),
                onClick = { showSaveSheet = true },
            )
            Spacer(Modifier.width(Spacing.sm))
            SheetTile(
                icon = Icons.Filled.Share,
                label = "Share",
                modifier = Modifier.weight(1f),
                onClick = {
                    shareTrack(context, track)
                    close()
                },
            )
        }
        Spacer(Modifier.height(Spacing.md))

        // ---- 8 aksi ----
        Column(Modifier.fillMaxWidth()) {
            SheetAction(
                icon = Icons.Filled.Radio,
                label = "Start mix",
                onClick = {
                    scope.launch {
                        // v1.10.19: tanpa artis yang bisa dipakai, jangan
                        // cari mix dari label tipe ("Video").
                        if (!isUsableArtist(track.artist)) {
                            showToast(context, "No mix found")
                            close()
                            return@launch
                        }
                        val ytmApi = YoutubeMusicApi()
                        val res = withContext(Dispatchers.IO) { ytmApi.search(displayArtist(track.artist)) }
                        res.onSuccess { found ->
                            val mix = found.filter { it.id != track.id }.take(20)
                            if (mix.isNotEmpty()) {
                                state.player.playQueue(listOf(track) + mix, 0)
                                showToast(context, "Mix started: ${displayArtist(track.artist)}")
                            } else {
                                showToast(context, "No mix found")
                            }
                        }.onFailure {
                            showToast(context, "Mix failed to load")
                        }
                        close()
                    }
                },
            )
            SheetAction(
                icon = Icons.Filled.QueueMusic,
                label = "Add to queue",
                onClick = {
                    state.player.addToQueue(track)
                    showToast(context, "Added to queue")
                    close()
                },
            )
            // v1.0.3: "Start radio" — lagu-lagu seperti lagu ini,
            // disimpan TERPISAH dari antrean user (seksi Radio di
            // layar detail); lagu user selalu menang. NON-Jam saja:
            // selama sesi Jam, antrean milik sesi (aksi disembunyikan
            // — sama seperti "Add to Jam queue" di atas yang hanya
            // muncul saat sesi hidup, dibalik). Tanpa videoId (lagu
            // lokal) radio tidak bisa dimulai — aksi disembunyikan.
            if (!jamAlive && track.youtubeVideoId != null) {
                SheetAction(
                    icon = Icons.Filled.Podcasts,
                    label = "Start radio",
                    onClick = {
                        state.player.startRadio(track)
                        showToast(context, "Radio started: ${track.title}")
                        close()
                    },
                )
            }
            // v1.10.11: "Add to Jam queue" — tepat setelah "Add to
            // queue", HANYA saat sesi Jam aktif. Memakai jalur tulis
            // antrean sesi yang SAMA dengan tombol + di dialog Jam
            // (RTDB queue push via addTrackToQueue — host maupun
            // tamu boleh menambah); penjagaan jujurnya ada di jalur
            // itu (sesi ternyata mati → pesan jujur, bukan diam).
            // Umpan balik toast berasal dari jalur yang sama
            // ("Added to the Jam queue.").
            if (jamAlive) {
                SheetAction(
                    icon = Icons.Filled.GroupAdd,
                    label = "Add to Jam queue",
                    onClick = {
                        state.jam.addTrackToQueue(track) { msg ->
                            showToast(context, msg)
                        }
                        close()
                    },
                )
            }
            // v1.0.5: kecepatan putar — disembunyikan saat sesi Jam
            // (drift Jam yang memiliki rate). Membuka dialog yang sama
            // dengan layar detail + Settings; sheet tetap di bawahnya.
            if (!jamAlive) {
                SheetAction(
                    icon = Icons.Filled.Speed,
                    label = "Playback speed • ${PlaybackSpeedPolicy.formatSpeed(userRate)}",
                    onClick = { showSpeedDialog = true },
                )
            }
            // v1.0.5: tandai played — episode podcast / konten panjang
            // saja (durasi tampil ≥ 10 mnt). Lokal; played dikeluarkan
            // dari Keep listening, un-mark mengembalikannya.
            if (track.isPodcastEpisode || shownDurationMs >= 600_000L) {
                val played = playedTick.let { state.store.isEpisodePlayed(track.id) }
                SheetAction(
                    icon = Icons.Filled.CheckCircle,
                    label = if (played) "Mark as not played" else "Mark as played",
                    onClick = {
                        state.store.setPlayedEpisode(track.id, !played)
                        playedTick++
                        showToast(
                            context,
                            if (!played) "Marked as played" else "Marked as not played",
                        )
                    },
                )
            }
            SheetAction(
                icon = Icons.Filled.BookmarkBorder,
                label = if (liked) "Remove from library" else "Save to library",
                onClick = {
                    liked = !liked
                    state.store.setLikedTrack(track, liked)
                    showToast(context, if (liked) "Saved to library" else "Removed from library")
                    close()
                },
            )
            SheetAction(
                icon = Icons.Filled.Download,
                label = downloadLabel,
                onClick = {
                    if (!canDownload) {
                        showToast(context, "Downloads are only for YouTube Music songs")
                        close()
                        return@SheetAction
                    }
                    val videoId = track.youtubeVideoId!!
                    if (downloads.isDownloaded(videoId)) {
                        showToast(context, "This song is already downloaded")
                        close()
                        return@SheetAction
                    }
                    when (downloadTask?.state) {
                        DownloadTaskState.DOWNLOADING, DownloadTaskState.QUEUED -> {
                            showToast(
                                context,
                                "Download in progress — see Offline downloads in Library",
                            )
                        }
                        DownloadTaskState.PAUSED -> {
                            DownloadManager.resume(context, videoId)
                            showToast(context, "Download resumed")
                        }
                        DownloadTaskState.FAILED -> {
                            DownloadManager.retry(context, videoId)
                            showToast(context, "Download restarted")
                        }
                        else -> {
                            DownloadManager.start(context, track)
                            showToast(
                                context,
                                "Downloading — progress in Offline downloads",
                            )
                        }
                    }
                    close()
                },
            )
            SheetAction(
                icon = Icons.Filled.Album,
                label = "Go to album",
                onClick = {
                    // v1.10.19: fallback artis hanya bila bisa dipakai.
                    val q = track.album.ifBlank { displayArtist(track.artist) }
                    if (q.isNotBlank()) onOpenSearch(q)
                    close()
                },
            )
            SheetAction(
                icon = Icons.Filled.Person,
                label = "Go to artist",
                onClick = {
                    // v1.10.19: tanpa artis yang bisa dipakai, aksi ini
                    // tidak menavigasi ke mana pun (bukan ke "Video").
                    if (isUsableArtist(track.artist)) {
                        val usable = displayArtist(track.artist)
                        if (onOpenArtist != null) {
                            onOpenArtist(
                                ArtistApi.firstArtistName(usable),
                                track.artistBrowseId,
                            )
                        } else {
                            onOpenSearch(usable)
                        }
                    }
                    close()
                },
            )
            SheetAction(
                icon = Icons.Filled.Groups,
                label = "View song credits",
                onClick = { showCredits = true },
            )
            // v1.0.3: info lagu — sumber + detail stream nyata dari
            // jalur resolve + jejak percobaan (dialog yang sama
            // dipakai tombol "Details" di kotak error layar detail).
            SheetAction(
                icon = Icons.Filled.Info,
                label = "Track info",
                onClick = { showTrackInfo = true },
            )
            SheetAction(
                icon = Icons.Filled.PushPin,
                label = "Pin to Speed dial",
                onClick = {
                    showToast(context, "Pin to Speed dial isn't available yet.")
                    close()
                },
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }

    if (showSaveSheet) {
        SaveToPlaylistSheet(
            track = track,
            state = state,
            onDismiss = { showSaveSheet = false },
        )
    }

    if (showCredits) {
        AlertDialog(
            onDismissRequest = { showCredits = false },
            title = { Text("Song credits") },
            text = {
                Text(
                    "Title: ${track.title}\n" +
                        // v1.10.19: konteks info seperti header detail.
                        "Artist: ${displayArtist(track.artist).ifBlank { "Unknown artist" }}\n" +
                        "Album: ${track.album.ifBlank { "—" }}\n" +
                        "Source: ${track.source.label}\n" +
                        "Duration: $shownDuration\n" +
                        "Songwriter/producer credits aren't available in SONARA.",
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showCredits = false },
                    // v1.10.40: teks aksen (default primary/accentFill 1,41:1 di SENJA Light).
                    colors = sonaraTextButtonColors(),
                ) { Text("Close") }
            },
        )
    }

    if (showTrackInfo) {
        TrackInfoDialog(
            track = track,
            state = state,
            onDismiss = { showTrackInfo = false },
        )
    }

    // v1.0.5: dialog kecepatan putar (pilihan yang sama dengan detail).
    if (showSpeedDialog) {
        com.sonara.app.ui.SpeedDialog(
            currentRate = userRate,
            onDismiss = { showSpeedDialog = false },
            onSelect = { rate ->
                state.player.setUserPlaybackRate(rate)
                showSpeedDialog = false
            },
        )
    }
}

/**
 * Dialog info lagu (v1.0.3) — dipakai aksi "Track info" di sheet
 * DAN tombol "Details" di kotak error layar detail (satu sumber).
 * Semua yang tampil berasal dari data nyata: sumber + videoId dari
 * [Track], format/bitrate dari stream terpilih pemutar, dan jejak
 * langkah resolve apa adanya. Lagu yang belum diputar tidak punya
 * info stream — dikatakan apa adanya, bukan dikarang.
 */
@Composable
fun TrackInfoDialog(
    track: Track,
    state: SonaraState,
    onDismiss: () -> Unit,
) {
    val currentTrack by state.player.currentTrack.collectAsState()
    val currentStream by state.player.currentStream.collectAsState()
    val resolveTrace by state.player.resolveTrace.collectAsState()
    val isCurrent = currentTrack?.id == track.id
    val stream = if (isCurrent) currentStream else null
    val traceLines = if (isCurrent) resolveTrace.toDisplayLines() else emptyList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track info") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Title: ${track.title}\n" +
                        "Artist: ${displayArtist(track.artist).ifBlank { "Unknown artist" }}\n" +
                        "Album: ${track.album.ifBlank { "—" }}\n" +
                        "Source: ${track.source.label}\n" +
                        "Video ID: ${track.youtubeVideoId ?: "—"}",
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(12.dp))
                if (!isCurrent) {
                    Text(
                        "Stream details appear once this song plays.",
                        fontSize = 14.sp,
                    )
                } else if (stream != null) {
                    Text(
                        buildString {
                            append("Stream: ")
                            if (stream.isLive) {
                                append("Live stream (HLS manifest)")
                            } else {
                                append(stream.mimeType.ifBlank { "Unknown format" })
                                append(" · itag ${stream.itag}")
                                if (stream.bitrate > 0) {
                                    append(" · ${stream.bitrate / 1000} kbps")
                                }
                            }
                            if (stream.hlsManifestUrl != null && !stream.isLive) {
                                append("\nFallback: HLS manifest")
                            }
                        },
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "How the stream was found:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (traceLines.isNotEmpty()) {
                            traceLines.joinToString("\n")
                        } else {
                            "No resolve was needed in this session — a ready stream URL was used."
                        },
                        fontSize = 14.sp,
                    )
                } else {
                    Text(
                        "No direct stream for this song — playing via the YouTube player.",
                        fontSize = 14.sp,
                    )
                    if (traceLines.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "What was tried, in order:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            traceLines.joinToString("\n"),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = sonaraTextButtonColors(),
            ) { Text("Close") }
        },
    )
}

@Composable
private fun SheetTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(Corners.card))
                // v1.10.11: tile abu gelap + ikon terang (referensi).
                .background(DarkSheetTile)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = DarkSheetText),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = DarkSheetText, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            label,
            fontSize = 13.sp,
            color = DarkSheetText,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = DarkSheetText),
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = DarkSheetText, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(Spacing.lg))
        Text(label, fontSize = 15.sp, color = DarkSheetText)
    }
}

/** Sheet "Save to playlist" — daftar playlist lokal + buat baru. NYATA. */
@Composable
fun SaveToPlaylistSheet(
    track: Track,
    state: SonaraState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(state.store.playlists()) }
    var newName by remember { mutableStateOf("") }

    ArusBottomSheet(onDismiss = onDismiss) {
        Text(
            "Save to playlist",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SheetText,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            track.title,
            fontSize = 13.sp,
            color = SheetMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.md))
        Column(Modifier.fillMaxWidth()) {
            if (playlists.isEmpty()) {
                Text(
                    "No playlists yet. Create one below.",
                    fontSize = 14.sp,
                    color = SheetMuted,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            for (pl in playlists) {
                val contains = pl.trackIds.contains(track.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            if (!contains) {
                                val updated = playlists.map {
                                    if (it.id == pl.id) it.copy(trackIds = it.trackIds + track.id)
                                    else it
                                }
                                state.store.savePlaylists(updated)
                                playlists = updated
                                // v1.0.6: auto-download bila sakelar
                                // playlist ini menyala.
                                val auto = DownloadManager.maybeAutoDownload(
                                    context, state.store, pl.id, listOf(track),
                                )
                                showToast(
                                    context,
                                    if (auto > 0) "Saved to ${pl.name} · Downloading 1 song"
                                    else "Saved to ${pl.name}",
                                )
                            } else {
                                showToast(context, "Already in ${pl.name}")
                            }
                            onDismiss()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.PlaylistAdd,
                        contentDescription = null,
                        tint = SheetText,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(Spacing.lg))
                    Text(pl.name, fontSize = 15.sp, color = SheetText, modifier = Modifier.weight(1f))
                    Text(
                        if (contains) "Already added" else "${pl.trackIds.size} songs",
                        fontSize = 12.sp,
                        color = SheetMuted,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            // Field di atas sheet kertas: border/label fokus memakai
            // aksen teks (v1.10.40, helper) — default M3 memakai
            // primary/accentFill yang pucat di SENJA/GETAR Light.
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = sonaraTextFieldColors(),
            )
            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = {
                    if (newName.isBlank()) return@Button
                    val created = Playlist(id = UUID.randomUUID().toString(), name = newName.trim())
                    val updated = playlists + created.copy(trackIds = listOf(track.id))
                    state.store.savePlaylists(updated)
                    // v1.0.6: playlist baru = auto-download mati (default),
                    // jadi panggilan ini selalu 0 — tetap dipanggil agar
                    // perilaku konsisten bila default berubah.
                    val auto = DownloadManager.maybeAutoDownload(
                        context, state.store, created.id, listOf(track),
                    )
                    showToast(
                        context,
                        if (auto > 0) "Playlist created + song saved · Downloading"
                        else "Playlist created + song saved",
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Create playlist + save") }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

/**
 * Sheet "Save to playlist" untuk BANYAK lagu (v1.10.21, aksi bulk
 * multi-select) — pola visual [SaveToPlaylistSheet] yang sama.
 * Lagu yang sudah ada di playlist dilewati (konsisten dengan sheet
 * satu lagu: tidak ada duplikat dari aksi ini); id yang sama
 * terpilih dua kali hanya ditambah sekali. Toast menghitung jujur
 * lagu yang benar-benar ditambahkan.
 */
@Composable
fun SaveTracksToPlaylistSheet(
    tracks: List<Track>,
    state: SonaraState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(state.store.playlists()) }
    var newName by remember { mutableStateOf("") }
    // Id unik terpilih, urutan dijaga (duplikat antar-baris dilewati).
    val distinctIds = remember(tracks) { tracks.map { it.id }.distinct() }

    ArusBottomSheet(onDismiss = onDismiss) {
        Text(
            "Save to playlist",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SheetText,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            if (tracks.size == 1) "1 song selected" else "${tracks.size} songs selected",
            fontSize = 13.sp,
            color = SheetMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.md))
        Column(Modifier.fillMaxWidth()) {
            if (playlists.isEmpty()) {
                Text(
                    "No playlists yet. Create one below.",
                    fontSize = 14.sp,
                    color = SheetMuted,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            for (pl in playlists) {
                val already = distinctIds.count { it in pl.trackIds }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            val toAdd = distinctIds.filter { it !in pl.trackIds }
                            if (toAdd.isNotEmpty()) {
                                val updated = playlists.map {
                                    if (it.id == pl.id) it.copy(trackIds = it.trackIds + toAdd)
                                    else it
                                }
                                state.store.savePlaylists(updated)
                                playlists = updated
                                // v1.0.6: auto-download lagu yang
                                // benar-benar baru ditambahkan.
                                val addedTracks = tracks.filter { it.id in toAdd }
                                    .distinctBy { it.id }
                                val auto = DownloadManager.maybeAutoDownload(
                                    context, state.store, pl.id, addedTracks,
                                )
                                showToast(
                                    context,
                                    if (toAdd.size == 1) {
                                        if (auto > 0) "Added 1 song to ${pl.name} · Downloading"
                                        else "Added 1 song to ${pl.name}"
                                    } else {
                                        if (auto > 0) "Added ${toAdd.size} songs to ${pl.name} · Downloading $auto"
                                        else "Added ${toAdd.size} songs to ${pl.name}"
                                    },
                                )
                            } else {
                                showToast(context, "Already in ${pl.name}")
                            }
                            onDismiss()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.PlaylistAdd,
                        contentDescription = null,
                        tint = SheetText,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(Spacing.lg))
                    Text(pl.name, fontSize = 15.sp, color = SheetText, modifier = Modifier.weight(1f))
                    Text(
                        if (already >= distinctIds.size) "Already added"
                        else "${pl.trackIds.size} songs",
                        fontSize = 12.sp,
                        color = SheetMuted,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = sonaraTextFieldColors(),
            )
            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = {
                    if (newName.isBlank()) return@Button
                    val created = Playlist(id = UUID.randomUUID().toString(), name = newName.trim())
                    val updated = playlists + created.copy(trackIds = distinctIds)
                    state.store.savePlaylists(updated)
                    val auto = DownloadManager.maybeAutoDownload(
                        context, state.store, created.id, tracks.distinctBy { it.id },
                    )
                    showToast(
                        context,
                        if (distinctIds.size == 1) {
                            if (auto > 0) "Playlist created + 1 song saved · Downloading"
                            else "Playlist created + 1 song saved"
                        } else {
                            if (auto > 0) "Playlist created + ${distinctIds.size} songs saved · Downloading $auto"
                            else "Playlist created + ${distinctIds.size} songs saved"
                        },
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Create playlist + save") }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

/** Status muat panel komentar (state lokal panel, memori saja). */
private sealed interface CommentsUiState {
    data object Loading : CommentsUiState
    data object Local : CommentsUiState
    data class Error(val message: String) : CommentsUiState
    data class Loaded(
        val comments: List<CommentsApi.Comment>,
        val nextPageToken: String?,
        val commentsOff: Boolean,
    ) : CommentsUiState
}

/** Pesan error komentar yang ramah — JSON mentah tidak pernah sampai ke UI. */
private fun friendlyCommentsError(message: String?): String {
    val msg = message?.trim().orEmpty()
    if (msg.isBlank() || msg.contains("{") || msg.contains("JSONException")) {
        return "Couldn't load comments. Check your internet, then try again."
    }
    return msg
}

/**
 * Panel komentar — dibuka dari pill Comments di detail lagu.
 * Komentar YouTube Music beneran, read-only (v1.10.19): avatar,
 * @handle, waktu, teks verbatim, like-count singkatan verbatim,
 * penanda heart, balasan yang bisa dibuka. TIDAK ADA composer,
 * tombol like/balas, atau total count — posting/like/balas butuh
 * login, jadi tidak ditawarkan. Cache memori saja selama panel
 * terbuka; dibuka ulang = refetch.
 */
@Composable
fun CommentsPanel(
    track: Track,
    ytmApi: YoutubeMusicApi,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val videoId = track.youtubeVideoId
    var uiState by remember(videoId) { mutableStateOf<CommentsUiState>(CommentsUiState.Loading) }
    var loadingMore by remember(videoId) { mutableStateOf(false) }
    var loadMoreError by remember(videoId) { mutableStateOf<String?>(null) }
    var expanded by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    var replies by remember(videoId) { mutableStateOf<Map<String, List<CommentsApi.Comment>>>(emptyMap()) }
    var replyNext by remember(videoId) { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var replyLoading by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    var replyError by remember(videoId) { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun loadFirst() {
        if (track.source == Source.LOCAL || videoId.isNullOrBlank()) {
            uiState = CommentsUiState.Local
            return
        }
        uiState = CommentsUiState.Loading
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                CommentsApi.fetchComments(ytmApi, videoId)
            }
            uiState = result.fold(
                onSuccess = {
                    CommentsUiState.Loaded(it.comments, it.nextPageToken, it.commentsOff)
                },
                onFailure = { CommentsUiState.Error(friendlyCommentsError(it.message)) },
            )
        }
    }

    fun loadMore(token: String) {
        if (loadingMore) return
        loadingMore = true
        loadMoreError = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                CommentsApi.fetchMore(ytmApi, token)
            }
            loadingMore = false
            result.fold(
                onSuccess = { page ->
                    val cur = uiState as? CommentsUiState.Loaded ?: return@fold
                    uiState = cur.copy(
                        comments = cur.comments + page.comments,
                        nextPageToken = page.nextPageToken,
                    )
                },
                onFailure = { loadMoreError = friendlyCommentsError(it.message) },
            )
        }
    }

    fun loadReplies(comment: CommentsApi.Comment) {
        val id = comment.commentId
        val token = comment.replyContinuationToken ?: return
        if (replyLoading.contains(id)) return
        replyLoading = replyLoading + id
        replyError = replyError - id
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                CommentsApi.fetchReplies(ytmApi, token)
            }
            replyLoading = replyLoading - id
            result.fold(
                onSuccess = { page ->
                    replies = replies + (id to page.comments)
                    replyNext = replyNext + (id to page.nextPageToken)
                },
                onFailure = { replyError = replyError + (id to friendlyCommentsError(it.message)) },
            )
        }
    }

    fun loadMoreReplies(comment: CommentsApi.Comment) {
        val id = comment.commentId
        val token = replyNext[id] ?: return
        if (replyLoading.contains(id)) return
        replyLoading = replyLoading + id
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                CommentsApi.fetchMore(ytmApi, token)
            }
            replyLoading = replyLoading - id
            result.fold(
                onSuccess = { page ->
                    replies = replies + (id to ((replies[id] ?: emptyList()) + page.comments))
                    replyNext = replyNext + (id to page.nextPageToken)
                },
                onFailure = { replyError = replyError + (id to friendlyCommentsError(it.message)) },
            )
        }
    }

    fun toggleReplies(comment: CommentsApi.Comment) {
        val id = comment.commentId
        if (expanded.contains(id)) {
            expanded = expanded - id
            return
        }
        expanded = expanded + id
        if (!replies.containsKey(id)) loadReplies(comment)
    }

    fun retryReplies(comment: CommentsApi.Comment) {
        if ((replies[comment.commentId] ?: emptyList()).isEmpty()) {
            loadReplies(comment)
        } else {
            loadMoreReplies(comment)
        }
    }

    LaunchedEffect(videoId) { loadFirst() }

    ArusBottomSheet(onDismiss = onDismiss) {
        Text(
            "Comments",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SheetText,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            run { val a = displayArtist(track.artist); if (a.isNotBlank()) track.title + " — " + a else track.title },
            fontSize = 13.sp,
            color = SheetMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "From YouTube Music • Read-only",
            fontSize = 12.sp,
            color = SheetMuted,
        )
        Spacer(Modifier.height(Spacing.xl))
        when (val state = uiState) {
            CommentsUiState.Loading -> {
                Text(
                    "Loading comments…",
                    fontSize = 14.sp,
                    color = SheetMuted,
                )
                Spacer(Modifier.height(Spacing.xl))
            }
            CommentsUiState.Local -> {
                Text(
                    "Comments aren't available for local music.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetText,
                )
                Spacer(Modifier.height(Spacing.xl))
            }
            is CommentsUiState.Error -> {
                Text(
                    state.message,
                    fontSize = 14.sp,
                    color = SheetText,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Try again",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BurntOrange,
                    modifier = Modifier.clickable { loadFirst() },
                )
                Spacer(Modifier.height(Spacing.xl))
            }
            is CommentsUiState.Loaded -> {
                if (state.commentsOff) {
                    Text(
                        "Comments are turned off for this song.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SheetText,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "The uploader turned comments off, so there's nothing to show.",
                        fontSize = 13.sp,
                        color = SheetMuted,
                    )
                } else if (state.comments.isEmpty()) {
                    Text(
                        "No comments yet.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SheetText,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Comments come from YouTube Music — nothing is made up.",
                        fontSize = 13.sp,
                        color = SheetMuted,
                    )
                } else {
                    for (comment in state.comments) {
                        CommentRow(
                            comment = comment,
                            avatarSize = 36.dp,
                            onToggleReplies = if (comment.replyContinuationToken != null) {
                                { toggleReplies(comment) }
                            } else {
                                null
                            },
                            repliesExpanded = expanded.contains(comment.commentId),
                        )
                        if (expanded.contains(comment.commentId)) {
                            val reps = replies[comment.commentId] ?: emptyList()
                            val err = replyError[comment.commentId]
                            val loading = replyLoading.contains(comment.commentId)
                            Column(Modifier.padding(start = 46.dp)) {
                                Spacer(Modifier.height(Spacing.sm))
                                if (loading && reps.isEmpty()) {
                                    Text(
                                        "Loading replies…",
                                        fontSize = 13.sp,
                                        color = SheetMuted,
                                    )
                                }
                                if (err != null) {
                                    Text(
                                        err,
                                        fontSize = 13.sp,
                                        color = ThemeError,
                                    )
                                    Spacer(Modifier.height(Spacing.xs))
                                    Text(
                                        "Try again",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BurntOrange,
                                        modifier = Modifier.clickable { retryReplies(comment) },
                                    )
                                }
                                for (rep in reps) {
                                    CommentRow(
                                        comment = rep,
                                        avatarSize = 28.dp,
                                        onToggleReplies = null,
                                        repliesExpanded = false,
                                    )
                                    Spacer(Modifier.height(Spacing.md))
                                }
                                val moreToken = replyNext[comment.commentId]
                                if (moreToken != null) {
                                    TextButton(
                                        onClick = { loadMoreReplies(comment) },
                                        enabled = !loading,
                                        colors = sonaraTextButtonColors(),
                                    ) {
                                        Text(if (loading) "Loading…" else "Show more replies")
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(Spacing.lg))
                    }
                    val nextToken = state.nextPageToken
                    if (nextToken != null) {
                        if (loadMoreError != null) {
                            Text(
                                loadMoreError!!,
                                fontSize = 13.sp,
                                color = ThemeError,
                            )
                            Spacer(Modifier.height(Spacing.xs))
                        }
                        TextButton(
                            onClick = { loadMore(nextToken) },
                            enabled = !loadingMore,
                            colors = sonaraTextButtonColors(),
                        ) {
                            Text(if (loadingMore) "Loading…" else "Load more comments")
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

/** Satu baris komentar: avatar + @handle + waktu + teks + meta. */
@Composable
private fun CommentRow(
    comment: CommentsApi.Comment,
    avatarSize: Dp,
    onToggleReplies: (() -> Unit)?,
    repliesExpanded: Boolean,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        CommentAvatar(comment.avatarUrl, comment.authorHandle, avatarSize)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.authorHandle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.publishedTimeText.isNotBlank()) {
                    Text(
                        " • " + comment.publishedTimeText,
                        fontSize = 12.sp,
                        color = SheetMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            comment.pinnedText?.let { pinned ->
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = SheetMuted,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        pinned,
                        fontSize = 12.sp,
                        color = SheetMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                comment.text,
                fontSize = 14.sp,
                color = SheetText,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (comment.likeCountText != null) {
                    Icon(
                        Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        tint = SheetMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        comment.likeCountText,
                        fontSize = 12.sp,
                        color = SheetMuted,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                if (comment.hearted) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = BurntOrange,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Hearted",
                        fontSize = 12.sp,
                        color = SheetMuted,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                if (onToggleReplies != null) {
                    val label = if (repliesExpanded) {
                        "Hide replies"
                    } else if (!comment.replyCountText.isNullOrBlank()) {
                        "View ${comment.replyCountText} replies"
                    } else {
                        "View replies"
                    }
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BurntOrange,
                        modifier = Modifier.clickable { onToggleReplies() },
                    )
                }
            }
        }
    }
}

/** Avatar bulat: inisial @handle di bawah, foto Coil di atas bila ada. */
@Composable
private fun CommentAvatar(url: String?, handle: String, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(SegmentBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            handle.removePrefix("@").take(1).uppercase(),
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            color = SheetMuted,
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * Sheet History (v1.10.18) — pill "History" di slot toggle Song/Video.
 *
 * Isi = narasi/fakta dari data NYATA saja, teks VERBATIM, tiap blok
 * berlabel jenis + sumbernya. Blok yang datanya kosong tidak dibuat;
 * bila tidak ada blok sama sekali → honest empty state. Sheet ini
 * mengikuti pola CommentsPanel (ArusBottomSheet: handle, Back,
 * scroll vertikal) — pemutaran tidak terganggu.
 */
@Composable
fun HistoryPanel(
    track: Track,
    /** Null = masih memuat. */
    content: ArtistApi.HistoryContent?,
    onDismiss: () -> Unit,
) {
    ArusBottomSheet(onDismiss = onDismiss) {
        Text(
            "History",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SheetText,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            run { val a = displayArtist(track.artist); if (a.isNotBlank()) track.title + " — " + a else track.title },
            fontSize = 13.sp,
            color = SheetMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.xl))
        if (content == null) {
            Text(
                "Loading history…",
                fontSize = 14.sp,
                color = SheetMuted,
            )
        } else if (content.blocks.isEmpty()) {
            Text(
                "No history available for this song yet.",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SheetText,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "SONARA only shows history from the song's video " +
                    "description and the artist page — nothing is made up.",
                fontSize = 13.sp,
                color = SheetMuted,
            )
        } else {
            for (block in content.blocks) {
                Text(
                    block.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SheetMuted,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    block.text,
                    fontSize = 15.sp,
                    color = SheetText,
                )
                Spacer(Modifier.height(Spacing.xl))
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}
