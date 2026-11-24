package io.github.lenticualrstone.civilight.designsystem.component

import android.content.Context
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.card.TiltListener
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- pose source

/** A pose written in place each frame (no allocation in the draw lambda). */
class HoloPoseBuffer {
    var x: Float = HoloPose.STILL.x
    var y: Float = HoloPose.STILL.y
    var sheen: Float = HoloPose.STILL.sheen

    fun set(x: Float, y: Float, sheen: Float) {
        this.x = x
        this.y = y
        this.sheen = sheen
    }
}

/** Writes the current pose into a buffer; called inside the draw phase, so state reads only redraw. */
fun interface HoloPoseSource {
    fun read(out: HoloPoseBuffer)
}

// ---------------------------------------------------------------- shaders (API 33+)

/**
 * AGSL of the image foil. FOIL is the board's `.hfoil` (tint stops over silver stops on two repeating 115°
 * lines; the lines come from [HoloMath]), drawn in `overlay`. GLINT adds the sparkle of a real foil: sparse
 * points on a [HoloMath.GLINT_CELL_DP] grid that light up and go out as the tilt turns, each a little
 * iridescent, drawn in `screen`.
 */
internal object HoloShaders {
    const val FOIL = """
uniform float2 uTS;
uniform float2 uTV;
uniform float2 uSS;
uniform float2 uSV;
uniform float uTintA;

float per(float2 p, float2 s, float2 v) {
    return fract(dot(p - s, v) / dot(v, v));
}

half3 silver(float t) {
    half3 c0 = half3(0.494, 0.518, 0.549);
    half3 c1 = half3(0.914, 0.925, 0.937);
    half3 c2 = half3(0.639, 0.663, 0.694);
    half3 c3 = half3(0.965, 0.969, 0.976);
    float a = 6.0 / 26.0;
    float b = 12.0 / 26.0;
    float c = 18.0 / 26.0;
    if (t < a) return mix(c0, c1, half(t / a));
    if (t < b) return mix(c1, c2, half((t - a) / (b - a)));
    if (t < c) return mix(c2, c3, half((t - b) / (c - b)));
    return mix(c3, c0, half((t - c) / (1.0 - c)));
}

half4 tint(float t) {
    half3 pink = half3(1.0, 0.588, 0.784);
    half3 sky = half3(0.549, 0.824, 1.0);
    half3 lilac = half3(0.745, 0.627, 1.0);
    if (t < 0.25) return half4(pink, half(t / 0.25));
    if (t < 0.5) return half4(mix(pink, sky, half((t - 0.25) / 0.25)), 1.0);
    if (t < 0.75) return half4(mix(sky, lilac, half((t - 0.5) / 0.25)), 1.0);
    return half4(lilac, half(1.0 - (t - 0.75) / 0.25));
}

half4 main(float2 p) {
    half3 s = silver(per(p, uSS, uSV));
    half4 t = tint(per(p, uTS, uTV));
    half a = t.a * half(uTintA);
    return half4(mix(s, t.rgb, a), 1.0);
}
"""

    const val GLINT = """
uniform float2 uTilt;
uniform float uCell;

float hash(float2 c) {
    return fract(sin(dot(c, float2(12.9898, 78.233))) * 43758.5453);
}

half4 main(float2 p) {
    float2 g = p / uCell;
    float2 cell = floor(g);
    float2 f = fract(g) - 0.5;
    float r1 = hash(cell);
    float r2 = hash(cell + float2(17.0, 31.0));
    float r3 = hash(cell + float2(5.0, 11.0));
    if (r2 < 0.8) return half4(0.0);
    float2 jit = (float2(r1, r3) - 0.5) * 0.5;
    float d = length(f - jit);
    float ph = r1 * 6.2831 + uTilt.x * (4.0 + r3 * 5.0) + uTilt.y * (3.0 + r1 * 4.0);
    float tw = pow(max(0.0, sin(ph)), 12.0);
    float a = tw * smoothstep(0.22, 0.0, d);
    float h = fract(dot(p, float2(0.0019, 0.0011)) + uTilt.x * 0.3 + uTilt.y * 0.2 + r3 * 0.2);
    float3 col = 0.74 + 0.26 * cos(6.2831 * (h + float3(0.0, 0.33, 0.67)));
    return half4(half3(col) * half(a), half(a));
}
"""

    /** Full-spectrum iridescent band along [uDir] (unit vector / period), phase [uPhase] in cycles. */
    const val IRIS = """
uniform float2 uDir;
uniform float uPhase;

half4 main(float2 p) {
    float h = fract(dot(p, uDir) + uPhase);
    float3 col = 0.5 + 0.5 * cos(6.2831 * (h + float3(0.0, 0.33, 0.67)));
    return half4(half3(col), 1.0);
}
"""

    /** Set once a shader failed to build on this device: every surface then draws the gradient fallback. */
    @Volatile
    var broken: Boolean = false
}

/** The two shaders of one surface, typed as plain [Shader] so pre-33 devices never load `RuntimeShader`. */
private class HoloShaderSet(val foil: Shader, val glint: Shader, val iris: Shader) {
    val foilBrush: ShaderBrush = ShaderBrush(foil)
    val glintBrush: ShaderBrush = ShaderBrush(glint)
    val irisBrush: ShaderBrush = ShaderBrush(iris)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object HoloShaders33 {
    fun create(): HoloShaderSet = HoloShaderSet(RuntimeShader(HoloShaders.FOIL), RuntimeShader(HoloShaders.GLINT), RuntimeShader(HoloShaders.IRIS))

    fun iris(s: Shader, dx: Float, dy: Float, phase: Float) {
        val r = s as RuntimeShader
        r.setFloatUniform("uDir", dx, dy)
        r.setFloatUniform("uPhase", phase)
    }

    fun foil(s: Shader, tsx: Float, tsy: Float, tvx: Float, tvy: Float, ssx: Float, ssy: Float, svx: Float, svy: Float, tintA: Float) {
        val r = s as RuntimeShader
        r.setFloatUniform("uTS", tsx, tsy)
        r.setFloatUniform("uTV", tvx, tvy)
        r.setFloatUniform("uSS", ssx, ssy)
        r.setFloatUniform("uSV", svx, svy)
        r.setFloatUniform("uTintA", tintA)
    }

    fun glint(s: Shader, x: Float, y: Float, cell: Float) {
        val r = s as RuntimeShader
        r.setFloatUniform("uTilt", x, y)
        r.setFloatUniform("uCell", cell)
    }
}

/**
 * One shader pair for every surface, built once (compiling AGSL per tile, or per frame while a shared-element
 * transition resizes a card, would stall). Sharing is safe: the uniforms are set right before each draw, and
 * the recording canvas snapshots a `RuntimeShader` at draw time. Main thread only (draw phase).
 */
private object HoloShaderPool {
    private var tried = false
    private var set: HoloShaderSet? = null

    fun get(): HoloShaderSet? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || HoloShaders.broken) return null
        if (!tried) {
            tried = true
            set = runCatching { HoloShaders33.create() }.onFailure { HoloShaders.broken = true }.getOrNull()
        }
        return set
    }
}

// ---------------------------------------------------------------- renderer

/**
 * The board's image foil (`scr_cards.py` `.hfoil` + `.hsheen`, `gen_holo.py` `.grain`) over what the node
 * draws, at the pose [pose] writes each frame:
 *
 * 1. foil: iridescent tint (pink · sky · lilac, .5 × [intensity]) over the four silver stops, both on
 *    repeating 115° bands that travel with the tilt (tint 50 %, silver 30 %: parallax), `overlay` at .7. API 33+
 *    draws it with an AGSL shader in one pass; below 33 (or if the shader fails) the same two bands are
 *    repeating linear gradients composited in an `overlay` layer.
 * 2. sheen: a 24 % wide white band at 105°, .6, `overlay`, sliding with the pose's sheen.
 * 3. grain: 1 dp lines every 3 dp, `soft-light` at .35.
 * 4. glints (shader path only): sparse iridescent points that blink as the tilt turns, `screen`.
 *
 * Gradients are built once per size; a moving pose only translates them (no allocation per frame). [fade]
 * scales the whole foil (turning on and off). [maskToContent] keeps the foil to the pixels the content drew
 * (a fitted image in a larger box: `보기`, the crop stage).
 */
fun Modifier.holoImageFoil(
    pose: HoloPoseSource,
    intensity: () -> Float = { 1f },
    fade: () -> Float = { 1f },
    maskToContent: Boolean = false,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val w = size.width
        val h = size.height
        val buf = HoloPoseBuffer()
        val zero = HoloPose.ZERO
        val tint0 = HoloMath.tintLine(w, h, zero)
        val silver0 = HoloMath.silverLine(w, h, zero)
        val sheen0 = HoloMath.sheenLine(w, h, zero)
        val shaders = HoloShaderPool.get()
        // The gradient fallback is always ready: a shader that fails at draw time switches to it for good.
        val tintBrush = repeating(tint0, tintStops())
        val silverBrush = repeating(silver0, silverStops())
        val sheenBrush = Brush.linearGradient(
            0f to Color.Transparent,
            HoloMath.SHEEN_FROM to Color.Transparent,
            0.5f to Color.White.copy(alpha = HoloMath.SHEEN_ALPHA),
            HoloMath.SHEEN_TO to Color.Transparent,
            1f to Color.Transparent,
            start = Offset(sheen0.startX, sheen0.startY),
            end = Offset(sheen0.endX, sheen0.endY),
        )
        val grainPx = HoloMath.GRAIN_PERIOD_DP.dp.toPx()
        val lineFrac = (HoloMath.GRAIN_LINE_DP / HoloMath.GRAIN_PERIOD_DP)
        val grainLine = Color.White.copy(alpha = HoloMath.GRAIN_LINE_ALPHA)
        val grainBrush = Brush.linearGradient(
            0f to grainLine, lineFrac to grainLine, lineFrac to Color.Transparent, 1f to Color.Transparent,
            start = Offset.Zero, end = Offset(0f, grainPx), tileMode = TileMode.Repeated,
        )
        val glintCell = HoloMath.GLINT_CELL_DP.dp.toPx()
        // Iridescence: the 115° direction, one hue cycle per period. The fallback is a repeating rainbow gradient
        // built once at phase 0 and slid along its line (no allocation per frame).
        val irisPeriod = HoloMath.irisPeriod(w, h).coerceAtLeast(1f)
        val irisRad = Math.toRadians(HoloMath.FOIL_ANGLE_DEG.toDouble())
        val irisUx = kotlin.math.sin(irisRad).toFloat()
        val irisUy = -kotlin.math.cos(irisRad).toFloat()
        val irisBrush = Brush.linearGradient(
            0f to Color(0xFFFF4D4D), 0.17f to Color(0xFFFFD24D), 0.33f to Color(0xFF6BFF6B), 0.5f to Color(0xFF4DE1FF),
            0.67f to Color(0xFF5B6BFF), 0.83f to Color(0xFFE04DFF), 1f to Color(0xFFFF4D4D),
            start = Offset.Zero, end = Offset(irisUx * irisPeriod, irisUy * irisPeriod), tileMode = TileMode.Repeated,
        )
        val bounds = Rect(Offset.Zero, size)
        val layerPaint = Paint().apply { blendMode = BlendMode.Overlay }
        val maskPaint = Paint().apply { blendMode = BlendMode.DstIn }
        onDrawWithContent {
            drawContent()
            val f = fade().coerceIn(0f, 1f)
            if (f <= 0f || w <= 0f || h <= 0f) return@onDrawWithContent
            val k = intensity()
            pose.read(buf)
            val tdx = HoloMath.bandShift(w, HoloMath.TINT_TRAVEL, buf.x)
            val tdy = HoloMath.bandShift(h, HoloMath.TINT_TRAVEL, buf.y)
            val sdx = HoloMath.bandShift(w, HoloMath.SILVER_TRAVEL, buf.x)
            val sdy = HoloMath.bandShift(h, HoloMath.SILVER_TRAVEL, buf.y)
            val foilA = HoloMath.foilAlpha(k, f)
            val shaderFoil = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaders != null && !HoloShaders.broken && try {
                HoloShaders33.foil(
                    shaders.foil,
                    tint0.startX + tdx, tint0.startY + tdy, tint0.dx, tint0.dy,
                    silver0.startX + sdx, silver0.startY + sdy, silver0.dx, silver0.dy,
                    HoloMath.tintAlpha(k),
                )
                HoloShaders33.glint(shaders.glint, buf.x, buf.y, glintCell)
                true
            } catch (e: IllegalArgumentException) {
                HoloShaders.broken = true
                false
            }
            if (shaderFoil && shaders != null) {
                drawRect(shaders.foilBrush, alpha = foilA, blendMode = BlendMode.Overlay)
            } else {
                layerPaint.alpha = foilA
                drawIntoCanvas { c ->
                    c.saveLayer(bounds, layerPaint)
                    shifted(silverBrush, sdx, sdy, 1f, BlendMode.SrcOver)
                    shifted(tintBrush, tdx, tdy, (k.coerceIn(0f, 1.4f)).coerceAtMost(1f), BlendMode.SrcOver)
                    c.restore()
                }
            }
            val irisA = HoloMath.irisAlpha(k, f)
            if (irisA > 0.004f) {
                val phase = HoloMath.irisPhase(buf.x, buf.y)
                if (shaderFoil && shaders != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HoloShaders33.iris(shaders.iris, irisUx / irisPeriod, irisUy / irisPeriod, phase)
                    drawRect(shaders.irisBrush, alpha = irisA, blendMode = BlendMode.Color)
                } else {
                    shifted(irisBrush, -irisUx * irisPeriod * phase, -irisUy * irisPeriod * phase, irisA, BlendMode.Color)
                }
            }
            shifted(sheenBrush, HoloMath.sheenShift(w, buf.sheen), 0f, f, BlendMode.Overlay)
            drawRect(grainBrush, alpha = HoloMath.GRAIN_OPACITY * f, blendMode = BlendMode.Softlight)
            if (shaderFoil && shaders != null) {
                drawRect(shaders.glintBrush, alpha = (HoloMath.GLINT_OPACITY * k * f).coerceIn(0f, 1f), blendMode = BlendMode.Screen)
            }
            if (maskToContent) {
                drawIntoCanvas { c ->
                    c.saveLayer(bounds, maskPaint)
                    drawContent()
                    c.restore()
                }
            }
        }
    }

/** A still foil at [pose] (reduced motion, previews, the experiment's own tilt). */
fun Modifier.holoImageFoil(pose: HoloPose, intensity: Float = 1f): Modifier =
    holoImageFoil(HoloPoseSource { it.set(pose.x, pose.y, pose.sheen) }, { intensity })

/** [brush] (built at pose 0) moved by ([dx], [dy]) over the whole node: the band slides, nothing is rebuilt. */
private fun DrawScope.shifted(brush: Brush, dx: Float, dy: Float, alpha: Float, mode: BlendMode) {
    translate(dx, dy) {
        drawRect(brush, topLeft = Offset(-dx, -dy), size = this@shifted.size, alpha = alpha, blendMode = mode)
    }
}

private fun repeating(line: HoloLine, stops: Array<Pair<Float, Color>>): Brush =
    Brush.linearGradient(*stops, start = Offset(line.startX, line.startY), end = Offset(line.endX, line.endY), tileMode = TileMode.Repeated)

private fun rgb(v: Int, alpha: Float = 1f): Color = Color(((v shr 16) and 0xFF) / 255f, ((v shr 8) and 0xFF) / 255f, (v and 0xFF) / 255f, alpha)

private fun silverStops(): Array<Pair<Float, Color>> =
    Array(HoloMath.SILVER_STOPS.size) { i -> HoloMath.SILVER_STOPS[i] to rgb(HoloMath.SILVER_RGB[i]) }

/** `rgba(255,150,200,0) 0, pink .5 6 %, sky .5 12 %, lilac .5 18 %, rgba(255,150,200,0) 24 %` (premultiplied ends keep the hue). */
private fun tintStops(): Array<Pair<Float, Color>> {
    val a = HoloMath.TINT_ALPHA
    val (pink, sky, lilac) = HoloMath.TINT_RGB.let { Triple(it[0], it[1], it[2]) }
    return arrayOf(
        0f to rgb(pink, 0f),
        0.25f to rgb(pink, a),
        0.5f to rgb(sky, a),
        0.75f to rgb(lilac, a),
        1f to rgb(lilac, 0f),
    )
}

// ---------------------------------------------------------------- shared device tilt

/**
 * One rotation-vector listener for every holo surface of the app (a grid of holo tiles must not register one
 * each): counted, registered while any surface is shown and its host resumed, removed with the last. Main
 * thread only. Values are DS-74 [DeviceTilt] values (±12°, low-pass).
 */
internal object HoloTiltShared {
    val tilt: DeviceTilt = DeviceTilt()
    private var listener: TiltListener? = null
    private var users = 0
    private var hasSensor: Boolean? = null

    fun available(context: Context): Boolean = hasSensor ?: run {
        val m = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val found = m?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null || m?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        hasSensor = found
        found
    }

    fun acquire(context: Context) {
        users++
        val l = listener ?: TiltListener(context.applicationContext, tilt).also { listener = it }
        l.register()
    }

    fun release() {
        users = (users - 1).coerceAtLeast(0)
        if (users == 0) listener?.unregister()
    }
}

/** The shared device tilt while [enabled] and the device has the sensor; null otherwise (drag + idle sway). */
@Composable
private fun rememberSharedHoloTilt(enabled: Boolean): DeviceTilt? {
    val context = LocalContext.current
    val has = remember(context) { HoloTiltShared.available(context) }
    if (!enabled || !has) return null
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle) {
        var held = false
        fun hold(on: Boolean) {
            if (on == held) return
            held = on
            if (on) HoloTiltShared.acquire(context) else HoloTiltShared.release()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> hold(true)
                Lifecycle.Event.ON_PAUSE -> hold(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            hold(false)
        }
    }
    return HoloTiltShared.tilt
}

// ---------------------------------------------------------------- surface

/** Finger, idle and sweep state of one holo surface. */
@Stable
internal class HoloInput {
    var dragX by mutableFloatStateOf(0f)
    var dragY by mutableFloatStateOf(0f)
    val idleWeight = Animatable(1f)
    val sweep = Animatable(1f)
    val settle = Animatable(1f)
    var seenTrigger: Int = Int.MIN_VALUE
    var stamp by mutableIntStateOf(0)
    var played: Int = 0
    private var release: Job? = null

    fun hold(scope: CoroutineScope) {
        release?.cancel()
        release = scope.launch { idleWeight.animateTo(0f, tween(CivilightMotion.standard.durationMs, easing = CivilightMotion.standard.easing)) }
    }

    /** Back to rest on the board's sway spring (`손을 떼면 천천히 흔들림`), then the idle sway fades back in. */
    fun letGo(scope: CoroutineScope) {
        release?.cancel()
        val fromX = dragX
        val fromY = dragY
        release = scope.launch {
            launch { idleWeight.animateTo(1f, tween(CivilightMotion.enterLong.durationMs, easing = CivilightMotion.enterLong.easing)) }
            val a = Animatable(0f)
            a.animateTo(1f, spring(CivilightMotion.holoSwayDamping, CivilightMotion.holoSwayStiffness, SWAY_THRESHOLD)) {
                dragX = fromX * (1f - value)
                dragY = fromY * (1f - value)
            }
            dragX = 0f
            dragY = 0f
        }
    }

    private companion object {
        const val SWAY_THRESHOLD = 0.002f
    }
}

/**
 * A holo surface (CNV-30, board `Holo`, `scr_cards.py` foil) while [on]: [holoImageFoil] over the node, its
 * pose from the device tilt (one shared listener, or [tilt] when the caller already has one, e.g. a 3D stage),
 * plus a finger drag on the surface; with no gyro, the 7 s idle sway (`hSway`) runs whenever no finger holds
 * it and the released drag swings back on the sway spring. The foil fades in and out on `standard`; raising
 * [sweepTrigger] while on plays the turn-on sweep (480 ms decelerate) once. Reduced motion (or animator scale
 * 0): a still foil at the board's resting pose, no sensor, no sweep, no idle loop.
 *
 * [consumeDrag]: true when the drag belongs to the surface (`보기`, the experiment); false inside a scroller,
 * where the finger is only watched so the row still scrolls and a tap still picks. [intensity] scales the
 * tint and the foil (1 = the board). Apply it to a box that is clipped to the image's shape.
 */
@Composable
fun Modifier.holoImage(
    on: Boolean,
    intensity: Float = 1f,
    sweepTrigger: Int = 0,
    consumeDrag: Boolean = false,
    tilt: DeviceTilt? = null,
    maskToContent: Boolean = false,
    reader: HoloPoseReader? = null,
): Modifier {
    val amount = animatedFraction(on, label = "holoImage")
    val input = remember { HoloInput() }
    if (input.seenTrigger == Int.MIN_VALUE) input.seenTrigger = sweepTrigger
    val fire = on && sweepTrigger != input.seenTrigger
    SideEffect {
        input.seenTrigger = sweepTrigger
        if (fire) input.stamp++
    }
    if (!on && amount.value <= 0f) {
        if (reader != null) SideEffect { reader.source = null }
        return this
    }
    val motion = LocalMotion.current
    val loops = motion.loops
    val external = tilt?.takeIf { it !== DeviceTilt.Still }
    val shared = rememberSharedHoloTilt(enabled = loops && external == null)
    val device = external ?: shared
    val idle: State<Float>? = if (loops && device == null) {
        rememberInfiniteTransition(label = "holoIdle").animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(CivilightMotion.holoIdleMs, easing = LinearEasing), RepeatMode.Restart),
            label = "holoIdlePhase",
        )
    } else {
        null
    }
    val stamp = input.stamp
    LaunchedEffect(stamp) {
        if (stamp <= input.played) return@LaunchedEffect
        input.played = stamp
        if (!loops) return@LaunchedEffect
        input.sweep.snapTo(0f)
        input.settle.snapTo(0f)
        input.sweep.animateTo(1f, tween(CivilightMotion.holoSweep.durationMs, easing = CivilightMotion.holoSweep.easing))
        input.settle.animateTo(1f, motion.spec(CivilightMotion.standard))
    }
    val scope = rememberCoroutineScope()
    val idleEase = CivilightMotion.holoIdle.easing
    val source = remember(input, device, idle, loops) {
        HoloPoseSource { out ->
            if (!loops) {
                out.set(HoloPose.STILL.x, HoloPose.STILL.y, HoloPose.STILL.sheen)
                return@HoloPoseSource
            }
            val sweepRunning = input.sweep.isRunning
            val sw = input.sweep.value
            if (sweepRunning) {
                val x = -1f + 2f * sw
                out.set(x, 0f, x * HoloMath.IDLE_SHEEN)
                return@HoloPoseSource
            }
            val side = idle?.let { HoloMath.idleSide(it.value) { t -> idleEase.transform(t) } } ?: 0f
            val wgt = if (idle != null) input.idleWeight.value else 0f
            // DeviceTilt leans the other way from the board's `gamma / 30`, `(beta − 45) / 30` (and the CNV-30
            // HoloRules reading): flip both axes so the bands run the way the board's do (HoloMath.fromDeviceTilt).
            val sx = device?.let { HoloMath.fromDeviceTilt(it.x) } ?: 0f
            val sy = device?.let { HoloMath.fromDeviceTilt(it.y) } ?: 0f
            val lx = (sx + input.dragX + side * HoloMath.IDLE_X * wgt).coerceIn(-1f, 1f)
            val ly = (sy + input.dragY + side * HoloMath.IDLE_Y * wgt).coerceIn(-1f, 1f)
            val ls = (sx + input.dragX + side * HoloMath.IDLE_SHEEN * wgt).coerceIn(-1.5f, 1.5f)
            val st = input.settle.value
            // The sweep ends at (1, 0, IDLE_SHEEN) and settles into the live pose on `standard`.
            out.set(1f + (lx - 1f) * st, ly * st, HoloMath.IDLE_SHEEN + (ls - HoloMath.IDLE_SHEEN) * st)
        }
    }
    if (reader != null) SideEffect { reader.source = if (loops) source else null }
    val dragMod = if (!loops) {
        Modifier
    } else if (consumeDrag) {
        Modifier.pointerInput(input) {
            detectDragGestures(
                onDragStart = { input.hold(scope) },
                onDragEnd = { input.letGo(scope) },
                onDragCancel = { input.letGo(scope) },
            ) { change, d ->
                change.consume()
                val (x, y) = HoloMath.drag(input.dragX, input.dragY, d.x, d.y, size.width.toFloat(), size.height.toFloat())
                input.dragX = x
                input.dragY = y
            }
        }
    } else {
        Modifier.pointerInput(input) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                input.hold(scope)
                var last = down.position
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val d = change.position - last
                    last = change.position
                    val (x, y) = HoloMath.drag(input.dragX, input.dragY, d.x, d.y, size.width.toFloat(), size.height.toFloat())
                    input.dragX = x
                    input.dragY = y
                }
                input.letGo(scope)
            }
        }
    }
    return this.then(dragMod).holoImageFoil(source, { intensity }, { amount.value }, maskToContent)
}

/**
 * A box whose [content] (an image) carries the holo foil while [on]; see [holoImage]. The caller clips
 * [modifier] to the image's shape. The `홀로` chip belongs next to it: the label never shows without this.
 */
@Composable
fun HoloSurface(
    on: Boolean,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    consumeDrag: Boolean = false,
    tilt: DeviceTilt? = null,
    sweepTrigger: Int = 0,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.holoImage(on, intensity, sweepTrigger, consumeDrag, tilt), content = content)
}


/** Lets a wrapper read the pose a [holoImage] surface follows (the tilting holo card turns with it). */
@Stable
class HoloPoseReader {
    var source: HoloPoseSource? by androidx.compose.runtime.mutableStateOf(null)
}

/**
 * The holo board's card (`gen_holo.py` `.card`): the image in a card that turns with the same pose as its foil,
 * `rotateX(ty · −14°) rotateY(tx · 16°)` under `perspective: 900px`, with the board's shadow and 1 px inner line,
 * the foil, sheen and grain on top; the device tilt, else a drag, else the 7 s idle sway (`hSway`). Holo off,
 * or reduced motion: a still, flat card. [shape] clips the card (board radius 22).
 */
@Composable
fun HoloCard(
    on: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val reader = remember { HoloPoseReader() }
    val buf = remember { HoloPoseBuffer() }
    Box(
        modifier
            .graphicsLayer {
                val src = reader.source
                if (src != null) {
                    src.read(buf)
                    rotationX = -buf.y * HoloMath.CARD_TURN_X
                    rotationY = buf.x * HoloMath.CARD_TURN_Y
                    cameraDistance = HoloMath.CARD_PERSPECTIVE_DP * density / HoloMath.CAMERA_PX_PER_UNIT
                } else {
                    rotationX = 0f
                    rotationY = 0f
                }
            }
            .shadow(HoloMath.CARD_SHADOW_DP.dp, shape)
            .clip(shape)
            .holoImage(on, consumeDrag = false, reader = reader),
        content = content,
    )
}
