package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.editor.DisplaySpan
import io.github.lenticualrstone.civilight.feature.script.editor.LinkLook
import io.github.lenticualrstone.civilight.feature.script.editor.RunLayout
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.TextRun.Paren
import io.github.lenticualrstone.civilight.model.text.TextRun.Plain
import org.junit.Assert.assertEquals
import org.junit.Test

/** SCR-04 / SCR-07: drawn text of runs and the caret maps between raw and display offsets. */
class RunLayoutTest {
    @Test
    fun `SCR-07 a paren run is drawn in parentheses that are not in the raw text`() {
        val layout = RunLayout.of(listOf(Paren("작게"), Plain(" 저기")))
        assertEquals("(작게) 저기", layout.text)
        assertEquals(listOf(DisplaySpan(0, 4, SpanKind.PAREN)), layout.spans)
        assertEquals(5, layout.rawLength)
        assertEquals(0, layout.toDisplay(0)) // before `(`
        assertEquals(2, layout.toDisplay(1)) // between 작 and 게
        assertEquals(4, layout.toDisplay(2)) // after `)`
        assertEquals(7, layout.toDisplay(5))
    }

    @Test
    fun `display offsets map back to raw offsets`() {
        val layout = RunLayout.of(listOf(Paren("작게"), Plain(" 저기")))
        for (raw in 0..layout.rawLength) assertEquals(raw, layout.toRaw(layout.toDisplay(raw)))
        assertEquals(0, layout.toRaw(1)) // tapping on `(`'s right is the start of the paren text
    }

    @Test
    fun `the paren layer off hides parentheticals`() {
        val layout = RunLayout.of(listOf(Paren("작게"), Plain(" 저기")), showParens = false)
        assertEquals(" 저기", layout.text)
        assertEquals(0, layout.toDisplay(2))
    }

    @Test
    fun `paren mode draws an empty pair with the caret inside`() {
        val layout = RunLayout.of(listOf(Plain("저기")), pendingParenAt = 0)
        assertEquals("()저기", layout.text)
        assertEquals(1, layout.toDisplay(0))
    }

    @Test
    fun `typing at the end of an open paren keeps the caret before its closing parenthesis`() {
        val layout = RunLayout.of(listOf(Paren("작게")), openParenEndAt = 2)
        assertEquals("(작게)", layout.text)
        assertEquals(3, layout.toDisplay(2))
    }

    @Test
    fun `a locked hidden link shows its mask`() {
        val link = TextRun.Link("L1", LinkTarget(LinkTargetKind.CHARACTER, "C1"), "노아")
        val layout = RunLayout.of(listOf(Plain("저건 "), link), linkLook = { LinkLook(SpanKind.HIDDEN_LINK, "숨긴 인물") })
        assertEquals("저건 숨긴 인물", layout.text)
        assertEquals(listOf(DisplaySpan(3, 8, SpanKind.HIDDEN_LINK)), layout.spans)
    }
}
