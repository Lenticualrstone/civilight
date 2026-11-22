package io.github.lenticualrstone.civilight.model.change

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.json.TaggedUnionSerializer
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * What a [ChangeRecord] changes (§2.4; JSON discriminator `kind`). Value schemas: `field` → a
 * [io.github.lenticualrstone.civilight.model.cast.FieldValue]; `displayName` → String or null;
 * `relationSlot` → [io.github.lenticualrstone.civilight.model.cast.SlotValue] (between, minds) or
 * String (addresses); `regionShape` → [RegionShapeValue]; `termReveal` → none.
 */
@Serializable(with = ChangeTargetSerializer::class)
sealed interface ChangeTarget {
    /**
     * Identity of the target for grouping records (`field:character:<key>:<fieldDefKey>` …); null
     * for an unknown kind, which `valueAt` ignores (§8.3).
     */
    val targetId: String?

    @Serializable
    data class Field(
        val entityKind: OpenEnum<EntityKind>,
        val entityKey: Key,
        val fieldDefKey: Key,
        override val ext: JsonObject? = null,
    ) : ChangeTarget, HasExt {
        constructor(entityKind: EntityKind, entityKey: Key, fieldDefKey: Key) : this(entityKind.open(), entityKey, fieldDefKey)

        override val targetId: String get() = "field:${entityKind.raw}:$entityKey:$fieldDefKey"
    }

    @Serializable
    data class DisplayName(
        val characterKey: Key,
        override val ext: JsonObject? = null,
    ) : ChangeTarget, HasExt {
        override val targetId: String get() = "displayName:$characterKey"
    }

    @Serializable
    data class RelationSlot(
        val relationKey: Key,
        val slot: OpenEnum<RelationSlotName>,
        override val ext: JsonObject? = null,
    ) : ChangeTarget, HasExt {
        constructor(relationKey: Key, slot: RelationSlotName) : this(relationKey, slot.open())

        override val targetId: String get() = "relationSlot:$relationKey:${slot.raw}"
    }

    @Serializable
    data class RegionShape(
        val mapKey: Key,
        val regionKey: Key,
        override val ext: JsonObject? = null,
    ) : ChangeTarget, HasExt {
        override val targetId: String get() = "regionShape:$mapKey:$regionKey"
    }

    @Serializable
    data class TermReveal(
        val termKey: Key,
        override val ext: JsonObject? = null,
    ) : ChangeTarget, HasExt {
        override val targetId: String get() = "termReveal:$termKey"
    }

    /** A target kind from a newer file; kept and ignored by `valueAt`. */
    data class Unknown(val raw: JsonObject) : ChangeTarget {
        val kind: String? get() = (raw["kind"] as? JsonPrimitive)?.contentOrNull
        override val targetId: String? get() = null
    }

    /** Same target, ignoring extension data. */
    fun sameAs(other: ChangeTarget): Boolean = targetId != null && targetId == other.targetId
}

/** Relation slots (CST-29/31): `사이`, each side's `마음`, each side's `호칭`. */
enum class RelationSlotName { BETWEEN, MIND_A, MIND_B, ADDRESS_A, ADDRESS_B }

object ChangeTargetSerializer : TaggedUnionSerializer<ChangeTarget>(serialName = "ChangeTarget", tagKey = "kind") {
    override val variants: List<Variant<out ChangeTarget>> = listOf(
        Variant("field", ChangeTarget.Field::class, ChangeTarget.Field.serializer()),
        Variant("displayName", ChangeTarget.DisplayName::class, ChangeTarget.DisplayName.serializer()),
        Variant("relationSlot", ChangeTarget.RelationSlot::class, ChangeTarget.RelationSlot.serializer()),
        Variant("regionShape", ChangeTarget.RegionShape::class, ChangeTarget.RegionShape.serializer()),
        Variant("termReveal", ChangeTarget.TermReveal::class, ChangeTarget.TermReveal.serializer()),
    )

    override fun unknown(raw: JsonObject): ChangeTarget = ChangeTarget.Unknown(raw)
    override fun rawOf(value: ChangeTarget): JsonObject? = (value as? ChangeTarget.Unknown)?.raw
}

/** Value of a `regionShape` record: `{ vertices: [[x, y], …] }` (WLD-65). */
@Serializable
data class RegionShapeValue(
    val vertices: List<io.github.lenticualrstone.civilight.model.Point>,
    override val ext: JsonObject? = null,
) : HasExt
