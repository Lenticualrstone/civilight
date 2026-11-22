package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.richtext.RichDocEdit
import kotlinx.serialization.Serializable

/**
 * The steps of `+ 변화` on the event detail (WLD-33): the item, then one of its fields, then the
 * new value. Each step replaces the one before inside the sheet (WLD-32 convention).
 */
enum class ChangeStep {
    ITEM, FIELD, VALUE, TARGET,
    ;

    /** How deep the step is: item, field, value, then the target picker over the value. */
    private val depth: Int get() = when (this) { ITEM -> 0; FIELD -> 1; VALUE -> 2; TARGET -> 3 }

    /** True when going from [from] to this step moves forward (the sheet pushes in), false going back. */
    fun isAhead(from: ChangeStep): Boolean = depth >= from.depth

    companion object {
        /** `forward` of a sheet step swap. */
        fun isAhead(from: ChangeStep, to: ChangeStep): Boolean = to.isAhead(from)
    }
}

/**
 * The form of `+ 변화`, saveable as JSON. [kind] / [entityKey] = the item, [fieldDefKey] = its
 * field; the value parts are read by the field's type: [text] (짧은 글 · 긴 글 · 숫자), [optionKeys]
 * (고르기 · 여러 개 고르기), [year] [month] [day] (날짜 · 기념일), [targetKeys] (연결).
 */
@Serializable
data class ChangeDraft(
    val kind: EntityKind? = null,
    val entityKey: Key? = null,
    val fieldDefKey: Key? = null,
    val text: String = "",
    val optionKeys: List<Key> = emptyList(),
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val targetKeys: List<Key> = emptyList(),
)

/** One field row of the `항목` step: name and the value in effect at the event (`없음` when empty). */
data class ChangeFieldUi(val defKey: Key, val name: String, val current: String)

/** What the value step draws for the chosen field. */
sealed interface ValueInput {
    data class Text(val multiline: Boolean, val unit: String?) : ValueInput
    data class Number(val unit: String?) : ValueInput
    data class Options(val options: List<Pair<Key, String>>, val multiple: Boolean) : ValueInput
    data class Date(val withMonthDay: Boolean) : ValueInput
    data object MonthDay : ValueInput
    data class Link(val pick: PickKind, val names: List<String>) : ValueInput
}

/**
 * `+ 변화` (WLD-33, rule 12 `세계 사건` anchor): a `◆` record on a project field of a character,
 * place, term or faction, anchored on this event (applies from before the story start). The write
 * is `domain:cast` [TimedValues.addRecord] with [Anchor.WorldEvent], the shared `◆` path.
 */
object EventChanges {
    /** The four kinds that carry project fields. */
    val PICK_KINDS: Set<PickKind> = setOf(PickKind.CHARACTER, PickKind.TERM, PickKind.PLACE, PickKind.FACTION)

    fun entityKindOf(kind: PickKind): EntityKind? = when (kind) {
        PickKind.CHARACTER -> EntityKind.CHARACTER
        PickKind.TERM -> EntityKind.TERM
        PickKind.PLACE -> EntityKind.PLACE
        PickKind.FACTION -> EntityKind.FACTION
        else -> null
    }

    /** True when `+ 변화` is offered: a live world event (a story event's changes sit on its scene instead). */
    fun offered(doc: ProjectDoc, eventKey: Key): Boolean {
        val event = doc.index.event(eventKey) ?: return false
        return event.deletedAt == null && event.kind.known != EventKind.STORY
    }

    /** Fields that can change at a point: live, shown and not calculated (`나이` follows `출생`). */
    fun fields(doc: ProjectDoc, kind: EntityKind, entityKey: Key): List<FieldDef> =
        FieldDefinitions.visibleFor(doc, kind, entityKey).filter { it.type.known != null && it.type.known != FieldType.CALC }

    /** The `항목` step rows with each field's base value. */
    fun fieldRows(doc: ProjectDoc, kind: EntityKind, entityKey: Key): List<ChangeFieldUi> = fields(doc, kind, entityKey).map { def ->
        val base = FieldReader.baseValue(doc, kind, entityKey, def.key)
        val text = base?.let { io.github.lenticualrstone.civilight.domain.cast.field.FieldText.format(doc, def, it).text }
        ChangeFieldUi(def.key, def.name, text?.takeIf { it.isNotBlank() } ?: WorldCopy.CHANGE_NONE)
    }

    fun def(doc: ProjectDoc, draft: ChangeDraft): FieldDef? {
        val kind = draft.kind ?: return null
        val entityKey = draft.entityKey ?: return null
        return fields(doc, kind, entityKey).firstOrNull { it.key == draft.fieldDefKey }
    }

    /** The step the sheet can show for [draft] (a missing item or field falls back to its step). */
    fun effectiveStep(doc: ProjectDoc, step: ChangeStep, draft: ChangeDraft): ChangeStep {
        val hasItem = draft.kind != null && draft.entityKey != null
        return when {
            step == ChangeStep.ITEM || !hasItem -> ChangeStep.ITEM
            step == ChangeStep.FIELD -> ChangeStep.FIELD
            def(doc, draft) == null -> ChangeStep.FIELD
            else -> step
        }
    }

    /** How the value step asks for the value of [def]. */
    fun input(doc: ProjectDoc, def: FieldDef, draft: ChangeDraft): ValueInput = when (def.type.known) {
        FieldType.SHORT_TEXT -> ValueInput.Text(multiline = false, unit = def.unit)
        FieldType.LONG_TEXT -> ValueInput.Text(multiline = true, unit = null)
        FieldType.NUMBER -> ValueInput.Number(def.unit)
        FieldType.CHOICE -> ValueInput.Options(options(def), multiple = false)
        FieldType.MULTI_CHOICE -> ValueInput.Options(options(def), multiple = true)
        FieldType.DATE -> ValueInput.Date(withMonthDay = def.datePrecision?.known == DatePrecision.YMD)
        FieldType.ANNIVERSARY -> ValueInput.MonthDay
        FieldType.LINK -> ValueInput.Link(linkPick(def), draft.targetKeys.mapNotNull { name(doc, def, it) })
        FieldType.CALC, null -> ValueInput.Text(multiline = false, unit = null)
    }

    private fun options(def: FieldDef): List<Pair<Key, String>> =
        def.options.orEmpty().sortedWith(compareBy({ it.order }, { it.key })).map { it.key to it.label }

    fun linkPick(def: FieldDef): PickKind = when (def.linkTargetKind?.known) {
        FieldLinkTargetKind.PLACE -> PickKind.PLACE
        FieldLinkTargetKind.TERM -> PickKind.TERM
        FieldLinkTargetKind.FACTION, null -> PickKind.FACTION
    }

    private fun name(doc: ProjectDoc, def: FieldDef, key: Key): String? = when (def.linkTargetKind?.known) {
        FieldLinkTargetKind.PLACE -> doc.index.place(key)?.name
        FieldLinkTargetKind.TERM -> doc.index.term(key)?.name
        FieldLinkTargetKind.FACTION, null -> doc.index.faction(key)?.name
    }

    /** The value the draft describes, or null when it is empty or not valid for the field. */
    fun value(def: FieldDef, draft: ChangeDraft, newKey: () -> Key): FieldValue? {
        val payload: FieldPayload = when (def.type.known) {
            FieldType.SHORT_TEXT -> draft.text.trim().takeIf { it.isNotEmpty() }?.let { FieldPayload.Texts(listOf(it)) }
            FieldType.LONG_TEXT -> draft.text.trim().takeIf { it.isNotEmpty() }?.let { FieldPayload.Rich(RichDocEdit.apply(emptyList(), it, newKey)) }
            FieldType.NUMBER -> draft.text.trim().replace(',', '.').takeIf { NUMBER.matches(it) }?.let { FieldPayload.Number(it) }
            FieldType.CHOICE -> draft.optionKeys.takeIf { it.size == 1 }?.let { FieldPayload.Choice(it) }
            FieldType.MULTI_CHOICE -> draft.optionKeys.takeIf { it.isNotEmpty() }?.let { FieldPayload.Choice(it) }
            FieldType.DATE -> date(def, draft)
            FieldType.ANNIVERSARY -> monthDay(draft)
            FieldType.LINK -> draft.targetKeys.takeIf { it.isNotEmpty() }?.let { FieldPayload.Link(it) }
            FieldType.CALC, null -> null
        } ?: return null
        return FieldValue.of(payload)
    }

    private fun date(def: FieldDef, draft: ChangeDraft): FieldPayload.Date? {
        val year = draft.year.trim().toIntOrNull() ?: return null
        if (def.datePrecision?.known != DatePrecision.YMD) return FieldPayload.Date(year)
        val month = draft.month.trim().takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: return null }
        val day = draft.day.trim().takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: return null }
        if (month != null && month !in 1..12) return null
        if (day != null && (month == null || day !in 1..31)) return null
        return FieldPayload.Date(year, month, day)
    }

    private fun monthDay(draft: ChangeDraft): FieldPayload.Anniversary? {
        val month = draft.month.trim().toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        val day = draft.day.trim().toIntOrNull()?.takeIf { it in 1..31 } ?: return null
        return FieldPayload.Anniversary(month, day)
    }

    /** `넣기`: the new `◆` from this event on, or null when the draft is incomplete. */
    fun add(doc: ProjectDoc, eventKey: Key, draft: ChangeDraft, ctx: EditContext): ProjectDoc? {
        if (!offered(doc, eventKey)) return null
        val kind = draft.kind ?: return null
        val entityKey = draft.entityKey ?: return null
        val def = def(doc, draft) ?: return null
        val value = value(def, draft, ctx.newKey) ?: return null
        val (next, _) = TimedValues.addRecord(
            doc,
            FieldReader.target(kind, entityKey, def),
            FieldValueEdits.encode(value),
            Anchor.WorldEvent(eventKey),
            ctx,
        )
        return next
    }

    private val NUMBER = Regex("""-?\d+(\.\d+)?""")
}
