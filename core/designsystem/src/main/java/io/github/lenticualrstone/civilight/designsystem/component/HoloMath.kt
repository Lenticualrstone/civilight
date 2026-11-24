package io.github.lenticualrstone.civilight.designsystem.component

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * One pose of the holo foil: the tilt the foil bands follow ([x], [y], each −1 … 1) and the sheen's own
 * sideways place ([sheen], −1 … 1). The sheen usually equals [x]; the idle sway and the turn-on sweep move it
 * further, as the board's `hSheen` keyframes do.
 */
data class HoloPose(val x: Float, val y: Float, val sheen: Float = x) {
    companion object {
        /** The board's resting value (`--tx .35`, `--ty −.2`): reduced motion and the first frame. */
        val STILL = HoloPose(0.35f, -0.2f)
        val ZERO = HoloPose(0f, 0f)
    }
}

/** A repeating gradient's period on the canvas: [startX], [startY] is offset 0, [endX], [endY] offset 1 (px). */
data class HoloLine(val startX: Float, val startY: Float, val endX: Float, val endY: Float) {
    val dx: Float get() = endX - startX
    val dy: Float get() = endY - startY

    /** The period offset (0 … 1, repeating) at a canvas point; the shader computes the same. */
    fun at(px: Float, py: Float): Float {
        val len2 = dx * dx + dy * dy
        if (len2 <= 0f) return 0f
        val t = ((px - startX) * dx + (py - startY) * dy) / len2
        return t - kotlin.math.floor(t)
    }
}

/**
 * The CSS of the board's image foil, as plain numbers (`scr_cards.py` `.hfoil` / `.hsheen`, `gen_holo.py`
 * `.foil` / `.sheen` / `.grain`, `hSway` / `hFoil` / `hSheen`). Pure, so the layout of the bands is tested on
 * the JVM; the AGSL shader and the gradient fallback both draw from these lines.
 *
 * - foil: `repeating-linear-gradient(115deg, tint 0/6/12/18/24 %)` over `repeating-linear-gradient(115deg,
 *   silver 0/6/12/18/26 %)`, `background-size 260 %`, `background-position calc(50 % + tx × 50 %)` (tint) and
 *   `calc(50 % + tx × 30 %)` (silver, parallax), in `overlay` at opacity .7;
 * - sheen: `inset −20 %`, `linear-gradient(105deg, transparent 38 %, white .6 50 %, transparent 62 %)`,
 *   `translateX(tx × 30 %)`, `overlay`;
 * - grain: `repeating-linear-gradient(0deg, white .18 0 1px, transparent 1px 3px)`, `soft-light` at .35.
 */
object HoloMath {
    const val FOIL_ANGLE_DEG = 115f
    const val SHEEN_ANGLE_DEG = 105f

    /** `background-size: 260% 260%`. */
    const val BACKGROUND_SCALE = 2.6f

    /** Tint period (24 % of the gradient line) and silver period (26 %). */
    const val TINT_PERIOD = 0.24f
    const val SILVER_PERIOD = 0.26f

    /** `calc(50% + var(--tx) * 50%)` for the tint, `* 30%` for the silver. */
    const val TINT_TRAVEL = 0.5f
    const val SILVER_TRAVEL = 0.3f

    /** The foil layer's `opacity: .7` and the tint stops' alpha .5 (`scr_cards.py`). */
    const val FOIL_OPACITY = 0.7f
    const val TINT_ALPHA = 0.5f

    /** Sheen: `inset: -20%` (box 1.4 ×), white .6 at 50 % between 38 % and 62 %, `translateX(tx * 30%)`. */
    const val SHEEN_INSET = 0.2f
    const val SHEEN_ALPHA = 0.6f
    const val SHEEN_FROM = 0.38f
    const val SHEEN_TO = 0.62f
    const val SHEEN_TRAVEL = 0.3f

    /** Grain: 1 px line of white .18 every 3 px (dp here), soft-light at .35. */
    const val GRAIN_LINE_DP = 1f
    const val GRAIN_PERIOD_DP = 3f
    const val GRAIN_LINE_ALPHA = 0.18f
    const val GRAIN_OPACITY = 0.35f

    /** Glints of the shader path (API 33+): one possible glint per cell of this many dp. */
    const val GLINT_CELL_DP = 5f
    const val GLINT_OPACITY = 0.42f

    /**
     * Iridescence (0.2.1, beyond the board, after user feedback that the board's foil read as no effect on a
     * warm image): a full-spectrum band laid in `color` blend (the image keeps its light, the band gives it the
     * foil's hue) at this alpha, one hue cycle every [IRIS_PERIOD] of the surface's diagonal along 115°, moving
     * [IRIS_TRAVEL] of a cycle per unit of tilt (x) and 0.6 × that for y. The board's own layers stay as they are.
     */
    const val IRIS_ALPHA = 0.20f
    const val IRIS_PERIOD = 0.85f
    const val IRIS_TRAVEL = 0.55f

    /** Iridescence period on a [w] × [h] surface (px). */
    fun irisPeriod(w: Float, h: Float): Float = kotlin.math.sqrt(w * w + h * h) * IRIS_PERIOD

    /** Iridescence phase (cycles) for a tilt ([x], [y]). */
    fun irisPhase(x: Float, y: Float): Float = x.coerceIn(-1.5f, 1.5f) * IRIS_TRAVEL + y.coerceIn(-1.5f, 1.5f) * IRIS_TRAVEL * 0.6f

    /** Iridescence alpha for an [intensity] and a whole-foil [fade]. */
    fun irisAlpha(intensity: Float, fade: Float): Float = (IRIS_ALPHA * intensity.coerceIn(0f, 1.4f) * fade.coerceIn(0f, 1f)).coerceIn(0f, 1f)

    /** The holo board's card turn: `rotateX(ty · −14deg) rotateY(tx · 16deg)`, `perspective: 900px`. */
    const val CARD_TURN_X = 14f
    const val CARD_TURN_Y = 16f
    const val CARD_PERSPECTIVE_DP = 900f

    /** Skia's camera unit (`cameraDistance` × 72 = px). */
    const val CAMERA_PX_PER_UNIT = 72f

    /** The card's `box-shadow: 0 30px 60px` as a Compose elevation. */
    const val CARD_SHADOW_DP = 18f

    /** Silver stops (0 / 6 / 12 / 18 / 26 %) as fractions of the silver period. */
    val SILVER_STOPS: FloatArray = floatArrayOf(0f, 6f / 26f, 12f / 26f, 18f / 26f, 1f)

    /** Tint stops (0 / 6 / 12 / 18 / 24 %) as fractions of the tint period. */
    val TINT_STOPS: FloatArray = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)

    /** Idle sway ends (`hFoil` `background-position 20% 30%` ↔ `80% 70%`, `hSheen translateX ∓25%`). */
    const val IDLE_X = 0.6f
    const val IDLE_Y = 0.4f

    /** `hSheen` ±25 % of the 1.4 × box against `tx × 30 %`: ±0.83 in tilt units. */
    const val IDLE_SHEEN = 0.25f / SHEEN_TRAVEL

    /**
     * One period of a CSS `repeating-linear-gradient([angleDeg])` drawn into a background tile [scale] × the
     * [w] × [h] box, placed at `background-position` ([posX], [posY]) (fractions, 0.5 = centred) and
     * repeating every [period] of its gradient line.
     */
    fun bandLine(w: Float, h: Float, angleDeg: Float, scale: Float, posX: Float, posY: Float, period: Float): HoloLine {
        val tw = w * scale
        val th = h * scale
        val ox = (w - tw) * posX
        val oy = (h - th) * posY
        val cx = ox + tw / 2f
        val cy = oy + th / 2f
        return line(cx, cy, tw, th, angleDeg, period)
    }

    /** The tint band of [pose] on a [w] × [h] foil. */
    fun tintLine(w: Float, h: Float, pose: HoloPose): HoloLine = bandLine(
        w, h, FOIL_ANGLE_DEG, BACKGROUND_SCALE,
        0.5f + pose.x.coerceIn(-1f, 1f) * TINT_TRAVEL, 0.5f + pose.y.coerceIn(-1f, 1f) * TINT_TRAVEL, TINT_PERIOD,
    )

    /** The silver band of [pose]: the same angle, a slower travel (parallax). */
    fun silverLine(w: Float, h: Float, pose: HoloPose): HoloLine = bandLine(
        w, h, FOIL_ANGLE_DEG, BACKGROUND_SCALE,
        0.5f + pose.x.coerceIn(-1f, 1f) * SILVER_TRAVEL, 0.5f + pose.y.coerceIn(-1f, 1f) * SILVER_TRAVEL, SILVER_PERIOD,
    )

    /** The whole sheen gradient line (0 … 1 over the 1.4 × box), moved sideways by [HoloPose.sheen]. */
    fun sheenLine(w: Float, h: Float, pose: HoloPose): HoloLine {
        val bw = w * (1f + 2f * SHEEN_INSET)
        val bh = h * (1f + 2f * SHEEN_INSET)
        val shift = pose.sheen.coerceIn(-1.5f, 1.5f) * SHEEN_TRAVEL * bw
        val cx = -SHEEN_INSET * w + bw / 2f + shift
        val cy = -SHEEN_INSET * h + bh / 2f
        return line(cx, cy, bw, bh, SHEEN_ANGLE_DEG, 1f)
    }

    /**
     * A CSS gradient line through ([cx], [cy]) for a [bw] × [bh] box at [angleDeg] (0° = to top, clockwise):
     * length `|w sin a| + |h cos a|`; the returned line starts at its 0 % point and spans [span] of it.
     */
    private fun line(cx: Float, cy: Float, bw: Float, bh: Float, angleDeg: Float, span: Float): HoloLine {
        val a = Math.toRadians(angleDeg.toDouble())
        val dx = sin(a).toFloat()
        val dy = -cos(a).toFloat()
        val len = abs(bw * dx) + abs(bh * dy)
        val sx = cx - dx * len / 2f
        val sy = cy - dy * len / 2f
        return HoloLine(sx, sy, sx + dx * len * span, sy + dy * len * span)
    }

    /** Shift (px) of a band's start along x (or y with [h]) when the tilt moves by [t]: `(w − 2.6 w) × travel × t`. */
    fun bandShift(w: Float, travel: Float, t: Float): Float = (w - w * BACKGROUND_SCALE) * travel * t.coerceIn(-1f, 1f)

    /** Shift (px) of the sheen line along x at a [sheen] place: `translateX(sheen × 30 %)` of the 1.4 × box. */
    fun sheenShift(w: Float, sheen: Float): Float = sheen.coerceIn(-1.5f, 1.5f) * SHEEN_TRAVEL * w * (1f + 2f * SHEEN_INSET)

    /** The idle sway's signed side (−1 … 1) at [phase] after [ease] on each half; see [idlePose]. */
    fun idleSide(phase: Float, ease: (Float) -> Float): Float {
        val p = phase - kotlin.math.floor(phase)
        val half = if (p < 0.5f) p * 2f else (p - 0.5f) * 2f
        val e = ease(half.coerceIn(0f, 1f))
        return if (p < 0.5f) -1f + 2f * e else 1f - 2f * e
    }

    /**
     * The idle sway at [phase] (0 … 1 of the 7 s loop; `hSway` 0 % = 100 % one side, 50 % the other) after
     * [ease] (CSS `ease-in-out`, applied to each half).
     */
    fun idlePose(phase: Float, ease: (Float) -> Float): HoloPose {
        val s = idleSide(phase, ease)
        return HoloPose(s * IDLE_X, s * IDLE_Y, s * IDLE_SHEEN)
    }

    /** The turn-on sweep at [fraction] (eased): one pass left → right, the sheen with it. */
    fun sweepPose(fraction: Float): HoloPose {
        val x = -1f + 2f * fraction.coerceIn(0f, 1f)
        return HoloPose(x, 0f, x * IDLE_SHEEN)
    }

    /** [from] → [to] at [fraction] (the sweep's end settling into the live pose). */
    fun blend(from: HoloPose, to: HoloPose, fraction: Float): HoloPose {
        val f = fraction.coerceIn(0f, 1f)
        return HoloPose(
            from.x + (to.x - from.x) * f,
            from.y + (to.y - from.y) * f,
            from.sheen + (to.sheen - from.sheen) * f,
        )
    }

    /**
     * The live pose: device tilt ([sensorX], [sensorY]) plus the finger ([dragX], [dragY]) plus the idle sway
     * weighted by [idleWeight] (1 while nothing moves the foil, 0 while a finger holds it), clamped to ±1.
     */
    fun live(sensorX: Float, sensorY: Float, dragX: Float, dragY: Float, idle: HoloPose, idleWeight: Float): HoloPose {
        val w = idleWeight.coerceIn(0f, 1f)
        val x = (sensorX + dragX + idle.x * w).coerceIn(-1f, 1f)
        val y = (sensorY + dragY + idle.y * w).coerceIn(-1f, 1f)
        val sheen = (sensorX + dragX + idle.sheen * w).coerceIn(-1.5f, 1.5f)
        return HoloPose(x, y, sheen)
    }

    /**
     * A DS-74 [io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt] axis (±12° = ±1, `TiltMath`
     * signs) as a holo tilt axis: the board's sign (`gamma`, `beta` grow as the right edge or the top edge
     * goes down; CNV-30 `HoloRules.anglesFromGravity`). Full range stays ±12°, so a small turn shows.
     */
    fun fromDeviceTilt(v: Float): Float = -v.coerceIn(-1f, 1f)

    /** A finger's drag of ([dx], [dy]) px over a [width] × [height] surface: across the surface = a full turn. */
    fun drag(startX: Float, startY: Float, dx: Float, dy: Float, width: Float, height: Float): Pair<Float, Float> =
        (startX + 2f * dx / width.coerceAtLeast(1f)).coerceIn(-1f, 1f) to (startY + 2f * dy / height.coerceAtLeast(1f)).coerceIn(-1f, 1f)

    /** The foil layer's alpha for an [intensity] (0 … 1.4, 1 = the board) and a whole-foil [fade]. */
    fun foilAlpha(intensity: Float, fade: Float): Float = (FOIL_OPACITY * intensity.coerceIn(0f, 1.4f) * fade.coerceIn(0f, 1f)).coerceIn(0f, 1f)

    /** The tint stops' alpha for an [intensity] (the board's .5 at 1). */
    fun tintAlpha(intensity: Float): Float = (TINT_ALPHA * intensity.coerceIn(0f, 1.4f)).coerceIn(0f, 0.8f)

    // Colours as 0xRRGGBB (FixedColors keeps the Compose colours).
    val SILVER_RGB: IntArray = intArrayOf(0x7E848C, 0xE9ECEF, 0xA3A9B1, 0xF6F7F9, 0x7E848C)
    val TINT_RGB: IntArray = intArrayOf(0xFF96C8, 0x8CD2FF, 0xBEA0FF)
}
