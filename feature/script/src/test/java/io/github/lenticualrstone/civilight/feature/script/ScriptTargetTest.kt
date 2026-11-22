package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.state.OpenTarget
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
        assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_4, null, null), ScriptTarget.resolve(doc, K.SCENE_1_7_4, null, false))
    }

    @Test
    fun `a gone block falls back to its scene, then to the last writing position`() {
        val merged = ScriptOps.backspaceAtStart(doc, K.BLOCK_043, Fx.ctx())!!.doc
        assertEquals(OpenTarget(K.EPISODE_1_7, K.SCENE_1_7_3, null, null), ScriptTarget.resolve(merged, K.SCENE_1_7_3, K.BLOCK_043, false))
        val fallback = ScriptTarget.resolve(merged, null, K.BLOCK_043, false)
        assertEquals(ScriptTarget.unitOf(merged, fallback.sceneKey!!), fallback.unitKey)
    }
}
