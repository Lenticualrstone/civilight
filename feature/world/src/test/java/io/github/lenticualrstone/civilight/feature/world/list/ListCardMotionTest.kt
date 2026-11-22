package io.github.lenticualrstone.civilight.feature.world.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-68 `목록 ↔ 카드` (board `MCardView`). */
class ListCardMotionTest {
    @Test
    fun `WLD-68 list text exits first, cards grow in place after it`() {
        assertEquals(ListCardMotion.Timing(160, 0, grow = false), ListCardMotion.exitTiming(reduced = false))
        val card = ListCardMotion.cardTiming(reduced = false)
        assertEquals(360, card.durationMs)
        assertEquals(160, card.delayMs)
        assertTrue(card.grow)
    }

    @Test
    fun `WLD-68 reduced motion is 120 ms fades only`() {
        listOf(ListCardMotion.exitTiming(true), ListCardMotion.cardTiming(true), ListCardMotion.listTiming(true)).forEach {
            assertEquals(120, it.durationMs)
            assertEquals(0, it.delayMs)
            assertFalse(it.grow)
        }
    }
}
