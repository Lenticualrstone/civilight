package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.isBlankText
import io.github.lenticualrstone.civilight.model.text.plainText
import java.text.BreakIterator

/**
 * Operations on text runs. Caret offsets count the shown text of each run: a paren run without its
 * parentheses, a link run's text, one character for a break, and an unknown run's text.
 */
object Runs {
    /** Shown length of [runs] in caret units. */
    fun length(runs: List<TextRun>): Int = runs.sumOf { it.shownText.length }

    /**
     * Splits [runs] at the caret [offset] into front and back. Plain and paren runs split inside;
     * a link, break or unknown run is never cut: a caret inside a link keeps the whole link in the
     * front part (`ED`).
     */
    fun splitAt(runs: List<TextRun>, offset: Int): Pair<List<TextRun>, List<TextRun>> {
        val front = mutableListOf<TextRun>()
        val back = mutableListOf<TextRun>()
        var pos = 0
        for (run in runs) {
            val len = run.shownText.length
            when {
                pos + len <= offset -> front += run
                pos >= offset -> back += run
                run is TextRun.Plain -> {
                    front += run.copy(text = run.text.substring(0, offset - pos))
                    back += run.copy(text = run.text.substring(offset - pos))
                }
                run is TextRun.Paren -> {
                    front += run.copy(text = run.text.substring(0, offset - pos))
                    back += run.copy(text = run.text.substring(offset - pos))
                }
                else -> front += run
            }
            pos += len
        }
        return normalize(front) to normalize(back)
    }

    /** Merges adjacent plain runs and drops empty plain and paren runs. */
    fun normalize(runs: List<TextRun>): List<TextRun> {
        val out = mutableListOf<TextRun>()
        for (run in runs) {
            if ((run is TextRun.Plain && run.text.isEmpty()) || (run is TextRun.Paren && run.text.isEmpty())) continue
            val last = out.lastOrNull()
            if (run is TextRun.Plain && last is TextRun.Plain && last.ext == null && run.ext == null) {
                out[out.lastIndex] = last.copy(text = last.text + run.text)
            } else {
                out += run
            }
        }
        return out
    }

    /** Removes leading whitespace of the first text runs (breaks at the start are removed too). */
    fun trimStart(runs: List<TextRun>): List<TextRun> {
        val out = runs.toMutableList()
        while (out.isNotEmpty()) {
            val first = out.first()
            val trimmed = when (first) {
                is TextRun.Plain -> first.copy(text = first.text.trimStart())
                is TextRun.Break -> null
                else -> return normalize(out)
            }
            if (trimmed == null || trimmed.text.isEmpty()) out.removeAt(0) else { out[0] = trimmed; break }
        }
        return normalize(out)
    }

    /** Removes trailing whitespace of the last text runs (breaks at the end are removed too). */
    fun trimEnd(runs: List<TextRun>): List<TextRun> {
        val out = runs.toMutableList()
        while (out.isNotEmpty()) {
            val last = out.last()
            val trimmed = when (last) {
                is TextRun.Plain -> last.copy(text = last.text.trimEnd())
                is TextRun.Break -> null
                else -> return normalize(out)
            }
            if (trimmed == null || trimmed.text.isEmpty()) out.removeAt(out.lastIndex) else { out[out.lastIndex] = trimmed; break }
        }
        return normalize(out)
    }

    /** [front] then [back] joined with one space when both show text (SCR-15 merge, `[PROPOSED]`). */
    fun join(front: List<TextRun>, back: List<TextRun>, separator: String = " "): List<TextRun> = when {
        front.isBlankText() -> normalize(trimEnd(front) + back)
        back.isBlankText() -> normalize(front + trimStart(back))
        else -> normalize(trimEnd(front) + TextRun.Plain(separator) + trimStart(back))
    }

    /** The same runs with every link given a fresh key (a copy is new content, §1.4). */
    fun remintLinks(runs: List<TextRun>, newKey: () -> Key): List<TextRun> =
        runs.map { if (it is TextRun.Link) it.copy(linkKey = newKey()) else it }

    /**
     * Copies recorder text into the manuscript (LRC-32): links get fresh keys and lose the project
     * key; links of kinds the script does not allow become plain text.
     */
    fun fromRecorder(runs: List<TextRun>, newKey: () -> Key): List<TextRun> = normalize(
        runs.map { run ->
            if (run !is TextRun.Link) run
            else if (run.target.kind.known in LinkTargetKind.SCRIPT) {
                run.copy(linkKey = newKey(), target = LinkTarget(run.target.kind, run.target.key))
            } else {
                TextRun.Plain(run.text)
            }
        },
    )

    /**
     * Inserts [text] at [offset] (typing). Inside a paren run the text joins it; anywhere else,
     * including right after a link, it is plain text (`[오리지늄]은`).
     */
    fun insertText(runs: List<TextRun>, offset: Int, text: String): List<TextRun> {
        var pos = 0
        for ((i, run) in runs.withIndex()) {
            val len = run.shownText.length
            if (run is TextRun.Paren && offset > pos && offset < pos + len) {
                val cut = offset - pos
                return runs.toMutableList().also { it[i] = run.copy(text = run.text.substring(0, cut) + text + run.text.substring(cut)) }
            }
            pos += len
        }
        val (front, back) = splitAt(runs, offset)
        return normalize(front + TextRun.Plain(text) + back)
    }

    /** Inserts [run] (a link from the picker, an empty paren run) at [offset]. */
    fun insertRun(runs: List<TextRun>, offset: Int, run: TextRun): List<TextRun> {
        val (front, back) = splitAt(runs, offset)
        return front + run + back
    }

    /**
     * Wraps the shown range [start, end) in a link run (`잇기` on a candidate). Fails when the range
     * crosses a run that is not plain text.
     */
    fun linkRange(runs: List<TextRun>, start: Int, end: Int, linkKey: Key, target: LinkTarget): List<TextRun>? {
        require(start in 0..end)
        val (front, rest) = splitAt(runs, start)
        val (middle, back) = splitAt(rest, end - start)
        if (middle.any { it !is TextRun.Plain }) return null
        val text = middle.plainText
        if (text.isEmpty()) return null
        return front + TextRun.Link(linkKey, target, text) + back
    }

    /** Text without paren runs, single-spaced (excerpts, `괄호 지문` layer off). */
    fun withoutParens(runs: List<TextRun>): String =
        runs.filterNot { it is TextRun.Paren }.plainText.replace(Regex("[ \\t]+"), " ").trim()

    /**
     * Export text: paren runs as `(text)` when [includeParens] (SCR Q-10 default: included), links
     * flattened to their text, breaks as `\n`.
     */
    fun exportText(runs: List<TextRun>, includeParens: Boolean = true): String {
        val raw = buildString {
            var afterParen = false
            for (run in runs) {
                if (run is TextRun.Paren) {
                    if (!includeParens) continue
                    if (isNotEmpty() && !last().isWhitespace()) append(' ')
                    append('(').append(run.text).append(')')
                    afterParen = true
                } else {
                    val text = run.shownText
                    if (afterParen && text.isNotEmpty() && !text.first().isWhitespace()) append(' ')
                    append(text)
                    afterParen = false
                }
            }
        }
        return raw.lines().joinToString("\n") { it.replace(Regex(" {2,}"), " ").trim() }.trim()
    }

    /** Display text with parens drawn `(text)` (manuscript and excerpts with the layer on). */
    fun displayText(runs: List<TextRun>): String = exportText(runs, includeParens = true)

    /** Count of grapheme clusters (STO-06 `ED`: spaces included). */
    fun graphemes(text: String): Int {
        if (text.isEmpty()) return 0
        val it = BreakIterator.getCharacterInstance()
        it.setText(text)
        var count = 0
        while (it.next() != BreakIterator.DONE) count++
        return count
    }
}
