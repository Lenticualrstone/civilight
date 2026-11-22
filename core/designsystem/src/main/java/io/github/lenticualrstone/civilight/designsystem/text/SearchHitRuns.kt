package io.github.lenticualrstone.civilight.designsystem.text

/** A run of a search result sentence; [isMatch] runs are drawn as inline boxes. */
data class HitRun(val text: String, val isMatch: Boolean)

/**
 * DS-45 / T-DS-20: splits [sentence] into plain and boxed runs. [matches] are UTF-16 ranges of the
 * displayed sentence (`1 until 4`); they are clamped, sorted and overlapping ones merged. The runs
 * concatenate to exactly [sentence]; nothing is inserted, so the sentence wraps as normal text.
 */
fun searchHitRuns(sentence: String, matches: List<IntRange>): List<HitRun> {
    val ranges = normalizeMatches(sentence.length, matches)
    val runs = mutableListOf<HitRun>()
    var cursor = 0
    for (r in ranges) {
        if (r.first > cursor) runs += HitRun(sentence.substring(cursor, r.first), isMatch = false)
        runs += HitRun(sentence.substring(r.first, r.last + 1), isMatch = true)
        cursor = r.last + 1
    }
    if (cursor < sentence.length) runs += HitRun(sentence.substring(cursor), isMatch = false)
    return runs
}

/** Clamps to the text, drops empty ranges, sorts and merges overlaps. */
fun normalizeMatches(length: Int, matches: List<IntRange>): List<IntRange> {
    val clamped = matches.mapNotNull { r ->
        val s = r.first.coerceIn(0, length)
        val e = (r.last + 1).coerceIn(0, length)
        if (e > s) s until e else null
    }.sortedBy { it.first }
    val merged = mutableListOf<IntRange>()
    for (r in clamped) {
        val last = merged.lastOrNull()
        if (last != null && r.first <= last.last) {
            merged[merged.lastIndex] = last.first..maxOf(last.last, r.last)
        } else {
            merged += r
        }
    }
    return merged
}

/** The search-hit decorations for [matches] (input of [DecoratedText]). */
fun searchHitDecorations(sentence: String, matches: List<IntRange>): List<DecorationRange> =
    normalizeMatches(sentence.length, matches).map { DecorationRange(it.first, it.last + 1, InlineDecoration.SEARCH_HIT) }
