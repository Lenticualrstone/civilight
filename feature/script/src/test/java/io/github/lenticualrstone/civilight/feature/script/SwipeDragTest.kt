package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.designsystem.component.SwipeSide
import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.script.motion.BlockSwipe
import io.github.lenticualrstone.civilight.feature.script.motion.ScriptMotion
import io.github.lenticualrstone.civilight.feature.script.motion.SwipeDrag
import io.github.lenticualrstone.civilight.feature.script.motion.TwoSideDrag
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

    @Test
    fun `0_2_4 gesture rules - a two-sided row takes a drag only in a direction it can move`() {
        val drag = TwoSideDrag()
        val ref = 192f
        val edit = 300f
        // Closed: left opens the edit side (right end), right the reference side (left end).
        assertTrue(drag.canStart(-10f, ref, edit, refOn = true, editOn = true))
        assertTrue(drag.canStart(10f, ref, edit, refOn = true, editOn = true))
        // No edit side (read-only, during 고치기): a leftward drag is left to the screen behind the row.
        assertFalse(drag.canStart(-10f, ref, edit, refOn = true, editOn = false))
        assertTrue(drag.canStart(10f, ref, edit, refOn = true, editOn = false))
        // Fully open on the edit side: further left is not this row's; right closes it.
        drag.onDelta(-edit, ref, edit, refOn = true, editOn = true)
        assertEquals(SwipeSide.EDIT, drag.release(ref, edit, refOn = true, editOn = true))
        assertFalse(drag.canStart(-10f, ref, edit, refOn = true, editOn = true))
        assertTrue(drag.canStart(10f, ref, edit, refOn = true, editOn = true))
    }

    @Test
    fun `0_2_4 sides - left swipe opens the edit buttons at the right end, right swipe the reference at the left`() {
        val ref = 192f
        val edit = 300f
        assertEquals(-edit, BlockSwipe.restOffset(SwipeSide.EDIT, ref, edit), 0f)
        assertEquals(ref, BlockSwipe.restOffset(SwipeSide.REFERENCE, ref, edit), 0f)
        assertEquals(0f, BlockSwipe.restOffset(SwipeSide.CLOSED, ref, edit), 0f)
        // Past half a side stays open; short of half closes.
        assertEquals(SwipeSide.EDIT, BlockSwipe.settle(-151f, ref, edit, refOn = true, editOn = true))
        assertEquals(SwipeSide.CLOSED, BlockSwipe.settle(-149f, ref, edit, refOn = true, editOn = true))
        assertEquals(SwipeSide.REFERENCE, BlockSwipe.settle(97f, ref, edit, refOn = true, editOn = true))
        assertEquals(SwipeSide.CLOSED, BlockSwipe.settle(95f, ref, edit, refOn = true, editOn = true))
        // No edit side: a left drag never moves the row, and an EDIT request rests closed.
        assertEquals(0f, BlockSwipe.clamp(-250f, ref, edit, refOn = true, editOn = false), 0f)
        assertEquals(SwipeSide.CLOSED, BlockSwipe.settle(-299f, ref, edit, refOn = true, editOn = false))
        assertEquals(SwipeSide.CLOSED, BlockSwipe.wanted(SwipeSide.EDIT, refOn = true, editOn = false))
        assertEquals(SwipeSide.REFERENCE, BlockSwipe.wanted(SwipeSide.REFERENCE, refOn = true, editOn = false))
        assertEquals(-edit, BlockSwipe.clamp(-500f, ref, edit, refOn = true, editOn = true), 0f)
        assertEquals(ref, BlockSwipe.clamp(500f, ref, edit, refOn = true, editOn = true), 0f)
        // Only the side being revealed is composed.
        assertTrue(BlockSwipe.showsEdit(-edit, editOn = true) && !BlockSwipe.showsReference(-edit, refOn = true))
        assertTrue(BlockSwipe.showsReference(ref, refOn = true) && !BlockSwipe.showsEdit(ref, editOn = true))
        assertFalse(BlockSwipe.showsEdit(0f, editOn = true) || BlockSwipe.showsReference(0f, refOn = true))
        assertFalse(BlockSwipe.showsEdit(-edit, editOn = false))
        assertEquals(1f, BlockSwipe.editProgress(-edit, edit), 0f)
        assertEquals(0.5f, BlockSwipe.referenceProgress(ref / 2, ref), 1e-4f)
    }

    @Test
    fun `0_2_4 open row - every button is fully opaque, the five edit buttons included`() {
        (0 until 5).forEach { i ->
            val p = ScriptMotion.buttonProgress(1f, i, reduced = false, count = 5)
            assertEquals(1f, p, 0f)
            assertEquals(1f, BlockSwipe.buttonAlpha(p, enabled = true), 0f)
        }
        // Mid-slide the stagger still runs in order, and the last button starts before the slide ends.
        val half = (0 until 5).map { ScriptMotion.buttonProgress(0.5f, it, reduced = false, count = 5) }
        assertEquals(half.sortedDescending(), half)
        assertTrue(ScriptMotion.buttonProgress(0.9f, 4, reduced = false, count = 5) > 0.5f)
        // Three buttons keep the 0.2.0 timing (60 ms apart, 140 ms each).
        assertEquals(60f to 140f, ScriptMotion.stagger(3))
        // A disabled button stays dimmed.
        assertEquals(0.4f, BlockSwipe.buttonAlpha(1f, enabled = false), 1e-4f)
    }

    @Test
    fun `the script tour's block step names the swipe that opens 시간 · 참고 · 속성`() {
        // 0.2.4 review: the sides were swapped but the tour still said `왼쪽으로`. In the read-only example the edit
        // side is empty, so a left swipe moves nothing there; the tour must name the right swipe.
        val step = ModuleTours.steps(TourModule.SCRIPT)[ScriptTourSteps.BLOCK]
        assertTrue(step.sub.startsWith("시간 · 참고 · 속성"))
        assertEquals("블록을 오른쪽으로", step.title)
        assertTrue(BlockSwipe.restOffset(SwipeSide.REFERENCE, refPx = 192f, editPx = 300f) > 0f)
        // Read-only example: no edit side, so a left drag cannot start and a right drag reaches the reference side.
        val drag = TwoSideDrag()
        assertFalse(drag.canStart(-10f, 192f, 300f, refOn = true, editOn = false))
        assertTrue(drag.canStart(10f, 192f, 300f, refOn = true, editOn = false))
        drag.onDelta(150f, 192f, 300f, refOn = true, editOn = false)
        assertEquals(SwipeSide.REFERENCE, drag.release(192f, 300f, refOn = true, editOn = false))
    }
}
