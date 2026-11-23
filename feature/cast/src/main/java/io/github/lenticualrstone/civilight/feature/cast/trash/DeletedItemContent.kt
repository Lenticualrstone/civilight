package io.github.lenticualrstone.civilight.feature.cast.trash

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastWrites
import io.github.lenticualrstone.civilight.feature.cast.common.Step
import io.github.lenticualrstone.civilight.feature.cast.common.actionContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.DeleteImpactSheet
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.feature.cast.create.CastCharacterPicker
import io.github.lenticualrstone.civilight.ui.picker.FactionPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.TermPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate

/**
 * A deleted item's check row in a sheet (CST-42, CST-44; `Check`): `◇ 지운 인물 · 노아`, `휴지통 · 3일
 * 남음 · 대사 14 · 씬 등장 2 · @ 3 · 레코더 1`, then `화자 한꺼번에 바꾸기` → the picker of that kind →
 * `대사 14 · 노아 → 오웬` → `바꾸기` (one undo step, toast with `되돌리기`), `되살리기` (in the trash only;
 * a purged name tag reads `영구 삭제 · 노아` and has none) and `이대로 두기` (the row moves to
 * `맞다고 표시한 것`). Shown by [DeleteImpactContent] for an item that is not live.
 */
@Composable
fun SheetScope.DeletedItemContent(sheet: DeleteImpactSheet, doc: ProjectDoc, readOnly: Boolean, recorder: RecorderLibrary, onDone: () -> Unit) {
    val deps = LocalFeatureDeps.current
    val gate = rememberHiddenGate(sheet.projectKey)
    val flow = rememberItemTrash(sheet.projectKey)
    val writes = remember(deps, sheet.projectKey) { CastWrites(deps, sheet.projectKey) }
    var bulkName by rememberSaveable { mutableStateOf<String?>(null) }
    var target by rememberSaveable { mutableStateOf<Key?>(null) }
    val row = remember(doc, recorder, gate.open) {
        DeletedItemModel.row(doc, sheet.kind, sheet.itemKey, deps.actionContext().trash, recorder, itemsLocked = !gate.open)
    }
    if (row == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val view = remember(row, doc) { DeletedItemModel.view(row, doc) }
    val action = bulkName?.let { name -> view.bulk.firstOrNull { it.kind.name == name } }
    val to = target
    when {
        action != null && to == null -> BulkPicker(sheet.projectKey, doc, sheet.kind, sheet.itemKey, gate, action.label, onPick = { target = it }) { bulkName = null }
        action != null && to != null -> {
            val line = remember(doc, to) { DeletedItemModel.preview(doc, sheet.kind, sheet.itemKey, to, action.kind) }
            SheetHeader(action.label, onClose = { target = null }, closeLabel = CastUiCopy.BTN_CLOSE)
            CivilightText(line.orEmpty(), CivilightTheme.type.command, Modifier.padding(top = 16.dp), color = CivilightTheme.colors.ink)
            SheetActions {
                CivilightButton(CastUiCopy.BTN_CANCEL, { target = null }, Modifier.weight(1f), ButtonVariant.GHOST)
                CivilightButton(
                    CastUiCopy.BTN_CHANGE,
                    {
                        val kind = sheet.kind
                        val from = sheet.itemKey
                        writes.launch(CastUiCopy.BTN_CHANGE, { d, ctx -> DeletedItemModel.apply(d, kind, from, to, action.kind, ctx.trash)?.let { Step(it.doc, it) } }) { done ->
                            deps.toasts.show(done.line, TrashCopy.UNDO) { writes.revert(TrashCopy.UNDO, done.change) }
                        }
                        target = null
                        bulkName = null
                    },
                    Modifier.weight(1f),
                    enabled = !readOnly && line != null,
                )
            }
        }
        else -> {
            SheetHeader(view.title, onClose = onDone, subtitle = view.sub, closeLabel = CastUiCopy.BTN_CLOSE)
            SheetScrollBody {
                if (!readOnly) for (bulk in view.bulk) ListRow(bulk.label, onClick = { bulkName = bulk.kind.name }, compact = true)
            }
            if (!readOnly && (view.canRestore || !view.kept)) {
                SheetActions {
                    if (!view.kept) {
                        CivilightButton(CastUiCopy.BTN_KEEP, { flow.keepItem(sheet.itemKey); onDone() }, Modifier.weight(1f), ButtonVariant.GHOST)
                    }
                    if (view.canRestore) {
                        CivilightButton(CastUiCopy.BTN_RESTORE, { flow.restore(sheet.kind, sheet.itemKey); onDone() }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** The replacement, from the picker of the deleted item's kind (search · tags for characters). */
@Composable
private fun SheetScope.BulkPicker(projectKey: Key, doc: ProjectDoc, kind: EntityKind, from: Key, gate: HiddenGate, title: String, onPick: (Key) -> Unit, onClose: () -> Unit) {
    val pick: (Key) -> Unit = { key -> if (key != from && ItemTrash.isLive(doc, kind, key)) onPick(key) }
    when (kind) {
        // U-15.3 (§8.3 Q45): the bulk speaker picker has the create row; a created character is live by construction.
        EntityKind.CHARACTER -> CastCharacterPicker(projectKey, doc, gate, title, onPick = { key -> if (key != from) onPick(key) }, onClose = onClose)
        EntityKind.PLACE -> PlacePickerSheet(doc, gate, title, onPick = { pick(it.key) }, onClose = onClose, includeRegions = false)
        EntityKind.TERM -> TermPickerSheet(doc, gate, title, onPick = { key, _ -> pick(key) }, onClose = onClose)
        EntityKind.FACTION -> FactionPickerSheet(doc, gate, title, onPick = pick, onClose = onClose)
        else -> LaunchedEffect(Unit) { onClose() }
    }
}
