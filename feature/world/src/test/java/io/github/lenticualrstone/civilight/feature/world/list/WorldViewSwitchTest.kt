package io.github.lenticualrstone.civilight.feature.world.list

import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.navigation.WorldTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-05 / WLD-68: which change of the list body is which motion. */
class WorldViewSwitchTest {
    private val terms = WorldView(WorldTab.TERMS, cards = false)
    private val termCards = WorldView(WorldTab.TERMS, cards = true)
    private val places = WorldView(WorldTab.PLACES, cards = false)

    @Test
    fun `a flip on one tab is a view change and a tab change is a swap`() {
        assertEquals(WorldViewSwitch.Kind.VIEW, WorldViewSwitch.kind(terms, termCards))
        assertEquals(WorldViewSwitch.Kind.VIEW, WorldViewSwitch.kind(termCards, terms))
        assertEquals(WorldViewSwitch.Kind.TAB, WorldViewSwitch.kind(terms, places))
        assertEquals(WorldViewSwitch.Kind.TAB, WorldViewSwitch.kind(termCards, places))
        assertEquals(WorldViewSwitch.Kind.NONE, WorldViewSwitch.kind(terms, terms))
    }

    @Test
    fun `cards grow only list to cards on the same tab in full motion`() {
        assertTrue(WorldViewSwitch.growsIn(terms, termCards, reduced = false))
        assertFalse(WorldViewSwitch.growsIn(termCards, terms, reduced = false))
        assertFalse(WorldViewSwitch.growsIn(places, termCards, reduced = false))
        assertFalse(WorldViewSwitch.growsIn(terms, termCards, reduced = true))
        assertFalse(WorldViewSwitch.growsIn(terms, terms, reduced = false))
    }

    @Test
    fun `the grow takes the shared plan timings`() {
        val plan = CardViewMotion.plan(reduced = false)
        assertEquals(CivilightMotion.enterLong.durationMs, plan.growMs)
        assertEquals(CivilightMotion.exit.durationMs, plan.textInDelayMs)
        assertEquals(null, CardViewMotion.plan(reduced = true).growMs)
    }

    @Test
    fun `lazy keys stay unique and keep the first use`() {
        assertEquals(listOf("a", "b", "a#2", "a#3", "c"), WorldViewSwitch.uniqueKeys(listOf("a", "b", "a", "a", "c")))
        assertEquals(emptyList<String>(), WorldViewSwitch.uniqueKeys(emptyList()))
    }
}
