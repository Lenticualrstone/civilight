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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SwipeReveal
import io.github.lenticualrstone.civilight.designsystem.component.SwipeSide
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.startHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlin.math.roundToInt

/** One swipe button: icon over its label (class b, accessibility name = label, SCR-30). */
class SwipeButtonSpec(
    val label: String,
    val onClick: () -> Unit,
    val labelColor: Color? = null,
    val enabled: Boolean = true,
    val icon: @Composable () -> Unit,
)

/**
 * (0.2.0, SCR-30) A block row with two swipe sides. Swiping left reveals [reference] (`◆ 시간 · 참고 · 속성`,
 * 3 × 64 dp); swiping right reveals [edit] (`고치기 · 이 뒤에 · 종류 · 씬 나누기 · 지우기`, 5 × 60 dp). A swipe only
 * reveals; nothing runs until a button is tapped. Released past half a side stays open; [edit] empty turns
 * the right swipe off (read-only, during `고치기`). Buttons appear 60 ms apart; the slide is 260 ms.
 */
@Composable
fun TwoSidedSwipeRow(
    side: SwipeSide,
    onSideChange: (SwipeSide) -> Unit,
    reference: List<SwipeButtonSpec>,
    edit: List<SwipeButtonSpec>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val motion = CivilightTheme.motion
    val colors = CivilightTheme.colors
    val density = LocalDensity.current
    val refWidth: Dp = (REF_BUTTON * reference.size).dp
    val editWidth: Dp = (EDIT_BUTTON * edit.size).dp
    val refPx = with(density) { refWidth.toPx() }
    val editPx = with(density) { editWidth.toPx() }
    val refOn = reference.isNotEmpty()
    val editOn = edit.isNotEmpty()
    val drag = remember { TwoSideDrag() }
    LaunchedEffect(side, refPx, editPx, drag.releases, drag.dragging, editOn) {
        if (drag.dragging) return@LaunchedEffect
        val wanted = if (side == SwipeSide.EDIT && !editOn) SwipeSide.CLOSED else side
        val target = SwipeReveal.restOffset(wanted, refPx, editPx)
        if (motion.reduced) {
            drag.place(target)
        } else if (drag.offset != target) {
            Animatable(drag.offset).animateTo(target, motion.spec(CivilightMotion.swipe)) { drag.place(value) }
        }
        drag.place(target)
    }
    LaunchedEffect(enabled) { if (!enabled) drag.cancel() }
    Box(modifier) {
        if (SwipeReveal.showsReference(drag.offset, refOn)) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                ButtonsRow(reference, refWidth, REF_BUTTON.dp, progress = (-drag.offset / refPx).coerceIn(0f, 1f), reduced = motion.reduced)
            }
        }
        if (SwipeReveal.showsEdit(drag.offset, editOn)) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterStart) {
                ButtonsRow(edit, editWidth, EDIT_BUTTON.dp, progress = (drag.offset / editPx).coerceIn(0f, 1f), reduced = motion.reduced)
            }
        }
        Box(
            Modifier
                .offset { IntOffset(drag.offset.roundToInt(), 0) }
                .background(colors.bg)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    state = rememberDraggableState { delta -> drag.onDelta(delta, refPx, editPx, refOn, editOn) },
                    onDragStopped = {
                        val settled = drag.release(refPx, editPx, refOn, editOn)
                        if (settled != side) onSideChange(settled)
                    },
                ),
        ) { content() }
    }
}

@Composable
private fun ButtonsRow(buttons: List<SwipeButtonSpec>, width: Dp, each: Dp, progress: Float, reduced: Boolean) {
    val colors = CivilightTheme.colors
    val density = LocalDensity.current
    Row(Modifier.width(width).fillMaxHeight().background(colors.raise)) {
        buttons.forEachIndexed { i, b ->
            val p = if (reduced) progress else ScriptMotion.buttonProgress(progress, i, reduced = false)
            Column(
                Modifier
                    .width(each)
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = if (b.enabled) p else p * DIM
                        translationX = with(density) { ScriptMotion.buttonShiftDp(p, reduced).dp.toPx() }
                    }
                    .startHairline(colors.line)
                    .pressable(onClick = b.onClick, enabled = b.enabled, hitTarget = HitTarget.BUTTON)
                    .semantics { contentDescription = b.label },
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                b.icon()
                CivilightText(b.label, CivilightTheme.type.chip.merge(TextStyle(fontSize = 12.sp)), color = b.labelColor ?: colors.ink, maxLines = 1)
            }
        }
    }
}

/** The finger state of a two-sided row (px; negative = slid left). Plain, so it is JVM-tested. */
class TwoSideDrag {
    private val offsetState = mutableFloatStateOf(0f)
    private val draggingState = mutableStateOf(false)
    private val releasesState = mutableIntStateOf(0)

    val offset: Float get() = offsetState.floatValue
    val dragging: Boolean get() = draggingState.value
    val releases: Int get() = releasesState.intValue

    fun onDelta(delta: Float, refPx: Float, editPx: Float, refOn: Boolean, editOn: Boolean) {
        draggingState.value = true
        offsetState.floatValue = SwipeReveal.clamp(offset + delta, refPx, editPx, refOn, editOn)
    }

    fun release(refPx: Float, editPx: Float, refOn: Boolean, editOn: Boolean): SwipeSide {
        draggingState.value = false
        releasesState.intValue++
        return SwipeReveal.settle(offset, refPx, editPx, refOn, editOn)
    }

    fun cancel() {
        if (!draggingState.value) return
        draggingState.value = false
        releasesState.intValue++
    }

    fun place(value: Float) {
        if (draggingState.value) return
        offsetState.floatValue = value
    }
}

private const val REF_BUTTON = 64
private const val EDIT_BUTTON = 60
private const val DIM = 0.4f
