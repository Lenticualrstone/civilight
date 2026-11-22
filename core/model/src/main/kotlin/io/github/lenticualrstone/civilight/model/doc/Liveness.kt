package io.github.lenticualrstone.civilight.model.doc

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.map.MapOverlay
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Derived liveness (§5.1): `isLive(row) = row.deletedAt == null && every owner of the row is live`.
 * Only the row the user deleted carries a mark; owned rows stop being live through their owner.
 * A missing owner (no row) is not live. Restore, purge and relocation algorithms belong to the
 * trash domain; this is only the read side.
 */
class Liveness internal constructor(private val doc: ProjectDoc) {
    private val index get() = doc.index

    private val liveUnits: Set<Key> by lazy {
        val memo = HashMap<Key, Boolean>()
        fun live(unit: StoryUnit, depth: Int): Boolean {
            memo[unit.key]?.let { return it }
            val parent = unit.parentKey?.let(index::unit)
            val result = unit.deletedAt == null && (parent == null || (depth < MAX_DEPTH && live(parent, depth + 1)))
            memo[unit.key] = result
            return result
        }
        doc.story.units.filter { live(it, 0) }.mapTo(HashSet()) { it.key }
    }

    /** A story unit: not marked and its parent (if any) live. A unit whose parent row is missing is treated as top level. */
    fun isUnitLive(key: Key): Boolean = key in liveUnits

    fun isLive(unit: StoryUnit): Boolean = isUnitLive(unit.key)

    /** A block: not marked and its scene live. */
    fun isLive(block: Block): Boolean = block.deletedAt == null && isUnitLive(block.sceneKey)

    fun isBlockLive(key: Key): Boolean = index.block(key)?.let(::isLive) ?: false

    fun isCharacterLive(key: Key): Boolean = index.character(key)?.let { it.deletedAt == null } ?: false
    fun isPlaceLive(key: Key): Boolean = index.place(key)?.let { it.deletedAt == null } ?: false
    fun isTermLive(key: Key): Boolean = index.term(key)?.let { it.deletedAt == null } ?: false
    fun isFactionLive(key: Key): Boolean = index.faction(key)?.let { it.deletedAt == null } ?: false
    fun isEventLive(key: Key): Boolean = index.event(key)?.let { it.deletedAt == null } ?: false
    fun isFieldDefLive(key: Key): Boolean = index.fieldDef(key)?.let { it.deletedAt == null } ?: false
    fun isMapLive(key: Key): Boolean = doc.maps[key]?.let { it.deletedAt == null } ?: false
    fun isPageLive(key: Key): Boolean = doc.pages[key]?.let { it.deletedAt == null } ?: false
    fun isCanvasLive(key: Key): Boolean = doc.canvases[key]?.let { it.deletedAt == null } ?: false

    /** An item with custom fields. */
    fun isEntityLive(kind: EntityKind, key: Key): Boolean = when (kind) {
        EntityKind.CHARACTER -> isCharacterLive(key)
        EntityKind.PLACE -> isPlaceLive(key)
        EntityKind.TERM -> isTermLive(key)
        EntityKind.FACTION -> isFactionLive(key)
    }

    /** A relation end. */
    fun isEndLive(ref: EntityRef): Boolean = when (ref.kind.known) {
        RelationEndKind.CHARACTER -> isCharacterLive(ref.key)
        RelationEndKind.FACTION -> isFactionLive(ref.key)
        null -> false
    }

    /** A relation: both ends live (TC-20-08). */
    fun isLive(relation: Relation): Boolean = isEndLive(relation.endA) && isEndLive(relation.endB)

    fun isRelationLive(key: Key): Boolean = index.relation(key)?.let(::isLive) ?: false

    /** A field value: its entity and its field definition live. */
    fun isFieldValueLive(kind: EntityKind, entityKey: Key, fieldDefKey: Key): Boolean =
        isEntityLive(kind, entityKey) && isFieldDefLive(fieldDefKey)

    /**
     * The entity a change target is about. An unknown target kind counts as live (it is kept;
     * `valueAt` ignores it).
     */
    fun isTargetLive(target: ChangeTarget): Boolean = when (target) {
        is ChangeTarget.Field -> target.entityKind.known?.let { isFieldValueLive(it, target.entityKey, target.fieldDefKey) } ?: false
        is ChangeTarget.DisplayName -> isCharacterLive(target.characterKey)
        is ChangeTarget.RelationSlot -> isRelationLive(target.relationKey)
        is ChangeTarget.RegionShape -> isMapLive(target.mapKey) && doc.maps[target.mapKey]?.regions?.any { it.key == target.regionKey } == true
        is ChangeTarget.TermReveal -> isTermLive(target.termKey)
        is ChangeTarget.Unknown -> true
    }

    /** A change record: not marked and its target live. The anchor never makes a record non-live. */
    fun isLive(record: ChangeRecord): Boolean = record.deletedAt == null && isTargetLive(record.target)

    fun isVoiceLineOrArchiveOwnerLive(characterKey: Key): Boolean = isCharacterLive(characterKey)

    /** An image use: its owner live (the project itself is always live here). */
    fun isLive(use: ImageUse): Boolean = when (use.ownerKind.known) {
        ImageOwnerKind.PROJECT -> true
        ImageOwnerKind.CHARACTER -> isCharacterLive(use.ownerKey)
        ImageOwnerKind.PLACE -> isPlaceLive(use.ownerKey)
        ImageOwnerKind.TERM -> isTermLive(use.ownerKey)
        ImageOwnerKind.FACTION -> isFactionLive(use.ownerKey)
        ImageOwnerKind.PAGE -> isPageLive(use.ownerKey)
        null -> true
    }

    /**
     * A gallery item (55 §12.2): not marked and its owner live (the project is always live here). An
     * item of an unknown owner kind counts as live (it is kept, never shown).
     */
    fun isLive(item: GalleryItem): Boolean = item.deletedAt == null && isGalleryOwnerLive(item.ownerKind.known, item.ownerKey)

    fun isGalleryItemLive(key: Key): Boolean = index.galleryItem(key)?.let(::isLive) ?: false

    /** The owner of a gallery: the project, or a live character. */
    fun isGalleryOwnerLive(kind: GalleryOwnerKind?, key: Key): Boolean = when (kind) {
        GalleryOwnerKind.PROJECT -> true
        GalleryOwnerKind.CHARACTER -> isCharacterLive(key)
        null -> true
    }

    /** A map overlay: its map live; a pin also needs its place (TC-20-24). */
    fun isOverlayLive(mapKey: Key, overlay: MapOverlay): Boolean =
        isMapLive(mapKey) && (overlay !is Pin || isPlaceLive(overlay.placeKey))

    /** Canvas nodes and edges: their canvas live (item nodes are references). */
    fun isCanvasPartLive(canvasKey: Key): Boolean = isCanvasLive(canvasKey)

    /** A term rejection: its term live. */
    fun isRejectionOwnerLive(termKey: Key): Boolean = isTermLive(termKey)

    companion object {
        private const val MAX_DEPTH = 64

        /** The rule itself: a row is live iff it has no direct mark and every owner is live. */
        fun isLive(deletedAt: Long?, vararg ownersLive: Boolean): Boolean = deletedAt == null && ownersLive.all { it }
    }
}
