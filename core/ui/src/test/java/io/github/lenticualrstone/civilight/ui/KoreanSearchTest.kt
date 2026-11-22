package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.ui.search.KoreanSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KoreanSearchTest {
    @Test
    fun `initial consonants of syllables`() {
        assertEquals("ㅎㄹㅌ", KoreanSearch.initials("하르트"))
        assertEquals("ㅇㄹㅈㄴ ㄷㄷ", KoreanSearch.initials("오리지늄 등등"))
        assertEquals('ㄲ', KoreanSearch.initialOf('꼬'))
        assertNull(KoreanSearch.initialOf('A'))
    }

    @Test
    fun `plain, initial-only and mixed queries match`() {
        assertEquals(0..2, KoreanSearch.find("하르트", "하르트"))
        assertEquals(0..2, KoreanSearch.find("하르트", "ㅎㄹㅌ"))
        assertEquals(0..1, KoreanSearch.find("하르트", "하ㄹ"))
        assertEquals(5..6, KoreanSearch.find("오리지늄 등불", "ㄷㅂ"))
        assertTrue(KoreanSearch.matches("Civilight 노트", "civil"))
        assertFalse(KoreanSearch.matches("하르트", "ㄹㅎ"))
    }

    @Test
    fun `blank queries match nothing and any of several texts can match`() {
        assertNull(KoreanSearch.find("리엔", "  "))
        assertTrue(KoreanSearch.matchesAny(listOf(null, "리엔", "주연"), "ㅈㅇ"))
        assertFalse(KoreanSearch.matchesAny(listOf(null, "리엔"), "세라"))
    }
}
