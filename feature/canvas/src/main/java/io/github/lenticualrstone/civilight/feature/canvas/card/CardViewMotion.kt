package io.github.lenticualrstone.civilight.feature.canvas.card

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.geometry.Rect
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion

/** Timings of one `목록 ↔ 카드` switch (CNV-45, board `MCardView`). */
data class CardViewPlan(
    /** The thumbnail grows in place into its card (null = no movement). */
    val growMs: Int?,
    /** List text leaves first (EXIT). */
    val textOutMs: Int,
    /** Card text enters after the list text has left. */
    val textInDelayMs: Int,
    val textInMs: Int,
    /** Thumbnails and card images are one shared element. */
    val sharedImages: Boolean,
)

/**
 * `목록 ↔ 카드` (CNV-45): each list thumbnail (48 × 48, radius 10) grows in place into its card, keeping
 * order and position, ENTER 360 ms; list text leaves first (EXIT 160 ms) and card text comes in after;
 * card → list reverses. Reduce motion: a 120 ms crossfade and nothing moves.
 */
object CardViewMotion {
    const val THUMB_DP = 48f
    const val THUMB_RADIUS_DP = 10f

    fun plan(reduced: Boolean): CardViewPlan =
        if (reduced) {
            CardViewPlan(null, CivilightMotion.reducedFadeMs, 0, CivilightMotion.reducedFadeMs, sharedImages = false)
        } else {
            CardViewPlan(
                growMs = CivilightMotion.enterLong.durationMs,
                textOutMs = CivilightMotion.exit.durationMs,
                textInDelayMs = CivilightMotion.exit.durationMs,
                textInMs = CivilightMotion.standard.durationMs,
                sharedImages = true,
            )
        }

    /** The bounds animation of a growing thumbnail: ENTER over 360 ms. */
    fun growSpec(): FiniteAnimationSpec<Rect> = tween(CivilightMotion.enterLong.durationMs, easing = CivilightMotion.enterLong.easing)

    /** The content transform of the switch. */
    fun transform(reduced: Boolean): ContentTransform {
        val p = plan(reduced)
        val enter = fadeIn(tween(p.textInMs, delayMillis = p.textInDelayMs, easing = if (reduced) LinearEasing else CivilightMotion.standard.easing))
        val exit = fadeOut(tween(p.textOutMs, easing = if (reduced) LinearEasing else CivilightMotion.exit.easing))
        return ContentTransform(enter, exit, sizeTransform = SizeTransform(clip = false))
    }
}
