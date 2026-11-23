package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.CardParallax
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRowFit
import io.github.lenticualrstone.civilight.designsystem.component.TopBarFit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Emulator round 1: card-row parallax baring a strip, top-bar title squeezed out in a 280 dp pane. */
class LayoutFitTest {
    private val width = CardMetrics.size(CardKind.PROJECT, inner = false).width // 222
    private val pitch = CardMetrics.pitch(CardKind.PROJECT, inner = false) // 236

    @Test
    fun `parallax shift never passes the scaled image's overhang`() {
        val room = CardParallax.maxShift(width, reduced = false)
        assertEquals("5 % of the width each side", 11.1f, room, 0.001f)
        // Every card position along a 5-card row: the image (scale 1.1 about its centre, shifted) still covers the card.
        for (i in 0..4) {
            val d = i * pitch
            val shift = CardParallax.imageShift(d, reduced = false, imageWidth = width)
            val scale = CardParallax.imageScale(reduced = false)
            val left = width / 2 - scale * width / 2 + shift
            val right = width / 2 + scale * width / 2 + shift
            assertTrue("card $i: image starts at $left", left <= 0.0001f)
            assertTrue("card $i: image ends at $right", right >= width - 0.0001f)
        }
        assertEquals("second card at rest is clamped", -room, CardParallax.imageShift(pitch, reduced = false, imageWidth = width), 0.001f)
        assertEquals("also when scrolled past the start", room, CardParallax.imageShift(-pitch, reduced = false, imageWidth = width), 0.001f)
    }

    @Test
    fun `parallax still moves near the snap point and keeps the old unbounded form`() {
        assertEquals(-0.08f * 50f, CardParallax.imageShift(50f, reduced = false, imageWidth = width), 0.001f)
        assertEquals(18.88f, CardParallax.imageShift(-236f, reduced = false), 0.001f)
        assertEquals(0f, CardParallax.imageShift(pitch, reduced = true, imageWidth = width), 0f)
        assertEquals(0f, CardParallax.maxShift(width, reduced = true), 0f)
        assertEquals("guard keeps a pixel back", 10.1f, CardParallax.maxShift(width, reduced = false, guard = 1f), 0.001f)
        assertEquals(0f, CardParallax.maxShift(4f, reduced = false, guard = 1f), 0f)
    }

    @Test
    fun `top bar actions wrap when the title would be squeezed out`() {
        val minTitle = TopBarFit.MIN_TITLE_DP.toInt()
        // Library at 1:2 (280 dp pane, 20 + 12 padding → 248): toggle 80 + 설정 · 내보내기 · + 44 each + gaps 12 = 224.
        // Inline the title had 24 dp (the "dot"); wrapped it takes the whole row.
        assertTrue(TopBarFit.wraps(width = 248, leading = 0, actions = 224, minTitle = minTitle, titleNatural = 70))
        assertEquals(248, TopBarFit.titleWidth(248, 0, 224, wrap = true))
        // Cast list at 1:2 (8 + 12 padding → 260): back 44, lens chip 69 · toggle 88 · filter · + 44 · gaps 16 = 261.
        assertTrue(TopBarFit.wraps(width = 260, leading = 44, actions = 261, minTitle = minTitle, titleNatural = 50))
        assertEquals(216, TopBarFit.titleWidth(260, 44, 261, wrap = true))
        assertEquals("one dp too wide: starts at 0 and runs into the end padding, not squeezed", 0, TopBarFit.actionsX(260, 261))
        assertEquals(36, TopBarFit.actionsX(260, 224))
    }

    @Test
    fun `top bar keeps one row where the title already fits`() {
        val minTitle = TopBarFit.MIN_TITLE_DP.toInt()
        // Cast list at cover width (390 dp → 370 after padding): `Demo / 인물` (~45 dp) fits in the 65 dp left.
        assertFalse(TopBarFit.wraps(width = 370, leading = 44, actions = 261, minTitle = minTitle, titleNatural = 45))
        // Even a long project name keeps one row there (ellipsised to 65 dp ≥ the 64 dp minimum).
        assertFalse(TopBarFit.wraps(width = 370, leading = 44, actions = 261, minTitle = minTitle, titleNatural = 200))
        assertEquals(65, TopBarFit.titleWidth(370, 44, 261, wrap = false))
        // Library at cover width and any wide pane: one row.
        assertFalse(TopBarFit.wraps(width = 358, leading = 0, actions = 224, minTitle = minTitle, titleNatural = 70))
        assertFalse(TopBarFit.wraps(width = 900, leading = 44, actions = 261, minTitle = minTitle))
        assertFalse("no actions: nothing to wrap", TopBarFit.wraps(width = 60, leading = 0, actions = 0, minTitle = minTitle))
        assertEquals(0, TopBarFit.titleWidth(100, 44, 200, wrap = false))
    }

    @Test
    fun `empty block buttons stack when a label does not fit its half`() {
        // 280 dp list pane at 1:2: 24 dp sides and the block's 20 dp padding leave 192; each half is 92.
        assertEquals(92, ButtonRowFit.slot(2, 192, 8))
        // `인물 추가` (≈ 90) fits, `라이프 레코더에서` (≈ 140) does not: stacked, nothing cut to `라이프 ...`.
        assertFalse(ButtonRowFit.sideBySide(listOf(90, 140), width = 192, gap = 8))
        // Cover width (390 dp: 302 inside) keeps flex 1 + flex 1 (DS-47).
        assertTrue(ButtonRowFit.sideBySide(listOf(90, 140), width = 302, gap = 8))
        assertTrue("exactly the half still fits", ButtonRowFit.sideBySide(listOf(147, 147), width = 302, gap = 8))
        assertTrue("one button never stacks", ButtonRowFit.sideBySide(listOf(400), width = 192, gap = 8))
    }
}
