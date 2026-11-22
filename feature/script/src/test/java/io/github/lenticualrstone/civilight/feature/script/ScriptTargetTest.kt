package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.feature.script.state.InsertAt
import io.github.lenticualrstone.civilight.feature.script.state.OpenTarget
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.feature.script.state.ScriptTarget
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-01 entry: where the script opens. */
class ScriptTargetTest {
    private val doc = Fx.example

    @Test
    fun `SCR-01 a block target is selected in its episode, with the caret at its end for 이어 쓰기`() {
        assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_3, K.BLOCK_043, null), ScriptTarget.resolve(doc, null, K.BLOCK_043, caretAtEnd = false))
        val end = Fx.block(doc, K.BLOCK_048).runs.plainText.length
        assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_3, K.BLOCK_048, end), ScriptTarget.resolve(doc, null, K.BLOCK_048, caretAtEnd = true))
    }

    @Test
    fun `SCR-01 a scene target opens at its head`() {
        assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_3, null, null), ScriptTarget.resolve(doc, K.SCENE_1_7_3, null, false))
    }

    @Test
    fun `a gone block falls back to its scene, then to the last writing position`() {
        val merged = ScriptOps.backspaceAtStart(doc, K.BLOCK_043, Fx.ctx())!!.doc
        assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_3, null, null), ScriptTarget.resolve(merged, K.SCENE_1_7_3, K.BLOCK_043, false))
        val fallback = ScriptTarget.resolve(merged, null, K.BLOCK_043, false)
        assertEquals(ScriptTarget.unitOf(merged, fallback.sceneKey!!), fallback.unitKey)
    }

    @Test
    fun `SCR-01 an empty scene opens with the caret in its first line`() {
        // Fixture: 1-7 씬 4 has no blocks.
        assertEquals(
            OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_4, null, null, emptyScene = K.SCENE_1_7_4),
            ScriptTarget.resolve(doc, K.SCENE_1_7_4, null, caretAtEnd = false),
        )
        // 이어 쓰기 with no block to name lands there too.
        assertEquals(K.SCENE_1_7_4, ScriptTarget.resolve(doc, K.SCENE_1_7_4, null, caretAtEnd = true).emptyScene)
    }

    @Test
    fun `SCR-01 a scene holding only an empty first line opens with the caret in it`() {
        // Fixture: 1-8 씬 1 holds one empty block #001.
        val line = doc.manuscript.liveBlocksOf(K.SCENE_1_8_1).single()
        assertEquals(OpenTarget(K.EPISODE_1_8, K.SCENE_1_8_1, line.key, 0), ScriptTarget.resolve(doc, K.SCENE_1_8_1, null, false))
        // A new scene's first line (SCR-49) behaves the same.
        val d = ScriptOps.insert(doc, InsertAt.SceneStart(K.SCENE_1_7_4), BlockKind.DIALOGUE, null, Fx.ctx()).doc
        val made = d.manuscript.liveBlocksOf(K.SCENE_1_7_4).single()
        assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_4, made.key, 0), ScriptTarget.resolve(d, K.SCENE_1_7_4, null, false))
    }

    @Test
    fun `SCR-01 caretAtEnd on a scene puts the caret at the end of its last text block`() {
        val last = doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).last()
        val t = ScriptTarget.resolve(doc, K.SCENE_1_7_3, null, caretAtEnd = true)
        if (last.kind.known in BlockOps.TEXT_KINDS) {
            assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_3, last.key, last.runs.plainText.length), t)
        } else {
            assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_3, null, null), t)
        }
    }
}
