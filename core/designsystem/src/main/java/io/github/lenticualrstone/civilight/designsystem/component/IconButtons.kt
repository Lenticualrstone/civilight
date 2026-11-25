package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtons
import io.github.lenticualrstone.civilight.designsystem.icon.IconClass
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** An icon action with its required Korean accessibility label (owner copy), e.g. back or `⋯`. */
data class IconAction(val label: String, val onClick: () -> Unit)

/** DS-31 icon button: 44 × 44, icon 20 in `ink2`, no fill. [contentDescription] is the Korean label. */
@Composable
fun CivilightIconButton(
    icon: IconId,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CivilightTheme.colors.ink2,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressable(onClick = onClick, enabled = enabled, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape)
            .size(44.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(icon, contentDescription = null, size = iconSize, tint = tint)
    }
}

/**
 * DS-70 class a by name: the same 44 × 44 button with a 20 dp icon. [contentDescription] is required and must
 * not be blank (`IconButtonLint` checks every call site).
 */
@Composable
fun IconOnlyButton(
    icon: IconId,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CivilightTheme.colors.ink2,
    enabled: Boolean = true,
) {
    CivilightIconButton(icon, contentDescription, onClick, modifier, tint = tint, iconSize = IconButtons.ICON_ONLY_DP.dp, enabled = enabled)
}

/**
 * DS-70 class b: an icon 16 … 18 before (or over, [stacked]) a short label, gap 6, touch area ≥ 44. The
 * accessibility name is [contentDescription], which defaults to the label and may be the longer old text
 * (`시점` reads `시점 추가`). [on] marks a toggle (`설명`, `뒤집기`) and exposes its state.
 */
@Composable
fun IconLabelButton(
    icon: IconId,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = label,
    tint: Color = CivilightTheme.colors.ink2,
    labelColor: Color = CivilightTheme.colors.ink,
    labelStyle: TextStyle = CivilightTheme.type.button,
    iconSize: Dp = IconButtons.ICON_WITH_TEXT_DEFAULT_DP.dp,
    enabled: Boolean = true,
    on: Boolean? = null,
    stacked: Boolean = false,
) {
    val description = contentDescription
    val action = if (on != null) PressAction.Toggle(on) { onClick() } else PressAction.Click(onClick)
    val base = modifier
        .alpha(if (enabled) 1f else DISABLED_ALPHA)
        .pressable(
            action,
            enabled = enabled,
            role = if (on != null) Role.Switch else Role.Button,
            hitTarget = HitTarget.BUTTON,
            shape = RoundedCornerShape(12.dp),
        )
        .heightIn(min = IconButtons.TOUCH_DP.dp)
        .widthIn(min = IconButtons.TOUCH_DP.dp)
        .semantics { this.contentDescription = description }
    // The visible label is read through [contentDescription] only (it can be the longer old text).
    val hidden = Modifier.clearAndSetSemantics { }
    if (stacked) {
        androidx.compose.foundation.layout.Column(
            base,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CivilightIcon(icon, contentDescription = null, size = iconSize, tint = tint)
            CivilightText(label, labelStyle, hidden, color = labelColor, maxLines = 1)
        }
    } else {
        Row(
            base.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(IconButtons.ICON_TEXT_GAP_DP.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CivilightIcon(icon, contentDescription = null, size = iconSize, tint = tint)
            CivilightText(label, labelStyle, hidden, color = labelColor, maxLines = 1)
        }
    }
}

/**
 * A DS-70 table button by its [spec]: class a draws [IconOnlyButton], class b [IconLabelButton] and class c a
 * text action, each with the table's accessibility name. [label] overrides the visible text (a b row whose
 * text follows the screen, e.g. `새 장소` for [IconButtonSpec.LIST_CREATE]).
 */
@Composable
fun CivilightIconButton(
    spec: IconButtonSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    on: Boolean? = null,
    tint: Color = CivilightTheme.colors.ink2,
) {
    val icon = spec.icon
    val text = label ?: spec.text
    when {
        spec.cls == IconClass.A && icon != null ->
            IconOnlyButton(icon, spec.accessibilityName, onClick, modifier, tint = tint, enabled = enabled)
        spec.cls == IconClass.B && icon != null && text != null ->
            IconLabelButton(
                icon, text, onClick, modifier,
                contentDescription = if (label != null && spec.accessibilityName == spec.text) label else spec.accessibilityName,
                tint = tint, enabled = enabled, on = on,
            )
        else -> TextAction(text ?: spec.accessibilityName, onClick, modifier)
    }
}

/** DS-31 glass round button over art (profile): 44 × 44, radius 22, glass. */
@Composable
fun GlassRoundButton(
    icon: IconId,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.ICON_BUTTON, shape = CircleShape)
            .size(44.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = CircleShape)
        CivilightIcon(icon, contentDescription = null, tint = CivilightTheme.colors.ink)
    }
}

/** DS-31 glass pill next to the round button: height 44, padding 0 × 16, 500 12 / 16 `ink` (`시점 · 1-5`). */
@Composable
fun GlassPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, textColor: Color = CivilightTheme.colors.ink) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON, shape = shape)
            .heightIn(min = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape)
        CivilightText(text, CivilightTheme.type.chip, Modifier.padding(horizontal = 16.dp), color = textColor, maxLines = 1)
    }
}

/**
 * DS-31 pill: height 44, radius 22, 500 13 / 16. [primary] = `pri` / `onpri` 600 with padding 0 × 18
 * (`줄바꿈`); otherwise `tint` with padding 0 × 14, and padding-left 6 when [leading] (an avatar) is set.
 */
@Composable
fun Pill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(22.dp)
    val fill = if (primary) colors.pri else colors.tint
    val content = if (primary) colors.onpri else colors.ink
    val style = if (primary) type.button else type.button.merge(TextStyle(fontWeight = FontWeight.Medium))
    Row(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON, shape = shape)
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(fill)
            .padding(start = if (leading != null) 6.dp else if (primary) 18.dp else 14.dp, end = if (primary) 18.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        CivilightText(text, style, color = content, maxLines = 1)
    }
}

/** The script's speaker pill: avatar 32 + `리 · 리엔` style label. */
@Composable
fun SpeakerPill(
    name: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: androidx.compose.ui.graphics.painter.Painter? = null,
) {
    Pill(text = label, onClick = onClick, modifier = modifier, leading = { Avatar(name, size = AvatarSize.Pill, image = image) })
}

/**
 * DS-29 mic: a 32 dp `pri` circle with `Mic` 16 in `onpri`, touch area 44; label `라이프 레코더`.
 */
@Composable
fun MicButton(onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String = "라이프 레코더") {
    val colors = CivilightTheme.colors
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.SMALL, shape = CircleShape)
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.pri)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(IconId.Mic, contentDescription = null, size = 16.dp, tint = colors.onpri)
    }
}

/**
 * DS-31 keyboard tool (`EditKeyboard` top row): height 44, min width 44, radius 12, 600 13 / 16; off `ink2`,
 * on `sel` fill + `ink`. Toggle tools pass [on]; plain actions leave it null.
 */
@Composable
fun KeyboardTool(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: IconId? = null,
    on: Boolean? = null,
    contentDescription: String? = null,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val isOn = on == true
    val content by animatedColor(if (isOn) colors.ink else colors.ink2, label = "toolContent")
    val fill = animatedFraction(isOn, label = "toolFill")
    val action = if (on != null) PressAction.Toggle(on) { onClick() } else PressAction.Click(onClick)
    Row(
        modifier = modifier
            .pressable(action, role = if (on != null) Role.Switch else Role.Button, hitTarget = HitTarget.BUTTON, shape = shape)
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .clip(shape)
            .drawBehind { val a = fill.value; if (a > 0f) drawRect(colors.sel, alpha = a) }
            .padding(horizontal = if (text != null) 8.dp else 0.dp)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) CivilightIcon(icon, contentDescription = null, tint = content)
        if (text != null) CivilightText(text, CivilightTheme.type.button, color = content, maxLines = 1)
    }
}

/** The keyboard tools row (above the IME). */
@Composable
fun KeyboardToolsRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .background(CivilightTheme.colors.raise)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
