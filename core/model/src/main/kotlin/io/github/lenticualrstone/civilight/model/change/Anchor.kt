package io.github.lenticualrstone.civilight.model.change

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.json.TaggedUnionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Where a record starts (§2.4; JSON discriminator `kind`). Positions and comparison: §4.3 and
 * [io.github.lenticualrstone.civilight.model.position.ManuscriptIndex].
 */
@Serializable(with = AnchorSerializer::class)
sealed interface Anchor {
    /** From that block, inclusive (`1-7 #043부터`). */
    @Serializable
    data class AtBlock(val blockKey: Key, override val ext: JsonObject? = null) : Anchor, HasExt

    /** From before the unit's first block (any level). */
    @Serializable
    data class UnitStart(val unitKey: Key, override val ext: JsonObject? = null) : Anchor, HasExt

    /** After the unit's last block, including blocks added to that unit later. */
    @Serializable
    data class UnitEnd(val unitKey: Key, override val ext: JsonObject? = null) : Anchor, HasExt

    /** Before the story, at a world event (세계 사건 자리). */
    @Serializable
    data class WorldEvent(val eventKey: Key, override val ext: JsonObject? = null) : Anchor, HasExt

    /** No surviving block after a deletion (`ED`); sorts after everything. */
    @Serializable
    data object Unplaced : Anchor

    /** An anchor kind from a newer file; kept verbatim and sorted as [Unplaced] (`ED`). */
    data class Unknown(val raw: JsonObject) : Anchor {
        val kind: String? get() = (raw["kind"] as? JsonPrimitive)?.contentOrNull
    }

    /** Same kind and referent, ignoring extension data. */
    fun sameAs(other: Anchor?): Boolean = when (this) {
        is AtBlock -> other is AtBlock && other.blockKey == blockKey
        is UnitStart -> other is UnitStart && other.unitKey == unitKey
        is UnitEnd -> other is UnitEnd && other.unitKey == unitKey
        is WorldEvent -> other is WorldEvent && other.eventKey == eventKey
        Unplaced -> other == Unplaced
        is Unknown -> other is Unknown && other.raw == raw
    }
}

object AnchorSerializer : TaggedUnionSerializer<Anchor>(serialName = "Anchor", tagKey = "kind") {
    override val variants: List<Variant<out Anchor>> = listOf(
        Variant("block", Anchor.AtBlock::class, Anchor.AtBlock.serializer()),
        Variant("unitStart", Anchor.UnitStart::class, Anchor.UnitStart.serializer()),
        Variant("unitEnd", Anchor.UnitEnd::class, Anchor.UnitEnd.serializer()),
        Variant("worldEvent", Anchor.WorldEvent::class, Anchor.WorldEvent.serializer()),
        Variant("unplaced", Anchor.Unplaced::class, Anchor.Unplaced.serializer()),
    )

    override fun unknown(raw: JsonObject): Anchor = Anchor.Unknown(raw)
    override fun rawOf(value: Anchor): JsonObject? = (value as? Anchor.Unknown)?.raw
}
