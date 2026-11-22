package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.PairHolder
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationKind
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

/** A value for one relation slot: a `사이`/`마음` value or a 호칭 text. */
sealed interface SlotInput {
    data class Kind(val value: SlotValue?) : SlotInput
    data class Address(val text: String?) : SlotInput
}

/**
 * Creating and editing relations (rule 13 §3, §5, §7; CST-29 … CST-31). One relation per unordered
 * pair. Pair kinds fill the other side as a fact; minds are never filled for the other side.
 */
object RelationEdits {
    /** The pair's relation, or a new empty one (`+ 관계` on an existing pair opens it, TC-13-10). */
    fun openOrCreate(doc: ProjectDoc, a: Key, b: Key, ctx: EditContext): Pair<ProjectDoc, Relation> {
        require(a != b) { "a relation needs two characters" }
        RelationReader.between(doc, a, b)?.let { return doc to it }
        val relation = Relation(
            key = ctx.newKey(), endA = RelationReader.characterRef(a), endB = RelationReader.characterRef(b),
            updatedAt = ctx.now, updatedBy = ctx.by,
        )
        return doc.copy(cast = doc.cast.copy(relations = doc.cast.relations + relation)) to relation
    }

    /**
     * A `사이` value with [kind]. For a pair kind, [holderOfName] holds the kind's first name
     * (`선배`) and the other end the pair name (`후배`).
     */
    fun betweenValue(relation: Relation, kind: RelationKind, holderOfName: Key?, description: String? = null): SlotValue {
        val pair = kind.category.known == RelationCategory.BETWEEN && kind.symmetry?.known == RelationSymmetry.PAIR
        val holder = if (!pair || holderOfName == null) null else when (RelationReader.sideOf(relation, holderOfName)) {
            Side.A -> PairHolder.A
            Side.B -> PairHolder.B
            null -> null
        }
        return SlotValue(kindKey = kind.key, description = description?.takeIf { it.isNotBlank() }, pairFirstHolder = holder?.open())
    }

    /** A mind value. */
    fun mindValue(kind: RelationKind?, description: String? = null): SlotValue =
        SlotValue(kindKey = kind?.key, description = description?.takeIf { it.isNotBlank() })

    fun encode(input: SlotInput): JsonElement? = when (input) {
        is SlotInput.Kind -> RelationReader.clean(input.value)?.let { ChangeRecord.encodeValue(SlotValue.serializer(), it) }
        is SlotInput.Address -> RelationReader.cleanAddress(input.text)?.let { ChangeRecord.encodeValue(String.serializer(), it) }
    }

    private fun checkSlot(slot: RelationSlotName, input: SlotInput) {
        require((input is SlotInput.Address) == RelationReader.isAddress(slot)) { "slot $slot takes ${if (RelationReader.isAddress(slot)) "a 호칭" else "a kind"}" }
    }

    /** Sets a base slot value (before any record). Only that slot changes. */
    fun setBase(doc: ProjectDoc, relationKey: Key, slot: RelationSlotName, input: SlotInput, ctx: EditContext): ProjectDoc {
        checkSlot(slot, input)
        return updateRelation(doc, relationKey, ctx) { r ->
            when (slot) {
                RelationSlotName.BETWEEN -> r.copy(between = RelationReader.clean((input as SlotInput.Kind).value))
                RelationSlotName.MIND_A -> r.copy(mindA = RelationReader.clean((input as SlotInput.Kind).value))
                RelationSlotName.MIND_B -> r.copy(mindB = RelationReader.clean((input as SlotInput.Kind).value))
                RelationSlotName.ADDRESS_A -> r.copy(addressA = RelationReader.cleanAddress((input as SlotInput.Address).text))
                RelationSlotName.ADDRESS_B -> r.copy(addressB = RelationReader.cleanAddress((input as SlotInput.Address).text))
            }
        }
    }

    /**
     * `기록` on the change sheet (CST-30) or `관계로 만들기`: one `◆` per chosen slot, all at [anchor]
     * (two minds on → two records, TC-13-08).
     */
    fun record(
        doc: ProjectDoc,
        relationKey: Key,
        values: Map<RelationSlotName, SlotInput>,
        anchor: Anchor,
        ctx: EditContext,
        note: String? = null,
    ): Pair<ProjectDoc, List<ChangeRecord>> {
        var current = doc
        val made = mutableListOf<ChangeRecord>()
        for ((slot, input) in values) {
            checkSlot(slot, input)
            val (next, record) = TimedValues.addRecord(current, RelationReader.target(relationKey, slot), encode(input), anchor, ctx, note)
            current = next
            made += record
        }
        return current to made
    }

    /** `이 값 고치기` on a slot with history. */
    fun correctInEffect(doc: ProjectDoc, relationKey: Key, slot: RelationSlotName, input: SlotInput, lens: LensPoint, ctx: EditContext): ProjectDoc {
        checkSlot(slot, input)
        return TimedValues.correctInEffect(doc, RelationReader.target(relationKey, slot), encode(input), lens, ctx) {
            setBase(it, relationKey, slot, input, ctx)
        }
    }

    /** `여기서부터 바뀜` on a slot. */
    fun changeFromHere(doc: ProjectDoc, relationKey: Key, slot: RelationSlotName, input: SlotInput, lens: LensPoint, ctx: EditContext): ProjectDoc {
        checkSlot(slot, input)
        return TimedValues.changeFromHere(doc, RelationReader.target(relationKey, slot), encode(input), lens, ctx, {
            setBase(it, relationKey, slot, input, ctx)
        })
    }

    /**
     * Sets [from]'s mind toward [to]. Only that side's slot is written; the other side stays empty
     * (TC-13-04). With [anchor] a `◆` is made, else the base value is set.
     */
    fun setMind(doc: ProjectDoc, from: Key, to: Key, value: SlotValue?, ctx: EditContext, anchor: Anchor? = null): ProjectDoc {
        val (withRelation, relation) = openOrCreate(doc, from, to, ctx)
        val slot = RelationReader.mindSlot(requireNotNull(RelationReader.sideOf(relation, from)))
        return if (anchor == null) setBase(withRelation, relation.key, slot, SlotInput.Kind(value), ctx)
        else record(withRelation, relation.key, mapOf(slot to SlotInput.Kind(value)), anchor, ctx).first
    }

    /**
     * Sets what [from] calls [to] (CST-31). A partner with no relation gets one with empty `사이` and
     * minds (TC-13-09); the map draws no line for it.
     */
    fun setAddress(doc: ProjectDoc, from: Key, to: Key, text: String?, ctx: EditContext, anchor: Anchor? = null): ProjectDoc {
        val (withRelation, relation) = openOrCreate(doc, from, to, ctx)
        val slot = RelationReader.addressSlot(requireNotNull(RelationReader.sideOf(relation, from)))
        return if (anchor == null) setBase(withRelation, relation.key, slot, SlotInput.Address(text), ctx)
        else record(withRelation, relation.key, mapOf(slot to SlotInput.Address(text)), anchor, ctx).first
    }

    /**
     * Edits a 호칭 chip (`말투` tab, TC-13-18): the value in effect at [lens] is corrected (the record
     * in effect, else the base). It is the same value the relation sheet and the script panel show.
     */
    fun editAddressChip(doc: ProjectDoc, from: Key, to: Key, text: String?, lens: LensPoint, ctx: EditContext): ProjectDoc {
        val (withRelation, relation) = openOrCreate(doc, from, to, ctx)
        val slot = RelationReader.addressSlot(requireNotNull(RelationReader.sideOf(relation, from)))
        return correctInEffect(withRelation, relation.key, slot, SlotInput.Address(text), lens, ctx)
    }

    /** Removes a relation row outright (merge of a mutual relation; purge). Its records are removed too. */
    fun removeRelation(doc: ProjectDoc, relationKey: Key): ProjectDoc {
        val ids = RelationReader.SLOTS.map { RelationReader.target(relationKey, it).targetId }.toSet()
        return doc.copy(
            cast = doc.cast.copy(relations = doc.cast.relations.filterNot { it.key == relationKey }),
            changes = doc.changes.copy(records = doc.changes.records.filterNot { it.target.targetId in ids }),
        )
    }

    internal fun updateRelation(doc: ProjectDoc, relationKey: Key, ctx: EditContext?, change: (Relation) -> Relation): ProjectDoc = doc.copy(
        cast = doc.cast.copy(relations = doc.cast.relations.map { r ->
            if (r.key != relationKey) r else change(r).let { if (ctx == null) it else it.copy(updatedAt = ctx.now, updatedBy = ctx.by) }
        }),
    )
}
