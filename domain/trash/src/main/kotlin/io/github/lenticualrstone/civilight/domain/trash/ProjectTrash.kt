package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
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

    /**
     * LIB-52 `덮어쓰기` (§6, `ED`): the existing project row is re-keyed to [newKey] (K′) and moved to
     * the trash with `reason = replacedByOverwrite`, so the imported file can take the old key K.
     * The caller re-keys the stored project with [rekey]. Restoring the replaced copy later gives
     * a separate project K′.
     */
    fun replaceByOverwrite(state: ProjectTrashState, projectKey: Key, newKey: Key, ctx: TrashContext): ProjectTrashState {
        val row = state.library.projects.firstOrNull { it.key == projectKey } ?: return state
        if (row.isExample || state.library.projects.any { it.key == newKey }) return state
        val rekeyed = state.copy(
            library = state.library.copy(projects = state.library.projects.map { if (it.key == projectKey) it.copy(key = newKey) else it }),
            entries = state.entries.map { if (it.projectKey == projectKey) it.copy(projectKey = newKey, targetKeys = it.targetKeys.map { k -> if (k == projectKey) newKey else k }) else it },
        )
        if (row.deletedAt != null) return rekeyed // already in the trash (§6 second ED row): only the key changes
        return delete(rekeyed, newKey, ctx, TrashReason.REPLACED_BY_OVERWRITE)
    }

    /**
     * The project content under a new portable key (§6): `Project.key`, the numbering-unit file of a
     * one-level structure (keyed by the project), and the project key on its trash rows. Item keys
     * are unchanged.
     */
    fun rekey(doc: ProjectDoc, newKey: Key): ProjectDoc {
        val old = doc.key
        if (old == newKey) return doc
        val episodes = doc.episodes.entries.associate { (k, file) ->
            if (k == old) newKey to file.copy(unitKey = newKey) else k to file
        }
        return doc.copy(
            project = doc.project.copy(key = newKey),
            episodes = episodes,
            trash = doc.trash.copy(entries = doc.trash.entries.map { if (it.projectKey == old) it.copy(projectKey = newKey) else it }),
        )
    }

    /** A hidden project (now or when it was deleted) needs authentication before `되살리기` (LIB-38). */
    fun needsAuth(state: ProjectTrashState, entry: TrashEntry): Boolean =
        entry.hiddenAtDelete || entry.targetKeys.any { k -> state.library.projects.any { it.key == k && it.hidden } }

    /** `되살리기`; a hidden project needs [authenticated]. */
    fun restore(state: ProjectTrashState, entryKey: Key, ctx: TrashContext, authenticated: Boolean = false): ProjectTrashState? {
        val entry = state.entries.firstOrNull { it.key == entryKey } ?: return null
        if (needsAuth(state, entry) && !authenticated) return null
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

    /**
     * `비우기` for project rows (LIB-39 `ED`): every project entry, except hidden ones while the
     * hidden group is locked.
     */
    fun emptyTrash(state: ProjectTrashState, hiddenUnlocked: Boolean): Pair<ProjectTrashState, List<Key>> {
        var current = state
        val gone = mutableListOf<Key>()
        for (entry in state.entries.filter { hiddenUnlocked || !needsAuth(state, it) }) {
            val (next, keys) = purge(current, entry.key)
            current = next
            gone += keys
        }
        return current to gone
    }
}
