package io.github.lenticualrstone.civilight.domain.recorder.state

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.target
import io.github.lenticualrstone.civilight.domain.recorder.put.CardDocs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord

/** `정리 안 됨 · 넣음 · 끝냄` (LRC-26): derived, never stored. */
enum class RecordState(val label: String) {
    UNSORTED(RecorderCopy.UNSORTED),
    PUT(RecorderCopy.PUT),
    DONE(RecorderCopy.DONE),
}

/** Where a copy stands (`넣은 곳` row). */
enum class PlacementStatus { ACTIVE, IN_TRASH, GONE }

/** §3.3 derived values of records and placements. */
object RecordStates {
    /**
     * `GONE` when the placement was marked purged, its project is permanently deleted or its
     * target row no longer exists; `IN_TRASH` when the target (or its container, or the project) is
     * not live; else `ACTIVE`. Without [env] only the stored mark counts.
     */
    fun placementStatus(placement: Placement, env: RecorderEnv?): PlacementStatus {
        if (placement.purgedAt != null) return PlacementStatus.GONE
        if (env == null) return PlacementStatus.ACTIVE
        val project = env.project(placement.projectKey) ?: return PlacementStatus.GONE
        val doc = project.doc
        val live: Boolean = when (placement.kind.known) {
            PlacementKind.SCRIPT_BLOCK -> {
                val block = doc.index.block(placement.targetKey) ?: return PlacementStatus.GONE
                doc.liveness.isLive(block)
            }
            PlacementKind.CARD_PARAGRAPH -> {
                val entityKey = placement.containerKey ?: return PlacementStatus.GONE
                val kind = CardDocs.kindOf(doc, entityKey) ?: return PlacementStatus.GONE
                if (!CardDocs.holdsParagraph(doc, entityKey, placement.targetKey)) return PlacementStatus.GONE
                CardDocs.isLive(doc, kind, entityKey)
            }
            PlacementKind.PAGE -> {
                if (doc.pages[placement.targetKey] == null) return PlacementStatus.GONE
                doc.liveness.isPageLive(placement.targetKey)
            }
            PlacementKind.MEMO_FIELD -> {
                if (doc.index.unit(placement.targetKey) == null) return PlacementStatus.GONE
                doc.liveness.isUnitLive(placement.targetKey)
            }
            PlacementKind.CHARACTER -> {
                if (doc.index.character(placement.targetKey) == null) return PlacementStatus.GONE
                doc.liveness.isCharacterLive(placement.targetKey)
            }
            null -> true
        }
        return if (live && !project.trashed) PlacementStatus.ACTIVE else PlacementStatus.IN_TRASH
    }

    /** `넣음` if any placement is not gone; else `끝냄` if marked; else `정리 안 됨`. */
    fun state(record: RecorderRecord, placements: List<Placement>, env: RecorderEnv?): RecordState = when {
        placements.any { placementStatus(it, env) != PlacementStatus.GONE } -> RecordState.PUT
        record.doneMarked -> RecordState.DONE
        else -> RecordState.UNSORTED
    }

    fun state(library: RecorderLibrary, record: RecorderRecord, env: RecorderEnv?): RecordState =
        state(record, library.placementsOf(record.key), env)

    /** `◇ 넣은 곳 지워짐`: there were placements and every one is gone. */
    fun placementsAllGone(placements: List<Placement>, env: RecorderEnv?): Boolean =
        placements.isNotEmpty() && placements.all { placementStatus(it, env) == PlacementStatus.GONE }

    /** `◇ 넣은 뒤 고침`: the record text changed after this copy was made. */
    fun editedAfter(record: RecorderRecord, placement: Placement): Boolean = record.textRevision > placement.copiedRevision

    /** Projects the record is linked to, explicitly or through a sub-link, that still exist. */
    fun impliedProjects(library: RecorderLibrary, recordKey: Key, env: RecorderEnv): Set<Key> =
        library.linksOf(recordKey).map { it.projectKey }.filter { env.project(it) != null }.toSet()

    /** True when the record links [target] exactly (any origin). */
    fun linksTo(library: RecorderLibrary, recordKey: Key, target: TargetRef): Boolean =
        library.linksOf(recordKey).any { it.target == target }
}
