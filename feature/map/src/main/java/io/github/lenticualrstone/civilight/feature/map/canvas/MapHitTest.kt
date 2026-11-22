package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import kotlin.math.hypot

/** What a tap on the map landed on. */
sealed interface MapHit {
    val key: Key

    data class Pin(override val key: Key) : MapHit
    data class Label(override val key: Key) : MapHit
    data class Node(override val key: Key) : MapHit
    data class Route(override val key: Key) : MapHit
    data class Region(override val key: Key) : MapHit
    data class Stamp(override val key: Key) : MapHit
    data class Line(override val key: Key) : MapHit
}

/**
 * Tap hit-testing in screen pixels, so a 44 dp target works at any zoom (DS-11): pins, labels and
 * nodes within [touchPx] / 2 of their anchor, routes within a third of it of a segment, then
 * regions containing the point. Nearest wins inside a kind; kinds are tried in that order.
 */
object MapHitTest {
    /**
     * [pinLiftPx] is the height of the pin icon above its tip (the target is centred on the icon),
     * [glyphPx] the width of one character of the name tag that follows the icon.
     */
    fun hit(
        scene: MapScene,
        math: ViewportMath,
        viewport: Viewport,
        tap: ScreenPoint,
        touchPx: Float,
        pinLiftPx: Float = touchPx * 12f / 44f,
        glyphPx: Float = touchPx * 12f / 44f,
        /** Stamps and drawn lines are hit only where they are edited (상세 지형 · 편집). */
        terrain: Boolean = false,
    ): MapHit? {
        val radius = touchPx / 2
        fun dist(p: Point): Float {
            val s = math.toScreen(viewport, p)
            return hypot(s.x - tap.x, s.y - tap.y)
        }
        fun pinDist(pin: ScenePin): Float? {
            val tip = math.toScreen(viewport, pin.at)
            val icon = hypot(tip.x - tap.x, tip.y - pinLiftPx - tap.y)
            if (icon <= radius) return icon
            val tagLeft = tip.x + pinLiftPx
            val tagRight = tagLeft + pinLiftPx + glyphPx * pin.name.length
            val inTag = tap.x in tagLeft..tagRight && tap.y in (tip.y - pinLiftPx - radius)..(tip.y - pinLiftPx + radius)
            return if (inTag) radius else null
        }
        scene.pins.mapNotNull { p -> pinDist(p)?.let { p to it } }.minByOrNull { it.second }?.let { return MapHit.Pin(it.first.key) }
        scene.nodes.filter { dist(it.at) <= radius }.minByOrNull { dist(it.at) }?.let { return MapHit.Node(it.key) }
        scene.labels.filter { dist(it.at) <= radius }.minByOrNull { dist(it.at) }?.let { return MapHit.Label(it.key) }
        if (terrain) scene.stamps.filter { dist(it.at) <= radius }.minByOrNull { dist(it.at) }?.let { return MapHit.Stamp(it.key) }
        val mapTap = math.toMap(viewport, tap)
        val lineTol = touchPx / 3 / math.contentW(viewport)
        scene.routes.mapNotNull { r ->
            r.points.zipWithNext { a, b -> MapGeometry.segmentDistance(mapTap, a, b) }.minOrNull()?.let { r to it }
        }.filter { it.second <= lineTol }.minByOrNull { it.second }?.let { return MapHit.Route(it.first.key) }
        if (terrain) scene.lines.mapNotNull { l ->
            l.points.zipWithNext { a, b -> MapGeometry.segmentDistance(mapTap, a, b) }.minOrNull()?.let { l to it }
        }.filter { it.second <= lineTol }.minByOrNull { it.second }?.let { return MapHit.Line(it.first.key) }
        scene.regions.lastOrNull { MapGeometry.contains(it.shape, mapTap) }?.let { return MapHit.Region(it.key) }
        return null
    }

    /** Index of the point of [points] within [touchPx] / 2 of [tap], nearest first (vertex and curve handles). */
    fun handle(points: List<Point>, math: ViewportMath, viewport: Viewport, tap: ScreenPoint, touchPx: Float): Int? =
        points.indices.map { i ->
            val s = math.toScreen(viewport, points[i])
            i to hypot(s.x - tap.x, s.y - tap.y)
        }.filter { it.second <= touchPx / 2 }.minByOrNull { it.second }?.first
}
