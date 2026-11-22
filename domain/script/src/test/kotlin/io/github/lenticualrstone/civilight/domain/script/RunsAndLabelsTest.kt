package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Edge cases of text runs, labels and toast copy. */
class RunsAndLabelsTest {
    private val link = TextRun.Link("01JZ000000HREF000000000077", LinkTarget(LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN), "오리지늄 등불")

    @Test
    fun `split cuts plain and paren runs but never a link`() {
        val runs = listOf(TextRun.Paren("숨을 삼키며"), TextRun.Plain("하르트 씨, 방금…"))
        assertEquals(listOf(TextRun.Paren("숨을")) to listOf(TextRun.Paren(" 삼키며"), TextRun.Plain("하르트 씨, 방금…")), Runs.splitAt(runs, 2))
        val withLink = listOf(TextRun.Plain("꺼진 "), link, TextRun.Plain("이 깜박였다."))
        val (front, back) = Runs.splitAt(withLink, 5)
        assertEquals(listOf(TextRun.Plain("꺼진 "), link), front)
        assertEquals(listOf(TextRun.Plain("이 깜박였다.")), back)
        assertEquals(withLink.plainText, (front + back).plainText)
    }

    @Test
    fun `join adds one space and trims the seam`() {
        assertEquals(listOf(TextRun.Plain("가. 나.")), Runs.join(listOf(TextRun.Plain("가.  ")), listOf(TextRun.Plain("  나."))))
        assertEquals(listOf(TextRun.Plain("나.")), Runs.join(listOf(TextRun.Plain("  ")), listOf(TextRun.Plain("나."))))
        assertEquals(listOf(TextRun.Plain("가.")), Runs.join(listOf(TextRun.Plain("가.")), listOf(TextRun.Break)))
    }

    @Test
    fun `normalize merges plain runs and drops empty ones`() {
        assertEquals(
            listOf(TextRun.Plain("가나"), link),
            Runs.normalize(listOf(TextRun.Plain("가"), TextRun.Plain(""), TextRun.Plain("나"), TextRun.Paren(""), link)),
        )
    }

    @Test
    fun `typing inside a paren run stays in it and after a link becomes plain`() {
        assertEquals(listOf(TextRun.Paren("작게 웃으며")), Runs.insertText(listOf(TextRun.Paren("작게 며")), 3, "웃으"))
        assertEquals(listOf(link, TextRun.Plain("이")), Runs.insertText(listOf(link), link.text.length, "이"))
    }

    @Test
    fun `a candidate range never crosses a link or paren`() {
        val runs = listOf(TextRun.Plain("그 "), link)
        assertNull(Runs.linkRange(runs, 1, 4, "01JZ000000HREF000000000078", LinkTarget(LinkTargetKind.TERM, K.TERM_BLACKOUT)))
        assertNull(Runs.linkRange(runs, 1, 1, "01JZ000000HREF000000000078", LinkTarget(LinkTargetKind.TERM, K.TERM_BLACKOUT)))
    }

    @Test
    fun `copies re-mint link keys and recorder text keeps only script link kinds`() {
        var n = 0
        val reminted = Runs.remintLinks(listOf(link), { "01JZ000000HREF00000000010${n++}" })
        assertNotEquals(link.linkKey, (reminted.single() as TextRun.Link).linkKey)
        val fromRecorder = Runs.fromRecorder(
            listOf(
                TextRun.Link("01JZ000000HREF000000000079", LinkTarget(LinkTargetKind.CHARACTER, K.SERA, K.PROJECT), "세라"),
                TextRun.Plain("와 "),
                TextRun.Link("01JZ000000HREF000000000080", LinkTarget(LinkTargetKind.PAGE, K.PAGE_LOWER_LIGHT, K.PROJECT), "빛 노트"),
            ),
            { "01JZ000000HREF000000000081" },
        )
        assertEquals(LinkTarget(LinkTargetKind.CHARACTER, K.SERA), (fromRecorder.first() as TextRun.Link).target)
        assertEquals(TextRun.Plain("와 빛 노트"), fromRecorder.last())
    }

    @Test
    fun `export text draws parens with spacing and drops them on request`() {
        val runs = listOf(TextRun.Paren("작게"), TextRun.Plain("저기…"), TextRun.Break, TextRun.Plain("불빛이 보여요."))
        assertEquals("(작게) 저기…\n불빛이 보여요.", Runs.exportText(runs))
        assertEquals("저기…\n불빛이 보여요.", Runs.exportText(runs, includeParens = false))
        assertEquals("저기… 불빛이 보여요.", Runs.withoutParens(listOf(TextRun.Paren("작게"), TextRun.Plain(" 저기…  불빛이 보여요."))))
    }

    @Test
    fun `graphemes count clusters and spaces`() {
        assertEquals(18, Runs.graphemes("멈추지 마. 저건 등불이 아니야."))
        assertEquals(2, Runs.graphemes("éa"))
        assertEquals(0, Runs.graphemes(""))
    }

    @Test
    fun `number labels and particles`() {
        assertEquals("#007", Labels.number(7))
        assertEquals("#043", Labels.number(43))
        assertEquals("#1043", Labels.number(1043))
        assertEquals("#045-2", Labels.optionNumber(45, 2))
        assertEquals("은", Labels.topic(1))
        assertEquals("는", Labels.topic(2))
        assertEquals("은", Labels.topic(10))
        assertEquals("과", Labels.and(3))
        assertEquals("와", Labels.and(4))
        assertEquals("1,240", Labels.thousands(1240))
        assertEquals("를", Manuscript.objectParticle("세라"))
        assertEquals("을", Manuscript.objectParticle("리엔"))
    }

    @Test
    fun `toast copy for every delete shape`() {
        assertEquals("블록 #043 지움", Toasts.blocksDeleted(1, "#043", 0, 0, null))
        assertEquals("블록 #044 지움 · ◇ 1개는 #045 자리로", Toasts.blocksDeleted(1, "#044", 0, 1, "#045"))
        assertEquals("블록 #043 지움 · ◆ 1 · ◇ 1은 #045 자리로", Toasts.blocksDeleted(1, "#043", 1, 1, "#045"))
        assertEquals("블록 #043 지움 · ◆ 1개는 예정으로", Toasts.blocksDeleted(1, "#043", 1, 0, null))
        assertEquals("블록 5개 지움 · ◆ 2개 다음 자리로", Toasts.blocksDeleted(5, null, 2, 0, "#043"))
        assertEquals("씬 3 지움", Toasts.sceneDeleted(3, 0, 0, "씬 4"))
        assertEquals("씬 3 지움 · ◆ 2는 예정으로", Toasts.sceneDeleted(3, 2, 0, null))
        assertNull(Toasts.recordsTo(0, 0, "#042"))
        assertEquals("◆ 1 · ◇ 1 #042로", Toasts.recordsTo(1, 1, "#042"))
        assertNull(Toasts.moved(0, "씬 3", "씬 4"))
    }
}
