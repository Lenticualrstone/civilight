package io.github.lenticualrstone.civilight.feature.canvas.board

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
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
    val shape = RoundedCornerShape(24.dp)
    Box(modifier) {
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
                Box(
                    Modifier
                        .then(if (cover) Modifier.weight(1f) else Modifier)
                        .pressable(PressAction.Select(selected) { onSelect(tool) }, role = Role.RadioButton, hitTarget = HitTarget.CHIP, shape = cellShape)
                        .heightIn(min = if (cover) 44.dp else 36.dp)
                        .clip(cellShape)
                        .background(if (selected) colors.pri else Color.Transparent)
                        .padding(horizontal = if (cover) 0.dp else 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CivilightText(
                        tool.label(cover),
                        CivilightTheme.type.label.merge(if (cover) ToolCover else ToolInner),
                        color = if (selected) colors.onpri else colors.ink,
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
fun ZoomColumn(percent: String, canIn: Boolean, canOut: Boolean, onIn: () -> Unit, onOut: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(modifier) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape, source = null)
        Column(Modifier.insetOutline(colors.line, 1.dp, shape), horizontalAlignment = Alignment.CenterHorizontally) {
            CivilightIconButton(IconId.Plus, CanvasCopy.ZOOM_IN, onIn, iconSize = 18.dp, enabled = canIn)
            CivilightText(percent, CivilightTheme.type.number.merge(TextStyle(fontSize = 12.sp, lineHeight = 16.sp)), color = colors.ink2, maxLines = 1)
            MinusButton(CanvasCopy.TODO_COPY_CANVAS_ZOOM_OUT, onOut, enabled = canOut)
        }
    }
}

/**
 * DS-12 has no minus glyph; the board draws `M5 12h14` with the DS stroke (1.6, round caps). Same
 * geometry as [CivilightIconButton] (Requests: add `IconId.Minus`).
 */
private val MinusVector: ImageVector by lazy {
    ImageVector.Builder("CanvasMinus", 24.dp, 24.dp, 24f, 24f).addPath(
        pathData = PathParser().parsePathString("M5 12h14").toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
}

@Composable
private fun MinusButton(label: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.38f)
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape)
            .size(44.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(MinusVector, contentDescription = null, size = 18.dp, tint = CivilightTheme.colors.ink2)
    }
}

/** Cover hint (`CanvasC`), small `ink3`. */
@Composable
fun CanvasHint(modifier: Modifier = Modifier) {
    CivilightText(CanvasCopy.HINT, CivilightTheme.type.small, modifier, color = CivilightTheme.colors.ink3)
}
