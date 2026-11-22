package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.EmptySceneRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.HeaderModel
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerLook
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-01, SCR-04 … SCR-06, SCR-20: the rows the manuscript draws for episode 1-7. */
class ScriptRowsTest {
    private val doc = Fx.example
    private val time = StoryTimes.source(doc)

    private fun rows(readOnly: Boolean = false, layers: ScriptLayers = ScriptLayers()): List<ScriptRow> =
        ScriptRows.build(doc, K.EPISODE_1_7, time, layers, ScriptPreview.WRITE, unlocked = false, readOnly = readOnly, zone = Fx.zone)

    private fun block(key: String) = rows().filterIsInstance<BlockRow>().single { it.blockKey == key }

    @Test
    fun `SCR-01 every scene starts with its head, then its blocks in order`() {
        val r = rows()
        val heads = r.filterIsInstance<HeadRow>()
        assertEquals(listOf(K.SCENE_1_7_1, K.SCENE_1_7_2, K.SCENE_1_7_3, K.SCENE_1_7_4), heads.map { it.sceneKey })
        assertEquals((1..4).toList(), heads.map { it.ordinal })
        val s3 = r.dropWhile { it.sceneKey != K.SCENE_1_7_3 }.takeWhile { it.sceneKey == K.SCENE_1_7_3 }
        assertTrue(s3.first() is HeadRow)
        assertEquals((36..48).map { "#0$it" }, s3.filterIsInstance<BlockRow>().map { it.number })
        assertEquals(r.map { it.key }.distinct(), r.map { it.key })
    }

    @Test
    fun `SCR-01 an empty scene gets one virtual dialogue line with the inherited speaker`() {
        val empty = rows().filterIsInstance<EmptySceneRow>().single()
        assertEquals(K.SCENE_1_7_4, empty.sceneKey)
        assertEquals("리엔", empty.speaker?.text)
        assertTrue(rows(readOnly = true).none { it is EmptySceneRow })
    }

    @Test
    fun `SCR-05 a dialogue row has its speaker and runs with the paren run kept`() {
        val r = block(K.BLOCK_042)
        assertEquals(BlockKind.DIALOGUE, r.kind)
        assertEquals("리엔", r.speaker?.text)
        assertTrue(r.runs.first() is TextRun.Paren)
        assertTrue(r.isText)
    }

    @Test
    fun `SCR-20 a display name differing at the block shows the real name after it`() {
        val r = block(K.BLOCK_043)
        assertEquals("???", r.speaker?.text)
        assertEquals("하르트", r.speaker?.realName)
        assertEquals(SpeakerLook.NAMED, r.speaker?.look)
        assertTrue(r.speakerMarks.any { it.glyph == '◆' })
    }

    @Test
    fun `SCR-20 a non-empty dialogue line without a speaker reads 화자 없음 with ▲`() {
        val r = rows().filterIsInstance<BlockRow>().single { it.blockKey == K.BLOCK_1_7_027 }
        assertEquals("화자 없음", r.speaker?.text)
        assertEquals(SpeakerLook.MISSING, r.speaker?.look)
        assertTrue(r.speakerMarks.any { it.glyph == '▲' })
    }

    @Test
    fun `SCR-06 narration has no speaker line and carries its first-reveal mark at the end`() {
        val r = block(K.BLOCK_044)
        assertEquals(BlockKind.NARRATION, r.kind)
        assertNull(r.speaker)
        assertTrue(r.textMarks.any { it.glyph == '◇' })
        assertTrue(r.runs.any { it is TextRun.Link })
    }

    @Test
    fun `SCR-11 a memo row has the ⌁ prefix`() {
        assertEquals("⌁ 작가 메모", block(K.BLOCK_046).memoPrefix)
    }

    @Test
    fun `SCR-02 the header shows the scene of the selected block, or saved while typing`() {
        val selected = EditorState(selected = K.BLOCK_043)
        val reading = HeaderModel.of(doc, K.EPISODE_1_7, time, selected, sceneUnderBar = K.SCENE_1_7_1, editing = false, saved = true)
        assertEquals(K.SCENE_1_7_3, reading.sceneKey)
        assertEquals("1-7 · 씬 3 · 2일째 새벽", reading.line)
        assertEquals("이 씬 · 1-7", reading.positionLabel)
        val typing = HeaderModel.of(doc, K.EPISODE_1_7, time, selected.copy(editing = true), null, editing = true, saved = true)
        assertEquals("1-7 · 씬 3 · 저장됨", typing.line)
        val under = HeaderModel.of(doc, K.EPISODE_1_7, time, EditorState(), sceneUnderBar = K.SCENE_1_7_2, editing = false, saved = true)
        assertEquals(K.SCENE_1_7_2, under.sceneKey)
    }
}
