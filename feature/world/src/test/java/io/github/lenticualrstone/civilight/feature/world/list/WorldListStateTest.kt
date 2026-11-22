package io.github.lenticualrstone.civilight.feature.world.list

import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermListRow
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.navigation.WorldTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 세계 list (WLD-01 … WLD-04, WLD-12) as the screen draws it. */
class WorldListStateTest {
    private fun terms(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc, hiddenOpen: Boolean = false) =
        WorldListState.build(doc, WorldTab.TERMS, WorldTestFixture.index(doc, hiddenOpen), hiddenOpen)

    private fun rows(ui: WorldListUi) = ui.items.filterIsInstance<WorldListItem.Row>()

    private fun sections(ui: WorldListUi) = ui.items.filterIsInstance<WorldListItem.Section>().map { it.title }

    @Test
    fun `WLD-T02 list row in state A`() {
        val ui = terms(WorldTestFixture.stateA())
        assertEquals("잿빛 새벽의 등불", ui.projectName)
        assertEquals(listOf("공개됨 · 최신 · 1-7", "아직 공개 전"), sections(ui))
        val row = rows(ui).single { it.key == T1 }
        assertEquals("오리지늄 등불", row.title)
        val description = io.github.lenticualrstone.civilight.feature.world.common.RichTextEdit.textOf(WorldTestFixture.stateA().index.term(T1)!!.description)
        assertEquals(description.lineSequence().first(), row.fragment)
        assertEquals("첫 공개 1-7", row.right)
        assertEquals("◇ 후보 1", row.candidates)
        assertEquals("▲ 앞에서 1회 사용", row.warning)
        assertFalse(ui.candidatesPending)
    }

    @Test
    fun `WLD-02 sample - revealed and planned terms at 최신 · 1-7`() {
        val ui = terms(WorldTestFixture.stateA())
        val items = ui.items
        val pendingAt = items.indexOfFirst { it is WorldListItem.Section && it.title == "아직 공개 전" }
        val revealed = items.subList(0, pendingAt).filterIsInstance<WorldListItem.Row>().map { it.title }
        val pending = items.subList(pendingAt, items.size).filterIsInstance<WorldListItem.Row>()
        assertEquals(setOf("오리지늄 등불", "대정전", "등불 결사"), revealed.toSet())
        // By reveal point (WLD-02): 2장's start comes before 2-5 (the spec's sample lists them the other way).
        assertEquals(listOf("상층 송전망", "잿불의 서약"), pending.map { it.title })
        assertEquals(listOf("공개 예정 2장", "공개 예정 2-5"), pending.map { it.right })
        assertEquals("첫 공개 1-4", rows(ui).single { it.key == K.TERM_BLACKOUT }.right)
        assertEquals("첫 공개 1-1", rows(ui).single { it.key == K.TERM_LANTERN_SOCIETY }.right)
        assertNull(rows(ui).single { it.key == K.TERM_BLACKOUT }.warning)
    }

    @Test
    fun `WLD-T40 the list follows a past lens`() {
        val doc = WorldTestFixture.atLens(WorldTestFixture.stateA(), K.episode(1, 5))
        val ui = terms(doc)
        val items = ui.items
        val pendingAt = items.indexOfFirst { it is WorldListItem.Section && it.title == "아직 공개 전" }
        val pending = items.subList(pendingAt, items.size).filterIsInstance<WorldListItem.Row>()
        val t1 = pending.single { it.key == T1 }
        assertEquals("첫 공개 1-7", t1.right)
        assertEquals("공개됨 · 1-5", sections(ui).first())
    }

    @Test
    fun `a pending index dims the candidate numbers`() {
        val doc = WorldTestFixture.stateA()
        val ui = WorldListState.build(doc, WorldTab.TERMS, CandidateIndex.pending(), hiddenOpen = false)
        assertTrue(ui.candidatesPending)
    }

    @Test
    fun `termRow splits the candidate part off the first line`() {
        val row = WorldListState.termRow(TermListRow(T1, "오리지늄 등불", "빛", "첫 공개 1-7 · ◇ 후보 1", "▲ 앞에서 1회 사용"))
        assertEquals("첫 공개 1-7", row.right)
        assertEquals("◇ 후보 1", row.candidates)
        assertEquals("▲ 앞에서 1회 사용", row.warning)
        val plain = WorldListState.termRow(TermListRow(T1, "잿불의 서약", "", "공개 예정 2-5", null))
        assertEquals("공개 예정 2-5", plain.right)
        assertNull(plain.candidates)
        assertEquals(WorldKind.TERM, plain.kind)
    }

    @Test
    fun `WLD-13 hidden terms collapse into one lock row while locked`() {
        val doc = WorldTestFixture.withTerm(WorldTestFixture.stateA(), K.TERM_EMBER_OATH) { it.copy(hidden = true) }
        val locked = terms(doc)
        assertEquals("숨긴 용어 1", (locked.items.first() as WorldListItem.Lock).text)
        assertTrue(rows(locked).none { it.key == K.TERM_EMBER_OATH })
        val open = terms(doc, hiddenOpen = true)
        assertTrue(open.items.none { it is WorldListItem.Lock })
        assertTrue(rows(open).any { it.key == K.TERM_EMBER_OATH })
    }

    @Test
    fun `WLD-02 empty state - 용어 없음 and the create button`() {
        val base = WorldTestFixture.stateA()
        val doc = base.copy(world = base.world.copy(terms = emptyList()))
        val ui = terms(doc)
        val empty = ui.items.single() as WorldListItem.Empty
        assertEquals("용어 없음", empty.line)
        assertEquals("용어 만들기", empty.button)
    }

    @Test
    fun `WLD-03 places - rows with 등장 and 지역 sections`() {
        val doc = WorldTestFixture.stateB()
        val ui = WorldListState.build(doc, WorldTab.PLACES, CandidateIndex.pending(), hiddenOpen = false)
        val rows = rows(ui)
        assertEquals(doc.world.places.count { it.deletedAt == null }, rows.size)
        assertTrue(rows.all { it.kind == WorldKind.PLACE && it.right.startsWith("등장") })
        val archive = rows.single { it.key == K.PLACE_ARCHIVE_ROOM }
        assertEquals("기록실", archive.title)
        // 기록실 lies under its 지역 결사의 문 (a section of that name precedes it).
        val archiveAt = ui.items.indexOf(archive)
        val section = ui.items.subList(0, archiveAt).filterIsInstance<WorldListItem.Section>().last()
        assertEquals("결사의 문", section.title)
    }

    @Test
    fun `WLD-04 factions - one row each`() {
        val doc = WorldTestFixture.stateB()
        val ui = WorldListState.build(doc, WorldTab.FACTIONS, CandidateIndex.pending(), hiddenOpen = false)
        assertEquals(listOf("등불 결사", "상층 경비대"), rows(ui).map { it.title })
        assertTrue(rows(ui).all { it.kind == WorldKind.FACTION })
    }

    @Test
    fun `WLD-01 tabs - a route tab wins, else the remembered one, else 용어`() {
        assertEquals(WorldTab.PLACES, WorldTabs.initial(WorldTab.PLACES, "FACTIONS"))
        assertEquals(WorldTab.FACTIONS, WorldTabs.initial(WorldTab.TERMS, "FACTIONS"))
        assertEquals(WorldTab.TERMS, WorldTabs.initial(WorldTab.TERMS, null))
        assertEquals(WorldTab.TERMS, WorldTabs.initial(WorldTab.TERMS, "SOMETHING_ELSE"))
        assertEquals(listOf("장소", "용어", "세력", "지도"), WorldSegment.labels)
        assertNull(WorldSegment.MAPS.tab)
        assertEquals(WorldKind.FACTION, WorldTabs.kindOf(WorldTab.FACTIONS))
    }
}
