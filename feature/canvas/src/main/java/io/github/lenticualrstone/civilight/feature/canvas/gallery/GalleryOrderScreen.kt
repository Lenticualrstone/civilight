package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.actions.GalleryMove
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * `순서 바꾸기` (GAL-15, boards `GalOrder`, `GalOrderMove`), an in-screen mode of the gallery (no route of its own;
 * back leaves). Every change saves at once, one undo step each. Rows are the owner's visible items in `직접 순서`:
 * thumbnail 44, title or `제목 없음`, position, `앞으로` · `뒤로`. Tapping a row picks it; the bottom bar reads
 * `고른 이미지 · <title> · <i>/<N>` with `맨 앞` · `옮기기` · `맨 뒤`. `옮기기` turns every row's buttons into one
 * `이 뒤로` (the moving row reads `옮기는 중`), with `맨 앞으로` above the first row and a bar with `취소`;
 * nothing moves until a target is tapped. Rows slide to their new place (`civilightItem`, `standard`).
 *
 * The grip and drag of the board are not built (see the status file): the buttons and `옮기기` are the complete,
 * visible way to move an item, also for 100+ items.
 */
@Composable
fun GalleryOrderScreen(
    doc: ProjectDoc,
    ownerKey: Key,
    ownerName: String,
    locked: Boolean,
    startKey: Key?,
    readOnly: Boolean,
    onMove: (Key, GalleryMove) -> Unit,
    onMoveAfter: (Key, Key?) -> Unit,
    onReadOnly: () -> Unit,
    onClose: (lastPicked: Key?) -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val rows = remember(doc, ownerKey, locked) { GalleryOrder.rows(doc, ownerKey, locked) }
    var pickedKey by rememberSaveable { mutableStateOf(startKey) }
    var movingKey by rememberSaveable { mutableStateOf<String?>(null) }
    val state = GalleryOrder.reconcile(OrderState(pickedKey, movingKey), rows)
    if (state.pickedKey != pickedKey || state.movingKey != movingKey) {
        LaunchedEffect(state) {
            pickedKey = state.pickedKey
            movingKey = state.movingKey
        }
    }
    fun set(next: OrderState) {
        pickedKey = next.pickedKey
        movingKey = next.movingKey
    }
    LocalBackHandler.current(true) { if (state.moving) set(GalleryOrder.cancel(state)) else onClose(state.pickedKey) }
    fun guarded(block: () -> Unit) = if (readOnly) onReadOnly() else block()

    val listState = rememberLazyListState()
    val n = rows.size
    val inner = LocalWindowLayout.current.isInner
    val picked = rows.firstOrNull { it.key == state.pickedKey }
    val moving = rows.firstOrNull { it.key == state.movingKey }
    // The moved row stays in view after `이 뒤로` (GAL-24 row: list scrolls to keep it in view).
    LaunchedEffect(picked?.position, state.moving) {
        val at = picked?.let { rows.indexOf(it) } ?: return@LaunchedEffect
        // Before the rows: the moving bar and the section label, then `맨 앞으로` while moving.
        val index = at + 2 + (if (state.moving) 1 else 0)
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.animateScrollToItem(index)
    }

    CivilightScreen(
        title = GalleryUiCopy.BTN_REORDER,
        smallLine = GalleryUiCopy.ownerGallery(ownerName),
        back = IconAction(GalleryUiCopy.BACK) { if (state.moving) set(GalleryOrder.cancel(state)) else onClose(state.pickedKey) },
        bottomBar = {
            FoldRevealValue(picked?.takeIf { !state.moving }, label = "orderBar") { row ->
                Column(
                    Modifier.fillMaxWidth().background(colors.raise).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CivilightText(
                        GalleryUiCopy.pickedBar(row.title ?: GalleryUiCopy.UNTITLED, GalleryUiCopy.position(row.position, n)),
                        type.small,
                        color = colors.ink2,
                        maxLines = 1,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CivilightButton(GalleryUiCopy.BTN_FIRST, { guarded { onMove(row.key, GalleryMove.FIRST) } }, Modifier.weight(1f), variant = ButtonVariant.GHOST, icon = IconButtonSpec.TO_TOP.icon, enabled = row.position > 1)
                        CivilightButton(GalleryUiCopy.BTN_MOVE, { guarded { set(GalleryOrder.startMove(state)) } }, Modifier.weight(1f), variant = ButtonVariant.PRIMARY, enabled = n > 1)
                        CivilightButton(GalleryUiCopy.BTN_LAST, { guarded { onMove(row.key, GalleryMove.LAST) } }, Modifier.weight(1f), variant = ButtonVariant.GHOST, icon = IconButtonSpec.TO_BOTTOM.icon, enabled = row.position < n)
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.fillMaxSize().then(if (inner) Modifier.widthIn(max = 560.dp) else Modifier),
                state = listState,
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                item(key = "movingBar") {
                    FoldRevealValue(moving, label = "movingBar", modifier = civilightItem()) { m ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(colors.tint).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))) { GalleryImage(m.item, Ratio.R1X1, GalleryImageSizes.THUMB) }
                            Column(Modifier.weight(1f)) {
                                CivilightText(GalleryUiCopy.movingBar(m.title ?: GalleryUiCopy.UNTITLED), type.label, color = colors.ink, maxLines = 1)
                                CivilightText(GalleryUiCopy.position(m.position, n), type.number, color = colors.ink3, maxLines = 1)
                            }
                            TextAction(GalleryUiCopy.BTN_CANCEL, { set(GalleryOrder.cancel(state)) })
                        }
                    }
                }
                item(key = "section") {
                    SectionLabel(GalleryUiCopy.orderSection(n), civilightItem().padding(horizontal = 20.dp, vertical = 8.dp))
                }
                if (state.moving) {
                    item(key = "toFirst") {
                        Row(civilightItem().fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
                            TextAction(GalleryUiCopy.TO_FIRST, {
                                val key = state.movingKey ?: return@TextAction
                                guarded { onMoveAfter(key, null) }
                                set(GalleryOrder.placed(state))
                            }, color = colors.acc)
                        }
                    }
                }
                items(rows, key = { it.key }) { row ->
                    OrderListRow(
                        row = row,
                        n = n,
                        picked = row.key == state.pickedKey,
                        state = state,
                        modifier = civilightItem(),
                        onTap = { set(GalleryOrder.tap(state, row.key)) },
                        onMove = { m -> guarded { onMove(row.key, m) } },
                        onAfter = {
                            val key = state.movingKey ?: return@OrderListRow
                            guarded { onMoveAfter(key, row.key) }
                            set(GalleryOrder.placed(state))
                        },
                    )
                }
            }
        }
    }
}

private enum class RowTrailing { BUTTONS, AFTER, MOVING }

/** One 64-high row: thumbnail 44, title, position, then `앞으로` · `뒤로` or, while moving, `이 뒤로` / `옮기는 중`. */
@Composable
private fun OrderListRow(
    row: OrderScreenRow,
    n: Int,
    picked: Boolean,
    state: OrderState,
    modifier: Modifier,
    onTap: () -> Unit,
    onMove: (GalleryMove) -> Unit,
    onAfter: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectedRowBackground(picked, colors.sel, colors.acc)
            .semantics { selected = picked }
            .pressable(PressAction.Select(picked, onTap), role = Role.Button, scaleOnPress = false)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))) { GalleryImage(row.item, Ratio.R1X1, GalleryImageSizes.THUMB) }
        CivilightText(row.title ?: GalleryUiCopy.UNTITLED, type.itemTitle, Modifier.weight(1f), color = if (row.title == null) colors.ink3 else colors.ink, maxLines = 1)
        CivilightText(row.position.toString(), type.number, color = colors.ink3, maxLines = 1)
        // `앞으로 · 뒤로` ↔ `이 뒤로` / `옮기는 중` crossfade when `옮기기` starts or ends.
        val trailing = when {
            state.movingKey == row.key -> RowTrailing.MOVING
            state.moving -> RowTrailing.AFTER
            else -> RowTrailing.BUTTONS
        }
        ContentSwap(trailing, label = "orderRowTrailing") { t ->
            when (t) {
                RowTrailing.MOVING -> CivilightText(GalleryUiCopy.MOVING, type.small, color = colors.acc, maxLines = 1)
                RowTrailing.AFTER -> CivilightButton(GalleryUiCopy.BTN_AFTER_THIS, onAfter, variant = ButtonVariant.COMPACT_GHOST)
                RowTrailing.BUTTONS -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CivilightButton(GalleryUiCopy.BTN_FORWARD, { onMove(GalleryMove.FORWARD) }, variant = ButtonVariant.COMPACT_GHOST, enabled = row.position > 1)
                    CivilightButton(GalleryUiCopy.BTN_BACKWARD, { onMove(GalleryMove.BACKWARD) }, variant = ButtonVariant.COMPACT_GHOST, enabled = row.position < n)
                }
            }
        }
    }
}
