package io.github.lenticualrstone.civilight.feature.cast.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.ChoiceCard
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TypeGridCell
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.field.FieldScope
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTypes
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.navigation.AddFieldSheet
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.json.Json

private val DraftSaver: Saver<AddFieldDraft, String> = Saver(
    save = { Json.encodeToString(AddFieldDraft.serializer(), it) },
    restore = { runCatching { Json.decodeFromString(AddFieldDraft.serializer(), it) }.getOrNull() },
)

/**
 * `항목 넣기` (shared [AddFieldSheet]). From 프로젝트 설정 ([AddFieldSheet.entityKey] null, CST-21,
 * `RCFieldAdd`): 이름 · 내보내기 이름 · 종류 grid · type inputs · 보이는 곳 · 비밀 항목 · `넣기`.
 * From a record (CST-22, `RCProfileAdd`): 이름 · 종류 · type inputs · `어디에` cards · `넣기`.
 */
@Composable
fun SheetScope.AddFieldSheetContent(sheet: AddFieldSheet) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val kind = sheet.kind
    val character = sheet.entityKey?.takeIf { kind == EntityKind.CHARACTER }?.let(doc.index::character)
    val fromRecord = sheet.entityKey != null
    var draft by rememberSaveable(sheet, stateSaver = DraftSaver) { mutableStateOf(AddFieldDraft()) }
    val check = remember(doc, draft, kind) { AddFieldModel.check(doc, draft, kind) }
    val focus = remember { FocusRequester() }
    if (store.readOnly) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val sub = if (character != null) CastUiCopy.recordSub(character.name, character.recordNo) else CastUiCopy.addFieldSub(kind)
    SheetHeader(CastUiCopy.ADD_FIELD_TITLE, onClose = ::close, subtitle = sub, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody(Modifier.padding(top = 14.dp)) {
        CivilightTextField(
            draft.name, { draft = draft.copy(name = it) }, Modifier.fillMaxWidth().focusRequester(focus),
            label = CastUiCopy.NAME, hint = check.sameNameNotice,
        )
        if (!fromRecord) {
            CivilightTextField(draft.exportName, { draft = draft.copy(exportName = it) }, Modifier.fillMaxWidth().padding(top = 10.dp), label = CastUiCopy.EXPORT_NAME)
        }
        Label(CastUiCopy.TYPE)
        TypeGrid(draft.type) { draft = AddFieldModel.withType(doc, kind, draft, it) }
        TypeInputs(draft, kind, AddFieldModel.calcSources(doc, kind).map { it.key to it.name }) { draft = it }
        if (fromRecord && character != null) {
            Label(CastUiCopy.WHERE)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceCard(CastUiCopy.SCOPE_ALL, draft.scope == FieldScope.ALL_CHARACTERS, { draft = draft.copy(scope = FieldScope.ALL_CHARACTERS) }, subtitle = CastUiCopy.SCOPE_ALL_SUB, compact = true)
                ChoiceCard(CastUiCopy.SCOPE_ONE, draft.scope == FieldScope.THIS_CHARACTER, { draft = draft.copy(scope = FieldScope.THIS_CHARACTER) }, subtitle = CastUiCopy.onlyFor(character.name), compact = true)
            }
        } else if (!fromRecord) {
            Label(CastUiCopy.PLACES)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceBox(CastUiCopy.PLACE_HEADER, draft.inHeader, true) { draft = draft.copy(inHeader = it) }
                PlaceBox(CastUiCopy.PLACE_LIST, draft.inList, true) { draft = draft.copy(inList = it) }
                PlaceBox(CastUiCopy.PLACE_FILTER, draft.inFilter, AddFieldModel.filterEnabled(draft.type)) { draft = draft.copy(inFilter = it) }
            }
            if (kind == EntityKind.CHARACTER) {
                SwitchRow(CastUiCopy.SECRET, draft.secret, { draft = draft.copy(secret = it) }, subtitle = CastUiCopy.SECRET_SUB)
            }
        }
    }
    SheetActions {
        CivilightButton(CastUiCopy.BTN_INSERT, {
            val saved = draft
            editor.act(CastUiCopy.ADD_FIELD_TITLE) { d, ctx -> AddFieldModel.add(d, saved, kind, sheet.entityKey, ctx) }
            host.close()
        }, Modifier.weight(1f), enabled = check.canSave)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun Label(text: String) {
    CivilightText(text, CivilightTheme.type.label, Modifier.padding(top = 16.dp, bottom = 6.dp), color = CivilightTheme.colors.ink2)
}

/** `종류`: the nine types as a two-column radio grid (`sel` fill on the chosen one). */
@Composable
private fun TypeGrid(selected: FieldType, onSelect: (FieldType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (pair in AddFieldModel.TYPES.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (t in pair) TypeGridCell(FieldTypes.label(t), t == selected, { onSelect(t) }, Modifier.weight(1f))
                if (pair.size == 1) Column(Modifier.weight(1f)) {}
            }
        }
    }
}

/** The inputs a type needs: 단위, 보기 (+ 직접 쓰기), 연결 target, 날짜 precision, 계산 source, 여러 개. */
@Composable
private fun TypeInputs(draft: AddFieldDraft, kind: EntityKind, calcSources: List<Pair<String, String>>, onDraft: (AddFieldDraft) -> Unit) {
    val colors = CivilightTheme.colors
    when (draft.type) {
        FieldType.NUMBER -> Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            CivilightTextField(draft.unit, { onDraft(draft.copy(unit = it)) }, Modifier.weight(1f), label = CastUiCopy.UNIT)
            CivilightText(CastUiCopy.NUMBER_NOTE, CivilightTheme.type.small, Modifier.weight(1.4f).padding(bottom = 12.dp), color = colors.ink3)
        }
        FieldType.CHOICE, FieldType.MULTI_CHOICE -> {
            Label(CastUiCopy.OPTIONS)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                draft.options.forEachIndexed { i, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CivilightTextField(option, { text -> onDraft(draft.copy(options = draft.options.toMutableList().also { it[i] = text })) }, Modifier.weight(1f))
                        CivilightIconButton(IconId.X, CastUiCopy.BTN_REMOVE, { onDraft(draft.copy(options = draft.options.filterIndexed { j, _ -> j != i })) })
                    }
                }
                AddChip(CastUiCopy.OPTIONS, { onDraft(draft.copy(options = draft.options + "")) })
            }
            if (draft.type == FieldType.CHOICE) {
                SwitchRow(CastUiCopy.FREE_TEXT, draft.allowFreeText, { onDraft(draft.copy(allowFreeText = it)) })
            }
        }
        FieldType.LINK -> {
            Label(CastUiCopy.LINK_TARGET)
            ChipFlow {
                for (target in AddFieldModel.LINK_TARGETS) {
                    FilterChip(target.label, draft.linkTarget == target, { onDraft(draft.copy(linkTarget = target)) })
                }
            }
        }
        FieldType.DATE -> {
            Label(CastUiCopy.DATE_PRECISION)
            ChipFlow {
                for (p in AddFieldModel.PRECISIONS) FilterChip(FieldTypes.precisionLabel(p), draft.precision == p, { onDraft(draft.copy(precision = p)) })
            }
        }
        FieldType.CALC -> {
            Label(CastUiCopy.CALC_SOURCE)
            ChipFlow {
                for ((key, name) in calcSources) FilterChip(CastUiCopy.fromSource(name), draft.calcSourceKey == key, { onDraft(draft.copy(calcSourceKey = key)) })
            }
        }
        FieldType.SHORT_TEXT -> SwitchRow(CastUiCopy.MULTIPLE, draft.multiple, { onDraft(draft.copy(multiple = it)) })
        FieldType.LONG_TEXT, FieldType.ANNIVERSARY -> Unit
    }
}

/** A `보이는 곳` checkbox with its label (20 dp box, the whole pair toggles). */
@Composable
private fun PlaceBox(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.pressable(onClick = { onChange(!checked) }, enabled = enabled, scaleOnPress = false, onClickLabel = label).padding(end = 10.dp).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckboxMark(checked && enabled, dense = true, enabled = enabled)
        CivilightText(label, CivilightTheme.type.label, color = if (enabled) CivilightTheme.colors.ink else CivilightTheme.colors.ink3)
    }
}
