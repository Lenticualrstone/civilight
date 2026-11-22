package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.world.map.LayerOrder
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapNesting
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.feature.map.view.MapViewState
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-45 레이어 (the `레이어` tab) and WLD-48 하위 지도 (pin property, place sheet row). */
class LayerAndChildMapTest {
    private val doc = MapFx.doc()

    @Test
    fun `WLD-45 layer rows carry name, shown switch, point and order buttons`() {
        val ctx = MapFx.ctx()
        val added = (MapEdits.addLayer(doc, K.MAP_WORLD, "길", ctx) as MapEdit.Applied).doc
        val rows = EditState.layerRows(added, added.maps.getValue(K.MAP_WORLD))
        assertEquals(listOf("정치", "길"), rows.map { it.name })
        assertEquals(listOf(false, true), rows.map { it.canUp })
        assertEquals(listOf(true, false), rows.map { it.canDown })
        assertTrue(rows.all { it.visible })
        val moved = LayerOrder.moveUp(added, K.MAP_WORLD, rows[1].key, ctx)!!
        assertEquals(listOf("길", "정치"), EditState.layerRows(moved, moved.maps.getValue(K.MAP_WORLD)).map { it.name })
        assertNull(LayerOrder.moveUp(moved, K.MAP_WORLD, rows[1].key, ctx))
    }

    @Test
    fun `WLD-45 a switched-off layer drops its overlays from the map`() {
        val world = doc.maps.getValue(K.MAP_WORLD)
        val politics = world.layers.first { it.name == "정치" }
        assertNotNull(MapScenes.build(doc, world, LensPoint.Latest, false, SceneMode.VIEW).region(K.REGION_UPPER_GUARD))
        val off = MapEdits.setLayerVisible(doc, K.MAP_WORLD, politics.key, false, MapFx.ctx())!!
        val offMap = off.maps.getValue(K.MAP_WORLD)
        assertNull(MapScenes.build(off, offMap, LensPoint.Latest, false, SceneMode.VIEW).region(K.REGION_UPPER_GUARD))
        assertFalse(EditState.layerRows(off, offMap).first().visible)
    }

    @Test
    fun `WLD-48 the pin 상층 lists its child map, 고르기 offers only maps that keep a tree`() {
        val place = MapViewState.build(doc, K.MAP_WORLD, LensPoint.Latest, false, null, K.PIN_WORLD_UPPER_CITY, null)!!.place!!
        assertEquals(listOf("하층 구역 지도"), place.childMaps.map { it.second })
        val candidates = MapNesting.candidates(doc, K.MAP_LOWER_DISTRICT, K.PIN_SOCIETY_GATE).map { it.key }
        assertFalse("a map never sits under its own child", K.MAP_WORLD in candidates)
        assertFalse(K.MAP_LOWER_DISTRICT in candidates)
    }
}
