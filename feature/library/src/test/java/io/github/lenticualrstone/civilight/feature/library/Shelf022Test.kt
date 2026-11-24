package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.designsystem.theme.ColorPalettes
import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.designsystem.theme.Rgba
import io.github.lenticualrstone.civilight.designsystem.theme.contrast
import io.github.lenticualrstone.civilight.feature.library.shelf.RecentsSheet
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfState
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi
import io.github.lenticualrstone.civilight.feature.library.shelf.views.DockScrim
import io.github.lenticualrstone.civilight.feature.library.shelf.views.LookA
import io.github.lenticualrstone.civilight.feature.library.shelf.views.LookB
import io.github.lenticualrstone.civilight.feature.library.shelf.views.LookC
import io.github.lenticualrstone.civilight.feature.library.shelf.views.ShelfScene
import io.github.lenticualrstone.civilight.navigation.RouteCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.2: the 서재 3D scene is the whole screen; the stage sits between the glass bars and the dock. */
class Shelf022Test {
    // A cover phone: top bar 96, bottom bar (search) 130, dock (caption + buttons + peek + gap) 152.
    private val top = 96f
    private val bottom = 130f
    private val dock = 152f

    @Test
    fun aTallRoomKeepsTheBoardCentredBetweenTheBars() {
        val h = 1100f
        val fit = ShelfScene.stageFit(h, top, bottom, dock)
        assertEquals(ShelfScene.STAGE_H, fit.height, 0f)
        assertEquals(1f, fit.scale, 0f)
        val from = top - ShelfScene.HEAD_OVERLAP
        val to = h - bottom - dock + ShelfScene.DOCK_OVERLAP
        // Centred in the room, inside it at both ends.
        assertEquals((from + to) / 2f, fit.top + fit.height / 2f, 1e-3f)
        assertTrue(fit.top >= from && fit.bottom <= to)
    }

    @Test
    fun aPhoneScalesTheBoardToTheRoomBetweenHeaderAndDock() {
        val h = 844f
        val fit = ShelfScene.stageFit(h, top, bottom, dock)
        val room = (h - bottom - dock + ShelfScene.DOCK_OVERLAP) - (top - ShelfScene.HEAD_OVERLAP)
        assertTrue(room in ShelfScene.STAGE_MIN_H..ShelfScene.STAGE_H)
        assertEquals(room, fit.height, 1e-3f)
        assertEquals(room / ShelfScene.STAGE_H, fit.scale, 1e-6f)
        // The stage reaches only into the faded foot of the top bar and the dock's top edge.
        assertEquals(top - ShelfScene.HEAD_OVERLAP, fit.top, 1e-3f)
        assertEquals(h - bottom - dock + ShelfScene.DOCK_OVERLAP, fit.bottom, 1e-3f)
    }

    @Test
    fun aShortPaneKeepsTheMinimumStageAndRunsItUnderTheGlassDock() {
        // The inner display's list pane: about 560 dp, the frame bar 120 below, the dock 152.
        val h = 560f
        val fit = ShelfScene.stageFit(h, top, 120f, dock)
        assertEquals(ShelfScene.STAGE_MIN_H, fit.height, 0f)
        assertEquals(0.6f, fit.scale, 1e-6f)
        // Starts right under the header (never above it) and ends under the dock, not under the search field.
        assertEquals(top - ShelfScene.HEAD_OVERLAP, fit.top, 1e-3f)
        val dockTop = h - 120f - dock
        assertTrue(fit.bottom > dockTop)
        assertTrue(fit.bottom <= h - 120f)
    }

    @Test
    fun aShortWindowPinsTheDockBetweenTheBarsOnScreen() {
        // A landscape phone (the review's case): 360 dp of content, header 80, search field 72, dock 168 with its gap.
        val h = 360f
        val barTop = 80f
        val barBottom = 72f
        val dockH = 168f
        val dockTop = ShelfScene.dockTop(h, barTop, barBottom, dockH)
        // The scene is the viewport, so this is the dock on screen: right above the search field, below the header.
        assertEquals(h - barBottom - dockH, dockTop, 1e-3f)
        assertTrue(dockTop >= barTop)
        assertTrue(dockTop + dockH <= h - barBottom + 1e-3f)
        // A large font (dock about 230 dp): the dock is capped to the room between the bars and never reaches
        // under the header; its caption scrolls inside it instead.
        val big = 230f
        assertEquals(h - barTop - barBottom - ShelfScene.DOCK_GAP, ShelfScene.dockMaxH(h, barTop, barBottom), 1e-3f)
        assertEquals(barTop, ShelfScene.dockTop(h, barTop, barBottom, big), 1e-3f)
        // The stage starts under the header and runs on under the glass dock.
        val fit = ShelfScene.stageFit(h, barTop, barBottom, dockH)
        assertEquals(barTop - ShelfScene.HEAD_OVERLAP, fit.top, 1e-3f)
        assertEquals(ShelfScene.STAGE_MIN_H, fit.height, 0f)
        // A portrait phone keeps its whole dock: no cap.
        assertEquals(844f - bottom - dock, ShelfScene.dockTop(844f, top, bottom, dock), 1e-3f)
        // No room at all never yields a negative cap.
        assertEquals(0f, ShelfScene.dockMaxH(100f, barTop, barBottom), 0f)
    }

    @Test
    fun noBarsStartTheStageAtTheTop() {
        val fit = ShelfScene.stageFit(400f, 0f, 0f, 0f)
        assertEquals(0f, fit.top, 0f)
        assertEquals(400f + ShelfScene.DOCK_OVERLAP, fit.height, 0f)
    }

    @Test
    fun fadeBandsMeltTheStageIntoTheBackgroundAndNeverEatIt() {
        val fit = ShelfScene.stageFit(1100f, top, bottom, dock)
        assertEquals(ShelfScene.FADE_TOP, fit.fadeTop, 0f)
        assertEquals(ShelfScene.FADE_BOTTOM, fit.fadeBottom, 0f)
        // A small stage: each band at most a quarter of it, so half of the board always shows unfaded.
        assertEquals(25f, ShelfScene.fadeTop(100f), 0f)
        assertEquals(25f, ShelfScene.fadeBottom(100f), 0f)
        assertTrue(fit.fadeTop + fit.fadeBottom <= fit.height / 2f)
    }

    @Test
    fun theStageWallsEndOnTheAppBackground() {
        // The fades draw `bg`; the boards' far wall colours already are `bg`, so the scene reads as one surface.
        val darkBg = ColorPalettes.dark.getValue(ColorRole.BG)
        val lightBg = ColorPalettes.light.getValue(ColorRole.BG)
        assertEquals(Rgba.hex(0x0B0C0E), darkBg)
        assertEquals(Rgba.hex(0xF6F5F1), lightBg)
        assertEquals(darkBg.toColor(), LookA.dark.wallB)
        assertEquals(lightBg.toColor(), LookA.light.wallB)
        assertEquals(darkBg.toColor(), LookB.dark.wall2)
        assertEquals(lightBg.toColor(), LookB.light.wall2)
        assertEquals(darkBg.toColor(), LookC.dark.bg)
        assertEquals(lightBg.toColor(), LookC.light.bg)
    }

    @Test
    fun dockTextKeepsFourAndAHalfToOneOverAnyCover() {
        for (theme in ResolvedTheme.entries) {
            val alpha = DockScrim.alpha(theme)
            val p = ColorPalettes.of(theme)
            // Never thinner than the theme's glass, never more than opaque.
            assertTrue(alpha >= p.getValue(ColorRole.GLASS).a - 1e-9 && alpha <= 1.0)
            // Over white, black, the theme's bg and a mid grey: every dock text colour keeps 4.5:1.
            val backdrops = listOf(Rgba(255, 255, 255), Rgba(0, 0, 0), p.getValue(ColorRole.BG), Rgba(128, 128, 128))
            for (backdrop in backdrops) {
                val fill = DockScrim.fillOver(theme, alpha, backdrop)
                for (role in DockScrim.TEXT) {
                    val ratio = contrast(p.getValue(role), fill)
                    assertTrue("$theme $role over $backdrop: $ratio", ratio >= DockScrim.TARGET)
                }
            }
            // The smallest such alpha: a hundredth less fails over the worst backdrop.
            if (alpha > p.getValue(ColorRole.GLASS).a + 0.01) assertTrue(DockScrim.minContrast(theme, alpha - 0.01) < DockScrim.TARGET)
        }
        assertEquals(DockScrim.alpha(ResolvedTheme.DARK).toFloat(), DockScrim.dark, 0f)
        assertEquals(DockScrim.alpha(ResolvedTheme.LIGHT).toFloat(), DockScrim.light, 0f)
    }

    @Test
    fun theRecentsSheetIsAboutNoProjectAndSurvivesARestart() {
        assertEquals(null, ShelfState.projectOf(RecentsSheet))
        val ui = ShelfUi(loaded = true)
        assertEquals(0, ShelfState.sheetsToClose(ui, listOf(RecentsSheet)))
        assertEquals(listOf(RecentsSheet), RouteCodec.decodeAll(RouteCodec.encodeAll(listOf(RecentsSheet))))
    }
}
