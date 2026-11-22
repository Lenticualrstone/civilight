package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.board.BoardAction
import io.github.lenticualrstone.civilight.feature.canvas.board.BoardReducer
import io.github.lenticualrstone.civilight.feature.canvas.board.BoardScene
import io.github.lenticualrstone.civilight.feature.canvas.board.BoardUi
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasTool
import io.github.lenticualrstone.civilight.ui.graph.EndWord
import io.github.lenticualrstone.civilight.ui.graph.GraphGeometry
import io.github.lenticualrstone.civilight.ui.graph.GraphHit
import io.github.lenticualrstone.civilight.ui.graph.GraphLine
import io.github.lenticualrstone.civilight.ui.graph.GraphNode
import io.github.lenticualrstone.civilight.ui.graph.GraphScene
import io.github.lenticualrstone.civilight.ui.graph.GraphSelection
import io.github.lenticualrstone.civilight.ui.graph.GraphViewport
import io.github.lenticualrstone.civilight.ui.graph.MiddleWord
import io.github.lenticualrstone.civilight.ui.graph.NodeLook
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Rect
import io.github.lenticualrstone.civilight.ui.graph.Sz
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.open
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** CNV-01 graph engine (viewport, hit tests, labels) and CNV-03 / CNV-07 tool behaviour. */
class GraphAndToolsTest {
    private val a = GraphNode("a", "리엔", "인물", Pt(0f, 0f))
    private val b = GraphNode("b", "하르트", "인물", Pt(300f, 0f))
    private val frame = GraphNode("g", "묶음", null, Pt(-100f, 200f), NodeLook.FRAME, Sz(260f, 180f))
    private val line = GraphLine("l", "a", "b")
    private val scene = GraphScene(listOf(frame, a, b), listOf(line))
    private val sizes = mapOf("a" to Sz(60f, 50f), "b" to Sz(80f, 50f))

    private fun near(x: Float, y: Float) = 0.01f.let { e -> { p: Pt -> abs(p.x - x) < e && abs(p.y - y) < e } }

    @Test
    fun `viewport maps both ways and zooms around a point`() {
        val vp = GraphViewport(offsetX = 30f, offsetY = -10f, zoom = 0.82f)
        val p = Pt(123f, 45f)
        assertTrue(near(p.x, p.y)(vp.toContent(vp.toScreen(p))))
        val focus = Pt(200f, 300f)
        val zoomed = vp.zoomAround(focus, 1.5f)
        assertTrue(near(focus.x, focus.y)(zoomed.toScreen(vp.toContent(focus))))
        assertEquals("82%", CanvasCopy.zoomPercent(vp.zoom))
    }

    @Test
    fun `zoom stays between 25 and 200 percent (Q-CV-07 default)`() {
        var vp = GraphViewport()
        repeat(20) { vp = vp.zoomIn(Pt(0f, 0f)) }
        assertEquals(2f, vp.zoom)
        assertFalse(vp.canZoomIn)
        repeat(30) { vp = vp.zoomOut(Pt(0f, 0f)) }
        assertEquals(0.25f, vp.zoom)
        assertFalse(vp.canZoomOut)
        assertEquals("125%", CanvasCopy.zoomPercent(GraphViewport().zoomIn(Pt(0f, 0f)).zoom))
    }

    @Test
    fun `fit shows the whole canvas between the bars, never past 100 percent`() {
        val content = Rect(0f, 0f, 600f, 400f)
        val vp = GraphViewport.fit(content, 390f, 844f, topInset = 112f, bottomInset = 80f)
        assertTrue(vp.zoom < 1f)
        val tl = vp.toScreen(Pt(0f, 0f))
        val br = vp.toScreen(Pt(600f, 400f))
        assertTrue(tl.x >= 0f && br.x <= 390f && tl.y >= 112f && br.y <= 844f - 80f)
        assertEquals(1f, GraphViewport.fit(Rect(0f, 0f, 50f, 50f), 390f, 844f, 112f, 80f).zoom)
    }

    @Test
    fun `hits - cards first, then lines within half a target, then frames`() {
        assertEquals(GraphHit.Node("a"), GraphGeometry.hit(scene, sizes, Pt(30f, 25f), 1f))
        assertEquals(GraphHit.Line("l"), GraphGeometry.hit(scene, sizes, Pt(150f, 40f), 1f))
        assertEquals(GraphHit.Empty, GraphGeometry.hit(scene, sizes, Pt(150f, 60f), 1f))
        assertEquals(GraphHit.Frame("g"), GraphGeometry.hit(scene, sizes, Pt(0f, 300f), 1f))
        // At 25 % a 50 dp card is 12.5 dp on screen; its target grows to 44 dp (176 content dp).
        assertEquals(GraphHit.Node("a"), GraphGeometry.hit(scene, sizes, Pt(30f, 100f), 0.25f))
        val grown = GraphGeometry.hitRect(Rect(0f, 0f, 60f, 50f), 0.25f)
        assertEquals(176f, grown.height, 0.01f)
    }

    @Test
    fun `lines leave the cards and labels sit on the visible part`() {
        val (from, to) = GraphGeometry.visibleSegment(GraphGeometry.rectOf(a, sizes), GraphGeometry.rectOf(b, sizes))
        assertEquals(60f, from.x, 0.01f)
        assertEquals(300f, to.x, 0.01f)
        assertEquals(180f, GraphGeometry.middleAnchor(from to to).x, 0.01f)
        assertEquals(60f + 240f * 0.28f, GraphGeometry.endAnchor(EndWord("경계", true), from to to).x, 0.01f)
    }

    @Test
    fun `pair words read left to right and arrows point at the partner`() {
        val left = Pt(0f, 0f)
        val right = Pt(100f, 0f)
        assertEquals("후배 ↔ 선배", GraphGeometry.middleText(MiddleWord("후배", "선배"), left, right))
        assertEquals("선배 ↔ 후배", GraphGeometry.middleText(MiddleWord("후배", "선배"), right, left))
        assertEquals("동료", GraphGeometry.middleText(MiddleWord("동료"), right, left))
        assertEquals("경계 →", GraphGeometry.endText(EndWord("경계", true), left, right))
        assertEquals("← 보호", GraphGeometry.endText(EndWord("보호", false), left, right))
        assertEquals("← 경계", GraphGeometry.endText(EndWord("경계", true), right, left))
    }

    @Test
    fun `CNV-03 one current tool, 노드 first, only it acts on empty space`() {
        val ui = BoardUi()
        assertEquals(CanvasTool.NODE, ui.tool)
        val at = Pt(10f, 10f)
        assertEquals(BoardAction.PickItem(CanvasTool.NODE, at), BoardReducer.tap(ui, GraphHit.Empty, at, false).second)
        assertEquals(BoardAction.PickItem(CanvasTool.CHARACTER, at), BoardReducer.tap(BoardReducer.selectTool(ui, CanvasTool.CHARACTER), GraphHit.Empty, at, false).second)
        assertEquals(BoardAction.AddMemo(at), BoardReducer.tap(BoardReducer.selectTool(ui, CanvasTool.MEMO), GraphHit.Empty, at, false).second)
        assertEquals(BoardAction.AddGroup(at), BoardReducer.tap(BoardReducer.selectTool(ui, CanvasTool.GROUP), GraphHit.Empty, at, false).second)
        assertEquals(BoardAction.None, BoardReducer.tap(BoardReducer.selectTool(ui, CanvasTool.LINE), GraphHit.Empty, at, false).second)
        assertEquals(BoardAction.ReadOnly, BoardReducer.tap(ui, GraphHit.Empty, at, readOnly = true).second)
        // A tap away from a selection only lets it go.
        val selected = ui.copy(selection = GraphSelection(nodeKey = "a"))
        val (cleared, action) = BoardReducer.tap(selected, GraphHit.Empty, at, false)
        assertEquals(BoardAction.None, action)
        assertEquals(GraphSelection.NONE, cleared.selection)
    }

    @Test
    fun `CNV-04 and CNV-08 a tap on a node or line opens its surface`() {
        val (ui, action) = BoardReducer.tap(BoardUi(), GraphHit.Node("a"), Pt.ZERO, false)
        assertEquals(BoardAction.EditNode("a"), action)
        assertEquals(GraphSelection(nodeKey = "a"), ui.selection)
        val (ui2, action2) = BoardReducer.tap(ui, GraphHit.Line("l"), Pt.ZERO, false)
        assertEquals(BoardAction.EditLine("l"), action2)
        assertEquals(GraphSelection(lineKey = "l"), ui2.selection)
        assertEquals(BoardAction.EditNode("g"), BoardReducer.tap(BoardUi(), GraphHit.Frame("g"), Pt.ZERO, false).second)
    }

    @Test
    fun `CNV-07 PROPOSED 선 draws by tapping one node then another`() {
        var ui = BoardReducer.selectTool(BoardUi(), CanvasTool.LINE)
        val (first, none) = BoardReducer.tap(ui, GraphHit.Node("a"), Pt.ZERO, false)
        assertEquals(BoardAction.None, none)
        assertEquals("a", first.lineStart)
        val (done, draw) = BoardReducer.tap(first, GraphHit.Node("b"), Pt.ZERO, false)
        assertEquals(BoardAction.DrawLine("a", "b"), draw)
        assertEquals(null, done.lineStart)
        // Tapping the same node again cancels; a frame never takes a line.
        assertEquals(null, BoardReducer.tap(first, GraphHit.Node("a"), Pt.ZERO, false).first.lineStart)
        assertEquals(BoardAction.None, BoardReducer.tap(first, GraphHit.Frame("g"), Pt.ZERO, false).second)
        assertEquals(BoardAction.ReadOnly, BoardReducer.tap(first, GraphHit.Node("b"), Pt.ZERO, readOnly = true).second)
        // Changing the tool drops a started line.
        ui = BoardReducer.selectTool(first, CanvasTool.NODE)
        assertEquals(null, ui.lineStart)
        assertEquals(GraphSelection(lineKey = "e"), BoardReducer.lineDrawn(ui, "e").selection)
    }

    @Test
    fun `board state survives the saved-state round trip`() {
        val ui = BoardUi(CanvasTool.LINE, GraphSelection(nodeKey = "a"), lineStart = "a")
        assertEquals(ui, BoardUi.restore(ui.save()))
        assertEquals(BoardUi(), BoardUi.restore(emptyList()))
    }

    @Test
    fun `a dragged 묶음 carries its members on screen before the drop is written`() {
        val doc = CanvasFx.s0()
        val c = CanvasFx.canvas(doc)
        val group = "g0"
        val withGroup = c.copy(
            nodes = c.nodes.map { if (it.key == CanvasFx.NODE_RIEN) it.copy(groupKey = group) else it } +
                CanvasNode(
                    key = group, kind = CanvasNodeKind.GROUP.open(),
                    x = 200f, y = 100f, w = 260f, h = 180f, z = "0",
                ),
        )
        val rien = withGroup.nodes.single { it.key == CanvasFx.NODE_RIEN }
        val moved = mapOf(group to Pt(250f, 130f))
        assertEquals(Pt(rien.x + 50f, rien.y + 30f), BoardScene.positionOf(withGroup, rien, moved))
        val scene = BoardScene.build(doc.copy(canvases = doc.canvases + (c.key to withGroup)), withGroup, CanvasFx.LATEST, false, moved)
        // Frames are drawn first, behind the cards.
        assertEquals(NodeLook.FRAME, scene.nodes.first().look)
    }
}
