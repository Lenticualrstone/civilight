package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The one set of injected sources of a compound action: clock, local zone, device id and key
 * source. The rule modules each have their own context type (`domain:story` / `domain:cast`
 * [EditContext], `domain:trash` [TrashContext], `domain:recorder` [RecorderEditContext]); the
 * converters below share the same values and the same key source, so one action never mints keys
 * or reads a clock twice in different ways.
 */
data class ActionContext(
    val now: Timestamp,
    val zone: ZoneId = ZoneId.of("Asia/Seoul"),
    val device: String = "",
    val newKey: () -> Key = Keys::newKey,
) {
    val today: LocalDate get() = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    /** `domain:story` and `domain:cast` edits. */
    val edit: EditContext get() = EditContext(newKey = newKey, now = now, device = device, zone = zone)

    /** `domain:trash` operations. */
    val trash: TrashContext get() = TrashContext(now = now, zone = zone, deviceId = device, newKey = newKey)

    /** (0.2.0) `domain:world` edits (create in place, U-15). */
    val world: io.github.lenticualrstone.civilight.domain.world.common.EditContext
        get() = io.github.lenticualrstone.civilight.domain.world.common.EditContext(newKey = newKey, now = { now }, device = device)

    /** `domain:recorder` edits. */
    val recorder: RecorderEditContext get() = RecorderEditContext(newKey = newKey, now = now, device = device, zone = zone)
}
