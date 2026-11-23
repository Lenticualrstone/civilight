package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Releasing `아님` records whose candidate is gone (WLD-22 lifetime) without trusting a shared,
 * possibly lagging candidate index: the spots holding rejections are rescanned on the very document
 * that is written, then [TermEdits.releaseStaleRejections] runs over exactly those spots.
 *
 * Meant to run inside the edit that changed the text (same undo step, never on a read-only
 * project): the script's block edit passes the edited spots; the world area folds it into its own
 * term writes. [stale] is the read-only form for lists that must not show released records yet.
 */
object StaleRejections {
    /** Script spots that hold at least one `아님` record of a term [access] may search. */
    fun spotsWithRejections(doc: ProjectDoc, access: TermAccess = TermAccess()): Set<ScriptSpot> =
        doc.world.terms.asSequence()
            .filter { access.canSearch(it) }
            .flatMap { it.rejections.asSequence() }
            .mapNotNull(TermRejections::spotOf)
            .toSet()

    /**
     * [doc] with the stale `아님` records of [spots] (all rejection spots when null) removed. The
     * candidates come from a fresh scan of those spots on [doc], so the result never depends on an
     * index that has not caught up with the text. Returns [doc] itself when nothing is stale.
     */
    fun release(
        doc: ProjectDoc,
        access: TermAccess = TermAccess(),
        spots: Set<ScriptSpot>? = null,
        options: MatcherOptions = MatcherOptions(),
    ): ProjectDoc {
        // (0.2.0, WLD-73) No scan and no release while `용어 후보` is off: the records are kept.
        if (!doc.project.termCandidates) return doc
        val withRejections = spotsWithRejections(doc, access)
        val targets = if (spots == null) withRejections else withRejections intersect spots
        if (targets.isEmpty()) return doc
        val (fresh, _) = CandidateIndex.pending().rescanSpots(doc, targets, access, options)
        return TermEdits.releaseStaleRejections(doc, fresh, access, targets)
    }

    /** Keys of the `아님` records [release] would remove now (for read-only views). */
    fun stale(doc: ProjectDoc, access: TermAccess = TermAccess(), options: MatcherOptions = MatcherOptions()): Set<Key> {
        val released = release(doc, access, null, options)
        if (released === doc) return emptySet()
        val kept = released.world.terms.flatMap { t -> t.rejections.map { it.key } }.toSet()
        return doc.world.terms.flatMap { t -> t.rejections.map { it.key } }.filterNot { it in kept }.toSet()
    }
}
