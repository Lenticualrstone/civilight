package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** What a submitted command-field text does in the script (SCR-12, SCR-68). */
sealed interface CommandAction {
    /** `/` alone: the `블록 추가` sheet. */
    data object OpenAddSheet : CommandAction

    /** `/대` … `/메`: a block of [kind] below the current block. */
    data class AddBlock(val kind: BlockKind) : CommandAction

    /** `/씬`: the `새 씬` routing (SCR-49). */
    data object NewScene : CommandAction

    /** `#043`, `씬 3` or a scene title of this episode: scroll to [rowKey]; select [blockKey] when given. */
    data class Jump(val rowKey: String, val blockKey: Key?) : CommandAction

    /** `렌즈를 이 씬으로 맞추기` (SCR-02, replaces `시점 바꾸기` in the script). */
    data object SetLens : CommandAction

    /** `번호 다시 매기기 · 1-7` (SCR-22, SCR-68): the preview sheet of numbering unit [unitKey]. */
    data class Renumber(val unitKey: Key) : CommandAction

    /** `점검 센터 열기` (SCR-68). */
    data object OpenCheckCenter : CommandAction

    /** `블록 고르기` (SCR-68 `[PROPOSED]`, SCR-36). */
    data object SelectMode : CommandAction

    /** `에셋` (SCR-68 `[PROPOSED]`): the asset list (SCR-61). */
    data object OpenAssets : CommandAction

    /** Anything else goes to the search area. */
    data class Search(val query: String) : CommandAction

    /** Blank input. */
    data object Nothing : CommandAction
}

/**
 * The command field's routing (SCR-12, SCR-68). Commands run only when the text is submitted
 * (the keyboard's search action), never while typing, so `/시` is never taken for the start of
 * `/씬` or a word in progress (no guessing).
 */
object CommandRouting {
    fun route(doc: ProjectDoc, numberingUnitKey: Key, text: String): CommandAction {
        val t = text.trim()
        if (t.isEmpty()) return CommandAction.Nothing
        when (val slash = ScriptCommands.parseSlash(t)) {
            ScriptCommands.Slash.OpenSheet -> return CommandAction.OpenAddSheet
            ScriptCommands.Slash.NewScene -> return CommandAction.NewScene
            is ScriptCommands.Slash.AddBlock -> return CommandAction.AddBlock(slash.kind)
            null -> Unit
        }
        if (t == ScriptCopy.SET_LENS_TO_SCENE) return CommandAction.SetLens
        if (t == ScriptCopy.OPEN_CHECK_CENTER) return CommandAction.OpenCheckCenter
        if (t == io.github.lenticualrstone.civilight.feature.script.select.MoveCopy.TITLE) return CommandAction.SelectMode
        if (t == io.github.lenticualrstone.civilight.feature.script.assets.AssetsCopy.TITLE) return CommandAction.OpenAssets
        renumberTarget(doc, numberingUnitKey, t)?.let { return CommandAction.Renumber(it) }
        when (val jump = ScriptCommands.jump(doc, numberingUnitKey, t)) {
            is ScriptCommands.Jump.ToBlock -> return CommandAction.Jump(ScriptRowKeys.block(jump.blockKey), jump.blockKey)
            is ScriptCommands.Jump.ToScene -> return CommandAction.Jump(ScriptRowKeys.head(jump.sceneKey), null)
            null -> Unit
        }
        return CommandAction.Search(t)
    }

    /**
     * `번호 다시 매기기` alone (this episode) or `번호 다시 매기기 · 1-8` (the episode with that
     * code). Null when the text is another command or names no numbering unit.
     */
    fun renumberTarget(doc: ProjectDoc, numberingUnitKey: Key, text: String): Key? {
        val word = ScriptCopy.RENUMBER
        if (text == word) return numberingUnitKey
        val code = text.removePrefix("$word · ").takeIf { it != text }?.trim() ?: return null
        if (code.isEmpty()) return numberingUnitKey
        val level = doc.project.structure.episodeLevel ?: return null
        return doc.units.firstOrNull { u ->
            u.level == level && doc.manuscript.isLiveUnit(u.key) && Labels.unitCode(doc, u.key) == code
        }?.key
    }
}
