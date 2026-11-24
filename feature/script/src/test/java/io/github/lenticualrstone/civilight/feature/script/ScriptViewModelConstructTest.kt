package io.github.lenticualrstone.civilight.feature.script

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.navigation.BackStackNavigator
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.NavResults
import io.github.lenticualrstone.civilight.navigation.Navigator
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.store.DeviceId
import io.github.lenticualrstone.civilight.store.ManualClock
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.store.StoreEnvironment
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetRegistry
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.security.BiometricGate
import io.github.lenticualrstone.civilight.ui.security.CaptureGuard
import io.github.lenticualrstone.civilight.ui.security.UnavailableBiometricGate
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import io.github.lenticualrstone.civilight.ui.window.WindowLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * (0.2.0 UP-05) On a device `viewModelScope` runs on `Dispatchers.Main.immediate`, so every launch in
 * an `init` block starts inside the constructor. A collector that touches a field declared further down
 * the class sees it still null (the 0.2.0 release crashed on every script open). The unconfined
 * main dispatcher here behaves the same way.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScriptViewModelConstructTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private class Deps(
        override val stores: StoreContainer,
        override val projects: ProjectSessions,
        override val appScope: CoroutineScope,
    ) : FeatureDeps {
        override val navigator: Navigator = BackStackNavigator(mutableListOf<NavKey>(Library))
        override val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings())
        override val biometric: BiometricGate = UnavailableBiometricGate
        override val clock: AppClock get() = stores.clock
        override val windowLayout: State<WindowLayout> = mutableStateOf(WindowLayout.Cover)
        override val unlock = UnlockController(now = { 0L }, policy = { AppSettings().relockPolicy }, appLockEnabled = { false })
        override val capture = CaptureGuard()
        override val toasts = ToastController(appScope)
        override val results = NavResults()
        override val sheets = SheetRegistry()
    }

    @Test
    fun `the script view model builds on an immediate main dispatcher, in a new project`() = runTest {
        val main = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        val scope = CoroutineScope(SupervisorJob() + main)
        val dataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val owner = ViewModelStore()
        try {
            val env = StoreEnvironment(scope, main, ManualClock.at("2026-11-24T00:00:00Z"), DeviceId.fixed("test-device"))
            val stores = StoreContainer(
                tmp.root,
                PreferenceDataStoreFactory.create(scope = dataScope, produceFile = { File(tmp.root, "datastore/settings.preferences_pb") }),
                env,
            )
            val project = stores.library.create("새 작품")
            val deps = Deps(stores, ProjectSessions(scope, stores.library, closeDelayMs = 0), scope)
            deps.unlock.unlockItems(project.key)

            val vm = ViewModelProvider.create(owner, viewModelFactory { initializer { ScriptViewModel(Script(project.key), deps) } })[ScriptViewModel::class]
            runCurrent()

            assertEquals(io.github.lenticualrstone.civilight.feature.script.input.Draft(), vm.draft.value)
            // The gate collector's first value is kept, not reset by a later `= false` initialiser.
            assertTrue(vm.gateOpen)
        } finally {
            owner.clear()
            scope.cancel()
            dataScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `two script screens of one project share one draft, and the lower one never writes an older one back`() = runTest {
        val main = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        val scope = CoroutineScope(SupervisorJob() + main)
        val dataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val lowerOwner = ViewModelStore()
        val upperOwner = ViewModelStore()
        try {
            val env = StoreEnvironment(scope, main, ManualClock.at("2026-11-24T00:00:00Z"), DeviceId.fixed("test-device"))
            val stores = StoreContainer(
                tmp.root,
                PreferenceDataStoreFactory.create(scope = dataScope, produceFile = { File(tmp.root, "datastore/settings.preferences_pb") }),
                env,
            )
            val project = stores.library.create("새 작품")
            val deps = Deps(stores, ProjectSessions(scope, stores.library, closeDelayMs = 0), scope)
            val lower = ViewModelProvider.create(lowerOwner, viewModelFactory { initializer { ScriptViewModel(Script(project.key), deps) } })[ScriptViewModel::class]
            lower.setDraft(io.github.lenticualrstone.civilight.feature.script.input.Draft(runs = listOf(io.github.lenticualrstone.civilight.model.text.TextRun.Plain("준비"))))
            val upper = ViewModelProvider.create(upperOwner, viewModelFactory { initializer { ScriptViewModel(Script(project.key, blockKey = "01JZ000000BLCK000000000001"), deps) } })[ScriptViewModel::class]
            runCurrent()
            assertEquals("준비", upper.draft.value.text)
            upper.setDraft(io.github.lenticualrstone.civilight.feature.script.input.Draft(runs = listOf(io.github.lenticualrstone.civilight.model.text.TextRun.Plain("준비된 문장 하나 더"))))
            // Back on the lower screen: it shows the newer draft, and leaving the upper one keeps it.
            assertEquals("준비된 문장 하나 더", lower.draft.value.text)
            upperOwner.clear()
            runCurrent()
            assertEquals("준비된 문장 하나 더", lower.draft.value.text)
            assertEquals("준비된 문장 하나 더", DraftMemory.get(project.key)?.text)
        } finally {
            lowerOwner.clear()
            upperOwner.clear()
            scope.cancel()
            dataScope.cancel()
            Dispatchers.resetMain()
        }
    }
}
