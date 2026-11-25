package com.sonara.app.data

/**
 * Opsi layout Beranda + Koleksi v1.0.7 — murni Kotlin.
 *
 * Layout mengatur presentasi RAK (poster rail / wide rail di
 * Beranda, seksi "Your playlists" di Koleksi); baris lagu, kartu
 * mix, dan Keep listening tidak berubah bentuk di semua mode.
 * Nilai tak dikenal di storage = EDITORIAL (default lama).
 */
enum class HomeLayout {
    EDITORIAL,
    COMPACT,
    GRID,
}

object LayoutPolicy {
    enum class RailMode { RAIL, LIST, GRID }

    fun storageKey(layout: HomeLayout): String = when (layout) {
        HomeLayout.EDITORIAL -> "editorial"
        HomeLayout.COMPACT -> "compact"
        HomeLayout.GRID -> "grid"
    }

    fun fromStorage(raw: String?): HomeLayout = when (raw?.trim()?.lowercase()) {
        "compact" -> HomeLayout.COMPACT
        "grid" -> HomeLayout.GRID
        else -> HomeLayout.EDITORIAL
    }

    /** Mode render rak untuk satu layout. */
    fun railMode(layout: HomeLayout): RailMode = when (layout) {
        HomeLayout.EDITORIAL -> RailMode.RAIL
        HomeLayout.COMPACT -> RailMode.LIST
        HomeLayout.GRID -> RailMode.GRID
    }

    fun displayName(layout: HomeLayout): String = when (layout) {
        HomeLayout.EDITORIAL -> "Editorial"
        HomeLayout.COMPACT -> "Compact"
        HomeLayout.GRID -> "Grid"
    }

    fun description(layout: HomeLayout): String = when (layout) {
        HomeLayout.EDITORIAL -> "Poster rails and wide cards, as before."
        HomeLayout.COMPACT -> "Shelves as dense song lists, no poster art."
        HomeLayout.GRID -> "Shelves as two-column artwork grids."
    }
}
