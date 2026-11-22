package io.github.lenticualrstone.civilight.feature.map.poly

import io.github.lenticualrstone.civilight.domain.world.map.PolyMaps
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.edit.MapEditViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.PolyProvince
import kotlinx.coroutines.Job

/**
 * 다각형 편집 (WLD-59) over `domain:world` `PolyMaps`; layers come from [MapEditViewModel]. Every
 * action is one undo step `지역 · <name>`.
 */
class PolyViewModel(projectKey: Key, mapKey: Key, env: MapEnv) : MapEditViewModel(projectKey, mapKey, env) {
    private fun name(d: ProjectDoc?, key: Key): String? = d?.maps?.get(mapKey)?.poly?.let { PolyUiBuilder.nameOf(it, key) }

    private fun label(key: Key): String = MapCopy.step(MapCopy.PROVINCE, name(doc.value, key))

    fun apply(commit: PolyCommit): Job {
        val ctx = editContext()
        return when (commit) {
            is PolyCommit.Assign -> commit(label(commit.provinceKey)) { d -> PolyMaps.assign(d, mapKey, listOf(commit.cell), commit.provinceKey, ctx) }
            is PolyCommit.Merge -> commit(label(commit.into)) { d -> PolyMaps.merge(d, mapKey, commit.from, commit.into, ctx) }
            is PolyCommit.ToggleLand -> commit(MapCopy.POLY_TOOL_LAND) { d -> PolyMaps.toggleLand(d, mapKey, commit.cell, ctx) }
        }
    }

    /** `새 지역`: `지역 N` in the next colour; [onDone] gets its key. */
    fun addProvince(onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        val poly = doc.value?.maps?.get(mapKey)?.poly
        val n = (poly?.provinces?.size ?: 0) + 1
        val nameText = MapCopy.defaultProvinceName(n)
        return commitValue(MapCopy.step(MapCopy.PROVINCE, nameText), onDone) { d ->
            val p = d.maps[mapKey]?.poly ?: return@commitValue null
            PolyMaps.addProvince(d, mapKey, nameText, PolyUiBuilder.nextColor(p), ctx)
        }
    }

    /** `나누기`: the chosen cells of [provinceKey] become `지역 N`. */
    fun split(provinceKey: Key, cells: List<Int>, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        val n = (doc.value?.maps?.get(mapKey)?.poly?.provinces?.size ?: 0) + 1
        val nameText = MapCopy.defaultProvinceName(n)
        return commitValue(MapCopy.step(MapCopy.PROVINCE, nameText), onDone) { d ->
            val p = d.maps[mapKey]?.poly ?: return@commitValue null
            PolyMaps.split(d, mapKey, provinceKey, cells, nameText, PolyUiBuilder.nextColor(p), ctx)
        }
    }

    fun updateProvince(provinceKey: Key, f: (PolyProvince) -> PolyProvince): Job {
        val ctx = editContext()
        return commit(label(provinceKey)) { d -> PolyMaps.updateProvince(d, mapKey, provinceKey, ctx, f) }
    }
}
