package io.github.lenticualrstone.civilight.domain.recorder.inbox

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.pointsAt
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.InboxTarget
import io.github.lenticualrstone.civilight.model.recorder.KindFilter
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.StatusFilter

/** Where the inbox was opened from (LRC-27 preset table). */
sealed interface InboxOpening {
    /** 서재 row `라이프 레코더`: no filter. */
    data object FromLibrary : InboxOpening

    /** `레코더 · 정리 안 됨 n ›` on a scene memo card or a profile. */
    data class FromTarget(val target: TargetRef) : InboxOpening

    /** Checklist `연결된 기록 n개 정리하기` (LIB-13) or empty cast `라이프 레코더에서` (CST). */
    data class FromProject(val projectKey: Key) : InboxOpening
}

/** Inbox filters (LRC-27). Filter state lives for the app session and is never stored in files. */
object InboxFilters {
    fun preset(opening: InboxOpening): InboxFilterState = when (opening) {
        InboxOpening.FromLibrary -> InboxFilterState()
        is InboxOpening.FromTarget -> InboxFilterState(
            status = StatusFilter.UNSORTED,
            project = ProjectFilter.All,
            target = opening.target.toInboxTarget(),
        )
        is InboxOpening.FromProject -> InboxFilterState(status = StatusFilter.UNSORTED, project = ProjectFilter.Project(opening.projectKey))
    }

    /** Status chips are single choice. */
    fun withStatus(state: InboxFilterState, status: StatusFilter) = state.copy(status = status)

    /** Kind chips are toggles, at most one on ([PROPOSED]); none = both kinds. */
    fun toggleKind(state: InboxFilterState, kind: KindFilter) =
        state.copy(kind = if (state.kind == kind || kind == KindFilter.NONE) KindFilter.NONE else kind)

    fun withProject(state: InboxFilterState, project: ProjectFilter) = state.copy(project = project)

    /** The target chip's ×: the target filter clears and the project filter returns to `모든 프로젝트`. */
    fun clearTarget(state: InboxFilterState) = state.copy(target = null, project = ProjectFilter.All)

    /**
     * True when [record] passes the kind, project and target filters (not the status filter).
     * Project: linked to it or to anything inside it; `연결 없음`: no link into a project that
     * still exists. Target: a link to exactly that target `(projectKey, kind, key)`, so a same
     * item key in another project (after a `새 프로젝트로` import) does not match.
     */
    fun passesNonStatus(library: RecorderLibrary, record: RecorderRecord, state: InboxFilterState, env: RecorderEnv): Boolean {
        when (state.kind) {
            KindFilter.VOICE -> if (record.kind.known != RecordKind.VOICE) return false
            KindFilter.TEXT -> if (record.kind.known != RecordKind.TEXT) return false
            KindFilter.NONE -> Unit
        }
        val projects = RecordStates.impliedProjects(library, record.key, env)
        when (val p = state.project) {
            ProjectFilter.All -> Unit
            is ProjectFilter.Project -> if (p.projectKey !in projects) return false
            ProjectFilter.NoLink -> if (projects.isNotEmpty()) return false
        }
        val target = state.target?.toTargetRef()
        if (target != null) {
            if (library.linksOf(record.key).none { it.pointsAt(target) }) return false
        }
        return true
    }

    fun passesStatus(state: RecordState, filter: StatusFilter): Boolean = when (filter) {
        StatusFilter.ALL -> true
        StatusFilter.UNSORTED -> state == RecordState.UNSORTED
        StatusFilter.PUT -> state == RecordState.PUT
        StatusFilter.DONE -> state == RecordState.DONE
    }

    /**
     * The target of a target filter when some record still links to it (a project target always),
     * else null (no chip).
     */
    fun targetRef(library: RecorderLibrary, state: InboxFilterState): TargetRef? {
        val target = state.target?.toTargetRef() ?: return null
        return target.takeIf { t -> t.isProject || library.links.any { it.pointsAt(t) } }
    }
}

/** The inbox filter form of a link target. */
fun TargetRef.toInboxTarget(): InboxTarget = InboxTarget(projectKey, kind, key)

/** The link target of an inbox target filter. */
fun InboxTarget.toTargetRef(): TargetRef = TargetRef(projectKey, kind, key)
