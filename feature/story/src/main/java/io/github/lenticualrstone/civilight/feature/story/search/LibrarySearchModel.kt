package io.github.lenticualrstone.civilight.feature.story.search

import io.github.lenticualrstone.civilight.domain.library.masking.CrossSearchScope
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.Presentation
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectSurfaces
import io.github.lenticualrstone.civilight.domain.library.masking.Surface
import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchResult
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.ui.security.UnlockState

/** One project's hits in 모든 프로젝트에서 찾기: its name (or the masked label) over its sections. */
data class CrossSection(
    val projectKey: Key,
    val title: String,
    val protections: Protections,
    val result: SearchResult,
    val context: SearchContext,
)

/** What `LibrarySearch` shows: the projects with hits, the scope footer, or the empty line. */
data class CrossSearchUi(
    val query: String,
    val filter: SearchFilter,
    val sections: List<CrossSection>,
    /** `숨긴 프로젝트 제외` (LIB-07) when the scope left a hidden project out. */
    val footer: String?,
    /** `‘<query>’ · 숨긴 프로젝트 제외` under `결과 없음` (LIB-70) when nothing matched. */
    val emptyLine: String?,
)

/** A project the cross search can read: its document and whether it is the example (LIB-17). */
data class CrossProject(val doc: ProjectDoc, val isExample: Boolean = false, val trashed: Boolean = false)

/**
 * 모든 프로젝트에서 찾기 (LIB-07 scope over the STO-37 engine). The scope is `domain:library`
 * [CrossSearchScope] (hidden projects out while locked; while open only with `숨긴 프로젝트도 검색`);
 * inside each project the in-project rules apply (deleted items never, locked hidden items' cards
 * left out). Commands are project-bound, so the `명령` chip and section are not offered here.
 */
object LibrarySearchModel {
    val filters: List<SearchFilter> = SearchFilter.entries - SearchFilter.COMMAND

    fun search(
        projects: List<CrossProject>,
        access: HiddenAccess,
        settings: AppSettings,
        itemsOpen: (Key) -> Boolean,
        query: String,
        filter: SearchFilter,
    ): CrossSearchUi? {
        if (query.isBlank()) return null
        val profiles = projects.map { ProjectProfile.of(it.doc, trashed = it.trashed, isExample = it.isExample) }
        val scope = CrossSearchScope.of(profiles, access, settings)
        val byKey = projects.associateBy { it.doc.project.key }
        val sections = scope.projectKeys.mapNotNull { key ->
            val project = byKey[key] ?: return@mapNotNull null
            val profile = profiles.first { it.key == key }
            val title = when (val p = ProjectSurfaces.present(Surface.CROSS_SEARCH, profile, access, settings)) {
                is Presentation.Shown -> p.name
                is Presentation.Label -> p.text
                else -> return@mapNotNull null
            }
            val context = SearchContext(lens = project.doc.app.view.lens.point, locked = !itemsOpen(key))
            val result = ProjectSearch(project.doc, context).search(query, filter)
            val kept = result.sections.filter { it.kind != SearchSectionKind.COMMAND && it.items.isNotEmpty() }
            if (result.empty != null || kept.isEmpty()) null else CrossSection(key, title, project.doc.project.protections, result.copy(sections = kept), context)
        }
        return CrossSearchUi(
            query = query,
            filter = filter,
            sections = sections,
            footer = scope.footer,
            emptyLine = if (sections.isEmpty()) CrossSearchScope.emptySub(query, scope) else null,
        )
    }

    /** The shell's unlock state as `domain:library` [HiddenAccess] (as the 서재 reads it). */
    fun access(state: UnlockState): HiddenAccess = HiddenAccess(
        session = UnlockSession(
            hiddenGroupUnlockedAt = state.hiddenGroupUnlockedAt,
            itemsUnlocked = state.itemsUnlockedAt,
            recordsUnlockedAt = state.recordsUnlockedAt,
        ),
        keptOpen = state.keptOpen,
    )
}
