package io.github.lenticualrstone.civilight.domain.story.search

import io.github.lenticualrstone.civilight.model.nfc

/** A text with the matched parts boxed (`mark.hit`), drawn on one line. */
data class Highlight(val text: String, val hits: List<IntRange>) {
    val boxed: List<String> get() = hits.map { text.substring(it) }
}

/**
 * A search query (STO-37): NFC, Latin case-insensitive substring; a query of only Hangul
 * compatibility consonants (`ㄱ … ㅎ`) matches the initial consonants of Hangul syllables (`ED`:
 * names, aliases, titles and block text alike; other characters must match themselves).
 */
class TextQuery(raw: String) {
    val text: String = raw.trim().nfc()
    private val folded: String = fold(text)

    val isEmpty: Boolean get() = text.isEmpty()

    /** True when every non-space character is a compatibility consonant. */
    val isInitials: Boolean = text.isNotEmpty() && text.all { it == ' ' || it in CONSONANTS }

    /** Every non-overlapping hit in [target] (NFC), as index ranges of the NFC text. */
    fun hits(target: String): List<IntRange> {
        if (isEmpty) return emptyList()
        val subject = target.nfc()
        val haystack = if (isInitials) initials(subject) else fold(subject)
        val needle = folded
        val out = mutableListOf<IntRange>()
        var from = 0
        while (true) {
            val i = haystack.indexOf(needle, from)
            if (i < 0) break
            out += i until i + needle.length
            from = i + needle.length
        }
        return out
    }

    fun matches(target: String): Boolean = hits(target).isNotEmpty()

    /** [target] (NFC) with its hits, or null when it does not match. */
    fun highlight(target: String): Highlight? {
        val h = hits(target)
        return if (h.isEmpty()) null else Highlight(target.nfc(), h)
    }

    companion object {
        private const val CONSONANTS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

        /** Initial consonants of the 19 Hangul leading jamo, in Unicode order. */
        private const val LEADS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

        /** Per-character Latin lowercase (length preserving). */
        fun fold(s: String): String = buildString(s.length) { for (c in s) append(c.lowercaseChar()) }

        /** `오리지늄 등불` → `ㅇㄹㅈㄴ ㄷㅂ` (length preserving; other characters folded). */
        fun initials(s: String): String = buildString(s.length) {
            for (c in s) {
                if (c in '가'..'힣') append(LEADS[(c - '가') / 588]) else append(c.lowercaseChar())
            }
        }

        /** Hangul syllables (and other characters) of [s], for similar-name distances. */
        fun syllables(s: String): List<Char> = s.nfc().filter { !it.isWhitespace() }.toList()

        /** Levenshtein distance over characters (syllables for Hangul). */
        fun distance(a: List<Char>, b: List<Char>): Int {
            var prev = IntArray(b.size + 1) { it }
            for (i in 1..a.size) {
                val cur = IntArray(b.size + 1)
                cur[0] = i
                for (j in 1..b.size) {
                    cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
                }
                prev = cur
            }
            return prev[b.size]
        }
    }
}
