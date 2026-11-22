package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.trash.TrashEntry

/**
 * The rule-19 gate on a trash entry (LIB-38, LIB-39, R19 §3/§6), one predicate for every place that
 * asks: the row's `되살리기는 지문 확인`, the auth step before `되살리기`, and which entries `비우기`
 * leaves alone while locked.
 */
object TrashGates {
    /**
     * True when restoring [entry] needs authentication first: the item was hidden when it was
     * deleted, is hidden now (a block follows its scene), or the project is hidden.
     */
    fun needsAuth(doc: ProjectDoc, entry: TrashEntry): Boolean {
        if (entry.hiddenAtDelete || doc.project.protections.hidden) return true
        val kind = ItemKind.of(entry.kind) ?: return false
        return entry.targetKeys.any { doc.itemHidden(ItemId(kind, it)) }
    }
}
