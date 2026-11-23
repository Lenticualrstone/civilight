package io.github.lenticualrstone.civilight.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.LocalBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.component.systemBottomWithoutKeyboard
import io.github.lenticualrstone.civilight.designsystem.theme.OverlayGaps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * The one toast of the window (DS-67), drawn by the shell above `NavDisplay` and the sheets and below the lock.
 * Screens and panes draw none of their own.
 *
 * - Position: cover 16 dp from both sides; inner centred, `min(W − 48, 560)` wide. Its bottom edge sits at
 *   `systemBottom + lift`, `lift = max(156, tallest bottom chrome + 6)` read **once** when the toast appears
 *   ([LocalBottomChrome]); afterwards it moves only with the keyboard, never because the screen behind changes.
 * - [hidden] (a lock is up, or something was relocked this frame) and an action toast whose project is not
 *   in [liveProjects] are not composed at all: gone in the same frame, no exit motion, so no name shows while
 *   locked. [ToastController.dismissNow] drops the node the same way.
 * - Motion is [ToastHost]'s (`enter` + 8 dp rise, `exit` fade, `swap` on replace); a finger on the toast holds
 *   its timer ([ToastHold.PRESSED]).
 */
@Composable
fun WindowToastHost(
    modifier: Modifier = Modifier,
    toasts: ToastController = LocalToasts.current,
    hidden: Boolean = false,
    liveProjects: Set<Key>? = null,
) {
    val toast by toasts.current.collectAsState()
    val instant by toasts.instantDismissals.collectAsState()
    val chrome = LocalBottomChrome.current
    val inner = LocalWindowLayout.current.isInner
    // The lift is read once per toast and frozen (a plain holder: reading it never recomposes anything).
    val frozen = remember { FrozenLift() }
    val shown = toast
    if (shown != null && shown.id != frozen.id) {
        frozen.id = shown.id
        frozen.lift = OverlayGaps.toastLift(chrome.tallest.value)
    }
    val released = shown != null && liveProjects != null && NoticeRules.releasedFrom(shown, liveProjects)
    val systemBottom = maxOf(systemBottomWithoutKeyboard(), rememberKeyboardBottom())
    val hold = remember(toasts) { { held: Boolean -> if (held) toasts.pause(ToastHold.PRESSED) else toasts.resume(ToastHold.PRESSED) } }
    BoxWithConstraints(
        modifier.fillMaxSize().padding(bottom = systemBottom + frozen.lift.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val width = OverlayGaps.toastWidth(maxWidth.value, inner).coerceAtLeast(0f)
        Box(Modifier.width(width.dp)) {
            if (!hidden && !released) {
                key(instant) {
                    ToastHost(
                        message = toast?.text,
                        modifier = Modifier.fillMaxWidth(),
                        actionLabel = toast?.actionLabel,
                        onAction = toast?.onAction?.let { { toasts.act() } },
                        id = toast?.id,
                        windowHost = true,
                        onHoldChange = hold,
                    )
                }
            }
        }
    }
}

private class FrozenLift {
    var id: Long? = null
    var lift: Float = OverlayGaps.toastLiftMin.value
}
