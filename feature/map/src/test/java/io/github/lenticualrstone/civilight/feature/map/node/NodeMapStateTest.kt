package io.github.lenticualrstone.civilight.feature.map.node

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-50 노드 지도 on the example's 잿더미 시장 (3 nodes, 2 links; board `MapNode`). */
class NodeMapStateTest {
    private val doc = MapFx.doc()
    private val map = doc.maps.getValue(K.MAP_ASH_MARKET)

    private fun build(s: NodeEditorState = NodeEditorState()) = NodeMapState.build(doc, K.MAP_ASH_MARKET, LensPoint.Latest, false, s)!!

    @Test
    fun `header breadcrumb, nodes and connections`() {
        val ui = build()
        assertEquals("세계 지도 › 하층 구역 지도 › 잿더미 시장", ui.small)
        assertEquals(3, ui.scene.nodes.size)
        assertEquals(2, ui.scene.links.size)
        assertTrue(ui.scene.links.all { l -> l.aKey.isNotEmpty() && l.bKey.isNotEmpty() })
        assertNull(ui.props)
        assertEquals(5, ui.items.size)
        assertTrue(ui.items.first().text.startsWith("노드 · "))
    }

    @Test
    fun `a selected node shows its card`() {
        val node = map.nodes.first()
        val props = build(NodeEditorState(selected = node.key)).props as NodePropsUi.NodeProps
        assertEquals(node.name, props.title)
        assertEquals(node.sub ?: "없음", props.sub)
        assertEquals(node.sub, props.subText)
        assertEquals(node.placeKey, props.placeKey)
    }

    @Test
    fun `a selected connection reads 'a → b'`() {
        val link = map.nodeLinks.first()
        val a = map.nodes.first { it.key == link.aNodeKey }.name
        val b = map.nodes.first { it.key == link.bNodeKey }.name
        val props = build(NodeEditorState(selected = link.key)).props as NodePropsUi.LinkProps
        assertEquals("$a → $b", props.title)
        assertEquals(link.label ?: "없음", props.label)
    }

    @Test
    fun `the step bars name the node`() {
        val node = map.nodes.first()
        assertEquals("연결 · ${node.name}", build(NodeEditorState(linkFrom = node.key)).linking)
        assertEquals("이동 · ${node.name}", build(NodeEditorState(movingNode = node.key)).moving)
    }

    @Test
    fun `장소 · 노드 찾기 matches names and 초성`() {
        val scene = build().scene
        val node = map.nodes.first()
        assertEquals(3, NodeMapState.search(doc, K.MAP_ASH_MARKET, scene, "").size)
        assertTrue(NodeMapState.search(doc, K.MAP_ASH_MARKET, scene, node.name).any { it.key == node.key })
        assertTrue(NodeMapState.search(doc, K.MAP_ASH_MARKET, scene, "없는이름").isEmpty())
    }
}
