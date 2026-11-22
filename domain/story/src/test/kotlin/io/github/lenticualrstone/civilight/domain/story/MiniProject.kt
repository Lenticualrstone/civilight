package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.CastFile
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.file.WorldFile
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapScale
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.Route
import io.github.lenticualrstone.civilight.model.map.RoutePoint
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.TravelMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.world.Place

/**
 * A small separate fixture for travel cases (TRV-09 … TRV-13): one chapter, one episode, scenes W
 * (day 1, nobody), X and Y with 세라 (silent cast), two places [distanceKm] apart on one map with a
 * km scale (100 km per unit).
 */
object MiniProject {
    const val PROJECT = "01JZ000000PRJT000000000077"
    const val CHAPTER = "01JZ000000CHAP000000000077"
    const val EPISODE = "01JZ000000EPSD000000007701"
    const val W = "01JZ000000SCNE000000770101"
    const val X = "01JZ000000SCNE000000770102"
    const val Y = "01JZ000000SCNE000000770103"
    const val SERA = "01JZ000000CHAR000000000077"
    const val PLACE_A = "01JZ000000P1CE000000007701"
    const val PLACE_B = "01JZ000000P1CE000000007702"
    const val MAP = "01JZ000000MAPS000000007701"
    const val PIN_A = "01JZ000000P1NS000000007701"
    const val PIN_B = "01JZ000000P1NS000000007702"
    const val WALK = "01JZ000000TRVM000000007701"

    fun build(
        distanceKm: Double,
        x: SceneTime,
        y: SceneTime,
        route: Boolean = true,
        scaled: Boolean = true,
        nodeMap: Boolean = false,
        samePlace: Boolean = false,
    ): ProjectDoc {
        val modes = TravelMode.defaults(newKey = sequenceOf(WALK, "01JZ000000TRVM000000007702", "01JZ000000TRVM000000007703").iterator()::next)
        val project = Project(key = PROJECT, name = "이동 점검", travelModes = modes)
        val orders = OrderKeys.evenlySpaced(3)
        val a = PlaceRef(PlaceRefKind.PLACE, PLACE_A)
        val b = if (samePlace) a else PlaceRef(PlaceRefKind.PLACE, PLACE_B)
        val units = listOf(
            StoryUnit(key = CHAPTER, level = 0, order = OrderKeys.FIRST, title = "장"),
            StoryUnit(key = EPISODE, parentKey = CHAPTER, level = 1, order = OrderKeys.FIRST, nextBlockNumber = 1),
            StoryUnit(key = W, parentKey = EPISODE, level = 2, order = orders[0], time = SceneTime.Direct(storyDay = 1)),
            StoryUnit(key = X, parentKey = EPISODE, level = 2, order = orders[1], place = a, time = x, silentCast = listOf(SERA)),
            StoryUnit(key = Y, parentKey = EPISODE, level = 2, order = orders[2], place = b, time = y, silentCast = listOf(SERA)),
        )
        val ax = 0.1f
        val bx = (0.1 + distanceKm / 100.0).toFloat()
        val map = WorldMap(
            key = MAP, order = OrderKeys.FIRST, name = "지도",
            baseKind = (if (nodeMap) MapBaseKind.NODE else MapBaseKind.SKETCH).open(),
            format = if (nodeMap) MapFormat.NODE.open() else null,
            scale = if (scaled) MapScale(Point(0f, 0.5f), Point(1f, 0.5f), 100.0, ScaleUnit.KM.open()) else null,
            pins = listOf(Pin(PIN_A, PLACE_A, ax, 0.5f), Pin(PIN_B, PLACE_B, bx, 0.5f)),
            routes = if (route) listOf(Route("01JZ000000RTES000000007701", listOf(RoutePoint.pin(PIN_A), RoutePoint.pin(PIN_B)))) else emptyList(),
        )
        return ProjectDoc(
            project = project,
            story = StoryIndexFile(units = units),
            cast = CastFile(characters = listOf(Character(key = SERA, recordNo = 1, name = "세라", order = OrderKeys.FIRST))),
            world = WorldFile(places = listOf(Place(PLACE_A, orders[0], "기록실"), Place(PLACE_B, orders[1], "잿더미 시장"))),
            maps = mapOf(MAP to map),
        )
    }

    fun key(k: Key) = k
}
