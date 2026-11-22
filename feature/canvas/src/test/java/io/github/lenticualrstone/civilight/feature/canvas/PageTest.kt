package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.actions.RecorderActions
import io.github.lenticualrstone.civilight.domain.recorder.put.SourceMarkTarget
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrash
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.domain.trash.RefKind
import io.github.lenticualrstone.civilight.domain.trash.ReferenceScanner
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.feature.canvas.page.LinkState
import io.github.lenticualrstone.civilight.feature.canvas.page.PageAccess
import io.github.lenticualrstone.civilight.feature.canvas.page.PageAccessRules
import io.github.lenticualrstone.civilight.feature.canvas.page.PageCopy
import io.github.lenticualrstone.civilight.feature.canvas.page.PageEdits
import io.github.lenticualrstone.civilight.feature.canvas.page.PageListModel
import io.github.lenticualrstone.civilight.feature.canvas.page.PageViews
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.model.text.ordered
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Pages (CNV-13 … CNV-16) on the example project; spec §4.3 cases by id. */
class PageTest {
    private val zone = ExampleClock.ZONE
    private val ctx = CanvasFx.ctx()
    private val actx = ActionContext(now = ExampleClock.NOW, zone = zone, device = "test-device", newKey = CanvasFx.ctx(9).newKey)

    private fun view(doc: ProjectDoc, key: Key = K.PAGE_LOWER_LIGHT, open: Boolean = false, recorder: RecorderLibrary? = ExampleRecorder.library()) =
        PageViews.build(doc, key, open, readOnly = false, zone = zone, recorder = recorder)!!

    private fun noahLink(doc: ProjectDoc) = view(doc).links.single { it.linkKey == K.link(7) }

    @Test
    fun `CNV-13 the page reads as the board`() {
        val v = view(CanvasFx.s0())
        assertEquals(PageAccess.OPEN, v.access)
        assertEquals("페이지 · 설정 노트", v.sectionLine)
        assertEquals("하층 구역의 빛", v.name)
        // updatedAt 2026-07-02 12:00 Z; two distinct link targets (오리지늄 등불, 노아), no page links here.
        assertEquals("마지막 수정 7월 2일 · 연결 2", v.meta)
        assertEquals(listOf(DocBlockKind.PARAGRAPH, DocBlockKind.PARAGRAPH, DocBlockKind.MEMO), v.paragraphs.map { it.kind })
        val first = v.paragraphs.first()
        val lantern = first.links.single()
        assertEquals("오리지늄 등불", first.text.substring(lantern.start, lantern.end))
        assertEquals(LinkState.LIVE, lantern.link.state)
        assertEquals(io.github.lenticualrstone.civilight.navigation.TermCard(K.PROJECT, K.TERM_ORIGINIUM_LANTERN), lantern.link.route)
        assertEquals(lantern.link, first.linkAt(lantern.start))
        assertNull(first.linkAt(0))
        assertEquals(PageCopy.MEMO_LABEL, v.paragraphs.last().memoLabel)
        assertNull("no source record", v.sourceMark)
    }

    @Test
    fun `T-CNV-17 page links never raise term warnings`() {
        val doc = CanvasFx.s0()
        val q = TermQueries(doc, CandidateIndex.build(doc))
        assertTrue(q.outside(K.TERM_ORIGINIUM_LANTERN).any { it.linkKey == K.LINK_LPG })
        assertTrue(q.usage(K.TERM_ORIGINIUM_LANTERN).none { it.linkKey == K.LINK_LPG })
        assertTrue(q.activeWarnings(K.TERM_ORIGINIUM_LANTERN).none { it.linkKey == K.LINK_LPG })
        // The page shows a plain live link: no warning mark of its own.
        assertEquals(LinkState.LIVE, view(doc).links.single { it.linkKey == K.LINK_LPG }.state)
    }

    @Test
    fun `T-CNV-18 plain words are not references`() {
        val doc = CanvasFx.s0() // 노아 is in the trash in S0.
        val memo = doc.pages.getValue(K.PAGE_LOWER_LIGHT).blocks.ordered().last()
        assertEquals("2장에서 값이 두 배로 뛰는 장면 넣기. 노아 시점으로?", memo.runs.plainText)
        val shown = view(doc).paragraphs.last()
        assertEquals(memo.runs.plainText, shown.text)
        assertTrue(shown.links.isEmpty())
        val fromPage = ReferenceScanner(doc).referencesTo(ItemId.character(K.NOAH)).filter { it.containerKey == K.PAGE_LOWER_LIGHT || it.ownerKey == K.PAGE_LOWER_LIGHT }
        assertEquals("only the @노아 link counts", 1, fromPage.size)
        assertEquals(RefKind.LINK, fromPage.single().kind)
    }

    @Test
    fun `T-CNV-19 a link to a deleted item`() {
        val doc = CanvasFx.s0()
        val link = noahLink(doc)
        assertEquals(LinkState.TRASHED, link.state)
        assertTrue(link.dotted)
        assertEquals("◇ 노아", link.inline)
        assertEquals("지운 인물 · 노아", link.cardTitle)
        assertEquals(listOf("다른 항목에 잇기", "글로 풀기", "되살리기", "이대로 두기"), link.actions)

        // 글로 풀기: the letters stay, the link goes.
        val unlinked = PageEdits.unlink(doc, K.link(7), ctx)
        val runs = unlinked.pages.getValue(K.PAGE_LOWER_LIGHT).blocks.ordered()[1].runs
        assertTrue(runs.none { it is TextRun.Link })
        assertEquals("시장 아이들 중 노아만 빛돌 시세를 외운다.", runs.plainText)

        // 되살리기: the same link is solid again, same key and target.
        val restored = PageEdits.restore(doc, link.item!!, ctx, authenticated = true)
        assertNotNull(restored.doc)
        assertNotNull(restored.toast)
        val again = noahLink(restored.doc!!)
        assertEquals(LinkState.LIVE, again.state)
        assertEquals("노아", again.inline)

        // 영구 삭제: `영구 삭제 · 노아`, 글로 풀기 stays, 되살리기 goes.
        val purged = CharacterActions.purge(doc, ExampleRecorder.library(), K.NOAH, actx)!!.doc
        val gone = noahLink(purged)
        assertEquals(LinkState.GONE, gone.state)
        assertEquals("영구 삭제 · 노아", gone.cardTitle)
        assertEquals("◇ 영구 삭제 · 노아", gone.inline)
        assertTrue(PageCopy.BTN_UNLINK in gone.actions)
        assertFalse(PageCopy.BTN_RESTORE in gone.actions)
    }

    @Test
    fun `이대로 두기 and 다른 항목에 잇기 on a deleted link`() {
        val doc = CanvasFx.s0()
        val link = noahLink(doc)
        val kept = PageEdits.keep(doc, link.item!!, ctx)
        assertTrue(io.github.lenticualrstone.civilight.domain.trash.Fixes.isKept(kept, K.NOAH))
        assertEquals("the dotted mark stays", LinkState.TRASHED, noahLink(kept).state)

        val relinked = PageEdits.relink(doc, K.PAGE_LOWER_LIGHT, K.link(7), LinkTargetKind.CHARACTER, K.OWEN, "오웬", ctx)
        val run = relinked.pages.getValue(K.PAGE_LOWER_LIGHT).blocks.ordered()[1].runs.filterIsInstance<TextRun.Link>().single()
        assertEquals(K.link(7), run.linkKey)
        assertEquals(LinkTarget(LinkTargetKind.CHARACTER, K.OWEN), run.target)
        assertEquals("오웬", run.text)
        assertEquals(LinkState.LIVE, view(relinked).links.single { it.linkKey == K.link(7) }.state)
    }

    @Test
    fun `T-CNV-20 page made from a record`() {
        val doc = CanvasFx.s0()
        val library = ExampleRecorder.library()
        val record = library.record(K.RECORDER_R3)!!
        val prefill = io.github.lenticualrstone.civilight.domain.recorder.put.PageInsert.prefillName(record)
        val made = RecorderActions.makePage(doc, library, K.RECORDER_R3, prefill, actx)
        val page = made.doc.pages.values.single { it.sourceRecord?.recordKey == K.RECORDER_R3 }
        assertTrue(page.name.isNotEmpty())
        assertTrue("2장 첫 장면은 비 오는 상층 관문에서 시작.".startsWith(page.name) || page.name.startsWith("2장 첫 장면"))
        assertTrue(page.blocks.docText.contains(ExampleRecorder.R3_TEXT))
        val after = made.library
        assertNotNull("the record stays in the library", after.record(K.RECORDER_R3))
        assertTrue(after.placementsOf(K.RECORDER_R3).any { it.kind.known == PlacementKind.PAGE && it.targetKey == page.key })
        assertTrue(after.linksOf(K.RECORDER_R3).any { it.projectKey == K.PROJECT })

        // The page reads the record's mark; editing the page leaves the record untouched.
        val v = view(made.doc, page.key, recorder = after)
        assertEquals(SourceMarkTarget.RECORD_SCREEN, v.sourceMark!!.target)
        assertTrue(v.sourceMark.text.startsWith("레코더 "))
        val edited = PageEdits.setBlocks(made.doc, page.key, page.blocks + DocBlock(K.docBlock(990), "zz", runs = listOf(TextRun.Plain("더 쓴 문단"))), ctx)
        assertEquals(after.record(K.RECORDER_R3), RecorderLibrary(after.records, after.links, after.placements).record(K.RECORDER_R3))
        assertTrue(edited.pages.getValue(page.key).blocks.docText.contains("더 쓴 문단"))
        // Editing the record leaves the page untouched.
        val recordEdited = after.copy(records = after.records.map { if (it.key == K.RECORDER_R3) it.copy(runs = listOf(TextRun.Plain("고친 기록")), textRevision = 2) else it })
        assertEquals(page.blocks, made.doc.pages.getValue(page.key).blocks)
        assertNotNull(recordEdited.record(K.RECORDER_R3))
    }

    @Test
    fun `T-CNV-21 the source mark follows the record's deletion`() {
        val doc = CanvasFx.s0() // 등불 결사 규약 was made from R0.
        val library = ExampleRecorder.library()
        val before = view(doc, K.PAGE_SOCIETY_RULES, recorder = library)
        assertEquals(SourceMarkTarget.RECORD_SCREEN, before.sourceMark!!.target)
        assertTrue(before.paragraphs.first().memoLabel!!.startsWith("⌁ 작가 메모 · 레코더 "))

        val trashed = RecorderTrash.delete(RecorderTrashState(library, ExampleRecorder.trashEntries()), K.RECORDER_R0, actx.trash)
        val inTrash = view(doc, K.PAGE_SOCIETY_RULES, recorder = trashed.library)
        assertEquals("레코더 · 휴지통", inTrash.sourceMark!!.text)
        assertEquals(SourceMarkTarget.TRASH_RECORDER_FILTER, inTrash.sourceMark.target)

        val entry = trashed.entries.single { K.RECORDER_R0 in it.targetKeys }
        val (purged, _) = RecorderTrash.purge(trashed, entry.key, actx.trash)
        val gone = view(doc, K.PAGE_SOCIETY_RULES, recorder = purged.library)
        assertEquals("지워진 기록", gone.sourceMark!!.text)
        assertEquals(SourceMarkTarget.NONE, gone.sourceMark.target)
        assertEquals(before.paragraphs.map { it.text }, gone.paragraphs.map { it.text })
    }

    @Test
    fun `T-CNV-22 hidden page while locked`() {
        val s0 = CanvasFx.s0()
        val hidden = PageEdits.setHidden(s0, K.PAGE_LOWER_LIGHT, true, ctx)
        val list = PageListModel.build(hidden, hiddenOpen = false, readOnly = false, zone = zone)
        assertEquals(listOf(K.PAGE_SOCIETY_RULES), list.rows.map { it.key })
        assertEquals(1, list.lockedCount)
        assertEquals("숨긴 페이지 1", list.lockedRow)
        assertTrue(list.rows.none { it.name == "하층 구역의 빛" })

        // A link to it from another page reads `숨긴 항목`.
        val linking = hidden.copy(
            pages = hidden.pages + (K.PAGE_SOCIETY_RULES to hidden.pages.getValue(K.PAGE_SOCIETY_RULES).let { p ->
                p.copy(blocks = p.blocks + DocBlock(K.docBlock(991), "zz", runs = listOf(TextRun.Link(K.link(990), LinkTarget(LinkTargetKind.PAGE, K.PAGE_LOWER_LIGHT), "하층 구역의 빛"))))
            }),
        )
        val link = view(linking, K.PAGE_SOCIETY_RULES).links.single { it.linkKey == K.link(990) }
        assertEquals(LinkState.HIDDEN, link.state)
        assertEquals("숨긴 항목", link.inline)

        // The page itself shows only the lock; open, it is in place with `숨김`.
        assertEquals(PageAccess.HIDDEN_LOCKED, view(hidden).access)
        assertTrue(view(hidden).paragraphs.isEmpty())
        assertNull("nothing of a locked page shows, its header image included", view(hidden).headerImage)
        assertEquals("", view(hidden).name)
        // Hidden and trashed: still the lock, never the name.
        val hiddenTrashed = hidden.copy(pages = hidden.pages + (K.PAGE_LOWER_LIGHT to hidden.pages.getValue(K.PAGE_LOWER_LIGHT).copy(deletedAt = 1L)))
        assertEquals(PageAccess.HIDDEN_LOCKED, view(hiddenTrashed).access)
        val open = PageListModel.build(hidden, hiddenOpen = true, readOnly = false, zone = zone)
        assertEquals(0, open.lockedCount)
        assertTrue(open.rows.single { it.key == K.PAGE_LOWER_LIGHT }.hiddenTag)
        assertEquals(LinkState.LIVE, view(linking, K.PAGE_SOCIETY_RULES, open = true).links.single { it.linkKey == K.link(990) }.state)
    }

    @Test
    fun `links to hidden items keep the editor closed until unlocked`() {
        val s0 = CanvasFx.s0()
        val hiddenTerm = s0.copy(world = s0.world.copy(terms = s0.world.terms.map { if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(hidden = true) else it }))
        val locked = view(hiddenTerm)
        assertTrue(locked.hasLockedLinks)
        assertEquals("숨긴 항목", locked.links.single { it.linkKey == K.LINK_LPG }.inline)
        assertFalse(PageAccessRules.canEdit(locked))
        assertTrue(PageAccessRules.canEdit(view(hiddenTerm, open = true)))
        assertFalse(PageAccessRules.canEdit(PageViews.build(s0, K.PAGE_LOWER_LIGHT, false, readOnly = true, zone = zone)!!))
    }

    @Test
    fun `page list, new page and delete to the trash`() {
        val s0 = CanvasFx.s0()
        val list = PageListModel.build(s0, hiddenOpen = false, readOnly = false, zone = zone)
        assertEquals("페이지 · 2", list.section)
        assertEquals(listOf(K.PAGE_LOWER_LIGHT, K.PAGE_SOCIETY_RULES), list.rows.map { it.key })
        assertEquals("설정 노트 · 7월 2일", list.rows.first().sub)
        assertEquals("하", list.rows.first().initial)

        val (made, key) = PageEdits.create(s0, ctx)
        val fresh = view(made, key)
        assertTrue("a new page opens into editing", fresh.isBlank)
        assertEquals(PageCopy.UNNAMED, fresh.shownName)
        assertEquals(key, PageEdits.pages(made).last().key)
        val named = PageEdits.rename(made, key, "새 이름", ctx)
        assertEquals("새 이름", named.pages.getValue(key).name)

        val (deleted, toast) = PageEdits.delete(s0, K.PAGE_LOWER_LIGHT, ctx)!!
        assertTrue(toast.message.startsWith("페이지 하층 구역의 빛 지움"))
        assertEquals("되돌리기", toast.action)
        assertNotNull(deleted.pages.getValue(K.PAGE_LOWER_LIGHT).deletedAt)
        assertTrue(deleted.trash.entries.any { K.PAGE_LOWER_LIGHT in it.targetKeys })
        assertEquals(listOf(K.PAGE_SOCIETY_RULES), PageListModel.build(deleted, false, false, zone).rows.map { it.key })
        assertEquals(PageAccess.GONE, view(deleted).access)
        // Restoring the page brings it back to the list.
        val back = PageEdits.restore(deleted, ItemId.page(K.PAGE_LOWER_LIGHT), ctx, authenticated = true).doc!!
        assertNull(back.pages.getValue(K.PAGE_LOWER_LIGHT).deletedAt)
    }

    @Test
    fun `slash adds a block of a kind after the caret or turns an empty one`() {
        val blocks = CanvasFx.s0().pages.getValue(K.PAGE_LOWER_LIGHT).blocks.ordered()
        val keys = CanvasFx.ctx(11)
        val added = PageEdits.slash(blocks, DocBlockKind.HEADING, blocks[0].key, keys.newKey).ordered()
        assertEquals(4, added.size)
        assertEquals(blocks[0].key, added[0].key)
        assertEquals(DocBlockKind.HEADING, added[1].kind.known)
        assertTrue(added[1].runs.isEmpty())
        assertEquals(blocks[1].key, added[2].key)

        val turned = PageEdits.slash(added, DocBlockKind.MEMO, added[1].key, keys.newKey).ordered()
        assertEquals(4, turned.size)
        assertEquals(DocBlockKind.MEMO, turned[1].kind.known)

        val atEnd = PageEdits.slash(blocks, DocBlockKind.PARAGRAPH, null, keys.newKey).ordered()
        assertEquals(DocBlockKind.PARAGRAPH, atEnd.last().kind.known)
        assertEquals(4, atEnd.size)
    }

    @Test
    fun `page copy reads as the board`() {
        assertEquals("@로 인물 · 용어 연결, /로 블록 추가", PageCopy.HINT)
        assertEquals("마지막 수정 11월 20일 · 연결 4", PageCopy.meta("11월 20일", 4))
        assertEquals("페이지 · 세계관 메모", PageCopy.sectionLine("세계관 메모"))
        assertEquals("페이지", PageCopy.sectionLine(null))
        assertEquals("11월 20일", PageCopy.monthDay(java.time.Instant.parse("2026-11-20T03:00:00Z").toEpochMilli(), zone))
    }
}
