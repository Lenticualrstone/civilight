package io.github.lenticualrstone.civilight.domain.library.hiding

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind

/** Hideable items inside a project (LIB-36; 장소 · 세력 from v1; gallery images, 55 GAL-19). */
enum class HideableKind(val word: String) {
    CHARACTER("인물"), TERM("용어"), PAGE("페이지"), SCENE("씬"), PLACE("장소"), FACTION("세력"),

    /** A gallery image with its own `숨기기` (55 GAL-19, §6 Q3). */
    GALLERY_IMAGE("이미지"),
}

/**
 * Which items of a project are hidden (rule 19 §6). Hiding covers the item's own records: its card,
 * profile, relations and change records. Items in the trash are not counted.
 */
class HiddenItems(private val doc: ProjectDoc) {
    private val index = doc.index
    private val liveness = doc.liveness

    val characters: Set<Key> = doc.cast.characters.filter { it.hidden && it.deletedAt == null }.map { it.key }.toSet()
    val terms: Set<Key> = doc.world.terms.filter { it.hidden && it.deletedAt == null }.map { it.key }.toSet()
    val places: Set<Key> = doc.world.places.filter { it.hidden && it.deletedAt == null }.map { it.key }.toSet()
    val factions: Set<Key> = doc.world.factions.filter { it.hidden && it.deletedAt == null }.map { it.key }.toSet()
    val pages: Set<Key> = doc.pages.values.filter { it.hidden && it.deletedAt == null }.map { it.key }.toSet()
    val scenes: Set<Key> = doc.story.units.filter { it.hidden && index.isScene(it) && liveness.isLive(it) }.map { it.key }.toSet()

    /**
     * Live gallery images with their own `숨기기` (GAL-19). Images of a hidden character are masked by
     * their owner and are not counted here.
     */
    val galleryImages: Set<Key> = doc.galleryItems.filter { it.hidden && liveness.isLive(it) }.map { it.key }.toSet()

    /**
     * Number of live hidden items of every kind (`숨긴 항목 <n>`, B-04, E-08), hidden gallery images
     * included: they make the project count as having hidden items (encryption default, LIB-51).
     */
    val count: Int get() = characters.size + terms.size + places.size + factions.size + pages.size + scenes.size + galleryImages.size

    val any: Boolean get() = count > 0

    fun of(kind: HideableKind): Set<Key> = when (kind) {
        HideableKind.CHARACTER -> characters
        HideableKind.TERM -> terms
        HideableKind.PAGE -> pages
        HideableKind.SCENE -> scenes
        HideableKind.PLACE -> places
        HideableKind.FACTION -> factions
        HideableKind.GALLERY_IMAGE -> galleryImages
    }

    fun isHidden(kind: HideableKind, key: Key): Boolean = key in of(kind)

    fun isEntityHidden(kind: EntityKind, key: Key): Boolean = when (kind) {
        EntityKind.CHARACTER -> key in characters
        EntityKind.PLACE -> key in places
        EntityKind.TERM -> key in terms
        EntityKind.FACTION -> key in factions
    }

    fun isEndHidden(ref: EntityRef): Boolean = when (ref.kind.known) {
        RelationEndKind.CHARACTER -> ref.key in characters
        RelationEndKind.FACTION -> ref.key in factions
        null -> false
    }

    /** A relation with a hidden end belongs to that hidden item. */
    fun isRelationHidden(relation: Relation): Boolean = isEndHidden(relation.endA) || isEndHidden(relation.endB)

    /** The target of an `@` link (or recorder link) is a hidden item; unit targets count when the scene is hidden. */
    fun isLinkTargetHidden(target: LinkTarget): Boolean = when (target.kind.known) {
        LinkTargetKind.CHARACTER -> target.key in characters
        LinkTargetKind.TERM -> target.key in terms
        LinkTargetKind.PLACE -> target.key in places
        LinkTargetKind.FACTION -> target.key in factions
        LinkTargetKind.PAGE -> target.key in pages
        LinkTargetKind.UNIT -> target.key in scenes
        else -> false
    }

    /** A change record about a hidden item (its field, display name, relation, region or term reveal). */
    fun isRecordHidden(record: ChangeRecord): Boolean = when (val t = record.target) {
        is ChangeTarget.Field -> t.entityKind.known?.let { isEntityHidden(it, t.entityKey) } ?: false
        is ChangeTarget.DisplayName -> t.characterKey in characters
        is ChangeTarget.RelationSlot -> index.relation(t.relationKey)?.let(::isRelationHidden) ?: false
        is ChangeTarget.RegionShape -> index.region(t.regionKey)?.second?.let { r ->
            r.factionKey?.let { it in factions } == true || r.placeKey?.let { it in places } == true
        } ?: false
        is ChangeTarget.TermReveal -> t.termKey in terms
        is ChangeTarget.Unknown -> false
    }
}
