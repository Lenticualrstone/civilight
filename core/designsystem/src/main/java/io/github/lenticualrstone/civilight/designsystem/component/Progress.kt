package io.github.lenticualrstone.civilight.designsystem.component

import io.github.lenticualrstone.civilight.designsystem.foundation.animatedInsetOutline
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.animateDpAsState
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** Segment state of a step bar. */
enum class StepState { DONE, CURRENT, UPCOMING }

fun stepState(index: Int, current: Int): StepState = when {
    index < current -> StepState.DONE
    index == current -> StepState.CURRENT
    else -> StepState.UPCOMING
}

/** DS-48 step bars: N equal segments, gap 4, height 3, radius 2; current `acc`, done `ink2`, upcoming `line`. */
@Composable
fun StepBars(count: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo((current + 1).toFloat(), 0f..count.toFloat(), count) },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(count) { i ->
            val c = animatedColor(
                when (stepState(i, current)) {
                    StepState.CURRENT -> colors.acc
                    StepState.DONE -> colors.ink2
                    StepState.UPCOMING -> colors.line
                },
                label = "stepBar",
            )
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).drawBehind { drawRect(c.value) })
        }
    }
}

/** Step bars with `건너뛰기` at the right (onboarding / new project top row). */
@Composable
fun StepBarsRow(count: Int, current: Int, skipLabel: String?, onSkip: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepBars(count, current, Modifier.weight(1f))
        if (skipLabel != null && onSkip != null) TextAction(skipLabel, onSkip)
    }
}

/** DS-48 tour progress: N bars 16 × 3, gap 3, radius 2; current `acc`, others `line`; `<label> · n/N` 11 sp. */
@Composable
fun TourProgress(count: Int, current: Int, label: String?, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(count) { i ->
                val c = animatedColor(if (i == current) colors.acc else colors.line, label = "tourBar")
                Box(Modifier.size(16.dp, 3.dp).clip(RoundedCornerShape(2.dp)).drawBehind { drawRect(c.value) })
            }
        }
        val text = if (label != null) "$label · ${current + 1}/$count" else "${current + 1}/$count"
        CivilightText(text, CivilightTheme.type.numberHeader.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3, maxLines = 1)
    }
}

/**
 * DS-48 pager marks (card rows): current `acc` 18 × 4, others `line` 6 × 4, gap 4, radius 2. The current
 * mark's width moves on `standard` and the colours cross over (S6); reduced motion: widths snap.
 */
@Composable
fun PagerMarks(count: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { i ->
            val on = i == current
            val width by animateDpAsState(if (on) 18.dp else 6.dp, motion.move(CivilightMotion.standard), label = "pagerWidth")
            val c = animatedColor(if (on) colors.acc else colors.line, label = "pagerColor")
            Box(Modifier.size(width, 4.dp).clip(RoundedCornerShape(2.dp)).drawBehind { drawRect(c.value) })
        }
    }
}

/**
 * DS-48 progress bar (restore, export): track 6 tall, radius 3, `line`; fill `acc`; below: `number` 12 sp
 * [valueText] (`218 / 340 MB`) left and `small` [estimateText] (`약 40초`) right.
 */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    estimateText: String? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val animated = animateFloatAsState(fraction.coerceIn(0f, 1f), LocalMotion.current.move(CivilightMotion.standard), label = "progress")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.line)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f) },
        ) {
            Box(
                Modifier.matchParentSize().drawBehind {
                    val w = size.width * animated.value
                    if (w > 0f) drawRoundRect(colors.acc, size = Size(w, size.height), cornerRadius = CornerRadius(size.height / 2))
                },
            )
        }
        if (valueText != null || estimateText != null) {
            Row(Modifier.fillMaxWidth()) {
                if (valueText != null) CivilightText(valueText, type.number, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
                else Box(Modifier.weight(1f))
                if (estimateText != null) CivilightText(estimateText, type.small, color = colors.ink3, maxLines = 1)
            }
        }
    }
}

/**
 * DS-48 step list (restore steps): 20 × 20 radius 6 markers — done `acc` fill; current 2 dp `acc` inset;
 * upcoming 1.5 dp `ink3` inset and `ink3` text.
 */
@Composable
fun StepList(steps: List<String>, current: Int, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(6.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { i, label ->
            val state = stepState(i, current)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val done = animatedFraction(state == StepState.DONE, label = "stepDone")
                val ring = animatedColor(
                    when (state) {
                        StepState.DONE -> colors.acc.copy(alpha = 0f)
                        StepState.CURRENT -> colors.acc
                        StepState.UPCOMING -> colors.ink3
                    },
                    label = "stepRing",
                )
                val ringWidth = animatedAlpha(if (state == StepState.CURRENT) 2f else 1.5f, label = "stepRingWidth")
                val text by animatedColor(if (state == StepState.UPCOMING) colors.ink3 else colors.ink, label = "stepText")
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(shape)
                        .drawBehind { val a = done.value; if (a > 0f) drawRect(colors.acc, alpha = a) }
                        .animatedInsetOutline(ring, { ringWidth.value.dp }, shape),
                )
                CivilightText(label, type.command, color = text)
            }
        }
    }
}
