package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.PairHolder
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.trash.MergeChoice
import io.github.lenticualrstone.civilight.model.trash.MergeRecord
import io.github.lenticualrstone.civilight.model.trash.MovedRef
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.ItemRefKind
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.world.Term
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A field both items fill with different values (`다른 칸`); the default is the target's value. */
data class FieldConflict(val fieldDefKey: Key, val fieldName: String, val mergedValue: FieldValue, val targetValue: FieldValue)

/** A relation slot both items' relations with the same partner fill differently (target's orientation). */
data class SlotConflict(val slot: RelationSlotName, val mergedValue: Any?, val targetValue: Any?)

/** Both items have a relation with [partnerKey] (one relation per pair, CST-45). */
data class RelationOverlap(
    val partnerKey: Key,
    val partnerName: String,
    val mergedRelationKey: Key,
    val targetRelationKey: Key,
    val conflicts: List<SlotConflict>,
)

/** The comparison sheet (`RCMerge`, CST-45, WLD-15). */
data class MergePreview(
    val kind: ItemKind,
    val mergedKey: Key,
    val targetKey: Key,
    val mergedName: String,
    val targetName: String,
    /** References that move to the target (`옮길 참조 · N`), live containers only. */
    val references: List<RefSite>,
    val fieldConflicts: List<FieldConflict>,
    /** Fields both have with the same value (`같은 항목`). */
    val sameFields: Int,
    /** Target-empty fields taken from the merged item without asking (`빈 칸 채움`). */
    val filled: List<Key>,
    val relationOverlaps: List<RelationOverlap>,
    /** Relations of the merged item that move to the target unchanged. */
    val movedRelations: List<Key>,
    /** The relation between the two items, deleted by the merge (`서로 관계 1 · 지워짐`). */
    val mutualRelationKey: Key?,
    /** Fixed voice IDs on the merged item's lines (unchanged; rule 12 `◇`). */
    val fixedVoiceIds: Int,
    /** Route ends that move from the merged place's pins to the target's (`경로 끝 N`). */
    val routeEnds: Int,
    /** Both terms have a live `◇ 첫 공개` (comparison row, default target). */
    val revealConflict: Boolean,
) {
    val referenceCount: Int get() = references.size
}

/** The author's choices on the comparison sheet; everything else keeps the target's value. */
data class MergeChoices(
    val fieldsFromMerged: Set<Key> = emptySet(),
    /** (partner key, slot in the target relation's orientation). */
    val slotsFromMerged: Set<Pair<Key, RelationSlotName>> = emptySet(),
    val revealFromMerged: Boolean = false,
)

data class MergeResult(
    val doc: ProjectDoc,
    val preview: MergePreview,
    val entry: TrashEntry,
    val record: MergeRecord,
    val change: DocChange,
)

/**
 * 합치기 (CST-45, WLD-15, §5.2): references are rewritten to the target key (recorded in
 * [MergeRecord.movedRefs]), chosen and target-empty values copied, the mutual relation deleted,
 * then the merged item is trashed with `reason = merged`, `mergedIntoKey = target`.
 */
object MergeOps {
    /** Key in [MergeRecord.ext] of the counts the restore sheet shows (`옮긴 참조 · 관계 · 칸`). */
    const val SUMMARY = "summary"

    fun preview(doc: ProjectDoc, kind: ItemKind, mergedKey: Key, targetKey: Key): MergePreview {
        require(kind.isNamedItem) { "merge is for characters, places, terms and factions" }
        val merged = ItemId(kind, mergedKey)
        val scanner = ReferenceScanner(doc)
        val mergedFields = fieldsOf(doc, merged)
        val targetFields = fieldsOf(doc, ItemId(kind, targetKey))
        val defs = doc.project.fieldDefs.filter { it.deletedAt == null && it.entityKind.known == kind.entityKind }
        val conflicts = mutableListOf<FieldConflict>()
        val filled = mutableListOf<Key>()
        var same = 0
        for (def in defs) {
            // A field holds a value when it has a base value or live `◆` records (a value that lives
            // only in records, like 리엔's 소속, is not empty and is never filled over, CST-45).
            val m = mergedFields[def.key]
            val t = targetFields[def.key]
            val mHistory = fieldHistory(doc, kind, mergedKey, def.key)
            val tHistory = fieldHistory(doc, kind, targetKey, def.key)
            if (m == null && mHistory.isEmpty()) continue
            when {
                t == null && tHistory.isEmpty() -> filled += def.key
                t == m && sameHistory(doc, mHistory, tHistory) -> same++
                else -> {
                    val mShown = shownValue(doc, m, mHistory) ?: continue
                    val tShown = shownValue(doc, t, tHistory) ?: continue
                    conflicts += FieldConflict(def.key, def.name, mShown, tShown)
                }
            }
        }
        val endKind = endKindOf(kind)
        val overlaps = mutableListOf<RelationOverlap>()
        val movedRelations = mutableListOf<Key>()
        var mutual: Key? = null
        if (endKind != null) {
            val mergedRef = EntityRef(endKind, mergedKey)
            val targetRef = EntityRef(endKind, targetKey)
            for (relation in doc.index.relationsOf(mergedRef).filter { doc.liveness.isLive(it) }) {
                val partner = relation.otherEnd(mergedRef) ?: continue
                if (partner.sameAs(targetRef)) {
                    mutual = relation.key
                    continue
                }
                val targetRelation = doc.index.relationBetween(targetRef, partner)
                if (targetRelation == null) {
                    movedRelations += relation.key
                } else {
                    overlaps += RelationOverlap(
                        partnerKey = partner.key,
                        partnerName = doc.index.character(partner.key)?.name ?: doc.index.faction(partner.key)?.name.orEmpty(),
                        mergedRelationKey = relation.key,
                        targetRelationKey = targetRelation.key,
                        conflicts = slotConflicts(relation, mergedRef, targetRelation, targetRef),
                    )
                }
            }
        }
        val fixed = if (kind == ItemKind.CHARACTER) {
            doc.manuscript.liveBlocks.sumOf { b ->
                val own = if (b.kind.known == BlockKind.DIALOGUE && b.speakerKey == mergedKey && b.voice?.mode?.known == VoiceMode.FIXED) 1 else 0
                own + b.options.count { it.speakerKey == mergedKey && it.voice?.mode?.known == VoiceMode.FIXED }
            }
        } else {
            0
        }
        val routeEnds = if (kind == ItemKind.PLACE) pinMapping(doc, mergedKey, targetKey).let { mapping ->
            doc.maps.values.sumOf { m -> m.routes.sumOf { r -> r.points.count { it.pinKey in mapping.keys } } }
        } else {
            0
        }
        val revealConflict = kind == ItemKind.TERM && liveReveal(doc, mergedKey) != null && liveReveal(doc, targetKey) != null
        return MergePreview(
            kind = kind, mergedKey = mergedKey, targetKey = targetKey,
            mergedName = doc.itemName(merged).orEmpty(), targetName = doc.itemName(ItemId(kind, targetKey)).orEmpty(),
            references = scanner.referencesTo(merged),
            fieldConflicts = conflicts, sameFields = same, filled = filled,
            relationOverlaps = overlaps, movedRelations = movedRelations, mutualRelationKey = mutual,
            fixedVoiceIds = fixed, routeEnds = routeEnds, revealConflict = revealConflict,
        )
    }

    /** Applies the merge with [choices] (defaults: target values everywhere). */
    fun merge(
        doc: ProjectDoc,
        kind: ItemKind,
        mergedKey: Key,
        targetKey: Key,
        ctx: TrashContext,
        choices: MergeChoices = MergeChoices(),
    ): MergeResult {
        val preview = preview(doc, kind, mergedKey, targetKey)
        val mergedItem = ItemId(kind, mergedKey)
        val snapshot = snapshotOf(doc, mergedItem)
        val moved = mutableListOf<MovedRef>()
        val chosen = mutableListOf<MergeChoice>()
        var d = doc

        // 1. References.
        d = rewriteReferences(d, kind, mergedKey, targetKey, ctx, moved)

        // 2. Field values: target-empty filled, chosen ones taken (the merged base value, or none when
        // its value lives only in records), and the merged item's `◆` records of a taken field move
        // to the target; the target's own records stay, except one at the very same point.
        val mergedFields = fieldsOf(d, mergedItem)
        val takenKeys = preview.filled + preview.fieldConflicts.filter { it.fieldDefKey in choices.fieldsFromMerged }.map { it.fieldDefKey }
        val taken: Map<Key, FieldValue?> = takenKeys.associateWith { mergedFields[it] }
        chosen += preview.fieldConflicts.filter { it.fieldDefKey in choices.fieldsFromMerged }.map { MergeChoice(fieldDefKey = it.fieldDefKey) }
        if (taken.isNotEmpty()) {
            d = d.updateFields(ItemId(kind, targetKey), ctx) { fields ->
                taken.entries.fold(fields) { acc, (key, value) -> if (value == null) acc - key else acc + (key to value) }
            }
            d = moveFieldRecords(d, kind, mergedKey, targetKey, takenKeys, ctx, moved)
        }

        // 3. Relations.
        val endKind = endKindOf(kind)
        if (endKind != null) {
            val mergedRef = EntityRef(endKind, mergedKey)
            val targetRef = EntityRef(endKind, targetKey)
            preview.mutualRelationKey?.let { key ->
                d = d.copy(
                    cast = d.cast.copy(relations = d.cast.relations.filter { it.key != key }),
                    changes = d.changes.copy(records = d.changes.records.filterNot { (it.target as? ChangeTarget.RelationSlot)?.relationKey == key }),
                )
            }
            for (overlap in preview.relationOverlaps) {
                val mergedRelation = d.index.relation(overlap.mergedRelationKey) ?: continue
                val targetRelation = d.index.relation(overlap.targetRelationKey) ?: continue
                var updated = targetRelation
                for (slot in RelationSlotName.entries) {
                    val fromMerged = mapSlot(slot, mergedRelation, mergedRef, targetRelation, targetRef) ?: continue
                    val mergedValue = mergedRelation.slotValue(fromMerged)
                    val targetValue = targetRelation.slotValue(slot)
                    val pick = (targetValue == null && mergedValue != null) || (overlap.partnerKey to slot) in choices.slotsFromMerged
                    if (pick && mergedValue != null) {
                        updated = updated.withSlot(slot, remapHolder(mergedValue, mergedRelation, mergedRef, targetRelation, targetRef))
                        chosen += MergeChoice(relationKey = targetRelation.key, slot = slot.open())
                    }
                }
                // Both sets of ◆ are kept: the merged relation's records now belong to the target's relation.
                d = d.copy(
                    cast = d.cast.copy(
                        relations = d.cast.relations.filter { it.key != mergedRelation.key }
                            .map { if (it.key == targetRelation.key && updated != targetRelation) updated.copy(updatedAt = ctx.now, updatedBy = ctx.deviceId) else it },
                    ),
                    changes = d.changes.copy(
                        records = d.changes.records.map { r ->
                            val t = r.target as? ChangeTarget.RelationSlot ?: return@map r
                            if (t.relationKey != mergedRelation.key) return@map r
                            val slot = t.slot.known ?: return@map r
                            val mapped = mapSlot(slot, mergedRelation, mergedRef, targetRelation, targetRef, reverse = true) ?: slot
                            moved += MovedRef("record", r.key, "target.relationKey")
                            r.copy(target = ChangeTarget.RelationSlot(targetRelation.key, mapped.open()), updatedAt = ctx.now, updatedBy = ctx.deviceId)
                        },
                    ),
                )
            }
            for (key in preview.movedRelations) {
                d = d.copy(
                    cast = d.cast.copy(
                        relations = d.cast.relations.mapIf({ it.key == key }) { r ->
                            moved += MovedRef("relation", r.key, if (r.endA.sameAs(mergedRef)) "endA" else "endB")
                            r.copy(
                                endA = if (r.endA.sameAs(mergedRef)) targetRef else r.endA,
                                endB = if (r.endB.sameAs(mergedRef)) targetRef else r.endB,
                                updatedAt = ctx.now, updatedBy = ctx.deviceId,
                            )
                        },
                    ),
                )
            }
        }

        // 3b. Gallery (55 GAL-20, §12.1): the merged character's images move to the target, appended in
        // their order; one whose source the target already holds (live or trashed) stays behind.
        if (kind == ItemKind.CHARACTER) d = moveGallery(d, mergedKey, targetKey, ctx, moved)

        // 4. Terms: first reveal and aliases.
        if (kind == ItemKind.TERM) d = mergeTermReveal(d, mergedKey, targetKey, choices.revealFromMerged, ctx, chosen)

        // 5. The merged item goes to the trash as `합침 → <target>`.
        val today = ctx.today
        val entry = TrashEntry(
            key = ctx.newKey(), kind = kind.trashKind.open(), projectKey = d.key, targetKeys = listOf(mergedKey),
            reason = TrashReason.MERGED.open(), mergedIntoKey = targetKey,
            hiddenAtDelete = d.itemHidden(mergedItem) || d.project.protections.hidden,
            deletedAt = ctx.now, purgeOn = TrashEntry.purgeDateFor(today),
        )
        val before = d
        d = d.setMark(mergedItem, ctx.now, entry.key, ctx.now, ctx.deviceId)
        d = d.copy(trash = d.trash.copy(entries = d.trash.entries + entry))
        d = TrashOps.dropAcksOfNonLive(before, d).first
        val record = MergeRecord(
            key = ctx.newKey(), kind = kind.nameTagKind!!.open(), mergedKey = mergedKey, targetKey = targetKey, at = ctx.now,
            movedRefs = moved, filledFieldDefKeys = preview.filled, chosenFromMerged = chosen,
            deletedMutualRelationKey = preview.mutualRelationKey, mergedOriginal = snapshot,
            ext = JsonObject(
                mapOf(
                    SUMMARY to JsonObject(
                        mapOf(
                            "refs" to JsonPrimitive(preview.referenceCount),
                            "relations" to JsonPrimitive(preview.relationOverlaps.size + preview.movedRelations.size),
                            "fields" to JsonPrimitive(taken.size),
                        ),
                    ),
                ),
            ),
        )
        d = d.copy(tombstones = d.tombstones.copy(mergeRecords = d.tombstones.mergeRecords + record))
        return MergeResult(d, preview, entry, record, DocChange(doc, d))
    }

    /** `되돌리기` of a merge: every change above is undone (TC-20-17). */
    fun undo(current: ProjectDoc, result: MergeResult): ProjectDoc = result.change.revert(current)

    /**
     * Brings a merged item back from the trash (TC-20-19): rebuilt from [MergeRecord.mergedOriginal]
     * as a separate item with its own original field values; moved references, relations and taken
     * values stay with the target, so it has no relations.
     */
    internal fun restoreMerged(doc: ProjectDoc, item: ItemId, ctx: TrashContext): ProjectDoc {
        val record = doc.tombstones.mergeRecords.filter { it.mergedKey == item.key }.maxByOrNull { it.at }
        val restored = record?.let { rebuild(doc, item, it.mergedOriginal, ctx) }
        return restored ?: doc.setMark(item, null, null, ctx.now, ctx.deviceId)
    }

    /** Moves [from]'s gallery items to [to] (see step 3b of [merge]); each move is a `galleryItem` [MovedRef]. */
    private fun moveGallery(doc: ProjectDoc, from: Key, to: Key, ctx: TrashContext, moved: MutableList<MovedRef>): ProjectDoc {
        val source = doc.index.galleryItemsOf(from).filter { it.ownerKind.known == GalleryOwnerKind.CHARACTER }
        if (source.isEmpty()) return doc
        val targetItems = doc.index.galleryItemsOf(to)
        val held = targetItems.mapTo(HashSet()) { it.source }
        val moving = source.filter { it.source !in held }
        if (moving.isEmpty()) return doc
        var last = targetItems.maxOfOrNull { it.order }
        val movedItems = moving.map { item ->
            val order = OrderKeys.after(last).also { last = it }
            moved += MovedRef(GALLERY_ITEM_REF, item.key, "ownerKey")
            item.copy(ownerKey = to, order = order, updatedAt = ctx.now, updatedBy = ctx.deviceId)
        }
        val movingKeys = moving.mapTo(HashSet()) { it.key }
        val d = doc.removeGalleryItems { it.key in movingKeys }
        val targetFile = d.gallery[to] ?: GalleryFile(GalleryOwnerKind.CHARACTER.open(), to)
        return d.copy(gallery = d.gallery + (to to targetFile.copy(items = targetFile.items + movedItems)))
    }

    /** [MovedRef.ownerKind] of a gallery item moved by a merge (§12.1). */
    const val GALLERY_ITEM_REF = "galleryItem"

    // --- Reference rewriting ---

    private fun rewriteReferences(doc: ProjectDoc, kind: ItemKind, from: Key, to: Key, ctx: TrashContext, moved: MutableList<MovedRef>): ProjectDoc {
        val at = ctx.now
        val by = ctx.deviceId
        val linkKind = when (kind) {
            ItemKind.CHARACTER -> LinkTargetKind.CHARACTER
            ItemKind.PLACE -> LinkTargetKind.PLACE
            ItemKind.TERM -> LinkTargetKind.TERM
            else -> LinkTargetKind.FACTION
        }
        var (d, owners) = doc.rewriteLinks(
            { link -> if (link.target.key == from && link.target.kind.known == linkKind) link.copy(target = link.target.copy(key = to)) else link },
            at, by,
        )
        owners.forEach { moved += MovedRef(it.kind, it.key, it.field) }

        if (kind == ItemKind.CHARACTER) {
            d = d.copy(
                episodes = d.episodes.mapValues { (_, file) ->
                    file.copy(
                        blocks = file.blocks.map { b ->
                            var changed = false
                            val speaker = if (b.speakerKey == from) {
                                changed = true; moved += MovedRef("block", b.key, "speakerKey"); to
                            } else {
                                b.speakerKey
                            }
                            val options = b.options.map { o ->
                                if (o.speakerKey == from) {
                                    changed = true; moved += MovedRef("block", b.key, "options.${o.key}.speakerKey"); o.copy(speakerKey = to)
                                } else {
                                    o
                                }
                            }
                            if (changed) b.copy(speakerKey = speaker, options = options, updatedAt = at, updatedBy = by) else b
                        },
                    )
                },
                story = d.story.copy(
                    units = d.story.units.map { u ->
                        if (from !in u.silentCast) return@map u
                        moved += MovedRef("unit", u.key, "silentCast")
                        val cast = if (to in u.silentCast) u.silentCast - from else u.silentCast.map { if (it == from) to else it }
                        u.copy(silentCast = cast, updatedAt = at, updatedBy = by)
                    },
                ),
                // (0.2.0, WLD-74) an event's `관련 인물` follows the merge.
                world = d.world.copy(
                    events = d.world.events.map { e ->
                        if (from !in e.relatedCharacterKeys) return@map e
                        moved += MovedRef("event", e.key, "relatedCharacterKeys")
                        e.copy(relatedCharacterKeys = e.relatedCharacterKeys.map { if (it == from) to else it }.distinct(), updatedAt = at, updatedBy = by)
                    },
                ),
            )
        }
        if (kind == ItemKind.PLACE) {
            val placeRef = PlaceRefKind.PLACE
            d = d.copy(
                story = d.story.copy(
                    units = d.story.units.map { u ->
                        if (!u.place.matches(placeRef) { it == from }) return@map u
                        moved += MovedRef("unit", u.key, "place")
                        u.copy(place = PlaceRef(PlaceRefKind.PLACE, to), updatedAt = at, updatedBy = by)
                    },
                ),
                world = d.world.copy(
                    places = d.world.places.map { p ->
                        if (p.parentPlaceKey != from || p.key == to) return@map p
                        moved += MovedRef("place", p.key, "parentPlaceKey")
                        p.copy(parentPlaceKey = to, updatedAt = at, updatedBy = by)
                    },
                    events = d.world.events.map { e ->
                        if (!e.place.matches(placeRef) { it == from }) return@map e
                        moved += MovedRef("event", e.key, "place")
                        e.copy(place = PlaceRef(PlaceRefKind.PLACE, to), updatedAt = at, updatedBy = by)
                    },
                ),
                assets = d.assets.copy(
                    scriptAssets = d.assets.scriptAssets.map { a ->
                        if (a.placeKey != from) return@map a
                        moved += MovedRef("scriptAsset", a.key, "placeKey")
                        a.copy(placeKey = to, updatedAt = at, updatedBy = by)
                    },
                ),
            )
            d = mergePins(d, from, to, ctx, moved)
        }
        if (kind == ItemKind.FACTION) {
            d = d.copy(
                maps = d.maps.mapValues { (_, m) ->
                    val regions = m.regions.map { r ->
                        if (r.factionKey != from) r else r.copy(factionKey = to, updatedAt = at, updatedBy = by).also { moved += MovedRef("region", r.key, "factionKey") }
                    }
                    val poly = m.poly?.let { p ->
                        p.copy(provinces = p.provinces.map { pr -> if (pr.factionKey != from) pr else pr.copy(factionKey = to).also { moved += MovedRef("province", pr.key, "factionKey") } })
                    }
                    if (regions != m.regions || poly != m.poly) m.copy(regions = regions, poly = poly, updatedAt = at, updatedBy = by) else m
                },
            )
        }
        if (kind == ItemKind.TERM) {
            d = d.copy(
                world = d.world.copy(
                    events = d.world.events.map { e ->
                        if (from !in e.relatedTermKeys) return@map e
                        moved += MovedRef("event", e.key, "relatedTermKeys")
                        e.copy(relatedTermKeys = (e.relatedTermKeys.map { if (it == from) to else it }).distinct(), updatedAt = at, updatedBy = by)
                    },
                ),
            )
        }
        // Field `연결` values (base values and record values), term `관련`, canvas item nodes.
        d = d.mapEntityFields(at, by) { ownerKind, ownerKey, defKey, value ->
            val payload = value.payload as? FieldPayload.Link ?: return@mapEntityFields value
            if (from !in payload.targetKeys) return@mapEntityFields value
            moved += MovedRef(ownerKind, ownerKey, "fields.$defKey")
            value.copy(payload = payload.copy(targetKeys = payload.targetKeys.map { if (it == from) to else it }.distinct()))
        }
        val itemRefKind = when (kind) {
            ItemKind.PLACE -> ItemRefKind.PLACE
            ItemKind.TERM -> ItemRefKind.TERM
            ItemKind.FACTION -> ItemRefKind.FACTION
            else -> null
        }
        if (itemRefKind != null) {
            d = d.copy(
                world = d.world.copy(
                    terms = d.world.terms.map { t ->
                        if (t.related.none { it.key == from && it.kind.known == itemRefKind }) return@map t
                        moved += MovedRef("term", t.key, "related")
                        val related = t.related.map { if (it.key == from && it.kind.known == itemRefKind) it.copy(key = to) else it }
                            .distinctBy { it.kind.raw to it.key }.filterNot { it.key == t.key }
                        t.copy(related = related, updatedAt = at, updatedBy = by)
                    },
                ),
            )
        }
        val nodeKind = when (kind) {
            ItemKind.CHARACTER -> io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind.CHARACTER
            ItemKind.TERM -> io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind.TERM
            ItemKind.FACTION -> io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind.FACTION
            else -> null
        }
        if (nodeKind != null) {
            d = d.copy(
                canvases = d.canvases.mapValues { (_, c) ->
                    if (c.nodes.none { it.refKey == from && it.kind.known == nodeKind }) return@mapValues c
                    c.copy(
                        nodes = c.nodes.map { n ->
                            if (n.refKey == from && n.kind.known == nodeKind) n.copy(refKey = to, updatedAt = at, updatedBy = by).also { moved += MovedRef("canvasNode", n.key, "refKey") } else n
                        },
                        updatedAt = at, updatedBy = by,
                    )
                },
            )
        }
        if (kind == ItemKind.PLACE) {
            d = d.copy(
                maps = d.maps.mapValues { (_, m) ->
                    val regions = m.regions.map { r -> if (r.placeKey != from) r else r.copy(placeKey = to).also { moved += MovedRef("region", r.key, "placeKey") } }
                    val stamps = m.stamps.map { s -> if (s.placeKey != from) s else s.copy(placeKey = to).also { moved += MovedRef("stamp", s.key, "placeKey") } }
                    val nodes = m.nodes.map { n -> if (n.placeKey != from) n else n.copy(placeKey = to).also { moved += MovedRef("mapNode", n.key, "placeKey") } }
                    if (regions != m.regions || stamps != m.stamps || nodes != m.nodes) {
                        m.copy(regions = regions, stamps = stamps, nodes = nodes, updatedAt = at, updatedBy = by)
                    } else {
                        m
                    }
                },
            )
        }
        return d
    }

    /** Two place pins on one map: the target's stays, route ends and sub-maps move to it; a lone merged pin moves to the target (`ED`). */
    private fun mergePins(doc: ProjectDoc, from: Key, to: Key, ctx: TrashContext, moved: MutableList<MovedRef>): ProjectDoc {
        val mapping = pinMapping(doc, from, to)
        val at = ctx.now
        val by = ctx.deviceId
        return doc.copy(
            maps = doc.maps.mapValues { (_, m) ->
                val hasTarget = m.pins.any { it.placeKey == to }
                val pins = m.pins.map { p ->
                    if (p.placeKey == from && !hasTarget) p.copy(placeKey = to, updatedAt = at, updatedBy = by).also { moved += MovedRef("pin", p.key, "placeKey") } else p
                }
                val routes = m.routes.map { r ->
                    if (r.points.none { it.pinKey in mapping.keys }) return@map r
                    moved += MovedRef("route", r.key, "points")
                    r.copy(points = r.points.map { pt -> pt.pinKey?.let(mapping::get)?.let { pt.copy(pinKey = it) } ?: pt }, updatedAt = at, updatedBy = by)
                }
                val parent = m.parent?.let { p -> mapping[p.pinKey]?.let { moved += MovedRef("map", m.key, "parent.pinKey"); p.copy(pinKey = it) } ?: p }
                if (pins != m.pins || routes != m.routes || parent != m.parent) m.copy(pins = pins, routes = routes, parent = parent, updatedAt = at, updatedBy = by) else m
            },
        )
    }

    /** Merged pin key → target pin key, for maps where both places have a pin. */
    private fun pinMapping(doc: ProjectDoc, from: Key, to: Key): Map<Key, Key> = buildMap {
        for (m in doc.maps.values) {
            val mergedPin = m.pins.firstOrNull { it.placeKey == from } ?: continue
            val targetPin = m.pins.firstOrNull { it.placeKey == to } ?: continue
            put(mergedPin.key, targetPin.key)
        }
    }

    private fun mergeTermReveal(doc: ProjectDoc, from: Key, to: Key, fromMerged: Boolean, ctx: TrashContext, chosen: MutableList<MergeChoice>): ProjectDoc {
        val mergedReveal = liveReveal(doc, from)
        val targetReveal = liveReveal(doc, to)
        val records = doc.changes.records.map { r ->
            when {
                mergedReveal != null && r.key == mergedReveal.key && (targetReveal == null || fromMerged) ->
                    r.copy(target = ChangeTarget.TermReveal(to), updatedAt = ctx.now, updatedBy = ctx.deviceId)
                targetReveal != null && r.key == targetReveal.key && mergedReveal != null && fromMerged ->
                    r.copy(target = ChangeTarget.TermReveal(from), updatedAt = ctx.now, updatedBy = ctx.deviceId)
                else -> r
            }
        }
        val merged = doc.index.term(from)
        val terms = doc.world.terms.map { t ->
            if (t.key != to || merged == null) return@map t
            // [PROPOSED, WLD Q-11] the merged term's name and aliases become aliases of the target.
            val aliases = (t.aliases + merged.name + merged.aliases).map { it.nfc() }.distinct().filter { it != t.name.nfc() }
            if (aliases == t.aliases) t else t.copy(aliases = aliases, updatedAt = ctx.now, updatedBy = ctx.deviceId)
        }
        return doc.copy(changes = doc.changes.copy(records = records), world = doc.world.copy(terms = terms))
    }

    internal fun liveReveal(doc: ProjectDoc, termKey: Key) = doc.changes.records.firstOrNull {
        it.deletedAt == null && (it.target as? ChangeTarget.TermReveal)?.termKey == termKey
    }

    // --- Fields and relations ---

    private fun fieldsOf(doc: ProjectDoc, item: ItemId): Map<Key, FieldValue> =
        item.kind.entityKind?.let { doc.index.fieldsOf(it, item.key) }.orEmpty()

    private fun fieldTarget(kind: ItemKind, key: Key, defKey: Key): ChangeTarget.Field? =
        kind.entityKind?.let { ChangeTarget.Field(it, key, defKey) }

    /** Live `◆` records of a field of an item, in story order. */
    private fun fieldHistory(doc: ProjectDoc, kind: ItemKind, key: Key, defKey: Key): List<ChangeRecord> =
        fieldTarget(kind, key, defKey)?.let { doc.history(it) }.orEmpty()

    /** Two histories hold the same values at the same points. */
    private fun sameHistory(doc: ProjectDoc, a: List<ChangeRecord>, b: List<ChangeRecord>): Boolean =
        a.map { doc.manuscript.rankOf(it.anchor) to it.value } == b.map { doc.manuscript.rankOf(it.anchor) to it.value }

    /** The value a comparison row shows: the record in effect at `최신`, else the base, else the last record. */
    private fun shownValue(doc: ProjectDoc, base: FieldValue?, history: List<ChangeRecord>): FieldValue? {
        val latest = history.lastOrNull { doc.manuscript.rankOf(it.anchor) <= doc.manuscript.rankOf(LensPoint.Latest) }
        return latest?.valueAs(FieldValue.serializer()) ?: base ?: history.lastOrNull()?.valueAs(FieldValue.serializer())
    }

    /**
     * The merged item's live `◆` records of the taken fields now belong to the target; a live target
     * record at the very same point as a moved one is dropped (the taken value wins there).
     */
    private fun moveFieldRecords(
        doc: ProjectDoc,
        kind: ItemKind,
        mergedKey: Key,
        targetKey: Key,
        defKeys: List<Key>,
        ctx: TrashContext,
        moved: MutableList<MovedRef>,
    ): ProjectDoc {
        val pairs = defKeys.mapNotNull { def ->
            val from = fieldTarget(kind, mergedKey, def) ?: return@mapNotNull null
            val to = fieldTarget(kind, targetKey, def) ?: return@mapNotNull null
            from to to
        }
        if (pairs.isEmpty()) return doc
        val movedAnchors = pairs.associate { (from, to) ->
            to.targetId to doc.changes.records.filter { it.deletedAt == null && it.target.sameAs(from) }.map { it.anchor }
        }
        if (movedAnchors.values.all { it.isEmpty() }) return doc
        val records = doc.changes.records.mapNotNull { r ->
            val pair = pairs.firstOrNull { (from, to) -> r.target.sameAs(from) || r.target.sameAs(to) } ?: return@mapNotNull r
            if (r.target.sameAs(pair.second)) {
                val clash = r.deletedAt == null && movedAnchors[pair.second.targetId].orEmpty().any { it.sameAs(r.anchor) }
                if (clash) null else r
            } else {
                moved += MovedRef("record", r.key, "target.entityKey")
                r.copy(target = pair.second, updatedAt = ctx.now, updatedBy = ctx.deviceId)
            }
        }
        return doc.copy(changes = doc.changes.copy(records = records))
    }

    private fun ProjectDoc.updateFields(item: ItemId, ctx: TrashContext, f: (Map<Key, FieldValue>) -> Map<Key, FieldValue>): ProjectDoc = when (item.kind) {
        ItemKind.CHARACTER -> copy(cast = cast.copy(characters = cast.characters.mapIf({ it.key == item.key }) { it.copy(fields = f(it.fields), updatedAt = ctx.now, updatedBy = ctx.deviceId) }))
        ItemKind.PLACE -> copy(world = world.copy(places = world.places.mapIf({ it.key == item.key }) { it.copy(fields = f(it.fields), updatedAt = ctx.now, updatedBy = ctx.deviceId) }))
        ItemKind.TERM -> copy(world = world.copy(terms = world.terms.mapIf({ it.key == item.key }) { it.copy(fields = f(it.fields), updatedAt = ctx.now, updatedBy = ctx.deviceId) }))
        ItemKind.FACTION -> copy(world = world.copy(factions = world.factions.mapIf({ it.key == item.key }) { it.copy(fields = f(it.fields), updatedAt = ctx.now, updatedBy = ctx.deviceId) }))
        else -> this
    }

    private fun endKindOf(kind: ItemKind): RelationEndKind? = when (kind) {
        ItemKind.CHARACTER -> RelationEndKind.CHARACTER
        ItemKind.FACTION -> RelationEndKind.FACTION
        else -> null
    }

    private fun Relation.isA(ref: EntityRef) = endA.sameAs(ref)

    /**
     * For a slot of the target relation, the corresponding slot of the merged relation (or, with
     * [reverse], the target slot of a merged slot): `사이` ↔ `사이`; the merged item's own side ↔ the
     * target's side; the partner's side ↔ the partner's side.
     */
    private fun mapSlot(
        slot: RelationSlotName,
        merged: Relation,
        mergedRef: EntityRef,
        target: Relation,
        targetRef: EntityRef,
        reverse: Boolean = false,
    ): RelationSlotName? {
        val mergedIsA = merged.isA(mergedRef)
        val targetIsA = target.isA(targetRef)
        val (fromIsA, toIsA) = if (reverse) mergedIsA to targetIsA else targetIsA to mergedIsA
        fun side(isOwnA: Boolean, ownSide: Boolean) = if (ownSide == isOwnA) Side.A else Side.B
        return when (slot) {
            RelationSlotName.BETWEEN -> RelationSlotName.BETWEEN
            RelationSlotName.MIND_A, RelationSlotName.ADDRESS_A, RelationSlotName.MIND_B, RelationSlotName.ADDRESS_B -> {
                val isASlot = slot == RelationSlotName.MIND_A || slot == RelationSlotName.ADDRESS_A
                // Is this slot the item's own side (merged or target) in its relation?
                val ownSide = isASlot == fromIsA
                val newSide = side(toIsA, ownSide)
                val mind = slot == RelationSlotName.MIND_A || slot == RelationSlotName.MIND_B
                when {
                    mind && newSide == Side.A -> RelationSlotName.MIND_A
                    mind -> RelationSlotName.MIND_B
                    newSide == Side.A -> RelationSlotName.ADDRESS_A
                    else -> RelationSlotName.ADDRESS_B
                }
            }
        }
    }

    private enum class Side { A, B }

    private fun Relation.slotValue(slot: RelationSlotName): Any? = when (slot) {
        RelationSlotName.BETWEEN -> between
        RelationSlotName.MIND_A -> mindA
        RelationSlotName.MIND_B -> mindB
        RelationSlotName.ADDRESS_A -> addressA
        RelationSlotName.ADDRESS_B -> addressB
    }

    private fun Relation.withSlot(slot: RelationSlotName, value: Any?): Relation = when (slot) {
        RelationSlotName.BETWEEN -> copy(between = value as SlotValue?)
        RelationSlotName.MIND_A -> copy(mindA = value as SlotValue?)
        RelationSlotName.MIND_B -> copy(mindB = value as SlotValue?)
        RelationSlotName.ADDRESS_A -> copy(addressA = value as String?)
        RelationSlotName.ADDRESS_B -> copy(addressB = value as String?)
    }

    /** A pair kind's holder refers to an end; keep it on the same person after the end moves. */
    private fun remapHolder(value: Any, merged: Relation, mergedRef: EntityRef, target: Relation, targetRef: EntityRef): Any {
        val slot = value as? SlotValue ?: return value
        val holder = slot.pairFirstHolder?.known ?: return value
        val holderIsMerged = (holder == PairHolder.A) == merged.isA(mergedRef)
        val newHolder = if (holderIsMerged == target.isA(targetRef)) PairHolder.A else PairHolder.B
        return slot.copy(pairFirstHolder = newHolder.open())
    }

    private fun slotConflicts(merged: Relation, mergedRef: EntityRef, target: Relation, targetRef: EntityRef): List<SlotConflict> =
        RelationSlotName.entries.mapNotNull { slot ->
            val fromMerged = mapSlot(slot, merged, mergedRef, target, targetRef) ?: return@mapNotNull null
            val m = merged.slotValue(fromMerged)
            val t = target.slotValue(slot)
            if (m != null && t != null && m != t) SlotConflict(slot, m, t) else null
        }

    // --- Snapshots ---

    private fun snapshotOf(doc: ProjectDoc, item: ItemId): JsonElement = when (item.kind) {
        ItemKind.CHARACTER -> CivilightJson.encodeElement(Character.serializer(), doc.index.character(item.key)!!)
        ItemKind.PLACE -> CivilightJson.encodeElement(Place.serializer(), doc.index.place(item.key)!!)
        ItemKind.TERM -> CivilightJson.encodeElement(Term.serializer(), doc.index.term(item.key)!!)
        ItemKind.FACTION -> CivilightJson.encodeElement(Faction.serializer(), doc.index.faction(item.key)!!)
        else -> error("not mergeable: ${item.kind}")
    }

    private fun rebuild(doc: ProjectDoc, item: ItemId, snapshot: JsonElement, ctx: TrashContext): ProjectDoc? = runCatching {
        val at = ctx.now
        val by = ctx.deviceId
        when (item.kind) {
            ItemKind.CHARACTER -> {
                val row = CivilightJson.decodeElement(Character.serializer(), snapshot).copy(deletedAt = null, trashEntryKey = null, updatedAt = at, updatedBy = by)
                doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.mapIf({ it.key == item.key }) { row }))
            }
            ItemKind.PLACE -> {
                val row = CivilightJson.decodeElement(Place.serializer(), snapshot).copy(deletedAt = null, trashEntryKey = null, updatedAt = at, updatedBy = by)
                doc.copy(world = doc.world.copy(places = doc.world.places.mapIf({ it.key == item.key }) { row }))
            }
            ItemKind.TERM -> {
                val row = CivilightJson.decodeElement(Term.serializer(), snapshot).copy(deletedAt = null, trashEntryKey = null, updatedAt = at, updatedBy = by)
                doc.copy(world = doc.world.copy(terms = doc.world.terms.mapIf({ it.key == item.key }) { row }))
            }
            ItemKind.FACTION -> {
                val row = CivilightJson.decodeElement(Faction.serializer(), snapshot).copy(deletedAt = null, trashEntryKey = null, updatedAt = at, updatedBy = by)
                doc.copy(world = doc.world.copy(factions = doc.world.factions.mapIf({ it.key == item.key }) { row }))
            }
            else -> null
        }
    }.getOrNull()

    /** Recorder links of the merged item move to the target (duplicates dropped); apply to the library after [merge]. */
    fun applyToRecorder(library: RecorderLibrary, projectKey: Key, kind: ItemKind, mergedKey: Key, targetKey: Key): RecorderLibrary {
        val targetKind = kind.recorderTargetKind ?: return library
        val existing = library.links.filter { it.projectKey == projectKey && it.targetKey == targetKey && it.targetKind.known == targetKind }
            .map { it.recordKey }.toSet()
        val links = library.links.mapNotNull { link ->
            if (link.projectKey != projectKey || link.targetKey != mergedKey || link.targetKind.known != targetKind) return@mapNotNull link
            if (link.recordKey in existing) null else link.copy(targetKey = targetKey)
        }
        return library.copy(links = links)
    }
}

/** Maps every custom-field value of every entity (and `field` record values). */
internal fun ProjectDoc.mapEntityFields(
    at: Long,
    by: String,
    f: (ownerKind: String, ownerKey: Key, defKey: Key, value: FieldValue) -> FieldValue,
): ProjectDoc {
    fun map(kind: String, key: Key, fields: Map<Key, FieldValue>): Map<Key, FieldValue> {
        var changed = false
        val out = fields.mapValues { (defKey, value) -> f(kind, key, defKey, value).also { if (it != value) changed = true } }
        return if (changed) out else fields
    }
    val records = changes.records.map { r ->
        val t = r.target as? ChangeTarget.Field ?: return@map r
        val value = r.fieldValue() ?: return@map r
        val next = f("record", r.key, t.fieldDefKey, value)
        if (next == value) r else r.copy(value = io.github.lenticualrstone.civilight.model.change.ChangeRecord.encodeValue(FieldValue.serializer(), next), updatedAt = at, updatedBy = by)
    }
    return copy(
        cast = cast.copy(characters = cast.characters.map { c -> map("character", c.key, c.fields).let { if (it !== c.fields) c.copy(fields = it, updatedAt = at, updatedBy = by) else c } }),
        world = world.copy(
            places = world.places.map { p -> map("place", p.key, p.fields).let { if (it !== p.fields) p.copy(fields = it, updatedAt = at, updatedBy = by) else p } },
            terms = world.terms.map { t -> map("term", t.key, t.fields).let { if (it !== t.fields) t.copy(fields = it, updatedAt = at, updatedBy = by) else t } },
            factions = world.factions.map { x -> map("faction", x.key, x.fields).let { if (it !== x.fields) x.copy(fields = it, updatedAt = at, updatedBy = by) else x } },
        ),
        changes = changes.copy(records = records),
    )
}
