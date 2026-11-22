package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.domain.world.common.Copy

/** A label with an optional sub line (the range of an approximate value). */
data class TimeLabel(val text: String, val sub: String? = null)

/** Time labels of the three timeline views (WLD-34). */
object TimeLabels {
    /** `약 m` with m = round((a + b) / 2) (rule 11 §6: 5~9 → 7, 11~15 → 13). */
    fun midpoint(a: Int, b: Int): Int = Math.floorDiv(a + b + 1, 2)

    /** `이야기 시작부터`: `3일째`, `약 13일째 (11~15)`. */
    fun storyDay(dayMin: Int, dayMax: Int): TimeLabel =
        if (dayMin == dayMax) TimeLabel("${dayMin}일째")
        else TimeLabel("약 ${midpoint(dayMin, dayMax)}일째 ($dayMin~$dayMax)")

    private fun signed(n: Int): String = when {
        n > 0 -> "+$n"
        n < 0 -> "${Copy.MINUS}${-n}"
        else -> "0"
    }

    /** `기준 사건` offsets in days: `−1일`, `+약 7일` with sub `5~9일`. */
    fun dayOffset(min: Int, max: Int): TimeLabel {
        if (min == max) return TimeLabel("${signed(min)}일")
        val mid = midpoint(min, max)
        val sign = when {
            mid > 0 -> "+"
            mid < 0 -> Copy.MINUS
            else -> ""
        }
        return TimeLabel("${sign}약 ${kotlin.math.abs(mid)}일", "${rangeNumber(min)}~${rangeNumber(max)}일")
    }

    private fun rangeNumber(n: Int): String = if (n < 0) "${Copy.MINUS}${-n}" else n.toString()

    /** `기준 사건` offsets in years: `−5년`. */
    fun yearOffset(years: Int): TimeLabel = TimeLabel("${signed(years)}년")

    /** The anchor row itself. */
    val ANCHOR = TimeLabel("기준")
}
