package io.github.lenticualrstone.civilight.feature.map.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import io.github.lenticualrstone.civilight.designsystem.theme.rememberUnhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import kotlinx.coroutines.delay
import androidx.compose.runtime.State
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken

/**
 * Motion pieces shared by the map screens (motion audit §4 feature:map). Every duration and curve is a
 * `CivilightMotion` token through the shared helpers; reduced motion is applied inside those helpers.
 */
object MapMotion {
    /** Floating bars and cards rise this far on `enter` (the shared 12 dp bar rise). */
    val BAR_RISE: Dp = 12.dp

    /** The step between two things that appear one after the other (shared 60 ms stagger). */
    const val STAGGER_MS: Int = CivilightMotion.searchBoxStaggerMs

    /** The pause between two stops while `인물 동선` plays: 900 ms, no token yet (see Requests). */
    const val PLAY_STEP_MS: Int = 900

    /** A stamp drops from this far above (CNV-37 `도장`, 14 dp) and settles. */
    val STAMP_DROP: Dp = 14.dp

    /** The alpha of a dimmed shape (a region, line, route or stamp not yet revealed). */
    const val SHAPE_DIM: Float = 0.4f

    /** Delay of the [index]-th of several things that appear one after the other. */
    fun stagger(index: Int): Int = index.coerceAtLeast(0) * STAGGER_MS
}

/** Bar swap: the new bar rises on `enter`, the old fades on `exit`; the size follows on `standard`. Reduced: fades. */
fun Motion.barSwap(): ContentTransform {
    val base = swap(clip = false)
    return ContentTransform(
        targetContentEnter = enter(CivilightMotion.enter, rise = MapMotion.BAR_RISE),
        initialContentExit = exit(CivilightMotion.exit),
        sizeTransform = base.sizeTransform,
    )
}

/**
 * What a map screen's floating bar shows: [kind] names the bar (the `AnimatedContent` key, so a change of
 * [ui] inside one kind updates in place) and [ui] is the plain data it draws. The outgoing bar keeps the
 * [ui] it had, so it fades out with its own last content.
 */
@Immutable
data class MapBarSlot<out T : Any>(val kind: String, val ui: T)

/**
 * The floating bar of a map screen (`핀 옮기기`, the place sheet, the tool bars, drafts), swapped with
 * [barSwap]. `null` shows nothing and takes no room. Keep gaps as padding inside [content], not as column
 * spacing, so an empty slot leaves no gap.
 */
@Composable
fun <T : Any> MapBar(
    slot: MapBarSlot<T>?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomCenter,
    content: @Composable (kind: String, ui: T) -> Unit,
) {
    val motion = CivilightTheme.motion
    AnimatedContent(
        targetState = slot,
        modifier = modifier,
        transitionSpec = { motion.barSwap() },
        contentAlignment = alignment,
        contentKey = { it?.kind },
        label = "mapBar",
    ) { s -> if (s != null) content(s.kind, s.ui) }
}

/**
 * Cover ↔ inner layer of a map screen (CNV-34): the overlay layer crossfades on `foldOpen` when it unfolds and
 * `foldClose` when it folds. The map itself stays put; only the floating parts are inside.
 */
@Composable
fun FoldSwitch(inner: Boolean, modifier: Modifier = Modifier.fillMaxSize(), content: @Composable BoxScope.(Boolean) -> Unit) {
    val motion = CivilightTheme.motion
    AnimatedContent(
        targetState = inner,
        modifier = modifier,
        transitionSpec = { motion.foldSwap(opening = targetState) },
        label = "mapFold",
    ) { i -> Box(Modifier.fillMaxSize()) { content(i) } }
}

/** The fold crossfade: `foldOpen` in when unfolding, `foldClose` in when folding (the old layer fades on `foldClose` / `exit`). */
fun Motion.foldSwap(opening: Boolean): ContentTransform {
    val into = if (opening) CivilightMotion.foldOpen else CivilightMotion.foldClose
    return ContentTransform(
        targetContentEnter = enter(into),
        initialContentExit = exit(CivilightMotion.foldClose),
        sizeTransform = null,
    )
}

/**
 * The alpha of a mark: [dimTo] when [dim], else 1; it changes on `standard` (120 ms when reduced), and a mark that is
 * [fresh] when it first composes fades in from 0 on `standard` (a region closing fills over 200 ms). Read the state in a `graphicsLayer` or draw lambda.
 */
@Composable
fun markAlpha(dim: Boolean, dimTo: Float, fresh: Boolean): State<Float> {
    val born = remember { fresh }
    var shown by remember { mutableStateOf(!born) }
    LaunchedEffect(Unit) { shown = true }
    return animatedAlpha(if (!shown) 0f else if (dim) dimTo else 1f, CivilightMotion.standard, label = "markAlpha")
}

/**
 * 0 → 1 over [token] when a mark is [fresh] as it first composes (a route that draws itself, a stamp that drops);
 * a mark that was already there starts at 1. Full motion only moves it (a snap when reduced: the alpha carries the change).
 */
@Composable
fun markProgress(fresh: Boolean, token: MotionToken): State<Float> {
    val born = remember { fresh }
    var shown by remember { mutableStateOf(!born) }
    LaunchedEffect(Unit) { shown = true }
    return animateFloatAsState(if (shown) 1f else 0f, CivilightTheme.motion.move(token), label = "markProgress")
}

/**
 * Appear once: fade in (and rise by [rise]) on [token] (`enter`) when the element first composes, after [delayMs]. Reduced motion:
 * a 120 ms fade, no rise. Use it for marks and rows that are new on a screen, not for everything on first open.
 */
@Composable
fun Modifier.appear(rise: Dp = 0.dp, delayMs: Int = 0, token: MotionToken = CivilightMotion.enter): Modifier {
    val motion = CivilightTheme.motion
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, motion.spec(token, delayMs), label = "appearAlpha")
    val lift by animateDpAsState(if (shown) 0.dp else rise, motion.move(token, delayMs), label = "appearLift")
    return graphicsLayer {
        this.alpha = alpha
        translationY = lift.toPx()
    }
}

/**
 * A mark that becomes visible when the lens moves (시점별 공개): it starts blurred and dim and clears on the `unhide`
 * motion (P5), [index] × the shared stagger after the first of its batch. A mark that is not [fresh] is clear from the start.
 * Reduced motion: an alpha fade only (inside the DS helper).
 */
@Composable
fun Modifier.unhideIn(fresh: Boolean, index: Int): Modifier {
    val motion = CivilightTheme.motion
    val born = remember { fresh }
    var revealed by remember { mutableStateOf(!born) }
    LaunchedEffect(Unit) {
        if (born) {
            delay(motion.delayMs(MapMotion.stagger(index)))
            revealed = true
        }
    }
    return unhide(rememberUnhide(revealed))
}
