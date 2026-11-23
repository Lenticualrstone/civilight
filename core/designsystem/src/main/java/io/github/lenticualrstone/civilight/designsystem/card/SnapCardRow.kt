package io.github.lenticualrstone.civilight.designsystem.card

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.PagerMarks
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics

/**
 * A horizontal card row (CNV-22; boards `LibCards`, `CastCards`, `MCarousel`, gallery `GalShelf`), the shared
 * copy of canvas `card/SnapCardRow.kt`: starts at the content inset and runs past the right edge so the next
 * card peeks out; a fling ends with the nearest card's left edge at the row start on the `enter` curve, with
 * one haptic tick per snap (kept when motion is reduced). Items enter, leave and move with [civilightItem].
 * The inset follows a fold or unfold on the fold tokens.
 *
 * [card] receives an `imageModifier` for the card's image layer: it applies the 8 % parallax and the constant
 * 1.1 scale inside `graphicsLayer` (no recomposition while scrolling; none when reduced). Put it on the image
 * inside the card's clip.
 */
@Composable
fun <T> SnapCardRow(
    items: List<T>,
    key: (T) -> Any,
    kind: CardKind,
    inner: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    showPager: Boolean = true,
    state: LazyListState = rememberLazyListState(),
    card: @Composable (item: T, imageModifier: Modifier) -> Unit,
) {
    val motion = LocalMotion.current
    val reduced by rememberUpdatedState(motion.reduced)
    val haptics = rememberHaptics()
    val ticker = remember { SnapTicker() }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (ticker.onScrolling(scrolling)) haptics.perform(HapticToken.TICK)
        }
    }
    val current by remember(state) { derivedStateOf { state.firstVisibleItemIndex } }
    // The settle continues the finger's fling, so it keeps the 320 ms decelerate curve even when reduced
    // (CNV-45: reduced keeps the snap, drops the parallax).
    val decay = rememberSplineBasedDecay<Float>()
    val fling = remember(state, decay) {
        snapFlingBehavior(
            snapLayoutInfoProvider = SnapLayoutInfoProvider(state, SnapPosition.Start),
            decayAnimationSpec = decay,
            snapAnimationSpec = CivilightMotion.spec(CardSnap.MOTION, reduced = false),
        )
    }
    val inset by animateDpAsState(
        CardMetrics.inset(inner).dp,
        motion.move(if (inner) CivilightMotion.foldOpen else CivilightMotion.foldClose),
        label = "cardInset",
    )
    val gap by animateDpAsState(
        CardMetrics.gap(inner).dp,
        motion.move(if (inner) CivilightMotion.foldOpen else CivilightMotion.foldClose),
        label = "cardGap",
    )
    Column(modifier.fillMaxWidth()) {
        if (title != null || (showPager && items.size > 1)) {
            Row(Modifier.fillMaxWidth().padding(start = inset, end = inset, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CivilightText(title.orEmpty(), CivilightTheme.type.sec, Modifier.weight(1f), color = CivilightTheme.colors.ink3, maxLines = 1)
                if (showPager && items.size > 1) PagerMarks(items.size, current.coerceIn(0, items.lastIndex))
            }
        }
        LazyRow(
            state = state,
            flingBehavior = fling,
            horizontalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(start = inset, end = inset),
        ) {
            itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
                val imageModifier = Modifier.cardParallax(state, index, { reduced })
                Box(civilightItem()) { card(item, imageModifier) }
            }
        }
    }
}

/**
 * The parallax of item [index] of a card row: shift `-displacement × 8 %` and scale 1.1, both read in the
 * draw phase; nothing when [reduced] returns true. Displacement is the item's offset from its resting place
 * at the row start (`LazyListItemInfo.offset` is already measured after the start content padding). The
 * shift is clamped to the scaled image's overhang ([CardParallax.maxShift] of this layer's width, less a
 * pixel), so cards further along the row never show a bare strip at their end.
 */
fun Modifier.cardParallax(state: LazyListState, index: Int, reduced: () -> Boolean): Modifier = graphicsLayer {
    val r = reduced()
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    val displacement = info?.offset?.toFloat() ?: 0f
    translationX = CardParallax.imageShift(displacement, r, imageWidth = size.width, guard = 1f)
    val s = CardParallax.imageScale(r)
    scaleX = s
    scaleY = s
}

/** [cardParallax] reading a [State] flag. */
fun Modifier.cardParallax(state: LazyListState, index: Int, reduced: State<Boolean>): Modifier =
    cardParallax(state, index) { reduced.value }
