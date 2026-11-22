package io.github.lenticualrstone.civilight.domain.story.time

import kotlin.math.roundToInt

/**
 * Duration texts (STO-29; R18 §3 examples, general rules `ED`). WLD's `formatDuration` must produce
 * exactly these strings, so world code should call this object.
 */
object Durations {
    /**
     * A required travel time: ≥ 24 h → `D일 H시간` (H rounded to the hour, `D일` when 0, no `약`);
     * < 24 h → `약 H시간 M분` with M rounded to 10 minutes.
     */
    fun required(minutes: Int): String {
        if (minutes >= DAY_MINUTES) {
            var days = minutes / DAY_MINUTES
            var hours = ((minutes - days * DAY_MINUTES) / 60.0).roundToInt()
            if (hours == 24) {
                days += 1
                hours = 0
            }
            return if (hours == 0) "${days}일" else "${days}일 ${hours}시간"
        }
        return approx(minutes)
    }

    /** `약 H시간 M분`, minutes rounded to the nearest 10 (`약 7시간 40분`, `약 9시간`, `약 40분`). */
    fun approx(minutes: Int): String {
        val rounded = (minutes / 10.0).roundToInt() * 10
        val h = rounded / 60
        val m = rounded % 60
        return when {
            h == 0 -> "약 ${m}분"
            m == 0 -> "약 ${h}시간"
            else -> "약 ${h}시간 ${m}분"
        }
    }

    /** An exact gap: `9시간 40분`, `12시간`, `1일 2시간 30분`, `0분`. */
    fun gap(minutes: Int): String {
        if (minutes < 0) return "−" + gap(-minutes)
        val d = minutes / DAY_MINUTES
        val rest = minutes % DAY_MINUTES
        val parts = buildList {
            if (d > 0) add("${d}일")
            if (rest / 60 > 0) add("${rest / 60}시간")
            if (rest % 60 > 0) add("${rest % 60}분")
        }
        return if (parts.isEmpty()) "0분" else parts.joinToString(" ")
    }

    /** `9시간 40분 – 12시간 40분`; a single value when the range is a point. */
    fun gapRange(span: Span): String = if (span.isPoint) gap(span.min) else "${gap(span.min)} – ${gap(span.max)}"

    /** A day count with sign for previews: `+2일`, `−1일`, `+약 2일`. */
    fun signedDays(days: Span): String {
        val n = if (days.isPoint) days.min else days.midpoint
        val sign = if (n < 0) "−" else "+"
        val approx = if (days.isPoint) "" else "약 "
        return "$sign$approx${kotlin.math.abs(n)}일"
    }
}
