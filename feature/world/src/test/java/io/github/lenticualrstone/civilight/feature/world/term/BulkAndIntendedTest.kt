package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.world.term.BulkMode
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermChip
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.feature.world.list.WorldListState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.RejectionContainerKind
import io.github.lenticualrstone.civilight.model.world.TermRejection
import io.github.lenticualrstone.civilight.model.open
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-23 모두 잇기 · 모두 아님, WLD-24 맞음 on the card, WLD-25 글자 찾기 on the card. */
class BulkAndIntendedTest {
    private val ctx = WorldTestFixture.ctx()

    private fun ready(doc: ProjectDoc, mode: BulkMode, unchecked: Set<String> = emptySet()): BulkSheetUi =
        (LinkCandidatesState.build(doc, T1, WorldTestFixture.index(doc), false, mode, unchecked) as BulkSheetState.Ready).ui

    private fun card(doc: ProjectDoc) = TermCardState.build(doc, PROJECT, T1, WorldTestFixture.index(doc), false, readOnly = false)

    @Test
    fun `WLD-T11 the sheet in state B and 넣기 links all three in one step`() {
        val b = WorldTestFixture.stateB()
        val ui = ready(b, BulkMode.LINK_ALL)
        assertEquals("후보 3 · 오리지늄 등불", ui.title)
        assertEquals(listOf("1-2 씬 1 · 리엔 · 끊김", "1-3 씬 2 · 세라 · 별칭 빛돌", "1-6 씬 1 · 오웬 · 별칭 빛돌"), ui.rows.map { it.location })
        assertTrue(ui.rows.all { it.checked })
        assertEquals("“등불 속 그 돌, 오리지… 뭐라고 했죠?”", ui.rows[0].text)
        assertEquals("오리지", ui.rows[0].text.substring(ui.rows[0].boxStart, ui.rows[0].boxEnd))
        assertEquals("고른 3개 · 넣으면 ▲ 공개 전 사용 1 → 4", ui.footer)
        assertEquals("넣기 · 3개", ui.button)
        assertEquals("넣은 뒤 · 한 단계로 되돌리기", ui.note)
        val done = LinkCandidatesState.apply(b, T1, WorldTestFixture.index(b), false, BulkMode.LINK_ALL, emptySet(), ctx) as BulkApply.Done
        assertEquals(3, done.count)
        assertEquals("쓰인 곳 · 5", card(done.doc).header)
        assertEquals(4, TermQueries(done.doc, WorldTestFixture.index(done.doc)).activeWarnings(T1).size)
        assertEquals(5, TermQueries(done.doc, CandidateIndex.build(done.doc)).usage(T1).size)
    }

    @Test
    fun `WLD-T12 unchecking the b61 row`() {
        val b = WorldTestFixture.stateB()
        val b61 = ready(b, BulkMode.LINK_ALL).rows.single { it.location.startsWith("1-6") }.id
        val ui = ready(b, BulkMode.LINK_ALL, setOf(b61))
        assertFalse(ui.rows.single { it.id == b61 }.checked)
        assertEquals("고른 2개 · 넣으면 ▲ 공개 전 사용 1 → 3", ui.footer)
        assertEquals("넣기 · 2개", ui.button)
    }

    @Test
    fun `WLD-T13 모두 아님 stores three rejections and has no footer`() {
        val b = WorldTestFixture.stateB()
        val ui = ready(b, BulkMode.REJECT_ALL)
        assertNull(ui.footer)
        val done = LinkCandidatesState.apply(b, T1, WorldTestFixture.index(b), false, BulkMode.REJECT_ALL, emptySet(), ctx) as BulkApply.Done
        val rejections = done.doc.index.term(T1)!!.rejections
        assertEquals(listOf("오리지" to 0, "빛돌" to 0, "빛돌" to 0), rejections.map { it.matchedText to it.occurrence })
        assertEquals("쓰인 곳 · 2", card(done.doc).header)
    }

    @Test
    fun `the sheet waits for the first scan and the card offers the group action only when final`() {
        val b = WorldTestFixture.stateB()
        assertEquals(BulkSheetState.Pending, LinkCandidatesState.build(b, T1, CandidateIndex.pending(), false, BulkMode.LINK_ALL, emptySet()))
        assertTrue(TermCardState.build(b, PROJECT, T1, CandidateIndex.pending(), false, false).bulkActions.isEmpty())
        assertEquals(listOf("모두 잇기", "모두 아님"), card(b).bulkActions)
        val noCandidates = TermEdits.setTextSearch(b, T1, false, ctx)
        assertEquals(BulkSheetState.Empty, LinkCandidatesState.build(noCandidates, T1, WorldTestFixture.index(noCandidates), false, BulkMode.LINK_ALL, emptySet()))
    }

    @Test
    fun `a row whose text changed drops out and 넣기 links the rest`() {
        val b = WorldTestFixture.stateB()
        val old = WorldTestFixture.index(b)
        val edited = io.github.lenticualrstone.civilight.domain.world.common.DocRuns.updateBlock(b, K.BLOCK_B12) {
            it.copy(runs = listOf(io.github.lenticualrstone.civilight.model.text.TextRun.Plain("등불 속 그 돌. 뭐라고 했죠?")))
        }!!
        // The old index still lists b12; the sheet rebuilt on the new text drops it, so 넣기 links the other two.
        val done = LinkCandidatesState.apply(edited, T1, old, false, BulkMode.LINK_ALL, emptySet(), ctx) as BulkApply.Done
        assertEquals(2, done.count)
    }

    @Test
    fun `WLD-T05 맞음 on the card ▲ row keeps the link listed with a tag and lowers the counts`() {
        val a = WorldTestFixture.stateA()
        val row = card(a).groups.first().rows.first { it.canMarkIntended }
        assertTrue(row.markers.first().warn)
        val queries = TermQueries(a, WorldTestFixture.index(a))
        val link = queries.usage(T1).single { it.linkKey == row.linkKey }
        val marked = TermEdits.markIntended(a, link, ctx)
        val after = card(marked).groups.first().rows.single { it.linkKey == row.linkKey }
        assertFalse(after.canMarkIntended)
        assertTrue("맞음" in after.tags)
        assertFalse("the ▲ line is no longer a warning", after.markers.first().warn)
        val list = WorldListState.build(marked, io.github.lenticualrstone.civilight.navigation.WorldTab.TERMS, WorldTestFixture.index(marked), false)
        assertTrue(list.toString(), list.toString().contains("첫 공개 1-7"))
        assertFalse(list.toString(), list.toString().contains("▲ 앞에서"))
        // Candidates never offer 맞음 (WLD-22).
        assertTrue(card(a).groups.flatMap { it.rows }.filter { it.candidate != null }.none { it.canMarkIntended })
    }

    @Test
    fun `WLD-T31 글자 찾기 off keeps the rejection and on brings candidates back without b12`() {
        val a = WorldTestFixture.stateA()
        val rejected = TermEdits.reject(a, WorldTestFixture.index(a).of(T1).single(), ctx)
        val off = TermEdits.setTextSearch(rejected, T1, false, ctx)
        val offCard = card(off)
        assertTrue(offCard.searchOff)
        assertTrue(offCard.groups.none { it.id == "candidates" })
        assertEquals(1, off.index.term(T1)!!.rejections.size)
        val on = TermEdits.setTextSearch(off, T1, true, ctx)
        val onCard = card(on)
        assertFalse(onCard.searchOff)
        assertTrue(onCard.groups.none { it.id == "candidates" })
    }

    @Test
    fun `WLD-T28 the 글자 찾기 끄기 chip appears after the eleventh rejection`() {
        val a = WorldTestFixture.stateA()
        fun withRejections(n: Int) = WorldTestFixture.withTerm(a, T1) { t ->
            t.copy(rejections = (1..n).map { i ->
                TermRejection(
                    key = "R$i", containerKind = RejectionContainerKind.BLOCK.open(), containerKey = K.BLOCK_B12,
                    matchedText = "x$i", occurrence = 0,
                )
            })
        }
        assertTrue(card(withRejections(10)).chips.none { it is TermChip.TurnOffSearch })
        assertTrue(card(withRejections(11)).chips.any { it is TermChip.TurnOffSearch })
        assertFalse(card(withRejections(11)).searchOff)
    }

    @Test
    fun `WLD-22 a stale rejection is not listed under 아님으로 둔 것`() {
        val a = WorldTestFixture.stateA()
        val rejected = TermEdits.reject(a, WorldTestFixture.index(a).of(T1).single(), ctx)
        assertEquals(1, RejectionRows.build(rejected, T1, false).size)
        val changed = io.github.lenticualrstone.civilight.domain.world.common.DocRuns.updateBlock(rejected, K.BLOCK_B12) {
            it.copy(runs = listOf(io.github.lenticualrstone.civilight.model.text.TextRun.Plain("등불 속 그 돌, 오리… 뭐라고 했죠?")))
        }!!
        assertTrue(RejectionRows.build(changed, T1, false).isEmpty())
        assertEquals(TermAccess(), TermAccess(false))
    }
}
