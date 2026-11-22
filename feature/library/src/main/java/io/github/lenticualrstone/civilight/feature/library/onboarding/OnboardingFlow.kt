package io.github.lenticualrstone.civilight.feature.library.onboarding

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.navigation.ExampleTour
import io.github.lenticualrstone.civilight.navigation.Onboarding
import io.github.lenticualrstone.civilight.navigation.OnboardingPage
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Walkthrough

/** Where an onboarding button leads. */
sealed interface OnbTarget {
    /** Push (or return to) [route]. */
    data class Go(val route: Route) : OnbTarget

    /** The replay from 도움말 ends: back to the screen under the walkthrough. */
    data object EndReplay : OnbTarget

    /** First run without an installed example: onboarding ends here (LIB-18 without the delete). */
    data object Finish : OnbTarget
}

/**
 * The first-run order (LIB-08, LIB-15 … LIB-18): 환영 → 화면 1/4 → 백업과 잠금 2/4 → 라이프 레코더
 * 3/4 → 워크스루 1/3 … 3/3 → 예시 둘러보기 4/4 → `온보딩 끝내기`. `나중에` = the next page without the
 * page's action; `건너뛰기` = straight to the walkthrough (§6 Q14 `ED`).
 *
 * `resumePage` values are exactly those the shell's `Startup.startRoutes` reads: an
 * [OnboardingPage] name, `WALKTHROUGH_<n>`, `EXAMPLE`, `EXAMPLE_SCRIPT`.
 *
 * Replay (LIB-21 `다시 보기`, onboarding already finished): the walkthrough runs on its own demos
 * (§6 Q9), never opens the example, never writes `resumePage`, and its last button ends the replay.
 */
object OnboardingFlow {
    const val SETUP_STEPS = 4
    const val WALK_PAGES = 3
    const val EXAMPLE = "EXAMPLE"
    const val EXAMPLE_SCRIPT = "EXAMPLE_SCRIPT"
    private const val WALK_PREFIX = "WALKTHROUGH_"

    /** The `resumePage` value of [route], or null for a route outside onboarding. */
    fun resumeId(route: Route): String? = when (route) {
        is Onboarding -> route.page.name
        is Walkthrough -> WALK_PREFIX + route.page.coerceIn(1, WALK_PAGES)
        is ExampleTour -> EXAMPLE
        else -> null
    }

    /** Whether showing [route] should record it as the resume page (never during a replay). */
    fun recordsResume(state: OnboardingState): Boolean = !state.firstRunCompleted

    /** `처음 설정 · k/4` step (1-based) of a setup page; 환영 has none. */
    fun setupStep(page: OnboardingPage): Int? = when (page) {
        OnboardingPage.WELCOME -> null
        OnboardingPage.LOOK -> 1
        OnboardingPage.SAFE -> 2
        OnboardingPage.VOICE -> 3
    }

    /** `시작하기`, `다음` and `나중에`. */
    fun next(page: OnboardingPage): Route = when (page) {
        OnboardingPage.WELCOME -> Onboarding(OnboardingPage.LOOK)
        OnboardingPage.LOOK -> Onboarding(OnboardingPage.SAFE)
        OnboardingPage.SAFE -> Onboarding(OnboardingPage.VOICE)
        OnboardingPage.VOICE -> Walkthrough(1)
    }

    /** `건너뛰기` on a setup page: the rest of 처음 설정 is skipped. */
    fun skip(): Route = Walkthrough(1)

    fun isReplay(state: OnboardingState): Boolean = state.firstRunCompleted

    /**
     * The example is there to browse (LIB-17) while its entry is live and onboarding is not over. It does not
     * wait for the `INSTALLED` flag: an example whose entry exists but whose state write was cut short
     * (QA 2026-11-23 #12) must still be shown, never deleted unseen by `Finish`. After `온보딩 끝내기`
     * (`DELETED`, `firstRunCompleted`) it is gone for good (LIB-18, §6 Q9).
     */
    fun exampleInstalled(state: OnboardingState, exampleKey: Key?): Boolean =
        exampleKey != null && !state.firstRunCompleted && state.exampleState != ExampleState.DELETED

    /** `다음` / `예시 둘러보기` / `마치기` of walkthrough [page]. */
    fun walkNext(page: Int, replay: Boolean, exampleKey: Key?): OnbTarget = when {
        page < WALK_PAGES -> OnbTarget.Go(Walkthrough(page + 1))
        else -> walkEnd(replay, exampleKey)
    }

    /** `건너뛰기` of the walkthrough: the same place as its last button. */
    fun walkSkip(replay: Boolean, exampleKey: Key?): OnbTarget = walkEnd(replay, exampleKey)

    private fun walkEnd(replay: Boolean, exampleKey: Key?): OnbTarget = when {
        replay -> OnbTarget.EndReplay
        exampleKey != null -> OnbTarget.Go(ExampleTour(exampleKey))
        else -> OnbTarget.Finish
    }

    /** `이전` from page 2 on. */
    fun walkPrevious(page: Int): Route? = if (page > 1) Walkthrough(page - 1) else null

    fun walkPrimaryLabel(page: Int, replay: Boolean): String = when {
        page < WALK_PAGES -> OnboardingCopy.NEXT
        replay -> OnboardingCopy.FINISH_REPLAY
        else -> OnboardingCopy.SEE_EXAMPLE
    }

    /** `예시 · 읽기 전용 · 온보딩 후 삭제` belongs to the first run only (the example is gone after it). */
    fun walkShowsBanner(replay: Boolean): Boolean = !replay
}
