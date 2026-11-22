package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.recordAt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.PairHolder
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.cast.RelationKind
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.serialization.builtins.serializer

/** An end of a relation. */
enum class Side {
    A, B;

    val other: Side get() = if (this == A) B else A
}

/** A relation read at one point: `사이`, each side's `마음` and `호칭` (rule 13 §2). */
data class RelationState(
    val relation: Relation,
    val between: SlotValue?,
    val mindA: SlotValue?,
    val mindB: SlotValue?,
    val addressA: String?,
    val addressB: String?,
    /** The record in effect per slot (absent = base value). */
    val records: Map<RelationSlotName, ChangeRecord>,
) {
    /** `만나기 전`: no slot has a value yet (never stored, not a kind). */
    val notMet: Boolean get() = between == null && mindA == null && mindB == null && addressA == null && addressB == null

    /** The map draws a line only for a `사이` or a mind; never for `만나기 전` or 호칭 only. */
    val drawsLine: Boolean get() = between != null || mindA != null || mindB != null

    fun mind(side: Side): SlotValue? = if (side == Side.A) mindA else mindB
    fun address(side: Side): String? = if (side == Side.A) addressA else addressB
}

/** Reading relations over story time (CST-28, CST-30, CST-31). */
object RelationReader {
    val SLOTS: List<RelationSlotName> = RelationSlotName.entries

    fun target(relationKey: Key, slot: RelationSlotName): ChangeTarget.RelationSlot = ChangeTarget.RelationSlot(relationKey, slot)

    fun characterRef(key: Key): EntityRef = EntityRef(RelationEndKind.CHARACTER, key)

    /** The relation of two characters (one per pair), live or not. */
    fun between(doc: ProjectDoc, a: Key, b: Key): Relation? = doc.index.relationBetween(characterRef(a), characterRef(b))

    /** Which end [characterKey] is, or null. */
    fun sideOf(relation: Relation, characterKey: Key): Side? = when {
        relation.endA.sameAs(characterRef(characterKey)) -> Side.A
        relation.endB.sameAs(characterRef(characterKey)) -> Side.B
        else -> null
    }

    fun endKey(relation: Relation, side: Side): Key = if (side == Side.A) relation.endA.key else relation.endB.key

    fun mindSlot(side: Side): RelationSlotName = if (side == Side.A) RelationSlotName.MIND_A else RelationSlotName.MIND_B

    /** 호칭 of [side] toward the other end. */
    fun addressSlot(side: Side): RelationSlotName = if (side == Side.A) RelationSlotName.ADDRESS_A else RelationSlotName.ADDRESS_B

    fun isAddress(slot: RelationSlotName): Boolean = slot == RelationSlotName.ADDRESS_A || slot == RelationSlotName.ADDRESS_B

    /** A slot value that holds nothing counts as empty. */
    fun clean(value: SlotValue?): SlotValue? =
        value?.takeIf { it.kindKey != null || !it.kindFallbackText.isNullOrBlank() || !it.description.isNullOrBlank() }

    fun cleanAddress(value: String?): String? = value?.takeIf { it.isNotBlank() }

    /** Decodes a slot record's value. */
    fun slotValueOf(record: ChangeRecord): SlotValue? = clean(record.valueAs(SlotValue.serializer()))

    fun addressOf(record: ChangeRecord): String? = cleanAddress(record.valueAs(String.serializer()))

    fun state(doc: ProjectDoc, relation: Relation, point: ReadPoint): RelationState {
        val records = HashMap<RelationSlotName, ChangeRecord>()
        fun slot(name: RelationSlotName): SlotValue? {
            val r = doc.recordAt(target(relation.key, name), point)
            if (r != null) records[name] = r
            return if (r != null) slotValueOf(r) else clean(relation.baseSlot(name))
        }
        fun address(name: RelationSlotName): String? {
            val r = doc.recordAt(target(relation.key, name), point)
            if (r != null) records[name] = r
            return if (r != null) addressOf(r) else cleanAddress(relation.baseAddress(name))
        }
        return RelationState(
            relation = relation,
            between = slot(RelationSlotName.BETWEEN),
            mindA = slot(RelationSlotName.MIND_A),
            mindB = slot(RelationSlotName.MIND_B),
            addressA = address(RelationSlotName.ADDRESS_A),
            addressB = address(RelationSlotName.ADDRESS_B),
            records = records,
        )
    }

    /** Live records of every slot of the relation. */
    fun records(doc: ProjectDoc, relation: Relation): List<ChangeRecord> {
        val ids = SLOTS.map { target(relation.key, it).targetId }.toSet()
        return doc.changes.records.filter { it.target.targetId in ids && doc.liveness.isLive(it) }
    }

    // --- Kind names ---

    fun kind(doc: ProjectDoc, value: SlotValue): RelationKind? = value.kindKey?.let { doc.index.relationKind(it) }

    /** The kind word of a slot (kind name, else the kept text of a deleted kind), or null. */
    fun kindName(doc: ProjectDoc, value: SlotValue): String? = kind(doc, value)?.name ?: value.kindFallbackText?.takeIf { it.isNotBlank() }

    /** The kind was deleted: show its text with `◇`. */
    fun isFallback(doc: ProjectDoc, value: SlotValue): Boolean = kind(doc, value) == null && !value.kindFallbackText.isNullOrBlank()

    /**
     * Role of [end] in a `사이`: a symmetric kind's name; for a pair kind the name held by that end
     * (`선배` for the holder of the first name, `후배` for the other end).
     */
    fun roleOf(doc: ProjectDoc, between: SlotValue, end: Side): String? {
        val k = kind(doc, between) ?: return kindName(doc, between)
        if (k.category.known != RelationCategory.BETWEEN || k.symmetry?.known != RelationSymmetry.PAIR) return k.name
        val holder = when (between.pairFirstHolder?.known) {
            PairHolder.A -> Side.A
            PairHolder.B -> Side.B
            null -> return k.name
        }
        return if (end == holder) k.name else k.pairName ?: k.name
    }

    /** One mind as text: `경계`, or a deleted kind's text, plus its description. */
    fun mindText(doc: ProjectDoc, value: SlotValue?): SlotText? = value?.let {
        SlotText(kindName(doc, it), it.description?.takeIf { d -> d.isNotBlank() }, isFallback(doc, it))
    }

    /** Words of the slots for an empty-state check. */
    fun describe(doc: ProjectDoc, state: RelationState): String =
        if (state.notMet) CastCopy.NOT_MET else listOfNotNull(state.between?.let { kindName(doc, it) }).joinToString()
}

/** A mind or `사이` as text. */
data class SlotText(val kind: String?, val description: String?, val fallback: Boolean) {
    /** The word shown after `→` / `←`: the kind, else the description. */
    val word: String get() = kind ?: description.orEmpty()

    /** `경계 · 불빛을 아는 낯선 사람 ◇` (deleted kind keeps both texts, CST Q16 default). */
    val full: String
        get() {
            val text = listOfNotNull(kind, description?.takeIf { it != kind }).joinToString(CastCopy.SEP)
            return if (fallback) "$text ${CastCopy.SOFT_MARK}" else text
        }
}
