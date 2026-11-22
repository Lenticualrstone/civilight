package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.NodeMapNode

/**
 * Node map edits beyond [MapEdits.addNode] and [MapEdits.linkNodes] (WLD-50): move a node, change
 * its name, sub line and place link, and change a connection's label. Each returns null when the map
 * or item is unknown, or (for names) when the name is empty.
 */
object NodeEdits {
    fun moveNode(doc: ProjectDoc, mapKey: Key, nodeKey: Key, to: Point, ctx: EditContext): ProjectDoc? {
        if (doc.maps[mapKey]?.nodes?.none { it.key == nodeKey } != false) return null
        val p = MapGeometry.clamp(to)
        return MapEdits.updateMap(doc, mapKey, ctx) { m -> m.copy(nodes = m.nodes.map { if (it.key == nodeKey) it.copy(x = p.x, y = p.y) else it }) }
    }

    fun updateNode(doc: ProjectDoc, mapKey: Key, nodeKey: Key, ctx: EditContext, f: (NodeMapNode) -> NodeMapNode): ProjectDoc? {
        if (doc.maps[mapKey]?.nodes?.none { it.key == nodeKey } != false) return null
        return MapEdits.updateMap(doc, mapKey, ctx) { m -> m.copy(nodes = m.nodes.map { if (it.key == nodeKey) f(it) else it }) }
    }

    /** Name and sub line (`기록실` · `세라 · 1-3`); a blank sub line clears it. */
    fun renameNode(doc: ProjectDoc, mapKey: Key, nodeKey: Key, name: String, sub: String?, ctx: EditContext): ProjectDoc? {
        val clean = name.trim().takeIf { it.isNotEmpty() } ?: return null
        return updateNode(doc, mapKey, nodeKey, ctx) { it.copy(name = clean, sub = sub?.trim()?.takeIf { s -> s.isNotEmpty() }) }
    }

    /** The node's place link (null = none). */
    fun linkPlace(doc: ProjectDoc, mapKey: Key, nodeKey: Key, placeKey: Key?, ctx: EditContext): ProjectDoc? =
        updateNode(doc, mapKey, nodeKey, ctx) { it.copy(placeKey = placeKey) }

    /** A connection's label (`계단 · 잠김`); blank clears it. */
    fun setLinkLabel(doc: ProjectDoc, mapKey: Key, linkKey: Key, label: String?, ctx: EditContext): ProjectDoc? {
        if (doc.maps[mapKey]?.nodeLinks?.none { it.key == linkKey } != false) return null
        val clean = label?.trim()?.takeIf { it.isNotEmpty() }
        return MapEdits.updateMap(doc, mapKey, ctx) { m -> m.copy(nodeLinks = m.nodeLinks.map { if (it.key == linkKey) it.copy(label = clean) else it }) }
    }

    /** True when [a] and [b] are already connected (either direction). */
    fun linked(doc: ProjectDoc, mapKey: Key, a: Key, b: Key): Boolean =
        doc.maps[mapKey]?.nodeLinks?.any { (it.aNodeKey == a && it.bNodeKey == b) || (it.aNodeKey == b && it.bNodeKey == a) } == true
}
