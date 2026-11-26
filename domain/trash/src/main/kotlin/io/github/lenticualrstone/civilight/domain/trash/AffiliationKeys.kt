package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.AffiliationBasis
import io.github.lenticualrstone.civilight.model.trash.MovedRef

/**
 * (2026-11-26) Rule 20 for the place card `소속 세력 및 인물` keys (00 §14): `Faction.baseKey`,
 * `Faction.memberOrder`, `Place.factionOrder`, `Place.looseOrder` and `CastSettings.affiliationBasis`.
 *
 * - Trash and restore change none of them: a faction or place keeps its own lists in its row, and
 *   references (`거점`, order entries, basis slots) keep the key, so they show dotted and come back
 *   by key.
 * - Merge moves every reference to the target ([onMerge]), and the merged item's own lists carry
 *   over to the target ([carryOwnLists]): a merged faction's `거점` (only when the target has none)
 *   and `memberOrder`, a merged place's `factionOrder` and `looseOrder`, each appended after the
 *   target's own entries (duplicates and keys of purged items dropped). The merged item's snapshot
 *   keeps its own lists for a restore; `되돌리기` of the merge undoes the carry like every other step.
 * - Purge ([onPurge]) drops the key from the order lists and clears a basis slot whose field is
 *   gone; `거점` keeps the key and shows the name tag, like any other place reference. A purged
 *   place's children move up to its parent (or have no parent when it had none), so places under
 *   them stay in the `하위 지역` chain of the places above.
 */
internal object AffiliationKeys {
    private fun List<Key>.swap(from: Key, to: Key): List<Key> = map { if (it == from) to else it }.distinct()

    fun onMerge(doc: ProjectDoc, kind: ItemKind, from: Key, to: Key, at: Long, by: String, moved: MutableList<MovedRef>): ProjectDoc = when (kind) {
        ItemKind.PLACE -> doc.copy(
            world = doc.world.copy(factions = doc.world.factions.map { f ->
                if (f.baseKey != from) return@map f
                moved += MovedRef("faction", f.key, "baseKey")
                f.copy(baseKey = to, updatedAt = at, updatedBy = by)
            }),
        )
        ItemKind.FACTION -> doc.copy(
            world = doc.world.copy(places = doc.world.places.map { p ->
                if (from !in p.factionOrder) return@map p
                moved += MovedRef("place", p.key, "factionOrder")
                p.copy(factionOrder = p.factionOrder.swap(from, to), updatedAt = at, updatedBy = by)
            }),
        )
        ItemKind.CHARACTER -> doc.copy(
            world = doc.world.copy(
                factions = doc.world.factions.map { f ->
                    if (from !in f.memberOrder) return@map f
                    moved += MovedRef("faction", f.key, "memberOrder")
                    f.copy(memberOrder = f.memberOrder.swap(from, to), updatedAt = at, updatedBy = by)
                },
                places = doc.world.places.map { p ->
                    if (from !in p.looseOrder) return@map p
                    moved += MovedRef("place", p.key, "looseOrder")
                    p.copy(looseOrder = p.looseOrder.swap(from, to), updatedAt = at, updatedBy = by)
                },
            ),
        )
        else -> doc
    }

    /**
     * The merged item's own `소속 세력 및 인물` lists go to the target (see the class note). Runs after
     * [onMerge], so the lists hold target keys already where they named the merged item.
     */
    fun carryOwnLists(doc: ProjectDoc, kind: ItemKind, from: Key, to: Key, at: Long, by: String): ProjectDoc = when (kind) {
        ItemKind.FACTION -> {
            val merged = doc.index.faction(from)
            if (merged == null) {
                doc
            } else {
                doc.copy(world = doc.world.copy(factions = doc.world.factions.map { f ->
                    if (f.key != to) return@map f
                    // A 거점 on a purged place is kept too (it shows the name tag, like any place reference).
                    val base = f.baseKey ?: merged.baseKey
                    val members = f.memberOrder.append(merged.memberOrder) { doc.index.character(it) != null }
                    if (base == f.baseKey && members == f.memberOrder) f else f.copy(baseKey = base, memberOrder = members, updatedAt = at, updatedBy = by)
                }))
            }
        }
        ItemKind.PLACE -> {
            val merged = doc.index.place(from)
            if (merged == null) {
                doc
            } else {
                doc.copy(world = doc.world.copy(places = doc.world.places.map { p ->
                    if (p.key != to) return@map p
                    val factions = p.factionOrder.append(merged.factionOrder) { doc.index.faction(it) != null }
                    val loose = p.looseOrder.append(merged.looseOrder) { doc.index.character(it) != null }
                    if (factions == p.factionOrder && loose == p.looseOrder) p else p.copy(factionOrder = factions, looseOrder = loose, updatedAt = at, updatedBy = by)
                }))
            }
        }
        else -> doc
    }

    /** This list, then the entries of [more] that are not in it yet and pass [exists]. */
    private fun List<Key>.append(more: List<Key>, exists: (Key) -> Boolean): List<Key> {
        if (more.isEmpty()) return this
        val have = toHashSet()
        val extra = more.filter { it !in have && exists(it) }.distinct()
        return if (extra.isEmpty()) this else this + extra
    }

    /**
     * After [item] left [doc] for good. [parentOfPurgedPlace] = the `지역` parent the purged place had
     * (its children move up to it).
     */
    fun onPurge(doc: ProjectDoc, item: ItemId, at: Long, by: String, parentOfPurgedPlace: Key? = null): ProjectDoc {
        val k = item.key
        return when (item.kind) {
            ItemKind.FACTION -> doc.copy(
                world = doc.world.copy(places = doc.world.places.map { p ->
                    if (k in p.factionOrder) p.copy(factionOrder = p.factionOrder - k, updatedAt = at, updatedBy = by) else p
                }),
            )
            ItemKind.CHARACTER -> doc.copy(
                world = doc.world.copy(
                    factions = doc.world.factions.map { f ->
                        if (k in f.memberOrder) f.copy(memberOrder = f.memberOrder - k, updatedAt = at, updatedBy = by) else f
                    },
                    places = doc.world.places.map { p ->
                        if (k in p.looseOrder) p.copy(looseOrder = p.looseOrder - k, updatedAt = at, updatedBy = by) else p
                    },
                ),
            )
            ItemKind.PLACE -> {
                // A parent that is gone too (or would make the child its own parent) leaves no parent.
                val parent = parentOfPurgedPlace?.takeIf { it != k && doc.index.place(it) != null }
                if (doc.world.places.none { it.parentPlaceKey == k }) {
                    doc
                } else {
                    doc.copy(world = doc.world.copy(places = doc.world.places.map { p ->
                        if (p.parentPlaceKey != k) return@map p
                        p.copy(parentPlaceKey = parent?.takeIf { it != p.key }, updatedAt = at, updatedBy = by)
                    }))
                }
            }
            ItemKind.FIELD_DEF -> {
                val b = doc.project.cast.affiliationBasis
                val next = AffiliationBasis(
                    affiliationFieldKey = b.affiliationFieldKey?.takeIf { it != k },
                    residenceFieldKey = b.residenceFieldKey?.takeIf { it != k },
                    nationalityFieldKey = b.nationalityFieldKey?.takeIf { it != k },
                    ext = b.ext,
                )
                if (next == b) doc else doc.copy(project = doc.project.copy(cast = doc.project.cast.copy(affiliationBasis = next), updatedAt = at, updatedBy = by))
            }
            else -> doc
        }
    }
}
