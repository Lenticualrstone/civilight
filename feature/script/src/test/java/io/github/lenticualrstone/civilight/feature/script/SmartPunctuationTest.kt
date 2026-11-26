package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** (0.5, review item 7) 문장부호 보정 rewrites only the just-typed span at the caret. */
class SmartPunctuationTest {
    private fun type(old: String, at: Int, typed: String) =
        Drafts.smartPunctuation(old, old.substring(0, at) + typed + old.substring(at), at + typed.length, at + typed.length, null)

    @Test
    fun `the third typed dot becomes an ellipsis, the caret follows`() {
        val r = type("그래..", 4, ".")
        assertEquals("그래…", r.text)
        assertEquals(3, r.selStart)
        assertEquals(3, r.selEnd)
    }

    @Test
    fun `the second typed dash becomes a long dash`() {
        val r = type("잠깐-", 3, "-")
        assertEquals("잠깐──", r.text)
        assertEquals(4, r.selStart)
    }

    @Test
    fun `text already in the draft is untouched`() {
        // Loaded by 고치기: `...` and `--` stay; only the new letter is typed.
        val loaded = "그래... 잠깐-- 왜"
        val r = type(loaded, loaded.length, "요")
        assertEquals("$loaded" + "요", r.text)
        assertNull(r.undo)
        // Typing right after an existing `--` does not rewrite it.
        val r2 = type("a--", 3, "b")
        assertEquals("a--b", r2.text)
        // A caret-only change (the field echoing a loaded text) changes nothing.
        val r3 = Drafts.smartPunctuation(loaded, loaded, 0, 0, null)
        assertEquals(loaded, r3.text)
    }

    @Test
    fun `typing in the middle shifts the caret only by replacements before it`() {
        // "앞..|뒤... 끝": the dot typed at 3 completes `...` before the caret; the later `...` stays.
        val old = "앞..뒤... 끝"
        val r = type(old, 3, ".")
        assertEquals("앞…뒤... 끝", r.text)
        assertEquals(2, r.selStart)
        // A paste of `x...y` mid-text: the replacement is before the caret at the paste end.
        val p = type("AB", 1, "x...y")
        assertEquals("Ax…yB", p.text)
        assertEquals(4, p.selStart)
    }

    @Test
    fun `an immediate backspace restores the typed literal and it is not substituted again`() {
        val r = type("그래..", 4, ".")
        assertNotNull(r.undo)
        val undo = r.undo!!
        assertEquals("그래", undo.backspaced)
        val back = Drafts.smartPunctuation(r.text, "그래", 2, 2, undo)
        assertEquals("그래...", back.text)
        assertEquals(5, back.selStart)
        assertNull(back.undo)
        // The next letter after the restored literal leaves it alone.
        val next = Drafts.smartPunctuation(back.text, "그래...요", 6, 6, back.undo)
        assertEquals("그래...요", next.text)
        // Long dash: one backspace brings `--` back.
        val d = type("x-", 2, "-")
        val dBack = Drafts.smartPunctuation(d.text, "x─", 2, 2, d.undo)
        assertEquals("x--", dBack.text)
        assertEquals(3, dBack.selStart)
    }

    @Test
    fun `a deletion or unrelated backspace substitutes nothing`() {
        val r = Drafts.smartPunctuation("a...b", "a..b", 3, 3, null)
        assertEquals("a..b", r.text)
        assertEquals(3, r.selStart)
    }
}
