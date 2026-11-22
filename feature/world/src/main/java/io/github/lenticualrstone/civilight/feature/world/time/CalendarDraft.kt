package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.runtime.saveable.Saver
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.MonthDay
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One month row of the editor: `이름` and `날 수` as typed. */
@Serializable
data class MonthDraft(val name: String = "", val days: String = "")

/**
 * The 세계 달력 editor (WLD-30, `[PROPOSED]`, Q-13): ordered months (`이름` + `날 수`, integer ≥ 1),
 * optional ordered weekday names (empty = no weekdays), and the story start date (month, day) in the
 * start year with the weekday of that day. Nothing is written until `저장`.
 */
@Serializable
data class CalendarDraft(
    val months: List<MonthDraft> = listOf(MonthDraft()),
    val weekdays: List<String> = emptyList(),
    /** 1-based month of the story start. */
    val startMonth: Int = 1,
    val startDay: String = "1",
    /** 1-based weekday of the story start (only with weekdays). */
    val startWeekday: Int = 1,
) {
    fun addMonth(): CalendarDraft = copy(months = months + MonthDraft())

    fun removeMonth(index: Int): CalendarDraft {
        val next = months.filterIndexed { i, _ -> i != index }
        val month = when {
            startMonth - 1 == index -> 1
            startMonth - 1 > index -> startMonth - 1
            else -> startMonth
        }
        return copy(months = next, startMonth = month.coerceAtMost(next.size.coerceAtLeast(1)))
    }

    fun setMonth(index: Int, month: MonthDraft): CalendarDraft = copy(months = months.mapIndexed { i, m -> if (i == index) month else m })

    fun addWeekday(): CalendarDraft = copy(weekdays = weekdays + "")

    fun removeWeekday(index: Int): CalendarDraft {
        val next = weekdays.filterIndexed { i, _ -> i != index }
        val day = when {
            startWeekday - 1 == index -> 1
            startWeekday - 1 > index -> startWeekday - 1
            else -> startWeekday
        }
        return copy(weekdays = next, startWeekday = day.coerceAtMost(next.size.coerceAtLeast(1)))
    }

    fun setWeekday(index: Int, name: String): CalendarDraft = copy(weekdays = weekdays.mapIndexed { i, w -> if (i == index) name else w })

    /** The line that stops `저장`, or null when the draft is a valid calendar. */
    fun problem(): String? {
        if (months.isEmpty()) return WorldCopy.TODO_COPY_WORLD_CALENDAR_NO_MONTH
        if (months.any { it.name.isBlank() }) return WorldCopy.TODO_COPY_WORLD_CALENDAR_MONTH_NAME
        if (months.any { days(it) == null }) return WorldCopy.TODO_COPY_WORLD_CALENDAR_MONTH_DAYS
        if (weekdays.any { it.isBlank() }) return WorldCopy.TODO_COPY_WORLD_CALENDAR_WEEKDAY_NAME
        val start = months.getOrNull(startMonth - 1) ?: return WorldCopy.TODO_COPY_WORLD_CALENDAR_START
        val day = startDay.trim().toIntOrNull() ?: return WorldCopy.TODO_COPY_WORLD_CALENDAR_START
        if (day !in 1..(days(start) ?: 0)) return WorldCopy.TODO_COPY_WORLD_CALENDAR_START
        return null
    }

    /** The calendar to save, or null while [problem] is set. */
    fun toCalendar(previous: WorldCalendar? = null): WorldCalendar? {
        if (problem() != null) return null
        val base = previous ?: WorldCalendar(months = emptyList())
        return base.copy(
            months = months.mapIndexed { i, m ->
                val old = previous?.months?.getOrNull(i)
                (old ?: CalendarMonth(name = "", days = 1)).copy(name = m.name.trim(), days = days(m)!!)
            },
            weekdays = weekdays.map { it.trim() },
            storyStart = MonthDay(startMonth, startDay.trim().toInt()),
            storyStartWeekday = if (weekdays.isEmpty()) null else startWeekday.coerceIn(1, weekdays.size),
        )
    }

    companion object {
        private fun days(m: MonthDraft): Int? = m.days.trim().toIntOrNull()?.takeIf { it >= 1 }

        /** The editor seeded from the stored calendar (one empty month row when there is none). */
        fun of(calendar: WorldCalendar?): CalendarDraft {
            if (calendar == null || calendar.months.isEmpty()) return CalendarDraft()
            return CalendarDraft(
                months = calendar.months.map { MonthDraft(it.name, it.days.toString()) },
                weekdays = calendar.weekdays,
                startMonth = calendar.storyStart?.month?.coerceIn(1, calendar.months.size) ?: 1,
                startDay = (calendar.storyStart?.day ?: 1).toString(),
                startWeekday = calendar.storyStartWeekday ?: 1,
            )
        }

        private val json = Json { ignoreUnknownKeys = true }

        val Saver: Saver<CalendarDraft, String> = Saver(
            save = { json.encodeToString(serializer(), it) },
            restore = { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() ?: CalendarDraft() },
        )
    }
}
