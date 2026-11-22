package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.merge.MergeChoices
import io.github.lenticualrstone.civilight.domain.cast.merge.MergePick
import io.github.lenticualrstone.civilight.domain.cast.merge.MergeSlot
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 합치기 through the one engine (domain:trash MergeOps) with the cast sheet's choices (TC-20-17 … TC-20-19). */
class MergeActionsTest {
    private val kid = Fx.key("CHAR", 14)
    private val kidRecord = Fx.key("CHGR", 1)
    private val kidLink = Fx.key("RLNK", 1)
    private val personality = FieldValue.of(FieldPayload.Texts(listOf("눈치가 빠르고 셈이 정확하다.")))

    /** `시장 꼬마` (3 of 노아's lines, 나이 `??`, 출생 1189, 성격, 소개, a `◆` on 성격, a recorder link) and the target 노아. */
    private val given: ProjectDoc = run {
        var d = Fx.addCharacter(
            Fx.noahLive(), kid, "시장 꼬마", 14,
            fields = mapOf(
                K.FIELD_AGE to FieldValue.UNKNOWN,
                K.FIELD_BIRTH to FieldValue.of(FieldPayload.Date(1189)),
                K.FIELD_PERSONALITY to personality,
            ),
            oneLiner = "시장 골목의 아이",
        )
        for (line in Fx.linesOf(d, K.NOAH).take(3)) d = Fx.editBlock(d, line.key) { it.copy(speakerKey = kid) }
        d.copy(
            changes = d.changes.copy(
                records = d.changes.records + ChangeRecord(
                    key = kidRecord, target = ChangeTarget.Field(EntityKind.CHARACTER, kid, K.FIELD_PERSONALITY),
                    value = ChangeRecord.encodeValue(FieldValue.serializer(), FieldValue.of(FieldPayload.Texts(listOf("셈이 더 빨라졌다.")))),
                    anchor = Anchor.AtBlock(K.block(1, 6, 19)),
                ),
            ),
        )
    }

    private val recorder: RecorderLibrary = Fx.recorder().let { lib ->
        val template = lib.links.first { it.recordKey == K.RECORDER_R4 || it.targetKey == K.HART }
        lib.copy(links = lib.links + template.copy(key = kidLink, recordKey = K.RECORDER_R4, targetKey = kid))
    }

    private fun noah(d: ProjectDoc) = d.index.character(K.NOAH)!!

    @Test
    fun `TC-20-17 default merge fills target-empty cells and keeps the target's values`() {
        val result = MergeActions.mergeCharacters(given, kid, K.NOAH, MergeChoices(), Fx.ctx(), recorder)
        val d = result.doc
        assertEquals(setOf(K.FIELD_BIRTH, K.FIELD_PERSONALITY), result.preview.filled.toSet())
        assertEquals(FieldValue.of(FieldPayload.Date(1189)), noah(d).fields[K.FIELD_BIRTH])
        assertEquals(personality, noah(d).fields[K.FIELD_PERSONALITY])
        assertEquals(noah(given).fields[K.FIELD_AGE], noah(d).fields[K.FIELD_AGE])
        assertEquals(noah(given).oneLiner, noah(d).oneLiner)
        // References moved, the merged item trashed as `합침 → 노아`.
        assertTrue(Fx.linesOf(d, kid).isEmpty())
        assertEquals(TrashReason.MERGED, result.entry.reason.known)
        assertEquals(K.NOAH, result.entry.mergedIntoKey)
        assertFalse(d.liveness.isCharacterLive(kid))
        assertTrue(result.toast.text, result.toast.text.startsWith("시장 꼬마 → 노아 합침 · 참조 "))
        // The cast sheet shows the same filled cells as the engine takes.
        val sheet = MergeActions.characterSheet(given, kid, K.NOAH)
        assertEquals(setOf(MergeSlot.Field(K.FIELD_BIRTH), MergeSlot.Field(K.FIELD_PERSONALITY)), sheet.filled.map { it.first }.toSet())
        // Recorder links follow the target.
        assertEquals(K.NOAH, result.recorder!!.links.single { it.key == kidLink }.targetKey)
    }

    @Test
    fun `the merged item's records of a taken field move to the target`() {
        val d = MergeActions.mergeCharacters(given, kid, K.NOAH, MergeChoices(), Fx.ctx()).doc
        assertEquals(ChangeTarget.Field(EntityKind.CHARACTER, K.NOAH, K.FIELD_PERSONALITY), d.index.record(kidRecord)!!.target)
    }

    @Test
    fun `per-cell choices of the cast sheet, character-only cells included`() {
        val choices = MergeChoices(cells = mapOf(MergeSlot.Field(K.FIELD_AGE) to MergePick.MERGED, MergeSlot.OneLiner to MergePick.MERGED))
        val result = MergeActions.mergeCharacters(given, kid, K.NOAH, choices, Fx.ctx())
        assertEquals(FieldValue.UNKNOWN, noah(result.doc).fields[K.FIELD_AGE])
        assertEquals("시장 골목의 아이", noah(result.doc).oneLiner)
        assertEquals(listOf(K.FIELD_AGE), result.record.chosenFromMerged.mapNotNull { it.fieldDefKey })
        assertEquals(listOf("oneLiner"), result.record.chosenFromMerged.mapNotNull { it.ext?.get(MergeActions.CELL)?.jsonPrimitive?.content })
        // 출생 · 성격 filled, 나이 and 소개 chosen.
        assertEquals("옮긴 참조 ${result.preview.referenceCount} · 관계 0 · 칸 4 · 노아에 그대로", MergeActions.restoreLine(result.doc, kid))
        assertEquals(result.record, result.doc.tombstones.mergeRecords.single { it.key == result.record.key })
    }

    @Test
    fun `TC-20-17 relation overlap keeps the target's slot unless the sheet picks the merged one`() {
        // 오웬's mind toward 시장 꼬마 `거래`, toward 노아 `보호` (same partner: one relation per pair after the merge).
        val kidOwen = Fx.key("RSHP", 1)
        val withOverlap = given.copy(
            cast = given.cast.copy(
                relations = given.cast.relations.map { if (it.key == K.REL_OWEN_NOAH) it.copy(mindA = SlotValue(kindKey = K.KIND_PROTECTION)) else it } +
                    Relation(
                        key = kidOwen, endA = EntityRef(RelationEndKind.CHARACTER, K.OWEN), endB = EntityRef(RelationEndKind.CHARACTER, kid),
                        mindA = SlotValue(description = "거래"),
                    ),
            ),
        )
        val kept = MergeActions.mergeCharacters(withOverlap, kid, K.NOAH, MergeChoices(), Fx.ctx()).doc
        assertEquals(SlotValue(kindKey = K.KIND_PROTECTION), kept.index.relation(K.REL_OWEN_NOAH)!!.mindA)
        assertNull(kept.index.relation(kidOwen))
        val choices = MergeChoices(relationSlots = mapOf((K.OWEN to RelationSlotName.MIND_A) to MergePick.MERGED))
        assertEquals(setOf(K.OWEN to RelationSlotName.MIND_A), MergeActions.engineChoices(choices).slotsFromMerged)
        val taken = MergeActions.mergeCharacters(withOverlap, kid, K.NOAH, choices, Fx.ctx()).doc
        assertEquals(SlotValue(description = "거래"), taken.index.relation(K.REL_OWEN_NOAH)!!.mindA)
    }

    @Test
    fun `a target value that lives only in records is not empty and keeps its history`() {
        // 리엔's 소속 lives only in R1 … R3 (no base value).
        val stray = Fx.key("CHAR", 15)
        val d = Fx.addCharacter(Fx.doc(), stray, "떠돌이", 15, fields = mapOf(K.FIELD_AFFILIATION to FieldValue.of(FieldPayload.Texts(listOf("잿더미 시장")))))
        val preview = MergeActions.itemPreview(d, ItemKind.CHARACTER, stray, K.RIEN)
        assertFalse(K.FIELD_AFFILIATION in preview.filled)
        assertTrue(preview.fieldConflicts.any { it.fieldDefKey == K.FIELD_AFFILIATION })

        val merged = MergeActions.mergeCharacters(d, stray, K.RIEN, MergeChoices(), Fx.ctx()).doc
        assertNull(merged.index.character(K.RIEN)!!.fields[K.FIELD_AFFILIATION])
        for (key in listOf(K.R1, K.R2, K.R3)) assertEquals(d.index.record(key), merged.index.record(key))

        val chosen = MergeActions.mergeCharacters(
            d, stray, K.RIEN, MergeChoices(cells = mapOf(MergeSlot.Field(K.FIELD_AFFILIATION) to MergePick.MERGED)), Fx.ctx(),
        ).doc
        assertEquals(FieldValue.of(FieldPayload.Texts(listOf("잿더미 시장"))), chosen.index.character(K.RIEN)!!.fields[K.FIELD_AFFILIATION])
        for (key in listOf(K.R1, K.R2, K.R3)) assertEquals(d.index.record(key)!!.target, chosen.index.record(key)!!.target)
    }

    @Test
    fun `TC-20-17 되돌리기 undoes the project rows and the recorder links`() {
        val choices = MergeChoices(cells = mapOf(MergeSlot.OneLiner to MergePick.MERGED))
        val result = MergeActions.mergeCharacters(given, kid, K.NOAH, choices, Fx.ctx(), recorder)
        val (doc, lib) = MergeActions.undo(result.doc, result.recorder, result)
        assertEquals(given.cast, doc.cast)
        assertEquals(given.changes, doc.changes)
        assertEquals(given.trash, doc.trash)
        assertEquals(given.tombstones, doc.tombstones)
        assertEquals(given.episodes, doc.episodes)
        assertEquals(recorder, lib)
    }

    @Test
    fun `TC-20-19 the merged character comes back from the trash as a separate item`() {
        val result = MergeActions.mergeCharacters(given, kid, K.NOAH, MergeChoices(), Fx.ctx())
        val back = CharacterActions.restore(result.doc, kid, Fx.ctx())
        val restored = back.outcome as RestoreOutcome.Restored
        assertTrue(restored.doc.liveness.isCharacterLive(kid))
        assertEquals("시장 꼬마", restored.doc.index.character(kid)!!.name)
        // Moved references and taken values stay with the target.
        assertTrue(Fx.linesOf(restored.doc, kid).isEmpty())
        assertEquals(FieldValue.of(FieldPayload.Date(1189)), noah(restored.doc).fields[K.FIELD_BIRTH])
    }

    @Test
    fun `world items merge through the same engine`() {
        val doc = Fx.doc()
        val result = MergeActions.mergeItems(doc, ItemKind.PLACE, K.PLACE_ARCHIVE_ROOM, K.PLACE_LOWER_STREET, Fx.ctx())
        val d = result.doc
        assertEquals(TrashReason.MERGED, result.entry.reason.known)
        assertFalse(d.liveness.isPlaceLive(K.PLACE_ARCHIVE_ROOM))
        assertTrue(d.manuscript.liveScenes.none { it.place?.key == K.PLACE_ARCHIVE_ROOM })
        val before = doc.manuscript.liveScenes.count { it.place?.key == K.PLACE_ARCHIVE_ROOM } +
            doc.manuscript.liveScenes.count { it.place?.key == K.PLACE_LOWER_STREET }
        assertEquals(before, d.manuscript.liveScenes.count { it.place?.key == K.PLACE_LOWER_STREET })
        assertEquals(doc.world, MergeActions.undo(d, null, result).first.world)
    }
}
