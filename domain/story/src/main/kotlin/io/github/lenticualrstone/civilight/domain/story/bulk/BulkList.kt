package io.github.lenticualrstone.civilight.domain.story.bulk

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** Filter chips `전체 31` · `장소 · 때 빈 것 12` · `제안 있음 3` (single choice). */
enum class BulkFilter { ALL, EMPTY, SUGGESTED }

/** `장소 | 22/31` with a flat bar ([filled] / [total]). */
data class BulkProgress(val label: String, val filled: Int, val total: Int) {
    val text: String get() = "$label | $filled/$total"
    val fraction: Double get() = if (total == 0) 0.0 else filled.toDouble() / total
}

/**
 * A scene row: `씬 1 · 잿더미 시장` + values `잿더미 시장 | 1일째 낮` (empty values `장소 없음` /
 * `때 없음` in `ink3`); [suggestion] is the `제안` column word for the inner table.
 */
data class BulkRow(
    val sceneKey: Key,
    val sceneRef: String,
    val label: String,
    val place: String,
    val time: String,
    val placeEmpty: Boolean,
    val timeEmpty: Boolean,
    val suggestion: SuggestionSource?,
) {
    /** Inner table cells `1-6 씬 2 | + 장소 | + 때 | 배경`. */
    val tableCells: List<String>
        get() = listOf(
            sceneRef,
            if (placeEmpty) "+ 장소" else place,
            if (timeEmpty) "+ 때" else time,
            suggestion?.word?.removeSuffix(" 연출") ?: "—",
        )
}

/** Rows of one episode under its code (`1-6`). */
data class BulkGroup(val code: String, val rows: List<BulkRow>)

/**
 * `장소 · 때 한꺼번에` list (STO-31/35, `BulkList`, `BulkWide`) over the live scenes of one chapter
 * (`ED`, Q23). A scene is empty when it lacks a place or a 때 (회상 counts as a 때).
 */
class BulkList(
    val doc: ProjectDoc,
    val chapterKey: Key,
    placeAliases: Map<Key, List<String>> = emptyMap(),
) {
    private val codes = UnitCodes(doc)
    private val times = SceneTimes(doc)
    private val labels = TimeLabels(times, codes)
    private val places = PlaceNames(doc)
    val suggestions = Suggestions(doc, placeAliases, codes)

    val chapter: StoryUnit = requireNotNull(doc.index.unit(chapterKey)) { "unknown unit $chapterKey" }

    /** Live scenes of the chapter in manuscript order. */
    val scenes: List<StoryUnit> = doc.manuscript.liveScenes.filter { s -> s.key == chapterKey || doc.index.ancestors(s).any { it.key == chapterKey } }

    val head: String get() = "${doc.project.name} · ${codes.levelLabel(chapter)}"
    val title: String get() = "장소 · 때 한꺼번에"
    val wideHead: String get() = "$head · 펼친 화면"

    fun hasPlace(s: StoryUnit): Boolean = s.place != null
    fun hasTime(s: StoryUnit): Boolean = s.time != SceneTime.None && s.time !is SceneTime.Unknown

    val placeProgress: BulkProgress get() = BulkProgress("장소", scenes.count(::hasPlace), scenes.size)
    val timeProgress: BulkProgress get() = BulkProgress("때", scenes.count(::hasTime), scenes.size)

    /** `장소 22/31 · 때 19/31` (inner head). */
    val progressLine: String get() = "장소 ${placeProgress.filled}/${scenes.size} · 때 ${timeProgress.filled}/${scenes.size}"

    private val suggested: Set<Key> by lazy { scenes.filter { suggestions.hasSuggestion(it.key) }.map { it.key }.toSet() }

    fun count(filter: BulkFilter): Int = filtered(filter).size

    /** `전체 31`, `장소 · 때 빈 것 12`, `제안 있음 3`. */
    fun chip(filter: BulkFilter): String = when (filter) {
        BulkFilter.ALL -> "전체 ${count(filter)}"
        BulkFilter.EMPTY -> "장소 · 때 빈 것 ${count(filter)}"
        BulkFilter.SUGGESTED -> "제안 있음 ${count(filter)}"
    }

    fun filtered(filter: BulkFilter): List<StoryUnit> = when (filter) {
        BulkFilter.ALL -> scenes
        BulkFilter.EMPTY -> scenes.filter { !hasPlace(it) || !hasTime(it) }
        BulkFilter.SUGGESTED -> scenes.filter { it.key in suggested }
    }

    fun row(s: StoryUnit): BulkRow = BulkRow(
        sceneKey = s.key,
        sceneRef = codes.sceneRef(s),
        label = "${codes.sceneLabel(s)} · ${s.title ?: "미정"}",
        place = places.label(s.place)?.wide ?: "장소 없음",
        time = labels.bulkValue(s.key),
        placeEmpty = !hasPlace(s),
        timeEmpty = !hasTime(s),
        suggestion = suggestions.suggestionFor(s.key)?.source,
    )

    /** Rows grouped by episode code, manuscript order. */
    fun groups(filter: BulkFilter = BulkFilter.ALL): List<BulkGroup> =
        filtered(filter).groupBy { codes.episodeOf(it)?.let(codes::episodeCode) ?: codes.levelLabel(chapter) }
            .map { (code, list) -> BulkGroup(code, list.map(::row)) }

    private fun ordered(selected: Collection<Key>): List<Key> = scenes.map { it.key }.filter { it in selected }

    /** `고른 씬 4개 · 1-6 씬 2 · 1-8 씬 1 · 1-8 씬 2 · 1-9 씬 1`. */
    fun footer(selected: Collection<Key>): String {
        val keys = ordered(selected)
        return (listOf("고른 씬 ${keys.size}개") + keys.map { codes.sceneRef(it).orEmpty() }).joinToString(" · ")
    }

    /** Scenes of the selection with a proposal (`제안 받기 3`; the button is disabled at 0, `ED`). */
    fun suggestCount(selected: Collection<Key>): Int = ordered(selected).count { it in suggested }

    /** `장소 정하기` · `때 정하기` · `제안 받기 3`. */
    fun buttons(selected: Collection<Key>): List<String> = listOf("장소 정하기", "때 정하기", "제안 받기 ${suggestCount(selected)}")

    /** Inner footer `고른 씬 4개 | 장소 정하기 | 때 정하기 | 제안 받기 3`. */
    fun wideFooter(selected: Collection<Key>): String = (listOf("고른 씬 ${ordered(selected).size}개") + buttons(selected)).joinToString(" | ")
}
