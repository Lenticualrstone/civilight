package io.github.lenticualrstone.civilight.domain.story.text

import io.github.lenticualrstone.civilight.model.nfc

/** The rule 21 particle list (WLD-21 §5): a name may be followed by one or two of these. */
object MatchParticles {
    val BUILT_IN: List<String> = listOf(
        "은", "는", "이", "가", "을", "를", "의", "에", "에서", "에게", "한테", "로", "으로", "와", "과", "도", "만",
        "까지", "부터", "처럼", "보다", "이나", "나", "이랑", "랑", "이야", "야", "이다", "이었", "였", "이라", "라",
    )
}

/** A matched span of [text] (NFC) at [range] for [value]. */
data class WordHit<T>(val range: IntRange, val matched: String, val value: T)

/**
 * Word-start matching like the rule 21 matcher (STO-34 reading rules): a pattern matches at a word
 * start and must be followed by the end, whitespace, punctuation or one or two particles. Patterns
 * shorter than [minSyllables] are skipped; on overlap the longer match wins.
 */
class WordMatcher<T>(
    patterns: List<Pair<String, T>>,
    extraParticles: List<String> = emptyList(),
    private val minSyllables: Int = 2,
) {
    private val patterns: List<Pair<String, T>> = patterns
        .map { (p, v) -> p.trim().nfc() to v }
        .filter { (p, _) -> p.isNotEmpty() && p.count { !it.isWhitespace() } >= minSyllables }
        .sortedByDescending { it.first.length }
    private val particles: List<String> = (MatchParticles.BUILT_IN + extraParticles).distinct().sortedByDescending { it.length }

    fun find(input: String): List<WordHit<T>> {
        val text = input.nfc()
        val hits = mutableListOf<WordHit<T>>()
        for ((pattern, value) in patterns) {
            var from = 0
            while (true) {
                val i = text.indexOf(pattern, from)
                if (i < 0) break
                from = i + 1
                if (i > 0 && text[i - 1].isLetterOrDigit()) continue
                if (!endsWell(text, i + pattern.length)) continue
                val range = i until i + pattern.length
                if (hits.any { it.range.first <= range.last && range.first <= it.range.last }) continue
                hits += WordHit(range, pattern, value)
            }
        }
        return hits.sortedBy { it.range.first }
    }

    private fun endsWell(text: String, at: Int, depth: Int = 0): Boolean {
        if (at >= text.length || !text[at].isLetterOrDigit()) return true
        if (depth >= 2) return false
        return particles.any { p -> text.startsWith(p, at) && endsWell(text, at + p.length, depth + 1) }
    }
}
