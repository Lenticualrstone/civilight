package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CreateActions
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.PickerCreator

/**
 * (0.2.0, U-15.3, WLD-70, WLD-52) The create row of a world picker (event place, term `관련`, description `@`):
 * the shared `core:ui` pickers draw `＋ 새 장소` / `＋ ‘<query>’ 새 장소`, the picker-mode card (`취소 · 만들기`,
 * `상위 지역` picked in place) and return the new item as the pick; this supplies the one write through
 * `domain:actions` `CreateActions.inPicker`. The shared sheet hides the row in a read-only project (U-15.10).
 */
@Composable
fun worldPickerCreator(vararg kinds: CreateKind): PickerCreator {
    val deps = LocalFeatureDeps.current
    return PickerCreator(kinds.toSet()) { d, v ->
        val ctx = ActionContext(now = deps.clock.now(), zone = deps.clock.zone, device = deps.stores.deviceId.value, newKey = Keys::newKey)
        CreateActions.inPicker(d, v.kind.name, v.name, ctx, v.displayName, v.note, v.alias, v.tagKeys, v.parentKey)
    }
}
