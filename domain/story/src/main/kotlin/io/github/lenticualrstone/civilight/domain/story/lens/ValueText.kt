package io.github.lenticualrstone.civilight.domain.story.lens

import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.FieldValueState
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.text.plainText
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Plain one-line text of a record value or base value, for lens rows, history lines and footnotes
 * (`ED`; the cast and world editors own rich rendering). Relation slots read `만나기 전` when empty.
 */
class ValueText(private val doc: ProjectDoc) {
    /** Text of a stored record [value] for [target]. */
    fun of(target: ChangeTarget, value: JsonElement?): String {
        val v = value?.takeUnless { it is JsonNull }
        return when (target) {
            is ChangeTarget.Field -> v?.let { field(CivilightJson.decodeElement(FieldValue.serializer(), it), target) } ?: "없음"
            is ChangeTarget.DisplayName -> v?.let { CivilightJson.decodeElement(String.serializer(), it) }
                ?: doc.index.character(target.characterKey)?.name.orEmpty()
            is ChangeTarget.RelationSlot -> when (target.slot.known) {
                RelationSlotName.ADDRESS_A, RelationSlotName.ADDRESS_B -> v?.let { CivilightJson.decodeElement(String.serializer(), it) } ?: "만나기 전"
                else -> v?.let { slot(CivilightJson.decodeElement(SlotValue.serializer(), it)) } ?: "만나기 전"
            }
            is ChangeTarget.RegionShape -> "영역 모양"
            is ChangeTarget.TermReveal -> "첫 공개"
            is ChangeTarget.Unknown -> "?"
        }
    }

    /** Text of the base value (before any record). */
    fun base(target: ChangeTarget): String = when (target) {
        is ChangeTarget.Field -> target.entityKind.known?.let { kind ->
            doc.index.fieldsOf(kind, target.entityKey)?.get(target.fieldDefKey)?.let { field(it, target) }
        } ?: "없음"
        is ChangeTarget.DisplayName -> doc.index.character(target.characterKey)?.let { it.displayNameBase ?: it.name }.orEmpty()
        is ChangeTarget.RelationSlot -> {
            val relation = doc.index.relation(target.relationKey)
            when (val slot = target.slot.known) {
                RelationSlotName.ADDRESS_A, RelationSlotName.ADDRESS_B -> relation?.baseAddress(slot) ?: "만나기 전"
                null -> "만나기 전"
                else -> relation?.baseSlot(slot)?.let(::slot) ?: "만나기 전"
            }
        }
        else -> of(target, null)
    }

    /** Kind name of a between/mind slot (`경계`), else its kept text or description. */
    fun slot(value: SlotValue): String =
        value.kindKey?.let { doc.index.relationKind(it)?.name } ?: value.kindFallbackText ?: value.description ?: "만나기 전"

    fun field(value: FieldValue, target: ChangeTarget.Field? = null): String = when (value.state.known) {
        FieldValueState.UNKNOWN -> "??"
        FieldValueState.FALLBACK_TEXT -> value.fallbackText.orEmpty()
        else -> when (val p = value.payload) {
            is FieldPayload.Texts -> p.values.joinToString(" · ")
            is FieldPayload.Number -> p.value + (target?.let { doc.index.fieldDef(it.fieldDefKey)?.unit }.orEmpty())
            is FieldPayload.Choice -> {
                val def = target?.let { doc.index.fieldDef(it.fieldDefKey) }
                (p.optionKeys.mapNotNull { k -> def?.options?.firstOrNull { it.key == k }?.label } + listOfNotNull(p.freeText)).joinToString(" · ")
            }
            is FieldPayload.Date -> listOfNotNull("${p.year}년", p.month?.let { "${it}월" }, p.day?.let { "${it}일" }).joinToString(" ")
            is FieldPayload.Anniversary -> "${p.month}월 ${p.day}일"
            is FieldPayload.Link -> p.targetKeys.mapNotNull { k ->
                EntityKind.entries.firstNotNullOfOrNull { kind -> doc.index.nameOf(kind, k) }
            }.joinToString(" · ")
            is FieldPayload.Rich -> p.doc.firstOrNull()?.runs?.plainText.orEmpty()
            else -> "?"
        }
    }
}
