package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkEdits
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.pointsAt
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.TextRun

/** What an insertion created in the destination project (removed again by undo, no trash entry). */
sealed interface CreatedContent {
    data class ScriptBlock(val blockKey: Key, val numberingUnitKey: Key) : CreatedContent

    /** [createdFieldValue] = the paragraph went into a field that had no value before. */
    data class CardParagraph(val entityKey: Key, val paragraphKey: Key, val createdFieldValue: Key?) : CreatedContent
    data class NewPage(val pageKey: Key) : CreatedContent

    /** [previous] = the scene memo or the description line before the line was appended. */
    data class MemoLine(val unitKey: Key, val isSceneMemo: Boolean, val previous: String?) : CreatedContent
    data class NewCharacter(val characterKey: Key) : CreatedContent
}

/** Everything needed to undo one insertion (toast `되돌리기`). */
data class InsertionUndo(
    val recordKey: Key,
    val projectKey: Key,
    val placementKey: Key,
    /** The link this insertion added (origin `넣기`), or null when the target was already linked. */
    val addedLinkKey: Key?,
    /** `doneMarked` before the insertion (an insertion clears it; undo restores the earlier state). */
    val previousDoneMarked: Boolean,
    val created: CreatedContent,
)

/**
 * Result of one 넣기 (LRC-32): the destination project and the library after it, the new
 * placement, the toast and the undo token.
 */
data class Insertion(
    val doc: ProjectDoc,
    val library: RecorderLibrary,
    val placement: Placement,
    val toast: Toast,
    val undo: InsertionUndo,
)

/** Why a 넣기 could not run. */
class InsertionException(message: String) : IllegalArgumentException(message)

/** Rules shared by every 넣기 (LRC-32). */
object Put {
    /** The record to copy: must exist, not be in the trash, and have text ([PROPOSED] LRC-33). */
    fun source(library: RecorderLibrary, recordKey: Key): RecorderRecord {
        val record = library.record(recordKey) ?: throw InsertionException("no record $recordKey")
        if (record.deletedAt != null) throw InsertionException("record $recordKey is in the trash")
        return record
    }

    fun requireText(record: RecorderRecord) {
        if (Runs.text(record.runs).isBlank()) throw InsertionException("record ${record.key} has no text")
    }

    fun sourceOf(record: RecorderRecord): RecorderSource = RecorderSource(record.key, record.createdAt)

    /**
     * The record's runs copied for a destination surface: a link into [projectKey] of a kind the
     * surface allows stays a link (new link key, project key dropped: §1.4); any other link becomes
     * plain text with the same characters (`@세라` → `세라`). Unknown runs keep their shown text.
     */
    fun copyRuns(runs: List<TextRun>, projectKey: Key, allowed: Set<LinkTargetKind>, ctx: RecorderEditContext): List<TextRun> =
        Runs.normalize(
            runs.map { run ->
                when (run) {
                    is TextRun.Link -> {
                        val kind = run.target.kind.known
                        if (run.target.projectKey == projectKey && kind != null && kind in allowed) {
                            TextRun.Link(ctx.newKey(), LinkTarget(kind, run.target.key), run.text)
                        } else {
                            TextRun.Plain(run.text)
                        }
                    }
                    is TextRun.Plain, TextRun.Break -> run
                    is TextRun.Paren -> TextRun.Plain(run.text)
                    is TextRun.Unknown -> TextRun.Plain(run.shownText)
                }
            },
        )

    /**
     * The library side of an insertion: the placement is added; a link (origin `넣기`) is added when
     * [target] is not linked yet; `doneMarked` is cleared. Returns the library and the added link.
     */
    fun commit(
        library: RecorderLibrary,
        record: RecorderRecord,
        projectKey: Key,
        kind: PlacementKind,
        targetKey: Key,
        containerKey: Key?,
        tabId: String?,
        label: String,
        linkTarget: TargetRef,
        ctx: RecorderEditContext,
    ): Triple<RecorderLibrary, Placement, Key?> {
        val placement = Placement(
            key = ctx.newKey(), recordKey = record.key, projectKey = projectKey, kind = kind.open(), targetKey = targetKey,
            containerKey = containerKey, tabId = tabId, copiedRevision = record.textRevision, createdAt = ctx.now,
            lastLabel = label, updatedAt = ctx.now, updatedBy = ctx.device,
        )
        val links = library.linksOf(record.key)
        val added = if (links.any { it.pointsAt(linkTarget) }) null else LinkEdits.newLink(record.key, linkTarget, LinkOrigin.INSERT, ctx)
        val records = library.records.map {
            if (it.key == record.key && it.doneMarked) it.copy(doneMarked = false, updatedAt = ctx.now, updatedBy = ctx.device) else it
        }
        val next = library.copy(
            records = records,
            links = library.links + listOfNotNull(added),
            placements = library.placements + placement,
        )
        return Triple(next, placement, added?.key)
    }

    fun undoToken(record: RecorderRecord, placement: Placement, addedLinkKey: Key?, created: CreatedContent) = InsertionUndo(
        recordKey = record.key, projectKey = placement.projectKey, placementKey = placement.key, addedLinkKey = addedLinkKey,
        previousDoneMarked = record.doneMarked, created = created,
    )
}
