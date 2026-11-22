package io.github.lenticualrstone.civilight.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The theme colour token names of DS-02. Names are identical in both themes. */
enum class ColorRole {
    BG, RAISE, INK, INK2, INK3, ACC, PRI, ONPRI, SEL, HL, TINT, LINE, EDGE, CHIP, GLASS, SHADE, WARN, R18, ONR18,
}

/** DS-02 token values as declared (rgba with declared alpha), per theme. */
object ColorPalettes {
    val dark: Map<ColorRole, Rgba> = mapOf(
        ColorRole.BG to Rgba.hex(0x0B0C0E),
        ColorRole.RAISE to Rgba.hex(0x16181C),
        ColorRole.INK to Rgba.hex(0xECECEF),
        ColorRole.INK2 to Rgba.hex(0xB4B6BD),
        ColorRole.INK3 to Rgba.hex(0x8E9099),
        ColorRole.ACC to Rgba.hex(0x8FB8E6),
        ColorRole.PRI to Rgba.hex(0x8FB8E6),
        ColorRole.ONPRI to Rgba.hex(0x031C35),
        ColorRole.SEL to Rgba(143, 184, 230, 0.12),
        ColorRole.HL to Rgba(143, 184, 230, 0.22),
        ColorRole.TINT to Rgba(255, 255, 255, 0.07),
        ColorRole.LINE to Rgba(255, 255, 255, 0.09),
        ColorRole.EDGE to Rgba(255, 255, 255, 0.38),
        ColorRole.CHIP to Rgba.hex(0x2A2D33),
        ColorRole.GLASS to Rgba(16, 17, 20, 0.58),
        ColorRole.SHADE to Rgba(0, 0, 0, 0.45),
        ColorRole.WARN to Rgba.hex(0xF2B66B),
        ColorRole.R18 to Rgba.hex(0xFF6B5E),
        ColorRole.ONR18 to Rgba.hex(0x0B0C0E),
    )

    val light: Map<ColorRole, Rgba> = mapOf(
        ColorRole.BG to Rgba.hex(0xF6F5F1),
        ColorRole.RAISE to Rgba.hex(0xECEAE4),
        ColorRole.INK to Rgba.hex(0x16171A),
        ColorRole.INK2 to Rgba.hex(0x4A4C52),
        ColorRole.INK3 to Rgba.hex(0x62646B),
        ColorRole.ACC to Rgba.hex(0x1F5590),
        ColorRole.PRI to Rgba.hex(0x031C35),
        ColorRole.ONPRI to Rgba.hex(0xF6F5F1),
        ColorRole.SEL to Rgba(3, 28, 53, 0.07),
        ColorRole.HL to Rgba(31, 85, 144, 0.13),
        ColorRole.TINT to Rgba(22, 23, 26, 0.05),
        ColorRole.LINE to Rgba(22, 23, 26, 0.10),
        ColorRole.EDGE to Rgba(22, 23, 26, 0.5),
        ColorRole.CHIP to Rgba.hex(0xE2E0D9),
        ColorRole.GLASS to Rgba(246, 245, 241, 0.66),
        ColorRole.SHADE to Rgba(30, 30, 28, 0.18),
        ColorRole.WARN to Rgba.hex(0x8A5A00),
        ColorRole.R18 to Rgba.hex(0xB3261E),
        ColorRole.ONR18 to Rgba.hex(0xFFFFFF),
    )

    fun of(theme: ResolvedTheme): Map<ColorRole, Rgba> = if (theme == ResolvedTheme.DARK) dark else light
}

/** DS-02 colour tokens of one theme, provided by [CivilightTheme]. */
@Immutable
data class CivilightColors(
    val bg: Color,
    val raise: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val acc: Color,
    val pri: Color,
    val onpri: Color,
    val sel: Color,
    val hl: Color,
    val tint: Color,
    val line: Color,
    val edge: Color,
    val chip: Color,
    val glass: Color,
    val shade: Color,
    val warn: Color,
    val r18: Color,
    val onr18: Color,
    val isDark: Boolean,
) {
    fun of(role: ColorRole): Color = when (role) {
        ColorRole.BG -> bg
        ColorRole.RAISE -> raise
        ColorRole.INK -> ink
        ColorRole.INK2 -> ink2
        ColorRole.INK3 -> ink3
        ColorRole.ACC -> acc
        ColorRole.PRI -> pri
        ColorRole.ONPRI -> onpri
        ColorRole.SEL -> sel
        ColorRole.HL -> hl
        ColorRole.TINT -> tint
        ColorRole.LINE -> line
        ColorRole.EDGE -> edge
        ColorRole.CHIP -> chip
        ColorRole.GLASS -> glass
        ColorRole.SHADE -> shade
        ColorRole.WARN -> warn
        ColorRole.R18 -> r18
        ColorRole.ONR18 -> onr18
    }

    companion object {
        fun from(palette: Map<ColorRole, Rgba>, isDark: Boolean): CivilightColors {
            fun c(role: ColorRole) = requireNotNull(palette[role]) { "missing $role" }.toColor()
            return CivilightColors(
                bg = c(ColorRole.BG), raise = c(ColorRole.RAISE), ink = c(ColorRole.INK), ink2 = c(ColorRole.INK2),
                ink3 = c(ColorRole.INK3), acc = c(ColorRole.ACC), pri = c(ColorRole.PRI), onpri = c(ColorRole.ONPRI),
                sel = c(ColorRole.SEL), hl = c(ColorRole.HL), tint = c(ColorRole.TINT), line = c(ColorRole.LINE),
                edge = c(ColorRole.EDGE), chip = c(ColorRole.CHIP), glass = c(ColorRole.GLASS),
                shade = c(ColorRole.SHADE), warn = c(ColorRole.WARN), r18 = c(ColorRole.R18),
                onr18 = c(ColorRole.ONR18), isDark = isDark,
            )
        }

        val dark: CivilightColors = from(ColorPalettes.dark, isDark = true)
        val light: CivilightColors = from(ColorPalettes.light, isDark = false)
    }
}

/** The colours of a resolved theme (T-DS-04 `colorsFor`). */
fun colorsFor(theme: ResolvedTheme): CivilightColors =
    if (theme == ResolvedTheme.DARK) CivilightColors.dark else CivilightColors.light

/** Contrast of two tokens of one theme, alpha tokens composited over that theme's `bg` (DS-04). */
fun tokenContrast(theme: ResolvedTheme, fg: ColorRole, bg: ColorRole): Double {
    val p = ColorPalettes.of(theme)
    return contrast(p.getValue(fg), p.getValue(bg), p.getValue(ColorRole.BG))
}
