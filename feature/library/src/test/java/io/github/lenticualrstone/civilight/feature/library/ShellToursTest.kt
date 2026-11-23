package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.library.shelf.LibraryTourSteps
import io.github.lenticualrstone.civilight.feature.library.tour.ShellTours
import io.github.lenticualrstone.civilight.feature.library.tour.TourPlacement
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.CharacterTrack
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.MapEdit
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-19 tours woken (2026-11-23): which route runs which tour, and `다시 보기` runs it again. */
class ShellToursTest {
    private val p = ExampleKeys.PROJECT

    @Test
    fun `each module tour has exactly one host route, the 서재 hosts its own`() {
        val routes = listOf(
            Script(p),
            CharacterProfile(p, "rien"),
            World(p),
            Recording(),
            MapEdit(p, "map"),
            CharacterTrack(p),
        )
        assertEquals(
            listOf(TourModule.SCRIPT, TourModule.CAST, TourModule.WORLD_TIMELINE, TourModule.RECORDER, TourModule.MAP, TourModule.ROUTE_PLACE_TIME),
            routes.map(ShellTours::moduleOf),
        )
        // List or detail neighbours never run a second copy of the same tour.
        listOf(CastList(p), TermCard(p, "term"), RecorderInbox(), Library, null).forEach { assertNull("$it", ShellTours.moduleOf(it)) }
        // Every module is hosted somewhere: six by the shell, 서재 · 숨기기 by LibraryScreen.
        assertEquals(TourModule.entries.toSet() - TourModule.LIBRARY_HIDING, routes.mapNotNull(ShellTours::moduleOf).toSet())
    }

    @Test
    fun `a woken tour starts on first entry and again after 다시 보기`() {
        val fresh = OnboardingState()
        TourModule.entries.forEach { assertTrue(it.name, ModuleTours.shouldStart(fresh, it)) }
        val seen = TourModule.entries.fold(fresh) { s, m -> ModuleTours.markSeen(s, m) }
        TourModule.entries.forEach { m ->
            assertTrue(m.name, !ModuleTours.shouldStart(seen, m))
            assertTrue(m.name, ModuleTours.shouldStart(ModuleTours.reset(seen, m), m))
        }
    }

    @Test
    fun `a target scrolled away or clipped gets no hole, one on screen does`() {
        fun usable(l: Float, t: Float, r: Float, b: Float) = TourPlacement.usableTarget(l, t, r, b, hostWidth = 400f, hostHeight = 800f)
        assertTrue(usable(16f, 700f, 384f, 760f))
        // Partly inside (a tab row scrolled half out) still points at what shows.
        assertTrue(usable(360f, 700f, 440f, 744f))
        // boundsInRoot of an element clipped away by its parent: zero size.
        assertFalse(usable(0f, 0f, 0f, 0f))
        assertFalse(usable(120f, 300f, 120f, 340f))
        // Wholly outside the host (above, below, beside).
        assertFalse(usable(16f, -80f, 384f, -20f))
        assertFalse(usable(16f, 820f, 384f, 880f))
        assertFalse(usable(410f, 100f, 460f, 140f))
        // The host not measured yet.
        assertFalse(TourPlacement.usableTarget(16f, 100f, 384f, 160f, hostWidth = 0f, hostHeight = 0f))
    }

    @Test
    fun `on the inner screen the card moves under its target, at cover width it stays put`() {
        fun place(centerX: Float, hostWidth: Float, cardWidth: Float) = TourPlacement.of(
            100f, 150f, centerX, hostHeight = 800f, cardHeight = 160f, cardLeft = (hostWidth - cardWidth) / 2f, cardWidth = cardWidth,
            gap = 12f, edge = 16f, arrowInset = 24f, hostWidth = hostWidth,
        )
        // Inner screen 900 wide, card 328: a lens chip at the right edge (x 860) pulls the card right, kept 16 inside.
        val right = place(860f, 900f, 328f)
        assertEquals(900f - 16f - 328f - (900f - 328f) / 2f, right.cardShift, 0.01f)
        assertEquals(860f - (900f - 16f - 328f), right.arrowX, 0.01f)
        // A target in the middle needs no shift; one at the left edge pulls it left.
        assertEquals(0f, place(450f, 900f, 328f).cardShift, 0.01f)
        assertEquals(16f - (900f - 328f) / 2f, place(30f, 900f, 328f).cardShift, 0.01f)
        // Cover width: the card spans the host less 16 on each side, so it never moves.
        listOf(30f, 200f, 380f).forEach { assertEquals(0f, place(it, 400f, 368f).cardShift, 0.01f) }
        // Without the host's width (old callers) nothing moves either.
        assertEquals(0f, TourPlacement.of(100f, 150f, 860f, 800f, 160f, 286f, 328f, 12f, 16f, 24f).cardShift, 0.01f)
    }

    @Test
    fun `서재 · 숨기기 - every step has a hole, 설정 backs up the ones that may be missing`() {
        // Emulator QA f-07: the empty 서재 showed `서재 안내 · 3/3` with nothing marked.
        assertEquals(3, ModuleTours.stepCount(TourModule.LIBRARY_HIDING))
        assertEquals(listOf(0, 1, 2), listOf(LibraryTourSteps.HIDE, LibraryTourSteps.HIDDEN_GROUP, LibraryTourSteps.SETTINGS))
        assertEquals(
            (0 until ModuleTours.stepCount(TourModule.LIBRARY_HIDING)).toSet(),
            LibraryTourSteps.SETTINGS_FALLBACK.toSet() + LibraryTourSteps.SETTINGS,
        )
        // `프로젝트 숨기기` points at the ⋯ of the first shown row, else the open group's first row, else 설정.
        assertEquals("a", LibraryTourSteps.menuRow(listOf("a", "b"), listOf("h")))
        assertEquals("h", LibraryTourSteps.menuRow(emptyList(), listOf("h", "i")))
        assertNull(LibraryTourSteps.menuRow(emptyList(), emptyList()))
    }

    @Test
    fun `a step's own target wins, its fallback takes over off screen, neither gives no hole`() {
        val own = floatArrayOf(300f, 900f, 340f, 940f)
        val settings = floatArrayOf(560f, 100f, 610f, 150f)
        assertTrue(own.contentEquals(TourPlacement.pickTarget(own, settings, 1080f, 2000f)))
        // The hidden group below the fold (LazyColumn: an outside rect) gives way to 설정.
        val below = floatArrayOf(0f, 2100f, 1080f, 2300f)
        assertTrue(settings.contentEquals(TourPlacement.pickTarget(below, settings, 1080f, 2000f)))
        assertTrue(settings.contentEquals(TourPlacement.pickTarget(null, settings, 1080f, 2000f)))
        assertNull(TourPlacement.pickTarget(below, null, 1080f, 2000f))
        assertNull(TourPlacement.pickTarget(null, null, 1080f, 2000f))
    }
}
