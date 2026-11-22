package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.story.time.ResolvedTime
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.story.SceneTime

/** A world date at a story point: a year, plus month and day when the calendar gives them. */
data class StoryDate(val year: Int, val month: Int? = null, val day: Int? = null) {
    val hasMonthDay: Boolean get() = month != null && day != null
}

/**
 * Where ages are computed (CST-25): the world date at a read point. `domain:story` owns the full
 * time engine; cast takes this as an input so a better resolver can replace [StoryDates.Default].
 */
fun interface StoryDateSource {
    /** The date at [point], or null when the project has no world time. */
    fun dateAt(doc: ProjectDoc, point: ReadPoint): StoryDate?
}

/**
 * Self-contained default (CST Q20): a lens on an episode or chapter reads the last live,
 * non-flashback scene up to the lens whose 때 resolves; a lens on one scene and a block read that
 * scene (a flashback scene reads its event's year or its own year); an event reads its world date.
 * Story days come from `domain:story`'s [SceneTimes] (a range uses its earliest day) and map to
 * years as `start year + (day − 1) ÷ 365` with `연도만`, and through the calendar's month lengths
 * with `세계 달력`. With no usable 때 the story start year applies.
 */
object StoryDates {
    val Default: StoryDateSource = StoryDateSource { doc, point -> resolve(doc, point) }

    private fun resolve(doc: ProjectDoc, point: ReadPoint): StoryDate? {
        val time = doc.project.worldTime
        val mode = time.mode.known ?: WorldTimeMode.YEAR_ONLY
        if (mode == WorldTimeMode.NONE) return null
        val start = time.storyStartYear ?: return null
        val resolver = Resolver(doc, start, if (mode == WorldTimeMode.CALENDAR) time.calendar else null)
        return when (point) {
            is ReadPoint.AtEvent -> doc.index.event(point.eventKey)?.worldDate?.let { StoryDate(it.year, it.month, it.day) }
                ?: StoryDate(start)
            is ReadPoint.BeforeBlock -> doc.index.block(point.blockKey)?.let { resolver.sceneDate(it.sceneKey) } ?: StoryDate(start)
            is ReadPoint.AtLens -> resolver.lensDate(point.lens)
        }
    }

    private class Resolver(private val doc: ProjectDoc, private val startYear: Int, private val calendar: WorldCalendar?) {
        private val times = SceneTimes(doc)

        fun lensDate(lens: LensPoint): StoryDate {
            // A lens on one scene reads that scene (a flashback scene reads its own year, CST-25).
            if (lens is LensPoint.UnitEnd) {
                val unit = doc.index.unit(lens.unitKey)
                if (unit != null && unit.level == doc.project.structure.sceneLevel) return sceneDate(unit.key)
            }
            val lensRank = doc.manuscript.rankOf(lens)
            val scenes = doc.manuscript.liveScenes.filter { s ->
                val end = doc.manuscript.rankOf(Position.End(s.key))
                end != null && end <= lensRank
            }
            for (scene in scenes.asReversed()) {
                storyDay(scene.key)?.let { return dateOfDay(it) }
            }
            return StoryDate(startYear)
        }

        fun sceneDate(sceneKey: Key): StoryDate {
            val scene = doc.index.unit(sceneKey) ?: return StoryDate(startYear)
            val flashback = times.resolve(sceneKey) as? ResolvedTime.Flashback
            if (flashback != null) {
                val time = scene.time as? SceneTime.Flashback
                val event = time?.eventKey?.let { doc.index.event(it) }?.worldDate
                if (event != null) return StoryDate(event.year, event.month, event.day)
                flashback.year?.let { y -> return StoryDate(y, time?.calendarDate?.month, time?.calendarDate?.day) }
            }
            storyDay(sceneKey)?.let { return dateOfDay(it) }
            // An untimed scene takes the last timed scene before it.
            val index = doc.manuscript.sceneIndex(sceneKey) ?: return StoryDate(startYear)
            for (i in index - 1 downTo 0) {
                storyDay(doc.manuscript.liveScenes[i].key)?.let { return dateOfDay(it) }
            }
            return StoryDate(startYear)
        }

        /** 1-based story day of a scene in the time chain (earliest day of a range), or null (flashback, untimed). */
        fun storyDay(sceneKey: Key): Int? = times.timed(sceneKey)?.dayRange?.min

        fun dateOfDay(day: Int): StoryDate {
            val cal = calendar
            if (cal == null || cal.months.isEmpty()) return StoryDate(startYear + Math.floorDiv(day - 1, DAYS_PER_YEAR))
            val math = CalendarMath(cal)
            val startOrdinal = cal.storyStart?.let { math.ordinal(it.month, it.day) } ?: 0
            val absolute = startOrdinal + day - 1
            val year = startYear + Math.floorDiv(absolute, math.yearLength)
            val (month, d) = math.monthDay(Math.floorMod(absolute, math.yearLength))
            return StoryDate(year, month, d)
        }
    }

    private const val DAYS_PER_YEAR = 365
}

/** Day arithmetic in a world calendar (1-based months and days). */
internal class CalendarMath(private val calendar: WorldCalendar) {
    val yearLength: Int = calendar.months.sumOf { it.days }.coerceAtLeast(1)

    /** 0-based day of the year. */
    fun ordinal(month: Int, day: Int): Int {
        val before = calendar.months.take((month - 1).coerceIn(0, calendar.months.size)).sumOf { it.days }
        return before + (day - 1).coerceAtLeast(0)
    }

    /** Month and day (1-based) of a 0-based day of the year. */
    fun monthDay(ordinal: Int): Pair<Int, Int> {
        var rest = ordinal
        for ((i, m) in calendar.months.withIndex()) {
            if (rest < m.days) return (i + 1) to (rest + 1)
            rest -= m.days
        }
        return calendar.months.size to (calendar.months.lastOrNull()?.days ?: 1)
    }
}
