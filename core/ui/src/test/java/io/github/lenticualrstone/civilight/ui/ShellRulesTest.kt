package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.designsystem.theme.TextScale
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.FeatureModule
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.Settings
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.ui.feature.FeatureContract
import io.github.lenticualrstone.civilight.ui.nav.ListDetailPairs
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.security.CaptureGuard
import io.github.lenticualrstone.civilight.ui.security.TaskLabel
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge
import io.github.lenticualrstone.civilight.ui.window.WidthClass
import io.github.lenticualrstone.civilight.ui.window.WindowLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellRulesTest {

    @Test
    fun `window classes - CNV-32`() {
        assertEquals(WidthClass.COMPACT, WindowLayout.classifyWidth(390))
        assertEquals(WidthClass.COMPACT, WindowLayout.classifyWidth(599))
        assertEquals(WidthClass.MEDIUM, WindowLayout.classifyWidth(600))
        assertEquals(WidthClass.MEDIUM, WindowLayout.classifyWidth(802))
        assertEquals(WidthClass.EXPANDED, WindowLayout.classifyWidth(840))
        assertFalse(WindowLayout.Cover.isInner)
        // 0.2.3: `가운데 한 칸` is gone; the inner screen always draws two panes.
        assertTrue(WindowLayout.Inner.twoPanes)
        assertTrue(WindowLayout.InnerLandscape.twoPanes)
        assertFalse(WindowLayout.Cover.twoPanes)
    }

    @Test
    fun `list and detail pairs - CNV-33`() {
        assertFalse(ListDetailPairs.isPair(Library, ProjectHome("P")))
        assertFalse(ListDetailPairs.isPair(ProjectHome("P"), StoryStructure("P")))
        assertTrue(ListDetailPairs.isPair(CastList("P"), CharacterProfile("P", "C")))
        assertTrue(ListDetailPairs.isPair(World("P"), TermCard("P", "T")))
        assertTrue(ListDetailPairs.isPair(CheckCenter("P"), Script("P")))
        assertFalse(ListDetailPairs.isPair(CastList("P"), CharacterProfile("Q", "C")))
        assertFalse(ListDetailPairs.isPair(World("P"), CharacterProfile("P", "C")))
        assertFalse(ListDetailPairs.isPair(Settings, Script("P")))
        assertFalse(ListDetailPairs.isList(Library))
        assertTrue(ListDetailPairs.isList(CastList("P")))
        assertFalse(ListDetailPairs.isList(Script("P")))
    }

    @Test
    fun `choosing another row swaps the detail instead of stacking it`() {
        val stack = mutableListOf<androidx.navigation3.runtime.NavKey>(Library, ProjectHome("Q"))
        val nav = io.github.lenticualrstone.civilight.navigation.BackStackNavigator(stack)
        nav.push(CastList("Q"))
        nav.openDetail(CharacterProfile("Q", "A"))
        nav.openDetail(CharacterProfile("Q", "B"))
        assertEquals(listOf(Library, ProjectHome("Q"), CastList("Q"), CharacterProfile("Q", "B")), stack)
    }

    @Test
    fun `window protection per project - LIB-26, LIB-29, S-05 to S-07`() {
        val hidden = WindowProtection.forProject(Protections(hidden = true))
        assertFalse(hidden.secure)
        assertTrue(hidden.hideRecentsPreview)
        assertEquals(TaskLabel.APP_NAME, hidden.taskLabel)
        val masked = WindowProtection.forProject(Protections(captureBlock = true, nameMask = true))
        assertTrue(masked.secure)
        assertEquals("이름 가림", masked.taskLabel?.text)
        assertTrue(WindowProtection.forProject(Protections()).isNone)
        assertEquals(TaskLabel.APP_NAME, (masked + hidden).taskLabel)
    }

    @Test
    fun `capture guard is reference counted - a leaving screen keeps the entering one's flag`() {
        val applied = mutableListOf<WindowProtection>()
        val guard = CaptureGuard { applied += it }
        val entering = Any()
        val leaving = Any()
        guard.set(leaving, WindowProtection(secure = true))
        guard.set(entering, WindowProtection(secure = true))
        guard.remove(leaving)
        assertTrue(guard.current.secure)
        guard.remove(entering)
        assertFalse(guard.current.secure)
        // Only real changes reach the window.
        assertEquals(2, applied.size)
        assertTrue(applied.first().secure)
    }

    @Test
    fun `theme bridge - text step and motion`() {
        assertEquals(TextScale.forStep(2), ThemeBridge.textScale(AppSettings()))
        assertEquals(TextScale.forStep(4), ThemeBridge.textScale(AppSettings(textSizeStep = 2)))
        assertTrue(ThemeBridge.reducedMotion(AnimationPref.ALWAYS_REDUCE, systemAnimationsOff = false))
        assertTrue(ThemeBridge.reducedMotion(AnimationPref.FOLLOW_SYSTEM, systemAnimationsOff = true))
        assertFalse(ThemeBridge.reducedMotion(AnimationPref.FOLLOW_SYSTEM, systemAnimationsOff = false))
        assertFalse(ThemeBridge.reducedMotion(AnimationPref.NEVER_REDUCE, systemAnimationsOff = true))
    }

    @Test
    fun `every feature module has one fixed entry binding`() {
        val modules = FeatureContract.bindings.map { it.module }
        assertEquals(FeatureModule.entries.filter { it != FeatureModule.SHELL }, modules)
        val script = FeatureContract.bindings.single { it.module == FeatureModule.SCRIPT }
        assertEquals("io.github.lenticualrstone.civilight.feature.script.ScriptEntries", script.className)
        assertEquals("scriptEntries", script.functionName)
        assertNull(FeatureContract.bindings.firstOrNull { it.module == FeatureModule.SHELL })
    }
}
