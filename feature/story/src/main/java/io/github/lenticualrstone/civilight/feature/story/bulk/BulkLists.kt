package io.github.lenticualrstone.civilight.feature.story.bulk

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ProgressBar
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkProgress
import io.github.lenticualrstone.civilight.domain.story.bulk.BulkRow
import io.github.lenticualrstone.civilight.feature.story.common.MiniMapView
import io.github.lenticualrstone.civilight.feature.story.common.MiniMaps
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** Cover list (`BulkList`): progress bars, filter chips, episode groups of checkbox rows. */
@Composable
fun BulkCover(ui: BulkUi, padding: PaddingValues, actions: BulkActions) {
    val colors = CivilightTheme.colors
    LazyColumn(
        Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
    ) {
        item(key = "progress") { Progress(ui, civilightItem()) }
        item(key = "chips") {
            Row(civilightItem().fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ui.chips.forEach { chip -> FilterChip(chip.label, chip.filter == ui.filter, { actions.onFilter(chip.filter) }) }
            }
        }
        ui.groups.forEach { group ->
            if (group.code.isNotEmpty()) {
                item(key = "g:${group.code}") { CivilightText(group.code, CivilightTheme.type.sec, civilightItem().padding(top = 14.dp, bottom = 2.dp), color = colors.ink3) }
            }
            items(group.rows, key = { it.sceneKey }) { row ->
                CoverRow(row, row.sceneKey in ui.selected, civilightItem()) { actions.onToggle(row.sceneKey) }
            }
        }
    }
}

@Composable
private fun Progress(ui: BulkUi, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ProgressLine(ui.place)
        ProgressLine(ui.time)
    }
}

/** `장소 | ▬▬▬ | 22/31`: label 34, the DS `ProgressBar` (its fill follows a change on `standard`), the figures 56 mono. */
@Composable
private fun ProgressLine(progress: BulkProgress) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(progress.label, type.small, Modifier.width(34.dp), color = colors.ink3, maxLines = 1)
        ProgressBar(progress.fraction.toFloat(), Modifier.weight(1f))
        ValueSwap("${progress.filled}/${progress.total}", type.dir, Modifier.width(56.dp), color = colors.ink3, textAlign = TextAlign.End, contentAlignment = Alignment.CenterEnd)
    }
}

/** `씬 2 · 오웬의 거래` with its values; the whole row toggles its checkbox (chosen rows: `sel` + `acc` bar). */
@Composable
private fun CoverRow(row: BulkRow, checked: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .selectedRowBackground(checked, colors.sel, colors.acc)
            .pressable(onClick = onToggle, scaleOnPress = false)
            .heightIn(min = 56.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckboxMark(checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CivilightText(row.label, type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Bold)), color = colors.ink, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (row.placeEmpty) EmptyValue(BulkCopy.NO_PLACE) else CivilightText(row.place, type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = colors.ink2, maxLines = 1)
                if (row.timeEmpty) EmptyValue(BulkCopy.NO_TIME) else CivilightText(row.time, type.dir, color = colors.ink2, maxLines = 1)
            }
        }
    }
}

/** `장소 없음` / `때 없음`: a 22 dp `edge` outlined tag in `ink3`. */
@Composable
private fun EmptyValue(text: String) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Box(Modifier.heightIn(min = 22.dp).insetOutline(colors.edge, 1.dp, shape).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        CivilightText(text, CivilightTheme.type.micro, color = colors.ink3, maxLines = 1)
    }
}

/** Inner table (`BulkWide`): progress, `씬 | 장소 | 때 | 제안` with `+ 장소` / `+ 때` cells, and the place map. */
@Composable
fun BulkWide(ui: BulkUi, doc: ProjectDoc, padding: PaddingValues, readOnly: Boolean, actions: BulkActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxSize().background(colors.bg).padding(start = 16.dp, end = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
        ) {
            item(key = "progress") { Progress(ui, civilightItem()) }
            item(key = "head") {
                Row(civilightItem().fillMaxWidth().padding(top = 12.dp).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(34.dp))
                    listOf(BulkCopy.COL_SCENE to 1.5f, BulkCopy.COL_PLACE to 1.1f, BulkCopy.COL_TIME to 1.1f, BulkCopy.COL_SUGGEST to 0.9f).forEach { (t, w) ->
                        CivilightText(t, type.sec, Modifier.weight(w), color = colors.ink3, maxLines = 1)
                    }
                }
            }
            items(ui.rows, key = { it.sceneKey }) { row -> WideRow(row, row.sceneKey in ui.selected, actions, civilightItem()) }
        }
        val counts = remember(doc, ui.rows, actions.locked) { placeCounts(doc, ui, actions.locked) }
        val map = remember(doc, counts, actions.locked) { MiniMaps.places(doc, labels = counts.mapValues { it.value.toString() }, prefer = counts.keys, exclude = BulkModel.hiddenPlaces(doc, actions.locked)) }
        Column(
            Modifier
                .width(252.dp)
                .padding(top = padding.calculateTopPadding())
                .clip(RoundedCornerShape(20.dp))
                .background(colors.tint)
                .insetOutline(colors.line, 1.dp, RoundedCornerShape(20.dp))
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CivilightText(BulkCopy.MAP_TITLE, type.sec, Modifier.padding(horizontal = 14.dp), color = colors.ink3)
            map?.let { MiniMapView(it, Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 1.dp), onPin = if (ui.canApply(readOnly)) actions.onPin else null) }
            CivilightText(BulkCopy.MAP_NOTE, type.small, Modifier.padding(horizontal = 14.dp), color = colors.ink3)
        }
    }
}

/** Scenes per place in the chapter (`6 · 5 · 4 · 3` on the map). */
private fun placeCounts(doc: ProjectDoc, ui: BulkUi, locked: Boolean): Map<String, Int> {
    val hidden = BulkModel.hiddenPlaces(doc, locked)
    return ui.rows.mapNotNull { doc.index.unit(it.sceneKey)?.place?.key }.filterNot { it in hidden }.groupingBy { it }.eachCount()
}

@Composable
private fun WideRow(row: BulkRow, checked: Boolean, actions: BulkActions, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val cells = row.tableCells
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .selectedRowBackground(checked, colors.sel, colors.acc, outset = 0.dp)
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(34.dp)) { CheckboxMark(checked, onCheckedChange = { actions.onToggle(row.sceneKey) }) }
        Column(Modifier.weight(1.5f)) {
            CivilightText(row.sceneRef, type.dir, color = colors.ink3, maxLines = 1)
            CivilightText(row.label.substringAfter(BulkCopy.SEP), type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Bold)), color = colors.ink, maxLines = 1)
        }
        Cell(cells[1], row.placeEmpty, Modifier.weight(1.1f)) { actions.onScene(row.sceneKey) }
        Cell(cells[2], row.timeEmpty, Modifier.weight(1.1f), mono = true) { actions.onScene(row.sceneKey) }
        Box(Modifier.weight(0.9f)) {
            if (row.suggestion == null) {
                CivilightText(BulkCopy.NONE, type.small, color = colors.ink3)
            } else {
                val shape = RoundedCornerShape(13.dp)
                Box(Modifier.heightIn(min = 26.dp).insetOutline(colors.acc, 1.dp, shape).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    CivilightText(cells[3], type.micro, color = colors.acc, maxLines = 1)
                }
            }
        }
    }
}

/** A value cell; an empty one is the `+ 장소` / `+ 때` button. Both open the scene's `장소와 때` sheet. */
@Composable
private fun Cell(text: String, empty: Boolean, modifier: Modifier, mono: Boolean = false, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Box(modifier.pressable(onClick = onClick, hitTarget = HitTarget.BUTTON, scaleOnPress = false).heightIn(min = 26.dp), contentAlignment = Alignment.CenterStart) {
        if (empty) {
            val shape = RoundedCornerShape(6.dp)
            Box(Modifier.heightIn(min = 26.dp).insetOutline(colors.edge, 1.dp, shape).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                CivilightText(text, type.micro, color = colors.ink3, maxLines = 1)
            }
        } else {
            CivilightText(text, if (mono) type.dir else type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = colors.ink2, maxLines = 2)
        }
    }
}

/**
 * The footer: cover = `고른 씬 4개 · 1-6 씬 2 · …` above `장소 정하기` · `때 정하기` (ghost) · `제안 받기 3`; inner =
 * one row `고른 씬 4개 | 장소 정하기 | 때 정하기 | 제안 받기 3`. Read-only projects can choose but not apply.
 */
@Composable
fun BulkFooter(ui: BulkUi, readOnly: Boolean, wide: Boolean, actions: BulkActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val can = ui.canApply(readOnly)
    // A fold changes the bar's height: it follows on the fold tokens (open = unfolded = the short inner bar).
    val barHeight by animateDpAsState(
        if (wide) 80.dp else 136.dp,
        LocalMotion.current.move(if (wide) CivilightMotion.foldOpen else CivilightMotion.foldClose),
        label = "bulkFooterHeight",
    )
    BottomGlassBar(barHeight = barHeight) {
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ValueSwap(ui.wideCount, type.small, Modifier.weight(1f), color = colors.ink3)
                CivilightButton(ui.placeButton, actions.onPlace, variant = ButtonVariant.COMPACT_GHOST, enabled = can)
                CivilightButton(ui.timeButton, actions.onTime, variant = ButtonVariant.COMPACT_GHOST, enabled = can)
                CivilightButton(ui.suggestButton, actions.onSuggest, variant = ButtonVariant.COMPACT_PRIMARY, enabled = ui.canSuggest())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.footer?.let { ValueSwap(it, type.small, Modifier.fillMaxWidth(), color = colors.ink3, maxLines = 2, textAlign = TextAlign.Center, contentAlignment = Alignment.Center) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CivilightButton(ui.placeButton, actions.onPlace, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = can)
                    CivilightButton(ui.timeButton, actions.onTime, Modifier.weight(1f), variant = ButtonVariant.GHOST, enabled = can)
                    CivilightButton(ui.suggestButton, actions.onSuggest, Modifier.weight(1.2f), enabled = ui.canSuggest())
                }
            }
        }
    }
}
