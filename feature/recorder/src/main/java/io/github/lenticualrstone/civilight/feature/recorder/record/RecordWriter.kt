package io.github.lenticualrstone.civilight.feature.recorder.record

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.RecorderActions
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.hide.RecordHiding
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordMenu
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.Insertion
import io.github.lenticualrstone.civilight.domain.recorder.put.InsertionException
import io.github.lenticualrstone.civilight.domain.recorder.put.InsertionUndo
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptSpot
import io.github.lenticualrstone.civilight.domain.recorder.inbox.RecordScreens
import io.github.lenticualrstone.civilight.domain.recorder.link.DeletedTargets
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.feature.recorder.capture.ProjectPort
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderPort
import io.github.lenticualrstone.civilight.feature.recorder.env.EnvSource
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A 넣기 that went through, with what its toast's `되돌리기` needs. */
data class PutDone(val projectKey: Key, val insertion: Insertion) {
    val toast: Toast get() = insertion.toast
}

/** `지우기` done: the trash entry the toast's `되돌리기` restores. */
data class DeleteDone(val recordKey: Key, val entryKey: Key, val toast: Toast)

/**
 * Every write on a saved record outside the capture (기록함, ⋯ menu, 넣기 sheets), through the
 * binding paths of DECISIONS §1: `domain:recorder` reducers for marks, hiding and links,
 * `domain:actions` `RecorderActions` for 넣기 and the trash. Each write reads the library as it is
 * at that moment ([RecorderPort.update]), so two writes never overwrite each other.
 */
class RecordWriter(
    private val recorder: RecorderPort,
    private val projects: ProjectPort,
    private val envSource: EnvSource,
    private val context: () -> ActionContext,
) {
    private val _projectDocs = MutableSharedFlow<ProjectDoc>(extraBufferCapacity = 16)

    /**
     * Every project document this writer changed, as written. Screens patch their env with it
     * (`RecorderEnv.withDoc`) before the library changes, so a new placement is never judged
     * against the document from before the write (it would read `정리 안 됨` until a reload).
     */
    val projectDocs: SharedFlow<ProjectDoc> = _projectDocs.asSharedFlow()

    // ---- 끝냄 (LRC-26) ----

    /** `끝냄으로 표시`, only for a live record in `정리 안 됨`; the toast (TODO_COPY_LRC_11 · `되돌리기`), or null when not offered. */
    suspend fun markDone(recordKey: Key, env: RecorderEnv): Toast? {
        val ctx = context().recorder
        var toast: Toast? = null
        recorder.update { lib ->
            val record = lib.record(recordKey)
            if (record == null || record.deletedAt != null || RecordStates.state(lib, record, env) != RecordState.UNSORTED) {
                lib
            } else {
                val (next, t) = RecordMenu.markDone(lib, recordKey, env, ctx)
                toast = t
                next
            }
        }
        return toast
    }

    /** The `끝냄` toast's `되돌리기`. */
    suspend fun undoMarkDone(recordKey: Key) {
        val ctx = context().recorder
        recorder.update { RecordMenu.undoMarkDone(it, recordKey, ctx) }
    }

    /** `다시 정리 안 됨`, only in `끝냄`. */
    suspend fun unmarkDone(recordKey: Key, env: RecorderEnv) {
        val ctx = context().recorder
        recorder.update { lib ->
            val record = lib.record(recordKey)
            if (record != null && RecordStates.state(lib, record, env) == RecordState.DONE) RecordMenu.unmarkDone(lib, recordKey, ctx) else lib
        }
    }

    // ---- hiding (LRC-40, LRC-41) ----

    /** `⋯ › 숨기기`: at once, no authentication. */
    suspend fun hide(recordKey: Key) {
        val ctx = context().recorder
        recorder.update { RecordHiding.hide(it, recordKey, ctx) }
    }

    /** `숨김 풀기` (offered only while hidden records are open). */
    suspend fun unhide(recordKey: Key) {
        val ctx = context().recorder
        recorder.update { RecordHiding.unhide(it, recordKey, ctx) }
    }

    // ---- trash (LRC-43) ----

    /** `⋯ › 지우기`: to the trash at once (one `recorder` entry); the toast is TODO_COPY_LRC_19 · `되돌리기`. */
    suspend fun delete(recordKey: Key): DeleteDone? {
        val ctx = context()
        val record = recorder.current().record(recordKey) ?: return null
        if (record.deletedAt != null) return null
        recorder.updateTrash { RecorderActions.delete(it, recordKey, ctx) }
        val entry = recorder.current().record(recordKey)?.trashEntryKey ?: return null
        return DeleteDone(recordKey, entry, Toast(RecorderCopy.TODO_DELETED_TOAST, listOf(RecorderCopy.UNDO)))
    }

    /** The `지우기` toast's `되돌리기`: back with its links, placements, hidden flag and state (the user's own delete, so no prompt). */
    suspend fun undoDelete(done: DeleteDone) {
        val ctx = context()
        recorder.updateTrash { RecorderActions.restore(it, done.entryKey, ctx, authenticated = true) ?: it }
    }

    // ---- links (LRC-18, LRC-20) ----

    /** `+ 연결` on a saved record: one link of origin `직접` (a second add of the same target is a no-op). */
    suspend fun addLink(recordKey: Key, target: TargetRef) {
        val ctx = context().recorder
        recorder.update { LinkEdits.add(it, recordKey, target, LinkOrigin.MANUAL, ctx) }
    }

    /** A chip tapped to choose another target: its links go, the first one's place takes [target]. */
    suspend fun replaceLinks(recordKey: Key, linkKeys: List<Key>, target: TargetRef) {
        if (linkKeys.isEmpty()) return addLink(recordKey, target)
        val ctx = context().recorder
        recorder.update { lib ->
            val rest = linkKeys.drop(1).fold(lib) { acc, k -> LinkEdits.remove(acc, recordKey, k) }
            LinkEdits.replace(rest, recordKey, linkKeys.first(), target, ctx)
        }
    }

    /** A chip's × (every link of a collapsed masked chip). */
    suspend fun removeLinks(recordKey: Key, linkKeys: List<Key>) {
        recorder.update { lib -> linkKeys.fold(lib) { acc, k -> LinkEdits.remove(acc, recordKey, k) } }
    }

    // ---- text (LRC-28) ----

    /** A committed edit of the record text (autosave): `@` links follow their runs; the revision grows when the text changed. */
    suspend fun editText(recordKey: Key, runs: List<TextRun>) {
        val ctx = context().recorder
        recorder.update { RecordScreens.editText(it, recordKey, runs, ctx) }
    }

    // ---- deleted targets (LRC-23) ----

    /** `글로 풀기`: the runs to [target] become plain text with the same characters; the `@` link goes. */
    suspend fun toPlainText(recordKey: Key, target: TargetRef) {
        val ctx = context().recorder
        recorder.update { DeletedTargets.toPlainText(it, recordKey, target, ctx) }
    }

    /** `다른 항목에 잇기` on an `@` chip: the runs to [from] point at [to]; links follow. */
    suspend fun relinkRuns(recordKey: Key, from: TargetRef, to: TargetRef) {
        val ctx = context().recorder
        recorder.update { DeletedTargets.relinkRuns(it, recordKey, from, to, ctx) }
    }

    /**
     * `되살리기` of a deleted card from its chip (LRC-23, rule 20): a character through
     * `CharacterActions.restore`, a place, term or faction through `RestoreOps` (DECISIONS §1), as one
     * undo step in its project. Returns the restore toast's text, or null when nothing came back
     * (read-only project, not in the trash, hidden without [authenticated]).
     */
    suspend fun restoreTarget(target: TargetRef, authenticated: Boolean): String? {
        val ctx = context()
        val doc = projects.edit(target.projectKey, RecorderCopy.RESTORE) { doc ->
            val restored = TargetRestore.restore(doc, target, ctx, authenticated)
            if (restored == null) doc to null else restored.first to restored
        } ?: return null
        _projectDocs.emit(doc.first)
        envSource.forget(target.projectKey)
        return doc.second
    }

    // ---- 넣기 (LRC-32 … LRC-38) ----

    /**
     * `원고에 넣기` at [spot] of project [projectKey]: one `⌁ 작가 메모` block (one undo step in the
     * project), then the placement, an added link and the cleared `끝냄` mark in the library. Null
     * when the project is read-only or gone, or the record or spot does not qualify.
     */
    suspend fun putInManuscript(recordKey: Key, projectKey: Key, spot: ManuscriptSpot): PutDone? =
        put(projectKey, RecorderCopy.PUT_MANUSCRIPT) { doc, ctx -> RecorderActions.putInManuscript(doc, recorder.current(), recordKey, spot, ctx) }

    /** `카드에 넣기` (LRC-35): a `⌁ 작가 메모` paragraph at the end of [tabId] of [card]; toast TODO_COPY_LRC_13 · `되돌리기`. */
    suspend fun putInCard(recordKey: Key, projectKey: Key, card: TargetRef, tabId: String): PutDone? =
        put(projectKey, RecorderCopy.PUT_CARD) { doc, ctx -> RecorderActions.putInCard(doc, recorder.current(), recordKey, card, tabId, ctx) }

    /** `페이지로 만들기` (LRC-36): one new page named [name] (blank = the prefill); toast TODO_COPY_LRC_15 · `되돌리기`. */
    suspend fun makePage(recordKey: Key, projectKey: Key, name: String): PutDone? =
        put(projectKey, RecorderCopy.MAKE_PAGE) { doc, ctx -> RecorderActions.makePage(doc, recorder.current(), recordKey, name, ctx) }

    /**
     * `⋯ › 메모 칸에 넣기` (LRC-37): the record text as one line at the end of the scene's `씬 메모`, or
     * of a chapter's or episode's description line [unitKey]; toast TODO_COPY_LRC_16 · `되돌리기`.
     */
    suspend fun putInMemoField(recordKey: Key, projectKey: Key, unitKey: Key): PutDone? =
        put(projectKey, RecorderCopy.PUT_MEMO_FIELD) { doc, ctx -> PendingRecorderActions.putInMemoField(doc, recorder.current(), recordKey, unitKey, ctx) }

    /**
     * `⋯ › 인물로 만들기` (LRC-38): a new character [name] with `한 줄 소개` = the first sentence and a
     * `⌁ 작가 메모` paragraph at the end of `개요`; toast TODO_COPY_LRC_17 · `되돌리기`.
     */
    suspend fun makeCharacter(recordKey: Key, projectKey: Key, name: String): PutDone? =
        put(projectKey, RecorderCopy.MAKE_CHARACTER) { doc, ctx -> PendingRecorderActions.makeCharacter(doc, recorder.current(), recordKey, name, ctx) }

    /** One 넣기: the content as one undo step in the project, then the library side on the library as it is now. */
    private suspend fun put(projectKey: Key, label: String, insert: (ProjectDoc, ActionContext) -> Insertion): PutDone? {
        val ctx = context()
        val insertion = try {
            projects.edit(projectKey, label) { doc ->
                val ins = insert(doc, ctx)
                ins.doc to ins
            }
        } catch (e: InsertionException) {
            null
        } ?: return null
        _projectDocs.emit(insertion.doc)
        recorder.update { Insertions.apply(it, insertion) }
        envSource.forget(projectKey)
        return PutDone(projectKey, insertion)
    }

    /** A 넣기 toast's `되돌리기`: the created content (no trash entry), the placement and an added link go; the earlier state returns. */
    suspend fun undoPut(done: PutDone) {
        val undo = done.insertion.undo
        val doc = projects.edit(done.projectKey, RecorderCopy.UNDO) { doc ->
            val next = RecorderActions.undoPut(doc, recorder.current(), undo).first
            next to next
        } ?: return
        _projectDocs.emit(doc)
        recorder.update { Insertions.undoLibrary(doc, it, undo) }
        envSource.forget(done.projectKey)
    }
}

/** The library side of a 넣기, re-applied on the library as it is now (the insertion was computed on the same one). */
object Insertions {
    fun apply(library: RecorderLibrary, insertion: Insertion): RecorderLibrary {
        val undo = insertion.undo
        if (library.placements.any { it.key == insertion.placement.key }) return library
        val added = undo.addedLinkKey?.let { k -> insertion.library.links.firstOrNull { it.key == k } }
        val stamped = insertion.library.record(undo.recordKey)
        return library.copy(
            records = library.records.map {
                if (it.key == undo.recordKey && it.doneMarked) it.copy(doneMarked = false, updatedAt = stamped?.updatedAt ?: it.updatedAt, updatedBy = stamped?.updatedBy ?: it.updatedBy) else it
            },
            links = library.links + listOfNotNull(added?.takeIf { a -> library.links.none { it.key == a.key } }),
            placements = library.placements + insertion.placement,
        )
    }

    fun undoLibrary(doc: ProjectDoc, library: RecorderLibrary, undo: InsertionUndo): RecorderLibrary =
        RecorderActions.undoPut(doc, library, undo).second
}
