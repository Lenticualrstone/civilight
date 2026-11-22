package io.github.lenticualrstone.civilight.feature.recorder.memo

import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * The memo field edits plain text; the memo itself is runs (`@` links are atomic runs). Each field
 * change becomes one `Runs.replace` over the changed range, so typing (including Hangul
 * composition) never rebuilds the field from the runs.
 */
object MemoEditing {
    /** Shown range [start, end) of the old text replaced by [inserted]. */
    data class Change(val start: Int, val end: Int, val inserted: String)

    /**
     * The one range that turns [old] into [new]: common prefix and suffix, with the suffix never
     * reaching left of [caret] in the new text, so a repeated character is placed where it was typed.
     */
    fun diff(old: String, new: String, caret: Int): Change? {
        if (old == new) return null
        val afterCaret = (new.length - caret.coerceIn(0, new.length))
        val maxSuffix = minOf(old.length, new.length, afterCaret)
        var s = 0
        while (s < maxSuffix && old[old.length - 1 - s] == new[new.length - 1 - s]) s++
        val maxPrefix = minOf(old.length - s, new.length - s)
        var p = 0
        while (p < maxPrefix && old[p] == new[p]) p++
        return Change(p, old.length - s, new.substring(p, new.length - s))
    }

    /** [runs] after the field went from their text to [new] with the caret at [caret]. */
    fun apply(runs: List<TextRun>, new: String, caret: Int): List<TextRun> {
        val change = diff(Runs.text(runs), new, caret) ?: return runs
        return Runs.replace(runs, change.start, change.end, Runs.plain(change.inserted))
    }
}

/**
 * The `@` drawn before every link run of the memo (`@세라`, board `LrText`). The `@` is not stored
 * (rule 23 §5: the run holds only the name), so the field shows it through an offset mapping:
 * [starts] are the link runs' start offsets in the stored text, ascending; [ends] their ends.
 */
class AtMarks(
    private val starts: IntArray,
    private val ends: IntArray,
    private val dotted: BooleanArray = BooleanArray(starts.size),
    private val diamond: BooleanArray = BooleanArray(starts.size),
) {
    val count: Int get() = starts.size

    /** Link run [index] points at a target in the trash (LRC-23): a dotted `ink3` line, as its chip. */
    fun isDotted(index: Int): Boolean = dotted.getOrElse(index) { false }

    /** Link run [index] points at a deleted card (인물 · 장소 · 용어 · 세력): `◇` instead of `@`, as its chip. */
    fun hasDiamond(index: Int): Boolean = diamond.getOrElse(index) { false }

    /** True when these marks were made for a text of [length] (the field may be one keystroke ahead). */
    fun fits(length: Int): Boolean = ends.all { it <= length }

    override fun equals(other: Any?): Boolean =
        other is AtMarks && starts.contentEquals(other.starts) && ends.contentEquals(other.ends) &&
            dotted.contentEquals(other.dotted) && diamond.contentEquals(other.diamond)

    override fun hashCode(): Int =
        31 * (31 * (31 * starts.contentHashCode() + ends.contentHashCode()) + dotted.contentHashCode()) + diamond.contentHashCode()

    /** The text as shown: `@` inserted before every link run (`◇` before a run to a deleted card). */
    fun show(text: String): String {
        if (starts.isEmpty()) return text
        val out = StringBuilder(text.length + starts.size)
        var from = 0
        for ((i, s) in starts.withIndex()) {
            out.append(text, from, s).append(if (hasDiamond(i)) DIAMOND else '@')
            from = s
        }
        return out.append(text, from, text.length).toString()
    }

    /** A caret at a link's start stays before its `@`. */
    fun originalToShown(offset: Int): Int = offset + starts.count { it < offset }

    /** Both sides of an `@` map to the link's start. */
    fun shownToOriginal(offset: Int): Int {
        var marksBefore = 0
        for ((i, s) in starts.withIndex()) if (s + i < offset) marksBefore++
        return offset - marksBefore
    }

    /** Shown ranges of the links including their `@` (for the 2 dp underline). */
    fun shownRanges(): List<IntRange> = starts.indices.map { i -> (starts[i] + i) until (ends[i] + i + 1) }

    companion object {
        val NONE = AtMarks(IntArray(0), IntArray(0))
        private const val DIAMOND = '◇'

        /**
         * Marks of [runs]; [dotted] says which link runs point at a target in the trash, [diamond]
         * which at a deleted card (LRC-23, the same flags as their chips).
         */
        fun of(
            runs: List<TextRun>,
            dotted: (TextRun.Link) -> Boolean = { false },
            diamond: (TextRun.Link) -> Boolean = { false },
        ): AtMarks {
            val links = Runs.linkRanges(runs)
            return AtMarks(
                links.map { it.second.first }.toIntArray(),
                links.map { it.second.last + 1 }.toIntArray(),
                links.map { dotted(it.first) }.toBooleanArray(),
                links.map { diamond(it.first) }.toBooleanArray(),
            )
        }
    }
}
