package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.AttachedLayer
import io.github.lenticualrstone.civilight.model.map.DrawnLine
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.LineOrigin
import io.github.lenticualrstone.civilight.model.map.Stamp
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open

/**
 * Detailed-terrain editing (WLD-60): the `강 · 길` tools (lines the author draws, and points of any
 * line moved by the author, both flagged as author-edited for the re-roll guard of WLD-56), stamps
 * that attach to the river or road they sit on, and the stamp chip `성 · 크기 2 · 0° · 장소 ‘결사의 문’`.
 */
object DetailEdits {
    /** A stamp this close (map units) to a river or road line is attached to that layer. */
    const val ATTACH_RADIUS = 0.015

    /**
     * Built stamps (`도시 · 마을 · 성 · 폐허 · 탑 · 다리 · 항구 · 표지석`) attach to the river or road
     * under them, so a re-roll of that layer can list them in `위치 확인`. Terrain stamps
     * (`산 · 언덕 · 숲 · 동굴`) never attach.
     */
    fun attaches(kind: StampKind): Boolean = when (kind) {
        StampKind.MOUNTAIN, StampKind.HILL, StampKind.FOREST, StampKind.CAVE -> false
        else -> true
    }

    /** The layer a stamp of [kind] at [at] attaches to: the nearest river or road within [ATTACH_RADIUS]. */
    fun attachedLayerAt(map: WorldMap, kind: StampKind, at: Point): AttachedLayer {
        if (!attaches(kind)) return AttachedLayer.NONE
        val nearest = map.drawnLines.mapNotNull { line ->
            val d = line.points.zipWithNext { a, b -> MapGeometry.segmentDistance(at, a, b) }.minOrNull() ?: return@mapNotNull null
            line to d
        }.filter { it.second <= ATTACH_RADIUS }.minByOrNull { it.second }?.first ?: return AttachedLayer.NONE
        return if (nearest.kind.known == DrawnLineKind.RIVER) AttachedLayer.RIVER else AttachedLayer.ROAD
    }

    /** `도장 · 탭 → 놓기`: a stamp, attached to the river or road under it (see [attaches]). */
    fun placeStamp(
        doc: ProjectDoc,
        mapKey: Key,
        kind: StampKind,
        at: Point,
        ctx: EditContext,
        size: Int = 2,
        rotation: Int = 0,
        placeKey: Key? = null,
    ): MapEdit<Stamp> {
        val placed = MapEdits.placeStamp(doc, mapKey, kind, at, ctx, size, rotation, placeKey)
        if (placed !is MapEdit.Applied) return placed
        val map = placed.doc.maps[mapKey] ?: return placed
        val layer = attachedLayerAt(map, kind, MapGeometry.clamp(at))
        if (layer == AttachedLayer.NONE) return placed
        val stamp = placed.value.copy(attachedLayer = layer.open())
        val next = MapEdits.updateMap(placed.doc, mapKey, ctx) { m -> m.copy(stamps = m.stamps.map { if (it.key == stamp.key) stamp else it }) } ?: return placed
        return MapEdit.Applied(next, stamp)
    }

    /** A stamp moved by the author (the `이동` tool): it re-attaches to what is under its new spot. */
    fun moveStamp(doc: ProjectDoc, mapKey: Key, stampKey: Key, to: Point, ctx: EditContext): ProjectDoc? {
        val map = doc.maps[mapKey] ?: return null
        val stamp = map.stamps.firstOrNull { it.key == stampKey } ?: return null
        val p = MapGeometry.clamp(to)
        val layer = stamp.kind.known?.let { attachedLayerAt(map, it, p) } ?: AttachedLayer.NONE
        return MapEdits.updateStamp(doc, mapKey, stampKey, ctx) { it.copy(x = p.x, y = p.y, attachedLayer = layer.open()) }
    }

    /** `강 · 길`: a line the author drew (at least 2 points), flagged as author-made. */
    fun addLine(doc: ProjectDoc, mapKey: Key, kind: DrawnLineKind, points: List<Point>, ctx: EditContext): MapEdit<DrawnLine> {
        if (points.size < 2) return MapEdit.Refused(MapRefusal.TOO_FEW_POINTS)
        if (doc.maps[mapKey] == null) return MapEdit.Refused(MapRefusal.NO_MAP)
        val line = DrawnLine(
            key = ctx.newKey(), kind = kind.open(), points = points.map(MapGeometry::clamp), origin = LineOrigin.AUTHOR.open(),
            editedByAuthor = true, updatedAt = ctx.now(), updatedBy = ctx.device,
        )
        val next = MapEdits.updateMap(doc, mapKey, ctx) { it.copy(drawnLines = it.drawnLines + line) } ?: return MapEdit.Refused(MapRefusal.NO_MAP)
        return MapEdit.Applied(next, line)
    }

    /** One point of a river or road moved by the author: the line is flagged as author-edited (WLD-56). */
    fun moveLinePoint(doc: ProjectDoc, mapKey: Key, lineKey: Key, index: Int, to: Point, ctx: EditContext): ProjectDoc? {
        val line = doc.maps[mapKey]?.drawnLines?.firstOrNull { it.key == lineKey } ?: return null
        if (index !in line.points.indices) return null
        val points = line.points.toMutableList().also { it[index] = MapGeometry.clamp(to) }
        return MapEdits.updateMap(doc, mapKey, ctx) { m ->
            m.copy(
                drawnLines = m.drawnLines.map {
                    if (it.key == lineKey) it.copy(points = points, editedByAuthor = true, updatedAt = ctx.now(), updatedBy = ctx.device) else it
                },
            )
        }
    }

    /** The line under [at] within [radius] (map units). */
    fun lineNear(map: WorldMap, at: Point, radius: Double): DrawnLine? = map.drawnLines.mapNotNull { line ->
        line.points.zipWithNext { a, b -> MapGeometry.segmentDistance(at, a, b) }.minOrNull()?.let { line to it }
    }.filter { it.second <= radius }.minByOrNull { it.second }?.first

    /** Name of a stamp kind (`산 · 언덕 · 숲 · 도시 · 마을 · 성 · 폐허 · 탑 · 다리 · 항구 · 동굴 · 표지석`). */
    fun stampName(kind: StampKind): String = when (kind) {
        StampKind.MOUNTAIN -> "산"
        StampKind.HILL -> "언덕"
        StampKind.FOREST -> "숲"
        StampKind.CITY -> "도시"
        StampKind.TOWN -> "마을"
        StampKind.CASTLE -> "성"
        StampKind.RUIN -> "폐허"
        StampKind.TOWER -> "탑"
        StampKind.BRIDGE -> "다리"
        StampKind.PORT -> "항구"
        StampKind.CAVE -> "동굴"
        StampKind.MARKER -> "표지석"
    }

    /** The selected-stamp chip `성 · 크기 2 · 0° · 장소 ‘결사의 문’`. */
    fun stampChip(doc: ProjectDoc, stamp: Stamp): String {
        val name = stamp.kind.known?.let(::stampName) ?: "도장"
        val place = stamp.placeKey?.let { doc.index.place(it)?.name }?.let { " · 장소 ‘$it’" }.orEmpty()
        return "$name · 크기 ${stamp.size} · ${stamp.rotation}°$place"
    }
}
