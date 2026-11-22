package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.checks.Fx.withAnchor
import io.github.lenticualrstone.civilight.domain.checks.Fx.withBlock
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermFingerprints
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rows 1–3 (`용어 공개`, WLD-28) against small edits of the example project. */
class TermChecksTest {
    private val doc = Fx.doc()

    /** CNV T-CNV-24: `맞음` moves the item to `맞다고 표시한 것` and `전체` drops by one. */
    @Test
    fun matchMovesTheWarningAndPersistsAcrossARevealMove() {
        val report = Fx.run(doc)
        val warning = report.ofType(CheckType.TERM_BEFORE_REVEAL).single()
        val acked = CheckAcks.acknowledge(doc, warning, Fx.ctx())
        val ack = acked.checks.acknowledgements.last()
        assertEquals(CheckType.TERM_BEFORE_REVEAL, ack.checkType)
        assertEquals(listOf(K.LINK_L42), ack.subjectKeys)
        assertEquals(TermFingerprints.beforeReveal(K.TERM_ORIGINIUM_LANTERN, K.LINK_L42), ack.fingerprint)
        assertEquals(AckKind.MATCH, ack.kind.known)

        val after = Fx.run(acked)
        assertEquals(report.count(CheckFilter.ALL) - 1, after.count(CheckFilter.ALL))
        assertEquals(3, after.count(CheckFilter.ACKNOWLEDGED))
        // The world area reads the same acknowledgement as `맞음` (no active ▲ left for the term).
        assertTrue(TermQueries(acked, CandidateIndex.build(acked)).activeWarnings(K.TERM_ORIGINIUM_LANTERN).isEmpty())

        val row = CheckCenterView.rows(after, CheckFilter.ACKNOWLEDGED).filterIsInstance<CheckListRow.Item>()
            .single { it.result.checkType == CheckType.TERM_BEFORE_REVEAL }
        assertEquals(listOf(FixAction("되돌리기", FixKind.REVERT, ack.key)), row.actions)

        // The acknowledgement is keyed by the link: moving the reveal (still after the use) keeps it.
        val moved = acked.withAnchor(K.R9, Anchor.UnitStart(K.EPISODE_1_8))
        assertEquals(1, Fx.run(moved).acknowledged.count { it.checkType == CheckType.TERM_BEFORE_REVEAL })

        // `되돌리기` reopens it.
        val reverted = CheckAcks.revert(acked, ack.key, Fx.ctx())
        assertEquals(report.count(CheckFilter.ALL), Fx.run(reverted).count(CheckFilter.ALL))
    }

    @Test
    fun revealBeforeEveryUseClearsWarningAndCandidates() {
        val report = Fx.run(doc.withAnchor(K.R9, Anchor.UnitStart(K.EPISODE_1_1)))
        assertTrue(report.ofType(CheckType.TERM_BEFORE_REVEAL).isEmpty())
        assertTrue(report.ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).isEmpty())
        assertEquals(4, report.count(CheckFilter.ALL))
    }

    @Test
    fun removingTheAliasDropsItsCandidates() {
        val stateA = doc.copy(world = doc.world.copy(terms = doc.world.terms.map {
            if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(aliases = it.aliases - "빛돌") else it
        }))
        val candidates = Fx.run(stateA).ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL)
        assertEquals(listOf("1-2 씬 1"), candidates.map { it.location })
        assertEquals(CheckFilter.TERM, candidates.single().filter)
    }

    @Test
    fun termWithoutRevealRaisesNoRevealRow() {
        val noReveal = doc.copy(changes = doc.changes.copy(records = doc.changes.records.map {
            if (it.key == K.R9) it.copy(deletedAt = 1L, trashEntryKey = "01JZ000000TRSH999999999999") else it
        }))
        val report = Fx.run(noReveal)
        val row = report.ofType(CheckType.TERM_NO_REVEAL).single()
        assertEquals("◇ 첫 공개 없음", row.title)
        assertEquals("용어 · 오리지늄 등불", row.location)
        assertEquals("오리지늄 등불 · 쓰인 곳 2", row.body)
        val chip = row.actions.first()
        assertEquals(FixKind.SET_REVEAL, chip.kind)
        assertTrue(chip.label, chip.label.startsWith("1-4 씬 2") && chip.label.endsWith("첫 공개로"))
        assertEquals("원고로 가기", row.actions.last().label)
        assertTrue(report.ofType(CheckType.TERM_BEFORE_REVEAL).isEmpty())
        assertTrue(report.ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).isEmpty())
    }

    /** A term revealed beyond the 집필 선: its pre-reveal links fold into `잿불의 서약 · ▲ n`. */
    @Test
    fun plannedTermLinksCollapseIntoOneRow() {
        val linked = linkEmberOath(doc)
        val report = Fx.run(linked)
        val warnings = report.ofType(CheckType.TERM_BEFORE_REVEAL).filter { it.group != null }
        assertEquals(2, warnings.size)
        val group = CheckCenterView.rows(report).filterIsInstance<CheckListRow.Group>().single()
        assertEquals("잿불의 서약 · ▲ 2", group.title)
        assertEquals(listOf("1-7 씬 3", "1-7 씬 3"), group.members.map { it.location })
        assertEquals(report.count(CheckFilter.ALL), Fx.run(doc).count(CheckFilter.ALL) + 2)
    }

    /** R21 §7: while the full scan runs the center shows `점검 중`, dims the counts and has no empty state. */
    @Test
    fun scanningWhileTheCandidateIndexIsPending() {
        val report = ProjectChecks.run(doc, Fx.inputs().copy(candidates = CandidateIndex.pending()))
        assertTrue(report.scanning)
        assertTrue(report.ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).isEmpty())
        assertEquals(1, report.ofType(CheckType.TERM_BEFORE_REVEAL).size)
        val header = CheckCenterView.header(report)
        assertEquals("점검 중", header.scanningLabel)
        assertTrue(header.chips.all { it.dimmed })
        assertNull(CheckCenterView.header(Fx.run(doc)).scanningLabel)
        assertNull(CheckCenterView.empty(ProjectChecks.run(Fx.allResolved(doc), Fx.inputs().copy(candidates = CandidateIndex.pending()))))
    }

    /** R19 §6: rows about a hidden term collapse while locked and come back when unlocked. */
    @Test
    fun hiddenTermRowsCollapseWhileLocked() {
        val hidden = doc.copy(world = doc.world.copy(terms = doc.world.terms.map {
            if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(hidden = true) else it
        }))
        val locked = Fx.run(hidden)
        val rows = CheckCenterView.rows(locked)
        assertEquals("▲ 숨긴 항목 관련 4 · 지문으로 보기", (rows.first() as CheckListRow.HiddenGroup).text)
        assertTrue(rows.drop(1).none { it is CheckListRow.Item && it.result.checkType.startsWith("term.") })
        assertEquals(8, locked.count(CheckFilter.ALL))

        val unlocked = Fx.run(hidden, Fx.inputs(hiddenUnlocked = true))
        assertTrue(CheckCenterView.rows(unlocked).none { it is CheckListRow.HiddenGroup })
        assertEquals(3, (CheckCenterView.rows(unlocked).first() as CheckListRow.Item).subRows.size)
    }

    private fun linkEmberOath(doc: ProjectDoc): ProjectDoc {
        fun link(n: Int) = TextRun.Link("01JZ000000HREF99999999990$n", LinkTarget(LinkTargetKind.TERM.open(), K.TERM_EMBER_OATH), "잿불의 서약")
        return doc
            .withBlock(K.block(1, 7, 36)) { it.copy(runs = it.runs + TextRun.Plain(" ") + link(1)) }
            .withBlock(K.block(1, 7, 37)) { it.copy(runs = it.runs + TextRun.Plain(" ") + link(2)) }
    }
}
