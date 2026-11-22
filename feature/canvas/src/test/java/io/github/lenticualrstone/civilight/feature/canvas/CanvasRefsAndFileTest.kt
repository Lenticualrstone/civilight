package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.feature.canvas.board.BoardScene
import io.github.lenticualrstone.civilight.feature.canvas.board.NodeLabels
import io.github.lenticualrstone.civilight.feature.canvas.common.RefFixes
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.feature.canvas.node.NodeEditModel
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.doc.toFileSet
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.ui.graph.NodeLook
import io.github.lenticualrstone.civilight.ui.graph.Pt
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** CNV-11 (deleted and hidden items on the canvas) and CNV-12 (canvas persistence). */
class CanvasRefsAndFileTest {
    private val ctx = CanvasFx.ctx()
    private val actx = ActionContext(now = ExampleClock.NOW, zone = ExampleClock.ZONE, device = "test-device", newKey = CanvasFx.ctx(7).newKey)
    private val nodeNoah = K.canvasNode(90)

    /** S0 with a node of 노아 (in the trash in S0) on `1장 구상`. */
    private fun withNoah() = CanvasFx.withNode(CanvasFx.s0(), nodeNoah, CanvasNodeKind.CHARACTER, K.NOAH, 40f, 40f)

    @Test
    fun `CNV-11 a node of a trashed item stays dotted with its restore and keep buttons`() {
        val doc = withNoah()
        val label = NodeLabels.of(doc, CanvasFx.canvas(doc).nodes.single { it.key == nodeNoah }, hiddenOpen = false)
        assertEquals("지운 인물 · 노아", label.title)
        assertEquals(NodeLook.DOTTED, label.look)
        val node = BoardScene.build(doc, CanvasFx.canvas(doc), CanvasFx.LATEST, false).nodesByKey.getValue(nodeNoah)
        assertEquals(NodeLook.DOTTED, node.look)
        assertEquals("지운 인물 · 노아", node.title) // the DS card draws the ◇ before it

        val view = NodeEditModel.build(doc, CanvasFx.CANVAS, nodeNoah, false, false)!!
        assertTrue(view.deleted)
        assertNull(view.openLabel)
        assertEquals("되살리기", view.restoreLabel)
        assertEquals("이대로 두기", view.keepLabel)
        assertEquals(ItemId.character(K.NOAH), view.item)

        // 되살리기: the node reads the name again, by key.
        val restored = RefFixes.restore(doc, view.item!!, ctx, authenticated = true).doc!!
        val live = NodeEditModel.build(restored, CanvasFx.CANVAS, nodeNoah, false, false)!!
        assertEquals("노아", live.title)
        assertFalse(live.deleted)
        assertEquals("인물 기록", live.openLabel)

        // 이대로 두기: stored once; the dotted mark stays and the button goes.
        val kept = RefFixes.keep(doc, view.item, ctx)
        assertTrue(Fixes.isKept(kept, K.NOAH))
        val afterKeep = NodeEditModel.build(kept, CanvasFx.CANVAS, nodeNoah, false, false)!!
        assertTrue(afterKeep.deleted)
        assertNull(afterKeep.keepLabel)

        // After the permanent delete: a name tag remains, no 되살리기.
        val purged = CharacterActions.purge(doc, ExampleRecorder.library(), K.NOAH, actx)!!.doc
        val tag = NodeEditModel.build(purged, CanvasFx.CANVAS, nodeNoah, false, false)!!
        assertEquals("영구 삭제 · 노아", tag.title)
        assertTrue(tag.deleted)
        assertNull(tag.restoreLabel)
    }

    @Test
    fun `CNV-11 a trashed event node restores through the trash entry`() {
        val doc = CanvasFx.s0()
        val trashed = TrashOps.delete(doc, ItemId.event(K.EVENT_BLACKOUT), actx.trash).doc
        val view = NodeEditModel.build(trashed, CanvasFx.CANVAS, CanvasFx.NODE_BLACKOUT, false, false)!!
        assertEquals("지운 사건 · 대정전", view.title)
        assertEquals("되살리기", view.restoreLabel)
        val back = RefFixes.restore(trashed, view.item!!, ctx, authenticated = true)
        assertNotNull(back.doc)
        assertEquals("대정전", NodeEditModel.build(back.doc!!, CanvasFx.CANVAS, CanvasFx.NODE_BLACKOUT, false, false)!!.title)
    }

    @Test
    fun `CNV-11 a converted line whose relation went to the trash draws plain`() {
        val doc = CanvasFx.s0()
        val before = BoardScene.build(doc, CanvasFx.canvas(doc), CanvasFx.LATEST, false).lines.single { it.key == CanvasFx.EDGE_RIEN_HART }
        assertEquals(io.github.lenticualrstone.civilight.ui.graph.LineStroke.DASHED, before.stroke)
        assertTrue(before.ends.isNotEmpty())

        // 하르트 goes to the trash with the relation it owns (rule 20 §2); the line and both nodes stay.
        val trashed = CharacterActions.delete(doc, K.HART, actx)!!.doc
        val scene = BoardScene.build(trashed, CanvasFx.canvas(trashed), CanvasFx.LATEST, false)
        val line = scene.lines.single { it.key == CanvasFx.EDGE_RIEN_HART }
        assertEquals(io.github.lenticualrstone.civilight.ui.graph.LineStroke.SOLID, line.stroke)
        assertTrue("no relation words", line.ends.isEmpty())
        assertEquals(K.REL_RIEN_HART, CanvasFx.edge(trashed, CanvasFx.EDGE_RIEN_HART).relationKey)
        assertEquals(NodeLook.DOTTED, scene.nodesByKey.getValue(CanvasFx.NODE_HART).look)
        assertEquals("지운 인물 · 하르트", scene.nodesByKey.getValue(CanvasFx.NODE_HART).title)
    }

    @Test
    fun `CNV-11 a hidden item while locked offers only 지문으로 보기`() {
        val s0 = CanvasFx.s0()
        val hidden = s0.copy(cast = s0.cast.copy(characters = s0.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
        val view = NodeEditModel.build(hidden, CanvasFx.CANVAS, CanvasFx.NODE_RIEN, false, false)!!
        assertEquals("숨긴 인물", view.title)
        assertEquals("지문으로 보기", view.unlockLabel)
        assertFalse(view.deleted)
        assertNull(view.restoreLabel)
        assertNull(NodeEditModel.build(hidden, CanvasFx.CANVAS, CanvasFx.NODE_RIEN, true, false)!!.unlockLabel)
    }

    @Test
    fun `CNV-12 the canvas file holds keys, positions, labels and relation keys only`() {
        val doc = CanvasFx.s0()
        val files = doc.toFileSet().encode()
        val file = files.getValue("canvas/${CanvasFx.CANVAS}.json").jsonObject
        assertEquals("1장 구상", file["name"]!!.jsonPrimitive.content)
        assertFalse("no viewport in the file", file.containsKey("viewport"))
        val nodes = file["nodes"]!!.jsonArray.map { it.jsonObject }
        assertEquals(6, nodes.size)
        val rien = nodes.single { it["key"]!!.jsonPrimitive.content == CanvasFx.NODE_RIEN }
        assertEquals(K.RIEN, rien["refKey"]!!.jsonPrimitive.content)
        assertTrue(rien.keys.containsAll(listOf("kind", "x", "y", "z", "updatedAt", "updatedBy")))
        assertFalse("item data is never copied", rien.containsKey("text") || rien.containsKey("name"))
        val edges = file["edges"] as JsonArray
        val line = edges.map { it.jsonObject }.single { it["key"]!!.jsonPrimitive.content == CanvasFx.EDGE_RIEN_HART }
        assertEquals(K.REL_RIEN_HART, line["relationKey"]!!.jsonPrimitive.content)
        assertFalse("relation data lives in the cast files", line.keys.any { it in setOf("between", "mind", "kind", "since") })
        // The viewport is app state (`_app/view.json`), never a canvas file.
        assertTrue(files.keys.none { it.startsWith("canvas/") && it != "canvas/${CanvasFx.CANVAS}.json" })
    }

    @Test
    fun `CNV-12 text is stored NFC and every write stamps time and device`() {
        val doc = CanvasFx.s0()
        val decomposed = java.text.Normalizer.normalize("등불", java.text.Normalizer.Form.NFD)
        val next = CanvasEdits.setText(doc, CanvasFx.CANVAS, CanvasFx.NODE_MEMO, decomposed, ctx)
        val memo = CanvasFx.canvas(next).nodes.single { it.key == CanvasFx.NODE_MEMO }
        assertEquals("등불", memo.text)
        assertEquals(ExampleClock.NOW, memo.updatedAt)
        assertEquals("test-device", memo.updatedBy)
        val (added, key) = CanvasEdits.addMemo(doc, CanvasFx.CANVAS, Pt(0f, 0f), ctx)
        val node = CanvasFx.canvas(added).nodes.single { it.key == key }
        assertTrue("ULID key", io.github.lenticualrstone.civilight.model.Keys.isValid(key))
        assertTrue("drawn on top", CanvasFx.canvas(doc).nodes.all { it.z < node.z })
    }

    @Test
    fun `CNV-02 the home shortcut opens the most recently opened canvas`() {
        val doc = CanvasFx.s0()
        val (two, second) = CanvasEdits.createCanvas(doc, "2장 구상", ctx)
        assertEquals(CanvasFx.CANVAS, CanvasEdits.resolveForRoute(two, null)!!.key)
        val opened = two.copy(app = two.app.copy(view = two.app.view.copy(lastCanvasKey = second)))
        assertEquals(second, CanvasEdits.resolveForRoute(opened, null)!!.key)
        assertEquals(CanvasFx.CANVAS, CanvasEdits.resolveForRoute(opened, CanvasFx.CANVAS)!!.key)
        // A trashed last canvas falls back to the first.
        val gone = opened.copy(canvases = opened.canvases + (second to opened.canvases.getValue(second).copy(deletedAt = 1L)))
        assertEquals(CanvasFx.CANVAS, CanvasEdits.resolveForRoute(gone, null)!!.key)
    }
}
