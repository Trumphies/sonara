package com.sonara.app.data

/**
 * Tema SONARA v1.10.37 — theme switcher di Settings.
 *
 * SATU sumber token untuk 7 tema x 2 mode = 14 varian. File ini MURNI
 * Kotlin (tanpa Compose) supaya harness bisa mengompilasinya mandiri
 * dan memeriksa kontras WCAG tiap pasangan dari tabel yang sama persis
 * dengan yang dipakai app (lihat harness127).
 *
 * Identitas tiap tema harus selamat dari flip mode: mode hanya mengubah
 * luminans permukaan, bukan bentuk/aksen/karakter tema.
 *
 * - ARUS: identitas bawaan (kertas hangat + oranye bakar). Light =
 *   token persis v1.7–v1.10.25; Dark = kertas menjadi arang hangat,
 *   oranye dipertahankan.
 * - SENJA (prototype "Senja Gelap", arah 01): plum-ungu + amber.
 *   Token WARNA dark dikutip persis dari CSS prototype
 *   (bg #17121F, surface #221A30, surface2 #2E2142, text #F7EFE1,
 *   muted #A99BC2, accent #FFB347, accent2 #FF7A4D, border #372A4E,
 *   Plus Jakarta Sans). Bentuk/densitas/gerak v1.10.27: radius
 *   lebih tajam (10/16dp), baris lebih rapat, glow amber pada
 *   elemen yang sedang jalan. Light = luminans dibalik dengan
 *   amber yang digelapkan untuk peran teks.
 * - BLOK (prototype arah 03): color-blocking berani, border tebal,
 *   bayangan keras, judul kapital. Token WARNA light dikutip persis
 *   dari CSS prototype (bg #FFF1C9, surface #FFFFFF, surface2
 *   #FFE36E, text #131313, muted #4A4438, accent #FF3D68, accent2
 *   #245BFF, border #131313, Bricolage Grotesque) beserta border
 *   2px + bayangan keras 5/4/3px dan h2 kapital. Bentuk v1.10.27
 *   mengikuti spec koreksi (nyaris-kotak 6/8dp) — radius 22px
 *   prototype TIDAK dipakai karena justru menyamarkan Blok dari
 *   ARUS. Dark = luminans dibalik, border menjadi krem.
 * - GETAR (prototype GETAR): field generatif seeded per lagu di atas
 *   dasar tinta #0E0E14, Archivo + JetBrains Mono. Aksen default =
 *   seed 0 (#FF8A7A); aksen per lagu berasal dari [getarSeedFor]
 *   dan hanya dipakai garis field (lihat ui/GetarField.kt).
 *
 * PITA dan Galeri SENGAJA tidak ada: PITA ditolak user ("kurang"),
 * Galeri terlalu mirip ARUS (sama-sama terang editorial).
 *
 * Peran warna:
 * - background/surface/surfaceVariant: permukaan struktural.
 * - text/textMuted/outline: teks + garis.
 * - accent: aksen peran TEKS (link, label aktif, ikon) — harus
 *   terbaca di atas background.
 * - accentFill: aksen peran ISI (tombol, pill aktif, indikator) —
 *   dipasangkan dengan onAccent.
 * - accentDark: varian aksen gelap (tekan/gradien).
 * - accentSoft: latar aksen lembut.
 * - accentOnDark: aksen di atas panggung GELAP (layar detail) —
 *   nilainya aksen mode gelap tema itu di KEDUA mode, karena
 *   panggung detail selalu gelap.
 * - chrome/onChrome: chrome mengambang yang MEMBALIK (pill aktif,
 *   chip terpilih) — kontras terhadap background.
 * - stage: permukaan gelap yang TETAP gelap di semua varian
 *   (kapsul mini player, strip Jam, kartu panggung) — isinya
 *   selalu teks putih tetap.
 * - avatar/onAvatar: lingkaran avatar + huruf di atasnya.
 * - poster/posterFallback: emas poster (dekorasi tetap) + latar
 *   kartu poster saat tanpa artwork.
 */
enum class AppThemeId {
    ARUS,
    SENJA,
    BLOK,
    GETAR,
    RASI,
    PASAR,
    SENYAP,
}

enum class AppThemeMode {
    LIGHT,
    DARK,
}

/** Font isi/body tiap tema (diselesaikan ke FontFamily di Theme.kt). */
enum class AppBodyFont {
    /** SansSerif sistem — ARUS + Blok (body tetap netral). */
    SYSTEM,

    /** Plus Jakarta Sans (dibundel) — Senja. */
    JAKARTA,

    /** Archivo (dibundel, OFL) — GETAR. */
    ARCHIVO,

    /** Space Grotesk (dibundel, OFL) — RASI. */
    SPACE_GROTESK,

    /** Inter (dibundel, OFL) — PASAR + SENYAP. */
    INTER,
}

/** Font label mono tiap tema (diselesaikan ke FontFamily di Theme.kt). */
enum class AppLabelFont {
    /** Monospace sistem — ARUS, Senja, Blok. */
    SYSTEM_MONO,

    /** JetBrains Mono (dibundel, OFL) — GETAR. */
    JETBRAINS,

    /** IBM Plex Mono (dibundel, OFL) — RASI (label koordinat). */
    PLEX_MONO,

    /** Inter (dibundel, OFL) — PASAR + SENYAP (label). */
    INTER,
}

/** Karakter easing gerak tiap tema (dipetakan ke Easing Compose di UI). */
enum class AppMotionEasing {
    FAST_OUT_SLOW_IN,
    LINEAR_OUT_SLOW_IN,
    LINEAR,
    /** Overshoot kenyal PASAR (cubic-bezier .34,1.8,.64,1 di Theme.kt). */
    OVERSHOOT,
}

/** Font display tiap tema (diselesaikan ke FontFamily di Theme.kt). */
enum class AppDisplayFont {
    /** SansSerif + Black — tampilan ARUS bawaan. */
    SYSTEM_BLACK,

    /** Plus Jakarta Sans (dibundel) — Senja. */
    JAKARTA,

    /** Bricolage Grotesque ExtraBold (dibundel, OFL) — Blok. */
    BRICOLAGE,

    /** Archivo Bold (dibundel, OFL) — GETAR. */
    ARCHIVO,

    /** Space Grotesk (dibundel, OFL) — RASI. */
    SPACE_GROTESK,

    /** Anton (dibundel, OFL) — PASAR. */
    ANTON,

    /** Fraunces (dibundel, OFL) — SENYAP. */
    FRAUNCES,
}

data class AppThemeSelection(
    val theme: AppThemeId,
    val mode: AppThemeMode,
) {
    companion object {
        val DEFAULT = AppThemeSelection(AppThemeId.ARUS, AppThemeMode.LIGHT)
    }
}

fun appThemeIdFromStorage(raw: String?): AppThemeId = when (raw?.lowercase()) {
    "senja" -> AppThemeId.SENJA
    "blok" -> AppThemeId.BLOK
    "getar" -> AppThemeId.GETAR
    "rasi" -> AppThemeId.RASI
    "pasar" -> AppThemeId.PASAR
    "senyap" -> AppThemeId.SENYAP
    else -> AppThemeId.ARUS
}

fun appThemeModeFromStorage(raw: String?): AppThemeMode = when (raw?.lowercase()) {
    "dark" -> AppThemeMode.DARK
    else -> AppThemeMode.LIGHT
}

fun AppThemeId.storageKey(): String = name.lowercase()

fun AppThemeMode.storageKey(): String = name.lowercase()

data class AppThemePalette(
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val text: Long,
    val textMuted: Long,
    val outline: Long,
    val accent: Long,
    val accentFill: Long,
    val accentDark: Long,
    val accentSoft: Long,
    val accentOnDark: Long,
    val onAccent: Long,
    val chrome: Long,
    val onChrome: Long,
    val stage: Long,
    val avatar: Long,
    val onAvatar: Long,
    val poster: Long,
    val posterFallback: Long,
    val error: Long,
    val cornerSmall: Int,
    val cornerMedium: Int,
    val cornerLarge: Int,
    val cornerPoster: Int,
    val cornerPill: Int,
    val cornerRowCover: Int,
    val uppercaseTitles: Boolean,
    val hardShadow: Boolean,
    val flowField: Boolean,
    val displayFont: AppDisplayFont,
    // --- Identitas STRUKTURAL v1.10.27 (invarian antar mode; mode
    // hanya membalik luminans). Nilai dp/sp sebagai Int.
    val cornerCard: Int,
    val cornerTile: Int,
    val cornerChip: Int,
    val cornerSheet: Int,
    val cornerButton: Int,
    val cornerMiniPlayer: Int,
    val rowPaddingV: Int,
    val rowMinHeight: Int,
    val sectionPaddingV: Int,
    val sectionTopPadding: Int,
    val cardPadding: Int,
    val gridGap: Int,
    val headerPaddingV: Int,
    val sectionTitleSize: Int,
    val posterTitleSize: Int,
    val detailTitleSize: Int,
    /** Letter-spacing label mono, persepuluh sp (13 = 1.3sp). */
    val labelLetterSpacingTenths: Int,
    /** Letter-spacing judul display, persepuluh sp (boleh negatif). */
    val displayLetterSpacingTenths: Int,
    val cardBorderWidth: Int,
    val dividerThickness: Int,
    /** Alpha divider, persen (8 = 0.08f; 100 = solid). */
    val dividerAlphaPercent: Int,
    val hardShadowPoster: Int,
    val hardShadowTile: Int,
    val hardShadowChip: Int,
    val glowElevation: Int,
    /** Alpha glow Senja, persen. */
    val glowAlphaPercent: Int,
    val bodyFont: AppBodyFont,
    val labelFont: AppLabelFont,
    val motionEasing: AppMotionEasing,
    val morphMs: Int,
    val tabSlideMs: Int,
    val panelFadeMs: Int,
    val panelExitMs: Int,
    val tintMs: Int,
    val waveAmpMs: Int,
    // --- Klaster bawah SLIM v1.10.29 (pilihan user dari POC
    // sonara-slim-bottom-cluster-poc, varian Slim) + nav dibesarkan
    // v1.10.30 (permintaan user 13:26 WIB): total 130dp).
    // NILAI IDENTIK di ke-8 varian: identitas tema dibawa radius/
    // border/glow/font, bukan tinggi klaster — tinggi klaster adalah
    // keputusan global. Mode/theme invariance diperiksa harness130.
    // Total tumpukan = wave + player + gap + nav + inset bawah.
    /** Tinggi strip wave Garis Arus di bibir mini player. */
    val clusterWaveHeight: Int,
    /** Tinggi kapsul mini player. */
    val clusterPlayerHeight: Int,
    /** Diameter cover berputar mini player. */
    val clusterPlayerCover: Int,
    /** Diameter tombol play oranye mini player. */
    val clusterPlayerAction: Int,
    /** Ukuran sentuh tombol like/prev/next (≥40dp). */
    val clusterPlayerControl: Int,
    /** Ikon skip prev/next mini player. */
    val clusterPlayerControlIcon: Int,
    /** Ikon hati mini player. */
    val clusterPlayerLikeIcon: Int,
    /** Ikon play/pause mini player. */
    val clusterPlayerPlayIcon: Int,
    /** Ukuran judul lagu mini player, sp. */
    val clusterPlayerTitleSize: Int,
    /** Ukuran nama artis mini player, sp. */
    val clusterPlayerArtistSize: Int,
    /** Padding horizontal dalam kapsul mini player. */
    val clusterPlayerPaddingH: Int,
    /** Jarak cover → kolom judul mini player. */
    val clusterPlayerGap: Int,
    /** Tinggi kapsul navigasi. */
    val clusterNavHeight: Int,
    /** Ikon item navigasi. */
    val clusterNavIcon: Int,
    /** Ukuran label navigasi, sp. */
    val clusterNavLabelSize: Int,
    /** Tinggi pill item navigasi di dalam kapsul. */
    val clusterNavItemHeight: Int,
    /** Padding vertikal dalam kapsul navigasi. */
    val clusterNavPaddingV: Int,
    /** Jarak antar-elemen tumpukan klaster bawah. */
    val clusterGap: Int,
    /** Inset bawah setelah kapsul navigasi. */
    val clusterBottomInset: Int,
    /** Tinggi chip status Jam ramping di atas klaster. */
    val clusterJamChipHeight: Int,
    /** Jarak chip Jam → wave/klaster. */
    val clusterJamChipGap: Int,
    /** Ikon broadcast di chip Jam. */
    val clusterJamChipIcon: Int,
    // --- Identitas v1.10.37 (RASI/PASAR/SENYAP). Default = perilaku
    // tema lama (semua treatment mati; cover detail = radius poster),
    // supaya literal 8 varian lama tetap kompilasi tanpa diubah.
    // Tema baru mengesetnya eksplisit via named arg. -1 pada
    // cornerDetailCover berarti "ikut cornerPoster" (lihat toTokens).
    val cornerDetailCover: Int = -1,
    /** Cincin orbit + titik mengorbit di sekeliling artwork (RASI). */
    val orbitRing: Boolean = false,
    /** Latar dot-grid di belakang konten (RASI). */
    val dotGridBackground: Boolean = false,
    /** Garis konektor konstelasi di belakang cover baris (RASI). */
    val constellationLines: Boolean = false,
    /** Border + divider putus-putus (PASAR). */
    val dashedBorders: Boolean = false,
    /** Kartu stub tiket: border putus-putus + notch (PASAR). */
    val ticketNotches: Boolean = false,
    /** Stiker miring + stempel bundar di kartu poster (PASAR). */
    val stampBadge: Boolean = false,
    /** Tanpa kartu/border/shadow — hanya whitespace + hairline (SENYAP). */
    val flatNoCards: Boolean = false,
    /** Navigasi berupa tab teks bergaris bawah, tanpa kapsul (SENYAP). */
    val textTabNav: Boolean = false,
    /** Numeral chart 01/02/… di samping baris lagu (SENYAP). */
    val chartNumerals: Boolean = false,
)

/** Palet lengkap 8 varian. Nilai 0xFF______ = ARGB penuh. */
fun paletteFor(theme: AppThemeId, mode: AppThemeMode): AppThemePalette = when (theme) {
    AppThemeId.ARUS -> when (mode) {
        AppThemeMode.LIGHT -> AppThemePalette(
            background = 0xFFFFF9ED,
            surface = 0xFFFFFDF7,
            surfaceVariant = 0xFFF2E7D3,
            text = 0xFF1B1813,
            textMuted = 0xFF8A8378,
            outline = 0xFFE7D3B3,
            accent = 0xFFFF4E24,
            accentFill = 0xFFFF4E24,
            accentDark = 0xFFD63E1A,
            accentSoft = 0xFFFBE7D8,
            accentOnDark = 0xFFFF4E24,
            // v1.10.40: tinta gelap tema sendiri (dulu putih 3.30:1
            // di atas accentFill — semua pendamping AccentFill kini
            // >= 5.37:1 di ARUS).
            onAccent = 0xFF1B1813,
            chrome = 0xFF1B1813,
            onChrome = 0xFFFFF9ED,
            stage = 0xFF1B1813,
            avatar = 0xFFFFB727,
            onAvatar = 0xFF1B1813,
            poster = 0xFFE8BA0F,
            posterFallback = 0xFFE8BA0F,
            error = 0xFFD92D20,
            cornerSmall = 4, cornerMedium = 8, cornerLarge = 16,
            cornerPoster = 28, cornerPill = 28, cornerRowCover = 14,
            uppercaseTitles = false, hardShadow = false, flowField = false,
            displayFont = AppDisplayFont.SYSTEM_BLACK,
            cornerCard = 16, cornerTile = 16, cornerChip = 999,
            cornerSheet = 28, cornerButton = 999, cornerMiniPlayer = 999,
            rowPaddingV = 8, rowMinHeight = 72, sectionPaddingV = 10,
            sectionTopPadding = 24, cardPadding = 16, gridGap = 10,
            headerPaddingV = 10, sectionTitleSize = 30, posterTitleSize = 42,
            detailTitleSize = 36, labelLetterSpacingTenths = 13,
            displayLetterSpacingTenths = 0, cardBorderWidth = 1,
            dividerThickness = 1, dividerAlphaPercent = 8,
            hardShadowPoster = 0, hardShadowTile = 0, hardShadowChip = 0,
            glowElevation = 0, glowAlphaPercent = 0,
            bodyFont = AppBodyFont.SYSTEM, labelFont = AppLabelFont.SYSTEM_MONO,
            motionEasing = AppMotionEasing.FAST_OUT_SLOW_IN,
            morphMs = 150, tabSlideMs = 300, panelFadeMs = 180,
            panelExitMs = 120, tintMs = 200, waveAmpMs = 450,
            clusterWaveHeight = 6, clusterPlayerHeight = 54,
            clusterPlayerCover = 38, clusterPlayerAction = 44,
            clusterPlayerControl = 40, clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22, clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14, clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10, clusterPlayerGap = 8,
            clusterNavHeight = 56, clusterNavIcon = 24,
            clusterNavLabelSize = 12, clusterNavItemHeight = 48,
            clusterNavPaddingV = 4, clusterGap = 6,
            clusterBottomInset = 8, clusterJamChipHeight = 28,
            clusterJamChipGap = 6, clusterJamChipIcon = 18,
        )
        AppThemeMode.DARK -> AppThemePalette(
            background = 0xFF1B1813,
            surface = 0xFF26211A,
            surfaceVariant = 0xFF322B20,
            text = 0xFFFFF9ED,
            textMuted = 0xFFA89E8D,
            outline = 0xFF453C2E,
            accent = 0xFFFF4E24,
            accentFill = 0xFFFF4E24,
            accentDark = 0xFFFF7A4D,
            accentSoft = 0xFF4A2C1C,
            accentOnDark = 0xFFFF4E24,
            // v1.10.40: tinta gelap tema sendiri (dulu putih 3.30:1).
            onAccent = 0xFF1B1813,
            chrome = 0xFFFFF9ED,
            onChrome = 0xFF1B1813,
            stage = 0xFF26211A,
            avatar = 0xFFFFB727,
            onAvatar = 0xFF1B1813,
            poster = 0xFFE8BA0F,
            posterFallback = 0xFF3A2C10,
            error = 0xFFF87171,
            cornerSmall = 4, cornerMedium = 8, cornerLarge = 16,
            cornerPoster = 28, cornerPill = 28, cornerRowCover = 14,
            uppercaseTitles = false, hardShadow = false, flowField = false,
            displayFont = AppDisplayFont.SYSTEM_BLACK,
            cornerCard = 16, cornerTile = 16, cornerChip = 999,
            cornerSheet = 28, cornerButton = 999, cornerMiniPlayer = 999,
            rowPaddingV = 8, rowMinHeight = 72, sectionPaddingV = 10,
            sectionTopPadding = 24, cardPadding = 16, gridGap = 10,
            headerPaddingV = 10, sectionTitleSize = 30, posterTitleSize = 42,
            detailTitleSize = 36, labelLetterSpacingTenths = 13,
            displayLetterSpacingTenths = 0, cardBorderWidth = 1,
            dividerThickness = 1, dividerAlphaPercent = 8,
            hardShadowPoster = 0, hardShadowTile = 0, hardShadowChip = 0,
            glowElevation = 0, glowAlphaPercent = 0,
            bodyFont = AppBodyFont.SYSTEM, labelFont = AppLabelFont.SYSTEM_MONO,
            motionEasing = AppMotionEasing.FAST_OUT_SLOW_IN,
            morphMs = 150, tabSlideMs = 300, panelFadeMs = 180,
            panelExitMs = 120, tintMs = 200, waveAmpMs = 450,
            clusterWaveHeight = 6, clusterPlayerHeight = 54,
            clusterPlayerCover = 38, clusterPlayerAction = 44,
            clusterPlayerControl = 40, clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22, clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14, clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10, clusterPlayerGap = 8,
            clusterNavHeight = 56, clusterNavIcon = 24,
            clusterNavLabelSize = 12, clusterNavItemHeight = 48,
            clusterNavPaddingV = 4, clusterGap = 6,
            clusterBottomInset = 8, clusterJamChipHeight = 28,
            clusterJamChipGap = 6, clusterJamChipIcon = 18,
        )
    }
    AppThemeId.SENJA -> when (mode) {
        AppThemeMode.DARK -> AppThemePalette(
            background = 0xFF17121F,
            surface = 0xFF221A30,
            surfaceVariant = 0xFF2E2142,
            text = 0xFFF7EFE1,
            textMuted = 0xFFA99BC2,
            outline = 0xFF372A4E,
            accent = 0xFFFFB347,
            accentFill = 0xFFFFB347,
            accentDark = 0xFFFF7A4D,
            accentSoft = 0xFF3A2C4A,
            accentOnDark = 0xFFFFB347,
            onAccent = 0xFF171006,
            chrome = 0xFF2E2142,
            onChrome = 0xFFF7EFE1,
            stage = 0xFF2E2142,
            avatar = 0xFFFFB347,
            onAvatar = 0xFF171006,
            poster = 0xFFFFB347,
            posterFallback = 0xFF3A2B52,
            error = 0xFFF87171,
            cornerSmall = 4, cornerMedium = 8, cornerLarge = 10,
            cornerPoster = 16, cornerPill = 999, cornerRowCover = 8,
            uppercaseTitles = false, hardShadow = false, flowField = false,
            displayFont = AppDisplayFont.JAKARTA,
            cornerCard = 10, cornerTile = 10, cornerChip = 999,
            cornerSheet = 20, cornerButton = 999, cornerMiniPlayer = 20,
            rowPaddingV = 6, rowMinHeight = 64, sectionPaddingV = 6,
            sectionTopPadding = 16, cardPadding = 12, gridGap = 8,
            headerPaddingV = 8, sectionTitleSize = 26, posterTitleSize = 36,
            detailTitleSize = 34, labelLetterSpacingTenths = 10,
            displayLetterSpacingTenths = 0, cardBorderWidth = 1,
            dividerThickness = 1, dividerAlphaPercent = 100,
            hardShadowPoster = 0, hardShadowTile = 0, hardShadowChip = 0,
            glowElevation = 12, glowAlphaPercent = 35,
            bodyFont = AppBodyFont.JAKARTA, labelFont = AppLabelFont.SYSTEM_MONO,
            motionEasing = AppMotionEasing.FAST_OUT_SLOW_IN,
            morphMs = 195, tabSlideMs = 390, panelFadeMs = 235,
            panelExitMs = 155, tintMs = 260, waveAmpMs = 580,
            clusterWaveHeight = 6, clusterPlayerHeight = 54,
            clusterPlayerCover = 38, clusterPlayerAction = 44,
            clusterPlayerControl = 40, clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22, clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14, clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10, clusterPlayerGap = 8,
            clusterNavHeight = 56, clusterNavIcon = 24,
            clusterNavLabelSize = 12, clusterNavItemHeight = 48,
            clusterNavPaddingV = 4, clusterGap = 6,
            clusterBottomInset = 8, clusterJamChipHeight = 28,
            clusterJamChipGap = 6, clusterJamChipIcon = 18,
        )
        AppThemeMode.LIGHT -> AppThemePalette(
            background = 0xFFF7EFE1,
            surface = 0xFFFFFDF8,
            surfaceVariant = 0xFFEAE2F0,
            text = 0xFF17121F,
            textMuted = 0xFF6E6390,
            outline = 0xFFD9CBE8,
            accent = 0xFF8A5A00,
            accentFill = 0xFFFFB347,
            accentDark = 0xFFE85A2E,
            accentSoft = 0xFFFBEED3,
            accentOnDark = 0xFFFFB347,
            onAccent = 0xFF171006,
            chrome = 0xFF17121F,
            onChrome = 0xFFF7EFE1,
            stage = 0xFF17121F,
            avatar = 0xFFFFB347,
            onAvatar = 0xFF171006,
            poster = 0xFFE8930C,
            posterFallback = 0xFF8A5A00,
            error = 0xFFB91C1C,
            cornerSmall = 4, cornerMedium = 8, cornerLarge = 10,
            cornerPoster = 16, cornerPill = 999, cornerRowCover = 8,
            uppercaseTitles = false, hardShadow = false, flowField = false,
            displayFont = AppDisplayFont.JAKARTA,
            cornerCard = 10, cornerTile = 10, cornerChip = 999,
            cornerSheet = 20, cornerButton = 999, cornerMiniPlayer = 20,
            rowPaddingV = 6, rowMinHeight = 64, sectionPaddingV = 6,
            sectionTopPadding = 16, cardPadding = 12, gridGap = 8,
            headerPaddingV = 8, sectionTitleSize = 26, posterTitleSize = 36,
            detailTitleSize = 34, labelLetterSpacingTenths = 10,
            displayLetterSpacingTenths = 0, cardBorderWidth = 1,
            dividerThickness = 1, dividerAlphaPercent = 100,
            hardShadowPoster = 0, hardShadowTile = 0, hardShadowChip = 0,
            glowElevation = 12, glowAlphaPercent = 35,
            bodyFont = AppBodyFont.JAKARTA, labelFont = AppLabelFont.SYSTEM_MONO,
            motionEasing = AppMotionEasing.FAST_OUT_SLOW_IN,
            morphMs = 195, tabSlideMs = 390, panelFadeMs = 235,
            panelExitMs = 155, tintMs = 260, waveAmpMs = 580,
            clusterWaveHeight = 6, clusterPlayerHeight = 54,
            clusterPlayerCover = 38, clusterPlayerAction = 44,
            clusterPlayerControl = 40, clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22, clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14, clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10, clusterPlayerGap = 8,
            clusterNavHeight = 56, clusterNavIcon = 24,
            clusterNavLabelSize = 12, clusterNavItemHeight = 48,
            clusterNavPaddingV = 4, clusterGap = 6,
            clusterBottomInset = 8, clusterJamChipHeight = 28,
            clusterJamChipGap = 6, clusterJamChipIcon = 18,
        )
    }
    AppThemeId.BLOK -> when (mode) {
        AppThemeMode.LIGHT -> AppThemePalette(
            background = 0xFFFFF1C9,
            surface = 0xFFFFFFFF,
            surfaceVariant = 0xFFFFE36E,
            text = 0xFF131313,
            textMuted = 0xFF4A4438,
            outline = 0xFF131313,
            accent = 0xFFFF3D68,
            accentFill = 0xFFFF3D68,
            accentDark = 0xFF245BFF,
            accentSoft = 0xFFFFD9E2,
            accentOnDark = 0xFFFF3D68,
            // v1.10.40: tinta gelap tema sendiri (dulu putih 3.43:1
            // di atas accentFill — kini >= 5.41:1 di BLOK).
            onAccent = 0xFF131313,
            chrome = 0xFF131313,
            onChrome = 0xFFFFF1C9,
            stage = 0xFF131313,
            avatar = 0xFFFF3D68,
            onAvatar = 0xFF131313,
            poster = 0xFFFF3D68,
            posterFallback = 0xFFFF3D68,
            error = 0xFFDC2626,
            cornerSmall = 4, cornerMedium = 6, cornerLarge = 6,
            cornerPoster = 6, cornerPill = 8, cornerRowCover = 6,
            uppercaseTitles = true, hardShadow = true, flowField = false,
            displayFont = AppDisplayFont.BRICOLAGE,
            cornerCard = 6, cornerTile = 6, cornerChip = 8,
            cornerSheet = 6, cornerButton = 8, cornerMiniPlayer = 8,
            rowPaddingV = 12, rowMinHeight = 76, sectionPaddingV = 14,
            sectionTopPadding = 28, cardPadding = 16, gridGap = 14,
            headerPaddingV = 12, sectionTitleSize = 32, posterTitleSize = 40,
            detailTitleSize = 38, labelLetterSpacingTenths = 8,
            displayLetterSpacingTenths = -5, cardBorderWidth = 2,
            dividerThickness = 2, dividerAlphaPercent = 100,
            hardShadowPoster = 5, hardShadowTile = 4, hardShadowChip = 3,
            glowElevation = 0, glowAlphaPercent = 0,
            bodyFont = AppBodyFont.SYSTEM, labelFont = AppLabelFont.SYSTEM_MONO,
            motionEasing = AppMotionEasing.LINEAR_OUT_SLOW_IN,
            morphMs = 110, tabSlideMs = 225, panelFadeMs = 135,
            panelExitMs = 90, tintMs = 150, waveAmpMs = 340,
            clusterWaveHeight = 6, clusterPlayerHeight = 54,
            clusterPlayerCover = 38, clusterPlayerAction = 44,
            clusterPlayerControl = 40, clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22, clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14, clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10, clusterPlayerGap = 8,
            clusterNavHeight = 56, clusterNavIcon = 24,
            clusterNavLabelSize = 12, clusterNavItemHeight = 48,
            clusterNavPaddingV = 4, clusterGap = 6,
            clusterBottomInset = 8, clusterJamChipHeight = 28,
            clusterJamChipGap = 6, clusterJamChipIcon = 18,
        )
        AppThemeMode.DARK -> AppThemePalette(
            background = 0xFF131313,
            surface = 0xFF1E1C19,
            surfaceVariant = 0xFF2E2A20,
            text = 0xFFFFF1C9,
            textMuted = 0xFFB8AC93,
            outline = 0xFFFFF1C9,
            accent = 0xFFFF3D68,
            accentFill = 0xFFFF3D68,
            accentDark = 0xFF7C9EFF,
            accentSoft = 0xFF4A2330,
            accentOnDark = 0xFFFF3D68,
            // v1.10.40: tinta gelap tema sendiri (dulu putih 3.43:1).
            onAccent = 0xFF131313,
            chrome = 0xFFFFF1C9,
            onChrome = 0xFF131313,
            stage = 0xFF262019,
            avatar = 0xFFFF3D68,
            onAvatar = 0xFF131313,
            poster = 0xFFFF3D68,
            posterFallback = 0xFFFF3D68,
            error = 0xFFF87171,
            cornerSmall = 4, cornerMedium = 6, cornerLarge = 6,
            cornerPoster = 6, cornerPill = 8, cornerRowCover = 6,
            uppercaseTitles = true, hardShadow = true, flowField = false,
            displayFont = AppDisplayFont.BRICOLAGE,
            cornerCard = 6, cornerTile = 6, cornerChip = 8,
            cornerSheet = 6, cornerButton = 8, cornerMiniPlayer = 8,
            rowPaddingV = 12, rowMinHeight = 76, sectionPaddingV = 14,
            sectionTopPadding = 28, cardPadding = 16, gridGap = 14,
            headerPaddingV = 12, sectionTitleSize = 32, posterTitleSize = 40,
            detailTitleSize = 38, labelLetterSpacingTenths = 8,
            displayLetterSpacingTenths = -5, cardBorderWidth = 2,
            dividerThickness = 2, dividerAlphaPercent = 100,
            hardShadowPoster = 5, hardShadowTile = 4, hardShadowChip = 3,
            glowElevation = 0, glowAlphaPercent = 0,
            bodyFont = AppBodyFont.SYSTEM, labelFont = AppLabelFont.SYSTEM_MONO,
            motionEasing = AppMotionEasing.LINEAR_OUT_SLOW_IN,
            morphMs = 110, tabSlideMs = 225, panelFadeMs = 135,
            panelExitMs = 90, tintMs = 150, waveAmpMs = 340,
            clusterWaveHeight = 6, clusterPlayerHeight = 54,
            clusterPlayerCover = 38, clusterPlayerAction = 44,
            clusterPlayerControl = 40, clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22, clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14, clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10, clusterPlayerGap = 8,
            clusterNavHeight = 56, clusterNavIcon = 24,
            clusterNavLabelSize = 12, clusterNavItemHeight = 48,
            clusterNavPaddingV = 4, clusterGap = 6,
            clusterBottomInset = 8, clusterJamChipHeight = 28,
            clusterJamChipGap = 6, clusterJamChipIcon = 18,
        )
    }
    AppThemeId.GETAR -> when (mode) {
        AppThemeMode.DARK -> AppThemePalette(
            background = 0xFF0E0E14,
            surface = 0xFF171724,
            surfaceVariant = 0xFF1F1F2E,
            text = 0xFFF2EFE9,
            textMuted = 0xFF9A97A3,
            outline = 0xFF2A2A3A,
            accent = 0xFFFF8A7A,
            accentFill = 0xFFFF8A7A,
            accentDark = 0xFFB3261E,
            accentSoft = 0xFF23232F,
            accentOnDark = 0xFFFF8A7A,
            onAccent = 0xFF0E0E14,
            chrome = 0xFF171724,
            onChrome = 0xFFF2EFE9,
            stage = 0xFF171724,
            avatar = 0xFFFF8A7A,
            onAvatar = 0xFF0E0E14,
            poster = 0xFFFF8A7A,
            posterFallback = 0xFF171724,
            error = 0xFFFF6B6B,
            cornerSmall = 4, cornerMedium = 8, cornerLarge = 12,
            cornerPoster = 12, cornerPill = 10, cornerRowCover = 8,
            uppercaseTitles = false, hardShadow = false, flowField = true,
            displayFont = AppDisplayFont.ARCHIVO,
            cornerCard = 12, cornerTile = 12, cornerChip = 10,
            cornerSheet = 12, cornerButton = 10, cornerMiniPlayer = 12,
            rowPaddingV = 11, rowMinHeight = 72, sectionPaddingV = 14,
            sectionTopPadding = 26, cardPadding = 18, gridGap = 12,
            headerPaddingV = 14, sectionTitleSize = 22, posterTitleSize = 34,
            detailTitleSize = 32, labelLetterSpacingTenths = 15,
            displayLetterSpacingTenths = -6, cardBorderWidth = 1,
            dividerThickness = 1, dividerAlphaPercent = 100,
            hardShadowPoster = 0, hardShadowTile = 0, hardShadowChip = 0,
            glowElevation = 0, glowAlphaPercent = 0,
            bodyFont = AppBodyFont.ARCHIVO, labelFont = AppLabelFont.JETBRAINS,
            motionEasing = AppMotionEasing.LINEAR,
            morphMs = 160, tabSlideMs = 200, panelFadeMs = 140,
            panelExitMs = 100, tintMs = 160, waveAmpMs = 400,
            clusterWaveHeight = 6, clusterPlayerHeight = 54,
            clusterPlayerCover = 38, clusterPlayerAction = 44,
            clusterPlayerControl = 40, clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22, clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14, clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10, clusterPlayerGap = 8,
            clusterNavHeight = 56, clusterNavIcon = 24,
            clusterNavLabelSize = 12, clusterNavItemHeight = 48,
            clusterNavPaddingV = 4, clusterGap = 6,
            clusterBottomInset = 8, clusterJamChipHeight = 28,
            clusterJamChipGap = 6, clusterJamChipIcon = 18,
        )
        AppThemeMode.LIGHT -> AppThemePalette(
            background = 0xFFF2EFE9,
            surface = 0xFFFFFFFF,
            surfaceVariant = 0xFFE6E2D9,
            text = 0xFF0E0E14,
            textMuted = 0xFF6E6A63,
            outline = 0xFFC9C6BE,
            accent = 0xFFB3261E,
            accentFill = 0xFFFF8A7A,
            accentDark = 0xFFB3261E,
            accentSoft = 0xFFFBE9E7,
            accentOnDark = 0xFFFF8A7A,
            onAccent = 0xFF0E0E14,
            chrome = 0xFF0E0E14,
            onChrome = 0xFFF2EFE9,
            stage = 0xFF0E0E14,
            avatar = 0xFFFF8A7A,
            onAvatar = 0xFF0E0E14,
            poster = 0xFFB3261E,
            posterFallback = 0xFF0E0E14,
            error = 0xFFB91C1C,
            cornerSmall = 4, cornerMedium = 8, cornerLarge = 12,
            cornerPoster = 12, cornerPill = 10, cornerRowCover = 8,
            uppercaseTitles = false, hardShadow = false, flowField = true,
            displayFont = AppDisplayFont.ARCHIVO,
            cornerCard = 12, cornerTile = 12, cornerChip = 10,
            cornerSheet = 12, cornerButton = 10, cornerMiniPlayer = 12,
            rowPaddingV = 11, rowMinHeight = 72, sectionPaddingV = 14,
            sectionTopPadding = 26, cardPadding = 18, gridGap = 12,
            headerPaddingV = 14, sectionTitleSize = 22, posterTitleSize = 34,
            detailTitleSize = 32, labelLetterSpacingTenths = 15,
            displayLetterSpacingTenths = -6, cardBorderWidth = 1,
            dividerThickness = 1, dividerAlphaPercent = 100,
            hardShadowPoster = 0, hardShadowTile = 0, hardShadowChip = 0,
            glowElevation = 0, glowAlphaPercent = 0,
            bodyFont = AppBodyFont.ARCHIVO, labelFont = AppLabelFont.JETBRAINS,
            motionEasing = AppMotionEasing.LINEAR,
            morphMs = 160, tabSlideMs = 200, panelFadeMs = 140,
            panelExitMs = 100, tintMs = 160, waveAmpMs = 400,
            clusterWaveHeight = 6, clusterPlayerHeight = 54,
            clusterPlayerCover = 38, clusterPlayerAction = 44,
            clusterPlayerControl = 40, clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22, clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14, clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10, clusterPlayerGap = 8,
            clusterNavHeight = 56, clusterNavIcon = 24,
            clusterNavLabelSize = 12, clusterNavItemHeight = 48,
            clusterNavPaddingV = 4, clusterGap = 6,
            clusterBottomInset = 8, clusterJamChipHeight = 28,
            clusterJamChipGap = 6, clusterJamChipIcon = 18,
        )
    }
    AppThemeId.RASI -> when (mode) {
        AppThemeMode.LIGHT -> AppThemePalette(
            background = 0xFFEEF4FB,
            surface = 0xFFFFFFFF,
            surfaceVariant = 0xFFDBE6F7,
            text = 0xFF101A2E,
            textMuted = 0xFF5A6B8A,
            outline = 0xFFD3DEEF,
            accent = 0xFF3157D5,
            accentFill = 0xFF3157D5,
            accentDark = 0xFF1E3FA0,
            accentSoft = 0xFFE3EBFA,
            accentOnDark = 0xFF8AB4FF,
            onAccent = 0xFFFFFFFF,
            chrome = 0xFF3157D5,
            onChrome = 0xFFFFFFFF,
            stage = 0xFF101A2E,
            avatar = 0xFF3157D5,
            onAvatar = 0xFFFFFFFF,
            poster = 0xFF8AB4FF,
            posterFallback = 0xFF3157D5,
            error = 0xFFB3261E,
            cornerSmall = 4,
            cornerMedium = 8,
            cornerLarge = 28,
            cornerPoster = 28,
            cornerPill = 999,
            cornerRowCover = 999,
            uppercaseTitles = false,
            hardShadow = false,
            flowField = false,
            displayFont = AppDisplayFont.SPACE_GROTESK,
            cornerCard = 28,
            cornerTile = 22,
            cornerChip = 999,
            cornerSheet = 28,
            cornerButton = 999,
            cornerMiniPlayer = 999,
            rowPaddingV = 12,
            rowMinHeight = 74,
            sectionPaddingV = 12,
            sectionTopPadding = 26,
            cardPadding = 18,
            gridGap = 12,
            headerPaddingV = 12,
            sectionTitleSize = 26,
            posterTitleSize = 38,
            detailTitleSize = 34,
            labelLetterSpacingTenths = 14,
            displayLetterSpacingTenths = -2,
            cardBorderWidth = 1,
            dividerThickness = 1,
            dividerAlphaPercent = 100,
            hardShadowPoster = 0,
            hardShadowTile = 0,
            hardShadowChip = 0,
            glowElevation = 0,
            glowAlphaPercent = 0,
            bodyFont = AppBodyFont.SPACE_GROTESK,
            labelFont = AppLabelFont.PLEX_MONO,
            motionEasing = AppMotionEasing.FAST_OUT_SLOW_IN,
            morphMs = 420,
            tabSlideMs = 650,
            panelFadeMs = 420,
            panelExitMs = 300,
            tintMs = 520,
            waveAmpMs = 900,
            clusterWaveHeight = 6,
            clusterPlayerHeight = 54,
            clusterPlayerCover = 38,
            clusterPlayerAction = 44,
            clusterPlayerControl = 40,
            clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22,
            clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14,
            clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10,
            clusterPlayerGap = 8,
            clusterNavHeight = 56,
            clusterNavIcon = 24,
            clusterNavLabelSize = 12,
            clusterNavItemHeight = 48,
            clusterNavPaddingV = 4,
            clusterGap = 6,
            clusterBottomInset = 8,
            clusterJamChipHeight = 28,
            clusterJamChipGap = 6,
            clusterJamChipIcon = 18,
            cornerDetailCover = 999,
            orbitRing = true,
            dotGridBackground = true,
            constellationLines = true,
            dashedBorders = false,
            ticketNotches = false,
            stampBadge = false,
            flatNoCards = false,
            textTabNav = false,
            chartNumerals = false,
        )
        AppThemeMode.DARK -> AppThemePalette(
            background = 0xFF070D24,
            surface = 0xFF101A3D,
            surfaceVariant = 0xFF16224A,
            text = 0xFFE8EFFF,
            textMuted = 0xFF93A7CC,
            outline = 0xFF22315E,
            accent = 0xFF8AB4FF,
            accentFill = 0xFF8AB4FF,
            accentDark = 0xFF6F9BF0,
            accentSoft = 0xFF16224A,
            accentOnDark = 0xFF8AB4FF,
            onAccent = 0xFF070D24,
            chrome = 0xFF8AB4FF,
            onChrome = 0xFF070D24,
            stage = 0xFF101A3D,
            avatar = 0xFF8AB4FF,
            onAvatar = 0xFF070D24,
            poster = 0xFF8AB4FF,
            posterFallback = 0xFF22315E,
            error = 0xFFFF8A80,
            cornerSmall = 4,
            cornerMedium = 8,
            cornerLarge = 28,
            cornerPoster = 28,
            cornerPill = 999,
            cornerRowCover = 999,
            uppercaseTitles = false,
            hardShadow = false,
            flowField = false,
            displayFont = AppDisplayFont.SPACE_GROTESK,
            cornerCard = 28,
            cornerTile = 22,
            cornerChip = 999,
            cornerSheet = 28,
            cornerButton = 999,
            cornerMiniPlayer = 999,
            rowPaddingV = 12,
            rowMinHeight = 74,
            sectionPaddingV = 12,
            sectionTopPadding = 26,
            cardPadding = 18,
            gridGap = 12,
            headerPaddingV = 12,
            sectionTitleSize = 26,
            posterTitleSize = 38,
            detailTitleSize = 34,
            labelLetterSpacingTenths = 14,
            displayLetterSpacingTenths = -2,
            cardBorderWidth = 1,
            dividerThickness = 1,
            dividerAlphaPercent = 100,
            hardShadowPoster = 0,
            hardShadowTile = 0,
            hardShadowChip = 0,
            glowElevation = 0,
            glowAlphaPercent = 0,
            bodyFont = AppBodyFont.SPACE_GROTESK,
            labelFont = AppLabelFont.PLEX_MONO,
            motionEasing = AppMotionEasing.FAST_OUT_SLOW_IN,
            morphMs = 420,
            tabSlideMs = 650,
            panelFadeMs = 420,
            panelExitMs = 300,
            tintMs = 520,
            waveAmpMs = 900,
            clusterWaveHeight = 6,
            clusterPlayerHeight = 54,
            clusterPlayerCover = 38,
            clusterPlayerAction = 44,
            clusterPlayerControl = 40,
            clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22,
            clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14,
            clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10,
            clusterPlayerGap = 8,
            clusterNavHeight = 56,
            clusterNavIcon = 24,
            clusterNavLabelSize = 12,
            clusterNavItemHeight = 48,
            clusterNavPaddingV = 4,
            clusterGap = 6,
            clusterBottomInset = 8,
            clusterJamChipHeight = 28,
            clusterJamChipGap = 6,
            clusterJamChipIcon = 18,
            cornerDetailCover = 999,
            orbitRing = true,
            dotGridBackground = true,
            constellationLines = true,
            dashedBorders = false,
            ticketNotches = false,
            stampBadge = false,
            flatNoCards = false,
            textTabNav = false,
            chartNumerals = false,
        )
    }
    AppThemeId.PASAR -> when (mode) {
        AppThemeMode.LIGHT -> AppThemePalette(
            background = 0xFFFFF6DC,
            surface = 0xFFFFFDF4,
            surfaceVariant = 0xFFFFEEC2,
            text = 0xFF1C1305,
            textMuted = 0xFF6B5A33,
            outline = 0xFF1C1305,
            accent = 0xFFE63329,
            accentFill = 0xFFC81E15,
            accentDark = 0xFFB71F18,
            accentSoft = 0xFFFDE3DF,
            accentOnDark = 0xFFFF3B30,
            onAccent = 0xFFFFFFFF,
            chrome = 0xFF1C1305,
            onChrome = 0xFFFFF6DC,
            stage = 0xFF1C1305,
            avatar = 0xFFF5B800,
            onAvatar = 0xFF1C1305,
            poster = 0xFFF5B800,
            posterFallback = 0xFFC81E15,
            error = 0xFFB3261E,
            cornerSmall = 4,
            cornerMedium = 6,
            cornerLarge = 6,
            cornerPoster = 6,
            cornerPill = 8,
            cornerRowCover = 6,
            uppercaseTitles = true,
            hardShadow = false,
            flowField = false,
            displayFont = AppDisplayFont.ANTON,
            cornerCard = 6,
            cornerTile = 6,
            cornerChip = 4,
            cornerSheet = 6,
            cornerButton = 4,
            cornerMiniPlayer = 999,
            rowPaddingV = 6,
            rowMinHeight = 62,
            sectionPaddingV = 8,
            sectionTopPadding = 18,
            cardPadding = 14,
            gridGap = 8,
            headerPaddingV = 8,
            sectionTitleSize = 30,
            posterTitleSize = 40,
            detailTitleSize = 36,
            labelLetterSpacingTenths = 8,
            displayLetterSpacingTenths = 1,
            cardBorderWidth = 2,
            dividerThickness = 2,
            dividerAlphaPercent = 35,
            hardShadowPoster = 0,
            hardShadowTile = 0,
            hardShadowChip = 0,
            glowElevation = 0,
            glowAlphaPercent = 0,
            bodyFont = AppBodyFont.INTER,
            labelFont = AppLabelFont.INTER,
            motionEasing = AppMotionEasing.OVERSHOOT,
            morphMs = 90,
            tabSlideMs = 200,
            panelFadeMs = 120,
            panelExitMs = 80,
            tintMs = 120,
            waveAmpMs = 280,
            clusterWaveHeight = 6,
            clusterPlayerHeight = 54,
            clusterPlayerCover = 38,
            clusterPlayerAction = 44,
            clusterPlayerControl = 40,
            clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22,
            clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14,
            clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10,
            clusterPlayerGap = 8,
            clusterNavHeight = 56,
            clusterNavIcon = 24,
            clusterNavLabelSize = 12,
            clusterNavItemHeight = 48,
            clusterNavPaddingV = 4,
            clusterGap = 6,
            clusterBottomInset = 8,
            clusterJamChipHeight = 28,
            clusterJamChipGap = 6,
            clusterJamChipIcon = 18,
            cornerDetailCover = 6,
            orbitRing = false,
            dotGridBackground = false,
            constellationLines = false,
            dashedBorders = true,
            ticketNotches = true,
            stampBadge = true,
            flatNoCards = false,
            textTabNav = false,
            chartNumerals = false,
        )
        AppThemeMode.DARK -> AppThemePalette(
            background = 0xFF1A1816,
            surface = 0xFF26221E,
            surfaceVariant = 0xFF332E26,
            text = 0xFFFFF6DC,
            textMuted = 0xFFB8AB8A,
            outline = 0xFFFFF6DC,
            accent = 0xFFFF3B30,
            accentFill = 0xFFFF3B30,
            accentDark = 0xFFFF6B61,
            accentSoft = 0xFF3D2A24,
            accentOnDark = 0xFFFF3B30,
            onAccent = 0xFF1A1816,
            chrome = 0xFFFFF6DC,
            onChrome = 0xFF1A1816,
            stage = 0xFF26221E,
            avatar = 0xFFFFD60A,
            onAvatar = 0xFF1A1816,
            poster = 0xFFFFD60A,
            posterFallback = 0xFF8A1A12,
            error = 0xFFFF8A80,
            cornerSmall = 4,
            cornerMedium = 6,
            cornerLarge = 6,
            cornerPoster = 6,
            cornerPill = 8,
            cornerRowCover = 6,
            uppercaseTitles = true,
            hardShadow = false,
            flowField = false,
            displayFont = AppDisplayFont.ANTON,
            cornerCard = 6,
            cornerTile = 6,
            cornerChip = 4,
            cornerSheet = 6,
            cornerButton = 4,
            cornerMiniPlayer = 999,
            rowPaddingV = 6,
            rowMinHeight = 62,
            sectionPaddingV = 8,
            sectionTopPadding = 18,
            cardPadding = 14,
            gridGap = 8,
            headerPaddingV = 8,
            sectionTitleSize = 30,
            posterTitleSize = 40,
            detailTitleSize = 36,
            labelLetterSpacingTenths = 8,
            displayLetterSpacingTenths = 1,
            cardBorderWidth = 2,
            dividerThickness = 2,
            dividerAlphaPercent = 35,
            hardShadowPoster = 0,
            hardShadowTile = 0,
            hardShadowChip = 0,
            glowElevation = 0,
            glowAlphaPercent = 0,
            bodyFont = AppBodyFont.INTER,
            labelFont = AppLabelFont.INTER,
            motionEasing = AppMotionEasing.OVERSHOOT,
            morphMs = 90,
            tabSlideMs = 200,
            panelFadeMs = 120,
            panelExitMs = 80,
            tintMs = 120,
            waveAmpMs = 280,
            clusterWaveHeight = 6,
            clusterPlayerHeight = 54,
            clusterPlayerCover = 38,
            clusterPlayerAction = 44,
            clusterPlayerControl = 40,
            clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22,
            clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14,
            clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10,
            clusterPlayerGap = 8,
            clusterNavHeight = 56,
            clusterNavIcon = 24,
            clusterNavLabelSize = 12,
            clusterNavItemHeight = 48,
            clusterNavPaddingV = 4,
            clusterGap = 6,
            clusterBottomInset = 8,
            clusterJamChipHeight = 28,
            clusterJamChipGap = 6,
            clusterJamChipIcon = 18,
            cornerDetailCover = 6,
            orbitRing = false,
            dotGridBackground = false,
            constellationLines = false,
            dashedBorders = true,
            ticketNotches = true,
            stampBadge = true,
            flatNoCards = false,
            textTabNav = false,
            chartNumerals = false,
        )
    }
    AppThemeId.SENYAP -> when (mode) {
        AppThemeMode.LIGHT -> AppThemePalette(
            background = 0xFFFAF8F3,
            surface = 0xFFFAF8F3,
            surfaceVariant = 0xFFFAF8F3,
            text = 0xFF111110,
            textMuted = 0xFF6F6C66,
            outline = 0xFF111110,
            accent = 0xFF111110,
            accentFill = 0xFF111110,
            accentDark = 0xFF000000,
            accentSoft = 0xFFECE8DF,
            accentOnDark = 0xFFFAF8F3,
            onAccent = 0xFFFAF8F3,
            chrome = 0xFF111110,
            onChrome = 0xFFFAF8F3,
            stage = 0xFF111110,
            avatar = 0xFF111110,
            onAvatar = 0xFFFAF8F3,
            poster = 0xFF111110,
            posterFallback = 0xFF111110,
            error = 0xFFB3261E,
            cornerSmall = 2,
            cornerMedium = 2,
            cornerLarge = 2,
            cornerPoster = 2,
            cornerPill = 2,
            cornerRowCover = 2,
            uppercaseTitles = false,
            hardShadow = false,
            flowField = false,
            displayFont = AppDisplayFont.FRAUNCES,
            cornerCard = 2,
            cornerTile = 2,
            cornerChip = 2,
            cornerSheet = 2,
            cornerButton = 2,
            cornerMiniPlayer = 2,
            rowPaddingV = 14,
            rowMinHeight = 78,
            sectionPaddingV = 16,
            sectionTopPadding = 30,
            cardPadding = 20,
            gridGap = 14,
            headerPaddingV = 16,
            sectionTitleSize = 26,
            posterTitleSize = 44,
            detailTitleSize = 40,
            labelLetterSpacingTenths = 10,
            displayLetterSpacingTenths = -2,
            cardBorderWidth = 0,
            dividerThickness = 1,
            dividerAlphaPercent = 18,
            hardShadowPoster = 0,
            hardShadowTile = 0,
            hardShadowChip = 0,
            glowElevation = 0,
            glowAlphaPercent = 0,
            bodyFont = AppBodyFont.INTER,
            labelFont = AppLabelFont.INTER,
            motionEasing = AppMotionEasing.LINEAR,
            morphMs = 500,
            tabSlideMs = 500,
            panelFadeMs = 500,
            panelExitMs = 400,
            tintMs = 500,
            waveAmpMs = 800,
            clusterWaveHeight = 6,
            clusterPlayerHeight = 54,
            clusterPlayerCover = 38,
            clusterPlayerAction = 44,
            clusterPlayerControl = 40,
            clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22,
            clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14,
            clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10,
            clusterPlayerGap = 8,
            clusterNavHeight = 56,
            clusterNavIcon = 24,
            clusterNavLabelSize = 12,
            clusterNavItemHeight = 48,
            clusterNavPaddingV = 4,
            clusterGap = 6,
            clusterBottomInset = 8,
            clusterJamChipHeight = 28,
            clusterJamChipGap = 6,
            clusterJamChipIcon = 18,
            cornerDetailCover = 2,
            orbitRing = false,
            dotGridBackground = false,
            constellationLines = false,
            dashedBorders = false,
            ticketNotches = false,
            stampBadge = false,
            flatNoCards = true,
            textTabNav = true,
            chartNumerals = true,
        )
        AppThemeMode.DARK -> AppThemePalette(
            background = 0xFF111110,
            surface = 0xFF111110,
            surfaceVariant = 0xFF111110,
            text = 0xFFFAF8F3,
            textMuted = 0xFFA3A099,
            outline = 0xFFFAF8F3,
            accent = 0xFFFAF8F3,
            accentFill = 0xFFFAF8F3,
            accentDark = 0xFFFFFFFF,
            accentSoft = 0xFF26241F,
            accentOnDark = 0xFFFAF8F3,
            onAccent = 0xFF111110,
            chrome = 0xFFFAF8F3,
            onChrome = 0xFF111110,
            stage = 0xFF1E1C1A,
            avatar = 0xFFFAF8F3,
            onAvatar = 0xFF111110,
            poster = 0xFFFAF8F3,
            posterFallback = 0xFF2A2926,
            error = 0xFFFF8A80,
            cornerSmall = 2,
            cornerMedium = 2,
            cornerLarge = 2,
            cornerPoster = 2,
            cornerPill = 2,
            cornerRowCover = 2,
            uppercaseTitles = false,
            hardShadow = false,
            flowField = false,
            displayFont = AppDisplayFont.FRAUNCES,
            cornerCard = 2,
            cornerTile = 2,
            cornerChip = 2,
            cornerSheet = 2,
            cornerButton = 2,
            cornerMiniPlayer = 2,
            rowPaddingV = 14,
            rowMinHeight = 78,
            sectionPaddingV = 16,
            sectionTopPadding = 30,
            cardPadding = 20,
            gridGap = 14,
            headerPaddingV = 16,
            sectionTitleSize = 26,
            posterTitleSize = 44,
            detailTitleSize = 40,
            labelLetterSpacingTenths = 10,
            displayLetterSpacingTenths = -2,
            cardBorderWidth = 0,
            dividerThickness = 1,
            dividerAlphaPercent = 18,
            hardShadowPoster = 0,
            hardShadowTile = 0,
            hardShadowChip = 0,
            glowElevation = 0,
            glowAlphaPercent = 0,
            bodyFont = AppBodyFont.INTER,
            labelFont = AppLabelFont.INTER,
            motionEasing = AppMotionEasing.LINEAR,
            morphMs = 500,
            tabSlideMs = 500,
            panelFadeMs = 500,
            panelExitMs = 400,
            tintMs = 500,
            waveAmpMs = 800,
            clusterWaveHeight = 6,
            clusterPlayerHeight = 54,
            clusterPlayerCover = 38,
            clusterPlayerAction = 44,
            clusterPlayerControl = 40,
            clusterPlayerControlIcon = 24,
            clusterPlayerLikeIcon = 22,
            clusterPlayerPlayIcon = 24,
            clusterPlayerTitleSize = 14,
            clusterPlayerArtistSize = 12,
            clusterPlayerPaddingH = 10,
            clusterPlayerGap = 8,
            clusterNavHeight = 56,
            clusterNavIcon = 24,
            clusterNavLabelSize = 12,
            clusterNavItemHeight = 48,
            clusterNavPaddingV = 4,
            clusterGap = 6,
            clusterBottomInset = 8,
            clusterJamChipHeight = 28,
            clusterJamChipGap = 6,
            clusterJamChipIcon = 18,
            cornerDetailCover = 2,
            orbitRing = false,
            dotGridBackground = false,
            constellationLines = false,
            dashedBorders = false,
            ticketNotches = false,
            stampBadge = false,
            flatNoCards = true,
            textTabNav = true,
            chartNumerals = true,
        )
    }

}

/**
 * Palet seed GETAR: satu pasangan (fill luminous untuk dasar gelap /
 * isi tombol, deep untuk teks di atas dasar terang) per seed. Seed
 * berasal dari hash id lagu — stabil antar sesi, deterministik.
 * Pasangan deep diverifikasi harness kontras ≥ 4.5 di atas dasar
 * terang GETAR; fill ≥ 3.0 di atas dasar gelap + dengan teks tinta.
 */
data class GetarSeed(val fill: Long, val deep: Long)

val GETAR_SEEDS: List<GetarSeed> = listOf(
    GetarSeed(fill = 0xFFFF8A7A, deep = 0xFFB3261E),
    GetarSeed(fill = 0xFFFF6B35, deep = 0xFFB93A06),
    GetarSeed(fill = 0xFFFF4D6D, deep = 0xFFBE123C),
    GetarSeed(fill = 0xFFF2B705, deep = 0xFF8A6100),
    GetarSeed(fill = 0xFFA78BFA, deep = 0xFF6D28D9),
    GetarSeed(fill = 0xFF38BDF8, deep = 0xFF0369A1),
    GetarSeed(fill = 0xFF2DD4BF, deep = 0xFF0F766E),
    GetarSeed(fill = 0xFFA3E635, deep = 0xFF3F6212),
    GetarSeed(fill = 0xFFFB7185, deep = 0xFFB91C2E),
    GetarSeed(fill = 0xFFFBBF24, deep = 0xFF854D0E),
)

/** Seed deterministik untuk satu id lagu (hashCode stabil di JVM). */
fun getarSeedFor(trackId: String): GetarSeed {
    val idx = (trackId.hashCode() and 0x7fffffff) % GETAR_SEEDS.size
    return GETAR_SEEDS[idx]
}

/** Fase garis field (0..1) deterministik per seed — garis tidak kembar. */
fun getarSeedPhase(trackId: String): Float {
    val h = trackId.hashCode() and 0x7fffffff
    return (h % 1000) / 1000f
}

/** Luminans relatif WCAG dari warna ARGB penuh (0xFF______). */
fun relativeLuminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val c = ((argb shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

/** Rasio kontras WCAG antara dua warna ARGB penuh. */
fun contrastRatio(a: Long, b: Long): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/**
 * Pasangan warna cakram play mini player (v1.0.1) — (isi, ikon).
 *
 * Cakram selalu duduk di atas kapsul GELAP (StageBg, atau text bila
 * flatNoCards), jadi isinya harus kontras terhadap kapsul — bukan
 * terhadap background tema. Aturan dari token yang ada, tanpa warna
 * baru:
 * - flatNoCards (SENYAP, kedua mode): cakram = background, ikon =
 *   text. accentFill SENYAP == warna kapsul (kontras 1.00 — cakram
 *   tak terlihat sama sekali).
 * - Bila accentOnDark != accentFill (RASI Light, PASAR Light): cakram
 *   memakai accentOnDark, aksen untuk permukaan gelap. RASI Light
 *   accentFill #3157D5 hanya 2.86 terhadap stage.
 * - Ikon = onAccent, kecuali kontrasnya terhadap cakram < 3.0 (RASI
 *   Light: putih di #8AB4FF hanya 2.09) → pakai text.
 * 10 varian lain tidak berubah: pasangan == (accentFill, onAccent).
 */
fun playDiscPair(p: AppThemePalette): Pair<Long, Long> {
    if (p.flatNoCards) return p.background to p.text
    val fill = if (p.accentOnDark != p.accentFill) p.accentOnDark else p.accentFill
    val tint = if (contrastRatio(p.onAccent, fill) >= 3.0) p.onAccent else p.text
    return fill to tint
}
