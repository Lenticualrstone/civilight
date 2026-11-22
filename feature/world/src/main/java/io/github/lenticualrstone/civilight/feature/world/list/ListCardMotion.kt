package io.github.lenticualrstone.civilight.feature.world.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * `목록 ↔ 카드` motion (WLD-68, board `MCardView`): list text leaves first (accelerate 160 ms),
 * then the cards come in where the rows were (decelerate 360 ms, growing in place from the top
 * left). Reduced motion: 120 ms fades only, nothing moves or grows.
 */
object ListCardMotion {
    const val EXIT_MS = 160
    const val ENTER_MS = 360
    const val REDUCED_MS = 120

    /** Timing of one change; pure so it can be checked on the JVM. */
    data class Timing(val durationMs: Int, val delayMs: Int, val grow: Boolean)

    fun exitTiming(reduced: Boolean) = Timing(if (reduced) REDUCED_MS else EXIT_MS, 0, grow = false)

    fun cardTiming(reduced: Boolean) = if (reduced) Timing(REDUCED_MS, 0, grow = false) else Timing(ENTER_MS, EXIT_MS, grow = true)

    fun listTiming(reduced: Boolean) = if (reduced) Timing(REDUCED_MS, 0, grow = false) else Timing(ENTER_MS, EXIT_MS, grow = false)

    fun out(reduced: Boolean): FiniteAnimationSpec<Float> = exitTiming(reduced).let { tween(it.durationMs, it.delayMs, FastOutLinearInEasing) }

    fun cardIn(reduced: Boolean): FiniteAnimationSpec<Float> = cardTiming(reduced).let { tween(it.durationMs, it.delayMs, LinearOutSlowInEasing) }

    fun listIn(reduced: Boolean): FiniteAnimationSpec<Float> = listTiming(reduced).let { tween(it.durationMs, it.delayMs, LinearOutSlowInEasing) }

    /** Start scale of a card row growing in place. */
    const val GROW_FROM = 0.92f
}

/**
 * A card row that grows in place from its top-left corner. [switched] is true only right after the
 * author flipped `목록 → 카드` (not on first composition, scrolling or tab changes); otherwise and
 * under reduced motion it is drawn at rest.
 */
@Composable
fun GrowIn(reduced: Boolean, switched: Boolean, content: @Composable () -> Unit) {
    val timing = ListCardMotion.cardTiming(reduced).let { if (switched) it else it.copy(grow = false) }
    val scale = remember { Animatable(if (timing.grow) ListCardMotion.GROW_FROM else 1f) }
    LaunchedEffect(Unit) {
        if (timing.grow) scale.animateTo(1f, tween(timing.durationMs, timing.delayMs, LinearOutSlowInEasing))
    }
    Box(
        Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin(0f, 0f)
        },
    ) { content() }
}
