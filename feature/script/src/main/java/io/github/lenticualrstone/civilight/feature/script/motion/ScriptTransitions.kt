package io.github.lenticualrstone.civilight.feature.script.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion

/**
 * The bars of the script swap between reading, typing and select mode with a crossfade (motion audit,
 * feature:script row 1). The bar's height, and with it the manuscript's top and bottom padding, changes
 * at once: the writing line (SCR-14) is pinned to those paddings pixel for pixel, so they never glide.
 */
fun Motion.barSwap(): ContentTransform = ContentTransform(
    targetContentEnter = fadeIn(spec(CivilightMotion.standard)),
    initialContentExit = fadeOut(spec(CivilightMotion.exit)),
    sizeTransform = SizeTransform(clip = false) { _, _ -> snap() },
)
