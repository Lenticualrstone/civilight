package io.github.lenticualrstone.civilight.feature.cast.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.NoResults
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.list.CastRow
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.HiddenMark
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.RelationMap
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/** 인물 목록 (CST-01, CST-02; boards `Cast`, `EmptyCast`, `TwoCast`, `ColCast`). */
@Composable
fun CastListScreen(route: CastList, deps: FeatureDeps) {
    val vm = entryViewModel { CastListViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val doc = ready.doc
    val gate = rememberHiddenGate(route.projectKey)
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val ui = CastListUi(selectedTag, typed.text, searching)
    val state = remember(doc, ui, gate.open, ready.readOnly) { CastListModel.build(doc, ui, gate.open, ready.readOnly) }
    val view = state.view
    val sheets = rememberSheetHost()
    val paneRole = LocalPaneRole.current
    val top = deps.navigator.backStack.lastOrNull()
    val selectedKey = if (paneRole == PaneRole.LIST) (top as? CharacterProfile)?.characterKey else null
    val open: (String) -> Unit = { key -> deps.navigator.openDetail(CharacterProfile(route.projectKey, key)) }

    CenterColumn {
        CivilightScreen(
            title = view.title,
            smallLine = view.projectName,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
            lens = lensChipLabel(doc),
            onLens = { sheets.open(LensSheet(route.projectKey)) },
            actions = {
                if (!state.readOnly) {
                    CivilightIconButton(IconId.Plus, CastUiCopy.NEW_CHARACTER, { sheets.open(NewCharacterSheet(route.projectKey)) }, tint = CivilightTheme.colors.ink)
                }
            },
            commandBar = {
                if (searching) {
                    SearchBar(typed, { typed = it }, view.searchHint) {
                        searching = false
                        typed = TextFieldValue("")
                    }
                } else {
                    CommandBar(
                        hint = view.searchHint,
                        onClick = { searching = true },
                        onMic = { deps.navigator.push(Recording(projectKey = route.projectKey)) },
                        micLabel = CastUiCopy.MIC,
                    )
                }
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is NewCharacterSheet -> {
                        { NewCharacterSheetContent { name -> vm.create(name) { created -> close(); open(created) } } }
                    }
                    else -> null
                }
            },
        ) { padding ->
            CastListBody(
                state = state,
                padding = padding,
                selectedKey = selectedKey,
                onTag = { selectedTag = it },
                onRow = open,
                onUnlock = gate.unlock,
                onAdd = { sheets.open(NewCharacterSheet(route.projectKey)) },
                onRecorder = { deps.navigator.push(RecorderInbox()) },
                onMap = { deps.navigator.push(RelationMap(route.projectKey)) },
            )
        }
    }
}

@Composable
private fun SearchBar(value: TextFieldValue, onChange: (TextFieldValue) -> Unit, hint: String, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    SearchField(value, onChange, Modifier.focusRequester(focus), placeholder = hint, onClose = onClose, closeLabel = CastUiCopy.BTN_CLOSE)
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun CastListBody(
    state: CastListState,
    padding: PaddingValues,
    selectedKey: String?,
    onTag: (String?) -> Unit,
    onRow: (String) -> Unit,
    onUnlock: () -> Unit,
    onAdd: () -> Unit,
    onRecorder: () -> Unit,
    onMap: () -> Unit,
) {
    val view = state.view
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
    ) {
        if (view.empty == null) {
            item(key = "mode") {
                // `목록 | 관계도` (CST-01; not on `EmptyCast`): `관계도` is the map (CST-33), a separate route for now.
                SegmentedControl(CastUiCopy.VIEW_MODES, 0, { if (it == 1) onMap() }, Modifier.padding(bottom = 12.dp))
            }
        }
        item(key = "chips") {
            ChipFlow {
                for (chip in view.chips) FilterChip(chip.label, chip.selected, { onTag(chip.tagKey) })
            }
        }
        view.empty?.let { empty ->
            item(key = "empty") {
                EmptyBlock(
                    title = empty.title,
                    subtitle = empty.sub,
                    primaryLabel = empty.primary.takeUnless { state.readOnly },
                    onPrimary = onAdd.takeUnless { state.readOnly },
                    secondaryLabel = empty.secondary,
                    onSecondary = onRecorder,
                )
            }
        }
        if (view.noResults) item(key = "none") { NoResults(CastUiCopy.NO_RESULTS) }
        view.lockRow?.let { lock ->
            item(key = "lock") {
                Box(Modifier.padding(top = 16.dp)) { LockRow(lock.title, lock.action, onUnlock, subtitle = lock.sub) }
            }
        }
        for (group in view.groups) {
            item(key = "sec-${group.tagKey}") { SectionLabel(group.title, Modifier.padding(top = 20.dp, bottom = 6.dp)) }
            items(group.rows, key = { "row-${group.tagKey}-${it.key}" }) { row ->
                CastRowView(row, selected = row.key == selectedKey, onClick = { onRow(row.key) })
            }
        }
    }
}

@Composable
private fun CastRowView(row: CastRow, selected: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    ListRow(
        title = row.name,
        subtitle = row.subtitle.ifEmpty { null },
        leading = { Avatar(row.name, size = 40.dp) },
        trailing = {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (row.hiddenTag) HiddenMark()
                row.firstAppearance?.let { CivilightText(it, CivilightTheme.type.small, color = colors.ink3, maxLines = 1) }
            }
        },
        onClick = onClick,
        selected = selected,
    )
}
