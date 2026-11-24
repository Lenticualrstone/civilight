package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.feature.cast.profile.SecretGate
import io.github.lenticualrstone.civilight.feature.cast.profile.SecretTabGate
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * (0.2.0 QA N-43) The 비밀 tab was locked, then the device's screen lock was removed: the prompt cannot
 * be shown. The tab stays closed (nothing secret is drawn) and the screen says why instead of a silent tap.
 */
class SecretTabNoDeviceLockTest {
    private val prompting = SecretTabGate.select(SecretTabGate.open(ProfileTab.OVERVIEW, gameTools = true, locked = true), ProfileTab.SECRET, locked = true)

    @Test
    fun `no device lock keeps the previous tab and asks for the notice`() {
        assertEquals(SecretGate.PROMPTING, prompting.gate)
        val after = SecretTabGate.answered(prompting, AuthResult.UNAVAILABLE)
        assertEquals(ProfileTab.OVERVIEW, after.shown(locked = true))
        assertEquals(SecretGate.LOCKED, after.gate)
        assertTrue(SecretTabGate.needsDeviceLock(AuthResult.UNAVAILABLE))
        // Tapping 비밀 again asks again (and so shows the notice again), it is never a dead tap.
        assertEquals(SecretGate.PROMPTING, SecretTabGate.select(after, ProfileTab.SECRET, locked = true).gate)
    }

    @Test
    fun `success opens, cancel and failure stay quiet`() {
        assertEquals(SecretGate.OPEN, SecretTabGate.answered(prompting, AuthResult.SUCCESS).gate)
        for (r in listOf(AuthResult.CANCELLED, AuthResult.FAILED)) {
            assertEquals(ProfileTab.OVERVIEW, SecretTabGate.answered(prompting, r).shown(locked = true))
            assertFalse(SecretTabGate.needsDeviceLock(r))
        }
        assertFalse(SecretTabGate.needsDeviceLock(AuthResult.SUCCESS))
    }
}
