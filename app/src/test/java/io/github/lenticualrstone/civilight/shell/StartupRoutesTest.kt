package io.github.lenticualrstone.civilight.shell

import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.Onboarding
import io.github.lenticualrstone.civilight.navigation.OnboardingPage
import io.github.lenticualrstone.civilight.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** LIB-08 start routing, and the relaunch in a process that outlived the first run. */
class StartupRoutesTest {
    private val atProcessStart: List<Route> = listOf(Onboarding())

    @Test
    fun firstRunGoesToOnboarding_finishedGoesToLibrary() {
        assertEquals(listOf(Onboarding()), Startup.startRoutes(OnboardingState(), exampleInstalled = true))
        val done = OnboardingState(firstRunCompleted = true, resumePage = OnboardingPage.entries.last().name, exampleState = ExampleState.DELETED)
        assertEquals(listOf(Library), Startup.startRoutes(done, exampleInstalled = false))
    }

    @Test
    fun freshActivityAfterOnboardingFinished_opensLibrary() {
        // Process started on onboarding; the user finished it, left with back and opened the app again.
        val live = OnboardingState(firstRunCompleted = true, exampleState = ExampleState.DELETED)
        val routes = Startup.forNewActivity(atProcessStart, restored = false) { Startup.startRoutes(live, exampleInstalled = false) }
        assertEquals(listOf(Library), routes)
    }

    @Test
    fun freshActivityMidOnboarding_resumesLivePage() {
        val page = OnboardingPage.entries.last()
        val live = OnboardingState(resumePage = page.name)
        val routes = Startup.forNewActivity(atProcessStart, restored = false) { Startup.startRoutes(live, exampleInstalled = true) }
        assertEquals(listOf(Onboarding(page)), routes)
    }

    @Test
    fun beforePrepareFinishes_staysNull_andRestoredKeepsPrepared() {
        var read = false
        assertNull(Startup.forNewActivity(null, restored = false) { read = true; listOf(Library) })
        assertFalse("nothing is read before prepare() is done", read)
        assertEquals(atProcessStart, Startup.forNewActivity(atProcessStart, restored = true) { read = true; listOf(Library) })
        assertFalse(read)
    }
}
