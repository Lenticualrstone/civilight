package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.UlidGenerator
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Everything a trash operation needs from the outside world: the clock, the local zone (trash days
 * and block groups are local dates), the device id written to `updatedBy`, and a key source. No
 * function of this module reads the system clock or makes keys on its own.
 */
data class TrashContext(
    val now: Timestamp,
    val zone: ZoneId,
    val deviceId: String = "",
    val newKey: () -> Key,
) {
    /** The local date of [now]. */
    val today: LocalDate get() = localDate(now)

    fun localDate(at: Timestamp): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    /**
     * `<d>일 남음` (LIB-38): `30 − calendar days between the deletion date and today`, local zone.
     * Deleted today → 30; 0 or less → due for the daily purge.
     */
    fun daysLeft(deletedAt: Timestamp): Int =
        (TrashEntry.RETENTION_DAYS - ChronoUnit.DAYS.between(localDate(deletedAt), today)).toInt()

    /** True when [entry] is due for the automatic purge (`d ≤ 0`, i.e. today ≥ `purgeOn`). */
    fun isDue(entry: TrashEntry): Boolean = !today.isBefore(entry.purgeOn)

    companion object {
        /** A context with keys from a fresh [UlidGenerator] on the same clock value. */
        fun at(now: Timestamp, zone: ZoneId, deviceId: String = ""): TrashContext {
            val generator = UlidGenerator(clock = { now })
            return TrashContext(now, zone, deviceId, generator::next)
        }
    }
}
