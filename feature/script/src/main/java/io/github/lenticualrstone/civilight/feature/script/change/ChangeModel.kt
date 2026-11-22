package io.github.lenticualrstone.civilight.feature.script.change

import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.relation.KindChip
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** Segments of `변화 기록` (SCR-41, board `ChangeCreate`): `관계 | 필드 | 용어 공개`. */
enum class ChangeSegment(val label: String) {
    RELATION(ChangeCopy.SEG_RELATION),
    FIELD(ChangeCopy.SEG_FIELD),
    TERM(ChangeCopy.SEG_TERM),
}

/** Which side a character pick fills. */
enum class PickRole { VIEWER, PARTNER, FIELD_OWNER }

/**
 * What the writer has chosen so far in the `변화 기록` sheet of [blockKey]. Pickers stack over the
 * sheet, so the draft lives in the view model, not in the sheet's composition.
 */
data class ChangeDraft(
    val blockKey: Key,
    val segment: ChangeSegment = ChangeSegment.RELATION,
    // 관계
    val viewerKey: Key? = null,
    val partnerKey: Key? = null,
    val slots: List<SheetSlot> = listOf(SheetSlot.VIEWER_MIND),
    val slotDrafts: Map<SheetSlot, SlotDraft> = emptyMap(),
    // 필드
    val ownerKey: Key? = null,
    /** A field definition, or [DISPLAY_NAME] for `표시 이름`. */
    val fieldKey: String? = null,
    val fieldText: String = "",
    val fieldOption: Key? = null,
    // 용어 공개
    val termKey: Key? = null,
) {
    companion object {
        /** [fieldKey] of the character's `표시 이름` (a character-level timed value, CST-11). */
        const val DISPLAY_NAME = "displayName"

        /**
         * A new draft (SCR-41 `[PROPOSED]`): the block's speaker fills one side and the other starts as
         * `인물 고르기`. As drawn on `ChangeCreate` (`리엔 · 하르트` on 하르트's line), the speaker is the
         * second chip, so the first `바꾸는 것` chip reads `{the other}의 마음` toward the speaker.
         */
        fun start(doc: ProjectDoc, blockKey: Key): ChangeDraft {
            val block = doc.index.block(blockKey)
            val speaker = block?.takeIf { it.kind.known == BlockKind.DIALOGUE }?.speakerKey?.takeIf { doc.index.character(it)?.deletedAt == null }
            return ChangeDraft(blockKey, partnerKey = speaker, ownerKey = speaker)
        }
    }
}

/** A `바꾸는 것` chip of the relation segment. */
data class SlotChip(val slot: SheetSlot, val label: String, val selected: Boolean)

/** One chosen slot: `값 · 만나기 전 →`, its kind chips (or the 호칭 text field), the current draft. */
data class SlotSection(val slot: SheetSlot, val valueLabel: String, val chips: List<KindChip>, val draft: SlotDraft, val addKind: String?)

/** The relation segment (cast `RelationDrafts.changeView`, CST-30). */
data class RelationSegment(
    val viewer: String,
    val partner: String,
    val slotChips: List<SlotChip>,
    val sections: List<SlotSection>,
    val reflectRow: Pair<String, String>?,
)

/** A field the `필드` segment can record (types with an editor here). */
data class FieldChoice(val key: String, val label: String, val selected: Boolean)

/** The field segment: owner chip, field chips, `값 · {now} →`, a text field or option chips. */
data class FieldSegment(
    val owner: String,
    val fields: List<FieldChoice>,
    val valueLabel: String?,
    val options: List<Pair<Key, String>>?,
    val textInput: Boolean,
    val number: Boolean,
)

/** The term segment: term chip and `첫 공개 {old} → {episode} #{n}` when the reveal moves. */
data class TermSegment(val term: String, val movePreview: String?)

/** The whole sheet (SCR-41). */
data class ChangeSheetModel(
    val title: String,
    val cancel: String,
    val sub: String,
    val segment: ChangeSegment,
    val relation: RelationSegment?,
    val field: FieldSegment?,
    val term: TermSegment?,
    /** `시작 시점 · 이 블록 · 1-7 #043`. */
    val startRow: Pair<String, String>,
    val primary: String,
    val canRecord: Boolean,
)

object ChangeModel {
    /** Field types with an editor in this sheet; others are recorded from the profile (CST-10). */
    val EDITABLE_TYPES: Set<FieldType> = setOf(FieldType.SHORT_TEXT, FieldType.NUMBER, FieldType.CHOICE)

    /**
     * The sheet for [draft]. With [unlocked] false (rule 19, SCR-58), a hidden character's name in the
     * chips reads `숨긴 인물` and a hidden term `숨긴 항목`.
     */
    fun of(doc: ProjectDoc, draft: ChangeDraft, unlocked: Boolean = true): ChangeSheetModel? {
        val block = doc.index.block(draft.blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return null
        val start = Records.startRow(doc, block.key).orEmpty()
        val relation = if (draft.segment == ChangeSegment.RELATION) relation(doc, draft, unlocked) else null
        val field = if (draft.segment == ChangeSegment.FIELD) field(doc, draft, unlocked) else null
        val term = if (draft.segment == ChangeSegment.TERM) term(doc, draft, unlocked) else null
        return ChangeSheetModel(
            title = RelationDrafts.CHANGE_TITLE,
            cancel = RelationDrafts.CANCEL,
            sub = Records.sheetSubline(doc, block.key).orEmpty(),
            segment = draft.segment,
            relation = relation,
            field = field,
            term = term,
            startRow = ChangeCopy.START to start.removePrefix(ChangeCopy.START + " · "),
            primary = RelationDrafts.RECORD,
            canRecord = canRecord(doc, draft),
        )
    }

    private fun name(doc: ProjectDoc, key: Key?, unlocked: Boolean = true): String = when {
        key == null -> ChangeCopy.PICK_CHARACTER
        !unlocked && doc.index.character(key)?.hidden == true -> ChangeCopy.HIDDEN_CHARACTER
        else -> Labels.speakerName(doc, key) ?: ChangeCopy.PICK_CHARACTER
    }

    fun relation(doc: ProjectDoc, draft: ChangeDraft, unlocked: Boolean = true): RelationSegment {
        val viewer = draft.viewerKey
        val partner = draft.partnerKey
        val viewerName = name(doc, viewer, unlocked)
        val partnerName = name(doc, partner, unlocked)
        val view = if (viewer != null && partner != null && viewer != partner) {
            RelationDrafts.changeView(doc, draft.blockKey, viewer, partner, draft.slots.toSet())
        } else null
        val chips = RelationDrafts.changeSlots(viewerName, partnerName).map { (slot, label) -> SlotChip(slot, label, slot in draft.slots) }
        val sections = view?.sections.orEmpty().map { s ->
            SlotSection(
                s.slot, s.valueLabel, s.chips, draft.slotDrafts[s.slot] ?: SlotDraft(),
                addKind = view?.addKindLabel?.takeIf { s.slot != SheetSlot.ADDRESS },
            )
        }
        return RelationSegment(viewerName, partnerName, chips, sections, view?.reflectRow)
    }

    /** `표시 이름`, then the character's fields of an editable type, in profile order. */
    fun fieldChoices(doc: ProjectDoc, ownerKey: Key, selected: String?): List<FieldChoice> {
        val defs = FieldDefinitions.visibleFor(doc, EntityKind.CHARACTER, ownerKey).filter { it.type.known in EDITABLE_TYPES }
        return listOf(FieldChoice(ChangeDraft.DISPLAY_NAME, ChangeCopy.DISPLAY_NAME, selected == ChangeDraft.DISPLAY_NAME)) +
            defs.map { FieldChoice(it.key, it.name, it.key == selected) }
    }

    fun fieldDef(doc: ProjectDoc, draft: ChangeDraft): FieldDef? =
        draft.fieldKey?.takeIf { it != ChangeDraft.DISPLAY_NAME }?.let { key -> doc.index.fieldDefs(EntityKind.CHARACTER).firstOrNull { it.key == key } }

    fun field(doc: ProjectDoc, draft: ChangeDraft, unlocked: Boolean = true): FieldSegment {
        val owner = draft.ownerKey?.takeIf { doc.index.character(it) != null }
        val ownerName = name(doc, owner, unlocked)
        if (owner == null) return FieldSegment(ownerName, emptyList(), null, null, textInput = false, number = false)
        val fields = fieldChoices(doc, owner, draft.fieldKey)
        val point = ReadPoint.BeforeBlock(draft.blockKey)
        if (draft.fieldKey == ChangeDraft.DISPLAY_NAME) {
            val now = DisplayNames.read(doc, owner, point).shown
            return FieldSegment(ownerName, fields, ChangeCopy.valueLabel(now), null, textInput = true, number = false)
        }
        val def = fieldDef(doc, draft) ?: return FieldSegment(ownerName, fields, null, null, textInput = false, number = false)
        val stored = FieldReader.storedAt(doc, EntityKind.CHARACTER, owner, def, point).first
        val now = FieldText.format(doc, def, stored).text.ifEmpty { ChangeCopy.EMPTY_VALUE }
        val choice = def.type.known == FieldType.CHOICE
        val options = if (choice) def.options.orEmpty().sortedBy { it.order }.map { it.key to it.label } else null
        return FieldSegment(ownerName, fields, ChangeCopy.valueLabel(now), options, textInput = !choice, number = def.type.known == FieldType.NUMBER)
    }

    fun term(doc: ProjectDoc, draft: ChangeDraft, unlocked: Boolean = true): TermSegment {
        val term = draft.termKey?.let { doc.index.term(it) }
        val masked = term != null && term.hidden && !unlocked
        val name = if (masked) ChangeCopy.HIDDEN_ITEM else term?.name ?: ChangeCopy.PICK_TERM
        return TermSegment(name, term?.takeIf { !masked }?.let { Records.revealMovePreview(doc, it.key, draft.blockKey) })
    }

    /** `기록` is enabled once the chosen segment names something to write. */
    fun canRecord(doc: ProjectDoc, draft: ChangeDraft): Boolean = when (draft.segment) {
        ChangeSegment.RELATION -> {
            val viewer = draft.viewerKey
            val partner = draft.partnerKey
            viewer != null && partner != null && viewer != partner && draft.slots.isNotEmpty() &&
                draft.slots.all { slot -> draft.slotDrafts[slot]?.let { it.kindKey != null || it.address.isNotBlank() } == true }
        }
        ChangeSegment.FIELD -> when {
            draft.ownerKey == null || draft.fieldKey == null -> false
            draft.fieldKey == ChangeDraft.DISPLAY_NAME -> true
            else -> fieldDef(doc, draft)?.let { def ->
                if (def.type.known == FieldType.CHOICE) draft.fieldOption != null else draft.fieldText.isNotBlank()
            } == true
        }
        ChangeSegment.TERM -> {
            val term = draft.termKey
            term != null && Records.existingReveal(doc, term)?.anchor?.let { (it as? io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock)?.blockKey } != draft.blockKey
        }
    }
}
