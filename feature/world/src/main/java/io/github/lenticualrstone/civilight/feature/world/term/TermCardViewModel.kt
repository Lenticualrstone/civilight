package io.github.lenticualrstone.civilight.feature.world.term

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.StaleRejections
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.ItemEdits
import io.github.lenticualrstone.civilight.feature.world.common.TermIndexes
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.ItemRef
import io.github.lenticualrstone.civilight.ui.project.EditResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What happened after a tap that edits; the screen shows the toast or the reason. */
sealed interface EditOutcome {
    /** Applied; [toast] null = no toast. */
    data class Done(val toast: String?) : EditOutcome

    /** Refused by a rule (`선택지 보기 줄에는 첫 공개를 걸 수 없음`). */
    data class Refused(val reason: String) : EditOutcome

    /** The project is read-only (the example). */
    data object ReadOnly : EditOutcome
}

/** The term card (WLD-08, WLD-18, WLD-19). */
@OptIn(ExperimentalCoroutinesApi::class)
class TermCardViewModel(projectKey: Key, val termKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    private val indexHandle = TermIndexes.acquire(projectKey, env.appScope)

    /** `후보 N 새로 찾음` after the last alias change on this card (WLD-16); cleared when the card closes. */
    private val aliasFound = MutableStateFlow<Int?>(null)

    val ui: StateFlow<TermCardUi?> = combine(doc.filterNotNull(), indexHandle.index, hiddenOpen, readOnly, aliasFound) { d, i, h, r, f -> Inputs(d, i, h, r, f) }
        .mapLatest { TermCardState.build(it.doc, projectKey, termKey, it.index, it.hiddenOpen, it.readOnly, it.aliasFound) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private data class Inputs(val doc: ProjectDoc, val index: CandidateIndex, val hiddenOpen: Boolean, val readOnly: Boolean, val aliasFound: Int?)

    init {
        viewModelScope.launch {
            combine(doc.filterNotNull(), hiddenOpen) { d, h -> d to h }.collect { (d, h) -> indexHandle.submit(d, TermAccess(h)) }
        }
    }

    /**
     * A write on this term's data: stale `아님` records (their matched text is gone, WLD-22) are
     * released in the same undo step, from a fresh scan of their blocks. A refused write (the
     * transform returns its input) releases nothing.
     */
    private suspend fun termWrite(label: String, transform: (ProjectDoc) -> ProjectDoc): EditResult {
        val access = TermAccess(hiddenOpen.value)
        return apply(label) { d ->
            val next = transform(d)
            if (next === d) d else StaleRejections.release(next, access)
        }
    }

    /** `공개 시점 옮기기` rows, the chip `1-2 씬 1을 첫 공개로`: applies at once (WLD-18). */
    suspend fun setReveal(target: RevealTarget): EditOutcome {
        var outcome: EditOutcome = EditOutcome.Done(null)
        val ctx = editContext()
        val result = termWrite(WorldCopy.REVEAL_TITLE) { d ->
            when (val r = RevealOptions.apply(d, termKey, target, ctx)) {
                is RevealApply.Refused -> d.also { outcome = EditOutcome.Refused(r.reason) }
                is RevealApply.Applied -> r.doc.also { outcome = EditOutcome.Done(r.toast) }
            }
        }
        return if (result == EditResult.READ_ONLY) EditOutcome.ReadOnly else outcome
    }

    /** `글자 찾기 꺼짐 · 켜기` and the chip `글자 찾기 끄기` (WLD-25). */
    fun setTextSearch(on: Boolean) {
        val ctx = editContext()
        viewModelScope.launch { termWrite(WorldCopy.UNDO_TEXT_SEARCH) { d -> TermEdits.setTextSearch(d, termKey, on, ctx) } }
    }

    /**
     * `잇기` on a card candidate (WLD-22): wraps exactly the matched span in a new link to the term.
     * False when the candidate is gone (the text changed meanwhile) or the project is read-only.
     */
    suspend fun linkCandidate(candidate: TermCandidate): Boolean {
        var linked = false
        val ctx = editContext()
        val result = termWrite(WorldCopy.BTN_LINK) { d -> TermEdits.link(d, candidate, ctx)?.doc?.also { linked = true } ?: d }
        return result == EditResult.APPLIED && linked
    }

    /** `아님` on a card candidate (WLD-22): stores the rejection; the candidate disappears everywhere. */
    suspend fun rejectCandidate(candidate: TermCandidate): Boolean {
        val ctx = editContext()
        return termWrite(WorldCopy.BTN_REJECT) { d -> TermEdits.reject(d, candidate, ctx) } == EditResult.APPLIED
    }

    /**
     * Adds or removes an alias (WLD-16): one undo step; the count of new candidates comes from a
     * rescan of this term against the index the card shows now. Returns the refusal line, or null.
     */
    suspend fun changeAliases(change: (ProjectDoc, CandidateIndex, TermAccess, io.github.lenticualrstone.civilight.domain.world.common.EditContext) -> AliasChange): String? {
        var outcome: AliasChange? = null
        val ctx = editContext()
        val index = indexHandle.index.value
        val access = TermAccess(hiddenOpen.value)
        val result = termWrite(WorldCopy.ROW_ALIASES) { d ->
            when (val c = change(d, index, access, ctx)) {
                is AliasChange.Saved -> c.doc.also { outcome = c }
                is AliasChange.Refused -> d.also { outcome = c }
            }
        }
        return when (val o = outcome) {
            is AliasChange.Refused -> o.message
            is AliasChange.Saved -> {
                if (result == EditResult.APPLIED) aliasFound.value = o.newlyFound
                null
            }
            null -> null
        }
    }

    suspend fun addAlias(alias: String): String? = changeAliases { d, i, a, c -> AliasEditor.add(d, termKey, alias, i, a, c) }

    suspend fun removeAlias(alias: String): String? = changeAliases { d, i, a, c -> AliasEditor.remove(d, termKey, alias, i, a, c) }

    /** `아님으로 둔 것 › 되돌리기` (WLD-22). */
    fun restoreRejection(rejectionKey: io.github.lenticualrstone.civilight.model.Key) {
        val ctx = editContext()
        viewModelScope.launch { termWrite(WorldCopy.BTN_UNDO) { d -> RejectionRows.restore(d, termKey, rejectionKey, ctx) } }
    }

    /**
     * `맞음` on a card `▲` row (WLD-24, Q-07): the author keeps this pre-reveal use; the link leaves
     * every `▲` count and the row keeps a small `맞음` tag. False when the link is gone or no longer
     * before the reveal, or the project is read-only.
     */
    suspend fun markIntended(linkKey: Key): Boolean {
        var marked = false
        val ctx = editContext()
        val access = TermAccess(hiddenOpen.value)
        val result = apply(WorldCopy.TAG_INTENDED) { d ->
            val queries = TermQueries(d, CandidateIndex.pending(), access)
            val link = queries.usage(termKey).firstOrNull { it.linkKey == linkKey && queries.isBeforeReveal(it) && !queries.isIntended(it) }
            if (link == null) d else TermEdits.markIntended(d, link, ctx).also { marked = true }
        }
        return result == EditResult.APPLIED && marked
    }

    /** Ticks or unticks an item in `관련`. */
    fun toggleRelated(ref: ItemRef) {
        val ctx = editContext()
        viewModelScope.launch { apply(WorldCopy.ROW_RELATED) { d -> ItemEdits.toggleRelated(d, termKey, ref, ctx) } }
    }

    override fun onCleared() {
        indexHandle.release()
        super.onCleared()
    }
}
