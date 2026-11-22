package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.ordered
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Card text editing that keeps links and paragraphs (WLD-06 `설명`, WLD-17 links). */
class RichTextEditTest {
    private val term = WorldTestFixture.T1
    private val l1 = WorldTestFixture.link("LINK00000000000000000000001", term, "오리지늄 등불")

    /** `대정전 이후 [오리지늄 등불]을 가진 쪽` / `거리를 가진다.` */
    private fun sample(): RichDoc {
        val orders = OrderKeys.evenlySpaced(2)
        return listOf(
            DocBlock("PARA1", orders[0], DocBlockKind.PARAGRAPH.open(), listOf(TextRun.Plain("대정전 이후 "), l1, TextRun.Plain("을 가진 쪽"))),
            DocBlock("PARA2", orders[1], DocBlockKind.PARAGRAPH.open(), listOf(TextRun.Plain("거리를 가진다."))),
        )
    }

    private fun links(doc: RichDoc) = doc.flatMap { it.runs }.filterIsInstance<TextRun.Link>()

    @Test
    fun `the editor text joins paragraphs with new lines`() {
        assertEquals("대정전 이후 오리지늄 등불을 가진 쪽\n거리를 가진다.", RichTextEdit.textOf(sample()))
    }

    @Test
    fun `an unchanged text returns the same document`() {
        val doc = sample()
        assertSame(doc, RichTextEdit.apply(doc, RichTextEdit.textOf(doc), WorldTestFixture.keys()))
    }

    @Test
    fun `typing outside a link keeps the link key and the paragraph keys`() {
        // The editor applies each change as it happens (two separate edits here).
        val first = RichTextEdit.apply(sample(), "그날 대정전 이후 오리지늄 등불을 가진 쪽\n거리를 가진다.", WorldTestFixture.keys())
        val edited = RichTextEdit.apply(first, "그날 대정전 이후 오리지늄 등불을 가진 쪽\n거리를 가진다!", WorldTestFixture.keys())
        assertEquals(listOf(l1), links(edited))
        assertEquals(listOf("PARA1", "PARA2"), edited.ordered().map { it.key })
        assertEquals("그날 대정전 이후 오리지늄 등불을 가진 쪽\n거리를 가진다!", RichTextEdit.textOf(edited))
    }

    @Test
    fun `one edit spanning text on both sides of a link takes the link with it`() {
        val edited = RichTextEdit.apply(sample(), "그날 대정전 이후 오리지늄 등불을 가진 쪽\n거리를 가진다!", WorldTestFixture.keys())
        assertTrue(links(edited).isEmpty())
    }

    @Test
    fun `a trailing space is dropped from the draft but the next letter comes back after it`() {
        val keys = WorldTestFixture.keys()
        val spaced = RichTextEdit.apply(sample(), RichTextEdit.textOf(sample()) + " ", keys)
        assertEquals(sample(), spaced)
        val typed = RichTextEdit.apply(spaced, RichTextEdit.textOf(sample()) + " 끝", keys)
        assertEquals(RichTextEdit.textOf(sample()) + " 끝", RichTextEdit.textOf(typed))
        assertEquals(listOf(l1), links(typed))
    }

    @Test
    fun `an edit inside a link turns it into plain text`() {
        val edited = RichTextEdit.apply(sample(), "대정전 이후 오리지늄 등잔을 가진 쪽\n거리를 가진다.", WorldTestFixture.keys())
        assertTrue(links(edited).isEmpty())
        assertEquals("대정전 이후 오리지늄 등잔을 가진 쪽\n거리를 가진다.", RichTextEdit.textOf(edited))
    }

    @Test
    fun `a new line starts a new paragraph with a new key`() {
        val edited = RichTextEdit.apply(sample(), "대정전 이후 오리지늄 등불을 가진 쪽\n거리를 가진다.\n빛은 권력이다.", WorldTestFixture.keys())
        val ordered = edited.ordered()
        assertEquals(3, ordered.size)
        assertEquals(listOf("PARA1", "PARA2"), ordered.take(2).map { it.key })
        assertEquals("빛은 권력이다.", ordered[2].runs.joinToString("") { it.shownText })
    }

    @Test
    fun `a blank text empties the description`() {
        assertEquals(emptyList<DocBlock>(), RichTextEdit.apply(sample(), "  \n ", WorldTestFixture.keys()))
    }

    @Test
    fun `WLD-17 a link is inserted at the caret and plain text splits around it`() {
        val l2 = WorldTestFixture.link("LINK00000000000000000000002", WorldTestFixture.K.TERM_BLACKOUT, "대정전")
        val text = RichTextEdit.textOf(sample())
        val caret = text.indexOf("거리")
        val (doc, after) = RichTextEdit.insertLink(sample(), caret, l2, WorldTestFixture.keys())
        assertEquals("대정전 이후 오리지늄 등불을 가진 쪽\n대정전거리를 가진다.", RichTextEdit.textOf(doc))
        assertEquals(caret + 3, after)
        assertEquals(listOf(l1, l2), links(doc))
        assertEquals(listOf("PARA1", "PARA2"), doc.ordered().map { it.key })
    }

    @Test
    fun `WLD-17 a caret inside a link moves to its end`() {
        val l2 = WorldTestFixture.link("LINK00000000000000000000002", WorldTestFixture.K.TERM_BLACKOUT, "대정전")
        val inside = RichTextEdit.textOf(sample()).indexOf("지늄")
        val (doc, after) = RichTextEdit.insertLink(sample(), inside, l2, WorldTestFixture.keys())
        assertEquals("대정전 이후 오리지늄 등불대정전을 가진 쪽\n거리를 가진다.", RichTextEdit.textOf(doc))
        assertEquals("대정전 이후 오리지늄 등불대정전".length, after)
        assertEquals(listOf(l1, l2), links(doc))
    }

    @Test
    fun `WLD-17 a link goes into an empty description as its first paragraph`() {
        val (doc, after) = RichTextEdit.insertLink(emptyList(), 0, l1, WorldTestFixture.keys())
        assertEquals("오리지늄 등불", RichTextEdit.textOf(doc))
        assertEquals(1, doc.size)
        assertEquals(7, after)
    }

    @Test
    fun `WLD-17 the draft takes typed text first, then the picked link with a new key`() {
        val keys = WorldTestFixture.keys()
        val (doc, caret) = DescriptionDraft.insert(sample(), "대정전 이후 오리지늄 등불을 가진 쪽\n그 거리를 가진다.", "대정전 이후 오리지늄 등불을 가진 쪽\n그 ".length,
            LinkTargetKind.PLACE, WorldTestFixture.K.PLACE_LOWER_STREET, "하층 거리", keys)
        assertEquals("대정전 이후 오리지늄 등불을 가진 쪽\n그 하층 거리거리를 가진다.", RichTextEdit.textOf(doc))
        val inserted = links(doc).last()
        assertEquals(LinkTargetKind.PLACE, inserted.target.kind.known)
        assertEquals(WorldTestFixture.K.PLACE_LOWER_STREET, inserted.target.key)
        assertTrue(inserted.linkKey != l1.linkKey)
        assertEquals("대정전 이후 오리지늄 등불을 가진 쪽\n그 하층 거리".length, caret)
    }

    @Test
    fun `the @ picker kinds map onto link kinds`() {
        assertEquals(LinkTargetKind.CHARACTER, DescriptionDraft.linkKind(PickKind.CHARACTER))
        assertEquals(LinkTargetKind.TERM, DescriptionDraft.linkKind(PickKind.TERM))
        assertEquals(LinkTargetKind.PLACE, DescriptionDraft.linkKind(PickKind.PLACE))
        assertNull(DescriptionDraft.linkKind(PickKind.UNIT))
    }

    @Test
    fun `the draft survives the save format`() {
        val saved = with(DescriptionDraft.Saver) { androidx.compose.runtime.saveable.SaverScope { true }.save(sample()) }!!
        assertEquals(sample(), DescriptionDraft.Saver.restore(saved))
    }
}
