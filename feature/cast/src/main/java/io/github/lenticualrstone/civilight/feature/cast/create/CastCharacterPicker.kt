package io.github.lenticualrstone.civilight.feature.cast.create

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.ItemPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickerChip
import io.github.lenticualrstone.civilight.ui.picker.PickerCopy
import io.github.lenticualrstone.civilight.ui.picker.PickerCreate
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore

/**
 * The cast's character picker (feature-local, until `core:ui` `CharacterPickerSheet` forwards a create
 * row): search, the chips of the **live** tags (N-44), sections and the locked-hidden row as the shared
 * sheet draws them, plus (U-15.3, CST-66) the `＋ 새 인물` row on top in an editable project. With a query
 * that matches nothing the row reads `＋ ‘세라’ 새 인물` and the card opens with the query as the name. The
 * card shows `취소 · 만들기`; `만들기` creates the character (one undo step `만들기`) and returns it as the
 * picked value. [creatable] false (the merge target, CST-45) leaves the row out.
 */
@Composable
fun ColumnScope.CastCharacterPicker(
    projectKey: Key,
    doc: ProjectDoc,
    gate: HiddenGate,
    title: String,
    onPick: (Key) -> Unit,
    onClose: () -> Unit,
    creatable: Boolean = true,
    currentKey: Key? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver<CreateCardState?, String>(
        save = { it?.let(CreateCardCodec::encode) ?: "" },
        restore = { CreateCardCodec.decode(it) },
    )) { mutableStateOf<CreateCardState?>(null) }
    val live = remember(doc) { CharacterEdits.tags(doc) }
    val liveTag = tag?.takeIf { t -> live.any { it.key == t } }
    val model = remember(doc, query, liveTag, gate.open, currentKey) {
        PickerModels.characters(doc, query, liveTag, hiddenOpen = gate.open, currentSpeakerKey = currentKey)
    }
    val chips = remember(live) { listOf(PickerChip(null, PickerCopy.ALL)) + live.map { PickerChip(it.key, it.name) } }
    val canCreate = creatable && !currentProjectStore.readOnly
    val editor = rememberCastEditor(projectKey)
    ItemPickerSheet(
        title = title,
        model = model,
        query = query,
        onQueryChange = { query = it },
        onPick = { onPick(it.key) },
        onClose = onClose,
        selectedKey = currentKey,
        chips = chips,
        selectedChip = liveTag,
        onChip = { tag = it },
        onUnlockHidden = gate.unlock,
        createKind = if (canCreate) CreateKind.CHARACTER else null,
        onCreate = if (canCreate) ({ q -> creating = CastCreate.openCharacter(CreateCardMode.PICKER, CastCreate.pickerQuery(q, PickerCreate.matched(q, model))) }) else null,
    )
    var busy by remember { mutableStateOf(false) }
    creating?.let { card ->
        CastCreateCard(
            state = card,
            onChange = { creating = it },
            onCancel = { creating = null },
            onCreate = {
                val made = card
                busy = true
                var key: Key? = null
                // The card closes only once the write applied; otherwise the typed values stay.
                editor.act(CreateCardCopy.CREATE, then = { result ->
                    busy = false
                    if (result == EditResult.APPLIED) {
                        creating = null
                        key?.let(onPick)
                    }
                }) { d, ctx ->
                    CastCreate.createCharacter(d, made, ctx)?.let { (next, k) ->
                        key = k
                        next
                    } ?: d
                }
            },
            canCreate = !busy && CastCreate.canCreate(card),
            tags = live,
        )
    }
}
