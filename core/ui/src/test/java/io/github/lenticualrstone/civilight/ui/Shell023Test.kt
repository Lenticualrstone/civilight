package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.model.app.UnfoldedLayout
import io.github.lenticualrstone.civilight.ui.back.BackCopy
import io.github.lenticualrstone.civilight.ui.back.BackLayers
import io.github.lenticualrstone.civilight.ui.back.BackPolicy
import io.github.lenticualrstone.civilight.ui.back.BackSnapshot
import io.github.lenticualrstone.civilight.ui.back.BackStep
import io.github.lenticualrstone.civilight.ui.scaffold.CommandBarRules
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope
import io.github.lenticualrstone.civilight.ui.window.PaneSplitRules
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.window.PaneSplitSource
import io.github.lenticualrstone.civilight.ui.window.PanelCopy
import io.github.lenticualrstone.civilight.ui.window.PanelRules
import io.github.lenticualrstone.civilight.ui.window.PanelSide
import io.github.lenticualrstone.civilight.ui.window.WindowFrame
import io.github.lenticualrstone.civilight.ui.window.WindowLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.3 shell: back policy, r3 window frames, the collapsible side panel and its migration, copy. */
class Shell023Test {

    private fun snap(
        ime: Boolean = false,
        layers: Int = 0,
        stack: Int = 1,
        locked: Boolean = false,
        armedAt: Long? = null,
        now: Long = 10_000L,
        hint: Boolean = false,
    ) = BackSnapshot(ime, layers, stack, locked, armedAt, now, hint)

    // ---- back

    @Test
    fun `back closes what is open first - keyboard, then the layer, then the screen`() {
        assertEquals(BackStep.HIDE_KEYBOARD, BackPolicy.decide(snap(ime = true, layers = 2, stack = 3)))
        assertEquals(BackStep.CLOSE_LAYER, BackPolicy.decide(snap(layers = 1, stack = 3)))
        assertEquals(BackStep.CLOSE_LAYER, BackPolicy.decide(snap(layers = 1, stack = 1)))
        assertEquals(BackStep.POP, BackPolicy.decide(snap(stack = 2)))
        assertEquals(BackStep.NONE, BackPolicy.decide(snap(locked = true, ime = true, layers = 1)))
    }

    @Test
    fun `bottom screen needs a second press within 2 s`() {
        assertEquals(BackStep.ARM_EXIT, BackPolicy.decide(snap()))
        assertEquals(BackStep.EXIT, BackPolicy.decide(snap(armedAt = 10_000L, now = 11_999L)))
        assertEquals(BackStep.EXIT, BackPolicy.decide(snap(armedAt = 10_000L, now = 12_000L)))
        assertEquals(BackStep.ARM_EXIT, BackPolicy.decide(snap(armedAt = 10_000L, now = 12_001L)))
        // A clock that went backwards never counts as armed.
        assertEquals(BackStep.ARM_EXIT, BackPolicy.decide(snap(armedAt = 10_000L, now = 9_000L)))
        // While the hint is still shown (accessibility timeout) its text stays true: the next press closes.
        assertEquals(BackStep.EXIT, BackPolicy.decide(snap(armedAt = 10_000L, now = 15_000L, hint = true)))
        // An open sheet on the bottom screen closes first, even when armed.
        assertEquals(BackStep.CLOSE_LAYER, BackPolicy.decide(snap(layers = 1, armedAt = 10_000L, now = 10_500L)))
    }

    @Test
    fun `root handler stays out of screens, layers and the lock`() {
        assertTrue(BackPolicy.rootIntercepts(snap()))
        assertTrue(BackPolicy.rootIntercepts(snap(ime = true, stack = 4)))
        assertFalse(BackPolicy.rootIntercepts(snap(stack = 2)))
        assertFalse(BackPolicy.rootIntercepts(snap(layers = 1)))
        assertFalse(BackPolicy.rootIntercepts(snap(locked = true)))
    }

    @Test
    fun `layers count up and never below zero`() {
        val l = BackLayers()
        l.add(); l.add(); l.remove()
        assertEquals(1, l.open)
        l.remove(); l.remove()
        assertEquals(0, l.open)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `exit hint shows for 2 s`() {
        val scope = TestScope()
        val toasts = ToastController(scope)
        val id = toasts.showBrief(BackCopy.EXIT_HINT, BackPolicy.EXIT_WINDOW_MS)
        assertTrue(toasts.isShowing(id))
        assertEquals(BackCopy.EXIT_HINT, toasts.current.value?.text)
        assertNull(toasts.current.value?.actionLabel)
        scope.advanceTimeBy(1_999L); scope.runCurrent()
        assertTrue(toasts.isShowing(id))
        scope.advanceTimeBy(2L); scope.runCurrent()
        assertFalse(toasts.isShowing(id))
    }

    // ---- window frames (Fold 8)

    @Test
    fun `fold 8 frames - cover, inner upright, inner on its side`() {
        assertEquals(WindowFrame.COVER, WindowLayout.Cover.frame)
        assertEquals(475 to 751, WindowLayout.Cover.widthDp to WindowLayout.Cover.heightDp)
        assertEquals(WindowFrame.INNER_PORTRAIT, WindowLayout.Inner.frame)
        assertEquals(704 to 933, WindowLayout.Inner.widthDp to WindowLayout.Inner.heightDp)
        assertEquals(WindowFrame.INNER_LANDSCAPE, WindowLayout.InnerLandscape.frame)
        assertEquals(933 to 704, WindowLayout.InnerLandscape.widthDp to WindowLayout.InnerLandscape.heightDp)
        assertFalse(WindowLayout.Cover.isInner)
        assertTrue(WindowLayout.Inner.isInner)
        assertTrue(WindowLayout.InnerLandscape.isLandscape)
        // Frames follow the window: the cover turned on its side (751 wide) is read as an inner landscape frame.
        assertEquals(WindowFrame.COVER, WindowLayout.classifyFrame(475, 751))
        assertEquals(WindowFrame.INNER_LANDSCAPE, WindowLayout.classifyFrame(751, 475))
    }

    // ---- side panel (r3 L* / Two*)

    @Test
    fun `open panel - main starts after the panel and its line`() {
        assertEquals(301f, PanelRules.mainInset(300f, 1f), 0.01f)
        assertEquals(321f, PanelRules.mainInset(320f, 1f), 0.01f)
        assertEquals(0f, PanelRules.panelShift(300f, 1f), 0.01f)
        assertEquals(291f, PanelRules.tabInset(300f, 1f), 0.01f)
        // An end-side panel (the script's): board `right: calc(var(--pw) - 11px)`.
        assertEquals(309f, PanelRules.tabInset(320f, 1f, PanelSide.END), 0.01f)
        assertEquals(16f, PanelRules.tabInset(320f, 0f, PanelSide.END), 0.01f)
        assertEquals(20f, PanelRules.tabWidth(1f), 0.01f)
    }

    @Test
    fun `closed panel - 56 dp rail with its hairline, tab on the rail`() {
        assertEquals(57f, PanelRules.mainInset(300f, 0f), 0.01f)
        assertEquals(300f, PanelRules.panelShift(300f, 0f), 0.01f)
        assertEquals(16f, PanelRules.tabInset(300f, 0f), 0.01f)
        assertEquals(24f, PanelRules.tabWidth(0f), 0.01f)
        // Board numbers: landscape main 933 - 57 = 876 closed, 933 - 321 = 612 open.
        assertEquals(876f, 933f - PanelRules.mainInset(320f, 0f), 0.01f)
        assertEquals(612f, 933f - PanelRules.mainInset(320f, 1f), 0.01f)
    }

    @Test
    fun `seam button is at least 44 dp and says what a tap does`() {
        assertTrue(PanelRules.TAB_HIT_WIDTH >= 44f && PanelRules.TAB_HIT_HEIGHT >= 44f)
        assertTrue(PanelRules.REOPEN_SIZE >= 44f)
        assertEquals("패널 닫기", PanelRules.label(closed = false))
        assertEquals("패널 열기", PanelRules.label(closed = true))
        // The chevron points to the panel's edge while open and away from it while closed.
        assertEquals(0f, PanelRules.chevronDegrees(PanelSide.START, 1f), 0.01f)
        assertEquals(180f, PanelRules.chevronDegrees(PanelSide.START, 0f), 0.01f)
        assertEquals(180f, PanelRules.chevronDegrees(PanelSide.END, 1f), 0.01f)
        assertFalse(PanelRules.canCollapse(inner = false))
    }

    @Test
    fun `panel state is remembered per screen and migrates the retired center column`() {
        val fresh = AppSettings()
        assertFalse(fresh.panelClosedOf(PaneSplit.Modules.CAST))
        // `가운데 한 칸` users start with every panel closed (one wide pane); the old value stays stored.
        val column = AppSettings(unfoldedLayout = UnfoldedLayout.CENTER_COLUMN)
        assertTrue(column.panelClosedOf(PaneSplit.Modules.LIBRARY))
        val reopened = column.withPanelClosed(PaneSplit.Modules.LIBRARY, false)
        assertFalse(reopened.panelClosedOf(PaneSplit.Modules.LIBRARY))
        assertTrue(reopened.panelClosedOf(PaneSplit.Modules.CAST))
        assertEquals(UnfoldedLayout.CENTER_COLUMN, reopened.unfoldedLayout)
        // Per screen: closing the cast list leaves the world list open.
        val cast = fresh.withPanelClosed(PaneSplit.Modules.CAST, true)
        assertTrue(cast.panelClosedOf(PaneSplit.Modules.CAST))
        assertFalse(cast.panelClosedOf(PaneSplit.Modules.WORLD))
    }

    @Test
    fun `pane split source writes the panel state`() {
        val source = PaneSplitSource.inMemory()
        assertNull(source.storedPanelClosed(PaneSplit.Modules.HOME))
        source.setPanelClosed(PaneSplit.Modules.HOME, true)
        assertTrue(source.panelClosed(PaneSplit.Modules.HOME))
        assertEquals(true, source.storedPanelClosed(PaneSplit.Modules.HOME))
    }

    @Test
    fun `closed list commands nothing in the frame bar`() {
        assertFalse(CommandBarRules.paneShown(CommandScope.LIST, listClosed = true))
        assertTrue(CommandBarRules.paneShown(CommandScope.DETAIL, listClosed = true))
        assertTrue(CommandBarRules.paneShown(CommandScope.LIST, listClosed = false))
    }

    @Test
    fun `a drag ending near the module's own width settles to default`() {
        // 704 portrait: default list 300; 1:2 = 234.3, 1:1 = 351.5.
        assertEquals(PaneSplit.DEFAULT, PaneSplitRules.settle(296f, 704f, 300f))
        assertEquals(PaneSplit.ONE_TWO, PaneSplitRules.settle(240f, 704f, 300f))
        assertEquals(PaneSplit.ONE_ONE, PaneSplitRules.settle(345f, 704f, 300f))
        assertEquals(10f, PanelRules.tabRadius(1f), 0.01f)
        assertEquals(12f, PanelRules.tabRadius(0f), 0.01f)
    }

    @Test
    fun `0_2_3 shell copy passes CopyLint`() {
        for (v in BackCopy.all + PanelCopy.all) assertTrue("$v: ${CopyLint.lint(v)}", CopyLint.lint(v).isEmpty())
        assertEquals("한 번 더 누르면 닫힘", BackCopy.EXIT_HINT)
    }
}
