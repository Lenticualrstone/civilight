package io.github.lenticualrstone.civilight.feature.map.node

import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.NodeEdits
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.edit.MapEditViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import kotlinx.coroutines.Job

/**
 * 노드 지도 (WLD-50): the editor's writes (labels, layers, `보이는 시점`) plus nodes and connections
 * over `MapEdits.addNode` / `linkNodes` and `NodeEdits`. Each user action is one undo step with its
 * `되돌리기` toast (`노드 · 기록실`, `연결 · 회랑 → 기록실`).
 */
class NodeMapViewModel(projectKey: Key, mapKey: Key, env: MapEnv) : MapEditViewModel(projectKey, mapKey, env) {
    private fun nodeName(key: Key): String? = doc.value?.maps?.get(mapKey)?.nodes?.firstOrNull { it.key == key }?.name

    /** A new node at [at] with its name and optional sub line (`기록실` · `세라 · 1-3`). */
    fun addNode(name: String, sub: String?, at: Point, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        val clean = name.trim()
        return commitValue(MapCopy.step(MapCopy.NODE, clean), onDone) { d ->
            if (clean.isEmpty()) return@commitValue null
            (MapEdits.addNode(d, mapKey, clean, at, ctx, sub = sub?.trim()?.takeIf { it.isNotEmpty() }) as? MapEdit.Applied)
                ?.let { it.doc to it.value.key }
        }
    }

    /** A connection between two nodes; refused when they are already connected (either way). */
    fun link(a: Key, b: Key, onDone: (Key) -> Unit = {}): Job {
        val ctx = editContext()
        return commitValue(MapCopy.step(MapCopy.TOOL_LINK, NodeMapState.linkTitle(nodeName(a), nodeName(b))), onDone) { d ->
            if (a == b || NodeEdits.linked(d, mapKey, a, b)) return@commitValue null
            (MapEdits.linkNodes(d, mapKey, a, b, ctx) as? MapEdit.Applied)?.let { it.doc to it.value.key }
        }
    }

    fun moveNode(nodeKey: Key, to: Point): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.TOOL_MOVE, nodeName(nodeKey))) { d -> NodeEdits.moveNode(d, mapKey, nodeKey, to, ctx) }
    }

    /** Name and sub line of a node. */
    fun editNode(nodeKey: Key, name: String, sub: String?): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.NODE, name.trim())) { d -> NodeEdits.renameNode(d, mapKey, nodeKey, name, sub, ctx) }
    }

    /** The node's place link (`장소 | 결사의 문`), or none. */
    fun linkPlace(nodeKey: Key, placeKey: Key?): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.PLACE, nodeName(nodeKey))) { d -> NodeEdits.linkPlace(d, mapKey, nodeKey, placeKey, ctx) }
    }

    /** A connection's label (`계단 · 잠김`); blank clears it. */
    fun setLinkLabel(linkKey: Key, label: String): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.TOOL_LINK, label.trim().ifEmpty { null })) { d -> NodeEdits.setLinkLabel(d, mapKey, linkKey, label, ctx) }
    }
}
