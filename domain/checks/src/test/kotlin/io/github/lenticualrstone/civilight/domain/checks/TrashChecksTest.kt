package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** Row 9 (`지운 항목`, CST-42, rule 20 §3) against the trash operations. */
class TrashChecksTest {
    private val doc = Fx.doc()
    private var keys = 0
    private val trashCtx = TrashContext(ExampleClock.NOW, ZoneId.of("Asia/Seoul"), "test", { "01JZ000000TRSH7" + (++keys).toString().padStart(11, '0') })

    /** CST TC-20-04: one row per deleted item with its reference counts. */
    @Test
    fun tc2004_deletedCharacterRow() {
        val row = Fx.run(doc).ofType(CheckType.DELETED_ITEM).single()
        assertEquals("◇ 지운 인물 · 노아 · 3일 남음 · 대사 14 · 씬 등장 2 · @ 3 · 레코더 1", row.line)
        assertEquals("휴지통", row.location)
        assertEquals(CheckFilter.DELETED, row.filter)
        assertEquals("화자 한꺼번에 바꾸기", row.actions.first().label)
        assertEquals(FixKind.BULK_REPLACE, row.actions.first().kind)
        assertEquals(listOf("되살리기", "이대로 두기"), row.actions.takeLast(2).map { it.label })
        assertEquals(listOf(FixKind.RESTORE, FixKind.ACKNOWLEDGE), row.actions.takeLast(2).map { it.kind })
    }

    /** CST TC-20-28 / CNV T-CNV-25: `이대로 두기` is a keep acknowledgement on the item. */
    @Test
    fun tc2028_keepMovesTheRowToAcknowledged() {
        val row = Fx.run(doc).ofType(CheckType.DELETED_ITEM).single()
        val kept = CheckAcks.acknowledge(doc, row, Fx.ctx())
        val ack = kept.checks.acknowledgements.last()
        assertEquals(CheckType.DELETED_ITEM, ack.checkType)
        assertEquals(listOf(K.NOAH), ack.subjectKeys)
        assertEquals(AckKind.KEEP, ack.kind.known)
        assertTrue(Fixes.isKept(kept, K.NOAH))
        val after = Fx.run(kept)
        assertEquals(0, after.count(CheckFilter.DELETED))
        assertEquals(3, after.count(CheckFilter.ACKNOWLEDGED))
        // Keep acknowledgements of deleted items are never voided by evaluation.
        assertTrue(CheckAcks.voided(after).isEmpty())
    }

    /** After a permanent delete the row reads `영구 삭제 · 노아` and offers no `되살리기`. */
    @Test
    fun purgedItemRow() {
        val purged = PurgeOps.purge(doc, K.TRASH_NOAH, trashCtx).doc
        val row = Fx.run(purged).ofType(CheckType.DELETED_ITEM).single()
        assertEquals("영구 삭제 · 노아", row.title)
        assertEquals(null, row.location)
        assertTrue(row.actions.none { it.kind == FixKind.RESTORE })
    }

    /** CST TC-20-15: a deleted term holds its `▲`; restoring brings it back to 1-4 씬 2. */
    @Test
    fun tc2015_deletedTermHoldsTheWarning() {
        val deleted = TrashOps.delete(doc, ItemId.term(K.TERM_ORIGINIUM_LANTERN), trashCtx)
        val report = Fx.run(deleted.doc)
        assertTrue(report.ofType(CheckType.TERM_BEFORE_REVEAL).isEmpty())
        assertTrue(report.ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).isEmpty())
        val row = report.ofType(CheckType.DELETED_ITEM).single { it.subjectKeys == listOf(K.TERM_ORIGINIUM_LANTERN) }
        assertTrue(row.line, row.title.startsWith("◇ 지운 용어 · 오리지늄 등불"))
        assertTrue(row.line, row.body!!.endsWith("▲ 1 보류"))

        val restored = RestoreOps.restore(deleted.doc, deleted.entries.single().key, trashCtx) as RestoreOutcome.Restored
        val back = Fx.run(restored.doc)
        assertEquals("1-4 씬 2", back.ofType(CheckType.TERM_BEFORE_REVEAL).single().location)
        assertTrue(back.ofType(CheckType.DELETED_ITEM).none { it.subjectKeys == listOf(K.TERM_ORIGINIUM_LANTERN) })
    }
}
