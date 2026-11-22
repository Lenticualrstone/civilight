package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * CST-45 on fields with `◆` history (the rule domain:cast's CharacterMerge already followed): a
 * target whose value lives only in records is not an empty cell, and a taken field brings the
 * merged item's records along (domain:actions MergeActions uses this engine for every kind).
 */
class MergeFieldHistoryTest {
    private val ctx = Fx.ctx()
    private val stray = Fx.key("CHAR", 15)
    private fun text(s: String) = FieldValue.of(FieldPayload.Texts(listOf(s)))

    private val doc = Fx.addCharacter(Fx.doc(), stray, "떠돌이", 15).let { d ->
        d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == stray) it.copy(fields = mapOf(K.FIELD_AFFILIATION to text("잿더미 시장"))) else it }))
    }

    @Test
    fun `a value that lives only in records is not filled over`() {
        // 리엔's 소속: R1 … R3, no base value.
        assertNull(doc.index.character(K.RIEN)!!.fields[K.FIELD_AFFILIATION])
        val preview = MergeOps.preview(doc, ItemKind.CHARACTER, stray, K.RIEN)
        assertFalse(K.FIELD_AFFILIATION in preview.filled)
        val conflict = preview.fieldConflicts.single { it.fieldDefKey == K.FIELD_AFFILIATION }
        assertEquals(text("잿더미 시장"), conflict.mergedValue)
        val merged = MergeOps.merge(doc, ItemKind.CHARACTER, stray, K.RIEN, ctx).doc
        assertNull(merged.index.character(K.RIEN)!!.fields[K.FIELD_AFFILIATION])
        for (key in listOf(K.R1, K.R2, K.R3)) assertEquals(doc.index.record(key), merged.index.record(key))
    }

    @Test
    fun `choosing the merged value takes its base and keeps the target's records`() {
        val merged = MergeOps.merge(doc, ItemKind.CHARACTER, stray, K.RIEN, ctx, MergeChoices(fieldsFromMerged = setOf(K.FIELD_AFFILIATION))).doc
        assertEquals(text("잿더미 시장"), merged.index.character(K.RIEN)!!.fields[K.FIELD_AFFILIATION])
        for (key in listOf(K.R1, K.R2, K.R3)) assertEquals(doc.index.record(key)!!.target, merged.index.record(key)!!.target)
    }

    @Test
    fun `a taken field brings the merged item's records, a target record at the same point gives way`() {
        val own = Fx.key("CHGR", 1)
        val clash = Fx.key("CHGR", 2)
        val at = Anchor.AtBlock(K.block(1, 6, 19))
        val withRecords = doc.copy(
            changes = doc.changes.copy(
                records = doc.changes.records +
                    ChangeRecord(own, ChangeTarget.Field(EntityKind.CHARACTER, stray, K.FIELD_AFFILIATION), ChangeRecord.encodeValue(FieldValue.serializer(), text("상층")), anchor = at) +
                    ChangeRecord(clash, ChangeTarget.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION), ChangeRecord.encodeValue(FieldValue.serializer(), text("하층")), anchor = at),
            ),
        )
        val result = MergeOps.merge(withRecords, ItemKind.CHARACTER, stray, K.RIEN, ctx, MergeChoices(fieldsFromMerged = setOf(K.FIELD_AFFILIATION)))
        assertEquals(ChangeTarget.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION), result.doc.index.record(own)!!.target)
        assertNull(result.doc.index.record(clash))
        assertTrue(result.record.movedRefs.any { it.ownerKind == "record" && it.ownerKey == own })
        // 되돌리기 brings both back as they were.
        val undone = MergeOps.undo(result.doc, result)
        assertEquals(withRecords.changes, undone.changes)
    }
}
