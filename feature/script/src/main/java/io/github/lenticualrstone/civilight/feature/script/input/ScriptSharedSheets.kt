package io.github.lenticualrstone.civilight.feature.script.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.domain.actions.VersionActions
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Replace
import io.github.lenticualrstone.civilight.feature.script.scenes.actionContext
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.version.VersionOrigin
import io.github.lenticualrstone.civilight.navigation.EpisodeVersionsSheet
import io.github.lenticualrstone.civilight.navigation.ManuscriptReplaceSheet
import io.github.lenticualrstone.civilight.navigation.VersionCompareSheet
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.launch

/*
 * (0.2.0, N-28, N-29) The script's shared sheets, opened over the script, the story structure (episode ⋯) and the
 * cast and world name fields (`원고 속 n곳 · 바꾸기`). They write through the project's store (not a screen's view
 * model), because they open over other areas' screens too, as `RenumberSheet` does.
 */

/**
 * One write as one undo step [label]; [toast] (from the documents before and after) shows with `되돌리기`, which
 * undoes only that step. A write that changes nothing records no step and shows nothing; a read-only project
 * changes nothing.
 */
internal fun FeatureDeps.writeStep(
    store: ProjectStore,
    label: String,
    toast: ((before: ProjectDoc, after: ProjectDoc) -> String?)? = null,
    transform: (ProjectDoc) -> ProjectDoc?,
) {
    appScope.launch {
        try {
            var before: ProjectDoc? = null
            val after = store.update(label) { d -> before = d; transform(d) ?: d }
            val was = before ?: return@launch
            if (after == was) return@launch
            val text = toast?.invoke(was, after) ?: return@launch
            val count = store.undoState.value.undoCount
            toasts.show(text) {
                appScope.launch {
                    // Only the step the toast reports: nothing may have been recorded since.
                    if (store.undoState.value.undoCount == count) runCatching { store.undo() }
                }
            }
        } catch (e: ReadOnlyProjectException) {
            // The example project: nothing changes.
        }
    }
}

private fun FeatureDeps.scriptContext(): EditContext =
    EditContext(now = clock.now(), zone = clock.zone, device = stores.deviceId.value, newKey = Keys::newKey)

/** `지난 판` · `판 남기기` of an episode (SCR-76). */
@Composable
fun SheetScope.EpisodeVersionsSheetContent(sheet: EpisodeVersionsSheet, deps: FeatureDeps) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val defaultName = VersionActions.defaultName(deps.actionContext().today, VersionOrigin.MANUAL)
    VersionsSheet(sheet, doc, store.readOnly, defaultName) { name ->
        deps.writeStep(store, WriteCopy.KEEP_VERSION) { d ->
            VersionActions.capture(d, sheet.episodeKey, name, VersionOrigin.MANUAL, deps.actionContext()).first
        }
    }
}

/** One version compared with the episode; `이 판 글로` per block (SCR-76). */
@Composable
fun SheetScope.VersionCompareSheetContent(sheet: VersionCompareSheet, deps: FeatureDeps) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(sheet.projectKey)
    VersionCompareSheetBody(sheet, doc, gate.open, store.readOnly, onUnlock = gate.unlock) { blockKey ->
        deps.writeStep(
            store,
            WriteCopy.UNDO_VERSION_TEXT,
            toast = { _, after -> after.index.block(blockKey)?.let { "${Labels.number(it.number)} · ${WriteCopy.UNDO_VERSION_TEXT}" } },
        ) { d -> VersionActions.restoreBlockText(d, sheet.episodeKey, sheet.versionKey, blockKey, deps.actionContext()) }
    }
}

/** Manuscript `바꾸기` with `찾을 말` filled in (SCR-75), from a name field's `원고 속 n곳 · 바꾸기`. */
@Composable
fun SheetScope.ManuscriptReplaceSheetContent(sheet: ManuscriptReplaceSheet, deps: FeatureDeps) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(sheet.projectKey)
    FindSheet(
        replace = true,
        initialQuery = sheet.query,
        doc = doc,
        unitKey = "",
        unlocked = gate.open,
        readOnly = store.readOnly,
        commands = emptyList(),
        actions = FindActions(
            jumpTo = {},
            command = {},
            replace = { hits, query, with ->
                if (hits.isEmpty()) return@FindActions
                val unlocked = gate.open
                var count = 0
                deps.writeStep(store, WriteCopy.UNDO_REPLACE, toast = { _, _ -> WriteCopy.replaced(count) }) { d ->
                    val r = Replace.applyCounted(d, hits, query, with, deps.scriptContext(), unlocked)
                    count = r.count
                    r.doc.takeIf { r.count > 0 }
                }
            },
        ),
    )
}
