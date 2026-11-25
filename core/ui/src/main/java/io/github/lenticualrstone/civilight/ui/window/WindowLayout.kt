package io.github.lenticualrstone.civilight.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/** Width class of the app window (CNV-32): Compact < 600 dp (cover 475), Medium 600–840 (inner 704), Expanded ≥ 840 (933). */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Device posture (CNV-36). Detecting it needs `androidx.window` (FoldingFeature), which is not a
 * dependency of core:ui or app yet, so it stays [UNKNOWN] and layouts treat it like [FLAT].
 */
enum class FoldPosture { UNKNOWN, FLAT, TABLETOP, BOOK }

/**
 * (0.2.3, r3) The three Galaxy Z Fold 8 frames layouts are drawn for: the cover (~475 × 751 dp, 10:16), the
 * inner screen upright (~704 × 933) and on its side (~933 × 704). Any other window falls into the nearest one by
 * width class and orientation ([WindowLayout.frame]).
 */
enum class WindowFrame { COVER, INNER_PORTRAIT, INNER_LANDSCAPE }

/**
 * The window as layouts see it (CNV-32, r3). Cover ≈ 475 dp wide (Compact); inner ≈ 704 upright (Medium) or
 * 933 on its side (Expanded). Screens fill [heightDp] exactly: nothing here assumes a board height.
 *
 * (0.2.3) The `펼친 화면 배치` choice is gone: list modules always draw two panes on the inner screen, and the
 * side panel closes by its own seam button instead (`PanelCollapse.kt`).
 */
@Immutable
data class WindowLayout(
    val widthDp: Int,
    val heightDp: Int,
    val posture: FoldPosture = FoldPosture.UNKNOWN,
) {
    val widthClass: WidthClass get() = classifyWidth(widthDp)

    /** Medium or Expanded: side panels, popovers, cards and centred sheets instead of bottom sheets. */
    val isInner: Boolean get() = widthClass != WidthClass.COMPACT

    /** Wider than tall. */
    val isLandscape: Boolean get() = widthDp > heightDp

    /** Which r3 frame this window is drawn as. */
    val frame: WindowFrame get() = classifyFrame(widthDp, heightDp)

    /** List modules draw list and detail side by side (CNV-33 `목록 + 상세`); always on the inner screen. */
    val twoPanes: Boolean get() = isInner

    companion object {
        /** r3 board sizes (`nmlib.COVER`, `INNER`, `INNER_L`; R3_LAYOUT.md). */
        const val COVER_REFERENCE_DP = 475
        const val COVER_REFERENCE_HEIGHT_DP = 751
        const val INNER_REFERENCE_DP = 704
        const val INNER_REFERENCE_HEIGHT_DP = 933

        /** Width of the list pane beside a detail when the frame is upright (r3 `Two*`: 300 + 1 + 403). */
        const val LIST_PANE_PORTRAIT_DP = 300

        /** Width of the list pane beside a detail when the frame is on its side (r3 `L*`: 320 + 1 + 612). */
        const val LIST_PANE_LANDSCAPE_DP = 320

        /** Reading measure of unfolded documents. */
        const val MEASURE_DP = 560

        val Cover = WindowLayout(COVER_REFERENCE_DP, COVER_REFERENCE_HEIGHT_DP)
        val Inner = WindowLayout(INNER_REFERENCE_DP, INNER_REFERENCE_HEIGHT_DP)
        val InnerLandscape = WindowLayout(INNER_REFERENCE_HEIGHT_DP, INNER_REFERENCE_DP)

        fun classifyWidth(widthDp: Int): WidthClass = when {
            widthDp < 600 -> WidthClass.COMPACT
            widthDp < 840 -> WidthClass.MEDIUM
            else -> WidthClass.EXPANDED
        }

        /** Compact → cover; otherwise upright or on its side by the window's own proportions. */
        fun classifyFrame(widthDp: Int, heightDp: Int): WindowFrame = when {
            classifyWidth(widthDp) == WidthClass.COMPACT -> WindowFrame.COVER
            widthDp > heightDp -> WindowFrame.INNER_LANDSCAPE
            else -> WindowFrame.INNER_PORTRAIT
        }
    }
}

/** The current [WindowLayout]; provided by the shell, recomputed on every fold / resize. */
val LocalWindowLayout = compositionLocalOf { WindowLayout.Cover }

/**
 * Measures the app window (not the screen) on every configuration change (CNV-32). The size is the window's
 * container size, so a screen laid out with `fillMaxSize` fills it exactly (r3 fill rule: no leftover band).
 */
@Composable
fun rememberWindowLayout(posture: FoldPosture = FoldPosture.UNKNOWN): WindowLayout {
    val density = LocalDensity.current
    val size = LocalWindowInfo.current.containerSize
    val configuration = LocalConfiguration.current
    val widthDp = if (size.width > 0) with(density) { size.width.toDp().value.toInt() } else configuration.screenWidthDp
    val heightDp = if (size.height > 0) with(density) { size.height.toDp().value.toInt() } else configuration.screenHeightDp
    return remember(widthDp, heightDp, posture) { WindowLayout(widthDp, heightDp, posture) }
}
