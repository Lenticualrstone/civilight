package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.domain.cast.relation.LineSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.CANVAS
import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.EDGE_RIEN_HART
import io.github.lenticualrstone.civilight.feature.canvas.label.LineEnd
import io.github.lenticualrstone.civilight.feature.canvas.label.LineLabelDraft
import io.github.lenticualrstone.civilight.feature.canvas.label.LineLabelModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** CNV-08 line label and `관계로 만들기` (spec §4.2). */
class LineLabelModelTest {
    private fun ends(doc: ProjectDoc, edge: Key): Pair<LineEnd, LineEnd> = requireNotNull(LineLabelModel.ends(doc, CANVAS, edge, hiddenOpen = false))

    private fun initial(doc: ProjectDoc, edge: Key): LineLabelDraft {
        val (a, b) = ends(doc, edge)
        return LineLabelModel.initialDraft(doc, CanvasFx.edge(doc, edge).labelText, a, b, CanvasFx.LATEST)
    }

    private fun view(doc: ProjectDoc, edge: Key, draft: LineLabelDraft = initial(doc, edge), readOnly: Boolean = false) =
        ends(doc, edge).let { (a, b) -> LineLabelModel.build(doc, a, b, draft, readOnly) }

    private fun relationsOfPair(doc: ProjectDoc, a: Key, b: Key): List<Relation> =
        doc.cast.relations.filter { it.involves(RelationReader.characterRef(a)) && it.involves(RelationReader.characterRef(b)) }

    @Test
    fun `T-CNV-09 default by drawing direction`() {
        val doc = CanvasFx.s0NoRel()
        val v = view(doc, EDGE_RIEN_HART)
        assertEquals(listOf("사이", "리엔의 마음", "하르트의 마음"), v.slotChips.map { it.label })
        assertEquals(listOf("리엔의 마음"), v.slotChips.filter { it.selected }.map { it.label })
        assertEquals("리엔 · 하르트", v.sub)
        assertEquals("선 라벨", v.title)

        val reversedKey = K.canvasEdge(30)
        val reversed = CanvasFx.withLine(CanvasFx.s0NoRel().let { d ->
            val c = CanvasFx.canvas(d)
            d.copy(canvases = d.canvases + (CANVAS to c.copy(edges = c.edges.filter { it.key != EDGE_RIEN_HART })))
        }, reversedKey, CanvasFx.NODE_HART, CanvasFx.NODE_RIEN)
        val rv = view(reversed, reversedKey)
        assertEquals(listOf("하르트의 마음"), rv.slotChips.filter { it.selected }.map { it.label })
        assertEquals(listOf("사이", "하르트의 마음", "리엔의 마음"), rv.slotChips.map { it.label })
    }

    @Test
    fun `T-CNV-10 only 인물–인물 lines can become relations`() {
        var doc = CanvasFx.s0NoRel()
        val lines = listOf(
            K.canvasEdge(31) to (CanvasFx.NODE_RIEN to CanvasFx.NODE_FACTION),
            K.canvasEdge(32) to (CanvasFx.NODE_BLACKOUT to CanvasFx.NODE_LANTERN),
            K.canvasEdge(33) to (CanvasFx.NODE_HART to CanvasFx.NODE_LANTERN),
            K.canvasEdge(34) to (CanvasFx.NODE_MEMO to CanvasFx.NODE_RIEN),
        )
        for ((key, pair) in lines) doc = CanvasFx.withLine(doc, key, pair.first, pair.second)
        for ((key, _) in lines) {
            val v = view(doc, key)
            assertFalse(v.relational)
            assertEquals(listOf("그냥 선으로 두기"), v.buttons)
            assertTrue(v.slotChips.isEmpty())
            assertTrue(v.kindChips.isEmpty())
            assertNull(v.startValue)
            assertNull(v.makeLabel)
        }
    }

    @Test
    fun `T-CNV-11 chips alone record nothing`() {
        val doc = CanvasFx.s0NoRel()
        var draft = initial(doc, EDGE_RIEN_HART)
        draft = LineLabelModel.withSlot(doc, draft, LineSlot.FROM_MIND).copy(kindKey = K.KIND_WARINESS)
        val v = view(doc, EDGE_RIEN_HART, draft)
        assertTrue(v.makeEnabled)
        // Building the surface and picking never touch the document; closing with `닫기` drops the draft.
        assertTrue(relationsOfPair(doc, K.RIEN, K.HART).isEmpty())
        assertEquals(CanvasFx.s0NoRel().changes, doc.changes)
        assertNull(CanvasFx.edge(doc, EDGE_RIEN_HART).relationKey)
    }

    @Test
    fun `T-CNV-12 그냥 선으로 두기 stays on the canvas`() {
        val doc = CanvasFx.s0NoRel()
        val draft = initial(doc, EDGE_RIEN_HART).copy(text = "경계하는 사이")
        val kept = LineLabelModel.keep(doc, CANVAS, EDGE_RIEN_HART, draft, CanvasFx.ctx())
        assertEquals("경계하는 사이", CanvasFx.edge(kept, EDGE_RIEN_HART).labelText)
        assertNull(CanvasFx.edge(kept, EDGE_RIEN_HART).relationKey)
        assertEquals(doc.cast, kept.cast)
        assertEquals(doc.changes, kept.changes)
    }

    @Test
    fun `T-CNV-13 관계로 만들기 writes a 시점 record`() {
        val doc = CanvasFx.s0NoRel()
        val draft = initial(doc, EDGE_RIEN_HART).copy(kindKey = K.KIND_WARINESS)
        assertEquals(K.EPISODE_1_7, draft.startUnitKey)
        assertEquals("1-7", view(doc, EDGE_RIEN_HART, draft).startValue)
        val made = LineLabelModel.make(doc, CANVAS, EDGE_RIEN_HART, draft, CanvasFx.ctx())

        val relation = relationsOfPair(made, K.RIEN, K.HART).single()
        val records = made.changes.records.filter { (it.target as? ChangeTarget.RelationSlot)?.relationKey == relation.key }
        val record = records.single()
        val rienSide = RelationReader.sideOf(relation, K.RIEN)!!
        assertEquals(RelationReader.mindSlot(rienSide), (record.target as ChangeTarget.RelationSlot).slot.known)
        assertEquals(K.KIND_WARINESS, RelationReader.slotValueOf(record)?.kindKey)
        assertTrue(record.anchor.sameAs(Anchor.UnitStart(K.EPISODE_1_7)))
        // The other side's 마음 and the 사이 stay empty.
        assertNull(relation.baseSlot(RelationReader.mindSlot(RelationReader.sideOf(relation, K.HART)!!)))
        assertNull(relation.between)
        assertEquals(relation.key, CanvasFx.edge(made, EDGE_RIEN_HART).relationKey)
    }

    @Test
    fun `T-CNV-14 one relationship per pair`() {
        val sera = K.canvasNode(40)
        val edge = K.canvasEdge(40)
        var doc = CanvasFx.withNode(CanvasFx.s0(), sera, io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind.CHARACTER, K.SERA, 600f, 300f)
        doc = CanvasFx.withLine(doc, edge, sera, CanvasFx.NODE_HART)
        val before = relationsOfPair(doc, K.SERA, K.HART).single()
        val draft = initial(doc, edge).copy(kindKey = K.KIND_TRUST)
        assertEquals(LineSlot.FROM_MIND, draft.slot)
        val made = LineLabelModel.make(doc, CANVAS, edge, draft, CanvasFx.ctx())
        val after = relationsOfPair(made, K.SERA, K.HART).single()
        assertEquals(before.key, after.key)
        assertEquals(before.between, after.between)
        val record = made.changes.records.single { r ->
            (r.target as? ChangeTarget.RelationSlot)?.relationKey == after.key && r !in doc.changes.records
        }
        assertEquals(RelationReader.mindSlot(RelationReader.sideOf(after, K.SERA)!!), (record.target as ChangeTarget.RelationSlot).slot.known)
        assertTrue(record.anchor.sameAs(Anchor.UnitStart(K.EPISODE_1_7)))
        assertEquals(after.key, CanvasFx.edge(made, edge).relationKey)
    }

    @Test
    fun `사이 switches the kind list and drops a 마음 kind`() {
        val doc = CanvasFx.s0NoRel()
        val mind = initial(doc, EDGE_RIEN_HART).copy(kindKey = K.KIND_WARINESS)
        val mindView = view(doc, EDGE_RIEN_HART, mind)
        assertTrue(mindView.kindChips.any { it.label == "경계" && it.selected })
        assertFalse(mindView.kindChips.any { it.label == "동료" })
        val between = LineLabelModel.withSlot(doc, mind, LineSlot.BETWEEN)
        assertEquals(LineSlot.BETWEEN, between.slot)
        assertNull(between.kindKey)
        val betweenView = view(doc, EDGE_RIEN_HART, between)
        assertTrue(betweenView.kindChips.any { it.label == "동료" })
        assertTrue(betweenView.kindChips.any { it.label == "선배 ↔ 후배" })
        assertFalse(betweenView.makeEnabled)
        // Back to a mind with the same kind kept when it belongs to the category.
        assertEquals(K.KIND_WARINESS, LineLabelModel.withSlot(doc, mind, LineSlot.TO_MIND).kindKey)
    }

    @Test
    fun `관계로 만들기 waits for a kind and is off in a read-only project`() {
        val doc = CanvasFx.s0NoRel()
        val draft = initial(doc, EDGE_RIEN_HART)
        assertNull(draft.kindKey)
        assertFalse(view(doc, EDGE_RIEN_HART, draft).makeEnabled)
        val picked = draft.copy(kindKey = K.KIND_WARINESS)
        assertTrue(view(doc, EDGE_RIEN_HART, picked).makeEnabled)
        val ro = view(doc, EDGE_RIEN_HART, picked, readOnly = true)
        assertFalse(ro.makeEnabled)
        assertTrue(ro.readOnly)
    }

    @Test
    fun `an already converted line opens with the same rows and reuses its relation`() {
        val doc = CanvasFx.s0()
        val v = view(doc, EDGE_RIEN_HART)
        assertTrue(v.relational)
        assertEquals(listOf("그냥 선으로 두기", "관계로 만들기"), v.buttons)
        val made = LineLabelModel.make(doc, CANVAS, EDGE_RIEN_HART, initial(doc, EDGE_RIEN_HART).copy(kindKey = K.KIND_TRUST), CanvasFx.ctx())
        assertEquals(K.REL_RIEN_HART, relationsOfPair(made, K.RIEN, K.HART).single().key)
        assertEquals(K.REL_RIEN_HART, CanvasFx.edge(made, EDGE_RIEN_HART).relationKey)
    }

    @Test
    fun `a locked hidden character cannot be named or related`() {
        val doc = CanvasFx.s0NoRel().let { d ->
            d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))
        }
        val (a, b) = ends(doc, EDGE_RIEN_HART)
        assertEquals("숨긴 인물", b.name)
        assertNull(b.characterKey)
        assertFalse(LineLabelModel.build(doc, a, b, LineLabelModel.initialDraft(doc, null, a, b, CanvasFx.LATEST), false).relational)
        assertNotNull(a.characterKey)
    }

    @Test
    fun `drafts survive the saved-state round trip`() {
        val draft = LineLabelDraft("경계하는 사이", LineSlot.TO_MIND, K.KIND_WARINESS, K.EPISODE_1_7)
        assertEquals(draft, LineLabelDraft.restore(draft.save()))
        val empty = LineLabelDraft("", null, null, null)
        assertEquals(empty, LineLabelDraft.restore(empty.save()))
        assertEquals(RelationSlotName.MIND_A, RelationReader.mindSlot(io.github.lenticualrstone.civilight.domain.cast.relation.Side.A))
    }
}
