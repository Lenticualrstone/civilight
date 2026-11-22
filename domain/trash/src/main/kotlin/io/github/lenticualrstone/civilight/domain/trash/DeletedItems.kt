package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState

/**
 * One check-center row of the filter `지운 항목` (CST-42): one row per deleted item (not per
 * reference), with per-kind counts and the actions the state allows.
 */
data class DeletedItemRow(
    val item: ItemId,
    val name: String,
    /** [RefState.TRASHED] or [RefState.TAGGED] (purged). */
    val state: RefState,
    /** `<d>일 남음` while in the trash; null after purge. */
    val daysLeft: Int?,
    val counts: Map<RefKind, Int>,
    /** `▲ 공개 전 사용` held for a deleted term (`▲ 1 보류`). */
    val heldWarnings: Int,
    /** `이대로 두기` was chosen: the row sits in `맞다고 표시한 것`. */
    val kept: Boolean,
    /** Locked hidden item: name masked (rows group under `▲ 숨긴 항목 관련`). */
    val masked: Boolean,
) {
    val canRestore: Boolean get() = state == RefState.TRASHED

    /** `◇ 지운 인물 · 노아` / `영구 삭제 · 노아` (masked: `영구 삭제 · 숨긴 항목` / `◇ 숨긴 인물`). */
    val label: String
        get() = when {
            state == RefState.TAGGED && masked -> TrashCopy.purgedHiddenLabel
            state == RefState.TAGGED -> TrashCopy.purgedLabel(name)
            masked -> "◇ ${TrashCopy.hiddenKind(item.kind)}"
            else -> "◇ ${TrashCopy.deletedLabel(item.kind, name)}"
        }

    /** `휴지통` while restorable. */
    val location: String? get() = if (state == RefState.TRASHED) TrashCopy.TRASH else null

    /** `3일 남음 · 대사 14 · 씬 등장 2 · @ 3 · 레코더 1` (+ `▲ 1 보류` for a term). */
    val body: String
        get() = TrashCopy.join(
            listOf(daysLeft?.let(TrashCopy::daysLeft)) + DeletedItems.countParts(counts) +
                listOf(if (heldWarnings > 0) "▲ $heldWarnings 보류" else null),
        )

    /** One bulk button per reference kind (CST-42), then `되살리기` (while in the trash) and `이대로 두기`. */
    val actions: List<String>
        get() = DeletedItems.bulkActions(item.kind, counts) + listOfNotNull(TrashCopy.RESTORE.takeIf { canRestore }, TrashCopy.KEEP)

    /** The whole row as one line: `◇ 지운 인물 · 노아 · 3일 남음 · 대사 14 · …`. */
    val line: String get() = TrashCopy.join(label, body)
}

object DeletedItems {
    /**
     * Rows for every character · place · term · faction that is in the trash (directly) or only
     * left as a purged name tag, and that live data still references.
     */
    fun rows(doc: ProjectDoc, ctx: TrashContext, recorder: RecorderLibrary? = null, locks: HidingLocks = HidingLocks()): List<DeletedItemRow> {
        val scanner = ReferenceScanner(doc, recorder)
        val trashed = buildList {
            doc.cast.characters.filter { it.deletedAt != null }.forEach { add(ItemId.character(it.key)) }
            doc.world.places.filter { it.deletedAt != null }.forEach { add(ItemId.place(it.key)) }
            doc.world.terms.filter { it.deletedAt != null }.forEach { add(ItemId.term(it.key)) }
            doc.world.factions.filter { it.deletedAt != null }.forEach { add(ItemId.faction(it.key)) }
        }
        val trashedRows = trashed.mapNotNull { item ->
            val refs = scanner.referencesTo(item)
            if (refs.isEmpty()) return@mapNotNull null
            val row = doc.rowOf(item)!!
            val heldWarnings = if (item.kind == ItemKind.TERM) {
                Impact.heldWarnings(doc.setMark(item, null, null, row.deletedAt ?: 0L, ""), doc, item.key)
            } else {
                0
            }
            DeletedItemRow(
                item = item, name = doc.itemName(item).orEmpty(), state = RefState.TRASHED,
                daysLeft = row.deletedAt?.let(ctx::daysLeft), counts = refs.groupingBy { it.kind }.eachCount(),
                heldWarnings = heldWarnings, kept = Fixes.isKept(doc, item.key), masked = locks.itemsLocked && doc.itemHidden(item),
            )
        }
        val taggedRows = doc.tombstones.nameTags.filter { it.state.known == NameTagState.PURGED }.mapNotNull { tag ->
            val kind = when (tag.kind.known) {
                NameTagKind.CHARACTER -> ItemKind.CHARACTER
                NameTagKind.PLACE -> ItemKind.PLACE
                NameTagKind.TERM -> ItemKind.TERM
                NameTagKind.FACTION -> ItemKind.FACTION
                else -> return@mapNotNull null
            }
            val item = ItemId(kind, tag.key)
            if (doc.exists(item)) return@mapNotNull null
            val refs = scanner.referencesTo(item)
            if (refs.isEmpty()) return@mapNotNull null
            DeletedItemRow(
                item = item, name = tag.name, state = RefState.TAGGED, daysLeft = null,
                counts = refs.groupingBy { it.kind }.eachCount(), heldWarnings = 0,
                kept = Fixes.isKept(doc, item.key), masked = locks.itemsLocked && tag.hidden,
            )
        }
        return trashedRows + taggedRows
    }

    /** Count fragments in the check-row order: `대사 · 씬 등장 · @ · 레코더`, then the world kinds (`ED`). */
    internal fun countParts(counts: Map<RefKind, Int>): List<String> = ORDER.mapNotNull { (kind, word) ->
        counts[kind]?.takeIf { it > 0 }?.let { if (word == "@") "@ $it" else "$word $it" }
    }

    /** `화자 한꺼번에 바꾸기` and the other per-kind bulk buttons (화자 · 장소 · @ · 연결, CST-42). */
    internal fun bulkActions(kind: ItemKind, counts: Map<RefKind, Int>): List<String> = buildList {
        if ((counts[RefKind.SPEAKER] ?: 0) > 0 && kind == ItemKind.CHARACTER) add("화자 한꺼번에 바꾸기")
        if ((counts[RefKind.SCENE_PLACE] ?: 0) > 0) add("장소 한꺼번에 바꾸기")
        if ((counts[RefKind.LINK] ?: 0) > 0) add("@ 한꺼번에 바꾸기")
        if ((counts[RefKind.FIELD_LINK] ?: 0) > 0) add("연결 한꺼번에 바꾸기")
    }

    private val ORDER = listOf(
        RefKind.SPEAKER to "대사",
        RefKind.SILENT_CAST to "씬 등장",
        RefKind.SCENE_PLACE to "장소",
        RefKind.LINK to "@",
        RefKind.FIELD_LINK to "연결",
        RefKind.RELATED to "관련",
        RefKind.MAP_LINK to "지도",
        RefKind.ROUTE_END to "경로 끝",
        RefKind.SUB_MAP to "하위 지도",
        RefKind.CHILD_PLACE to "하위 장소",
        RefKind.EVENT_PLACE to "사건",
        RefKind.EVENT_TERM to "사건",
        RefKind.CANVAS_NODE to "캔버스",
        RefKind.RECORDER_LINK to "레코더",
    )
}
