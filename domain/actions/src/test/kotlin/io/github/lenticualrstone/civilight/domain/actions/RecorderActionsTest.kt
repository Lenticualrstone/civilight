package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.CardDocs
import io.github.lenticualrstone.civilight.domain.recorder.put.CardInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptSpot
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

class RecorderActionsTest {
    private val doc = Fx.doc()
    private val library = Fx.recorder()

    @Test
    fun `원고에 넣기 adds one memo block that copies the record's runs, undo removes it`() {
        val ins = RecorderActions.putInManuscript(doc, library, K.RECORDER_R2, ManuscriptSpot.SceneEnd(K.SCENE_1_7_3), Fx.ctx())
        val created = ins.doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).last()
        assertEquals(BlockKind.MEMO, created.kind.known)
        assertEquals(K.RECORDER_R2, created.recorderSource?.recordKey)
        // LRC-32: the `@` link into this project is kept as a link.
        assertTrue(created.runs.any { it is TextRun.Link && it.target.key == K.SERA })
        assertTrue(ins.library.placements.any { it.key == ins.placement.key && it.targetKey == created.key })
        val (undone, lib) = RecorderActions.undoPut(ins.doc, ins.library, ins.undo)
        assertEquals(doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }, undone.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key })
        assertEquals(library.placements, lib.placements)
    }

    @Test
    fun `카드에 넣기 and 페이지로 만들기`() {
        val card = TargetRef(doc.key, RecorderTargetKind.CHARACTER, K.RIEN)
        val tab = CardInsert.tabs(doc, card).first().id
        val toCard = RecorderActions.putInCard(doc, library, K.RECORDER_R4, card, tab, Fx.ctx())
        assertEquals(doc.key, toCard.placement.projectKey)
        assertTrue(CardDocs.holdsParagraph(toCard.doc, K.RIEN, toCard.placement.targetKey))
        val page = RecorderActions.makePage(doc, library, K.RECORDER_R4, "", Fx.ctx())
        assertEquals(doc.pages.size + 1, page.doc.pages.size)
        assertTrue(page.doc.pages.values.any { it.sourceRecord?.recordKey == K.RECORDER_R4 })
    }

    @Test
    fun `delete and restore a record through the recorder trash`() {
        val state = RecorderTrashState(library, ExampleRecorder.trashEntries())
        val deleted = RecorderActions.delete(state, K.RECORDER_R4, Fx.ctx())
        val record = deleted.library.record(K.RECORDER_R4)!!
        assertNotNull(record.deletedAt)
        val entry = deleted.entries.single { K.RECORDER_R4 in it.targetKeys }
        val back = RecorderActions.restore(deleted, entry.key, Fx.ctx())!!
        assertNull(back.library.record(K.RECORDER_R4)!!.deletedAt)
        assertTrue(back.entries.none { it.key == entry.key })
        val (purged, log) = RecorderActions.purge(deleted, entry.key, Fx.ctx())
        assertNull(purged.library.record(K.RECORDER_R4))
        assertTrue(log.any { it.key == K.RECORDER_R4 })
    }
}
