package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** `모두 잇기 | 모두 아님`. */
enum class BulkMode { LINK_ALL, REJECT_ALL }

/** One row of the bulk sheet: `1-2 씬 1 · 리엔 · 끊김` and the boxed sentence. */
data class BulkRow(val candidate: TermCandidate, val checked: Boolean, val location: String, val snippet: Snippet)

/**
 * The sheet `후보 N · <용어>` (WLD-23). Rows start checked; [apply] runs every checked row in one
 * transaction, so one undo (the doc from before) reverts all.
 */
data class BulkSheet(
    val termKey: Key,
    val termName: String,
    val mode: BulkMode,
    val rows: List<BulkRow>,
    /** `▲ 공개 전 사용` count now (A). */
    private val warningsNow: Int,
    /** Checked rows that are before the reveal (each adds one `▲` when linked). */
    private val preReveal: Set<TermCandidate>,
) {
    val title: String get() = "후보 ${rows.size}${Copy.SEP}$termName"
    val checkedCount: Int get() = rows.count { it.checked }

    /** `고른 3개 · 넣으면 ▲ 공개 전 사용 1 → 4`, only in `모두 잇기` and only when the count changes. */
    val footer: String?
        get() {
            if (mode != BulkMode.LINK_ALL) return null
            val after = warningsNow + rows.count { it.checked && it.candidate in preReveal }
            if (after == warningsNow) return null
            return "고른 ${checkedCount}개${Copy.SEP}넣으면 ▲ 공개 전 사용 $warningsNow → $after"
        }

    val button: String get() = "넣기${Copy.SEP}${checkedCount}개"
    val note: String get() = "넣은 뒤${Copy.SEP}한 단계로 되돌리기"

    fun withMode(mode: BulkMode): BulkSheet = copy(mode = mode)

    fun toggle(candidate: TermCandidate): BulkSheet =
        copy(rows = rows.map { if (it.candidate == candidate) it.copy(checked = !it.checked) else it })

    /** Applies the checked rows; for `모두 잇기` the new link keys come with the doc. Null when a row went stale. */
    fun apply(doc: ProjectDoc, ctx: EditContext): Linked? {
        val chosen = rows.filter { it.checked }.map { it.candidate }
        return when (mode) {
            BulkMode.LINK_ALL -> TermEdits.linkAll(doc, chosen, ctx)
            BulkMode.REJECT_ALL -> Linked(TermEdits.rejectAll(doc, chosen, ctx), emptyList())
        }
    }

    companion object {
        /** Opens the sheet for a term from its card's `후보` group. */
        fun open(queries: TermQueries, termKey: Key, mode: BulkMode): BulkSheet? {
            val term = queries.term(termKey) ?: return null
            val candidates = queries.candidates(termKey)
            val rows = candidates.map { c ->
                val parts = listOfNotNull(queries.describer.location(c.spot), queries.describer.speaker(c.spot), queries.candidateTag(c))
                BulkRow(c, checked = true, location = parts.joinToString(Copy.SEP), snippet = queries.snippetOf(c))
            }
            return BulkSheet(
                termKey = termKey,
                termName = term.name,
                mode = mode,
                rows = rows,
                warningsNow = queries.activeWarnings(termKey).size,
                preReveal = candidates.filter(queries::isBeforeReveal).toSet(),
            )
        }
    }
}

/** The toast after a reveal block moved (WLD-18): `◇ 첫 공개 1-7 → 1-8 · ▲ 새로 1`. */
object RevealMoves {
    /** Links that became `▲` between [before] and [after]. */
    fun newWarnings(before: TermQueries, after: TermQueries, termKey: Key): List<TermLink> {
        val old = before.activeWarnings(termKey).map { it.linkKey }.toSet()
        return after.activeWarnings(termKey).filter { it.linkKey !in old }
    }

    fun toast(before: TermQueries, after: TermQueries, termKey: Key): String? {
        val from = before.reveal(termKey)?.episodeLabel ?: return null
        val to = after.reveal(termKey)?.episodeLabel ?: return null
        val added = newWarnings(before, after, termKey).size
        return buildString {
            append("◇ 첫 공개 $from → $to")
            if (added > 0) append("${Copy.SEP}▲ 새로 $added")
        }
    }
}
