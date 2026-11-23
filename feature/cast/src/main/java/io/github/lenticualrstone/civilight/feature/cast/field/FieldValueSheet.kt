package io.github.lenticualrstone.civilight.feature.cast.field

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
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.ui.richtext.RichTextEditor
import io.github.lenticualrstone.civilight.ui.richtext.rememberRichTextEditorState
import io.github.lenticualrstone.civilight.navigation.FieldValueEditSheet
import io.github.lenticualrstone.civilight.domain.cast.entityKind
import io.github.lenticualrstone.civilight.feature.cast.trash.rememberItemTrash
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable

/**
 * `값 고치기` of a custom field (shared [FieldValueEditSheet]; CST-09, CST-10, S10) for any item
 * kind: the type's inputs, `알 수 없음`, and one or two save buttons. A `연결` field swaps the inputs
 * for the item picker in place, so the draft is kept.
 */
@Composable
fun SheetScope.FieldValueSheetContent(sheet: FieldValueEditSheet) {
    val host = this
    val deps = LocalFeatureDeps.current
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val gate = rememberHiddenGate(sheet.projectKey)
    val trash = rememberItemTrash(sheet.projectKey)
    val lens = doc.castLens
    val subject = TimedSubject.Field(sheet.kind, sheet.entityKey, sheet.fieldDefKey)
    val def = TimedEditModel.def(doc, subject)
    val view = remember(doc, subject, lens) { TimedEditModel.fieldView(doc, subject, lens) }
    if (def == null || view == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val reading = remember(doc, subject, lens) { TimedEditModel.reading(doc, subject, def, lens) }
    val initial = remember(def.key) { FieldDrafts.from(def, reading.value) }
    var draft by rememberSaveable(sheet, stateSaver = FieldDraftSaver) { mutableStateOf(initial) }
    var picking by rememberSaveable(sheet) { mutableStateOf(false) }
    val linkKind = def.linkTargetKind?.known

    if (picking && linkKind != null) {
        LinkPicker(doc, gate, linkKind, onPick = { key ->
            draft = draft.copy(linkKeys = (draft.linkKeys + key).distinct(), unknown = false)
            picking = false
        }, onClose = { picking = false })
        return
    }

    val editable = !store.readOnly && view.derived == null
    val longText = def.type.known == FieldType.LONG_TEXT
    val rich = rememberRichTextEditorState(remember(sheet) { (reading.value?.payload as? FieldPayload.Rich)?.doc.orEmpty() }, key = sheet)
    val check = FieldDrafts.check(def, draft, initial, FieldDrafts.monthCount(doc))
    val notice = FieldText.format(doc, def, reading.value).notice
    SheetHeader(view.title, onClose = ::close, subtitle = view.derived, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        (view.mode as? EditMode.TwoButtons)?.takeIf { editable }?.let { TwoButtonHead(it) { deps.lens(store).backToLatest() } }
        if (!editable) {
            CivilightText(view.currentText, CivilightTheme.type.lead, Modifier.padding(top = 12.dp), color = CivilightTheme.colors.ink)
        } else {
            notice?.let {
                CivilightText(CastCopy.join(reading.value?.fallbackText, it), CivilightTheme.type.small, Modifier.padding(top = 8.dp), color = CivilightTheme.colors.ink3)
            }
            FieldValueEditor(
                doc = doc,
                def = def,
                draft = draft,
                onDraft = { draft = it },
                onPickLink = { picking = true },
                longTextEditable = LongTextEdit.editable((reading.value?.payload as? FieldPayload.Rich)?.doc.orEmpty()),
                hiddenUnlocked = gate.open,
                modifier = Modifier.padding(top = 14.dp),
                dottedLinks = linkKind?.entityKind?.let { kind -> LinkFixes.rows(doc, kind, draft.linkKeys, sheet.entityKey, gate.open) }.orEmpty(),
                onRestoreLink = { key -> linkKind?.entityKind?.let { trash.restore(it, key) } },
                onKeepLink = { key -> trash.keep(key, sheet.entityKey) },
                richLongText = {
                    RichTextEditor(
                        state = rich,
                        projectKey = sheet.projectKey,
                        linkPickerTitle = CastUiCopy.LINK_PICKER_TITLE,
                        openSheet = host::open,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
            if (def.scopeCharacterKey != null) {
                // CST-22: an `이 인물만` field can become a project field later; this value is kept.
                TextAction(CastUiCopy.BTN_PROMOTE, {
                    editor.act(CastUiCopy.BTN_PROMOTE) { d, ctx -> FieldDefinitions.promoteToAll(d, def.key, ctx) }
                }, Modifier.padding(top = 8.dp), color = CivilightTheme.colors.ink2, style = CivilightTheme.type.label)
            }
            val canSave = if (longText) (check.canSave || (rich.changed && !draft.unknown)) else check.canSave
            SaveButtons(view.mode, canSave) { choice ->
                val saved = draft
                val savedRich = rich.result
                editor.act(choice.label(view.mode)) { d, ctx ->
                    val value = if (longText) FieldDrafts.richValue(saved, savedRich) else FieldDrafts.toValue(def, saved, reading.value, ctx)
                    TimedEditModel.saveField(d, subject, value, choice, lens, ctx)
                }
                host.close()
            }
        }
    }
}

/** The undo label of a save: the pressed button's label (UI copy). */
internal fun SaveChoice.label(mode: EditMode): String = when (this) {
    SaveChoice.PLAIN -> CastUiCopy.BTN_SAVE
    SaveChoice.CORRECT -> (mode as? EditMode.TwoButtons)?.sheet?.correctLabel ?: CastUiCopy.BTN_LENS_CORRECT
    SaveChoice.FROM_HERE -> (mode as? EditMode.TwoButtons)?.sheet?.changeLabel ?: CastUiCopy.BTN_LENS_CORRECT
}

/** `표시 이름` value sheet (CST-11) of a character; feature-local (the shared key names a field). */
@Serializable
data class DisplayNameEditSheet(val projectKey: Key, val characterKey: Key) : SheetKey

/** 표시 이름 with the same one-or-two buttons as fields; blank = show 이름 from there on. */
@Composable
fun SheetScope.DisplayNameSheetContent(sheet: DisplayNameEditSheet) {
    val host = this
    val deps = LocalFeatureDeps.current
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val lens = doc.castLens
    val subject = TimedSubject.DisplayName(sheet.characterKey)
    val view = remember(doc, subject, lens) { TimedEditModel.displayNameView(doc, subject, lens) }
    if (view == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val initial = remember(sheet) { TimedEditModel.displayNameDraft(doc, subject, lens) }
    var text by rememberSaveable(sheet) { mutableStateOf(initial) }
    val editable = !store.readOnly
    SheetHeader(view.title, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        (view.mode as? EditMode.TwoButtons)?.takeIf { editable }?.let { TwoButtonHead(it) { deps.lens(store).backToLatest() } }
        if (!editable) {
            CivilightText(view.currentText, CivilightTheme.type.lead, Modifier.padding(top = 12.dp))
        } else {
            CivilightTextField(text, { text = it }, Modifier.fillMaxWidth().padding(top = 14.dp), placeholder = doc.index.character(sheet.characterKey)?.name)
            SaveButtons(view.mode, text.trim() != initial.trim()) { choice ->
                val saved = text
                editor.act(choice.label(view.mode)) { d, ctx -> TimedEditModel.saveDisplayName(d, subject, saved, choice, lens, ctx) }
                host.close()
            }
        }
    }
}
