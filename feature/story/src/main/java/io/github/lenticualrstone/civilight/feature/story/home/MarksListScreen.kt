package io.github.lenticualrstone.civilight.feature.story.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.model.marks.MarkState
import io.github.lenticualrstone.civilight.navigation.MarksList
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen

/**
 * (0.2.0, N-41) `할 일 · 복선` (route [MarksList], from home `작업`): the single-choice filters `열림 · 거둠 ·
 * 끝냄` with counts and the rows of the picked filter. A row opens its block in the script; a row whose plant
 * has no live block after it is drawn without a press state. Read-only projects list the same rows (opening
 * a block only reads). The route's filter is a [MarkState] name; null or unknown = `열림`.
 */
@Composable
fun MarksListScreen(route: MarksList, deps: FeatureDeps) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    var filterName by rememberSaveable { mutableStateOf(MarksListRoute.filterOf(route).name) }
    val filter = MarkState.valueOf(filterName)
    val locked = !unlock.itemsOpen(route.projectKey)
    val ui = remember(doc, filter, locked) { HomeMarks.build(doc, filter, locked) }
    val colors = CivilightTheme.colors
    CivilightScreen(
        title = HomeCopy.MARKS_TITLE,
        smallLine = doc.project.name,
        back = IconAction(StoryCopy.BACK) { deps.navigator.pop() },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).padding(horizontal = 16.dp)) {
            ChipFlow(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                ui.chips.forEach { chip -> FilterChip(chip.label, chip.state == filter, { filterName = chip.state.name }) }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = padding.calculateBottomPadding())) {
                items(ui.rows, key = { it.key }) { row ->
                    ListRow(
                        modifier = civilightItem(),
                        title = row.title,
                        subtitle = listOf(row.kind, row.sub).filter { it.isNotEmpty() }.joinToString(" · "),
                        titleDotted = row.moved,
                        onClick = row.blockKey?.let { block -> { deps.navigator.push(Script(route.projectKey, blockKey = block)) } },
                        trailing = if (row.blockKey != null) {
                            { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/** The route's filter (JVM tested). */
object MarksListRoute {
    fun filterOf(route: MarksList): MarkState = MarkState.entries.firstOrNull { it.name == route.filter } ?: MarkState.OPEN
}
