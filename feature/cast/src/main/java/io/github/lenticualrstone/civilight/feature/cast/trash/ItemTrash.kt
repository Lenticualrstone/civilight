package io.github.lenticualrstone.civilight.feature.cast.trash

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.domain.cast.refs.DeleteImpact as CastImpact
import io.github.lenticualrstone.civilight.domain.cast.refs.DeletedItems
import io.github.lenticualrstone.civilight.domain.trash.DeleteImpact
import io.github.lenticualrstone.civilight.domain.trash.DocChange
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.Impact
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.SheetRow
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/** A count row of the impact sheet as `RCDeleteImpact` draws it: label (+ sub) left, count in mono right. */
data class ImpactRowView(val label: String, val count: String, val sub: String?)

/** The impact sheet (`RCDeleteImpact`, CST-40, S21) for one item of any named kind. */
data class ImpactSheetView(
    val title: String,
    val sub: String,
    val ownedTitle: String,
    val owned: List<ImpactRowView>,
    val stayingTitle: String,
    val staying: List<ImpactRowView>,
    /** The dotted sample at the right of the staying title (`노아 ◇`). */
    val dottedName: String,
    val checkRow: Pair<String, String>,
    val duplicateRow: Pair<String, String>,
    /** `다른 인물과 합치기 ›` at the right of the `중복이면` row (CST-45); null for several items. */
    val mergeAction: String?,
    val footnote: String,
    val cancelLabel: String,
    val confirmLabel: String,
)

/** What `휴지통으로` did: the next document, its toast (`… 지움 · 참조 20 · 점검 센터` + `되돌리기`) and the change to revert. */
data class ItemDeleted(val doc: ProjectDoc, val toast: Toast, val change: DocChange)

/** What `되살리기` did, or why it did nothing ([needsAuth]: a hidden item, ask the biometric prompt first). */
data class ItemRestored(val doc: ProjectDoc?, val toast: Toast?, val needsAuth: Boolean)

/**
 * Deleting and restoring a character, place, term or faction (rule 20; CST-40, CST-41, CST-43).
 * Counts come from `domain:trash` [Impact] and its sheet copy from [Messages] (DECISIONS §1);
 * characters go through `domain:actions` [CharacterActions], other items through [TrashOps].
 */
object ItemTrash {
    fun itemId(kind: EntityKind, key: Key): ItemId = ItemId(ItemKind.of(kind), key)

    /** True while the item exists and is not in the trash (hidden items count: the caller already opened them). */
    fun isLive(doc: ProjectDoc, kind: EntityKind, key: Key): Boolean =
        ItemRefs.resolve(doc, kind, key, hiddenUnlocked = true) is ResolvedItem.Live

    /** The impact of deleting a live item; null when it is gone or already in the trash. */
    fun impact(doc: ProjectDoc, kind: EntityKind, key: Key, recorder: RecorderLibrary?): DeleteImpact? {
        if (!isLive(doc, kind, key)) return null
        return if (kind == EntityKind.CHARACTER) CharacterActions.impact(doc, key, recorder) else Impact.of(doc, itemId(kind, key), recorder)
    }

    /** The sheet for [impact] (`RCDeleteImpact`). A hidden item shows counts only (no scene list). */
    fun sheet(kind: EntityKind, impact: DeleteImpact): ImpactSheetView {
        val sheet = Messages.impactSheet(impact)
        val word = kind.label
        return ImpactSheetView(
            title = sheet.title,
            sub = sheet.sub,
            ownedTitle = sheet.ownedTitle,
            owned = sheet.ownedRows.map(::split),
            // `Messages` writes `지운 세력로 보임`; the particle follows the word (`세력으로`, `인물로`).
            stayingTitle = CastCopy.join("제자리에 남음", "지운 $word${CastImpact.directionParticle(word)} 보임"),
            staying = sheet.stayingRows.map(::split),
            dottedName = impact.names.firstOrNull().orEmpty(),
            checkRow = sheet.checkRow,
            duplicateRow = sheet.duplicateRow,
            mergeAction = sheet.mergeAction,
            footnote = sheet.footnote,
            cancelLabel = sheet.buttons[0],
            confirmLabel = sheet.buttons[1],
        )
    }

    /** `관계 1` → `관계` | `1`: the count is the row's last word. */
    fun split(row: SheetRow): ImpactRowView {
        val cut = row.label.lastIndexOf(' ')
        return if (cut <= 0) ImpactRowView(row.label, "", row.sub) else ImpactRowView(row.label.substring(0, cut), row.label.substring(cut + 1), row.sub)
    }

    /** `휴지통으로`. Null when the item is gone or already in the trash. */
    fun delete(doc: ProjectDoc, kind: EntityKind, key: Key, ctx: ActionContext, recorder: RecorderLibrary?): ItemDeleted? {
        if (!isLive(doc, kind, key)) return null
        if (kind == EntityKind.CHARACTER) {
            val r = CharacterActions.delete(doc, key, ctx, recorder) ?: return null
            return ItemDeleted(r.doc, r.toast, r.change)
        }
        val impact = Impact.of(doc, itemId(kind, key), recorder)
        val r = TrashOps.delete(doc, itemId(kind, key), ctx.trash)
        if (r.doc == doc) return null
        return ItemDeleted(r.doc, Messages.deleteToast(impact), DocChange(doc, r.doc))
    }

    /**
     * `지우기` of a field definition (CST-23, TC-F-12): one `fieldDef` trash entry (filter `인물`) for
     * 30 days through `domain:trash` (DECISIONS §1); values and `◆` stop being live and come back on
     * restore. Toast `항목 신장 지움 · 되돌리기`. Null when the definition is gone or already deleted.
     */
    fun deleteFieldDef(doc: ProjectDoc, defKey: Key, ctx: ActionContext): ItemDeleted? {
        val item = ItemId.fieldDef(defKey)
        if (doc.index.fieldDef(defKey)?.deletedAt != null) return null
        val impact = Impact.of(doc, item)
        if (impact.items.isEmpty()) return null
        val r = TrashOps.delete(doc, item, ctx.trash)
        if (r.doc == doc) return null
        return ItemDeleted(r.doc, Messages.deleteToast(impact), DocChange(doc, r.doc))
    }

    /**
     * `되살리기` of a trashed item (CST-43): the key is unchanged, so every reference re-attaches. A
     * character's toast counts what re-linked and what the author changed meanwhile.
     */
    fun restore(doc: ProjectDoc, kind: EntityKind, key: Key, ctx: ActionContext, authenticated: Boolean): ItemRestored {
        if (kind == EntityKind.CHARACTER) {
            val r = CharacterActions.restore(doc, key, ctx, authenticated)
            return outcome(r.outcome, r.toast)
        }
        val entry = DeletedItems.trashEntryOf(doc, kind, key) ?: return ItemRestored(null, null, false)
        val outcome = RestoreOps.restore(doc, entry.key, ctx.trash, authenticated)
        return outcome(outcome, (outcome as? RestoreOutcome.Restored)?.let { Messages.restoreToast(it.report) })
    }

    private fun outcome(outcome: RestoreOutcome, toast: Toast?): ItemRestored = when (outcome) {
        is RestoreOutcome.Restored -> ItemRestored(outcome.doc, toast, false)
        RestoreOutcome.NeedsAuth -> ItemRestored(null, null, true)
        else -> ItemRestored(null, null, false)
    }

    /**
     * `이대로 두기` on one dotted reference row (CST-41): a `deleted.item` acknowledgement for
     * [itemKey] and the [referencingKey] that shows it (the relation of a partner row). The dotted
     * mark stays; only the fix buttons go.
     */
    fun keep(doc: ProjectDoc, itemKey: Key, referencingKey: Key, ctx: ActionContext): ProjectDoc =
        Fixes.keep(doc, itemKey, ctx.trash, referencingKey).doc

    /**
     * `이대로 두기` on a deleted item's whole check row (CST-42, TC-20-28): the row moves to
     * `맞다고 표시한 것`; the dotted marks stay.
     */
    fun keepItem(doc: ProjectDoc, itemKey: Key, ctx: ActionContext): ProjectDoc = Fixes.keep(doc, itemKey, ctx.trash).doc

    /** The row was kept, either on its own or with the item's whole check row (`맞다고 표시한 것`). */
    fun isKept(doc: ProjectDoc, itemKey: Key, referencingKey: Key): Boolean =
        Fixes.isKept(doc, itemKey) || doc.checks.acknowledgements.any {
            it.checkType == CheckType.DELETED_ITEM && it.subjectKeys == listOf(itemKey, referencingKey)
        }
}
