package io.github.lenticualrstone.civilight.designsystem.component

import io.github.lenticualrstone.civilight.designsystem.foundation.animatedInsetOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
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
 * Default `ink`; [selected] icon and label `acc`; [dim] (not yet revealed under the lens) 45 % opacity. Both
 * changes cross over on `standard` (S18g; 120 ms when reduced).
 */
@Composable
fun MapPin(name: String, modifier: Modifier = Modifier, selected: Boolean = false, dim: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val color by animatedColor(if (selected) colors.acc else colors.ink, label = "pinColor")
    val alpha = animatedAlpha(if (dim) MAP_DIM_ALPHA else 1f, label = "pinDim")
    val labelShape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .graphicsLayer { this.alpha = alpha.value }
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
 * touch area 44 × 44. A newly placed vertex pops .6 → 1.3 → 1 on `enter` (CNV-37 `영역 꼭짓점`, S18g) when
 * [popOnAppear]; reduced motion: it fades in instead. The fill crosses over when it stops being the last.
 */
@Composable
fun MapVertex(modifier: Modifier = Modifier, last: Boolean = false, onClick: (() -> Unit)? = null, popOnAppear: Boolean = true) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(4.dp)
    val motion = LocalMotion.current
    val fill by animatedColor(if (last) colors.acc else colors.bg, label = "vertexFill")
    val pop = remember { Animatable(if (popOnAppear) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (pop.value < 1f) pop.animateTo(1f, motion.spec(CivilightMotion.enter))
    }
    Box(
        modifier
            .graphicsLayer {
                val p = pop.value
                if (motion.reduced) {
                    alpha = p
                } else {
                    val s = vertexPopScale(p)
                    scaleX = s
                    scaleY = s
                }
            }
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, hitTarget = HitTarget.VERTEX, scaleOnPress = false) else Modifier)
            .size(14.dp)
            .clip(shape)
            .drawBehind { drawRect(fill) }
            .insetOutline(colors.acc, 2.dp, shape),
    )
}

/** Dim opacity of a not-yet-revealed pin (DS-56). */
const val MAP_DIM_ALPHA: Float = 0.45f

/** Vertex pop scale at [progress] 0…1: .6 → 1.3 (at 40 %) → 1 (CNV-37). */
fun vertexPopScale(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return if (p < VERTEX_POP_PEAK) 0.6f + (1.3f - 0.6f) * (p / VERTEX_POP_PEAK) else 1.3f - 0.3f * ((p - VERTEX_POP_PEAK) / (1f - VERTEX_POP_PEAK))
}

private const val VERTEX_POP_PEAK = 0.4f

/**
 * DS-56 route stop (`.stop`): height 20, padding 0 × 6, radius 6, 600 10 / 12 `tnum`; the [chosen]
 * character's stops `acc` fill + `bg` text; other characters' `raise` fill, `ink2`, 1 dp `ink3` inset.
 */
@Composable
fun RouteStop(label: String, chosen: Boolean, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(6.dp)
    val fill by animatedColor(if (chosen) colors.acc else colors.raise, label = "stopFill")
    val text by animatedColor(if (chosen) colors.bg else colors.ink2, label = "stopText")
    val outline = animatedColor(if (chosen) colors.ink3.copy(alpha = 0f) else colors.ink3, label = "stopOutline")
    Box(
        modifier
            .heightIn(min = 20.dp)
            .clip(shape)
            .drawBehind { drawRect(fill) }
            .animatedInsetOutline(outline, { 1.dp }, shape)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        CivilightText(
            label,
            CivilightTheme.type.badge.merge(TextStyle(fontSize = 10.sp, lineHeight = 12.sp)),
            color = text,
            maxLines = 1,
        )
    }
}

/**
 * DS-56 person chip (`.who2`): height 26, padding 0 9 0 4, radius 13, glass + 12 dp blur, 1 dp `line`
 * inset, gap 4, 600 12 / 16 `ink`; mini avatar 18.
 */
@Composable
fun PersonChip(
    name: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    image: androidx.compose.ui.graphics.painter.Painter? = null,
) {
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
            Avatar(name, size = AvatarSize.MapChip, image = image)
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
