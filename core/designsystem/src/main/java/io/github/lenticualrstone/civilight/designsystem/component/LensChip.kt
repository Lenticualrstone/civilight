package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.animatedInsetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor

/**
 * DS-38 lens chip (`.lens`): height 32, padding 0 × 12, radius 16, `chip` role. At 최신: 1 dp `line` inset,
 * `ink2` (`최신 · 1-7`); not 최신: `acc` text and inset (`시점 · 1-5`); script position label: no outline,
 * no right padding (`이 씬 · 1-7`). Text and state belong to STO. A value change swaps the text with
 * [Motion.valueSwap] (old up 8 dp, new from below, 200 ms; the chip width follows on `standard`) and sweeps
 * an `acc` underline from the left (CNV-42); a state change crosses the outline and text colours over (S6).
 * Reduced motion: the text crossfades, no sweep, the width snaps.
 */
@Composable
fun LensChip(
    text: String,
    state: LensChipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val style = lensChipStyle(state, colors)
    // (0.2.0, S-1, CST-70, WLD-71) the small chip of the cast and world heads: 28 tall, padding 10, `small` text.
    val chipSize = LensChipSize.of(small)
    val shape = RoundedCornerShape(chipSize.radius.dp)
    val sweep = remember { Animatable(1f) }
    val previous = remember { mutableStateOf(text) }
    val outline = animatedColor(style.outline ?: style.text.copy(alpha = 0f), label = "lensOutline")
    val textColor by animatedColor(style.text, label = "lensText")
    LaunchedEffect(text) {
        if (text != previous.value) {
            previous.value = text
            if (!motion.reduced) {
                sweep.snapTo(0f)
                sweep.animateTo(1f, motion.spec(CivilightMotion.standard))
            }
        }
    }
    Box(
        modifier = modifier
            .pressable(onClick = onClick, hitTarget = HitTarget.LENS_CHIP, shape = shape)
            .heightIn(min = chipSize.height.dp)
            .clip(shape)
            .animatedInsetOutline(outline, { 1.dp }, shape)
            .drawWithContent {
                drawContent()
                if (sweep.value < 1f) {
                    val h = 1.5.dp.toPx()
                    val side = chipSize.side
                    drawRect(colors.acc, Offset(side.dp.toPx(), this.size.height - 7.dp.toPx()), Size((this.size.width - 2 * side.dp.toPx()) * sweep.value, h))
                }
            }
            .padding(start = chipSize.side.dp, end = if (style.endPadding > 0.dp) chipSize.side.dp else style.endPadding),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { motion.valueSwap() },
            label = "lens",
        ) { value ->
            CivilightText(value, if (small) CivilightTheme.type.small else CivilightTheme.type.chip, color = textColor, maxLines = 1)
        }
    }
}

/** DS-38 lens chip sizes: regular 32 tall, padding 12, radius 16; small (CST-70) 28 tall, padding 10, radius 14. */
data class LensChipSize(val height: Int, val side: Int, val radius: Int) {
    companion object {
        val REGULAR = LensChipSize(height = 32, side = 12, radius = 16)
        val SMALL = LensChipSize(height = 28, side = 10, radius = 14)

        fun of(small: Boolean): LensChipSize = if (small) SMALL else REGULAR
    }
}
