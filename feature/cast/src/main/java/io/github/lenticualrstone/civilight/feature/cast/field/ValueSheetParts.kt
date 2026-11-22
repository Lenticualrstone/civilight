package io.github.lenticualrstone.civilight.feature.cast.field

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.LensEditBanner
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastEditorViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import kotlinx.serialization.json.Json

/** View-model key of the shared cast sheets inside whichever entry opened them. */
private const val EDITOR_KEY = "feature.cast.editor"

/** The editor view model of the shared cast sheets, from the entry the sheet is drawn in. */
@Composable
fun rememberCastEditor(projectKey: Key): CastEditorViewModel {
    val deps = LocalFeatureDeps.current
    return entryViewModel(key = "$EDITOR_KEY.$projectKey") { CastEditorViewModel(projectKey, deps.projects, deps::editContext) }
}

/** Saves a [FieldDraft] with its sheet (process death included) as JSON. */
val FieldDraftSaver: Saver<FieldDraft, String> = Saver(
    save = { Json.encodeToString(FieldDraft.serializer(), it) },
    restore = { runCatching { Json.decodeFromString(FieldDraft.serializer(), it) }.getOrNull() },
)

/**
 * The top of a value sheet with history (`RTEditTwo`, `LensEdit`): the past-lens banner, the
 * `지금 값` row and the history line.
 */
@Composable
fun TwoButtonHead(mode: EditMode.TwoButtons, onLatest: () -> Unit) {
    val colors = CivilightTheme.colors
    val sheet = mode.sheet
    sheet.banner?.let { banner ->
        LensEditBanner(banner.title, banner.action, onLatest, Modifier.padding(top = 10.dp), subtitle = banner.sub)
    }
    FieldTableRow(CastUiCopy.CURRENT_VALUE, sheet.currentLine, Modifier.padding(top = 8.dp))
    if (sheet.historyLine.isNotEmpty()) CivilightText(sheet.historyLine, CivilightTheme.type.small, color = colors.ink3)
}

/**
 * Save buttons of a value sheet: one primary `이 값 고치기` ([EditMode.Plain]) or the two stacked
 * buttons with their second lines (`이 값 고치기` primary, `여기서부터 바뀜 · 1-7` ghost).
 */
@Composable
fun SaveButtons(mode: EditMode, enabled: Boolean, onSave: (SaveChoice) -> Unit) {
    val colors = CivilightTheme.colors
    when (mode) {
        EditMode.Plain -> Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            CivilightButton(CastUiCopy.BTN_CORRECT, { onSave(SaveChoice.PLAIN) }, Modifier.weight(1f), enabled = enabled)
        }
        is EditMode.TwoButtons -> Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val sheet = mode.sheet
            CivilightButton(sheet.correctLabel, { onSave(SaveChoice.CORRECT) }, Modifier.fillMaxWidth(), ButtonVariant.TWO_LINE, subText = sheet.correctSub, enabled = enabled)
            if (sheet.changeLabel != null) {
                CivilightButton(
                    sheet.changeLabel!!, { onSave(SaveChoice.FROM_HERE) }, Modifier.fillMaxWidth(), ButtonVariant.TWO_LINE_GHOST,
                    subText = sheet.changeSub ?: "", enabled = enabled,
                )
            }
            sheet.saveNote?.let { CivilightText(it, CivilightTheme.type.small, color = colors.ink3) }
        }
    }
}

/** One row of the history sheet: mono point | value (bold) and note; in effect `sel` + `acc` bar. */
@Composable
fun HistoryRow(row: HistoryRowView) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .selectedRowBackground(row.inEffect, colors.sel, colors.acc)
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CivilightText(row.point, type.number, Modifier.widthIn(min = 40.dp), color = colors.ink3, maxLines = 1)
        Column(Modifier.weight(1f)) {
            CivilightText(row.value, type.itemTitle, color = if (row.dim) colors.ink3 else colors.ink)
            row.note?.let { CivilightText(it, type.small, color = colors.ink3) }
        }
    }
}
