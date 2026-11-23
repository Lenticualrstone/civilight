package io.github.lenticualrstone.civilight.domain.recorder.inbox

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.RecorderTimeLabels
import io.github.lenticualrstone.civilight.domain.recorder.hide.RecordHiding
import io.github.lenticualrstone.civilight.domain.recorder.link.Chip
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkViews
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.link.Targets
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordStates
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.KindFilter
import io.github.lenticualrstone.civilight.model.recorder.ProjectFilter
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.StatusFilter

/** What a filter chip does. */
sealed interface FilterAction {
    data class Status(val filter: StatusFilter) : FilterAction
    data class Kind(val filter: KindFilter) : FilterAction
    data object ProjectMenu : FilterAction
    data object ClearTarget : FilterAction
}

/** One chip of the filter row (`전체 12`, `정리 안 됨 3`, `넣음`, `끝냄`, `음성`, `글`, `프로젝트 ▾`). */
data class FilterChip(val label: String, val selected: Boolean, val action: FilterAction)

/** One entry of the `프로젝트 ▾` menu. */
data class ProjectMenuItem(val label: String, val filter: ProjectFilter, val selected: Boolean)

/**
 * One inbox row (LRC-25): header (`음성 0:24` · `오늘 08:41` · state), body text, player for voice
 * records, link tags, actions. [actions] are `원고에 넣기 · 카드에 넣기 · 페이지로 만들기` on every row,
 * then `+ 연결` on a row without links; the `⋯` button follows (its aria label is TODO_COPY_LRC_09).
 */
data class InboxRow(
    val recordKey: Key,
    val kindLabel: String,
    val meta: String,
    val state: RecordState,
    val text: String,
    val hasPlayer: Boolean,
    val linkTags: List<Chip>,
    val actions: List<String>,
    /** In the open hidden group: the right action `숨김 풀기`. */
    val unhideAction: String? = null,
) {
    val stateLabel: String get() = state.label
}

/** The locked group row (DS `LockRow`, LRC-40). */
data class LockedGroup(val count: Int) {
    val title: String get() = "${RecorderCopy.HIDDEN_RECORDS} $count"
    val sub: String get() = RecorderCopy.HIDDEN_GROUP_SUB
    val button: String get() = RecorderCopy.OPEN_WITH_FINGERPRINT
}

/** The open hidden group (LRC-41): header `숨긴 기록 3 · 열림`, right note, the hidden rows. */
data class OpenGroup(val count: Int, val relockNote: String, val rows: List<InboxRow>) {
    val header: String get() = "${RecorderCopy.HIDDEN_RECORDS} $count${RecorderCopy.SEP}${RecorderCopy.OPENED}"
}

/** `EmptyLr` (LRC-30): the library has no records at all (trash not counted). */
data class EmptyInbox(
    val chip: String = "${RecorderCopy.ALL} 0",
    val title: String = RecorderCopy.EMPTY_TITLE,
    val sub: String = RecorderCopy.EMPTY_SUB,
    val buttons: List<String> = listOf(RecorderCopy.RECORD_BY_VOICE, RecorderCopy.BY_TEXT_SHORT),
)

/** The whole inbox screen (LRC-25 … LRC-27, LRC-30, LRC-40, LRC-41). */
data class InboxScreen(
    val smallLine: String,
    val title: String,
    /** `다시 잠그기` while hidden records are open. */
    val headerAction: String?,
    val filters: List<FilterChip>,
    val lockedGroup: LockedGroup?,
    val openGroup: OpenGroup?,
    val rows: List<InboxRow>,
    val empty: EmptyInbox?,
    /** Filtered list is empty (copy TODO_COPY_LRC_12); the filter row stays. */
    val filteredEmpty: Boolean,
    val bottomButtons: List<String>,
)

/** The 기록함 (inbox). */
object Inbox {
    /** Records not in the trash, newest first only (`createdAt` descending; never floated by state). */
    fun ordered(library: RecorderLibrary): List<RecorderRecord> =
        library.records.filter { it.deletedAt == null }.sortedWith(compareByDescending<RecorderRecord> { it.createdAt }.thenByDescending { it.key })

    fun row(library: RecorderLibrary, record: RecorderRecord, env: RecorderEnv, now: Timestamp, inOpenGroup: Boolean = false): InboxRow {
        val links = library.linksOf(record.key)
        val actions = listOf(RecorderCopy.PUT_MANUSCRIPT, RecorderCopy.PUT_CARD, RecorderCopy.MAKE_PAGE) +
            (if (links.isEmpty()) listOf(RecorderCopy.ADD_LINK) else emptyList())
        return InboxRow(
            recordKey = record.key,
            kindLabel = RecorderTimeLabels.kindLabel(record),
            meta = RecorderTimeLabels.meta(record.createdAt, now, env.zone),
            state = RecordStates.state(library, record, env),
            text = Runs.text(record.runs),
            hasPlayer = record.kind.known == RecordKind.VOICE && record.audio != null,
            linkTags = Chips.inboxTags(record, links, env),
            actions = actions,
            unhideAction = if (inOpenGroup) RecorderCopy.UNHIDE else null,
        )
    }

    /**
     * Counts over records not in the trash that pass the project and target filters, excluding
     * hidden records while they are locked: `전체 <n>` and `정리 안 됨 <u>`. The kind chips
     * (`음성` · `글`) do not change them: they only narrow the list, so `전체` keeps reading as the
     * status total (emulator round 1; departs from the LRC-27 `[PROPOSED]` "after the kind filter").
     * Project and target stay in, so `정리 안 됨 <u>` matches the `레코더 · 정리 안 됨 n ›` line that opened it.
     */
    fun counts(library: RecorderLibrary, filter: InboxFilterState, env: RecorderEnv): Pair<Int, Int> {
        val scope = filter.copy(kind = KindFilter.NONE)
        val visible = ordered(library).filter { (!it.hidden || env.recordsOpen) && InboxFilters.passesNonStatus(library, it, scope, env) }
        return visible.size to visible.count { RecordStates.state(library, it, env) == RecordState.UNSORTED }
    }

    fun filterChips(library: RecorderLibrary, filter: InboxFilterState, env: RecorderEnv): List<FilterChip> {
        val (all, unsorted) = counts(library, filter, env)
        val target = InboxFilters.targetRef(library, filter)
        val targetChip = target?.let {
            FilterChip("${targetLabel(it, env)}${RecorderCopy.CLEAR}", true, FilterAction.ClearTarget)
        }
        return listOfNotNull(targetChip) + listOf(
            FilterChip("${RecorderCopy.ALL} $all", filter.status == StatusFilter.ALL, FilterAction.Status(StatusFilter.ALL)),
            FilterChip("${RecorderCopy.UNSORTED} $unsorted", filter.status == StatusFilter.UNSORTED, FilterAction.Status(StatusFilter.UNSORTED)),
            FilterChip(RecorderCopy.PUT, filter.status == StatusFilter.PUT, FilterAction.Status(StatusFilter.PUT)),
            FilterChip(RecorderCopy.DONE, filter.status == StatusFilter.DONE, FilterAction.Status(StatusFilter.DONE)),
            FilterChip(RecorderCopy.VOICE, filter.kind == KindFilter.VOICE, FilterAction.Kind(KindFilter.VOICE)),
            FilterChip(RecorderCopy.TEXT, filter.kind == KindFilter.TEXT, FilterAction.Kind(KindFilter.TEXT)),
            FilterChip(projectChipLabel(filter.project, env), filter.project != ProjectFilter.All, FilterAction.ProjectMenu),
        )
    }

    /** `프로젝트 ▾`, or `<project label> ▾` / `연결 없음 ▾` when set ([PROPOSED]). */
    fun projectChipLabel(filter: ProjectFilter, env: RecorderEnv): String = when (filter) {
        ProjectFilter.All -> RecorderCopy.PROJECT_FILTER
        ProjectFilter.NoLink -> "${RecorderCopy.NO_LINK}${RecorderCopy.DROPDOWN}"
        is ProjectFilter.Project -> "${projectLabel(filter.projectKey, env) ?: ""}${RecorderCopy.DROPDOWN}"
    }

    /**
     * `모든 프로젝트` · each project in 서재 order · `연결 없음`. Locked hidden projects are not listed;
     * projects with `라이프 레코더 연결 가리기` show their rating (`ED` Q17).
     */
    fun projectMenu(filter: ProjectFilter, env: RecorderEnv): List<ProjectMenuItem> =
        listOf(ProjectMenuItem(RecorderCopy.ALL_PROJECTS, ProjectFilter.All, filter == ProjectFilter.All)) +
            env.projects.filter { !env.isLockedHidden(it) }.map {
                val f = ProjectFilter.Project(it.key)
                ProjectMenuItem(projectLabel(it.key, env).orEmpty(), f, filter == f)
            } +
            ProjectMenuItem(RecorderCopy.NO_LINK, ProjectFilter.NoLink, filter == ProjectFilter.NoLink)

    private fun projectLabel(key: Key, env: RecorderEnv): String? {
        val project = env.project(key) ?: return null
        return when {
            env.isLockedHidden(project) -> RecorderCopy.HIDDEN_PROJECT
            project.linkMask -> LinkViews.ratingBadge(project.rating)
            else -> project.name
        }
    }

    private fun targetLabel(target: TargetRef, env: RecorderEnv): String {
        val project = env.project(target.projectKey)
        return when {
            project != null && env.isLockedHidden(project) -> RecorderCopy.HIDDEN_PROJECT
            project != null && project.linkMask -> LinkViews.ratingBadge(project.rating)
            else -> Targets.resolve(env, target).let { r ->
                if (!target.isProject && r.hidden && !env.itemsOpen(target.projectKey)) RecorderCopy.HIDDEN_ITEM else r.label.orEmpty()
            }
        }
    }

    /**
     * The inbox screen. Hidden records: while locked they are not listed, not counted and collapse
     * into the locked row; while open they are listed in the open group (with `숨김 풀기`) and counted.
     * [relockPolicy] picks the open group's note (`앱을 떠나면 잠김`; TODO_COPY_LRC_18 otherwise).
     */
    fun screen(
        library: RecorderLibrary,
        filter: InboxFilterState,
        env: RecorderEnv,
        now: Timestamp,
        relockPolicy: RelockPolicy = RelockPolicy.ON_LEAVE,
    ): InboxScreen {
        val all = ordered(library)
        val bottom = listOf(RecorderCopy.TO_TEXT, RecorderCopy.RECORD_BY_VOICE)
        if (all.isEmpty()) {
            return InboxScreen(
                RecorderCopy.LIBRARY_LINE, RecorderCopy.APP_NAME, null, emptyList(), null, null, emptyList(),
                EmptyInbox(), filteredEmpty = false, bottomButtons = emptyList(),
            )
        }
        fun passes(r: RecorderRecord) = InboxFilters.passesNonStatus(library, r, filter, env) &&
            InboxFilters.passesStatus(RecordStates.state(library, r, env), filter.status)
        val hiddenCount = RecordHiding.hiddenCount(library)
        val rows = all.filter { !it.hidden && passes(it) }.map { row(library, it, env, now) }
        val lockedGroup = if (hiddenCount > 0 && !env.recordsOpen) LockedGroup(hiddenCount) else null
        val openGroup = if (hiddenCount > 0 && env.recordsOpen) {
            OpenGroup(
                hiddenCount,
                if (relockPolicy == RelockPolicy.ON_LEAVE) RecorderCopy.LOCKS_ON_LEAVE else RecorderCopy.TODO_RELOCK_NOTE,
                all.filter { it.hidden && passes(it) }.map { row(library, it, env, now, inOpenGroup = true) },
            )
        } else {
            null
        }
        return InboxScreen(
            smallLine = RecorderCopy.LIBRARY_LINE,
            title = RecorderCopy.APP_NAME,
            headerAction = if (env.recordsOpen && hiddenCount > 0) RecorderCopy.RELOCK else null,
            filters = filterChips(library, filter, env),
            lockedGroup = lockedGroup,
            openGroup = openGroup,
            rows = rows,
            empty = null,
            filteredEmpty = rows.isEmpty() && openGroup?.rows.isNullOrEmpty(),
            bottomButtons = bottom,
        )
    }
}
