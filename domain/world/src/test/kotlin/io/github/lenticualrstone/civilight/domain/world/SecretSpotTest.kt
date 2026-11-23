package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.term.OutsideSpot
import io.github.lenticualrstone.civilight.model.text.DocBlock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0 CST-73) A character's secret text is left out of term readings while `이 탭 잠그기` is on. */
class SecretSpotTest {
    @Test
    fun `secret body links are listed only while the tab is not locked`() {
        val base = WorldFixture.stateB()
        val c = base.cast.characters.first { it.deletedAt == null }
        val body = listOf(DocBlock(key = WorldFixture.key(901), order = "a", runs = listOf(WorldFixture.link(WorldFixture.key(902), T1, "빛돌"))))
        fun withChar(locked: Boolean) = base.copy(
            cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == c.key) it.copy(secretBody = body, secretTabLocked = locked) else it }),
        )
        fun secretListed(locked: Boolean) = WorldFixture.queries(withChar(locked)).outside(T1)
            .any { (it.spot as? OutsideSpot.CharacterBody)?.let { s -> s.secret && s.characterKey == c.key } == true }
        assertTrue(secretListed(locked = false))
        assertFalse(secretListed(locked = true))
    }
}
