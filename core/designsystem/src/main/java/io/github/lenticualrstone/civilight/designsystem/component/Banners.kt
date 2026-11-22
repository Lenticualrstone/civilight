package io.github.lenticualrstone.civilight.designsystem.component

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

private val BannerTitle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)

/**
 * DS-28 past-lens edit banner (`LensEdit`): glass, radius 16, padding 10 · 12 · 10 · 14, 1.5 dp `acc` inset;
 * [title] (600 13 / 18, `1-5 시점 값 고치는 중`) over `small` [subtitle]; a compact ghost button at the right
 * (`최신으로`, 36 tall, touch 44). Place it 12 dp from the sides, 64 dp from the top. Behaviour STO.
 */
@Composable
fun LensEditBanner(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.fillMaxWidth()) {
        GlassBackground(GlassSurface.BANNER, Modifier.matchParentSize(), shape = shape)
        Row(
            Modifier
                .fillMaxWidth()
                .insetOutline(colors.acc, 1.5.dp, shape)
                .padding(start = 14.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                CivilightText(title, type.label.merge(BannerTitle), color = colors.ink)
                if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3)
            }
            CivilightButton(actionLabel, onAction, variant = ButtonVariant.COMPACT_GHOST)
        }
    }
}

/**
 * DS-28 read-only example banner (`OnbExample`): glass, radius 18, padding 10 × 14, 1 dp `line` inset;
 * step bars above a row of `Lock` 16 in `acc`, [title] (600 13 / 18) and `small` [subtitle]. Copy LIB.
 */
@Composable
fun ReadOnlyBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    stepCount: Int = 0,
    currentStep: Int = 0,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.fillMaxWidth()) {
        GlassBackground(GlassSurface.BANNER, Modifier.matchParentSize(), shape = shape)
        Column(
            Modifier
                .fillMaxWidth()
                .insetOutline(colors.line, 1.dp, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (stepCount > 0) StepBars(stepCount, currentStep)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightIcon(IconId.Lock, contentDescription = null, size = 16.dp, tint = colors.acc)
                CivilightText(title, type.label.merge(BannerTitle), Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 1)
                if (subtitle != null) CivilightText(subtitle, type.small, color = colors.ink3, maxLines = 1)
            }
        }
    }
}

/**
 * DS-28 image fade into the page (profile header art): [image] drawn full width, [height] tall (430 on
 * cover); a backdrop blur of `blur-edge` 22 dp masked from transparent at 25 % to opaque at 60 % of the
 * height (API 31+), and a gradient from transparent at [fadeStart] to `bg` at 85 % of the remaining height.
 * No glass bar on top; back and lens float as glass round buttons. Decorative for accessibility.
 */
@Composable
fun FadingHeaderArt(
    modifier: Modifier = Modifier,
    height: Dp = 430.dp,
    fadeStart: Dp = 230.dp,
    image: @Composable BoxScope.() -> Unit,
) {
    val bg = CivilightTheme.colors.bg
    val edgeBlur = CivilightTheme.blur.edge
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics { }
            .drawWithContent {
                drawContent()
                val start = (fadeStart / height).coerceIn(0f, 1f)
                val end = start + (1f - start) * 0.85f
                drawRect(Brush.verticalGradient(0f to Color.Transparent, start to Color.Transparent, end to bg, 1f to bg))
            },
    ) {
        Box(Modifier.matchParentSize(), content = image)
        if (Build.VERSION.SDK_INT >= 31) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(0f to Color.Transparent, 0.25f to Color.Transparent, 0.6f to Color.Black, 1f to Color.Black),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val r = edgeBlur.toPx()
                            renderEffect = BlurEffect(r, r, TileMode.Clamp)
                        },
                    content = image,
                )
            }
        }
    }
}
