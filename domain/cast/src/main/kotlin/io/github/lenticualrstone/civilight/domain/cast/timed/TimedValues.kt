package io.github.lenticualrstone.civilight.domain.cast.timed

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.position.LensPoint
import kotlinx.serialization.json.JsonElement

/** One record of a timed value's history (`FieldHistory` sheet, relation steps). */
data class HistoryRow(
    val record: ChangeRecord,
    /** `1-3`. */
    val point: String,
    /** `1-7 #043`, `1-5 #—`. */
    val fullPoint: String,
    /** The record in effect at the lens (highlighted). */
    val inEffect: Boolean,
    /** Anchored after the lens point. */
    val afterLens: Boolean,
    /** After the 집필 선 (`예정`). */
    val planned: Boolean,
) {
    val note: String? get() = record.note

    /** Status words for the row: `예정` past the 집필 선, `지금 시점보다 뒤` for written records after the lens. */
    val marks: List<String>
        get() = buildList {
            if (afterLens) add(CastCopy.AFTER_LENS)
            if (planned) add(CastCopy.PLANNED)
        }
}

/**
 * The timed-value engine shared by fields, display names and relation slots (rule 11 §1, CST-10):
 * reading with the base fallback, history rows, and the two edit buttons
 * `이 값 고치기` / `여기서부터 바뀜`.
 */
object TimedValues {
    /** History rows of [target] at [lens] in story order. */
    fun history(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint): List<HistoryRow> {
        val inEffect = doc.recordAt(target, lens)
        val lensRank = doc.manuscript.rankOf(lens)
        return doc.history(target).map { r ->
            HistoryRow(
                record = r,
                point = StoryLabels.anchorPoint(doc, r.anchor),
                fullPoint = StoryLabels.anchorFull(doc, r.anchor),
                inEffect = inEffect?.key == r.key,
                afterLens = doc.manuscript.rankOf(r.anchor) > lensRank,
                planned = doc.isPlanned(r),
            )
        }
    }

    /** True when the target has at least one live record (shows `◆`). */
    fun hasHistory(doc: ProjectDoc, target: ChangeTarget): Boolean = doc.history(target).isNotEmpty()

    /** The next live record after the one in effect at [lens] (`예정 · 2-5`, `다음 변화 2-5`). */
    fun nextAfter(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint): ChangeRecord? {
        val lensRank = doc.manuscript.rankOf(lens)
        return doc.history(target).firstOrNull { doc.manuscript.rankOf(it.anchor) > lensRank }
    }

    /**
     * Where `여기서부터 바뀜` puts its record (CST-10, §2.4): at `최신` after the scene holding the
     * 집필 선 (`unitEnd`), at a past lens from the start of that lens unit (`unitStart`, `1-5 #—`).
     * Null at `최신` when nothing is written (no 집필 선): then only `이 값 고치기` is offered.
     */
    fun changeFromHereAnchor(doc: ProjectDoc, lens: LensPoint): Anchor? = when (lens) {
        LensPoint.Latest -> doc.manuscript.writingLineSceneKey?.let { Anchor.UnitEnd(it) }
        is LensPoint.UnitEnd -> if (doc.index.unit(lens.unitKey) != null) Anchor.UnitStart(lens.unitKey) else null
    }

    /** A new live record. */
    fun newRecord(target: ChangeTarget, value: JsonElement?, anchor: Anchor, note: String?, ctx: EditContext): ChangeRecord =
        ChangeRecord(
            key = ctx.newKey(), target = target, value = value, note = note, anchor = anchor,
            createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.by,
        )

    /** Adds a record (`시점 추가`, change sheet `기록`, canvas `관계로 만들기`). */
    fun addRecord(
        doc: ProjectDoc,
        target: ChangeTarget,
        value: JsonElement?,
        anchor: Anchor,
        ctx: EditContext,
        note: String? = null,
    ): Pair<ProjectDoc, ChangeRecord> {
        val record = newRecord(target, value, anchor, note, ctx)
        return doc.copy(changes = doc.changes.copy(records = doc.changes.records + record)) to record
    }

    /** Replaces one record's value (and note when given). */
    fun updateRecordValue(doc: ProjectDoc, recordKey: String, value: JsonElement?, ctx: EditContext, note: String? = null): ProjectDoc =
        doc.copy(
            changes = doc.changes.copy(
                records = doc.changes.records.map { r ->
                    if (r.key != recordKey) r else r.copy(value = value, note = note ?: r.note, updatedAt = ctx.now, updatedBy = ctx.by)
                },
            ),
        )

    /**
     * `이 값 고치기`: overwrites the record in effect at [lens]; with none in effect, the base value
     * through [setBase]. No new record is made.
     */
    fun correctInEffect(
        doc: ProjectDoc,
        target: ChangeTarget,
        value: JsonElement?,
        lens: LensPoint,
        ctx: EditContext,
        setBase: (ProjectDoc) -> ProjectDoc,
    ): ProjectDoc {
        val inEffect = doc.recordAt(target, lens) ?: return setBase(doc)
        return updateRecordValue(doc, inEffect.key, value, ctx)
    }

    /**
     * `여기서부터 바뀜`: a new `◆` at [changeFromHereAnchor]. When that is not available (nothing
     * written), falls back to [correctInEffect].
     */
    fun changeFromHere(
        doc: ProjectDoc,
        target: ChangeTarget,
        value: JsonElement?,
        lens: LensPoint,
        ctx: EditContext,
        setBase: (ProjectDoc) -> ProjectDoc,
        note: String? = null,
    ): ProjectDoc {
        val anchor = changeFromHereAnchor(doc, lens) ?: return correctInEffect(doc, target, value, lens, ctx, setBase)
        return addRecord(doc, target, value, anchor, ctx, note).first
    }

    /** The two-button sheet copy for a value with history (`RTEditTwo`, `LensEdit`). */
    fun editSheet(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint, currentText: String, valueText: (ChangeRecord) -> String): EditSheet {
        val rows = history(doc, target, lens)
        val inEffect = rows.firstOrNull { it.inEffect }
        val lensLabel = StoryLabels.lensPoint(doc, lens)
        val historyLine = rows.joinToString(CastCopy.SEP) { row ->
            val text = "${row.point} ${valueText(row.record)}"
            if (row.planned) "${CastCopy.PLANNED} $text" else text
        }
        val correctSub = if (inEffect != null) {
            "${inEffect.point} 기록을 고침 · ${inEffect.point}부터 새 값"
        } else {
            "처음 값을 고침"
        }
        val changeSub = when (lens) {
            LensPoint.Latest -> StoryLabels.writingLineScene(doc)?.let { "새 ◆ · 집필 선 $it 뒤" }
            is LensPoint.UnitEnd -> "새 ◆ · ${StoryLabels.lensPoint(doc, lens)}부터"
        }
        val banner = when (lens) {
            LensPoint.Latest -> null
            is LensPoint.UnitEnd -> PastLensBanner(
                title = "$lensLabel 시점 값 고치는 중",
                sub = "고치면 ${lensLabel}부터 적용",
                action = "최신으로",
            )
        }
        val saveNote = when (lens) {
            LensPoint.Latest -> null
            is LensPoint.UnitEnd -> changeFromHereAnchor(doc, lens)?.let {
                "저장 시 ◆ 변화 기록 · ${StoryLabels.anchorFull(doc, it)} · 이전 값 ‘$currentText’"
            }
        }
        val since = inEffect?.let { "${it.point}부터" }
        return EditSheet(
            currentLine = CastCopy.join(currentText, since),
            historyLine = historyLine,
            correctLabel = "이 값 고치기",
            correctSub = correctSub,
            changeLabel = if (changeFromHereAnchor(doc, lens) != null) "여기서부터 바뀜 · $lensLabel" else null,
            changeSub = changeSub,
            banner = banner,
            saveNote = saveNote,
        )
    }

    /** History sheet header and buttons (`FieldHistory`). */
    fun historySheet(doc: ProjectDoc, target: ChangeTarget, lens: LensPoint, title: String): HistorySheet {
        val rows = history(doc, target, lens)
        val inEffect = rows.firstOrNull { it.inEffect }
        return HistorySheet(
            title = "$title · 변화 기록",
            sub = "시점별 값 · 프로필에는 ${StoryLabels.lensPoint(doc, lens)} 값",
            rows = rows,
            addLabel = "시점 추가",
            editLabel = inEffect?.let { "${it.point} 값 고치기" } ?: "이 값 고치기",
        )
    }
}

/** `값 고치기` sheet with two buttons (CST-10). */
data class EditSheet(
    /** `등불 결사 · 1-3부터`. */
    val currentLine: String,
    /** `1-1 없음 · 1-3 등불 결사 · 예정 2-5 …`. */
    val historyLine: String,
    val correctLabel: String,
    val correctSub: String,
    /** `여기서부터 바뀜 · 1-7`; null when nothing is written. */
    val changeLabel: String?,
    val changeSub: String?,
    val banner: PastLensBanner?,
    /** `저장 시 ◆ 변화 기록 · 1-5 #— · 이전 값 ‘등불 결사’` at a past lens. */
    val saveNote: String?,
)

/** Glass banner at a past lens (`LensEdit`). */
data class PastLensBanner(val title: String, val sub: String, val action: String)

/** `<항목> · 변화 기록` sheet. */
data class HistorySheet(
    val title: String,
    val sub: String,
    val rows: List<HistoryRow>,
    val addLabel: String,
    val editLabel: String,
)
