package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.ConversionResult
import io.github.lenticualrstone.civilight.domain.cast.field.FieldConversion
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTypes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldOption
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/** One option of a `고르기 · 여러 개 고르기` field with who holds it (`리엔 외 3명`, TC-F-08). */
data class OptionRowView(val key: Key, val label: String, val holders: String)

/** The field edit screen (`FieldDefEdit`, CST-23) for one definition. */
data class FieldDefView(
    val key: Key,
    val kind: EntityKind,
    val name: String,
    val exportName: String,
    val type: FieldType?,
    /** `짧은 글`, `숫자` … */
    val typeLabel: String,
    /** The row sub as `인물 항목` writes it (`숫자 · cm · 머리`). */
    val sub: String,
    /** Types this field may change to (CST-23 table); empty for long text (TC-F-07). */
    val offered: List<FieldType>,
    val unit: String?,
    val options: List<OptionRowView>,
    val allowFreeText: Boolean?,
    val inHeader: Boolean,
    val inList: Boolean,
    val inFilter: Boolean,
    /** `필터` can be checked only for 고르기 · 여러 개 고르기 · 연결 (TC-F-20). */
    val filterEnabled: Boolean,
    val secret: Boolean,
    val hidden: Boolean,
    /** An `이 인물만` field (CST-22): `모든 인물에 넣기` is offered. */
    val characterOnly: Boolean,
    /** `값이 있는 인물 12명` (TC-F-12). */
    val deleteSummary: String,
)

/**
 * Pure model of changing a field definition (CST-23) over `domain:cast` [FieldDefinitions] and
 * [FieldConversion]. Every change keeps the hidden key, so values, `◆` records and the export name
 * follow it. Blank names and labels change nothing.
 */
object FieldDefEditModel {
    fun view(doc: ProjectDoc, defKey: Key): FieldDefView? {
        val def = doc.index.fieldDef(defKey)?.takeIf { it.deletedAt == null } ?: return null
        val kind = def.entityKind.known ?: return null
        val type = def.type.known
        return FieldDefView(
            key = def.key,
            kind = kind,
            name = def.name,
            exportName = def.exportName.orEmpty(),
            type = type,
            typeLabel = type?.let(FieldTypes::label) ?: def.type.raw,
            sub = FieldSettings.row(doc, def).sub,
            offered = FieldConversion.offered(def),
            unit = if (type == FieldType.NUMBER) def.unit.orEmpty() else null,
            options = if (type != null && FieldTypes.hasOptions(type)) options(doc, def) else emptyList(),
            allowFreeText = if (type == FieldType.CHOICE) def.allowFreeText == true else null,
            inHeader = def.inHeader,
            inList = def.inList,
            inFilter = def.inFilter,
            filterEnabled = type != null && FieldTypes.canFilter(type),
            secret = def.isSecret,
            hidden = def.definitionHidden,
            characterOnly = def.scopeCharacterKey != null,
            deleteSummary = FieldDefinitions.deleteSummary(doc, def.key),
        )
    }

    private fun options(doc: ProjectDoc, def: FieldDef): List<OptionRowView> =
        def.options.orEmpty().sortedWith(compareBy<FieldOption>({ it.order }, { it.key })).map { o ->
            OptionRowView(o.key, o.label, FieldDefinitions.namesSummary(FieldDefinitions.usersOfOption(doc, def.key, o.key)))
        }

    /** Rename (`키 → 신장`): values, `◆` and the export name stay (TC-F-11). */
    fun rename(doc: ProjectDoc, defKey: Key, name: String, ctx: EditContext): ProjectDoc =
        if (name.isBlank() || doc.index.fieldDef(defKey)?.name == name.trim().nfc()) doc else FieldDefinitions.rename(doc, defKey, name, ctx)

    /** `내보내기 이름`; empty = the field name (CST-26). */
    fun setExportName(doc: ProjectDoc, defKey: Key, exportName: String, ctx: EditContext): ProjectDoc =
        if (doc.index.fieldDef(defKey)?.exportName.orEmpty() == exportName.trim()) doc else FieldDefinitions.setExportName(doc, defKey, exportName, ctx)

    /** Unit text only; numbers are never converted (TC-F-10, `환산하지 않음`). */
    fun setUnit(doc: ProjectDoc, defKey: Key, unit: String, ctx: EditContext): ProjectDoc =
        if (doc.index.fieldDef(defKey)?.unit.orEmpty() == unit.trim()) doc else FieldDefinitions.setUnit(doc, defKey, unit, ctx)

    fun addOption(doc: ProjectDoc, defKey: Key, label: String, ctx: EditContext): ProjectDoc =
        if (label.isBlank()) doc else FieldDefinitions.addOption(doc, defKey, label, ctx)

    /** Option rename (`여 → 여성`): every holder shows the new label (TC-F-09). */
    fun renameOption(doc: ProjectDoc, defKey: Key, optionKey: Key, label: String, ctx: EditContext): ProjectDoc {
        val current = doc.index.fieldDef(defKey)?.options.orEmpty().firstOrNull { it.key == optionKey }?.label
        return if (label.isBlank() || current == label.trim().nfc()) doc else FieldDefinitions.renameOption(doc, defKey, optionKey, label, ctx)
    }

    /** Option delete: holders keep the text with `◇ 보기에 없는 값` (TC-F-08). */
    fun removeOption(doc: ProjectDoc, defKey: Key, optionKey: Key, ctx: EditContext): ProjectDoc =
        FieldDefinitions.removeOption(doc, defKey, optionKey, ctx)

    /** `연결` asks for its target kind; the others need none. */
    fun needsTarget(to: FieldType): Boolean = to == FieldType.LINK

    /**
     * What a type change would do, computed on a copy before anything is written (TC-F-05,
     * TC-F-22): the result's [ConversionResult.notice] reads `◇ 바꾸지 못한 값 1`.
     */
    fun preview(doc: ProjectDoc, defKey: Key, to: FieldType, target: FieldLinkTargetKind?, ctx: EditContext): ConversionResult? {
        val def = doc.index.fieldDef(defKey) ?: return null
        if (to !in FieldConversion.offered(def)) return null
        return FieldConversion.convert(doc, defKey, to, ctx, target)
    }

    /** Applies a confirmed type change; a pair outside the CST-23 table changes nothing. */
    fun convert(doc: ProjectDoc, defKey: Key, to: FieldType, target: FieldLinkTargetKind?, ctx: EditContext): ProjectDoc =
        preview(doc, defKey, to, target, ctx)?.doc ?: doc
}
