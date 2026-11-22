package io.github.lenticualrstone.civilight.shell

import android.util.Log
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.navigation.ExampleTour
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.Onboarding
import io.github.lenticualrstone.civilight.navigation.OnboardingPage
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.Walkthrough
import io.github.lenticualrstone.civilight.store.ProjectKeyTakenException
import io.github.lenticualrstone.civilight.store.StoreContainer
import kotlinx.coroutines.CancellationException

/**
 * What happens before the first frame (the splash stays up meanwhile, LIB-08): settings and
 * onboarding are read, the example project 「잿빛 새벽의 등불」 is installed once on first run
 * (LIB-17: read-only, `isExample`), and the first back stack is chosen. The app lock itself is an
 * overlay, not a route.
 */
class Startup(private val stores: StoreContainer) {
    data class Result(val routes: List<Route>, val settings: AppSettings)

    /** Never throws: anything unreadable falls back to 서재 with default settings. */
    suspend fun prepare(): Result {
        val settings = attempt("settings") { stores.settings.current() } ?: AppSettings()
        attempt("app state") { stores.appState.ensureLoaded() }
        attempt("library") { stores.library.ensureLoaded() }
        val onboarding = attempt("example seed") { seedExampleIfNeeded() } ?: stores.appState.onboarding.state.value
        return Result(startRoutes(onboarding, exampleInstalled = isExampleInstalled()), settings)
    }

    /**
     * Installs the example on the very first run only: never after onboarding finished, never again
     * after `온보딩 끝내기` deleted it (DELETED). An example already on disk counts as installed.
     */
    private suspend fun seedExampleIfNeeded(): OnboardingState {
        val state = stores.appState.onboarding.state.value
        if (state.firstRunCompleted || state.exampleState != ExampleState.NOT_INSTALLED) return state
        try {
            stores.library.add(ExampleProject.build(), isExample = true, readOnly = true)
        } catch (e: ProjectKeyTakenException) {
            // Installed by an earlier run that stopped before recording it.
        }
        return stores.appState.onboarding.update { it.copy(exampleState = ExampleState.INSTALLED) }
    }

    private fun isExampleInstalled(): Boolean = stores.library.state.value.entry(EXAMPLE_KEY)?.let { it.deletedAt == null } ?: false

    private suspend fun <T> attempt(what: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Log.w(TAG, "startup: $what failed", e)
        null
    }

    companion object {
        private const val TAG = "Civilight"
        val EXAMPLE_KEY: Key = ExampleKeys.PROJECT

        fun fallback(): List<Route> = listOf(Library)

        /**
         * LIB-08 routing: first run → onboarding at the page it was left on (LIB-18 `ED`), else 서재.
         * `OnboardingState.resumePage` values (written by feature:library): an [OnboardingPage] name,
         * `WALKTHROUGH_1` … `WALKTHROUGH_3`, `EXAMPLE` (예시 둘러보기), `EXAMPLE_SCRIPT` (예시 스크립트).
         */
        fun startRoutes(onboarding: OnboardingState, exampleInstalled: Boolean): List<Route> {
            if (onboarding.firstRunCompleted) return listOf(Library)
            val page = onboarding.resumePage ?: return listOf(Onboarding())
            OnboardingPage.entries.firstOrNull { it.name == page }?.let { return listOf(Onboarding(it)) }
            if (page.startsWith(WALKTHROUGH_PREFIX)) {
                val n = page.removePrefix(WALKTHROUGH_PREFIX).toIntOrNull()?.coerceIn(1, 3) ?: 1
                return listOf(Walkthrough(n))
            }
            if (exampleInstalled) {
                when (page) {
                    EXAMPLE -> return listOf(ExampleTour(EXAMPLE_KEY))
                    EXAMPLE_SCRIPT -> return listOf(ExampleTour(EXAMPLE_KEY), Script(EXAMPLE_KEY, sceneKey = ExampleKeys.SCENE_1_7_3))
                }
            }
            return listOf(Onboarding())
        }

        const val WALKTHROUGH_PREFIX = "WALKTHROUGH_"
        const val EXAMPLE = "EXAMPLE"
        const val EXAMPLE_SCRIPT = "EXAMPLE_SCRIPT"
    }
}
