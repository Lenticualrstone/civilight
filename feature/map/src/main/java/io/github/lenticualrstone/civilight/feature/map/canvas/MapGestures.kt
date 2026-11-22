package io.github.lenticualrstone.civilight.feature.map.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Pan and pinch with an end hook (motion audit §4 feature:map row 3). It follows `detectTransformGestures`: nothing is
 * consumed before the touch slop, so a tap still reaches the tap detector; [onGesture] gets the centroid and the change
 * of every event; [onEnd] always runs when the fingers lift, with the finger velocity (px/s) of a pan, or zero when the gesture zoomed or never left the touch slop (a tap).
 */
internal suspend fun PointerInputScope.detectMapTransform(
    onStart: () -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onEnd: (velocity: Offset) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        var zoomed = false
        val touchSlop = viewConfiguration.touchSlop
        val tracker = VelocityTracker()
        awaitFirstDown(requireUnconsumed = false)
        onStart()
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()
                    if (zoomMotion > touchSlop || panMotion > touchSlop) pastTouchSlop = true
                }
                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        if (zoomChange != 1f) zoomed = true
                        onGesture(centroid, panChange, zoomChange)
                        tracker.addPosition(event.changes[0].uptimeMillis, event.calculateCentroid(useCurrent = true))
                    }
                    event.changes.fastForEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
        // Always report the end, so a tap during a running settle cannot leave an over-pinch where it was.
        if (pastTouchSlop) {
            val v = tracker.calculateVelocity()
            onEnd(if (zoomed) Offset.Zero else Offset(v.x, v.y))
        } else {
            onEnd(Offset.Zero)
        }
    }
}

/** A finger must leave at least this fast (px/s) for the pan to carry on. */
internal const val MIN_FLING_SPEED = 120f

/**
 * What happens after the fingers lift: a pan carries on and decays (not when motion is reduced), then a pinch that went past
 * the zoom limits settles back on `standard` (a snap when reduced). Pure steps are in [ViewportMath] and [Viewport.lerp].
 */
internal suspend fun settleViewport(
    start: Viewport,
    velocity: Offset,
    math: ViewportMath,
    centre: ScreenPoint,
    motion: Motion,
    apply: (Viewport) -> Unit,
) {
    var vp = start
    if (!motion.reduced && velocity.getDistance() > MIN_FLING_SPEED && !math.overshoots(vp)) {
        val anim = Animatable(Offset.Zero, Offset.VectorConverter)
        var last = Offset.Zero
        anim.animateDecay(velocity, exponentialDecay()) {
            val delta = value - last
            last = value
            vp = math.transform(vp, centre, delta.x, delta.y, 1f)
            apply(vp)
        }
    }
    val target = math.clamp(vp)
    if (target != vp) {
        val from = vp
        Animatable(0f).animateTo(1f, motion.move(CivilightMotion.standard)) { apply(Viewport.lerp(from, target, value)) }
        apply(target)
    }
}

/** Holds the running settle so a new touch can stop it. */
@Stable
internal class SettleHolder {
    private var job: Job? = null
    fun cancel() { job?.cancel(); job = null }
    fun start(scope: CoroutineScope, block: suspend () -> Unit) { cancel(); job = scope.launch { block() } }
}

/**
 * Moves a screen's viewport on its own (a focused pin, a picked node, a reset): `animateTo` along [token] (`foldOpen`,
 * 360 ms, as WLD-68 asks), a snap when motion is reduced. The first [Viewport.UNSET] resolve is not animated. Gestures
 * call [cancel] first (the screens pass `onViewportChange = { mover.cancel(); … }`).
 */
@Stable
class ViewportMover internal constructor(
    private val scope: CoroutineScope,
    private val motion: State<Motion>,
    private val current: State<() -> Viewport>,
    private val apply: State<(Viewport) -> Unit>,
) {
    private var job: Job? = null

    fun cancel() { job?.cancel(); job = null }

    fun moveTo(target: Viewport, token: MotionToken = CivilightMotion.foldOpen) {
        cancel()
        val from = current.value()
        if (from.zoom <= 0f || target.zoom <= 0f) {
            apply.value(target)
            return
        }
        job = scope.launch {
            Animatable(0f).animateTo(1f, motion.value.move(token)) { apply.value(Viewport.lerp(from, target, value)) }
            apply.value(target)
        }
    }
}

/** A [ViewportMover] over the screen's own viewport state: [current] reads it, [apply] writes it. */
@Composable
fun rememberViewportMover(current: () -> Viewport, apply: (Viewport) -> Unit): ViewportMover {
    val scope = rememberCoroutineScope()
    val motion = rememberUpdatedState(CivilightTheme.motion)
    val cur = rememberUpdatedState(current)
    val app = rememberUpdatedState(apply)
    return remember(scope) { ViewportMover(scope, motion, cur, app) }
}
