package io.github.lenticualrstone.civilight.domain.world.common

/** Korean object particle (`을 · 를`) after a label, as the chips print it (WLD-T42). */
object Josa {
    /** Digits read in Sino-Korean: 0 영 · 1 일 · 3 삼 · 6 육 · 7 칠 · 8 팔 end in a consonant. */
    private val DIGIT_HAS_FINAL = mapOf(
        '0' to true, '1' to true, '2' to false, '3' to true, '4' to false,
        '5' to false, '6' to true, '7' to true, '8' to true, '9' to false,
    )

    /** True when the last letter or digit of [text] ends in a consonant sound. */
    fun hasFinalConsonant(text: String): Boolean {
        val last = text.lastOrNull { it.isLetterOrDigit() } ?: return true
        DIGIT_HAS_FINAL[last]?.let { return it }
        if (last in '가'..'힣') return (last.code - 0xAC00) % 28 != 0
        return true
    }

    /** `을` or `를`. */
    fun objectParticle(text: String): String = if (hasFinalConsonant(text)) "을" else "를"

    /** [text] followed by its object particle. */
    fun withObject(text: String): String = text + objectParticle(text)
}
