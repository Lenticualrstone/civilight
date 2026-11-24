package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import io.github.lenticualrstone.civilight.feature.library.settings.ViewStyles
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ShelfScene
import io.github.lenticualrstone.civilight.model.app.LibraryStyle

/** The picker's three groups (r3 `GROUPS`): 새 보기 · 3D · 기본. */
enum class PickGroup { NEW, THREE_D, BASIC }

/** One style in the view picker: its name, its fragment, its group and its glyph (24-grid path). */
data class PickEntry(val style: LibraryStyle, val label: String, val frag: String, val group: PickGroup, val glyph: String)

/**
 * (0.2.3, r3 page `viewpick`, `scr_v_preview.py`) The view picker's pure parts: which styles it lists (only those
 * with a renderer, plus `목록`; the board's `격자` has no 서재 view and is not listed), how the pick moves, and
 * when `이 보기로` does something.
 */
object ViewPick {
    fun frag(style: LibraryStyle): String = when (style) {
        LibraryStyle.TIMELINE -> FlatViewCopy.FRAG_TIMELINE
        LibraryStyle.MANUSCRIPT_GRID -> FlatViewCopy.FRAG_PAPER
        LibraryStyle.MAILBOX -> FlatViewCopy.FRAG_MAIL
        LibraryStyle.PROGRESS_CELLS -> FlatViewCopy.FRAG_CELLS
        LibraryStyle.COVER_WALL -> FlatViewCopy.FRAG_WALL
        LibraryStyle.POSTER -> FlatViewCopy.FRAG_POSTER
        LibraryStyle.TABLE -> FlatViewCopy.FRAG_TABLE
        LibraryStyle.BOOKSHELF -> FlatViewCopy.FRAG_SHELF
        LibraryStyle.COVER_SPIN -> FlatViewCopy.FRAG_FLOW
        LibraryStyle.MANUSCRIPT_STACK -> FlatViewCopy.FRAG_DESK
        LibraryStyle.ARCHIVE_HALL -> FlatViewCopy.FRAG_HALL
        LibraryStyle.INDEX_DRAWER -> FlatViewCopy.FRAG_DRAWER
        LibraryStyle.CONSTELLATION -> FlatViewCopy.FRAG_STARS
        LibraryStyle.LIST -> FlatViewCopy.FRAG_LIST
    }

    /** Board glyphs (`scr_v_lib.I`, `scr_v_fold3d.LIB_ICO`), one stroke path on a 24 grid. */
    fun glyph(style: LibraryStyle): String = when (style) {
        LibraryStyle.TIMELINE -> "M3 12h18M6 12V7M11 12v5M15 12V8M19 12v4"
        LibraryStyle.MANUSCRIPT_GRID -> "M5 4h14v16H5zM5 9h14M5 14h14M10 4v16M14.5 4v16"
        LibraryStyle.MAILBOX -> "M3.5 6h17v12h-17zM3.5 6l8.5 7 8.5-7"
        LibraryStyle.PROGRESS_CELLS -> "M4 5h4v4H4zM10 5h4v4h-4zM16 5h4v4h-4zM4 11h4v4H4zM10 11h4v4h-4zM4 17h4v2H4z"
        LibraryStyle.COVER_WALL -> "M4 4h7v9H4zM13 4h7v5h-7zM13 11h7v9h-7zM4 15h7v5H4z"
        LibraryStyle.POSTER -> "M6 3h12v18H6zM9 15l3-4 3 4"
        LibraryStyle.TABLE -> "M3.5 5h17v14h-17zM3.5 10h17M3.5 14.5h17M9 5v14"
        LibraryStyle.BOOKSHELF -> "M5 4v15M9.5 4v15M14 6.5l3.2 12.5M3 20h18"
        LibraryStyle.COVER_SPIN -> "M9 5h6v14H9zM3.5 7.5l3 1v7l-3 1zM20.5 7.5l-3 1v7l3 1z"
        LibraryStyle.MANUSCRIPT_STACK -> "M8 3.5h11v14H8zM5 6.5v14h11"
        LibraryStyle.ARCHIVE_HALL -> "M3 3l6 5v8l-6 5M21 3l-6 5v8l6 5M9 8h6M9 16h6"
        LibraryStyle.INDEX_DRAWER -> "M4 4h16v7H4zM4 11h16v9H4zM10 7.5h4M10 15h4"
        LibraryStyle.CONSTELLATION -> "M4 16l5-7 6 4 5-8M12 4l.6 1.4L14 6l-1.4.6L12 8l-.6-1.4L10 6l1.4-.6z"
        LibraryStyle.LIST -> "M4 6h16M4 12h16M4 18h16"
    }

    fun group(style: LibraryStyle): PickGroup = when {
        FlatViews.isFlat(style) -> PickGroup.NEW
        style.isList -> PickGroup.BASIC
        else -> PickGroup.THREE_D
    }

    fun groupLabel(group: PickGroup): String = when (group) {
        PickGroup.NEW -> FlatViewCopy.GROUP_NEW
        PickGroup.THREE_D -> FlatViewCopy.GROUP_3D
        PickGroup.BASIC -> FlatViewCopy.GROUP_BASIC
    }

    /** The grid's side label (`새` · `3D` · `기본`). */
    fun groupShort(group: PickGroup): String = if (group == PickGroup.NEW) FlatViewCopy.GROUP_NEW_SHORT else groupLabel(group)

    /** The listed styles in order: the new views, the 3D views, `목록` (only styles that draw something). */
    fun entries(available: Set<LibraryStyle> = ShelfViewRenderers.available): List<PickEntry> =
        (FlatViews.styles.filter { it in available } + ShelfScene.styles.filter { it in available } + LibraryStyle.LIST)
            .map { PickEntry(it, ViewStyles.label(it), frag(it), group(it), glyph(it)) }

    /** The entry that is on now: the stored style when it is listed, else `목록` (what the 서재 then draws). */
    fun currentIndex(entries: List<PickEntry>, stored: LibraryStyle): Int =
        entries.indexOfFirst { it.style == stored }.takeIf { it >= 0 } ?: entries.indexOfFirst { it.style.isList }.coerceAtLeast(0)

    /** ◀ ▶ and the swipe go round. */
    fun wrap(i: Int, n: Int): Int = if (n <= 0) 0 else ((i % n) + n) % n

    /** The signed shortest distance from [sel] to [i] on the ring (board `d`). */
    fun distance(i: Int, sel: Int, n: Int): Int {
        var d = i - sel
        if (d > n / 2) d -= n
        if (d < -n / 2) d += n
        return d
    }

    /** `이 보기로` applies only a different style (the same one would change nothing). */
    fun canApply(sel: Int, cur: Int): Boolean = sel != cur

    /** A released drag of [dx] dp moves the pick when it passed [SWIPE] (board: 56 px). */
    fun afterSwipe(sel: Int, dx: Float, n: Int): Int = when {
        dx < -SWIPE -> wrap(sel + 1, n)
        dx > SWIPE -> wrap(sel - 1, n)
        else -> sel
    }

    const val SWIPE = 56f

    /** The stage's two neighbours are drawn at this share of the picked style's scale (board `C_S2`). */
    const val SIDE_SCALE = .78f

    /** The cover preview's native size (dp; the Fold 8 cover board's 475 × 751 ratio). */
    const val COVER_W = 380f
    const val COVER_H = 601f
}
