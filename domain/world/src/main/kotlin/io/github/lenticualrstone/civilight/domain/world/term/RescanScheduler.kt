package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * When the candidate index is refreshed (WLD-26): a typed block is rescanned alone about 0.5 s
 * after typing stops; a changed term is swept at once with its own patterns; opening or restoring
 * a project runs a full scan in the background, during which the index is pending (`점검 중`).
 *
 * All work runs in [scope]; [current] reads the project state at the time the work runs.
 */
class RescanScheduler(
    private val scope: CoroutineScope,
    private val current: () -> ProjectDoc,
    private val access: () -> TermAccess = { TermAccess() },
    private val options: MatcherOptions = MatcherOptions(),
    private val debounceMs: Long = DEBOUNCE_MS,
    initial: CandidateIndex = CandidateIndex.pending(),
    /** Called after every scan with the new index and what was scanned. */
    private val onScanned: (CandidateIndex, ScanReport) -> Unit = { _, _ -> },
) {
    private val lock = Any()
    private val pending = LinkedHashSet<ScriptSpot>()
    private var typingJob: Job? = null

    @Volatile
    var index: CandidateIndex = initial
        private set

    /** Text of [spot] changed; rescans after [debounceMs] without further input there or elsewhere. */
    fun textChanged(spot: ScriptSpot) {
        synchronized(lock) {
            pending += spot
            typingJob?.cancel()
            typingJob = scope.launch {
                delay(debounceMs)
                flush()
            }
        }
    }

    /** Rescans the pending spots now. */
    fun flush() {
        val spots = synchronized(lock) { pending.toList().also { pending.clear() } }
        if (spots.isEmpty()) return
        val (next, report) = index.rescanSpots(current(), spots, access(), options)
        index = next
        onScanned(next, report)
    }

    /** A term, its aliases or its `글자 찾기` changed: sweep for that term only. */
    fun termChanged(termKey: Key): Job = scope.launch {
        val (next, report) = index.rescanTerm(current(), termKey, access(), options)
        index = next
        onScanned(next, report)
    }

    /** Project opened or restored: mark pending, then scan everything in the background. */
    fun fullScan(): Job {
        index = index.withComplete(false)
        return scope.launch {
            val (next, report) = CandidateIndex.buildWithReport(current(), access(), options)
            index = next
            onScanned(next, report)
        }
    }

    companion object {
        const val DEBOUNCE_MS = 500L
    }
}
