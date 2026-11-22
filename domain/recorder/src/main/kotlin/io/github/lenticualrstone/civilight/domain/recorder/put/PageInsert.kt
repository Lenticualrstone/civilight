package io.github.lenticualrstone.civilight.domain.recorder.put

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.text.RecordText
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind

/**
 * `페이지로 만들기` (LRC-36, CNV-15): one new page whose body is a `⌁` paragraph holding the copied
 * text with its source (as the example's page 등불 결사 규약 made from R0; CNV Q-PG-06 leaves
 * paragraph vs memo open), and `Page.sourceRecord` pointing back at the record.
 */
object PageInsert {
    /** `이름` prefill: the start of the first sentence (`ED`). */
    fun prefillName(record: RecorderRecord): String = RecordText.pageNamePrefill(Runs.text(record.runs))

    fun insert(doc: ProjectDoc, library: RecorderLibrary, recordKey: Key, name: String, ctx: RecorderEditContext): Insertion {
        val record = Put.source(library, recordKey)
        Put.requireText(record)
        val pageName = name.trim().nfc().ifEmpty { prefillName(record) }
        if (pageName.isEmpty()) throw InsertionException("page name is empty")
        val source = Put.sourceOf(record)
        val body = CardInsert.memoParagraph(record, doc, emptyList(), ctx)
        val page = Page(
            key = ctx.newKey(),
            order = OrderKeys.after(doc.pages.values.maxOfOrNull { it.order }),
            name = pageName,
            blocks = listOf(body),
            sourceRecord = source,
            createdAt = ctx.now,
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        val nextDoc = doc.copy(pages = doc.pages + (page.key to page))
        val (nextLibrary, placement, addedLink) = Put.commit(
            library, record, doc.key, PlacementKind.PAGE, page.key, null, null, page.name,
            TargetRef(doc.key, RecorderTargetKind.PAGE, page.key), ctx,
        )
        return Insertion(
            doc = nextDoc,
            library = nextLibrary,
            placement = placement,
            toast = Toast(RecorderCopy.TODO_PAGE_TOAST, listOf(RecorderCopy.UNDO)),
            undo = Put.undoToken(record, placement, addedLink, CreatedContent.NewPage(page.key)),
        )
    }
}
