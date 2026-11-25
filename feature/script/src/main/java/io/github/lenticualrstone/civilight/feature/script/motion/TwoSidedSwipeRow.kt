package io.github.lenticualrstone.civilight.feature.script.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clipToBounds
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
import io.github.lenticualrstone.civilight.designsystem.component.SwipeSide
import io.github.lenticualrstone.civilight.designsystem.foundation.GestureRules
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.horizontalSwipe
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
 * (0.2.0, SCR-30; sides swapped in 0.2.4) A block row with two swipe sides. Swiping **left** reveals [edit]
 * (`고치기 · 이 뒤에 · 종류 · 씬 나누기 · 지우기`, 5 × 60 dp) at the row's **right** end, under the thumb, since those
 * are the buttons used while writing; swiping **right** reveals [reference] (`◆ 시간 · 참고 · 속성`, 3 × 64 dp) at
 * the left end ([BlockSwipe]). A swipe only reveals; nothing runs until a button is pressed. Released past
 * half a side stays open; [edit] empty turns the left swipe off (read-only, during `고치기`). Buttons come in
 * staggered over the 260 ms slide and are all fully opaque once the row is open.
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
        val wanted = BlockSwipe.wanted(side, refOn, editOn)
        val target = BlockSwipe.restOffset(wanted, refPx, editPx)
        if (motion.reduced) {
            drag.place(target)
        } else if (drag.offset != target) {
            Animatable(drag.offset).animateTo(target, motion.spec(CivilightMotion.swipe)) { drag.place(value) }
        }
        drag.place(target)
    }
    LaunchedEffect(enabled) { if (!enabled) drag.cancel() }
    Box(modifier.clipToBounds()) {
        // Only the side being revealed is composed: the two layers overlap on a cover screen (192 + 300 > 390).
        if (BlockSwipe.showsReference(drag.offset, refOn)) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterStart) {
                ButtonsRow(reference, refWidth, REF_BUTTON.dp, progress = BlockSwipe.referenceProgress(drag.offset, refPx), reduced = motion.reduced, atStart = true)
            }
        }
        if (BlockSwipe.showsEdit(drag.offset, editOn)) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                ButtonsRow(edit, editWidth, EDIT_BUTTON.dp, progress = BlockSwipe.editProgress(drag.offset, editPx), reduced = motion.reduced, atStart = false)
            }
        }
        Box(
            Modifier
                // 0.2.3 gesture rules (core `GestureRules`): horizontalSwipe runs before offset so pointer
                // deltas stay in stationary coordinates instead of oscillating with the sliding child.
                .horizontalSwipe(
                    enabled = enabled,
                    canStart = { dx -> drag.canStart(dx, refPx, editPx, refOn, editOn) },
                    onDelta = { delta -> drag.onDelta(delta, refPx, editPx, refOn, editOn) },
                    onStop = {
                        val settled = drag.release(refPx, editPx, refOn, editOn)
                        if (settled != side) onSideChange(settled)
                    },
                )
                .offset { IntOffset(drag.offset.roundToInt(), 0) }
                .background(colors.bg),
        ) { content() }
    }
}

@Composable
private fun ButtonsRow(buttons: List<SwipeButtonSpec>, width: Dp, each: Dp, progress: Float, reduced: Boolean, atStart: Boolean) {
    val colors = CivilightTheme.colors
    val density = LocalDensity.current
    Row(Modifier.width(width).fillMaxHeight().background(colors.raise)) {
        buttons.forEachIndexed { i, b ->
            // The button nearest the block comes in first; the outer edge's button last.
            val order = if (atStart) buttons.size - 1 - i else i
            val p = ScriptMotion.buttonProgress(progress, order, reduced, count = buttons.size)
            Column(
                Modifier
                    .width(each)
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = BlockSwipe.buttonAlpha(p, b.enabled)
                        translationX = with(density) { ScriptMotion.buttonShiftDp(p, reduced, fromStart = atStart).dp.toPx() }
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

/**
 * (0.2.4) Pure side rules of the script's two-sided block row (JVM-tested). Offsets are px, negative = slid
 * left. The edit side opens with a **left** swipe (the block slides left, the buttons sit at the right end,
 * under the thumb); the reference side with a **right** swipe (buttons at the left end). The core
 * [io.github.lenticualrstone.civilight.designsystem.component.SwipeReveal] keeps the 0.2.0 orientation for its
 * own row; this row follows these rules instead.
 */
object BlockSwipe {
    /** The side the row may rest on: a side without buttons closes it. */
    fun wanted(side: SwipeSide, refOn: Boolean, editOn: Boolean): SwipeSide = when (side) {
        SwipeSide.EDIT -> if (editOn) side else SwipeSide.CLOSED
        SwipeSide.REFERENCE -> if (refOn) side else SwipeSide.CLOSED
        SwipeSide.CLOSED -> side
    }

    /** Where the block rests for [side]. */
    fun restOffset(side: SwipeSide, refPx: Float, editPx: Float): Float = when (side) {
        SwipeSide.CLOSED -> 0f
        SwipeSide.EDIT -> -editPx
        SwipeSide.REFERENCE -> refPx
    }

    /** The farthest left the block may go (edit side open), px. */
    fun min(editPx: Float, editOn: Boolean): Float = if (editOn) -editPx else 0f

    /** The farthest right the block may go (reference side open), px. */
    fun max(refPx: Float, refOn: Boolean): Float = if (refOn) refPx else 0f

    /** A drag's offset kept inside the sides that may open. */
    fun clamp(offset: Float, refPx: Float, editPx: Float, refOn: Boolean, editOn: Boolean): Float =
        offset.coerceIn(min(editPx, editOn), max(refPx, refOn))

    /** On release: past half a side's width the row stays open on that side; otherwise it closes. */
    fun settle(offset: Float, refPx: Float, editPx: Float, refOn: Boolean, editOn: Boolean): SwipeSide = when {
        editOn && offset < -editPx / 2f -> SwipeSide.EDIT
        refOn && offset > refPx / 2f -> SwipeSide.REFERENCE
        else -> SwipeSide.CLOSED
    }

    /** Whether the edit buttons (right end) are composed: only while the block sits left of rest. */
    fun showsEdit(offset: Float, editOn: Boolean): Boolean = editOn && offset < -EDGE_PX

    /** Whether `◆ 시간 · 참고 · 속성` (left end) are composed: only while the block sits right of rest. */
    fun showsReference(offset: Float, refOn: Boolean): Boolean = refOn && offset > EDGE_PX

    /** How far open (0…1) the edit side is. */
    fun editProgress(offset: Float, editPx: Float): Float = if (editPx <= 0f) 0f else (-offset / editPx).coerceIn(0f, 1f)

    /** How far open (0…1) the reference side is. */
    fun referenceProgress(offset: Float, refPx: Float): Float = if (refPx <= 0f) 0f else (offset / refPx).coerceIn(0f, 1f)

    /** A button's alpha at its own progress [p]: fully opaque once in; a disabled button stays dimmed. */
    fun buttonAlpha(p: Float, enabled: Boolean): Float = if (enabled) p else p * DIM

    private const val EDGE_PX = 0.5f
}

/** The finger state of a two-sided row (px; negative = slid left). Plain, so it is JVM-tested. */
class TwoSideDrag {
    private val offsetState = mutableFloatStateOf(0f)
    private val draggingState = mutableStateOf(false)
    private val releasesState = mutableIntStateOf(0)

    val offset: Float get() = offsetState.floatValue
    val dragging: Boolean get() = draggingState.value
    val releases: Int get() = releasesState.intValue

    /**
     * Whether a drag starting toward [dx]'s sign can move this row (0.2.3 `GestureRules.canMove`): left down to the
     * edit side, right up to the reference side, each only when that side has buttons.
     */
    fun canStart(dx: Float, refPx: Float, editPx: Float, refOn: Boolean, editOn: Boolean): Boolean =
        GestureRules.canMove(dx, offset, min = BlockSwipe.min(editPx, editOn), max = BlockSwipe.max(refPx, refOn))

    fun onDelta(delta: Float, refPx: Float, editPx: Float, refOn: Boolean, editOn: Boolean) {
        draggingState.value = true
        offsetState.floatValue = BlockSwipe.clamp(offset + delta, refPx, editPx, refOn, editOn)
    }

    fun release(refPx: Float, editPx: Float, refOn: Boolean, editOn: Boolean): SwipeSide {
        draggingState.value = false
        releasesState.intValue++
        return BlockSwipe.settle(offset, refPx, editPx, refOn, editOn)
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
