package io.github.lenticualrstone.civilight.feature.library.tour

import io.github.lenticualrstone.civilight.domain.library.onboarding.FirstUseTips
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.launch

/**
 * Shows a one-line first-use tip (LIB-20) the first time [tipId] is reached: a toast without an
 * action (`ED`), never while `기능 첫 사용 짧은 안내` is off, never twice. Returns whether it showed.
 * The rule is `domain:library` `FirstUseTips`; this is the presenter other areas can copy until a
 * shared one exists in core:ui (Request). Tip ids and copy belong to each area (not in sources).
 */
fun FeatureDeps.showFirstUseTip(tipId: String, text: String): Boolean {
    val onboarding = stores.appState.onboarding
    if (!FirstUseTips.shouldShow(onboarding.state.value, settings.value.firstUseTips, tipId)) return false
    toasts.show(text, actionLabel = null)
    appScope.launch { onboarding.update { FirstUseTips.markShown(it, tipId) } }
    return true
}
