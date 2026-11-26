package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.ui.image.PortraitGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rule 19 (0.5 cross-review): a hidden character's portrait is held back while hidden items are locked. */
class PortraitGateTest {
    private val doc = ExampleProject.build()

    @Test
    fun hiddenCharacterPortraitIsLockedUntilItemsOpen() {
        val c = doc.cast.characters.first { it.deletedAt == null && !it.hidden }
        assertFalse(PortraitGate.isLocked(doc, c.key, itemsOpen = false))

        val hidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == c.key) it.copy(hidden = true) else it }))
        assertTrue(PortraitGate.isLocked(hidden, c.key, itemsOpen = false))
        assertFalse(PortraitGate.isLocked(hidden, c.key, itemsOpen = true))

        // No doc or no key: nothing to hold back (the tile draws its plain icon anyway).
        assertFalse(PortraitGate.isLocked(null, c.key, itemsOpen = false))
        assertFalse(PortraitGate.isLocked(hidden, null, itemsOpen = false))
    }
}
