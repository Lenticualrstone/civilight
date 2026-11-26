package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationOrder
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationSection
import io.github.lenticualrstone.civilight.domain.world.item.AffiliationSettings
import io.github.lenticualrstone.civilight.domain.world.item.Affiliations
import io.github.lenticualrstone.civilight.domain.world.item.BaseResult
import io.github.lenticualrstone.civilight.domain.world.item.BasisResult
import io.github.lenticualrstone.civilight.domain.world.item.BasisSlot
import io.github.lenticualrstone.civilight.domain.world.item.FactionMembers
import io.github.lenticualrstone.civilight.domain.world.item.OrderList
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint

/**
 * (2026-11-26) Place card `소속 세력 및 인물`, faction `거점 · 소속 인물` and the `묶음 기준` settings
 * (board `ImmPlaceRules`). Each edit is one pure `ProjectDoc -> ProjectDoc`, written by the caller
 * with one `ProjectStore.update(label)`: one undo step, labelled with the matching `UNDO_*` text. A
 * call that changes nothing returns the same document (no undo step).
 */
object AffiliationActions {
    const val UNDO_FACTION_ORDER = Copy.Affiliation.UNDO_FACTION_ORDER
    const val UNDO_MEMBER_ORDER = Copy.Affiliation.UNDO_MEMBER_ORDER
    const val UNDO_LOOSE_ORDER = Copy.Affiliation.UNDO_LOOSE_ORDER
    const val UNDO_BASIS = Copy.Affiliation.UNDO_BASIS
    const val UNDO_BASE = Copy.Affiliation.UNDO_BASE

    // --- Views ---

    fun section(doc: ProjectDoc, placeKey: Key, lens: LensPoint, includeSubPlaces: Boolean = true, hiddenUnlocked: Boolean = false): AffiliationSection? =
        Affiliations(doc, hiddenUnlocked).section(placeKey, lens, includeSubPlaces)

    fun factionMembers(doc: ProjectDoc, factionKey: Key, lens: LensPoint, hiddenUnlocked: Boolean = false): FactionMembers? =
        Affiliations(doc, hiddenUnlocked).factionMembers(factionKey, lens)

    // --- Order ---

    fun moveFaction(doc: ProjectDoc, section: AffiliationSection, factionKey: Key, toIndex: Int, ctx: ActionContext): ProjectDoc =
        AffiliationOrder.moveFaction(doc, section, factionKey, toIndex, ctx.world)

    fun moveMember(doc: ProjectDoc, section: AffiliationSection, factionKey: Key, characterKey: Key, toIndex: Int, ctx: ActionContext): ProjectDoc =
        AffiliationOrder.moveMember(doc, section, factionKey, characterKey, toIndex, ctx.world)

    fun moveMember(doc: ProjectDoc, members: FactionMembers, characterKey: Key, toIndex: Int, ctx: ActionContext): ProjectDoc =
        AffiliationOrder.moveMember(doc, members, characterKey, toIndex, ctx.world)

    fun moveLoose(doc: ProjectDoc, section: AffiliationSection, characterKey: Key, toIndex: Int, ctx: ActionContext): ProjectDoc =
        AffiliationOrder.moveLoose(doc, section, characterKey, toIndex, ctx.world)

    /** `위로 옮기기` on any of the three lists. */
    fun moveUp(doc: ProjectDoc, list: OrderList, shown: List<Key>, key: Key, ctx: ActionContext): ProjectDoc =
        AffiliationOrder.moveUp(doc, list, shown, key, ctx.world)

    /** `아래로 옮기기` on any of the three lists. */
    fun moveDown(doc: ProjectDoc, list: OrderList, shown: List<Key>, key: Key, ctx: ActionContext): ProjectDoc =
        AffiliationOrder.moveDown(doc, list, shown, key, ctx.world)

    /** The undo label of a move on [list]. */
    fun undoLabel(list: OrderList): String = when (list) {
        is OrderList.Factions -> UNDO_FACTION_ORDER
        is OrderList.Members -> UNDO_MEMBER_ORDER
        is OrderList.Loose -> UNDO_LOOSE_ORDER
    }

    // --- Settings ---

    /** `묶음 기준` slot: a character `연결` field of the right target, or null to clear. */
    fun setBasis(doc: ProjectDoc, slot: BasisSlot, fieldDefKey: Key?, ctx: ActionContext): BasisResult =
        AffiliationSettings.setBasis(doc, slot, fieldDefKey, ctx.world)

    /** `거점`: a live place, or null to clear. */
    fun setBase(doc: ProjectDoc, factionKey: Key, placeKey: Key?, ctx: ActionContext): BaseResult =
        AffiliationSettings.setBase(doc, factionKey, placeKey, ctx.world)
}
