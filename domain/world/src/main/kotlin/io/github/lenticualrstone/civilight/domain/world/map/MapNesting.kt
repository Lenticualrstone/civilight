package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.map.WorldMap

/**
 * `하위 지도 · 고르기` (WLD-48): an existing map becomes the child of a pin. The link is stored once,
 * on the child (`WorldMap.parent`, C-26). A map can never sit under itself or under one of its own
 * descendants, so the nesting stays a tree.
 */
object MapNesting {
    /** The maps above [map], nearest first. */
    fun ancestors(doc: ProjectDoc, map: WorldMap): List<WorldMap> {
        val out = ArrayList<WorldMap>()
        val seen = hashSetOf(map.key)
        var parent = map.parent?.let { doc.maps[it.mapKey] }
        while (parent != null && seen.add(parent.key)) {
            out += parent
            parent = parent.parent?.let { doc.maps[it.mapKey] }
        }
        return out
    }

    /** True when [child] may become a child map of a pin on [parentMapKey]. */
    fun canNest(doc: ProjectDoc, childKey: Key, parentMapKey: Key): Boolean {
        if (childKey == parentMapKey) return false
        val parent = doc.maps[parentMapKey]?.takeIf { it.deletedAt == null } ?: return false
        if (doc.maps[childKey]?.takeIf { it.deletedAt == null } == null) return false
        return ancestors(doc, parent).none { it.key == childKey }
    }

    /** Live maps that `고르기` offers for a pin on [parentMapKey]: not that map, not above it, not already this pin's. */
    fun candidates(doc: ProjectDoc, parentMapKey: Key, pinKey: Key): List<WorldMap> =
        MapViews.liveMaps(doc).filter { m -> m.parent?.pinKey != pinKey && canNest(doc, m.key, parentMapKey) }

    /** Makes [childKey] the child of [pinKey] on [parentMapKey]; null when refused. */
    fun setParent(doc: ProjectDoc, childKey: Key, parentMapKey: Key, pinKey: Key, ctx: EditContext): ProjectDoc? {
        if (!canNest(doc, childKey, parentMapKey)) return null
        if (doc.maps[parentMapKey]?.pins?.none { it.key == pinKey } != false) return null
        return MapEdits.updateMap(doc, childKey, ctx) { it.copy(parent = MapParent(parentMapKey, pinKey)) }
    }
}
