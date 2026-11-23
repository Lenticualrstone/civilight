package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction

/**
 * DS-30 button. Labels are verbs (never `확인` alone). [subText] is required for the two-line variants.
 * Pressed: content scales to 0.94 (ghost variants also show an `ink` 8 % state layer); disabled: 38 %.
 * Compact variants are drawn 36 tall with a 44 dp touch area.
 */
@Composable
fun CivilightButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    icon: IconId? = null,
    subText: String? = null,
    enabled: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val style = buttonStyle(variant, colors)
    val shape = RoundedCornerShape(style.radius)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val ghostLike = style.fill == colors.tint
    val twoLine = variant == ButtonVariant.TWO_LINE || variant == ButtonVariant.TWO_LINE_GHOST
    val textStyle = when {
        style.large -> type.buttonLarge
        style.compact -> type.button.copy(fontSize = 12.sp)
        else -> type.button
    }
    val ghostLayer = animatedFraction(pressed && ghostLike && enabled, CivilightMotion.press, label = "ghostLayer")
    val enabledAlpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "enabled")
    Box(
        modifier = modifier
            .graphicsLayer { alpha = enabledAlpha.value }
            .pressable(
                onClick = onClick,
                enabled = enabled,
                hitTarget = HitTarget.BUTTON,
                shape = shape,
                interactionSource = source,
                pressOverlay = !ghostLike,
            )
            .heightIn(min = style.height)
            .clip(shape)
            .background(style.fill)
            .then(if (style.outline != null) Modifier.border(1.dp, style.outline, shape) else Modifier)
            .drawWithContent {
                drawContent()
                val a = ghostLayer.value
                if (a > 0f) drawRect(colors.ink, alpha = 0.08f * a)
            }
            .padding(
                if (twoLine) PaddingValues(horizontal = style.horizontalPadding, vertical = 8.dp)
                else PaddingValues(horizontal = style.horizontalPadding),
            ),
        contentAlignment = if (twoLine) Alignment.CenterStart else Alignment.Center,
    ) {
        if (twoLine) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CivilightText(text, TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold).withFamily(type.button), color = style.content)
                if (subText != null) {
                    CivilightText(
                        subText,
                        TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal).withFamily(type.button),
                        color = style.content.copy(alpha = style.content.alpha * 0.8f),
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    CivilightIcon(icon, contentDescription = null, size = 16.dp, tint = style.content)
                    Spacer(Modifier.width(6.dp))
                }
                CivilightText(text, textStyle, color = style.content, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * A text action such as `닫기` or `건너뛰기`: `label` role in `ink2` (or [color]), no fill, touch area ≥ 44.
 */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = CivilightTheme.colors.ink2,
    style: TextStyle = CivilightTheme.type.label,
) {
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(text, style, color = color, maxLines = 1)
    }
}

/** Keeps the family and typesetting of [base] with this style's size and weight. */
internal fun TextStyle.withFamily(base: TextStyle): TextStyle = base.merge(this)

/**
 * A row of buttons 8 dp apart (sheet / dialog actions). Children use `Modifier.weight(...)`:
 * equal flex, or flex 2 for the primary when a cancel exists (`scr_new_scene.py` `actions`).
 */
@Composable
fun ButtonRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

/**
 * Whether equal-width buttons fit side by side (펼친 화면 비율, CNV-47): in a 280 dp list pane at 1:2 the
 * empty block's `라이프 레코더에서` was cut to `라이프 ...` at flex 1. Each button gets
 * `(width - gaps) / n`; when any label's natural width needs more, the buttons stack full width, [gap] apart.
 */
object ButtonRowFit {
    fun sideBySide(natural: List<Int>, width: Int, gap: Int): Boolean {
        if (natural.size <= 1) return true
        val slot = slot(natural.size, width, gap)
        return natural.all { it <= slot }
    }

    /** One button's width in the side-by-side row. */
    fun slot(count: Int, width: Int, gap: Int): Int = ((width - gap * (count - 1)) / count.coerceAtLeast(1)).coerceAtLeast(0)
}

/**
 * [ButtonRow] for equal-flex buttons that keeps every label whole: side by side at flex 1 while each label fits
 * its half ([ButtonRowFit]), else stacked full width in the given order, [gap] apart. Children take no weight.
 */
@Composable
fun FittingButtonRow(modifier: Modifier = Modifier, gap: Dp = 8.dp, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else natural.sum() + gapPx * (natural.size - 1).coerceAtLeast(0)
        if (ButtonRowFit.sideBySide(natural, width, gapPx)) {
            val slot = ButtonRowFit.slot(measurables.size, width, gapPx)
            val placeables = measurables.map { it.measure(Constraints(minWidth = slot, maxWidth = slot)) }
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
                placeables.forEachIndexed { i, p -> p.placeRelative(i * (slot + gapPx), (height - p.height) / 2) }
            }
        } else {
            val placeables = measurables.map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
            val height = placeables.sumOf { it.height } + gapPx * (placeables.size - 1).coerceAtLeast(0)
            layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
                var y = 0
                placeables.forEach { p -> p.placeRelative(0, y); y += p.height + gapPx }
            }
        }
    }
}
