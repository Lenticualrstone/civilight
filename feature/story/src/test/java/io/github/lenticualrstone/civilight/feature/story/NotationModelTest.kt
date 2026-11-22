package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.feature.story.common.StoryActions
import io.github.lenticualrstone.civilight.feature.story.settings.NotationModel
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `때 표기` (STO-17): two choices, each with the sample scene's label under it (TIME-06). */
class NotationModelTest {
    @Test
    fun bothChoicesShowTheWritingLineSceneUnderThem() {
        val ui = NotationModel.build(Fx.doc(), readOnly = false)
        assertEquals(listOf("에피소드 기준", "이야기 시작부터"), ui.options.map { it.label })
        assertEquals(listOf(true, false), ui.options.map { it.selected })
        // 1-7 씬 3 (the 집필 선 scene, S9): TIME-06 and the story-relative form.
        assertEquals(listOf("1-7 · 2일째 · 새벽", "1-7 · 약 13일째 · 새벽"), ui.options.map { it.sample })
    }

    @Test
    fun theStoredChoiceIsSelected() {
        val d = StoryActions.setNotation(Fx.doc(), SceneNotation.STORY_START, Fx.ctx())
        val ui = NotationModel.build(d, readOnly = true)
        assertEquals(listOf(false, true), ui.options.map { it.selected })
        assertTrue(ui.readOnly)
        assertEquals("1-7 · 2일째 · 새벽", ui.options[0].sample)
    }

    @Test
    fun aProjectWithoutTimedScenesShowsNoSample() {
        val ui = NotationModel.build(Fx.mini(listOf("장", "에피소드", "씬"), Fx.node("C1", null, null, Fx.node("E1", null, null, Fx.node("S1", text = "첫 줄")))), readOnly = false)
        assertEquals(listOf(null, null), ui.options.map { it.sample })
    }
}
