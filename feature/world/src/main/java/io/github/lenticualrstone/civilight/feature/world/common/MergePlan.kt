package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.actions.MergeActions
import io.github.lenticualrstone.civilight.domain.trash.DeleteImpact
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RefKind
import io.github.lenticualrstone.civilight.domain.trash.SheetRow
import io.github.lenticualrstone.civilight.domain.world.item.FieldText
import io.github.lenticualrstone.civilight.domain.world.item.MergeRows
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.TermReveals
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.domain.trash.MergeChoices as TrashMergeChoices

/** The impact sheet of `지우기` (board `RCDeleteImpact` shell, WLD-14 content). */
data class DeleteSheetUi(
    val title: String,
    val sub: String,
    val ownedTitle: String,
    val owned: List<SheetRow>,
    val stayingTitle: String,
    val staying: List<SheetRow>,
    /** `점검 센터 | ◇ 1줄로 모임 · 한꺼번에 바꾸기`. */
    val checkRow: Pair<String, String>,
    /** `중복이면 | 지우지 않고 합치기` with `다른 장소와 합치기 ›`. */
    val duplicateRow: Pair<String, String>,
    val mergeAction: String?,
    val footnote: String,
    val cancel: String,
    val confirm: String,
)

/** One `다른 칸` row: the field and its two values; [fromMerged] = the merged item's value is taken. */
data class MergeConflictUi(val fieldDefKey: Key, val field: String, val mergedValue: String, val targetValue: String, val fromMerged: Boolean)

/** The comparison sheet of `합치기` (board `RCMerge` shell, WLD-15 rows). */
data class MergeSheetUi(
    /** `용어 합치기`. */
    val title: String,
    /** `빛돌 → 오리지늄 등불 · 기본은 오리지늄 등불 값`. */
    val sub: String,
    /** `옮길 참조 · 5`. */
    val refsLine: String,
    /** `@ 3 · 관련 1`. */
    val refsParts: String,
    /** `다른 칸 · 2`, null when no field differs. */
    val conflictsLine: String?,
    /** Column heads `빛돌 · 합쳐짐` | `오리지늄 등불 · 남음`. */
    val mergedHead: String,
    val targetHead: String,
    val conflicts: List<MergeConflictUi>,
    /** Both terms have a `◇ 첫 공개` (`첫 공개 | 1-3 씬 2 · #032` vs `1-7 씬 3 · #044`); null otherwise. */
    val reveal: MergeConflictUi?,
    /** `경로 끝 2 · 잿더미 시장 핀으로`. */
    val routeEnds: String?,
    /** `빈 칸 채움 · 2`, null when nothing is filled. */
    val filledLine: String?,
    /** `빛돌 · ‘합침 → 오리지늄 등불’로 휴지통`. */
    val footer: String,
    val cancel: String,
    val confirm: String,
)

/**
 * Pure rules of `지우기` and `합치기` sheets for world items. Counts and copy come from
 * `domain:trash` (`Messages`, `MergeOps.preview` through `MergeActions.itemPreview`); world rows
 * (`경로 끝`, two `첫 공개`) from `domain:world`.
 */
object MergePlan {
    fun deleteSheet(impact: DeleteImpact, hiddenLocked: Boolean = false): DeleteSheetUi {
        // While routes are closed ([SHOWS_ROUTES] false) route ends are left out of the sheet (its `경로 끝 N` row
        // and the `참조 N` total), display only; the delete itself always turns them dotted.
        val shown = if (SHOWS_ROUTES) impact else impact.copy(references = impact.references.filterNot { it.kind == RefKind.ROUTE_END })
        val sheet = Messages.impactSheet(shown, hiddenLocked)
        return DeleteSheetUi(
            title = sheet.title,
            sub = sheet.sub,
            ownedTitle = sheet.ownedTitle,
            owned = sheet.ownedRows,
            stayingTitle = sheet.stayingTitle,
            staying = sheet.stayingRows,
            checkRow = sheet.checkRow,
            duplicateRow = sheet.duplicateRow,
            mergeAction = sheet.mergeAction,
            footnote = sheet.footnote,
            cancel = sheet.buttons[0],
            confirm = sheet.buttons[1],
        )
    }

    /** Both live, of [kind], and different items. */
    fun canMerge(doc: ProjectDoc, kind: WorldKind, mergedKey: Key, targetKey: Key): Boolean {
        if (mergedKey == targetKey) return false
        return ItemLifecycle.isLive(doc, ItemLifecycle.itemId(kind, mergedKey)) && ItemLifecycle.isLive(doc, ItemLifecycle.itemId(kind, targetKey))
    }

    /** The comparison sheet for [choices] (default: every value of the target). */
    fun mergeSheet(doc: ProjectDoc, kind: WorldKind, mergedKey: Key, targetKey: Key, choices: TrashMergeChoices): MergeSheetUi? {
        if (!canMerge(doc, kind, mergedKey, targetKey)) return null
        val preview = MergeActions.itemPreview(doc, ItemLifecycle.itemKind(kind), mergedKey, targetKey)
        val (title, sub) = Messages.mergeSheetHead(preview)
        val conflicts = preview.fieldConflicts.map {
            MergeConflictUi(
                fieldDefKey = it.fieldDefKey,
                field = it.fieldName,
                mergedValue = FieldText.of(doc, it.mergedValue) ?: WorldCopy.EMPTY_VALUE,
                targetValue = FieldText.of(doc, it.targetValue) ?: WorldCopy.EMPTY_VALUE,
                fromMerged = it.fieldDefKey in choices.fieldsFromMerged,
            )
        }
        val reveal = if (kind == WorldKind.TERM && preview.revealConflict) {
            MergeConflictUi(
                fieldDefKey = REVEAL_ROW,
                field = WorldCopy.ROW_REVEAL,
                mergedValue = TermReveals.of(doc, mergedKey)?.cardValue ?: WorldCopy.EMPTY_VALUE,
                targetValue = TermReveals.of(doc, targetKey)?.cardValue ?: WorldCopy.EMPTY_VALUE,
                fromMerged = choices.revealFromMerged,
            )
        } else {
            null
        }
        val differing = conflicts.size + (if (reveal != null) 1 else 0)
        return MergeSheetUi(
            title = title,
            sub = sub,
            // (0.2.0, WLD-75) Route ends are not counted while routes are hidden, as in [refParts].
            refsLine = WorldCopy.countLine(WorldCopy.MERGE_REFS, shownReferenceCount(preview.referenceCount, preview.references.count { it.kind == RefKind.ROUTE_END })),
            refsParts = refParts(preview.references.groupingBy { it.kind }.eachCount()),
            conflictsLine = if (differing > 0) WorldCopy.countLine(WorldCopy.MERGE_CONFLICTS, differing) else null,
            mergedHead = WorldCopy.withWord(preview.mergedName, WorldCopy.MERGE_MERGED),
            targetHead = WorldCopy.withWord(preview.targetName, WorldCopy.MERGE_KEPT),
            conflicts = conflicts,
            reveal = reveal,
            // (0.2.0, WLD-75) Routes are hidden: no `경로 끝 N · … 핀으로` row (the ends still move to the target's pin).
            routeEnds = if (kind == WorldKind.PLACE && SHOWS_ROUTES) MergeRows.routeEndRow(doc, mergedKey, targetKey) else null,
            filledLine = if (preview.filled.isNotEmpty()) Messages.filledLine(preview) else null,
            footer = WorldCopy.mergedToTrash(preview.mergedName, preview.targetName),
            cancel = WorldCopy.BTN_CANCEL,
            confirm = WorldCopy.MENU_MERGE,
        )
    }

    /** Toggles one `다른 칸` row (or the `첫 공개` row) between the two items. */
    fun toggle(choices: TrashMergeChoices, row: MergeConflictUi, fromMerged: Boolean): TrashMergeChoices = when (row.fieldDefKey) {
        REVEAL_ROW -> choices.copy(revealFromMerged = fromMerged)
        else -> choices.copy(
            fieldsFromMerged = if (fromMerged) choices.fieldsFromMerged + row.fieldDefKey else choices.fieldsFromMerged - row.fieldDefKey,
        )
    }

    /** `대사 3 · @ 1 · 씬 등장 1`: the moved references by kind, in the trash rows' order. */
    fun refParts(counts: Map<RefKind, Int>): String = REF_ORDER.mapNotNull { (kind, word) ->
        // Route ends are not counted while routes are closed.
        if (kind == RefKind.ROUTE_END && !SHOWS_ROUTES) return@mapNotNull null
        counts[kind]?.takeIf { it > 0 }?.let { if (word == "@") "@ $it" else "$word $it" }
    }.joinToString(WorldCopy.SEP)

    /** `옮길 참조 · N` without the route ends while routes are hidden. */
    fun shownReferenceCount(total: Int, routeEnds: Int): Int = if (SHOWS_ROUTES) total else (total - routeEnds).coerceAtLeast(0)

    /** (0.3.0) Routes are drawn and edited again, so the sheets count their pin ends (`경로 끝 N`). */
    const val SHOWS_ROUTES: Boolean = true

    /** Pseudo field key of the `첫 공개` comparison row. */
    const val REVEAL_ROW = "reveal"

    private val REF_ORDER = listOf(
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
