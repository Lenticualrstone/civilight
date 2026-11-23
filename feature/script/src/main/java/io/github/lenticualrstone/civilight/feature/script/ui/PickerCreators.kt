package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.domain.actions.CreateActions
import io.github.lenticualrstone.civilight.feature.script.scenes.actionContext
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.PickerCreator

/**
 * (0.2.0, U-15.3) The create row of the script's pickers (speaker, `@` link, scene place, change partner):
 * one write through `domain:actions` `CreateActions.inPicker`, the context taken at the moment of the write.
 * The shared picker hides the row in a read-only project (U-15.10).
 */
@Composable
internal fun scriptPickerCreator(vararg kinds: CreateKind): PickerCreator {
    val deps = LocalFeatureDeps.current
    return PickerCreator(kinds.toSet()) { d, v ->
        CreateActions.inPicker(d, v.kind.name, v.name, deps.actionContext(), v.displayName, v.note, v.alias, v.tagKeys, v.parentKey)
    }
}

/** The `@` picker's kinds (SCR-55): people, terms and places. */
internal val LINK_CREATE_KINDS = arrayOf(CreateKind.CHARACTER, CreateKind.TERM, CreateKind.PLACE)
