package io.github.lenticualrstone.civilight.model.doc

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.cast.ArchiveDoc
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationKind
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.cast.VoiceLine
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.map.MapRegion
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.ScriptAsset
import io.github.lenticualrstone.civilight.model.serialName
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.world.Term
import io.github.lenticualrstone.civilight.model.world.TimelineEvent

/**
 * Keyed lookups over a [ProjectDoc] (live and trashed rows alike; use [ProjectDoc.liveness] to
 * filter). Built once per doc instance, on first use.
 */
class ProjectIndex internal constructor(private val doc: ProjectDoc) {
    private val structure = doc.project.structure

    // Story tree.
    private val unitsByKey: Map<Key, StoryUnit> = doc.story.units.associateBy { it.key }
    private val childrenByParent: Map<Key?, List<StoryUnit>> = doc.story.units
        .groupBy { u -> u.parentKey?.takeIf { it in unitsByKey } }
        .mapValues { (_, list) -> list.sortedWith(UNIT_ORDER) }

    // Blocks.
    private val blocksByKey: Map<Key, Block> = doc.allBlocks.associateBy { it.key }
    private val groupOfBlock: Map<Key, Key> = buildMap {
        for ((groupKey, file) in doc.episodes) for (b in file.blocks) put(b.key, groupKey)
    }
    private val blocksByScene: Map<Key, List<Block>> = doc.allBlocks.groupBy { it.sceneKey }
        .mapValues { (_, list) -> list.sortedWith(BLOCK_ORDER) }

    // Cast and world.
    private val charactersByKey = doc.cast.characters.associateBy { it.key }
    private val relationsByKey = doc.cast.relations.associateBy { it.key }
    private val relationsByPair = doc.cast.relations.groupBy { it.pairId }
    private val voiceLinesByCharacter = doc.cast.voiceLines.groupBy { it.characterKey }
    private val archiveDocsByCharacter = doc.cast.archiveDocs.groupBy { it.characterKey }
    private val placesByKey = doc.world.places.associateBy { it.key }
    private val termsByKey = doc.world.terms.associateBy { it.key }
    private val factionsByKey = doc.world.factions.associateBy { it.key }
    private val eventsByKey = doc.world.events.associateBy { it.key }
    private val fieldDefsByKey = doc.project.fieldDefs.associateBy { it.key }
    private val tagsByKey = doc.project.tags.associateBy { it.key }
    private val relationKindsByKey = doc.project.relationKinds.associateBy { it.key }

    // Maps.
    private val regionsByKey: Map<Key, Pair<WorldMap, MapRegion>> = buildMap {
        for (map in doc.maps.values) for (r in map.regions) put(r.key, map to r)
    }
    private val pinsByKey: Map<Key, Pair<WorldMap, Pin>> = buildMap {
        for (map in doc.maps.values) for (p in map.pins) put(p.key, map to p)
    }

    // Records and the rest.
    private val recordsByKey = doc.changes.records.associateBy { it.key }
    private val recordsByTarget: Map<String, List<ChangeRecord>> = doc.changes.records
        .filter { it.target.targetId != null }
        .groupBy { it.target.targetId!! }
    private val imageUsesByOwnerSlot = doc.assets.imageUses.associateBy { Triple(it.ownerKind.raw, it.ownerKey, it.slot.raw) }
    private val blobsByHash = doc.assets.blobs.associateBy { it.hash }
    private val scriptAssetsByKey = doc.assets.scriptAssets.associateBy { it.key }
    private val acksByKey = doc.checks.acknowledgements.associateBy { it.key }
    private val nameTagsByKey = doc.tombstones.nameTags.associateBy { it.key }
    private val trashEntriesByKey = doc.trash.entries.associateBy { it.key }

    // Gallery (lazy: most screens never ask).
    private val galleryByKey: Map<Key, GalleryItem> by lazy { doc.galleryItems.associateBy { it.key } }
    private val galleryBySource: Map<String, List<GalleryItem>> by lazy { doc.galleryItems.groupBy { it.source } }

    // --- Story tree ---

    fun unit(key: Key): StoryUnit? = unitsByKey[key]

    /** Children of [parentKey] (null = top level) in order, live and trashed. */
    fun children(parentKey: Key?): List<StoryUnit> = childrenByParent[parentKey].orEmpty()

    fun parent(unit: StoryUnit): StoryUnit? = unit.parentKey?.let(unitsByKey::get)

    /** Ancestors from the parent up to the top. */
    fun ancestors(unit: StoryUnit): List<StoryUnit> {
        val out = mutableListOf<StoryUnit>()
        val seen = mutableSetOf(unit.key)
        var current = parent(unit)
        while (current != null && seen.add(current.key)) {
            out += current
            current = parent(current)
        }
        return out
    }

    fun isScene(unit: StoryUnit): Boolean = unit.level == structure.sceneLevel

    /** All scene-level units in manuscript (depth-first) order, live and trashed. */
    val allScenesInOrder: List<StoryUnit> by lazy {
        val out = mutableListOf<StoryUnit>()
        val seen = mutableSetOf<Key>()
        fun visit(u: StoryUnit) {
            if (!seen.add(u.key)) return
            if (isScene(u)) out += u
            children(u.key).forEach(::visit)
        }
        children(null).forEach(::visit)
        out
    }

    /**
     * The numbering unit of a scene (§4.1): its ancestor at the episode level, or the project key
     * for a 1-level structure. A scene without an episode-level ancestor (inconsistent tree) uses
     * its topmost ancestor (itself when top-level). Null for an unknown scene.
     */
    fun numberingUnitKeyOfScene(sceneKey: Key): Key? {
        val scene = unitsByKey[sceneKey] ?: return null
        val episodeLevel = structure.episodeLevel ?: return doc.project.key
        if (scene.level == episodeLevel) return scene.key
        val ancestors = ancestors(scene)
        return ancestors.firstOrNull { it.level == episodeLevel }?.key ?: (ancestors.lastOrNull() ?: scene).key
    }

    /** Scenes whose numbering unit is [numberingUnitKey], in manuscript order. */
    fun scenesOfNumberingUnit(numberingUnitKey: Key): List<StoryUnit> =
        allScenesInOrder.filter { numberingUnitKeyOfScene(it.key) == numberingUnitKey }

    // --- Blocks ---

    fun block(key: Key): Block? = blocksByKey[key]

    /** Blocks of a scene by order (live and trashed). */
    fun blocksOfScene(sceneKey: Key): List<Block> = blocksByScene[sceneKey].orEmpty()

    /** The [ProjectDoc.episodes] key whose file currently holds the block. */
    fun episodeGroupOfBlock(blockKey: Key): Key? = groupOfBlock[blockKey]

    /** Discarded voice IDs of a numbering unit (episode file plus any read from `story/index.json`). */
    fun discardedVoiceIds(numberingUnitKey: Key): List<DiscardedVoiceId> {
        val inIndex = doc.story.discardedVoiceIds.filter { d ->
            blocksByKey[d.blockKey]?.let { numberingUnitKeyOfScene(it.sceneKey) } == numberingUnitKey
        }
        return doc.episodes[numberingUnitKey]?.discardedVoiceIds.orEmpty() + inIndex
    }

    /** Every discarded voice ID of the project. */
    val allDiscardedVoiceIds: List<DiscardedVoiceId> by lazy {
        doc.episodes.values.flatMap { it.discardedVoiceIds } + doc.story.discardedVoiceIds
    }

    // --- Cast ---

    fun character(key: Key): Character? = charactersByKey[key]
    fun relation(key: Key): Relation? = relationsByKey[key]

    /** The relation of an unordered pair, if any. */
    fun relationBetween(a: EntityRef, b: EntityRef): Relation? = relationsByPair[Relation.pairIdOf(a, b)]?.firstOrNull()

    /** Relations with [ref] as an end. */
    fun relationsOf(ref: EntityRef): List<Relation> = doc.cast.relations.filter { it.involves(ref) }

    fun voiceLines(characterKey: Key): List<VoiceLine> =
        voiceLinesByCharacter[characterKey].orEmpty().sortedWith(compareBy<VoiceLine> { it.order }.thenBy { it.key })

    fun archiveDocs(characterKey: Key): List<ArchiveDoc> =
        archiveDocsByCharacter[characterKey].orEmpty().sortedWith(compareBy<ArchiveDoc> { it.order }.thenBy { it.key })

    fun fieldDef(key: Key): FieldDef? = fieldDefsByKey[key]

    /** Field definitions of an entity kind in order. */
    fun fieldDefs(kind: EntityKind): List<FieldDef> = doc.project.fieldDefs
        .filter { it.entityKind.known == kind }
        .sortedWith(compareBy<FieldDef> { it.order }.thenBy { it.key })

    fun tag(key: Key): Tag? = tagsByKey[key]
    fun relationKind(key: Key): RelationKind? = relationKindsByKey[key]

    // --- World ---

    fun place(key: Key): Place? = placesByKey[key]
    fun term(key: Key): Term? = termsByKey[key]
    fun faction(key: Key): Faction? = factionsByKey[key]
    fun event(key: Key): TimelineEvent? = eventsByKey[key]

    /** Base field values of an item with custom fields. */
    fun fieldsOf(kind: EntityKind, key: Key): Map<Key, FieldValue>? = when (kind) {
        EntityKind.CHARACTER -> character(key)?.fields
        EntityKind.PLACE -> place(key)?.fields
        EntityKind.TERM -> term(key)?.fields
        EntityKind.FACTION -> faction(key)?.fields
    }

    /** Display name of an item that can be a link or reference target, from its live or trashed row. */
    fun nameOf(kind: EntityKind, key: Key): String? = when (kind) {
        EntityKind.CHARACTER -> character(key)?.name
        EntityKind.PLACE -> place(key)?.name
        EntityKind.TERM -> term(key)?.name
        EntityKind.FACTION -> faction(key)?.name
    }

    // --- Maps ---

    fun map(key: Key): WorldMap? = doc.maps[key]

    /** A region and its map, by the region's project-unique key. */
    fun region(key: Key): Pair<WorldMap, MapRegion>? = regionsByKey[key]

    fun pin(key: Key): Pair<WorldMap, Pin>? = pinsByKey[key]

    /** Child maps of a pin (derived from `WorldMap.parent`). */
    fun childMaps(pinKey: Key): List<WorldMap> = doc.maps.values.filter { it.parent?.pinKey == pinKey }

    // --- Records, checks, assets, trash ---

    fun record(key: Key): ChangeRecord? = recordsByKey[key]

    /** All records of a target (live and trashed), in file order. */
    fun recordsOf(target: ChangeTarget): List<ChangeRecord> =
        target.targetId?.let { recordsByTarget[it] }.orEmpty()

    fun acknowledgement(key: Key): Acknowledgement? = acksByKey[key]

    fun imageUse(ownerKind: ImageOwnerKind, ownerKey: Key, slot: ImageSlot): ImageUse? =
        imageUsesByOwnerSlot[Triple(ownerKind.serialName, ownerKey, slot.serialName)]

    fun blob(hash: String): Blob? = blobsByHash[hash]
    fun scriptAsset(key: Key): ScriptAsset? = scriptAssetsByKey[key]
    fun nameTag(key: Key): NameTag? = nameTagsByKey[key]
    fun trashEntry(key: Key): TrashEntry? = trashEntriesByKey[key]

    // --- Gallery (55 §3, §12.5) ---

    /** One gallery item (live or trashed). */
    fun galleryItem(key: Key): GalleryItem? = galleryByKey[key]

    /** Every item of one owner (live and trashed) in `직접 순서`. */
    fun galleryItemsOf(ownerKey: Key): List<GalleryItem> = doc.gallery[ownerKey]?.ordered.orEmpty()

    /** Items holding [source] in any gallery (live and trashed; uniqueness, `이미 있음`, blob references). */
    fun galleryItemsWithSource(source: String): List<GalleryItem> = galleryBySource[source].orEmpty()

    companion object {
        val UNIT_ORDER: Comparator<StoryUnit> = compareBy<StoryUnit> { it.order }.thenBy { it.key }
        val BLOCK_ORDER: Comparator<Block> = compareBy<Block> { it.order }.thenBy { it.key }
    }
}
