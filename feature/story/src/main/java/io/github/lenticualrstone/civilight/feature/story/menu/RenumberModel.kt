package io.github.lenticualrstone.civilight.feature.story.menu

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.Numbering
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * `번호 다시 매기기 · 1-7` from the episode ⋯ (STO-07 entry, SCR-22 rules in `domain:script`
 * [Numbering]): the episode is its own numbering unit, so its key goes straight in. The preview's
 * lines (title, figures, rows, footer) are `domain:script`'s, as on `RTRenumber`.
 */
object RenumberModel {
    /** The preview, or null when [episodeKey] is not a live episode-level unit any more. */
    fun preview(doc: ProjectDoc, episodeKey: Key): Numbering.RenumberPreview? {
        val unit = doc.index.unit(episodeKey) ?: return null
        val episodeLevel = doc.project.structure.episodeLevel ?: return null
        if (unit.level != episodeLevel || !doc.liveness.isUnitLive(unit.key)) return null
        return Numbering.renumberPreview(doc, episodeKey)
    }

    /** `다시 매기기` is offered only when a number would change (`ED`). */
    fun canApply(preview: Numbering.RenumberPreview?, readOnly: Boolean): Boolean =
        preview != null && preview.changed > 0 && !readOnly

    /** Applies it as one step; a stale sheet (episode gone) leaves the doc as it is. */
    fun apply(doc: ProjectDoc, episodeKey: Key, ctx: ActionContext): ProjectDoc {
        preview(doc, episodeKey) ?: return doc
        return Numbering.renumber(doc, episodeKey, EditContext(now = ctx.now, zone = ctx.zone, device = ctx.device, newKey = ctx.newKey))
    }
}
