package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint
import io.github.lenticualrstone.civilight.domain.checks.CheckInputs
import io.github.lenticualrstone.civilight.domain.checks.WritingChecks
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.search.LineSpeaker
import io.github.lenticualrstone.civilight.domain.story.search.MoreItem
import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchCommand
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchMore
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.domain.story.search.Highlight
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.feature.story.check.CheckModel
import io.github.lenticualrstone.civilight.feature.story.search.SearchModel
import io.github.lenticualrstone.civilight.feature.story.search.SearchTap
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** (0.2.0) N-13 chips, S-1 lens commands, S-2 / D33 check center, DS-70 icon buttons of this module. */
class Story020Test {
    private val doc = Fx.doc()
    private val p = K.PROJECT

    @Test
    fun moreAndSpeakerChipLabels() {
        assertEquals("그 밖 ▾", SearchModel.moreChip(null))
        assertEquals("페이지 ▾", SearchModel.moreChip(SearchMore.PAGE))
        val speakers = listOf(LineSpeaker(K.SERA, "세라"))
        assertEquals("화자 ▾", SearchModel.speakerChip(null, speakers))
        assertEquals("세라 ▾", SearchModel.speakerChip(K.SERA, speakers))
        assertEquals("화자 ▾", SearchModel.speakerChip(K.RIEN, speakers))
    }

    @Test
    fun speakerChipOnlyUnderLines() {
        assertTrue(SearchModel.showsSpeaker(SearchFilter.LINE, null))
        assertFalse(SearchModel.showsSpeaker(SearchFilter.ALL, null))
        assertFalse(SearchModel.showsSpeaker(SearchFilter.LINE, SearchMore.PLACE))
    }

    @Test
    fun moreRowsOpenTheirOwnScreens() {
        val h = Highlight("x", emptyList())
        assertEquals(SearchTap.Open(PlaceCard(p, "a")), SearchModel.tap(p, MoreItem(SearchMore.PLACE, "a", h)))
        assertEquals(SearchTap.Open(FactionCard(p, "a")), SearchModel.tap(p, MoreItem(SearchMore.FACTION, "a", h)))
        assertEquals(SearchTap.Open(EventDetail(p, "a")), SearchModel.tap(p, MoreItem(SearchMore.EVENT, "a", h)))
        assertEquals(SearchTap.Open(PageDoc(p, "a")), SearchModel.tap(p, MoreItem(SearchMore.PAGE, "a", h)))
    }

    /** S-1: no `시점 바꾸기` from home and no `렌즈를 이 씬으로 맞추기` in the script; the other commands stay. */
    @Test
    fun searchListsNoLensCommands() {
        val home = SearchModel.withoutLensCommands(ProjectSearch(doc, SearchContext(currentEpisodeKey = K.EPISODE_1_7)).search("", SearchFilter.COMMAND))
        val commands = home.section(SearchSectionKind.COMMAND)!!.items.map { (it as SearchItem.CommandItem).command }
        assertFalse(SearchCommand.CHANGE_LENS in commands)
        assertTrue(SearchCommand.OPEN_CHECKS in commands)
        val script = SearchModel.withoutLensCommands(
            ProjectSearch(doc, SearchContext(inScript = true, currentSceneKey = K.SCENE_1_7_3, currentEpisodeKey = K.EPISODE_1_7)).search("", SearchFilter.COMMAND),
        )
        assertFalse(script.section(SearchSectionKind.COMMAND)!!.items.any { (it as SearchItem.CommandItem).command == SearchCommand.LENS_TO_SCENE })
        val onlyLens = SearchModel.withoutLensCommands(ProjectSearch(doc).search("시점", SearchFilter.COMMAND))
        assertTrue(onlyLens.sections.isEmpty())
        assertTrue(onlyLens.empty != null)
    }

    /** S-1, SRCH-07: over the cast and world heads (where the lens chip is drawn) `시점 바꾸기` stays and opens the lens sheet. */
    @Test
    fun searchKeepsChangeLensOverTheCastAndWorldHeads() {
        for (below in listOf(CastList(p), World(p))) {
            val context = SearchModel.context(doc, below, locked = true, openChecks = 0)
            val result = SearchModel.withoutLensCommands(ProjectSearch(doc, context).search("시점", SearchFilter.COMMAND), below)
            val lens = result.section(SearchSectionKind.COMMAND)!!.items.single() as SearchItem.CommandItem
            assertEquals(SearchCommand.CHANGE_LENS, lens.command)
            assertEquals(SearchTap.Lens, SearchModel.tap(p, lens, context))
            assertTrue(SearchModel.keepsLensCommand(below))
        }
        for (below in listOf(ProjectHome(p), Script(p, sceneKey = K.SCENE_1_7_3), CharacterProfile(p, K.SERA), TermCard(p, K.TERM_ORIGINIUM_LANTERN), null)) {
            assertFalse(below.toString(), SearchModel.keepsLensCommand(below))
            val context = SearchModel.context(doc, below, locked = true, openChecks = 0)
            val all = SearchModel.withoutLensCommands(ProjectSearch(doc, context).search("", SearchFilter.COMMAND), below)
            val commands = all.section(SearchSectionKind.COMMAND)?.items.orEmpty().map { (it as SearchItem.CommandItem).command }
            assertFalse(below.toString(), SearchCommand.CHANGE_LENS in commands || SearchCommand.LENS_TO_SCENE in commands)
        }
    }

    /** S-2: with the app defaults (travel check off) and the switch off, no `이동` or `보이스` chip. */
    @Test
    fun checkCenterChipsFollowTheGates() {
        val off = doc.copy(project = doc.project.copy(gameTools = false))
        val inputs = CheckInputs(now = Fx.ctx().now, zone = Fx.zone, travel = TravelSettings(checkTravel = false), hiddenUnlocked = true)
        val ui = CheckModel.build(WritingChecks.run(off, inputs), CheckFilter.VOICE, stale = false)
        assertEquals(CheckFilter.ALL, ui.filter)
        assertTrue(ui.header.chips.none { it.filter == CheckFilter.VOICE || it.filter == CheckFilter.TRAVEL })
        val on = CheckInputs(now = Fx.ctx().now, zone = Fx.zone, travel = TravelSettings(checkTravel = true), hiddenUnlocked = true)
        val all = CheckModel.build(WritingChecks.run(doc, on), CheckFilter.TRAVEL, stale = false)
        assertEquals(CheckFilter.TRAVEL, all.filter)
        assertTrue(all.header.chips.any { it.filter == CheckFilter.VOICE })
    }

    /** DS-70: every icon-only button of this module carries a non-blank accessibility name. */
    @Test
    fun iconButtonLint() {
        val issues = IconButtonLint.lintDir(File("src/main"))
        assertEquals(IconButtonLint.report(issues), 0, issues.size)
    }
}
