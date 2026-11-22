package io.github.lenticualrstone.civilight.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.navigation.BackStackNavigator
import io.github.lenticualrstone.civilight.navigation.FeatureModule
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.NavResults
import io.github.lenticualrstone.civilight.navigation.Navigator
import io.github.lenticualrstone.civilight.navigation.RouteCodec
import io.github.lenticualrstone.civilight.navigation.SceneSplitSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.ui.fakefeature.FakeSpeakerSheet
import io.github.lenticualrstone.civilight.ui.fakefeature.MARK
import io.github.lenticualrstone.civilight.ui.feature.FeatureBinding
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.FeatureLoader
import io.github.lenticualrstone.civilight.ui.feature.SheetRegistry
import io.github.lenticualrstone.civilight.ui.feature.registerShellSheets
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.security.BiometricGate
import io.github.lenticualrstone.civilight.ui.security.CaptureGuard
import io.github.lenticualrstone.civilight.ui.security.UnavailableBiometricGate
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import io.github.lenticualrstone.civilight.ui.window.WindowLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The feature entry contract (shell.md) exercised from a feature's side with `fakefeature`. */
class FeatureContractTest {
    private class FakeDeps : FeatureDeps {
        override val stores: StoreContainer get() = error("not used")
        override val navigator: Navigator = BackStackNavigator(mutableListOf<NavKey>(Library))
        override val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings())
        override val biometric: BiometricGate = UnavailableBiometricGate
        override val clock: AppClock get() = error("not used")
        override val windowLayout: State<WindowLayout> = mutableStateOf(WindowLayout.Cover)
        override val projects: ProjectSessions get() = error("not used")
        override val unlock = UnlockController(now = { 0L }, policy = { AppSettings().relockPolicy }, appLockEnabled = { false })
        override val capture = CaptureGuard()
        override val toasts: ToastController get() = error("not used")
        override val results = NavResults()
        override val sheets = SheetRegistry().apply { registerShellSheets() }
        override val appScope: CoroutineScope get() = error("not used")
    }

    private val fake = FeatureBinding(FeatureModule.SCRIPT, "io.github.lenticualrstone.civilight.ui.fakefeature.FakeEntries", "fakeEntries")
    private val absent = FeatureBinding(FeatureModule.STORY, "io.github.lenticualrstone.civilight.feature.story.NotWrittenYet", "storyEntries")
    private val bad = FeatureBinding(FeatureModule.CAST, "io.github.lenticualrstone.civilight.ui.fakefeature.BadEntries", "badEntries")

    @Test
    fun `a feature found by its JVM name registers entries and shared sheets, the rest falls back`() {
        val deps = FakeDeps()
        var called: Map<FeatureModule, Boolean> = emptyMap()
        val provider = entryProvider<NavKey>(fallback = { key -> NavEntry(key, metadata = mapOf(PLACEHOLDER to true)) {} }) {
            called = FeatureLoader.registerAll(this, deps, listOf(fake, absent))
        }
        assertEquals(mapOf(FeatureModule.SCRIPT to true, FeatureModule.STORY to false), called)
        assertEquals(true, provider(Script("P")).metadata[MARK])
        assertEquals(true, provider(Library).metadata[PLACEHOLDER])
        assertTrue(deps.sheets.isRegistered(SceneSplitSheet::class))
        assertTrue(deps.sheets.contentFor(SceneSplitSheet("P", "B")) != null)
    }

    @Test
    fun `a feature with the wrong signature fails loudly`() {
        try {
            entryProvider<NavKey>(fallback = { key -> NavEntry(key) {} }) { FeatureLoader.registerAll(this, FakeDeps(), listOf(bad)) }
            fail("expected the loader to refuse BadEntries")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("badEntries"))
        }
    }

    @Test
    fun `a shared sheet cannot be registered twice and local sheet keys save with the host`() {
        val registry = SheetRegistry().apply { registerShellSheets() }
        try {
            registry.registerShellSheets()
            fail("expected a duplicate registration to fail")
        } catch (e: IllegalArgumentException) {
            assertFalse(e.message.isNullOrEmpty())
        }
        val host = SheetHost(listOf(FakeSpeakerSheet("B1"), SceneSplitSheet("P", "B2")))
        val saved = RouteCodec.encodeAll(host.sheets)
        assertEquals(host.sheets, RouteCodec.decodeAll(saved))
        host.close()
        assertEquals(FakeSpeakerSheet("B1"), host.top)
        assertNotEquals(saved, RouteCodec.encodeAll(host.sheets))
    }

    private companion object {
        const val PLACEHOLDER = "placeholder"
    }
}
