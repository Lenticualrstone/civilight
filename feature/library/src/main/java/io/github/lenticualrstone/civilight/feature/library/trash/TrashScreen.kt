package io.github.lenticualrstone.civilight.feature.library.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.ConfirmDialog
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.trash.TrashFilter
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.navigation.Trash
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/**
 * 휴지통 (LIB-38, LIB-39; board `Trash`): small `30일 뒤 자동 삭제`, title `휴지통`, `비우기`; the filter
 * chips; rows with title, `<d>일 남음`, sub line and `되살리기` · `영구 삭제`. A block group opens to
 * its blocks, each with its own `되살리기`. Hidden things show only their masked form while locked
 * and ask for the prompt before they come back.
 */
@Composable
internal fun TrashScreen(route: Trash, services: LibraryServices) {
    val deps = services.deps
    val vm = entryViewModel { TrashViewModel(services) }
    val ui by vm.ui.collectAsState()
    val working by vm.working.collectAsState()
    val waiting by vm.sceneChoice.collectAsState()
    val sheets = rememberSheetHost()
    var filterName by rememberSaveable { mutableStateOf(route.filter.name) }
    val filter = TrashFilter.valueOf(filterName)
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    var purging by rememberSaveable { mutableStateOf<String?>(null) }
    var emptying by rememberSaveable { mutableStateOf(false) }
    val rows = remember(ui, filter) { ui.visible(filter) }

    LaunchedEffect(waiting) {
        val choice = waiting ?: return@LaunchedEffect
        sheets.open(SceneChoiceSheet(choice.projectKey))
    }
    // A confirm about a row that left the list (restored elsewhere, purged) or became masked closes.
    LaunchedEffect(ui) {
        if (ui.loaded && purging?.let(ui::row) == null) purging = null
    }

    CivilightScreen(
        title = TrashScreenCopy.TITLE,
        smallLine = TrashScreenCopy.SMALL,
        back = IconAction(BACK_LABEL) { deps.navigator.pop() },
        actions = {
            if (ui.purgeable.isNotEmpty()) TextAction(TrashScreenCopy.EMPTY_TRASH, { if (!working) emptying = true })
        },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is SceneChoiceSheet -> sheetBody { SceneChoiceContent(vm, waiting != null) }
                is ScenePickSheet -> sheetBody { ScenePickContent(key.projectKey, services, vm, waiting != null) }
                else -> null
            }
        },
    ) { padding ->
        CenterColumn {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                item(key = "chips") {
                    ChipFlow(civilightItem().fillMaxWidth()) {
                        ui.chips.forEach { chip ->
                            FilterChip(chip.label, selected = chip.filter == filter, onClick = { filterName = chip.filter.name })
                        }
                    }
                }
                if (ui.loaded && rows.isEmpty()) {
                    item(key = "empty") {
                        // LIB-70 shared empty form (DS `EmptyBlock`); no board draws an empty 휴지통, so the title is a fragment.
                        Column(civilightItem()) { EmptyBlock(title = TrashScreenCopy.EMPTY) }
                    }
                }
                item(key = "gap") { Column(Modifier.padding(top = 10.dp)) {} }
                items(rows, key = { it.key }) { row ->
                    TrashRowItem(
                        modifier = civilightItem(),
                        row = row,
                        expanded = row.key in expanded,
                        enabled = !working,
                        onToggle = { expanded = if (row.key in expanded) expanded - row.key else expanded + row.key },
                        onRestore = { vm.restore(row) },
                        onPurge = { purging = row.key },
                        onRestoreBlock = { block -> vm.restoreBlock(row, block) },
                    )
                }
            }
        }
    }

    purging?.let { key ->
        val row = ui.row(key) ?: return@let
        ConfirmDialog(
            title = "${row.row.title}\n${TrashScreenCopy.PURGE_CONFIRM}",
            confirmLabel = TrashScreenCopy.PURGE,
            cancelLabel = TrashScreenCopy.CANCEL,
            hardToUndo = true,
            onConfirm = {
                purging = null
                vm.purge(row)
            },
            onDismissRequest = { purging = null },
        )
    }
    if (emptying) {
        ConfirmDialog(
            title = "${TrashScreenCopy.EMPTY_CONFIRM} (${ui.purgeable.size}건)",
            confirmLabel = TrashScreenCopy.EMPTY_TRASH,
            cancelLabel = TrashScreenCopy.CANCEL,
            hardToUndo = true,
            onConfirm = {
                emptying = false
                vm.emptyTrash()
            },
            onDismissRequest = { emptying = false },
        )
    }
}

/**
 * One row (board `Trash`): title `itemTitleCompact` with `<d>일 남음`, sub `small`, then the two tags.
 * A block group shows a visible open/close control; open, each block's first line has `되살리기`.
 */
@Composable
private fun TrashRowItem(
    modifier: Modifier,
    row: TrashRowUi,
    expanded: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
    onRestoreBlock: (String) -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    row.protections?.let { ProtectWindow(WindowProtection.forProject(it)) }
    val motion = CivilightTheme.motion
    // Chevron turns on `standard` (snaps when reduced); the block lines fold open and closed (audit row 10).
    val turn by animateFloatAsState(if (expanded) 90f else 0f, motion.move(CivilightMotion.standard), label = "trashChevron")
    Column(modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightText(row.row.title, type.itemTitleCompact, Modifier.weight(1f), color = colors.ink)
            CivilightText(row.row.daysLeftLabel, type.small, color = colors.ink3, maxLines = 1)
            if (row.isBlockGroup && row.row.lines.isNotEmpty()) {
                CivilightIconButton(
                    IconId.Next,
                    if (expanded) TrashScreenCopy.COLLAPSE_LABEL else TrashScreenCopy.EXPAND_LABEL,
                    onToggle,
                    Modifier.graphicsLayer { rotationZ = turn },
                    tint = colors.ink3,
                )
            }
        }
        CivilightText(row.row.sub, type.small, color = colors.ink3)
        FoldReveal(expanded && row.row.lines.isNotEmpty()) {
            Column {
                row.row.lines.forEach { line ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CivilightText("#" + line.number.toString().padStart(3, '0'), type.number, color = colors.ink3)
                        CivilightText(line.firstLine, type.command, Modifier.weight(1f), color = colors.ink2, maxLines = 1)
                        TextAction(TrashScreenCopy.RESTORE, { if (enabled) onRestoreBlock(line.blockKey) })
                    }
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(TrashScreenCopy.RESTORE, selected = false, onClick = onRestore, enabled = enabled)
            FilterChip(TrashScreenCopy.PURGE, selected = false, onClick = onPurge, enabled = enabled)
        }
    }
}
