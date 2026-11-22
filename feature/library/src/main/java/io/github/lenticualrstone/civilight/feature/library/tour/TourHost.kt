package io.github.lenticualrstone.civilight.feature.library.tour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ArrowEdge
import io.github.lenticualrstone.civilight.designsystem.component.TourDim
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler

/** One tooltip step as the card shows it. */
data class TourCardStep(val title: String, val sub: String)

/** The card's button words (copy LIB-19: `건너뛰기`, `이전`, `다음`, `마치기`). */
data class TourButtons(val skip: String, val previous: String, val next: String, val finish: String)

/** Where each step's target is, in root coordinates (filled by [tourTarget]). */
class TourTargets {
    internal val rects = mutableStateMapOf<Int, Rect>()
}

val LocalTourTargets = compositionLocalOf<TourTargets?> { null }

/** Marks this element as the hole of tour step [step] (0-based) while a [TourHost] encloses it. */
fun Modifier.tourTarget(step: Int): Modifier = composed {
    val targets = LocalTourTargets.current
    if (targets == null) this else onGloballyPositioned { targets.rects[step] = it.boundsInRoot() }
}

/**
 * The tooltip-tour overlay (LIB-19; DS-49 `TourDim` + `TourCard`): while [running], a shade covers
 * [content] with one rounded hole around the current step's target ([tourTarget]) and the card sits
 * below or above it ([TourPlacement]). The shade takes every touch, so the tour is the only thing
 * that reacts; `건너뛰기`, `마치기` and system back end it through [onDone], which marks the module
 * tour seen. A step without a placed target shows its card in the middle with no hole.
 *
 * Data-only API (no domain types), so it can move to core:ui unchanged for the other areas' tours.
 */
@Composable
fun TourHost(
    steps: List<TourCardStep>,
    label: String?,
    running: Boolean,
    buttons: TourButtons,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val targets = remember { TourTargets() }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    var hostHeight by remember { mutableStateOf(0f) }
    Box(
        modifier.onGloballyPositioned {
            hostOrigin = it.boundsInRoot().topLeft
            hostHeight = it.size.height.toFloat()
        },
    ) {
        CompositionLocalProvider(LocalTourTargets provides targets) { content() }
        if (running && steps.isNotEmpty()) {
            var index by rememberSaveable { mutableIntStateOf(0) }
            val step = steps[index.coerceIn(0, steps.size - 1)]
            val last = index >= steps.size - 1
            LocalBackHandler.current(true, onDone)
            val target = targets.rects[index]?.translate(-hostOrigin)
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
            ) {
                TourDim(target)
                TourCardAt(
                    target = target,
                    hostHeight = hostHeight,
                ) { arrow, arrowX ->
                    TourStepCard(
                        stepCount = steps.size,
                        stepIndex = index,
                        title = step.title,
                        subtitle = step.sub,
                        label = label,
                        buttons = buttons,
                        onSkip = onDone,
                        onPrevious = if (index > 0) ({ index-- }) else null,
                        onNext = { if (last) onDone() else index++ },
                        isLast = last,
                        arrowEdge = arrow,
                        arrowX = arrowX,
                    )
                }
            }
        }
    }
}

/** Measures the card, then places it with [TourPlacement]. */
@Composable
private fun BoxScope.TourCardAt(
    target: Rect?,
    hostHeight: Float,
    card: @Composable (ArrowEdge, androidx.compose.ui.unit.Dp) -> Unit,
) {
    val density = LocalDensity.current
    var cardHeight by remember { mutableStateOf(0f) }
    var cardLeft by remember { mutableStateOf(0f) }
    var cardWidth by remember { mutableStateOf(0f) }
    val placement = with(density) {
        TourPlacement.of(
            targetTop = target?.top?.minus(6.dp.toPx()),
            targetBottom = target?.bottom?.plus(6.dp.toPx()),
            targetCenterX = target?.center?.x,
            hostHeight = hostHeight,
            cardHeight = cardHeight,
            cardLeft = cardLeft,
            cardWidth = cardWidth,
            gap = 12.dp.toPx(),
            edge = 16.dp.toPx(),
            arrowInset = 24.dp.toPx(),
        )
    }
    val arrow = when (placement.arrow) {
        TourPlacement.Arrow.TOP -> ArrowEdge.TOP
        TourPlacement.Arrow.BOTTOM -> ArrowEdge.BOTTOM
        TourPlacement.Arrow.NONE -> ArrowEdge.NONE
    }
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .offset { IntOffset(0, placement.cardTop.toInt()) }
            .readingMeasure(360.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .onGloballyPositioned { cardLeft = it.positionInParentX() }
            .onSizeChanged {
                cardHeight = it.height.toFloat()
                cardWidth = it.width.toFloat()
            },
    ) {
        card(arrow, with(density) { placement.arrowX.toDp() })
    }
}

private fun androidx.compose.ui.layout.LayoutCoordinates.positionInParentX(): Float =
    parentLayoutCoordinates?.localPositionOf(this, Offset.Zero)?.x ?: 0f
