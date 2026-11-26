package io.github.lenticualrstone.civilight.feature.cast

import androidx.compose.ui.Alignment
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveMath
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.profile.ModularTabCopy
import io.github.lenticualrstone.civilight.feature.cast.profile.RecordHeroLayout
import io.github.lenticualrstone.civilight.model.app.ProfileTabKind
import io.github.lenticualrstone.civilight.model.asset.Ratio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordHeroLayoutTest {
    private val bg = SlotImage("ash-dawn", null, null, holo = false, ratio = Ratio.R9X16)
    private val flat = SlotImage("lantern", null, null, holo = false, ratio = Ratio.R4X5)
    private val holo = flat.copy(holo = true)

    // ---- layers: holo × background × setting ----

    @Test
    fun `holo with background draws both the background and the holo card`() {
        for (setting in listOf(true, false)) {
            val l = RecordHeroLayout.layers(bg, holo, setting)
            assertSame(bg, l.back)
            assertSame(holo, l.card)
            assertFalse(l.backIsPortrait)
            assertFalse(l.ambient)
        }
    }

    @Test
    fun `holo without background keeps the portrait as a blurred back layer behind the card`() {
        val l = RecordHeroLayout.layers(null, holo, portraitBack = true)
        assertSame(holo, l.back)
        assertSame(holo, l.card)
        assertTrue(l.backIsPortrait)
        assertTrue(l.ambient)
    }

    @Test
    fun `holo without background and setting off draws the card only`() {
        val l = RecordHeroLayout.layers(null, holo, portraitBack = false)
        assertNull(l.back)
        assertSame(holo, l.card)
        assertFalse(l.ambient)
    }

    @Test
    fun `flat portrait without background is the full-bleed art, no card`() {
        val l = RecordHeroLayout.layers(null, flat, portraitBack = true)
        assertSame(flat, l.back)
        assertNull(l.card)
        assertTrue(l.backIsPortrait)
        assertFalse(l.ambient)
    }

    @Test
    fun `flat portrait without background and setting off still shows as a still card`() {
        val l = RecordHeroLayout.layers(null, flat, portraitBack = false)
        assertNull(l.back)
        assertSame(flat, l.card)
    }

    @Test
    fun `flat portrait with background shows the background only`() {
        val l = RecordHeroLayout.layers(bg, flat, portraitBack = true)
        assertSame(bg, l.back)
        assertNull(l.card)
    }

    @Test
    fun `no images is empty`() {
        assertTrue(RecordHeroLayout.layers(null, null, true).isEmpty)
        val onlyBg = RecordHeroLayout.layers(bg, null, true)
        assertSame(bg, onlyBg.back)
        assertNull(onlyBg.card)
    }

    // ---- immersive card (pane sizes) ----

    @Test
    fun `holo card is 300 by 430 on the cover and sits between the header and the title`() {
        // Cover board 475 × 751, status 28, no nav inset.
        val b = RecordHeroLayout.cardBox(475f, 751f, statusInset = 28f, navInset = 0f, hasSubtitle = true)
        assertEquals(300f, b.width, 0.01f)
        assertEquals(430f, b.height, 0.01f)
        assertEquals((475f - 300f) / 2f, b.left, 0.01f)
        val headerFoot = ImmersiveMath.headerHeight(28f) + RecordHeroLayout.CARD_GAP_TOP
        val titleTop = ImmersiveMath.titleStartY(751f, ImmersiveMath.titleLineHeight(34f), 0f, hasBar = true, hasSubtitle = true)
        assertTrue(b.top >= headerFoot - 0.01f)
        assertTrue(b.top + b.height <= titleTop - RecordHeroLayout.CARD_GAP_BOTTOM + 0.01f)
    }

    @Test
    fun `card narrows with a narrow pane and shrinks in a short pane, keeping 300 to 430`() {
        val narrow = RecordHeroLayout.cardBox(300f, 900f, 24f, 0f, hasSubtitle = false)
        assertEquals(300f - 2 * ImmersiveMath.GUTTER_COVER_DP, narrow.width, 0.01f)
        assertEquals(narrow.width * 430f / 300f, narrow.height, 0.01f)
        val short = RecordHeroLayout.cardBox(700f, 500f, 24f, 16f, hasSubtitle = true)
        assertTrue(short.height < 430f)
        assertEquals(short.height * 300f / 430f, short.width, 0.01f)
        val none = RecordHeroLayout.cardBox(400f, 120f, 24f, 0f, hasSubtitle = true)
        assertEquals(0f, none.height, 0f)
    }

    @Test
    fun `card is sized from the pane width, not the window`() {
        // Inner right pane ≈ 460 wide: cover gutters; a full inner screen 900 wide: inner gutters, still 300.
        assertEquals(300f, RecordHeroLayout.cardBox(460f, 900f, 24f, 0f, true).width, 0.01f)
        val wide = RecordHeroLayout.cardBox(900f, 1200f, 24f, 0f, true)
        assertEquals(300f, wide.width, 0.01f)
        assertEquals(300f, wide.left, 0.01f)
    }

    @Test
    fun `art is cropped top-aligned, background and portrait alike`() {
        assertEquals(Alignment.TopCenter, RecordHeroLayout.alignment)
    }

    // ---- terse copy ----

    @Test
    fun `modular tab empty copy is fragments, not sentences`() {
        assertEquals("장비 없음", ModularTabCopy.empty("장비"))
        for (kind in ProfileTabKind.entries) {
            val hint = ModularTabCopy.hint(kind) ?: continue
            assertFalse(hint, hint.endsWith("요.") || hint.endsWith("다") || hint.endsWith("."))
        }
        assertNull(ModularTabCopy.hint(ProfileTabKind.CUSTOM))
    }
}
