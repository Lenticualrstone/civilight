package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.bulk.BulkFilter
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkList
import io.github.lenticualrstone.civilight.domain.story.bulk.WhenMode
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.Fx.withUnit
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkModel
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkPlaceForm
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkSuggestForm
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkSuggestModel
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkWhenForm
import io.github.lenticualrstone.civilight.feature.story.common.MiniMaps
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 장소 · 때 한꺼번에 (STO-31 … STO-35) over the example project (`domain:story` numbers, BulkTest). */
class BulkModelTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()
    private val s162 = K.SCENE_1_6_2
    private val s181 = K.SCENE_1_8_1
    private val s182 = K.SCENE_1_8_2
    private val s191 = K.SCENE_1_9_1
    private val selection = listOf(s162, s181, s182, s191)

    /** BLK-02's Given: 1-7 씬 4 and 1-8 씬 1 untimed. */
    private fun untimed(): ProjectDoc = doc.withUnit(K.SCENE_1_7_4) { it.copy(time = SceneTime.None) }.withUnit(s181) { it.copy(time = SceneTime.None) }

    private fun ui(d: ProjectDoc = doc, filter: BulkFilter = BulkFilter.ALL, selected: List<String> = emptyList(), locked: Boolean = false) =
        BulkModel.build(d, K.CHAPTER_1, filter, selected, locked)!!

    @Test
    fun blk01_headProgressAndChipsMatchTheDomainList() {
        val ui = ui()
        val list = BulkList(doc, K.CHAPTER_1)
        assertEquals("잿빛 새벽의 등불 · 1장", ui.head)
        assertEquals("잿빛 새벽의 등불 · 1장 · 펼친 화면", ui.wideHead)
        assertEquals("장소 · 때 한꺼번에", ui.title)
        assertEquals("장소 | 22/31", ui.place.text)
        assertEquals("때 | 19/31", ui.time.text)
        assertEquals(listOf("전체 31", "장소 · 때 빈 것 14", "제안 있음 3"), ui.chips.map { it.label })
        assertEquals(BulkFilter.entries.map(list::chip), ui.chips.map { it.label })
        assertEquals(list.groups().map { it.code }, ui.groups.map { it.code })
        assertEquals(list.groups().flatMap { it.rows }, ui.rows)
        assertNull(ui.footer)
        assertFalse(ui.canApply(readOnly = false))
    }

    @Test
    fun filtersAndSelectionGiveTheFooterAndButtons() {
        val empty = ui(filter = BulkFilter.EMPTY)
        assertEquals(14, empty.rows.size)
        assertTrue(empty.rows.all { it.placeEmpty || it.timeEmpty })
        val suggested = ui(filter = BulkFilter.SUGGESTED)
        assertEquals(listOf(K.SCENE_1_4_1, s162, s182), suggested.rows.map { it.sceneKey })
        val chosen = ui(untimed(), selected = selection.reversed())
        assertEquals(selection, chosen.selected)
        assertEquals("고른 씬 4개 · 1-6 씬 2 · 1-8 씬 1 · 1-8 씬 2 · 1-9 씬 1", chosen.footer)
        assertEquals("고른 씬 4개", chosen.wideCount)
        assertEquals("제안 받기 2", chosen.suggestButton)
        assertTrue(chosen.canApply(readOnly = false))
        assertFalse("read-only (the example) can choose but not apply", chosen.canApply(readOnly = true))
        assertTrue("the proposals can still be read", chosen.canSuggest())
    }

    @Test
    fun selectAllChoosesTheShownScenesAndASecondTapLetsThemGo() {
        val empty = ui(filter = BulkFilter.EMPTY)
        val all = BulkModel.selectAll(empty, emptyList())
        assertEquals(empty.rows.map { it.sceneKey }, all)
        val again = BulkModel.selectAll(ui(filter = BulkFilter.EMPTY, selected = all), all)
        assertTrue(again.isEmpty())
        assertEquals(listOf(s162), BulkModel.toggle(emptyList(), s162))
        assertTrue(BulkModel.toggle(listOf(s162), s162).isEmpty())
    }

    @Test
    fun theChapterComesFromThePreselectionElseTheSceneBeingWritten() {
        assertEquals(K.CHAPTER_1, BulkModel.chapterKey(doc, listOf(s162)))
        assertEquals(K.CHAPTER_1, BulkModel.chapterKey(doc))
        assertEquals(listOf(s162, s182), BulkModel.preselection(listOf(s162, s182)))
        assertNull(BulkModel.preselection("bogus"))
        assertEquals("story.bulk.preselect.${K.PROJECT}", BulkModel.preselectKey(K.PROJECT))
    }

    @Test
    fun aOneLevelStructureTakesEveryScene() {
        val mini = Fx.mini(listOf("씬"), Fx.node("S1", "하나"), Fx.node("S2", "둘"))
        assertNull(BulkModel.chapterKey(mini))
        val ui = BulkModel.build(mini, null, BulkFilter.ALL, emptyList(), locked = false)!!
        assertEquals(listOf("S1", "S2"), ui.rows.map { it.sceneKey })
        assertEquals("작은 이야기", ui.head)
        assertEquals("장소 | 0/2", ui.place.text)
    }

    @Test
    fun lockedHiddenScenesLeaveTheScope_R19() {
        val hidden = doc.withUnit(s162) { it.copy(hidden = true) }
        assertFalse(ui(hidden, locked = true).rows.any { it.sceneKey == s162 })
        assertEquals("장소 | 22/30", ui(hidden, locked = true).place.text)
        assertTrue(ui(hidden, locked = false).rows.any { it.sceneKey == s162 })
    }

    @Test
    fun blk02_theWhenFormPreviewsThenAppliesAsOneStep() {
        val d = untimed()
        val form = BulkWhenForm()
        assertEquals(WhenMode.CHAIN, form.mode)
        assertEquals(TimePrecision.APPROX, form.precision)
        val plan = form.plan(d, selection)
        assertEquals(
            listOf(
                "1-6 씬 2 | 1-6 · 2일째 · 대략 | 직전 1-6 씬 1 · 1일째 낮",
                "1-8 씬 1 | 1-8 · 1일째 · 대략 | 1-7 씬 3 기준 · 직전 씬 때 없음",
                "1-8 씬 2 | 1-8 · 2일째 · 대략 | 직전 1-8 씬 1",
                "1-9 씬 1 | 1-9 · 1일째 · 대략 | 직전 1-8 씬 2",
            ),
            plan.rows.map { it.line },
        )
        assertEquals("씬 4개에 넣기", plan.button)
        for (k in selection) assertEquals(SceneTime.None, d.unit(k).time)
        val after = form.plan(d, selection, ctx).result
        for (k in selection) assertTrue(after.unit(k).time is SceneTime.Relative)
        // A scene deleted meanwhile is left out on the doc the step is applied to.
        val gone = d.copy(story = d.story.copy(units = d.story.units.filterNot { it.key == s191 }))
        assertEquals(3, form.plan(gone, selection).rows.size)
    }

    @Test
    fun theWhenFormSurvivesItsSavedText() {
        val form = BulkWhenForm(WhenMode.ALL_SAME, 2, perScene = true, perSceneChips = mapOf(s162 to 0), band = Band.NIGHT.name, precision = TimePrecision.EXACT)
        assertEquals(form, BulkWhenForm.decode(form.encode()))
        assertEquals(BulkWhenForm(), BulkWhenForm.decode("garbage"))
        assertEquals(0, form.chipOf(s162))
        assertEquals(2, form.chipOf(s182))
    }

    @Test
    fun blk03_samePlaceForTheChoiceIsOneStep() {
        val scope = BulkModel.scope(doc, K.CHAPTER_1, locked = false).map { it.key }
        val market = BulkPlaceForm.options(doc, scope, regions = false).single { it.name == "잿더미 시장" }
        assertEquals("씬 4개", market.countText)
        assertEquals("씬 4개 → 잿더미 시장", BulkPlaceForm.footer(doc, selection.size, listOf(market.ref)))
        val after = BulkPlaceForm.applySame(doc, selection, market.ref, ctx)
        for (k in selection) assertTrue(market.ref.sameAs(after.unit(k).place))
        val changed = after.story.units.zip(doc.story.units).filter { (a, b) -> a != b }.map { it.first.key }.toSet()
        assertEquals(selection.toSet(), changed)
        // A place that is gone changes nothing.
        assertEquals(doc, BulkPlaceForm.applySame(doc, selection, PlaceRef(PlaceRefKind.PLACE, "gone"), ctx))
    }

    @Test
    fun oneByOneGivesEachSceneItsPlaceAndNamesThemInOrder() {
        val lower = PlaceRef(PlaceRefKind.PLACE, K.PLACE_LOWER_STREET)
        val market = PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET)
        val choices = mapOf(s162 to market, s182 to lower)
        assertEquals("씬 2개 → 잿더미 시장 · 하층 거리", BulkPlaceForm.footer(doc, 2, listOf(market, lower)))
        val after = BulkPlaceForm.applyEach(doc, choices, ctx)
        assertTrue(market.sameAs(after.unit(s162).place))
        assertTrue(lower.sameAs(after.unit(s182).place))
        assertEquals(doc.unit(s191).place, after.unit(s191).place)
    }

    @Test
    fun sug01_putFillsFromTheProposalAndNothingChangesBefore_SUG05() {
        val sheet = BulkSuggestModel.sheet(doc, selection, handled = emptyList())
        assertEquals("제안 받기", sheet.title)
        assertEquals("장소 · 때 한꺼번에 · 고른 씬 4개", sheet.head)
        assertEquals(selection, sheet.cards.map { it.sceneKey })
        assertEquals(listOf(s162, s182), BulkSuggestModel.withProposal(sheet))
        val after = BulkSuggestForm.put(doc, s162, ctx)
        assertTrue(PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET).sameAs(after.unit(s162).place))
        // Put again on the new doc: the scene has nothing left to fill, so nothing changes.
        assertEquals(after, BulkSuggestForm.put(after, s162, ctx))
        // Handled cards leave the page.
        assertEquals(listOf(s181, s182, s191), BulkSuggestModel.sheet(after, selection, listOf(s162)).cards.map { it.sceneKey })
    }

    @Test
    fun putAllIsOneStepOverEveryProposal() {
        val after = BulkSuggestForm.putAll(doc, listOf(s162, s182), ctx)
        assertNotNull(after.unit(s162).place)
        assertNotNull(after.unit(s182).place)
        assertEquals(doc, BulkSuggestForm.putAll(doc, listOf(s191), ctx))
    }

    @Test
    fun thePlaceMapShowsLivePinsWithCountsAndTheChosenOne() {
        val map = MiniMaps.places(doc, labels = mapOf(K.PLACE_ASH_MARKET to "4"), accent = setOf(K.PLACE_ASH_MARKET))
        assertNotNull(map)
        val market = map!!.pins.single { it.placeKey == K.PLACE_ASH_MARKET }
        assertEquals("4", market.label)
        assertTrue(market.accent)
        assertTrue(map.pins.filter { it.placeKey != K.PLACE_ASH_MARKET }.none { it.accent })
        val hit = io.github.lenticualrstone.civilight.feature.story.common.MiniMaps.pinAt(map, market.at, 0.01f)
        assertEquals(K.PLACE_ASH_MARKET, hit?.placeKey)
    }

    @Test
    fun lockedHiddenPlacesNeverShowTheirName_R19() {
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(hidden = true) else it }))
        val locked = ui(hidden, locked = true)
        val row161 = locked.rows.single { it.sceneKey == K.SCENE_1_6_1 }
        assertEquals("숨긴 항목", row161.place)
        assertEquals("잿더미 시장", ui(hidden, locked = false).rows.single { it.sceneKey == K.SCENE_1_6_1 }.place)
        // 1-6 씬 2's proposal names the hidden place: no proposal word, not counted.
        assertNull(locked.rows.single { it.sceneKey == s162 }.suggestion)
        assertEquals("제안 있음 2", locked.chips.single { it.filter == BulkFilter.SUGGESTED }.label)
        val scope = BulkModel.scope(hidden, K.CHAPTER_1, locked = true).map { it.key }
        assertFalse(BulkPlaceForm.options(hidden, scope, regions = false, locked = true).any { it.ref.key == K.PLACE_ASH_MARKET })
        val map = MiniMaps.places(hidden, exclude = BulkModel.hiddenPlaces(hidden, locked = true))
        assertFalse(map?.pins.orEmpty().any { it.placeKey == K.PLACE_ASH_MARKET })
        val card = BulkSuggestModel.sheet(hidden, selection, handled = emptyList(), locked = true).cards.single { it.sceneKey == s162 }
        assertNull(card.suggestion)
        assertFalse(card.header.contains("잿더미"))
    }
}
