package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.doc.liveBlocksOf
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 10-script §4.2 rule-12 cases that exercise this module's delete, undo and restore (the script
 * area owns the editor side). On the example project 1-7 씬 3 holds R4, R5 and R5A on #043 (the
 * spec's F1 has one record R1 there), and 1-7 씬 4 has no blocks (fixture conflict #1).
 */
class ScriptTrashCasesTest {
    private val ctx = Fx.ctx()

    @Test
    fun `T-12-01 deleting an anchor block relocates the record (fixture variant)`() {
        val result = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        val block = result.doc.index.block(K.BLOCK_043)!!
        assertEquals(K.BLOCK_042, block.trashPlacement!!.prevKey)
        assertEquals(K.BLOCK_044, block.trashPlacement!!.nextKey)
        assertEquals(7, block.trashPlacement!!.ordinal)
        for (key in listOf(K.R4, K.R5, K.R5A)) {
            val record = result.doc.index.record(key)!!
            assertEquals(Anchor.AtBlock(K.BLOCK_044), record.anchor)
            assertEquals(Anchor.AtBlock(K.BLOCK_043), record.originalAnchor)
            assertEquals(AnchorState.ORPHANED, record.anchorState.known)
        }
        assertEquals("블록 #043 지움 · ◆ 3개는 #044 자리로 · 되돌리기", Messages.blockDeleteToast(result)!!.text)
        assertTrue(result.discardedVoiceIds.isEmpty())
    }

    @Test
    fun `T-12-03 undo re-attaches`() {
        val doc = Fx.doc()
        val result = TrashOps.delete(doc, ItemId.block(K.BLOCK_043), ctx)
        val undone = TrashOps.undo(result.doc, result)
        val live = undone.liveBlocksOf(K.SCENE_1_7_3).map { it.key }
        assertEquals(live.indexOf(K.BLOCK_042) + 1, live.indexOf(K.BLOCK_043))
        val r4 = undone.index.record(K.R4)!!
        assertEquals(Anchor.AtBlock(K.BLOCK_043), r4.anchor)
        assertNull(r4.originalAnchor)
        assertEquals(doc, undone)
    }

    @Test
    fun `T-12-04 restoring keeps a choice the user made`() {
        val deleted = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        // `다른 블록에 잇기` (domain:script) re-anchors R4 to #047: a new normal anchor, no original.
        val relinked = deleted.doc.copy(
            changes = deleted.doc.changes.copy(
                records = deleted.doc.changes.records.map {
                    if (it.key == K.R4) it.copy(anchor = Anchor.AtBlock(K.BLOCK_047), anchorState = AnchorState.NORMAL.open(), originalAnchor = null) else it
                },
            ),
        )
        val back = Fx.restored(RestoreOps.restore(relinked, deleted.entries.single().key, ctx))
        assertEquals(Anchor.AtBlock(K.BLOCK_047), back.doc.index.record(K.R4)!!.anchor)
        assertEquals(AnchorState.NORMAL, back.doc.index.record(K.R4)!!.anchorState.known)
        assertEquals(setOf(K.R5, K.R5A), back.report.reattached.toSet())
    }

    @Test
    fun `T-12-05 기록 지우기`() {
        val deleted = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        val removed = TrashOps.delete(deleted.doc, ItemId.change(K.R4), ctx)
        assertEquals(ctx.now, removed.doc.index.record(K.R4)!!.deletedAt)
        val row = TrashListing.projectItemRows(removed.doc, ctx).single { it.entryKey == removed.entries.single().key }
        assertEquals(TrashKind.CHANGE, row.kind.known)
        assertEquals(listOf(row), TrashListing.list(listOf(row), TrashFilter.CHANGE))
        // #044 keeps only the records still there (R5, R5A moved in, R9 its own); R4 is no longer live.
        val atNext = removed.doc.changes.records.filter { removed.doc.liveness.isLive(it) && it.anchor == Anchor.AtBlock(K.BLOCK_044) }
        assertEquals(setOf(K.R5, K.R5A, K.R9), atNext.map { it.key }.toSet())
    }

    @Test
    fun `T-12-07 deleting a scene (fixture variant)`() {
        val result = TrashOps.delete(Fx.doc(), ItemId.unit(K.SCENE_1_7_3), ctx)
        assertEquals(TrashKind.SCENE, result.entries.single().kind.known)
        // 1-7 씬 4 has no blocks here, so R4, R5, R5A and the first reveal go to 1-8 #001.
        val moved = result.relocations.associate { it.recordKey to it.to }
        assertEquals(setOf(K.R4, K.R5, K.R5A, K.R9), moved.keys)
        assertTrue(moved.values.all { it == Anchor.AtBlock(K.BLOCK_1_8_001) })
        assertEquals("씬 3 지움 · ◆ 3 · ◇ 1은 1-8 씬 1 처음으로 · 되돌리기", Messages.sceneDeleteToast(result)!!.text)
    }

    @Test
    fun `T-12-24 restore placement and preserved state`() {
        // (a) #043 fixed; then #042 and #044 deleted; #043 alone restored.
        val doc = Fx.fixVoice(Fx.doc(), K.BLOCK_043, "HRT_0107_043")
        val first = TrashOps.delete(doc, ItemId.block(K.BLOCK_043), ctx)
        val neighbours = TrashOps.delete(first.doc, listOf(K.BLOCK_042, K.BLOCK_044).map(ItemId::block), ctx)
        val entryKey = first.entries.single().key
        val back = Fx.restored(RestoreOps.restoreBlocks(neighbours.doc, entryKey, listOf(K.BLOCK_043), ctx)).doc
        val live = back.liveBlocksOf(K.SCENE_1_7_3).map { it.key }
        assertEquals(
            listOf(36, 37, 38, 39, 40, 41, 45, 43, 46, 47, 48).map { K.block(1, 7, it) },
            live,
        )
        val block = back.index.block(K.BLOCK_043)!!
        assertEquals(43, block.number)
        assertEquals(VoiceMode.FIXED, block.voice!!.mode.known)
        assertEquals("HRT_0107_043", block.voice!!.fixedId)
        assertTrue(back.index.allDiscardedVoiceIds.none { it.blockKey == K.BLOCK_043 })

        // (b) #043 deleted, then S3 deleted; 다른 씬 끝에 · S4 appends it with its number.
        val scene = TrashOps.delete(first.doc, ItemId.unit(K.SCENE_1_7_3), ctx)
        val choice = RestoreOps.restore(scene.doc, entryKey, ctx)
        assertEquals(RestoreOutcome.NeedsSceneChoice(K.SCENE_1_7_3, listOf(K.BLOCK_043)), choice)
        val appended = Fx.restored(RestoreOps.restore(scene.doc, entryKey, ctx, sceneChoice = SceneChoice.OtherSceneEnd(K.SCENE_1_7_4))).doc
        assertEquals(listOf(K.BLOCK_043), appended.liveBlocksOf(K.SCENE_1_7_4).map { it.key })
        assertEquals(43, appended.index.block(K.BLOCK_043)!!.number)
    }
}
