package io.github.lenticualrstone.civilight.feature.script.editor

import io.github.lenticualrstone.civilight.model.text.TextRun

/** How a stretch of the displayed text is decorated. */
enum class SpanKind {
    /** `괄호 지문`, drawn `(text)` in the paren role (SCR-07). */
    PAREN,

    /** A live `@` link: `ink` text with an `ink3` underline (SCR-55). */
    LINK,

    /** A link to a locked hidden item, shown as its mask (rule 19). */
    HIDDEN_LINK,

    /** A link to a deleted item: dotted underline (rule 20). */
    DELETED_LINK,
}

data class DisplaySpan(val start: Int, val end: Int, val kind: SpanKind)

/** How a link run is drawn: its kind and, for a locked hidden target, the mask shown instead of its text. */
data class LinkLook(val kind: SpanKind, val maskText: String? = null)

/**
 * The text a block shows for its runs, and the maps between raw caret offsets (the domain's
 * `Runs` units: a paren run without its parentheses, a link's text, one character per break) and
 * display indices.
 *
 * - Paren runs are drawn `(text)` when [showParens]; the parentheses are display-only, so the raw
 *   text never holds them. A caret at the start of a paren run sits before `(`; at its end it sits
 *   after `)`, except at [openParenEndAt] (paren mode is on there), where it sits before `)`.
 * - [pendingParenAt] draws an empty `()` at that raw offset with the caret between them (the
 *   toolbar `( )` before anything is typed).
 * - A hidden link shows its mask; every raw offset inside it maps to the mask's end.
 */
class RunLayout private constructor(
    val text: String,
    val spans: List<DisplaySpan>,
    private val rawToDisplay: IntArray,
    private val displayToRaw: IntArray,
) {
    val rawLength: Int get() = rawToDisplay.size - 1

    /** Display index of the caret at raw offset [raw] (clamped). */
    fun toDisplay(raw: Int): Int = rawToDisplay[raw.coerceIn(0, rawLength)]

    /** Raw offset of the caret at display index [display] (clamped). */
    fun toRaw(display: Int): Int = displayToRaw[display.coerceIn(0, displayToRaw.size - 1)]

    companion object {
        const val OPEN = '('
        const val CLOSE = ')'

        fun of(
            runs: List<TextRun>,
            showParens: Boolean = true,
            pendingParenAt: Int? = null,
            openParenEndAt: Int? = null,
            linkLook: (TextRun.Link) -> LinkLook = { LinkLook(SpanKind.LINK) },
        ): RunLayout = Builder(runs.sumOf { it.shownText.length }, pendingParenAt).build(runs, showParens, openParenEndAt, linkLook)
    }

    private class Builder(val rawLength: Int, pendingParenAt: Int?) {
        val toDisplay = IntArray(rawLength + 1) { -1 }
        val toRaw = ArrayList<Int>(rawLength + 8)
        val sb = StringBuilder()
        val spans = mutableListOf<DisplaySpan>()
        var raw = 0
        val pendingAt: Int? = pendingParenAt?.takeIf { it in 0..rawLength }
        var pendingDone = pendingAt == null

        fun mark(at: Int) {
            if (toDisplay[at] < 0) toDisplay[at] = sb.length
        }

        fun decoration(c: Char, rawAt: Int) {
            toRaw += rawAt
            sb.append(c)
        }

        fun placePending() {
            if (!pendingDone && raw == pendingAt) {
                decoration(OPEN, raw)
                toDisplay[raw] = sb.length
                decoration(CLOSE, raw)
                pendingDone = true
            }
        }

        /** A raw character; [keepMark] leaves the caret mark of this offset where it is (start of a paren). */
        fun rawChar(c: Char, keepMark: Boolean = false) {
            if (!keepMark) {
                placePending()
                mark(raw)
            }
            toRaw += raw
            sb.append(c)
            raw++
        }

        fun build(
            runs: List<TextRun>,
            showParens: Boolean,
            openParenEndAt: Int?,
            linkLook: (TextRun.Link) -> LinkLook,
        ): RunLayout {
            for (run in runs) {
                when (run) {
                    is TextRun.Paren -> paren(run, showParens, openParenEndAt)
                    is TextRun.Link -> link(run, linkLook(run))
                    else -> run.shownText.forEach { rawChar(it) }
                }
            }
            placePending()
            for (i in 0..rawLength) if (toDisplay[i] < 0) toDisplay[i] = sb.length
            toRaw += rawLength
            return RunLayout(sb.toString(), spans, toDisplay, toRaw.toIntArray())
        }

        private fun paren(run: TextRun.Paren, showParens: Boolean, openParenEndAt: Int?) {
            placePending()
            if (!showParens) {
                repeat(run.text.length) {
                    mark(raw)
                    raw++
                }
                return
            }
            val start = sb.length
            mark(raw)
            decoration(OPEN, raw)
            run.text.forEachIndexed { i, c -> rawChar(c, keepMark = i == 0) }
            if (openParenEndAt == raw) mark(raw)
            decoration(CLOSE, raw)
            spans += DisplaySpan(start, sb.length, SpanKind.PAREN)
        }

        private fun link(run: TextRun.Link, look: LinkLook) {
            val start = sb.length
            val mask = look.maskText
            if (mask == null) {
                run.text.forEach { rawChar(it) }
            } else {
                placePending()
                val rawStart = raw
                mark(rawStart)
                mask.forEach { decoration(it, rawStart) }
                repeat(run.text.length) {
                    raw++
                    if (raw < rawStart + run.text.length) mark(raw)
                }
            }
            if (sb.length > start) spans += DisplaySpan(start, sb.length, look.kind)
        }
    }
}
