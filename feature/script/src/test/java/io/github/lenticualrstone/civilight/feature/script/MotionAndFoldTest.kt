package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.fold.FoldRules
import io.github.lenticualrstone.civilight.feature.script.layout.TabletopLayout
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.feature.script.motion.ScriptMotion
import io.github.lenticualrstone.civilight.feature.script.motion.ScriptScroll
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.model.script.ScriptRestore
import io.github.lenticualrstone.civilight.ui.window.FoldPosture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-67 / CNV-39 / CNV-40 motion values, SCR-66 fold continuity, SCR-69 tabletop split. */
class MotionAndFoldTest {
    private val b43 = K.block(1, 7, 43)
    private val b44 = K.block(1, 7, 44)

    @Test
    fun `CNV-40 swipe buttons - 260 ms slide, each button 60 ms after the one before`() {
        assertEquals(260, ScriptMotion.swipeMs)
        assertEquals(60, ScriptMotion.staggerMs)
        assertEquals(140, ScriptMotion.buttonRunMs)
        (0 until 3).forEach { i ->
            assertEquals(0f, ScriptMotion.buttonProgress(0f, i, reduced = false), 0f)
            assertEquals(1f, ScriptMotion.buttonProgress(1f, i, reduced = false), 0f)
        }
        // 60 ms into the slide: the first button is under way, the second just starts, the third waits.
        val at60 = 60f / 260f
        assertEquals(60f / 140f, ScriptMotion.buttonProgress(at60, 0, reduced = false), 1e-4f)
        assertEquals(0f, ScriptMotion.buttonProgress(at60, 1, reduced = false), 1e-4f)
        assertEquals(0f, ScriptMotion.buttonProgress(at60, 2, reduced = false), 1e-4f)
        assertEquals(10f / 140f, ScriptMotion.buttonProgress(130f / 260f, 2, reduced = false), 1e-4f)
        // Buttons come in from 16 dp to the right.
        assertEquals(16f, ScriptMotion.buttonShiftDp(0f, reduced = false), 0f)
        assertEquals(0f, ScriptMotion.buttonShiftDp(1f, reduced = false), 0f)
    }

    @Test
    fun `CNV-40 reduced motion - the buttons fade with the row, no slide, no stagger`() {
        assertEquals(0.3f, ScriptMotion.buttonProgress(0.3f, 2, reduced = true), 0f)
        assertEquals(0f, ScriptMotion.buttonShiftDp(0f, reduced = true), 0f)
    }

    @Test
    fun `CNV-39 SCR-67 placement - the typed line never moves, a new block's neighbours follow it`() {
        assertNull(ScriptMotion.placement(isCaretRow = true, entering = true))
        assertNull(ScriptMotion.placement(isCaretRow = true, entering = false, structural = true))
        assertEquals(ScriptMotion.Placement.ENTER, ScriptMotion.placement(isCaretRow = false, entering = true))
        // A removed or inserted row (the key set changed) closes its gap, typing or not.
        assertEquals(ScriptMotion.Placement.EXIT, ScriptMotion.placement(isCaretRow = false, entering = false, structural = true))
        // A pure edit moves nothing through placement: a growing line pushes the rows below at once.
        assertNull(ScriptMotion.placement(isCaretRow = false, entering = false, structural = false))
        assertEquals(320, ScriptMotion.token(ScriptMotion.Placement.ENTER).durationMs)
        assertEquals(160, ScriptMotion.token(ScriptMotion.Placement.EXIT).durationMs)
        assertEquals(8, ScriptMotion.ENTER_SHIFT_DP)
    }

    @Test
    fun `SCR-67 a change of the row set is structural, the first composition and a pure edit are not`() {
        assertFalse(ScriptMotion.structural(null, setOf("a", "b")))
        assertFalse(ScriptMotion.structural(setOf("a", "b"), setOf("b", "a")))
        assertTrue(ScriptMotion.structural(setOf("a", "b"), setOf("a", "b", "c")))
        assertTrue(ScriptMotion.structural(setOf("a", "b"), setOf("a")))
        assertTrue(ScriptMotion.structural(setOf("a"), setOf("b")))
    }

    @Test
    fun `SCR-66 the sheet and the side panel are one element across fold and unfold`() {
        assertNull(FoldRules.onLayoutChange(wasSide = true, isSide = true, sheetOpen = false, panelOpen = true, editing = false, selecting = false))
        // Unfold with the panel sheet open: the sheet goes, the side panel shows.
        assertEquals(
            FoldRules.Handover(closeSheet = true, panelOpen = true),
            FoldRules.onLayoutChange(wasSide = false, isSide = true, sheetOpen = true, panelOpen = false, editing = false, selecting = false),
        )
        // Unfold with no sheet: a closed panel stays closed.
        assertEquals(
            FoldRules.Handover(panelOpen = false),
            FoldRules.onLayoutChange(wasSide = false, isSide = true, sheetOpen = false, panelOpen = true, editing = false, selecting = false),
        )
        // Fold with the side panel open: it becomes the sheet.
        assertEquals(
            FoldRules.Handover(openSheet = true, panelOpen = true),
            FoldRules.onLayoutChange(wasSide = true, isSide = false, sheetOpen = false, panelOpen = true, editing = false, selecting = false),
        )
        // Fold while typing or selecting: caret and keyboard stay, no sheet.
        assertFalse(FoldRules.onLayoutChange(true, false, false, panelOpen = true, editing = true, selecting = false)!!.openSheet)
        assertFalse(FoldRules.onLayoutChange(true, false, false, panelOpen = true, editing = false, selecting = true)!!.openSheet)
        // Fold with the panel closed: nothing opens.
        assertFalse(FoldRules.onLayoutChange(true, false, false, panelOpen = false, editing = false, selecting = false)!!.openSheet)
    }

    @Test
    fun `SCR-66 the anchor keeps its height - caret row, else the selected block, else the top row`() {
        val visible = listOf("head:s3" to -40, ScriptRowKeys.block(b43) to 120, ScriptRowKeys.block(b44) to 300)
        val typing = EditorState(selected = b44, editing = true)
        assertEquals(FoldRules.Anchor(ScriptRowKeys.block(b44), 300), FoldRules.anchorOf(visible, typing))
        assertEquals(FoldRules.Anchor(ScriptRowKeys.block(b43), 120), FoldRules.anchorOf(visible, EditorState(selected = b43)))
        assertEquals(FoldRules.Anchor("head:s3", -40), FoldRules.anchorOf(visible, EditorState()))
        // The selected block scrolled away: the top row.
        assertEquals("head:s3", FoldRules.anchorOf(visible, EditorState(selected = K.block(1, 7, 36)))?.rowKey)
        assertNull(FoldRules.anchorOf(emptyList(), EditorState()))
        val keys = listOf("head:s3", ScriptRowKeys.block(b43), ScriptRowKeys.block(b44))
        assertEquals(2 to 300, FoldRules.restoreScroll(keys, FoldRules.Anchor(ScriptRowKeys.block(b44), 300)))
        assertNull(FoldRules.restoreScroll(keys, FoldRules.Anchor("block:gone", 0)))
    }

    @Test
    fun `SCR-66 the restore point - episode, block, caret offset and the top block`() {
        val typing = FoldRules.restorePoint(K.EPISODE_1_7, EditorState(selected = b43, editing = true), caretOffset = 7, topRowKey = ScriptRowKeys.block(b43))
        assertEquals(ScriptRestore(K.EPISODE_1_7, b43, 7, b43), typing)
        val reading = FoldRules.restorePoint(K.EPISODE_1_7, EditorState(selected = b44), caretOffset = 7, topRowKey = "head:s3")
        assertEquals(ScriptRestore(K.EPISODE_1_7, b44, null, null), reading)
        assertTrue(FoldRules.shouldRestore(typing, K.EPISODE_1_7))
        assertFalse(FoldRules.shouldRestore(typing, K.EPISODE_1_8))
        assertFalse(FoldRules.shouldRestore(null, K.EPISODE_1_7))
    }

    @Test
    fun `SCR-31 SCR-66 per-tab scroll round-trips through the view state, zero offsets are not stored`() {
        val stored = FoldRules.storedScroll(mapOf(PanelTab.TIME to 0, PanelTab.REF to 240, PanelTab.MODS to 12))
        assertEquals(setOf(240, 12), stored.values.toSet())
        assertEquals(240, FoldRules.scrollOf(stored, PanelTab.REF))
        assertEquals(12, FoldRules.scrollOf(stored, PanelTab.MODS))
        assertEquals(0, FoldRules.scrollOf(stored, PanelTab.TIME))
        assertEquals(0, FoldRules.scrollOf(emptyMap(), PanelTab.PROPS))
    }

    @Test
    fun `SCR-69 tabletop - manuscript above the fold, panel and command field below, a free band at the fold`() {
        assertNull(TabletopLayout.of(FoldPosture.FLAT, 800f))
        assertNull(TabletopLayout.of(FoldPosture.BOOK, 800f))
        assertNull(TabletopLayout.of(FoldPosture.UNKNOWN, 800f))
        assertEquals(TabletopLayout(388f, 24f, 388f), TabletopLayout.of(FoldPosture.TABLETOP, 800f))
        assertEquals(TabletopLayout(288f, 24f, 488f), TabletopLayout.of(FoldPosture.TABLETOP, 800f, foldAt = 300f))
        // Too low to split (a landscape cover screen).
        assertNull(TabletopLayout.of(FoldPosture.TABLETOP, 400f))
    }

    @Test
    fun `CNV-40 the swipe settle scales with the distance left and never falls under the reduced fade`() {
        assertEquals(260, ScriptMotion.settle(1f).durationMs)
        assertEquals(130, ScriptMotion.settle(0.5f).durationMs)
        assertEquals(120, ScriptMotion.settle(0.1f).durationMs)
        assertEquals(120, ScriptMotion.settle(0f).durationMs)
        assertEquals(260, ScriptMotion.settle(3f).durationMs)
        assertEquals(CivilightMotion.swipe.curve, ScriptMotion.settle(0.5f).curve)
    }

    @Test
    fun `a programmatic scroll glides the row under the bar, half a screen at most`() {
        // The row is 300 px down and the bar covers 120 px: it travels 180.
        assertEquals(180f, ScriptScroll.travel(rowOffset = 300, beforeContentPadding = 120), 0f)
        assertEquals(-40f, ScriptScroll.travel(rowOffset = 80, beforeContentPadding = 120), 0f)
        assertEquals(0f, ScriptScroll.travel(rowOffset = 120, beforeContentPadding = 120), 0f)
        assertEquals(900, ScriptScroll.glidePx(viewportHeight = 1800))
        assertEquals(0, ScriptScroll.glidePx(viewportHeight = -5))
    }
}
