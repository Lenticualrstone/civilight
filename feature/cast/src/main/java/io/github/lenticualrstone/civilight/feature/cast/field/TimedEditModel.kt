package io.github.lenticualrstone.civilight.feature.cast.field

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReading
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTypes
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.timed.EditSheet
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.story.lens.LensEdit
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import kotlinx.serialization.builtins.serializer

/** How a value sheet saves (CST-09, CST-10). */
sealed interface EditMode {
    /** No history at `최신` ([LensEdit] `Plain`): one primary `저장` (U-10) that writes the base value. */
    data object Plain : EditMode

    /** The two stacked buttons of `RTEditTwo` / `LensEdit`, with their copy from `TimedValues.editSheet`. */
    data class TwoButtons(val sheet: EditSheet) : EditMode
}

/** Which button saved. */
enum class SaveChoice { PLAIN, CORRECT, FROM_HERE }

/** The value sheet of a field or a display name at the lens. */
data class ValueEditView(
    val title: String,
    /** The value in effect as text (`비어 있음` when empty). */
    val currentText: String,
    val mode: EditMode,
    /** A computed value cannot be typed: `계산 · 출생에서`; null when the value is typed. */
    val derived: String?,
)

/** One row of the `<항목> · 변화 기록` sheet (`FieldHistory`). */
data class HistoryRowView(
    val recordKey: Key,
    val point: String,
    val value: String,
    /** The record's note plus `지금 시점보다 뒤` / `예정`. */
    val note: String?,
    val inEffect: Boolean,
    /** After the lens: drawn `ink3`. */
    val dim: Boolean,
)

/** The history sheet (CST-10). */
data class HistoryView(
    val title: String,
    val sub: String,
    val rows: List<HistoryRowView>,
    val addLabel: String,
    val editLabel: String,
)

/** The timed values the cast sheets edit: a custom field of any item kind, or a character's 표시 이름. */
sealed interface TimedSubject {
    val target: ChangeTarget

    data class Field(val kind: EntityKind, val entityKey: Key, val defKey: Key) : TimedSubject {
        override val target: ChangeTarget get() = ChangeTarget.Field(kind.open(), entityKey, defKey)
    }

    data class DisplayName(val characterKey: Key) : TimedSubject {
        override val target: ChangeTarget get() = DisplayNames.target(characterKey)
    }
}

/**
 * Pure models of the value and history sheets (CST-09 … CST-11). One-or-two buttons follow
 * `domain:story` [LensEdit.choice] (DECISIONS §1); the button copy comes from `domain:cast`
 * [TimedValues.editSheet]; writes go through `FieldValueEdits` / `DisplayNames`.
 */
object TimedEditModel {
    /** The live definition of a field subject, or null when it is gone. */
    fun def(doc: ProjectDoc, s: TimedSubject.Field): FieldDef? = doc.index.fieldDef(s.defKey)?.takeIf { it.deletedAt == null }

    fun reading(doc: ProjectDoc, s: TimedSubject.Field, def: FieldDef, lens: LensPoint): FieldReading =
        FieldReader.read(doc, s.kind, s.entityKey, def, ReadPoint.AtLens(lens))

    /** A value as history rows and the current line print it; empty reads `비어 있음`. */
    fun fieldText(doc: ProjectDoc, def: FieldDef, value: FieldValue?): String =
        FieldText.format(doc, def, value).text.ifEmpty { CastUiCopy.EMPTY_VALUE }

    fun recordText(doc: ProjectDoc, s: TimedSubject, record: ChangeRecord): String = when (s) {
        is TimedSubject.Field -> def(doc, s)?.let { fieldText(doc, it, record.valueAs(FieldValue.serializer())) } ?: CastUiCopy.EMPTY_VALUE
        is TimedSubject.DisplayName -> record.valueAs(String.serializer())?.takeIf { it.isNotBlank() }
            ?: doc.index.character(s.characterKey)?.name.orEmpty()
    }

    private fun mode(doc: ProjectDoc, s: TimedSubject, lens: LensPoint, currentText: String): EditMode =
        when (LensEdit.choice(doc, s.target, lens)) {
            EditChoice.Plain -> EditMode.Plain
            is EditChoice.TwoButtons -> EditMode.TwoButtons(TimedValues.editSheet(doc, s.target, lens, currentText) { recordText(doc, s, it) })
        }

    /** The value sheet of a field (null when the field or item is gone). */
    fun fieldView(doc: ProjectDoc, s: TimedSubject.Field, lens: LensPoint): ValueEditView? {
        val def = def(doc, s) ?: return null
        doc.index.fieldsOf(s.kind, s.entityKey) ?: return null
        val r = reading(doc, s, def, lens)
        val current = fieldText(doc, def, r.value)
        val derived = if (r.readOnly) {
            CastCopy.joinAll(listOf(def.type.known?.let(FieldTypes::label), FieldSettings.detail(doc, def)))
        } else {
            null
        }
        return ValueEditView(def.name, current, mode(doc, s, lens, current), derived)
    }

    /** The 표시 이름 sheet (CST-11); the current text is the override in effect, or the name. */
    fun displayNameView(doc: ProjectDoc, s: TimedSubject.DisplayName, lens: LensPoint): ValueEditView? {
        doc.index.character(s.characterKey) ?: return null
        val current = DisplayNames.read(doc, s.characterKey, lens).shown
        return ValueEditView(CastUiCopy.DISPLAY_NAME, current, mode(doc, s, lens, current), null)
    }

    /** The override text a display-name sheet opens with (empty = show 이름). */
    fun displayNameDraft(doc: ProjectDoc, s: TimedSubject.DisplayName, lens: LensPoint): String =
        DisplayNames.read(doc, s.characterKey, lens).override.orEmpty()

    /** Saves a field value with the chosen button. */
    fun saveField(doc: ProjectDoc, s: TimedSubject.Field, value: FieldValue?, choice: SaveChoice, lens: LensPoint, ctx: EditContext): ProjectDoc {
        val def = def(doc, s) ?: return doc
        return when (choice) {
            SaveChoice.PLAIN -> FieldValueEdits.setBase(doc, s.kind, s.entityKey, def.key, value, ctx)
            SaveChoice.CORRECT -> FieldValueEdits.correctInEffect(doc, s.kind, s.entityKey, def, value, lens, ctx)
            SaveChoice.FROM_HERE -> FieldValueEdits.changeFromHere(doc, s.kind, s.entityKey, def, value, lens, ctx)
        }
    }

    /** Saves a display name (blank = show 이름 from there on). */
    fun saveDisplayName(doc: ProjectDoc, s: TimedSubject.DisplayName, text: String, choice: SaveChoice, lens: LensPoint, ctx: EditContext): ProjectDoc {
        val value = text.trim().nfc().takeIf { it.isNotEmpty() }
        return when (choice) {
            SaveChoice.PLAIN -> DisplayNames.setBase(doc, s.characterKey, value, ctx)
            SaveChoice.CORRECT -> DisplayNames.correctInEffect(doc, s.characterKey, value, lens, ctx)
            SaveChoice.FROM_HERE -> DisplayNames.changeFromHere(doc, s.characterKey, value, lens, ctx)
        }
    }

    /** `시점 추가`: a record from the start of [unitKey] (rule 12 §1 point anchor). */
    fun addPoint(doc: ProjectDoc, s: TimedSubject, unitKey: Key, fieldValue: FieldValue?, displayText: String?, ctx: EditContext): ProjectDoc =
        when (s) {
            is TimedSubject.Field -> def(doc, s)?.let { FieldValueEdits.addPointRecord(doc, s.kind, s.entityKey, it, fieldValue, unitKey, ctx).first } ?: doc
            is TimedSubject.DisplayName -> {
                val value = displayText?.trim()?.nfc()?.takeIf { it.isNotEmpty() }?.let { ChangeRecord.encodeValue(String.serializer(), it) }
                TimedValues.addRecord(doc, s.target, value, Anchor.UnitStart(unitKey), ctx).first
            }
        }

    /** The history sheet of a subject at the lens. */
    fun history(doc: ProjectDoc, s: TimedSubject, lens: LensPoint): HistoryView? {
        val title = when (s) {
            is TimedSubject.Field -> def(doc, s)?.name ?: return null
            is TimedSubject.DisplayName -> doc.index.character(s.characterKey)?.let { CastUiCopy.DISPLAY_NAME } ?: return null
        }
        val sheet = TimedValues.historySheet(doc, s.target, lens, title)
        val rows = sheet.rows.map { row ->
            HistoryRowView(
                recordKey = row.record.key,
                point = row.point,
                value = recordText(doc, s, row.record),
                note = CastCopy.joinAll(listOf(row.note) + row.marks).ifEmpty { null },
                inEffect = row.inEffect,
                dim = row.afterLens,
            )
        }
        return HistoryView(sheet.title, sheet.sub, rows, sheet.addLabel, sheet.editLabel)
    }
}
