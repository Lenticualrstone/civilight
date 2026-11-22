package io.github.lenticualrstone.civilight.domain.recorder.text

import io.github.lenticualrstone.civilight.model.nfc

/** Name matching for the link picker: name, alias and 초성 (initial consonants). */
object Hangul {
    private const val SYLLABLE_FIRST = 0xAC00
    private const val SYLLABLE_LAST = 0xD7A3
    private const val PER_INITIAL = 21 * 28
    private val INITIALS = charArrayOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
    )

    /** The 초성 of each syllable; other characters stay as they are. */
    fun initials(text: String): String = buildString {
        for (c in text.nfc()) {
            val code = c.code
            if (code in SYLLABLE_FIRST..SYLLABLE_LAST) append(INITIALS[(code - SYLLABLE_FIRST) / PER_INITIAL]) else append(c)
        }
    }

    /** True when [query] is made of compatibility initial consonants only (`ㅅㄹ`). */
    fun isInitialsQuery(query: String): Boolean = query.isNotEmpty() && query.all { it in INITIALS || it == ' ' } && query.any { it in INITIALS }

    /** Lower-case NFC without surrounding spaces: the form names are compared in. */
    fun fold(text: String): String = text.nfc().trim().lowercase()
}
