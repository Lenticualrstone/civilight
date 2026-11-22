package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ImageCard
import io.github.lenticualrstone.civilight.designsystem.component.PagerMarks
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.HapticToken
import io.github.lenticualrstone.civilight.designsystem.theme.rememberHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge

/** Lets a horizontal row run under the column's side padding to the screen edges (boards bleed the row). */
private fun Modifier.bleed(side: Dp): Modifier = layout { measurable, constraints ->
    val extra = side.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

/**
 * The card view's project block (LIB-04; boards `LibCards`, `LibCardsWide`): `프로젝트 · N · 숨김 M`
 * with the pager marks, then one snapping row of 3:4 project cards, the lock card while the hidden
 * group is locked, and on the wide row the outlined `새 프로젝트` card.
 */
@Composable
internal fun ProjectCardsBlock(
    ui: ShelfUi,
    wide: Boolean,
    side: Dp,
    onOpen: (Key) -> Unit,
    onMenu: (Key) -> Unit,
    onUnlock: () -> Unit,
    onNewProject: () -> Unit,
) {
    val w = if (wide) ShelfCards.WIDE_CARD_W.dp else ShelfCards.COVER_CARD_W.dp
    val h = if (wide) ShelfCards.WIDE_CARD_H.dp else ShelfCards.COVER_CARD_H.dp
    val gap = if (wide) ShelfCards.WIDE_GAP.dp else ShelfCards.COVER_GAP.dp
    val state = rememberLazyListState()
    val pages = ShelfCards.pageCount(ui, wide)
    val current by remember(state) { derivedStateOf { state.firstVisibleItemIndex } }
    val density = LocalDensity.current
    val targetPx = with(density) { w.roundToPx() }
    val stridePx = with(density) { (w + gap).roundToPx() }
    val reduced = CivilightTheme.reducedMotion
    val haptics = rememberHaptics()
    // `MCarousel`: a tick each time the row settles on another card (haptics stay with reduced motion).
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to state.firstVisibleItemIndex }
            .filter { (scrolling, _) -> !scrolling }
            .map { (_, index) -> index }
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics.perform(HapticToken.TICK) }
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(ui.cardSectionLabel, Modifier.weight(1f))
            if (pages > 1) PagerMarks(pages, current.coerceAtMost(pages - 1))
        }
        LazyRow(
            state = state,
            modifier = Modifier.bleed(side).padding(top = 10.dp),
            contentPadding = PaddingValues(horizontal = side),
            horizontalArrangement = Arrangement.spacedBy(gap),
            flingBehavior = rememberSnapFlingBehavior(state),
        ) {
            items(ui.cards, key = { it.row.key }) { card ->
                if (card.hiddenOpen) ProtectWindow(WindowProtection.forProject(card.row.protections))
                ImageCard(
                    title = card.row.title,
                    meta = card.row.meta,
                    rating = ThemeBridge.rating(card.row.rating),
                    statusChip = card.chip,
                    more = IconAction(ShelfCopy.TODO_COPY_LIB02_MORE_LABEL) { onMenu(card.row.key) },
                    onClick = { onOpen(card.row.key) },
                    modifier = Modifier.size(w, h),
                ) {
                    // `MCarousel` image parallax 8 % (none with reduced motion).
                    Box(
                        Modifier.matchParentSize().graphicsLayer {
                            if (!reduced) {
                                // Item offsets start after the content padding, where a snapped card rests (0).
                                val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == card.row.key }
                                val shift = info?.let { ShelfCards.parallaxShift(it.offset, 0, stridePx, targetPx) } ?: 0f
                                val scale = ShelfCards.parallaxScale(shift, targetPx)
                                translationX = shift
                                scaleX = scale
                                scaleY = scale
                            }
                        },
                    ) { CoverArt(card.cover, targetPx) }
                }
            }
            ui.lockedGroup?.let { group -> item(key = "lockCard") { LockCard(group, onUnlock, Modifier.size(w, h)) } }
            if (wide) item(key = "newCard") { NewProjectCard(onNewProject, Modifier.size(w, h)) }
        }
    }
}

/** The hidden group as one card (LIB-04): lock icon, `숨긴 프로젝트 <M>`, `이름 · 표지 가림`, `지문으로 보기`. */
@Composable
private fun LockCard(group: HiddenGroupRow, onUnlock: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.clip(shape).background(colors.tint).insetOutline(colors.edge, 1.dp, shape)) {
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            CivilightIcon(IconId.Lock, contentDescription = null, tint = colors.ink2)
            CivilightText(group.title, type.title.merge(TextStyle(fontSize = 20.sp, lineHeight = 26.sp)), Modifier.padding(top = 10.dp), color = colors.ink, maxLines = 1)
            CivilightText(group.cardSub, type.small, color = colors.ink3, maxLines = 1)
            CivilightButton(group.button, onUnlock, Modifier.padding(top = 14.dp), variant = ButtonVariant.COMPACT_PRIMARY)
        }
    }
}

/** `새 프로젝트` (wide row only): outlined 1.5 `edge`, `+` and the label, centred. */
@Composable
private fun NewProjectCard(onClick: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .insetOutline(colors.edge, 1.5.dp, shape)
            .pressable(onClick = onClick, role = Role.Button, shape = shape),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, tint = colors.ink2)
        CivilightText(ShelfCopy.NEW_PROJECT, type.label, color = colors.ink2)
    }
}

/**
 * `최근 연 파일` (LIB-05): small cards (image, `<kind> · <project>`, title, time), newest first; 3 on
 * the cover row, 4 on the wide one. A card that names content of a capture-blocked project protects
 * the window; a masked `R-18 프로젝트` card does not (S-12).
 */
@Composable
internal fun RecentsBlock(recents: List<RecentCardUi>, wide: Boolean, side: Dp, onOpen: (RecentItem) -> Unit) {
    val shown = recents.take(ShelfCards.recentLimit(wide))
    if (shown.isEmpty()) return
    val w = if (wide) ShelfCards.WIDE_RECENT_W.dp else ShelfCards.COVER_RECENT_W.dp
    val h = if (wide) ShelfCards.WIDE_RECENT_H.dp else ShelfCards.COVER_RECENT_H.dp
    val gap = if (wide) ShelfCards.WIDE_GAP.dp else ShelfCards.COVER_GAP.dp
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val targetPx = with(LocalDensity.current) { w.roundToPx() }
    Column(Modifier.fillMaxWidth().padding(top = if (wide) 26.dp else 24.dp)) {
        SectionLabel(ShelfCopy.RECENTS)
        LazyRow(
            modifier = Modifier.bleed(side).padding(top = 10.dp),
            contentPadding = PaddingValues(horizontal = side),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(shown, key = { it.item.key }) { card ->
                if (!card.masked) ProtectWindow(WindowProtection.forProject(card.protections))
                val shape = RoundedCornerShape(14.dp)
                Column(Modifier.width(w).pressable(onClick = { onOpen(card.item) }, role = Role.Button, scaleOnPress = false, onClickLabel = card.title ?: card.kindLine)) {
                    Box(Modifier.size(w, h).clip(shape).background(colors.chip)) {
                        card.imageHash?.let { BlobImage(it, crop = null, targetWidth = targetPx) }
                    }
                    CivilightText(card.kindLine, type.small, Modifier.padding(top = 8.dp), color = colors.ink3, maxLines = 1)
                    card.title?.let {
                        CivilightText(it, type.itemTitleCompact.merge(TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)), color = colors.ink, maxLines = 1)
                    }
                    CivilightText(card.time, type.small, color = colors.ink3, maxLines = 1)
                }
            }
        }
    }
}
