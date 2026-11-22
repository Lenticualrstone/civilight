package io.github.lenticualrstone.civilight.domain.cast.merge

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.refs.ItemReference
import io.github.lenticualrstone.civilight.domain.cast.refs.RefKind
import io.github.lenticualrstone.civilight.domain.cast.refs.References
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.relation.Side
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.text.docText

/** A comparable cell of a character: a project field or a fixed field. */
sealed interface MergeSlot {
    data class Field(val defKey: Key) : MergeSlot
    data object OneLiner : MergeSlot
    data object DisplayName : MergeSlot
    data object Tags : MergeSlot
    data object Speech : MergeSlot
    data object Secret : MergeSlot
}

/** Whose value a row keeps; the default is always the target's. */
enum class MergePick { TARGET, MERGED }

/** A `다른 칸` row: both have a value and they differ. */
data class CompareRow(val slot: MergeSlot, val label: String, val mergedText: String, val targetText: String)

/** A `관계 겹침` row: both have a relation with the same partner and a slot differs (slot named in the target relation). */
data class RelationOverlapRow(
    val partnerKey: Key,
    val partnerName: String,
    val slot: RelationSlotName,
    /** `사이`, `노아의 마음`, `오웬의 마음`, `호칭 노아 → 오웬` …. */
    val slotLabel: String,
    val mergedText: String,
    val targetText: String,
)

/** Choices of the comparison sheet. Absent entries keep the target's value. */
data class MergeChoices(
    val cells: Map<MergeSlot, MergePick> = emptyMap(),
    /** Keyed by partner and slot of the target's relation with that partner. */
    val relationSlots: Map<Pair<Key, RelationSlotName>, MergePick> = emptyMap(),
) {
    fun pick(slot: MergeSlot): MergePick = cells[slot] ?: MergePick.TARGET
    fun pick(partner: Key, slot: RelationSlotName): MergePick = relationSlots[partner to slot] ?: MergePick.TARGET
}

/** The comparison sheet (`RCMerge`, CST-45). */
data class MergePreview(
    val merged: Character,
    val target: Character,
    /** Live references that move to the target. */
    val references: List<ItemReference>,
    val differing: List<CompareRow>,
    val sameCount: Int,
    /** Target-empty cells taken from the merged item without asking. */
    val filled: List<Pair<MergeSlot, String>>,
    val relationOverlaps: List<RelationOverlapRow>,
    val mutualRelationKey: Key?,
    /** Fixed voice IDs on the merged item's live lines (unchanged; rule 12 §6 `◇`). */
    val fixedVoiceIds: Int,
) {
    val title: String get() = "인물 합치기"
    val sub: String get() = "${merged.name} → ${target.name} · 기본은 ${target.name} 값"

    /** `옮길 참조 · 5`. */
    val movedCount: Int get() = references.count { it.kind in MOVED_KINDS }
    val movedTitle: String get() = "옮길 참조 · $movedCount"

    /** `대사 3 · @연결 1 · 씬 등장 1`. */
    val movedLine: String
        get() = CastCopy.joinAll(
            listOf(
                references.count { it.kind == RefKind.SPEAKER }.takeIf { it > 0 }?.let { "대사 $it" },
                references.count { it.kind == RefKind.LINK }.takeIf { it > 0 }?.let { "@연결 $it" },
                references.count { it.kind == RefKind.APPEARANCE }.takeIf { it > 0 }?.let { "씬 등장 $it" },
                references.count { it.kind == RefKind.RECORDER }.takeIf { it > 0 }?.let { "레코더 칩 $it" },
            ),
        )

    val differingTitle: String get() = "다른 칸 · ${differing.size}"
    val columns: Pair<String, String> get() = "${merged.name} · 합쳐짐" to "${target.name} · 남음"
    val overlapTitle: String get() = "관계 겹침 · ${relationOverlaps.map { it.partnerKey }.distinct().size}"

    /** `같은 항목 | 6 · 펼치기`. */
    val sameRow: Pair<String, String> get() = "같은 항목" to "$sameCount · 펼치기"

    /** `빈 칸 채움 | 출생 · 성격 · 2`. */
    val filledRow: Pair<String, String> get() = "빈 칸 채움" to CastCopy.joinAll(filled.map { it.second } + filled.size.toString())

    /** `보이스 ID | 고정 2 · 그대로 · ◇`, or null. */
    val voiceRow: Pair<String, String>? get() = if (fixedVoiceIds > 0) "보이스 ID" to "고정 $fixedVoiceIds · 그대로 · ${CastCopy.SOFT_MARK}" else null

    /** `서로 관계 1 · 지워짐`, or null. */
    val mutualRow: String? get() = mutualRelationKey?.let { "서로 관계 1 · 지워짐" }

    val footnote: String get() = "${merged.name} · ‘합침 → ${target.name}’로 휴지통"
    val buttons: Pair<String, String> get() = "취소" to "합치기"

    companion object {
        val MOVED_KINDS = setOf(RefKind.SPEAKER, RefKind.LINK, RefKind.APPEARANCE, RefKind.RECORDER)
    }
}

/** Building the comparison (CST-45). */
object MergePlanner {
    fun cells(doc: ProjectDoc, mergedKey: Key, targetKey: Key): List<MergeSlot> =
        FieldDefinitions.live(doc, EntityKind.CHARACTER).filter { it.scopeCharacterKey == null }.map { MergeSlot.Field(it.key) } +
            listOf(MergeSlot.OneLiner, MergeSlot.DisplayName, MergeSlot.Tags, MergeSlot.Speech, MergeSlot.Secret)

    fun label(doc: ProjectDoc, slot: MergeSlot): String = when (slot) {
        is MergeSlot.Field -> doc.index.fieldDef(slot.defKey)?.name.orEmpty()
        MergeSlot.OneLiner -> "소개"
        MergeSlot.DisplayName -> "표시 이름"
        MergeSlot.Tags -> "태그"
        MergeSlot.Speech -> "말투"
        MergeSlot.Secret -> "비밀"
    }

    /** A cell's value as comparable data (null = empty). */
    fun value(c: Character, slot: MergeSlot): Any? = when (slot) {
        is MergeSlot.Field -> FieldValueEdits.normalized(c.fields[slot.defKey])?.copy(ext = null)
        MergeSlot.OneLiner -> c.oneLiner?.takeIf { it.isNotBlank() }
        MergeSlot.DisplayName -> c.displayNameBase?.takeIf { it.isNotBlank() }
        MergeSlot.Tags -> c.tagKeys.takeIf { it.isNotEmpty() }
        MergeSlot.Speech -> c.speechBody.takeIf { it.docText.isNotBlank() }
        MergeSlot.Secret -> c.secretBody.takeIf { it.docText.isNotBlank() }
    }

    fun text(doc: ProjectDoc, c: Character, slot: MergeSlot): String = when (slot) {
        is MergeSlot.Field -> doc.index.fieldDef(slot.defKey)?.let { FieldText.format(doc, it, c.fields[slot.defKey]).text }.orEmpty()
        MergeSlot.OneLiner -> c.oneLiner.orEmpty()
        MergeSlot.DisplayName -> c.displayNameBase.orEmpty()
        MergeSlot.Tags -> CastCopy.joinAll(c.tagKeys.map { doc.index.tag(it)?.name })
        MergeSlot.Speech -> c.speechBody.docText
        MergeSlot.Secret -> c.secretBody.docText
    }

    /** Slot of the target relation that corresponds to [slot] of the merged relation. */
    fun mapSlot(slot: RelationSlotName, mergedSide: Side, targetSide: Side): RelationSlotName {
        if (mergedSide == targetSide || slot == RelationSlotName.BETWEEN) return slot
        return when (slot) {
            RelationSlotName.MIND_A -> RelationSlotName.MIND_B
            RelationSlotName.MIND_B -> RelationSlotName.MIND_A
            RelationSlotName.ADDRESS_A -> RelationSlotName.ADDRESS_B
            RelationSlotName.ADDRESS_B -> RelationSlotName.ADDRESS_A
            RelationSlotName.BETWEEN -> slot
        }
    }

    /** A between value moved to the other orientation keeps who holds the pair's first name. */
    fun mapBetween(value: SlotValue?, mergedSide: Side, targetSide: Side): SlotValue? {
        if (value == null || mergedSide == targetSide || value.pairFirstHolder == null) return value
        val flipped = when (value.pairFirstHolder?.known) {
            io.github.lenticualrstone.civilight.model.cast.PairHolder.A -> io.github.lenticualrstone.civilight.model.cast.PairHolder.B
            io.github.lenticualrstone.civilight.model.cast.PairHolder.B -> io.github.lenticualrstone.civilight.model.cast.PairHolder.A
            null -> return value
        }
        return value.copy(pairFirstHolder = io.github.lenticualrstone.civilight.model.OpenEnum.of(flipped))
    }

    /** Base value of a slot as comparable data. */
    fun baseOf(r: Relation, slot: RelationSlotName): Any? =
        if (RelationReader.isAddress(slot)) RelationReader.cleanAddress(r.baseAddress(slot)) else RelationReader.clean(r.baseSlot(slot))?.copy(ext = null)

    /** Partner relations: (merged relation, target relation or null, partner key). Mutual relation excluded. */
    fun partnerRelations(doc: ProjectDoc, mergedKey: Key, targetKey: Key): List<Triple<Relation, Relation?, Key>> =
        RelationViews.relationsOf(doc, mergedKey).mapNotNull { mr ->
            val side = RelationReader.sideOf(mr, mergedKey) ?: return@mapNotNull null
            val partner = RelationReader.endKey(mr, side.other)
            if (partner == targetKey) return@mapNotNull null
            Triple(mr, RelationReader.between(doc, targetKey, partner), partner)
        }

    fun slotLabel(doc: ProjectDoc, tr: Relation, slot: RelationSlotName): String {
        fun name(side: Side) = doc.index.character(RelationReader.endKey(tr, side))?.name.orEmpty()
        return when (slot) {
            RelationSlotName.BETWEEN -> "사이"
            RelationSlotName.MIND_A -> "${name(Side.A)}의 마음"
            RelationSlotName.MIND_B -> "${name(Side.B)}의 마음"
            RelationSlotName.ADDRESS_A -> "호칭 ${name(Side.A)} → ${name(Side.B)}"
            RelationSlotName.ADDRESS_B -> "호칭 ${name(Side.B)} → ${name(Side.A)}"
        }
    }

    fun slotText(doc: ProjectDoc, r: Relation, slot: RelationSlotName, viewerSide: Side): String =
        if (RelationReader.isAddress(slot)) RelationReader.cleanAddress(r.baseAddress(slot)).orEmpty()
        else RelationReader.clean(r.baseSlot(slot))?.let { v ->
            if (slot == RelationSlotName.BETWEEN) RelationReader.roleOf(doc, v, viewerSide.other) ?: "" else RelationReader.kindName(doc, v) ?: v.description.orEmpty()
        }.orEmpty()

    fun preview(doc: ProjectDoc, mergedKey: Key, targetKey: Key, recorderLinks: List<RecorderLink> = emptyList()): MergePreview {
        require(mergedKey != targetKey) { "an item cannot merge into itself" }
        val merged = requireNotNull(doc.index.character(mergedKey)) { "no character $mergedKey" }
        val target = requireNotNull(doc.index.character(targetKey)) { "no character $targetKey" }
        val differing = mutableListOf<CompareRow>()
        val filled = mutableListOf<Pair<MergeSlot, String>>()
        var same = 0
        for (slot in cells(doc, mergedKey, targetKey)) {
            val m = value(merged, slot)
            val t = value(target, slot)
            when {
                m == null -> Unit
                t == null -> filled += slot to label(doc, slot)
                m == t -> same++
                else -> differing += CompareRow(slot, label(doc, slot), text(doc, merged, slot), text(doc, target, slot))
            }
        }
        val overlaps = mutableListOf<RelationOverlapRow>()
        for ((mr, tr, partner) in partnerRelations(doc, mergedKey, targetKey)) {
            if (tr == null) continue
            val ms = RelationReader.sideOf(mr, mergedKey)!!
            val ts = RelationReader.sideOf(tr, targetKey)!!
            for (slot in RelationReader.SLOTS) {
                val tSlot = mapSlot(slot, ms, ts)
                val mv = if (slot == RelationSlotName.BETWEEN) mapBetween(RelationReader.clean(mr.between), ms, ts)?.copy(ext = null) else baseOf(mr, slot)
                val tv = baseOf(tr, tSlot)
                if (mv != null && tv != null && mv != tv) {
                    overlaps += RelationOverlapRow(
                        partnerKey = partner,
                        partnerName = doc.index.character(partner)?.name.orEmpty(),
                        slot = tSlot,
                        slotLabel = slotLabel(doc, tr, tSlot),
                        mergedText = slotText(doc, mr, slot, ms),
                        targetText = slotText(doc, tr, tSlot, ts),
                    )
                }
            }
        }
        val refs = References.scan(doc, EntityKind.CHARACTER, mergedKey, recorderLinks).filter { it.live }
        val fixed = doc.allBlocks.filter { doc.liveness.isLive(it) }.sumOf { b ->
            (if (b.speakerKey == mergedKey && b.voice?.mode?.known == VoiceMode.FIXED) 1 else 0) +
                b.options.count { it.speakerKey == mergedKey && it.voice?.mode?.known == VoiceMode.FIXED }
        }
        return MergePreview(
            merged = merged,
            target = target,
            references = refs,
            differing = differing,
            sameCount = same,
            filled = filled,
            relationOverlaps = overlaps,
            mutualRelationKey = RelationReader.between(doc, mergedKey, targetKey)?.key,
            fixedVoiceIds = fixed,
        )
    }
}
