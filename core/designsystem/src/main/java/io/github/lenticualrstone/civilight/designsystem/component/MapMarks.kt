package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

private val Label600 = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)

/**
 * DS-56 map pin (`.mpin`): `Pin` 24 + a glass name label (padding 2 × 8, radius 10, 12 dp blur, 600 12 / 16).
 * Default `ink`; [selected] icon and label `acc`; [dim] (not yet revealed under the lens) 45 % opacity.
 */
@Composable
fun MapPin(name: String, modifier: Modifier = Modifier, selected: Boolean = false, dim: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val color = if (selected) colors.acc else colors.ink
    val labelShape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .alpha(if (dim) 0.45f else 1f)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, hitTarget = HitTarget.BUTTON) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CivilightIcon(IconId.Pin, null, size = 24.dp, tint = color)
        Box {
            GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = labelShape)
            CivilightText(name, CivilightTheme.type.chip.merge(Label600), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = color, maxLines = 1)
        }
    }
}

/**
 * DS-56 vertex (`.vx`): 14 × 14, radius 4, `bg` fill, 2 dp `acc` inset; the [last] placed vertex filled `acc`;
 * touch area 44 × 44.
 */
@Composable
fun MapVertex(modifier: Modifier = Modifier, last: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, hitTarget = HitTarget.VERTEX, scaleOnPress = false) else Modifier)
            .size(14.dp)
            .clip(shape)
            .background(if (last) colors.acc else colors.bg)
            .insetOutline(colors.acc, 2.dp, shape),
    )
}

/**
 * DS-56 route stop (`.stop`): height 20, padding 0 × 6, radius 6, 600 10 / 12 `tnum`; the [chosen]
 * character's stops `acc` fill + `bg` text; other characters' `raise` fill, `ink2`, 1 dp `ink3` inset.
 */
@Composable
fun RouteStop(label: String, chosen: Boolean, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier
            .heightIn(min = 20.dp)
            .clip(shape)
            .background(if (chosen) colors.acc else colors.raise)
            .then(if (!chosen) Modifier.insetOutline(colors.ink3, 1.dp, shape) else Modifier)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(
            label,
            CivilightTheme.type.badge.merge(TextStyle(fontSize = 10.sp, lineHeight = 12.sp)),
            color = if (chosen) colors.bg else colors.ink2,
            maxLines = 1,
        )
    }
}

/**
 * DS-56 person chip (`.who2`): height 26, padding 0 9 0 4, radius 13, glass + 12 dp blur, 1 dp `line`
 * inset, gap 4, 600 12 / 16 `ink`; mini avatar 18.
 */
@Composable
fun PersonChip(name: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(13.dp)
    Box(modifier.then(if (onClick != null) Modifier.pressable(onClick = onClick, hitTarget = HitTarget.CHIP, shape = shape) else Modifier)) {
        GlassBackground(GlassSurface.CHIP, Modifier.matchParentSize(), shape = shape)
        Row(
            Modifier
                .heightIn(min = 26.dp)
                .insetOutline(colors.line, 1.dp, shape)
                .padding(start = 4.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Avatar(name, size = AvatarSize.MapChip)
            CivilightText(name, CivilightTheme.type.chip.merge(Label600), color = colors.ink, maxLines = 1)
        }
    }
}

/**
 * DS-56 breadcrumb (`.crumb`): gap 4, 500 12 / 16 `ink3`, separators `›`, the current (last) segment 600
 * `ink` (`세계 › 하층 구역`). Earlier segments are tappable when [onSegment] is set.
 */
@Composable
fun Breadcrumb(segments: List<String>, modifier: Modifier = Modifier, onSegment: ((Int) -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        segments.forEachIndexed { i, s ->
            val current = i == segments.lastIndex
            val style = type.chip.merge(TextStyle(fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium))
            val m = if (!current && onSegment != null) Modifier.pressable(onClick = { onSegment(i) }, role = Role.Button, hitTarget = HitTarget.SMALL) else Modifier
            CivilightText(s, style, m, color = if (current) colors.ink else colors.ink3, maxLines = 1)
            if (!current) CivilightText("›", type.chip, color = colors.ink3)
        }
    }
}

/**
 * DS-56 scale bar: a 96 × 6 box with a 1.5 dp `ink2` inset, its left half filled `ink2`; [label] (`10 km`)
 * in `number` 11 sp `ink2` 3 dp below.
 */
@Composable
fun ScaleBar(label: String, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.size(96.dp, 6.dp).insetOutline(colors.ink2, 1.5.dp, RoundedCornerShape(0.dp))) {
            Box(Modifier.width(48.dp).fillMaxHeight().background(colors.ink2))
            Spacer(Modifier.weight(1f))
        }
        CivilightText(label, CivilightTheme.type.number.merge(TextStyle(fontSize = 11.sp)), color = colors.ink2, maxLines = 1)
    }
}

/** DS-56 timeline scrubber label (`.ep`): 500 10 / 12 `tnum` `ink3`; the [current] point `acc` 700. */
@Composable
fun TimelineLabel(text: String, current: Boolean, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    CivilightText(
        text,
        CivilightTheme.type.number.merge(
            TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = if (current) FontWeight.Bold else FontWeight.Medium),
        ),
        modifier,
        color = if (current) colors.acc else colors.ink3,
        maxLines = 1,
    )
}
