package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.TrashPlacement
import io.github.lenticualrstone.civilight.model.text.plainText

// Direct-row access by ItemId: existence, mark, liveness, name and hidden flag, and setting or
// clearing the §5.1 mark on exactly that row.

/** The directly trashable row of [item], or null when it is not in the project. */
internal fun ProjectDoc.rowOf(item: ItemId): Trashable? = when (item.kind) {
    ItemKind.CHARACTER -> index.character(item.key)
    ItemKind.PLACE -> index.place(item.key)
    ItemKind.TERM -> index.term(item.key)
    ItemKind.FACTION -> index.faction(item.key)
    ItemKind.FIELD_DEF -> index.fieldDef(item.key)
    ItemKind.CHANGE -> index.record(item.key)
    ItemKind.MAP -> maps[item.key]
    ItemKind.EVENT -> index.event(item.key)
    ItemKind.PAGE -> pages[item.key]
    ItemKind.CANVAS -> canvases[item.key]
    ItemKind.UNIT -> index.unit(item.key)
    ItemKind.BLOCK -> index.block(item.key)
}

internal fun ProjectDoc.exists(item: ItemId): Boolean = rowOf(item) != null

/** Derived liveness of [item] (§5.1). */
internal fun ProjectDoc.isItemLive(item: ItemId): Boolean = when (item.kind) {
    ItemKind.CHARACTER -> liveness.isCharacterLive(item.key)
    ItemKind.PLACE -> liveness.isPlaceLive(item.key)
    ItemKind.TERM -> liveness.isTermLive(item.key)
    ItemKind.FACTION -> liveness.isFactionLive(item.key)
    ItemKind.FIELD_DEF -> liveness.isFieldDefLive(item.key)
    ItemKind.CHANGE -> index.record(item.key)?.let(liveness::isLive) ?: false
    ItemKind.MAP -> liveness.isMapLive(item.key)
    ItemKind.EVENT -> liveness.isEventLive(item.key)
    ItemKind.PAGE -> liveness.isPageLive(item.key)
    ItemKind.CANVAS -> liveness.isCanvasLive(item.key)
    ItemKind.UNIT -> liveness.isUnitLive(item.key)
    ItemKind.BLOCK -> liveness.isBlockLive(item.key)
}

/** The name an item is shown by (its row's name or title); null for a change record. */
internal fun ProjectDoc.itemName(item: ItemId): String? = when (item.kind) {
    ItemKind.CHARACTER -> index.character(item.key)?.name
    ItemKind.PLACE -> index.place(item.key)?.name
    ItemKind.TERM -> index.term(item.key)?.name
    ItemKind.FACTION -> index.faction(item.key)?.name
    ItemKind.FIELD_DEF -> index.fieldDef(item.key)?.name
    ItemKind.CHANGE -> null
    ItemKind.MAP -> maps[item.key]?.name
    ItemKind.EVENT -> index.event(item.key)?.title
    ItemKind.PAGE -> pages[item.key]?.name
    ItemKind.CANVAS -> canvases[item.key]?.name
    ItemKind.UNIT -> index.unit(item.key)?.title
    ItemKind.BLOCK -> index.block(item.key)?.firstLine()
}

/** Rule-19 hiding of the item itself (a block follows its scene). */
internal fun ProjectDoc.itemHidden(item: ItemId): Boolean = when (item.kind) {
    ItemKind.CHARACTER -> index.character(item.key)?.hidden ?: false
    ItemKind.PLACE -> index.place(item.key)?.hidden ?: false
    ItemKind.TERM -> index.term(item.key)?.hidden ?: false
    ItemKind.FACTION -> index.faction(item.key)?.hidden ?: false
    ItemKind.PAGE -> pages[item.key]?.hidden ?: false
    ItemKind.UNIT -> index.unit(item.key)?.hidden ?: false
    ItemKind.BLOCK -> index.block(item.key)?.let { index.unit(it.sceneKey)?.hidden } ?: false
    else -> false
}

/** First line of a block's text (trash block-group lines). */
internal fun Block.firstLine(): String {
    val text = when {
        runs.isNotEmpty() -> runs.plainText
        options.isNotEmpty() -> options.sortedBy { it.order }.joinToString(" / ") { it.runs.plainText }
        else -> ""
    }
    return text.lineSequence().firstOrNull()?.trim().orEmpty()
}

/**
 * Sets (or with nulls clears) the direct mark of [item]. Touches only that row; bookkeeping is
 * stamped with [at] and [by].
 */
internal fun ProjectDoc.setMark(item: ItemId, deletedAt: Timestamp?, entryKey: Key?, at: Timestamp, by: String): ProjectDoc {
    val key = item.key
    return when (item.kind) {
        ItemKind.CHARACTER -> copy(
            cast = cast.copy(
                characters = cast.characters.mapIf({ it.key == key }) {
                    it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)
                },
            ),
        )
        ItemKind.PLACE -> copy(
            world = world.copy(
                places = world.places.mapIf({ it.key == key }) {
                    it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)
                },
            ),
        )
        ItemKind.TERM -> copy(
            world = world.copy(
                terms = world.terms.mapIf({ it.key == key }) {
                    it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)
                },
            ),
        )
        ItemKind.FACTION -> copy(
            world = world.copy(
                factions = world.factions.mapIf({ it.key == key }) {
                    it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)
                },
            ),
        )
        ItemKind.EVENT -> copy(
            world = world.copy(
                events = world.events.mapIf({ it.key == key }) {
                    it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)
                },
            ),
        )
        ItemKind.FIELD_DEF -> copy(
            project = project.copy(
                fieldDefs = project.fieldDefs.mapIf({ it.key == key }) {
                    it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)
                },
            ),
        )
        ItemKind.CHANGE -> copy(
            changes = changes.copy(
                records = changes.records.mapIf({ it.key == key }) {
                    it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)
                },
            ),
        )
        ItemKind.MAP -> maps[key]?.let {
            copy(maps = maps + (key to it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)))
        } ?: this
        ItemKind.PAGE -> pages[key]?.let {
            copy(pages = pages + (key to it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)))
        } ?: this
        ItemKind.CANVAS -> canvases[key]?.let {
            copy(canvases = canvases + (key to it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)))
        } ?: this
        ItemKind.UNIT -> copy(
            story = story.copy(
                units = story.units.mapIf({ it.key == key }) {
                    it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, updatedAt = at, updatedBy = by)
                },
            ),
        )
        ItemKind.BLOCK -> updateBlock(key) {
            it.copy(
                deletedAt = deletedAt, trashEntryKey = entryKey,
                trashPlacement = if (deletedAt == null) null else it.trashPlacement,
                updatedAt = at, updatedBy = by,
            )
        }
    }
}

/** Replaces one block wherever it is filed. */
internal fun ProjectDoc.updateBlock(key: Key, transform: (Block) -> Block): ProjectDoc {
    val group = index.episodeGroupOfBlock(key) ?: return this
    val file = episodes.getValue(group)
    return copy(episodes = episodes + (group to file.copy(blocks = file.blocks.mapIf({ it.key == key }, transform))))
}

/** Marks a block as trashed with its restore position (SCR-39). */
internal fun ProjectDoc.markBlock(key: Key, deletedAt: Timestamp, entryKey: Key, placement: TrashPlacement, by: String): ProjectDoc =
    updateBlock(key) {
        it.copy(deletedAt = deletedAt, trashEntryKey = entryKey, trashPlacement = placement, updatedAt = deletedAt, updatedBy = by)
    }

internal inline fun <T> List<T>.mapIf(predicate: (T) -> Boolean, transform: (T) -> T): List<T> =
    map { if (predicate(it)) transform(it) else it }
