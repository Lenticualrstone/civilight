package io.github.lenticualrstone.civilight.feature.library.newproject

import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.navigation.NewProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * The 새 프로젝트 draft shared by the four step entries (each `NewProject(step)` is its own back-stack
 * entry, so `이전` and system back are plain pops). One per activity; `+` starts it afresh. After
 * process death the first step entry composed seeds it from its saved snapshot ([restore]).
 */
class NewProjectDraftHolder {
    private val _form = MutableStateFlow(NewProjectForm())
    val form: StateFlow<NewProjectForm> = _form.asStateFlow()
    private var started = false

    /** A new run from `+` or the empty 서재's `새 프로젝트`. */
    fun reset() {
        _form.value = NewProjectForm()
        started = true
    }

    fun update(transform: (NewProjectForm) -> NewProjectForm) {
        started = true
        _form.update(transform)
    }

    /** After a restart: the saved snapshot of the step on screen becomes the draft, once. */
    fun restore(snapshot: String?) {
        if (started) return
        started = true
        _form.value = snapshot?.let(::decode) ?: NewProjectForm()
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(form: NewProjectForm): String = json.encodeToString(NewProjectForm.serializer(), form)

        fun decode(text: String): NewProjectForm? = runCatching { json.decodeFromString(NewProjectForm.serializer(), text) }.getOrNull()
    }
}

/** `+` / `새 프로젝트`: a fresh draft and step 1. */
fun LibraryServices.startNewProject() {
    draft.reset()
    deps.navigator.push(NewProject())
}
