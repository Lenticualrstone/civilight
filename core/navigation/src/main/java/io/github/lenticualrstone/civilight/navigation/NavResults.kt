package io.github.lenticualrstone.civilight.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Results handed from one screen to the screen below it (e.g. [PickOnMap] → `씬 · 장소와 때`).
 * The caller picks a request key, pushes the picking route, and collects [results] for that key;
 * the picker calls [send] and pops. A result waits until [consume]d. Held in memory for the process.
 */
class NavResults {
    private val pending = MutableStateFlow<Map<String, Any>>(emptyMap())

    fun send(requestKey: String, value: Any) = pending.update { it + (requestKey to value) }

    /** Takes the waiting result for [requestKey], if any. */
    fun consume(requestKey: String): Any? {
        var taken: Any? = null
        pending.update { map ->
            taken = map[requestKey]
            map - requestKey
        }
        return taken
    }

    /** Emits whenever a result for [requestKey] is waiting (the collector then calls [consume]). */
    fun results(requestKey: String): Flow<Unit> = pending.filter { requestKey in it }.map { }
}
