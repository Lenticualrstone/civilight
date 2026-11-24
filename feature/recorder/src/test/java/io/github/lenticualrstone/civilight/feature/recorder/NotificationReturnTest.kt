package io.github.lenticualrstone.civilight.feature.recorder

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.outside.OutsideEntries
import io.github.lenticualrstone.civilight.navigation.BackStackNavigator
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.openFromOutside
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * (0.2.0 QA M-08) The ongoing notification's route names the surface the take started on, so it never
 * equals the home mic's route. Pushed as is it stacked a second recorder over the live one; the second
 * closed with the take and the first stayed as an empty overlay that swallowed the next mic tap.
 */
class NotificationReturnTest {
    @Test
    fun `the notification's route differs from the mic's, and opening it returns to the live recorder`() {
        val mic = Recording(projectKey = "P")
        val fromNotification = OutsideEntries.resume(EntryRoutes.entryOf(mic))
        assertNotEquals(mic, fromNotification)

        val stack = mutableListOf<NavKey>(Library, ProjectHome("P"))
        val nav = BackStackNavigator(stack)
        nav.push(mic)
        nav.openFromOutside(fromNotification)
        assertEquals(listOf(Library, ProjectHome("P"), mic), stack)
        // 기록함에 두기 closes the recorder; the next mic tap opens a fresh one.
        nav.pop()
        nav.push(mic)
        assertEquals(listOf(Library, ProjectHome("P"), mic), stack)
    }
}
