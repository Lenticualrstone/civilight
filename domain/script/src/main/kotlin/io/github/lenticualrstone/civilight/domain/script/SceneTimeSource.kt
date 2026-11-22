package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.BandStarts

/**
 * A scene's resolved 때 as the place & time area computes it (STO-22). The script never resolves
 * base + interval itself: heads, transition text, snapshots and exports take this as input.
 */
data class ResolvedSceneTime(
    /** Story days from the story start (`이야기 N일째`), min..max. Null for a flashback. */
    val storyDay: IntSpan? = null,
    /** `N` of `N일째` (episode-relative), null when not counted (flashback, untimed). */
    val episodeDay: Int? = null,
    /** True when the episode day is uncertain (`약 N일째`). */
    val episodeDayApprox: Boolean = false,
    /** The stored band, else the band containing the exact clock. */
    val band: Band? = null,
    /** The exact clock, null when the time is a band or the whole day. */
    val clock: ClockTime? = null,
    /** Precision of the scene's own 때 (`정확 · 대략`). */
    val approx: Boolean = false,
    val flashback: Boolean = false,
    /** Year of a flashback (`회상 · 1195`). */
    val flashbackYear: Int? = null,
    /** Time of day in minutes after the 04:00 day start, min..max (snapshot input). */
    val tRange: IntSpan? = null,
)

/** Resolved 때 of a scene, or null when it has none. Supplied by the place & time area. */
fun interface SceneTimeSource {
    fun timeOf(sceneKey: Key): ResolvedSceneTime?

    companion object {
        /** No scene has 때. */
        val NONE: SceneTimeSource = SceneTimeSource { null }

        /** A fixed table (tests, previews). */
        fun of(times: Map<Key, ResolvedSceneTime>): SceneTimeSource = SceneTimeSource { times[it] }
    }
}

/** Band helpers (rule 18 §1–§2): 새벽 04–06 · 아침 06–10 · 낮 10–17 · 저녁 17–20 · 밤 20–04. */
object Bands {
    /** The band containing [clock] for the project's band starts. */
    fun bandOf(clock: ClockTime, starts: BandStarts = BandStarts()): Band {
        val order = Band.entries.sortedBy { starts.startOf(it).minuteOfDay }
        return order.lastOrNull { starts.startOf(it).minuteOfDay <= clock.minuteOfDay } ?: order.last()
    }

    /** Minutes after the 04:00 day start of a clock (`06:10` → 130, `01:30` → 1290). */
    fun minutesAfterDayStart(clock: ClockTime): Int = (clock.minuteOfDay - 240 + ClockTime.MINUTES_PER_DAY) % ClockTime.MINUTES_PER_DAY

    /** The clock of a minutes-after-04:00 value. */
    fun clockOf(minutesAfterDayStart: Int): ClockTime = ClockTime((minutesAfterDayStart + 240) % ClockTime.MINUTES_PER_DAY)
}
