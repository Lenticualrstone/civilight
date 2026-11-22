package io.github.lenticualrstone.civilight.feature.world.faction

import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.navigation.MapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The faction card (WLD-10). */
class FactionCardStateTest {
    @Test
    fun `WLD-10 head, project fields and description links`() {
        val ui = FactionCardState.build(WorldTestFixture.stateB(), PROJECT, K.FACTION_LANTERN_SOCIETY, hiddenOpen = false)
        assertEquals(CardAccess.Open, ui.access)
        assertEquals("세력", ui.head!!.eyebrow)
        assertEquals("등불 결사", ui.head!!.name)
        val leader = ui.fields.filled.single { it.fieldDefKey == K.FIELD_FACTION_LEADER }
        assertEquals("수장", leader.label)
        assertEquals("마르타", leader.value)
        // 소속 is free text in the fixture (fixture.md conflict 10): no character links here by `연결`.
        assertTrue(ui.sections.none { it.id == "linked" })
        // The description links 마르타 (fixture): drawn with the link shape.
        assertTrue(ui.head!!.description.any { p -> p.decorations.isNotEmpty() })
    }

    @Test
    fun `WLD-10 map areas linked to the faction open their map`() {
        val ui = FactionCardState.build(WorldTestFixture.stateB(), PROJECT, K.FACTION_UPPER_GUARD, hiddenOpen = false)
        val areas = ui.sections.single { it.id == "areas" }
        assertEquals(WorldCopy.FACTION_AREAS, areas.title)
        assertTrue(areas.rows.isNotEmpty())
        assertTrue(areas.rows.all { it.target is MapView && it.title.contains(" · ") })
    }

    @Test
    fun `WLD-13 a hidden faction is locked`() {
        val base = WorldTestFixture.stateB()
        val doc = base.copy(world = base.world.copy(factions = base.world.factions.map {
            if (it.key == K.FACTION_UPPER_GUARD) it.copy(hidden = true) else it
        }))
        val ui = FactionCardState.build(doc, PROJECT, K.FACTION_UPPER_GUARD, hiddenOpen = false)
        assertEquals(CardAccess.Locked("숨긴 세력 1"), ui.access)
        assertEquals("숨긴 항목", ui.title)
    }
}
