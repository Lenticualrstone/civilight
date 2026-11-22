package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.editor.FieldChange
import io.github.lenticualrstone.civilight.feature.script.editor.FieldDiff
import org.junit.Assert.assertEquals
import org.junit.Test

/** SCR-15: what a change of the block's text field means. */
class FieldDiffTest {
    private val s = FieldDiff.SENTINEL

    @Test
    fun `typing at the end is an insertion`() {
        assertEquals(FieldChange.Text(3, 3, "다"), FieldDiff.classify("가나 ", "${s}가나 다", 5, parenMode = false))
    }

    @Test
    fun `a repeated letter is placed where the caret is`() {
        // 아아 → 아아아: the insertion is placed at or before the caret, never after it.
        assertEquals(FieldChange.Text(2, 2, "아"), FieldDiff.classify("아아", "${s}아아아", 3, parenMode = false))
        assertEquals(FieldChange.Text(1, 1, "아"), FieldDiff.classify("아아", "${s}아아아", 2, parenMode = false))
    }

    @Test
    fun `Hangul composition rewrites the last syllable`() {
        assertEquals(FieldChange.Text(1, 2, "각"), FieldDiff.classify("가가", "${s}가각", 3, parenMode = false))
    }

    @Test
    fun `SCR-15 Enter arrives as a line break and becomes a block operation`() {
        assertEquals(FieldChange.Enter(2), FieldDiff.classify("멈추", "${s}멈추\n", 4, parenMode = false))
        assertEquals(FieldChange.Enter(1), FieldDiff.classify("멈추", "${s}멈\n추", 3, parenMode = false))
    }

    @Test
    fun `SCR-15 Backspace at the start deletes the sentinel`() {
        assertEquals(FieldChange.BackspaceAtStart, FieldDiff.classify("하르트", "하르트", 0, parenMode = false))
        assertEquals(FieldChange.BackspaceAtStart, FieldDiff.classify("", "", 0, parenMode = false))
    }

    @Test
    fun `SCR-07 parentheses switch paren mode instead of being typed`() {
        assertEquals(FieldChange.OpenParen(0), FieldDiff.classify("", "$s(", 2, parenMode = false))
        assertEquals(FieldChange.CloseParen(2), FieldDiff.classify("작게", "${s}작게)", 4, parenMode = true))
        assertEquals(FieldChange.Text(2, 2, ")"), FieldDiff.classify("작게", "${s}작게)", 4, parenMode = false))
    }

    @Test
    fun `a caret move is no change`() {
        assertEquals(FieldChange.None, FieldDiff.classify("하르트", "${s}하르트", 2, parenMode = false))
    }

    @Test
    fun `offsets map across the sentinel`() {
        assertEquals(0, FieldDiff.rawOffset(0))
        assertEquals(0, FieldDiff.rawOffset(1))
        assertEquals(3, FieldDiff.rawOffset(4))
        assertEquals(4, FieldDiff.fieldOffset(3))
    }
}
