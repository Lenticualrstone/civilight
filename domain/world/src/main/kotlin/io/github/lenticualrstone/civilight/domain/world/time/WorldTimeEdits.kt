package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode

/**
 * `프로젝트 설정 › 세계 시간` (WLD-29): the mode `연도만 · 세계 달력 · 시간 없이` and `이야기가 시작되는 해`.
 * Changing the mode never deletes stored dates: event dates, the calendar and the start year stay
 * as they are, and modes that cannot show them simply do not read them.
 */
object WorldTimeEdits {
    /** Sets the mode; everything else of [WorldTime] is kept. */
    fun setMode(doc: ProjectDoc, mode: WorldTimeMode): ProjectDoc =
        withWorldTime(doc) { it.copy(mode = mode.open()) }

    /** Sets `이야기가 시작되는 해` (null = not set). */
    fun setStartYear(doc: ProjectDoc, year: Int?): ProjectDoc =
        withWorldTime(doc) { it.copy(storyStartYear = year) }

    /**
     * Saves the world calendar (WLD-30): months in order, weekday names (may be empty) and the story
     * start date. The mode is not changed and event dates are kept as stored.
     */
    fun setCalendar(doc: ProjectDoc, calendar: io.github.lenticualrstone.civilight.model.project.WorldCalendar?): ProjectDoc =
        withWorldTime(doc) { it.copy(calendar = calendar) }

    /** The year typed in the field: an integer (a leading minus allowed), else null. */
    fun parseYear(text: String): Int? = text.trim().takeIf { YEAR.matches(it) }?.toIntOrNull()

    private val YEAR = Regex("-?\\d{1,6}")

    private fun withWorldTime(doc: ProjectDoc, f: (WorldTime) -> WorldTime): ProjectDoc {
        val next = f(doc.project.worldTime)
        return if (next == doc.project.worldTime) doc else doc.copy(project = doc.project.copy(worldTime = next))
    }
}
