package io.github.lenticualrstone.civilight.domain.recorder.trash

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderTimeLabels
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A recorder row of the trash (filter `레코더`, LRC-43). */
data class TrashRow(val recordKey: Key, val title: String, val sub: String, val daysLeft: String, val actions: List<String>)

/**
 * Deleting records (LRC-43): `지우기` moves the record to the trash at once (no confirmation: a
 * record has no references to show); its links and placements stay with it; restore brings back
 * links, placements, hidden flag and state; purge erases record, links and placements (copies keep
 * their text and show `지워진 기록`). Deleting a project or a target never trashes records. The
 * trash host itself is `domain:trash` (`RecorderTrash`); these are the same rules for this area's
 * own `⋯ › 지우기`.
 */
object RecordTrash {
    fun delete(library: RecorderLibrary, recordKey: Key, ctx: RecorderEditContext): Triple<RecorderLibrary, TrashEntry, Toast>? {
        val record = library.record(recordKey)?.takeIf { it.deletedAt == null } ?: return null
        val entry = TrashEntry(
            key = ctx.newKey(), kind = TrashKind.RECORDER.open(), projectKey = null, targetKeys = listOf(recordKey),
            hiddenAtDelete = record.hidden, deletedAt = ctx.now, purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        val next = library.copy(records = library.records.map {
            if (it.key == recordKey) it.copy(deletedAt = ctx.now, trashEntryKey = entry.key, updatedAt = ctx.now, updatedBy = ctx.device) else it
        })
        return Triple(next, entry, Toast(RecorderCopy.TODO_DELETED_TOAST, listOf(RecorderCopy.UNDO)))
    }

    /** `되살리기` (also the delete toast's `되돌리기`). A hidden record needs [authenticated]. */
    fun restore(library: RecorderLibrary, entry: TrashEntry, ctx: RecorderEditContext, authenticated: Boolean = false): RecorderLibrary? {
        val keys = entry.targetKeys.toSet()
        val hidden = entry.hiddenAtDelete || library.records.any { it.key in keys && it.hidden }
        if (hidden && !authenticated) return null
        return library.copy(records = library.records.map {
            if (it.key in keys) it.copy(deletedAt = null, trashEntryKey = null, updatedAt = ctx.now, updatedBy = ctx.device) else it
        })
    }

    /** `영구 삭제`, `비우기` or 30 days after `deletedAt`. */
    fun purge(library: RecorderLibrary, entry: TrashEntry): RecorderLibrary {
        val keys = entry.targetKeys.toSet()
        return library.copy(
            records = library.records.filter { it.key !in keys },
            links = library.links.filter { it.recordKey !in keys },
            placements = library.placements.filter { it.recordKey !in keys },
        )
    }

    fun isDue(entry: TrashEntry, today: LocalDate): Boolean = !today.isBefore(entry.purgeOn)

    /**
     * The trash row: `음성 0:12` (text records `글` [PROPOSED]; hidden records `숨긴 기록`) · `라이프 레코더`
     * · `<d>일 남음` with `되살리기` · `영구 삭제`.
     */
    fun row(record: RecorderRecord, entry: TrashEntry, today: LocalDate): TrashRow {
        val title = if (record.hidden || entry.hiddenAtDelete) RecorderCopy.HIDDEN_RECORDS else RecorderTimeLabels.kindLabel(record)
        val days = ChronoUnit.DAYS.between(today, entry.purgeOn).coerceAtLeast(0)
        return TrashRow(record.key, title, RecorderCopy.APP_NAME, "$days${RecorderCopy.DAYS_LEFT}", listOf(RecorderCopy.RESTORE, RecorderCopy.PURGE))
    }
}
