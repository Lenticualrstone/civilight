package io.github.lenticualrstone.civilight.feature.script.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.ScriptFieldSettings
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TransitionTextSettings
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import kotlinx.serialization.Serializable

/** `ID 형식` (SCR-24): `인물 · 에피소드 · 블록` or `직접` (Q-06 `ED`). */
@Serializable
data class IdFormatSheetKey(val projectKey: Key) : SheetKey

/**
 * 프로젝트 설정 › `스크립트 추가 칸` (SCR-24, SCR-25, board `ProjSettings`): `무대 위치` and `보이스 ID`
 * switches, `ID 형식` with its sample and `바꾸기 ›`. After a format change while IDs are fixed:
 * `고정 ID {n}개는 그대로` and `고정 ID도 새 형식으로`. The read-only example shows the values only.
 * `전환 문구` (SCR-48) opens its own screen (`TransitionTextSettings`).
 */
@Composable
fun ScriptFieldsScreen(route: ScriptFieldSettings, deps: FeatureDeps) {
    val vm = entryViewModel { ScriptFieldsViewModel(route.projectKey, deps) }
    val ui by vm.ui.collectAsState()
    val sheets = rememberSheetHost()
    // The notice belongs to a format change made here (SCR-24), not to every visit.
    var formatChanged by rememberSaveable { mutableStateOf(false) }
    val current = ui
    CivilightScreen(
        title = ScriptCopy.FIELDS_TITLE,
        smallLine = current?.projectName,
        back = IconAction(BACK_LABEL) { deps.navigator.pop() },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is IdFormatSheetKey -> current?.let { state ->
                    {
                        IdFormatSheet(state) { mode ->
                            if (state.choices.none { it.mode == mode && it.selected }) {
                                formatChanged = true
                                vm.setMode(mode)
                            }
                        }
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        if (current == null) return@CivilightScreen
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                SwitchRow(
                    title = ScriptCopy.STAGE_FIELD,
                    subtitle = ScriptCopy.STAGE_SETTING_SUB,
                    checked = current.stageOn,
                    onCheckedChange = vm::setStage,
                    enabled = !current.readOnly,
                )
                SwitchRow(
                    title = ScriptCopy.VOICE_FIELD,
                    subtitle = ScriptCopy.VOICE_SETTING_SUB,
                    checked = current.voiceOn,
                    onCheckedChange = vm::setVoice,
                    enabled = !current.readOnly,
                )
                SettingsRow(
                    title = ScriptCopy.ID_FORMAT,
                    subtitle = current.formatSub,
                    value = ScriptCopy.CHANGE,
                    onClick = { if (!current.readOnly) sheets.open(IdFormatSheetKey(route.projectKey)) },
                )
                // SCR-48: `전환 문구 · 새 씬 기본 없음 · 틀 장소 · 시각 · 바꾸기 ›` (board `ProjSettings`).
                SettingsRow(
                    title = TransitionCopy.TITLE,
                    subtitle = current.transitionSub,
                    value = ScriptCopy.CHANGE,
                    onClick = { deps.navigator.push(TransitionTextSettings(route.projectKey)) },
                )
                if (formatChanged && current.notice.isNotEmpty()) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CivilightText(current.notice.first(), CivilightTheme.type.small, color = CivilightTheme.colors.ink2)
                        if (current.canRefix) {
                            CivilightButton(current.notice.last(), vm::refix, variant = ButtonVariant.GHOST)
                        }
                    }
                }
            }
        }
    }
}

/** The two format choices; picking one applies it at once and closes the sheet. */
@Composable
private fun SheetScope.IdFormatSheet(state: ScriptFieldsUi, onPick: (io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode) -> Unit) {
    SheetHeader(title = ScriptCopy.ID_FORMAT, onClose = ::close)
    Column(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        state.choices.forEach { choice ->
            RadioRow(
                title = choice.title,
                subtitle = choice.subtitle,
                selected = choice.selected,
                enabled = !state.readOnly,
                onClick = {
                    this@IdFormatSheet.close()
                    onPick(choice.mode)
                },
            )
        }
    }
}
