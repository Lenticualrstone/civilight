package io.github.lenticualrstone.civilight.feature.cast.create

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.actions.CreateActions
import io.github.lenticualrstone.civilight.feature.cast.common.actionContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PickerCreator

/**
 * The cast's character picker (relation partner, bulk speaker change, change-record partner): the shared
 * `core:ui` `CharacterPickerSheet` with (U-15.3, CST-66) the `＋ 새 인물` row on top in an editable project,
 * `＋ ‘세라’ 새 인물` when the query matches nothing (the query fills the name), the card `취소 · 만들기`, and the
 * created character returned as the pick (one undo step `만들기`). [creatable] false (the merge target,
 * CST-45) leaves the row out. Trashed tags make no chip (N-44, in the shared sheet).
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
    val creator = castPickerCreator(CreateKind.CHARACTER).takeIf { creatable }
    CharacterPickerSheet(
        doc = doc,
        gate = gate,
        onPick = onPick,
        onClose = onClose,
        title = title,
        currentKey = currentKey,
        creator = creator,
    )
}

/**
 * (0.2.0, U-15.3) The create row of a cast picker: one write through `domain:actions` `CreateActions.inPicker`
 * (CST-66 characters; WLD-70 places, terms and factions for a `연결` field). Hidden in read-only projects by
 * the shared sheet (U-15.10).
 */
@Composable
fun castPickerCreator(kind: CreateKind): PickerCreator {
    val deps = LocalFeatureDeps.current
    return PickerCreator(kind) { d, v ->
        CreateActions.inPicker(d, v.kind.name, v.name, deps.actionContext(), v.displayName, v.note, v.alias, v.tagKeys, v.parentKey)
    }
}
