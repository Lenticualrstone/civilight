package io.github.lenticualrstone.civilight.domain.world.term

import java.text.Normalizer

/** Character classes and counting used by the candidate matcher (WLD-21). */
object TermText {
    /** Cut-off marks after a prefix (`끊김`): `…` U+2026, `⋯` U+22EF, `—` U+2014, `-` U+002D. */
    val CUT_OFF_MARKS: Set<Char> = setOf('…', '⋯', '—', '-')

    private fun isMark(c: Char): Boolean = when (Character.getType(c).toByte()) {
        Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK -> true
        else -> false
    }

    /**
     * A word boundary character: whitespace, punctuation, quotes, brackets and symbols, i.e. anything
     * that is neither a letter, a digit nor a combining mark.
     */
    fun isBoundary(c: Char): Boolean = !Character.isLetterOrDigit(c) && !isMark(c)

    /** Latin letters compare case-insensitively; other scripts are unchanged. Keeps the length. */
    fun fold(c: Char): Char = Character.toLowerCase(c)

    fun fold(s: String): String = buildString(s.length) { for (c in s) append(fold(c)) }

    /**
     * Syllable count (Q-04 g): every Hangul syllable, letter, digit or CJK character counts 1;
     * spaces and punctuation count 0.
     */
    fun syllables(s: String): Int = Normalizer.normalize(s, Normalizer.Form.NFC).count { Character.isLetterOrDigit(it) }

    /** NFC, trimmed, inner whitespace runs collapsed to one space: the stored form of names and aliases. */
    fun clean(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ")
}

/**
 * NFC text with a map back to offsets of the original (possibly decomposed) text, so candidates
 * found on NFC text report spans of the text as stored (WLD-T25).
 */
internal class NfcText private constructor(
    val text: String,
    private val originalStart: IntArray?,
    private val originalLength: Int,
) {
    /** Offset in the original text of NFC offset [index] (`index == text.length` maps to the end). */
    fun toOriginal(index: Int): Int = when {
        originalStart == null -> index
        index >= text.length -> originalLength
        else -> originalStart[index]
    }

    companion object {
        fun of(original: String): NfcText {
            if (Normalizer.isNormalized(original, Normalizer.Form.NFC)) return NfcText(original, null, original.length)
            val nfc = Normalizer.normalize(original, Normalizer.Form.NFC)
            val starts = IntArray(nfc.length)
            var next = 0
            for (k in 1..original.length) {
                val len = Normalizer.normalize(original.substring(0, k), Normalizer.Form.NFC).length
                while (next < len && next < nfc.length) {
                    starts[next] = k - 1
                    next++
                }
            }
            return NfcText(nfc, starts, original.length)
        }
    }
}
