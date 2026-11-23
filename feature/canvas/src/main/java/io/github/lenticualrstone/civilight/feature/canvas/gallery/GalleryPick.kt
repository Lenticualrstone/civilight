package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.gallery.GalleryViewState

/**
 * The pick of one gallery screen (GAL-08): at most one item, changed only by an explicit tap or by arriving
 * with an item named (profile card, search result, closing the viewer). Scrolling never changes it.
 * [expanded] = `더 보기` open on the panel.
 */
data class PickState(val pickedKey: Key? = null, val expanded: Boolean = false) {
    companion object {
        fun of(saved: GalleryViewState): PickState = PickState(saved.pickedKey, saved.pickedKey != null && saved.panelExpanded)
    }
}

/** Pure reducer of [PickState] (T-GAL-01, T-GAL-02, T-GAL-34). */
object GalleryPick {
    /** Tap a card, tile or row: picks it; tapping the picked one again unpicks it and closes the panel. */
    fun tap(state: PickState, key: Key): PickState =
        if (state.pickedKey == key) PickState() else PickState(key, expanded = false)

    /** `⋯` on a card that is not picked opens its menu and picks it (GAL-08); on the picked card nothing changes. */
    fun menu(state: PickState, key: Key): PickState = if (state.pickedKey == key) state else PickState(key, expanded = false)

    /** Arriving with [key] named (route `pickKey`, closing the viewer): picks it when it is visible; else unchanged. */
    fun arrive(state: PickState, key: Key?, visible: Collection<Key>): PickState =
        if (key == null || key !in visible || key == state.pickedKey) state else PickState(key, expanded = false)

    /**
     * After the visible set changed (filter, relock, delete): a pick that is no longer visible is dropped and its
     * panel closes (GAL-08, GAL-19). Call only with a loaded document, never with the empty list of a loading screen.
     */
    fun reconcile(state: PickState, visible: Collection<Key>): PickState =
        if (state.pickedKey != null && state.pickedKey !in visible) PickState() else state

    /** `더 보기` · `접기`. */
    fun toggleExpanded(state: PickState): PickState = if (state.pickedKey == null) state else state.copy(expanded = !state.expanded)

    /** The saved form (`_app/view.json`, CNV-35), keeping the scroll fields of [saved]. */
    fun save(saved: GalleryViewState, state: PickState): GalleryViewState =
        saved.copy(pickedKey = state.pickedKey, panelExpanded = state.pickedKey != null && state.expanded)
}

/** The screen arrangement the gallery is drawn in (GAL-23, CNV-33). */
enum class GalleryLayout {
    /** Compact width. */
    COVER,

    /** Inner `목록 + 상세` with `목록` or `격자`: list left, picked item right (no inline panel). */
    INNER_SPLIT,

    /** Inner `가운데 한 칸`: the cover layout in the 560 column. */
    INNER_COLUMN,

    /** Inner `카드` (either setting): full width, two-column panel under the row. */
    INNER_CARDS,
    ;

    companion object {
        fun of(inner: Boolean, listDetail: Boolean, view: GalleryViewKind): GalleryLayout = when {
            !inner -> COVER
            view == GalleryViewKind.CARD -> INNER_CARDS
            listDetail -> INNER_SPLIT
            else -> INNER_COLUMN
        }
    }
}

/** Where the picked item's text appears (GAL-08 "Where the panel sits", GAL-23). */
enum class PanelPlace { UNDER_ROW, ROW_EXPANDS, RIGHT_PANE }

/** Where a hint shows while nothing is picked (GAL-08, T-GAL-39). */
enum class HintPlace { NONE, UNDER_ROW, ORDER_LINE, RIGHT_PANE }

/** What the right slot of the order line shows (GAL-04). */
sealed interface OrderSlot {
    data class Pager(val count: Int) : OrderSlot
    data class Readout(val n: Int) : OrderSlot
    data class Count(val text: String) : OrderSlot
    data class Hint(val text: String) : OrderSlot
    data object None : OrderSlot
}

/** Placement rules of panel, hint and order-line slot (pure, T-GAL-39). */
object GalleryPlacement {
    fun panel(view: GalleryViewKind, layout: GalleryLayout): PanelPlace = when {
        layout == GalleryLayout.INNER_SPLIT -> PanelPlace.RIGHT_PANE
        view == GalleryViewKind.LIST -> PanelPlace.ROW_EXPANDS
        else -> PanelPlace.UNDER_ROW
    }

    /** The hint `카드 누르기 · 설명 보기` only in `카드`; inner list + detail shows `이미지 누르기 · 설명 보기` on the right. */
    fun hint(view: GalleryViewKind, layout: GalleryLayout, picked: Boolean, grouped: Boolean, empty: Boolean): HintPlace = when {
        picked || empty -> HintPlace.NONE
        layout == GalleryLayout.INNER_SPLIT -> HintPlace.RIGHT_PANE
        view != GalleryViewKind.CARD -> HintPlace.NONE
        grouped -> HintPlace.ORDER_LINE
        else -> HintPlace.UNDER_ROW
    }

    fun orderSlot(view: GalleryView, viewKind: GalleryViewKind, hint: HintPlace): OrderSlot = when {
        hint == HintPlace.ORDER_LINE -> OrderSlot.Hint(GalleryUiCopy.HINT_CARD)
        viewKind != GalleryViewKind.CARD -> OrderSlot.Count(view.countLine)
        view.grouped -> OrderSlot.None
        view.count > GalleryRows.MAX_PAGER_MARKS -> OrderSlot.Readout(view.count)
        view.count > 1 -> OrderSlot.Pager(view.count)
        else -> OrderSlot.None
    }
}

/** One line of the `격자` column: a row of up to three tiles, the panel under the picked row, or the locked tile row. */
sealed interface GridLine {
    val key: String

    data class Tiles(val sectionId: String, val index: Int, val entries: List<GalleryEntry>, val lockTile: Boolean) : GridLine {
        override val key: String get() = "row-$sectionId-$index"
    }

    data class Panel(val pickedKey: Key) : GridLine {
        override val key: String get() = "panel"
    }
}

/** `격자` rows (GAL-06): three columns; the panel is inserted right after the row that holds the pick (T-GAL-05). */
object GalleryGrid {
    const val COLUMNS = 3

    fun lines(entries: List<GalleryEntry>, pickedKey: Key?, lockTile: Boolean, sectionId: String = "own", panel: Boolean = true): List<GridLine> {
        val out = ArrayList<GridLine>()
        val rows = entries.chunked(COLUMNS)
        rows.forEachIndexed { i, row ->
            val last = i == rows.lastIndex
            val withLock = lockTile && last && row.size < COLUMNS
            out += GridLine.Tiles(sectionId, i, row, withLock)
            if (panel && pickedKey != null && row.any { it.key == pickedKey }) out += GridLine.Panel(pickedKey)
        }
        if (lockTile && (rows.isEmpty() || rows.last().size == COLUMNS)) out += GridLine.Tiles(sectionId, rows.size, emptyList(), true)
        return out
    }
}

/** The card row's pick and scroll relation (GAL-07, GAL-08 `고른 카드로`, GAL-25 readout). */
object CardRowRules {
    /** `41 · 128`: the first visible card (1-based) and the count. */
    fun readout(firstVisibleIndex: Int, n: Int): String = GalleryUiCopy.readout((firstVisibleIndex + 1).coerceIn(1, maxOf(1, n)), n)

    /**
     * The panel arrow's centre x (dp from the row start), or null when the picked card is away (`고른 카드로`):
     * not laid out, or its centre outside the row. [cardStart] is
     * the card's left edge in the row viewport; the arrow stays inside the panel's rounded corners.
     */
    fun arrowX(cardStart: Float?, cardWidth: Float, rowWidth: Float, corner: Float = 24f): Float? {
        cardStart ?: return null
        val centre = cardStart + cardWidth / 2f
        if (centre < 0f || centre > rowWidth) return null
        return centre.coerceIn(corner, (rowWidth - corner).coerceAtLeast(corner))
    }
}

/** `이전` · `다음` of the viewer (GAL-09): through the visible items in the current order and filter, no wrap. */
data class ViewerNav(val keys: List<Key>, val current: Key) {
    val index: Int get() = keys.indexOf(current)
    val position: String get() = GalleryUiCopy.position(index + 1, keys.size)
    val hasPrev: Boolean get() = index > 0
    val hasNext: Boolean get() = index in 0 until keys.lastIndex

    fun prev(): Key = if (hasPrev) keys[index - 1] else current
    fun next(): Key = if (hasNext) keys[index + 1] else current

    companion object {
        /** The viewer's item after the list changed: kept while visible, else the nearest, else null (closes). */
        fun settle(keys: List<Key>, current: Key?, lastIndex: Int): Key? = when {
            current == null -> null
            current in keys -> current
            keys.isEmpty() -> null
            else -> keys[lastIndex.coerceIn(0, keys.lastIndex)]
        }
    }
}

/** Viewing-only zoom of the viewer (GAL-09): pinch 1…4, pan clamped so the image never leaves the screen. */
object ViewerZoom {
    const val MIN = 1f
    const val MAX = 4f

    fun scale(current: Float, factor: Float): Float = (current * factor).coerceIn(MIN, MAX)

    /** Pan limit on one axis for an image of [content] px scaled by [scale] in [viewport] px. */
    fun limit(content: Float, viewport: Float, scale: Float): Float = ((content * scale - viewport) / 2f).coerceAtLeast(0f)

    fun pan(offset: Float, delta: Float, content: Float, viewport: Float, scale: Float): Float {
        val l = limit(content, viewport, scale)
        return (offset + delta).coerceIn(-l, l)
    }
}

/** Facts shown about an original (GAL-09 inner column, GAL-21): `4.2 MB`, `11-20`. */
object GalleryFacts {
    /** `4.2 MB` from 1 MB, `820 KB` below (one decimal for MB, decimal units as the picker shows them). */
    fun size(bytes: Long): String = when {
        bytes >= 1_000_000L -> {
            val tenths = (bytes + 50_000L) / 100_000L
            "${tenths / 10}.${tenths % 10} MB"
        }
        else -> "${((bytes + 500L) / 1000L).coerceAtLeast(1L)} KB"
    }

    /** `MM-DD` of [at] (epoch ms) in [zone]. */
    fun monthDay(at: Long, zone: java.time.ZoneId): String {
        val d = java.time.Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        return "%02d-%02d".format(d.monthValue, d.dayOfMonth)
    }
}

/** Where an item sits in the gallery's LazyColumn (`목록` · `격자`), to scroll it into view (GAL-01, GAL-08). */
object GalleryColumn {
    /** Items before the sections: chips and order line, then the unlocked bar when shown. */
    fun leading(v: GalleryView): Int = 2 + (if (v.unlockedBar != null) 1 else 0)

    /**
     * The column index of [itemKey]'s row (`목록`) or of the grid line that holds it (`격자`); null when it is not
     * visible or the view is `카드` (cards scroll their own row) or the filter matches nothing.
     */
    fun indexOfItem(itemKey: Key, v: GalleryView, viewKind: GalleryViewKind, layout: GalleryLayout): Int? {
        if (viewKind == GalleryViewKind.CARD || v.filteredEmpty) return null
        var i = leading(v)
        val panel = GalleryPlacement.panel(viewKind, layout) == PanelPlace.UNDER_ROW
        for (section in v.sections) {
            if (section.header != null) i++
            val at = section.entries.indexOfFirst { it.key == itemKey }
            if (viewKind == GalleryViewKind.LIST) {
                if (at >= 0) return i + at
                i += section.entries.size
            } else {
                // The picked item is the one being scrolled to, so its panel line sits after its row.
                val lines = GalleryGrid.lines(section.entries, itemKey.takeIf { at >= 0 }, v.lockedRow != null && !v.grouped, section.id, panel)
                if (at >= 0) return i + lines.indexOfFirst { it is GridLine.Tiles && it.entries.any { e -> e.key == itemKey } }
                i += lines.size
            }
        }
        return null
    }
}

/** Bringing the picked item and its opened description into view (GAL-01, GAL-05, GAL-06). */
object GalleryReveal {
    /**
     * Pixels to scroll forward so an item spanning [top]..[bottom] ends above [viewEnd] (the end of the area the
     * bars leave clear), without moving its [top] above [viewStart]; 0 when it already fits. A span taller than
     * the clear area stops with its top at [viewStart].
     */
    fun delta(top: Int, bottom: Int, viewStart: Int, viewEnd: Int): Int {
        if (bottom <= viewEnd) return 0
        return minOf(bottom - viewEnd, maxOf(top - viewStart, 0))
    }

    /** A tap pick opens a description inside the column (`목록` row, `격자` panel line); cards and the right pane do not. */
    fun applies(view: GalleryViewKind, layout: GalleryLayout): Boolean =
        view != GalleryViewKind.CARD && GalleryPlacement.panel(view, layout) != PanelPlace.RIGHT_PANE

    /**
     * Arriving with a route item (search result, `보기` back): the picked item is scrolled to whenever it is
     * visible, also when it already was the saved pick (then the pick does not change, but the row starts at 0).
     */
    fun scrollsOnArrival(routeKey: Key?, visible: Collection<Key>): Boolean = routeKey != null && routeKey in visible
}

/**
 * The right pane of inner `목록 + 상세` (GAL-23): the card crop is drawn at 261 × 348 when the pane is tall enough,
 * else it shrinks (3:4 kept) so `설명 쓰기` · `보기` stay on screen without scrolling; a long description still scrolls.
 */
object GalleryDetailSizes {
    const val CARD_W = 261f
    const val CARD_H = 348f
    const val MIN_CARD_H = 240f

    /** Rows besides the card at one description line: position line 44, title 34, meta 22, description 24, buttons 48, five gaps of 10. */
    const val OTHER_ROWS = 222f

    /** Padding the pane adds above and below its content (dp), on top of the bars' padding. */
    const val TOP_GAP = 16f
    const val BOTTOM_GAP = 24f

    /** The card height (dp) in a pane [paneHeight] tall whose bars take [top] and [bottom]. */
    fun cardHeight(paneHeight: Float, top: Float, bottom: Float): Float =
        (paneHeight - top - TOP_GAP - bottom - BOTTOM_GAP - OTHER_ROWS).coerceIn(MIN_CARD_H, CARD_H)

    fun cardWidth(height: Float): Float = height * CARD_W / CARD_H
}
