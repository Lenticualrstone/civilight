package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.theme.LayoutMetrics
import io.github.lenticualrstone.civilight.designsystem.theme.OverlayGaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DS-66 overlay gaps (T-DS-38) and the DS-67 toast slot (T-DS-39). */
class OverlayGapsTest {
    private val h = 844f
    private val gestureBar = 24f

    @Test
    fun `T-DS-38 tokens`() {
        assertEquals(16f, OverlayGaps.edgeCompact.value)
        assertEquals(24f, OverlayGaps.edgeInner.value)
        assertEquals(6f, OverlayGaps.aboveBar.value)
        assertEquals(8f, OverlayGaps.aboveKeyboard.value)
        assertEquals(24f, OverlayGaps.sheetBottom.value)
        assertEquals(16f, OverlayGaps.sheetBottomIme.value)
        assertEquals(12f, OverlayGaps.sideCompact.value)
        assertEquals(24f, OverlayGaps.sideInner.value)
        assertEquals(8f, OverlayGaps.top.value)
        assertEquals(24f, OverlayGaps.dialogBand.value)
        assertEquals(156f, OverlayGaps.toastLiftMin.value)
        assertEquals(720f, LayoutMetrics.commandRowMaxInner.value)
        assertEquals(24f, LayoutMetrics.commandBarSideInner.value)
        assertEquals(20f, LayoutMetrics.commandBarBottomInner.value)
    }

    @Test
    fun `T-DS-38 sheets and dialogs`() {
        assertEquals(48f, OverlayGaps.sheetBottomPadding(systemBottom = gestureBar, ime = false))
        assertEquals(16f, OverlayGaps.sheetBottomPadding(systemBottom = 300f, ime = true))
        assertEquals(40f, OverlayGaps.floatingDialogY(inner = false, systemBottom = gestureBar))
        assertEquals(48f, OverlayGaps.floatingDialogY(inner = true, systemBottom = gestureBar))
        assertEquals(308f, OverlayGaps.floatingDialogY(inner = false, systemBottom = 300f, ime = true))
        assertEquals(48f..796f, OverlayGaps.dialogBand(statusTop = 24f, windowHeight = h, systemBottom = gestureBar))
    }

    @Test
    fun `T-DS-38 popovers flip above the bar`() {
        val barTop = h - 154f
        assertEquals(684f, OverlayGaps.popoverBottomLimit(h, gestureBar, barTop, inner = false))
        val above = OverlayGaps.popoverY(anchorTop = 556f, anchorBottom = 600f, height = 200f, windowHeight = h, systemBottom = gestureBar, statusTop = 24f, barTop = barTop)
        assertEquals(556f - 6f - 200f, above)
        val below = OverlayGaps.popoverY(anchorTop = 256f, anchorBottom = 300f, height = 200f, windowHeight = h, systemBottom = gestureBar, statusTop = 24f, barTop = barTop)
        assertEquals(306f, below)
        // Never above the status bar + 8.
        val clamped = OverlayGaps.popoverY(anchorTop = 60f, anchorBottom = 700f, height = 200f, windowHeight = h, systemBottom = gestureBar, statusTop = 24f, barTop = barTop)
        assertEquals(32f, clamped)
        // Sides: 12 on cover, 24 inner.
        assertEquals(12f, OverlayGaps.popoverX(anchorLeft = 0f, width = 268f, windowWidth = 390f, inner = false))
        assertEquals(390f - 268f - 12f, OverlayGaps.popoverX(anchorLeft = 300f, width = 268f, windowWidth = 390f, inner = false))
        assertEquals(24f, OverlayGaps.popoverX(anchorLeft = 0f, width = 268f, windowWidth = 800f, inner = true))
    }

    @Test
    fun `T-DS-38 nothing reaches below the system bottom`() {
        for (sb in listOf(0f, 24f, 48f, 300f)) {
            assertTrue(OverlayGaps.sheetBottomPadding(sb, ime = false) >= sb)
            assertTrue(OverlayGaps.floatingDialogY(false, sb) > sb)
            assertTrue(OverlayGaps.dialogBand(24f, h, sb).endInclusive <= h - sb)
            assertTrue(OverlayGaps.popoverBottomLimit(h, sb, null, inner = false) <= h - sb)
            assertTrue(OverlayGaps.toastBottom(sb, 130f) > sb)
        }
    }

    @Test
    fun `T-DS-39 toast slot`() {
        assertEquals(180f, OverlayGaps.toastBottom(systemBottom = 24f, tallestChrome = 130f))
        assertEquals(180f, OverlayGaps.toastBottom(24f, 150f))
        // Settings screen, no bar: the same slot, so it never jumps when the next screen has a bar.
        assertEquals(180f, OverlayGaps.toastBottom(24f, 0f))
        assertEquals(200f, OverlayGaps.toastBottom(24f, 170f))
        assertEquals(456f, OverlayGaps.toastBottom(300f, 130f))
        assertEquals(390f - 32f, OverlayGaps.toastWidth(390f, inner = false))
        assertEquals(560f, OverlayGaps.toastWidth(800f, inner = true))
        assertEquals(120f, OverlayGaps.toastLeft(800f, inner = true))
        assertEquals(16f, OverlayGaps.toastLeft(390f, inner = false))
    }
}
