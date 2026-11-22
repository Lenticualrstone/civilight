package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.CANVAS
import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.EDGE_RIEN_HART
import io.github.lenticualrstone.civilight.feature.canvas.board.NodeLabels
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.feature.canvas.edit.LineResult
import io.github.lenticualrstone.civilight.ui.graph.NodeLook
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Sz
import io.github.lenticualrstone.civilight.feature.canvas.node.NodeEditModel
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.EventDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Canvas document edits (CNV-02, CNV-04 … CNV-07, CNV-09 delete, CNV-10) and node labels. */
class CanvasEditsTest {
    private val ctx = CanvasFx.ctx()

    @Test
    fun `CNV-02 the route's canvas, else the first one, else none`() {
        val doc = CanvasFx.s0()
        assertEquals(CANVAS, CanvasEdits.resolve(doc, null)?.key)
        assertEquals(CANVAS, CanvasEdits.resolve(doc, "missing")?.key)
        val empty = doc.copy(canvases = emptyMap())
        assertNull(CanvasEdits.resolve(empty, null))
        val (made, key) = CanvasEdits.createCanvas(empty, "캔버스", ctx)
        assertEquals(key, CanvasEdits.resolve(made, null)?.key)
        assertEquals("캔버스", made.canvases.getValue(key).name)
        assertTrue(made.canvases.getValue(key).nodes.isEmpty())
    }

    @Test
    fun `CNV-04 an item node stores only kind and key and shows the current name`() {
        val (doc, key) = CanvasEdits.addItemNode(CanvasFx.s0(), CANVAS, CanvasNodeKind.CHARACTER, K.SERA, Pt(10f, 20f), ctx)
        val node = CanvasFx.canvas(doc).nodes.single { it.key == key }
        assertEquals(K.SERA, node.refKey)
        assertNull(node.text)
        assertEquals(10f to 20f, node.x to node.y)
        assertTrue(CanvasFx.canvas(doc).nodes.filter { it.key != key }.all { it.z < node.z })
        assertEquals("세라", NodeLabels.of(doc, node, hiddenOpen = false).title)
        // Renaming the character renames the node; the canvas holds no copy.
        val renamed = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.SERA) it.copy(name = "세라 하") else it }))
        assertEquals("세라 하", NodeLabels.of(renamed, node, hiddenOpen = false).title)
        assertEquals("인물", NodeLabels.of(renamed, node, hiddenOpen = false).kindWord)
    }

    @Test
    fun `CNV-04 node kinds read as the board`() {
        val doc = CanvasFx.s0()
        val labels = CanvasFx.canvas(doc).nodes.associate { it.key to NodeLabels.of(doc, it, hiddenOpen = false) }
        assertEquals("등불 결사" to "세력", labels.getValue(CanvasFx.NODE_FACTION).let { it.title to it.kindWord })
        assertEquals("리엔" to "인물", labels.getValue(CanvasFx.NODE_RIEN).let { it.title to it.kindWord })
        assertEquals("하르트" to "인물", labels.getValue(CanvasFx.NODE_HART).let { it.title to it.kindWord })
        assertEquals("대정전" to "사건", labels.getValue(CanvasFx.NODE_BLACKOUT).let { it.title to it.kindWord })
        assertEquals("오리지늄 등불" to "용어", labels.getValue(CanvasFx.NODE_LANTERN).let { it.title to it.kindWord })
        assertEquals("2장 떡밥?" to "메모", labels.getValue(CanvasFx.NODE_MEMO).let { it.title to it.kindWord })
        assertEquals(NodeLook.MEMO, labels.getValue(CanvasFx.NODE_MEMO).look)
    }

    @Test
    fun `CNV-11 PROPOSED a deleted item stays as a dotted reference`() {
        val (doc, key) = CanvasEdits.addItemNode(CanvasFx.s0(), CANVAS, CanvasNodeKind.CHARACTER, K.NOAH, Pt(0f, 0f), ctx)
        val label = NodeLabels.of(doc, CanvasFx.canvas(doc).nodes.single { it.key == key }, hiddenOpen = false)
        assertEquals("지운 인물 · 노아", label.title)
        assertEquals(NodeLook.DOTTED, label.look)
        assertFalse(label.openable)
    }

    @Test
    fun `CNV-05 memo text is owned by the canvas`() {
        val (doc, key) = CanvasEdits.addMemo(CanvasFx.s0(), CANVAS, Pt(0f, 0f), ctx)
        val blank = CanvasFx.canvas(doc).nodes.single { it.key == key }
        assertEquals("메모", NodeLabels.of(doc, blank, false).title)
        val written = CanvasEdits.setText(doc, CANVAS, key, "3장 복선", ctx)
        assertEquals("3장 복선", CanvasFx.canvas(written).nodes.single { it.key == key }.text)
        // Item nodes have no text to set.
        assertNull(CanvasFx.canvas(CanvasEdits.setText(doc, CANVAS, CanvasFx.NODE_RIEN, "x", ctx)).nodes.single { it.key == CanvasFx.NODE_RIEN }.text)
    }

    @Test
    fun `CNV-06 PROPOSED a 묶음 carries its members and a dropped card joins or leaves it`() {
        var (doc, group) = CanvasEdits.addGroup(CanvasFx.s0(), CANVAS, Pt(1000f, 1000f), ctx)
        // Drop 리엔 inside the frame: it joins.
        doc = CanvasEdits.moveNode(doc, CANVAS, CanvasFx.NODE_RIEN, Pt(1020f, 1020f), ctx, Sz(60f, 50f))
        assertEquals(group, CanvasFx.canvas(doc).nodes.single { it.key == CanvasFx.NODE_RIEN }.groupKey)
        // Move the frame: 리엔 moves by the same distance.
        doc = CanvasEdits.moveNode(doc, CANVAS, group, Pt(1100f, 1050f), ctx)
        val rien = CanvasFx.canvas(doc).nodes.single { it.key == CanvasFx.NODE_RIEN }
        assertEquals(1120f to 1070f, rien.x to rien.y)
        // Drop 리엔 outside: it leaves.
        doc = CanvasEdits.moveNode(doc, CANVAS, CanvasFx.NODE_RIEN, Pt(0f, 0f), ctx, Sz(60f, 50f))
        assertNull(CanvasFx.canvas(doc).nodes.single { it.key == CanvasFx.NODE_RIEN }.groupKey)
        // Removing a frame frees its members.
        doc = CanvasEdits.moveNode(doc, CANVAS, CanvasFx.NODE_HART, Pt(1120f, 1070f), ctx, Sz(60f, 50f))
        doc = CanvasEdits.removeNode(doc, CANVAS, group, ctx)
        assertNull(CanvasFx.canvas(doc).nodes.single { it.key == CanvasFx.NODE_HART }.groupKey)
        val (fresh, freshKey) = CanvasEdits.addGroup(CanvasFx.s0(), CANVAS, Pt(0f, 0f), CanvasFx.ctx())
        assertEquals("묶음", NodeLabels.of(fresh, CanvasFx.canvas(fresh).nodes.single { it.key == freshKey }, false).title)
    }

    @Test
    fun `CNV-07 PROPOSED tap–tap lines, no self-line, one line per pair`() {
        val doc = CanvasFx.s0()
        val (made, created) = CanvasEdits.addLine(doc, CANVAS, CanvasFx.NODE_RIEN, CanvasFx.NODE_FACTION, ctx)
        val key = (created as LineResult.Created).edgeKey
        val edge = CanvasFx.edge(made, key)
        assertEquals(CanvasFx.NODE_RIEN to CanvasFx.NODE_FACTION, edge.fromNodeKey to edge.toNodeKey)
        assertNull(edge.relationKey)
        assertEquals(doc.cast, made.cast)
        assertEquals(LineResult.Existing(EDGE_RIEN_HART), CanvasEdits.addLine(doc, CANVAS, CanvasFx.NODE_HART, CanvasFx.NODE_RIEN, ctx).second)
        assertEquals(LineResult.Refused, CanvasEdits.addLine(doc, CANVAS, CanvasFx.NODE_RIEN, CanvasFx.NODE_RIEN, ctx).second)
        val (withGroup, group) = CanvasEdits.addGroup(doc, CANVAS, Pt(0f, 0f), ctx)
        assertEquals(LineResult.Refused, CanvasEdits.addLine(withGroup, CANVAS, CanvasFx.NODE_RIEN, group, ctx).second)
    }

    @Test
    fun `CNV-09 PROPOSED removing a converted line keeps the relationship`() {
        val doc = CanvasFx.s0()
        val removed = CanvasEdits.removeLine(doc, CANVAS, EDGE_RIEN_HART, ctx)
        assertTrue(CanvasFx.canvas(removed).edges.none { it.key == EDGE_RIEN_HART })
        assertNotNull(removed.index.relation(K.REL_RIEN_HART))
        assertEquals(doc.changes, removed.changes)
    }

    @Test
    fun `CNV-10 PROPOSED removing a node takes its lines, never the item`() {
        val doc = CanvasFx.s0()
        val removed = CanvasEdits.removeNode(doc, CANVAS, CanvasFx.NODE_RIEN, ctx)
        val canvas = CanvasFx.canvas(removed)
        assertTrue(canvas.nodes.none { it.key == CanvasFx.NODE_RIEN })
        assertTrue(canvas.edges.none { it.fromNodeKey == CanvasFx.NODE_RIEN || it.toNodeKey == CanvasFx.NODE_RIEN })
        assertNotNull(removed.index.character(K.RIEN))
        assertNotNull(removed.index.relation(K.REL_RIEN_HART))
        assertEquals(ctx.now, canvas.updatedAt)
    }

    @Test
    fun `CNV-10 node edit opens the item and edits memo text`() {
        val doc = CanvasFx.s0()
        val rien = NodeEditModel.build(doc, CANVAS, CanvasFx.NODE_RIEN, hiddenOpen = false, readOnly = false)!!
        assertEquals("인물 기록", rien.openLabel)
        assertEquals(CharacterProfile(doc.key, K.RIEN), rien.openRoute)
        assertNull(rien.text)
        val event = NodeEditModel.build(doc, CANVAS, CanvasFx.NODE_BLACKOUT, false, false)!!
        assertEquals("사건 상세" to EventDetail(doc.key, K.EVENT_BLACKOUT), event.openLabel to event.openRoute)
        assertEquals("용어 카드", NodeEditModel.build(doc, CANVAS, CanvasFx.NODE_LANTERN, false, false)!!.openLabel)
        assertEquals("세력 카드", NodeEditModel.build(doc, CANVAS, CanvasFx.NODE_FACTION, false, false)!!.openLabel)
        val memo = NodeEditModel.build(doc, CANVAS, CanvasFx.NODE_MEMO, false, true)!!
        assertEquals("2장 떡밥?", memo.text)
        assertNull(memo.openLabel)
        assertTrue(memo.readOnly)
        // A locked hidden item cannot be opened from the canvas.
        val hidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
        val masked = NodeEditModel.build(hidden, CANVAS, CanvasFx.NODE_RIEN, false, false)!!
        assertEquals("숨긴 인물", masked.title)
        assertNull(masked.openLabel)
        assertNull(masked.kindWord)
    }

    @Test
    fun `event picker searches titles with 초성`() {
        val doc = CanvasFx.s0()
        // The shared event picker (core pass, PickKind.EVENT) replaced the canvas's local list.
        assertTrue(PickerModels.events(doc, "").items.any { it.key == K.EVENT_BLACKOUT })
        assertTrue(PickerModels.events(doc, "ㄷㅈㅈ").items.any { it.key == K.EVENT_BLACKOUT })
        assertTrue(PickerModels.events(doc, "없는 사건").items.isEmpty())
    }
}
