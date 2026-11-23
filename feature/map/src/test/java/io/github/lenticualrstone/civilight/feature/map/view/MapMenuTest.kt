package io.github.lenticualrstone.civilight.feature.map.view

import io.github.lenticualrstone.civilight.domain.world.gen.StampCheck
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.common.StampCheckRow
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.open
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The map menu rows (WLD-36 rename, WLD-56 … 62 entries) and the WLD-57 style chip. */
class MapMenuTest {
    private val doc = MapFx.doc()

    @Test
    fun `WLD-75 format editors, re-roll and 인물 동선 are hidden from every map menu`() {
        val world = doc.maps.getValue(K.MAP_WORLD)
        assertEquals(listOf("이름", "지우기"), MapMenu.rows(world, MapMenuScreen.VIEW).map { it.title })
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        assertEquals(listOf("이름"), MapMenu.rows(lower, MapMenuScreen.EDIT).map { it.title })
        assertEquals("하층 구역 지도", MapMenu.rows(lower, MapMenuScreen.EDIT).first().subtitle)
        val node = doc.maps.getValue(K.MAP_ASH_MARKET)
        assertEquals(listOf(MapMenuAction.RENAME, MapMenuAction.DELETE), MapMenu.rows(node, MapMenuScreen.NODE).map { it.action })
    }

    @Test
    fun `WLD-75 no re-roll row, whatever the generator version`() {
        val world = doc.maps.getValue(K.MAP_WORLD)
        val old = world.copy(generation = world.generation!!.copy(generatorVersion = "gen-0"))
        assertTrue(MapMenu.rows(old, MapMenuScreen.VIEW).none { it.action == MapMenuAction.REROLL })
        assertFalse(MapMenu.canReroll(old))
        assertEquals("단계 다시 만들기", MapCopy.REROLL_TITLE)
    }

    @Test
    fun `WLD-57 the style chip is for generated detailed-terrain and polygon maps`() {
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        assertEquals("먹선", MapViewState.styleChip(lower))
        assertEquals("양피지", MapViewState.styleChip(lower.copy(style = MapStyle.PARCHMENT.open())))
        assertEquals("색면", MapViewState.styleChip(lower.copy(format = MapFormat.POLY.open(), style = null)))
        assertNull(MapViewState.styleChip(doc.maps.getValue(K.MAP_WORLD)))
        assertNull(MapViewState.styleChip(doc.maps.getValue(K.MAP_ASH_MARKET)))
    }

    @Test
    fun `WLD-56 위치 확인 rows keep the stamp, the old spot and the nearest new one`() {
        val row = StampCheckRow.of(StampCheck("s", "다리", Point(0.1f, 0.2f), Point(0.3f, 0.4f)))
        assertEquals(StampCheckRow("s", "다리", 0.1f, 0.2f, 0.3f, 0.4f), row)
        assertNull(StampCheckRow.of(StampCheck("s", "다리", Point(0.1f, 0.2f), null)).nearX)
    }
}
