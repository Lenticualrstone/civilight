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
    fun `r3 LScript - landscape 933 x 704 puts a 320 panel right and narrows the column beside it`() {
        val l = ScriptLayout.of(933f, panelOpen = true, height = 704f)
        assertTrue(l.panel)
        assertFalse(l.rail)
        assertEquals(320f, l.panelWidth)
        assertEquals(613f, l.areaWidth)
        assertEquals(40f, l.columnStart)
        assertEquals(557f, l.columnWidth)
    }

    @Test
    fun `r3 PScript - portrait 704 x 933 keeps the 272 panel`() {
        val l = ScriptLayout.of(704f, panelOpen = true, height = 933f)
        assertTrue(l.panel)
        assertEquals(272f, l.panelWidth)
        assertEquals(432f, l.areaWidth)
        assertEquals(376f, l.columnWidth)
        assertEquals(272f, ScriptLayout.panelWidthFor(800f, 800f))
    }

    @Test
    fun `r3 Shut - a panel the writer closed leaves the 56 rail and its hairline`() {
        val l = ScriptLayout.of(933f, panelOpen = false, height = 704f)
        assertFalse(l.panel)
        assertTrue(l.rail)
        assertEquals(933f - 57f, l.areaWidth)
        assertEquals(640f, l.columnWidth)
        assertEquals((876f - 640f) / 2f, l.columnStart)
        val p = ScriptLayout.of(704f, panelOpen = false, height = 933f)
        assertTrue(p.rail)
        assertEquals(647f, p.areaWidth)
    }

    @Test
    fun `r3 a panel hidden for a while (select mode, tabletop) leaves no rail`() {
        val open = ScriptLayout.of(933f, panelOpen = true, height = 704f, hidden = true)
        assertFalse(open.panel)
        assertFalse(open.rail)
        assertEquals(933f, open.areaWidth)
        val closed = ScriptLayout.of(933f, panelOpen = false, height = 704f, hidden = true)
        assertFalse(closed.rail)
        assertEquals(933f, closed.areaWidth)
    }

    @Test
    fun `SCR-03 a closed panel gives the area the full width and centers the 640 column`() {
        val l = ScriptLayout.of(800f, panelOpen = false, hidden = true)
        assertFalse(l.panel)
        assertEquals(800f, l.areaWidth)
        assertEquals(640f, l.columnWidth)
        assertEquals((800f - 640f) / 2f, l.columnStart)
        assertEquals(640f, ScriptLayout.of(1200f, panelOpen = true).columnWidth)
    }

    @Test
    fun `a narrow two-pane detail is laid out as cover, and a tight inner width drops the panel`() {
        assertFalse(ScriptLayout.of(410f, panelOpen = true).wide)
        // Z Fold 8 at 480 dpi inner portrait (616 x 816 dp) keeps the side panel (288 dp column >= 280 dp).
        assertTrue(ScriptLayout.of(616f, panelOpen = true, height = 816f).panel)
        val tight = ScriptLayout.of(600f, panelOpen = true)
        assertTrue(tight.wide)
        assertFalse(tight.panel)
        // No room for the panel: no rail either (nothing to reopen).
        assertFalse(tight.rail)
        assertFalse(ScriptLayout.of(600f, panelOpen = false).rail)
        assertEquals(560f, tight.columnWidth)
    }

    @Test
    fun `CNV-47 the chosen manuscript width sets the panel width and the column`() {
        val l = ScriptLayout.of(1000f, panelOpen = true, area = 600f)
        assertTrue(l.panel)
        assertEquals(600f, l.areaWidth)
        assertEquals(400f, l.panelWidth)
        assertEquals(544f, l.columnWidth)
        // The default keeps the r3 panel (272 portrait / square, 320 landscape).
        assertEquals(272f, ScriptLayout.of(1000f, panelOpen = true).panelWidth)
        assertEquals(320f, ScriptLayout.of(1000f, panelOpen = true, height = 700f).panelWidth)
        // The choice counts only while the panel is drawn.
        val closed = ScriptLayout.of(1000f, panelOpen = false, area = 600f)
        assertEquals(1000f - ScriptLayout.RAIL_SPACE, closed.areaWidth)
        assertEquals(640f, closed.columnWidth)
        assertEquals((closed.areaWidth - 640f) / 2f, closed.columnStart)
    }

    @Test
    fun `SCR-66 a panel sliding in leaves the manuscript at its old width until it has arrived`() {
        val open = ScriptLayout.of(1000f, panelOpen = true)
        val closed = ScriptLayout.of(1000f, panelOpen = false, hidden = true)
        val holding = open.keepingAreaOf(closed)
        assertTrue(holding.panel)
        assertEquals(1000f, holding.areaWidth)
        assertEquals(640f, holding.columnWidth)
        assertEquals(open.panelWidth, holding.panelWidth)
    }

    @Test
    fun `r3 SCR-66 the manuscript holds its last width until the panel arrives - rail, full width, or after a resize`() {
        val rail = ScriptLayout.of(933f, panelOpen = false, height = 704f)
        assertEquals(rail, ScriptLayout.beforePanel(rail, 933f, 933f, 704f))
        val selecting = ScriptLayout.of(933f, panelOpen = true, height = 704f, hidden = true)
        assertEquals(933f, ScriptLayout.beforePanel(selecting, 933f, 933f, 704f).areaWidth)
        // Unfold: the last no-panel layout was the cover's; the manuscript starts from the full inner width.
        val cover = ScriptLayout.of(475f, panelOpen = true, height = 751f)
        assertEquals(933f, ScriptLayout.beforePanel(cover, 475f, 933f, 704f).areaWidth)
        assertEquals(704f, ScriptLayout.beforePanel(null, null, 704f, 933f).areaWidth)
    }

    @Test
    fun `r3 the panel slides out of the frame only to or from the rail`() {
        assertTrue(ScriptLayout.slidesFromRail(ScriptLayout.of(933f, panelOpen = false, height = 704f)))
        assertFalse(ScriptLayout.slidesFromRail(ScriptLayout.of(933f, panelOpen = true, height = 704f, hidden = true)))
        assertFalse(ScriptLayout.slidesFromRail(ScriptLayout.of(475f, panelOpen = false, height = 751f)))
    }
}
