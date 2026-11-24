package io.github.lenticualrstone.civilight.feature.library.shelf.views

import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/*
 * The pure parts of the six 서재 3D views (LIB-93, U-16) that every view shares: which items are composed, how
 * the scroll position settles, the picked item's buttons and the accessibility words. Each view's own board
 * geometry is in its file (`BookcaseView` … `StarsView`), ported from the U-16 generators.
 */

/** Behind and ahead of the item in front: `behind + 1 + ahead` items are composed. */
data class SceneReach(val behind: Int, val ahead: Int)

/** The picked item's own pair of text buttons (DS-70 c-class, LIB-93 `열기` and the view's pair). */
data class PickPair(val pick: String, val back: String)

/** Constellation clusters, in 형식 order; [OTHER] holds the lock item and `새 프로젝트`. */
enum class StarGroup { SERIAL, SHORT, SETTING_ONLY, OTHER }

object ShelfScene {
    /** Items composed at once (`ED`, 60 fps): 7 on the cover screen, 9 on the inner one. */
    const val COVER_CAP = 7
    const val WIDE_CAP = 9

    /** Every board's stage: 390 × 500 (board A `.stage`; boards B and C show the same band of their 844 board). */
    const val STAGE_W = 390f
    const val STAGE_H = 500f

    /** A fling adds velocity × this many seconds before the settle picks the nearest item. */
    const val FLING_SECONDS = 0.14f

    /** Above the stage: the section label and its gap (dp). */
    const val LABEL_H = 30f

    /** Under the stage: the board dock (caption row + one 48 dp button row + gaps), dp. */
    const val DOCK_H = 132f

    /** The smallest stage a short pane gets before the list scrolls instead (dp). */
    const val STAGE_MIN_H = 300f

    /**
     * The stage height in a pane [viewport] dp tall (under the header, above the command bar): the board's 500
     * when it fits with the label and the dock, else what is left (never under [STAGE_MIN_H]). The dock stays
     * on screen above the command bar (the inner display's list pane is about 700 dp tall).
     */
    fun stageHeight(viewport: Float?): Float {
        if (viewport == null || viewport <= 0f) return STAGE_H
        return (viewport - LABEL_H - DOCK_H).coerceIn(STAGE_MIN_H, STAGE_H)
    }

    /** The board's scale in a stage [stageH] dp tall: the whole 390 × 500 board, scaled about its top centre. */
    fun stageScale(stageH: Float): Float = (stageH / STAGE_H).coerceIn(STAGE_MIN_H / STAGE_H, 1f)

    fun cap(wide: Boolean): Int = if (wide) WIDE_CAP else COVER_CAP

    /** The styles that draw a 3D view (every [LibraryStyle] but `목록`). */
    val styles: List<LibraryStyle> = LibraryStyle.entries.filterNot { it.isList }

    /**
     * The view's own pair for the picked item, or null when the view has none (a tap only brings the item
     * forward). The boards' words: 책장 `꺼내기` · `제자리`, 원고 더미 `들어 보기` · `내려놓기`, 서랍
     * `꺼내기` · `돌려놓기`, 별자리 `앞으로` · `돌려놓기`.
     */
    fun pickPair(style: LibraryStyle): PickPair? = when (style) {
        LibraryStyle.BOOKSHELF -> PickPair(ShelfViewCopy.PULL_OUT, ShelfViewCopy.IN_PLACE)
        LibraryStyle.MANUSCRIPT_STACK -> PickPair(ShelfViewCopy.LIFT, ShelfViewCopy.PUT_DOWN)
        LibraryStyle.INDEX_DRAWER -> PickPair(ShelfViewCopy.PULL_OUT, ShelfViewCopy.PUT_BACK)
        LibraryStyle.CONSTELLATION -> PickPair(ShelfViewCopy.FORWARD, ShelfViewCopy.PUT_BACK)
        LibraryStyle.COVER_SPIN, LibraryStyle.ARCHIVE_HALL, LibraryStyle.LIST -> null
    }

    /**
     * Behind and ahead of the item in front. The corridor and the drawer barely show what is behind (a passed
     * bay is hidden, a passed card falls and fades), so they spend their slots ahead; the others are even.
     */
    fun reach(style: LibraryStyle, wide: Boolean): SceneReach = SceneReach(behind(style, wide), ahead(style, wide))

    /** [reach] without the allocation, for per-frame reads. */
    fun behind(style: LibraryStyle, wide: Boolean): Int = if (aheadOnly(style)) 1 else cap(wide) / 2

    fun ahead(style: LibraryStyle, wide: Boolean): Int = cap(wide) - 1 - behind(style, wide)

    private fun aheadOnly(style: LibraryStyle): Boolean =
        style == LibraryStyle.ARCHIVE_HALL || style == LibraryStyle.INDEX_DRAWER

    /** The item's name for accessibility: its title, the group's words for the lock item (never a hidden name). */
    fun itemName(item: ShelfCardItem): String = when (item) {
        is ShelfCardItem.Project -> item.card.row.title
        is ShelfCardItem.Lock -> item.group.title
        ShelfCardItem.NewProject -> ShelfCopy.NEW_PROJECT
    }

    /**
     * What a tap on the item does, for accessibility: in front, the view's pick word ([pair], `제자리` and the
     * like once [picked]) or `열기`, the lock's unlock words, `새 프로젝트`. Null away from the front (a tap
     * brings the item forward; no DS-70 word for that, so the default action is read).
     */
    fun tapLabel(item: ShelfCardItem, inFront: Boolean, pair: PickPair?, picked: Boolean): String? = when {
        !inFront -> null
        item is ShelfCardItem.Project -> when {
            pair == null -> ShelfViewCopy.OPEN
            picked -> pair.back
            else -> pair.pick
        }
        item is ShelfCardItem.Lock -> item.group.button
        else -> ShelfCopy.NEW_PROJECT
    }

    /** The composed index range: [cap] items around [focus], shifted to stay inside 0 until [count]. Empty for 0 items. */
    fun window(focus: Int, count: Int, cap: Int): IntRange = window(focus, count, SceneReach(cap / 2, cap - 1 - cap / 2))

    /** The composed index range for [reach], shifted to stay inside 0 until [count]. Empty for 0 items. */
    fun window(focus: Int, count: Int, reach: SceneReach): IntRange {
        val cap = reach.behind + 1 + reach.ahead
        if (count <= 0 || cap <= 0) return IntRange.EMPTY
        if (count <= cap) return 0 until count
        val f = focus.coerceIn(0, count - 1)
        val start = (f - reach.behind).coerceIn(0, count - cap)
        return start until start + cap
    }

    fun clampIndex(index: Int, count: Int): Int = if (count <= 0) 0 else index.coerceIn(0, count - 1)

    /** The scroll position rounded to the item in front. */
    fun nearest(position: Float, count: Int): Int = clampIndex(position.roundToInt(), count)

    /** Where a drag released at [position] with [velocityItems] (items per second) settles. */
    fun settle(position: Float, velocityItems: Float, count: Int): Int = nearest(position + velocityItems * FLING_SECONDS, count)

    /** The drag range: the ends give a little (a third of an item) before the settle brings them back. */
    fun dragRange(count: Int): ClosedFloatingPointRange<Float> = -0.35f..(max(count - 1, 0) + 0.35f)

    /** The index of [key] in [keys], or [fallback] clamped when it is not there (a re-lock, a removed project). */
    fun indexOf(keys: List<Any>, key: Any?, fallback: Int): Int {
        val i = if (key == null) -1 else keys.indexOf(key)
        return if (i >= 0) i else clampIndex(fallback, keys.size)
    }

    fun starGroup(item: ShelfCardItem): StarGroup = when (item) {
        // A project whose document is still loading sits with the model default (연재 장편).
        is ShelfCardItem.Project -> when (item.card.row.format ?: ProjectFormat.SERIAL) {
            ProjectFormat.SERIAL -> StarGroup.SERIAL
            ProjectFormat.SHORT -> StarGroup.SHORT
            ProjectFormat.SETTING_ONLY -> StarGroup.SETTING_ONLY
        }
        is ShelfCardItem.Lock, ShelfCardItem.NewProject -> StarGroup.OTHER
    }

    /** 별자리 cluster label: the 형식 word, none for [StarGroup.OTHER]. */
    fun groupLabel(group: StarGroup): String? = when (group) {
        StarGroup.SERIAL -> ShelfViewCopy.FORMAT_SERIAL
        StarGroup.SHORT -> ShelfViewCopy.FORMAT_SHORT
        StarGroup.SETTING_ONLY -> ShelfViewCopy.FORMAT_SETTING
        StarGroup.OTHER -> null
    }
}
