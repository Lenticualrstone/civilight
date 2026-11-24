package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import androidx.compose.runtime.remember
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * 설정 (LIB-63, board `Settings`; head `Civilight` / `설정`). Sections `화면` (theme segmented, 글자
 * 크기, 움직임 · 투명도, 지도 · 이미지 · 연표), `보관` (Drive, 복구 암호) and `보안` (앱
 * 잠금, 숨기기), then the rows the board does not draw (§6 Q27 `ED`): 휴지통, 라이프 레코더, 도움말 ·
 * 안내 다시 보기. Each row's route is [SettingsRoutes.routeOf] (CNV-18 row map).
 */
@Composable
internal fun SettingsScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.root.collectAsState()
    val open: (SettingsRootRow) -> Unit = { row -> navigator.push(SettingsRoutes.routeOf(row)) }
    SettingsFrame(
        title = SettingsCopy.TITLE,
        smallLine = SettingsCopy.SMALL,
        onBack = { navigator.pop() },
    ) {
        SettingsSection(SettingsCopy.SECTION_SCREEN, first = true)
        ThemeSegmented(ui.themeIndex, vm::setTheme, Modifier.padding(top = 4.dp, bottom = 6.dp))
        LinkRow(ui.textSize) { open(SettingsRootRow.TEXT_SIZE) }
        // (0.2.3, r3) `펼친 화면 배치` is gone: inner screens always draw two panes with a closable side panel.
        // (0.2.0, LIB-94) After `글자 크기`: `서재 보기`, `카드 보기`, then the tilt switch, each drawn
        // only while a 3D view it would change has a renderer (no control that does nothing).
        ui.libraryView?.let { row -> LinkRow(row) { navigator.push(ViewKind.LIBRARY.route) } }
        ui.cardView?.let { row -> LinkRow(row) { navigator.push(ViewKind.CARD.route) } }
        // Hidden under reduced motion: tilt stops then whatever the switch says.
        ViewStyles.tiltShown(ui.tilt, CivilightTheme.motion.reduced)?.let { row -> SwitchSettingRow(row, vm::setViewTilt) }
        LinkRow(ui.motion) { open(SettingsRootRow.MOTION) }
        LinkRow(ui.more) { open(SettingsRootRow.MORE) }

        SettingsSection(SettingsCopy.SECTION_KEEP)
        // §6 Q27 `ED`: the 보관 rows open 설정 › 보관; 복구 암호 opens its own screen (LIB-58).
        SwitchLinkRow(ui.drive) { open(SettingsRootRow.DRIVE) }
        LinkRow(ui.recovery) { open(SettingsRootRow.RECOVERY) }

        SettingsSection(SettingsCopy.SECTION_SECURITY)
        SwitchSettingRow(ui.appLock, vm::setAppLock)
        LinkRow(ui.hiding) { open(SettingsRootRow.HIDING) }

        SettingsSection(SettingsCopy.SECTION_CAST)
        LinkRow(ui.templates) { open(SettingsRootRow.TEMPLATES) }

        Spacer(Modifier.height(22.dp))
        SettingsRow(title = SettingsCopy.TRASH, onClick = { open(SettingsRootRow.TRASH) })
        SettingsRow(title = SettingsCopy.RECORDER, onClick = { open(SettingsRootRow.RECORDER) })
        SettingsRow(title = SettingsCopy.HELP, onClick = { open(SettingsRootRow.HELP) })
    }
}

/**
 * (0.2.0, LIB-94) 설정 › 서재 보기 / 카드 보기 (routes `SettingsLibraryView`, `SettingsCardView`): head `설정` /
 * the list's name; one radio per value that can take effect; a pick applies at once and goes back.
 */
@Composable
internal fun ViewStyleScreen(kind: ViewKind, services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { SettingsViewModel(services) }
    val settings by services.deps.settings.collectAsState()
    // (0.2.3, r3 `viewpick`) 서재 보기 is the view picker with its live preview, over the current view (inert).
    // 카드 보기 keeps the radio list: the card views are drawn by feature:cast, which this module cannot preview.
    if (kind == ViewKind.LIBRARY) {
        LibraryViewPickScreen(services, io.github.lenticualrstone.civilight.model.app.ShownView.library(settings), onApply = { style ->
            vm.chooseView(kind, style.name)
            navigator.pop()
        }, onCancel = { navigator.pop() })
        return
    }
    val options = remember(settings, kind) { ViewStyles.options(kind, settings) }
    SettingsFrame(
        title = ViewStyles.title(kind),
        smallLine = SettingsCopy.TITLE,
        onBack = { navigator.pop() },
    ) {
        options.forEach { option ->
            RadioRow(
                title = option.title,
                selected = option.selected,
                onClick = {
                    vm.chooseView(kind, option.id)
                    navigator.pop()
                },
            )
        }
    }
}

/** The picker's own masked 서재 (no recents recording, no onboarding clean-up: see [shelfUiFlow]). */
internal class ViewPickViewModel(services: LibraryServices) : androidx.lifecycle.ViewModel() {
    private val docs = io.github.lenticualrstone.civilight.feature.library.common.LibraryDocs(services.deps.stores.library, viewModelScope)
    val state: kotlinx.coroutines.flow.StateFlow<io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi> =
        io.github.lenticualrstone.civilight.feature.library.shelf.shelfUiFlow(services, docs)
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi())
}

@Composable
private fun LibraryViewPickScreen(
    services: LibraryServices,
    stored: io.github.lenticualrstone.civilight.model.app.LibraryStyle,
    onApply: (io.github.lenticualrstone.civilight.model.app.LibraryStyle) -> Unit,
    onCancel: () -> Unit,
) {
    val pvm = entryViewModel { ViewPickViewModel(services) }
    val ui by pvm.state.collectAsState()
    val shown = remember(stored) {
        val entries = io.github.lenticualrstone.civilight.feature.library.shelf.flat.ViewPick.entries()
        entries[io.github.lenticualrstone.civilight.feature.library.shelf.flat.ViewPick.currentIndex(entries, stored)].style
    }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg)) {
        // The screen behind keeps clear of the system bars; the picker pads its own sheet or dialog.
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            io.github.lenticualrstone.civilight.feature.library.shelf.flat.StylePreview(shown, ui, maxWidth, maxHeight)
        }
        // Until the stores answer, only the screen behind is drawn (the picker opens on the user's projects).
        if (ui.loaded) io.github.lenticualrstone.civilight.feature.library.shelf.flat.LibraryViewPicker(ui, stored, onApply, onCancel)
    }
}
