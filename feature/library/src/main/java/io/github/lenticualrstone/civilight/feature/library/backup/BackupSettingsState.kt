package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.backup.BackupCandidate
import io.github.lenticualrstone.civilight.domain.library.backup.BackupFailureBanner
import io.github.lenticualrstone.civilight.domain.library.backup.BackupPlanner
import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsLinkRow
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsSwitchRow
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import java.time.ZoneId

/** A warning under the 보관 rows; [toRecovery] = it ends in `복구 암호 정하기 ›` and opens LIB-58. */
data class BackupWarning(val text: String, val toRecovery: Boolean)

/**
 * 설정 › 보관 (board `ErrBackup`): the LIB-61 failure [banner] on top, then the LIB-57 switch, the LIB-62
 * [storage] row (its sub empty until the sizes are read), `백업 기록` (LIB-59), `복구 암호` (LIB-58) and
 * the warnings of the next run.
 */
data class BackupSettingsUi(
    val banner: BackupFailureBanner?,
    val drive: SettingsSwitchRow,
    val storage: SettingsLinkRow,
    val history: SettingsLinkRow,
    val recovery: SettingsLinkRow,
    val warnings: List<BackupWarning>,
)

/** Pure builder of 설정 › 보관. */
object BackupSettingsState {
    /**
     * While a failure streak runs (LIB-61, B-09, B-10) the banner shows and the Drive row reads
     * `마지막 성공 <M월 D일 HH:mm>` (or the plain sub when nothing ever succeeded); otherwise
     * `마지막 백업 <relative time>`.
     */
    fun build(
        settings: AppSettings,
        book: BackupBook,
        warnings: List<String>,
        now: Timestamp,
        zone: ZoneId,
        storage: StorageFacts? = null,
    ): BackupSettingsUi {
        val last = book.state.lastSuccessAt
        // `ED`: with the switch off nothing runs, so no failure is reported (as the warnings).
        val banner = if (settings.driveBackup) BackupFailureBanner.of(book.state, now, zone) else null
        val driveSub = when {
            banner != null -> banner.driveRow ?: BackupCopy.DRIVE_SUB
            last != null -> "${BackupCopy.LAST_BACKUP} ${TimeLabels.relative(last, now, zone)}"
            else -> BackupCopy.DRIVE_SUB
        }
        return BackupSettingsUi(
            banner = banner,
            drive = SettingsSwitchRow(title = BackupCopy.DRIVE, sub = driveSub, on = settings.driveBackup),
            // `ED`: the detail screen is not drawn (LIB-62, P2), so the row only shows the sizes.
            storage = SettingsLinkRow(BackupCopy.STORAGE, storage?.line, null),
            history = SettingsLinkRow(BackupCopy.HISTORY, BackupCopy.HISTORY_SUB, null),
            recovery = SettingsLinkRow(
                title = BackupCopy.RECOVERY,
                sub = if (book.recovery != null) "${BackupCopy.RECOVERY_SUB} · ${BackupCopy.RECOVERY_SET}" else BackupCopy.RECOVERY_SUB,
                value = if (book.recovery != null) BackupCopy.RECOVERY_CHANGE else BackupCopy.RECOVERY_DEFINE,
            ),
            // `ED`: the warnings say what the next run would do, so they show while the backup is on.
            warnings = if (settings.driveBackup) warnings.map { BackupWarning(it, toRecovery = it.endsWith("›")) } else emptyList(),
        )
    }

    /** `숨긴 프로젝트 <n> · 백업 안 됨` and ◇ `숨긴 항목 <n> · 암호 없이 백업 · …` from the next run's plan. */
    fun warnings(docs: List<ProjectDoc>, settings: AppSettings, recoverySet: Boolean, templates: List<FieldTemplate> = emptyList()): List<String> =
        BackupPlanner.plan(docs.map { BackupCandidate.of(it) }, settings, recoverySet).warnings +
            TemplateBackup.plan(templates, settings, recoverySet).warnings
}
