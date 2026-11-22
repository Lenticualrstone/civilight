package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Pure edits of text runs. Offsets are in run coordinates ([TextSpots.textOf]). */
object RunEdits {
    /**
     * Wraps exactly [start, end) in [link] (its text becomes the span). The span must lie in plain
     * runs only; returns null otherwise.
     */
    fun wrap(runs: List<TextRun>, start: Int, end: Int, link: TextRun.Link): List<TextRun>? {
        if (start < 0 || end <= start) return null
        val out = ArrayList<TextRun>(runs.size + 2)
        val linkText = StringBuilder()
        var cursor = 0
        var placed = false
        for (run in runs) {
            val len = run.shownText.length
            val rs = cursor
            val re = cursor + len
            cursor = re
            if (len == 0 || re <= start || rs >= end) {
                out += run
                continue
            }
            if (run !is TextRun.Plain) return null
            val a = maxOf(start, rs) - rs
            val b = minOf(end, re) - rs
            if (a > 0) out += run.copy(text = run.text.substring(0, a))
            linkText.append(run.text, a, b)
            if (re >= end) {
                out += link.copy(text = linkText.toString())
                placed = true
                if (b < len) out += run.copy(text = run.text.substring(b))
            }
        }
        return if (placed) out else null
    }

    /** Replaces the link [linkKey] by plain text and merges neighbouring plain runs; null when absent. */
    fun unwrap(runs: List<TextRun>, linkKey: Key): List<TextRun>? {
        if (runs.none { it is TextRun.Link && it.linkKey == linkKey }) return null
        return mergePlain(runs.map { if (it is TextRun.Link && it.linkKey == linkKey) TextRun.Plain(it.text) else it })
    }

    /** Merges adjacent plain runs without extension data; drops empty plain runs. */
    fun mergePlain(runs: List<TextRun>): List<TextRun> {
        val out = ArrayList<TextRun>(runs.size)
        for (run in runs) {
            if (run is TextRun.Plain && run.ext == null && run.text.isEmpty()) continue
            val last = out.lastOrNull()
            if (run is TextRun.Plain && run.ext == null && last is TextRun.Plain && last.ext == null) {
                out[out.size - 1] = TextRun.Plain(last.text + run.text)
            } else {
                out += run
            }
        }
        return out
    }

    /** Applies [f] to every link run; returns the same list instance when nothing changed. */
    fun mapLinks(runs: List<TextRun>, f: (TextRun.Link) -> TextRun.Link): List<TextRun> {
        var changed = false
        val out = runs.map { run ->
            if (run is TextRun.Link) {
                val next = f(run)
                if (next != run) changed = true
                next
            } else {
                run
            }
        }
        return if (changed) out else runs
    }
}
