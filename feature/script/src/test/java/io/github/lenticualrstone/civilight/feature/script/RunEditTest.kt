package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.editor.RunEdit
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.TextRun.Break
import io.github.lenticualrstone.civilight.model.text.TextRun.Paren
import io.github.lenticualrstone.civilight.model.text.TextRun.Plain
import org.junit.Assert.assertEquals
import org.junit.Test

/** SCR-04 / SCR-07 / SCR-55: typing keeps every run it does not touch. */
class RunEditTest {
    private val link = TextRun.Link("L1", LinkTarget(LinkTargetKind.TERM, "T1"), "오리지늄 등불")

    @Test
    fun `typing inside a plain run edits that run`() {
        assertEquals(listOf(Plain("하르트 씨")), RunEdit.replace(listOf(Plain("하트 씨")), 1, 1, "르"))
    }

    @Test
    fun `SCR-07 a paren run keeps its kind when typed into`() {
        val runs = listOf(Paren("작게"), Plain(" 저기…"))
        assertEquals(listOf(Paren("작고 게"), Plain(" 저기…")), RunEdit.replace(runs, 1, 1, "고 "))
        assertEquals(listOf(Paren("아주 작게"), Plain(" 저기…")), RunEdit.replace(runs, 0, 2, "아주 작게"))
    }

    @Test
    fun `SCR-07 paren mode types into a new paren run at a boundary`() {
        val runs = listOf(Plain("저기"))
        assertEquals(listOf(Paren("작게"), Plain("저기")), RunEdit.replace(runs, 0, 0, "작게", parenMode = true))
        assertEquals(listOf(Paren("작게 말하며"), Plain("저기")), RunEdit.replace(listOf(Paren("작게"), Plain("저기")), 2, 2, " 말하며", parenMode = true))
    }

    @Test
    fun `text typed right after a paren run is plain`() {
        assertEquals(listOf(Paren("작게"), Plain(" 저기")), RunEdit.replace(listOf(Paren("작게")), 2, 2, " 저기"))
    }

    @Test
    fun `SCR-55 a link keeps its key when its text is edited`() {
        val runs = listOf(Plain("꺼진 "), link, Plain("이"))
        val edited = RunEdit.replace(runs, 3 + 4, 3 + 4, "의")
        assertEquals(listOf(Plain("꺼진 "), link.copy(text = "오리지늄의 등불"), Plain("이")), edited)
    }

    @Test
    fun `deleting a whole link removes it`() {
        val runs = listOf(Plain("꺼진 "), link, Plain("이"))
        assertEquals(listOf(Plain("꺼진 이")), RunEdit.delete(runs, 3, 3 + link.text.length))
    }

    @Test
    fun `typing at a link's edge is plain text, not part of the link`() {
        val runs = listOf(link)
        assertEquals(listOf(link, Plain("이")), RunEdit.replace(runs, link.text.length, link.text.length, "이"))
    }

    @Test
    fun `pasting several lines makes break runs`() {
        assertEquals(listOf(Plain("가"), Break, Plain("나")), RunEdit.replace(emptyList(), 0, 0, "가\n나"))
    }

    @Test
    fun `SCR-15 a selection across runs is replaced and the cut runs keep their kinds`() {
        val runs = listOf(Paren("작게"), Plain(" 저기"))
        assertEquals(listOf(Paren("작요"), Plain(" 저기")), RunEdit.replace(runs, 1, 2, "요"))
        assertEquals(listOf(Paren("작"), Plain("요저기")), RunEdit.replace(runs, 1, 3, "요"))
    }
}
