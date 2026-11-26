package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.ui.tags.TagForm
import io.github.lenticualrstone.civilight.ui.tags.TagSheetCopy
import io.github.lenticualrstone.civilight.ui.tags.TagSheetCreate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) The shared tag sheet's form state and copy (board `ImmTermTagSheet`). */
class TagSheetTest {
    @Test
    fun `form defaults to the kind's own scope and needs a name`() {
        val f = TagForm().opened()
        assertTrue(f.open)
        assertFalse(f.everywhere)
        assertFalse(f.canCreate)
        assertTrue(f.named("광물").canCreate)
        assertTrue(f.named("광물").scoped(true).everywhere)
    }

    @Test
    fun `created closes the form, taken keeps it with the visible action`() {
        val f = TagForm().opened().named("광물")
        assertEquals(TagForm(), f.after(TagSheetCreate.Created))
        val fits = f.after(TagSheetCreate.Taken("k", "광물", fits = true))
        assertEquals(TagSheetCopy.USE_EXISTING, fits.takenAction(canWiden = true))
        val other = f.after(TagSheetCreate.Taken("k", "주연", fits = false))
        assertEquals(TagSheetCopy.WIDEN, other.takenAction(canWiden = true))
        assertNull(other.takenAction(canWiden = false))
        // Typing again clears the message.
        assertNull(fits.named("광물2").taken)
        assertEquals(f, f.after(TagSheetCreate.Blank))
    }

    @Test
    fun `copy passes the lint`() {
        val all = TagSheetCopy.all + listOf(TagSheetCopy.subtitle("용어"), TagSheetCopy.ownScope("용어"), TagSheetCopy.taken("광물"))
        all.forEach { assertEquals(it, emptyList<Any>(), CopyLint.lint(it)) }
        assertEquals("용어에서 쓸 수 있는 것", TagSheetCopy.subtitle("용어"))
        assertEquals("용어 전용", TagSheetCopy.ownScope("용어"))
    }
}
