package io.github.lenticualrstone.civilight.feature.canvas.image

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import io.github.lenticualrstone.civilight.designsystem.component.HoloStrength
import io.github.lenticualrstone.civilight.designsystem.component.HoloMath
import io.github.lenticualrstone.civilight.designsystem.component.HoloPose
import io.github.lenticualrstone.civilight.designsystem.component.holoFoil
import io.github.lenticualrstone.civilight.designsystem.component.holoImage
import io.github.lenticualrstone.civilight.designsystem.component.holoImageFoil
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The rotation sensor behind [TiltSensorPort]: the device's lean and pitch from the gravity row of the game
 * rotation vector's matrix, on the screen's axes (display rotation), relative to how the device was held when
 * the first holo surface appeared ([HoloRules.anglesFromGravity]: smooth when held upright, where
 * `getOrientation`'s roll swings through ±180°). A change of display rotation takes the new holding as the base.
 */
private class AndroidTiltSensor(context: Context, private val onTilt: (Tilt) -> Unit) : TiltSensorPort, SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val displays = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val matrix = FloatArray(9)
    private var base: DeviceAngles? = null
    private var baseRotation = -1

    override val available: Boolean get() = sensor != null

    override fun register() {
        base = null
        sensor?.let { manager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun unregister() {
        manager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        val rotation = displays?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0
        val now = HoloRules.anglesFromGravity(matrix[6], matrix[7], matrix[8], rotation)
        if (rotation != baseRotation) {
            base = null
            baseRotation = rotation
        }
        val b = base ?: now.also { base = it }
        onTilt(HoloRules.fromAngles(now, b))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/** One shared tilt for the holo surfaces of a screen (HoloKit: `같은 기울기 값`), with the sensor gate. */
class HoloTiltHub internal constructor(context: Context) {
    var deviceTilt by mutableStateOf(Tilt.ZERO)
        private set
    private val port = AndroidTiltSensor(context) { deviceTilt = it }
    val gate = HoloSensorGate(port)
}

private val LocalHoloTilt = compositionLocalOf<HoloTiltHub?> { null }

/** Gives the holo surfaces inside [content] one shared tilt (one sensor listener for the screen). */
@Composable
fun ProvideHoloTilt(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val hub = remember(context) { HoloTiltHub(context.applicationContext) }
    CompositionLocalProvider(LocalHoloTilt provides hub, content = content)
}

@Composable
private fun currentHub(): HoloTiltHub {
    val provided = LocalHoloTilt.current
    if (provided != null) return provided
    val context = LocalContext.current
    return remember(context) { HoloTiltHub(context.applicationContext) }
}

/**
 * The tilt of one holo surface (CNV-30): the device tilt while a rotation sensor exists, else a drag that
 * sways back on release (a damped spring); the turn-on sweep while it plays, and after it the sweep's end
 * settles into the live tilt on `standard`; [Tilt.STILL] when decorative loops do not run (reduced motion or
 * animator scale 0). Composing it counts the surface as visible for the sensor gate (T-CNV-55).
 */
@Stable
class HoloTilt internal constructor(
    private val hub: HoloTiltHub,
    private val loops: Boolean,
    private val sweep: Animatable<Float, *>,
    private val settle: Animatable<Float, *>,
    internal val drag: DragTilt,
    internal val scope: CoroutineScope,
) {
    private val live: Tilt get() = if (hub.gate.usesSensor) hub.deviceTilt else drag.value

    val value: Tilt
        get() = when {
            !loops -> Tilt.STILL
            sweep.isRunning -> HoloRules.sweepTilt(sweep.value)
            else -> HoloRules.blend(HoloRules.sweepTilt(1f), live, settle.value)
        }

    /** True when the surface must be dragged to tilt (no gyro, loops running). */
    val usesDrag: Boolean get() = loops && !hub.gate.usesSensor
}

/** A [HoloTilt] for a surface without a turn-on sweep (the holo experiment's shared tilt). */
@Composable
fun rememberHoloTilt(): HoloTilt = rememberHoloTilt(null)

/** The edge detector of one holo surface: [stamp] is raised once per toggle edge; [played] is the last one that swept. */
@Stable
internal class SweepGate(initialTrigger: Int) {
    var seenTrigger: Int = initialTrigger
    var stamp: Int by mutableIntStateOf(0)
    var played: Int = 0
}

/** A [HoloTilt] for a surface; the 480 ms sweep plays once for each edge [gate] reports. */
@Composable
internal fun rememberHoloTilt(gate: SweepGate?): HoloTilt {
    val motion = CivilightTheme.motion
    val loops = motion.loops
    val hub = currentHub()
    DisposableEffect(hub, loops) {
        hub.gate.reducedMotion(!loops)
        hub.gate.shown()
        onDispose { hub.gate.hidden() }
    }
    val sweep = remember { Animatable(1f) }
    val settle = remember { Animatable(1f) }
    val stamp = gate?.stamp ?: 0
    LaunchedEffect(stamp) {
        if (gate == null || stamp <= gate.played) return@LaunchedEffect
        gate.played = stamp
        val token = HoloRules.sweepOnTurnOn(!loops) ?: return@LaunchedEffect
        sweep.snapTo(0f)
        settle.snapTo(0f)
        sweep.animateTo(1f, tween(token.durationMs, easing = token.easing))
        settle.animateTo(1f, motion.spec(CivilightMotion.standard))
    }
    val scope = rememberCoroutineScope()
    val drag = remember { DragTilt() }
    return remember(hub, loops, sweep, settle, drag, scope) { HoloTilt(hub, loops, sweep, settle, drag, scope) }
}

/**
 * Drag-to-tilt input for [tilt] when the device has no rotation sensor (board `Holo`).
 *
 * [consume] true: the drag is the surface's own (the experiment, the viewer). False, for a surface inside a
 * scroller (card rows, the grid, list rows): the finger is only watched, never consumed, so the row or column
 * still scrolls and a tap still picks; the foil follows the finger until the scroll takes over.
 */
fun Modifier.holoDrag(tilt: HoloTilt, consume: Boolean = true): Modifier = when {
    !tilt.usesDrag -> this
    consume -> pointerInput(tilt) {
        detectDragGestures(
            onDragStart = { tilt.drag.hold() },
            onDragEnd = { tilt.drag.release(tilt.scope) },
            onDragCancel = { tilt.drag.release(tilt.scope) },
        ) { change, amount ->
            change.consume()
            tilt.drag.value = HoloRules.fromDrag(tilt.drag.value, amount.x, amount.y, size.width.toFloat(), size.height.toFloat())
        }
    }
    else -> pointerInput(tilt) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            tilt.drag.hold()
            var last = down.position
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val d = change.position - last
                last = change.position
                tilt.drag.value = HoloRules.fromDrag(tilt.drag.value, d.x, d.y, size.width.toFloat(), size.height.toFloat())
            }
            tilt.drag.release(tilt.scope)
        }
    }
}

/** The DS foil at [tilt]'s current value. */
fun Modifier.holoFoilAt(tilt: Tilt, theme: ResolvedTheme, strength: Float = HoloStrength.of(theme)): Modifier =
    holoFoil(Offset(tilt.x, tilt.y), theme, strength)

/** The DS image foil (board `.hfoil`: silver, iridescent tint, sheen, grain, glints) at [tilt]'s current value. */
fun Modifier.holoImageFoilAt(tilt: Tilt, intensity: Float = 1f): Modifier =
    holoImageFoil(HoloPose(tilt.x, tilt.y), intensity)

/** [tilt] followed through a critically damped spring, so a noisy sensor reads smoothly (audit §4 canvas 11). */
@Composable
fun rememberSpringTilt(target: Tilt): Tilt {
    val x by animateFloatAsState(target.x, spring(dampingRatio = CivilightMotion.holoTiltDamping, stiffness = CivilightMotion.holoTiltStiffness), label = "tiltX")
    val y by animateFloatAsState(target.y, spring(dampingRatio = CivilightMotion.holoTiltDamping, stiffness = CivilightMotion.holoTiltStiffness), label = "tiltY")
    return Tilt(x, y)
}

/**
 * The holo foil of a surface while [on] (CNV-30): the DS [holoImage] (AGSL foil on API 33+, gradient foil
 * below; the board's tint, silver, sheen and grain; one shared tilt listener; drag; the 7 s idle sway without a
 * gyro; the 480 ms turn-on sweep on the edge of [sweepTrigger]; a still foil under reduced motion). [strength]
 * is the tint alpha of the old API (the board's .5 = intensity 1). The foil keeps to the drawn image, never
 * the empty box around a fitted one. [consumeDrag]: false for surfaces inside a scrolling row or column.
 */
@Composable
fun Modifier.holoSurface(on: Boolean, sweepTrigger: Int = 0, strength: Float? = null, consumeDrag: Boolean = true): Modifier =
    holoImage(
        on = on,
        intensity = strength?.let { it / HoloMath.TINT_ALPHA } ?: 1f,
        sweepTrigger = sweepTrigger,
        consumeDrag = consumeDrag,
        maskToContent = true,
    )

/** Drag-to-tilt without a gyro: follows the finger, then sways back to rest on a damped spring ([HoloRules.SWAY_DAMPING]). */
internal class DragTilt {
    var value by mutableStateOf(Tilt.ZERO)
    private var sway: Job? = null

    fun hold() {
        sway?.cancel()
    }

    fun release(scope: CoroutineScope) {
        sway?.cancel()
        val from = value
        sway = scope.launch {
            val a = Animatable(Offset(from.x, from.y), Offset.VectorConverter)
            a.animateTo(
                Offset.Zero,
                spring(HoloRules.SWAY_DAMPING, HoloRules.SWAY_STIFFNESS, Offset(SWAY_THRESHOLD, SWAY_THRESHOLD)),
            ) { this@DragTilt.value = Tilt(a.value.x, a.value.y) }
            value = Tilt.ZERO
        }
    }

    private companion object {
        /** Tilt units; the Offset default (0.5) would stop a ±1 tilt at once. */
        const val SWAY_THRESHOLD = 0.002f
    }
}
