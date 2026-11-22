package io.github.lenticualrstone.civilight.feature.library.onboarding

import io.github.lenticualrstone.civilight.domain.library.newproject.Onboarding
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What finishing onboarding touches outside this module (stores, sessions, back stack). */
interface FinishPorts {
    fun onboarding(): OnboardingState

    suspend fun updateOnboarding(transform: (OnboardingState) -> OnboardingState)

    /** The example project's live 서재 row, if it is still installed. */
    fun exampleEntry(): LibraryEntry?

    /** Leaves every screen of onboarding and the example: the stack becomes [서재] (empty `OnbEmpty`). */
    suspend fun showLibrary()

    /** Waits until no screen holds the project's store any more and the cached store is closed. */
    suspend fun releaseSessions(projectKey: Key)

    /** Deletes the example without the trash, with its recorder links and recents (LIB-18, LRC-23). */
    suspend fun deleteExample(projectKey: Key)
}

/**
 * `온보딩 끝내기` (LIB-18, P-07): onboarding is marked finished first (so an interrupted run never
 * brings the onboarding back), then the 서재 is shown, the example's screens and cached store are let
 * go, and the example is deleted for good (no trash entry). [cleanUpLeftover] finishes a delete that
 * was cut off (process death between the two writes).
 */
class OnboardingFinisher(private val ports: FinishPorts) {
    /**
     * One delete at a time: the 서재 that [finish] shows runs [cleanUpLeftover] while [finish] still
     * waits for the example's store, and a second tap on `온보딩 끝내기` runs [finish] again. Each
     * re-reads the example inside the lock, so the example is deleted exactly once.
     */
    private val deleting = Mutex()

    suspend fun finish() {
        val example = ports.exampleEntry()
        ports.updateOnboarding { state -> finishedState(state, exampleExisted = example != null || state.exampleState == ExampleState.DELETED) }
        ports.showLibrary()
        deleteExampleIfPresent()
    }

    /** An example still on disk after onboarding finished with `DELETED` is removed now. */
    suspend fun cleanUpLeftover() {
        val state = ports.onboarding()
        if (!state.firstRunCompleted || state.exampleState != ExampleState.DELETED) return
        deleteExampleIfPresent()
    }

    private suspend fun deleteExampleIfPresent() = deleting.withLock {
        val example = ports.exampleEntry() ?: return@withLock
        ports.releaseSessions(example.key)
        // Re-read after the wait: another path may have deleted it meanwhile.
        if (ports.exampleEntry()?.key == example.key) ports.deleteExample(example.key)
    }

    companion object {
        /**
         * The state after `온보딩 끝내기` (domain:library `Onboarding.finish`): finished, no resume page,
         * example `DELETED`. A first run whose example was never installed keeps `NOT_INSTALLED`, so
         * the empty 서재 does not claim `예시 프로젝트 삭제됨`.
         */
        fun finishedState(state: OnboardingState, exampleExisted: Boolean): OnboardingState {
            val finished = Onboarding.finish(state, emptyList(), RecorderLibrary()).state
            val neverInstalled = !exampleExisted && state.exampleState == ExampleState.NOT_INSTALLED
            return if (neverInstalled) finished.copy(exampleState = ExampleState.NOT_INSTALLED) else finished
        }

        /**
         * `예시 프로젝트 삭제됨` shows until the first project exists (`ED`): once a live project other
         * than the example exists, the notice is used up for good.
         */
        fun noticeUsedUp(state: OnboardingState, library: List<LibraryEntry>): Boolean =
            state.exampleState == ExampleState.DELETED &&
                !state.emptyLibraryNoticeShown &&
                library.any { it.deletedAt == null && !it.isExample }
    }
}
