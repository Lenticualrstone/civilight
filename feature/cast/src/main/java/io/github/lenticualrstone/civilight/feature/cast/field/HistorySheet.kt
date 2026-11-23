package io.github.lenticualrstone.civilight.feature.cast.field

import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.FieldHistorySheet
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable

/** `표시 이름 · 변화 기록` (CST-10, CST-11); feature-local (the shared key names a field). */
@Serializable
data class DisplayNameHistorySheet(val projectKey: Key, val characterKey: Key) : SheetKey

/** `<항목> · 변화 기록` of a custom field (shared [FieldHistorySheet], S9). */
@Composable
fun SheetScope.FieldHistorySheetContent(sheet: FieldHistorySheet) = TimedHistoryContent(
    projectKey = sheet.projectKey,
    subject = TimedSubject.Field(sheet.kind, sheet.entityKey, sheet.fieldDefKey),
    editSheet = FieldValueEditSheet(sheet.projectKey, sheet.kind, sheet.entityKey, sheet.fieldDefKey),
)

/** `표시 이름 · 변화 기록`. */
@Composable
fun SheetScope.DisplayNameHistoryContent(sheet: DisplayNameHistorySheet) = TimedHistoryContent(
    projectKey = sheet.projectKey,
    subject = TimedSubject.DisplayName(sheet.characterKey),
    editSheet = DisplayNameEditSheet(sheet.projectKey, sheet.characterKey),
)

/** Steps of `시점 추가`: the list, then picking the point, then the value at that point. */
private enum class HistoryStep { LIST, PICK_POINT, VALUE }

/**
 * The history sheet (`FieldHistory`): one row per record in story order, the record in effect at
 * the lens highlighted, later ones `ink3` with `지금 시점보다 뒤` / `예정`; buttons `시점 추가`
 * (point picker, then the value; a point-anchored record) and `<point> 값 고치기` (opens the value
 * sheet over this one).
 */
@Composable
private fun SheetScope.TimedHistoryContent(projectKey: Key, subject: TimedSubject, editSheet: SheetKey) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(projectKey)
    val gate = rememberHiddenGate(projectKey)
    val lens = doc.castLens
    val view = remember(doc, subject, lens) { TimedEditModel.history(doc, subject, lens) }
    if (view == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    var step by rememberSaveable { mutableStateOf(HistoryStep.LIST) }
    var unitKey by rememberSaveable { mutableStateOf<String?>(null) }
    val editable = !store.readOnly
    when (step) {
        HistoryStep.PICK_POINT -> UnitPickerSheet(
            doc, gate, view.addLabel,
            onPick = {
                unitKey = it
                step = HistoryStep.VALUE
            },
            onClose = { step = HistoryStep.LIST },
            selectedKey = unitKey,
        )
        HistoryStep.VALUE -> {
            val unit = unitKey
            if (unit == null) {
                LaunchedEffect(Unit) { step = HistoryStep.LIST }
            } else {
                PointValueStep(doc, subject, unit, gate, onBack = { step = HistoryStep.LIST }) { add ->
                    editor.act(CastUiCopy.BTN_ADD_POINT) { d, ctx -> add(d, ctx) }
                    step = HistoryStep.LIST
                }
            }
        }
        HistoryStep.LIST -> {
            SheetHeader(view.title, onClose = ::close, subtitle = view.sub, closeLabel = CastUiCopy.BTN_CLOSE)
            SheetScrollBody(Modifier.padding(top = 10.dp)) {
                for (row in view.rows) HistoryRow(row)
            }
            if (editable) {
                SheetActions {
                    // Icon list row `소속 기록 시트 · 시점 추가` (class b): icon `plus` + `시점`, read as `시점 추가`.
                    if (view.addLabel == CastUiCopy.BTN_ADD_POINT) {
                        CivilightIconButton(IconButtonSpec.ADD_POINT, { step = HistoryStep.PICK_POINT }, Modifier.weight(1f))
                    } else {
                        CivilightButton(view.addLabel, { step = HistoryStep.PICK_POINT }, Modifier.weight(1f), ButtonVariant.GHOST)
                    }
                    CivilightButton(view.editLabel, { host.open(editSheet) }, Modifier.weight(1f))
                }
            }
        }
    }
}

/** The value at a picked point: the type's inputs (or the display-name line) and `시점 추가`. */
@Composable
private fun SheetScope.PointValueStep(
    doc: ProjectDoc,
    subject: TimedSubject,
    unitKey: Key,
    gate: HiddenGate,
    onBack: () -> Unit,
    onAdd: ((ProjectDoc, io.github.lenticualrstone.civilight.domain.cast.EditContext) -> ProjectDoc) -> Unit,
) {
    val point = remember(doc, unitKey) { UnitLabels(doc).reference(doc.index.unit(unitKey) ?: return@remember "") }
    when (subject) {
        is TimedSubject.Field -> {
            val def = TimedEditModel.def(doc, subject)
            if (def == null) {
                LaunchedEffect(Unit) { onBack() }
                return
            }
            var draft by rememberSaveable(stateSaver = FieldDraftSaver) { mutableStateOf(FieldDraft()) }
            var picking by rememberSaveable { mutableStateOf(false) }
            val linkKind = def.linkTargetKind?.known
            if (picking && linkKind != null) {
                LinkPicker(doc, gate, linkKind, onPick = { key ->
                    draft = draft.copy(linkKeys = (draft.linkKeys + key).distinct(), unknown = false)
                    picking = false
                }, onClose = { picking = false })
                return
            }
            SheetHeader(def.name, onClose = onBack, subtitle = point, closeLabel = CastUiCopy.BTN_CLOSE)
            SheetScrollBody {
                FieldValueEditor(doc, def, draft, { draft = it }, onPickLink = { picking = true }, longTextEditable = true, hiddenUnlocked = gate.open, modifier = Modifier.padding(top = 14.dp))
            }
            SheetActions {
                CivilightButton(CastUiCopy.BTN_ADD_POINT, {
                    val saved = draft
                    onAdd { d, ctx -> TimedEditModel.addPoint(d, subject, unitKey, FieldDrafts.toValue(def, saved, null, ctx), null, ctx) }
                }, Modifier.weight(1f), enabled = FieldDrafts.isValid(def, draft, FieldDrafts.monthCount(doc)))
            }
        }
        is TimedSubject.DisplayName -> {
            var text by rememberSaveable { mutableStateOf("") }
            SheetHeader(CastUiCopy.DISPLAY_NAME, onClose = onBack, subtitle = point, closeLabel = CastUiCopy.BTN_CLOSE)
            CivilightTextField(text, { text = it }, Modifier.fillMaxWidth().padding(top = 14.dp), placeholder = doc.index.character(subject.characterKey)?.name)
            SheetActions {
                CivilightButton(CastUiCopy.BTN_ADD_POINT, {
                    val saved = text
                    onAdd { d, ctx -> TimedEditModel.addPoint(d, subject, unitKey, null, saved, ctx) }
                }, Modifier.weight(1f))
            }
        }
    }
}
