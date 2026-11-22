package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.trash.TrashEntry

// Small structural edits of a ProjectDoc shared by the operations of this module.

/** Applies [transform] to every block of every numbering unit. */
internal fun ProjectDoc.mapBlocks(transform: (Block) -> Block): ProjectDoc =
    copy(episodes = episodes.mapValues { (_, file) -> file.copy(blocks = file.blocks.map(transform)) })

/** Replaces blocks by key (blocks not in [byKey] are kept). */
internal fun ProjectDoc.replaceBlocks(byKey: Map<Key, Block>): ProjectDoc =
    if (byKey.isEmpty()) this else mapBlocks { byKey[it.key] ?: it }

internal fun ProjectDoc.replaceBlock(block: Block): ProjectDoc = replaceBlocks(mapOf(block.key to block))

/** Removes blocks entirely (merge, empty-block delete). */
internal fun ProjectDoc.removeBlocks(keys: Set<Key>): ProjectDoc =
    if (keys.isEmpty()) this
    else copy(episodes = episodes.mapValues { (_, file) -> file.copy(blocks = file.blocks.filterNot { it.key in keys }) })

/** Adds blocks to the numbering unit file of their scene (created when missing). */
internal fun ProjectDoc.addBlocks(blocks: List<Block>): ProjectDoc {
    if (blocks.isEmpty()) return this
    val out = LinkedHashMap(episodes)
    for (block in blocks) {
        val unitKey = index.numberingUnitKeyOfScene(block.sceneKey) ?: project.key
        val file = out[unitKey] ?: EpisodeFile(unitKey = unitKey)
        out[unitKey] = file.copy(blocks = file.blocks + block)
    }
    return copy(episodes = out)
}

/** Applies [transform] to the unit with [key]. */
internal fun ProjectDoc.updateUnit(key: Key, transform: (StoryUnit) -> StoryUnit): ProjectDoc =
    copy(story = story.copy(units = story.units.map { if (it.key == key) transform(it) else it }))

internal fun ProjectDoc.updateUnits(byKey: Map<Key, StoryUnit>): ProjectDoc =
    if (byKey.isEmpty()) this else copy(story = story.copy(units = story.units.map { byKey[it.key] ?: it }))

internal fun ProjectDoc.mapRecords(transform: (ChangeRecord) -> ChangeRecord): ProjectDoc =
    copy(changes = changes.copy(records = changes.records.map(transform)))

internal fun ProjectDoc.addDiscarded(numberingUnitKey: Key, ids: List<DiscardedVoiceId>): ProjectDoc {
    if (ids.isEmpty()) return this
    val file = episodes[numberingUnitKey] ?: EpisodeFile(unitKey = numberingUnitKey)
    return copy(episodes = episodes + (numberingUnitKey to file.copy(discardedVoiceIds = file.discardedVoiceIds + ids)))
}

internal fun ProjectDoc.removeDiscarded(predicate: (DiscardedVoiceId) -> Boolean): ProjectDoc = copy(
    episodes = episodes.mapValues { (_, f) -> f.copy(discardedVoiceIds = f.discardedVoiceIds.filterNot(predicate)) },
    story = story.copy(discardedVoiceIds = story.discardedVoiceIds.filterNot(predicate)),
)

internal fun ProjectDoc.upsertTrashEntries(entries: List<TrashEntry>): ProjectDoc {
    if (entries.isEmpty()) return this
    val byKey = entries.associateBy { it.key }
    val kept = trash.entries.map { byKey[it.key] ?: it }
    val added = entries.filter { e -> trash.entries.none { it.key == e.key } }
    return copy(trash = trash.copy(entries = kept + added))
}

/** Removes [key] from every trash entry; entries left without targets are deleted. */
internal fun ProjectDoc.removeFromTrash(key: Key): ProjectDoc = copy(
    trash = trash.copy(
        entries = trash.entries.mapNotNull { e ->
            if (key !in e.targetKeys) e else e.copy(targetKeys = e.targetKeys - key).takeIf { it.targetKeys.isNotEmpty() }
        },
    ),
)

/** The numbering unit (§4.1) of a block's scene, or the project key. */
internal fun ProjectDoc.numberingUnitOf(block: Block): Key =
    index.numberingUnitKeyOfScene(block.sceneKey) ?: project.key

internal fun ProjectDoc.numberingUnitOfScene(sceneKey: Key): Key =
    index.numberingUnitKeyOfScene(sceneKey) ?: project.key

/** Records anchored on a block (not directly trashed), live target or not. */
internal fun ProjectDoc.recordsAnchoredOn(blockKey: Key): List<ChangeRecord> =
    changes.records.filter { it.deletedAt == null && (it.anchor as? Anchor.AtBlock)?.blockKey == blockKey }

internal val ChangeRecord.isReveal: Boolean get() = target is ChangeTarget.TermReveal

/**
 * An order key right after [prevKey] in [sceneKey] (null = at the scene start), strictly before
 * the next block of the scene in full order (live or trashed), so siblings never share a key.
 * Returns [count] increasing keys.
 */
internal fun ProjectDoc.orderKeysAfter(sceneKey: Key, prevKey: Key?, count: Int, excluding: Set<Key> = emptySet()): List<OrderKey> {
    val all = index.blocksOfScene(sceneKey).filterNot { it.key in excluding }
    val prev = prevKey?.let { k -> all.firstOrNull { it.key == k } }
    val next = if (prev == null) all.firstOrNull() else all.getOrNull(all.indexOf(prev) + 1)
    return OrderKeys.between(prev?.order, next?.order, count)
}

/** Order keys before [nextKey] in [sceneKey], after whatever block precedes it in full order. */
internal fun ProjectDoc.orderKeysBefore(sceneKey: Key, nextKey: Key, count: Int, excluding: Set<Key> = emptySet()): List<OrderKey> {
    val all = index.blocksOfScene(sceneKey).filterNot { it.key in excluding }
    val i = all.indexOfFirst { it.key == nextKey }
    val prev = if (i > 0) all[i - 1] else null
    return OrderKeys.between(prev?.order, all.getOrNull(i)?.order, count)
}

/** Order keys at the end of [sceneKey]. */
internal fun ProjectDoc.orderKeysAtEnd(sceneKey: Key, count: Int, excluding: Set<Key> = emptySet()): List<OrderKey> {
    val last = index.blocksOfScene(sceneKey).filterNot { it.key in excluding }.lastOrNull()
    return OrderKeys.between(last?.order, null, count)
}
