package io.github.lenticualrstone.civilight.feature.story.common

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.actions.SceneDeleteResult
import io.github.lenticualrstone.civilight.domain.story.structure.NewUnit
import io.github.lenticualrstone.civilight.domain.story.structure.NewUnitResult
import io.github.lenticualrstone.civilight.domain.story.structure.UnitSettingsEdit
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.SceneNotation

/**
 * The story screens' edits as pure `ProjectDoc` functions over the one write path (DECISIONS §1):
 * scene delete through `domain:actions` [SceneActions]; new episode / chapter, `완료` and `때 표기`
 * through `domain:story`. Each returns null (or the same doc) when its precondition no longer holds,
 * so a double tap or a stale menu never throws inside a store transform.
 */
object StoryActions {
    /** `씬 지우기` (STO-10): null when [sceneKey] is not a live scene any more. */
    fun deleteScene(doc: ProjectDoc, sceneKey: Key, ctx: ActionContext): SceneDeleteResult? {
        val scene = doc.index.unit(sceneKey) ?: return null
        if (scene.level != doc.project.structure.sceneLevel || !doc.liveness.isUnitLive(sceneKey)) return null
        return SceneActions.delete(doc, sceneKey, ctx)
    }

    /** Undo label and toast subject of `씬 지우기`: `씬 3 지움`. */
    fun deleteLabel(doc: ProjectDoc, sceneKey: Key): String =
        StoryCopy.deleted(doc.index.unit(sceneKey)?.let(UnitCodes(doc)::sceneLabel) ?: StoryCopy.UNTITLED)

    /** `되돌리기` on the delete toast: row-level revert of exactly that delete. */
    fun undoDelete(current: ProjectDoc, result: SceneDeleteResult): ProjectDoc = SceneActions.undo(current, result)

    /**
     * A new unit of [unitKey]'s level directly after it, under the same parent (`새 에피소드` from the
     * episode ⋯; `ED` new chapters also get their first episode, see [NewUnit.create]).
     */
    fun newUnitAfter(doc: ProjectDoc, unitKey: Key, ctx: ActionContext): NewUnitResult? {
        val unit = doc.index.unit(unitKey)?.takeIf { doc.liveness.isUnitLive(it.key) } ?: return null
        return NewUnit.create(doc, unit.parentKey, unit.level, unit.key, ctx.edit)
    }

    /** A new top-level unit after the last one (`새 장` from the structure ⋯); null in a 1-level structure. */
    fun newTopUnit(doc: ProjectDoc, ctx: ActionContext): NewUnitResult? {
        if (!NewUnit.topLevelCreatable(doc)) return null
        val last = doc.index.children(null).lastOrNull { it.level == 0 && doc.liveness.isUnitLive(it.key) }
        return NewUnit.create(doc, null, 0, last?.key, ctx.edit)
    }

    /** The author-set `완료` flag (STO-06). */
    fun setDone(doc: ProjectDoc, unitKey: Key, done: Boolean, ctx: ActionContext): ProjectDoc =
        UnitSettingsEdit.setDone(doc, unitKey, done, ctx.edit)

    /** `때 표기` (STO-17). */
    fun setNotation(doc: ProjectDoc, notation: SceneNotation, ctx: ActionContext): ProjectDoc =
        UnitSettingsEdit.setNotation(doc, notation, ctx.edit)
}
