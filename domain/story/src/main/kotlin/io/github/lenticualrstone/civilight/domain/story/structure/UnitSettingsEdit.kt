package io.github.lenticualrstone.civilight.domain.story.structure

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.SceneNotation

/** Small project and unit settings owned by the story area (STO-06 `완료`, STO-17 `때 표기`). */
object UnitSettingsEdit {
    /**
     * The author-set `완료` flag of a live unit (STO-06, Q3). Labels and statuses are derived from it;
     * nothing else changes. Unknown or trashed units leave the doc unchanged.
     */
    fun setDone(doc: ProjectDoc, unitKey: Key, done: Boolean, ctx: EditContext): ProjectDoc {
        val unit = doc.index.unit(unitKey) ?: return doc
        if (!doc.liveness.isUnitLive(unitKey) || unit.doneMarked == done) return doc
        val changed = unit.copy(doneMarked = done, updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == unitKey) changed else it }))
    }

    /**
     * `때 표기` (STO-17): `에피소드 기준` or `이야기 시작부터`. Only labels change; stored 때 stays
     * byte-identical (R11 §6).
     */
    fun setNotation(doc: ProjectDoc, notation: SceneNotation, ctx: EditContext): ProjectDoc {
        val settings = doc.project.sceneTime
        if (settings.notation.known == notation) return doc
        val project = doc.project.copy(
            sceneTime = settings.copy(notation = notation.open()),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return doc.copy(project = project)
    }
}
