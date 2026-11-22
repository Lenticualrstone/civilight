package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.feature.script.panel.PropsModel
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.HeaderModel
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.feature.script.state.SlotModel
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-02 header, SCR-21 numbers, SCR-46 heads, SCR-08/09/10 rows, SCR-34 `속성` subset. */
class PanelAndRowsTest {
    private val doc = Fx.example
    private val time = StoryTimes.source(doc)

    private fun rows(unit: String = K.EPISODE_1_7, layers: ScriptLayers = ScriptLayers()) =
        ScriptRows.build(doc, unit, time, layers, ScriptPreview.WRITE, unlocked = false, readOnly = false, zone = Fx.zone)

    private fun block(key: String, unit: String = K.EPISODE_1_7) = rows(unit).filterIsInstance<BlockRow>().single { it.blockKey == key }

    @Test
    fun `T-11-03 the header follows the caret block, else the scene under the top bar`() {
        val caret = HeaderModel.of(doc, K.EPISODE_1_7, time, EditorState(selected = K.BLOCK_045), null, editing = false, saved = true)
        assertEquals("1-7 · 씬 3 · 2일째 새벽", caret.line)
        assertEquals("이 씬 · 1-7", caret.positionLabel)
        val under = HeaderModel.of(doc, K.EPISODE_1_7, time, EditorState(), sceneUnderBar = K.SCENE_1_7_4, editing = false, saved = true)
        assertEquals(K.SCENE_1_7_4, under.sceneKey)
        assertTrue(under.line.startsWith("1-7 · 씬 4 · 2일째 아침"))
    }

    @Test
    fun `T-22-01 and T-22-23 scene 4's head and its transition line, which the system layer hides`() {
        val head = rows().filterIsInstance<HeadRow>().single { it.sceneKey == K.SCENE_1_7_4 }
        assertEquals("씬 4 · 결사의 문 · 2일째 아침 06:10", head.title)
        assertEquals("전환 문구 · 결사의 문 앞 · 06:10 · 직접", head.transitionLine)
        assertEquals(4, head.ordinal)
        val hidden = rows(layers = ScriptLayers(system = false)).filterIsInstance<HeadRow>().single { it.sceneKey == K.SCENE_1_7_4 }
        assertEquals(head.title, hidden.title)
        assertNull(hidden.transitionLine)
    }

    @Test
    fun `SCR-21 the number shows on the selected block of every kind, never on unselected narration`() {
        assertEquals(SlotModel("나레이션", null, "#044"), block(K.BLOCK_044).slot(selected = true))
        assertNull(block(K.BLOCK_044).slot(selected = false))
        assertEquals(SlotModel("연출", null, "#041"), block(K.BLOCK_041).slot(selected = true))
        assertEquals(SlotModel("작가 메모", null, "#046"), block(K.BLOCK_046).slot(selected = true))
        assertNull(block(K.BLOCK_043).slot(selected = true))
        assertEquals(SlotModel("시스템", "화면 문구", null), ScriptRows.slotOf(BlockKind.SYSTEM, "#050", selected = false))
        // A direction block with no cue yet keeps its label, so the row can be found.
        assertEquals(SlotModel("연출", null, null), ScriptRows.slotOf(BlockKind.DIRECTION, "#058", selected = false, empty = true))
        assertNull(block(K.BLOCK_041).slot(selected = false))
    }

    @Test
    fun `T-12-16 choice options read #006-1 and #006-2 while selected and 1, 2 otherwise`() {
        val choice = block(K.BLOCK_1_5_CHOICE, K.EPISODE_1_5)
        assertEquals(SlotModel("선택지", "다음 블록에서 합류", "#006"), choice.slot(selected = true))
        assertEquals(listOf("1", "2"), choice.options.map { it.index })
        assertEquals(listOf("#006-1", "#006-2"), choice.options.map { it.number })
        assertEquals(listOf("시장 쪽으로 간다", "결사로 돌아간다"), choice.options.map { it.text })
    }

    @Test
    fun `SCR-08 a direction row reads its cues`() {
        assertEquals("▸ 배경 하층 거리(새벽) ♪ 잔불", block(K.BLOCK_041).text)
        assertFalse(block(K.BLOCK_041).isText)
    }

    @Test
    fun `SCR-34 the 속성 tab shows the selected block's number, kind and speaker`() {
        val props = PropsModel.of(doc, K.BLOCK_043, readOnly = false)
        assertEquals("고른 블록 · #043", props.title)
        assertEquals("대사", props.kind!!.value)
        assertTrue(props.kind!!.canChange)
        assertEquals("하르트", props.speaker!!.value)
        assertNull(PropsModel.of(doc, K.BLOCK_044, readOnly = false).speaker)
        assertFalse(PropsModel.of(doc, K.BLOCK_043, readOnly = true).kind!!.canChange)
        assertEquals("고른 블록 없음", PropsModel.of(doc, null, readOnly = false).title)
    }
}
