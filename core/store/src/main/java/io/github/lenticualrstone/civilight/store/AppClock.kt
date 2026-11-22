package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Wall clock of the stores: [now] stamps `updatedAt`, `deletedAt` and file times; [zone] turns an
 * instant into the local date used for trash purge dates (`purgeOn`, LIB-38) and block groups.
 */
interface AppClock {
    fun now(): Timestamp
    val zone: ZoneId

    /** The local date of [now]. */
    fun today(): LocalDate = localDateOf(now())

    /** The local date of [at] in [zone]. */
    fun localDateOf(at: Timestamp): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    /** The device clock in the device time zone. */
    object System : AppClock {
        override fun now(): Timestamp = java.lang.System.currentTimeMillis()
        override val zone: ZoneId get() = ZoneId.systemDefault()
    }
}

/** A clock that moves only when told to (tests, previews). */
class ManualClock(
    @Volatile private var millis: Timestamp,
    override val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) : AppClock {
    override fun now(): Timestamp = millis

    fun set(at: Timestamp) {
        millis = at
    }

    fun advance(byMillis: Long) {
        millis += byMillis
    }

    companion object {
        /** A clock at the ISO instant [text] (`"2026-11-22T00:00:00Z"`). */
        fun at(text: String, zone: ZoneId = ZoneId.of("Asia/Seoul")) = ManualClock(Instant.parse(text).toEpochMilli(), zone)
    }
}
