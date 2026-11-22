package io.github.lenticualrstone.civilight.feature.script.editor

/**
 * What one change of the block's text field means (SCR-15). The field holds [SENTINEL] followed by
 * the raw text, so Backspace at the start deletes the sentinel instead of doing nothing (soft
 * keyboards send no key event there). Enter arrives as an inserted line break.
 */
sealed interface FieldChange {
    /** Raw [start, end) was replaced with [inserted]. */
    data class Text(val start: Int, val end: Int, val inserted: String) : FieldChange

    /** Enter at raw offset [at]: a new block or a split (SCR-15). */
    data class Enter(val at: Int) : FieldChange

    /** Backspace with the caret at the start of the block: merge, remove or nothing (SCR-15). */
    data object BackspaceAtStart : FieldChange

    /** `(` typed outside paren mode: paren mode starts at [at], nothing is inserted (SCR-07). */
    data class OpenParen(val at: Int) : FieldChange

    /** `)` typed in paren mode: paren mode ends, nothing is inserted. */
    data class CloseParen(val at: Int) : FieldChange

    /** Only the caret or selection moved. */
    data object None : FieldChange
}

object FieldDiff {
    /** Zero-width space in front of the raw text. */
    const val SENTINEL = '​'

    /** The field text for [raw]. */
    fun fieldText(raw: String): String = SENTINEL + raw

    /** Raw offset of a field offset (the sentinel is offset 0). */
    fun rawOffset(fieldOffset: Int): Int = (fieldOffset - 1).coerceAtLeast(0)

    /** Field offset of a raw offset. */
    fun fieldOffset(rawOffset: Int): Int = rawOffset + 1

    /**
     * Classifies a new field value. [oldRaw] is the raw text before; [newField] the field text after;
     * [newCaretField] the caret (selection end) after, in field offsets. The diff is anchored on the
     * caret, so repeated letters (`아아` → `아아아`) are placed where they were typed. Without
     * [parens] (blocks that hold no `괄호 지문`, SCR-07) `(` and `)` are plain text.
     */
    fun classify(oldRaw: String, newField: String, newCaretField: Int, parenMode: Boolean, parens: Boolean = true): FieldChange {
        val sentinelKept = newField.startsWith(SENTINEL)
        val newRaw = newField.replace(SENTINEL.toString(), "")
        if (!sentinelKept && newRaw == oldRaw) return FieldChange.BackspaceAtStart
        if (newRaw == oldRaw) return FieldChange.None
        val caret = (if (sentinelKept) rawOffset(newCaretField) else newCaretField).coerceIn(0, newRaw.length)
        val span = span(oldRaw, newRaw, caret)
        val start = span.start
        val end = span.end
        val inserted = span.inserted
        if (start == end) {
            when (inserted) {
                "\n" -> return FieldChange.Enter(start)
                "(" -> if (parens && !parenMode) return FieldChange.OpenParen(start)
                ")" -> if (parens && parenMode) return FieldChange.CloseParen(start)
            }
        }
        return FieldChange.Text(start, end, inserted)
    }

    /** Raw [start, end) of the old text that [inserted] replaced. */
    data class Span(val start: Int, val end: Int, val inserted: String)

    /** The one replaced range between two raw texts, anchored on [caret] (a raw offset of [newRaw]). */
    fun span(oldRaw: String, newRaw: String, caret: Int): Span {
        val c = caret.coerceIn(0, newRaw.length)
        var prefix = 0
        val maxPrefix = minOf(oldRaw.length, newRaw.length, c)
        while (prefix < maxPrefix && oldRaw[prefix] == newRaw[prefix]) prefix++
        var suffix = 0
        val maxSuffix = minOf(oldRaw.length - prefix, newRaw.length - maxOf(prefix, c))
        while (suffix < maxSuffix && oldRaw[oldRaw.length - 1 - suffix] == newRaw[newRaw.length - 1 - suffix]) suffix++
        return Span(prefix, oldRaw.length - suffix, newRaw.substring(prefix, newRaw.length - suffix))
    }
}
