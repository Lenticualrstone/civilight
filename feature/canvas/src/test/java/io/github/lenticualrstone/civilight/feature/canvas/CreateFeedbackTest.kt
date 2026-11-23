package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.CanvasFx.CANVAS
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasListEdits
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasListModel
import io.github.lenticualrstone.civilight.feature.canvas.common.CreatedScroll
import io.github.lenticualrstone.civilight.feature.canvas.page.JustCreatedPages
import io.github.lenticualrstone.civilight.feature.canvas.page.PageAutoEdit
import io.github.lenticualrstone.civilight.feature.canvas.page.PageEdits
import io.github.lenticualrstone.civilight.feature.canvas.page.PageViews
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** U-15 feedback after `만들기` / `만들고 열기`: page auto-edit, the new-row flash, the scroll target. */
class CreateFeedbackTest {
    private val ctx = CanvasFx.ctx()
    private val zone = ExampleClock.ZONE

    @After
    fun tearDown() = JustCreatedPages.clear()

    @Test
    fun `U-15 a named page made with 만들고 열기 opens into editing once`() {
        val (doc, key) = PageEdits.create(CanvasFx.s0(), "새 페이지 이름", ctx)
        val view = PageViews.build(doc, key, hiddenOpen = false, readOnly = false, zone = zone)!!
        assertFalse("a named page is not blank", view.isBlank)
        // `만들기` (no mark): the page opens in the read view later.
        assertFalse(PageAutoEdit.shouldStart(view, JustCreatedPages.take(key)))
        // `만들고 열기`: the list marks it, the page screen reads the mark once.
        JustCreatedPages.mark(key)
        assertTrue(PageAutoEdit.shouldStart(view, JustCreatedPages.take(key)))
        assertFalse("the mark is read once", PageAutoEdit.shouldStart(view, JustCreatedPages.take(key)))
    }

    @Test
    fun `U-15 a blank page still opens into editing and read-only never does`() {
        val (doc, key) = PageEdits.create(CanvasFx.s0(), ctx)
        val blank = PageViews.build(doc, key, hiddenOpen = false, readOnly = false, zone = zone)!!
        assertTrue(PageAutoEdit.shouldStart(blank, justCreated = false))
        val readOnly = PageViews.build(doc, key, hiddenOpen = false, readOnly = true, zone = zone)!!
        assertFalse(PageAutoEdit.shouldStart(readOnly, justCreated = true))
        // An existing page with text is never auto-edited.
        val existing = PageViews.build(CanvasFx.s0(), K.PAGE_LOWER_LIGHT, hiddenOpen = false, readOnly = false, zone = zone)!!
        assertFalse(PageAutoEdit.shouldStart(existing, justCreated = false))
    }

    @Test
    fun `U-15_6 the new canvas row is highlighted, not current`() {
        val (doc, made) = requireNotNull(CanvasListEdits.create(CanvasFx.s0(), "2장 구상", ctx))
        val view = CanvasListModel.build(doc, CANVAS, readOnly = false, highlightedKey = made)
        val open = view.rows.single { it.key == CANVAS }
        val new = view.rows.single { it.key == made }
        assertTrue(open.current)
        assertFalse(open.highlighted)
        assertTrue(new.highlighted)
        assertFalse("only the open canvas is current", new.current)
        assertEquals(1, view.rows.count { it.current })
    }

    @Test
    fun `U-15_6 the scroll target skips leading items and waits for the row`() {
        val rows = listOf("a", "b", "c")
        assertEquals(4, CreatedScroll.index(rows, "c", leading = 2))
        assertEquals(2, CreatedScroll.index(rows, "c", leading = 0))
        assertNull("not in the list yet", CreatedScroll.index(rows, "d", leading = 2))
        assertNull(CreatedScroll.index(rows, null, leading = 2))
    }
}
