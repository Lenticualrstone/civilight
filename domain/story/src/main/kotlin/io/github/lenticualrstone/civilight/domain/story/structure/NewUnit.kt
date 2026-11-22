package io.github.lenticualrstone.civilight.domain.story.structure

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.scene.NewScene
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** What [NewUnit.create] made: the project after it, the new unit and every unit created under it. */
data class NewUnitResult(val doc: ProjectDoc, val unitKey: Key, val createdKeys: List<Key>)

/**
 * New units above the scene level (STO-05: `새 에피소드 · 새 장`, created only from the story
 * structure). Scenes are made by `NewScene` (through `SceneActions.create`).
 */
object NewUnit {
    /**
     * Creates an empty unit at [level] under [parentKey], directly after the live sibling [afterKey]
     * (null = after the last child). [level] must be above the scene level and one below the
     * parent's level (0 for a top-level unit).
     *
     * `ED`: a unit above the episode level also gets one empty unit on every level down to the
     * episode level (a new chapter starts with its first episode), so `새 씬` has an episode to
     * fill. Episode-level units are numbering units (§4.1) and start their block counter at 1.
     * No other unit's order, code, 때 or data changes. Returns null when the position is invalid
     * (unknown or trashed parent, [afterKey] not a live child, wrong level).
     */
    fun create(
        doc: ProjectDoc,
        parentKey: Key?,
        level: Int,
        afterKey: Key?,
        ctx: EditContext,
        title: String? = null,
    ): NewUnitResult? {
        val structure = doc.project.structure
        if (level < 0 || level >= structure.sceneLevel) return null
        val parent = parentKey?.let { doc.index.unit(it) ?: return null }
        if (parent != null && (!doc.liveness.isUnitLive(parent.key) || parent.level != level - 1)) return null
        if (parent == null && level != 0) return null
        val live = doc.index.children(parentKey).filter { it.level == level && doc.liveness.isUnitLive(it.key) }
        val after = when {
            afterKey != null -> afterKey.takeIf { k -> live.any { it.key == k } } ?: return null
            else -> doc.index.children(parentKey).lastOrNull()?.key
        }
        val created = ArrayList<StoryUnit>()
        var d = doc
        var parentOfNext: Key? = parentKey
        var afterOfNext: Key? = after
        var l = level
        val episodeLevel = structure.episodeLevel ?: return null
        while (l <= episodeLevel) {
            val unit = StoryUnit(
                key = ctx.newKey(),
                parentKey = parentOfNext,
                level = l,
                order = NewScene.orderAfter(d, parentOfNext, afterOfNext),
                title = if (l == level) title?.trim()?.ifEmpty { null } else null,
                nextBlockNumber = if (l == episodeLevel) 1 else null,
                updatedAt = ctx.now,
                updatedBy = ctx.device,
            )
            created += unit
            d = d.copy(story = d.story.copy(units = d.story.units + unit))
            parentOfNext = unit.key
            afterOfNext = null
            l++
        }
        return NewUnitResult(d, created.first().key, created.map { it.key })
    }

    /** The level a structure-menu `새 장` creates: the top level, when it is above the scene level. */
    fun topLevelCreatable(doc: ProjectDoc): Boolean = doc.project.structure.sceneLevel > 0
}
