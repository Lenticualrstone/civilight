package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.world.item.NewWorldItem
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.ItemPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import kotlinx.coroutines.launch

/**
 * (0.2.0, U-15.3, WLD-70, WLD-52) The place picker of the world area with its create row on top:
 * `＋ 새 장소`, or `＋ ‘<query>’ 새 장소` when the query matches nothing (the query fills `이름`). The row
 * opens the create card in picker mode (`취소 · 만들기`); [onCreate] writes the place (one edit, one undo
 * step) and the new place is returned through [onPick] at once. The card's `상위 지역` swaps this sheet to
 * a plain place list (no create row, so no nested card) and back. No create row in a read-only project
 * (U-15.10). The `core:ui` `PlacePickerSheet` has no create row in 0.2.0, so this is world-local (Requests).
 */
@Composable
fun ColumnScope.WorldPlacePicker(
    doc: ProjectDoc,
    gate: HiddenGate,
    title: String,
    readOnly: Boolean,
    onPick: (PlaceRef) -> Unit,
    onClose: () -> Unit,
    onCreate: suspend (NewWorldItem) -> PlaceRef?,
    selected: PlaceRef? = null,
    includeRegions: Boolean = true,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var createText by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingParent by rememberSaveable { mutableStateOf(false) }
    val draft = WorldCreateDraft.decode(createText)
    val scope = rememberCoroutineScope()
    if (draft != null && pickingParent) {
        PlacePickerSheet(
            doc = doc,
            gate = gate,
            title = WorldCopy.PICK_PLACE,
            onPick = { ref ->
                createText = draft.copy(parentKey = ref.key).encode()
                pickingParent = false
            },
            onClose = { pickingParent = false },
            selected = draft.parentKey?.let { PlaceRef(PlaceRefKind.PLACE, it) },
            includeRegions = false,
        )
        return
    }
    val model = remember(doc, query, gate.open, includeRegions) { PickerModels.places(doc, query, gate.open, includeRegions) }
    ItemPickerSheet(
        title = title,
        model = model,
        query = query,
        onQueryChange = { query = it },
        onPick = { item -> PickerModels.placeRefKind(item)?.let { onPick(PlaceRef(it, item.key)) } },
        onClose = onClose,
        selectedKey = selected?.key,
        onUnlockHidden = gate.unlock,
        createKind = if (readOnly) null else CreateKind.PLACE,
        onCreate = if (readOnly) null else ({ q -> createText = WorldCreateDraft.of(WorldKind.PLACE, CreateCardMode.PICKER, q).encode() }),
    )
    if (draft != null && !readOnly) {
        WorldCreateCard(
            draft = draft,
            doc = doc,
            hiddenOpen = gate.open,
            onChange = { createText = it.encode() },
            onCancel = { createText = null },
            onCreate = {
                scope.launch {
                    val made = onCreate(WorldCreateCards.item(draft)) ?: return@launch
                    createText = null
                    onPick(made)
                }
            },
            onCreateAndOpen = {},
            onPickParent = { pickingParent = true },
        )
    }
}
