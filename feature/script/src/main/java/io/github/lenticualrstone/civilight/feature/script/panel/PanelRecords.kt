package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.script.Anchors
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Panels
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.recordBeforeBlock
import kotlinx.serialization.builtins.serializer

/** Where `고치기` on a `◆` card goes (SCR-32 inner): the record's editor in the cast area. */
sealed interface RecordEdit {
    /** The shared `RelationEditSheet` of the relation. */
    data class Relation(val relationKey: Key) : RecordEdit

    /** The shared `FieldValueEditSheet` of the field. */
    data class Field(val kind: EntityKind, val entityKey: Key, val fieldDefKey: Key) : RecordEdit

    /** The character's record (표시 이름 history). */
    data class Profile(val characterKey: Key) : RecordEdit
}

/**
 * A record card of the `시간` tab (SCR-32): `◆ 관계 · 리엔의 마음 → 하르트` with `만나기 전 → 경계 ·
 * 1-7 #043부터`; `◇ 첫 공개 · 오리지늄 등불` with `이 블록 · 1-7 #044`; an orphaned record with
 * `근거 #043 지워짐 · #044로 옮겨 옴` (dotted).
 */
data class RecordCard(
    val recordKey: Key,
    val reveal: Boolean,
    val title: String,
    val line: String,
    val dotted: Boolean,
    val edit: RecordEdit?,
    /** `관계도에서 보기` focus (relation records only). */
    val mapFocus: Key?,
    /** Flagged `근거 지워짐` (SCR-43): the card shows `다른 블록에 잇기 · 이대로 두기 · 기록 지우기`. */
    val orphan: Boolean = false,
    /** The deleted original block, while it sits in the trash: `#043 · 휴지통 · 30일 · 되살리면 다시 이어짐` (SCR-39). */
    val trashNote: TrashNote? = null,
    /** The record's subject is a deleted item (SCR-57): `대상 지워짐` with `되살리기 · 이대로 두기 · 기록 지우기`. */
    val deletedSubject: DeletedSubject? = null,
)

/** The note under orphan cards (SCR-43, board `RSOrphan`) and the block it names (SCR-39 restore). */
data class TrashNote(val text: String, val blockKey: Key)

/** Records anchored on blocks, as the panel reads them (rule 12, SCR-32/34). */
object PanelRecords {
    /** Live records whose seat is [blockKey], in stored order. */
    fun onBlock(doc: ProjectDoc, blockKey: Key): List<ChangeRecord> = doc.changes.records.filter {
        it.deletedAt == null && (it.anchor as? Anchor.AtBlock)?.blockKey == blockKey
    }

    fun isReveal(record: ChangeRecord): Boolean = record.target is ChangeTarget.TermReveal

    /** `변화 연결` line (board `WideProps`): `◆ 관계 변화 · 리엔 → 하르트`, `◇ 첫 공개 · 오리지늄 등불`. */
    fun changeLink(doc: ProjectDoc, record: ChangeRecord): String =
        if (isReveal(record)) "◇ ${Records.shortTitle(doc, record)}"
        else ScriptCopy.changeLink(Records.kindLabel(doc, record), Records.subject(doc, record))

    fun card(doc: ProjectDoc, record: ChangeRecord, blockKey: Key): RecordCard {
        val reveal = isReveal(record)
        val dotted = Anchors.isDotted(record) || !doc.liveness.isTargetLive(record.target)
        val location = Records.anchorLocation(doc, record.anchor)
        val title = if (reveal) "◇ ${Records.shortTitle(doc, record)}"
        else "◆ ${Records.kindLabel(doc, record)} · ${Records.subject(doc, record, possessive = true)}"
        val flagged = record.anchorState.known == AnchorState.ORPHANED
        val orphanCard = if (Anchors.isDotted(record)) Panels.orphanCard(doc, record) else null
        val orphan = orphanCard?.first
        val trashNote = orphanCard?.second?.let { text ->
            (record.originalAnchor as? Anchor.AtBlock)?.blockKey?.let { TrashNote(text, it) }
        }
        val deletedSubject = if (orphan == null) SubjectDeleted.of(doc, record) else null
        val line = when {
            orphan != null -> orphan
            deletedSubject != null -> SubjectDeleted.LABEL
            reveal -> ScriptCopy.revealHere(location)
            else -> change(doc, record, blockKey)?.let { (before, after) -> "$before → $after · ${location}부터" } ?: "${location}부터"
        }
        return RecordCard(
            record.key, reveal, title, line, dotted, editOf(record).takeIf { !reveal }, mapFocusOf(doc, record),
            orphan = flagged, trashNote = trashNote.takeIf { flagged }, deletedSubject = deletedSubject,
        )
    }

    /** The value just before [blockKey] and the record's value (rule 11 §2), as words. */
    fun change(doc: ProjectDoc, record: ChangeRecord, blockKey: Key): Pair<String, String>? = when (val t = record.target) {
        is ChangeTarget.RelationSlot -> {
            val before = doc.recordBeforeBlock(t, blockKey)?.let { relationWord(doc, t, it) } ?: relationBase(doc, t) ?: ScriptCopy.NOT_MET
            before to (relationWord(doc, t, record) ?: ScriptCopy.NOT_MET)
        }
        is ChangeTarget.DisplayName -> {
            val c = doc.index.character(t.characterKey)
            val name = c?.name ?: "?"
            val before = doc.recordBeforeBlock(t, blockKey)?.valueAs(String.serializer()) ?: c?.displayNameBase ?: name
            before to (record.valueAs(String.serializer()) ?: name)
        }
        is ChangeTarget.Field -> {
            val kind = t.entityKind.known
            val def = doc.index.fieldDef(t.fieldDefKey)
            if (kind == null || def == null) null
            else {
                val before = FieldReader.storedAt(doc, kind, t.entityKey, def, ReadPoint.BeforeBlock(blockKey)).first
                FieldText.format(doc, def, before).text to FieldText.format(doc, def, record.valueAs(FieldValue.serializer())).text
            }
        }
        else -> null
    }

    private fun isAddress(t: ChangeTarget.RelationSlot) =
        t.slot.known == RelationSlotName.ADDRESS_A || t.slot.known == RelationSlotName.ADDRESS_B

    private fun relationWord(doc: ProjectDoc, t: ChangeTarget.RelationSlot, record: ChangeRecord): String? =
        if (isAddress(t)) record.valueAs(String.serializer())?.let { "‘$it’" }
        else Panels.slotWord(doc, record.valueAs(SlotValue.serializer()))

    private fun relationBase(doc: ProjectDoc, t: ChangeTarget.RelationSlot): String? {
        val r = doc.index.relation(t.relationKey) ?: return null
        return when (t.slot.known) {
            RelationSlotName.BETWEEN -> Panels.slotWord(doc, r.between)
            RelationSlotName.MIND_A -> Panels.slotWord(doc, r.mindA)
            RelationSlotName.MIND_B -> Panels.slotWord(doc, r.mindB)
            RelationSlotName.ADDRESS_A -> r.addressA?.let { "‘$it’" }
            RelationSlotName.ADDRESS_B -> r.addressB?.let { "‘$it’" }
            null -> null
        }
    }

    private fun editOf(record: ChangeRecord): RecordEdit? = when (val t = record.target) {
        is ChangeTarget.RelationSlot -> RecordEdit.Relation(t.relationKey)
        is ChangeTarget.DisplayName -> RecordEdit.Profile(t.characterKey)
        is ChangeTarget.Field -> t.entityKind.known?.let { RecordEdit.Field(it, t.entityKey, t.fieldDefKey) }
        else -> null
    }

    /** The character whose side the relation record is on (`리엔의 마음` → 리엔). */
    private fun mapFocusOf(doc: ProjectDoc, record: ChangeRecord): Key? {
        val t = record.target as? ChangeTarget.RelationSlot ?: return null
        val r = doc.index.relation(t.relationKey) ?: return null
        return when (t.slot.known) {
            RelationSlotName.MIND_B, RelationSlotName.ADDRESS_B -> r.endB.key
            else -> r.endA.key
        }
    }

    /** `#043`. */
    fun number(doc: ProjectDoc, blockKey: Key): String? = doc.index.block(blockKey)?.let { Labels.number(it.number) }
}
