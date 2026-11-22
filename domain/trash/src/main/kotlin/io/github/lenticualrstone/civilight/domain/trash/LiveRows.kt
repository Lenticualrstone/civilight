package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * The keys of every live row of a project, by category (§5.1). Comparing two snapshots gives what a
 * delete takes along ("goes to the trash with the item") without a second ownership table: owned
 * rows are exactly those live before the mark and not live after it.
 */
internal data class LiveRows(
    val items: Set<Key>,
    val units: Set<Key>,
    val blocks: Set<Key>,
    val relations: Set<Key>,
    val records: Set<Key>,
    val voiceLines: Set<Key>,
    val archiveDocs: Set<Key>,
    val imageUses: Set<Key>,
    val pins: Set<Key>,
    val overlays: Set<Key>,
    /** `entityKey|fieldDefKey`. */
    val fieldValues: Set<String>,
    val rejections: Set<Key>,
    val canvasParts: Set<Key>,
    /** Link keys of `@` runs in live containers. */
    val links: Set<Key>,
) {
    /** Every live key (used to drop acknowledgements whose subject stopped being live, STO-41). */
    val all: Set<Key> by lazy {
        items + units + blocks + relations + records + voiceLines + archiveDocs + imageUses + pins + overlays +
            rejections + canvasParts + links
    }

    companion object {
        fun of(doc: ProjectDoc): LiveRows {
            val live = doc.liveness
            val items = buildSet {
                doc.cast.characters.filter { it.deletedAt == null }.forEach { add(it.key) }
                doc.world.places.filter { it.deletedAt == null }.forEach { add(it.key) }
                doc.world.terms.filter { it.deletedAt == null }.forEach { add(it.key) }
                doc.world.factions.filter { it.deletedAt == null }.forEach { add(it.key) }
                doc.world.events.filter { it.deletedAt == null }.forEach { add(it.key) }
                doc.project.fieldDefs.filter { it.deletedAt == null }.forEach { add(it.key) }
                doc.maps.values.filter { it.deletedAt == null }.forEach { add(it.key) }
                doc.pages.values.filter { it.deletedAt == null }.forEach { add(it.key) }
                doc.canvases.values.filter { it.deletedAt == null }.forEach { add(it.key) }
            }
            val pins = HashSet<Key>()
            val overlays = HashSet<Key>()
            for (map in doc.maps.values) {
                if (!live.isMapLive(map.key)) continue
                map.pins.filter { live.isOverlayLive(map.key, it) }.forEach { pins += it.key }
                (map.layers + map.regions + map.routes + map.labels + map.stamps + map.nodes + map.nodeLinks)
                    .forEach { overlays += it.key }
                map.drawnLines.forEach { overlays += it.key }
            }
            val fieldValues = HashSet<String>()
            fun fields(kind: EntityKind, key: Key, values: Map<Key, *>) {
                for (defKey in values.keys) if (live.isFieldValueLive(kind, key, defKey)) fieldValues += "$key|$defKey"
            }
            doc.cast.characters.forEach { fields(EntityKind.CHARACTER, it.key, it.fields) }
            doc.world.places.forEach { fields(EntityKind.PLACE, it.key, it.fields) }
            doc.world.terms.forEach { fields(EntityKind.TERM, it.key, it.fields) }
            doc.world.factions.forEach { fields(EntityKind.FACTION, it.key, it.fields) }
            return LiveRows(
                items = items,
                units = doc.story.units.filter(live::isLive).mapTo(HashSet()) { it.key },
                blocks = doc.allBlocks.filter(live::isLive).mapTo(HashSet()) { it.key },
                relations = doc.cast.relations.filter(live::isLive).mapTo(HashSet()) { it.key },
                records = doc.changes.records.filter(live::isLive).mapTo(HashSet()) { it.key },
                voiceLines = doc.cast.voiceLines.filter { live.isCharacterLive(it.characterKey) }.mapTo(HashSet()) { it.key },
                archiveDocs = doc.cast.archiveDocs.filter { live.isCharacterLive(it.characterKey) }.mapTo(HashSet()) { it.key },
                imageUses = doc.assets.imageUses.filter(live::isLive).mapTo(HashSet()) { it.key },
                pins = pins,
                overlays = overlays,
                fieldValues = fieldValues,
                rejections = doc.world.terms.filter { it.deletedAt == null }.flatMap { t -> t.rejections.map { it.key } }.toSet(),
                canvasParts = doc.canvases.values.filter { it.deletedAt == null }
                    .flatMap { c -> c.nodes.map { it.key } + c.edges.map { it.key } }.toSet(),
                links = ReferenceScanner(doc).links.mapTo(HashSet()) { it.link.linkKey },
            )
        }
    }
}

/** Rows live in [before] and not in [after]: what a delete takes along, by category. */
internal data class OwnedRows(
    val relations: Set<Key>,
    val records: Set<Key>,
    val voiceLines: Set<Key>,
    val archiveDocs: Set<Key>,
    val imageUses: Set<Key>,
    val pins: Set<Key>,
    val overlays: Set<Key>,
    val blocks: Set<Key>,
    val units: Set<Key>,
    val fieldValues: Set<String>,
    val rejections: Set<Key>,
    val canvasParts: Set<Key>,
) {
    companion object {
        fun between(before: LiveRows, after: LiveRows) = OwnedRows(
            relations = before.relations - after.relations,
            records = before.records - after.records,
            voiceLines = before.voiceLines - after.voiceLines,
            archiveDocs = before.archiveDocs - after.archiveDocs,
            imageUses = before.imageUses - after.imageUses,
            pins = before.pins - after.pins,
            overlays = before.overlays - after.overlays,
            blocks = before.blocks - after.blocks,
            units = before.units - after.units,
            fieldValues = before.fieldValues - after.fieldValues,
            rejections = before.rejections - after.rejections,
            canvasParts = before.canvasParts - after.canvasParts,
        )
    }
}
