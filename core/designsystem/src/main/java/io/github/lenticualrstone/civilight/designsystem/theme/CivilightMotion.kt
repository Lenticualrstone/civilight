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

    /**
     * (0.2.3, r3) A side panel closing or opening: the panel slides out while the main pane widens (r3 `L*` /
     * `Two*`: 320 ms, standard). Reduced: the geometry snaps and the panel fades 120 ms (`move` / `spec`).
     */
    val panelSlide: MotionToken = MotionToken(standardCurve, 320)

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

    /**
     * Holo idle sway (board `Holo` `.card.auto`: `hSway` / `hFoil` / `hSheen 7s ease-in-out infinite`): with no
     * gyro and no finger the foil drifts across and back once per 7 s, each half on CSS `ease-in-out`.
     */
    const val holoIdleMs: Int = 7000
    val holoIdle: MotionToken = MotionToken(easeInOutCurve, holoIdleMs)

    // 0.2.0 (U-16, DS-74): the 3D 서재 views and character card views.
    /**
     * A 3D view's focus settling after a drag, a step or a pick (서재 views, card views). Critically damped
     * like the tilt follow: the boards draw a light overshoot, but give no value for it (open in the spec).
     */
    const val viewSettleStiffness: Float = Spring.StiffnessMediumLow

    /** Damping ratio of [viewSettleStiffness]: no bounce. */
    const val viewSettleDamping: Float = Spring.DampingRatioNoBouncy

    // 0.2.1 (U-16 cards boards `scr_u16_cards_a/b/c.py`): the springs as the boards run them. Board springs
    // given as (k, c) are stiffness k, damping ratio c / (2√k); (k, ζ) are used as is; (ζ, ω) give k = ω².
    /** 부채꼴 덱 · 회전 목마 focus (board `sp(st.cur, 180, 24)`). */
    const val cardDeckStiffness: Float = 180f
    const val cardDeckDamping: Float = 0.894f

    /** 부채꼴 덱 card turn (board `sp(st.flip, 210, 22)`). */
    const val cardFlipStiffness: Float = 210f
    const val cardFlipDamping: Float = 0.759f

    /** 겹친 더미: a card sliding back in under the pile (board `to(slot, 170, 0.82)`). */
    const val cardPileStiffness: Float = 170f
    const val cardPileDamping: Float = 0.82f

    /** 겹친 더미: a dragged card let go short of a throw springs back (board `settle`: 230, 0.72). */
    const val cardPileBackStiffness: Float = 230f
    const val cardPileBackDamping: Float = 0.72f

    /** 타로: a card dealt to its place (150, 0.78), lifted (220, 0.9) and laid down (170, 0.72). */
    const val tarotDealStiffness: Float = 150f
    const val tarotDealDamping: Float = 0.78f
    const val tarotLiftStiffness: Float = 220f
    const val tarotLiftDamping: Float = 0.9f
    const val tarotDropStiffness: Float = 170f
    const val tarotDropDamping: Float = 0.72f

    /** 타로: the focus card rising (240, 0.66) and a card turning over (160, 0.6). */
    const val tarotFocusStiffness: Float = 240f
    const val tarotFocusDamping: Float = 0.66f
    const val tarotTurnStiffness: Float = 160f
    const val tarotTurnDamping: Float = 0.6f

    /** 타로: one card dealt after another, and the lift before a card lays down again. */
    const val tarotDealStaggerMs: Int = 110
    const val tarotDropDelayMs: Int = 180
    const val tarotTurnDropMs: Int = 190

    /** 기울어진 벽: a card pulled out of the wall (board `SP_MOVE`, ζ 0.74, ω 12). */
    const val wallPullStiffness: Float = 144f
    const val wallPullDamping: Float = 0.74f

    /** 롤로덱스: the drum settling on a card (board `SP_BNC`, ζ 0.56, ω 13). */
    const val rolodexStiffness: Float = 169f
    const val rolodexDamping: Float = 0.56f

    /** Idle sway of a card stage while nothing tilts it (boards: `0.45 sin(2πt / 9 s)`, `0.3 sin(2πt / 6.5 s)`). */
    const val cardSwayPeriodXMs: Int = 9000
    const val cardSwayPeriodYMs: Int = 6500
    const val cardSwayX: Float = 0.45f
    const val cardSwayY: Float = 0.3f

    /** One slow turn of the 별자리 view's idle orbit (LIB-93); no orbit under reduced motion. */
    const val starOrbitPeriodMs: Long = 90_000L

    // 0.2.1 (U-16 boards `scr_u16_lib_a` … `c`): the 서재 3D view springs. The boards sample a damped spring
    // into CSS `linear()` that settles at the transition's end: ω = 6.9 / (ζ · duration) (board A) or
    // ω = 10 / duration (board C); stiffness = ω² (mass 1). Board B's `cubic-bezier(.32,1.28,.52,1)` is read as
    // ζ 0.65 settling in its duration.
    /** Board A `spr` (ζ 0.62): a book's slot pulling out (680 ms), a cover-flow step (680 ms). */
    const val libPullStiffness: Float = 268f
    const val libSpringDamping: Float = 0.62f

    /** Board A `spr` 600 ms: the book turning from spine to cover (`.bk`). */
    const val libTurnStiffness: Float = 344f

    /** Board A `soft` (ζ 0.84) 640 ms: the opened book zooming to fill the stage; 700 ms for the flipped cover. */
    const val libOpenStiffness: Float = 165f
    const val libOpenFlowStiffness: Float = 138f
    const val libSoftDamping: Float = 0.84f

    /** Board A delays: the turn starts 160 ms after the pull, the slot returns 140 ms after the turn back. */
    const val libTurnDelayMs: Long = 160L
    const val libReturnDelayMs: Long = 140L

    /** Board A / B: the opened project takes over (home fade) this long after `열기` starts the open move. */
    const val libOpenHandoffMs: Long = 520L

    /** Board B sheets (720 ms) and corridor bays (800 ms), ζ 0.65. */
    const val libSheetStiffness: Float = 217f
    const val libBayStiffness: Float = 176f
    const val libBoardBDamping: Float = 0.65f

    /** Board B: the fan opens one sheet after another 45 ms apart; the pile gathers 30 ms apart. */
    const val libFanStaggerMs: Long = 45L
    const val libPileStaggerMs: Long = 30L

    /** Board B corridor camera: `a = 140 · (target − cam) − 21 · v`, so stiffness 140, ζ = 21 / (2√140). */
    const val libWalkStiffness: Float = 140f
    const val libWalkDamping: Float = 0.887f

    /** Board C: a drawer card flipping (560 ms, ζ 0.62) and pulled out (680 ms, ζ 0.8). */
    const val libFlipStiffness: Float = 319f
    const val libDrawerPullStiffness: Float = 216f
    const val libDrawerPullDamping: Float = 0.8f

    /** Board C: a star book coming forward (900 ms, ζ 0.62) and going back (700 ms, ζ 0.8). */
    const val libStarFocusStiffness: Float = 123f
    const val libStarReturnStiffness: Float = 204f

    /** Board C idle sway (`lcIdle 11s ease-in-out infinite`): rotateY ±2.2°, rotateX ±0.8°, sheen ±18 %. */
    const val libIdleSwayMs: Long = 11_000L

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

    /**
     * (0.2.4) A long-pressed item lifting out of its list while its menu is open (the script block menu): it
     * grows to this scale on [enter] and settles back on [exit]. Reduced motion: no scale, the shadow only.
     */
    const val liftScale: Float = 1.02f

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
