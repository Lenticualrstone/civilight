package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/** The time-of-day part of a stored 때 (band and clock), or nulls. */
internal val SceneTime.bandValue: OpenEnum<Band>?
    get() = when (this) {
        is SceneTime.Relative -> band
        is SceneTime.Direct -> band
        is SceneTime.Flashback -> band
        else -> null
    }

internal val SceneTime.clockValue: ClockTime?
    get() = when (this) {
        is SceneTime.Relative -> clock
        is SceneTime.Direct -> clock
        is SceneTime.Flashback -> clock
        else -> null
    }

/**
 * Rewrites scene [sceneKey]'s 때 as `기준 [newBaseKey] + interval` so that its absolute time stays
 * (R11 §4; STO-09, STO-10, STO-21 `기준을 직전 씬으로`). The interval is the shared-variable day
 * difference: exact when the two chains share every `대략` link, else a `대략` day range. The
 * time of day is kept. Without a new base, an exact day becomes a `직접` story day; an inexact one
 * cannot be expressed and the stored value is returned unchanged.
 *
 * @param times the engine of the state **before** the rewrite (the absolute time to keep).
 */
fun keepAbsoluteOn(times: SceneTimes, sceneKey: Key, newBaseKey: Key?, rebasedFromKey: Key? = null): SceneTime? {
    val scene = times.doc.index.unit(sceneKey) ?: return null
    val now = times.timed(sceneKey) ?: return null
    val stored = scene.time
    if (newBaseKey == null) {
        if (!now.day.isExact) return stored
        return SceneTime.Direct(storyDay = now.day.constant, band = stored.bandValue, clock = stored.clockValue)
    }
    val base = times.timed(newBaseKey) ?: return null
    val diff = (now.day - base.day).range
    val ownPrecision = (stored as? SceneTime.Relative)?.precision
    val precision = when {
        !diff.isPoint -> TimePrecision.APPROX.open()
        ownPrecision != null -> ownPrecision
        else -> TimePrecision.EXACT.open()
    }
    return SceneTime.Relative(
        baseKey = newBaseKey,
        dayMin = diff.min,
        dayMax = diff.max,
        precision = precision,
        band = stored.bandValue,
        clock = stored.clockValue,
        rebasedFromKey = rebasedFromKey,
        ext = (stored as? SceneTime.Relative)?.ext,
    )
}

/** The same stored 때 shifted by [days] (interval + k for `기준`, day + k for `직접`; STO-30 time chip). */
fun SceneTime.shiftedByDays(days: Int): SceneTime = when (this) {
    is SceneTime.Relative -> copy(dayMin = dayMin + days, dayMax = dayMax + days)
    is SceneTime.Direct -> storyDay?.let { copy(storyDay = it + days) } ?: this
    else -> this
}

/** `<P> 기준 · 같은 때`: interval 0 with P's band, clock and precision (STO-05, STO-09). */
fun sameTimeAs(times: SceneTimes, baseKey: Key): SceneTime {
    val base = times.doc.index.unit(baseKey)?.time ?: SceneTime.None
    return SceneTime.Relative(
        baseKey = baseKey,
        dayMin = 0,
        dayMax = 0,
        precision = (base as? SceneTime.Relative)?.precision ?: TimePrecision.EXACT.open(),
        band = base.bandValue,
        clock = base.clockValue,
    )
}
