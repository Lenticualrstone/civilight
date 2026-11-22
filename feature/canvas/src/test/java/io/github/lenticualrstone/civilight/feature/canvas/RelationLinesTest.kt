package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import io.github.lenticualrstone.civilight.domain.cast.relation.LineSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationEdits
import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.CANVAS
import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.EDGE_BLACKOUT_LANTERN
import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.EDGE_RIEN_HART
import io.github.lenticualrstone.civilight.feature.canvas.board.BoardScene
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphGeometry
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphLine
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphScene
import io.github.lenticualrstone.civilight.feature.canvas.graph.LineStroke
import io.github.lenticualrstone.civilight.feature.canvas.graph.MiddleWord
import io.github.lenticualrstone.civilight.feature.canvas.graph.Sz
import io.github.lenticualrstone.civilight.feature.canvas.label.LineLabelDraft
import io.github.lenticualrstone.civilight.feature.canvas.label.LineLabelModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** CNV-01 relation styling and CNV-09 relation lines under the lens (spec §4.1, T-CNV-58). */
class RelationLinesTest {
    private fun scene(doc: ProjectDoc, lens: LensPoint): GraphScene =
        BoardScene.build(doc, doc.canvases.getValue(CANVAS), lens, hiddenOpen = false)

    private fun line(doc: ProjectDoc, lens: LensPoint, key: Key): GraphLine = scene(doc, lens).lines.single { it.key == key }

    /** Label texts as drawn (arrows from the real node positions). */
    private fun texts(doc: ProjectDoc, lens: LensPoint, key: Key): Pair<String?, List<String>> {
        val s = scene(doc, lens)
        val l = s.lines.single { it.key == key }
        val a = GraphGeometry.rectOf(s.nodesByKey.getValue(l.from), emptyMap()).center
        val b = GraphGeometry.rectOf(s.nodesByKey.getValue(l.to), emptyMap()).center
        return l.middle?.let { GraphGeometry.middleText(it, a, b) } to l.ends.map { GraphGeometry.endText(it, a, b) }
    }

    @Test
    fun `T-CNV-01 relation line at 최신`() {
        val l = line(CanvasFx.s0(), CanvasFx.LATEST, EDGE_RIEN_HART)
        assertEquals(LineStroke.DASHED, l.stroke)
        assertNull(l.middle)
        assertEquals(null to listOf("경계 →", "← 보호"), texts(CanvasFx.s0(), CanvasFx.LATEST, EDGE_RIEN_HART))
    }

    @Test
    fun `T-CNV-02 relation line at 1-9`() {
        val l = line(CanvasFx.s0(), CanvasFx.L1_9, EDGE_RIEN_HART)
        assertEquals(LineStroke.SOLID, l.stroke)
        assertEquals("동료" to listOf("신뢰 →", "← 보호"), texts(CanvasFx.s0(), CanvasFx.L1_9, EDGE_RIEN_HART))
    }

    @Test
    fun `T-CNV-03 1-8 equals 1-7`() {
        val doc = CanvasFx.s0()
        assertEquals(line(doc, CanvasFx.L1_7, EDGE_RIEN_HART), line(doc, CanvasFx.L1_8, EDGE_RIEN_HART))
        assertEquals(line(doc, CanvasFx.LATEST, EDGE_RIEN_HART), line(doc, CanvasFx.L1_7, EDGE_RIEN_HART))
    }

    @Test
    fun `T-CNV-04 canvas variant - before the first change the line is plain (CNV-09 PROPOSED)`() {
        val l = line(CanvasFx.s0(), CanvasFx.L1_6, EDGE_RIEN_HART)
        assertEquals(LineStroke.SOLID, l.stroke)
        assertNull(l.middle)
        assertTrue(l.ends.isEmpty())
    }

    @Test
    fun `T-CNV-05 사이 only`() {
        val owen = K.canvasNode(20)
        val edge = K.canvasEdge(20)
        var doc = CanvasFx.withNode(CanvasFx.s0(), owen, CanvasNodeKind.CHARACTER, K.OWEN, 40f, 160f)
        doc = CanvasFx.withLine(doc, edge, CanvasFx.NODE_RIEN, owen, relationKey = K.REL_RIEN_OWEN)
        val l = line(doc, CanvasFx.LATEST, edge)
        assertEquals(LineStroke.SOLID, l.stroke)
        assertEquals("거래 상대" to emptyList<String>(), texts(doc, CanvasFx.LATEST, edge))
    }

    @Test
    fun `T-CNV-06 one side's 마음 never mirrored`() {
        val sera = K.canvasNode(21)
        val edge = K.canvasEdge(21)
        var doc = CanvasFx.withNode(CanvasFx.s0(), sera, CanvasNodeKind.CHARACTER, K.SERA, 600f, 160f)
        doc = CanvasFx.withLine(doc, edge, CanvasFx.NODE_RIEN, sera, relationKey = K.REL_RIEN_SERA)
        val l = line(doc, CanvasFx.LATEST, edge)
        assertEquals(LineStroke.SOLID, l.stroke)
        val (middle, ends) = texts(doc, CanvasFx.LATEST, edge)
        assertEquals(listOf("의지 →"), ends)
        assertTrue(l.ends.single().nearFrom)
        // Paired 사이 (Q-CV-11; cast default Q15): both roles, left to right; 리엔 is 세라's 후배.
        assertEquals("후배 ↔ 선배", middle)
    }

    @Test
    fun `T-CNV-07 호칭 only draws a plain line`() {
        val ctx = CanvasFx.ctx()
        var doc = RelationEdits.setAddress(CanvasFx.s0(), K.SERA, K.OWEN, "오웬", ctx)
        val relation = doc.cast.relations.single { it.involves(io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader.characterRef(K.SERA)) && it.involves(io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader.characterRef(K.OWEN)) }
        val sera = K.canvasNode(22)
        val owen = K.canvasNode(23)
        val edge = K.canvasEdge(22)
        doc = CanvasFx.withNode(doc, sera, CanvasNodeKind.CHARACTER, K.SERA, 0f, 0f)
        doc = CanvasFx.withNode(doc, owen, CanvasNodeKind.CHARACTER, K.OWEN, 200f, 0f)
        doc = CanvasFx.withLine(doc, edge, sera, owen, relationKey = relation.key)
        val l = line(doc, CanvasFx.LATEST, edge)
        assertEquals(LineStroke.SOLID, l.stroke)
        assertNull(l.middle)
        assertTrue(l.ends.isEmpty())
    }

    @Test
    fun `T-CNV-08 never colour by kind`() {
        val doc = CanvasFx.s0()
        val wary = line(doc, CanvasFx.LATEST, EDGE_RIEN_HART)
        val trusting = line(doc, CanvasFx.L1_9, EDGE_RIEN_HART)
        for (l in listOf(wary, trusting)) {
            val s = GraphGeometry.stroke(l, selected = false, cover = true)
            assertEquals(ColorRole.INK3, s.color)
            assertEquals(1.2f, s.widthDp)
        }
        assertEquals(ColorRole.ACC, GraphGeometry.stroke(wary, selected = true, cover = true).color)
        assertEquals(2.2f, GraphGeometry.stroke(wary, selected = true, cover = true).widthDp)
        assertEquals(2f, GraphGeometry.stroke(wary, selected = true, cover = false).widthDp)
    }

    @Test
    fun `T-CNV-58 converted lines look like the relationship map, plain lines stay grey`() {
        val start = CanvasFx.s0NoRel()
        val draft = LineLabelDraft("", LineSlot.FROM_MIND, K.KIND_WARINESS, K.EPISODE_1_7)
        val doc = LineLabelModel.make(start, CANVAS, EDGE_RIEN_HART, draft, CanvasFx.ctx())
        val converted = line(doc, CanvasFx.LATEST, EDGE_RIEN_HART)
        assertEquals(LineStroke.DASHED, converted.stroke)
        assertEquals(null to listOf("경계 →"), texts(doc, CanvasFx.LATEST, EDGE_RIEN_HART))
        val plain = line(doc, CanvasFx.LATEST, EDGE_BLACKOUT_LANTERN)
        assertEquals(LineStroke.SOLID, plain.stroke)
        assertEquals(MiddleWord("그날 꺼진 빛"), plain.middle)
        assertTrue(plain.ends.isEmpty())
        assertEquals(ColorRole.INK3, GraphGeometry.stroke(plain, selected = false, cover = true).color)
    }

    @Test
    fun `T-CNV-16 a start point after the 집필 선 is 예정 on the canvas`() {
        val draft = LineLabelDraft("", LineSlot.FROM_MIND, K.KIND_WARINESS, K.EPISODE_1_9)
        val doc = LineLabelModel.make(CanvasFx.s0NoRel(), CANVAS, EDGE_RIEN_HART, draft, CanvasFx.ctx())
        assertTrue(line(doc, CanvasFx.LATEST, EDGE_RIEN_HART).ends.isEmpty())
        assertEquals(null to listOf("경계 →"), texts(doc, CanvasFx.L1_9, EDGE_RIEN_HART))
    }

    @Test
    fun `T-CNV-15 canvas part - a 시점 record applies from the start of its unit`() {
        val draft = LineLabelDraft("", LineSlot.FROM_MIND, K.KIND_WARINESS, K.EPISODE_1_7)
        val doc = LineLabelModel.make(CanvasFx.s0NoRel(), CANVAS, EDGE_RIEN_HART, draft, CanvasFx.ctx())
        assertTrue(line(doc, CanvasFx.L1_6, EDGE_RIEN_HART).ends.isEmpty())
        assertEquals(listOf("경계 →"), texts(doc, CanvasFx.L1_7, EDGE_RIEN_HART).second)
    }

    @Test
    fun `arrows follow the drawn positions, not the drawing direction`() {
        // Move 리엔 to the right of 하르트: 리엔's mind now reads `← 경계`, 하르트's `보호 →`.
        val doc = CanvasFx.s0()
        val c = doc.canvases.getValue(CANVAS)
        val swapped = doc.copy(canvases = doc.canvases + (CANVAS to c.copy(nodes = c.nodes.map { if (it.key == CanvasFx.NODE_RIEN) it.copy(x = 800f) else it })))
        assertEquals(null to listOf("← 경계", "보호 →"), texts(swapped, CanvasFx.LATEST, EDGE_RIEN_HART))
    }

    @Test
    fun `a locked hidden end draws no relation labels`() {
        val doc = CanvasFx.s0().let { d ->
            d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))
        }
        val l = line(doc, CanvasFx.LATEST, EDGE_RIEN_HART)
        assertTrue(l.ends.isEmpty())
        assertEquals("숨긴 인물", scene(doc, CanvasFx.LATEST).nodesByKey.getValue(CanvasFx.NODE_HART).title)
        // Opened: the relation look is back.
        val open = BoardScene.build(doc, doc.canvases.getValue(CANVAS), CanvasFx.LATEST, hiddenOpen = true)
        assertEquals(2, open.lines.single { it.key == EDGE_RIEN_HART }.ends.size)
        assertEquals(Sz.DEFAULT, Sz.DEFAULT)
    }
}
