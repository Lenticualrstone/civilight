package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.CardInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.Insertion
import io.github.lenticualrstone.civilight.domain.recorder.put.InsertionUndo
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptSpot
import io.github.lenticualrstone.civilight.domain.recorder.put.PageInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.Undo
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrash
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry

/**
 * Recorder actions that write into a project or the recorder trash (DECISIONS §1): the three
 * `넣기` go through `domain:recorder` `put` package (copies the record's runs, `@` links into the
 * destination kept, LRC-32; placement, link and undo token); delete, restore and purge of a record
 * go through `domain:trash` [RecorderTrash]. Each `넣기` throws
 * `domain:recorder` `InsertionException` when the record or destination does not qualify.
 */
object RecorderActions {
    /** `원고에 넣기` / `여기 넣기` (LRC-33, LRC-34): one `⌁` memo block at [spot], numbered with the unit's next number. */
    fun putInManuscript(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, spot: ManuscriptSpot, ctx: ActionContext): Insertion =
        ManuscriptInsert.insert(doc, library, recordKey, spot, ctx.recorder)

    /** `카드에 넣기` (LRC-35): a memo paragraph in [tabId] of the card [card]. */
    fun putInCard(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, card: TargetRef, tabId: String, ctx: ActionContext): Insertion =
        CardInsert.insert(doc, library, recordKey, card, tabId, ctx.recorder)

    /** `페이지로 만들기` (LRC-36): a new page named [name] (blank = the prefill) holding the record's text. */
    fun makePage(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, name: String, ctx: ActionContext): Insertion =
        PageInsert.insert(doc, library, recordKey, name, ctx.recorder)

    /** `되돌리기` of a `넣기`: the created content, the placement and an added link go. */
    fun undoPut(doc: ProjectDoc, library: RecorderLibrary, undo: InsertionUndo): Pair<ProjectDoc, RecorderLibrary> =
        Undo.undo(doc, library, undo)

    /** `지우기` on a record (LRC-43): direct mark and one `recorder` entry. */
    fun delete(state: RecorderTrashState, recordKey: Key, ctx: ActionContext): RecorderTrashState =
        RecorderTrash.delete(state, recordKey, ctx.trash)

    /** `되살리기` of a record; null when hidden records are locked ([authenticated] false) or the entry is gone. */
    fun restore(state: RecorderTrashState, entryKey: Key, ctx: ActionContext, authenticated: Boolean = false): RecorderTrashState? =
        RecorderTrash.restore(state, entryKey, ctx.trash, authenticated)

    /** `영구 삭제` of one record entry, with the deletion-log rows. */
    fun purge(state: RecorderTrashState, entryKey: Key, ctx: ActionContext): Pair<RecorderTrashState, List<DeletionLogEntry>> =
        RecorderTrash.purge(state, entryKey, ctx.trash)
}
