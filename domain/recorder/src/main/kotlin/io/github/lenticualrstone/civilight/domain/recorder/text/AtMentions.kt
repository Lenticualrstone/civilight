package io.github.lenticualrstone.civilight.domain.recorder.text

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.toLinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * `@` in a text memo (LRC-17). The user types `@` and a query; the picker offers candidates; the
 * chosen one replaces `@query` with a link run whose text is the chosen name or alias. The `@`
 * character is not stored (it is drawn as the 2 px underline).
 */
object AtMentions {
    /** Longest query looked back from the caret (`ED`). */
    const val MAX_QUERY = 20

    /** The `@` being typed: [start] = index of `@`, [end] = caret, [query] = text between them. */
    data class AtQuery(val start: Int, val end: Int, val query: String)

    /**
     * The open `@` query at [caret] in the shown text of [runs], or null. The `@` must start the
     * text or follow whitespace or punctuation; the query runs to the caret without a line break,
     * is at most [MAX_QUERY] characters and lies in plain text (not inside an existing link).
     */
    fun queryAt(runs: List<TextRun>, caret: Int): AtQuery? {
        val text = Runs.text(runs)
        if (caret !in 0..text.length) return null
        val from = (caret - MAX_QUERY - 1).coerceAtLeast(0)
        val at = text.lastIndexOf('@', caret - 1).takeIf { it >= from } ?: return null
        if (at > 0 && text[at - 1].isLetterOrDigit()) return null
        val query = text.substring(at + 1, caret)
        if (query.contains('\n') || query.contains('@')) return null
        val linkRanges = Runs.linkRanges(runs).map { it.second }
        if (linkRanges.any { r -> at in r || (caret - 1) in r && caret > at + 1 }) return null
        return AtQuery(at, caret, query)
    }

    /**
     * [runs] with the `@query` replaced by a link run to [target] showing [runText]. Returns the
     * new runs and the caret position after the run.
     */
    fun complete(
        runs: List<TextRun>,
        query: AtQuery,
        target: TargetRef,
        runText: String,
        ctx: RecorderEditContext,
    ): Pair<List<TextRun>, Int> {
        val run = Runs.linkRun(ctx.newKey(), target.kind.toLinkTargetKind(), target.key, target.projectKey, runText)
        val next = Runs.replace(runs, query.start, query.end, listOf(run))
        return next to query.start + run.text.length
    }
}
