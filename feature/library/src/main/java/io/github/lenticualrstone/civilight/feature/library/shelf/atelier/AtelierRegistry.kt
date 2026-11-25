package io.github.lenticualrstone.civilight.feature.library.shelf.atelier

import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderer
import io.github.lenticualrstone.civilight.model.app.LibraryStyle

/**
 * Single registry for all 10 handcrafted Atelier library views:
 * 5 Spatial 3D views + 5 Kinetic workspace views.
 * Replaces all legacy 0.2.x 3D and flat shelf views.
 */
object AtelierRegistry {
    val spatialStyles: List<LibraryStyle> = listOf(
        LibraryStyle.GLASS_PRISM,
        LibraryStyle.ARCHIVE_VAULT,
        LibraryStyle.BOOK_SPINE_3D,
        LibraryStyle.ORBIT_STAGE,
        LibraryStyle.FOLIO_SPREAD,
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
        LibraryStyle.GLASS_PRISM to { input: ShelfViewInput -> GlassPrismView(input) },
        LibraryStyle.ARCHIVE_VAULT to { input: ShelfViewInput -> ArchiveVaultView(input) },
        LibraryStyle.BOOK_SPINE_3D to { input: ShelfViewInput -> BookSpine3dView(input) },
        LibraryStyle.ORBIT_STAGE to { input: ShelfViewInput -> OrbitStageView(input) },
        LibraryStyle.FOLIO_SPREAD to { input: ShelfViewInput -> FolioSpreadView(input) },
        LibraryStyle.KINETIC_DECK to { input: ShelfViewInput -> KineticDeckView(input) },
        LibraryStyle.BENTO_MOSAIC to { input: ShelfViewInput -> BentoMosaicView(input) },
        LibraryStyle.FILM_REEL to { input: ShelfViewInput -> FilmReelView(input) },
        LibraryStyle.PULSE_TIMELINE to { input: ShelfViewInput -> PulseTimelineView(input) },
        LibraryStyle.LIST to { input: ShelfViewInput -> StudioListView(input) },
    )

    /** 24x24 outline stroke SVG path for each Atelier view. */
    fun glyph(style: LibraryStyle): String = when (style) {
        LibraryStyle.GLASS_PRISM -> "M12 3l8 4.5v9L12 21l-8-4.5v-9zM12 3v18M4 7.5l8 4.5 8-4.5"
        LibraryStyle.ARCHIVE_VAULT -> "M3 4l5 4v8l-5 4M21 4l-5 4v8l5 4M8 8h8v8H8z"
        LibraryStyle.BOOK_SPINE_3D -> "M5 4v16M9.5 4v16M14 5.5l4.5 14.5M3 20h18"
        LibraryStyle.ORBIT_STAGE -> "M12 12m-9 0a9 4.5 0 1 0 18 0a9 4.5 0 1 0 -18 0M12 7v10"
        LibraryStyle.FOLIO_SPREAD -> "M4 5h7a2 2 0 0 1 2 2v12a2 2 0 0 0-2-2H4zM20 5h-7a2 2 0 0 0-2 2v12a2 2 0 0 1 2-2h7z"
        LibraryStyle.KINETIC_DECK -> "M7 6h10v13H7zM4 8.5l3-1v10l-3-1zM20 8.5l-3-1v10l3-1z"
        LibraryStyle.BENTO_MOSAIC -> "M4 4h16v6H4zM4 12h7v8H4zM13 12h7v8h-7z"
        LibraryStyle.FILM_REEL -> "M4 5h16v14H4zM4 8h16M4 16h16M9 8v8M15 8v8"
        LibraryStyle.PULSE_TIMELINE -> "M6 4v16M6 8h14M6 16h14M4 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"
        LibraryStyle.LIST -> "M4 6h16M4 12h16M4 18h16"
    }
}
