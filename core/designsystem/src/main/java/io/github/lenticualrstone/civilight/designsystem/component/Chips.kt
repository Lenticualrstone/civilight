package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors

/**
 * DS-36 filter / choice chip (`.tag`): height 30, padding 0 × 12, radius 15, 1 dp `edge` inset, `chip` role
 * `ink2`; selected `pri` fill + `onpri`, no outline. Counts go inside the label (`전체 12`). Touch area
 * `max(w + 8, 44) × 44`.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(15.dp)
    val on = animatedFraction(selected, label = "chip")
    val text by animatedColor(if (selected) colors.onpri else colors.ink2, label = "chipText")
    Box(
        modifier = modifier
            .pressable(PressAction.Select(selected, onClick), enabled = enabled, role = Role.Button, hitTarget = HitTarget.CHIP, shape = shape)
            .heightIn(min = 30.dp)
            .clip(shape)
            .drawWithContent {
                val a = on.value
                if (a > 0f) drawRect(colors.pri, alpha = a)
                drawContent()
                if (a < 1f) {
                    val w = 1.dp.toPx()
                    val outline = shape.createOutline(Size(size.width - w, size.height - w), layoutDirection, this)
                    translate(w / 2, w / 2) { drawOutline(outline, colors.edge, alpha = 1f - a, style = Stroke(w)) }
                }
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(label, CivilightTheme.type.chip, color = text, maxLines = 1)
    }
}

/** Chip rows wrap with 6 dp gaps (DS-36). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(
    modifier: Modifier = Modifier,
    gap: Dp = 6.dp,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/**
 * DS-36 removable chip (recorder `연결`): height 32, gap 6, text `ink`, optional leading icon 14, trailing
 * `X` 14 that removes ([removeLabel] is its accessibility label, from the owning area's copy).
 */
@Composable
fun RemovableChip(
    label: String,
    onRemove: () -> Unit,
    removeLabel: String,
    modifier: Modifier = Modifier,
    leadingIcon: IconId? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape) else Modifier)
            .heightIn(min = 32.dp)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(start = 12.dp, end = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) CivilightIcon(leadingIcon, contentDescription = null, size = 14.dp, tint = colors.ink2)
        CivilightText(label, CivilightTheme.type.chip, color = colors.ink, maxLines = 1)
        Box(
            modifier = Modifier
                .pressable(onClick = onRemove, hitTarget = HitTarget.SMALL)
                .heightIn(min = 28.dp)
                .padding(horizontal = 6.dp)
                .semantics { contentDescription = removeLabel },
            contentAlignment = Alignment.Center,
        ) {
            CivilightIcon(IconId.X, contentDescription = null, size = 14.dp, tint = colors.ink2)
        }
    }
}

/** An add chip without `X` (`+ 연결`): the removable-chip shape with a leading `Plus`. */
@Composable
fun AddChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape)
            .heightIn(min = 32.dp)
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, size = 14.dp, tint = colors.ink2)
        CivilightText(label, CivilightTheme.type.chip, color = colors.ink, maxLines = 1)
    }
}

/** DS-36 map value chip (`.chipv`): height 32, padding 0 × 12, radius 16, 600 12 / 16, gap 6; fill per owner. */
@Composable
fun MapValueChip(
    label: String,
    modifier: Modifier = Modifier,
    fill: Color = CivilightTheme.colors.tint,
    contentColor: Color = CivilightTheme.colors.ink,
    icon: IconId? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape) else Modifier)
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(fill)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) CivilightIcon(icon, contentDescription = null, size = 14.dp, tint = contentColor)
        CivilightText(label, CivilightTheme.type.chip.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = contentColor, maxLines = 1)
    }
}

/**
 * DS-36 layer flag chip (`표시 · 용어 후보 켬`): the lens-chip shape at 28 tall, `ink` text with the
 * [value] word in `acc` 600.
 */
@Composable
fun LayerFlagChip(prefix: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val text = buildAnnotatedString {
        append(prefix)
        append(" ")
        withStyle(SpanStyle(color = colors.acc, fontWeight = FontWeight.SemiBold)) { append(value) }
    }
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.LENS_CHIP, shape = shape)
            .heightIn(min = 28.dp)
            .clip(shape)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        DecoratedText(text, CivilightTheme.type.chip, color = colors.ink, maxLines = 1)
    }
}

/**
 * DS-51 status chip over images (`.hchip`): height 24, padding 0 × 9, radius 12, 600 11 / 14 `onImage` on
 * `imageChipFill` with a 12 dp blur (`홀로`, `쓰는 중 · 홀로`).
 */
@Composable
fun ImageStatusChip(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = modifier.heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape, fill = FixedColors.imageChipFill)
        CivilightText(
            text,
            CivilightTheme.type.micro.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
            Modifier.padding(horizontal = 9.dp),
            color = FixedColors.onImage,
            maxLines = 1,
        )
    }
}
