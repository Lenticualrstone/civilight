package io.github.lenticualrstone.civilight.designsystem.card

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion

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
 * `목록 ↔ 카드` (CNV-45), the shared copy of canvas `card/CardViewMotion.kt`: each list thumbnail (48 × 48,
 * radius 10) grows in place into its card, keeping order and position, `enterLong` 360 ms; list text leaves
 * first (`exit` 160 ms) and card text comes in after it on `standard`; card → list reverses. Reduce motion: a
 * 120 ms crossfade and nothing moves. Use with `SharedTransitionLayout` + `sharedBounds(boundsTransform =
 * { _, _ -> growSpec(motion) })` on the thumbnail.
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

    /** The bounds animation of a growing thumbnail: `enterLong` 360 ms; a snap when reduced. */
    fun growSpec(motion: Motion): FiniteAnimationSpec<Rect> = motion.move(CivilightMotion.enterLong)

    /**
     * The content transform of the switch: text out on `exit`, text in on `standard` after the exit. The
     * container size does not animate (the shared thumbnails carry the movement).
     */
    fun transform(motion: Motion): ContentTransform {
        val p = plan(motion.reduced)
        val enter = fadeIn(motion.spec(CivilightMotion.standard, delayMs = p.textInDelayMs))
        val exit = fadeOut(motion.spec(CivilightMotion.exit))
        return ContentTransform(enter, exit, sizeTransform = SizeTransform(clip = false) { _, _ -> snap<IntSize>() })
    }
}
