package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** `이름` of a character (fixed field). */
@Serializable
data class NameEditSheet(val projectKey: Key, val characterKey: Key) : SheetKey

/** `한 줄 소개` (fixed field). */
@Serializable
data class OneLinerEditSheet(val projectKey: Key, val characterKey: Key) : SheetKey

/** `태그` (CST-12 [PROPOSED]: project tags as checkable chips plus a line to add a new tag). */
@Serializable
data class TagsEditSheet(val projectKey: Key, val characterKey: Key) : SheetKey

/** One text line and `이 값 고치기` (fields without history, CST-09). */
@Composable
private fun SheetScope.TextValueSheet(
    title: String,
    initial: String,
    singleLine: Boolean,
    canSave: (String) -> Boolean,
    onSave: (String) -> Unit,
) {
    val host = this
    var text by rememberSaveable { mutableStateOf(initial) }
    val readOnly = currentProjectStore.readOnly
    SheetHeader(title, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    CivilightTextField(text, { text = it }, Modifier.fillMaxWidth().padding(top = 14.dp), singleLine = singleLine, enabled = !readOnly)
    if (!readOnly) {
        SheetActions {
            CivilightButton(CastUiCopy.BTN_CORRECT, {
                onSave(text)
                host.close()
            }, Modifier.weight(1f), enabled = canSave(text))
        }
    }
}

@Composable
fun SheetScope.NameSheetContent(sheet: NameEditSheet) {
    val doc by currentProjectStore.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val current = doc.index.character(sheet.characterKey)?.name
    if (current == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    TextValueSheet(CastUiCopy.NAME, current, singleLine = true, canSave = { FixedFieldEdits.canRename(it, current) }) { name ->
        editor.act(CastUiCopy.BTN_CORRECT) { d, ctx -> FixedFieldEdits.rename(d, sheet.characterKey, name, ctx) }
    }
}

@Composable
fun SheetScope.OneLinerSheetContent(sheet: OneLinerEditSheet) {
    val doc by currentProjectStore.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val c = doc.index.character(sheet.characterKey)
    if (c == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val current = c.oneLiner.orEmpty()
    TextValueSheet(CastUiCopy.ONE_LINER, current, singleLine = false, canSave = { it.trim() != current }) { text ->
        editor.act(CastUiCopy.BTN_CORRECT) { d, ctx -> FixedFieldEdits.setOneLiner(d, sheet.characterKey, text, ctx) }
    }
}

private val PicksSaver: Saver<List<TagPick>, String> = Saver(
    save = { Json.encodeToString(ListSerializer(TagPick.serializer()), it) },
    restore = { runCatching { Json.decodeFromString(ListSerializer(TagPick.serializer()), it) }.getOrNull() },
)

/** `태그`: every project tag as a chip (on = the character has it), new names added below. */
@Composable
fun SheetScope.TagsSheetContent(sheet: TagsEditSheet) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    if (doc.index.character(sheet.characterKey) == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val initial = remember(sheet) { FixedFieldEdits.picks(doc, sheet.characterKey) }
    var picks by rememberSaveable(stateSaver = PicksSaver) { mutableStateOf(initial) }
    var name by rememberSaveable { mutableStateOf("") }
    val editable = !store.readOnly
    SheetHeader(CastUiCopy.TAGS, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody(Modifier.padding(top = 14.dp)) {
        ChipFlow {
            for (tag in CharacterEdits.tags(doc)) {
                FilterChip(tag.name, picks.any { it.key == tag.key }, { picks = FixedFieldEdits.toggle(picks, tag.key, tag.name) }, enabled = editable)
            }
            for (pick in picks.filter { it.key == null }) {
                FilterChip(pick.name, true, { picks = picks - pick }, enabled = editable)
            }
        }
        if (editable) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                CivilightTextField(name, { name = it }, Modifier.weight(1f), label = CastUiCopy.NAME)
                CivilightButton(CastUiCopy.BTN_INSERT, {
                    picks = FixedFieldEdits.addName(doc, picks, name)
                    name = ""
                }, variant = ButtonVariant.GHOST, enabled = name.isNotBlank())
            }
        }
    }
    if (editable) {
        SheetActions {
            CivilightButton(CastUiCopy.BTN_CORRECT, {
                val saved = picks
                editor.act(CastUiCopy.BTN_CORRECT) { d, ctx -> FixedFieldEdits.apply(d, sheet.characterKey, saved, ctx) }
                host.close()
            }, Modifier.weight(1f), enabled = picks != initial)
        }
    }
}
