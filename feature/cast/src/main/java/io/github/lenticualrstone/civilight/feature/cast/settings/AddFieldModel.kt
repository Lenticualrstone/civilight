package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldScope
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTypes
import io.github.lenticualrstone.civilight.domain.cast.field.NewField
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.serialization.Serializable

/** The inputs of `항목 넣기` (CST-21, `RCFieldAdd`; CST-22 `RCProfileAdd` adds [scope]). */
@Serializable
data class AddFieldDraft(
    val name: String = "",
    val exportName: String = "",
    val type: FieldType = FieldType.SHORT_TEXT,
    val unit: String = "",
    val options: List<String> = emptyList(),
    val allowFreeText: Boolean = false,
    val multiple: Boolean = false,
    val precision: DatePrecision = DatePrecision.YEAR,
    val linkTarget: FieldLinkTargetKind = FieldLinkTargetKind.FACTION,
    val calcSourceKey: Key? = null,
    val inHeader: Boolean = false,
    val inList: Boolean = false,
    val inFilter: Boolean = false,
    val secret: Boolean = false,
    val scope: FieldScope = FieldScope.ALL_CHARACTERS,
)

/** `넣기` state and the notice under the name. */
data class AddFieldCheck(val canSave: Boolean, val sameNameNotice: String?)

/** Pure model of the `항목 넣기` sheet over `domain:cast` [FieldDefinitions] (CST-20, CST-21, CST-22). */
object AddFieldModel {
    /** The nine types in table order (also the radio grid order). */
    val TYPES: List<FieldType> = FieldTypes.ALL

    /** `연결` targets: exactly 세력 · 장소 · 용어 (TC-13-16). */
    val LINK_TARGETS: List<FieldLinkTargetKind> = FieldTypes.LINK_TARGETS

    val PRECISIONS: List<DatePrecision> = listOf(DatePrecision.YEAR, DatePrecision.YMD)

    /** `필터` can be checked only for 고르기 · 여러 개 고르기 · 연결 (TC-F-20). */
    fun filterEnabled(type: FieldType): Boolean = FieldTypes.canFilter(type)

    /** Fields a `계산` age can be computed from: live `날짜` fields of the kind shared by every item. */
    fun calcSources(doc: ProjectDoc, kind: EntityKind): List<FieldDef> =
        FieldDefinitions.live(doc, kind).filter { it.type.known == FieldType.DATE && it.scopeCharacterKey == null }

    /** The draft after choosing a type: `필터` is dropped where it cannot apply; `계산` takes the first date field. */
    fun withType(doc: ProjectDoc, kind: EntityKind, draft: AddFieldDraft, type: FieldType): AddFieldDraft = draft.copy(
        type = type,
        inFilter = draft.inFilter && filterEnabled(type),
        calcSourceKey = if (type == FieldType.CALC) draft.calcSourceKey ?: calcSources(doc, kind).firstOrNull()?.key else draft.calcSourceKey,
    )

    fun toNewField(draft: AddFieldDraft, kind: EntityKind, characterKey: Key?): NewField = NewField(
        name = draft.name,
        type = draft.type,
        entityKind = kind,
        exportName = draft.exportName.takeIf { it.isNotBlank() },
        inHeader = draft.inHeader,
        inList = draft.inList,
        inFilter = draft.inFilter && filterEnabled(draft.type),
        isSecret = draft.secret && kind == EntityKind.CHARACTER,
        shortTextMultiple = draft.multiple,
        unit = draft.unit,
        options = draft.options,
        allowFreeText = draft.allowFreeText,
        datePrecision = draft.precision,
        linkTargetKind = draft.linkTarget,
        calcSourceKey = draft.calcSourceKey,
        scopeCharacterKey = characterKey?.takeIf { kind == EntityKind.CHARACTER && draft.scope == FieldScope.THIS_CHARACTER },
    )

    /** `넣기` is enabled with a name (and, for `계산`, a source field); same name shows `◇ 같은 이름 항목 있음`. */
    fun check(doc: ProjectDoc, draft: AddFieldDraft, kind: EntityKind): AddFieldCheck {
        val base = FieldDefinitions.check(doc, toNewField(draft, kind, null))
        val calcOk = draft.type != FieldType.CALC || draft.calcSourceKey?.let { key -> calcSources(doc, kind).any { it.key == key } } == true
        return AddFieldCheck(base.canSave && calcOk, base.sameNameNotice)
    }

    /** Adds the field at the end of its kind's order. */
    fun add(doc: ProjectDoc, draft: AddFieldDraft, kind: EntityKind, characterKey: Key?, ctx: EditContext): ProjectDoc =
        if (!check(doc, draft, kind).canSave) doc else FieldDefinitions.add(doc, toNewField(draft, kind, characterKey), ctx).first
}
