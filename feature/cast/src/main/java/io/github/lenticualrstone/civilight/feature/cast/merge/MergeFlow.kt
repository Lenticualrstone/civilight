package io.github.lenticualrstone.civilight.feature.cast.merge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.lenticualrstone.civilight.domain.actions.MergeActionResult
import io.github.lenticualrstone.civilight.domain.actions.MergeActions
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.MergeOps
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastWrites
import io.github.lenticualrstone.civilight.feature.cast.common.Step
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.launch

/**
 * The `합치기` write (CST-45). It runs on the app scope ([CastWrites]) because the merged item's own
 * screen leaves as soon as the item is in the trash. Recorder links of the merged item move to the
 * target in the recorder library too, and the toast's `되돌리기` reverts both (row by row).
 */
class MergeFlow(private val deps: FeatureDeps, private val projectKey: Key) {
    private val writes = CastWrites(deps, projectKey)

    /** Merges [mergedKey] into [targetKey]; [then] runs after the write (the sheet closes). */
    fun merge(kind: EntityKind, mergedKey: Key, targetKey: Key, picks: Set<String>, then: () -> Unit) {
        deps.appScope.launch {
            deps.stores.recorder.ensureLoaded()
            val recorder = deps.stores.recorder.state.value.library
            writes.launch(
                CastUiCopy.BTN_MERGE,
                { doc, ctx -> MergeModel.merge(doc, kind, mergedKey, targetKey, picks, ctx, recorder)?.let { Step(it.doc, it) } },
            ) { result ->
                if (result.recorder != result.recorderBefore) {
                    deps.appScope.launch {
                        deps.stores.recorder.updateLibrary { lib -> MergeOps.applyToRecorder(lib, projectKey, ItemKind.of(kind), mergedKey, targetKey) }
                    }
                }
                then()
                deps.toasts.show(result.toast.message, result.toast.action ?: TrashCopy.UNDO) { undo(result) }
            }
        }
    }

    private fun undo(result: MergeActionResult) {
        writes.revert(TrashCopy.UNDO, result.change)
        if (result.recorder != result.recorderBefore) {
            deps.appScope.launch {
                deps.stores.recorder.updateLibrary { lib -> MergeActions.undo(result.doc, lib, result).second ?: lib }
            }
        }
    }
}

@Composable
fun rememberMergeFlow(projectKey: Key): MergeFlow {
    val deps = LocalFeatureDeps.current
    return remember(deps, projectKey) { MergeFlow(deps, projectKey) }
}
