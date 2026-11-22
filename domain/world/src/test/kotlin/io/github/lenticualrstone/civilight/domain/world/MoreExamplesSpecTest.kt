package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.addBlockAfter
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.key
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.plain
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateA
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.CandidateKind
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TermCheckRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD §4.8 rule-21 worked examples (WLD-T66 … WLD-T68). Rule-20 cases are in [DeleteMergeSpecTest]. */
class MoreExamplesSpecTest {
    @Test
    fun `WLD-T66 a post-reveal candidate stays on the card`() {
        // b7346: a narration after #045 in 1-7 씬 3 (the fixture's #046 is a memo, so a new block stands in).
        val b7346 = key(660)
        val doc = addBlockAfter(stateA(), K.BLOCK_045, b7346, listOf(plain("꺼진 오리지늄은 다시 빛나지 않았다.")), number = 900)
        val q = queries(doc)
        val card = q.card(T1)!!
        assertEquals("쓰인 곳 · 2 · 후보 2", card.header)
        val candidates = q.candidates(T1)
        assertEquals(listOf(ScriptSpot.BlockText(K.BLOCK_B12), ScriptSpot.BlockText(b7346)), candidates.map { it.spot })
        assertEquals(CandidateKind.CUT_OFF, candidates[0].kind)
        val post = candidates[1]
        assertEquals(CandidateKind.FULL, post.kind)
        assertEquals("오리지늄", post.matchedText)
        assertEquals(3 to 7, post.start to post.end)
        assertEquals("첫 공개 1-7 · ◇ 후보 1", q.list().revealed.single { it.termKey == T1 }.line1)
        val rows = q.checks().rows
        assertEquals(2, rows.size)
        assertTrue(rows[0] is TermCheckRow.BeforeReveal)
        assertEquals(ScriptSpot.BlockText(K.BLOCK_B12), (rows[1] as TermCheckRow.CandidateBeforeReveal).candidate.spot)
    }

    private fun scanFor(term: String, text: String, termKey: String = key(670)) =
        CandidateIndex.build(addBlockAfter(WorldFixture.addTerm(stateA(), termKey, term), K.BLOCK_044, key(671), listOf(plain(text))))
            .at(ScriptSpot.BlockText(key(671))).filter { it.termKey == termKey }

    @Test
    fun `WLD-T67 no free continuation`() {
        assertTrue(scanFor("등불", "등불지기는 밤에 일한다.").isEmpty())
        assertEquals(1, scanFor("등불", "등불은 밤에 탄다.").size)
    }

    @Test
    fun `WLD-T68 other cut-off marks`() {
        for (text in listOf("오리지⋯ 그거", "오리지— 그거", "오리지- 그거")) {
            val c = CandidateIndex.build(addBlockAfter(stateA(), K.BLOCK_044, key(681), listOf(plain(text))))
                .at(ScriptSpot.BlockText(key(681))).single { it.termKey == T1 }
            assertEquals(text, CandidateKind.CUT_OFF, c.kind)
            assertEquals(text, "오리지", c.matchedText)
        }
    }
}
