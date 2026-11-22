package io.github.lenticualrstone.civilight.feature.story.search

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.domain.story.search.SearchCommand
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.domain.story.search.SimilarName
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.FactionCard
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
}

/** 검색 · 명령 as plain data over `domain:story` `ProjectSearch` (STO-36 … STO-38). */
object SearchModel {
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
