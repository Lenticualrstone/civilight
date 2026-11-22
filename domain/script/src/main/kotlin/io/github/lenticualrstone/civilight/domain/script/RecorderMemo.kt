package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Memo blocks made by the Life Recorder (SCR-59, rule 23 §4). */
object RecorderMemo {
    data class Inserted(val doc: ProjectDoc, val blockKey: Key, val toast: String) {
        val actions: List<String> get() = listOf(Toasts.UNDO, Toasts.GO_THERE)
    }

    /**
     * `원고에 넣기` / `여기 넣기`: exactly one `⌁` author-memo block at [position] with a copy of the
     * record's text (links re-minted, recorder-only link kinds flattened) and its source; it gets the
     * episode's next number. Editing either side never changes the other.
     */
    fun insert(doc: ProjectDoc, position: BlockPosition, recordRuns: List<TextRun>, source: RecorderSource, ctx: EditContext): Inserted {
        val ins = BlockOps.insert(
            doc, position, BlockKind.MEMO, ctx, runs = Runs.fromRecorder(recordRuns, ctx.newKey), recorderSource = source,
        )
        val block = ins.doc.index.block(ins.blockKey)!!
        return Inserted(ins.doc, ins.blockKey, Toasts.recorderInserted(Labels.sceneLocation(ins.doc, block.sceneKey), Labels.number(block.number)))
    }

    /**
     * `마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤` from the project's last caret position
     * (`ProjectActivity.lastEditedBlockKey`); null when there is none or the block is gone.
     */
    fun lastWritingPlace(doc: ProjectDoc): String? {
        val key = doc.app.activity.activity.lastEditedBlockKey ?: return null
        val block = doc.index.block(key)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return null
        return "마지막으로 쓰던 곳 · ${Labels.sceneLocation(doc, block.sceneKey)} · ${Labels.number(block.number)} 뒤"
    }

    /** The insert position of `마지막으로 쓰던 곳`. */
    fun lastWritingPosition(doc: ProjectDoc): BlockPosition? =
        doc.app.activity.activity.lastEditedBlockKey?.takeIf { doc.manuscript.isLiveBlock(it) }?.let { BlockPosition.After(it) }
}
