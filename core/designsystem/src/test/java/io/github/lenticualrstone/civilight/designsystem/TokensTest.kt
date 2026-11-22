package io.github.lenticualrstone.civilight.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.Bezier
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Rgba
import io.github.lenticualrstone.civilight.designsystem.theme.Terrain
import io.github.lenticualrstone.civilight.designsystem.theme.ThemeMode
import io.github.lenticualrstone.civilight.designsystem.theme.colorsFor
import io.github.lenticualrstone.civilight.designsystem.theme.resolveTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class TokensTest {

    private fun argb(c: Color): Int = c.toArgb()

    @Test
    fun `T-DS-01 dark tokens`() {
        val d = CivilightColors.dark
        assertEquals(0xFF0B0C0E.toInt(), argb(d.bg))
        assertEquals(0xFF16181C.toInt(), argb(d.raise))
        assertEquals(0xFFECECEF.toInt(), argb(d.ink))
        assertEquals(0xFFB4B6BD.toInt(), argb(d.ink2))
        assertEquals(0xFF8E9099.toInt(), argb(d.ink3))
        assertEquals(0xFF8FB8E6.toInt(), argb(d.acc))
        assertEquals(0xFF8FB8E6.toInt(), argb(d.pri))
        assertEquals(0xFF031C35.toInt(), argb(d.onpri))
        assertEquals(0xFFF2B66B.toInt(), argb(d.warn))
        assertEquals(0xFFFF6B5E.toInt(), argb(d.r18))
        assertEquals(0xFF0B0C0E.toInt(), argb(d.onr18))
        assertEquals(0xFF2A2D33.toInt(), argb(d.chip))
        assertEquals(0x1F8FB8E6, argb(d.sel))
        assertEquals(0x388FB8E6, argb(d.hl))
        assertEquals(0x12FFFFFF, argb(d.tint))
        assertEquals(0x17FFFFFF, argb(d.line))
        assertEquals(0x61FFFFFF, argb(d.edge))
        assertEquals(0x94101114.toInt(), argb(d.glass))
        assertEquals(0x73000000, argb(d.shade))
    }

    @Test
    fun `T-DS-02 light tokens`() {
        val l = CivilightColors.light
        assertEquals(0xFFF6F5F1.toInt(), argb(l.bg))
        assertEquals(0xFFECEAE4.toInt(), argb(l.raise))
        assertEquals(0xFF16171A.toInt(), argb(l.ink))
        assertEquals(0xFF4A4C52.toInt(), argb(l.ink2))
        assertEquals(0xFF62646B.toInt(), argb(l.ink3))
        assertEquals(0xFF1F5590.toInt(), argb(l.acc))
        assertEquals(0xFF031C35.toInt(), argb(l.pri))
        assertEquals(0xFFF6F5F1.toInt(), argb(l.onpri))
        assertEquals(0xFF8A5A00.toInt(), argb(l.warn))
        assertEquals(0xFFB3261E.toInt(), argb(l.r18))
        assertEquals(0xFFFFFFFF.toInt(), argb(l.onr18))
        assertEquals(0xFFE2E0D9.toInt(), argb(l.chip))
        assertEquals(0x12031C35, argb(l.sel))
        assertEquals(0x211F5590, argb(l.hl))
        assertEquals(0x0D16171A, argb(l.tint))
        assertEquals(0x1A16171A, argb(l.line))
        assertEquals(0x8016171A.toInt(), argb(l.edge))
        assertEquals(0xA8F6F5F1.toInt(), argb(l.glass))
        assertEquals(0x2E1E1E1C, argb(l.shade))
    }

    @Test
    fun `T-DS-03 alpha rounding`() {
        assertEquals(128, Rgba.alphaByte(0.5))
        assertEquals(26, Rgba.alphaByte(0.10))
        assertEquals(148, Rgba.alphaByte(0.58))
        assertEquals(0x80, Rgba(22, 23, 26, 0.5).alphaByte)
    }

    @Test
    fun `T-DS-04 theme resolution`() {
        assertEquals(ResolvedTheme.DARK, resolveTheme(ThemeMode.SYSTEM, systemDark = true))
        assertEquals(ResolvedTheme.LIGHT, resolveTheme(ThemeMode.SYSTEM, systemDark = false))
        assertEquals(ResolvedTheme.LIGHT, resolveTheme(ThemeMode.LIGHT, systemDark = true))
        assertEquals(ResolvedTheme.DARK, resolveTheme(ThemeMode.DARK, systemDark = false))
        assertEquals(0xFF0B0C0E.toInt(), argb(colorsFor(ResolvedTheme.DARK).bg))
        assertEquals(0xFFF6F5F1.toInt(), argb(colorsFor(ResolvedTheme.LIGHT).bg))
    }

    @Test
    fun `DS-03 fixed colours`() {
        assertEquals(0xFF031C35.toInt(), argb(FixedColors.navyCore))
        assertEquals(0xD1F4F4F6.toInt(), argb(FixedColors.onImage2))
        assertEquals(0xBFF4F4F6.toInt(), argb(FixedColors.onImageBadge))
        assertEquals(0xFFFF8A80.toInt(), argb(FixedColors.onImageR18))
        assertEquals(0xDB08090C.toInt(), argb(FixedColors.imageShadeBottom))
        assertEquals(0x800A0B0E.toInt(), argb(FixedColors.imageChipFill))
        assertEquals(5, FixedColors.mapArt(ResolvedTheme.DARK).size)
        assertEquals(0xFF3B4A66.toInt(), argb(FixedColors.mapArt(ResolvedTheme.DARK)[0]))
        assertEquals(0xFFB8C3D6.toInt(), argb(FixedColors.mapArt(ResolvedTheme.LIGHT)[4]))
        assertEquals(10, FixedColors.terrain.size)
        assertEquals(0xFF24394A.toInt(), argb(FixedColors.terrain.getValue(Terrain.DEEP)))
        assertEquals(0xFFC6CED4.toInt(), argb(FixedColors.terrain.getValue(Terrain.SNOW)))
        assertEquals(7, FixedColors.recorderGlow(ResolvedTheme.DARK).size)
        assertEquals(0xFF3E6FA8.toInt(), argb(FixedColors.recorderGlow(ResolvedTheme.DARK)[0]))
        assertEquals(0xFF5B8CC8.toInt(), argb(FixedColors.recorderGlow(ResolvedTheme.LIGHT)[0]))
    }

    @Test
    fun `T-DS-33 motion tokens`() {
        assertEquals(Bezier(0.2f, 0f, 0f, 1f), CivilightMotion.standard.curve)
        assertEquals(200, CivilightMotion.standard.durationMs)
        assertEquals(Bezier(0.05f, 0.7f, 0.1f, 1f), CivilightMotion.enter.curve)
        assertEquals(320, CivilightMotion.enter.durationMs)
        assertEquals(Bezier(0.3f, 0f, 0.8f, 0.15f), CivilightMotion.exit.curve)
        assertEquals(160, CivilightMotion.exit.durationMs)
        assertEquals(120, CivilightMotion.reducedFadeMs)
        assertEquals(360, CivilightMotion.foldOpenMs)
        assertEquals(240, CivilightMotion.foldCloseMs)
        assertEquals(0.965f, CivilightMotion.sheetBackScale, 0f)
        assertEquals(2.dp, CivilightMotion.sheetBackBlur)
        assertEquals(7.dp, CivilightMotion.unhideBlur)
        assertEquals(260, CivilightMotion.swipeMs)
        assertEquals(60, CivilightMotion.swipeStaggerMs)
        assertEquals(480, CivilightMotion.holoSweepMs)
        assertEquals(CivilightMotion.enterCurve, CivilightMotion.foldOpen.curve)
        assertEquals(CivilightMotion.exitCurve, CivilightMotion.foldClose.curve)
    }
}
