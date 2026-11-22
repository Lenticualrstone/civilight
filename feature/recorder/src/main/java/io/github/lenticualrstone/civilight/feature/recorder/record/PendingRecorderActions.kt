package io.github.lenticualrstone.civilight.feature.recorder.record

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.recorder.put.CharacterInsert
import io.github.lenticualrstone.civilight.domain.recorder.put.Insertion
import io.github.lenticualrstone.civilight.domain.recorder.put.MemoFieldInsert
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/**
 * Stand-ins for `domain:actions` `RecorderActions.putInMemoField` / `makeCharacter`, which do not
 * exist yet (requested in docs/status/feature-recorder.md). DECISIONS §1 routes every recorder 넣기
 * through `RecorderActions`, whose other 넣기 are one-line delegates to `domain:recorder` `put`; these
 * do the same with the requested signatures, so switching to the real ones is an import change in
 * [RecordWriter]. Undo goes through `RecorderActions.undoPut` like every other 넣기.
 */
internal object PendingRecorderActions {
    /** `⋯ › 메모 칸에 넣기` (LRC-37): one line at the end of [unitKey]'s 씬 메모 or description line. */
    fun putInMemoField(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, unitKey: Key, ctx: ActionContext): Insertion =
        MemoFieldInsert.insert(doc, library, recordKey, unitKey, ctx.recorder)

    /** `⋯ › 인물로 만들기` (LRC-38): a new character named [name] (required). */
    fun makeCharacter(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, name: String, ctx: ActionContext): Insertion =
        CharacterInsert.insert(doc, library, recordKey, name, ctx.recorder)
}
