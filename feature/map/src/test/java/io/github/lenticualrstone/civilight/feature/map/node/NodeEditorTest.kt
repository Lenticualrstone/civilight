package io.github.lenticualrstone.civilight.feature.map.node

import io.github.lenticualrstone.civilight.feature.map.canvas.MapHit
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneLink
import io.github.lenticualrstone.civilight.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** WLD-50 노드 지도: the tool bar `이동 · 노드 · 연결 · 라벨 · 레이어`; only the chosen tool acts. */
class NodeEditorTest {
    private val p = Point(0.5f, 0.5f)

    @Test
    fun `노드 on an empty spot asks for a name there`() {
        val out = NodeEditor.tap(NodeEditor.chooseTool(NodeEditorState(), NodeTool.NODE), p, null, null)
        assertEquals(NodeEffect.NEW_NODE, out.effect)
        assertEquals(p, out.state.pendingAt)
        val placed = NodeEditor.placed(out.state, "n1")
        assertNull(placed.pendingAt)
        assertEquals("n1", placed.selected)
    }

    @Test
    fun `노드 on a node selects it instead`() {
        val out = NodeEditor.tap(NodeEditor.chooseTool(NodeEditorState(), NodeTool.NODE), p, MapHit.Node("n1"), null)
        assertNull(out.effect)
        assertEquals("n1", out.state.selected)
    }

    @Test
    fun `연결 is two taps on two nodes`() {
        var s = NodeEditor.chooseTool(NodeEditorState(), NodeTool.LINK)
        s = NodeEditor.tap(s, p, null, null).state
        assertNull(s.linkFrom)
        s = NodeEditor.tap(s, p, MapHit.Node("a"), null).state
        assertEquals("a", s.linkFrom)
        val out = NodeEditor.tap(s, p, MapHit.Node("b"), null)
        assertEquals(NodeCommit.Link("a", "b"), out.commit)
        assertNull(out.state.linkFrom)
    }

    @Test
    fun `the same node twice cancels the connection`() {
        val s = NodeEditor.tap(NodeEditor.chooseTool(NodeEditorState(), NodeTool.LINK), p, MapHit.Node("a"), null).state
        val out = NodeEditor.tap(s, p, MapHit.Node("a"), null)
        assertNull(out.commit)
        assertNull(out.state.linkFrom)
    }

    @Test
    fun `연결 on a node card starts a connection from it`() {
        val s = NodeEditor.startLink(NodeEditorState(selected = "a"), "a")
        assertEquals(NodeTool.LINK, s.tool)
        assertEquals("a", s.linkFrom)
        assertEquals(NodeCommit.Link("a", "c"), NodeEditor.tap(s, p, MapHit.Node("c"), null).commit)
    }

    @Test
    fun `이동 on a card, then a tap places the node`() {
        val s = NodeEditor.startMove(NodeEditorState(), "a")
        val out = NodeEditor.tap(s, Point(0.1f, 0.2f), MapHit.Node("b"), null)
        assertEquals(NodeCommit.MoveNode("a", Point(0.1f, 0.2f)), out.commit)
        assertNull(out.state.movingNode)
        assertEquals("a", out.state.selected)
        assertNull(NodeEditor.cancelMove(s).movingNode)
    }

    @Test
    fun `이동 selects nodes, labels and connections`() {
        val s = NodeEditorState(panelTab = 2)
        assertEquals("l1", NodeEditor.tap(s, p, null, "l1").state.selected)
        assertEquals(0, NodeEditor.tap(s, p, null, "l1").state.panelTab)
        assertEquals("t1", NodeEditor.tap(s, p, MapHit.Label("t1"), null).state.selected)
        assertNull(NodeEditor.tap(s, p, null, null).state.selected)
    }

    @Test
    fun `라벨 on an empty spot asks for the text`() {
        val out = NodeEditor.tap(NodeEditor.chooseTool(NodeEditorState(), NodeTool.LABEL), p, null, null)
        assertEquals(NodeEffect.LABEL_TEXT, out.effect)
    }

    @Test
    fun `changing tools drops half-made steps and 레이어 opens its tab`() {
        val s = NodeEditorState(tool = NodeTool.LINK, linkFrom = "a", movingNode = "b", pendingAt = p)
        val next = NodeEditor.chooseTool(s, NodeTool.LAYER)
        assertNull(next.linkFrom)
        assertNull(next.movingNode)
        assertNull(next.pendingAt)
        assertEquals(1, next.panelTab)
        assertEquals(0, NodeEditor.chooseTool(next, NodeTool.MOVE).panelTab)
    }

    @Test
    fun `a tap near a connection line finds it`() {
        val scene = MapScene(links = listOf(SceneLink("l1", Point(0.1f, 0.5f), Point(0.9f, 0.5f), null, false)))
        assertEquals("l1", NodeEditor.linkAt(scene, Point(0.5f, 0.51f)))
        assertNull(NodeEditor.linkAt(scene, Point(0.5f, 0.6f)))
    }

    @Test
    fun `the editor state survives as JSON`() {
        val s = NodeEditorState(NodeTool.LINK, "a", "a", null, p, 2)
        assertEquals(s, NodeEditorState.decode(s.encode()))
        assertEquals(NodeEditorState(), NodeEditorState.decode("{"))
    }
}
