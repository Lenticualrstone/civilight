package io.github.lenticualrstone.civilight.ui.search

import io.github.lenticualrstone.civilight.model.nfc

/**
 * Name search used by every picker (SCR-19 `초성 검색`, SCR-55, CST-02): a query matches when it
 * appears in the text, where each query letter that is a lone initial consonant (`ㅎ`) also matches
 * any syllable starting with it (`하`, `한` …). Case-insensitive; text and query are NFC.
 */
object KoreanSearch {
    private const val SYLLABLE_FIRST = 0xAC00
    private const val SYLLABLE_LAST = 0xD7A3
    private const val SYLLABLES_PER_INITIAL = 21 * 28

    /** The 19 initial consonants in syllable order (compatibility jamo). */
    private const val INITIALS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

    /** The initial consonant of a Hangul syllable, or null. */
    fun initialOf(c: Char): Char? {
        val code = c.code
        if (code < SYLLABLE_FIRST || code > SYLLABLE_LAST) return null
        return INITIALS[(code - SYLLABLE_FIRST) / SYLLABLES_PER_INITIAL]
    }

    fun isInitial(c: Char): Boolean = c in INITIALS

    /** `하르트` → `ㅎㄹㅌ`; other characters are kept. */
    fun initials(text: String): String = buildString { for (c in text.nfc()) append(initialOf(c) ?: c) }

    /** Where [query] matches in [text] (indices of [text] after NFC), or null. A blank query matches nothing. */
    fun find(text: String, query: String): IntRange? {
        val q = normalize(query)
        if (q.isEmpty()) return null
        val t = text.nfc()
        val lower = t.lowercase()
        if (lower.length != t.length) return lower.indexOf(q).takeIf { it >= 0 }?.let { it until it + q.length }
        for (start in 0..lower.length - q.length) {
            var ok = true
            for (j in q.indices) {
                if (!charMatches(q[j], lower[start + j])) {
                    ok = false
                    break
                }
            }
            if (ok) return start until start + q.length
        }
        return null
    }

    fun matches(text: String, query: String): Boolean = find(text, query) != null

    /** True when any of [texts] matches. */
    fun matchesAny(texts: Iterable<String?>, query: String): Boolean = texts.any { it != null && matches(it, query) }

    private fun normalize(query: String): String = query.nfc().trim().lowercase()

    private fun charMatches(q: Char, t: Char): Boolean = q == t || (isInitial(q) && initialOf(t) == q)
}
