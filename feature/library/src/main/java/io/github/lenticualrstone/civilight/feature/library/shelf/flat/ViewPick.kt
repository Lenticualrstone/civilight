package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import io.github.lenticualrstone.civilight.feature.library.settings.ViewStyles
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.AtelierCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.AtelierRegistry
import io.github.lenticualrstone.civilight.model.app.LibraryStyle

/** The picker's two Atelier groups: 공간 · 3D (5) and 모션 · 작업 (5). */
enum class PickGroup { SPATIAL, KINETIC }

/** One style in the view picker: its name, its fragment, its group and its glyph (24-grid path). */
data class PickEntry(val style: LibraryStyle, val label: String, val frag: String, val group: PickGroup, val glyph: String)

/**
 * Pure logic of the Atelier 10-view picker: lists the 5 spatial 3D views and 5 kinetic workspace
 * views, handles ring navigation and swipe settle.
 */
object ViewPick {
    fun frag(style: LibraryStyle): String = AtelierCopy.frag(style)

    fun glyph(style: LibraryStyle): String = AtelierRegistry.glyph(style)

    fun group(style: LibraryStyle): PickGroup = if (style.isSpatial) PickGroup.SPATIAL else PickGroup.KINETIC

    fun groupLabel(group: PickGroup): String = when (group) {
        PickGroup.SPATIAL -> AtelierCopy.GROUP_SPATIAL
        PickGroup.KINETIC -> AtelierCopy.GROUP_KINETIC
    }

    /** The grid's side label (`3D` · `작업`). */
    fun groupShort(group: PickGroup): String = when (group) {
        PickGroup.SPATIAL -> AtelierCopy.GROUP_SPATIAL_SHORT
        PickGroup.KINETIC -> AtelierCopy.GROUP_KINETIC_SHORT
    }

    /** The listed 10 styles in order: 5 spatial 3D views + 5 kinetic workspace views. */
    fun entries(available: Set<LibraryStyle> = ShelfViewRenderers.available): List<PickEntry> =
        AtelierRegistry.allStyles
            .filter { it in available || it.isList }
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
