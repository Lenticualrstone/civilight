package io.github.lenticualrstone.civilight.feature.script.fold

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion

/**
 * Fold motion of the side panel (SCR-66, board `FoldSpec`): the second pane comes in from 48 dp to
 * the right over 360 ms decelerate on unfold, and leaves with a 240 ms accelerate fade on fold;
 * reduced motion: a 120 ms crossfade, no movement.
 */
object FoldMotion {
    /** How far the second pane travels in on unfold. */
    const val PANE_SHIFT_DP: Int = 48

    fun panelEnter(reduced: Boolean, density: Density): EnterTransition {
        if (reduced) return fadeIn(tween(CivilightMotion.reducedFadeMs, easing = LinearEasing))
        val token = CivilightMotion.foldOpen
        val shift = with(density) { PANE_SHIFT_DP.dp.roundToPx() }
        return slideInHorizontally(tween(token.durationMs, easing = token.easing)) { shift } +
            fadeIn(tween(token.durationMs, easing = token.easing))
    }

    fun panelExit(reduced: Boolean): ExitTransition {
        if (reduced) return fadeOut(tween(CivilightMotion.reducedFadeMs, easing = LinearEasing))
        val token = CivilightMotion.foldClose
        return fadeOut(tween(token.durationMs, easing = token.easing))
    }
}
