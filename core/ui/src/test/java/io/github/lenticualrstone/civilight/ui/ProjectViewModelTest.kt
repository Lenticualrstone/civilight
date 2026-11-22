package io.github.lenticualrstone.civilight.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.store.DeviceId
import io.github.lenticualrstone.civilight.store.ManualClock
import io.github.lenticualrstone.civilight.store.StoreEnvironment
import io.github.lenticualrstone.civilight.store.library.LibraryStore
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectViewModelTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private class ExampleVm(key: String, sessions: ProjectSessions) : ProjectViewModel(key, sessions)

    private fun TestScope.setUp(): Pair<LibraryStore, ProjectSessions> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val env = StoreEnvironment(scope, dispatcher, ManualClock(ExampleClock.NOW), DeviceId.fixed("test-device"))
        val library = LibraryStore(tmp.root, env)
        return library to ProjectSessions(scope, library, closeDelayMs = 1_000)
    }

    private fun make(store: ViewModelStore, key: String, sessions: ProjectSessions): ExampleVm =
        ViewModelProvider.create(store, viewModelFactory { initializer { ExampleVm(key, sessions) } })[ExampleVm::class]

    @Test
    fun `the example opens read-only, app state still changes, and the store closes after the last screen`() = runTest {
        try {
            val (library, sessions) = setUp()
            library.add(ExampleProject.build(), isExample = true, readOnly = true)
            val owner = ViewModelStore()
            val vm = make(owner, ExampleKeys.PROJECT, sessions)
            runCurrent()
            val ready = vm.state.value as ProjectLoad.Ready
            assertTrue(ready.readOnly)
            assertEquals(ExampleKeys.PROJECT, vm.doc.value?.key)

            assertEquals(EditResult.READ_ONLY, vm.update("이름 바꾸기") { it.copy(project = it.project.copy(name = "x")) })
            vm.updateApp { app -> app.copy(view = app.view.copy(lens = LensRules.setLensToScene(app.view.lens, ExampleKeys.SCENE_1_7_3))) }
            runCurrent()
            assertEquals(LensPoint.UnitEnd(ExampleKeys.SCENE_1_7_3), vm.doc.value?.app?.view?.lens?.savedForScript)
            assertNotNull(sessions.peek(ExampleKeys.PROJECT))

            owner.clear()
            runCurrent()
            advanceTimeBy(1_001)
            runCurrent()
            assertNull(sessions.peek(ExampleKeys.PROJECT))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `a project that is gone reports a failure instead of loading forever`() = runTest {
        try {
            val (_, sessions) = setUp()
            val vm = make(ViewModelStore(), "01JZ000000PRJTGONE00000000", sessions)
            runCurrent()
            assertTrue(vm.state.value is ProjectLoad.Failed)
            assertNull(vm.doc.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
