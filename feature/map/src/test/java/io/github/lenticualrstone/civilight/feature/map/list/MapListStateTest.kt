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
        // (0.2.0, WLD-75) `지도 N`: the smallest N no live map is named with.
        assertEquals(io.github.lenticualrstone.civilight.domain.world.map.MapNames.nextDefault(doc), map.name)
        assertEquals("지도 1", map.name)
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

    @Test
    fun `최근 고친 곳 lists the newest edits, at most three, never a hidden place`() {
        // Only the edits set below carry a time.
        val base = doc.copy(maps = doc.maps.mapValues { (_, m) ->
            m.copy(
                pins = m.pins.map { it.copy(updatedAt = 0L) }, regions = m.regions.map { it.copy(updatedAt = 0L) },
                routes = m.routes.map { it.copy(updatedAt = 0L) }, labels = m.labels.map { it.copy(updatedAt = 0L) },
            )
        })
        val lower = base.maps.getValue(K.MAP_LOWER_DISTRICT)
        val day = 24L * 3600_000L
        val now = 100 * day + day / 2
        val edited = lower.copy(
            pins = lower.pins.map { p ->
                when (p.key) {
                    K.PIN_ASH_MARKET -> p.copy(updatedAt = now - 1000)
                    K.PIN_SOCIETY_GATE -> p.copy(updatedAt = now - day)
                    else -> p
                }
            },
            routes = lower.routes.map { if (it.key == K.ROUTE_GATE_TO_UPPER_GATE) it.copy(updatedAt = now - 2000) else it },
        )
        val d = base.copy(maps = base.maps + (edited.key to edited))
        val zone = java.time.ZoneOffset.UTC
        val ui = MapListState.build(d, LensPoint.Latest, false, now, zone)
        assertTrue(ui.recent.size <= MapListState.RECENT_COUNT)
        assertEquals(K.PIN_ASH_MARKET, ui.recent.first().key)
        assertEquals(K.PIN_ASH_MARKET, ui.recent.first().pinKey)
        assertEquals("하층 구역 지도 · 핀 ‘잿더미 시장’", ui.recent.first().title)
        assertTrue(ui.recent.first().sub, ui.recent.first().sub.startsWith("오늘 "))
        assertEquals("하층 구역 지도 · 경로 ‘결사의 문 → 상층 관문’", ui.recent[1].title)
        assertTrue(ui.recent[1].sub, ui.recent[1].sub.endsWith("도보 2일 4시간"))
        assertEquals("하층 구역 지도 · 핀 ‘결사의 문’", ui.recent[2].title)
        assertEquals("어제 · 핀", ui.recent[2].sub)
        // Rule 19: a hidden place's pin is not listed while locked, and not named in a route row.
        val hidden = d.copy(world = d.world.copy(places = d.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(hidden = true) else it }))
        val locked = MapListState.build(hidden, LensPoint.Latest, false, now, zone)
        assertTrue(locked.recent.none { "잿더미 시장" in it.title })
        val open = MapListState.build(hidden, LensPoint.Latest, true, now, zone)
        assertEquals(K.PIN_ASH_MARKET, open.recent.first().key)
        // A place name with ` · ` stays whole in the route row's title.
        val dotted = d.copy(world = d.world.copy(places = d.world.places.map { if (it.key == K.PLACE_SOCIETY_GATE) it.copy(name = "동문 · 외곽") else it }))
        val row = MapListState.build(dotted, LensPoint.Latest, false, now, zone).recent.first { it.key == K.ROUTE_GATE_TO_UPPER_GATE }
        assertEquals("하층 구역 지도 · 경로 ‘동문 · 외곽 → 상층 관문’", row.title)
        assertTrue(row.sub, row.sub.endsWith("도보 2일 4시간"))
    }
}
