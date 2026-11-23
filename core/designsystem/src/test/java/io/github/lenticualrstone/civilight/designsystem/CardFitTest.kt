package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.card.CardFit
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.CardSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** U-11: the 카드 진열 card stands whole between the bars, with the next card peeking, on cover and inner. */
class CardFitTest {
    private val cover = CardSize(222f, 296f)
    private val inner = CardSize(236f, 315f)

    // Top bar 112 + status 24 + the column's 8; bottom bar 130 + gesture bar 24; chips + order line 92; row 14 + 12.
    private val top = 144f
    private val bottom = 154f
    private val head = 92f
    private val extra = 26f

    private fun room(h: Float) = CardFit.room(h, top, bottom, head, extra)

    @Test
    fun `cover portrait 390 x 844 keeps the reference card`() {
        val size = CardFit.fit(cover, room(844f), 390f, CardMetrics.inset(false), CardMetrics.gap(false))
        assertSame(cover, size)
        // The next card starts at 20 + 222 + 14 = 256 < 390: it peeks.
        assertTrue(CardMetrics.inset(false) + size.width + CardMetrics.gap(false) < 390f - CardFit.MIN_PEEK)
    }

    @Test
    fun `inner portrait keeps the reference card`() {
        assertSame(inner, CardFit.fit(inner, room(841f), 701f, CardMetrics.inset(true), CardMetrics.gap(true)))
    }

    @Test
    fun `inner landscape 841 x 701 shrinks the card to stand above the command bar`() {
        val r = room(701f)
        assertEquals(285f, r, 0.001f)
        val size = CardFit.fit(inner, r, 841f, CardMetrics.inset(true), CardMetrics.gap(true))
        assertEquals(285f, size.height, 0.001f)
        assertEquals(285f * 236f / 315f, size.width, 0.001f)
        // The card's bottom: top + head + 14 (lift room) + height ends above the bottom bar.
        assertTrue(top + head + 14f + size.height <= 701f - bottom)
    }

    @Test
    fun `ratio is kept`() {
        val size = CardFit.fit(cover, 250f, 390f, 20f, 14f)
        assertEquals(cover.width / cover.height, size.width / size.height, 0.0001f)
    }

    @Test
    fun `a short landscape drops the head rows then stops at the least height`() {
        // 390 tall: nothing above fits; the row alone has 66 dp, so the card stops at 176 and the column scrolls.
        val r = room(390f)
        assertEquals(390f - top - bottom - extra, r, 0.001f)
        val size = CardFit.fit(inner, r, 844f, 32f, 16f)
        assertEquals(CardFit.MIN_HEIGHT, size.height, 0.001f)
    }

    @Test
    fun `head rows are left out only when they leave less than the least height`() {
        // 600 tall: between = 276, with head 184 (>= 176) → keeps the head.
        assertEquals(184f, room(600f), 0.001f)
        // 560 tall: with head 144 < 176 → the row alone, 236.
        assertEquals(236f, room(560f), 0.001f)
    }

    @Test
    fun `a narrow row shrinks the card so the next one peeks`() {
        val size = CardFit.fit(cover, 1000f, 260f, 20f, 14f)
        assertEquals(260f - 20f - 14f - CardFit.MIN_PEEK, size.width, 0.001f)
    }

    @Test
    fun `never larger than the reference, and small group cards are not raised to the floor`() {
        assertSame(cover, CardFit.fit(cover, 2000f, 2000f, 20f, 14f))
        val group = CardSize(132f, 176f)
        assertSame(group, CardFit.fit(group, 100f, 390f, 20f, 14f))
    }
}
