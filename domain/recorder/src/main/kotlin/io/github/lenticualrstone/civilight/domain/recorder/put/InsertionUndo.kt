package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/**
 * Toast `되돌리기` of a 넣기 (LRC-32): the created content is deleted (no trash entry, block
 * numbers are never reissued), the placement and the link this insertion added are removed, and
 * the record returns to the state it had (`doneMarked` restored). Not a deletion: no `◇`.
 */
object Undo {
    fun undo(doc: ProjectDoc, library: RecorderLibrary, undo: InsertionUndo): Pair<ProjectDoc, RecorderLibrary> {
        val nextDoc = when (val c = undo.created) {
            is CreatedContent.ScriptBlock -> {
                val file = doc.episodes[c.numberingUnitKey]
                if (file == null) doc
                else doc.copy(episodes = doc.episodes + (c.numberingUnitKey to file.copy(blocks = file.blocks.filter { it.key != c.blockKey })))
            }
            is CreatedContent.CardParagraph -> CardDocs.removeParagraph(doc, c.entityKey, c.paragraphKey, c.createdFieldValue)
            is CreatedContent.NewPage -> doc.copy(pages = doc.pages - c.pageKey)
            is CreatedContent.MemoLine -> doc.copy(
                story = doc.story.copy(units = doc.story.units.map {
                    when {
                        it.key != c.unitKey -> it
                        c.isSceneMemo -> it.copy(memo = c.previous)
                        else -> it.copy(synopsis = c.previous)
                    }
                }),
            )
            is CreatedContent.NewCharacter -> doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.filter { it.key != c.characterKey }))
        }
        val nextLibrary = library.copy(
            records = library.records.map { if (it.key == undo.recordKey) it.copy(doneMarked = undo.previousDoneMarked) else it },
            links = library.links.filter { it.key != undo.addedLinkKey },
            placements = library.placements.filter { it.key != undo.placementKey },
        )
        return nextDoc to nextLibrary
    }
}
