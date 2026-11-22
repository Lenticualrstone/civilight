package io.github.lenticualrstone.civilight.domain.cast.timed

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.doc.recordBeforeBlock
import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.position.recordAt

/**
 * Where a timed value is read: at a lens (profile, lists, map), just before a block (script panel,
 * rule 11 §2), or at a world event (timeline ages).
 */
sealed interface ReadPoint {
    data class AtLens(val lens: LensPoint) : ReadPoint

    /** State in force just before the block; that block's own records are its `여기서` rows. */
    data class BeforeBlock(val blockKey: Key) : ReadPoint

    data class AtEvent(val eventKey: Key) : ReadPoint

    companion object {
        val LATEST: ReadPoint = AtLens(LensPoint.Latest)
    }
}

/** The live record of [target] in force at [point], or null for the base value. */
fun ProjectDoc.recordAt(target: ChangeTarget, point: ReadPoint): ChangeRecord? = when (point) {
    is ReadPoint.AtLens -> recordAt(target, point.lens)
    is ReadPoint.BeforeBlock -> recordBeforeBlock(target, point.blockKey)
    is ReadPoint.AtEvent -> {
        val rank = manuscript.rankOf(Position.Event(point.eventKey)) ?: Rank(Long.MIN_VALUE)
        manuscript.recordAt(changes.records, target, rank) { liveness.isLive(it) }
    }
}

/** Live records of [target] anchored exactly at [blockKey] (`여기서`). */
fun ProjectDoc.recordsHere(target: ChangeTarget, blockKey: Key): List<ChangeRecord> =
    recordsHere(blockKey).filter { it.target.sameAs(target) }
