package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken

/**
 * DS-72 / U-15.6: the `hl` fill behind a row just made with `만들기`, full at first and faded out over
 * [CivilightMotion.createdRowHighlightMs] on the standard curve (reduced motion: the reduced fade). A
 * drawing only, not a selection: TalkBack does not read the row as selected. [onDone] runs once, when the fade
 * ended or when a drawn row leaves composition before that, so the caller can clear its key.
 */
@Composable
fun Modifier.createdHighlight(active: Boolean, onDone: () -> Unit = {}): Modifier {
    if (!active) return this
    val motion = CivilightTheme.motion
    val color = CivilightTheme.colors.hl
    val alpha = remember { Animatable(1f) }
    val done = remember { booleanArrayOf(false) }
    val latestOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        alpha.animateTo(0f, motion.spec(MotionToken(CivilightMotion.standardCurve, CivilightMotion.createdRowHighlightMs)))
        done[0] = true
        latestOnDone()
    }
    // The row was drawn, then left composition before the fade ended (scrolled out of a lazy list, screen
    // popped): the key is cleared anyway, so the flash does not replay each time the row comes back. A row
    // only composed ahead (lazy prefetch) and never drawn keeps its key, so the flash is still seen.
    val drawn = remember { booleanArrayOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            if (!done[0] && drawn[0]) {
                done[0] = true
                latestOnDone()
            }
        }
    }
    return drawBehind {
        drawn[0] = true
        drawRect(color, alpha = alpha.value)
    }
}
