package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import io.github.lenticualrstone.civilight.model.world.Faction

private typealias AC = Copy.Affiliation

/**
 * A place named by a reference (`거점`, `거주`, `국적`). [deleted] = the place is in the trash or
 * purged (rule 20: drawn dotted; a purged one keeps its name tag); [hidden] = a hidden place while
 * locked (name replaced by `숨긴 장소`, rule 19).
 */
data class PlaceName(val key: Key, val name: String, val deleted: Boolean = false, val hidden: Boolean = false)

/**
 * A person under a faction group. [multi] = faction groups of this section the person is in.
 * [deleted] = the person is in the trash (rule 20: dotted, [Copy.Affiliation.DELETED_ACTIONS]; not counted).
 */
data class AffiliationMember(val characterKey: Key, val name: String, val multi: Int = 1, val deleted: Boolean = false) {
    /** `2곳`, or null for one faction. */
    val multiLabel: String? get() = if (multi > 1) AC.multi(multi) else null
}

/**
 * One faction group of a place card. [deleted] = the faction is in the trash but people still link
 * to it (rule 20: dotted, actions [Copy.Affiliation.DELETED_ACTIONS]); such a group is shown only
 * while it has members.
 */
data class FactionGroup(
    val factionKey: Key,
    val name: String,
    /** `거점` (this place or a sub-place). */
    val base: PlaceName?,
    val deleted: Boolean,
    /** Empty when the section has no `소속` basis ([AffiliationSection.membersAvailable] false). */
    val members: List<AffiliationMember>,
) {
    /** `인물 3` (the collapsed row). */
    val countLabel: String get() = AC.count(members.size)
}

/** A person of `무소속`: nationality first, then residence. [deleted] as in [AffiliationMember]. */
data class LooseMember(
    val characterKey: Key,
    val name: String,
    val nationality: List<PlaceName>,
    val residence: List<PlaceName>,
    val deleted: Boolean = false,
) {
    /** `국적 오리지아 · 거주 하층 거리`, `국적 오리지아` (no residence), `거주 하층 거리` (no nationality). */
    val line: String
        get() = listOfNotNull(
            nationality.takeIf { it.isNotEmpty() }?.let { "${AC.NATIONALITY} " + it.joinToString(Copy.SEP) { p -> p.name } },
            residence.takeIf { it.isNotEmpty() }?.let { "${AC.RESIDENCE} " + it.joinToString(Copy.SEP) { p -> p.name } },
        ).joinToString(Copy.SEP)
}

/**
 * `소속 세력 및 인물` of a place card at a lens (board `ImmPlaceRules`). Never hidden as a whole.
 *
 * Counts leave out locked hidden factions and people (they are in [lockLine]) and trashed factions
 * and people (shown dotted); a person is counted once however many groups hold them.
 */
data class AffiliationSection(
    val placeKey: Key,
    val includeSubPlaces: Boolean,
    val factions: List<FactionGroup>,
    /** False when no usable `소속` basis is set: factions come from `거점` only, with [pickBasisLine]. */
    val membersAvailable: Boolean,
    val loose: List<LooseMember>,
    /** False when neither `거주` nor `국적` is set: the `무소속` group is not shown. */
    val looseAvailable: Boolean,
    val factionCount: Int,
    val characterCount: Int,
    val hiddenFactions: Int,
    val hiddenCharacters: Int,
    /** `시점 1-5 · 소속 기준 ‘소속’`, or null without a basis. */
    val basisLine: String?,
) {
    val title: String get() = AC.TITLE

    /** `세력 2 · 인물 5` (right of the section title). */
    val header: String get() = AC.header(factionCount, characterCount)

    /** `소속 기준 고르기` button line in place of the members, or null. */
    val pickBasisLine: String? get() = if (membersAvailable) null else AC.PICK_BASIS

    /** `숨긴 세력 1 · 숨긴 인물 2` (UI adds `지문으로 보기`), or null. */
    val lockLine: String?
        get() = listOfNotNull(
            hiddenFactions.takeIf { it > 0 }?.let { "${AC.HIDDEN_FACTION} $it" },
            hiddenCharacters.takeIf { it > 0 }?.let { "${AC.HIDDEN_CHARACTER} $it" },
        ).joinToString(Copy.SEP).ifEmpty { null }
}

/** The faction card's `거점` and `소속 인물` (the same calculation for one faction). */
data class FactionMembers(
    val factionKey: Key,
    val base: PlaceName?,
    /** [AffiliationMember.multi] = live shown factions the person is in. */
    val members: List<AffiliationMember>,
    val membersAvailable: Boolean,
    val hiddenCharacters: Int,
) {
    val pickBasisLine: String? get() = if (membersAvailable) null else AC.PICK_BASIS
    val lockLine: String? get() = hiddenCharacters.takeIf { it > 0 }?.let { "${AC.HIDDEN_CHARACTER} $it" }
}

/**
 * Grouping people on place and faction cards by the project's `묶음 기준` (rule rows of board
 * `ImmPlaceRules`). Values are read at the lens through the card field engine
 * ([WorldCards.valueAt]: `◆` record in force, else the base value); nothing is guessed: an unset
 * or unusable basis slot is simply not used ([AffiliationSettings.usableDef]).
 *
 * Default order is creation order (`order`, then key, as every world list); the user's order is
 * [io.github.lenticualrstone.civilight.model.world.Place.factionOrder],
 * [io.github.lenticualrstone.civilight.model.world.Faction.memberOrder] and
 * [io.github.lenticualrstone.civilight.model.world.Place.looseOrder]; unlisted keys follow in
 * creation order and stale keys are ignored.
 */
class Affiliations(private val doc: ProjectDoc, private val hiddenUnlocked: Boolean = false) {
    private val cards = WorldCards(doc, hiddenUnlocked)

    /**
     * Live and trashed people (a trashed one still holds its values and shows dotted, rule 20); purged ones are gone.
     * A person trashed by a merge (`합침 → 대상`) is left out: its references moved to the target, so it is no
     * dotted ghost beside the target.
     */
    private val people: List<Character> = doc.cast.characters.filterNot(::mergedAway).sortedWith(compareBy({ it.order }, { it.key }))

    private fun mergedAway(c: Character): Boolean =
        c.deletedAt != null && c.trashEntryKey?.let { doc.index.trashEntry(it) }?.reason?.known == TrashReason.MERGED

    /** Locked hidden and live: counted in `숨긴 인물 N`. */
    private fun hiddenCounted(c: Character) = locked(c) && c.deletedAt == null

    private fun locked(c: Character) = c.hidden && !hiddenUnlocked

    private fun locked(f: Faction) = f.hidden && !hiddenUnlocked

    /** Every place under [placeKey] through the `지역` chain (trashed ones included; cycles are cut). */
    fun subPlaces(placeKey: Key): Set<Key> {
        val children = doc.world.places.filter { it.parentPlaceKey != null }.groupBy({ it.parentPlaceKey!! }, { it.key })
        val out = LinkedHashSet<Key>()
        val queue = ArrayDeque(children[placeKey].orEmpty())
        while (queue.isNotEmpty()) {
            val k = queue.removeFirst()
            if (k == placeKey || !out.add(k)) continue
            queue.addAll(children[k].orEmpty())
        }
        return out
    }

    /** This place, plus its sub-places when [includeSubPlaces]. */
    fun scope(placeKey: Key, includeSubPlaces: Boolean): Set<Key> =
        if (includeSubPlaces) setOf(placeKey) + subPlaces(placeKey) else setOf(placeKey)

    /** [def] is a 비밀 field of [c] whose secret tab is locked (CST-73): its value cannot be read. */
    private fun unreadable(c: Character, def: FieldDef?): Boolean = def != null && def.isSecret && c.secretTabLocked

    /** Link targets of [def] for [c] at [lens]; empty without a def, for a locked 비밀 field (CST-73) or a non-link value. */
    private fun targets(c: Character, def: FieldDef?, lens: LensPoint): List<Key> {
        if (def == null) return emptyList()
        if (unreadable(c, def)) return emptyList()
        val value = cards.valueAt(EntityKind.CHARACTER, c.key, def.key, c.fields[def.key], lens)
        return (value?.payload as? FieldPayload.Link)?.targetKeys.orEmpty()
    }

    /** Name of a referenced place: live, trashed (dotted), purged (name tag, dotted) or locked hidden. */
    fun placeName(key: Key): PlaceName {
        val place = doc.index.place(key)
        if (place == null) {
            val tag = doc.tombstones.nameTags.firstOrNull { it.key == key }
            val hidden = tag?.hidden == true && !hiddenUnlocked
            return PlaceName(key, if (hidden) AC.HIDDEN_PLACE else tag?.name ?: AC.DELETED_PLACE, deleted = true, hidden = hidden)
        }
        val hidden = place.hidden && !hiddenUnlocked
        return PlaceName(key, if (hidden) AC.HIDDEN_PLACE else place.name, deleted = place.deletedAt != null, hidden = hidden)
    }

    fun section(placeKey: Key, lens: LensPoint = LensPoint.Latest, includeSubPlaces: Boolean = true): AffiliationSection? {
        val place = doc.index.place(placeKey) ?: return null
        val scope = scope(placeKey, includeSubPlaces)
        val affDef = AffiliationSettings.usableDef(doc, BasisSlot.AFFILIATION)
        val resDef = AffiliationSettings.usableDef(doc, BasisSlot.RESIDENCE)
        val natDef = AffiliationSettings.usableDef(doc, BasisSlot.NATIONALITY)

        val based = doc.world.factions.filter { it.baseKey != null && it.baseKey in scope }.sortedWith(compareBy({ it.order }, { it.key }))
        val basedKeys = based.mapTo(HashSet()) { it.key }
        // Memberships here at the lens, decided before any masking so a locked faction's people never leak into 무소속.
        val here: Map<Key, Set<Key>> = if (affDef == null) {
            emptyMap()
        } else {
            people.associate { c -> c.key to targets(c, affDef, lens).filterTo(LinkedHashSet()) { it in basedKeys } }
        }

        val liveShown = based.filter { it.deletedAt == null && !locked(it) }
        val hiddenFactions = based.count { it.deletedAt == null && locked(it) }
        // A locked hidden faction stays masked in the trash too (its people are masked as with a live one).
        val trashedWithPeople = based.filter { f ->
            f.deletedAt != null && !locked(f) && people.any { !locked(it) && f.key in here[it.key].orEmpty() }
        }
        // A trashed faction keeps its place in the order (drawn dotted).
        val groupsInOrder = ordered(based.filter { it in liveShown || it in trashedWithPeople }, place.factionOrder) { it.key }
        val shownGroupKeys = groupsInOrder.mapTo(HashSet()) { it.key }

        val hiddenPeople = LinkedHashSet<Key>()
        val groups = groupsInOrder.map { f ->
            val members = ordered(people.filter { f.key in here[it.key].orEmpty() }, f.memberOrder) { it.key }
            hiddenPeople += members.filter(::hiddenCounted).map { it.key }
            FactionGroup(
                factionKey = f.key,
                name = f.name,
                base = f.baseKey?.let(::placeName),
                deleted = f.deletedAt != null,
                members = members.filterNot(::locked).map { c ->
                    AffiliationMember(c.key, c.name, multi = here[c.key].orEmpty().count { it in shownGroupKeys }, deleted = c.deletedAt != null)
                },
            )
        }
        // A non-hidden person whose only faction here is a locked one is masked with it and not counted;
        // a hidden person in any faction here is counted once.
        people.filter { hiddenCounted(it) && here[it.key].orEmpty().isNotEmpty() }.forEach { hiddenPeople += it.key }

        val looseAvailable = resDef != null || natDef != null
        val loose = if (!looseAvailable) {
            emptyList()
        } else {
            // A person whose 소속 or 거주 is a locked 비밀 field (CST-73) cannot be placed: they may be in a faction
            // here, or live elsewhere. When the readable values point here they are counted in `숨긴 인물` instead.
            val candidates = ArrayList<Character>()
            for (c in people) {
                if (here[c.key].orEmpty().isNotEmpty()) continue
                val residence = targets(c, resDef, lens)
                val match = if (residence.isNotEmpty()) residence.any { it in scope } else placeKey in targets(c, natDef, lens)
                if (!match) continue
                if (unreadable(c, affDef) || unreadable(c, resDef)) {
                    if (c.deletedAt == null) hiddenPeople += c.key
                } else {
                    candidates += c
                }
            }
            hiddenPeople += candidates.filter(::hiddenCounted).map { it.key }
            ordered(candidates.filterNot(::locked), place.looseOrder) { it.key }.map { c ->
                LooseMember(
                    characterKey = c.key,
                    name = c.name,
                    nationality = targets(c, natDef, lens).map(::placeName),
                    residence = targets(c, resDef, lens).map(::placeName),
                    deleted = c.deletedAt != null,
                )
            }
        }

        val counted = groups.flatMap { g -> g.members.filterNot { it.deleted }.map { it.characterKey } }.toSet() +
            loose.filterNot { it.deleted }.map { it.characterKey }
        return AffiliationSection(
            placeKey = placeKey,
            includeSubPlaces = includeSubPlaces,
            factions = groups,
            membersAvailable = affDef != null,
            loose = loose,
            looseAvailable = looseAvailable,
            factionCount = groups.count { !it.deleted },
            characterCount = counted.size,
            hiddenFactions = hiddenFactions,
            hiddenCharacters = hiddenPeople.size,
            basisLine = affDef?.let { AC.basisLine(StoryCodes(doc).lensLabel(lens), it.name) },
        )
    }

    /** The faction card's `거점` and `소속 인물` at [lens]. */
    fun factionMembers(factionKey: Key, lens: LensPoint = LensPoint.Latest): FactionMembers? {
        val faction = doc.index.faction(factionKey) ?: return null
        val affDef = AffiliationSettings.usableDef(doc, BasisSlot.AFFILIATION)
        val shownFactions = doc.world.factions.filter { it.deletedAt == null && !locked(it) }.mapTo(HashSet()) { it.key }
        val linked = if (affDef == null) emptyList() else people.mapNotNull { c -> targets(c, affDef, lens).takeIf { factionKey in it }?.let { c to it } }
        val members = ordered(linked, faction.memberOrder) { it.first.key }
        return FactionMembers(
            factionKey = factionKey,
            base = faction.baseKey?.let(::placeName),
            members = members.filterNot { locked(it.first) }.map { (c, keys) ->
                AffiliationMember(c.key, c.name, multi = keys.distinct().count { it in shownFactions }.coerceAtLeast(1), deleted = c.deletedAt != null)
            },
            membersAvailable = affDef != null,
            hiddenCharacters = members.count { hiddenCounted(it.first) },
        )
    }

    companion object {
        /**
         * [items] (already in creation order) with the keys of [stored] first in that order; unlisted
         * items follow in their order; stale or repeated stored keys are ignored.
         */
        fun <T> ordered(items: List<T>, stored: List<Key>, key: (T) -> Key): List<T> {
            if (stored.isEmpty()) return items
            val byKey = items.associateBy(key)
            val first = stored.distinct().mapNotNull { byKey[it] }
            val listed = first.mapTo(HashSet(), key)
            return first + items.filter { key(it) !in listed }
        }
    }
}
