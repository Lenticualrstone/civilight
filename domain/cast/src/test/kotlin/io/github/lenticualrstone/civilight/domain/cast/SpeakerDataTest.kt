package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldFilters
import io.github.lenticualrstone.civilight.domain.cast.script.SpeakerData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** CST-37 (picker field chips) and CST-38 (the script panel's cast data at one block). */
class SpeakerDataTest {
    private val doc = Fx.doc()

    @Test
    fun `CST-38 하르트 at #042 #043 #044 reads the state just before each block plus its own changes`() {
        val at42 = SpeakerData.panel(doc, K.HART, K.BLOCK_042)!!
        assertEquals("리엔 · 만나기 전", at42.relations.single { it.partnerKey == K.RIEN }.line)

        val at43 = SpeakerData.panel(doc, K.HART, K.BLOCK_043)!!.relations.single { it.partnerKey == K.RIEN }
        assertEquals("만나기 전 → 보호 ◆ 여기서", at43.own.text)
        assertEquals("만나기 전 → 경계 ◆ 여기서", at43.otherLine)

        val at44 = SpeakerData.panel(doc, K.HART, K.BLOCK_044)!!
        val rien = at44.relations.single { it.partnerKey == K.RIEN }
        assertEquals("리엔 · → 보호 · 호칭 ‘꼬마’", rien.line)
        assertEquals("← 경계", rien.otherLine)
        assertEquals(listOf("리엔을" to "‘꼬마’"), at44.addresses)
    }

    @Test
    fun `TC-F-26 the panel head uses the first two header fields before the block`() {
        assertEquals("화자 · 리엔 · 17세 · 등불 결사", SpeakerData.panel(doc, K.RIEN, K.BLOCK_042)!!.headLine)
    }

    @Test
    fun `TC-N43-01 the 말투 line is the one-line summary, never the prose, the display name shows only while it stands in`() {
        val rien = SpeakerData.panel(doc, K.RIEN, K.BLOCK_042)!!
        assertNull(Fx.character(doc, K.RIEN).speechSummary)
        assertNull("no summary: the line is left out, not the prose", rien.speech)
        assertNull(rien.displayName)
        val withSummary = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.RIEN) it.copy(speechSummary = "반말 · 짧게 끊음") else it }))
        assertEquals("반말 · 짧게 끊음", SpeakerData.panel(withSummary, K.RIEN, K.BLOCK_042)!!.speech)
    }

    @Test
    fun `rule 19 a locked hidden partner is one row and a locked hidden speaker has no panel`() {
        val hidden = Fx.hide(doc, K.OWEN)
        val rien = SpeakerData.panel(hidden, K.RIEN, K.BLOCK_044)!!
        assertTrue(rien.relations.none { it.partnerKey == K.OWEN })
        assertEquals("숨긴 인물과의 관계 1", rien.hiddenRelations)
        assertNull(SpeakerData.panel(hidden, K.OWEN, K.BLOCK_044))
        assertTrue(SpeakerData.panel(hidden, K.RIEN, K.BLOCK_044, hiddenUnlocked = true)!!.relations.any { it.partnerKey == K.OWEN })
    }

    @Test
    fun `rule 20 a deleted speaker has no panel`() {
        assertNull(SpeakerData.panel(doc, K.NOAH, K.BLOCK_044))
    }

    @Test
    fun `CST-37 picker chips list each option of each 필터 field and filter by the value`() {
        val chips = SpeakerData.fieldChips(doc)
        val expected = FieldFilters.chips(doc).sumOf { it.options.size }
        assertEquals(expected, chips.size)
        chips.forEach { assertTrue(it.label.contains(CastCopy.SEP)) }
        val everyone = doc.cast.characters.filter { it.deletedAt == null }.map { it.key }
        for (chip in chips) {
            val kept = SpeakerData.filter(doc, everyone, chip)
            assertTrue(everyone.containsAll(kept))
        }
    }
}
