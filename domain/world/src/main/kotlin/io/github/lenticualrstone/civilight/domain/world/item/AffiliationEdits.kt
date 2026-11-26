package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.AffiliationBasis

/** The three separate `묶음 기준` slots and the link target each needs. */
enum class BasisSlot(val label: String, val target: FieldLinkTargetKind) {
    AFFILIATION(Copy.Affiliation.SLOT_AFFILIATION, FieldLinkTargetKind.FACTION),
    RESIDENCE(Copy.Affiliation.RESIDENCE, FieldLinkTargetKind.PLACE),
    NATIONALITY(Copy.Affiliation.NATIONALITY, FieldLinkTargetKind.PLACE),
}

/** Why a field cannot be a `묶음 기준`. */
enum class BasisRefusal {
    /** No such field definition (or purged). */
    NO_FIELD,

    /** The field is in the trash. */
    TRASHED,

    /** Not a character field. */
    NOT_CHARACTER,

    /** A `이 인물만` field. */
    ONE_CHARACTER,

    /** Not of type `연결`. */
    NOT_LINK,

    /** A `연결` to the wrong kind (소속 → 세력, 거주 · 국적 → 장소). */
    WRONG_TARGET,
}

sealed interface BasisResult {
    data class Done(val doc: ProjectDoc) : BasisResult

    data class Refused(val reason: BasisRefusal) : BasisResult
}

/** Why a `거점` cannot be set. */
enum class BaseRefusal {
    /** No such faction, or it is in the trash. */
    NO_FACTION,

    /** No such place (or purged). */
    NO_PLACE,

    /** The place is in the trash. */
    PLACE_TRASHED,
}

sealed interface BaseResult {
    data class Done(val doc: ProjectDoc) : BaseResult

    data class Refused(val reason: BaseRefusal) : BaseResult
}

/**
 * `묶음 기준` in 프로젝트 설정 and the faction's `거점` (board `ImmPlaceRules`). The user picks each
 * slot; nothing is guessed. Pure `ProjectDoc -> ProjectDoc`: one call is one undo step; a call that
 * changes nothing returns the same document.
 */
object AffiliationSettings {
    fun storedKey(basis: AffiliationBasis, slot: BasisSlot): Key? = when (slot) {
        BasisSlot.AFFILIATION -> basis.affiliationFieldKey
        BasisSlot.RESIDENCE -> basis.residenceFieldKey
        BasisSlot.NATIONALITY -> basis.nationalityFieldKey
    }

    fun withKey(basis: AffiliationBasis, slot: BasisSlot, key: Key?): AffiliationBasis = when (slot) {
        BasisSlot.AFFILIATION -> basis.copy(affiliationFieldKey = key)
        BasisSlot.RESIDENCE -> basis.copy(residenceFieldKey = key)
        BasisSlot.NATIONALITY -> basis.copy(nationalityFieldKey = key)
    }

    /** Null when [def] may fill [slot]. */
    fun refusal(def: FieldDef?, slot: BasisSlot): BasisRefusal? = when {
        def == null -> BasisRefusal.NO_FIELD
        def.deletedAt != null -> BasisRefusal.TRASHED
        def.entityKind.known != EntityKind.CHARACTER -> BasisRefusal.NOT_CHARACTER
        def.scopeCharacterKey != null -> BasisRefusal.ONE_CHARACTER
        def.type.known != FieldType.LINK -> BasisRefusal.NOT_LINK
        def.linkTargetKind?.known != slot.target -> BasisRefusal.WRONG_TARGET
        else -> null
    }

    /**
     * The field [slot] uses now; null when unset or no longer usable (trashed, purged, type or
     * target changed). The stored key is kept, so a restored field is used again.
     */
    fun usableDef(doc: ProjectDoc, slot: BasisSlot): FieldDef? {
        val key = storedKey(doc.project.cast.affiliationBasis, slot) ?: return null
        val def = doc.index.fieldDef(key)
        return def.takeIf { refusal(it, slot) == null }
    }

    /** The picker rows of [slot]: live character `연결` fields of the right target, in order. */
    fun choices(doc: ProjectDoc, slot: BasisSlot): List<FieldDef> =
        doc.project.fieldDefs.filter { refusal(it, slot) == null }.sortedWith(compareBy({ it.order }, { it.key }))

    /** Sets [slot] to [fieldDefKey], or clears it when null. */
    fun setBasis(doc: ProjectDoc, slot: BasisSlot, fieldDefKey: Key?, ctx: EditContext): BasisResult {
        if (fieldDefKey != null) refusal(doc.index.fieldDef(fieldDefKey), slot)?.let { return BasisResult.Refused(it) }
        val basis = doc.project.cast.affiliationBasis
        if (storedKey(basis, slot) == fieldDefKey) return BasisResult.Done(doc)
        val project = doc.project.copy(
            cast = doc.project.cast.copy(affiliationBasis = withKey(basis, slot, fieldDefKey)),
            updatedAt = ctx.now(), updatedBy = ctx.device,
        )
        return BasisResult.Done(doc.copy(project = project))
    }

    /** Sets a live faction's `거점` to a live place, or clears it when [placeKey] is null. */
    fun setBase(doc: ProjectDoc, factionKey: Key, placeKey: Key?, ctx: EditContext): BaseResult {
        val faction = doc.index.faction(factionKey)?.takeIf { it.deletedAt == null } ?: return BaseResult.Refused(BaseRefusal.NO_FACTION)
        if (placeKey != null) {
            val place = doc.index.place(placeKey) ?: return BaseResult.Refused(BaseRefusal.NO_PLACE)
            if (place.deletedAt != null) return BaseResult.Refused(BaseRefusal.PLACE_TRASHED)
        }
        if (faction.baseKey == placeKey) return BaseResult.Done(doc)
        val now = ctx.now()
        return BaseResult.Done(
            doc.copy(world = doc.world.copy(factions = doc.world.factions.map {
                if (it.key == factionKey) it.copy(baseKey = placeKey, updatedAt = now, updatedBy = ctx.device) else it
            })),
        )
    }
}

/** Which stored order a move writes. */
sealed interface OrderList {
    /** Faction groups of a place card (`Place.factionOrder`). */
    data class Factions(val placeKey: Key) : OrderList

    /** People of a faction, the same on every place card and the faction card (`Faction.memberOrder`). */
    data class Members(val factionKey: Key) : OrderList

    /** `무소속` of a place card (`Place.looseOrder`). */
    data class Loose(val placeKey: Key) : OrderList
}

/**
 * `순서 바꾸기` (drag) and the accessibility actions `위로 옮기기 · 아래로 옮기기`: factions among
 * factions, people within one faction, `무소속` among themselves. [shown] is the list as the user
 * sees it (it depends on the lens, the lock and `하위 지역 포함`); `toIndex` is an index in it.
 * Only the slots of shown keys are rewritten, so keys not shown now keep their place.
 * One call is one undo step; a move that changes nothing returns the same document.
 */
object AffiliationOrder {
    /**
     * The new stored order, or null when [key] is not shown or nothing moves. Stale stored keys stay
     * (they are ignored when read and dropped on purge).
     */
    fun reorder(stored: List<Key>, shown: List<Key>, key: Key, toIndex: Int): List<Key>? {
        val from = shown.indexOf(key)
        if (from < 0) return null
        val to = toIndex.coerceIn(0, shown.lastIndex)
        if (from == to) return null
        val moved = shown.toMutableList().apply { removeAt(from); add(to, key) }
        val shownSet = shown.toSet()
        val base = stored.distinct() + shown.filter { it !in stored }
        val next = moved.iterator()
        val result = base.map { if (it in shownSet) next.next() else it }
        return result.takeIf { it != stored }
    }

    fun move(doc: ProjectDoc, list: OrderList, shown: List<Key>, key: Key, toIndex: Int, ctx: EditContext): ProjectDoc {
        val now = ctx.now()
        return when (list) {
            is OrderList.Factions -> {
                val place = doc.index.place(list.placeKey) ?: return doc
                val next = reorder(place.factionOrder, shown, key, toIndex) ?: return doc
                doc.copy(world = doc.world.copy(places = doc.world.places.map {
                    if (it.key == place.key) it.copy(factionOrder = next, updatedAt = now, updatedBy = ctx.device) else it
                }))
            }
            is OrderList.Loose -> {
                val place = doc.index.place(list.placeKey) ?: return doc
                val next = reorder(place.looseOrder, shown, key, toIndex) ?: return doc
                doc.copy(world = doc.world.copy(places = doc.world.places.map {
                    if (it.key == place.key) it.copy(looseOrder = next, updatedAt = now, updatedBy = ctx.device) else it
                }))
            }
            is OrderList.Members -> {
                val faction = doc.index.faction(list.factionKey) ?: return doc
                val next = reorder(faction.memberOrder, shown, key, toIndex) ?: return doc
                doc.copy(world = doc.world.copy(factions = doc.world.factions.map {
                    if (it.key == faction.key) it.copy(memberOrder = next, updatedAt = now, updatedBy = ctx.device) else it
                }))
            }
        }
    }

    /** `위로 옮기기`; the first row stays. */
    fun moveUp(doc: ProjectDoc, list: OrderList, shown: List<Key>, key: Key, ctx: EditContext): ProjectDoc {
        val at = shown.indexOf(key)
        return if (at <= 0) doc else move(doc, list, shown, key, at - 1, ctx)
    }

    /** `아래로 옮기기`; the last row stays. */
    fun moveDown(doc: ProjectDoc, list: OrderList, shown: List<Key>, key: Key, ctx: EditContext): ProjectDoc {
        val at = shown.indexOf(key)
        return if (at < 0 || at >= shown.lastIndex) doc else move(doc, list, shown, key, at + 1, ctx)
    }

    // --- From a shown section ---

    fun moveFaction(doc: ProjectDoc, section: AffiliationSection, factionKey: Key, toIndex: Int, ctx: EditContext): ProjectDoc =
        move(doc, OrderList.Factions(section.placeKey), section.factions.map { it.factionKey }, factionKey, toIndex, ctx)

    fun moveMember(doc: ProjectDoc, section: AffiliationSection, factionKey: Key, characterKey: Key, toIndex: Int, ctx: EditContext): ProjectDoc {
        val shown = section.factions.firstOrNull { it.factionKey == factionKey }?.members?.map { it.characterKey } ?: return doc
        return move(doc, OrderList.Members(factionKey), shown, characterKey, toIndex, ctx)
    }

    /** People on the faction card. */
    fun moveMember(doc: ProjectDoc, members: FactionMembers, characterKey: Key, toIndex: Int, ctx: EditContext): ProjectDoc =
        move(doc, OrderList.Members(members.factionKey), members.members.map { it.characterKey }, characterKey, toIndex, ctx)

    fun moveLoose(doc: ProjectDoc, section: AffiliationSection, characterKey: Key, toIndex: Int, ctx: EditContext): ProjectDoc =
        move(doc, OrderList.Loose(section.placeKey), section.loose.map { it.characterKey }, characterKey, toIndex, ctx)
}
