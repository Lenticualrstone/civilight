package io.github.lenticualrstone.civilight.feature.library.tour

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.AnimatedVisibility
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ArrowEdge
import io.github.lenticualrstone.civilight.designsystem.component.TourDim
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.ui.tour.TourStepFilter
import io.github.lenticualrstone.civilight.ui.tour.tourTarget as sharedTourTarget

/** One tooltip step as the card shows it. */
data class TourCardStep(val title: String, val sub: String)

/** The card's button words (copy LIB-19: `건너뛰기`, `이전`, `다음`, `마치기`). */
data class TourButtons(val skip: String, val previous: String, val next: String, val finish: String)

/** Where each step's target is (moved to `core:ui` so every feature can mark targets). */
typealias TourTargets = io.github.lenticualrstone.civilight.ui.tour.TourTargets

val LocalTourTargets: ProvidableCompositionLocal<TourTargets?>
    get() = io.github.lenticualrstone.civilight.ui.tour.LocalTourTargets

/**
 * Marks this element as the hole of tour step [step] (0-based) while a [TourHost] encloses it; with
 * [fallback], only while the step's own target is missing or off screen.
 */
fun Modifier.tourTarget(step: Int, fallback: Boolean = false): Modifier = sharedTourTarget(step, fallback)

/**
 * The tooltip-tour overlay (LIB-19; DS-49 `TourDim` + `TourCard`): while [running], a shade covers
 * [content] with one rounded hole around the current step's target ([tourTarget]) and the card sits
 * below or above it ([TourPlacement]). The shade takes every touch, so the tour is the only thing
 * that reacts; `건너뛰기`, `마치기` and system back end it through [onDone], which marks the module
 * tour seen. A step whose own target is missing or off screen takes its fallback target
 * (`tourTarget(step, fallback = true)`); a step with neither shows its card in the middle with no hole
 * (content should not leave a step so: mark a fallback or declare it absent). A step the content
 * declared absent (`TourStepAbsent`) is skipped and not counted.
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
    val motion = CivilightTheme.motion
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    var hostHeight by remember { mutableStateOf(0f) }
    var hostWidth by remember { mutableStateOf(0f) }
    // Steps the content declared absent (`TourStepAbsent`) are left out; the rest keep their target ids.
    val shown = TourStepFilter.shown(steps.size, targets.absent.keys)
    // A content still loading holds the tour back (`TourHold`), so its steps are known before a card shows.
    val showing = running && shown.isNotEmpty() && targets.holds == 0
    // The content learns that its tour is showing (a script being typed leaves typing, so its command row is the target).
    SideEffect { targets.running = showing }
    // The keyboard would cover the targets at the bottom (emulator QA: `명령 칸` under the keyboard).
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(showing) {
        if (showing) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    Box(
        modifier.onGloballyPositioned {
            hostOrigin = it.boundsInRoot().topLeft
            hostHeight = it.size.height.toFloat()
            hostWidth = it.size.width.toFloat()
        },
    ) {
        CompositionLocalProvider(LocalTourTargets provides targets) { content() }
        // The tour fades in and out as a whole (audit row 11); a closed tour is not composed at all.
        AnimatedVisibility(showing, Modifier.matchParentSize(), enter = motion.enter(), exit = motion.exit()) {
            var index by rememberSaveable { mutableIntStateOf(0) }
            // The last steps stay drawn while the tour fades out; an empty list (never while running) draws nothing.
            if (shown.isEmpty()) return@AnimatedVisibility
            val at = index.coerceIn(0, shown.size - 1)
            val stepId = shown[at]
            val step = steps[stepId]
            val last = at >= shown.size - 1
            LocalBackHandler.current(true, onDone)
            // A target scrolled away or clipped (empty or outside rect) is not used (TourPlacement.usableTarget):
            // the step's fallback target takes the hole, else there is none.
            fun inHost(rect: Rect?) = rect?.translate(-hostOrigin)?.let { floatArrayOf(it.left, it.top, it.right, it.bottom) }
            val target = TourPlacement.pickTarget(inHost(targets.rects[stepId]), inHost(targets.fallbacks[stepId]), hostWidth, hostHeight)
                ?.let { Rect(it[0], it[1], it[2], it[3]) }
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
            ) {
                TourDim(target)
                TourCardAt(
                    target = target,
                    hostHeight = hostHeight,
                    hostWidth = hostWidth,
                ) { arrow, arrowX ->
                    TourStepCard(
                        stepCount = shown.size,
                        stepIndex = at,
                        title = step.title,
                        subtitle = step.sub,
                        label = label,
                        buttons = buttons,
                        onSkip = onDone,
                        onPrevious = if (at > 0) ({ index = at - 1 }) else null,
                        onNext = { if (last) onDone() else index = at + 1 },
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
    hostWidth: Float,
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
            hostWidth = hostWidth,
        )
    }
    val arrow = when (placement.arrow) {
        TourPlacement.Arrow.TOP -> ArrowEdge.TOP
        TourPlacement.Arrow.BOTTOM -> ArrowEdge.BOTTOM
        TourPlacement.Arrow.NONE -> ArrowEdge.NONE
    }
    // The card glides to its next place on `standard` (snaps when reduced and until it is first measured).
    val motion = CivilightTheme.motion
    val cardOffset by animateIntOffsetAsState(
        // Sideways too on the inner screen, under the target (`cardShift`, 0 at cover width).
        IntOffset(placement.cardShift.toInt(), placement.cardTop.toInt()),
        if (cardHeight > 0f) motion.move(CivilightMotion.standard) else snap(),
        label = "tourCard",
    )
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .offset { cardOffset }
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
