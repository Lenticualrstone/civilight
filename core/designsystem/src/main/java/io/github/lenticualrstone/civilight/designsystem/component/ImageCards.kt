package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * DS-51 image card (`.cv`): radius 20, clipped; [image] fills it; a bottom shade over the lower 62 %;
 * text 16 dp from the sides, 14 dp from the bottom in on-image colours: rating badge, title 800 22 / 28
 * (16 / 21 single line when [compactTitle]), meta 400 12 / 17. Optional status chip at 12 / 12 and a `⋯`
 * button at the top-right (44, icon 18 `onImage`). Sizes and content are CNV-22 / CNV-23.
 */
@Composable
fun ImageCard(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    rating: Rating? = null,
    statusChip: String? = null,
    more: IconAction? = null,
    onClick: (() -> Unit)? = null,
    compactTitle: Boolean = false,
    image: @Composable BoxScope.() -> Unit,
) {
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(PressAction.Click(onClick), role = Role.Button, shape = shape) else Modifier)
            .clip(shape),
    ) {
        Box(Modifier.matchParentSize().clearAndSetSemantics { }, content = image)
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val top = size.height * (1f - FixedColors.IMAGE_SHADE_FRACTION)
                    drawRect(
                        Brush.verticalGradient(listOf(FixedColors.imageShadeTop, FixedColors.imageShadeBottom), startY = top, endY = size.height),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, size.height - top),
                    )
                },
        )
        if (statusChip != null) ImageStatusChip(statusChip, Modifier.align(Alignment.TopStart).padding(12.dp))
        if (more != null) {
            CivilightIconButton(IconId.More, more.label, more.onClick, Modifier.align(Alignment.TopEnd), tint = FixedColors.onImage, iconSize = 18.dp)
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (rating != null) RatingBadge(rating, surface = BadgeSurface.ON_IMAGE)
            CivilightText(
                title,
                if (compactTitle) type.title.merge(TextStyle(fontSize = 16.sp, lineHeight = 21.sp)) else type.title,
                color = FixedColors.onImage,
                maxLines = if (compactTitle) 1 else 2,
            )
            if (meta != null) CivilightText(meta, type.small, color = FixedColors.onImage2, maxLines = 1)
        }
    }
}

/**
 * Abstract angular-shard art: a 165° gradient and three polygon layers in [palette] (at least 2 colours).
 * Placeholder art for image slots and previews; the 8 preset palettes are CNV-26.
 */
@Composable
fun ShardArt(palette: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().clearAndSetSemantics { }) {
        val c = if (palette.size >= 2) palette else listOf(Color.DarkGray, Color.Gray)
        val angle = Math.toRadians(165.0 - 90.0)
        val dx = (cos(angle) * size.width / 2).toFloat()
        val dy = (sin(angle) * size.height / 2).toFloat()
        val center = Offset(size.width / 2, size.height / 2)
        drawRect(Brush.linearGradient(listOf(c[0], c[1]), start = center - Offset(dx, dy), end = center + Offset(dx, dy)))
        val w = size.width
        val h = size.height
        val layers = listOf(
            listOf(0f to 0.55f, 0.6f to 0.3f, 1f to 0.48f, 1f to 1f, 0f to 1f),
            listOf(0.15f to 1f, 0.5f to 0.42f, 0.95f to 0.7f, 0.8f to 1f),
            listOf(0.35f to 0f, 0.75f to 0f, 0.55f to 0.38f),
        )
        layers.forEachIndexed { i, pts ->
            val color = c[(i + 2) % c.size].copy(alpha = 0.55f - i * 0.12f)
            val path = Path().apply {
                pts.forEachIndexed { j, (x, y) -> if (j == 0) moveTo(x * w, y * h) else lineTo(x * w, y * h) }
                close()
            }
            drawPath(path, color)
        }
    }
}

/**
 * DS-52 image tile: the image in its slot ratio (presets 86 × 114), radius 12; [name] below in `small` (6 dp
 * gap) `ink3`. Selected: a double ring (2 dp `bg` gap, then 2 dp `acc`) and the name in `ink`.
 * The slot is backed by the opaque [imageTileBase], so a dimmed (translucent) or still-loading [image]
 * never shows what lies behind the tile — on a glass sheet that is the blurred, saturated back screen,
 * which tinted dimmed tiles purple / yellow over colourful galleries.
 */
@Composable
fun ImageTile(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 86f / 114f,
    image: @Composable BoxScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val ring = animatedFraction(selected, label = "tileRing")
    val nameColor by animatedColor(if (selected) colors.ink else colors.ink3, label = "tileName")
    Column(
        modifier.pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, shape = shape),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .drawWithContent {
                    drawContent()
                    val a = ring.value
                    if (a > 0f) {
                        val g = 2.dp.toPx()
                        val r = 12.dp.toPx()
                        drawRoundRect(colors.bg, Offset(-g / 2, -g / 2), Size(size.width + g, size.height + g), CornerRadius(r + g / 2), style = Stroke(g), alpha = a)
                        drawRoundRect(colors.acc, Offset(-g * 1.5f, -g * 1.5f), Size(size.width + g * 3, size.height + g * 3), CornerRadius(r + g * 1.5f), style = Stroke(g), alpha = a)
                    }
                },
        ) {
            Box(Modifier.matchParentSize().clip(shape).background(imageTileBase(colors)), content = image)
        }
        CivilightText(name, CivilightTheme.type.small, color = nameColor, maxLines = 1)
    }
}

/**
 * The opaque backing of an [ImageTile] slot: `raise`, which both themes declare fully opaque. A tile's
 * image may be translucent (the add sheet dims tiles it cannot choose to 38 %), and tiles sit on glass.
 */
fun imageTileBase(colors: CivilightColors): Color = colors.raise.copy(alpha = 1f)

/**
 * DS-53 crop frame overlay for a stage (corners 12): outside dimmed `cropDim`, a 1.5 dp `cropOutline` frame
 * of [ratio] (width ÷ height) inset by [margin], and rule-of-thirds lines 1 dp `cropGrid`. Tools and
 * behaviour: CNV-28.
 */
@Composable
fun CropFrame(ratio: Float, modifier: Modifier = Modifier, margin: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).clearAndSetSemantics { }) {
        val m = margin.toPx()
        val availW = size.width - m * 2
        val availH = size.height - m * 2
        val (fw, fh) = if (availW / availH > ratio) (availH * ratio) to availH else availW to (availW / ratio)
        val left = (size.width - fw) / 2
        val top = (size.height - fh) / 2
        val dim = FixedColors.cropDim
        drawRect(dim, Offset.Zero, Size(size.width, top))
        drawRect(dim, Offset(0f, top + fh), Size(size.width, size.height - top - fh))
        drawRect(dim, Offset(0f, top), Size(left, fh))
        drawRect(dim, Offset(left + fw, top), Size(size.width - left - fw, fh))
        val grid = 1.dp.toPx()
        for (i in 1..2) {
            val x = left + fw * i / 3f
            val y = top + fh * i / 3f
            drawLine(FixedColors.cropGrid, Offset(x, top), Offset(x, top + fh), grid)
            drawLine(FixedColors.cropGrid, Offset(left, y), Offset(left + fw, y), grid)
        }
        val o = 1.5.dp.toPx()
        drawRect(FixedColors.cropOutline, Offset(left + o / 2, top + o / 2), Size(fw - o, fh - o), style = Stroke(o))
    }
}

/** Ratio chips under the crop stage (`카드 3:4` · `목록 1:1`) with a `small` note (`규격 고정`). */
@Composable
fun CropRatioRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, l -> FilterChip(l, i == selectedIndex, { onSelect(i) }) }
        if (note != null) {
            Box(Modifier.weight(1f))
            CivilightText(note, CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
        }
    }
}
