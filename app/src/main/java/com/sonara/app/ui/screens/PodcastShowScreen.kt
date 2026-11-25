package com.sonara.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.data.FollowedShow
import com.sonara.app.data.PodcastApi
import com.sonara.app.data.PodcastShow
import com.sonara.app.data.Track
import com.sonara.app.data.formatDurationMs
import com.sonara.app.ui.ArusBackButton
import com.sonara.app.ui.ContentMargin
import com.sonara.app.ui.CoverArt
import com.sonara.app.ui.EmptyState
import com.sonara.app.ui.SecondaryButton
import com.sonara.app.ui.SectionHeader
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.TrackRow
import com.sonara.app.ui.showToast
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.theme.displayCased
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Target halaman acara podcast: browseId MPSP… + judul fallback dari hasil search. */
data class PodcastTarget(val browseId: String, val title: String)

/**
 * Halaman acara podcast (v1.0.5) — masuk back-stack yang sudah ada.
 *
 * Semua konten dari API InnerTube (/browse WEB_REMIX, signed-out):
 * header (judul + host + deskripsi + artwork), daftar episode
 * (judul + info tonton/tanggal + durasi + deskripsi), paginasi
 * "Load more" dari continuation token, tombol Follow (lokal saja),
 * dan tandai played per episode (lokal saja). Episode yang sedang
 * diputar memakai posisi-lanjut yang sama dengan konten panjang lain.
 * Bila browse gagal, empty state jujur + Try again — bukan halaman palsu.
 */
@Composable
fun PodcastShowScreen(
    state: SonaraState,
    target: PodcastTarget?,
    onBack: () -> Unit,
    onOpenDetail: (Track) -> Unit,
    onOpenShow: (PodcastTarget) -> Unit,
) {
    val context = LocalContext.current
    var show by remember(target) { mutableStateOf<PodcastShow?>(null) }
    var failed by remember(target) { mutableStateOf(false) }
    var reloadTick by remember(target) { mutableStateOf(0) }
    var loadingMore by remember(target) { mutableStateOf(false) }
    var moreFailed by remember(target) { mutableStateOf(false) }
    var descriptionExpanded by remember(target) { mutableStateOf(false) }
    // Dibaca ulang tiap toggle played/follow (store tidak reaktif).
    var playedTick by remember(target) { mutableStateOf(0) }
    val currentTrack by state.player.currentTrack.collectAsState()
    val isPlaying by state.player.isPlaying.collectAsState()
    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler { onBack() }

    LaunchedEffect(target, reloadTick) {
        show = null
        failed = false
        val bid = target?.browseId?.trim().orEmpty()
        if (bid.isEmpty()) {
            failed = true
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val root = state.ytmApi.browse(bid).getOrThrow()
                PodcastApi.parseShowPage(root, bid)
            }.getOrNull()
        }
        if (loaded != null) show = loaded else failed = true
    }

    fun loadMore() {
        val token = show?.continuationToken ?: return
        if (loadingMore) return
        loadingMore = true
        moreFailed = false
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val root = state.ytmApi.browseContinuation(token).getOrThrow()
                    PodcastApi.parseShowContinuation(root)
                }.getOrNull()
            }
            loadingMore = false
            if (res == null) {
                moreFailed = true
                return@launch
            }
            val (more, next) = res
            val current = show ?: return@launch
            val seen = current.episodes.map { it.videoId }.toSet()
            show = current.copy(
                episodes = current.episodes + more.filter { it.videoId !in seen },
                continuationToken = next,
            )
        }
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
                displayCased("Podcast", CurrentTokens),
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
                    title = "Couldn't load this show",
                    subtitle = "Check your internet connection, then try again.",
                    actionText = "Try again",
                    onAction = { reloadTick++ },
                )
            }
            show == null -> {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = InkBlack)
                }
            }
            else -> {
                val s = show!!
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.padding(horizontal = ContentMargin)) {
                            Row(verticalAlignment = Alignment.Top) {
                                CoverArt(
                                    track = Track(
                                        id = "show:${s.browseId}",
                                        title = s.title,
                                        artist = s.host,
                                        source = com.sonara.app.data.Source.YOUTUBE_MUSIC,
                                        engine = com.sonara.app.data.Engine.YOUTUBE,
                                        imageUrl = s.imageUrl,
                                    ),
                                    size = 96.dp,
                                    corner = Corners.card,
                                    letterSize = 28.sp,
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        s.title,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = InkBlack,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (s.host.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            s.host,
                                            fontSize = 14.sp,
                                            color = InkMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    val isFollowing = state.podcastFollowVersion.let {
                                        state.store.isFollowingShow(s.browseId)
                                    }
                                    SecondaryButton(
                                        text = if (isFollowing) "Following" else "Follow",
                                        onClick = {
                                            state.setShowFollowed(
                                                FollowedShow(
                                                    key = "show:${s.browseId}",
                                                    title = s.title,
                                                    browseId = s.browseId,
                                                    imageUrl = s.imageUrl,
                                                    host = s.host,
                                                    followedAtMs = System.currentTimeMillis(),
                                                ),
                                                !isFollowing,
                                            )
                                            showToast(
                                                context,
                                                if (isFollowing) "Unfollowed ${s.title}"
                                                else "Followed ${s.title}",
                                            )
                                        },
                                    )
                                }
                            }
                            if (s.description.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    s.description,
                                    fontSize = 14.sp,
                                    color = InkMuted,
                                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                TextButton(onClick = { descriptionExpanded = !descriptionExpanded }) {
                                    Text(
                                        if (descriptionExpanded) "Show less" else "Show more",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkBlack,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SectionHeader(
                            title = "Episodes",
                            rightText = "${s.episodes.size} SHOWN",
                        )
                    }
                    if (s.episodes.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No episodes found",
                                subtitle = "This show has no episodes listed right now.",
                            )
                        }
                    }
                    itemsIndexed(
                        s.episodes,
                        key = { _, ep -> "showep:${ep.videoId}" },
                    ) { _, ep ->
                        val track = remember(ep, s) {
                            PodcastApi.episodeToTrack(ep, s.browseId, s.title)
                        }
                        val played = playedTick.let {
                            state.store.isEpisodePlayed(track.id)
                        }
                        val savedMs = playedTick.let {
                            state.store.resumePosition(track.id)
                        }
                        TrackRow(
                            track = track,
                            durations = state.durations,
                            onClick = {
                                state.player.playTrack(track)
                                onOpenDetail(track)
                            },
                            onMenu = { sheetTrack = track },
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            trailing = {
                                Column(horizontalAlignment = Alignment.End) {
                                    if (played) {
                                        Text(
                                            "Played",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = InkMuted,
                                        )
                                    } else if (savedMs > 0 && track.durationMs > 0) {
                                        Text(
                                            "${formatDurationMs(track.durationMs - savedMs)} left",
                                            fontSize = 12.sp,
                                            color = InkMuted,
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            state.store.setPlayedEpisode(track.id, !played)
                                            playedTick++
                                            showToast(
                                                context,
                                                if (!played) "Marked as played"
                                                else "Marked as not played",
                                            )
                                        },
                                    ) {
                                        Text(
                                            if (played) "Unmark" else "Mark played",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = InkBlack,
                                        )
                                    }
                                }
                            },
                        )
                    }
                    if (s.continuationToken != null) {
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (loadingMore) {
                                    CircularProgressIndicator(color = InkBlack)
                                } else {
                                    SecondaryButton(
                                        text = "Load more episodes",
                                        onClick = { loadMore() },
                                    )
                                    if (moreFailed) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Couldn't load more — check your internet, then try again.",
                                            fontSize = 12.sp,
                                            color = InkMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (sheetTrack != null) {
        TrackActionSheet(
            track = sheetTrack!!,
            state = state,
            onDismiss = { sheetTrack = null },
            onOpenSearch = {},
            onOpenArtist = null,
        )
    }
}
