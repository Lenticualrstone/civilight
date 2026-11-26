package io.github.lenticualrstone.civilight.feature.script.layers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.select.MoveCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** `표시` (SCR-26), opened by the layers button in the header or the command row. */
@Serializable
data class LayersSheetKey(val projectKey: Key) : SheetKey

/**
 * `표시` sheet (board `Layers`): title and `닫기`, `보기에만 적용 · 파일 내용은 그대로`, one switch
 * row per layer, then `미리 보기` with `쓰기 · 대사만 · 낭독` (`낭독` disabled, SCR-28). Changes apply
 * at once; the sheet stays open.
 */
@Composable
fun SheetScope.LayersSheet(
    layers: ScriptLayers,
    preview: ScriptPreview,
    onLayer: (LayerRow, Boolean) -> Unit,
    onPreview: (ScriptPreview) -> Unit,
    /** SCR-28: the `낭독` chip is dimmed when the episode has no step. */
    readAloudEnabled: Boolean = true,
    /** Visible way into `블록 고르기` (지우기 · 옮기기 · 복사) besides swipe and long-press; null in read-only. */
    onSelectBlocks: (() -> Unit)? = null,
) {
    SheetHeader(title = ScriptCopy.LAYERS_TITLE, onClose = ::close, subtitle = ScriptCopy.LAYERS_SUB)
    SheetScrollBody(Modifier.padding(top = 6.dp, bottom = 16.dp)) {
        LayersModel.ROWS.forEach { row ->
            SwitchRow(
                title = row.title,
                subtitle = row.sub,
                checked = LayersModel.isOn(layers, row),
                onCheckedChange = { on -> onLayer(row, on) },
            )
        }
        Spacer(Modifier.height(18.dp))
        SectionLabel(ScriptCopy.PREVIEW_SECTION)
        Spacer(Modifier.height(8.dp))
        val chosen = LayersModel.selectedChip(preview)
        ChipFlow {
            LayersModel.chips(readAloudEnabled).forEach { chip ->
                FilterChip(
                    label = chip.label,
                    selected = chip.preview == chosen,
                    onClick = { onPreview(chip.preview) },
                    enabled = chip.enabled,
                )
            }
        }
        if (onSelectBlocks != null) {
            Spacer(Modifier.height(18.dp))
            ListRow(
                title = MoveCopy.TITLE,
                subtitle = ScriptCopy.SELECT_BLOCKS_SUB,
                compact = true,
                leading = { CivilightIcon(IconId.Check, contentDescription = null, size = 18.dp, tint = CivilightTheme.colors.acc) },
                onClick = {
                    this@LayersSheet.close()
                    onSelectBlocks()
                },
            )
        }
    }
}
