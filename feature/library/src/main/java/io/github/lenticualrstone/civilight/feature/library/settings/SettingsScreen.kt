package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.BlockSpacing
import io.github.lenticualrstone.civilight.model.app.DragResistance
import io.github.lenticualrstone.civilight.model.app.MiniRailProjectItem
import io.github.lenticualrstone.civilight.model.app.MiniRailScriptItem
import io.github.lenticualrstone.civilight.model.app.ProfileShape
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

@Serializable
private data object MiniRailSheet : SheetKey

/**
 * 설정 (0.4.0 reorganized into 6 related groups with category icons, inline tactile controls,
 * customizable mini-rail preview sheet, and experimental section).
 */
@Composable
internal fun SettingsScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.root.collectAsState()
    val settings by vm.settings.collectAsState()
    val sheets = rememberSheetHost()
    val open: (SettingsRootRow) -> Unit = { row -> navigator.push(SettingsRoutes.routeOf(row)) }

    SettingsFrame(
        title = SettingsCopy.TITLE,
        smallLine = SettingsCopy.SMALL,
        onBack = { navigator.pop() },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                MiniRailSheet -> sheetBody {
                    MiniRailCustomizeContent(
                        settings = settings,
                        onToggleScript = vm::toggleMiniRailScript,
                        onToggleProject = vm::toggleMiniRailProject,
                        onClose = { close() },
                    )
                }
                else -> null
            }
        },
    ) {
        // 1. 화면 · 보기
        SettingsSection(SettingsCopy.SECTION_DISPLAY, first = true)
        ThemeSegmented(ui.themeIndex, vm::setTheme, Modifier.padding(top = 4.dp, bottom = 6.dp))
        LinkRow(ui.textSize, leadingIcon = IconId.Text) { open(SettingsRootRow.TEXT_SIZE) }
        ui.libraryView?.let { row -> LinkRow(row, leadingIcon = IconId.Grid) { navigator.push(ViewKind.LIBRARY.route) } }
        ui.cardView?.let { row -> LinkRow(row, leadingIcon = IconId.Layers) { navigator.push(ViewKind.CARD.route) } }
        SettingsSegmentedBlock(
            title = SettingsCopy.PROFILE_SHAPE,
            icon = IconId.User,
            options = listOf(SettingsCopy.SHAPE_SQUARE, SettingsCopy.SHAPE_CIRCLE),
            selectedIndex = ProfileShape.entries.indexOf(settings.profileShape).coerceAtLeast(0),
            onSelect = { idx -> ProfileShape.entries.getOrNull(idx)?.let(vm::setProfileShape) },
        )
        SettingsValueRow(
            title = SettingsCopy.MINI_RAIL,
            subtitle = SettingsCopy.MINI_RAIL_SUB,
            value = "${settings.miniRailScript.size} · ${settings.miniRailProject.size}",
            leadingIcon = IconId.Panel,
            onClick = { sheets.open(MiniRailSheet) },
        )

        // 2. 움직임 · 손맛
        SettingsSection(SettingsCopy.SECTION_FEEL)
        SettingsSegmentedBlock(
            title = SettingsCopy.DRAG_RESISTANCE,
            subtitle = SettingsCopy.DRAG_RESISTANCE_SUB,
            icon = IconId.Sliders,
            options = listOf(
                SettingsCopy.RESISTANCE_SOFT,
                SettingsCopy.RESISTANCE_NORMAL,
                SettingsCopy.RESISTANCE_FIRM,
                SettingsCopy.RESISTANCE_HEAVY,
            ),
            selectedIndex = DragResistance.entries.indexOf(settings.dragResistance).coerceAtLeast(0),
            onSelect = { idx -> DragResistance.entries.getOrNull(idx)?.let(vm::setDragResistance) },
        )
        ViewStyles.tiltShown(ui.tilt, CivilightTheme.motion.reduced)?.let { row ->
            SwitchSettingRow(row, leadingIcon = IconId.Rotate, onChange = vm::setViewTilt)
        }
        SwitchRow(
            title = SettingsCopy.LONG_PRESS_WIGGLE,
            subtitle = SettingsCopy.LONG_PRESS_WIGGLE_SUB,
            checked = settings.longPressWiggle,
            onCheckedChange = vm::setLongPressWiggle,
            leadingIcon = IconId.Spark,
        )
        LinkRow(ui.motion, leadingIcon = IconId.Wave) { open(SettingsRootRow.MOTION) }

        // 3. 작업 기본값
        SettingsSection(SettingsCopy.SECTION_WORK)
        SettingsSegmentedBlock(
            title = SettingsCopy.BLOCK_SPACING,
            icon = IconId.List,
            options = listOf(SettingsCopy.SPACING_COMPACT, SettingsCopy.SPACING_NORMAL, SettingsCopy.SPACING_RELAXED),
            selectedIndex = BlockSpacing.entries.indexOf(settings.blockSpacing).coerceAtLeast(0),
            onSelect = { idx -> BlockSpacing.entries.getOrNull(idx)?.let(vm::setBlockSpacing) },
        )
        SwitchRow(
            title = SettingsCopy.TYPEWRITER_SCROLL,
            subtitle = SettingsCopy.TYPEWRITER_SCROLL_SUB,
            checked = settings.typewriterScroll,
            onCheckedChange = vm::setTypewriterScroll,
            leadingIcon = IconId.Outline,
        )
        SwitchRow(
            title = SettingsCopy.SMART_PUNCTUATION,
            subtitle = SettingsCopy.SMART_PUNCTUATION_SUB,
            checked = settings.smartPunctuation,
            onCheckedChange = vm::setSmartPunctuation,
            leadingIcon = IconId.Edit,
        )
        LinkRow(ui.more, leadingIcon = IconId.Map) { open(SettingsRootRow.MORE) }
        LinkRow(ui.templates, leadingIcon = IconId.File) { open(SettingsRootRow.TEMPLATES) }

        // 4. 보관 · 보안
        SettingsSection(SettingsCopy.SECTION_KEEP_SECURITY)
        SwitchLinkRow(ui.drive, leadingIcon = IconId.Drive) { open(SettingsRootRow.DRIVE) }
        LinkRow(ui.recovery, leadingIcon = IconId.Shield) { open(SettingsRootRow.RECOVERY) }
        SwitchSettingRow(ui.appLock, leadingIcon = IconId.Lock, onChange = vm::setAppLock)
        LinkRow(ui.hiding, leadingIcon = IconId.EyeOff) { open(SettingsRootRow.HIDING) }

        // 5. 실험 기능
        SettingsSection(SettingsCopy.SECTION_EXPERIMENTAL)
        SwitchRow(
            title = SettingsCopy.SMART_SPEAKER_SWAP,
            subtitle = SettingsCopy.SMART_SPEAKER_SWAP_SUB,
            checked = settings.smartSpeakerSwap,
            onCheckedChange = vm::setSmartSpeakerSwap,
            leadingIcon = IconId.User,
        )
        SwitchRow(
            title = SettingsCopy.LIFE_RECORDER_EXP,
            subtitle = SettingsCopy.LIFE_RECORDER_EXP_SUB,
            checked = settings.lifeRecorderEnabled,
            onCheckedChange = vm::setLifeRecorderEnabled,
            leadingIcon = IconId.Mic,
        )
        if (settings.lifeRecorderEnabled) {
            SettingsRow(title = SettingsCopy.RECORDER, leadingIcon = IconId.Mic, onClick = { open(SettingsRootRow.RECORDER) })
        }

        // 6. 관리 · 지원
        SettingsSection(SettingsCopy.SECTION_SUPPORT)
        SettingsRow(title = SettingsCopy.TRASH, leadingIcon = IconId.Trash, onClick = { open(SettingsRootRow.TRASH) })
        SettingsRow(title = SettingsCopy.HELP, leadingIcon = IconId.Book, onClick = { open(SettingsRootRow.HELP) })
    }
}

@Composable
private fun SettingsSegmentedBlock(
    title: String,
    icon: IconId,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    subtitle: String? = null,
) {
    val colors = CivilightTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink2)
            Column(Modifier.weight(1f)) {
                CivilightText(title, CivilightTheme.type.itemTitleCompact, color = colors.ink)
                if (subtitle != null) CivilightText(subtitle, CivilightTheme.type.small, color = colors.ink3)
            }
        }
        SegmentedControl(options = options, selectedIndex = selectedIndex, onSelect = onSelect)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.MiniRailCustomizeContent(
    settings: AppSettings,
    onToggleScript: (MiniRailScriptItem) -> Unit,
    onToggleProject: (MiniRailProjectItem) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    var tab by remember { mutableIntStateOf(0) }
    val previewIcons: List<IconId> = if (tab == 0) {
        settings.miniRailScript.map(::scriptRailIcon)
    } else {
        settings.miniRailProject.map(::projectRailIcon)
    }

    SheetHeader(title = SettingsCopy.MINI_RAIL, closeLabel = "닫기", onClose = onClose)
    SheetScrollBody {
        SegmentedControl(
            options = listOf(SettingsCopy.MINI_RAIL_SCRIPT, SettingsCopy.MINI_RAIL_PROJECT),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Live Z Fold unfolded mini-rail preview
        CivilightText(SettingsCopy.MINI_RAIL_PREVIEW, CivilightTheme.type.sec, color = colors.ink3, modifier = Modifier.padding(bottom = 6.dp))
        val frameShape = RoundedCornerShape(16.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(frameShape)
                .background(colors.bg)
                .insetOutline(colors.line, 1.dp, frameShape),
        ) {
            // Left or right rail preview (script = right rail, project = left rail)
            if (tab == 1) {
                MiniRailPreviewStrip(previewIcons, colors)
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
            }
            // Mock unfolded canvas area
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth(0.45f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.ink3.copy(alpha = 0.25f)))
                Box(Modifier.fillMaxWidth(0.85f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.ink3.copy(alpha = 0.14f)))
                Box(Modifier.fillMaxWidth(0.70f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.ink3.copy(alpha = 0.14f)))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(0.62f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.acc.copy(alpha = 0.18f)))
            }
            if (tab == 0) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                MiniRailPreviewStrip(previewIcons, colors)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (tab == 0) {
            MiniRailScriptItem.entries.forEach { item ->
                CheckboxRow(
                    label = scriptRailLabel(item),
                    checked = item in settings.miniRailScript,
                    onCheckedChange = { onToggleScript(item) },
                )
            }
        } else {
            MiniRailProjectItem.entries.forEach { item ->
                CheckboxRow(
                    label = projectRailLabel(item),
                    checked = item in settings.miniRailProject,
                    onCheckedChange = { onToggleProject(item) },
                )
            }
        }
    }
}

@Composable
private fun MiniRailPreviewStrip(
    icons: List<IconId>,
    colors: io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors,
) {
    Column(
        Modifier
            .width(42.dp)
            .fillMaxHeight()
            .background(colors.tint.copy(alpha = 0.55f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        CivilightIcon(IconId.Panel, contentDescription = null, size = 14.dp, tint = colors.acc)
        Box(Modifier.width(18.dp).height(1.dp).background(colors.line))
        icons.take(6).forEach { icon ->
            CivilightIcon(icon, contentDescription = null, size = 13.dp, tint = colors.ink2)
        }
    }
}

private fun scriptRailIcon(item: MiniRailScriptItem): IconId = when (item) {
    MiniRailScriptItem.TIME -> IconId.Clock
    MiniRailScriptItem.REF -> IconId.Book
    MiniRailScriptItem.PROPS -> IconId.Sliders
    MiniRailScriptItem.MODS -> IconId.Grid
    MiniRailScriptItem.OUTLINE -> IconId.Outline
    MiniRailScriptItem.SPEAKER_FOCUS -> IconId.User
}

private fun scriptRailLabel(item: MiniRailScriptItem): String = when (item) {
    MiniRailScriptItem.TIME -> "시간"
    MiniRailScriptItem.REF -> "참고"
    MiniRailScriptItem.PROPS -> "속성"
    MiniRailScriptItem.MODS -> "모듈"
    MiniRailScriptItem.OUTLINE -> "씬 목차"
    MiniRailScriptItem.SPEAKER_FOCUS -> "화자 포커스"
}

private fun projectRailIcon(item: MiniRailProjectItem): IconId = when (item) {
    MiniRailProjectItem.SCRIPT -> IconId.Edit
    MiniRailProjectItem.CAST -> IconId.User
    MiniRailProjectItem.WORLD -> IconId.Book
    MiniRailProjectItem.MAP -> IconId.Map
    MiniRailProjectItem.TIMELINE -> IconId.Clock
    MiniRailProjectItem.CHECKS -> IconId.Check
    MiniRailProjectItem.GALLERY -> IconId.Image
    MiniRailProjectItem.SETTINGS -> IconId.Settings
}

private fun projectRailLabel(item: MiniRailProjectItem): String = when (item) {
    MiniRailProjectItem.SCRIPT -> "원고"
    MiniRailProjectItem.CAST -> "인물"
    MiniRailProjectItem.WORLD -> "세계"
    MiniRailProjectItem.MAP -> "지도"
    MiniRailProjectItem.TIMELINE -> "연표"
    MiniRailProjectItem.CHECKS -> "점검"
    MiniRailProjectItem.GALLERY -> "갤러리"
    MiniRailProjectItem.SETTINGS -> "설정"
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
            .stateIn(viewModelScope, SharingStarted.Eagerly, io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi())
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
    Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            io.github.lenticualrstone.civilight.feature.library.shelf.flat.StylePreview(shown, ui, maxWidth, maxHeight)
        }
        if (ui.loaded) io.github.lenticualrstone.civilight.feature.library.shelf.flat.LibraryViewPicker(ui, stored, onApply, onCancel)
    }
}
