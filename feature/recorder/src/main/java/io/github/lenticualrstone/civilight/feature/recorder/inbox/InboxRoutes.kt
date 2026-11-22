package io.github.lenticualrstone.civilight.feature.recorder.inbox

import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxFilters
import io.github.lenticualrstone.civilight.domain.recorder.inbox.InboxOpening
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.KindFilter
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.StatusFilter
import io.github.lenticualrstone.civilight.navigation.RecorderInbox

/**
 * The `RecorderInbox` route as an opening (LRC-27 presets):
 * - nothing set = from 서재 (no filter, `전체`)
 * - `projectKey` only = that project + `정리 안 됨` (LIB-13 checklist, empty cast `라이프 레코더에서`)
 * - `projectKey` + `targetKind` + `targetKey` = that target + `정리 안 됨` (`레코더 · 정리 안 됨 n ›`)
 *
 * `targetKind` uses the strings of `Recording.targetKind`. `filter` then selects one chip by id
 * ([CHIP_IDS]); unknown ids are ignored.
 */
object InboxRoutes {
    /** Chip ids accepted in `RecorderInbox.filter`. */
    val CHIP_IDS = listOf("all", "unsorted", "put", "done", "voice", "text", "noLink")

    fun opening(route: RecorderInbox): InboxOpening {
        val projectKey = route.projectKey ?: return InboxOpening.FromLibrary
        val kind = EntryRoutes.targetKind(route.targetKind)
        val key = route.targetKey
        return when {
            kind == RecorderTargetKind.PROJECT -> InboxOpening.FromTarget(TargetRef.project(projectKey))
            kind != null && key != null -> InboxOpening.FromTarget(TargetRef(projectKey, kind, key))
            else -> InboxOpening.FromProject(projectKey)
        }
    }

    /** The filter state the inbox opens with. */
    fun preset(route: RecorderInbox): InboxFilterState = withChip(InboxFilters.preset(opening(route)), route.filter)

    private fun withChip(state: InboxFilterState, id: String?): InboxFilterState = when (id?.lowercase()) {
        "all" -> state.copy(status = StatusFilter.ALL)
        "unsorted" -> state.copy(status = StatusFilter.UNSORTED)
        "put" -> state.copy(status = StatusFilter.PUT)
        "done" -> state.copy(status = StatusFilter.DONE)
        "voice" -> state.copy(kind = KindFilter.VOICE)
        "text" -> state.copy(kind = KindFilter.TEXT)
        "nolink" -> state.copy(project = ProjectFilter.NoLink, target = null)
        else -> state
    }
}
