package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.feature.cast.profile.FixedField
import io.github.lenticualrstone.civilight.feature.cast.profile.RecordHeroTaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordHeroTapsTest {
    @Test
    fun `art tap opens the background picker only while a background is set`() {
        assertEquals(FixedField.BACKGROUND, RecordHeroTaps.artField(hasBackground = true))
        assertEquals(FixedField.PORTRAIT, RecordHeroTaps.artField(hasBackground = false))
        assertEquals(CastUiCopy.BACKGROUND, RecordHeroTaps.label(FixedField.BACKGROUND))
        assertEquals(CastUiCopy.PORTRAIT, RecordHeroTaps.label(FixedField.PORTRAIT))
    }

    @Test
    fun `card tap always opens the portrait picker`() {
        assertEquals(FixedField.PORTRAIT, RecordHeroTaps.cardField)
    }

    @Test
    fun `art and card taps only on editable records`() {
        assertTrue(RecordHeroTaps.artTaps(editable = true))
        assertFalse(RecordHeroTaps.artTaps(editable = false))
    }

    @Test
    fun `title opens the name sheet, the display name sheet while masked by an override`() {
        assertEquals(FixedField.NAME, RecordHeroTaps.titleField(editable = true, hidden = false, hiddenUnlocked = false, overridden = false))
        assertEquals(FixedField.DISPLAY_NAME, RecordHeroTaps.titleField(editable = true, hidden = false, hiddenUnlocked = false, overridden = true))
        assertEquals(FixedField.NAME, RecordHeroTaps.titleField(editable = true, hidden = true, hiddenUnlocked = true, overridden = false))
    }

    @Test
    fun `title is plain when read-only or hidden and locked`() {
        assertNull(RecordHeroTaps.titleField(editable = false, hidden = false, hiddenUnlocked = true, overridden = false))
        assertNull(RecordHeroTaps.titleField(editable = true, hidden = true, hiddenUnlocked = false, overridden = false))
        assertNull(RecordHeroTaps.titleField(editable = false, hidden = true, hiddenUnlocked = false, overridden = true))
    }

    @Test
    fun `subtitle opens the one-liner sheet on the same terms`() {
        assertEquals(FixedField.ONE_LINER, RecordHeroTaps.subtitleField(editable = true, hidden = false, hiddenUnlocked = false))
        assertEquals(FixedField.ONE_LINER, RecordHeroTaps.subtitleField(editable = true, hidden = true, hiddenUnlocked = true))
        assertNull(RecordHeroTaps.subtitleField(editable = false, hidden = false, hiddenUnlocked = true))
        assertNull(RecordHeroTaps.subtitleField(editable = true, hidden = true, hiddenUnlocked = false))
    }
}
