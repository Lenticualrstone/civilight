package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.addBlockAfter
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.key
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.plain
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateA
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.CandidateKind
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermChip
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.RejectionContainerKind
import io.github.lenticualrstone.civilight.model.world.TermRejection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

/** WLD §4.3 matcher cases (WLD-T15 … WLD-T32): T1 with alias `오리지늄`, each text a narration after #044. */
class MatcherSpecTest {
    private val probe = key(1)
    private val T7 = key(7)

    private fun docWith(text: String, base: ProjectDoc = stateA(), runs: List<TextRun> = listOf(plain(text))): ProjectDoc =
        addBlockAfter(base, K.BLOCK_044, probe, runs)

    private fun scan(text: String, base: ProjectDoc = stateA(), term: Key = T1): List<TermCandidate> =
        CandidateIndex.build(docWith(text, base)).at(ScriptSpot.BlockText(probe)).filter { it.termKey == term }

    private fun assertOneFull(text: String, matched: String, start: Int? = null, end: Int? = null) {
        val c = scan(text).single()
        assertEquals(CandidateKind.FULL, c.kind)
        assertEquals(matched, c.matchedText)
        if (start != null) assertEquals(start to end, c.start to c.end)
    }

    @Test
    fun `WLD-T15 particle 은`() = assertOneFull("오리지늄은 타지 않아.", "오리지늄", 0, 4)

    @Test
    fun `WLD-T16 particles 을 and 이`() {
        assertOneFull("오리지늄을 쥐었다.", "오리지늄")
        assertOneFull("오리지늄이 빛났다.", "오리지늄")
    }

    @Test
    fun `WLD-T17 name without its space wins over the alias`() = assertOneFull("꺼진 오리지늄등불이 깜박였다.", "오리지늄등불", 3, 9)

    @Test
    fun `WLD-T18 name with its space`() = assertOneFull("꺼진 오리지늄 등불이 깜박였다.", "오리지늄 등불", 3, 10)

    @Test
    fun `WLD-T19 not at word start`() = assertTrue(scan("하층오리지늄은 비싸다.").isEmpty())

    @Test
    fun `WLD-T20 other spellings and free continuations`() {
        assertTrue(scan("오리지니움은").isEmpty())
        assertTrue(scan("오리지늄스러운 빛").isEmpty())
    }

    @Test
    fun `WLD-T21 quote and bracket boundaries`() {
        assertOneFull("“오리지늄은 타지 않아.”", "오리지늄", 1, 5)
        assertOneFull("(오리지늄)", "오리지늄", 1, 5)
    }

    @Test
    fun `WLD-T22 two chained particles`() {
        assertOneFull("오리지늄에서는", "오리지늄")
        assertOneFull("오리지늄으로도", "오리지늄")
        assertOneFull("오리지늄이라는", "오리지늄")
    }

    @Test
    fun `WLD-T23 one-syllable cut-off is not searched`() = assertTrue(scan("오… 그거?").isEmpty())

    @Test
    fun `WLD-T24 particle 의 then a space`() = assertOneFull("오리지늄의 빛.", "오리지늄")

    @Test
    fun `WLD-T25 decomposed text matches and stores NFC`() {
        val nfd = Normalizer.normalize("오리지늄은", Normalizer.Form.NFD)
        val c = scan(nfd).single()
        assertEquals(CandidateKind.FULL, c.kind)
        assertEquals("오리지늄", c.matchedText)
        assertTrue(Normalizer.isNormalized(c.matchedText, Normalizer.Form.NFC))
        assertEquals(0 to Normalizer.normalize("오리지늄", Normalizer.Form.NFD).length, c.start to c.end)
    }

    @Test
    fun `WLD-T26 one-syllable term`() {
        val light = key(26)
        var doc = WorldFixture.addTerm(stateA(), light, "빛")
        doc = docWith("빛은 꺼졌다.", doc)
        doc = addBlockAfter(doc, probe, key(27), listOf(WorldFixture.link(key(28), light, "빛"), plain("이 남았다.")), number = 901)
        val q = queries(doc)
        assertTrue(q.index.at(ScriptSpot.BlockText(probe)).none { it.termKey == light })
        val card = q.card(light)!!
        assertTrue(card.notices.contains("1음절 · 글자 찾기 안 함"))
        assertEquals(1, card.usageCount)
    }

    @Test
    fun `WLD-T27 overlap - the longest wins`() {
        val base = WorldFixture.addTerm(stateA(), T7, "등불")
        val first = CandidateIndex.build(docWith("꺼진 오리지늄 등불이", base)).at(ScriptSpot.BlockText(probe))
        assertEquals(listOf(T1 to (3 to 10)), first.map { it.termKey to (it.start to it.end) })
        val second = CandidateIndex.build(docWith("등불을 들고 나갔다.", base)).at(ScriptSpot.BlockText(probe))
        assertEquals(listOf(T7 to (0 to 2)), second.map { it.termKey to (it.start to it.end) })
    }

    @Test
    fun `WLD-T28 homonym threshold`() {
        fun rejection(i: Int) = TermRejection(
            key = key(100 + i), containerKind = RejectionContainerKind.BLOCK.open(), containerKey = key(200 + i),
            matchedText = "등불", occurrence = 0,
        )
        val base = WorldFixture.addTerm(stateA(), T7, "등불")
        val ten = WorldFixture.withTerm(base, T7) { it.copy(rejections = (1..10).map(::rejection)) }
        assertTrue(queries(ten).card(T7)!!.chips.none { it is TermChip.TurnOffSearch })
        val eleven = WorldFixture.withTerm(ten, T7) { it.copy(rejections = it.rejections + rejection(11)) }
        val card = queries(eleven).card(T7)!!
        assertEquals("글자 찾기 끄기", card.chips.filterIsInstance<TermChip.TurnOffSearch>().single().label)
        assertTrue(eleven.index.term(T7)!!.textSearch)
    }

    @Test
    fun `WLD-T29 text kinds`() {
        var doc = stateA()
        doc = addBlockAfter(doc, K.BLOCK_044, key(31), listOf(plain("⌁ 오리지늄은 나중에")), kind = BlockKind.MEMO, number = 901)
        doc = addBlockAfter(doc, key(31), key(32), listOf(plain("오리지늄을 비춘다")), kind = BlockKind.DIRECTION, number = 902)
        doc = addBlockAfter(doc, key(32), key(33), emptyList(), kind = BlockKind.CHOICE, number = 903)
        doc = WorldFixture.withBlock(doc, key(33)) {
            it.copy(options = listOf(
                ChoiceOption(key(34), OrderKeys.FIRST, listOf(plain("오리지늄을 건넨다"))),
                ChoiceOption(key(35), OrderKeys.after(OrderKeys.FIRST), listOf(plain("돌아선다"))),
            ))
        }
        doc = addBlockAfter(
            doc, key(33), key(36), listOf(TextRun.Paren("오리지늄을 보며"), plain("가자.")),
            kind = BlockKind.DIALOGUE, number = 904, speakerKey = K.RIEN,
        )
        // A speaker whose name is the term's alias: names are never scanned.
        doc = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.OWEN) it.copy(name = "오리지늄") else it }))
        // Page and character text.
        doc = doc.copy(pages = doc.pages.mapValues { (_, p) ->
            p.copy(blocks = p.blocks + DocBlock(key(37), "z", runs = listOf(plain("오리지늄은 페이지에만"))))
        })
        val q = queries(doc)
        val spots = q.candidates(T1).map { it.spot }
        assertTrue(ScriptSpot.BlockText(key(31)) !in spots)
        assertTrue(ScriptSpot.BlockText(key(32)) !in spots)
        assertTrue(ScriptSpot.BlockText(key(36)) !in spots)
        val option = q.candidates(T1).single { it.spot is ScriptSpot.OptionText }
        assertEquals(ScriptSpot.OptionText(key(33), key(34)), option.spot)
        assertEquals(listOf("선택지"), q.card(T1)!!.candidates.rows.single { it.candidate == option }.tags)
        // Only b12 (cut-off) and the option line are candidates.
        assertEquals(2, q.candidates(T1).size)
    }

    @Test
    fun `WLD-T30 hidden term while locked`() {
        val doc = WorldFixture.withTerm(stateA(), T1) { it.copy(hidden = true) }
        val locked = TermAccess(hiddenUnlocked = false)
        val index = CandidateIndex.build(doc, locked)
        assertTrue(index.of(T1).isEmpty())
        val q = TermQueries(doc, index, locked)
        assertNull(q.card(T1)!!.candidateCount)
        assertEquals("숨긴 용어 1", q.list().lockRow)
        val unlocked = TermAccess(hiddenUnlocked = true)
        val open = TermQueries(doc, CandidateIndex.build(doc, unlocked), unlocked)
        assertEquals("쓰인 곳 · 2 · 후보 1", open.card(T1)!!.header)
        assertEquals("오리지", open.candidates(T1).single().matchedText)
    }

    @Test
    fun `WLD-T31 text search off and on`() {
        val doc = stateA()
        val rejected = TermEdits.reject(doc, queries(doc).candidates(T1).single(), ctx())
        val off = TermEdits.setTextSearch(rejected, T1, false, ctx())
        val (offIndex, _) = WorldFixture.index(rejected).rescanTerm(off, T1)
        val offReleased = TermEdits.releaseStaleRejections(off, offIndex)
        val q = TermQueries(offReleased, offIndex)
        assertTrue(q.candidates(T1).isEmpty())
        assertTrue(q.card(T1)!!.notices.contains("글자 찾기 꺼짐 · 켜기"))
        assertEquals(1, offReleased.index.term(T1)!!.rejections.size)
        val on = TermEdits.setTextSearch(offReleased, T1, true, ctx())
        val (onIndex, _) = offIndex.rescanTerm(on, T1)
        val onReleased = TermEdits.releaseStaleRejections(on, onIndex)
        assertEquals(1, onIndex.of(T1).size)
        assertTrue(TermQueries(onReleased, onIndex).candidates(T1).isEmpty())
        assertEquals(1, onReleased.index.term(T1)!!.rejections.size)
    }

    @Test
    fun `WLD-T32 pending defaults`() {
        assertTrue(scan("오리지늄이었다").isEmpty())
        assertOneFull("오리지늄…", "오리지늄")
        assertTrue(scan("오리지...").isEmpty())
    }
}
