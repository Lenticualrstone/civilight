package io.github.lenticualrstone.civilight.designsystem.foundation

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A 1 dp line at the top edge (row dividers use `line`; deleted counterparts use [dashed], DS-18). */
fun Modifier.topHairline(color: Color, dashed: Boolean = false, width: Dp = 1.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawLine(
        color,
        Offset(0f, w / 2),
        Offset(size.width, w / 2),
        strokeWidth = w,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
    )
}

/** A 1 dp line at the start edge (side panel inset). */
fun Modifier.startHairline(color: Color, width: Dp = 1.dp): Modifier = drawBehind {
    drawRect(color, Offset.Zero, Size(width.toPx(), size.height))
}

/** A 1 dp line at the bottom edge. */
fun Modifier.bottomHairline(color: Color, width: Dp = 1.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRect(color, Offset(0f, size.height - w), Size(size.width, w))
}

/**
 * An inside outline (CSS `box-shadow: inset 0 0 0 Npx`): drawn over the content within the bounds,
 * so it never changes layout. [dashed] draws the memo-node dashed variant (4 / 3 dp); [dotted] the
 * rule 20 "attached to something deleted" variant (round dots, 1.5 / 3 dp). [dotted] wins when both are set.
 */
fun Modifier.insetOutline(
    color: Color,
    width: Dp,
    shape: Shape,
    dashed: Boolean = false,
    overContent: Boolean = true,
    dotted: Boolean = false,
): Modifier = drawWithContent {
    if (!overContent) drawInset(color, width, shape, dashed, dotted)
    drawContent()
    if (overContent) drawInset(color, width, shape, dashed, dotted)
}

/**
 * [insetOutline] whose colour and width are read in the draw phase (animated selection outlines, S6), so the
 * owner does not recompose per frame.
 */
fun Modifier.animatedInsetOutline(color: State<Color>, width: () -> Dp, shape: Shape, dashed: Boolean = false, dotted: Boolean = false): Modifier =
    drawWithContent {
        drawContent()
        drawInset(color.value, width(), shape, dashed, dotted)
    }

private fun DrawScope.drawInset(color: Color, width: Dp, shape: Shape, dashed: Boolean, dotted: Boolean = false) {
    val w = width.toPx()
    if (w <= 0f) return
    val inner = Size(size.width - w, size.height - w)
    val outline = shape.createOutline(inner, layoutDirection, this)
    translate(w / 2, w / 2) {
        drawOutline(
            outline,
            color,
            style = when {
                dotted -> Stroke(w, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, 3.dp.toPx() + w)))
                dashed -> Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                else -> Stroke(w)
            },
        )
    }
}

/** DS-08 / DS-09: manuscript columns never exceed `measure` 560 dp. */
fun Modifier.readingMeasure(measure: Dp = 560.dp): Modifier = this.then(Modifier.widthIn(max = measure))
