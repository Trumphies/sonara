package com.sonara.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sonara.app.data.DownloadManager
import com.sonara.app.data.DownloadTaskState
import com.sonara.app.data.DownloadTasks
import com.sonara.app.data.Downloads
import com.sonara.app.data.Engine
import com.sonara.app.data.LibraryOrg
import com.sonara.app.data.LibraryOrgPolicy
import com.sonara.app.data.HomeLayout
import com.sonara.app.data.LayoutPolicy
import com.sonara.app.data.LocalFilesPolicy
import com.sonara.app.data.LocalFilesScan
import com.sonara.app.data.LocalFolderEntry
import com.sonara.app.data.LocalTagOverride
import com.sonara.app.data.LocalTrackGroup
import com.sonara.app.data.LocalMusic
import com.sonara.app.data.formatDurationMs
import com.sonara.app.data.PlayCount
import com.sonara.app.data.Playlist
import com.sonara.app.data.PlaylistDetailSort
import com.sonara.app.data.PlaylistFolder
import com.sonara.app.data.PlaylistNameProblem
import com.sonara.app.data.Source
import com.sonara.app.data.Track
import com.sonara.app.data.TrackDurations
import com.sonara.app.data.commitCoverFile
import com.sonara.app.data.deletePlaylistCover
import com.sonara.app.data.deletePlaylistFrom
import com.sonara.app.data.discardCoverFile
import com.sonara.app.data.mostPlayedArtists
import com.sonara.app.data.mostPlayedTracks
import com.sonara.app.data.moveTrackIds
import com.sonara.app.data.occurrenceKeys
import com.sonara.app.data.playlistCoverFile
import com.sonara.app.data.prunePlaylistCovers
import com.sonara.app.data.renamePlaylistIn
import com.sonara.app.data.selectionKey
import com.sonara.app.data.stagePlaylistCover
import com.sonara.app.data.validatePlaylistName
import com.sonara.app.player.PlayerManager
import com.sonara.app.ui.ArusBackButton
import com.sonara.app.ui.ArusSearchField
import com.sonara.app.ui.ChromeScrollEffect
import com.sonara.app.ui.ContentMargin
import com.sonara.app.ui.EmptyState
import com.sonara.app.ui.HapticKind
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.PrimaryButton
import com.sonara.app.ui.SecondaryButton
import com.sonara.app.ui.SectionHeader
import com.sonara.app.ui.sonaraTextButtonColors
import com.sonara.app.ui.sonaraTextFieldColors
import com.sonara.app.ui.SelectionActionBar
import com.sonara.app.ui.SelectionState
import com.sonara.app.ui.TrackRow
import com.sonara.app.ui.rememberSelectionState
import com.sonara.app.data.mergeTracks
import com.sonara.app.ui.rememberHaptic
import com.sonara.app.ui.theme.CardOffline
import com.sonara.app.ui.theme.CardSlate
import com.sonara.app.ui.theme.BurntOrange
import com.sonara.app.ui.theme.OnAccent
import com.sonara.app.ui.theme.ChromeBg
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.displayCased
import com.sonara.app.ui.theme.Density
import com.sonara.app.ui.hardShadowDraw
import com.sonara.app.ui.theme.GenrePop
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.theme.OnChrome
import com.sonara.app.ui.theme.PaperOutline
import com.sonara.app.ui.theme.PaperSurface
import com.sonara.app.ui.theme.PosterGold
import com.sonara.app.ui.theme.SegmentBg
import com.sonara.app.ui.theme.Spacing
import com.sonara.app.ui.theme.Type
import com.sonara.app.ui.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Photo-picker launcher for a playlist cover (v1.10.41): the system
 * photo picker, images only — no storage permission needed. The
 * picked Uri is handed to [onPicked]; copying it into app-private
 * storage (staging) happens at the call site, off the main thread.
 */
@Composable
private fun rememberCoverPickerLauncher(
    onPicked: (Uri) -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onPicked(uri)
    }
    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
}

/**
 * A playlist's cover file, if one exists on this device. [tick]
 * forces a re-read after a cover is committed/removed (file
 * existence is not observable state). A null id = no cover.
 */
@Composable
private fun rememberPlaylistCover(
    context: Context,
    playlistId: String?,
    tick: Int,
): File? {
    return remember(playlistId, tick) {
        playlistId?.let { playlistCoverFile(context, it).takeIf { file -> file.exists() } }
    }
}

/**
 * Coil cache key for a cover file: path + last-modified, so
 * replacing a cover at the same path never shows a stale cached
 * image (Coil keys File data by path alone).
 */
private fun coverCacheKey(cover: File?): String? =
    cover?.let { "${it.absolutePath}:${it.lastModified()}" }

/**
 * Cover pick row shared by the create + rename dialogs (v1.10.41):
 * 56dp preview (staged image, existing cover, or placeholder icon),
 * "Choose image" + an optional remove action. Styled with the
 * dialog's MaterialTheme + the sonara helpers (v1.10.40).
 */
@Composable
private fun CoverPickRow(
    previewFile: File?,
    onChooseImage: () -> Unit,
    onRemove: (() -> Unit)?,
    removeLabel: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(Corners.rowCover))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (previewFile != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(previewFile)
                        .memoryCacheKey(coverCacheKey(previewFile))
                        .diskCacheKey(coverCacheKey(previewFile))
                        .crossfade(300)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Cover image (optional)", fontSize = 14.sp)
            Text(
                "Shown on the playlist card.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onChooseImage,
            colors = sonaraTextButtonColors(),
        ) { Text("Choose image") }
        if (onRemove != null) {
            TextButton(
                onClick = onRemove,
                colors = sonaraTextButtonColors(),
            ) { Text(removeLabel) }
        }
    }
}

/**
 * Daftar antrean — dipakai layar Antrean DAN panel UP NEXT di detail
 * lagu. Baris format screenshot, lagu yang diputar ber-equalizer.
 *
 * Drag-to-reorder (v1.6): tahan baris sampai terangkat (getar),
 * seret ke posisi baru, lepas — urutan baru langsung dipakai
 * pemutar via onMove. Kunci baris = queueUid (stabil saat urutan
 * berubah). Auto-scroll tepi viewport aktif saat drag.
 */
@Composable
fun QueueList(
    queue: List<Track>,
    currentIndex: Int,
    isPlaying: Boolean,
    onPlayAt: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
    animateIn: Boolean = false,
    onMove: ((Int, Int) -> Unit)? = null,
    /** Swipe-hapus (v1.10.16): (indeks, lagu) yang digeser. Null = swipe nonaktif. */
    onSwipeRemove: ((Int, Track) -> Unit)? = null,
    /** Swipe nonaktif selama sesi Jam (antrean sesi read-only untuk tamu; antrean lokal bukan antrean sesi). */
    swipeToRemoveEnabled: Boolean = true,
    /** Multi-select (v1.10.21): status milik pemanggil; null = tidak ada seleksi. */
    selectionState: SelectionState? = null,
    /** Kunci seleksi stabil per baris (default: queueUid). */
    selectionKeyFor: ((Int, Track) -> String)? = null,
    /** Durasi per videoId (v1.10.31) — diteruskan ke TrackRow. */
    durations: TrackDurations? = null,
) {
    if (queue.isEmpty()) {
        EmptyState(
            title = "Queue is empty",
            subtitle = "Play a song from Home or Search — YouTube Music " +
                "and local songs can mix in one queue.",
        )
        return
    }
    val haptic = rememberHaptic()
    val lazyListState = rememberLazyListState()
    var draggingUid by remember { mutableStateOf<String?>(null) }
    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var prevItemOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(draggingUid) {
        while (draggingUid != null) {
            delay(16)
            val info = lazyListState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == draggingUid }
                ?: continue
            val center = item.offset + item.size / 2 + dragOffsetY
            val viewportH = info.viewportSize.height
            when {
                center < 80 -> lazyListState.scrollBy(-18f)
                center > viewportH - 80 -> lazyListState.scrollBy(18f)
            }
        }
    }

    LazyColumn(modifier, state = lazyListState) {
        itemsIndexed(queue, key = { _, t -> t.queueUid }) { index, track ->
            val isCurrent = index == currentIndex
            val dragging = draggingUid == track.queueUid
            // v1.10.21: seleksi — tap = toggle saat mode aktif,
            // drag + swipe nonaktif selama mode aktif.
            val selKey = selectionKeyFor?.invoke(index, track) ?: track.queueUid
            val selecting = selectionState?.modeActive == true
            val isSelected = selectionState?.isSelected(selKey) == true
            Column(
                modifier = Modifier.then(
                    if (dragging) Modifier else Modifier.animateItem(),
                ),
            ) {
                if (isCurrent) {
                    Text(
                        "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        // v1.10.40: aksen teks (dulu colorScheme.primary/
                        // accentFill — 2,29:1 di GETAR Light).
                        color = BurntOrange,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )
                }
                // v1.10.16: swipe-hapus — lagu yang sedang diputar
                // TIDAK bisa digeser (barisnya tidak merespons swipe);
                // baris lain digeser ke kiri untuk menghapus + Undo
                // lewat snackbar di root. Modifier seret-tetap
                // (long-press reorder) tinggal di dalam konten.
                val rowModifier = Modifier
                    .graphicsLayer {
                        translationY = if (dragging) dragOffsetY else 0f
                        scaleX = if (dragging) 1.03f else 1f
                        scaleY = if (dragging) 1.03f else 1f
                    }
                    .shadow(
                        if (dragging) 8.dp else 0.dp,
                        RoundedCornerShape(Corners.small),
                    )
                    .zIndex(if (dragging) 1f else 0f)
                    .pointerInput(track.queueUid, selecting) {
                        // v1.10.21: seret nonaktif selama mode seleksi
                        // (kunci pointerInput menyertakan `selecting`
                        // supaya detector di-restart saat mode berubah).
                        // v1.10.36: lagu yang sedang diputar DIPIN —
                        // tidak bisa digeser (konsisten dengan
                        // pemblokiran swipe-hapus barisnya, v1.10.16);
                        // memindah lagu lain melewati posisinya tetap
                        // menggeser currentIndex lewat moveQueue.
                        if (onMove == null || selecting || isCurrent) return@pointerInput
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingUid = track.queueUid
                                dragIndex = index
                                dragOffsetY = 0f
                                prevItemOffset = lazyListState.layoutInfo
                                    .visibleItemsInfo
                                    .firstOrNull { it.key == track.queueUid }
                                    ?.offset?.toFloat() ?: 0f
                                haptic(HapticKind.LongPress)
                            },
                            onDragEnd = {
                                draggingUid = null
                                dragIndex = -1
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggingUid = null
                                dragIndex = -1
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val info = lazyListState.layoutInfo
                                val item = info.visibleItemsInfo
                                    .firstOrNull { it.key == track.queueUid }
                                    ?: return@detectDragGesturesAfterLongPress
                                dragOffsetY += prevItemOffset - item.offset
                                prevItemOffset = item.offset.toFloat()
                                val centerPx =
                                    (item.offset + item.size / 2 + dragOffsetY).toInt()
                                val target = info.visibleItemsInfo
                                    .firstOrNull { candidate ->
                                        candidate.key != track.queueUid &&
                                            centerPx in candidate.offset..<
                                                (candidate.offset + candidate.size)
                                    }
                                    ?.index
                                if (target != null && target != dragIndex) {
                                    haptic(HapticKind.Tap)
                                    onMove(dragIndex, target)
                                    dragIndex = target
                                }
                            },
                        )
                    }
                val rowContent: @Composable () -> Unit = {
                    TrackRow(
                        chartNumber = index + 1,
                        track = track,
                        durations = durations,
                        onClick = {
                            if (selecting) selectionState?.toggle(selKey)
                            else onPlayAt(index)
                        },
                        isCurrent = isCurrent,
                        isPlaying = isPlaying,
                        selected = isSelected,
                        animateIn = animateIn,
                        index = index,
                        showDivider = false,
                        trailing = {
                            IconButton(onClick = { onRemove(index) }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove from queue",
                                )
                            }
                        },
                        modifier = rowModifier,
                    )
                }
                val canSwipe = swipeToRemoveEnabled && onSwipeRemove != null && !isCurrent && !selecting
                if (canSwipe) {
                    com.sonara.app.ui.SwipeToDismissDelete(
                        onDismissed = { onSwipeRemove!!(index, track) },
                    ) {
                        rowContent()
                    }
                } else {
                    rowContent()
                }
            }
        }
    }
}

@Composable
fun QueueScreen(
    state: SonaraState,
    onBack: () -> Unit,
    /** Undo swipe-hapus (v1.10.16): root menampilkan snackbar Undo. */
    onQueueItemRemoved: (Track, Int) -> Unit = { _, _ -> },
) {
    val queue by state.player.queue.collectAsState()
    val currentIndex by state.player.currentIndex.collectAsState()
    val isPlaying by state.player.isPlaying.collectAsState()
    val shuffle by state.player.shuffle.collectAsState()
    val repeat by state.player.repeat.collectAsState()
    // v1.10.16: swipe-hapus nonaktif selama sesi Jam aktif — antrean
    // sesi milik sesi (RTDB), bukan antrean lokal; tamu read-only,
    // host pun tidak menghapus antrean sesi dari layar ini.
    val jamUiState by state.jam.uiState.collectAsState()
    val swipeEnabled = !jamUiState.active
    // v1.10.21: multi-select — masuk lewat tombol "Select" (long-press
    // sudah dipakai drag-to-reorder di daftar ini).
    val context = LocalContext.current
    val selection = rememberSelectionState()
    var bulkSaveTracks by remember { mutableStateOf<List<Track>?>(null) }
    BackHandler(enabled = selection.modeActive) { selection.exitMode() }
    // Antrean berubah (main baru / tambah / hapus) = seleksi basi.
    LaunchedEffect(queue.map { it.queueUid }) {
        if (selection.modeActive) selection.exitMode()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // v1.10.1: tombol Back ARUS bersama (sebelumnya IconButton polos).
            ArusBackButton(onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Text(
                displayCased("Queue", CurrentTokens),
                fontSize = Type.screenTitle,
                fontFamily = CurrentTokens.displayFont,
                letterSpacing = CurrentTokens.displayLetterSpacing,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            "${queue.size} songs",
            fontSize = Type.artist,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextButton(
                onClick = { state.player.toggleShuffle() },
                modifier = Modifier.height(48.dp),
                colors = sonaraTextButtonColors(),
            ) { Text(if (shuffle) "Shuffle: ON" else "Shuffle: OFF") }
            TextButton(
                onClick = { state.player.cycleRepeat() },
                modifier = Modifier.height(48.dp),
                colors = sonaraTextButtonColors(),
            ) {
                Text(
                    when (repeat) {
                        PlayerManager.RepeatMode.OFF -> "Repeat: Off"
                        PlayerManager.RepeatMode.ALL -> "Repeat: All"
                        PlayerManager.RepeatMode.ONE -> "Repeat: One"
                    },
                )
            }
            TextButton(
                onClick = { state.player.clearQueue() },
                modifier = Modifier.height(48.dp),
                colors = sonaraTextButtonColors(),
            ) { Text("Clear") }
            if (queue.isNotEmpty()) {
                TextButton(
                    onClick = {
                        if (selection.modeActive) selection.exitMode()
                        else selection.enterMode()
                    },
                    modifier = Modifier.height(48.dp),
                    colors = sonaraTextButtonColors(),
                ) { Text("Select") }
            }
        }
        if (selection.modeActive) {
            SelectionActionBar(
                count = selection.count,
                jamActive = jamUiState.active,
                onAddToQueue = {
                    val selected = queue.filter { selection.isSelected(it.queueUid) }
                    val added = state.player.addTracksToQueue(selected)
                    showToast(
                        context,
                        if (added == 1) "Added 1 song to queue"
                        else "Added $added songs to queue",
                    )
                    selection.exitMode()
                },
                onSaveToPlaylist = {
                    bulkSaveTracks = queue.filter { selection.isSelected(it.queueUid) }
                },
                onExit = { selection.exitMode() },
            )
        }
        QueueList(
            queue = queue,
            currentIndex = currentIndex,
            isPlaying = isPlaying,
            onPlayAt = { state.player.playAt(it) },
            onRemove = { state.player.removeFromQueue(it) },
            modifier = Modifier.fillMaxSize(),
            onMove = { from, to -> state.player.moveQueue(from, to) },
            onSwipeRemove = { index, track ->
                state.player.removeFromQueue(index)
                onQueueItemRemoved(track, index)
            },
            swipeToRemoveEnabled = swipeEnabled,
            selectionState = selection,
            selectionKeyFor = { _, t -> t.queueUid },
            durations = state.durations,
        )
        bulkSaveTracks?.let { tracks ->
            SaveTracksToPlaylistSheet(
                tracks = tracks,
                state = state,
                onDismiss = {
                    bulkSaveTracks = null
                    selection.exitMode()
                },
            )
        }
    }
}

private sealed interface CollectionView {
    data object Liked : CollectionView
    data object Offline : CollectionView
    /** Lagu yang tidak disukai (v1.0.3, lokal saja, terbaru dulu). */
    data object Disliked : CollectionView
    data class LocalPlaylist(val playlist: Playlist) : CollectionView
}

/** Urutan daftar Disliked (v1.0.3): terbaru dulu / judul / artis. */
private enum class DislikedSort { NEWEST, TITLE, ARTIST }

/**
 * Koleksi persis screenshot ground truth v1.7:
 * headline "Koleksi lu, udah kecampur." + subteks, grid kartu 2×2
 * (Disukai / Download Offline / 2 playlist ASLI user — atau kartu
 * "Buat playlist" yang jujur bila belum ada), lalu "Most played"
 * (hitungan putar NYATA dari v1.10.16) + "Terakhir diputar"
 * (riwayat putar NYATA di perangkat ini) + baris lagu format
 * screenshot. Musik lokal tetap ada di bawah sebagai seksi ringkas.
 * Semua angka = data asli (jumlah lagu, MB download, hitungan putar).
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LibraryScreen(
    state: SonaraState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    /** Buka Cari dengan query terisi (dipakai tap artis "Most played"). */
    onOpenSearch: (String?) -> Unit = {},
    /** Buka halaman acara podcast (v1.0.5, dari seksi acara diikuti). */
    onOpenPodcastShow: ((String, String) -> Unit)? = null,
    /** Buka halaman statistik (v1.0.6). */
    onOpenStats: () -> Unit = {},
    /** Auto-hide chrome bawah (v1.10.18, hanya list root). */
    onChromeScroll: ((Int, Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mostPlayedCounts = remember { state.store.playCounts() }
    // v1.10.16: 5 lagu + 3 artis teratas dari hitungan putar nyata.
    // Tanpa klaim rentang waktu — hitungan tercatat sejak versi ini
    // (riwayat lama tidak punya hitungan/timestamp yang bisa
    // dipakai); daftar kosong = empty state jujur.
    val topTracks = remember(mostPlayedCounts) {
        mostPlayedTracks(mostPlayedCounts, 5)
    }
    val topArtists = remember(mostPlayedCounts) {
        mostPlayedArtists(mostPlayedCounts, 3)
    }
    var newPlaylistName by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    // v1.10.41: cover playlist (lokal saja). coverTick memaksa baca
    // ulang keberadaan file cover setelah commit/hapus (file bukan
    // state observable); staged cover dipegang sampai dialog
    // dikonfirmasi (commit) atau ditutup (buang).
    var coverTick by remember { mutableIntStateOf(0) }
    var createStagedCover by remember { mutableStateOf<File?>(null) }
    var renameStagedCover by remember { mutableStateOf<File?>(null) }
    var renameRemoveCover by remember { mutableStateOf(false) }
    fun stagePickedCover(uri: Uri, onStaged: (File?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val staged = stagePlaylistCover(context, uri)
            withContext(Dispatchers.Main) {
                if (staged == null) showToast(context, "Couldn't use that image.")
                onStaged(staged)
            }
        }
    }
    val launchCreateCoverPicker = rememberCoverPickerLauncher { uri ->
        stagePickedCover(uri) { staged ->
            if (staged != null) createStagedCover = staged
        }
    }
    val launchRenameCoverPicker = rememberCoverPickerLauncher { uri ->
        stagePickedCover(uri) { staged ->
            if (staged != null) {
                renameStagedCover = staged
                renameRemoveCover = false
            }
        }
    }
    // v1.0.7: status file lokal — DIDEKLARASIKAN SEBELUM scanLocal /
    // pemilih folder (variabel lokal harus dideklarasi sebelum dipakai).
    // Grup duplikat + berkas hilang + folder pilihan user + override
    // tag. localTracks dipertahankan (sumber daftar lain) = track
    // efektif tiap grup.
    var localTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var localScanned by remember { mutableStateOf(false) }
    var localGroups by remember { mutableStateOf<List<LocalTrackGroup>>(emptyList()) }
    var localMissingCount by remember { mutableStateOf(0) }
    var localFoldersVersion by remember { mutableStateOf(0) }
    var hideMissingLocal by remember { mutableStateOf(state.store.hideMissingLocal) }
    var tagEditTrack by remember { mutableStateOf<Track?>(null) }
    var tagEditTitle by remember { mutableStateOf("") }
    var tagEditArtist by remember { mutableStateOf("") }
    var tagEditAlbum by remember { mutableStateOf("") }
    var copiesGroup by remember { mutableStateOf<LocalTrackGroup?>(null) }
    var missingInfoTrack by remember { mutableStateOf<Track?>(null) }
    val localFoldersFresh = remember(localFoldersVersion) { state.store.localFolders() }
    // v1.0.7: scan gabungan — MediaStore + folder SAF pilihan user,
    // override tag + sumber pilihan + penandaan berkas hilang.
    // Dipanggil saat Koleksi dibuka, saat Rescan, dan setelah folder /
    // override / preferensi berubah. Tanpa background watcher.
    fun scanLocal() {
        scope.launch {
            val folders = state.store.localFolders()
            val overrides = state.store.localTagOverrides()
            val preferred = state.store.localPreferredSources()
            val hide = state.store.hideMissingLocal
            val (groups, missing) = withContext(Dispatchers.IO) {
                LocalFilesScan.scanAll(context, folders, overrides, preferred, hide)
            }
            localGroups = groups
            localMissingCount = missing.size
            localTracks = groups.map { it.effective }
            localScanned = true
        }
    }
    // v1.0.7: pemilih folder musik (SAF) — izin baca persisten,
    // folder disimpan lokal, lalu scan ulang.
    val launchFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            state.store.addLocalFolder(
                LocalFolderEntry(
                    uriString = uri.toString(),
                    displayName = LocalFilesPolicy.folderDisplayName(uri.toString()),
                ),
            )
            localFoldersVersion++
            scanLocal()
        }
    }
    var openView by remember { mutableStateOf<CollectionView?>(null) }
    // v1.0.3: versi daftar tidak disukai — Restore di tampilan
    // detail menaikkan versi supaya daftar dibaca ulang dari store.
    var dislikedVersion by remember { mutableStateOf(0) }
    // v1.10.21: multi-select daftar root (Most played / Recently
    // played / Local music) — satu status, kunci ber-scope per daftar.
    val librarySelection = rememberSelectionState()
    var libraryBulkSave by remember { mutableStateOf<List<Track>?>(null) }
    val libraryJamState by state.jam.uiState.collectAsState()
    BackHandler(enabled = librarySelection.modeActive && openView == null) {
        librarySelection.exitMode()
    }
    var playlists by remember { mutableStateOf(state.store.playlists()) }
    // v1.0.6: organisasi Koleksi (pin/folder/tag/auto-download, lokal
    // saja, tidak disinkron). orgVersion memaksa baca ulang dari store
    // setiap ada perubahan (prefs bukan state observable).
    var orgVersion by remember { mutableStateOf(0) }
    val org = remember(orgVersion) { state.store.playlistOrg() }
    val orderedPlaylists = remember(playlists, org) {
        LibraryOrgPolicy.applyOrgToPlaylists(playlists, org)
    }
    // v1.0.7: layout Koleksi dibaca segar dari store (pilihan Settings).
    val libraryLayout = LayoutPolicy.fromStorage(state.store.homeLayout)
    fun saveOrg(updated: LibraryOrg) {
        state.store.savePlaylistOrg(updated)
        orgVersion++
    }
    // v1.0.6: tugas unduhan (progres/jeda/lanjut) — state flow dari
    // DownloadManager, dipakai tampilan Offline.
    val downloadTasks by DownloadManager.tasks.collectAsState()
    LaunchedEffect(Unit) { DownloadManager.ensureLoaded(context) }
    // v1.0.6: filter tag + dialog folder di daftar root.
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderRenameTarget by remember { mutableStateOf<PlaylistFolder?>(null) }
    var folderRenameText by remember { mutableStateOf("") }
    var folderDeleteTarget by remember { mutableStateOf<PlaylistFolder?>(null) }
    val likedTracks = remember { state.store.likedTracks() }
    val recentTracks = remember { state.store.recentTracks() }
    // v1.0.5: acara podcast yang diikuti (lokal saja) — dibaca ulang
    // tiap follow/unfollow lewat podcastFollowVersion.
    val followedShows = remember(state.podcastFollowVersion) {
        state.store.followedShows()
    }

    val downloads = remember { Downloads(context, state.store.audioQuality) }
    var downloadedTracks by remember { mutableStateOf(downloads.listTracks()) }
    var downloadBytes by remember { mutableStateOf(downloads.totalBytes()) }
    // Snapshot track dari cloud (playlist perangkat lain) — supaya
    // lagu-lagunya tampil + bisa diputar walau belum dikenal lokal.
    val knownTracks = remember { state.store.knownTracks() }

    fun refresh() {
        playlists = state.store.playlists()
        // v1.10.41: buang file cover yang playlist-nya sudah tidak ada
        // (mis. terhapus lewat sinkron cloud) — cover milik playlist
        // yang masih hidup tidak pernah disentuh.
        prunePlaylistCovers(context, playlists.map { it.id })
        // v1.0.6: buang state org milik playlist yang sudah tidak ada.
        state.store.prunePlaylistOrg(playlists.map { it.id })
        orgVersion++
        downloadedTracks = downloads.listTracks()
        downloadBytes = downloads.totalBytes()
    }

    LaunchedEffect(Unit) {
        scanLocal()
    }

    // ---- Tampilan detail satu koleksi (kartu diketik) ----
    // v1.10.1: Back sistem dari tampilan detail menutup tampilan
    // dulu, bukan keluar dari Koleksi. v1.10.21: Back saat mode
    // seleksi aktif keluar dari mode dulu.
    val detailSelection = remember(openView) { SelectionState() }
    var detailBulkSave by remember { mutableStateOf<List<Track>?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var showDeletePlaylist by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var renameProblem by remember { mutableStateOf<PlaylistNameProblem?>(null) }
    val detailJamState by state.jam.uiState.collectAsState()
    BackHandler(enabled = openView != null) {
        if (detailSelection.modeActive) detailSelection.exitMode() else openView = null
    }
    val view = openView
    if (view != null) {
        // v1.0.3: daftar tidak disukai dibaca segar dari store tiap
        // versi berubah (Restore) — tanpa remember basi.
        val dislikedFresh = remember(dislikedVersion) { state.store.dislikedTracks() }
        // v1.0.3: cari + urut tampilan Disliked (reset tiap ganti view).
        var dislikedQuery by remember(view) { mutableStateOf("") }
        var dislikedSort by remember(view) { mutableStateOf(DislikedSort.NEWEST) }
        val playlistView = view as? CollectionView.LocalPlaylist
        // Playlist segar dari store (snapshot openView bisa basi
        // setelah rename/reorder) — sumber kebenaran urutan + nama.
        val livePlaylist = playlistView?.let { pv ->
            playlists.find { it.id == pv.playlist.id }
        } ?: playlistView?.playlist
        // v1.10.41: cover playlist ini (lokal saja; null = tanpa cover
        // atau bukan tampilan playlist — Liked/Offline tidak punya).
        val detailCover = rememberPlaylistCover(context, livePlaylist?.id, coverTick)
        // v1.0.6: cari + urut + dialog organize khusus tampilan
        // playlist (reset tiap ganti view).
        var playlistQuery by remember(view) { mutableStateOf("") }
        var playlistSort by remember(view) { mutableStateOf(PlaylistDetailSort.DATE_ADDED) }
        var showOrganize by remember(view) { mutableStateOf(false) }
        var newTagText by remember(view) { mutableStateOf("") }
        val allKnown = likedTracks + downloadedTracks + localTracks + recentTracks + knownTracks
        val (title, tracks, isOffline) = when (view) {
            CollectionView.Liked -> {
                val merged = mergeTracks(likedTracks).map { it.first }
                Triple("Liked songs", merged, false)
            }
            CollectionView.Offline -> Triple("Offline downloads", downloadedTracks, true)
            CollectionView.Disliked -> {
                // Terbaru dulu = urutan simpan store (setDislikedTrack
                // menaruh yang baru di depan).
                val merged = mergeTracks(dislikedFresh).map { it.first }
                Triple("Disliked songs", merged, false)
            }
            is CollectionView.LocalPlaylist -> {
                val resolved = (livePlaylist?.trackIds.orEmpty()).mapNotNull { id ->
                    allKnown.find { it.id == id }
                }
                Triple(livePlaylist?.name ?: view.playlist.name, resolved, false)
            }
        }
        // Entri tampil: (indeks di trackIds, track, kunci kemunculan).
        // Kunci kemunculan stabil saat reorder (kemunculan ke-k dari
        // satu id tetap ke-k) dan unik walau lagu yang sama ada dua
        // kali (playlist memang mengizinkannya — v1.10.19).
        // v1.0.3: tampilan Disliked memakai daftar yang sudah
        // disaring/diurut (cari + urut); tampilan lain tidak berubah.
        val shownTracks = if (view is CollectionView.Disliked) {
            val q = dislikedQuery.trim().lowercase()
            val filtered = if (q.isBlank()) {
                tracks
            } else {
                tracks.filter {
                    it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q)
                }
            }
            when (dislikedSort) {
                DislikedSort.NEWEST -> filtered
                DislikedSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
                DislikedSort.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            }
        } else {
            tracks
        }
        val fullIds: List<String> = livePlaylist?.trackIds.orEmpty()
        val fullKeys = remember(fullIds) { occurrenceKeys(fullIds) }
        val detailEntries: List<Triple<Int, Track, String>> = if (playlistView != null) {
            fullIds.mapIndexedNotNull { idx, id ->
                allKnown.find { it.id == id }?.let { track -> Triple(idx, track, fullKeys[idx]) }
            }
        } else {
            val displayKeys = occurrenceKeys(shownTracks.map { it.id })
            shownTracks.mapIndexed { idx, track -> Triple(idx, track, displayKeys[idx]) }
        }
        // v1.0.6: entri tampil playlist = saring + urut. Lagu yang sama
        // boleh muncul dua kali — penyambungan kembali memakai antrean
        // kemunculan per id supaya kunci + indeks aslinya tepat.
        val playCountMap = remember(mostPlayedCounts) {
            mostPlayedCounts.associate { it.track.id to it.count }
        }
        val playlistShownEntries: List<Triple<Int, Track, String>> = if (playlistView != null) {
            val filteredTracks = LibraryOrgPolicy.filterPlaylistEntries(
                detailEntries.map { it.second }, playlistQuery,
            )
            val sortedTracks = LibraryOrgPolicy.sortPlaylistEntries(
                filteredTracks, playlistSort, playCountMap,
            )
            val queues = detailEntries.groupBy({ it.second.id }, { it })
                .mapValues { ArrayDeque(it.value) }
            sortedTracks.mapNotNull { queues[it.id]?.removeFirstOrNull() }
        } else {
            detailEntries
        }
        val shownDetailEntries = if (playlistView != null) playlistShownEntries else detailEntries
        // Drag-reorder hanya aman saat urutan tampil = urutan simpan
        // (tanpa cari/urut): kalau tidak, indeks tampil tidak memetakan
        // ke indeks trackIds dan move akan salah sasaran.
        val reorderEnabled = playlistView != null && playlistQuery.isBlank() &&
            playlistSort == PlaylistDetailSort.DATE_ADDED
        val isEmptyView = if (playlistView != null) {
            shownDetailEntries.isEmpty()
        } else {
            shownTracks.isEmpty()
        }
        val playAllTracks = if (playlistView != null) {
            shownDetailEntries.map { it.second }
        } else {
            shownTracks
        }
        LaunchedEffect(view, shownTracks.map { it.id }) {
            if (detailSelection.modeActive) detailSelection.exitMode()
        }
        // Referensi segar untuk gesture drag yang sedang berjalan
        // (closure pointerInput tidak di-restart tiap recomposition).
        val latestEntries by rememberUpdatedState(detailEntries)
        val latestLivePlaylist by rememberUpdatedState(livePlaylist)
        val applyPlaylistMove: (Int, Int) -> Unit = { fromDisplay, toDisplay ->
            val entries = latestEntries
            val fromIdx = entries.getOrNull(fromDisplay)?.first
            val toIdx = entries.getOrNull(toDisplay)?.first
            val current = latestLivePlaylist
            if (fromIdx != null && toIdx != null && current != null) {
                val newIds = moveTrackIds(current.trackIds, fromIdx, toIdx)
                if (newIds != current.trackIds) {
                    val updated = current.copy(trackIds = newIds)
                    val updatedList = state.store.playlists()
                        .map { if (it.id == updated.id) updated else it }
                    state.store.savePlaylists(updatedList)
                    playlists = updatedList
                    openView = CollectionView.LocalPlaylist(updated)
                }
            }
        }
        // Status drag (hanya playlist; mirror pola QueueList).
        val haptic = rememberHaptic()
        val detailListState = rememberLazyListState()
        var draggingKey by remember(view) { mutableStateOf<String?>(null) }
        var dragDisplayIndex by remember(view) { mutableStateOf(-1) }
        var dragOffsetY by remember(view) { mutableFloatStateOf(0f) }
        var prevItemOffset by remember(view) { mutableFloatStateOf(0f) }
        LaunchedEffect(draggingKey) {
            while (draggingKey != null) {
                delay(16)
                val info = detailListState.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.key == draggingKey }
                    ?: continue
                val center = item.offset + item.size / 2 + dragOffsetY
                val viewportH = info.viewportSize.height
                when {
                    center < 80 -> detailListState.scrollBy(-18f)
                    center > viewportH - 80 -> detailListState.scrollBy(18f)
                }
            }
        }
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // v1.10.1: tombol Back ARUS bersama.
                ArusBackButton(onClick = {
                    if (detailSelection.modeActive) detailSelection.exitMode()
                    else openView = null
                })
                Spacer(Modifier.width(8.dp))
                // v1.10.41: cover playlist di header detail (hanya
                // playlist buatan user yang punya cover).
                if (playlistView != null && detailCover != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(detailCover)
                            .memoryCacheKey(coverCacheKey(detailCover))
                            .diskCacheKey(coverCacheKey(detailCover))
                            .crossfade(300)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(Corners.rowCover)),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    title,
                    fontSize = Type.screenTitle,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (detailEntries.isNotEmpty()) {
                    TextButton(onClick = {
                        if (detailSelection.modeActive) detailSelection.exitMode()
                        else detailSelection.enterMode()
                    }, colors = sonaraTextButtonColors()) { Text("Select") }
                }
                if (playlistView != null) {
                    IconButton(onClick = { showOrganize = true }) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Organize playlist",
                        )
                    }
                    IconButton(onClick = {
                        renameText = livePlaylist?.name ?: title
                        renameProblem = null
                        // v1.10.41: edit cover mulai bersih tiap buka.
                        renameStagedCover = null
                        renameRemoveCover = false
                        showRename = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Rename playlist",
                        )
                    }
                    IconButton(onClick = { showDeletePlaylist = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete playlist",
                        )
                    }
                }
            }
            if (detailSelection.modeActive) {
                SelectionActionBar(
                    count = detailSelection.count,
                    jamActive = detailJamState.active,
                    onAddToQueue = {
                        val selected = detailEntries
                            .filter { detailSelection.isSelected(selectionKey("collection", it.third)) }
                            .map { it.second }
                        val added = state.player.addTracksToQueue(selected)
                        showToast(
                            context,
                            if (added == 1) "Added 1 song to queue"
                            else "Added $added songs to queue",
                        )
                        detailSelection.exitMode()
                    },
                    onSaveToPlaylist = {
                        detailBulkSave = detailEntries
                            .filter { detailSelection.isSelected(selectionKey("collection", it.third)) }
                            .map { it.second }
                    },
                    onExit = { detailSelection.exitMode() },
                )
            }
            // v1.0.3: cari + urut khusus tampilan Disliked — di atas
            // daftar, di bawah header; tampilan lain tidak berubah.
            if (view is CollectionView.Disliked) {
                ArusSearchField(
                    value = dislikedQuery,
                    onValueChange = { dislikedQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (sort in DislikedSort.entries) {
                        val isSelected = sort == dislikedSort
                        Box(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(Corners.chip))
                                .background(if (isSelected) ChromeBg else PaperSurface)
                                .border(
                                    CurrentTokens.cardBorderWidth,
                                    PaperOutline,
                                    RoundedCornerShape(Corners.chip),
                                )
                                .clickable { dislikedSort = sort }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                when (sort) {
                                    DislikedSort.NEWEST -> "Newest"
                                    DislikedSort.TITLE -> "Title"
                                    DislikedSort.ARTIST -> "Artist"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) OnChrome else InkBlack,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (playlistView != null) {
                ArusSearchField(
                    value = playlistQuery,
                    onValueChange = { playlistQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentMargin),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentMargin),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val sortLabels = listOf(
                        PlaylistDetailSort.DATE_ADDED to "Date added",
                        PlaylistDetailSort.TITLE to "Title",
                        PlaylistDetailSort.ARTIST to "Artist",
                        PlaylistDetailSort.PLAYS to "Most played",
                    )
                    for ((sort, label) in sortLabels) {
                        val selected = playlistSort == sort
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(Corners.rowCover))
                                .background(if (selected) ChromeBg else PaperSurface)
                                .border(
                                    1.dp,
                                    if (selected) ChromeBg else PaperOutline,
                                    RoundedCornerShape(Corners.rowCover),
                                )
                                .clickable { playlistSort = sort }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                color = if (selected) OnChrome else InkBlack,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (isEmptyView) {
                Text(
                    "This collection is still empty.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                PrimaryButton(
                    text = "Play all",
                    onClick = { state.player.playQueue(playAllTracks, 0) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(Modifier.fillMaxSize(), state = detailListState) {
                // v1.0.6: unduhan aktif di tampilan Offline — progres,
                // jeda/lanjut, coba lagi, hapus. Drag-reorder hanya
                // aktif di tampilan playlist, jadi item tambahan ini
                // tidak menggeser indeks drag playlist.
                if (isOffline) {
                    val activeTasks = downloadTasks.filter {
                        it.state != DownloadTaskState.COMPLETED
                    }
                    if (activeTasks.isNotEmpty()) {
                        item {
                            ActiveDownloadsSection(tasks = activeTasks)
                        }
                    }
                }
                itemsIndexed(shownDetailEntries, key = { _, e -> e.third }) { displayIndex, entry ->
                    val (_, track, entryKey) = entry
                    val dragging = draggingKey == entryKey
                    val selecting = detailSelection.modeActive
                    val selKey = selectionKey("collection", entryKey)
                    val isSelected = detailSelection.isSelected(selKey)
                    Column(
                        modifier = Modifier.then(
                            if (dragging) Modifier else Modifier.animateItem(),
                        ),
                    ) {
                        val rowModifier = Modifier
                            .graphicsLayer {
                                translationY = if (dragging) dragOffsetY else 0f
                                scaleX = if (dragging) 1.03f else 1f
                                scaleY = if (dragging) 1.03f else 1f
                            }
                            .shadow(
                                if (dragging) 8.dp else 0.dp,
                                RoundedCornerShape(Corners.small),
                            )
                            .zIndex(if (dragging) 1f else 0f)
                            .pointerInput(entryKey, selecting, reorderEnabled) {
                                // Long-press = drag-reorder (playlist
                                // saja); nonaktif selama mode seleksi
                                // dan saat daftar sedang dicari/diurut
                                // (v1.0.6) — indeks tampil tidak memetakan
                                // ke indeks simpan dalam kondisi itu.
                                if (playlistView == null || selecting || !reorderEnabled) return@pointerInput
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingKey = entryKey
                                        dragDisplayIndex = displayIndex
                                        dragOffsetY = 0f
                                        prevItemOffset = detailListState.layoutInfo
                                            .visibleItemsInfo
                                            .firstOrNull { it.key == entryKey }
                                            ?.offset?.toFloat() ?: 0f
                                        haptic(HapticKind.LongPress)
                                    },
                                    onDragEnd = {
                                        draggingKey = null
                                        dragDisplayIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggingKey = null
                                        dragDisplayIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val info = detailListState.layoutInfo
                                        val item = info.visibleItemsInfo
                                            .firstOrNull { it.key == entryKey }
                                            ?: return@detectDragGesturesAfterLongPress
                                        dragOffsetY += prevItemOffset - item.offset
                                        prevItemOffset = item.offset.toFloat()
                                        val centerPx =
                                            (item.offset + item.size / 2 + dragOffsetY).toInt()
                                        val target = info.visibleItemsInfo
                                            .firstOrNull { candidate ->
                                                candidate.key != entryKey &&
                                                    centerPx in candidate.offset..<
                                                        (candidate.offset + candidate.size)
                                            }
                                            ?.index
                                        if (target != null && target != dragDisplayIndex) {
                                            haptic(HapticKind.Tap)
                                            applyPlaylistMove(dragDisplayIndex, target)
                                            dragDisplayIndex = target
                                        }
                                    },
                                )
                            }
                        TrackRow(
                            chartNumber = displayIndex + 1,
                            track = track,
                            durations = state.durations,
                            onClick = {
                                if (selecting) detailSelection.toggle(selKey)
                                else state.player.playTrack(track)
                            },
                            onLongClick = when {
                                selecting -> ({ detailSelection.toggle(selKey) })
                                playlistView == null -> ({ detailSelection.enter(selKey) })
                                else -> null
                            },
                            selected = isSelected,
                            trailing = if (isOffline && !selecting) {
                                {
                                    TextButton(onClick = {
                                        track.youtubeVideoId?.let {
                                            downloads.delete(it)
                                            // v1.0.6: lupakan juga catatan
                                            // tugasnya supaya tidak tampil
                                            // sebagai unduhan aktif.
                                            DownloadManager.forget(context, it)
                                            refresh()
                                            showToast(context, "Download deleted")
                                        }
                                    }, colors = sonaraTextButtonColors()) { Text("Delete", fontSize = 13.sp) }
                                }
                            } else if (view is CollectionView.Disliked && !selecting) {
                                // v1.0.3: Restore — batal tidak suka:
                                // lagu keluar dari daftar ini + tidak
                                // lagi dilewati saat antrean maju.
                                {
                                    TextButton(onClick = {
                                        state.store.setDislikedTrack(track, false)
                                        state.store.setDisliked(track.id, false)
                                        dislikedVersion++
                                        showToast(context, "Restored \"${track.title}\"")
                                    }, colors = sonaraTextButtonColors()) { Text("Restore", fontSize = 13.sp) }
                                }
                            } else {
                                null
                            },
                            modifier = rowModifier,
                        )
                    }
                }
            }
        }
        if (showRename && livePlaylist != null) {
            val pl = livePlaylist
            // Cover saat ini (untuk preview dialog rename).
            val renameCurrentCover = rememberPlaylistCover(context, pl.id, coverTick)
            AlertDialog(
                onDismissRequest = {
                    discardCoverFile(renameStagedCover)
                    renameStagedCover = null
                    renameRemoveCover = false
                    showRename = false
                },
                title = { Text("Rename playlist") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = {
                                renameText = it
                                renameProblem = null
                            },
                            label = { Text("Playlist name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = sonaraTextFieldColors(),
                        )
                        if (renameProblem != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when (renameProblem) {
                                    PlaylistNameProblem.BLANK -> "Name can't be empty."
                                    PlaylistNameProblem.DUPLICATE ->
                                        "A playlist with this name already exists."
                                    null -> ""
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        // v1.10.41: ganti/hapus cover (lokal saja).
                        Spacer(Modifier.height(16.dp))
                        CoverPickRow(
                            previewFile = if (renameRemoveCover) {
                                null
                            } else {
                                renameStagedCover ?: renameCurrentCover
                            },
                            onChooseImage = launchRenameCoverPicker,
                            onRemove = if (!renameRemoveCover &&
                                (renameStagedCover != null || renameCurrentCover != null)
                            ) {
                                {
                                    discardCoverFile(renameStagedCover)
                                    renameStagedCover = null
                                    renameRemoveCover = true
                                }
                            } else {
                                null
                            },
                            removeLabel = "Remove cover",
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val problem = validatePlaylistName(
                            renameText,
                            playlists.filter { it.id != pl.id }.map { it.name },
                        )
                        if (problem != null) {
                            renameProblem = problem
                            return@Button
                        }
                        val updated = pl.copy(name = renameText.trim())
                        val updatedList = playlists.map {
                            if (it.id == updated.id) updated else it
                        }
                        state.store.savePlaylists(updatedList)
                        playlists = updatedList
                        openView = CollectionView.LocalPlaylist(updated)
                        // v1.10.41: terapkan perubahan cover (id tidak
                        // berubah saat rename, jadi cover lama tetap
                        // kecuali diganti/dihapus eksplisit di sini).
                        val staged = renameStagedCover
                        val remove = renameRemoveCover
                        renameStagedCover = null
                        renameRemoveCover = false
                        if (staged != null || remove) {
                            scope.launch(Dispatchers.IO) {
                                if (remove) {
                                    deletePlaylistCover(context, pl.id)
                                } else if (staged != null) {
                                    commitCoverFile(context, pl.id, staged)
                                }
                                withContext(Dispatchers.Main) { coverTick++ }
                            }
                        }
                        showRename = false
                        showToast(context, "Playlist renamed")
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            discardCoverFile(renameStagedCover)
                            renameStagedCover = null
                            renameRemoveCover = false
                            showRename = false
                        },
                        colors = sonaraTextButtonColors(),
                    ) { Text("Cancel") }
                },
            )
        }
        if (showDeletePlaylist && livePlaylist != null) {
            val pl = livePlaylist
            AlertDialog(
                onDismissRequest = { showDeletePlaylist = false },
                title = { Text("Delete playlist?") },
                text = {
                    Text(
                        "\u201C${pl.name}\u201D will be deleted. " +
                            "Your songs, liked songs, and history will stay.",
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val updatedList = deletePlaylistFrom(playlists, pl.id)
                        state.store.savePlaylists(updatedList)
                        // v1.10.41: cover ikut terhapus bersama playlist.
                        deletePlaylistCover(context, pl.id)
                        // v1.0.6: state org (pin/folder/tag/auto-download)
                        // ikut dibersihkan supaya tidak menggantung.
                        state.store.prunePlaylistOrg(updatedList.map { it.id })
                        orgVersion++
                        playlists = updatedList
                        openView = null
                        showDeletePlaylist = false
                        showToast(context, "Playlist deleted")
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeletePlaylist = false },
                        colors = sonaraTextButtonColors(),
                    ) { Text("Cancel") }
                },
            )
        }
        // v1.0.6: dialog organize — pin, folder, tag, auto-download.
        // Semua state lokal saja (tidak disinkron); perubahan disimpan
        // langsung lewat saveOrg.
        if (showOrganize && livePlaylist != null) {
            val pl = livePlaylist
            val pinned = LibraryOrgPolicy.isPinned(org, pl.id)
            val autoDl = LibraryOrgPolicy.isAutoDownload(org, pl.id)
            val currentFolderId = LibraryOrgPolicy.folderOf(org, pl.id)
            val currentTags = org.tags[pl.id].orEmpty()
            val usedTags = LibraryOrgPolicy.allTagsUsed(org)
            AlertDialog(
                onDismissRequest = { showOrganize = false; newTagText = "" },
                title = { Text("Organize playlist") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Pinned to top",
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                            )
                            Switch(
                                checked = pinned,
                                onCheckedChange = {
                                    saveOrg(LibraryOrgPolicy.togglePin(org, pl.id))
                                },
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Auto-download new songs",
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                            )
                            Switch(
                                checked = autoDl,
                                onCheckedChange = {
                                    saveOrg(
                                        LibraryOrgPolicy.toggleAutoDownload(org, pl.id),
                                    )
                                },
                            )
                        }
                        Text(
                            "Songs added later to this playlist download automatically, when possible.",
                            fontSize = 13.sp,
                            color = InkMuted,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Folder", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        OrganizeFolderRow(
                            label = "No folder",
                            selected = currentFolderId == null,
                            onClick = {
                                saveOrg(
                                    LibraryOrgPolicy.movePlaylistToFolder(org, pl.id, null),
                                )
                            },
                        )
                        for (folder in org.folders) {
                            OrganizeFolderRow(
                                label = folder.name,
                                selected = currentFolderId == folder.id,
                                onClick = {
                                    saveOrg(
                                        LibraryOrgPolicy.movePlaylistToFolder(
                                            org, pl.id, folder.id,
                                        ),
                                    )
                                },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Tags", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = newTagText,
                                onValueChange = { newTagText = it },
                                label = { Text("New tag") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = {
                                val normalized = LibraryOrgPolicy.normalizeTag(newTagText)
                                if (normalized != null) {
                                    saveOrg(LibraryOrgPolicy.toggleTag(org, pl.id, normalized))
                                }
                                newTagText = ""
                            }) { Text("Add") }
                        }
                        if (currentTags.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                items(currentTags.toList(), key = { it }) { tag ->
                                    TagChip(
                                        label = "$tag ×",
                                        selected = true,
                                        onClick = {
                                            saveOrg(
                                                LibraryOrgPolicy.toggleTag(org, pl.id, tag),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        val quickTags = usedTags - currentTags.toSet()
                        if (quickTags.isNotEmpty()) {
                            Text(
                                "Tap to add:",
                                fontSize = 13.sp,
                                color = InkMuted,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                items(quickTags.toList(), key = { it }) { tag ->
                                    TagChip(
                                        label = tag,
                                        selected = false,
                                        onClick = {
                                            saveOrg(
                                                LibraryOrgPolicy.toggleTag(org, pl.id, tag),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showOrganize = false; newTagText = "" },
                        colors = sonaraTextButtonColors(),
                    ) { Text("Done") }
                },
            )
        }
        detailBulkSave?.let { bulkTracks ->
            SaveTracksToPlaylistSheet(
                tracks = bulkTracks,
                state = state,
                onDismiss = {
                    detailBulkSave = null
                    detailSelection.exitMode()
                },
            )
        }
        return
    }

    val allLikedCount = mergeTracks(likedTracks).size
    val offlineMb = (downloadBytes + 500_000L) / 1_000_000L
    // Kunci kemunculan daftar root — dihitung sekali per daftar
    // (bukan per baris) untuk seleksi multi-lagu.
    val mostPlayedKeys = remember(topTracks) { occurrenceKeys(topTracks.map { it.track.id }) }
    val recentKeys = remember(recentTracks) { occurrenceKeys(recentTracks.map { it.id }) }
    val localKeys = remember(localTracks) { occurrenceKeys(localTracks.map { it.id }) }

    // v1.10.21: daftar root berubah = seleksi basi.
    LaunchedEffect(
        topTracks.map { it.track.id },
        recentTracks.map { it.id },
        localTracks.map { it.id },
    ) {
        if (librarySelection.modeActive) librarySelection.exitMode()
    }
    // v1.10.18: observer scroll hanya untuk list root (tampilan
    // detail koleksi memakai LazyColumn sendiri — chrome tetap).
    val libraryListState = rememberLazyListState()
    if (onChromeScroll != null && openView == null) {
        ChromeScrollEffect(
            listState = libraryListState,
            enabled = true,
            onScroll = onChromeScroll,
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        state = libraryListState,
        // v1.10.7: ruang napas bawah pengaman (lihat HomeScreen).
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
    ) {
        stickyHeader {
            if (librarySelection.modeActive) {
                SelectionActionBar(
                    count = librarySelection.count,
                    jamActive = libraryJamState.active,
                    onAddToQueue = {
                        val selected = librarySelectedTracks(
                            librarySelection, topTracks, recentTracks, localTracks,
                        )
                        val added = state.player.addTracksToQueue(selected)
                        showToast(
                            context,
                            if (added == 1) "Added 1 song to queue"
                            else "Added $added songs to queue",
                        )
                        librarySelection.exitMode()
                    },
                    onSaveToPlaylist = {
                        libraryBulkSave = librarySelectedTracks(
                            librarySelection, topTracks, recentTracks, localTracks,
                        )
                    },
                    onExit = { librarySelection.exitMode() },
                )
            }
        }
        // v1.10.1: tombol Back di kiri atas Koleksi.
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ContentMargin, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArusBackButton(onClick = onBack)
            }
        }
        // v1.10.6: kartu akun & sinkron DIHAPUS dari Library (dan
        // semua page) — sinkron otomatis; Sign out / Delete cloud
        // data pindah ke Settings sebagai baris biasa.
        // ---- Grid kartu 2×2 ----
        item {
            Column(Modifier.padding(horizontal = ContentMargin)) {
                Row(Modifier.fillMaxWidth()) {
                    CollectionCard(
                        title = "Liked songs",
                        meta = "$allLikedCount songs",
                        background = GenrePop,
                        onClick = { openView = CollectionView.Liked },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                        // Lingkaran hias lebih terang di kanan atas.
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 30.dp, y = (-30).dp)
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF8C71)),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    CollectionCard(
                        title = "Offline downloads",
                        meta = "${downloadedTracks.size} songs · $offlineMb MB",
                        background = CardOffline,
                        onClick = { openView = CollectionView.Offline },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                        // Pita bawah lebih gelap.
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(Color.Black.copy(alpha = 0.18f)),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    // Slot playlist 1 & 2: playlist ASLI user (lokal).
                    // Slot kosong = kartu "Buat playlist". Urutan ikut
                    // pin (v1.0.6): yang di-pin tampil dulu.
                    val pool: List<Pair<String, String>> =
                        orderedPlaylists.map { it.name to "Playlist · ${it.trackIds.size} songs" }
                    // v1.10.41: cover kedua slot playlist (null = tanpa cover).
                    val slotCover0 = rememberPlaylistCover(context, orderedPlaylists.getOrNull(0)?.id, coverTick)
                    val slotCover1 = rememberPlaylistCover(context, orderedPlaylists.getOrNull(1)?.id, coverTick)
                    val slotCovers = listOf(slotCover0, slotCover1)
                    for (slot in 0..1) {
                        val entry = pool.getOrNull(slot)
                        val gold = slot % 2 == 0
                        if (entry != null) {
                            val slotCover = slotCovers[slot]
                            CollectionCard(
                                title = entry.first,
                                meta = entry.second,
                                background = if (gold) PosterGold else CardSlate,
                                onClick = {
                                    openView = CollectionView.LocalPlaylist(orderedPlaylists[slot])
                                },
                                modifier = Modifier.weight(1f),
                                coverFile = slotCover,
                                coverKey = coverCacheKey(slotCover),
                            ) {
                                if (gold) {
                                    SunDecoration()
                                } else {
                                    // Garis diagonal.
                                    Box(
                                        Modifier
                                            .align(Alignment.CenterEnd)
                                            .offset(x = 18.dp)
                                            .width(30.dp)
                                            .height(200.dp)
                                            .graphicsLayer { rotationZ = 20f }
                                            .background(Color.White.copy(alpha = 0.18f)),
                                    )
                                }
                            }
                        } else {
                            CollectionCard(
                                title = "Create playlist",
                                meta = "New playlist",
                                background = SegmentBg,
                                titleColor = InkBlack,
                                metaColor = InkMuted,
                                darkenBottom = false,
                                onClick = { showCreate = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = InkBlack,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        if (slot == 0) Spacer(Modifier.width(12.dp))
                    }
                }
            }
        }
        // v1.0.3: baris "Disliked songs" di bawah grid 2×2 —
        // lagu yang tidak disukai (lokal saja, terbaru dulu);
        // jumlah dibaca segar dari store tiap komposisi.
        item {
            val dislikedCount = state.store.dislikedTracks().size
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ContentMargin,
                        end = ContentMargin,
                        top = 12.dp,
                    )
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(Corners.card))
                    .background(PaperSurface)
                    .border(
                        CurrentTokens.cardBorderWidth,
                        PaperOutline,
                        RoundedCornerShape(Corners.card),
                    )
                    .clickable { openView = CollectionView.Disliked }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.ThumbDown,
                    contentDescription = null,
                    tint = InkBlack,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Disliked songs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBlack,
                        maxLines = 1,
                    )
                    Text(
                        if (dislikedCount == 1) "1 song" else "$dislikedCount songs",
                        fontSize = 13.sp,
                        color = InkMuted,
                        maxLines = 1,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open disliked songs",
                    tint = InkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        // v1.0.6: baris "Your stats" — halaman statistik dari data
        // lokal nyata (hitungan putar, riwayat, lagu disukai).
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ContentMargin,
                        end = ContentMargin,
                        top = 12.dp,
                    )
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(Corners.card))
                    .background(PaperSurface)
                    .border(
                        CurrentTokens.cardBorderWidth,
                        PaperOutline,
                        RoundedCornerShape(Corners.card),
                    )
                    .clickable { onOpenStats() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = InkBlack,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Your stats",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBlack,
                        maxLines = 1,
                    )
                    Text(
                        "Plays, top songs & your year",
                        fontSize = 13.sp,
                        color = InkMuted,
                        maxLines = 1,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open your stats",
                    tint = InkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        // v1.0.6: seksi "Your playlists" — semua playlist (grid di atas
        // hanya menampung dua slot): pin dulu, lalu folder, lalu sisanya.
        // Filter tag multi-pilih (AND); label grup hanya muncul bila ada
        // pin/folder supaya tampilan tetap bersih bila belum ada org.
        item {
            val usedTags = LibraryOrgPolicy.allTagsUsed(org)
            val visiblePlaylists = LibraryOrgPolicy.filterPlaylistsByTags(
                orderedPlaylists, org, selectedTags,
            )
            val groups = LibraryOrgPolicy.groupPlaylists(visiblePlaylists, org)
            val showGroupLabels = groups.pinned.isNotEmpty() || groups.folders.isNotEmpty()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ContentMargin,
                        end = ContentMargin,
                        top = 16.dp,
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayCased("Your playlists", CurrentTokens),
                        fontSize = 20.sp,
                        fontFamily = CurrentTokens.displayFont,
                        letterSpacing = CurrentTokens.displayLetterSpacing,
                        fontWeight = FontWeight.Black,
                        color = InkBlack,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { showNewFolder = true },
                        colors = sonaraTextButtonColors(),
                    ) { Text("New folder") }
                }
                if (usedTags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    ) {
                        items(usedTags, key = { it }) { tag ->
                            TagChip(
                                label = tag,
                                selected = tag in selectedTags,
                                onClick = {
                                    selectedTags = if (tag in selectedTags) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                },
                            )
                        }
                    }
                }
                if (groups.pinned.isNotEmpty()) {
                    if (showGroupLabels) {
                        OrgGroupLabel(text = "Pinned")
                    }
                    PlaylistOrgEntries(
                        playlists = groups.pinned,
                        org = org,
                        coverTick = coverTick,
                        layout = libraryLayout,
                        onOpen = { pl -> openView = CollectionView.LocalPlaylist(pl) },
                        onTogglePin = { pl ->
                            saveOrg(LibraryOrgPolicy.togglePin(org, pl.id))
                        },
                    )
                }
                for ((folder, members) in groups.folders) {
                    if (showGroupLabels) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                folder.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkBlack,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            Text(
                                "${members.size}",
                                fontSize = 13.sp,
                                color = InkMuted,
                            )
                            IconButton(onClick = {
                                folderRenameText = folder.name
                                folderRenameTarget = folder
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Rename folder",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(onClick = { folderDeleteTarget = folder }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete folder",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        PlaylistOrgEntries(
                            playlists = members,
                            org = org,
                            coverTick = coverTick,
                            layout = libraryLayout,
                            onOpen = { pl -> openView = CollectionView.LocalPlaylist(pl) },
                            onTogglePin = { pl ->
                                saveOrg(LibraryOrgPolicy.togglePin(org, pl.id))
                            },
                        )
                    }
                }
                if (groups.unfiled.isNotEmpty()) {
                    if (showGroupLabels && (groups.pinned.isNotEmpty() || groups.folders.isNotEmpty())) {
                        OrgGroupLabel(text = "Playlists")
                    }
                    PlaylistOrgEntries(
                        playlists = groups.unfiled,
                        org = org,
                        coverTick = coverTick,
                        layout = libraryLayout,
                        onOpen = { pl -> openView = CollectionView.LocalPlaylist(pl) },
                        onTogglePin = { pl ->
                            saveOrg(LibraryOrgPolicy.togglePin(org, pl.id))
                        },
                    )
                }
                if (visiblePlaylists.isEmpty() && orderedPlaylists.isNotEmpty()) {
                    Text(
                        "No playlists match the selected tags.",
                        fontSize = 14.sp,
                        color = InkMuted,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
        // ---- Most played (hitungan putar nyata, v1.10.16) ----
        // Di atas seksi riwayat, seperti diminta: lagu + artis yang
        // benar-benar paling sering diputar di perangkat ini.
        item {
            Text(
                displayCased("Most played", CurrentTokens),
                fontSize = 20.sp,
                fontFamily = CurrentTokens.displayFont,
                letterSpacing = CurrentTokens.displayLetterSpacing,
                fontWeight = FontWeight.Black,
                color = InkBlack,
                modifier = Modifier.padding(
                    start = ContentMargin,
                    end = ContentMargin,
                    top = Density.sectionTopPadding,
                    bottom = Density.sectionPaddingV,
                ),
            )
            if (topTracks.isEmpty()) {
                Text(
                    "No plays counted yet — play a song and it will show up here.",
                    fontSize = 14.sp,
                    color = InkMuted,
                    modifier = Modifier.padding(horizontal = ContentMargin, vertical = 8.dp),
                )
            }
        }
        // v1.10.23: kunci ber-prefix seksi — keempat blok berbagi SATU
        // LazyColumn; id#index tanpa prefix bertabrakan bila lagu yang
        // sama ada di Most played dan Recently played pada indeks lokal
        // yang sama (crash 10.22: "ytm:xTvyyoF_LZY#1").
        itemsIndexed(topTracks, key = { i, pc -> "mostplayed:" + pc.track.id + "#" + i }) { index, pc ->
            val topKey = selectionKey("mostplayed", mostPlayedKeys[index])
            TrackRow(
                chartNumber = index + 1,
                track = pc.track,
                durations = state.durations,
                onClick = {
                    if (librarySelection.modeActive) librarySelection.toggle(topKey)
                    else state.player.playTrack(pc.track)
                },
                onLongClick = {
                    if (librarySelection.modeActive) librarySelection.toggle(topKey)
                    else librarySelection.enter(topKey)
                },
                selected = librarySelection.isSelected(topKey),
            )
        }
        if (topArtists.isNotEmpty()) {
            item {
                Text(
                    "Top artists",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack,
                    modifier = Modifier.padding(
                        start = ContentMargin,
                        end = ContentMargin,
                        top = 16.dp,
                        bottom = 2.dp,
                    ),
                )
            }
            itemsIndexed(topArtists, key = { i, a -> "topartist:" + a.artist.lowercase() + "#" + i }) { _, a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSearch(a.artist) }
                        .padding(horizontal = ContentMargin, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        a.artist,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (a.plays == 1) "1 play" else "${a.plays} plays",
                        fontSize = 13.sp,
                        color = InkMuted,
                    )
                }
            }
        }
        // ---- Terakhir diputar (riwayat nyata) ----
        item {
            SectionHeader(
                title = "Recently played",
                rightText = "${recentTracks.size} SONGS",
            )
        }
        if (recentTracks.isEmpty()) {
            item {
                Text(
                    "No songs have been played on this device yet. " +
                        "Play a song from Home or Search — it will show up here.",
                    fontSize = 14.sp,
                    color = InkMuted,
                    modifier = Modifier.padding(horizontal = ContentMargin, vertical = 8.dp),
                )
            }
        }
        itemsIndexed(recentTracks, key = { i, t -> "recent:" + t.id + "#" + i }) { index, track ->
            val recentKey = selectionKey("recent", recentKeys[index])
            TrackRow(
                chartNumber = index + 1,
                track = track,
                durations = state.durations,
                onClick = {
                    if (librarySelection.modeActive) librarySelection.toggle(recentKey)
                    else state.player.playTrack(track)
                },
                onLongClick = {
                    if (librarySelection.modeActive) librarySelection.toggle(recentKey)
                    else librarySelection.enter(recentKey)
                },
                selected = librarySelection.isSelected(recentKey),
            )
        }
        // ---- Acara podcast yang diikuti (v1.0.5, lokal saja) ----
        // Ketuk baris = buka halaman acara; unfollow dari halaman itu.
        // Kosong = seksi disembunyikan, tanpa konten palsu.
        if (followedShows.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Podcasts you follow",
                    rightText = "${followedShows.size} SHOWS",
                )
            }
            itemsIndexed(
                followedShows,
                key = { i, s -> "followedshow:" + s.browseId + "#$i" },
            ) { index, show ->
                val showTrack = Track(
                    id = "show:${show.browseId}",
                    title = show.title,
                    artist = if (show.host.isNotBlank()) "Podcast • ${show.host}" else "Podcast",
                    album = "",
                    source = Source.YOUTUBE_MUSIC,
                    engine = Engine.YOUTUBE,
                    durationMs = 0,
                    youtubeVideoId = null,
                    imageUrl = show.imageUrl,
                    podcastShowBrowseId = show.browseId,
                    podcastShowTitle = show.title,
                )
                TrackRow(
                    chartNumber = index + 1,
                    track = showTrack,
                    durations = state.durations,
                    onClick = {
                        onOpenPodcastShow?.invoke(show.browseId, show.title)
                    },
                )
            }
        }
        // ---- Musik lokal: folder + duplikat + berkas hilang (v1.0.7) ----
        // Scan ulang tiap Koleksi dibuka (LaunchedEffect) + tombol
        // Rescan; tanpa background watcher.
        item {
            Column {
                Text(
                    "Local music",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = InkBlack,
                    modifier = Modifier.padding(
                        start = ContentMargin,
                        end = ContentMargin,
                        top = 24.dp,
                        bottom = 6.dp,
                    ),
                )
                if (!localScanned) {
                    Text(
                        "Scanning music on this device…",
                        fontSize = 13.sp,
                        color = InkMuted,
                        modifier = Modifier.padding(horizontal = ContentMargin),
                    )
                } else if (localGroups.isEmpty() && localMissingCount == 0) {
                    Text(
                        "No music files on this device, or media access permission hasn't been granted.",
                        fontSize = 13.sp,
                        color = InkMuted,
                        modifier = Modifier.padding(horizontal = ContentMargin),
                    )
                } else {
                    Text(
                        buildString {
                            append("${localGroups.size} song${if (localGroups.size == 1) "" else "s"}")
                            if (localMissingCount > 0) {
                                append(" · $localMissingCount file${if (localMissingCount == 1) "" else "s"} missing")
                            }
                        },
                        fontSize = 13.sp,
                        color = InkMuted,
                        modifier = Modifier.padding(horizontal = ContentMargin),
                    )
                }
                Row(
                    Modifier.padding(start = ContentMargin, end = ContentMargin, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { launchFolderPicker.launch(null) },
                        colors = sonaraTextButtonColors(),
                    ) { Text("Add folder") }
                    TextButton(
                        onClick = { scanLocal() },
                        colors = sonaraTextButtonColors(),
                    ) { Text("Rescan") }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Hide missing",
                        fontSize = 13.sp,
                        color = InkMuted,
                    )
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = hideMissingLocal,
                        onCheckedChange = {
                            hideMissingLocal = it
                            state.store.hideMissingLocal = it
                            scanLocal()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnAccent,
                            checkedTrackColor = BurntOrange,
                            checkedBorderColor = Color.Transparent,
                        ),
                    )
                }
                if (localFoldersFresh.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(
                            start = ContentMargin,
                            end = ContentMargin,
                            top = 2.dp,
                        ),
                    ) {
                        items(localFoldersFresh, key = { it.uriString }) { folder ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(Corners.chip))
                                    .background(SegmentBg)
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        folder.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkBlack,
                                        maxLines = 1,
                                    )
                                    IconButton(
                                        onClick = {
                                            state.store.removeLocalFolder(folder.uriString)
                                            localFoldersVersion++
                                            scanLocal()
                                        },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove folder ${folder.displayName}",
                                            tint = InkMuted,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        itemsIndexed(localGroups, key = { i, g -> "local:" + g.effective.id + "#" + i }) { index, group ->
            val track = group.effective
            val localKey = selectionKey("local", localKeys[index])
            Column {
                TrackRow(
                    chartNumber = index + 1,
                    track = track,
                    durations = state.durations,
                    modifier = if (group.missing) Modifier.alpha(0.45f) else Modifier,
                    onClick = {
                        if (group.missing) {
                            missingInfoTrack = track
                        } else if (librarySelection.modeActive) {
                            librarySelection.toggle(localKey)
                        } else {
                            state.player.playTrack(track)
                        }
                    },
                    onLongClick = {
                        if (librarySelection.modeActive) librarySelection.toggle(localKey)
                        else librarySelection.enter(localKey)
                    },
                    selected = librarySelection.isSelected(localKey),
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (group.copies.size > 1) {
                                TextButton(
                                    onClick = { copiesGroup = group },
                                    colors = sonaraTextButtonColors(),
                                ) {
                                    Text(
                                        "${group.copies.size} copies",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            IconButton(onClick = {
                                val ov = state.store.localTagOverrides()[track.id]
                                tagEditTitle = ov?.title?.ifBlank { track.title } ?: track.title
                                tagEditArtist = ov?.artist?.ifBlank { track.artist } ?: track.artist
                                tagEditAlbum = ov?.album?.ifBlank { track.album } ?: track.album
                                tagEditTrack = track
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit tags for ${track.title}",
                                    tint = InkMuted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                )
                if (group.missing) {
                    Text(
                        "File missing — tap the song for details.",
                        fontSize = 12.sp,
                        color = InkMuted,
                        modifier = Modifier.padding(
                            start = ContentMargin,
                            end = ContentMargin,
                            bottom = 4.dp,
                        ),
                    )
                }
            }
        }
        item {
            // Akses Pengaturan tetap ada dari Koleksi.
            SecondaryButton(
                text = "Settings",
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ContentMargin, vertical = 12.dp),
            )
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    libraryBulkSave?.let { bulkTracks ->
        SaveTracksToPlaylistSheet(
            tracks = bulkTracks,
            state = state,
            onDismiss = {
                libraryBulkSave = null
                librarySelection.exitMode()
            },
        )
    }

    // ---- Dialog file lokal (v1.0.7): edit tag / pilih salinan / berkas hilang ----
    tagEditTrack?.let { editTrack ->
        AlertDialog(
            onDismissRequest = { tagEditTrack = null },
            title = { Text("Edit song details") },
            text = {
                Column {
                    Text(
                        "Saved on this device only. The audio file is never changed.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tagEditTitle,
                        onValueChange = { tagEditTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = sonaraTextFieldColors(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tagEditArtist,
                        onValueChange = { tagEditArtist = it },
                        label = { Text("Artist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = sonaraTextFieldColors(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tagEditAlbum,
                        onValueChange = { tagEditAlbum = it },
                        label = { Text("Album") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = sonaraTextFieldColors(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        state.store.saveLocalTagOverride(
                            editTrack.id,
                            LocalTagOverride(
                                title = tagEditTitle,
                                artist = tagEditArtist,
                                album = tagEditAlbum,
                            ),
                        )
                        tagEditTrack = null
                        scanLocal()
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            state.store.clearLocalTagOverride(editTrack.id)
                            tagEditTrack = null
                            scanLocal()
                        },
                        colors = sonaraTextButtonColors(),
                    ) { Text("Clear saved tags") }
                    TextButton(
                        onClick = { tagEditTrack = null },
                        colors = sonaraTextButtonColors(),
                    ) { Text("Cancel") }
                }
            },
        )
    }
    copiesGroup?.let { group ->
        val preferredId = state.store.localPreferredSources()[group.key]
        AlertDialog(
            onDismissRequest = { copiesGroup = null },
            title = { Text(group.effective.title) },
            text = {
                Column {
                    Text(
                        "The same song was found in more than one place. " +
                            "Pick which copy plays when you tap this song.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    for (copy in group.copies) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    state.store.setLocalPreferredSource(group.key, copy.id)
                                    copiesGroup = null
                                    scanLocal()
                                },
                        ) {
                            RadioButton(
                                selected = copy.id == (preferredId ?: group.effective.id),
                                onClick = {
                                    state.store.setLocalPreferredSource(group.key, copy.id)
                                    copiesGroup = null
                                    scanLocal()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = BurntOrange),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    LocalFilesPolicy.copyLabel(copy),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkBlack,
                                )
                                Text(
                                    formatDurationMs(copy.durationMs),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { copiesGroup = null },
                    colors = sonaraTextButtonColors(),
                ) { Text("Cancel") }
            },
            confirmButton = {},
        )
    }
    missingInfoTrack?.let { missingTrack ->
        AlertDialog(
            onDismissRequest = { missingInfoTrack = null },
            title = { Text("File missing") },
            text = {
                Text(
                    "\"${missingTrack.title}\" was in the last scan, but its file " +
                        "is no longer on this device — it may have been moved, " +
                        "renamed, or deleted outside SONARA. The song cannot be " +
                        "played until the file comes back. Hide missing files " +
                        "to remove rows like this from the list.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        hideMissingLocal = true
                        state.store.hideMissingLocal = true
                        missingInfoTrack = null
                        scanLocal()
                    },
                ) { Text("Hide missing files") }
            },
            dismissButton = {
                TextButton(
                    onClick = { missingInfoTrack = null },
                    colors = sonaraTextButtonColors(),
                ) { Text("Keep showing") }
            },
        )
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = {
                discardCoverFile(createStagedCover)
                createStagedCover = null
                showCreate = false
            },
            title = { Text("New playlist") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = sonaraTextFieldColors(),
                    )
                    // v1.10.41: cover dari galeri (opsional, lokal saja).
                    Spacer(Modifier.height(16.dp))
                    CoverPickRow(
                        previewFile = createStagedCover,
                        onChooseImage = launchCreateCoverPicker,
                        onRemove = if (createStagedCover != null) {
                            {
                                discardCoverFile(createStagedCover)
                                createStagedCover = null
                            }
                        } else {
                            null
                        },
                        removeLabel = "Remove",
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            val id = UUID.randomUUID().toString()
                            val updated = playlists + Playlist(
                                id = id,
                                name = newPlaylistName.trim(),
                            )
                            state.store.savePlaylists(updated)
                            val staged = createStagedCover
                            createStagedCover = null
                            newPlaylistName = ""
                            showCreate = false
                            refresh()
                            if (staged != null) {
                                scope.launch(Dispatchers.IO) {
                                    commitCoverFile(context, id, staged)
                                    withContext(Dispatchers.Main) { coverTick++ }
                                }
                            }
                        }
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        discardCoverFile(createStagedCover)
                        createStagedCover = null
                        showCreate = false
                    },
                    colors = sonaraTextButtonColors(),
                ) { Text("Cancel") }
            },
        )
    }

    // v1.0.6: dialog folder — buat / ganti nama / hapus. Hapus folder
    // TIDAK menghapus playlist di dalamnya (playlist kembali ke daftar
    // tanpa folder).
    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false; newFolderName = "" },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sonaraTextFieldColors(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    val name = newFolderName.trim()
                    if (name.isNotEmpty()) {
                        LibraryOrgPolicy.createFolder(org, name)?.let { saveOrg(it) }
                        newFolderName = ""
                        showNewFolder = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewFolder = false; newFolderName = "" },
                    colors = sonaraTextButtonColors(),
                ) { Text("Cancel") }
            },
        )
    }
    folderRenameTarget?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderRenameTarget = null },
            title = { Text("Rename folder") },
            text = {
                OutlinedTextField(
                    value = folderRenameText,
                    onValueChange = { folderRenameText = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sonaraTextFieldColors(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    val name = folderRenameText.trim()
                    if (name.isNotEmpty()) {
                        LibraryOrgPolicy.renameFolder(org, folder.id, name)?.let { saveOrg(it) }
                    }
                    folderRenameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = { folderRenameTarget = null },
                    colors = sonaraTextButtonColors(),
                ) { Text("Cancel") }
            },
        )
    }
    folderDeleteTarget?.let { folder ->
        val memberCount = LibraryOrgPolicy.folderCounts(playlists, org)[folder.id] ?: 0
        AlertDialog(
            onDismissRequest = { folderDeleteTarget = null },
            title = { Text("Delete folder?") },
            text = {
                Text(
                    "\u201C${folder.name}\u201D will be deleted. " +
                        if (memberCount == 0) {
                            "No playlists are inside it."
                        } else if (memberCount == 1) {
                            "1 playlist inside it will stay, without a folder."
                        } else {
                            "$memberCount playlists inside it will stay, without a folder."
                        },
                )
            },
            confirmButton = {
                Button(onClick = {
                    saveOrg(LibraryOrgPolicy.deleteFolder(org, folder.id))
                    folderDeleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = { folderDeleteTarget = null },
                    colors = sonaraTextButtonColors(),
                ) { Text("Cancel") }
            },
        )
    }
}

/** Lagu terpilih di tiga daftar root Koleksi, urutan daftar dijaga. */
private fun librarySelectedTracks(
    selection: SelectionState,
    topTracks: List<PlayCount>,
    recentTracks: List<Track>,
    localTracks: List<Track>,
): List<Track> {
    val out = ArrayList<Track>()
    val mpKeys = occurrenceKeys(topTracks.map { it.track.id })
    topTracks.forEachIndexed { i, pc ->
        if (selection.isSelected(selectionKey("mostplayed", mpKeys[i]))) out.add(pc.track)
    }
    val rKeys = occurrenceKeys(recentTracks.map { it.id })
    recentTracks.forEachIndexed { i, t ->
        if (selection.isSelected(selectionKey("recent", rKeys[i]))) out.add(t)
    }
    val lKeys = occurrenceKeys(localTracks.map { it.id })
    localTracks.forEachIndexed { i, t ->
        if (selection.isSelected(selectionKey("local", lKeys[i]))) out.add(t)
    }
    return out
}

/** Satu kartu grid Koleksi — 1:1 (v1.10.40), radius 24dp, judul + meta putih. */
@Composable
private fun CollectionCard(
    title: String,
    meta: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.White,
    metaColor: Color = Color.White.copy(alpha = 0.8f),
    /**
     * Scrim bawah (v1.10.34): true untuk kartu bertitel putih di
     * atas warna tetap; false untuk kartu bertitel token-tema
     * ("Create playlist" — teks gelap di atas SegmentBg, scrim
     * justru merusaknya).
     */
    darkenBottom: Boolean = true,
    /**
     * Cover foto buatan user (v1.10.41, hanya playlist buatan user):
     * menggantikan warna latar + dekorasi; scrim + judul/meta putih
     * tetap di atasnya. [coverKey] = kunci cache Coil berversi
     * (path + lastModified) supaya cover yang diganti tidak
     * menampilkan gambar basi dari cache.
     */
    coverFile: File? = null,
    coverKey: String? = null,
    decoration: @Composable BoxScope.() -> Unit = {},
) {
    // SENYAP: judul/meta warna teks tema (kartu transparan — warna
    // putih bawaan tidak terbaca di atas latar terang). Pengecualian
    // v1.10.41: kartu ber-cover foto selalu tampil sebagai kartu foto
    // (scrim + teks putih) di semua tema, termasuk SENYAP — konten
    // user menang atas treatment datar.
    val hasCover = coverFile != null
    val flatCard = CurrentTokens.flatNoCards && !hasCover
    Box(
        modifier
            // v1.10.40: 1:1 — lebar kolom grid menentukan tinggi
            // (dulu height 136dp tetap: kartu lebih lebar dari
            // tinggi). Scrim + dekorasi kini FULL-BLEED: padding
            // pindah ke kolom teks, supaya scrim gradien tidak lagi
            // terkurung di dalam padding dan tampak seperti pita
            // persegi yang melayang (screenshot HP user, BLOK Light).
            .aspectRatio(1f)
            .hardShadowDraw(Corners.card, CurrentTokens.hardShadowTile)
            .clip(RoundedCornerShape(Corners.card))
            // SENYAP: tanpa isi kartu — transparan + hairline, judul/meta
            // warna teks tema; darkenBottom dimatikan (tidak ada scrim).
            .background(if (flatCard) Color.Transparent else background)
            .then(
                if (flatCard) {
                    Modifier.border(1.dp, CurrentTokens.outline, RoundedCornerShape(Corners.card))
                } else {
                    Modifier
                },
            )
            .clickable { onClick() },
    ) {
        if (hasCover && coverFile != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverFile)
                    .memoryCacheKey(coverKey)
                    .diskCacheKey(coverKey)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        // v1.10.34: scrim bawah di belakang judul/meta putih — tanpa
        // ini judul di atas kartu emas 1.83:1 dan di atas Pop 3.30:1;
        // dengan scrim 0.55 semua latar kartu >= 7.34:1. Hue kartu
        // tidak berubah; dekorasi tetap di atas scrim. Dilewati bila
        // darkenBottom=false (kartu bertitel token-tema) atau SENYAP
        // (judul sudah warna teks tema, tanpa latar). Di atas foto
        // cover scrim SELALU dipakai (judul putih harus terbaca).
        // v1.10.40: fillMaxSize di sini = batas kartu sebenarnya
        // (padding sudah pindah ke kolom teks di bawah).
        if (darkenBottom && (!CurrentTokens.flatNoCards || hasCover)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                            ),
                        ),
                    ),
            )
        }
        // Dekorasi (matahari/garis) hanya untuk kartu tanpa foto —
        // di atas foto user dekorasi menumpuk dan mengotori gambar.
        if (!hasCover) {
            decoration()
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(Density.cardPadding),
        ) {
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                color = if (hasCover) Color.White
                    else if (flatCard) CurrentTokens.text else titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                meta,
                fontSize = 14.sp,
                color = if (hasCover) Color.White.copy(alpha = 0.8f)
                    else if (flatCard) CurrentTokens.textMuted else metaColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Hiasan matahari kecil (kartu playlist gold) — digambar, bukan emoji. */
@Composable
private fun BoxScope.SunDecoration() {
    Canvas(
        Modifier
            .align(Alignment.TopStart)
            .padding(0.dp)
            .size(28.dp),
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 4f
        for (i in 0 until 8) {
            val a = (i * 45f) * Math.PI.toFloat() / 180f
            drawLine(
                color = Color.White,
                start = Offset(c.x + kotlin.math.cos(a) * (r + 3.dp.toPx()), c.y + kotlin.math.sin(a) * (r + 3.dp.toPx())),
                end = Offset(c.x + kotlin.math.cos(a) * (r + 8.dp.toPx()), c.y + kotlin.math.sin(a) * (r + 8.dp.toPx())),
                strokeWidth = 2.dp.toPx(),
            )
        }
        drawCircle(color = Color.White, radius = r, center = c)
    }
    // Lingkaran hias lebih terang di kanan atas (v1.10.40: dulu di
    // kiri bawah, menumpuk di belakang judul putih — kontras judul
    // jatuh di atas lingkarannya; kini sejajar kartu Liked, zona
    // judul/meta bersih).
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .offset(x = 30.dp, y = (-30).dp)
            .size(96.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
    )
}

/** Label grup kecil di atas daftar playlist terorganisir (v1.0.6). */
@Composable
private fun OrgGroupLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = InkMuted,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/**
 * Baris playlist di seksi "Your playlists" (v1.0.6): thumb cover 48dp
 * (atau ikon folder bila tanpa cover), nama + jumlah lagu + tag,
 * tombol pin. Tap baris membuka detail playlist.
 */
@Composable
private fun PlaylistOrgRow(
    playlist: Playlist,
    org: LibraryOrg,
    coverTick: Int,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    /** v1.0.7: varian rapat — cover lebih kecil, padding lebih ketat. */
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val cover = rememberPlaylistCover(context, playlist.id, coverTick)
    val tags = org.tags[playlist.id].orEmpty()
    val pinned = LibraryOrgPolicy.isPinned(org, playlist.id)
    val coverSize = if (compact) 36.dp else 48.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.card))
            .clickable { onOpen() }
            .padding(vertical = if (compact) 3.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(RoundedCornerShape(Corners.rowCover))
                .background(SegmentBg),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(Corners.rowCover)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                )
            }
        }
        Spacer(Modifier.width(if (compact) 10.dp else 12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.name,
                fontSize = if (compact) 15.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkBlack,
                maxLines = 1,
            )
            Text(
                buildString {
                    append(if (playlist.trackIds.size == 1) "1 song" else "${playlist.trackIds.size} songs")
                    if (tags.isNotEmpty()) {
                        append(" · ")
                        append(tags.joinToString(" "))
                    }
                },
                fontSize = 13.sp,
                color = InkMuted,
                maxLines = 1,
            )
        }
        IconButton(onClick = onTogglePin) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = if (pinned) "Unpin playlist" else "Pin playlist",
                tint = if (pinned) BurntOrange else InkMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Entri-entri satu grup seksi "Your playlists" (v1.0.7): EDITORIAL =
 * baris PlaylistOrgRow; COMPACT = baris rapat; GRID = ubin 2 kolom.
 * Pin/folder/tag tetap bekerja di semua layout.
 */
@Composable
private fun PlaylistOrgEntries(
    playlists: List<Playlist>,
    org: LibraryOrg,
    coverTick: Int,
    layout: HomeLayout,
    onOpen: (Playlist) -> Unit,
    onTogglePin: (Playlist) -> Unit,
) {
    if (LayoutPolicy.railMode(layout) == LayoutPolicy.RailMode.GRID) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (chunk in playlists.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (pl in chunk) {
                        Box(Modifier.weight(1f)) {
                            PlaylistGridTile(
                                playlist = pl,
                                org = org,
                                coverTick = coverTick,
                                onOpen = { onOpen(pl) },
                                onTogglePin = { onTogglePin(pl) },
                            )
                        }
                    }
                    if (chunk.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    } else {
        for (pl in playlists) {
            PlaylistOrgRow(
                playlist = pl,
                org = org,
                coverTick = coverTick,
                onOpen = { onOpen(pl) },
                onTogglePin = { onTogglePin(pl) },
                compact = layout == HomeLayout.COMPACT,
            )
        }
    }
}

/** Satu ubin grid Koleksi (v1.0.7): cover + nama + jumlah lagu + pin. */
@Composable
private fun PlaylistGridTile(
    playlist: Playlist,
    org: LibraryOrg,
    coverTick: Int,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val context = LocalContext.current
    val cover = rememberPlaylistCover(context, playlist.id, coverTick)
    val pinned = LibraryOrgPolicy.isPinned(org, playlist.id)
    Column(
        Modifier
            .clip(RoundedCornerShape(Corners.card))
            .clickable { onOpen() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Corners.card))
                .background(SegmentBg),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (playlist.trackIds.size == 1) "1 song" else "${playlist.trackIds.size} songs",
                    fontSize = 12.sp,
                    color = InkMuted,
                    maxLines = 1,
                )
            }
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = if (pinned) "Unpin playlist" else "Pin playlist",
                    tint = if (pinned) BurntOrange else InkMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Chip tag — terpilih = ChromeBg, tidak = PaperSurface (v1.0.6). */
@Composable
private fun TagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(Corners.rowCover))
            .background(if (selected) ChromeBg else PaperSurface)
            .border(
                1.dp,
                if (selected) ChromeBg else PaperOutline,
                RoundedCornerShape(Corners.rowCover),
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (selected) OnChrome else InkBlack,
            maxLines = 1,
        )
    }
}

/** Baris pilihan folder di dialog organize (v1.0.6). */
@Composable
private fun OrganizeFolderRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.rowCover))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = if (selected) BurntOrange else InkMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontSize = 15.sp,
            color = InkBlack,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = BurntOrange,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Seksi unduhan aktif di tampilan Offline (v1.0.6): progres per
 * unduhan + jeda/lanjut/coba lagi/hapus. Judul + artis dibaca dari
 * catatan tugas (metadata saat mulai); bila metadata kosong,
 * tampilkan id video.
 */
@Composable
private fun ActiveDownloadsSection(
    tasks: List<DownloadTasks.DownloadTask>,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentMargin, vertical = 8.dp),
    ) {
        Text(
            "Active downloads",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
        )
        Spacer(Modifier.height(4.dp))
        for (task in tasks) {
            val progress = DownloadTasks.progress(task.downloadedBytes, task.totalBytes)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            task.title.ifBlank { task.videoId },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBlack,
                            maxLines = 1,
                        )
                        Text(
                            when (task.state) {
                                DownloadTaskState.DOWNLOADING ->
                                    if (task.totalBytes > 0) {
                                        "Downloading ${DownloadTasks.formatBytes(task.downloadedBytes)} of ${DownloadTasks.formatBytes(task.totalBytes)}"
                                    } else {
                                        "Downloading…"
                                    }
                                DownloadTaskState.QUEUED -> "Waiting to start"
                                DownloadTaskState.PAUSED -> "Paused at ${DownloadTasks.formatBytes(task.downloadedBytes)}"
                                DownloadTaskState.FAILED ->
                                    "Failed${task.error?.let { ": $it" } ?: ""}"
                                DownloadTaskState.COMPLETED -> "Downloaded"
                            },
                            fontSize = 13.sp,
                            color = InkMuted,
                            maxLines = 2,
                        )
                    }
                    when (task.state) {
                        DownloadTaskState.DOWNLOADING, DownloadTaskState.QUEUED -> {
                            IconButton(onClick = {
                                DownloadManager.pause(context, task.videoId)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Pause,
                                    contentDescription = "Pause download",
                                )
                            }
                        }
                        DownloadTaskState.PAUSED -> {
                            IconButton(onClick = {
                                DownloadManager.resume(context, task.videoId)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Resume download",
                                )
                            }
                        }
                        DownloadTaskState.FAILED -> {
                            IconButton(onClick = {
                                DownloadManager.retry(context, task.videoId)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Retry download",
                                )
                            }
                        }
                        DownloadTaskState.COMPLETED -> Unit
                    }
                    IconButton(onClick = {
                        DownloadManager.delete(context, task.videoId)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete download",
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (task.state == DownloadTaskState.DOWNLOADING && task.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (task.state == DownloadTaskState.DOWNLOADING) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (task.state == DownloadTaskState.PAUSED && task.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
