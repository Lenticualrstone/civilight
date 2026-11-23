package io.github.lenticualrstone.civilight.feature.cast.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.DecorationRange
import io.github.lenticualrstone.civilight.designsystem.text.InlineDecoration
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.navigation.DeleteImpactSheet
import io.github.lenticualrstone.civilight.navigation.MergeItemsSheet
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad

/**
 * The shared `지우기 · 영향` sheet (`RCDeleteImpact`, CST-40, S21) for a character, place, term or
 * faction; world cards open it too. Counts only: nothing to type, nothing to fix first. An item with
 * no references and nothing owned is deleted at once with its toast and the sheet closes
 * (TC-20-03). The `중복이면` row's `다른 인물과 합치기 ›` opens the shared merge sheet (CST-45).
 *
 * Opened for an item that is already in the trash or left as a name tag, the same key shows that
 * item's check row instead (CST-42, CST-44; [DeletedItemContent]): counts, the bulk changes,
 * `되살리기` and `이대로 두기`. An item that is gone with nothing referencing it just closes the sheet.
 */
@Composable
fun SheetScope.DeleteImpactContent(sheet: DeleteImpactSheet) {
    val deps = LocalFeatureDeps.current
    val editor = rememberCastEditor(sheet.projectKey)
    val load by editor.state.collectAsState()
    val recorder by deps.stores.recorder.state.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    val flow = rememberItemTrash(sheet.projectKey)
    // Set once this sheet closes itself, so the item leaving the doc cannot close it (or the sheet below) twice.
    var done by remember { mutableStateOf(false) }
    if (done) return
    val ready = load as? ProjectLoad.Ready ?: return
    if (!recorder.loaded) return
    val impact = remember(ready.doc, recorder.library) { ItemTrash.impact(ready.doc, sheet.kind, sheet.itemKey, recorder.library) }
    // Whether the item was live when the sheet opened: once it leaves (deleted, merged) the sheet
    // closes instead of turning into the deleted item's check row.
    val openedLive = rememberSaveable { impact != null }
    if (impact == null && !openedLive) {
        DeletedItemContent(sheet, ready.doc, ready.readOnly, recorder.library) {
            done = true
            close()
        }
        return
    }
    if (impact == null || ready.readOnly) {
        LaunchedEffect(Unit) {
            done = true
            close()
        }
        return
    }
    if (!impact.needsSheet) {
        LaunchedEffect(Unit) {
            done = true
            flow.delete(sheet.kind, sheet.itemKey)
            close()
        }
        return
    }
    val gameTools = io.github.lenticualrstone.civilight.model.game.GameTools.on(ready.doc)
    val view = remember(impact, unlock, gameTools) { ItemTrash.sheet(sheet.kind, impact, hiddenLocked = !unlock.itemsOpen(sheet.projectKey), gameTools = gameTools) }
    ImpactSheetBody(
        view,
        onCancel = {
            done = true
            close()
        },
        onMerge = {
            done = true
            close()
            open(MergeItemsSheet(sheet.projectKey, sheet.kind, sheet.itemKey, sheet.itemKey))
        },
    ) {
        done = true
        flow.delete(sheet.kind, sheet.itemKey)
        close()
    }
}

@Composable
private fun SheetScope.ImpactSheetBody(view: ImpactSheetView, onCancel: () -> Unit, onMerge: () -> Unit, onConfirm: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetHeader(view.title, onClose = onCancel, subtitle = view.sub, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        if (view.owned.isNotEmpty()) {
            SectionLabel(view.ownedTitle, Modifier.padding(top = 16.dp, bottom = 4.dp))
            for (row in view.owned) ImpactRow(row)
        }
        if (view.staying.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(view.stayingTitle, Modifier.weight(1f))
                DottedName(view.dottedName)
            }
            for (row in view.staying) ImpactRow(row)
        }
        ListRow(view.checkRow.first, subtitle = view.checkRow.second, compact = true, divider = io.github.lenticualrstone.civilight.designsystem.component.RowDivider.NONE)
        ListRow(
            view.duplicateRow.first,
            subtitle = view.duplicateRow.second,
            compact = true,
            divider = io.github.lenticualrstone.civilight.designsystem.component.RowDivider.NONE,
            trailing = view.mergeAction?.let { label -> { TextAction(label, onMerge, color = colors.acc, style = type.small) } },
        )
        CivilightText(view.footnote, type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
    }
    SheetActions {
        CivilightButton(view.cancelLabel, onCancel, Modifier.weight(1f), ButtonVariant.GHOST)
        CivilightButton(view.confirmLabel, onConfirm, Modifier.weight(1f))
    }
}

/** `노아 ◇`: the name dotted-underlined in `ink2`, then `◇` in `ink3` (DS-18). */
@Composable
private fun DottedName(name: String) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        DecoratedText(
            AnnotatedString(name),
            type.itemTitleCompact,
            color = colors.ink2,
            decorations = listOf(DecorationRange(0, name.length, InlineDecoration.DELETED_REF)),
            maxLines = 1,
        )
        CivilightText(CastCopy.SOFT_MARK, type.label, color = colors.ink3)
    }
}

@Composable
private fun ImpactRow(row: ImpactRowView) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 44.dp).padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(row.label, type.command, color = colors.ink)
            row.sub?.let { CivilightText(it, type.small, color = colors.ink3) }
        }
        CivilightText(row.count, type.number.merge(TextStyle(fontSize = 15.sp)), color = colors.ink)
    }
}
