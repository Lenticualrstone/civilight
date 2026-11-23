package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DS-66 `systemBottom`: `max(navigationBars, mandatorySystemGestures, ime)` at the window bottom. The lowest
 * line any app-drawn touch target or overlay may reach.
 */
@Composable
fun rememberSystemBottom(): Dp = maxOf(systemBottomWithoutKeyboard(), WindowInsets.ime.asPaddingValues().calculateBottomPadding())

/** [rememberSystemBottom] without the keyboard: the gesture bar or 3-button navigation. */
@Composable
fun systemBottomWithoutKeyboard(): Dp = maxOf(
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    WindowInsets.mandatorySystemGestures.asPaddingValues().calculateBottomPadding(),
)

/** True while the keyboard covers part of the window. */
@Composable
fun keyboardUp(): Boolean = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

/** The status-bar inset (the `top` of DS-66 is measured from it). */
@Composable
fun statusTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/**
 * The bottom chrome of the window (DS-66, DS-67): the height above `systemBottom` of each bottom glass bar or
 * pane action bar drawn now. The toast reads [tallest] once when it appears; popovers keep 6 dp above it. A
 * screen reports its bar with [ReportBottomChrome]; the shell provides one per window as [LocalBottomChrome].
 */
@Stable
class BottomChrome {
    private val heights = mutableStateMapOf<Any, Dp>()

    /** The tallest bottom chrome drawn now, above `systemBottom` (0 with no bar). */
    val tallest: Dp get() = heights.values.maxOrNull() ?: 0.dp

    fun report(token: Any, height: Dp) {
        if (heights[token] != height) heights[token] = height
    }

    fun clear(token: Any) {
        heights.remove(token)
    }
}

/** The window's [BottomChrome]; the shell provides one. */
val LocalBottomChrome = staticCompositionLocalOf { BottomChrome() }

/** Reports [heightAboveSystemBottom] as one bottom chrome of the window while this is composed. */
@Composable
fun ReportBottomChrome(heightAboveSystemBottom: Dp) {
    val chrome = LocalBottomChrome.current
    val token = remember { Any() }
    SideEffect { chrome.report(token, heightAboveSystemBottom.coerceAtLeast(0.dp)) }
    DisposableEffect(chrome, token) { onDispose { chrome.clear(token) } }
}

/**
 * True inside a window whose shell draws one toast for the whole window (DS-67). A [ToastHost] that is not
 * that window host then draws nothing, so a screen that still draws its own host never doubles the toast.
 */
val LocalWindowToasts = staticCompositionLocalOf { false }
