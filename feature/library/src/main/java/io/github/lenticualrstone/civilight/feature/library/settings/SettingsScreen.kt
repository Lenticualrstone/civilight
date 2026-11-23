package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.layout.Spacer
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

/**
 * 설정 (LIB-63, board `Settings`; head `Civilight` / `설정`). Sections `화면` (theme segmented, 글자
 * 크기, 펼친 화면 배치, 움직임 · 투명도, 지도 · 이미지 · 연표), `보관` (Drive, 복구 암호) and `보안` (앱
 * 잠금, 숨기기), then the rows the board does not draw (§6 Q27 `ED`): 휴지통, 라이프 레코더, 도움말 ·
 * 안내 다시 보기. Each row's route is [SettingsRoutes.routeOf] (CNV-18 row map).
 */
@Composable
internal fun SettingsScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.root.collectAsState()
    val open: (SettingsRootRow) -> Unit = { row -> navigator.push(SettingsRoutes.routeOf(row)) }
    val sheets = rememberSheetHost()
    SettingsFrame(
        title = SettingsCopy.TITLE,
        smallLine = SettingsCopy.SMALL,
        onBack = { navigator.pop() },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is ViewStyleSheet -> sheetBody { ViewStylePickContent(key.kind, vm, services) }
                else -> null
            }
        },
    ) {
        SettingsSection(SettingsCopy.SECTION_SCREEN, first = true)
        ThemeSegmented(ui.themeIndex, vm::setTheme, Modifier.padding(top = 4.dp, bottom = 6.dp))
        LinkRow(ui.textSize) { open(SettingsRootRow.TEXT_SIZE) }
        LinkRow(ui.layout) { open(SettingsRootRow.LAYOUT) }
        // (0.2.0, LIB-94) After `펼친 화면 배치`: `서재 보기`, `카드 보기`, then the tilt switch, each drawn
        // only while a 3D view it would change has a renderer (no control that does nothing).
        ui.libraryView?.let { row -> LinkRow(row) { sheets.open(ViewStyleSheet(ViewKind.LIBRARY)) } }
        ui.cardView?.let { row -> LinkRow(row) { sheets.open(ViewStyleSheet(ViewKind.CARD)) } }
        ui.tilt?.let { row -> SwitchSettingRow(row, vm::setViewTilt) }
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

/** (0.2.0, LIB-94) Head = `서재 보기` or `카드 보기`; one radio per value; a pick applies at once and closes. */
@Composable
private fun SheetScope.ViewStylePickContent(kind: ViewKind, vm: SettingsViewModel, services: LibraryServices) {
    val scope = this
    val settings by services.deps.settings.collectAsState()
    val options = remember(settings, kind) { ViewStyles.options(kind, settings) }
    SheetHeader(title = ViewStyles.title(kind), onClose = scope::close, closeLabel = MoreSettingsCopy.CLOSE)
    SheetScrollBody(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
        options.forEach { option ->
            RadioRow(
                title = option.title,
                selected = option.selected,
                onClick = {
                    vm.chooseView(kind, option.id)
                    scope.close()
                },
            )
        }
    }
}
