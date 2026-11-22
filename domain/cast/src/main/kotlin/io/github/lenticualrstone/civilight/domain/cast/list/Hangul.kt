package io.github.lenticualrstone.civilight.domain.cast.list

import io.github.lenticualrstone.civilight.model.nfc

/** Korean initial-consonant (초성) matching for search (CST-02: `ㄹㅇ` matches 리엔). */
object Hangul {
    private const val SYLLABLE_FIRST = 0xAC00
    private const val SYLLABLE_LAST = 0xD7A3
    private const val PER_INITIAL = 21 * 28

    /** Compatibility jamo of the 19 initials, in Unicode initial order. */
    private const val INITIALS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

    fun isSyllable(c: Char): Boolean = c.code in SYLLABLE_FIRST..SYLLABLE_LAST

    fun isInitialJamo(c: Char): Boolean = INITIALS.indexOf(c) >= 0

    /** The initial of a syllable as compatibility jamo, or null. */
    fun initialOf(c: Char): Char? = if (isSyllable(c)) INITIALS[(c.code - SYLLABLE_FIRST) / PER_INITIAL] else null

    /** `리엔` → `ㄹㅇ`; other characters are kept. */
    fun initials(text: String): String = text.nfc().map { initialOf(it) ?: it }.joinToString("")

    private fun charMatches(q: Char, t: Char): Boolean =
        q == t || q.lowercaseChar() == t.lowercaseChar() || (isInitialJamo(q) && initialOf(t) == q)

    /** Index of the first match of [query] in [text] (a query jamo matches any syllable with that initial), or −1. */
    fun indexOf(text: String, query: String): Int {
        val t = text.nfc()
        val q = query.nfc().trim()
        if (q.isEmpty()) return 0
        if (q.length > t.length) return -1
        outer@ for (i in 0..t.length - q.length) {
            for (j in q.indices) if (!charMatches(q[j], t[i + j])) continue@outer
            return i
        }
        return -1
    }

    fun matches(text: String, query: String): Boolean = indexOf(text, query) >= 0
}
