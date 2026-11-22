package io.github.lenticualrstone.civilight.designsystem

import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.nodeCardLook
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcons
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** DS-50 node card variants (rule 20 deleted reference, CNV-11) and the icons added in the core pass. */
class NodeCardLookTest {
    private val c = CivilightColors.light

    @Test
    fun `a card is filled with a line outline`() {
        val look = nodeCardLook(memo = false, selected = false, deleted = false, colors = c)
        assertTrue(look.filled)
        assertEquals(c.line, look.outline)
        assertFalse(look.dashed || look.dotted)
        assertEquals(c.ink, look.titleColor)
    }

    @Test
    fun `a memo is dashed and a deleted reference is dotted, never both`() {
        val memo = nodeCardLook(memo = true, selected = false, deleted = false, colors = c)
        assertTrue(memo.dashed && !memo.dotted && !memo.filled)
        val deleted = nodeCardLook(memo = false, selected = false, deleted = true, colors = c)
        assertTrue(deleted.dotted && !deleted.dashed && !deleted.filled)
        assertEquals(c.ink3, deleted.outline)
        assertEquals(c.ink2, deleted.titleColor)
        val deletedMemo = nodeCardLook(memo = true, selected = false, deleted = true, colors = c)
        assertTrue(deletedMemo.dotted && !deletedMemo.dashed)
    }

    @Test
    fun `selection wins the outline but a deleted node keeps its dots`() {
        val look = nodeCardLook(memo = false, selected = true, deleted = true, colors = c)
        assertEquals(c.acc, look.outline)
        assertEquals(1.5.dp, look.outlineWidth)
        assertTrue(look.dotted)
    }

    @Test
    fun `the icons added for library and canvas parse`() {
        assertEquals("M5 12h14", IconId.Minus.pathData)
        for (id in listOf(IconId.Pencil, IconId.Shield, IconId.Download, IconId.Minus)) {
            assertEquals(id.name, CivilightIcons.get(id).name)
        }
    }
}
