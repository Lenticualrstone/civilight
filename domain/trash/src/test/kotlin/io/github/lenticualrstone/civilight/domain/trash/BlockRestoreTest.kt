package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.doc.liveBlocksOf
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SCR-39 / LIB-38 block restore positions and grouping (§5.3). */
class BlockRestoreTest {
    private val ctx = Fx.ctx()

    @Test
    fun `one row of an expanded block group restores alone`() {
        val group = listOf(K.BLOCK_036, K.BLOCK_037, K.BLOCK_038, K.BLOCK_039)
        val deleted = TrashOps.delete(Fx.doc(), group.map(ItemId::block), ctx)
        val entry = deleted.entries.single()
        val back = Fx.restored(RestoreOps.restoreBlocks(deleted.doc, entry.key, listOf(K.BLOCK_036), ctx)).doc
        assertEquals(K.BLOCK_036, back.liveBlocksOf(K.SCENE_1_7_3).first().key)
        assertEquals(listOf(K.BLOCK_037, K.BLOCK_038, K.BLOCK_039), back.index.trashEntry(entry.key)!!.targetKeys)
        // The rest in one go: back in their old order.
        val all = Fx.restored(RestoreOps.restore(back, entry.key, ctx)).doc
        assertEquals((36..48).map { K.block(1, 7, it) }, all.liveBlocksOf(K.SCENE_1_7_3).map { it.key })
        assertNull(all.index.trashEntry(entry.key))
    }

    @Test
    fun `both neighbours gone falls back to the ordinal`() {
        val alone = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_037), ctx)
        val placement = alone.doc.index.block(K.BLOCK_037)!!.trashPlacement!!
        assertEquals(K.BLOCK_036, placement.prevKey)
        assertEquals(K.BLOCK_038, placement.nextKey)
        assertEquals(1, placement.ordinal)
        // Same scene, same day: the neighbours join #037's entry; restore #037's row alone.
        val neighbours = TrashOps.delete(alone.doc, listOf(K.BLOCK_036, K.BLOCK_038).map(ItemId::block), ctx)
        val entry = neighbours.doc.index.trashEntry(alone.entries.single().key)!!
        assertEquals(listOf(K.BLOCK_037, K.BLOCK_036, K.BLOCK_038), entry.targetKeys)
        val back = Fx.restored(RestoreOps.restoreBlocks(neighbours.doc, entry.key, listOf(K.BLOCK_037), ctx)).doc
        val live = back.liveBlocksOf(K.SCENE_1_7_3).map { it.key }
        assertEquals(1, live.indexOf(K.BLOCK_037))
        assertEquals(listOf(K.BLOCK_039, K.BLOCK_037, K.BLOCK_040), live.take(3))
    }

    @Test
    fun `blocks deleted on one day in one scene share an entry`() {
        val a = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_036), ctx)
        val b = TrashOps.delete(a.doc, ItemId.block(K.BLOCK_040), ctx)
        val groups = b.doc.trash.entries.filter { it.kind.known == TrashKind.BLOCK_GROUP && it.groupSceneKey == K.SCENE_1_7_3 }
        assertEquals(1, groups.size)
        assertEquals(listOf(K.BLOCK_036, K.BLOCK_040), groups.single().targetKeys)
        // Another scene the same day gets its own entry.
        val c = TrashOps.delete(b.doc, ItemId.block(K.block(1, 7, 20)), ctx)
        assertEquals(2, c.doc.trash.entries.count { it.kind.known == TrashKind.BLOCK_GROUP && it.groupDate == ctx.today })
    }

    @Test
    fun `다른 씬 끝에 into another episode takes its next number`() {
        val doc = Fx.fixVoice(Fx.doc(), K.BLOCK_043, "HRT_0107_043")
        val block = TrashOps.delete(doc, ItemId.block(K.BLOCK_043), ctx)
        val scene = TrashOps.delete(block.doc, ItemId.unit(K.SCENE_1_7_3), ctx)
        val counter = scene.doc.index.unit(K.EPISODE_1_8)!!.nextBlockNumber!!
        val back = Fx.restored(
            RestoreOps.restore(scene.doc, block.entries.single().key, ctx, sceneChoice = SceneChoice.OtherSceneEnd(K.SCENE_1_8_1)),
        ).doc
        val moved = back.index.block(K.BLOCK_043)!!
        assertEquals(K.SCENE_1_8_1, moved.sceneKey)
        assertEquals(counter, moved.number)
        assertEquals(counter + 1, back.index.unit(K.EPISODE_1_8)!!.nextBlockNumber)
        assertEquals(K.BLOCK_043, back.liveBlocksOf(K.SCENE_1_8_1).last().key)
        assertEquals(K.EPISODE_1_8, back.index.episodeGroupOfBlock(K.BLOCK_043))
        // The fixed voice ID is kept; the scene stays in the trash.
        assertEquals(VoiceMode.FIXED, moved.voice!!.mode.known)
        assertEquals("HRT_0107_043", moved.voice!!.fixedId)
        assertTrue(!back.liveness.isUnitLive(K.SCENE_1_7_3))
        assertTrue(back.liveness.isBlockLive(K.BLOCK_043))
    }

    @Test
    fun `block delete toast counts relocated records`() {
        val one = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_043), ctx)
        assertEquals("블록 #043 지움 · ◆ 3개는 #044 자리로 · 되돌리기", Messages.blockDeleteToast(one)!!.text)
        val reveal = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_044), ctx)
        assertEquals("블록 #044 지움 · ◇ 1개는 #045 자리로 · 되돌리기", Messages.blockDeleteToast(reveal)!!.text)
        val plain = TrashOps.delete(Fx.doc(), ItemId.block(K.BLOCK_036), ctx)
        assertEquals("블록 #036 지움 · 되돌리기", Messages.blockDeleteToast(plain)!!.text)
    }
}
