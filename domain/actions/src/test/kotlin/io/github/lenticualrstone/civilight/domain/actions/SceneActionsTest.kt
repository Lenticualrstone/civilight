package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.story.scene.SceneTrash
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RefKind
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.SceneChoice
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

class SceneActionsTest {
    private val doc = Fx.doc()

    @Test
    fun `delete composes the story mark and rebase with the trash settle and toast`() {
        val result = SceneActions.delete(doc, K.SCENE_1_7_3, Fx.ctx())
        // Story side: mark, entry and 때 rebase exactly as SceneTrash.
        val storyOnly = SceneTrash.delete(doc, K.SCENE_1_7_3, Fx.ctx().edit)
        assertEquals(storyOnly.story.units, result.doc.story.units)
        assertEquals(TrashKind.SCENE, result.entry.kind.known)
        assertEquals(listOf(K.SCENE_1_7_3), result.entry.targetKeys)
        // Trash side: the same relocations, records and toast as trash's own unit delete.
        val own = TrashOps.delete(doc, ItemId.unit(K.SCENE_1_7_3), Fx.ctx().trash)
        assertEquals(own.relocations, result.relocations)
        assertEquals(own.doc.changes, result.doc.changes)
        assertEquals(own.doc.checks, result.doc.checks)
        assertEquals(Messages.sceneDeleteToast(own)!!.text, result.toast.text)
        assertEquals(TrashOps.refSitesOf(own.entries.single()), TrashOps.refSitesOf(result.entry))
        assertTrue(result.toast.text.startsWith("씬 3 지움"))
    }

    @Test
    fun `delete rebases the scenes based on the deleted one`() {
        val dependents = doc.story.units.filter { (it.time as? SceneTime.Relative)?.baseKey == K.SCENE_1_7_3 && doc.liveness.isUnitLive(it.key) }
        val result = SceneActions.delete(doc, K.SCENE_1_7_3, Fx.ctx())
        for (d in dependents) {
            val time = result.doc.index.unit(d.key)!!.time as SceneTime.Relative
            assertEquals(K.SCENE_1_7_3, time.rebasedFromKey)
        }
    }

    @Test
    fun `undo brings everything back`() {
        val result = SceneActions.delete(doc, K.SCENE_1_7_3, Fx.ctx())
        val undone = SceneActions.undo(result.doc, result)
        assertEquals(doc.story.units, undone.story.units)
        assertEquals(doc.changes, undone.changes)
        assertEquals(doc.trash, undone.trash)
        assertEquals(doc.checks, undone.checks)
    }

    @Test
    fun `restore brings the scene, its 때 and its records back`() {
        val deleted = SceneActions.delete(doc, K.SCENE_1_7_3, Fx.ctx()).doc
        val back = Fx.restored(SceneActions.restore(deleted, K.SCENE_1_7_3, Fx.ctx()))
        assertTrue(back.doc.liveness.isUnitLive(K.SCENE_1_7_3))
        assertTrue(back.doc.trash.entries.none { K.SCENE_1_7_3 in it.targetKeys })
        assertEquals(doc.story.units.map { it.key to it.time }, back.doc.story.units.map { it.key to it.time })
        assertTrue(setOf(K.R4, K.R5, K.R5A, K.R9).all { back.doc.index.record(it)!!.anchorState.known == AnchorState.NORMAL })
        assertTrue(back.report.reattached.containsAll(listOf(K.R4, K.R5, K.R5A, K.R9)))
        assertEquals(ItemId.unit(K.SCENE_1_7_3), back.report.item)
        // 1-7 씬 4 is based on 씬 3 again: the base reference re-linked, nothing was changed meanwhile.
        assertEquals(1, back.report.relinked[RefKind.BASE_SCENE])
        assertTrue(back.report.keptChanged.isEmpty())
    }

    @Test
    fun `restore of the fixture's trashed scene and of the entry`() {
        val byScene = Fx.restored(SceneActions.restore(doc, K.SCENE_1_6_5, Fx.ctx()))
        val byEntry = Fx.restored(SceneActions.restoreEntry(doc, K.TRASH_SCENE_1_6_5, Fx.ctx()))
        assertTrue(byScene.doc.liveness.isUnitLive(K.SCENE_1_6_5))
        assertEquals(byScene.doc, byEntry.doc)
        assertNull(byScene.doc.index.trashEntry(K.TRASH_SCENE_1_6_5))
    }

    @Test
    fun `restore of a live scene finds nothing and a hidden scene needs authentication`() {
        assertEquals(RestoreOutcome.NotFound, SceneActions.restore(doc, K.SCENE_1_7_4, Fx.ctx()))
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_7_3) it.copy(hidden = true) else it }))
        val deleted = SceneActions.delete(hidden, K.SCENE_1_7_3, Fx.ctx()).doc
        assertEquals(RestoreOutcome.NeedsAuth, SceneActions.restore(deleted, K.SCENE_1_7_3, Fx.ctx()))
        assertTrue(SceneActions.restore(deleted, K.SCENE_1_7_3, Fx.ctx(), authenticated = true) is RestoreOutcome.Restored)
    }

    @Test
    fun `SCR-39 씬도 되살리기 restores the scene with its 때, then the block at its place`() {
        val blockGone = TrashOps.delete(doc, ItemId.block(K.BLOCK_043), Fx.ctx().trash)
        val blockEntry = blockGone.entries.single().key
        val sceneGone = SceneActions.delete(blockGone.doc, K.SCENE_1_7_3, Fx.ctx(seed = 9))
        assertEquals(
            RestoreOutcome.NeedsSceneChoice(K.SCENE_1_7_3, listOf(K.BLOCK_043)),
            SceneActions.restoreBlocks(sceneGone.doc, blockEntry, listOf(K.BLOCK_043), Fx.ctx()),
        )
        val back = Fx.restored(SceneActions.restoreBlocks(sceneGone.doc, blockEntry, listOf(K.BLOCK_043), Fx.ctx(), sceneChoice = SceneChoice.RestoreScene))
        assertTrue(back.doc.liveness.isUnitLive(K.SCENE_1_7_3))
        assertTrue(back.doc.liveness.isBlockLive(K.BLOCK_043))
        // 때 as before the scene delete: the rebase was undone, not left behind.
        assertEquals(doc.story.units.map { it.key to it.time }, back.doc.story.units.map { it.key to it.time })
        assertTrue(back.doc.trash.entries.none { it.key == blockEntry || K.SCENE_1_7_3 in it.targetKeys })
        assertEquals(doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }, back.doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key })
        assertTrue(setOf(K.R4, K.R5, K.R5A).all { back.doc.index.record(it)!!.anchorState.known == AnchorState.NORMAL })
    }

    @Test
    fun `structure pass-throughs`() {
        val ctx = Fx.ctx()
        val (created, key) = SceneActions.create(doc, K.EPISODE_1_7, K.SCENE_1_7_3, ctx, title = "새 씬")
        val scene = created.index.unit(key)!!
        assertEquals(K.EPISODE_1_7, scene.parentKey)
        val siblings = created.index.children(K.EPISODE_1_7).map { it.key }
        assertEquals(siblings.indexOf(K.SCENE_1_7_3) + 1, siblings.indexOf(key))
        val split = SceneActions.split(doc, K.SCENE_1_7_3, K.BLOCK_043, Fx.ctx())
        assertNotNull(split.result.index.unit(split.newSceneKey))
        val move = SceneActions.move(doc, K.SCENE_1_7_4, K.EPISODE_1_7, null, Fx.ctx())
        assertEquals(K.SCENE_1_7_4, move.result.index.children(K.EPISODE_1_7).first().key)
    }
}
