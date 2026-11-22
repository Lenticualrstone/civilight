package io.github.lenticualrstone.civilight.domain.recorder.hooks

import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.pointsAt
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementStatus
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord

/**
 * `레코더 · 정리 안 됨 n ›` (LRC-24) from inside one open project, for the screens that show it (the
 * script's scene memo card, the character profile `개요` header): they hold [doc] and the recorder
 * library, not a [RecorderEnv] of the whole 서재.
 *
 * [RecorderHooks.unsortedLinkedCount] reads a project missing from its env as permanently deleted,
 * so an env of [doc] alone would count a record whose copy lives in another project as `정리 안 됨`.
 * Here a placement in another project counts as still there unless it is marked purged (its
 * project's trash state is unknown from here); placements in [doc] are judged against [doc].
 */
object TargetLines {
    /** Records `정리 안 됨`, not in the trash, linked to exactly [target]; locked hidden records ([recordsOpen] false) not counted. */
    fun unsortedLinkedCount(library: RecorderLibrary, doc: ProjectDoc, target: TargetRef, recordsOpen: Boolean): Int {
        val env = RecorderEnv(
            projects = listOf(ProjectSnapshot.of(doc)),
            unlock = UnlockSession(recordsUnlockedAt = if (recordsOpen) 0L else null),
        )
        return library.records.count { record ->
            record.deletedAt == null &&
                (!record.hidden || recordsOpen) &&
                library.linksOf(record.key).any { it.pointsAt(target) } &&
                state(library, record, env, doc) == RecordState.UNSORTED
        }
    }

    /** The line, or null when nothing is to be shown (count 0). Tap → `RecorderInbox(p, kind, key)` (target + `정리 안 됨`). */
    fun line(library: RecorderLibrary, doc: ProjectDoc, target: TargetRef, recordsOpen: Boolean): String? =
        RecorderHooks.targetLine(unsortedLinkedCount(library, doc, target, recordsOpen))

    private fun state(library: RecorderLibrary, record: RecorderRecord, env: RecorderEnv, doc: ProjectDoc): RecordState {
        val placed = library.placementsOf(record.key).any { p ->
            if (p.projectKey == doc.key) RecordStates.placementStatus(p, env) != PlacementStatus.GONE else p.purgedAt == null
        }
        return when {
            placed -> RecordState.PUT
            record.doneMarked -> RecordState.DONE
            else -> RecordState.UNSORTED
        }
    }
}
