package io.github.lenticualrstone.civilight.feature.story.search

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.domain.story.search.EmptySearch
import io.github.lenticualrstone.civilight.domain.story.search.GalleryHit
import io.github.lenticualrstone.civilight.domain.story.search.GalleryMatchField
import io.github.lenticualrstone.civilight.domain.story.search.GallerySearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchCommand
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchResult
import io.github.lenticualrstone.civilight.domain.story.search.TextQuery
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.domain.story.search.SimilarName
import io.github.lenticualrstone.civilight.domain.story.search.LineSpeaker
import io.github.lenticualrstone.civilight.domain.story.search.MoreItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchMore
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.PlaceCard
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.navigation.WorldTab

/** What tapping a search row does (STO-37 taps, STO-38 commands). */
sealed interface SearchTap {
    /** Replaces the search overlay with [route]. */
    data class Open(val route: Route) : SearchTap

    /** `› 시점 바꾸기`: the lens sheet (STO-13). */
    data object Lens : SearchTap

    /** `› 렌즈를 이 씬으로 맞추기` (script only, STO-16). */
    data class LensToScene(val sceneKey: Key) : SearchTap

    /** `› 번호 다시 매기기 · 1-7` (SCR-22 sheet). */
    data class Renumber(val episodeKey: Key) : SearchTap

    /**
     * A gallery result of the search opened over that gallery: closes the overlay and replaces the gallery
     * under it with [route] (the same gallery with the item picked), so no second gallery is stacked.
     */
    data class ReplaceBelow(val route: Route) : SearchTap
}

/**
 * The search opened from a gallery's command bar (GAL-04, board `GalSearch`): only gallery images, no chips,
 * the hint line under the field. A character's gallery searches that gallery; the project gallery searches
 * every gallery (`모든 갤러리`, GAL-10).
 */
data class GalleryScope(val ownerKind: GalleryOwnerKind, val ownerKey: Key) {
    val allGalleries: Boolean get() = ownerKind == GalleryOwnerKind.PROJECT

    /** The owner [GallerySearch] narrows to; null = every gallery. */
    val onlyOwnerKey: Key? get() = if (allGalleries) null else ownerKey

    val hint: String get() = if (allGalleries) SearchCopy.ALL_GALLERIES_FIND else SearchCopy.GALLERY_FIND
}

/** A header and its gallery rows: `갤러리 · 2` in the project search, `설명 · 1` by match field in [GalleryScope]. */
data class GallerySection(val header: String, val hits: List<GalleryHit>)

/** (0.2.0, N-13) The `그 밖 ▾` choices sheet and the `화자 ▾` sheet, local to the search overlay. */
@kotlinx.serialization.Serializable
data class SearchMoreSheet(val projectKey: Key) : io.github.lenticualrstone.civilight.navigation.SheetKey

@kotlinx.serialization.Serializable
data class SearchSpeakerSheet(val projectKey: Key) : io.github.lenticualrstone.civilight.navigation.SheetKey

/** 검색 · 명령 as plain data over `domain:story` `ProjectSearch` (STO-36 … STO-38). */
object SearchModel {
    /** `그 밖 ▾`, or the picked kind `장소 ▾` (N-13). */
    fun moreChip(more: SearchMore?): String = (more?.word ?: SearchCopy.MORE) + SearchCopy.CHEVRON

    /** `화자 ▾`, or the picked speaker `세라 ▾`; a speaker no longer listed reads `화자 ▾`. */
    fun speakerChip(speakerKey: Key?, speakers: List<LineSpeaker>): String =
        (speakers.firstOrNull { it.key == speakerKey }?.name ?: SearchCopy.SPEAKER) + SearchCopy.CHEVRON

    /** The `화자 ▾` chip is drawn only under `대사` (N-13). */
    fun showsSpeaker(filter: SearchFilter, more: SearchMore?): Boolean = more == null && filter == SearchFilter.LINE

    /**
     * S-1: the lens chip stays only on the cast and world heads, and the script has no lens. So the search
     * keeps `시점 바꾸기` only when opened over one of those heads ([keepsLensCommand]) and never lists
     * `렌즈를 이 씬으로 맞추기` (SRCH-07).
     */
    fun withoutLensCommands(result: SearchResult, below: NavKey? = null): SearchResult {
        val removed = if (keepsLensCommand(below)) setOf(SearchCommand.LENS_TO_SCENE) else LENS_COMMANDS
        val sections = result.sections.mapNotNull { section ->
            if (section.kind != SearchSectionKind.COMMAND) return@mapNotNull section
            val kept = section.items.filterNot { (it as? SearchItem.CommandItem)?.command in removed }
            if (kept.isEmpty()) null else section.copy(items = kept)
        }
        if (sections == result.sections) return result
        val empty = if (sections.isEmpty() && result.gallery.isEmpty() && result.query.isNotBlank()) {
            result.empty ?: EmptySearch(result.query, result.hiddenExcluded, emptyList())
        } else {
            result.empty
        }
        return result.copy(sections = sections, empty = empty)
    }

    private val LENS_COMMANDS = setOf(SearchCommand.CHANGE_LENS, SearchCommand.LENS_TO_SCENE)

    /** S-1: the screens whose head draws the lens chip (the cast and world heads). */
    fun keepsLensCommand(below: NavKey?): Boolean = below is CastList || below is World

    /** Where a `그 밖 ▾` row goes: place and faction → card, event → detail, page → page. */
    fun tap(projectKey: Key, item: MoreItem): SearchTap = SearchTap.Open(
        when (item.kind) {
            SearchMore.PLACE -> PlaceCard(projectKey, item.key)
            SearchMore.FACTION -> FactionCard(projectKey, item.key)
            SearchMore.EVENT -> EventDetail(projectKey, item.key)
            SearchMore.PAGE -> PageDoc(projectKey, item.key)
        },
    )

    /** Stable lazy key of a `그 밖 ▾` row. */
    fun moreKey(item: MoreItem): String = "more:${item.kind}:${item.key}"

    /**
     * Whether the result list is drawn for [query] under [filter]: a typed query always, and the `명령` chip
     * also with an empty one (it lists every command of the context; QA 2026-11-23 #11).
     */
    fun listsWithoutQuery(query: String, filter: SearchFilter): Boolean = query.isNotBlank() || filter == SearchFilter.COMMAND

    /**
     * Stable lazy keys for the [items] of [section]: the item's kind and key, never its index, so a row that
     * stays keeps its place. A repeated key (a term's name and its aliases share the item key) gets `#2`, `#3`
     * in order; a command has no key and uses its label.
     */
    fun itemKeys(section: String, items: List<SearchItem>): List<String> {
        val seen = HashMap<String, Int>()
        return items.map { item ->
            val identity = item.key ?: (item as? SearchItem.CommandItem)?.let { it.command.name }.orEmpty()
            val base = "$section:${item::class.simpleName}:$identity"
            val n = (seen[base] ?: 0) + 1
            seen[base] = n
            if (n == 1) base else "$base#$n"
        }
    }

    /** The gallery scope when the overlay was opened over a gallery screen, else null. */
    fun galleryScope(below: NavKey?): GalleryScope? = (below as? Gallery)?.let { GalleryScope(it.ownerKind, it.ownerKey) }

    /**
     * The gallery-only result of [scope] for [raw]: gallery hits, `숨긴 항목 제외` while locked hidden images
     * exist, and `결과 없음` without `비슷한 이름` when nothing matches.
     */
    fun galleryResult(doc: ProjectDoc, raw: String, locked: Boolean, scope: GalleryScope): SearchResult {
        val q = TextQuery(raw)
        val search = GallerySearch(doc, locked, scope.onlyOwnerKey)
        val hits = search.search(q)
        val hidden = search.hiddenExcluded()
        val empty = if (hits.isEmpty() && !q.isEmpty) EmptySearch(q.text, hidden, emptyList()) else null
        return SearchResult(q.text, SearchFilter.ALL, emptyList(), hidden, empty, hits)
    }

    /**
     * The gallery rows of [result]: in the project search one `갤러리` section (count when more than one, as
     * the other sections); in a [GalleryScope] one section per match field in `제목 · 설명 · 태그` order with
     * its count (`설명 · 1`, board `GalSearch`).
     */
    fun gallerySections(result: SearchResult, scoped: Boolean): List<GallerySection> {
        val hits = result.gallery
        if (hits.isEmpty()) return emptyList()
        if (!scoped) return listOf(GallerySection(if (hits.size > 1) "${SearchCopy.GALLERY} · ${hits.size}" else SearchCopy.GALLERY, hits))
        return GalleryMatchField.entries.mapNotNull { field ->
            hits.filter { it.field == field }.takeIf { it.isNotEmpty() }?.let { GallerySection("${field.word} · ${it.size}", it) }
        }
    }

    /** Stable lazy key of a gallery row (one hit per image). */
    fun galleryKey(hit: GalleryHit): String = "gallery:${hit.itemKey}"

    /**
     * Where a gallery row goes: the gallery with the item picked (GAL-04, T-GAL-19). Over a gallery ([scope])
     * that gallery is reused (the project gallery shows every owner's images); else the image's own gallery.
     */
    fun tap(projectKey: Key, hit: GalleryHit, scope: GalleryScope?): SearchTap =
        if (scope != null) {
            SearchTap.ReplaceBelow(Gallery(projectKey, scope.ownerKind, scope.ownerKey, pickKey = hit.itemKey))
        } else {
            SearchTap.Open(Gallery(projectKey, hit.ownerKind, hit.ownerKey, pickKey = hit.itemKey))
        }

    /**
     * The search context from the screen under the overlay: the script (no `시점 바꾸기`, but `렌즈를 이
     * 씬으로 맞추기`, and `#043` in its episode), else the episode of the last edit; the lens shown in
     * `지금: 최신 · 1-7`; the open check count; whether hidden items are locked.
     */
    fun context(doc: ProjectDoc, below: NavKey?, locked: Boolean, openChecks: Int): SearchContext {
        val script = below as? Script
        val sceneKey = script?.let { s -> s.sceneKey ?: s.blockKey?.let { doc.index.block(it)?.sceneKey } }
            ?: StoryOverview(doc).currentSceneKey
        return SearchContext(
            inScript = script != null,
            lens = doc.app.view.lens.point,
            openCheckCount = openChecks,
            currentEpisodeKey = sceneKey?.let(doc.index::numberingUnitKeyOfScene),
            currentSceneKey = if (script != null) sceneKey else null,
            locked = locked,
        )
    }

    /** Where a result row goes: term → card, character → profile, scene → script head, line → block. */
    fun tap(projectKey: Key, item: SearchItem, context: SearchContext): SearchTap? = when (item) {
        is SearchItem.CharacterItem -> SearchTap.Open(CharacterProfile(projectKey, item.key))
        is SearchItem.TermItem -> SearchTap.Open(TermCard(projectKey, item.key))
        is SearchItem.SceneItem -> SearchTap.Open(Script(projectKey, sceneKey = item.key))
        is SearchItem.LineItem -> SearchTap.Open(Script(projectKey, blockKey = item.key))
        is SearchItem.NamedItem -> named(projectKey, item.kind, item.key)?.let(SearchTap::Open)
        is SearchItem.CommandItem -> when (item.command) {
            SearchCommand.CHANGE_LENS -> SearchTap.Lens
            SearchCommand.LENS_TO_SCENE -> context.currentSceneKey?.let(SearchTap::LensToScene)
            SearchCommand.OPEN_CHECKS -> SearchTap.Open(CheckCenter(projectKey))
            SearchCommand.RENUMBER -> context.currentEpisodeKey?.let(SearchTap::Renumber)
        }
    }

    /** A `비슷한 이름` row. */
    fun tap(projectKey: Key, similar: SimilarName): SearchTap? = when (similar.kind) {
        SearchSectionKind.CHARACTER -> SearchTap.Open(CharacterProfile(projectKey, similar.key))
        SearchSectionKind.TERM -> SearchTap.Open(TermCard(projectKey, similar.key))
        else -> named(projectKey, similar.kind, similar.key)?.let(SearchTap::Open)
    }

    /**
     * `‘오리지누’ 용어로 만들기`: the 세계 new-term form (core pass `createTerm`). The route cannot carry the
     * name yet (Requests), so the form opens empty.
     */
    fun createTerm(projectKey: Key): Route = World(projectKey, WorldTab.TERMS, createTerm = true)

    private fun named(projectKey: Key, kind: SearchSectionKind, key: Key): Route? = when (kind) {
        SearchSectionKind.PLACE -> PlaceCard(projectKey, key)
        SearchSectionKind.FACTION -> FactionCard(projectKey, key)
        SearchSectionKind.EVENT -> EventDetail(projectKey, key)
        SearchSectionKind.CHARACTER -> CharacterProfile(projectKey, key)
        SearchSectionKind.TERM -> TermCard(projectKey, key)
        SearchSectionKind.LINE, SearchSectionKind.SCENE, SearchSectionKind.COMMAND -> null
    }
}
