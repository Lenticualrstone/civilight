package io.github.lenticualrstone.civilight.feature.canvas.common

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** The rule-module contexts of one canvas edit, from the one [EditContext] a view model makes. */
fun EditContext.actions(): ActionContext = ActionContext(now = now, zone = zone, device = device, newKey = newKey)

/** What a `되살리기` did: the new document (null when nothing changed) and its toast, or that it needs `지문으로 보기`. */
data class RestoreResult(val doc: ProjectDoc?, val toast: Toast?, val needsAuth: Boolean = false)

/**
 * The fix buttons of a reference to a deleted item (rule 20 §2), shared by page links (CNV-16) and canvas
 * item nodes (CNV-11). Writes go through `domain:actions` and `domain:trash` only (DECISIONS §1).
 */
object RefFixes {
    /** `이대로 두기`: `Fixes.keep` on the item; its check-center row moves to `맞다고 표시한 것`, the dotted marks stay. */
    fun keep(doc: ProjectDoc, item: ItemId, ctx: EditContext): ProjectDoc = Fixes.keep(doc, item.key, ctx.actions().trash).doc

    fun isKept(doc: ProjectDoc, item: ItemId): Boolean = Fixes.isKept(doc, item.key)

    /** `되살리기`: characters through `CharacterActions`, every other kind through `RestoreOps` by its trash entry. */
    fun restore(doc: ProjectDoc, item: ItemId, ctx: EditContext, authenticated: Boolean): RestoreResult {
        val actx = ctx.actions()
        if (item.kind == ItemKind.CHARACTER) {
            val r = CharacterActions.restore(doc, item.key, actx, authenticated)
            return when (val o = r.outcome) {
                is RestoreOutcome.Restored -> RestoreResult(o.doc, r.toast)
                RestoreOutcome.NeedsAuth -> RestoreResult(null, null, needsAuth = true)
                else -> RestoreResult(null, null)
            }
        }
        val entryKey = rowOf(doc, item)?.trashEntryKey ?: return RestoreResult(null, null)
        return when (val o = RestoreOps.restore(doc, entryKey, actx.trash, authenticated)) {
            is RestoreOutcome.Restored -> RestoreResult(o.doc, Messages.restoreToast(o.report))
            RestoreOutcome.NeedsAuth -> RestoreResult(null, null, needsAuth = true)
            else -> RestoreResult(null, null)
        }
    }

    /** True when the item sits in the trash itself (restorable), not only as a name tag. */
    fun inTrash(doc: ProjectDoc, item: ItemId): Boolean = rowOf(doc, item)?.let { it.deletedAt != null && it.trashEntryKey != null } == true

    private fun rowOf(doc: ProjectDoc, item: ItemId): Trashable? = when (item.kind) {
        ItemKind.CHARACTER -> doc.index.character(item.key)
        ItemKind.TERM -> doc.index.term(item.key)
        ItemKind.PLACE -> doc.index.place(item.key)
        ItemKind.FACTION -> doc.index.faction(item.key)
        ItemKind.EVENT -> doc.index.event(item.key)
        ItemKind.PAGE -> doc.pages[item.key]
        else -> null
    }
}
