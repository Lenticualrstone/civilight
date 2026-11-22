package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion

/**
 * The selected-cell indicator shared by tabs (underline) and segmented controls (pill), motion audit S6: in
 * full motion it slides and resizes on `standard`; with reduced motion it does not move: the old one fades
 * out while the new one fades in (120 ms). The first placement never animates.
 */
@Stable
internal class SlidingIndicator {
    /** Cell bounds (x, width) in the row's coordinates, filled by `onPlaced`. */
    val bounds: SnapshotStateMap<Int, Pair<Float, Float>> = mutableStateMapOf()
    private val x = Animatable(0f)
    private val w = Animatable(0f)
    private val fade = Animatable(1f)
    private var fromX by mutableFloatStateOf(0f)
    private var fromW by mutableFloatStateOf(0f)

    suspend fun moveTo(tx: Float, tw: Float, motion: Motion) {
        when {
            w.value == 0f -> {
                x.snapTo(tx)
                w.snapTo(tw)
                fade.snapTo(1f)
            }
            motion.reduced -> {
                if (x.value == tx && w.value == tw) return
                fromX = x.value
                fromW = w.value
                x.snapTo(tx)
                w.snapTo(tw)
                fade.snapTo(0f)
                fade.animateTo(1f, motion.spec(CivilightMotion.standard))
            }
            else -> {
                fade.snapTo(1f)
                val spec = motion.move<Float>(CivilightMotion.standard)
                launchBoth({ x.animateTo(tx, spec) }, { w.animateTo(tw, spec) })
            }
        }
    }

    /** Draws the indicator through [block] (x, width, alpha); call from a draw lambda. */
    fun DrawScope.drawIndicator(block: DrawScope.(x: Float, width: Float, alpha: Float) -> Unit) {
        val f = fade.value
        if (f < 1f && fromW > 0f) block(fromX, fromW, 1f - f)
        if (w.value > 0f) block(x.value, w.value, f)
    }
}

/** Remembers a [SlidingIndicator] that follows [selectedIndex]. */
@Composable
internal fun rememberSlidingIndicator(selectedIndex: Int): SlidingIndicator {
    val indicator = remember { SlidingIndicator() }
    val motion = LocalMotion.current
    val target = indicator.bounds[selectedIndex]
    LaunchedEffect(target, motion.reduced) {
        val t = target ?: return@LaunchedEffect
        indicator.moveTo(t.first, t.second, motion)
    }
    return indicator
}
