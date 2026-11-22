package io.github.lenticualrstone.civilight.feature.recorder.inbox

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** How the hidden rows' text appears (LRC-41, `MotionSpec` / board `MUnlock`). */
enum class RevealLook {
    /** `흐림 7 px → 0 · 320 ms`, opacity 0.55 → 1. */
    BLUR,

    /** Blur not available (API < 31): the same 320 ms, opacity only. */
    FADE,

    /** Reduced motion: a 120 ms fade. */
    QUICK_FADE,
}

/**
 * The hidden-record reveal (LRC-41): `숨김 풀기 · 흐림 7 px → 0 · 320 ms`, `다시 잠글 때 반대로`, reduced
 * motion → 120 ms fade. Pure values at a progress 0 (hidden look) … 1 (clear), so the timing is
 * JVM-tested; [revealText] draws them on the text paragraph only (the board blurs the `<p>`).
 */
object RevealMotion {
    const val DURATION_MS = 320
    const val REDUCED_MS = 120
    const val BLUR_DP = 7f
    const val START_ALPHA = 0.55f

    fun look(reducedMotion: Boolean, sdk: Int = Build.VERSION.SDK_INT): RevealLook = when {
        reducedMotion -> RevealLook.QUICK_FADE
        sdk >= 31 -> RevealLook.BLUR
        else -> RevealLook.FADE
    }

    fun durationMs(look: RevealLook): Int = if (look == RevealLook.QUICK_FADE) REDUCED_MS else DURATION_MS

    /** Blur radius in dp at [progress]; 0 for the fades. */
    fun blurDp(look: RevealLook, progress: Float): Float = if (look == RevealLook.BLUR) BLUR_DP * (1f - progress.coerceIn(0f, 1f)) else 0f

    /** Opacity at [progress]: 0.55 → 1 under the blur (as drawn), 0 → 1 for the fades. */
    fun alpha(look: RevealLook, progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return if (look == RevealLook.BLUR) START_ALPHA + (1f - START_ALPHA) * p else p
    }
}

/** Draws [RevealMotion] at [progress] (read in the draw phase, so the animation never recomposes the rows). */
fun Modifier.revealText(look: RevealLook, progress: () -> Float): Modifier = graphicsLayer {
    val p = progress()
    alpha = RevealMotion.alpha(look, p)
    val r = RevealMotion.blurDp(look, p).dp.toPx()
    renderEffect = if (r > 0f && Build.VERSION.SDK_INT >= 31) BlurEffect(r, r, TileMode.Decal) else null
}
