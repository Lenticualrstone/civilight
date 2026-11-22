package io.github.lenticualrstone.civilight.feature.map.travel

import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.feature.map.MapFx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-47 이동 수단 (rule 18 §3; boards `ProjSettings`, `MapRouteC`, `MapScale`). */
class TravelModeRowsTest {
    private val doc = MapFx.doc()

    @Test
    fun `rows read 도보 15 km · 8시간 and mark the project default`() {
        val rows = TravelModeRows.rows(doc, null)
        assertEquals(listOf("도보 15 km · 8시간", "말 40 km · 8시간", "배 30 km · 24시간"), rows.map { it.label }.take(3))
        assertEquals(listOf(true, false, false), rows.map { it.selected }.take(3))
        val horse = Travel.modes(doc)[1]
        assertTrue(TravelModeRows.rows(doc, horse.key)[1].selected)
    }

    @Test
    fun `a project with no stored modes shows the defaults`() {
        val bare = doc.copy(project = doc.project.copy(travelModes = emptyList()))
        assertEquals("도보 15 km · 8시간", TravelModeRows.rows(bare, null).first().label)
    }

    @Test
    fun `the form refuses empty names, duplicates and impossible hours`() {
        assertTrue(TravelModeForm("수레", "25", "10").canSave(doc, null))
        assertTrue(TravelModeForm("수레", "12,5", "10").canSave(doc, null))
        assertFalse(TravelModeForm(" ", "25", "10").canSave(doc, null))
        assertFalse(TravelModeForm("도보", "25", "10").canSave(doc, null))
        assertFalse(TravelModeForm("수레", "0", "10").canSave(doc, null))
        assertFalse(TravelModeForm("수레", "25", "25").canSave(doc, null))
        assertFalse(TravelModeForm("수레", "스물", "10").canSave(doc, null))
        val walk = Travel.modes(doc).first()
        assertTrue("a mode keeps its own name", TravelModeForm("도보", "16", "8").canSave(doc, walk.key))
    }

    @Test
    fun `editing a mode starts from its numbers`() {
        val walk = Travel.modes(doc).first()
        assertEquals(TravelModeForm("도보", "15", "8"), TravelModeForm.of(doc, walk.key))
        assertEquals(TravelModeForm(), TravelModeForm.of(doc, null))
    }
}
