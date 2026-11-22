package io.github.lenticualrstone.civilight.feature.script.renumber

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.Numbering
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.navigation.RenumberSheet
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.launch

/**
 * `번호 다시 매기기 · 1-7` (SCR-22, board `RTRenumber`): the shared `RenumberSheet`, opened from the
 * story structure's episode ⋯ and the script's command field. Nothing changes before `다시 매기기`,
 * which is one undo step; `취소` closes. It writes through the project's store (not a screen's view
 * model), because it opens over other areas' screens too.
 */
@Composable
fun SheetScope.RenumberSheetContent(sheet: RenumberSheet, deps: FeatureDeps) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val preview = remember(doc, sheet.episodeKey) { RenumberModel.preview(doc, sheet.episodeKey) }
    SheetHeader(title = preview?.title ?: ScriptCopy.RENUMBER, onClose = ::close, subtitle = preview?.subtitle)
    if (preview == null) return
    SheetScrollBody(Modifier.padding(top = 12.dp)) { RenumberBody(preview) }
    val (cancel, apply) = preview.buttons
    SheetActions {
        CivilightButton(cancel, { host.close() }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            text = apply,
            onClick = {
                val title = preview.title
                val ctx = EditContext(now = deps.clock.now(), zone = deps.clock.zone, device = deps.stores.deviceId.value, newKey = Keys::newKey)
                deps.appScope.launch {
                    try {
                        store.update(title) { d -> RenumberModel.apply(d, sheet.episodeKey, ctx) }
                    } catch (e: ReadOnlyProjectException) {
                        // The example project: nothing changes.
                    }
                }
                host.close()
            },
            modifier = Modifier.weight(1.6f),
            subText = if (store.readOnly) ScriptCommands.READ_ONLY else null,
            enabled = RenumberModel.canApply(preview, store.readOnly),
        )
    }
}

/** Figures, `바뀌는 번호 · 7` rows, the `외 n개` line and the footer box, as `RTRenumber` draws them. */
@Composable
private fun RenumberBody(preview: Numbering.RenumberPreview) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val figure = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        preview.figures.forEach { (value, label) ->
            Column(Modifier.weight(1f).clip(figure).background(colors.tint).padding(horizontal = 12.dp, vertical = 10.dp)) {
                CivilightText(value, type.stat, color = colors.ink, maxLines = 1)
                CivilightText(label, type.small, color = colors.ink3, maxLines = 1)
            }
        }
    }
    if (preview.changed > 0) {
        CivilightText(preview.sectionTitle, type.sec, Modifier.padding(top = 16.dp, bottom = 6.dp), color = colors.ink3)
        preview.listedRows.forEach { row ->
            Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CivilightText(
                        row.change, type.number.merge(TextStyle(fontSize = 13.sp)), Modifier.widthIn(min = 122.dp),
                        color = colors.ink, maxLines = 1,
                    )
                    CivilightText(row.where, type.command.merge(TextStyle(fontSize = 13.sp)), Modifier.weight(1f), color = colors.ink2, maxLines = 1)
                }
                row.voiceLine?.let { CivilightText(it, type.small, Modifier.padding(start = 132.dp), color = colors.ink3, maxLines = 1) }
            }
        }
        preview.restLine?.let {
            CivilightText(it, type.small, Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 10.dp), color = colors.ink3)
        }
    }
    val box = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(box)
            .background(colors.tint)
            .insetOutline(colors.edge, 1.dp, box)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        preview.footer.forEachIndexed { i, line ->
            if (i == 0) {
                CivilightText(line, type.command.merge(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)), color = colors.ink)
            } else {
                CivilightText(line, type.small, Modifier.padding(top = 2.dp), color = colors.ink3)
            }
        }
    }
}
