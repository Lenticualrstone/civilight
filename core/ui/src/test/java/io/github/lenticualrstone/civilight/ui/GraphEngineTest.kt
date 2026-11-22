package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.ui.graph.EndWord
import io.github.lenticualrstone.civilight.ui.graph.GraphGeometry
import io.github.lenticualrstone.civilight.ui.graph.GraphHit
import io.github.lenticualrstone.civilight.ui.graph.GraphLine
import io.github.lenticualrstone.civilight.ui.graph.GraphNode
import io.github.lenticualrstone.civilight.ui.graph.GraphScene
import io.github.lenticualrstone.civilight.ui.graph.GraphViewport
import io.github.lenticualrstone.civilight.ui.graph.MiddleWord
import io.github.lenticualrstone.civilight.ui.graph.NodeLook
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Rect
import io.github.lenticualrstone.civilight.ui.graph.Sz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** CNV-01 shared graph engine (copied from feature:canvas with its graph tests): viewport, hits, labels. */
class GraphEngineTest {
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
    }

    @Test
    fun `zoom stays between 25 and 200 percent`() {
        var vp = GraphViewport()
        repeat(20) { vp = vp.zoomIn(Pt(0f, 0f)) }
        assertEquals(2f, vp.zoom)
        assertFalse(vp.canZoomIn)
        repeat(30) { vp = vp.zoomOut(Pt(0f, 0f)) }
        assertEquals(0.25f, vp.zoom)
        assertFalse(vp.canZoomOut)
    }

    @Test
    fun `fit shows the whole graph between the bars, never past 100 percent`() {
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
        assertEquals(GraphHit.Node("a"), GraphGeometry.hit(scene, sizes, Pt(30f, 100f), 0.25f))
        assertEquals(176f, GraphGeometry.hitRect(Rect(0f, 0f, 60f, 50f), 0.25f).height, 0.01f)
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
    }

    @Test
    fun `frames take no lines, cards do`() {
        assertFalse(frame.connectable)
        assertTrue(a.connectable)
        assertTrue(GraphNode("d", "지운 인물 · 노아", null, Pt.ZERO, NodeLook.DOTTED).connectable)
    }
}
