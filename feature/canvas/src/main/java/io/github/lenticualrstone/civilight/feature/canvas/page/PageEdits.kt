package io.github.lenticualrstone.civilight.feature.canvas.page

import io.github.lenticualrstone.civilight.feature.canvas.common.RefFixes
import io.github.lenticualrstone.civilight.feature.canvas.common.RestoreResult
import io.github.lenticualrstone.civilight.feature.canvas.common.actions
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.Impact
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.isBlankText
import io.github.lenticualrstone.civilight.model.text.ordered

/**
 * Page document edits (CNV-13 … CNV-16): pure `ProjectDoc -> ProjectDoc` functions. Trash, `글로 풀기`,
 * `이대로 두기` and `되살리기` go through `domain:trash` / `domain:actions` (DECISIONS §1); only the
 * page's own fields and runs are written here.
 */
object PageEdits {
    fun page(doc: ProjectDoc, key: Key): Page? = doc.pages[key]?.takeIf { it.deletedAt == null }

    /** Live pages in their order. */
    fun pages(doc: ProjectDoc): List<Page> =
        doc.pages.values.filter { it.deletedAt == null }.sortedWith(compareBy<Page>({ it.order }, { it.key }))

    /** A new empty page after the others (CNV-14 [PROPOSED]); it opens straight into editing. */
    fun create(doc: ProjectDoc, ctx: EditContext): Pair<ProjectDoc, Key> = create(doc, "", ctx)

    /** U-15.9: a new page named [name] from the create card (`이름` only) after the others. */
    fun create(doc: ProjectDoc, name: String, ctx: EditContext): Pair<ProjectDoc, Key> {
        val key = ctx.newKey()
        val page = Page(
            key = key,
            order = OrderKeys.after(doc.pages.values.maxOfOrNull { it.order }),
            name = name.trim().nfc(),
            createdAt = ctx.now,
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return doc.copy(pages = doc.pages + (key to page)) to key
    }

    fun rename(doc: ProjectDoc, key: Key, name: String, ctx: EditContext): ProjectDoc =
        update(doc, key, ctx) { it.copy(name = name.nfc()) }

    /** The body as the shared editor left it (paragraph keys, kinds, link keys and recorder sources kept). */
    fun setBlocks(doc: ProjectDoc, key: Key, blocks: RichDoc, ctx: EditContext): ProjectDoc =
        update(doc, key, ctx) { p -> if (p.blocks == blocks) p else p.copy(blocks = blocks) }

    /** `숨기기` / `숨김 풀기` (rule 19 §6: item hiding covers pages). */
    fun setHidden(doc: ProjectDoc, key: Key, hidden: Boolean, ctx: EditContext): ProjectDoc =
        update(doc, key, ctx) { it.copy(hidden = hidden) }

    /**
     * `/로 블록 추가`: an empty block of [kind] right after [afterKey] (or at the end), returned with its
     * key. Block kinds of a page: `본문 · 소제목 · ⌁ 작가 메모` (Q-PG-01).
     */
    fun addBlock(blocks: RichDoc, kind: DocBlockKind, afterKey: Key?, newKey: () -> Key): Pair<RichDoc, Key> {
        val ordered = blocks.ordered()
        val index = ordered.indexOfFirst { it.key == afterKey }.let { if (it < 0) ordered.lastIndex else it }
        val lower = ordered.getOrNull(index)?.order
        val upper = ordered.getOrNull(index + 1)?.order
        val key = newKey()
        val block = DocBlock(key = key, order = OrderKeys.between(lower, upper), kind = kind.open())
        return (ordered + block).ordered() to key
    }

    /** Changes the kind of one block (`/` on an empty block turns it instead of adding another). */
    fun setKind(blocks: RichDoc, blockKey: Key, kind: DocBlockKind): RichDoc =
        blocks.map { if (it.key == blockKey) it.copy(kind = kind.open()) else it }

    /**
     * `/` with the caret in block [caretBlockKey]: an empty block takes [kind]; otherwise an empty block
     * of [kind] is added right after it (at the end without a caret block).
     */
    fun slash(blocks: RichDoc, kind: DocBlockKind, caretBlockKey: Key?, newKey: () -> Key): RichDoc {
        val caret = blocks.firstOrNull { it.key == caretBlockKey }
        if (caret != null && caret.runs.isBlankText()) return setKind(blocks, caret.key, kind)
        return addBlock(blocks, kind, caret?.key, newKey).first
    }

    /**
     * `다른 항목에 잇기` (rule 20 §2 row `@연결`): the link run [linkKey] of this page points at another
     * item and shows its name; the link key stays (it survives edits, §1.4).
     */
    fun relink(doc: ProjectDoc, pageKey: Key, linkKey: Key, kind: LinkTargetKind, targetKey: Key, text: String, ctx: EditContext): ProjectDoc =
        update(doc, pageKey, ctx) { p ->
            p.copy(
                blocks = p.blocks.map { b ->
                    b.copy(runs = b.runs.map { r -> if (r is TextRun.Link && r.linkKey == linkKey) r.copy(target = LinkTarget(kind, targetKey), text = text.nfc()) else r })
                },
            )
        }

    /** `글로 풀기` (TC-20-22): `domain:trash` `Fixes.unlink`; the letters stay, the link goes. */
    fun unlink(doc: ProjectDoc, linkKey: Key, ctx: EditContext): ProjectDoc = Fixes.unlink(doc, linkKey, ctx.actions().trash).doc

    /** `이대로 두기` on the item behind a link (see [RefFixes.keep]). */
    fun keep(doc: ProjectDoc, item: ItemId, ctx: EditContext): ProjectDoc = RefFixes.keep(doc, item, ctx)

    /** `되살리기` of the item behind a link (see [RefFixes.restore]). */
    fun restore(doc: ProjectDoc, item: ItemId, ctx: EditContext, authenticated: Boolean): RestoreResult =
        RefFixes.restore(doc, item, ctx, authenticated)

    /**
     * `지우기` of a page (Q-PG-08 [PROPOSED]: to the trash like every other item, `domain:trash`
     * `TrashOps`; the trash lists pages under `전체`). Returns the document and the toast
     * (`페이지 하층 구역의 빛 지움 · 되돌리기`).
     */
    fun delete(doc: ProjectDoc, key: Key, ctx: EditContext): Pair<ProjectDoc, Toast>? {
        if (page(doc, key) == null) return null
        val item = ItemId.page(key)
        val toast = Messages.deleteToast(Impact.of(doc, item))
        val result = TrashOps.delete(doc, item, ctx.actions().trash)
        if (item !in result.trashed) return null
        return result.doc to toast
    }

    private fun update(doc: ProjectDoc, key: Key, ctx: EditContext, change: (Page) -> Page): ProjectDoc {
        val page = page(doc, key) ?: return doc
        val next = change(page)
        if (next == page) return doc
        return doc.copy(pages = doc.pages + (key to next.copy(updatedAt = ctx.now, updatedBy = ctx.device)))
    }
}
