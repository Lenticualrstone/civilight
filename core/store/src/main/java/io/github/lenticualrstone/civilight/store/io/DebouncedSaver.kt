package io.github.lenticualrstone.civilight.store.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Debounced saving (§8.2): [markDirty] (re)starts a [debounceMs] timer, and a [maxDelayMs] timer
 * started by the first unsaved change makes sure continuous editing still saves. [flush] saves at
 * once. Saves never overlap; [save] itself decides whether anything is left to write.
 */
class DebouncedSaver(
    private val scope: CoroutineScope,
    private val debounceMs: Long,
    private val maxDelayMs: Long,
    private val save: suspend () -> Unit,
) {
    private val lock = Any()
    private val saving = Mutex()
    private var debounceJob: Job? = null
    private var maxJob: Job? = null

    /** Schedules a save. Safe from any thread; never suspends. */
    fun markDirty() {
        synchronized(lock) {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(debounceMs)
                runSave()
            }
            if (maxJob == null) {
                maxJob = scope.launch {
                    delay(maxDelayMs)
                    runSave()
                }
            }
        }
    }

    /** Saves now and waits for it (and for a save already running). */
    suspend fun flush() = runSave()

    /** Drops pending timers without saving. */
    fun cancel() {
        synchronized(lock) {
            debounceJob?.cancel()
            maxJob?.cancel()
            debounceJob = null
            maxJob = null
        }
    }

    private suspend fun runSave() {
        val self = currentCoroutineContext()[Job]
        synchronized(lock) {
            debounceJob?.let { if (it !== self) it.cancel() }
            maxJob?.let { if (it !== self) it.cancel() }
            debounceJob = null
            maxJob = null
        }
        saving.withLock { save() }
    }
}
