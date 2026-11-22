package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.json.ClockTimeSerializer
import io.github.lenticualrstone.civilight.model.json.IntSpanSerializer
import io.github.lenticualrstone.civilight.model.json.IsoDateSerializer
import io.github.lenticualrstone.civilight.model.json.PointSerializer
import io.github.lenticualrstone.civilight.model.json.TimestampSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate

/** An instant as epoch milliseconds (UTC); ISO-8601 with `Z` in JSON (§1.5). */
typealias Timestamp = @Serializable(with = TimestampSerializer::class) Long

/** A calendar date (`"2026-11-22"` in JSON). */
typealias IsoDate = @Serializable(with = IsoDateSerializer::class) LocalDate

/**
 * A clock in a story day, minutes after 00:00 (`"HH:mm"` in JSON, §1.5). Scene clocks are stored in
 * the canonical 04:00-day form regardless of the project day boundary (STO-19), so 01:30 is still
 * `ClockTime(90)`; the day it belongs to is decided by the time engine.
 */
@Serializable(with = ClockTimeSerializer::class)
@JvmInline
value class ClockTime(val minuteOfDay: Int) : Comparable<ClockTime> {
    init {
        require(minuteOfDay in 0 until MINUTES_PER_DAY) { "clock out of range: $minuteOfDay" }
    }

    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60

    override fun compareTo(other: ClockTime): Int = minuteOfDay.compareTo(other.minuteOfDay)

    override fun toString(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    companion object {
        const val MINUTES_PER_DAY = 24 * 60

        fun of(hour: Int, minute: Int): ClockTime = ClockTime(hour * 60 + minute)

        fun parse(text: String): ClockTime = requireNotNull(parseOrNull(text)) { "not an HH:mm clock: $text" }

        fun parseOrNull(text: String): ClockTime? {
            val match = Regex("""^(\d{2}):(\d{2})$""").matchEntire(text) ?: return null
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h > 23 || m > 59) return null
            return of(h, m)
        }
    }
}

/** A point on a map base, both coordinates in [0, 1] (x right, y down); `[x, y]` in JSON. */
@Serializable(with = PointSerializer::class)
data class Point(val x: Float, val y: Float)

/** An inclusive integer pair, `[first, last]` in JSON (story-day and minute ranges). */
@Serializable(with = IntSpanSerializer::class)
data class IntSpan(val first: Int, val last: Int)

/** Anything that has the §1.3 extension slot. Unknown properties read from a file live in `ext["_unknown"]`. */
interface HasExt {
    val ext: JsonObject?
}

/** A persisted keyed entity with the §1.3 bookkeeping fields. */
interface Entity : HasExt {
    val key: Key
    val updatedAt: Long
    val updatedBy: String
}

/** An entity that can be directly trashed (§5.1: only the deleted row carries the mark). */
interface Trashable {
    val deletedAt: Long?
    val trashEntryKey: Key?

    /** True when this row itself carries a trash mark (it may still be non-live through an owner). */
    val isDirectlyTrashed: Boolean get() = deletedAt != null
}

/** An item in a manual order (§1.2). */
interface Ordered {
    val order: OrderKey
}
