package io.github.lenticualrstone.civilight.ui.graph

import kotlin.math.round

/**
 * The visible part of the plane: `screen = content × zoom + offset` (all in dp). Zoom range and button
 * step are not given by the sources (Q-CV-07); defaults: 25 % … 200 %, ×1.25 per button press.
 */
data class GraphViewport(val offsetX: Float = 0f, val offsetY: Float = 0f, val zoom: Float = 1f) {
    fun toScreen(p: Pt): Pt = Pt(p.x * zoom + offsetX, p.y * zoom + offsetY)

    fun toContent(screen: Pt): Pt = Pt((screen.x - offsetX) / zoom, (screen.y - offsetY) / zoom)

    fun panBy(dx: Float, dy: Float): GraphViewport = copy(offsetX = offsetX + dx, offsetY = offsetY + dy)

    /** Zooms by [factor] keeping the content point under [focus] (screen dp) in place; clamped. */
    fun zoomAround(focus: Pt, factor: Float): GraphViewport {
        val next = clampZoom(zoom * factor)
        if (next == zoom) return this
        val anchor = toContent(focus)
        return GraphViewport(offsetX = focus.x - anchor.x * next, offsetY = focus.y - anchor.y * next, zoom = next)
    }

    /** One `+` press, around the centre of the view. */
    fun zoomIn(viewCenter: Pt): GraphViewport = zoomAround(viewCenter, STEP)

    /** One `−` press. */
    fun zoomOut(viewCenter: Pt): GraphViewport = zoomAround(viewCenter, 1f / STEP)

    val canZoomIn: Boolean get() = zoom < MAX_ZOOM - EPS
    val canZoomOut: Boolean get() = zoom > MIN_ZOOM + EPS

    companion object {
        const val MIN_ZOOM = 0.25f
        const val MAX_ZOOM = 2f
        const val STEP = 1.25f
        private const val EPS = 0.001f

        fun clampZoom(z: Float): Float {
            val c = z.coerceIn(MIN_ZOOM, MAX_ZOOM)
            // Snap tiny float drift so the readout stays stable (`100%`, not `99%`).
            val snapped = round(c * 1000f) / 1000f
            return snapped
        }

        /**
         * A viewport showing [content] inside a view of [viewW] × [viewH] dp, between [topInset] and
         * [bottomInset] (glass bars), with [margin] around; never zoomed in past 100 %.
         */
        fun fit(content: Rect?, viewW: Float, viewH: Float, topInset: Float, bottomInset: Float, margin: Float = 24f): GraphViewport {
            if (content == null || viewW <= 0f || viewH <= 0f) return GraphViewport(offsetX = margin, offsetY = topInset + margin)
            val availW = (viewW - 2 * margin).coerceAtLeast(1f)
            val availH = (viewH - topInset - bottomInset - 2 * margin).coerceAtLeast(1f)
            val zoom = clampZoom(minOf(1f, availW / content.width.coerceAtLeast(1f), availH / content.height.coerceAtLeast(1f)))
            val cx = viewW / 2f
            val cy = topInset + margin + availH / 2f
            val c = content.center
            return GraphViewport(offsetX = cx - c.x * zoom, offsetY = cy - c.y * zoom, zoom = zoom)
        }
    }
}
