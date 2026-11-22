package io.github.lenticualrstone.civilight.feature.story.where

import io.github.lenticualrstone.civilight.domain.story.time.FlashbackLines
import io.github.lenticualrstone.civilight.domain.story.time.ResolvedTime
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.story.CalendarDate
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.FlashbackRelation
import io.github.lenticualrstone.civilight.model.story.SceneTime

/**
 * The 회상 form (STO-24, board `RTFlashback`): `때 · 기준 | 회상은 직전 씬 기준 없음`, segmented
 * `기준 사건 | 직접`, then either `기준 사건 | 대정전 1195 바꾸기 ›` with `전 · 그날 · 후`, or the year.
 *
 * @property eventName the base event's title; null when none is picked.
 * @property eventDeleted the base event is in the trash: dotted name with `◇` (WLD-32 `[PROPOSED]`); the
 *   stored time stays.
 * @property relation `그날` when the stored relation is missing (as the result line reads it).
 * @property year the stored year of a `직접` flashback.
 * @property skipLine `씬 3 때 기준` · `씬 1 · 회상 건너뜀`.
 * @property months the world calendar's month names when the calendar is on (`직접` then takes a date
 *   too, STO-24 / STO-18); null otherwise.
 * @property date the stored calendar date of a `직접` flashback (1-based month and day).
 */
data class FlashbackUi(
    val mode: FlashbackMode,
    val eventKey: Key?,
    val eventName: String?,
    val eventYear: Int?,
    val eventDeleted: Boolean,
    val relation: FlashbackRelation,
    val year: Int?,
    val skipLine: Pair<String, String>?,
    val months: List<CalendarMonth>? = null,
    val date: CalendarDate? = null,
)

/** One control change in the 회상 form. */
sealed interface FlashbackAction {
    data class Mode(val mode: FlashbackMode) : FlashbackAction

    data class Event(val eventKey: Key) : FlashbackAction

    data class Relation(val relation: FlashbackRelation) : FlashbackAction

    data class Year(val year: Int) : FlashbackAction

    /** `직접` date with the world calendar on: [month] and [day] are 1-based. */
    data class Date(val month: Int, val day: Int) : FlashbackAction
}

/**
 * The 회상 form as a pure reducer: each control gives the scene's next stored `SceneTime.Flashback`,
 * built with `copy` so the band, clock, the Q20 amount placeholder and unknown fields survive. A
 * flashback is outside the chain, so these edits never move another scene (no cascade preview).
 */
object FlashbackModel {
    fun ui(doc: ProjectDoc, sceneKey: Key): FlashbackUi? {
        val t = doc.index.unit(sceneKey)?.time as? SceneTime.Flashback ?: return null
        val event = t.eventKey?.let(doc.index::event)
        return FlashbackUi(
            mode = t.mode.known ?: FlashbackMode.DIRECT,
            eventKey = t.eventKey,
            eventName = event?.title,
            eventYear = event?.worldDate?.year,
            eventDeleted = event != null && event.deletedAt != null,
            relation = t.relation?.known ?: FlashbackRelation.ON,
            year = t.year,
            skipLine = FlashbackLines.skipLine(doc, sceneKey),
            months = calendar(doc)?.months,
            date = t.calendarDate,
        )
    }

    /** The world calendar while the calendar mode is on (WLD-30), else null. */
    private fun calendar(doc: ProjectDoc): WorldCalendar? =
        doc.project.worldTime.takeIf { it.mode.known == WorldTimeMode.CALENDAR }?.calendar?.takeIf { it.months.isNotEmpty() }

    /** The next stored 때, or null when the change is not possible (not a flashback, no such event). */
    fun next(doc: ProjectDoc, sceneKey: Key, action: FlashbackAction): SceneTime? {
        val t = doc.index.unit(sceneKey)?.time as? SceneTime.Flashback ?: return null
        return when (action) {
            is FlashbackAction.Mode -> when (action.mode) {
                FlashbackMode.ANCHOR -> t.copy(mode = FlashbackMode.ANCHOR.open())
                // `직접` starts at the year the scene shows now, so switching alone changes no label.
                FlashbackMode.DIRECT -> t.copy(mode = FlashbackMode.DIRECT.open(), year = t.year ?: shownYear(doc, sceneKey))
            }
            is FlashbackAction.Event -> {
                doc.index.event(action.eventKey) ?: return null
                t.copy(mode = FlashbackMode.ANCHOR.open(), eventKey = action.eventKey)
            }
            is FlashbackAction.Relation -> t.copy(relation = action.relation.open())
            is FlashbackAction.Year -> t.copy(
                mode = FlashbackMode.DIRECT.open(),
                year = action.year,
                calendarDate = t.calendarDate?.copy(yearOffset = yearOffset(doc, action.year)),
            )
            is FlashbackAction.Date -> {
                val month = calendar(doc)?.months?.getOrNull(action.month - 1) ?: return null
                if (action.day !in 1..month.days) return null
                val year = t.year ?: shownYear(doc, sceneKey)
                t.copy(
                    mode = FlashbackMode.DIRECT.open(),
                    year = year,
                    calendarDate = CalendarDate(action.month, action.day, yearOffset(doc, year), t.calendarDate?.ext),
                )
            }
        }
    }

    /** The year the flashback resolves to now (event year in `기준 사건`, stored year in `직접`). */
    private fun shownYear(doc: ProjectDoc, sceneKey: Key): Int? = (SceneTimes(doc).resolve(sceneKey) as? ResolvedTime.Flashback)?.year

    /** [CalendarDate.yearOffset]: years from the story start year (0 when either is unknown). */
    private fun yearOffset(doc: ProjectDoc, year: Int?): Int {
        val start = doc.project.worldTime.storyStartYear ?: return 0
        return if (year == null) 0 else year - start
    }

    /** A day typed into the `직접` date field: 1 … [maxDay]; null while it is not one. */
    fun parseDay(text: String, maxDay: Int): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..maxDay }

    /** A year typed into the `직접` field: digits, optionally negative; null while it is not one. */
    fun parseYear(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in -MAX_YEAR..MAX_YEAR }

    private const val MAX_YEAR = 99_999
}
