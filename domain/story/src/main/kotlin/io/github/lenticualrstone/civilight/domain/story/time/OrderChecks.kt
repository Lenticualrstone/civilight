package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.domain.story.checks.AckSubject
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType

/**
 * `◇ 순서와 때가 다름` (STO-25): scene X is definitely earlier than the nearest earlier timed
 * non-flashback scene P (`max(A(X) − A(P)) < 0`). A range that straddles 0 raises nothing; it is
 * never a ▲.
 *
 * @property difference `A(X) − A(P)` in minutes.
 */
data class OrderNotice(val sceneKey: Key, val previousKey: Key, val difference: Span) : AckSubject {
    /** `직전 씬보다 앞 · 9시간 전` (exact) or `직전 씬보다 앞 · 약 9시간 전` (midpoint, `ED`). */
    val line: String
        get() {
            val earlier = Span(-difference.max, -difference.min)
            val text = if (earlier.isPoint) Durations.gap(earlier.min) else Durations.approx(earlier.midpoint)
            return "직전 씬보다 앞 · $text 전"
        }

    override val checkType: String get() = CheckType.TIME_ORDER

    /** Scene + previous scene (STO-40 row 16). */
    override val subjectKeys: List<Key> get() = listOf(sceneKey, previousKey)

    /** Any change of the two scenes' time difference voids a `맞음` (ORD-03). */
    override val fingerprint: String
        get() = Fingerprints.of(checkType, sceneKey, previousKey, difference.min, difference.max)
}

/** `◇ 시각과 시간대가 다름` (STO-19, registry row 15) for a scene whose clock lies outside its band. */
data class ClockBandNotice(val sceneKey: Key, val line: String, val bandWord: String, val clock: String) : AckSubject {
    override val checkType: String get() = CheckType.TIME_CLOCK_BAND_MISMATCH
    override val subjectKeys: List<Key> get() = listOf(sceneKey)
    override val fingerprint: String get() = Fingerprints.of(checkType, sceneKey, bandWord, clock)
}

object OrderChecks {
    /** Every order/time notice of the project, manuscript order. */
    fun notices(times: SceneTimes): List<OrderNotice> = times.liveScenes.mapNotNull { notice(times, it.key) }

    /** The notice for one scene, or null. */
    fun notice(times: SceneTimes, sceneKey: Key): OrderNotice? {
        if (times.timed(sceneKey) == null) return null
        val previous = times.previousTimed(sceneKey) ?: return null
        val diff = times.minutesBetween(previous.key, sceneKey) ?: return null
        return if (diff.max < 0) OrderNotice(sceneKey, previous.key, diff) else null
    }

    /** Notices present in [after] but not in [before] (by scene and previous scene). */
    fun newNotices(before: SceneTimes, after: SceneTimes): List<OrderNotice> {
        val old = notices(before).map { it.sceneKey to it.previousKey }.toSet()
        return notices(after).filter { (it.sceneKey to it.previousKey) !in old }
    }

    /** `◇ 시각과 시간대가 다름` items: `<scene> · 새벽 07:00` (`ED` row copy). */
    fun clockBandNotices(times: SceneTimes, codes: io.github.lenticualrstone.civilight.domain.story.units.UnitCodes): List<ClockBandNotice> =
        times.liveScenes.mapNotNull { s ->
            if (!times.clockBandMismatch(s.key)) return@mapNotNull null
            val t = times.timed(s.key)!!
            val word = t.storedBand!!.word
            ClockBandNotice(s.key, "${codes.sceneRef(s)} · $word ${t.clock}", word, t.clock.toString())
        }
}
