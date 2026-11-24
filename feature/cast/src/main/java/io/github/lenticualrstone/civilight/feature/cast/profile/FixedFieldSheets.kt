package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.CreateRow
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreate
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreateCard
import io.github.lenticualrstone.civilight.feature.cast.create.CreateCardCodec
import io.github.lenticualrstone.civilight.ui.project.EditResult
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

/** One text line and `저장` (fields without history, CST-09). */
@Composable
private fun SheetScope.TextValueSheet(
    title: String,
    initial: String,
    singleLine: Boolean,
    canSave: (String) -> Boolean,
    below: (@Composable () -> Unit)? = null,
    onSave: (String) -> Unit,
) {
    val host = this
    var text by rememberSaveable { mutableStateOf(initial) }
    val readOnly = currentProjectStore.readOnly
    SheetHeader(title, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    CivilightTextField(text, { text = it }, Modifier.fillMaxWidth().padding(top = 14.dp), singleLine = singleLine, enabled = !readOnly)
    below?.invoke()
    if (!readOnly) {
        SheetActions {
            CivilightButton(CastUiCopy.BTN_SAVE, {
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
    // (0.2.0, N-28) `원고 속 n곳 · 바꾸기` under the name: the stored name's manuscript hits (locked hidden scenes
    // are neither counted nor listed); it opens the script's 바꾸기 with `찾을 말` filled in. Not in read-only.
    val gate = io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate(sheet.projectKey)
    val hits = remember(doc, current, gate.open) { ManuscriptHits.count(doc, current, gate.open) }
    val readOnly = currentProjectStore.readOnly
    val below: (@Composable () -> Unit)? = if (readOnly || hits == 0) null else ({
        TextAction(CastUiCopy.manuscriptReplace(hits), { open(io.github.lenticualrstone.civilight.navigation.ManuscriptReplaceSheet(sheet.projectKey, current)) }, Modifier.padding(top = 6.dp))
    })
    TextValueSheet(CastUiCopy.NAME, current, singleLine = true, canSave = { FixedFieldEdits.canRename(it, current) }, below = below) { name ->
        editor.act(CastUiCopy.BTN_SAVE) { d, ctx -> FixedFieldEdits.rename(d, sheet.characterKey, name, ctx) }
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
        editor.act(CastUiCopy.BTN_SAVE) { d, ctx -> FixedFieldEdits.setOneLiner(d, sheet.characterKey, text, ctx) }
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
    var initial by remember(sheet) { mutableStateOf(FixedFieldEdits.picks(doc, sheet.characterKey)) }
    var picks by rememberSaveable(stateSaver = PicksSaver) { mutableStateOf(initial) }
    var creating by rememberSaveable(stateSaver = TagFieldCardSaver) { mutableStateOf<CreateCardState?>(null) }
    val editable = !store.readOnly
    SheetHeader(CastUiCopy.TAGS, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody(Modifier.padding(top = 14.dp)) {
        // U-15.4 (CST-75): `＋ 새 태그` on top opens the create card in picker mode.
        if (editable) CreateRow(CreateCard.rowLabel(CreateKind.TAG), { creating = CreateCardState.open(CreateKind.TAG, CreateCardMode.PICKER) }, Modifier.padding(bottom = 8.dp))
        ChipFlow {
            for (tag in CharacterEdits.tags(doc)) {
                FilterChip(tag.name, picks.any { it.key == tag.key }, { picks = FixedFieldEdits.toggle(picks, tag.key, tag.name) }, enabled = editable)
            }
            for (pick in picks.filter { it.key == null }) {
                FilterChip(pick.name, true, { picks = picks - pick }, enabled = editable)
            }
        }
    }
    var tagBusy by remember { mutableStateOf(false) }
    creating?.let { card ->
        CastCreateCard(
            state = card,
            onChange = { creating = it },
            onCancel = { creating = null },
            onCreate = {
                val made = card
                tagBusy = true
                var created: Key? = null
                // CST-75: the new tag is created and checked on the character in one write (one undo step).
                // The card closes only once the write applied; otherwise the typed name stays.
                editor.act(CreateCardCopy.CREATE, then = { result ->
                    tagBusy = false
                    if (result == EditResult.APPLIED) {
                        creating = null
                        created?.let { k ->
                            picks = FixedFieldEdits.created(picks, k, made.name.trim())
                            initial = FixedFieldEdits.created(initial, k, made.name.trim())
                        }
                    }
                }) { d, ctx -> CastCreate.createTagOn(d, sheet.characterKey, made, ctx.toAction())?.let { (next, k) -> created = k; next } ?: d }
            },
            canCreate = !tagBusy && CastCreate.canCreateTag(doc, card),
        )
    }
    if (editable) {
        SheetActions {
            CivilightButton(CastUiCopy.BTN_SAVE, {
                val saved = picks
                editor.act(CastUiCopy.BTN_SAVE) { d, ctx -> FixedFieldEdits.apply(d, sheet.characterKey, saved, ctx) }
                host.close()
            }, Modifier.weight(1f), enabled = picks != initial)
        }
    }
}

private val TagFieldCardSaver: Saver<CreateCardState?, String> = Saver(
    save = { it?.let(CreateCardCodec::encode) ?: "" },
    restore = { CreateCardCodec.decode(it) },
)

/** (0.2.0, N-28) The manuscript hits of a name (the count of `원고 속 n곳 · 바꾸기`), as the 바꾸기 sheet lists them. */
object ManuscriptHits {
    fun count(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc, name: String, unlocked: Boolean): Int =
        if (name.isBlank()) 0 else io.github.lenticualrstone.civilight.domain.script.Replace.find(doc, name, unlocked).size
}
