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
import androidx.compose.ui.draw.alpha
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
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

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
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressable(
                onClick = onClick,
                enabled = enabled,
                hitTarget = HitTarget.BUTTON,
                shape = shape,
                interactionSource = source,
            )
            .heightIn(min = style.height)
            .clip(shape)
            .background(style.fill)
            .then(if (style.outline != null) Modifier.border(1.dp, style.outline, shape) else Modifier)
            .drawWithContent {
                drawContent()
                if (pressed && ghostLike && enabled) drawRect(colors.ink.copy(alpha = 0.08f))
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
