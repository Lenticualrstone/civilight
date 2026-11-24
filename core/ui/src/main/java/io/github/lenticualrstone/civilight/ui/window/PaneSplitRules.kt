package io.github.lenticualrstone.civilight.ui.window

import io.github.lenticualrstone.civilight.model.app.PaneSplit

/**
 * CNV-47 펼친 화면 비율 as plain numbers (dp). A two-pane frame is `left | 1 dp divider | right`; the
 * stored [PaneSplit] gives the left pane's share of the two panes' width, `DEFAULT` the module's own left
 * width (r3 list 300 · 320, script panel its own). JVM-tested; the composables in `PaneSplitFrame.kt` only call these.
 */
object PaneSplitRules {
    /** Each pane keeps at least this width (CNV-47). */
    const val MIN_PANE_DP: Float = 280f

    /** The `line` divider between the panes. */
    const val DIVIDER_DP: Float = 1f

    /** Width the two panes share in a frame [totalDp] wide. */
    fun panesWidth(totalDp: Float): Float = (totalDp - DIVIDER_DP).coerceAtLeast(0f)

    /**
     * Whether the frame can show the handle: an inner screen ([inner]) whose two panes both fit their
     * minimum. Otherwise the module keeps today's layout and shows no handle.
     */
    fun canResize(totalDp: Float, inner: Boolean): Boolean = inner && panesWidth(totalDp) >= 2 * MIN_PANE_DP

    /** A left width kept between the two minimums (only meaningful when [canResize]). */
    fun clampLeft(leftDp: Float, totalDp: Float): Float {
        val panes = panesWidth(totalDp)
        if (panes < 2 * MIN_PANE_DP) return leftDp.coerceIn(0f, panes)
        return leftDp.coerceIn(MIN_PANE_DP, panes - MIN_PANE_DP)
    }

    /**
     * The left pane's width for [split] in a frame [totalDp] wide. `DEFAULT`, and any split when the frame
     * cannot be resized, give [defaultLeftDp] unchanged (today's layout). A chosen ratio that would push a
     * pane under the minimum is clamped for this window; the stored choice is kept.
     */
    fun leftWidth(totalDp: Float, split: PaneSplit, defaultLeftDp: Float, inner: Boolean = true): Float {
        val fraction = split.listFraction
        if (fraction == null || !canResize(totalDp, inner)) return defaultLeftDp
        return clampLeft(panesWidth(totalDp) * fraction, totalDp)
    }

    /** The left pane's share for a dragged [leftDp]. */
    fun fractionOf(leftDp: Float, totalDp: Float): Float {
        val panes = panesWidth(totalDp)
        return if (panes <= 0f) 0.5f else (leftDp / panes).coerceIn(0f, 1f)
    }

    /** Where a released drag settles: the nearest of `1:2 · 1:1 · 2:1`. */
    fun settle(leftDp: Float, totalDp: Float): PaneSplit = PaneSplit.nearest(fractionOf(leftDp, totalDp))

    /**
     * (0.2.3) Where a released drag settles when the module's own width [defaultLeftDp] is a target too: `기본`
     * when the drag ends nearer to it than to any of `1:2 · 1:1 · 2:1`, so every choice stays reachable by touch.
     */
    fun settle(leftDp: Float, totalDp: Float, defaultLeftDp: Float): PaneSplit {
        val snap = settle(leftDp, totalDp)
        val snapLeft = leftWidth(totalDp, snap, defaultLeftDp)
        return if (kotlin.math.abs(leftDp - defaultLeftDp) < kotlin.math.abs(leftDp - snapLeft)) PaneSplit.DEFAULT else snap
    }

    /**
     * The default left width of `목록 + 상세` (r3): 300 upright (`Two*`), 320 on its side (`L*`), never more
     * than half of the frame (Q-LY-01).
     */
    fun listDetailDefaultLeft(totalDp: Float, landscape: Boolean = false): Float {
        val pane = if (landscape) WindowLayout.LIST_PANE_LANDSCAPE_DP else WindowLayout.LIST_PANE_PORTRAIT_DP
        return minOf(pane.toFloat(), totalDp / 2f)
    }

    /** Popover buttons in on-screen order (`1:2 · 1:1 · 2:1 · 기본`). */
    val CHOICES: List<PaneSplit> = PaneSplit.SNAP + PaneSplit.DEFAULT

    /** The button label of [split]. */
    fun label(split: PaneSplit): String = when (split) {
        PaneSplit.ONE_TWO -> PaneSplitCopy.ONE_TWO
        PaneSplit.ONE_ONE -> PaneSplitCopy.ONE_ONE
        PaneSplit.TWO_ONE -> PaneSplitCopy.TWO_ONE
        PaneSplit.DEFAULT -> PaneSplitCopy.DEFAULT
    }
}

/** Copy of the CNV-47 handle and popover (spec 50 §E). */
object PaneSplitCopy {
    /** Content description of the handle (`[PROPOSED]` in CNV-47). */
    const val HANDLE = "화면 비율"
    const val ONE_TWO = "1:2"
    const val ONE_ONE = "1:1"
    const val TWO_ONE = "2:1"
    const val DEFAULT = "기본"
}
