package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.world.WorldDate

/** A date in the world calendar; [month] and [day] are 1-based. */
data class CalendarDay(val year: Int, val month: Int, val day: Int)

/**
 * World-time arithmetic (WLD-29, WLD-30). In `연도만` a year has 365 days (Q-13 default) and the
 * story starts on its first day; in `세계 달력` a year is the sum of the month lengths and the
 * story starts on `storyStart` (default the first day). Story day 1 is offset 0.
 */
class WorldClock(val worldTime: WorldTime) {
    val mode: WorldTimeMode get() = worldTime.mode.knownOr(WorldTimeMode.YEAR_ONLY)
    val startYear: Int? get() = worldTime.storyStartYear
    val calendar: WorldCalendar? get() = worldTime.calendar?.takeIf { mode == WorldTimeMode.CALENDAR && it.months.isNotEmpty() }

    /** False in `시간 없이`: the 연표 module is off and events are not dated (WLD-29, T51). */
    val hasTimeline: Boolean get() = mode != WorldTimeMode.NONE

    val yearLength: Int get() = calendar?.months?.sumOf { it.days.coerceAtLeast(1) } ?: YEAR_DAYS

    /** 0-based day of year of the story start. */
    val startDayOfYear: Int
        get() = calendar?.storyStart?.let { dayOfYear(it.month, it.day) } ?: 0

    /** 0-based day of year of a calendar month/day (clamped into the calendar). */
    fun dayOfYear(month: Int, day: Int): Int {
        val months = calendar?.months ?: return 0
        val m = month.coerceIn(1, months.size)
        val before = months.take(m - 1).sumOf { it.days.coerceAtLeast(1) }
        return before + (day.coerceIn(1, months[m - 1].days.coerceAtLeast(1)) - 1)
    }

    /** The calendar date of a 0-based day offset from the story start (calendar mode only). */
    fun dateOfOffset(offset: Int): CalendarDay? {
        val months = calendar?.months ?: return null
        val year0 = startYear ?: 0
        val total = startDayOfYear + offset
        val yearOffset = Math.floorDiv(total, yearLength)
        var doy = Math.floorMod(total, yearLength)
        for ((i, m) in months.withIndex()) {
            val len = m.days.coerceAtLeast(1)
            if (doy < len) return CalendarDay(year0 + yearOffset, i + 1, doy + 1)
            doy -= len
        }
        return null
    }

    /** Day offset from the story start of a calendar date. */
    fun offsetOf(date: CalendarDay): Int {
        val year0 = startYear ?: 0
        return (date.year - year0) * yearLength + dayOfYear(date.month, date.day) - startDayOfYear
    }

    /** Weekday name of a day offset, or null without weekdays. */
    fun weekdayOf(offset: Int): String? {
        val cal = calendar ?: return null
        if (cal.weekdays.isEmpty()) return null
        val first = ((cal.storyStartWeekday ?: 1) - 1).coerceIn(0, cal.weekdays.size - 1)
        return cal.weekdays[Math.floorMod(first + offset, cal.weekdays.size)]
    }

    /** `나달 16일`. */
    fun dateLabel(date: CalendarDay): String {
        val name = calendar?.months?.getOrNull(date.month - 1)?.name ?: date.month.toString()
        return "$name ${date.day}일"
    }

    /** Absolute day number (for ordering) of story day [day] (1-based). */
    fun absoluteOfStoryDay(day: Int): Long =
        (startYear ?: 0).toLong() * yearLength + startDayOfYear + (day - 1)

    /** Absolute day number of a world date; year-only dates sit at the year's first day. */
    fun absoluteOf(date: WorldDate): Long {
        val doy = date.month?.takeIf { calendar != null }?.let { dayOfYear(it, date.day ?: 1) } ?: 0
        return date.year.toLong() * yearLength + doy
    }

    /** Absolute day number of a year (its first day). */
    fun absoluteOfYear(year: Int): Long = year.toLong() * yearLength

    /** The world year of story day [day]. */
    fun yearOfStoryDay(day: Int): Int? {
        val start = startYear ?: return null
        return start + Math.floorDiv(startDayOfYear + day - 1, yearLength)
    }

    companion object {
        const val YEAR_DAYS = 365
    }
}
