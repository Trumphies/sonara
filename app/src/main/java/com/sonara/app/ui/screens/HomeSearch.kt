package com.sonara.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.sonara.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonara.app.data.MoodChip
import com.sonara.app.data.HomeLayout
import com.sonara.app.data.LayoutPolicy
import com.sonara.app.data.SearchSort
import com.sonara.app.data.SearchSuggestion
import com.sonara.app.data.Source
import com.sonara.app.data.Engine
import com.sonara.app.data.YtmLinkResult
import com.sonara.app.data.parseYtmLink
import com.sonara.app.data.sortSearchTracks
import androidx.compose.material.icons.filled.Search
import com.sonara.app.data.archiveMixTracks
import com.sonara.app.data.communityPicksTracks
import com.sonara.app.data.CHARTS_PROVENANCE
import com.sonara.app.data.ChartRegion
import com.sonara.app.data.EXPLORE_TILES
import com.sonara.app.data.ExploreDestination
import com.sonara.app.data.MOOD_GROUPS
import com.sonara.app.data.RankedArtist
import com.sonara.app.data.RankedTrack
import com.sonara.app.data.chartRegionParams
import com.sonara.app.data.displayArtist
import com.sonara.app.data.formatDurationMs
import com.sonara.app.data.isUsableArtist
import com.sonara.app.data.KeepListeningPolicy
import com.sonara.app.data.moodChipLabel
import com.sonara.app.data.posterTracks
import com.sonara.app.data.recapCards
import com.sonara.app.data.replayMixTracks
import com.sonara.app.data.Track
import com.sonara.app.data.TrackDurations
import com.sonara.app.data.GenreApi
import com.sonara.app.data.PodcastApi
import com.sonara.app.data.YoutubeMusicApi
import com.sonara.app.data.resolveHeaderIdentity
import com.sonara.app.ui.ArusBackButton
import com.sonara.app.ui.ChromeScrollEffect
import com.sonara.app.ui.CoverArt
import com.sonara.app.ui.dashedBorder
import com.sonara.app.ui.ArusGearIcon
import com.sonara.app.ui.ArusSearchField
import com.sonara.app.ui.ContentMargin
import com.sonara.app.ui.EmptyState
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.SecondaryButton
import com.sonara.app.ui.SectionHeader
import com.sonara.app.ui.SelectionActionBar
import com.sonara.app.ui.SelectionState
import com.sonara.app.ui.rememberSelectionState
import com.sonara.app.ui.showToast
import com.sonara.app.ui.SkeletonHome
import com.sonara.app.ui.SkeletonRows
import com.sonara.app.ui.TrackRow
import com.sonara.app.data.mergeTracks
import com.sonara.app.data.occurrenceKeys
import com.sonara.app.data.selectionKey
import com.sonara.app.ui.theme.AvatarGold
import com.sonara.app.ui.theme.GenrePodcast
import com.sonara.app.ui.theme.BurntOrange
import com.sonara.app.ui.theme.SegmentBg
import com.sonara.app.ui.theme.AccentFill
import com.sonara.app.ui.theme.BurntOrangeSoft
import com.sonara.app.ui.theme.ChromeBg
import com.sonara.app.ui.GetarField
import com.sonara.app.ui.hardShadowDraw
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.displayCased
import com.sonara.app.ui.theme.Density
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.InkFixed
import com.sonara.app.ui.theme.GenreAcoustic
import com.sonara.app.ui.theme.OnAvatar
import com.sonara.app.ui.theme.GenreDangdut
import com.sonara.app.ui.theme.OnAccent
import com.sonara.app.ui.theme.GenreElectronic
import com.sonara.app.ui.theme.OnChrome
import com.sonara.app.ui.theme.GenreGalau
import com.sonara.app.ui.theme.PaperSurface
import com.sonara.app.ui.theme.GenreHipHop
import com.sonara.app.ui.theme.StageBg
import com.sonara.app.ui.theme.GenreIndie
import com.sonara.app.ui.theme.GenreJazz
import com.sonara.app.ui.theme.GenreKPop
import com.sonara.app.ui.theme.GenreMetal
import com.sonara.app.ui.theme.GenrePop
import com.sonara.app.ui.theme.PosterFallback
import com.sonara.app.ui.theme.GenrePodcast
import com.sonara.app.ui.theme.GenreRnB
import com.sonara.app.ui.theme.GenreRock
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.theme.PaperOutline
import com.sonara.app.ui.theme.Spacing
import com.sonara.app.ui.theme.Type
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..10 -> "morning"
    in 11..14 -> "afternoon"
    in 15..18 -> "evening"
    else -> "evening"
}

/**
 * Beranda persis screenshot ground truth v1.7:
 * header avatar gold "T" + sapaan mengikuti waktu + "tuan 👋" +
 * tombol not bulat putih; Kartu Poster gold 204dp; caption mono;
 * "Lagi ramai" + baris lagu format screenshot (cover 54dp,
 * titik sumber + durasi + label di kanan).
 * v1.10.4: bila login Google, header menampilkan identitas akun
 * Google (foto profil + display name) — belum login/logout =
 * tampilan lama. Data = pencarian YTM nyata + lagu yang disukai
 * (lokal) — tidak pernah konten palsu. Data dipegang SonaraState
 * supaya ganti tab tidak memuat ulang + kedip.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HomeScreen(
    state: SonaraState,
    onBack: () -> Unit,
    onOpenSearch: (String?) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetail: (Track) -> Unit,
    onOpenJam: () -> Unit,
    /** Auto-hide chrome bawah (v1.10.18). Null = tidak ada observer. */
    onChromeScroll: ((Int, Int) -> Unit)? = null,
    /** Buka profil artis dari sheet lagu (v1.10.18). */
    onOpenArtist: ((String, String?) -> Unit)? = null,
) {
    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    val player = state.player
    val scope = rememberCoroutineScope()
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    // v1.10.4: identitas akun Google untuk header (reaktif — berubah
    // begitu login/logout selesai, tanpa restart app).
    val syncState by state.sync.uiState.collectAsState()
    val identity = resolveHeaderIdentity(
        syncState.signedIn,
        syncState.displayName,
        syncState.photoUrl,
    )

    LaunchedEffect(Unit) {
        if (!state.homeLoaded && !state.homeLoading) state.loadHome(scope)
        state.loadHomeSections(scope)
    }
    // v1.10.21: rak artis diikuti + multi-select daftar Home.
    val homeContext = LocalContext.current
    val homeSelection = rememberSelectionState()
    var homeBulkSave by remember { mutableStateOf<List<Track>?>(null) }
    val homeJamState by state.jam.uiState.collectAsState()
    // v1.0.7: layout Beranda dibaca segar dari store tiap komposisi.
    val homeLayout = LayoutPolicy.fromStorage(state.store.homeLayout)
    BackHandler(enabled = homeSelection.modeActive) { homeSelection.exitMode() }
    LaunchedEffect(Unit) { state.loadFollowedReleases(scope) }
    LaunchedEffect(state.followedVersion) { state.loadFollowedReleases(scope) }
    // v1.10.18: chip mood (All = feed Beranda yang sudah ada;
    // mood lain = satu query bertema lewat jalur search yang sama).
    // v1.10.25: semua query Indonesia + gl=ID/hl=id.
    var mood by remember { mutableStateOf(MoodChip.ALL) }
    LaunchedEffect(mood) { state.loadMood(scope, mood) }
    // Data lokal ASLI untuk mix + recap, disegarkan tiap ganti lagu
    // (hitungan/riwayat dicatat saat lagu mulai diputar).
    val recentLocal = remember(currentTrack?.id) { state.store.recentTracks() }
    val playCountsLocal = remember(currentTrack?.id) { state.store.playCounts() }

    // Daftar Beranda: trending + lagu yang disukai (lokal),
    // duplikat judul+artis digabung jadi satu baris.
    val mergedTracks: List<Pair<Track, Set<Source>>> =
        mergeTracks(state.homeTrending + state.homeLiked)
    // v1.10.21: daftar berubah = seleksi basi.
    LaunchedEffect(mergedTracks, state.followedReleases, state.sectionCommunity, mood) {
        if (homeSelection.modeActive) homeSelection.exitMode()
    }
    val loaded = state.homeLoaded
    val loadFailed = state.homeFailed

    Column(Modifier.fillMaxSize()) {
        // Header (v1.10.18): Back DIHAPUS di Home root — Home
        // adalah tab root, tidak ada layar di bawahnya; Back sistem
        // tetap berfungsi. Avatar + gir 40dp, sapaan ellipsis.
        // v1.10.4: avatar = foto profil akun Google bila login
        // (inisial nama sebagai fallback bila foto gagal/offline),
        // nama = display name akun Google; belum login = "T"/"tuan".
        Box(Modifier.fillMaxWidth()) {
            // v1.10.27: field GETAR di belakang header Beranda —
            // mengalir saat lagu jalan (seed lagu berjalan), diam
            // saat jeda; statis saat reduced-motion (GetarField).
            val headerTokens = CurrentTokens
            if (headerTokens.flowField) {
                GetarField(
                    trackId = currentTrack?.id ?: "home",
                    playing = isPlaying,
                    darkGround = headerTokens.background.luminance() < 0.5f,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = ContentMargin,
                        vertical = Density.headerPaddingV,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AvatarGold)
                    .border(2.dp, InkBlack, CircleShape)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    identity.initial ?: "T",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = OnAvatar,
                )
                // Foto profil menutup inisial bila berhasil dimuat;
                // gagal/offline → AsyncImage tidak menggambar apa-apa
                // sehingga inisial tetap terlihat (tanpa crash).
                if (identity.photoUrl != null) {
                    AsyncImage(
                        model = identity.photoUrl,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().clip(CircleShape),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // v1.10.7: kolom sapaan/nama fleksibel — tiap baris
            // maxLines=1 + ellipsis, TIDAK PERNAH wrap di tengah
            // kata ("Good afternoo/n", "frien/d" di HP user v1.10.6).
            // Proporsi ditata ulang (avatar 48dp, gir 44dp, spacer
            // diperketat) supaya kolom tidak gepeng di 360dp.
            Column(Modifier.weight(1f)) {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                Text(
                    "Good ${greetingForHour(hour)},",
                    fontSize = 13.sp,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    identity.displayText,
                    fontSize = 22.sp,
                    fontFamily = CurrentTokens.displayFont,
                    fontWeight = FontWeight.Black,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            // v1.10.6: affordance Jam — satu elemen tambahan di sisi
            // kanan baris header (tidak menggeser layout lain);
            // v1.10.40: mark broadcast logo-saja (dulu pill "Jam" /
            // kode saat sesi aktif); cakram AccentFill menandai LIVE.
            Spacer(Modifier.width(6.dp))
            JamEntryPill(state = state, onClick = onOpenJam)
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PaperSurface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center,
            ) {
                // v1.10.6: gir KUSTOM gambar tangan (ArusGearIcon —
                // vektor orisinal gaya ARUS, bukan ikon template),
                // tint ink seperti glyph sebelumnya, posisi/ukuran/
                // perilaku tombol tidak berubah (buka Settings).
                Icon(
                    imageVector = ArusGearIcon,
                    contentDescription = "Settings",
                    tint = InkBlack,
                    modifier = Modifier.size(24.dp),
                )
            }
            }
        }

        if (!loaded) {
            SkeletonHome(Modifier.fillMaxWidth())
        } else if (loadFailed) {
            EmptyState(
                title = "Home couldn't load",
                subtitle = "Check your internet connection, then try again.",
                actionText = "Try again",
                onAction = { state.loadHome(scope) },
            )
        } else if (mergedTracks.isEmpty()) {
            EmptyState(
                title = "No songs here yet",
                subtitle = "Try searching in the Search tab.",
                actionText = "Try again",
                onAction = { state.loadHome(scope) },
            )
        } else {
            // v1.10.7: contentPadding bawah pengaman — padding
            // Scaffold sudah mengosongkan chrome bawah (strip +
            // mini player + nav); 8dp ini ruang napas ekstra supaya
            // baris terakhir tidak pernah terpotong di balik strip.
            val homeListState = rememberLazyListState()
            if (onChromeScroll != null) {
                ChromeScrollEffect(
                    listState = homeListState,
                    enabled = sheetTrack == null,
                    onScroll = onChromeScroll,
                )
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                state = homeListState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
            ) {
                    // Feed mengikuti mood: All = feed Beranda
                    // yang sudah ada; mood lain = moodTracks.
                    // Dihitung di sini (di luar scope item) supaya
                    // stickyHeader bar seleksi bisa memakainya.
                    val feedRows: List<Pair<Track, Set<Source>>> =
                        if (mood == MoodChip.ALL) {
                            mergedTracks
                        } else {
                            mergeTracks(state.moodTracks)
                        }
                    val railTracks = if (mood == MoodChip.ALL) {
                        posterTracks(state.homeTrending + state.homeTop)
                    } else {
                        posterTracks(state.moodTracks)
                    }
                    val community = communityPicksTracks(state.sectionCommunity)
                    val followedShelf = state.followedReleases
                    // Kunci dihitung langsung (tanpa remember): isi
                    // scope LazyColumn bukan konteks @Composable.
                    val feedKeys = occurrenceKeys(feedRows.map { it.first.id })
                    val communityKeys = occurrenceKeys(community.map { it.id })
                    val followedKeys = occurrenceKeys(followedShelf.map { it.id })
                    stickyHeader {
                        if (homeSelection.modeActive) {
                            SelectionActionBar(
                                count = homeSelection.count,
                                jamActive = homeJamState.active,
                                onAddToQueue = {
                                    val selected = homeSelectedTracks(
                                        homeSelection, feedRows, community, followedShelf,
                                        feedKeys, communityKeys, followedKeys,
                                    )
                                    val added = player.addTracksToQueue(selected)
                                    showToast(
                                        homeContext,
                                        if (added == 1) "Added 1 song to queue"
                                        else "Added $added songs to queue",
                                    )
                                    homeSelection.exitMode()
                                },
                                onSaveToPlaylist = {
                                    homeBulkSave = homeSelectedTracks(
                                        homeSelection, feedRows, community, followedShelf,
                                        feedKeys, communityKeys, followedKeys,
                                    )
                                },
                                onExit = { homeSelection.exitMode() },
                            )
                        }
                    }
                    item {
                        MoodChipBar(selected = mood, onSelect = { mood = it })
                    }
                    item {
                        HomePosterRail(
                            kicker = "The best tunes right now",
                            title = if (mood == MoodChip.ALL) {
                                "Hits today"
                            } else {
                                moodChipLabel(mood)
                            },
                            tracks = railTracks,
                            loading = mood != MoodChip.ALL && state.moodLoading,
                            layout = homeLayout,
                            onPlay = { track ->
                                player.playTrack(track)
                                onOpenDetail(track)
                            },
                        )
                    }
                    // Kartu bertinta "Community Picks" — SATU hasil
                    // pencarian bertema; label jujur, bukan klaim
                    // personalisasi ("Made for you" tidak ditulis).
                    if (community.isNotEmpty()) {
                        item {
                            CommunityPicksCard(
                                tracks = community,
                                onPlayAll = {
                                    player.playQueue(community, 0)
                                    onOpenDetail(community.first())
                                },
                                onPlay = { player.playTrack(it) },
                                onMenu = { sheetTrack = it },
                                selectionState = homeSelection,
                                selectionKeys = communityKeys,
                                isCurrentTrack = { trackId -> currentTrack?.id == trackId },
                                isPlaying = isPlaying,
                                durations = state.durations,
                            )
                        }
                    } else if (state.sectionsLoaded) {
                        item {
                            HomeSectionError(
                                text = "Couldn't load Community Picks.",
                                onRetry = { state.loadHomeSections(scope, force = true) },
                            )
                        }
                    }
                    // Mix besar dari data lokal ASLI (riwayat +
                    // hitungan putar). Keduanya kosong → seksi
                    // disembunyikan (tanpa mix karangan).
                    val archiveMix = archiveMixTracks(recentLocal, playCountsLocal)
                    val replayMix = replayMixTracks(playCountsLocal)
                    if (archiveMix.isNotEmpty() || replayMix.isNotEmpty()) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = ContentMargin,
                                        vertical = 8.dp,
                                    ),
                            ) {
                                if (archiveMix.isNotEmpty()) {
                                    MixCard(
                                        title = "Archive Mix",
                                        tracks = archiveMix,
                                        onClick = {
                                            player.playQueue(archiveMix, 0)
                                            onOpenDetail(archiveMix.first())
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (archiveMix.isNotEmpty() && replayMix.isNotEmpty()) {
                                    Spacer(Modifier.width(12.dp))
                                }
                                if (replayMix.isNotEmpty()) {
                                    MixCard(
                                        title = "Replay Mix",
                                        tracks = replayMix,
                                        onClick = {
                                            player.playQueue(replayMix, 0)
                                            onOpenDetail(replayMix.first())
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    // Workout: kartu lebar horizontal.
                    if (state.sectionWorkout.isNotEmpty()) {
                        item {
                            HomeWideRail(
                                title = "Workout",
                                tracks = state.sectionWorkout,
                                layout = homeLayout,
                                onPlay = { player.playTrack(it) },
                            )
                        }
                    } else if (state.sectionsLoaded) {
                        item {
                            HomeSectionError(
                                text = "Couldn't load Workout songs.",
                                onRetry = { state.loadHomeSections(scope, force = true) },
                            )
                        }
                    }
                    // New releases: poster row dari pencarian rilisan baru.
                    if (state.sectionNewReleases.isNotEmpty()) {
                        item {
                            HomePosterRail(
                                kicker = null,
                                title = "New releases",
                                tracks = posterTracks(state.sectionNewReleases),
                                loading = false,
                                layout = homeLayout,
                                onPlay = { track ->
                                    player.playTrack(track)
                                    onOpenDetail(track)
                                },
                            )
                        }
                    } else if (state.sectionsLoaded) {
                        item {
                            HomeSectionError(
                                text = "Couldn't load new releases.",
                                onRetry = { state.loadHomeSections(scope, force = true) },
                            )
                        }
                    }
                    // v1.0.5: rak Keep listening — episode/konten
                    // panjang dengan posisi tersimpan, terbaru dulu,
                    // tanpa yang sudah ditandai played. Dibaca langsung
                    // dari store tiap komposisi (store tidak reaktif) —
                    // kembali dari halaman acara langsung segar.
                    // Kosong = disembunyikan, tanpa konten palsu.
                    val keepListening = KeepListeningPolicy.keepListeningList(
                        state.store.resumeSnapshots(),
                        state.store.playedEpisodeIds(),
                    )
                    if (keepListening.isNotEmpty()) {
                        item {
                            Column {
                                SectionHeader(
                                    title = "Keep listening",
                                    rightText = "${keepListening.size} EPISODES",
                                )
                                for ((kIndex, snap) in keepListening.withIndex()) {
                                    val kTrack = Track(
                                        id = snap.trackId,
                                        title = snap.title,
                                        artist = snap.artist,
                                        album = "",
                                        source = Source.YOUTUBE_MUSIC,
                                        engine = Engine.YOUTUBE,
                                        durationMs = snap.durationMs,
                                        youtubeVideoId = snap.videoId,
                                        imageUrl = snap.imageUrl,
                                        isPodcastEpisode = true,
                                        podcastShowTitle = snap.showTitle,
                                        podcastShowBrowseId = snap.showBrowseId
                                            .takeIf { it.isNotBlank() },
                                    )
                                    val kKey = selectionKey("keeplisten", snap.trackId)
                                    key(kKey) {
                                        TrackRow(
                                            chartNumber = kIndex + 1,
                                            track = kTrack,
                                            durations = state.durations,
                                            onClick = {
                                                if (homeSelection.modeActive) homeSelection.toggle(kKey)
                                                else {
                                                    player.playTrack(kTrack)
                                                    onOpenDetail(kTrack)
                                                }
                                            },
                                            onLongClick = {
                                                if (homeSelection.modeActive) homeSelection.toggle(kKey)
                                                else homeSelection.enter(kKey)
                                            },
                                            isCurrent = currentTrack?.id == kTrack.id,
                                            isPlaying = isPlaying,
                                            selected = homeSelection.isSelected(kKey),
                                            trailing = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        "${formatDurationMs(KeepListeningPolicy.remainingMs(snap))} left",
                                                        fontSize = 12.sp,
                                                        color = InkMuted,
                                                    )
                                                    IconButton(onClick = { sheetTrack = kTrack }) {
                                                        Icon(
                                                            imageVector = Icons.Filled.MoreVert,
                                                            contentDescription = "Episode menu",
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // v1.10.21: rak artis diikuti — hanya bila ada
                    // yang diikuti DAN ada isinya (cache/refresh);
                    // kosong = disembunyikan, tanpa konten palsu.
                    if (followedShelf.isNotEmpty()) {
                        item {
                            Column {
                                SectionHeader(
                                    title = "New from artists you follow",
                                    rightText = "${followedShelf.size} SONGS",
                                )
                                for ((fIndex, fTrack) in followedShelf.withIndex()) {
                                    val fKey = selectionKey("followed", followedKeys[fIndex])
                                    key(fKey) {
                                        TrackRow(
                                            chartNumber = fIndex + 1,
                                            track = fTrack,
                                            durations = state.durations,
                                            onClick = {
                                                if (homeSelection.modeActive) homeSelection.toggle(fKey)
                                                else {
                                                    player.playTrack(fTrack)
                                                    onOpenDetail(fTrack)
                                                }
                                            },
                                            onLongClick = {
                                                if (homeSelection.modeActive) homeSelection.toggle(fKey)
                                                else homeSelection.enter(fKey)
                                            },
                                            isCurrent = currentTrack?.id == fTrack.id,
                                            isPlaying = isPlaying,
                                            selected = homeSelection.isSelected(fKey),
                                            trailing = {
                                                IconButton(onClick = { sheetTrack = fTrack }) {
                                                    Icon(
                                                        imageVector = Icons.Filled.MoreVert,
                                                        contentDescription = "Song menu",
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Recap: 2 kartu warna-warni dari data lokal asli,
                    // tanpa klaim rentang waktu. Kosong → sembunyi.
                    val recaps = recapCards(playCountsLocal, recentLocal)
                    if (recaps.isNotEmpty()) {
                        item {
                            HomeRecapRail(
                                cards = recaps,
                                onPlay = { card ->
                                    player.playQueue(card.tracks, 0)
                                    onOpenDetail(card.tracks.first())
                                },
                            )
                        }
                    }
                    // Kartu stasiun gelap: masuk Jam dari Home.
                    item {
                        JamStationCard(onOpenJam = onOpenJam)
                    }
                    item {
                        SectionHeader(
                            title = if (mood == MoodChip.ALL) {
                                "Trending now"
                            } else {
                                moodChipLabel(mood)
                            },
                            rightText = "${feedRows.size} SONGS",
                        )
                    }
                    if (mood != MoodChip.ALL && state.moodLoading && feedRows.isEmpty()) {
                        item {
                            SkeletonRows(count = 6, modifier = Modifier.fillMaxWidth())
                        }
                    } else if (feedRows.isEmpty()) {
                        item {
                            Text(
                                "No songs found for this mood yet.",
                                fontSize = 14.sp,
                                color = InkMuted,
                                modifier = Modifier.padding(
                                    horizontal = ContentMargin,
                                    vertical = 8.dp,
                                ),
                            )
                        }
                    }
                    itemsIndexed(
                        feedRows,
                        key = { i, (t, _) -> t.id + "#" + i },
                    ) { index, (track, sources) ->
                        val feedKey = selectionKey("feed", feedKeys[index])
                        TrackRow(
                            chartNumber = index + 1,
                            track = track,
                            durations = state.durations,
                            sources = sources,
                            onClick = {
                                if (homeSelection.modeActive) homeSelection.toggle(feedKey)
                                else player.playTrack(track)
                            },
                            onLongClick = {
                                if (homeSelection.modeActive) homeSelection.toggle(feedKey)
                                else homeSelection.enter(feedKey)
                            },
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            selected = homeSelection.isSelected(feedKey),
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
                    item {
                        Spacer(Modifier.height(Spacing.xl))
                    }
                }
        }
    }

    sheetTrack?.let { track ->
        TrackActionSheet(
            track = track,
            state = state,
            onDismiss = { sheetTrack = null },
            onOpenSearch = { query -> onOpenSearch(query) },
            onOpenArtist = onOpenArtist,
        )
    }

    // v1.10.21: sheet simpan banyak lagu — menutup sheet keluar mode.
    homeBulkSave?.let { bulkTracks ->
        SaveTracksToPlaylistSheet(
            tracks = bulkTracks,
            state = state,
            onDismiss = {
                homeBulkSave = null
                homeSelection.exitMode()
            },
        )
    }
}


// ---- Komponen tumpukan Home baru (v1.10.18) ----------------------------

/** Lagu terpilih di semua daftar Home (feed, Community Picks, rak ikuti). */
private fun homeSelectedTracks(
    selection: SelectionState,
    feedRows: List<Pair<Track, Set<Source>>>,
    community: List<Track>,
    followedShelf: List<Track>,
    feedKeys: List<String>,
    communityKeys: List<String>,
    followedKeys: List<String>,
): List<Track> {
    val out = ArrayList<Track>()
    feedRows.forEachIndexed { i, (track, _) ->
        if (selection.isSelected(selectionKey("feed", feedKeys[i]))) out.add(track)
    }
    community.forEachIndexed { i, track ->
        if (selection.isSelected(selectionKey("picks", communityKeys[i]))) out.add(track)
    }
    followedShelf.forEachIndexed { i, track ->
        if (selection.isSelected(selectionKey("followed", followedKeys[i]))) out.add(track)
    }
    return out
}

/** Bar chip mood tap-only di bawah header Home. */
@Composable
private fun MoodChipBar(selected: MoodChip, onSelect: (MoodChip) -> Unit) {
    val moodTokens = CurrentTokens
    LazyRow(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ContentMargin),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(MoodChip.entries, key = { it.name }) { mood ->
            val isSelected = mood == selected
            Box(
                Modifier
                    .height(40.dp)
                    .hardShadowDraw(Corners.chip, moodTokens.hardShadowChip)
                    .clip(RoundedCornerShape(Corners.chip))
                    .background(if (isSelected) ChromeBg else PaperSurface)
                    .border(
                        moodTokens.cardBorderWidth,
                        PaperOutline,
                        RoundedCornerShape(Corners.chip),
                    )
                    .clickable { onSelect(mood) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    moodChipLabel(mood),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) OnChrome else InkBlack,
                )
            }
        }
    }
}

/** Rel poster horizontal (artwork asli + judul + artis, tap = putar). */
@Composable
private fun HomePosterRail(
    kicker: String?,
    title: String,
    tracks: List<Track>,
    loading: Boolean,
    onPlay: (Track) -> Unit,
    /** v1.0.7: EDITORIAL = rel horizontal; COMPACT = daftar; GRID = ubin 2 kolom. */
    layout: HomeLayout = HomeLayout.EDITORIAL,
) {
    Column {
        if (kicker != null) {
            Text(
                kicker,
                fontSize = 12.sp,
                color = InkMuted,
                modifier = Modifier.padding(
                    start = ContentMargin,
                    end = ContentMargin,
                    top = 8.dp,
                ),
            )
        }
        SectionHeader(title = title, rightText = "TAP TO PLAY")
        if (loading) {
            Text(
                "Loading…",
                fontSize = 14.sp,
                color = InkMuted,
                modifier = Modifier.padding(horizontal = ContentMargin, vertical = 8.dp),
            )
            return
        }
        if (tracks.isEmpty()) return
        // v1.0.7: COMPACT/GRID dirender non-lazy di dalam Column yang
        // sama; EDITORIAL jatuh ke LazyRow di bawah (tidak berubah).
        val railMode = LayoutPolicy.railMode(layout)
        if (railMode != LayoutPolicy.RailMode.RAIL) {
            if (railMode == LayoutPolicy.RailMode.LIST) {
                Column(Modifier.padding(horizontal = ContentMargin)) {
                    for (track in tracks) {
                        HomeCompactRow(track = track, onPlay = onPlay)
                    }
                }
            } else {
                Column(
                    Modifier.padding(horizontal = ContentMargin),
                    verticalArrangement = Arrangement.spacedBy(Density.gridGap),
                ) {
                    for (chunk in tracks.chunked(2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Density.gridGap)) {
                            for (track in chunk) {
                                Box(Modifier.weight(1f)) {
                                    HomeGridTile(track = track, onPlay = onPlay)
                                }
                            }
                            if (chunk.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            return
        }
        LazyRow(
            Modifier
                .fillMaxWidth()
                .height(196.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ContentMargin),
            horizontalArrangement = Arrangement.spacedBy(Density.gridGap),
        ) {
            itemsIndexed(tracks, key = { i, t -> t.id + "#" + i }) { _, track ->
                Column(
                    Modifier
                        .width(140.dp)
                        .clickable { onPlay(track) },
                ) {
                    CoverArt(track = track, size = 140.dp, corner = Corners.large)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        displayArtist(track.artist),
                        fontSize = 13.sp,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Satu ubin grid Beranda (v1.0.7): CoverArt + judul + artis. */
@Composable
private fun HomeGridTile(track: Track, onPlay: (Track) -> Unit) {
    Column(Modifier.clickable { onPlay(track) }) {
        CoverArt(track = track, size = 160.dp, corner = Corners.large)
        Spacer(Modifier.height(8.dp))
        Text(
            track.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            displayArtist(track.artist),
            fontSize = 13.sp,
            color = InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Satu baris rapat Beranda (v1.0.7): cover kecil + judul + artis. */
@Composable
private fun HomeCompactRow(track: Track, onPlay: (Track) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onPlay(track) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(track = track, size = 44.dp, corner = Corners.medium)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                displayArtist(track.artist),
                fontSize = 13.sp,
                color = InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = BurntOrange,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Kartu bertinta "Community Picks": tint oranye muda ARUS + header
 * (judul + tombol play besar) + 3 baris lagu dengan menu 3-titik.
 */
@Composable
private fun CommunityPicksCard(
    tracks: List<Track>,
    onPlayAll: () -> Unit,
    onPlay: (Track) -> Unit,
    onMenu: (Track) -> Unit,
    /** Multi-select (v1.10.21): null = tidak ada seleksi di kartu ini. */
    selectionState: SelectionState? = null,
    selectionKeys: List<String> = emptyList(),
    isCurrentTrack: (String) -> Boolean = { false },
    isPlaying: Boolean = false,
    /** Durasi per videoId (v1.10.31) — diteruskan ke TrackRow. */
    durations: TrackDurations? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentMargin, vertical = 8.dp)
            .clip(RoundedCornerShape(Corners.card))
            .background(BurntOrangeSoft)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    displayCased("Community Picks", CurrentTokens),
                    fontSize = 20.sp,
                    fontFamily = CurrentTokens.displayFont,
                    letterSpacing = CurrentTokens.displayLetterSpacing,
                    fontWeight = FontWeight.Black,
                    color = InkBlack,
                )
                Text(
                    "Community favorites, from YouTube Music search",
                    fontSize = 13.sp,
                    color = InkMuted,
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentFill)
                    .clickable(onClick = onPlayAll),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Community Picks",
                    tint = OnAccent,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        for ((pIndex, track) in tracks.withIndex()) {
            val pickKey = selectionKey(
                "picks",
                selectionKeys.getOrElse(pIndex) { "${track.id}#$pIndex" },
            )
            val picking = selectionState?.modeActive == true
            TrackRow(
                chartNumber = pIndex + 1,
                track = track,
                durations = durations,
                onClick = {
                    if (picking) selectionState?.toggle(pickKey) else onPlay(track)
                },
                onLongClick = {
                    if (picking) selectionState?.toggle(pickKey)
                    else selectionState?.enter(pickKey)
                },
                isCurrent = isCurrentTrack(track.id),
                isPlaying = isPlaying,
                selected = selectionState?.isSelected(pickKey) == true,
                trailing = {
                    IconButton(onClick = { onMenu(track) }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Song menu",
                        )
                    }
                },
            )
        }
    }
}

/** Kartu mix besar: artwork asli lagu pertama + scrim + judul. */
@Composable
private fun MixCard(
    title: String,
    tracks: List<Track>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = tracks.first()
    Box(
        modifier
            .height(148.dp)
            .clip(RoundedCornerShape(Corners.card))
            .background(StageBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (!first.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = first.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f),
                            ),
                        ),
                    ),
            )
        }
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                "${tracks.size} songs",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                // v1.10.34: InkFixed, bukan InkBlack — lingkaran ini
                // putih paten; InkBlack membalik antar mode dan tak
                // kelihatan di 4 varian (cacat yang sama dengan
                // tombol Back, lihat ArusBackButton).
                tint = InkFixed,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Rel kartu lebar horizontal (Workout): kartu putih + artwork + teks. */
@Composable
private fun HomeWideRail(
    title: String,
    tracks: List<Track>,
    onPlay: (Track) -> Unit,
    /** v1.0.7: sama seperti HomePosterRail — rel / daftar / grid. */
    layout: HomeLayout = HomeLayout.EDITORIAL,
) {
    Column {
        SectionHeader(title = title, rightText = "TAP TO PLAY")
        // v1.0.7: COMPACT/GRID seperti HomePosterRail; EDITORIAL ke LazyRow.
        val wideMode = LayoutPolicy.railMode(layout)
        if (wideMode != LayoutPolicy.RailMode.RAIL) {
            if (wideMode == LayoutPolicy.RailMode.LIST) {
                Column(Modifier.padding(horizontal = ContentMargin)) {
                    for (track in tracks) {
                        HomeCompactRow(track = track, onPlay = onPlay)
                    }
                }
            } else {
                Column(
                    Modifier.padding(horizontal = ContentMargin),
                    verticalArrangement = Arrangement.spacedBy(Density.gridGap),
                ) {
                    for (chunk in tracks.chunked(2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Density.gridGap)) {
                            for (track in chunk) {
                                Box(Modifier.weight(1f)) {
                                    HomeGridTile(track = track, onPlay = onPlay)
                                }
                            }
                            if (chunk.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            return
        }
        LazyRow(
            Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ContentMargin),
            horizontalArrangement = Arrangement.spacedBy(Density.gridGap),
        ) {
            itemsIndexed(tracks, key = { i, t -> t.id + "#" + i }) { _, track ->
                Row(
                    Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(Corners.card))
                        .background(PaperSurface)
                        .border(
                            CurrentTokens.cardBorderWidth,
                            PaperOutline,
                            RoundedCornerShape(Corners.card),
                        )
                        .clickable { onPlay(track) }
                        .padding(Density.cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverArt(track = track, size = 56.dp, corner = Corners.medium)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            displayArtist(track.artist),
                            fontSize = 13.sp,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = BurntOrange,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Rel kartu recap warna-warni (warna digambar sendiri, bukan aset YTM). */
@Composable
private fun HomeRecapRail(
    cards: List<com.sonara.app.data.RecapCard>,
    onPlay: (com.sonara.app.data.RecapCard) -> Unit,
) {
    // Lime + magenta, digambar sendiri sebagai warna blok.
    val recapColors = listOf(Color(0xFFD8F65C), Color(0xFFF26D9D))
    Column {
        SectionHeader(title = "Your recap", rightText = "TAP TO PLAY")
        LazyRow(
            Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ContentMargin),
            horizontalArrangement = Arrangement.spacedBy(Density.gridGap),
        ) {
            itemsIndexed(cards, key = { i, c -> c.title + i }) { index, card ->
                Column(
                    Modifier
                        .width(200.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(Corners.card))
                        .background(recapColors[index % recapColors.size])
                        .clickable { onPlay(card) }
                        .padding(16.dp),
                ) {
                    Text(
                        card.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = InkBlack,
                    )
                    Text(
                        card.subtitle,
                        fontSize = 13.sp,
                        color = InkBlack.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        card.tracks.firstOrNull()?.title ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${card.tracks.size} songs",
                        fontSize = 12.sp,
                        color = InkBlack.copy(alpha = 0.7f),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Kartu stasiun gelap "Listen together, in sync" + mark broadcast. */
@Composable
private fun JamStationCard(onOpenJam: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentMargin, vertical = 8.dp)
            .clip(RoundedCornerShape(Corners.large))
            .background(StageBg)
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_jam_broadcast_dark),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Listen together, in sync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "Start a Jam and listen with friends, in sync.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .hardShadowDraw(Corners.button, CurrentTokens.hardShadowChip)
                    .clip(RoundedCornerShape(Corners.button))
                    .background(AccentFill)
                    .clickable(onClick = onOpenJam)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Start a Jam",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnAccent,
                )
            }
        }
    }
}

/** Baris error per seksi (gagal load) — tidak mengosongkan Home. */
@Composable
private fun HomeSectionError(text: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentMargin, vertical = 8.dp),
    ) {
        Text(text, fontSize = 14.sp, color = InkMuted)
        Spacer(Modifier.height(8.dp))
        SecondaryButton(text = "Try again", onClick = onRetry)
    }
}

private data class GenreTileData(val name: String, val color: Color, val query: String = name)

/** Target halaman genre: label tampil + query jalur fallback search. */
private data class GenreTarget(val label: String, val query: String)

private val GENRES = listOf(
    GenreTileData("Pop", GenrePop),
    GenreTileData("Indie", GenreIndie),
    GenreTileData("Heartbreak", GenreGalau),
    GenreTileData("Rock", GenreRock),
    // v1.10.7: kategori diperbanyak (13 total bersama Podcasts) —
    // selera Indonesia + global. Label merangkap query pencarian
    // (seperti "Heartbreak" menggantikan "Galau"): tiap label
    // harus menghasilkan hasil pencarian yang benar; query
    // dikirim lewat body JSON POST (aman untuk "R&B").
    GenreTileData("Dangdut", GenreDangdut),
    GenreTileData("K-Pop", GenreKPop),
    GenreTileData("Hip-Hop", GenreHipHop),
    GenreTileData("R&B", GenreRnB),
    GenreTileData("Jazz", GenreJazz),
    GenreTileData("Electronic", GenreElectronic),
    GenreTileData("Acoustic", GenreAcoustic),
    GenreTileData("Metal", GenreMetal),
)

// Semua kategori Browse, termasuk Podcasts (v1.10.6). Artwork tile
// diambil dari hasil teratas pencarian YTM kategori itu (bukan
// gambar buatan) — lihat SonaraState.loadGenreArtworks. YtmResult
// tidak membawa penanda tipe podcast, jadi tile Podcasts adalah
// pencarian podcast (query "podcasts"); perilakunya ditulis jujur
// di BUILD-v1.10.6.md.
private val BROWSE_CATEGORIES = GENRES + GenreTileData("Podcasts", GenrePodcast, query = "podcasts")

/**
 * Tile kategori ber-artwork asli YTM (v1.10.6): artwork hasil
 * teratas + scrim gelap + label. Bila artwork belum/gagal dimuat,
 * fallback jujur ke blok warna kategori (bukan tile kosong/rusak).
 */
@Composable
private fun GenreArtworkTile(
    genre: GenreTileData,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = CurrentTokens
    val tileShape = RoundedCornerShape(Corners.tile)
    // v1.10.33: tile 1:1 — lebar dari weight pemanggil, tinggi mengikuti
    // (dulu height 104.dp tetap).
    Box(modifier.aspectRatio(1f)) {
        // Blok: bayangan keras + border tebal (prototype).
        if (tokens.hardShadow) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(tokens.hardShadowTile, tokens.hardShadowTile)
                    .clip(tileShape)
                    .background(tokens.text),
            )
        }
    Box(
        Modifier
            .fillMaxSize()
            .clip(tileShape)
            .background(genre.color)
            .then(
                when {
                    // PASAR: border putus-putus 2dp di atas tile.
                    tokens.dashedBorders -> Modifier.dashedBorder(2.dp, tokens.text, Corners.tile)
                    tokens.hardShadow -> Modifier.border(2.dp, tokens.text, tileShape)
                    else -> Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
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
        } else {
            // v1.10.34: scrim yang SAMA di atas warna fallback —
            // tanpa artwork, label putih di atas warna terang
            // (Hip-Hop 1.79:1, Electronic 2.74:1, Jazz 2.94:1)
            // praktis tak terbaca; dengan scrim 0.55 semua warna
            // fallback >= 7.34:1. Hue tile tidak berubah.
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
        Text(
            displayCased(genre.name, tokens),
            fontSize = 20.sp,
            fontFamily = tokens.displayFont,
            letterSpacing = tokens.displayLetterSpacing,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(Density.cardPadding),
        )
    }
    }
}

/** Rel lagu horizontal untuk seksi Browse (Popular now / Trending). */
@Composable
private fun BrowseSongRail(
    title: String,
    tracks: List<Track>,
    homeLoaded: Boolean,
    onPlay: (Track) -> Unit,
    onRetry: () -> Unit,
) {
    SectionHeader(title = title, rightText = "TAP TO PLAY")
    if (!homeLoaded) return
    if (tracks.isEmpty()) {
        Column(Modifier.padding(horizontal = ContentMargin, vertical = 8.dp)) {
            Text(
                "Couldn't load songs from YouTube Music.",
                fontSize = 14.sp,
                color = InkMuted,
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Try again", onClick = onRetry)
        }
        return
    }
    LazyRow(
        Modifier
            .fillMaxWidth()
            .height(196.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ContentMargin),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Density.gridGap),
    ) {
        itemsIndexed(tracks, key = { i, t -> t.id + "#" + i }) { _, track ->
            Column(
                Modifier
                    .width(140.dp)
                    .clickable { onPlay(track) },
            ) {
                CoverArt(track = track, size = 140.dp, corner = Corners.large)
                Spacer(Modifier.height(8.dp))
                Text(
                    track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    displayArtist(track.artist),
                    fontSize = 13.sp,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Cari persis screenshot ground truth v1.7:
 * headline editorial 2 baris + subteks + kolom cari pill putih
 * border gelap 2dp + "Jelajahi" + tile genre 2×2 + "Semua lagu"
 * + daftar format screenshot. Pencarian HIDUP (debounce 500ms),
 * sumber tunggal YouTube Music. Tap tile genre = cari genre itu
 * (data real).
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SearchScreen(
    state: SonaraState,
    onBack: () -> Unit,
    presetQuery: String?,
    onPresetConsumed: () -> Unit,
    /** Auto-hide chrome bawah (v1.10.18). Null = tidak ada observer. */
    onChromeScroll: ((Int, Int) -> Unit)? = null,
    /** Buka profil artis dari sheet lagu (v1.10.18). */
    onOpenArtist: ((String, String?) -> Unit)? = null,
    /** Buka halaman acara podcast dari baris acara hasil Cari (v1.0.5). */
    onOpenPodcastShow: ((String, String) -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    // v1.10.33: halaman genre khusus (tap tile genre/mood membuka
    // halaman genre ala YTM, bukan daftar hasil search) + grid Browse
    // ciut (4 genre + View all / Show less).
    var genreTarget by remember { mutableStateOf<GenreTarget?>(null) }
    var genresExpanded by remember { mutableStateOf(false) }
    BackHandler(enabled = genreTarget != null) { genreTarget = null }
    val scope = rememberCoroutineScope()
    val ytmApi = remember { YoutubeMusicApi() }
    val currentTrack by state.player.currentTrack.collectAsState()
    val isPlaying by state.player.isPlaying.collectAsState()
    val focusManager = LocalFocusManager.current
    val searchContext = LocalContext.current
    // v1.0.3: urutan hasil + saran ketikan.
    var searchSort by remember { mutableStateOf(SearchSort.BEST_MATCH) }
    var suggestions by remember { mutableStateOf<List<SearchSuggestion>>(emptyList()) }

    // ---- Riwayat pencarian (v1.10.16, lokal saja) ----
    // Hanya query yang benar-benar dieksekusi (tombol Search /
    // ketuk hasil). Query kategori Browse (tile) bukan pencarian
    // ketikan user — tidak dicatat.
    var recentVersion by remember { mutableStateOf(0) }
    val recentSearches = remember(recentVersion) { state.store.recentSearches() }
    val genreQueries = remember { BROWSE_CATEGORIES.map { it.query }.toSet() }
    fun recordSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.length < 2 || genreQueries.contains(trimmed)) return
        // Link yang ditempel bukan pencarian — tidak dicatat.
        if (parseYtmLink(trimmed) != YtmLinkResult.Invalid || trimmed.contains("://")) return
        state.store.addRecentSearch(trimmed)
        recentVersion++
    }

    /**
     * Link yang ditempel (v1.0.3): video → pola pending-link yang
     * sama dengan MainActivity (SonaraApp me-resolve + memutar +
     * membuka detail); playlist saja → toast jujur; link layanan
     * lain (link layanan lain tidak didukung) → toast jujur,
     * tanpa pencarian teks sampah. True = sudah ditangani.
     */
    fun openPastedLink(text: String): Boolean {
        val q = text.trim()
        return when (val parsed = parseYtmLink(q)) {
            is YtmLinkResult.Video -> {
                suggestions = emptyList()
                state.app.pendingYtmVideoId.value = parsed.videoId
                true
            }
            is YtmLinkResult.PlaylistOnly -> {
                showToast(
                    searchContext,
                    "Playlists from links aren't supported yet — try searching the playlist name.",
                )
                true
            }
            YtmLinkResult.Invalid -> {
                if (q.contains("://")) {
                    showToast(
                        searchContext,
                        "That link isn't supported — paste a YouTube Music link.",
                    )
                    true
                } else {
                    false
                }
            }
        }
    }

    // Data browse "Semua lagu" (saat query kosong): katalog nyata
    // Beranda yang sudah dimuat di root.
    LaunchedEffect(Unit) {
        if (!state.homeLoaded && !state.homeLoading) state.loadHome(scope)
        state.loadGenreArtworks(scope)
    }
    val browseTracks = remember(state.homeTrending, state.homeTop, state.homeLiked) {
        mergeTracks(state.homeTrending + state.homeTop + state.homeLiked)
    }

    fun doSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        // v1.0.3: link ditempel → buka langsung, JANGAN dicari
        // sebagai teks.
        if (openPastedLink(q)) return
        state.searchError = null
        state.searchBusy = true
        state.searchResults = emptyList()
        searchJob?.cancel()
        searchJob = scope.launch {
            val ytmResults = ArrayList<Track>()
            var ytmError: String? = null
            var showRows = emptyList<Track>()
            withContext(Dispatchers.IO) {
                // v1.0.3: urutan selain best match memaksa jalur
                // Kotlin (urut sisi klien dari hitungan/tahun yang
                // benar-benar dibawa respons — lihat SearchSortPolicy).
                ytmApi.search(q, forceKotlin = searchSort != SearchSort.BEST_MATCH)
                    .onSuccess { ytmResults.addAll(it) }
                    .onFailure { ytmError = it.message }
                // v1.0.5: baris acara podcast dari respons search yang
                // sama (jalur terpisah — Go engine tidak membawa baris
                // acara). Gagal → daftar kosong, hasil lagu tetap tampil.
                showRows = ytmApi.searchShows(q).take(3).map { row ->
                    Track(
                        id = "show:${row.browseId}",
                        title = row.title,
                        artist = if (row.host.isNotBlank()) "Podcast • ${row.host}" else "Podcast",
                        album = "",
                        source = Source.YOUTUBE_MUSIC,
                        engine = Engine.YOUTUBE,
                        durationMs = 0,
                        youtubeVideoId = null,
                        imageUrl = row.imageUrl,
                        podcastShowBrowseId = row.browseId,
                        podcastShowTitle = row.title,
                    )
                }
            }
            state.searchResults = showRows + sortSearchTracks(ytmResults, searchSort)
            state.searchError = when {
                ytmResults.isNotEmpty() || showRows.isNotEmpty() -> null
                ytmError != null -> ytmError
                else -> null
            }
            state.searchBusy = false
        }
    }

    LaunchedEffect(presetQuery) {
        if (presetQuery != null) {
            query = presetQuery
            onPresetConsumed()
        }
    }

    // Pencarian HIDUP: mengetik langsung mencari setelah jeda
    // singkat. Query kosong mengosongkan hasil (kembali ke browse).
    // Ganti urutan (v1.0.3) menjalankan ulang pencarian yang sama.
    // Link yang ditempel menunggu Enter — tidak dicari sebagai teks
    // dan tidak auto-play (openPastedLink hanya dari doSearch/Enter).
    LaunchedEffect(query, searchSort) {
        if (query.isBlank()) {
            searchJob?.cancel()
            state.searchResults = emptyList()
            state.searchError = null
            state.searchBusy = false
        } else if (parseYtmLink(query.trim()) != YtmLinkResult.Invalid ||
            query.trim().contains("://")
        ) {
            searchJob?.cancel()
            state.searchResults = emptyList()
            state.searchError = null
            state.searchBusy = false
        } else {
            delay(500)
            doSearch()
        }
    }

    // Saran ketikan (v1.0.3): debounce 300ms, jalur suggest YTM
    // signed-out; hasil basi (query sudah berubah) dibuang.
    // Link / query terlalu pendek = tidak ada saran.
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2 || parseYtmLink(q) != YtmLinkResult.Invalid || q.contains("://")) {
            suggestions = emptyList()
        } else {
            delay(300)
            val fetched = withContext(Dispatchers.IO) { ytmApi.searchSuggestions(q) }
            if (query.trim() == q) {
                suggestions = fetched.take(6)
            }
        }
    }

    // v1.10.33: halaman genre — pola yang sama dengan sub-halaman
    // Explore: return dini, Back kembali ke Browse.
    val currentGenreTarget = genreTarget
    if (currentGenreTarget != null) {
        GenrePage(
            state = state,
            scope = scope,
            target = currentGenreTarget,
            artworkUrl = state.genreArtworks[currentGenreTarget.label],
            onBack = { genreTarget = null },
            onPlay = { state.player.playTrack(it) },
            onPlayQueue = { tracks -> state.player.playQueue(tracks, 0) },
            onOpenSearch = { q ->
                query = q
                genreTarget = null
                focusManager.clearFocus()
            },
            onOpenArtist = onOpenArtist,
        )
        return
    }

    // v1.10.18: sub-halaman Explore — return dini, Back kembali
    // ke Browse (pola yang sama dengan halaman genre di atas).
    var subPage by remember { mutableStateOf<ExploreDestination?>(null) }
    BackHandler(enabled = subPage != null) { subPage = null }
    LaunchedEffect(subPage) {
        when (subPage) {
            ExploreDestination.CHARTS -> state.loadCharts(scope, state.chartRegion)
            ExploreDestination.NEW_RELEASES -> state.loadHomeSections(scope)
            else -> Unit
        }
    }
    val currentSubPage = subPage
    if (currentSubPage != null) {
        when (currentSubPage) {
            ExploreDestination.CHARTS -> ChartsPage(
                state = state,
                scope = scope,
                onBack = { subPage = null },
                onPlay = { state.player.playTrack(it) },
                onOpenArtist = onOpenArtist,
                onOpenSearch = { q ->
                    query = q
                    subPage = null
                    focusManager.clearFocus()
                },
                onMenu = { sheetTrack = it },
            )
            ExploreDestination.MOODS_AND_GENRES -> MoodsGenresPage(
                onBack = { subPage = null },
                onSelectCategory = { label ->
                    genreTarget = GenreTarget(label = label, query = label)
                    subPage = null
                    focusManager.clearFocus()
                },
            )
            ExploreDestination.NEW_RELEASES -> NewReleasesPage(
                state = state,
                scope = scope,
                onBack = { subPage = null },
                onPlay = { state.player.playTrack(it) },
                onPlayQueue = { tracks -> state.player.playQueue(tracks, 0) },
                onMenu = { sheetTrack = it },
            )
        }
        return
    }

    val searching = query.isNotBlank()
    val displayTracks: List<Pair<Track, Set<Source>>> = if (searching) {
        mergeTracks(state.searchResults)
    } else {
        browseTracks
    }
    // v1.10.21: multi-select hasil Cari/Browse.
    val searchSelection = rememberSelectionState()
    var searchBulkSave by remember { mutableStateOf<List<Track>?>(null) }
    val searchJamState by state.jam.uiState.collectAsState()
    BackHandler(enabled = searchSelection.modeActive && subPage == null) {
        searchSelection.exitMode()
    }
    val displayKeys = remember(displayTracks) {
        occurrenceKeys(displayTracks.map { it.first.id })
    }
    LaunchedEffect(displayTracks) {
        if (searchSelection.modeActive) searchSelection.exitMode()
    }

    val searchListState = rememberLazyListState()
    if (onChromeScroll != null) {
        ChromeScrollEffect(
            listState = searchListState,
            enabled = sheetTrack == null && subPage == null,
            onScroll = onChromeScroll,
        )
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .imePadding(),
        state = searchListState,
        // v1.10.7: ruang napas bawah pengaman (lihat HomeScreen).
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
    ) {
        // v1.10.21: bar aksi multi-select menempel di atas daftar.
        stickyHeader {
            if (searchSelection.modeActive) {
                SelectionActionBar(
                    count = searchSelection.count,
                    jamActive = searchJamState.active,
                    onAddToQueue = {
                        val selected = displayTracks.mapIndexedNotNull { i, (track, _) ->
                            track.takeIf {
                                searchSelection.isSelected(
                                    selectionKey("search", displayKeys[i]),
                                )
                            }
                        }
                        val added = state.player.addTracksToQueue(selected)
                        showToast(
                            searchContext,
                            if (added == 1) "Added 1 song to queue"
                            else "Added $added songs to queue",
                        )
                        searchSelection.exitMode()
                    },
                    onSaveToPlaylist = {
                        searchBulkSave = displayTracks.mapIndexedNotNull { i, (track, _) ->
                            track.takeIf {
                                searchSelection.isSelected(
                                    selectionKey("search", displayKeys[i]),
                                )
                            }
                        }
                    },
                    onExit = { searchSelection.exitMode() },
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = ContentMargin, vertical = 8.dp)) {
                // v1.10.1: tombol Back di kiri atas.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArusBackButton(onClick = onBack)
                }
                Spacer(Modifier.height(8.dp))
                ArusSearchField(
                    value = query,
                    onValueChange = { query = it },
                    onSearchSubmit = {
                        // Enter: catat (bukan link), sembunyikan
                        // saran, jalankan pencarian/link sekarang.
                        recordSearch(query)
                        suggestions = emptyList()
                        doSearch()
                    },
                )
            }
        }
        // Saran ketikan (v1.0.3): daftar di bawah kolom cari —
        // ketuk saran lagu = langsung putar (pola pending-link),
        // ketuk saran query = cari query itu. Sembunyi saat
        // submit/clear (query kosong / suggestions dikosongkan).
        if (query.isNotBlank() && suggestions.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentMargin),
                ) {
                    for (suggestion in suggestions) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(Corners.chip))
                                .clickable {
                                    if (suggestion.videoId != null) {
                                        state.app.pendingYtmVideoId.value =
                                            suggestion.videoId
                                        suggestions = emptyList()
                                    } else {
                                        val q = suggestion.query ?: suggestion.text
                                        query = q
                                        recordSearch(q)
                                        suggestions = emptyList()
                                        focusManager.clearFocus()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (suggestion.videoId != null) {
                                    Icons.Filled.PlayArrow
                                } else {
                                    Icons.Filled.Search
                                },
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                suggestion.text,
                                fontSize = 15.sp,
                                color = InkBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        // Riwayat pencarian (v1.10.16): hanya saat query kosong
        // (mode Browse) dan ada yang tercatat — chip tap = cari lagi.
        if (query.isBlank() && recentSearches.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = ContentMargin, end = ContentMargin, top = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Recent searches",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            state.store.clearRecentSearches()
                            recentVersion++
                        }) {
                            Text(
                                "Clear all",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BurntOrange,
                            )
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        items(recentSearches, key = { it }) { recent ->
                            Box(
                                Modifier
                                    .hardShadowDraw(
                                        Corners.chip,
                                        CurrentTokens.hardShadowChip,
                                    )
                                    .clip(RoundedCornerShape(Corners.chip))
                                    .border(
                                        CurrentTokens.cardBorderWidth,
                                        PaperOutline,
                                        RoundedCornerShape(Corners.chip),
                                    )
                                    .background(PaperSurface)
                                    .clickable {
                                        query = recent
                                        focusManager.clearFocus()
                                    }
                                    .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        recent,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkBlack,
                                        maxLines = 1,
                                    )
                                    IconButton(
                                        onClick = {
                                            state.store.removeRecentSearch(recent)
                                            recentVersion++
                                        },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove \"$recent\"",
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
        // Baris tile Explore (v1.10.18) — hanya saat tidak mencari.
        if (!searching) {
            item {
                ExploreTilesRow(onSelect = { subPage = it })
            }
        }
        // Seksi Browse (header + grid genre, v1.10.33) — v1.10.38:
        // disembunyikan seluruhnya saat hasil pencarian tampil;
        // kembali saat query dikosongkan. Tidak ada celah tersisa.
        if (!searching) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = ContentMargin,
                            end = ContentMargin,
                            top = 24.dp,
                            bottom = 12.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        displayCased("Browse", CurrentTokens),
                        fontSize = 26.sp,
                        fontFamily = CurrentTokens.displayFont,
                        letterSpacing = CurrentTokens.displayLetterSpacing,
                        fontWeight = FontWeight.Black,
                        color = InkBlack,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(Corners.button))
                            .clickable { genresExpanded = !genresExpanded }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (genresExpanded) "Show less" else "View all",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BurntOrange,
                        )
                    }
                }
            }
            item {
                // v1.10.33: ciut = 4 genre pertama (2×2, tile 1:1);
                // View all = semua kategori. Layout tile tidak berubah.
                val shownGenres = if (genresExpanded) BROWSE_CATEGORIES
                    else BROWSE_CATEGORIES.take(4)
                Column(Modifier.padding(horizontal = ContentMargin)) {
                    for (row in shownGenres.chunked(2)) {
                        Row(Modifier.fillMaxWidth()) {
                            for (genre in row) {
                                GenreArtworkTile(
                                    genre = genre,
                                    artworkUrl = state.genreArtworks[genre.name],
                                    onClick = {
                                        genreTarget = GenreTarget(
                                            label = genre.name,
                                            query = genre.query,
                                        )
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                if (genre != row.last()) Spacer(Modifier.width(12.dp))
                            }
                            if (row.size < 2) {
                                Spacer(Modifier.width(12.dp))
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
        // Seksi Popular now + Trending (v1.10.6; v1.10.25: feed
        // Indonesia yang sama dengan Beranda, gl=ID/hl=id) —
        // hanya tampil saat tidak sedang mencari.
        if (!searching) {
            item {
                BrowseSongRail(
                    title = "Popular now",
                    tracks = state.homeTop,
                    homeLoaded = state.homeLoaded,
                    onPlay = { state.player.playTrack(it) },
                    onRetry = { state.loadHome(scope) },
                )
            }
            item {
                BrowseSongRail(
                    title = "Trending",
                    tracks = state.homeTrending,
                    homeLoaded = state.homeLoaded,
                    onPlay = { state.player.playTrack(it) },
                    onRetry = { state.loadHome(scope) },
                )
            }
        }
        // Urutan hasil (v1.0.3): hanya saat sedang mencari — ganti
        // urutan menjalankan ulang pencarian yang sama (lihat
        // LaunchedEffect di atas). Best match = urutan server.
        if (searching) {
            item {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = ContentMargin,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(SearchSort.entries, key = { it.name }) { sort ->
                        val isSelected = sort == searchSort
                        Box(
                            Modifier
                                .height(40.dp)
                                .hardShadowDraw(
                                    Corners.chip,
                                    CurrentTokens.hardShadowChip,
                                )
                                .clip(RoundedCornerShape(Corners.chip))
                                .background(if (isSelected) ChromeBg else PaperSurface)
                                .border(
                                    CurrentTokens.cardBorderWidth,
                                    PaperOutline,
                                    RoundedCornerShape(Corners.chip),
                                )
                                .clickable { searchSort = sort }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                when (sort) {
                                    SearchSort.BEST_MATCH -> "Best match"
                                    SearchSort.MOST_PLAYED -> "Most played"
                                    SearchSort.NEWEST -> "Newest"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) OnChrome else InkBlack,
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = if (searching) "Search results" else "All songs",
                rightText = if (searching) {
                    "${displayTracks.size} RESULTS"
                } else {
                    "TAP TO PLAY"
                },
            )
        }
        if (state.searchBusy && searching && state.searchResults.isEmpty()) {
            item { SkeletonRows(count = 6, modifier = Modifier.fillMaxWidth()) }
        }
        state.searchError?.let { err ->
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentMargin, vertical = Spacing.md)
                        .clip(RoundedCornerShape(Corners.medium))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .padding(Spacing.lg),
                ) {
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(text = "Try again", onClick = { doSearch() })
                }
            }
        }
        itemsIndexed(displayTracks, key = { i, (t, _) -> t.id + "#" + i }) { index, (track, sources) ->
            val searchKey = selectionKey("search", displayKeys[index])
            // v1.0.5: baris acara BUKAN lagu — ketuk membuka halaman
            // acara, tanpa masuk antrean/seleksi/sheet lagu.
            val isShowRow = track.id.startsWith("show:") &&
                track.podcastShowBrowseId != null
            TrackRow(
                chartNumber = if (isShowRow) null else index + 1,
                track = track,
                durations = state.durations,
                sources = sources,
                // v1.0.3: hitungan hanya di hasil Cari (data nyata
                // dari respons — lihat TrackRow.showPlayCount).
                showPlayCount = searching && !isShowRow,
                onClick = {
                    if (isShowRow) {
                        if (searching) recordSearch(query)
                        onOpenPodcastShow?.invoke(
                            track.podcastShowBrowseId!!,
                            track.podcastShowTitle ?: track.title,
                        )
                    } else if (searchSelection.modeActive) searchSelection.toggle(searchKey)
                    else {
                        // Ketuk hasil = pencarian itu dipakai: catat.
                        if (searching) recordSearch(query)
                        state.player.playTrack(track)
                    }
                },
                onLongClick = {
                    if (!isShowRow) {
                        if (searchSelection.modeActive) searchSelection.toggle(searchKey)
                        else searchSelection.enter(searchKey)
                    }
                },
                isCurrent = currentTrack?.id == track.id,
                isPlaying = isPlaying,
                selected = searchSelection.isSelected(searchKey),
                animateIn = index < 8,
                index = index,
                trailing = if (isShowRow) null else {
                    {
                        IconButton(onClick = { sheetTrack = track }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Song menu",
                            )
                        }
                    }
                },
            )
        }
        if (!state.searchBusy && displayTracks.isEmpty() && state.searchError == null) {
            item {
                if (searching) {
                    EmptyState(
                        title = "No results",
                        subtitle = "Try a different title or artist.",
                    )
                } else if (state.homeLoaded) {
                    EmptyState(
                        title = "No songs here yet",
                        subtitle = "Type a song title or artist name — results appear automatically.",
                        actionText = "Try again",
                        onAction = { state.loadHome(scope) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }

    sheetTrack?.let { track ->
        TrackActionSheet(
            track = track,
            state = state,
            onDismiss = { sheetTrack = null },
            onOpenSearch = { q -> query = q },
            onOpenArtist = onOpenArtist,
        )
    }

    // v1.10.21: sheet simpan banyak lagu — menutup sheet keluar mode.
    searchBulkSave?.let { bulkTracks ->
        SaveTracksToPlaylistSheet(
            tracks = bulkTracks,
            state = state,
            onDismiss = {
                searchBulkSave = null
                searchSelection.exitMode()
            },
        )
    }
}

// ---- Sub-halaman Explore (v1.10.18) ---------------------------------------

/** Baris 3 tile Explore di Browse (ikon digambar dari warna + panah, tanpa aset). */
@Composable
private fun ExploreTilesRow(onSelect: (ExploreDestination) -> Unit) {
    Column(
        Modifier.padding(
            start = ContentMargin,
            end = ContentMargin,
            top = 8.dp,
        ),
    ) {
        Text(
            displayCased("Explore", CurrentTokens),
            fontSize = 26.sp,
            fontFamily = CurrentTokens.displayFont,
            letterSpacing = CurrentTokens.displayLetterSpacing,
            fontWeight = FontWeight.Black,
            color = InkBlack,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            for ((i, tile) in EXPLORE_TILES.withIndex()) {
                val (bg, fg) = when (tile.destination) {
                    ExploreDestination.NEW_RELEASES -> BurntOrangeSoft to InkFixed
                    ExploreDestination.CHARTS -> InkFixed to Color.White
                    ExploreDestination.MOODS_AND_GENRES -> AvatarGold to InkFixed
                }
                Column(
                    Modifier
                        .weight(1f)
                        .height(104.dp)
                        .clip(RoundedCornerShape(Corners.tile))
                        .background(bg)
                        .clickable { onSelect(tile.destination) }
                        .padding(Density.cardPadding),
                ) {
                    Text(
                        displayCased(tile.title, CurrentTokens),
                        fontSize = 16.sp,
                        fontFamily = CurrentTokens.displayFont,
                        letterSpacing = CurrentTokens.displayLetterSpacing,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                        maxLines = 2,
                    )
                    Text(
                        tile.subtitle,
                        fontSize = 12.sp,
                        color = fg.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (i < EXPLORE_TILES.size - 1) Spacer(Modifier.width(12.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Satu baris berperingkat Charts (rank + artwork + judul + artis). */
@Composable
private fun RankedTrackRow(
    ranked: RankedTrack,
    onPlay: (Track) -> Unit,
    onMenu: (Track) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onPlay(ranked.track) }
            .padding(horizontal = ContentMargin, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${ranked.rank}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = InkMuted,
            modifier = Modifier.width(28.dp),
        )
        CoverArt(track = ranked.track, size = 48.dp, corner = Corners.medium)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                ranked.track.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                displayArtist(ranked.track.artist),
                fontSize = 13.sp,
                color = InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { onMenu(ranked.track) }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Song menu",
            )
        }
    }
}

/** Satu baris artis berperingkat (ketuk → profil artis). */
@Composable
private fun RankedArtistRow(
    ranked: RankedArtist,
    onOpen: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(ranked.artist) }
            .padding(horizontal = ContentMargin, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${ranked.rank}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = InkMuted,
            modifier = Modifier.width(28.dp),
        )
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AvatarGold),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                ranked.artist.firstOrNull()?.toString() ?: "?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = OnAvatar,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                ranked.artist,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                ranked.topTrack.title,
                fontSize = 13.sp,
                color = InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = InkMuted,
        )
    }
}

/**
 * Sub-halaman Charts: chip region Indonesia/Global (jalur Kotlin yang
 * dipaksa supaya gl/hl benar-benar mengubah hasil) + seksi Trending
 * 20 / Daily Top / Top artists. PROVENANCE JUJUR tampil di UI:
 * tidak ada endpoint chart resmi — dibangun dari pencarian
 * trending/top. "Daily Top" tanpa angka 100: satu halaman search
 * hanya mengembalikan ~20 hasil, mengklaim 100 baris = karangan.
 */
@Composable
private fun ChartsPage(
    state: SonaraState,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
    onPlay: (Track) -> Unit,
    onOpenArtist: ((String, String?) -> Unit)?,
    onOpenSearch: (String) -> Unit,
    onMenu: (Track) -> Unit,
) {
    val region = state.chartRegion
    val data = state.chartData
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentMargin, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArusBackButton(onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Text(
                displayCased("Charts", CurrentTokens),
                fontSize = 26.sp,
                fontFamily = CurrentTokens.displayFont,
                letterSpacing = CurrentTokens.displayLetterSpacing,
                fontWeight = FontWeight.Black,
                color = InkBlack,
            )
        }
        Text(
            CHARTS_PROVENANCE,
            fontSize = 13.sp,
            color = InkMuted,
            modifier = Modifier.padding(horizontal = ContentMargin, vertical = 4.dp),
        )
        Row(
            Modifier.padding(horizontal = ContentMargin, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (r in ChartRegion.entries) {
                val isSelected = r == region
                Box(
                    Modifier
                        .height(40.dp)
                        .hardShadowDraw(Corners.chip, CurrentTokens.hardShadowChip)
                        .clip(RoundedCornerShape(Corners.chip))
                        .background(if (isSelected) ChromeBg else PaperSurface)
                        .border(
                            CurrentTokens.cardBorderWidth,
                            PaperOutline,
                            RoundedCornerShape(Corners.chip),
                        )
                        .clickable { state.loadCharts(scope, r) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (r == ChartRegion.INDONESIA) "Indonesia" else "Global",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) OnChrome else InkBlack,
                    )
                }
            }
        }
        when {
            state.chartLoading && data == null -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = InkBlack)
                }
            }
            state.chartFailed && data == null -> {
                EmptyState(
                    title = "Charts couldn't load",
                    subtitle = "Check your internet connection, then try again.",
                    actionText = "Try again",
                    onAction = { state.loadCharts(scope, region, force = true) },
                )
            }
            data != null -> {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
                ) {
                    if (data.trending.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Trending 20", rightText = "TAP TO PLAY")
                        }
                        items(data.trending, key = { "trending#" + it.track.id + "#" + it.rank }) { ranked ->
                            RankedTrackRow(ranked = ranked, onPlay = onPlay, onMenu = onMenu)
                        }
                    }
                    if (data.dailyTop.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Daily Top", rightText = "TAP TO PLAY")
                        }
                        items(data.dailyTop, key = { "daily#" + it.track.id + "#" + it.rank }) { ranked ->
                            RankedTrackRow(ranked = ranked, onPlay = onPlay, onMenu = onMenu)
                        }
                    }
                    if (data.topArtists.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Top artists", rightText = "TAP TO OPEN")
                        }
                        items(data.topArtists, key = { "artist#" + it.artist }) { ranked ->
                            RankedArtistRow(ranked = ranked) { artist ->
                                if (onOpenArtist != null) {
                                    onOpenArtist(artist, null)
                                } else {
                                    onOpenSearch(artist)
                                }
                            }
                        }
                    }
                    if (data.trending.isEmpty() && data.dailyTop.isEmpty() &&
                        data.topArtists.isEmpty()
                    ) {
                        item {
                            EmptyState(
                                title = "No chart data yet",
                                subtitle = "Try the other region.",
                            )
                        }
                    }
                    item { Spacer(Modifier.height(Spacing.xl)) }
                }
            }
        }
    }
}

/** Sub-halaman Moods & genres: grid 2 kolom berkelompok (label = query). */
@Composable
private fun MoodsGenresPage(
    onBack: () -> Unit,
    onSelectCategory: (String) -> Unit,
) {
    val palette = listOf(
        GenrePop, GenreIndie, GenreRock, GenreJazz,
        GenreDangdut, GenreKPop, GenreMetal, GenreElectronic,
    )
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ContentMargin, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArusBackButton(onClick = onBack)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Moods & genres",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = InkBlack,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Pick a mood or genre to see what's inside.",
            fontSize = 17.sp,
            lineHeight = 24.sp,
            color = InkMuted,
        )
        Spacer(Modifier.height(20.dp))
        var colorIndex = 0
        for (group in MOOD_GROUPS) {
            Text(
                group.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = InkBlack,
            )
            Spacer(Modifier.height(12.dp))
            for (row in group.tiles.chunked(2)) {
                Row(Modifier.fillMaxWidth()) {
                    for (tile in row) {
                        val color = palette[colorIndex % palette.size]
                        colorIndex++
                        Box(
                            Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(Corners.tile))
                                .background(color)
                                .clickable { onSelectCategory(tile) },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            // v1.10.39: scrim yang SAMA dengan tile
                            // artwork (10.34) — label putih di atas
                            // warna terang (Jazz, Electronic) tak
                            // terbaca tanpa ini (harness139 §A2).
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
                            Text(
                                displayCased(tile, CurrentTokens),
                                fontSize = 16.sp,
                                fontFamily = CurrentTokens.displayFont,
                                letterSpacing = CurrentTokens.displayLetterSpacing,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 14.dp),
                            )
                        }
                        if (tile != row.last()) Spacer(Modifier.width(12.dp))
                    }
                    if (row.size < 2) {
                        Spacer(Modifier.width(12.dp))
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Halaman genre khusus (v1.10.33): seperti halaman genre YouTube
 * Music — header + seksi Songs, Featured playlists, Community
 * playlists, Music videos, Albums dalam satu guliran. Semua data
 * REAL (browse kategori GenreApi, atau komposisi fallback search
 * yang hanya menampilkan seksi yang datanya ada). Ketuk playlist/
 * album = putar isi koleksinya; ketuk video = putar audionya.
 * Back kembali ke Browse (ditangani SearchScreen).
 */
@Composable
private fun GenrePage(
    state: SonaraState,
    scope: kotlinx.coroutines.CoroutineScope,
    target: GenreTarget,
    artworkUrl: String?,
    onBack: () -> Unit,
    onPlay: (Track) -> Unit,
    onPlayQueue: (List<Track>) -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenArtist: ((String, String?) -> Unit)?,
) {
    val context = LocalContext.current
    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    var collectionLoading by remember { mutableStateOf<String?>(null) }
    val currentTrack by state.player.currentTrack.collectAsState()
    val isPlaying by state.player.isPlaying.collectAsState()

    LaunchedEffect(target) {
        state.loadGenrePage(scope, target.label, target.query)
    }
    val data = state.genrePage.takeIf { state.genrePageLabel == target.label }

    fun playCollection(card: GenreApi.GenreCardItem) {
        val browseId = card.browseId ?: return
        if (collectionLoading != null) return
        collectionLoading = browseId
        scope.launch {
            val tracks = withContext(Dispatchers.IO) { state.fetchCollectionTracks(browseId) }
            collectionLoading = null
            if (tracks.isEmpty()) {
                showToast(
                    context,
                    "Couldn't load \"${card.title}\". Check your internet, then try again.",
                )
            } else {
                onPlayQueue(tracks)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentMargin, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArusBackButton(onClick = onBack)
        }
        when {
            state.genrePageLoading && data == null -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = InkBlack)
                }
            }
            state.genrePageFailed && data == null -> {
                EmptyState(
                    title = "This genre couldn't load",
                    subtitle = "Check your internet connection, then try again.",
                    actionText = "Try again",
                    onAction = {
                        state.loadGenrePage(scope, target.label, target.query, force = true)
                    },
                )
            }
            data != null -> {
                val songs = data.songs.map { GenreApi.genreSongToTrack(it) }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
                ) {
                    // Header: artwork genre + scrim + judul (pola tile),
                    // atau judul teks bila tidak ada artwork.
                    item {
                        if (!artworkUrl.isNullOrBlank()) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(176.dp)
                                    .clip(RoundedCornerShape(Corners.tile)),
                            ) {
                                AsyncImage(
                                    model = artworkUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
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
                                Text(
                                    displayCased(data.title, CurrentTokens),
                                    fontSize = 28.sp,
                                    fontFamily = CurrentTokens.displayFont,
                                    letterSpacing = CurrentTokens.displayLetterSpacing,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(Density.cardPadding),
                                )
                            }
                        } else {
                            Text(
                                displayCased(data.title, CurrentTokens),
                                fontSize = 26.sp,
                                fontFamily = CurrentTokens.displayFont,
                                letterSpacing = CurrentTokens.displayLetterSpacing,
                                fontWeight = FontWeight.Black,
                                color = InkBlack,
                                modifier = Modifier.padding(
                                    horizontal = ContentMargin,
                                    vertical = 4.dp,
                                ),
                            )
                        }
                    }
                    item {
                        Text(
                            if (data.fromBrowse) "From YouTube Music."
                            else "From YouTube Music search.",
                            fontSize = 13.sp,
                            color = InkMuted,
                            modifier = Modifier.padding(
                                horizontal = ContentMargin,
                                vertical = 4.dp,
                            ),
                        )
                    }
                    if (songs.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Songs", rightText = "TAP TO PLAY")
                        }
                        itemsIndexed(songs, key = { i, t -> "genresong#" + t.id + "#" + i }) { index, track ->
                            TrackRow(
                                chartNumber = index + 1,
                                track = track,
                                durations = state.durations,
                                onClick = { onPlay(track) },
                                isCurrent = currentTrack?.id == track.id,
                                isPlaying = isPlaying,
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
                    if (data.featured.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Featured playlists", rightText = "TAP TO PLAY")
                        }
                        item {
                            GenreCardRail(
                                cards = data.featured,
                                kind = "featured",
                                loadingId = collectionLoading,
                                onCardClick = { playCollection(it) },
                            )
                        }
                    }
                    if (data.community.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Community playlists", rightText = "TAP TO PLAY")
                        }
                        item {
                            GenreCardRail(
                                cards = data.community,
                                kind = "community",
                                loadingId = collectionLoading,
                                onCardClick = { playCollection(it) },
                            )
                        }
                    }
                    if (data.videos.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Music videos", rightText = "TAP TO PLAY")
                        }
                        item {
                            GenreCardRail(
                                cards = data.videos,
                                kind = "videos",
                                loadingId = null,
                                onCardClick = { card ->
                                    GenreApi.cardVideoToTrack(card)?.let { onPlay(it) }
                                },
                            )
                        }
                    }
                    if (data.albums.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Albums", rightText = "TAP TO PLAY")
                        }
                        item {
                            GenreCardRail(
                                cards = data.albums,
                                kind = "albums",
                                loadingId = collectionLoading,
                                onCardClick = { playCollection(it) },
                            )
                        }
                    }
                    if (songs.isEmpty() && data.featured.isEmpty() &&
                        data.community.isEmpty() && data.videos.isEmpty() &&
                        data.albums.isEmpty()
                    ) {
                        item {
                            EmptyState(
                                title = "Nothing here yet",
                                subtitle = "Try again in a moment.",
                                actionText = "Try again",
                                onAction = {
                                    state.loadGenrePage(
                                        scope, target.label, target.query, force = true,
                                    )
                                },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(Spacing.xl)) }
                }
            }
        }
    }

    sheetTrack?.let { track ->
        TrackActionSheet(
            track = track,
            state = state,
            onDismiss = { sheetTrack = null },
            onOpenSearch = onOpenSearch,
            onOpenArtist = onOpenArtist,
        )
    }
}

/** Rel horizontal kartu twoRow (playlist/video/album) halaman genre. */
@Composable
private fun GenreCardRail(
    cards: List<GenreApi.GenreCardItem>,
    kind: String,
    loadingId: String?,
    onCardClick: (GenreApi.GenreCardItem) -> Unit,
) {
    LazyRow(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ContentMargin),
        horizontalArrangement = Arrangement.spacedBy(Density.gridGap),
    ) {
        itemsIndexed(cards, key = { i, c -> "genre-$kind#" + c.title + "#" + i }) { _, card ->
            GenreCard(
                card = card,
                loading = loadingId != null && card.browseId == loadingId,
                onClick = { onCardClick(card) },
            )
        }
    }
}

/** Kartu twoRow: artwork 1:1 + judul + subtitle (pola rel poster). */
@Composable
private fun GenreCard(
    card: GenreApi.GenreCardItem,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Corners.large)),
        ) {
            if (!card.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = card.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(PosterFallback),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            card.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (loading) "Loading…" else displayArtist(card.subtitle),
            fontSize = 13.sp,
            color = InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Sub-halaman New releases: kartu "New Release Mix" + poster row
 * "Albums & singles" (TANPA label tipe — API tidak mengembalikan
 * tipe) + baris "Music videos" yang memutar AUDIO normal (mode
 * Video dihapus di v1.10.18).
 */
@Composable
private fun NewReleasesPage(
    state: SonaraState,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
    onPlay: (Track) -> Unit,
    onPlayQueue: (List<Track>) -> Unit,
    onMenu: (Track) -> Unit,
) {
    val releases = state.sectionNewReleases
    // v1.10.21: multi-select daftar "Music videos" di halaman ini.
    val releasesContext = LocalContext.current
    val releasesSelection = rememberSelectionState()
    var releasesBulkSave by remember { mutableStateOf<List<Track>?>(null) }
    val releasesJamState by state.jam.uiState.collectAsState()
    val releasesShown = releases.take(6)
    val releasesKeys = remember(releasesShown) {
        occurrenceKeys(releasesShown.map { it.id })
    }
    BackHandler(enabled = releasesSelection.modeActive) { releasesSelection.exitMode() }
    LaunchedEffect(releasesShown) {
        if (releasesSelection.modeActive) releasesSelection.exitMode()
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentMargin, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArusBackButton(onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Text(
                "New releases",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = InkBlack,
            )
        }
        if (releasesSelection.modeActive) {
            SelectionActionBar(
                count = releasesSelection.count,
                jamActive = releasesJamState.active,
                onAddToQueue = {
                    val selected = releasesShown.mapIndexedNotNull { i, track ->
                        track.takeIf {
                            releasesSelection.isSelected(
                                selectionKey("releases", releasesKeys[i]),
                            )
                        }
                    }
                    val added = state.player.addTracksToQueue(selected)
                    showToast(
                        releasesContext,
                        if (added == 1) "Added 1 song to queue"
                        else "Added $added songs to queue",
                    )
                    releasesSelection.exitMode()
                },
                onSaveToPlaylist = {
                    releasesBulkSave = releasesShown.mapIndexedNotNull { i, track ->
                        track.takeIf {
                            releasesSelection.isSelected(
                                selectionKey("releases", releasesKeys[i]),
                            )
                        }
                    }
                },
                onExit = { releasesSelection.exitMode() },
            )
        }
        if (releases.isEmpty()) {
            if (state.sectionsLoaded) {
                EmptyState(
                    title = "New releases couldn't load",
                    subtitle = "Check your internet connection, then try again.",
                    actionText = "Try again",
                    onAction = { state.loadHomeSections(scope, force = true) },
                )
            } else {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = InkBlack)
                }
            }
            return
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
        ) {
            item {
                Box(Modifier.padding(horizontal = ContentMargin, vertical = 8.dp)) {
                    MixCard(
                        title = "New Release Mix",
                        tracks = releases,
                        onClick = { onPlayQueue(releases) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                HomePosterRail(
                    kicker = null,
                    title = "Albums & singles",
                    tracks = posterTracks(releases),
                    loading = false,
                    onPlay = onPlay,
                )
            }
            item {
                SectionHeader(title = "Music videos", rightText = "TAP TO PLAY")
            }
            itemsIndexed(releasesShown, key = { i, t -> t.id + "#" + i }) { index, track ->
                val releaseKey = selectionKey("releases", releasesKeys[index])
                TrackRow(
                    chartNumber = index + 1,
                    track = track,
                    durations = state.durations,
                    onClick = {
                        if (releasesSelection.modeActive) releasesSelection.toggle(releaseKey)
                        else onPlay(track)
                    },
                    onLongClick = {
                        if (releasesSelection.modeActive) releasesSelection.toggle(releaseKey)
                        else releasesSelection.enter(releaseKey)
                    },
                    isCurrent = false,
                    isPlaying = false,
                    selected = releasesSelection.isSelected(releaseKey),
                    animateIn = index < 6,
                    index = index,
                    trailing = {
                        IconButton(onClick = { onMenu(track) }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Song menu",
                            )
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(Spacing.xl)) }
        }
        releasesBulkSave?.let { bulkTracks ->
            SaveTracksToPlaylistSheet(
                tracks = bulkTracks,
                state = state,
                onDismiss = {
                    releasesBulkSave = null
                    releasesSelection.exitMode()
                },
            )
        }
    }
}
