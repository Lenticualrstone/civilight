package io.github.lenticualrstone.civilight.feature.library.onboarding

import io.github.lenticualrstone.civilight.feature.library.common.SessionRelease
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Walkthrough
import io.github.lenticualrstone.civilight.navigation.resetTo
import io.github.lenticualrstone.civilight.store.NoSuchProjectException
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The onboarding pages' shared actions (one per activity, in `LibraryServices`): the resume page,
 * navigation between pages, the walkthrough flag and `온보딩 끝내기`. Writes run in the app scope,
 * so leaving a page never cancels them.
 */
class OnboardingActions(private val deps: FeatureDeps) {
    private val appState = deps.stores.appState
    val state: StateFlow<OnboardingState> get() = appState.onboarding.state

    private val ports = object : FinishPorts {
        override fun onboarding(): OnboardingState = appState.onboarding.state.value

        override suspend fun updateOnboarding(transform: (OnboardingState) -> OnboardingState) {
            appState.onboarding.update(transform)
        }

        override fun exampleEntry(): LibraryEntry? = deps.stores.library.state.value.live.firstOrNull { it.isExample }

        override suspend fun showLibrary() = withContext(Dispatchers.Main) { deps.navigator.resetTo(Library) }

        override suspend fun releaseSessions(projectKey: Key) = SessionRelease.await(deps.projects, projectKey)

        override suspend fun deleteExample(projectKey: Key) {
            // Already gone (deleted by an earlier run of this flow): nothing left to do.
            try {
                deps.stores.deleteExampleProject(projectKey)
            } catch (e: NoSuchProjectException) {
                return
            }
        }
    }
    private val finisher = OnboardingFinisher(ports)

    /** The example's key while it is installed (LIB-17). */
    fun exampleKey(): Key? = ports.exampleEntry()?.key?.takeIf { OnboardingFlow.exampleInstalled(state.value, it) }

    /** Records [route] as the page a later launch resumes at (first run only, LIB-18 `ED`). */
    fun recordResume(route: Route) {
        val id = OnboardingFlow.resumeId(route) ?: return
        recordResumeId(id)
    }

    fun recordResumeId(id: String) {
        if (!OnboardingFlow.recordsResume(state.value) || state.value.resumePage == id) return
        deps.appScope.launch {
            appState.onboarding.update { if (OnboardingFlow.recordsResume(it)) it.copy(resumePage = id) else it }
        }
    }

    /** `워크스루 · 봤음` (LIB-21) once the last page or `건너뛰기` was reached. */
    fun markWalkthroughSeen() {
        if (state.value.walkthroughSeen) return
        deps.appScope.launch { appState.onboarding.update { it.copy(walkthroughSeen = true) } }
    }

    /** Pushes [route], or returns to it when it is already on the stack (`이전` after `다음`). */
    fun go(route: Route) = deps.navigator.push(route)

    /** `이전` on a page that was resumed without its predecessor below: swap in place. */
    fun back(route: Route) {
        if (deps.navigator.backStack.contains(route)) deps.navigator.push(route) else deps.navigator.replace(route)
    }

    fun follow(target: OnbTarget) {
        when (target) {
            is OnbTarget.Go -> go(target.route)
            OnbTarget.EndReplay -> {
                markWalkthroughSeen()
                if (!deps.navigator.popTo(inclusive = false) { it !is Walkthrough }) deps.navigator.resetTo(Library)
            }
            OnbTarget.Finish -> finish()
        }
    }

    /** `온보딩 끝내기` (LIB-18). Runs in the app scope: the screen that asked leaves on the way. */
    fun finish() {
        deps.appScope.launch { finisher.finish() }
    }

    /** Removes an example left by an interrupted `온보딩 끝내기` (called when the 서재 opens). */
    fun cleanUpLeftover() {
        deps.appScope.launch { finisher.cleanUpLeftover() }
    }

    /** `예시 프로젝트 삭제됨` is used up once the first project exists (`ED`). */
    fun useUpNoticeIfDue(library: List<LibraryEntry>) {
        if (!OnboardingFinisher.noticeUsedUp(state.value, library)) return
        deps.appScope.launch { appState.onboarding.update { it.copy(emptyLibraryNoticeShown = true) } }
    }
}
