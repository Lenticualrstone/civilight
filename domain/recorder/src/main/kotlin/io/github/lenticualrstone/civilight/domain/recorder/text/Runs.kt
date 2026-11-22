package io.github.lenticualrstone.civilight.domain.recorder.text

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText

/**
 * Editing recorder text as runs over its shown text (offsets count the characters each run
 * shows; a break counts as one). A link run is atomic: an edit that touches only part of it turns
 * the rest into plain text (the link is gone); an edit that covers all of it removes it.
 */
object Runs {
    fun text(runs: List<TextRun>): String = runs.plainText

    /** NFC text, adjacent plain runs merged, empty plain runs dropped. */
    fun normalize(runs: List<TextRun>): List<TextRun> {
        val out = mutableListOf<TextRun>()
        for (run in runs) {
            val r = when (run) {
                is TextRun.Plain -> run.copy(text = run.text.nfc())
                is TextRun.Paren -> run.copy(text = run.text.nfc())
                is TextRun.Link -> run.copy(text = run.text.nfc())
                else -> run
            }
            if (r is TextRun.Plain && r.text.isEmpty()) continue
            val last = out.lastOrNull()
            if (r is TextRun.Plain && last is TextRun.Plain && last.ext == null && r.ext == null) {
                out[out.size - 1] = TextRun.Plain(last.text + r.text)
            } else {
                out += r
            }
        }
        return out
    }

    fun plain(text: String): List<TextRun> = if (text.isEmpty()) emptyList() else listOf(TextRun.Plain(text.nfc()))

    /** [runs] with the shown range [start, end) replaced by [insert]. */
    fun replace(runs: List<TextRun>, start: Int, end: Int, insert: List<TextRun>): List<TextRun> {
        require(start in 0..end) { "bad range $start..$end" }
        val out = mutableListOf<TextRun>()
        var pos = 0
        var inserted = false
        fun putInsert() {
            if (!inserted) {
                out += insert
                inserted = true
            }
        }
        for (run in runs) {
            val shown = run.shownText
            val a = pos
            val b = pos + shown.length
            pos = b
            when {
                b <= start -> out += run
                a >= end -> {
                    putInsert()
                    out += run
                }
                else -> {
                    // The run overlaps the edited range.
                    val keepHead = shown.substring(0, (start - a).coerceIn(0, shown.length))
                    val keepTail = shown.substring((end - a).coerceIn(0, shown.length))
                    when (run) {
                        is TextRun.Plain, is TextRun.Paren -> {
                            if (keepHead.isNotEmpty()) out += asSameKind(run, keepHead)
                            putInsert()
                            if (keepTail.isNotEmpty()) out += asSameKind(run, keepTail)
                        }
                        else -> {
                            // Links (and breaks, unknown runs) are atomic.
                            if (keepHead.isNotEmpty()) out += TextRun.Plain(keepHead)
                            putInsert()
                            if (keepTail.isNotEmpty()) out += TextRun.Plain(keepTail)
                        }
                    }
                }
            }
        }
        putInsert()
        return normalize(out)
    }

    /** Inserts plain [text] at shown offset [at]. */
    fun insertText(runs: List<TextRun>, at: Int, text: String): List<TextRun> = replace(runs, at, at, plain(text))

    /** Deletes the shown range [start, end). */
    fun delete(runs: List<TextRun>, start: Int, end: Int): List<TextRun> = replace(runs, start, end, emptyList())

    /** Shown ranges of the link runs, in order. */
    fun linkRanges(runs: List<TextRun>): List<Pair<TextRun.Link, IntRange>> {
        val out = mutableListOf<Pair<TextRun.Link, IntRange>>()
        var pos = 0
        for (run in runs) {
            val len = run.shownText.length
            if (run is TextRun.Link) out += run to (pos until pos + len)
            pos += len
        }
        return out
    }

    /** The link runs pointing at [target] turned into plain text with the same characters (`글로 풀기`). */
    fun unlink(runs: List<TextRun>, matches: (TextRun.Link) -> Boolean): List<TextRun> =
        normalize(runs.map { if (it is TextRun.Link && matches(it)) TextRun.Plain(it.text) else it })

    /** A new link run (the `@` itself is not stored: it is drawn as the underline). */
    fun linkRun(linkKey: Key, kind: LinkTargetKind, key: Key, projectKey: Key, text: String): TextRun.Link =
        TextRun.Link(linkKey, LinkTarget(kind, key, projectKey), text.nfc())

    private fun asSameKind(run: TextRun, text: String): TextRun = when (run) {
        is TextRun.Paren -> TextRun.Paren(text)
        else -> TextRun.Plain(text)
    }
}
