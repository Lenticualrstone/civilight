package io.github.lenticualrstone.civilight.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Cubic-bezier control points; kept as data so tests can read them. */
data class Bezier(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
    val easing: Easing get() = CubicBezierEasing(x1, y1, x2, y2)
}

/** A motion token: curve (null = linear) and duration. */
data class MotionToken(val curve: Bezier?, val durationMs: Int) {
    val easing: Easing get() = curve?.easing ?: LinearEasing
}

/**
 * DS-14 motion tokens (identical to CNV-37). Where each plays, and the reduce-motion rules, belong to CNV.
 */
object CivilightMotion {
    val standardCurve: Bezier = Bezier(0.2f, 0f, 0f, 1f)
    val enterCurve: Bezier = Bezier(0.05f, 0.7f, 0.1f, 1f)
    val exitCurve: Bezier = Bezier(0.3f, 0f, 0.8f, 0.15f)

    val standard: MotionToken = MotionToken(standardCurve, 200)
    val enter: MotionToken = MotionToken(enterCurve, 320)
    val exit: MotionToken = MotionToken(exitCurve, 160)

    /** Linear: caret blink and recorder glow only; their durations are their own. */
    val linear: MotionToken = MotionToken(null, 0)
    val foldOpen: MotionToken = MotionToken(enterCurve, 360)
    val foldClose: MotionToken = MotionToken(exitCurve, 240)
    val enterLong: MotionToken = MotionToken(enterCurve, 360)
    val swipe: MotionToken = MotionToken(standardCurve, 260)
    val holoSweep: MotionToken = MotionToken(enterCurve, 480)

    const val reducedFadeMs: Int = 120
    const val foldOpenMs: Int = 360
    const val foldCloseMs: Int = 240
    const val enterLongMs: Int = 360
    const val swipeMs: Int = 260
    const val swipeStaggerMs: Int = 60
    const val holoSweepMs: Int = 480
    const val unhideMs: Int = 320
    const val caretPeriodMs: Int = 1000
    const val pressMs: Int = 100
    const val searchBoxStaggerMs: Int = 60

    const val sheetBackScale: Float = 0.965f
    val sheetBackBlur: Dp = 2.dp
    val unhideBlur: Dp = 7.dp
    const val unhideStartAlpha: Float = 0.55f
    const val pressScale: Float = 0.94f

    /**
     * A tween for [token]; when [reduced] every movement becomes the 120 ms linear fade
     * (callers animate opacity only in that case).
     */
    fun <T> spec(token: MotionToken, reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) tween(reducedFadeMs, easing = LinearEasing) else tween(token.durationMs, easing = token.easing)
}
