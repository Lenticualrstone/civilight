package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.BandStarts

/** Minutes in a day. */
const val DAY_MINUTES = 1440

/** The stored day starts at 04:00 (STO-19, STO-21); clocks are kept in that canonical form. */
const val DAY_START_MINUTE = 4 * 60

/** Minutes after the 04:00 day start for a clock (`05:40` → 100, `01:30` → 1290). */
fun ClockTime.minutesAfterDayStart(): Int = Math.floorMod(minuteOfDay - DAY_START_MINUTE, DAY_MINUTES)

/** The clock at [minutes] after the 04:00 day start. */
fun clockAtMinutesAfterDayStart(minutes: Int): ClockTime =
    ClockTime(Math.floorMod(minutes + DAY_START_MINUTE, DAY_MINUTES))

/** Korean band word (`새벽 · 아침 · 낮 · 저녁 · 밤`). */
val Band.word: String
    get() = when (this) {
        Band.DAWN -> "새벽"
        Band.MORNING -> "아침"
        Band.DAY -> "낮"
        Band.EVENING -> "저녁"
        Band.NIGHT -> "밤"
    }

/** Parses a band word. */
fun bandOfWord(word: String): Band? = Band.entries.firstOrNull { it.word == word }

/**
 * The five bands of a project (STO-19): contiguous half-open ranges `[start, next start)` covering
 * 24 h; 밤 wraps to the next 새벽. Positions are minutes after the 04:00 day start.
 */
class BandTable(private val starts: BandStarts = BandStarts()) {
    private val ordered: List<Band> = listOf(Band.DAWN, Band.MORNING, Band.DAY, Band.EVENING, Band.NIGHT)

    fun start(band: Band): ClockTime = starts.startOf(band)

    /**
     * The band as a closed minute range after the day start, e.g. 저녁 `[780, 960]` (17:00–20:00).
     * The end is the next band's start (closed bound, so differences reach both ends: TRV-03).
     */
    fun range(band: Band): Span {
        val s = starts.startOf(band).minutesAfterDayStart()
        val next = ordered[(ordered.indexOf(band) + 1) % ordered.size]
        var e = starts.startOf(next).minutesAfterDayStart()
        if (e <= s) e += DAY_MINUTES
        return Span(s, e)
    }

    /** The band containing a clock (`05:40` → 새벽, `03:59` → 밤). */
    fun bandOf(clock: ClockTime): Band {
        val m = clock.minuteOfDay
        for (band in ordered) {
            val s = starts.startOf(band).minuteOfDay
            val next = ordered[(ordered.indexOf(band) + 1) % ordered.size]
            val e = starts.startOf(next).minuteOfDay
            val inside = if (s < e) m in s until e else m >= s || m < e
            if (inside) return band
        }
        return Band.NIGHT
    }

    /** `17–20` (hours; `:mm` only when not on the hour). */
    fun hoursLabel(band: Band): String {
        val s = starts.startOf(band)
        val next = ordered[(ordered.indexOf(band) + 1) % ordered.size]
        val e = starts.startOf(next)
        return "${hourText(s)}–${hourText(e)}"
    }

    private fun hourText(c: ClockTime): String =
        if (c.minute == 0) c.hour.toString() else "${c.hour}:${c.minute.toString().padStart(2, '0')}"
}
