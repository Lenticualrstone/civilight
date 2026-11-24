package io.github.lenticualrstone.civilight.designsystem.foundation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs
import kotlin.math.hypot

/** The axis a drag has been read as. */
enum class GestureAxis { HORIZONTAL, VERTICAL }

/**
 * (0.2.3) How touch gestures share a finger: long press, horizontal swipe rows, pager drags and vertical scroll.
 * Pure, so the arbitration is JVM-tested; [horizontalSwipe] and the long press of [pressable] follow it.
 *
 * - Nothing is decided inside the touch slop: a tap or a long press stays possible.
 * - A drag is horizontal only when it is clearly sideways ([HORIZONTAL_LEAD]: |dx| ≥ 1.5 |dy|, within about 34°
 *   of the horizontal). A drag that reaches the slop vertically first is vertical, and a sideways control lets
 *   it go at once (it never consumes), so the list under it scrolls.
 * - A sideways control takes a horizontal drag only in a direction it can move ([canMove]); otherwise the drag
 *   is left to the pager or the screen behind it.
 * - Once the finger has travelled past the slop a long press can no longer fire ([cancelsLongPress]).
 */
object GestureRules {
    /** How much a horizontal drag must lead the vertical one. */
    const val HORIZONTAL_LEAD: Float = 1.5f

    /** The axis of a drag that has moved [dx] × [dy] px, or null while it is still undecided. */
    fun axis(dx: Float, dy: Float, slop: Float): GestureAxis? {
        val ax = abs(dx)
        val ay = abs(dy)
        return when {
            ax >= slop && ax >= ay * HORIZONTAL_LEAD -> GestureAxis.HORIZONTAL
            ay >= slop -> GestureAxis.VERTICAL
            else -> null
        }
    }

    /** True once a finger that went down for a long press has moved too far for it ([slop] px). */
    fun cancelsLongPress(dx: Float, dy: Float, slop: Float): Boolean = hypot(dx, dy) > slop

    /**
     * Whether a sideways control resting at [offset] (px; negative = slid left) within [min] … [max] can follow a
     * drag that started toward [dx]'s sign. A closed row that cannot move that way lets the drag through.
     */
    fun canMove(dx: Float, offset: Float, min: Float, max: Float): Boolean = when {
        dx < 0f -> offset > min
        dx > 0f -> offset < max
        else -> false
    }
}

/**
 * A sideways drag that never fights a vertical scroll, a pager or a long press ([GestureRules]): undecided inside
 * the slop, given up (unconsumed) as soon as it reads vertical or [canStart] refuses its direction, and consumed
 * only once taken. [onDelta] gets the x movement in px after the slop; [onStop] the release velocity (px/s), also
 * when the gesture is cancelled.
 */
@Composable
fun Modifier.horizontalSwipe(
    enabled: Boolean = true,
    canStart: (dx: Float) -> Boolean = { true },
    onStart: () -> Unit = {},
    onDelta: (dx: Float) -> Unit,
    onStop: (velocityX: Float) -> Unit = {},
): Modifier {
    val start by rememberUpdatedState(onStart)
    val delta by rememberUpdatedState(onDelta)
    val stop by rememberUpdatedState(onStop)
    val can by rememberUpdatedState(canStart)
    if (!enabled) return this
    return this.then(
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val slop = viewConfiguration.touchSlop
                var dx = 0f
                var dy = 0f
                var taken = false
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!taken) {
                            // An inner control (or a pager handled first) already took this finger.
                            if (change.isConsumed) break
                            val d = change.positionChange()
                            dx += d.x
                            dy += d.y
                            when (GestureRules.axis(dx, dy, slop)) {
                                GestureAxis.HORIZONTAL -> {
                                    if (!can(dx)) break
                                    taken = true
                                    start()
                                    change.consume()
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    delta(dx - if (dx > 0f) slop else -slop)
                                }
                                GestureAxis.VERTICAL -> break
                                null -> Unit
                            }
                        } else {
                            val d = change.positionChange()
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            if (d.x != 0f) delta(d.x)
                        }
                    }
                } finally {
                    if (taken) stop(tracker.calculateVelocity().x)
                }
            }
        },
    )
}
