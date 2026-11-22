package io.github.lenticualrstone.civilight.feature.script.editor

import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * Typing applied to runs: one range replace in raw caret units that keeps every untouched run as it
 * is (a link keeps its key, a paren stays a paren). Never collapses the block to one plain run.
 */
object RunEdit {
    /**
     * Replaces the raw range [start, end) with [text].
     *
     * - A range inside one plain, paren or link run is replaced inside that run (Hangul composition
     *   rewrites the last syllable in place, even at a run's end).
     * - A pure insertion strictly inside a run goes into that run (a link's text is edited).
     * - A pure insertion at a run boundary is plain text, or paren text with [parenMode] (into the
     *   paren run ending or starting there, else a new paren run).
     * - Line breaks inside [text] become `Break` runs (pasting several lines).
     */
    fun replace(runs: List<TextRun>, start: Int, end: Int, text: String, parenMode: Boolean = false): List<TextRun> {
        val total = Runs.length(runs)
        val s = start.coerceIn(0, total)
        val e = end.coerceIn(s, total)
        if (s == e && text.isEmpty()) return runs
        if (!text.contains('\n')) {
            homeRun(runs, s, e)?.let { (i, pos) ->
                val edited = replaceInside(runs[i], s - pos, e - pos, text)
                if (edited != null) return Runs.normalize(runs.toMutableList().also { it[i] = edited })
            }
        }
        val afterDelete = delete(runs, s, e)
        return insert(afterDelete, s, text, parenMode)
    }

    /** Index and start position of the run that holds the whole range (not at its edges for an insertion). */
    private fun homeRun(runs: List<TextRun>, start: Int, end: Int): Pair<Int, Int>? {
        var pos = 0
        for ((i, run) in runs.withIndex()) {
            val len = run.shownText.length
            val editable = run is TextRun.Plain || run is TextRun.Paren || run is TextRun.Link
            if (editable) {
                val inside = if (start == end) start > pos && start < pos + len else start >= pos && end <= pos + len
                // A replace that covers a whole link removes the link (it becomes plain text below).
                val wholeLink = run is TextRun.Link && start == pos && end == pos + len
                if (inside && !wholeLink) return i to pos
            }
            pos += len
        }
        return null
    }

    private fun replaceInside(run: TextRun, from: Int, to: Int, text: String): TextRun? = when (run) {
        is TextRun.Plain -> run.copy(text = run.text.substring(0, from) + text + run.text.substring(to))
        is TextRun.Paren -> run.copy(text = run.text.substring(0, from) + text + run.text.substring(to))
        is TextRun.Link -> (run.text.substring(0, from) + text + run.text.substring(to)).takeIf { it.isNotEmpty() }?.let { run.copy(text = it) }
        else -> null
    }

    /** Removes the raw range; runs cut in the middle keep their kind, a link emptied by it goes. */
    fun delete(runs: List<TextRun>, start: Int, end: Int): List<TextRun> {
        if (start >= end) return runs
        val out = mutableListOf<TextRun>()
        var pos = 0
        for (run in runs) {
            val len = run.shownText.length
            val rs = pos
            val re = pos + len
            pos = re
            if (re <= start || rs >= end || len == 0) {
                out += run
                continue
            }
            val from = (start - rs).coerceAtLeast(0)
            val to = (end - rs).coerceAtMost(len)
            when (run) {
                is TextRun.Plain -> out += run.copy(text = run.text.removeRange(from, to))
                is TextRun.Paren -> out += run.copy(text = run.text.removeRange(from, to))
                is TextRun.Link -> run.text.removeRange(from, to).takeIf { it.isNotEmpty() }?.let { out += run.copy(text = it) }
                is TextRun.Break -> Unit
                is TextRun.Unknown -> {
                    val rest = run.shownText.removeRange(from, to)
                    if (rest.isNotEmpty()) out += TextRun.Plain(rest)
                }
            }
        }
        return Runs.normalize(out)
    }

    /** Inserts [text] at raw [at] (no range replaced). */
    private fun insert(runs: List<TextRun>, at: Int, text: String, parenMode: Boolean): List<TextRun> {
        if (text.isEmpty()) return runs
        if (!text.contains('\n')) {
            if (parenMode) return insertParenText(runs, at, text)
            homeRun(runs, at, at)?.let { (i, pos) ->
                replaceInside(runs[i], at - pos, at - pos, text)?.let { edited ->
                    return Runs.normalize(runs.toMutableList().also { it[i] = edited })
                }
            }
            return insertAtBoundary(runs, at, listOf(TextRun.Plain(text)))
        }
        val pieces = mutableListOf<TextRun>()
        text.split('\n').forEachIndexed { i, line ->
            if (i > 0) pieces += TextRun.Break
            if (line.isNotEmpty()) pieces += TextRun.Plain(line)
        }
        return insertAtBoundary(runs, at, pieces)
    }

    /** Paren mode: into the paren run that contains or ends at [at], else a new paren run there. */
    private fun insertParenText(runs: List<TextRun>, at: Int, text: String): List<TextRun> {
        var pos = 0
        for ((i, run) in runs.withIndex()) {
            val len = run.shownText.length
            if (run is TextRun.Paren && at > pos && at <= pos + len) {
                val cut = at - pos
                return runs.toMutableList().also { it[i] = run.copy(text = run.text.substring(0, cut) + text + run.text.substring(cut)) }
            }
            pos += len
        }
        return insertAtBoundary(runs, at, listOf(TextRun.Paren(text)))
    }

    /**
     * Inserts [pieces] at [at]. Inside a plain run the run is cut; inside any other run (only reached
     * for multi-line pastes) the pieces go after that run.
     */
    private fun insertAtBoundary(runs: List<TextRun>, at: Int, pieces: List<TextRun>): List<TextRun> {
        val out = mutableListOf<TextRun>()
        var pos = 0
        var placed = false
        for (run in runs) {
            val len = run.shownText.length
            if (!placed && at <= pos) {
                out += pieces
                placed = true
            }
            if (!placed && at < pos + len) {
                if (run is TextRun.Plain) {
                    val cut = at - pos
                    out += TextRun.Plain(run.text.substring(0, cut))
                    out += pieces
                    out += TextRun.Plain(run.text.substring(cut))
                } else {
                    out += run
                    out += pieces
                }
                placed = true
            } else {
                out += run
            }
            pos += len
        }
        if (!placed) out += pieces
        return Runs.normalize(out)
    }
}
