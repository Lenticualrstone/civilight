package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason

/** The project index with the project-level trash rows (filter `프로젝트`). */
data class ProjectTrashState(val library: LibraryIndexFile, val entries: List<TrashEntry>)

/**
 * Whole projects in the trash (LIB-03, LIB-38, §5.2 last row): one entry per project, everything in
 * it stops being live with it. The project's own files stay in its store until the purge.
 */
object ProjectTrash {
    /**
     * Moves a project to the trash ([reason] `replacedByOverwrite` for LIB-52 `덮어쓰기`). Example
     * projects are never trashed (onboarding purges them directly).
     */
    fun delete(state: ProjectTrashState, projectKey: Key, ctx: TrashContext, reason: TrashReason = TrashReason.DELETED): ProjectTrashState {
        val row = state.library.projects.firstOrNull { it.key == projectKey } ?: return state
        if (row.deletedAt != null || row.isExample) return state
        val entry = TrashEntry(
            key = ctx.newKey(), kind = TrashKind.PROJECT.open(), projectKey = projectKey, targetKeys = listOf(projectKey),
            reason = reason.open(), hiddenAtDelete = row.hidden, deletedAt = ctx.now, purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        val projects = state.library.projects.map { if (it.key == projectKey) it.copy(deletedAt = ctx.now, trashEntryKey = entry.key, updatedAt = ctx.now) else it }
        return ProjectTrashState(state.library.copy(projects = projects), state.entries + entry)
    }

    /** `되살리기`; a hidden project needs [authenticated]. */
    fun restore(state: ProjectTrashState, entryKey: Key, ctx: TrashContext, authenticated: Boolean = false): ProjectTrashState? {
        val entry = state.entries.firstOrNull { it.key == entryKey } ?: return null
        if (entry.hiddenAtDelete && !authenticated) return null
        val projects = state.library.projects.map {
            if (it.key in entry.targetKeys) it.copy(deletedAt = null, trashEntryKey = null, updatedAt = ctx.now) else it
        }
        return ProjectTrashState(state.library.copy(projects = projects), state.entries.filter { it.key != entryKey })
    }

    /** `영구 삭제`: the project row and its entry go; returns the keys whose stores the caller deletes. */
    fun purge(state: ProjectTrashState, entryKey: Key): Pair<ProjectTrashState, List<Key>> {
        val entry = state.entries.firstOrNull { it.key == entryKey } ?: return state to emptyList()
        val keys = entry.targetKeys
        return ProjectTrashState(
            state.library.copy(projects = state.library.projects.filter { it.key !in keys }),
            state.entries.filter { it.key != entryKey },
        ) to keys
    }

    /** The daily purge of project entries. */
    fun purgeDue(state: ProjectTrashState, ctx: TrashContext): Pair<ProjectTrashState, List<Key>> {
        var current = state
        val gone = mutableListOf<Key>()
        for (entry in state.entries.filter(ctx::isDue)) {
            val (next, keys) = purge(current, entry.key)
            current = next
            gone += keys
        }
        return current to gone
    }
}
