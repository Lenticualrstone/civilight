package io.github.lenticualrstone.civilight.shell

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.PurgeActions
import io.github.lenticualrstone.civilight.domain.trash.ProjectTrashState
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeResult
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrash
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import kotlinx.coroutines.CancellationException

/** Copy of the start-up purge: the undo label of its one step (the history is cleared right after). */
internal object StartupPurgeCopy {
    const val PURGE = "영구 삭제"
}

/**
 * Start-up maintenance (DECISIONS §1 "Daily purge and start-up maintenance", LIB-39 `ED`), run once
 * per process start off the main thread (no once-per-day guard exists in the stores, so none is
 * added): first `core:store` [StoreContainer.runMaintenance] (whole projects and recorder records
 * whose 30 days are over, blob collection), then `domain:actions` [PurgeActions.runDue] over every
 * live project the user can write (not the example, not read-only), so rows trashed inside a
 * project are purged without opening the 휴지통.
 *
 * The [PurgeActions.runDue] result is applied piece by piece, never as whole snapshots, so an edit
 * made meanwhile is not overwritten: each changed project gets one store step that purges what is
 * due in its current document (the same `PurgeOps.purgeDue` + plain unit labels as `runDue`), then
 * its undo history is cleared (as the 휴지통's `영구 삭제` does); the recorder links into it are
 * updated on the current recorder library. Whole projects or records `runDue` still finds due go
 * through the stores' own paths. Last, `blobsToDelete` is deleted through
 * [StoreContainer.deleteUnreferencedBlobs], which re-checks every project and record first.
 */
internal class StartupPurge(private val stores: StoreContainer, private val sessions: ProjectSessions) {
    suspend fun run() {
        attempt { stores.runMaintenance() }
        attempt { purgeDue() }
    }

    private suspend fun purgeDue() {
        stores.library.ensureLoaded()
        stores.recorder.ensureLoaded()
        val ctx = ActionContext(now = stores.clock.now(), zone = stores.clock.zone, device = stores.deviceId.value)
        val library = stores.library.state.value
        val docs = library.live.filter { !it.isExample && !it.readOnly }.mapNotNull { entry -> attempt { stores.library.readDoc(entry.key) } }
        val recorder = stores.recorder.state.value
        val run = PurgeActions.runDue(
            ctx,
            docs,
            ProjectTrashState(library.index, library.trash.entries),
            RecorderTrashState(recorder.library, recorder.trash.entries),
        )
        for (key in run.changedProjects) attempt { purgeInProject(key, ctx) }
        if (run.purgedProjects.isNotEmpty()) {
            sessions.closeIdle()
            for (key in run.purgedProjects) attempt { stores.purgeProject(key) }
        }
        if (run.recorderLog.isNotEmpty()) {
            attempt {
                stores.recorder.update { s ->
                    val next = RecorderTrash.purgeDue(RecorderTrashState(s.library, s.trash.entries), ctx.trash).first
                    s.copy(library = next.library, trash = s.trash.copy(entries = next.entries))
                }
            }
        }
        stores.deleteUnreferencedBlobs(run.blobsToDelete)
    }

    /** One project's due entries, purged in its current document as one step; then its recorder links. */
    private suspend fun purgeInProject(key: Key, ctx: ActionContext) {
        var done: PurgeResult? = null
        val store = sessions.acquire(key)
        try {
            if (store.readOnly) return
            // `done` is set on every run of the transform, so a re-run never leaves a stale result behind.
            store.update(StartupPurgeCopy.PURGE) { doc -> due(doc, ctx).also { done = it }?.doc ?: doc }
            if (done != null) store.clearUndo()
        } catch (e: ReadOnlyProjectException) {
            return
        } finally {
            sessions.release(key)
        }
        val purge = done ?: return
        stores.recorder.updateLibrary { PurgeActions.applyProjectPurge(it, key, purge, ctx) }
    }

    private fun due(doc: ProjectDoc, ctx: ActionContext): PurgeResult? {
        val result = PurgeOps.purgeDue(doc, ctx.trash)
        if (result.purgedEntries.isEmpty()) return null
        return PurgeActions.withPlainUnitLabels(doc, result)
    }

    /** Runs [block]; a failure is skipped until the next start (cancellation still ends the run). */
    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
