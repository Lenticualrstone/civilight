package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Inputs every edit needs besides the doc: the current time, the device's zone (trash days,
 * `11월 20일`), the device id written to `updatedBy`, and the key source.
 */
data class EditContext(
    val now: Timestamp,
    val zone: ZoneId,
    val device: String = "",
    val newKey: () -> Key = Keys::newKey,
) {
    /** The local date of [now]. */
    val today: LocalDate get() = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
}
