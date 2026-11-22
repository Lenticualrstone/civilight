package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.state.EditorRules
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-14: where the writer is, and how it follows the document. */
class EditorRulesTest {
    @Test
    fun `SCR-14 tapping text gives the caret, 완료 keeps the block selected`() {
        val editing = EditorRules.edit(EditorState(), K.BLOCK_043)
        assertEquals(K.BLOCK_043, editing.caretBlock)
        assertEquals(ScriptRowKeys.block(K.BLOCK_043), editing.caretRowKey)
        val done = EditorRules.stopEditing(editing)
        assertNull(done.caretBlock)
        assertNull(done.caretRowKey)
        assertEquals(K.BLOCK_043, done.selected)
        assertTrue(done.hasPosition)
    }

    @Test
    fun `the virtual first line of an empty scene holds the caret without a block`() {
        val s = EditorRules.editVirtual(EditorState(selected = K.BLOCK_043), K.SCENE_1_7_4)
        assertNull(s.selected)
        assertNull(s.caretBlock)
        assertEquals(ScriptRowKeys.empty(K.SCENE_1_7_4), s.caretRowKey)
        assertTrue(s.hasPosition)
    }

    @Test
    fun `a selection whose block left the manuscript is dropped`() {
        val doc = Fx.example
        val state = EditorState(selected = K.BLOCK_043, editing = true, inheritedHint = K.BLOCK_043, pendingSpeaker = "gone")
        // #043 merged into #042 (SCR-15): it is no longer a live block.
        val merged = ScriptOps.backspaceAtStart(doc, K.BLOCK_043, Fx.ctx())!!.doc
        val after = EditorRules.reconcile(state, merged)
        assertNull(after.selected)
        assertFalse(after.editing)
        assertNull(after.inheritedHint)
        assertNull(after.pendingSpeaker)
        assertEquals(state.copy(pendingSpeaker = null), EditorRules.reconcile(state, doc))
    }
}
