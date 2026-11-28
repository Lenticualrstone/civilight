package io.github.lenticualrstone.civilight.designsystem.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightBlur
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors

/** How glass renders (DS-22). The decision belongs to CNV-46; [resolve] implements its tiers. */
enum class GlassMode {
    BLUR, BLUR_COSTLY, SCRIM, OPAQUE;

    companion object {
        /** CNV-46 tiers: API ≥ 33 blur, 31–32 costly blur, ≤ 30 scrim; reduced transparency → opaque. */
        fun resolve(apiLevel: Int, reduceTransparency: Boolean): GlassMode = when {
            reduceTransparency -> OPAQUE
            apiLevel >= 33 -> BLUR
            apiLevel >= 31 -> BLUR_COSTLY
            else -> SCRIM
        }
    }
}

/** Which glass surface is drawn; bars get edge masks, chips use the 12 dp blur, DOCK is an unmasked arched tray. */
enum class GlassSurface { TOP_BAR, BOTTOM_BAR, DOCK, SHEET, PANEL, POPOVER, BANNER, CHIP }

/**
 * Edge mask of a bar: fully opaque over [opaqueFraction] of the height measured from the anchored edge,
 * then a linear fade to transparent at the far edge. [anchoredAtTop] is true for the top bar.
 */
data class GlassMask(val opaqueFraction: Float, val anchoredAtTop: Boolean)

/** Everything a glass surface draws for one mode (T-DS-34). */
data class GlassStyle(
    val fill: Color,
    val blur: Dp,
    val saturation: Float,
    val mask: GlassMask?,
    /** 1 dp border at the bottom edge (top bar in OPAQUE mode). */
    val bottomBorder: Color?,
    /** 1 dp border at the top edge (bottom bar in OPAQUE mode). */
    val topBorder: Color?,
)

/** Alpha byte of the SCRIM fallback: `raise` at 92 % (DS-22 proposed, Q-DS-14). */
const val SCRIM_ALPHA: Float = 0xEB / 255f

fun glassStyle(
    mode: GlassMode,
    surface: GlassSurface,
    colors: CivilightColors,
    blur: CivilightBlur = CivilightBlur(),
): GlassStyle {
    val mask = when (surface) {
        GlassSurface.TOP_BAR -> GlassMask(0.72f, anchoredAtTop = true)
        GlassSurface.BOTTOM_BAR -> GlassMask(0.70f, anchoredAtTop = false)
        else -> null
    }
    val blurRadius = if (surface == GlassSurface.CHIP) blur.chip else blur.glass
    return when (mode) {
        GlassMode.BLUR, GlassMode.BLUR_COSTLY -> GlassStyle(
            fill = colors.glass,
            blur = blurRadius,
            saturation = blur.glassSaturation,
            mask = mask,
            bottomBorder = null,
            topBorder = null,
        )
        GlassMode.SCRIM -> GlassStyle(
            fill = colors.raise.copy(alpha = SCRIM_ALPHA),
            blur = 0.dp,
            saturation = 1f,
            mask = mask,
            bottomBorder = null,
            topBorder = null,
        )
        GlassMode.OPAQUE -> GlassStyle(
            fill = colors.raise,
            blur = 0.dp,
            saturation = 1f,
            mask = null,
            bottomBorder = if (surface == GlassSurface.TOP_BAR) colors.line else null,
            topBorder = if (surface == GlassSurface.BOTTOM_BAR) colors.line else null,
        )
    }
}
