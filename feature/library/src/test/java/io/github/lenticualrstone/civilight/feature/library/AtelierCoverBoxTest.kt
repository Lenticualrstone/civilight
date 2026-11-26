package io.github.lenticualrstone.civilight.feature.library

import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.AtelierCoverBox
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.atelierStackDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cover windows stay 3:4; a short stage shrinks width with height (never squashes to a square). */
class AtelierCoverBoxTest {
    private val eps = 0.01f

    // Fold 8 screens in dp (r3 board): cover 475×751, inner 704×933.
    // Stage = screen minus top bar (~56) and bottom bar + dock (~206) — the deck/CD/reel stage.
    private val coverStage = 475f to (751f - 262f)
    private val innerStage = 704f to (933f - 262f)

    @Test
    fun `box is exactly 3 to 4`() {
        listOf(coverStage, innerStage, 360f to 300f, 900f to 2000f).forEach { (w, h) ->
            val b = AtelierCoverBox.fit(w, h, 0.58f, 0.82f, 176f, 420f)
            assertEquals(0.75f, b.width / b.height, 0.0001f)
        }
    }

    @Test
    fun `short stage shrinks width with height, never only height`() {
        val tall = AtelierCoverBox.fit(475f, 900f, 0.58f, 0.82f, 176f, 420f)
        val short = AtelierCoverBox.fit(475f, 300f, 0.58f, 0.82f, 176f, 420f)
        assertTrue(short.width < tall.width)
        assertEquals(0.75f, short.width / short.height, 0.0001f)
        assertTrue(short.height <= 300f * 0.82f + eps)
        // Even below the min width, height-limited boxes keep the ratio.
        val tiny = AtelierCoverBox.fit(475f, 120f, 0.58f, 0.82f, 176f, 420f)
        assertTrue(tiny.width < 176f)
        assertEquals(0.75f, tiny.width / tiny.height, 0.0001f)
    }

    @Test
    fun `deck sizes on both Fold screens`() {
        val (cw, ch) = AtelierCoverBox.deck(coverStage.first.dp, coverStage.second.dp)
        assertEquals(275.5f, cw.value, eps) // 475 × 0.58, width-limited
        assertEquals(367.33f, ch.value, eps)
        val (iw, ih) = AtelierCoverBox.deck(innerStage.first.dp, innerStage.second.dp)
        assertEquals(408.32f, iw.value, eps) // 704 × 0.58 (> old 264 cap)
        assertEquals(544.43f, ih.value, eps)
        assertTrue(iw > cw)
    }

    @Test
    fun `reel window and jewel case scale up on the inner screen`() {
        val (cw, ch) = AtelierCoverBox.reelWindow(475.dp, (489f - 74f).dp)
        val (iw, ih) = AtelierCoverBox.reelWindow(704.dp, (671f - 74f).dp)
        assertEquals(0.75f, cw.value / ch.value, 0.0001f)
        assertEquals(0.75f, iw.value / ih.value, 0.0001f)
        assertTrue(iw.value > 320f) // old cap
        assertTrue(ih.value <= (671f - 74f) * 0.92f + eps)

        val (caseW, caseH) = AtelierCoverBox.jewelCase(704.dp, 671.dp)
        assertEquals(AtelierCoverBox.JEWEL_CASE_ASPECT, caseW.value / caseH.value, 0.0001f)
        assertTrue(caseH.value > 296f) // old cap
        // The 3:4 booklet fits inside the case beside the spine (28dp) and 4dp rim.
        val bookletW = (caseH.value - 8f) * AtelierCoverBox.COVER_ASPECT
        assertTrue(bookletW <= caseW.value - 32f)
    }

    @Test
    fun `stack distance only changes at half steps`() {
        assertEquals(atelierStackDistance(2, 1.01f), atelierStackDistance(2, 1.49f), 0f)
        assertTrue(atelierStackDistance(2, 1.6f) < atelierStackDistance(2, 1.4f))
        // Nearer card stays on top of farther one.
        assertTrue(atelierStackDistance(1, 1.3f) < atelierStackDistance(2, 1.3f))
        assertTrue(atelierStackDistance(2, 1.7f) < atelierStackDistance(1, 1.7f))
    }
}
