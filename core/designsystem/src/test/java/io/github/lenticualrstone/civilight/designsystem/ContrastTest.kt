package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.ColorPalettes
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.ACC
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.BG
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.EDGE
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.HL
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.INK
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.INK2
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.INK3
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.ONPRI
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.ONR18
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.PRI
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.R18
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.RAISE
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.SEL
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.TINT
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole.WARN
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme.DARK
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme.LIGHT
import io.github.lenticualrstone.civilight.designsystem.theme.Rgba
import io.github.lenticualrstone.civilight.designsystem.theme.contrast
import io.github.lenticualrstone.civilight.designsystem.theme.round1
import io.github.lenticualrstone.civilight.designsystem.theme.round2
import io.github.lenticualrstone.civilight.designsystem.theme.tokenContrast
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DS-04: recomputes the contrast table from the token objects (T-DS-05 … T-DS-11). */
class ContrastTest {

    private fun c(theme: ResolvedTheme, fg: ColorRole, bg: ColorRole) = round2(tokenContrast(theme, fg, bg))

    @Test
    fun `T-DS-05 faintest text`() {
        assertEquals(6.15, c(DARK, INK3, BG), 0.0)
        assertEquals(5.42, c(LIGHT, INK3, BG), 0.0)
        assertTrue(c(DARK, INK3, BG) >= 5.40)
        assertTrue(c(LIGHT, INK3, BG) >= 5.40)
    }

    @Test
    fun `T-DS-06 accent marks`() {
        assertEquals(9.47, c(DARK, ACC, BG), 0.0)
        assertEquals(9.5, round1(tokenContrast(DARK, ACC, BG)), 0.0)
        assertEquals(6.97, c(LIGHT, ACC, BG), 0.0)
        assertEquals(7.0, round1(tokenContrast(LIGHT, ACC, BG)), 0.0)
        assertEquals(8.60, c(DARK, ACC, RAISE), 0.0)
        assertEquals(6.32, c(LIGHT, ACC, RAISE), 0.0)
    }

    @Test
    fun `T-DS-07 navy-core is never a mark`() {
        val navy = Rgba.hex(0x031C35)
        assertEquals(1.04, round2(contrast(navy, Rgba.hex(0x16171A))), 0.0)
        assertEquals(1.14, round2(contrast(navy, Rgba.hex(0x0B0C0E))), 0.0)
        val navyArgb = FixedColors.navyCore.toArgb()
        assertNotEquals(navyArgb, CivilightColors.dark.acc.toArgb())
        assertNotEquals(navyArgb, CivilightColors.light.acc.toArgb())
        assertEquals(navyArgb, CivilightColors.light.pri.toArgb())
        assertEquals(navyArgb, CivilightColors.dark.onpri.toArgb())
    }

    @Test
    fun `T-DS-08 meaningful borders`() {
        val darkEdge = ColorPalettes.dark.getValue(EDGE).over(ColorPalettes.dark.getValue(BG))
        val lightEdge = ColorPalettes.light.getValue(EDGE).over(ColorPalettes.light.getValue(BG))
        assertEquals(0x68686A, darkEdge.rgbHex)
        assertEquals(0x868686, lightEdge.rgbHex)
        assertEquals(3.52, c(DARK, EDGE, BG), 0.0)
        assertEquals(3.34, c(LIGHT, EDGE, BG), 0.0)
        assertTrue(c(DARK, EDGE, BG) >= 3.0 && c(LIGHT, EDGE, BG) >= 3.0)
    }

    @Test
    fun `T-DS-09 body text`() {
        assertEquals(16.60, c(DARK, INK, BG), 0.0)
        assertEquals(16.43, c(LIGHT, INK, BG), 0.0)
        assertEquals(15.07, c(DARK, INK, RAISE), 0.0)
        assertEquals(14.90, c(LIGHT, INK, RAISE), 0.0)
        assertEquals(9.66, c(DARK, INK2, BG), 0.0)
        assertEquals(7.87, c(LIGHT, INK2, BG), 0.0)
        assertTrue(c(LIGHT, INK2, BG) >= 7.0)
        assertEquals(8.77, c(DARK, INK2, RAISE), 0.0)
        assertEquals(7.13, c(LIGHT, INK2, RAISE), 0.0)
        assertEquals(5.59, c(DARK, INK3, RAISE), 0.0)
        assertEquals(4.91, c(LIGHT, INK3, RAISE), 0.0)
    }

    @Test
    fun `T-DS-10 fills with text`() {
        assertEquals(8.32, c(DARK, ONPRI, PRI), 0.0)
        assertEquals(15.76, c(LIGHT, ONPRI, PRI), 0.0)
        assertEquals(7.00, c(DARK, ONR18, R18), 0.0)
        assertEquals(6.54, c(LIGHT, ONR18, R18), 0.0)
        assertEquals(16.60, c(DARK, BG, INK), 0.0)
        assertEquals(16.43, c(LIGHT, BG, INK), 0.0)
    }

    @Test
    fun `T-DS-11 warn and r18 as text`() {
        assertEquals(10.88, c(DARK, WARN, BG), 0.0)
        assertEquals(5.43, c(LIGHT, WARN, BG), 0.0)
        assertEquals(7.00, c(DARK, R18, BG), 0.0)
        assertEquals(5.99, c(LIGHT, R18, BG), 0.0)
        listOf(DARK, LIGHT).forEach { t ->
            assertTrue(c(t, WARN, BG) >= 5.40)
            assertTrue(c(t, R18, BG) >= 5.40)
        }
    }

    @Test
    fun `DS-04 ink3 on tinted surfaces (Q-DS-04 values)`() {
        assertEquals(5.10, c(DARK, INK3, SEL), 0.0)
        assertEquals(4.08, c(DARK, INK3, HL), 0.0)
        assertEquals(5.30, c(DARK, INK3, TINT), 0.0)
        assertEquals(4.72, c(LIGHT, INK3, SEL), 0.0)
        assertEquals(4.43, c(LIGHT, INK3, HL), 0.0)
        assertEquals(4.91, c(LIGHT, INK3, TINT), 0.0)
    }
}
