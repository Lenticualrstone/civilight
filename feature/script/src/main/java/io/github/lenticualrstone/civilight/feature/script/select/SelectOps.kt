package io.github.lenticualrstone.civilight.feature.script.select

import io.github.lenticualrstone.civilight.domain.script.BlockMove
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * Select-mode operations over `domain:script` `BlockMove` (SCR-37, SCR-40; consequences per SCR-44:
 * records ride along, numbers and voice IDs follow the destination's numbering unit). Each is one
 * undo step with the domain's toast.
 */
object SelectOps {
    /** `옮기기` / handle drag: the blocks keep their keys; the first moved block is shown. */
    fun move(doc: ProjectDoc, keys: List<Key>, destination: BlockPosition, ctx: EditContext): ScriptOps.Outcome? {
        val live = keys.filter { doc.manuscript.isLiveBlock(it) }
        if (live.isEmpty()) return null
        val r = BlockMove.move(doc, live, destination, ctx)
        if (r.movedKeys.isEmpty() || r.doc === doc) return null
        return ScriptOps.Outcome(r.doc, toast = r.toast)
    }

    /**
     * Paste below [below] (SCR-40: below the current block): a cut moves its blocks there, a copy
     * inserts new blocks (new keys and numbers, automatic voice IDs, no records; toast
     * `◆는 복사되지 않음`). The pasted blocks' first one is selected.
     */
    fun paste(doc: ProjectDoc, clipboard: BlockMove.Clipboard, below: Key, ctx: EditContext): ScriptOps.Outcome? {
        if (!doc.manuscript.isLiveBlock(below)) return null
        val live = if (clipboard.cut) clipboard.copy(blocks = clipboard.blocks.filter { doc.manuscript.isLiveBlock(it.key) }) else clipboard
        if (live.blocks.isEmpty()) return null
        val r = BlockMove.paste(doc, live, BlockPosition.After(below), ctx)
        if (r.blockKeys.isEmpty()) return null
        return ScriptOps.Outcome(r.doc, select = r.blockKeys.first(), toast = r.toast)
    }
}
