package io.github.lenticualrstone.civilight.feature.story.common

import io.github.lenticualrstone.civilight.domain.story.travel.Distance
import io.github.lenticualrstone.civilight.domain.story.travel.TravelWarning
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/** A point in map base units: `x` in [0, 1], `y` in [0, aspect] (isotropic, as `domain:story` `Distances`). */
data class MiniPoint(val x: Float, val y: Float)

/** A pin of [placeKey]; [label] is drawn next to it (`6` scene count); [accent] = the chosen or travelled one. */
data class MiniPin(val placeKey: Key, val at: MiniPoint, val label: String?, val accent: Boolean)

/** A route (solid) or the straight line used when no route connects the pins (dashed, `경로 없음`). */
data class MiniLine(val points: List<MiniPoint>, val dashed: Boolean)

/** The rectangle of the map to show, in base units. */
data class MiniBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * A schematic map drawn from the model's geometry (pins, routes): the story lane has no map renderer,
 * so the base image is not drawn, only a `tint` field with the pins and lines (STO-30 card, STO-32 sheet,
 * STO-35 `장소별 씬 수`).
 */
data class MiniMap(val mapKey: Key, val pins: List<MiniPin>, val lines: List<MiniLine>, val bounds: MiniBounds)

/** Builds [MiniMap]s from a project (pure, tested in `TravelLookTest` and `BulkModelTest`). */
object MiniMaps {
    /**
     * The STO-30 card map: the two pins of the pair on the map the distance was measured on, the routes
     * that touch either pin (solid) or, for a straight-line distance, a dashed line between them. Null when
     * the pins are not on that map (child-map pins lifted to a parent are not drawn).
     */
    fun travel(doc: ProjectDoc, warning: TravelWarning): MiniMap? {
        val mapKey = when (val d = warning.distance) {
            is Distance.Route -> d.mapKey
            is Distance.Straight -> d.mapKey
            else -> return null
        }
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        val aspect = aspect(map)
        val fromPlace = placeKeyOf(doc, warning.fromSceneKey) ?: return null
        val toPlace = placeKeyOf(doc, warning.toSceneKey) ?: return null
        val from = map.pins.firstOrNull { it.placeKey == fromPlace } ?: return null
        val to = map.pins.firstOrNull { it.placeKey == toPlace } ?: return null
        val pins = listOf(from, to).map { MiniPin(it.placeKey, MiniPoint(it.x, it.y * aspect), label = null, accent = true) }
        val lines = if (warning.distance is Distance.Straight) {
            listOf(MiniLine(pins.map { it.at }, dashed = true))
        } else {
            map.routes.filter { r -> r.points.any { it.pinKey == from.key || it.pinKey == to.key } }
                .map { r -> MiniLine(r.points.mapNotNull { p -> point(map, p.pinKey, p.x, p.y, aspect) }, dashed = false) }
                .filter { it.points.size >= 2 }
        }
        val all = pins.map { it.at } + lines.flatMap { it.points }
        return MiniMap(map.key, pins, lines, around(all, aspect))
    }

    /**
     * The place map of the bulk sheets (STO-32 sheet, STO-35 `장소별 씬 수`): of the live maps that are not
     * node maps, the one with the most pins of [prefer] (the places in play; every live place when empty),
     * then the higher one; every pin of a live place not in [exclude] (locked hidden places) on it, [labels] next to them, [accent] pins in `acc`.
     */
    fun places(
        doc: ProjectDoc,
        labels: Map<Key, String> = emptyMap(),
        accent: Set<Key> = emptySet(),
        prefer: Set<Key> = emptySet(),
        exclude: Set<Key> = emptySet(),
    ): MiniMap? {
        val livePlaces = doc.world.places.filter { doc.liveness.isPlaceLive(it.key) && it.key !in exclude }.map { it.key }.toSet()
        val wanted = (prefer + labels.keys + accent).filter { it in livePlaces }.toSet().ifEmpty { livePlaces }
        val map = doc.maps.values
            .filter { doc.liveness.isMapLive(it.key) && it.baseKind.known != MapBaseKind.NODE }
            .sortedWith(compareBy({ depth(doc, it) }, { it.order }, { it.key }))
            .maxByOrNull { m -> m.pins.map { it.placeKey }.distinct().count { it in wanted } }
            ?.takeIf { m -> m.pins.any { it.placeKey in livePlaces } }
            ?: return null
        val aspect = aspect(map)
        val pins = map.pins.filter { it.placeKey in livePlaces }.distinctBy { it.placeKey }
            .map { MiniPin(it.placeKey, MiniPoint(it.x, it.y * aspect), labels[it.placeKey], it.placeKey in accent) }
        val lines = map.routes.map { r -> MiniLine(r.points.mapNotNull { p -> point(map, p.pinKey, p.x, p.y, aspect) }, dashed = false) }
            .filter { it.points.size >= 2 }
        return MiniMap(map.key, pins, lines, MiniBounds(0f, 0f, 1f, aspect))
    }

    private fun depth(doc: ProjectDoc, map: WorldMap): Int {
        var d = 0
        var parent = map.parent?.mapKey
        while (parent != null && d < 16) {
            d++
            parent = doc.maps[parent]?.parent?.mapKey
        }
        return d
    }

    /** The pin nearest to [at] within [radius] (base units), for `고른 씬 → 핀 탭`. */
    fun pinAt(map: MiniMap, at: MiniPoint, radius: Float): MiniPin? =
        map.pins.map { it to dist2(it.at, at) }.filter { it.second <= radius * radius }.minByOrNull { it.second }?.first

    private fun placeKeyOf(doc: ProjectDoc, sceneKey: Key): Key? =
        doc.index.unit(sceneKey)?.place?.takeIf { it.kind.known == PlaceRefKind.PLACE }?.key

    private fun aspect(map: WorldMap): Float = map.baseSize?.takeIf { it.w > 0 && it.h > 0 }?.let { it.h.toFloat() / it.w } ?: 1f

    private fun point(map: WorldMap, pinKey: Key?, x: Float?, y: Float?, aspect: Float): MiniPoint? {
        if (pinKey != null) return map.pins.firstOrNull { it.key == pinKey }?.let { MiniPoint(it.x, it.y * aspect) }
        return if (x != null && y != null) MiniPoint(x, y * aspect) else null
    }

    /** The points' box, padded by 25 % of its larger side (at least 0.08), clipped to the map. */
    private fun around(points: List<MiniPoint>, aspect: Float): MiniBounds {
        val l = points.minOf { it.x }
        val r = points.maxOf { it.x }
        val t = points.minOf { it.y }
        val b = points.maxOf { it.y }
        val pad = maxOf(maxOf(r - l, b - t) * 0.25f, 0.08f)
        return MiniBounds((l - pad).coerceAtLeast(0f), (t - pad).coerceAtLeast(0f), (r + pad).coerceAtMost(1f), (b + pad).coerceAtMost(aspect))
    }

    private fun dist2(a: MiniPoint, b: MiniPoint): Float = (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
}
