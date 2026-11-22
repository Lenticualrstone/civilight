package io.github.lenticualrstone.civilight.feature.recorder.inbox

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.Toast
import io.github.lenticualrstone.civilight.domain.recorder.inbox.FilterAction
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxFilters
import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuAction
import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuOption
import io.github.lenticualrstone.civilight.domain.recorder.inbox.MenuPreview
import io.github.lenticualrstone.civilight.domain.recorder.inbox.ProjectMenuItem
import io.github.lenticualrstone.civilight.domain.recorder.inbox.CaptureGuard
import io.github.lenticualrstone.civilight.domain.recorder.inbox.Inbox
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderPort
import io.github.lenticualrstone.civilight.feature.recorder.env.EnvSource
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordMenuSource
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordMenus
import io.github.lenticualrstone.civilight.domain.recorder.put.PutKind
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordWriter
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The record `⋯` sheet as drawn (LRC-29): preview card and options; [enabled] = the option runs in this build. */
data class MenuView(val preview: MenuPreview, val options: List<MenuOptionView>)

data class MenuOptionView(val option: MenuOption, val enabled: Boolean)

/** What a menu option asks the screen to do next. */
sealed interface MenuOutcome {
    /** Nothing more (the write is done, or the option did nothing). */
    data object Done : MenuOutcome

    /** Show this toast; its `되돌리기` runs [undo]. */
    data class ShowToast(val toast: Toast, val undo: suspend () -> Unit) : MenuOutcome

    /** Open the 넣기 sheet of [kind] for the record. */
    data class OpenPut(val recordKey: Key, val kind: PutKind = PutKind.MANUSCRIPT) : MenuOutcome
}

/**
 * The 기록함's state and actions (LRC-25 … LRC-27, LRC-29), plain Kotlin over the recorder ports so
 * it runs on the JVM. The filter is this inbox entry's own state, set once from the route's preset
 * (LRC-27: it lives for the session and resets when the inbox is opened from 서재 again). The env
 * (projects, unlock state) is reloaded when the unlock state changes and after every write into a
 * project; the library is followed as it changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InboxController(
    private val scope: CoroutineScope,
    private val recorder: RecorderPort,
    private val envSource: EnvSource,
    private val writer: RecordWriter,
    private val now: () -> Timestamp,
    private val relockPolicy: () -> RelockPolicy,
    unlockChanges: Flow<Any?>,
    preset: InboxFilterState,
) : RecordMenuSource {
    private val filter = MutableStateFlow(preset)
    private val query = MutableStateFlow("")
    private val _env = MutableStateFlow<RecorderEnv?>(null)

    /** The env the view was built with (null until the first load). */
    val env: StateFlow<RecorderEnv?> = _env.asStateFlow()

    val view: StateFlow<InboxView?> = combine(recorder.observe(), _env.filterNotNull(), filter, query) { library, env, f, q ->
        InboxViews.of(library, f, env, now(), relockPolicy(), q)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch { recorder.ensureLoaded() }
        scope.launch { unlockChanges.collectLatest { _env.value = envSource.load() } }
        scope.launch { writer.projectDocs.collect { doc -> _env.update { it?.withDoc(doc) } } }
    }

    /** Reloads the projects (after a write into one, or when coming back to the screen). */
    fun refresh() {
        scope.launch { _env.value = envSource.load() }
    }

    // ---- filters (LRC-27) ----

    /** A chip of the filter row; `프로젝트 ▾` is opened by the screen ([projectMenu]). */
    fun onFilter(action: FilterAction) {
        filter.value = when (action) {
            is FilterAction.Status -> InboxFilters.withStatus(filter.value, action.filter)
            is FilterAction.Kind -> InboxFilters.toggleKind(filter.value, action.filter)
            FilterAction.ClearTarget -> InboxFilters.clearTarget(filter.value)
            FilterAction.ProjectMenu -> filter.value
        }
    }

    fun projectMenu(): List<ProjectMenuItem> = _env.value?.let { Inbox.projectMenu(filter.value.project, it) }.orEmpty()

    fun setProject(project: ProjectFilter) {
        filter.value = InboxFilters.withProject(filter.value, project)
    }

    /** The header search (§6 Q11 `ED`). */
    fun search(text: String) {
        query.value = text
    }

    // ---- ⋯ menu (LRC-29) ----

    override fun menu(recordKey: Key): MenuView? {
        val env = _env.value ?: return null
        return RecordMenus.menu(recorder.current(), recordKey, env, now())
    }

    override suspend fun onMenu(recordKey: Key, action: MenuAction): MenuOutcome {
        val env = _env.value ?: return MenuOutcome.Done
        return RecordMenus.run(writer, recordKey, action, env)
    }

    /** `숨김 풀기` on a row of the open hidden group. */
    suspend fun unhide(recordKey: Key) = writer.unhide(recordKey)

    // ---- links on a row (`+ 연결`, LRC-20) ----

    suspend fun addLink(recordKey: Key, target: TargetRef) = writer.addLink(recordKey, target)

    // ---- capture guard (LRC-22, LIB-26) ----

    /** Projects whose `화면 캡처 가리기` applies to the rows on screen now. */
    fun protectedProjects(library: RecorderLibrary, visible: Collection<Key>): Set<Key> {
        val env = _env.value ?: return emptySet()
        return CaptureGuard.projectsFor(library, visible, env)
    }
}
