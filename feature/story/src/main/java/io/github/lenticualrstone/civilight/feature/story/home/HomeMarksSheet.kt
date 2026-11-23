package io.github.lenticualrstone.civilight.feature.story.home

import androidx.compose.foundation.layout.padding
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
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.marks.MarkState
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore

/**
 * (0.2.0, N-41) `할 일 · 복선` over home: the single-choice filters `열림 · 거둠 · 끝냄` with counts and the
 * rows of the picked filter. A row opens its block in the script; a row whose plant has no live block after
 * it is drawn without a press state. Read-only projects list the same rows (opening a block only reads).
 */
@Composable
fun SheetScope.HomeMarksContent(sheet: HomeMarksSheet, deps: FeatureDeps) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    var filterName by rememberSaveable { mutableStateOf(MarkState.OPEN.name) }
    val filter = MarkState.valueOf(filterName)
    val locked = !unlock.itemsOpen(sheet.projectKey)
    val ui = remember(doc, filter, locked) { HomeMarks.build(doc, filter, locked) }
    val colors = CivilightTheme.colors
    val closeSheet: () -> Unit = ::close
    SheetHeader(title = HomeCopy.MARKS_TITLE, onClose = ::close)
    ChipFlow(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        ui.chips.forEach { chip -> FilterChip(chip.label, chip.state == filter, { filterName = chip.state.name }) }
    }
    SheetScrollBody {
        ui.rows.forEach { row ->
            ListRow(
                title = row.title,
                subtitle = listOf(row.kind, row.sub).filter { it.isNotEmpty() }.joinToString(" · "),
                titleDotted = row.moved,
                onClick = row.blockKey?.let { block ->
                    {
                        closeSheet()
                        deps.navigator.push(Script(sheet.projectKey, blockKey = block))
                    }
                },
                trailing = if (row.blockKey != null) {
                    { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) }
                } else {
                    null
                },
            )
        }
    }
}
