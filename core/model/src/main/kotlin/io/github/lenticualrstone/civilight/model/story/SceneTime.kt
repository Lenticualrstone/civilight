@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.story

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.json.TaggedUnionSerializer
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A scene's stored 때 (STO-21; JSON discriminator `type`). Resolved time, `N일째`, labels and time
 * order are derived by the time engine. [clock] is stored in the canonical 04:00-day form.
 */
@Serializable(with = SceneTimeSerializer::class)
sealed interface SceneTime {
    /** No 때. */
    @Serializable
    data object None : SceneTime

    /** Base scene + interval in story days (`dayMin == dayMax` when exact; may be negative). */
    @Serializable
    data class Relative(
        val baseKey: Key,
        val dayMin: Int,
        val dayMax: Int,
        @EncodeDefault val precision: OpenEnum<TimePrecision> = TimePrecision.EXACT.open(),
        val band: OpenEnum<Band>? = null,
        val clock: ClockTime? = null,
        /** Scene this was rebased away from (STO-10). */
        val rebasedFromKey: Key? = null,
        override val ext: JsonObject? = null,
    ) : SceneTime, HasExt

    /** A fixed point: a story day or a calendar date. */
    @Serializable
    data class Direct(
        val storyDay: Int? = null,
        val calendarDate: CalendarDate? = null,
        val band: OpenEnum<Band>? = null,
        val clock: ClockTime? = null,
        override val ext: JsonObject? = null,
    ) : SceneTime, HasExt

    /** 회상; out of the time chain. */
    @Serializable
    data class Flashback(
        val mode: OpenEnum<FlashbackMode>,
        val eventKey: Key? = null,
        val relation: OpenEnum<FlashbackRelation>? = null,
        /** Placeholder until STO Q20 decides the 전 · 후 amount. */
        val amount: FlashbackAmount? = null,
        val year: Int? = null,
        val calendarDate: CalendarDate? = null,
        val band: OpenEnum<Band>? = null,
        val clock: ClockTime? = null,
        override val ext: JsonObject? = null,
    ) : SceneTime, HasExt

    /** A variant from a newer file; kept verbatim and treated as no 때. */
    data class Unknown(val raw: JsonObject) : SceneTime {
        val type: String? get() = (raw["type"] as? JsonPrimitive)?.contentOrNull
    }
}

enum class TimePrecision { EXACT, APPROX }

enum class FlashbackMode { ANCHOR, DIRECT }

enum class FlashbackRelation { BEFORE, ON, AFTER }

/** A date in the world calendar relative to the story start year. */
@Serializable
data class CalendarDate(
    val month: Int,
    val day: Int,
    @EncodeDefault val yearOffset: Int = 0,
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class FlashbackAmount(
    val days: Int,
    override val ext: JsonObject? = null,
) : HasExt

object SceneTimeSerializer : TaggedUnionSerializer<SceneTime>(serialName = "SceneTime", tagKey = "type") {
    override val variants: List<Variant<out SceneTime>> = listOf(
        Variant("none", SceneTime.None::class, SceneTime.None.serializer()),
        Variant("relative", SceneTime.Relative::class, SceneTime.Relative.serializer()),
        Variant("direct", SceneTime.Direct::class, SceneTime.Direct.serializer()),
        Variant("flashback", SceneTime.Flashback::class, SceneTime.Flashback.serializer()),
    )

    override fun unknown(raw: JsonObject): SceneTime = SceneTime.Unknown(raw)
    override fun rawOf(value: SceneTime): JsonObject? = (value as? SceneTime.Unknown)?.raw
}
