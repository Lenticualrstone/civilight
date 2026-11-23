package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.feature.story.Fx.node
import io.github.lenticualrstone.civilight.feature.story.common.StoryActions
import io.github.lenticualrstone.civilight.feature.story.Fx.withUnit
import io.github.lenticualrstone.civilight.feature.story.home.HomeModel
import io.github.lenticualrstone.civilight.feature.story.home.HomeShortcut
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.CanvasBoard
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.PageList
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.navigation.ProjectSettings
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.Timeline
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 프로젝트 홈 (STO-01, STO-06, STO-13 chip; 20-story-time §4.14 STR-05). */
class HomeModelTest {
    private val doc = Fx.doc()
    private fun home(d: io.github.lenticualrstone.civilight.model.doc.ProjectDoc = doc, locked: Boolean = true, readOnly: Boolean = true) =
        HomeModel.build(d, readOnly, locked, Fx.today, Fx.zone)

    /** U-06: the head names the project; S-1 / S-4: home has no lens chip and no rating badge to build. */
    @Test
    fun headAndStatsOfTheExample() {
        val ui = home()
        assertEquals("잿빛 새벽의 등불", ui.name)
        assertEquals(listOf("7/10" to "1장 에피소드", "0" to "오늘 쓴 글자", "11" to "인물"), ui.stats.map { it.value to it.label })
        assertEquals("명령 또는 검색 · 인물 · 용어 · 대사", ui.commandHint)
        assertTrue(ui.readOnly)
    }

    /** S-1: a stored past lens changes nothing on home (no chip; the store opens a project at 최신, D9). */
    @Test
    fun aStoredPastLensLeavesHomeAsIs() {
        val d = doc.copy(app = doc.app.copy(view = doc.app.view.copy(lens = LensState(point = LensPoint.UnitEnd(K.EPISODE_1_5)))))
        assertEquals(home(), home(d))
    }

    @Test
    fun resumeOpensTheBlockItNames() {
        val resume = home().resume!!
        assertEquals("1-7 씬 3 · 꺼진 등불", resume.where)
        assertEquals("“바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 오리지늄 등불이 한 번 깜박였다.”", resume.quote)
        assertEquals(K.BLOCK_044, resume.blockKey)
    }

    @Test
    fun resumeSkipsALockedHiddenSceneAndComesBackWhenUnlocked() {
        val hidden = doc.withUnit(K.SCENE_1_7_3) { it.copy(hidden = true) }
        assertFalse(home(hidden, locked = true).resume!!.where.startsWith("1-7 씬 3"))
        assertEquals(K.BLOCK_044, home(hidden, locked = false).resume!!.blockKey)
    }

    @Test
    fun emptyProjectHasNoResume() {
        val empty = doc.copy(episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = emptyList()) })
        assertNull(home(empty).resume)
    }

    @Test
    fun str05_currentChapterRows() {
        val ui = home()
        assertEquals("Ⅰ 1장 · 등불지기", ui.listHeader)
        val lines = ui.rows.associate { it.code to "${it.code} | ${it.title} | ${it.status}" }
        assertEquals("1-7 | 잿빛 새벽의 등불 | 쓰는 중 · 씬 3", lines["1-7"])
        assertEquals("1-8 | — | 미정", lines["1-8"])
        assertEquals("1-5 | 갈림길 | 완료", lines["1-5"])
        assertEquals(10, ui.rows.size)
        val current = ui.rows.single { it.current }
        assertEquals(K.EPISODE_1_7, current.key)
        assertTrue(current.writing)
        assertFalse(ui.rows.single { it.code == "1-5" }.writing)
    }

    @Test
    fun shortcutsInBoardOrderAndTheirRoutes() {
        val ui = home()
        assertEquals(listOf("스토리", "할 일 · 복선", "인물", "세계", "연표", "캔버스", "갤러리", "페이지", "점검 센터", "설정"), ui.shortcuts.map { it.label })
        assertTrue(HomeShortcut.SETTINGS.dim)
        val p = K.PROJECT
        assertEquals(
            listOf(
                StoryStructure(p), null, CastList(p), World(p), Timeline(p), CanvasBoard(p),
                Gallery.ofProject(p), PageList(p), CheckCenter(p), ProjectSettings(p),
            ),
            HomeShortcut.entries.map { it.route(p) },
        )
    }

    @Test
    fun settingsKeepsItsOwnLastRow_GAL03() {
        val rows = HomeModel.shortcutRows(HomeModel.shortcuts(doc))
        assertEquals(listOf(3, 3, 3, 1), rows.map { it.size })
        assertEquals(listOf(HomeShortcut.SETTINGS), rows.last())
        assertTrue(rows.dropLast(1).all { HomeShortcut.SETTINGS !in it })
        val none = doc.copy(project = doc.project.copy(worldTime = doc.project.worldTime.copy(mode = WorldTimeMode.NONE.open())))
        val noTime = HomeModel.shortcutRows(HomeModel.shortcuts(none))
        assertEquals(listOf(3, 3, 2, 1), noTime.map { it.size })
        assertEquals(listOf(HomeShortcut.SETTINGS), noTime.last())
    }

    @Test
    fun twoLevelProjectListsEveryEpisodeWithoutHeader() {
        val d = Fx.mini(
            listOf("막", "장면"),
            node("A1", "첫 막", null, node("S1", "들판", "바람이 불었다."), node("S2")),
            node("A2", null, null, node("S3")),
        )
        val ui = home(d)
        assertNull(ui.listHeader)
        assertEquals(listOf("1막", "2막"), ui.rows.map { it.code })
        assertEquals("—", ui.rows[1].title)
        assertEquals("1/2" to "막", ui.stats.first().let { it.value to it.label })
        assertEquals("1막 장면 1 · 들판", ui.resume!!.where)
        // The empty `2막` marked `완료` counts and reads `완료` (STO-06 `ED`, emulator round 2).
        val marked = home(StoryActions.setDone(d, "A2", true, Fx.ctx()))
        assertEquals("2/2", marked.stats.first().value)
        assertEquals("완료", marked.rows[1].status)
    }

    @Test
    fun oneLevelProjectListsItsScenes() {
        val d = Fx.mini(listOf("씬"), node("S1", "첫 장", "바람이 불었다."), node("S2"))
        val ui = home(d)
        assertNull(ui.listHeader)
        assertEquals(listOf("씬 1" to "첫 장", "씬 2" to "—"), ui.rows.map { it.code to it.title })
        assertEquals(listOf("쓰는 중", "미정"), ui.rows.map { it.status })
        assertEquals("1/2" to "씬", ui.stats.first().let { it.value to it.label })
    }

    @Test
    fun fourLevelProjectUsesTheTopLevelAsChapter() {
        val d = Fx.mini(
            listOf("부", "장", "화", "씬"),
            node("P1", "첫 부", null, node("C1", "첫 장", null, node("E1", "첫 화", null, node("S1", null, "글")), node("E2"))),
        )
        val ui = home(d)
        assertEquals("Ⅰ 1부 · 첫 부", ui.listHeader)
        assertEquals(listOf("1-1-1", "1-1-2"), ui.rows.map { it.code })
        assertEquals("1/2" to "1부 화", ui.stats.first().let { it.value to it.label })
    }
}
