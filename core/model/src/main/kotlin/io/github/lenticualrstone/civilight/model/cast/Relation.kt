@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.cast

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A relation between two characters (or, storage-only in v1, factions) (§2.5,
 * `cast.json › relations`). One relation per unordered pair ([pairId]). Base slot values hold before
 * the first record; an empty slot reads `만나기 전` (derived).
 */
@Serializable
data class Relation(
    override val key: Key,
    val endA: EntityRef,
    val endB: EntityRef,
    val between: SlotValue? = null,
    val mindA: SlotValue? = null,
    val mindB: SlotValue? = null,
    /** 호칭 A → B. */
    val addressA: String? = null,
    /** 호칭 B → A. */
    val addressB: String? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity {
    /** The unique-pair index value: `min:max` of `kind:key` of both ends. */
    val pairId: String get() = pairIdOf(endA, endB)

    /** True when [ref] is one of the ends. */
    fun involves(ref: EntityRef): Boolean = endA.sameAs(ref) || endB.sameAs(ref)

    /** The other end, or null if [ref] is not an end. */
    fun otherEnd(ref: EntityRef): EntityRef? = when {
        endA.sameAs(ref) -> endB
        endB.sameAs(ref) -> endA
        else -> null
    }

    /** Base value of a between/mind slot. */
    fun baseSlot(slot: RelationSlotName): SlotValue? = when (slot) {
        RelationSlotName.BETWEEN -> between
        RelationSlotName.MIND_A -> mindA
        RelationSlotName.MIND_B -> mindB
        RelationSlotName.ADDRESS_A, RelationSlotName.ADDRESS_B -> null
    }

    /** Base value of an address slot. */
    fun baseAddress(slot: RelationSlotName): String? = when (slot) {
        RelationSlotName.ADDRESS_A -> addressA
        RelationSlotName.ADDRESS_B -> addressB
        else -> null
    }

    companion object {
        fun pairIdOf(a: EntityRef, b: EntityRef): String {
            val x = a.id
            val y = b.id
            return if (x <= y) "$x|$y" else "$y|$x"
        }
    }
}

/** A relation end: `{ kind: character · faction, key }`. */
@Serializable
data class EntityRef(
    val kind: OpenEnum<RelationEndKind>,
    val key: Key,
    override val ext: JsonObject? = null,
) : HasExt {
    constructor(kind: RelationEndKind, key: Key) : this(kind.open(), key)

    /** `kind:key`, the comparison form of the unique-pair index. */
    val id: String get() = "${kind.raw}:$key"

    fun sameAs(other: EntityRef): Boolean = other.kind == kind && other.key == key
}

enum class RelationEndKind { CHARACTER, FACTION }

/** Value of a between or mind slot. */
@Serializable
data class SlotValue(
    val kindKey: Key? = null,
    val description: String? = null,
    /** Between + pair kind only: which end holds the kind's first name (세라 holds 선배). */
    val pairFirstHolder: OpenEnum<PairHolder>? = null,
    /** Kind name kept after its relation kind was deleted. */
    val kindFallbackText: String? = null,
    override val ext: JsonObject? = null,
) : HasExt

enum class PairHolder { A, B }

/** A relation kind (`project.json › relationKinds`); seed list = rule 13 §3. */
@Serializable
data class RelationKind(
    override val key: Key,
    val category: OpenEnum<RelationCategory>,
    /** Between only. */
    val symmetry: OpenEnum<RelationSymmetry>? = null,
    val name: String,
    /** Pair kinds: the other side's name (`후배` for `선배`). */
    val pairName: String? = null,
    override val order: OrderKey,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered

enum class RelationCategory { BETWEEN, MIND }

enum class RelationSymmetry { SYMMETRIC, PAIR }
