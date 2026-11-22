package io.github.lenticualrstone.civilight.feature.story.where

import io.github.lenticualrstone.civilight.model.ClockTime

/** Text fields of the 때 part: what a draft means once it is complete. */
object WhereInputs {
    /** What a clock draft asks for: nothing yet (still typing), a clock, or clearing it. */
    sealed interface ClockDraft {
        data object Incomplete : ClockDraft

        data object Clear : ClockDraft

        data class Value(val clock: ClockTime) : ClockDraft
    }

    /**
     * `05:40` or `0540` is a clock (two-digit hours, so a half-deleted `0:00` is not one); an empty field
     * clears it; anything else waits for more input.
     */
    fun clock(text: String): ClockDraft {
        val t = text.trim()
        if (t.isEmpty()) return ClockDraft.Clear
        val digits = when {
            Regex("""^\d{2}:\d{2}$""").matches(t) -> t.replace(":", "")
            Regex("""^\d{4}$""").matches(t) -> t
            else -> return ClockDraft.Incomplete
        }
        val h = digits.substring(0, 2).toInt()
        val m = digits.substring(2, 4).toInt()
        if (h > 23 || m > 59) return ClockDraft.Incomplete
        return ClockDraft.Value(ClockTime.of(h, m))
    }

    /** The `직접` story day (1 or more), or null while the field is not a day number. */
    fun storyDay(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..MAX_DAY }

    private const val MAX_DAY = 100_000
}
