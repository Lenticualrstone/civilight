package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateA
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateB
import io.github.lenticualrstone.civilight.domain.world.term.AliasEdit
import io.github.lenticualrstone.civilight.domain.world.term.BulkMode
import io.github.lenticualrstone.civilight.domain.world.term.BulkSheet
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.CandidateKind
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TermCheckRow
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.RejectionContainerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD §4.2: usage, candidates and `▲` on Fixture F (WLD-T01 … WLD-T14). */
class TermUsageSpecTest {
    private val b12 = K.BLOCK_B12
    private val b32 = K.BLOCK_B32
    private val b42 = K.BLOCK_B42
    private val b61 = K.BLOCK_B61
    private val b7344 = K.BLOCK_044

    private fun b12Candidate(q: TermQueries) = q.candidates(T1).single { (it.spot as ScriptSpot.BlockText).blockKey == b12 }

    @Test
    fun `WLD-T01 card in state A`() {
        val q = queries(stateA())
        val card = q.card(T1)!!
        assertEquals("쓰인 곳 · 2 · 후보 1", card.header)
        assertEquals("쓰인 곳 · 2", card.usage.title)
        assertEquals(listOf(b42, b7344), card.usage.rows.map { (it.link.spot as ScriptSpot.BlockText).blockKey })
        assertEquals(listOf("▲ 공개(1-7) 전 사용"), card.usage.rows[0].markers)
        assertEquals(listOf("◇ 첫 공개"), card.usage.rows[1].markers)
        assertEquals("후보 · 1 · 확인 안 함", card.candidates.title)
        val c = card.candidates.rows.single().candidate
        assertEquals(ScriptSpot.BlockText(b12), c.spot)
        assertEquals(CandidateKind.CUT_OFF, c.kind)
        assertEquals("오리지", c.matchedText)
        assertEquals(10 to 13, c.start to c.end)
        assertEquals("◇ 후보 · 끊김 · 잇기 · 아님", card.candidates.rows.single().marker)
        assertEquals("원고 밖 · 메모 · 1", card.outside.title)
        val page = card.outside.rows.single()
        assertEquals("페이지 · 하층 구역의 빛", page.location)
        assertTrue(page.markers.none { it.startsWith("▲") })
        assertFalse(page.preReveal)
        assertTrue(q.candidates(T1).none { (it.spot as ScriptSpot.BlockText).blockKey in setOf(b32, b61) })
    }

    @Test
    fun `WLD-T02 list row in state A`() {
        val list = queries(stateA()).list()
        assertEquals("공개됨 · 최신 · 1-7", list.revealedTitle)
        val row = list.revealed.single { it.termKey == T1 }
        assertEquals("첫 공개 1-7 · ◇ 후보 1", row.line1)
        assertEquals("▲ 앞에서 1회 사용", row.line2)
    }

    @Test
    fun `WLD-T03 check rows in state A`() {
        val report = queries(stateA()).checks()
        assertEquals(2, report.rows.size)
        val warning = report.rows[0] as TermCheckRow.BeforeReveal
        assertEquals("▲ 공개 전 사용 · 1-4 씬 2", warning.title)
        assertEquals("세라: “[오리지늄]은 타지 않아. 빛만 남지.”", warning.snippet.renderBoxed())
        assertEquals("‘오리지늄 등불’ 첫 공개 1-7 · 사용 1-4", warning.detail)
        assertEquals(listOf("원고로 가기", "공개 시점 옮기기", "맞음"), warning.buttons)
        val candidate = report.rows[1] as TermCheckRow.CandidateBeforeReveal
        assertEquals(
            "◇ 공개 전 후보 · 1-2 씬 1 · 리엔: “등불 속 그 돌, 오리지… 뭐라고 했죠?” · ‘오리지늄 등불’ 끊김 · 첫 공개 1-7",
            candidate.text,
        )
        assertEquals(listOf("원고로 가기", "잇기", "아님"), candidate.buttons)
        assertEquals(1, report.badgeCount)
        assertEquals(2, report.totalCount)
    }

    @Test
    fun `WLD-T04 link the cut-off candidate`() {
        val doc = stateA()
        val q = queries(doc)
        val linked = TermEdits.link(doc, b12Candidate(q), ctx())!!
        val runs = linked.doc.index.block(b12)!!.runs
        val link = runs.filterIsInstance<TextRun.Link>().single()
        assertEquals("오리지", link.text)
        assertEquals(T1, link.target.key)
        assertEquals(linked.linkKeys.single(), link.linkKey)
        assertEquals("등불 속 그 돌, ", (runs[0] as TextRun.Plain).text)
        val after = queries(linked.doc)
        assertEquals("쓰인 곳 · 3", after.card(T1)!!.header)
        val row = after.list().revealed.single { it.termKey == T1 }
        assertEquals("첫 공개 1-7", row.line1)
        assertEquals("▲ 앞에서 2회 사용", row.line2)
        val checks = after.checks()
        val b12Row = checks.rows.filterIsInstance<TermCheckRow.BeforeReveal>().single { it.link.linkKey == link.linkKey }
        assertEquals("▲ 공개 전 사용 · 1-2 씬 1", b12Row.title)
        assertEquals(listOf("원고로 가기", "공개 시점 옮기기", "맞음"), b12Row.buttons)
        assertEquals(2, checks.badgeCount)
        // The stale index entry is not shown either.
        assertTrue(TermQueries(linked.doc, q.index).candidates(T1).isEmpty())
    }

    @Test
    fun `WLD-T05 intended use after linking`() {
        val doc = stateA()
        val linked = TermEdits.link(doc, b12Candidate(queries(doc)), ctx())!!
        val link = queries(linked.doc).usage(T1).single { it.linkKey == linked.linkKeys.single() }
        val marked = TermEdits.markIntended(linked.doc, link, ctx())
        val ack = marked.checks.acknowledgements.single { it.subjectKeys == listOf(link.linkKey) }
        assertEquals("term.beforeReveal", ack.checkType)
        val q = queries(marked)
        val report = q.checks()
        assertEquals(listOf(link.linkKey), report.intended.map { it.link.linkKey })
        assertEquals(1, report.badgeCount)
        assertEquals("▲ 앞에서 1회 사용", q.list().revealed.single { it.termKey == T1 }.line2)
        assertTrue(q.card(T1)!!.usage.rows.single { it.link.linkKey == link.linkKey }.tags.contains("맞음"))
    }

    private fun rejectedB12(): Pair<io.github.lenticualrstone.civilight.model.doc.ProjectDoc, TermQueries> {
        val doc = stateA()
        val rejected = TermEdits.reject(doc, b12Candidate(queries(doc)), ctx())
        return rejected to queries(rejected)
    }

    @Test
    fun `WLD-T06 reject the cut-off candidate`() {
        val (doc, q) = rejectedB12()
        val r = doc.index.term(T1)!!.rejections.single()
        assertEquals(RejectionContainerKind.BLOCK.open(), r.containerKind)
        assertEquals(b12, r.containerKey)
        assertNull(r.optionKey)
        assertEquals("오리지", r.matchedText)
        assertEquals(0, r.occurrence)
        assertEquals("쓰인 곳 · 2", q.card(T1)!!.header)
        assertEquals("첫 공개 1-7", q.list().revealed.single { it.termKey == T1 }.line1)
        assertTrue(q.checks().rows.none { it is TermCheckRow.CandidateBeforeReveal })
        assertEquals(1, q.card(T1)!!.rejections.size)
    }

    @Test
    fun `WLD-T07 rejection survives other edits`() {
        val (doc, q) = rejectedB12()
        val edited = WorldFixture.withBlockText(doc, b12, "등불 속 그 돌, 오리지… 뭐였죠?")
        val (index, _) = q.index.rescanSpots(edited, listOf(ScriptSpot.BlockText(b12)))
        val released = TermEdits.releaseStaleRejections(edited, index)
        assertTrue(TermQueries(released, index).candidates(T1).isEmpty())
        assertEquals(1, released.index.term(T1)!!.rejections.size)
    }

    @Test
    fun `WLD-T08 rejection released when the matched text changes`() {
        val (doc, q) = rejectedB12()
        val edited = WorldFixture.withBlockText(doc, b12, "등불 속 그 돌, 오리… 뭐라고 했죠?")
        val (index, _) = q.index.rescanSpots(edited, listOf(ScriptSpot.BlockText(b12)))
        val released = TermEdits.releaseStaleRejections(edited, index)
        val c = TermQueries(released, index).candidates(T1).single()
        assertEquals(CandidateKind.CUT_OFF, c.kind)
        assertEquals("오리", c.matchedText)
        assertEquals(10 to 12, c.start to c.end)
        assertTrue(released.index.term(T1)!!.rejections.isEmpty())
    }

    @Test
    fun `WLD-T09 rejection survives renumbering and a move to another episode`() {
        val (doc, _) = rejectedB12()
        val moved = WorldFixture.withBlock(WorldFixture.moveBlockToSceneStart(doc, b12, K.SCENE_1_3_1), b12) { it.copy(number = 12) }
        assertEquals(K.EPISODE_1_3, moved.index.episodeGroupOfBlock(b12))
        val index = CandidateIndex.build(moved)
        val released = TermEdits.releaseStaleRejections(moved, index)
        assertTrue(TermQueries(released, index).candidates(T1).isEmpty())
        assertEquals(1, released.index.term(T1)!!.rejections.size)
    }

    @Test
    fun `WLD-T10 adding an alias rescans that term only`() {
        val doc = stateA()
        val before = WorldFixture.index(doc)
        val edit = TermEdits.setAliases(doc, T1, listOf("오리지늄", "빛돌"), ctx()) as AliasEdit.Applied
        val (after, report) = before.rescanTerm(edit.doc, T1)
        assertEquals(setOf(T1), report.sweptTerms)
        assertEquals(2, TermEdits.newlyFound(before, after, T1))
        val q = TermQueries(edit.doc, after)
        assertEquals("쓰인 곳 · 2 · 후보 3", q.card(T1)!!.header)
        val cs = q.candidates(T1)
        assertEquals(listOf(b12, b32, b61), cs.map { (it.spot as ScriptSpot.BlockText).blockKey })
        assertEquals(listOf("오리지", "빛돌", "빛돌"), cs.map { it.matchedText })
        assertEquals(listOf(CandidateKind.CUT_OFF, CandidateKind.FULL, CandidateKind.FULL), cs.map { it.kind })
        assertEquals(doc.index.block(b32)!!.runs, edit.doc.index.block(b32)!!.runs)
        assertEquals(queries(doc).allLinks, q.allLinks)
    }

    @Test
    fun `WLD-T11 link all`() {
        val doc = stateB()
        val q = queries(doc)
        val sheet = BulkSheet.open(q, T1, BulkMode.LINK_ALL)!!
        assertEquals("후보 3 · 오리지늄 등불", sheet.title)
        assertEquals(listOf("1-2 씬 1 · 리엔 · 끊김", "1-3 씬 2 · 세라 · 별칭 빛돌", "1-6 씬 1 · 오웬 · 별칭 빛돌"), sheet.rows.map { it.location })
        assertTrue(sheet.rows.all { it.checked })
        assertEquals("고른 3개 · 넣으면 ▲ 공개 전 사용 1 → 4", sheet.footer)
        assertEquals("넣기 · 3개", sheet.button)
        val applied = sheet.apply(doc, ctx())!!
        assertEquals(3, applied.linkKeys.size)
        val after = queries(applied.doc)
        assertEquals("쓰인 곳 · 5", after.card(T1)!!.header)
        assertEquals(4, after.activeWarnings(T1).size)
        // One undo step restores the doc from before the sheet (pure edits keep it); unlinking the
        // three links also gives back exactly the text of state B.
        val undone = applied.linkKeys.fold(applied.doc) { d, k -> TermEdits.unlink(d, k) }
        assertEquals(doc.allBlocks.map { it.key to it.runs }, undone.allBlocks.map { it.key to it.runs })
        assertEquals("쓰인 곳 · 2 · 후보 3", queries(undone).card(T1)!!.header)
    }

    @Test
    fun `WLD-T12 link all with one row unchecked`() {
        val q = queries(stateB())
        val sheet = BulkSheet.open(q, T1, BulkMode.LINK_ALL)!!
        val unchecked = sheet.toggle(sheet.rows.single { (it.candidate.spot as ScriptSpot.BlockText).blockKey == b61 }.candidate)
        assertEquals("고른 2개 · 넣으면 ▲ 공개 전 사용 1 → 3", unchecked.footer)
        assertEquals("넣기 · 2개", unchecked.button)
    }

    @Test
    fun `WLD-T13 reject all`() {
        val doc = stateB()
        val sheet = BulkSheet.open(queries(doc), T1, BulkMode.REJECT_ALL)!!
        assertNull(sheet.footer)
        val applied = sheet.apply(doc, ctx())!!
        val rejections = applied.doc.index.term(T1)!!.rejections
        assertEquals(listOf(b12 to "오리지", b32 to "빛돌", b61 to "빛돌"), rejections.map { it.containerKey to it.matchedText })
        assertTrue(rejections.all { it.occurrence == 0 })
        assertEquals("쓰인 곳 · 2", queries(applied.doc).card(T1)!!.header)
    }

    @Test
    fun `WLD-T14 linked text is never a candidate`() {
        val index = WorldFixture.index(stateA())
        assertTrue(WorldFixture.candidatesAt(index, b42).isEmpty())
        assertTrue(WorldFixture.candidatesAt(index, b7344).isEmpty())
        assertNotNull(WorldFixture.candidatesAt(index, b12).singleOrNull())
    }
}
