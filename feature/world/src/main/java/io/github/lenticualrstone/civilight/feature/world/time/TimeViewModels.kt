package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.world.time.TimelineView
import io.github.lenticualrstone.civilight.domain.world.time.WorldTimeEdits
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.Deleted
import io.github.lenticualrstone.civilight.feature.world.common.ItemLifecycle
import io.github.lenticualrstone.civilight.feature.world.common.Restored
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
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

/** The 연표 (WLD-31): rows at the lens for the chosen view. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(projectKey: Key, env: WorldEnv) : EventEditorViewModel(projectKey, env) {
    private data class ViewInput(val view: TimelineView, val anchorKey: Key?)

    private val viewInput = MutableStateFlow(ViewInput(TimelineView.EPISODE, null))

    val ui: StateFlow<TimelineUi?> = combine(doc.filterNotNull(), hiddenOpen, viewInput) { d, h, v -> Triple(d, h, v) }
        .mapLatest { (d, h, v) -> TimelineState.build(d, v.view, v.anchorKey, h) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The view chip (and the anchor event of `기준 사건`); the screen keeps them saveable. */
    fun setView(view: TimelineView, anchorKey: Key?) {
        viewInput.value = ViewInput(view, anchorKey)
    }
}

/**
 * Writes of the 사건 sheet and detail (WLD-32): `넣기` / `고치기`, `지우기` (`domain:trash`) and
 * `되살리기`. One instance per host screen and project (the shared [EventCreateSheet] content uses it
 * through [rememberEventEditor]).
 */
open class EventEditorViewModel(projectKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    /** Saves [draft] as a new event ([eventKey] null) or into [eventKey]; returns the event key. */
    suspend fun save(draft: EventDraft, eventKey: Key?): Key? {
        var saved: Key? = null
        val ctx = editContext()
        val label = if (eventKey == null) WorldCopy.CREATE_EVENT else WorldCopy.BTN_EDIT
        val result = apply(label) { d ->
            when (val r = EventForms.save(d, draft, eventKey, ctx)) {
                is EventSave.Saved -> r.doc.also { saved = r.eventKey }
                EventSave.Invalid -> d
            }
        }
        return if (result == EditResult.APPLIED) saved else null
    }

    /** `⋯ › 지우기`: the event goes to the trash; returns the toast (null when nothing happened). */
    suspend fun delete(eventKey: Key): Deleted? {
        var deleted: Deleted? = null
        val ctx = actionContext()
        val result = apply(WorldCopy.MENU_DELETE) { d -> ItemLifecycle.delete(d, ItemId.event(eventKey), ctx)?.also { deleted = it }?.doc ?: d }
        return if (result == EditResult.APPLIED) deleted else null
    }

    /** `되살리기` of a trashed event. */
    suspend fun restore(eventKey: Key, authenticated: Boolean): Restored {
        var outcome: Restored = Restored.NotFound
        val ctx = actionContext()
        apply(WorldCopy.BTN_RESTORE) { d ->
            when (val r = ItemLifecycle.restore(d, ItemId.event(eventKey), ctx, authenticated)) {
                is Restored.Done -> r.doc.also { outcome = r }
                else -> d.also { outcome = r }
            }
        }
        return outcome
    }
}

/** 사건 상세 (WLD-32 edit and delete entry, WLD-33 view). */
@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModel(projectKey: Key, val eventKey: Key, env: WorldEnv) : EventEditorViewModel(projectKey, env) {
    val ui: StateFlow<EventDetailUi?> = combine(doc.filterNotNull(), hiddenOpen) { d, h -> d to h }
        .mapLatest { (d, h) -> EventDetailState.build(d, eventKey, h) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** `+ 변화 › 넣기` (WLD-33): a `◆` from this event on, one undo step. */
    suspend fun addChange(draft: ChangeDraft): Boolean {
        var added = false
        val ctx = actionContext().edit
        val result = apply(WorldCopy.CHANGE_TITLE) { d -> EventChanges.add(d, eventKey, draft, ctx)?.also { added = true } ?: d }
        return result == EditResult.APPLIED && added
    }
}

/** `프로젝트 설정 › 세계 시간` (WLD-29). */
class WorldTimeViewModel(projectKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    val ui: StateFlow<WorldTimeUi?> = combine(doc.filterNotNull(), readOnly) { d, r -> WorldTimeState.build(d, r) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setMode(mode: WorldTimeMode) = edit(WorldCopy.WORLD_TIME) { d -> WorldTimeEdits.setMode(d, mode) }

    /** Typing the year: consecutive keystrokes are one undo step; a blank field clears the year. */
    fun setStartYear(text: String) {
        val year = WorldTimeEdits.parseYear(text)
        if (text.isNotBlank() && year == null) return
        edit(WorldCopy.STORY_START_YEAR, mergeKey = MERGE_YEAR) { d -> WorldTimeEdits.setStartYear(d, year) }
    }

    /** `저장` of the 세계 달력 editor (WLD-30): one undo step; false while the draft has a problem. */
    suspend fun saveCalendar(draft: CalendarDraft): Boolean {
        var saved = false
        val result = apply(WorldCopy.CALENDAR_TITLE) { d ->
            val calendar = draft.toCalendar(d.project.worldTime.calendar) ?: return@apply d
            saved = true
            WorldTimeEdits.setCalendar(d, calendar)
        }
        return saved && result == EditResult.APPLIED
    }

    private companion object {
        const val MERGE_YEAR = "world.storyStartYear"
    }
}

/** The event editor of the screen hosting a sheet (the shared [EventCreateSheet] is shown over other areas' screens). */
@Composable
fun rememberEventEditor(projectKey: Key): EventEditorViewModel {
    val deps = LocalFeatureDeps.current
    return entryViewModel(key = "world.eventEditor.$projectKey") { EventEditorViewModel(projectKey, WorldEnv.of(deps)) }
}

/** True when the 연표 module is on (`시간 없이` turns it off, WLD-29). */
fun timelineAvailable(doc: ProjectDoc): Boolean = doc.project.worldTime.mode.known != WorldTimeMode.NONE
