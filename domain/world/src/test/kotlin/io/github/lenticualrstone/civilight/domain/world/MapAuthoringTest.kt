package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.map.LayerOrder
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapNesting
import io.github.lenticualrstone.civilight.domain.world.map.NodeEdits
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdit
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdits
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeRefusal
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Map authoring helpers of the map lane (WLD-45, WLD-47, WLD-48, WLD-50). */
class MapAuthoringTest {
    private val doc = ExampleProject.build()

    // --- WLD-47 travel modes ---

    @Test
    fun `WLD-47 modes are stored once before a key is kept`() {
        val empty = doc.copy(project = doc.project.copy(travelModes = emptyList()))
        // Without stored modes every read gives fresh keys, so a stored key would dangle.
        assertTrue(Travel.modes(empty).first().key != Travel.modes(empty).first().key)
        val stored = TravelModeEdits.ensureStored(empty, ctx())
        assertEquals(listOf("도보", "말", "배"), stored.project.travelModes.map { it.name })
        assertEquals(Travel.modes(stored).map { it.key }, Travel.modes(stored).map { it.key })
        assertTrue(TravelModeEdits.ensureStored(doc, ctx()) === doc)
    }

    @Test
    fun `WLD-47 add and edit a mode`() {
        val added = TravelModeEdits.add(doc, " 마차 ", 30.0, 10.0, ctx()) as TravelModeEdit.Applied
        assertEquals("마차", Travel.modes(added.doc).last().name)
        assertEquals("마차 30 km · 10시간", Travel.modeLabel(Travel.modes(added.doc).last()))
        val walk = Travel.modes(added.doc).first { it.name == "도보" }
        val edited = TravelModeEdits.update(added.doc, walk.key, "도보", 20.0, 8.0, ctx()) as TravelModeEdit.Applied
        assertEquals(20.0, Travel.mode(edited.doc, walk.key)!!.kmPerDay, 0.0)
    }

    @Test
    fun `WLD-47 bad values are refused`() {
        fun reason(e: TravelModeEdit) = (e as TravelModeEdit.Refused).reason
        assertEquals(TravelModeRefusal.EMPTY_NAME, reason(TravelModeEdits.add(doc, " ", 10.0, 8.0, ctx())))
        assertEquals(TravelModeRefusal.BAD_DISTANCE, reason(TravelModeEdits.add(doc, "썰매", 0.0, 8.0, ctx())))
        assertEquals(TravelModeRefusal.BAD_HOURS, reason(TravelModeEdits.add(doc, "썰매", 10.0, 25.0, ctx())))
        assertEquals(TravelModeRefusal.DUPLICATE_NAME, reason(TravelModeEdits.add(doc, "말", 10.0, 8.0, ctx())))
        assertEquals(12.5, TravelModeEdits.parseNumber("12,5")!!, 0.0)
        assertNull(TravelModeEdits.parseNumber("1.2.3"))
        assertNull(TravelModeEdits.parseNumber("열"))
    }

    // --- WLD-48 nesting ---

    @Test
    fun `WLD-48 a map never nests under itself or its descendants`() {
        // 세계 지도 › 하층 구역 지도 › 잿더미 시장 (node map).
        assertFalse(MapNesting.canNest(doc, K.MAP_WORLD, K.MAP_WORLD))
        assertFalse(MapNesting.canNest(doc, K.MAP_WORLD, K.MAP_LOWER_DISTRICT))
        assertFalse(MapNesting.canNest(doc, K.MAP_WORLD, K.MAP_ASH_MARKET))
        assertTrue(MapNesting.canNest(doc, K.MAP_ASH_MARKET, K.MAP_WORLD))
        assertNull(MapNesting.setParent(doc, K.MAP_WORLD, K.MAP_LOWER_DISTRICT, K.PIN_SOCIETY_GATE, ctx()))
        val candidates = MapNesting.candidates(doc, K.MAP_LOWER_DISTRICT, K.PIN_SOCIETY_GATE).map { it.key }
        assertEquals(listOf(K.MAP_ASH_MARKET), candidates)
    }

    @Test
    fun `WLD-48 pick an existing map as a pin's child map`() {
        val made = MapEdits.newMap(doc, MapBaseKind.IMAGE, ctx()) as MapEdit.Applied
        val nested = MapNesting.setParent(made.doc, made.value.key, K.MAP_LOWER_DISTRICT, K.PIN_SOCIETY_GATE, ctx())
        assertNotNull(nested)
        val child = nested!!.maps.getValue(made.value.key)
        assertEquals(K.PIN_SOCIETY_GATE, child.parent!!.pinKey)
        assertEquals(listOf(K.MAP_WORLD, K.MAP_LOWER_DISTRICT), MapNesting.ancestors(nested, child).map { it.key }.reversed())
        // A pin that is not on the parent map is refused.
        assertNull(MapNesting.setParent(made.doc, made.value.key, K.MAP_WORLD, K.PIN_SOCIETY_GATE, ctx()))
    }

    // --- WLD-50 node maps ---

    @Test
    fun `WLD-50 move rename and relabel nodes`() {
        val map = doc.maps.getValue(K.MAP_ASH_MARKET)
        val node = map.nodes.first()
        val moved = NodeEdits.moveNode(doc, map.key, node.key, Point(1.4f, 0.3f), ctx())!!
        assertEquals(1f, moved.maps.getValue(map.key).nodes.first().x, 0f)
        val renamed = NodeEdits.renameNode(moved, map.key, node.key, " 시장 정문 ", " 입구 · 1-2 ", ctx())!!
        val n = renamed.maps.getValue(map.key).nodes.first()
        assertEquals("시장 정문", n.name)
        assertEquals("입구 · 1-2", n.sub)
        assertNull(NodeEdits.renameNode(moved, map.key, node.key, "  ", null, ctx()))
        val b = map.nodes[1]
        val linked = (MapEdits.linkNodes(renamed, map.key, node.key, b.key, ctx()) as MapEdit.Applied)
        assertTrue(NodeEdits.linked(linked.doc, map.key, b.key, node.key))
        val labelled = NodeEdits.setLinkLabel(linked.doc, map.key, linked.value.key, "계단 · 잠김", ctx())!!
        assertEquals("계단 · 잠김", labelled.maps.getValue(map.key).nodeLinks.first { it.key == linked.value.key }.label)
        val cleared = NodeEdits.setLinkLabel(labelled, map.key, linked.value.key, " ", ctx())!!
        assertNull(cleared.maps.getValue(map.key).nodeLinks.first { it.key == linked.value.key }.label)
    }

    // --- WLD-45 layer order ---

    @Test
    fun `WLD-45 layers move one step at a time`() {
        var d: ProjectDoc = doc
        for (name in listOf("지형", "세력")) d = (MapEdits.addLayer(d, K.MAP_LOWER_DISTRICT, name, ctx(name.hashCode().toLong())) as MapEdit.Applied).doc
        fun names(x: ProjectDoc) = MapEdits.layers(x.maps.getValue(K.MAP_LOWER_DISTRICT)).map { it.name }
        assertEquals(listOf("길", "지형", "세력"), names(d))
        val key = { name: String -> MapEdits.layers(d.maps.getValue(K.MAP_LOWER_DISTRICT)).first { it.name == name }.key }
        d = LayerOrder.moveUp(d, K.MAP_LOWER_DISTRICT, key("세력"), ctx())!!
        assertEquals(listOf("길", "세력", "지형"), names(d))
        d = LayerOrder.moveUp(d, K.MAP_LOWER_DISTRICT, key("세력"), ctx())!!
        assertEquals(listOf("세력", "길", "지형"), names(d))
        assertNull(LayerOrder.moveUp(d, K.MAP_LOWER_DISTRICT, key("세력"), ctx()))
        d = LayerOrder.moveDown(d, K.MAP_LOWER_DISTRICT, key("세력"), ctx())!!
        assertEquals(listOf("길", "세력", "지형"), names(d))
        assertNull(LayerOrder.moveDown(d, K.MAP_LOWER_DISTRICT, key("지형"), ctx()))
    }
}
