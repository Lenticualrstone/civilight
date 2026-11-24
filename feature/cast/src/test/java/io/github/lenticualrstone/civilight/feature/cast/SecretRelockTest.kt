package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.feature.cast.profile.SecretTabGate
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** (CST-73) The 비밀 tab body is not kept composed (fading out) once a relock hides it. */
class SecretRelockTest {
    @Test
    fun `a relocked secret body is dropped at once, an unlocked one keeps its swap`() {
        val opened = SecretTabGate.answered(SecretTabGate.select(SecretTabGate.open(ProfileTab.OVERVIEW, true, locked = true), ProfileTab.SECRET, locked = true), AuthResult.SUCCESS)
        assertTrue(SecretTabGate.composesBody(ProfileTab.SECRET, opened, locked = true))
        // App to the background, then back: the gate is not open, so the outgoing secret slot is empty.
        val stopped = SecretTabGate.background(opened)
        assertFalse(SecretTabGate.composesBody(ProfileTab.SECRET, stopped, locked = true))
        val back = SecretTabGate.foreground(stopped, locked = true)
        assertFalse(SecretTabGate.composesBody(ProfileTab.SECRET, back, locked = true))
        // Other tabs are composed as usual.
        assertTrue(SecretTabGate.composesBody(ProfileTab.OVERVIEW, back, locked = true))
        // Leaving an open locked tab drops its body too.
        assertFalse(SecretTabGate.composesBody(ProfileTab.SECRET, SecretTabGate.select(opened, ProfileTab.OVERVIEW, locked = true), locked = true))
        // Not locked: the body fades out normally.
        val plain = SecretTabGate.open(ProfileTab.SECRET, true, locked = false)
        assertTrue(SecretTabGate.composesBody(ProfileTab.SECRET, SecretTabGate.select(plain, ProfileTab.OVERVIEW, locked = false), locked = false))
    }
}
