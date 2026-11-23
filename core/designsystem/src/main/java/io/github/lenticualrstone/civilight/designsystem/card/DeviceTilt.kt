package io.github.lenticualrstone.civilight.designsystem.card

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.min

/**
 * (0.2.0, DS-74) Tilt maths of the 3D views: device lean and pitch from the gravity row of the rotation
 * matrix, relative to the pose when the view appeared, clamped at ±[MAX_DEG] and low-pass filtered with
 * [TAU_MS]. Pure, so it is tested on the JVM (T-DS-52).
 */
object TiltMath {
    const val MAX_DEG = 12f
    const val TAU_MS = 120f

    /** Largest parallax depth (DS-74: ≤ 8 dp). */
    const val DEPTH_DP = 8f

    private const val DEG = 180f / PI.toFloat()

    /** Gravity in screen axes for a display rotation (`Surface.ROTATION_0` … `_270` as 0 … 3). */
    fun screenX(gx: Float, gy: Float, rotation: Int): Float = when (rotation and 3) {
        1 -> -gy
        2 -> -gx
        3 -> gy
        else -> gx
    }

    fun screenY(gx: Float, gy: Float, rotation: Int): Float = when (rotation and 3) {
        1 -> gx
        2 -> -gy
        3 -> -gx
        else -> gy
    }

    /** Sideways lean in degrees (−90 … 90). */
    fun lean(sx: Float): Float = kotlin.math.asin(sx.coerceIn(-1f, 1f)) * DEG

    /** Forward pitch in degrees (−180 … 180). */
    fun pitch(sy: Float, gz: Float): Float = kotlin.math.atan2(sy, gz) * DEG

    /** [deg] against [baseDeg], wrapped to −180 … 180, as −1 … 1 of [MAX_DEG]. */
    fun normalized(deg: Float, baseDeg: Float): Float {
        var d = deg - baseDeg
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return (d / MAX_DEG).coerceIn(-1f, 1f)
    }

    /** One low-pass step towards [target] after [dtMs]. */
    fun lowPass(previous: Float, target: Float, dtMs: Float): Float {
        if (dtMs <= 0f) return previous
        val k = 1f - kotlin.math.exp(-dtMs / TAU_MS)
        return previous + (target - previous) * k
    }

    /** `Tilt.offset(tilt, depth)` of DS-74, one axis: `tilt × depth`, depth capped at [DEPTH_DP]. */
    fun offset(tilt: Float, depthDp: Float): Float = tilt.coerceIn(-1f, 1f) * min(depthDp, DEPTH_DP)
}

/**
 * (0.2.0, DS-74) The device tilt of a 3D view, each axis −1 … 1. Read [x] and [y] only inside
 * `graphicsLayer {}` or a draw lambda, so a sensor event redraws layers and never recomposes.
 */
@Stable
class DeviceTilt internal constructor() {
    internal val xState: MutableFloatState = mutableFloatStateOf(0f)
    internal val yState: MutableFloatState = mutableFloatStateOf(0f)
    val x: Float get() = xState.floatValue
    val y: Float get() = yState.floatValue

    internal fun zero() {
        xState.floatValue = 0f
        yState.floatValue = 0f
    }

    companion object {
        /** No tilt (switch off, reduced motion, no sensor). */
        val Still: DeviceTilt = DeviceTilt()
    }
}

/**
 * The rotation-vector listener behind [rememberTilt]: no allocation per event (one matrix, two floats of
 * base), relative to the pose at the first event after registering; a display rotation takes a new base.
 */
private class TiltListener(context: Context, private val tilt: DeviceTilt) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val displays = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val matrix = FloatArray(9)
    private var hasBase = false
    private var baseLean = 0f
    private var basePitch = 0f
    private var baseRotation = -1
    private var lastNanos = 0L
    private var registered = false

    fun register() {
        if (registered || sensor == null || manager == null) return
        hasBase = false
        lastNanos = 0L
        registered = manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun unregister() {
        if (!registered) return
        manager?.unregisterListener(this)
        registered = false
        tilt.zero()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 3) return
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        val rotation = displays?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0
        val sx = TiltMath.screenX(matrix[6], matrix[7], rotation)
        val sy = TiltMath.screenY(matrix[6], matrix[7], rotation)
        val lean = TiltMath.lean(sx)
        val pitch = TiltMath.pitch(sy, matrix[8])
        if (!hasBase || rotation != baseRotation) {
            hasBase = true
            baseLean = lean
            basePitch = pitch
            baseRotation = rotation
        }
        val dtMs = if (lastNanos == 0L) 0f else (event.timestamp - lastNanos) / 1_000_000f
        lastNanos = event.timestamp
        tilt.xState.floatValue = TiltMath.lowPass(tilt.x, TiltMath.normalized(lean, baseLean), dtMs)
        tilt.yState.floatValue = TiltMath.lowPass(tilt.y, TiltMath.normalized(pitch, basePitch), dtMs)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/**
 * DS-74 `rememberTilt(enabled)`: one tilt source for the 서재 views and the character card views. Pass
 * `enabled = Tilt.follows(viewTilt, …)` (the LIB-94 switch on and motion not reduced). The listener is
 * registered only while [enabled] and the host is resumed, removed on pause and when the view leaves; with
 * [enabled] false no listener exists and the result is [DeviceTilt.Still] (0, 0). Devices without the
 * sensor stay at (0, 0).
 */
@Composable
fun rememberTilt(enabled: Boolean): DeviceTilt {
    if (!enabled) return DeviceTilt.Still
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tilt = remember { DeviceTilt() }
    DisposableEffect(context, lifecycle, tilt) {
        val listener = TiltListener(context.applicationContext, tilt)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> listener.register()
                Lifecycle.Event.ON_PAUSE -> listener.unregister()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            listener.unregister()
        }
    }
    return tilt
}
