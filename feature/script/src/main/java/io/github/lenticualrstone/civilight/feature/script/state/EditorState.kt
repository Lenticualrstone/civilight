package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.liveCharacters

/**
 * Where the writer is in the manuscript (SCR-01, SCR-14): the selected block, whether the caret is
 * in it (editing), the virtual first line of an empty scene, and small transient flags. The caret
 * offset and the field text live with the text field in the view model.
 */
data class EditorState(
    /** The selected block: the writing position used by the command row and the add sheet. */
    val selected: Key? = null,
    /** The caret is in [selected] (or in the virtual line of [virtualScene]). */
    val editing: Boolean = false,
    /** Caret in the not-yet-created first line of this empty scene. */
    val virtualScene: Key? = null,
    /** Block showing `직전 화자` until its first character (SCR-18). */
    val inheritedHint: Key? = null,
    /** Typed text goes into a paren run at the caret (toolbar `( )` or a typed `(`, SCR-07). */
    val parenMode: Boolean = false,
    /** A speaker picked while the current block is not dialogue: used by the next dialogue insert. */
    val pendingSpeaker: Key? = null,
) {
    /** The block the caret is in, if any. */
    val caretBlock: Key? get() = if (editing && virtualScene == null) selected else null

    /** A writing position exists (caret or selected block): the command row shows the speaker pill. */
    val hasPosition: Boolean get() = selected != null || virtualScene != null

    /** List key of the row holding the caret (`block:…` or the virtual line `empty:…`), if any. */
    val caretRowKey: String?
        get() = when {
            !editing -> null
            virtualScene != null -> ScriptRowKeys.empty(virtualScene)
            selected != null -> ScriptRowKeys.block(selected)
            else -> null
        }
}

object EditorRules {
    /**
     * After every document change: a selection whose block is gone is dropped (with the caret), the
     * virtual line disappears with its scene (the view model moves the caret when it creates the
     * line's block), hints of removed blocks clear,
     * and a pending speaker that is no longer a live character is forgotten.
     */
    fun reconcile(state: EditorState, doc: ProjectDoc): EditorState {
        val m = doc.manuscript
        var next = state
        if (next.selected != null && !m.isLiveBlock(next.selected)) {
            next = next.copy(selected = null, editing = next.editing && next.virtualScene != null, parenMode = false)
        }
        val virtual = next.virtualScene
        if (virtual != null && !m.isLiveUnit(virtual)) {
            next = next.copy(virtualScene = null, editing = next.editing && next.selected != null)
        }
        if (next.inheritedHint != null && !m.isLiveBlock(next.inheritedHint)) next = next.copy(inheritedHint = null)
        val pending = next.pendingSpeaker
        if (pending != null && doc.liveCharacters.none { it.key == pending }) next = next.copy(pendingSpeaker = null)
        return next
    }

    /** Selecting a block without a caret (non-text kinds, read-only, `완료`). */
    fun select(state: EditorState, blockKey: Key): EditorState =
        state.copy(selected = blockKey, editing = false, virtualScene = null, parenMode = false)

    /** The caret goes into [blockKey]. */
    fun edit(state: EditorState, blockKey: Key): EditorState =
        state.copy(selected = blockKey, editing = true, virtualScene = null, parenMode = false)

    /** The caret goes into the virtual first line of [sceneKey]. */
    fun editVirtual(state: EditorState, sceneKey: Key): EditorState =
        state.copy(selected = null, editing = true, virtualScene = sceneKey, parenMode = false)

    /** `완료` or the field lost focus: the keyboard goes, the block stays selected. */
    fun stopEditing(state: EditorState): EditorState = state.copy(editing = false, virtualScene = null, parenMode = false)
}
