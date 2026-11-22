package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.AttachedLayer
import io.github.lenticualrstone.civilight.model.map.DrawnLine
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.GeneratedMapRecord
import io.github.lenticualrstone.civilight.model.map.GenerationStep
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.LineOrigin
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapLabel
import io.github.lenticualrstone.civilight.model.map.MapLayer
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.map.MapRegion
import io.github.lenticualrstone.civilight.model.map.MapScale
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.map.NodeMapLink
import io.github.lenticualrstone.civilight.model.map.NodeMapNode
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.Route
import io.github.lenticualrstone.civilight.model.map.RoutePoint
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import io.github.lenticualrstone.civilight.model.map.Stamp
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.LAST_EDIT
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.at
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * The example maps: 세계 지도 (hex, generated) › 하층 구역 지도 (detail, generated; scale 2일 도보 for
 * 0.25, so 120 km per unit) › 잿더미 시장 (node map).
 */
internal object ExampleMaps {
    /** Base shape S1 of region 상층 경비대; the `◆` at 1-9 holds [UPPER_GUARD_SHAPE_1_9]. */
    val UPPER_GUARD_SHAPE: List<Point> = listOf(Point(0.50f, 0.25f), Point(0.62f, 0.25f), Point(0.62f, 0.40f), Point(0.50f, 0.40f))
    val UPPER_GUARD_SHAPE_1_9: List<Point> = listOf(Point(0.48f, 0.22f), Point(0.66f, 0.22f), Point(0.66f, 0.44f), Point(0.48f, 0.44f))

    /** Pin coordinates on 하층 구역 지도. */
    val SOCIETY_GATE_AT = Point(0.20f, 0.40f)
    val LOWER_STREET_AT = Point(0.22f, 0.43f)
    val ASH_MARKET_AT = Point(0.45f, 0.40f)
    val ARCHIVE_ROOM_AT = Point(0.26f, 0.36f)
    val UPPER_GATE_AT = Point(0.416667f, 0.30f)

    /** Bend of the 결사의 문 → 상층 관문 route; total length 0.316667 = 38 km. */
    val GATE_ROUTE_BEND = Point(0.20f, 0.30f)

    /** km per map unit on 하층 구역 지도: 2 days × 15 km over 0.25. */
    const val LOWER_DISTRICT_KM_PER_UNIT = 120.0

    private val GENERATED_AT: Long = at("2026-03-02T11:00:00Z")

    private fun pin(key: Key, place: Key, at: Point, reveal: PointRef? = null) =
        Pin(key = key, placeKey = place, x = at.x, y = at.y, revealPoint = reveal, updatedAt = LAST_EDIT, updatedBy = DEVICE)

    private fun steps(vararg seeds: Int): List<GenerationStep> =
        listOf(
            GenerationStepKind.COAST, GenerationStepKind.MOUNTAIN, GenerationStepKind.RIVER,
            GenerationStepKind.FOREST_CLIMATE, GenerationStepKind.TOWN_ROAD,
        ).zip(seeds.toList()) { kind, seed -> GenerationStep(step = kind.open(), seed = seed, locked = kind == GenerationStepKind.COAST) }

    fun maps(): List<WorldMap> {
        val orders = OrderKeys.evenlySpaced(3)
        val world = WorldMap(
            key = K.MAP_WORLD, order = orders[0], name = "세계 지도",
            baseKind = MapBaseKind.GENERATED.open(), format = MapFormat.HEX.open(), style = MapStyle.PARCHMENT.open(),
            contours = true,
            scale = MapScale(a = Point(0.10f, 0.50f), b = Point(0.90f, 0.50f), value = 400.0, unit = ScaleUnit.KM.open()),
            layers = listOf(MapLayer(key = K.mapLayer(1), order = OrderKeys.FIRST, name = "정치", updatedAt = LAST_EDIT, updatedBy = DEVICE)),
            pins = listOf(
                pin(K.PIN_WORLD_UPPER_CITY, K.PLACE_UPPER_CITY, Point(0.55f, 0.35f)),
                pin(K.PIN_WORLD_PYLON_RUINS, K.PLACE_PYLON_RUINS, Point(0.75f, 0.30f), PointRef.unitStart(K.EPISODE_2_3)),
            ),
            regions = listOf(
                MapRegion(
                    key = K.REGION_UPPER_GUARD, name = "상층 경비대", kind = "세력 영역", factionKey = K.FACTION_UPPER_GUARD,
                    color = "map-art-red", layerKey = K.mapLayer(1), revealPoint = PointRef.unitStart(K.EPISODE_1_7),
                    vertices = UPPER_GUARD_SHAPE, updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
            ),
            labels = listOf(MapLabel(key = K.mapLabel(1), text = "잿빛 평원", x = 0.30f, y = 0.62f, updatedAt = LAST_EDIT, updatedBy = DEVICE)),
            stamps = listOf(
                Stamp(
                    key = K.stamp(1), kind = StampKind.RUIN.open(), x = 0.75f, y = 0.30f, placeKey = K.PLACE_PYLON_RUINS,
                    updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
                Stamp(key = K.stamp(2), kind = StampKind.MOUNTAIN.open(), x = 0.20f, y = 0.20f, size = 3, rotation = 15, updatedAt = LAST_EDIT, updatedBy = DEVICE),
            ),
            hex = HexGrid(
                cellKm = 24.0, cols = 6, rows = 4, coordEvery = 3,
                cells = List(24) { i ->
                    when {
                        i < 6 -> Terrain.SEA
                        i % 6 == 0 -> Terrain.HILL
                        i % 5 == 0 -> Terrain.MOUNTAIN
                        else -> Terrain.PLAIN
                    }.open()
                },
                overrides = mapOf(13 to Terrain.FOREST.open()),
            ),
            generation = GeneratedMapRecord(
                generatorVersion = "gen-1", kind = GeneratedMapKind.CONTINENT.open(), landRatio = LandRatio.HALF.open(),
                steps = steps(4827, 112, 31, 907, 215), result = ExampleMapResults.world,
            ),
            updatedAt = GENERATED_AT, updatedBy = DEVICE,
        )
        val lower = WorldMap(
            key = K.MAP_LOWER_DISTRICT, order = orders[1], name = "하층 구역 지도",
            parent = MapParent(mapKey = K.MAP_WORLD, pinKey = K.PIN_WORLD_UPPER_CITY),
            baseKind = MapBaseKind.GENERATED.open(), format = MapFormat.DETAIL.open(), style = MapStyle.INK.open(),
            scale = MapScale(a = SOCIETY_GATE_AT, b = ASH_MARKET_AT, value = 2.0, unit = ScaleUnit.DAY.open(), travelModeKey = K.TRAVEL_WALK),
            revealViewDim = true,
            layers = listOf(MapLayer(key = K.mapLayer(2), order = OrderKeys.FIRST, name = "길", updatedAt = LAST_EDIT, updatedBy = DEVICE)),
            pins = listOf(
                pin(K.PIN_SOCIETY_GATE, K.PLACE_SOCIETY_GATE, SOCIETY_GATE_AT),
                pin(K.PIN_LOWER_STREET, K.PLACE_LOWER_STREET, LOWER_STREET_AT, PointRef.unitStart(K.EPISODE_1_1)),
                pin(K.PIN_ASH_MARKET, K.PLACE_ASH_MARKET, ASH_MARKET_AT, PointRef.unitStart(K.EPISODE_1_3)),
                pin(K.PIN_ARCHIVE_ROOM, K.PLACE_ARCHIVE_ROOM, ARCHIVE_ROOM_AT, PointRef.unitStart(K.EPISODE_1_5)),
                pin(K.PIN_UPPER_GATE, K.PLACE_UPPER_GATE, UPPER_GATE_AT, PointRef.unitStart(K.EPISODE_1_7)),
            ),
            regions = listOf(
                MapRegion(
                    key = K.REGION_LOWER_DISTRICT, name = "하층 구역", kind = "지역", color = "map-art-ash",
                    revealPoint = PointRef.unitStart(K.EPISODE_1_1),
                    vertices = listOf(Point(0.10f, 0.31f), Point(0.55f, 0.31f), Point(0.55f, 0.60f), Point(0.10f, 0.60f)),
                    updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
            ),
            routes = listOf(
                Route(
                    key = K.ROUTE_GATE_TO_UPPER_GATE,
                    points = listOf(RoutePoint.pin(K.PIN_SOCIETY_GATE), RoutePoint.at(GATE_ROUTE_BEND.x, GATE_ROUTE_BEND.y), RoutePoint.pin(K.PIN_UPPER_GATE)),
                    layerKey = K.mapLayer(2), shownModeKey = K.TRAVEL_WALK, updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
                Route(
                    key = K.ROUTE_GATE_TO_MARKET,
                    points = listOf(RoutePoint.pin(K.PIN_SOCIETY_GATE), RoutePoint.at(0.33f, 0.44f), RoutePoint.pin(K.PIN_ASH_MARKET)),
                    layerKey = K.mapLayer(2), updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
            ),
            labels = listOf(MapLabel(key = K.mapLabel(2), text = "하층 구역", x = 0.30f, y = 0.55f, updatedAt = LAST_EDIT, updatedBy = DEVICE)),
            stamps = listOf(
                Stamp(
                    key = K.stamp(3), kind = StampKind.TOWN.open(), x = 0.45f, y = 0.40f, placeKey = K.PLACE_ASH_MARKET,
                    updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
                Stamp(
                    key = K.stamp(4), kind = StampKind.TOWER.open(), x = 0.42f, y = 0.28f, size = 1, placeKey = K.PLACE_UPPER_GATE,
                    attachedLayer = AttachedLayer.ROAD.open(), updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
            ),
            drawnLines = listOf(
                DrawnLine(
                    key = K.drawnLine(1), kind = DrawnLineKind.ROAD.open(),
                    points = listOf(SOCIETY_GATE_AT, LOWER_STREET_AT, ASH_MARKET_AT),
                    origin = LineOrigin.GENERATED.open(), updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
            ),
            generation = GeneratedMapRecord(
                generatorVersion = "gen-1", kind = GeneratedMapKind.REGION.open(), landRatio = LandRatio.LAND_MORE.open(),
                steps = steps(5120, 77, 3021, 18, 640), result = ExampleMapResults.lowerDistrict,
            ),
            updatedAt = LAST_EDIT, updatedBy = DEVICE,
        )
        val market = WorldMap(
            key = K.MAP_ASH_MARKET, order = orders[2], name = "잿더미 시장",
            parent = MapParent(mapKey = K.MAP_LOWER_DISTRICT, pinKey = K.PIN_ASH_MARKET),
            baseKind = MapBaseKind.NODE.open(), format = MapFormat.NODE.open(),
            nodes = listOf(
                NodeMapNode(key = K.mapNode(1), name = "시장 입구", placeKey = K.PLACE_ASH_MARKET, x = 0.20f, y = 0.50f, updatedAt = LAST_EDIT, updatedBy = DEVICE),
                NodeMapNode(key = K.mapNode(2), name = "오웬의 노점", sub = "밀수상", x = 0.50f, y = 0.40f, updatedAt = LAST_EDIT, updatedBy = DEVICE),
                NodeMapNode(
                    key = K.mapNode(3), name = "뒷골목", x = 0.80f, y = 0.60f, revealPoint = PointRef.unitStart(K.EPISODE_1_6),
                    updatedAt = LAST_EDIT, updatedBy = DEVICE,
                ),
            ),
            nodeLinks = listOf(
                NodeMapLink(key = K.mapNodeLink(1), aNodeKey = K.mapNode(1), bNodeKey = K.mapNode(2), label = "천막 길", updatedAt = LAST_EDIT, updatedBy = DEVICE),
                NodeMapLink(key = K.mapNodeLink(2), aNodeKey = K.mapNode(2), bNodeKey = K.mapNode(3), updatedAt = LAST_EDIT, updatedBy = DEVICE),
            ),
            updatedAt = LAST_EDIT, updatedBy = DEVICE,
        )
        return listOf(world, lower, market)
    }
}
