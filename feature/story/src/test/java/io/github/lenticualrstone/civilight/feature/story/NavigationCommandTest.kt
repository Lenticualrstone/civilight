package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.feature.story.home.HomeModel
import io.github.lenticualrstone.civilight.feature.story.search.SearchModel
import io.github.lenticualrstone.civilight.feature.story.search.SearchTap
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** STO-38 navigation commands (`1-7 씬 3`, `#043`) and the STO-43 `TwoHome` selection. */
class NavigationCommandTest {
    private val doc = Fx.doc()
    private val p = K.PROJECT

    @Test
    fun sto38_aSceneTitleListsTheSceneAndOpensItsHead() {
        val home = SearchModel.context(doc, ProjectHome(p), locked = true, openChecks = 0)
        val title = doc.index.unit(K.SCENE_1_7_3)!!.title!!
        val scenes = ProjectSearch(doc, home).search(title, SearchFilter.SCENE).section(SearchSectionKind.SCENE)!!.items
        val scene = scenes.filterIsInstance<SearchItem.SceneItem>().first { it.key == K.SCENE_1_7_3 }
        assertEquals(SearchTap.Open(Script(p, sceneKey = K.SCENE_1_7_3)), SearchModel.tap(p, scene, home))
    }

    @Test
    fun sto38_hashNumberInsideTheScriptListsThatBlockOfTheEpisode() {
        val script = SearchModel.context(doc, Script(p, sceneKey = K.SCENE_1_7_3), locked = true, openChecks = 0)
        val result = ProjectSearch(doc, script).search("#043", SearchFilter.ALL)
        val lines = result.section(SearchSectionKind.LINE)!!.items.filterIsInstance<SearchItem.LineItem>()
        assertTrue(lines.any { it.key == K.BLOCK_043 })
        assertEquals(SearchTap.Open(Script(p, blockKey = K.BLOCK_043)), SearchModel.tap(p, lines.first { it.key == K.BLOCK_043 }, script))
    }

    @Test
    fun sto43_theEpisodeShownBesideHomeIsSelected() {
        val stack = listOf(Library, ProjectHome(p), StoryStructure(p, focusUnitKey = K.EPISODE_1_7))
        assertEquals(K.EPISODE_1_7, HomeModel.selectedKey(stack, p))
        assertNull(HomeModel.selectedKey(listOf(Library, ProjectHome(p)), p))
        assertNull(HomeModel.selectedKey(listOf(ProjectHome(p), Script(p, sceneKey = K.SCENE_1_7_3)), p))
    }
}
