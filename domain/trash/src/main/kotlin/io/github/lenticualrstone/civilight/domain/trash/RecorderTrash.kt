package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind

/** The recorder library with its trash rows (`projectKey` null, filter `레코더`). */
data class RecorderTrashState(val library: RecorderLibrary, val entries: List<TrashEntry>)

/**
 * Recorder side of rule 20 and LRC-43: trashing, restoring and purging records (links and
 * placements are owned by the record and never go to the trash on their own), and marking links
 * and placements whose project target was purged.
 */
object RecorderTrash {
    /** `지우기` on a record: direct mark + one `recorder` entry; its links and placements stop being live with it. */
    fun delete(state: RecorderTrashState, recordKey: Key, ctx: TrashContext): RecorderTrashState {
        val record = state.library.record(recordKey) ?: return state
        if (record.deletedAt != null) return state
        val entry = TrashEntry(
            key = ctx.newKey(), kind = TrashKind.RECORDER.open(), projectKey = null, targetKeys = listOf(recordKey),
            hiddenAtDelete = record.hidden, deletedAt = ctx.now, purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        val records = state.library.records.map {
            if (it.key == recordKey) it.copy(deletedAt = ctx.now, trashEntryKey = entry.key, updatedAt = ctx.now, updatedBy = ctx.deviceId) else it
        }
        return RecorderTrashState(state.library.copy(records = records), state.entries + entry)
    }

    /** `되살리기`: the record and with it its links and placements come back. Hidden records need [authenticated]. */
    fun restore(state: RecorderTrashState, entryKey: Key, ctx: TrashContext, authenticated: Boolean = false): RecorderTrashState? {
        val entry = state.entries.firstOrNull { it.key == entryKey } ?: return null
        val hidden = entry.hiddenAtDelete || entry.targetKeys.any { state.library.record(it)?.hidden == true }
        if (hidden && !authenticated) return null
        val targets = entry.targetKeys.toSet()
        val records = state.library.records.map {
            if (it.key in targets) it.copy(deletedAt = null, trashEntryKey = null, updatedAt = ctx.now, updatedBy = ctx.deviceId) else it
        }
        return RecorderTrashState(state.library.copy(records = records), state.entries.filter { it.key != entryKey })
    }

    /**
     * `영구 삭제` of a record: the record, its links and placements are removed; copies in projects
     * keep their text (their mark becomes `지워진 기록`, derived). Returns the log rows to keep 30 days.
     */
    fun purge(state: RecorderTrashState, entryKey: Key, ctx: TrashContext): Pair<RecorderTrashState, List<DeletionLogEntry>> {
        val entry = state.entries.firstOrNull { it.key == entryKey } ?: return state to emptyList()
        val targets = entry.targetKeys.toSet()
        val library = state.library.copy(
            records = state.library.records.filter { it.key !in targets },
            links = state.library.links.filter { it.recordKey !in targets },
            placements = state.library.placements.filter { it.recordKey !in targets },
        )
        return RecorderTrashState(library, state.entries.filter { it.key != entryKey }) to
            targets.map { DeletionLogEntry(it, "recorder", ctx.now) }
    }

    /** `비우기` for recorder rows (LIB-39 `ED`): hidden records stay while their gate is locked. */
    fun emptyTrash(state: RecorderTrashState, ctx: TrashContext, recordsUnlocked: Boolean): Pair<RecorderTrashState, List<DeletionLogEntry>> {
        var current = state
        val log = mutableListOf<DeletionLogEntry>()
        for (entry in state.entries) {
            val hidden = entry.hiddenAtDelete || entry.targetKeys.any { state.library.record(it)?.hidden == true }
            if (hidden && !recordsUnlocked) continue
            val (next, rows) = purge(current, entry.key, ctx)
            current = next
            log += rows
        }
        return current to log
    }

    /** The daily purge of recorder entries (T-07, T-08). */
    fun purgeDue(state: RecorderTrashState, ctx: TrashContext): Pair<RecorderTrashState, List<DeletionLogEntry>> {
        var current = state
        val log = mutableListOf<DeletionLogEntry>()
        for (entry in state.entries.filter(ctx::isDue)) {
            val (next, rows) = purge(current, entry.key, ctx)
            current = next
            log += rows
        }
        return current to log
    }

    /**
     * After a project purge (§5.3): recorder links to the purged targets get `purgedAt` and keep
     * `lastLabel` (hidden targets never print their name: `lastLabelHidden`); placements whose
     * target or container row was removed get `purgedAt`.
     */
    fun applyProjectPurge(library: RecorderLibrary, projectKey: Key, result: PurgeResult, ctx: TrashContext): RecorderLibrary {
        val targets = result.purgedTargets.associateBy { it.key }
        val links = library.links.map { link ->
            val target = targets[link.targetKey]
            if (link.projectKey != projectKey || target == null || link.targetKind.known != target.kind.recorderTargetKind || link.purgedAt != null) {
                link
            } else {
                link.copy(
                    purgedAt = ctx.now, lastLabel = target.label, lastLabelHidden = target.hidden,
                    updatedAt = ctx.now, updatedBy = ctx.deviceId,
                )
            }
        }
        val placements = library.placements.map { p ->
            val gone = p.targetKey in result.removedKeys || p.containerKey in result.removedKeys
            if (p.projectKey != projectKey || !gone || p.purgedAt != null) p else p.copy(purgedAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.deviceId)
        }
        return library.copy(links = links, placements = placements)
    }

    /** After a whole project is purged: every link and placement into it gets `purgedAt`, links keep the project name. */
    fun applyProjectGone(library: RecorderLibrary, projectKey: Key, projectName: String, hidden: Boolean, ctx: TrashContext): RecorderLibrary =
        library.copy(
            links = library.links.map {
                if (it.projectKey != projectKey || it.purgedAt != null) it else it.copy(
                    purgedAt = ctx.now, lastLabel = it.lastLabel ?: projectName, lastLabelHidden = it.lastLabelHidden || hidden,
                    updatedAt = ctx.now, updatedBy = ctx.deviceId,
                )
            },
            placements = library.placements.map {
                if (it.projectKey != projectKey || it.purgedAt != null) it else it.copy(purgedAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.deviceId)
            },
        )
}
