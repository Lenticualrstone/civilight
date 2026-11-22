package io.github.lenticualrstone.civilight.feature.cast.gametext

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.richtext.RichTextEditor
import io.github.lenticualrstone.civilight.ui.richtext.rememberRichTextEditorState
import kotlinx.serialization.Serializable

/** A voice line's sheet (CST-16); [lineKey] null = `+`, a new line at the end. Feature-local. */
@Serializable
data class VoiceLineSheet(val projectKey: Key, val characterKey: Key, val lineKey: Key? = null) : SheetKey

/**
 * One voice line: its slot name (`이름`: `임명 · 대화 1 …`) and its text with `@` links (shared
 * rich editor). `넣기` adds a new line, `이 값 고치기` saves an existing one. The state label is
 * toggled on the tab itself. Removing a line is not offered (no source draws it; a plain removal
 * would bypass the trash).
 */
@Composable
fun SheetScope.VoiceLineContent(sheet: VoiceLineSheet) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val line = sheet.lineKey?.let { GameTextModel.line(doc, sheet.characterKey, it) }
    if (sheet.lineKey != null && line == null || doc.index.character(sheet.characterKey) == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    var title by rememberSaveable(sheet) { mutableStateOf(line?.title.orEmpty()) }
    val text = rememberRichTextEditorState(remember(sheet) { line?.text.orEmpty() }, key = sheet)
    SheetHeader(line?.title ?: CastUiCopy.NEW_VOICE_LINE, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        CivilightTextField(title, { title = it }, Modifier.fillMaxWidth().padding(top = 14.dp), label = CastUiCopy.NAME, enabled = !store.readOnly)
        RichTextEditor(
            state = text,
            projectKey = sheet.projectKey,
            linkPickerTitle = CastUiCopy.TODO_COPY_CAST_LINK_PICKER_TITLE,
            openSheet = host::open,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            enabled = !store.readOnly,
        )
    }
    if (store.readOnly) return
    SheetActions {
        val label = if (line == null) CastUiCopy.BTN_INSERT else CastUiCopy.BTN_CORRECT
        val changed = line == null || title.trim() != line.title || text.changed
        CivilightButton(label, {
            val savedTitle = title
            val savedText = text.result
            editor.act(label) { d, ctx ->
                if (line == null) {
                    GameTextModel.addLine(d, sheet.characterKey, savedTitle, savedText, ctx)?.first ?: d
                } else {
                    GameTextModel.saveLine(d, line.key, savedTitle, savedText, ctx)
                }
            }
            host.close()
        }, Modifier.weight(1f), enabled = changed && GameTextModel.canSaveLine(title))
    }
}

