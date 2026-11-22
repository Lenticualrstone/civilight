package io.github.lenticualrstone.civilight.model.position

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.json.TaggedUnionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A story position (§4.3). Positions are compared through ranks of a [ManuscriptIndex]; a link's
 * position is its block's (choice options count as their block, transition runs as [Transition]).
 */
sealed interface Position {
    /** Before everything under the unit. */
    data class Start(val unitKey: Key) : Position

    /** A scene's transition text. */
    data class Transition(val sceneKey: Key) : Position

    /** A block. */
    data class AtBlock(val blockKey: Key) : Position

    /** After everything under the unit, including blocks added to it later. */
    data class End(val unitKey: Key) : Position

    /** A world event, before the story. */
    data class Event(val eventKey: Key) : Position

    /** After everything. */
    data object Unplaced : Position
}

/**
 * A lens point (§4.4): `최신` or the end of an episode or scene. Stored in app-only `LensState`
 * (`{kind:"latest"}` / `{kind:"unitEnd", unitKey}`).
 */
@Serializable(with = LensPointSerializer::class)
sealed interface LensPoint {
    /** The 집필 선, or +∞ when nothing is written. */
    @Serializable
    data object Latest : LensPoint

    @Serializable
    data class UnitEnd(val unitKey: Key) : LensPoint
}

object LensPointSerializer : TaggedUnionSerializer<LensPoint>(serialName = "LensPoint", tagKey = "kind", defaultTag = "latest") {
    override val variants: List<Variant<out LensPoint>> = listOf(
        Variant("latest", LensPoint.Latest::class, LensPoint.Latest.serializer()),
        Variant("unitEnd", LensPoint.UnitEnd::class, LensPoint.UnitEnd.serializer()),
    )

    override fun unknown(raw: JsonObject): LensPoint = LensPoint.Latest
    override fun rawOf(value: LensPoint): JsonObject? = null
}

/**
 * A position rank (§4.3): unit starts, transitions, blocks and unit ends from 0 upward in manuscript
 * order; world events negative (`−1 − index`, latest date first); [UNPLACED] = +∞.
 */
@JvmInline
value class Rank(val value: Long) : Comparable<Rank> {
    override fun compareTo(other: Rank): Int = value.compareTo(other.value)

    val isUnplaced: Boolean get() = value == Long.MAX_VALUE

    override fun toString(): String = if (isUnplaced) "+∞" else value.toString()

    companion object {
        val UNPLACED = Rank(Long.MAX_VALUE)
    }
}
