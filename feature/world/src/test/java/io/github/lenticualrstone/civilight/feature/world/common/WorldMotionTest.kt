package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.list.WorldCardRows
import io.github.lenticualrstone.civilight.feature.world.list.WorldListState
import io.github.lenticualrstone.civilight.feature.world.list.WorldViewSwitch
import io.github.lenticualrstone.civilight.feature.world.time.ChangeStep
import io.github.lenticualrstone.civilight.navigation.WorldTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure decisions behind the world motion rows (motion audit §4 feature:world). */
class WorldMotionTest {
    @Test
    fun `a card screen body stage follows the access`() {
        assertEquals(BodyStage.LOADING, stageOf(null))
        assertEquals(BodyStage.LOCKED, stageOf(CardAccess.Locked("숨긴 용어 1")))
        assertEquals(BodyStage.GONE, stageOf(CardAccess.Gone("지운 용어 · 등불", dotted = true)))
        assertEquals(BodyStage.OPEN, stageOf(CardAccess.Open))
    }

    @Test
    fun `the event change steps push forward and pop back`() {
        assertTrue(ChangeStep.isAhead(ChangeStep.ITEM, ChangeStep.FIELD))
        assertTrue(ChangeStep.isAhead(ChangeStep.FIELD, ChangeStep.VALUE))
        assertTrue(ChangeStep.isAhead(ChangeStep.VALUE, ChangeStep.TARGET))
        assertFalse(ChangeStep.isAhead(ChangeStep.TARGET, ChangeStep.VALUE))
        assertFalse(ChangeStep.isAhead(ChangeStep.VALUE, ChangeStep.FIELD))
        assertFalse(ChangeStep.isAhead(ChangeStep.FIELD, ChangeStep.ITEM))
    }

    @Test
    fun `lazy keys of every tab are unique on the example project`() {
        val doc = WorldTestFixture.stateA()
        for (tab in WorldTab.entries) {
            val ui = WorldListState.build(doc, tab, WorldTestFixture.index(doc, hiddenOpen = false), hiddenOpen = false)
            val keys = WorldViewSwitch.uniqueKeys(ui.items.map { it.id })
            assertEquals("list keys of $tab", keys.size, keys.toSet().size)
            val cards = WorldCardRows.build(doc, ui.items).map { it.id }
            assertEquals("card row keys of $tab", cards.size, cards.toSet().size)
            // A card row is keyed by its section, not by its place in the list.
            cards.forEach { assertTrue(it, it.startsWith("cards:")) }
        }
    }

    @Test
    fun `card rows keep their key when a section above them goes`() {
        val doc = WorldTestFixture.stateA()
        val ui = WorldListState.build(doc, WorldTab.TERMS, WorldTestFixture.index(doc, hiddenOpen = false), hiddenOpen = false)
        val all = WorldCardRows.build(doc, ui.items)
        assertTrue(all.size >= 2)
        val pendingId = all.last().id
        val firstSection = ui.items.indexOfFirst { it is io.github.lenticualrstone.civilight.feature.world.list.WorldListItem.Section }
        val secondSection = ui.items.withIndex().filter { it.value is io.github.lenticualrstone.civilight.feature.world.list.WorldListItem.Section }[1].index
        val withoutFirst = WorldCardRows.build(doc, ui.items.subList(secondSection, ui.items.size))
        assertEquals(pendingId, withoutFirst.last().id)
        assertTrue(firstSection >= 0)
    }
}
