package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateFields
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.actions.CanvasActions
import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.CANVAS
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasListEdits
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasListModel
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasListSheet
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasRename
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasRowMenuSheet
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.feature.canvas.page.PageEdits
import io.github.lenticualrstone.civilight.feature.canvas.page.PageListModel
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.RouteCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** N-58 several canvases and U-15 (pages, canvases): every write goes through `CanvasActions`. */
class CanvasListTest {
    private val ctx = CanvasFx.ctx()

    private fun opened(doc: ProjectDoc, key: String): ProjectDoc = doc.copy(app = CanvasActions.opened(doc.app, key))

    @Test
    fun `N-58 create adds an empty canvas at the end of the list`() {
        val s0 = CanvasFx.s0()
        val before = CanvasListModel.build(s0, CANVAS, readOnly = false)
        assertEquals(1, before.rows.size)
        assertTrue(before.canCreate)
        val (doc, key) = requireNotNull(CanvasListEdits.create(s0, "  2장 구상 ", ctx))
        val after = CanvasListModel.build(doc, CANVAS, readOnly = false)
        assertEquals(listOf(CANVAS, key), after.rows.map { it.key })
        assertEquals("2장 구상", after.rows.last().name)
        assertEquals(0, after.rows.last().nodes)
        assertTrue(after.rows.first().current)
        assertFalse(after.rows.last().current)
        // The first canvas's nodes are untouched (D25).
        assertEquals(CanvasFx.canvas(s0).nodes, CanvasFx.canvas(doc).nodes)
    }

    @Test
    fun `N-58 a blank name makes nothing and the create card keeps 만들기 off`() {
        assertNull(CanvasListEdits.create(CanvasFx.s0(), "   ", ctx))
        val card = CreateCardState.open(CreateKind.CANVAS, CreateCardMode.LIST)
        assertFalse(card.canCreate)
        assertTrue(card.with(CreateFields.NAME, "2장").canCreate)
        assertEquals(listOf(CreateFields.NAME), CreateKind.CANVAS.fields.map { it.key })
        assertEquals(listOf(CreateFields.NAME), CreateKind.PAGE.fields.map { it.key })
        assertEquals("새 캔버스", CreateCard.title(CreateKind.CANVAS))
        assertEquals("새 페이지", CreateCard.title(CreateKind.PAGE))
    }

    @Test
    fun `N-58 switch and last opened decide what home 캔버스 opens`() {
        val (made, second) = requireNotNull(CanvasListEdits.create(CanvasFx.s0(), "2장 구상", ctx))
        // Home pushes `CanvasBoard(p)` without a key.
        assertEquals(CANVAS, CanvasEdits.resolveForRoute(opened(made, CANVAS), null)?.key)
        val switched = opened(made, second)
        assertEquals(second, CanvasActions.openTarget(switched))
        assertEquals(second, CanvasEdits.resolveForRoute(switched, null)?.key)
        // A requested canvas wins over the last opened one.
        assertEquals(CANVAS, CanvasEdits.resolveForRoute(switched, CANVAS)?.key)
        // Opening the same canvas again changes nothing.
        assertTrue(CanvasActions.opened(switched.app, second) === switched.app)
    }

    @Test
    fun `N-58 delete to the trash and restore brings nodes and lines back`() {
        val s0 = CanvasFx.s0()
        val (made, second) = requireNotNull(CanvasListEdits.create(s0, "2장 구상", ctx))
        val doc = opened(made, CANVAS)
        val nodes = CanvasFx.canvas(doc).nodes
        val edges = CanvasFx.canvas(doc).edges
        assertTrue(nodes.isNotEmpty() && edges.isNotEmpty())

        val deleted = requireNotNull(CanvasListEdits.delete(doc, CANVAS, ctx))
        assertTrue(deleted.toast.message.isNotBlank())
        val afterDelete = CanvasListModel.build(deleted.doc, null, readOnly = false)
        assertEquals(listOf(second), afterDelete.rows.map { it.key })
        // The deleted canvas was the last opened: home and the open screen fall back to a live one.
        assertEquals(second, CanvasListModel.afterDelete(deleted.doc))
        assertEquals(second, CanvasEdits.resolveForRoute(deleted.doc, null)?.key)
        assertEquals(second, CanvasEdits.resolveForRoute(deleted.doc, CANVAS)?.key)

        val restored = requireNotNull(CanvasListEdits.restore(deleted.doc, deleted.entryKey, ctx))
        assertEquals(listOf(CANVAS, second), CanvasListModel.build(restored, CANVAS, readOnly = false).rows.map { it.key })
        assertEquals(nodes, CanvasFx.canvas(restored).nodes)
        assertEquals(edges, CanvasFx.canvas(restored).edges)
        assertNull(CanvasFx.canvas(restored).deletedAt)
    }

    @Test
    fun `N-58 the only live canvas cannot be deleted`() {
        val s0 = CanvasFx.s0()
        assertFalse(CanvasListModel.canDelete(s0, CANVAS, readOnly = false))
        assertFalse(CanvasListModel.build(s0, CANVAS, readOnly = false).rows.single().canDelete)
        assertNull(CanvasListEdits.delete(s0, CANVAS, ctx))
        val (two, second) = requireNotNull(CanvasListEdits.create(s0, "2장 구상", ctx))
        assertTrue(CanvasListModel.canDelete(two, CANVAS, readOnly = false))
        assertTrue(CanvasListModel.canDelete(two, second, readOnly = false))
    }

    @Test
    fun `N-58 rename trims, refuses a blank name and keeps the canvas otherwise`() {
        val s0 = CanvasFx.s0()
        val renamed = CanvasListEdits.rename(s0, CANVAS, "  1장 · 다시 ", ctx)
        assertEquals("1장 · 다시", CanvasFx.canvas(renamed).name)
        assertTrue(CanvasListEdits.rename(s0, CANVAS, "  ", ctx) === s0)
        val current = CanvasFx.canvas(s0).name
        assertFalse(CanvasRename.canRename(current, current))
        assertFalse(CanvasRename.canRename(" ", current))
        assertTrue(CanvasRename.canRename("새 이름", current))
    }

    @Test
    fun `U-15_10 read-only projects show rows only`() {
        val (two, _) = requireNotNull(CanvasListEdits.create(CanvasFx.s0(), "2장 구상", ctx))
        val view = CanvasListModel.build(two, CANVAS, readOnly = true)
        assertFalse(view.canCreate)
        assertFalse(view.canEdit)
        assertTrue(view.rows.none { it.canDelete })
        assertEquals(2, view.rows.size)
    }

    @Test
    fun `U-15 pages are made from the create card with their name`() {
        val s0 = CanvasFx.s0()
        val before = PageEdits.pages(s0).size
        val (doc, key) = PageEdits.create(s0, " 설정 노트 ", ctx)
        assertEquals(before + 1, PageEdits.pages(doc).size)
        assertEquals("설정 노트", doc.pages.getValue(key).name)
        assertEquals(key, PageEdits.pages(doc).last().key)
        val list = PageListModel.build(doc, hiddenOpen = false, readOnly = false, zone = CanvasFx.ctx().zone)
        assertTrue(list.canCreate)
        assertTrue(list.rows.any { it.key == key && it.name == "설정 노트" })
        assertFalse(PageListModel.build(doc, hiddenOpen = false, readOnly = true, zone = CanvasFx.ctx().zone).canCreate)
        // With 0 pages the create row stays (U-15.1).
        val none = PageListModel.build(s0.copy(pages = emptyMap()), hiddenOpen = false, readOnly = false, zone = CanvasFx.ctx().zone)
        assertTrue(none.isEmpty && none.canCreate)
    }

    @Test
    fun `N-58 list sheets survive the saved sheet stack`() {
        val keys = listOf(CanvasListSheet(K.PROJECT, CANVAS), CanvasRowMenuSheet(K.PROJECT, CANVAS, CANVAS))
        for (key in keys) assertEquals(key, RouteCodec.decode(RouteCodec.encode(key)))
    }
}
