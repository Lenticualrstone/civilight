package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.SnapCardRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ImageCard
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Unhide
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenGroupRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.feature.library.tour.tourTarget
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge

/**
 * The card view's project block (LIB-04; boards `LibCards`, `LibCardsWide`): `프로젝트 · N · 숨김 M`
 * with the pager marks, then one snapping row of 3:4 project cards, the lock card while the hidden
 * group is locked, and on the wide row the outlined `새 프로젝트` card. The row is the DS [SnapCardRow]:
 * the snap settles on `enter`, ticks once per card, and each card's picture slides 8 % (none when reduced).
 * Cards of the open hidden group come out of the blur with [unhide] (CNV-43); [covers] is the shared cover
 * of the `목록 ↔ 카드` switch.
 */
@Composable
internal fun ProjectCardsBlock(
    ui: ShelfUi,
    wide: Boolean,
    covers: SharedCovers,
    unhide: Unhide,
    onOpen: (Key) -> Unit,
    onMenu: (Key) -> Unit,
    onUnlock: () -> Unit,
    onNewProject: () -> Unit,
) {
    // LIB-19 `서재 · 숨기기`: the first project card and the hidden group's card are the tour's holes.
    val tourHide = ui.cards.firstOrNull()?.row?.key
    val tourGroup = ui.cards.firstOrNull { it.hiddenOpen }?.row?.key
    val w = if (wide) ShelfCards.WIDE_CARD_W.dp else ShelfCards.COVER_CARD_W.dp
    val h = if (wide) ShelfCards.WIDE_CARD_H.dp else ShelfCards.COVER_CARD_H.dp
    val targetPx = with(LocalDensity.current) { w.roundToPx() }
    val items = remember(ui.cards, ui.lockedGroup, wide) { ShelfCards.cardItems(ui, wide) }
    SnapCardRow(
        items = items,
        key = { it.key },
        kind = CardKind.PROJECT,
        inner = wide,
        title = ui.cardSectionLabel,
    ) { item, imageModifier ->
        when (item) {
            is ShelfCardItem.Project -> {
                val card = item.card
                if (card.hiddenOpen) ProtectWindow(WindowProtection.forProject(card.row.protections))
                else if (ShelfState.coverProtects(card.row.protections, card.cover)) ProtectWindow(COVER_PROTECTION)
                ImageCard(
                    title = card.row.title,
                    meta = card.row.meta,
                    // (0.2.0, LIB-92) No rating badge on cards.
                    rating = null,
                    statusChip = card.chip,
                    more = IconAction(ShelfCopy.MORE_LABEL) { onMenu(card.row.key) },
                    onClick = { onOpen(card.row.key) },
                    modifier = Modifier.size(w, h)
                        .then(if (card.row.key == tourHide) Modifier.tourTarget(LibraryTourSteps.HIDE) else Modifier)
                        .then(if (card.row.key == tourGroup) Modifier.tourTarget(LibraryTourSteps.HIDDEN_GROUP) else Modifier)
                        .then(if (card.hiddenOpen) Modifier.unhide(unhide) else Modifier),
                ) {
                    // The shared cover first (its bounds stay still), the parallax and scale inside it.
                    Box(Modifier.matchParentSize().then(covers.cover(card.row.key.toString(), CardMetrics.radius(CardKind.PROJECT))).then(imageModifier)) {
                        CoverArt(card.cover, targetPx)
                    }
                }
            }
            is ShelfCardItem.Lock -> LockCard(item.group, onUnlock, Modifier.size(w, h).tourTarget(LibraryTourSteps.HIDDEN_GROUP))
            ShelfCardItem.NewProject -> NewProjectCard(onNewProject, Modifier.size(w, h))
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
internal fun RecentsBlock(recents: List<RecentCardUi>, wide: Boolean, side: Dp, label: Boolean = true, onOpen: (RecentItem) -> Unit) {
    val shown = recents.take(ShelfCards.recentLimit(wide))
    if (shown.isEmpty()) return
    val w = if (wide) ShelfCards.WIDE_RECENT_W.dp else ShelfCards.COVER_RECENT_W.dp
    val h = if (wide) ShelfCards.WIDE_RECENT_H.dp else ShelfCards.COVER_RECENT_H.dp
    val gap = if (wide) ShelfCards.WIDE_GAP.dp else ShelfCards.COVER_GAP.dp
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val targetPx = with(LocalDensity.current) { w.roundToPx() }
    Column(Modifier.fillMaxWidth().padding(top = if (!label) 0.dp else if (wide) 26.dp else 24.dp)) {
        if (label) SectionLabel(ShelfCopy.RECENTS, Modifier.padding(horizontal = side))
        LazyRow(
            modifier = Modifier.padding(top = if (label) 10.dp else 0.dp),
            contentPadding = PaddingValues(horizontal = side),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(shown, key = { it.item.key }) { card ->
                if (!card.masked) ProtectWindow(WindowProtection.forProject(card.protections))
                val shape = RoundedCornerShape(14.dp)
                Column(civilightItem().width(w).pressable(onClick = { onOpen(card.item) }, role = Role.Button, scaleOnPress = false, onClickLabel = card.title ?: card.kindLine)) {
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
