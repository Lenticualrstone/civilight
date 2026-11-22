package io.github.lenticualrstone.civilight.feature.map.gen

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.world.gen.GenFormats
import io.github.lenticualrstone.civilight.domain.world.gen.GenInput
import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.domain.world.gen.MapGenerator
import io.github.lenticualrstone.civilight.domain.world.gen.TerrainGenerator
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapParent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 지형 만들기 (WLD-53 … 55): previews are generated off the main thread and kept by input (the
 * generator is deterministic, so a candidate chosen again is the same world); `이 지도로 시작` builds
 * the map off the main thread and commits only its insertion, as one undo step `새 지도`.
 */
class MapGenerateViewModel(projectKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    private val cache = object : LinkedHashMap<GenInput, GeneratedWorld>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<GenInput, GeneratedWorld>?): Boolean = size > CACHE
    }

    private fun cached(input: GenInput): GeneratedWorld? = synchronized(cache) { cache[input] }

    /** The world of [input] (cached). */
    suspend fun world(input: GenInput): GeneratedWorld = cached(input) ?: withContext(Dispatchers.Default) {
        TerrainGenerator.generate(input)
    }.also { w -> synchronized(cache) { cache[input] = w } }

    /** The base a preview draws: hex cells for the hex format, else the generated world. */
    fun base(world: GeneratedWorld, format: MapFormat): MapBase =
        MapBase.Generated(world, if (format == MapFormat.HEX) runCatching { GenFormats.hex(world) }.getOrNull() else null)

    /** `이 지도로 시작`: [onDone] gets the new map's key. */
    fun start(state: GenFlowState, parent: MapParent?, onDone: (Key) -> Unit) {
        viewModelScope.launch {
            if (readOnly.value) {
                env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
                return@launch
            }
            val snapshot = doc.value ?: return@launch
            val ctx = editContext()
            val made = try {
                withContext(Dispatchers.Default) { MapGenerator.start(snapshot, state.session(), ctx, parent = parent) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            val map = (made as? MapEdit.Applied)?.value ?: return@launch
            val finished = if (state.contours && state.format != MapFormat.NODE) map.copy(contours = true) else map
            commitValue(MapCopy.STEP_NEW_MAP, onDone) { d ->
                val placed = finished.copy(order = OrderKeys.after(d.maps.values.maxOfOrNull { it.order }))
                d.copy(maps = d.maps + (placed.key to placed)) to placed.key
            }
        }
    }

    private companion object {
        const val CACHE = 16
    }
}
