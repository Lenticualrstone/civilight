package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.WritingCounts
import io.github.lenticualrstone.civilight.feature.script.panel.ModCard
import io.github.lenticualrstone.civilight.feature.script.panel.ModCardId
import io.github.lenticualrstone.civilight.feature.script.panel.ModsTab
import io.github.lenticualrstone.civilight.feature.script.panel.PanelBuild
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.settings.ScriptFieldsModel
import io.github.lenticualrstone.civilight.feature.script.settings.TransitionSettingsModel
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.TransitionTemplate
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The `모듈` tab (SCR-35) and the `전환 문구` setting (SCR-48) on the example project. */
class ModsAndTransitionTest {
    private val doc = Fx.example
    private val today = LocalDate.of(2026, 11, 22)

    private fun mods(d: ProjectDoc = doc, selected: String? = null, header: String? = K.SCENE_1_7_3, line: String? = null) =
        ModsTab.of(d, selected, header, today, Fx.zone, readOnly = false, unlocked = true) { line }

    @Test
    fun `SCR-35 the cards of the scene under the header, in the drawn order`() {
        val model = mods(line = "레코더 · 정리 안 됨 1 ›")
        assertEquals(K.SCENE_1_7_3, model.sceneKey)
        assertEquals(listOf(ModCardId.TODAY, ModCardId.SCENE, ModCardId.CHECKS, ModCardId.MEMO), model.order)
        val todayCard = model.cards[0] as ModCard.Today
        assertEquals(WritingCounts.todayCard(doc, today, Fx.zone, 2000).written, todayCard.written)
        assertEquals("목표 2,000자", todayCard.goal)
        assertEquals(WritingCounts.sceneSpeakers(doc, K.SCENE_1_7_3), (model.cards[1] as ModCard.SceneCount).text)
        val memo = model.cards[3] as ModCard.Memo
        assertEquals("등불이 두 번째로 깜박이는 타이밍", memo.text)
        assertEquals("레코더 · 정리 안 됨 1 ›", memo.recorderLine)
        // A selected block's scene wins over the header's.
        assertEquals(K.SCENE_1_7_1, mods(selected = doc.manuscript.liveBlocksOf(K.SCENE_1_7_1).first().key).sceneKey)
        // Without a scene only `오늘` is left.
        assertEquals(listOf(ModCardId.TODAY), mods(header = null).order)
    }

    @Test
    fun `SCR-35 the stored card order wins, unknown ids drop out, missing cards follow`() {
        assertEquals(
            listOf(ModCardId.MEMO, ModCardId.TODAY, ModCardId.SCENE, ModCardId.CHECKS),
            ModCardId.order(listOf("memo", "bogus", "today", "memo")),
        )
        val stored = doc.copy(app = doc.app.copy(view = doc.app.view.copy(script = doc.app.view.script.copy(moduleCardOrder = listOf("checks")))))
        assertEquals(ModCardId.CHECKS, mods(stored).order.first())
    }

    @Test
    fun `SCR-35 점검 counts this scene's 화자 없음 lines, and reads nothing when there is none`() {
        val clean = ModsTab.checksLine(doc, K.SCENE_1_7_3)
        // #048 (리엔) loses its speaker: a non-empty dialogue line without one.
        val added = doc.copy(episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == K.BLOCK_048) it.copy(speakerKey = null) else it }) })
        val line = ModsTab.checksLine(added, K.SCENE_1_7_3)!!
        assertTrue(line.startsWith("▲ "))
        assertTrue(line.endsWith("화자 없음 1"))
        if (clean != null) assertTrue(clean.startsWith("▲ 공개 전 사용 "))
        // Another scene does not count it.
        assertEquals(ModsTab.checksLine(doc, K.SCENE_1_7_1), ModsTab.checksLine(added, K.SCENE_1_7_1))
    }

    @Test
    fun `SCR-35 the panel builds 모듈 from the header scene with no block selected`() {
        val content = PanelBuild.of(doc, PanelTab.MODS, null, K.SCENE_1_7_3, StoryTimes.source(doc), ScriptLayers(), true, false, Fx.zone, today)
        assertEquals(K.SCENE_1_7_3, content.mods!!.sceneKey)
        assertNull(content.props)
    }

    @Test
    fun `SCR-48 the setting row reads the default and the template, and the screen offers both`() {
        val ui = TransitionSettingsModel.of(doc, readOnly = false)
        assertEquals("새 씬 기본 없음 · 틀 장소 · 시각", ui.rowSub)
        assertEquals("새 씬 기본 없음 · 틀 장소 · 시각", ScriptFieldsModel.of(doc, false).transitionSub)
        assertEquals(listOf("없음", "자동"), ui.modes.map { it.title })
        assertEquals(listOf(true, false), ui.modes.map { it.selected })
        assertEquals(listOf("장소 · 시각", "장소만"), ui.templates.map { it.title })
        val timeSample = ui.templates[0].sample!!
        val placeSample = ui.templates[1].sample!!
        assertTrue(timeSample.startsWith("$placeSample · "))

        val auto = TransitionSettingsModel.withDefault(TransitionSettingsModel.withTemplate(doc, TransitionTemplate.PLACE_ONLY), TransitionMode.AUTO)
        assertEquals("새 씬 기본 자동 · 틀 장소만", TransitionSettingsModel.of(auto, false).rowSub)
        // T-22-03 (template 장소만): the 자동 text drops the time.
        assertEquals(placeSample, TransitionSettingsModel.sample(auto, TransitionTemplate.PLACE_ONLY))
    }
}
