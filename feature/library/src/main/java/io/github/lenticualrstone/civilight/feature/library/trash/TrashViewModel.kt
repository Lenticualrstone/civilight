package io.github.lenticualrstone.civilight.feature.library.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.domain.trash.SceneChoice
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.LibraryDocs
import io.github.lenticualrstone.civilight.feature.library.common.actionContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The 휴지통 (LIB-38, LIB-39): project rows from the 서재 index, each live project's own rows (its
 * document read without opening a store) and the recorder rows, turned into [TrashUi] by
 * [TrashState] with the shell's unlock state; writes through [TrashActions].
 */
internal class TrashViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val stores = deps.stores
    private val liveDocs = LibraryDocs(stores.library, viewModelScope)
    private val trashedDocs = LibraryDocs(stores.library, viewModelScope) { it.trashed }

    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    /** A block restore waiting for `씬도 되살리기` · `다른 씬 끝에` (T-14). */
    private val _sceneChoice = MutableStateFlow<TrashOutcome.NeedsSceneChoice?>(null)
    val sceneChoice: StateFlow<TrashOutcome.NeedsSceneChoice?> = _sceneChoice.asStateFlow()

    private val actions = TrashActions(StoreTrashPorts())

    val ui: StateFlow<TrashUi> = combine(
        stores.library.state,
        liveDocs.docs,
        trashedDocs.docs,
        stores.recorder.state,
        deps.unlock.state,
    ) { library, live, trashed, recorder, unlock ->
        val liveEntries = library.live.filter { !it.isExample }
        val inputs = TrashInputs(
            index = library.index,
            projectEntries = library.trash.entries,
            trashedDocs = trashed.mapNotNull { (k, d) -> d?.let { k to it } }.toMap(),
            liveDocs = liveEntries.mapNotNull { live[it.key] },
            recorder = recorder.library,
            recorderEntries = recorder.trash.entries,
        )
        val loaded = library.loaded && recorder.loaded && liveEntries.all { it.key in live }
        TrashState.build(inputs, unlock, stores.actionContext().trash, loaded)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, TrashUi())

    init {
        // A prompt answered for one flow never carries over a change of the lock state.
        viewModelScope.launch {
            deps.unlock.state.map { it.hiddenGroupUnlockedAt to it.itemsUnlockedAt to it.keptOpenSince to it.recordsUnlockedAt }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    actions.forgetAll()
                    _sceneChoice.value = null
                }
        }
        viewModelScope.launch {
            stores.recorder.ensureLoaded()
            stores.library.ensureLoaded()
            // The daily purge's share (LIB-39 `ED`: also on opening the 휴지통): whole projects and
            // records whose 30 days are over, then each live project's own due entries (T-08).
            runCatching { stores.runMaintenance() }.onFailure { if (it is CancellationException) throw it }
            actions.purgeDueRecords()
            for (entry in stores.library.state.value.live.filter { !it.isExample && !it.readOnly }) {
                val doc = runCatching { stores.library.readDoc(entry.key) }.getOrNull() ?: continue
                actions.purgeDue(doc)
            }
        }
    }

    private fun run(block: suspend TrashActions.() -> TrashOutcome) {
        if (_working.value) return
        _working.value = true
        _sceneChoice.value = null
        viewModelScope.launch {
            try {
                val outcome = actions.block()
                if (outcome is TrashOutcome.NeedsSceneChoice) _sceneChoice.value = outcome
            } finally {
                _working.value = false
            }
        }
    }

    fun restore(row: TrashRowUi) = run { restore(row) }

    fun restoreBlock(row: TrashRowUi, blockKey: Key) = run { restoreBlock(row, blockKey) }

    fun purge(row: TrashRowUi) = run { purge(row) }

    fun emptyTrash() = run { emptyTrash(ui.value.rows) }

    /** `씬도 되살리기` or `다른 씬 끝에` <scene> for the waiting block restore. */
    fun chooseScene(choice: SceneChoice) {
        val waiting = _sceneChoice.value ?: return
        _sceneChoice.value = null
        val row = ui.value.row(waiting.entryKey) ?: return
        run { restoreBlocksWith(row, waiting.blockKeys, choice) }
    }

    fun dismissSceneChoice() {
        _sceneChoice.value?.let { actions.forget(it.entryKey) }
        _sceneChoice.value = null
    }

    private inner class StoreTrashPorts : TrashPorts {
        override suspend fun authenticate(): Boolean =
            deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS

        override suspend fun restoreProject(projectKey: Key) {
            // A store closed by the move to the trash may still be cached by the sessions for a few
            // seconds; drop idle ones so reopening the restored project opens it afresh.
            deps.projects.closeIdle()
            stores.library.restore(projectKey)
        }

        override suspend fun purgeProject(projectKey: Key) {
            deps.projects.closeIdle()
            stores.purgeProject(projectKey)
        }

        override suspend fun editProject(projectKey: Key, label: String, transform: (ProjectDoc) -> ProjectDoc): EditResult =
            services.writer.edit(projectKey, label) { doc, _ -> transform(doc) }

        override suspend fun purgeInProject(projectKey: Key, transform: (ProjectDoc) -> ProjectDoc): EditResult =
            services.writer.purge(projectKey, TrashScreenCopy.PURGE, transform)

        override suspend fun updateRecorder(transform: (RecorderTrashState) -> RecorderTrashState) {
            stores.recorder.ensureLoaded()
            stores.recorder.update { s ->
                val next = transform(RecorderTrashState(s.library, s.trash.entries))
                s.copy(library = next.library, trash = s.trash.copy(entries = next.entries))
            }
        }

        override suspend fun updateRecorderLibrary(transform: (RecorderLibrary) -> RecorderLibrary) {
            stores.recorder.ensureLoaded()
            stores.recorder.updateLibrary(transform)
        }

        override suspend fun collectBlobs() {
            stores.collectBlobGarbage()
        }

        override fun toast(text: String) = deps.toasts.show(text, actionLabel = null)

        override fun context(): ActionContext = stores.actionContext()
    }
}
