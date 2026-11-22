package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.term.BulkMode
import io.github.lenticualrstone.civilight.domain.world.term.BulkSheet
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** One row of `후보 N · <용어>`: checkbox, `1-2 씬 1 · 리엔 · 끊김`, the whole sentence with the span boxed. */
data class BulkRowUi(
    val id: String,
    val location: String,
    val text: String,
    val boxStart: Int,
    val boxEnd: Int,
    val checked: Boolean,
)

/**
 * The sheet `모두 잇기 · 모두 아님` (WLD-23, board `RTLinkAll`): title `후보 3 · 오리지늄 등불`, the
 * segmented `모두 잇기 | 모두 아님`, rows (all checked at first), footer
 * `고른 3개 · 넣으면 ▲ 공개 전 사용 1 → 4` (모두 잇기 only, only when the count changes), the button
 * `넣기 · 3개` and the note `넣은 뒤 · 한 단계로 되돌리기`. Every word comes from `domain:world`
 * [BulkSheet].
 */
data class BulkSheetUi(
    val title: String,
    val mode: BulkMode,
    val rows: List<BulkRowUi>,
    val footer: String?,
    val button: String,
    val note: String,
    val checkedCount: Int,
)

/** What the sheet shows: the rows, the first scan still running, or nothing (no candidate, term gone or locked). */
sealed interface BulkSheetState {
    data class Ready(val ui: BulkSheetUi) : BulkSheetState

    /** The index is still pending (`점검 중`): candidate numbers are not final, `넣기` waits. */
    data object Pending : BulkSheetState

    data object Empty : BulkSheetState
}

/** What `넣기` did. */
sealed interface BulkApply {
    /** Applied as one undo step; [count] rows. */
    data class Done(val doc: ProjectDoc, val count: Int) : BulkApply

    /** A checked row is no longer in the text (it changed meanwhile): rebuild and show again. */
    data object Stale : BulkApply

    /** No row checked. */
    data object Nothing : BulkApply
}

/**
 * Pure state of the bulk sheet. Saved state is only the mode and the ids of unchecked rows, so
 * the rows are rebuilt from the current index each time (a row that went away simply drops out).
 */
object LinkCandidatesState {
    /** Stable id of a candidate row (also used by the card's `후보` rows). */
    fun idOf(c: TermCandidate): String = "cand:${c.spot}:${c.start}:${c.matchedText}:${c.occurrence}"

    fun sheet(doc: ProjectDoc, termKey: Key, index: CandidateIndex, hiddenOpen: Boolean, mode: BulkMode, unchecked: Set<String>): BulkSheet? {
        val term = doc.index.term(termKey) ?: return null
        if (term.deletedAt != null || (term.hidden && !hiddenOpen)) return null
        val queries = TermQueries(doc, index, TermAccess(hiddenOpen))
        val opened = BulkSheet.open(queries, termKey, mode) ?: return null
        return opened.rows.fold(opened) { sheet, row -> if (idOf(row.candidate) in unchecked) sheet.toggle(row.candidate) else sheet }
    }

    fun build(doc: ProjectDoc, termKey: Key, index: CandidateIndex, hiddenOpen: Boolean, mode: BulkMode, unchecked: Set<String>): BulkSheetState {
        if (!index.complete) return BulkSheetState.Pending
        val sheet = sheet(doc, termKey, index, hiddenOpen, mode, unchecked) ?: return BulkSheetState.Empty
        if (sheet.rows.isEmpty()) return BulkSheetState.Empty
        val rows = sheet.rows.map { row ->
            val (text, start, end) = TermCardState.shown(doc, row.candidate.spot, row.snippet, hiddenOpen)
            BulkRowUi(idOf(row.candidate), row.location, text, start, end, row.checked)
        }
        return BulkSheetState.Ready(
            BulkSheetUi(
                title = sheet.title,
                mode = sheet.mode,
                rows = rows,
                footer = sheet.footer,
                button = sheet.button,
                note = sheet.note,
                checkedCount = sheet.checkedCount,
            ),
        )
    }

    /** `넣기`: every checked row in one transaction (one undo step reverts all). */
    fun apply(
        doc: ProjectDoc,
        termKey: Key,
        index: CandidateIndex,
        hiddenOpen: Boolean,
        mode: BulkMode,
        unchecked: Set<String>,
        ctx: EditContext,
    ): BulkApply {
        val sheet = sheet(doc, termKey, index, hiddenOpen, mode, unchecked) ?: return BulkApply.Stale
        if (sheet.checkedCount == 0) return BulkApply.Nothing
        val linked = sheet.apply(doc, ctx) ?: return BulkApply.Stale
        return BulkApply.Done(linked.doc, sheet.checkedCount)
    }

    /** The segmented control's labels in order (`모두 잇기 | 모두 아님`). */
    val modes: List<BulkMode> = listOf(BulkMode.LINK_ALL, BulkMode.REJECT_ALL)
}
