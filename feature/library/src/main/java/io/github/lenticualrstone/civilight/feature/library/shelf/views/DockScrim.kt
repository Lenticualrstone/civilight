package io.github.lenticualrstone.civilight.feature.library.shelf.views

import io.github.lenticualrstone.civilight.designsystem.theme.ColorPalettes
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Rgba
import io.github.lenticualrstone.civilight.designsystem.theme.contrast
import kotlin.math.ceil

/**
 * (0.2.2) The glass dock floats over the 3D scene, and a cover under it may be any colour. Its fill is the
 * theme's `glass` colour at the smallest alpha that keeps every text colour on the dock at least [TARGET]:1
 * over the worst backdrop the scene can put there (white under the dark theme, black under the light one),
 * blur aside. Pure, so the JVM tests check the ratio for both themes.
 */
object DockScrim {
    const val TARGET = 4.5

    /** The text on the dock: title (`ink`), meta, counter and the peek row (`ink2`), 원고 더미's counter (`acc`). */
    val TEXT: List<ColorRole> = listOf(ColorRole.INK, ColorRole.INK2, ColorRole.ACC)

    /** Never thinner than the theme's own glass (DS-22: 58 % dark, 66 % light). */
    private fun glass(theme: ResolvedTheme): Rgba = ColorPalettes.of(theme).getValue(ColorRole.GLASS)

    fun worstBackdrop(theme: ResolvedTheme): Rgba = if (theme == ResolvedTheme.DARK) Rgba(255, 255, 255) else Rgba(0, 0, 0)

    /** The dock fill at [alpha] over [backdrop], opaque. */
    fun fillOver(theme: ResolvedTheme, alpha: Double, backdrop: Rgba = worstBackdrop(theme)): Rgba =
        glass(theme).copy(a = alpha).over(backdrop)

    /** The lowest ratio of the dock's [TEXT] on the fill at [alpha] over [backdrop]. */
    fun minContrast(theme: ResolvedTheme, alpha: Double, backdrop: Rgba = worstBackdrop(theme)): Double {
        val p = ColorPalettes.of(theme)
        val fill = fillOver(theme, alpha, backdrop)
        return TEXT.minOf { contrast(p.getValue(it), fill) }
    }

    /** The fill alpha (two decimals, rounded up) that keeps [minContrast] at [TARGET] or more. */
    fun alpha(theme: ResolvedTheme): Double {
        var a = ceil(glass(theme).a * 100) / 100
        while (a < 1.0 && minContrast(theme, a) < TARGET) a = ceil(a * 100 + 0.5) / 100
        return a.coerceAtMost(1.0)
    }

    /** [alpha] for both themes, computed once. */
    val dark: Float = alpha(ResolvedTheme.DARK).toFloat()
    val light: Float = alpha(ResolvedTheme.LIGHT).toFloat()
}
