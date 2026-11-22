package io.github.lenticualrstone.civilight.domain.cast.merge

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.refs.RefKind
import io.github.lenticualrstone.civilight.domain.cast.refs.RefRewriter
import io.github.lenticualrstone.civilight.domain.cast.refs.References
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.Side
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.trash.MergeChoice
import io.github.lenticualrstone.civilight.model.trash.MergeRecord
import io.github.lenticualrstone.civilight.model.trash.MovedRef
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import kotlinx.serialization.Serializable

/**
 * `MergeRecord.mergedOriginal` of a character merge: model JSON of every row as it was before the
 * merge (`merged` is the merged character to rebuild on restore). Undo writes these rows back.
 */
@Serializable
data class MergeSnapshot(
    val merged: Character,
    val target: Character,
    /** Relations moved, folded, changed or deleted by the merge. */
    val relations: List<Relation> = emptyList(),
    /** Records retargeted or dropped by the merge. */
    val records: List<ChangeRecord> = emptyList(),
    val trashEntryKey: Key,
    /** Cells taken from the merged item (`fields:<key>`, `oneLiner` …). */
    val takenCells: List<String> = emptyList(),
    /** Relations whose values or records went to the target. */
    val movedRelations: Int = 0,
)

/** The merged project, the merge record and the toast. */
data class MergeResult(
    val doc: ProjectDoc,
    val record: MergeRecord,
    /** `시장 꼬마 → 노아 합침 · 참조 5 · 되돌리기`. */
    val toast: String,
    /** Recorder links (outside the project) the recorder store must point at the target. */
    val recorderLinkKeys: List<Key>,
)

/**
 * Character merge (rule 20 §6, CST-45): every reference moves to the target; values default to
 * the target's and are chosen per row; target-empty cells take the merged value; relations with the
 * same partner are folded into the target's (both sets of `◆` kept); the mutual relation is
 * deleted; the merged item goes to the trash as `합침 → <target>`.
 */
object CharacterMerge {
    private fun cellId(slot: MergeSlot): String = when (slot) {
        is MergeSlot.Field -> "fields:${slot.defKey}"
        MergeSlot.OneLiner -> "oneLiner"
        MergeSlot.DisplayName -> "displayName"
        MergeSlot.Tags -> "tags"
        MergeSlot.Speech -> "speechBody"
        MergeSlot.Secret -> "secretBody"
    }

    private fun take(target: Character, merged: Character, slot: MergeSlot): Character = when (slot) {
        is MergeSlot.Field -> merged.fields[slot.defKey]?.let { target.copy(fields = target.fields + (slot.defKey to it)) } ?: target
        MergeSlot.OneLiner -> target.copy(oneLiner = merged.oneLiner)
        MergeSlot.DisplayName -> target.copy(displayNameBase = merged.displayNameBase)
        MergeSlot.Tags -> target.copy(tagKeys = merged.tagKeys)
        MergeSlot.Speech -> target.copy(speechBody = merged.speechBody)
        MergeSlot.Secret -> target.copy(secretBody = merged.secretBody)
    }

    private fun ownerTarget(slot: MergeSlot, characterKey: Key): ChangeTarget? = when (slot) {
        is MergeSlot.Field -> ChangeTarget.Field(EntityKind.CHARACTER, characterKey, slot.defKey)
        MergeSlot.DisplayName -> ChangeTarget.DisplayName(characterKey)
        else -> null
    }

    fun merge(
        doc: ProjectDoc,
        mergedKey: Key,
        targetKey: Key,
        choices: MergeChoices,
        ctx: EditContext,
        recorderLinks: List<RecorderLink> = emptyList(),
    ): MergeResult {
        val preview = MergePlanner.preview(doc, mergedKey, targetKey, recorderLinks)
        val merged = preview.merged
        val targetBefore = preview.target
        val moved = mutableListOf<MovedRef>()
        val snapRelations = LinkedHashMap<Key, Relation>()
        val snapRecords = LinkedHashMap<Key, ChangeRecord>()
        fun snap(r: ChangeRecord) { snapRecords.putIfAbsent(r.key, r) }
        fun snap(r: Relation) { snapRelations.putIfAbsent(r.key, r) }
        var d = doc

        // 1. References (all of them, also inside trashed containers, so a restored scene speaks to the target).
        val refs = References.scan(doc, EntityKind.CHARACTER, mergedKey, recorderLinks)
        val linkKeys = HashSet<Key>()
        val recorderKeys = mutableListOf<Key>()
        for (ref in refs) when (ref.kind) {
            RefKind.SPEAKER -> {
                val option = ref.field.takeIf { it.startsWith("options:") }?.removePrefix("options:")
                d = RefRewriter.setSpeaker(d, ref.ownerKey, option, mergedKey, targetKey)
                moved += MovedRef(ref.ownerKind, ref.ownerKey, ref.field)
            }
            RefKind.APPEARANCE -> {
                val (next, how) = RefRewriter.moveSilentCast(d, ref.ownerKey, mergedKey, targetKey)
                d = next
                if (how != null) moved += MovedRef("scene", ref.ownerKey, how)
            }
            RefKind.LINK -> {
                linkKeys += ref.field.removePrefix("link:")
                moved += MovedRef(ref.ownerKind, ref.ownerKey, ref.field)
            }
            RefKind.CANVAS_NODE -> {
                d = RefRewriter.setCanvasNodeRef(d, ref.ownerKey, targetKey)
                moved += MovedRef(ref.ownerKind, ref.ownerKey, ref.field)
            }
            RefKind.RECORDER -> {
                recorderKeys += ref.ownerKey
                moved += MovedRef(ref.ownerKind, ref.ownerKey, ref.field)
            }
            else -> Unit
        }
        d = RefRewriter.retargetLinks(d, linkKeys, targetKey)

        // 2. Cells: chosen values and target-empty fills, with the history of taken fields.
        var target = targetBefore
        val taken = mutableListOf<String>()
        val filledDefs = mutableListOf<Key>()
        val chosen = mutableListOf<MergeChoice>()
        val takenTargets = mutableListOf<Pair<ChangeTarget, ChangeTarget>>()
        for (slot in MergePlanner.cells(doc, mergedKey, targetKey)) {
            val m = MergePlanner.value(merged, slot) ?: continue
            val t = MergePlanner.value(targetBefore, slot)
            val fill = t == null
            if (!fill && (choices.pick(slot) != MergePick.MERGED || m == t)) continue
            target = take(target, merged, slot)
            taken += cellId(slot)
            if (slot is MergeSlot.Field) if (fill) filledDefs += slot.defKey else chosen += MergeChoice(fieldDefKey = slot.defKey)
            val from = ownerTarget(slot, mergedKey)
            val to = ownerTarget(slot, targetKey)
            if (from != null && to != null) takenTargets += from to to
        }
        if (takenTargets.isNotEmpty()) {
            val records = d.changes.records.mapNotNull { r ->
                val pair = takenTargets.firstOrNull { (from, to) -> r.target.sameAs(from) || r.target.sameAs(to) }
                when {
                    pair == null -> r
                    r.target.sameAs(pair.second) -> { snap(r); null } // the target's own history yields to the taken value
                    else -> { snap(r); moved += MovedRef("record", r.key, "target"); r.copy(target = pair.second, updatedAt = ctx.now, updatedBy = ctx.by) }
                }
            }
            d = d.copy(changes = d.changes.copy(records = records))
        }
        d = d.copy(cast = d.cast.copy(characters = d.cast.characters.map {
            if (it.key == targetKey) target.copy(updatedAt = ctx.now, updatedBy = ctx.by) else it
        }))

        // 3. Relations.
        var movedRelations = 0
        val mutual = RelationReader.between(d, mergedKey, targetKey)
        if (mutual != null) {
            snap(mutual)
            val ids = RelationReader.SLOTS.map { RelationReader.target(mutual.key, it).targetId }.toSet()
            d.changes.records.filter { it.target.targetId in ids }.forEach(::snap)
            d = d.copy(
                cast = d.cast.copy(relations = d.cast.relations.filterNot { it.key == mutual.key }),
                changes = d.changes.copy(records = d.changes.records.filterNot { it.target.targetId in ids }),
            )
        }
        for ((mr, tr, partner) in MergePlanner.partnerRelations(d, mergedKey, targetKey)) {
            snap(mr)
            movedRelations++
            val ms = RelationReader.sideOf(mr, mergedKey)!!
            if (tr == null) {
                val ref = RelationReader.characterRef(targetKey)
                d = d.copy(cast = d.cast.copy(relations = d.cast.relations.map { r ->
                    if (r.key != mr.key) r else if (ms == Side.A) r.copy(endA = ref, updatedAt = ctx.now, updatedBy = ctx.by)
                    else r.copy(endB = ref, updatedAt = ctx.now, updatedBy = ctx.by)
                }))
                moved += MovedRef("relation", mr.key, if (ms == Side.A) "endA" else "endB")
                continue
            }
            snap(tr)
            val ts = RelationReader.sideOf(tr, targetKey)!!
            // Base slots.
            var folded = d.index.relation(tr.key)!!
            for (slot in RelationReader.SLOTS) {
                val tSlot = MergePlanner.mapSlot(slot, ms, ts)
                val mv = if (slot == RelationSlotName.BETWEEN) MergePlanner.mapBetween(RelationReader.clean(mr.between), ms, ts) else mr.baseSlot(slot)
                val ma = mr.baseAddress(slot)
                val hasMerged = if (RelationReader.isAddress(slot)) RelationReader.cleanAddress(ma) != null else RelationReader.clean(mv) != null
                if (!hasMerged) continue
                val tv = MergePlanner.baseOf(tr, tSlot)
                val pick = choices.pick(partner, tSlot)
                if (tv != null && pick != MergePick.MERGED) continue
                folded = withSlot(folded, tSlot, mv, ma)
                chosen += MergeChoice(relationKey = tr.key, slot = tSlot.open())
            }
            d = d.copy(cast = d.cast.copy(relations = d.cast.relations.mapNotNull { r ->
                when (r.key) {
                    mr.key -> null
                    tr.key -> folded.copy(updatedAt = ctx.now, updatedBy = ctx.by)
                    else -> r
                }
            }))
            moved += MovedRef("relation", mr.key, "folded:${tr.key}")
            // Records: both sets kept; the same slot at the same anchor keeps the chosen one.
            val mrIds = RelationReader.SLOTS.associateBy { RelationReader.target(mr.key, it).targetId }
            val existing = d.changes.records.filter { (it.target as? ChangeTarget.RelationSlot)?.relationKey == tr.key }
            val drop = HashSet<Key>()
            val records = d.changes.records.map { r ->
                val slot = mrIds[r.target.targetId] ?: return@map r
                snap(r)
                val tSlot = MergePlanner.mapSlot(slot, ms, ts)
                val newTarget = RelationReader.target(tr.key, tSlot)
                val clash = existing.firstOrNull { it.target.sameAs(newTarget) && it.anchor.sameAs(r.anchor) && it.deletedAt == null }
                if (clash != null) {
                    if (choices.pick(partner, tSlot) == MergePick.MERGED) { snap(clash); drop += clash.key } else { drop += r.key }
                }
                val value = if (slot == RelationSlotName.BETWEEN) {
                    r.valueAs(SlotValue.serializer())?.let { MergePlanner.mapBetween(it, ms, ts) }?.let { ChangeRecord.encodeValue(SlotValue.serializer(), it) }
                } else {
                    r.value
                }
                moved += MovedRef("record", r.key, "target")
                r.copy(target = newTarget, value = value, updatedAt = ctx.now, updatedBy = ctx.by)
            }
            d = d.copy(changes = d.changes.copy(records = records.filterNot { it.key in drop }))
        }

        // 4. The merged item goes to the trash as `합침 → target`.
        val entry = TrashEntry(
            key = ctx.newKey(),
            kind = TrashKind.CHARACTER.open(),
            projectKey = doc.project.key,
            targetKeys = listOf(mergedKey),
            reason = TrashReason.MERGED.open(),
            mergedIntoKey = targetKey,
            hiddenAtDelete = merged.hidden,
            deletedAt = ctx.now,
            purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        d = d.copy(
            cast = d.cast.copy(characters = d.cast.characters.map {
                if (it.key == mergedKey) it.copy(deletedAt = ctx.now, trashEntryKey = entry.key, updatedAt = ctx.now, updatedBy = ctx.by) else it
            }),
            trash = d.trash.copy(entries = d.trash.entries + entry),
        )
        val snapshot = MergeSnapshot(
            merged = merged, target = targetBefore, relations = snapRelations.values.toList(), records = snapRecords.values.toList(),
            trashEntryKey = entry.key, takenCells = taken, movedRelations = movedRelations,
        )
        val record = MergeRecord(
            key = ctx.newKey(),
            kind = NameTagKind.CHARACTER.open(),
            mergedKey = mergedKey,
            targetKey = targetKey,
            at = ctx.now,
            movedRefs = moved,
            filledFieldDefKeys = filledDefs,
            chosenFromMerged = chosen,
            deletedMutualRelationKey = mutual?.key,
            mergedOriginal = CivilightJson.encodeElement(MergeSnapshot.serializer(), snapshot),
        )
        d = d.copy(tombstones = d.tombstones.copy(mergeRecords = d.tombstones.mergeRecords + record))
        val toast = "${merged.name} → ${targetBefore.name} 합침 · 참조 ${preview.movedCount} · 되돌리기"
        return MergeResult(d, record, toast, recorderKeys)
    }

    private fun withSlot(r: Relation, slot: RelationSlotName, value: SlotValue?, address: String?): Relation = when (slot) {
        RelationSlotName.BETWEEN -> r.copy(between = value)
        RelationSlotName.MIND_A -> r.copy(mindA = value)
        RelationSlotName.MIND_B -> r.copy(mindB = value)
        RelationSlotName.ADDRESS_A -> r.copy(addressA = address)
        RelationSlotName.ADDRESS_B -> r.copy(addressB = address)
    }

    fun snapshotOf(record: MergeRecord): MergeSnapshot = CivilightJson.decodeElement(MergeSnapshot.serializer(), record.mergedOriginal)

    /** `되돌리기` right after the merge: every change is undone. Recorder links are returned for the recorder store. */
    fun undo(doc: ProjectDoc, record: MergeRecord): Pair<ProjectDoc, List<Key>> {
        val snap = snapshotOf(record)
        var d = doc
        val linkKeys = HashSet<Key>()
        val recorder = mutableListOf<Key>()
        for (ref in record.movedRefs) when {
            ref.ownerKind == "block" && ref.field == "speakerKey" -> d = RefRewriter.setSpeaker(d, ref.ownerKey, null, record.targetKey, record.mergedKey)
            ref.ownerKind == "block" && ref.field.startsWith("options:") ->
                d = RefRewriter.setSpeaker(d, ref.ownerKey, ref.field.removePrefix("options:"), record.targetKey, record.mergedKey)
            ref.ownerKind == "scene" && ref.field.startsWith("silentCast") ->
                d = RefRewriter.undoSilentCast(d, ref.ownerKey, ref.field, record.mergedKey, record.targetKey)
            ref.field.startsWith("link:") -> linkKeys += ref.field.removePrefix("link:")
            ref.ownerKind == "canvasNode" -> d = RefRewriter.setCanvasNodeRef(d, ref.ownerKey, record.mergedKey)
            ref.ownerKind == "recorderLink" -> recorder += ref.ownerKey
        }
        d = RefRewriter.retargetLinks(d, linkKeys, record.mergedKey)
        val rows = mapOf(snap.merged.key to snap.merged, snap.target.key to snap.target)
        val relations = snap.relations.associateBy { it.key }
        val records = snap.records.associateBy { it.key }
        d = d.copy(
            cast = d.cast.copy(
                characters = d.cast.characters.map { rows[it.key] ?: it },
                relations = d.cast.relations.map { relations[it.key] ?: it } + snap.relations.filter { r -> d.cast.relations.none { it.key == r.key } },
            ),
            changes = d.changes.copy(
                records = d.changes.records.map { records[it.key] ?: it } + snap.records.filter { r -> d.changes.records.none { it.key == r.key } },
            ),
            trash = d.trash.copy(entries = d.trash.entries.filterNot { it.key == snap.trashEntryKey }),
            tombstones = d.tombstones.copy(mergeRecords = d.tombstones.mergeRecords.filterNot { it.key == record.key }),
        )
        return d to recorder
    }

    /**
     * Restoring the merged item from the trash (TC-20-19): it comes back as a separate character
     * with its own original values and no relations; moved references, relations and taken values
     * stay with the target. Returns the sheet line `옮긴 참조 5 · 관계 1 · 칸 3 · 노아에 그대로`.
     */
    fun restoreMerged(doc: ProjectDoc, mergedKey: Key, ctx: EditContext): Pair<ProjectDoc, String> {
        val record = requireNotNull(doc.tombstones.mergeRecords.lastOrNull { it.mergedKey == mergedKey }) { "no merge of $mergedKey" }
        val snap = snapshotOf(record)
        val current = doc.index.character(mergedKey)
        val restored = snap.merged.copy(deletedAt = null, trashEntryKey = null, updatedAt = ctx.now, updatedBy = ctx.by)
        val d = doc.copy(
            cast = doc.cast.copy(
                characters = if (current == null) doc.cast.characters + restored else doc.cast.characters.map { if (it.key == mergedKey) restored else it },
                relations = doc.cast.relations.filterNot { RelationReader.sideOf(it, mergedKey) != null },
            ),
            trash = doc.trash.copy(entries = doc.trash.entries.filterNot { it.key == (current?.trashEntryKey ?: snap.trashEntryKey) }),
        )
        val refs = record.movedRefs.count { it.ownerKind != "relation" && it.ownerKind != "record" && it.ownerKind != "canvasNode" }
        val target = doc.index.character(record.targetKey)?.name.orEmpty()
        val line = CastCopy.join("옮긴 참조 $refs", "관계 ${snap.movedRelations}", "칸 ${snap.takenCells.size}", "${target}에 그대로")
        return d to line
    }
}
