package com.sonara.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaRouter
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import com.sonara.app.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.sonara.app.data.JamControlAction
import com.sonara.app.data.JamQueueItem
import com.sonara.app.data.JamRole
import com.sonara.app.data.occurrenceKeys
import com.sonara.app.data.selectionKey
import com.sonara.app.data.toTrack
import com.sonara.app.data.AppThemeId
import com.sonara.app.data.AppThemeMode
import com.sonara.app.data.AppThemeSelection
import com.sonara.app.data.AudioDevice
import com.sonara.app.data.AudioOutputPolicy
import com.sonara.app.data.AudioRoute
import com.sonara.app.data.CustomRadius
import com.sonara.app.data.CustomThemeOverride
import com.sonara.app.data.CustomThemePolicy
import com.sonara.app.data.HomeLayout
import com.sonara.app.data.LayoutPolicy
import com.sonara.app.data.paletteFor
import com.sonara.app.data.AudioQuality
import com.sonara.app.data.DownloadManager
import com.sonara.app.data.Downloads
import com.sonara.app.data.SyncConfig
import com.sonara.app.data.LyricCard
import com.sonara.app.data.LocalStore
import com.sonara.app.data.ArtistApi
import com.sonara.app.data.Chapter
import com.sonara.app.data.ChaptersApi
import com.sonara.app.data.DetailMotionSpec
import com.sonara.app.data.LyricsApi
import com.sonara.app.data.LyricsVersionPolicy
import com.sonara.app.data.LyricsFollowState
import com.sonara.app.data.LyricsOffsetPolicy
import com.sonara.app.data.LyricsFindPolicy
import com.sonara.app.data.LyricsEditCodec
import com.sonara.app.data.LyricsStyle
import com.sonara.app.data.SLEEP_TIMER_CHOICES_MINUTES
import com.sonara.app.data.PlaybackSpeedPolicy
import com.sonara.app.data.sleepRemainingLabel
import com.sonara.app.data.Source
import com.sonara.app.data.Track
import com.sonara.app.data.TrackDurations
import com.sonara.app.data.YoutubeMusicApi
import com.sonara.app.data.displayArtist
import com.sonara.app.data.formatDurationMs
import com.sonara.app.data.isUsableArtist
import com.sonara.app.data.shareTextFor
import com.sonara.app.player.PlayerManager
import com.sonara.app.ui.ArusBackButton
import com.sonara.app.ui.ArusWaveProgress
import com.sonara.app.ui.BigPlayButton
import com.sonara.app.ui.CoverArt
import com.sonara.app.ui.DragHandleDots
import com.sonara.app.ui.GetarField
import com.sonara.app.ui.HapticKind
import com.sonara.app.ui.SonaraState
import com.sonara.app.ui.MorphPlayPauseIcon
import com.sonara.app.ui.PrimaryButton
import com.sonara.app.ui.ReorderableColumn
import com.sonara.app.ui.SecondaryButton
import com.sonara.app.ui.StaggerIn
import com.sonara.app.ui.sonaraTextButtonColors
import com.sonara.app.ui.prefetchArtwork
import com.sonara.app.ui.rememberDetailPanelColors
import com.sonara.app.ui.rememberDominantColor
import com.sonara.app.ui.rememberWavePalette
import com.sonara.app.ui.rememberHaptic
import com.sonara.app.ui.rememberReducedMotion
import com.sonara.app.ui.hardShadowDraw
import com.sonara.app.ui.senjaGlow
import com.sonara.app.ui.OrbitRingOverlay
import com.sonara.app.ui.theme.Corners
import com.sonara.app.ui.theme.Density
import com.sonara.app.ui.theme.displayCased
import com.sonara.app.ui.theme.themeEasing
import com.sonara.app.ui.theme.CurrentTokens
import com.sonara.app.ui.theme.AccentFill
import com.sonara.app.ui.theme.BurntOrange
import com.sonara.app.ui.theme.DetailAccent
import com.sonara.app.ui.theme.OnAccent
import com.sonara.app.ui.theme.DetailBgFallback
import com.sonara.app.ui.theme.DetailLyricsCardBg
import com.sonara.app.ui.theme.DetailMiniHeaderBg
import com.sonara.app.ui.theme.DetailRelatedPanelBg
import com.sonara.app.ui.theme.DetailTextPrimary
import com.sonara.app.ui.theme.DetailTextSecondary
import com.sonara.app.ui.theme.Motion
import com.sonara.app.ui.theme.Sizes
import com.sonara.app.ui.theme.Type
import com.sonara.app.ui.theme.InkBlack
import com.sonara.app.ui.theme.LyricsFontFamily
import com.sonara.app.ui.theme.PaperOutline
import com.sonara.app.ui.theme.SegmentBg
import com.sonara.app.ui.theme.ThemeError
import com.sonara.app.ui.theme.WaveDetailProgress
import com.sonara.app.ui.theme.WaveDetailTrack
import com.sonara.app.ui.theme.WaveRed
import com.sonara.app.ui.showToast
import com.sonara.app.ui.SelectionActionBar
import com.sonara.app.ui.rememberSelectionState
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Halaman kategori Settings (v1.10.7): navigasi dua tingkat —
 *  halaman utama hanya daftar kategori; kontrol tiap kategori
 *  ada di halaman kategorinya sendiri. */
private enum class SettingsPage(val title: String) {
    ACCOUNT("Account"),
    APPEARANCE("Appearance"),
    PLAYBACK("Playback"),
    DOWNLOADS("Downloads & Storage"),
    ABOUT("About"),
}

/** Nama tampil tema (proper name — dipertahankan di semua bahasa UI). */
/** v1.10.35: label versi runtime dari PackageManager — layar About
 *  sebelumnya menampilkan string hardcode "v1.10.26 (37)" sejak 10.26
 *  karena build.sh tidak pernah menulis ulang string itu, sehingga
 *  laporan screenshot tidak bisa dipakai untuk memastikan versi
 *  build yang terpasang. Pola yang sama dengan CrashReportStore. */
private fun appVersionLabel(context: Context): String = try {
    @Suppress("DEPRECATION")
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    @Suppress("DEPRECATION")
    "SONARA v${info.versionName ?: "?"} (${info.versionCode})"
} catch (_: Exception) {
    "SONARA"
}

private fun themeDisplayName(theme: AppThemeId): String = when (theme) {
    AppThemeId.ARUS -> "Arus"
    AppThemeId.SENJA -> "Senja Gelap"
    AppThemeId.BLOK -> "Blok"
    AppThemeId.GETAR -> "Getar"
    AppThemeId.RASI -> "Rasi"
    AppThemeId.PASAR -> "Pasar"
    AppThemeId.SENYAP -> "Senyap"
}

private fun themeDescription(theme: AppThemeId): String = when (theme) {
    AppThemeId.ARUS -> "Warm paper and burnt orange."
    AppThemeId.SENJA -> "Dusk plum and warm amber."
    AppThemeId.BLOK -> "Bold color blocks and chunky borders."
    AppThemeId.GETAR -> "A living field, seeded per song."
    AppThemeId.RASI -> "A star map: orbit rings and constellation lines."
    AppThemeId.PASAR -> "Night-market ticket stubs and stamps."
    AppThemeId.SENYAP -> "Silence: no cards, only type and hairlines."
}

@Composable
fun SettingsScreen(
    state: SonaraState,
    onBack: () -> Unit,
    onShowCrash: () -> Unit,
    themeSelection: AppThemeSelection,
    onThemeChange: (AppThemeSelection) -> Unit,
    /** v1.0.7: tema kustom aktif (null = mati) + penulisannya. */
    customTheme: CustomThemeOverride? = null,
    onCustomThemeChange: (CustomThemeOverride?) -> Unit = {},
    /** v1.0.7: halaman awal dari command palette ("playback"/null). */
    startPage: String? = null,
    onStartPageConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val versionLabel = remember { appVersionLabel(context) }
    val scope = rememberCoroutineScope()
    var showAbout by remember { mutableStateOf(false) }
    var crossfade by remember { mutableStateOf(state.store.crossfadeSeconds) }
    var gapless by remember { mutableStateOf(state.store.gapless) }
    // v1.0.3: lewati bagian sponsor (SponsorBlock) — default nyala.
    var skipSponsored by remember { mutableStateOf(state.store.skipSponsoredSegments) }
    // v1.0.4: offset timing lirik global — default 0 (tidak geser).
    var lyricsOffsetGlobal by remember { mutableStateOf(state.store.globalLyricsOffsetMs) }
    var quality by remember { mutableStateOf(state.store.audioQuality) }
    val syncState by state.sync.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteDownloadsConfirm by remember { mutableStateOf(false) }
    // v1.10.7: null = daftar kategori; non-null = halaman kategori.
    // v1.0.7: halaman awal bisa diminta command palette.
    var settingsPage by remember {
        mutableStateOf<SettingsPage?>(
            if (startPage == "playback") SettingsPage.PLAYBACK else null,
        )
    }
    LaunchedEffect(startPage) {
        if (startPage != null) onStartPageConsumed()
    }
    BackHandler(enabled = settingsPage != null) { settingsPage = null }

    fun toast(msg: String) = showToast(context, msg)

    // Sign-in launcher — dipindah dari kartu AccountSyncSection yang
    // dihapus (v1.10.6); tanpa kartu ini login tidak bisa dijangkau.
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        state.sync.handleGoogleSignInResult(result.data, ::toast)
    }

    // Sleep timer (nyata): pilihan menit + jam berakhir yang ditampilkan.
    var sleepMinutes by remember { mutableStateOf(0) }
    val sleepEndsAt by state.player.sleepTimerEndsAtMs.collectAsState()
    // v1.0.5: mode berhenti-di-akhir-lagu (mesin sleep timer yang sama).
    val sleepEndOfTrack by state.player.sleepEndOfTrack.collectAsState()
    // v1.0.5: kecepatan default (dialog yang sama dengan detail).
    var showSpeedDialog by remember { mutableStateOf(false) }
    val defaultRate by state.player.userPlaybackRate.collectAsState()
    // v1.0.7: layout Beranda/Koleksi + draf tema kustom + dialog output.
    var layoutChoice by remember { mutableStateOf(LayoutPolicy.fromStorage(state.store.homeLayout)) }
    var customDraft by remember(customTheme) {
        mutableStateOf(customTheme ?: CustomThemeOverride.DEFAULT)
    }
    var showAudioOutput by remember { mutableStateOf(false) }

    // Storage (nyata): jumlah + ukuran unduhan, dihitung dari file.
    var downloadCount by remember { mutableStateOf(0) }
    var downloadBytes by remember { mutableStateOf(0L) }
    fun refreshStorage() {
        scope.launch {
            val entries = withContext(Dispatchers.IO) {
                Downloads(context, state.store.audioQuality).listEntries()
            }
            downloadCount = entries.size
            downloadBytes = entries.sumOf { it.bytes }
        }
    }
    LaunchedEffect(Unit) { refreshStorage() }

    Column(Modifier.fillMaxSize()) {
        // v1.10.1: tombol Back di kiri atas, sebaris dengan judul.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArusBackButton(onClick = { if (settingsPage != null) settingsPage = null else onBack() })
            Spacer(Modifier.width(8.dp))
            Text(
                settingsPage?.title ?: "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
        }


        if (settingsPage == null) {
            // Daftar kategori — hanya daftar, tanpa kontrol panjang.
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                SettingsCategoryRow(
                    icon = Icons.Filled.Person,
                    title = "Account",
                    subtitle = if (syncState.signedIn) syncState.email else "Not signed in",
                    onClick = { settingsPage = SettingsPage.ACCOUNT },
                )
                SettingsCategoryRow(
                    icon = Icons.Filled.Palette,
                    title = "Appearance",
                    subtitle = "${themeDisplayName(themeSelection.theme)} • " +
                        if (themeSelection.mode == AppThemeMode.DARK) "Dark" else "Light",
                    onClick = { settingsPage = SettingsPage.APPEARANCE },
                )
                SettingsCategoryRow(
                    icon = Icons.Filled.MusicNote,
                    title = "Playback",
                    subtitle = "Audio quality, crossfade, gapless, sleep timer",
                    onClick = { settingsPage = SettingsPage.PLAYBACK },
                )
                SettingsCategoryRow(
                    icon = Icons.Filled.Download,
                    title = "Downloads & Storage",
                    subtitle = "$downloadCount song${if (downloadCount == 1) "" else "s"} downloaded • " +
                        "${(downloadBytes + 500_000L) / 1_000_000L} MB",
                    onClick = { settingsPage = SettingsPage.DOWNLOADS },
                )
                SettingsCategoryRow(
                    icon = Icons.Filled.Album,
                    title = "About",
                    subtitle = versionLabel,
                    onClick = { settingsPage = SettingsPage.ABOUT },
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
        // ---- Account (v1.10.6): baris biasa, BUKAN kartu sinkron.
        // Kartu AccountSyncSection dihapus dari semua page; fungsi
        // Sign out + Delete cloud data tetap di sini sebagai baris.
                if (settingsPage == SettingsPage.ACCOUNT) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    if (syncState.signedIn && syncState.displayName.isNotEmpty()) {
                        syncState.displayName
                    } else if (syncState.signedIn) {
                        "Google Account"
                    } else {
                        "Not signed in"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (syncState.signedIn) {
                        syncState.email
                    } else {
                        "Sign in to save playlists, liked songs, and history in the cloud."
                    },
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (!syncState.signedIn) {
                    SettingsRow(
                        title = "Sign in with Google",
                        subtitle = "Sync playlists, liked songs, and history across devices.",
                        onClick = {
                            if (!SyncConfig.isConfigured) {
                                toast("Cloud sync isn't configured in this app.")
                            } else {
                                val intent = state.sync.googleSignInIntent()
                                if (intent != null) signInLauncher.launch(intent)
                                else toast("Google sign-in isn't available on this device.")
                            }
                        },
                    )
                } else {
                    SettingsRow(
                        title = "Sign out",
                        subtitle = "Data on this device stays.",
                        onClick = {
                            state.sync.signOut()
                            toast("Signed out. Data on this device stays.")
                        },
                    )
                    SettingsRow(
                        title = "Delete my cloud data",
                        subtitle = "Permanently removes cloud playlists, likes, and history.",
                        destructive = true,
                        onClick = { showDeleteConfirm = true },
                    )
                }
                Text(
                    "Sync runs automatically every minute while the app is open, " +
                        "and after every change you make. No manual sync needed.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
                }

        // ---- Appearance (v1.10.26, 7 tema sejak v1.10.37): theme switcher — 7 tema x 2 mode.
        // Pilihan langsung diterapkan ke seluruh app (state di root)
        // dan disimpan di prefs; tanpa restart.
                if (settingsPage == SettingsPage.APPEARANCE) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "Each theme keeps its own look in light and dark mode.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                for (theme in AppThemeId.entries) {
                    val selected = themeSelection.theme == theme
                    val preview = paletteFor(theme, themeSelection.mode)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .hardShadowDraw(
                                Corners.card,
                                CurrentTokens.hardShadowChip,
                            )
                            .clip(RoundedCornerShape(Corners.card))
                            // v1.10.40: kartu TAK TERPILIH berlatar
                            // surface OPAK (dulu Transparent — rect
                            // bayangan hardShadow warna tokens.text
                            // tembus sehingga nama tak terbaca 1.00:1).
                            .background(if (selected) SegmentBg else MaterialTheme.colorScheme.surface)
                            .border(
                                CurrentTokens.cardBorderWidth,
                                if (selected) AccentFill else PaperOutline,
                                RoundedCornerShape(Corners.card),
                            )
                            .clickable { onThemeChange(themeSelection.copy(theme = theme)) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        // Swatch: background / surfaceVariant / accentFill.
                        Row {
                            for (swatch in listOf(
                                Color(preview.background),
                                Color(preview.surfaceVariant),
                                Color(preview.accentFill),
                            )) {
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                        .border(1.dp, PaperOutline, CircleShape),
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                themeDisplayName(theme),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                // v1.10.40: warna eksplisit — dulu tanpa
                                // warna (mewarisi tokens.text) sehingga
                                // 1.00:1 di atas bayangan kartu.
                                color = InkBlack,
                            )
                            Text(
                                themeDescription(theme),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                // v1.10.40: tinta teks (dulu AccentFill
                                // 1.41:1 di SENJA Light) — >=12:1 di
                                // semua varian di atas surfaceVariant.
                                tint = InkBlack,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    for (mode in AppThemeMode.entries) {
                        val selected = themeSelection.mode == mode
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(Corners.button))
                                .background(if (selected) AccentFill else SegmentBg)
                                .clickable { onThemeChange(themeSelection.copy(mode = mode)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (mode == AppThemeMode.DARK) "Dark" else "Light",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) OnAccent else InkBlack,
                            )
                        }
                        if (mode == AppThemeMode.LIGHT) Spacer(Modifier.width(8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Applies instantly across the whole app.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // ---- Layout Beranda + Koleksi (v1.0.7) ----
                Spacer(Modifier.height(16.dp))
                Text("Layout", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "How shelves look on Home and in Library. Song rows stay the same.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                for (layout in HomeLayout.entries) {
                    val selected = layoutChoice == layout
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(Corners.card))
                            .background(if (selected) SegmentBg else Color.Transparent)
                            .border(
                                CurrentTokens.cardBorderWidth,
                                if (selected) AccentFill else PaperOutline,
                                RoundedCornerShape(Corners.card),
                            )
                            .clickable {
                                layoutChoice = layout
                                state.store.homeLayout = LayoutPolicy.storageKey(layout)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                LayoutPolicy.displayName(layout),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkBlack,
                            )
                            Text(
                                LayoutPolicy.description(layout),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = InkBlack,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                // ---- Aksen + radius kustom (v1.0.7) ----
                Spacer(Modifier.height(8.dp))
                Text("Custom accent", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "Your accent color and corner shape, on top of the selected theme. " +
                        "Every combination is checked so text stays readable.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Use a custom accent", fontSize = 15.sp)
                        Text(
                            if (customDraft.enabled) "On — applied across the app."
                            else "Off — the theme's own colors apply.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = customDraft.enabled,
                        onCheckedChange = { on ->
                            customDraft = customDraft.copy(enabled = on)
                            onCustomThemeChange(customDraft)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnAccent,
                            checkedTrackColor = BurntOrange,
                            checkedBorderColor = Color.Transparent,
                        ),
                    )
                }
                if (customDraft.enabled) {
                    val basePalette = paletteFor(themeSelection.theme, themeSelection.mode)
                    val resolved = CustomThemePolicy.resolve(basePalette, customDraft)
                    // Pratinjau jujur: warna HASIL resolve (bisa lebih
                    // gelap dari swatch bila kontrasnya menuntut).
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Corners.card))
                            .background(Color(resolved.accentFill))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Preview",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(resolved.onAccent),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "Sample text",
                            fontSize = 13.sp,
                            color = Color(resolved.onAccent),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (swatch in CustomThemePolicy.ACCENT_SWATCHES) {
                            val selected = customDraft.accent == swatch
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch))
                                    .border(
                                        2.dp,
                                        if (selected) InkBlack else PaperOutline,
                                        CircleShape,
                                    )
                                    .clickable {
                                        customDraft = customDraft.copy(accent = swatch)
                                        onCustomThemeChange(customDraft)
                                    },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Corners", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (radius in CustomRadius.entries) {
                            val selected = customDraft.radius == radius
                            Box(
                                Modifier
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(Corners.button))
                                    .background(if (selected) AccentFill else SegmentBg)
                                    .clickable {
                                        customDraft = customDraft.copy(radius = radius)
                                        onCustomThemeChange(customDraft)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    when (radius) {
                                        CustomRadius.SHARP -> "Sharp"
                                        CustomRadius.BALANCED -> "Balanced"
                                        CustomRadius.ROUND -> "Round"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) OnAccent else InkBlack,
                                )
                            }
                        }
                    }
                }
            }
                }

        // ---- Playback ----
                if (settingsPage == SettingsPage.PLAYBACK) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Audio quality (YouTube Music):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                for (q in AudioQuality.entries) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                quality = q
                                state.store.audioQuality = q
                            },
                    ) {
                        RadioButton(
                            selected = quality == q,
                            onClick = {
                                quality = q
                                state.store.audioQuality = q
                            },
                            // v1.10.40: dot aksen teks (default primary/
                            // accentFill pucat di SENJA/GETAR Light).
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BurntOrange,
                            ),
                        )
                        Text(q.label, fontSize = 14.sp)
                    }
                }
                Text(
                    "Data saver picks a smaller stream for playback and downloads; " +
                        "Best picks the highest-quality stream.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Crossfade: ${if (crossfade == 0) "off" else "$crossfade seconds"}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    "Volume fades out at the end of a song, then the next one comes in. " +
                        "Not a two-player crossfade.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = crossfade.toFloat(),
                    onValueChange = {
                        crossfade = it.toInt()
                        state.store.crossfadeSeconds = it.toInt()
                    },
                    valueRange = 0f..12f,
                    steps = 11,
                    modifier = Modifier.fillMaxWidth(),
                    // v1.10.40: trek/jempol aksen teks (default primary/
                    // accentFill pucat di SENJA/GETAR Light).
                    colors = SliderDefaults.colors(
                        thumbColor = BurntOrange,
                        activeTrackColor = BurntOrange,
                    ),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Gapless (no gap between songs)", Modifier.weight(1f), fontSize = 15.sp)
                    Switch(
                        checked = gapless,
                        onCheckedChange = {
                            gapless = it
                            state.store.gapless = it
                        },
                        // v1.10.40: trek ON aksen teks + jempol OnAccent
                        // (default primary/accentFill pucat di
                        // SENJA/GETAR Light).
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnAccent,
                            checkedTrackColor = BurntOrange,
                            checkedBorderColor = Color.Transparent,
                        ),
                    )
                }
                // v1.0.3: lewati bagian sponsor/intro/outro yang
                // dilaporkan komunitas (SponsorBlock) + tandai di
                // seek bar. Mati = lagu diputar utuh, tanpa lompatan.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Skip sponsored parts", fontSize = 15.sp)
                        Text(
                            "Skips sponsor, intro, and outro parts reported by listeners. An Undo button appears when a part is skipped.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = skipSponsored,
                        onCheckedChange = {
                            skipSponsored = it
                            state.store.skipSponsoredSegments = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnAccent,
                            checkedTrackColor = BurntOrange,
                            checkedBorderColor = Color.Transparent,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                // v1.0.4: offset timing lirik global — geser semua lirik
                // sinkron; offset per-lagu (di layar detail) ditambah
                // di atas nilai ini. 0 = tidak digeser.
                Text(
                    "Lyrics timing offset: ${LyricsOffsetPolicy.offsetLabel(lyricsOffsetGlobal)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    "Shifts synced lyrics earlier or later for all songs. " +
                        "Per-song offsets set on the detail screen add to this.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = lyricsOffsetGlobal / 1000f,
                    onValueChange = {
                        lyricsOffsetGlobal = (it * 1000).toLong()
                        state.store.globalLyricsOffsetMs = (it * 1000).toLong()
                    },
                    valueRange = -10f..10f,
                    steps = 39,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = BurntOrange,
                        activeTrackColor = BurntOrange,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text("Sleep timer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "Stops playback when the timer ends, even if the song changes.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    for (minutes in listOf(0, 15, 30, 60)) {
                        val selected = sleepMinutes == minutes && !sleepEndOfTrack
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(Corners.button))
                                .background(if (selected) AccentFill else SegmentBg)
                                .clickable {
                                    sleepMinutes = minutes
                                    state.player.setSleepTimer(minutes)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (minutes == 0) "Off" else "$minutes min",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) OnAccent else InkBlack,
                            )
                        }
                        if (minutes != 60) Spacer(Modifier.width(8.dp))
                    }
                }
                // v1.0.5: berhenti di akhir lagu/episode saat ini.
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(Corners.button))
                            .background(if (sleepEndOfTrack) AccentFill else SegmentBg)
                            .clickable {
                                state.player.setSleepTimerEndOfTrack()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "End of track",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sleepEndOfTrack) OnAccent else InkBlack,
                        )
                    }
                }
                if (sleepEndsAt != null) {
                    Spacer(Modifier.height(6.dp))
                    val endTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(sleepEndsAt!!))
                    Text(
                        "Sleep timer on — playback stops at $endTime.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (sleepEndOfTrack) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sleep timer on — playback stops at the end of the current track.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // v1.0.5: kecepatan default — dialog yang sama dengan
                // layar detail; pilihan per episode diingat terpisah.
                Spacer(Modifier.height(12.dp))
                SettingsRow(
                    title = "Playback speed",
                    subtitle = "Default speed for new tracks: ${PlaybackSpeedPolicy.formatSpeed(defaultRate)}. " +
                        "Long episodes remember their own speed.",
                    onClick = { showSpeedDialog = true },
                )
                // v1.0.7: output audio — perangkat nyata dari
                // AudioManager + rute sistem; tanpa Cast (lihat BUILD).
                SettingsRow(
                    title = "Audio output",
                    subtitle = "See connected speakers and headphones, and ask the system to switch.",
                    onClick = { showAudioOutput = true },
                )
            }
                }

        // ---- Downloads & Storage (nyata) ----
                if (settingsPage == SettingsPage.DOWNLOADS) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "$downloadCount song${if (downloadCount == 1) "" else "s"} downloaded • " +
                        "${(downloadBytes + 500_000L) / 1_000_000L} MB",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsRow(
                    title = "Delete all downloads",
                    subtitle = "Removes downloaded song files from this device.",
                    onClick = { showDeleteDownloadsConfirm = true },
                )
                SettingsRow(
                    title = "Clear image cache",
                    subtitle = "Removes cached artwork. Artwork reloads when needed.",
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val loader = coil.Coil.imageLoader(context)
                                loader.memoryCache?.clear()
                                loader.diskCache?.clear()
                            }
                            toast("Image cache cleared.")
                        }
                    },
                )
            }
                }

        // ---- About & Limits ----
                if (settingsPage == SettingsPage.ABOUT) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                // v1.10.13: lockup logo "Dua riak, satu sumber" (pilihan
                // user, FINAL) — mark saja, tanpa teks/wordmark baru.
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_ripples),
                    contentDescription = "SONARA logo",
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showAbout = !showAbout },
                    // v1.10.40: teks aksen (default primary/accentFill).
                    colors = sonaraTextButtonColors(),
                ) {
                    Text(if (showAbout) "Hide" else "Read the v1.10.11 limits")
                }
                if (showAbout) {
                    Text(
                        "• Google sign-in (optional) saves your playlists, " +
                            "liked songs, and play history to this project's Firebase " +
                            "cloud, so they work across " +
                            "devices. Without sign-in, all data is only stored " +
                            "on this device. Sync uses Firebase REST " +
                            "(no SDK); tokens are stored encrypted. Sync runs " +
                            "automatically every minute while the app is open and " +
                            "after every change — Android doesn't allow 1-minute " +
                            "periodic work in the background.\n" +
                            "• SONARA uses only YouTube Music: search via " +
                            "the YTM web API, playback via direct streams (visionOS " +
                            "player API, Opus) in ExoPlayer. Guest mode (signed-out) — " +
                            "unofficial and may change; no YTM library while signed in yet.\n" +
                            "• Jamming Together syncs playback between phones via " +
                            "Firebase Realtime Database (same project). The host " +
                            "controls playback; guests follow. Real two-phone sync " +
                            "can only be verified on two real phones.\n" +
                            "• Lyrics from LRCLIB (free, no key; synced, or plain " +
                            "if only untimed text exists) plus built-in YouTube " +
                            "Music lyrics; if no source has lyrics, the app shows " +
                            "'unavailable' — lyrics are never made up.\n" +
                            "• No paywall/DRM bypass.\n" +
                            "• The YouTube (embed) player is only a fallback if the direct stream fails.\n" +
                            "• Song detail is audio-only (video mode was removed " +
                            "in v1.10.18). The History pill shows the song's " +
                            "history from its video description and the artist " +
                            "page — verbatim, never made up.\n" +
                            "• Comments are read-only and come from YouTube " +
                            "Music: avatar, @handle, time, verbatim text, and " +
                            "the abbreviated like counts YouTube shows. No " +
                            "total count is shown, and posting, liking, or " +
                            "replying needs sign-in, so they aren't offered.\n" +
                            "• Garis Arus wave colors come from the playing song's " +
                            "artwork palette (a proxy for the artwork's mood — no " +
                            "audio analysis). Without artwork, the wave falls back " +
                            "to red + light green.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onShowCrash, modifier = Modifier.height(48.dp)) {
                    Text("View Crash Report")
                }
                Spacer(Modifier.height(8.dp))
                Text("$versionLabel • com.sonara.app", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete cloud data?") },
            text = {
                Text(
                    "All playlists, liked songs, and history in the cloud " +
                        "will be permanently deleted. Data on this device will NOT " +
                        "be deleted, and you'll be signed out.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        state.sync.deleteCloudData(::toast)
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    colors = sonaraTextButtonColors(),
                ) { Text("Cancel") }
            },
        )
    }
    if (showDeleteDownloadsConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteDownloadsConfirm = false },
            title = { Text("Delete all downloads?") },
            text = { Text("Downloaded song files will be removed from this device. Your playlists and liked songs stay.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDownloadsConfirm = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                Downloads(context, state.store.audioQuality).deleteAll()
                            }
                            // v1.0.6: hentikan unduhan aktif + hapus
                            // catatan tugas + berkas .part (berkas .webm
                            // sudah dihapus deleteAll di atas).
                            DownloadManager.resetAll(context)
                            refreshStorage()
                            toast("Downloads deleted.")
                        }
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDownloadsConfirm = false },
                    colors = sonaraTextButtonColors(),
                ) { Text("Cancel") }
            },
        )
    }

    // v1.0.7: dialog output audio — perangkat nyata, bukan Cast.
    if (showAudioOutput) {
        AudioOutputDialog(
            context = context,
            onDismiss = { showAudioOutput = false },
        )
    }

    // v1.0.5: dialog kecepatan default (pilihan yang sama dengan detail).
    if (showSpeedDialog) {
        com.sonara.app.ui.SpeedDialog(
            currentRate = defaultRate,
            onDismiss = { showSpeedDialog = false },
            onSelect = { rate ->
                state.player.setUserPlaybackRate(rate)
                showSpeedDialog = false
            },
        )
    }
}

/**
 * Dialog output audio (v1.0.7) — HANYA perangkat/rute yang benar-benar
 * dilaporkan platform. Daftar perangkat dibaca dari AudioManager
 * (sink saja, deduplikasi nama+tipe); rute dibaca best-effort dari
 * framework MediaRouter. Tap perangkat yang punya rute yang cocok →
 * MediaRouter.selectRoute; tanpa rute → toast jujur + handoff ke
 * pengaturan Bluetooth sistem. TIDAK ada Cast: Cast SDK tidak
 * ditambahkan (lihat BUILD-v1.0.7.md); tombol Cast tidak pernah
 * ditampilkan.
 */
@Composable
private fun AudioOutputDialog(
    context: Context,
    onDismiss: () -> Unit,
) {
    var devices by remember { mutableStateOf<List<AudioDevice>>(emptyList()) }
    var routes by remember { mutableStateOf<List<AudioRoute>>(emptyList()) }
    var routeInfos by remember {
        mutableStateOf<Map<String, MediaRouter.RouteInfo>>(emptyMap())
    }
    LaunchedEffect(Unit) {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val rawDevices = runCatching {
            audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
        }.getOrDefault(emptyArray())
        val mapped = rawDevices.mapIndexed { index, info ->
            AudioDevice(
                id = index,
                type = info.type,
                productName = info.productName?.toString().orEmpty(),
                isSink = info.isSink,
            )
        }
        devices = AudioOutputPolicy.listDevices(mapped)
        runCatching {
            val router =
                context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
            if (router != null) {
                val selectedName = runCatching {
                    router.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
                        ?.name?.toString()
                }.getOrNull()
                val found = mutableListOf<AudioRoute>()
                val infos = HashMap<String, MediaRouter.RouteInfo>()
                for (i in 0 until router.routeCount) {
                    val info = router.getRouteAt(i) ?: continue
                    val name = runCatching { info.name?.toString() }.getOrNull()
                        ?: continue
                    val id = "route:$i"
                    infos[id] = info
                    found += AudioRoute(
                        id = id,
                        name = name,
                        selected = selectedName != null && selectedName == name,
                    )
                }
                routes = found
                routeInfos = infos
            }
        }
    }
    val toast = { msg: String ->
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio output") },
        text = {
            Column {
                Text(
                    "Devices Android reports right now. " +
                        "Tap one to ask the system to switch to it.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (devices.isEmpty()) {
                    Text(
                        "No output device reported.",
                        fontSize = 14.sp,
                    )
                } else {
                    for (device in devices) {
                        val label = AudioOutputPolicy.deviceLabel(device)
                        val route = AudioOutputPolicy.matchRoute(device, routes)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    val info = route?.let { routeInfos[it.id] }
                                    if (info != null) {
                                        runCatching {
                                            val router = context.getSystemService(
                                                Context.MEDIA_ROUTER_SERVICE,
                                            ) as? MediaRouter
                                            router?.selectRoute(
                                                MediaRouter.ROUTE_TYPE_LIVE_AUDIO,
                                                info,
                                            )
                                        }.onSuccess {
                                            toast("Switched to ${route.name}.")
                                            onDismiss()
                                        }.onFailure {
                                            toast("Could not switch output.")
                                        }
                                    } else {
                                        toast(
                                            "Android did not offer a switchable " +
                                                "route for this device.",
                                        )
                                    }
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    audioDeviceDetail(device, label),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (route?.selected == true) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = InkBlack,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Chromecast is not built in: casting needs the Cast SDK " +
                        "and a separate remote-playback session, which this " +
                        "build does not include.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                },
                colors = sonaraTextButtonColors(),
            ) { Text("Bluetooth settings") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = sonaraTextButtonColors(),
            ) { Text("Close") }
        },
    )
}

/** Subjudul perangkat output: nama produk bila label generik, else jenis koneksi. */
private fun audioDeviceDetail(device: AudioDevice, label: String): String {
    val name = device.productName.trim()
    if (name.isNotEmpty() && name != label) return name
    return when (device.type) {
        AudioOutputPolicy.TYPE_BLUETOOTH_A2DP,
        AudioOutputPolicy.TYPE_BLUETOOTH_SCO,
        AudioOutputPolicy.TYPE_BLE_HEADSET,
        AudioOutputPolicy.TYPE_BLE_SPEAKER,
        -> "Bluetooth audio"
        AudioOutputPolicy.TYPE_BUILTIN_SPEAKER,
        AudioOutputPolicy.TYPE_BUILTIN_SPEAKER_SAFE,
        -> "Built-in speaker"
        AudioOutputPolicy.TYPE_BUILTIN_EARPIECE -> "Built-in earpiece"
        AudioOutputPolicy.TYPE_WIRED_HEADSET,
        AudioOutputPolicy.TYPE_WIRED_HEADPHONES,
        -> "Wired audio"
        AudioOutputPolicy.TYPE_HDMI -> "HDMI"
        AudioOutputPolicy.TYPE_USB_DEVICE,
        AudioOutputPolicy.TYPE_USB_HEADSET,
        -> "USB audio"
        else -> "Audio output"
    }
}

/** Baris kategori Settings (v1.10.7): ikon ARUS + nama + chevron,
 *  target sentuh ≥48dp. Halaman utama hanya berisi baris-baris ini. */
@Composable
private fun SettingsCategoryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(Corners.large))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClickLabel = title,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SegmentBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BurntOrange, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Baris pengaturan biasa (v1.10.6): judul + subjudul, target sentuh ≥48dp. */
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val titleColor = if (destructive) ThemeError
        else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = titleColor),
                onClickLabel = title,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NowPlayingScreen(
    state: SonaraState,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: (String?) -> Unit,
    /** Buka profil artis (v1.10.18). Null = fallback cari nama artis. */
    onOpenArtist: ((String, String?) -> Unit)? = null,
    /** Buka halaman acara podcast dari judul acara episode (v1.0.5). */
    onOpenPodcastShow: ((String, String) -> Unit)? = null,
    /** True saat transisi hero berjalan — artwork disembunyikan karena overlay yang menampilkannya. */
    heroHidingArtwork: Boolean = false,
    /** Melaporkan posisi artwork (koordinat jendela) untuk target transisi hero. */
    onArtworkBounds: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    /** Undo swipe-hapus (v1.10.16): root menampilkan snackbar Undo. Null = swipe nonaktif. */
    onQueueItemRemoved: ((Track, Int) -> Unit)? = null,
) {
    val track by state.player.currentTrack.collectAsState()
    val isPlaying by state.player.isPlaying.collectAsState()
    val isBuffering by state.player.isBuffering.collectAsState()
    val positionMs by state.player.positionMs.collectAsState()
    val durationMs by state.player.durationMs.collectAsState()
    val shuffle by state.player.shuffle.collectAsState()
    val repeat by state.player.repeat.collectAsState()
    val playerError by state.player.error.collectAsState()
    val queue by state.player.queue.collectAsState()
    val currentIndex by state.player.currentIndex.collectAsState()
    // v1.0.3: radio (seksi terpisah di UP NEXT) + segmen SponsorBlock
    // (penanda wave) + undo lompatan terakhir (chip sementara).
    val radioQueue by state.player.radioQueue.collectAsState()
    val radioIndex by state.player.radioIndex.collectAsState()
    val radioActive by state.player.radioActive.collectAsState()
    val segments by state.player.segments.collectAsState()
    val skipUndo by state.player.skipUndo.collectAsState()
    val context = LocalContext.current
    // v1.10.6: tamu Jam tidak boleh mengontrol putar — semua kontrol
    // di bawah dibungkus guardedPlayback (toast, bukan aksi).
    // Like/save/komentar tetap aktif (aksi koleksi lokal, bukan putar).
    val guestLocked = rememberGuestLocked(state)
    val jamUiState by state.jam.uiState.collectAsState()
    // v1.10.42: identitas peran tamu (BUKAN guestLocked) — saat
    // kontrol bersama NYALA, tamu boleh MEMINTA kontrol lewat host;
    // guestLocked justru false dalam status itu.
    val isJamGuest = jamUiState.active && jamUiState.role == JamRole.GUEST
    /**
     * Gerbang kontrol playback: kontrol bersama MATI → toast
     * host-only (guestLocked, perilaku lama); NYALA → permintaan
     * kontrol ke host (pola jumpRequests), toast hanya bila manager
     * mengembalikan pesan. Di luar sesi: aksi lokal biasa.
     */
    fun guardedPlayback(
        action: JamControlAction,
        positionMs: Long = 0L,
        local: () -> Unit,
    ) {
        if (guestLocked) {
            jamHostOnlyToast(context)
            return
        }
        if (isJamGuest) {
            state.jam.requestPlaybackControl(action, positionMs) { msg ->
                if (msg.isNotEmpty()) showToast(context, msg)
            }
            return
        }
        local()
    }
    /**
     * Gerbang mode putar (shuffle/repeat): preferensi mode LOKAL,
     * bukan kontrol sesi — tamu TIDAK PERNAH mengubahnya, di kedua
     * status toggle kontrol bersama. Selalu toast untuk tamu.
     */
    fun guardedPlaybackMode(local: () -> Unit) {
        if (isJamGuest) jamHostOnlyToast(context) else local()
    }
    // v1.10.16: swipe-hapus UP NEXT — hapus dari antrean lokal lalu
    // serahkan ke root untuk snackbar Undo. Selama sesi Jam, panel
    // UP NEXT menampilkan antrean sesi (tanpa swipe) sehingga handler
    // ini tidak pernah terpanggil untuk antrean sesi.
    val swipeHandler: ((Int, Track) -> Unit)? = onQueueItemRemoved?.let { cb ->
        { idx: Int, tr: Track ->
            state.player.removeFromQueue(idx)
            cb(tr, idx)
        }
    }
    val density = LocalDensity.current
    val reducedMotion = rememberReducedMotion()
    val haptic = rememberHaptic()
    // v1.10.16: scope untuk berbagi kartu lirik (render di background).
    val lyricShareScope = rememberCoroutineScope()
    // v1.10.21: multi-select Up Next — Back keluar mode dulu.
    val detailSelection = rememberSelectionState()
    var detailBulkSave by remember { mutableStateOf<List<Track>?>(null) }
    LaunchedEffect(queue) {
        if (detailSelection.modeActive) detailSelection.exitMode()
    }
    BackHandler {
        if (detailSelection.modeActive) detailSelection.exitMode() else onBack()
    }

    // Landscape: artwork mengecil supaya judul + kontrol tetap muat.
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coverSize = if (isLandscape) 240.dp else Sizes.nowPlayingCover

    val t = track
    com.sonara.app.ui.SystemBarAppearance(darkBackground = true)
    // Background gelap GRADIEN dari warna dominan artwork (v1.7,
    // screenshot detail: atas #483807 untuk artwork gold → bawah
    // hampir hitam coklat), bertransisi halus 450ms saat ganti lagu.
    val bgColor = rememberDominantColor(t?.imageUrl)
    // v1.10.31: warna panel + mini-header turunan artwork (satu keluarga
    // dengan gradien stage); null = tanpa artwork → warna tetap lama.
    val detailPanelColors = rememberDetailPanelColors(t?.imageUrl)
    // v1.10.6: warna Garis Arus detail dari palet artwork lagu
    // (fallback merah + hijau muda bila tanpa artwork).
    val wavePalette = rememberWavePalette(t?.imageUrl)
    val bgTop by animateColorAsState(
        targetValue = bgColor,
        animationSpec = tween(Motion.slowMs),
        label = "bgTint",
    )
    val bgBottom = Color(
        red = (bgTop.red * 0.32f).coerceIn(0f, 1f),
        green = (bgTop.green * 0.32f).coerceIn(0f, 1f),
        blue = (bgTop.blue * 0.32f).coerceIn(0f, 1f),
    )

    // Tab detail: 0 = UP NEXT, 1 = LYRICS, 2 = RELATED.
    var detailTab by remember { mutableStateOf(0) }
    // v1.10.7: header COMPACT diseragamkan untuk ketiga tab.
    // Tampilan AWAL saat detail dibuka = tampilan penuh (artwork
    // besar + pill + gelombang + kontrol). Begitu pengguna mengetuk
    // tab mana pun, header menjadi mini-header dan TETAP mini saat
    // berpindah antar ketiga tab — termasuk kembali ke UP NEXT
    // (tidak boleh kembali ke tampilan penuh). Ketuk kartu
    // mini-header = kembali ke tampilan penuh (satu-satunya jalan
    // kembali). State ini bertahan antar lagu (seperti detailTab);
    // komposisi detail yang ditutup mengatur ulang ke tampilan penuh.
    var headerCompact by remember { mutableStateOf(false) }
    fun selectTab(i: Int) {
        detailTab = i
        headerCompact = true
    }
    // State scroll daftar UP NEXT mode mini-header (di-hoist supaya
    // posisi dipertahankan saat pindah tab, seperti tab lain).
    val upNextCompactListState = rememberLazyListState()
    // v1.10.18: History — diambil per lagu (deskripsi video +
    // About artis, teks verbatim dari API). Null = masih memuat;
    // kosong = tidak ada blok = empty state jujur di sheet.
    var historyContent by remember { mutableStateOf<ArtistApi.HistoryContent?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    LaunchedEffect(t?.id) {
        historyContent = null
        val track = t ?: return@LaunchedEffect
        if (track.source == Source.LOCAL) return@LaunchedEffect
        historyContent = withContext(Dispatchers.IO) {
            runCatching { ArtistApi.fetchHistory(state.ytmApi, track) }.getOrNull()
        }
    }
    // v1.0.5: Chapters — diambil per lagu HANYA bila episode podcast
    // atau konten panjang (durasi metadata ≥ 10 mnt) dan jalur YTM.
    // Respons /next signed-out membawa panel chapters untuk episode
    // (terbukti di probe); bila panel tidak ada → null → tab CHAPTERS
    // tidak tampil (jujur, tanpa panel palsu). Lirik tidak disentuh.
    var chapters by remember { mutableStateOf<List<Chapter>?>(null) }
    LaunchedEffect(t?.id) {
        chapters = null
        val track = t ?: return@LaunchedEffect
        val vid = track.youtubeVideoId ?: return@LaunchedEffect
        if (track.source != Source.YOUTUBE_MUSIC) return@LaunchedEffect
        val longForm = track.isPodcastEpisode || track.durationMs >= 600_000L
        if (!longForm) return@LaunchedEffect
        chapters = withContext(Dispatchers.IO) {
            state.ytmApi.fetchChapters(vid).getOrNull()?.takeIf { it.isNotEmpty() }
        }
    }
    // Tab CHAPTERS hanya ada bila chapters benar-benar termuat.
    // detailTab yang tersimpan bisa 3 dari lagu sebelumnya — jepit ke
    // 0..2 bila tab tidak ada, tanpa mengubah state tersimpan.
    val showChaptersTab = chapters != null
    val effectiveDetailTab = if (showChaptersTab) detailTab else detailTab.coerceIn(0, 2)
    var showSheet by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }
    // v1.10.44: sleep timer — dialog pilihan menit + pilihan terakhir
    // di layar ini; status hidup dibaca dari mesin yang sama dengan
    // Settings (sleepTimerEndsAtMs), bukan state lokal kedua.
    var showSleepTimer by remember { mutableStateOf(false) }
    var sleepChoiceMinutes by remember { mutableStateOf(0) }
    // v1.0.5: dialog kecepatan putar — nilai dari PlayerManager
    // (userPlaybackRate); disembunyikan saat sesi Jam aktif (drift
    // Jam yang memiliki rate saat itu).
    var showSpeedDialog by remember { mutableStateOf(false) }
    val userRate by state.player.userPlaybackRate.collectAsState()
    // v1.0.3: dialog info lagu (dari aksi "Track info" + tombol
    // "Details" di kotak error).
    var showTrackInfo by remember { mutableStateOf(false) }
    val sleepEndsAt by state.player.sleepTimerEndsAtMs.collectAsState()
    // v1.0.5: mode berhenti-di-akhir-lagu (mesin sleep timer yang sama).
    val sleepEndOfTrack by state.player.sleepEndOfTrack.collectAsState()
    LaunchedEffect(sleepEndsAt) {
        if (sleepEndsAt == null) sleepChoiceMinutes = 0
    }
    var likeTick by remember { mutableStateOf(0) }
    val liked = run { likeTick; t != null && state.store.isLiked(t.id) }
    val disliked = run { likeTick; t != null && state.store.isDisliked(t.id) }
    val savedToPlaylist = run {
        likeTick
        t != null && state.store.playlists().any { it.trackIds.contains(t.id) }
    }

    // v1.10.6: scroll state tab di-hoist ke level layar supaya
    // pindah tab MEMPERTAHANKAN posisi scroll (tidak reload/reset).
    // UP NEXT: reset ke atas hanya saat lagu berganti (LaunchedEffect
    // t.id di bawah). RELATED: ScrollState dioper ke RelatedPanel.
    val upNextListState = rememberLazyListState()
    val relatedScrollState = rememberScrollState()
    var detailIntroDone by remember { mutableStateOf(false) }
    LaunchedEffect(t?.id) {
        upNextListState.scrollToItem(0)
        upNextCompactListState.scrollToItem(0)
    }
    LaunchedEffect(Unit) {
        // Intro berjenjang pertama selesai setelah ~525ms (5 x 45ms
        // + 300ms); sesudah itu kembalinya ke tab ini instan.
        kotlinx.coroutines.delay(600)
        detailIntroDone = true
    }

    // Lirik — dimuat tiap ganti lagu (v1.10.1: LRCLIB varian +
    // polos + lirik bawaan YTM; TIDAK PERNAH dikarang).
    var lyricsResult by remember { mutableStateOf<LyricsApi.LyricsResult?>(null) }
    var lyricsTried by remember { mutableStateOf(false) }
    val lyricsListState = rememberLazyListState()
    // v1.10.19: follow-hold — sentuhan jari pada daftar lirik
    // menangguhkan follow (hold 30 detik selama jari menempel, lalu
    // 3 detik setelah lepas). Deteksi lewat event sentuh LANGSUNG di
    // LyricsPanel (pointerInput, tanpa consume — tidak mengganggu
    // gesture scroll), BUKAN lewat isScrollInProgress + flag boolean
    // 10.18: flag itu rapuh — scroll manual yang dimulai DI TENGAH
    // scroll programatik tidak pernah mempersenjatai hold (watcher
    // tidak melihat transisi), dan urutan flag-vs-settle tidak
    // dijamin. Event sentuh membedakan manual vs programatik secara
    // konstruksi: scroll programatik tidak pernah membangkitkannya.
    var lyricsFollowHoldUntilMs by remember { mutableStateOf(0L) }
    // Dipanggil LyricsPanel saat jari menyentuh / lepas dari daftar.
    val onLyricsTouchDown: () -> Unit = {
        lyricsFollowHoldUntilMs =
            System.currentTimeMillis() + LyricsFollowState.TOUCH_HOLD_CAP_MS
    }
    val onLyricsTouchUp: () -> Unit = {
        lyricsFollowHoldUntilMs = LyricsFollowState.holdUntilFrom(
            System.currentTimeMillis(),
        )
    }
    // v1.10.39: durasi TERVERIFIKASI untuk fetch lirik (lihat
    // LyricsVersionPolicy). Saat lagu berganti, t.durationMs biasanya
    // 0 (salinan feed — durasi hanya untuk tampil, bukan untuk putar)
    // dan durasi asli baru tiba lewat resolve stream / ExoPlayer.
    // Fetch pertama bersifat provisional; begitu durasi diketahui,
    // fetch diulang dengan durasi itu (needsFetch) supaya entri
    // versi-salah tidak diterima tanpa verifikasi. Efek lama yang
    // hanya berkunci t.id tidak pernah mengulang fetch — itulah akar
    // "banyak lirik gak singkron". durationMs player di sini aman
    // dipakai: startTrack mengesetnya bersamaan dengan track baru
    // (0 sampai resolve lagu ini selesai), jadi nilainya selalu
    // milik lagu ini atau 0.
    var lyricsFetchedTrackId by remember { mutableStateOf<String?>(null) }
    var lyricsFetchedDurationMs by remember { mutableStateOf(-1L) }
    // v1.0.4: preferensi sumber lirik + sumber yang dipakai fetch
    // terakhir (ganti preferensi = ambil-ulang, walau lagu/durasi sama).
    var lyricsSourcePref by remember { mutableStateOf(state.store.lyricsSourcePref) }
    var lyricsFetchedSource by remember { mutableStateOf<LyricsApi.LyricsSource?>(null) }
    val lyricsSourceEnum = runCatching {
        LyricsApi.LyricsSource.valueOf(lyricsSourcePref)
    }.getOrDefault(LyricsApi.LyricsSource.AUTO)
    // v1.0.4: naik tiap edit/offset lirik disimpan — baca ulang dari store.
    var lyricsPrefsTick by remember { mutableStateOf(0) }
    var lyricsEditTick by remember { mutableStateOf(0) }
    var showLyricsFullscreen by remember { mutableStateOf(false) }
    val knownDurationMs = t?.durationMs?.takeIf { it > 0 }
        ?: durationMs.takeIf { it > 0 } ?: 0L
    LaunchedEffect(t?.id, knownDurationMs, lyricsSourcePref) {
        val track = t
        if (track == null || track.source == Source.LOCAL) {
            lyricsResult = null
            lyricsTried = true
            lyricsFetchedTrackId = null
            lyricsFetchedDurationMs = -1L
            return@LaunchedEffect
        }
        if (!LyricsVersionPolicy.needsFetch(
                lyricsFetchedTrackId, lyricsFetchedDurationMs,
                track.id, knownDurationMs,
            ) && lyricsFetchedSource == lyricsSourceEnum
        ) {
            return@LaunchedEffect
        }
        // Ganti lagu = reset tampilan; ambil-ulang terverifikasi =
        // timpa diam-diam (tanpa kedip ke state kosong).
        if (lyricsFetchedTrackId != track.id) {
            lyricsResult = null
            lyricsTried = false
        }
        val result = withContext(Dispatchers.IO) {
            LyricsApi.fetchLyrics(
                track.artist, track.title, track.album,
                knownDurationMs, track.youtubeVideoId, lyricsSourceEnum,
            )
        }
        lyricsResult = result
        lyricsTried = true
        lyricsFetchedTrackId = track.id
        lyricsFetchedDurationMs = knownDurationMs
        lyricsFetchedSource = lyricsSourceEnum
    }
    // v1.0.4: override edit lokal menang atas hasil fetch untuk lagu
    // ini (sampai di-reset); kind ikut edit, sumber tetap sumber fetch.
    val lyricsEdit = remember(lyricsEditTick, t?.id) {
        t?.let { state.store.lyricsEdit(it.id) }
    }
    val lyricsEdited = lyricsEdit != null
    val lyrics = lyricsEdit?.lines ?: lyricsResult?.lines ?: emptyList()
    val lyricsKind = lyricsEdit?.kind ?: lyricsResult?.kind ?: LyricsApi.Kind.EMPTY
    val lyricsSource = lyricsResult?.source ?: ""
    // v1.0.4: offset timing (global + per-lagu) hanya menggeser
    // posisi yang dipakai baris aktif + highlight kata — posisi
    // putar/pemutar tidak berubah.
    val lyricsPerSongOffsetMs = remember(lyricsPrefsTick, t?.id) {
        t?.let { state.store.lyricsOffsetMs(it.id) } ?: 0L
    }
    val lyricsGlobalOffsetMs = remember(lyricsPrefsTick) {
        state.store.globalLyricsOffsetMs
    }
    val effectiveLyricPositionMs = LyricsOffsetPolicy.effectivePositionMs(
        positionMs, lyricsGlobalOffsetMs, lyricsPerSongOffsetMs,
    )
    val activeLyricIndex = remember(lyrics, lyricsKind, effectiveLyricPositionMs) {
        // Hanya lirik sinkron yang punya baris aktif mengikuti lagu;
        // lirik polos tidak pura-pura sinkron (active = -1).
        if (lyricsKind != LyricsApi.Kind.SYNCED || lyrics.isEmpty()) -1
        else {
            var idx = -1
            for (i in lyrics.indices) {
                if (lyrics[i].timeMs <= effectiveLyricPositionMs) idx = i else break
            }
            idx
        }
    }
    // Auto-scroll mengikuti posisi putar (v1.6 + follow v1.10.18,
    // centering v1.10.19).
    // Perilaku: SETIAP baris aktif berpindah, daftar digulir sehingga
    // baris aktif berada DI TENGAH viewport lirik (selalu — bukan
    // hanya saat baris keluar viewport; logika "comfortablyVisible"
    // 10.18 DIHAPUS karena membuat layar tampak diam berpuluh detik).
    // Skip hanya bila baris sudah dalam toleransi kecil dari tengah.
    // Masuk tab LYRICS ikut memicu efek ini (detailTab di kunci)
    // sehingga daftar langsung memusat saat tab dibuka di tengah lagu.
    // Scroll SELALU halus (animateScrollToItem). Gulir manual menahan
    // follow (hold sentuh, lihat atas); lompatan baris >3 (seek) dan
    // ganti lagu membatalkan hold.
    //
    // Audit root cause "tidak follow sama sekali" (22 Nov 2026,
    // dibuktikan dari kode): (a) positionMs segar (polling ExoPlayer
    // 100ms) — BUKAN penyebab; (b) guard detailTab == 1 cocok dengan
    // indeks tab LYRICS (tab = detailTab) — BUKAN penyebab; (d) layout
    // viewport TERBATAS (Box weight(1f) → LazyColumn weight(1f)) —
    // BUKAN penyebab; penyebab UTAMA = skip comfortablyVisible +
    // offset hardcoded 56dp sebagai setengah tinggi item (implisit
    // item 112dp; baris nyata ~48dp/1 baris, ~76dp/2 baris) sehingga
    // layar nyaris tidak pernah bergerak dan kalaupun bergerak
    // mendarat di atas tengah; penyebab KEDUA = mekanisme hold
    // flag/isScrollInProgress yang rapuh (lihat atas) — keduanya
    // diganti di 10.19.
    // v1.10.23: follow worker PERSISTEN + closed-loop.
    // Efek 10.18–10.22 restart di SETIAP pindah baris dan menggulir
    // satu tembakan dari satu snapshot layout, tanpa verifikasi:
    // bila gulir itu batal di tengah jalan (restart), terpotong,
    // atau dihitung dari layout sesaat, baris aktif tertinggal di
    // luar tengah sampai pindah baris berikutnya — persis video
    // 10.22 (beku di tepi bawah, lalu lompat). Worker di bawah
    // restart HANYA saat tab/lagu berubah; target baris mengalir
    // lewat Channel CONFLATED (restart per baris tidak lagi
    // membatalkan gulir); tiap target digulir lalu DIVERIFIKASI
    // dari layout segar dan dikoreksi (maks FOLLOW_MAX_CORRECTIONS)
    // sampai benar-benar di tengah. Hold sentuh, seek-jump,
    // easing/durasi 10.22, dan crossfade tidak berubah.
    // v1.10.28: basis koordinat DIPERBAIKI — offset item dari
    // layoutInfo diukur dari awal konten (setelah
    // beforeContentPadding); ketiga rumus pemusatan kini memakai
    // padding itu. Tanpa ini, worker closed-loop 10.23 konvergen
    // ke titik tetap yang salah (tepi bawah daftar) dan verifikasi
    // buta — dibuktikan video HP + source Compose 1.7.0.
    val lyricsFollowRequests =
        remember { Channel<Int>(Channel.CONFLATED) }
    // Penerus target: hanya trySend (non-suspending) — restart-nya
    // tidak pernah membatalkan gulir yang sedang berjalan.
    LaunchedEffect(activeLyricIndex, detailTab, t?.id) {
        if (detailTab == 1 && activeLyricIndex >= 0) {
            lyricsFollowRequests.trySend(activeLyricIndex)
        }
    }
    LaunchedEffect(detailTab, t?.id) {
        if (detailTab != 1) return@LaunchedEffect
        // Ganti lagu / masuk tab: follow mulai dari nol, hold dibatalkan.
        var lastCenteredIndex = -1
        lyricsFollowHoldUntilMs = 0L
        // Masuk tab di tengah lagu: langsung memusat (aturan 10.18).
        if (activeLyricIndex >= 0) {
            lyricsFollowRequests.trySend(activeLyricIndex)
        }
        val tolerancePx = with(density) { 16.dp.roundToPx() }
        for (firstTarget in lyricsFollowRequests) {
            if (firstTarget < 0) continue
            // Lompatan baris >3 (seek) membatalkan hold — dinilai
            // terhadap baris terakhir yang benar-benar dipusatkan.
            if (LyricsFollowState.isSeekJump(lastCenteredIndex, firstTarget)) {
                lyricsFollowHoldUntilMs = 0L
            }
            // Tunggu hold habis (diperpanjang bila pengguna terus
            // menggeser) — tanpa merebut layar saat dibaca. Target
            // TIDAK hilang selama menunggu: worker persisten.
            while (System.currentTimeMillis() < lyricsFollowHoldUntilMs) {
                kotlinx.coroutines.delay(100)
            }
            // Gabungkan target yang tiba selama hold: gulir SEKALI ke
            // target terbaru, bukan sekali per baris yang terlewat.
            var target = firstTarget
            while (true) {
                val newer = lyricsFollowRequests.tryReceive().getOrNull() ?: break
                if (newer >= 0) {
                    if (LyricsFollowState.isSeekJump(lastCenteredIndex, newer)) {
                        lyricsFollowHoldUntilMs = 0L
                    }
                    target = newer
                }
            }
            if (target < 0) continue
            // Gulir + verifikasi closed-loop: ulangi dari layout SEGAR
            // sampai baris aktif di tengah. Satu tembakan tanpa
            // verifikasi adalah cacat 10.22 — koreksi inilah yang
            // menutupnya.
            var attempts = 0
            while (attempts <= LyricsFollowState.FOLLOW_MAX_CORRECTIONS) {
                attempts++
                val info = lyricsListState.layoutInfo
                val viewportH = info.viewportSize.height
                // v1.10.28: offset item diukur dari AWAL KONTEN
                // (setelah beforeContentPadding) — basis pemusatan
                // harus memakai padding ini, kalau tidak baris
                // mendarat di tepi bawah dan verifikasi buta
                // (cacat 10.19–10.27, video HP 23 Nov).
                val beforePad = info.beforeContentPadding
                if (viewportH <= 0) break
                val visible = info.visibleItemsInfo
                val activeItem = visible.firstOrNull { it.index == target }
                if (activeItem != null && LyricsFollowState.isNearCenter(
                        activeItem.offset, activeItem.size, viewportH, tolerancePx,
                        beforePad,
                    )
                ) {
                    break
                }
                // Tinggi item AKTUAL yang terukur; fallback = rata-rata
                // item terlihat; terakhir = estimasi 1 baris dari
                // lineHeight 28sp + padding vertikal 2×10dp (bukan
                // 56dp tanpa dasar).
                val itemH = activeItem?.size
                    ?: visible.map { it.size }.average().takeIf { it > 0 }?.toInt()
                    ?: with(density) { 28.sp.roundToPx() + 20.dp.roundToPx() }
                try {
                    if (activeItem != null) {
                        // Item aktif TERLIHAT — gulir berbasis jarak
                        // terukur dengan tween eksplisit (FastOutSlowIn)
                        // + durasi proporsional jarak (10.22).
                        val distance = LyricsFollowState.centerDistancePx(
                            activeItem.offset, activeItem.size, viewportH, beforePad,
                        )
                        lyricsListState.animateScrollBy(
                            distance.toFloat(),
                            tween(
                                durationMillis =
                                    LyricsFollowState.followScrollDurationMs(distance),
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    } else {
                        // Item aktif belum ter-layout (mis. lompatan
                        // seek jauh): fallback jalur 10.19 — lompat ke
                        // item dengan offset tengah.
                        lyricsListState.animateScrollToItem(
                            target,
                            LyricsFollowState.centerScrollOffset(viewportH, itemH, beforePad),
                        )
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
                // Beri layout pass untuk settle sebelum verifikasi ulang.
                kotlinx.coroutines.delay(LyricsFollowState.FOLLOW_VERIFY_DELAY_MS)
            }
            lastCenteredIndex = target
        }
    }

    // Prefetch artwork 2 lagu berikutnya (v1.6).
    LaunchedEffect(t?.id, currentIndex) {
        val upcoming = queue.drop(currentIndex + 1).take(2).mapNotNull { it.imageUrl }
        if (upcoming.isNotEmpty()) prefetchArtwork(context, upcoming)
    }

    // RELATED: lagu lain dari artis yang sama (hasil pencarian YTM nyata).
    val ytmApi = remember { YoutubeMusicApi() }
    var related by remember { mutableStateOf<List<Track>>(emptyList()) }
    LaunchedEffect(t?.id) {
        related = emptyList()
        // v1.10.19: jangan cari RELATED dari label tipe ("Video").
        if (t != null && isUsableArtist(t.artist)) {
            related = withContext(Dispatchers.IO) {
                ytmApi.search(displayArtist(t.artist)).getOrDefault(emptyList())
                    .filter { it.id != t.id }
                    .take(8)
            }
        }
    }

    if (t == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(bgTop, bgBottom)),
                ),
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
                // v1.10.1: tombol Back ARUS (varian gelap) — ganti chevron.
                ArusBackButton(onClick = onBack, dark = true)
                Spacer(Modifier.height(24.dp))
                Text(
                    "No song is playing.",
                    fontSize = 15.sp,
                    color = DetailTextPrimary,
                )
            }
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom))),
    ) {
        // v1.10.26: field GETAR di belakang konten detail — mengalir
        // saat lagu jalan, diam saat jeda (GetarField, seed lagu).
        if (CurrentTokens.flowField) {
            GetarField(
                trackId = t.id,
                playing = isPlaying,
                darkGround = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.fillMaxSize()) {
            // ---- Bar atas: tombol Back + pill History + titik tiga ----
            // v1.10.6 (CACAT 3): statusBarsPadding() DI DALAM latar
            // gelap — bar tetap di bawah status bar, tapi tidak ada
            // lagi celah terang di atas detail (Scaffold memberi
            // top = 0 untuk layar ini; ikon status bar tetap terang
            // lewat SystemBarAppearance di atas).
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // v1.10.1: panah Back ARUS ganti chevron-bawah; perilaku
                // sama (tutup detail → kembali ke layar sebelumnya).
                // Bar ini dipakai mode penuh DAN mode header mini.
                ArusBackButton(onClick = onBack, dark = true)
                Spacer(Modifier.weight(1f))
                // v1.10.18: pill "History" di slot toggle Song/Video
                // yang dihapus — posisi/ukuran/gaya pill SAMA seperti
                // toggle (tengah atas, sejajar Back + menu). Ketuk →
                // sheet History (pola CommentsPanel). Mode Video
                // dihapus total; detail kini Song-only.
                Row(
                    Modifier
                        .clip(RoundedCornerShape(Corners.pill))
                        .background(DetailTextPrimary.copy(alpha = 0.10f))
                        .padding(3.dp),
                ) {
                    DetailModeChip("History", true) { showHistory = true }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSheet = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Song menu",
                        tint = DetailTextPrimary,
                    )
                }
            }

            // v1.10.6: ganti MODE (penuh ↔ header mini) INSTAN.
            // v1.10.24: isi panel antar tab di mode mini crossfade +
            // geser 180ms (AnimatedContent) — state tiap tab tetap
            // dipertahankan (scroll di-hoist, data keyed by track id)
            // sehingga tidak ada reload saat pindah.
            Box(Modifier.weight(1f)) {
                // v1.0.5: tab efektif — CHAPTERS (3) dijepit ke 0 bila
                // lagu saat ini tidak membawa chapters.
                val tab = effectiveDetailTab
                if (!headerCompact) {
                    // ============ TAMPILAN PENUH (awal dibuka) ============
                    // Satu scroll: artwork → judul → pill → progress →
                    // kontrol → TAB → daftar UP NEXT. Mengetuk tab
                    // mana pun beralih ke header mini (v1.10.7).
                    key(t.id) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = upNextListState,
                        ) {
                            item {
                                StaggerIn(index = 0, enabled = !detailIntroDone) {
                                    // v1.10.24: napas artwork — skala halus
                                    // 1.0↔1.035 selama lagu jalan; saat jeda
                                    // kembali ke 1.0 dengan tween 450ms
                                    // (tanpa lompatan). Reduced-motion: diam.
                                    // graphicsLayer di Box bayangan: layout
                                    // bounds (transisi hero) tidak berubah.
                                    val breatheScale = remember { Animatable(1f) }
                                    LaunchedEffect(isPlaying, reducedMotion) {
                                        if (DetailMotionSpec.breatheActive(
                                                isPlaying, reducedMotion,
                                            )
                                        ) {
                                            while (true) {
                                                breatheScale.animateTo(
                                                    DetailMotionSpec.BREATHE_MAX,
                                                    tween(
                                                        durationMillis =
                                                            DetailMotionSpec.BREATHE_MS / 2,
                                                        easing = FastOutSlowInEasing,
                                                    ),
                                                )
                                                breatheScale.animateTo(
                                                    DetailMotionSpec.BREATHE_MIN,
                                                    tween(
                                                        durationMillis =
                                                            DetailMotionSpec.BREATHE_MS / 2,
                                                        easing = FastOutSlowInEasing,
                                                    ),
                                                )
                                            }
                                        } else {
                                            breatheScale.animateTo(
                                                DetailMotionSpec.BREATHE_MIN,
                                                tween(
                                                    durationMillis = Motion.slowMs,
                                                    easing = FastOutSlowInEasing,
                                                ),
                                            )
                                        }
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                alpha = if (heroHidingArtwork) 0f else 1f
                                            }
                                            .onGloballyPositioned {
                                                onArtworkBounds(it.boundsInWindow())
                                            }
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.TopCenter,
                                    ) {
                                        Box(
                                            Modifier
                                                .graphicsLayer {
                                                    scaleX = breatheScale.value
                                                    scaleY = breatheScale.value
                                                }
                                                .senjaGlow(
                                                    RoundedCornerShape(Corners.detailCover),
                                                    active = isPlaying,
                                                )
                                                .shadow(
                                                    16.dp,
                                                    RoundedCornerShape(Corners.detailCover),
                                                ),
                                        ) {
                                            CoverArt(
                                                track = t,
                                                size = coverSize,
                                                corner = Corners.detailCover,
                                                letterSize = 72.sp,
                                            )
                                            // RASI: cincin orbit + titik mengorbit di tepi cover.
                                            OrbitRingOverlay()
                                        }
                                    }
                                }
                            }
                            item {
                                StaggerIn(index = 1, enabled = !detailIntroDone) {
                                    Column(Modifier.padding(horizontal = 20.dp)) {
                                        // v1.10.7: 16dp (dulu 20dp) — bagian
                                        // dari anggaran tinggi supaya tab muat
                                        // dalam satu layar (lihat BUILD doc).
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            // v1.10.39: judul lagu TIDAK
                                            // pernah di-caps (aturan
                                            // displayCased) — BLOK/PASAR
                                            // mengkapitalkan judul lagu
                                            // di sini; hanya judul
                                            // display/seksi yang boleh.
                                            t.title,
                                            fontSize = CurrentTokens.detailTitleSize,
                                            fontFamily = CurrentTokens.displayFont,
                                            letterSpacing = CurrentTokens.displayLetterSpacing,
                                            // v1.8: lineHeight EKSPLISIT 42sp. Di HP user
                                            // v1.7 dua baris judul tumpang tindih —
                                            // leading default font Black di perangkat
                                            // itu ternyata lebih rapat dari tinggi
                                            // glyph; leading eksplisit memaksa jarak
                                            // antar-baris aman apa pun font perangkat.
                                            lineHeight = 42.sp,
                                            fontWeight = FontWeight.Black,
                                            color = DetailTextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        // v1.10.18: nama artis bisa diketuk →
                                        // profil artis (artis pertama bila
                                        // beberapa artis). Underline tipis
                                        // sebagai indikasi halus; tipografi
                                        // dan ukuran TIDAK berubah.
                                        // v1.10.19: artis kosong/label tipe →
                                        // "Unknown artist", TANPA underline,
                                        // TANPA ketuk, TANPA navigasi profil.
                                        val headerArtist = displayArtist(t.artist)
                                        if (headerArtist.isBlank()) {
                                            Text(
                                                "Unknown artist",
                                                fontSize = Type.artistLarge,
                                                color = DetailTextSecondary.copy(alpha = 0.75f),
                                            )
                                        } else {
                                            Text(
                                                headerArtist,
                                                fontSize = Type.artistLarge,
                                                color = DetailTextSecondary.copy(alpha = 0.75f),
                                                textDecoration = TextDecoration.Underline,
                                                modifier = Modifier.clickable(
                                                    interactionSource = remember {
                                                        MutableInteractionSource()
                                                    },
                                                    indication = ripple(color = DetailTextPrimary),
                                                    onClickLabel = "Open artist profile",
                                                    onClick = {
                                                        // v1.0.5: episode podcast → halaman
                                                        // acara, bukan profil artis.
                                                        val showBid = t.podcastShowBrowseId
                                                        if (t.isPodcastEpisode && showBid != null &&
                                                            onOpenPodcastShow != null
                                                        ) {
                                                            onOpenPodcastShow(
                                                                showBid,
                                                                t.podcastShowTitle ?: headerArtist,
                                                            )
                                                        } else if (onOpenArtist != null) {
                                                            onOpenArtist(
                                                                ArtistApi.firstArtistName(headerArtist),
                                                                t.artistBrowseId,
                                                            )
                                                        } else {
                                                            onOpenSearch(
                                                                ArtistApi.firstArtistName(headerArtist),
                                                            )
                                                        }
                                                    },
                                                ),
                                            )
                                        }
                                        // v1.10.18: label sumber ("YouTube Music"
                                        // + dot merah) DIHAPUS dari detail —
                                        // app hanya memakai YouTube Music
                                        // sejak v1.9, labelnya redundan
                                        // (laporan HP user 20:58 WIB).
                                        // Nama artis di atas tetap.
                                        if (t.album.isNotEmpty()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                t.album,
                                                fontSize = 13.sp,
                                                color = DetailTextSecondary,
                                            )
                                        }
                                        // v1.10.6: penanda LIVE sesi Jam
                                        // (status + jumlah pendengar).
                                        // v1.10.14: tap kembali ke layar
                                        // utama (tempat kartu nempel Jam).
                                        if (jamUiState.active) {
                                            Spacer(Modifier.height(8.dp))
                                            JamLivePill(state = state, onClick = onBack)
                                        }
                                    }
                                }
                            }
                            item {
                                // Baris pill aksi (v1.10.6, CACAT 2):
                                // TANPA scroll horizontal — bobot membuat
                                // keempat pill muat dalam lebar layar
                                // (360dp) tanpa ada yang terpotong.
                                // v1.10.44: Comments + Save jadi IKON
                                // SAJA (screenshot user 26_yaki: teks
                                // label dihapus, ikon + pill tetap) dan
                                // pill sleep timer keempat memakai mesin
                                // yang sama dengan Settings. Pill
                                // Like/Dislike TIDAK berubah.
                                // ANGKA like/komentar TIDAK ditampilkan:
                                // tidak ada API asli yang mengembalikannya,
                                // app dilarang mengarang angka (v1.7).
                                StaggerIn(index = 2, enabled = !detailIntroDone) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            // v1.10.7: 12dp (dulu 16dp) —
                                            // anggaran tinggi tab satu layar.
                                            .padding(top = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        DetailLikePill(
                                            modifier = Modifier.weight(1.0f),
                                            liked = liked,
                                            disliked = disliked,
                                            onLike = {
                                                if (liked) {
                                                    state.store.setLikedTrack(t, false)
                                                } else {
                                                    state.store.setLikedTrack(t, true)
                                                    state.store.setDisliked(t.id, false)
                                                    state.store.setDislikedTrack(t, false)
                                                }
                                                likeTick++
                                            },
                                            onDislike = {
                                                if (disliked) {
                                                    state.store.setDisliked(t.id, false)
                                                    state.store.setDislikedTrack(t, false)
                                                } else {
                                                    state.store.setDisliked(t.id, true)
                                                    state.store.setDislikedTrack(t, true)
                                                    state.store.setLikedTrack(t, false)
                                                    // v1.0.3: lagu yang sedang
                                                    // diputar + tidak disukai =
                                                    // lewati (di luar Jam saja —
                                                    // onTrackDisliked menjaga
                                                    // gerbangnya).
                                                    state.player.onTrackDisliked(t.id)
                                                }
                                                likeTick++
                                            },
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        DetailPill(
                                            icon = Icons.Filled.ChatBubbleOutline,
                                            label = "Comments",
                                            iconOnly = true,
                                            modifier = Modifier.weight(0.6f),
                                            onClick = { showComments = true },
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        DetailPill(
                                            icon = Icons.Filled.PlaylistAdd,
                                            label = "Save",
                                            iconOnly = true,
                                            active = savedToPlaylist,
                                            modifier = Modifier.weight(0.6f),
                                            onClick = { showSaveSheet = true },
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        DetailSleepPill(
                                            endsAtMs = sleepEndsAt,
                                            modifier = Modifier.weight(0.6f),
                                            onClick = { showSleepTimer = true },
                                        )
                                    }
                                }
                            }
                            item {
                                StaggerIn(index = 3, enabled = !detailIntroDone) {
                                    Column(Modifier.padding(horizontal = 20.dp)) {
                                        // v1.10.7: 16dp (dulu 20dp) —
                                        // anggaran tinggi tab satu layar.
                                        Spacer(Modifier.height(16.dp))
                                        // Garis Arus ABU (screenshot
                                        // detail), bisa diklik/diseret seek.
                                        // v1.10.24: lebih hadir (32dp/6dp)
                                        // + amplitudo merespons main/jeda.
                                        ArusWaveProgress(
                                            progress = if (durationMs > 0) {
                                                (positionMs.toFloat() / durationMs)
                                                    .coerceIn(0f, 1f)
                                            } else {
                                                0f
                                            },
                                            playing = isPlaying,
                                            // v1.0.3: siaran live tidak
                                            // bisa di-seek — onSeek null
                                            // mematikan gesture wave.
                                            onSeek = if (t.isLive) {
                                                null
                                            } else {
                                                { frac ->
                                                    val target = (frac * durationMs).toLong()
                                                    guardedPlayback(JamControlAction.SEEK, target) {
                                                        if (durationMs > 0) {
                                                            state.player.seekTo(target)
                                                        }
                                                    }
                                                }
                                            },
                                            onSeekEnd = { haptic(HapticKind.Confirm) },
                                            waveColorA = wavePalette.colorA,
                                            waveColorB = wavePalette.colorB,
                                            trackAlpha = 0.9f,
                                            height = 32.dp,
                                            amplitude = DetailMotionSpec.WAVE_AMP_PLAYING_DP.dp,
                                            responsiveAmplitude = true,
                                            // v1.0.3: tick penanda segmen
                                            // SponsorBlock (awal tiap
                                            // segmen yang dilewati).
                                            markers = if (durationMs > 0) {
                                                segments.map {
                                                    (it.startSec * 1000.0 / durationMs)
                                                        .toFloat()
                                                        .coerceIn(0f, 1f)
                                                }
                                            } else {
                                                emptyList()
                                            },
                                        )
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(
                                                formatMs(positionMs),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = DetailTextSecondary,
                                            )
                                            Spacer(Modifier.weight(1f))
                                            Text(
                                                // v1.0.3: siaran live
                                                // menampilkan LIVE, bukan
                                                // durasi (tidak ada).
                                                if (t.isLive) "LIVE" else formatMs(durationMs),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = DetailTextSecondary,
                                            )
                                        }
                                        // v1.0.3: chip Undo lompatan
                                        // SponsorBlock — 6 detik (pemutar
                                        // menghapusnya sendiri), hanya
                                        // untuk lagu yang dilompati.
                                        skipUndo?.let { undo ->
                                            if (undo.trackId == t.id) {
                                                Spacer(Modifier.height(8.dp))
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text(
                                                        "Skipped a sponsored part",
                                                        fontSize = 13.sp,
                                                        color = DetailTextSecondary,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    TextButton(
                                                        onClick = { state.player.undoLastSkip() },
                                                    ) {
                                                        Text(
                                                            "Undo",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = DetailTextPrimary,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                StaggerIn(index = 4, enabled = !detailIntroDone) {
                                    Column(Modifier.padding(horizontal = 20.dp)) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // v1.10.24: skala tekan + tint
                                            // animasi + titik aktif
                                            // (DetailTransportButton).
                                            DetailTransportButton(
                                                icon = Icons.Filled.Shuffle,
                                                contentDescription = "Shuffle",
                                                onClick = {
                                                    guardedPlaybackMode {
                                                        haptic(HapticKind.Tap)
                                                        state.player.toggleShuffle()
                                                    }
                                                },
                                                buttonSize = 48.dp,
                                                iconSize = 24.dp,
                                                tint = if (shuffle) {
                                                    DetailTextPrimary
                                                } else {
                                                    DetailTextSecondary
                                                },
                                                active = shuffle,
                                            )
                                            Spacer(Modifier.weight(1f))
                                            DetailTransportButton(
                                                icon = Icons.Filled.SkipPrevious,
                                                contentDescription = "Previous",
                                                onClick = {
                                                    guardedPlayback(JamControlAction.PREVIOUS) {
                                                        haptic(HapticKind.Tap)
                                                        state.player.previous()
                                                    }
                                                },
                                                buttonSize = 56.dp,
                                                iconSize = 34.dp,
                                                tint = DetailTextPrimary,
                                            )
                                            // v1.10.24: tanpa pembungkus key
                                            // di sekelilingnya — morph ikon
                                            // di BigPlayButton butuh
                                            // komposisi yang bertahan antar
                                            // toggle.
                                            BigPlayButton(
                                                isPlaying = isPlaying,
                                                onClick = {
                                                    guardedPlayback(JamControlAction.TOGGLE) {
                                                        state.player.togglePlay()
                                                    }
                                                },
                                            )
                                            DetailTransportButton(
                                                icon = Icons.Filled.SkipNext,
                                                contentDescription = "Next",
                                                onClick = {
                                                    guardedPlayback(JamControlAction.NEXT) {
                                                        haptic(HapticKind.Tap)
                                                        state.player.next()
                                                    }
                                                },
                                                buttonSize = 56.dp,
                                                iconSize = 34.dp,
                                                tint = DetailTextPrimary,
                                            )
                                            Spacer(Modifier.weight(1f))
                                            DetailTransportButton(
                                                icon = when (repeat) {
                                                    PlayerManager.RepeatMode.ONE ->
                                                        Icons.Filled.RepeatOne
                                                    else -> Icons.Filled.Repeat
                                                },
                                                contentDescription = "Repeat",
                                                onClick = {
                                                    guardedPlaybackMode {
                                                        haptic(HapticKind.Tap)
                                                        state.player.cycleRepeat()
                                                    }
                                                },
                                                buttonSize = 48.dp,
                                                iconSize = 24.dp,
                                                tint = if (repeat != PlayerManager.RepeatMode.OFF) {
                                                    DetailTextPrimary
                                                } else {
                                                    DetailTextSecondary
                                                },
                                                active = repeat != PlayerManager.RepeatMode.OFF,
                                            )
                                        }
                                        // v1.0.5: tombol kecepatan putar —
                                        // di tengah di bawah kontrol, HANYA
                                        // di luar sesi Jam (drift Jam yang
                                        // memiliki rate saat sesi aktif).
                                        if (!jamUiState.active) {
                                            Spacer(Modifier.height(2.dp))
                                            Box(
                                                Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                TextButton(onClick = { showSpeedDialog = true }) {
                                                    Text(
                                                        "Speed: ${PlaybackSpeedPolicy.formatSpeed(userRate)}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = DetailTextPrimary,
                                                    )
                                                }
                                            }
                                        }
                                        if (isBuffering) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Loading…",
                                                fontSize = 13.sp,
                                                color = DetailTextSecondary,
                                            )
                                        }
                                        playerError?.let { err ->
                                            Spacer(Modifier.height(12.dp))
                                            Column(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(Corners.card))
                                                    .background(
                                                        DetailTextPrimary.copy(alpha = 0.10f),
                                                    )
                                                    .padding(16.dp),
                                            ) {
                                                Text(
                                                    err,
                                                    color = DetailTextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    SecondaryButton(
                                                        text = "Try again",
                                                        onClick = { state.player.retryCurrent() },
                                                    )
                                                    // v1.0.3: rincian
                                                    // kegagalan — apa yang
                                                    // dicoba + kenapa gagal
                                                    // (dialog Track info).
                                                    Spacer(Modifier.width(12.dp))
                                                    TextButton(
                                                        onClick = { showTrackInfo = true },
                                                    ) {
                                                        Text(
                                                            "Details",
                                                            color = DetailTextPrimary,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        // v1.10.7: 12dp (dulu 16dp) — anggaran
                                        // tinggi supaya tab muat satu layar.
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            }
                            // ---- Tab (di tengah, di atas daftar) ----
                            // Mengetuk tab = masuk mode header mini.
                            // v1.10.24: ikut intro berjenjang (indeks 5).
                            item {
                                StaggerIn(index = 5, enabled = !detailIntroDone) {
                                    DetailTabsRow(
                                        selected = effectiveDetailTab,
                                        onSelect = ::selectTab,
                                        showChapters = showChaptersTab,
                                    )
                                }
                            }
                            // ---- Daftar UP NEXT (drag-to-reorder) ----
                            // v1.10.10: selama sesi Jam aktif, daftar
                            // ini MENCERMINKAN session queue (sama
                            // dengan dialog Jam) — bukan antrean lokal.
                            // Tanpa reorder + tanpa "View full queue"
                            // (daftar sesi sudah lengkap di dialog).
                            if (jamUiState.active) {
                                val sessionUpcoming = jamUiState.upcomingQueue
                                val sessionKeys = occurrenceKeys(
                                    sessionUpcoming.map { it.key },
                                )
                                // v1.10.21: bar aksi menempel saat mode
                                // seleksi; tombol Select saat tidak.
                                if (detailSelection.modeActive) {
                                    stickyHeader {
                                        SelectionActionBar(
                                            count = detailSelection.count,
                                            jamActive = true,
                                            onAddToQueue = {
                                                val selected = compactSelectedSessionTracks(
                                                    detailSelection, sessionUpcoming, sessionKeys,
                                                )
                                                val added = state.player.addTracksToQueue(selected)
                                                showToast(
                                                    context,
                                                    if (added == 1) "Added 1 song to queue"
                                                    else "Added $added songs to queue",
                                                )
                                                detailSelection.exitMode()
                                            },
                                            onSaveToPlaylist = {
                                                detailBulkSave = compactSelectedSessionTracks(
                                                    detailSelection, sessionUpcoming, sessionKeys,
                                                )
                                            },
                                            onExit = { detailSelection.exitMode() },
                                        )
                                    }
                                } else if (sessionUpcoming.isNotEmpty()) {
                                    item {
                                        DetailSelectRow(
                                            onSelect = { detailSelection.enterMode() },
                                        )
                                    }
                                }
                                if (sessionUpcoming.isEmpty()) {
                                    item {
                                        Text(
                                            "No next song — the queue ends at this song.",
                                            fontSize = 13.sp,
                                            color = DetailTextSecondary,
                                            modifier = Modifier.padding(
                                                horizontal = 20.dp,
                                                vertical = 8.dp,
                                            ),
                                        )
                                    }
                                } else {
                                    item {
                                        Box(Modifier.padding(horizontal = 20.dp)) {
                                            SessionUpNextList(
                                                items = sessionUpcoming,
                                                isGuest = isJamGuest,
                                                onPlaySessionItem = { item ->
                                                    state.jam.jumpToQueueItem(item) { msg ->
                                                        showToast(context, msg)
                                                    }
                                                },
                                                onRequestSessionJump = { item ->
                                                    state.jam.requestJumpToQueueItem(item) { msg ->
                                                        showToast(context, msg)
                                                    }
                                                },
                                                onRemoveSessionItem = if (isJamGuest) null else { item ->
                                                    state.jam.removeQueueItem(item) { msg ->
                                                        showToast(
                                                            context,
                                                            if (msg == "Removed from the Jam queue.") {
                                                                "Removed \"${item.title}\" from the Jam queue."
                                                            } else {
                                                                msg
                                                            },
                                                        )
                                                    }
                                                },
                                                // v1.10.36: susun ulang Up Next sesi (host
                                                // langsung, tamu lewat permintaan ke host).
                                                onMoveSessionItem = { from, to ->
                                                    val moved = sessionUpcoming.toMutableList().apply {
                                                        add(to, removeAt(from))
                                                    }
                                                    state.jam.reorderSessionQueue(moved) { msg ->
                                                        showToast(context, msg)
                                                    }
                                                },
                                                selectionState = detailSelection,
                                                selectionKeys = sessionKeys,
                                            )
                                        }
                                    }
                                }
                                item {
                                    Spacer(Modifier.height(16.dp))
                                }
                            } else {
                                val upcoming = queue.drop(currentIndex + 1).take(5)
                                // v1.10.21: bar aksi menempel saat mode
                                // seleksi; tombol Select saat tidak.
                                if (detailSelection.modeActive) {
                                    stickyHeader {
                                        SelectionActionBar(
                                            count = detailSelection.count,
                                            jamActive = false,
                                            onAddToQueue = {
                                                val selected = upcoming.filter {
                                                    detailSelection.isSelected(
                                                        selectionKey("detail", it.queueUid),
                                                    )
                                                }
                                                val added = state.player.addTracksToQueue(selected)
                                                showToast(
                                                    context,
                                                    if (added == 1) "Added 1 song to queue"
                                                    else "Added $added songs to queue",
                                                )
                                                detailSelection.exitMode()
                                            },
                                            onSaveToPlaylist = {
                                                detailBulkSave = upcoming.filter {
                                                    detailSelection.isSelected(
                                                        selectionKey("detail", it.queueUid),
                                                    )
                                                }
                                            },
                                            onExit = { detailSelection.exitMode() },
                                        )
                                    }
                                } else if (upcoming.isNotEmpty()) {
                                    item {
                                        DetailSelectRow(
                                            onSelect = { detailSelection.enterMode() },
                                        )
                                    }
                                }
                                if (upcoming.isEmpty()) {
                                    item {
                                        Text(
                                            "No next song — the queue ends at this song.",
                                            fontSize = 13.sp,
                                            color = DetailTextSecondary,
                                            modifier = Modifier.padding(
                                                horizontal = 20.dp,
                                                vertical = 8.dp,
                                            ),
                                        )
                                    }
                                } else {
                                    item {
                                        Box(Modifier.padding(horizontal = 20.dp)) {
                                            ReorderableUpcoming(
                                                upcoming = upcoming,
                                                currentIndex = currentIndex,
                                                onMove = { from, to ->
                                                    state.player.moveQueue(from, to)
                                                },
                                                onPlayAt = { idx -> state.player.playAt(idx) },
                                                haptic = haptic,
                                                // v1.10.21: swipe mati saat mode seleksi.
                                                onSwipeRemove = if (detailSelection.modeActive) null else swipeHandler,
                                                selectionState = detailSelection,
                                                durations = state.durations,
                                            )
                                        }
                                    }
                                }
                                item {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            // v1.10.24: 8dp (dulu 12dp) —
                                            // aksi menempel ke empty state.
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        TextButton(onClick = onOpenQueue) {
                                            Text("View full queue", color = DetailTextPrimary)
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                                // v1.0.3: seksi Radio — HANYA di luar
                                // Jam (cabang ini memang non-Jam).
                                // Daftar TERPISAH dari antrean user:
                                // tanpa reorder/swipe, ketuk = putar
                                // lagu radio itu; lagu yang sedang
                                // diputar ditandai; "Stop radio"
                                // menghentikan radio sepenuhnya.
                                if (radioActive) {
                                    item {
                                        Column(
                                            Modifier.padding(horizontal = 20.dp),
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    "Radio",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DetailTextPrimary,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                TextButton(
                                                    onClick = { state.player.clearRadio() },
                                                ) {
                                                    Text(
                                                        "Stop radio",
                                                        color = DetailTextPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                }
                                            }
                                            for ((rIndex, rTrack) in radioQueue.withIndex()) {
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    // Penanda lagu radio
                                                    // yang sedang diputar:
                                                    // titik kecil di depan
                                                    // baris (tanpa reorder/
                                                    // swipe — bukan antrean
                                                    // user).
                                                    if (rIndex == radioIndex) {
                                                        Box(
                                                            Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(DetailTextPrimary),
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                    }
                                                    Box(Modifier.weight(1f)) {
                                                        DetailTrackRow(
                                                            track = rTrack,
                                                            onClick = {
                                                                state.player.playRadioAt(rIndex)
                                                            },
                                                            showDuration = true,
                                                            durations = state.durations,
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ============ HEADER MINI + PANEL (semua tab, v1.10.7) ====
                    // Mini-header kompak YANG SAMA untuk UP NEXT, LYRICS,
                    // dan RELATED: kartu thumbnail + judul/artis + tombol
                    // play, lalu baris tab, lalu isi tab. Ketuk kartu
                    // header = kembali ke tampilan penuh.
                    Column(Modifier.fillMaxSize()) {
                        // Panel header mini — sudut atas membulat,
                        // berisi cover + judul/artis + tombol play.
                        // Ketuk header kembali ke tampilan penuh.
                        // v1.10.31: latar menganimasikan mengikuti warna
                        // artwork (sama seperti panel di bawahnya).
                        val miniHeaderBg by animateColorAsState(
                            targetValue = detailPanelColors?.miniHeader
                                ?: DetailMiniHeaderBg,
                            animationSpec = tween(
                                durationMillis = CurrentTokens.tabSlideMs,
                                easing = FastOutSlowInEasing,
                            ),
                            label = "detailMiniHeaderBg",
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = Corners.sheet,
                                        topEnd = Corners.sheet,
                                    ),
                                )
                                .background(miniHeaderBg),
                        ) {
                            Column {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = ripple(color = DetailTextPrimary),
                                            onClickLabel = "Back to full view",
                                            // v1.10.7: satu-satunya jalan
                                            // kembali ke tampilan penuh.
                                            onClick = { detailTab = 0; headerCompact = false },
                                        )
                                        .padding(
                                            horizontal = 16.dp,
                                            vertical = 10.dp,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CoverArt(
                                        track = t,
                                        size = 56.dp,
                                        corner = Corners.rowCover,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            t.title,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DetailTextPrimary,
                                            maxLines = 1,
                                        )
                                        // v1.10.19: sama seperti header penuh —
                                        // kosong/label → "Unknown artist"
                                        // tanpa underline/ketuk/navigasi.
                                        val miniHeaderArtist = displayArtist(t.artist)
                                        if (miniHeaderArtist.isBlank()) {
                                            Text(
                                                "Unknown artist",
                                                fontSize = 15.sp,
                                                color = DetailTextSecondary.copy(alpha = 0.75f),
                                                maxLines = 1,
                                            )
                                        } else {
                                            Text(
                                                miniHeaderArtist,
                                                fontSize = 15.sp,
                                                color = DetailTextSecondary.copy(alpha = 0.75f),
                                                textDecoration = TextDecoration.Underline,
                                                maxLines = 1,
                                                modifier = Modifier.clickable(
                                                    interactionSource = remember {
                                                        MutableInteractionSource()
                                                    },
                                                    indication = ripple(color = DetailTextPrimary),
                                                    onClickLabel = "Open artist profile",
                                                    onClick = {
                                                        // v1.0.5: episode podcast → halaman
                                                        // acara, bukan profil artis.
                                                        val showBid = t.podcastShowBrowseId
                                                        if (t.isPodcastEpisode && showBid != null &&
                                                            onOpenPodcastShow != null
                                                        ) {
                                                            onOpenPodcastShow(
                                                                showBid,
                                                                t.podcastShowTitle ?: miniHeaderArtist,
                                                            )
                                                        } else if (onOpenArtist != null) {
                                                            onOpenArtist(
                                                                ArtistApi.firstArtistName(miniHeaderArtist),
                                                                t.artistBrowseId,
                                                            )
                                                        } else {
                                                            onOpenSearch(
                                                                ArtistApi.firstArtistName(miniHeaderArtist),
                                                            )
                                                        }
                                                    },
                                                ),
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            guardedPlayback(JamControlAction.TOGGLE) {
                                                haptic(HapticKind.Tap)
                                                state.player.togglePlay()
                                            }
                                        },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        // v1.10.24: morph yang sama dengan
                                        // tombol play besar.
                                        MorphPlayPauseIcon(
                                            isPlaying = isPlaying,
                                            tint = DetailTextPrimary,
                                            iconSize = 32.dp,
                                        )
                                    }
                                }
                                DetailTabsRow(
                                    selected = effectiveDetailTab,
                                    onSelect = { detailTab = it },
                                    showChapters = showChaptersTab,
                                )
                            }
                        }
                        // Panel lembar sudut membulat (warna per tab).
                        // v1.10.24: latar menganimasikan antar tab
                        // (300ms); isi panel crossfade + geser 8dp.
                        // State tiap tab tetap di-hoist (tidak reload).
                        val detailTokens = CurrentTokens
                        val detailEasing = themeEasing(detailTokens)
                        val panelBg by animateColorAsState(
                            // v1.10.31: satu warna turunan artwork untuk
                            // semua tab; tanpa artwork, kembali ke warna
                            // tetap per tab seperti sebelumnya.
                            targetValue = detailPanelColors?.panel
                                ?: if (tab == 1) DetailLyricsCardBg
                                else DetailRelatedPanelBg,
                            animationSpec = tween(
                                durationMillis = detailTokens.tabSlideMs,
                                easing = detailEasing,
                            ),
                            label = "detailPanelBg",
                        )
                        val panelFadeMs =
                            if (reducedMotion) 0 else detailTokens.panelFadeMs
                        val panelExitMs =
                            if (reducedMotion) 0 else detailTokens.panelExitMs
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        topStart = Corners.sheet,
                                        topEnd = Corners.sheet,
                                    ),
                                )
                                .background(panelBg),
                        ) {
                            AnimatedContent(
                                targetState = tab,
                                transitionSpec = {
                                    (fadeIn(
                                        tween(
                                            durationMillis = panelFadeMs,
                                            easing = detailEasing,
                                        ),
                                    ) + slideInVertically(
                                        tween(
                                            durationMillis = panelFadeMs,
                                            easing = FastOutSlowInEasing,
                                        ),
                                    ) { it / 12 }) togetherWith fadeOut(
                                        tween(
                                            durationMillis = panelExitMs,
                                            easing = FastOutSlowInEasing,
                                        ),
                                    )
                                },
                                label = "detailTabPanel",
                            ) { panelTab ->
                                when (panelTab) {
                                // v1.10.7: UP NEXT memakai panel yang
                                // sama strukturnya dengan tab lain di
                                // mode mini-header (bukan tampilan penuh).
                                0 -> UpNextCompactPanel(
                                    queue = queue,
                                    currentIndex = currentIndex,
                                    listState = upNextCompactListState,
                                    player = state.player,
                                    haptic = haptic,
                                    onOpenQueue = onOpenQueue,
                                    jamActive = jamUiState.active,
                                    sessionQueue = jamUiState.upcomingQueue,
                                    isGuest = isJamGuest,
                                    onPlaySessionItem = { item ->
                                        state.jam.jumpToQueueItem(item) { msg ->
                                            showToast(context, msg)
                                        }
                                    },
                                    onRequestSessionJump = { item ->
                                        state.jam.requestJumpToQueueItem(item) { msg ->
                                            showToast(context, msg)
                                        }
                                    },
                                    onSwipeRemove = swipeHandler,
                                    onRemoveSessionItem = if (isJamGuest) null else { item ->
                                        state.jam.removeQueueItem(item) { msg ->
                                            showToast(
                                                context,
                                                if (msg == "Removed from the Jam queue.") {
                                                    "Removed \"${item.title}\" from the Jam queue."
                                                } else {
                                                    msg
                                                },
                                            )
                                        }
                                    },
                                    // v1.10.36: susun ulang Up Next sesi (host langsung,
                                    // tamu lewat permintaan ke host).
                                    onMoveSessionItem = { from, to ->
                                        val moved = jamUiState.upcomingQueue.toMutableList().apply {
                                            add(to, removeAt(from))
                                        }
                                        state.jam.reorderSessionQueue(moved) { msg ->
                                            showToast(context, msg)
                                        }
                                    },
                                    // v1.10.21: multi-select panel mini.
                                    selectionState = detailSelection,
                                    onBulkAddToQueue = { selected ->
                                        val added = state.player.addTracksToQueue(selected)
                                        showToast(
                                            context,
                                            if (added == 1) "Added 1 song to queue"
                                            else "Added $added songs to queue",
                                        )
                                        detailSelection.exitMode()
                                    },
                                    onBulkSave = { selected ->
                                        detailBulkSave = selected
                                    },
                                    durations = state.durations,
                                )
                                1 -> LyricsPanel(
                                    lyrics = lyrics,
                                    lyricsTried = lyricsTried,
                                    activeLyricIndex = activeLyricIndex,
                                    lyricsListState = lyricsListState,
                                    // v1.0.4: posisi EFEKTIF (offset) untuk
                                    // highlight kata; baris aktif sudah
                                    // memakai nilai yang sama di atas.
                                    positionMs = effectiveLyricPositionMs,
                                    kind = lyricsKind,
                                    source = lyricsSource,
                                    track = t,
                                    store = state.store,
                                    sourcePref = lyricsSourceEnum,
                                    onSourcePrefChange = { pref ->
                                        state.store.lyricsSourcePref = pref.name
                                        lyricsSourcePref = pref.name
                                    },
                                    lyricsEdited = lyricsEdited,
                                    onLyricsEdited = { lyricsEditTick++ },
                                    perSongOffsetMs = lyricsPerSongOffsetMs,
                                    globalOffsetMs = lyricsGlobalOffsetMs,
                                    onOffsetSaved = { lyricsPrefsTick++ },
                                    onOpenFullscreen = { showLyricsFullscreen = true },
                                    onShare = { shareTrack(context, t) },
                                    // v1.10.16: kartu lirik — render di
                                    // background, gagal = toast jujur.
                                    onShareLyricCard = {
                                        lyricShareScope.launch {
                                            val ok = LyricCard.share(
                                                context, t, lyricsKind, lyrics,
                                                activeLyricIndex, lyricsSource,
                                            )
                                            if (!ok) {
                                                showToast(
                                                    context,
                                                    "Couldn't create the lyric card.",
                                                )
                                            }
                                        }
                                    },
                                    // v1.10.19: sentuhan manual menahan follow.
                                    onTouchDown = onLyricsTouchDown,
                                    onTouchUp = onLyricsTouchUp,
                                )
                                2 -> RelatedPanel(
                                    related = related,
                                    track = t,
                                    state = state,
                                    scrollState = relatedScrollState,
                                )
                                // v1.0.5: CHAPTERS — hanya tercapai bila
                                // chapters termuat (tab dijepit bila tidak).
                                3 -> ChaptersPanel(
                                    chapters = chapters.orEmpty(),
                                    positionMs = positionMs,
                                    onSeek = { target ->
                                        guardedPlayback(JamControlAction.SEEK, target) {
                                            state.player.seekTo(target)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showSheet) {
        TrackActionSheet(
            track = t,
            state = state,
            onDismiss = { showSheet = false },
            onOpenSearch = onOpenSearch,
            onOpenArtist = onOpenArtist,
        )
    }
    if (showComments) {
        CommentsPanel(track = t, ytmApi = state.ytmApi, onDismiss = { showComments = false })
    }
    // v1.0.3: dialog info lagu — dari tombol "Details" di kotak
    // error (jejak kegagalan resolve) + aksi "Track info" di sheet.
    if (showTrackInfo) {
        TrackInfoDialog(
            track = t,
            state = state,
            onDismiss = { showTrackInfo = false },
        )
    }
    // v1.0.4: lirik fullscreen — daftar sendiri, follow memakai rumus
    // pemusatan yang sama (LyricsFollowState), gaya Standard/Large.
    if (showLyricsFullscreen) {
        LyricsFullscreenDialog(
            lyrics = lyrics,
            kind = lyricsKind,
            source = lyricsSource,
            activeLyricIndex = activeLyricIndex,
            positionMs = effectiveLyricPositionMs,
            lyricsEdited = lyricsEdited,
            onDismiss = { showLyricsFullscreen = false },
        )
    }
    if (showHistory && t != null) {
        HistoryPanel(
            track = t,
            content = historyContent,
            onDismiss = { showHistory = false },
        )
    }
    if (showSaveSheet) {
        SaveToPlaylistSheet(
            track = t,
            state = state,
            onDismiss = {
                showSaveSheet = false
                likeTick++
            },
        )
    }
    // v1.10.44: dialog sleep timer — pilihan menit yang SAMA dengan
    // Settings (Playback) dan memanggil mesin yang sama
    // (setSleepTimer); Off membatalkan. Status hidup dibaca dari
    // endsAtMs mesin, jadi dialog ini dan Settings selalu sepakat.
    if (showSleepTimer) {
        AlertDialog(
            onDismissRequest = { showSleepTimer = false },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    Text("Stops playback when the timer ends, even if the song changes.")
                    if (sleepEndsAt != null) {
                        Spacer(Modifier.height(6.dp))
                        val endTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(sleepEndsAt!!))
                        Text(
                            "Sleep timer on — playback stops at $endTime.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (sleepEndOfTrack) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Sleep timer on — playback stops at the end of the current track.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        for (minutes in SLEEP_TIMER_CHOICES_MINUTES) {
                            val selected = sleepChoiceMinutes == minutes && !sleepEndOfTrack
                            Box(
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(Corners.button))
                                    .background(if (selected) AccentFill else SegmentBg)
                                    .clickable {
                                        sleepChoiceMinutes = minutes
                                        state.player.setSleepTimer(minutes)
                                        showSleepTimer = false
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (minutes == 0) "Off" else "$minutes min",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) OnAccent else InkBlack,
                                )
                            }
                            if (minutes != SLEEP_TIMER_CHOICES_MINUTES.last()) {
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                    // v1.0.5: berhenti di akhir lagu/episode saat ini
                    // (mode mesin yang sama — tanpa menit).
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(Corners.button))
                                .background(if (sleepEndOfTrack) AccentFill else SegmentBg)
                                .clickable {
                                    state.player.setSleepTimerEndOfTrack()
                                    showSleepTimer = false
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "End of track",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sleepEndOfTrack) OnAccent else InkBlack,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSleepTimer = false },
                    colors = sonaraTextButtonColors(),
                ) { Text("Done") }
            },
        )
    }
    // v1.0.5: dialog kecepatan putar (tombol "Speed:" di bawah
    // kontrol + TrackSheet + Settings memakai SpeedDialog yang sama).
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
    // v1.10.21: sheet simpan banyak lagu — menutup sheet keluar mode.
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
}

/**
 * Tombol transport detail (v1.10.24): skala tekan spring +
 * tint menganimasikan 200ms + titik aktif 4dp di bawah ikon
 * (shuffle/repeat). Klik = lambda pemanggil persis seperti
 * IconButton sebelumnya (guardedPlayback + haptic di pemanggil).
 */
@Composable
private fun DetailTransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    buttonSize: Dp,
    iconSize: Dp,
    tint: Color,
    active: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = DetailMotionSpec.pressScale(
            pressed, DetailMotionSpec.PRESS_TRANSPORT,
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "transportPress",
    )
    val animatedTint by animateColorAsState(
        targetValue = tint,
        animationSpec = tween(
            durationMillis = CurrentTokens.tintMs,
            easing = themeEasing(CurrentTokens),
        ),
        label = "transportTint",
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = CurrentTokens.tintMs,
            easing = themeEasing(CurrentTokens),
        ),
        label = "transportDot",
    )
    Box(
        modifier = Modifier
            .size(buttonSize)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = DetailTextPrimary),
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = animatedTint,
            modifier = Modifier.size(iconSize),
        )
        if (dotAlpha > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(DetailTextPrimary.copy(alpha = dotAlpha)),
            )
        }
    }
}

/**
 * Pill Like/Dislike gabungan di baris pill detail (rekonstruksi
 * v1.10.18: definisi aslinya ikut terhapus saat penghapusan mode Video
 * dihapus — perilaku dipulihkan dari call site + DetailPill:
 * satu pill 48dp, dua paruh ketuk ThumbUp/ThumbDown, tanpa angka).
 */
@Composable
private fun DetailLikePill(
    modifier: Modifier = Modifier,
    liked: Boolean,
    disliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
) {
    val haptic = rememberHaptic()
    // v1.10.24: skala tekan per paruh (spring) — respons sentuh.
    val likeInteraction = remember { MutableInteractionSource() }
    val dislikeInteraction = remember { MutableInteractionSource() }
    val likePressed by likeInteraction.collectIsPressedAsState()
    val dislikePressed by dislikeInteraction.collectIsPressedAsState()
    val pressSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val likeScale by animateFloatAsState(
        targetValue = DetailMotionSpec.pressScale(
            likePressed, DetailMotionSpec.PRESS_PILL,
        ),
        animationSpec = pressSpec,
        label = "likePress",
    )
    val dislikeScale by animateFloatAsState(
        targetValue = DetailMotionSpec.pressScale(
            dislikePressed, DetailMotionSpec.PRESS_PILL,
        ),
        animationSpec = pressSpec,
        label = "dislikePress",
    )
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(Corners.pill))
            .background(
                DetailTextPrimary.copy(alpha = if (liked || disliked) 0.20f else 0.12f),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .scale(likeScale)
                .clickable(
                    interactionSource = likeInteraction,
                    indication = ripple(color = DetailTextPrimary),
                    onClickLabel = "Like",
                    onClick = {
                        haptic(HapticKind.Tap)
                        onLike()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ThumbUp,
                contentDescription = null,
                tint = if (liked) DetailTextPrimary else DetailTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(
            Modifier
                .width(1.dp)
                .height(24.dp)
                .background(DetailTextPrimary.copy(alpha = 0.25f)),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .scale(dislikeScale)
                .clickable(
                    interactionSource = dislikeInteraction,
                    indication = ripple(color = DetailTextPrimary),
                    onClickLabel = "Dislike",
                    onClick = {
                        haptic(HapticKind.Tap)
                        onDislike()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ThumbDown,
                contentDescription = null,
                tint = if (disliked) DetailTextPrimary else DetailTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Baris 3 tab detail (rekonstruksi v1.10.18 — lihat DetailLikePill):
 * tombol DetailTabButton yang sudah ada, selebar layar.
 * v1.10.24: SATU indikator yang BERGESER antar tab (bukan underline
 * per tombol yang muncul/hilang) — offset dianimasikan 300ms
 * FastOutSlowIn, instan saat reduced-motion. Garis dasar 1dp tetap.
 */
@Composable
/**
 * Baris tab detail (v1.0.5): 3 tab (UP NEXT/LYRICS/RELATED) atau 4
 * bila [showChapters] true (CHAPTERS ke-4, untuk episode/konten
 * panjang yang respons /next-nya membawa panel chapters). Lebar tab
 * dan indeks indikator mengikuti [tabCount] — rumus indikator
 * (DetailMotionSpec.tabIndicatorIndex) tetap yang lama.
 */
private fun DetailTabsRow(
    selected: Int,
    onSelect: (Int) -> Unit,
    showChapters: Boolean = false,
) {
    val reducedMotion = rememberReducedMotion()
    val tabCount = if (showChapters) 4 else 3
    val tabIndex = DetailMotionSpec.tabIndicatorIndex(selected, tabCount)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        val tabWidth = maxWidth / tabCount
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * tabIndex,
            animationSpec = if (reducedMotion) {
                tween(durationMillis = 0)
            } else {
                tween(
                    durationMillis = CurrentTokens.tabSlideMs,
                    easing = themeEasing(CurrentTokens),
                )
            },
            label = "tabIndicator",
        )
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                DetailTabButton(
                    label = "UP NEXT",
                    selected = selected == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(0) },
                )
                DetailTabButton(
                    label = "LYRICS",
                    selected = selected == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(1) },
                )
                DetailTabButton(
                    label = "RELATED",
                    selected = selected == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(2) },
                )
                if (showChapters) {
                    DetailTabButton(
                        label = "CHAPTERS",
                        selected = selected == 3,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(3) },
                    )
                }
            }
            // Zona indikator 3dp: garis dasar + indikator geser.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(DetailTextPrimary.copy(alpha = 0.25f)),
                )
                Box(
                    Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(DetailTextPrimary),
                )
            }
        }
    }
}

/**
 * Panel CHAPTERS (v1.0.5): daftar chapter dari panel
 * macro-markers /next (judul + waktu mulai verbatim). Chapter aktif
 * (dari posisi putar) ditebalkan; ketuk chapter = lompat ke waktu
 * mulai (seek sudah di-guard pemanggil untuk Jam). Waktu tampil
 * format M:SS/H:MM:SS dari ms — tanpa angka yang dikarang.
 */
@Composable
private fun ChaptersPanel(
    chapters: List<Chapter>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
) {
    val active = ChaptersApi.activeChapter(chapters, positionMs)
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(
            chapters,
            key = { i, c -> "chapter#$i#${c.startMs}#${c.title}" },
        ) { _, chapter ->
            val isActive = chapter == active
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(chapter.startMs) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatDurationMs(chapter.startMs),
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) DetailTextPrimary else DetailTextSecondary,
                    modifier = Modifier.width(64.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    chapter.title,
                    fontSize = 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) DetailTextPrimary else DetailTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DetailModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val haptic = rememberHaptic()
    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(Corners.pill))
            .background(
                if (selected) Color(0xFF6B5F43) else androidx.compose.ui.graphics.Color.Transparent,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = DetailTextPrimary),
                onClickLabel = "$label mode",
                onClick = {
                    haptic(HapticKind.Selection)
                    onClick()
                },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) DetailTextPrimary else DetailTextSecondary.copy(alpha = 0.75f),
        )
    }
}

/** Tombol tab detail — label + skala tekan; underline-nya milik DetailTabsRow (indikator geser). */
@Composable
private fun DetailTabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = rememberHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = DetailMotionSpec.pressScale(
            pressed, DetailMotionSpec.PRESS_TAB,
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tabPress",
    )
    val animatedColor by animateColorAsState(
        targetValue = if (selected) DetailTextPrimary
        else DetailTextSecondary.copy(alpha = 0.75f),
        animationSpec = tween(
            durationMillis = CurrentTokens.tintMs,
            easing = themeEasing(CurrentTokens),
        ),
        label = "tabLabelColor",
    )
    Column(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = DetailTextPrimary),
                onClickLabel = "$label tab",
                onClick = {
                    haptic(HapticKind.Selection)
                    onClick()
                },
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            fontFamily = CurrentTokens.labelFont,
            fontSize = 14.sp,
            letterSpacing = CurrentTokens.labelLetterSpacing,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = animatedColor,
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** Satu pill aksi di baris pill detail (Like / Dislike / Komentar / Save). */
@Composable
private fun DetailPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    popKey: Boolean = false,
    hapticKind: HapticKind = HapticKind.Tap,
    // v1.10.44: true = hanya ikon (tanpa teks label); bentuk, latar,
    // tinggi, dan aksi pill tidak berubah. Label tetap jadi
    // contentDescription ikon + onClickLabel (aksesibilitas).
    iconOnly: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Like pop meletup saat berubah jadi disukai (hormat reduced-motion).
    val reducedMotion = rememberReducedMotion()
    val haptic = rememberHaptic()
    val pop = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (popKey && active && !reducedMotion) {
            pop.snapTo(0.6f)
            pop.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }
    // v1.10.24: skala tekan (spring) — respons sentuh di seluruh pill.
    val pressInteraction = remember { MutableInteractionSource() }
    val pressPressed by pressInteraction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = DetailMotionSpec.pressScale(
            pressPressed, DetailMotionSpec.PRESS_PILL,
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pillPress",
    )
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(Corners.pill))
            .background(DetailTextPrimary.copy(alpha = if (active) 0.20f else 0.12f))
            .scale(pressScale)
            .clickable(
                interactionSource = pressInteraction,
                indication = ripple(color = DetailTextPrimary),
                onClickLabel = label,
                onClick = {
                    haptic(hapticKind)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            // v1.10.39: padding 6dp (dulu 8dp) + ikon 16dp (dulu 18dp)
            // + letterSpacing 0 eksplisit — "Comments" terpotong di
            // SEMUA tema pada layar 320–360dp (diukur metrik font
            // asli, harness139: 19 kegagalan → 0). Klaim 10.24 bahwa
            // label muat ternyata salah untuk font tema yang lebar.
            Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.scale(pop.value)) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (iconOnly) label else null,
                    tint = if (active) DetailTextPrimary else DetailTextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (!iconOnly) {
                Spacer(Modifier.width(3.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    fontFamily = CurrentTokens.bodyFont,
                    letterSpacing = 0.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) DetailTextPrimary else DetailTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Pill sleep timer di baris pill detail (v1.10.44): jalan pintas
 * satu ketuk ke mesin yang sama dengan Settings → Playback
 * (`PlayerManager.setSleepTimer`, v1.10.6 — TIDAK ada mesin
 * kedua). Nonaktif = ikon bulan saja; aktif = ikon + sisa waktu
 * ("24m", dihitung [sleepRemainingLabel] dari endsAtMs mesin,
 * diperbarui tiap 30 detik). Ketuk membuka dialog pilihan menit
 * (milik pemanggil). Bentuk/latar/tinggi mengikuti [DetailPill].
 */
@Composable
private fun DetailSleepPill(
    endsAtMs: Long?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = rememberHaptic()
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAtMs) {
        while (endsAtMs != null) {
            kotlinx.coroutines.delay(30_000L)
            nowMs = System.currentTimeMillis()
        }
    }
    val remaining = sleepRemainingLabel(endsAtMs, nowMs)
    val active = remaining != null
    // v1.10.24: skala tekan (spring) — sama dengan DetailPill.
    val pressInteraction = remember { MutableInteractionSource() }
    val pressPressed by pressInteraction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = DetailMotionSpec.pressScale(
            pressPressed, DetailMotionSpec.PRESS_PILL,
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "sleepPillPress",
    )
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(Corners.pill))
            .background(DetailTextPrimary.copy(alpha = if (active) 0.20f else 0.12f))
            .scale(pressScale)
            .clickable(
                interactionSource = pressInteraction,
                indication = ripple(color = DetailTextPrimary),
                onClickLabel = "Sleep timer",
                onClick = {
                    haptic(HapticKind.Tap)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = "Sleep timer",
                tint = if (active) DetailTextPrimary else DetailTextSecondary,
                modifier = Modifier.size(18.dp),
            )
            if (active) {
                Spacer(Modifier.width(3.dp))
                Text(
                    remaining!!,
                    fontSize = 12.sp,
                    fontFamily = CurrentTokens.bodyFont,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailTextPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Baris lagu di atas background gelap detail (UP NEXT / RELATED) — badge E di depan nama artis (screenshot). */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DetailTrackRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToQueue: (() -> Unit)? = null,
    showDuration: Boolean = false,
    /** Multi-select (v1.10.21): lencana centang + interaksi ketuk. */
    selected: Boolean = false,
    /** Long-press masuk mode seleksi; null = perilaku lama (tanpa long-press). */
    onLongClick: (() -> Unit)? = null,
    /** Durasi per videoId (v1.10.31) — baris UP NEXT ikut cache lookupVideo. */
    durations: TrackDurations? = null,
) {
    // Durasi tampil: bawaan track bila ada, else cache. Gate showDuration
    // dipertahankan: tanpa durasi nyata, tidak ada placeholder.
    val durationCache by (durations?.cache
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap<String, Long>()) })
        .collectAsState()
    val shownDurationMs = track.durationMs.takeIf { it > 0 }
        ?: (durationCache[track.youtubeVideoId] ?: 0L)
    LaunchedEffect(track.youtubeVideoId, durations) {
        // Hanya minta lookup bila durasinya benar-benar ditampilkan:
        // baris Jam (showDuration=false) tidak memicu request.
        if (showDuration && durations != null && track.durationMs <= 0) {
            durations.request(track.youtubeVideoId)
        }
    }
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = DetailTextPrimary),
            onClickLabel = "Play ${track.title}",
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = DetailTextPrimary),
            onClickLabel = "Play ${track.title}",
            onClick = onClick,
        )
    }
    Row(
        modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AccentFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = OnAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        CoverArt(track = track, size = 56.dp, corner = Corners.rowCover)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DetailTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.explicit) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DetailTextPrimary.copy(alpha = 0.20f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "E",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DetailTextPrimary,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = run {
                        // v1.10.19: sama seperti TrackRow (label → kosong).
                        val artistText = displayArtist(track.artist)
                        when {
                            track.album.isNotBlank() && artistText.isNotBlank() ->
                                "$artistText · ${track.album}"
                            track.album.isNotBlank() -> track.album
                            else -> artistText
                        }
                    },
                    fontSize = 14.sp,
                    color = DetailTextSecondary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showDuration && shownDurationMs > 0) {
            Text(
                formatDurationMs(shownDurationMs),
                fontSize = 13.sp,
                color = DetailTextSecondary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (onAddToQueue != null) {
            IconButton(onClick = onAddToQueue, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Filled.PlaylistPlay,
                    contentDescription = "Add to queue",
                    tint = DetailTextPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/**
 * Pratinjau UP NEXT yang bisa digeser urutannya (v1.6).
 *
 * Tahan baris sampai terangkat (getar long-press), seret ke posisi
 * baru — garis oranye menandai posisi drop — lalu lepas: antrean
 * langsung dipindah via moveQueue. Posisi jari dihitung dari delta
 * seret (tahan terhadap transformasi), target dari tinggi baris yang
 * terukur. Scroll luar tidak berebut gesture: seret baru dimulai
 * setelah long-press, gesek biasa tetap scroll.
 */
@Composable
private fun ReorderableUpcoming(
    upcoming: List<Track>,
    currentIndex: Int,
    onMove: (Int, Int) -> Unit,
    onPlayAt: (Int) -> Unit,
    haptic: (HapticKind) -> Unit,
    /** Swipe-hapus (v1.10.16): (indeks absolut, lagu). Null = swipe nonaktif. */
    onSwipeRemove: ((Int, Track) -> Unit)? = null,
    /**
     * Multi-select (v1.10.21). Long-press dimiliki drag-to-reorder,
     * jadi masuk mode HANYA lewat tombol Select di layar detail;
     * saat mode aktif drag dinonaktifkan dan ketuk = toggle.
     */
    selectionState: com.sonara.app.ui.SelectionState? = null,
    /** Durasi per videoId (v1.10.31) — baris UP NEXT ikut cache lookupVideo. */
    durations: TrackDurations? = null,
) {
    val selecting = selectionState?.modeActive == true
    var draggingUid by remember { mutableStateOf<String?>(null) }
    var dragDeltaY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dropTarget by remember { mutableStateOf<Int?>(null) }
    var rowHeightPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var listTopPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // Posisi atas tiap baris (koordinat jendela), untuk menghitung
    // posisi jari absolut saat drag dimulai.
    val rowTops = remember { HashMap<String, Float>() }
    // Posisi atas tiap GAGANG seret (v1.10.36) — sama dengan atas
    // baris bila gagang setinggi baris, dicatat terpisah supaya
    // posisi jari tetap benar apa pun penempatan gagangnya.
    val handleTops = remember { HashMap<String, Float>() }
    var dragStartFingerY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    fun resetDrag() {
        draggingUid = null
        dragDeltaY = 0f
        dropTarget = null
    }
    fun computeTarget(): Int {
        val h = rowHeightPx
        if (h <= 0f) return -1
        val fingerY = dragStartFingerY + dragDeltaY
        return ((fingerY - listTopPx) / h).toInt()
            .coerceIn(0, upcoming.lastIndex)
    }
    // v1.10.36: callback drag dipakai DUA pemicu — long-press baris
    // (perilaku lama) DAN gagang seret yang terlihat (baru). Keduanya
    // menulis state drag yang sama; gagang hidup di luar kotak swipe
    // supaya gestur vertikalnya tidak berebut dengan swipe-hapus.
    fun beginDrag(uid: String, offset: Int, fingerY: Float) {
        draggingUid = uid
        dragDeltaY = 0f
        dropTarget = offset
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
    fun endDrag(offset: Int) {
        val target = dropTarget
        val from = currentIndex + 1 + offset
        resetDrag()
        if (target != null && target != offset) {
            haptic(HapticKind.Tap)
            onMove(from, currentIndex + 1 + target)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { listTopPx = it.boundsInWindow().top },
    ) {
        Column(Modifier.fillMaxWidth()) {
            for ((offset, next) in upcoming.withIndex()) {
                // v1.10.20: key per queueUid — WAJIB. Tanpa key, slot
                // Column dipakai ulang secara posisional: dismissState
                // (EndToStart) baris yang dihapus diwarisi baris
                // berikutnya → ghost bar merah + ikon stay (laporan HP
                // 23:09 WIB; lagu-nya sendiri benar-benar terhapus).
                // Dengan key, composable baris yang dihapus ikut
                // di-dispose bersama dismissState-nya.
                key(next.queueUid) {
                val uid = next.queueUid
                val dragging = draggingUid == uid
                // Visual baris (translasi + bayangan saat diseret)
                // menempel di wadah Row [gagang + konten], supaya
                // gagang ikut terangkat bersama barisnya. Mekanik
                // swipe/Undo/drag + key per queueUid TIDAK berubah
                // (v1.10.20: key WAJIB — tanpa key, slot Column
                // dipakai ulang secara posisional dan dismissState
                // baris yang dihapus diwarisi baris berikutnya).
                val rowVisualModifier = Modifier
                    .onGloballyPositioned { coords ->
                        rowTops[uid] = coords.boundsInWindow().top
                        if (rowHeightPx == 0f) rowHeightPx = coords.size.height.toFloat()
                    }
                    .graphicsLayer {
                        translationY = if (dragging) dragDeltaY else 0f
                        scaleX = if (dragging) 1.03f else 1f
                        scaleY = if (dragging) 1.03f else 1f
                    }
                    .shadow(if (dragging) 8.dp else 0.dp, RoundedCornerShape(Corners.small))
                    .zIndex(if (dragging) 1f else 0f)
                // Gestur long-press baris (perilaku lama, dipertahankan).
                val rowGestureModifier = Modifier
                    .pointerInput(uid, selecting) {
                        // v1.10.21: drag mati saat mode seleksi —
                        // long-press tidak boleh mencuri ketuk-toggle.
                        if (selecting) return@pointerInput
                        detectDragGesturesAfterLongPress(
                            onDragStart = { down ->
                                beginDrag(uid, offset, (rowTops[uid] ?: 0f) + down.y)
                            },
                            onDragEnd = { endDrag(offset) },
                            onDragCancel = { resetDrag() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                updateDrag(dragAmount.y)
                            },
                        )
                    }
                // Gagang seret yang terlihat (v1.10.36): pemicu drag
                // kedua, di luar kotak swipe. Disembunyikan saat mode
                // seleksi (ketuk = toggle).
                val handleModifier = if (selecting) {
                    Modifier
                } else {
                    Modifier
                        .onGloballyPositioned { coords ->
                            handleTops[uid] = coords.boundsInWindow().top
                        }
                        .pointerInput(uid, selecting) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { down ->
                                    beginDrag(
                                        uid,
                                        offset,
                                        (handleTops[uid] ?: rowTops[uid] ?: 0f) + down.y,
                                    )
                                },
                                onDragEnd = { endDrag(offset) },
                                onDragCancel = { resetDrag() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    updateDrag(dragAmount.y)
                                },
                            )
                        }
                }
                val rowContent: @Composable () -> Unit = {
                    val detailKey = selectionKey("detail", next.queueUid)
                    DetailTrackRow(
                        track = next,
                        onClick = {
                            if (selecting) selectionState?.toggle(detailKey)
                            else onPlayAt(currentIndex + 1 + offset)
                        },
                        showDuration = true,
                        selected = selectionState?.isSelected(detailKey) == true,
                        durations = durations,
                        modifier = rowGestureModifier,
                    )
                }
                Row(
                    modifier = rowVisualModifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!selecting) {
                        Box(
                            modifier = handleModifier.size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            DragHandleDots(tint = DetailTextSecondary)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        // v1.10.16: swipe-hapus — baris ini semuanya "upcoming"
                        // (bukan lagu berjalan), jadi semuanya bisa digeser.
                        if (onSwipeRemove != null) {
                            com.sonara.app.ui.SwipeToDismissDelete(
                                onDismissed = {
                                    onSwipeRemove(currentIndex + 1 + offset, next)
                                },
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
        }
        // Garis indikator posisi drop.
        val target = dropTarget
        if (draggingUid != null && target != null && rowHeightPx > 0f) {
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

/**
 * Daftar UP NEXT selama sesi Jam aktif (v1.10.10): mencerminkan
 * session queue — daftar yang SAMA dengan dialog Jam (lagu sama,
 * urutan sama, dari [com.sonara.app.data.upcomingJamQueue]), bukan
 * antrean lokal. Tanpa "View full queue" (daftar sesi sudah lengkap
 * di dialog Jam). Ketuk baris = mainkan lagu itu di sesi: host
 * melompat langsung, tamu mengirim permintaan lompat ke host
 * (v1.10.34 — tamu tidak pernah memutar lokal).
 *
 * v1.10.36: SUSUN ULANG lewat gagang seret ([ReorderableColumn]) —
 * host menyusun langsung, tamu lewat permintaan reorder ke host,
 * kedua HP konvergen lewat snapshot. Gagang satu-satunya pemicu drag
 * (ketuk baris + long-press seleksi tidak terganggu). Lagu berjalan
 * tidak ada di daftar ini, jadi tidak bisa dipindah. Baris dikunci
 * push key antrean sesi (stabil melewati lompat/autoplay/reorder,
 * supaya pelacakan konsumsi per kunci tidak rusak).
 */
@Composable
private fun SessionUpNextList(
    items: List<JamQueueItem>,
    /**
     * True bila client ini TAMU di sesi aktif — routing lompat +
     * visibilitas hapus memakai PERAN, bukan [guestLocked]
     * (v1.10.42: saat kontrol bersama NYALA, guestLocked false tapi
     * tamu tetap harus minta lompat + tetap tidak melihat hapus).
     */
    isGuest: Boolean,
    /** Host: lompat sesi ke item. */
    onPlaySessionItem: (JamQueueItem) -> Unit,
    /** Tamu: minta host melompat ke item. */
    onRequestSessionJump: (JamQueueItem) -> Unit,
    /** Hapus item (v1.10.18, host saja). Null = tombol hapus tidak tampil (tamu). */
    onRemoveSessionItem: ((JamQueueItem) -> Unit)? = null,
    /** Multi-select (v1.10.21): kunci per kunci antrean sesi. */
    selectionState: com.sonara.app.ui.SelectionState? = null,
    selectionKeys: List<String> = emptyList(),
    /** Susun ulang (v1.10.36): (dari, ke) dalam [items]. Null = tanpa reorder. */
    onMoveSessionItem: ((Int, Int) -> Unit)? = null,
) {
    val selecting = selectionState?.modeActive == true
    // v1.10.21: drag mati saat mode seleksi (gagang disembunyikan,
    // ketuk = toggle) — pola yang sama dengan ReorderableUpcoming.
    val reorderEnabled = onMoveSessionItem != null && !selecting
    ReorderableColumn(
        items = items,
        keyOf = { it.key },
        dragEnabled = reorderEnabled,
        onMove = { from, to -> onMoveSessionItem?.invoke(from, to) },
        rowContent = { item, sIndex, handleModifier ->
            val track = item.toTrack()
            val sessionKey = selectionKey(
                "jamsession",
                selectionKeys.getOrElse(sIndex) { "${item.key}#$sIndex" },
            )
            if (reorderEnabled) {
                Box(
                    modifier = handleModifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DragHandleDots(
                        tint = DetailTextSecondary,
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                DetailTrackRow(
                    track = track,
                    onClick = {
                        if (selecting) selectionState?.toggle(sessionKey)
                        else if (isGuest) onRequestSessionJump(item)
                        else onPlaySessionItem(item)
                    },
                    onLongClick = {
                        if (selecting) selectionState?.toggle(sessionKey)
                        else selectionState?.enter(sessionKey)
                    },
                    selected = selectionState?.isSelected(sessionKey) == true,
                    showDuration = false,
                )
            }
            // v1.10.21: tombol × disembunyikan saat mode seleksi
            // (ketuk baris = toggle, bukan hapus).
            if (onRemoveSessionItem != null && !selecting) {
                IconButton(
                    onClick = { onRemoveSessionItem(item) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove ${item.title} from the Jam queue",
                        tint = DetailTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    )
}

/**
 * Isi tab UP NEXT dalam mode header mini (v1.10.7): daftar antrean
 * yang SAMA dengan tampilan penuh (drag-to-reorder +
 * "View full queue"), tanpa artwork besar/pill/gelombang/kontrol
 * di atasnya — headernya sudah diseragamkan dengan LYRICS/RELATED.
 * Baris memakai DetailTrackRow (warna teks detail gelap), cocok di
 * atas panel DetailRelatedPanelBg. Isi tetap bisa di-scroll di
 * dalam areanya; posisi scroll di-hoist (upNextCompactListState).
 *
 * v1.10.10: selama sesi Jam aktif, panel ini mencerminkan session
 * queue (sama dengan dialog Jam) lewat [SessionUpNextList] — tanpa
 * reorder dan tanpa "View full queue".
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun UpNextCompactPanel(
    queue: List<Track>,
    currentIndex: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    player: PlayerManager,
    haptic: (HapticKind) -> Unit,
    onOpenQueue: () -> Unit,
    jamActive: Boolean,
    sessionQueue: List<JamQueueItem>,
    /** Peran tamu (v1.10.42) — diteruskan ke SessionUpNextList. */
    isGuest: Boolean,
    /** Host: lompat sesi ke item Up Next (v1.10.34). */
    onPlaySessionItem: (JamQueueItem) -> Unit,
    /** Tamu: minta host melompat ke item Up Next (v1.10.34). */
    onRequestSessionJump: (JamQueueItem) -> Unit,
    /** Swipe-hapus (v1.10.16). Null = nonaktif (termasuk saat sesi Jam). */
    onSwipeRemove: ((Int, Track) -> Unit)? = null,
    /** Hapus item sesi (v1.10.18, host saja). Null = tombol hapus tidak tampil. */
    onRemoveSessionItem: ((JamQueueItem) -> Unit)? = null,
    /** Susun ulang sesi (v1.10.36): (dari, ke) dalam sessionQueue. Null = tanpa reorder. */
    onMoveSessionItem: ((Int, Int) -> Unit)? = null,
    /** Multi-select (v1.10.21): state dari layar detail. */
    selectionState: com.sonara.app.ui.SelectionState? = null,
    /** Aksi bulk — toast + keluar mode ditangani layar detail. */
    onBulkAddToQueue: ((List<Track>) -> Unit)? = null,
    onBulkSave: ((List<Track>) -> Unit)? = null,
    /** Durasi per videoId (v1.10.31) — diteruskan ke ReorderableUpcoming. */
    durations: TrackDurations? = null,
) {
    val selecting = selectionState?.modeActive == true
    if (jamActive) {
        if (sessionQueue.isEmpty()) {
            Text(
                "No next song — the queue ends at this song.",
                fontSize = 13.sp,
                color = DetailTextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            return
        }
        val sessionKeys = remember(sessionQueue) {
            occurrenceKeys(sessionQueue.map { it.key })
        }
        Column(Modifier.fillMaxSize()) {
            if (!selecting) {
                CompactSelectRow(onSelect = { selectionState?.enterMode() })
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp,
                    vertical = 8.dp,
                ),
            ) {
                if (selecting) {
                    stickyHeader {
                        SelectionActionBar(
                            count = selectionState?.count ?: 0,
                            jamActive = true,
                            onAddToQueue = {
                                onBulkAddToQueue?.invoke(
                                    compactSelectedSessionTracks(
                                        selectionState, sessionQueue, sessionKeys,
                                    ),
                                )
                            },
                            onSaveToPlaylist = {
                                onBulkSave?.invoke(
                                    compactSelectedSessionTracks(
                                        selectionState, sessionQueue, sessionKeys,
                                    ),
                                )
                            },
                            onExit = { selectionState?.exitMode() },
                        )
                    }
                }
                item {
                    SessionUpNextList(
                        items = sessionQueue,
                        isGuest = isGuest,
                        onPlaySessionItem = onPlaySessionItem,
                        onRequestSessionJump = onRequestSessionJump,
                        onRemoveSessionItem = onRemoveSessionItem,
                        selectionState = selectionState,
                        selectionKeys = sessionKeys,
                        onMoveSessionItem = onMoveSessionItem,
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        return
    }
    val upcoming = queue.drop(currentIndex + 1).take(5)
    if (upcoming.isEmpty()) {
        Text(
            "No next song — the queue ends at this song.",
            fontSize = 13.sp,
            color = DetailTextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        return
    }
    Column(Modifier.fillMaxSize()) {
        if (!selecting) {
            CompactSelectRow(onSelect = { selectionState?.enterMode() })
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 8.dp,
            ),
        ) {
            if (selecting) {
                stickyHeader {
                    SelectionActionBar(
                        count = selectionState?.count ?: 0,
                        jamActive = false,
                        onAddToQueue = {
                            onBulkAddToQueue?.invoke(
                                upcoming.filter {
                                    selectionState?.isSelected(
                                        selectionKey("detail", it.queueUid),
                                    ) == true
                                },
                            )
                        },
                        onSaveToPlaylist = {
                            onBulkSave?.invoke(
                                upcoming.filter {
                                    selectionState?.isSelected(
                                        selectionKey("detail", it.queueUid),
                                    ) == true
                                },
                            )
                        },
                        onExit = { selectionState?.exitMode() },
                    )
                }
            }
            item {
                ReorderableUpcoming(
                    upcoming = upcoming,
                    currentIndex = currentIndex,
                    onMove = { from, to -> player.moveQueue(from, to) },
                    onPlayAt = { idx -> player.playAt(idx) },
                    haptic = haptic,
                    // v1.10.21: swipe mati saat mode seleksi.
                    onSwipeRemove = if (selecting) null else onSwipeRemove,
                    selectionState = selectionState,
                    durations = durations,
                )
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(onClick = onOpenQueue) {
                        Text("View full queue", color = DetailTextPrimary)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Baris tombol "Select" di atas panel UP NEXT mini (v1.10.21). */
@Composable
private fun CompactSelectRow(onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(end = 20.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onSelect) {
            Text("Select", color = DetailTextPrimary)
        }
    }
}

/** Baris tombol "Select" di atas daftar UP NEXT tampilan penuh (v1.10.21). */
@Composable
private fun DetailSelectRow(onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onSelect) {
            Text("Select", color = DetailTextPrimary)
        }
    }
}

/** Lagu sesi Jam terpilih di panel mini, urutan sesi dijaga. */
private fun compactSelectedSessionTracks(
    selectionState: com.sonara.app.ui.SelectionState?,
    sessionQueue: List<JamQueueItem>,
    sessionKeys: List<String>,
): List<Track> {
    if (selectionState == null) return emptyList()
    val out = ArrayList<Track>()
    sessionQueue.forEachIndexed { i, item ->
        if (selectionState.isSelected(selectionKey("jamsession", sessionKeys[i]))) {
            out.add(item.toTrack())
        }
    }
    return out
}

/** Panel LYRICS — kartu #3D3119, ikon not, pill Share, caption sumber.
 *
 * v1.10.6: SEMUA baris memakai SATU ukuran font + lineHeight yang
 * tetap (22sp/28sp) — tidak ada lagi pembesaran baris aktif
 * (28sp/34sp) yang menggeser teks ke bawah. Penekanan baris aktif
 * HANYA lewat warna/opacity (penuh vs redup), tanpa perubahan
 * fontWeight, sehingga tinggi total daftar stabil di posisi aktif
 * mana pun. Footer (pill Share + caption) kini berada DI DALAM
 * alur kolom di bawah daftar, bukan overlay — tidak pernah
 * menutupi baris lirik yang di-scroll.
 */
@Composable
private fun LyricsPanel(
    lyrics: List<LyricsApi.Line>,
    lyricsTried: Boolean,
    activeLyricIndex: Int,
    lyricsListState: androidx.compose.foundation.lazy.LazyListState,
    positionMs: Long,
    kind: LyricsApi.Kind,
    source: String,
    onShare: () -> Unit,
    /** Bagikan kartu lirik (v1.10.16). Null = tombol tidak tampil. */
    onShareLyricCard: (() -> Unit)? = null,
    /** Sentuhan jari pada daftar (v1.10.19): menangguhkan follow. */
    onTouchDown: () -> Unit,
    /** Jari lepas dari daftar (v1.10.19): hold 3 detik lalu follow lanjut. */
    onTouchUp: () -> Unit,
    /** Lagu yang sedang tampil (v1.0.4: kunci edit/offset). */
    track: Track?,
    store: LocalStore,
    /** Preferensi sumber lirik aktif (v1.0.4). */
    sourcePref: LyricsApi.LyricsSource,
    onSourcePrefChange: (LyricsApi.LyricsSource) -> Unit,
    /** True bila baris yang tampil adalah hasil edit lokal (v1.0.4). */
    lyricsEdited: Boolean,
    onLyricsEdited: () -> Unit,
    perSongOffsetMs: Long,
    globalOffsetMs: Long,
    onOffsetSaved: () -> Unit,
    /** Buka tampilan lirik fullscreen (v1.0.4). */
    onOpenFullscreen: () -> Unit,
) {
    // v1.0.4: cari / edit / offset / sumber — state lokal panel.
    var showFind by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findPos by remember { mutableStateOf(-1) }
    var showEdit by remember { mutableStateOf(false) }
    var showOffset by remember { mutableStateOf(false) }
    var showSource by remember { mutableStateOf(false) }
    val panelScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val findMatches = remember(lyrics, findQuery) {
        LyricsFindPolicy.findMatches(lyrics, findQuery)
    }
    // Lompat ke baris hasil cari: pusatkan dengan rumus yang sama
    // dengan follow (LyricsFollowState) — bukan scroll mentah.
    fun jumpToLyric(index: Int) {
        panelScope.launch {
            val info = lyricsListState.layoutInfo
            val viewportH = info.viewportSize.height
            val beforePad = info.beforeContentPadding
            val itemH = info.visibleItemsInfo.map { it.size }.average()
                .takeIf { it > 0 }?.toInt()
                ?: with(density) { 76.dp.roundToPx() }
            if (viewportH > 0) {
                lyricsListState.scrollToItem(
                    index,
                    LyricsFollowState.centerScrollOffset(viewportH, itemH, beforePad),
                )
            } else {
                lyricsListState.scrollToItem(index)
            }
        }
    }
    fun jumpToMatch(forward: Boolean) {
        if (findMatches.isEmpty()) return
        val nextPos = if (findPos < 0) 0 else {
            val target = LyricsFindPolicy.nextMatch(findMatches, findPos, forward)
            findMatches.indexOf(target)
        }
        findPos = nextPos
        jumpToLyric(findMatches[nextPos])
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = DetailTextSecondary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.weight(1f))
            // v1.0.4: cari di lirik.
            if (lyrics.isNotEmpty()) {
                IconButton(
                    onClick = {
                        showFind = !showFind
                        if (!showFind) {
                            findQuery = ""
                            findPos = -1
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Find in lyrics",
                        tint = DetailTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                // v1.0.4: edit lirik (lokal saja).
                IconButton(
                    onClick = { showEdit = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit lyrics",
                        tint = DetailTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                // v1.0.4: lirik fullscreen.
                IconButton(
                    onClick = onOpenFullscreen,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Fullscreen lyrics",
                        tint = DetailTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            // v1.0.4: offset timing lirik.
            IconButton(
                onClick = { showOffset = true },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Lyrics timing offset",
                    tint = DetailTextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            // Tombol kartu lirik: hanya bila ada lirik yang bisa
            // dibagikan (synced/plain). Tanpa lirik → tidak tampil.
            if (onShareLyricCard != null && lyrics.isNotEmpty()) {
                IconButton(
                    onClick = onShareLyricCard,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share lyric card",
                        tint = DetailTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        // v1.0.4: baris sumber + penanda edit/offset.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { showSource = true },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text(
                    text = when (sourcePref) {
                        LyricsApi.LyricsSource.AUTO -> "Source: Auto"
                        LyricsApi.LyricsSource.LRCLIB -> "Source: LRCLIB"
                        LyricsApi.LyricsSource.YOUTUBE_MUSIC -> "Source: YouTube Music"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailTextSecondary,
                )
            }
            if (lyricsEdited) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "Edited",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailAccent,
                )
            }
            val totalOffset = globalOffsetMs + perSongOffsetMs
            if (totalOffset != 0L) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "Offset ${LyricsOffsetPolicy.offsetLabel(totalOffset)}",
                    fontSize = 12.sp,
                    color = DetailTextSecondary,
                )
            }
        }
        // v1.0.4: baris cari — hitungan + naik/turun antar hasil.
        if (showFind && lyrics.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = {
                        findQuery = it
                        findPos = -1
                    },
                    singleLine = true,
                    placeholder = { Text("Find in lyrics", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { jumpToMatch(forward = true) },
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = DetailAccent,
                        unfocusedIndicatorColor = DetailTextSecondary.copy(alpha = 0.4f),
                        cursorColor = DetailTextPrimary,
                        focusedTextColor = DetailTextPrimary,
                        unfocusedTextColor = DetailTextPrimary,
                        focusedPlaceholderColor = DetailTextSecondary,
                        unfocusedPlaceholderColor = DetailTextSecondary,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (findQuery.isBlank()) "" else if (findMatches.isEmpty()) "0 found"
                    else "${if (findPos < 0) 1 else findPos + 1} of ${findMatches.size}",
                    fontSize = 13.sp,
                    color = DetailTextSecondary,
                )
                IconButton(
                    onClick = { jumpToMatch(forward = false) },
                    enabled = findMatches.isNotEmpty(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Previous match",
                        tint = DetailTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = { jumpToMatch(forward = true) },
                    enabled = findMatches.isNotEmpty(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Next match",
                        tint = DetailTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        if (!lyricsTried) {
            Text("Loading lyrics…", fontSize = 14.sp, color = DetailTextSecondary)
        } else if (lyrics.isEmpty()) {
            Text(
                "No lyrics available for this song.",
                fontSize = 14.sp,
                color = DetailTextSecondary,
            )
        } else {
            // v1.10.19: padding konten setengah viewport di atas/bawah
            // agar baris PERTAMA dan TERAKHIR pun bisa dipusatkan
            // (seperti YTM — tanpa padding, scroll mentok
            // sebelum baris tepi mencapai tengah). Padding diukur dari
            // tinggi area daftar yang sebenarnya (BoxWithConstraints).
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val halfViewport = maxHeight / 2
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        // Deteksi sentuh manual (v1.10.19): mengamati
                        // event Press/Release TANPA consume — gesture
                        // scroll daftar tidak terganggu. Scroll
                        // programatik tidak membangkitkan event ini.
                        .pointerInput(onTouchDown, onTouchUp) {
                            // awaitPointerEventScope = member PointerInputScope
                            // (versi Compose proyek — tanpa import).
                            // Tidak ada PointerEventType.Cancel di versi
                            // ini: lepas terdeteksi dari Release, atau dari
                            // event apa pun tanpa pointer tertekan setelah
                            // Press (gesture dibatalkan sistem).
                            awaitPointerEventScope {
                                var touching = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            touching = true
                                            onTouchDown()
                                        }
                                        PointerEventType.Release -> {
                                            if (touching) {
                                                touching = false
                                                onTouchUp()
                                            }
                                        }
                                        else -> {
                                            if (touching && event.changes.none { it.pressed }) {
                                                touching = false
                                                onTouchUp()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    state = lyricsListState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = halfViewport,
                        bottom = halfViewport,
                    ),
                ) {
                itemsIndexed(lyrics) { index, line ->
                    // Lirik POLOS tidak punya baris aktif — semua
                    // baris tampil terang merata (bukan redup seperti
                    // baris non-aktif lirik sinkron).
                    val isPlain = kind == LyricsApi.Kind.PLAIN
                    val active = !isPlain && index == activeLyricIndex
                    // Polish v1.10.18: SEMUA baris 22sp/28sp SERAGAM
                    // (keputusan 10.6 + batas keras keluhan 13:19 —
                    // tinggi daftar stabil, teks tidak kegeser).
                    // Yang berubah: font Plus Jakarta Sans,
                    // letterSpacing, crossfade warna, dan aksen
                    // bar oranye di kolom tetap 14dp (kolom dicadangkan
                    // untuk SEMUA baris — tidak ada pergeseran layout
                    // saat baris menjadi aktif).
                    // v1.10.22: crossfade 350ms → Motion.slowMs (450ms,
                    // FastOutSlowIn implisit di tween) agar pergantian
                    // baris aktif terbaca sebagai animasi yang tenang,
                    // selaras dengan easing gulir follow. Font/
                    // lineHeight TETAP; penekanan HANYA warna/opacity.
                    val targetColor = if (active) DetailTextPrimary
                    else if (isPlain) DetailTextPrimary.copy(alpha = 0.9f)
                    else DetailTextPrimary.copy(alpha = 0.45f)
                    val animatedColor by animateColorAsState(
                        targetValue = targetColor,
                        animationSpec = tween(Motion.slowMs),
                        label = "lyricLineColor",
                    )
                    val barColor by animateColorAsState(
                        targetValue = if (active) DetailAccent else Color.Transparent,
                        animationSpec = tween(Motion.slowMs),
                        label = "lyricActiveBar",
                    )
                    // v1.0.4: sorot hasil cari — hasil saat ini lebih
                    // kuat; hasil lain samar. Tanpa cari = transparan.
                    val matchPos = findMatches.indexOf(index)
                    val isCurrentMatch = showFind && matchPos >= 0 && matchPos == findPos
                    val isMatch = showFind && matchPos >= 0
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isCurrentMatch) DetailAccent.copy(alpha = 0.18f)
                                else if (isMatch) DetailTextPrimary.copy(alpha = 0.07f)
                                else Color.Transparent,
                            )
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.width(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(width = 4.dp, height = 20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor),
                            )
                        }
                        if (active && line.words.isNotEmpty()) {
                            // Highlight PER KATA (v1.6), warna saja:
                            // kata yang sudah dinyanyikan menyala penuh,
                            // sisanya redup — tanpa perubahan ukuran/bobot.
                            // Timing per kata ASLI dari sumber lirik.
                            val annotated = buildAnnotatedString {
                                for (word in line.words) {
                                    val sung = positionMs >= word.timeMs
                                    withStyle(
                                        SpanStyle(
                                            color = if (sung) DetailTextPrimary
                                            else DetailTextPrimary.copy(alpha = 0.45f),
                                        ),
                                    ) {
                                        append(word.text)
                                    }
                                }
                            }
                            Text(
                                text = annotated,
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontFamily = LyricsFontFamily,
                                letterSpacing = 0.3.sp,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Text(
                                text = line.text,
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontFamily = LyricsFontFamily,
                                letterSpacing = 0.3.sp,
                                color = animatedColor,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
                }
            }
            // Footer di dalam alur (bukan overlay): pill Share di
            // tengah + caption sumber di bawahnya.
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(Corners.pill))
                    .background(DetailTextPrimary)
                    .clickable(onClick = onShare)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = DetailBgFallback,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailBgFallback,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    if (lyricsEdited) append("Edited on this device. ")
                    if (kind == LyricsApi.Kind.PLAIN) {
                        append("Untimed lyrics (from $source). ")
                        append("Lines don't follow the song position.")
                    } else if (lyrics.isEmpty()) {
                        append("No lyrics available for this song.")
                    } else {
                        append("Synced lyrics ($source) — the active line follows the song position. Try ")
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("jumping with the wave")
                        }
                        append(" Garis Arus")
                    }
                },
                fontSize = 13.sp,
                color = DetailTextSecondary.copy(alpha = 0.8f),
            )
        }
        // v1.0.4: dialog cari/edit/offset/sumber — di luar rantai
        // if/else di atas supaya tetap bisa dibuka walau lirik kosong
        // (offset) atau saat loading.
        if (showEdit && track != null && lyrics.isNotEmpty()) {
            LyricsEditDialog(
                lines = lyrics,
                kind = kind,
                edited = lyricsEdited,
                onDismiss = { showEdit = false },
                onSave = { texts ->
                    val edited = LyricsEditCodec.applyEdit(lyrics, texts)
                    if (edited.isNotEmpty()) {
                        store.saveLyricsEdit(
                            track.id,
                            com.sonara.app.data.LyricsEdit(
                                kind = kind,
                                lines = edited,
                                savedAtMs = System.currentTimeMillis(),
                            ),
                        )
                    }
                    showEdit = false
                    onLyricsEdited()
                },
                onReset = {
                    store.clearLyricsEdit(track.id)
                    showEdit = false
                    onLyricsEdited()
                },
            )
        }
        if (showOffset && track != null) {
            LyricsOffsetDialog(
                perSongOffsetMs = perSongOffsetMs,
                globalOffsetMs = globalOffsetMs,
                onDismiss = { showOffset = false },
                onSave = { ms ->
                    store.setLyricsOffsetMs(track.id, ms)
                    showOffset = false
                    onOffsetSaved()
                },
            )
        }
        if (showSource) {
            LyricsSourceDialog(
                current = sourcePref,
                onDismiss = { showSource = false },
                onSelect = { pref ->
                    showSource = false
                    onSourcePrefChange(pref)
                },
            )
        }
    }
}

/**
 * Dialog edit lirik (v1.0.4): satu field per baris, timestamp
 * dipertahankan per baris; baris yang dikosongkan dibuang saat
 * disimpan. Edit HANYA lokal di perangkat ini — tidak disinkron,
 * tidak mengubah lirik di sumber mana pun.
 */
@Composable
private fun LyricsEditDialog(
    lines: List<LyricsApi.Line>,
    kind: LyricsApi.Kind,
    edited: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    onReset: () -> Unit,
) {
    var texts by remember { mutableStateOf(lines.map { it.text }) }
    val canSave = texts.any { it.isNotBlank() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
        ) {
            Text("Edit lyrics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Changes stay on this device only. Line timings are kept; " +
                    "empty lines are removed when saved.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                texts.forEachIndexed { i, text ->
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v ->
                            texts = texts.toMutableList().also { it[i] = v }
                        },
                        singleLine = true,
                        label = {
                            Text(
                                "Line ${i + 1}" +
                                    if (kind == LyricsApi.Kind.SYNCED) {
                                        " · ${formatDurationMs(lines[i].timeMs)}"
                                    } else {
                                        ""
                                    },
                                fontSize = 12.sp,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = BurntOrange,
                            cursorColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (edited) {
                    TextButton(
                        onClick = onReset,
                        colors = sonaraTextButtonColors(),
                    ) { Text("Reset to fetched") }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = sonaraTextButtonColors(),
                ) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(texts) },
                    enabled = canSave,
                ) { Text("Save") }
            }
        }
    }
}

/**
 * Dialog offset timing per-lagu (v1.0.4): geser lirik ±10 detik
 * langkah 0,5 detik. Offset global (Settings → Playback) ditampilkan
 * sebagai konteks — yang diubah di sini hanya lagu ini.
 */
@Composable
private fun LyricsOffsetDialog(
    perSongOffsetMs: Long,
    globalOffsetMs: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var seconds by remember { mutableStateOf(perSongOffsetMs / 1000f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics timing for this song") },
        text = {
            Column {
                Text(
                    "Shift the lyrics earlier or later for this song only. " +
                        "Positive = lyrics appear later.",
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    LyricsOffsetPolicy.offsetLabel(
                        (seconds * 1000).toLong(),
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = seconds,
                    onValueChange = { seconds = it },
                    valueRange = -10f..10f,
                    steps = 39,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = BurntOrange,
                        activeTrackColor = BurntOrange,
                    ),
                )
                Text(
                    "Global offset: " +
                        LyricsOffsetPolicy.offsetLabel(globalOffsetMs) +
                        " (Settings → Playback).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave((seconds * 1000).toLong()) },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = sonaraTextButtonColors(),
            ) { Text("Cancel") }
        },
    )
}

/**
 * Dialog pemilih sumber lirik (v1.0.4): Auto (perilaku lama), LRCLIB
 * saja, atau YouTube Music saja. Pilihan eksplisit tidak jatuh ke
 * sumber lain — kosong dari sumber itu tampil kosong dengan jujur.
 */
@Composable
private fun LyricsSourceDialog(
    current: LyricsApi.LyricsSource,
    onDismiss: () -> Unit,
    onSelect: (LyricsApi.LyricsSource) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics source") },
        text = {
            Column {
                for (option in LyricsApi.LyricsSource.entries) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onSelect(option) },
                    ) {
                        RadioButton(
                            selected = current == option,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BurntOrange,
                            ),
                        )
                        Column {
                            Text(
                                when (option) {
                                    LyricsApi.LyricsSource.AUTO -> "Auto"
                                    LyricsApi.LyricsSource.LRCLIB -> "LRCLIB"
                                    LyricsApi.LyricsSource.YOUTUBE_MUSIC -> "YouTube Music"
                                },
                                fontSize = 15.sp,
                            )
                            Text(
                                when (option) {
                                    LyricsApi.LyricsSource.AUTO ->
                                        "Synced lyrics first, then YouTube Music."
                                    LyricsApi.LyricsSource.LRCLIB ->
                                        "Synced or plain lyrics from LRCLIB only."
                                    LyricsApi.LyricsSource.YOUTUBE_MUSIC ->
                                        "Untimed lyrics from YouTube Music only."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = sonaraTextButtonColors(),
            ) { Text("Cancel") }
        },
    )
}

/**
 * Lirik fullscreen (v1.0.4): daftar sendiri dengan follow memakai
 * rumus pemusatan YANG SAMA dengan panel tab (LyricsFollowState —
 * centerScrollOffset + isNearCenter, satu koreksi). Gaya Standard /
 * Large hanya mengubah ukuran teks. Warna memakai token stage detail
 * yang sama, jadi tampil konsisten di semua 14 varian tema.
 */
@Composable
private fun LyricsFullscreenDialog(
    lyrics: List<LyricsApi.Line>,
    kind: LyricsApi.Kind,
    source: String,
    activeLyricIndex: Int,
    positionMs: Long,
    lyricsEdited: Boolean,
    onDismiss: () -> Unit,
) {
    var style by remember { mutableStateOf(LyricsStyle.STANDARD) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    BackHandler { onDismiss() }
    // Follow: pusatkan baris aktif saat berpindah, verifikasi sekali
    // dari layout segar (pola worker tab, versi ringkas).
    LaunchedEffect(activeLyricIndex) {
        if (activeLyricIndex < 0) return@LaunchedEffect
        kotlinx.coroutines.delay(60)
        val info = listState.layoutInfo
        val viewportH = info.viewportSize.height
        if (viewportH <= 0) return@LaunchedEffect
        val beforePad = info.beforeContentPadding
        val itemH = info.visibleItemsInfo.firstOrNull { it.index == activeLyricIndex }?.size
            ?: info.visibleItemsInfo.map { it.size }.average().takeIf { it > 0 }?.toInt()
            ?: with(density) { style.lineHeightSp.sp.roundToPx() + 20.dp.roundToPx() }
        listState.scrollToItem(
            activeLyricIndex,
            LyricsFollowState.centerScrollOffset(viewportH, itemH, beforePad),
        )
        kotlinx.coroutines.delay(LyricsFollowState.FOLLOW_VERIFY_DELAY_MS)
        val fresh = listState.layoutInfo
        val item = fresh.visibleItemsInfo.firstOrNull { it.index == activeLyricIndex }
        if (item != null && !LyricsFollowState.isNearCenter(
                item.offset, item.size, fresh.viewportSize.height,
                with(density) { 16.dp.roundToPx() }, fresh.beforeContentPadding,
            )
        ) {
            val distance = LyricsFollowState.centerDistancePx(
                item.offset, item.size, fresh.viewportSize.height,
                fresh.beforeContentPadding,
            )
            listState.animateScrollBy(distance.toFloat())
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(DetailBgFallback)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close fullscreen lyrics",
                        tint = DetailTextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    "Lyrics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                for (option in LyricsStyle.entries) {
                    val selected = style == option
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(Corners.pill))
                            .background(
                                if (selected) DetailTextPrimary
                                else Color.Transparent,
                            )
                            .clickable { style = option }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (option == LyricsStyle.STANDARD) "Standard" else "Large",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) DetailBgFallback else DetailTextSecondary,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
            }
            if (lyrics.isEmpty()) {
                Text(
                    "No lyrics available for this song.",
                    fontSize = 14.sp,
                    color = DetailTextSecondary,
                )
            } else {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val halfViewport = maxHeight / 2
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = halfViewport,
                            bottom = halfViewport,
                        ),
                    ) {
                        itemsIndexed(lyrics) { index, line ->
                            val isPlain = kind == LyricsApi.Kind.PLAIN
                            val active = !isPlain && index == activeLyricIndex
                            val targetColor = if (active) DetailTextPrimary
                            else if (isPlain) DetailTextPrimary.copy(alpha = 0.9f)
                            else DetailTextPrimary.copy(alpha = 0.45f)
                            val animatedColor by animateColorAsState(
                                targetValue = targetColor,
                                animationSpec = tween(Motion.slowMs),
                                label = "fullscreenLyricColor",
                            )
                            val barColor by animateColorAsState(
                                targetValue = if (active) DetailAccent else Color.Transparent,
                                animationSpec = tween(Motion.slowMs),
                                label = "fullscreenLyricBar",
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.width(14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        Modifier
                                            .size(width = 4.dp, height = 20.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(barColor),
                                    )
                                }
                                if (active && line.words.isNotEmpty()) {
                                    val annotated = buildAnnotatedString {
                                        for (word in line.words) {
                                            val sung = positionMs >= word.timeMs
                                            withStyle(
                                                SpanStyle(
                                                    color = if (sung) DetailTextPrimary
                                                    else DetailTextPrimary.copy(alpha = 0.45f),
                                                ),
                                            ) {
                                                append(word.text)
                                            }
                                        }
                                    }
                                    Text(
                                        text = annotated,
                                        fontSize = style.fontSizeSp.sp,
                                        lineHeight = style.lineHeightSp.sp,
                                        fontFamily = LyricsFontFamily,
                                        letterSpacing = 0.3.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Text(
                                        text = line.text,
                                        fontSize = style.fontSizeSp.sp,
                                        lineHeight = style.lineHeightSp.sp,
                                        fontFamily = LyricsFontFamily,
                                        letterSpacing = 0.3.sp,
                                        color = animatedColor,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = buildString {
                        if (lyricsEdited) append("Edited on this device. ")
                        if (kind == LyricsApi.Kind.PLAIN) {
                            append("Untimed lyrics (from $source).")
                        } else {
                            append("Synced lyrics ($source).")
                        }
                    },
                    fontSize = 13.sp,
                    color = DetailTextSecondary.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/**
 * Panel RELATED — "You might also like" dari pencarian artis yang sama
 * (data nyata YTM) + "Recommended playlists" dari playlist NYATA user
 * (playlist lokal). Kolase memakai cover lagu terkait sebagai hiasan;
 * nama + jumlah lagu playlist adalah data asli.
 */
@Composable
private fun RelatedPanel(
    related: List<Track>,
    track: Track,
    state: SonaraState,
    scrollState: androidx.compose.foundation.ScrollState,
) {
    val context = LocalContext.current
    val localPlaylists = state.store.playlists()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            "You might also like",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = DetailTextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        if (related.isEmpty()) {
            Text(
                "No recommendations for this artist yet.",
                fontSize = 14.sp,
                color = DetailTextSecondary,
            )
        } else {
            for (rel in related) {
                DetailTrackRow(
                    track = rel,
                    onClick = { state.player.playTrack(rel) },
                    onAddToQueue = {
                        state.player.addToQueue(rel)
                        showToast(context, "Added to queue")
                    },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Recommended playlists",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = DetailTextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        if (localPlaylists.isEmpty()) {
            Text(
                "No playlists yet — create one in Library.",
                fontSize = 14.sp,
                color = DetailTextSecondary,
            )
        } else {
            LazyRow {
                items(localPlaylists.size) { i ->
                    val pl = localPlaylists[i]
                    val covers = resolvePlaylistCovers(pl.trackIds, related, state)
                    PlaylistCollageCard(
                        name = pl.name,
                        meta = "${pl.trackIds.size} songs • My playlists",
                        covers = covers,
                        onClick = {
                            val tracks = resolvePlaylistTracks(pl.trackIds, related, state)
                            if (tracks.isNotEmpty()) {
                                state.player.playQueue(tracks, 0)
                            } else {
                                showToast(context, "This playlist is still empty")
                            }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Kartu kolase 2×2 untuk satu playlist di panel RELATED. */
@Composable
private fun PlaylistCollageCard(
    name: String,
    meta: String,
    covers: List<Track>,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(168.dp)
            .padding(end = 12.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(168.dp)
                .clip(RoundedCornerShape(Corners.card))
                .background(DetailTextPrimary.copy(alpha = 0.08f)),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    CollageCell(covers.getOrNull(0), Modifier.weight(1f))
                    CollageCell(covers.getOrNull(1), Modifier.weight(1f))
                }
                Row(Modifier.weight(1f)) {
                    CollageCell(covers.getOrNull(2), Modifier.weight(1f))
                    CollageCell(covers.getOrNull(3), Modifier.weight(1f))
                }
            }
            // Avatar inisial di sudut kiri bawah kolase (sesuai referensi).
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AccentFill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "M",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnAccent,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DetailTextPrimary,
            maxLines = 1,
        )
        Text(meta, fontSize = 12.sp, color = DetailTextSecondary, maxLines = 1)
    }
}

@Composable
private fun CollageCell(track: Track?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DetailTextPrimary.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        if (track != null) {
            CoverArt(track = track, size = 84.dp, corner = 0.dp)
        } else {
            Text(
                "♪",
                fontSize = 24.sp,
                color = DetailTextSecondary,
            )
        }
    }
}

/** Resolve track playlist dari lagu yang dikenal: disukai + terkait. */
private fun resolvePlaylistTracks(
    ids: List<String>,
    related: List<Track>,
    state: SonaraState,
): List<Track> {
    val known = state.store.likedTracks() + related
    return ids.mapNotNull { id -> known.find { it.id == id } }
}

private fun resolvePlaylistCovers(
    ids: List<String>,
    related: List<Track>,
    state: SonaraState,
): List<Track> {
    val resolved = resolvePlaylistTracks(ids, related, state)
    return (resolved + related).distinctBy { it.id }.take(4)
}

/** Bagikan lagu — teks + tautan YouTube Music aslinya (format sama dengan sheet, v1.10.4). */
private fun shareTrack(context: Context, track: Track) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareTextFor(track))
    }
    context.startActivity(Intent.createChooser(intent, "Share song"))
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun CrashReportScreen(
    reportText: String,
    handlerMarker: String? = null,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        // v1.10.1: tombol Back di kiri atas.
        ArusBackButton(onClick = onBack)
        Spacer(Modifier.height(8.dp))
        Text("Crash Report", fontSize = Type.screenTitle, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            handlerMarker ?: "Crash handler installed (check the marker when a report exists).",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(reportText, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Row {
            Button(
                onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("SONARA Crash Report", reportText),
                    )
                    showToast(context, "Report copied")
                },
                modifier = Modifier.height(48.dp),
            ) { Text("Copy Report") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onDelete, modifier = Modifier.height(48.dp)) { Text("Delete") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onContinue, modifier = Modifier.height(48.dp)) { Text("Continue") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
