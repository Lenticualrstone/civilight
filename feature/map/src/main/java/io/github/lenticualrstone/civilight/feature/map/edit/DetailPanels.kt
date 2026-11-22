package io.github.lenticualrstone.civilight.feature.map.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.world.map.DetailEdits
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.GlassCard
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/** `장소 연결` of a stamp (WLD-60). */
@Serializable
data class StampPlaceSheet(val stampKey: Key) : SheetKey

/**
 * `도장` tab (WLD-60, board `MapDetail`): the 12 stamps, the hint `탭 → 놓기 · 모서리 → 크기 · 위
 * 손잡이 → 회전` and `크기 | 1 · 2 · 3` for the next stamp. Choosing a stamp also chooses the tool.
 */
@Composable
fun ColumnScope.StampPalette(kind: StampKind, size: Int, onKind: (StampKind) -> Unit, onSize: (Int) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    ChipFlow(Modifier.fillMaxWidth()) {
        StampKind.entries.forEach { k -> FilterChip(DetailEdits.stampName(k), k == kind, { onKind(k) }) }
    }
    CivilightText(MapCopy.STAMP_HINT, type.small, color = colors.ink3)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CivilightText(MapCopy.STAMP_SIZE, type.label, color = colors.ink3)
        SegmentedControl(MapCopy.STAMP_SIZES, size - 1, { onSize(it + 1) }, Modifier.weight(1f))
    }
}

/** `범례` tab: `산 · 3`, `강 · 2` …. */
@Composable
fun ColumnScope.LegendPanel(rows: List<String>) {
    val colors = CivilightTheme.colors
    if (rows.isEmpty()) CivilightText(MapCopy.NONE, CivilightTheme.type.small, color = colors.ink3)
    rows.forEach { CivilightText(it, CivilightTheme.type.label, maxLines = 1) }
}

/**
 * WLD-65 (rule 11-2): a vertex moved under the lens. `이 값 고치기` (primary, default) and
 * `여기서부터 바뀜 · 1-7`, each with its small line, the past-lens banner when there is one, and
 * `취소`. The labels are `domain:story` `LensEdit`'s.
 */
@Composable
fun ShapeChoiceBar(choice: EditChoice.TwoButtons, readOnly: Boolean, onOverwrite: () -> Unit, onFromHere: () -> Unit, onCancel: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    GlassCard(modifier) {
        choice.banner?.let { CivilightText(it.text, type.small, color = colors.acc, maxLines = 2) }
        CivilightButton(choice.overwrite.label, onOverwrite, Modifier.fillMaxWidth(), enabled = !readOnly)
        CivilightText(choice.overwrite.sub, type.small, color = colors.ink3, maxLines = 1)
        CivilightButton(choice.fromHere.label, onFromHere, Modifier.fillMaxWidth(), variant = ButtonVariant.GHOST, enabled = !readOnly)
        CivilightText(choice.fromHere.sub, type.small, color = colors.ink3, maxLines = 1)
        CivilightButton(MapCopy.BTN_CANCEL, onCancel, Modifier.fillMaxWidth(), variant = ButtonVariant.COMPACT_GHOST)
    }
}
