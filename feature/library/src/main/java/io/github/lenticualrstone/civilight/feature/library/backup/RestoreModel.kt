package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.backup.BackupPlanner
import io.github.lenticualrstone.civilight.domain.library.backup.RestoreRunRow
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.BackupEntry
import io.github.lenticualrstone.civilight.model.app.BackupKind
import io.github.lenticualrstone.civilight.model.app.BackupRun
import java.time.ZoneId

/** One run row of `백업 · 30일`. */
data class RestoreRunUi(val runId: String, val title: String, val sub: String, val selected: Boolean)

/**
 * One project of the chosen run (§6 Q25 `ED`: a checklist after choosing a run). Hidden projects are
 * one row, `숨긴 프로젝트 <h>`, until the 복구 암호 opens them ([key] null).
 */
data class RestoreProjectUi(val key: Key?, val label: String, val checked: Boolean)

/** 백업에서 되살리기 (LIB-59, board `Restore`). */
data class RestoreUi(
    val connected: Boolean,
    val runs: List<RestoreRunUi>,
    val projects: List<RestoreProjectUi>,
    val choice: SameProjectChoice,
    /** `<M월 D일> 백업 되살리기`; null while no run is chosen. */
    val button: String?,
    val canRestore: Boolean,
)

/** What the author picked on the screen (saved across process death by the screen). */
data class RestorePicks(
    val runId: String? = null,
    /** Visible projects the author unchecked. */
    val unchecked: Set<Key> = emptySet(),
    /** The `숨긴 프로젝트 <h>` row unchecked. */
    val hiddenUnchecked: Boolean = false,
    val choice: SameProjectChoice = SameProjectChoice.AS_NEW_PROJECT,
)

/** What `<M월 D일> 백업 되살리기` restores. */
data class RestoreRequest(
    val run: BackupRun,
    val projects: List<BackupEntry>,
    /** The Life Recorder library file of the run (merged by key, R23 §6), or null. */
    val recorder: BackupEntry?,
    val choice: SameProjectChoice,
    /** The 인물 템플릿 files of the run (merged by key, CST-63); the hidden one only with the hidden projects. */
    val templates: List<BackupEntry> = emptyList(),
) {
    /** Locked files need the 복구 암호, asked once (LIB-59). */
    val needsPassword: Boolean get() = projects.any { it.encrypted } || recorder?.encrypted == true || templates.any { it.encrypted }

    val totalBytes: Long get() = projects.sumOf { it.sizeBytes } + (recorder?.sizeBytes ?: 0) + templates.sumOf { it.sizeBytes }

    val hiddenCount: Int get() = projects.count { it.hidden }
}

/** Pure rules of 백업에서 되살리기 (LIB-59, B-08). */
object RestoreModel {
    /** Ok runs of the last 30 days, newest first; the local book and the Drive index merged by id. */
    fun runs(local: List<BackupRun>, drive: DriveRunIndex?, now: Timestamp): List<BackupRun> =
        (local + drive?.runs.orEmpty())
            .filter { it.ok && it.startedAt >= now - BackupPlanner.RETENTION_MS }
            .distinctBy { it.runId }
            .sortedByDescending { it.startedAt }

    /** The chosen run; the newest one when nothing was chosen yet (board: the first row selected). */
    fun selected(runs: List<BackupRun>, picks: RestorePicks): BackupRun? =
        runs.firstOrNull { it.runId == picks.runId } ?: runs.firstOrNull()

    fun build(connected: Boolean, runs: List<BackupRun>, names: Map<Key, String>, picks: RestorePicks, zone: ZoneId): RestoreUi {
        val run = selected(runs, picks)
        val rows = runs.map { r ->
            val row = RestoreRunRow.of(r, zone)
            RestoreRunUi(r.runId, row.title, row.sub, selected = r.runId == run?.runId)
        }
        val projects = run?.let { projectRows(it, names, picks) }.orEmpty()
        val request = run?.let { request(it, picks) }
        return RestoreUi(
            connected = connected,
            runs = rows,
            projects = projects,
            choice = picks.choice,
            button = run?.let { RestoreRunRow.of(it, zone).button },
            canRestore = connected && request != null && (request.projects.isNotEmpty() || request.recorder != null || request.templates.isNotEmpty()),
        )
    }

    private fun projectRows(run: BackupRun, names: Map<Key, String>, picks: RestorePicks): List<RestoreProjectUi> {
        val entries = BackupPlanner.projectEntries(run)
        val visible = entries.filter { !it.hidden }.mapNotNull { e ->
            val key = e.projectKey ?: return@mapNotNull null
            RestoreProjectUi(key, names[key] ?: RestoreCopy.UNNAMED_PROJECT, key !in picks.unchecked)
        }
        val hidden = entries.count { it.hidden }
        return if (hidden > 0) visible + RestoreProjectUi(null, "${RestoreCopy.HIDDEN_PROJECTS} $hidden", !picks.hiddenUnchecked) else visible
    }

    fun request(run: BackupRun, picks: RestorePicks): RestoreRequest {
        val projects = BackupPlanner.projectEntries(run).filter { e ->
            if (e.hidden) !picks.hiddenUnchecked else e.projectKey !in picks.unchecked
        }
        val templates = run.entries.filter { it.kind == BackupKind.TEMPLATES && (!it.hidden || !picks.hiddenUnchecked) }
        return RestoreRequest(run, projects, run.entries.firstOrNull { it.kind == BackupKind.RECORDER }, picks.choice, templates)
    }

    /** Taps on the checklist. */
    fun toggle(picks: RestorePicks, row: RestoreProjectUi): RestorePicks = when (val key = row.key) {
        null -> picks.copy(hiddenUnchecked = !picks.hiddenUnchecked)
        else -> picks.copy(unchecked = if (key in picks.unchecked) picks.unchecked - key else picks.unchecked + key)
    }

    /** Another run: its own checklist starts with everything checked. */
    fun selectRun(picks: RestorePicks, runId: String): RestorePicks =
        if (picks.runId == runId) picks else picks.copy(runId = runId, unchecked = emptySet(), hiddenUnchecked = false)
}
