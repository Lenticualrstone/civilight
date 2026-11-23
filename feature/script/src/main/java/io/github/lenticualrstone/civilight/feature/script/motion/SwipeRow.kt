package io.github.lenticualrstone.civilight.feature.script.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.startHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.text.MarkerGlyph
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlin.math.roundToInt

/**
 * The swipe-left reveal of a block (SCR-30, CNV-40), the DS-44 look with the staggered buttons the
 * DS row cannot draw: the block follows the finger and settles at −192 dp or back over 260 ms
 * (released past half stays open); each of `시간 · 참고 · 속성` fades in from 16 dp to the right,
 * 60 ms after the one before ([ScriptMotion.buttonProgress]). Nothing runs on swipe; only the buttons
 * act. Off while typing ([enabled] false). Reduced motion: the buttons fade with the row, no slide.
 */
@Composable
fun StaggeredSwipeRow(
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onTime: () -> Unit,
    onReference: () -> Unit,
    onProperties: () -> Unit,
    timeHasChange: Boolean,
    labels: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val motion = CivilightTheme.motion
    val reduced = motion.reduced
    val colors = CivilightTheme.colors
    val width = CivilightTheme.sizes.swipeRow
    val widthPx = with(LocalDensity.current) { width.toPx() }
    val density = LocalDensity.current
    // The finger writes the offset at once (no coroutine per delta), so a release always sees the last
    // delta and no late delta can stop the settle half way (emulator QA: rows left between open and closed).
    val drag = remember { SwipeDrag(open = revealed, widthPx = widthPx) }
    drag.widthPx = widthPx
    // The one owner of the settle: after every release, and whenever [revealed] changes; a drag wins.
    LaunchedEffect(revealed, widthPx, drag.releases, drag.dragging) {
        if (drag.dragging) return@LaunchedEffect
        val target = SwipeDrag.anchor(revealed, widthPx)
        val fadeTarget = if (revealed) 1f else 0f
        if (reduced) {
            // Reduced motion: the block never slides on its own, the buttons fade on their own run (motion audit, row 7).
            drag.place(target)
            if (drag.fade != fadeTarget) {
                Animatable(drag.fade).animateTo(fadeTarget, motion.spec(CivilightMotion.swipe)) { drag.placeFade(value) }
            }
        } else {
            drag.placeFade(fadeTarget)
            if (drag.offset != target) {
                val token = ScriptMotion.settle(distanceLeft = kotlin.math.abs(target - drag.offset) / widthPx)
                Animatable(drag.offset).animateTo(target, motion.move(token)) { drag.place(value) }
            }
        }
        drag.place(target)
    }
    // Typing starts mid-drag (the row turns off): the drag ends where [revealed] says, never stuck as a drag.
    LaunchedEffect(enabled) { if (!enabled) drag.cancel() }
    val progress = { drag.progress }
    // Closed (at its actual place, not only by [revealed]), the buttons are not read by TalkBack (the row's
    // custom actions offer the same three).
    val buttonsShown by remember(reduced) { derivedStateOf { if (reduced) drag.fade > 0f else drag.progress > 0f } }
    Box(modifier) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            val hidden = if (buttonsShown) Modifier else Modifier.clearAndSetSemantics { }
            Row(Modifier.width(width).fillMaxHeight().then(hidden).graphicsLayer { alpha = if (buttonsShown) 1f else 0f }.background(colors.raise)) {
                val actions = listOf(onTime, onReference, onProperties)
                actions.forEachIndexed { i, action ->
                    SwipeButton(
                        label = labels[i],
                        onClick = action,
                        labelColor = if (i == 0 && timeHasChange) colors.acc else colors.ink,
                        modifier = Modifier.graphicsLayer {
                            val p = if (reduced) drag.fade else ScriptMotion.buttonProgress(progress(), i, reduced = false)
                            alpha = p
                            translationX = with(density) { ScriptMotion.buttonShiftDp(p, reduced).dp.toPx() }
                        },
                    ) {
                        when (i) {
                            0 -> if (timeHasChange) MarkerGlyph(MarkerKind.CHANGE, size = 11.dp)
                            1 -> CivilightIcon(IconId.Paper, null, size = 18.dp, tint = colors.ink2)
                            else -> CivilightIcon(IconId.Props, null, size = 18.dp, tint = colors.ink2)
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(drag.offset.roundToInt(), 0) }
                .background(colors.bg)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    // The buttons follow the finger under reduced motion too ([SwipeDrag.onDelta] sets the fade).
                    state = rememberDraggableState { delta -> drag.onDelta(delta) },
                    onDragStopped = {
                        // Decided and reported at once, before any animation: the settle effect then runs to it.
                        val open = drag.release()
                        if (open != revealed) onRevealedChange(open)
                    },
                ),
        ) { content() }
    }
}

/**
 * The swipe row's finger state (CNV-40), plain so it is JVM-tested: [offset] in px (0 = closed,
 * −[widthPx] = open), written synchronously by each drag delta; [release] decides open or closed
 * (past half stays open) and bumps [releases] so the settle runs even when the state did not change.
 */
class SwipeDrag(open: Boolean, var widthPx: Float) {
    private val offsetState = mutableFloatStateOf(anchor(open, widthPx))
    private val fadeState = mutableFloatStateOf(if (open) 1f else 0f)
    private val draggingState = mutableStateOf(false)
    private val releasesState = mutableIntStateOf(0)

    /** Where the block is drawn, in px. */
    val offset: Float get() = offsetState.floatValue

    /** The buttons' fade under reduced motion (follows the finger, then its own run). */
    val fade: Float get() = fadeState.floatValue

    val dragging: Boolean get() = draggingState.value
    val releases: Int get() = releasesState.intValue

    /** 0 = closed, 1 = fully open. */
    val progress: Float get() = if (widthPx <= 0f) 0f else (-offset / widthPx).coerceIn(0f, 1f)

    /** One drag delta (px, negative = left), applied at once and kept between open and closed. */
    fun onDelta(delta: Float) {
        draggingState.value = true
        offsetState.floatValue = (offset + delta).coerceIn(-widthPx, 0f)
        fadeState.floatValue = progress
    }

    /** The finger lifted: true = settle open. */
    fun release(): Boolean {
        draggingState.value = false
        releasesState.intValue++
        return opensOnRelease(offset, widthPx)
    }

    /** A drag that ends without a release (the row turned off): settles back to the open state as it was. */
    fun cancel() {
        if (!draggingState.value) return
        draggingState.value = false
        releasesState.intValue++
    }

    /** The settle animation's frame; ignored while a finger holds the row (the drag wins at once). */
    fun place(value: Float) {
        if (draggingState.value) return
        offsetState.floatValue = value.coerceIn(-widthPx, 0f)
    }

    /** The reduced-motion fade's frame; ignored while a finger holds the row. */
    fun placeFade(value: Float) {
        if (draggingState.value) return
        fadeState.floatValue = value.coerceIn(0f, 1f)
    }

    companion object {
        fun anchor(open: Boolean, widthPx: Float): Float = if (open) -widthPx else 0f

        /** Released past half the buttons' width stays open. */
        fun opensOnRelease(offset: Float, widthPx: Float): Boolean = offset < -widthPx / 2
    }
}

@Composable
private fun RowScope.SwipeButton(
    label: String,
    onClick: () -> Unit,
    labelColor: Color,
    modifier: Modifier,
    top: @Composable () -> Unit,
) {
    Column(
        modifier
            .weight(1f)
            .fillMaxHeight()
            .startHairline(CivilightTheme.colors.line)
            .pressable(onClick = onClick, hitTarget = HitTarget.BUTTON),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        top()
        CivilightText(label, CivilightTheme.type.chip.merge(TextStyle(fontSize = 12.sp)), color = labelColor, maxLines = 1)
    }
}
