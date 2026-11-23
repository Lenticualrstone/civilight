package io.github.lenticualrstone.civilight.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
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

    /** CSS `ease-in-out`: the recorder glow sweep only (DS-61). */
    val easeInOutCurve: Bezier = Bezier(0.42f, 0f, 0.58f, 1f)

    // D1 (orchestrator 2026-11-23): map and text-to-map motions of the CNV-37 registry.
    /** `단계 다시`: the new layer flows in (after the old one leaves on [exit]). */
    val mapReroll: MotionToken = MotionToken(standardCurve, 400)

    /** `글로 설명`: a condition row appears 250 ms after its underline. */
    val rowIn: MotionToken = MotionToken(standardCurve, 250)

    /** `지도 생성`: the whole step-order draw, about 1.2 s. */
    val mapGenerate: MotionToken = MotionToken(standardCurve, 1200)

    /** `도장`: drop from 14 dp above and settle (D1 says standard easing; CNV-37 lists ENTER, see status). */
    val stampDrop: MotionToken = MotionToken(standardCurve, 240)

    // D3: recorder glow durations as tokens; reduced motion stops the glow (STILL).
    /** PROCESSING sweep of the glow band group, ±90 dp, alternating (DS-61). */
    val recorderGlowSweep: MotionToken = MotionToken(easeInOutCurve, 2400)

    /** RECORDING level oscillation of each band, alternating (DS-61). */
    const val recorderLevelPeriodMs: Int = 1600

    /** Phase shift between neighbouring glow bands. */
    const val recorderBandPhaseMs: Int = 230

    // D2: card rows (CNV-22 / CNV-45).
    /** Image parallax inside a card: the image moves 8 % less than its card. */
    const val cardParallax: Float = 0.08f

    /** Constant image scale under the parallax, so the shifted image never shows an edge (1.0 when reduced). */
    const val cardImageScale: Float = 1.1f

    // Holo (CNV-30): springs, so the system animator scale applies; values unchanged from feature:canvas.
    /** Released drag sways back to rest (board `Holo`): period 1.4 s, decay 0.9 s, stiffness (2π / 1.4 s)² ≈ 20. */
    const val holoSwayStiffness: Float = 20f

    /** Damping ratio of the drag sway (about 0.25: a few slow swings). */
    const val holoSwayDamping: Float = 0.25f

    /** Device-tilt follow: Compose `StiffnessMediumLow`, so a noisy sensor reads smoothly. */
    const val holoTiltStiffness: Float = Spring.StiffnessMediumLow

    /** Damping ratio of the tilt follow: critically damped (no bounce). */
    const val holoTiltDamping: Float = Spring.DampingRatioNoBouncy

    /** Fold pane shift (CNV-34): the second pane comes in from 48 dp to the right. */
    val foldPaneShift: Dp = 48.dp

    /** Lens value swap travel (CNV-42). */
    val valueSwapShift: Dp = 8.dp

    // 0.2.0 (U-14, U-15).
    /** `씬 n 끝에` flash when the scene in view changes (SCR-71): fill in, hold, out on `standard`, 600 ms in all. */
    val placeFlash: MotionToken = MotionToken(standardCurve, 600)

    /** A new list row's `hl` highlight after `만들기` (DS-72), faded out on `standard`. */
    const val createdRowHighlightMs: Int = 1200

    const val sheetBackScale: Float = 0.965f
    val sheetBackBlur: Dp = 2.dp
    val unhideBlur: Dp = 7.dp
    const val unhideStartAlpha: Float = 0.55f
    const val pressScale: Float = 0.94f

    /** Press feedback (scale or overlay): `pressMs` on the standard curve. */
    val press: MotionToken = MotionToken(standardCurve, pressMs)

    /**
     * A tween for [token]; when [reduced] every movement becomes the 120 ms linear fade
     * (callers animate opacity only in that case).
     */
    fun <T> spec(token: MotionToken, reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) tween(reducedFadeMs, easing = LinearEasing) else tween(token.durationMs, easing = token.easing)

    /** [spec] that starts after [delayMs] (the delay is kept under reduced motion so order is kept). */
    fun <T> spec(token: MotionToken, reduced: Boolean, delayMs: Int): FiniteAnimationSpec<T> =
        if (reduced) {
            tween(reducedFadeMs, delayMillis = delayMs.coerceAtLeast(0), easing = LinearEasing)
        } else {
            tween(token.durationMs, delayMillis = delayMs.coerceAtLeast(0), easing = token.easing)
        }
}
