package io.github.lenticualrstone.civilight.ui.richtext

import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** How a link run is drawn in the editor (rule 20 dotted for deleted targets, R19 hidden). */
enum class LinkLook { LIVE, HIDDEN, DELETED }

/** What a stretch of the shown text is, for styling. */
enum class SpanKind { LINK, LINK_HIDDEN, LINK_DELETED, PAREN, MEMO, HEADING }

/** A styled stretch of the shown (transformed) text. */
data class ShownSpan(val start: Int, val end: Int, val kind: SpanKind)

/**
 * Text drawn around the editor text without being part of it: `(` `)` around a `괄호 지문` and the
 * `⌁ ` mark at the start of a memo paragraph. [caretAfter] = a caret at [at] sits after the inserted
 * text (memo mark) instead of before it (parentheses).
 */
data class Insertion(val at: Int, val text: String, val caretAfter: Boolean, val rank: Int)

/**
 * The editor's display: the editor text with [Insertion]s, the spans to style, and the caret mapping
 * both ways (Compose `OffsetMapping` semantics, every value clamped to the texts' bounds).
 */
class ShownText(val original: String, insertions: List<Insertion>, originalSpans: List<ShownSpan>) {
    private val inserts = insertions.sortedWith(compareBy<Insertion>({ it.at }, { it.rank }))

    /** The transformed text. */
    val text: String = buildString {
        var pos = 0
        for (ins in inserts) {
            append(original, pos, ins.at)
            append(ins.text)
            pos = ins.at
        }
        append(original, pos, original.length)
    }

    /** Shown offset of every original caret offset 0..n (non-decreasing). */
    private val toShown: IntArray = IntArray(original.length + 1).also { arr ->
        var added = 0
        var i = 0
        for (o in 0..original.length) {
            // Insertions before o (at < o) are all added; at o only the leading caret-after ones.
            while (i < inserts.size && inserts[i].at < o) added += inserts[i++].text.length
            var lead = 0
            var j = i
            while (j < inserts.size && inserts[j].at == o && inserts[j].caretAfter) lead += inserts[j++].text.length
            arr[o] = o + added + lead
        }
    }

    /** Shown ranges of the inserted texts, for mapping a tap inside one back to its place. */
    private val insertRanges: List<Triple<Int, Int, Int>> = buildList {
        var pos = 0
        var shown = 0
        for (ins in inserts) {
            shown += ins.at - pos
            pos = ins.at
            add(Triple(shown, shown + ins.text.length, ins.at))
            shown += ins.text.length
        }
    }

    fun originalToShown(offset: Int): Int = toShown[offset.coerceIn(0, original.length)].coerceIn(0, text.length)

    fun shownToOriginal(offset: Int): Int {
        val t = offset.coerceIn(0, text.length)
        for ((from, to, at) in insertRanges) if (t > from && t < to) return at
        // Largest o with toShown[o] <= t.
        var lo = 0
        var hi = original.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (toShown[mid] <= t) lo = mid else hi = mid - 1
        }
        return lo
    }

    /** [originalSpans] in shown offsets, plus the inserted marks. */
    val spans: List<ShownSpan> = buildList {
        for (s in originalSpans) {
            val start = originalToShownStart(s.start)
            val end = originalToShownEnd(s.end)
            if (end > start) add(ShownSpan(start, end, s.kind))
        }
        var pos = 0
        var shown = 0
        for (ins in inserts) {
            shown += ins.at - pos
            pos = ins.at
            add(ShownSpan(shown, shown + ins.text.length, if (ins.text.trim() == MEMO_MARK.trim()) SpanKind.MEMO else SpanKind.PAREN))
            shown += ins.text.length
        }
    }

    /** Where text from original [offset] starts (after every insertion at it). */
    private fun originalToShownStart(offset: Int): Int = offset + inserts.filter { it.at <= offset }.sumOf { it.text.length }

    /** Where text up to original [offset] ends (before every insertion at it). */
    private fun originalToShownEnd(offset: Int): Int = offset + inserts.filter { it.at < offset }.sumOf { it.text.length }

    companion object {
        const val MEMO_MARK = "⌁ "

        /** The display of [layout] (from [RichDocEdit.layout] of the same text). */
        fun of(original: String, layout: TextLayout, linkLook: (TextRun.Link) -> LinkLook = { LinkLook.LIVE }): ShownText {
            val insertions = ArrayList<Insertion>()
            val spans = ArrayList<ShownSpan>()
            for (p in layout.paragraphs) {
                when (p.block.kind.known) {
                    DocBlockKind.MEMO -> {
                        insertions += Insertion(p.start, MEMO_MARK, caretAfter = true, rank = 1)
                        if (p.end > p.start) spans += ShownSpan(p.start, p.end, SpanKind.MEMO)
                    }
                    DocBlockKind.HEADING -> if (p.end > p.start) spans += ShownSpan(p.start, p.end, SpanKind.HEADING)
                    else -> Unit
                }
            }
            for (r in layout.runs) {
                when (val run = r.run) {
                    is TextRun.Paren -> {
                        insertions += Insertion(r.start, "(", caretAfter = false, rank = 2)
                        insertions += Insertion(r.end, ")", caretAfter = false, rank = 0)
                        if (r.end > r.start) spans += ShownSpan(r.start, r.end, SpanKind.PAREN)
                    }
                    is TextRun.Link -> if (r.end > r.start) {
                        spans += ShownSpan(
                            r.start, r.end,
                            when (linkLook(run)) {
                                LinkLook.LIVE -> SpanKind.LINK
                                LinkLook.HIDDEN -> SpanKind.LINK_HIDDEN
                                LinkLook.DELETED -> SpanKind.LINK_DELETED
                            },
                        )
                    }
                    else -> Unit
                }
            }
            return ShownText(original, insertions, spans)
        }
    }
}
