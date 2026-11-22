package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.animatedInsetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcons
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction

/**
 * DS-32 switch: track 46 × 28 radius 14; off `tint` + 1 dp `line`, thumb `ink3` at x 3; on `acc`, thumb `bg`
 * at x 21. Thumb moves with `Standard` (jumps when motion is reduced). Touch area 54 × 44. Pass a null
 * [onCheckedChange] when the whole row toggles.
 */
@Composable
fun CivilightSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val style = switchStyle(checked, colors)
    val motion = LocalMotion.current
    val thumbX by animateDpAsState(
        targetValue = style.thumbX,
        animationSpec = motion.move(CivilightMotion.standard),
        label = "thumb",
    )
    val track = animatedColor(style.track, label = "track")
    val thumb = animatedColor(style.thumb, label = "thumbColor")
    val outline = animatedFraction(style.trackOutline != null, label = "trackOutline")
    val enabledAlpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "enabled")
    val interactive = if (onCheckedChange != null) {
        Modifier.pressable(
            PressAction.Toggle(checked, onCheckedChange),
            enabled = enabled,
            role = Role.Switch,
            hitTarget = HitTarget.SWITCH,
            shape = RoundedCornerShape(14.dp),
            scaleOnPress = false,
        )
    } else Modifier
    Canvas(
        modifier
            .graphicsLayer { alpha = enabledAlpha.value }
            .then(interactive)
            .size(46.dp, 28.dp),
    ) {
        val r = CornerRadius(14.dp.toPx())
        drawRoundRect(track.value, cornerRadius = r)
        val o = outline.value
        if (o > 0f) {
            val w = 1.dp.toPx()
            drawRoundRect(colors.line, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), CornerRadius(r.x - w / 2), style = Stroke(w), alpha = o)
        }
        val t = 22.dp.toPx()
        drawRoundRect(thumb.value, Offset(thumbX.toPx(), 3.dp.toPx()), Size(t, t), CornerRadius(t / 2))
    }
}

/**
 * DS-32 radio: 22 dp circle, 1.5 dp ring; on: `acc` ring + 12 dp `acc` dot. Touch area 44 × 44. The ring
 * colour crosses over and the dot scales 0 → 1 on `enter` (S6); reduced motion: the dot fades in place.
 */
@Composable
fun RadioMark(selected: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, enabled: Boolean = true) {
    val colors = CivilightTheme.colors
    val style = radioStyle(selected, colors)
    val dotColor = radioStyle(true, colors).dot ?: colors.acc
    val reduced = LocalMotion.current.reduced
    val ring = animatedColor(style.ring ?: colors.ink3, label = "radioRing")
    val dot = animatedFraction(selected, if (selected) CivilightMotion.enter else CivilightMotion.exit, label = "radioDot")
    val enabledAlpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "enabled")
    val interactive = if (onClick != null) {
        Modifier.pressable(PressAction.Select(selected, onClick), enabled = enabled, role = Role.RadioButton, hitTarget = HitTarget.RADIO, shape = CircleShape, scaleOnPress = false)
    } else Modifier
    Canvas(modifier.graphicsLayer { alpha = enabledAlpha.value }.then(interactive).size(22.dp)) {
        val w = style.ringWidth.toPx()
        drawCircle(ring.value, radius = size.minDimension / 2 - w / 2, style = Stroke(w))
        val d = dot.value
        if (d > 0f) {
            if (reduced) drawCircle(dotColor, radius = 6.dp.toPx(), alpha = d)
            else drawCircle(dotColor, radius = 6.dp.toPx() * d)
        }
    }
}

/**
 * DS-32 checkbox: 22 × 22 (20 when [dense]), radius 6; off 1.5 dp `ink3` ring; on `acc` fill with the
 * `Check` tick 16 dp in `bg`, stroke 3 grid units. Touch area 44 × 44.
 */
@Composable
fun CheckboxMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    dense: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val on = checkboxStyle(true, colors)
    val off = checkboxStyle(false, colors)
    val side = if (dense) 20.dp else 22.dp
    val shape = RoundedCornerShape(6.dp)
    val reduced = LocalMotion.current.reduced
    val fill = animatedFraction(checked, label = "checkFill")
    val tick = animatedFraction(checked, if (checked) CivilightMotion.enter else CivilightMotion.exit, label = "checkTick")
    val enabledAlpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "enabled")
    val interactive = if (onCheckedChange != null) {
        Modifier.pressable(PressAction.Toggle(checked, onCheckedChange), enabled = enabled, role = Role.Checkbox, hitTarget = HitTarget.CHECKBOX, shape = shape, scaleOnPress = false)
    } else Modifier
    val ringWidth = off.ringWidth
    Box(
        modifier = modifier
            .graphicsLayer { alpha = enabledAlpha.value }
            .then(interactive)
            .size(side)
            .clip(shape)
            .drawBehind {
                val f = fill.value
                if (f > 0f) drawRect(on.fill ?: colors.acc, alpha = f)
                if (f < 1f) {
                    val w = ringWidth.toPx()
                    val outline = shape.createOutline(Size(size.width - w, size.height - w), layoutDirection, this)
                    translate(w / 2, w / 2) { drawOutline(outline, off.ring ?: colors.ink3, alpha = 1f - f, style = Stroke(w)) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(CivilightIcons.checkBold),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    val t = tick.value
                    alpha = t
                    val s = if (reduced) 1f else 0.5f + 0.5f * t
                    scaleX = s
                    scaleY = s
                },
            colorFilter = ColorFilter.tint(on.tick ?: colors.bg),
        )
    }
}

/** Radio row (`.li` with the radio at the right): title 15 / 22 700 + `small` sub-line; the whole row selects. */
@Composable
fun RadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    divider: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val enabledAlpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "enabled")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = enabledAlpha.value }
            .then(if (divider) Modifier.topHairline(colors.line) else Modifier)
            .pressable(PressAction.Select(selected, onClick), enabled = enabled, role = Role.RadioButton, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(title, type.itemTitleCompact, color = colors.ink)
            if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3, maxLines = 1)
        }
        RadioMark(selected)
    }
}

/** Checkbox row: checkbox at the left, gap 12, padding 11 · 0, optional right `small` note; the row toggles. */
@Composable
fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    subtitle: String? = null,
    dense: Boolean = false,
    divider: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val enabledAlpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "enabled")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = enabledAlpha.value }
            .then(if (divider) Modifier.topHairline(colors.line) else Modifier)
            .pressable(PressAction.Toggle(checked, onCheckedChange), enabled = enabled, role = Role.Checkbox, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckboxMark(checked, dense = dense)
        Column(Modifier.weight(1f)) {
            CivilightText(label, type.command, color = colors.ink)
            if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3)
        }
        if (note != null) CivilightText(note, type.small, color = colors.ink3, maxLines = 1)
    }
}

/** Settings switch row: title + optional `small` sub-line, switch at the right; the row toggles. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    divider: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val enabledAlpha = animatedAlpha(if (enabled) 1f else DISABLED_ALPHA, label = "enabled")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = enabledAlpha.value }
            .then(if (divider) Modifier.topHairline(colors.line) else Modifier)
            .pressable(PressAction.Toggle(checked, onCheckedChange), enabled = enabled, role = Role.Switch, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(title, type.itemTitleCompact, color = colors.ink)
            if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3)
        }
        CivilightSwitch(checked, onCheckedChange = null)
    }
}

/**
 * DS-32 choice card (radio card): padding 14 × 16 (12 × 14 [compact]), radius 16 (14), selected `sel` +
 * 1.5 dp `acc` inset, unselected 1 dp `edge` inset; title `itemTitle`, sub `small`, optional [extra]
 * content, radio at the far right. Cards stack 8 dp apart.
 */
@Composable
fun ChoiceCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    compact: Boolean = false,
    selectedOutline: androidx.compose.ui.graphics.Color? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val radius: Dp = if (compact) 14.dp else 16.dp
    val shape = RoundedCornerShape(radius)
    val on = animatedFraction(selected, label = "choice")
    val outline = animatedColor(if (selected) selectedOutline ?: colors.acc else colors.edge, label = "choiceOutline")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, shape = shape, scaleOnPress = false)
            .clip(shape)
            .drawBehind { val a = on.value; if (a > 0f) drawRect(colors.sel, alpha = a) }
            .animatedInsetOutline(outline, { 1.dp + 0.5.dp * on.value }, shape)
            .padding(horizontal = if (compact) 14.dp else 16.dp, vertical = if (compact) 12.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CivilightText(title, type.itemTitle, color = colors.ink)
            if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3)
            extra?.invoke()
        }
        RadioMark(selected)
    }
}

/** Type grid cell: radio + 14 / 20 label; selected cell `sel` fill, radius 12, label 600. */
@Composable
fun TypeGridCell(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val on = animatedFraction(selected, label = "typeCell")
    Row(
        modifier = modifier
            .pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, shape = shape, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .clip(shape)
            .drawBehind { val a = on.value; if (a > 0f) drawRect(colors.sel, alpha = a) }
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioMark(selected)
        CivilightText(
            label,
            CivilightTheme.type.command.merge(TextStyle(fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)),
            color = colors.ink,
            maxLines = 1,
        )
    }
}
