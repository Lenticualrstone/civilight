package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.FlashbackRelation

/** A scene's 때 resolved to story time (STO-22); derived, never stored. */
sealed interface ResolvedTime {
    /** No 때 (or a variant from a newer file). */
    data object None : ResolvedTime

    /** 회상: outside the chain, no `N일째`, skipped by checks. */
    data class Flashback(
        val year: Int?,
        val eventKey: Key?,
        val relation: FlashbackRelation?,
        val band: Band?,
        val clock: ClockTime?,
    ) : ResolvedTime

    /**
     * A scene in the time chain.
     *
     * @property day story day (1-based) as a shared-variable expression.
     * @property t time of day in minutes after 04:00, closed: a point for an exact clock, the band
     *   range for a band, `[0, 1440]` for neither. Never inherited along the chain.
     * @property storedBand the band the author picked (null when only a clock or nothing).
     * @property band the label band: [storedBand], else the band containing [clock].
     * @property fixed a `직접` fixed point.
     * @property ownApprox the scene's own link is `대략`.
     * @property baseKey the base actually used (the stored base, or the fallback when it was unusable).
     */
    data class Timed(
        val day: LinExpr,
        val t: Span,
        val storedBand: Band?,
        val clock: ClockTime?,
        val band: Band?,
        val fixed: Boolean,
        val ownApprox: Boolean,
        val baseKey: Key?,
    ) : ResolvedTime {
        /** Story-day bounds. */
        val dayRange: Span get() = day.range

        /** Bounds of the absolute minute `(day − 1)·1440 + t`. */
        val absolute: Span get() = (day.range - 1) * DAY_MINUTES + t
    }
}

/** Absolute-minute difference `A(to) − A(from)` with shared variables cancelled (STO-22). */
fun minutesBetween(from: ResolvedTime.Timed, to: ResolvedTime.Timed): Span {
    val days = (to.day - from.day).range
    return Span(
        days.min * DAY_MINUTES + to.t.min - from.t.max,
        days.max * DAY_MINUTES + to.t.max - from.t.min,
    )
}

/** Story-day difference `day(to) − day(from)`, shared variables cancelled. */
fun daysBetween(from: ResolvedTime.Timed, to: ResolvedTime.Timed): LinExpr = to.day - from.day
