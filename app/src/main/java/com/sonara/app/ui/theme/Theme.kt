package com.sonara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.R
import com.sonara.app.data.AppBodyFont
import com.sonara.app.data.AppDisplayFont
import com.sonara.app.data.AppLabelFont
import com.sonara.app.data.AppMotionEasing
import com.sonara.app.data.AppThemeId
import com.sonara.app.data.AppThemeMode
import com.sonara.app.data.AppThemePalette
import com.sonara.app.data.CustomThemeOverride
import com.sonara.app.data.CustomThemePolicy
import com.sonara.app.data.paletteFor
import com.sonara.app.data.playDiscPair

/**
 * Token tema v1.10.26 — SATU sumber kebenaran visual.
 *
 * Nilai WARNA hidup di [AppThemePalette] (data/AppTheme.kt, murni
 * Kotlin, diperiksa harness kontras); di sini hanya konversi ke
 * Compose + CompositionLocal. Nama-nama lama (PaperBg, InkBlack,
 * BurntOrange, ...) dipertahankan sebagai getter @Composable supaya
 * ~300 call site tidak berubah makna: getter = peran yang sama di
 * tema aktif. Pengecualian: peran yang dulu digabung dalam satu val
 * kini dipisah — lihat [ChromeBg]/[StageBg] (permukaan gelap),
 * [AccentFill] (isi tombol) dan [DetailAccent] (aksen di panggung
 * gelap). Warna dekorasi tetap (Genre*, Detail*, Wave*, SourceDot*)
 * TIDAK bertema — nilainya identitas artwork/panggung, bukan
 * struktur, dan didokumentasikan sebagai allowlist di BUILD doc.
 */

/** Seluruh token visual satu varian tema (4 tema x 2 mode). */
data class SonaraTokens(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val text: Color,
    val textMuted: Color,
    val outline: Color,
    val accent: Color,
    val accentFill: Color,
    val accentDark: Color,
    val accentSoft: Color,
    val accentOnDark: Color,
    val onAccent: Color,
    /** Isi + ikon cakram play mini player (v1.0.1, dari [playDiscPair]). */
    val playDiscFill: Color,
    val playDiscTint: Color,
    val chrome: Color,
    val onChrome: Color,
    val stage: Color,
    val avatar: Color,
    val onAvatar: Color,
    val poster: Color,
    val posterFallback: Color,
    val error: Color,
    val cornerSmall: Dp,
    val cornerMedium: Dp,
    val cornerLarge: Dp,
    val cornerPoster: Dp,
    val cornerPill: Dp,
    val cornerRowCover: Dp,
    val cornerCard: Dp,
    val cornerTile: Dp,
    val cornerChip: Dp,
    val cornerSheet: Dp,
    val cornerButton: Dp,
    val cornerMiniPlayer: Dp,
    /** Radius cover detail (RASI = bundar 999; tema lain = radius poster). */
    val cornerDetailCover: Dp,
    val rowPaddingV: Dp,
    val rowMinHeight: Dp,
    val sectionPaddingV: Dp,
    val sectionTopPadding: Dp,
    val cardPadding: Dp,
    val gridGap: Dp,
    val headerPaddingV: Dp,
    val sectionTitleSize: androidx.compose.ui.unit.TextUnit,
    val posterTitleSize: androidx.compose.ui.unit.TextUnit,
    val detailTitleSize: androidx.compose.ui.unit.TextUnit,
    val labelLetterSpacing: androidx.compose.ui.unit.TextUnit,
    val displayLetterSpacing: androidx.compose.ui.unit.TextUnit,
    val cardBorderWidth: Dp,
    val dividerThickness: Dp,
    val dividerAlpha: Float,
    val hardShadowPoster: Dp,
    val hardShadowTile: Dp,
    val hardShadowChip: Dp,
    val glowElevation: Dp,
    val glowAlpha: Float,
    val uppercaseTitles: Boolean,
    val hardShadow: Boolean,
    val flowField: Boolean,
    // --- Treatment v1.10.37 (RASI/PASAR/SENYAP) — dibaca composable
    // treatment di UiCommon/SonaraApp; tema lama semuanya false.
    val orbitRing: Boolean,
    val dotGridBackground: Boolean,
    val constellationLines: Boolean,
    val dashedBorders: Boolean,
    val ticketNotches: Boolean,
    val stampBadge: Boolean,
    val flatNoCards: Boolean,
    val textTabNav: Boolean,
    val chartNumerals: Boolean,
    val displayFont: FontFamily,
    val bodyFont: FontFamily,
    val labelFont: FontFamily,
    val motionEasing: AppMotionEasing,
    val morphMs: Int,
    val tabSlideMs: Int,
    val panelFadeMs: Int,
    val panelExitMs: Int,
    val tintMs: Int,
    val waveAmpMs: Int,
    val themeId: AppThemeId,
    // --- Klaster bawah SLIM v1.10.29 (dari AppThemePalette; total
    // tumpukan 130dp — lihat BottomCluster di bawah).
    val clusterWaveHeight: Dp,
    val clusterPlayerHeight: Dp,
    val clusterPlayerCover: Dp,
    val clusterPlayerAction: Dp,
    val clusterPlayerControl: Dp,
    val clusterPlayerControlIcon: Dp,
    val clusterPlayerLikeIcon: Dp,
    val clusterPlayerPlayIcon: Dp,
    val clusterPlayerTitleSize: androidx.compose.ui.unit.TextUnit,
    val clusterPlayerArtistSize: androidx.compose.ui.unit.TextUnit,
    val clusterPlayerPaddingH: Dp,
    val clusterPlayerGap: Dp,
    val clusterNavHeight: Dp,
    val clusterNavIcon: Dp,
    val clusterNavLabelSize: androidx.compose.ui.unit.TextUnit,
    val clusterNavItemHeight: Dp,
    val clusterNavPaddingV: Dp,
    val clusterGap: Dp,
    val clusterBottomInset: Dp,
    val clusterJamChipHeight: Dp,
    val clusterJamChipGap: Dp,
    val clusterJamChipIcon: Dp,
)

/** Plus Jakarta Sans: Regular (lama) + Bold (baru, v1.10.26). */
val JakartaFontFamily: FontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

/** Bricolage Grotesque ExtraBold — display Blok (OFL, instans statis). */
val BricolageFontFamily: FontFamily = FontFamily(
    Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
)

/** Archivo Bold — display GETAR (OFL, instans statis). */
val ArchivoFontFamily: FontFamily = FontFamily(
    Font(R.font.archivo_bold, FontWeight.Bold),
)

/** JetBrains Mono — label mono GETAR (OFL, instans statis). */
val JetBrainsMonoFontFamily: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/** Space Grotesk — display + body RASI (OFL, instans statis). */
val SpaceGroteskFontFamily: FontFamily = FontFamily(
    Font(R.font.space_grotesk_regular),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/** Anton — display PASAR (OFL, satu bobot). */
val AntonFontFamily: FontFamily = FontFamily(
    Font(R.font.anton_regular),
)

/** Fraunces — display SENYAP (OFL, instans statis opsz 144). */
val FrauncesFontFamily: FontFamily = FontFamily(
    Font(R.font.fraunces_light, FontWeight.Light),
    Font(R.font.fraunces_black, FontWeight.Black),
)

/** Inter — body + label PASAR + SENYAP (OFL, instans statis). */
val InterFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** IBM Plex Mono — label koordinat RASI (OFL, instans statis). */
val PlexMonoFontFamily: FontFamily = FontFamily(
    Font(R.font.ibm_plex_mono_regular),
    Font(R.font.ibm_plex_mono_bold, FontWeight.Bold),
)

fun AppThemePalette.toTokens(themeId: AppThemeId): SonaraTokens = SonaraTokens(
    background = Color(background),
    surface = Color(surface),
    surfaceVariant = Color(surfaceVariant),
    text = Color(text),
    textMuted = Color(textMuted),
    outline = Color(outline),
    accent = Color(accent),
    accentFill = Color(accentFill),
    accentDark = Color(accentDark),
    accentSoft = Color(accentSoft),
    accentOnDark = Color(accentOnDark),
    onAccent = Color(onAccent),
    playDiscFill = Color(playDiscPair(this).first),
    playDiscTint = Color(playDiscPair(this).second),
    chrome = Color(chrome),
    onChrome = Color(onChrome),
    stage = Color(stage),
    avatar = Color(avatar),
    onAvatar = Color(onAvatar),
    poster = Color(poster),
    posterFallback = Color(posterFallback),
    error = Color(error),
    cornerSmall = cornerSmall.dp,
    cornerMedium = cornerMedium.dp,
    cornerLarge = cornerLarge.dp,
    cornerPoster = cornerPoster.dp,
    cornerPill = cornerPill.dp,
    cornerRowCover = cornerRowCover.dp,
    cornerCard = cornerCard.dp,
    cornerTile = cornerTile.dp,
    cornerChip = cornerChip.dp,
    cornerSheet = cornerSheet.dp,
    cornerButton = cornerButton.dp,
    cornerMiniPlayer = cornerMiniPlayer.dp,
    cornerDetailCover = if (cornerDetailCover >= 0) cornerDetailCover.dp else cornerPoster.dp,
    rowPaddingV = rowPaddingV.dp,
    rowMinHeight = rowMinHeight.dp,
    sectionPaddingV = sectionPaddingV.dp,
    sectionTopPadding = sectionTopPadding.dp,
    cardPadding = cardPadding.dp,
    gridGap = gridGap.dp,
    headerPaddingV = headerPaddingV.dp,
    sectionTitleSize = sectionTitleSize.sp,
    posterTitleSize = posterTitleSize.sp,
    detailTitleSize = detailTitleSize.sp,
    labelLetterSpacing = (labelLetterSpacingTenths / 10f).sp,
    displayLetterSpacing = (displayLetterSpacingTenths / 10f).sp,
    cardBorderWidth = cardBorderWidth.dp,
    dividerThickness = dividerThickness.dp,
    dividerAlpha = dividerAlphaPercent / 100f,
    hardShadowPoster = hardShadowPoster.dp,
    hardShadowTile = hardShadowTile.dp,
    hardShadowChip = hardShadowChip.dp,
    glowElevation = glowElevation.dp,
    glowAlpha = glowAlphaPercent / 100f,
    uppercaseTitles = uppercaseTitles,
    hardShadow = hardShadow,
    flowField = flowField,
    orbitRing = orbitRing,
    dotGridBackground = dotGridBackground,
    constellationLines = constellationLines,
    dashedBorders = dashedBorders,
    ticketNotches = ticketNotches,
    stampBadge = stampBadge,
    flatNoCards = flatNoCards,
    textTabNav = textTabNav,
    chartNumerals = chartNumerals,
    displayFont = when (displayFont) {
        AppDisplayFont.SYSTEM_BLACK -> FontFamily.Default
        AppDisplayFont.JAKARTA -> JakartaFontFamily
        AppDisplayFont.BRICOLAGE -> BricolageFontFamily
        AppDisplayFont.ARCHIVO -> ArchivoFontFamily
        AppDisplayFont.SPACE_GROTESK -> SpaceGroteskFontFamily
        AppDisplayFont.ANTON -> AntonFontFamily
        AppDisplayFont.FRAUNCES -> FrauncesFontFamily
    },
    bodyFont = when (bodyFont) {
        AppBodyFont.SYSTEM -> FontFamily.Default
        AppBodyFont.JAKARTA -> JakartaFontFamily
        AppBodyFont.ARCHIVO -> ArchivoFontFamily
        AppBodyFont.SPACE_GROTESK -> SpaceGroteskFontFamily
        AppBodyFont.INTER -> InterFontFamily
    },
    labelFont = when (labelFont) {
        AppLabelFont.SYSTEM_MONO -> FontFamily.Monospace
        AppLabelFont.JETBRAINS -> JetBrainsMonoFontFamily
        AppLabelFont.PLEX_MONO -> PlexMonoFontFamily
        AppLabelFont.INTER -> InterFontFamily
    },
    motionEasing = motionEasing,
    morphMs = morphMs,
    tabSlideMs = tabSlideMs,
    panelFadeMs = panelFadeMs,
    panelExitMs = panelExitMs,
    tintMs = tintMs,
    waveAmpMs = waveAmpMs,
    clusterWaveHeight = clusterWaveHeight.dp,
    clusterPlayerHeight = clusterPlayerHeight.dp,
    clusterPlayerCover = clusterPlayerCover.dp,
    clusterPlayerAction = clusterPlayerAction.dp,
    clusterPlayerControl = clusterPlayerControl.dp,
    clusterPlayerControlIcon = clusterPlayerControlIcon.dp,
    clusterPlayerLikeIcon = clusterPlayerLikeIcon.dp,
    clusterPlayerPlayIcon = clusterPlayerPlayIcon.dp,
    clusterPlayerTitleSize = clusterPlayerTitleSize.sp,
    clusterPlayerArtistSize = clusterPlayerArtistSize.sp,
    clusterPlayerPaddingH = clusterPlayerPaddingH.dp,
    clusterPlayerGap = clusterPlayerGap.dp,
    clusterNavHeight = clusterNavHeight.dp,
    clusterNavIcon = clusterNavIcon.dp,
    clusterNavLabelSize = clusterNavLabelSize.sp,
    clusterNavItemHeight = clusterNavItemHeight.dp,
    clusterNavPaddingV = clusterNavPaddingV.dp,
    clusterGap = clusterGap.dp,
    clusterBottomInset = clusterBottomInset.dp,
    clusterJamChipHeight = clusterJamChipHeight.dp,
    clusterJamChipGap = clusterJamChipGap.dp,
    clusterJamChipIcon = clusterJamChipIcon.dp,
    themeId = themeId,
)

val LocalSonaraTokens = compositionLocalOf {
    paletteFor(AppThemeId.ARUS, AppThemeMode.LIGHT).toTokens(AppThemeId.ARUS)
}

/** Akses token tema aktif dari composable mana pun. */
val CurrentTokens: SonaraTokens
    @Composable
    get() = LocalSonaraTokens.current

@Composable
fun SonaraTheme(
    theme: AppThemeId,
    mode: AppThemeMode,
    /** v1.0.7: override aksen + radius kustom (null/mati = token dasar apa adanya). */
    customOverride: CustomThemeOverride? = null,
    content: @Composable () -> Unit,
) {
    val tokens = run {
        val basePalette = paletteFor(theme, mode)
        if (customOverride?.enabled == true) {
            // Melapis DI ATAS palet dasar: struktur tema (font,
            // border, treatment, gerak) tetap; hanya warna aksen +
            // radius yang ditimpa, keduanya sudah dijamin lolos
            // batang kontras oleh CustomThemePolicy.
            val resolved = CustomThemePolicy.resolve(basePalette, customOverride)
            val radiused = CustomThemePolicy.applyRadius(basePalette, customOverride.radius)
            radiused.copy(
                accent = resolved.accent,
                accentFill = resolved.accentFill,
                accentDark = resolved.accentDark,
                accentSoft = resolved.accentSoft,
                onAccent = resolved.onAccent,
            ).toTokens(theme)
        } else {
            basePalette.toTokens(theme)
        }
    }
    // Tipografi bertema (v1.10.27): komponen Material3 (dialog,
    // menu, sheet sistem, tombol M3) mengikuti font tema — display
    // untuk headline/title, body untuk isi, mono untuk label.
    // Text eksplisit di layar memakai token langsung (lihat
    // SectionHeader/PosterCard/dll), bukan typography ini.
    val baseTypography = Typography()
    val typography = baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(
            fontFamily = tokens.displayFont, fontWeight = FontWeight.Black,
        ),
        displayMedium = baseTypography.displayMedium.copy(
            fontFamily = tokens.displayFont, fontWeight = FontWeight.Black,
        ),
        headlineLarge = baseTypography.headlineLarge.copy(
            fontFamily = tokens.displayFont, fontWeight = FontWeight.Black,
        ),
        headlineMedium = baseTypography.headlineMedium.copy(
            fontFamily = tokens.displayFont, fontWeight = FontWeight.Bold,
        ),
        titleLarge = baseTypography.titleLarge.copy(
            fontFamily = tokens.displayFont, fontWeight = FontWeight.Bold,
        ),
        titleMedium = baseTypography.titleMedium.copy(
            fontFamily = tokens.displayFont, fontWeight = FontWeight.Bold,
        ),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = tokens.bodyFont),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = tokens.bodyFont),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = tokens.bodyFont),
        labelLarge = baseTypography.labelLarge.copy(
            fontFamily = tokens.labelFont,
            letterSpacing = tokens.labelLetterSpacing,
        ),
        labelMedium = baseTypography.labelMedium.copy(
            fontFamily = tokens.labelFont,
            letterSpacing = tokens.labelLetterSpacing,
        ),
        labelSmall = baseTypography.labelSmall.copy(
            fontFamily = tokens.labelFont,
            letterSpacing = tokens.labelLetterSpacing,
        ),
    )
    val scheme = if (mode == AppThemeMode.DARK) {
        darkColorScheme(
            primary = tokens.accentFill,
            onPrimary = tokens.onAccent,
            secondary = tokens.textMuted,
            tertiary = tokens.accent,
            background = tokens.background,
            surface = tokens.surface,
            surfaceVariant = tokens.surfaceVariant,
            surfaceContainerLowest = tokens.surface,
            surfaceContainerLow = tokens.surface,
            surfaceContainer = tokens.surfaceVariant,
            surfaceContainerHigh = tokens.surfaceVariant,
            surfaceContainerHighest = tokens.surfaceVariant,
            onBackground = tokens.text,
            onSurface = tokens.text,
            onSurfaceVariant = tokens.textMuted,
            error = tokens.error,
            outline = tokens.outline,
        )
    } else {
        lightColorScheme(
            primary = tokens.accentFill,
            onPrimary = tokens.onAccent,
            secondary = tokens.textMuted,
            tertiary = tokens.accent,
            background = tokens.background,
            surface = tokens.surface,
            surfaceVariant = tokens.surfaceVariant,
            surfaceContainerLowest = tokens.surface,
            surfaceContainerLow = tokens.surface,
            surfaceContainer = tokens.surfaceVariant,
            surfaceContainerHigh = tokens.surfaceVariant,
            surfaceContainerHighest = tokens.surfaceVariant,
            onBackground = tokens.text,
            onSurface = tokens.text,
            onSurfaceVariant = tokens.textMuted,
            error = tokens.error,
            outline = tokens.outline,
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSonaraTokens provides tokens,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            content = content,
        )
    }
}

/** Easing Compose untuk karakter gerak tema aktif. */
fun themeEasing(tokens: SonaraTokens): Easing = when (tokens.motionEasing) {
    AppMotionEasing.FAST_OUT_SLOW_IN -> FastOutSlowInEasing
    AppMotionEasing.LINEAR_OUT_SLOW_IN -> LinearOutSlowInEasing
    AppMotionEasing.LINEAR -> LinearEasing
    // PASAR: pop kenyal prototype (cubic-bezier .34,1.8,.64,1).
    AppMotionEasing.OVERSHOOT -> androidx.compose.animation.core.CubicBezierEasing(0.34f, 1.8f, 0.64f, 1f)
}

/**
 * Casing judul display sesuai tema: Blok memakai kapital semua
 * (prototype arah 03, h2 uppercase); tema lain apa adanya. Hanya
 * untuk judul DISPLAY/seksi — judul lagu dan nama orang tidak
 * pernah diubah casing-nya.
 */
fun displayCased(text: String, tokens: SonaraTokens): String =
    if (tokens.uppercaseTitles) text.uppercase() else text

/**
 * Sistem desain SONARA v1.7 — rebuild TOTAL dari 7 screenshot
 * ground truth prototype (user: v1.6 "masih gak mirip prototype").
 *
 * Semua nilai di bawah DIUKUR dari screenshot 907×2048 (skala
 * ≈2,13 px/dp), bukan dari ringkasan spec lama. Yang bertentangan
 * dengan DESIGN-SPEC.md §5 (lama) dikalahkan screenshot.
 *
 * Palet prototype: kertas #FFF9ED, segmen #F2E7D3, hitam pekat
 * #1B1813, gold poster #E8BA0F, oranye bakar #FF4E24, teks
 * sekunder coklat-abu. Font display = SansSerif Black (900);
 * font proprietary TIDAK dibundel.
 * Label mono = Monospace kapital, letter-spacing lebar.
 */

/** Kertas — background utama (sampling piksel screenshot). Bertema v1.10.26. */
val PaperBg: Color
    @Composable get() = LocalSonaraTokens.current.background

/** Permukaan kartu di atas kertas — putih hangat. Bertema v1.10.26. */
val PaperSurface: Color
    @Composable get() = LocalSonaraTokens.current.surface

/** Container segmen (pill besar di bawah poster). Bertema v1.10.26. */
val SegmentBg: Color
    @Composable get() = LocalSonaraTokens.current.surfaceVariant

/** Permukaan sekunder lama — dipertahankan sebagai alias. */
val PaperSurfaceVariant: Color
    @Composable get() = LocalSonaraTokens.current.surfaceVariant

/** Hitam pekat — PERAN TEKS v1.10.26 (dulu juga dipakai sebagai
 *  permukaan gelap; permukaan gelap kini [ChromeBg]/[StageBg]).
 *  Dipakai untuk teks/ikon/border kuat di atas permukaan terang. */
val InkBlack: Color
    @Composable get() = LocalSonaraTokens.current.text

/** Tinta — teks primer di atas kertas. */
val Ink: Color
    @Composable get() = LocalSonaraTokens.current.text

/** Tinta redup — teks sekunder coklat-abu (sampling piksel). */
val InkMuted: Color
    @Composable get() = LocalSonaraTokens.current.textMuted

/** Garis tepi hangat — border kartu/field/kapsul + divider. */
val PaperOutline: Color
    @Composable get() = LocalSonaraTokens.current.outline

/** Gold poster / placeholder artwork — TETAP (dekorasi, bukan struktur).
 *  Latar kartu poster saat tanpa artwork memakai [PosterFallback]. */
val PosterGold = Color(0xFFE8BA0F)

/** Latar kartu poster saat tanpa artwork — bertema (tiap varian
 *  cukup gelap/kontras untuk judul putih di atasnya). */
val PosterFallback: Color
    @Composable get() = LocalSonaraTokens.current.posterFallback

/** Gold avatar "T" di header Beranda. Bertema v1.10.26. */
val AvatarGold: Color
    @Composable get() = LocalSonaraTokens.current.avatar

/** Huruf/ikon di atas lingkaran avatar. Selalu tinta gelap. */
val OnAvatar: Color
    @Composable get() = LocalSonaraTokens.current.onAvatar

/** Aksen utama — PERAN TEKS v1.10.26 (link, label aktif, ikon).
 *  Isi tombol/pill memakai [AccentFill]. */
val BurntOrange: Color
    @Composable get() = LocalSonaraTokens.current.accent
val BurntOrangeDark: Color
    @Composable get() = LocalSonaraTokens.current.accentDark
val BurntOrangeSoft: Color
    @Composable get() = LocalSonaraTokens.current.accentSoft

/** Aksen peran ISI — latar tombol, pill aktif, indikator. */
val AccentFill: Color
    @Composable get() = LocalSonaraTokens.current.accentFill

/** Teks/ikon di atas [AccentFill]. */
val OnAccent: Color
    @Composable get() = LocalSonaraTokens.current.onAccent

/** Aksen di atas panggung GELAP (layar detail) — nilai aksen mode
 *  gelap tema itu di kedua mode, karena panggung selalu gelap. */
val DetailAccent: Color
    @Composable get() = LocalSonaraTokens.current.accentOnDark

/** Chrome mengambang yang MEMBALIK antar mode (pill nav aktif, chip
 *  terpilih). Isinya memakai [OnChrome]. */
val ChromeBg: Color
    @Composable get() = LocalSonaraTokens.current.chrome
val OnChrome: Color
    @Composable get() = LocalSonaraTokens.current.onChrome

/** Permukaan gelap yang TETAP gelap di semua varian (kapsul mini
 *  player, strip Jam, kartu panggung). Isinya teks putih tetap. */
val StageBg: Color
    @Composable get() = LocalSonaraTokens.current.stage

/** Warna error teks/ikon — bertema (merah terang di mode gelap). */
val ThemeError: Color
    @Composable get() = LocalSonaraTokens.current.error

/** Hitam pekat TETAP — elemen grafis dekoratif (vinyl, gradien
 *  tile) yang memang harus hitam di semua tema. */
val InkFixed = Color(0xFF1B1813)

/** Kertas TETAP — pasangan [InkFixed] untuk gradien dekoratif. */
val PaperFixed = Color(0xFFFFF9ED)

/** Garis Arus poster — anyaman hijau + krem (screenshot poster). */
val WaveGreen = Color(0xFF1FA34F)
val WaveCream = Color(0xFFF5E9C8)

/** Hijau muda — pasangan merah untuk gelombang Garis Arus tanpa artwork (v1.10.6). */
val WaveLightGreen = Color(0xFF7ED957)

/** Merah YTM (dipakai titik sumber detail + default gelombang lama). */
val WaveRed = Color(0xFFFF0000)

/** Garis Arus strip layar terang — gelombang tan redup. */
val WaveStripTrack = Color(0xFFDCCFB2)
val WaveStripProgress = Color(0xFF8A8378)

/** Gelombang progress detail — abu-abu (screenshot detail). */
val WaveDetailTrack = Color(0xFF6F6862)
val WaveDetailProgress = Color(0xFF8B8683)

/** Titik kecil di label sumber. */
val SourceDotYtm = Color(0xFFFF0000)
val SourceDotLocal = Color(0xFF8E8E9C)

/** Warna kartu genre / koleksi (sampling piksel screenshot). */
val GenrePop = Color(0xFFFF4E24)
val GenreIndie = Color(0xFF0E7C7B)
val GenreGalau = Color(0xFF7A50D0)
val GenreRock = Color(0xFF1A1613)
val GenrePodcast = Color(0xFF2E6B4F)
// v1.10.7: warna fallback tile kategori Browse tambahan — berbeda
// hue satu sama lain dan dari 5 warna di atas (artwork asli YTM
// menutupi sebagian besar tile; warna hanya fallback).
val GenreDangdut = Color(0xFFD81B60)
val GenreKPop = Color(0xFF1E88E5)
val GenreHipHop = Color(0xFFFFB300)
val GenreRnB = Color(0xFF7B1E3B)
val GenreJazz = Color(0xFFBF8F00)
val GenreElectronic = Color(0xFF00ACC1)
val GenreAcoustic = Color(0xFF6D4C41)
val GenreMetal = Color(0xFF455A64)
val CardOffline = Color(0xFF147C7B)
val CardLikedDeco = Color(0xFFFF8C71)
val CardSlate = Color(0xFF445879)

/** Latar detail lagu — gelap, diwarnai dari artwork (fallback netral). */
val DetailBgFallback = Color(0xFF241407)
val DetailTextPrimary = Color(0xFFFFFFFF)
val DetailTextSecondary = Color(0xFFD8C9B8)

/** Panel header mini (tab LYRICS/RELATED) / kartu lirik / panel related. */
val DetailMiniHeaderBg = Color(0xFF473706)
val DetailLyricsCardBg = Color(0xFF3D3119)
val DetailRelatedPanelBg = Color(0xFF403517)

/** Rect mode Video 16:9. */
val VideoRectBg = Color(0xFF7F6609)

/** Spacing — skala ARUS. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val x6 = 6.dp
    val x20 = 20.dp
}

/** Corner radius — bertema v1.10.27 (bentuk tiap tema BERBEDA:
 *  ARUS lembut 16/28, Senja tajam 10/16, Blok nyaris-kotak 6/8,
 *  GETAR minimal 12/10 — lihat tabel token AppTheme.kt). */
object Corners {
    val small: Dp
        @Composable get() = LocalSonaraTokens.current.cornerSmall
    val medium: Dp
        @Composable get() = LocalSonaraTokens.current.cornerMedium
    val large: Dp
        @Composable get() = LocalSonaraTokens.current.cornerLarge
    val poster: Dp
        @Composable get() = LocalSonaraTokens.current.cornerPoster
    val pill: Dp
        @Composable get() = LocalSonaraTokens.current.cornerPill
    val rowCover: Dp
        @Composable get() = LocalSonaraTokens.current.cornerRowCover
    val card: Dp
        @Composable get() = LocalSonaraTokens.current.cornerCard
    val tile: Dp
        @Composable get() = LocalSonaraTokens.current.cornerTile
    val chip: Dp
        @Composable get() = LocalSonaraTokens.current.cornerChip
    val sheet: Dp
        @Composable get() = LocalSonaraTokens.current.cornerSheet
    val button: Dp
        @Composable get() = LocalSonaraTokens.current.cornerButton
    val miniPlayer: Dp
        @Composable get() = LocalSonaraTokens.current.cornerMiniPlayer
    val detailCover: Dp
        @Composable get() = LocalSonaraTokens.current.cornerDetailCover
}

/**
 * Densitas per tema (v1.10.27) — padding/gap/tinggi baris yang
 * membuat tiap tema terasa berbeda kepadatannya: Senja rapat,
 * Blok lapang + chunky, GETAR whitespace lega, ARUS seperti semula.
 */
object Density {
    val rowPaddingV: Dp
        @Composable get() = LocalSonaraTokens.current.rowPaddingV
    val rowMinHeight: Dp
        @Composable get() = LocalSonaraTokens.current.rowMinHeight
    val sectionPaddingV: Dp
        @Composable get() = LocalSonaraTokens.current.sectionPaddingV
    val sectionTopPadding: Dp
        @Composable get() = LocalSonaraTokens.current.sectionTopPadding
    val cardPadding: Dp
        @Composable get() = LocalSonaraTokens.current.cardPadding
    val gridGap: Dp
        @Composable get() = LocalSonaraTokens.current.gridGap
    val headerPaddingV: Dp
        @Composable get() = LocalSonaraTokens.current.headerPaddingV
}

/**
 * Klaster bawah SLIM v1.10.29 — ukuran wave + mini player +
 * navigasi + chip Jam dari token tema aktif (identik di ke-8
 * varian; total tumpukan 130dp: 6 + 54 + 6 + 56 + 8). Dipakai
 * HANYA oleh bottomBar/MiniPlayer/CapsuleNav/JamStatusChip —
 * tinggi klaster tidak boleh di-hardcode di composable.
 */
object BottomCluster {
    val waveHeight: Dp
        @Composable get() = LocalSonaraTokens.current.clusterWaveHeight
    val playerHeight: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerHeight
    val playerCover: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerCover
    val playerAction: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerAction
    val playerControl: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerControl
    val playerControlIcon: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerControlIcon
    val playerLikeIcon: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerLikeIcon
    val playerPlayIcon: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerPlayIcon
    val playerTitleSize: androidx.compose.ui.unit.TextUnit
        @Composable get() = LocalSonaraTokens.current.clusterPlayerTitleSize
    val playerArtistSize: androidx.compose.ui.unit.TextUnit
        @Composable get() = LocalSonaraTokens.current.clusterPlayerArtistSize
    val playerPaddingH: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerPaddingH
    val playerGap: Dp
        @Composable get() = LocalSonaraTokens.current.clusterPlayerGap
    val navHeight: Dp
        @Composable get() = LocalSonaraTokens.current.clusterNavHeight
    val navIcon: Dp
        @Composable get() = LocalSonaraTokens.current.clusterNavIcon
    val navLabelSize: androidx.compose.ui.unit.TextUnit
        @Composable get() = LocalSonaraTokens.current.clusterNavLabelSize
    val navItemHeight: Dp
        @Composable get() = LocalSonaraTokens.current.clusterNavItemHeight
    val navPaddingV: Dp
        @Composable get() = LocalSonaraTokens.current.clusterNavPaddingV
    val gap: Dp
        @Composable get() = LocalSonaraTokens.current.clusterGap
    val bottomInset: Dp
        @Composable get() = LocalSonaraTokens.current.clusterBottomInset
    val jamChipHeight: Dp
        @Composable get() = LocalSonaraTokens.current.clusterJamChipHeight
    val jamChipGap: Dp
        @Composable get() = LocalSonaraTokens.current.clusterJamChipGap
    val jamChipIcon: Dp
        @Composable get() = LocalSonaraTokens.current.clusterJamChipIcon
}

/** Ukuran komponen — diukur dari screenshot (907×2048, ÷2,13). */
object Sizes {
    val rowCover = 54.dp        // cover baris daftar (52–56dp)
    val rowMinHeight = 72.dp    // tinggi baris lagu
    val shelfCover = 160.dp
    val shelfPeek = 24.dp
    // Ukuran klaster bawah (mini player/nav) v1.10.29 pindah ke
    // token tema [BottomCluster] — jangan tambah konstanta di sini.
    val playButton = 72.dp      // tombol play bundar PUTIH di detail lagu
    val playButtonIcon = 36.dp
    val skipButton = 56.dp
    val skipIcon = 32.dp
    val controlButton = 48.dp
    val controlIcon = 24.dp
    // v1.10.7: 256dp (dulu 320dp) — artwork detail dikecilkan
    // ~20% supaya baris tab + awal isi tab muat dalam satu layar
    // tanpa scroll halaman (anggaran tinggi §BUILD-v1.10.7).
    // Mode Video 1:1 TIDAK memakai konstanta ini (tetap selebar
    // layar) dan landscape tetap 240dp (lihat NowPlayingScreen).
    val nowPlayingCover = 256.dp
    val playlistCover = 56.dp
    val segmentHeight = 64.dp   // segmen filter sumber (screenshot)
    val segmentPillHeight = 48.dp
    val posterHeight = 204.dp   // Kartu Poster Beranda (screenshot)
    val posterPlayButton = 64.dp
}

/**
 * Hierarki tipografi v1.7 — judul Black (900), leading rapat;
 * label mono kapital letter-spacing lebar.
 */
object Type {
    val greeting = 32.sp
    val screenTitle = 24.sp
    val sectionTitle = 30.sp    // "Lagi ramai" / "Terakhir diputar"
    val posterTitle = 42.sp     // judul di Kartu Poster
    val trackTitle = 17.sp      // judul lagu di baris (Bold)
    val trackTitleLarge = 36.sp // judul lagu di detail (Black)
    val artist = 14.sp
    val artistLarge = 16.sp
    val sourceLabel = 11.sp     // label mono mungil (caption baris)
    val body = 14.sp
    val caption = 12.sp
    val lyricActive = 28.sp     // lirik aktif (screenshot LYRICS)
    val lyricInactive = 22.sp   // lirik non-aktif
    // Label navigasi v1.10.29 pindah ke token tema [BottomCluster].
    val headline = 38.sp        // headline editorial Cari / Koleksi
}

/** Letter-spacing label mono (0,12em ≈ 1,3sp pada 11sp). */
val MonoLetterSpacing = 1.3.sp

/** Font mono untuk label kapital. */
val MonoFont: FontFamily = FontFamily.Monospace

/**
 * Font lirik (v1.10.18, polish tipografi): Plus Jakarta Sans Regular
 * — SATU family open-license (SIL OFL 1.1, google/fonts), instans
 * statis wght=400 dari variable font resmi, dipakai HANYA untuk
 * baris lirik. Bila resource font gagal dimuat, Compose jatuh ke
 * FontFamily.Default secara otomatis (tidak ada crash). Ukuran /
 * lineHeight lirik TIDAK berubah (22sp/28sp seragam, keputusan
 * 10.6) — yang berubah hanya bentuk huruf + letterSpacing + animasi
 * warna; baris aktif tetap dibedakan lewat warna/opacity + aksen
 * non-layout saja.
 */
val LyricsFontFamily: FontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(
        com.sonara.app.R.font.plus_jakarta_sans_regular,
    ),
)

/**
 * Token GERAKAN + RESPONS SENTUH (dipertahankan dari v1.5/v1.6).
 */
object Motion {
    const val fastMs = 150
    const val mediumMs = 300
    const val slowMs = 450
    const val staggerMs = 45L
    const val spinMs = 8000
    const val waveFlowMs = 2500
}

/** Warna titik kecil di label sumber. */
fun sourceDot(source: com.sonara.app.data.Source): Color = when (source) {
    com.sonara.app.data.Source.YOUTUBE_MUSIC -> SourceDotYtm
    com.sonara.app.data.Source.LOCAL -> SourceDotLocal
}
