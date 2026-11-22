package io.github.lenticualrstone.civilight.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** DS-09 spacing tokens. */
@Immutable
data class CivilightSpacing(
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 20.dp,
    val s6: Dp = 24.dp,
    val s7: Dp = 26.dp,
)

/** DS-10 corner radii. */
@Immutable
data class CivilightShapes(
    val xs: Dp = 5.dp,
    val sm: Dp = 12.dp,
    val cmd: Dp = 14.dp,
    val md: Dp = 16.dp,
    val card: Dp = 20.dp,
    val dialog: Dp = 26.dp,
    val lg: Dp = 28.dp,
) {
    val xsShape: RoundedCornerShape get() = RoundedCornerShape(xs)
    val smShape: RoundedCornerShape get() = RoundedCornerShape(sm)
    val cmdShape: RoundedCornerShape get() = RoundedCornerShape(cmd)
    val mdShape: RoundedCornerShape get() = RoundedCornerShape(md)
    val cardShape: RoundedCornerShape get() = RoundedCornerShape(card)
    val dialogShape: RoundedCornerShape get() = RoundedCornerShape(dialog)
    val lgShape: RoundedCornerShape get() = RoundedCornerShape(lg)

    /** Sheet top corners `radius-lg`. */
    val sheetShape: RoundedCornerShape get() = RoundedCornerShape(topStart = lg, topEnd = lg)

    /** `radius-full` (pills, chips, avatars, mic). */
    val full: RoundedCornerShape get() = RoundedCornerShape(50)
}

/** DS-10 size tokens. */
@Immutable
data class CivilightSizes(
    val hitMin: Dp = 44.dp,
    val cmdH: Dp = 52.dp,
    val measure: Dp = 560.dp,
    val cardW: Dp = 222.dp,
    val mapTool: Dp = 52.dp,
    val vertex: Dp = 14.dp,
    val icon: Dp = 20.dp,
    val mic: Dp = 32.dp,
    val grab: DpSize = DpSize(36.dp, 4.dp),
    val switchTrack: DpSize = DpSize(46.dp, 28.dp),
    val control: Dp = 22.dp,
    val sidePanel: Dp = 290.dp,
    val swipeRow: Dp = 192.dp,
)

/** DS-10 blur tokens plus the component blurs listed with them. */
@Immutable
data class CivilightBlur(
    val glass: Dp = 36.dp,
    val edge: Dp = 22.dp,
    val back: Dp = 2.dp,
    val chip: Dp = 12.dp,
    val hidden: Dp = 7.dp,
    val mapReveal: Dp = 6.dp,
    val glow: Dp = 34.dp,
    val glassSaturation: Float = 1.4f,
)

/** One drop shadow (offset Y and blur radius). */
@Immutable
data class ShadowSpec(val offsetY: Dp, val blur: Dp, val color: Color)

/** DS-10 `shadow-float` per theme; glass and manuscript have no shadow. */
@Immutable
data class CivilightElevation(
    val floatDark: ShadowSpec = ShadowSpec(20.dp, 50.dp, Rgba(0, 0, 0, 0.45).toColor()),
    val floatLight: ShadowSpec = ShadowSpec(20.dp, 50.dp, Rgba(30, 30, 28, 0.18).toColor()),
) {
    fun float(isDark: Boolean): ShadowSpec = if (isDark) floatDark else floatLight

    /** Popover shadow `0 10 28 shade` (DS-26). */
    fun popover(colors: CivilightColors): ShadowSpec = ShadowSpec(10.dp, 28.dp, colors.shade)
}

/** DS-09 layout metrics (board references; real layout follows CNV-32). */
object LayoutMetrics {
    val topBarCover: Dp = 112.dp
    val topBarScript: Dp = 118.dp
    val topBarInnerPanelTabs: Dp = 96.dp
    val topBarContentTopCover: Dp = 16.dp
    val topBarContentTopInner: Dp = 18.dp
    val topBarStartCover: Dp = 20.dp
    val topBarStartInner: Dp = 32.dp
    val bottomBar: Dp = 130.dp
    val bottomBarWithRow: Dp = 150.dp
    val bottomBarOnboarding: Dp = 120.dp
    val commandBarSide: Dp = 12.dp
    val commandBarBottom: Dp = 18.dp
    val sheetTop: Dp = 10.dp
    val sheetSide: Dp = 20.dp
    val sheetSidePicker: Dp = 16.dp
    val manuscriptGutter: Dp = 24.dp
    val manuscriptColumnInnerX: Dp = 40.dp
    val toastSide: Dp = 16.dp
    val toastAboveBar: Dp = 6.dp
    val sheetMaxTopBelowStatusBar: Dp = 56.dp
    val centredSheetMaxWidth: Dp = 560.dp
    val dialogMaxWidth: Dp = 420.dp

    /** Elevation order (z) of DS-10. */
    object Z {
        const val CONTENT: Float = 0f
        const val GLASS_BARS: Float = 2f
        const val BAR_CONTENTS: Float = 3f
        const val SCRIM: Float = 3f
        const val SHEET: Float = 4f
        const val TOAST: Float = 5f
        const val TOUR_DIM: Float = 7f
        const val TOOLTIP: Float = 9f
    }
}
