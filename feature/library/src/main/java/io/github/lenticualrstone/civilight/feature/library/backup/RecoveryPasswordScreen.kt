package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.KeyValueLine
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.settings.BottomButton
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsFrame
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 복구 암호 정하기: the counts it shows and `정하기` (PBKDF2 off the main thread, then back). */
internal class RecoveryPasswordViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val _hidden = MutableStateFlow(0)
    private val _visible = MutableStateFlow<List<ProjectDoc>>(emptyList())
    private val _busy = MutableStateFlow(false)
    val hiddenProjects: StateFlow<Int> = _hidden.asStateFlow()
    val visibleDocs: StateFlow<List<ProjectDoc>> = _visible.asStateFlow()
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        viewModelScope.launch {
            deps.stores.library.ensureLoaded()
            val live = deps.stores.library.state.value.live.filter { !it.isExample }
            _hidden.value = live.count { it.hidden }
            _visible.value = withContext(Dispatchers.IO) {
                live.filter { !it.hidden }.mapNotNull { e -> deps.projects.peekDoc(e.key) ?: runCatching { deps.stores.library.readDoc(e.key) }.getOrNull() }
            }
        }
    }

    /** `정하기`: a new key locked by [password] (zeroed here); the page closes when it is stored. */
    fun set(password: CharArray) {
        if (_busy.value) {
            password.fill('\u0000')
            return
        }
        _busy.value = true
        viewModelScope.launch {
            try {
                services.backups.keyring.set(password, deps.clock.now())
                deps.navigator.pop()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The Keystore or the disk refused: nothing was stored; the page stays.
                deps.toasts.show(RecoveryCopy.SET_FAILED, actionLabel = null)
            } finally {
                password.fill('\u0000')
                _busy.value = false
            }
        }
    }
}

/**
 * 복구 암호 정하기 (route `RecoveryPassword`, LIB-58, board `RHRecoveryPw`): the lock card, `복구 암호`
 * with the 4-bar meter, `한 번 더`, each with its eye button, the ◇ warning, `함께 잠금`, and the bottom
 * `정하기`. The typed text stays in memory only (never saved state).
 */
@Composable
internal fun RecoveryPasswordScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { RecoveryPasswordViewModel(services) }
    val hidden by vm.hiddenProjects.collectAsState()
    val visible by vm.visibleDocs.collectAsState()
    val busy by vm.busy.collectAsState()
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val ui = RecoveryPasswordState.build(password.toCharArray(), repeat.toCharArray(), hidden, visible)
    SettingsFrame(
        title = RecoveryCopy.TITLE,
        smallLine = RecoveryCopy.SMALL,
        onBack = { navigator.pop() },
        bottomButton = BottomButton(RecoveryCopy.SET, enabled = ui.canSet && !busy) { vm.set(password.toCharArray()) },
    ) {
        LockCard(ui.cardTitle)
        PasswordInput(RecoveryCopy.FIELD, password, { password = it }, Modifier.padding(top = 16.dp))
        FoldRevealValue(ui.strength) { StrengthMeter(it) }
        PasswordInput(RecoveryCopy.REPEAT, repeat, { repeat = it }, Modifier.padding(top = 16.dp), error = ui.mismatch)
        WarningBlock()
        SectionLabel(RecoveryCopy.LOCKED_TOGETHER, Modifier.padding(top = 24.dp, bottom = 8.dp))
        ui.rows.forEach { row -> KeyValueLine(row.label, row.value) }
    }
}

/** The card: 40 dp lock tile, `숨긴 프로젝트 <n>` (700 16 / 22) and `이 암호로 잠금 · Google Drive 자동 백업`. */
@Composable
private fun LockCard(title: String) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.raise), contentAlignment = Alignment.Center) {
            CivilightIcon(IconId.Lock, contentDescription = null, tint = colors.ink)
        }
        Column(Modifier.weight(1f)) {
            CivilightText(title, type.itemTitle, color = colors.ink)
            CivilightText(RecoveryCopy.CARD_SUB, type.small, color = colors.ink3)
        }
    }
}

/** A password field with its eye button (`보이기`; the board draws it inside the field's end). */
@Composable
internal fun PasswordInput(label: String?, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, error: String? = null) {
    var shown by remember { mutableStateOf(false) }
    val eye by animatedColor(if (shown) CivilightTheme.colors.acc else CivilightTheme.colors.ink2)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        CivilightTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            label = label,
            error = error,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation('•'),
        )
        CivilightIconButton(
            icon = IconId.EyeOff,
            contentDescription = if (shown) RecoveryCopy.HIDE else RecoveryCopy.SHOW,
            onClick = { shown = !shown },
            modifier = Modifier.padding(bottom = if (error != null) 24.dp else 3.dp),
            tint = eye,
        )
    }
}

/** Four 28 × 4 bars (lit `acc`, others `line`) and `강도 · 좋음 · 14자`. */
@Composable
private fun StrengthMeter(strength: StrengthUi) {
    val colors = CivilightTheme.colors
    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { i ->
                // Lit bars and unlit bars trade colour on `standard` (S6), so a longer password fills the meter.
                val bar by animatedColor(if (i < strength.bars) colors.acc else colors.line)
                Box(Modifier.width(28.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(bar))
            }
        }
        ValueSwap(strength.line, CivilightTheme.type.small, color = colors.ink3)
    }
}

/** ◇ `잊으면 숨긴 프로젝트 백업을 열 수 없음` · `앱 · 서버에 저장 안 함 · 찾아 줄 수 없음`. */
@Composable
private fun WarningBlock() {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CivilightText("◇", type.body, color = colors.ink2)
        Column(Modifier.weight(1f)) {
            CivilightText(RecoveryCopy.WARNING, type.itemTitle, color = colors.ink)
            CivilightText(RecoveryCopy.WARNING_SUB, type.small, color = colors.ink3)
        }
    }
}
