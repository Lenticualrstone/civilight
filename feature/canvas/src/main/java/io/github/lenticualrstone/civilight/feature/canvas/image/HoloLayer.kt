package io.github.lenticualrstone.civilight.feature.canvas.image

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import io.github.lenticualrstone.civilight.designsystem.component.HoloStrength
import io.github.lenticualrstone.civilight.designsystem.component.holoFoil
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The rotation sensor behind [TiltSensorPort]: pitch and roll from the game rotation vector. */
private class AndroidTiltSensor(context: Context, private val onTilt: (Tilt) -> Unit) : TiltSensorPort, SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val matrix = FloatArray(9)
    private val angles = FloatArray(3)
    private var base: Pair<Float, Float>? = null

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
        SensorManager.getOrientation(matrix, angles)
        val pitch = Math.toDegrees(angles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(angles[2].toDouble()).toFloat()
        val b = base ?: (pitch to roll).also { base = it }
        onTilt(HoloRules.fromDevice(pitch, roll, b.first, b.second))
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
 * sways back on release; the turn-on sweep while it plays; [Tilt.STILL] under reduced motion. Composing it
 * counts the surface as visible for the sensor gate (T-CNV-55).
 */
@Stable
class HoloTilt internal constructor(
    private val hub: HoloTiltHub,
    private val reduced: Boolean,
    private val sweep: Animatable<Float, *>,
    internal val drag: DragTilt,
    internal val scope: CoroutineScope,
) {
    val value: Tilt
        get() = when {
            reduced -> Tilt.STILL
            sweep.isRunning -> HoloRules.sweepTilt(sweep.value)
            hub.gate.usesSensor -> hub.deviceTilt
            else -> drag.value
        }

    /** True when the surface must be dragged to tilt (no gyro, motion not reduced). */
    val usesDrag: Boolean get() = !reduced && !hub.gate.usesSensor
}

/** A [HoloTilt] for a surface; [sweepOnStart] plays the 480 ms sweep once (holo was just switched on). */
@Composable
fun rememberHoloTilt(sweepOnStart: Boolean = false): HoloTilt {
    val reduced = CivilightTheme.reducedMotion
    val hub = currentHub()
    DisposableEffect(hub, reduced) {
        hub.gate.reducedMotion(reduced)
        hub.gate.shown()
        onDispose { hub.gate.hidden() }
    }
    val sweep = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (!sweepOnStart) return@LaunchedEffect
        val token = HoloRules.sweepOnTurnOn(reduced) ?: return@LaunchedEffect
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(token.durationMs, easing = token.easing))
    }
    val scope = rememberCoroutineScope()
    val drag = remember { DragTilt() }
    return remember(hub, reduced, sweep, drag, scope) { HoloTilt(hub, reduced, sweep, drag, scope) }
}

/** Drag-to-tilt input for [tilt] when the device has no rotation sensor (board `Holo`). */
fun Modifier.holoDrag(tilt: HoloTilt): Modifier = if (!tilt.usesDrag) this else pointerInput(tilt) {
    detectDragGestures(
        onDragStart = { tilt.drag.hold() },
        onDragEnd = { tilt.drag.release(tilt.scope) },
        onDragCancel = { tilt.drag.release(tilt.scope) },
    ) { change, amount ->
        change.consume()
        tilt.drag.value = HoloRules.fromDrag(tilt.drag.value, amount.x, amount.y, size.width.toFloat(), size.height.toFloat())
    }
}

/** The DS foil at [tilt]'s current value. */
fun Modifier.holoFoilAt(tilt: Tilt, theme: ResolvedTheme, strength: Float = HoloStrength.of(theme)): Modifier =
    holoFoil(Offset(tilt.x, tilt.y), theme, strength)

/**
 * The holo foil of a surface while [on] (CNV-30): DS `holoFoil` driven by the device tilt; without a
 * rotation sensor the surface is dragged to tilt and sways back on release; when [sweepOnStart] (the
 * surface appears because holo was just switched on) one sweep left → right, 480 ms decelerate. Reduced
 * motion: a still foil, no sweep, no sensor. The sensor is registered only while a holo surface is
 * composed (on screen), T-CNV-55. [strength] defaults to the theme's tint strength.
 */
@Composable
fun Modifier.holoSurface(on: Boolean, sweepOnStart: Boolean = false, strength: Float? = null): Modifier {
    if (!on) return this
    val theme = if (CivilightTheme.colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT
    val tilt = rememberHoloTilt(sweepOnStart)
    return holoDrag(tilt).holoFoilAt(tilt.value, theme, strength ?: HoloStrength.of(theme))
}

/** Drag-to-tilt without a gyro: follows the finger, then sways slowly back to rest ([HoloRules.sway]). */
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
            val start = withFrameMillis { it }
            while (true) {
                val t = HoloRules.sway(from, withFrameMillis { it } - start)
                value = t
                if (t == Tilt.ZERO) break
            }
        }
    }
}
