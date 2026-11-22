package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.key
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.link
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateA
import io.github.lenticualrstone.civilight.domain.world.common.Josa
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.RevealEdit
import io.github.lenticualrstone.civilight.domain.world.term.RevealMoves
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TermCheckRow
import io.github.lenticualrstone.civilight.domain.world.term.TermChip
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD §4.4 reveal comparison (WLD-T33 … WLD-T42). */
class RevealSpecTest {
    private val b12 = K.BLOCK_B12
    private val b42 = K.BLOCK_B42
    private val b7344 = K.BLOCK_044

    private fun appendRun(doc: ProjectDoc, blockKey: Key, run: TextRun): ProjectDoc =
        WorldFixture.withBlock(doc, blockKey) { it.copy(runs = it.runs + TextRun.Plain(" ") + run) }

    /** First dialogue or narration block of an episode, in manuscript order. */
    private fun textBlockOf(doc: ProjectDoc, episodeKey: Key): Key = doc.manuscript.liveBlocks.first {
        doc.index.numberingUnitKeyOfScene(it.sceneKey) == episodeKey &&
            it.kind.known in setOf(BlockKind.DIALOGUE, BlockKind.NARRATION)
    }.key

    @Test
    fun `WLD-T33 a link in the reveal block itself counts as after`() {
        val q = queries(stateA())
        val l44 = q.usage(T1).single { it.linkKey == K.LINK_L44 }
        assertFalse(q.isBeforeReveal(l44))
        assertEquals(listOf(K.LINK_L42), q.activeWarnings(T1).map { it.linkKey })
    }

    @Test
    fun `WLD-T34 flashback compared by manuscript order`() {
        val blackout = K.TERM_BLACKOUT
        var doc = appendRun(stateA(), b42, link(key(341), blackout, "대정전"))
        doc = appendRun(doc, K.BLOCK_B32, link(key(342), blackout, "대정전"))
        val q = queries(doc)
        assertEquals(K.BLOCK_B41, q.reveal(blackout)!!.blockKey)
        val inFlashback = q.usage(blackout).single { it.linkKey == key(341) }
        val earlier = q.usage(blackout).single { it.linkKey == key(342) }
        assertFalse(q.isBeforeReveal(inFlashback))
        assertTrue(q.isBeforeReveal(earlier))
    }

    @Test
    fun `WLD-T35 no first reveal`() {
        val t8 = key(350)
        var doc = WorldFixture.addTerm(stateA(), t8, "검은 등잔")
        doc = appendRun(doc, b12, link(key(351), t8, "검은 등잔"))
        doc = appendRun(doc, b42, link(key(352), t8, "검은 등잔"))
        doc = appendRun(doc, K.BLOCK_B61, link(key(353), t8, "검은 등잔"))
        val q = queries(doc)
        assertTrue(q.usage(t8).none(q::isBeforeReveal))
        val card = q.card(t8)!!
        assertTrue(card.notices.contains("첫 공개 없음 · 쓰인 곳 3"))
        val chip = card.chips.filterIsInstance<TermChip.SuggestReveal>().single()
        assertEquals("1-2 씬 1을 첫 공개로", chip.label)
        val row = q.checks().rows.filterIsInstance<TermCheckRow.NoReveal>().single { it.termKey == t8 }
        assertEquals("◇ 첫 공개 없음 · 검은 등잔 · 쓰인 곳 3", row.text)
        assertEquals(chip, row.chip)

        val applied = TermEdits.setReveal(doc, t8, chip.suggestion.target, ctx()) as RevealEdit.Applied
        assertEquals(Anchor.AtBlock(b12), applied.record.anchor)
        val after = queries(applied.doc)
        assertEquals(b12, after.reveal(t8)!!.blockKey)
        assertTrue(after.activeWarnings(t8).isEmpty())
    }

    @Test
    fun `WLD-T36 reveal planned beyond the writing line`() {
        val oath = K.TERM_EMBER_OATH
        var doc = stateA()
        for ((i, episode) in listOf(K.EPISODE_1_5, K.EPISODE_1_6, K.EPISODE_1_7).withIndex()) {
            doc = appendRun(doc, textBlockOf(doc, episode), link(key(360 + i), oath, "잿불의 서약"))
        }
        val q = queries(doc)
        assertEquals(3, q.activeWarnings(oath).size)
        val group = q.checks().rows.filterIsInstance<TermCheckRow.BeforeRevealGroup>().single { it.termKey == oath }
        assertEquals("잿불의 서약 · ▲ 3", group.title)
        assertEquals(3, group.rows.size)
        val list = q.list()
        val row = list.pending.single { it.termKey == oath }
        assertEquals("공개 예정 2-5", row.line1)
        assertTrue(list.revealed.none { it.termKey == oath })
    }

    @Test
    fun `WLD-T37 reveal block deleted and restored`() {
        val base = stateA()
        // The block goes to the trash; the anchor engine (SCR / domain:trash) relocates the record.
        // Built here by hand: the world module cannot call domain:trash.
        val trashed = WorldFixture.trashBlock(base, b7344)
        val target = trashed.manuscript.relocationTarget(Anchor.AtBlock(b7344))
        assertEquals(Anchor.AtBlock(K.BLOCK_045), target)
        val relocated = trashed.copy(changes = trashed.changes.copy(records = trashed.changes.records.map {
            if (it.target == ChangeTarget.TermReveal(T1)) {
                it.copy(anchor = target, anchorState = AnchorState.ORPHANED.open(), originalAnchor = Anchor.AtBlock(b7344))
            } else {
                it
            }
        }))
        val q = queries(relocated)
        val reveal = q.reveal(T1)!!
        assertEquals(K.BLOCK_045, reveal.blockKey)
        assertTrue(reveal.orphaned)
        assertEquals(Anchor.AtBlock(b7344), reveal.record.originalAnchor)
        assertEquals("쓰인 곳 · 1 · 후보 1", q.card(T1)!!.header)
        val b42Row = q.card(T1)!!.usage.rows.single()
        assertTrue(b42Row.markers.contains("▲ 공개(1-7) 전 사용"))
        assertTrue(b42Row.markers.contains("◇ 근거 지워짐"))
        val check = q.checks().rows.filterIsInstance<TermCheckRow.BeforeReveal>().single()
        assertEquals("◇ 근거 지워짐", check.marker)

        // Restore: the block is live again and the record re-attaches.
        val restored = relocated.copy(
            episodes = WorldFixture.withBlock(relocated, b7344) { it.copy(deletedAt = null) }.episodes,
            changes = relocated.changes.copy(records = relocated.changes.records.map {
                if (it.target == ChangeTarget.TermReveal(T1)) it.copy(anchor = Anchor.AtBlock(b7344), anchorState = AnchorState.NORMAL.open(), originalAnchor = null) else it
            }),
        )
        val back = queries(restored)
        assertFalse(back.reveal(T1)!!.orphaned)
        assertEquals("쓰인 곳 · 2 · 후보 1", back.card(T1)!!.header)
        assertEquals(listOf("◇ 첫 공개"), back.card(T1)!!.usage.rows[1].markers)
    }

    @Test
    fun `reveal block in the trash reads as orphaned before the record is relocated`() {
        val q = queries(WorldFixture.trashBlock(stateA(), b7344))
        val reveal = q.reveal(T1)!!
        assertTrue(reveal.orphaned)
        assertEquals(K.BLOCK_045, reveal.blockKey)
        assertEquals("1-7", reveal.episodeLabel)
    }

    @Test
    fun `WLD-T38 choice option line`() {
        val choice = K.BLOCK_1_5_CHOICE
        val doc = stateA()
        val options = doc.index.block(choice)!!.options
        val refused = TermEdits.setReveal(doc, T1, RevealTarget.OptionLine(choice, options[0].key), ctx())
        assertEquals(RevealEdit.Refused("선택지 보기 줄에는 첫 공개를 걸 수 없음"), refused)

        val linked = WorldFixture.withBlock(doc, choice) { b ->
            b.copy(options = b.options.map { o -> if (o.key == options[1].key) o.copy(runs = o.runs + TextRun.Plain(" ") + link(key(381), T1, "오리지늄")) else o })
        }
        val q = queries(linked)
        val l = q.usage(T1).single { it.linkKey == key(381) }
        assertEquals(ScriptSpot.OptionText(choice, options[1].key), l.spot)
        assertTrue(q.isBeforeReveal(l))
        assertEquals(listOf("선택지"), q.card(T1)!!.usage.rows.single { it.link.linkKey == key(381) }.tags)
    }

    @Test
    fun `WLD-T39 moving the reveal block`() {
        val l47 = key(391)
        val before = appendRun(stateA(), K.BLOCK_047, link(l47, T1, "오리지늄 등불"))
        val qBefore = queries(before)
        assertFalse(qBefore.isBeforeReveal(qBefore.usage(T1).single { it.linkKey == l47 }))
        val after = WorldFixture.moveBlockToSceneStart(before, b7344, K.SCENE_1_8_1)
        val qAfter = queries(after)
        assertFalse(qAfter.isBeforeReveal(qAfter.usage(T1).single { it.linkKey == K.LINK_L44 }))
        assertTrue(qAfter.isBeforeReveal(qAfter.usage(T1).single { it.linkKey == l47 }))
        assertEquals("◇ 첫 공개 1-7 → 1-8 · ▲ 새로 1", RevealMoves.toast(qBefore, qAfter, T1))
    }

    @Test
    fun `WLD-T40 lens dims rows and counts stay`() {
        val q = queries(stateA())
        val lens = LensPoint.UnitEnd(K.EPISODE_1_5)
        val card = q.card(T1, lens)!!
        assertEquals("쓰인 곳 · 2 · 후보 1", card.header)
        assertFalse(card.usage.rows.single { it.link.linkKey == K.LINK_L42 }.dimmed)
        assertTrue(card.usage.rows.single { it.link.linkKey == K.LINK_L44 }.dimmed)
        assertFalse(card.candidates.rows.single().dimmed)
        val list = q.list(lens)
        assertEquals("공개됨 · 1-5", list.revealedTitle)
        assertTrue(list.revealed.none { it.termKey == T1 })
        assertTrue(list.pending.single { it.termKey == T1 }.line1.startsWith("첫 공개 1-7"))
    }

    @Test
    fun `WLD-T41 intended use lifecycle`() {
        val doc = stateA()
        val l42 = queries(doc).usage(T1).single { it.linkKey == K.LINK_L42 }
        val marked = TermEdits.markIntended(doc, l42, ctx())
        val moved = (TermEdits.setReveal(marked, T1, RevealTarget.Unit(K.EPISODE_1_8), ctx()) as RevealEdit.Applied).doc
        val qMoved = queries(moved)
        assertEquals("1-8", qMoved.reveal(T1)!!.episodeLabel)
        assertTrue(qMoved.isIntended(qMoved.usage(T1).single { it.linkKey == K.LINK_L42 }))
        assertEquals(1, moved.checks.acknowledgements.count { it.subjectKeys == listOf(K.LINK_L42) })

        val unlinked = TermEdits.unlink(moved, K.LINK_L42)
        assertTrue(unlinked.checks.acknowledgements.none { it.subjectKeys == listOf(K.LINK_L42) })
        assertEquals("오리지늄은 타지 않아. 빛만 남지.", unlinked.index.block(b42)!!.runs.joinToString("") { (it as TextRun.Plain).text })
        val (index, _) = CandidateIndex.build(moved).rescanSpots(unlinked, listOf(ScriptSpot.BlockText(b42)))
        val candidate = TermQueries(unlinked, index).candidates(T1).single { it.spot == ScriptSpot.BlockText(b42) }
        assertEquals("오리지늄", candidate.matchedText)
    }

    @Test
    fun `WLD-T42 particle of the suggestion chip`() {
        assertEquals("1-2 씬 1을 첫 공개로", Josa.withObject("1-2 씬 1") + " 첫 공개로")
        assertEquals("1-2 씬 2를 첫 공개로", Josa.withObject("1-2 씬 2") + " 첫 공개로")
        assertEquals("1-6 씬 4를 첫 공개로", Josa.withObject("1-6 씬 4") + " 첫 공개로")
        for (d in "036781") assertEquals("을", Josa.objectParticle("씬 $d"))
        for (d in "2459") assertEquals("를", Josa.objectParticle("씬 $d"))
        // Through the card: a term whose first link sits in 1-6 씬 4.
        val t = key(420)
        val doc = appendRun(WorldFixture.addTerm(stateA(), t, "장부 도장"), K.BLOCK_1_6_025, link(key(421), t, "장부 도장"))
        val chip = queries(doc).card(t)!!.chips.filterIsInstance<TermChip.SuggestReveal>().single()
        assertEquals("1-6 씬 4를 첫 공개로", chip.label)
        assertNotNull(chip.suggestion)
        assertNull(queries(doc).reveal(t))
    }
}
