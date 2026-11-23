package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.recordAt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.PairHolder
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open

/**
 * A slot as a relation sheet names it, from the viewer's side (rule 13 §7): `사이`, `<viewer>의 마음`,
 * `<partner>의 마음`, `호칭` (what the viewer calls the partner).
 */
enum class SheetSlot { BETWEEN, VIEWER_MIND, PARTNER_MIND, ADDRESS }

/**
 * A kind chip of a relation sheet. A pair kind gives two chips, one per role of the partner:
 * picking `선배` in 리엔's sheet for 세라 means 세라 holds `선배` (CST-29), a fact, not a guess.
 */
data class KindChip(val kindKey: Key, val label: String, val partnerHoldsFirst: Boolean? = null)

/** The typed value of one slot: a kind chip (or none), an optional description, or the 호칭 text. */
data class SlotDraft(
    val kindKey: Key? = null,
    val partnerHoldsFirst: Boolean? = null,
    val description: String = "",
    val address: String = "",
) {
    fun picks(chip: KindChip): Boolean = kindKey == chip.kindKey && partnerHoldsFirst == chip.partnerHoldsFirst
}

/** One `바꾸는 것` section of the change sheet (`값 · 만나기 전 →` + kind chips). */
data class ChangeSection(val slot: SheetSlot, val valueLabel: String, val chips: List<KindChip>)

/** The `관계` segment of the script's `◆ 변화 기록` sheet (`ChangeCreate`, CST-30). */
data class RelationChangeView(
    val title: String,
    val cancelLabel: String,
    /** `#043 · 1-7 씬 3`. */
    val sub: String,
    val viewerName: String,
    val partnerName: String,
    /** `리엔의 마음 · 하르트의 마음 · 사이 · 호칭` with the chosen ones. */
    val slotChips: List<Pair<SheetSlot, String>>,
    val sections: List<ChangeSection>,
    val addKindLabel: String,
    val startRow: Pair<String, String>,
    val reflectRow: Pair<String, String>,
    val primaryLabel: String,
)

/**
 * Shared pieces of the relation sheets (CST-29 `+ 관계`, CST-30 change from a block): slot names
 * from the viewer's side, kind chips, the value a draft writes and the text a value reads as.
 */
object RelationDrafts {
    const val CHANGE_TITLE = "변화 기록"
    const val CANCEL = "취소"
    const val BETWEEN = "사이"
    const val ADDRESS = "호칭"
    const val CHANGE_ITEMS = "바꾸는 것"
    const val START = "시작 시점"
    const val REFLECT = "반영"
    const val REFLECT_VALUE = "인물 · 관계도 · 시점 렌즈"
    const val RECORD = "기록"

    /** `리엔의 마음`. */
    fun mindLabel(name: String): String = "${name}의 마음"

    /** Chip order of the `+ 관계` sheet: `사이 · <A>의 마음 · <B>의 마음 · 호칭`. */
    fun sheetSlots(viewerName: String, partnerName: String): List<Pair<SheetSlot, String>> = listOf(
        SheetSlot.BETWEEN to BETWEEN,
        SheetSlot.VIEWER_MIND to mindLabel(viewerName),
        SheetSlot.PARTNER_MIND to mindLabel(partnerName),
        SheetSlot.ADDRESS to ADDRESS,
    )

    /** Chip order of the change sheet (`ChangeCreate`): `리엔의 마음 · 하르트의 마음 · 사이 · 호칭`. */
    fun changeSlots(viewerName: String, partnerName: String): List<Pair<SheetSlot, String>> = listOf(
        SheetSlot.VIEWER_MIND to mindLabel(viewerName),
        SheetSlot.PARTNER_MIND to mindLabel(partnerName),
        SheetSlot.BETWEEN to BETWEEN,
        SheetSlot.ADDRESS to ADDRESS,
    )

    /**
     * The stored slot. [relation] null = the pair has none yet; it will be created with the viewer
     * as end A ([RelationEdits.openOrCreate] puts its first argument there).
     */
    fun slotName(relation: Relation?, viewerKey: Key, slot: SheetSlot): RelationSlotName {
        val side = relation?.let { RelationReader.sideOf(it, viewerKey) } ?: Side.A
        return when (slot) {
            SheetSlot.BETWEEN -> RelationSlotName.BETWEEN
            SheetSlot.VIEWER_MIND -> RelationReader.mindSlot(side)
            SheetSlot.PARTNER_MIND -> RelationReader.mindSlot(side.other)
            SheetSlot.ADDRESS -> RelationReader.addressSlot(side)
        }
    }

    /** Kind chips of a slot (none for `호칭`); a pair kind gives `선배` and `후배` for the partner. */
    fun kindChips(doc: ProjectDoc, slot: SheetSlot): List<KindChip> = when (slot) {
        SheetSlot.ADDRESS -> emptyList()
        SheetSlot.BETWEEN -> RelationKinds.of(doc, RelationCategory.BETWEEN).flatMap { k ->
            if (k.symmetry?.known == RelationSymmetry.PAIR) {
                listOf(KindChip(k.key, k.name, partnerHoldsFirst = true), KindChip(k.key, k.pairName ?: k.name, partnerHoldsFirst = false))
            } else {
                listOf(KindChip(k.key, k.name))
            }
        }
        SheetSlot.VIEWER_MIND, SheetSlot.PARTNER_MIND -> RelationKinds.of(doc, RelationCategory.MIND).map { KindChip(it.key, it.name) }
    }

    /** The draft of a slot's value now in effect at [point] (empty when nothing is set). */
    fun draftOf(doc: ProjectDoc, relation: Relation?, viewerKey: Key, slot: SheetSlot, point: ReadPoint): SlotDraft {
        relation ?: return SlotDraft()
        val state = RelationReader.state(doc, relation, point)
        val side = RelationReader.sideOf(relation, viewerKey) ?: return SlotDraft()
        return when (slot) {
            SheetSlot.ADDRESS -> SlotDraft(address = state.address(side).orEmpty())
            SheetSlot.BETWEEN -> state.between?.let { v ->
                val kind = RelationReader.kind(doc, v)
                val pair = kind?.symmetry?.known == RelationSymmetry.PAIR
                val holder = when (v.pairFirstHolder?.known) {
                    PairHolder.A -> Side.A
                    PairHolder.B -> Side.B
                    null -> null
                }
                SlotDraft(
                    kindKey = kind?.key,
                    partnerHoldsFirst = if (pair && holder != null) holder == side.other else null,
                    description = v.description.orEmpty(),
                )
            } ?: SlotDraft()
            SheetSlot.VIEWER_MIND, SheetSlot.PARTNER_MIND -> {
                val v = state.mind(if (slot == SheetSlot.VIEWER_MIND) side else side.other)
                SlotDraft(kindKey = v?.let { RelationReader.kind(doc, it)?.key }, description = v?.description.orEmpty())
            }
        }
    }

    /**
     * The value [draft] writes. A pair kind gets its first-name holder from the chip (the partner or
     * the viewer); nothing chosen and nothing typed clears the slot.
     */
    fun input(doc: ProjectDoc, relation: Relation?, viewerKey: Key, partnerKey: Key, slot: SheetSlot, draft: SlotDraft): SlotInput {
        if (slot == SheetSlot.ADDRESS) return SlotInput.Address(draft.address.trim().nfc().takeIf { it.isNotEmpty() })
        val description = draft.description.trim().nfc().takeIf { it.isNotEmpty() }
        val kind = draft.kindKey?.let { doc.index.relationKind(it) }
        if (kind == null) return SlotInput.Kind(description?.let { SlotValue(description = it) })
        if (slot != SheetSlot.BETWEEN) return SlotInput.Kind(RelationEdits.mindValue(kind, description))
        val pair = kind.symmetry?.known == RelationSymmetry.PAIR
        val value = if (!pair) {
            SlotValue(kindKey = kind.key, description = description)
        } else {
            // The holder's side in the relation-to-be: viewer = A when the pair has no relation yet.
            val viewerSide = relation?.let { RelationReader.sideOf(it, viewerKey) } ?: Side.A
            val holderSide = if (draft.partnerHoldsFirst != false) viewerSide.other else viewerSide
            SlotValue(kindKey = kind.key, description = description, pairFirstHolder = (if (holderSide == Side.A) PairHolder.A else PairHolder.B).open())
        }
        return SlotInput.Kind(value)
    }

    /** True when [draft] writes something different from the value in effect ([current]). */
    fun changes(doc: ProjectDoc, relation: Relation?, viewerKey: Key, partnerKey: Key, slot: SheetSlot, draft: SlotDraft, current: SlotDraft): Boolean =
        RelationEdits.encode(input(doc, relation, viewerKey, partnerKey, slot, draft)) != RelationEdits.encode(input(doc, relation, viewerKey, partnerKey, slot, current))

    /** A slot value read from the viewer's side: `선배 · 의지하는 선배`, `경계`, `‘꼬마’`-style 호칭 text; empty for nothing. */
    fun valueText(doc: ProjectDoc, relation: Relation?, viewerKey: Key, slot: SheetSlot, draft: SlotDraft): String {
        if (slot == SheetSlot.ADDRESS) return draft.address
        val kind = draft.kindKey?.let { doc.index.relationKind(it) }
        val word = when {
            kind == null -> null
            slot == SheetSlot.BETWEEN && kind.symmetry?.known == RelationSymmetry.PAIR ->
                if (draft.partnerHoldsFirst == false) kind.pairName ?: kind.name else kind.name
            else -> kind.name
        }
        return CastCopy.join(word, draft.description.takeIf { it != word })
    }

    /** The text of a stored slot record from the viewer's side (history rows, the two-button sheet). */
    fun recordText(doc: ProjectDoc, relation: Relation, viewerKey: Key, record: ChangeRecord): String {
        val slot = (record.target as? io.github.lenticualrstone.civilight.model.change.ChangeTarget.RelationSlot)?.slot?.known ?: return ""
        if (RelationReader.isAddress(slot)) return RelationReader.addressOf(record).orEmpty()
        val value = RelationReader.slotValueOf(record) ?: return ""
        val side = RelationReader.sideOf(relation, viewerKey) ?: Side.A
        val word = if (slot == RelationSlotName.BETWEEN) RelationReader.roleOf(doc, value, side.other) else RelationReader.kindName(doc, value)
        return CastCopy.join(word, value.description?.takeIf { it != word })
    }

    /** `+ 새 종류` of a slot: a `사이` kind is added as `대칭` (pair kinds come from `관계 종류` settings, CST-32). */
    fun addKind(doc: ProjectDoc, slot: SheetSlot, name: String, ctx: EditContext): Pair<ProjectDoc, KindChip>? {
        if (slot == SheetSlot.ADDRESS || name.isBlank()) return null
        val category = if (slot == SheetSlot.BETWEEN) RelationCategory.BETWEEN else RelationCategory.MIND
        val existing = RelationKinds.of(doc, category).firstOrNull { it.name == name.trim().nfc() }
        if (existing != null) return doc to KindChip(existing.key, existing.name, if (existing.symmetry?.known == RelationSymmetry.PAIR) true else null)
        val (next, kind) = RelationKinds.add(doc, category, name, ctx)
        return next to KindChip(kind.key, kind.name)
    }

    /**
     * (0.2.0, CST-76) `만들기` of the relation-kind create card in a relation sheet: the category comes from
     * the slot (`사이` or `<A>의 마음`, never asked). A `사이` kind with [pairName] filled is a 짝 kind
     * (`선배 ↔ 후배`) whose [name] the partner holds (as 세라 holds `선배` in 리엔's sheet); empty makes a
     * 대칭 kind. A `마음` kind has a name only. An existing kind of the same name and category is picked.
     */
    fun addKindFromCard(doc: ProjectDoc, slot: SheetSlot, name: String, pairName: String?, ctx: EditContext): Pair<ProjectDoc, KindChip>? {
        if (slot == SheetSlot.ADDRESS || name.isBlank()) return null
        val pair = pairName?.trim()?.nfc()?.takeIf { it.isNotEmpty() && slot == SheetSlot.BETWEEN }
            ?: return addKind(doc, slot, name, ctx)
        val existing = RelationKinds.of(doc, RelationCategory.BETWEEN).firstOrNull { it.name == name.trim().nfc() }
        if (existing != null) return addKind(doc, slot, name, ctx)
        val (next, kind) = RelationKinds.add(doc, RelationCategory.BETWEEN, name, ctx, RelationSymmetry.PAIR, pair)
        return next to KindChip(kind.key, kind.name, true)
    }

    // --- CST-30: the `관계` segment of `◆ 변화 기록` ---

    /** The segment at [blockKey] for the pair; values read just before the block (`만나기 전` for the first record). */
    fun changeView(doc: ProjectDoc, blockKey: Key, viewerKey: Key, partnerKey: Key, chosen: Set<SheetSlot>): RelationChangeView? {
        val block = doc.index.block(blockKey) ?: return null
        val viewer = doc.index.character(viewerKey) ?: return null
        val partner = doc.index.character(partnerKey) ?: return null
        val relation = RelationReader.between(doc, viewerKey, partnerKey)
        val point = ReadPoint.BeforeBlock(blockKey)
        val number = "#" + block.number.toString().padStart(3, '0')
        val scene = StoryLabels.unitCode(doc, block.sceneKey).orEmpty()
        val episode = StoryLabels.episodeCodeOf(doc, block.sceneKey).orEmpty()
        val slots = changeSlots(viewer.name, partner.name)
        val sections = slots.filter { it.first in chosen }.map { (slot, _) ->
            val current = valueText(doc, relation, viewerKey, slot, draftOf(doc, relation, viewerKey, slot, point)).ifEmpty { CastCopy.NOT_MET }
            ChangeSection(slot, "값 · $current →", kindChips(doc, slot))
        }
        return RelationChangeView(
            title = CHANGE_TITLE,
            cancelLabel = CANCEL,
            sub = CastCopy.join(number, scene),
            viewerName = viewer.name,
            partnerName = partner.name,
            slotChips = slots,
            sections = sections,
            addKindLabel = RelationKinds.ADD,
            startRow = START to CastCopy.join("이 블록", "$episode $number".trim()),
            reflectRow = REFLECT to REFLECT_VALUE,
            primaryLabel = RECORD,
        )
    }

    /**
     * `기록` (CST-30): one `◆` per chosen slot, all anchored at the block (applies from this block
     * on; rule 12 §1). Two chips → two records (TC-13-08). The pair's relation is created when it
     * has none (`만나기 전` before it).
     */
    fun recordAtBlock(
        doc: ProjectDoc,
        blockKey: Key,
        viewerKey: Key,
        partnerKey: Key,
        drafts: Map<SheetSlot, SlotDraft>,
        ctx: EditContext,
    ): Pair<ProjectDoc, List<ChangeRecord>> {
        if (drafts.isEmpty() || viewerKey == partnerKey) return doc to emptyList()
        val existing = RelationReader.between(doc, viewerKey, partnerKey)
        val (withRelation, relation) = RelationEdits.openOrCreate(doc, viewerKey, partnerKey, ctx)
        val values = drafts.entries.associate { (slot, draft) ->
            slotName(relation, viewerKey, slot) to input(withRelation, existing ?: relation, viewerKey, partnerKey, slot, draft)
        }
        return RelationEdits.record(withRelation, relation.key, values, Anchor.AtBlock(blockKey), ctx)
    }
}
