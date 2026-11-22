package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.editor.CaretEvent
import io.github.lenticualrstone.civilight.feature.script.editor.WritingLine
import io.github.lenticualrstone.civilight.feature.script.editor.WritingLineTracker
import org.junit.Assert.assertEquals
import org.junit.Test

/** SCR-14: the line holding the caret keeps its screen position; content above moves. */
class WritingLineTest {
    @Test
    fun `SCR-14 the caret line goes back to where it was`() {
        // The line grew downward by 30 px (a new block below after Enter): content scrolls up 30.
        val a = WritingLine.adjust(anchorTop = 400f, caretTop = 430f, caretBottom = 460f, visibleTop = 76f, visibleBottom = 700f)
        assertEquals(30f, a.scrollBy, 0.01f)
        assertEquals(400f, a.anchorTop, 0.01f)
    }

    @Test
    fun `SCR-14 when the keyboard covers the line the manuscript scrolls by the least amount`() {
        val a = WritingLine.adjust(anchorTop = 600f, caretTop = 600f, caretBottom = 630f, visibleTop = 76f, visibleBottom = 450f)
        assertEquals(180f, a.scrollBy, 0.01f)
        assertEquals(420f, a.anchorTop, 0.01f)
    }

    @Test
    fun `a line under the top bar is brought below it`() {
        val a = WritingLine.adjust(anchorTop = 40f, caretTop = 40f, caretBottom = 70f, visibleTop = 76f, visibleBottom = 700f)
        assertEquals(-36f, a.scrollBy, 0.01f)
        assertEquals(76f, a.anchorTop, 0.01f)
    }

    @Test
    fun `the tracker anchors on the first report and keeps that place while typing`() {
        val t = WritingLineTracker()
        assertEquals(0f, t.onCaret(CaretEvent.LAYOUT, 300f, 330f, 76f, 700f), 0.01f)
        assertEquals(300f, t.anchorTop!!, 0.01f)
        assertEquals(30f, t.onCaret(CaretEvent.EDIT, 330f, 360f, 76f, 700f), 0.01f)
        assertEquals(0f, t.onCaret(CaretEvent.LAYOUT, 250f, 280f, 76f, 700f), 0.01f)
    }

    @Test
    fun `a drag by the writer moves the place to keep`() {
        val t = WritingLineTracker()
        t.onCaret(CaretEvent.EDIT, 300f, 330f, 76f, 700f)
        assertEquals(0f, t.onCaret(CaretEvent.USER_SCROLL, 200f, 230f, 76f, 700f), 0.01f)
        assertEquals(200f, t.anchorTop!!, 0.01f)
        assertEquals(10f, t.onCaret(CaretEvent.EDIT, 210f, 240f, 76f, 700f), 0.01f)
    }

    @Test
    fun `the keyboard opening keeps the line visible`() {
        val t = WritingLineTracker()
        t.onCaret(CaretEvent.EDIT, 600f, 630f, 76f, 800f)
        assertEquals(180f, t.onCaret(CaretEvent.VIEWPORT, 600f, 630f, 76f, 450f), 0.01f)
        t.reset()
        assertEquals(null, t.anchorTop)
    }
}
