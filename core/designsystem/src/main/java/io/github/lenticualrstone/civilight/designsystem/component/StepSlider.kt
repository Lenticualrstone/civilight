package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlin.math.roundToInt

/** The step nearest to [fraction] (0…1) for a slider of [stepCount] steps. */
fun sliderStepFor(fraction: Float, stepCount: Int): Int =
    if (stepCount <= 1) 0 else (fraction.coerceIn(0f, 1f) * (stepCount - 1)).roundToInt()

/**
 * DS-34 step slider (`글자 크기`): track 4 dp, radius 2, `line`, filled part `acc`; thumb an 18 × 18 rounded
 * square (radius 5) in `ink`, never a circle; a small `가` (13 sp) at the start and a large `가` (22 sp)
 * at the end, 12 dp from the track. Steps snap with `Standard`; no tick marks. Touch height 44.
 */
@Composable
fun StepSlider(
    value: Int,
    stepCount: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    smallLabel: String = "가",
    largeLabel: String = "가",
    contentDescription: String? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val reduced = CivilightTheme.reducedMotion
    val target = if (stepCount <= 1) 0f else value.coerceIn(0, stepCount - 1).toFloat() / (stepCount - 1)
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val shown by animateFloatAsState(
        if (dragFraction >= 0f) dragFraction else target,
        CivilightMotion.spec(CivilightMotion.standard, reduced),
        label = "slider",
    )
    val currentValue by rememberUpdatedState(value)
    val onChange by rememberUpdatedState(onValueChange)
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CivilightText(smallLabel, type.label.merge(TextStyle(fontSize = 13.sp, lineHeight = 16.sp)), color = colors.ink2)
        Canvas(
            Modifier
                .weight(1f)
                .height(44.dp)
                .semantics {
                    if (contentDescription != null) this.contentDescription = contentDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..(stepCount - 1).coerceAtLeast(0).toFloat(), (stepCount - 2).coerceAtLeast(0))
                    setProgress { v ->
                        val step = v.roundToInt().coerceIn(0, stepCount - 1)
                        if (step != currentValue) onChange(step)
                        true
                    }
                }
                .pointerInput(stepCount) {
                    detectTapGestures { pos ->
                        val step = sliderStepFor(pos.x / size.width, stepCount)
                        if (step != currentValue) onChange(step)
                    }
                }
                .pointerInput(stepCount) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragFraction = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            val step = sliderStepFor(dragFraction, stepCount)
                            dragFraction = -1f
                            if (step != currentValue) onChange(step)
                        },
                        onDragCancel = { dragFraction = -1f },
                    ) { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val step = sliderStepFor(dragFraction, stepCount)
                        if (step != currentValue) onChange(step)
                    }
                },
        ) {
            val thumb = 18.dp.toPx()
            val track = 4.dp.toPx()
            val usable = size.width - thumb
            val x = thumb / 2 + usable * shown
            val cy = size.height / 2
            drawRoundRect(colors.line, Offset(thumb / 2, cy - track / 2), Size(usable, track), CornerRadius(track / 2))
            drawRoundRect(colors.acc, Offset(thumb / 2, cy - track / 2), Size(x - thumb / 2, track), CornerRadius(track / 2))
            drawRoundRect(colors.ink, Offset(x - thumb / 2, cy - thumb / 2), Size(thumb, thumb), CornerRadius(5.dp.toPx()))
        }
        CivilightText(largeLabel, type.label.merge(TextStyle(fontSize = 22.sp, lineHeight = 26.sp)), color = colors.ink2)
    }
}
