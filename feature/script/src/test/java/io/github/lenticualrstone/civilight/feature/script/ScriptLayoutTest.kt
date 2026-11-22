package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.layout.ScriptLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SCR-03 inner layout and the cover layout, from the measured width. */
class ScriptLayoutTest {
    @Test
    fun `SCR-01 cover keeps the full width and no panel`() {
        val l = ScriptLayout.of(390f, panelOpen = true)
        assertFalse(l.wide)
        assertFalse(l.panel)
        assertEquals(390f, l.columnWidth)
        assertEquals(0f, l.columnStart)
    }

    @Test
    fun `SCR-03 the 800 board puts the panel right and narrows the column beside it`() {
        val l = ScriptLayout.of(800f, panelOpen = true)
        assertTrue(l.panel)
        assertEquals(510f, l.areaWidth)
        assertEquals(40f, l.columnStart)
        assertEquals(454f, l.columnWidth)
    }

    @Test
    fun `SCR-03 a closed panel gives the area the full width and keeps the 560 column`() {
        val l = ScriptLayout.of(800f, panelOpen = false)
        assertFalse(l.panel)
        assertEquals(800f, l.areaWidth)
        assertEquals(560f, l.columnWidth)
        assertEquals(560f, ScriptLayout.of(1200f, panelOpen = true).columnWidth)
    }

    @Test
    fun `a narrow two-pane detail is laid out as cover, and a tight inner width drops the panel`() {
        assertFalse(ScriptLayout.of(410f, panelOpen = true).wide)
        val tight = ScriptLayout.of(620f, panelOpen = true)
        assertTrue(tight.wide)
        assertFalse(tight.panel)
        assertEquals(560f, tight.columnWidth)
    }
}
