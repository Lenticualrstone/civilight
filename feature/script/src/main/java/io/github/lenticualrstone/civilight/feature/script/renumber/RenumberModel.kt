package io.github.lenticualrstone.civilight.feature.script.renumber

import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.Numbering
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * `번호 다시 매기기 · 1-7` (SCR-22): which numbering unit the shared `RenumberSheet` names, its
 * preview, and the one-step apply. The lines (title, figures, rows, footer) are `domain:script`
 * [Numbering]'s, as board `RTRenumber` draws them.
 */
object RenumberModel {
    /**
     * The numbering unit of `RenumberSheet.episodeKey`: a live unit at the episode level, or the
     * project key in a structure without an episode level (1-level projects number per project).
     */
    fun unitOf(doc: ProjectDoc, key: Key): Key? {
        val level = doc.project.structure.episodeLevel
        if (key == doc.project.key) return key.takeIf { level == null }
        val unit = doc.index.unit(key) ?: return null
        if (level == null || unit.level != level || !doc.manuscript.isLiveUnit(unit.key)) return null
        return unit.key
    }

    /** The preview, or null when [key] names no numbering unit any more (a stale sheet). */
    fun preview(doc: ProjectDoc, key: Key): Numbering.RenumberPreview? =
        unitOf(doc, key)?.let { Numbering.renumberPreview(doc, it) }

    /** `다시 매기기` is offered only when a number would change and the project is writable (`ED`). */
    fun canApply(preview: Numbering.RenumberPreview?, readOnly: Boolean): Boolean =
        preview != null && preview.changed > 0 && !readOnly

    /** Live blocks get 1…N in manuscript order, as one step; a stale sheet leaves the doc as it is. */
    fun apply(doc: ProjectDoc, key: Key, ctx: EditContext): ProjectDoc {
        val unit = unitOf(doc, key) ?: return doc
        return Numbering.renumber(doc, unit, ctx)
    }
}
