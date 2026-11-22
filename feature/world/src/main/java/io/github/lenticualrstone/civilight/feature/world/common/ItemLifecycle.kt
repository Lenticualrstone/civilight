package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.trash.DeleteImpact
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.Impact
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.domain.trash.TrashGates
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/** The outcome of `지우기` (rule 20): the next document and the toast `장소 잿더미 시장 지움 · 참조 9 · 점검 센터 · 되돌리기`. */
data class Deleted(val doc: ProjectDoc, val toast: Toast)

/** The outcome of `되살리기` from a card. */
sealed interface Restored {
    /** Back, with the toast `잿더미 시장 되살림 · 다시 이어짐 · …`. */
    data class Done(val doc: ProjectDoc, val toast: Toast) : Restored

    /** The item was hidden: `지문으로 보기` first (LIB-38 `되살리기는 지문 확인`). */
    data object NeedsAuth : Restored

    data object NotFound : Restored
}

/**
 * Hiding, deleting, restoring and same-name notices of world items and events (WLD-13, WLD-14,
 * WLD-15 entry), always through `domain:trash` (DECISIONS §1); `domain:world` only flips the hidden
 * flag. Pure: view models call these inside one undo step.
 */
object ItemLifecycle {
    fun itemKind(kind: WorldKind): ItemKind = when (kind) {
        WorldKind.PLACE -> ItemKind.PLACE
        WorldKind.TERM -> ItemKind.TERM
        WorldKind.FACTION -> ItemKind.FACTION
    }

    fun itemId(kind: WorldKind, key: Key): ItemId = ItemId(itemKind(kind), key)

    /** `숨기기` / `숨김 풀기` (rule 19): only the flag; values stay. */
    fun setHidden(doc: ProjectDoc, kind: WorldKind, key: Key, hidden: Boolean, ctx: EditContext): ProjectDoc =
        WorldItems.setHidden(doc, kind, key, hidden, ctx)

    fun isHidden(doc: ProjectDoc, kind: WorldKind, key: Key): Boolean = when (kind) {
        WorldKind.PLACE -> doc.index.place(key)?.hidden
        WorldKind.TERM -> doc.index.term(key)?.hidden
        WorldKind.FACTION -> doc.index.faction(key)?.hidden
    } == true

    /** The impact of putting [item] in the trash (`같이 휴지통으로` · `제자리에 남음`). */
    fun impact(doc: ProjectDoc, item: ItemId, recorder: RecorderLibrary? = null): DeleteImpact = Impact.of(doc, item, recorder)

    /** `휴지통으로`: null when the item is not live (already in the trash, unknown). */
    fun delete(doc: ProjectDoc, item: ItemId, ctx: ActionContext, recorder: RecorderLibrary? = null): Deleted? {
        if (!isLive(doc, item)) return null
        val impact = Impact.of(doc, item, recorder)
        val result = TrashOps.delete(doc, item, ctx.trash)
        if (item !in result.trashed) return null
        return Deleted(result.doc, Messages.deleteToast(impact))
    }

    /** `되살리기` of the item's own trash entry (hidden items need [authenticated]). */
    fun restore(doc: ProjectDoc, item: ItemId, ctx: ActionContext, authenticated: Boolean): Restored {
        val entryKey = trashable(doc, item)?.trashEntryKey ?: return Restored.NotFound
        val entry = doc.index.trashEntry(entryKey) ?: return Restored.NotFound
        if (TrashGates.needsAuth(doc, entry) && !authenticated) return Restored.NeedsAuth
        return when (val outcome = RestoreOps.restore(doc, entryKey, ctx.trash, authenticated)) {
            is RestoreOutcome.Restored -> Restored.Done(outcome.doc, Messages.restoreToast(outcome.report))
            RestoreOutcome.NeedsAuth -> Restored.NeedsAuth
            else -> Restored.NotFound
        }
    }

    /** True when the item has a trash entry the card can offer `되살리기` for (not purged, not merged away). */
    fun restorable(doc: ProjectDoc, item: ItemId): Boolean {
        val row = trashable(doc, item) ?: return false
        return row.deletedAt != null && row.trashEntryKey?.let { doc.index.trashEntry(it) } != null
    }

    /** `◇ 같은 이름 2 · 노아` (WLD-15 entry) when another live item of the kind has the same name. */
    fun sameName(doc: ProjectDoc, kind: WorldKind, key: Key): String? = Fixes.sameNameNotice(doc, itemId(kind, key))

    fun isLive(doc: ProjectDoc, item: ItemId): Boolean = trashable(doc, item)?.deletedAt == null && trashable(doc, item) != null

    private fun trashable(doc: ProjectDoc, item: ItemId): Trashable? = when (item.kind) {
        ItemKind.PLACE -> doc.index.place(item.key)
        ItemKind.TERM -> doc.index.term(item.key)
        ItemKind.FACTION -> doc.index.faction(item.key)
        ItemKind.EVENT -> doc.index.event(item.key)
        else -> null
    }
}
