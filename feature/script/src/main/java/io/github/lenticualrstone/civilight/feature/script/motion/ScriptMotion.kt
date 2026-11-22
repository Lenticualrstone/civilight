package io.github.lenticualrstone.civilight.feature.script.motion

import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken

/**
 * The script's motion table (SCR-67, boards `MType`, `MSwipe`, `MotionSpec`), as plain values the
 * composables read:
 * - new block: from 8 dp below, 320 ms decelerate (CNV-39); the blocks below slide down with it;
 * - swipe row: the block slides 260 ms, the three buttons fade in from 16 dp to the right, 60 ms
 *   apart (CNV-40);
 * - delete (and merge, move away): 160 ms accelerate; the rows below close the gap on the same curve;
 * - tab or value change: 200 ms standard;
 * - never moves: the line being typed (its row has no placement motion), the caret, glass bars;
 * - reduced motion: no movement, every change a 120 ms fade (or nothing).
 */
object ScriptMotion {
    /** A new block's start offset below its place (CNV-39). */
    const val ENTER_SHIFT_DP: Int = 8

    /** A swipe button's start offset to the right (CNV-40). */
    const val BUTTON_SHIFT_DP: Int = 16

    /** Buttons of the swipe row (`시간 · 참고 · 속성`). */
    const val SWIPE_BUTTONS: Int = 3

    val swipeMs: Int get() = CivilightMotion.swipeMs
    val staggerMs: Int get() = CivilightMotion.swipeStaggerMs

    /** One button's own run: the row's 260 ms less the stagger of the buttons after the first. */
    val buttonRunMs: Int get() = swipeMs - staggerMs * (SWIPE_BUTTONS - 1)

    /**
     * Progress (0…1) of swipe button [index] when the row is [rowProgress] of the way open (0 = at
     * rest, 1 = −192 dp). Button i starts 60 ms × i into the 260 ms slide and ends with it, so the
     * same mapping works for the finger and for the settle. Reduced motion: all buttons fade with
     * the row, no stagger.
     */
    fun buttonProgress(rowProgress: Float, index: Int, reduced: Boolean): Float {
        val p = rowProgress.coerceIn(0f, 1f)
        if (reduced) return p
        val t = p * swipeMs
        return ((t - staggerMs * index) / buttonRunMs).coerceIn(0f, 1f)
    }

    /** Horizontal offset (dp) of a swipe button at [progress]; none with reduced motion. */
    fun buttonShiftDp(progress: Float, reduced: Boolean): Float =
        if (reduced) 0f else (1f - progress.coerceIn(0f, 1f)) * BUTTON_SHIFT_DP

    /** How a row's position change animates, or null for none. */
    enum class Placement { ENTER, EXIT }

    /**
     * Placement motion of a manuscript row: never for the row being typed (the typed line never
     * moves) or with reduced motion; while a new block enters, the rows around it follow its 320 ms
     * decelerate. Outside typing, a delete, merge or move closing a gap uses the 160 ms accelerate.
     * While typing, nothing else moves: the typed block growing a line pushes the rows below at once,
     * as the writing line scrolls (SCR-67 animates add and delete only).
     */
    fun placement(isCaretRow: Boolean, entering: Boolean, reduced: Boolean, editing: Boolean = false): Placement? = when {
        reduced || isCaretRow -> null
        entering -> Placement.ENTER
        editing -> null
        else -> Placement.EXIT
    }

    fun token(placement: Placement): MotionToken = when (placement) {
        Placement.ENTER -> CivilightMotion.enter
        Placement.EXIT -> CivilightMotion.exit
    }

    /** A removed row fades out over 160 ms accelerate; reduced motion 120 ms. */
    fun removeMs(reduced: Boolean): Int = if (reduced) CivilightMotion.reducedFadeMs else CivilightMotion.exit.durationMs

    /** A panel tab's content swaps with a 200 ms standard fade; reduced motion 120 ms. */
    fun tabMs(reduced: Boolean): Int = if (reduced) CivilightMotion.reducedFadeMs else CivilightMotion.standard.durationMs
}
