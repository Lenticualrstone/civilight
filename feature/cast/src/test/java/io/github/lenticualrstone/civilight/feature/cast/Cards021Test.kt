package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardPoses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** 0.2.1 QA fixes of the 3D card views: board-size cards everywhere, the fan's `deck_t`, every 타로 card reachable. */
class Cards021Test {
    @Test
    fun cardsAreDrawnOneBoardPxPerDpWhateverThePane() {
        for (density in listOf(1f, 2.625f, 3.5f)) assertEquals(density, CardPoses.boardUnit(density), 0f)
        assertEquals(220f, CardPoses.DECK_W, 0f)
        assertEquals(308f, CardPoses.DECK_H, 0f)
        assertEquals(168f, CardPoses.RING_W, 0f)
        assertEquals(270f, CardPoses.PILE_W, 0f)
        assertEquals(342f, CardPoses.ROLO_W, 0f)
        assertEquals(84f, CardPoses.TAROT_W, 0f)
        assertEquals(92f, CardPoses.WALL_CW, 0f)
    }

    @Test
    fun fanFollowsTheBoardsDeckT() {
        val c = CardPoses.fan(0f)!!
        assertEquals(0f, c.x, 1e-4f)
        assertEquals(-30f, c.y, 1e-4f)
        assertEquals(40f, c.z, 1e-4f)
        assertEquals(1f, c.scale, 0f)
        for (p in listOf(1f, -1f, 3f, -3f)) {
            val f = CardPoses.fan(p)!!
            val a = Math.toRadians(p * 9.0)
            assertEquals((sin(a) * 520).toFloat(), f.x, 1e-3f)
            assertEquals(((1 - cos(a)) * 520).toFloat(), f.y, 1e-3f)
            assertEquals(-kotlin.math.abs(p) * 36f, f.z, 1e-3f)
            assertEquals(p * 9f, f.rotZ, 1e-4f)
            assertEquals(-p * 7f, f.rotY, 1e-4f)
            assertEquals(1f - kotlin.math.abs(p) * 0.045f, f.scale, 1e-5f)
        }
        // p = 1: 81 right, 6 down (the QA numbers).
        val one = CardPoses.fan(1f)!!
        assertEquals(81.3f, one.x, 0.1f)
        assertEquals(6.4f, one.y, 0.1f)
        assertNull(CardPoses.fan(4f))
    }

    @Test
    fun tarotDealsEveryCardInSpreadsOfFive() {
        assertEquals(1, CardPoses.hands(0))
        assertEquals(1, CardPoses.hands(5))
        assertEquals(2, CardPoses.hands(7))
        assertEquals(2, CardPoses.hands(8))
        assertEquals(3, CardPoses.hands(11))
        val n = 8
        val seen = (0 until CardPoses.hands(n)).flatMap { CardPoses.hand(it, n).toList() }
        assertEquals((0 until n).toList(), seen)
        assertEquals(5..7, CardPoses.hand(1, n).let { it.first..it.last })
        // A short second hand is centred on the arc.
        assertEquals(-8f, CardPoses.tarotAngle(0, 3), 0f)
        assertEquals(8f, CardPoses.tarotAngle(2, 3), 0f)
    }
}
