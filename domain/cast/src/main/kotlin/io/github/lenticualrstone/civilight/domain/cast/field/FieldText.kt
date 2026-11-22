package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.entityKind
import io.github.lenticualrstone.civilight.model.cast.FallbackReason
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.FieldValueState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.text.docText

/** The three value states of CST-09, plus kept text after an option removal or failed conversion. */
enum class ValueState { EMPTY, UNKNOWN, VALUE, FALLBACK }

val FieldValue?.valueState: ValueState
    get() = when (this?.state?.known) {
        // A state from a newer file: show its payload if any, else treat it as `??`.
        null -> if (this == null) ValueState.EMPTY else if (payload != null) ValueState.VALUE else ValueState.UNKNOWN
        FieldValueState.UNKNOWN -> ValueState.UNKNOWN
        FieldValueState.VALUE -> if (payload == null) ValueState.EMPTY else ValueState.VALUE
        FieldValueState.FALLBACK_TEXT -> ValueState.FALLBACK
    }

/** A value as a field row prints it. */
data class FieldDisplay(
    /** Printed text; empty for an empty value. */
    val text: String,
    val state: ValueState,
    /** `◇ 보기에 없는 값` / `◇ 바꾸지 못한 값` for kept fallback text. */
    val notice: String? = null,
    /** Resolved `연결` targets, in value order. */
    val refs: List<ResolvedItem> = emptyList(),
) {
    val isEmpty: Boolean get() = state == ValueState.EMPTY

    /** Any target is deleted (dotted + `◇`). */
    val dotted: Boolean get() = refs.any { it.dotted }
}

/** Formatting of field values (CST-20, CST-23, CST-41). */
object FieldText {
    const val OPTION_REMOVED = "◇ 보기에 없는 값"
    const val CONVERSION_FAILED = "◇ 바꾸지 못한 값"
    const val AGE_UNIT = "세"

    fun format(doc: ProjectDoc, def: FieldDef, value: FieldValue?, hiddenUnlocked: Boolean = false): FieldDisplay {
        val type = def.type.known
        return when (value.valueState) {
            ValueState.EMPTY -> FieldDisplay("", ValueState.EMPTY)
            ValueState.UNKNOWN -> FieldDisplay(unknownText(def), ValueState.UNKNOWN)
            ValueState.FALLBACK -> FieldDisplay(
                text = value?.fallbackText.orEmpty(),
                state = ValueState.FALLBACK,
                notice = when (value?.fallbackReason?.known) {
                    FallbackReason.OPTION_REMOVED -> OPTION_REMOVED
                    else -> CONVERSION_FAILED
                },
            )
            ValueState.VALUE -> {
                val payload = value!!.payload!!
                if (type == FieldType.LINK && payload is FieldPayload.Link) {
                    val kind = def.linkTargetKind?.known?.entityKind
                    val refs = if (kind == null) emptyList() else payload.targetKeys.map { ItemRefs.resolve(doc, kind, it, hiddenUnlocked) }
                    FieldDisplay(refs.joinToString(CastCopy.SEP) { it.wideLabel }, ValueState.VALUE, refs = refs)
                } else {
                    FieldDisplay(payloadText(doc, def, payload), ValueState.VALUE)
                }
            }
        }
    }

    /** `??` keeping the unit: `??세`, `?? cm`. */
    fun unknownText(def: FieldDef): String = when (def.type.known) {
        FieldType.CALC -> CastCopy.UNKNOWN + AGE_UNIT
        FieldType.NUMBER -> def.unit?.takeIf { it.isNotBlank() }?.let { "${CastCopy.UNKNOWN} $it" } ?: CastCopy.UNKNOWN
        else -> CastCopy.UNKNOWN
    }

    /** The visible text of a payload (also what a conversion to `짧은 글` keeps, TC-F-24). */
    fun payloadText(doc: ProjectDoc, def: FieldDef, payload: FieldPayload): String = when (payload) {
        is FieldPayload.Texts -> payload.values.filter { it.isNotBlank() }.joinToString(CastCopy.SEP)
        is FieldPayload.Rich -> payload.doc.docText
        is FieldPayload.Number -> when (def.type.known) {
            FieldType.CALC -> payload.value + AGE_UNIT
            else -> def.unit?.takeIf { it.isNotBlank() }?.let { "${payload.value} $it" } ?: payload.value
        }
        is FieldPayload.Choice -> {
            val labels = payload.optionKeys.mapNotNull { key -> def.options.orEmpty().firstOrNull { it.key == key }?.label }
            (labels + listOfNotNull(payload.freeText?.takeIf { it.isNotBlank() })).joinToString(CastCopy.SEP)
        }
        is FieldPayload.Date -> dateText(doc, payload.year, payload.month, payload.day)
        is FieldPayload.Anniversary -> monthDayText(doc, payload.month, payload.day)
        is FieldPayload.Link -> payload.targetKeys.joinToString(CastCopy.SEP) { key ->
            val kind = def.linkTargetKind?.known?.entityKind
            if (kind == null) key else ItemRefs.resolve(doc, kind, key, hiddenUnlocked = true).wideLabel
        }
        is FieldPayload.Unknown -> ""
    }

    /** `1183`, or `1183년 5월 10일` (world month names with `세계 달력`). */
    fun dateText(doc: ProjectDoc, year: Int, month: Int?, day: Int?): String =
        if (month == null) year.toString() else "${year}년 ${monthDayText(doc, month, day)}"

    /** `3월 14일`, or `<월 이름> 14일` with `세계 달력`. */
    fun monthDayText(doc: ProjectDoc, month: Int, day: Int?): String {
        val time = doc.project.worldTime
        val monthName = time.calendar?.months?.getOrNull(month - 1)?.name
            ?.takeIf { time.mode.known == WorldTimeMode.CALENDAR }
            ?: "${month}월"
        return if (day == null) monthName else "$monthName ${day}일"
    }

    /** Fix paths of a dotted `연결` value (CST-41 table row `Field 연결 values`). */
    fun linkFixPaths(ref: ResolvedItem): List<String> = when (ref) {
        is ResolvedItem.Trashed -> listOf("다른 항목 고르기", "비우기", "되살리기", "이대로 두기")
        is ResolvedItem.Tagged, is ResolvedItem.Missing -> listOf("다른 항목 고르기", "비우기", "이대로 두기")
        else -> emptyList()
    }
}
