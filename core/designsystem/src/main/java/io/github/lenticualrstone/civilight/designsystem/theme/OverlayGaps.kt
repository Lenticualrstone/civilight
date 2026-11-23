package io.github.lenticualrstone.civilight.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DS-66 gaps between anything that floats (sheets, dialogs, popovers, menus, tooltips, the toast) and what sits
 * at the window's bottom: the bottom glass bar, the gesture bar or 3-button navigation, and the keyboard.
 *
 * `systemBottom` = `max(navigationBars.bottom, mandatorySystemGestures.bottom, ime.bottom)` in dp; no app-drawn
 * touch target may lie below `H − systemBottom`. The functions are pure (dp as `Float`, `y` measured from the
 * window top unless named otherwise), so the placement is tested on the JVM (T-DS-38, T-DS-39).
 */
object OverlayGaps {
    /** An overlay's bottom edge and `systemBottom` when no app chrome is below it (cover). */
    val edgeCompact: Dp = 16.dp

    /** The same on the inner screen. */
    val edgeInner: Dp = 24.dp

    /** An overlay's bottom edge and the top of the bottom glass bar (incl. its fade). */
    val aboveBar: Dp = 6.dp

    /** An overlay's bottom edge and the keyboard top when nothing rides on the keyboard. */
    val aboveKeyboard: Dp = 8.dp

    /** A bottom sheet's last content and `systemBottom` (keyboard hidden). */
    val sheetBottom: Dp = 24.dp

    /** A bottom sheet's last content and the keyboard top (the sheet rides on the keyboard). */
    val sheetBottomIme: Dp = 16.dp

    /** A popover, menu or tooltip and the frame's side edges (cover). */
    val sideCompact: Dp = 12.dp

    /** The same on the inner screen. */
    val sideInner: Dp = 24.dp

    /** Any overlay's top edge and the status-bar inset. */
    val top: Dp = 8.dp

    /** A centred dialog's or sheet's band to the status inset and to `systemBottom`; also its side margins. */
    val dialogBand: Dp = 24.dp

    /** The toast slot above `systemBottom` (DS-67): the tallest standard bottom bar 150 + 6. */
    val toastLiftMin: Dp = 156.dp

    /** Widest toast on the inner screen (DS-67). */
    val toastMaxWidthInner: Dp = 560.dp

    /** Toast side inset on cover screens (DS-27). */
    val toastSideCompact: Dp = 16.dp

    /** Bottom floating dialog (`ErrBio`) max width on the inner screen. */
    val floatingDialogMaxWidthInner: Dp = 420.dp

    fun edge(inner: Boolean): Dp = if (inner) edgeInner else edgeCompact

    fun side(inner: Boolean): Dp = if (inner) sideInner else sideCompact

    /**
     * A bottom sheet's content bottom padding: `systemBottom + 24` above the gesture bar with the keyboard
     * hidden; 16 above the keyboard top while it is up ([systemBottom] is then the keyboard).
     */
    fun sheetBottomPadding(systemBottom: Float, ime: Boolean): Float =
        if (ime) sheetBottomIme.value else systemBottom + sheetBottom.value

    /** The bottom floating dialog's distance from the window bottom: `systemBottom + edge`, or `+ 8` over the keyboard. */
    fun floatingDialogY(inner: Boolean, systemBottom: Float, ime: Boolean = false): Float =
        systemBottom + if (ime) aboveKeyboard.value else edge(inner).value

    /** The band a centred dialog or sheet is centred in: `[statusTop + 24, H − systemBottom − 24]`. */
    fun dialogBand(statusTop: Float, windowHeight: Float, systemBottom: Float): ClosedFloatingPointRange<Float> {
        val top = statusTop + dialogBand.value
        val bottom = windowHeight - systemBottom - dialogBand.value
        return top..maxOf(top, bottom)
    }

    /**
     * The lowest `y` a popover's bottom edge may reach: above `systemBottom + edge`, and 6 dp above the bottom
     * glass bar's top ([barTop], null when no bar is drawn).
     */
    fun popoverBottomLimit(windowHeight: Float, systemBottom: Float, barTop: Float?, inner: Boolean): Float {
        val system = windowHeight - systemBottom - edge(inner).value
        return if (barTop == null) system else minOf(system, barTop - aboveBar.value)
    }

    /**
     * A popover's top `y` (DS-26, DS-66): [gap] below its anchor if its bottom stays above
     * [popoverBottomLimit]; otherwise [gap] above the anchor; never above `statusTop + 8`.
     */
    fun popoverY(
        anchorTop: Float,
        anchorBottom: Float,
        height: Float,
        windowHeight: Float,
        systemBottom: Float,
        statusTop: Float,
        barTop: Float?,
        inner: Boolean = false,
        gap: Float = aboveBar.value,
    ): Float {
        val limit = popoverBottomLimit(windowHeight, systemBottom, barTop, inner)
        val below = anchorBottom + gap
        val y = if (below + height <= limit) below else anchorTop - gap - height
        return y.coerceAtLeast(statusTop + top.value)
    }

    /** A popover's start `x`: at its anchor, kept [side] dp inside both frame edges. */
    fun popoverX(anchorLeft: Float, width: Float, windowWidth: Float, inner: Boolean): Float {
        val s = side(inner).value
        val maxX = maxOf(s, windowWidth - width - s)
        return anchorLeft.coerceIn(s, maxX)
    }

    /** DS-67 lift above `systemBottom`: `max(156, tallestBottomChrome + 6)`, read once when the toast appears. */
    fun toastLift(tallestChrome: Float): Float = maxOf(toastLiftMin.value, tallestChrome + aboveBar.value)

    /** The toast's bottom edge from the window bottom: `systemBottom + lift`. */
    fun toastBottom(systemBottom: Float, tallestChrome: Float): Float = systemBottom + toastLift(tallestChrome)

    /** The toast's width: cover `W − 32`; inner `min(W − 48, 560)`, centred. */
    fun toastWidth(windowWidth: Float, inner: Boolean): Float =
        if (inner) minOf(windowWidth - 2 * edgeInner.value, toastMaxWidthInner.value) else windowWidth - 2 * toastSideCompact.value

    /** The toast's start `x`. */
    fun toastLeft(windowWidth: Float, inner: Boolean): Float = (windowWidth - toastWidth(windowWidth, inner)) / 2f
}
