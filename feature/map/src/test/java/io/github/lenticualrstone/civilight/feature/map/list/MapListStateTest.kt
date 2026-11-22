package io.github.lenticualrstone.civilight.feature.map.list

import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.common.MapCreation
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.map.MapSize
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-36 list and new maps; WLD-38 image bases; WLD-39 sketch maps; WLD-50 node maps (creation). */
class MapListStateTest {
    private val doc = MapFx.doc()

    @Test
    fun `지도 · N with a card per live map`() {
        val ui = MapListState.build(doc, LensPoint.Latest, false)
        assertEquals("잿빛 새벽의 등불 · 세계", ui.small)
        assertEquals("지도 · 3", ui.section)
        assertEquals(listOf("세계 지도", "하층 구역 지도", "잿더미 시장"), ui.cards.map { it.name })
        assertTrue(ui.cards[0].line.startsWith("생성 · 핀 2"))
        assertEquals("세계 지도 › 핀 · 생성", ui.cards[1].line)
        assertTrue(ui.cards[2].base is MapBase.Node)
    }

    @Test
    fun `the 상위 line of a new child map`() {
        val parent = MapListState.parent(doc, K.MAP_WORLD, K.PIN_WORLD_UPPER_CITY)!!
        assertEquals(MapParent(K.MAP_WORLD, K.PIN_WORLD_UPPER_CITY), parent.parent)
        assertTrue(parent.tag.startsWith("세계 지도 · 핀 ‘"))
        assertNull(MapListState.parent(doc, K.MAP_WORLD, "gone"))
        assertNull(MapListState.parent(doc, null, null))
    }

    @Test
    fun `WLD-38 an imported image becomes an uncropped IMAGE map with its blob catalogued`() {
        val blob = MapFx.blob(w = 8000, h = 6000)
        val made = MapCreation.imageMap(doc, blob, MapFx.ctx())!!
        val map = made.doc.maps.getValue(made.mapKey)
        assertEquals(MapBaseKind.IMAGE, map.baseKind.known)
        assertEquals(blob.hash, map.baseImageHash)
        assertEquals(MapSize(8000, 6000), map.baseSize)
        assertEquals("지도 4", map.name)
        assertTrue(made.doc.assets.blobs.any { it.hash == blob.hash })
        val again = MapCreation.imageMap(made.doc, blob, MapFx.ctx(9))!!
        assertEquals(1, again.doc.assets.blobs.count { it.hash == blob.hash })
    }

    @Test
    fun `지도 이미지 바꾸기 keeps every overlay at its 0–1 coordinates`() {
        val blob = MapFx.blob(hash = "b".repeat(64), w = 1200, h = 900)
        val next = MapCreation.replaceBase(doc, K.MAP_LOWER_DISTRICT, blob, MapFx.ctx())!!
        val before = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        val after = next.maps.getValue(K.MAP_LOWER_DISTRICT)
        assertEquals(blob.hash, after.baseImageHash)
        assertEquals(before.pins, after.pins)
        assertEquals(before.regions, after.regions)
        assertEquals(before.routes, after.routes)
        assertEquals(before.labels, after.labels)
    }

    @Test
    fun `WLD-39 a sketch map keeps the photo and its corners, the base is the straightened image`() {
        val photo = MapFx.blob(hash = "c".repeat(64), source = BlobSource.SKETCH)
        val flat = MapFx.blob(hash = "d".repeat(64), w = 2000, h = 1500, source = BlobSource.SKETCH)
        val corners = listOf(Point(0.1f, 0.1f), Point(0.9f, 0.12f), Point(0.88f, 0.9f), Point(0.12f, 0.88f))
        val made = MapCreation.sketchMap(doc, photo, flat, corners, MapFx.ctx(), MapParent(K.MAP_WORLD, K.PIN_WORLD_UPPER_CITY))!!
        val map = made.doc.maps.getValue(made.mapKey)
        assertEquals(MapBaseKind.SKETCH, map.baseKind.known)
        assertEquals(flat.hash, map.baseImageHash)
        assertEquals(photo.hash, map.sketch!!.photoHash)
        assertEquals(corners, map.sketch!!.corners)
        assertEquals(K.MAP_WORLD, map.parent!!.mapKey)
        assertEquals(2, made.doc.assets.blobs.count { it.hash == photo.hash || it.hash == flat.hash })
    }

    @Test
    fun `노드 지도 has no base`() {
        val made = MapCreation.nodeMap(doc, MapFx.ctx())!!
        val map = made.doc.maps.getValue(made.mapKey)
        assertEquals(MapBaseKind.NODE, map.baseKind.known)
        assertNull(map.baseImageHash)
        assertNotNull(made.doc.maps[made.mapKey])
        assertTrue(MapEdit.Applied::class.isInstance(io.github.lenticualrstone.civilight.domain.world.map.MapEdits.newMap(doc, MapBaseKind.NODE, MapFx.ctx())))
    }
}
