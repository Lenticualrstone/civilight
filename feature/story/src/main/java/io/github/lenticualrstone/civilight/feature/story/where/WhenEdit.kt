package io.github.lenticualrstone.civilight.feature.story.where

import io.github.lenticualrstone.civilight.domain.story.bulk.BulkWhen
import io.github.lenticualrstone.civilight.domain.story.time.BandTable
import io.github.lenticualrstone.civilight.domain.story.time.BaseRules
import io.github.lenticualrstone.civilight.domain.story.time.DAY_START_MINUTE
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/** Day chips `같은 날 · +1일 · +2일 · 직접` (STO-20), as label days relative to the base. */
enum class WhenDay(val days: Int?) { SAME_DAY(0), PLUS_ONE(1), PLUS_TWO(2), DIRECT(null) }

/**
 * What the 때 part of the `장소와 때` sheet shows selected (STO-20/21). Values are what the author
 * sees: day chips count label days (so under `자정` a scene at 01:30 still reads `+1일` when it is the
 * next calendar day), [directDay] is the label story day of a `직접` fixed point.
 *
 * @property editable the 때 controls apply (false for 회상, whose form is STO-24, and for unknown variants).
 * @property onlyDirect no earlier timed scene: only `직접` is enabled (BASE-03).
 * @property day the selected chip; null for none or an interval no chip shows (Q16).
 * @property bandsEnabled band chips and the clock field need a 때 first (no guessed day).
 */
data class WhenControls(
    val editable: Boolean,
    val onlyDirect: Boolean,
    val day: WhenDay?,
    val band: Band?,
    val clock: ClockTime?,
    val precision: TimePrecision,
    val precisionEnabled: Boolean,
    val directDay: Int?,
    val bandsEnabled: Boolean,
)

/** One control change in the 때 part. */
sealed interface WhenAction {
    data class Day(val day: WhenDay) : WhenAction

    /** A band chip; tapping the selected band changes nothing (no hidden clearing). */
    data class PickBand(val band: Band) : WhenAction

    /** The exact-time field; null clears the clock and keeps the band. */
    data class SetClock(val clock: ClockTime?) : WhenAction

    data class Precision(val precision: TimePrecision) : WhenAction

    /** The story day of a `직접` fixed point (`이야기 12일째`). */
    data class DirectDay(val day: Int) : WhenAction
}

/**
 * The 때 controls as a pure reducer (STO-20, STO-21, STO-19): each control change gives the scene's
 * next stored [SceneTime], which the sheet then stores through the cascade check (STO-23).
 *
 * Stored values never depend on the day-boundary option (STO-19, STO-21): clocks are kept in the
 * 04:00-day form, and under `자정` a clock before 04:00 labels one story day later
 * (`SceneTimes.labelDayShift`). The chips and the `직접` day are label days, so entering 01:30 under
 * `자정` stores the interval one day shorter and the label day stays what the author picked.
 */
object WhenEdit {
    fun controls(doc: ProjectDoc, sceneKey: Key): WhenControls {
        val times = SceneTimes(doc)
        val scene = doc.index.unit(sceneKey)
        val onlyDirect = scene != null && BaseRules.onlyDirectEnabled(times, sceneKey)
        return when (val t = scene?.time) {
            is SceneTime.Relative -> {
                val own = shift(doc, t.clock)
                val base = baseShift(times, times.timed(sceneKey)?.baseKey ?: t.baseKey)
                // Back to label days first, then read the chip (a `대략` range is the chip's range in label days).
                val shift = own - base
                val nominal = nominal(t.dayMin + shift, t.dayMax + shift)
                val chip = nominal?.let { n -> WhenDay.entries.firstOrNull { it.days == n } }
                WhenControls(
                    editable = true,
                    onlyDirect = onlyDirect,
                    day = chip,
                    band = labelBand(doc, t.band?.known, t.clock),
                    clock = t.clock,
                    precision = t.precision.knownOr(TimePrecision.EXACT),
                    precisionEnabled = true,
                    directDay = null,
                    bandsEnabled = true,
                )
            }
            is SceneTime.Direct -> WhenControls(
                editable = true,
                onlyDirect = onlyDirect,
                day = WhenDay.DIRECT,
                band = labelBand(doc, t.band?.known, t.clock),
                clock = t.clock,
                precision = TimePrecision.EXACT,
                precisionEnabled = false,
                directDay = t.storyDay?.plus(shift(doc, t.clock)),
                bandsEnabled = true,
            )
            SceneTime.None -> WhenControls(true, onlyDirect, null, null, null, TimePrecision.EXACT, false, null, bandsEnabled = false)
            else -> WhenControls(false, onlyDirect, null, null, null, TimePrecision.EXACT, false, null, bandsEnabled = false)
        }
    }

    /** The stored 때 after [action], or null when the action does not apply to the scene's 때 now. */
    fun next(doc: ProjectDoc, sceneKey: Key, action: WhenAction): SceneTime? {
        val scene = doc.index.unit(sceneKey)?.takeIf { doc.liveness.isUnitLive(it.key) } ?: return null
        val times = SceneTimes(doc)
        val current = scene.time
        val controls = controls(doc, sceneKey)
        if (!controls.editable) return null
        return when (action) {
            is WhenAction.Day -> day(doc, times, sceneKey, current, controls, action.day)
            is WhenAction.PickBand -> when {
                !controls.bandsEnabled -> null
                controls.band == action.band -> null
                else -> {
                    val keep = current.clock?.takeIf { BandTable(doc.project.sceneTime.bandStarts).bandOf(it) == action.band }
                    withTimeOfDay(doc, times, sceneKey, current, band = action.band, clock = keep)
                }
            }
            is WhenAction.SetClock -> when {
                !controls.bandsEnabled -> null
                action.clock == null -> withTimeOfDay(doc, times, sceneKey, current, band = current.band, clock = null)
                else -> {
                    val band = BandTable(doc.project.sceneTime.bandStarts).bandOf(action.clock)
                    withTimeOfDay(doc, times, sceneKey, current, band = band, clock = action.clock)
                }
            }
            is WhenAction.Precision -> precision(current, controls, action.precision, doc, times)
            is WhenAction.DirectDay -> {
                val direct = current as? SceneTime.Direct ?: return null
                if (action.day < 1) return null
                direct.copy(storyDay = action.day - shift(doc, direct.clock), calendarDate = null)
            }
        }
    }

    private fun day(doc: ProjectDoc, times: SceneTimes, sceneKey: Key, current: SceneTime, controls: WhenControls, day: WhenDay): SceneTime? {
        val band = current.band
        val clock = current.clock
        val own = shift(doc, clock)
        if (day == WhenDay.DIRECT) {
            if (current is SceneTime.Direct) return null
            val labelDay = times.storyDay(sceneKey)?.min ?: times.previousTimed(sceneKey)?.let { times.storyDay(it.key)?.min } ?: 1
            return SceneTime.Direct(storyDay = maxOf(1, labelDay) - own, band = band?.open(), clock = clock)
        }
        if (controls.onlyDirect) return null
        val baseKey = (current as? SceneTime.Relative)?.let { times.timed(sceneKey)?.baseKey ?: it.baseKey }
            ?: BaseRules.defaultBase(times, sceneKey)
            ?: return null
        val n = day.days!!
        val approx = (current as? SceneTime.Relative)?.precision?.known == TimePrecision.APPROX
        val offset = baseShift(times, baseKey) - own
        val range = (if (approx) BulkWhen.approxRange(n) else Span.point(n)) + offset
        val rel = current as? SceneTime.Relative
        return SceneTime.Relative(
            baseKey = baseKey,
            dayMin = range.min,
            dayMax = range.max,
            precision = (if (approx) TimePrecision.APPROX else TimePrecision.EXACT).open(),
            band = band?.open(),
            clock = clock,
            ext = rel?.ext,
        )
    }

    /** New band and clock; the stored day moves with the `자정` label shift so the label day stays. */
    private fun withTimeOfDay(doc: ProjectDoc, times: SceneTimes, sceneKey: Key, current: SceneTime, band: Band?, clock: ClockTime?): SceneTime? {
        val delta = shift(doc, current.clock) - shift(doc, clock)
        return when (current) {
            is SceneTime.Relative -> current.copy(dayMin = current.dayMin + delta, dayMax = current.dayMax + delta, band = band?.open(), clock = clock)
            is SceneTime.Direct -> current.copy(storyDay = current.storyDay?.plus(delta), band = band?.open(), clock = clock)
            else -> null
        }.takeIf { it != current }
    }

    private fun precision(current: SceneTime, controls: WhenControls, precision: TimePrecision, doc: ProjectDoc, times: SceneTimes): SceneTime? {
        val rel = current as? SceneTime.Relative ?: return null
        if (rel.precision.known == precision) return null
        val offset = baseShift(times, rel.baseKey) - shift(doc, rel.clock)
        val chip = controls.day?.days
        val range = when {
            precision == TimePrecision.APPROX && chip != null -> BulkWhen.approxRange(chip) + offset
            precision == TimePrecision.APPROX -> if (rel.dayMin == rel.dayMax) BulkWhen.approxRange(rel.dayMin) else Span(rel.dayMin, rel.dayMax)
            chip != null -> Span.point(chip + offset)
            else -> Span.point((rel.dayMin + rel.dayMax) / 2)
        }
        return rel.copy(dayMin = range.min, dayMax = range.max, precision = precision.open())
    }

    /** The chip day a stored interval stands for: a point, or the `대략` range of a chip (Q17). */
    internal fun nominal(min: Int, max: Int): Int? = when {
        min == max -> min
        min == 0 && max == 1 -> 0
        max - min == 2 -> min + 1
        else -> null
    }

    private fun midnight(doc: ProjectDoc): Boolean = doc.project.sceneTime.dayBoundary.knownOr(DayBoundary.DAWN_0400) == DayBoundary.MIDNIGHT

    /** +1 under `자정` for a clock before 04:00 (it labels the next story day). */
    internal fun shift(doc: ProjectDoc, clock: ClockTime?): Int =
        if (midnight(doc) && clock != null && clock.minuteOfDay < DAY_START_MINUTE) 1 else 0

    private fun baseShift(times: SceneTimes, baseKey: Key?): Int = baseKey?.let(times::timed)?.let(times::labelDayShift) ?: 0

    private fun labelBand(doc: ProjectDoc, stored: Band?, clock: ClockTime?): Band? =
        stored ?: clock?.let { BandTable(doc.project.sceneTime.bandStarts).bandOf(it) }

    private val SceneTime.band: Band?
        get() = when (this) {
            is SceneTime.Relative -> band?.known
            is SceneTime.Direct -> band?.known
            else -> null
        }

    private val SceneTime.clock: ClockTime?
        get() = when (this) {
            is SceneTime.Relative -> clock
            is SceneTime.Direct -> clock
            else -> null
        }
}
