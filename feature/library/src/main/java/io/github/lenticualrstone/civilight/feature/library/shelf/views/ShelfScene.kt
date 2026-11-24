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

/** (0.2.2) Where the stage sits on the full-screen scene, dp from the content box's top. */
data class StageFit(val top: Float, val height: Float, val scale: Float, val fadeTop: Float, val fadeBottom: Float) {
    val bottom: Float get() = top + height
}

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

    /** Under the stage in 0.2.1: the board dock's height (dp); 원고 더미 still runs its wall this far past the stage. */
    const val DOCK_H = 132f

    /** The smallest stage (dp): a shorter pane runs the stage on under the glass dock instead of shrinking it further. */
    const val STAGE_MIN_H = 300f

    /** (0.2.2) The stage may reach this far up under the top bar's faded foot (dp). */
    const val HEAD_OVERLAP = 20f

    /** (0.2.2) The stage may reach this far down under the glass dock's top edge (dp). */
    const val DOCK_OVERLAP = 12f

    /** (0.2.2) The gap between the glass dock and the bottom bar (the search field), dp. */
    const val DOCK_GAP = 8f

    /** (0.2.2) The fade bands that melt the stage into the screen's `bg` (no hard top or bottom line), dp. */
    const val FADE_TOP = 36f
    const val FADE_BOTTOM = 48f

    /**
     * (0.2.2) The stage on the full-screen scene: the board's 390 × 500 between the top bar and the glass dock
     * ([dockH] = the dock with its gap), a little under each so its faded edges meet the bars' translucent edges.
     * The board keeps 500 when it fits and is centred in the room; a shorter room scales it down to [STAGE_MIN_H],
     * and below that the stage keeps [STAGE_MIN_H] from the top and runs on under the dock (glass over the scene).
     * All values in dp of the content box ([screenH] tall, bars as its [barTop] / [barBottom] padding); the scene
     * is the viewport itself, never taller, so these are on-screen positions.
     */
    fun stageFit(screenH: Float, barTop: Float, barBottom: Float, dockH: Float): StageFit {
        val from = (barTop - HEAD_OVERLAP).coerceAtLeast(0f)
        val to = screenH - barBottom - dockH + DOCK_OVERLAP
        val room = to - from
        val h = room.coerceIn(STAGE_MIN_H, STAGE_H)
        val top = from + ((room - h) / 2f).coerceAtLeast(0f)
        return StageFit(top = top, height = h, scale = stageScale(h), fadeTop = fadeTop(h), fadeBottom = fadeBottom(h))
    }

    /**
     * (0.2.2) The tallest the glass dock may be in a content box [viewport] dp tall: the room between the top
     * bar ([barTop]) and the bottom bar ([barBottom]) less [DOCK_GAP]. The scene never scrolls (the glass bars
     * float over the viewport, so a scrolled scene only hides the dock): the dock stays pinned above the search
     * field and below the header, and a taller dock (a landscape phone, a large font) scrolls its own caption
     * while its buttons and the peek row stay in view. The stage gets what is left and runs on under the glass.
     */
    fun dockMaxH(viewport: Float, barTop: Float, barBottom: Float): Float =
        (viewport - barTop - barBottom - DOCK_GAP).coerceAtLeast(0f)

    /** (0.2.2) The dock's top edge on screen (dp) for a dock [dockH] tall (its gap included), capped by [dockMaxH]. */
    fun dockTop(viewport: Float, barTop: Float, barBottom: Float, dockH: Float): Float =
        viewport - barBottom - min(dockH, dockMaxH(viewport, barTop, barBottom) + DOCK_GAP)

    /** The top fade band of a stage [stageH] dp tall: [FADE_TOP], never more than a quarter of the stage. */
    fun fadeTop(stageH: Float): Float = min(FADE_TOP, stageH / 4f).coerceAtLeast(0f)

    /** The bottom fade band of a stage [stageH] dp tall: [FADE_BOTTOM], never more than a quarter of the stage. */
    fun fadeBottom(stageH: Float): Float = min(FADE_BOTTOM, stageH / 4f).coerceAtLeast(0f)

    /** (0.2.3) The side fade band (dp) of 원고 더미 and 서고 회랑, never more than a sixth of the width. */
    const val SIDE_FADE = 28f

    fun sideFade(style: LibraryStyle, widthDp: Float): Float =
        if (style == LibraryStyle.MANUSCRIPT_STACK || style == LibraryStyle.ARCHIVE_HALL) min(SIDE_FADE, widthDp / 6f).coerceAtLeast(0f) else 0f

    /**
     * (0.2.3) The frame's height in board units for a stage box [runDp] tall drawn at [scale]: the board's 500, or
     * more where the box runs on under the dock and the search field (the views draw their backdrop to its foot).
     */
    fun frameH(runDp: Float, scale: Float): Float = max(STAGE_H, runDp / scale.coerceAtLeast(0.01f))

    /** (0.2.3) The run of the stage box: from the fit's top to the viewport's foot (never shorter than the fit). */
    fun runHeight(screenH: Float, fit: StageFit): Float = max(fit.height, screenH - fit.top)

    /** The board's scale in a stage [stageH] dp tall: the whole 390 × 500 board, scaled about its top centre. */
    fun stageScale(stageH: Float): Float = (stageH / STAGE_H).coerceIn(STAGE_MIN_H / STAGE_H, 1f)

    fun cap(wide: Boolean): Int = if (wide) WIDE_CAP else COVER_CAP

    /**
     * The styles that draw a 3D view (the six U-16 boards). Explicit: the flat r3 views added in 0.2.3
     * (`FlatViews`) are not 3D scenes and must never reach [ShelfScene3D].
     */
    val styles: List<LibraryStyle> = listOf(
        LibraryStyle.BOOKSHELF,
        LibraryStyle.COVER_SPIN,
        LibraryStyle.MANUSCRIPT_STACK,
        LibraryStyle.ARCHIVE_HALL,
        LibraryStyle.INDEX_DRAWER,
        LibraryStyle.CONSTELLATION,
    )

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
        else -> null
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
