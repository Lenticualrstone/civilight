package io.github.lenticualrstone.civilight.feature.canvas.common

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CreateActions
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.PickerCreator

/**
 * (0.2.0, U-15.3) The create row of the canvas item pickers (인물 · 세력 · 용어 nodes): one write through
 * `domain:actions` `CreateActions.inPicker`; the new item is placed as the picked one. Hidden in read-only
 * projects by the shared sheet (U-15.10).
 */
@Composable
internal fun canvasPickerCreator(kind: CreateKind): PickerCreator {
    val deps = LocalFeatureDeps.current
    return PickerCreator(kind) { d, v ->
        val ctx = ActionContext(now = deps.clock.now(), zone = deps.clock.zone, device = deps.stores.deviceId.value, newKey = Keys::newKey)
        CreateActions.inPicker(d, v.kind.name, v.name, ctx, v.displayName, v.note, v.alias, v.tagKeys, v.parentKey)
    }
}
