package io.github.lenticualrstone.civilight.domain.library.backup

import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.BackupEntry
import io.github.lenticualrstone.civilight.model.app.BackupKind
import io.github.lenticualrstone.civilight.model.app.BackupRun
import io.github.lenticualrstone.civilight.model.app.LockKind

/** A project considered for the daily backup. */
data class BackupCandidate(
    val project: ProjectProfile,
    /** Live hidden items in the project (`숨긴 항목 <n>`). */
    val hiddenItemCount: Int,
    /** Last change of the project's content. */
    val updatedAt: Timestamp,
)

enum class BackupAction {
    /** Upload a new version. */
    UPLOAD,

    /** Unchanged since its last upload: the run index references the previous version. */
    REUSE,

    /** Hidden project without 복구 암호, or `숨긴 것도 백업` off. */
    EXCLUDED_HIDDEN,

    /** Example or trashed project: never uploaded. */
    SKIPPED,
}

data class BackupItem(
    val projectKey: Key,
    val action: BackupAction,
    /** Lock of the uploaded (or reused) version; null = unencrypted. */
    val lock: LockKind?,
    /** The version referenced when reused. */
    val previous: BackupEntry?,
)

/** One run's decisions (LIB-57, R19 §5). */
data class BackupPlan(val items: List<BackupItem>, val hiddenItemsUnencrypted: Int) {
    fun of(projectKey: Key): BackupItem? = items.firstOrNull { it.projectKey == projectKey }

    val uploads: List<BackupItem> get() = items.filter { it.action == BackupAction.UPLOAD }
    val hiddenExcluded: Int get() = items.count { it.action == BackupAction.EXCLUDED_HIDDEN }

    /** 설정 › 보관 warnings: `숨긴 프로젝트 <n> · 백업 안 됨`, ◇ `숨긴 항목 <n> · 암호 없이 백업 · 복구 암호 정하기 ›`. */
    val warnings: List<String>
        get() = buildList {
            if (hiddenExcluded > 0) add("숨긴 프로젝트 $hiddenExcluded · 백업 안 됨")
            if (hiddenItemsUnencrypted > 0) add("숨긴 항목 $hiddenItemsUnencrypted · 암호 없이 백업 · 복구 암호 정하기 ›")
        }
}

/**
 * The daily Drive backup decisions (LIB-57):
 * - hidden projects: locked with the 복구 암호; excluded when none is set or `숨긴 것도 백업` is off;
 * - projects with hidden items: locked with the 복구 암호 when set (`ED`, whole file, §6 Q5), else
 *   uploaded unencrypted with a ◇ warning; never excluded;
 * - R-18 projects that are not hidden: unencrypted (§6 Q29);
 * - example and trashed projects are not uploaded; unchanged projects reuse their last version.
 */
object BackupPlanner {
    fun plan(
        candidates: List<BackupCandidate>,
        settings: AppSettings,
        recoverySet: Boolean,
        lastUploads: Map<Key, Pair<Timestamp, BackupEntry>> = emptyMap(),
    ): BackupPlan {
        var unencryptedHiddenItems = 0
        val items = candidates.map { c ->
            val key = c.project.key
            when {
                c.project.isExample || c.project.trashed -> BackupItem(key, BackupAction.SKIPPED, null, null)
                c.project.hidden && (!recoverySet || !settings.backupHidden) -> BackupItem(key, BackupAction.EXCLUDED_HIDDEN, null, null)
                else -> {
                    val lock = if (recoverySet && (c.project.hidden || c.hiddenItemCount > 0)) LockKind.RECOVERY else null
                    if (lock == null && c.hiddenItemCount > 0) unencryptedHiddenItems += c.hiddenItemCount
                    val last = lastUploads[key]
                    val unchanged = last != null && c.updatedAt <= last.first && last.second.lockType == lock
                    if (unchanged) BackupItem(key, BackupAction.REUSE, lock, last!!.second) else BackupItem(key, BackupAction.UPLOAD, lock, null)
                }
            }
        }
        return BackupPlan(items, unencryptedHiddenItems)
    }

    /**
     * Versions to delete (B-07): every project/recorder/settings version not referenced by any run
     * that started within the last 30 days.
     */
    fun prune(runs: List<BackupRun>, now: Timestamp): List<BackupEntry> {
        val cutoff = now - RETENTION_MS
        val keep = runs.filter { it.startedAt >= cutoff }.flatMap { it.entries }.map { it.driveFileId to it.versionId }.toSet()
        return runs.filter { it.startedAt < cutoff }
            .flatMap { it.entries }
            .filter { (it.driveFileId to it.versionId) !in keep }
            .distinctBy { it.driveFileId to it.versionId }
    }

    const val RETENTION_MS: Long = 30L * 24 * 60 * 60 * 1000

    /** The project entries of a run. */
    fun projectEntries(run: BackupRun): List<BackupEntry> = run.entries.filter { it.kind == BackupKind.PROJECT }
}
