package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.ui.reorder.ReorderKey
import io.github.lenticualrstone.civilight.ui.reorder.ReorderMath
import io.github.lenticualrstone.civilight.ui.reorder.ReorderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shared `순서 바꾸기` rows (board `ImmPlaceRules`): drop target, drop line, a11y moves and the mode. */
class ReorderTest {
    private val rows = listOf(60f, 52f, 52f, 52f)

    @Test
    fun `a row passes a neighbour once its centre crosses the neighbour's centre`() {
        assertEquals(1, ReorderMath.target(rows, 1, 0f))
        // Row 1 centre 86; row 2 centre 138: 52 down is exactly on it (not past), 53 is past.
        assertEquals(1, ReorderMath.target(rows, 1, 52f))
        assertEquals(2, ReorderMath.target(rows, 1, 53f))
        assertEquals(3, ReorderMath.target(rows, 1, 500f))
        assertEquals(0, ReorderMath.target(rows, 1, -57f))
        assertEquals(1, ReorderMath.target(rows, 1, -56f))
        assertEquals(0, ReorderMath.target(rows, 3, -10_000f))
    }

    @Test
    fun `unmeasured rows stop the walk and a bad index does nothing`() {
        assertEquals(1, ReorderMath.target(listOf(60f, 52f, 0f, 52f), 1, 500f))
        assertEquals(7, ReorderMath.target(rows, 7, 50f))
    }

    @Test
    fun `the drop line sits on top moving up and under moving down, on the target row only`() {
        assertEquals(ReorderMath.Line.TOP, ReorderMath.line(index = 0, from = 2, to = 0))
        assertEquals(ReorderMath.Line.BOTTOM, ReorderMath.line(index = 3, from = 1, to = 3))
        assertEquals(ReorderMath.Line.NONE, ReorderMath.line(index = 2, from = 1, to = 3))
        assertEquals(ReorderMath.Line.NONE, ReorderMath.line(index = 1, from = 1, to = 1))
        assertEquals(ReorderMath.Line.NONE, ReorderMath.line(index = 0, from = -1, to = -1))
    }

    @Test
    fun `위로 옮기기 · 아래로 옮기기 stop at the ends`() {
        assertNull(ReorderMath.up(0))
        assertEquals(1, ReorderMath.up(2))
        assertEquals(3, ReorderMath.down(2, 4))
        assertNull(ReorderMath.down(3, 4))
        assertNull(ReorderMath.down(0, 1))
    }

    @Test
    fun `the button and a long press enter the same mode, and one row is picked at a time`() {
        val s = ReorderState()
        s.toggle()
        assertTrue(s.ordering)
        assertNull("the button picks nothing", s.selected)
        s.select(ReorderKey("factions", "f1"))
        assertTrue(s.isSelected("factions", "f1"))
        s.lift(ReorderKey("members:f1", "c2"))
        assertFalse(s.isSelected("factions", "f1"))
        assertTrue(s.isSelected("members:f1", "c2"))
        s.toggle()
        assertFalse(s.ordering)
        assertNull(s.selected)

        val t = ReorderState()
        t.lift(ReorderKey("loose", "c4"))
        assertTrue("a long press turns the mode on", t.ordering)
    }

    @Test
    fun `the pick is dropped when its row is no longer shown`() {
        val s = ReorderState()
        s.lift(ReorderKey("loose", "c4"))
        s.keepShown("factions", listOf("f1"))
        assertTrue(s.isSelected("loose", "c4"))
        s.keepShown("loose", listOf("c5"))
        assertNull(s.selected)
        assertTrue("the mode stays on", s.ordering)
    }
}
