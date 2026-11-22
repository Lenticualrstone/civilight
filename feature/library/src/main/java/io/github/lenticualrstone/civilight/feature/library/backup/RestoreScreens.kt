package io.github.lenticualrstone.civilight.feature.library.backup

import androidx.compose.foundation.layout.Column
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.onboarding.DriveConnect
import io.github.lenticualrstone.civilight.feature.library.settings.BottomButton
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsFrame
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.BackupRun
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** The 복구 암호 sheet of 백업에서 되살리기 (feature-local; the LIB-54 field with the `복구 암호` label). */
@Serializable
internal data object RestorePasswordSheet : SheetKey

/** 백업에서 되살리기: the runs (local book + Drive index), the author's picks, the password check. */
internal class RestoreViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val backups = services.backups
    private val _connected = MutableStateFlow(false)
    private val _runs = MutableStateFlow<List<BackupRun>>(emptyList())
    private val _names = MutableStateFlow<Map<Key, String>>(emptyMap())
    private val _passwordError = MutableStateFlow<String?>(null)
    private val _checking = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()
    val runs: StateFlow<List<BackupRun>> = _runs.asStateFlow()
    val names: StateFlow<Map<Key, String>> = _names.asStateFlow()
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        backups.book.ensureLoaded()
        val account = runCatching { backups.drive.account() }.getOrNull()
        _connected.value = account != null
        val index = if (account != null) runCatching { backups.drive.readIndex() }.getOrNull() else null
        _runs.value = RestoreModel.runs(backups.book.state.value.runs, index, deps.clock.now())
        _names.value = index?.projectNames.orEmpty()
    }

    /** `연결` while not connected (needsExternal: the stub answers that it is not available). */
    fun connect() {
        viewModelScope.launch {
            if (backups.connect() is DriveConnect.Unavailable) {
                deps.toasts.show(BackupCopy.DRIVE_NOT_READY, actionLabel = null)
            }
            load()
        }
    }

    /** `<M월 D일> 백업 되살리기` without locked files: start at once. */
    fun start(request: RestoreRequest) {
        backups.restore.start(request, null, onFirstRunDone = services.onboarding::finish)
    }

    /** `열기` on the 복구 암호 sheet: a wrong password keeps the sheet with its error. */
    fun startWithPassword(request: RestoreRequest, password: String, onStarted: () -> Unit) {
        if (_checking.value || password.isEmpty()) return
        _checking.value = true
        _passwordError.value = null
        viewModelScope.launch {
            val chars = password.toCharArray()
            try {
                if (backups.restore.checkPassword(request, chars)) {
                    onStarted()
                    backups.restore.start(request, chars, onFirstRunDone = services.onboarding::finish)
                } else {
                    _passwordError.value = RestoreCopy.WRONG_PASSWORD
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Drive could not be reached: nothing started; the sheet stays for another try.
                deps.toasts.show(RestoreCopy.PARTLY_FAILED, actionLabel = null)
            } finally {
                chars.fill('\u0000')
                _checking.value = false
            }
        }
    }

    fun clearError() {
        _passwordError.value = null
    }
}

/**
 * 백업에서 되살리기 (route `RestoreBackup`, LIB-59, board `Restore`): `Google Drive · 계정 연결됨`,
 * `백업 · 30일` runs (the chosen one `sel` with the left accent bar), the chosen run's projects
 * (§6 Q25 `ED` checklist), `같은 이름이 이미 있을 때` segmented, the note and the bottom
 * `<M월 D일> 백업 되살리기`. Locked files ask for the 복구 암호 once.
 */
@Composable
internal fun RestoreBackupScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { RestoreViewModel(services) }
    val connected by vm.connected.collectAsState()
    val runs by vm.runs.collectAsState()
    val names by vm.names.collectAsState()
    var runId by rememberSaveable { mutableStateOf<String?>(null) }
    var unchecked by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var hiddenUnchecked by rememberSaveable { mutableStateOf(false) }
    var overwrite by rememberSaveable { mutableStateOf(false) }
    val picks = RestorePicks(runId, unchecked.toSet(), hiddenUnchecked, if (overwrite) SameProjectChoice.OVERWRITE else SameProjectChoice.AS_NEW_PROJECT)
    val ui = RestoreModel.build(connected, runs, names, picks, services.deps.clock.zone)
    val request = RestoreModel.selected(runs, picks)?.let { RestoreModel.request(it, picks) }
    val sheets = rememberSheetHost()
    fun apply(next: RestorePicks) {
        runId = next.runId
        unchecked = next.unchecked.toList()
        hiddenUnchecked = next.hiddenUnchecked
    }

    SettingsFrame(
        title = RestoreCopy.TITLE,
        smallLine = RestoreCopy.SMALL,
        onBack = { navigator.pop() },
        bottomButton = ui.button?.let { label ->
            BottomButton(label, enabled = ui.canRestore && !services.backups.restore.running) {
                val r = request ?: return@BottomButton
                if (r.needsPassword) sheets.open(RestorePasswordSheet) else vm.start(r)
            }
        },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                RestorePasswordSheet -> { { RestorePasswordContent(vm, request) } }
                else -> null
            }
        },
    ) {
        AccountLine(connected, onConnect = vm::connect)
        SectionLabel(RestoreCopy.RUNS, Modifier.padding(top = 18.dp, bottom = 6.dp))
        FoldReveal(ui.runs.isEmpty()) { CivilightText(RestoreCopy.NO_RUNS, CivilightTheme.type.small, color = CivilightTheme.colors.ink3) }
        ui.runs.forEach { row ->
            ListRow(
                title = row.title,
                subtitle = row.sub,
                selected = row.selected,
                onClick = { apply(RestoreModel.selectRun(picks, row.runId)) },
                trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = CivilightTheme.colors.ink3) },
            )
        }
        FoldRevealValue(ui.projects.takeIf { it.isNotEmpty() }) { projects ->
            Column {
                SectionLabel(RestoreCopy.PROJECTS, Modifier.padding(top = 22.dp, bottom = 6.dp))
                projects.forEach { row ->
                    CheckboxRow(label = row.label, checked = row.checked, onCheckedChange = { apply(RestoreModel.toggle(picks, row)) })
                }
            }
        }
        SectionLabel(RestoreCopy.SAME_NAME, Modifier.padding(top = 22.dp, bottom = 8.dp))
        SegmentedControl(
            options = listOf(RestoreCopy.AS_NEW, RestoreCopy.OVERWRITE),
            selectedIndex = if (overwrite) 1 else 0,
            onSelect = { overwrite = it == 1 },
        )
        CivilightText(RestoreCopy.NOTE, CivilightTheme.type.small, Modifier.padding(top = 12.dp), color = CivilightTheme.colors.ink3)
    }
}

/** Cloud icon + `Google Drive · 계정 연결됨`, or the sign-in line and `연결` (copy not in sources). */
@Composable
private fun AccountLine(connected: Boolean, onConnect: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CivilightIcon(IconId.Cloud, contentDescription = null, tint = colors.ink2)
        ValueSwap(if (connected) RestoreCopy.CONNECTED else RestoreCopy.NOT_CONNECTED, CivilightTheme.type.small, Modifier.weight(1f), color = colors.ink3)
        FoldReveal(!connected, horizontal = true) { CivilightButton(RestoreCopy.CONNECT, onClick = onConnect, variant = ButtonVariant.COMPACT_PRIMARY) }
    }
}

/** The 복구 암호 sheet: one field (label `복구 암호`, LIB-54), `취소` · `열기`; asked once per restore. */
@Composable
private fun SheetScope.RestorePasswordContent(vm: RestoreViewModel, request: RestoreRequest?) {
    var password by remember { mutableStateOf("") }
    val error by vm.passwordError.collectAsState()
    val checking by vm.checking.collectAsState()
    val sheet = this
    LaunchedEffect(Unit) { vm.clearError() }
    SheetHeader(RestoreCopy.PASSWORD_TITLE, onClose = sheet::close)
    PasswordInput(RestoreCopy.PASSWORD_FIELD, password, { password = it }, Modifier.padding(horizontal = 20.dp), error = error)
    CivilightText(RestoreCopy.NOTE, CivilightTheme.type.small, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = CivilightTheme.colors.ink3)
    SheetActions {
        CivilightButton(RestoreCopy.CANCEL, onClick = sheet::close, modifier = Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            RestoreCopy.OPEN,
            onClick = { request?.let { vm.startWithPassword(it, password) { sheet.close() } } },
            modifier = Modifier.weight(1f),
            enabled = password.isNotEmpty() && !checking && request != null,
        )
    }
}
