package io.github.lenticualrstone.civilight.feature.story.bulk

import io.github.lenticualrstone.civilight.domain.story.bulk.BulkFilter
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkGroup
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkList
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkProgress
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkRow
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.domain.story.bulk.Suggestions
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** One filter chip: `전체 31` · `장소 · 때 빈 것 12` · `제안 있음 3`. */
data class BulkChip(val filter: BulkFilter, val label: String)

/**
 * Everything 장소 · 때 한꺼번에 draws (STO-31 `BulkList`, STO-35 `BulkWide`).
 *
 * @property head `잿빛 새벽의 등불 · 1장` (cover) and [wideHead] `… · 펼친 화면` (inner).
 * @property selected the chosen scenes still in scope, manuscript order.
 * @property footer `고른 씬 4개 · 1-6 씬 2 · …`, null while nothing is chosen.
 * @property emptyChip the inner head's `장소 · 때 빈 것 12`.
 */
data class BulkUi(
    val head: String,
    val wideHead: String,
    val title: String,
    val place: BulkProgress,
    val time: BulkProgress,
    val chips: List<BulkChip>,
    val filter: BulkFilter,
    val groups: List<BulkGroup>,
    val selected: List<Key>,
    val footer: String?,
    val wideCount: String,
    val suggestCount: Int,
    val placeButton: String,
    val timeButton: String,
    val suggestButton: String,
    val emptyChip: BulkChip,
    val allSelected: Boolean,
) {
    val rows: List<BulkRow> get() = groups.flatMap { it.rows }

    /** `장소 정하기` · `때 정하기` need a choice; `제안 받기 n` also needs n > 0 (`ED`). */
    fun canApply(readOnly: Boolean): Boolean = !readOnly && selected.isNotEmpty()

    /** `제안 받기 n` opens the page whenever a chosen scene has a proposal (read-only: to read; `넣기` is off). */
    fun canSuggest(): Boolean = selected.isNotEmpty() && suggestCount > 0
}

/**
 * 장소 · 때 한꺼번에 as plain data. The scope is one chapter (Q23 `ED`; `domain:story` [BulkList]); in a
 * 1-level structure, where no chapter exists, it is every live scene. Hidden scenes are left out of the
 * scope while the project's hidden items are locked (R19 §6: their names and values never show).
 */
object BulkModel {
    /** Where `한꺼번에 ›` (점검 센터) leaves the scenes to choose: `deps.results` under this key. */
    fun preselectKey(projectKey: Key): String = "story.bulk.preselect.$projectKey"

    /**
     * The chapter the screen shows: the chapter of the first preselected scene, else of the scene being
     * written (`이어 쓰기`), else the first chapter with a live scene; null in a 1-level structure.
     */
    fun chapterKey(doc: ProjectDoc, preselected: List<Key> = emptyList()): Key? {
        if (doc.project.structure.sceneLevel == 0) return null
        val codes = UnitCodes(doc)
        val scene = preselected.firstNotNullOfOrNull { k -> doc.index.unit(k)?.takeIf { s -> doc.manuscript.liveScenes.any { it.key == s.key } } }
            ?: StoryOverview(doc).currentSceneKey?.let(doc.index::unit)
            ?: doc.manuscript.liveScenes.firstOrNull()
            ?: return null
        return codes.chapterOf(scene).key
    }

    /** The scenes in scope, manuscript order. */
    fun scope(doc: ProjectDoc, chapterKey: Key?, locked: Boolean): List<StoryUnit> {
        val live = doc.manuscript.liveScenes
        val inChapter = if (chapterKey == null || doc.index.unit(chapterKey) == null) {
            if (doc.project.structure.sceneLevel == 0) live else emptyList()
        } else {
            live.filter { s -> s.key == chapterKey || doc.index.ancestors(s).any { it.key == chapterKey } }
        }
        return if (locked) inChapter.filterNot { it.hidden } else inChapter
    }

    /** A [BulkList] for rows and suggestions: on the chapter, else on any live scene (1-level). */
    fun list(doc: ProjectDoc, chapterKey: Key?): BulkList? {
        val key = chapterKey?.takeIf { doc.index.unit(it) != null } ?: doc.manuscript.liveScenes.firstOrNull()?.key ?: return null
        return BulkList(doc, key)
    }

    fun build(doc: ProjectDoc, chapterKey: Key?, filter: BulkFilter, selected: Collection<Key>, locked: Boolean): BulkUi? {
        val list = list(doc, chapterKey) ?: return null
        val scenes = scope(doc, chapterKey, locked)
        val codes = UnitCodes(doc)
        val suggested = scenes.filter { s -> list.suggestions.suggestionFor(s.key)?.let { visible(doc, it.place?.ref, locked) } == true }.map { it.key }.toSet()
        fun filtered(f: BulkFilter): List<StoryUnit> = when (f) {
            BulkFilter.ALL -> scenes
            BulkFilter.EMPTY -> scenes.filter { !list.hasPlace(it) || !list.hasTime(it) }
            BulkFilter.SUGGESTED -> scenes.filter { it.key in suggested }
        }
        fun chip(f: BulkFilter): BulkChip = BulkChip(f, BulkCopy.chip(f, filtered(f).size))
        val chapter = chapterKey?.let(doc.index::unit)
        val head = listOfNotNull(doc.project.name, chapter?.let(codes::levelLabel)).joinToString(BulkCopy.SEP)
        val shown = filtered(filter)
        val groups = shown.groupBy { s -> codes.episodeOf(s)?.let(codes::episodeCode) ?: chapter?.let(codes::levelLabel).orEmpty() }
            .map { (code, rows) -> BulkGroup(code, rows.map { masked(doc, list.row(it), list.suggestions, locked) }) }
        val chosen = scenes.map { it.key }.filter { it in selected }
        val suggestCount = chosen.count { it in suggested }
        val count = BulkCopy.chosenCount(chosen.size)
        return BulkUi(
            head = head,
            wideHead = listOf(head, BulkCopy.WIDE).joinToString(BulkCopy.SEP),
            title = BulkCopy.TITLE,
            place = BulkProgress(BulkCopy.PLACE, scenes.count(list::hasPlace), scenes.size),
            time = BulkProgress(BulkCopy.TIME, scenes.count(list::hasTime), scenes.size),
            chips = BulkFilter.entries.map(::chip),
            filter = filter,
            groups = groups,
            selected = chosen,
            footer = if (chosen.isEmpty()) null else (listOf(count) + chosen.map { codes.sceneRef(it).orEmpty() }).joinToString(BulkCopy.SEP),
            wideCount = count,
            suggestCount = suggestCount,
            placeButton = BulkCopy.SET_PLACE,
            timeButton = BulkCopy.SET_TIME,
            suggestButton = BulkCopy.suggestButton(suggestCount),
            emptyChip = chip(BulkFilter.EMPTY),
            allSelected = shown.isNotEmpty() && shown.all { it.key in selected },
        )
    }

    /** A place ref may be shown: not a hidden place while the project's hidden items are locked (R19 §6). */
    fun visible(doc: ProjectDoc, ref: PlaceRef?, locked: Boolean): Boolean =
        !(locked && ref != null && ref.kind.known == PlaceRefKind.PLACE && doc.index.place(ref.key)?.hidden == true)

    /** The row with a locked hidden place read as `숨긴 항목` and no proposal word from it. */
    private fun masked(doc: ProjectDoc, row: BulkRow, suggestions: Suggestions, locked: Boolean): BulkRow {
        if (!locked) return row
        val place = doc.index.unit(row.sceneKey)?.place
        val hidePlace = !visible(doc, place, locked)
        val proposal = if (row.suggestion != null) suggestions.suggestionFor(row.sceneKey) else null
        val hideProposal = proposal != null && !visible(doc, proposal.place?.ref, locked)
        return row.copy(
            place = if (hidePlace) BulkCopy.HIDDEN_PLACE else row.place,
            suggestion = if (hideProposal) null else row.suggestion,
        )
    }

    /** Hidden places are left out of the place list and the map while locked. */
    fun hiddenPlaces(doc: ProjectDoc, locked: Boolean): Set<Key> =
        if (!locked) emptySet() else doc.world.places.filter { it.hidden }.map { it.key }.toSet()

    /** One row's checkbox. */
    fun toggle(selected: List<Key>, key: Key): List<Key> = if (key in selected) selected - key else selected + key

    /**
     * `모두 고르기`: every scene the filter shows is chosen; when all of them already are, they are let go
     * (`ED`: the drawn button is the only bulk control, no clearing copy is drawn).
     */
    fun selectAll(ui: BulkUi, selected: List<Key>): List<Key> {
        val shown = ui.rows.map { it.sceneKey }
        return if (ui.allSelected) selected - shown.toSet() else selected + shown.filterNot { it in selected }
    }

    /** The preselection handed over by `한꺼번에 ›`, if it is a list of keys. */
    fun preselection(value: Any?): List<Key>? = (value as? List<*>)?.filterIsInstance<String>()
}
