package io.github.lenticualrstone.civilight.feature.world.list

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.item.WorldList
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermListRow
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.WorldTab
import io.github.lenticualrstone.civilight.ui.lens.LensRules

/** The four segments `장소 · 용어 · 세력 · 지도` (WLD-01); 지도 is feature:map's `MapList`. */
enum class WorldSegment(val label: String, val tab: WorldTab?) {
    PLACES(WorldCopy.TAB_PLACES, WorldTab.PLACES),
    TERMS(WorldCopy.TAB_TERMS, WorldTab.TERMS),
    FACTIONS(WorldCopy.TAB_FACTIONS, WorldTab.FACTIONS),
    MAPS(WorldCopy.TAB_MAPS, null),
    ;

    companion object {
        val labels: List<String> = entries.map { it.label }

        fun of(tab: WorldTab): WorldSegment = entries.first { it.tab == tab }
    }
}

/** Which tab opens (WLD-01: remembered per project `[PROPOSED]`, in `_app/view.json › worldTab`). */
object WorldTabs {
    /**
     * A route naming another tab than the default wins (a link into `장소`); the default route
     * opens the tab remembered for the project, else `용어`.
     */
    fun initial(routeTab: WorldTab, remembered: String?): WorldTab {
        if (routeTab != WorldTab.TERMS) return routeTab
        return WorldTab.entries.firstOrNull { it.name == remembered } ?: WorldTab.TERMS
    }

    fun kindOf(tab: WorldTab): WorldKind = when (tab) {
        WorldTab.TERMS -> WorldKind.TERM
        WorldTab.PLACES -> WorldKind.PLACE
        WorldTab.FACTIONS -> WorldKind.FACTION
    }
}

/** One line of a world list, in order. */
sealed interface WorldListItem {
    val id: String

    /** `.sec` header: `공개됨 · 최신 · 1-7`, `아직 공개 전`, a place's `지역`. */
    data class Section(val title: String) : WorldListItem {
        override val id: String get() = "sec:$title"
    }

    /**
     * A row (`.li`): name, first line of the description, right column. Terms split the right column
     * into [right] (`첫 공개 1-7`), [candidates] (`◇ 후보 1`, `ink3`) and [warning]
     * (`▲ 앞에서 1회 사용`, `warn`).
     */
    data class Row(
        val kind: WorldKind,
        val key: Key,
        val title: String,
        val fragment: String,
        val right: String,
        val candidates: String? = null,
        val warning: String? = null,
    ) : WorldListItem {
        override val id: String get() = "row:$key"
    }

    /** `숨긴 용어 N` with `지문으로 보기` (rule 19). */
    data class Lock(val text: String) : WorldListItem {
        override val id: String get() = "lock"
    }

    /**
     * No items yet: `용어 없음`. (0.2.0, WLD-02) The primary [button] is superseded by the list's create row
     * and is no longer drawn; the line stays under the create row.
     */
    data class Empty(val line: String, val button: String) : WorldListItem {
        override val id: String get() = "empty"
    }

    /**
     * (0.2.0, WLD-73) A row of the `용어 후보 · N` section: term name, `후보 3`, and `◇ 공개 전 후보 1` when
     * some candidates come before the reveal. A tap opens the term card.
     */
    data class CandidateRow(val key: Key, val title: String, val fragment: String, val right: String?) : WorldListItem {
        override val id: String get() = "cand:$key"
    }
}

/** Everything the 세계 list screen draws. */
data class WorldListUi(
    val projectName: String,
    val tab: WorldTab,
    val items: List<WorldListItem>,
    /** The candidate scan is still running (`◇ 후보` numbers are not final). */
    val candidatesPending: Boolean,
)

/** Builds the 세계 list for one tab (WLD-02, WLD-03, WLD-04) at the lens; values follow the lens (WLD-12). */
object WorldListState {
    fun build(doc: ProjectDoc, tab: WorldTab, index: CandidateIndex, hiddenOpen: Boolean): WorldListUi {
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val items = when (tab) {
            WorldTab.TERMS -> termItems(doc, lens, index, hiddenOpen)
            WorldTab.PLACES -> worldItems(WorldKind.PLACE, WorldCards(doc, hiddenOpen).placeList())
            WorldTab.FACTIONS -> worldItems(WorldKind.FACTION, WorldCards(doc, hiddenOpen).factionList())
        }
        return WorldListUi(doc.project.name, tab, items, candidatesPending = tab == WorldTab.TERMS && doc.project.termCandidates && !index.complete)
    }

    private fun termItems(doc: ProjectDoc, lens: LensPoint, index: CandidateIndex, hiddenOpen: Boolean): List<WorldListItem> {
        val queries = TermQueries(doc, index, TermAccess(hiddenOpen))
        val list = queries.list(lens)
        val out = ArrayList<WorldListItem>()
        list.lockRow?.let { out += WorldListItem.Lock(it) }
        if (list.emptyLine != null) {
            out += WorldListItem.Empty(list.emptyLine!!, WorldCopy.CREATE_TERM)
            return out
        }
        if (list.revealed.isNotEmpty()) {
            out += WorldListItem.Section(list.revealedTitle)
            list.revealed.mapTo(out, ::termRow)
        }
        if (list.pending.isNotEmpty()) {
            out += WorldListItem.Section(list.pendingTitle)
            list.pending.mapTo(out, ::termRow)
        }
        // (0.2.0, WLD-73) The candidate list is the last section while the project's `용어 후보` is on.
        queries.candidateSection(lens)?.let { section ->
            out += WorldListItem.Section(section.title)
            section.rows.mapTo(out) { WorldListItem.CandidateRow(it.termKey, it.name, it.fragment, it.right) }
        }
        return out
    }

    /** Splits `첫 공개 1-7 · ◇ 후보 1` into the label and the `◇` part (drawn in `ink3`). */
    fun termRow(row: TermListRow): WorldListItem.Row {
        val cut = row.line1.indexOf("${Copy.SEP}◇")
        val right = if (cut < 0) row.line1 else row.line1.substring(0, cut)
        val candidates = if (cut < 0) null else row.line1.substring(cut + Copy.SEP.length)
        return WorldListItem.Row(WorldKind.TERM, row.termKey, row.name, row.fragment, right, candidates, row.line2)
    }

    private fun worldItems(kind: WorldKind, list: WorldList): List<WorldListItem> {
        val out = ArrayList<WorldListItem>()
        list.lockRow?.let { out += WorldListItem.Lock(it) }
        if (list.emptyLine != null) {
            out += WorldListItem.Empty(list.emptyLine!!, WorldCopy.createLabel(kind))
            return out
        }
        for (section in list.sections) {
            section.title?.let { out += WorldListItem.Section(it) }
            section.rows.mapTo(out) { WorldListItem.Row(kind, it.key, it.name, it.fragment, it.right) }
        }
        return out
    }
}
