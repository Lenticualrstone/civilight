package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.navigation.PresetImages
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import kotlinx.serialization.Serializable

/** The picker of one `… ›` value row (no board draws it, `ED`: a radio list in the DS sheet). */
@Serializable
data class MoreValueSheet(val value: MoreValue) : SheetKey

/**
 * 설정 · 지도 · 이미지 · 연표 (LIB-68 container; rows CNV-19 `이미지`, WLD-67 `지도`, STO-17 / STO-42 `연표 · 때`;
 * board `SettingsMore`, head `설정` / `지도 · 이미지 · 연표`). Switches write at once; each value row opens
 * its picker sheet; `기본 이미지 · 보기 ›` opens the canvas lane's `PresetImages` (CNV-26).
 */
@Composable
internal fun MoreSettingsScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.more.collectAsState()
    val sheets = rememberSheetHost()
    SettingsFrame(
        title = MoreSettingsCopy.TITLE,
        smallLine = MoreSettingsCopy.SMALL,
        onBack = { navigator.pop() },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is MoreValueSheet -> sheetBody { ValuePickContent(key.value, vm) }
                else -> null
            }
        },
    ) {
        val pick: (MoreValueRow) -> Unit = { row -> sheets.open(MoreValueSheet(row.value)) }
        SettingsSection(MoreSettingsCopy.SECTION_IMAGE, first = true)
        SwitchSettingRow(ui.holo, vm::setHoloDefault)
        LinkRow(ui.presets) { navigator.push(PresetImages) }

        SettingsSection(MoreSettingsCopy.SECTION_MAP)
        ValueRow(ui.mapStyle, pick)
        ValueRow(ui.terrain, pick)
        ValueRow(ui.largeExport, pick)

        SettingsSection(MoreSettingsCopy.SECTION_TIME)
        ValueRow(ui.notation, pick)
        SwitchSettingRow(ui.checkTravel, vm::setCheckTravel)
        SwitchSettingRow(ui.checkNoPlace, vm::setCheckNoPlace)
    }
}

@Composable
private fun ValueRow(row: MoreValueRow, onClick: (MoreValueRow) -> Unit) {
    SettingsRow(title = row.title, onClick = { onClick(row) }, value = row.current, subtitle = row.sub)
}

/** Head = the row's title; one radio per choice; a pick writes and closes. */
@Composable
private fun SheetScope.ValuePickContent(value: MoreValue, vm: SettingsViewModel) {
    val scope = this
    // Re-read on every settings change, so the radio follows the stored value.
    val more by vm.more.collectAsState()
    val options = remember(more) { vm.options(value) }
    SheetHeader(title = DisplaySettingsState.title(value), onClose = scope::close, closeLabel = MoreSettingsCopy.CLOSE)
    SheetScrollBody(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
        options.forEach { option ->
            RadioRow(
                title = option.title,
                selected = option.selected,
                onClick = {
                    vm.choose(value, option.id)
                    scope.close()
                },
                subtitle = option.sub,
            )
        }
    }
}
