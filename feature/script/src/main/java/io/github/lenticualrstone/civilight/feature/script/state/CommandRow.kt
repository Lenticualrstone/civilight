package io.github.lenticualrstone.civilight.feature.script.state

/**
 * What the reading command row above the command field holds (SCR-01, SCR-30, Q-02 `ED`).
 *
 * - [speaker] and [lineBreak]: a writable position (caret or selected block) in `쓰기`, as board `Main`.
 * - [panelTabs]: `◆ 시간 · 참고 · 속성` for the selected block while no side panel is on screen
 *   (cover, or an inner screen whose panel was closed). They are the visible tap path to the same
 *   buttons the swipe reveals (the swipe is a shortcut only), and the way back to a closed panel.
 *   Read-only projects get them too.
 * - [layers]: the row's `연출 · 메모 표시` button. On cover the header already has one, so the row's
 *   copy gives its place to the tab buttons when those are shown (the row is 358 dp wide there).
 */
data class CommandRow(
    val speaker: Boolean,
    val layers: Boolean,
    val panelTabs: Boolean,
    val lineBreak: Boolean,
) {
    /** The row is drawn at all (else the bar is the command field alone). */
    val visible: Boolean get() = speaker || panelTabs

    companion object {
        val HIDDEN = CommandRow(speaker = false, layers = false, panelTabs = false, lineBreak = false)

        fun of(
            editor: EditorState,
            readOnly: Boolean,
            previewOnly: Boolean,
            panelShown: Boolean,
            wide: Boolean,
        ): CommandRow {
            if (editor.editing && editor.virtualScene == null && editor.selected != null) return HIDDEN
            val writing = editor.hasPosition && !readOnly && !previewOnly
            val tabs = editor.selected != null && !editor.editing && !panelShown && !previewOnly
            if (!writing && !tabs) return HIDDEN
            return CommandRow(
                speaker = writing,
                layers = wide || !tabs,
                panelTabs = tabs,
                lineBreak = writing,
            )
        }
    }
}
