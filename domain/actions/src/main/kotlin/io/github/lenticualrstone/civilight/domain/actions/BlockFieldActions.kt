package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.BlockTodo

/**
 * (0.2.0) Block fields added in 0.2.0, each one undo step for the caller: the per-block display name
 * (N-43, SCR-78) and a memo's `할 일` (N-41, SCR-77). Number, voice ID, speaker and `◆` never change.
 */
object BlockFieldActions {
    /**
     * `이 블록만 · 표시 이름` on a live dialogue block; trimmed, blank stored as null. Other kinds
     * are refused (the row exists on dialogue only); a stored value survives a kind change.
     */
    fun setDisplayNameOverride(doc: ProjectDoc, blockKey: Key, value: String?, ctx: ActionContext): ProjectDoc {
        val block = liveBlock(doc, blockKey) ?: return doc
        if (block.kind.known != BlockKind.DIALOGUE) return doc
        val v = value?.nfc()?.trim()?.ifEmpty { null }
        return replace(doc, block, block.copy(displayNameOverride = v), ctx)
    }

    /** The label of a dialogue block: `{override} ({speaker name})` when it has one (SCR-20). */
    fun speakerLabel(override: String?, speakerName: String): String =
        if (override == null) speakerName else "$override ($speakerName)"

    /** `할 일` switch on a live memo block: on makes it `열림`, off removes it. */
    fun setTodo(doc: ProjectDoc, blockKey: Key, on: Boolean, ctx: ActionContext): ProjectDoc {
        val block = liveBlock(doc, blockKey) ?: return doc
        if (block.kind.known != BlockKind.MEMO) return doc
        val todo = if (on) block.todo ?: BlockTodo() else null
        return replace(doc, block, block.copy(todo = todo), ctx)
    }

    /** Checks a memo to-do done (`끝냄`) or back open (`열림`). */
    fun setTodoDone(doc: ProjectDoc, blockKey: Key, done: Boolean, ctx: ActionContext): ProjectDoc {
        val block = liveBlock(doc, blockKey) ?: return doc
        val todo = block.todo ?: return doc
        val next = todo.copy(done = done, doneAt = if (done) ctx.now else null)
        return replace(doc, block, block.copy(todo = next), ctx)
    }

    private fun liveBlock(doc: ProjectDoc, key: Key): Block? = doc.index.block(key)?.takeIf { doc.liveness.isLive(it) }

    private fun replace(doc: ProjectDoc, old: Block, new: Block, ctx: ActionContext): ProjectDoc {
        if (old == new) return doc
        val stamped = new.copy(updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(
            episodes = doc.episodes.mapValues { (_, f) ->
                if (f.blocks.none { it.key == old.key }) f else f.copy(blocks = f.blocks.map { if (it.key == old.key) stamped else it })
            },
        )
    }
}
