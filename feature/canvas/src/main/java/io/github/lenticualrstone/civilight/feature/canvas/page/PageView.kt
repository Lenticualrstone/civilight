package io.github.lenticualrstone.civilight.feature.canvas.page

import io.github.lenticualrstone.civilight.domain.recorder.put.SourceMarkTarget
import io.github.lenticualrstone.civilight.domain.recorder.put.SourceMarks
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.canvas.Page
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.docLinks
import io.github.lenticualrstone.civilight.model.text.ordered
import java.time.ZoneId

/** A link inside a paragraph's shown text: [start] until [end] (UTF-16). */
data class LinkSpan(val start: Int, val end: Int, val link: PageLink)

/** One block of a page as shown: its text with link and paren ranges (CNV-13). */
data class PageParagraph(
    val key: Key,
    val kind: DocBlockKind,
    val text: String,
    val links: List<LinkSpan>,
    val parens: List<IntRange>,
    /** Label of a memo block: `⌁ 작가 메모`, with the recorder source when it came from a record. */
    val memoLabel: String?,
) {
    fun linkAt(offset: Int): PageLink? = links.firstOrNull { offset >= it.start && offset < it.end }?.link
}

/** The page's own source mark (CNV-15): `레코더 11월 5일 22:00` · `레코더 · 휴지통` · `지워진 기록`. */
data class SourceMarkUi(val text: String, val recordKey: Key, val target: SourceMarkTarget)

/** Access to a page (rule 19 §6): a hidden page while locked shows only the lock. */
enum class PageAccess { OPEN, HIDDEN_LOCKED, GONE }

data class PageView(
    val key: Key,
    val access: PageAccess,
    /** `페이지 · 설정 노트`. */
    val sectionLine: String,
    /** The page name; empty while a new page has none. */
    val name: String,
    /** `마지막 수정 11월 20일 · 연결 4`. */
    val meta: String,
    val sourceMark: SourceMarkUi?,
    val paragraphs: List<PageParagraph>,
    val hidden: Boolean,
    val headerImage: ImageUse?,
    /** Links to items hidden while locked: editing shows stored link text, so it waits for `지문으로 보기`. */
    val hasLockedLinks: Boolean,
    val readOnly: Boolean,
) {
    val shownName: String get() = name.ifBlank { PageCopy.UNNAMED }

    /**
     * No name and no text: such a page opens straight into editing. A page made with `만들고 열기` has a name,
     * so it is opened into editing through [JustCreatedPages] ([PageAutoEdit]).
     */
    val isBlank: Boolean get() = name.isBlank() && paragraphs.all { it.text.isBlank() }

    val links: List<PageLink> get() = paragraphs.flatMap { p -> p.links.map { it.link } }
}

/**
 * Row builder of a page (CNV-13 … CNV-16): the board `Page` layout as data. Text sits on `bg`; links are
 * resolved by key with the hiding gate; a page never raises term warnings (rule 21 §3–4: the world area
 * lists page mentions under `원고 밖 · 메모`).
 */
object PageViews {
    fun build(
        doc: ProjectDoc,
        pageKey: Key,
        hiddenOpen: Boolean,
        readOnly: Boolean,
        zone: ZoneId,
        recorder: RecorderLibrary? = null,
    ): PageView? {
        val page = doc.pages[pageKey] ?: return null
        // Hidden before trashed (as [PageLinks]): a trashed hidden page must not show its name while locked.
        val access = when {
            page.hidden && !hiddenOpen -> PageAccess.HIDDEN_LOCKED
            page.deletedAt != null -> PageAccess.GONE
            else -> PageAccess.OPEN
        }
        val locked = access != PageAccess.OPEN
        val paragraphs = if (locked) emptyList() else page.blocks.ordered().map { paragraph(doc, it.key, it.kind.known ?: DocBlockKind.PARAGRAPH, it.runs, it.recorderSource, hiddenOpen, recorder, zone) }
        return PageView(
            key = page.key,
            access = access,
            sectionLine = PageCopy.sectionLine(page.sectionLabel),
            name = if (locked) "" else page.name,
            meta = PageCopy.meta(PageCopy.monthDay(page.updatedAt, zone), linkCount(doc, page)),
            sourceMark = if (locked) null else page.sourceRecord?.let { s ->
                recorder?.let { SourceMarkUi(SourceMarks.mark(s, it, zone), s.recordKey, SourceMarks.target(s, it)) }
            },
            paragraphs = paragraphs,
            hidden = page.hidden,
            // A locked or trashed page shows nothing of itself, its header image included (rule 19 §6).
            headerImage = if (locked) null else doc.index.imageUse(ImageOwnerKind.PAGE, page.key, ImageSlot.PAGE_HEADER),
            hasLockedLinks = paragraphs.any { p -> p.links.any { it.link.state == LinkState.HIDDEN } },
            readOnly = readOnly,
        )
    }

    private fun paragraph(
        doc: ProjectDoc,
        key: Key,
        kind: DocBlockKind,
        runs: List<TextRun>,
        source: io.github.lenticualrstone.civilight.model.text.RecorderSource?,
        hiddenOpen: Boolean,
        recorder: RecorderLibrary?,
        zone: ZoneId,
    ): PageParagraph {
        val text = StringBuilder()
        val links = ArrayList<LinkSpan>()
        val parens = ArrayList<IntRange>()
        for (run in runs) {
            val start = text.length
            when (run) {
                is TextRun.Link -> {
                    val link = PageLinks.resolve(doc, run, hiddenOpen)
                    text.append(link.inline)
                    links += LinkSpan(start, text.length, link)
                }
                is TextRun.Paren -> {
                    text.append("(").append(run.text).append(")")
                    parens += start until text.length
                }
                else -> text.append(run.shownText)
            }
        }
        val memoLabel = if (kind == DocBlockKind.MEMO) {
            if (source != null && recorder != null) {
                "${PageCopy.MEMO_LABEL}${PageCopy.SEP}${SourceMarks.mark(source, recorder, zone)}"
            } else {
                PageCopy.MEMO_LABEL
            }
        } else {
            null
        }
        return PageParagraph(key, kind, text.toString(), links, parens, memoLabel)
    }

    /**
     * `연결 N` (Q-PG-04 [PROPOSED]): distinct items this page links to plus other live pages that link
     * to it. Computed, never stored.
     */
    fun linkCount(doc: ProjectDoc, page: Page): Int {
        val outgoing = page.blocks.docLinks.map { it.target.kind.raw to it.target.key }.toSet().size
        val incoming = doc.pages.values.count { other ->
            other.key != page.key && other.deletedAt == null && other.blocks.docLinks.any { it.target.key == page.key }
        }
        return outgoing + incoming
    }
}

/** Who may edit a page now. */
object PageAccessRules {
    /**
     * Editing needs an open, writable page whose links to hidden items are visible ([PageView.hasLockedLinks]
     * is computed with the gate, so it is false once `지문으로 보기` opened them): the shared editor shows
     * the stored link text, which must not reveal a hidden name (rule 19 §6).
     */
    fun canEdit(view: PageView): Boolean =
        view.access == PageAccess.OPEN && !view.readOnly && !view.hasLockedLinks
}

/** What back does on the page screen (top button and system back alike). */
enum class PageBack { LEAVE, STOP_EDITING }

object PageBackRules {
    /**
     * Editing the user started (tap into the text, `고치기`, the command bar) ends first and shows the page with its
     * links again. Editing that began by itself (a blank page just made) leaves the page at once: ending such a
     * session changes nothing visible, so that back read as ignored (emulator round 1).
     */
    fun onBack(editing: Boolean, autoStarted: Boolean): PageBack =
        if (editing && !autoStarted) PageBack.STOP_EDITING else PageBack.LEAVE
}
