package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import io.github.lenticualrstone.civilight.model.app.PaneSplit
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.Timeline
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCardCopy
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCardRules
import io.github.lenticualrstone.civilight.ui.nav.ListDetailPairs
import io.github.lenticualrstone.civilight.ui.project.PROJECT_OPEN_FAILED
import io.github.lenticualrstone.civilight.ui.window.ListDetailCopy
import io.github.lenticualrstone.civilight.ui.window.PaneSplitCopy
import io.github.lenticualrstone.civilight.ui.window.PaneSplitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** CNV-47 pane split rules, the gallery card rules, and the copy and motion lints over core:ui. */
class PaneSplitAndMotionTest {

    @Test
    fun `handle only on inner screens where two 280 dp panes fit - CNV-47`() {
        assertTrue(PaneSplitRules.canResize(800f, inner = true))
        assertTrue(PaneSplitRules.canResize(561f, inner = true))
        assertFalse(PaneSplitRules.canResize(560f, inner = true))
        assertFalse(PaneSplitRules.canResize(800f, inner = false))
    }

    @Test
    fun `default keeps each module's own layout`() {
        assertEquals(390f, PaneSplitRules.leftWidth(800f, PaneSplit.DEFAULT, 390f), 0.01f)
        // r3: list 300 upright (`Two*` 300 + 1 + 403), 320 on its side (`L*` 320 + 1 + 612), at most half.
        assertEquals(300f, PaneSplitRules.listDetailDefaultLeft(704f), 0.01f)
        assertEquals(320f, PaneSplitRules.listDetailDefaultLeft(933f, landscape = true), 0.01f)
        assertEquals(290f, PaneSplitRules.listDetailDefaultLeft(580f), 0.01f)
        // A window too narrow for two minimums keeps today's layout whatever is stored.
        assertEquals(275f, PaneSplitRules.leftWidth(550f, PaneSplit.TWO_ONE, 275f), 0.01f)
        assertEquals(390f, PaneSplitRules.leftWidth(800f, PaneSplit.TWO_ONE, 390f, inner = false), 0.01f)
    }

    @Test
    fun `ratios share the panes and clamp to the minimum for this window`() {
        // 901 wide: 900 for the panes.
        assertEquals(300f, PaneSplitRules.leftWidth(901f, PaneSplit.ONE_TWO, 390f), 0.01f)
        assertEquals(450f, PaneSplitRules.leftWidth(901f, PaneSplit.ONE_ONE, 390f), 0.01f)
        assertEquals(600f, PaneSplitRules.leftWidth(901f, PaneSplit.TWO_ONE, 390f), 0.01f)
        // 801 wide: 1:2 would be 266.7, clamped to 280; 2:1 would leave 266.7 on the right, clamped to 520.
        assertEquals(280f, PaneSplitRules.leftWidth(801f, PaneSplit.ONE_TWO, 390f), 0.01f)
        assertEquals(520f, PaneSplitRules.leftWidth(801f, PaneSplit.TWO_ONE, 390f), 0.01f)
        assertEquals(280f, PaneSplitRules.clampLeft(100f, 801f), 0.01f)
        assertEquals(520f, PaneSplitRules.clampLeft(700f, 801f), 0.01f)
    }

    @Test
    fun `a released drag settles to the nearest ratio`() {
        assertEquals(PaneSplit.ONE_TWO, PaneSplitRules.settle(300f, 901f))
        assertEquals(PaneSplit.ONE_ONE, PaneSplitRules.settle(430f, 901f))
        assertEquals(PaneSplit.TWO_ONE, PaneSplitRules.settle(560f, 901f))
        assertEquals(0.5f, PaneSplitRules.fractionOf(450f, 901f), 0.001f)
    }

    @Test
    fun `popover buttons in order with their labels`() {
        assertEquals(listOf(PaneSplit.ONE_TWO, PaneSplit.ONE_ONE, PaneSplit.TWO_ONE, PaneSplit.DEFAULT), PaneSplitRules.CHOICES)
        assertEquals(listOf("1:2", "1:1", "2:1", "기본"), PaneSplitRules.CHOICES.map(PaneSplitRules::label))
    }

    @Test
    fun `every list module has its own ratio id`() {
        assertNull(ListDetailPairs.moduleOf(Library))
        assertNull(ListDetailPairs.moduleOf(ProjectHome("P")))
        assertEquals(PaneSplit.Modules.CAST, ListDetailPairs.moduleOf(CastList("P")))
        assertEquals(PaneSplit.Modules.WORLD, ListDetailPairs.moduleOf(World("P")))
        assertEquals(PaneSplit.Modules.TIMELINE, ListDetailPairs.moduleOf(Timeline("P")))
        assertEquals(PaneSplit.Modules.RECORDER, ListDetailPairs.moduleOf(RecorderInbox()))
        assertEquals(PaneSplit.Modules.CHECKS, ListDetailPairs.moduleOf(CheckCenter("P")))
        assertNull(ListDetailPairs.moduleOf(Script("P")))
        assertTrue(ListDetailPairs.isList(CastList("P")))
        assertFalse(ListDetailPairs.isList(Library))
        assertFalse(ListDetailPairs.isList(ProjectHome("P")))
        assertFalse(ListDetailPairs.isList(Script("P")))
    }

    @Test
    fun `gallery card sizes and chip - GAL-07`() {
        assertEquals(222f, GalleryCardRules.size(inner = false).width, 0f)
        assertEquals(296f, GalleryCardRules.size(inner = false).height, 0f)
        assertEquals(236f, GalleryCardRules.size(inner = true).width, 0f)
        assertEquals(315f, GalleryCardRules.size(inner = true).height, 0f)
        assertEquals(236f, GalleryCardRules.pitch(inner = false), 0f)
        assertEquals(252f, GalleryCardRules.pitch(inner = true), 0f)
        assertEquals("초상 · 홀로", GalleryCardRules.chip(portrait = true, background = false, cover = false, holo = true))
        assertEquals("배경 · 표지", GalleryCardRules.chip(portrait = false, background = true, cover = true, holo = false))
        assertNull(GalleryCardRules.chip(portrait = false, background = false, cover = false, holo = false))
        assertNull(GalleryCardRules.shownTitle("  "))
        assertEquals("외출복 설정", GalleryCardRules.shownTitle("외출복 설정"))
    }

    @Test
    fun `new shell copy passes CopyLint`() {
        val values = listOf(PaneSplitCopy.HANDLE, PaneSplitCopy.DEFAULT, PaneSplitCopy.ONE_TWO, PROJECT_OPEN_FAILED) +
            listOf(GalleryCardCopy.MENU, GalleryCardCopy.UNTITLED, GalleryCardCopy.PORTRAIT, GalleryCardCopy.BACKGROUND, GalleryCardCopy.COVER, GalleryCardCopy.HOLO) +
            ListDetailCopy.all
        for (v in values) assertTrue("$v: ${CopyLint.lint(v)}", CopyLint.lint(v).isEmpty())
        assertFalse(PROJECT_OPEN_FAILED.startsWith("TODO_COPY"))
    }

    @Test
    fun `empty right pane has a drawn line - Q-LY-02`() {
        assertEquals("고른 항목 없음", ListDetailCopy.NO_SELECTION)
        assertTrue(ListDetailCopy.all.none { it.isBlank() || it.startsWith("TODO_COPY") })
    }

    @Test
    fun `no TODO copy left in core ui sources`() {
        val hits = File("src/main").walkTopDown().filter { it.extension == "kt" }
            .filter { it.readText().contains("\"TODO_COPY") }.map { it.name }.toList()
        assertEquals(emptyList<String>(), hits)
    }

    @Test
    fun `core ui passes MotionLint`() {
        val issues = MotionLint.lintModule(File("."))
        assertTrue(issues.joinToString("\n"), issues.isEmpty())
    }
}
