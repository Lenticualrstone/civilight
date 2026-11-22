package io.github.lenticualrstone.civilight.feature.canvas.card

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.PagerMarks
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics

/**
 * A horizontal card row (CNV-22; boards `LibCards`, `CastCards`, `MCarousel`): starts at the content
 * inset and runs past the right edge so the next card peeks out; a fling ends with the nearest card's
 * left edge at the row start, with one haptic tick per snap. [card] receives the image shift in dp
 * relative to the card (parallax 8 %, none with reduced motion). [title] and the pager marks sit above.
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
    card: @Composable (item: T, imageShiftDp: Float) -> Unit,
) {
    val reduced = CivilightTheme.reducedMotion
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val ticker = remember { SnapTicker() }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (ticker.onScrolling(scrolling)) haptics.perform(HapticToken.TICK)
        }
    }
    val current by remember(state) { derivedStateOf { state.firstVisibleItemIndex } }
    // The nearest card's left edge settles at the row start ([CardSnap]) with the 320 ms decelerate curve;
    // reduced motion keeps the snap (CNV-45: snap only, no parallax).
    val decay = rememberSplineBasedDecay<Float>()
    val fling = remember(state, decay) {
        snapFlingBehavior(
            snapLayoutInfoProvider = SnapLayoutInfoProvider(state, SnapPosition.Start),
            decayAnimationSpec = decay,
            snapAnimationSpec = CivilightMotion.spec(CardSnap.MOTION, reduced = false),
        )
    }
    val inset = CardMetrics.inset(inner)
    Column(modifier.fillMaxWidth()) {
        if (title != null || (showPager && items.size > 1)) {
            Row(Modifier.fillMaxWidth().padding(start = inset.dp, end = inset.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CivilightText(title.orEmpty(), CivilightTheme.type.sec, Modifier.weight(1f), color = CivilightTheme.colors.ink3, maxLines = 1)
                if (showPager && items.size > 1) PagerMarks(items.size, current.coerceIn(0, items.lastIndex))
            }
        }
        LazyRow(
            state = state,
            flingBehavior = fling,
            horizontalArrangement = Arrangement.spacedBy(CardMetrics.gap(inner).dp),
            contentPadding = PaddingValues(start = inset.dp, end = inset.dp),
        ) {
            itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
                val shift = if (reduced) 0f else {
                    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                    val offsetDp = info?.let { with(density) { it.offset.toDp().value } } ?: 0f
                    CardParallax.imageShift(offsetDp, reduced = false)
                }
                card(item, shift)
            }
        }
    }
}
