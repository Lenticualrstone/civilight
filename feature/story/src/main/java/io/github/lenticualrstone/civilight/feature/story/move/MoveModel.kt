package io.github.lenticualrstone.civilight.feature.story.move

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.story.scene.MovePreview
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** One place a unit can go: under [parentKey] after [afterKey] (null = `맨 앞`). */
data class MoveSlot(val parentKey: Key?, val afterKey: Key?, val label: String, val current: Boolean)

/**
 * `옮기기` (STO-08) as plain data: the destination slots of the explicit picker (no hidden gesture,
 * R12 §3), the scene move preview and the moves themselves. A scene move always shows the preview
 * (`SceneActions.move`); an episode or chapter moves directly (`SceneActions.reorderUnit`, `ED` Q6).
 * Every function is a no-op on a unit that is no longer live (a sheet restored after process death).
 */
object MoveModel {
    private fun live(doc: ProjectDoc, key: Key?): Boolean = key != null && doc.index.unit(key) != null && doc.liveness.isUnitLive(key)

    fun isScene(doc: ProjectDoc, key: Key): Boolean = doc.index.unit(key)?.level == doc.project.structure.sceneLevel

    /** Scene moves need a parent unit (`SceneMove.plan`): not offered in a 1-level structure. */
    fun movable(doc: ProjectDoc, unitKey: Key): Boolean {
        if (!live(doc, unitKey)) return false
        val unit = doc.index.unit(unitKey)!!
        return !(isScene(doc, unitKey) && doc.project.structure.levels.size == 1) && (unit.level == 0 || unit.parentKey != null)
    }

    /** Parents a unit may go under: live units one level up (null for the top level). */
    fun parentLevel(doc: ProjectDoc, unitKey: Key): Int? = doc.index.unit(unitKey)?.level?.minus(1)?.takeIf { it >= 0 }

    /** `맨 앞` and `씬 3 뒤` … under [parentKey], without the moving unit itself. */
    fun slots(doc: ProjectDoc, unitKey: Key, parentKey: Key?): List<MoveSlot> {
        val unit = doc.index.unit(unitKey) ?: return emptyList()
        val codes = UnitCodes(doc)
        val siblings = doc.index.children(parentKey).filter { it.level == unit.level && doc.liveness.isUnitLive(it.key) }
        val currentAfter = if (unit.parentKey == parentKey) siblings.getOrNull(siblings.indexOfFirst { it.key == unitKey } - 1)?.key else null
        val inPlace = unit.parentKey == parentKey
        val others = siblings.filter { it.key != unitKey }
        return listOf(MoveSlot(parentKey, null, MoveCopy.FIRST, current = inPlace && currentAfter == null)) +
            others.map { s ->
                val label = if (isScene(doc, s.key)) codes.sceneLabel(s) else codes.unitLabel(s)
                MoveSlot(parentKey, s.key, "$label ${MoveCopy.AFTER}", current = inPlace && currentAfter == s.key)
            }
    }

    /** The `RSSceneMove` preview, or null when the scene or the destination is gone. */
    fun preview(doc: ProjectDoc, sceneKey: Key, parentKey: Key, afterKey: Key?, ctx: ActionContext): MovePreview? {
        if (!live(doc, sceneKey) || !isScene(doc, sceneKey) || !live(doc, parentKey)) return null
        if (afterKey != null && (!live(doc, afterKey) || doc.index.unit(afterKey)!!.parentKey != parentKey)) return null
        if (doc.index.unit(parentKey)!!.level != doc.project.structure.sceneLevel - 1) return null
        return SceneActions.move(doc, sceneKey, parentKey, afterKey, ctx)
    }

    /** `옮기기` in the preview: recomputed on [doc] (one undo step); the doc unchanged when stale. */
    fun moveScene(doc: ProjectDoc, sceneKey: Key, parentKey: Key, afterKey: Key?, ctx: ActionContext): ProjectDoc =
        preview(doc, sceneKey, parentKey, afterKey, ctx)?.result ?: doc

    /** An episode or chapter to another slot: codes renumber, no 때 changes, no preview (`ED` Q6). */
    fun moveUnit(doc: ProjectDoc, unitKey: Key, parentKey: Key?, afterKey: Key?, ctx: ActionContext): ProjectDoc {
        if (!live(doc, unitKey) || isScene(doc, unitKey)) return doc
        val unit = doc.index.unit(unitKey)!!
        if (parentKey != null && (!live(doc, parentKey) || doc.index.unit(parentKey)!!.level != unit.level - 1)) return doc
        if (parentKey == null && unit.level != 0) return doc
        if (afterKey != null && (!live(doc, afterKey) || doc.index.unit(afterKey)!!.parentKey != parentKey)) return doc
        return SceneActions.reorderUnit(doc, unitKey, parentKey, afterKey, ctx)
    }
}
