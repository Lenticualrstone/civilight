package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.5, review item 7) 문장부호 보정 is OFF by default; an explicitly stored `true` is kept. */
class SmartPunctuationSettingTest {
    @Test
    fun `default is off, stored true is kept, missing reads off`() {
        assertFalse(AppSettings().smartPunctuation)
        assertFalse(CivilightJson.decode(AppSettings.serializer(), "{}").smartPunctuation)
        assertTrue(CivilightJson.decode(AppSettings.serializer(), """{"smartPunctuation":true}""").smartPunctuation)
        val on = AppSettings(smartPunctuation = true)
        val json = CivilightJson.encode(AppSettings.serializer(), on)
        assertTrue(json.contains("smartPunctuation"))
        assertTrue(CivilightJson.decode(AppSettings.serializer(), json).smartPunctuation)
    }
}
