package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.term.CandidateKind
import io.github.lenticualrstone.civilight.domain.world.term.TermChip
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.FixedRowUi
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The term card (WLD-08, WLD-17 … WLD-19) as the screen draws it. */
class TermCardStateTest {
    private fun card(doc: ProjectDoc, termKey: String = T1, hiddenOpen: Boolean = false, readOnly: Boolean = false) =
        TermCardState.build(doc, PROJECT, termKey, WorldTestFixture.index(doc, hiddenOpen), hiddenOpen, readOnly)

    private fun UsageRowUi.boxedText() = text.substring(boxStart, boxEnd)

    @Test
    fun `WLD-T01 card in state A`() {
        val ui = card(WorldTestFixture.stateA())
        assertEquals(CardAccess.Open, ui.access)
        assertEquals("오리지늄 등불", ui.title)
        assertEquals("용어", ui.head!!.eyebrow)
        assertEquals("쓰인 곳 · 2 · 후보 1", ui.header)
        assertEquals(listOf("usage", "candidates", "outside"), ui.groups.map { it.id })
        assertEquals(listOf("쓰인 곳 · 2", "후보 · 1 · 확인 안 함", "원고 밖 · 메모 · 1"), ui.groups.map { it.title })

        val (b42, b7344) = ui.groups[0].rows
        assertEquals("1-4 씬 2", b42.location)
        assertTrue("dialogue is quoted", b42.text.startsWith("“") && b42.text.endsWith("”"))
        assertEquals("오리지늄", b42.boxedText())
        assertEquals(listOf(MarkerUi("▲ 공개(1-7) 전 사용", warn = true)), b42.markers)
        assertEquals(Script(PROJECT, blockKey = K.BLOCK_B42), b42.target)
        assertEquals("1-7 씬 3", b7344.location)
        assertEquals("오리지늄 등불", b7344.boxedText())
        assertEquals(listOf(MarkerUi("◇ 첫 공개", warn = false)), b7344.markers)

        val candidate = ui.groups[1].rows.single()
        assertEquals("1-2 씬 1", candidate.location)
        assertEquals("오리지", candidate.boxedText())
        assertEquals("◇ 후보 · 끊김", candidate.markers.single().text)
        assertEquals(CandidateKind.CUT_OFF, candidate.candidate!!.kind)

        val page = ui.groups[2].rows.single()
        assertEquals("", page.location)
        assertEquals("페이지 · 하층 구역의 빛", page.placeLine)
        assertEquals("오리지늄 등불", page.boxedText())
        assertTrue(page.markers.none { it.warn })
        assertEquals(PageDoc(PROJECT, K.PAGE_LOWER_LIGHT), page.target)
        assertNull(page.candidate)
    }

    @Test
    fun `WLD-08 fixed rows - 첫 공개, 별칭, 관련`() {
        val ui = card(WorldTestFixture.stateB())
        assertEquals(FixedRowUi("첫 공개", "1-7 씬 3 · #044", editable = true), ui.reveal)
        assertEquals("오리지늄 · 빛돌", ui.aliases!!.value)
        assertEquals("등불 결사 · 대정전", ui.related!!.value)
        assertTrue(ui.related!!.editable)
        val readOnly = card(WorldTestFixture.stateB(), readOnly = true)
        assertFalse(readOnly.reveal!!.editable)
        assertFalse(readOnly.related!!.editable)
    }

    @Test
    fun `WLD-T10 state B finds the 빛돌 candidates too`() {
        val ui = card(WorldTestFixture.stateB())
        assertEquals("쓰인 곳 · 2 · 후보 3", ui.header)
        val candidates = ui.groups.single { it.id == "candidates" }.rows
        assertEquals(listOf("1-2 씬 1", "1-3 씬 2", "1-6 씬 1"), candidates.map { it.location })
        assertEquals(listOf("오리지", "빛돌", "빛돌"), candidates.map { it.boxedText() })
    }

    @Test
    fun `WLD-T40 lens dims rows after it, counts stay`() {
        val doc = WorldTestFixture.atLens(WorldTestFixture.stateA(), K.episode(1, 5))
        val ui = card(doc)
        assertEquals("쓰인 곳 · 2 · 후보 1", ui.header)
        val (b42, b7344) = ui.groups[0].rows
        assertFalse(b42.dimmed)
        assertTrue(b7344.dimmed)
        assertFalse(ui.groups[1].rows.single().dimmed)
    }

    @Test
    fun `WLD-T35 superseded - a term with links reads its automatic reveal, no notice and no chip`() {
        val ctx = WorldTestFixture.ctx()
        val created = WorldItems.createTerm(WorldTestFixture.stateA(), "검은 등잔", ctx) as CreateResult.Created
        val t8 = created.key
        var doc = WorldTestFixture.appendRun(created.doc, K.BLOCK_B12, WorldTestFixture.link(ctx.newKey(), t8, "검은 등잔"))
        doc = WorldTestFixture.appendRun(doc, K.BLOCK_B42, WorldTestFixture.link(ctx.newKey(), t8, "검은 등잔"))
        val ui = card(doc, t8)
        // (0.2.0, WLD-72) The first link (1-2 씬 1) is the automatic reveal.
        assertTrue(ui.reveal!!.value.startsWith("1-2 씬 1 · #"))
        assertTrue(ui.notices.none { it.startsWith("첫 공개 없음") })
        assertTrue(ui.chips.none { it is TermChip.SuggestReveal })
        assertTrue(ui.groups[0].rows.all { row -> row.markers.none { it.warn } })
    }

    @Test
    fun `WLD-T26 one-syllable term - notice, links still counted`() {
        val ctx = WorldTestFixture.ctx()
        val created = WorldItems.createTerm(WorldTestFixture.stateA(), "빛", ctx) as CreateResult.Created
        val doc = WorldTestFixture.appendRun(created.doc, K.BLOCK_B42, WorldTestFixture.link(ctx.newKey(), created.key, "빛"))
        val ui = card(doc, created.key)
        assertTrue(ui.notices.contains("1음절 · 글자 찾기 안 함"))
        assertEquals("쓰인 곳 · 1", ui.header)
    }

    @Test
    fun `WLD-25 search off shows 글자 찾기 꺼짐 with 켜기 and no candidates`() {
        val doc = TermEdits.setTextSearch(WorldTestFixture.stateA(), T1, false, WorldTestFixture.ctx())
        val ui = card(doc)
        assertTrue(ui.searchOff)
        assertTrue(ui.notices.none { it.startsWith("글자 찾기 꺼짐") })
        assertTrue(ui.groups.none { it.id == "candidates" })
    }

    @Test
    fun `WLD-13 a hidden term is one lock row while locked`() {
        val doc = WorldTestFixture.withTerm(WorldTestFixture.stateA(), T1) { it.copy(hidden = true) }
        val locked = card(doc)
        assertEquals(CardAccess.Locked("숨긴 용어 1"), locked.access)
        assertEquals("숨긴 항목", locked.title)
        assertNull(locked.head)
        assertTrue(locked.groups.isEmpty())
        assertEquals(CardAccess.Open, card(doc, hiddenOpen = true).access)
    }

    @Test
    fun `WLD-14 a trashed term shows its dotted label only`() {
        val doc = WorldTestFixture.withTerm(WorldTestFixture.stateA(), T1) { it.copy(deletedAt = 1L, trashEntryKey = "E") }
        val ui = card(doc)
        val gone = ui.access as CardAccess.Gone
        assertTrue(gone.dotted)
        assertEquals("지운 용어 · 오리지늄 등불", gone.text)
        assertEquals(gone.text, ui.title)
    }

    @Test
    fun `candidate marker drops the two action words`() {
        assertEquals("◇ 후보 · 끊김", TermCardState.candidateMarker("◇ 후보 · 끊김 · 잇기 · 아님"))
        assertEquals("◇ 후보", TermCardState.candidateMarker("◇ 후보 · 잇기 · 아님"))
    }

    @Test
    fun `WLD-T04 잇기 on the card candidate makes it usage`() {
        val doc = WorldTestFixture.stateA()
        val candidate = card(doc).groups[1].rows.single().candidate!!
        val linked = TermEdits.link(doc, candidate, WorldTestFixture.ctx())
        assertNotNull(linked)
        val ui = card(linked!!.doc)
        assertEquals("쓰인 곳 · 3", ui.header)
        assertTrue(ui.groups.none { it.id == "candidates" })
    }

    @Test
    fun `WLD-T06 아님 on the card candidate removes it`() {
        val doc = WorldTestFixture.stateA()
        val candidate = card(doc).groups[1].rows.single().candidate!!
        val ui = card(TermEdits.reject(doc, candidate, WorldTestFixture.ctx()))
        assertEquals("쓰인 곳 · 2", ui.header)
        assertTrue(ui.groups.none { it.id == "candidates" })
    }
}
