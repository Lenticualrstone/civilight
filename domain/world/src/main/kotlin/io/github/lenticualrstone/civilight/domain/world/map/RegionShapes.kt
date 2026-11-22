package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RegionShapeValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.map.MapRegion
import io.github.lenticualrstone.civilight.model.position.LensPoint

enum class ShapeChange { GREW, SHRANK, SAME }

/**
 * Two shapes of one region (WLD-65 `비교`). The renderer draws both and dots the part of [after]
 * outside [before] (clipping is a drawing operation; the domain does no polygon boolean ops).
 */
data class ShapeComparison(val before: List<Point>, val after: List<Point>, val change: ShapeChange, val changed: Boolean)

/** Region shapes over story time (WLD-65): the shape is a value with `◆` history (target `regionShape`). */
object RegionShapes {
    /** The shape in effect at [lens]; `예정` shapes beyond the 집필 선 are not applied at 최신. */
    fun shapeAt(doc: ProjectDoc, mapKey: Key, region: MapRegion, lens: LensPoint = LensPoint.Latest): List<Point> {
        val record = doc.recordAt(ChangeTarget.RegionShape(mapKey, region.key), lens)
        return record?.valueAs(RegionShapeValue.serializer())?.vertices ?: region.vertices
    }

    fun compare(doc: ProjectDoc, mapKey: Key, region: MapRegion, from: LensPoint, to: LensPoint): ShapeComparison {
        val a = shapeAt(doc, mapKey, region, from)
        val b = shapeAt(doc, mapKey, region, to)
        val areaA = MapGeometry.area(a)
        val areaB = MapGeometry.area(b)
        val change = when {
            areaB > areaA + EPS -> ShapeChange.GREW
            areaB < areaA - EPS -> ShapeChange.SHRANK
            else -> ShapeChange.SAME
        }
        return ShapeComparison(a, b, change, a != b)
    }

    /** `상층 경비대 | 영역 · 1-9 ◆ 넓어짐` caption parts for the 연표 + 지도 comparison (WLD-64). */
    fun caption(doc: ProjectDoc, region: MapRegion, comparison: ShapeComparison, to: LensPoint): Pair<String, String> {
        val point = when (to) {
            LensPoint.Latest -> "최신"
            is LensPoint.UnitEnd -> StoryCodes(doc).unitLabel(to.unitKey).orEmpty()
        }
        val word = when (comparison.change) {
            ShapeChange.GREW -> "넓어짐"
            ShapeChange.SHRANK -> "좁아짐"
            ShapeChange.SAME -> "바뀜"
        }
        val name = region.factionKey?.let { doc.index.faction(it)?.name } ?: region.name
        return name to "영역${Copy.SEP}$point ◆ $word"
    }

    private const val EPS = 1e-9
}
