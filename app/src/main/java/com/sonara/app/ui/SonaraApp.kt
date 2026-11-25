package com.sonara.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.sonara.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.sonara.app.SonaraApplication
import com.sonara.app.data.COMMUNITY_PICKS_QUERY
import com.sonara.app.data.ChartData
import com.sonara.app.data.ChartRegion
import com.sonara.app.data.HOME_TOP_QUERY
import com.sonara.app.data.HOME_TRENDING_QUERY
import com.sonara.app.data.MoodChip
import com.sonara.app.data.NEW_RELEASES_QUERY
import com.sonara.app.data.Track
import com.sonara.app.data.TrackDurations
import com.sonara.app.data.WORKOUT_QUERY
import com.sonara.app.data.JamControlAction
import com.sonara.app.data.JamRole
import com.sonara.app.data.YoutubeMusicApi
import com.sonara.app.data.GenreApi
import org.json.JSONObject
import com.sonara.app.data.ArtistApi
import com.sonara.app.data.FOLLOWED_REFRESH_MAX_ARTISTS
import com.sonara.app.data.FollowedArtist
import com.sonara.app.data.FollowedShow
import com.sonara.app.data.followKey
import com.sonara.app.data.followedCacheStale
import com.sonara.app.data.mergeFollowedReleases
import com.sonara.app.data.displayArtist
import com.sonara.app.data.buildChartData
import com.sonara.app.data.chartQueries
import com.sonara.app.data.chartRegionParams
import com.sonara.app.data.moodQueries
import com.sonara.app.ui.screens.CrashReportScreen
import com.sonara.app.ui.screens.HomeScreen
import com.sonara.app.ui.screens.ArtistProfileScreen
import com.sonara.app.ui.screens.ArtistTarget
import com.sonara.app.ui.screens.PodcastShowScreen
import com.sonara.app.ui.screens.PodcastTarget
import com.sonara.app.ui.screens.StatsScreen
import com.sonara.app.data.JAM_STACK_MAX_SENYAP
import com.sonara.app.data.JamMember
import com.sonara.app.ui.screens.JamMarkIcon
import com.sonara.app.ui.screens.JamMemberStack
import com.sonara.app.ui.screens.JamScreen
import com.sonara.app.ui.screens.JamSheetOverlay
import com.sonara.app.ui.screens.JamStatusChip
import com.sonara.app.ui.screens.LibraryScreen
import com.sonara.app.data.AppThemeMode
import com.sonara.app.data.AppThemeSelection
import com.sonara.app.data.CustomThemeOverride
import com.sonara.app.data.appThemeIdFromStorage
import com.sonara.app.data.appThemeModeFromStorage
import com.sonara.app.data.storageKey
import com.sonara.app.ui.screens.NowPlayingScreen
import com.sonara.app.ui.screens.QueueScreen
import com.sonara.app.ui.screens.SearchScreen
import com.sonara.app.ui.screens.SettingsScreen
import com.sonara.app.ui.screens.jamHostOnlyToast
import com.sonara.app.ui.screens.rememberGuestLocked
import com.sonara.app.ui.theme.AccentFill
import com.sonara.app.ui.theme.BottomCluster
import com.sonara.app.ui.theme.ChromeBg
import com.sonara.app.ui.theme.OnAccent
import com.sonara.app.ui.theme.OnChrome
import com.sonara.app.ui.theme.StageBg
import com.sonara.app.ui.hardShadowDraw
import com.sonara.app.ui.senjaGlow
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.InkMuted
import com.sonara.app.ui.theme.SonaraTheme
import com.sonara.app.ui.theme.Sizes
import com.sonara.app.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen(val title: String) {
    HOME("Home"),
    SEARCH("Search"),
    LIBRARY("Library"),
    JAM("Jam"),
    SETTINGS("Settings"),
    QUEUE("Queue"),
    NOW_PLAYING("Now Playing"),
    ARTIST_PROFILE("Artist"),
    PODCAST_SHOW("Podcast"),
    STATS("Stats"),
    CRASH("Crash Report"),
}

/**
 * State aplikasi: dipegang sekali di root (bukan ViewModel terpisah,
 * supaya build manual tetap sederhana). Semua panggilan jaringan
 * jalan di Dispatchers.IO lewat coroutineScope.
 */
class SonaraState(val app: SonaraApplication) {
    val store = app.store
    val player = app.player
    val sync = app.sync
    val jam = app.jam

    var searchResults by mutableStateOf<List<Track>>(emptyList())
    var searchBusy by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)
    var searchSource by mutableStateOf("All")

    // ---- Data Beranda (dipegang di root, BUKAN di HomeScreen) ----
    // Polish v1.5: sebelumnya data Beranda hidup di HomeScreen, dan
    // AnimatedContent membuang komposisinya tiap ganti tab — kembali ke
    // Beranda selalu memuat ulang + spinner + kedip. Sekarang data
    // dimuat sekali dan bertahan antar tab; refresh hanya saat tombol
    // "Coba Lagi" ditekan.
    val ytmApi = YoutubeMusicApi()

    /**
     * Durasi lagu per videoId (v1.10.31): jalur feed tidak membawa
     * durasi — baris yang tampil meminta durasinya lewat lookupVideo,
     * hasilnya di-cache + dipersistensikan. Lihat [TrackDurations].
     */
    val durations = TrackDurations(
        loadPersisted = { store.durationCache() },
        savePersisted = { store.saveDurationCache(it) },
        lookup = { id -> ytmApi.lookupVideo(id).map { it.durationMs } },
        scope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO,
        ),
    )

    var homeTrending by mutableStateOf<List<Track>>(emptyList())
        private set
    var homeTop by mutableStateOf<List<Track>>(emptyList())
        private set
    var homeLiked by mutableStateOf<List<Track>>(emptyList())
        private set
    var homeLoaded by mutableStateOf(false)
        private set
    var homeLoading by mutableStateOf(false)
        private set
    var homeFailed by mutableStateOf(false)
        private set
    private var homeStale = false

    // ---- Artwork tile Browse (v1.10.6) ----
    // Nama kategori -> URL artwork hasil teratas pencarian YTM
    // (gl=US/hl=en). Dimuat SEKALI dan ditahan di root (bertahan
    // antar tab/recomposition); null/absen = fallback tile warna.
    var genreArtworks by mutableStateOf<Map<String, String?>>(emptyMap())
        private set
    private var genreArtworksLoading = false

    fun loadGenreArtworks(scope: kotlinx.coroutines.CoroutineScope) {
        if (genreArtworksLoading || genreArtworks.isNotEmpty()) return
        genreArtworksLoading = true
        scope.launch {
            val queries = listOf(
                "Pop" to "Pop",
                "Indie" to "Indie",
                "Heartbreak" to "Heartbreak",
                "Rock" to "Rock",
                // v1.10.7: artwork untuk semua kategori Browse baru
                // (nama harus sama persis dengan GenreTileData.name —
                // tile mencarinya lewat state.genreArtworks[name]).
                "Dangdut" to "Dangdut",
                "K-Pop" to "K-Pop",
                "Hip-Hop" to "Hip-Hop",
                "R&B" to "R&B",
                "Jazz" to "Jazz",
                "Electronic" to "Electronic",
                "Acoustic" to "Acoustic",
                "Metal" to "Metal",
                "Podcasts" to "podcasts",
            )
            val defs = queries.map { (name, q) ->
                async(Dispatchers.IO) {
                    name to ytmApi.search(q, gl = "US", hl = "en")
                        .getOrDefault(emptyList()).firstOrNull()?.imageUrl
                }
            }
            genreArtworks = defs.awaitAll().toMap()
            genreArtworksLoading = false
        }
    }

    fun loadHome(scope: kotlinx.coroutines.CoroutineScope) {
        if (homeLoading) {
            // Muat ulang diminta saat muatan sedang jalan — tandai,
            // muat ulang sekali setelah muatan ini selesai, bukan
            // menumpuk coroutine.
            homeStale = true
            return
        }
        homeLoading = true
        homeLoaded = false
        homeFailed = false
        scope.launch {
            // v1.10.25: feed Indonesia — query Indonesia + gl=ID/hl=id
            // lewat jalur Kotlin yang dipaksa (pola Charts 10.19;
            // mesin Go mengabaikan gl/hl dan tidak bisa diprobe dari
            // VM). Jalur pencarian biasa user tidak berubah
            // (default gl=ID/hl=id). Feed global tetap ada lewat
            // chip Global di Charts.
            val trendingDef = async(Dispatchers.IO) {
                ytmApi.search(HOME_TRENDING_QUERY, gl = "ID", hl = "id", forceKotlin = true)
            }
            val topDef = async(Dispatchers.IO) {
                ytmApi.search(HOME_TOP_QUERY, gl = "ID", hl = "id", forceKotlin = true)
            }
            val likedDef = async(Dispatchers.IO) {
                store.likedTracks()
            }
            val trending = trendingDef.await().getOrDefault(emptyList()).take(10)
            val top = topDef.await().getOrDefault(emptyList()).take(10)
            val liked = likedDef.await().take(10)
            homeTrending = trending
            homeTop = top
            homeLiked = liked
            homeFailed = trending.isEmpty() && top.isEmpty() && liked.isEmpty()
            homeLoaded = true
            homeLoading = false
            if (homeStale) {
                homeStale = false
                loadHome(scope)
            }
        }
    }

    // ---- Seksi Home + Charts baru (v1.10.18) -------------------------
    // Semua dari pencarian YTM asli (v1.10.25: gl=ID/hl=id jalur
    // Kotlin yang dipaksa, kecuali Charts Global) atau data lokal asli.
    // Dimuat SEKALI per sesi (guard seperti genreArtworks); gagal per
    // seksi → list kosong → UI menampilkan empty state per seksi,
    // tidak pernah mengosongkan seluruh Home.
    var sectionCommunity by mutableStateOf<List<Track>>(emptyList())
        private set
    var sectionWorkout by mutableStateOf<List<Track>>(emptyList())
        private set
    var sectionNewReleases by mutableStateOf<List<Track>>(emptyList())
        private set
    var sectionsLoaded by mutableStateOf(false)
        private set
    private var sectionsLoading = false

    fun loadHomeSections(scope: kotlinx.coroutines.CoroutineScope, force: Boolean = false) {
        if (sectionsLoading || (sectionsLoaded && !force)) return
        sectionsLoading = true
        scope.launch {
            val communityDef = async(Dispatchers.IO) {
                // v1.10.25: query + region Indonesia, jalur Kotlin
                // (pola Charts 10.19) — lihat COMMUNITY_PICKS_QUERY.
                ytmApi.search(COMMUNITY_PICKS_QUERY, gl = "ID", hl = "id", forceKotlin = true)
            }
            val workoutDef = async(Dispatchers.IO) {
                // v1.10.25: sama — lihat WORKOUT_QUERY.
                ytmApi.search(WORKOUT_QUERY, gl = "ID", hl = "id", forceKotlin = true)
            }
            val releasesDef = async(Dispatchers.IO) {
                // v1.10.19: query + region Indonesia, jalur Kotlin
                // (Go mengabaikan gl/hl) — lihat NEW_RELEASES_QUERY.
                ytmApi.search(NEW_RELEASES_QUERY, gl = "ID", hl = "id", forceKotlin = true)
            }
            sectionCommunity = communityDef.await().getOrDefault(emptyList()).take(6)
            sectionWorkout = workoutDef.await().getOrDefault(emptyList()).take(10)
            sectionNewReleases = releasesDef.await().getOrDefault(emptyList()).take(10)
            sectionsLoaded = true
            sectionsLoading = false
        }
    }

    // ---- Rak "New from artists you follow" (v1.10.21) ----
    // Diambil saat Home dibuka (tanpa background work/notifikasi):
    // cache per artis dulu (rak langsung tampil), lalu refresh
    // maksimal 5 artis yang cache-nya basi (> 1 jam). Himpunan
    // diikuti + cache LOKAL saja — cakupan sync tidak berubah.
    var followedReleases by mutableStateOf<List<Track>>(emptyList())
        private set
    /** Bertambah tiap follow/unfollow — UI membaca ulang status follow. */
    var followedVersion by mutableStateOf(0)
        private set
    private var followedLoading = false

    fun setArtistFollowed(name: String, browseId: String?, followed: Boolean) {
        val key = followKey(name, browseId)
        store.setFollowedArtist(
            FollowedArtist(
                key = key,
                name = name.trim(),
                browseId = browseId?.takeIf { it.isNotBlank() },
                followedAtMs = System.currentTimeMillis(),
            ),
            followed,
        )
        followedVersion++
        if (!followed) {
            // Cache artis itu sudah dihapus di store — rak ikut segar.
            followedReleases = mergeFollowedReleases(
                store.followedArtists()
                    .map { store.followedReleaseCache()[it.key]?.second.orEmpty() },
            )
        }
    }

    // ---- Follow acara podcast (v1.0.5) — lokal saja, cakupan sync tak berubah. ----
    /** Bertambah tiap follow/unfollow acara — UI membaca ulang status follow. */
    var podcastFollowVersion by mutableStateOf(0)
        private set

    fun setShowFollowed(show: FollowedShow, followed: Boolean) {
        store.setFollowedShow(show, followed)
        podcastFollowVersion++
    }

    fun loadFollowedReleases(scope: kotlinx.coroutines.CoroutineScope) {
        if (followedLoading) return
        val followed = store.followedArtists()
        if (followed.isEmpty()) {
            followedReleases = emptyList()
            return
        }
        val cache = store.followedReleaseCache()
        followedReleases = mergeFollowedReleases(
            followed.map { cache[it.key]?.second.orEmpty() },
        )
        followedLoading = true
        scope.launch {
            val now = System.currentTimeMillis()
            val stale = followed
                .filter { followedCacheStale(cache[it.key]?.first, now) }
                .take(FOLLOWED_REFRESH_MAX_ARTISTS)
            if (stale.isNotEmpty()) {
                val updated = cache.toMutableMap()
                for (artist in stale) {
                    val profile = withContext(Dispatchers.IO) {
                        runCatching {
                            ArtistApi.fetchArtistProfile(ytmApi, artist.name, artist.browseId)
                        }.getOrNull()
                    } ?: continue
                    // Lagu yang bisa diputar dari profil artis
                    // (top songs via browse yang sudah ada). Artis
                    // yang tidak mengembalikan apa pun = tidak ada
                    // item — tidak ada konten palsu.
                    val tracks = profile.topSongs.take(3)
                        .map { ytmApi.trackFromResult(it) }
                    updated[artist.key] = now to tracks
                }
                store.saveFollowedReleaseCache(updated)
                val all = store.followedArtists()
                val fresh = store.followedReleaseCache()
                followedReleases = mergeFollowedReleases(
                    all.map { fresh[it.key]?.second.orEmpty() },
                )
            }
            followedLoading = false
        }
    }

    // Feed per mood (v1.10.18; v1.10.25: query Indonesia + ID/id):
    // "All" memakai feed Beranda loadHome (tidak ada pemuatan
    // tambahan); mood lain memuat satu query bertema lewat jalur
    // search yang sama (jalur Kotlin dipaksa).
    var moodTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var moodLoading by mutableStateOf(false)
        private set
    private var moodLoadedFor: MoodChip? = null

    fun loadMood(scope: kotlinx.coroutines.CoroutineScope, mood: MoodChip) {
        if (mood == MoodChip.ALL || moodLoadedFor == mood || moodLoading) return
        moodLoading = true
        scope.launch {
            val q = moodQueries(mood).first()
            moodTracks = withContext(Dispatchers.IO) {
                // v1.10.25: jalur Kotlin dipaksa supaya gl/hl dari
                // moodQueries (ID/id) benar-benar berlaku — mesin Go
                // mengabaikan gl/hl (pola Charts 10.19).
                ytmApi.search(q.query, gl = q.gl, hl = q.hl, forceKotlin = true)
            }.getOrDefault(emptyList()).take(12)
            moodLoadedFor = mood
            moodLoading = false
        }
    }

    // Charts (v1.10.18): dibangun dari pencarian trending/top dengan
    // jalur Kotlin yang dipaksa (Go mengabaikan gl/hl) supaya chip
    // region benar-benar mengubah hasil. Provenance jujur di UI.
    var chartRegion by mutableStateOf(ChartRegion.GLOBAL)
        private set
    var chartData by mutableStateOf<ChartData?>(null)
        private set
    var chartLoading by mutableStateOf(false)
        private set
    var chartFailed by mutableStateOf(false)
        private set
    private var chartLoadedFor: ChartRegion? = null

    fun loadCharts(scope: kotlinx.coroutines.CoroutineScope, region: ChartRegion, force: Boolean = false) {
        chartRegion = region
        if ((chartLoadedFor == region && !force) || chartLoading) return
        chartLoading = true
        chartFailed = false
        scope.launch {
            val params = chartRegionParams(region)
            val queries = chartQueries(region)
            val trendingDef = async(Dispatchers.IO) {
                ytmApi.search(queries.trendingQuery, gl = params.gl, hl = params.hl, forceKotlin = true)
            }
            val topDef = async(Dispatchers.IO) {
                ytmApi.search(queries.topQuery, gl = params.gl, hl = params.hl, forceKotlin = true)
            }
            val trending = trendingDef.await().getOrDefault(emptyList())
            val top = topDef.await().getOrDefault(emptyList())
            chartFailed = trending.isEmpty() && top.isEmpty()
            chartData = buildChartData(trending, top)
            chartLoadedFor = region
            chartLoading = false
        }
    }

    // ---- Halaman genre (v1.10.33) ----
    // Cache per label (judul + query); judul halaman dari respons
    // browse (bukan label mentah) bila jalur browse berhasil.
    // Respons Explore (tombol genre + params) diambil SEKALI per sesi
    // — guard seperti genreArtworks; gagal = semua label jatuh ke
    // jalur fallback search, bukan halaman rusak.
    var genrePage by mutableStateOf<GenreApi.GenrePageData?>(null)
        private set
    var genrePageLabel by mutableStateOf("")
        private set
    var genrePageLoading by mutableStateOf(false)
        private set
    var genrePageFailed by mutableStateOf(false)
        private set
    private var genreLoadedFor: String? = null
    private var genreExploreRoot: JSONObject? = null
    private var genreExploreLoading = false

    fun loadGenrePage(
        scope: kotlinx.coroutines.CoroutineScope,
        label: String,
        query: String,
        force: Boolean = false,
    ) {
        genrePageLabel = label
        if ((genreLoadedFor == label && !force) || genrePageLoading) return
        genrePageLoading = true
        genrePageFailed = false
        scope.launch {
            var page: GenreApi.GenrePageData? = null
            withContext(Dispatchers.IO) {
                // Jalur browse: params dari respons Explore (cache sesi).
                if (genreExploreRoot == null && !genreExploreLoading) {
                    genreExploreLoading = true
                    genreExploreRoot = ytmApi.browse(GenreApi.EXPLORE_BROWSE_ID).getOrNull()
                    genreExploreLoading = false
                }
                val params = genreExploreRoot?.let { GenreApi.resolveGenreParams(it, label) }
                if (params != null) {
                    val root = ytmApi.browse(
                        GenreApi.GENRE_CATEGORY_BROWSE_ID,
                        params = params,
                    ).getOrNull()
                    val parsed = root?.let { GenreApi.parseGenreBrowse(it, label) }
                    if (parsed != null &&
                        (parsed.songs.isNotEmpty() || parsed.featured.isNotEmpty() ||
                            parsed.community.isNotEmpty() || parsed.videos.isNotEmpty() ||
                            parsed.albums.isNotEmpty())
                    ) {
                        page = parsed
                    }
                }
                // Jalur fallback: pencarian biasa. Semua hasil tampil di
                // Songs; seksi Videos/playlist/album TIDAK dibuat-buat
                // (lihat GenreApi) — halaman fallback hanya punya seksi
                // yang datanya benar-benar ada.
                if (page == null) {
                    val results = ytmApi.search(query, gl = "ID", hl = "id", forceKotlin = true)
                        .getOrDefault(emptyList())
                    val songs = results
                        .filter { !it.youtubeVideoId.isNullOrBlank() }
                        .take(GenreApi.SECTION_LIMIT)
                        .map {
                            GenreApi.GenreSong(
                                videoId = it.youtubeVideoId ?: "",
                                title = it.title,
                                artist = it.artist,
                                durationMs = it.durationMs,
                                imageUrl = it.imageUrl,
                                explicit = it.explicit,
                                artistBrowseId = it.artistBrowseId,
                            )
                        }
                    if (songs.isNotEmpty()) {
                        page = GenreApi.GenrePageData(
                            title = label,
                            songs = songs,
                            featured = emptyList(),
                            community = emptyList(),
                            videos = emptyList(),
                            albums = emptyList(),
                            fromBrowse = false,
                        )
                    }
                }
            }
            genrePageFailed = page == null ||
                (page?.songs?.isEmpty() == true && page?.featured?.isEmpty() == true &&
                    page?.community?.isEmpty() == true && page?.videos?.isEmpty() == true &&
                    page?.albums?.isEmpty() == true)
            genrePage = page
            genreLoadedFor = label
            genrePageLoading = false
        }
    }

    /**
     * Track koleksi (playlist VL… / album MPRE…) untuk diputar —
     * browse + parse, blocking; panggil dari Dispatchers.IO.
     * Kosong = koleksi gagal dimuat (pemanggil menampilkan toast
     * jujur, bukan antrean palsu).
     */
    fun fetchCollectionTracks(browseId: String): List<Track> {
        val root = ytmApi.browse(browseId).getOrNull() ?: return emptyList()
        return GenreApi.parseCollectionItems(root).map { GenreApi.genreSongToTrack(it) }
    }

}

/** Fase transisi hero artwork (v1.6): cover mini player morph jadi artwork detail. */
private enum class HeroPhase { Hidden, Opening, Open, Closing }

@Composable
fun SonaraApp(app: SonaraApplication, crashReport: String?, onCrashHandled: () -> Unit) {
    val state = remember { SonaraState(app) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    // ---- Tumpukan navigasi (v1.10.1: tombol Back di semua layar) ----
    // Elemen terakhir == layar aktif. Setiap pindah tab / buka detail /
    // buka Pengaturan / buka laporan crash mendorong layar ke tumpukan;
    // Back mengambil layar sebelumnya. Tumpukan TIDAK pernah kosong.
    var navStack by remember { mutableStateOf(listOf(Screen.HOME)) }
    val activity = LocalContext.current as? Activity
    // v1.10.6: auto-sync foreground — ON_START: sync langsung (tenang)
    // + mulai loop 60 detik; ON_STOP: hentikan loop. Tidak ada
    // background work periodik 1 menit (batas platform, BUILD doc).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    state.sync.syncPeriodic { msg ->
                        activity?.let { showToast(it, msg) }
                    }
                    state.sync.startPeriodicSync()
                }
                Lifecycle.Event.ON_STOP -> state.sync.stopPeriodicSync()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun navigateTo(target: Screen) {
        if (target == screen) return
        navStack = navStack + target
        screen = target
    }
    /** Kembali satu langkah. True bila ada layar sebelumnya. */
    fun popStack(): Boolean {
        if (navStack.size <= 1) return false
        navStack = navStack.dropLast(1)
        screen = navStack.last()
        return true
    }
    /** Aksi Back terpadu: pop tumpukan, atau ke latar belakang di root. */
    fun handleBackPress() {
        if (!popStack()) {
            activity?.moveTaskToBack(true)
        }
    }
    // Tombol Back sistem Android → tumpukan yang sama dengan tombol
    // Back di layar. Handler yang lebih dalam (detail, sheet, tampilan
    // detail Koleksi) lebih dulu menangani Back saat aktif.
    BackHandler(enabled = true) { handleBackPress() }
    var showCrash by remember { mutableStateOf(crashReport != null) }
    var pendingSearchQuery by remember { mutableStateOf<String?>(null) }
    // v1.10.18: target profil artis (nama + browseId bila diketahui).
    var artistTarget by remember { mutableStateOf<ArtistTarget?>(null) }
    fun openArtist(name: String, browseId: String?) {
        artistTarget = ArtistTarget(name, browseId)
        navigateTo(Screen.ARTIST_PROFILE)
    }
    // v1.0.5: target halaman acara podcast (browseId MPSP… + judul fallback).
    var podcastTarget by remember { mutableStateOf<PodcastTarget?>(null) }
    fun openPodcastShow(browseId: String, title: String) {
        podcastTarget = PodcastTarget(browseId, title)
        navigateTo(Screen.PODCAST_SHOW)
    }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val snackbarHostState = remember { SnackbarHostState() }

    // v1.10.16: Undo swipe-hapus antrean — snackbar "Removed from
    // queue" + aksi Undo (4 detik) → lagu disisipkan kembali di
    // posisi semula via insertIntoQueue (queueUid dipertahankan).
    fun handleQueueItemRemoved(track: Track, index: Int) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Removed from queue",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                app.player.insertIntoQueue(index, track)
            }
        }
    }

    val currentTrack by app.player.currentTrack.collectAsState()
    val isPlaying by app.player.isPlaying.collectAsState()
    val positionMs by app.player.positionMs.collectAsState()
    // v1.10.6: palet Garis Arus strip dari artwork lagu yang sedang
    // diputar (fallback merah + hijau muda bila tanpa artwork).
    val wavePalette = rememberWavePalette(currentTrack?.imageUrl)
    val durationMs by app.player.durationMs.collectAsState()
    val playerError by app.player.error.collectAsState()

    // v1.10.42: layar Jam penuh (tab ke-4) menggantikan sheet kecil
    // Start/Join 10.14 — masuk sesi dari tab Jam, mark header, atau
    // deep link. Kartu nempel/chip ramping tetap membuka sheet
    // overlay (JamSheetOverlay) seperti sebelumnya.
    val jamUiState by state.jam.uiState.collectAsState()
    // v1.10.18: UI Jam mekar kini BOTTOM SHEET OVERLAY (bukan kartu
    // nempel) — dibuka dari chip ramping di chrome bawah / pill
    // header. Sesi berakhir → sheet menutup sendiri.
    var jamSheetOpen by remember { mutableStateOf(false) }
    LaunchedEffect(jamUiState.active) {
        if (!jamUiState.active) jamSheetOpen = false
    }
    val guestLocked = rememberGuestLocked(state)
    val jamContext = LocalContext.current
    /**
     * Gerbang kontrol playback (v1.10.42): tamu di sesi aktif TIDAK
     * memutar lokal. Kontrol bersama MATI → toast host-only
     * (perilaku lama, guestLocked). Kontrol bersama NYALA →
     * permintaan kontrol dikirim ke host (pola jumpRequests);
     * toast hanya bila manager mengembalikan pesan (mis. sesi
     * berakhir di tengah jalan). Di luar sesi: aksi lokal biasa.
     */
    fun guardedPlayback(
        action: JamControlAction,
        positionMs: Long = 0L,
        local: () -> Unit,
    ) {
        if (guestLocked) {
            jamHostOnlyToast(jamContext)
            return
        }
        if (jamUiState.active && jamUiState.role == JamRole.GUEST) {
            state.jam.requestPlaybackControl(action, positionMs) { msg ->
                if (msg.isNotEmpty()) showToast(jamContext, msg)
            }
            return
        }
        local()
    }

    // v1.10.11/10.18/10.42: deep link Jam (custom scheme / App Link
    // https) — konsumsi SEKALI dari Application, buka LAYAR Jam
    // dengan kode terisi. Penanganan join-nya di JamScreen
    // (JamEmptyState: auto-join bila sudah login, login dulu bila
    // belum, konfirmasi bila sudah di sesi lain) — di sini hanya
    // meneruskan kode + navigasi.
    val pendingJamCode by app.pendingJamCode.collectAsState()
    var deepLinkJoinCode by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingJamCode) {
        val code = pendingJamCode ?: return@LaunchedEffect
        app.pendingJamCode.value = null
        deepLinkJoinCode = code
        navigateTo(Screen.JAM)
    }

    // ---- Transisi hero artwork (v1.6) ----
    // Dua rect diukur di koordinat jendela yang sama, lalu diubah ke
    // koordinat lokal root (rootOrigin) supaya overlay sejajar persis
    // dengan kedua cover aslinya. heroTarget hanya diperbarui saat
    // artwork sedang diam (bukan saat morph berjalan) supaya overlay
    // tidak mengejar target yang bergerak.
    val reducedMotion = rememberReducedMotion()
    var heroFrom by remember { mutableStateOf<Rect?>(null) }
    var heroTarget by remember { mutableStateOf<Rect?>(null) }
    var heroPhase by remember { mutableStateOf(HeroPhase.Hidden) }
    var heroTrack by remember { mutableStateOf<Track?>(null) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    // Morph dijalankan sebagai progres 0→1 + lerp antar-rect (bukan
    // Animatable<Rect>: Rect.VectorConverter tidak tersedia di
    // animation-core 1.7.0 pada build manual ini).
    val heroProgress = remember { Animatable(0f) }
    var heroFromRect by remember { mutableStateOf<Rect?>(null) }
    var heroToRect by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current
    // v1.10.18: auto-hide chrome bawah saat scroll (logika murni di
    // ChromeVisibilityState; ambang ~36dp dari densitas layar).
    // `visible` di kelas itu var biasa — cermin Compose-nya di sini.
    val chromeVisibility = remember {
        ChromeVisibilityState(with(density) { 36.dp.roundToPx() })
    }
    var chromeVisible by remember { mutableStateOf(true) }
    fun applyChromeScroll(index: Int, offsetPx: Int) {
        chromeVisible = chromeVisibility.onScroll(index, offsetPx)
    }
    fun showChromeNow() {
        chromeVisibility.forceVisible()
        chromeVisible = true
    }
    // Ganti lagu / pindah layar → chrome harus terlihat.
    LaunchedEffect(currentTrack?.id) { showChromeNow() }
    LaunchedEffect(screen) { showChromeNow() }
    fun toLocal(windowRect: Rect): Rect =
        windowRect.translate(-rootOrigin.x, -rootOrigin.y)

    fun openDetailWithHero(track: Track?) {
        val from = heroFrom
        val target = heroTarget
        if (!reducedMotion && from != null && target != null && track != null) {
            heroTrack = track
            heroPhase = HeroPhase.Opening
            navigateTo(Screen.NOW_PLAYING)
        } else {
            navigateTo(Screen.NOW_PLAYING)
        }
    }
    fun requestCloseDetail() {
        val from = heroFrom
        val target = heroTarget
        if (!reducedMotion && from != null && target != null && heroPhase == HeroPhase.Open) {
            heroPhase = HeroPhase.Closing
        } else if (!popStack()) {
            // Pengaman: tumpukan hanya berisi detail — kembali ke Beranda.
            screen = Screen.HOME
        }
    }

    // v1.10.21: link YouTube Music dari app lain — konsumsi SEKALI,
    // resolve metadata via jalur /player yang sudah ada, lalu putar
    // + buka detail. Gagal resolve = toast jujur, tanpa crash.
    // (Di sini, SETELAH openDetailWithHero dideklarasikan — fun
    // lokal hanya terlihat setelah titik deklarasinya.)
    val pendingYtmVideoId by app.pendingYtmVideoId.collectAsState()
    LaunchedEffect(pendingYtmVideoId) {
        val videoId = pendingYtmVideoId ?: return@LaunchedEffect
        app.pendingYtmVideoId.value = null
        val meta = withContext(Dispatchers.IO) {
            runCatching { state.ytmApi.lookupVideo(videoId) }.getOrNull()?.getOrNull()
        }
        if (meta == null) {
            showToast(jamContext, "Couldn't open that link.")
            return@LaunchedEffect
        }
        val track = Track(
            id = "ytm:${meta.videoId}",
            title = meta.title,
            artist = meta.artist,
            album = "",
            source = com.sonara.app.data.Source.YOUTUBE_MUSIC,
            engine = com.sonara.app.data.Engine.YOUTUBE,
            durationMs = meta.durationMs,
            youtubeVideoId = meta.videoId,
            imageUrl = meta.imageUrl,
            explicit = false,
        )
        state.player.playTrack(track)
        openDetailWithHero(track)
    }
    LaunchedEffect(heroPhase) {
        // heroFrom/heroTarget disimpan dalam koordinat JENDELA;
        // SATU-SATUNYA konversi ke lokal root terjadi di sini
        // (v1.10.40: konversi ganda = salinan bergeser sejauh
        // rootOrigin). Jangan tambah toLocal() di situs assignment.
        when (heroPhase) {
            HeroPhase.Opening -> {
                val from = heroFrom?.let(::toLocal)
                val target = heroTarget?.let(::toLocal)
                if (from == null || target == null) {
                    // Pengaman: rect belum terukur (mis. buka pertama) —
                    // batalkan hero, tampilkan detail seperti biasa.
                    heroPhase = HeroPhase.Hidden
                    return@LaunchedEffect
                }
                heroFromRect = from
                heroToRect = target
                heroProgress.snapTo(0f)
                heroProgress.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
                heroPhase = HeroPhase.Open
            }
            HeroPhase.Closing -> {
                val from = heroTarget?.let(::toLocal)
                val target = heroFrom?.let(::toLocal)
                if (from == null || target == null) {
                    heroPhase = HeroPhase.Hidden
                    if (!popStack()) screen = Screen.HOME
                    return@LaunchedEffect
                }
                heroFromRect = from
                heroToRect = target
                heroProgress.snapTo(0f)
                heroProgress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                if (!popStack()) screen = Screen.HOME
                heroPhase = HeroPhase.Hidden
            }
            else -> Unit
        }
    }

    // Error player → snackbar dengan aksi "Coba lagi" (v1.10.1):
    // mengetuknya memutar ulang lagu saat ini dengan stream baru.
    LaunchedEffect(playerError) {
        playerError?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Try again",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                app.player.retryCurrent()
            }
            app.player.clearError()
        }
    }

    val showChrome = screen != Screen.NOW_PLAYING && screen != Screen.CRASH

    // v1.10.26: theme switcher — pilihan tema + mode dibaca dari
    // prefs, dipegang sebagai state di root supaya ganti tema
    // me-recompose SELURUH pohon tanpa restart. Default = ARUS Light.
    // (v1.5: dulu SATU-SATUNYA tema ARUS terang — kini 4 tema x 2 mode.)
    var themeSelection by remember {
        mutableStateOf(
            AppThemeSelection(
                theme = appThemeIdFromStorage(state.store.themeId),
                mode = appThemeModeFromStorage(state.store.themeMode),
            ),
        )
    }
    fun applyThemeSelection(sel: AppThemeSelection) {
        state.store.themeId = sel.theme.storageKey()
        state.store.themeMode = sel.mode.storageKey()
        themeSelection = sel
    }
    // v1.0.7: tema kustom — override aksen + radius, dipegang di root
    // supaya berlaku ke seluruh pohon; mati = null (token dasar).
    var customTheme by remember {
        mutableStateOf(state.store.customThemeOverride())
    }
    fun applyCustomTheme(override: CustomThemeOverride?) {
        if (override == null) {
            state.store.clearCustomTheme()
            customTheme = null
        } else {
            state.store.saveCustomTheme(override)
            customTheme = override
        }
    }
    // v1.0.7: halaman Settings yang diminta command palette
    // ("playback") — dikonsumsi SettingsScreen saat dibuka.
    var pendingSettingsPage by remember { mutableStateOf<String?>(null) }
    SonaraTheme(
        theme = themeSelection.theme,
        mode = themeSelection.mode,
        customOverride = customTheme,
    ) {
        // Ikon status/nav bar mengikuti luminans tema (terang =
        // ikon gelap). Layar detail menimpa sementara dengan
        // SystemBarAppearance(darkBackground = true) miliknya.
        SystemBarAppearance(darkBackground = themeSelection.mode == AppThemeMode.DARK)
    // WebView embed (1x1 dp, audio saja) — dimiliki Application
    // (v1.0.2): composition hanya me-host view yang sama; view-nya
    // bertahan melewati hancur/bangun-ulang activity.
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.boundsInWindow().topLeft },
    ) {
        AndroidView(
            factory = { app.embedPlayer.ownedView() },
            modifier = Modifier.size(1.dp),
        )

        if (showCrash && crashReport != null) {
            CrashReportScreen(
                reportText = crashReport,
                // Overlay awal crash: Back = tutup overlay (sama
                // seperti Lanjutkan), bukan keluar app.
                onBack = {
                    showCrash = false
                    onCrashHandled()
                },
                onDelete = {
                    com.sonara.app.data.CrashReportStore.clearAll(app)
                    showCrash = false
                    onCrashHandled()
                },
                onContinue = {
                    showCrash = false
                    onCrashHandled()
                },
            )
            return@Box
        }

        // v1.6 daya tahan: di layar besar (tablet/foldable), konten
        // dibatasi 720dp dan dipusatkan — tidak melar selebar layar.
        // Di HP (< 720dp) tidak ada perubahan sama sekali.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxSize().widthIn(max = 720.dp)) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            // v1.5 ARUS: tidak ada TopAppBar persisten; di bawah ada
            // kapsul mini player + kapsul navigasi di zona jempol.
            bottomBar = {
                if (showChrome) {
                    // v1.10.18: klaster bawah auto-hide (geser + fade
                    // ~250ms) saat scroll ke bawah; kembali saat
                    // scroll ke atas / ganti lagu / pill mengambang
                    // diketuk. Isi: chip Jam ramping + mini player
                    // (wave Garis Arus menempel di bibir atas kapsul)
                    // + nav kapsul. v1.10.29 SLIM (pilihan user dari
                    // POC; nav 56dp sejak v1.10.30): total tumpukan 130dp — ukuran hidup di
                    // token tema [BottomCluster], bukan di sini.
                    // Mini player TETAP LENGKAP (semua kontrol ada).
                    AnimatedVisibility(
                        visible = chromeVisible,
                        enter = slideInVertically(tween(250)) { it } +
                            fadeIn(tween(250)),
                        exit = slideOutVertically(tween(250)) { it } +
                            fadeOut(tween(250)),
                    ) {
                    Column {
                        // Chip Jam ramping SATU baris (~30dp): mark
                        // broadcast + LIVE • KODE • N listening •
                        // judul. Ketuk → sheet overlay Jam.
                        if (jamUiState.active) {
                            JamStatusChip(
                                state = state,
                                onClick = { jamSheetOpen = true },
                            )
                            Spacer(Modifier.height(BottomCluster.jamChipGap))
                        }
                        if (currentTrack != null) {
                            // Wave Garis Arus TERINTEGRASI di bibir
                            // atas kapsul mini player (strip sebagai
                            // baris tersendiri DIHAPUS): matematika
                            // wave + warna artwork + sinkron posisi
                            // PERSIS seperti strip lama, hanya pindah
                            // tempat; timestamp hanya di layar detail.
                            Column {
                                ArusWaveProgress(
                                    progress = if (durationMs > 0) {
                                        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    },
                                    playing = isPlaying,
                                    onSeek = { frac ->
                                        val target = (frac * durationMs).toLong()
                                        guardedPlayback(JamControlAction.SEEK, target) {
                                            if (durationMs > 0) {
                                                app.player.seekTo(target)
                                            }
                                        }
                                    },
                                    onSeekEnd = { haptic(HapticKind.Confirm) },
                                    showKnob = false,
                                    amplitude = 2.dp,
                                    strokeWidth = 1.5.dp,
                                    height = BottomCluster.waveHeight,
                                    waveColorA = wavePalette.colorA,
                                    waveColorB = wavePalette.colorB,
                                    trackAlpha = 0.45f,
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                )
                            MiniPlayer(
                                track = currentTrack!!,
                                isPlaying = isPlaying,
                                positionMs = positionMs,
                                durationMs = durationMs,
                                isLiked = state.store.isLiked(currentTrack!!.id),
                                onToggleLike = {
                                    val t = currentTrack!!
                                    val liked = state.store.isLiked(t.id)
                                    state.store.setLikedTrack(t, !liked)
                                },
                                onToggle = { guardedPlayback(JamControlAction.TOGGLE) { app.player.togglePlay() } },
                                onNext = { guardedPlayback(JamControlAction.NEXT) { app.player.next() } },
                                onPrevious = { guardedPlayback(JamControlAction.PREVIOUS) { app.player.previous() } },
                                onSeek = { frac ->
                                    val target = (frac * durationMs).toLong()
                                    guardedPlayback(JamControlAction.SEEK, target) {
                                        if (durationMs > 0) {
                                            app.player.seekTo(target)
                                        }
                                    }
                                },
                                onClick = { openDetailWithHero(currentTrack) },
                                onCoverBounds = { windowRect ->
                                    // v1.10.40: simpan apa adanya dalam
                                    // koordinat JENDELA. Konversi ke
                                    // lokal root hanya SEKALI di
                                    // LaunchedEffect(heroPhase); dulu
                                    // toLocal() dipakai di sini DAN di
                                    // effect (konversi ganda) sehingga
                                    // salinan hero mendarat bergeser
                                    // tepat sejauh rootOrigin begitu
                                    // origin jendela tak nol.
                                    heroFrom = windowRect
                                },
                            )
                            Spacer(Modifier.height(BottomCluster.gap))
                        }
                        }
                        CapsuleNav(
                            current = screen,
                            onSelect = { navigateTo(it) },
                            jamActive = jamUiState.active,
                            jamMembers = jamUiState.members,
                        )
                        Spacer(Modifier.height(BottomCluster.bottomInset))
                    }
                    }
                }
            },
        ) { padding ->
            // Transisi layar — Now Playing masuk dengan slide-up dan
            // keluar dengan slide-down (overlay detail); layar lain fade.
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (targetState == Screen.NOW_PLAYING) {
                        (slideInVertically(tween(300)) { it } + fadeIn(tween(300)))
                            .togetherWith(slideOutVertically(tween(300)) { it } + fadeOut(tween(300)))
                    } else if (initialState == Screen.NOW_PLAYING) {
                        (fadeIn(tween(200)))
                            .togetherWith(slideOutVertically(tween(300)) { it } + fadeOut(tween(300)))
                    } else {
                        (fadeIn(tween(200))).togetherWith(fadeOut(tween(200)))
                    }
                },
                label = "screen",
                modifier = Modifier.fillMaxSize(),
            ) { target ->
                // v1.10.6 (CACAT 3): detail full-screen memakai top = 0
                // supaya latar gelap menutup sampai tepi atas jendela
                // (di belakang status bar); padding status bar
                // diterapkan DI DALAM latar gelap oleh detail sendiri.
                // Layar lain tetap memakai padding Scaffold penuh.
                val screenPadding = if (target == Screen.NOW_PLAYING) {
                    PaddingValues(bottom = padding.calculateBottomPadding())
                } else {
                    padding
                }
                Box(Modifier.fillMaxSize().dotGridBackground().padding(screenPadding)) {
                    when (target) {
                        Screen.HOME -> HomeScreen(
                            state = state,
                            onBack = { handleBackPress() },
                            onChromeScroll = { i, o -> applyChromeScroll(i, o) },
                            onOpenArtist = { name, browseId -> openArtist(name, browseId) },
                            onOpenSearch = { preset ->
                                pendingSearchQuery = preset
                                navigateTo(Screen.SEARCH)
                            },
                            onOpenSettings = { navigateTo(Screen.SETTINGS) },
                            onOpenDetail = { track -> openDetailWithHero(track) },
                            onOpenJam = {
                                // v1.10.42: SELALU layar Jam penuh
                                // (status kosong = Start/Join di sana).
                                navigateTo(Screen.JAM)
                            },
                        )
                        Screen.SEARCH -> SearchScreen(
                            state = state,
                            onBack = { handleBackPress() },
                            presetQuery = pendingSearchQuery,
                            onPresetConsumed = { pendingSearchQuery = null },
                            onChromeScroll = { i, o -> applyChromeScroll(i, o) },
                            onOpenArtist = { name, browseId -> openArtist(name, browseId) },
                            onOpenPodcastShow = { browseId, title ->
                                openPodcastShow(browseId, title)
                            },
                        )
                        Screen.LIBRARY -> LibraryScreen(
                            state = state,
                            onBack = { handleBackPress() },
                            onOpenSettings = { navigateTo(Screen.SETTINGS) },
                            // v1.10.16: tap artis "Most played" → Cari
                            // dengan query artis itu (jalur preset yang
                            // sama dengan Beranda).
                            onOpenSearch = { query ->
                                pendingSearchQuery = query
                                navigateTo(Screen.SEARCH)
                            },
                            onOpenPodcastShow = { browseId, title ->
                                openPodcastShow(browseId, title)
                            },
                            onOpenStats = { navigateTo(Screen.STATS) },
                            onChromeScroll = { i, o -> applyChromeScroll(i, o) },
                        )
                        Screen.JAM -> JamScreen(
                            state = state,
                            onBack = { handleBackPress() },
                            onOpenDetail = { track -> openDetailWithHero(track) },
                            initialJoinCode = deepLinkJoinCode,
                            onInitialJoinCodeConsumed = { deepLinkJoinCode = null },
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            state = state,
                            onBack = { handleBackPress() },
                            onShowCrash = { navigateTo(Screen.CRASH) },
                            themeSelection = themeSelection,
                            onThemeChange = { applyThemeSelection(it) },
                            // v1.0.7: tema kustom + halaman awal dari
                            // command palette (mis. Playback).
                            customTheme = customTheme,
                            onCustomThemeChange = { applyCustomTheme(it) },
                            startPage = pendingSettingsPage,
                            onStartPageConsumed = { pendingSettingsPage = null },
                        )
                        Screen.QUEUE -> QueueScreen(
                            state = state,
                            onBack = { popStack() },
                            onQueueItemRemoved = { track, index ->
                                handleQueueItemRemoved(track, index)
                            },
                        )
                        Screen.NOW_PLAYING -> NowPlayingScreen(
                            state = state,
                            onBack = { requestCloseDetail() },
                            onOpenArtist = { name, browseId -> openArtist(name, browseId) },
                            onOpenPodcastShow = { browseId, title ->
                                openPodcastShow(browseId, title)
                            },
                            onOpenQueue = { navigateTo(Screen.QUEUE) },
                            onOpenSearch = { query ->
                                pendingSearchQuery = query
                                navigateTo(Screen.SEARCH)
                            },
                            onQueueItemRemoved = { track, index ->
                                handleQueueItemRemoved(track, index)
                            },
                            heroHidingArtwork = heroPhase == HeroPhase.Opening ||
                                heroPhase == HeroPhase.Closing,
                            onArtworkBounds = { windowRect ->
                                // Jangan perbarui target saat morph
                                // berjalan — overlay mengejar target
                                // yang bergerak kalau rect ini berubah.
                                // v1.10.40: simpan koordinat jendela
                                // apa adanya (lihat onCoverBounds) —
                                // konversi ke lokal root hanya sekali
                                // di LaunchedEffect(heroPhase).
                                if (heroPhase != HeroPhase.Opening) {
                                    heroTarget = windowRect
                                }
                            },
                        )
                        Screen.ARTIST_PROFILE -> ArtistProfileScreen(
                            state = state,
                            target = artistTarget,
                            onBack = { handleBackPress() },
                            onOpenDetail = { track -> openDetailWithHero(track) },
                            onOpenSearch = { query ->
                                pendingSearchQuery = query
                                navigateTo(Screen.SEARCH)
                            },
                            onOpenArtist = { name, browseId -> openArtist(name, browseId) },
                        )
                        Screen.PODCAST_SHOW -> PodcastShowScreen(
                            state = state,
                            target = podcastTarget,
                            onBack = { handleBackPress() },
                            onOpenDetail = { track -> openDetailWithHero(track) },
                            onOpenShow = { t -> openPodcastShow(t.browseId, t.title) },
                        )
                        Screen.STATS -> StatsScreen(
                            state = state,
                            onBack = { handleBackPress() },
                        )
                        Screen.CRASH -> CrashReportScreen(
                            reportText = com.sonara.app.data.CrashReportStore.readLatest(app)
                                ?: "No crash reports saved.",
                            onBack = { handleBackPress() },
                            onDelete = {
                                com.sonara.app.data.CrashReportStore.clearAll(app)
                                popStack()
                            },
                            onContinue = { popStack() },
                        )
                    }
                }
            }
        }
        // Floating pill Jam (v1.10.18): HANYA saat sesi aktif DAN
        // chrome tersembunyi — mark broadcast + kode, ~32dp, pojok
        // kanan bawah. Ketuk → chrome kembali (TIDAK langsung
        // membuka sheet; ketuk chip yang muncul untuk itu).
        if (!chromeVisible && jamUiState.active && showChrome &&
            !jamSheetOpen
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Row(
                    Modifier
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 16.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(Corners.pill))
                        .background(StageBg)
                        .clickable { showChromeNow() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_jam_broadcast_dark),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        jamUiState.code,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
        } // Box 720dp
        } // Box pemusat

        // v1.10.18: UI Jam mekar sebagai bottom sheet overlay (~75%
        // layar + scrim; Back menutup sheet dulu). Isi = JamSessionBody
        // yang sama dengan layar Jam penuh.
        JamSheetOverlay(
            state = state,
            visible = jamSheetOpen && jamUiState.active,
            onDismiss = { jamSheetOpen = false },
        )

        // ---- Overlay transisi hero (v1.6) ----
        // Cover "terbang" dari kapsul mini player ke artwork detail
        // (dan sebaliknya saat menutup), di atas segalanya, dalam
        // koordinat lokal root. Selama morph, ketukan diblok supaya
        // tidak ada tombol di bawahnya yang ikut terpencet.
        if (heroPhase == HeroPhase.Opening || heroPhase == HeroPhase.Closing) {
            val fromRect = heroFromRect
            val toRect = heroToRect
            val rect = if (fromRect != null && toRect != null) {
                lerp(fromRect, toRect, heroProgress.value)
            } else {
                null
            }
            val ht = heroTrack
            if (ht != null && rect != null && rect.width > 1f && rect.height > 1f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {},
                ) {
                    val w = with(density) { rect.width.toDp() }
                    val h = with(density) { rect.height.toDp() }
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    rect.left.roundToInt(),
                                    rect.top.roundToInt(),
                                )
                            }
                            .size(w, h)
                            .clip(RoundedCornerShape(Corners.poster)),
                    ) {
                        CoverArt(track = ht, size = w, corner = 0.dp)
                    }
                }
            }
        }
    }
    } // SonaraTheme
}

/**
 * Kapsul navigasi persis screenshot — kapsul PUTIH berbayangan;
 * item aktif = pill HITAM berisi ikon + label putih; item nonaktif
 * ikon + label coklat-abu. Label: Beranda / Cari / Koleksi
 * (ikon Koleksi = grid, bukan hati) + Jam (v1.10.42, tab ke-4:
 * mark broadcast Canvas + dot LIVE aksen saat sesi aktif;
 * v1.10.44: tumpukan avatar anggota saat sesi hidup).
 */
@Composable
private fun CapsuleNav(
    current: Screen,
    onSelect: (Screen) -> Unit,
    jamActive: Boolean,
    jamMembers: List<JamMember> = emptyList(),
) {
    // SENYAP: tanpa kapsul — tab teks bergaris bawah (prototype).
    if (CurrentTokens.textTabNav) {
        TextTabNav(current = current, onSelect = onSelect, jamActive = jamActive, jamMembers = jamMembers)
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(BottomCluster.navHeight)
            .hardShadowDraw(Corners.miniPlayer, CurrentTokens.hardShadowTile)
            .shadow(8.dp, RoundedCornerShape(Corners.miniPlayer))
            .clip(RoundedCornerShape(Corners.miniPlayer))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = BottomCluster.navPaddingV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val navItems = listOf(
            Triple(Screen.HOME, "Home", Icons.Filled.Home to Icons.Outlined.Home),
            Triple(Screen.SEARCH, "Search", Icons.Filled.Search to Icons.Outlined.Search),
            Triple(Screen.LIBRARY, "Library", Icons.Filled.GridView to Icons.Filled.GridView),
            // Jam: pasangan ikon sentinel — ikon sebenarnya digambar
            // JamMarkIcon (Canvas, warna token) di bawah.
            Triple(Screen.JAM, "Jam", Icons.Filled.Home to Icons.Filled.Home),
        )
        for ((item, label, icons) in navItems) {
            val selected = current == item
            BouncyBox(
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            ) {
                // v1.10.44: pembungkus Box supaya tumpukan avatar
                // anggota bisa menempel di sudut kanan atas item Jam
                // tanpa menggeser ikon/label.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(BottomCluster.navItemHeight),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(Corners.pill))
                            .background(if (selected) ChromeBg else Color.Transparent),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        JumpIcon(selected = selected) {
                            if (item == Screen.JAM) {
                                // Mark broadcast + dot LIVE (sesi aktif
                                // tapi daftar anggota masih kosong) /
                                // tumpukan avatar (v1.10.44) bila ada
                                // anggota segar — keduanya tidak tampil
                                // bersamaan.
                                Box(Modifier.size(BottomCluster.navIcon)) {
                                    JamMarkIcon(
                                        color = if (selected) OnChrome else InkMuted,
                                        markSize = BottomCluster.navIcon,
                                    )
                                    if (jamActive && jamMembers.isEmpty()) {
                                        Box(
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(AccentFill)
                                                .border(
                                                    1.5.dp,
                                                    if (selected) ChromeBg else MaterialTheme.colorScheme.surface,
                                                    CircleShape,
                                                ),
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    if (selected) icons.first else icons.second,
                                    contentDescription = label,
                                    modifier = Modifier.size(BottomCluster.navIcon),
                                    tint = if (selected) OnChrome else InkMuted,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            label,
                            fontSize = BottomCluster.navLabelSize,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) OnChrome else InkMuted,
                        )
                    }
                    // v1.10.44: tumpukan avatar anggota di sudut kanan
                    // atas item Jam — hanya saat sesi hidup dan ada
                    // anggota segar. Bukan tombol (tap = tap item).
                    if (item == Screen.JAM && jamActive && jamMembers.isNotEmpty()) {
                        JamMemberStack(
                            members = jamMembers,
                            avatarSize = 16.dp,
                            borderColor = if (selected) ChromeBg else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 1.dp, end = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Navigasi SENYAP (v1.10.37): tanpa kapsul/bayangan — empat tab teks
 * dengan garis bawah 2dp pada tab aktif + hairline di atas bar,
 * seperti prototype. Tinggi + ukuran label mengikuti token klaster.
 * Tab Jam (v1.10.42) membawa dot LIVE aksen saat sesi aktif.
 */
@Composable
private fun TextTabNav(
    current: Screen,
    onSelect: (Screen) -> Unit,
    jamActive: Boolean,
    jamMembers: List<JamMember> = emptyList(),
) {
    val tokens = CurrentTokens
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawBehind {
                drawLine(
                    color = tokens.text.copy(alpha = 0.20f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(BottomCluster.navHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for ((item, label) in listOf(
                Screen.HOME to "Home",
                Screen.SEARCH to "Search",
                Screen.LIBRARY to "Library",
                Screen.JAM to "Jam",
            )) {
                val selected = current == item
                BouncyBox(
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(BottomCluster.navItemHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                fontSize = BottomCluster.navLabelSize,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) tokens.text else InkMuted,
                            )
                            // v1.10.44: tumpukan avatar anggota (maks 2,
                            // ruang tab teks sempit) menggantikan dot
                            // LIVE bila ada anggota segar; dot tetap
                            // bila sesi aktif tapi anggota kosong.
                            if (item == Screen.JAM && jamActive && jamMembers.isNotEmpty()) {
                                Spacer(Modifier.width(5.dp))
                                JamMemberStack(
                                    members = jamMembers,
                                    maxShown = JAM_STACK_MAX_SENYAP,
                                    avatarSize = 14.dp,
                                    borderColor = tokens.background,
                                )
                            } else if (item == Screen.JAM && jamActive) {
                                Spacer(Modifier.width(5.dp))
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(AccentFill),
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier
                                .width(28.dp)
                                .height(2.dp)
                                .background(if (selected) tokens.text else Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mini player persis screenshot — kapsul HITAM #1B1813 SLIM v1.10.29
 * setinggi 54dp (dulu 73dp): cover BULAT berputar saat jalan +
 * judul/artis terpotong 1 baris + ikon hati outline + prev +
 * tombol play ORANYE #FF4E24 44dp (segitiga putih) + next.
 * Swipe kiri/kanan = next/previous. Ketuk → detail lagu.
 * (Garis Arus kini strip terpisah di atasnya.)
 * Ukuran hidup di token tema [BottomCluster] — jangan hardcode.
 */
@Composable
private fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onClick: () -> Unit,
    isLiked: Boolean = false,
    onToggleLike: () -> Unit = {},
    onCoverBounds: (Rect) -> Unit = {},
) {
    var dragX by remember { mutableStateOf(0f) }
    val haptic = rememberHaptic()
    // Flip 360° cover tiap ganti lagu: putaran Y 600ms.
    var flipCount by remember { mutableStateOf(0) }
    LaunchedEffect(track.id) { flipCount++ }
    val flipRotation by animateFloatAsState(
        targetValue = 360f * flipCount,
        animationSpec = tween(600),
        label = "coverFlip",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(BottomCluster.playerHeight)
            .hardShadowDraw(Corners.miniPlayer, CurrentTokens.hardShadowTile)
            .senjaGlow(RoundedCornerShape(Corners.miniPlayer), active = isPlaying)
            .clip(RoundedCornerShape(Corners.miniPlayer))
            // SENYAP: batang tinta penuh (tanpa kapsul panggung) + teks
            // warna latar; selebihnya kapsul StageBg + teks putih.
            .background(if (CurrentTokens.flatNoCards) CurrentTokens.text else StageBg)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onHorizontalDrag = { _, dx -> dragX += dx },
                    onDragEnd = {
                        when {
                            dragX < -100f -> {
                                haptic(HapticKind.Tap)
                                onNext()
                            }
                            dragX > 100f -> {
                                haptic(HapticKind.Tap)
                                onPrevious()
                            }
                        }
                        dragX = 0f
                    },
                )
            }
            .clickable(
                onClickLabel = "Open song details",
                onClick = onClick,
            )
            .padding(horizontal = BottomCluster.playerPaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .graphicsLayer { rotationY = flipRotation % 360f }
                .onGloballyPositioned { onCoverBounds(it.boundsInWindow()) },
            contentAlignment = Alignment.Center,
        ) {
            SpinningCover(playing = isPlaying) {
                CoverArt(
                    track = track,
                    size = BottomCluster.playerCover,
                    corner = 100.dp,
                    letterSize = 16.sp,
                )
            }
        }
        Spacer(Modifier.width(BottomCluster.playerGap))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = BottomCluster.playerTitleSize,
                fontWeight = FontWeight.Bold,
                color = if (CurrentTokens.flatNoCards) CurrentTokens.background else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // v1.10.19: artis label/kosong → baris kedua dikosongkan.
                text = displayArtist(track.artist),
                fontSize = BottomCluster.playerArtistSize,
                color = (if (CurrentTokens.flatNoCards) CurrentTokens.background else Color.White).copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        androidx.compose.material3.IconButton(
            onClick = onToggleLike,
            modifier = Modifier.size(BottomCluster.playerControl),
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (CurrentTokens.flatNoCards) CurrentTokens.background else Color.White,
                modifier = Modifier.size(BottomCluster.playerLikeIcon),
            )
        }
        androidx.compose.material3.IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(BottomCluster.playerControl),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = if (CurrentTokens.flatNoCards) CurrentTokens.background else Color.White,
                modifier = Modifier.size(BottomCluster.playerControlIcon),
            )
        }
        // v1.0.1: Box biasa (pola yang sama dengan cakram play
        // Community Picks — terbukti tampil di HP ini, tema ini).
        // BouncyBox DILEPAS di sini: satu-satunya pembungkus yang
        // menaruh clip+background di dalam graphicsLayer scale, dan
        // tombolnya tidak tergambar sama sekali di screenshot user
        // (ruang 44dp ada, lingkaran + ikon hilang total) sementara
        // warna token-nya terbukti benar. Warna cakram dari pasangan
        // [playDiscPair] (kontras terhadap kapsul gelap di ke-14
        // varian). Ripple + getar dipertahankan.
        Box(
            Modifier
                .size(BottomCluster.playerAction)
                .clip(CircleShape)
                .background(CurrentTokens.playDiscFill)
                .clickable(
                    onClickLabel = if (isPlaying) "Pause" else "Play",
                    onClick = {
                        haptic(HapticKind.Tap)
                        onToggle()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = CurrentTokens.playDiscTint,
                modifier = Modifier.size(BottomCluster.playerPlayIcon),
            )
        }
        androidx.compose.material3.IconButton(
            onClick = onNext,
            modifier = Modifier.size(BottomCluster.playerControl),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = if (CurrentTokens.flatNoCards) CurrentTokens.background else Color.White,
                modifier = Modifier.size(BottomCluster.playerControlIcon),
            )
        }
    }
}
