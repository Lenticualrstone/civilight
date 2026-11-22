package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.lenticualrstone.civilight.designsystem.component.DISABLED_ALPHA
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha

/**
 * A control that is on or off (`줄바꿈`, `붙이기`, a speaker row of a layer that is off): it fades to the
 * disabled opacity and back on the standard token instead of cutting (motion audit, row 12). Read in
 * the draw phase.
 */
@Composable
fun Modifier.dimmedWhen(dimmed: Boolean): Modifier {
    val alpha by animatedAlpha(if (dimmed) DISABLED_ALPHA else 1f, label = "dim")
    return graphicsLayer { this.alpha = alpha }
}
