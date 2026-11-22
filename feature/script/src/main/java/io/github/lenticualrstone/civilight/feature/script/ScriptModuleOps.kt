package io.github.lenticualrstone.civilight.feature.script

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.script.SceneMemo
import io.github.lenticualrstone.civilight.model.Key
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The `모듈` tab's writes (SCR-35).

/** A typed `씬 메모` not yet written. */
internal data class PendingMemo(val sceneKey: Key, val text: String) {
    val mergeKey: String get() = "memo:$sceneKey"
}

/**
 * `씬 메모` typed in place: written [ScriptViewModel.COMMIT_DELAY_MS] after the last change, merged
 * per scene (`memo:{scene}`), so one pause is one undo step. Typing another scene's memo writes the
 * previous one first; the screen stopping and the view model going write it too ([flushMemo]).
 */
fun ScriptViewModel.editSceneMemo(sceneKey: Key, text: String) {
    if (readOnly) return
    val previous = pendingMemo
    if (previous != null && previous.sceneKey != sceneKey) flushMemo()
    pendingMemo = PendingMemo(sceneKey, text)
    memoJob?.cancel()
    memoJob = viewModelScope.launch {
        delay(ScriptViewModel.COMMIT_DELAY_MS)
        flushMemo()
    }
}

/** Writes the typed memo now (pause over, another scene, screen stopping). */
internal fun ScriptViewModel.flushMemo() {
    memoJob?.cancel()
    val memo = pendingMemo ?: return
    pendingMemo = null
    enqueue { update(ScriptCopy.MOD_MEMO, memo.mergeKey) { d -> SceneMemo.set(d, memo.sceneKey, memo.text, ctx()) } }
}
