package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.EventKind

/** Something placed in time: a scene or a timeline event. */
sealed interface TimeSubject {
    data class Scene(val sceneKey: Key) : TimeSubject
    data class Event(val eventKey: Key) : TimeSubject
}

/** Where a subject sits in world time. */
sealed interface Moment {
    /** Absolute day number for ordering (see [WorldClock]); the earliest day for a range. */
    val absolute: Long

    /** Story days (1-based, inclusive). */
    data class StoryDays(val dayMin: Int, val dayMax: Int, override val absolute: Long) : Moment

    /** A world date (world events, 회상). */
    data class WorldYear(val year: Int, val month: Int?, val day: Int?, override val absolute: Long) : Moment
}

/** Places scenes and events in world time and labels them for the timeline views (WLD-31, WLD-34). */
class Moments(private val doc: ProjectDoc, private val times: SceneTimeSource) {
    val clock = WorldClock(doc.project.worldTime)

    /** The moment of a scene or event, or null when it has no time. */
    fun of(subject: TimeSubject): Moment? = when (subject) {
        is TimeSubject.Scene -> ofScene(subject.sceneKey)
        is TimeSubject.Event -> ofEvent(subject.eventKey)
    }

    fun ofScene(sceneKey: Key): Moment? = when (val w = times.timeOf(sceneKey)) {
        null -> null
        is SceneWhen.Story -> Moment.StoryDays(w.dayMin, w.dayMax, clock.absoluteOfStoryDay(w.dayMin))
        is SceneWhen.Flashback -> {
            val eventDate = w.eventKey?.let { doc.index.event(it)?.worldDate }
            val year = w.year ?: eventDate?.year
            if (year == null) {
                null
            } else {
                val month = w.month ?: eventDate?.month.takeIf { w.year == null }
                val day = w.day ?: eventDate?.day.takeIf { w.year == null }
                Moment.WorldYear(year, month, day, clock.absoluteOf(io.github.lenticualrstone.civilight.model.world.WorldDate(year, month, day)))
            }
        }
    }

    fun ofEvent(eventKey: Key): Moment? {
        val event = doc.index.event(eventKey) ?: return null
        return when (event.kind.known) {
            EventKind.STORY -> event.sceneKey?.let(::ofScene)
            else -> event.worldDate?.let { Moment.WorldYear(it.year, it.month, it.day, clock.absoluteOf(it)) }
        }
    }

    /** The world year of a moment. */
    fun yearOf(moment: Moment): Int? = when (moment) {
        is Moment.WorldYear -> moment.year
        is Moment.StoryDays -> clock.yearOfStoryDay(moment.dayMin)
    }

    /** The scene a subject stands for (a scene, or a story event's scene). */
    fun sceneOf(subject: TimeSubject): Key? = when (subject) {
        is TimeSubject.Scene -> subject.sceneKey
        is TimeSubject.Event -> doc.index.event(subject.eventKey)?.takeIf { it.kind.known == EventKind.STORY }?.sceneKey
    }

    /** `이야기 시작부터` label: story days, or the year for world dates. */
    fun storyStartLabel(subject: TimeSubject): TimeLabel? = when (val m = of(subject)) {
        null -> null
        is Moment.StoryDays -> TimeLabels.storyDay(m.dayMin, m.dayMax)
        is Moment.WorldYear -> TimeLabel(m.year.toString())
    }

    /**
     * `기준 사건` label of [subject] relative to [anchor]: `기준` on the anchor itself, days between
     * story moments (`−1일`, `+약 7일` with `5~9일`), years when a world date is involved (`−5년`).
     */
    fun anchorLabel(subject: TimeSubject, anchor: TimeSubject): TimeLabel? {
        if (subject == anchor || sceneOf(subject) != null && sceneOf(subject) == sceneOf(anchor)) return TimeLabels.ANCHOR
        val a = of(subject) ?: return null
        val b = of(anchor) ?: return null
        if (a is Moment.StoryDays && b is Moment.StoryDays) {
            return TimeLabels.dayOffset(a.dayMin - b.dayMax, a.dayMax - b.dayMin)
        }
        val ya = yearOf(a) ?: return null
        val yb = yearOf(b) ?: return null
        return TimeLabels.yearOffset(ya - yb)
    }
}
