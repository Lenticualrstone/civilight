package io.github.lenticualrstone.civilight.feature.library.trash

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.RecorderActions
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrash
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.SceneChoice
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.ui.project.EditResult

/** What a trash action came to, for the screen. */
sealed interface TrashOutcome {
    data object Done : TrashOutcome

    /** Authentication was cancelled or failed: nothing changed, no toast (LIB-37). */
    data object Cancelled : TrashOutcome

    /** The row is gone or nothing could be done (read-only, already restored elsewhere). */
    data object NothingDone : TrashOutcome

    /** A block whose scene is in the trash: ask `씬도 되살리기` · `다른 씬 끝에` (T-14). */
    data class NeedsSceneChoice(val projectKey: Key, val entryKey: Key, val blockKeys: List<Key>) : TrashOutcome
}

/** The stores and the prompt a trash action touches (fakes in tests). */
interface TrashPorts {
    /** The rule-19 prompt; true only on success. */
    suspend fun authenticate(): Boolean

    /** A whole project back from the trash (`LibraryStore.restore`). */
    suspend fun restoreProject(projectKey: Key)

    /** A whole project deleted for good (`StoreContainer.purgeProject`: index, recorder links, recents, blobs). */
    suspend fun purgeProject(projectKey: Key)

    /** One labelled undo step on [projectKey]. */
    suspend fun editProject(projectKey: Key, label: String, transform: (ProjectDoc) -> ProjectDoc): EditResult

    /**
     * A permanent delete inside [projectKey]: written, then the project's undo history is cleared,
     * because undoing an earlier step would bring purged rows back from its snapshot.
     */
    suspend fun purgeInProject(projectKey: Key, transform: (ProjectDoc) -> ProjectDoc): EditResult

    /** The recorder library and its trash rows. */
    suspend fun updateRecorder(transform: (RecorderTrashState) -> RecorderTrashState)

    /** The recorder library alone (links of purged items keep their label). */
    suspend fun updateRecorderLibrary(transform: (RecorderLibrary) -> RecorderLibrary)

    /** Blob files nothing references any more (after an item purge). */
    suspend fun collectBlobs()

    /** A plain toast (no action). */
    fun toast(text: String)

    fun context(): ActionContext
}

/**
 * Every write of the 휴지통 (LIB-38, LIB-39), one rule for all sources: a row that needs
 * authentication asks once before anything is written ([TrashRowUi.row] `restoreNeedsAuth` for
 * `되살리기`; a masked row for `영구 삭제`), and a cancelled prompt changes nothing.
 */
class TrashActions(private val ports: TrashPorts) {
    /** Entries authenticated in this flow, so a follow-up (`씬도 되살리기`) does not ask again. */
    private val authorized = HashSet<Key>()

    private suspend fun gate(row: TrashRowUi, needsAuth: Boolean): Boolean {
        if (!needsAuth || row.key in authorized) return true
        if (!ports.authenticate()) return false
        authorized += row.key
        return true
    }

    /** The flow of [entryKey] ended without a write (the scene choice was dismissed): ask again next time. */
    fun forget(entryKey: Key) {
        authorized -= entryKey
    }

    /** The lock state changed (a re-lock, another unlock): every authorization of this screen ends. */
    fun forgetAll() {
        authorized.clear()
    }

    /** `되살리기` of a whole row. */
    suspend fun restore(row: TrashRowUi): TrashOutcome {
        if (!gate(row, row.row.restoreNeedsAuth)) return TrashOutcome.Cancelled
        return when (row.source) {
            TrashSource.PROJECT -> {
                val key = row.projectKey ?: return TrashOutcome.NothingDone
                ports.restoreProject(key)
                // The name only when the row showed it; a masked row stays unnamed (R19 §3).
                if (!row.row.masked) ports.toast(TrashScreenCopy.restored(row.row.title))
                TrashOutcome.Done
            }
            TrashSource.RECORDER -> {
                var restored = false
                ports.updateRecorder { state ->
                    RecorderActions.restore(state, row.key, ports.context(), authenticated = true)?.also { restored = true } ?: state
                }
                if (restored) TrashOutcome.Done else TrashOutcome.NothingDone
            }
            TrashSource.ITEM -> restoreInProject(row, null) { doc, ctx -> TrashDecisions.restoreItem(doc, row.key, ctx, authenticated = true) }
        }
    }

    /** One block of an expanded block group. */
    suspend fun restoreBlock(row: TrashRowUi, blockKey: Key): TrashOutcome {
        if (!gate(row, row.row.restoreNeedsAuth)) return TrashOutcome.Cancelled
        return restoreInProject(row, listOf(blockKey)) { doc, ctx -> TrashDecisions.restoreBlocks(doc, row.key, listOf(blockKey), ctx, authenticated = true) }
    }

    /** The answer to [TrashOutcome.NeedsSceneChoice]. */
    suspend fun restoreBlocksWith(row: TrashRowUi, blockKeys: List<Key>, choice: SceneChoice): TrashOutcome {
        if (!gate(row, row.row.restoreNeedsAuth)) return TrashOutcome.Cancelled
        return restoreInProject(row, blockKeys) { doc, ctx -> TrashDecisions.restoreBlocks(doc, row.key, blockKeys, ctx, authenticated = true, sceneChoice = choice) }
    }

    private suspend fun restoreInProject(
        row: TrashRowUi,
        blockKeys: List<Key>?,
        run: (ProjectDoc, ActionContext) -> ItemRestore,
    ): TrashOutcome {
        val projectKey = row.projectKey ?: return TrashOutcome.NothingDone
        val ctx = ports.context()
        var result: ItemRestore? = null
        val written = ports.editProject(projectKey, TrashScreenCopy.RESTORE) { doc ->
            val r = run(doc, ctx)
            result = r
            (r.outcome as? RestoreOutcome.Restored)?.doc ?: doc
        }
        val r = result ?: return TrashOutcome.NothingDone
        if (written == EditResult.READ_ONLY) return TrashOutcome.NothingDone
        return when (val outcome = r.outcome) {
            is RestoreOutcome.Restored -> {
                // A masked row's toast would name what the lock hides (R19 §3, §6): none then.
                if (!row.row.masked) r.toast?.let(ports::toast)
                authorized -= row.key
                TrashOutcome.Done
            }
            is RestoreOutcome.NeedsSceneChoice -> TrashOutcome.NeedsSceneChoice(projectKey, row.key, blockKeys ?: outcome.blockKeys)
            RestoreOutcome.NeedsAuth -> TrashOutcome.Cancelled
            RestoreOutcome.NotFound -> {
                // Refused with a reason (a tag whose name a live tag has): the tap says why (CST-75).
                r.refused?.let(ports::toast)
                TrashOutcome.NothingDone
            }
        }
    }

    /** `영구 삭제` of one row (after the confirm). A masked row asks first, as its restore would. */
    suspend fun purge(row: TrashRowUi): TrashOutcome {
        if (!gate(row, row.row.masked)) return TrashOutcome.Cancelled
        when (row.source) {
            TrashSource.PROJECT -> ports.purgeProject(row.projectKey ?: return TrashOutcome.NothingDone)
            TrashSource.RECORDER -> ports.updateRecorder { RecorderActions.purge(it, row.key, ports.context()).first }
            TrashSource.ITEM -> purgeInProject(row.projectKey ?: return TrashOutcome.NothingDone, listOf(row.key))
        }
        authorized -= row.key
        return TrashOutcome.Done
    }

    /**
     * `비우기` (after the confirm): every row of every filter except the ones locked now (masked),
     * which stay (LIB-39 `ED`). No prompt: nothing locked is touched.
     */
    suspend fun emptyTrash(rows: List<TrashRowUi>): TrashOutcome {
        val open = rows.filter { !it.row.masked }
        if (open.isEmpty()) return TrashOutcome.NothingDone
        open.filter { it.source == TrashSource.PROJECT }.mapNotNull { it.projectKey }.forEach { ports.purgeProject(it) }
        val records = open.filter { it.source == TrashSource.RECORDER }.map { it.key }.toSet()
        if (records.isNotEmpty()) {
            ports.updateRecorder { state ->
                var s = state
                for (key in records) s = RecorderTrash.purge(s, key, ports.context().trash).first
                s
            }
        }
        open.filter { it.source == TrashSource.ITEM }.groupBy { it.projectKey }.forEach { (projectKey, items) ->
            if (projectKey != null) purgeInProject(projectKey, items.map { it.key }, collect = false)
        }
        ports.collectBlobs()
        return TrashOutcome.Done
    }

    /** The 30-day purge of one project's entries (T-08), run when the 휴지통 opens. */
    suspend fun purgeDue(doc: ProjectDoc) {
        val due = TrashDecisions.dueEntries(doc, ports.context())
        if (due.isNotEmpty()) purgeInProject(doc.key, due)
    }

    /** The 30-day purge of recorder records (T-07, T-08). */
    suspend fun purgeDueRecords() {
        ports.updateRecorder { RecorderTrash.purgeDue(it, ports.context().trash).first }
    }

    private suspend fun purgeInProject(projectKey: Key, entryKeys: List<Key>, collect: Boolean = true) {
        val ctx = ports.context()
        var purge: ItemPurge? = null
        ports.purgeInProject(projectKey) { doc -> TrashDecisions.purge(doc, entryKeys, ctx).also { purge = it }.doc }
        val done = purge?.takeIf { it.purged > 0 } ?: return
        ports.updateRecorderLibrary { done.applyTo(it, ctx) }
        if (collect) ports.collectBlobs()
    }
}
