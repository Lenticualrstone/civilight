package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max
import kotlin.math.min

/**
 * U-02: the caret line of a growing (multi-line) field stays in view while typing. When a new line (or a
 * caret move) would put the caret under the keyboard, under a bar or past the end of the scroll container,
 * the nearest scroll container scrolls just enough to show the caret line with a small margin. The whole
 * box is not brought in, only the caret line.
 *
 * The rules are plain Kotlin (JVM tested): [step] decides when to bring, [target] decides what to bring.
 */
object CaretInView {
    /** What the field holds now: whether it is being typed in, its text and its selection. */
    data class Typing(val focused: Boolean, val text: String, val selectionStart: Int, val selectionEnd: Int)

    /**
     * One step of the follow: [bring] says the caret line is brought into view now; [seen] is the state the
     * next step compares with. The caller sets `seen` to the current state once the bring has finished, so
     * a bring cut short by the next key is done again with the newer caret.
     */
    data class Step(val bring: Boolean, val seen: Typing?)

    /**
     * Brings only while the field is focused and after a change of its text or selection. Focus arriving
     * is left to the field itself (the system brings a newly focused field in), and the keyboard rising is
     * left to the screen (gallery sheets keep their whole block in view then), so the two never compete.
     * A range selection that changes without a text change (select all, dragging a handle) does not bring:
     * the view stays where the person is looking.
     */
    fun step(seen: Typing?, now: Typing): Step = when {
        !now.focused -> Step(bring = false, seen = null)
        seen == null -> Step(bring = false, seen = now)
        seen.text != now.text -> Step(bring = true, seen = seen)
        (seen.selectionStart != now.selectionStart || seen.selectionEnd != now.selectionEnd) &&
            now.selectionStart == now.selectionEnd -> Step(bring = true, seen = seen)
        seen != now -> Step(bring = false, seen = now)
        else -> Step(bring = false, seen = seen)
    }

    /** A vertical span in root px. */
    data class Span(val top: Float, val bottom: Float)

    /**
     * Where things are, in root px: the [caret] line; the text's own box ([field], not clipped) and the part
     * of it left inside every parent's bounds ([shown], null when nothing is left); the [frame] the screen's
     * scroll containers fill (the glass scaffold, or the whole window); the part of the frame not under a
     * bar or the keyboard ([clear]); and the [margin] kept around the caret line.
     */
    data class Geometry(
        val caret: Span,
        val field: Span,
        val shown: Span?,
        val frame: Span,
        val clear: Span,
        val margin: Float,
    )

    /**
     * The span (root px) to hand to the scroll container so the caret line lands inside [Geometry.clear]
     * with the margin, or null when it is already there.
     *
     * A scroll container only knows its own bounds (its viewport). Where the text box is cut by a parent,
     * that cut is the viewport edge; where it is not, the viewport is taken to be the frame (screens scroll
     * under their bars and the keyboard, with that much padding at the end). The span asked for lies just
     * past the viewport edge by the distance the caret line must travel, so the container scrolls exactly
     * that far: in a sheet (which sits above the keyboard) that is the caret line itself, on a screen it
     * also clears the bars and the keyboard.
     */
    fun target(g: Geometry): Span? {
        val m = g.margin
        val line = (g.caret.bottom - g.caret.top).coerceAtLeast(0f)
        val shown = g.shown ?: return Span(g.caret.top - m, g.caret.bottom + m)
        val viewTop = if (shown.top > g.field.top + EDGE) shown.top else g.frame.top
        val viewBottom = if (shown.bottom < g.field.bottom - EDGE) shown.bottom else g.frame.bottom
        val clearTop = max(viewTop, g.clear.top)
        val clearBottom = min(viewBottom, g.clear.bottom)
        val down = g.caret.bottom + m - clearBottom
        if (down > EDGE) return Span(viewBottom + down - line - m, viewBottom + down)
        val up = clearTop - (g.caret.top - m)
        if (up > EDGE) return Span(viewTop - up, viewTop - up + line + m)
        return null
    }

    /** Below half a pixel an edge counts as not cut and a distance as nothing. */
    private const val EDGE = 0.5f
}

/**
 * The glass scaffold's frame for [CaretInView]: where it is and how tall its top and bottom bars are, so a
 * field inside the scaffold's content keeps its caret line out from under the bars and the keyboard. Set by
 * `GlassScaffold` only.
 */
class CaretFrame internal constructor() {
    internal var coords: LayoutCoordinates? = null
    internal var topBar: Int = 0
    internal var bottomBar: Int = 0
}

/** The [CaretFrame] around the current content (null outside a glass scaffold, e.g. inside a sheet). */
val LocalCaretFrame = staticCompositionLocalOf<CaretFrame?> { null }

/**
 * Follows the caret of one text field ([rememberCaretFollow]). Pass [onTextLayout] to the field's
 * `onTextLayout` and put [caretFollow] on a box around the field's `innerTextField()` in its decoration
 * box, so the caret rect and the box share one coordinate space.
 */
class CaretFollow internal constructor() {
    internal val requester = BringIntoViewRequester()
    internal var coords: LayoutCoordinates? = null
    internal var layout by mutableStateOf<TextLayoutResult?>(null)

    /** The field's `onTextLayout`. */
    fun onTextLayout(result: TextLayoutResult) {
        layout = result
    }

    /** The caret line's rect in the inner text's coordinates, to bring into view, or null when it is in view. */
    internal fun target(shownOffset: Int, frame: CaretFrame?, keyboard: Int, margin: Float): Rect? {
        val c = coords?.takeIf { it.isAttached } ?: return null
        val l = layout ?: return null
        val caret = l.getCursorRect(shownOffset.coerceIn(0, l.layoutInput.text.length))
        val origin = c.positionInRoot().y
        val rootHeight = c.findRootCoordinates().size.height.toFloat()
        val clipped = c.boundsInRoot()
        val shown = if (clipped.width > 0f && clipped.height > 0f) CaretInView.Span(clipped.top, clipped.bottom) else null
        val frameCoords = frame?.coords?.takeIf { it.isAttached }
        val frameSpan = if (frameCoords != null) {
            val top = frameCoords.positionInRoot().y
            CaretInView.Span(top, top + frameCoords.size.height)
        } else {
            CaretInView.Span(0f, rootHeight)
        }
        val keyboardTop = rootHeight - keyboard
        val clear = if (frameCoords != null) {
            CaretInView.Span(frameSpan.top + frame.topBar, min(frameSpan.bottom - frame.bottomBar, keyboardTop))
        } else {
            CaretInView.Span(frameSpan.top, min(frameSpan.bottom, keyboardTop))
        }
        val span = CaretInView.target(
            CaretInView.Geometry(
                caret = CaretInView.Span(origin + caret.top, origin + caret.bottom),
                field = CaretInView.Span(origin, origin + c.size.height),
                shown = shown,
                frame = frameSpan,
                clear = clear,
                margin = margin,
            ),
        ) ?: return null
        return Rect(caret.left, span.top - origin, caret.right.coerceAtLeast(caret.left + 1f), span.bottom - origin)
    }
}

/** Marks the box around a field's `innerTextField()` as the caret's coordinate space and scroll target. */
fun Modifier.caretFollow(follow: CaretFollow): Modifier =
    this.bringIntoViewRequester(follow.requester).onGloballyPositioned { follow.coords = it }

/**
 * Keeps the caret line of a growing field in view while typing ([CaretInView]). [value] is the field's
 * value, [focused] its focus, [enabled] false for a field that does not grow (single line). [shownOffset]
 * maps the caret offset in the value to the shown text (the field's visual transformation).
 */
@Composable
fun rememberCaretFollow(
    value: TextFieldValue,
    focused: Boolean,
    enabled: Boolean = true,
    shownOffset: (TextFieldValue) -> Int = { it.selection.end },
): CaretFollow {
    val follow = remember { CaretFollow() }
    val currentValue by rememberUpdatedState(value)
    val currentFocused by rememberUpdatedState(focused && enabled)
    val currentShown by rememberUpdatedState(shownOffset)
    val frame = LocalCaretFrame.current
    val ime = WindowInsets.ime
    val navigation = WindowInsets.navigationBars
    val keyboard = remember(ime, navigation) { ime.union(navigation) }
    val density = LocalDensity.current
    LaunchedEffect(follow, frame, keyboard, density) {
        var seen: CaretInView.Typing? = null
        snapshotFlow {
            val v = currentValue
            CaretInView.Typing(currentFocused, v.text, v.selection.start, v.selection.end) to follow.layout
        }.collectLatest { (now, _) ->
            val step = CaretInView.step(seen, now)
            if (!step.bring) {
                seen = step.seen
                return@collectLatest
            }
            // The new text is composed and laid out in the next frame (and the scroll container takes the
            // field's new height); its placement is final when the frame after it starts. A newer key or
            // layout in between restarts this from the top.
            withFrameNanos { }
            withFrameNanos { }
            val rect = follow.target(
                shownOffset = currentShown(currentValue),
                frame = frame,
                keyboard = keyboard.getBottom(density),
                margin = with(density) { CARET_MARGIN.toPx() },
            )
            if (rect != null) follow.requester.bringIntoView(rect)
            seen = now
        }
    }
    return follow
}

/** Room kept below (or above) the caret line when it is brought into view. */
private val CARET_MARGIN = 24.dp
