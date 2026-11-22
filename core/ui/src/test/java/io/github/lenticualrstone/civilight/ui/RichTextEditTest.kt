package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.ui.lens.LensEditBanners
import io.github.lenticualrstone.civilight.ui.lens.LensEditCopy
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.richtext.LinkLook
import io.github.lenticualrstone.civilight.ui.richtext.LinkPick
import io.github.lenticualrstone.civilight.ui.richtext.RichDocEdit
import io.github.lenticualrstone.civilight.ui.richtext.RichTextCopy
import io.github.lenticualrstone.civilight.ui.richtext.RichTextEditorState
import io.github.lenticualrstone.civilight.ui.richtext.ShownText
import io.github.lenticualrstone.civilight.ui.richtext.SpanKind
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shared rich text editor's logic: runs and paragraphs survive plain-text editing. */
class RichTextEditTest {
    private var n = 0
    private val newKey = { "NEW${n++}" }
    private val ext = JsonObject(mapOf("x" to JsonPrimitive(1)))
    private val link = TextRun.Link("L1", LinkTarget(LinkTargetKind.CHARACTER, "C1"), "리엔")
    private val unknown = TextRun.Unknown(JsonObject(mapOf("k" to JsonPrimitive("ruby"), "t" to JsonPrimitive("등불"))))
    private val source = RecorderSource("R1", 1_000L)

    /** `리엔은 (조용히) 웃었다` / `⌁ 메모 한 줄` (recorder) / `등불 끝`. */
    private val doc: RichDoc = listOf(
        DocBlock("P1", "V", runs = listOf(link, TextRun.Plain("은 "), TextRun.Paren("조용히"), TextRun.Plain(" 웃었다")), ext = ext),
        DocBlock("P2", "W", DocBlockKind.MEMO.open(), listOf(TextRun.Plain("메모 한 줄")), recorderSource = source),
        DocBlock("P3", "X", runs = listOf(unknown, TextRun.Plain(" 끝"))),
    )

    private fun text(d: RichDoc = doc) = RichDocEdit.textOf(d)

    @Test
    fun `the editor text joins paragraphs and shows runs without parentheses`() {
        assertEquals("리엔은 조용히 웃었다\n메모 한 줄\n등불 끝", text())
    }

    @Test
    fun `typing at the end keeps every run, key, kind, source and ext`() {
        val edited = RichDocEdit.apply(doc, text() + "다", newKey)
        assertEquals(listOf("P1", "P2", "P3"), edited.map { it.key })
        assertEquals(doc[0], edited[0])
        assertEquals(doc[1], edited[1])
        assertEquals(unknown, edited[2].runs.first())
        assertEquals("등불 끝다", text(edited).substringAfterLast('\n'))
    }

    @Test
    fun `typing inside a link edits the link and keeps its key`() {
        val t = text()
        val edited = RichDocEdit.apply(doc, t.substring(0, 1) + "_" + t.substring(1), newKey)
        val first = edited[0].runs.first() as TextRun.Link
        assertEquals("L1", first.linkKey)
        assertEquals("리_엔", first.text)
    }

    @Test
    fun `replacing a whole link turns it into plain text`() {
        val edited = RichDocEdit.apply(doc, "노아" + text().substring(2), newKey)
        assertEquals(TextRun.Plain("노아은 "), edited[0].runs.first())
        assertTrue(edited[0].runs.none { it is TextRun.Link })
    }

    @Test
    fun `typing inside a paren run stays a paren and paren mode types parens`() {
        val t = text()
        val at = t.indexOf("조용히") + 1
        val inside = RichDocEdit.apply(doc, t.substring(0, at) + "아주" + t.substring(at), newKey)
        assertEquals(TextRun.Paren("조아주용히"), inside[0].runs[2])
        val end = t.indexOf(" 웃었다")
        val plain = RichDocEdit.apply(doc, t.substring(0, end) + "만" + t.substring(end), newKey)
        assertEquals(TextRun.Plain("만 웃었다"), plain[0].runs[3])
        val paren = RichDocEdit.apply(doc, t.substring(0, end) + "만" + t.substring(end), newKey, parenMode = true)
        assertEquals(TextRun.Paren("조용히만"), paren[0].runs[2])
        val start = RichDocEdit.apply(doc, "(" + t, newKey, parenMode = true)
        assertEquals(TextRun.Paren("("), start[0].runs.first())
    }

    @Test
    fun `Hangul composition rewrites the last syllable in place`() {
        val base = RichDocEdit.apply(doc, text() + "ㅎ", newKey)
        val composed = RichDocEdit.apply(base, text() + "하", newKey)
        val done = RichDocEdit.apply(composed, text() + "한", newKey)
        assertEquals(text() + "한", text(done))
        assertEquals(listOf("P1", "P2", "P3"), done.map { it.key })
    }

    @Test
    fun `a new line splits a paragraph and the second half keeps the kind but not the source`() {
        val t = text()
        val at = t.indexOf("한 줄")
        val split = RichDocEdit.apply(doc, t.substring(0, at) + "\n" + t.substring(at), newKey)
        assertEquals(4, split.size)
        assertEquals("P2", split[1].key)
        assertEquals(source, split[1].recorderSource)
        assertEquals(DocBlockKind.MEMO, split[2].kind.known)
        assertNull(split[2].recorderSource)
        assertTrue(split[1].order < split[2].order && split[2].order < split[3].order)
        // A line typed on its own after the last paragraph is plain.
        val added = RichDocEdit.apply(doc, t + "\n새 줄", newKey)
        assertEquals(DocBlockKind.PARAGRAPH, added.last().kind.known)
        assertEquals("새 줄", (added.last().runs.single() as TextRun.Plain).text)
    }

    @Test
    fun `joining two paragraphs keeps the first one's key and an untouched unknown run`() {
        val t = text()
        val cut = t.lastIndexOf('\n')
        val joined = RichDocEdit.apply(doc, t.removeRange(cut, cut + 1), newKey)
        assertEquals(listOf("P1", "P2"), joined.map { it.key })
        assertEquals(unknown, joined[1].runs[1])
    }

    @Test
    fun `an edit touching an unknown run replaces it with plain text`() {
        val t = text()
        val at = t.indexOf("등불") + 1
        val edited = RichDocEdit.apply(doc, t.substring(0, at) + "x" + t.substring(at), newKey)
        assertTrue(edited[2].runs.none { it is TextRun.Unknown })
        assertEquals("등x불 끝", text(edited).substringAfterLast('\n'))
    }

    @Test
    fun `a run without text at the caret is kept once when typing there`() {
        val silent = TextRun.Unknown(JsonObject(mapOf("k" to JsonPrimitive("anchor"))))
        val d = listOf(DocBlock("P1", "V", runs = listOf(TextRun.Plain("앞"), silent, TextRun.Plain("뒤"))))
        val edited = RichDocEdit.apply(d, "앞x뒤", newKey)
        assertEquals(1, edited.single().runs.count { it == silent })
        assertEquals("앞x뒤", text(edited))
    }

    @Test
    fun `insert link at a caret, never inside another link`() {
        val noah = TextRun.Link("L2", LinkTarget(LinkTargetKind.CHARACTER, "C2"), "노아")
        val (inside, caret) = RichDocEdit.insertLink(doc, 1, noah, newKey)
        assertEquals(listOf(link, noah), inside[0].runs.take(2))
        assertEquals(4, caret)
        val (atEnd, end) = RichDocEdit.insertLink(doc, text().length, noah, newKey)
        assertEquals(noah, atEnd.last().runs.last())
        assertEquals(text().length + 2, end)
        val (empty, _) = RichDocEdit.insertLink(emptyList(), 0, noah, newKey)
        assertEquals(listOf(noah), empty.single().runs)
    }

    @Test
    fun `saving drops trailing empty paragraphs and an all-blank document`() {
        val withBlank = RichDocEdit.apply(doc, text() + "\n", newKey)
        assertEquals(4, withBlank.size)
        assertEquals(doc, RichDocEdit.trimmed(withBlank))
        assertEquals(emptyList<DocBlock>(), RichDocEdit.trimmed(RichDocEdit.apply(doc, " ", newKey)))
        assertEquals(emptyList<DocBlock>(), RichDocEdit.apply(doc, "", newKey))
    }

    @Test
    fun `the shown text adds parentheses and the memo mark and maps the caret both ways`() {
        val t = text()
        val shown = ShownText.of(t, RichDocEdit.layout(doc)) { LinkLook.DELETED }
        assertEquals("리엔은 (조용히) 웃었다\n⌁ 메모 한 줄\n등불 끝", shown.text)
        val parenStart = t.indexOf("조용히")
        assertEquals(shown.text.indexOf("("), shown.originalToShown(parenStart))
        val parenEnd = parenStart + 3
        assertEquals(shown.text.indexOf(")"), shown.originalToShown(parenEnd))
        val memo = t.indexOf("메모")
        assertEquals(shown.text.indexOf("메모"), shown.originalToShown(memo))
        for (o in 0..t.length) assertEquals(o, shown.shownToOriginal(shown.originalToShown(o)))
        // A tap inside the mark lands at the paragraph start.
        assertEquals(memo, shown.shownToOriginal(shown.text.indexOf("⌁") + 1))
        assertTrue(shown.spans.any { it.kind == SpanKind.LINK_DELETED && it.start == 0 && it.end == 2 })
        assertTrue(shown.spans.any { it.kind == SpanKind.PAREN && shown.text.substring(it.start, it.end) == "조용히" })
        assertTrue(shown.spans.any { it.kind == SpanKind.MEMO && shown.text.substring(it.start, it.end) == "메모 한 줄" })
        for (o in 0..t.length) assertTrue(shown.originalToShown(o) in 0..shown.text.length)
    }

    @Test
    fun `the editor state follows typing, inserts picks at the remembered caret and saves`() {
        val state = RichTextEditorState.of(doc, newKey)
        assertFalse(state.changed)
        state.onFieldChange(state.field.copy(text = state.field.text + "다", selection = androidx.compose.ui.text.TextRange(state.field.text.length + 1)))
        assertTrue(state.changed)
        state.onFieldChange(state.field.copy(selection = androidx.compose.ui.text.TextRange(0)))
        state.beginLink()
        state.insertLink(LinkPick(PickKind.FACTION, ExampleKeys.RIEN, "결사"))
        val first = state.doc.first().runs.first() as TextRun.Link
        assertEquals(LinkTargetKind.FACTION, first.target.kind.known)
        assertEquals(2, state.field.selection.start)
        state.insertLink(LinkPick(PickKind.REGION, "R", "구역"))
        assertEquals(1, state.doc.first().runs.count { it is TextRun.Link && it.target.kind.known == LinkTargetKind.FACTION })
        state.toggleParen()
        assertTrue(state.parenMode)
    }

    @Test
    fun `shared lens banner and editor copy`() {
        val example = ExampleProject.build()
        assertNull(LensEditBanners.of(example, readOnly = true))
        assertEquals("1-5 시점 값 고치는 중", LensEditCopy.title("1-5"))
        assertEquals("고치면 1-5부터 적용", LensEditCopy.subtitle("1-5"))
        for (value in RichTextCopy.all + LensEditCopy.TO_LATEST) assertTrue(value, CopyLint.lint(value).isEmpty())
    }
}
