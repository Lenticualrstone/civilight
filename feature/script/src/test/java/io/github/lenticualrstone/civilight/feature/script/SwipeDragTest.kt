package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.motion.SwipeDrag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CNV-40 swipe row (emulator QA: a fast drag left the row between open and closed). Every delta lands
 * before the release with no coroutine in between, the release sees the last one, and each release asks
 * for a settle even when the open state did not change.
 */
class SwipeDragTest {
    private val width = 576f

    @Test
    fun `fast drag - every delta applies at once and the release sees the last one`() {
        val drag = SwipeDrag(open = false, widthPx = width)
        repeat(20) { drag.onDelta(-20f) }
        assertTrue(drag.dragging)
        assertEquals(-400f, drag.offset, 0f)
        val releasesBefore = drag.releases
        assertTrue(drag.release())
        assertFalse(drag.dragging)
        assertEquals(releasesBefore + 1, drag.releases)
    }

    @Test
    fun `open then close repeatedly - the decision follows the finger, never a stale offset`() {
        val drag = SwipeDrag(open = false, widthPx = width)
        repeat(3) {
            repeat(10) { drag.onDelta(-40f) }
            assertTrue(drag.release())
            drag.place(SwipeDrag.anchor(true, width))
            assertEquals(-width, drag.offset, 0f)
            repeat(10) { drag.onDelta(40f) }
            assertFalse(drag.release())
            drag.place(SwipeDrag.anchor(false, width))
            assertEquals(0f, drag.offset, 0f)
        }
        assertEquals(6, drag.releases)
    }

    @Test
    fun `deltas are kept between open and closed`() {
        val drag = SwipeDrag(open = true, widthPx = width)
        assertEquals(-width, drag.offset, 0f)
        drag.onDelta(-500f)
        assertEquals(-width, drag.offset, 0f)
        drag.onDelta(2000f)
        assertEquals(0f, drag.offset, 0f)
        assertEquals(0f, drag.progress, 0f)
    }

    @Test
    fun `released past half stays open, short of half closes`() {
        assertTrue(SwipeDrag.opensOnRelease(-width / 2 - 1f, width))
        assertFalse(SwipeDrag.opensOnRelease(-width / 2, width))
        assertFalse(SwipeDrag.opensOnRelease(-100f, width))
        assertEquals(-width, SwipeDrag.anchor(true, width), 0f)
        assertEquals(0f, SwipeDrag.anchor(false, width), 0f)
    }

    @Test
    fun `reduced motion - the buttons' fade follows the finger`() {
        val drag = SwipeDrag(open = false, widthPx = width)
        assertEquals(0f, drag.fade, 0f)
        drag.onDelta(-width / 4)
        assertEquals(0.25f, drag.fade, 1e-4f)
        assertEquals(0.25f, drag.progress, 1e-4f)
    }

    @Test
    fun `a drag after a settle starts from where the row is drawn`() {
        val drag = SwipeDrag(open = false, widthPx = width)
        drag.place(-300f) // mid settle
        drag.onDelta(-10f)
        assertEquals(-310f, drag.offset, 0f)
    }

    @Test
    fun `a settle frame landing after the finger took the row is ignored`() {
        val drag = SwipeDrag(open = false, widthPx = width)
        drag.place(-300f)
        drag.onDelta(-10f)
        drag.place(-200f) // late frame of the cancelled settle
        drag.placeFade(0f)
        assertEquals(-310f, drag.offset, 0f)
        assertEquals(310f / width, drag.fade, 1e-4f)
        drag.release()
        drag.place(-width)
        assertEquals(-width, drag.offset, 0f)
    }

    @Test
    fun `a drag cut off without a release stops being a drag and asks for a settle`() {
        val drag = SwipeDrag(open = false, widthPx = width)
        drag.onDelta(-100f)
        val before = drag.releases
        drag.cancel()
        assertFalse(drag.dragging)
        assertEquals(before + 1, drag.releases)
        drag.cancel() // no drag: nothing
        assertEquals(before + 1, drag.releases)
    }
}
