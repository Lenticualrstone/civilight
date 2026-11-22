package io.github.lenticualrstone.civilight.feature.library.newproject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectFlow
import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.hooks.StartMaterialRow
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.LibraryDocs
import io.github.lenticualrstone.civilight.feature.library.common.actionContext
import io.github.lenticualrstone.civilight.feature.library.common.toAccess
import io.github.lenticualrstone.civilight.feature.library.common.toSession
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.WorldCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Step 4's lists: `라이프 레코더 기록 연결` candidates and `다른 프로젝트에서` sources. */
data class StartInputs(
    val records: List<StartMaterialRow> = emptyList(),
    val bundleSources: List<ProjectProfile> = emptyList(),
)

/**
 * One 새 프로젝트 step entry. The draft itself lives in [LibraryServices.draft] (shared by the four
 * entries); this view model supplies the lists step 4 shows and runs `만들기`.
 */
internal class NewProjectViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val stores = deps.stores
    private val docs = LibraryDocs(stores.library, viewModelScope)
    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    val inputs: StateFlow<StartInputs> = combine(stores.library.state, docs.docs, stores.recorder.state, deps.unlock.state) { library, docs, recorder, unlock ->
        val live = library.live.filter { !it.isExample }
        val snapshots = live.mapNotNull { e -> docs[e.key]?.let { ProjectSnapshot(e, it) } }
        val env = RecorderEnv(snapshots, unlock.toSession(), deps.clock.zone)
        val profiles = live.mapNotNull { e -> docs[e.key]?.let { ProjectProfile.of(it, isExample = e.isExample) } }
        StartInputs(
            records = RecorderHooks.startMaterialCandidates(recorder.library, env, deps.clock.now()),
            bundleSources = NewProjectFlow.bundleSources(profiles, unlock.toAccess()),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartInputs())

    init {
        viewModelScope.launch { stores.recorder.ensureLoaded() }
    }

    /** The source document of `다른 프로젝트에서`, when one is picked. */
    fun sourceDoc(form: NewProjectForm): ProjectDoc? = form.bundleSourceKey?.let { docs.docs.value[it] }

    /** `만들기`: creates the project and returns the stack to show (서재 › 프로젝트 홈 [› 세계 달력]). */
    fun create(form: NewProjectForm, onCreated: (List<Route>) -> Unit) {
        if (_creating.value) return
        _creating.value = true
        viewModelScope.launch {
            try {
                val result = ProjectCreator.create(
                    ports = StoreCreatorPorts(services),
                    form = form,
                    defaults = deps.settings.value.ratingDefaults,
                    ctx = stores.actionContext(),
                    bundleSource = sourceDoc(form),
                )
                onCreated(listOfNotNull(Library, ProjectHome(result.projectKey), if (result.opensCalendar) WorldCalendar(result.projectKey) else null))
            } finally {
                _creating.value = false
            }
        }
    }
}

private class StoreCreatorPorts(private val services: LibraryServices) : CreatorPorts {
    private val stores = services.deps.stores

    override suspend fun addProject(doc: ProjectDoc) {
        stores.library.add(doc)
    }

    override suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary) {
        stores.recorder.ensureLoaded()
        stores.recorder.updateLibrary(transform)
    }
}
