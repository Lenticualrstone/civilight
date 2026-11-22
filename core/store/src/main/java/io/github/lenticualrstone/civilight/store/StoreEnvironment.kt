package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.Timestamp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * What every store shares: the [scope] its save timers run in, the [io] dispatcher for file work,
 * the [clock] and the [deviceId] written as `updatedBy`. Tests pass a test scope, a test
 * dispatcher and a [ManualClock].
 */
class StoreEnvironment(
    val scope: CoroutineScope,
    val io: CoroutineDispatcher,
    val clock: AppClock,
    val deviceId: DeviceId,
    val timing: StoreTiming = StoreTiming(),
) {
    /** `updatedAt` / `updatedBy` for an edit made now. */
    fun stamp(): EditStamp = EditStamp(clock.now(), deviceId.value)
}

/** Save timing and history limits (§8.2: about 600 ms debounce). */
data class StoreTiming(
    val saveDebounceMs: Long = 600,
    /** Continuous editing still saves at least this often. */
    val saveMaxDelayMs: Long = 3_000,
    /** Undo steps kept per open project. */
    val undoLimit: Int = 100,
    /** Edits with the same merge key closer than this form one undo step. */
    val undoMergeWindowMs: Long = 2_000,
    /** A blob younger than this is never garbage-collected (it may be mid-import or mid-crop). */
    val blobGraceMs: Long = 24L * 60 * 60 * 1000,
)

/** The bookkeeping pair of §1.3 for one edit. */
data class EditStamp(val at: Timestamp, val by: String)
