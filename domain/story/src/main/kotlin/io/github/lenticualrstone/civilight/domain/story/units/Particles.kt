package io.github.lenticualrstone.civilight.domain.story.units

/** Korean particle choice after a number read in Sino-Korean (`씬 3과`, `씬 2와`). */
object Particles {
    /** True when the number's reading ends in a consonant (일 삼 육 칠 팔 십 백 천 만 …). */
    fun endsWithConsonant(n: Int): Boolean {
        val v = kotlin.math.abs(n)
        if (v == 0) return true // 영
        val last = v % 10
        return if (last == 0) true else last !in setOf(2, 4, 5, 9)
    }

    /** `3과` · `2와`. */
    fun gwa(n: Int): String = "$n${if (endsWithConsonant(n)) "과" else "와"}"
}
