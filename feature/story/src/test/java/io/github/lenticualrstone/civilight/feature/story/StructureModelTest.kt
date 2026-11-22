package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.feature.story.Fx.node
import io.github.lenticualrstone.civilight.feature.story.structure.EpisodeItem
import io.github.lenticualrstone.civilight.feature.story.structure.GroupItem
import io.github.lenticualrstone.civilight.feature.story.structure.NewSceneTarget
import io.github.lenticualrstone.civilight.feature.story.structure.SceneItem
import io.github.lenticualrstone.civilight.feature.story.structure.StructureModel
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 스토리 구조 list (STO-04, STO-05 target, STO-06; 20-story-time §4.14 STR-01, STR-02, STR-04). */
class StructureModelTest {
    private val doc = Fx.doc()

    private fun build(d: ProjectDoc = doc, expanded: List<String> = StructureModel.initialExpanded(d, null), locked: Boolean = true) =
        StructureModel.build(d, expanded, locked, readOnly = false)

    @Test
    fun str01_groupHeadersAndPlannedChapter() {
        val ui = build()
        assertEquals("단계 이름: 장 › 에피소드 › 씬", ui.levelLine)
        val groups = ui.items.filterIsInstance<GroupItem>()
        assertEquals(listOf("Ⅰ 1장 · 등불지기" to "7/10", "Ⅱ 2장 · 잿불" to "0/8 · 계획"), groups.map { it.row.header to it.row.progress })
        assertFalse(groups[0].expandable)
        assertTrue(groups[1].expandable)
        assertFalse(groups[1].expanded)
        assertEquals("상층으로 올라간 리엔이 꺼진 빛의 값을 알게 된다.", groups[1].row.synopsis)
        // The planned chapter shows its synopsis instead of rows: nothing after its header.
        assertEquals(groups[1], ui.items.last())
        assertEquals("명령 또는 검색 · 씬 이동", ui.commandHint)
    }

    @Test
    fun exactlyTheCurrentEpisodeIsExpandedOnEntry() {
        assertEquals(listOf(K.EPISODE_1_7), StructureModel.initialExpanded(doc, null))
        val ui = build()
        val episodes = ui.items.filterIsInstance<EpisodeItem>()
        assertEquals(listOf(K.EPISODE_1_7), episodes.filter { it.row.expanded }.map { it.row.key })
        assertTrue(episodes.single { it.row.key == K.EPISODE_1_7 }.current)
        val scenes = ui.items.filterIsInstance<SceneItem>()
        assertEquals(listOf("씬 1", "씬 2", "씬 3", "씬 4"), scenes.map { it.row.label.substringBefore(" ·") })
        assertTrue(scenes.all { it.indented })
        // Scene rows follow their episode row.
        val at = ui.items.indexOfFirst { it is EpisodeItem && it.row.key == K.EPISODE_1_7 }
        assertEquals(scenes, ui.items.subList(at + 1, at + 5))
    }

    @Test
    fun str02_sceneRowsOfTheExpandedEpisode() {
        val scenes = build().items.filterIsInstance<SceneItem>().map { it.row }
        assertEquals("2일째 새벽 | 씬 3 · 꺼진 등불 | 쓰는 중", scenes[2].let { "${it.time} | ${it.label} | ${it.meta}" })
        assertTrue(scenes[2].current)
        assertEquals("2일째 아침 | 씬 4 · 미정 | —", scenes[3].let { "${it.time} | ${it.label} | ${it.meta}" })
        val episode = build().items.filterIsInstance<EpisodeItem>().single { it.row.key == K.EPISODE_1_7 }.row
        assertTrue(episode.meta.startsWith("씬 4개 · "))
    }

    @Test
    fun str04_aTrashedSceneIsNotListedNorCounted() {
        val deleted = SceneActions.delete(doc, K.SCENE_1_7_3, Fx.ctx()).doc
        val ui = build(deleted, listOf(K.EPISODE_1_7))
        val scenes = ui.items.filterIsInstance<SceneItem>()
        assertEquals(listOf("씬 1", "씬 2", "씬 3"), scenes.map { it.row.label.substringBefore(" ·") })
        assertTrue(ui.items.filterIsInstance<EpisodeItem>().single { it.row.key == K.EPISODE_1_7 }.row.meta.startsWith("씬 3개"))
    }

    @Test
    fun focusOpensTheEpisodeOfAScene_andPlannedChaptersAboveIt() {
        assertEquals(listOf(K.EPISODE_1_5), StructureModel.initialExpanded(doc, K.SCENE_1_5_2))
        assertEquals(listOf(K.EPISODE_1_5), StructureModel.initialExpanded(doc, K.EPISODE_1_5))
        assertEquals(listOf(K.CHAPTER_2, K.EPISODE_2_3), StructureModel.initialExpanded(doc, K.EPISODE_2_3))
        val ui = build(expanded = StructureModel.initialExpanded(doc, K.EPISODE_2_3))
        assertTrue(ui.items.filterIsInstance<GroupItem>().last().expanded)
        assertTrue(ui.items.any { it is EpisodeItem && it.row.key == K.EPISODE_2_3 && it.row.expanded })
    }

    @Test
    fun toggleKeepsTheNewestLast() {
        val a = StructureModel.toggle(listOf(K.EPISODE_1_7), K.EPISODE_1_5)
        assertEquals(listOf(K.EPISODE_1_7, K.EPISODE_1_5), a)
        assertEquals(listOf(K.EPISODE_1_5), StructureModel.toggle(a, K.EPISODE_1_7))
    }

    @Test
    fun newSceneGoesAfterTheSceneBeingWritten() {
        assertEquals(NewSceneTarget(K.EPISODE_1_7, K.SCENE_1_7_3), build().newScene)
    }

    @Test
    fun newSceneGoesToTheMostRecentlyExpandedEpisode() {
        val view = StoryOverview(doc)
        val last15 = view.scenesOf(doc.index.unit(K.EPISODE_1_5)!!).last().key
        assertEquals(NewSceneTarget(K.EPISODE_1_5, last15), build(expanded = listOf(K.EPISODE_1_7, K.EPISODE_1_5)).newScene)
        val emptyEpisode = Fx.mini(listOf("장", "에피소드", "씬"), node("C", null, null, node("E1", null, null, node("S1", null, "글")), node("E2")))
        assertEquals(NewSceneTarget("E2", null), build(emptyEpisode, listOf("E2")).newScene)
        assertEquals(NewSceneTarget("E1", "S1"), build(emptyEpisode, listOf("E1")).newScene)
    }

    @Test
    fun oneLevelListsScenesAtTheTopAndCreatesThere() {
        val d = Fx.mini(listOf("씬"), node("S1", "첫 장", "글"), node("S2"))
        val ui = build(d, emptyList())
        assertEquals(listOf("씬 1 · 첫 장", "씬 2 · 미정"), ui.items.filterIsInstance<SceneItem>().map { it.row.label })
        assertTrue(ui.items.filterIsInstance<SceneItem>().none { it.indented })
        assertEquals(io.github.lenticualrstone.civilight.feature.story.structure.NewSceneTarget(null, "S2"), ui.newScene)
    }

    @Test
    fun twoAndFourLevelHierarchies() {
        val two = Fx.mini(listOf("막", "장면"), node("A1", "첫 막", null, node("S1", null, "글")), node("A2"))
        val twoUi = build(two, listOf("A1"))
        assertEquals(listOf("1막", "2막"), twoUi.items.filterIsInstance<EpisodeItem>().map { it.row.code })
        assertTrue(twoUi.items.none { it is GroupItem })
        assertEquals(listOf("장면 1 · 미정"), twoUi.items.filterIsInstance<SceneItem>().map { it.row.label })
        val four = Fx.mini(
            listOf("부", "장", "화", "씬"),
            node("P1", "첫 부", null, node("C1", "첫 장", null, node("E1", null, null, node("S1", null, "글")))),
        )
        val fourUi = build(four, listOf("E1"))
        val groups = fourUi.items.filterIsInstance<GroupItem>()
        assertEquals(listOf(0, 1), groups.map { it.depth })
        assertEquals(listOf("1-1-1"), fourUi.items.filterIsInstance<EpisodeItem>().map { it.row.code })
        assertEquals(NewSceneTarget("E1", "S1"), fourUi.newScene)
    }

    @Test
    fun lockedHiddenSceneIsMasked() {
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_7_4) it.copy(hidden = true, title = "문 앞") else it }))
        val locked = build(hidden).items.filterIsInstance<SceneItem>().single { it.row.key == K.SCENE_1_7_4 }
        assertEquals("씬 4 · 숨긴 씬", locked.row.label)
        val open = build(hidden, locked = false).items.filterIsInstance<SceneItem>().single { it.row.key == K.SCENE_1_7_4 }
        assertEquals("씬 4 · 문 앞", open.row.label)
    }
}
