package com.sonara.app.data

/**
 * Logika follow lirik (v1.10.18, centering v1.10.19, easing v1.10.22,
 * closed-loop v1.10.23, basis koordinat v1.10.28) — murni, bisa diuji
 * tanpa Compose.
 *
 * Aturan: follow berjalan saat baris aktif berpindah; SETIAP perpindahan
 * menggulir daftar sehingga baris aktif berada DI TENGAH viewport
 * (bukan hanya saat baris keluar viewport — perilaku 10.18 yang
 * membuat layar tampak tidak mengikuti). Skip hanya bila baris sudah
 * dalam toleransi kecil dari tengah. Gulir manual pengguna
 * menangguhkan follow selama menggeser + [HOLD_MS] setelah geseran
 * terakhir; lompatan baris aktif > [SEEK_JUMP_LINES] (seek) dan ganti
 * lagu membatalkan hold. Lirik polos tidak pernah di-follow (tidak ada
 * baris aktif — timing tidak dikarang).
 *
 * v1.10.22: gulir follow memakai easing eksplisit — jarak piksel dari
 * tengah ([centerDistancePx]) digulir dengan tween FastOutSlowIn +
 * durasi proporsional jarak ([followScrollDurationMs]), bukan spec
 * default `animateScrollToItem` (spring implisit). Nilai durasi adalah
 * pilihan builder (prototype GETAR tidak punya mekanisme/easing apa
 * pun — diverifikasi dari source-nya, lihat BUILD-v1.10.22.md).
 *
 * v1.10.28 — BASIS KOORDINAT (cacat yang lolos 10.19–10.27, dibuktikan
 * dari source Compose 1.7.0 + video HP user): `LazyListItemInfo.offset`
 * diukur dari AWAL KONTEN (setelah beforeContentPadding), BUKAN dari
 * tepi atas viewport. Di `LazyListMeasure.kt` item diposisikan dalam
 * koordinat konten dan `visualOffset` (= beforeContentPadding) baru
 * ditambahkan saat `place()`; `viewportStartOffset = -beforeContentPadding`
 * dan `viewportEndOffset = ukuran layout − beforeContentPadding`.
 * Rumus 10.19–10.27 mengasumsikan offset dari tepi viewport sehingga
 * setiap gulir undershoot tepat sebesar beforeContentPadding dan
 * loop verifikasi (basis yang sama salahnya) melaporkan "tengah"
 * padahal baris mendarat di tepi bawah daftar — persis video HP
 * (baris aktif menempel di tepi bawah, lalu tersentak ke atas).
 * Ketiga fungsi pemusatan kini menerima [beforeContentPaddingPx]
 * eksplisit dari `layoutInfo` dan memusatkan terhadap pusat viewport
 * DALAM koordinat konten. Lihat BUILD-v1.10.28.md.
 */
object LyricsFollowState {

    /** Jeda setelah geseran manual terakhir sebelum follow lanjut. */
    const val HOLD_MS: Long = 3000L

    /**
     * Batas hold selama jari menempel di daftar lirik (v1.10.19):
     * sentuhan yang event lepasnya terlewat tidak boleh membekukan
     * follow selamanya — hold selama sentuhan dibatasi nilai ini,
     * lalu hold 3 detik biasa berlaku saat lepas.
     */
    const val TOUCH_HOLD_CAP_MS: Long = 30_000L

    /** Lompatan baris aktif di atas ini dianggap seek, bukan lagu jalan. */
    const val SEEK_JUMP_LINES: Int = 3

    /** True bila perpindahan baris aktif adalah lompatan seek. */
    fun isSeekJump(previousIndex: Int, newIndex: Int): Boolean =
        previousIndex >= 0 && newIndex >= 0 &&
            kotlin.math.abs(newIndex - previousIndex) > SEEK_JUMP_LINES

    /** Follow boleh jalan: ada baris aktif dan hold sudah habis. */
    fun shouldFollow(activeIndex: Int, nowMs: Long, holdUntilMs: Long): Boolean =
        activeIndex >= 0 && nowMs >= holdUntilMs

    /** Hold baru dari "sekarang" (akhir geseran manual). */
    fun holdUntilFrom(nowMs: Long): Long = nowMs + HOLD_MS

    /**
     * Pusat viewport DALAM koordinat konten (v1.10.28): pusat layar
     * ([viewportHeightPx] / 2) digeser ke atas sebesar
     * [beforeContentPaddingPx], karena `LazyListItemInfo.offset`
     * diukur dari awal konten. Untuk padding halfViewport
     * ([beforeContentPaddingPx] == [viewportHeightPx] / 2) nilainya 0:
     * baris yang terpusat bersinggungan dengan awal konten.
     */
    fun viewportCenterInContentCoords(
        viewportHeightPx: Int,
        beforeContentPaddingPx: Int,
    ): Int = viewportHeightPx / 2 - beforeContentPaddingPx

    /**
     * Offset scroll (px) untuk `LazyListState.animateScrollToItem`
     * agar item setinggi [itemHeightPx] berada DI TENGAH viewport
     * (v1.10.19, basis diperbaiki v1.10.28): `scrollToItem(index, s)`
     * menempatkan item pada offset konten −s, jadi s harus memenuhi
     * −s + [beforeContentPaddingPx] + setengah item = setengah viewport.
     * Nilai boleh negatif (item di bawah awal konten) — valid untuk
     * `scrollToItem`; pemanggil lirik selalu memakai padding
     * halfViewport sehingga hasilnya selalu setengah tinggi item (> 0).
     * Sengaja TANPA clamp: clamp 0 akan mengembalikan rumus lama
     * yang salah untuk konfigurasi padding kecil.
     */
    fun centerScrollOffset(
        viewportHeightPx: Int,
        itemHeightPx: Int,
        beforeContentPaddingPx: Int,
    ): Int =
        beforeContentPaddingPx + itemHeightPx / 2 - viewportHeightPx / 2

    /**
     * True bila item sudah berada di tengah viewport (toleransi
     * [tolerancePx]): pusat item ([itemOffsetPx] + setengah tinggi,
     * koordinat konten) berjarak ≤ toleransi dari pusat viewport
     * dalam koordinat konten. Dipakai agar follow tidak menggeser
     * daftar yang sudah terpusat (anti-jitter). Item yang tidak
     * terlihat → false (selalu digulir). Basis v1.10.28: tanpa
     * [beforeContentPaddingPx], pemeriksaan ini menerima baris di
     * tepi bawah sebagai "tengah" (cacat 10.19–10.27).
     */
    fun isNearCenter(
        itemOffsetPx: Int,
        itemSizePx: Int,
        viewportHeightPx: Int,
        tolerancePx: Int,
        beforeContentPaddingPx: Int,
    ): Boolean {
        val itemCenter = itemOffsetPx + itemSizePx / 2
        val viewportCenter =
            viewportCenterInContentCoords(viewportHeightPx, beforeContentPaddingPx)
        return kotlin.math.abs(itemCenter - viewportCenter) <= tolerancePx
    }

    /**
     * Jarak scroll (px, bertanda) agar item berada DI TENGAH viewport
     * (v1.10.22, basis diperbaiki v1.10.28): selisih pusat item
     * ([itemOffsetPx] + setengah tinggi, koordinat konten) terhadap
     * pusat viewport dalam koordinat konten. Positif = item di bawah
     * tengah → gulir maju sebesar itu (`animateScrollBy`); negatif =
     * sebaliknya; nol = sudah tepat di tengah. Dipakai saat item aktif
     * TERLIHAT (offset terukur); item tak terlihat memakai
     * [centerScrollOffset] + `animateScrollToItem`.
     */
    fun centerDistancePx(
        itemOffsetPx: Int,
        itemSizePx: Int,
        viewportHeightPx: Int,
        beforeContentPaddingPx: Int,
    ): Int = (itemOffsetPx + itemSizePx / 2) -
        viewportCenterInContentCoords(viewportHeightPx, beforeContentPaddingPx)

    /** Durasi gulir follow minimum (v1.10.22): perpindahan baris dekat. */
    const val FOLLOW_SCROLL_MIN_MS: Int = 300

    /** Durasi gulir follow maksimum (v1.10.22): lompatan jauh/seek. */
    const val FOLLOW_SCROLL_MAX_MS: Int = 600

    /**
     * Durasi tween gulir follow (v1.10.22): proporsional terhadap
     * [distancePx] (0,5 ms/px) dalam [FOLLOW_SCROLL_MIN_MS]…
     * [FOLLOW_SCROLL_MAX_MS] — perpindahan satu baris terasa tenang,
     * lompatan jauh tidak berlarut. Nilai pilihan builder; prototype
     * GETAR tidak mendefinisikan durasi apa pun.
     */
    fun followScrollDurationMs(distancePx: Int): Int =
        (FOLLOW_SCROLL_MIN_MS + kotlin.math.abs(distancePx) / 2)
            .coerceIn(FOLLOW_SCROLL_MIN_MS, FOLLOW_SCROLL_MAX_MS)

    /**
     * Koreksi pemusatan maksimum per target baris (v1.10.23): worker
     * follow closed-loop mengulangi gulir dari layout segar sampai
     * baris aktif di tengah, paling banyak sebanyak ini — satu
     * tembakan tanpa verifikasi (10.22) bisa meninggalkan baris di
     * luar tengah sampai pindah baris berikutnya.
     */
    const val FOLLOW_MAX_CORRECTIONS: Int = 2

    /**
     * Jeda settle layout (v1.10.23) sebelum verifikasi ulang
     * pemusatan: satu frame lebih agar layout pass setelah gulir
     * selesai terbaca dari layoutInfo.
     */
    const val FOLLOW_VERIFY_DELAY_MS: Long = 50L
}
