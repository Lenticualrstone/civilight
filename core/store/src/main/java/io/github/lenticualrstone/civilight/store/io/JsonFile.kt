package io.github.lenticualrstone.civilight.store.io

import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.store.StoreEnvironment
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.io.File

/**
 * One small app-only JSON file (class A) as a [StateFlow] with debounced atomic saves. An
 * unreadable file reads as [default] and is set aside, never overwritten.
 */
class JsonFile<T>(
    private val directory: StoreDirectory,
    private val path: String,
    private val serializer: KSerializer<T>,
    private val default: T,
    private val env: StoreEnvironment,
) {
    private val _state = MutableStateFlow(default)
    private val loadLock = Mutex()
    private val editing = Mutex()
    @Volatile private var loaded = false
    private var written: Any? = UNSET
    private val saver = DebouncedSaver(env.scope, env.timing.saveDebounceMs, env.timing.saveMaxDelayMs, ::save)

    val state: StateFlow<T> = _state.asStateFlow()

    suspend fun ensureLoaded() {
        if (loaded) return
        loadLock.withLock {
            if (loaded) return
            val value = withContext(env.io) { read() }
            editing.withLock {
                _state.value = value
                loaded = true
            }
        }
    }

    suspend fun update(transform: (T) -> T): T {
        ensureLoaded()
        return editing.withLock {
            val next = transform(_state.value)
            if (next != _state.value) {
                _state.value = next
                saver.markDirty()
            }
            next
        }
    }

    suspend fun flush() = saver.flush()

    private suspend fun save() {
        if (!loaded) return
        val snapshot = editing.withLock { _state.value }
        if (snapshot == written) return
        withContext(env.io + NonCancellable) {
            directory.write(path, CivilightJson.encode(serializer, snapshot).toByteArray())
        }
        written = snapshot
    }

    private fun read(): T {
        val text = directory.readText(path)
        if (text == null) {
            written = default
            return default
        }
        return try {
            CivilightJson.decode(serializer, text).also { written = it }
        } catch (_: IllegalArgumentException) {
            val from = directory.file(path)
            AtomicFiles.moveAtomically(from, File(from.parentFile, "${from.name}.damaged-${env.clock.now()}"))
            default
        }
    }

    private companion object {
        val UNSET = Any()
    }
}
