package io.github.lenticualrstone.civilight.feature.map.text

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.world.gen.GenInput
import io.github.lenticualrstone.civilight.domain.world.gen.GenSession
import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.domain.world.gen.MapGenerator
import io.github.lenticualrstone.civilight.domain.world.gen.TerrainGenerator
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 글로 설명 (WLD-61): worlds are generated off the main thread and kept by input (deterministic);
 * `이 지도로 시작` stores the map with its condition list as one step `새 지도`.
 */
class TextMapViewModel(projectKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    private val cache = object : LinkedHashMap<GenInput, GeneratedWorld>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<GenInput, GeneratedWorld>?): Boolean = size > 8
    }

    suspend fun world(session: GenSession): GeneratedWorld {
        val input = session.input()
        synchronized(cache) { cache[input] }?.let { return it }
        return withContext(Dispatchers.Default) { TerrainGenerator.generate(input) }.also { w -> synchronized(cache) { cache[input] = w } }
    }

    fun start(session: GenSession, onDone: (Key) -> Unit) {
        viewModelScope.launch {
            if (readOnly.value) {
                env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
                return@launch
            }
            val snapshot = doc.value ?: return@launch
            val ctx = editContext()
            val made = try {
                withContext(Dispatchers.Default) { MapGenerator.start(snapshot, session, ctx) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            val map = (made as? MapEdit.Applied)?.value ?: return@launch
            commitValue(MapCopy.STEP_NEW_MAP, onDone) { d ->
                val placed = map.copy(order = OrderKeys.after(d.maps.values.maxOfOrNull { it.order }))
                d.copy(maps = d.maps + (placed.key to placed)) to placed.key
            }
        }
    }
}
