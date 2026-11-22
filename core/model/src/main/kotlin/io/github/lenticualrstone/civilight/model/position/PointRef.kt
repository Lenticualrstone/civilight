package io.github.lenticualrstone.civilight.model.position

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A reveal or unlock point (§4.6): `unitStart(u)` (map `보이는 시점` `X부터`, event reveal) or
 * `unitEnd(u)` (archive `X 이후 열림`). An item is visible at lens P iff `pos(point) ≤ pos(P)`. A point
 * whose unit is not live resolves to the start of the next live unit (or +∞) without being
 * rewritten; an unknown kind never becomes visible (`ED`).
 */
@Serializable
data class PointRef(
    val kind: OpenEnum<PointRefKind>,
    val unitKey: Key,
    override val ext: JsonObject? = null,
) : HasExt {
    constructor(kind: PointRefKind, unitKey: Key) : this(kind.open(), unitKey)

    companion object {
        fun unitStart(unitKey: Key) = PointRef(PointRefKind.UNIT_START, unitKey)
        fun unitEnd(unitKey: Key) = PointRef(PointRefKind.UNIT_END, unitKey)
    }
}

enum class PointRefKind { UNIT_START, UNIT_END }
