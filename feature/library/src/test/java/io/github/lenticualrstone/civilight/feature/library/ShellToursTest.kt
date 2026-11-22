package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.library.tour.ShellTours
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
}
