package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.refs.DeletedItems
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

class CharacterActionsTest {
    private val live = Fx.noahLive()

    @Test
    fun `delete writes the cast reference snapshot into the trash entry`() {
        val result = CharacterActions.delete(live, K.NOAH, Fx.ctx(), Fx.recorder())!!
        assertEquals(DeletedItems.snapshot(live, K.NOAH), CharacterActions.snapshotOf(result.entry))
        assertEquals(result.entry, result.doc.index.trashEntry(result.entry.key))
        assertEquals(result.entry.key, result.doc.index.character(K.NOAH)!!.trashEntryKey)
        assertTrue(result.toast.text, result.toast.text.startsWith("인물 노아 지움 · 참조 "))
        assertTrue(result.toast.text.endsWith("되돌리기"))
    }

    @Test
    fun `delete of a trashed or unknown character does nothing`() {
        assertNull(CharacterActions.delete(Fx.doc(), K.NOAH, Fx.ctx()))
        assertNull(CharacterActions.delete(live, Fx.key("CHAR", 99), Fx.ctx()))
    }

    @Test
    fun `TC-20-05 restore toast counts what re-linked and what the author changed meanwhile`() {
        val deleted = CharacterActions.delete(live, K.NOAH, Fx.ctx())!!.doc
        val three = Fx.linesOf(deleted, K.NOAH).take(3).map { it.key }.toSet()
        val (changed, _) = DeletedItems.changeSpeaker(deleted, K.NOAH, K.OWEN, Fx.ctx().edit, onlyBlocks = three)
        val result = CharacterActions.restore(changed, K.NOAH, Fx.ctx())
        val restored = result.outcome as RestoreOutcome.Restored
        assertTrue(restored.doc.liveness.isCharacterLive(K.NOAH))
        assertEquals("노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3", result.toast!!.text)
    }

    @Test
    fun `restore of an entry without a snapshot falls back to the trash report`() {
        // The fixture's TRASH_NOAH was written before this action existed.
        val result = CharacterActions.restore(Fx.doc(), K.NOAH, Fx.ctx())
        assertTrue(result.outcome is RestoreOutcome.Restored)
        assertTrue(result.toast!!.text, result.toast!!.text.startsWith("노아 되살림"))
    }

    @Test
    fun `undo of a delete`() {
        val result = CharacterActions.delete(live, K.NOAH, Fx.ctx())!!
        val undone = CharacterActions.undo(result.doc, result)
        assertEquals(live.cast, undone.cast)
        assertEquals(live.trash, undone.trash)
        assertEquals(live.changes, undone.changes)
    }

    @Test
    fun `purge leaves a name tag and the recorder link keeps the name`() {
        val deleted = CharacterActions.delete(live, K.NOAH, Fx.ctx())!!.doc
        val purged = CharacterActions.purge(deleted, Fx.recorder(), K.NOAH, Fx.ctx())!!
        assertNull(purged.doc.index.character(K.NOAH))
        val tag = purged.doc.index.nameTag(K.NOAH)!!
        assertEquals(NameTagState.PURGED, tag.state.known)
        assertEquals("노아", tag.name)
        val link = purged.recorder.links.single { it.targetKey == K.NOAH }
        assertNotNull(link.purgedAt)
        assertEquals("노아", link.lastLabel)
        assertNull(CharacterActions.purge(live, Fx.recorder(), K.NOAH, Fx.ctx()))
    }
}
