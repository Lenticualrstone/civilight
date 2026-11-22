package io.github.lenticualrstone.civilight.domain.world.common

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys

/**
 * What every world edit needs from outside: new keys, the clock and the device id written to
 * `updatedBy`. Tests pass deterministic ones.
 */
data class EditContext(
    val newKey: () -> Key = Keys::newKey,
    val now: () -> Long = System::currentTimeMillis,
    val device: String = "",
)

/** Separators and marks used in world copy (never an em dash). */
object Copy {
    const val SEP = " · "
    const val MINUS = "−"
}
