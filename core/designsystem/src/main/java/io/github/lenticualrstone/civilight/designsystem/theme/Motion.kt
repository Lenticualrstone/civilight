package io.github.lenticualrstone.civilight.designsystem.theme

import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import kotlin.math.roundToLong

/*
 * Shared motion helpers (motion audit §3.1 / §3.2). Feature code never branches on `reduced` for motion:
 * the helpers apply the one reduce rule.
 *  - Appear, disappear, swap: opacity only (120 ms linear) when reduced; no slide, scale, expand or blur.
 *  - Placement, size, offset, viewport, width: no animation when reduced (null placement, snap).
 *  - Colour, fill and alpha changes: kept, 120 ms linear when reduced (they are not movement).
 *  - Decorative loops (glow, caret blink, parallax) stop when reduced or when the system animator scale is 0.
 * Compose's own animations already follow the system animator duration scale (the window recomposer reads
 * it); hand-rolled timing (coroutine delays, frame clocks) uses [Motion.delayMs] and [Motion.loops].
 */

/** Pure rules behind [Motion]; JVM-tested. */
object MotionRules {
    /** Reduced when the app setting says so, or when the system animator duration scale is 0 (animations off). */
    fun effectiveReduced(setting: Boolean, animatorScale: Float): Boolean = setting || animatorScale <= 0f

    /** Decorative loops run only with full motion and a non-zero animator scale. */
    fun loopsRun(reduced: Boolean, animatorScale: Float): Boolean = !reduced && animatorScale > 0f

    /** A hand-rolled wait of [ms], scaled like Compose scales its animations. */
    fun scaledMs(ms: Int, animatorScale: Float): Long = (ms.coerceAtLeast(0) * animatorScale.coerceAtLeast(0f)).roundToLong()
}

/**
 * Motion settings plus the transition builders of the audit's §3.2. Constructible outside composition
 * (e.g. Nav3 metadata lambdas): pass the [density] used to turn dp offsets into pixels.
 */
@Immutable
class Motion(
    val reduced: Boolean,
    val animatorScale: Float = 1f,
    val density: Density = Density(1f),
) {
    /** Whether decorative loops (glow, caret blink, parallax, holo sway) may run. */
    val loops: Boolean get() = MotionRules.loopsRun(reduced, animatorScale)

    /** A hand-rolled wait of [ms] (coroutine `delay`), scaled by the system animator scale. */
    fun delayMs(ms: Int): Long = MotionRules.scaledMs(ms, animatorScale)

    /** Opacity / colour spec for [token]; 120 ms linear when reduced. */
    fun <T> spec(token: MotionToken = CivilightMotion.standard, delayMs: Int = 0): FiniteAnimationSpec<T> =
        CivilightMotion.spec(token, reduced, delayMs)

    /**
     * Movement spec for size, offset, width, rect, rotation and viewport (`animate*AsState`,
     * `animateContentSize`, `Animatable.animateTo`, none of which take null): [token] in full motion, a snap
     * when reduced.
     */
    fun <T> move(token: MotionToken = CivilightMotion.standard, delayMs: Int = 0): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(token.durationMs, delayMillis = delayMs.coerceAtLeast(0), easing = token.easing)

    /** Placement spec for `animateItem`: null (no move) when reduced. */
    fun placement(token: MotionToken = CivilightMotion.standard): FiniteAnimationSpec<IntOffset>? =
        if (reduced) null else tween(token.durationMs, easing = token.easing)

    /**
     * Appear: a fade on [token], plus (full motion only) a slide from [rise] (positive = from below) and an
     * expand ([expand]; [horizontal] expands from the start edge instead of the top).
     */
    fun enter(
        token: MotionToken = CivilightMotion.enter,
        rise: Dp? = null,
        expand: Boolean = false,
        horizontal: Boolean = false,
        delayMs: Int = 0,
    ): EnterTransition {
        var t: EnterTransition = fadeIn(spec(token, delayMs))
        if (reduced) return t
        if (rise != null) {
            val px = px(rise)
            t = slideInVertically(move(token, delayMs)) { px } + t
        }
        if (expand) {
            t = if (horizontal) {
                expandHorizontally(move(token, delayMs), expandFrom = Alignment.Start) + t
            } else {
                expandVertically(move(token, delayMs), expandFrom = Alignment.Top) + t
            }
        }
        return t
    }

    /** Disappear: the mirror of [enter] ([rise] is the offset it leaves towards; positive = down). */
    fun exit(
        token: MotionToken = CivilightMotion.exit,
        rise: Dp? = null,
        shrink: Boolean = false,
        horizontal: Boolean = false,
    ): ExitTransition {
        var t: ExitTransition = fadeOut(spec(token))
        if (reduced) return t
        if (rise != null) {
            val px = px(rise)
            t = slideOutVertically(move(token)) { px } + t
        }
        if (shrink) {
            t = if (horizontal) {
                shrinkHorizontally(move(token), shrinkTowards = Alignment.Start) + t
            } else {
                shrinkVertically(move(token), shrinkTowards = Alignment.Top) + t
            }
        }
        return t
    }

    /** `AnimatedContent` swap: crossfade (in on [enter], out on [exit]) with the size following on `standard`. */
    fun swap(
        enter: MotionToken = CivilightMotion.standard,
        exit: MotionToken = CivilightMotion.exit,
        clip: Boolean = true,
    ): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(spec(enter)),
        initialContentExit = fadeOut(spec(exit)),
        sizeTransform = sizeTransform(clip),
    )

    /**
     * Lens value swap (CNV-42, lifted from the lens chip): the old value leaves up [distance], the new one
     * enters from below, `standard` 200 ms; reduced: a 120 ms crossfade.
     */
    fun valueSwap(distance: Dp = CivilightMotion.valueSwapShift): ContentTransform {
        val s = CivilightMotion.standard
        if (reduced) return ContentTransform(fadeIn(spec(s)), fadeOut(spec(s)), sizeTransform = sizeTransform(true))
        val d = px(distance)
        return ContentTransform(
            targetContentEnter = slideInVertically(move(s)) { d } + fadeIn(spec(s)),
            initialContentExit = slideOutVertically(move(s)) { -d } + fadeOut(spec(s)),
            sizeTransform = sizeTransform(true),
        )
    }

    /**
     * Push / pop inside one route (wizard steps, sub-pages), shaped like the shell transition (S1): forward
     * slides in an eighth of the width on `enter` with a fade, the old page fades out on `exit`; back
     * reverses. Reduced: crossfade.
     */
    fun push(forward: Boolean = true): ContentTransform {
        if (reduced) return swap()
        val e = CivilightMotion.enter
        val x = CivilightMotion.exit
        return if (forward) {
            ContentTransform(
                slideInHorizontally(move(e)) { it / 8 } + fadeIn(spec(e)),
                fadeOut(spec(x)),
                sizeTransform = sizeTransform(true),
            )
        } else {
            ContentTransform(
                fadeIn(spec(e)),
                slideOutHorizontally(move(x)) { it / 8 } + fadeOut(spec(x)),
                sizeTransform = sizeTransform(true),
            ).also { it.targetContentZIndex = -1f }
        }
    }

    /** Side panel / second pane on unfold (CNV-34, from script `FoldMotion`): 48 dp from the right on `foldOpen`. */
    fun panelEnter(): EnterTransition {
        val t = CivilightMotion.foldOpen
        if (reduced) return fadeIn(spec(t))
        val shift = px(CivilightMotion.foldPaneShift)
        return slideInHorizontally(move(t)) { shift } + fadeIn(spec(t))
    }

    /** The pane leaves with a `foldClose` fade (CNV-34). */
    fun panelExit(): ExitTransition = fadeOut(spec(CivilightMotion.foldClose))

    private fun sizeTransform(clip: Boolean): SizeTransform =
        SizeTransform(clip) { _, _ -> move<IntSize>(CivilightMotion.standard) }

    private fun px(dp: Dp): Int = with(density) { dp.roundToPx() }

    override fun equals(other: Any?): Boolean =
        other is Motion && other.reduced == reduced && other.animatorScale == animatorScale && other.density == density

    override fun hashCode(): Int = (reduced.hashCode() * 31 + animatorScale.hashCode()) * 31 + density.hashCode()

    override fun toString(): String = "Motion(reduced=$reduced, animatorScale=$animatorScale)"
}

/** The motion settings of the current theme (provided by [CivilightTheme]). */
val LocalMotion = staticCompositionLocalOf { Motion(reduced = false) }

/**
 * Reads the system animator duration scale (developer options / "remove animations") and follows changes
 * live. 1 in previews.
 */
@Composable
fun rememberAnimatorScale(): Float {
    val view = LocalView.current
    if (view.isInEditMode) return 1f
    val resolver = LocalContext.current.contentResolver
    var scale by remember(resolver) { mutableFloatStateOf(readAnimatorScale(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scale = readAnimatorScale(resolver)
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        runCatching { resolver.registerContentObserver(uri, false, observer) }
        onDispose { runCatching { resolver.unregisterContentObserver(observer) } }
    }
    return scale
}

private fun readAnimatorScale(resolver: android.content.ContentResolver): Float =
    runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)

// ---- Composable helpers ----

/** A colour that moves to [target] on [token] (120 ms linear when reduced). Read it in draw lambdas to skip recomposition. */
@Composable
fun animatedColor(target: Color, token: MotionToken = CivilightMotion.standard, label: String = "color"): State<Color> =
    animateColorAsState(target, LocalMotion.current.spec(token), label = label)

/** 0 → 1 while [on] (alpha, fill amount, overlay), on [token]; 120 ms linear when reduced. */
@Composable
fun animatedFraction(on: Boolean, token: MotionToken = CivilightMotion.standard, label: String = "fraction"): State<Float> =
    animateFloatAsState(if (on) 1f else 0f, LocalMotion.current.spec(token), label = label)

/** An alpha (or other non-movement float) that moves to [target] on [token]; 120 ms linear when reduced. */
@Composable
fun animatedAlpha(target: Float, token: MotionToken = CivilightMotion.standard, label: String = "alpha"): State<Float> =
    animateFloatAsState(target, LocalMotion.current.spec(token), label = label)

/**
 * `animateItem` with the motion tokens (fade in on [fadeIn], move on [placement], fade out on [fadeOut]; folds
 * pass `foldOpen` / `foldClose`). Reduced: 120 ms fades and no placement move. Pass null to leave a part out.
 */
@Composable
fun LazyItemScope.civilightItem(
    fadeIn: MotionToken? = CivilightMotion.enter,
    placement: MotionToken? = CivilightMotion.standard,
    fadeOut: MotionToken? = CivilightMotion.exit,
): Modifier {
    val m = LocalMotion.current
    return Modifier.animateItem(
        fadeInSpec = fadeIn?.let { m.spec(it) },
        placementSpec = placement?.let { m.placement(it) },
        fadeOutSpec = fadeOut?.let { m.spec(it) },
    )
}

/** [civilightItem] for lazy grids (gallery `격자`). */
@Composable
fun LazyGridItemScope.civilightItem(
    fadeIn: MotionToken? = CivilightMotion.enter,
    placement: MotionToken? = CivilightMotion.standard,
    fadeOut: MotionToken? = CivilightMotion.exit,
): Modifier {
    val m = LocalMotion.current
    return Modifier.animateItem(
        fadeInSpec = fadeIn?.let { m.spec(it) },
        placementSpec = placement?.let { m.placement(it) },
        fadeOutSpec = fadeOut?.let { m.spec(it) },
    )
}

/**
 * Text whose value swaps with the lens value motion (CNV-42): old up 8 dp, new from below, 200 ms; reduced:
 * crossfade. Unchanged text does not animate.
 */
@Composable
fun ValueSwap(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = CivilightTheme.colors.ink,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
    contentAlignment: Alignment = Alignment.CenterStart,
) {
    val motion = LocalMotion.current
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = { motion.valueSwap() },
        contentAlignment = contentAlignment,
        label = "valueSwap",
    ) { value ->
        CivilightText(value, style, color = color, maxLines = maxLines, textAlign = textAlign)
    }
}

/**
 * Loading / empty / filled / error (or any `when` body) swap: a crossfade with the size following on
 * `standard`; reduced: crossfade, size snaps. [contentKey] decides what counts as a new state.
 */
@Composable
fun <S> ContentSwap(
    state: S,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    contentKey: (S) -> Any? = { it },
    label: String = "contentSwap",
    content: @Composable AnimatedContentScope.(S) -> Unit,
) {
    val motion = LocalMotion.current
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = { motion.swap() },
        contentAlignment = contentAlignment,
        label = label,
        contentKey = contentKey,
        content = content,
    )
}

/**
 * A conditional block that folds open (expand + fade on `foldOpen`) and closed (shrink + fade on
 * `foldClose`), keeping its content composed while it leaves. [horizontal] grows from the start edge (header
 * actions). Reduced: fade only.
 */
@Composable
fun FoldReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    label: String = "foldReveal",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = motion.enter(CivilightMotion.foldOpen, expand = true, horizontal = horizontal),
        exit = motion.exit(CivilightMotion.foldClose, shrink = true, horizontal = horizontal),
        label = label,
        content = content,
    )
}

/** [FoldReveal] for a nullable value: shown while non-null, and the last value stays drawn while it folds away. */
@Composable
fun <T : Any> FoldRevealValue(
    value: T?,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    label: String = "foldReveal",
    content: @Composable AnimatedVisibilityScope.(T) -> Unit,
) {
    val last = rememberLastNonNull(value)
    FoldReveal(value != null, modifier, horizontal, label) {
        if (last != null) content(last)
    }
}

/** Holds the last non-null [value], so exiting content (banner, toast, panel) stays composed while it leaves. */
@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val holder = remember { LastValue<T>() }
    if (value != null) holder.value = value
    return holder.value
}

private class LastValue<T : Any> {
    var value: T? = null
}

// ---- Unhide (CNV-43, promoted from recorder inbox/RevealMotion) ----

/** How hidden content appears when unlocked (CNV-43). */
enum class UnhideLook {
    /** Blur 7 dp → 0 and opacity 0.55 → 1 over 320 ms (API 31+). */
    BLUR,

    /** Blur not available (API < 31): the same 320 ms, opacity 0 → 1. */
    FADE,

    /** Reduced motion: a 120 ms opacity fade. */
    QUICK_FADE,
}

/** Pure values of the unhide at a progress 0 (hidden look) … 1 (clear); JVM-tested. */
object UnhideMotion {
    fun look(reduced: Boolean, sdk: Int = Build.VERSION.SDK_INT): UnhideLook = when {
        reduced -> UnhideLook.QUICK_FADE
        sdk >= 31 -> UnhideLook.BLUR
        else -> UnhideLook.FADE
    }

    fun durationMs(look: UnhideLook): Int = if (look == UnhideLook.QUICK_FADE) CivilightMotion.reducedFadeMs else CivilightMotion.unhideMs

    /** Blur radius in dp at [progress]; 0 for the fades. */
    fun blurDp(look: UnhideLook, progress: Float): Float =
        if (look == UnhideLook.BLUR) CivilightMotion.unhideBlur.value * (1f - progress.coerceIn(0f, 1f)) else 0f

    /** Opacity at [progress]: 0.55 → 1 under the blur, 0 → 1 for the fades. */
    fun alpha(look: UnhideLook, progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return if (look == UnhideLook.BLUR) CivilightMotion.unhideStartAlpha + (1f - CivilightMotion.unhideStartAlpha) * p else p
    }
}

/**
 * The running unhide of one group: [value] is the progress (0 hidden look … 1 clear), [look] how it draws,
 * [isRunning] true while it plays (wait for it instead of a fixed delay).
 */
@Stable
class Unhide internal constructor(val look: UnhideLook, private val anim: Animatable<Float, AnimationVector1D>) : State<Float> {
    override val value: Float get() = anim.value
    val isRunning: Boolean get() = anim.isRunning
}

/**
 * Plays the unhide when [revealed] turns true and the reverse (re-lock) when it turns false. A group that is
 * already revealed on first composition starts clear (process restore is a cut). Draw it with [unhide].
 */
@Composable
fun rememberUnhide(revealed: Boolean): Unhide {
    val motion = LocalMotion.current
    val look = UnhideMotion.look(motion.reduced)
    val anim = remember { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(revealed, look) {
        val target = if (revealed) 1f else 0f
        if (anim.value != target) {
            anim.animateTo(target, tween(UnhideMotion.durationMs(look), easing = if (look == UnhideLook.QUICK_FADE) androidx.compose.animation.core.LinearEasing else CivilightMotion.enter.easing))
        }
    }
    return remember(look, anim) { Unhide(look, anim) }
}

/** Draws [unhide] on this node (blur + alpha in the draw phase, so the rows do not recompose per frame). */
fun Modifier.unhide(unhide: Unhide): Modifier = unhide(unhide.look) { unhide.value }

/** Draws the unhide look at [progress] (read in the draw phase). */
fun Modifier.unhide(look: UnhideLook, progress: () -> Float): Modifier = graphicsLayer {
    val p = progress()
    alpha = UnhideMotion.alpha(look, p)
    val r = UnhideMotion.blurDp(look, p).dp.toPx()
    renderEffect = if (r > 0f && Build.VERSION.SDK_INT >= 31) BlurEffect(r, r, TileMode.Decal) else null
}

/** The current [Motion] (also `CivilightTheme.motion`). */
val currentMotion: Motion
    @Composable @ReadOnlyComposable get() = LocalMotion.current
