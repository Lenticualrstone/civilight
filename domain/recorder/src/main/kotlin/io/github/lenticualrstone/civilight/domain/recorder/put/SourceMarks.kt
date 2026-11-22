package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderTimeLabels
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import java.time.ZoneId

/** Where a source mark leads when tapped (LRC-32). */
enum class SourceMarkTarget { RECORD_SCREEN, TRASH_RECORDER_FILTER, NONE }

/**
 * Source marks on copies (LRC-32, LRC-39), rendered by the owning areas: `레코더 11월 22일 08:41`
 * (date of the record's `createdAt`); `레코더 · 휴지통` while the record is in the trash;
 * `지워진 기록` after it was permanently deleted. The mark never reveals a hidden record's text.
 */
object SourceMarks {
    fun mark(source: RecorderSource, library: RecorderLibrary, zone: ZoneId): String {
        val record = library.record(source.recordKey) ?: return RecorderCopy.RECORD_PURGED
        if (record.deletedAt != null) return "${RecorderCopy.RECORDER_MARK}${RecorderCopy.SEP}${RecorderCopy.IN_TRASH}"
        return "${RecorderCopy.RECORDER_MARK} ${RecorderTimeLabels.sourceDate(source.recordedAt, zone)}"
    }

    /** The memo line prefix of a script block: `⌁ 작가 메모 · 레코더 11월 22일 08:41`. */
    fun blockMemoPrefix(source: RecorderSource, library: RecorderLibrary, zone: ZoneId): String =
        "${RecorderCopy.MEMO_MARK} ${RecorderCopy.AUTHOR_MEMO}${RecorderCopy.SEP}${mark(source, library, zone)}"

    /** Tap: the record screen (where audio plays); for a trashed record the trash filtered to `레코더` ([PROPOSED]). */
    fun target(source: RecorderSource, library: RecorderLibrary): SourceMarkTarget {
        val record = library.record(source.recordKey) ?: return SourceMarkTarget.NONE
        return if (record.deletedAt != null) SourceMarkTarget.TRASH_RECORDER_FILTER else SourceMarkTarget.RECORD_SCREEN
    }
}
