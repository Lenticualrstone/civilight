package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.UnitLabels
import io.github.lenticualrstone.civilight.domain.recorder.text.RecordText
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/**
 * `⋯ › 메모 칸에 넣기` (LRC-37): the record text as **one line** at the end of a scene's `씬 메모`, or
 * at the end of a chapter's or episode's description line (`synopsis`). Multi-line text is joined
 * with spaces ([PROPOSED]). A new memo line starts on its own line; the description line gets the
 * text after a space (`ED`: it is one line).
 */
object MemoFieldInsert {
    fun appended(previous: String?, line: String, isSceneMemo: Boolean): String = when {
        previous.isNullOrBlank() -> line
        isSceneMemo -> "${previous.trimEnd()}\n$line"
        else -> "${previous.trimEnd()} $line"
    }

    fun insert(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, unitKey: Key, ctx: RecorderEditContext): Insertion {
        val record = Put.source(library, recordKey)
        Put.requireText(record)
        val unit = doc.index.unit(unitKey)?.takeIf { doc.liveness.isLive(it) } ?: throw InsertionException("unit $unitKey is not live")
        val line = RecordText.oneLine(Runs.text(record.runs))
        val labels = UnitLabels(doc)
        val isScene = labels.isScene(unit)
        val previous = if (isScene) unit.memo else unit.synopsis
        val text = appended(previous, line, isScene)
        val nextUnit = (if (isScene) unit.copy(memo = text) else unit.copy(synopsis = text)).copy(updatedAt = ctx.now, updatedBy = ctx.device)
        val nextDoc = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == unitKey) nextUnit else it }))
        val label = if (isScene) "${labels.sceneRef(unit)}${RecorderCopy.SEP}${RecorderCopy.SCENE_MEMO}" else labels.label(unit)
        val (nextLibrary, placement, addedLink) = Put.commit(
            library, record, doc.key, PlacementKind.MEMO_FIELD, unitKey, null, null, label,
            TargetRef(doc.key, RecorderTargetKind.UNIT, unitKey), ctx,
        )
        return Insertion(
            doc = nextDoc,
            library = nextLibrary,
            placement = placement,
            toast = Toast(RecorderCopy.TODO_MEMO_TOAST, listOf(RecorderCopy.UNDO)),
            undo = Put.undoToken(record, placement, addedLink, CreatedContent.MemoLine(unitKey, isScene, previous)),
        )
    }
}
