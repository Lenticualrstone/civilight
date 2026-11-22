package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Date, duration and timer labels of the recorder (LRC-07, LRC-25, LRC-32). */
object RecorderTimeLabels {
    /**
     * Row meta: same local day `오늘 HH:mm`, previous day `어제 HH:mm`, earlier in the same year
     * `M월 d일`, another year `yyyy년 M월 d일` (`ED` for TODO_COPY_LRC_10).
     */
    fun meta(createdAt: Timestamp, now: Timestamp, zone: ZoneId): String {
        val at = local(createdAt, zone)
        val today = local(now, zone).toLocalDate()
        val day = at.toLocalDate()
        val clock = "%02d:%02d".format(at.hour, at.minute)
        return when {
            day == today -> "오늘 $clock"
            day == today.minusDays(1) -> "어제 $clock"
            day.year == today.year -> "${day.monthValue}월 ${day.dayOfMonth}일"
            else -> "${day.year}년 ${day.monthValue}월 ${day.dayOfMonth}일"
        }
    }

    /** Day only (`오늘`, `어제`, `11월 18일`): the start-material rows (LIB-12 `음성 0:24 · 오늘`). */
    fun day(createdAt: Timestamp, now: Timestamp, zone: ZoneId): String {
        val day = local(createdAt, zone).toLocalDate()
        val today = local(now, zone).toLocalDate()
        return when {
            day == today -> "오늘"
            day == today.minusDays(1) -> "어제"
            day.year == today.year -> "${day.monthValue}월 ${day.dayOfMonth}일"
            else -> "${day.year}년 ${day.monthValue}월 ${day.dayOfMonth}일"
        }
    }

    /** `0:24`, `1:02`; `1:00:05` from one hour ([PROPOSED]). Seconds are truncated. */
    fun duration(ms: Long): String {
        val total = (ms.coerceAtLeast(0)) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** The recording timer `00:12` (`mm:ss`); `h:mm:ss` from one hour (`ED`). */
    fun timer(ms: Long): String {
        val total = (ms.coerceAtLeast(0)) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** `음성 0:24` for a voice record (just `음성` without a duration), `글` for a text record. */
    fun kindLabel(record: RecorderRecord): String = when (record.kind) {
        RecordKind.VOICE.open() -> record.durationMs?.let { "${RecorderCopy.VOICE} ${duration(it)}" } ?: RecorderCopy.VOICE
        else -> RecorderCopy.TEXT
    }

    /** `11월 22일 08:41`: the date of a source mark (`레코더 11월 22일 08:41`). */
    fun sourceDate(createdAt: Timestamp, zone: ZoneId): String {
        val at = local(createdAt, zone)
        return "${at.monthValue}월 ${at.dayOfMonth}일 %02d:%02d".format(at.hour, at.minute)
    }

    private fun local(ms: Timestamp, zone: ZoneId): ZonedDateTime = Instant.ofEpochMilli(ms).atZone(zone)
}
