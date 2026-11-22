package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** A relation seen from one character (rule 13 §4): own mind `→`, the other's `←`, `사이` beside the partner. */
data class ViewerRelation(
    val viewerKey: Key,
    val partnerKey: Key,
    val state: RelationState,
    /** The partner's role (`세라 · 선배` = 세라 is the viewer's 선배), or the symmetric kind. */
    val partnerRole: String?,
    val ownMind: SlotText?,
    val otherMind: SlotText?,
    /** What the viewer calls the partner. */
    val ownAddress: String?,
    /** What the partner calls the viewer. */
    val otherAddress: String?,
) {
    val relation: Relation get() = state.relation

    /** `세라 · 선배`, or the partner's name alone. */
    fun title(partnerName: String): String = CastCopy.join(partnerName, partnerRole)

    /**
     * Summary line: `→ 경계 · ← 보호`; with one mind, its description follows (`→ 의지 · 의지하는 선배`);
     * `마음 적지 않음` with no mind; `만나기 전` before the first value ([PROPOSED] combination rule).
     */
    val summary: String
        get() {
            if (state.notMet) return CastCopy.NOT_MET
            val own = ownMind
            val other = otherMind
            if (own == null && other == null) return CastCopy.NO_MIND
            val parts = listOfNotNull(own?.let { "→ ${it.word}" }, other?.let { "← ${it.word}" }).toMutableList()
            val single = if (own != null && other == null) own else if (own == null) other else null
            single?.description?.takeIf { it != single.word }?.let { parts += it }
            return parts.joinToString(CastCopy.SEP)
        }
}

/** Step of a relation's history on the `관계` tab. */
data class RelationStep(
    val point: String,
    /** `1-7 · 처음 만남 · → 경계 · ← 보호`. */
    val text: String,
    val status: StepStatus,
) {
    /** `예정` / `지금 시점보다 뒤`, or null. */
    val statusLabel: String?
        get() = when (status) {
            StepStatus.PLANNED -> CastCopy.PLANNED
            StepStatus.AFTER_LENS -> CastCopy.AFTER_LENS
            else -> null
        }
}

/** `ink` for the step in effect, `ink3` for the others. */
enum class StepStatus { PAST, IN_EFFECT, AFTER_LENS, PLANNED }

/** One partner block of the `관계` tab. */
data class RelationBlock(
    val partnerKey: Key,
    val partnerName: String,
    /** `세라 · 선배`. */
    val title: String,
    /** `→ 의지 · 의지하는 선배`. */
    val sub: String,
    /** `◆` at the right. */
    val hasRecords: Boolean,
    val steps: List<RelationStep>,
)

/** Dotted row of a relation whose partner is in the trash (CST-28, CST-41). */
data class DeletedPartnerRow(val partnerKey: Key, val name: String, val label: String, val actions: List<String>)

/** The `관계` tab (CST-28). */
data class RelationTab(
    /** `관계 · 시점 1-7`. */
    val header: String,
    val mapLink: String,
    val blocks: List<RelationBlock>,
    val deletedRows: List<DeletedPartnerRow>,
    /** `숨긴 인물과의 관계 1` while locked. */
    val hiddenRow: String?,
    val addLabel: String,
)

/** A 호칭 chip of the `말투` tab: `<partner> → <호칭>` (CST-15). */
data class AddressChip(val partnerKey: Key, val relationKey: Key, val label: String, val address: String)

/** Viewer-side presentations: `관계` tab, `말투` chips, history steps (CST-15, CST-28, CST-31). */
object RelationViews {
    const val MAP_LINK = "관계도로 보기"
    const val ADD = "+ 관계"

    fun view(doc: ProjectDoc, relation: Relation, viewerKey: Key, point: ReadPoint): ViewerRelation? {
        val side = RelationReader.sideOf(relation, viewerKey) ?: return null
        val state = RelationReader.state(doc, relation, point)
        return ViewerRelation(
            viewerKey = viewerKey,
            partnerKey = RelationReader.endKey(relation, side.other),
            state = state,
            partnerRole = state.between?.let { RelationReader.roleOf(doc, it, side.other) },
            ownMind = RelationReader.mindText(doc, state.mind(side)),
            otherMind = RelationReader.mindText(doc, state.mind(side.other)),
            ownAddress = state.address(side),
            otherAddress = state.address(side.other),
        )
    }

    fun view(doc: ProjectDoc, a: Key, b: Key, lens: LensPoint): ViewerRelation? =
        RelationReader.between(doc, a, b)?.let { view(doc, it, a, ReadPoint.AtLens(lens)) }

    /** Character relations of [characterKey] (both ends characters), live or not. */
    fun relationsOf(doc: ProjectDoc, characterKey: Key): List<Relation> =
        doc.index.relationsOf(RelationReader.characterRef(characterKey))
            .filter { it.endA.kind.known == RelationEndKind.CHARACTER && it.endB.kind.known == RelationEndKind.CHARACTER }

    private val partnerOrder: Comparator<Pair<Relation, Character>> =
        compareBy<Pair<Relation, Character>>({ it.second.order }, { it.second.key })

    /**
     * The `관계` tab of [viewerKey]: one block per live, visible partner (partner order); dotted rows
     * for partners in the trash; one `숨긴 인물과의 관계 N` row for locked hidden partners.
     */
    fun tab(doc: ProjectDoc, viewerKey: Key, lens: LensPoint, hiddenUnlocked: Boolean = false): RelationTab {
        val point = ReadPoint.AtLens(lens)
        val withPartners = relationsOf(doc, viewerKey).mapNotNull { r ->
            val side = RelationReader.sideOf(r, viewerKey) ?: return@mapNotNull null
            val partner = doc.index.character(RelationReader.endKey(r, side.other)) ?: return@mapNotNull null
            r to partner
        }.sortedWith(partnerOrder)
        val blocks = mutableListOf<RelationBlock>()
        val deleted = mutableListOf<DeletedPartnerRow>()
        var hidden = 0
        for ((relation, partner) in withPartners) {
            when (val ref = ItemRefs.character(doc, partner.key, hiddenUnlocked)) {
                is ResolvedItem.HiddenLocked -> hidden++
                is ResolvedItem.Trashed -> deleted += DeletedPartnerRow(
                    partnerKey = partner.key, name = ref.name,
                    label = "${CastCopy.SOFT_MARK} 지운 인물 · 관계 1", actions = listOf("되살리기", "이대로 두기"),
                )
                is ResolvedItem.Live -> {
                    val v = view(doc, relation, viewerKey, point) ?: continue
                    blocks += RelationBlock(
                        partnerKey = partner.key,
                        partnerName = partner.name,
                        title = v.title(partner.name),
                        sub = v.summary,
                        hasRecords = RelationReader.records(doc, relation).isNotEmpty(),
                        steps = steps(doc, relation, viewerKey, lens),
                    )
                }
                else -> Unit
            }
        }
        return RelationTab(
            header = "관계 · ${StoryLabels.lensSection(doc, lens)}",
            mapLink = MAP_LINK,
            blocks = blocks,
            deletedRows = deleted,
            hiddenRow = if (hidden > 0) hiddenRow(hidden) else null,
            addLabel = ADD,
        )
    }

    fun hiddenRow(count: Int): String = "숨긴 인물과의 관계 $count"

    /**
     * History steps from the viewer's side, one per point (records grouped by their point label in
     * story order): `1-7 · 처음 만남 · → 경계 · ← 보호`. 호칭 records are listed only when a point has
     * nothing else.
     */
    fun steps(doc: ProjectDoc, relation: Relation, viewerKey: Key, lens: LensPoint): List<RelationStep> {
        val side = RelationReader.sideOf(relation, viewerKey) ?: return emptyList()
        return groupedSteps(doc, relation, lens) { record, slot ->
            val value = record
            when (slot) {
                RelationSlotName.BETWEEN -> RelationReader.slotValueOf(value)?.let { "사이 ${RelationReader.roleOf(doc, it, side.other)}" }
                RelationReader.mindSlot(side) -> RelationReader.mindText(doc, RelationReader.slotValueOf(value))?.let { "→ ${it.word}" }
                RelationReader.mindSlot(side.other) -> RelationReader.mindText(doc, RelationReader.slotValueOf(value))?.let { "← ${it.word}" }
                RelationReader.addressSlot(side) -> RelationReader.addressOf(value)?.let { "호칭 ‘$it’" }
                else -> RelationReader.addressOf(value)?.let { "← 호칭 ‘$it’" }
            }
        }
    }

    /**
     * Neutral steps for the map's `고른 관계` card: `1-7 | 리엔 → 경계 · 하르트 → 보호`, with the status
     * word appended after the lens (`지금 시점보다 뒤` / `예정`).
     */
    fun neutralSteps(doc: ProjectDoc, relation: Relation, lens: LensPoint): List<Pair<String, String>> {
        val nameA = doc.index.character(relation.endA.key)?.name.orEmpty()
        val nameB = doc.index.character(relation.endB.key)?.name.orEmpty()
        return groupedSteps(doc, relation, lens, withNotes = false) { record, slot ->
            when (slot) {
                RelationSlotName.BETWEEN -> RelationReader.slotValueOf(record)?.let { v ->
                    "사이 ${RelationReader.kind(doc, v)?.let { k -> if (k.pairName != null) "${k.name} ↔ ${k.pairName}" else k.name } ?: RelationReader.kindName(doc, v)}"
                }
                RelationSlotName.MIND_A -> RelationReader.mindText(doc, RelationReader.slotValueOf(record))?.let { "$nameA → ${it.word}" }
                RelationSlotName.MIND_B -> RelationReader.mindText(doc, RelationReader.slotValueOf(record))?.let { "$nameB → ${it.word}" }
                RelationSlotName.ADDRESS_A -> RelationReader.addressOf(record)?.let { "$nameA 호칭 ‘$it’" }
                RelationSlotName.ADDRESS_B -> RelationReader.addressOf(record)?.let { "$nameB 호칭 ‘$it’" }
            }
        }.map { step ->
            val body = step.text.removePrefix("${step.point}${CastCopy.SEP}")
            step.point to listOfNotNull(body, step.statusLabel).joinToString(CastCopy.SEP)
        }
    }

    private fun groupedSteps(
        doc: ProjectDoc,
        relation: Relation,
        lens: LensPoint,
        withNotes: Boolean = true,
        part: (ChangeRecord, RelationSlotName) -> String?,
    ): List<RelationStep> {
        val records = RelationReader.records(doc, relation)
            .sortedWith(compareBy<ChangeRecord>({ doc.manuscript.rankOf(it.anchor) }, { it.createdAt }, { it.key }))
        if (records.isEmpty()) return emptyList()
        val lensRank = doc.manuscript.rankOf(lens)
        val groups = LinkedHashMap<String, MutableList<ChangeRecord>>()
        for (r in records) groups.getOrPut(StoryLabels.anchorPoint(doc, r.anchor)) { mutableListOf() } += r
        val slotOrder = listOf(
            RelationSlotName.BETWEEN, RelationSlotName.MIND_A, RelationSlotName.MIND_B, RelationSlotName.ADDRESS_A, RelationSlotName.ADDRESS_B,
        )
        val inEffectGroup = groups.entries.lastOrNull { (_, rs) -> rs.any { doc.manuscript.rankOf(it.anchor) <= lensRank } }?.key
        return groups.map { (point, rs) ->
            val bySlot = rs.mapNotNull { r -> ((r.target as? ChangeTarget.RelationSlot)?.slot?.known)?.let { it to r } }
                .sortedBy { slotOrder.indexOf(it.first) }
            val nonAddress = bySlot.filterNot { RelationReader.isAddress(it.first) }
            val shown = nonAddress.ifEmpty { bySlot }
            val parts = shown.mapNotNull { (slot, r) -> part(r, slot) }
            val notes = if (!withNotes) emptyList() else rs.mapNotNull { it.note?.takeIf { n -> n.isNotBlank() } }.distinct()
            val after = rs.all { doc.manuscript.rankOf(it.anchor) > lensRank }
            val status = when {
                after && rs.any { doc.isPlanned(it) } -> StepStatus.PLANNED
                after -> StepStatus.AFTER_LENS
                point == inEffectGroup -> StepStatus.IN_EFFECT
                else -> StepStatus.PAST
            }
            RelationStep(point, CastCopy.joinAll(listOf(point) + notes + parts), status)
        }
    }

    /**
     * `말투` tab chips (CST-15): the viewer's 호칭 toward each visible partner at the lens. Edits go
     * to the relation's 호칭 slot (there is no separate 말투 value).
     */
    fun addressChips(doc: ProjectDoc, viewerKey: Key, lens: LensPoint, hiddenUnlocked: Boolean = false): List<AddressChip> =
        relationsOf(doc, viewerKey).mapNotNull { r ->
            val side = RelationReader.sideOf(r, viewerKey) ?: return@mapNotNull null
            val partnerKey = RelationReader.endKey(r, side.other)
            val partner = ItemRefs.character(doc, partnerKey, hiddenUnlocked) as? ResolvedItem.Live ?: return@mapNotNull null
            val address = RelationReader.state(doc, r, ReadPoint.AtLens(lens)).address(side) ?: return@mapNotNull null
            Triple(doc.index.character(partnerKey)!!, r, AddressChip(partnerKey, r.key, "${partner.name} → $address", address))
        }.sortedWith(compareBy({ it.first.order }, { it.first.key })).map { it.third }

    /** `숨긴 인물과의 관계 N` for the `말투` tab while locked, or null. */
    fun hiddenPartnerCount(doc: ProjectDoc, viewerKey: Key, hiddenUnlocked: Boolean): Int = relationsOf(doc, viewerKey).count { r ->
        val side = RelationReader.sideOf(r, viewerKey) ?: return@count false
        ItemRefs.character(doc, RelationReader.endKey(r, side.other), hiddenUnlocked) is ResolvedItem.HiddenLocked
    }
}
