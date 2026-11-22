package io.github.lenticualrstone.civilight.feature.library.rating

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.navigation.RatingDefaultsApply
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection

/**
 * `이미 있는 <rating> 프로젝트 <N> · 같이 바꾸기` (LIB-23): the projects of that rating whose values
 * differ from the changed defaults, each with what would change, all checked at first (`ED`);
 * `같이 바꾸기` sets the group's defaults on the checked ones (hiding off asks first) and returns.
 */
@Composable
internal fun RatingDefaultsApplyScreen(route: RatingDefaultsApply, services: LibraryServices) {
    val deps = services.deps
    val vm = entryViewModel { RatingSettingsViewModel(services) }
    val projects by vm.projects.collectAsState()
    val settings by deps.settings.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    val rows = remember(projects, settings.ratingDefaults, unlock.hiddenGroupOpen) {
        RatingSettingsState.applyRows(route.rating, settings.ratingDefaults, projects, unlock.hiddenGroupOpen)
    }
    var unchecked by rememberSaveable { mutableStateOf(setOf<String>()) }
    var working by remember { mutableStateOf(false) }
    val checkedRows = rows.filter { it.project.key !in unchecked }
    CivilightScreen(
        title = RatingSettingsState.applyTitle(route.rating, rows.size),
        smallLine = RatingCopy.SETTINGS_SMALL,
        back = IconAction(BACK_LABEL) { deps.navigator.pop() },
        commandBar = {
            CivilightButton(
                RatingCopy.APPLY_TOGETHER,
                onClick = {
                    working = true
                    vm.apply(checkedRows) { outcome ->
                        working = false
                        if (RatingSheetModel.closesAfter(outcome)) deps.navigator.pop()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = checkedRows.isNotEmpty() && !working,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            ) {
                rows.forEach { row ->
                    if (row.project.protections.hidden) ProtectWindow(WindowProtection.forProject(row.project.protections))
                    CheckboxRow(
                        label = row.project.name,
                        checked = row.project.key !in unchecked,
                        onCheckedChange = { on -> unchecked = if (on) unchecked - row.project.key else unchecked + row.project.key },
                        subtitle = row.changes,
                    )
                }
            }
        }
    }
}
