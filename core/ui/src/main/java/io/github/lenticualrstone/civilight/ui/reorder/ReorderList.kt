package io.github.lenticualrstone.civilight.ui.reorder

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.foundation.GestureRules
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.tokenShadow
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics

/** Words of the shared reorder rows (the caller's copy object): `위로 옮기기 · 아래로 옮기기` and the grip's name. */
data class ReorderLabels(val moveUp: String, val moveDown: String, val grip: String)

/**
 * What a row of [ReorderList] gets: whether it is picked, whether the mode is on, the [press] modifier for the
 * row's own area (long press lifts it; in the mode a tap picks it) and the [Handle] to place at its start.
 */
@Stable
class ReorderRowScope internal constructor(
    val selected: Boolean,
    val ordering: Boolean,
    val press: Modifier,
    private val handle: @Composable () -> Unit,
) {
    /** The grip (⠿, 44 dp): shown on the picked row only; dragging it moves the row at once. */
    @Composable
    fun Handle() = handle()
}

/**
 * One reorderable list (board `ImmPlaceRules` `순서 바꾸기`), shared by every screen that orders rows by hand:
 *
 * - `순서 바꾸기` ([ReorderState.toggle]) or a long press on a row's [ReorderRowScope.press] area enter the same
 *   state; a long press picks that row and keeps following the finger, so press-and-drag is one gesture.
 * - Only the picked row shows the grip and floats (lift scale `liftScale`, `shadow-float`, `raise`); the place it
 *   would drop on shows a 2 dp `acc` line; `틱` on lift and on each new place, `확인` on a drop that moves.
 * - Moves stay inside this list; [onMove] gets the key and its new index (one undo step for the caller).
 * - Vertical only, and never against the scroll: a long press is cancelled once the finger passes the slop
 *   ([GestureRules.cancelsLongPress]); after it fires the finger is the row's ([PointerEventPass.Initial], so the
 *   row's own buttons do not also fire). The grip drags at once (a child, so it wins over the scroll).
 * - Accessibility: `위로 옮기기 · 아래로 옮기기` on every row while [enabled].
 * - Reduced motion: no scale, the shadow and colour fade only.
 */
@Composable
fun <K : Any> ReorderList(
    state: ReorderState,
    list: String,
    keys: List<K>,
    labels: ReorderLabels,
    enabled: Boolean,
    onMove: (key: K, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable ReorderRowScope.(key: K, index: Int) -> Unit,
) {
    SideEffect { state.keepShown(list, keys) }
    Column(modifier) {
        keys.forEachIndexed { index, k ->
            key(k) { ReorderItem(state, list, keys, k, index, labels, enabled, onMove, row) }
        }
    }
}

@Composable
private fun <K : Any> ReorderItem(
    state: ReorderState,
    list: String,
    keys: List<K>,
    k: K,
    index: Int,
    labels: ReorderLabels,
    enabled: Boolean,
    onMove: (key: K, toIndex: Int) -> Unit,
    row: @Composable ReorderRowScope.(key: K, index: Int) -> Unit,
) {
    val colors = CivilightTheme.colors
    val elevation = CivilightTheme.elevation
    val reduced = LocalMotion.current.reduced
    val haptics = rememberHaptics()
    val rk = ReorderKey(list, k)
    val selected = state.selected == rk
    val listDragging = state.dragging && state.selected?.list == list
    val lift = animatedFraction(selected, CivilightMotion.standard, label = "reorderLift")
    val currentIndex by rememberUpdatedState(index)
    val currentKeys by rememberUpdatedState(keys)
    val move by rememberUpdatedState(onMove)
    val shape = RoundedCornerShape(14.dp)
    val line = colors.acc

    fun begin() {
        state.startDrag(currentIndex)
        haptics.perform(HapticToken.TICK)
    }

    fun dragBy(dy: Float) {
        val before = state.target
        val to = state.drag(dy, state.heightsOf(list, currentKeys))
        if (to != before) haptics.perform(HapticToken.TICK)
    }

    fun drop() {
        val r = state.endDrag() ?: return
        if (r.first != r.second) {
            haptics.perform(HapticToken.CONFIRM)
            move(k, r.second)
        }
    }

    val press = if (!enabled) {
        Modifier
    } else {
        Modifier
            .pointerInput(rk) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val slop = viewConfiguration.touchSlop
                    var dx = 0f
                    var dy = 0f
                    // Undecided inside the slop; a move past it or a lift before the timeout leaves the finger alone.
                    val early = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var gone = false
                        while (!gone) {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            val c = e.changes.firstOrNull { it.id == down.id }
                            if (c == null || !c.pressed || c.isConsumed) {
                                gone = true
                            } else {
                                val d = c.positionChange()
                                dx += d.x
                                dy += d.y
                                gone = GestureRules.cancelsLongPress(dx, dy, slop)
                            }
                        }
                        true
                    }
                    if (early != null) return@awaitEachGesture
                    state.lift(rk)
                    begin()
                    try {
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            val d = c.positionChange()
                            c.consume()
                            if (!c.pressed) break
                            if (d.y != 0f) dragBy(d.y)
                        }
                    } finally {
                        drop()
                    }
                }
            }
            .then(
                if (state.ordering) Modifier.pressable(PressAction.Click({ state.select(rk) }), scaleOnPress = false) else Modifier,
            )
    }

    val handle: @Composable () -> Unit = {
        FoldReveal(selected && enabled, horizontal = true, label = "reorderGrip") {
            Box(
                Modifier
                    .size(44.dp)
                    .semantics { contentDescription = labels.grip }
                    .pointerInput(rk) {
                        detectVerticalDragGestures(
                            onDragStart = { begin() },
                            onDragEnd = { drop() },
                            onDragCancel = { drop() },
                            onVerticalDrag = { change, delta ->
                                change.consume()
                                dragBy(delta)
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                CivilightIcon(IconId.Grip, contentDescription = null, tint = colors.ink3)
            }
        }
    }

    val a11y = if (!enabled) {
        Modifier
    } else {
        Modifier.semantics {
            customActions = listOfNotNull(
                ReorderMath.up(index)?.let { to -> CustomAccessibilityAction(labels.moveUp) { move(k, to); true } },
                ReorderMath.down(index, keys.size)?.let { to -> CustomAccessibilityAction(labels.moveDown) { move(k, to); true } },
            )
        }
    }

    Box(
        Modifier
            .onSizeChanged { state.measure(list, k, it.height.toFloat()) }
            .zIndex(if (selected) 1f else 0f)
            .graphicsLayer {
                translationY = if (selected && state.dragging) state.offset else 0f
                val s = if (reduced) 1f else 1f + (CivilightMotion.liftScale - 1f) * lift.value
                scaleX = s
                scaleY = s
            }
            .then(if (selected) Modifier.tokenShadow(shape, elevation.float(colors.isDark)) else Modifier)
            .then(if (selected) Modifier.background(colors.raise.copy(alpha = colors.raise.alpha * lift.value), shape) else Modifier)
            .drawWithContent {
                drawContent()
                if (!listDragging) return@drawWithContent
                val h = 2.dp.toPx()
                when (ReorderMath.line(index, state.from, state.target)) {
                    ReorderMath.Line.TOP -> drawRect(line, Offset(0f, 0f), Size(size.width, h))
                    ReorderMath.Line.BOTTOM -> drawRect(line, Offset(0f, size.height - h), Size(size.width, h))
                    ReorderMath.Line.NONE -> Unit
                }
            }
            .then(a11y),
    ) {
        ReorderRowScope(selected, state.ordering, press, handle).row(k, index)
    }
}
