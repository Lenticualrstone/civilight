package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.component.imageTileBase
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.Rgba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** GALLERY round 1: dimmed tiles on a glass sheet must not take the blurred back screen's colour. */
class ImageTileBaseTest {

    private fun rgba(c: androidx.compose.ui.graphics.Color) = Rgba(
        (c.red * 255 + 0.5f).toInt(),
        (c.green * 255 + 0.5f).toInt(),
        (c.blue * 255 + 0.5f).toInt(),
        c.alpha.toDouble(),
    )

    @Test
    fun `tile base is opaque in both themes`() {
        for (colors in listOf(CivilightColors.dark, CivilightColors.light)) {
            assertEquals(1f, imageTileBase(colors).alpha, 0f)
        }
    }

    @Test
    fun `a dimmed tile looks the same over any backdrop`() {
        val purple = Rgba(150, 40, 200)
        val yellow = Rgba(230, 200, 40)
        val art = Rgba(90, 120, 110, 0.38) // a preset dimmed to 38 % (add sheet `이미 있음`)
        for (colors in listOf(CivilightColors.dark, CivilightColors.light)) {
            val glass = rgba(colors.glass)
            val base = rgba(imageTileBase(colors))
            fun seen(backdrop: Rgba) = art.over(base.over(glass.over(backdrop)))
            assertEquals(seen(purple), seen(yellow))
            // Without the base the backdrop leaks through (the bug).
            assertNotEquals(art.over(glass.over(purple)), art.over(glass.over(yellow)))
        }
    }
}
