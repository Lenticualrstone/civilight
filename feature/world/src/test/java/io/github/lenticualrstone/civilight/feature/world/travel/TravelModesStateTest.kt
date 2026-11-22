package io.github.lenticualrstone.civilight.feature.world.travel

import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-47 이동 수단 editor (the TravelModes route). */
class TravelModesStateTest {
    private val ctx = WorldTestFixture.ctx()

    private fun bare() = WorldTestFixture.stateB().let { it.copy(project = it.project.copy(travelModes = emptyList())) }

    @Test
    fun `WLD-47 the defaults are listed as 도보 15 km · 8시간`() {
        val ui = TravelModesState.build(bare(), readOnly = false)
        assertEquals(listOf("도보", "말", "배"), ui.rows.map { it.name })
        assertEquals("15 km · 8시간", ui.rows[0].line)
        assertEquals(TravelModeDraft("배", "30", "24"), TravelModesState.draftOf(bare(), 2))
    }

    @Test
    fun `editing a default mode stores the modes once and changes that row`() {
        val saved = TravelModesState.save(bare(), 1, TravelModeDraft("말", "45", "8"), ctx) as TravelModeSave.Saved
        assertEquals(3, saved.doc.project.travelModes.size)
        assertEquals(45.0, Travel.modes(saved.doc)[1].kmPerDay, 0.0)
    }

    @Test
    fun `직접 adds a mode after the others and refusals carry their line`() {
        val added = TravelModesState.save(bare(), null, TravelModeDraft("마차", "30", "10"), ctx) as TravelModeSave.Saved
        assertEquals("마차", Travel.modes(added.doc).last().name)
        assertEquals(TravelModeSave.Refused(WorldCopy.TRAVEL_DISTANCE_BAD), TravelModesState.save(bare(), null, TravelModeDraft("x", "abc", "8"), ctx))
        assertEquals(TravelModeSave.Refused(WorldCopy.TRAVEL_HOURS_BAD), TravelModesState.save(bare(), null, TravelModeDraft("x", "10", "30"), ctx))
        assertEquals(TravelModeSave.Refused(WorldCopy.TRAVEL_NAME_TAKEN), TravelModesState.save(added.doc, null, TravelModeDraft("마차", "10", "8"), ctx))
        assertTrue(TravelModesState.save(bare(), null, TravelModeDraft(" ", "10", "8"), ctx) is TravelModeSave.Refused)
    }
}
