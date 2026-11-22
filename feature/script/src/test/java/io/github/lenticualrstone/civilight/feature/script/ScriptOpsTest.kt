package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.feature.script.state.Caret
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.InsertAt
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.SceneSplitSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The script's edit operations on the example project (SCR-12, SCR-15, SCR-18, SCR-19). */
class ScriptOpsTest {
    private val doc = Fx.example

    @Test
    fun `SCR-12 a dialogue block goes directly below the current block with the caret in it`() {
        val o = ScriptOps.insert(doc, InsertAt.After(K.BLOCK_043), BlockKind.DIALOGUE, null, Fx.ctx())
        val key = o.caret!!.blockKey
        val made = Fx.block(o.doc, key)
        val scene = o.doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }
        assertEquals(scene.indexOf(K.BLOCK_043) + 1, scene.indexOf(key))
        assertEquals(58, made.number)
        assertEquals(key, o.created)
        assertEquals(0, o.caret!!.offset)
        // SCR-18: the speaker of the nearest dialogue above (하르트), shown as `직전 화자`.
        assertEquals(K.HART, made.speakerKey)
        assertTrue(o.inherited)
    }

    @Test
    fun `SCR-12 a direction block is selected, not typed into`() {
        val o = ScriptOps.insert(doc, InsertAt.After(K.BLOCK_043), BlockKind.DIRECTION, null, Fx.ctx())
        assertNull(o.caret)
        assertNotNull(o.select)
        assertEquals(BlockKind.DIRECTION, Fx.block(o.doc, o.select!!).kind.known)
    }

    @Test
    fun `SCR-12 without a current block the block goes to the end of the scene under the header`() {
        assertEquals(InsertAt.After(K.BLOCK_043), ScriptOps.insertTarget(doc, K.BLOCK_043, K.SCENE_1_7_1))
        assertEquals(InsertAt.SceneEnd(K.SCENE_1_7_3), ScriptOps.insertTarget(doc, null, K.SCENE_1_7_3))
        assertNull(ScriptOps.insertTarget(doc, null, null))
    }

    @Test
    fun `SCR-18 a picked speaker is used instead of the inherited one`() {
        val o = ScriptOps.insert(doc, InsertAt.After(K.BLOCK_043), BlockKind.DIALOGUE, K.SERA, Fx.ctx())
        assertEquals(K.SERA, Fx.block(o.doc, o.caret!!.blockKey).speakerKey)
        assertFalse(o.inherited)
    }

    @Test
    fun `SCR-15 Enter at the end of a dialogue line makes a new line with the same speaker`() {
        val end = ScriptOps.endOf(doc, K.BLOCK_043)
        val o = ScriptOps.enter(doc, K.BLOCK_043, end, Fx.ctx())!!
        val made = Fx.block(o.doc, o.caret!!.blockKey)
        assertEquals(BlockKind.DIALOGUE, made.kind.known)
        assertEquals(K.HART, made.speakerKey)
        assertEquals(Caret(made.key, 0), o.caret)
        assertTrue(o.inherited)
    }

    @Test
    fun `T-12-09 SCR-15 Enter mid-line splits and the caret starts the back part`() {
        val o = ScriptOps.enter(doc, K.BLOCK_047, "뒤돌아보지 말고".length, Fx.ctx())!!
        assertEquals("뒤돌아보지 말고", Runs.displayText(Fx.block(o.doc, K.BLOCK_047).runs))
        val back = Fx.block(o.doc, o.caret!!.blockKey)
        assertEquals("따라와.", Runs.displayText(back.runs))
        assertEquals(58, back.number)
        assertEquals(o.caret!!.blockKey, o.created)
    }

    @Test
    fun `T-12-08 SCR-15 Backspace at the start merges into the previous block with its toast`() {
        val o = ScriptOps.backspaceAtStart(doc, K.BLOCK_043, Fx.ctx())!!
        assertNull(o.doc.index.block(K.BLOCK_043)?.takeIf { o.doc.manuscript.isLiveBlock(it.key) })
        val merged = Fx.block(o.doc, K.BLOCK_042)
        assertEquals(Caret(K.BLOCK_042, o.caret!!.offset), o.caret)
        assertTrue(Runs.displayText(merged.runs).endsWith("멈추지 마. 저건 등불이 아니야."))
        // The example anchors three `◆` on #043 (spec F1 has one: `◆ 1개 #042로`).
        assertEquals("◆ 3개 #042로", o.toast)
    }

    @Test
    fun `T-22-07 SCR-15 Backspace at the start of a scene's first block does nothing`() {
        assertNull(ScriptOps.backspaceAtStart(doc, K.BLOCK_036, Fx.ctx()))
    }

    @Test
    fun `SCR-15 Backspace in an empty block removes it with no toast and the caret ends the previous block`() {
        val added = ScriptOps.insert(doc, InsertAt.After(K.BLOCK_048), BlockKind.NARRATION, null, Fx.ctx())
        val empty = added.caret!!.blockKey
        val o = ScriptOps.backspaceAtStart(added.doc, empty, Fx.ctx())!!
        assertNull(o.doc.index.block(empty))
        assertNull(o.toast)
        assertEquals(Caret(K.BLOCK_048, ScriptOps.endOf(o.doc, K.BLOCK_048)), o.caret)
        assertTrue(o.doc.trash.entries.none { empty in it.targetKeys })
    }

    @Test
    fun `typing replaces the runs and leaves an unchanged block alone`() {
        val runs = listOf(TextRun.Plain("멈춰."))
        val d = ScriptOps.setRuns(doc, K.BLOCK_043, runs, Fx.ctx())
        assertEquals(runs, Fx.block(d, K.BLOCK_043).runs)
        val same = Fx.block(doc, K.BLOCK_043).runs
        assertTrue(ScriptOps.setRuns(doc, K.BLOCK_043, same, Fx.ctx()) === doc)
    }

    @Test
    fun `SCR-19 the speaker target is the current dialogue block, else the next dialogue insert`() {
        val onDialogue = ScriptOps.speakerTarget(doc, EditorState(selected = K.BLOCK_043))
        assertEquals(K.BLOCK_043, onDialogue.blockKey)
        assertEquals(K.HART, onDialogue.speakerKey)
        val onNarration = ScriptOps.speakerTarget(doc, EditorState(selected = K.BLOCK_044))
        assertNull(onNarration.blockKey)
        assertEquals(K.HART, onNarration.speakerKey)
        val held = ScriptOps.speakerTarget(doc, EditorState(selected = K.BLOCK_044, pendingSpeaker = K.SERA))
        assertEquals(K.SERA, held.speakerKey)
    }

    @Test
    fun `SCR-19 this scene's cast lists speakers then silent cast`() {
        val cast = ScriptOps.sceneCast(doc, K.SCENE_1_7_3)
        assertEquals(listOf(K.SERA, K.OWEN), cast.take(2))
        assertTrue(K.MARTHA in cast)
        assertEquals(cast.distinct(), cast)
    }

    @Test
    fun `SCR-19 recent speakers put the newest first without repeats`() {
        assertEquals(listOf(K.SERA, K.HART, K.OWEN), ScriptOps.recentSpeakers(listOf(K.HART, K.SERA, K.OWEN), K.SERA))
    }

    @Test
    fun `SCR-12 새 씬 routes to a split mid-scene and to the next scene at its end`() {
        val p = doc.project.key
        assertEquals(SceneSplitSheet(p, K.BLOCK_043), ScriptOps.newSceneSheet(doc, p, K.BLOCK_043, K.SCENE_1_7_3, K.EPISODE_1_7))
        assertEquals(
            NewUnitSheet(p, parentKey = K.EPISODE_1_7, afterKey = K.SCENE_1_7_3),
            ScriptOps.newSceneSheet(doc, p, K.BLOCK_048, K.SCENE_1_7_3, K.EPISODE_1_7),
        )
    }

    @Test
    fun `the kind of the writing position`() {
        assertEquals(BlockKind.DIALOGUE, ScriptOps.currentKind(doc, EditorState(selected = K.BLOCK_043)))
        assertEquals(BlockKind.NARRATION, ScriptOps.currentKind(doc, EditorState(selected = K.BLOCK_044)))
        assertEquals(BlockKind.DIALOGUE, ScriptOps.currentKind(doc, EditorState(virtualScene = K.SCENE_1_7_4, editing = true)))
        assertNull(ScriptOps.currentKind(doc, EditorState()))
    }
}
