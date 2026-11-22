package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-64 연표 + 지도 and WLD-65 region comparison on the example project. */
class TimelineMapStateTest {
    private val doc = WorldTestFixture.stateB()

    @Test
    fun `WLD-64 rows in time order and the selected event lights its pin`() {
        val ui = TimelineMapState.build(doc, K.EVENT_JOINS_SOCIETY, null, CompareMode.NOW, hiddenOpen = false)
        assertEquals("사건마다 장소 · 고르면 지도에서 강조", ui.hint)
        val titles = ui.rows.map { it.title }
        assertTrue(titles.toString(), titles.indexOf("대정전") < titles.indexOf("리엔, 결사 합류"))
        val row = ui.rows.single { it.eventKey == K.EVENT_JOINS_SOCIETY }
        assertTrue(row.selected)
        assertEquals("결사의 문", row.place)
        val lit = ui.pins.filter { it.highlighted }
        assertEquals(listOf("결사의 문"), lit.map { it.name })
        assertTrue(ui.maps.size >= 2)
    }

    @Test
    fun `no selection shows the first top-level map with nothing lit`() {
        val ui = TimelineMapState.build(doc, null, null, CompareMode.NOW, hiddenOpen = false)
        assertNotNull(ui.mapKey)
        assertTrue(ui.pins.none { it.highlighted })
        assertTrue(ui.regions.none { it.highlighted })
    }

    @Test
    fun `WLD-65 the region with a ◆ shape offers the comparison chips and the dotted note`() {
        val now = TimelineMapState.build(doc, null, ExampleKeys.MAP_WORLD, CompareMode.NOW, hiddenOpen = false)
        val compare = now.compare
        assertNotNull(now.toString(), compare)
        assertEquals(3, compare!!.chips.size)
        assertTrue(compare.chips[1], compare.chips[1].endsWith("◆"))
        assertEquals("비교", compare.chips[2])
        assertEquals(null, compare.dottedNote)
        val both = TimelineMapState.build(doc, null, ExampleKeys.MAP_WORLD, CompareMode.BOTH, hiddenOpen = false)
        assertTrue(both.compare!!.dottedNote!!.startsWith("점선 = "))
        assertTrue(both.regions.any { it.added != null })
    }

    @Test
    fun `WLD-13 a hidden place is masked and not lit while locked`() {
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_SOCIETY_GATE) it.copy(hidden = true) else it }))
        val ui = TimelineMapState.build(hidden, K.EVENT_JOINS_SOCIETY, null, CompareMode.NOW, hiddenOpen = false)
        assertEquals("숨긴 항목", ui.rows.single { it.eventKey == K.EVENT_JOINS_SOCIETY }.place)
        assertTrue(ui.pins.none { it.name == "결사의 문" })
        assertFalse(ui.pins.any { it.highlighted })
        val open = TimelineMapState.build(hidden, K.EVENT_JOINS_SOCIETY, null, CompareMode.NOW, hiddenOpen = true)
        assertEquals("결사의 문", open.rows.single { it.eventKey == K.EVENT_JOINS_SOCIETY }.place)
    }
}
