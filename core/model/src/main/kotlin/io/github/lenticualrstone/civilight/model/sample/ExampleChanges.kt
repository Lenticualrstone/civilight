package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RegionShapeValue
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.file.ChangesFile
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.DEVICE
import io.github.lenticualrstone.civilight.model.sample.ExampleClock.at
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * `changes.json` of the example: STO Fixture R (R1 … R9, with CST/CNV's 호칭 records R5a and R7a),
 * one `◇` per term, two world-event records, a region shape, 노아's two records and one record in
 * the trash.
 */
internal object ExampleChanges {
    val TRASHED_RECORD_DELETED_AT: Long = at("2026-11-18T03:00:00Z")

    val affiliationOfRien = ChangeTarget.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION)

    private fun text(value: String): JsonElement =
        ChangeRecord.encodeValue(FieldValue.serializer(), FieldValue.of(FieldPayload.Texts(listOf(value))))

    private fun slot(kind: Key, description: String? = null): JsonElement =
        ChangeRecord.encodeValue(SlotValue.serializer(), SlotValue(kindKey = kind, description = description))

    private fun address(value: String): JsonElement = ChangeRecord.encodeValue(String.serializer(), value)

    private fun slotOf(relation: Key, slot: RelationSlotName) = ChangeTarget.RelationSlot(relation, slot)

    private fun record(
        key: Key,
        target: ChangeTarget,
        anchor: Anchor,
        value: JsonElement?,
        createdAt: String,
        note: String? = null,
    ): ChangeRecord {
        val time = at(createdAt)
        return ChangeRecord(
            key = key, target = target, value = value, note = note, anchor = anchor, createdAt = time,
            updatedAt = time, updatedBy = DEVICE,
        )
    }

    fun records(): List<ChangeRecord> {
        val atHart = Anchor.AtBlock(K.BLOCK_043)
        val from19 = Anchor.UnitStart(K.EPISODE_1_9)
        val blackout = Anchor.WorldEvent(K.EVENT_BLACKOUT)
        val live = listOf(
            record(K.R1, affiliationOfRien, Anchor.UnitStart(K.EPISODE_1_1), text("없음"), "2026-04-01T12:00:00Z", note = "하층 구역의 고아"),
            record(K.R2, affiliationOfRien, Anchor.UnitStart(K.EPISODE_1_3), text("등불 결사"), "2026-04-26T12:00:00Z", note = "세라의 안내로 합류"),
            record(K.R3, affiliationOfRien, Anchor.UnitStart(K.EPISODE_2_5), text("등불 결사 · 등불지기장"), "2026-06-15T12:00:00Z"),
            record(
                K.R4, slotOf(K.REL_RIEN_HART, RelationSlotName.MIND_A), atHart,
                slot(K.KIND_WARINESS, "불빛을 아는 낯선 사람"), "2026-11-21T12:00:00Z", note = "처음 만남",
            ),
            record(K.R5, slotOf(K.REL_RIEN_HART, RelationSlotName.MIND_B), atHart, slot(K.KIND_PROTECTION), "2026-11-21T12:00:01Z"),
            record(K.R5A, slotOf(K.REL_RIEN_HART, RelationSlotName.ADDRESS_B), atHart, address("꼬마"), "2026-11-21T12:00:02Z"),
            record(K.R6, slotOf(K.REL_RIEN_HART, RelationSlotName.BETWEEN), from19, slot(K.KIND_COLLEAGUE), "2026-11-21T12:10:00Z"),
            record(K.R7, slotOf(K.REL_RIEN_HART, RelationSlotName.MIND_A), from19, slot(K.KIND_TRUST), "2026-11-21T12:10:01Z"),
            record(K.R7A, slotOf(K.REL_RIEN_HART, RelationSlotName.ADDRESS_A), from19, address("하르트"), "2026-11-21T12:10:02Z"),
            // value null: show 이름 from here on.
            record(K.R8, ChangeTarget.DisplayName(K.HART), from19, null, "2026-11-21T12:10:03Z", note = "이름 공개"),
            record(K.R9, ChangeTarget.TermReveal(K.TERM_ORIGINIUM_LANTERN), Anchor.AtBlock(K.BLOCK_044), null, "2026-11-21T12:20:00Z"),
            record(K.REVEAL_BLACKOUT, ChangeTarget.TermReveal(K.TERM_BLACKOUT), Anchor.AtBlock(K.BLOCK_B41), null, "2026-05-10T12:00:00Z"),
            record(
                K.REVEAL_LANTERN_SOCIETY, ChangeTarget.TermReveal(K.TERM_LANTERN_SOCIETY), Anchor.AtBlock(K.BLOCK_1_1_001), null,
                "2026-04-01T11:30:00Z",
            ),
            record(K.REVEAL_EMBER_OATH, ChangeTarget.TermReveal(K.TERM_EMBER_OATH), Anchor.UnitStart(K.EPISODE_2_5), null, "2026-06-15T12:05:00Z"),
            record(K.REVEAL_UPPER_GRID, ChangeTarget.TermReveal(K.TERM_UPPER_GRID), Anchor.UnitStart(K.CHAPTER_2), null, "2026-06-15T12:06:00Z"),
            record(
                K.W1, ChangeTarget.Field(EntityKind.CHARACTER, K.MARTHA, K.FIELD_AFFILIATION), blackout, text("등불 결사"),
                "2026-03-05T12:00:00Z", note = "대정전 뒤 경비대를 떠남",
            ),
            record(
                K.W2, ChangeTarget.Field(EntityKind.PLACE, K.PLACE_LOWER_STREET, K.FIELD_PLACE_STATE), blackout, text("대정전 이후"),
                "2026-03-05T12:01:00Z",
            ),
            record(
                K.REGION_SHAPE_1_9, ChangeTarget.RegionShape(K.MAP_WORLD, K.REGION_UPPER_GUARD), from19,
                ChangeRecord.encodeValue(RegionShapeValue.serializer(), RegionShapeValue(ExampleMaps.UPPER_GUARD_SHAPE_1_9)),
                "2026-06-20T12:00:00Z",
            ),
        )
        // Not live: their target (노아, his relation) is in the trash; then a directly trashed record.
        val noah = listOf(
            record(
                K.N1, ChangeTarget.Field(EntityKind.CHARACTER, K.NOAH, K.FIELD_AFFILIATION), Anchor.AtBlock(K.BLOCK_1_6_007),
                text("잿더미 시장 심부름꾼"), "2026-07-01T12:00:00Z",
            ),
            record(K.N2, slotOf(K.REL_OWEN_NOAH, RelationSlotName.MIND_A), Anchor.AtBlock(K.BLOCK_1_6_025), slot(K.KIND_PROTECTION), "2026-07-01T12:30:00Z"),
        )
        val trashed = record(
            K.RECORD_TRASHED, ChangeTarget.Field(EntityKind.CHARACTER, K.SERA, K.FIELD_CODENAME), Anchor.UnitStart(K.EPISODE_1_5),
            text("기록관"), "2026-06-01T12:00:00Z",
        ).copy(deletedAt = TRASHED_RECORD_DELETED_AT, trashEntryKey = K.TRASH_RECORD, updatedAt = TRASHED_RECORD_DELETED_AT)
        return live + noah + trashed
    }

    fun changesFile(): ChangesFile = ChangesFile(records())
}
