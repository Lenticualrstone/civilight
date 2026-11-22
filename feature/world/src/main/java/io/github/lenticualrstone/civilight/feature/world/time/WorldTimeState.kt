package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode

/** One radio of `세계 시간` (board `NewTime`): `연도만 · 예: 1200년`. */
data class WorldTimeOptionUi(val mode: WorldTimeMode, val title: String, val subtitle: String, val selected: Boolean)

/** `프로젝트 설정 › 세계 시간` (WLD-29). */
data class WorldTimeUi(
    val projectName: String,
    val options: List<WorldTimeOptionUi>,
    /** `이야기가 시작되는 해`; hidden in `시간 없이`. */
    val showStartYear: Boolean,
    val startYear: String,
    /** `달력: 가달 · 나달` once a calendar exists (the editor is WLD-30). */
    val calendarLine: String?,
    val readOnly: Boolean,
    /** The month and weekday editor shows under `세계 달력 만들기` (WLD-30). */
    val showCalendarEditor: Boolean = false,
    /** The stored calendar (kept in every mode, WLD-29). */
    val calendar: io.github.lenticualrstone.civilight.model.project.WorldCalendar? = null,
)

/** Builds the 세계 시간 screen; the mode change never touches stored dates (`WorldTimeEdits`). */
object WorldTimeState {
    fun build(doc: ProjectDoc, readOnly: Boolean): WorldTimeUi {
        val time = doc.project.worldTime
        val mode = time.mode.knownOr(WorldTimeMode.YEAR_ONLY)
        val options = listOf(
            WorldTimeOptionUi(WorldTimeMode.YEAR_ONLY, WorldCopy.TIME_YEAR_ONLY, WorldCopy.TIME_YEAR_ONLY_SUB, mode == WorldTimeMode.YEAR_ONLY),
            WorldTimeOptionUi(WorldTimeMode.CALENDAR, WorldCopy.TIME_CALENDAR, WorldCopy.TIME_CALENDAR_SUB, mode == WorldTimeMode.CALENDAR),
            WorldTimeOptionUi(WorldTimeMode.NONE, WorldCopy.TIME_NONE, WorldCopy.TIME_NONE_SUB, mode == WorldTimeMode.NONE),
        )
        val months = time.calendar?.months.orEmpty()
        return WorldTimeUi(
            projectName = doc.project.name,
            options = options,
            showStartYear = mode != WorldTimeMode.NONE,
            startYear = time.storyStartYear?.toString().orEmpty(),
            calendarLine = if (mode == WorldTimeMode.CALENDAR && months.isNotEmpty()) WorldCopy.calendarLine(months.map { it.name }) else null,
            readOnly = readOnly,
            showCalendarEditor = mode == WorldTimeMode.CALENDAR,
            calendar = time.calendar,
        )
    }
}
