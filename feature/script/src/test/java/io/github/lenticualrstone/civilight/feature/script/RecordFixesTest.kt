package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.feature.script.panel.TimeTab
import io.github.lenticualrstone.civilight.feature.script.records.RecordFixes
import io.github.lenticualrstone.civilight.feature.script.records.Relink
import io.github.lenticualrstone.civilight.feature.script.state.InsertAt
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * `근거 지워짐` on the panel (SCR-43, SCR-39) and `블록에 잇기` (SCR-42), on the example project, where
 * `#043` carries three `◆` (spec F1 has one, so counts read 3 where the spec says 1).
 */
class RecordFixesTest {
    private val doc = Fx.example

    private fun deleted043(): ProjectDoc = ScriptOps.deleteBlocks(doc, listOf(K.BLOCK_043), Fx.ctx())!!.doc

    private fun action(): ActionContext {
        var n = 0
        return ActionContext(now = ExampleClock.NOW, zone = Fx.zone, device = "test", newKey = { "01JZ000000ACTN" + (++n).toString().padStart(12, '0') })
    }

    @Test
    fun `T-12-01 after deleting #043 the 시간 tab of #044 shows the orphan cards with three buttons and the trash note`() {
        val d = deleted043()
        val tab = TimeTab.of(d, K.BLOCK_044, null, StoryTimes.source(d))
        val orphans = tab.cards.filter { it.orphan }
        assertEquals(3, orphans.size)
        assertTrue(orphans.all { it.line == "근거 #043 지워짐 · #044로 옮겨 옴" && it.dotted })
        // The ◇ of #044 is its own: not flagged.
        assertTrue(tab.cards.single { it.reveal }.let { !it.orphan && it.line == "이 블록 · 1-7 #044" })
        assertEquals(listOf("#043 · 휴지통 · 30일 · 되살리면 다시 이어짐"), tab.trashNotes.map { it.text })
        assertEquals(K.BLOCK_043, tab.trashNotes.single().blockKey)
        assertTrue(tab.canRecord && tab.editable)
        // Read-only projects get no buttons.
        assertFalse(TimeTab.of(d, K.BLOCK_044, null, StoryTimes.source(d), readOnly = true).editable)
    }

    @Test
    fun `T-12-02 a line written after #042 is the first candidate, and choosing it re-anchors with a solid mark`() {
        val d0 = deleted043()
        val inserted = ScriptOps.insert(d0, InsertAt.After(K.BLOCK_042), BlockKind.DIALOGUE, K.HART, Fx.ctx())
        val newBlock = inserted.caret!!.blockKey
        val d = inserted.doc
        val record = d.changes.records.first { it.anchorState.known == AnchorState.ORPHANED }
        val sheet = Relink.of(d, record.key, unlocked = true)!!
        assertEquals("다른 블록에 잇기", sheet.title)
        assertEquals(newBlock, sheet.rows.first().blockKey)
        assertEquals("하르트 #058", sheet.rows.first().line.label)

        val out = RecordFixes.relink(d, record.key, newBlock, Fx.ctx())!!
        val moved = out.doc.index.record(record.key)!!
        assertEquals(Anchor.AtBlock(newBlock), moved.anchor)
        assertEquals(AnchorState.NORMAL, moved.anchorState.known)
        assertNull(moved.originalAnchor)
        assertEquals(newBlock, out.select)
        assertTrue(Records.marksOn(out.doc, newBlock).single { it.recordKey == record.key }.let { !it.dotted })
    }

    @Test
    fun `SCR-43 이대로 두기 keeps the record dotted without buttons, 기록 지우기 trashes it with a toast`() {
        val d = deleted043()
        val record = d.changes.records.first { it.anchorState.known == AnchorState.ORPHANED }

        val kept = RecordFixes.keep(d, record.key, Fx.ctx())!!.doc
        val keptCard = TimeTab.of(kept, K.BLOCK_044, null, StoryTimes.source(kept)).cards.single { it.recordKey == record.key }
        assertFalse(keptCard.orphan)
        assertTrue(keptCard.dotted)
        assertNull(keptCard.trashNote)
        assertFalse(Relink.isOrphan(kept, record.key))

        val trashed = RecordFixes.trash(d, record.key, Fx.ctx())!!
        assertEquals("${Records.shortTitle(d, record)} 지움", trashed.toast)
        assertTrue(trashed.toast!!.startsWith("◆ 관계 · "))
        val gone = trashed.doc.index.record(record.key)!!
        assertNotNull(gone.deletedAt)
        assertEquals(TrashKind.CHANGE, trashed.doc.index.trashEntry(gone.trashEntryKey!!)?.kind?.known)
        assertTrue(TimeTab.of(trashed.doc, K.BLOCK_044, null, StoryTimes.source(trashed.doc)).cards.none { it.recordKey == record.key })
    }

    @Test
    fun `SCR-39 되살리기 on the note brings #043 back at its place and its records re-attach`() {
        val d = deleted043()
        val result = RecordFixes.restoreBlock(d, K.BLOCK_043, action(), authenticated = false)
        val done = result as RecordFixes.Restore.Done
        val out = done.outcome.doc
        assertTrue(out.manuscript.isLiveBlock(K.BLOCK_043))
        assertEquals("블록 #043 되살림", done.outcome.toast)
        assertEquals(K.BLOCK_043, done.outcome.select)
        val order = out.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }
        assertEquals(order.indexOf(K.BLOCK_042) + 1, order.indexOf(K.BLOCK_043))
        assertEquals(3, out.changes.records.count { (it.anchor as? Anchor.AtBlock)?.blockKey == K.BLOCK_043 && it.anchorState.known == AnchorState.NORMAL })
        // Nothing to restore for a live block.
        assertEquals(RecordFixes.Restore.Nothing, RecordFixes.restoreBlock(doc, K.BLOCK_043, action(), authenticated = false))
    }

    @Test
    fun `T-12-25 블록에 잇기 on a point line turns it into a block-seat record`() {
        val point = ChangeRecord(
            key = "01JZ000000CHGRTEST00000001",
            target = ChangeTarget.DisplayName(K.RIEN),
            value = JsonPrimitive("등불지기"),
            anchor = Anchor.UnitStart(K.EPISODE_1_7),
        )
        val d = doc.copy(changes = doc.changes.copy(records = doc.changes.records + point))
        val line = TimeTab.of(d, null, K.SCENE_1_7_1, StoryTimes.source(d)).inScene.first { it.point }
        assertEquals(point.key, line.recordKey)
        val sheet = Relink.of(d, point.key, unlocked = true)!!
        assertEquals("블록에 잇기", sheet.title)
        val scene1 = d.manuscript.liveBlocksOf(K.SCENE_1_7_1).map { it.key }
        assertEquals(scene1, sheet.rows.map { it.blockKey })
        val second = scene1[1]
        val out = RecordFixes.relink(d, point.key, second, Fx.ctx())!!.doc
        assertTrue(TimeTab.of(out, null, K.SCENE_1_7_1, StoryTimes.source(out)).inScene.none { it.point })
        assertTrue(Records.marksOn(out, second).any { it.recordKey == point.key })
    }
}
