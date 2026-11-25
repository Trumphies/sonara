package com.sonara.app.data

/**
 * Spec gerak layar detail (v1.10.24) — konstanta + fungsi MURNI.
 *
 * File ini sengaja tanpa dependensi Compose supaya harness (kotlinc
 * polos, tanpa classpath Compose) menguji nilai yang BENAR-BENAR
 * dipakai UI: durasi/skala/amplitudo di bawah dibaca langsung oleh
 * `SettingsAbout.kt`, `UiCommon.kt` (MorphPlayPauseIcon), dan
 * `ArusWave.kt`. Mengubah angka di sini mengubah perilaku app —
 * harness125 mengunci nilainya.
 *
 * Prinsip: setiap animasi terikat state nyata (main/jeda, tab
 * aktif, sentuhan), bukan hiasan berjalan sendiri. Reduced-motion
 * (skala durasi animator sistem = 0) mematikan gerak dekoratif;
 * umpan balik sentuh (press-scale, ripple) tetap karena itu
 * respons langsung atas sentuhan, bukan animasi ambient.
 * Lirik TIDAK diatur di sini — perilakunya milik LyricsFollow.kt
 * + worker 10.23 dan tidak disentuh v1.10.24.
 */
object DetailMotionSpec {
    /**
     * Karakter gerak per tema (v1.10.27) — dibaca dari tabel token
     * tema aktif (AppTheme.kt), BUKAN konstanta baru di UI. Nilai
     * ARUS sama persis dengan konstanta lama di bawah (dikunci
     * harness125); Senja lebih lambat/lembut (×1,3), Blok lebih
     * cepat (×0,75), GETAR presisi (nilai tetap). Lirik + Jam TIDAK
     * memakai fungsi ini — perilakunya milik LyricsFollow/Jam.
     */
    fun morphMs(theme: AppThemeId): Int =
        paletteFor(theme, AppThemeMode.LIGHT).morphMs

    fun tabSlideMs(theme: AppThemeId): Int =
        paletteFor(theme, AppThemeMode.LIGHT).tabSlideMs

    fun panelFadeMs(theme: AppThemeId): Int =
        paletteFor(theme, AppThemeMode.LIGHT).panelFadeMs

    fun panelExitMs(theme: AppThemeId): Int =
        paletteFor(theme, AppThemeMode.LIGHT).panelExitMs

    fun tintMs(theme: AppThemeId): Int =
        paletteFor(theme, AppThemeMode.LIGHT).tintMs

    fun waveAmpMs(theme: AppThemeId): Int =
        paletteFor(theme, AppThemeMode.LIGHT).waveAmpMs

    fun motionEasing(theme: AppThemeId): AppMotionEasing =
        paletteFor(theme, AppThemeMode.LIGHT).motionEasing

    /** Morph ikon play/pause (fade + skala). */
    const val MORPH_MS = 150

    /** Animasi tint tombol aktif (shuffle/repeat, label tab). */
    const val TINT_MS = 200

    /** Geser indikator tab (FastOutSlowIn). */
    const val TAB_SLIDE_MS = 300

    /** Crossfade + geser isi panel tab (masuk). */
    const val PANEL_FADE_MS = 180

    /** Fade keluar isi panel tab lama. */
    const val PANEL_EXIT_MS = 120

    /** Amplitudo gelombang merespons main/jeda. */
    const val WAVE_AMP_MS = 450

    /** Satu napas artwork (1.0 → maks → 1.0 bolak-balik). */
    const val BREATHE_MS = 5200

    /** Skala tekan tombol transport (shuffle/prev/play/next/repeat). */
    const val PRESS_TRANSPORT = 0.88f

    /** Skala tekan pill aksi (Like/Comments/Save). */
    const val PRESS_PILL = 0.94f

    /** Skala tekan tombol tab. */
    const val PRESS_TAB = 0.97f

    /** Amplitudo gelombang (dp) saat lagu jalan. */
    const val WAVE_AMP_PLAYING_DP = 6f

    /** Amplitudo gelombang (dp) saat jeda — lebih datar, lebih tenang. */
    const val WAVE_AMP_PAUSED_DP = 3f

    /** Napas artwork: skala minimum (diam). */
    const val BREATHE_MIN = 1f

    /** Napas artwork: skala maksimum (halus, ±3,5% — tanpa layout thrash). */
    const val BREATHE_MAX = 1.035f

    /**
     * Titik kontrol CubicBezier FastOutSlowInEasing (0.4, 0.0, 0.2,
     * 1.0) — easing yang dipakai semua transisi state di layar
     * detail. Disimpan sebagai data supaya harness bisa mengunci
     * kurvanya tanpa classpath Compose; UI memakai
     * FastOutSlowInEasing yang memang dibangun dari titik ini.
     */
    val FAST_OUT_SLOW_IN_BEZIER = floatArrayOf(0.4f, 0.0f, 0.2f, 1.0f)

    /**
     * Amplitudo gelombang target (dp). Responsif hanya bila gerak
     * diizinkan: reduced-motion memakai satu amplitudo statis
     * (tanpa perubahan bentuk saat main/jeda).
     */
    fun waveAmplitudeDp(playing: Boolean, reducedMotion: Boolean): Float =
        if (reducedMotion) WAVE_AMP_PLAYING_DP
        else if (playing) WAVE_AMP_PLAYING_DP else WAVE_AMP_PAUSED_DP

    /**
     * Posisi indikator tab: nomor tab (0..tabCount-1) sebagai
     * kelipatan lebar satu tab. Di luar rentang dijepit.
     */
    fun tabIndicatorIndex(selected: Int, tabCount: Int): Int {
        if (tabCount <= 0) return 0
        return selected.coerceIn(0, tabCount - 1)
    }

    /** Skala tekan: [pressedScale] saat ditekan, 1 saat lepas. */
    fun pressScale(pressed: Boolean, pressedScale: Float): Float =
        if (pressed) pressedScale.coerceIn(0.8f, 1f) else 1f

    /** Napas artwork hidup hanya saat lagu jalan + gerak diizinkan. */
    fun breatheActive(playing: Boolean, reducedMotion: Boolean): Boolean =
        playing && !reducedMotion

    /** Gerak dekoratif (napas, aliran, geser) diizinkan? */
    fun decorativeMotionAllowed(reducedMotion: Boolean): Boolean = !reducedMotion
}
