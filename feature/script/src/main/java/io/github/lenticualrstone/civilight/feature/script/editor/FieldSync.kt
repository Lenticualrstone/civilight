package io.github.lenticualrstone.civilight.feature.script.editor

import io.github.lenticualrstone.civilight.model.Key

/** The text field that emitted a change: its row and, in a choice block, the option line (SCR-09). */
data class FieldOwner(val rowKey: String, val optionKey: Key? = null)

/** Text typed into a field after its Enter was taken, for the block or option line the Enter made. */
sealed interface TypedAhead {
    /** [text] replaced [length] characters at [offset] of the new block's raw text. */
    data class Edit(val offset: Int, val length: Int, val text: String) : TypedAhead

    /** Another Enter, at [offset] of the new block. */
    data class Enter(val offset: Int) : TypedAhead
}

/** What [FieldSync.judge] decided about one change of a field. */
sealed interface Judgement {
    /** An ordinary change of the block with the caret: diff it against that block's text. */
    data object Normal : Judgement

    /** The change belongs to the block the pending Enter makes: replay [edits] there; the field keeps [FieldSync]'s text. */
    data class Ahead(val edits: List<TypedAhead>) : Judgement
}

/**
 * Keeps a rejected Enter from leaking into the next block (QA 2026-11-23 #5).
 *
 * Enter is not a text change here: the view model takes it and queues the structural operation (a
 * store update). The operation runs later, and until the new block has the caret the old field goes
 * on emitting changes. The view model used to give the old text back to the field and go on diffing
 * against the block that held the caret, but the field only takes a published value back at its next
 * recomposition, so a key typed in between was built on a buffer that held the old text plus the line
 * break. Diffed against the new, empty block it read `Line one.\nL`: the new block got the old text,
 * a break and the letter, while the old block kept its text.
 *
 * Now the view model **does not take the text back** after [rejectEnter]: the field is published as
 * it is (`front + "\n" + back`), so the field's buffer is always exactly what [judge] has seen, and
 * what comes after the line break is typed-ahead text of the new block. There is one buffer state,
 * no guessing which of two a key came from. Typed-ahead text is queued behind the Enter's operation
 * and replayed there; a second Enter is replayed as an Enter. A field whose Enter changed nothing
 * ([landed] with `stayed`) is given its old text back by the view model.
 *
 * A change from that field that no longer has the shape `front + "\n" + typed + back` (the writer moved
 * to the old part and edited it) ends the record and is read as an ordinary change.
 */
class FieldSync {
    private var owner: FieldOwner? = null
    private var front = ""
    private var back = ""

    /** The typed-ahead text seen so far (between the line break and [back]). */
    private var ahead = ""

    /** The last [landed] left the caret elsewhere: typed-ahead text has a block to go to. */
    var advanced: Boolean = false
        private set

    /** The text a rejected Enter leaves in its field (the view model publishes it as it is). */
    val armed: Boolean get() = owner != null

    /** Enter at raw [at] of [oldRaw] in the field of [from] was taken; its operation is in flight. */
    fun rejectEnter(from: FieldOwner?, oldRaw: String, at: Int) {
        if (from == null) return
        val cut = at.coerceIn(0, oldRaw.length)
        owner = from
        front = oldRaw.substring(0, cut)
        back = oldRaw.substring(cut)
        ahead = ""
        advanced = false
    }

    /**
     * The Enter's operation has run. [stayed] = the caret is still in the field that took the Enter
     * (the operation changed nothing): the record ends, and the view model gives the field its text back.
     */
    fun landed(stayed: Boolean) {
        advanced = !stayed
        if (stayed) owner = null
    }

    /** The caret is now in the field of [now]: a rejected buffer of that field no longer applies. */
    fun caretIn(now: FieldOwner?) {
        if (now != null && now == owner) owner = null
    }

    /** Reads a change of [from]'s field (field text [newField], caret [caretField] in field offsets). */
    fun judge(from: FieldOwner?, newField: String, caretField: Int): Judgement {
        val o = owner ?: return Judgement.Normal
        if (from == null || from != o || !newField.startsWith(FieldDiff.SENTINEL)) return Judgement.Normal
        val raw = newField.replace(FieldDiff.SENTINEL.toString(), "")
        val head = front + "\n"
        if (raw.length < head.length + back.length || !raw.startsWith(head) || !raw.endsWith(back)) {
            owner = null
            return Judgement.Normal
        }
        val typed = raw.substring(head.length, raw.length - back.length)
        if (typed == ahead) return Judgement.Ahead(emptyList())
        val caret = (FieldDiff.rawOffset(caretField) - head.length).coerceIn(0, typed.length)
        val span = FieldDiff.span(ahead, typed, caret)
        // The block that takes an edit is the one the last typed-ahead Enter made: offsets count from its line.
        val lineStart = ahead.lastIndexOf('\n') + 1
        ahead = typed
        return Judgement.Ahead(listOfNotNull(edit(span, lineStart)))
    }

    private fun edit(span: FieldDiff.Span, lineStart: Int): TypedAhead? = when {
        // An edit in a line the replay has already left cannot be placed.
        span.start < lineStart -> null
        span.start == span.end && span.inserted == "\n" -> TypedAhead.Enter(span.start - lineStart)
        span.start == span.end && span.inserted.isEmpty() -> null
        else -> TypedAhead.Edit(span.start - lineStart, span.end - span.start, span.inserted)
    }
}
