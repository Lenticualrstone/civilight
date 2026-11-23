package io.github.lenticualrstone.civilight.feature.cast.field

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.field.ValueState
import io.github.lenticualrstone.civilight.domain.cast.field.valueState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.docText
import kotlinx.serialization.Serializable

/**
 * What the field sheet's inputs hold while the user types (CST-09, CST-20): one shape for all nine
 * types, so a sheet keeps its draft across recomposition and process death (it is saved as JSON).
 * [unknown] is the `알 수 없음` toggle (`??`).
 */
@Serializable
data class FieldDraft(
    val unknown: Boolean = false,
    /** `짧은 글`: one line, several with `여러 개`. */
    val texts: List<String> = listOf(""),
    /** `긴 글` as plain paragraphs (one per line). */
    val longText: String = "",
    /** `숫자`, and a typed `계산` age. */
    val number: String = "",
    /** `고르기` (at most one) and `여러 개 고르기`. */
    val optionKeys: List<Key> = emptyList(),
    /** `고르기` with `직접 쓰기`. */
    val freeText: String = "",
    val year: String = "",
    val month: String = "",
    val day: String = "",
    /** `연결` targets in value order. */
    val linkKeys: List<Key> = emptyList(),
)

/** Result of checking a draft: whether `저장` (and the lens buttons) may run. */
data class DraftCheck(val valid: Boolean, val changed: Boolean) {
    val canSave: Boolean get() = valid && changed
}

/** Converting between stored values and [FieldDraft]s (CST-09 three states, CST-20 inputs). */
object FieldDrafts {
    private val DECIMAL = Regex("""-?\d+(\.\d+)?""")

    /** The draft a value opens with; kept fallback text and unknown payloads start empty. */
    fun from(def: FieldDef, value: FieldValue?): FieldDraft {
        if (value.valueState == ValueState.UNKNOWN) return FieldDraft(unknown = true)
        val payload = value?.payload?.takeIf { value.valueState == ValueState.VALUE } ?: return FieldDraft()
        return when (payload) {
            is FieldPayload.Texts -> FieldDraft(texts = payload.values.ifEmpty { listOf("") })
            is FieldPayload.Rich -> FieldDraft(longText = payload.doc.docText)
            is FieldPayload.Number -> FieldDraft(number = payload.value)
            is FieldPayload.Choice -> FieldDraft(optionKeys = payload.optionKeys, freeText = payload.freeText.orEmpty())
            is FieldPayload.Date -> FieldDraft(year = payload.year.toString(), month = payload.month?.toString().orEmpty(), day = payload.day?.toString().orEmpty())
            is FieldPayload.Anniversary -> FieldDraft(month = payload.month.toString(), day = payload.day.toString())
            is FieldPayload.Link -> FieldDraft(linkKeys = payload.targetKeys)
            is FieldPayload.Unknown -> FieldDraft()
        }
    }

    /** Months of the project's year: the world calendar's when it has one, else 12. */
    fun monthCount(doc: ProjectDoc): Int {
        val time = doc.project.worldTime
        val calendar = time.calendar?.months?.size?.takeIf { time.mode.known == WorldTimeMode.CALENDAR && it > 0 }
        return calendar ?: 12
    }

    /** Whether the inputs hold a storable value (blank counts as empty, which is valid). */
    fun isValid(def: FieldDef, draft: FieldDraft, monthCount: Int = 12): Boolean {
        if (draft.unknown) return true
        return when (def.type.known) {
            FieldType.NUMBER, FieldType.CALC -> draft.number.isBlank() || DECIMAL.matches(draft.number.trim())
            FieldType.DATE -> {
                val year = draft.year.trim()
                when {
                    year.isEmpty() -> draft.month.isBlank() && draft.day.isBlank()
                    year.toIntOrNull() == null -> false
                    def.datePrecision?.known != DatePrecision.YMD -> true
                    else -> monthDayValid(draft.month, draft.day, monthCount, allowEmpty = true)
                }
            }
            FieldType.ANNIVERSARY -> (draft.month.isBlank() && draft.day.isBlank()) || monthDayValid(draft.month, draft.day, monthCount, allowEmpty = false)
            else -> true
        }
    }

    private fun monthDayValid(month: String, day: String, monthCount: Int, allowEmpty: Boolean): Boolean {
        if (month.isBlank() && day.isBlank()) return allowEmpty
        val m = month.trim().toIntOrNull() ?: return false
        if (m !in 1..monthCount) return false
        if (day.isBlank()) return allowEmpty
        val d = day.trim().toIntOrNull() ?: return false
        return d in 1..31
    }

    /**
     * The value to store, or null for empty (empty is never stored). [original] is the value the
     * sheet opened with: a `긴 글` keeps its untouched paragraphs (keys, memo marks, recorder sources).
     */
    fun toValue(def: FieldDef, draft: FieldDraft, original: FieldValue?, ctx: EditContext): FieldValue? {
        if (draft == from(def, original)) return original
        if (draft.unknown) return FieldValue.UNKNOWN
        val payload: FieldPayload? = when (def.type.known) {
            FieldType.SHORT_TEXT -> {
                val lines = draft.texts.map { it.trim().nfc() }.filter { it.isNotEmpty() }
                val kept = if (def.shortTextMultiple == true) lines else lines.take(1)
                kept.takeIf { it.isNotEmpty() }?.let { FieldPayload.Texts(it) }
            }
            FieldType.LONG_TEXT -> {
                val before: RichDoc = (original?.payload as? FieldPayload.Rich)?.doc.orEmpty()
                LongTextEdit.apply(before, draft.longText, ctx).takeIf { it.isNotEmpty() }?.let { FieldPayload.Rich(it) }
            }
            FieldType.NUMBER, FieldType.CALC -> draft.number.trim().takeIf { it.isNotEmpty() }?.let { FieldPayload.Number(it) }
            FieldType.CHOICE -> {
                val free = draft.freeText.trim().nfc().takeIf { def.allowFreeText == true && it.isNotEmpty() }
                val keys = draft.optionKeys.take(1)
                if (keys.isEmpty() && free == null) null else FieldPayload.Choice(keys, if (keys.isEmpty()) free else null)
            }
            FieldType.MULTI_CHOICE -> {
                val order = def.options.orEmpty().map { it.key }
                draft.optionKeys.filter { it in order }.sortedBy { order.indexOf(it) }.takeIf { it.isNotEmpty() }?.let { FieldPayload.Choice(it) }
            }
            FieldType.DATE -> draft.year.trim().toIntOrNull()?.let { year ->
                if (def.datePrecision?.known == DatePrecision.YMD) {
                    FieldPayload.Date(year, draft.month.trim().toIntOrNull(), draft.day.trim().toIntOrNull()?.takeIf { draft.month.isNotBlank() })
                } else {
                    FieldPayload.Date(year)
                }
            }
            FieldType.ANNIVERSARY -> {
                val m = draft.month.trim().toIntOrNull()
                val d = draft.day.trim().toIntOrNull()
                if (m != null && d != null) FieldPayload.Anniversary(m, d) else null
            }
            FieldType.LINK -> draft.linkKeys.distinct().takeIf { it.isNotEmpty() }?.let { FieldPayload.Link(it) }
            null -> null
        }
        return FieldValueEdits.normalized(payload?.let { FieldValue.of(it) })
    }

    /**
     * A `긴 글` saved from the shared rich editor (`@` links and paren runs kept): `알 수 없음` wins,
     * an empty text is no value (empty is never stored).
     */
    fun richValue(draft: FieldDraft, rich: RichDoc): FieldValue? = when {
        draft.unknown -> FieldValue.UNKNOWN
        rich.isEmpty() -> null
        else -> FieldValueEdits.normalized(FieldValue.of(FieldPayload.Rich(rich)))
    }

    /** [isValid] plus whether anything differs from the value the sheet opened with. */
    fun check(def: FieldDef, draft: FieldDraft, initial: FieldDraft, monthCount: Int = 12): DraftCheck =
        DraftCheck(isValid(def, draft, monthCount), draft != initial)

    /** Toggles an option: `고르기` keeps one, `여러 개 고르기` keeps several. */
    fun toggleOption(def: FieldDef, draft: FieldDraft, optionKey: Key): FieldDraft {
        val multi = def.type.known == FieldType.MULTI_CHOICE
        val keys = when {
            optionKey in draft.optionKeys -> draft.optionKeys - optionKey
            multi -> draft.optionKeys + optionKey
            else -> listOf(optionKey)
        }
        return draft.copy(optionKeys = keys, freeText = if (!multi && keys.isNotEmpty()) "" else draft.freeText, unknown = false)
    }
}
