package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.memo.AtMarks
import io.github.lenticualrstone.civilight.feature.recorder.memo.MemoEditing
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoEditingTest {
    private val sera = Runs.linkRun("01JZ000000TSTK000000000001", LinkTargetKind.CHARACTER, "SERA", "P1", "세라")

    /** `탈퇴 이유가 ` + [세라] + `와 관련`. */
    private val runs: List<TextRun> = listOf(TextRun.Plain("탈퇴 이유가 "), sera, TextRun.Plain("와 관련"))

    @Test
    fun diffFindsTheEditedRangeAtTheCaret() {
        assertNull(MemoEditing.diff("abc", "abc", 3))
        assertEquals(MemoEditing.Change(1, 2, ""), MemoEditing.diff("abc", "ac", 1))
        // A repeated character is placed where the caret says it was typed.
        assertEquals(MemoEditing.Change(1, 1, "a"), MemoEditing.diff("aa", "aaa", 2))
        assertEquals(MemoEditing.Change(0, 0, "a"), MemoEditing.diff("aa", "aaa", 1))
        // Hangul composition replaces the composing syllable.
        assertEquals(MemoEditing.Change(0, 1, "셀"), MemoEditing.diff("세", "셀", 1))
        assertEquals(MemoEditing.Change(2, 5, "X"), MemoEditing.diff("abcdef", "abXf", 3))
    }

    @Test
    fun typingAfterALinkKeepsItAndEditingInsideItTurnsItToText() {
        val text = Runs.text(runs)
        val after = MemoEditing.apply(runs, text.replace("세라와", "세라 와"), "탈퇴 이유가 세라 ".length)
        assertTrue(after.any { it is TextRun.Link })
        assertEquals("탈퇴 이유가 세라 와 관련", Runs.text(after))

        val inside = MemoEditing.apply(runs, text.replace("세라", "세"), "탈퇴 이유가 세".length)
        assertTrue(inside.none { it is TextRun.Link })
        assertEquals("탈퇴 이유가 세와 관련", Runs.text(inside))
    }

    @Test
    fun atMarksShowAndMapOffsets() {
        val marks = AtMarks.of(runs)
        val text = Runs.text(runs)
        assertEquals("탈퇴 이유가 @세라와 관련", marks.show(text))
        val start = "탈퇴 이유가 ".length
        // A caret at the link's start stays before the `@`; after the link it moves by one.
        assertEquals(start, marks.originalToShown(start))
        assertEquals(start + 3, marks.originalToShown(start + 2))
        // Both sides of the `@` map back to the link's start.
        assertEquals(start, marks.shownToOriginal(start))
        assertEquals(start, marks.shownToOriginal(start + 1))
        assertEquals(start + 2, marks.shownToOriginal(start + 3))
        assertEquals(listOf(start until start + 3), marks.shownRanges())
        assertTrue(marks.fits(text.length))
        assertEquals(AtMarks.of(runs), marks)
    }

    @Test
    fun everyShownOffsetMapsInsideTheText() {
        val marks = AtMarks.of(listOf<TextRun>(sera, TextRun.Plain(" 와 "), sera.copy(linkKey = "01JZ000000TSTK000000000002")))
        val text = "세라 와 세라"
        val shown = marks.show(text)
        assertEquals("@세라 와 @세라", shown)
        for (t in 0..shown.length) assertTrue(marks.shownToOriginal(t) in 0..text.length)
        for (o in 0..text.length) assertTrue(marks.originalToShown(o) in 0..shown.length)
    }
}
