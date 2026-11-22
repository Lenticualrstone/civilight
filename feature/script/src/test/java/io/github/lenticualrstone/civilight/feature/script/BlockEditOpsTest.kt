package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.feature.script.state.CueEditor
import io.github.lenticualrstone.civilight.feature.script.state.EditorRules
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.InsertAt
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-08 cues, SCR-09 option lines, SCR-13 kind change and SCR-38 delete, on the example project. */
class BlockEditOpsTest {
    private val doc = Fx.example

    // --- SCR-38 ---

    @Test
    fun `T-12-01 deleting #043 trashes it, relocates its records to #044 and says so`() {
        val o = ScriptOps.deleteBlocks(doc, listOf(K.BLOCK_043), Fx.ctx())!!
        assertFalse(o.doc.manuscript.isLiveBlock(K.BLOCK_043))
        val block = Fx.block(o.doc, K.BLOCK_043)
        assertNotNull(block.deletedAt)
        assertEquals(K.BLOCK_042, block.trashPlacement?.prevKey)
        assertEquals(K.BLOCK_044, block.trashPlacement?.nextKey)
        assertTrue(o.doc.trash.entries.any { it.kind.known == TrashKind.BLOCK_GROUP && K.BLOCK_043 in it.targetKeys })
        // The example anchors three ◆ on #043 (fixture; spec F1 has one).
        assertEquals("블록 #043 지움 · ◆ 3개는 #044 자리로", o.toast)
    }

    @Test
    fun `SCR-38 an empty block is removed with no toast and no trash`() {
        val added = ScriptOps.insert(doc, InsertAt.After(K.BLOCK_043), BlockKind.NARRATION, null, Fx.ctx())
        val key = added.caret!!.blockKey
        val o = ScriptOps.deleteBlocks(added.doc, listOf(key), Fx.ctx())!!
        assertNull(o.doc.index.block(key))
        assertNull(o.toast)
        assertEquals(added.doc.trash.entries, o.doc.trash.entries)
    }

    // --- SCR-13 ---

    @Test
    fun `SCR-13 kind change keeps key and number, text kinds convert freely`() {
        val o = ScriptOps.changeKind(doc, K.BLOCK_043, BlockKind.NARRATION, Fx.ctx())!!
        val b = Fx.block(o.doc, K.BLOCK_043)
        assertEquals(BlockKind.NARRATION, b.kind.known)
        assertEquals(43, b.number)
        assertNull(b.speakerKey)
        assertEquals(Fx.block(doc, K.BLOCK_043).runs, b.runs)
        assertEquals(K.BLOCK_043, o.select)
        // Records stay ("기록은 내용에 붙지 종류에 붙지 않는다").
        assertEquals(doc.changes.records.count { (it.anchor as? io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock)?.blockKey == K.BLOCK_043 },
            o.doc.changes.records.count { (it.anchor as? io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock)?.blockKey == K.BLOCK_043 })
    }

    @Test
    fun `Q-12 the sheet dims direction and choice for a block with content, and marks the current kind`() {
        val choices = ScriptOps.kindChoices(doc, K.BLOCK_043).associateBy { it.row.kind }
        assertEquals(6, choices.size)
        assertTrue(choices.getValue(BlockKind.DIALOGUE).current)
        assertTrue(choices.getValue(BlockKind.NARRATION).enabled)
        assertTrue(choices.getValue(BlockKind.MEMO).enabled)
        assertFalse(choices.getValue(BlockKind.DIRECTION).enabled)
        assertFalse(choices.getValue(BlockKind.CHOICE).enabled)
        assertNull(ScriptOps.changeKind(doc, K.BLOCK_043, BlockKind.CHOICE, Fx.ctx()))
    }

    // --- SCR-09 ---

    @Test
    fun `SCR-09 a new choice block puts the caret in its first option`() {
        val o = ScriptOps.insert(doc, InsertAt.After(K.BLOCK_043), BlockKind.CHOICE, null, Fx.ctx())
        val block = Fx.block(o.doc, o.created!!)
        val options = BlockOps.orderedOptions(block)
        assertEquals(2, options.size)
        assertEquals(options.first().key, o.caret!!.optionKey)
        assertEquals(0, o.caret!!.offset)
    }

    @Test
    fun `SCR-09 Enter in an option adds the next one, moving the text after the caret`() {
        val block = Fx.block(doc, K.BLOCK_1_5_CHOICE)
        val first = BlockOps.orderedOptions(block).first()
        val text = first.runs.plainText
        val o = ScriptOps.enterOption(doc, block.key, first.key, 2, Fx.ctx())!!
        val options = BlockOps.orderedOptions(Fx.block(o.doc, block.key))
        assertEquals(3, options.size)
        assertEquals(text.take(2), options[0].runs.plainText)
        assertEquals(text.drop(2), options[1].runs.plainText)
        assertEquals(options[1].key, o.caret!!.optionKey)
    }

    @Test
    fun `SCR-09 Backspace removes an empty option while two remain, and an all-empty choice block goes`() {
        val block = Fx.block(doc, K.BLOCK_1_5_CHOICE)
        val first = BlockOps.orderedOptions(block).first()
        val withNew = ScriptOps.enterOption(doc, block.key, first.key, Runs.length(first.runs), Fx.ctx())!!
        val empty = withNew.caret!!.optionKey!!
        val removed = ScriptOps.backspaceOption(withNew.doc, block.key, empty, Fx.ctx())!!
        assertEquals(2, Fx.block(removed.doc, block.key).options.size)
        assertEquals(first.key, removed.caret!!.optionKey)
        assertEquals(Runs.length(first.runs), removed.caret!!.offset)
        // Two options left: a non-empty option is not removed.
        assertNull(ScriptOps.backspaceOption(removed.doc, block.key, first.key, Fx.ctx()))

        val fresh = ScriptOps.insert(doc, InsertAt.After(K.BLOCK_043), BlockKind.CHOICE, null, Fx.ctx())
        val gone = ScriptOps.backspaceOption(fresh.doc, fresh.created!!, fresh.caret!!.optionKey!!, Fx.ctx())!!
        assertNull(gone.doc.index.block(fresh.created!!))
        assertEquals(K.BLOCK_043, gone.caret?.blockKey)
    }

    @Test
    fun `SCR-09 typing in an option writes that option only`() {
        val block = Fx.block(doc, K.BLOCK_1_5_CHOICE)
        val second = BlockOps.orderedOptions(block)[1]
        val out = ScriptOps.setOptionRuns(doc, block.key, second.key, listOf(TextRun.Plain("돌아간다")), Fx.ctx())
        val after = BlockOps.orderedOptions(Fx.block(out, block.key))
        assertEquals("돌아간다", after[1].runs.plainText)
        assertEquals(BlockOps.orderedOptions(block)[0].runs, after[0].runs)
    }

    @Test
    fun `an option removed elsewhere leaves the choice block selected without a caret`() {
        val state = EditorState(selected = K.BLOCK_1_5_CHOICE, editing = true, option = "gone")
        val next = EditorRules.reconcile(state, doc)
        assertEquals(K.BLOCK_1_5_CHOICE, next.selected)
        assertFalse(next.editing)
        assertNull(next.option)
    }

    // --- SCR-08 ---

    @Test
    fun `SCR-08 the cue sheet lists cues, kind chips and the assets of a kind`() {
        val editor = CueEditor.of(doc, K.BLOCK_041)!!
        assertEquals("연출 지시", editor.title)
        assertEquals("#041", editor.number)
        assertEquals(listOf("▸ 배경 하층 거리(새벽)", "♪ 잔불"), editor.cues.map { it.line })
        assertEquals(listOf("배경", "BGM", "효과음", "CG", "화면 효과"), editor.kinds.map { it.second })
        val backgrounds = CueEditor.picks(doc, CueKind.BACKGROUND, K.EPISODE_1_7)
        assertTrue(backgrounds.all { it.assetKey != null && it.effect == null })
        assertEquals(listOf("페이드", "암전"), CueEditor.picks(doc, CueKind.SCREEN_EFFECT).map { it.title })
        assertNull(CueEditor.of(doc, K.BLOCK_043))
    }

    @Test
    fun `SCR-08 taking out the last cue removes the empty direction block`() {
        val added = ScriptOps.insert(doc, InsertAt.After(K.BLOCK_043), BlockKind.DIRECTION, null, Fx.ctx())
        val key = added.select!!
        val withCue = ScriptOps.addCue(added.doc, key, CueKind.SCREEN_EFFECT, null, "fade", Fx.ctx())!!
        val cue = Fx.block(withCue.doc, key).cues.single()
        assertEquals("화면 효과 페이드", CueEditor.of(withCue.doc, key)!!.cues.single().line)
        val removed = ScriptOps.removeCue(withCue.doc, key, cue.key, Fx.ctx())!!
        assertNull(removed.doc.index.block(key))
        assertNull(removed.toast)
        // A cue that does not fit changes nothing.
        assertNull(ScriptOps.addCue(added.doc, key, CueKind.BGM, K.ASSET_BG_LOWER_STREET_DAWN, null, Fx.ctx()))
    }
}
