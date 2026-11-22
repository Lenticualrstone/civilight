package io.github.lenticualrstone.civilight.feature.cast.merge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.KeyValueLine
import io.github.lenticualrstone.civilight.designsystem.component.RadioMark
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.MergeItemsSheet
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.FactionPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.TermPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad

/**
 * The shared `합치기` sheet (`RCMerge`, CST-45, WLD-15; S22) for a character, place, term or faction.
 * [MergeItemsSheet.otherKey] is merged into [MergeItemsSheet.keepKey] (the one that stays). With
 * `keepKey == otherKey` the sheet first shows the picker of that kind (the speaker picker's window
 * for characters, CST-45 "pick the target"), so callers that know only the item open it that way.
 * The default of every row is the target's value; nothing is guessed.
 */
@Composable
fun SheetScope.MergeItemsContent(sheet: MergeItemsSheet) {
    val deps = LocalFeatureDeps.current
    val editor = rememberCastEditor(sheet.projectKey)
    val load by editor.state.collectAsState()
    val recorderState by deps.stores.recorder.state.collectAsState()
    LaunchedEffect(Unit) { deps.stores.recorder.ensureLoaded() }
    val gate = rememberHiddenGate(sheet.projectKey)
    val flow = rememberMergeFlow(sheet.projectKey)
    var target by rememberSaveable(sheet) { mutableStateOf(sheet.keepKey.takeIf { it != sheet.otherKey }) }
    var done by remember { mutableStateOf(false) }
    if (done) return
    val ready = load as? ProjectLoad.Ready ?: return
    val doc = ready.doc
    val merged = sheet.otherKey
    val chosen = target
    if (chosen == null) {
        TargetPicker(doc, sheet.kind, merged, gate, onPick = { target = it }, onClose = ::close)
        return
    }
    var picks by rememberSaveable(sheet, chosen) { mutableStateOf(listOf<String>()) }
    var showSame by rememberSaveable(sheet, chosen) { mutableStateOf(false) }
    val recorder = recorderState.library.takeIf { recorderState.loaded }
    val view = remember(doc, chosen, picks, recorder, gate.open) { MergeModel.view(doc, sheet.kind, merged, chosen, picks.toSet(), recorder, gate.open) }
    if (view == null) {
        // Either item left (merged, deleted) while the sheet was open.
        LaunchedEffect(Unit) {
            done = true
            close()
        }
        return
    }
    MergeBody(
        view = view,
        editable = !ready.readOnly,
        showSame = showSame,
        onToggleSame = { showSame = !showSame },
        onPick = { id, fromMerged -> picks = MergeModel.pick(picks.toSet(), id, fromMerged).toList() },
        onCancel = {
            done = true
            close()
        },
        onConfirm = {
            done = true
            flow.merge(sheet.kind, merged, chosen, picks.toSet()) {}
            close()
        },
    )
}

/** Step 1: the item that stays, from the picker of [kind]; the merged item itself is ignored. */
@Composable
private fun SheetScope.TargetPicker(doc: ProjectDoc, kind: EntityKind, merged: Key, gate: HiddenGate, onPick: (Key) -> Unit, onClose: () -> Unit) {
    val title = MergeModel.title(kind)
    val pick: (Key) -> Unit = { key -> if (key != merged && MergeModel.canMerge(doc, kind, merged, key)) onPick(key) }
    when (kind) {
        EntityKind.CHARACTER -> CharacterPickerSheet(doc, gate, onPick = pick, onClose = onClose, title = title)
        EntityKind.PLACE -> PlacePickerSheet(doc, gate, title, onPick = { pick(it.key) }, onClose = onClose, includeRegions = false)
        EntityKind.TERM -> TermPickerSheet(doc, gate, title, onPick = { key, _ -> pick(key) }, onClose = onClose)
        EntityKind.FACTION -> FactionPickerSheet(doc, gate, title, onPick = pick, onClose = onClose)
        else -> LaunchedEffect(Unit) { onClose() }
    }
}

@Composable
private fun SheetScope.MergeBody(
    view: MergeView,
    editable: Boolean,
    showSame: Boolean,
    onToggleSame: () -> Unit,
    onPick: (String, Boolean) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(view.title, onClose = onCancel, subtitle = view.sub, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        SectionLabel(view.movedTitle, Modifier.padding(top = 16.dp))
        if (view.movedLine.isNotEmpty()) {
            CivilightText(view.movedLine, type.small.merge(TextStyle(fontSize = 13.sp)), Modifier.padding(top = 4.dp), color = colors.ink2)
        }
        view.differingTitle?.let { title ->
            SectionLabel(title, Modifier.padding(top = 16.dp))
            ColumnHeads(view.columns)
            for (cell in view.cells) CellRow(cell, editable, onPick)
        }
        view.overlapTitle?.let { title ->
            SectionLabel(title, Modifier.padding(top = 16.dp))
            for (cell in view.overlaps) CellRow(cell, editable, onPick)
        }
        Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            view.sameRow?.let { (label, value) ->
                if (view.sameLabels.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                        CivilightText(label, type.small, Modifier.weight(1f), color = colors.ink3)
                        TextAction(value, onToggleSame, color = colors.ink, style = type.command)
                    }
                    if (showSame) CivilightText(view.sameLabels.joinToString(CastUiCopy.SEP), type.small, Modifier.padding(bottom = 8.dp), color = colors.ink2)
                } else {
                    KeyValueLine(label, value)
                }
            }
            for ((label, value) in view.infoRows) KeyValueLine(label, value)
        }
        CivilightText(view.footnote, type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
    }
    SheetActions {
        CivilightButton(view.cancelLabel, onCancel, Modifier.weight(1f), ButtonVariant.GHOST)
        CivilightButton(view.confirmLabel, onConfirm, Modifier.weight(1f), enabled = editable)
    }
}

/** `노아(시장) · 합쳐짐` | `노아 · 남음` over the two value columns. */
@Composable
private fun ColumnHeads(columns: Pair<String, String>) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Spacer(Modifier.width(LABEL_WIDTH))
        CivilightText(columns.first, type.small, Modifier.weight(1f).padding(start = 8.dp), color = colors.ink3, maxLines = 1)
        CivilightText(columns.second, type.small, Modifier.weight(1f).padding(start = 8.dp), color = colors.ink2, maxLines = 1)
    }
}

/** One row: label (and sub) in a 76 dp column, then the merged and the target cell, each a radio. */
@Composable
private fun CellRow(cell: MergeCellView, editable: Boolean, onPick: (String, Boolean) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(LABEL_WIDTH)) {
            CivilightText(cell.label, type.label, color = colors.ink2)
            cell.sub?.let { CivilightText(it, type.small, color = colors.ink3) }
        }
        ChoiceCell(cell.mergedText, cell.fromMerged, editable, Modifier.weight(1f)) { onPick(cell.id, true) }
        ChoiceCell(cell.targetText, !cell.fromMerged, editable, Modifier.weight(1f)) { onPick(cell.id, false) }
    }
}

@Composable
private fun ChoiceCell(text: String, selected: Boolean, editable: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (selected) Modifier.background(colors.sel) else Modifier)
            .pressable(PressAction.Select(selected, onClick), enabled = editable, role = Role.RadioButton, scaleOnPress = false)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioMark(selected)
        CivilightText(
            text,
            type.itemTitleCompact.merge(TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)),
            color = if (selected) colors.ink else colors.ink2,
            maxLines = 3,
        )
    }
}

private val LABEL_WIDTH = 76.dp
