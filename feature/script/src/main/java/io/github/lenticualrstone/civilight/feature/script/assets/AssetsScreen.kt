package io.github.lenticualrstone.civilight.feature.script.assets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.NoResults
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.feature.script.ui.ScriptCommandField
import io.github.lenticualrstone.civilight.model.script.ScriptAssetKind
import io.github.lenticualrstone.civilight.navigation.ScriptAssets
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost

/**
 * `에셋` (SCR-61, board `Assets`): header small line = project name, title `에셋`, `+` on the right;
 * filter chips `전체 · 배경 · BGM · 효과음 · CG · 안 쓰는 것`; sections `배경 · 4` with rows; the
 * command field at the bottom searches names and file names as typed (hint
 * `에셋 이름이나 파일 이름으로 찾기`). A row opens its form, `+` the add form of the chip's kind.
 * Inner screens keep the list in the 560 reading column. Read-only projects get no `+`.
 */
@Composable
fun AssetsScreen(route: ScriptAssets, deps: FeatureDeps) {
    val vm = entryViewModel { AssetsViewModel(route.projectKey, deps) }
    val ui by vm.ui.collectAsState()
    val sheets = rememberSheetHost()
    var filterName by rememberSaveable { mutableStateOf(AssetFilter.ALL.name) }
    var query by rememberSaveable { mutableStateOf("") }
    var typing by rememberSaveable { mutableStateOf(false) }
    val filter = AssetFilter.valueOf(filterName)
    LaunchedEffect(filter) { vm.setFilter(filter) }
    LaunchedEffect(query) { vm.setQuery(query) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val current = ui
    fun stopSearch() {
        typing = false
        query = ""
        focusManager.clearFocus()
    }
    CivilightScreen(
        title = AssetsCopy.TITLE,
        smallLine = current?.projectName,
        back = IconAction(BACK_LABEL) { deps.navigator.pop() },
        actions = {
            if (current != null && !current.readOnly) {
                CivilightIconButton(IconId.Plus, AssetsCopy.ADD_TITLE, onClick = {
                    sheets.open(AssetFormSheetKey(route.projectKey, kind = filter.kind ?: ScriptAssetKind.BACKGROUND))
                })
            }
        },
        commandBar = {
            ScriptCommandField(
                typing = typing,
                text = query,
                onStart = { typing = true },
                onText = { query = it },
                onSubmit = { focusManager.clearFocus() },
                onClose = ::stopSearch,
                focusRequester = focus,
                hint = AssetsCopy.SEARCH_HINT,
            )
        },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is AssetFormSheetKey -> { { AssetFormSheet(key) } }
                else -> null
            }
        },
    ) { padding ->
        if (current == null) return@CivilightScreen
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.readingMeasure().fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 24.dp, end = 24.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                item(key = "chips") {
                    ChipFlow {
                        AssetFilter.entries.forEach { f ->
                            FilterChip(f.label, selected = f == filter, onClick = { filterName = f.name })
                        }
                    }
                }
                if (current.nothingFound) {
                    item(key = "none") { NoResults(AssetsCopy.NOTHING_FOUND) }
                }
                if (current.noAssets) {
                    item(key = "empty") {
                        EmptyBlock(
                            title = AssetsCopy.NO_ASSETS,
                            modifier = Modifier.padding(top = 40.dp),
                            primaryLabel = AssetsCopy.ADD_TITLE.takeIf { !current.readOnly },
                            onPrimary = { sheets.open(AssetFormSheetKey(route.projectKey, kind = filter.kind ?: ScriptAssetKind.BACKGROUND)) },
                        )
                    }
                }
                current.sections.forEach { section ->
                    item(key = "sec:${section.title}") {
                        SectionLabel(section.title, Modifier.padding(top = 22.dp, bottom = 4.dp))
                    }
                    items(section.rows, key = { it.key }) { row ->
                        AssetRowView(row) { sheets.open(AssetFormSheetKey(route.projectKey, assetKey = row.key)) }
                    }
                }
            }
        }
    }
}
