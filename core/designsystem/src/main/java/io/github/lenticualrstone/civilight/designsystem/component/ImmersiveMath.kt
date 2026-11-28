package io.github.lenticualrstone.civilight.designsystem.component

import kotlin.math.roundToInt

/**
 * The numbers of the immersive detail screen (`design/immersive-detail-header/project/Immersive.dc.html`,
 * rules table `ImmSpec.dc.html`) as pure functions; JVM-tested. Lengths are in one unit chosen by the caller
 * (dp or px) unless a name says `Dp`.
 *
 * Driving values, as in the board code:
 *  - `p`  = clamp(scroll / D, 0, 1), D = viewport height × 0.5 (linear): bottom shade, subtitle.
 *  - `e`  = smoothstep(p): title x / y / scale, hero scale, hero blur, tone, panel glass alpha.
 *  - `hp` = clamp((p − 0.5) / 0.4, 0, 1): header alpha, title + icon colour, button circle, title shadow.
 *  - `q`  = clamp((scroll − D) / (0.6 D), 0, 1): the extra hero blur 22 → 48 after the header docks.
 */
object ImmersiveMath {
    /** D = viewport height × this. */
    const val DISTANCE_FRACTION: Float = 0.5f

    /** The header ramp runs over p [HEADER_START] … [HEADER_START] + [HEADER_SPAN] (0.5 … 0.9). */
    const val HEADER_START: Float = 0.5f
    const val HEADER_SPAN: Float = 0.4f

    /** Extra blur runs over scroll D … (1 + [EXTRA_SPAN]) D. */
    const val EXTRA_SPAN: Float = 0.6f

    /** Width (dp) above which the inner-screen class applies (title 40, gutter 32). */
    const val WIDE_MIN_DP: Float = 600f
    const val GUTTER_COVER_DP: Float = 24f
    const val GUTTER_WIDE_DP: Float = 32f
    const val TITLE_START_COVER_SP: Float = 34f
    const val TITLE_START_WIDE_SP: Float = 40f
    const val TITLE_DOCKED_SP: Float = 17f
    const val TITLE_LINE_RATIO: Float = 1.24f

    /** Docked title start x (after the 44 dp back button). */
    const val TITLE_DOCKED_X_DP: Float = 64f

    /** Header row below the status-bar inset. */
    const val HEADER_ROW_DP: Float = 56f

    /** Header buttons: 44 dp, 6 dp below the inset, gutter − 12 from the edges. */
    const val BUTTON_DP: Float = 44f
    const val BUTTON_TOP_DP: Float = 6f
    const val BUTTON_EDGE_PULL_DP: Float = 12f
    const val BUTTON_CIRCLE_ALPHA: Float = 0.34f
    const val BUTTON_BLUR_DP: Float = 12f

    /** Bottom bar: 64 dp row + 24 dp mask fade above it; tabs 6 dp above its bottom. */
    const val BAR_ROW_DP: Float = 64f
    const val BAR_FADE_DP: Float = 24f
    const val BAR_CONTENT_BOTTOM_DP: Float = 6f

    /** Title / subtitle block: 20 dp above the bar row; subtitle 20 line + 4 gap. */
    const val TITLE_ABOVE_BAR_DP: Float = 20f
    const val SUBTITLE_LINE_DP: Float = 20f
    const val SUBTITLE_GAP_DP: Float = 4f
    const val SUBTITLE_FADE_END: Float = 0.32f
    const val SUBTITLE_RISE_DP: Float = 12f
    const val SUBTITLE_ALPHA: Float = 0.84f

    const val TITLE_SHADOW_ALPHA: Float = 0.38f
    const val TITLE_SHADOW_BLUR_DP: Float = 14f
    const val SUBTITLE_SHADOW_ALPHA: Float = 0.35f
    const val SUBTITLE_SHADOW_BLUR_DP: Float = 10f

    const val HERO_SCALE_MAX: Float = 1.08f
    const val HERO_ORIGIN_X: Float = 0.5f
    const val HERO_ORIGIN_Y: Float = 0.3f
    const val HERO_BLUR_DOCKED_DP: Float = 22f
    const val HERO_BLUR_EXTRA_DP: Float = 26f

    /**
     * Hero blur under reduced motion (`이미지 블러 고정`): the board gives no value; 0 keeps the art sharp and
     * the panel glass still blurs behind itself. To confirm.
     */
    const val HERO_BLUR_REDUCED_DP: Float = 0f

    const val TONE_MAX: Float = 0.30f

    /** Tone when the hero cannot blur (API < 31, scrim / opaque glass): stronger, never opaque. Own value. */
    const val TONE_MAX_NO_BLUR: Float = 0.62f

    /** Bottom shade (to top): alpha .68 at 0, .30 at 34 %, 0 at 64 %; fades completely out by p 0.75. */
    val SHADE_STOPS: List<Pair<Float, Float>> = listOf(0f to 0.68f, 0.34f to 0.30f, 0.64f to 0f)
    const val SHADE_FADE_END: Float = 0.75f

    /** Spring detent thresholds inside 0 … dockDistance (directional hysteresis + flick velocity). */
    const val SNAP_UP_THRESHOLD: Float = 0.22f
    const val SNAP_DOWN_THRESHOLD: Float = 0.78f
    const val SNAP_FLING_PX_S: Float = 350f

    const val PANEL_RADIUS_DP: Float = 24f
    const val PANEL_TOP_PAD_DP: Float = 28f
    const val PANEL_BOTTOM_EXTRA_DP: Float = 32f
    const val PANEL_ALPHA_DARK_START: Float = 0.56f
    const val PANEL_ALPHA_DARK_END: Float = 0.64f
    const val PANEL_ALPHA_LIGHT_START: Float = 0.62f
    const val PANEL_ALPHA_LIGHT_END: Float = 0.70f

    fun clamp01(v: Float): Float = if (v.isNaN()) 0f else v.coerceIn(0f, 1f)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Where the glass content panel starts at scroll 0: right at the top of the bottom bar / inset. */
    fun panelStartY(viewportHeight: Float, bottomInset: Float = 0f, hasBar: Boolean = true): Float =
        (viewportHeight - bottomInset - (if (hasBar) BAR_ROW_DP else 0f)).coerceAtLeast(0f)

    /**
     * Synchronized scroll distance from rest (scroll 0) to docked (panel top touches header bottom):
     * `panelStartY - headerHeight`. Title, header and panel arrive at the exact same scroll pixel.
     */
    fun dockDistance(
        viewportHeight: Float,
        statusInset: Float = 0f,
        bottomInset: Float = 0f,
        hasBar: Boolean = true,
    ): Float {
        if (viewportHeight <= 0f) return 0f
        return (panelStartY(viewportHeight, bottomInset, hasBar) - headerHeight(statusInset)).coerceAtLeast(1f)
    }

    /** Legacy fallback D = viewportHeight × 0.5 when no header/panel metrics are supplied. */
    fun distance(viewportHeight: Float): Float = viewportHeight * DISTANCE_FRACTION

    /** p for an explicit dock distance [dockDist]. */
    fun progressForDistance(scroll: Float, dockDist: Float): Float =
        if (dockDist <= 0f) 0f else clamp01(scroll / dockDist)

    /** p. 0 while the viewport is unknown. */
    fun progress(scroll: Float, viewportHeight: Float): Float = progressForDistance(scroll, distance(viewportHeight))

    /** smoothstep. */
    fun ease(p: Float): Float {
        val c = clamp01(p)
        return c * c * (3f - 2f * c)
    }

    /** hp. */
    fun headerAlpha(p: Float): Float = clamp01((p - HEADER_START) / HEADER_SPAN)

    /** q for an explicit dock distance [dockDist]. */
    fun extraBlurForDistance(scroll: Float, dockDist: Float): Float =
        if (dockDist <= 0f) 0f else clamp01((scroll - dockDist) / (dockDist * EXTRA_SPAN))

    /** q. */
    fun extraBlurProgress(scroll: Float, viewportHeight: Float): Float = extraBlurForDistance(scroll, distance(viewportHeight))

    /**
     * Spring detent target inside the hero ↔ docked transition zone (0 < scrollPx < dockPx).
     * Flicks follow velocity; slow releases use directional hysteresis so small pulls commit crisply.
     */
    fun snapTarget(scrollPx: Float, dockPx: Float, startScrollPx: Float, velocityPxPerSec: Float = 0f): Float {
        if (dockPx <= 0f || scrollPx <= 0f || scrollPx >= dockPx) return scrollPx.coerceAtLeast(0f)
        if (velocityPxPerSec > SNAP_FLING_PX_S) return dockPx
        if (velocityPxPerSec < -SNAP_FLING_PX_S) return 0f
        val p = scrollPx / dockPx
        val movingUp = scrollPx >= startScrollPx
        return if (movingUp) {
            if (p >= SNAP_UP_THRESHOLD) dockPx else 0f
        } else {
            if (p <= SNAP_DOWN_THRESHOLD) 0f else dockPx
        }
    }

    /** Reduced motion: one step at p ≥ 0.5 (no interpolation). */
    fun snapped(p: Float): Float = if (p >= HEADER_START) 1f else 0f

    fun isWide(widthDp: Float): Boolean = widthDp > WIDE_MIN_DP

    fun gutterDp(wide: Boolean): Float = if (wide) GUTTER_WIDE_DP else GUTTER_COVER_DP

    /** Title size at rest: 34 cover, 40 inner. */
    fun titleStartSp(wide: Boolean): Float = if (wide) TITLE_START_WIDE_SP else TITLE_START_COVER_SP

    /** Line height of the resting title: round(size × 1.24) → 42 / 50. */
    fun titleLineHeight(sizeSp: Float): Float = (sizeSp * TITLE_LINE_RATIO).roundToInt().toFloat()

    /** Title scale 1 → 17 / T0. */
    fun titleScale(e: Float, wide: Boolean): Float = 1f + (TITLE_DOCKED_SP / titleStartSp(wide) - 1f) * e

    fun titleDockedScale(wide: Boolean): Float = TITLE_DOCKED_SP / titleStartSp(wide)

    /** Docked title x: 64 after the back button, the gutter when there is no back button (no empty slot). */
    fun titleDockedX(wide: Boolean, hasBack: Boolean = true): Float = if (hasBack) TITLE_DOCKED_X_DP else gutterDp(wide)

    /** Title x: gutter → 64 (stays at the gutter without a back button). */
    fun titleX(e: Float, wide: Boolean, hasBack: Boolean = true): Float = lerp(gutterDp(wide), titleDockedX(wide, hasBack), e)

    /** Bottom of the title / subtitle block: 20 above the bar row (bar row only when there is a bar). */
    fun blockBottom(viewportHeight: Float, bottomInset: Float, hasBar: Boolean): Float =
        viewportHeight - bottomInset - (if (hasBar) BAR_ROW_DP else 0f) - TITLE_ABOVE_BAR_DP

    /** Resting title top y0 = H − bar − 20 − (20 + 4 with a subtitle) − LH. */
    fun titleStartY(
        viewportHeight: Float,
        lineHeight: Float,
        bottomInset: Float = 0f,
        hasBar: Boolean = true,
        hasSubtitle: Boolean = true,
    ): Float {
        val sub = if (hasSubtitle) SUBTITLE_LINE_DP + SUBTITLE_GAP_DP else 0f
        return blockBottom(viewportHeight, bottomInset, hasBar) - sub - lineHeight
    }

    /** Docked title top y1: centred on the header row (inset + 28), at the docked scale. */
    fun titleDockedY(statusInset: Float, lineHeight: Float, wide: Boolean): Float =
        statusInset + HEADER_ROW_DP / 2f - lineHeight * titleDockedScale(wide) / 2f

    fun titleY(e: Float, startY: Float, dockedY: Float): Float = lerp(startY, dockedY, e)

    /** Title colour mix white → ink (and icons). */
    fun titleInkMix(hp: Float): Float = clamp01(hp)

    fun titleShadowAlpha(hp: Float): Float = TITLE_SHADOW_ALPHA * (1f - clamp01(hp))

    /** Subtitle opacity 1 → 0 over p 0 … 0.32. */
    fun subtitleAlpha(p: Float): Float = clamp01(1f - p / SUBTITLE_FADE_END)

    /** Subtitle shift −12 p (over the whole p, as in the board: −3.84 when it has faded at p 0.32). */
    fun subtitleShiftDp(p: Float): Float = -SUBTITLE_RISE_DP * clamp01(p)

    fun headerHeight(statusInset: Float): Float = statusInset + HEADER_ROW_DP

    fun buttonCircleAlpha(hp: Float): Float = BUTTON_CIRCLE_ALPHA * (1f - clamp01(hp))

    fun heroScale(e: Float): Float = 1f + (HERO_SCALE_MAX - 1f) * e

    /** Hero blur 0 → 22 (e), then + 26 (q) → 48; fixed under reduced motion; 0 when blur is unavailable. */
    fun heroBlurDp(e: Float, q: Float, reduced: Boolean = false, canBlur: Boolean = true): Float = when {
        !canBlur -> 0f
        reduced -> HERO_BLUR_REDUCED_DP
        else -> HERO_BLUR_DOCKED_DP * e + HERO_BLUR_EXTRA_DP * q
    }

    /** Background-colour tone over the hero: 0 → .30 (0 → .62 without blur). */
    fun toneAlpha(e: Float, canBlur: Boolean = true): Float = (if (canBlur) TONE_MAX else TONE_MAX_NO_BLUR) * e

    /** Bottom shade opacity 1 → 0 over p 0 … 0.75 so no dark smear lingers behind the reading panel. */
    fun shadeAlpha(p: Float): Float = clamp01(1f - clamp01(p) / SHADE_FADE_END)

    /**
     * Panel glass alpha: dark .74 → .86, light .80 → .90 (e). Reading content stays crisp and legible
     * while softly blurring the hero backdrop underneath.
     */
    fun panelGlassAlpha(e: Float, dark: Boolean, canBlur: Boolean = true): Float {
        val start = if (dark) PANEL_ALPHA_DARK_START else PANEL_ALPHA_LIGHT_START
        val end = if (dark) PANEL_ALPHA_DARK_END else PANEL_ALPHA_LIGHT_END
        return if (canBlur) lerp(start, end, e) else end
    }

    /** Bottom padding inside the panel: bar row + 32 (+ the navigation inset). */
    fun panelBottomPadding(bottomInset: Float, hasBar: Boolean): Float =
        bottomInset + (if (hasBar) BAR_ROW_DP else 0f) + PANEL_BOTTOM_EXTRA_DP

    /** Bar height: 64 + 24 fade (+ the navigation inset). */
    fun barHeight(bottomInset: Float): Float = bottomInset + BAR_ROW_DP + BAR_FADE_DP

    /**
     * The bottom inset the scaffold uses: [override] when the screen already reserves the space below
     * (e.g. a `목록 + 상세` pane padded by the frame bar), else the navigation-bar inset. Never negative.
     */
    fun effectiveBottomInset(navigationInset: Float, override: Float?): Float = (override ?: navigationInset).coerceAtLeast(0f)

    /** Smallest side of a tap target over the art (the title's hit box grows to it, never the drawn title). */
    const val HIT_MIN_DP: Float = 44f

    /** Extra room on each side so a [sizeDp] long side of the title's hit box reaches [HIT_MIN_DP]. */
    fun hitPadDp(sizeDp: Float): Float = ((HIT_MIN_DP - sizeDp) / 2f).coerceAtLeast(0f)

    /** The title is a tap target only while it rests over the art (not docked in the header). */
    fun titleTappable(hasClick: Boolean, docked: Boolean): Boolean = hasClick && !docked
}

/**
 * Every value of one frame at [scroll] in a [viewportHeight] viewport. [reduced]: no interpolation; title,
 * header, tone and panel switch at p ≥ 0.5 (the scaffold fades alpha / colour over 120 ms, positions snap).
 */
data class ImmersiveFrame(
    val p: Float,
    val e: Float,
    val hp: Float,
    val q: Float,
) {
    companion object {
        fun at(scroll: Float, viewportHeight: Float, reduced: Boolean = false): ImmersiveFrame =
            atDistance(scroll, ImmersiveMath.distance(viewportHeight), reduced)

        fun atDistance(scroll: Float, dockDistance: Float, reduced: Boolean = false): ImmersiveFrame {
            val p = ImmersiveMath.progressForDistance(scroll, dockDistance)
            return if (reduced) {
                val s = ImmersiveMath.snapped(p)
                ImmersiveFrame(p = p, e = s, hp = s, q = 0f)
            } else {
                ImmersiveFrame(
                    p = p,
                    e = ImmersiveMath.ease(p),
                    hp = ImmersiveMath.headerAlpha(p),
                    q = ImmersiveMath.extraBlurForDistance(scroll, dockDistance),
                )
            }
        }
    }
}
