package io.github.lenticualrstone.civilight.feature.script.layout

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.ui.window.FoldPosture
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout

/**
 * Where a half-open device folds in tabletop posture (SCR-69): the fold's centre in window pixels,
 * [Float.NaN] when the posture is known but the hinge bounds are not (the middle is used), or null
 * when the device is not in tabletop posture.
 *
 * The shell's `WindowLayout.posture` wins when it is known (core does not fill it yet, see Requests);
 * otherwise the window's folding features are read here (Material adaptive `Posture`, a horizontal
 * half-open hinge). Book posture and flat screens give null: SCR-03's two panes already use the fold
 * as their border.
 */
@Composable
fun rememberTabletopFoldPx(): Float? {
    val shell = LocalWindowLayout.current.posture
    if (shell == FoldPosture.BOOK || shell == FoldPosture.FLAT) return null
    val posture = currentWindowAdaptiveInfo().windowPosture
    if (shell != FoldPosture.TABLETOP && !posture.isTabletop) return null
    val hinge = posture.hingeList.firstOrNull { !it.isVertical && !it.isFlat }
    return hinge?.bounds?.center?.y ?: Float.NaN
}

/** The fold's distance (dp) from the top of a frame whose top is [frameTopPx] in the window, or null for "the middle". */
fun foldInFrameDp(foldPx: Float, frameTopPx: Float, density: Float): Float? =
    if (foldPx.isNaN() || density <= 0f) null else (foldPx - frameTopPx) / density
