package io.github.lenticualrstone.civilight.feature.library.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ArrowEdge
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.TourProgress
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-49 tour card with the third button the boards draw from step 2 on (`건너뛰기 | 이전 | 다음`,
 * boards `Tour2`, `Tour4`). Same geometry as the DS `TourCard` (radius 16, `raise`, 1 dp `line`,
 * `shadow-float`, arrow 12 × 12 turned 45°), which has no `이전` slot yet (Request to core:designsystem).
 */
@Composable
internal fun TourStepCard(
    stepCount: Int,
    stepIndex: Int,
    title: String,
    subtitle: String?,
    label: String?,
    buttons: TourButtons,
    onSkip: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: () -> Unit,
    isLast: Boolean,
    arrowEdge: ArrowEdge,
    arrowX: Dp,
    modifier: Modifier = Modifier,
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
        CivilightText(title, type.itemTitle, color = colors.ink)
        if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextAction(buttons.skip, onSkip, style = type.label.merge(TextStyle(fontSize = 12.sp)))
            Spacer(Modifier.weight(1f))
            if (onPrevious != null) CivilightButton(buttons.previous, onPrevious, variant = ButtonVariant.COMPACT_GHOST)
            CivilightButton(if (isLast) buttons.finish else buttons.next, onNext, variant = ButtonVariant.COMPACT_PRIMARY)
        }
    }
}
