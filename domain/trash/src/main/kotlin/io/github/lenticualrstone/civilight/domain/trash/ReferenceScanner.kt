package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.docLinks
import io.github.lenticualrstone.civilight.model.world.ItemRefKind

/** What kind of place holds a reference (rule 20 §2 tables, CST-41, CST-47). */
enum class RefKind {
    /** Dialogue speaker of a block or a spoken choice option (`대사 화자`). */
    SPEAKER,

    /** Scene `등장` without lines (`씬 등장`). */
    SILENT_CAST,

    /** An `@` link run on any surface (`@연결`). */
    LINK,

    /** Scene `장소 · 지역`. */
    SCENE_PLACE,

    /** A field `연결` value (base value or a `◆` record's value). */
    FIELD_LINK,

    /** Term `관련`. */
    RELATED,

    /** An item node on a canvas. */
    CANVAS_NODE,

    /** Region, stamp, node-map node, polygon province or script asset linked to a place or faction. */
    MAP_LINK,

    /** A route point attached to one of the place's pins. */
    ROUTE_END,

    /** A map whose parent is a pin of the place (or, for a map, whose parent map it is). */
    SUB_MAP,

    /** A place whose `지역/소속` parent is the place. */
    CHILD_PLACE,

    /** (2026-11-26) A faction whose `거점` is the place. */
    BASE_PLACE,

    /** Timeline event place. */
    EVENT_PLACE,

    /** Timeline event related term. */
    EVENT_TERM,

    /** Story event on a scene. */
    EVENT_SCENE,

    /** Flashback scene time naming an event. */
    FLASHBACK,

    /** A `◆` record anchored at a world event. */
    EVENT_ANCHOR,

    /** Relative scene time based on the scene. */
    BASE_SCENE,

    /** A Life Recorder `연결` chip (`레코더 칩`). */
    RECORDER_LINK,
}

/** Where an `@` link run sits. */
enum class LinkSurface {
    BLOCK, TRANSITION, CHARACTER_BODY, FIELD, VOICE_LINE, ARCHIVE_DOC, WORLD_DESCRIPTION, EVENT_DESCRIPTION, PAGE,
    RECORD_VALUE, RECORDER,

    /** The description of a live gallery image (55 GAL-13). */
    GALLERY,
}

/**
 * One reference to an item. [ownerKey] is the row that holds it (block, scene, link key for a link,
 * entity, canvas node, map overlay, recorder link …); [slot] tells which part (option key, field
 * definition key, surface); [sceneKey] is set for references inside the manuscript.
 */
data class RefSite(
    val kind: RefKind,
    val ownerKey: Key,
    val slot: String? = null,
    val sceneKey: Key? = null,
    /** For a link: the row whose text holds it (block, page, character, record …). */
    val containerKey: Key? = null,
) {
    /** Stable identity of the reference site (used for restore reports). */
    val id: String get() = listOfNotNull(kind.name, ownerKey, slot).joinToString(":")
}

/** An `@` link run in a live container. */
data class LinkSite(
    val link: TextRun.Link,
    val surface: LinkSurface,
    val containerKey: Key,
    /** Scene of a manuscript link (block or transition). */
    val sceneKey: Key? = null,
    /** Block of a manuscript link (choice options count as their block). */
    val blockKey: Key? = null,
)

/**
 * Finds every key reference to an item (§3: speaker, `@` link, scene place, field `연결`, term
 * `관련`, canvas node, region/stamp/asset place link, route end, sub-map, recorder link, silent
 * cast …). References inside non-live containers are not counted (CST-41): only live blocks of live
 * scenes, live pages, live characters and so on are scanned. Plain text is never a reference.
 *
 * [recorder] (optional) adds recorder `연결` chips and `@` runs of recorder text that point into this
 * project.
 */
class ReferenceScanner(private val doc: ProjectDoc, private val recorder: RecorderLibrary? = null) {
    private val live = doc.liveness

    /** Every `@` link run in a live container. */
    val links: List<LinkSite> by lazy { collectLinks() }

    private val linksByTarget: Map<Key, List<LinkSite>> by lazy { links.groupBy { it.link.target.key } }

    /** Live `@` links whose target is [key] (any kind). */
    fun linksTo(key: Key): List<LinkSite> = linksByTarget[key].orEmpty()

    /** References to [item] in live containers, in a stable order. */
    fun referencesTo(item: ItemId): List<RefSite> = when (item.kind) {
        ItemKind.CHARACTER -> speakers(item.key) + silentCast(item.key) + linkRefs(item.key, LinkTargetKind.CHARACTER) +
            canvasNodes(item.key, CanvasNodeKind.CHARACTER) + recorderLinks(item.key, RecorderTargetKind.CHARACTER)
        ItemKind.PLACE -> scenePlaces(PlaceRefKind.PLACE) { it == item.key } + linkRefs(item.key, LinkTargetKind.PLACE) +
            fieldLinks(item.key) + related(item.key, ItemRefKind.PLACE) + placeMapLinks(item.key) + routeEnds(item.key) +
            subMapsOfPlace(item.key) + childPlaces(item.key) + basedFactions(item.key) + eventPlaces(PlaceRefKind.PLACE) { it == item.key } +
            recorderLinks(item.key, RecorderTargetKind.PLACE)
        ItemKind.TERM -> linkRefs(item.key, LinkTargetKind.TERM) + fieldLinks(item.key) + related(item.key, ItemRefKind.TERM) +
            eventTerms(item.key) + canvasNodes(item.key, CanvasNodeKind.TERM) + recorderLinks(item.key, RecorderTargetKind.TERM)
        ItemKind.FACTION -> linkRefs(item.key, LinkTargetKind.FACTION) + fieldLinks(item.key) + related(item.key, ItemRefKind.FACTION) +
            factionMapLinks(item.key) + canvasNodes(item.key, CanvasNodeKind.FACTION) + recorderLinks(item.key, RecorderTargetKind.FACTION)
        ItemKind.EVENT -> eventAnchors(item.key) + flashbacks(item.key) + canvasNodes(item.key, CanvasNodeKind.EVENT)
        ItemKind.PAGE -> linkRefs(item.key, LinkTargetKind.PAGE) + recorderLinks(item.key, RecorderTargetKind.PAGE)
        ItemKind.MAP -> subMapsOfMap(item.key) + regionRefsOfMap(item.key)
        ItemKind.UNIT -> baseScenes(item.key) + eventScenes(item.key) + linkRefs(item.key, LinkTargetKind.UNIT) +
            recorderLinks(item.key, RecorderTargetKind.UNIT)
        ItemKind.FIELD_DEF, ItemKind.CHANGE, ItemKind.CANVAS, ItemKind.BLOCK, ItemKind.GALLERY_ITEM -> emptyList()
    }

    // --- Manuscript ---

    private fun speakers(key: Key): List<RefSite> = doc.manuscript.liveBlocks.flatMap { block ->
        val own = if (block.kind.known == BlockKind.DIALOGUE && block.speakerKey == key) {
            listOf(RefSite(RefKind.SPEAKER, block.key, sceneKey = block.sceneKey))
        } else {
            emptyList()
        }
        own + block.options.filter { it.speakerKey == key }
            .map { RefSite(RefKind.SPEAKER, block.key, slot = it.key, sceneKey = block.sceneKey) }
    }

    private fun silentCast(key: Key): List<RefSite> = doc.manuscript.liveScenes
        .filter { key in it.silentCast }
        .map { RefSite(RefKind.SILENT_CAST, it.key, sceneKey = it.key) }

    private fun scenePlaces(kind: PlaceRefKind, match: (Key) -> Boolean): List<RefSite> = doc.manuscript.liveScenes
        .filter { it.place.matches(kind, match) }
        .map { RefSite(RefKind.SCENE_PLACE, it.key, sceneKey = it.key) }

    private fun baseScenes(key: Key): List<RefSite> = doc.manuscript.liveScenes
        .filter { (it.time as? SceneTime.Relative)?.baseKey == key }
        .map { RefSite(RefKind.BASE_SCENE, it.key, sceneKey = it.key) }

    private fun flashbacks(key: Key): List<RefSite> = doc.manuscript.liveScenes
        .filter { (it.time as? SceneTime.Flashback)?.eventKey == key }
        .map { RefSite(RefKind.FLASHBACK, it.key, sceneKey = it.key) }

    private fun linkRefs(key: Key, kind: LinkTargetKind): List<RefSite> = linksTo(key)
        .filter { it.link.target.kind.known == kind }
        .map { RefSite(RefKind.LINK, it.link.linkKey, slot = it.surface.name, sceneKey = it.sceneKey, containerKey = it.containerKey) }

    // --- Cast and world ---

    private fun fieldLinks(key: Key): List<RefSite> {
        val base = liveEntities().flatMap { (_, entityKey, fields) ->
            fields.filter { (defKey, value) -> live.isFieldDefLive(defKey) && value.linkTargets().contains(key) }
                .map { (defKey, _) -> RefSite(RefKind.FIELD_LINK, entityKey, slot = defKey, containerKey = entityKey) }
        }
        val records = doc.changes.records.filter { live.isLive(it) && it.target is ChangeTarget.Field }.mapNotNull { record ->
            val value = record.fieldValue() ?: return@mapNotNull null
            if (key in value.linkTargets()) RefSite(RefKind.FIELD_LINK, record.key, slot = "record", containerKey = record.key) else null
        }
        return base + records
    }

    private fun related(key: Key, kind: ItemRefKind): List<RefSite> = doc.world.terms
        .filter { it.deletedAt == null && it.related.any { r -> r.key == key && r.kind.known == kind } }
        .map { RefSite(RefKind.RELATED, it.key) }

    private fun eventTerms(key: Key): List<RefSite> = doc.world.events
        .filter { it.deletedAt == null && key in it.relatedTermKeys }
        .map { RefSite(RefKind.EVENT_TERM, it.key) }

    private fun eventPlaces(kind: PlaceRefKind, match: (Key) -> Boolean): List<RefSite> = doc.world.events
        .filter { it.deletedAt == null && it.place.matches(kind, match) }
        .map { RefSite(RefKind.EVENT_PLACE, it.key) }

    private fun eventScenes(key: Key): List<RefSite> = doc.world.events
        .filter { it.deletedAt == null && it.sceneKey == key }
        .map { RefSite(RefKind.EVENT_SCENE, it.key) }

    private fun eventAnchors(key: Key): List<RefSite> = doc.changes.records
        .filter { live.isLive(it) && (it.anchor as? Anchor.WorldEvent)?.eventKey == key }
        .map { RefSite(RefKind.EVENT_ANCHOR, it.key) }

    private fun childPlaces(key: Key): List<RefSite> = doc.world.places
        .filter { it.deletedAt == null && it.parentPlaceKey == key }
        .map { RefSite(RefKind.CHILD_PLACE, it.key) }

    private fun basedFactions(key: Key): List<RefSite> = doc.world.factions
        .filter { it.deletedAt == null && it.baseKey == key }
        .map { RefSite(RefKind.BASE_PLACE, it.key, slot = "baseKey") }

    private fun canvasNodes(key: Key, kind: CanvasNodeKind): List<RefSite> = doc.canvases.values
        .filter { it.deletedAt == null }
        .flatMap { canvas ->
            canvas.nodes.filter { it.refKey == key && it.kind.known == kind }
                .map { RefSite(RefKind.CANVAS_NODE, it.key, slot = canvas.key) }
        }

    // --- Maps ---

    private fun liveMaps() = doc.maps.values.filter { it.deletedAt == null }

    private fun pinKeysOfPlace(key: Key): Set<Key> =
        doc.maps.values.flatMap { m -> m.pins.filter { it.placeKey == key }.map { it.key } }.toSet()

    private fun placeMapLinks(key: Key): List<RefSite> {
        val overlays = liveMaps().flatMap { map ->
            map.regions.filter { it.placeKey == key }.map { RefSite(RefKind.MAP_LINK, it.key, slot = map.key) } +
                map.stamps.filter { it.placeKey == key }.map { RefSite(RefKind.MAP_LINK, it.key, slot = map.key) } +
                map.nodes.filter { it.placeKey == key }.map { RefSite(RefKind.MAP_LINK, it.key, slot = map.key) }
        }
        val assets = doc.assets.scriptAssets.filter { it.placeKey == key }.map { RefSite(RefKind.MAP_LINK, it.key, slot = "asset") }
        return overlays + assets
    }

    private fun factionMapLinks(key: Key): List<RefSite> = liveMaps().flatMap { map ->
        map.regions.filter { it.factionKey == key }.map { RefSite(RefKind.MAP_LINK, it.key, slot = map.key) } +
            map.poly?.provinces.orEmpty().filter { it.factionKey == key }.map { RefSite(RefKind.MAP_LINK, it.key, slot = map.key) }
    }

    private fun routeEnds(placeKey: Key): List<RefSite> {
        val pins = pinKeysOfPlace(placeKey)
        if (pins.isEmpty()) return emptyList()
        return liveMaps().flatMap { map ->
            map.routes.flatMap { route ->
                route.points.mapNotNull { it.pinKey }.filter { it in pins }.distinct()
                    .map { pin -> RefSite(RefKind.ROUTE_END, route.key, slot = pin) }
            }
        }
    }

    private fun subMapsOfPlace(placeKey: Key): List<RefSite> {
        val pins = pinKeysOfPlace(placeKey)
        return liveMaps().filter { it.parent?.pinKey in pins }.map { RefSite(RefKind.SUB_MAP, it.key) }
    }

    private fun subMapsOfMap(mapKey: Key): List<RefSite> =
        liveMaps().filter { it.parent?.mapKey == mapKey }.map { RefSite(RefKind.SUB_MAP, it.key) }

    private fun regionRefsOfMap(mapKey: Key): List<RefSite> {
        val regions = doc.maps[mapKey]?.regions.orEmpty().map { it.key }.toSet()
        if (regions.isEmpty()) return emptyList()
        return scenePlaces(PlaceRefKind.REGION) { it in regions } + eventPlaces(PlaceRefKind.REGION) { it in regions }
    }

    // --- Recorder ---

    private fun recorderLinks(key: Key, kind: RecorderTargetKind): List<RefSite> {
        val library = recorder ?: return emptyList()
        return library.links
            .filter { it.projectKey == doc.key && it.targetKey == key && it.targetKind.known == kind && it.purgedAt == null }
            .filter { library.record(it.recordKey)?.deletedAt == null }
            .map { RefSite(RefKind.RECORDER_LINK, it.key, containerKey = it.recordKey) }
    }

    // --- Link collection ---

    private data class EntityFields(val kind: EntityKind, val key: Key, val fields: Map<Key, FieldValue>)

    private fun liveEntities(): List<EntityFields> =
        doc.cast.characters.filter { it.deletedAt == null }.map { EntityFields(EntityKind.CHARACTER, it.key, it.fields) } +
            doc.world.places.filter { it.deletedAt == null }.map { EntityFields(EntityKind.PLACE, it.key, it.fields) } +
            doc.world.terms.filter { it.deletedAt == null }.map { EntityFields(EntityKind.TERM, it.key, it.fields) } +
            doc.world.factions.filter { it.deletedAt == null }.map { EntityFields(EntityKind.FACTION, it.key, it.fields) }

    private fun collectLinks(): List<LinkSite> = buildList {
        fun runs(runs: List<TextRun>, surface: LinkSurface, container: Key, scene: Key? = null, block: Key? = null) {
            runs.filterIsInstance<TextRun.Link>().forEach { add(LinkSite(it, surface, container, scene, block)) }
        }

        fun rich(doc: RichDoc, surface: LinkSurface, container: Key) {
            doc.docLinks.forEach { add(LinkSite(it, surface, container)) }
        }

        for (block in doc.manuscript.liveBlocks) {
            runs(block.runs, LinkSurface.BLOCK, block.key, block.sceneKey, block.key)
            block.options.forEach { runs(it.runs, LinkSurface.BLOCK, block.key, block.sceneKey, block.key) }
        }
        for (scene in doc.manuscript.liveScenes) {
            scene.transitionRuns?.let { runs(it, LinkSurface.TRANSITION, scene.key, scene.key) }
        }
        for (character in doc.cast.characters.filter { it.deletedAt == null }) {
            rich(character.speechBody, LinkSurface.CHARACTER_BODY, character.key)
            rich(character.secretBody, LinkSurface.CHARACTER_BODY, character.key)
        }
        for (entity in liveEntities()) {
            for ((defKey, value) in entity.fields) {
                val payload = value.payload as? FieldPayload.Rich ?: continue
                if (live.isFieldDefLive(defKey)) rich(payload.doc, LinkSurface.FIELD, entity.key)
            }
        }
        for (line in doc.cast.voiceLines.filter { live.isCharacterLive(it.characterKey) }) rich(line.text, LinkSurface.VOICE_LINE, line.key)
        for (archive in doc.cast.archiveDocs.filter { live.isCharacterLive(it.characterKey) }) rich(archive.body, LinkSurface.ARCHIVE_DOC, archive.key)
        for (term in doc.world.terms.filter { it.deletedAt == null }) rich(term.description, LinkSurface.WORLD_DESCRIPTION, term.key)
        for (place in doc.world.places.filter { it.deletedAt == null }) rich(place.description, LinkSurface.WORLD_DESCRIPTION, place.key)
        for (faction in doc.world.factions.filter { it.deletedAt == null }) rich(faction.description, LinkSurface.WORLD_DESCRIPTION, faction.key)
        for (event in doc.world.events.filter { it.deletedAt == null }) rich(event.description, LinkSurface.EVENT_DESCRIPTION, event.key)
        for (page in doc.pages.values.filter { it.deletedAt == null }) rich(page.blocks, LinkSurface.PAGE, page.key)
        for (item in doc.galleryItems.filter { live.isLive(it) }) item.caption?.let { rich(it, LinkSurface.GALLERY, item.key) }
        for (record in doc.changes.records.filter { live.isLive(it) && it.target is ChangeTarget.Field }) {
            val payload = record.fieldValue()?.payload as? FieldPayload.Rich ?: continue
            rich(payload.doc, LinkSurface.RECORD_VALUE, record.key)
        }
        recorder?.records?.filter { it.deletedAt == null }?.forEach { record ->
            record.runs.filterIsInstance<TextRun.Link>()
                .filter { it.target.projectKey == doc.key }
                .forEach { add(LinkSite(it, LinkSurface.RECORDER, record.key)) }
        }
    }
}

internal fun PlaceRef?.matches(kind: PlaceRefKind, match: (Key) -> Boolean): Boolean =
    this != null && this.kind.known == kind && match(this.key)

/** Targets of a `연결` field value. */
internal fun FieldValue.linkTargets(): List<Key> = (payload as? FieldPayload.Link)?.targetKeys.orEmpty()

/** The value of a `field` record as a [FieldValue], or null when absent or not decodable. */
internal fun io.github.lenticualrstone.civilight.model.change.ChangeRecord.fieldValue(): FieldValue? =
    runCatching { valueAs(FieldValue.serializer()) }.getOrNull()
