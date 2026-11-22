package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Edge cases of insert, Enter, Backspace, kind change, options, moves and undo (SCR-12…18, 37–40). */
class BlockOpsEdgeTest {
    private val ctx = testCtx()
    private fun b(n: Int) = F1.b(n)
    private val doc = F1.build()

    @Test
    fun `enter at the end makes a block of the same kind and speaker`() {
        val r = BlockOps.enter(doc, b(47), Runs.length(doc.block(b(47)).runs), ctx) as BlockOps.EnterResult.NewBlock
        val nb = r.doc.block(r.blockKey)
        assertEquals(BlockKind.DIALOGUE, nb.kind.known)
        assertEquals(K.HART, nb.speakerKey)
        assertEquals(58, nb.number)
        val numbers = r.doc.liveNumbers(F1.S3)
        assertEquals(numbers.indexOf(47) + 1, numbers.indexOf(58))
        val narration = BlockOps.enter(doc, b(44), Runs.length(doc.block(b(44)).runs), ctx) as BlockOps.EnterResult.NewBlock
        assertEquals(BlockKind.NARRATION, narration.doc.block(narration.blockKey).kind.known)
        assertTrue(BlockOps.enter(doc, b(41), 0, ctx) is BlockOps.EnterResult.Nothing)
    }

    @Test
    fun `split inside a link keeps the link in the front block`() {
        val r = BlockOps.enter(doc, b(44), "바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 오리".length, ctx) as BlockOps.EnterResult.Split
        assertTrue(r.doc.block(b(44)).runs.any { it is TextRun.Link })
        assertEquals("이 한 번 깜박였다.", r.doc.block(r.newBlockKey).runs.plainText)
        assertEquals(Anchor.AtBlock(b(44)), r.doc.record(F1.REVEAL).anchor)
    }

    @Test
    fun `backspace in an empty block, at the scene start and across kinds`() {
        val ins = BlockOps.insert(doc, BlockPosition.After(b(48)), BlockKind.DIALOGUE, ctx)
        val removed = BlockOps.backspace(ins.doc, ins.blockKey, atStart = true, ctx) as BlockOps.BackspaceResult.RemovedEmpty
        assertEquals(b(48), removed.caretBlockKey)
        assertNull(removed.doc.index.block(ins.blockKey))
        assertEquals(ins.doc.trash, removed.doc.trash)

        val first = BlockOps.insert(doc, BlockPosition.SceneStart(F1.S3), BlockKind.NARRATION, ctx)
        assertTrue(BlockOps.backspace(first.doc, first.blockKey, atStart = true, ctx) is BlockOps.BackspaceResult.Nothing)
        assertTrue(BlockOps.backspace(doc, b(43), atStart = false, ctx) is BlockOps.BackspaceResult.Nothing)
        assertTrue(BlockOps.backspace(doc, b(42), atStart = true, ctx) is BlockOps.BackspaceResult.Nothing)

        val narrationIntoDialogue = BlockOps.backspace(doc, b(44), atStart = true, ctx) as BlockOps.BackspaceResult.Merged
        val merged = narrationIntoDialogue.doc.block(b(43))
        assertEquals(BlockKind.DIALOGUE, merged.kind.known)
        assertEquals(K.HART, merged.speakerKey)
        assertEquals(Anchor.AtBlock(b(43)), narrationIntoDialogue.doc.record(F1.REVEAL).anchor)
        assertEquals("◇ 1개 #043로", narrationIntoDialogue.toast)
        assertEquals("멈추지 마. 저건 등불이 아니야.".length + 1, narrationIntoDialogue.caretOffset)
    }

    @Test
    fun `kind change rules`() {
        val direction = doc.block(b(41))
        assertFalse(BlockOps.canChangeKind(direction, BlockKind.DIALOGUE))
        assertSame(doc, BlockOps.changeKind(doc, b(41), BlockKind.DIALOGUE, ctx))
        val narration = BlockOps.changeKind(doc, b(43), BlockKind.NARRATION, ctx).block(b(43))
        assertNull(narration.speakerKey)
        assertEquals(43, narration.number)
        assertEquals(doc.block(b(43)).runs, narration.runs)
        val memoToDialogue = BlockOps.changeKind(doc, b(46), BlockKind.DIALOGUE, ctx).block(b(46))
        assertEquals(BlockKind.DIALOGUE, memoToDialogue.kind.known)
        assertNull(VoiceIds.autoId(doc, memoToDialogue))
        val empty = BlockOps.insert(doc, BlockPosition.After(b(48)), BlockKind.NARRATION, ctx)
        val choice = BlockOps.changeKind(empty.doc, empty.blockKey, BlockKind.CHOICE, ctx).block(empty.blockKey)
        assertEquals(2, choice.options.size)
    }

    @Test
    fun `choice options keep at least two`() {
        val f3 = F3.build()
        val (added, newKey) = BlockOps.addOption(f3, b(45), F3.OPTION_1, ctx)!!
        val options = BlockOps.orderedOptions(added.block(b(45)))
        assertEquals(listOf(F3.OPTION_1, newKey, F3.OPTION_2), options.map { it.key })
        val removed = BlockOps.removeOption(added, b(45), newKey, ctx)
        assertEquals(2, removed.block(b(45)).options.size)
        assertSame(removed, BlockOps.removeOption(removed, b(45), F3.OPTION_2, ctx))
    }

    @Test
    fun `inherited speaker stays inside the episode`() {
        assertEquals(K.RIEN, BlockOps.inheritedSpeaker(doc, BlockPosition.After(b(46))))
        assertNull(BlockOps.inheritedSpeaker(doc, BlockPosition.SceneStart(F1.S1)))
        val ins = BlockOps.insert(doc, BlockPosition.After(b(46)), BlockKind.DIALOGUE, ctx)
        assertTrue(ins.inheritedSpeaker)
        assertEquals(K.RIEN, ins.doc.block(ins.blockKey).speakerKey)
        assertEquals(K.SERA, BlockOps.inheritedSpeaker(doc, BlockPosition.SceneStart(F1.S5)))
    }

    @Test
    fun `move within a scene keeps numbers and shows no toast`() {
        val r = BlockMove.move(doc, listOf(b(43)), BlockPosition.After(b(46)), ctx)
        assertNull(r.toast)
        assertEquals(listOf(36, 37, 38, 39, 40, 41, 42, 44, 45, 46, 43, 47, 48), r.doc.liveNumbers(F1.S3))
        val several = BlockMove.move(doc, listOf(b(48), b(36)), BlockPosition.SceneStart(F1.S4), ctx)
        assertEquals(listOf(36, 48, 49, 50, 51, 52), several.doc.liveNumbers(F1.S4))
        assertNull(several.toast)
    }

    @Test
    fun `undo of a multi-block delete restores order and records`() {
        val r9 = "01JZ000000CHGR000000000099"
        val withR9 = doc.withRecords(record(r9, Anchor.AtBlock(b(40))))
        val result = BlockTrash.delete(withR9, (38..42).map(::b), ctx)
        val undone = BlockTrash.undoDelete(result.doc, result, ctx)
        assertEquals(withR9.liveNumbers(F1.S3), undone.liveNumbers(F1.S3))
        assertEquals(Anchor.AtBlock(b(40)), undone.record(r9).anchor)
        assertEquals(Anchor.AtBlock(b(42)), undone.record(F1.R4).anchor)
        assertTrue(undone.trash.entries.none { e -> e.targetKeys.any { it in (38..42).map(::b) } })
    }

    @Test
    fun `copies get new keys for cues, options and links`() {
        val clip = BlockMove.copy(doc, listOf(b(41), b(44)))
        assertTrue(clip.hadRecords)
        val r = BlockMove.paste(doc, clip, BlockPosition.After(b(48)), ctx)
        val (cues, narration) = r.blockKeys.map(r.doc::block)
        assertNotEquals(doc.block(b(41)).cues.map { it.key }, cues.cues.map { it.key })
        assertEquals(doc.block(b(41)).cues.map { it.assetKey }, cues.cues.map { it.assetKey })
        val link = narration.runs.filterIsInstance<TextRun.Link>().single()
        assertNotEquals(K.LINK_L44, link.linkKey)
        assertEquals(listOf(58, 59), r.blockKeys.map { r.doc.block(it).number })
        assertTrue(r.doc.recordsAnchoredOn(narration.key).isEmpty())
    }
}
