package io.github.lenticualrstone.civilight.feature.library.help

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.library.onboarding.OnboardingFlow
import io.github.lenticualrstone.civilight.model.app.OnboardingState

/** Copy of 도움말 · 안내 다시 보기 (LIB-21, board `HelpReplay`). */
object HelpCopy {
    const val HEAD_SMALL = "설정"
    const val TITLE = "도움말 · 안내 다시 보기"
    const val SECTION_FIRST = "처음 안내"
    const val WALKTHROUGH = "워크스루"
    const val SECTION_MODULES = "모듈 안내 · 처음 들어갈 때"
    const val REPLAY = "다시 보기"
    const val TIPS = "기능 첫 사용 짧은 안내"
    const val TIPS_SUB = "한 줄 · 한 번만"
    const val RESET_ALL = "모든 안내 처음부터"
    const val SEEN = "봤음"
    const val NOT_SEEN = "아직 안 봄"
    const val STEPS_SUFFIX = "단계"

    /** `3단계 · 봤음`. */
    fun stepsLine(steps: Int, seen: Boolean): String = "$steps$STEPS_SUFFIX · ${if (seen) SEEN else NOT_SEEN}"
}

/** One replay row: title, `<n>단계 · 봤음|아직 안 봄`, tag `다시 보기`. [module] null = the walkthrough. */
data class ReplayRow(val title: String, val sub: String, val module: TourModule?)

data class HelpReplayUi(val walkthrough: ReplayRow, val modules: List<ReplayRow>, val tipsOn: Boolean)

/** Builds the LIB-21 rows from `OnboardingState` and the tips switch. */
object HelpReplayState {
    fun build(state: OnboardingState, tipsOn: Boolean): HelpReplayUi = HelpReplayUi(
        walkthrough = ReplayRow(HelpCopy.WALKTHROUGH, HelpCopy.stepsLine(OnboardingFlow.WALK_PAGES, state.walkthroughSeen), null),
        modules = TourModule.entries.map { m ->
            ReplayRow(m.rowTitle, HelpCopy.stepsLine(ModuleTours.stepCount(m), ModuleTours.seen(state, m)), m)
        },
        tipsOn = tipsOn,
    )
}
