package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.ui.HeadLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Emulator QA (1:2 split, 280 dp): the scene head broke as `때 없 / 음`; it may break between 어절 only. */
class HeadUnbrokenTest {
    private val wj = HeadLines.WORD_JOINER.toString()
    private val nb = HeadLines.NO_BREAK_SPACE

    /** Where a line may break: after each plain space (the only break left inside the display text). */
    private fun breakable(display: String): List<String> = display.split(' ').map { it.replace(wj, "").replace(nb, ' ') }

    @Test
    fun `a head breaks only at the spaces after a separator`() {
        val display = HeadLines.unbroken("씬 1 · 장소 없음 · 때 없음")
        // The text reads the same once the joiners are dropped.
        assertEquals("씬 1 · 장소 없음 · 때 없음", display.replace(wj, "").replace(nb, ' '))
        assertTrue(display.contains("없${wj}음"))
        assertTrue(display.contains("때 없${wj}음"))
        // No line starts with `·`: the space before it does not break.
        assertEquals(listOf("씬", "1 ·", "장소", "없음 ·", "때", "없음"), breakable(display))
    }

    @Test
    fun `the transition line and a titled head`() {
        assertEquals(listOf("전환", "문구 ·", "없음"), breakable(HeadLines.unbroken("전환 문구 · 없음")))
        assertEquals("씬 3 · 꺼진 등불 · 결사의 문 · 2일째 새벽 05:40", HeadLines.unbroken("씬 3 · 꺼진 등불 · 결사의 문 · 2일째 새벽 05:40").replace(wj, "").replace(nb, ' '))
    }

    @Test
    fun `a very long word stays breakable, letters are never split`() {
        val long = "가".repeat(HeadLines.MAX_JOINED + 1)
        assertFalse(HeadLines.unbroken(long).contains(wj))
        // A combining mark stays on its letter (no joiner inside one letter).
        assertEquals("é${wj}a", HeadLines.unbroken("éa"))
    }
}
