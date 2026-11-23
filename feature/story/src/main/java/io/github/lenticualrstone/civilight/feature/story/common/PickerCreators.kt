package io.github.lenticualrstone.civilight.feature.story.common

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.actions.CreateActions
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.PickerCreator

/**
 * (0.2.0, U-15.3) The create row of the story's pickers (scene `등장`, scene place, new-scene place, `화자
 * 고르기`): one write through `domain:actions` `CreateActions.inPicker`. The shared picker hides the row in a
 * read-only project (U-15.10).
 */
@Composable
internal fun storyPickerCreator(kind: CreateKind): PickerCreator {
    val deps = LocalFeatureDeps.current
    return PickerCreator(kind) { d, v ->
        CreateActions.inPicker(d, v.kind.name, v.name, deps.storyServices().actionContext(), v.displayName, v.note, v.alias, v.tagKeys, v.parentKey)
    }
}
