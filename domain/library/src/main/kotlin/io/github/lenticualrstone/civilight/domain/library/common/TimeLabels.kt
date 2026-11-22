package io.github.lenticualrstone.civilight.domain.library.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Time and size words of this area. Every function takes the instant `now` and the zone explicitly
 * (spec §0 "Clock"); nothing reads the device clock.
 */
object TimeLabels {
    fun localDate(epochMillis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    private fun zoned(epochMillis: Long, zone: ZoneId): ZonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)

    /** Calendar days from [from] to [to] in [zone] (negative when [to] is earlier). */
    fun daysBetween(from: Long, to: Long, zone: ZoneId): Long =
        ChronoUnit.DAYS.between(localDate(from, zone), localDate(to, zone))

    /**
     * Last-modified time of a 서재 row (LIB-01, `ED`): same day `오늘 HH:mm`; previous day `어제`;
     * 2–6 days `N일 전`; 7+ days `M월 D일`, or `YYYY년 M월 D일` in a different year.
     */
    fun relative(at: Long, now: Long, zone: ZoneId): String {
        val days = daysBetween(at, now, zone)
        val t = zoned(at, zone)
        return when {
            days <= 0L -> "오늘 ${hhmm(t)}"
            days == 1L -> "어제"
            days in 2L..6L -> "${days}일 전"
            t.year != zoned(now, zone).year -> "${t.year}년 ${t.monthValue}월 ${t.dayOfMonth}일"
            else -> "${t.monthValue}월 ${t.dayOfMonth}일"
        }
    }

    /** Day word without a clock (`오늘`, `어제`, `N일 전`, `M월 D일`), e.g. `오늘 지움` in 휴지통. */
    fun dayWord(at: Long, now: Long, zone: ZoneId): String {
        val days = daysBetween(at, now, zone)
        val t = zoned(at, zone)
        return when {
            days <= 0L -> "오늘"
            days == 1L -> "어제"
            days in 2L..6L -> "${days}일 전"
            t.year != zoned(now, zone).year -> "${t.year}년 ${t.monthValue}월 ${t.dayOfMonth}일"
            else -> "${t.monthValue}월 ${t.dayOfMonth}일"
        }
    }

    /** `11월 20일`. */
    fun monthDay(at: Long, zone: ZoneId): String {
        val t = zoned(at, zone)
        return "${t.monthValue}월 ${t.dayOfMonth}일"
    }

    /** `11월 20일 08:12`. */
    fun monthDayTime(at: Long, zone: ZoneId): String = "${monthDay(at, zone)} ${hhmm(zoned(at, zone))}"

    /** `MMDD` (`1122`), used in default export file names. */
    fun mmdd(at: Long, zone: ZoneId): String {
        val t = zoned(at, zone)
        return t.monthValue.toString().padStart(2, '0') + t.dayOfMonth.toString().padStart(2, '0')
    }

    private fun hhmm(t: ZonedDateTime) = t.hour.toString().padStart(2, '0') + ":" + t.minute.toString().padStart(2, '0')

    /**
     * File size (`ED`): `340 MB`, `1.2 MB`, `820 KB`, `2.1 GB`; one decimal below 10 units, binary
     * multiples.
     */
    fun size(bytes: Long): String {
        val units = listOf("B" to 1.0, "KB" to 1024.0, "MB" to 1024.0 * 1024, "GB" to 1024.0 * 1024 * 1024)
        val (name, factor) = units.last { bytes >= it.second || it.first == "B" }
        if (name == "B") return "$bytes B"
        val value = bytes / factor
        return if (value < 10) "${"%.1f".format(java.util.Locale.ROOT, value)} $name" else "${Math.round(value)} $name"
    }
}
