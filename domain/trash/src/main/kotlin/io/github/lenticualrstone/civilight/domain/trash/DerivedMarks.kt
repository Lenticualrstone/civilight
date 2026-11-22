package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagState

/** A place's pin and whether its map is there (TC-20-24: `◇ 지도 없음` · `지도 되살리기`). */
data class PinStatus(val pinKey: Key, val mapKey: Key, val x: Float, val y: Float, val mapLive: Boolean) {
    /** `◇ 지도 없음` when the map is in the trash. */
    val mark: String? get() = if (mapLive) null else TrashCopy.MAP_MISSING

    /** `지도 되살리기 · 핀 다시 놓기` (`[PROPOSED]` button copy, WLD-14). */
    val actions: List<String> get() = if (mapLive) emptyList() else listOf(TrashCopy.RESTORE_MAP, "핀 다시 놓기")
}

/** Marks derived from deleted things (§3, CST-47); nothing here is stored. */
object DerivedMarks {
    /**
     * `대상 지워짐` (§3, TC-20-23, SCR-57): records anchored at [blockKey] that are not live because
     * their target is not live (they have no mark of their own). The block shows them dotted with
     * `되살리기 · 이대로 두기 · 기록 지우기`.
     */
    fun targetDeleted(doc: ProjectDoc, blockKey: Key): List<ChangeRecord> = doc.changes.records.filter { r ->
        r.deletedAt == null && (r.anchor as? Anchor.AtBlock)?.blockKey == blockKey && !doc.liveness.isTargetLive(r.target)
    }

    /** Actions on a `대상 지워짐` mark. */
    val targetDeletedActions: List<String> get() = listOf(TrashCopy.RESTORE, TrashCopy.KEEP, "기록 지우기")

    /** Pins of a place with the state of their maps. */
    fun pinsOf(doc: ProjectDoc, placeKey: Key): List<PinStatus> = doc.maps.values.flatMap { map ->
        map.pins.filter { it.placeKey == placeKey }.map { PinStatus(it.key, map.key, it.x, it.y, doc.liveness.isMapLive(map.key)) }
    }

    /**
     * A map shows as top level when it has no parent or its parent pin is gone (purged with its
     * place, TC-20-25); `WorldMap.parent` itself is never rewritten.
     */
    fun isTopLevelMap(doc: ProjectDoc, mapKey: Key): Boolean {
        val parent = doc.maps[mapKey]?.parent ?: return true
        return doc.index.pin(parent.pinKey) == null
    }

    /**
     * Name tags an export without `휴지통도 포함` writes (§5.6, `ED`): one `notExported` tag for each
     * trashed character · place · term · faction · page · unit that live exported data references,
     * plus every existing `purged` tag.
     */
    fun exportNameTags(doc: ProjectDoc): List<NameTag> {
        val scanner = ReferenceScanner(doc)
        val candidates = buildList {
            doc.cast.characters.filter { it.deletedAt != null }.forEach { add(ItemId.character(it.key)) }
            doc.world.places.filter { it.deletedAt != null }.forEach { add(ItemId.place(it.key)) }
            doc.world.terms.filter { it.deletedAt != null }.forEach { add(ItemId.term(it.key)) }
            doc.world.factions.filter { it.deletedAt != null }.forEach { add(ItemId.faction(it.key)) }
            doc.pages.values.filter { it.deletedAt != null }.forEach { add(ItemId.page(it.key)) }
            doc.story.units.filter { it.deletedAt != null }.forEach { add(ItemId.unit(it.key)) }
        }
        val tags = candidates.filter { scanner.referencesTo(it).isNotEmpty() }.map { item ->
            NameTag(
                key = item.key,
                kind = item.kind.nameTagKind!!.open(),
                name = if (item.kind == ItemKind.UNIT) UnitCodes(doc).codeAndTitle(item.key).orEmpty() else doc.itemName(item).orEmpty(),
                voiceCode = if (item.kind == ItemKind.CHARACTER) doc.index.character(item.key)?.voiceCode else null,
                hidden = doc.itemHidden(item),
                state = NameTagState.NOT_EXPORTED.open(),
            )
        }
        val purged = doc.tombstones.nameTags.filter { it.state.known == NameTagState.PURGED }
        return purged + tags.filter { t -> purged.none { it.key == t.key } }
    }
}
