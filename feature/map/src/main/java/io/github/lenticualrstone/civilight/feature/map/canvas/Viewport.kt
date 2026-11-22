package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.model.Point
import kotlin.math.roundToInt

/**
 * What part of a map is on screen: the map point at the view centre ([cx], [cy] in 0–1) and the
 * zoom (1 = the whole base fits the view). Saved per screen so fold and process death keep centre
 * and zoom (WLD-37 fold rule).
 */
data class Viewport(val cx: Float = 0.5f, val cy: Float = 0.5f, val zoom: Float = 1f) {
    /** `120%`. */
    val percent: Int get() = (zoom * 100).roundToInt()

    fun centeredOn(p: Point): Viewport = copy(cx = p.x.coerceIn(0f, 1f), cy = p.y.coerceIn(0f, 1f))

    /** As a float array for `rememberSaveable`. */
    fun toArray(): FloatArray = floatArrayOf(cx, cy, zoom)

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 8f

        fun fromArray(a: FloatArray?): Viewport = if (a == null || a.size < 3) Viewport() else Viewport(a[0], a[1], a[2])

        /** A screen's first viewport: [ViewportMath.resolve] turns it into the fill zoom once the view size is known. */
        val UNSET = Viewport(zoom = 0f)
    }
}

/** A screen position in pixels. */
data class ScreenPoint(val x: Float, val y: Float)

/**
 * Map ↔ screen conversion for a view of [viewW] × [viewH] px showing a base whose width / height
 * is [aspect]. At zoom 1 the base fits inside the view (contain). Pure: every gesture and tap goes
 * through here, so it is tested on the JVM.
 */
class ViewportMath(val viewW: Float, val viewH: Float, val aspect: Float) {
    private val safeAspect = if (aspect.isFinite() && aspect > 0f) aspect else 1f

    /** Base size in px at zoom 1. */
    val fitW: Float = if (viewW / viewH.coerceAtLeast(1f) > safeAspect) viewH * safeAspect else viewW
    val fitH: Float = if (viewW / viewH.coerceAtLeast(1f) > safeAspect) viewH else viewW / safeAspect

    fun contentW(v: Viewport): Float = fitW * v.zoom
    fun contentH(v: Viewport): Float = fitH * v.zoom

    fun toScreen(v: Viewport, x: Float, y: Float): ScreenPoint =
        ScreenPoint(viewW / 2 + (x - v.cx) * contentW(v), viewH / 2 + (y - v.cy) * contentH(v))

    fun toScreen(v: Viewport, p: Point): ScreenPoint = toScreen(v, p.x, p.y)

    /** The map point under a screen position (not clamped: taps outside the base are refused by the caller). */
    fun toMap(v: Viewport, s: ScreenPoint): Point =
        Point(v.cx + (s.x - viewW / 2) / contentW(v), v.cy + (s.y - viewH / 2) / contentH(v))

    /** True when [p] lies on the base. */
    fun onBase(p: Point): Boolean = p.x in 0f..1f && p.y in 0f..1f

    /**
     * A two-finger gesture: zoom by [factor] around [centroid] (the point under it stays put), then
     * pan by [panX], [panY] px. The centre stays on the base and the zoom within the limits.
     */
    fun transform(v: Viewport, centroid: ScreenPoint, panX: Float, panY: Float, factor: Float): Viewport {
        val anchor = toMap(v, centroid)
        val zoom = (v.zoom * factor).coerceIn(Viewport.MIN_ZOOM, Viewport.MAX_ZOOM)
        val zoomed = v.copy(zoom = zoom)
        val cx = anchor.x - (centroid.x - viewW / 2) / contentW(zoomed)
        val cy = anchor.y - (centroid.y - viewH / 2) / contentH(zoomed)
        val panned = Viewport(cx - panX / contentW(zoomed), cy - panY / contentH(zoomed), zoom)
        return clamp(panned)
    }

    /** The zoom at which the base fills the view (no empty bands), as the boards draw maps. */
    fun fillZoom(): Float = maxOf(viewW / fitW, viewH / fitH).coerceIn(Viewport.MIN_ZOOM, Viewport.MAX_ZOOM)

    /** [Viewport.UNSET] (zoom ≤ 0) becomes the fill zoom; any other viewport is kept. */
    fun resolve(v: Viewport): Viewport = if (v.zoom > 0f) v else v.copy(zoom = fillZoom())

    fun clamp(v: Viewport): Viewport = Viewport(
        v.cx.coerceIn(0f, 1f),
        v.cy.coerceIn(0f, 1f),
        v.zoom.coerceIn(Viewport.MIN_ZOOM, Viewport.MAX_ZOOM),
    )

    /** The visible part of the base in 0–1 space (left, top, right, bottom), clipped to the base. */
    fun visibleRect(v: Viewport): FloatArray {
        val tl = toMap(v, ScreenPoint(0f, 0f))
        val br = toMap(v, ScreenPoint(viewW, viewH))
        return floatArrayOf(tl.x.coerceIn(0f, 1f), tl.y.coerceIn(0f, 1f), br.x.coerceIn(0f, 1f), br.y.coerceIn(0f, 1f))
    }
}
