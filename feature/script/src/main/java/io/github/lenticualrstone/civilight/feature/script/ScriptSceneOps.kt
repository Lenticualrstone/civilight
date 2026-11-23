package io.github.lenticualrstone.civilight.feature.script

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

/**
 * Scenes made from the script with story's `NewUnitSheet` (SCR-49 `다음 씬 만들기`, SCR-53 `앞에 새
 * 씬`): the new scene's head appears and its first (virtual) dialogue line takes the caret
 * (`[PROPOSED]`). The sheet and the write are story's, so the script recognises the new scene in its
 * next content ([NewSceneFollow]).
 */
object NewSceneFollow {
    /** Live scenes of the numbering unit [unitKey] (the project key in a structure without one). */
    fun known(doc: ProjectDoc, unitKey: Key): Set<Key> =
        doc.manuscript.liveScenes.filter { unitOf(doc, it.key) == unitKey }.mapTo(HashSet()) { it.key }

    /** A live, empty scene of [unitKey] that was not in [known]; null while none was made. */
    fun created(doc: ProjectDoc, unitKey: Key, known: Set<Key>): Key? =
        doc.manuscript.liveScenes.firstOrNull { s ->
            s.key !in known && unitOf(doc, s.key) == unitKey && doc.manuscript.liveBlocksOf(s.key).isEmpty()
        }?.key

    private fun unitOf(doc: ProjectDoc, sceneKey: Key): Key = doc.index.numberingUnitKeyOfScene(sceneKey) ?: doc.project.key
}

/** Starts following a `NewUnitSheet` opened from this episode: a new empty scene gets the caret. */
internal fun ScriptViewModel.followNewScene(unitKey: Key) {
    val d = latestDoc ?: return
    val known = NewSceneFollow.known(d, unitKey)
    newSceneJob?.cancel()
    newSceneJob = viewModelScope.launch {
        val scene = content.filterNotNull().mapNotNull { c -> NewSceneFollow.created(c.doc, unitKey, known) }.first()
        // (0.2.0, SCR-49) The new scene's head sits directly above the input bar, so it is the target scene.
        aboveInput.tryEmit(ScriptRowKeys.head(scene))
    }
}

/**
 * The `NewUnitSheet` closed: a scene it made arrives with the next store update, so the follow stays
 * a moment longer, then stops (`닫기` made nothing).
 */
internal fun ScriptViewModel.stopFollowingNewScene() {
    val job = newSceneJob ?: return
    viewModelScope.launch {
        delay(FOLLOW_GRACE_MS)
        job.cancel()
        if (newSceneJob === job) newSceneJob = null
    }
}

private const val FOLLOW_GRACE_MS = 2_000L
