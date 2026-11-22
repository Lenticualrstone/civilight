package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Injected sources for edits that mint keys or stamp rows (`updatedAt`, `updatedBy`, `createdAt`);
 * [zone] gives local dates (trash purge day).
 */
data class EditContext(
    val newKey: () -> Key = Keys::newKey,
    val now: Timestamp,
    val device: String,
    val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) {
    val today: LocalDate get() = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
}
