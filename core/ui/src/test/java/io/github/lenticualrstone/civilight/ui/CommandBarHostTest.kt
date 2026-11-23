package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.nav.ListDetailPairs
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.scaffold.CommandBarCopy
import io.github.lenticualrstone.civilight.ui.scaffold.CommandBarHost
import io.github.lenticualrstone.civilight.ui.scaffold.CommandBarRules
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope.DETAIL
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope.LIST
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScopeEvent
import io.github.lenticualrstone.civilight.ui.scaffold.PaneCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DS-65: one command bar per frame (T-DS-36, T-DS-37). */
class CommandBarHostTest {
    private val library = PaneCommand(declared = true, title = "서재")
    private val home = PaneCommand(declared = true, title = "잿빛 새벽의 등불")

    @Test
    fun `T-DS-36 one bar per frame on an inner window`() {
        val alone = CommandBarRules.layout(800f, inner = true, list = library, detail = null, scope = LIST)
        assertEquals(1, alone.barCount)
        assertFalse(alone.showsSwitch)
        assertEquals(LIST, alone.target)
        assertEquals(800f, alone.barWidth)
        assertEquals(720f, alone.commandRowWidth)

        val both = CommandBarRules.layout(800f, inner = true, list = library, detail = home, scope = DETAIL)
        assertEquals(1, both.barCount)
        assertTrue(both.showsSwitch)
        assertEquals(listOf("서재", "잿빛 새벽의 등불"), both.cells)
        assertEquals(listOf(LIST, DETAIL), both.cellScopes)
        assertEquals(DETAIL, both.target)
        assertEquals(130, both.barHeight)

        val checks = PaneCommand(declared = true, title = "점검 센터")
        val script = PaneCommand(declared = true, title = "원고", hasExtraRow = true)
        val withRow = CommandBarRules.layout(800f, inner = true, list = checks, detail = script, scope = DETAIL)
        assertEquals(1, withRow.barCount)
        assertEquals(150, withRow.barHeight)
        // The extra row lies inside the script pane's column only: 12 dp from the divider and the frame edge.
        val span = CommandBarRules.extraRowSpan(DETAIL, frameWidth = 800f, leftWidth = 390f)
        assertEquals(403f, span.start)
        assertEquals(788f, span.endInclusive)
        assertEquals(12f..378f, CommandBarRules.extraRowSpan(LIST, frameWidth = 800f, leftWidth = 390f))
    }

    @Test
    fun `T-DS-36 a locked pane never gives its title to the switch`() {
        val locked = PaneCommand(declared = true, title = "검은 서약", locked = true)
        val layout = CommandBarRules.layout(800f, inner = true, list = library, detail = locked, scope = DETAIL)
        assertEquals(1, layout.barCount)
        assertFalse(layout.showsSwitch)
        assertTrue(layout.cells.size <= 1)
        assertTrue(layout.cells.none { it == "검은 서약" })
        // The scope pane locked: the bar acts for the other pane.
        assertEquals(LIST, layout.target)

        val nothing = CommandBarRules.layout(800f, inner = true, list = null, detail = locked, scope = DETAIL)
        assertEquals(0, nothing.barCount)
        assertNull(nothing.target)
        assertTrue(nothing.cells.isEmpty())
    }

    @Test
    fun `T-DS-36 cover window has one bar and no switch`() {
        for (scope in CommandScope.entries) {
            val layout = CommandBarRules.layout(390f, inner = false, list = library, detail = home, scope = scope)
            assertEquals(1, layout.barCount)
            assertFalse(layout.showsSwitch)
            assertEquals(366f, layout.commandRowWidth)
        }
    }

    @Test
    fun `T-DS-37 scope changes only by explicit acts`() {
        assertEquals(DETAIL, CommandScope.next(LIST, CommandScopeEvent.DetailOpened))
        assertEquals(LIST, CommandScope.next(LIST, CommandScopeEvent.Picked(LIST)))
        assertEquals(LIST, CommandScope.next(DETAIL, CommandScopeEvent.Picked(LIST)))
        assertEquals(DETAIL, CommandScope.next(LIST, CommandScopeEvent.DetailReplaced))
        assertEquals(LIST, CommandScope.next(DETAIL, CommandScopeEvent.DetailClosed))
        assertEquals(LIST, CommandScope.next(LIST, CommandScopeEvent.TouchedIn(DETAIL)))
        assertEquals(DETAIL, CommandScope.next(DETAIL, CommandScopeEvent.TouchedIn(LIST)))
        assertEquals(LIST, CommandScope.next(DETAIL, CommandScopeEvent.PaneLocked(DETAIL)))
        assertEquals(LIST, CommandScope.next(LIST, CommandScopeEvent.PaneLocked(DETAIL)))
    }

    @Test
    fun `detail key changes map to scope events`() {
        assertEquals(CommandScopeEvent.DetailOpened, CommandScopeEvent.ofDetail(null, "home"))
        assertEquals(CommandScopeEvent.DetailReplaced, CommandScopeEvent.ofDetail("home", "story"))
        assertEquals(CommandScopeEvent.DetailClosed, CommandScopeEvent.ofDetail("story", null))
        assertNull(CommandScopeEvent.ofDetail("story", "story"))

        val host = CommandBarHost(LIST)
        host.onEvent(CommandScopeEvent.ofDetail(null, "home")!!)
        assertEquals(DETAIL, host.scope)
        host.onEvent(CommandScopeEvent.Picked(LIST))
        assertEquals(LIST, host.scope)
        host.onEvent(CommandScopeEvent.TouchedIn(DETAIL))
        assertEquals(LIST, host.scope)
        host.onEvent(CommandScopeEvent.ofDetail("home", "story")!!)
        assertEquals(DETAIL, host.scope)
        // No pane declared yet: no bar, no padding.
        assertFalse(host.hasBar)
        assertEquals(0f, host.panePadding.value)
    }

    @Test
    fun `details with their own frame keep their own bar until they register`() {
        assertTrue(ListDetailPairs.detailDrawsOwnBar(Script("P", "S")))
        assertTrue(ListDetailPairs.detailDrawsOwnBar(CharacterProfile("P", "C")))
        assertFalse(ListDetailPairs.detailDrawsOwnBar(ProjectHome("P")))
        assertFalse(ListDetailPairs.detailDrawsOwnBar(null))
    }

    @Test
    fun `pane roles map to scopes and the switch copy is not invented`() {
        assertEquals(LIST, CommandScope.of(PaneRole.LIST))
        assertEquals(DETAIL, CommandScope.of(PaneRole.DETAIL))
        assertNull(CommandScope.of(PaneRole.SINGLE))
        // TODO_COPY_DS_COMMAND_SCOPE is under Requests; nothing is made up meanwhile.
        assertNull(CommandBarCopy.SCOPE_GROUP)
        assertTrue(CommandBarCopy.all.isEmpty())
    }
}
