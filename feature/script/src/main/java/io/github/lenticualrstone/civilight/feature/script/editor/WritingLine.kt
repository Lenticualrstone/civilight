package io.github.lenticualrstone.civilight.feature.script.editor

/**
 * The fixed writing line (SCR-14): while typing, the line holding the caret keeps its screen
 * position and the content above moves instead. When the keyboard and toolbar would cover the caret
 * line, the manuscript scrolls by the least amount that shows it ([PROPOSED]).
 */
object WritingLine {
    /** Scroll to apply (px; positive moves the content up) and where the caret line's top ends up. */
    data class Adjust(val scrollBy: Float, val anchorTop: Float)

    /**
     * [anchorTop] is where the caret line's top should stay; [caretTop] / [caretBottom] are where the
     * caret line is now; [visibleTop] / [visibleBottom] bound the area not covered by the bars.
     */
    fun adjust(anchorTop: Float, caretTop: Float, caretBottom: Float, visibleTop: Float, visibleBottom: Float): Adjust {
        val height = caretBottom - caretTop
        var scroll = caretTop - anchorTop
        var top = caretTop - scroll
        if (top + height > visibleBottom) {
            scroll += top + height - visibleBottom
            top = caretTop - scroll
        }
        if (top < visibleTop) {
            scroll -= visibleTop - top
            top = visibleTop
        }
        return Adjust(scroll, top)
    }
}

/** Why the caret line's position is reported to [WritingLineTracker]. */
enum class CaretEvent {
    /** Typing, a caret move, or a new caret row: keep the line where it was (or show it). */
    EDIT,

    /** The visible area changed (keyboard or toolbar opened or closed): keep the line, show it. */
    VIEWPORT,

    /** The writer dragged the manuscript: the line's new place becomes the place to keep. */
    USER_SCROLL,

    /** Any other layout pass (including the tracker's own scroll): nothing to do. */
    LAYOUT,
}

/**
 * Holds where the writing line stays while the caret is in a block (SCR-14). The screen reports the
 * caret line in root coordinates; the tracker answers how far to scroll the manuscript.
 */
class WritingLineTracker {
    /** Where the caret line's top is kept; null until the first report after editing starts. */
    var anchorTop: Float? = null
        private set

    /** Editing ended: the next caret starts a new anchor where it is. */
    fun reset() {
        anchorTop = null
    }

    /** Scroll (px, positive moves content up) for a report; 0 when nothing should move. */
    fun onCaret(event: CaretEvent, caretTop: Float, caretBottom: Float, visibleTop: Float, visibleBottom: Float): Float {
        val anchor = anchorTop
        if (anchor == null || event == CaretEvent.USER_SCROLL) anchorTop = caretTop
        if (event == CaretEvent.USER_SCROLL || event == CaretEvent.LAYOUT) return 0f
        val adjust = WritingLine.adjust(anchorTop ?: caretTop, caretTop, caretBottom, visibleTop, visibleBottom)
        anchorTop = adjust.anchorTop
        return adjust.scrollBy
    }
}
