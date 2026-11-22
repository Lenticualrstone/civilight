package io.github.lenticualrstone.civilight.domain.library.onboarding

import io.github.lenticualrstone.civilight.model.app.OnboardingState

/**
 * One-line first-use tips (LIB-20): a tip shows the first time its feature is used, once per tip
 * id, never again, and never while `기능 첫 사용 짧은 안내` (AppSettings.firstUseTips) is off. Each
 * module area owns its tip ids (`<area>.<feature>`) and copy; presentation is a toast without an
 * action (`ED`). Shown ids live in `OnboardingState.tipsShown`.
 */
object FirstUseTips {
    /** Whether [tipId] shows now. */
    fun shouldShow(state: OnboardingState, tipsOn: Boolean, tipId: String): Boolean =
        tipsOn && tipId.isNotBlank() && tipId !in state.tipsShown

    /** Records that [tipId] was shown (it never shows again until `모든 안내 처음부터`). */
    fun markShown(state: OnboardingState, tipId: String): OnboardingState =
        if (tipId in state.tipsShown) state else state.copy(tipsShown = state.tipsShown + tipId)

    /**
     * `모든 안내 처음부터` (LIB-21): every module tour and every tip id start over, and the
     * walkthrough reads `아직 안 봄` again (`ED`). The example project is not reinstalled, and the
     * rest of onboarding (finished, example deleted, resume page) is kept.
     */
    fun resetAll(state: OnboardingState): OnboardingState = state.copy(
        walkthroughSeen = false,
        moduleTourSeen = TourModule.entries.associate { it.id to false },
        tipsShown = emptySet(),
    )
}
