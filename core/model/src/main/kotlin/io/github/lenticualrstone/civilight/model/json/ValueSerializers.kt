package io.github.lenticualrstone.civilight.model.json

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.Point
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Epoch milliseconds in memory, ISO-8601 UTC with `Z` in JSON (§1.5). */
object TimestampSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("civilight.Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeString(format(value))

    override fun deserialize(decoder: Decoder): Long = parse(decoder.decodeString())

    fun format(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).toString()

    fun parse(text: String): Long = try {
        Instant.parse(text).toEpochMilli()
    } catch (e: DateTimeParseException) {
        try {
            OffsetDateTime.parse(text).toInstant().toEpochMilli()
        } catch (e2: DateTimeParseException) {
            throw SerializationException("not an ISO-8601 instant: $text", e2)
        }
    }
}

/** `LocalDate` as `"2026-11-22"` (§1.5). */
object IsoDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("civilight.IsoDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate {
        val text = decoder.decodeString()
        return try {
            LocalDate.parse(text)
        } catch (e: DateTimeParseException) {
            throw SerializationException("not an ISO date: $text", e)
        }
    }
}

/** A story-day clock as `"HH:mm"` (§1.5). */
object ClockTimeSerializer : KSerializer<ClockTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("civilight.ClockTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ClockTime) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): ClockTime {
        val text = decoder.decodeString()
        return ClockTime.parseOrNull(text) ?: throw SerializationException("not an HH:mm clock: $text")
    }
}

private val floatList = ListSerializer(Float.serializer())
private val intList = ListSerializer(Int.serializer())

/** A map point as `[x, y]` (§2.7). */
object PointSerializer : KSerializer<Point> {
    override val descriptor: SerialDescriptor = floatList.descriptor

    override fun serialize(encoder: Encoder, value: Point) =
        encoder.encodeSerializableValue(floatList, listOf(value.x, value.y))

    override fun deserialize(decoder: Decoder): Point {
        val list = decoder.decodeSerializableValue(floatList)
        if (list.size != 2) throw SerializationException("a point is [x, y], got $list")
        return Point(list[0], list[1])
    }
}

/** An integer pair as `[first, last]` (`dayRange`, `tRange`). */
object IntSpanSerializer : KSerializer<IntSpan> {
    override val descriptor: SerialDescriptor = intList.descriptor

    override fun serialize(encoder: Encoder, value: IntSpan) =
        encoder.encodeSerializableValue(intList, listOf(value.first, value.last))

    override fun deserialize(decoder: Decoder): IntSpan {
        val list = decoder.decodeSerializableValue(intList)
        if (list.size != 2) throw SerializationException("a range is [first, last], got $list")
        return IntSpan(list[0], list[1])
    }
}
