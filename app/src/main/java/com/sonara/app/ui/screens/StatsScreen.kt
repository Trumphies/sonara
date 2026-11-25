package com.sonara.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.data.PlayEvents
import com.sonara.app.data.StatsCard
import com.sonara.app.data.mostPlayedArtists
import com.sonara.app.data.mostPlayedTracks
import com.sonara.app.ui.ArusBackButton
import com.sonara.app.ui.ContentMargin
import com.sonara.app.ui.PrimaryButton
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.showToast
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.theme.PaperOutline
import com.sonara.app.ui.theme.PaperSurface
import com.sonara.app.ui.theme.Type
import com.sonara.app.ui.theme.displayCased
import kotlinx.coroutines.launch

/**
 * Halaman statistik (v1.0.6): angka dari data lokal NYATA saja —
 * hitungan putar (sejak v1.10.16), peristiwa putar bertimestamp
 * (sejak v1.0.6), lagu disukai. Tanpa data = empty state jujur,
 * bukan angka karangan. "Your Year" hanya memakai peristiwa
 * tercatat tahun berjalan; putar sebelum v1.0.6 tidak bisa
 * dikaitkan ke tahun mana pun dan tidak diklaim.
 */
@Composable
fun StatsScreen(
    state: SonaraState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playCounts = remember { state.store.playCounts() }
    val likedCount = remember { state.store.likedTracks().size }
    val events = remember { state.store.playEvents() }
    val year = PlayEvents.yearOf(System.currentTimeMillis())
    val yearStats = remember(events, year) { PlayEvents.computeYearStats(events, year) }
    val totalPlays = remember(playCounts) { playCounts.sumOf { it.count } }
    val topTracks = remember(playCounts) { mostPlayedTracks(playCounts, 5) }
    val topArtists = remember(playCounts) { mostPlayedArtists(playCounts, 5) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ContentMargin, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArusBackButton(onClick = onBack)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Your stats",
                    fontSize = Type.screenTitle,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = InkBlack,
                    modifier = Modifier.weight(1f),
                )
                if (yearStats.hasData) {
                    IconButton(onClick = {
                        scope.launch {
                            val ok = StatsCard.share(context, yearStats, year)
                            if (!ok) showToast(context, "Couldn't share the year card.")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share year card",
                            tint = InkBlack,
                        )
                    }
                }
            }
        }
        // ---- Your Year ----
        item {
            Text(
                displayCased("Your $year", CurrentTokens),
                fontSize = 20.sp,
                fontFamily = CurrentTokens.displayFont,
                letterSpacing = CurrentTokens.displayLetterSpacing,
                fontWeight = FontWeight.Black,
                color = InkBlack,
                modifier = Modifier.padding(
                    start = ContentMargin,
                    end = ContentMargin,
                    top = 8.dp,
                    bottom = 6.dp,
                ),
            )
        }
        if (!yearStats.hasData) {
            item {
                Text(
                    "No plays recorded yet in $year. Plays count here from " +
                        "v1.0.6 onward — play a song and it will show up.",
                    fontSize = 14.sp,
                    color = InkMuted,
                    modifier = Modifier.padding(horizontal = ContentMargin, vertical = 8.dp),
                )
            }
        } else {
            item {
                Row(Modifier.padding(horizontal = ContentMargin)) {
                    StatTile(
                        value = "${yearStats.plays}",
                        label = if (yearStats.plays == 1) "play" else "plays",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    StatTile(
                        value = "${yearStats.minutes}",
                        label = "minutes",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (yearStats.knownDurationPlays < yearStats.plays) {
                    Text(
                        "Listening time counts only the ${yearStats.knownDurationPlays} of " +
                            "${yearStats.plays} plays with known lengths.",
                        fontSize = 13.sp,
                        color = InkMuted,
                        modifier = Modifier.padding(horizontal = ContentMargin),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                PrimaryButton(
                    text = "Share year card",
                    onClick = {
                        scope.launch {
                            val ok = StatsCard.share(context, yearStats, year)
                            if (!ok) showToast(context, "Couldn't share the year card.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentMargin),
                )
                Spacer(Modifier.height(4.dp))
            }
            if (yearStats.topArtists.isNotEmpty()) {
                item {
                    StatsSectionHeader("Top artists this year")
                }
                items(yearStats.topArtists.size) { i ->
                    val a = yearStats.topArtists[i]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ContentMargin, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${i + 1}.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkMuted,
                            modifier = Modifier.width(28.dp),
                        )
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
            if (yearStats.topTracks.isNotEmpty()) {
                item {
                    StatsSectionHeader("Top songs this year")
                }
                items(yearStats.topTracks.size) { i ->
                    val t = yearStats.topTracks[i]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ContentMargin, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${i + 1}.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkMuted,
                            modifier = Modifier.width(28.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                t.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (t.artist.isNotBlank()) {
                                Text(
                                    t.artist,
                                    fontSize = 13.sp,
                                    color = InkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            if (t.plays == 1) "1 play" else "${t.plays} plays",
                            fontSize = 13.sp,
                            color = InkMuted,
                        )
                    }
                }
            }
        }
        // ---- All time ----
        item {
            StatsSectionHeader("All time")
            if (totalPlays == 0) {
                Text(
                    "No plays counted yet — play a song and it will show up here.",
                    fontSize = 14.sp,
                    color = InkMuted,
                    modifier = Modifier.padding(horizontal = ContentMargin, vertical = 8.dp),
                )
            } else {
                Row(Modifier.padding(horizontal = ContentMargin)) {
                    StatTile(
                        value = "$totalPlays",
                        label = if (totalPlays == 1) "play" else "plays",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    StatTile(
                        value = "$likedCount",
                        label = if (likedCount == 1) "liked song" else "liked songs",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        if (topTracks.isNotEmpty()) {
            item {
                StatsSectionHeader("Most played")
            }
            items(topTracks.size) { i ->
                val pc = topTracks[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentMargin, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${i + 1}.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                        modifier = Modifier.width(28.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            pc.track.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val artist = com.sonara.app.data.displayArtist(pc.track.artist)
                        if (artist.isNotBlank()) {
                            Text(
                                artist,
                                fontSize = 13.sp,
                                color = InkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        if (pc.count == 1) "1 play" else "${pc.count} plays",
                        fontSize = 13.sp,
                        color = InkMuted,
                    )
                }
            }
        }
        if (topArtists.isNotEmpty()) {
            item {
                StatsSectionHeader("Top artists")
            }
            items(topArtists.size) { i ->
                val a = topArtists[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentMargin, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${i + 1}.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                        modifier = Modifier.width(28.dp),
                    )
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
        item {
            Row(
                Modifier.padding(horizontal = ContentMargin, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Stats come only from plays on this device. " +
                        "Play counts started with v1.10.16; yearly plays with v1.0.6.",
                    fontSize = 13.sp,
                    color = InkMuted,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Corners.card))
            .background(PaperSurface)
            .border(
                CurrentTokens.cardBorderWidth,
                PaperOutline,
                RoundedCornerShape(Corners.card),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = InkBlack,
            maxLines = 1,
        )
        Text(
            label,
            fontSize = 13.sp,
            color = InkMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatsSectionHeader(title: String) {
    Text(
        displayCased(title, CurrentTokens),
        fontSize = 20.sp,
        fontFamily = CurrentTokens.displayFont,
        letterSpacing = CurrentTokens.displayLetterSpacing,
        fontWeight = FontWeight.Black,
        color = InkBlack,
        modifier = Modifier.padding(
            start = ContentMargin,
            end = ContentMargin,
            top = 16.dp,
            bottom = 2.dp,
        ),
    )
}
