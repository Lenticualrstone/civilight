package io.github.lenticualrstone.civilight.feature.library.help

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.domain.library.onboarding.FirstUseTips
import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsFrame
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsSection
import io.github.lenticualrstone.civilight.navigation.Walkthrough
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 도움말 · 안내 다시 보기 (LIB-21): reads onboarding progress and the tips switch, writes the stores. */
internal class HelpReplayViewModel(private val services: LibraryServices) : ViewModel() {
    private val deps = services.deps
    private val onboarding = deps.stores.appState.onboarding

    val ui: StateFlow<HelpReplayUi> = combine(onboarding.state, deps.settings) { state, settings ->
        HelpReplayState.build(state, settings.firstUseTips)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HelpReplayState.build(onboarding.state.value, deps.settings.value.firstUseTips))

    init {
        viewModelScope.launch { deps.stores.appState.ensureLoaded() }
    }

    /** `다시 보기` of a row: the walkthrough replays now; a module tour runs on its next entry (`ED`). */
    fun replay(row: ReplayRow) {
        val module = row.module
        if (module == null) {
            deps.navigator.push(Walkthrough(1))
            return
        }
        deps.appScope.launch { onboarding.update { ModuleTours.reset(it, module) } }
    }

    fun setTips(on: Boolean) {
        viewModelScope.launch { deps.stores.settings.setFirstUseTips(on) }
    }

    /** `모든 안내 처음부터`: every tour and tip starts over; the example is not reinstalled. */
    fun resetAll() {
        deps.appScope.launch { onboarding.update(FirstUseTips::resetAll) }
    }
}

/**
 * 도움말 · 안내 다시 보기 (LIB-21, board `HelpReplay`; head `설정` / `도움말 · 안내 다시 보기`):
 * `처음 안내` with the walkthrough, `모듈 안내 · 처음 들어갈 때` with the seven tours, the tips
 * switch and `모든 안내 처음부터`. Each row's `다시 보기` is a visible tag.
 */
@Composable
internal fun HelpReplayScreen(services: LibraryServices) {
    val vm = entryViewModel { HelpReplayViewModel(services) }
    val ui by vm.ui.collectAsState()
    SettingsFrame(title = HelpCopy.TITLE, smallLine = HelpCopy.HEAD_SMALL, onBack = { services.deps.navigator.pop() }) {
        SettingsSection(HelpCopy.SECTION_FIRST, first = true)
        ReplayLine(ui.walkthrough, vm::replay)
        SettingsSection(HelpCopy.SECTION_MODULES)
        ui.modules.forEach { ReplayLine(it, vm::replay) }
        SwitchRow(title = HelpCopy.TIPS, checked = ui.tipsOn, onCheckedChange = vm::setTips, subtitle = HelpCopy.TIPS_SUB)
        CivilightButton(HelpCopy.RESET_ALL, vm::resetAll, Modifier.fillMaxWidth().padding(top = 16.dp), variant = ButtonVariant.GHOST)
    }
}

@Composable
private fun ReplayLine(row: ReplayRow, onReplay: (ReplayRow) -> Unit) {
    ListRow(
        title = row.title,
        subtitle = row.sub,
        compact = true,
        trailing = { FilterChip(HelpCopy.REPLAY, selected = false, onClick = { onReplay(row) }) },
    )
}
