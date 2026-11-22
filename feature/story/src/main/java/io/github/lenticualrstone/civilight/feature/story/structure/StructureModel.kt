package io.github.lenticualrstone.civilight.feature.story.structure

import io.github.lenticualrstone.civilight.domain.story.search.CommandBar
import io.github.lenticualrstone.civilight.domain.story.search.CommandScreen
import io.github.lenticualrstone.civilight.domain.story.structure.ChapterRow
import io.github.lenticualrstone.civilight.domain.story.structure.EpisodeRow
import io.github.lenticualrstone.civilight.domain.story.structure.SceneRow
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** One line of the 스토리 list (STO-04), in screen order. */
sealed interface StructureItem {
    val id: String
}

/**
 * A unit above the episode level (`Ⅰ 1장 · 등불지기 | 7/10`). A planned one (`0/8 · 계획`) shows its
 * synopsis instead of rows; `ED`: tapping its header shows or hides its rows ([expandable]).
 */
data class GroupItem(val row: ChapterRow, val depth: Int, val expandable: Boolean, val expanded: Boolean) : StructureItem {
    override val id: String get() = "group:${row.key}"
}

/** `1-5 | 첫 번째 신호 | 씬 3개`; the title is bold when expanded or [current] (the episode of the last edit). */
data class EpisodeItem(val row: EpisodeRow, val current: Boolean) : StructureItem {
    override val id: String get() = "episode:${row.key}"
}

/** `1일째 새벽 | 씬 1 · 새벽 순찰 | 1,820자`, indented under its episode (not in a 1-level structure). */
data class SceneItem(val row: SceneRow, val indented: Boolean) : StructureItem {
    override val id: String get() = "scene:${row.key}"
}

/** Where `새 씬` creates its scene: under [parentKey] right after [afterKey] (null = first, an empty episode). */
data class NewSceneTarget(val parentKey: Key, val afterKey: Key?)

/** Everything 스토리 draws (STO-04, board `Story`). */
data class StructureUi(
    val projectKey: Key,
    val projectName: String,
    /** `단계 이름: 장 › 에피소드 › 씬`. */
    val levelLine: String,
    val items: List<StructureItem>,
    /** Null when `새 씬` has no episode to go to, or in a 1-level structure (see Requests). */
    val newScene: NewSceneTarget?,
    val readOnly: Boolean,
    val commandHint: String,
)

/**
 * The 스토리 list as plain data (STO-04, STO-06, STO-11 masks), for any hierarchy of 1 to 4 levels
 * (STO-02): units above the episode level are group headers, episode-level units are expandable
 * rows, scene-level units are rows under an expanded episode (or the whole list at 1 level).
 * [expanded] is the screen's saved list of expanded keys, most recently expanded last.
 */
object StructureModel {
    fun build(doc: ProjectDoc, expanded: List<Key>, locked: Boolean, readOnly: Boolean): StructureUi {
        val view = StoryOverview(doc, locked = locked)
        val structure = doc.project.structure
        val episodeLevel = structure.episodeLevel
        val currentEpisode = view.currentSceneKey?.let { view.codes.episodeOf(it) }?.key
        val open = expanded.toSet()
        val items = ArrayList<StructureItem>()
        fun visit(parentKey: Key?, depth: Int) {
            for (unit in doc.index.children(parentKey)) {
                if (!doc.liveness.isUnitLive(unit.key)) continue
                when (unit.level) {
                    structure.sceneLevel -> items += SceneItem(view.sceneRow(unit), indented = episodeLevel != null)
                    episodeLevel -> {
                        val isOpen = unit.key in open
                        items += EpisodeItem(view.episodeRow(unit, isOpen), current = unit.key == currentEpisode)
                        if (isOpen) visit(unit.key, depth + 1)
                    }
                    else -> {
                        val row = view.chapterRow(unit)
                        val isOpen = !row.planned || unit.key in open
                        items += GroupItem(row, depth, expandable = row.planned, expanded = isOpen)
                        if (isOpen) visit(unit.key, depth + 1)
                    }
                }
            }
        }
        visit(null, 0)
        return StructureUi(
            projectKey = doc.project.key,
            projectName = doc.project.name,
            levelLine = view.levelLine,
            items = items,
            newScene = newSceneTarget(view, expanded),
            readOnly = readOnly,
            commandHint = CommandBar.placeholder(CommandScreen.STORY),
        )
    }

    /**
     * Expanded on entry (`ED`, STO-04 "exactly the current episode"): the episode of [focusUnitKey]
     * (an episode, or a scene's episode), else the episode of the last edit; planned groups above it
     * are opened too so it is visible.
     */
    fun initialExpanded(doc: ProjectDoc, focusUnitKey: Key?): List<Key> {
        val view = StoryOverview(doc)
        val focus = focusUnitKey?.let(doc.index::unit)?.takeIf { doc.liveness.isUnitLive(it.key) }
        val target = when {
            focus == null -> view.currentSceneKey?.let { view.codes.episodeOf(it) }
            focus.level == doc.project.structure.sceneLevel -> view.codes.episodeOf(focus)
            focus.level == doc.project.structure.episodeLevel -> focus
            else -> focus
        } ?: return emptyList()
        val plannedAbove = doc.index.ancestors(target).reversed().filter { view.chapterRow(it).planned }.map { it.key }
        return plannedAbove + target.key
    }

    /** Tap on an episode row or a planned group header: shows or hides it; the newest stays last. */
    fun toggle(expanded: List<Key>, key: Key): List<Key> = if (key in expanded) expanded - key else expanded + key

    /**
     * `새 씬` (STO-05, `ED` insertion point): in the most recently expanded episode, else the episode
     * of the last edit; after the scene being written when it is in that episode, else after its last
     * scene. Null in a 1-level structure (`SceneActions.create` needs a parent, see Requests).
     */
    fun newSceneTarget(view: StoryOverview, expanded: List<Key>): NewSceneTarget? {
        val doc = view.doc
        val episodeLevel = doc.project.structure.episodeLevel ?: return null
        val episode = expanded.asReversed().firstNotNullOfOrNull { key ->
            doc.index.unit(key)?.takeIf { it.level == episodeLevel && doc.liveness.isUnitLive(it.key) }
        } ?: view.currentSceneKey?.let { view.codes.episodeOf(it) } ?: return null
        val scenes = view.scenesOf(episode)
        val after = view.currentSceneKey?.takeIf { k -> scenes.any { it.key == k } } ?: scenes.lastOrNull()?.key
        return NewSceneTarget(episode.key, after)
    }
}
