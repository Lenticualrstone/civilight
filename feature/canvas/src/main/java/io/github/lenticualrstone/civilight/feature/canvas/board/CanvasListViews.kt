package io.github.lenticualrstone.civilight.feature.canvas.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardOverlay
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.DialogCard
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.ListCreateRow
import io.github.lenticualrstone.civilight.designsystem.component.LocalDialogDismiss
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.ScrimDialog
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.statusTop
import io.github.lenticualrstone.civilight.designsystem.component.systemBottomWithoutKeyboard
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.glass.GlassBackground
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.OnGlass
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics
import io.github.lenticualrstone.civilight.designsystem.theme.OverlayGaps
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.common.createdFlash
import io.github.lenticualrstone.civilight.feature.canvas.page.ActionRows
import io.github.lenticualrstone.civilight.feature.canvas.page.SheetAction
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.lens.LensChipLabel

/**
 * The canvas head (N-58): back, the small line `캔버스` and the canvas name with `▾`. Name and line are one
 * button (≥ 44 tall) that opens the list sheet, whatever the number of canvases. Feature-local because
 * `TopGlassBar` takes a plain title (Requests: an `onTitle` on the shared bar).
 */
@Composable
internal fun CanvasTopBar(
    name: String,
    back: IconAction,
    wide: Boolean,
    lens: LensChipLabel?,
    onLens: () -> Unit,
    onName: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val start = if (wide) LayoutMetrics.topBarStartInner else LayoutMetrics.topBarStartCover
    val top = if (wide) LayoutMetrics.topBarContentTopInner else LayoutMetrics.topBarContentTopCover
    Box(Modifier.fillMaxWidth().heightIn(min = LayoutMetrics.topBarCover + inset)) {
        GlassBackground(GlassSurface.TOP_BAR, Modifier.matchParentSize())
        OnGlass {
            Row(
                Modifier.fillMaxWidth().padding(top = inset + top, start = start - 12.dp, end = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                CivilightIconButton(IconId.Back, back.label, back.onClick, tint = colors.ink)
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .pressable(onClick = onName, shape = RoundedCornerShape(10.dp))
                        .semantics { contentDescription = "$name${CanvasCopy.SEP}${CanvasCopy.LIST_OPEN}" }
                        .padding(top = 2.dp),
                ) {
                    CivilightText(CanvasCopy.SMALL_LINE, type.numberHeader, color = colors.ink3, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CivilightText(
                            name,
                            if (wide) type.headerTitleInner else type.headerTitle,
                            Modifier.weight(1f, fill = false),
                            color = colors.ink,
                            maxLines = 1,
                        )
                        CivilightIcon(IconId.Down, contentDescription = null, size = 16.dp, tint = colors.ink2)
                    }
                }
                if (lens != null) {
                    Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) { LensChip(lens.text, lens.state, onLens) }
                }
            }
        }
    }
}

/**
 * The list sheet (N-58): `＋ 새 캔버스` on top (U-15), then the live canvases; tapping a row opens it, its `⋯`
 * opens [CanvasRowMenuSheet]. Read-only projects show the rows only.
 */
@Composable
internal fun SheetScope.CanvasListContent(
    view: CanvasListView,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onMenu: (String) -> Unit,
) {
    SheetHeader(title = CanvasCopy.LIST_TITLE, onClose = ::close, closeLabel = CanvasCopy.BTN_CLOSE)
    if (view.canCreate) ListCreateRow(CreateKind.CANVAS, onCreate, Modifier.padding(top = 8.dp))
    Column(Modifier.padding(top = if (view.canCreate) 0.dp else 8.dp)) {
        view.rows.forEach { row ->
            ListRow(
                title = row.name,
                subtitle = CanvasCopy.nodeCount(row.nodes),
                onClick = { onOpen(row.key) },
                selected = row.current,
                compact = true,
                modifier = Modifier.createdFlash(row.highlighted, outset = 10.dp),
                trailing = if (view.canEdit) ({ CivilightIconButton(IconId.More, CanvasCopy.ROW_MENU, { onMenu(row.key) }) }) else null,
            )
        }
    }
}

/** A canvas row's `⋯`: `이름 바꾸기 · 지우기`, drawn like the page menu (S-6, D14). */
@Composable
internal fun SheetScope.CanvasRowMenuContent(
    row: CanvasListRow?,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val actions = buildList {
        if (row != null) {
            add(SheetAction(CanvasCopy.BTN_RENAME, onRename))
            if (row.canDelete) add(SheetAction(CanvasCopy.BTN_DELETE, onDelete, destructive = true))
        }
    }
    ActionRows(row?.name.orEmpty(), CanvasCopy.SMALL_LINE.takeIf { row != null }, actions, ::close)
}

/** U-15.9: the create card of a canvas or a page (`이름` only), list mode `취소 · 만들고 열기 · 만들기`. */
@Composable
internal fun NameCreateCard(
    state: CreateCardState,
    onChange: (CreateCardState) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    onCreateAndOpen: () -> Unit,
) {
    CreateCardOverlay(
        title = CreateCard.title(state.kind),
        mode = state.mode,
        canCreate = state.canCreate,
        onCancel = onCancel,
        onCreate = onCreate,
        onCreateAndOpen = onCreateAndOpen,
        dismissOnScrim = CreateCard.dismissOnScrim(state),
    ) {
        CreateCardFields(state, { key, value -> onChange(state.with(key, value)) })
    }
}

/**
 * `이름 바꾸기` of a canvas: a centred card that stays above the keyboard (as the DS-72 create card),
 * the name field focused with the caret at its end, `취소 · 바꾸기` (c-class). `바꾸기` is off while the
 * name is blank or unchanged.
 */
@Composable
internal fun CanvasRenameCard(current: String, onCancel: () -> Unit, onRename: (String) -> Unit) {
    var value by rememberSaveable(current, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(current, TextRange(current.length))) }
    val focus = remember { FocusRequester() }
    val canRename = CanvasRename.canRename(value.text, current)
    val outsideBottom = systemBottomWithoutKeyboard()
    val outsideTop = statusTop()
    val outsideKeyboard = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    ScrimDialog(onDismissRequest = onCancel, contentAlignment = Alignment.TopCenter, dismissOnScrim = !canRename) {
        val keyboard = maxOf(outsideKeyboard, WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        val bottom = if (keyboard > 0.dp) keyboard + OverlayGaps.aboveKeyboard else maxOf(outsideBottom, systemBottomWithoutKeyboard()) + CreateCard.BOTTOM_GAP_DP.dp
        Box(
            Modifier.fillMaxSize().padding(top = maxOf(outsideTop, statusTop()) + OverlayGaps.top, bottom = bottom),
            contentAlignment = Alignment.Center,
        ) {
            DialogCard(CanvasCopy.RENAME_TITLE) {
                CivilightTextField(value, { value = it }, Modifier.focusRequester(focus), label = CanvasCopy.FIELD_NAME)
                ButtonRow(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    val cancel = LocalDialogDismiss.current ?: onCancel
                    CivilightButton(CanvasCopy.BTN_CANCEL, cancel, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                    CivilightButton(CanvasCopy.BTN_CHANGE, { onRename(value.text) }, Modifier.weight(1f), variant = ButtonVariant.PRIMARY, enabled = canRename)
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** Pure rule of the rename card (JVM-tested). */
object CanvasRename {
    fun canRename(name: String, current: String): Boolean = name.isNotBlank() && name.trim() != current.trim()
}
