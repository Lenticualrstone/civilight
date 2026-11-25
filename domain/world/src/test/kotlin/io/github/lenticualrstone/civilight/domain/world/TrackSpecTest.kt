package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.item.FieldRow
import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.domain.world.map.MapReveal
import io.github.lenticualrstone.civilight.domain.world.time.SceneCastSource
import io.github.lenticualrstone.civilight.domain.world.time.SceneTimeSource
import io.github.lenticualrstone.civilight.domain.world.time.SceneWhen
import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.domain.world.track.CharacterTrack
import io.github.lenticualrstone.civilight.domain.world.track.CharacterTracks
import io.github.lenticualrstone.civilight.domain.world.track.PlaceTimeRow
import io.github.lenticualrstone.civilight.domain.world.track.PlaceTimes
import io.github.lenticualrstone.civilight.domain.world.track.SkipReason
import io.github.lenticualrstone.civilight.domain.world.track.TimelineMaps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/** WLD §4.7 map ↔ timeline (WLD-T58 … WLD-T60) and WLD-T45, with place and faction cards. */
class TrackSpecTest {
    private val base = ExampleProject.build()

    private fun withScenes(doc: ProjectDoc, f: (io.github.lenticualrstone.civilight.model.story.StoryUnit) -> io.github.lenticualrstone.civilight.model.story.StoryUnit) =
        doc.copy(story = doc.story.copy(units = doc.story.units.map(f)))

    private fun placeOf(doc: ProjectDoc, sceneKey: Key, place: Key?): ProjectDoc =
        withScenes(doc) { if (it.key == sceneKey) it.copy(place = place?.let { k -> PlaceRef(PlaceRefKind.PLACE, k) }) else it }

    private val rienScenes = listOf(K.SCENE_1_2_1, K.SCENE_1_3_2, K.SCENE_1_4_2, K.SCENE_1_6_1, K.SCENE_1_7_3, K.SCENE_1_8_1)

    /** WLD-T58's Given: 1-8 씬 1 without place, no route among the places, 리엔 only in the listed scenes. */
    private fun t58(): Triple<ProjectDoc, CharacterTracks, CharacterTrack> {
        var doc = placeOf(base, K.SCENE_1_8_1, null)
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        doc = doc.copy(maps = doc.maps + (lower.key to lower.copy(routes = lower.routes.filterNot { it.key == K.ROUTE_GATE_TO_MARKET })))
        val tracks = CharacterTracks(doc, StoryEngine(doc), SceneCastSource.of(rienScenes.associateWith { listOf(K.RIEN) }))
        return Triple(doc, tracks, tracks.track(K.RIEN)!!)
    }

    @Test
    fun `WLD-T58 character track`() {
        val (_, tracks, track) = t58()
        assertEquals(listOf(K.SCENE_1_2_1, K.SCENE_1_3_2, K.SCENE_1_6_1, K.SCENE_1_7_3), track.stops.map { it.sceneKey })
        assertEquals(mapOf(K.SCENE_1_4_2 to SkipReason.FLASHBACK, K.SCENE_1_8_1 to SkipReason.NO_PLACE), track.skipped)
        assertEquals(3, track.legs.size)
        assertTrue(track.legs.all { it.estimated && it.label == CharacterTrack.ESTIMATED })
        assertEquals("결사의 문", tracks.positionAt(track, LensPoint.UnitEnd(K.EPISODE_1_5))!!.placeName)
        assertEquals("리엔 · 씬 순서", track.listTitle)
        assertEquals(Triple("1-2", "하층 거리", "씬 1 · 1일째 새벽"), track.stops.first().row)
        assertEquals(listOf("1-2", "1-3", "1-6", "1-7"), track.stops.map { it.episodeLabel })
    }

    @Test
    fun `track legs follow a route when one joins the two places`() {
        val tracks = CharacterTracks(base, StoryEngine(base), SceneCastSource.of(rienScenes.associateWith { listOf(K.RIEN) }))
        val track = tracks.track(K.RIEN)!!
        val gateToMarket = track.legs.single { it.from.sceneKey == K.SCENE_1_3_2 }
        assertEquals(K.ROUTE_GATE_TO_MARKET, gateToMarket.routeKey)
        assertFalse(gateToMarket.estimated)
        assertNull(gateToMarket.label)
        // In the fixture 1-8 씬 1 is at 송전탑 폐허 (fixture conflict 4), so it is a stop.
        assertEquals(K.SCENE_1_8_1, track.stops.last().sceneKey)
    }

    @Test
    fun `while locked a leg never follows a route through a hidden place's pin`() {
        var doc = base.copy(world = base.world.copy(places = base.world.places.map { if (it.key == K.PLACE_ARCHIVE_ROOM) it.copy(hidden = true) else it }))
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        val through = lower.routes.map { r ->
            if (r.key != K.ROUTE_GATE_TO_MARKET) r
            else r.copy(points = listOf(r.points.first(), io.github.lenticualrstone.civilight.model.map.RoutePoint.pin(K.PIN_ARCHIVE_ROOM), r.points.last()))
        }
        doc = doc.copy(maps = doc.maps + (lower.key to lower.copy(routes = through)))
        val cast = SceneCastSource.of(rienScenes.associateWith { listOf(K.RIEN) })
        val locked = CharacterTracks(doc, StoryEngine(doc), cast).track(K.RIEN)!!.legs.single { it.from.sceneKey == K.SCENE_1_3_2 }
        assertNull(locked.routeKey)
        assertTrue(locked.estimated)
        assertEquals(CharacterTrack.ESTIMATED, locked.label)
        val open = CharacterTracks(doc, StoryEngine(doc), cast, hiddenUnlocked = true).track(K.RIEN)!!.legs.single { it.from.sceneKey == K.SCENE_1_3_2 }
        assertEquals(K.ROUTE_GATE_TO_MARKET, open.routeKey)
    }

    @Test
    fun `track from the real cast skips deleted places and dots hidden scenes`() {
        var doc = WorldFixture.withTerm(base, K.TERM_BLACKOUT) { it } // no-op copy
        doc = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(deletedAt = 1L) else it }))
        doc = withScenes(doc) { if (it.key == K.SCENE_1_3_2) it.copy(hidden = true) else it }
        val engine = StoryEngine(doc)
        val track = CharacterTracks(doc, engine, engine).track(K.RIEN)!!
        assertEquals(SkipReason.DELETED_PLACE, track.skipped[K.SCENE_1_6_1])
        assertEquals(SkipReason.HIDDEN_SCENE, track.skipped[K.SCENE_1_3_2])
        assertTrue(track.stops.none { it.sceneKey == K.SCENE_1_6_1 || it.sceneKey == K.SCENE_1_3_2 })
        val dotted = track.legs.filter { it.dotted }
        assertEquals(1, dotted.size)
        assertEquals(CharacterTrack.HIDDEN_SCENE, dotted.single().label)
        val unlocked = CharacterTracks(doc, engine, engine, hiddenUnlocked = true).track(K.RIEN)!!
        assertTrue(unlocked.stops.any { it.sceneKey == K.SCENE_1_3_2 })
        // A hidden character has no track while locked.
        val hiddenRien = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
        assertNull(CharacterTracks(hiddenRien, engine, engine).track(K.RIEN))
    }

    @Test
    fun `WLD-T59 track order follows time`() {
        val doc = placeOf(base, K.SCENE_1_6_2, K.PLACE_LOWER_STREET)
        val times = SceneTimeSource.overriding(StoryEngine(doc), mapOf(K.SCENE_1_6_2 to SceneWhen.Story(5, 5)))
        val cast = SceneCastSource.of(mapOf(K.SCENE_1_6_1 to listOf(K.RIEN), K.SCENE_1_6_2 to listOf(K.RIEN)))
        val track = CharacterTracks(doc, times, cast).track(K.RIEN)!!
        assertEquals(listOf(K.SCENE_1_6_2, K.SCENE_1_6_1), track.stops.map { it.sceneKey })
    }

    /** WLD-T60's Given: the listed scenes are the only ones at 하층 거리. */
    private fun t60(): ProjectDoc {
        var doc = base
        for (s in listOf(K.SCENE_1_1_1, K.SCENE_1_2_2, K.SCENE_1_5_2)) doc = placeOf(doc, s, null)
        return doc
    }

    private fun rowTexts(rows: List<PlaceTimeRow>): List<String> = rows.map {
        when (it) {
            is PlaceTimeRow.EventRow -> "${it.time} · ${it.title}"
            is PlaceTimeRow.SceneRow -> "${it.time} · ${it.title}"
            is PlaceTimeRow.Divider -> it.text
        }
    }

    @Ignore("spec conflict: the fixture titles 1-2 씬 1 `새벽 순찰`, the case expects `리엔의 새벽 순찰` (WLD Q-15)")
    @Test
    fun `WLD-T60 place time`() {
        val doc = t60()
        val engine = StoryEngine(doc)
        val rows = PlaceTimes(doc, engine, engine).rows(K.PLACE_LOWER_STREET, LensPoint.UnitEnd(K.EPISODE_1_7))
        assertEquals(
            listOf(
                "1195 · 대정전",
                "1-4 씬 2 · 세라의 회상",
                "1-2 씬 1 · 리엔의 새벽 순찰",
                "1-7 씬 1 · 첫 번째 깜박임",
                "시점 1-7 · 이 아래는 아직",
                "2-3 씬 2 · 경비대 검문",
            ),
            rowTexts(rows),
        )
    }

    @Test
    fun `place time on the fixture titles`() {
        val doc = t60()
        val engine = StoryEngine(doc)
        val rows = PlaceTimes(doc, engine, engine).rows(K.PLACE_LOWER_STREET, LensPoint.UnitEnd(K.EPISODE_1_7))
        assertEquals(
            listOf(
                "1195 · 대정전",
                "1-4 씬 2 · 세라의 회상",
                "1-2 씬 1 · 새벽 순찰",
                "1-7 씬 1 · 첫 번째 깜박임",
                "시점 1-7 · 이 아래는 아직",
                "2-3 씬 2 · 경비대 검문",
            ),
            rowTexts(rows),
        )
        val scenes = rows.filterIsInstance<PlaceTimeRow.SceneRow>()
        assertEquals("1일째 새벽 · 세라 · 리엔", scenes.single { it.sceneKey == K.SCENE_1_2_1 }.sub)
        assertEquals("대정전 그날 · 세라 · 리엔", scenes.single { it.sceneKey == K.SCENE_1_4_2 }.sub)
        assertEquals("계획 · 하르트", scenes.single { it.sceneKey == K.SCENE_2_3_2 }.sub)
        assertTrue(scenes.single { it.sceneKey == K.SCENE_2_3_2 }.dimmed)
        assertTrue(scenes.filter { it.sceneKey != K.SCENE_2_3_2 }.none { it.dimmed })
        val event = rows.filterIsInstance<PlaceTimeRow.EventRow>().single()
        assertTrue(event.sub, event.sub.startsWith("세계 사건 · "))
        // At 최신 the divider names the 집필 선's episode.
        val latest = PlaceTimes(doc, engine, engine).rows(K.PLACE_LOWER_STREET)
        assertTrue(rowTexts(latest).contains("시점 1-7 · 이 아래는 아직"))
    }

    @Test
    fun `timeline and map highlight each event's place`() {
        val engine = StoryEngine(base)
        val tm = TimelineMaps(base, engine)
        val highlight = tm.highlight(K.EVENT_BLACKOUT).single()
        assertEquals(K.MAP_LOWER_DISTRICT, highlight.mapKey)
        assertEquals(listOf(K.PIN_LOWER_STREET), highlight.pinKeys)
        val rows = tm.rows()
        assertEquals(listOf(K.EVENT_BLACKOUT, K.EVENT_JOINS_SOCIETY, K.EVENT_GRID_ACCIDENT), rows.map { it.eventKey })
        val joins = rows[1]
        assertEquals("1200 · 1-3", joins.time)
        assertEquals("씬 2 · 1일째 아침", joins.sub)
        assertEquals("결사의 문", joins.placeName)
        val change = tm.regionChanges().single()
        assertEquals(K.REGION_UPPER_GUARD, change.regionKey)
        assertEquals("1-9", change.pointLabel)
        val (cmp, caption) = tm.compare(K.MAP_WORLD, K.REGION_UPPER_GUARD, LensPoint.UnitEnd(K.EPISODE_1_7), LensPoint.UnitEnd(K.EPISODE_1_9))!!
        assertTrue(cmp.changed)
        assertEquals("상층 경비대" to "영역 · 1-9 ◆ 넓어짐", caption)
        assertEquals("점선 = 1-9부터 더해진 곳", tm.dottedNote(LensPoint.UnitEnd(K.EPISODE_1_9)))
    }

    @Test
    fun `WLD-T45 hidden place on a map`() {
        val doc = base.copy(world = base.world.copy(places = base.world.places.map { if (it.key == K.PLACE_ARCHIVE_ROOM) it.copy(hidden = true) else it }))
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        val locked = MapReveal.at(doc, lower, LensPoint.Latest, hiddenUnlocked = false)
        assertTrue(locked.items.none { it.key == K.PIN_ARCHIVE_ROOM })
        assertEquals("숨긴 장소 1", WorldCards(doc).placeList().lockRow)
        assertTrue(WorldCards(doc).placeList().sections.flatMap { it.rows }.none { it.key == K.PLACE_ARCHIVE_ROOM })
        val open = MapReveal.at(doc, lower, LensPoint.Latest, hiddenUnlocked = true)
        assertTrue(open.items.any { it.key == K.PIN_ARCHIVE_ROOM })
        assertNull(WorldCards(doc, hiddenUnlocked = true).placeList().lockRow)
    }

    @Test
    fun `place and faction cards`() {
        val cards = WorldCards(base)
        val street = cards.placeCard(K.PLACE_LOWER_STREET)!!
        assertEquals("1-1 · 1-2 외 4", street.appearance)
        assertEquals(listOf(FieldRow(K.FIELD_PLACE_STATE, "상태", "대정전 이후", changed = true)), street.fields)
        assertEquals(listOf("개요", "이곳의 시간", "지도"), street.tabs)
        val archive = cards.placeCard(K.PLACE_ARCHIVE_ROOM)!!
        assertEquals("장소 · 결사의 문", archive.eyebrow)
        assertEquals("결사의 문", archive.parentName)
        val list = cards.placeList()
        assertEquals(null, list.sections.first().title)
        assertTrue(list.sections.any { it.title == "결사의 문" && it.rows.any { r -> r.key == K.PLACE_ARCHIVE_ROOM } })
        val guard = cards.factionCard(K.FACTION_UPPER_GUARD)!!
        assertEquals(listOf("상층 경비대 · 세계 지도"), guard.areas)
        assertEquals("카일", guard.fields.single().value)
        assertEquals("인물 0", cards.factionList().sections.single().rows.first().right)
    }
}
