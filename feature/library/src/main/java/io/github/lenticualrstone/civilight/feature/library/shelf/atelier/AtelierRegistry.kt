package io.github.lenticualrstone.civilight.feature.library.shelf.atelier

import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderer
import io.github.lenticualrstone.civilight.model.app.LibraryStyle

/**
 * Single registry for all 7 handcrafted Atelier library views:
 * 2 hyper-detailed 3D physical object views (CD Jewel Case, 3.5" Floppy + CRT)
 * + 5 tactile kinetic workspace views.
 */
object AtelierRegistry {
    val spatialStyles: List<LibraryStyle> = listOf(
        LibraryStyle.CD_JEWEL_CASE,
        LibraryStyle.FLOPPY_CRT,
    )

    val kineticStyles: List<LibraryStyle> = listOf(
        LibraryStyle.KINETIC_DECK,
        LibraryStyle.BENTO_MOSAIC,
        LibraryStyle.FILM_REEL,
        LibraryStyle.PULSE_TIMELINE,
        LibraryStyle.LIST,
    )

    val allStyles: List<LibraryStyle> = spatialStyles + kineticStyles

    val renderers: Map<LibraryStyle, ShelfViewRenderer> = mapOf(
        LibraryStyle.CD_JEWEL_CASE to { input: ShelfViewInput -> CdJewelCaseView(input) },
        LibraryStyle.FLOPPY_CRT to { input: ShelfViewInput -> FloppyCrtView(input) },
        LibraryStyle.KINETIC_DECK to { input: ShelfViewInput -> KineticDeckView(input) },
        LibraryStyle.BENTO_MOSAIC to { input: ShelfViewInput -> BentoMosaicView(input) },
        LibraryStyle.FILM_REEL to { input: ShelfViewInput -> FilmReelView(input) },
        LibraryStyle.PULSE_TIMELINE to { input: ShelfViewInput -> PulseTimelineView(input) },
        LibraryStyle.LIST to { input: ShelfViewInput -> StudioListView(input) },
    )

    /** 24x24 outline stroke SVG path for each Atelier view. */
    fun glyph(style: LibraryStyle): String = when (style) {
        // Jewel case + disc peeking on the right
        LibraryStyle.CD_JEWEL_CASE -> "M3 5h12v14H3zM6 5v14M15 7a5 5 0 1 1 0 10M15 10.5a1.5 1.5 0 1 1 0 3"
        // CRT monitor + 3.5" floppy slot
        LibraryStyle.FLOPPY_CRT -> "M3 4h18v12H3zM6 7h12v6H6zM7 19h10M12 16v3"
        LibraryStyle.KINETIC_DECK -> "M7 6h10v13H7zM4 8.5l3-1v10l-3-1zM20 8.5l-3-1v10l3-1z"
        LibraryStyle.BENTO_MOSAIC -> "M4 4h16v6H4zM4 12h7v8H4zM13 12h7v8h-7z"
        LibraryStyle.FILM_REEL -> "M4 5h16v14H4zM4 8h16M4 16h16M9 8v8M15 8v8"
        LibraryStyle.PULSE_TIMELINE -> "M6 4v16M6 8h14M6 16h14M4 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"
        LibraryStyle.LIST -> "M4 6h16M4 12h16M4 18h16"
    }
}
