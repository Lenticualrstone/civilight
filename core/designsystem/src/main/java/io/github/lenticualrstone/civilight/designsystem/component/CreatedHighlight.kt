package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
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
 * drawing only, not a selection: TalkBack does not read the row as selected. [onDone] runs once the fade
 * ended, so the caller can clear its key.
 */
@Composable
fun Modifier.createdHighlight(active: Boolean, onDone: () -> Unit = {}): Modifier {
    if (!active) return this
    val motion = CivilightTheme.motion
    val color = CivilightTheme.colors.hl
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(0f, motion.spec(MotionToken(CivilightMotion.standardCurve, CivilightMotion.createdRowHighlightMs)))
        onDone()
    }
    return drawBehind { drawRect(color, alpha = alpha.value) }
}
