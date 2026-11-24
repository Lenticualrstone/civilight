package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A board stage's shared 3D space (U-16 cards boards): CSS `perspective` [perspective] seen from
 * `perspective-origin` ([originX], [originY]), and the rig (`.rig` · `.tilt` · `.plane` · `.drum`) that turns
 * around ([rigX], [rigY]). All in board px of a 390-wide stage, y down, z towards the viewer (CSS axes).
 * [rigFirstY]: the rig turns as `rotateY(b) rotateX(a)` (the 타로 table) instead of `rotateX(a) rotateY(b)`.
 */
internal class Scene(
    val perspective: Float,
    val originX: Float,
    val originY: Float,
    val rigX: Float,
    val rigY: Float,
    val rigFirstY: Boolean = false,
)

/** How a card's own CSS transform orders its turns (after its `translate3d`). */
internal enum class TurnOrder {
    /** 부채꼴 덱: `rotateZ rotateY`. */
    ZY,

    /** 회전 목마: `rotateY`. 롤로덱스: `rotateX` (both via the X-Y-Z order with zero angles). */
    XYZ,

    /** 타로 (on the table): `rotateZ rotateX`. */
    ZX,
}

/**
 * Where one card lands on screen: what a Compose `graphicsLayer` needs to draw it at its board pose
 * ([translationX]/[translationY] in board px from its resting pivot, [scale], the X-Y-Z turn), its [depth]
 * after the rig turn (draw order), and whether its back faces the viewer ([backFacing]). Reused per card,
 * written in the layer block, so nothing is allocated per frame.
 */
internal class CardXform {
    var translationX = 0f
    var translationY = 0f
    var scale = 1f
    var rotationX = 0f
    var rotationY = 0f
    var rotationZ = 0f
    var depth = 0f
    var backFacing = false

    /** Set by the view after [Projector.solve]: the card's fade and shade (0 = full light). */
    var alpha = 1f
    var dim = 0f
}

/**
 * The CSS 3D model of the boards for Compose (JVM-tested). Compose gives each layer its own camera and
 * flattens a turned parent, so the boards' one shared perspective is solved here: the card's pivot goes
 * through the rig turn, then the perspective divide from the origin (`P / (P − z)`), and its own turn is
 * combined with the rig's into one rotation and split back into the X-Y-Z angles a layer takes.
 *
 * Platform facts this relies on. Read from the Compose 1.11 sources in this change: `graphicsLayer.cameraDistance`
 * goes straight to `RenderNode.setCameraDistance` (no density factor). Taken from the HWUI / Skia `Sk3DView`
 * code as remembered, NOT re-read or measured here: the camera sits at `distance × 72` px (72 points per
 * inch), so a CSS perspective of P dp needs `P × density / 72`; the layer applies scale, then
 * `rotateX · rotateY · rotateZ` (Z turns the card first); and rotationX / rotationY turn the same way as CSS
 * `rotateX` / `rotateY`, rotationZ clockwise. Device tells if a fact is off: the ring's far cards must sit
 * higher than the front card, and a fan card right of centre (rotateY −7°) must have its right edge nearer.
 */
internal class Projector {
    // Scratch rotation matrices (row-major 3×3), one projector per stage: nothing allocated per frame.
    private val rig = DoubleArray(9)
    private val card = DoubleArray(9)
    private val tmp = DoubleArray(9)
    private val out = DoubleArray(9)

    /**
     * Solves card pose (x, y, z, turns, [scale]) whose pivot rests at ([pivotX], [pivotY]) for [scene] turned
     * by ([rigRotX], [rigRotY]) into [into]. [flipY] is an extra `rotateY` after the card's own turns (the
     * `.fl` flip layer).
     */
    fun solve(
        into: CardXform,
        scene: Scene,
        rigRotX: Float,
        rigRotY: Float,
        pivotX: Float,
        pivotY: Float,
        x: Float,
        y: Float,
        z: Float,
        rotX: Float = 0f,
        rotY: Float = 0f,
        rotZ: Float = 0f,
        order: TurnOrder = TurnOrder.XYZ,
        flipY: Float = 0f,
        scale: Float = 1f,
    ) {
        // Rig rotation.
        if (scene.rigFirstY) {
            rotY(rig, rigRotY)
            rotX(tmp, rigRotX)
            mul(rig, tmp, out)
            copy(out, rig)
        } else {
            rotX(rig, rigRotX)
            rotY(tmp, rigRotY)
            mul(rig, tmp, out)
            copy(out, rig)
        }
        // Pivot in rig space, turned, back in stage space.
        val px = (pivotX - scene.rigX + x).toDouble()
        val py = (pivotY - scene.rigY + y).toDouble()
        val pz = z.toDouble()
        val wx = rig[0] * px + rig[1] * py + rig[2] * pz + scene.rigX
        val wy = rig[3] * px + rig[4] * py + rig[5] * pz + scene.rigY
        val wz = rig[6] * px + rig[7] * py + rig[8] * pz
        // Perspective divide from the origin; a point at or past the camera stays just in front of it.
        val p = scene.perspective.toDouble()
        val k = p / (p - wz.coerceAtMost(p * 0.9))
        val sx = scene.originX + (wx - scene.originX) * k
        val sy = scene.originY + (wy - scene.originY) * k
        into.translationX = (sx - pivotX).toFloat()
        into.translationY = (sy - pivotY).toFloat()
        into.scale = (k * scale).toFloat()
        into.depth = wz.toFloat()
        // The card's own turn, then the flip layer.
        when (order) {
            TurnOrder.ZY -> {
                rotZ(card, rotZ)
                rotY(tmp, rotY)
                mul(card, tmp, out)
                copy(out, card)
            }
            TurnOrder.ZX -> {
                rotZ(card, rotZ)
                rotX(tmp, rotX)
                mul(card, tmp, out)
                copy(out, card)
            }
            TurnOrder.XYZ -> {
                rotX(card, rotX)
                rotY(tmp, rotY)
                mul(card, tmp, out)
                rotZ(tmp, rotZ)
                mul(out, tmp, card)
            }
        }
        if (flipY != 0f) {
            rotY(tmp, flipY)
            mul(card, tmp, out)
            copy(out, card)
        }
        mul(rig, card, out)
        // R = Rx(a) · Ry(b) · Rz(c): b = asin(R02), a = atan2(−R12, R22), c = atan2(−R01, R00).
        val b = asin(out[2].coerceIn(-1.0, 1.0))
        val a = atan2(-out[5], out[8])
        val c = atan2(-out[1], out[0])
        into.rotationX = Math.toDegrees(a).toFloat()
        into.rotationY = Math.toDegrees(b).toFloat()
        into.rotationZ = Math.toDegrees(c).toFloat()
        // The card's normal (0, 0, 1) after the turn; its z below 0 means the back is towards the viewer.
        into.backFacing = out[8] < 0.0
    }

    private fun rotX(m: DoubleArray, deg: Float) {
        val r = Math.toRadians(deg.toDouble())
        val c = cos(r)
        val s = sin(r)
        set(m, 1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c)
    }

    private fun rotY(m: DoubleArray, deg: Float) {
        val r = Math.toRadians(deg.toDouble())
        val c = cos(r)
        val s = sin(r)
        set(m, c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c)
    }

    private fun rotZ(m: DoubleArray, deg: Float) {
        val r = Math.toRadians(deg.toDouble())
        val c = cos(r)
        val s = sin(r)
        set(m, c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0)
    }

    private fun set(m: DoubleArray, a: Double, b: Double, c: Double, d: Double, e: Double, f: Double, g: Double, h: Double, i: Double) {
        m[0] = a; m[1] = b; m[2] = c
        m[3] = d; m[4] = e; m[5] = f
        m[6] = g; m[7] = h; m[8] = i
    }

    private fun copy(from: DoubleArray, to: DoubleArray) {
        for (i in 0 until 9) to[i] = from[i]
    }

    /** [r] = [a] · [b]; [r] must be neither. */
    private fun mul(a: DoubleArray, b: DoubleArray, r: DoubleArray) {
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                r[row * 3 + col] = a[row * 3] * b[col] + a[row * 3 + 1] * b[3 + col] + a[row * 3 + 2] * b[6 + col]
            }
        }
    }

    companion object {
        /** Skia's camera unit: `cameraDistance` × this = camera distance in px. */
        const val CAMERA_PX_PER_UNIT = 72f

        /** `graphicsLayer.cameraDistance` for a CSS perspective of [perspective] board px drawn at [pxPerUnit] px each. */
        fun cameraDistance(perspective: Float, pxPerUnit: Float): Float = perspective * pxPerUnit / CAMERA_PX_PER_UNIT
    }
}
