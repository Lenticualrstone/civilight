package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ErrorBanner
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.domain.library.backup.BackupFailureBanner
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsLinkRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.onboarding.DriveConnect
import io.github.lenticualrstone.civilight.feature.library.settings.LinkRow
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsFrame
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsSection
import io.github.lenticualrstone.civilight.feature.library.settings.SwitchSettingRow
import io.github.lenticualrstone.civilight.navigation.RecoveryPassword
import io.github.lenticualrstone.civilight.navigation.RestoreBackup
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 설정 › 보관: the switch, the warnings of the next run, and the rows to LIB-58 / LIB-59. */
internal class BackupSettingsViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val backups = services.backups
    private val warnings = MutableStateFlow<List<String>>(emptyList())
    private val storage = MutableStateFlow<StorageFacts?>(null)

    /** A run is going (start-up or `지금 다시 시도`): the banner's buttons wait. */
    val running: StateFlow<Boolean> = backups.running

    val ui: StateFlow<BackupSettingsUi> = combine(deps.settings, backups.book.state, warnings, storage) { settings, book, w, size ->
        BackupSettingsState.build(settings, book, w, deps.clock.now(), deps.clock.zone, size)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BackupSettingsState.build(deps.settings.value, backups.book.state.value, emptyList(), deps.clock.now(), deps.clock.zone),
    )

    init {
        viewModelScope.launch { storage.value = runCatching { backups.storage.read() }.getOrNull() }
        viewModelScope.launch {
            backups.book.ensureLoaded()
            combine(deps.settings, deps.stores.library.state, backups.book.state) { s, _, b -> s to (b.recovery != null) }
                .collect { (settings, recoverySet) ->
                    val docs = withContext(Dispatchers.IO) { StoreBackupSources(deps).projects().map { it.doc } }
                    warnings.value = BackupSettingsState.warnings(docs, settings, recoverySet)
                }
        }
    }

    /**
     * The switch: on signs in first when no account is connected (needsExternal; the stub says it is
     * not available and the switch stays off); off keeps the account for the next time.
     */
    fun setDrive(on: Boolean) {
        viewModelScope.launch {
            val settings = deps.stores.settings
            if (!on) {
                settings.setDriveBackup(false, deps.settings.value.driveAccountId)
                return@launch
            }
            val account = backups.drive.account()
            if (account != null) {
                settings.setDriveBackup(true, account)
            } else if (backups.connect() == DriveConnect.Unavailable) {
                deps.toasts.show(BackupCopy.DRIVE_NOT_READY, actionLabel = null)
            }
        }
    }

    /**
     * `다시 로그인` (LIB-61): sign in again, then run at once (`ED`) so the banner clears when Drive
     * answers. Runs in the app scope, so leaving the screen does not cut the run short.
     */
    fun relogin() {
        if (running.value) return
        deps.appScope.launch {
            when (backups.connect()) {
                is DriveConnect.Connected -> report(backups.runNow(services.writtenBy()))
                DriveConnect.Unavailable -> deps.toasts.show(BackupCopy.DRIVE_NOT_READY, actionLabel = null)
                else -> Unit // cancelled sign-in: nothing changes, silently (LIB-37)
            }
        }
    }

    /** `지금 다시 시도` (LIB-61): one run now; a new failure keeps the banner (its reason updates). */
    fun retry() {
        if (running.value) return
        deps.appScope.launch { report(backups.runNow(services.writtenBy())) }
    }

    private fun report(outcome: BackupOutcome) {
        if (outcome == BackupOutcome.NotRunning) deps.toasts.show(BackupCopy.DRIVE_NOT_READY, actionLabel = null)
    }
}

/**
 * 설정 › 보관 (route `SettingsBackup`; board `ErrBackup`): the LIB-61 failure banner with `다시 로그인` ·
 * `지금 다시 시도` under it, then `보관`: Drive switch, `기기 저장 공간` (LIB-62, sizes only), `백업 기록 ›`,
 * `복구 암호 ›` and the warnings.
 */
@Composable
internal fun BackupSettingsScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { BackupSettingsViewModel(services) }
    val ui by vm.ui.collectAsState()
    val running by vm.running.collectAsState()
    SettingsFrame(title = BackupCopy.TITLE, smallLine = BackupCopy.SMALL, onBack = { navigator.pop() }) {
        FoldRevealValue(ui.banner) { banner ->
            FailureBanner(banner, enabled = !running, onRelogin = vm::relogin, onRetry = vm::retry)
        }
        SettingsSection(BackupCopy.SECTION, first = ui.banner == null)
        SwitchSettingRow(ui.drive, vm::setDrive)
        InfoRow(ui.storage)
        LinkRow(ui.history) { navigator.push(RestoreBackup) }
        LinkRow(ui.recovery) { navigator.push(RecoveryPassword) }
        ui.warnings.forEach { warning ->
            WarningLine(warning, onClick = if (warning.toRecovery) ({ navigator.push(RecoveryPassword) }) else null)
        }
    }
}

/**
 * LIB-61 / LIB-71 banner form (board `ErrBackup`): the DS error card (`r18` outline, warning icon,
 * title, sub), then the two buttons 10 dp under the card, primary and ghost at equal width.
 */
@Composable
private fun FailureBanner(banner: BackupFailureBanner, enabled: Boolean, onRelogin: () -> Unit, onRetry: () -> Unit) {
    ErrorBanner(title = banner.title, subtitle = banner.sub)
    ButtonRow(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
        CivilightButton(banner.buttons[0], onRelogin, Modifier.weight(1f), enabled = enabled)
        CivilightButton(banner.buttons[1], onRetry, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = enabled)
    }
}

/** A `title · sub` row with nothing to open (LIB-62 `기기 저장 공간`: no detail screen is drawn). */
@Composable
private fun InfoRow(row: SettingsLinkRow) {
    ListRow(title = row.title, subtitle = row.sub, compact = true)
}

/** `숨긴 프로젝트 n · 백업 안 됨`, or ◇ `숨긴 항목 n · 암호 없이 백업 · 복구 암호 정하기 ›` (opens LIB-58). */
@Composable
private fun WarningLine(warning: BackupWarning, onClick: (() -> Unit)?) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (warning.toRecovery) CivilightText("◇", type.small, color = colors.ink2)
        CivilightText(warning.text, type.small, color = if (warning.toRecovery) colors.ink2 else colors.warn)
    }
}
