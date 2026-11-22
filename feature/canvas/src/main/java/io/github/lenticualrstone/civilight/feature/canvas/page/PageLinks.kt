package io.github.lenticualrstone.civilight.feature.canvas.page

import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.TermCard

/** How a link run in a page reads at render time (CNV-16; rule 19 §6, rule 20 §2). */
enum class LinkState {
    /** The target is live and may be opened. */
    LIVE,

    /** A hidden target while the project's hidden items are locked: `숨긴 인물` / `숨긴 항목`. */
    HIDDEN,

    /** The target is in the trash: dotted chip with `◇`, card `지운 인물 · 노아` with `되살리기`. */
    TRASHED,

    /** Only a name tag is left, or nothing (`영구 삭제 · 노아`): dotted, no `되살리기`. */
    GONE,

    /** A kind a page does not link to (a unit or project link pasted from elsewhere): plain text. */
    OTHER,
}

/** One link run as the page shows it. */
data class PageLink(
    val linkKey: Key,
    val state: LinkState,
    /** The inline text: the stored text when live, `숨긴 인물` while hidden, `◇ 노아` / `◇ 영구 삭제 · 노아` when deleted. */
    val inline: String,
    /** The card title of a deleted link (`지운 인물 · 노아`, `영구 삭제 · 노아`); null otherwise. */
    val cardTitle: String?,
    /** The item behind the link (for `되살리기` and `이대로 두기`); null for kinds the trash does not know. */
    val item: ItemId?,
    /** Where a live link opens. */
    val route: Route?,
) {
    val dotted: Boolean get() = state == LinkState.TRASHED || state == LinkState.GONE

    /** Rule 20 §2 row `@연결`: the buttons of a deleted link's card. */
    val actions: List<String>
        get() = when (state) {
            LinkState.TRASHED -> listOf(PageCopy.BTN_RELINK, PageCopy.BTN_UNLINK, PageCopy.BTN_RESTORE, PageCopy.BTN_KEEP)
            LinkState.GONE -> listOf(PageCopy.BTN_RELINK, PageCopy.BTN_UNLINK, PageCopy.BTN_KEEP)
            else -> emptyList()
        }
}

/**
 * Link resolution for pages: names are resolved by key at render time (CNV-13 storage); characters,
 * terms, places and factions through `domain:cast` `ItemRefs` (the one resolver of rule 19 / 20), page
 * targets here (a page is not one of the cast resolver's kinds). Plain text is never a reference.
 */
object PageLinks {
    fun resolve(doc: ProjectDoc, link: TextRun.Link, hiddenOpen: Boolean): PageLink {
        val target = link.target
        val kind = target.kind.known
        val entity = when (kind) {
            LinkTargetKind.CHARACTER -> EntityKind.CHARACTER
            LinkTargetKind.TERM -> EntityKind.TERM
            LinkTargetKind.PLACE -> EntityKind.PLACE
            LinkTargetKind.FACTION -> EntityKind.FACTION
            LinkTargetKind.PAGE -> return page(doc, link, hiddenOpen)
            else -> null
        } ?: return PageLink(link.linkKey, LinkState.OTHER, link.text, null, null, null)
        val item = ItemId(itemKind(entity), target.key)
        return when (val r = ItemRefs.resolve(doc, entity, target.key, hiddenOpen)) {
            is ResolvedItem.Live -> PageLink(link.linkKey, LinkState.LIVE, link.text, null, item, routeOf(doc.key, entity, target.key))
            is ResolvedItem.HiddenLocked -> PageLink(link.linkKey, LinkState.HIDDEN, r.narrowLabel, null, item, null)
            is ResolvedItem.Trashed -> PageLink(link.linkKey, LinkState.TRASHED, deletedInline(r.narrowLabel), r.wideLabel, item, null)
            is ResolvedItem.Tagged -> PageLink(link.linkKey, LinkState.GONE, deletedInline(r.narrowLabel), r.wideLabel, item, null)
            is ResolvedItem.Missing -> PageLink(link.linkKey, LinkState.GONE, deletedInline(r.narrowLabel), r.wideLabel, item, null)
        }
    }

    private fun page(doc: ProjectDoc, link: TextRun.Link, hiddenOpen: Boolean): PageLink {
        val key = link.target.key
        val item = ItemId.page(key)
        val page = doc.pages[key]
        return when {
            page == null -> {
                val label = PageCopy.DELETED_PAGE
                PageLink(link.linkKey, LinkState.GONE, deletedInline(label), label, item, null)
            }
            page.hidden && !hiddenOpen -> PageLink(link.linkKey, LinkState.HIDDEN, PageCopy.HIDDEN_ITEM, null, item, null)
            page.deletedAt != null -> {
                val name = page.name.ifBlank { PageCopy.UNNAMED }
                PageLink(link.linkKey, LinkState.TRASHED, deletedInline(name), TrashCopy.deletedLabel(ItemKind.PAGE, name), item, null)
            }
            else -> PageLink(link.linkKey, LinkState.LIVE, link.text, null, item, PageDoc(doc.key, key))
        }
    }

    private fun deletedInline(text: String): String = "${PageCopy.DELETED_MARK} $text"

    fun itemKind(entity: EntityKind): ItemKind = when (entity) {
        EntityKind.CHARACTER -> ItemKind.CHARACTER
        EntityKind.TERM -> ItemKind.TERM
        EntityKind.PLACE -> ItemKind.PLACE
        EntityKind.FACTION -> ItemKind.FACTION
    }

    fun routeOf(projectKey: Key, kind: EntityKind, key: Key): Route = when (kind) {
        EntityKind.CHARACTER -> CharacterProfile(projectKey, key)
        EntityKind.TERM -> TermCard(projectKey, key)
        EntityKind.PLACE -> PlaceCard(projectKey, key)
        EntityKind.FACTION -> FactionCard(projectKey, key)
    }
}
