package io.github.lenticualrstone.civilight.feature.library

import android.content.Context

import io.github.lenticualrstone.civilight.feature.library.backup.BackupServices
import io.github.lenticualrstone.civilight.feature.library.common.ProjectWriter
import io.github.lenticualrstone.civilight.feature.library.common.ProtectionFacts
import io.github.lenticualrstone.civilight.feature.library.common.SessionProjectWriter
import io.github.lenticualrstone.civilight.feature.library.export.ExportDrafts
import io.github.lenticualrstone.civilight.feature.library.export.SaveDialog
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectDraftHolder
import io.github.lenticualrstone.civilight.feature.library.onboarding.OnboardingActions
import io.github.lenticualrstone.civilight.feature.library.protection.AppProtectionPorts
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionActions
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionCopy
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionOutcome
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.security.AuthAvailability

/**
 * What every screen of this module shares, made once per activity in [libraryEntries] (in-memory
 * objects only; no I/O at creation): the shell's [deps], the one write path for projects that are
 * not the current screen's, the protection rules, the facts of later batches, the 새 프로젝트 draft
 * that its four step entries share and the open 내보내기 sheets' drafts (passwords stay in memory).
 */
class LibraryServices(val deps: FeatureDeps) {
    val writer: ProjectWriter = SessionProjectWriter(deps.projects)
    val protection = ProtectionActions(AppProtectionPorts(writer, deps.biometric, deps.stores.appState, deps.unlock))
    /** Drive backup, 복구 암호 and restore (LIB-57 … LIB-60, batch 5). */
    val backups = BackupServices(deps)

    /** 복구 암호 set and unencrypted Drive versions, from the backup book (batch 5; was `NoBackupYet`). */
    val facts: ProtectionFacts = backups.facts
    val draft = NewProjectDraftHolder()
    val exports = ExportDrafts()
    val onboarding = OnboardingActions(deps)

    @Volatile private var appContext: Context? = null

    /** The application context, given by the first screen of this lane that composes (for `writtenBy`). */
    fun attach(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    /** `Civilight <versionName> (Android)` for files this lane writes; without a context yet, no version. */
    fun writtenBy(): String = appContext?.let(SaveDialog::writtenBy) ?: "Civilight (Android)"

    /** Whether the device has a secure lock screen; hiding needs one (§6 Q19 `ED`). */
    fun deviceSecure(): Boolean = deps.biometric.availability() != AuthAvailability.NONE

    /** Hiding was refused for want of a device lock: say so once (copy not in sources). */
    fun showNoDeviceLock() = deps.toasts.show(ProtectionCopy.TODO_COPY_LIB31_NO_DEVICE_LOCK, actionLabel = null)

    /** The one user-visible consequence of a protection outcome; cancel and success stay silent (LIB-37). */
    fun report(outcome: ProtectionOutcome) {
        if (outcome == ProtectionOutcome.NO_DEVICE_LOCK) showNoDeviceLock()
    }
}
