package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.feature.story.search.SearchModel
import io.github.lenticualrstone.civilight.feature.story.search.SearchTap
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.navigation.WorldTab
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 검색 · 명령 (STO-36 … STO-38): context from the screen under the overlay, and where each row goes. */
class SearchModelTest {
    private val doc = Fx.doc()
    private val p = K.PROJECT

    @Test
    fun contextFollowsTheScreenUnderTheOverlay() {
        val inScript = SearchModel.context(doc, Script(p, blockKey = K.BLOCK_043), locked = true, openChecks = 3)
        assertTrue(inScript.inScript)
        assertEquals(K.SCENE_1_7_3, inScript.currentSceneKey)
        assertEquals(K.EPISODE_1_7, inScript.currentEpisodeKey)
        assertEquals(3, inScript.openCheckCount)
        val home = SearchModel.context(doc, ProjectHome(p), locked = true, openChecks = 0)
        assertFalse(home.inScript)
        assertNull(home.currentSceneKey)
        assertEquals("the episode of the last edit (#043 line, 1-7)", K.EPISODE_1_7, home.currentEpisodeKey)
    }

    @Test
    fun srch01_termAndLineRowsOpenTheirCardAndBlock() {
        val context = SearchModel.context(doc, ProjectHome(p), locked = true, openChecks = 3)
        val result = ProjectSearch(doc, context).search("오리지", SearchFilter.ALL)
        val term = result.section(SearchSectionKind.TERM)!!.items.first() as SearchItem.TermItem
        assertEquals(SearchTap.Open(TermCard(p, term.key)), SearchModel.tap(p, term, context))
        val line = result.section(SearchSectionKind.LINE)!!.items.first() as SearchItem.LineItem
        assertEquals(SearchTap.Open(Script(p, blockKey = line.key)), SearchModel.tap(p, line, context))
    }

    @Test
    fun commandsRouteToTheLensTheCheckCenterAndRenumber_STO38() {
        val home = SearchModel.context(doc, ProjectHome(p), locked = true, openChecks = 3)
        val commands = ProjectSearch(doc, home).search("", SearchFilter.COMMAND).section(SearchSectionKind.COMMAND)?.items.orEmpty()
            .ifEmpty { ProjectSearch(doc, home).search("점검", SearchFilter.COMMAND).section(SearchSectionKind.COMMAND)!!.items }
        val open = commands.filterIsInstance<SearchItem.CommandItem>().first { it.label.text.startsWith("점검 센터") }
        assertEquals(SearchTap.Open(CheckCenter(p)), SearchModel.tap(p, open, home))
        val lens = ProjectSearch(doc, home).search("시점", SearchFilter.COMMAND).section(SearchSectionKind.COMMAND)!!.items.single() as SearchItem.CommandItem
        assertEquals(SearchTap.Lens, SearchModel.tap(p, lens, home))
        val script = SearchModel.context(doc, Script(p, sceneKey = K.SCENE_1_7_3), locked = true, openChecks = 0)
        val toScene = ProjectSearch(doc, script).search("렌즈", SearchFilter.COMMAND).section(SearchSectionKind.COMMAND)!!.items.single()
        assertEquals(SearchTap.LensToScene(K.SCENE_1_7_3), SearchModel.tap(p, toScene, script))
        val renumber = ProjectSearch(doc, script).search("번호", SearchFilter.COMMAND).section(SearchSectionKind.COMMAND)!!.items.single()
        assertEquals(SearchTap.Renumber(K.EPISODE_1_7), SearchModel.tap(p, renumber, script))
    }

    @Test
    fun emptySearchOffersSimilarNamesAndTermCreation() {
        val context = SearchModel.context(doc, ProjectHome(p), locked = true, openChecks = 0)
        val result = ProjectSearch(doc, context).search("오리지누", SearchFilter.ALL)
        assertNotNull(result.empty)
        val empty = result.empty!!
        assertTrue(empty.similar.isNotEmpty())
        assertTrue(SearchModel.tap(p, empty.similar.first()) is SearchTap.Open)
        assertEquals(World(p, WorldTab.TERMS, createTerm = true), SearchModel.createTerm(p))
    }
}
