package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.foundation.GestureAxis
import io.github.lenticualrstone.civilight.designsystem.foundation.GestureRules
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.3 touch arbitration: long press, swipe rows, pagers and vertical scroll share a finger without fighting. */
class GestureRulesTest {
    private val slop = 8f

    @Test
    fun `nothing is decided inside the slop`() {
        assertNull(GestureRules.axis(0f, 0f, slop))
        assertNull(GestureRules.axis(7f, 7f, slop))
        assertNull(GestureRules.axis(-7.9f, 3f, slop))
    }

    @Test
    fun `sideways only when clearly sideways`() {
        assertEquals(GestureAxis.HORIZONTAL, GestureRules.axis(9f, 0f, slop))
        assertEquals(GestureAxis.HORIZONTAL, GestureRules.axis(-12f, 8f, slop))
        // 45 degrees: not sideways; it becomes vertical once the vertical part reaches the slop.
        assertNull(GestureRules.axis(9f, 7f, slop))
        assertEquals(GestureAxis.VERTICAL, GestureRules.axis(9f, 8f, slop))
        assertEquals(GestureAxis.VERTICAL, GestureRules.axis(2f, -20f, slop))
    }

    @Test
    fun `a closed row lets a drag it cannot follow through`() {
        // Left-only row (0 … -192) at rest: a left drag is its own, a right drag goes to the pager behind it.
        assertTrue(GestureRules.canMove(-10f, 0f, -192f, 0f))
        assertFalse(GestureRules.canMove(10f, 0f, -192f, 0f))
        // Once open it can be pushed back.
        assertTrue(GestureRules.canMove(10f, -192f, -192f, 0f))
        assertFalse(GestureRules.canMove(-10f, -192f, -192f, 0f))
        assertFalse(GestureRules.canMove(0f, -50f, -192f, 0f))
    }

    @Test
    fun `long press ends when the finger travels past the slop`() {
        assertFalse(GestureRules.cancelsLongPress(5f, 5f, slop))
        assertTrue(GestureRules.cancelsLongPress(6f, 6f, slop))
    }

    @Test
    fun `panel slide token and icon`() {
        assertEquals(320, CivilightMotion.panelSlide.durationMs)
        assertEquals(CivilightMotion.standardCurve, CivilightMotion.panelSlide.curve)
        assertEquals("M4 5h16v14H4zM9 5v14", IconId.Panel.pathData)
    }
}
