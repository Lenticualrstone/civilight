package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.node.NodeEditorState
import io.github.lenticualrstone.civilight.feature.map.node.NodeMapState
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/** The layer and item panels are keyed lazy lists (motion audit row 13): a repeated key would crash them. */
class PanelListKeysTest {
    private val doc = MapFx.doc()

    @Test
    fun `layer and item rows of every map have distinct keys`() {
        val maps = doc.maps.values.filter { doc.liveness.isMapLive(it.key) }
        assertEquals(true, maps.isNotEmpty())
        for (map in maps) {
            if (MapScales.isNodeMap(map)) {
                val ui = NodeMapState.build(doc, map.key, LensPoint.Latest, true, NodeEditorState())!!
                assertEquals("node map ${map.name} items", ui.items.map { it.key }.distinct().size, ui.items.size)
                assertEquals("node map ${map.name} layers", ui.layers.map { it.key }.distinct().size, ui.layers.size)
            } else {
                val ui = EditState.build(doc, map.key, LensPoint.Latest, true, EditorState())!!
                assertEquals("map ${map.name} items", ui.items.map { it.key }.distinct().size, ui.items.size)
                assertEquals("map ${map.name} layers", ui.layers.map { it.key }.distinct().size, ui.layers.size)
            }
        }
    }
}
