package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * A block that fades and grows in on the fold tokens when it joins the composition (`if (x) AppearOnJoin { }`)
 * and leaves at once. For parents that space their children (`spacedBy`): an `AnimatedVisibility` that is
 * hidden still counts as a child there and would keep a gap open, which `FoldReveal` therefore cannot be
 * used for. Reduced motion: a 120 ms fade.
 */
@Composable
fun AppearOnJoin(modifier: Modifier = Modifier, horizontal: Boolean = false, content: @Composable () -> Unit) {
    val motion = CivilightTheme.motion
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = motion.enter(CivilightMotion.foldOpen, expand = true, horizontal = horizontal),
        exit = ExitTransition.None,
        label = "appearOnJoin",
    ) { content() }
}
