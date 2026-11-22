package io.github.lenticualrstone.civilight.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import io.github.lenticualrstone.civilight.model.app.UnfoldedLayout

/** Width class of the app window (CNV-32): Compact < 600 dp, Medium 600–840, Expanded ≥ 840. */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Device posture (CNV-36). Detecting it needs `androidx.window` (FoldingFeature), which is not a
 * dependency of core:ui or app yet, so it stays [UNKNOWN] and layouts treat it like [FLAT].
 */
enum class FoldPosture { UNKNOWN, FLAT, TABLETOP, BOOK }

/**
 * The window as layouts see it. Cover ≈ 390 dp wide (Compact); inner ≈ 800 dp (Medium/Expanded).
 * [unfoldedLayout] is the user's `펼친 화면 배치` choice (LIB-66, CNV-33).
 */
@Immutable
data class WindowLayout(
    val widthDp: Int,
    val heightDp: Int,
    val unfoldedLayout: UnfoldedLayout = UnfoldedLayout.LIST_DETAIL,
    val posture: FoldPosture = FoldPosture.UNKNOWN,
) {
    val widthClass: WidthClass get() = classifyWidth(widthDp)

    /** Medium or Expanded: side panels, popovers, cards and centred sheets instead of bottom sheets. */
    val isInner: Boolean get() = widthClass != WidthClass.COMPACT

    /** List modules draw list and detail side by side (CNV-33 `목록 + 상세`). */
    val twoPanes: Boolean get() = isInner && unfoldedLayout == UnfoldedLayout.LIST_DETAIL

    /** List modules draw one centred 560 column (CNV-33 `가운데 한 칸`). */
    val centerColumn: Boolean get() = isInner && unfoldedLayout == UnfoldedLayout.CENTER_COLUMN

    companion object {
        const val COVER_REFERENCE_DP = 390
        const val INNER_REFERENCE_DP = 800

        /** Width of the list pane in two panes (390 at the 800 reference). */
        const val LIST_PANE_DP = 390

        /** Reading measure of the centred column and unfolded documents. */
        const val MEASURE_DP = 560

        val Cover = WindowLayout(COVER_REFERENCE_DP, 844)
        val Inner = WindowLayout(INNER_REFERENCE_DP, 800)

        fun classifyWidth(widthDp: Int): WidthClass = when {
            widthDp < 600 -> WidthClass.COMPACT
            widthDp < 840 -> WidthClass.MEDIUM
            else -> WidthClass.EXPANDED
        }
    }
}

/** The current [WindowLayout]; provided by the shell, recomputed on every fold / resize. */
val LocalWindowLayout = compositionLocalOf { WindowLayout.Cover }

/** Measures the app window (not the screen) on every configuration change (CNV-32). */
@Composable
fun rememberWindowLayout(unfoldedLayout: UnfoldedLayout): WindowLayout {
    val density = LocalDensity.current
    val size = LocalWindowInfo.current.containerSize
    val configuration = LocalConfiguration.current
    val widthDp = if (size.width > 0) with(density) { size.width.toDp().value.toInt() } else configuration.screenWidthDp
    val heightDp = if (size.height > 0) with(density) { size.height.toDp().value.toInt() } else configuration.screenHeightDp
    return remember(widthDp, heightDp, unfoldedLayout) { WindowLayout(widthDp, heightDp, unfoldedLayout) }
}
