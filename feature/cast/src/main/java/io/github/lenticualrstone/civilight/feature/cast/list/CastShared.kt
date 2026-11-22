@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.lenticualrstone.civilight.feature.cast.list

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
import io.github.lenticualrstone.civilight.model.Key

/**
 * The shared portrait of the `목록 ↔ 카드` switch (CNV-45, CST-04): the list avatar and the card image are one
 * shared element, so each avatar grows in place into its card on `enterLong` (a snap when reduced, where
 * the whole body crossfades instead). The two pictures differ in ratio (1:1 and 4:5 crops), so the bounds
 * are shared and the content crossfades. [None] draws nothing shared.
 *
 * A character can sit in several tag groups, so the key carries the group.
 */
@Stable
internal class CastShared private constructor(
    private val scope: SharedTransitionScope?,
    private val visibility: AnimatedVisibilityScope?,
    private val motion: Motion?,
) {
    /** The shared-bounds modifier of [characterKey]'s portrait in the group [tagKey], with [radiusDp] at the corners. */
    @Composable
    fun portrait(tagKey: Key?, characterKey: Key, radiusDp: Float): Modifier {
        val scope = scope ?: return Modifier
        val visibility = visibility ?: return Modifier
        val motion = motion ?: return Modifier
        if (!CardViewMotion.plan(motion.reduced).sharedImages) return Modifier
        return with(scope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "portrait-${tagKey.orEmpty()}-$characterKey"),
                animatedVisibilityScope = visibility,
                enter = fadeIn(motion.spec(CivilightMotion.standard)),
                exit = fadeOut(motion.spec(CivilightMotion.exit)),
                boundsTransform = BoundsTransform { _, _ -> CardViewMotion.growSpec(motion) },
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(radiusDp.dp)),
            )
        }
    }

    companion object {
        val None = CastShared(null, null, null)

        fun of(scope: SharedTransitionScope, visibility: AnimatedVisibilityScope, motion: Motion) = CastShared(scope, visibility, motion)
    }
}
