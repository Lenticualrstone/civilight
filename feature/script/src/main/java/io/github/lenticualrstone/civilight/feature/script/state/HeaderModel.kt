package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.SceneText
import io.github.lenticualrstone.civilight.domain.script.SceneTimeSource
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** The script header (SCR-01, SCR-02, SCR-14). */
data class HeaderModel(
    /** Mono line: `1-7 · 씬 3 · 2일째 새벽`, or `1-7 · 씬 3 · 저장됨` while editing. */
    val line: String,
    /** Position label `이 씬 · 1-7` (not a lens); null without a scene. */
    val positionLabel: String?,
    /** The scene the header describes. */
    val sceneKey: Key?,
) {
    companion object {
        /**
         * Which scene (rule 22 §6): the scene of the caret or selected block, else the scene of the row
         * under the top bar ([sceneUnderBar]). [editing] switches to the editing line with [saved].
         */
        fun of(
            doc: ProjectDoc,
            unitKey: Key,
            time: SceneTimeSource,
            state: EditorState,
            sceneUnderBar: Key?,
            editing: Boolean,
            saved: Boolean,
        ): HeaderModel {
            val stateScene = state.selected?.let(doc.index::block)?.takeIf { doc.manuscript.isLiveBlock(it.key) }?.sceneKey
                ?: state.virtualScene
            val sceneKey = (stateScene ?: sceneUnderBar)?.takeIf { doc.manuscript.isLiveUnit(it) }
            if (sceneKey == null) {
                val code = if (unitKey == doc.project.key) "" else Labels.unitCode(doc, unitKey)
                return HeaderModel(code, null, null)
            }
            val line = if (editing) SceneText.editingLine(doc, sceneKey, saved) else SceneText.positionLine(doc, sceneKey, time.timeOf(sceneKey))
            return HeaderModel(line, positionLabel(doc, sceneKey), sceneKey)
        }

        /** `이 씬 · 1-7`; a 1-level project has no episode code, so the label is `이 씬`. */
        fun positionLabel(doc: ProjectDoc, sceneKey: Key): String =
            if (Labels.episodeCodeOfScene(doc, sceneKey).isEmpty()) SceneText.positionLabel(doc, sceneKey).removeSuffix(" · ")
            else SceneText.positionLabel(doc, sceneKey)

        /** The scene of the first row at or below [index] (the row under the top bar). */
        fun sceneAt(rows: List<ScriptRow>, index: Int): Key? = rows.getOrNull(index.coerceAtLeast(0))?.sceneKey ?: rows.lastOrNull()?.sceneKey
    }
}
