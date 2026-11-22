package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateRectAsState
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** Where the tooltip arrow sits. */
enum class ArrowEdge { TOP, BOTTOM, NONE }

/**
 * DS-49 tour card: padding 14 · 16 · 12, radius 16, `raise`, 1 dp `line` inset + `shadow-float`; an arrow
 * (a 12 × 12 square turned 45°, 6 dp outside the [arrowEdge], at [arrowX] from the card's start);
 * tour progress + `label · n/N`, title 700 16 / 22, `small` sub; `건너뛰기` at the left and a compact
 * primary `다음` at the right (copy LIB).
 */
@Composable
fun TourCard(
    stepCount: Int,
    stepIndex: Int,
    title: String,
    nextLabel: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    subtitle: String? = null,
    skipLabel: String? = null,
    onSkip: (() -> Unit)? = null,
    arrowEdge: ArrowEdge = ArrowEdge.TOP,
    arrowX: Dp = 24.dp,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (arrowEdge == ArrowEdge.NONE) return@drawBehind
                val half = 6.dp.toPx()
                val x = arrowX.toPx()
                val edgeY = if (arrowEdge == ArrowEdge.TOP) 0f else size.height
                val tipY = if (arrowEdge == ArrowEdge.TOP) -half else size.height + half
                val tri = Path().apply {
                    moveTo(x - half, edgeY)
                    lineTo(x, tipY)
                    lineTo(x + half, edgeY)
                    close()
                }
                drawPath(tri, colors.raise)
                val edge = Path().apply {
                    moveTo(x - half, edgeY)
                    lineTo(x, tipY)
                    lineTo(x + half, edgeY)
                }
                drawPath(edge, colors.line, style = Stroke(1.dp.toPx()))
            }
            .tokenShadow(shape, CivilightTheme.elevation.float(colors.isDark))
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TourProgress(stepCount, stepIndex, label)
        // The step body crossfades when the step changes (S8); its height follows on `standard`.
        ContentSwap(Triple(stepIndex, title, subtitle), contentKey = { it.first }, label = "tourStep") { (_, t, sub) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CivilightText(t, type.itemTitle, color = colors.ink)
                if (sub != null) CivilightText(sub, type.small, color = colors.ink3)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (skipLabel != null && onSkip != null) {
                TextAction(skipLabel, onSkip, style = type.label.merge(TextStyle(fontSize = 12.sp)))
            }
            Spacer(Modifier.weight(1f))
            CivilightButton(nextLabel, onNext, variant = ButtonVariant.COMPACT_PRIMARY)
        }
    }
}

/**
 * DS-49 tour dim: `shade` over the screen with a cut-out = [target] padded 6 dp on every side, framed by a
 * rounded rectangle (radius 12) with a 1.5 dp `acc` ring. Never a circle. [target] is in this composable's
 * coordinates (px). A new target moves the hole on `standard` (S8; it jumps when reduced); the hole fades
 * in and out when the target appears or goes.
 */
@Composable
fun TourDim(target: Rect?, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val last = rememberLastNonNull(target)
    val holeRect by animateRectAsState(last ?: Rect.Zero, motion.move(CivilightMotion.standard), label = "tourHole")
    val holeAlpha = animatedFraction(target != null, label = "tourHoleAlpha")
    Canvas(
        modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        drawRect(colors.shade)
        val a = holeAlpha.value
        if (last != null && a > 0f) {
            val t = holeRect
            val pad = 6.dp.toPx()
            val hole = Rect(t.left - pad, t.top - pad, t.right + pad, t.bottom + pad)
            val r = CornerRadius(12.dp.toPx())
            val path = Path().apply { addRoundRect(RoundRect(hole, r)) }
            drawPath(path, Color.Black, alpha = a, blendMode = BlendMode.DstOut)
            drawRoundRect(colors.acc, Offset(hole.left, hole.top), hole.size, r, style = Stroke(1.5.dp.toPx()), alpha = a)
        }
    }
}
