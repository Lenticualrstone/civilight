package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.trash.MergeRecord
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** A toast (LIB-40): ink surface, one action. [text] is the whole line as the specs quote it. */
data class Toast(val message: String, val action: String? = null) {
    val text: String get() = TrashCopy.join(message, action)
}

/** One count row of a sheet. */
data class SheetRow(val label: String, val sub: String? = null)

/** The impact sheet (`RCDeleteImpact`, CST-40). Rows with a zero count are left out. */
data class ImpactSheet(
    val title: String,
    val sub: String,
    val ownedRows: List<SheetRow>,
    val stayingTitle: String,
    val stayingRows: List<SheetRow>,
    val mergeAction: String?,
) {
    val ownedTitle: String get() = TrashCopy.SHEET_OWNED
    val checkRow: Pair<String, String> get() = TrashCopy.SHEET_CHECK_ROW to TrashCopy.SHEET_CHECK_SUB
    val duplicateRow: Pair<String, String> get() = TrashCopy.SHEET_DUPLICATE to TrashCopy.SHEET_DUPLICATE_SUB
    val footnote: String get() = TrashCopy.SHEET_FOOTNOTE
    val buttons: List<String> get() = listOf(TrashCopy.CANCEL, TrashCopy.TO_TRASH)

    /** `관계 1 · ◆ 변화 기록 2`. */
    val ownedLine: String get() = TrashCopy.join(ownedRows.map { it.label })

    /** `대사 화자 14 · 씬 등장 2 · @연결 3 · 레코더 칩 1`. */
    val stayingLine: String get() = TrashCopy.join(stayingRows.map { it.label })
}

/** Toasts and sheets built from the trash operations' results. */
object Messages {
    /** The impact sheet for one or several items (CST-40; TC-20-01, TC-20-10, TC-20-11). */
    fun impactSheet(impact: DeleteImpact): ImpactSheet {
        val kind = impact.items.firstOrNull()?.kind ?: ItemKind.CHARACTER
        val word = TrashCopy.kindWord(kind)
        val single = impact.items.size == 1
        val owned = listOfNotNull(
            row("관계", impact.owned(OwnedKind.RELATION), impact.relationPartners.takeIf { !impact.hidden && it.isNotEmpty() }?.joinToString(TrashCopy.SEP)),
            row("◇ 첫 공개", impact.owned(OwnedKind.FIRST_REVEAL)),
            row("◆ 변화 기록", impact.owned(OwnedKind.CHANGE_RECORD)),
            row("게임 텍스트", impact.owned(OwnedKind.GAME_TEXT)),
            row("핀", impact.owned(OwnedKind.PIN)),
            row("블록", impact.owned(OwnedKind.BLOCK)),
        )
        val staying = STAYING.mapNotNull { (refKind, label) ->
            val sub = if (refKind == RefKind.SPEAKER) impact.speakerLocations else null
            row(label, impact.count(refKind), sub)
        }
        val title = if (single) "‘${impact.names.firstOrNull().orEmpty()}’ 지우기" else "$word ${impact.items.size} 지우기"
        val sub = if (single) {
            TrashCopy.join(word, "참조 ${impact.referenceTotal}")
        } else {
            TrashCopy.join(listOf("$word ${impact.items.size}") + DeletedItems.countParts(impact.referenceCounts))
        }
        return ImpactSheet(
            title = title,
            sub = sub,
            ownedRows = owned,
            stayingTitle = TrashCopy.join("제자리에 남음", "지운 ${word}로 보임"),
            stayingRows = staying,
            mergeAction = if (kind.isNamedItem && single) "다른 ${word}과 합치기 ›".let { if (word.endsWithVowel()) "다른 ${word}와 합치기 ›" else it } else null,
        )
    }

    /** After `휴지통으로` (CST-40): `인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기`; no references: `인물 노아 지움 · 되돌리기`. */
    fun deleteToast(impact: DeleteImpact): Toast {
        val kind = impact.items.firstOrNull()?.kind ?: ItemKind.CHARACTER
        val word = TrashCopy.kindWord(kind)
        val subject = if (impact.items.size == 1) "$word ${impact.names.firstOrNull().orEmpty()} 지움" else "$word ${impact.items.size} 지움"
        if (impact.referenceTotal == 0 && impact.heldWarnings == 0) return Toast(subject, TrashCopy.UNDO)
        return Toast(
            TrashCopy.join(
                subject,
                "참조 ${impact.referenceTotal}".takeIf { impact.referenceTotal > 0 },
                "▲ 공개 전 사용 ${impact.heldWarnings}".takeIf { impact.heldWarnings > 0 },
                TrashCopy.CHECK_CENTER,
            ),
            TrashCopy.UNDO,
        )
    }

    /** CST-43: `노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3`. */
    fun restoreToast(report: RestoreReport): Toast {
        val relinked = restoreParts(report.relinked)
        val kept = restoreParts(report.keptChanged)
        val parts = mutableListOf<String?>("${report.name.orEmpty()} 되살림")
        if (relinked.isNotEmpty()) parts += listOf("다시 이어짐") + relinked
        if (kept.isNotEmpty()) parts += listOf("그대로 둔 것") + kept
        return Toast(TrashCopy.join(parts))
    }

    /** CST-45: `시장 꼬마 → 노아 합침 · 참조 5 · 되돌리기`. */
    fun mergeToast(preview: MergePreview): Toast =
        Toast(TrashCopy.join("${preview.mergedName} → ${preview.targetName} 합침", "참조 ${preview.referenceCount}"), TrashCopy.UNDO)

    /** Comparison sheet head: `인물 합치기` / `시장 꼬마 → 노아 · 기본은 노아 값`. */
    fun mergeSheetHead(preview: MergePreview): Pair<String, String> =
        "${TrashCopy.kindWord(preview.kind)} 합치기" to TrashCopy.join("${preview.mergedName} → ${preview.targetName}", "기본은 ${preview.targetName} 값")

    /** `빈 칸 채움 · 2`. */
    fun filledLine(preview: MergePreview): String = TrashCopy.join("빈 칸 채움", preview.filled.size.toString())

    /** `서로 관계 1 · 지워짐`, when the two items have a relation. */
    fun mutualRelationLine(preview: MergePreview): String? =
        preview.mutualRelationKey?.let { TrashCopy.join("서로 관계 1", "지워짐") }

    /** Sheet when restoring a merged item (TC-20-19): `옮긴 참조 5 · 관계 1 · 칸 3 · 노아에 그대로`. */
    fun mergedRestoreLine(record: MergeRecord, targetName: String): String {
        val summary = record.ext?.get(MergeOps.SUMMARY) as? JsonObject
        fun n(key: String) = summary?.get(key)?.jsonPrimitive?.intOrNull ?: 0
        return TrashCopy.join("옮긴 참조 ${n("refs")}", "관계 ${n("relations")}", "칸 ${n("fields")}", "${targetName}에 그대로")
    }

    /**
     * SCR-38 block toasts: `블록 #043 지움 · ◆ 1개는 #044 자리로`, `블록 #043 지움`, `블록 5개 지움 ·
     * ◆ 2개 다음 자리로`, `… · ◆ 1개는 예정으로`, `블록 #044 지움 · ◇ 1개는 #045 자리로`, `블록 #043 지움
     * · ◆ 1 · ◇ 1은 #045 자리로`. Null when nothing went to the trash (empty blocks: no toast).
     */
    fun blockDeleteToast(result: DeleteResult): Toast? {
        val blocks = result.trashed.filter { it.kind == ItemKind.BLOCK }
        if (blocks.isEmpty()) return null
        val before = result.change.before
        val after = result.doc
        val subject = if (blocks.size == 1) "블록 ${number(before, blocks.single().key)} 지움" else "블록 ${blocks.size}개 지움"
        val moves = result.relocations
        if (moves.isEmpty()) return Toast(subject, TrashCopy.UNDO)
        val reveals = moves.count { before.index.record(it.recordKey)?.target is ChangeTarget.TermReveal }
        val changes = moves.size - reveals
        val targets = moves.map { it.to }.distinctBy { (it as? Anchor.AtBlock)?.blockKey ?: "unplaced" }
        val place = when {
            blocks.size > 1 -> "다음 자리로"
            targets.size == 1 && targets.single() is Anchor.AtBlock -> "${number(after, (targets.single() as Anchor.AtBlock).blockKey)} 자리로"
            targets.size == 1 -> "예정으로"
            else -> "다음 자리로"
        }
        val counts = when {
            reveals == 0 -> "◆ ${changes}개는 $place"
            changes == 0 -> "◇ ${reveals}개는 $place"
            else -> TrashCopy.join("◆ $changes", "◇ ${reveals}은 $place")
        }
        return Toast(TrashCopy.join(subject, counts), TrashCopy.UNDO)
    }

    /** SCR-43 scene toast: `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로` (scene words as before the delete). */
    fun sceneDeleteToast(result: DeleteResult): Toast? {
        val scene = result.trashed.singleOrNull { it.kind == ItemKind.UNIT } ?: return null
        val before = result.change.before
        val codes = UnitCodes(before)
        val subject = "${codes.sceneShort(scene.key)} 지움"
        val moves = result.relocations
        if (moves.isEmpty()) return Toast(subject, TrashCopy.UNDO)
        val reveals = moves.count { before.index.record(it.recordKey)?.target is ChangeTarget.TermReveal }
        val changes = moves.size - reveals
        val target = moves.first().to
        val place = if (target is Anchor.AtBlock) {
            val targetScene = result.doc.index.block(target.blockKey)?.sceneKey
            val sameParent = targetScene != null && before.index.unit(targetScene)?.parentKey == before.index.unit(scene.key)?.parentKey
            val label = targetScene?.let { if (sameParent) codes.sceneShort(it) else codes.code(it) }
            "$label 처음으로"
        } else {
            "예정으로"
        }
        val counts = when {
            reveals == 0 -> "◆ ${changes}은 $place"
            changes == 0 -> "◇ ${reveals}은 $place"
            else -> TrashCopy.join("◆ $changes", "◇ ${reveals}은 $place")
        }
        return Toast(TrashCopy.join(subject, counts), TrashCopy.UNDO)
    }

    private fun number(doc: ProjectDoc, blockKey: String): String =
        "#" + (doc.index.block(blockKey)?.number ?: 0).toString().padStart(3, '0')

    private fun row(label: String, count: Int, sub: String? = null): SheetRow? =
        if (count > 0) SheetRow("$label $count", sub) else null

    private fun restoreParts(counts: Map<RefKind, Int>): List<String> = RESTORE_ORDER.mapNotNull { (kind, word) ->
        counts[kind]?.takeIf { it > 0 }?.let { "$word $it" }
    }

    private fun String.endsWithVowel(): Boolean {
        val last = lastOrNull() ?: return false
        if (last !in '가'..'힣') return false
        return (last - '가') % 28 == 0
    }

    /** Rows of `제자리에 남음` in sheet order (character rows sourced; world rows `ED`). */
    private val STAYING = listOf(
        RefKind.SPEAKER to "대사 화자",
        RefKind.SILENT_CAST to "씬 등장",
        RefKind.SCENE_PLACE to "씬 장소",
        RefKind.LINK to "@연결",
        RefKind.FIELD_LINK to "연결 값",
        RefKind.RELATED to "관련",
        RefKind.MAP_LINK to "지도 연결",
        RefKind.ROUTE_END to "경로 끝",
        RefKind.SUB_MAP to "하위 지도",
        RefKind.CHILD_PLACE to "하위 장소",
        RefKind.EVENT_PLACE to "사건 장소",
        RefKind.EVENT_TERM to "사건 용어",
        RefKind.EVENT_ANCHOR to "사건 기록",
        RefKind.FLASHBACK to "회상 씬",
        RefKind.CANVAS_NODE to "캔버스",
        RefKind.RECORDER_LINK to "레코더 칩",
    )

    /** Restore toast order (CST-43: `대사 · @ · 씬 등장`, then the others). */
    private val RESTORE_ORDER = listOf(
        RefKind.SPEAKER to "대사",
        RefKind.LINK to "@",
        RefKind.SILENT_CAST to "씬 등장",
        RefKind.SCENE_PLACE to "장소",
        RefKind.FIELD_LINK to "연결",
        RefKind.RELATED to "관련",
        RefKind.MAP_LINK to "지도",
        RefKind.ROUTE_END to "경로 끝",
        RefKind.SUB_MAP to "하위 지도",
        RefKind.CANVAS_NODE to "캔버스",
    )
}
