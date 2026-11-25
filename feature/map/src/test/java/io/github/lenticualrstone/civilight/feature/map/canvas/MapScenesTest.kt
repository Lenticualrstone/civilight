package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapScenesTest {
    private val doc = MapFx.doc()
    private val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
    private val world = doc.maps.getValue(K.MAP_WORLD)

    @Test
    fun `view at 1-5 draws revealed items and dims the rest on a map with 숨은 것 흐리게`() {
        val scene = MapScenes.build(doc, lower, LensPoint.UnitEnd(K.EPISODE_1_5), hiddenOpen = false, mode = SceneMode.VIEW)
        val gate = scene.pin(K.PIN_UPPER_GATE)!!
        assertTrue(gate.dim)
        assertFalse(scene.pin(K.PIN_SOCIETY_GATE)!!.dim)
        assertEquals("결사의 문", scene.pin(K.PIN_SOCIETY_GATE)!!.name)
    }

    @Test
    fun `view without 숨은 것 흐리게 leaves unrevealed items out, edit dims them`() {
        val plain = lower.copy(revealViewDim = false)
        val d = doc.copy(maps = doc.maps + (plain.key to plain))
        val view = MapScenes.build(d, plain, LensPoint.UnitEnd(K.EPISODE_1_5), false, SceneMode.VIEW)
        assertNull(view.pin(K.PIN_UPPER_GATE))
        assertEquals(4, view.pins.size)
        val edit = MapScenes.build(d, plain, LensPoint.UnitEnd(K.EPISODE_1_5), false, SceneMode.EDIT)
        assertTrue(edit.pin(K.PIN_UPPER_GATE)!!.dim)
        assertEquals(5, edit.pins.size)
    }

    @Test
    fun `routes are drawn again, their map chip carries the mode, duration and length`() {
        val scene = MapScenes.build(doc, lower, LensPoint.Latest, false, SceneMode.VIEW)
        val drawn = scene.routes.first { it.key == K.ROUTE_GATE_TO_UPPER_GATE }
        assertEquals(Point(0.20f, 0.40f), drawn.points.first())
        assertEquals("결사의 문 → 상층 관문 · 도보 2일 4시간 · 38 km", drawn.label)
        // The stored 0.1.x scale is read as before.
        val route = lower.routes.first { it.key == K.ROUTE_GATE_TO_UPPER_GATE }
        assertEquals("결사의 문 → 상층 관문 · 도보 2일 4시간 · 38 km", io.github.lenticualrstone.civilight.domain.world.map.Routes.sheet(doc, lower, route).mapLabel)
        assertEquals("하층 구역", scene.labels.single().text)
    }

    @Test
    fun `a route through a hidden place's pin is not drawn while locked`() {
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_UPPER_GATE) it.copy(hidden = true) else it }))
        val m = hidden.maps.getValue(K.MAP_LOWER_DISTRICT)
        val gatePin = m.pins.first { it.placeKey == K.PLACE_UPPER_GATE }
        val locked = MapScenes.build(hidden, m, LensPoint.Latest, false, SceneMode.EDIT)
        assertNull(locked.routes.firstOrNull { it.key == K.ROUTE_GATE_TO_UPPER_GATE })
        locked.routes.forEach { r ->
            assertFalse(r.label, "상층 관문" in r.label)
            assertFalse(r.label, Point(gatePin.x, gatePin.y) in r.points)
        }
        val open = MapScenes.build(hidden, m, LensPoint.Latest, true, SceneMode.EDIT)
        assertEquals("결사의 문 → 상층 관문 · 도보 2일 4시간 · 38 km", open.routes.first { it.key == K.ROUTE_GATE_TO_UPPER_GATE }.label)
    }

    @Test
    fun `a layer filter keeps only that layer`() {
        val scene = MapScenes.build(doc, lower, LensPoint.Latest, false, SceneMode.VIEW, layerFilter = K.mapLayer(2))
        assertEquals(2, lower.routes.count { it.layerKey == K.mapLayer(2) })
        assertEquals(lower.routes.filter { it.layerKey == K.mapLayer(2) }.map { it.key }.toSet(), scene.routes.map { it.key }.toSet())
        assertTrue(scene.pins.isEmpty())
        assertTrue(scene.regions.isEmpty())
    }

    @Test
    fun `region shape follows the lens`() {
        val latest = MapScenes.build(doc, world, LensPoint.Latest, false, SceneMode.EDIT).region(K.REGION_UPPER_GUARD)!!
        val later = MapScenes.build(doc, world, LensPoint.UnitEnd(K.EPISODE_1_9), false, SceneMode.EDIT).region(K.REGION_UPPER_GUARD)!!
        assertEquals(Point(0.50f, 0.25f), latest.shape.first())
        assertEquals(Point(0.48f, 0.22f), later.shape.first())
    }

    @Test
    fun `node maps draw nodes and links`() {
        val market = doc.maps.getValue(K.MAP_ASH_MARKET)
        val scene = MapScenes.build(doc, market, LensPoint.Latest, false, SceneMode.VIEW)
        assertEquals(listOf("시장 입구", "오웬의 노점", "뒷골목"), scene.nodes.map { it.name })
        assertEquals(2, scene.links.size)
        assertEquals(MapBase.Node, MapBases.of(market))
    }

    @Test
    fun `bases by kind`() {
        val gen = MapBases.of(lower)
        assertTrue(gen is MapBase.Generated)
        assertNotNull((gen as MapBase.Generated).world)
        assertEquals(64f / 48f, gen.aspect, 1e-4f)
        val image = lower.copy(baseKind = MapBaseKind.IMAGE.open(), baseImageHash = "h", baseSize = io.github.lenticualrstone.civilight.model.map.MapSize(3000, 1500))
        assertEquals(MapBase.Image("h", 3000, 1500), MapBases.of(image))
        assertEquals(2f, MapBases.of(image).aspect, 1e-4f)
        val bare = lower.copy(baseKind = MapBaseKind.IMAGE.open(), generation = null)
        assertEquals(MapBase.Blank, MapBases.of(bare))
    }

    @Test
    fun `kinds and layers of overlay keys`() {
        assertEquals(io.github.lenticualrstone.civilight.domain.world.map.OverlayKind.ROUTE, MapScenes.kindOf(lower, K.ROUTE_GATE_TO_MARKET))
        assertEquals(K.mapLayer(2), MapScenes.layerOf(lower, K.ROUTE_GATE_TO_MARKET))
        assertNull(MapScenes.layerOf(lower, K.PIN_SOCIETY_GATE))
    }

    @Test
    fun `region colours`() {
        assertEquals(0, MapColors.artIndex("map-art-1"))
        assertEquals(4, MapColors.artIndex("map-art-5"))
        assertNull(MapColors.artIndex("#3B4A66"))
        assertEquals(0xFF3B4A66L, MapColors.parseHex("#3B4A66"))
        assertEquals(MapColors.artIndex("map-art-ash"), MapColors.artIndex("map-art-ash"))
        assertTrue(MapColors.artIndex("map-art-ash")!! in 0..4)
    }
}

/** What is new to a scene fades in; the first scene of a screen does not animate (motion audit row 7). */
class SceneFreshnessTest {
    private fun scene(vararg pins: String) = MapScene(pins = pins.map { ScenePin(it, "p$it", it, Point(0.5f, 0.5f), false) })

    @Test
    fun `keys cover every kind of overlay`() {
        val s = MapScene(
            lines = listOf(SceneLine("l", io.github.lenticualrstone.civilight.model.map.DrawnLineKind.RIVER, emptyList(), false)),
            regions = listOf(SceneRegion("r", "", "", emptyList(), false)),
            routes = listOf(SceneRoute("o", emptyList(), "", false)),
            links = listOf(SceneLink("k", Point(0f, 0f), Point(1f, 1f), null, false)),
            labels = listOf(SceneLabel("t", "", Point(0f, 0f), null, false)),
            nodes = listOf(SceneNode("n", "", null, Point(0f, 0f), false)),
            pins = listOf(ScenePin("p", "pl", "", Point(0f, 0f), false)),
        )
        assertEquals(setOf("l", "r", "o", "k", "t", "n", "p"), s.keys())
    }

    @Test
    fun `the first scene has nothing fresh, later ones only what was added`() {
        assertEquals(emptySet<String>(), MapScenes.fresh(null, scene("a", "b").keys()))
        assertEquals(setOf("c"), MapScenes.fresh(scene("a", "b").keys(), scene("a", "b", "c").keys()))
        assertEquals(emptySet<String>(), MapScenes.fresh(scene("a", "b").keys(), scene("b").keys()))
    }
}
