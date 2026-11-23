package io.github.lenticualrstone.civilight.feature.library.common

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.store.EditStamp
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions

/**
 * Writes to a project that is not the current screen's (서재 ⋯ menu, the shared rating sheet, the
 * `같이 바꾸기` list): acquires the shared store from [ProjectSessions], applies one labelled undo
 * step and releases it again, so no screen keeps a project open it does not show.
 */
interface ProjectWriter {
    suspend fun edit(projectKey: Key, label: String, transform: (ProjectDoc, EditStamp) -> ProjectDoc): EditResult

    suspend fun read(projectKey: Key): ProjectDoc

    /**
     * (0.2.0) A write that is no undo step and that undo and redo never revert (core:store
     * `updateWithoutUndo`): what an export writes after its save (`내보낸 판`, fixed voice IDs).
     */
    suspend fun editWithoutUndo(
        projectKey: Key,
        carry: ((step: ProjectDoc, before: ProjectDoc, after: ProjectDoc) -> ProjectDoc)? = null,
        transform: (ProjectDoc, EditStamp) -> ProjectDoc,
    ): EditResult = edit(projectKey, "") { d, s -> transform(d, s) }

    /**
     * A permanent delete (휴지통 `영구 삭제`, `비우기`, the 30-day purge): written as one step, then the
     * project's undo history is cleared, since undoing an earlier step would restore whole-document
     * snapshots that still hold the purged rows. [label] names the step while it is written.
     */
    suspend fun purge(projectKey: Key, label: String, transform: (ProjectDoc) -> ProjectDoc): EditResult
}

internal class SessionProjectWriter(private val sessions: ProjectSessions) : ProjectWriter {
    override suspend fun edit(projectKey: Key, label: String, transform: (ProjectDoc, EditStamp) -> ProjectDoc): EditResult {
        val store = sessions.acquire(projectKey)
        return try {
            if (store.readOnly) {
                EditResult.READ_ONLY
            } else {
                val stamp = store.stamp()
                store.update(label) { transform(it, stamp) }
                EditResult.APPLIED
            }
        } catch (e: ReadOnlyProjectException) {
            EditResult.READ_ONLY
        } finally {
            sessions.release(projectKey)
        }
    }

    override suspend fun editWithoutUndo(
        projectKey: Key,
        carry: ((step: ProjectDoc, before: ProjectDoc, after: ProjectDoc) -> ProjectDoc)?,
        transform: (ProjectDoc, EditStamp) -> ProjectDoc,
    ): EditResult {
        val store = sessions.acquire(projectKey)
        return try {
            if (store.readOnly) {
                EditResult.READ_ONLY
            } else {
                val stamp = store.stamp()
                store.updateWithoutUndo(carry) { transform(it, stamp) }
                EditResult.APPLIED
            }
        } catch (e: ReadOnlyProjectException) {
            EditResult.READ_ONLY
        } finally {
            sessions.release(projectKey)
        }
    }

    override suspend fun purge(projectKey: Key, label: String, transform: (ProjectDoc) -> ProjectDoc): EditResult {
        val store = sessions.acquire(projectKey)
        return try {
            if (store.readOnly) {
                EditResult.READ_ONLY
            } else {
                val before = store.doc.value
                val after = store.update(label) { transform(it) }
                if (after != before) store.clearUndo()
                EditResult.APPLIED
            }
        } catch (e: ReadOnlyProjectException) {
            EditResult.READ_ONLY
        } finally {
            sessions.release(projectKey)
        }
    }

    override suspend fun read(projectKey: Key): ProjectDoc {
        val store = sessions.acquire(projectKey)
        return try {
            store.doc.value
        } finally {
            sessions.release(projectKey)
        }
    }
}
