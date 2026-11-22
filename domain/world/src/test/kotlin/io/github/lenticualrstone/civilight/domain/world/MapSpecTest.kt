package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.key
import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.map.DistanceKind
import io.github.lenticualrstone.civilight.domain.world.map.DistanceProblem
import io.github.lenticualrstone.civilight.domain.world.map.Distances
import io.github.lenticualrstone.civilight.domain.world.map.HexCell
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry
import io.github.lenticualrstone.civilight.domain.world.map.MapReveal
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.domain.world.map.Routes
import io.github.lenticualrstone.civilight.domain.world.map.ShapeChange
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.change.RegionShapeValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD §4.7 maps (WLD-T53 … WLD-T57, WLD-T64, WLD-T65). */
class MapSpecTest {
    private val doc = ExampleProject.build()
    private val lower get() = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
    private fun place(key: Key) = PlaceRef(PlaceRefKind.PLACE, key)

    @Test
    fun `WLD-T53 route durations`() {
        val modes = Travel.modes(doc).associateBy { it.name }
        val walk = Travel.requiredMinutes(38.0, modes.getValue("도보"))
        val horse = Travel.requiredMinutes(38.0, modes.getValue("말"))
        val ship = Travel.requiredMinutes(38.0, modes.getValue("배"))
        assertEquals(3136.0, walk, 1e-9)
        assertEquals(456.0, horse, 1e-9)
        assertEquals(1824.0, ship, 1e-9)
        assertEquals("2일 4시간", Travel.formatDuration(walk))
        assertEquals("약 7시간 40분", Travel.formatDuration(horse))
        assertEquals("1일 6시간", Travel.formatDuration(ship))
        for (minutes in listOf(walk, horse, ship, 1440.0, 3600.0, 59.0)) assertTrue(!Travel.formatDuration(minutes).contains('.'))
    }

    @Test
    fun `WLD-T54 scale from days`() {
        assertEquals(120.0, MapScales.kmPerUnit(doc, lower)!!, 1e-3)
        val scale = MapScales.scale(Point(0.20f, 0.40f), Point(0.45f, 0.40f), 2.0, ScaleUnit.DAY, K.TRAVEL_WALK)
        assertEquals(30.0, MapScales.valueKm(doc, scale)!!, 1e-9)
        val route = lower.routes.single { it.key == K.ROUTE_GATE_TO_UPPER_GATE }
        assertEquals(0.316667, Routes.length(lower, route), 1e-5)
        assertEquals(38.0, Routes.km(doc, lower, route)!!, 0.01)
        val sheet = Routes.sheet(doc, lower, route)
        assertEquals("결사의 문 → 상층 관문", sheet.title)
        assertEquals("2일 4시간", sheet.duration)
        assertEquals("38 km · 지점 3", sheet.detail)
        assertEquals("결사의 문 → 상층 관문 · 도보 2일 4시간 · 38 km", sheet.mapLabel)
        assertEquals(listOf("도보 15 km · 8시간", "말", "배", "직접"), sheet.modeChips)
        assertEquals("1일 · 15 km", MapScales.barLabel(doc, lower))
    }

    @Test
    fun `WLD-T55 distance kinds`() {
        val d = Distances(doc)
        val same = d.distanceKm(place(K.PLACE_LOWER_STREET), place(K.PLACE_LOWER_STREET))
        assertEquals(DistanceKind.ZERO, same.kind)
        assertEquals(0.0, same.km!!, 0.0)
        val routed = d.distanceKm(place(K.PLACE_SOCIETY_GATE), place(K.PLACE_UPPER_GATE))
        assertEquals(DistanceKind.ROUTE, routed.kind)
        assertEquals(38.0, routed.km!!, 0.01)
        val straight = d.distanceKm(place(K.PLACE_LOWER_STREET), place(K.PLACE_SOCIETY_GATE))
        assertEquals(DistanceKind.STRAIGHT, straight.kind)
        assertEquals(MapGeometry.distance(Point(0.22f, 0.43f), Point(0.20f, 0.40f)) * 120.0, straight.km!!, 1e-3)
        // Across maps: 하층 거리 is on the sub-map of pin 상층; measured between 상층 and 송전탑 폐허 on 세계 지도.
        val world = doc.maps.getValue(K.MAP_WORLD)
        val across = d.distanceKm(place(K.PLACE_LOWER_STREET), place(K.PLACE_PYLON_RUINS))
        assertEquals(DistanceKind.STRAIGHT, across.kind)
        assertEquals(K.MAP_WORLD, across.mapKey)
        val worldKm = MapScales.kmPerUnit(doc, world)!!
        assertEquals(MapGeometry.distance(Point(0.55f, 0.35f), Point(0.75f, 0.30f)) * worldKm, across.km!!, 1e-3)

        // A map without scale.
        val unscaled = doc.copy(maps = doc.maps + (K.MAP_LOWER_DISTRICT to lower.copy(scale = null)))
        val noScale = Distances(unscaled).distanceKm(place(K.PLACE_LOWER_STREET), place(K.PLACE_SOCIETY_GATE))
        assertEquals(DistanceProblem.NO_SCALE, noScale.unavailable)
        assertNull(noScale.km)
        // Two places known only as nodes of the node map 잿더미 시장.
        var withNodes = doc
        val a = (WorldItems.createPlace(withNodes, "천막 골목", ctx()) as CreateResult.Created).also { withNodes = it.doc }.key
        val b = (WorldItems.createPlace(withNodes, "노점 뒤편", ctx(8)) as CreateResult.Created).also { withNodes = it.doc }.key
        val market = withNodes.maps.getValue(K.MAP_ASH_MARKET)
        withNodes = withNodes.copy(maps = withNodes.maps + (K.MAP_ASH_MARKET to market.copy(nodes = market.nodes.mapIndexed { i, n ->
            when (i) {
                1 -> n.copy(placeKey = a)
                2 -> n.copy(placeKey = b)
                else -> n
            }
        })))
        assertEquals(DistanceProblem.NO_SCALE, Distances(withNodes).distanceKm(place(a), place(b)).unavailable)
    }

    @Test
    fun `WLD-T56 reveal by point`() {
        // Given: also a pin of 송전탑 폐허 (2-3) on 하층 구역 지도, so the six pins of the case share one map.
        val pylon = Pin(key(560), K.PLACE_PYLON_RUINS, 0.50f, 0.20f, revealPoint = PointRef.unitStart(K.EPISODE_2_3))
        val given = doc.copy(maps = doc.maps + (K.MAP_LOWER_DISTRICT to lower.copy(pins = lower.pins + pylon)))
        val view = MapReveal.at(given, given.maps.getValue(K.MAP_LOWER_DISTRICT), LensPoint.UnitEnd(K.EPISODE_1_5))
        assertEquals("보임 · 핀 4 · 영역 1 | 아직 공개 전 · 2", view.summary)
        assertEquals(listOf("상층 관문 · 1-7", "송전탑 폐허 · 2-3"), view.pendingTags)
    }

    @Test
    fun `reveal by point on the fixture map alone`() {
        val view = MapReveal.at(doc, lower, LensPoint.UnitEnd(K.EPISODE_1_5))
        assertEquals("보임 · 핀 4 · 영역 1 | 아직 공개 전 · 1", view.summary)
        val atLatest = MapReveal.at(doc, lower, LensPoint.Latest)
        assertEquals("보임 · 핀 5 · 영역 1 | 아직 공개 전 · 0", atLatest.summary)
        // Hidden layer points apply too: the 길 layer from 1-9 hides both routes at 최신.
        val layered = lower.copy(layers = lower.layers.map { it.copy(revealPoint = PointRef.unitStart(K.EPISODE_1_9)) })
        val routes = MapReveal.at(doc, layered, LensPoint.Latest).items.filter { it.key in setOf(K.ROUTE_GATE_TO_UPPER_GATE, K.ROUTE_GATE_TO_MARKET) }
        assertTrue(routes.none { it.visible })
    }

    @Test
    fun `WLD-T57 region shape over time`() {
        val world = doc.maps.getValue(K.MAP_WORLD)
        val region = world.regions.single { it.key == K.REGION_UPPER_GUARD }
        val s2 = doc.index.record(K.REGION_SHAPE_1_9)!!.valueAs(RegionShapeValue.serializer())!!.vertices
        assertEquals(region.vertices, RegionShapes.shapeAt(doc, K.MAP_WORLD, region, LensPoint.Latest))
        assertEquals(s2, RegionShapes.shapeAt(doc, K.MAP_WORLD, region, LensPoint.UnitEnd(K.EPISODE_1_9)))
        val cmp = RegionShapes.compare(doc, K.MAP_WORLD, region, LensPoint.UnitEnd(K.EPISODE_1_7), LensPoint.UnitEnd(K.EPISODE_1_9))
        assertEquals(region.vertices, cmp.before)
        assertEquals(s2, cmp.after)
        assertEquals(ShapeChange.GREW, cmp.change)
        // S1 lies inside S2, so S2∖S1 is the ring the renderer dots.
        assertTrue(cmp.before.all { MapGeometry.contains(cmp.after, it) })
        assertEquals("상층 경비대" to "영역 · 1-9 ◆ 넓어짐", RegionShapes.caption(doc, region, cmp, LensPoint.UnitEnd(K.EPISODE_1_9)))
    }

    private fun hexDoc(): Pair<ProjectDoc, Key> {
        val created = MapEdits.newMap(doc, MapBaseKind.GENERATED, ctx(), name = "육각 시험") as MapEdit.Applied
        val rows = 6
        val cols = 8
        // A forest group of 5 cells in row 1 (cols 1..5), plain around it, other forest at row 4.
        val forest = setOf(1 * cols + 1, 1 * cols + 2, 1 * cols + 3, 1 * cols + 4, 1 * cols + 5, 4 * cols + 2, 4 * cols + 3)
        val grid = HexGrid(cols = cols, rows = rows, cells = List(rows * cols) { if (it in forest) Terrain.FOREST.open() else Terrain.PLAIN.open() })
        val map = created.value.copy(hex = grid)
        return created.doc.copy(maps = created.doc.maps + (map.key to map)) to map.key
    }

    @Test
    fun `WLD-T64 hex flood fill`() {
        val (given, mapKey) = hexDoc()
        val (filled, cells) = HexMaps.fill(given, mapKey, HexCell(1, 3), Terrain.PLAIN, ctx())!!
        assertEquals(setOf(HexCell(1, 1), HexCell(1, 2), HexCell(1, 3), HexCell(1, 4), HexCell(1, 5)), cells.toSet())
        assertEquals(HexCell(1, 3), cells.first())
        val grid = filled.maps.getValue(mapKey).hex!!
        assertEquals(5, grid.overrides.size)
        assertEquals(Terrain.FOREST, HexMaps.terrainAt(grid, 4 * 8 + 2))
        assertEquals(Terrain.PLAIN, HexMaps.terrainAt(grid, 1 * 8 + 1))
    }

    @Test
    fun `WLD-T65 hex coordinates`() {
        val grid = HexGrid(cols = 32, rows = 24)
        assertEquals("O09", HexMaps.label(HexCell(14, 9)))
        assertEquals("A00", HexMaps.label(HexCell(0, 0)))
        assertTrue(HexMaps.showsLabel(grid, HexCell(3, 6)))
        assertTrue(!HexMaps.showsLabel(grid, HexCell(14, 9)))
        val labelled = (0 until 24).flatMap { r -> (0 until 32).map { c -> HexCell(r, c) } }.count { HexMaps.showsLabel(grid, it) }
        assertEquals(8 * 11, labelled)
    }

    @Test
    fun `place picker lists scene counts and picks on the map`() {
        val picker = io.github.lenticualrstone.civilight.domain.world.item.PlacePicker(doc)
        assertEquals("잿더미 시장 · 씬 4개", picker.rows("잿더미").single().text)
        assertEquals("하층 구역 · 씬 1개", picker.rows(regions = true).single { it.name == "하층 구역" }.text)
        assertEquals(place(K.PLACE_ASH_MARKET), picker.pickAt(K.MAP_LOWER_DISTRICT, Point(0.451f, 0.401f)))
        assertEquals(PlaceRef(PlaceRefKind.REGION, K.REGION_LOWER_DISTRICT), picker.pickAt(K.MAP_LOWER_DISTRICT, Point(0.30f, 0.50f), regions = true))
        assertNull(picker.pickAt(K.MAP_LOWER_DISTRICT, Point(0.9f, 0.9f)))
        assertEquals("씬 4개 → 잿더미 시장", picker.confirmBar(4, place(K.PLACE_ASH_MARKET)))
        assertEquals("새 장소 · 지도에 핀을 꽂으며 만들기", io.github.lenticualrstone.civilight.domain.world.item.PlacePicker.NEW_PLACE_ROW)
    }

    @Test
    fun `hex cell info and place creation at a cell`() {
        val (given, mapKey) = hexDoc()
        val map = given.maps.getValue(mapKey)
        val info = HexMaps.info(given, map, HexCell(1, 2))!!
        assertEquals("B02 | 숲 | 칸 24 km · 장소 없음", info.text)
        val made = HexMaps.createPlaceAt(given, mapKey, HexCell(1, 2), "숲속 오두막", ctx()) as MapEdit.Applied
        val after = HexMaps.info(made.doc, made.doc.maps.getValue(mapKey), HexCell(1, 2))!!
        assertEquals("숲속 오두막", after.placeName)
        val pin = made.value
        assertEquals(HexMaps.center(map.hex!!, HexCell(1, 2)), Point(pin.x, pin.y))
    }
}
