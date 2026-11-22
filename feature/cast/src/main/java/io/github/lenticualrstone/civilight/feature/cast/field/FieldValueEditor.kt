package io.github.lenticualrstone.civilight.feature.cast.field

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.RemovableChip
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.entityKind
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.ui.picker.FactionPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.TermPickerSheet
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * The inputs of one field value by type (CST-09, CST-20): text lines, number with its unit, radio
 * list (+ `직접 쓰기`), checkboxes, year or year · month · day, month · day, linked items, and the
 * `알 수 없음` switch. [onPickLink] opens the item picker of a `연결` field (drawn by the caller in
 * place of these inputs). [longTextEditable] is false for a `긴 글` holding links or paren runs.
 */
@Composable
fun FieldValueEditor(
    doc: ProjectDoc,
    def: FieldDef,
    draft: FieldDraft,
    onDraft: (FieldDraft) -> Unit,
    onPickLink: () -> Unit,
    longTextEditable: Boolean,
    hiddenUnlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (def.type.known) {
            FieldType.SHORT_TEXT -> {
                val multiple = def.shortTextMultiple == true
                // With `여러 개` an empty line always waits at the end, so a new line needs no extra button.
                val lines = if (multiple && draft.texts.lastOrNull()?.isNotBlank() == true) draft.texts + "" else draft.texts.ifEmpty { listOf("") }
                lines.forEachIndexed { i, line ->
                    CivilightTextField(
                        value = line,
                        onValueChange = { text ->
                            val next = lines.toMutableList().also { it[i] = text }
                            onDraft(draft.copy(texts = next, unknown = false))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            FieldType.LONG_TEXT -> if (longTextEditable) {
                CivilightTextField(
                    value = draft.longText,
                    onValueChange = { onDraft(draft.copy(longText = it, unknown = false)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    textStyle = type.body,
                )
            } else {
                CivilightText(draft.longText, type.body, color = colors.ink)
                CivilightText(CastUiCopy.TODO_COPY_CAST_LONG_TEXT_RICH, type.small, color = colors.ink3)
            }
            FieldType.NUMBER, FieldType.CALC -> CivilightTextField(
                value = draft.number,
                onValueChange = { onDraft(draft.copy(number = it.filter { c -> c.isDigit() || c == '.' || c == '-' }, unknown = false)) },
                modifier = Modifier.fillMaxWidth(),
                unit = if (def.type.known == FieldType.CALC) FieldText.AGE_UNIT else def.unit,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            FieldType.CHOICE, FieldType.MULTI_CHOICE -> {
                val multi = def.type.known == FieldType.MULTI_CHOICE
                for (option in def.options.orEmpty().sortedBy { it.order }) {
                    val on = option.key in draft.optionKeys
                    if (multi) {
                        CheckboxRow(option.label, on, { onDraft(FieldDrafts.toggleOption(def, draft, option.key)) })
                    } else {
                        RadioRow(option.label, on, { onDraft(FieldDrafts.toggleOption(def, draft, option.key)) })
                    }
                }
                if (!multi && def.allowFreeText == true) {
                    CivilightTextField(
                        value = draft.freeText,
                        onValueChange = { onDraft(draft.copy(freeText = it, optionKeys = emptyList(), unknown = false)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = CastUiCopy.FREE_TEXT,
                    )
                }
            }
            FieldType.DATE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberBox(CastUiCopy.YEAR, draft.year, Modifier.weight(1.4f)) { onDraft(draft.copy(year = it, unknown = false)) }
                if (def.datePrecision?.known == DatePrecision.YMD) {
                    NumberBox(CastUiCopy.MONTH, draft.month, Modifier.weight(1f), monthHint(doc, draft.month)) { onDraft(draft.copy(month = it, unknown = false)) }
                    NumberBox(CastUiCopy.DAY, draft.day, Modifier.weight(1f)) { onDraft(draft.copy(day = it, unknown = false)) }
                }
            }
            FieldType.ANNIVERSARY -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberBox(CastUiCopy.MONTH, draft.month, Modifier.weight(1f), monthHint(doc, draft.month)) { onDraft(draft.copy(month = it, unknown = false)) }
                NumberBox(CastUiCopy.DAY, draft.day, Modifier.weight(1f)) { onDraft(draft.copy(day = it, unknown = false)) }
            }
            FieldType.LINK -> {
                val kind = def.linkTargetKind?.known?.entityKind
                ChipFlow {
                    for (key in draft.linkKeys) {
                        val label = kind?.let { ItemRefs.resolve(doc, it, key, hiddenUnlocked).wideLabel } ?: key
                        RemovableChip(label, { onDraft(draft.copy(linkKeys = draft.linkKeys - key, unknown = false)) }, CastUiCopy.BTN_REMOVE)
                    }
                    if (kind != null) AddChip(CastUiCopy.pickTitle(kind), onPickLink)
                }
            }
            null -> CivilightText(draft.longText, type.body, color = colors.ink3)
        }
        SwitchRow(CastUiCopy.UNKNOWN_TOGGLE, draft.unknown, { onDraft(if (it) FieldDraft(unknown = true) else draft.copy(unknown = false)) })
    }
}

/** The item picker of a `연결` value (세력 · 장소 · 용어), drawn in place of the value inputs. */
@Composable
fun ColumnScope.LinkPicker(doc: ProjectDoc, gate: HiddenGate, kind: FieldLinkTargetKind, onPick: (Key) -> Unit, onClose: () -> Unit) {
    val title = CastUiCopy.pickTitle(kind.entityKind)
    when (kind) {
        FieldLinkTargetKind.FACTION -> FactionPickerSheet(doc, gate, title, onPick, onClose)
        FieldLinkTargetKind.TERM -> TermPickerSheet(doc, gate, title, { key, _ -> onPick(key) }, onClose)
        FieldLinkTargetKind.PLACE -> PlacePickerSheet(doc, gate, title, { onPick(it.key) }, onClose, includeRegions = false)
    }
}

/** The world calendar's month name under a typed month (`세계 달력`), else nothing. */
private fun monthHint(doc: ProjectDoc, month: String): String? {
    val m = month.trim().toIntOrNull() ?: return null
    if (m !in 1..FieldDrafts.monthCount(doc)) return null
    return FieldText.monthDayText(doc, m, null).takeIf { it != CastUiCopy.monthNumber(m) }
}

@Composable
private fun NumberBox(label: String, value: String, modifier: Modifier, hint: String? = null, onChange: (String) -> Unit) {
    CivilightTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        modifier = modifier,
        label = label,
        hint = hint,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
