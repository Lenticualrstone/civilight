package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** Layer order by visible buttons (WLD-45 `순서`): one step up or down, no drag. */
object LayerOrder {
    /** Moves [layerKey] one place up (towards the start); null at the top or for an unknown layer. */
    fun moveUp(doc: ProjectDoc, mapKey: Key, layerKey: Key, ctx: EditContext): ProjectDoc? {
        val layers = doc.maps[mapKey]?.let(MapEdits::layers) ?: return null
        val i = layers.indexOfFirst { it.key == layerKey }
        if (i <= 0) return null
        return MapEdits.moveLayer(doc, mapKey, layerKey, after = layers.getOrNull(i - 2)?.key, before = layers[i - 1].key, ctx = ctx)
    }

    /** Moves [layerKey] one place down; null at the bottom or for an unknown layer. */
    fun moveDown(doc: ProjectDoc, mapKey: Key, layerKey: Key, ctx: EditContext): ProjectDoc? {
        val layers = doc.maps[mapKey]?.let(MapEdits::layers) ?: return null
        val i = layers.indexOfFirst { it.key == layerKey }
        if (i < 0 || i >= layers.lastIndex) return null
        return MapEdits.moveLayer(doc, mapKey, layerKey, after = layers[i + 1].key, before = layers.getOrNull(i + 2)?.key, ctx = ctx)
    }
}
