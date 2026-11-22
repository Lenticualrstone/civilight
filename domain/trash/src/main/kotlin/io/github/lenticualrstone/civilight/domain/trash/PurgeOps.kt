package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.serialName
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import io.github.lenticualrstone.civilight.model.trash.TrashEntry

/** A purged item that recorder links and placements may point at (they get `purgedAt` and keep the label). */
data class PurgedTarget(val kind: ItemKind, val key: Key, val label: String, val hidden: Boolean)

data class PurgeResult(
    val doc: ProjectDoc,
    val purgedEntries: List<TrashEntry>,
    val nameTags: List<NameTag>,
    /** Every row key removed (items, owned rows, blocks, units). */
    val removedKeys: Set<Key>,
    val purgedTargets: List<PurgedTarget>,
    /**
     * Blob hashes no remaining row of this project references; they are dropped from this
     * project's `assets/index.json`. The global blob store deletes a file only when no project
     * references it (§5.3).
     */
    val unreferencedBlobs: List<String>,
)

/** `영구 삭제`, `비우기` and the 30-day purge (§5.3, LIB-39). */
object PurgeOps {
    fun purge(doc: ProjectDoc, entryKey: Key, ctx: TrashContext): PurgeResult = purgeEntries(doc, listOf(entryKey), ctx)

    /** The daily job (and the check on app start): every entry with `d ≤ 0` (T-08). */
    fun purgeDue(doc: ProjectDoc, ctx: TrashContext): PurgeResult =
        purgeEntries(doc, doc.trash.entries.filter(ctx::isDue).map { it.key }, ctx)

    /**
     * `비우기`: every entry regardless of the selected filter; entries of hidden items are purged only
     * while their gate is open ([hiddenUnlocked]), locked ones stay (LIB-39 `ED`).
     */
    fun emptyTrash(doc: ProjectDoc, ctx: TrashContext, hiddenUnlocked: Boolean): PurgeResult =
        purgeEntries(doc, doc.trash.entries.filter { hiddenUnlocked || !it.hiddenAtDelete }.map { it.key }, ctx)

    fun purgeEntries(doc: ProjectDoc, entryKeys: List<Key>, ctx: TrashContext): PurgeResult {
        val state = State(doc, ctx)
        val purged = mutableListOf<TrashEntry>()
        for (entryKey in entryKeys.distinct()) {
            val entry = state.doc.index.trashEntry(entryKey) ?: continue
            purged += entry
            val kind = ItemKind.of(entry.kind)
            if (kind != null) entry.targetKeys.forEach { state.purgeItem(ItemId(kind, it)) }
            state.doc = state.doc.removeEntry(entryKey)
        }
        state.finish()
        return PurgeResult(
            doc = state.doc,
            purgedEntries = purged,
            nameTags = state.nameTags,
            removedKeys = state.removed,
            purgedTargets = state.targets,
            unreferencedBlobs = state.blobs,
        )
    }

    private class State(var doc: ProjectDoc, val ctx: TrashContext) {
        val removed = LinkedHashSet<Key>()
        val nameTags = mutableListOf<NameTag>()
        val targets = mutableListOf<PurgedTarget>()
        val log = mutableListOf<DeletionLogEntry>()
        val namedItems = mutableListOf<ItemId>()
        var blobs: List<String> = emptyList()

        fun purgeItem(item: ItemId) {
            if (!doc.exists(item)) return
            val name = doc.itemName(item)
            val hidden = doc.itemHidden(item)
            when (item.kind) {
                ItemKind.CHARACTER -> purgeEntity(item, EntityKind.CHARACTER, RelationEndKind.CHARACTER)
                ItemKind.PLACE -> {
                    purgeEntity(item, EntityKind.PLACE, null)
                    val pins = doc.maps.values.flatMap { m -> m.pins.filter { it.placeKey == item.key }.map { it.key } }.toSet()
                    removed += pins
                    doc = doc.copy(maps = doc.maps.mapValues { (_, m) -> if (m.pins.any { it.key in pins }) m.copy(pins = m.pins.filter { it.key !in pins }) else m })
                }
                ItemKind.TERM -> {
                    val linkKeys = ReferenceScanner(doc).linksTo(item.key).map { it.link.linkKey }.toSet()
                    purgeEntity(item, EntityKind.TERM, null)
                    // `▲` acknowledgements of its uses go too (CST-47 `▲` records deleted).
                    dropAcks { it.checkType == CheckType.TERM_BEFORE_REVEAL && it.subjectKeys.any { k -> k in linkKeys } }
                }
                ItemKind.FACTION -> purgeEntity(item, EntityKind.FACTION, RelationEndKind.FACTION)
                ItemKind.FIELD_DEF -> {
                    val k = item.key
                    doc = doc.copy(
                        project = doc.project.copy(fieldDefs = doc.project.fieldDefs.filter { it.key != k }),
                        cast = doc.cast.copy(characters = doc.cast.characters.map { if (k in it.fields) it.copy(fields = it.fields - k) else it }),
                        world = doc.world.copy(
                            places = doc.world.places.map { if (k in it.fields) it.copy(fields = it.fields - k) else it },
                            terms = doc.world.terms.map { if (k in it.fields) it.copy(fields = it.fields - k) else it },
                            factions = doc.world.factions.map { if (k in it.fields) it.copy(fields = it.fields - k) else it },
                        ),
                    )
                    removeRecords { (it.target as? ChangeTarget.Field)?.fieldDefKey == k }
                    removed += k
                }
                ItemKind.CHANGE -> {
                    removeRecords { it.key == item.key }
                }
                ItemKind.MAP -> {
                    val map = doc.maps[item.key]
                    map?.let { m -> removed += m.pins.map { it.key } + m.regions.map { it.key } + m.routes.map { it.key } + m.stamps.map { it.key } }
                    doc = doc.copy(maps = doc.maps - item.key)
                    removeRecords { (it.target as? ChangeTarget.RegionShape)?.mapKey == item.key }
                    removed += item.key
                }
                ItemKind.EVENT -> {
                    doc = doc.copy(world = doc.world.copy(events = doc.world.events.filter { it.key != item.key }))
                    // §5.5 ED: the anchor stays (it sorts first among events) and the record becomes orphaned.
                    doc = doc.copy(
                        changes = doc.changes.copy(
                            records = doc.changes.records.map { r ->
                                val anchor = r.anchor
                                if (anchor is Anchor.WorldEvent && anchor.eventKey == item.key && r.anchorState.known != AnchorState.ORPHANED) {
                                    r.copy(anchorState = AnchorState.ORPHANED.open(), originalAnchor = anchor, updatedAt = ctx.now, updatedBy = ctx.deviceId)
                                } else {
                                    r
                                }
                            },
                        ),
                    )
                    removed += item.key
                }
                ItemKind.PAGE -> {
                    doc = doc.copy(pages = doc.pages - item.key)
                    removeImages(ImageOwnerKind.PAGE, item.key)
                    removed += item.key
                }
                ItemKind.CANVAS -> {
                    doc.canvases[item.key]?.let { c -> removed += c.nodes.map { it.key } + c.edges.map { it.key } }
                    doc = doc.copy(canvases = doc.canvases - item.key)
                    removed += item.key
                }
                ItemKind.UNIT -> purgeUnit(item.key)
                ItemKind.BLOCK -> purgeBlocks(setOf(item.key))
            }
            log += DeletionLogEntry(item.key, logKind(item.kind), ctx.now)
            item.kind.nameTagKind?.takeIf { item.kind.isNamedItem }?.let { tagKind ->
                val tag = NameTag(
                    key = item.key, kind = tagKind.open(), name = name.orEmpty(),
                    voiceCode = nameTagVoiceCode, hidden = hidden, state = NameTagState.PURGED.open(), purgedAt = ctx.now,
                )
                nameTags += tag
                doc = doc.copy(tombstones = doc.tombstones.copy(nameTags = doc.tombstones.nameTags.filter { it.key != item.key } + tag))
                namedItems += item
            }
            if (item.kind.recorderTargetKind != null && item.kind != ItemKind.UNIT) {
                targets += PurgedTarget(item.kind, item.key, name.orEmpty(), hidden)
            }
        }

        private var nameTagVoiceCode: String? = null

        private fun purgeEntity(item: ItemId, kind: EntityKind, endKind: RelationEndKind?) {
            val k = item.key
            nameTagVoiceCode = if (kind == EntityKind.CHARACTER) doc.index.character(k)?.voiceCode else null
            val relations: Set<Key> = if (endKind == null) {
                emptySet()
            } else {
                doc.cast.relations.filter { it.hasEnd(endKind, k) }.map { it.key }.toSet()
            }
            doc = doc.copy(
                cast = doc.cast.copy(
                    characters = if (kind == EntityKind.CHARACTER) doc.cast.characters.filter { it.key != k } else doc.cast.characters,
                    relations = doc.cast.relations.filter { it.key !in relations },
                    voiceLines = doc.cast.voiceLines.filter { it.characterKey != k || kind != EntityKind.CHARACTER }.also { kept ->
                        removed += doc.cast.voiceLines.filter { it !in kept }.map { it.key }
                    },
                    archiveDocs = doc.cast.archiveDocs.filter { it.characterKey != k || kind != EntityKind.CHARACTER }.also { kept ->
                        removed += doc.cast.archiveDocs.filter { it !in kept }.map { it.key }
                    },
                ),
                world = doc.world.copy(
                    places = if (kind == EntityKind.PLACE) doc.world.places.filter { it.key != k } else doc.world.places,
                    terms = if (kind == EntityKind.TERM) doc.world.terms.filter { it.key != k } else doc.world.terms,
                    factions = if (kind == EntityKind.FACTION) doc.world.factions.filter { it.key != k } else doc.world.factions,
                ),
            )
            removed += relations
            removed += k
            removeRecords { r ->
                when (val t = r.target) {
                    is ChangeTarget.Field -> t.entityKind.known == kind && t.entityKey == k
                    is ChangeTarget.DisplayName -> kind == EntityKind.CHARACTER && t.characterKey == k
                    is ChangeTarget.RelationSlot -> t.relationKey in relations
                    is ChangeTarget.TermReveal -> kind == EntityKind.TERM && t.termKey == k
                    else -> false
                }
            }
            removeImages(imageOwnerOf(kind), k)
            if (kind == EntityKind.CHARACTER) {
                doc = doc.copy(checks = doc.checks.copy(travelOverrides = doc.checks.travelOverrides.filter { it.characterKey != k }))
            }
        }

        private fun purgeUnit(key: Key) {
            val units = LinkedHashSet<Key>()
            fun collect(k: Key) {
                if (!units.add(k)) return
                doc.index.children(k).forEach { collect(it.key) }
            }
            collect(key)
            val codes = UnitCodes(doc)
            units.forEach { k ->
                val unit = doc.index.unit(k)
                targets += PurgedTarget(ItemKind.UNIT, k, codes.codeAndTitle(k).orEmpty(), unit?.hidden ?: false)
            }
            val blocks = doc.allBlocks.filter { it.sceneKey in units }.map { it.key }.toSet()
            doc = doc.copy(story = doc.story.copy(units = doc.story.units.filter { it.key !in units }))
            purgeBlocks(blocks)
            doc = doc.copy(
                episodes = doc.episodes.filter { (unitKey, file) -> unitKey !in units || file.discardedVoiceIds.isNotEmpty() },
                checks = doc.checks.copy(
                    travelOverrides = doc.checks.travelOverrides.filter { it.fromSceneKey !in units && it.toSceneKey !in units },
                ),
            )
            dropRejections(units)
            removed += units
            units.filter { it != key }.forEach { log += DeletionLogEntry(it, logKind(ItemKind.UNIT), ctx.now) }
            dropAcks { ack -> ack.subjectKeys.any { it in units } }
        }

        private fun purgeBlocks(keys: Set<Key>) {
            if (keys.isEmpty()) return
            doc = doc.copy(episodes = doc.episodes.mapValues { (_, f) -> if (f.blocks.any { it.key in keys }) f.copy(blocks = f.blocks.filter { it.key !in keys }) else f })
            dropRejections(keys)
            dropAcks { ack -> ack.subjectKeys.any { it in keys } }
            removed += keys
            keys.forEach { log += DeletionLogEntry(it, logKind(ItemKind.BLOCK), ctx.now) }
        }

        private fun dropRejections(containers: Set<Key>) {
            doc = doc.copy(
                world = doc.world.copy(
                    terms = doc.world.terms.map { t ->
                        if (t.rejections.any { it.containerKey in containers }) t.copy(rejections = t.rejections.filter { it.containerKey !in containers }) else t
                    },
                ),
            )
        }

        private fun removeRecords(predicate: (ChangeRecord) -> Boolean) {
            val gone = doc.changes.records.filter(predicate)
            if (gone.isEmpty()) return
            removed += gone.map { it.key }
            doc = doc.copy(changes = doc.changes.copy(records = doc.changes.records - gone.toSet()))
        }

        private fun removeImages(owner: ImageOwnerKind, key: Key) {
            val gone = doc.assets.imageUses.filter { it.ownerKind.known == owner && it.ownerKey == key }
            if (gone.isEmpty()) return
            removed += gone.map { it.key }
            doc = doc.copy(assets = doc.assets.copy(imageUses = doc.assets.imageUses - gone.toSet()))
        }

        private fun dropAcks(predicate: (io.github.lenticualrstone.civilight.model.checks.Acknowledgement) -> Boolean) {
            val keep = doc.checks.acknowledgements.filterNot { it.checkType != CheckType.DELETED_ITEM && predicate(it) }
            if (keep.size != doc.checks.acknowledgements.size) doc = doc.copy(checks = doc.checks.copy(acknowledgements = keep))
        }

        fun finish() {
            // Acknowledgements about removed rows (other than deleted.item, which stay while references remain).
            dropAcks { ack -> ack.subjectKeys.any { it in removed } }
            val scanner = ReferenceScanner(doc)
            val unreferenced = namedItems.filter { scanner.referencesTo(it).isEmpty() }.map { it.key }.toSet()
            doc = doc.dropDeletedItemAcks(unreferenced)
            // Entries whose targets no longer exist (a block inside a purged scene, a record of a purged item).
            doc = doc.copy(
                trash = doc.trash.copy(
                    entries = doc.trash.entries.mapNotNull { entry ->
                        val kind = ItemKind.of(entry.kind) ?: return@mapNotNull entry
                        val left = entry.targetKeys.filter { doc.exists(ItemId(kind, it)) }
                        when {
                            left.isEmpty() -> null
                            left.size == entry.targetKeys.size -> entry
                            else -> entry.copy(targetKeys = left)
                        }
                    },
                ),
            )
            // Deletion log: add, and keep 30 days.
            val today = ctx.today
            val kept = doc.deletions.entries.filter { ctx.localDate(it.deletedAt).plusDays(TrashEntry.RETENTION_DAYS).isAfter(today) }
            doc = doc.copy(deletions = doc.deletions.copy(entries = kept + log))
            // Blobs no row references any more.
            val referenced = referencedBlobs(doc)
            val (keepBlobs, dropBlobs) = doc.assets.blobs.partition { it.hash in referenced }
            blobs = dropBlobs.map { it.hash }
            if (dropBlobs.isNotEmpty()) doc = doc.copy(assets = doc.assets.copy(blobs = keepBlobs))
        }
    }

    private fun Relation.hasEnd(kind: RelationEndKind, key: Key): Boolean =
        (endA.kind.known == kind && endA.key == key) || (endB.kind.known == kind && endB.key == key)

    private fun imageOwnerOf(kind: EntityKind): ImageOwnerKind = when (kind) {
        EntityKind.CHARACTER -> ImageOwnerKind.CHARACTER
        EntityKind.PLACE -> ImageOwnerKind.PLACE
        EntityKind.TERM -> ImageOwnerKind.TERM
        EntityKind.FACTION -> ImageOwnerKind.FACTION
    }

    private fun logKind(kind: ItemKind): String = when (kind) {
        ItemKind.UNIT -> "unit"
        ItemKind.BLOCK -> "block"
        else -> kind.trashKind.serialName
    }

    /** Blob hashes referenced by any row (live or trashed) of the project. */
    internal fun referencedBlobs(doc: ProjectDoc): Set<String> = buildSet {
        doc.assets.imageUses.mapNotNullTo(this) { it.blobHash }
        doc.assets.scriptAssets.mapNotNullTo(this) { it.fileHash }
        doc.maps.values.forEach { m ->
            m.baseImageHash?.let(::add)
            m.sketch?.photoHash?.let(::add)
        }
    }
}
