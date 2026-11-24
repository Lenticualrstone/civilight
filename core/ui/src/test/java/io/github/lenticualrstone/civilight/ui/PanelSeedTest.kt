package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.ui.window.PaneSplitSource
import io.github.lenticualrstone.civilight.ui.window.PanelCollapseState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.3 review: a stored `closed` that cannot apply (no detail beside the list) draws open from the first frame. */
class PanelSeedTest {
    @Test
    fun `stored closed with nothing beside the list draws open before follow runs`() {
        val state = PanelCollapseState(PaneSplit.Modules.CAST, PaneSplitSource.inMemory(), initialClosed = true)
        state.available = false
        assertFalse(state.closed)
        assertEquals(1f, state.open, 0f)
        assertEquals(1f, state.panelIn, 0f)
        assertEquals(1f, state.panelAlpha, 0f)
        assertFalse(state.gone)
    }

    @Test
    fun `stored closed that applies draws closed before follow runs`() {
        val state = PanelCollapseState(PaneSplit.Modules.CAST, PaneSplitSource.inMemory(), initialClosed = true)
        assertTrue(state.closed)
        assertEquals(0f, state.open, 0f)
        assertTrue(state.gone)
    }
}
