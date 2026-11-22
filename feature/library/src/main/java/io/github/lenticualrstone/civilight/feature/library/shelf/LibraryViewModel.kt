package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.LibraryDocs
import io.github.lenticualrstone.civilight.feature.library.common.toAccess
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectRoute
import io.github.lenticualrstone.civilight.navigation.resetTo
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.ui.security.AuthCopy
import io.github.lenticualrstone.civilight.ui.security.AuthRequest
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import androidx.compose.runtime.snapshotFlow
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.model.app.ViewMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The 서재 (LIB-01 … LIB-03): the index, each live project's document (row meta and the ⋯ sheet;
 * never opens a project store), the recorder library, onboarding, unlock state and settings, turned
 * into [ShelfUi] by [ShelfState]. Writes go through the stores' own operations or [LibraryServices].
 */
internal class LibraryViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val stores = deps.stores
    private val libraryDocs = LibraryDocs(stores.library, viewModelScope)

    /** Documents by project key, for the ⋯ sheet and the delete dialog. */
    val documents: StateFlow<Map<Key, ProjectDoc?>> = libraryDocs.docs

    val state: StateFlow<ShelfUi> = combine(
        combine(stores.library.state, libraryDocs.docs) { library, docs -> library to docs },
        combine(stores.recorder.state, stores.appState.recents.state) { recorder, recents -> recorder to recents },
        stores.appState.onboarding.state,
        deps.unlock.state,
        deps.settings,
    ) { (library, docs), (recorder, recents), onboarding, unlock, settings ->
        ShelfState.build(
            projects = library.live.map { ShelfProject(it, docs[it.key]) },
            access = unlock.toAccess(),
            relockPolicy = settings.relockPolicy,
            recorder = recorder.library,
            onboarding = onboarding,
            now = deps.clock.now(),
            zone = deps.clock.zone,
            loaded = library.loaded,
            showCards = ShelfCards.showCards(settings),
            recentItems = recents.items,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShelfUi())

    private val recorder = RecentsRecorder(
        ports = object : RecentsRecorder.Ports {
            override fun openDoc(projectKey: Key): ProjectDoc? = deps.projects.peekDoc(projectKey)
            override fun storedDoc(projectKey: Key): ProjectDoc? = libraryDocs.docs.value[projectKey]
            override fun isExample(projectKey: Key): Boolean = stores.library.state.value.entry(projectKey)?.isExample != false
            override suspend fun record(item: RecentItem) = stores.appState.recordRecent(item)
        },
        now = { deps.clock.now() },
        newKey = Keys::newKey,
    )

    init {
        viewModelScope.launch {
            stores.recorder.ensureLoaded()
            stores.appState.ensureLoaded()
            // LIB-18: an example left by an interrupted `온보딩 끝내기` goes now.
            services.onboarding.cleanUpLeftover()
        }
        // `예시 프로젝트 삭제됨` shows until the first project exists (LIB-18 `ED`).
        viewModelScope.launch {
            stores.appState.ensureLoaded()
            combine(stores.library.state, stores.appState.onboarding.state) { library, _ -> library }
                .collect { library -> if (library.loaded) services.onboarding.useUpNoticeIfDue(library.live) }
        }
        // 최근 연 파일 (LIB-05): the 서재 stays at the bottom of the stack while other screens are
        // opened over it, so it records each new top route (`ED`; no area records opens itself yet).
        viewModelScope.launch {
            stores.appState.ensureLoaded()
            snapshotFlow { deps.navigator.backStack.lastOrNull() }
                .distinctUntilChanged()
                .collectLatest { route -> if (route != null) recorder.onTop(route) }
        }
    }

    /** `목록 | 카드` (LIB-04): remembered for the 서재 in `AppSettings.viewByScreen`. */
    fun showCards(cards: Boolean) {
        viewModelScope.launch { stores.settings.setViewMode(ShelfCards.SCREEN, if (cards) ViewMode.CARD else ViewMode.LIST) }
    }

    /** `지문으로 보기` (LIB-32): one prompt opens the whole group; cancel or failure changes nothing. */
    fun unlockHiddenGroup() {
        viewModelScope.launch {
            if (deps.biometric.authenticate(AuthRequest(AuthCopy.HIDDEN_PROJECTS)) == AuthResult.SUCCESS) {
                deps.unlock.unlockHiddenGroup()
            }
        }
    }

    /** `다시 잠그기` (LIB-33). */
    fun relock() = deps.unlock.relockNow()

    /** ⋯ `프로젝트 숨기기` (LIB-31). */
    fun hide(key: Key) {
        viewModelScope.launch { services.report(services.protection.hide(key)) }
    }

    /** `숨김 풀기` inside the open group (LIB-31). */
    fun unhide(key: Key) {
        viewModelScope.launch { services.protection.unhideInOpenGroup(key) }
    }

    /** `이름 바꾸기` (LIB-02): the store renames `project.json` and the index as one undo step. */
    fun rename(key: Key, name: String) {
        viewModelScope.launch {
            // The example is read-only and a blank name is refused; both leave the name as it was.
            runCatching { stores.library.rename(key, name) }
                .onFailure { if (it !is ReadOnlyProjectException && it !is IllegalArgumentException) throw it }
        }
    }

    /**
     * `휴지통으로` (LIB-03): screens of the project leave the stack first (the store closes it), then
     * the project moves to the trash as one entry and leaves 최근 연 파일.
     */
    fun moveToTrash(key: Key) {
        val navigator = deps.navigator
        if (navigator.backStack.any { (it as? ProjectRoute)?.projectKey == key }) navigator.resetTo(Library)
        viewModelScope.launch {
            // The example never enters the trash (LIB-18 deletes it); the ⋯ sheet does not offer it.
            runCatching { stores.library.moveToTrash(key) }
                .onSuccess { stores.appState.removeRecentsOf(key) }
                .onFailure { if (it !is ReadOnlyProjectException) throw it }
        }
    }
}
