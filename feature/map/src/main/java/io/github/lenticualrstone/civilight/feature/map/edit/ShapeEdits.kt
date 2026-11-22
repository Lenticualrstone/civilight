package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.story.lens.LensEdit
import io.github.lenticualrstone.civilight.domain.story.lens.LensView
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RegionShapeValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.domain.story.scene.EditContext as StoryEditContext

/**
 * A region's shape over story time (WLD-65): the shape is a value with `◆` history, so a vertex
 * moved under the lens goes through the two buttons of rule 11-2 (`LensEdit.choice`, DECISIONS §1).
 * With no history at 최신 the base shape is edited at once.
 */
object ShapeEdits {
    fun target(mapKey: Key, regionKey: Key): ChangeTarget = ChangeTarget.RegionShape(mapKey, regionKey)

    /** `Plain` (edit the base at once) or the two buttons with their labels. */
    fun choice(doc: ProjectDoc, mapKey: Key, regionKey: Key, lens: LensPoint): EditChoice = LensEdit.choice(doc, target(mapKey, regionKey), lens)

    /** The shape in force at [lens] with vertex [index] moved to [to]; null when the region or index is unknown. */
    fun moved(doc: ProjectDoc, mapKey: Key, regionKey: Key, lens: LensPoint, index: Int, to: Point): List<Point>? {
        val region = doc.maps[mapKey]?.regions?.firstOrNull { it.key == regionKey } ?: return null
        val shape = RegionShapes.shapeAt(doc, mapKey, region, lens)
        if (index !in shape.indices) return null
        return shape.toMutableList().also { it[index] = MapGeometry.clamp(to) }
    }

    /**
     * `이 값 고치기`: the `◆` record in force at [lens] takes the new shape; with none in force, the
     * base shape changes (`LensEdit.overwrite` has no base for region shapes, so the map edit does it).
     */
    fun overwrite(doc: ProjectDoc, mapKey: Key, regionKey: Key, lens: LensPoint, vertices: List<Point>, ctx: EditContext, story: StoryEditContext): ProjectDoc? {
        if (vertices.size < 3) return null
        val target = target(mapKey, regionKey)
        val inForce = LensView(doc).recordAt(target, lens)
            ?: return MapEdits.updateRegion(doc, mapKey, regionKey, ctx) { it.copy(vertices = vertices.map(MapGeometry::clamp)) }
        val value = ChangeRecord.encodeValue(RegionShapeValue.serializer(), RegionShapeValue(vertices.map(MapGeometry::clamp)))
        val next = LensEdit.overwrite(doc, target, lens, value, story)
        return next.takeIf { it.changes.records.any { r -> r.key == inForce.key } }
    }

    /** `여기서부터 바뀜`: a new `◆` shape anchored where `LensEdit.newAnchor` puts it. */
    fun fromHere(doc: ProjectDoc, mapKey: Key, regionKey: Key, lens: LensPoint, vertices: List<Point>, ctx: EditContext): ProjectDoc? =
        (MapEdits.changeShapeFrom(doc, mapKey, regionKey, LensEdit.newAnchor(doc, lens), vertices, ctx) as? MapEdit.Applied)?.doc
}
