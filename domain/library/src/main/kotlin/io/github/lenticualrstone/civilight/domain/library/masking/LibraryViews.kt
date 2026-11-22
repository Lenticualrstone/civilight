package io.github.lenticualrstone.civilight.domain.library.masking

import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.model.app.RecentKind

/** The locked hidden-group row (`LockRow`, LIB-01) or lock card (LIB-04). Never names, covers or ratings. */
data class HiddenGroupRow(val count: Int) {
    val title: String get() = "숨긴 프로젝트 $count"
    val sub: String get() = "이름 가림 · 검색 · 위젯 · 알림 제외"
    val cardSub: String get() = "이름 · 표지 가림"
    val button: String get() = "지문으로 보기"
}

/** 서재 list content after masking (LIB-01, LIB-04, LIB-31). */
data class LibraryListing(
    /** Visible projects (non-hidden; hidden ones too while the group is open). */
    val rows: List<ProjectProfile>,
    /** Hidden projects shown inside the open group (`LibUnlocked`). */
    val openGroup: List<ProjectProfile>,
    /** The locked group row, or null when there is no hidden project or the group is open. */
    val lockedGroup: HiddenGroupRow?,
    val visibleCount: Int,
    val hiddenCount: Int,
) {
    /** `프로젝트 · N`. */
    val sectionLabel: String get() = "프로젝트 · $visibleCount"

    /** Card view: `프로젝트 · N · 숨김 M` (M only when ≥ 1). */
    val cardSectionLabel: String get() = if (hiddenCount >= 1) "프로젝트 · $visibleCount · 숨김 $hiddenCount" else sectionLabel

    /** Header right action: `다시 잠그기` while the group is open, else `+`. */
    val headerAction: String get() = if (openGroup.isNotEmpty()) "다시 잠그기" else "+"

    companion object {
        /** Trashed and example projects are not listed (`ED` for the example, LIB-17). */
        fun of(projects: List<ProjectProfile>, access: HiddenAccess): LibraryListing {
            val listed = projects.filter { !it.trashed && !it.isExample }
            val (hidden, visible) = listed.partition { it.hidden }
            val open = if (access.groupOpen) hidden else emptyList()
            return LibraryListing(
                rows = visible,
                openGroup = open,
                lockedGroup = if (hidden.isNotEmpty() && !access.groupOpen) HiddenGroupRow(hidden.size) else null,
                visibleCount = visible.size,
                hiddenCount = hidden.size,
            )
        }
    }
}

/** Scope of 모든 프로젝트에서 찾기 (LIB-07). */
data class CrossSearchScope(val projectKeys: List<Key>, val footer: String?) {
    companion object {
        const val FOOTER_HIDDEN_PROJECTS = "숨긴 프로젝트 제외"

        fun of(projects: List<ProjectProfile>, access: HiddenAccess, settings: AppSettings = AppSettings()): CrossSearchScope {
            val candidates = projects.filter { !it.trashed }
            val included = candidates.filter { ProjectSurfaces.present(Surface.CROSS_SEARCH, it, access, settings) !is Presentation.Omitted }
            val excludedHidden = candidates.any { it.hidden && it !in included }
            return CrossSearchScope(included.map { it.key }, if (excludedHidden) FOOTER_HIDDEN_PROJECTS else null)
        }

        /** Empty result sub (LIB-70): `‘<query>’ · 숨긴 프로젝트 제외`. */
        fun emptySub(query: String, scope: CrossSearchScope): String =
            listOfNotNull("‘$query’", scope.footer).joinToString(" · ")
    }
}

/** What 최근 연 파일 needs to know about an item's target. */
data class RecentTarget(val title: String, val hidden: Boolean, val trashed: Boolean)

/** One 최근 연 파일 card (LIB-05). [title] is null when the project's name is masked (`ED`). */
data class RecentCard(val item: RecentItem, val kindLine: String, val title: String?, val openedAt: Timestamp)

/** 최근 연 파일 after masking (LIB-05, R19 §3/§6); computed every time, never stored. */
object Recents {
    fun kindWord(kind: RecentKind): String = when (kind) {
        RecentKind.SCENE -> "씬"
        RecentKind.CHARACTER -> "인물"
        RecentKind.PAGE -> "페이지"
        RecentKind.TERM -> "용어"
        RecentKind.PLACE -> "장소"
        RecentKind.FACTION -> "세력"
    }

    /**
     * Visible cards, newest first, at most [limit]. Items of hidden or trashed projects, hidden or
     * trashed items, and items whose target is gone are left out (locked or open); a name-masked
     * project shows as `R-18 프로젝트` without the item title.
     */
    fun cards(
        items: List<RecentItem>,
        projects: Map<Key, ProjectProfile>,
        target: (RecentItem) -> RecentTarget?,
        limit: Int = RecentItem.MAX,
    ): List<RecentCard> = items.sortedByDescending { it.openedAt }.mapNotNull { item ->
        val project = projects[item.projectKey] ?: return@mapNotNull null
        if (project.trashed || project.hidden) return@mapNotNull null
        val t = target(item) ?: return@mapNotNull null
        if (t.hidden || t.trashed) return@mapNotNull null
        if (project.protections.nameMask) {
            RecentCard(item, "${kindWord(item.kind)} · ${RatingLabels.MASKED_PROJECT}", null, item.openedAt)
        } else {
            RecentCard(item, "${kindWord(item.kind)} · ${project.name}", t.title, item.openedAt)
        }
    }.take(limit)
}
