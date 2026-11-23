package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.RescanScheduler
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** What a candidate scan reads: the document and who may see hidden terms. */
data class ScanInput(val doc: ProjectDoc, val access: TermAccess) {
    /**
     * True when [other] would scan to the same index: the same manuscript, units, terms and
     * particle list (compared by reference: an edit elsewhere, the lens or `_app/` state keeps them).
     */
    fun sameText(other: ScanInput): Boolean = access == other.access &&
        doc.episodes === other.doc.episodes &&
        doc.story === other.doc.story &&
        doc.world.terms === other.doc.world.terms &&
        doc.project.termMatching === other.doc.project.termMatching
}

/**
 * The candidate index of each open project (WLD-26 `색인`), shared by every world screen of that
 * project: list and card in two panes read one index. It is derived and app-only. The first scan
 * after a project is acquired leaves the index pending (`점검 중`, candidate numbers dimmed); later
 * scans keep the previous index until the new one is ready. Usage and `▲` never wait for it.
 *
 * Each new document is diffed against the last one ([RescanPlans]) and handed to the domain's
 * [RescanScheduler]: a typed block is rescanned alone about 0.5 s after typing stops, a changed term
 * is swept at once with its own patterns, and a change of access or particles runs a full scan. All
 * scans of one project run on one thread, so no scan result is lost.
 *
 * Stale `아님` records are never written from here (that would add undo steps and touch read-only
 * projects): readers hide them with `StaleRejections.stale`, and the world area's own term writes
 * release them in the same step (`StaleRejections.release`).
 */
object TermIndexes {
    /** One project's index; [release] when the screen's view model is cleared. */
    class Handle internal constructor(private val holder: Holder, private val onRelease: () -> Unit) {
        private val released = java.util.concurrent.atomic.AtomicBoolean(false)

        val index: StateFlow<CandidateIndex> get() = holder.index.asStateFlow()

        /** The document or access changed; scans run for what changed. */
        fun submit(doc: ProjectDoc, access: TermAccess) {
            holder.input.value = ScanInput(doc, access)
        }

        /** Gives the index back; the last release stops scanning and drops it. Idempotent. */
        fun release() {
            if (released.compareAndSet(false, true)) onRelease()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal class Holder(scope: CoroutineScope, dispatcher: CoroutineDispatcher, debounceMs: Long) {
        val index = MutableStateFlow(CandidateIndex.pending())
        val input = MutableStateFlow<ScanInput?>(null)
        var users = 0

        @Volatile
        private var latest: ScanInput? = null
        private val worker = dispatcher.limitedParallelism(1)
        private val work = SupervisorJob(scope.coroutineContext[Job])
        private val workScope = CoroutineScope(scope.coroutineContext + worker + work)
        private val scheduler = RescanScheduler(
            scope = workScope,
            current = { latest!!.doc },
            access = { latest!!.access },
            debounceMs = debounceMs,
            onScanned = { next, _ -> index.value = next },
        )

        val job: Job = workScope.launch {
            var previous: ScanInput? = null
            input.filterNotNull().collect { next ->
                latest = next
                // (0.2.0, WLD-26, WLD-73) While `용어 후보` is off the index is neither built nor updated; turning
                // it on again runs the "project opened" full scan (previous = null → full, pending first).
                // The index then reads empty and complete, so no reader waits on a scan that never runs
                // (`점검 중` in the check center, domain:checks `TermSource.scanning`).
                if (!next.doc.project.termCandidates) {
                    previous = null
                    index.value = OFF_INDEX
                    return@collect
                }
                when (val plan = RescanPlans.of(previous, next)) {
                    RescanPlan.Full -> {
                        scheduler.fullScan()
                        // Pending only before the first scan; later full scans keep showing the old numbers.
                        if (previous == null) index.value = scheduler.index
                    }
                    is RescanPlan.Incremental -> {
                        plan.terms.forEach(scheduler::termChanged)
                        plan.spots.forEach(scheduler::textChanged)
                    }
                    RescanPlan.None -> Unit
                }
                previous = next
            }
        }

        fun cancel() {
            work.cancel()
        }
    }

    /** (0.2.0, WLD-73) The index while `용어 후보` is off: no candidates, nothing pending. */
    val OFF_INDEX: CandidateIndex = CandidateIndex.pending().withComplete(true)

    private val holders = HashMap<Key, Holder>()

    /** The index of [projectKey]; scans run on [dispatcher] in [scope] (the app scope). */
    fun acquire(
        projectKey: Key,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        debounceMs: Long = RescanScheduler.DEBOUNCE_MS,
    ): Handle {
        val holder = synchronized(holders) {
            holders.getOrPut(projectKey) { Holder(scope, dispatcher, debounceMs) }.also { it.users++ }
        }
        return Handle(holder) {
            synchronized(holders) {
                holder.users--
                if (holder.users <= 0 && holders[projectKey] === holder) {
                    holders.remove(projectKey)
                    holder.cancel()
                }
            }
        }
    }
}
