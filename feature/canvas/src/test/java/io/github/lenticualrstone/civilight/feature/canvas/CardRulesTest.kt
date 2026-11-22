package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.card.CardCopy
import io.github.lenticualrstone.civilight.feature.canvas.card.CardInitial
import io.github.lenticualrstone.civilight.feature.canvas.card.CardKind
import io.github.lenticualrstone.civilight.feature.canvas.card.CardMetrics
import io.github.lenticualrstone.civilight.feature.canvas.card.CardParallax
import io.github.lenticualrstone.civilight.feature.canvas.card.CardSnap
import io.github.lenticualrstone.civilight.feature.canvas.card.SnapTicker
import io.github.lenticualrstone.civilight.feature.canvas.card.ViewChoice
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Card view mechanics (CNV-21 … CNV-24); spec §4.5 cases by id. */
class CardRulesTest {
    @Test
    fun `CNV-21 the view is chosen per screen and starts as the list`() {
        val settings = AppSettings()
        assertFalse(ViewChoice.showCards(settings.viewByScreen, ViewChoice.PAGES))
        val cards = ViewChoice.with(settings.viewByScreen, ViewChoice.PAGES, cards = true)
        assertTrue(ViewChoice.showCards(cards, ViewChoice.PAGES))
        assertEquals("another screen keeps its own choice", settings.viewByScreen["library"], cards["library"])
        assertEquals(ViewMode.LIST, ViewChoice.with(cards, ViewChoice.PAGES, cards = false)[ViewChoice.PAGES])
    }

    @Test
    fun `CNV-22 card pitch per kind and width`() {
        assertEquals(236f, CardMetrics.pitch(CardKind.PROJECT, inner = false))
        assertEquals(252f, CardMetrics.pitch(CardKind.PROJECT, inner = true))
        assertEquals(164f, CardMetrics.pitch(CardKind.RECENT, inner = false))
        assertEquals(192f, CardMetrics.pitch(CardKind.RECENT, inner = true))
        assertEquals(210f, CardMetrics.pitch(CardKind.PORTRAIT, inner = false))
        assertEquals(146f, CardMetrics.pitch(CardKind.PORTRAIT_SMALL, inner = false))
        assertEquals(20f, CardMetrics.inset(inner = false))
        assertEquals(32f, CardMetrics.inset(inner = true))
        assertEquals(296f, CardMetrics.size(CardKind.PROJECT, inner = false).height)
        assertEquals(110f, CardMetrics.size(CardKind.RECENT, inner = true).height)
    }

    @Test
    fun `T-CNV-41 snapping`() {
        val pitch = CardMetrics.pitch(CardKind.PROJECT, inner = false)
        assertEquals(236f, CardSnap.target(300f, pitch))
        assertEquals(472f, CardSnap.target(360f, pitch))
        assertEquals(0f, CardSnap.target(100f, pitch))
        assertEquals("never past the last card", 236f, CardSnap.target(900f, pitch, count = 2))
        assertEquals(2, CardSnap.indexAt(472f, pitch))
        assertEquals("decelerate 320 ms", CardSnap.DECELERATE_MS, CardSnap.MOTION.durationMs)

        val ticker = SnapTicker()
        val ticks = listOf(true, true, false, false, true, false).count { ticker.onScrolling(it) }
        assertEquals("one tick per snap", 2, ticks)
    }

    @Test
    fun `T-CNV-42 parallax and reduce`() {
        assertEquals(18.88f, CardParallax.imageShift(-236f, reduced = false), 0.001f)
        assertEquals(-236f * (1 - 0.08f), CardParallax.imageOnScreen(-236f, reduced = false), 0.001f)
        assertEquals(0f, CardParallax.imageShift(-236f, reduced = true), 0f)
        assertEquals(-236f, CardParallax.imageOnScreen(-236f, reduced = true), 0f)
    }

    @Test
    fun `T-CNV-38 initial-letter placeholder`() {
        assertEquals("노", CardInitial.of("노아"))
        assertEquals("유", CardInitial.of("유리 정원"))
        assertEquals("이미지 고르기", CardCopy.BTN_PICK_IMAGE)
    }

    @Test
    fun `CNV-23 card copy`() {
        assertEquals("숨긴 프로젝트 2", CardCopy.hiddenProjects(2))
        assertEquals("쓰는 중 · 홀로", CardCopy.statusChip("쓰는 중", holo = true))
        assertEquals("홀로", CardCopy.statusChip(null, holo = true))
        assertNull(CardCopy.statusChip(null, holo = false))
    }
}
