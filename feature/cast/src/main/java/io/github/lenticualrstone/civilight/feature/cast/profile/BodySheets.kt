package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.richtext.RichTextEditor
import io.github.lenticualrstone.civilight.ui.richtext.rememberRichTextEditorState
import kotlinx.serialization.Serializable

/** The prose bodies of a record: `말투` (CST-15) and `비밀` (CST-18). */
enum class BodyPart { SPEECH, SECRET }

/** Editing a record's prose body with the shared rich editor (`@` links, `( )`, `⌁` memo paragraphs kept). Feature-local. */
@Serializable
data class BodyEditSheet(val projectKey: Key, val characterKey: Key, val part: BodyPart) : SheetKey

@Composable
fun SheetScope.BodyEditContent(sheet: BodyEditSheet) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val c = doc.index.character(sheet.characterKey)
    if (c == null || store.readOnly) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val initial = remember(sheet) { if (sheet.part == BodyPart.SPEECH) c.speechBody else c.secretBody }
    val state = rememberRichTextEditorState(initial, key = sheet)
    val title = if (sheet.part == BodyPart.SPEECH) CastUiCopy.SPEECH else CastUiCopy.TABS[4]
    SheetHeader(title, onClose = ::close, subtitle = c.name, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        RichTextEditor(
            state = state,
            projectKey = sheet.projectKey,
            linkPickerTitle = CastUiCopy.LINK_PICKER_TITLE,
            openSheet = host::open,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            minHeight = 180.dp,
        )
    }
    Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        CivilightButton(CastUiCopy.BTN_SAVE, {
            val body = state.result
            editor.act(CastUiCopy.BTN_SAVE) { d, ctx ->
                if (sheet.part == BodyPart.SPEECH) CharacterEdits.setSpeech(d, sheet.characterKey, body, ctx) else CharacterEdits.setSecret(d, sheet.characterKey, body, ctx)
            }
            host.close()
        }, Modifier.weight(1f), enabled = state.changed)
    }
}
