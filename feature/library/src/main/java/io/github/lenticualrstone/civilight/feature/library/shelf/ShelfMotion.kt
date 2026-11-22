@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion

/**
 * The shared cover of the `목록 ↔ 카드` switch (CNV-45, LIB-04): the 48 dp list thumbnail and the card image are
 * one shared element, so each thumbnail grows in place into its card on `enterLong` (a snap when reduced).
 * The two pictures differ in ratio (1:1 crop and 3:4 crop), so the bounds are shared and the content
 * crossfades. [None] draws nothing shared (outside a switch, or reduced motion).
 */
@Stable
internal class SharedCovers private constructor(
    private val scope: SharedTransitionScope?,
    private val visibility: AnimatedVisibilityScope?,
    private val motion: Motion?,
) {
    /** The shared-bounds modifier of project [projectKey]'s cover with [radiusDp] at the corners. */
    @Composable
    fun cover(projectKey: String, radiusDp: Float): Modifier {
        val scope = scope ?: return Modifier
        val visibility = visibility ?: return Modifier
        val motion = motion ?: return Modifier
        if (!CardViewMotion.plan(motion.reduced).sharedImages) return Modifier
        return with(scope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "cover-$projectKey"),
                animatedVisibilityScope = visibility,
                enter = fadeIn(motion.spec(CivilightMotion.standard)),
                exit = fadeOut(motion.spec(CivilightMotion.exit)),
                boundsTransform = BoundsTransform { _, _ -> CardViewMotion.growSpec(motion) },
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(radiusDp.dp)),
            )
        }
    }

    companion object {
        val None = SharedCovers(null, null, null)

        fun of(scope: SharedTransitionScope, visibility: AnimatedVisibilityScope, motion: Motion) = SharedCovers(scope, visibility, motion)
    }
}
