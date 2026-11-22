package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.merge.MergePick
import io.github.lenticualrstone.civilight.domain.cast.merge.MergePlanner
import io.github.lenticualrstone.civilight.domain.cast.merge.MergeSlot
import io.github.lenticualrstone.civilight.domain.trash.DocChange
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.MergeOps
import io.github.lenticualrstone.civilight.domain.trash.MergePreview
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.trash.MergeChoice
import io.github.lenticualrstone.civilight.model.trash.MergeRecord
import io.github.lenticualrstone.civilight.model.trash.MovedRef
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import io.github.lenticualrstone.civilight.domain.cast.merge.MergeChoices as CastMergeChoices
import io.github.lenticualrstone.civilight.domain.cast.merge.MergePreview as CastMergePreview
import io.github.lenticualrstone.civilight.domain.trash.MergeChoices as TrashMergeChoices

/** What `합치기` did (CST-45, WLD-15). */
data class MergeActionResult(
    val doc: ProjectDoc,
    /** The engine's comparison of the two items (counts for the toast and the record). */
    val preview: MergePreview,
    /** The merged item's `합침 → <target>` trash entry. */
    val entry: TrashEntry,
    val record: MergeRecord,
    /** `시장 꼬마 → 노아 합침 · 참조 5` with `되돌리기`. */
    val toast: Toast,
    /** Before / after of the whole merge; `되돌리기` reverts it row by row. */
    val change: DocChange,
    /** The recorder library with the merged item's links moved to the target (null when none was given). */
    val recorder: RecorderLibrary?,
    /** The library before the merge, for [MergeActions.undo]. */
    val recorderBefore: RecorderLibrary?,
)

/**
 * 합치기 for characters and world items (DECISIONS §1 and §4): one engine, `domain:trash`
 * [MergeOps] (all four named kinds, row-level undo through [DocChange], restore of the merged item
 * through `RestoreOps.restore` from its `mergedOriginal`). The character comparison sheet and its
 * per-cell choices stay `domain:cast` ([MergePlanner.preview], [CastMergeChoices]);
 * [mergeCharacters] translates those choices for the engine and adds the cells only a character
 * has (소개, 표시 이름 with its `◆` records, 태그, 말투, 비밀), which the engine does not know.
 */
object MergeActions {
    /** Ids written into `MergeChoice.ext["cell"]` for the character-only cells taken from the merged item. */
    const val CELL = "cell"

    // --- Sheets ---

    /** The character comparison sheet (`RCMerge`, CST-45): `domain:cast`. */
    fun characterSheet(doc: ProjectDoc, mergedKey: Key, targetKey: Key, recorderLinks: List<RecorderLink> = emptyList()): CastMergePreview =
        MergePlanner.preview(doc, mergedKey, targetKey, recorderLinks)

    /** The comparison of two places, terms or factions (WLD-15), or of two characters as the engine sees them. */
    fun itemPreview(doc: ProjectDoc, kind: ItemKind, mergedKey: Key, targetKey: Key): MergePreview =
        MergeOps.preview(doc, kind, mergedKey, targetKey)

    /** The cast sheet's choices as the engine's: field cells by definition key, relation slots by (partner, slot). */
    fun engineChoices(choices: CastMergeChoices): TrashMergeChoices = TrashMergeChoices(
        fieldsFromMerged = choices.cells.filter { (slot, pick) -> pick == MergePick.MERGED && slot is MergeSlot.Field }
            .keys.map { (it as MergeSlot.Field).defKey }.toSet(),
        slotsFromMerged = choices.relationSlots.filterValues { it == MergePick.MERGED }.keys,
    )

    // --- Writes ---

    /**
     * Merges character [mergedKey] into [targetKey] with the cast sheet's [choices]: references,
     * fields, relations, the trash entry and the merge record through [MergeOps.merge]; then the
     * character-only cells (target-empty ones filled, chosen ones taken; the merged item's
     * `표시 이름` records move with a taken display name); recorder links move to the target.
     */
    fun mergeCharacters(
        doc: ProjectDoc,
        mergedKey: Key,
        targetKey: Key,
        choices: CastMergeChoices,
        ctx: ActionContext,
        recorder: RecorderLibrary? = null,
    ): MergeActionResult {
        val mergedBefore = requireNotNull(doc.index.character(mergedKey)) { "unknown character $mergedKey" }
        val targetBefore = requireNotNull(doc.index.character(targetKey)) { "unknown character $targetKey" }
        val result = MergeOps.merge(doc, ItemKind.CHARACTER, mergedKey, targetKey, ctx.trash, engineChoices(choices))
        var d = result.doc

        // Character-only cells.
        var target = requireNotNull(d.index.character(targetKey))
        val filled = mutableListOf<String>()
        val chosen = mutableListOf<String>()
        val moved = mutableListOf<MovedRef>()
        for (slot in CHARACTER_CELLS) {
            val m = MergePlanner.data(doc, mergedBefore, slot) ?: continue
            val t = MergePlanner.data(doc, targetBefore, slot)
            val fill = t == null
            if (!fill && (choices.pick(slot) != MergePick.MERGED || m == t)) continue
            target = take(target, mergedBefore, slot)
            if (fill) filled += cellId(slot) else chosen += cellId(slot)
            if (slot == MergeSlot.DisplayName) {
                d = moveRecords(d, ChangeTarget.DisplayName(mergedKey), ChangeTarget.DisplayName(targetKey), ctx, moved)
            }
        }
        var record = result.record
        if (filled.isNotEmpty() || chosen.isNotEmpty()) {
            d = d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == targetKey) target.copy(updatedAt = ctx.now, updatedBy = ctx.device) else it }))
            record = record.copy(
                movedRefs = record.movedRefs + moved,
                chosenFromMerged = record.chosenFromMerged + chosen.map { MergeChoice(ext = JsonObject(mapOf(CELL to JsonPrimitive(it)))) },
                ext = withFieldCount(record.ext, filled.size + chosen.size),
            )
            d = d.copy(tombstones = d.tombstones.copy(mergeRecords = d.tombstones.mergeRecords.map { if (it.key == record.key) record else it }))
        }
        val after = recorder?.let { MergeOps.applyToRecorder(it, doc.key, ItemKind.CHARACTER, mergedKey, targetKey) }
        return MergeActionResult(d, result.preview, result.entry, record, Messages.mergeToast(result.preview), DocChange(doc, d), after, recorder)
    }

    /** Merges place, term or faction [mergedKey] into [targetKey] with the engine's [choices] (WLD-15, TC-20-26, TC-20-27). */
    fun mergeItems(
        doc: ProjectDoc,
        kind: ItemKind,
        mergedKey: Key,
        targetKey: Key,
        ctx: ActionContext,
        choices: TrashMergeChoices = TrashMergeChoices(),
        recorder: RecorderLibrary? = null,
    ): MergeActionResult {
        require(kind == ItemKind.PLACE || kind == ItemKind.TERM || kind == ItemKind.FACTION) { "use mergeCharacters for $kind" }
        val result = MergeOps.merge(doc, kind, mergedKey, targetKey, ctx.trash, choices)
        val after = recorder?.let { MergeOps.applyToRecorder(it, doc.key, kind, mergedKey, targetKey) }
        return MergeActionResult(result.doc, result.preview, result.entry, result.record, Messages.mergeToast(result.preview), DocChange(doc, result.doc), after, recorder)
    }

    /**
     * `되돌리기` right after a merge: the project rows go back ([DocChange.revert]); recorder links the
     * merge changed or dropped go back, later edits of other links stay.
     */
    fun undo(current: ProjectDoc, currentRecorder: RecorderLibrary?, result: MergeActionResult): Pair<ProjectDoc, RecorderLibrary?> {
        val doc = result.change.revert(current)
        val before = result.recorderBefore
        val after = result.recorder
        if (currentRecorder == null || before == null || after == null) return doc to currentRecorder
        return doc to currentRecorder.copy(links = revertLinks(currentRecorder.links, before.links, after.links))
    }

    /** Sheet line when restoring a merged item (TC-20-19): `옮긴 참조 5 · 관계 1 · 칸 2 · 노아에 그대로`; null when it was not merged. */
    fun restoreLine(doc: ProjectDoc, mergedKey: Key): String? {
        val record = doc.tombstones.mergeRecords.filter { it.mergedKey == mergedKey }.maxByOrNull { it.at } ?: return null
        val targetName = doc.index.character(record.targetKey)?.name
            ?: doc.index.place(record.targetKey)?.name
            ?: doc.index.term(record.targetKey)?.name
            ?: doc.index.faction(record.targetKey)?.name
            ?: doc.index.nameTag(record.targetKey)?.name
        return Messages.mergedRestoreLine(record, targetName.orEmpty())
    }

    // --- Helpers ---

    private val CHARACTER_CELLS = listOf(MergeSlot.OneLiner, MergeSlot.DisplayName, MergeSlot.Tags, MergeSlot.Speech, MergeSlot.Secret)

    private fun cellId(slot: MergeSlot): String = when (slot) {
        is MergeSlot.Field -> "fields:${slot.defKey}"
        MergeSlot.OneLiner -> "oneLiner"
        MergeSlot.DisplayName -> "displayNameBase"
        MergeSlot.Tags -> "tagKeys"
        MergeSlot.Speech -> "speechBody"
        MergeSlot.Secret -> "secretBody"
    }

    private fun take(target: Character, merged: Character, slot: MergeSlot): Character = when (slot) {
        is MergeSlot.Field -> target
        MergeSlot.OneLiner -> target.copy(oneLiner = merged.oneLiner)
        MergeSlot.DisplayName -> target.copy(displayNameBase = merged.displayNameBase)
        MergeSlot.Tags -> target.copy(tagKeys = merged.tagKeys)
        MergeSlot.Speech -> target.copy(speechBody = merged.speechBody)
        MergeSlot.Secret -> target.copy(secretBody = merged.secretBody)
    }

    /** The merged item's live records of [from] move to [to]; a live [to] record at the same point is dropped. */
    private fun moveRecords(doc: ProjectDoc, from: ChangeTarget, to: ChangeTarget, ctx: ActionContext, moved: MutableList<MovedRef>): ProjectDoc {
        val anchors = doc.changes.records.filter { it.deletedAt == null && it.target.sameAs(from) }.map { it.anchor }
        if (anchors.isEmpty()) return doc
        val records = doc.changes.records.mapNotNull { r ->
            when {
                r.target.sameAs(to) && r.deletedAt == null && anchors.any { it.sameAs(r.anchor) } -> null
                r.target.sameAs(from) -> {
                    moved += MovedRef("record", r.key, "target.characterKey")
                    r.copy(target = to, updatedAt = ctx.now, updatedBy = ctx.device)
                }
                else -> r
            }
        }
        return doc.copy(changes = doc.changes.copy(records = records))
    }

    /** `ext.summary.fields` raised by [extra] (the restore sheet's `칸`). */
    private fun withFieldCount(ext: JsonObject?, extra: Int): JsonObject {
        val base = ext ?: JsonObject(emptyMap())
        val summary = base[MergeOps.SUMMARY] as? JsonObject ?: JsonObject(emptyMap())
        val fields = (summary["fields"]?.jsonPrimitive?.intOrNull ?: 0) + extra
        return JsonObject(base + (MergeOps.SUMMARY to JsonObject(summary + ("fields" to JsonPrimitive(fields)))))
    }

    /** Row-level revert of recorder links by key (as [DocChange.revert] does for project rows). */
    private fun revertLinks(current: List<RecorderLink>, before: List<RecorderLink>, after: List<RecorderLink>): List<RecorderLink> {
        val b = before.associateBy { it.key }
        val a = after.associateBy { it.key }
        val changed = a.filter { (k, v) -> b[k] != null && b[k] != v }.keys
        val removed = b.keys - a.keys
        val added = a.keys - b.keys
        val out = current.filter { it.key !in added || it != a[it.key] }.map { if (it.key in changed && it == a[it.key]) b.getValue(it.key) else it }
        val present = out.map { it.key }.toSet()
        return out + removed.filter { it !in present }.map(b::getValue)
    }

}
