package io.github.lenticualrstone.civilight.feature.map.node

import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.feature.map.edit.EditState
import io.github.lenticualrstone.civilight.feature.map.edit.ItemRowUi
import io.github.lenticualrstone.civilight.feature.map.edit.LayerRowUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.search.KoreanSearch

/** The card of what is selected on a node map. */
sealed interface NodePropsUi {
    val key: Key
    val title: String

    /** A node: name, sub line, place link (`장소 | 시장 입구`), `보이는 시점`. */
    data class NodeProps(
        override val key: Key,
        override val title: String,
        /** The sub line as shown (`없음` when unset) and as stored (for the edit sheet). */
        val sub: String,
        val subText: String?,
        val place: String,
        val placeKey: Key?,
        val reveal: String,
    ) : NodePropsUi

    /** A connection `시장 입구 → 오웬의 노점`: label and `보이는 시점`. */
    data class LinkProps(override val key: Key, override val title: String, val label: String, val labelText: String?, val reveal: String) : NodePropsUi

    data class LabelProps(override val key: Key, override val title: String, val reveal: String, val layer: String) : NodePropsUi
}

/** A search result of `장소 · 노드 찾기`: the node and its sub line or place. */
data class NodeHit(val key: Key, val title: String, val sub: String?)

/** 노드 지도 (WLD-50, board `MapNode`). */
data class NodeMapUi(
    /** `하층 구역 › 결사 본부`. */
    val small: String,
    val base: MapBase,
    val scene: MapScene,
    val props: NodePropsUi?,
    val layers: List<LayerRowUi>,
    val items: List<ItemRowUi>,
    /** `연결 · 시장 입구` while the second node is being chosen. */
    val linking: String?,
    /** The node being moved (`이동 · 시장 입구`). */
    val moving: String?,
)

object NodeMapState {
    fun build(doc: ProjectDoc, mapKey: Key, lens: LensPoint, hiddenOpen: Boolean, s: NodeEditorState): NodeMapUi? {
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        val scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.EDIT)
        fun nodeName(key: Key?) = key?.let { k -> scene.nodes.firstOrNull { it.key == k }?.name }
        val props = s.selected?.let { key ->
            map.nodes.firstOrNull { it.key == key }?.takeIf { n -> scene.nodes.any { it.key == n.key } }?.let { n ->
                NodePropsUi.NodeProps(
                    n.key, n.name, n.sub ?: MapCopy.NONE, n.sub,
                    n.placeKey?.let { doc.index.place(it)?.name } ?: MapCopy.NONE, n.placeKey,
                    EditState.revealLabel(doc, n.revealPoint),
                )
            } ?: map.nodeLinks.firstOrNull { it.key == key }?.takeIf { l -> scene.links.any { it.key == l.key } }?.let { l ->
                NodePropsUi.LinkProps(l.key, linkTitle(nodeName(l.aNodeKey), nodeName(l.bNodeKey)), l.label ?: MapCopy.NONE, l.label, EditState.revealLabel(doc, l.revealPoint))
            } ?: map.labels.firstOrNull { it.key == key }?.takeIf { l -> scene.labels.any { it.key == l.key } }?.let { l ->
                NodePropsUi.LabelProps(l.key, l.text, EditState.revealLabel(doc, l.revealPoint), EditState.layerLabel(map, l.layerKey))
            }
        }
        return NodeMapUi(
            small = MapCopy.crumbs(MapViews.breadcrumb(doc, map).map { it.text }),
            base = MapBase.Node,
            scene = scene,
            props = props,
            layers = EditState.layerRows(doc, map),
            items = scene.nodes.map { ItemRowUi(it.key, "${MapCopy.NODE}${MapCopy.SEP}${it.name}", it.key == s.selected) } +
                scene.links.map { l -> ItemRowUi(l.key, "${MapCopy.TOOL_LINK}${MapCopy.SEP}${l.label ?: linkTitle(scene, l.key)}", l.key == s.selected) } +
                scene.labels.map { ItemRowUi(it.key, "${MapCopy.LABEL}${MapCopy.SEP}${it.text}", it.key == s.selected) },
            linking = nodeName(s.linkFrom)?.let(MapCopy::linking),
            moving = nodeName(s.movingNode)?.let { "${MapCopy.TOOL_MOVE}${MapCopy.SEP}$it" },
        )
    }

    /** `회랑 → 기록실`. */
    fun linkTitle(a: String?, b: String?): String = "${a.orEmpty()} → ${b.orEmpty()}"

    private fun linkTitle(scene: MapScene, key: Key): String {
        val link = scene.links.firstOrNull { it.key == key } ?: return ""
        return linkTitle(scene.nodes.firstOrNull { it.key == link.aKey }?.name, scene.nodes.firstOrNull { it.key == link.bKey }?.name)
    }

    /** `장소 · 노드 찾기`: nodes whose name, sub line or linked place matches (초성 too). */
    fun search(doc: ProjectDoc, mapKey: Key, scene: MapScene, query: String): List<NodeHit> {
        val map = doc.maps[mapKey] ?: return emptyList()
        val drawn = scene.nodes.map { it.key }.toSet()
        return map.nodes.filter { it.key in drawn }.filter { n ->
            query.isBlank() || KoreanSearch.matchesAny(listOf(n.name, n.sub, n.placeKey?.let { doc.index.place(it)?.name }), query)
        }.map { n -> NodeHit(n.key, n.name, n.sub ?: n.placeKey?.let { doc.index.place(it)?.name }) }
    }
}
