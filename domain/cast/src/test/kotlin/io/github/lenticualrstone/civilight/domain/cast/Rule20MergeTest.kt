package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.merge.CharacterMerge
import io.github.lenticualrstone.civilight.domain.cast.merge.MergeChoices
import io.github.lenticualrstone.civilight.domain.cast.merge.MergePick
import io.github.lenticualrstone.civilight.domain.cast.merge.MergePlanner
import io.github.lenticualrstone.civilight.domain.cast.merge.MergeSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
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
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Rule 20 §6 · 합치기 (30-cast.md TC-20-17 … TC-20-19), with characters. */
class Rule20MergeTest {
    private val kid = Fx.key("CHAR", 14)
    private val kidLink = Fx.key("HREF", 1)
    private val kidOwen = Fx.key("RSHP", 1)
    private val kidRecord = Fx.key("CHGR", 1)
    private val ctx = Fx.ctx()

    private fun rich(text: String) = FieldValue.of(
        FieldPayload.Rich(listOf(DocBlock(key = Fx.key("DOCB", 1), order = OrderKeys.FIRST, runs = listOf(TextRun.Plain(text))))),
    )

    /**
     * `시장 꼬마` (3 lines with 2 fixed voice IDs, 1 `@`, 1 scene appearance; 나이 `??`; 오웬's mind toward
     * it `거래`; 출생 and 성격 filled) and the target `노아` (나이 11세; 오웬's mind `보호`; 출생 and 성격 empty).
     */
    private val kidLines: List<Key>
    private val given: ProjectDoc

    init {
        var d = Fx.noahLive()
        d = Fx.addCharacter(
            d, kid, "시장 꼬마", 14,
            fields = mapOf(
                K.FIELD_AGE to FieldValue.UNKNOWN,
                K.FIELD_BIRTH to Fx.year(1189),
                K.FIELD_PERSONALITY to rich("눈치가 빠르고 셈이 정확하다."),
            ),
        )
        kidLines = Fx.linesOf(d, K.NOAH).take(3).map { it.key }
        for ((i, line) in kidLines.withIndex()) {
            d = Fx.editBlock(d, line) { b ->
                val fixed = if (i < 2) VoiceState(mode = VoiceMode.FIXED.open(), fixedId = "NOA_0106_00$i", fixedAt = ExampleClock.VOICE_EXPORT) else b.voice
                b.copy(speakerKey = kid, voice = fixed)
            }
        }
        d = Fx.editBlock(d, K.BLOCK_038) { it.copy(runs = it.runs + TextRun.Link(kidLink, LinkTarget(LinkTargetKind.CHARACTER, kid), "시장 꼬마")) }
        d = d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == K.SCENE_1_1_1) it.copy(silentCast = it.silentCast + kid) else it }))
        d = d.copy(
            cast = d.cast.copy(
                relations = d.cast.relations.map { if (it.key == K.REL_OWEN_NOAH) it.copy(mindA = SlotValue(kindKey = K.KIND_PROTECTION)) else it } +
                    Relation(
                        key = kidOwen, endA = EntityRef(RelationEndKind.CHARACTER, K.OWEN), endB = EntityRef(RelationEndKind.CHARACTER, kid),
                        mindA = SlotValue(description = "거래"),
                    ),
            ),
            changes = d.changes.copy(
                records = d.changes.records + ChangeRecord(
                    key = kidRecord, target = ChangeTarget.RelationSlot(kidOwen, RelationSlotName.MIND_A),
                    value = ChangeRecord.encodeValue(SlotValue.serializer(), SlotValue(kindKey = K.KIND_WARINESS)),
                    anchor = Anchor.AtBlock(K.block(1, 6, 19)),
                ),
            ),
        )
        given = d
    }

    private fun age(d: ProjectDoc, key: Key): String {
        val def = Fx.def(d, "나이")
        return FieldText.format(d, def, FieldReader.read(d, EntityKind.CHARACTER, key, def, ReadPoint.LATEST).value).text
    }

    @Test
    fun `TC-20-17 comparison sheet`() {
        val p = MergePlanner.preview(given, kid, K.NOAH)
        assertEquals("인물 합치기", p.title)
        assertEquals("시장 꼬마 → 노아 · 기본은 노아 값", p.sub)
        assertEquals("옮길 참조 · 5", p.movedTitle)
        assertEquals("대사 3 · @연결 1 · 씬 등장 1", p.movedLine)
        assertEquals("다른 칸 · 1", p.differingTitle)
        assertEquals("시장 꼬마 · 합쳐짐" to "노아 · 남음", p.columns)
        val ageRow = p.differing.single()
        assertEquals("나이", ageRow.label)
        assertEquals("??세", ageRow.mergedText)
        assertEquals("11세", ageRow.targetText)
        assertEquals("빈 칸 채움" to "출생 · 성격 · 2", p.filledRow)
        assertEquals("관계 겹침 · 1", p.overlapTitle)
        val overlap = p.relationOverlaps.single()
        assertEquals(K.OWEN, overlap.partnerKey)
        assertEquals("오웬의 마음", overlap.slotLabel)
        assertEquals("거래", overlap.mergedText)
        assertEquals("보호", overlap.targetText)
        assertEquals("보이스 ID" to "고정 2 · 그대로 · ◇", p.voiceRow)
        assertNull(p.mutualRow)
        assertEquals("시장 꼬마 · ‘합침 → 노아’로 휴지통", p.footnote)
        assertEquals("취소" to "합치기", p.buttons)
    }

    @Test
    fun `TC-20-17 merge`() {
        val result = CharacterMerge.merge(given, kid, K.NOAH, MergeChoices(), ctx)
        val d = result.doc
        // References moved.
        for (line in kidLines) assertEquals(K.NOAH, d.index.block(line)!!.speakerKey)
        val link = d.index.block(K.BLOCK_038)!!.runs.filterIsInstance<TextRun.Link>().single { it.linkKey == kidLink }
        assertEquals(K.NOAH, link.target.key)
        assertEquals("시장 꼬마", link.text)
        val scene = d.index.unit(K.SCENE_1_1_1)!!
        assertTrue(K.NOAH in scene.silentCast)
        assertFalse(kid in scene.silentCast)
        // 나이 keeps the target's stored value; 출생 and 성격 filled without asking.
        val noah = d.index.character(K.NOAH)!!
        assertEquals(Fx.number("11"), noah.fields[K.FIELD_AGE])
        assertEquals(Fx.year(1189), noah.fields[K.FIELD_BIRTH])
        assertNotNull(noah.fields[K.FIELD_PERSONALITY])
        assertEquals(setOf(K.FIELD_BIRTH, K.FIELD_PERSONALITY), result.record.filledFieldDefKeys.toSet())
        // The 오웬 relation keeps 노아's values; both sets of ◆ are kept.
        assertNull(d.index.relation(kidOwen))
        val owenNoah = d.index.relation(K.REL_OWEN_NOAH)!!
        assertEquals(K.KIND_PROTECTION, owenNoah.mindA?.kindKey)
        val records = RelationReader.records(d, owenNoah).map { it.key }.toSet()
        assertEquals(setOf(K.N2, kidRecord), records)
        // Fixed voice IDs unchanged.
        for ((i, line) in kidLines.take(2).withIndex()) assertEquals("NOA_0106_00$i", d.index.block(line)!!.voice?.fixedId)
        // The merged item goes to the trash as `합침 → 노아`.
        val merged = d.index.character(kid)!!
        assertNotNull(merged.deletedAt)
        val entry = d.index.trashEntry(merged.trashEntryKey!!)!!
        assertEquals(TrashReason.MERGED, entry.reason.known)
        assertEquals(K.NOAH, entry.mergedIntoKey)
        assertEquals("시장 꼬마 → 노아 합침 · 참조 5 · 되돌리기", result.toast)
        // Shared record shape with domain:trash: the merged row as it was, plus the restore counts.
        assertEquals(given.index.character(kid), CharacterMerge.mergedOriginalOf(result.record))
        assertEquals(5, CharacterMerge.summaryCount(result.record, "refs"))
        assertEquals(1, CharacterMerge.summaryCount(result.record, "relations"))
        assertEquals(2, CharacterMerge.summaryCount(result.record, "fields"))
        assertTrue(result.record in d.tombstones.mergeRecords)
    }

    @Test
    fun `TC-20-17 되돌리기 undoes every change`() {
        val result = CharacterMerge.merge(given, kid, K.NOAH, MergeChoices(), ctx)
        val (undone, _) = CharacterMerge.undo(result.doc, result)
        assertEquals(given.cast.characters, undone.cast.characters)
        assertEquals(given.cast.relations.toSet(), undone.cast.relations.toSet())
        assertEquals(given.changes.records.toSet(), undone.changes.records.toSet())
        assertEquals(given.episodes, undone.episodes)
        assertEquals(given.story, undone.story)
        assertEquals(given.trash, undone.trash)
        assertEquals(given.tombstones, undone.tombstones)
    }

    @Test
    fun `TC-20-17 a choice takes the merged value`() {
        val choices = MergeChoices(
            cells = mapOf(MergeSlot.Field(K.FIELD_AGE) to MergePick.MERGED),
            relationSlots = mapOf((K.OWEN to RelationSlotName.MIND_A) to MergePick.MERGED),
        )
        val d = CharacterMerge.merge(given, kid, K.NOAH, choices, ctx).doc
        assertEquals(FieldValue.UNKNOWN, d.index.character(K.NOAH)!!.fields[K.FIELD_AGE])
        assertEquals(listOf(K.FIELD_AGE), CharacterMerge.merge(given, kid, K.NOAH, choices, ctx).record.chosenFromMerged.mapNotNull { it.fieldDefKey })
        assertEquals(SlotValue(description = "거래"), d.index.relation(K.REL_OWEN_NOAH)!!.mindA)
    }

    @Test
    fun `merge never drops the target's history`() {
        // 리엔's 소속 lives only in records (R1 … R3, no base): it is not an empty cell.
        val stray = Fx.key("CHAR", 15)
        val d = Fx.addCharacter(Fx.doc(), stray, "떠돌이", 15, fields = mapOf(K.FIELD_AFFILIATION to Fx.text("잿더미 시장")))
        val preview = MergePlanner.preview(d, stray, K.RIEN)
        assertTrue(preview.filled.none { it.first == MergeSlot.Field(K.FIELD_AFFILIATION) })
        val row = preview.differing.single { it.slot == MergeSlot.Field(K.FIELD_AFFILIATION) }
        assertEquals("잿더미 시장" to "등불 결사", row.mergedText to row.targetText)

        val merged = CharacterMerge.merge(d, stray, K.RIEN, MergeChoices(), ctx).doc
        for (key in listOf(K.R1, K.R2, K.R3)) assertEquals(d.index.record(key), merged.index.record(key))
        val affiliation = Fx.def(merged, "소속")
        assertEquals(
            "등불 결사",
            FieldText.format(merged, affiliation, FieldReader.read(merged, EntityKind.CHARACTER, K.RIEN, affiliation, ReadPoint.LATEST).value).text,
        )
        assertNull(merged.index.character(K.RIEN)!!.fields[K.FIELD_AFFILIATION])

        // Choosing the merged value takes its base and keeps both sets of records.
        val chosen = CharacterMerge.merge(
            d, stray, K.RIEN, MergeChoices(cells = mapOf(MergeSlot.Field(K.FIELD_AFFILIATION) to MergePick.MERGED)), ctx,
        ).doc
        assertEquals(Fx.text("잿더미 시장"), chosen.index.character(K.RIEN)!!.fields[K.FIELD_AFFILIATION])
        for (key in listOf(K.R1, K.R2, K.R3)) assertEquals(d.index.record(key)!!.target, chosen.index.record(key)!!.target)
    }

    @Test
    fun `TC-20-18 mutual relation on merge`() {
        val mutual = Fx.key("RSHP", 2)
        val withMutual = given.copy(
            cast = given.cast.copy(
                relations = given.cast.relations + Relation(
                    key = mutual, endA = EntityRef(RelationEndKind.CHARACTER, kid), endB = EntityRef(RelationEndKind.CHARACTER, K.NOAH),
                    between = SlotValue(kindKey = K.KIND_FRIEND),
                ),
            ),
        )
        assertEquals("서로 관계 1 · 지워짐", MergePlanner.preview(withMutual, kid, K.NOAH).mutualRow)
        val result = CharacterMerge.merge(withMutual, kid, K.NOAH, MergeChoices(), ctx)
        assertNull(result.doc.index.relation(mutual))
        assertEquals(mutual, result.record.deletedMutualRelationKey)
    }

    @Ignore("spec conflict: TC-20-19 expects `칸 3` but TC-20-17's defaults take exactly two cells (`빈 칸 채움 · 출생 · 성격 · 2`)")
    @Test
    fun `TC-20-19 restore the merged item as stated`() {
        val merged = CharacterMerge.merge(given, kid, K.NOAH, MergeChoices(), ctx).doc
        assertEquals("옮긴 참조 5 · 관계 1 · 칸 3 · 노아에 그대로", CharacterMerge.restoreMerged(merged, kid, ctx).second)
    }

    @Test
    fun `TC-20-19 restore the merged item`() {
        val merged = CharacterMerge.merge(given, kid, K.NOAH, MergeChoices(), ctx).doc
        val (restored, line) = CharacterMerge.restoreMerged(merged, kid, ctx)
        assertEquals("옮긴 참조 5 · 관계 1 · 칸 2 · 노아에 그대로", line)
        val back = restored.index.character(kid)!!
        assertNull(back.deletedAt)
        assertEquals(given.index.character(kid)!!.fields, back.fields)
        assertTrue(restored.cast.relations.none { RelationReader.sideOf(it, kid) != null })
        // References, the relation and the taken values stay with 노아.
        for (line in kidLines) assertEquals(K.NOAH, restored.index.block(line)!!.speakerKey)
        assertEquals(Fx.year(1189), restored.index.character(K.NOAH)!!.fields[K.FIELD_BIRTH])
        assertEquals(setOf(K.N2, kidRecord), RelationReader.records(restored, restored.index.relation(K.REL_OWEN_NOAH)!!).map { it.key }.toSet())
        assertTrue(restored.trash.entries.none { it.mergedIntoKey == K.NOAH })
    }
}
