package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.text.RecordText
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/**
 * `⋯ › 인물로 만들기` (LRC-38, CST-49): a new character (next record number, never reused) with
 * `한 줄 소개` = the record's first sentence and a `⌁` paragraph with the full copied text at the end
 * of `개요` (its first non-secret `긴 글` field, as `domain:cast` does; without such a field no
 * paragraph is written). A duplicate made this way is fixed by the cast area's merge.
 */
object CharacterInsert {
    fun insert(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, name: String, ctx: RecorderEditContext): Insertion {
        val record = Put.source(library, recordKey)
        Put.requireText(record)
        val characterName = name.trim().nfc()
        if (characterName.isEmpty()) throw InsertionException("character name is empty")
        val counters = doc.project.counters
        val overview = CardDocs.overviewField(doc, "")
        val paragraph = overview?.let { CardInsert.memoParagraph(record, doc, emptyList(), ctx) }
        val character = Character(
            key = ctx.newKey(),
            recordNo = counters.nextCharacterRecordNo,
            name = characterName,
            oneLiner = RecordText.firstSentence(Runs.text(record.runs)).ifEmpty { null },
            order = OrderKeys.after(doc.cast.characters.maxOfOrNull { it.order }),
            fields = if (overview != null && paragraph != null) mapOf(overview.key to FieldValue.of(FieldPayload.Rich(listOf(paragraph)))) else emptyMap(),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        val nextDoc = doc.copy(
            project = doc.project.copy(counters = counters.copy(nextCharacterRecordNo = counters.nextCharacterRecordNo + 1)),
            cast = doc.cast.copy(characters = doc.cast.characters + character),
        )
        val (nextLibrary, placement, addedLink) = Put.commit(
            library, record, doc.key, PlacementKind.CHARACTER, character.key, null, null, character.name,
            TargetRef(doc.key, RecorderTargetKind.CHARACTER, character.key), ctx,
        )
        return Insertion(
            doc = nextDoc,
            library = nextLibrary,
            placement = placement,
            toast = Toast(RecorderCopy.TODO_CHARACTER_TOAST, listOf(RecorderCopy.UNDO)),
            undo = Put.undoToken(record, placement, addedLink, CreatedContent.NewCharacter(character.key)),
        )
    }
}
