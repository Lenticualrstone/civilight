package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
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
 * after a project is acquired leaves the index pending (`점검 중`, candidate numbers dimmed);
 * later scans keep the previous index until the new one is ready. Usage and `▲` never wait for it.
 *
 * Batch 1 rescans the whole manuscript when its text changes; the per-block and per-term rescans of
 * WLD-26 (`RescanScheduler`) replace [Holder.run] later without changing callers.
 */
object TermIndexes {
    /** One project's index; [release] when the screen's view model is cleared. */
    class Handle internal constructor(private val holder: Holder, private val onRelease: () -> Unit) {
        private val released = java.util.concurrent.atomic.AtomicBoolean(false)

        val index: StateFlow<CandidateIndex> get() = holder.index.asStateFlow()

        /** The document or access changed; a scan runs unless the text is the same. */
        fun submit(doc: ProjectDoc, access: TermAccess) {
            holder.input.value = ScanInput(doc, access)
        }

        /** Gives the index back; the last release stops scanning and drops it. Idempotent. */
        fun release() {
            if (released.compareAndSet(false, true)) onRelease()
        }
    }

    internal class Holder(scope: CoroutineScope, dispatcher: CoroutineDispatcher) {
        val index = MutableStateFlow(CandidateIndex.pending())
        val input = MutableStateFlow<ScanInput?>(null)
        var users = 0
        val job: Job = scope.launch(dispatcher) {
            input.filterNotNull()
                .distinctUntilChanged { a, b -> a.sameText(b) }
                .conflate()
                .collect { index.value = run(it) }
        }

        private fun run(input: ScanInput): CandidateIndex = CandidateIndex.build(input.doc, input.access)
    }

    private val holders = HashMap<Key, Holder>()

    /** The index of [projectKey]; scans run on [dispatcher] in [scope] (the app scope). */
    fun acquire(projectKey: Key, scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default): Handle {
        val holder = synchronized(holders) {
            holders.getOrPut(projectKey) { Holder(scope, dispatcher) }.also { it.users++ }
        }
        return Handle(holder) {
            synchronized(holders) {
                holder.users--
                if (holder.users <= 0 && holders[projectKey] === holder) {
                    holders.remove(projectKey)
                    holder.job.cancel()
                }
            }
        }
    }
}
