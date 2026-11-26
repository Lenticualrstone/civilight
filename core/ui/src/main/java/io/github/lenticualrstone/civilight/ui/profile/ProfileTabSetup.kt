package io.github.lenticualrstone.civilight.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ProfileTabConfig
import io.github.lenticualrstone.civilight.model.app.ProfileTabKind
import io.github.lenticualrstone.civilight.model.app.ProfileTabPreset

/** Copy of the one character-tab setup sheet (from the record's tab bar and from 설정). Labels only. */
object ProfileTabSetupCopy {
    const val TITLE = "인물 카드 탭 구성"
    const val SUB = "프리셋 · 탭 순서 · 새 탭"
    const val PRESETS = "프리셋"
    const val TABS = "탭 · 순서"
    const val UP = "↑"
    const val DOWN = "↓"
    const val UP_LABEL = "위로"
    const val DOWN_LABEL = "아래로"
    const val NEW_TAB = "새 탭 이름"
    const val BTN_ADD = "더하기"
    const val BTN_CLOSE = "닫기"
    const val PRESET_STANDARD = "표준 창작"
    const val PRESET_MODERN = "현대극 · 드라마"
    const val PRESET_GAME_VN = "게임 · VN"
    const val PRESET_MINIMAL = "미니멀"

    fun presetLabel(preset: ProfileTabPreset): String = when (preset) {
        ProfileTabPreset.STANDARD -> PRESET_STANDARD
        ProfileTabPreset.MODERN_DRAMA -> PRESET_MODERN
        ProfileTabPreset.GAME_VN -> PRESET_GAME_VN
        ProfileTabPreset.MINIMAL -> PRESET_MINIMAL
    }

    val all: List<String> = listOf(
        TITLE, SUB, PRESETS, TABS, UP_LABEL, DOWN_LABEL, NEW_TAB, BTN_ADD, BTN_CLOSE,
        PRESET_STANDARD, PRESET_MODERN, PRESET_GAME_VN, PRESET_MINIMAL,
    )
}

/** The pure edits of the sheet: every change returns the whole new tab list, or null when it must not apply. */
object ProfileTabSetup {
    /** The tabs the user works on: the custom list, else the preset's. */
    fun active(settings: AppSettings): List<ProfileTabConfig> =
        settings.customProfileTabs.ifEmpty { settings.profileTabPreset.defaultTabs() }

    /** A preset row is selected only while no custom list overrides it. */
    fun presetSelected(settings: AppSettings, preset: ProfileTabPreset): Boolean =
        settings.profileTabPreset == preset && settings.customProfileTabs.isEmpty()

    /** Turning a tab on or off; null when it would leave no tab on. */
    fun toggle(tabs: List<ProfileTabConfig>, id: String, on: Boolean): List<ProfileTabConfig>? {
        val next = tabs.map { if (it.id == id) it.copy(enabled = on) else it }
        return next.takeIf { list -> list.any { it.enabled } }
    }

    /** Moving the tab at [index] by [delta] (−1 up, +1 down); null at the ends. */
    fun move(tabs: List<ProfileTabConfig>, index: Int, delta: Int): List<ProfileTabConfig>? {
        val to = index + delta
        if (index !in tabs.indices || to !in tabs.indices) return null
        return tabs.toMutableList().apply { add(to, removeAt(index)) }
    }

    /** A custom tab named [name] (trimmed) at the end; null for a blank name. */
    fun addCustom(tabs: List<ProfileTabConfig>, name: String, id: String): List<ProfileTabConfig>? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        return tabs + ProfileTabConfig(id = id, label = trimmed, kind = ProfileTabKind.CUSTOM, enabled = true)
    }
}

/**
 * The one character-tab setup sheet, opened from the record's tab bar and from 설정 › 인물 카드 탭 구성: presets,
 * then the tabs with on / off and `↑ ↓` order (44 dp text actions), then a new custom tab.
 */
@Composable
fun ColumnScope.ProfileTabSetupContent(
    settings: AppSettings,
    onSelectPreset: (ProfileTabPreset) -> Unit,
    onUpdateTabs: (List<ProfileTabConfig>) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val tabs = ProfileTabSetup.active(settings)
    SheetHeader(title = ProfileTabSetupCopy.TITLE, closeLabel = ProfileTabSetupCopy.BTN_CLOSE, onClose = onClose)
    SheetScrollBody {
        CivilightText(ProfileTabSetupCopy.PRESETS, type.sec, color = colors.ink3, modifier = Modifier.padding(bottom = 6.dp))
        ProfileTabPreset.entries.forEach { preset ->
            RadioRow(
                title = ProfileTabSetupCopy.presetLabel(preset),
                subtitle = preset.defaultTabs().joinToString(" · ") { it.label },
                selected = ProfileTabSetup.presetSelected(settings, preset),
                onClick = { onSelectPreset(preset) },
            )
        }
        Spacer(Modifier.height(12.dp))
        CivilightText(ProfileTabSetupCopy.TABS, type.sec, color = colors.ink3, modifier = Modifier.padding(bottom = 6.dp))
        tabs.forEachIndexed { index, tab ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    CheckboxRow(
                        label = tab.label,
                        checked = tab.enabled,
                        onCheckedChange = { on -> ProfileTabSetup.toggle(tabs, tab.id, on)?.let(onUpdateTabs) },
                    )
                }
                if (index > 0) {
                    TextAction(
                        ProfileTabSetupCopy.UP,
                        onClick = { ProfileTabSetup.move(tabs, index, -1)?.let(onUpdateTabs) },
                        modifier = Modifier.semantics { contentDescription = ProfileTabSetupCopy.UP_LABEL },
                    )
                }
                if (index < tabs.lastIndex) {
                    TextAction(
                        ProfileTabSetupCopy.DOWN,
                        onClick = { ProfileTabSetup.move(tabs, index, 1)?.let(onUpdateTabs) },
                        modifier = Modifier.semantics { contentDescription = ProfileTabSetupCopy.DOWN_LABEL },
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        var name by rememberSaveable { mutableStateOf("") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = ProfileTabSetupCopy.NEW_TAB,
                modifier = Modifier.weight(1f),
            )
            TextAction(
                ProfileTabSetupCopy.BTN_ADD,
                onClick = {
                    ProfileTabSetup.addCustom(tabs, name, "custom_${System.currentTimeMillis()}")?.let {
                        onUpdateTabs(it)
                        name = ""
                    }
                },
                color = colors.acc,
            )
        }
    }
}
