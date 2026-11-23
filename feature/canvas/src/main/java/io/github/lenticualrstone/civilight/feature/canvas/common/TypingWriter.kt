package io.github.lenticualrstone.civilight.feature.canvas.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Writes a field typed into to the document only after typing pauses, instead of on every keystroke. The field
 * stays the source of truth while typing; the store write (a new document, its index and every screen that
 * watches it) runs once per pause, so a fast burst of keys is never interleaved with document writes on the
 * main thread. [flush] writes the pending value at once (the sheet closes, another sheet is stacked over it).
 * Each write still goes through the view model's merged undo step, so a field edit stays one undo step.
 *
 * While the field holds focus ([hold] … [release]) nothing is written on a pause at all: a pause timer counts
 * wall time, so a main thread busy for a moment (a slow frame, a garbage collection) looks like a pause, the
 * write it starts makes the next keys slower still, and on a long burst the system drops the keys that waited
 * too long (the tail of the burst was lost that way). Holding writes the value when focus leaves, when the
 * sheet closes ([flush]) or when the app goes to the background ([flush]).
 */
class TypingWriter<T>(
    private val scope: CoroutineScope,
    private val quietMillis: Long = QUIET_MILLIS,
    private val write: (T) -> Unit,
) {
    private var pending: Pending<T>? = null
    private var latest: Pending<T>? = null
    private var job: Job? = null
    private var held = false

    /** True while a typed value waits for its write. */
    val hasPending: Boolean get() = pending != null

    /** True between [hold] and [release]: typed values wait instead of being written on a pause. */
    val isHeld: Boolean get() = held

    /** A new value from the field: replaces any waiting one and restarts the pause (no pause while held). */
    fun offer(value: T) {
        val p = Pending(value)
        pending = p
        latest = p
        job?.cancel()
        job = null
        if (held) return
        job = scope.launch {
            delay(quietMillis)
            job = null
            flush()
        }
    }

    /**
     * Offers [value] unless it is already the value offered last; before any offer, unless [isStart] (the
     * field still shows what it opened with). For a field whose changes reach [offer] a frame late, so a
     * close or focus change right after the last key still writes that key.
     */
    fun catchUp(value: T, isStart: Boolean) {
        val last = latest
        val same = if (last == null) isStart else last.value == value
        if (!same) offer(value)
    }

    /** The field took focus: from now on typed values wait for [release] or [flush], not for a pause. */
    fun hold() {
        held = true
        job?.cancel()
        job = null
    }

    /** The field lost focus: writes what waits at once and goes back to writing on a pause. */
    fun release() {
        held = false
        flush()
    }

    /** Writes the waiting value now (nothing when nothing waits). */
    fun flush() {
        job?.cancel()
        job = null
        val p = pending ?: return
        pending = null
        write(p.value)
    }

    private class Pending<T>(val value: T)

    companion object {
        /** The pause after the last key that counts as "typing stopped" (not a motion duration). */
        const val QUIET_MILLIS: Long = 450L
    }
}
