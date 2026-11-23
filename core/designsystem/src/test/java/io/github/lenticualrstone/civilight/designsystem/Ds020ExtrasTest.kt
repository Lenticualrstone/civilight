package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.component.LensChipSize
import io.github.lenticualrstone.civilight.designsystem.component.TagColor
import io.github.lenticualrstone.civilight.designsystem.component.TagColorCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0) Tag palette (CST-75) and the small lens chip (CST-70, WLD-71). */
class Ds020ExtrasTest {
    @Test
    fun `tag palette tokens are stable, unique and unknown names have no colour`() {
        assertEquals(listOf("red", "orange", "yellow", "green", "teal", "blue", "violet", "gray"), TagColor.entries.map { it.token })
        TagColor.entries.forEach { assertEquals(it, TagColor.of(it.token)) }
        assertNull(TagColor.of(null))
        assertNull(TagColor.of("magenta"))
        TagColor.entries.forEach { assertNotEquals(it.color(dark = false), it.color(dark = true)) }
        assertEquals(TagColor.entries.size + 1, TagColorCopy.all.toSet().size)
    }

    @Test
    fun `the small lens chip is smaller than the regular one`() {
        val small = LensChipSize.of(true)
        val regular = LensChipSize.of(false)
        assertEquals(32, regular.height)
        assertTrue(small.height < regular.height && small.side < regular.side)
    }
}
