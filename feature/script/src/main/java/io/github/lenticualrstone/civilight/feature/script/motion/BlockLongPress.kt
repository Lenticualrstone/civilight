package io.github.lenticualrstone.civilight.feature.script.motion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import io.github.lenticualrstone.civilight.designsystem.foundation.GestureRules
import kotlinx.coroutines.withTimeoutOrNull

/**
 * (0.2.4) The pure decision of a block's long press (JVM-tested). While the finger is down and the platform's
 * long-press timeout has not passed, the press keeps waiting; it is cancelled when the finger lifts, moves past
 * the touch slop (0.2.3 [GestureRules.cancelsLongPress]: the swipe row or the manuscript's scroll now owns it),
 * another gesture consumed the finger, or a second finger lands.
 */
object BlockPress {
    enum class Verdict { WAIT, CANCEL }

    /**
     * Whether a finger that lands may become a long press: not when it lands while the manuscript is still moving
     * ([scrolling]); that touch only catches the fling, as a RecyclerView takes it on Android (0.2.4 review).
     */
    fun mayStart(scrolling: Boolean): Boolean = !scrolling

    fun verdict(dx: Float, dy: Float, slop: Float, lifted: Boolean, takenByOther: Boolean, pointers: Int): Verdict = when {
        lifted || takenByOther || pointers > 1 -> Verdict.CANCEL
        GestureRules.cancelsLongPress(dx, dy, slop) -> Verdict.CANCEL
        else -> Verdict.WAIT
    }
}

/**
 * (0.2.4) Long press on a block: held still within the touch slop for the system long-press timeout, it gives
 * the long-press haptic and calls [onLongPress]. The rest of that press is swallowed before anything under it
 * sees it, so the block's tap, a link tap, the swipe row and the scroll never act on the same finger. It
 * watches on the final pass, so a swipe or a scroll that takes the finger first cancels it ([BlockPress]); a finger
 * that lands while the manuscript is [scrolling] only stops it and never opens the menu.
 * TalkBack's long click is set by the caller (with the menu's actions as custom actions).
 */
@Composable
fun Modifier.blockLongPress(enabled: Boolean, scrolling: () -> Boolean = { false }, onLongPress: () -> Unit): Modifier {
    if (!enabled) return this
    val haptic = LocalHapticFeedback.current
    val action by rememberUpdatedState(onLongPress)
    val isScrolling by rememberUpdatedState(scrolling)
    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            // Read as the down arrives: a down that catches a fling still finds the list scrolling (the fling, or the
            // drag the list starts at once on that down), so the press only stops the list.
            if (!BlockPress.mayStart(isScrolling())) return@awaitEachGesture
            val slop = viewConfiguration.touchSlop
            val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                var verdict = BlockPress.Verdict.WAIT
                while (verdict == BlockPress.Verdict.WAIT) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    verdict = if (change == null) {
                        BlockPress.Verdict.CANCEL
                    } else {
                        val d = change.position - down.position
                        BlockPress.verdict(
                            d.x, d.y, slop,
                            lifted = !change.pressed,
                            takenByOther = change.isConsumed,
                            pointers = event.changes.count { it.pressed },
                        )
                    }
                }
                true
            }
            if (cancelled != null) return@awaitEachGesture
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            action()
            // Swallow the rest of this press on the initial pass: no tap, no link, no swipe, no scroll.
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
                if (event.changes.none { it.pressed }) break
            }
        }
    }
}
