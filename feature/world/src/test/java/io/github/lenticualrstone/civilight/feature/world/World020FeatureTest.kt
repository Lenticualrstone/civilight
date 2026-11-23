package io.github.lenticualrstone.civilight.feature.world

import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.time.TimelineView
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateCards
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateDraft
import io.github.lenticualrstone.civilight.feature.world.list.WorldCardRows
import io.github.lenticualrstone.civilight.feature.world.list.WorldListItem
import io.github.lenticualrstone.civilight.feature.world.list.WorldListState
import io.github.lenticualrstone.civilight.feature.world.place.PlaceCardState
import io.github.lenticualrstone.civilight.feature.world.term.TermCardState
import io.github.lenticualrstone.civilight.feature.world.time.TimelineState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.navigation.WorldTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 0.2.0 world screens (U-15, S-1, S-5, G-2): WLD-T79, T82, T84, T86, T87 state models and the icon lint. */
class World020FeatureTest {
    private fun candidatesOff(doc: ProjectDoc) = doc.copy(project = doc.project.copy(termCandidates = false))
    private fun gameToolsOff(doc: ProjectDoc) = doc.copy(project = doc.project.copy(gameTools = false))

    // --- U-15 create card -------------------------------------------------------------------------

    @Test
    fun `U-15_7 field sets of the world cards, hidden fields left out`() {
        fun labels(kind: CreateKind) = WorldCreateCards.fields(kind).map { it.label }
        assertEquals(listOf("이름", "상위 지역", "한 줄 설명"), labels(CreateKind.PLACE))
        assertEquals(listOf("이름", "다른 이름", "한 줄 설명"), labels(CreateKind.TERM))
        assertEquals(listOf("이름", "한 줄 설명"), labels(CreateKind.FACTION))
        assertEquals(listOf("이름", "때"), labels(CreateKind.EVENT))
        assertEquals("새 장소", WorldCreateCards.title(WorldCreateDraft.of(WorldKind.PLACE)))
        assertEquals("새 용어", WorldCreateCards.title(WorldCreateDraft.of(WorldKind.TERM)))
        assertEquals("새 세력", WorldCreateCards.title(WorldCreateDraft.of(WorldKind.FACTION)))
        assertEquals("새 사건", WorldCreateCards.title(WorldCreateDraft.event()))
    }

    @Test
    fun `U-15_5 only the name is required`() {
        val doc = WorldTestFixture.stateA()
        val blank = WorldCreateDraft.of(WorldKind.TERM)
        assertFalse(WorldCreateCards.canCreate(doc, blank))
        assertFalse(WorldCreateCards.canCreate(doc, blank.copy(name = "   ", alias = "반디")))
        assertTrue(WorldCreateCards.canCreate(doc, blank.copy(name = "반딧돌")))
        assertFalse(blank.hasContent)
        assertTrue(blank.copy(note = "x").hasContent)
    }

    @Test
    fun `WLD-T82 the card writes name, alias and one-line description`() {
        val draft = WorldCreateDraft.of(WorldKind.TERM).copy(name = "반딧돌", alias = "반디, 개똥", note = "작은 빛을 내는 돌", parentKey = "x")
        val item = WorldCreateCards.item(draft)
        assertEquals("반딧돌", item.name)
        assertEquals("반디, 개똥", item.alias)
        assertEquals("작은 빛을 내는 돌", item.description)
        assertNull("a term has no parent", item.parentPlaceKey)
        val place = WorldCreateCards.item(WorldCreateDraft.of(WorldKind.PLACE).copy(name = "은빛 항구", alias = "x", parentKey = "p"))
        assertEquals("p", place.parentPlaceKey)
        assertEquals("", place.alias)
    }

    @Test
    fun `WLD-T84 때 decides the event kind`() {
        val doc = WorldTestFixture.stateA()
        val base = WorldCreateDraft.event().copy(name = "결사 창립")
        val world = WorldCreateCards.eventDraft(WorldCreateCards.withYear(base, "1190"))
        assertFalse(world.story)
        assertEquals("1190", world.year)
        val scene = doc.manuscript.liveScenes.first().key
        val story = WorldCreateCards.eventDraft(WorldCreateCards.withScene(base.copy(year = "1190"), scene))
        assertTrue(story.story)
        assertEquals(scene, story.sceneKey)
        assertEquals("", story.year)
        assertEquals(null, WorldCreateCards.withYear(base.copy(sceneKey = scene), "1").sceneKey)
        assertTrue(WorldCreateCards.canCreate(doc, base))
        assertFalse(WorldCreateCards.canCreate(doc, base.copy(year = "열")))
        val saved = io.github.lenticualrstone.civilight.feature.world.time.EventForms.save(doc, WorldCreateCards.eventDraft(base), null, WorldTestFixture.ctx())
            as io.github.lenticualrstone.civilight.feature.world.time.EventSave.Saved
        val event = saved.doc.index.event(saved.eventKey)!!
        assertEquals(EventKind.WORLD, event.kind.known)
        assertNull(event.worldDate)
    }

    @Test
    fun `the draft survives a save round trip`() {
        val draft = WorldCreateDraft.of(WorldKind.PLACE, CreateCardMode.PICKER, " 은빛 항구 ").copy(parentKey = "p", note = "n")
        assertEquals("은빛 항구", draft.name)
        assertEquals(draft, WorldCreateDraft.decode(draft.encode()))
        assertNull(WorldCreateDraft.decode("not json"))
        assertNull(WorldCreateDraft.decode(null))
    }

    // --- WLD-73 candidates ------------------------------------------------------------------------

    @Test
    fun `WLD-T79 candidates off - no section, no candidate column, no 글자 찾기 row`() {
        val doc = candidatesOff(WorldTestFixture.withTerm(WorldTestFixture.stateA(), T1) { it.copy(textSearch = false) })
        val ui = WorldListState.build(doc, WorldTab.TERMS, WorldTestFixture.index(doc), hiddenOpen = false)
        assertTrue(ui.items.none { it is WorldListItem.CandidateRow })
        assertTrue(ui.items.filterIsInstance<WorldListItem.Section>().none { it.title.startsWith("용어 후보") })
        assertNull(ui.items.filterIsInstance<WorldListItem.Row>().single { it.key == T1 }.candidates)
        val card = TermCardState.build(doc, PROJECT, T1, WorldTestFixture.index(doc), false, false)
        assertFalse(card.searchOff)
        assertTrue(card.groups.none { it.id == "candidates" })
    }

    @Test
    fun `WLD-73 the candidate section rows open their term, and stay in card view`() {
        val doc = WorldTestFixture.stateB()
        val ui = WorldListState.build(doc, WorldTab.TERMS, WorldTestFixture.index(doc), hiddenOpen = false)
        val rows = ui.items.filterIsInstance<WorldListItem.CandidateRow>()
        assertTrue(rows.any { it.key == T1 && it.fragment == "후보 3" && it.right == "◇ 공개 전 후보 3" })
        val section = ui.items.indexOfFirst { it is WorldListItem.Section && it.title.startsWith("용어 후보") }
        assertTrue(section >= 0 && ui.items.drop(section + 1).all { it is WorldListItem.CandidateRow })
        val cardLines = WorldCardRows.cardViewLines(ui.items)
        assertEquals(rows, cardLines.filterIsInstance<WorldListItem.CandidateRow>())
        assertTrue(cardLines.none { it is WorldListItem.Row })
    }

    @Test
    fun `WLD-73 the list is never pending while 용어 후보 is off`() {
        val doc = candidatesOff(WorldTestFixture.stateA())
        val pending = io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex.pending()
        assertFalse(WorldListState.build(doc, WorldTab.TERMS, pending, hiddenOpen = false).candidatesPending)
        assertTrue(WorldListState.build(WorldTestFixture.stateA(), WorldTab.TERMS, pending, hiddenOpen = false).candidatesPending)
    }

    // --- WLD-75 routes in the delete and merge sheets ----------------------------------------------

    @Test
    fun `WLD-75 route ends are not counted in the merge line while routes are hidden`() {
        val counts = mapOf(
            io.github.lenticualrstone.civilight.domain.trash.RefKind.SCENE_PLACE to 2,
            io.github.lenticualrstone.civilight.domain.trash.RefKind.ROUTE_END to 3,
        )
        assertFalse(io.github.lenticualrstone.civilight.feature.world.common.MergePlan.SHOWS_ROUTES)
        assertEquals("장소 2", io.github.lenticualrstone.civilight.feature.world.common.MergePlan.refParts(counts))
    }

    @Test
    fun `WLD-75 the delete sheet's 참조 N total leaves out route ends while routes are hidden`() {
        fun site(kind: io.github.lenticualrstone.civilight.domain.trash.RefKind, owner: String) =
            io.github.lenticualrstone.civilight.domain.trash.RefSite(kind, owner)
        val impact = io.github.lenticualrstone.civilight.domain.trash.DeleteImpact(
            items = listOf(io.github.lenticualrstone.civilight.domain.trash.ItemId.place("p1")),
            names = listOf("잿더미 시장"),
            hidden = false,
            owned = emptyMap(),
            relationPartners = emptyList(),
            references = listOf(
                site(io.github.lenticualrstone.civilight.domain.trash.RefKind.SCENE_PLACE, "s1"),
                site(io.github.lenticualrstone.civilight.domain.trash.RefKind.SCENE_PLACE, "s2"),
                site(io.github.lenticualrstone.civilight.domain.trash.RefKind.ROUTE_END, "r1"),
            ),
            heldWarnings = 0,
            speakerLocations = null,
        )
        val sheet = io.github.lenticualrstone.civilight.feature.world.common.MergePlan.deleteSheet(impact)
        assertEquals("장소 · 참조 2", sheet.sub)
        assertTrue(sheet.staying.none { it.label.startsWith("경로 끝") })
        assertEquals(1, io.github.lenticualrstone.civilight.feature.world.common.MergePlan.shownReferenceCount(3, 2))
    }

    // --- WLD-74 events --------------------------------------------------------------------------------

    @Test
    fun `WLD-T84 a name alone makes a dateless world event listed before the story`() {
        val doc = WorldTestFixture.stateA()
        val draft = WorldCreateDraft.event().copy(name = "결사 창립")
        assertTrue(WorldCreateCards.canCreate(doc, draft))
        val saved = io.github.lenticualrstone.civilight.feature.world.time.EventForms.save(doc, WorldCreateCards.eventDraft(draft), null, WorldTestFixture.ctx())
            as io.github.lenticualrstone.civilight.feature.world.time.EventSave.Saved
        val rows = TimelineState.build(saved.doc, TimelineView.EPISODE, null, false).rows
        val at = rows.indexOfFirst { it.eventKey == saved.eventKey }
        assertTrue("the new event is listed", at >= 0)
        val divider = rows.indexOfFirst { it.kind == io.github.lenticualrstone.civilight.feature.world.time.TimelineRowKind.DIVIDER }
        assertTrue("before the story", divider < 0 || at < divider)
    }

    // --- S-1 lens ---------------------------------------------------------------------------------

    @Test
    fun `WLD-T86 the 연표 and 연표 + 지도 read at 최신 whatever lens the world head picked`() {
        val latest = WorldTestFixture.stateA()
        val past = WorldTestFixture.atLens(latest, K.EPISODE_1_5)
        assertEquals(TimelineState.build(latest, TimelineView.EPISODE, null, false), TimelineState.build(past, TimelineView.EPISODE, null, false))
        // The world list (with its chip) still follows the picked point.
        val listPast = WorldListState.build(past, WorldTab.TERMS, WorldTestFixture.index(past), false)
        val pending = listPast.items.indexOfFirst { it is WorldListItem.Section && it.title == "아직 공개 전" }
        assertTrue(listPast.items.drop(pending).any { it is WorldListItem.Row && it.key == T1 })
    }

    // --- G-2 switch -------------------------------------------------------------------------------

    @Test
    fun `WLD-T87 the place card has no 배경 에셋 row with the switch off`() {
        val doc = WorldTestFixture.stateA()
        val place = doc.world.places.first { it.deletedAt == null && !it.hidden }.key
        assertNotNull(PlaceCardState.build(doc, PROJECT, place, hiddenOpen = false, readOnly = false).assets)
        assertNull(PlaceCardState.build(gameToolsOff(doc), PROJECT, place, hiddenOpen = false, readOnly = false).assets)
    }

    // --- ICON -------------------------------------------------------------------------------------

    @Test
    fun `ICON every icon-only button of the world screens has an accessibility name`() {
        val root = listOf(File("src/main"), File("feature/world/src/main")).first { it.exists() }
        assertEquals("", IconButtonLint.report(IconButtonLint.lintDir(root)))
    }
}
