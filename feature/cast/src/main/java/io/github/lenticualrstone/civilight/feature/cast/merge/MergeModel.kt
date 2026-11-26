package io.github.lenticualrstone.civilight.feature.cast.merge

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.MergeActionResult
import io.github.lenticualrstone.civilight.domain.actions.MergeActions
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.domain.cast.merge.MergeChoices
import io.github.lenticualrstone.civilight.domain.cast.merge.MergePick
import io.github.lenticualrstone.civilight.domain.cast.merge.MergePlanner
import io.github.lenticualrstone.civilight.domain.cast.merge.MergeSlot
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RefKind
import io.github.lenticualrstone.civilight.domain.world.item.FieldText
import io.github.lenticualrstone.civilight.domain.world.item.MergeRows
import io.github.lenticualrstone.civilight.domain.world.term.TermReveals
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.domain.trash.MergeChoices as ItemChoices

/**
 * One row with two radio cells (`RCMerge`): [label] in the left column (with [sub] under it, e.g.
 * `사이` for a relation slot), the merged item's value, then the target's. [fromMerged] = the merged
 * value is taken; the default is always the target's.
 */
data class MergeCellView(
    val id: String,
    val label: String,
    val sub: String?,
    val mergedText: String,
    val targetText: String,
    val fromMerged: Boolean,
)

/** The comparison sheet (`RCMerge`, CST-45, WLD-15; S22) for any named kind. */
data class MergeView(
    val title: String,
    val sub: String,
    /** `옮길 참조 · 5` and `대사 3 · @연결 1 · 씬 등장 1` (empty when nothing moves). */
    val movedTitle: String,
    val movedLine: String,
    /** `다른 칸 · 3`, null when no cell differs. */
    val differingTitle: String?,
    /** Column heads `노아(시장) · 합쳐짐` | `노아 · 남음`. */
    val columns: Pair<String, String>,
    val cells: List<MergeCellView>,
    /** `관계 겹침 · 1`, null without overlaps (characters only). */
    val overlapTitle: String?,
    val overlaps: List<MergeCellView>,
    /** `같은 항목 | 6 · 펼치기` (characters) or `같은 항목 | 6`. */
    val sameRow: Pair<String, String>?,
    /** Labels of the equal cells, listed when `펼치기` is pressed (characters only). */
    val sameLabels: List<String>,
    /** `빈 칸 채움 | 출생 · 성격 · 2`, `보이스 ID | 고정 2 · 그대로 · ◇`, `서로 관계 1 · 지워짐`, `경로 끝 2 · …`. */
    val infoRows: List<Pair<String, String>>,
    val footnote: String,
    val cancelLabel: String,
    val confirmLabel: String,
)

/**
 * 합치기 for characters, places, terms and factions (CST-45, CST-46, WLD-15). The sheet text of a
 * character comes from `domain:cast` [MergePlanner] (through [MergeActions.characterSheet]); world
 * items use the engine's preview ([MergeActions.itemPreview]) with `domain:world` rows. The write is
 * always `domain:actions` [MergeActions] (DECISIONS §1, §4). The author's picks are a set of cell ids
 * (saved with the sheet): `f:<field>`, `c:<cell>`, `r:<partner>:<slot>`, [REVEAL].
 */
object MergeModel {
    /**
     * (0.2.0, CST-73) The comparison shows secret values, so it opens only after the device lock when
     * either character's `이 탭 잠그기` is on. Other kinds have no 비밀 tab.
     */
    fun needsSecretAuth(doc: ProjectDoc, kind: EntityKind, mergedKey: Key, targetKey: Key): Boolean =
        kind == EntityKind.CHARACTER &&
            listOf(mergedKey, targetKey).any { doc.index.character(it)?.secretTabLocked == true }

    const val REVEAL = "reveal"
    private const val FIELD = "f:"
    private const val CELL = "c:"
    private const val RELATION = "r:"

    /** Both live (hidden ones count: the caller opened them), of [kind], and different. */
    fun canMerge(doc: ProjectDoc, kind: EntityKind, mergedKey: Key, targetKey: Key): Boolean =
        kind in MERGEABLE && mergedKey != targetKey &&
            ItemTrash.isLive(doc, kind, mergedKey) && ItemTrash.isLive(doc, kind, targetKey)

    /** `인물 합치기` (the picker title and the sheet title). */
    fun title(kind: EntityKind): String = "${kind.label} 합치기"

    /**
     * The sheet for merging [mergedKey] into [targetKey]. While hidden items are locked
     * ([hiddenUnlocked] false) a relation overlap with a hidden partner reads `숨긴 인물` (rule 19 §6).
     */
    fun view(
        doc: ProjectDoc,
        kind: EntityKind,
        mergedKey: Key,
        targetKey: Key,
        picks: Set<String>,
        recorder: RecorderLibrary? = null,
        hiddenUnlocked: Boolean = false,
    ): MergeView? {
        if (!canMerge(doc, kind, mergedKey, targetKey)) return null
        return if (kind == EntityKind.CHARACTER) characterView(doc, mergedKey, targetKey, picks, recorder, hiddenUnlocked) else itemView(doc, kind, mergedKey, targetKey, picks)
    }

    /** Toggles a cell to the merged (`true`) or the target (`false`) value. */
    fun pick(picks: Set<String>, id: String, fromMerged: Boolean): Set<String> = if (fromMerged) picks + id else picks - id

    /** The cast sheet's choices (characters). */
    fun characterChoices(picks: Set<String>): MergeChoices = MergeChoices(
        cells = picks.mapNotNull { id -> slotOf(id)?.let { it to MergePick.MERGED } }.toMap(),
        relationSlots = picks.mapNotNull { id -> relationOf(id)?.let { it to MergePick.MERGED } }.toMap(),
    )

    /** The engine's choices (places, terms, factions). */
    fun itemChoices(picks: Set<String>): ItemChoices = ItemChoices(
        fieldsFromMerged = picks.filter { it.startsWith(FIELD) }.map { it.removePrefix(FIELD) }.toSet(),
        revealFromMerged = REVEAL in picks,
    )

    /**
     * `합치기`: [mergedKey] into [targetKey] with the picks. Null when either item is no longer live
     * (it changed while the sheet was open). The recorder library goes in so the result carries its
     * before / after for `되돌리기`.
     */
    fun merge(doc: ProjectDoc, kind: EntityKind, mergedKey: Key, targetKey: Key, picks: Set<String>, ctx: ActionContext, recorder: RecorderLibrary?): MergeActionResult? {
        if (!canMerge(doc, kind, mergedKey, targetKey)) return null
        return if (kind == EntityKind.CHARACTER) {
            MergeActions.mergeCharacters(doc, mergedKey, targetKey, characterChoices(picks), ctx, recorder)
        } else {
            MergeActions.mergeItems(doc, ItemKind.of(kind), mergedKey, targetKey, ctx, itemChoices(picks), recorder)
        }
    }

    // --- Characters ---

    private fun characterView(doc: ProjectDoc, mergedKey: Key, targetKey: Key, picks: Set<String>, recorder: RecorderLibrary?, hiddenUnlocked: Boolean): MergeView {
        val p = MergeActions.characterSheet(doc, mergedKey, targetKey, recorder?.links.orEmpty())
        val cells = p.differing.map { row ->
            val id = idOf(row.slot)
            MergeCellView(id, row.label, null, row.mergedText.ifBlank { CastUiCopy.EMPTY_VALUE }, row.targetText.ifBlank { CastUiCopy.EMPTY_VALUE }, id in picks)
        }
        val overlaps = p.relationOverlaps.map { row ->
            val id = relationId(row.partnerKey, row.slot)
            val masked = !hiddenUnlocked && doc.index.character(row.partnerKey)?.hidden == true
            MergeCellView(id, if (masked) CastCopy.HIDDEN_CHARACTER else row.partnerName, if (masked) null else row.slotLabel, row.mergedText.ifBlank { CastUiCopy.EMPTY_VALUE }, row.targetText.ifBlank { CastUiCopy.EMPTY_VALUE }, id in picks)
        }
        val merged = doc.index.character(mergedKey)!!
        val target = doc.index.character(targetKey)!!
        val sameLabels = MergePlanner.cells(doc, mergedKey, targetKey).filter { slot ->
            val m = MergePlanner.data(doc, merged, slot)
            m != null && m == MergePlanner.data(doc, target, slot)
        }.map { MergePlanner.label(doc, it) }
        return MergeView(
            title = p.title,
            sub = p.sub,
            movedTitle = p.movedTitle,
            movedLine = p.movedLine,
            differingTitle = p.differingTitle.takeIf { cells.isNotEmpty() },
            columns = p.columns,
            cells = cells,
            overlapTitle = p.overlapTitle.takeIf { overlaps.isNotEmpty() },
            overlaps = overlaps,
            sameRow = p.sameRow.takeIf { p.sameCount > 0 },
            sameLabels = sameLabels,
            infoRows = listOfNotNull(
                p.filledRow.takeIf { p.filled.isNotEmpty() },
                // CST-74: the voice-ID row belongs to the game tools; the IDs are kept and moved either way.
                p.voiceRow.takeIf { GameTools.on(doc) },
                p.mutualRow?.let(::splitRow),
            ),
            footnote = p.footnote,
            cancelLabel = p.buttons.first,
            confirmLabel = p.buttons.second,
        )
    }

    // --- Places, terms, factions ---

    private fun itemView(doc: ProjectDoc, kind: EntityKind, mergedKey: Key, targetKey: Key, picks: Set<String>): MergeView {
        val p = MergeActions.itemPreview(doc, ItemKind.of(kind), mergedKey, targetKey)
        val (title, sub) = Messages.mergeSheetHead(p)
        val cells = p.fieldConflicts.map {
            val id = FIELD + it.fieldDefKey
            MergeCellView(
                id, it.fieldName, null,
                FieldText.of(doc, it.mergedValue) ?: CastUiCopy.EMPTY_VALUE,
                FieldText.of(doc, it.targetValue) ?: CastUiCopy.EMPTY_VALUE,
                id in picks,
            )
        } + listOfNotNull(
            if (kind == EntityKind.TERM && p.revealConflict) {
                MergeCellView(
                    REVEAL, CastUiCopy.FIRST_REVEAL, null,
                    TermReveals.of(doc, mergedKey)?.cardValue ?: CastUiCopy.EMPTY_VALUE,
                    TermReveals.of(doc, targetKey)?.cardValue ?: CastUiCopy.EMPTY_VALUE,
                    REVEAL in picks,
                )
            } else {
                null
            },
        )
        val counts = p.references.groupingBy { it.kind }.eachCount()
        return MergeView(
            title = title,
            sub = sub,
            movedTitle = CastUiCopy.movedTitle(p.referenceCount),
            movedLine = refParts(counts),
            differingTitle = CastUiCopy.differingTitle(cells.size).takeIf { cells.isNotEmpty() },
            columns = CastUiCopy.mergedColumn(p.mergedName) to CastUiCopy.keptColumn(p.targetName),
            cells = cells,
            overlapTitle = null,
            overlaps = emptyList(),
            sameRow = (CastUiCopy.SAME_FIELDS to p.sameFields.toString()).takeIf { p.sameFields > 0 },
            sameLabels = emptyList(),
            infoRows = listOfNotNull(
                Messages.filledLine(p).takeIf { p.filled.isNotEmpty() }?.let(::splitRow),
                if (kind == EntityKind.PLACE) MergeRows.routeEndRow(doc, mergedKey, targetKey)?.let(::splitRow) else null,
                Messages.mutualRelationLine(p)?.let(::splitRow),
            ),
            footnote = CastUiCopy.mergedFootnote(p.mergedName, p.targetName),
            cancelLabel = CastUiCopy.BTN_CANCEL,
            confirmLabel = CastUiCopy.BTN_MERGE,
        )
    }

    /** `경로 끝 2 · 잿더미 시장 핀으로` → `경로 끝 2` | `잿더미 시장 핀으로`. */
    private fun splitRow(line: String): Pair<String, String> =
        line.substringBefore(CastCopy.SEP) to line.substringAfter(CastCopy.SEP, "")

    /** `대사 3 · @ 1 · 씬 등장 1`: the moved references by kind (the check rows' order). */
    fun refParts(counts: Map<RefKind, Int>): String = REF_ORDER.mapNotNull { (kind, word) ->
        counts[kind]?.takeIf { it > 0 }?.let { "$word $it" }
    }.joinToString(CastCopy.SEP)

    // --- Ids ---

    private fun idOf(slot: MergeSlot): String = when (slot) {
        is MergeSlot.Field -> FIELD + slot.defKey
        MergeSlot.OneLiner -> CELL + "oneLiner"
        MergeSlot.DisplayName -> CELL + "displayName"
        MergeSlot.Tags -> CELL + "tags"
        MergeSlot.Speech -> CELL + "speech"
        MergeSlot.Secret -> CELL + "secret"
    }

    private fun slotOf(id: String): MergeSlot? = when {
        id.startsWith(FIELD) -> MergeSlot.Field(id.removePrefix(FIELD))
        id == CELL + "oneLiner" -> MergeSlot.OneLiner
        id == CELL + "displayName" -> MergeSlot.DisplayName
        id == CELL + "tags" -> MergeSlot.Tags
        id == CELL + "speech" -> MergeSlot.Speech
        id == CELL + "secret" -> MergeSlot.Secret
        else -> null
    }

    private fun relationId(partner: Key, slot: RelationSlotName): String = "$RELATION$partner:${slot.name}"

    private fun relationOf(id: String): Pair<Key, RelationSlotName>? {
        if (!id.startsWith(RELATION)) return null
        val rest = id.removePrefix(RELATION)
        val cut = rest.lastIndexOf(':')
        if (cut <= 0) return null
        val slot = runCatching { RelationSlotName.valueOf(rest.substring(cut + 1)) }.getOrNull() ?: return null
        return rest.substring(0, cut) to slot
    }

    private val MERGEABLE = setOf(EntityKind.CHARACTER, EntityKind.PLACE, EntityKind.TERM, EntityKind.FACTION)

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
        RefKind.BASE_PLACE to "거점",
        RefKind.EVENT_PLACE to "사건",
        RefKind.EVENT_TERM to "사건",
        RefKind.CANVAS_NODE to "캔버스",
        RefKind.RECORDER_LINK to "레코더",
    )
}
