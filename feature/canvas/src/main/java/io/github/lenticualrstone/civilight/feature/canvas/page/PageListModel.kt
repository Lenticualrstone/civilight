package io.github.lenticualrstone.civilight.feature.canvas.page

import io.github.lenticualrstone.civilight.feature.canvas.card.CardInitial
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import java.time.ZoneId

/** One page of the list (CNV-14). */
data class PageRowUi(
    val key: Key,
    val name: String,
    /** `설정 노트 · 7월 2일`. */
    val sub: String,
    /** Card kind line: `페이지 · 설정 노트`. */
    val kindLine: String,
    /** `7월 2일`. */
    val date: String,
    /** A hidden page while hidden items are open: in place with a small `숨김` tag (rule 19 §6). */
    val hiddenTag: Boolean,
    /** The placeholder letter (CNV-24). */
    val initial: String,
    val headerImage: ImageUse?,
)

data class PageListView(
    /** `페이지 · 2`. */
    val section: String,
    val rows: List<PageRowUi>,
    /** Hidden pages while locked: one locked row with the count (rule 19 §6), never their names. */
    val lockedCount: Int,
    val readOnly: Boolean,
) {
    val lockedRow: String? get() = if (lockedCount > 0) PageCopy.hiddenPages(lockedCount) else null
    val isEmpty: Boolean get() = rows.isEmpty() && lockedCount == 0
}

/** The page list (CNV-14, CNV-16, T-CNV-22): live pages in their order; trashed pages leave it. */
object PageListModel {
    fun build(doc: ProjectDoc, hiddenOpen: Boolean, readOnly: Boolean, zone: ZoneId): PageListView {
        val pages = PageEdits.pages(doc)
        val shown = pages.filter { !it.hidden || hiddenOpen }
        val rows = shown.map { p ->
            val name = p.name.ifBlank { PageCopy.UNNAMED }
            val date = PageCopy.monthDay(p.updatedAt, zone)
            PageRowUi(
                key = p.key,
                name = name,
                sub = PageCopy.rowSub(p.sectionLabel, date),
                kindLine = PageCopy.sectionLine(p.sectionLabel),
                date = date,
                hiddenTag = p.hidden,
                initial = CardInitial.of(name),
                headerImage = doc.index.imageUse(ImageOwnerKind.PAGE, p.key, ImageSlot.PAGE_HEADER),
            )
        }
        return PageListView(
            section = PageCopy.listSection(rows.size),
            rows = rows,
            lockedCount = pages.size - shown.size,
            readOnly = readOnly,
        )
    }
}
