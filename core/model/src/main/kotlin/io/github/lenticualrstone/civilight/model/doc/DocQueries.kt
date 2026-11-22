package io.github.lenticualrstone.civilight.model.doc

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.position.history
import io.github.lenticualrstone.civilight.model.position.isBeforeReveal
import io.github.lenticualrstone.civilight.model.position.recordAt
import io.github.lenticualrstone.civilight.model.position.recordBeforeBlock
import io.github.lenticualrstone.civilight.model.position.recordsHere
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.story.StoryUnit

// Convenience queries over a whole ProjectDoc, applying its liveness (§5.1) to records.

private val ProjectDoc.liveRecord: (ChangeRecord) -> Boolean get() = { liveness.isLive(it) }

/** `valueAt(target, P)` (§4.5): the winning live record at the lens, or null for the base value. */
fun ProjectDoc.recordAt(target: ChangeTarget, lens: LensPoint): ChangeRecord? =
    manuscript.recordAt(changes.records, target, lens, liveRecord)

/** Script panel: the live record in force just before [blockKey], or null for the base value. */
fun ProjectDoc.recordBeforeBlock(target: ChangeTarget, blockKey: Key): ChangeRecord? =
    manuscript.recordBeforeBlock(changes.records, target, blockKey, liveRecord)

/** Script panel `여기서` rows of a block. */
fun ProjectDoc.recordsHere(blockKey: Key): List<ChangeRecord> =
    manuscript.recordsHere(changes.records, blockKey, liveRecord)

/** Live records of a target in position order. */
fun ProjectDoc.history(target: ChangeTarget): List<ChangeRecord> =
    manuscript.history(changes.records, target, liveRecord)

/** True when the live record is `예정` (after the 집필 선). */
fun ProjectDoc.isPlanned(record: ChangeRecord): Boolean = manuscript.isPlanned(record.anchor)

/** `▲ 공개 전 사용` for a link to [termKey] inside [blockKey] (or its choice options). */
fun ProjectDoc.isLinkBeforeReveal(termKey: Key, blockKey: Key): Boolean {
    val rank = manuscript.blockRankOf(blockKey) ?: return false
    return manuscript.isBeforeReveal(changes.records, termKey, rank, liveRecord)
}

/** `▲ 공개 전 사용` for a link to [termKey] in the transition text of [sceneKey]. */
fun ProjectDoc.isTransitionLinkBeforeReveal(termKey: Key, sceneKey: Key): Boolean {
    val rank = manuscript.transitionRankOf(sceneKey) ?: return false
    return manuscript.isBeforeReveal(changes.records, termKey, rank, liveRecord)
}

/** Rank of a position in this project (null when its referent is unknown). */
fun ProjectDoc.rankOf(position: Position): Rank? = manuscript.rankOf(position)

/** Live scenes in manuscript order (§4.2; hidden scenes included). */
val ProjectDoc.liveScenes: List<StoryUnit> get() = manuscript.liveScenes

/** Live blocks of a scene by order. */
fun ProjectDoc.liveBlocksOf(sceneKey: Key): List<Block> = manuscript.liveBlocksOf(sceneKey)

/** Live children of a unit (null = top level) by order. */
fun ProjectDoc.liveChildren(parentKey: Key?): List<StoryUnit> = index.children(parentKey).filter { liveness.isLive(it) }

/** Live characters by order. */
val ProjectDoc.liveCharacters: List<Character>
    get() = cast.characters.filter { it.deletedAt == null }.sortedWith(compareBy<Character> { it.order }.thenBy { it.key })
