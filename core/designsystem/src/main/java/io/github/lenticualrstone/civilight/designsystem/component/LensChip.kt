package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-38 lens chip (`.lens`): height 32, padding 0 × 12, radius 16, `chip` role. At 최신: 1 dp `line` inset,
 * `ink2` (`최신 · 1-7`); not 최신: `acc` text and inset (`시점 · 1-5`); script position label: no outline,
 * no right padding (`이 씬 · 1-7`). Text and state belong to STO. A value change swaps the text (old up
 * 8 dp, new from below, 200 ms) and sweeps an `acc` underline from the left (CNV-42); reduced motion fades.
 */
@Composable
fun LensChip(
    text: String,
    state: LensChipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val reduced = CivilightTheme.reducedMotion
    val style = lensChipStyle(state, colors)
    val shape = RoundedCornerShape(16.dp)
    val sweep = remember { Animatable(1f) }
    val density = LocalDensity.current
    val previous = remember { mutableStateOf(text) }
    LaunchedEffect(text) {
        if (text != previous.value) {
            previous.value = text
            if (!reduced) {
                sweep.snapTo(0f)
                sweep.animateTo(1f, CivilightMotion.spec(CivilightMotion.standard, reduced = false))
            }
        }
    }
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.LENS_CHIP, shape = shape)
            .heightIn(min = 32.dp)
            .clip(shape)
            .then(if (style.outline != null) Modifier.insetOutline(style.outline, 1.dp, shape) else Modifier)
            .drawWithContent {
                drawContent()
                if (sweep.value < 1f) {
                    val h = 1.5.dp.toPx()
                    drawRect(colors.acc, Offset(12.dp.toPx(), size.height - 7.dp.toPx()), Size((size.width - 24.dp.toPx()) * sweep.value, h))
                }
            }
            .padding(start = 12.dp, end = style.endPadding),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                if (reduced) {
                    fadeIn(CivilightMotion.spec(CivilightMotion.standard, true)) togetherWith fadeOut(CivilightMotion.spec(CivilightMotion.standard, true))
                } else {
                    val d = with(density) { 8.dp.roundToPx() }
                    (slideInVertically(CivilightMotion.spec(CivilightMotion.standard, false)) { d } + fadeIn(CivilightMotion.spec(CivilightMotion.standard, false))) togetherWith
                        (slideOutVertically(CivilightMotion.spec(CivilightMotion.standard, false)) { -d } + fadeOut(CivilightMotion.spec(CivilightMotion.standard, false)))
                }
            },
            label = "lens",
        ) { value ->
            CivilightText(value, CivilightTheme.type.chip, color = style.text, maxLines = 1)
        }
    }
}
