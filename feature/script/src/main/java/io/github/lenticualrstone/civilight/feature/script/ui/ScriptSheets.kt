package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerTarget
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.LinkPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import kotlinx.serialization.Serializable

// Feature-local sheets of the script screen (shell.md §5). Keys are saved with the screen.

/** `블록 추가` (SCR-12); [headerScene] is the scene under the header (insert target without a block). */
@Serializable
data class BlockAddSheetKey(val projectKey: Key, val headerScene: Key? = null) : SheetKey

/** `화자` (SCR-19) for [blockKey], or for the next dialogue insert when it is null. */
@Serializable
data class SpeakerSheetKey(
    val projectKey: Key,
    val blockKey: Key? = null,
    val speakerKey: Key? = null,
    val sceneKey: Key? = null,
) : SheetKey {
    val target: SpeakerTarget get() = SpeakerTarget(blockKey, speakerKey, sceneKey)

    companion object {
        fun of(projectKey: Key, target: SpeakerTarget) = SpeakerSheetKey(projectKey, target.blockKey, target.speakerKey, target.sceneKey)
    }
}

/** `@` link picker at raw [rawOffset] of [blockKey], where the caret was (SCR-16 → SCR-55). */
@Serializable
data class LinkSheetKey(val projectKey: Key, val blockKey: Key, val rawOffset: Int) : SheetKey

/** Position-label menu: `렌즈를 이 씬으로 맞추기` (SCR-02). */
@Serializable
data class PositionSheetKey(val projectKey: Key, val sceneKey: Key, val label: String) : SheetKey

/**
 * `블록 추가` (SCR-12, board `Blocks`): rows with description and shortcut; the current block's kind
 * is highlighted; read-only projects dim every row with `읽기 전용`.
 */
@Composable
fun SheetScope.BlockAddSheet(
    subline: String,
    currentKind: BlockKind?,
    readOnly: Boolean,
    onPick: (ScriptCommands.AddRow) -> Unit,
) {
    SheetHeader(title = ScriptCommands.SHEET_TITLE, onClose = ::close, subtitle = subline)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        ScriptCommands.ADD_ROWS.forEach { row ->
            AddRowItem(row, selected = row.kind != null && row.kind == currentKind, readOnly = readOnly) {
                this@BlockAddSheet.close()
                onPick(row)
            }
        }
    }
}

@Composable
private fun AddRowItem(row: ScriptCommands.AddRow, selected: Boolean, readOnly: Boolean, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .selectedRowBackground(selected && !readOnly, colors.sel, colors.acc)
            .pressable(PressAction.Click(onClick), enabled = !readOnly, role = Role.Button, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .alpha(if (readOnly) 0.38f else 1f)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(row.name, type.itemTitle, color = colors.ink, maxLines = 1)
            CivilightText(row.description, type.small, color = colors.ink3, maxLines = 1)
        }
        CivilightText(if (readOnly) ScriptCommands.READ_ONLY else row.shortcut, type.number, color = colors.ink3, maxLines = 1)
    }
}

/**
 * `화자` (SCR-19): the shell's picker with the script's data: this scene's cast first, the project's
 * recent speakers, the block's current speaker marked, and `표시 이름 ???` when the display name at
 * this block differs from the name.
 */
@Composable
fun SheetScope.SpeakerSheet(
    key: SpeakerSheetKey,
    doc: ProjectDoc,
    sceneCast: List<Key>,
    onPick: (Key) -> Unit,
    onRestoreDeleted: ((Key, Boolean) -> Unit)?,
) {
    val gate = rememberHiddenGate(key.projectKey)
    val blockKey = key.blockKey
    CharacterPickerSheet(
        doc = doc,
        gate = gate,
        onPick = { picked ->
            close()
            onPick(picked)
        },
        onClose = ::close,
        sceneCast = sceneCast,
        recent = doc.app.view.script.recentSpeakers,
        currentKey = key.speakerKey,
        onRestoreDeleted = onRestoreDeleted?.let { restore ->
            { characterKey: Key ->
                close()
                restore(characterKey, gate.open)
            }
        },
        subtitleOf = { c ->
            val display = blockKey?.let { Manuscript.displayNameAt(doc, c.key, it) }
            if (display != null && display != c.name) "${ScriptCopy.DISPLAY_NAME} $display" else PickerModels.defaultSubtitle(doc, c)
        },
    )
}

/** `@` at the caret (SCR-16): people, terms and places; the pick becomes a link run. */
@Composable
fun SheetScope.LinkSheet(key: LinkSheetKey, doc: ProjectDoc, onPick: (PickKind, Key, String) -> Unit) {
    LinkPickerSheet(
        doc = doc,
        gate = rememberHiddenGate(key.projectKey),
        title = ScriptCopy.LINK_PICKER_TITLE,
        onPick = { kind, target, text ->
            close()
            onPick(kind, target, text)
        },
        onClose = ::close,
    )
}

/** The position label's one-row menu (SCR-02). */
@Composable
fun SheetScope.PositionSheet(key: PositionSheetKey, onSetLens: (Key) -> Unit) {
    SheetHeader(title = key.label, onClose = ::close)
    ListRow(
        title = ScriptCopy.SET_LENS_TO_SCENE,
        modifier = Modifier.padding(top = 8.dp),
        compact = true,
        onClick = {
            close()
            onSetLens(key.sceneKey)
        },
    )
}
