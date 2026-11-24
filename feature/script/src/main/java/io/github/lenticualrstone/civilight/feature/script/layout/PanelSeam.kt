package io.github.lenticualrstone.civilight.feature.script.layout

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion

/**
 * The script panel's own toggle motion (r3 `LScript` / `PScript`, decision 0.2.3 · r3): closed with the shared seam
 * button or reopened from the rail (`core:ui` `PanelSeamButton` / `PanelRail`), the panel slides out of the frame to
 * the right and back on `panelSlide` (320 ms), the same token the seam button and the rail follow. Reduced motion: no
 * slide, a 120 ms fade (inside [Motion.spec]). The unfold entrance stays `panelEnter` (CNV-34).
 */
fun Motion.panelToggleEnter(): EnterTransition {
    val fade = fadeIn(spec<Float>(CivilightMotion.panelSlide))
    if (reduced) return fade
    return slideInHorizontally(move(CivilightMotion.panelSlide)) { it } + fade
}

fun Motion.panelToggleExit(): ExitTransition {
    val fade = fadeOut(spec<Float>(CivilightMotion.panelSlide))
    if (reduced) return fade
    return slideOutHorizontally(move(CivilightMotion.panelSlide)) { it } + fade
}
