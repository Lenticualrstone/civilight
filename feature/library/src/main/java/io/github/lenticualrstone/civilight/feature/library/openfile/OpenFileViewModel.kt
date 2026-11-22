package io.github.lenticualrstone.civilight.feature.library.openfile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecision
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.ProjectRoute
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.resetTo
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

/**
 * 파일에서 열기 (LIB-52 … LIB-55) for one `content://` or `file://` [uri]. The file is read and decoded
 * off the main thread; PBKDF2 of an encrypted file too. The flow restarts from the file after a
 * process death (the password is never kept).
 *
 * `ED` (LIB-27): when the same-key project is hidden and its group is locked, the hidden-project
 * prompt comes before the sheet (the sheet would show its scene count and offer to replace it); a
 * cancelled prompt closes the flow.
 */
internal class OpenFileViewModel(
    private val uri: String,
    private val services: LibraryServices,
    private val context: Context,
) : ViewModel() {
    private val deps = services.deps
    private val stores = deps.stores
    private val _stage = MutableStateFlow<OpenStage>(OpenStage.Reading)
    val stage: StateFlow<OpenStage> = _stage.asStateFlow()

    /** Set when the flow is over (closed, refused by the prompt, or imported): the screen leaves. */
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private var bytes: ByteArray? = null
    private val sceneCounts = HashMap<Key, Int>()

    init {
        viewModelScope.launch { read() }
    }

    private suspend fun read() {
        stores.library.ensureLoaded()
        val data = withContext(Dispatchers.IO) { runCatching { readUri() }.getOrNull() }
        bytes = data
        val library = stores.library.state.value.entries
        loadSceneCounts(library)
        val name = withContext(Dispatchers.IO) { runCatching { displayName() }.getOrNull() } ?: Uri.parse(uri).lastPathSegment.orEmpty()
        val next = withContext(Dispatchers.Default) {
            OpenFileFlow.open(data, name, library, ::sceneCountOf, deps.clock.now(), deps.clock.zone)
        }
        show(next)
    }

    /** `열기` on the password sheet. */
    fun unlock(password: String) {
        val current = _stage.value as? OpenStage.Password ?: return
        val outer = bytes ?: return
        if (current.busy || password.isEmpty()) return
        _stage.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            val chars = password.toCharArray()
            val library = stores.library.state.value.entries
            val next = withContext(Dispatchers.Default) {
                try {
                    OpenFileFlow.unlock(outer, chars, current.sheet, library, ::sceneCountOf, deps.clock.now(), deps.clock.zone)
                } finally {
                    chars.fill('\u0000')
                }
            }
            show(next)
        }
    }

    fun choose(choice: SameProjectChoice) {
        val ask = _stage.value as? OpenStage.Ask ?: return
        if (!ask.busy) _stage.value = ask.choose(choice)
    }

    fun close() {
        _finished.value = true
    }

    /**
     * `열기` on `RHOpenFile`: runs in the app scope (it outlives this screen: `덮어쓰기` takes the old
     * copy's screens off the stack first). Ends at the 서재; a project that is not hidden opens
     * beside it. A partly damaged file says which part was left out (LIB-55).
     */
    fun import() {
        val ask = _stage.value as? OpenStage.Ask ?: return
        if (ask.busy) return
        _stage.value = ask.copy(busy = true)
        val importer = ProjectImporter(StoreImportPorts(deps))
        deps.appScope.launch {
            val outcome = runCatching {
                importer.import(ask.pkg, ask.decision, ask.choice, deps.clock.now(), deps.clock.zone, Keys::newKey)
            }
            outcome.onSuccess { done ->
                deps.navigator.resetTo(Library)
                if (!done.hidden) deps.navigator.openDetail(ProjectHome(done.projectKey))
                done.failedMessage?.let { deps.toasts.show(it, actionLabel = null) }
            }.onFailure {
                deps.toasts.show(OpenFileCopy.TODO_COPY_LIB52_IMPORT_FAILED, actionLabel = null)
            }
            _finished.value = true
        }
    }

    /** `열기` on the `.civilight-recorder` sheet (LIB-48): merge, then the 서재 with the 기록함 on top. */
    fun importRecorder() {
        val r = _stage.value as? OpenStage.Recorder ?: return
        if (r.busy) return
        _stage.value = r.copy(busy = true)
        deps.appScope.launch {
            runCatching { RecorderFiles.import(r.content, StoreImportPorts(deps), Keys::newKey) }
                .onSuccess {
                    deps.navigator.resetTo(Library)
                    deps.navigator.push(RecorderInbox())
                }
                .onFailure { deps.toasts.show(OpenFileCopy.TODO_COPY_LIB52_IMPORT_FAILED, actionLabel = null) }
            _finished.value = true
        }
    }

    private suspend fun show(next: OpenStage) {
        if (next is OpenStage.Ask && next.existingHidden) {
            val existing = (next.decision as? OpenDecision.AskSameProject)?.existing
            if (existing != null && !deps.unlock.state.value.projectOpen(existing.key)) {
                if (deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) != AuthResult.SUCCESS) {
                    _finished.value = true
                    return
                }
                deps.unlock.unlockHiddenGroup()
            }
        }
        _stage.value = next
    }

    private suspend fun loadSceneCounts(library: List<LibraryEntry>) {
        for (entry in library.filter { it.deletedAt == null }) {
            sceneCounts[entry.key] = runCatching { stores.library.readDoc(entry.key).manuscript.liveScenes.size }.getOrDefault(0)
        }
    }

    private fun sceneCountOf(entry: LibraryEntry): Int = sceneCounts[entry.key] ?: 0

    private fun readUri(): ByteArray? =
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }

    private fun displayName(): String? =
        context.contentResolver.query(Uri.parse(uri), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
}

/** [ImportPorts] over the app's stores, navigator and project sessions. */
internal class StoreImportPorts(private val deps: FeatureDeps) : ImportPorts {
    private val stores = deps.stores

    override fun libraryEntries(): List<LibraryEntry> = stores.library.state.value.entries

    override suspend fun importBlob(hash: String, bytes: ByteArray, mime: String, fileName: String) {
        if (stores.blobs.contains(hash)) return
        stores.blobs.import(ByteArrayInputStream(bytes), mime, BlobSource.FILE, fileName)
    }

    override suspend fun releaseProject(key: Key) {
        withContext(Dispatchers.Main) {
            if (deps.navigator.backStack.any { (it as? ProjectRoute)?.projectKey == key }) deps.navigator.resetTo(Library)
        }
        // Screens release their sessions when their entries are cleared; the cached store must be
        // closed before the key is reused by the import (the 15 s session cache, hazard of batch 2).
        repeat(SESSION_WAIT_TRIES) {
            deps.projects.closeIdle()
            if (deps.projects.peek(key) == null) return
            delay(SESSION_WAIT_MS)
        }
    }

    override suspend fun moveAside(key: Key) {
        stores.library.moveAsideForImport(key)
    }

    override suspend fun addProject(doc: ProjectDoc) {
        stores.library.add(doc)
        deps.projects.closeIdle()
    }

    override suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary) {
        stores.recorder.ensureLoaded()
        stores.recorder.updateLibrary(transform)
    }

    override suspend fun removeRecentsOf(key: Key) {
        stores.appState.ensureLoaded()
        stores.appState.removeRecentsOf(key)
    }

    private companion object {
        const val SESSION_WAIT_TRIES = 40
        const val SESSION_WAIT_MS = 50L
    }
}
