package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.component.CaretInView
import io.github.lenticualrstone.civilight.designsystem.component.CaretInView.Geometry
import io.github.lenticualrstone.civilight.designsystem.component.CaretInView.Span
import io.github.lenticualrstone.civilight.designsystem.component.CaretInView.Typing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** U-02: when the caret line of a growing field is brought into view, and how far it scrolls. */
class CaretInViewTest {
    private fun typing(text: String, caret: Int = text.length, focused: Boolean = true) = Typing(focused, text, caret, caret)

    @Test
    fun `focus arriving is left to the field and nothing happens without focus`() {
        val first = CaretInView.step(null, typing("가"))
        assertFalse(first.bring)
        assertEquals(typing("가"), first.seen)
        val off = CaretInView.step(typing("가"), typing("가나", focused = false))
        assertFalse(off.bring)
        assertNull(off.seen)
    }

    @Test
    fun `a new line or a caret move while focused brings the caret in`() {
        val seen = typing("첫 줄")
        assertTrue(CaretInView.step(seen, typing("첫 줄\n")).bring)
        assertTrue(CaretInView.step(seen, typing("첫 줄", caret = 1)).bring)
        // Until the bring has finished, the caller keeps the old state, so a cut-short bring is done again.
        assertEquals(seen, CaretInView.step(seen, typing("첫 줄\n")).seen)
    }

    @Test
    fun `a range selection without a text change does not bring, a text change does`() {
        val seen = typing("긴 글 한 줄\n둘째 줄", caret = 3)
        val all = Typing(true, seen.text, 0, seen.text.length)
        val step = CaretInView.step(seen, all)
        assertFalse(step.bring)
        assertEquals(all, step.seen)
        assertTrue(CaretInView.step(all, Typing(true, "x", 1, 1)).bring)
    }

    @Test
    fun `a new layout of the same text does not bring`() {
        val seen = typing("같은 글")
        assertFalse(CaretInView.step(seen, typing("같은 글")).bring)
    }

    // Screen 2000 px tall: top bar 200, keyboard from 1200 down (no bottom bar shown above it), margin 24.
    private val screen = Span(0f, 2000f)
    private val screenClear = Span(200f, 1200f)

    @Test
    fun `on a screen the caret under the keyboard scrolls just above the keyboard`() {
        // The field box (900..1400) is fully inside the screen's scroll viewport; its caret line is 1340..1380.
        val g = Geometry(Span(1340f, 1380f), Span(900f, 1400f), Span(900f, 1400f), screen, screenClear, 24f)
        val span = CaretInView.target(g)!!
        // The viewport is the screen (2000): the span lies 204 px past it, so the content scrolls 204 px and the
        // caret line ends at 1380 - 204 = 1176, 24 px above the keyboard.
        assertEquals(2000f + 204f, span.bottom, 0.01f)
        assertEquals(span.bottom - 40f - 24f, span.top, 0.01f)
    }

    @Test
    fun `a caret line already clear of the keyboard and bars stays put`() {
        val g = Geometry(Span(1000f, 1040f), Span(900f, 1400f), Span(900f, 1400f), screen, screenClear, 24f)
        assertNull(CaretInView.target(g))
    }

    @Test
    fun `in a sheet the cut at the sheet's scroll edge is the viewport`() {
        // The sheet's scroll body ends at 1150 (above the keyboard); the box runs on to 1300, cut at 1150.
        // Outside a glass scaffold the frame is the window and only the keyboard is in the way.
        val g = Geometry(Span(1160f, 1200f), Span(800f, 1300f), Span(800f, 1150f), screen, Span(0f, 1200f), 24f)
        val span = CaretInView.target(g)!!
        // Scroll by 1200 + 24 - 1150 = 74: the span is the caret line itself plus the margin.
        assertEquals(1224f, span.bottom, 0.01f)
        assertEquals(1160f, span.top, 0.01f)
    }

    @Test
    fun `a caret line under the top bar scrolls down below it`() {
        val g = Geometry(Span(150f, 190f), Span(100f, 600f), Span(100f, 600f), screen, screenClear, 24f)
        val span = CaretInView.target(g)!!
        // 200 - (150 - 24) = 74 above the viewport top (0).
        assertEquals(-74f, span.top, 0.01f)
    }

    @Test
    fun `a field scrolled out of sight brings the plain caret line`() {
        val g = Geometry(Span(2600f, 2640f), Span(2500f, 2700f), null, screen, screenClear, 24f)
        assertEquals(Span(2576f, 2664f), CaretInView.target(g))
    }

    @Test
    fun `a bottom bar riding on the keyboard is cleared too`() {
        // Bottom bar of 300 px including the keyboard lift: clear ends at 2000 - 300 = 1700 vs keyboard 1800.
        val g = Geometry(Span(1690f, 1730f), Span(1000f, 1740f), Span(1000f, 1740f), screen, Span(200f, 1700f), 24f)
        val span = CaretInView.target(g)!!
        // 1730 + 24 - 1700 = 54 past the screen's end.
        assertEquals(2054f, span.bottom, 0.01f)
    }
}
