package com.sonara.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonara.app.data.ArtistApi
import com.sonara.app.data.followKey
import com.sonara.app.data.occurrenceKeys
import com.sonara.app.data.selectionKey
import com.sonara.app.data.Track
import com.sonara.app.ui.ArusBackButton
import com.sonara.app.ui.theme.AvatarGold
import com.sonara.app.ui.ContentMargin
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.displayCased
import com.sonara.app.ui.theme.Density
import com.sonara.app.ui.CoverArt
import com.sonara.app.ui.EmptyState
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.OnAvatar
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.SecondaryButton
import com.sonara.app.ui.SectionHeader
import com.sonara.app.ui.SelectionActionBar
import com.sonara.app.ui.rememberSelectionState
import com.sonara.app.ui.showToast
import com.sonara.app.ui.theme.Spacing
import com.sonara.app.ui.TrackRow
import com.sonara.app.data.mergeTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Target halaman profil artis: nama + browseId bila diketahui. */
data class ArtistTarget(val name: String, val browseId: String?)

/**
 * Halaman profil artis (v1.10.18) — masuk back-stack yang sudah ada.
 *
 * Semua konten dari API InnerTube (endpoint /browse, jalur yang sama
 * dengan lirik): header nama + artwork HANYA bila API mengembalikan,
 * Top songs (bisa diputar), Albums & singles (label Album/Single/EP
 * HANYA bila subtitle API benar-benar menyebutkannya), About
 * (teks verbatim bila ada), Similar artists (ketuk → profilnya).
 * TIDAK ADA angka apa pun (subscriber/play count tidak ditampilkan).
 * Bila browseId tidak bisa di-resolve, halaman menjadi halaman hasil
 * pencarian dengan label jujur — BUKAN profil palsu.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ArtistProfileScreen(
    state: SonaraState,
    target: ArtistTarget?,
    onBack: () -> Unit,
    onOpenDetail: (Track) -> Unit,
    onOpenSearch: (String?) -> Unit,
    onOpenArtist: (String, String?) -> Unit,
) {
    // v1.10.21: Back keluar mode seleksi dulu, baru halaman.
    val artistContext = LocalContext.current
    val artistSelection = rememberSelectionState()
    var artistBulkSave by remember { mutableStateOf<List<Track>?>(null) }
    val artistJamState by state.jam.uiState.collectAsState()
    BackHandler {
        if (artistSelection.modeActive) artistSelection.exitMode() else onBack()
    }
    val name = target?.name?.trim().orEmpty()
    var profile by remember(target) { mutableStateOf<ArtistApi.ArtistProfile?>(null) }
    var failed by remember(target) { mutableStateOf(false) }
    var reloadTick by remember(target) { mutableStateOf(0) }
    val currentTrack by state.player.currentTrack.collectAsState()
    val isPlaying by state.player.isPlaying.collectAsState()
    var sheetTrack by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(target, reloadTick) {
        profile = null
        failed = false
        if (name.isEmpty()) {
            failed = true
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                ArtistApi.fetchArtistProfile(
                    state.ytmApi, name, target?.browseId,
                )
            }.getOrNull()
        }
        if (loaded != null) profile = loaded else failed = true
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentMargin, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArusBackButton(onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Text(
                displayCased("Artist", CurrentTokens),
                fontSize = 20.sp,
                fontFamily = CurrentTokens.displayFont,
                letterSpacing = CurrentTokens.displayLetterSpacing,
                fontWeight = FontWeight.Black,
                color = InkBlack,
            )
        }

        when {
            failed -> {
                EmptyState(
                    title = "Couldn't load this artist",
                    subtitle = "Check your internet connection, then try again.",
                    actionText = "Try again",
                    onAction = { reloadTick++ },
                )
            }
            profile == null -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = InkBlack)
                }
            }
            else -> {
                val p = profile!!
                // v1.10.21: daftar + kunci seleksi dihitung sekali
                // di sini (bukan di dalam item) supaya bar aksi
                // stickyHeader bisa mengumpulkan yang terpilih.
                val songsMerged = mergeTracks(p.searchTracks)
                val topMerged = mergeTracks(
                    p.topSongs.map { r ->
                        Track(
                            id = "ytm:${r.videoId}",
                            title = r.title,
                            artist = r.artist,
                            album = "",
                            source = com.sonara.app.data.Source.YOUTUBE_MUSIC,
                            engine = com.sonara.app.data.Engine.YOUTUBE,
                            durationMs = r.durationMs,
                            youtubeVideoId = r.videoId,
                            imageUrl = r.imageUrl,
                            explicit = r.explicit,
                        )
                    },
                )
                val songsKeys = occurrenceKeys(songsMerged.map { it.first.id })
                val topKeys = occurrenceKeys(topMerged.map { it.first.id })
                LaunchedEffect(p) {
                    if (artistSelection.modeActive) artistSelection.exitMode()
                }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
                ) {
                    stickyHeader {
                        if (artistSelection.modeActive) {
                            SelectionActionBar(
                                count = artistSelection.count,
                                jamActive = artistJamState.active,
                                onAddToQueue = {
                                    val selected = artistSelectedTracks(
                                        artistSelection, songsMerged, topMerged,
                                        songsKeys, topKeys,
                                    )
                                    val added = state.player.addTracksToQueue(selected)
                                    showToast(
                                        artistContext,
                                        if (added == 1) "Added 1 song to queue"
                                        else "Added $added songs to queue",
                                    )
                                    artistSelection.exitMode()
                                },
                                onSaveToPlaylist = {
                                    artistBulkSave = artistSelectedTracks(
                                        artistSelection, songsMerged, topMerged,
                                        songsKeys, topKeys,
                                    )
                                },
                                onExit = { artistSelection.exitMode() },
                            )
                        }
                    }
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ContentMargin, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ArtistAvatar(imageUrl = p.imageUrl, name = p.name)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                p.name,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = InkBlack,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            // v1.10.21: Follow/Unfollow — lokal saja,
                            // tidak disinkron (cakupan sync tak berubah).
                            val followKeyVal = followKey(
                                p.name,
                                p.browseId.ifBlank { null },
                            )
                            val isFollowing = state.followedVersion.let {
                                state.store.isFollowing(followKeyVal)
                            }
                            Spacer(Modifier.height(10.dp))
                            SecondaryButton(
                                text = if (isFollowing) "Following" else "Follow",
                                onClick = {
                                    state.setArtistFollowed(
                                        p.name,
                                        p.browseId.ifBlank { null },
                                        !isFollowing,
                                    )
                                    showToast(
                                        artistContext,
                                        if (isFollowing) "Unfollowed ${p.name}"
                                        else "Followed ${p.name}",
                                    )
                                },
                            )
                            if (p.isSearchFallback) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Search results for \u201C${p.name}\u201D",
                                    fontSize = 14.sp,
                                    color = InkMuted,
                                )
                            }
                        }
                    }
                    if (p.isSearchFallback) {
                        // Fallback jujur: daftar lagu hasil pencarian.
                        val merged = songsMerged
                        if (merged.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No results",
                                    subtitle = "Try a different artist name.",
                                )
                            }
                        } else {
                            item {
                                SectionHeader(
                                    title = "Songs",
                                    rightText = "${merged.size} RESULTS",
                                )
                            }
                            itemsIndexed(merged, key = { i, (t, _) -> t.id + "#" + i }) { index, (track, sources) ->
                                val songKey = selectionKey("songs", songsKeys[index])
                                TrackRow(
                                    chartNumber = index + 1,
                                    track = track,
                                    durations = state.durations,
                                    sources = sources,
                                    onClick = {
                                        if (artistSelection.modeActive) artistSelection.toggle(songKey)
                                        else {
                                            state.player.playTrack(track)
                                            onOpenDetail(track)
                                        }
                                    },
                                    onLongClick = {
                                        if (artistSelection.modeActive) artistSelection.toggle(songKey)
                                        else artistSelection.enter(songKey)
                                    },
                                    isCurrent = currentTrack?.id == track.id,
                                    isPlaying = isPlaying,
                                    selected = artistSelection.isSelected(songKey),
                                    animateIn = index < 8,
                                    index = index,
                                    trailing = {
                                        IconButton(onClick = { sheetTrack = track }) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = "Song menu",
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    } else {
                        // ---- Top songs ----
                        if (p.topSongs.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Top songs",
                                    rightText = "TAP TO PLAY",
                                )
                            }
                            val merged = topMerged
                            itemsIndexed(merged, key = { i, (t, _) -> t.id + "#" + i }) { index, (track, sources) ->
                                val topKey = selectionKey("topsongs", topKeys[index])
                                TrackRow(
                                    chartNumber = index + 1,
                                    track = track,
                                    durations = state.durations,
                                    sources = sources,
                                    onClick = {
                                        if (artistSelection.modeActive) artistSelection.toggle(topKey)
                                        else {
                                            state.player.playTrack(track)
                                            onOpenDetail(track)
                                        }
                                    },
                                    onLongClick = {
                                        if (artistSelection.modeActive) artistSelection.toggle(topKey)
                                        else artistSelection.enter(topKey)
                                    },
                                    isCurrent = currentTrack?.id == track.id,
                                    isPlaying = isPlaying,
                                    selected = artistSelection.isSelected(topKey),
                                    animateIn = index < 8,
                                    index = index,
                                    trailing = {
                                        IconButton(onClick = { sheetTrack = track }) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = "Song menu",
                                            )
                                        }
                                    },
                                )
                            }
                        }
                        // ---- Albums & singles ----
                        if (p.albums.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Albums & singles",
                                    rightText = "${p.albums.size} RELEASES",
                                )
                            }
                            item {
                                LazyRow(
                                    Modifier.fillMaxWidth(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = ContentMargin,
                                    ),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Density.gridGap),
                                ) {
                                    itemsIndexed(p.albums, key = { i, a -> a.title + "|" + a.subtitle + "#" + i }) { _, album ->
                                        Column(
                                            Modifier
                                                .width(140.dp)
                                                .clickable {
                                                    // Album belum punya halaman
                                                    // sendiri — ketuk mencari
                                                    // judul album itu (nyata).
                                                    onOpenSearch(album.title)
                                                },
                                        ) {
                                            if (album.imageUrl != null) {
                                                AsyncImage(
                                                    model = album.imageUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(140.dp)
                                                        .clip(RoundedCornerShape(Corners.card)),
                                                )
                                            } else {
                                                Box(
                                                    Modifier
                                                        .size(140.dp)
                                                        .clip(RoundedCornerShape(Corners.card))
                                                        .background(AvatarGold),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        album.title.firstOrNull()?.toString() ?: "?",
                                                        fontSize = 48.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = InkBlack,
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                album.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = InkBlack,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                // Label tipe HANYA bila API
                                                // menyebutkannya; subtitle
                                                // apa adanya (mis. tahun).
                                                if (album.kindLabel.isNotEmpty()) {
                                                    "${album.kindLabel} • ${album.subtitle}"
                                                } else {
                                                    album.subtitle
                                                },
                                                fontSize = 13.sp,
                                                color = InkMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(Spacing.md))
                            }
                        }
                        // ---- About ----
                        if (!p.aboutText.isNullOrBlank()) {
                            item {
                                SectionHeader(title = "About", rightText = "ARTIST PAGE")
                            }
                            item {
                                Text(
                                    p.aboutText!!,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = InkBlack,
                                    modifier = Modifier.padding(
                                        horizontal = ContentMargin,
                                        vertical = 4.dp,
                                    ),
                                )
                                Spacer(Modifier.height(Spacing.md))
                            }
                        }
                        // ---- Similar artists ----
                        if (p.similarArtists.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Fans might also like",
                                    rightText = "TAP TO OPEN",
                                )
                            }
                            itemsIndexed(p.similarArtists, key = { i, a -> "similar:" + a.name + "#" + i }) { _, artist ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onOpenArtist(artist.name, artist.browseId)
                                        }
                                        .padding(
                                            horizontal = ContentMargin,
                                            vertical = 10.dp,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (artist.imageUrl != null) {
                                        AsyncImage(
                                            model = artist.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(AvatarGold),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                artist.name.firstOrNull()?.toString() ?: "?",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = OnAvatar,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        artist.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = InkMuted,
                                    )
                                }
                            }
                        }
                        // Profil kosong total (browse berhasil tapi
                        // semua seksi kosong) → empty state jujur.
                        if (p.topSongs.isEmpty() && p.albums.isEmpty() &&
                            p.aboutText.isNullOrBlank() && p.similarArtists.isEmpty()
                        ) {
                            item {
                                EmptyState(
                                    title = "Nothing here yet",
                                    subtitle = "This artist page came back empty.",
                                )
                            }
                        }
                        item { Spacer(Modifier.height(Spacing.xl)) }
                    }
                }
            }
        }
    }

    sheetTrack?.let { track ->
        TrackActionSheet(
            track = track,
            state = state,
            onDismiss = { sheetTrack = null },
            onOpenSearch = { q -> onOpenSearch(q) },
            onOpenArtist = onOpenArtist,
        )
    }

    // v1.10.21: sheet simpan banyak lagu — menutup sheet keluar mode.
    artistBulkSave?.let { bulkTracks ->
        SaveTracksToPlaylistSheet(
            tracks = bulkTracks,
            state = state,
            onDismiss = {
                artistBulkSave = null
                artistSelection.exitMode()
            },
        )
    }
}

/** Lagu terpilih di dua daftar profil artis (Songs + Top songs). */
private fun artistSelectedTracks(
    selection: com.sonara.app.ui.SelectionState,
    songsMerged: List<Pair<Track, Set<com.sonara.app.data.Source>>>,
    topMerged: List<Pair<Track, Set<com.sonara.app.data.Source>>>,
    songsKeys: List<String>,
    topKeys: List<String>,
): List<Track> {
    val out = ArrayList<Track>()
    songsMerged.forEachIndexed { i, (track, _) ->
        if (selection.isSelected(selectionKey("songs", songsKeys[i]))) out.add(track)
    }
    topMerged.forEachIndexed { i, (track, _) ->
        if (selection.isSelected(selectionKey("topsongs", topKeys[i]))) out.add(track)
    }
    return out
}

/** Avatar artis: foto API bila ada, inisial bila tidak. */
@Composable
private fun ArtistAvatar(imageUrl: String?, name: String) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, InkBlack, CircleShape),
        )
    } else {
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(AvatarGold)
                .border(2.dp, InkBlack, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.firstOrNull()?.toString() ?: "?",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = OnAvatar,
            )
        }
    }
}
