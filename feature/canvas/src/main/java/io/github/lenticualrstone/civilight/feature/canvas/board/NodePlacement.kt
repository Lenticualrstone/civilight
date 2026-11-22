package io.github.lenticualrstone.civilight.feature.canvas.board

import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Rect
import io.github.lenticualrstone.civilight.ui.graph.Sz

/**
 * Where a new node goes (CNV-04 … CNV-06, QA 2026-11-23 #10): centred on the middle of the visible canvas,
 * moved to the nearest free spot when that would sit on another node. Pure, so it is tested on the JVM; the
 * screen feeds it the viewport centre in content dp and the rectangles of the nodes already there.
 */
object NodePlacement {
    /** Space left between a new node and its neighbours (dp). */
    const val GAP = 12f

    /**
     * What a not yet measured item or memo card is assumed to take (dp): a two-line card of about six Korean
     * characters. Only used to pick the spot; the card then measures itself.
     */
    val CARD = Sz(112f, 56f)

    private const val MAX_RINGS = 40

    /** The top-left of a node of [size] centred on [center]. */
    fun topLeftFor(center: Pt, size: Sz): Pt = Pt(center.x - size.w / 2f, center.y - size.h / 2f)

    /**
     * The top-left for a node of [size] near [center]: exactly centred when free, else on the nearest free
     * grid spot around it (steps of the node's own size plus [gap]). The same input always gives the same spot.
     */
    fun place(center: Pt, size: Sz, occupied: List<Rect>, gap: Float = GAP): Pt {
        val blocked = occupied.map { it.inflate(gap) }
        fun free(c: Pt): Boolean {
            val r = Rect.at(topLeftFor(c, size), size)
            return blocked.none { overlaps(r, it) }
        }
        if (free(center)) return topLeftFor(center, size)
        val stepX = size.w + gap
        val stepY = size.h + gap
        for (k in 1..MAX_RINGS) {
            val ring = buildList {
                for (i in -k..k) for (j in -k..k) if (maxOf(kotlin.math.abs(i), kotlin.math.abs(j)) == k) add(Pt(center.x + i * stepX, center.y + j * stepY))
            }
            val spot = ring
                .sortedWith(compareBy({ (it.x - center.x) * (it.x - center.x) + (it.y - center.y) * (it.y - center.y) }, { it.y }, { it.x }))
                .firstOrNull(::free)
            if (spot != null) return topLeftFor(spot, size)
        }
        return topLeftFor(center, size)
    }

    private fun overlaps(a: Rect, b: Rect): Boolean = a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
}
