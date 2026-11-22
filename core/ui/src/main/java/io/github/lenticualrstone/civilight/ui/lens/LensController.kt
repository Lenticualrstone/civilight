package io.github.lenticualrstone.civilight.ui.lens

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The lens of one open project (STO-13, STO-16): one value per project, shared by every lens
 * screen, kept in `_app/view.json` (survives restarts, never exported). Writes go through
 * `updateApp`, so they work in the read-only example and add no undo step.
 */
class LensController(private val store: ProjectStore, private val scope: CoroutineScope) {
    /** The lens value as it changes. */
    val state: Flow<LensState> = store.doc.map { it.app.view.lens }.distinctUntilChanged()

    val point: Flow<LensPoint> = state.map { it.point }.distinctUntilChanged()

    fun current(): LensState = store.doc.value.app.view.lens

    /** `이 시점으로 보기` / `최신으로 돌아가기`. */
    fun set(point: LensPoint): Job = change { it.copy(point = point) }

    fun backToLatest(): Job = set(LensPoint.Latest)

    /** Call when the script screen is entered. */
    fun enterScript(): Job = change(LensRules::enterScript)

    /** Call when the script screen is left (popped or replaced). */
    fun leaveScript(): Job = change(LensRules::leaveScript)

    /** `렌즈를 이 씬으로 맞추기` (SCR-02, STO-16). */
    fun setLensToScene(sceneKey: Key): Job = change { LensRules.setLensToScene(it, sceneKey) }

    private fun change(transform: (LensState) -> LensState): Job = scope.launch {
        store.updateApp { app -> app.withLens(transform(app.view.lens)) }
    }

    private fun ProjectAppState.withLens(lens: LensState): ProjectAppState =
        if (lens == view.lens) this else copy(view = view.copy(lens = lens))
}
