package io.github.lenticualrstone.civilight.feature.canvas.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.CardSize
import io.github.lenticualrstone.civilight.designsystem.card.SnapCardRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.library.hiding.LockedItemsRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCard
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCardCopy
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCardRules

/** Everything the three bodies draw from (built once per frame by the screen). */
class GalleryBodyState(
    val view: GalleryView,
    val viewKind: GalleryViewKind,
    val layout: GalleryLayout,
    val pick: PickState,
    val panel: PanelModel?,
    val inner: Boolean,
    val handlers: GalleryHandlers,
    /** The shared-element modifier of an item's image (card → `보기`, GAL-24); identity when not available. */
    val sharedImage: @Composable (Key) -> Modifier,
    /** An entry's description spans with `@` links masked (the panel builder's rule); null = `설명 없음`. */
    val captionOf: (GalleryEntry) -> List<CaptionSpan>?,
    /** `여러 장 고르기` (GAL-18): while active, a tap toggles the checkbox instead of picking. */
    val multi: MultiPick = MultiPick.OFF,
) {
    val place: PanelPlace get() = GalleryPlacement.panel(viewKind, layout)
    val hint: HintPlace get() = GalleryPlacement.hint(viewKind, layout, pick.pickedKey != null, view.grouped, view.isEmpty || view.entries.isEmpty())
    val side get() = if (inner) 32.dp else 20.dp
}

/** One slot of a card row: an item card, or the locked card at the end (GAL-19). */
private sealed interface CardSlot {
    val key: Any

    data class Item(val entry: GalleryEntry) : CardSlot {
        override val key: Any get() = entry.key
    }

    data class Locked(val row: LockedItemsRow) : CardSlot {
        override val key: Any get() = "locked"
    }
}

// ---------------------------------------------------------------- 카드 (GAL-07, GAL-08)

/**
 * The card rows (GAL-07): one snapping row per section, the 설명 판 right under the row that holds the pick with
 * its arrow on the card (GAL-08), or the hint while nothing is picked. Scrolling never picks.
 */
fun LazyListScope.cardRows(
    s: GalleryBodyState,
    rowStates: (String) -> LazyListState,
    fit: (CardSize) -> CardSize = { it },
    onScrollToPick: (String, Int) -> Unit,
) {
    s.view.sections.forEach { section ->
        section.header?.let { header -> item(key = "head-${section.id}") { GroupHeader(s, section, header, rowStates(section.id), civilightItem()) } }
        item(key = "cards-${section.id}") {
            CardRowSection(s, section, rowStates(section.id), onScrollToPick, civilightItem(), fit)
        }
    }
    // Grouped rows (GAL-10): hidden images of visible owners collapse into one row after the groups.
    if (s.view.grouped) s.view.lockedRow?.let { row -> lockRowItem(s, row) }
    s.view.hiddenCharacters?.let { row -> lockRowItem(s, row, key = "lockedCharacters") }
}

@Composable
private fun CardRowSection(
    s: GalleryBodyState,
    section: GallerySection,
    rowState: LazyListState,
    onScrollToPick: (String, Int) -> Unit,
    modifier: Modifier,
    fit: (CardSize) -> CardSize = { it },
) {
    val density = LocalDensity.current
    val slots = remember(section, s.view.lockedRow, s.view.grouped) {
        section.entries.map<GalleryEntry, CardSlot> { CardSlot.Item(it) } +
            listOfNotNull(s.view.lockedRow?.takeIf { !s.view.grouped }?.let { CardSlot.Locked(it) })
    }
    val pickedKey = s.pick.pickedKey
    val pickedIndex = section.entries.indexOfFirst { it.key == pickedKey }.takeIf { it >= 0 }
    // The reference size, shrunk to stand whole between the bars with the next card peeking (CardFit).
    val cardSize = fit(GalleryCardSizes.of(section, s.view.grouped, s.inner))
    val cardW = cardSize.width
    val inset = CardMetrics.inset(s.inner)
    Column(modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        SnapCardRow(
            items = slots,
            key = { it.key },
            kind = CardKind.PROJECT,
            inner = s.inner,
            showPager = false,
            state = rowState,
            // Room for the picked card's 10 dp lift and ring.
            modifier = Modifier.padding(top = 14.dp),
        ) { slot, imageModifier ->
            when (slot) {
                is CardSlot.Item -> {
                    val e = slot.entry
                    GalleryCard(
                        title = e.title,
                        meta = e.meta,
                        chip = chipWithHidden(e),
                        inner = s.inner,
                        onClick = { s.handlers.tap(e.key) },
                        // A leading size wins over the card's own: grouped rows draw smaller cards (GAL-10).
                        modifier = Modifier.size(cardSize.width.dp, cardSize.height.dp),
                        onMenu = s.handlers.menu?.takeIf { !s.multi.active }?.let { m -> { m(e.key) } },
                        picked = e.key == pickedKey,
                    ) {
                        Box(Modifier.matchParentSize().then(s.sharedImage(e.key))) {
                            GalleryImage(e.item, Ratio.R3X4, GalleryImageSizes.CARD, imageModifier)
                        }
                        MultiCheck(s, e.key, Modifier.align(Alignment.TopEnd).padding(10.dp))
                    }
                }
                is CardSlot.Locked -> LockedTile(slot.row, s.handlers.unlock, Modifier.size(cardSize.width.dp, cardSize.height.dp))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = inset.dp)) {
            val panelW = maxWidth.value
            val cardStart by remember(rowState, pickedIndex, density) {
                derivedStateOf {
                    val info = rowState.layoutInfo
                    val item = pickedIndex?.let { i -> info.visibleItemsInfo.firstOrNull { it.index == i } }
                    item?.let { with(density) { (it.offset - info.viewportStartOffset).toDp().value } - inset }
                }
            }
            val arrow = CardRowRules.arrowX(cardStart, cardW, panelW)
            val away = pickedIndex != null && arrow == null
            val shown = s.panel?.takeIf { pickedIndex != null && s.place == PanelPlace.UNDER_ROW }
            Column {
                FoldRevealValue(shown, label = "cardPanel") { panel ->
                    Column {
                        PanelArrow(arrow)
                        GalleryPanelCard(
                            panel = panel,
                            clampLines = PanelModel.clampLines(GalleryViewKind.CARD),
                            expanded = s.pick.expanded,
                            away = away,
                            twoColumns = s.layout == GalleryLayout.INNER_CARDS,
                            handlers = GalleryHandlers(
                                tap = s.handlers.tap, view = s.handlers.view, toggleMore = s.handlers.toggleMore,
                                openScene = s.handlers.openScene, unlock = s.handlers.unlock, openOwner = s.handlers.openOwner,
                                backToPick = { pickedIndex?.let { onScrollToPick(section.id, it) } },
                                menu = s.handlers.menu, edit = s.handlers.edit,
                            ),
                        )
                    }
                }
                FoldReveal(s.hint == HintPlace.UNDER_ROW && !s.view.grouped, label = "cardHint") { GalleryHint(GalleryUiCopy.HINT_CARD) }
            }
        }
    }
}

/** `<owner> · <N>` with the pager or readout of its row, and `<name> 갤러리 ›` for a character group (GAL-10). */
@Composable
private fun GroupHeader(s: GalleryBodyState, section: GallerySection, header: String, rowState: LazyListState?, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier.fillMaxWidth().padding(horizontal = s.side).padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightText(header, type.sec, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
        if (s.viewKind == GalleryViewKind.CARD && rowState != null) RowPosition(section.entries.size, rowState)
        section.ownerLink?.let { key -> TextAction(GalleryUiCopy.ownerLink(section.ownerName.orEmpty()), { s.handlers.openOwner(key) }, color = colors.acc) }
    }
}

/** Pager marks (12 or fewer cards) or the `41 · 128` readout; they show the scroll position, not the pick. */
@Composable
fun RowPosition(n: Int, rowState: LazyListState, modifier: Modifier = Modifier) {
    val first by remember(rowState) { derivedStateOf { rowState.firstVisibleItemIndex } }
    if (n <= 1) return
    if (n > GalleryRows.MAX_PAGER_MARKS) {
        CivilightText(CardRowRules.readout(first, n), CivilightTheme.type.number, modifier, color = CivilightTheme.colors.ink3, maxLines = 1)
    } else {
        io.github.lenticualrstone.civilight.designsystem.component.PagerMarks(n, first.coerceIn(0, n - 1), modifier)
    }
}

// ---------------------------------------------------------------- 격자 (GAL-06)

/** Three columns of 3:4 tiles; the panel opens right under the row that holds the pick; the lock tile ends the grid. */
fun LazyListScope.gridRows(s: GalleryBodyState) {
    s.view.sections.forEach { section ->
        section.header?.let { header -> item(key = "head-${section.id}") { GroupHeader(s, section, header, null, civilightItem()) } }
        val lines = GalleryGrid.lines(
            section.entries,
            s.pick.pickedKey?.takeIf { k -> section.entries.any { it.key == k } },
            lockTile = s.view.lockedRow != null && !s.view.grouped,
            sectionId = section.id,
            panel = s.place == PanelPlace.UNDER_ROW,
        )
        items(lines, key = { "${section.id}-${it.key}" }) { line ->
            when (line) {
                is GridLine.Tiles -> Row(
                    civilightItem().fillMaxWidth().padding(horizontal = s.side).padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    line.entries.forEach { e -> GalleryTile(s, e, Modifier.weight(1f)) }
                    if (line.lockTile) s.view.lockedRow?.let { LockedTile(it, s.handlers.unlock, Modifier.weight(1f)) }
                    repeat(GalleryGrid.COLUMNS - line.entries.size - (if (line.lockTile) 1 else 0)) { Spacer(Modifier.weight(1f)) }
                }
                is GridLine.Panel -> s.panel?.let { panel ->
                    GalleryPanelCard(
                        panel, PanelModel.clampLines(GalleryViewKind.GRID), s.pick.expanded, away = false, twoColumns = false, handlers = s.handlers,
                        modifier = civilightItem(fadeIn = CivilightMotion.enter).padding(horizontal = s.side).padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
    if (s.view.grouped) s.view.lockedRow?.let { row -> lockRowItem(s, row) }
    s.view.hiddenCharacters?.let { row -> lockRowItem(s, row, key = "lockedCharacters") }
}

/** A grid tile (the picker's `ImageTile` look, radius 14, no text): selection ring without the check badge. */
@Composable
private fun GalleryTile(s: GalleryBodyState, e: GalleryEntry, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val picked = s.pick.pickedKey == e.key
    val ring = animatedFraction(picked, label = "tileRing")
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .aspectRatio(3f / 4f)
            .semantics { selected = picked; contentDescription = e.title ?: GalleryCardCopy.UNTITLED }
            .drawWithContent {
                drawContent()
                val a = ring.value
                if (a > 0f) {
                    val g = 2.dp.toPx()
                    val r = 14.dp.toPx()
                    drawRoundRect(colors.bg, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r), style = Stroke(g), alpha = a)
                    drawRoundRect(colors.acc, Offset(-g / 2, -g / 2), Size(size.width + g, size.height + g), CornerRadius(r + g), style = Stroke(g), alpha = a)
                }
            }
            .pressable(PressAction.Select(picked) { s.handlers.tap(e.key) }, role = Role.Button, shape = shape)
            .clip(shape),
    ) {
        Box(Modifier.matchParentSize().then(s.sharedImage(e.key))) { GalleryImage(e.item, Ratio.R3X4, GalleryImageSizes.TILE) }
        chipWithHidden(e)?.let { ImageStatusChip(it, Modifier.align(Alignment.TopStart).padding(6.dp)) }
        MultiCheck(s, e.key, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

/** The only custom lock variant (GAL-06): `tint`, `edge` outline, lock icon, `숨긴 이미지 <H>`, small `지문으로 보기`; the whole tile is the button. */
@Composable
private fun LockedTile(row: LockedItemsRow, onUnlock: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .aspectRatio(3f / 4f)
            .clip(shape)
            .background(colors.tint)
            .insetOutline(colors.edge, 1.dp, shape)
            .pressable(PressAction.Click(onUnlock), role = Role.Button, shape = shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CivilightIcon(IconId.Lock, null, size = 20.dp, tint = colors.ink2)
        CivilightText(row.title, CivilightTheme.type.label, color = colors.ink, maxLines = 2)
        CivilightText(row.action, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
    }
}

/** The checkbox of `여러 장 고르기` (GAL-18) on a card, tile or row; nothing outside the mode. */
@Composable
private fun MultiCheck(s: GalleryBodyState, key: Key, modifier: Modifier) {
    FoldReveal(s.multi.active, modifier) {
        io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark(key in s.multi.keys, onCheckedChange = { s.handlers.tap(key) })
    }
}

// ---------------------------------------------------------------- 목록 (GAL-05)

/** One row per item; the picked row expands in place (cover) or only highlights (inner list + detail). */
fun LazyListScope.listRows(s: GalleryBodyState) {
    s.view.sections.forEach { section ->
        section.header?.let { header -> item(key = "head-${section.id}") { GroupHeader(s, section, header, null, civilightItem()) } }
        items(section.entries, key = { it.key }) { e -> GalleryListRow(s, e, civilightItem()) }
    }
    s.view.lockedRow?.let { row -> lockRowItem(s, row) }
    s.view.hiddenCharacters?.let { row -> lockRowItem(s, row, key = "lockedCharacters") }
}

@Composable
private fun GalleryListRow(s: GalleryBodyState, e: GalleryEntry, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val picked = s.pick.pickedKey == e.key
    val fill by animatedColor(if (picked) colors.sel else colors.bg, label = "rowFill")
    val bar = animatedFraction(picked, label = "rowBar")
    val panel = s.panel?.takeIf { picked && s.place == PanelPlace.ROW_EXPANDS }
    Column(
        modifier
            .fillMaxWidth()
            // A row is never shorter than its thumbnail (12 + 64 + 12), also while it opens.
            .heightIn(min = LIST_ROW_MIN)
            .drawBehind {
                drawRect(fill)
                if (bar.value > 0f) drawRect(colors.acc, size = Size(2.dp.toPx(), size.height), alpha = bar.value)
                drawRect(colors.line, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                // Closed, the whole thumbnail height is the tap target; open, the row ends under the title.
                .heightIn(min = if (panel == null) LIST_ROW_MIN else 0.dp)
                .semantics { selected = picked }
                .pressable(PressAction.Select(picked) { s.handlers.tap(e.key) }, role = Role.Button)
                .padding(start = s.side, end = s.side - 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // The thumbnail takes no height in the row, so an opened description starts right under the title
            // instead of under the thumbnail (it hangs into the opened part, beside the description).
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, 0) { placeable.place(0, 0) }
                    }
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(s.sharedImage(e.key)),
            ) {
                GalleryImage(e.item, Ratio.R1X1, GalleryImageSizes.THUMB)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CivilightText(e.title ?: GalleryUiCopy.UNTITLED, type.itemTitle, Modifier.weight(1f, fill = false), color = if (e.title == null) colors.ink3 else colors.ink, maxLines = 1)
                    if (e.hiddenTag) HiddenTag()
                }
                if (panel == null) {
                    val caption = remember(e.item) { s.captionOf(e) }
                    if (caption == null) CivilightText(GalleryUiCopy.NO_DESCRIPTION, type.small, color = colors.ink3, maxLines = 2)
                    else androidx.compose.foundation.text.BasicText(
                        captionText(caption),
                        style = type.small.merge(androidx.compose.ui.text.TextStyle(color = colors.ink2)),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    e.meta?.let { CivilightText(it, type.number, color = colors.ink3, maxLines = 1) }
                }
            }
            if (s.multi.active) MultiCheck(s, e.key, Modifier.padding(top = 10.dp, end = 8.dp))
            else s.handlers.menu?.let { m -> CivilightIconButton(IconId.More, GalleryUiCopy.MENU, { m(e.key) }) }
        }
        FoldRevealValue(panel, label = "rowExpand") { p -> ExpandedRow(s, p) }
    }
}

/** The expanded row (GAL-05): full description, meta with scene link and tags, position, `보기` · `고치기` · `⋯`. */
@Composable
private fun ExpandedRow(s: GalleryBodyState, p: PanelModel) {
    val colors = CivilightTheme.colors
    Column(Modifier.fillMaxWidth().padding(start = s.side + 76.dp, end = s.side, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val caption = p.caption
        if (caption == null) CivilightText(GalleryUiCopy.NO_DESCRIPTION, CivilightTheme.type.body, color = colors.ink3)
        else androidx.compose.foundation.text.BasicText(captionText(caption), style = CivilightTheme.type.body.merge(androidx.compose.ui.text.TextStyle(color = colors.ink2)))
        PanelMeta(p, away = true, handlers = s.handlers)
        PanelButtons(p, s.handlers, vertical = false)
    }
}

// ---------------------------------------------------------------- inner list + detail (GAL-23)

/**
 * The right pane of inner `목록 + 상세` (GAL-23): `고른 이미지 · <i>/<N>`, title, `⋯`, the card crop at 261 × 348
 * with holo and chip, meta, full description, `보기` · `고치기`. Nothing picked: the first item dimmed with
 * `이미지 누르기 · 설명 보기` (§6 Q8).
 */
@Composable
fun GalleryDetailPane(s: GalleryBodyState, padding: PaddingValues) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val picked = s.panel?.let { p -> s.view.entry(p.key)?.let { it to p } }
    ContentSwap(picked, Modifier.fillMaxSize(), contentKey = { it?.first?.key }, label = "galleryDetail") { shown ->
      BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardH = GalleryDetailSizes.cardHeight(maxHeight.value, padding.calculateTopPadding().value, padding.calculateBottomPadding().value)
        val card = Modifier.size(GalleryDetailSizes.cardWidth(cardH).dp, cardH.dp)
        // The button row stays outside the scroll, right above the bottom bar: when the pane is too short for the
        // rows (the card stops at its least height), the rows above scroll and `설명 쓰기 · 보기` stay in reach.
        // On a tall pane the rows take their own height (`fill = false`), so the buttons follow the description.
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding() + GalleryDetailSizes.BOTTOM_GAP.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding() + GalleryDetailSizes.TOP_GAP.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (shown == null) {
                    val first = s.view.entries.firstOrNull()
                    if (first != null) {
                        Box(card.graphicsLayer { alpha = DIMMED }.clip(RoundedCornerShape(20.dp)).align(Alignment.CenterHorizontally)) {
                            GalleryImage(first.item, Ratio.R3X4, GalleryImageSizes.CARD)
                        }
                    }
                    GalleryHint(GalleryUiCopy.HINT_INNER)
                } else {
                    val (entry, p) = shown
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CivilightText(GalleryUiCopy.PICKED_IMAGE + GalleryUiCopy.SEP + p.position, type.number, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
                        s.handlers.menu?.let { m -> CivilightIconButton(IconId.More, GalleryUiCopy.MENU, { m(p.key) }) }
                    }
                    CivilightText(p.title, type.headerTitleInner, color = if (p.untitled) colors.ink3 else colors.ink)
                    Box(card.clip(RoundedCornerShape(20.dp)).then(s.sharedImage(p.key))) {
                        GalleryImage(entry.item, Ratio.R3X4, GalleryImageSizes.CARD)
                        chipWithHidden(entry)?.let { ImageStatusChip(it, Modifier.align(Alignment.TopStart).padding(12.dp)) }
                    }
                    PanelMeta(p, away = false, handlers = s.handlers)
                    val caption = p.caption
                    if (caption == null) CivilightText(GalleryUiCopy.NO_DESCRIPTION, type.body, color = colors.ink3)
                    else androidx.compose.foundation.text.BasicText(captionText(caption), style = type.body.merge(androidx.compose.ui.text.TextStyle(color = colors.ink2)))
                }
            }
            shown?.let { (_, p) -> PanelButtons(p, s.handlers, vertical = false) }
        }
      }
    }
}

private const val DIMMED = 0.45f

/** A list row's least height: the 64 dp thumbnail with 12 above and below (GAL-05). */
private val LIST_ROW_MIN = 88.dp

// ---------------------------------------------------------------- shared bits

private fun LazyListScope.lockRowItem(s: GalleryBodyState, row: LockedItemsRow, key: String = "locked") {
    item(key = key) {
        LockRow(
            title = row.title,
            subtitle = row.sub,
            actionLabel = row.action,
            onAction = s.handlers.unlock,
            modifier = civilightItem().fillMaxWidth().padding(horizontal = s.side, vertical = 12.dp),
        )
    }
}

/** The tile / card chip with `숨김` first for an unlocked hidden item (GAL-19). */
private fun chipWithHidden(e: GalleryEntry): String? =
    listOfNotNull(GalleryUiCopy.HIDDEN_TAG.takeIf { e.hiddenTag }, e.chip).joinToString(GalleryUiCopy.SEP).ifEmpty { null }

@Composable
private fun HiddenTag() {
    val colors = CivilightTheme.colors
    CivilightText(
        GalleryUiCopy.HIDDEN_TAG,
        CivilightTheme.type.small,
        Modifier.clip(RoundedCornerShape(8.dp)).background(colors.tint).padding(horizontal = 6.dp, vertical = 1.dp),
        color = colors.ink2,
        maxLines = 1,
    )
}

/** Small `ink3` line (empty and filtered states). */
@Composable
fun SmallLine(text: String, modifier: Modifier = Modifier) {
    CivilightText(text, CivilightTheme.type.small, modifier, color = CivilightTheme.colors.ink3)
}

/** A primary button of the empty states. */
@Composable
internal fun GalleryPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CivilightButton(text, onClick, modifier.heightIn(min = 44.dp), variant = ButtonVariant.PRIMARY)
}
