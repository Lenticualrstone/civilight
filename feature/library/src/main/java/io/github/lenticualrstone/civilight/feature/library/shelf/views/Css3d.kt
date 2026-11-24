package io.github.lenticualrstone.civilight.feature.library.shelf.views

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * The CSS 3D model of the U-16 boards, ported as is so the views draw the boards' own numbers: a perspective
 * container (`perspective` + `perspective-origin`), nested `preserve-3d` transforms, `transform-origin`, and
 * planes (one DOM box each). Coordinates are the boards' CSS px, which are dp 1:1 (the boards are 390 wide).
 * y points down, z points at the viewer, angles are degrees, transform lists apply left to right (CSS).
 * Pure and allocation-free per frame, so the JVM tests check it against the generators.
 */

/** A 4×4 matrix, row-major (`v[row * 4 + col]`), acting on column vectors: p' = M · p. */
class M4 {
    val v = FloatArray(16)
    private val t = FloatArray(16)

    init {
        reset()
    }

    fun reset(): M4 {
        v.fill(0f)
        v[0] = 1f; v[5] = 1f; v[10] = 1f; v[15] = 1f
        return this
    }

    fun set(o: M4): M4 {
        o.v.copyInto(v)
        return this
    }

    /** this = this · (a b c / d e f / g h i), translation untouched (a 3×3 linear part). */
    private fun mul3(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float, g: Float, h: Float, i: Float): M4 {
        for (r in 0 until 4) {
            val x = v[r * 4]
            val y = v[r * 4 + 1]
            val z = v[r * 4 + 2]
            v[r * 4] = x * a + y * d + z * g
            v[r * 4 + 1] = x * b + y * e + z * h
            v[r * 4 + 2] = x * c + y * f + z * i
        }
        return this
    }

    /** this = this · o. */
    fun mul(o: M4): M4 {
        for (r in 0 until 4) for (c in 0 until 4) {
            var s = 0f
            for (k in 0 until 4) s += v[r * 4 + k] * o.v[k * 4 + c]
            t[r * 4 + c] = s
        }
        t.copyInto(v)
        return this
    }

    /** CSS `translate3d(x, y, z)`. */
    fun translate(x: Float, y: Float, z: Float = 0f): M4 {
        for (r in 0 until 4) v[r * 4 + 3] += v[r * 4] * x + v[r * 4 + 1] * y + v[r * 4 + 2] * z
        return this
    }

    /** CSS `rotateX(deg)`: y' = y cos − z sin, z' = y sin + z cos (a positive angle tips the top away). */
    fun rotateX(deg: Float): M4 {
        if (deg == 0f) return this
        val a = deg * RAD
        val c = cos(a)
        val s = sin(a)
        return mul3(1f, 0f, 0f, 0f, c, -s, 0f, s, c)
    }

    /** CSS `rotateY(deg)`: x' = x cos + z sin, z' = −x sin + z cos. */
    fun rotateY(deg: Float): M4 {
        if (deg == 0f) return this
        val a = deg * RAD
        val c = cos(a)
        val s = sin(a)
        return mul3(c, 0f, s, 0f, 1f, 0f, -s, 0f, c)
    }

    /** CSS `rotateZ(deg)` (clockwise on screen). */
    fun rotateZ(deg: Float): M4 {
        if (deg == 0f) return this
        val a = deg * RAD
        val c = cos(a)
        val s = sin(a)
        return mul3(c, -s, 0f, s, c, 0f, 0f, 0f, 1f)
    }

    /** CSS `scale(sx, sy)` (2D: z untouched). */
    fun scale(sx: Float, sy: Float = sx): M4 {
        if (sx == 1f && sy == 1f) return this
        return mul3(sx, 0f, 0f, 0f, sy, 0f, 0f, 0f, 1f)
    }

    /** CSS `scaleY(-1)` and the like. */
    fun scale3(sx: Float, sy: Float, sz: Float): M4 = mul3(sx, 0f, 0f, 0f, sy, 0f, 0f, 0f, sz)

    fun mapX(x: Float, y: Float, z: Float): Float = v[0] * x + v[1] * y + v[2] * z + v[3]
    fun mapY(x: Float, y: Float, z: Float): Float = v[4] * x + v[5] * y + v[6] * z + v[7]
    fun mapZ(x: Float, y: Float, z: Float): Float = v[8] * x + v[9] * y + v[10] * z + v[11]

    companion object {
        const val RAD = (PI / 180.0).toFloat()
    }
}

/**
 * A CSS perspective container: `perspective: [p]px; perspective-origin: [ox]px [oy]px`. A point at depth z
 * draws at `o + (xy − o) · p / (p − z)`.
 */
class Persp(var p: Float, var ox: Float, var oy: Float) {
    fun set(p: Float, ox: Float, oy: Float): Persp {
        this.p = p; this.ox = ox; this.oy = oy
        return this
    }

    fun k(z: Float): Float = p / (p - z)
    fun x(x: Float, z: Float): Float = ox + (x - ox) * k(z)
    fun y(y: Float, z: Float): Float = oy + (y - oy) * k(z)
}

/**
 * Projected corners of a plane `w × h` (local corners (0,0) (w,0) (w,h) (0,h), in that order) through [m]
 * and [persp], into [out] as x0 y0 … x3 y3, and the mean depth. Returns NaN depth (and leaves [out]) when a
 * corner reaches [nearFrac] of the perspective distance: CSS draws such a plane behind the eye, which a
 * 2D homography cannot.
 */
object Quad {
    fun project(m: M4, w: Float, h: Float, persp: Persp, out: FloatArray, nearFrac: Float = 0.92f): Float {
        var depth = 0f
        for (i in 0 until 4) {
            val lx = if (i == 1 || i == 2) w else 0f
            val ly = if (i >= 2) h else 0f
            val x = m.mapX(lx, ly, 0f)
            val y = m.mapY(lx, ly, 0f)
            val z = m.mapZ(lx, ly, 0f)
            if (z >= persp.p * nearFrac) return Float.NaN
            out[i * 2] = persp.x(x, z)
            out[i * 2 + 1] = persp.y(y, z)
            depth += z
        }
        return depth / 4f
    }

    /** Twice the signed area (shoelace) in screen axes (y down): positive when the front faces the viewer. */
    fun signedArea(q: FloatArray): Float {
        var s = 0f
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            s += q[i * 2] * q[j * 2 + 1] - q[j * 2] * q[i * 2 + 1]
        }
        return s
    }

    /** Whether (x, y) is inside the convex quad [q] (either winding). */
    fun contains(q: FloatArray, x: Float, y: Float): Boolean {
        var pos = false
        var neg = false
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            val cross = (q[j * 2] - q[i * 2]) * (y - q[i * 2 + 1]) - (q[j * 2 + 1] - q[i * 2 + 1]) * (x - q[i * 2])
            if (cross > 0f) pos = true else if (cross < 0f) neg = true
            if (pos && neg) return false
        }
        return true
    }

    fun minX(q: FloatArray): Float = minOf(minOf(q[0], q[2]), minOf(q[4], q[6]))
    fun maxX(q: FloatArray): Float = maxOf(maxOf(q[0], q[2]), maxOf(q[4], q[6]))
    fun minY(q: FloatArray): Float = minOf(minOf(q[1], q[3]), minOf(q[5], q[7]))
    fun maxY(q: FloatArray): Float = maxOf(maxOf(q[1], q[3]), maxOf(q[5], q[7]))
}

/** Small shared helpers of the boards' scripts. */
object Css {
    fun clamp(v: Float, a: Float, b: Float): Float = if (v < a) a else if (v > b) b else v

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** `u * u * (3 - 2 * u)`: the corridor's smoothstep between stops. */
    fun smooth(u: Float): Float = u * u * (3f - 2f * u)

    /** CSS `ease-in-out` (cubic-bezier(.42,0,.58,1)) at [x], solved by bisection (idle sway only). */
    fun easeInOut(x: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f
        var lo = 0f
        var hi = 1f
        var t = x
        repeat(20) {
            t = (lo + hi) / 2f
            val bx = 3f * (1f - t) * (1f - t) * t * 0.42f + 3f * (1f - t) * t * t * 0.58f + t * t * t
            if (bx < x) lo = t else hi = t
        }
        return 3f * (1f - t) * t * t + t * t * t
    }

    /** The boards' LCG (`(s * 1103515245 + 12345) & 0x7fffffff`), one step. */
    fun lcg(s: Long): Long = (s * 1103515245L + 12345L) and 0x7fffffffL
}
