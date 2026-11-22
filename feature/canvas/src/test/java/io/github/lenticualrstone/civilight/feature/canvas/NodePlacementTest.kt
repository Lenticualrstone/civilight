package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.board.NodePlacement
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Rect
import io.github.lenticualrstone.civilight.ui.graph.Sz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** QA 2026-11-23 #10: new nodes land on the middle of the view and never on top of another node. */
class NodePlacementTest {
    private val size = NodePlacement.CARD
    private val centre = Pt(200f, 300f)

    private fun rectAt(c: Pt, s: Sz = size) = Rect.at(NodePlacement.topLeftFor(c, s), s)

    private fun overlap(a: Rect, b: Rect) = a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

    @Test
    fun `an empty view puts the node exactly on the centre`() {
        val at = NodePlacement.place(centre, size, emptyList())
        assertEquals(Pt(200f - 56f, 300f - 28f), at)
    }

    @Test
    fun `a free centre is kept even with nodes elsewhere`() {
        val far = rectAt(Pt(900f, 900f))
        assertEquals(NodePlacement.topLeftFor(centre, size), NodePlacement.place(centre, size, listOf(far)))
    }

    @Test
    fun `a node on the centre moves the new one to the nearest free spot`() {
        val blocker = rectAt(centre)
        val at = NodePlacement.place(centre, size, listOf(blocker))
        val placed = Rect.at(at, size)
        assertFalse("no overlap", overlap(placed, blocker))
        assertTrue("gap kept", placed.left >= blocker.right + NodePlacement.GAP - 0.01f || placed.right <= blocker.left - NodePlacement.GAP + 0.01f ||
            placed.top >= blocker.bottom + NodePlacement.GAP - 0.01f || placed.bottom <= blocker.top - NodePlacement.GAP + 0.01f)
        val d = Pt(placed.center.x - centre.x, placed.center.y - centre.y)
        assertTrue("stays near", kotlin.math.abs(d.x) <= size.w * 2 && kotlin.math.abs(d.y) <= size.h * 2)
    }

    @Test
    fun `many new nodes in a row never overlap each other or the old ones`() {
        val placed = mutableListOf(rectAt(centre), rectAt(Pt(centre.x + 130f, centre.y)))
        repeat(25) {
            val at = NodePlacement.place(centre, size, placed)
            val r = Rect.at(at, size)
            assertTrue("node $it is free", placed.none { overlap(r, it) })
            placed += r
        }
    }

    @Test
    fun `the same input gives the same spot`() {
        val occupied = listOf(rectAt(centre), rectAt(Pt(centre.x + 130f, centre.y)))
        assertEquals(NodePlacement.place(centre, size, occupied), NodePlacement.place(centre, size, occupied))
    }

    @Test
    fun `a group frame is placed by its own size`() {
        val frame = Sz(260f, 180f)
        val existing = rectAt(centre, frame)
        val at = NodePlacement.place(centre, frame, listOf(existing))
        assertFalse(overlap(Rect.at(at, frame), existing))
    }
}
