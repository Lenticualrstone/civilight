package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.scene.SceneHeads
import io.github.lenticualrstone.civilight.feature.story.Fx.node
import io.github.lenticualrstone.civilight.feature.story.menu.MenuAction
import io.github.lenticualrstone.civilight.feature.story.menu.MenuSurface
import io.github.lenticualrstone.civilight.feature.story.menu.StructureMenuModel
import io.github.lenticualrstone.civilight.feature.story.menu.UnitMenuModel
import io.github.lenticualrstone.civilight.feature.story.menu.menuSurfaceFor
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The ⋯ menus (STO-07; SCR-47 `RSHeadMenu` rows). */
class MenuModelTest {
    private val doc = Fx.doc()

    @Test
    fun sceneMenuInTheStoryList() {
        val menu = UnitMenuModel.build(doc, K.SCENE_1_7_4, MenuSurface.STORY_LIST, readOnly = false)!!
        assertEquals("씬 4 · 1-7", menu.title)
        assertEquals(
            listOf(MenuAction.PLACE_TIME, MenuAction.TRANSITION, MenuAction.MERGE_PREVIOUS, MenuAction.SPLIT_HERE, MenuAction.DELETE_SCENE, MenuAction.MARK_DONE),
            menu.rows.map { it.action },
        )
        val byAction = menu.rows.associateBy { it.action }
        assertEquals("장소와 때" to "결사의 문 · 2일째 아침 06:10", byAction.getValue(MenuAction.PLACE_TIME).let { it.label to it.value })
        assertEquals("직접 · 결사의 문 앞 · 06:10", byAction.getValue(MenuAction.TRANSITION).value)
        assertEquals("앞 씬과 합치기" to "씬 3 · 꺼진 등불", byAction.getValue(MenuAction.MERGE_PREVIOUS).let { it.label to it.value })
        assertEquals("여기서 나누기", byAction.getValue(MenuAction.SPLIT_HERE).label)
        assertNull(byAction.getValue(MenuAction.SPLIT_HERE).value)
        assertEquals("씬 지우기" to "휴지통 · 30일", byAction.getValue(MenuAction.DELETE_SCENE).let { it.label to it.value })
        assertEquals("미정", byAction.getValue(MenuAction.MARK_DONE).value)
        assertTrue(menu.rows.all { it.enabled })
    }

    @Test
    fun firstSceneOfAnEpisodeCannotMergeWithThePrevious() {
        val menu = UnitMenuModel.build(doc, K.SCENE_1_7_1, MenuSurface.STORY_LIST, readOnly = false)!!
        val merge = menu.rows.single { it.action == MenuAction.MERGE_PREVIOUS }
        assertFalse(merge.enabled)
        assertNull(merge.value)
    }

    @Test
    fun sceneHeadMenuIsTheScriptsRowSet_SCR47() {
        val menu = UnitMenuModel.build(doc, K.SCENE_1_7_4, MenuSurface.SCENE_HEAD, readOnly = false)!!
        val head = SceneHeads(doc).menu(K.SCENE_1_7_4)!!
        assertEquals(head.title, menu.title)
        assertEquals(head.rows.map { it.label to it.value }, menu.rows.map { it.label to it.value })
        assertEquals(
            listOf(MenuAction.PLACE_TIME, MenuAction.TRANSITION, MenuAction.MERGE_PREVIOUS, MenuAction.DELETE_SCENE, MenuAction.NEW_SCENE_BEFORE),
            menu.rows.map { it.action },
        )
    }

    @Test
    fun readOnlyDisablesEveryMutatingRow() {
        for (surface in MenuSurface.entries) {
            val menu = UnitMenuModel.build(doc, K.SCENE_1_7_4, surface, readOnly = true)!!
            assertTrue(menu.rows.isNotEmpty())
            menu.rows.filter { it.action != MenuAction.MERGE_PREVIOUS || it.value != null }.forEach { row ->
                assertFalse("$row", row.enabled)
                assertEquals("읽기 전용", row.value)
            }
        }
        val episode = UnitMenuModel.build(doc, K.EPISODE_1_7, MenuSurface.STORY_LIST, readOnly = true)!!
        assertTrue(episode.rows.none { it.enabled })
        val structure = StructureMenuModel.build(doc, readOnly = true)
        assertEquals(listOf(true, true, false), structure.rows.map { it.enabled })
    }

    @Test
    fun episodeMenu() {
        val menu = UnitMenuModel.build(doc, K.EPISODE_1_7, MenuSurface.STORY_LIST, readOnly = false)!!
        assertEquals("1-7 · 잿빛 새벽의 등불", menu.title)
        assertEquals(listOf(MenuAction.RENUMBER, MenuAction.NEW_UNIT_AFTER, MenuAction.MARK_DONE), menu.rows.map { it.action })
        assertEquals("번호 다시 매기기 · 1-7", menu.rows[0].label)
        assertEquals("TODO_COPY_STO_NEW_EPISODE", menu.rows[1].label)
        assertEquals("쓰는 중 · 씬 3", menu.rows[2].value)
        val done = UnitMenuModel.build(doc, K.EPISODE_1_5, MenuSurface.STORY_LIST, readOnly = false)!!
        assertEquals(MenuAction.UNMARK_DONE to "완료", done.rows[2].let { it.action to it.value })
        assertEquals("1-8 · 미정", UnitMenuModel.build(doc, K.EPISODE_1_8, MenuSurface.STORY_LIST, readOnly = false)!!.title)
    }

    @Test
    fun chapterMenuAndGoneUnits() {
        val menu = UnitMenuModel.build(doc, K.CHAPTER_1, MenuSurface.STORY_LIST, readOnly = false)!!
        assertEquals("Ⅰ 1장 · 등불지기", menu.title)
        assertEquals(listOf(MenuAction.NEW_UNIT_AFTER), menu.rows.map { it.action })
        assertEquals("TODO_COPY_STO_NEW_CHAPTER", menu.rows[0].label)
        assertNull(UnitMenuModel.build(doc, "unknown", MenuSurface.STORY_LIST, readOnly = false))
    }

    @Test
    fun structureMenuFollowsTheLevelCount() {
        val three = StructureMenuModel.build(doc, readOnly = false)
        assertEquals("스토리", three.title)
        assertEquals(listOf(MenuAction.BULK, MenuAction.LEVEL_NAMES, MenuAction.NEW_TOP_UNIT), three.rows.map { it.action })
        assertEquals("장소 · 때 한꺼번에", three.rows[0].label)
        assertEquals("단계 이름" to "장 › 에피소드 › 씬", three.rows[1].let { it.label to it.value })
        assertEquals("TODO_COPY_STO_NEW_CHAPTER", three.rows[2].label)
        val two = StructureMenuModel.build(Fx.mini(listOf("막", "장면"), node("A1")), readOnly = false)
        assertEquals("TODO_COPY_STO_NEW_EPISODE", two.rows.last().label)
        val one = StructureMenuModel.build(Fx.mini(listOf("씬"), node("S1")), readOnly = false)
        assertEquals(listOf(MenuAction.BULK, MenuAction.LEVEL_NAMES), one.rows.map { it.action })
    }

    @Test
    fun surfaceFollowsTheHostScreen() {
        assertEquals(MenuSurface.SCENE_HEAD, menuSurfaceFor(Script(K.PROJECT, sceneKey = K.SCENE_1_7_3)))
        assertEquals(MenuSurface.STORY_LIST, menuSurfaceFor(StoryStructure(K.PROJECT)))
        assertEquals(MenuSurface.STORY_LIST, menuSurfaceFor(ProjectHome(K.PROJECT)))
        assertEquals(MenuSurface.STORY_LIST, menuSurfaceFor(null))
    }
}
