package io.github.lenticualrstone.civilight.feature.cast.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Lets a child run past its parent's horizontal content padding to the screen edge: the child is measured
 * [side] wider on each side and placed [side] to the left, so it reports the parent's width and the next
 * card of a snapping row peeks out at the edge (QA 7, GAL-01). [side] is the parent's content padding.
 */
fun Modifier.bleed(side: Dp): Modifier = layout { measurable, constraints ->
    val extra = (side * 2).roundToPx()
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
