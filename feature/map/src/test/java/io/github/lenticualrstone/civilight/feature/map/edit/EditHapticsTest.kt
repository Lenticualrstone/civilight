package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.common.MapAsideRules
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.ui.window.PanelCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Boards `MapRegionDraw`, `MapRouteDraw`, `MapStampPlace`: when the editor ticks; `MapEdit`: the floating panel. */
class EditHapticsTest {
    private val doc = MapFx.doc()
    private val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)

    @Test
    fun `a placed vertex or route point ticks, undo and tool changes do not`() {
        var s = Editor.chooseTool(EditorState(), EditTool.REGION)
        val first = Editor.tap(s, lower, Point(0.6f, 0.6f), null).state
        assertTrue(EditHaptics.placedPoint(s, first))
        s = first
        val second = Editor.tap(s, lower, Point(0.7f, 0.6f), null).state
        assertTrue(EditHaptics.placedPoint(s, second))
        assertFalse(EditHaptics.placedPoint(second, s))
        assertFalse(EditHaptics.placedPoint(EditorState(), Editor.chooseTool(EditorState(), EditTool.LABEL)))
        val route = Editor.chooseTool(EditorState(), EditTool.ROUTE)
        assertTrue(EditHaptics.placedPoint(route, Editor.tap(route, lower, Point(0.20f, 0.40f), null).state))
    }

    @Test
    fun `a stamp ticks on each 15 degree detent and each size step only`() {
        val stamp = lower.stamps.first()
        val c = Point(stamp.x, stamp.y)
        val same = c.copy(y = c.y - 0.1f)
        val atSame = StampHandles.rotationToward(c, same)
        assertEquals(atSame != stamp.rotation, EditHaptics.rotationDetent(stamp, same))
        assertFalse(EditHaptics.rotationDetent(stamp.copy(rotation = atSame), same))
        assertTrue(EditHaptics.sizePicked(1, 2))
        assertFalse(EditHaptics.sizePicked(2, 2))
        val far = c.copy(x = c.x + 0.2f)
        assertFalse(EditHaptics.sizeStep(stamp.copy(size = StampHandles.sizeToward(c, far)), far))
    }

    @Test
    fun `the floating panel follows the board, closes by its button and is stored per app`() {
        assertEquals(252f, MapAsideRules.WIDTH)
        assertEquals(20f, MapAsideRules.RADIUS)
        assertEquals(0f, MapAsideRules.shift(1f))
        assertEquals(252f + 16f, MapAsideRules.shift(0f))
        assertTrue(MapAsideRules.REOPEN >= 44f)
        assertEquals("패널 닫기", PanelCopy.CLOSE)
        assertEquals("패널 열기", PanelCopy.OPEN)
        assertTrue(PaneSplit.Modules.MAP_PANEL in PaneSplit.Modules.ALL)
        // Closed on the inner screen: the cover's cards come back above the tool bar.
        val panelShown = io.github.lenticualrstone.civilight.feature.map.common.MapBars::panelShown
        assertTrue(panelShown(true, false))
        assertFalse(panelShown(true, true))
        assertFalse(panelShown(false, false))
        val bars = io.github.lenticualrstone.civilight.feature.map.common.MapBars
        assertEquals(bars.PROPS, bars.edit(false, false, false, true, false, panelShown(true, true)))
        assertEquals(null, bars.edit(false, false, false, true, false, panelShown(true, false)))
        assertEquals(bars.LAYERS, bars.node(false, true, false, panelShown(true, true)))
    }
}
