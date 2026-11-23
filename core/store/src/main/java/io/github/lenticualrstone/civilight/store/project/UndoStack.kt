package io.github.lenticualrstone.civilight.store.project

import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** One undo or redo step: its label and the document to return to. */
data class UndoEntry(
    val label: String,
    /** The document this step returns to (stored without its lazy caches). */
    val doc: ProjectDoc,
    val mergeKey: String? = null,
    val at: Timestamp,
)

/** What the UI shows for undo and redo (`되돌리기`, toasts). */
data class UndoState(
    val undoLabel: String? = null,
    val redoLabel: String? = null,
    val undoCount: Int = 0,
    val redoCount: Int = 0,
) {
    val canUndo: Boolean get() = undoCount > 0
    val canRedo: Boolean get() = redoCount > 0
}

/**
 * The labelled undo history of one open project (class A `UndoStep`, in memory only). Each step
 * keeps the whole document as it was before the edit; documents are immutable and share every
 * unchanged part, so a step costs only what the edit changed. App-only state (`doc.app`: lens,
 * scroll, last position) is never part of undo: [undo] and [redo] keep the current one.
 */
data class UndoStack(
    val undo: List<UndoEntry> = emptyList(),
    val redo: List<UndoEntry> = emptyList(),
    val limit: Int = 100,
    val mergeWindowMs: Long = 2_000,
) {
    val state: UndoState
        get() = UndoState(undo.lastOrNull()?.label, redo.lastOrNull()?.label, undo.size, redo.size)

    /**
     * Records an edit from [before]. With a [mergeKey] equal to the last step's, made within
     * [mergeWindowMs] and with nothing to redo, the edit joins that step (typing).
     */
    fun record(label: String, before: ProjectDoc, mergeKey: String?, at: Timestamp): UndoStack {
        val top = undo.lastOrNull()
        if (mergeKey != null && top != null && top.mergeKey == mergeKey && redo.isEmpty() && at - top.at in 0..mergeWindowMs) {
            return copy(undo = undo.dropLast(1) + top.copy(at = at))
        }
        val entry = UndoEntry(label, before.copy(), mergeKey, at)
        return copy(undo = (undo + entry).takeLast(limit), redo = emptyList())
    }

    /**
     * (0.2.0) Applies a write that is not an undo step to every kept step too, so a later undo or redo never
     * reverts it. A step whose document the write cannot apply to (it throws) is kept as it was.
     */
    fun patch(transform: (ProjectDoc) -> ProjectDoc): UndoStack {
        fun UndoEntry.patched(): UndoEntry = runCatching { copy(doc = transform(doc).copy()) }.getOrDefault(this)
        return copy(undo = undo.map { it.patched() }, redo = redo.map { it.patched() })
    }

    /** Steps back from [current]; null when there is nothing to undo. */
    fun undo(current: ProjectDoc): Pair<UndoStack, UndoEntry>? {
        val entry = undo.lastOrNull() ?: return null
        val back = UndoEntry(entry.label, current.copy(), null, entry.at)
        return copy(undo = undo.dropLast(1), redo = redo + back) to entry.copy(doc = entry.doc.copy(app = current.app, unknownFiles = current.unknownFiles))
    }

    /** Steps forward again; null when there is nothing to redo. */
    fun redo(current: ProjectDoc): Pair<UndoStack, UndoEntry>? {
        val entry = redo.lastOrNull() ?: return null
        val back = UndoEntry(entry.label, current.copy(), null, entry.at)
        return copy(undo = undo + back, redo = redo.dropLast(1)) to entry.copy(doc = entry.doc.copy(app = current.app, unknownFiles = current.unknownFiles))
    }

    fun clear(): UndoStack = copy(undo = emptyList(), redo = emptyList())
}
