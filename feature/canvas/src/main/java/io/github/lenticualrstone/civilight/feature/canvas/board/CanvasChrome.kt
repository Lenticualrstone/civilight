package io.github.lenticualrstone.civilight.feature.canvas.board

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import androidx.compose.ui.semantics.semantics
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy

private val ToolCover = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
private val ToolInner = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

/**
 * The canvas tool bar (CNV-03): a glass pill with a 1 dp `line` inset, padding 6, gap 4; the current
 * tool `pri` / `onpri`. Cover (`CanvasC`): five equal columns, 44 tall, labels `노드 · 인물 · 메모 · 선 ·
 * 묶음`. Inner (`Canvas`): a centred pill of 36-tall cells with `인물 카드`. Drawn over the surface, so
 * the glass shows its SCRIM fill (it is not a descendant of a glass source).
 */
@Composable
fun CanvasToolBar(current: CanvasTool, cover: Boolean, onSelect: (CanvasTool) -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    val shape = RoundedCornerShape(24.dp)
    // A fold or unfold moves the bar's width, cell padding and height on the fold tokens (audit §4 canvas 10).
    val fold = if (cover) CivilightMotion.foldClose else CivilightMotion.foldOpen
    val cellPadding by animateDpAsState(if (cover) 0.dp else 14.dp, motion.move(fold), label = "toolCellPadding")
    val cellHeight by animateDpAsState(if (cover) 44.dp else 36.dp, motion.move(fold), label = "toolCellHeight")
    Box(modifier.animateContentSize(motion.move(fold))) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape, source = null)
        Row(
            Modifier
                .then(if (cover) Modifier.fillMaxWidth() else Modifier)
                .insetOutline(colors.line, 1.dp, shape)
                .padding(6.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (tool in CanvasTool.entries) {
                val selected = tool == current
                val cellShape = RoundedCornerShape(18.dp)
                val fill = animatedFraction(selected)
                val ink by animatedColor(if (selected) colors.onpri else colors.ink, label = "toolInk")
                Box(
                    Modifier
                        .then(if (cover) Modifier.weight(1f) else Modifier)
                        .pressable(PressAction.Select(selected) { onSelect(tool) }, role = Role.RadioButton, hitTarget = HitTarget.CHIP, shape = cellShape)
                        .heightIn(min = cellHeight)
                        .clip(cellShape)
                        .drawBehind { drawRect(colors.pri, alpha = fill.value) }
                        .padding(horizontal = cellPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CivilightText(
                        tool.label(cover),
                        CivilightTheme.type.label.merge(if (cover) ToolCover else ToolInner),
                        color = ink,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Zoom column (CNV-01, `CanvasC`): glass, radius 16, 1 dp `line`; `+`, the percentage in the number
 * role `ink2`, `−`; buttons 44 × 44.
 */
@Composable
fun ZoomColumn(
    percent: String,
    canIn: Boolean,
    canOut: Boolean,
    onIn: () -> Unit,
    onOut: () -> Unit,
    modifier: Modifier = Modifier,
    swapPercent: Boolean = false,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(modifier) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape, source = null)
        Column(Modifier.insetOutline(colors.line, 1.dp, shape), horizontalAlignment = Alignment.CenterHorizontally) {
            CivilightIconButton(IconId.Plus, CanvasCopy.ZOOM_IN, onIn, iconSize = 18.dp, enabled = canIn)
            val style = CivilightTheme.type.number.merge(TextStyle(fontSize = 12.sp, lineHeight = 16.sp))
            // A button press swaps the value like a lens value (CNV-42, audit P4); a pinch changes it every frame, so it is shown as is.
            if (swapPercent) ValueSwap(percent, style, color = colors.ink2, contentAlignment = Alignment.Center)
            else CivilightText(percent, style, color = colors.ink2, maxLines = 1)
            CivilightIconButton(IconId.Minus, CanvasCopy.ZOOM_OUT, onOut, iconSize = 18.dp, enabled = canOut)
        }
    }
}

/** Cover hint (`CanvasC`), small `ink3`. */
@Composable
fun CanvasHint(modifier: Modifier = Modifier) {
    CivilightText(CanvasCopy.HINT, CivilightTheme.type.small, modifier, color = CivilightTheme.colors.ink3)
}
