package io.github.lenticualrstone.civilight.ui.project

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Shares one opened value per key between users: the first [acquire] opens it, every user calls
 * [release] once, and the value is closed [closeDelayMs] after the last release unless someone
 * acquires it again in between (moving between screens of one project never reopens it). A failed
 * open is forgotten, so the next [acquire] tries again. [scope] should use a `SupervisorJob`.
 */
class RefCountedSessions<K : Any, T : Any>(
    private val scope: CoroutineScope,
    private val open: suspend (K) -> T,
    private val close: suspend (K, T) -> Unit,
    private val closeDelayMs: Long = DEFAULT_CLOSE_DELAY_MS,
) {
    private class Slot<T>(val value: CompletableDeferred<T>) {
        @Volatile var users = 0
        var closing: Job? = null

        @Volatile var ready: T? = null
    }

    private val mutex = Mutex()
    private val slots = ConcurrentHashMap<K, Slot<T>>()

    /** Opens (or shares) the value of [key]. Throws what the open threw; the caller then must not [release]. */
    suspend fun acquire(key: K): T {
        var starter = false
        val slot = mutex.withLock {
            val existing = slots[key]
            val slot = existing ?: Slot<T>(CompletableDeferred()).also {
                slots[key] = it
                starter = true
            }
            slot.users++
            slot.closing?.cancel()
            slot.closing = null
            slot
        }
        if (starter) {
            scope.launch {
                runCatching { open(key) }
                    .onSuccess { slot.ready = it; slot.value.complete(it) }
                    .onFailure { slot.value.completeExceptionally(it) }
            }
        }
        return try {
            slot.value.await()
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                var toClose: T? = null
                mutex.withLock {
                    slot.users = (slot.users - 1).coerceAtLeast(0)
                    if (slots[key] === slot && slot.users <= 0) {
                        if (slot.value.isCompleted && slot.ready == null) {
                            // Open failed and nobody else is waiting: forget the failed slot.
                            slots.remove(key)
                        } else if (slot.ready != null) {
                            // Open succeeded just as the sole waiter cancelled: schedule normal close.
                            slots.remove(key)
                            toClose = slot.ready
                        }
                    }
                }
                toClose?.let { runCatching { close(key, it) } }
            }
            throw e
        }
    }

    /** One user of [key] is done with it. */
    fun release(key: K) {
        scope.launch {
            mutex.withLock {
                val slot = slots[key] ?: return@withLock
                slot.users--
                if (slot.users > 0) return@withLock
                slot.closing = scope.launch {
                    delay(closeDelayMs)
                    val closeIt = mutex.withLock {
                        if (slot.users <= 0 && slots[key] === slot) {
                            slots.remove(key)
                            true
                        } else {
                            false
                        }
                    }
                    if (closeIt) slot.ready?.let { close(key, it) }
                }
            }
        }
    }

    /** The value of [key] if it is open now (no waiting, no reference taken). */
    fun peek(key: K): T? = slots[key]?.ready

    /** Users currently holding [key] (tests, diagnostics). */
    fun users(key: K): Int = slots[key]?.users ?: 0

    /** Closes every value nobody holds, at once (going to the background keeps used ones). */
    suspend fun closeIdle() {
        val idle = mutex.withLock {
            val keys = slots.filterValues { it.users <= 0 }.keys.toList()
            keys.mapNotNull { k -> slots.remove(k)?.let { s -> s.closing?.cancel(); s.ready?.let { k to it } } }
        }
        idle.forEach { (k, v) -> close(k, v) }
    }

    companion object {
        const val DEFAULT_CLOSE_DELAY_MS = 15_000L
    }
}
